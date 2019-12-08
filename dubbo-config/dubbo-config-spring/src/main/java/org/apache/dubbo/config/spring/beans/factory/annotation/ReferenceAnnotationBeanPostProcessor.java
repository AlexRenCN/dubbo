/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.spring.beans.factory.annotation;

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;

import com.alibaba.spring.beans.factory.annotation.AbstractAnnotationBeanPostProcessor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static com.alibaba.spring.util.AnnotationUtils.getAttributes;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.apache.dubbo.config.spring.beans.factory.annotation.ServiceBeanNameBuilder.create;
import static org.springframework.util.StringUtils.hasText;

/**
 * 针对带Reference注解的类，在spring注册Bean Definition时候的后置处理器
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that Consumer service {@link Reference} annotated fields
 *
 * @since 2.5.7
 */
public class ReferenceAnnotationBeanPostProcessor extends AbstractAnnotationBeanPostProcessor implements
        ApplicationContextAware, ApplicationListener {

    /**
     * The bean name of {@link ReferenceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "referenceAnnotationBeanPostProcessor";

    /**
     * Cache size
     */
    private static final int CACHE_SIZE = Integer.getInteger(BEAN_NAME + ".cache.size", 32);

    /**
     * 引用接口名-引用对象缓存
     */
    private final ConcurrentMap<String, ReferenceBean<?>> referenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    /**
     * 引用接口名-引用对象反射器缓存
     */
    private final ConcurrentHashMap<String, ReferenceBeanInvocationHandler> localReferenceBeanInvocationHandlerCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    /**
     * 自动注入的属性-引用对象反射器缓存
     */
    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedFieldReferenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    /**
     * 自动注入的方法-引用对象反射器缓存
     */
    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedMethodReferenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    private ApplicationContext applicationContext;

    /**
     * To support the legacy annotation that is @com.alibaba.dubbo.config.annotation.Reference since 2.7.3
     */
    public ReferenceAnnotationBeanPostProcessor() {
        super(Reference.class, com.alibaba.dubbo.config.annotation.Reference.class);
    }

    /**
     * Gets all beans of {@link ReferenceBean}
     *
     * @return non-null read-only {@link Collection}
     * @since 2.5.9
     */
    public Collection<ReferenceBean<?>> getReferenceBeans() {
        return referenceBeanCache.values();
    }

    /**
     * Get {@link ReferenceBean} {@link Map} in injected field.
     *
     * @return non-null {@link Map}
     * @since 2.5.11
     */
    public Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> getInjectedFieldReferenceBeanMap() {
        return Collections.unmodifiableMap(injectedFieldReferenceBeanCache);
    }

    /**
     * Get {@link ReferenceBean} {@link Map} in injected method.
     *
     * @return non-null {@link Map}
     * @since 2.5.11
     */
    public Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> getInjectedMethodReferenceBeanMap() {
        return Collections.unmodifiableMap(injectedMethodReferenceBeanCache);
    }

    /**
     * 获取需要注入的引用接口
     * @param attributes
     * @param bean
     * @param beanName
     * @param injectedType
     * @param injectedElement
     * @return
     * @throws Exception
     */
    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {
        /**
         * 获取Service引用接口的名称
         * The name of bean that annotated Dubbo's {@link Service @Service} in local Spring {@link ApplicationContext}
         */
        String referencedBeanName = buildReferencedBeanName(attributes, injectedType);

        /**
         * 获取Reference引用接口的名称
         * The name of bean that is declared by {@link Reference @Reference} annotation injection
         */
        String referenceBeanName = getReferenceBeanName(attributes, injectedType);

        //创建引用接口对象
        ReferenceBean referenceBean = buildReferenceBeanIfAbsent(referenceBeanName, attributes, injectedType);

        //注册对象到spring的类工厂中
        registerReferenceBean(referencedBeanName, referenceBean, attributes, injectedType);

        //对Autowired修饰的属性/方法和引用对象的关系进行缓存
        cacheInjectedReferenceBean(referenceBean, injectedElement);

        //获取或者创建一个远程引用接口的代理类
        return getOrCreateProxy(referencedBeanName, referenceBeanName, referenceBean, injectedType);
    }

    /**
     * 在spring中注册引用的对象
     * Register an instance of {@link ReferenceBean} as a Spring Bean
     *
     * @param referencedBeanName The name of bean that annotated Dubbo's {@link Service @Service} in the Spring {@link ApplicationContext}
     * @param referenceBean      the instance of {@link ReferenceBean} is about to register into the Spring {@link ApplicationContext}
     * @param attributes         the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param interfaceClass     the {@link Class class} of Service interface
     * @since 2.7.3
     */
    private void registerReferenceBean(String referencedBeanName, ReferenceBean referenceBean,
                                       AnnotationAttributes attributes,
                                       Class<?> interfaceClass) {

        //获取支持配置的spring 对象工厂
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();

        //获取Reference引用接口的名称
        String beanName = getReferenceBeanName(attributes, interfaceClass);

        //如果找得到这个对象，说明他是一个本地实现
        if (existsServiceBean(referencedBeanName)) { // If @Service bean is local one
            /**
             * 从类工厂获取类定义
             * Get  the @Service's BeanDefinition from {@link BeanFactory}
             * Refer to {@link ServiceAnnotationBeanPostProcessor#buildServiceBeanDefinition}
             */
            AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) beanFactory.getBeanDefinition(referencedBeanName);
            //获取引用的具体类
            RuntimeBeanReference runtimeBeanReference = (RuntimeBeanReference) beanDefinition.getPropertyValues().get("ref");
            //获取类的name
            // The name of bean annotated @Service
            String serviceBeanName = runtimeBeanReference.getBeanName();

            // 注册别名而不是新的bean名称，以减少重复的bean
            // register Alias rather than a new bean name, in order to reduce duplicated beans
            beanFactory.registerAlias(serviceBeanName, beanName);
        //找不到对象，这是一个远程引用对象
        } else { // Remote @Service Bean
            //如果还没有注册这个引用对象
            if (!beanFactory.containsBean(beanName)) {
                //将引用对象以单例模式的形式注册在spring的类工厂中
                beanFactory.registerSingleton(beanName, referenceBean);
            }
        }
    }

    /**
     * 获取Reference引用接口的名称
     * Get the bean name of {@link ReferenceBean} if {@link Reference#id() id attribute} is present,
     * or {@link #generateReferenceBeanName(AnnotationAttributes, Class) generate}.
     *
     * @param attributes     the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param interfaceClass the {@link Class class} of Service interface
     * @return non-null
     * @since 2.7.3
     */
    private String getReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        //优先使用Reference里配置的id
        // id attribute appears since 2.7.3
        String beanName = getAttribute(attributes, "id");
        if (!hasText(beanName)) {
            //生成引用接口的名称
            beanName = generateReferenceBeanName(attributes, interfaceClass);
        }
        return beanName;
    }

    /**
     * 生成引用接口的名称（用于Reference注解修饰的引用接口）
     * Build the bean name of {@link ReferenceBean}
     *
     * @param attributes     the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param interfaceClass the {@link Class class} of Service interface
     * @return
     * @since 2.7.3
     */
    private String generateReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        StringBuilder beanNameBuilder = new StringBuilder("@Reference");

        //拼接每一个属性键可值
        if (!attributes.isEmpty()) {
            beanNameBuilder.append('(');
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                beanNameBuilder.append(entry.getKey())
                        .append('=')
                        .append(entry.getValue())
                        .append(',');
            }
            // replace the latest "," to be ")"
            beanNameBuilder.setCharAt(beanNameBuilder.lastIndexOf(","), ')');
        }

        //再加上接口名
        beanNameBuilder.append(" ").append(interfaceClass.getName());

        return beanNameBuilder.toString();
    }

    /**
     * 是否在上下文中可以查找到远程引用接口
     * @param referencedBeanName
     * @return
     */
    private boolean existsServiceBean(String referencedBeanName) {
        return applicationContext.containsBean(referencedBeanName);
    }

    /**
     * 获取或者创建一个远程引用接口的代理类
     * Get or Create a proxy of {@link ReferenceBean} for the specified the type of Dubbo service interface
     *
     * @param referencedBeanName   The name of bean that annotated Dubbo's {@link Service @Service} in the Spring {@link ApplicationContext}
     * @param referenceBeanName    the bean name of {@link ReferenceBean}
     * @param referenceBean        the instance of {@link ReferenceBean}
     * @param serviceInterfaceType the type of Dubbo service interface
     * @return non-null
     * @since 2.7.4
     */
    private Object getOrCreateProxy(String referencedBeanName, String referenceBeanName, ReferenceBean referenceBean, Class<?> serviceInterfaceType) {
        //如果在本地能找到引用的接口
        if (existsServiceBean(referencedBeanName)) { // If the local @Service Bean exists, build a proxy of ReferenceBean
            //创建一个新的代理类
            return newProxyInstance(getClassLoader(), new Class[]{serviceInterfaceType},
                    wrapInvocationHandler(referenceBeanName, referenceBean));
        //应该初始化远程引用接口的代理类并立即获取
        } else { // ReferenceBean should be initialized and get immediately
            /**
             * TODO, if we can make sure this happens after {@link DubboLifecycleComponentApplicationListener},
             * TODO, then we can avoid starting bootstrap in here, because bootstrap should has been started.
             */
            //通过spring FactoryBean接口的get方法，使用工厂方法创建一个代理对象
            return referenceBean.get();
        }
    }

    /**
     * Wrap an instance of {@link InvocationHandler} that is used to get the proxy of {@link ReferenceBean} after
     * the specified local referenced bean that annotated {@link @Service} exported.
     *
     * @param referenceBeanName the bean name of {@link ReferenceBean}
     * @param referenceBean     the instance of {@link ReferenceBean}
     * @return non-null
     * @since 2.7.4
     */
    private InvocationHandler wrapInvocationHandler(String referenceBeanName, ReferenceBean referenceBean) {
        return localReferenceBeanInvocationHandlerCache.computeIfAbsent(referenceBeanName, name ->
                new ReferenceBeanInvocationHandler(referenceBean));
    }

    private static class ReferenceBeanInvocationHandler implements InvocationHandler {

        private final ReferenceBean referenceBean;

        private Object bean;

        private ReferenceBeanInvocationHandler(ReferenceBean referenceBean) {
            this.referenceBean = referenceBean;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result;
            try {
                if (bean == null) { // If the bean is not initialized, invoke init()
                    // issue: https://github.com/apache/dubbo/issues/3429
                    init();
                }
                result = method.invoke(bean, args);
            } catch (InvocationTargetException e) {
                // re-throws the actual Exception.
                throw e.getTargetException();
            }
            return result;
        }

        /**
         * 使用spring的dubbo工厂类创建引用
         */
        private void init() {
            this.bean = referenceBean.get();
        }
    }

    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
                                                 Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        return buildReferencedBeanName(attributes, injectedType) +
                "#source=" + (injectedElement.getMember()) +
                "#attributes=" + getAttributes(attributes, getEnvironment());
    }

    /**
     * 获取Service引用接口的名称
     * @param attributes           the attributes of {@link Reference @Reference}
     * @param serviceInterfaceType the type of Dubbo's service interface
     * @return The name of bean that annotated Dubbo's {@link Service @Service} in local Spring {@link ApplicationContext}
     */
    private String buildReferencedBeanName(AnnotationAttributes attributes, Class<?> serviceInterfaceType) {
        //构建需要引用的接口名构造器
        ServiceBeanNameBuilder serviceBeanNameBuilder = create(attributes, serviceInterfaceType, getEnvironment());
        return serviceBeanNameBuilder.build();
    }

    /**
     * 创建引用接口bean
     * @param referenceBeanName
     * @param attributes
     * @param referencedType
     * @return
     * @throws Exception
     */
    private ReferenceBean buildReferenceBeanIfAbsent(String referenceBeanName, AnnotationAttributes attributes,
                                                     Class<?> referencedType)
            throws Exception {

        //先从缓存里获取对应的bean
        ReferenceBean<?> referenceBean = referenceBeanCache.get(referenceBeanName);

        if (referenceBean == null) {
            //如果没有缓存，就使用当前上下文中注解属性来创建一个引用对象的构造器
            ReferenceBeanBuilder beanBuilder = ReferenceBeanBuilder
                    .create(attributes, applicationContext)
                    .interfaceClass(referencedType);
            //创建引用对象的bean并放到缓存中
            referenceBean = beanBuilder.build();
            referenceBeanCache.put(referenceBeanName, referenceBean);
        //如果有缓存但是类型不同，说明出现冲突，应该抛出异常
        } else if (!referencedType.isAssignableFrom(referenceBean.getInterfaceClass())) {
            throw new IllegalArgumentException("reference bean name " + referenceBeanName + " has been duplicated, but interfaceClass " +
                    referenceBean.getInterfaceClass().getName() + " cannot be assigned to " + referencedType.getName());
        }
        return referenceBean;
    }

    /**
     * 对Autowired修饰的属性/方法和引用对象的关系进行缓存
     * @param referenceBean
     * @param injectedElement
     */
    private void cacheInjectedReferenceBean(ReferenceBean referenceBean,
                                            InjectionMetadata.InjectedElement injectedElement) {
        //InjectedElement标识被Autowired修饰的字段和方法
        //如果自动注入的是属性
        if (injectedElement.getMember() instanceof Field) {
            injectedFieldReferenceBeanCache.put(injectedElement, referenceBean);
        //如果自动注入的是方法
        } else if (injectedElement.getMember() instanceof Method) {
            injectedMethodReferenceBeanCache.put(injectedElement, referenceBean);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        //如果是一个接口对外暴露的事件
        if (event instanceof ServiceBeanExportedEvent) {
            //处理接口对外暴露的事件
            onServiceBeanExportEvent((ServiceBeanExportedEvent) event);
        }
    }

    /**
     * 处理接口对外暴露的事件
     * @param event
     */
    private void onServiceBeanExportEvent(ServiceBeanExportedEvent event) {
        //通过事件获取广播的接口
        ServiceBean serviceBean = event.getServiceBean();
        //初始化ReferenceBeanInvocationHandler，用于将Reference注解的接口注册到spring中
        initReferenceBeanInvocationHandler(serviceBean);
    }

    /**
     * 初始化ReferenceBeanInvocationHandler，用于将Reference注解的接口注册到spring中
     * @param serviceBean
     */
    private void initReferenceBeanInvocationHandler(ServiceBean serviceBean) {
        //获取远程引用接口的类名
        String serviceBeanName = serviceBean.getBeanName();
        //如果还没有在缓存中就初始化
        // Remove ServiceBean when it's exported
        ReferenceBeanInvocationHandler handler = localReferenceBeanInvocationHandlerCache.remove(serviceBeanName);
        //使用spring的dubbo工厂类创建引用
        // Initialize
        if (handler != null) {
            handler.init();
        }
    }


    @Override
    public void destroy() throws Exception {
        //调用spring的销毁机制
        super.destroy();
        //清空引用接口名-引用对象缓存
        this.referenceBeanCache.clear();
        //清空引用接口名-引用对象反射器缓存
        this.localReferenceBeanInvocationHandlerCache.clear();
        //清空自动注入的属性-引用对象反射器缓存
        this.injectedFieldReferenceBeanCache.clear();
        //清空自动注入的方法-引用对象反射器缓存
        this.injectedMethodReferenceBeanCache.clear();
    }
}
