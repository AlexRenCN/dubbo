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

import com.sun.xml.internal.bind.v2.TODO;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.spring.context.annotation.DubboConfigBindingRegistrar;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfigBinding;
import org.apache.dubbo.config.spring.context.config.DubboConfigBeanCustomizer;
import org.apache.dubbo.config.spring.context.properties.DefaultDubboConfigBinder;
import org.apache.dubbo.config.spring.context.properties.DubboConfigBinder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

/**
 * dubbo 配置绑定的 bean后置处理器
 * Dubbo Config Binding {@link BeanPostProcessor}
 *
 * @see EnableDubboConfigBinding
 * @see DubboConfigBindingRegistrar
 * @since 2.5.8
 */

public class DubboConfigBindingBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, InitializingBean
        , BeanDefinitionRegistryPostProcessor {

    private final Log log = LogFactory.getLog(getClass());

    /**
     * 配置属性的前缀
     * The prefix of Configuration Properties
     */
    private final String prefix;

    /**
     * 绑定Bean名称
     * Binding Bean Name
     */
    private final String beanName;

    /**
     * dubbo spring配置接入接口
     */
    private DubboConfigBinder dubboConfigBinder;

    /**
     * spring上下文
     */
    private ApplicationContext applicationContext;

    /**
     * 类定义注册器
     */
    private BeanDefinitionRegistry beanDefinitionRegistry;

    /**
     * 是否忽略未知属性
     */
    private boolean ignoreUnknownFields = true;

    /**
     * 是否忽略类型异常
     */
    private boolean ignoreInvalidFields = true;

    private List<DubboConfigBeanCustomizer> configBeanCustomizers = Collections.emptyList();

    /**
     * @param prefix   the prefix of Configuration Properties
     * @param beanName the binding Bean Name
     */
    public DubboConfigBindingBeanPostProcessor(String prefix, String beanName) {
        Assert.notNull(prefix, "The prefix of Configuration Properties must not be null");
        Assert.notNull(beanName, "The name of bean must not be null");
        this.prefix = prefix;
        this.beanName = beanName;
    }

    /**
     * dubbo config 配置绑定，spring容器初始化bean之前的处理器
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        //如果是在spring中注册AbstractConfig
        if (this.beanName.equals(beanName) && bean instanceof AbstractConfig) {

            AbstractConfig dubboConfig = (AbstractConfig) bean;

            //用指定前缀绑定Dubbo Config对象
            bind(prefix, dubboConfig);

            //设置spring容器里 AbstractConfig bean对象属性
            customize(beanName, dubboConfig);

        }

        return bean;

    }

    /**
     * 用指定前缀绑定Dubbo Config对象
     * @param prefix
     * @param dubboConfig
     */
    private void bind(String prefix, AbstractConfig dubboConfig) {

        //用指定前缀绑定Dubbo Config对象。
        dubboConfigBinder.bind(prefix, dubboConfig);

        if (log.isInfoEnabled()) {
            log.info("The properties of bean [name : " + beanName + "] have been binding by prefix of " +
                    "configuration properties : " + prefix);
        }
    }

    /**
     * 设置spring容器里 AbstractConfig bean对象属性
     * @param beanName
     * @param dubboConfig
     */
    private void customize(String beanName, AbstractConfig dubboConfig) {

        for (DubboConfigBeanCustomizer customizer : configBeanCustomizers) {
            customizer.customize(beanName, dubboConfig);
        }

    }

    public boolean isIgnoreUnknownFields() {
        return ignoreUnknownFields;
    }

    public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
        this.ignoreUnknownFields = ignoreUnknownFields;
    }

    public boolean isIgnoreInvalidFields() {
        return ignoreInvalidFields;
    }

    public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
        this.ignoreInvalidFields = ignoreInvalidFields;
    }

    public DubboConfigBinder getDubboConfigBinder() {
        return dubboConfigBinder;
    }

    public void setDubboConfigBinder(DubboConfigBinder dubboConfigBinder) {
        this.dubboConfigBinder = dubboConfigBinder;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AbstractConfig) {
            String id = ((AbstractConfig) bean).getId();
            if (beanDefinitionRegistry != null && beanDefinitionRegistry instanceof DefaultListableBeanFactory) {
                DefaultListableBeanFactory factory = (DefaultListableBeanFactory) beanDefinitionRegistry;
                if (!StringUtils.isBlank(id) && !factory.hasAlias(beanName, id)) {
                    beanDefinitionRegistry.registerAlias(beanName, id);
                }
            }
        }
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        //初始化dubbo spring配置接入接入
        initDubboConfigBinder();

        //初始化配置加载顺序
        initConfigBeanCustomizers();

    }

    /**
     * 初始化dubbo spring配置接入接入
     */
    private void initDubboConfigBinder() {

        //如果还没有加载dubbo spring配置接入接口
        if (dubboConfigBinder == null) {
            try {
                //从上下文中获取bean
                dubboConfigBinder = applicationContext.getBean(DubboConfigBinder.class);
            } catch (BeansException ignored) {
                if (log.isDebugEnabled()) {
                    log.debug("DubboConfigBinder Bean can't be found in ApplicationContext.");
                }
                //如果找不到dubbo的spring配置接入接口，就使用上下文的默认配置接入接口
                // Use Default implementation
                dubboConfigBinder = createDubboConfigBinder(applicationContext.getEnvironment());
            }
        }

        //是否忽略未知属性
        dubboConfigBinder.setIgnoreUnknownFields(ignoreUnknownFields);
        //是否忽略类型异常
        dubboConfigBinder.setIgnoreInvalidFields(ignoreInvalidFields);

    }

    /**
     * 初始化配置加载顺序，通过实现spring的order接口获得的
     */
    private void initConfigBeanCustomizers() {

        Collection<DubboConfigBeanCustomizer> configBeanCustomizers =
                beansOfTypeIncludingAncestors(applicationContext, DubboConfigBeanCustomizer.class).values();

        this.configBeanCustomizers = new ArrayList<>(configBeanCustomizers);

        AnnotationAwareOrderComparator.sort(this.configBeanCustomizers);
    }

    /**
     * Create {@link DubboConfigBinder} instance.
     *
     * @param environment
     * @return {@link DefaultDubboConfigBinder}
     */
    protected DubboConfigBinder createDubboConfigBinder(Environment environment) {
        DefaultDubboConfigBinder defaultDubboConfigBinder = new DefaultDubboConfigBinder();
        defaultDubboConfigBinder.setEnvironment(environment);
        return defaultDubboConfigBinder;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (this.beanDefinitionRegistry == null) {
            this.beanDefinitionRegistry = registry;
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        //do nothing here
    }
}
