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
package org.apache.dubbo.config.spring.schema;

import com.sun.xml.internal.bind.v2.TODO;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.beans.factory.annotation.DubboConfigAliasPostProcessor;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static com.alibaba.spring.util.BeanRegistrar.registerInfrastructureBean;
import static org.apache.dubbo.common.constants.CommonConstants.HIDE_KEY_PREFIX;

/**
 * 抽象Bean Definition解析器
 * AbstractBeanDefinitionParser
 *
 * @export
 */
public class DubboBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(DubboBeanDefinitionParser.class);
    private static final Pattern GROUP_AND_VERION = Pattern.compile("^[\\-.0-9_a-zA-Z]+(\\:[\\-.0-9_a-zA-Z]+)?$");
    private static final String ONRETURN = "onreturn";
    private static final String ONTHROW = "onthrow";
    private static final String ONINVOKE = "oninvoke";
    private static final String METHOD = "Method";
    private final Class<?> beanClass;
    private final boolean required;

    public DubboBeanDefinitionParser(Class<?> beanClass, boolean required) {
        this.beanClass = beanClass;
        this.required = required;
    }

    /**
     * TODO dubbo配置文件解析到内存的代码
     * @param element
     * @param parserContext
     * @param beanClass
     * @param required
     * @return
     */
    @SuppressWarnings("unchecked")
    private static BeanDefinition parse(Element element, ParserContext parserContext, Class<?> beanClass, boolean required) {
        //创建单个配置的根BeanDefinition，并设置为不需要懒加载
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        //获取配置的ID
        String id = element.getAttribute("id");
        //如果没有指定id并且需要自动生成
        if (StringUtils.isEmpty(id) && required) {
            //开始自动生成ID
            //先通过配置的name来作为前缀
            String generatedBeanName = element.getAttribute("name");
            //如果没有配置的name，尝试看下这个类是不是服务提供者协议配置
            if (StringUtils.isEmpty(generatedBeanName)) {
                //是协议配置就用dubbo做前缀
                if (ProtocolConfig.class.equals(beanClass)) {
                    generatedBeanName = "dubbo";
                } else {
                    //不是协议配置就用interface做前缀
                    generatedBeanName = element.getAttribute("interface");
                }
            }
            //保底使用类的名称作为前缀
            if (StringUtils.isEmpty(generatedBeanName)) {
                generatedBeanName = beanClass.getName();
            }
            id = generatedBeanName;
            //用计数器作为后缀
            int counter = 2;
            //从解析上下文里获取上次注册的次数并向上累加
            while (parserContext.getRegistry().containsBeanDefinition(id)) {
                id = generatedBeanName + (counter++);
            }
        }
        //检查生成的id有没有重复
        if (StringUtils.isNotEmpty(id)) {
            //如果在解析上下文中已经存在就需要抛出异常
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            //注册到解析上下文中
            parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
            //将id保存在根BeanDefinition中
            beanDefinition.getPropertyValues().addPropertyValue("id", id);
        }
        //如果是服务提供者协议配置
        if (ProtocolConfig.class.equals(beanClass)) {
            for (String name : parserContext.getRegistry().getBeanDefinitionNames()) {
                //检查解析出来的BeanDefinition
                BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(name);
                //如果有具体内容
                PropertyValue property = definition.getPropertyValues().getPropertyValue("protocol");
                if (property != null) {
                    Object value = property.getValue();
                    //处理id和name相同的特殊情况
                    if (value instanceof ProtocolConfig && id.equals(((ProtocolConfig) value).getName())) {
                        definition.getPropertyValues().addPropertyValue("protocol", new RuntimeBeanReference(id));
                    }
                }
            }
        //如果是指定的Service类
        } else if (ServiceBean.class.equals(beanClass)) {
            String className = element.getAttribute("class");
            if (StringUtils.isNotEmpty(className)) {
                RootBeanDefinition classDefinition = new RootBeanDefinition();
                classDefinition.setBeanClass(ReflectUtils.forName(className));
                classDefinition.setLazyInit(false);
                //解析name和value
                parseProperties(element.getChildNodes(), classDefinition);
                //记录指定的接口类
                beanDefinition.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, id + "Impl"));
            }
        //服务提供者配置
        } else if (ProviderConfig.class.equals(beanClass)) {
            //解析provider里嵌套的service
            parseNested(element, parserContext, ServiceBean.class, true, "service", "provider", id, beanDefinition);
        //服务消费者配置
        } else if (ConsumerConfig.class.equals(beanClass)) {
            //解析consumer里嵌套的service
            parseNested(element, parserContext, ReferenceBean.class, false, "reference", "consumer", id, beanDefinition);
        }
        Set<String> props = new HashSet<>();
        ManagedMap parameters = null;
        //遍历方法找出所有的set方法
        for (Method setter : beanClass.getMethods()) {
            String name = setter.getName();
            //set开头
            if (name.length() > 3 && name.startsWith("set")
                    //public修饰
                    && Modifier.isPublic(setter.getModifiers())
                    //有参数
                    && setter.getParameterTypes().length == 1) {
                //获取类型，set方法规范里是一个类型，取第一个就够了
                Class<?> type = setter.getParameterTypes()[0];
                //去除"set",修改驼峰命名规范的第一个字母
                String beanProperty = name.substring(3, 4).toLowerCase() + name.substring(4);
                //获取属性值
                String property = StringUtils.camelToSplitName(beanProperty, "-");
                props.add(property);
                //用获取的属性值再检查setter/getter是否能匹配
                // check the setter/getter whether match
                Method getter = null;
                try {
                    //以非boolean类型的get标准检查
                    getter = beanClass.getMethod("get" + name.substring(3), new Class<?>[0]);
                } catch (NoSuchMethodException e) {
                    try {
                        //以boolean类型的get标准检查
                        getter = beanClass.getMethod("is" + name.substring(3), new Class<?>[0]);
                    } catch (NoSuchMethodException e2) {
                        // ignore, there is no need any log here since some class implement the interface: EnvironmentAware,
                        // ApplicationAware, etc. They only have setter method, otherwise will cause the error log during application start up.
                    }
                }
                //如果没有get 或者get方法不是公开的  或者get和set匹配不上类型
                if (getter == null
                        || !Modifier.isPublic(getter.getModifiers())
                        || !type.equals(getter.getReturnType())) {
                    //没有匹配到get方法，直接跳过
                    continue;
                }
                //如果是获取的选项参数
                if ("parameters".equals(property)) {
                    //继续解析子节点，获得所有的选项参数
                    parameters = parseParameters(element.getChildNodes(), beanDefinition);
                //如果是获取的方法集合
                } else if ("methods".equals(property)) {
                    //继续解析子节点，获得所有的方法
                    parseMethods(id, element.getChildNodes(), beanDefinition, parserContext);
                //如果是获取的方法参数
                } else if ("arguments".equals(property)) {
                    //继续解析子节点，获得所有的方法参数
                    parseArguments(id, element.getChildNodes(), beanDefinition, parserContext);
                } else {
                    //普通的单个属性
                    String value = element.getAttribute(property);
                    if (value != null) {
                        value = value.trim();
                        if (value.length() > 0) {
                            //不注册到注册中心 及配置value是N/A
                            if ("registry".equals(property) && RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(value)) {
                                //封装配置中心的配置，记录在类定义beanDefinition中
                                RegistryConfig registryConfig = new RegistryConfig();
                                registryConfig.setAddress(RegistryConfig.NO_AVAILABLE);
                                beanDefinition.getPropertyValues().addPropertyValue(beanProperty, registryConfig);
                            } else if ("provider".equals(property) || "registry".equals(property) || ("protocol".equals(property) && ServiceBean.class.equals(beanClass))) {
                                /**
                                 * 对于'provider' 'protocol' 'registry'
                                 * 保留文本值（应为id/name）并将该值设置为“registryIds”“providerIds”“protocolIds”
                                 * For 'provider' 'protocol' 'registry', keep literal value (should be id/name) and set the value to 'registryIds' 'providerIds' protocolIds'
                                 * 下面的过程应该确保每个id引用相应的实例，下面是如何为不同的用例查找实例：
                                 * The following process should make sure each id refers to the corresponding instance, here's how to find the instance for different use cases:
                                 * 一。Spring，用ID检查现有的bean，查看ServiceBean#afterPropertiesSet()，然后尝试使用ID查找在远程配置中心定义的配置文件。
                                 * 1. Spring, check existing bean by id, see{@link ServiceBean#afterPropertiesSet()}; then try to use id to find configs defined in remote Config Center
                                 * 二。API，直接使用id查找在远程配置中心定义的配置；如果所有配置实例都是在本地定义的
                                 * 2. API, directly use id to find configs defined in remote Config Center; if all config instances are defined locally, please use {@link ServiceConfig#setRegistries(List)}
                                 */
                                //处理多个提供者、注册中心、协议的情况，'provider' 'protocol' 'registry'把key后面添加"Ids"
                                beanDefinition.getPropertyValues().addPropertyValue(beanProperty + "Ids", value);
                            } else {
                                Object reference;
                                //如果是基础类型
                                if (isPrimitive(type)) {
                                    //旧版本xsd中默认值的向后兼容性，将一些配置覆盖成null
                                    if ("async".equals(property) && "false".equals(value)
                                            || "timeout".equals(property) && "0".equals(value)
                                            || "delay".equals(property) && "0".equals(value)
                                            || "version".equals(property) && "0.0.0".equals(value)
                                            || "stat".equals(property) && "-1".equals(value)
                                            || "reliable".equals(property) && "false".equals(value)) {
                                        // backward compatibility for the default value in old version's xsd
                                        value = null;
                                    }
                                    reference = value;
                                //如果是方法调用、方法返回、方法异常拦截器
                                } else if (ONRETURN.equals(property) || ONTHROW.equals(property) || ONINVOKE.equals(property)) {
                                    int index = value.lastIndexOf(".");
                                    //引用
                                    String ref = value.substring(0, index);
                                    //方法名
                                    String method = value.substring(index + 1);
                                    reference = new RuntimeBeanReference(ref);
                                    beanDefinition.getPropertyValues().addPropertyValue(property + METHOD, method);
                                } else {
                                    //如果是引用的接口，检查service是不是单例的
                                    if ("ref".equals(property) && parserContext.getRegistry().containsBeanDefinition(value)) {
                                        BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                                        //如果不是单例的就要异常
                                        if (!refBean.isSingleton()) {
                                            throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                                        }
                                    }
                                    reference = new RuntimeBeanReference(value);
                                }
                                beanDefinition.getPropertyValues().addPropertyValue(beanProperty, reference);
                            }
                        }
                    }
                }
            }
        }
        //检查所有的子节点，将没有解析到的键值对记录下来
        NamedNodeMap attributes = element.getAttributes();
        int len = attributes.getLength();
        for (int i = 0; i < len; i++) {
            Node node = attributes.item(i);
            String name = node.getLocalName();
            if (!props.contains(name)) {
                if (parameters == null) {
                    parameters = new ManagedMap();
                }
                String value = node.getNodeValue();
                parameters.put(name, new TypedStringValue(value, String.class));
            }
        }
        if (parameters != null) {
            //作为普通的参数记录在类定义beanDefinition中
            beanDefinition.getPropertyValues().addPropertyValue("parameters", parameters);
        }
        return beanDefinition;
    }

    /**
     * 是不是基础类型
     * @param cls
     * @return
     */
    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == Boolean.class || cls == Byte.class
                || cls == Character.class || cls == Short.class || cls == Integer.class
                || cls == Long.class || cls == Float.class || cls == Double.class
                || cls == String.class || cls == Date.class || cls == Class.class;
    }

    /**
     * 解析嵌套关系
     * @param element
     * @param parserContext
     * @param beanClass
     * @param required
     * @param tag
     * @param property
     * @param ref
     * @param beanDefinition
     */
    private static void parseNested(Element element, ParserContext parserContext, Class<?> beanClass, boolean required, String tag, String property, String ref, BeanDefinition beanDefinition) {
        NodeList nodeList = element.getChildNodes();
        if (nodeList == null) {
            return;
        }
        boolean first = true;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            if (tag.equals(node.getNodeName())
                    || tag.equals(node.getLocalName())) {
                if (first) {
                    first = false;
                    String isDefault = element.getAttribute("default");
                    if (StringUtils.isEmpty(isDefault)) {
                        beanDefinition.getPropertyValues().addPropertyValue("default", "false");
                    }
                }
                BeanDefinition subDefinition = parse((Element) node, parserContext, beanClass, required);
                if (subDefinition != null && StringUtils.isNotEmpty(ref)) {
                    subDefinition.getPropertyValues().addPropertyValue(property, new RuntimeBeanReference(ref));
                }
            }
        }
    }

    /**
     * 解析xml里的service标签
     * @param nodeList
     * @param beanDefinition
     */
    private static void parseProperties(NodeList nodeList, RootBeanDefinition beanDefinition) {
        if (nodeList == null) {
            return;
        }
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (!(nodeList.item(i) instanceof Element)) {
                continue;
            }
            Element element = (Element) nodeList.item(i);
            if ("property".equals(element.getNodeName())
                    || "property".equals(element.getLocalName())) {
                String name = element.getAttribute("name");
                if (StringUtils.isNotEmpty(name)) {
                    String value = element.getAttribute("value");
                    String ref = element.getAttribute("ref");
                    if (StringUtils.isNotEmpty(value)) {
                        beanDefinition.getPropertyValues().addPropertyValue(name, value);
                    } else if (StringUtils.isNotEmpty(ref)) {
                        beanDefinition.getPropertyValues().addPropertyValue(name, new RuntimeBeanReference(ref));
                    } else {
                        throw new UnsupportedOperationException("Unsupported <property name=\"" + name + "\"> sub tag, Only supported <property name=\"" + name + "\" ref=\"...\" /> or <property name=\"" + name + "\" value=\"...\" />");
                    }
                }
            }
        }
    }

    /**
     * 解析选项参数集合，获取所有的选项参数
     * @param nodeList
     * @param beanDefinition
     * @return
     */
    @SuppressWarnings("unchecked")
    private static ManagedMap parseParameters(NodeList nodeList, RootBeanDefinition beanDefinition) {
        if (nodeList == null) {
            return null;
        }
        ManagedMap parameters = null;
        //循环处理子节点
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (!(nodeList.item(i) instanceof Element)) {
                continue;
            }
            Element element = (Element) nodeList.item(i);
            if ("parameter".equals(element.getNodeName())
                    || "parameter".equals(element.getLocalName())) {
                if (parameters == null) {
                    parameters = new ManagedMap();
                }
                //记录每个子节点的键值
                String key = element.getAttribute("key");
                String value = element.getAttribute("value");
                boolean hide = "true".equals(element.getAttribute("hide"));
                if (hide) {
                    key = HIDE_KEY_PREFIX + key;
                }
                //记录每个选项参数
                parameters.put(key, new TypedStringValue(value, String.class));
            }
        }
        return parameters;
    }

    /**
     * 解析方法集合，获取所有的方法
     * @param id
     * @param nodeList
     * @param beanDefinition
     * @param parserContext
     */
    @SuppressWarnings("unchecked")
    private static void parseMethods(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                     ParserContext parserContext) {
        if (nodeList == null) {
            return;
        }
        ManagedList methods = null;
        //循环处理子节点
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (!(nodeList.item(i) instanceof Element)) {
                continue;
            }
            Element element = (Element) nodeList.item(i);
            if ("method".equals(element.getNodeName()) || "method".equals(element.getLocalName())) {
                String methodName = element.getAttribute("name");
                if (StringUtils.isEmpty(methodName)) {
                    throw new IllegalStateException("<dubbo:method> name attribute == null");
                }
                if (methods == null) {
                    methods = new ManagedList();
                }
                //解析每个方法
                BeanDefinition methodBeanDefinition = parse(element,
                        parserContext, MethodConfig.class, false);
                String name = id + "." + methodName;
                BeanDefinitionHolder methodBeanDefinitionHolder = new BeanDefinitionHolder(
                        methodBeanDefinition, name);
                //记录每个方法
                methods.add(methodBeanDefinitionHolder);
            }
        }
        if (methods != null) {
            beanDefinition.getPropertyValues().addPropertyValue("methods", methods);
        }
    }

    /**
     * 解析方法参数集合，获取所有的参数
     * @param id
     * @param nodeList
     * @param beanDefinition
     * @param parserContext
     */
    @SuppressWarnings("unchecked")
    private static void parseArguments(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                       ParserContext parserContext) {
        if (nodeList == null) {
            return;
        }
        ManagedList arguments = null;
        //循环处理子节点
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (!(nodeList.item(i) instanceof Element)) {
                continue;
            }
            Element element = (Element) nodeList.item(i);
            if ("argument".equals(element.getNodeName()) || "argument".equals(element.getLocalName())) {
                String argumentIndex = element.getAttribute("index");
                if (arguments == null) {
                    arguments = new ManagedList();
                }
                //解析每个方法参数
                BeanDefinition argumentBeanDefinition = parse(element,
                        parserContext, ArgumentConfig.class, false);
                String name = id + "." + argumentIndex;
                BeanDefinitionHolder argumentBeanDefinitionHolder = new BeanDefinitionHolder(
                        argumentBeanDefinition, name);
                //记录每个方法参数
                arguments.add(argumentBeanDefinitionHolder);
            }
        }
        if (arguments != null) {
            beanDefinition.getPropertyValues().addPropertyValue("arguments", arguments);
        }
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        // Register DubboConfigAliasPostProcessor
        registerDubboConfigAliasPostProcessor(parserContext.getRegistry());

        return parse(element, parserContext, beanClass, required);
    }

    /**
     * Register {@link DubboConfigAliasPostProcessor}
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @since 2.7.5 [Feature] https://github.com/apache/dubbo/issues/5093
     */
    private void registerDubboConfigAliasPostProcessor(BeanDefinitionRegistry registry) {
        registerInfrastructureBean(registry, DubboConfigAliasPostProcessor.BEAN_NAME, DubboConfigAliasPostProcessor.class);
    }
}
