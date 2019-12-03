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
package org.apache.dubbo.config;

import com.sun.xml.internal.bind.v2.TODO;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.CompositeConfiguration;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.MethodUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.context.ConfigConfigurationAdapter;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.utils.ReflectUtils.findMethodByMethodSignature;

/**
 * 抽象配置类,基本上所有的配置类都直接或间接继承自该类（ArgumentConfig例外）
 * Utility methods and public methods for parsing configuration
 * 提供配置解析与校验相关的工具方法
 *
 * @export
 */
public abstract class AbstractConfig implements Serializable {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractConfig.class);
    private static final long serialVersionUID = 4267533505537413570L;

    /**
     * 旧属性map
     * The legacy properties container
     */
    private static final Map<String, String> LEGACY_PROPERTIES = new HashMap<String, String>();

    /**
     * 后缀map
     * The suffix container
     */
    private static final String[] SUFFIXES = new String[]{"Config", "Bean", "ConfigBase"};

    static {
        LEGACY_PROPERTIES.put("dubbo.protocol.name", "dubbo.service.protocol");
        LEGACY_PROPERTIES.put("dubbo.protocol.host", "dubbo.service.server.host");
        LEGACY_PROPERTIES.put("dubbo.protocol.port", "dubbo.service.server.port");
        LEGACY_PROPERTIES.put("dubbo.protocol.threads", "dubbo.service.max.thread.pool.size");
        LEGACY_PROPERTIES.put("dubbo.consumer.timeout", "dubbo.service.invoke.timeout");
        LEGACY_PROPERTIES.put("dubbo.consumer.retries", "dubbo.service.max.retry.providers");
        LEGACY_PROPERTIES.put("dubbo.consumer.check", "dubbo.service.allow.no.provider");
        LEGACY_PROPERTIES.put("dubbo.service.url", "dubbo.service.address");
    }

    /**
     * 配置的编号
     * The config id
     */
    protected String id;
    /**
     * 环境
     */
    protected String prefix;

    protected final AtomicBoolean refreshed = new AtomicBoolean(false);

    private static String convertLegacyValue(String key, String value) {
        if (value != null && value.length() > 0) {
            if ("dubbo.service.max.retry.providers".equals(key)) {
                return String.valueOf(Integer.parseInt(value) - 1);
            } else if ("dubbo.service.allow.no.provider".equals(key)) {
                return String.valueOf(!Boolean.parseBoolean(value));
            }
        }
        return value;
    }

    public static String getTagName(Class<?> cls) {
        String tag = cls.getSimpleName();
        for (String suffix : SUFFIXES) {
            if (tag.endsWith(suffix)) {
                tag = tag.substring(0, tag.length() - suffix.length());
                break;
            }
        }
        return StringUtils.camelToSplitName(tag, "-");
    }

    public static void appendParameters(Map<String, String> parameters, Object config) {
        appendParameters(parameters, config, null);
    }

    @SuppressWarnings("unchecked")
    public static void appendParameters(Map<String, String> parameters, Object config, String prefix) {
        if (config == null) {
            return;
        }
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                String name = method.getName();
                //如果是get方法
                if (MethodUtils.isGetter(method)) {
                    //获取Parameter注解参数
                    Parameter parameter = method.getAnnotation(Parameter.class);
                    //如果方法的返回值是Object类型  或者  Parameter注解指定了排除  则跳过
                    if (method.getReturnType() == Object.class || parameter != null && parameter.excluded()) {
                        continue;
                    }
                    String key;
                    //如果有别名，则使用指定的别名
                    if (parameter != null && parameter.key().length() > 0) {
                        key = parameter.key();
                    } else {
                        //没指定别名，从方法名中截取
                        key = calculatePropertyFromGetter(name);
                    }
                    //从配置中获取对应的值
                    Object value = method.invoke(config);
                    String str = String.valueOf(value).trim();
                    //如果得到了配置
                    if (value != null && str.length() > 0) {
                        //如果需要转义
                        if (parameter != null && parameter.escaped()) {
                            //对配置进行转义
                            str = URL.encode(str);
                        }
                        //如果需要拼接默认属性
                        if (parameter != null && parameter.append()) {
                            //追接上指定的key
                            String pre = parameters.get(key);
                            if (pre != null && pre.length() > 0) {
                                str = pre + "," + str;
                            }
                        }
                        //如果有前缀则在指定key前面追加前缀
                        if (prefix != null && prefix.length() > 0) {
                            key = prefix + "." + key;
                        }
                        //保存指定的配置和值
                        parameters.put(key, str);
                    } else if (parameter != null && parameter.required()) {
                        throw new IllegalStateException(config.getClass().getSimpleName() + "." + key + " == null");
                    }
                //如果是getParameters方法
                } else if (isParametersGetter(method)) {
                    //获取所有需要获取的配置
                    Map<String, String> map = (Map<String, String>) method.invoke(config, new Object[0]);
                    //加工后所有配置并且保存
                    parameters.putAll(convert(map, prefix));
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    /**
     * 处理@Parameter attribute属性是true的方法
     * @param parameters
     * @param config
     */
    @Deprecated
    protected static void appendAttributes(Map<String, Object> parameters, Object config) {
        appendAttributes(parameters, config, null);
    }

    /**
     * 处理@Parameter attribute属性是true的方法
     * @param parameters
     * @param config
     * @param prefix
     */
    @Deprecated
    protected static void appendAttributes(Map<String, Object> parameters, Object config, String prefix) {
        if (config == null) {
            return;
        }
        //获取所有方法并遍历
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                //在每个方法上寻找Parameter注解，没有的话则跳过
                Parameter parameter = method.getAnnotation(Parameter.class);
                if (parameter == null || !parameter.attribute()) {
                    continue;
                }
                //获取方法名
                String name = method.getName();
                //判断是不是get方法
                if (MethodUtils.isGetter(method)) {
                    String key;
                    //如果从parameter属性里指定了key，则使用指定的key
                    if (parameter.key().length() > 0) {
                        key = parameter.key();
                    } else {
                        //没有指定key，则通过解析方法名获取属性的名称
                        key = calculateAttributeFromGetter(name);
                    }
                    //获取配置项的值
                    Object value = method.invoke(config);
                    if (value != null) {
                        //如果有配置，则看下需不需要添加前缀，并添加前缀
                        if (prefix != null && prefix.length() > 0) {
                            key = prefix + "." + key;
                        }
                        //保存解析到的配置
                        parameters.put(key, value);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    public static ConsumerModel.AsyncMethodInfo convertMethodConfig2AsyncInfo(MethodConfig methodConfig) {
        if (methodConfig == null || (methodConfig.getOninvoke() == null && methodConfig.getOnreturn() == null && methodConfig.getOnthrow() == null)) {
            return null;
        }

        //check config conflict
        if (Boolean.FALSE.equals(methodConfig.isReturn()) && (methodConfig.getOnreturn() != null || methodConfig.getOnthrow() != null)) {
            throw new IllegalStateException("method config error : return attribute must be set true when onreturn or onthrow has been set.");
        }

        ConsumerModel.AsyncMethodInfo asyncMethodInfo = new ConsumerModel.AsyncMethodInfo();

        asyncMethodInfo.setOninvokeInstance(methodConfig.getOninvoke());
        asyncMethodInfo.setOnreturnInstance(methodConfig.getOnreturn());
        asyncMethodInfo.setOnthrowInstance(methodConfig.getOnthrow());

        try {
            String oninvokeMethod = methodConfig.getOninvokeMethod();
            if (StringUtils.isNotEmpty(oninvokeMethod)) {
                asyncMethodInfo.setOninvokeMethod(getMethodByName(methodConfig.getOninvoke().getClass(), oninvokeMethod));
            }

            String onreturnMethod = methodConfig.getOnreturnMethod();
            if (StringUtils.isNotEmpty(onreturnMethod)) {
                asyncMethodInfo.setOnreturnMethod(getMethodByName(methodConfig.getOnreturn().getClass(), onreturnMethod));
            }

            String onthrowMethod = methodConfig.getOnthrowMethod();
            if (StringUtils.isNotEmpty(onthrowMethod)) {
                asyncMethodInfo.setOnthrowMethod(getMethodByName(methodConfig.getOnthrow().getClass(), onthrowMethod));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        return asyncMethodInfo;
    }

    private static Method getMethodByName(Class<?> clazz, String methodName) {
        try {
            return ReflectUtils.findMethodByMethodName(clazz, methodName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected static Set<String> getSubProperties(Map<String, String> properties, String prefix) {
        return properties.keySet().stream().filter(k -> k.contains(prefix)).map(k -> {
            k = k.substring(prefix.length());
            return k.substring(0, k.indexOf("."));
        }).collect(Collectors.toSet());
    }

    /**
     * 通过set方法解析属性名
     * @param clazz
     * @param setter
     * @return
     * @throws Exception
     */
    private static String extractPropertyName(Class<?> clazz, Method setter) throws Exception {
        //获取属性名
        String propertyName = setter.getName().substring("set".length());
        Method getter = null;
        try {
            //获取get方法
            getter = clazz.getMethod("get" + propertyName);
        } catch (NoSuchMethodException e) {
            //兼容boolean类型的get方法
            getter = clazz.getMethod("is" + propertyName);
        }
        //从get方法上找到Parameter注解
        Parameter parameter = getter.getAnnotation(Parameter.class);
        //如果指定了key，并且用key作为属性
        if (parameter != null && StringUtils.isNotEmpty(parameter.key()) && parameter.useKeyAsProperty()) {
            //使用指定的属性名
            propertyName = parameter.key();
        } else {
            //通过方法解析属性名
            propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
        }
        return propertyName;
    }

    private static String calculatePropertyFromGetter(String name) {
        int i = name.startsWith("get") ? 3 : 2;
        return StringUtils.camelToSplitName(name.substring(i, i + 1).toLowerCase() + name.substring(i + 1), ".");
    }

    private static String calculateAttributeFromGetter(String getter) {
        int i = getter.startsWith("get") ? 3 : 2;
        return getter.substring(i, i + 1).toLowerCase() + getter.substring(i + 1);
    }

    private static void invokeSetParameters(Class c, Object o, Map map) {
        try {
            //反射调用setParameters
            Method method = findMethodByMethodSignature(c, "setParameters", new String[]{Map.class.getName()});
            if (method != null && isParametersSetter(method)) {
                method.invoke(o, map);
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    private static Map<String, String> invokeGetParameters(Class c, Object o) {
        try {
            Method method = findMethodByMethodSignature(c, "getParameters", null);
            if (method != null && isParametersGetter(method)) {
                return (Map<String, String>) method.invoke(o);
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    /**
     * 如果是getParameters方法
     * @param method
     * @return
     */
    private static boolean isParametersGetter(Method method) {
        String name = method.getName();
        return ("getParameters".equals(name)
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterTypes().length == 0
                && method.getReturnType() == Map.class);
    }

    /**
     * 如果是setParameters方法
     * @param method
     * @return
     */
    private static boolean isParametersSetter(Method method) {
        return ("setParameters".equals(method.getName())
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterCount() == 1
                && Map.class == method.getParameterTypes()[0]
                && method.getReturnType() == void.class);
    }

    /**
     * 加工后的所有配置
     * @param parameters
     * @param prefix
     * @return
     */
    private static Map<String, String> convert(Map<String, String> parameters, String prefix) {
        //如果没有需要获取的配置，则直接返回空集合
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();
        //前缀
        String pre = (prefix != null && prefix.length() > 0 ? prefix + "." : "");
        //遍历需要获取的配置key
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            //依次放入配置项和配置的值
            result.put(pre + key, value);
            //为了兼容，把-替换成.
            // For compatibility, key like "registry-type" will has a duplicate key "registry.type"
            if (key.contains("-")) {
                result.put(pre + key.replace('-', '.'), value);
            }
        }
        return result;
    }

    @Parameter(excluded = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void updateIdIfAbsent(String value) {
        if (StringUtils.isNotEmpty(value) && StringUtils.isEmpty(id)) {
            this.id = value;
        }
    }

    protected void appendAnnotation(Class<?> annotationClass, Object annotation) {
        //获取注解所有方法并遍历
        Method[] methods = annotationClass.getMethods();
        for (Method method : methods) {
            //如果这个方法不是Object类重写的方法
            if (method.getDeclaringClass() != Object.class
                    //并且这个方法有返回值
                    && method.getReturnType() != void.class
                    //并且这个方法没有参数
                    && method.getParameterTypes().length == 0
                    //并且这个方法是public方法
                    && Modifier.isPublic(method.getModifiers())
                    //并且这个方法不是静态的
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    //获取注解的方法名
                    String property = method.getName();
                    //如果这个方法是获取接口名的（方法名是interfaceClass或者interfaceName）
                    if ("interfaceClass".equals(property) || "interfaceName".equals(property)) {
                        //修改方法名变量的值为interface
                        property = "interface";
                    }
                    //拼接属性的set方法名
                    String setter = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
                    //反射方法获取配置的值
                    Object value = method.invoke(annotation);
                    //如果指定了配置并且不是使用的默认值
                    if (value != null && !value.equals(method.getDefaultValue())) {
                        //获取get方法的返回类型
                        Class<?> parameterType = ReflectUtils.getBoxedClass(method.getReturnType());
                        //如果返回的是过滤器或者监听器，用String接收返回值
                        if ("filter".equals(property) || "listener".equals(property)) {
                            parameterType = String.class;
                            //并且将多个过滤器或者监听器按","拼接成一个字符串
                            value = StringUtils.join((String[]) value, ",");
                        //如果返回的是属性集合
                        } else if ("parameters".equals(property)) {
                            //按map返回
                            parameterType = Map.class;
                            value = CollectionUtils.toStringMap((String[]) value);
                        }
                        try {
                            //通过反射set方法，将配置注入到对应的实例中
                            Method setterMethod = getClass().getMethod(setter, parameterType);
                            setterMethod.invoke(this, value);
                        } catch (NoSuchMethodException e) {
                            // 忽略set方法缺失
                            // ignore
                        }
                    }
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Should be called after Config was fully initialized.
     * // FIXME: this method should be completely replaced by appendParameters
     *
     * @return
     * @see AbstractConfig#appendParameters(Map, Object, String)
     * <p>
     * Notice! This method should include all properties in the returning map, treat @Parameter differently compared to appendParameters.
     */
    public Map<String, String> getMetaData() {
        Map<String, String> metaData = new HashMap<>();
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            try {
                String name = method.getName();
                if (MethodUtils.isMetaMethod(method)) {
                    String key;
                    Parameter parameter = method.getAnnotation(Parameter.class);
                    if (parameter != null && parameter.key().length() > 0 && parameter.useKeyAsProperty()) {
                        key = parameter.key();
                    } else {
                        key = calculateAttributeFromGetter(name);
                    }
                    // treat url and configuration differently, the value should always present in configuration though it may not need to present in url.
                    //if (method.getReturnType() == Object.class || parameter != null && parameter.excluded()) {
                    if (method.getReturnType() == Object.class) {
                        metaData.put(key, null);
                        continue;
                    }

                    /**
                     * Attributes annotated as deprecated should not override newly added replacement.
                     */
                    if (MethodUtils.isDeprecated(method) && metaData.get(key) != null) {
                        continue;
                    }

                    Object value = method.invoke(this);
                    String str = String.valueOf(value).trim();
                    if (value != null && str.length() > 0) {
                        metaData.put(key, str);
                    } else {
                        metaData.put(key, null);
                    }
                } else if (isParametersGetter(method)) {
                    Map<String, String> map = (Map<String, String>) method.invoke(this, new Object[0]);
                    metaData.putAll(convert(map, ""));
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return metaData;
    }

    @Parameter(excluded = true)
    public String getPrefix() {
        return StringUtils.isNotEmpty(prefix) ? prefix : (CommonConstants.DUBBO + "." + getTagName(this.getClass()));
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void refresh() {
        //获取环境信息
        Environment env = ApplicationModel.getEnvironment();
        try {
            // 获取所有的配置
            CompositeConfiguration compositeConfiguration = env.getConfiguration(getPrefix(), getId());
            Configuration config = new ConfigConfigurationAdapter(this);
            //如果优先使用配置中心的配置
            if (env.isConfigCenterFirst()) {
                //顺序是：系统配置，应用外部配置，外部配置，基础配置，配置文件配置
                // The sequence would be: SystemConfiguration -> AppExternalConfiguration -> ExternalConfiguration -> AbstractConfig -> PropertiesConfiguration
                compositeConfiguration.addConfiguration(4, config);
            } else {
                //顺序是：系统配置，基础配置，应用外部配置，外部配置，配置文件配置
                // The sequence would be: SystemConfiguration -> AbstractConfig -> AppExternalConfiguration -> ExternalConfiguration -> PropertiesConfiguration
                compositeConfiguration.addConfiguration(2, config);
            }

            //循环处理方法，获取重写的值将新的值写入
            // loop methods, get override value and set the new value back to method
            Method[] methods = getClass().getMethods();
            for (Method method : methods) {
                //如果是setter方法
                if (MethodUtils.isSetter(method)) {
                    try {
                        //通过set方法进行解析，获得配置的key
                        String value = StringUtils.trim(compositeConfiguration.getString(extractPropertyName(getClass(), method)));
                        // 调用isTypeMatch（）是为了避免重复和不正确的更新，例如，我们在ReferenceConfig中有两个“setGeneric”方法。
                        // isTypeMatch() is called to avoid duplicate and incorrect update, for example, we have two 'setGeneric' methods in ReferenceConfig.
                        if (StringUtils.isNotEmpty(value) && ClassUtils.isTypeMatch(method.getParameterTypes()[0], value)) {
                            //反射调用方法，保证方法存在
                            method.invoke(this, ClassUtils.convertPrimitive(method.getParameterTypes()[0], value));
                        }
                    } catch (NoSuchMethodException e) {
                        //方法不存在时进行记录
                        logger.info("Failed to override the property " + method.getName() + " in " +
                                this.getClass().getSimpleName() +
                                ", please make sure every property has getter/setter method provided.");
                    }
                //如果是批量set方法
                } else if (isParametersSetter(method)) {
                    //获取所有需要处理的配置key
                    String value = StringUtils.trim(compositeConfiguration.getString(extractPropertyName(getClass(), method)));
                    if (StringUtils.isNotEmpty(value)) {
                        Map<String, String> map = invokeGetParameters(getClass(), this);
                        map = map == null ? new HashMap<>() : map;
                        //将所有的配置key作为键，给定默认值是""
                        map.putAll(convert(StringUtils.parseParameters(value), ""));
                        //通过反射保存
                        invokeSetParameters(getClass(), this, map);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to override ", e);
        }
    }

    @Override
    public String toString() {
        try {
            StringBuilder buf = new StringBuilder();
            buf.append("<dubbo:");
            buf.append(getTagName(getClass()));
            Method[] methods = getClass().getMethods();
            for (Method method : methods) {
                try {
                    if (MethodUtils.isGetter(method)) {
                        String name = method.getName();
                        String key = calculateAttributeFromGetter(name);
                        Object value = method.invoke(this);
                        if (value != null) {
                            buf.append(" ");
                            buf.append(key);
                            buf.append("=\"");
                            buf.append(value);
                            buf.append("\"");
                        }
                    }
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
            buf.append(" />");
            return buf.toString();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            return super.toString();
        }
    }

    /**
     * FIXME check @Parameter(required=true) and any conditions that need to match.
     */
    @Parameter(excluded = true)
    public boolean isValid() {
        return true;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj.getClass().getName().equals(this.getClass().getName()))) {
            return false;
        }

        Method[] methods = this.getClass().getMethods();
        for (Method method1 : methods) {
            if (MethodUtils.isGetter(method1)) {
                Parameter parameter = method1.getAnnotation(Parameter.class);
                if (parameter != null && parameter.excluded()) {
                    continue;
                }
                try {
                    Method method2 = obj.getClass().getMethod(method1.getName(), method1.getParameterTypes());
                    Object value1 = method1.invoke(this, new Object[]{});
                    Object value2 = method2.invoke(obj, new Object[]{});
                    if (!Objects.equals(value1, value2)) {
                        return false;
                    }
                } catch (Exception e) {
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * Add {@link AbstractConfig instance} into {@link ConfigManager}
     * <p>
     * Current method will invoked by Spring or Java EE container automatically, or should be triggered manually.
     *
     * @see ConfigManager#addConfig(AbstractConfig)
     * @since 2.7.5
     */
    @PostConstruct
    public void addIntoConfigManager() {
        ApplicationModel.getConfigManager().addConfig(this);
    }
}
