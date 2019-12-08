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
package org.apache.dubbo.common.extension;

import com.sun.xml.internal.bind.v2.TODO;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.context.Lifecycle;
import org.apache.dubbo.common.extension.support.ActivateComparator;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOVE_VALUE_PREFIX;

/**
 *
 * dubbo 扩展加载器
 * {@link org.apache.dubbo.rpc.model.ApplicationModel}, {@code DubboBootstrap} and this class are
 * at present designed to be singleton or static (by itself totally static or uses some static fields).
 * So the instances returned from them are of process or classloader scope. If you want to support
 * multiple dubbo servers in a single process, you may need to refactor these three classes.
 *
 * Load dubbo extensions
 * <ul>
 * <li>auto inject dependency extension </li>
 * <li>auto wrap extension in wrapper </li>
 * <li>default extension is an adaptive instance</li>
 * </ul>
 *
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Service%20Provider">Service Provider in Java 5</a>
 * @see org.apache.dubbo.common.extension.SPI
 * @see org.apache.dubbo.common.extension.Adaptive
 * @see org.apache.dubbo.common.extension.Activate
 */
public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String SERVICES_DIRECTORY = "META-INF/services/";

    private static final String DUBBO_DIRECTORY = "META-INF/dubbo/";

    /**
     * TODO dubbo 内置扩展加载器所在路径（在本项目的resource中）
     */
    private static final String DUBBO_INTERNAL_DIRECTORY = DUBBO_DIRECTORY + "internal/";

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    /**
     * 拓展加载器缓存（扩展器接口-扩展器加载器）
     */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    /**
     * 拓展实现类缓存
     */
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    /**
     * 拓展接口
     */
    private final Class<?> type;

    /**
     * 工厂类
     */
    private final ExtensionFactory objectFactory;

    /**
     * 缓存扩展类和类名的关系
     */
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<>();

    /**
     * dubbo扩展实现类缓存
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    /**
     * 缓存自适应实现类类名和Activate注解关系
     */
    private final Map<String, Object> cachedActivates = new ConcurrentHashMap<>();
    /**
     * dubbo扩展实现name和具体的Holder对应关系的缓存
     */
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    /**
     * 自适应扩展类实例缓存
     */
    private final Holder<Object> cachedAdaptiveInstance = new Holder<>();
    private volatile Class<?> cachedAdaptiveClass = null;

    /**
     * dubbo 默认扩展类类名缓存
     */
    private String cachedDefaultName;

    /**
     * 创建自适应实例的异常
     */
    private volatile Throwable createAdaptiveInstanceError;

    /**
     * dubbo扩展类的包装类缓存
     */
    private Set<Class<?>> cachedWrapperClasses;

    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<>();

    private ExtensionLoader(Class<?> type) {
        //扩展接口
        this.type = type;
        //根据扩展类接口，获取对应工厂
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }

    /**
     * 有没有SPI注解
     * @param type
     * @param <T>
     * @return
     */
    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    /**
     * 根据扩展类接口，获取dubbo扩展类实现
     * @param type
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        //扩展类接口不能是null
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        //扩展类接口必须是个接口
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an interface!");
        }
        //是不是带SPI注解
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type (" + type +
                    ") is not an extension, because it is NOT annotated with @" + SPI.class.getSimpleName() + "!");
        }

        //根据扩展器接口获取扩展器加载器
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        //要是没有加载器
        if (loader == null) {
            //生成新的扩展器实现类，放进缓存里
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            //从缓存中再次拿取
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

    // For testing purposes only
    public static void resetExtensionLoader(Class type) {
        ExtensionLoader loader = EXTENSION_LOADERS.get(type);
        if (loader != null) {
            // Remove all instances associated with this loader as well
            Map<String, Class<?>> classes = loader.getExtensionClasses();
            for (Map.Entry<String, Class<?>> entry : classes.entrySet()) {
                EXTENSION_INSTANCES.remove(entry.getValue());
            }
            classes.clear();
            EXTENSION_LOADERS.remove(type);
        }
    }

    public static void destroyAll() {
        EXTENSION_INSTANCES.forEach((_type, instance) -> {
            if (instance instanceof Lifecycle) {
                Lifecycle lifecycle = (Lifecycle) instance;
                try {
                    lifecycle.destroy();
                } catch (Exception e) {
                    logger.error("Error destroying extension " + lifecycle, e);
                }
            }
        });
    }

    /**
     * 获取合适的类加载器
     * @return
     */
    private static ClassLoader findClassLoader() {
        //获取合适的类加载器
        return ClassUtils.getClassLoader(ExtensionLoader.class);
    }

    public String getExtensionName(T extensionInstance) {
        return getExtensionName(extensionInstance.getClass());
    }

    public String getExtensionName(Class<?> extensionClass) {
        getExtensionClasses();// load class
        return cachedNames.get(extensionClass);
    }

    /**
     * This is equivalent to {@code getActivateExtension(url, key, null)}
     *
     * @param url url
     * @param key url parameter key which used to get extension point names
     * @return extension list which are activated.
     * @see #getActivateExtension(org.apache.dubbo.common.URL, String, String)
     */
    public List<T> getActivateExtension(URL url, String key) {
        return getActivateExtension(url, key, null);
    }

    /**
     * 获取已经激活dubbo扩展类集合
     * This is equivalent to {@code getActivateExtension(url, values, null)}
     *
     * @param url    url
     * @param values extension point names
     * @return extension list which are activated
     * @see #getActivateExtension(org.apache.dubbo.common.URL, String[], String)
     */
    public List<T> getActivateExtension(URL url, String[] values) {
        //获取已经激活dubbo扩展类集合
        return getActivateExtension(url, values, null);
    }

    /**
     * 获取已经激活dubbo扩展类集合
     * This is equivalent to {@code getActivateExtension(url, url.getParameter(key).split(","), null)}
     *
     * @param url   url
     * @param key   url parameter key which used to get extension point names
     * @param group group
     * @return extension list which are activated.
     * @see #getActivateExtension(org.apache.dubbo.common.URL, String[], String)
     */
    public List<T> getActivateExtension(URL url, String key, String group) {
        String value = url.getParameter(key);
        //获取已经激活dubbo扩展类集合
        return getActivateExtension(url, StringUtils.isEmpty(value) ? null : COMMA_SPLIT_PATTERN.split(value), group);
    }

    /**
     * 获取已经激活dubbo扩展类集合。
     * Get activate extensions.
     *
     * @param url    url
     * @param values extension point names
     * @param group  group
     * @return extension list which are activated
     * @see org.apache.dubbo.common.extension.Activate
     */
    public List<T> getActivateExtension(URL url, String[] values, String group) {
        //已经激活的dubbo扩展类集合
        List<T> exts = new ArrayList<>();
        List<String> names = values == null ? new ArrayList<>(0) : Arrays.asList(values);
        //如果没有默认扩展配置，才进行操作
        if (!names.contains(REMOVE_VALUE_PREFIX + DEFAULT_KEY)) {
            //获取dubbo扩展实现类集合
            getExtensionClasses();
            //循环处理自适应实现类类名和Activate注解关系
            for (Map.Entry<String, Object> entry : cachedActivates.entrySet()) {
                String name = entry.getKey();
                Object activate = entry.getValue();

                String[] activateGroup, activateValue;

                //如果带有激活扩展条件
                if (activate instanceof Activate) {
                    //激活扩展程序需要的组
                    activateGroup = ((Activate) activate).group();
                    //激活扩展程序需要的URL key
                    activateValue = ((Activate) activate).value();
                //如果带有激活扩展条件（兼容老版本）
                } else if (activate instanceof com.alibaba.dubbo.common.extension.Activate) {
                    //激活扩展程序需要的组
                    activateGroup = ((com.alibaba.dubbo.common.extension.Activate) activate).group();
                    //激活扩展程序需要的URL key
                    activateValue = ((com.alibaba.dubbo.common.extension.Activate) activate).value();
                } else {
                    //跳过
                    continue;
                }
                //能匹配上组信心
                if (isMatchGroup(group, activateGroup)
                        //在扩展类集合中
                        && !names.contains(name)
                        //不是默认的需要移除的扩展类
                        && !names.contains(REMOVE_VALUE_PREFIX + name)
                        //检查激活条件能匹配上URL key
                        && isActive(activateValue, url)) {
                    //添加到已经激活的dubbo扩展类集合
                    exts.add(getExtension(name));
                }
            }
            //进行排序
            exts.sort(ActivateComparator.COMPARATOR);
        }
        List<T> usrs = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            //不是需要移除的扩展类
            if (!name.startsWith(REMOVE_VALUE_PREFIX)
                    && !names.contains(REMOVE_VALUE_PREFIX + name)) {
                //而是默认的扩展类
                if (DEFAULT_KEY.equals(name)) {
                    if (!usrs.isEmpty()) {
                        //将默认扩展作为第一个扩展类
                        exts.addAll(0, usrs);
                        usrs.clear();
                    }
                } else {
                    //TODO 1
                    usrs.add(getExtension(name));
                }
            }
        }
        if (!usrs.isEmpty()) {
            exts.addAll(usrs);
        }
        return exts;
    }

    /**
     * 是否能够匹配上组
     * @param group
     * @param groups
     * @return
     */
    private boolean isMatchGroup(String group, String[] groups) {
        if (StringUtils.isEmpty(group)) {
            return true;
        }
        if (groups != null && groups.length > 0) {
            for (String g : groups) {
                if (group.equals(g)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查激活条件能匹配上URL key
     * @param keys
     * @param url
     * @return
     */
    private boolean isActive(String[] keys, URL url) {
        if (keys.length == 0) {
            return true;
        }
        for (String key : keys) {
            // @Active(value="key1:value1, key2:value2")
            String keyValue = null;
            if (key.contains(":")) {
                //解析键值对
                String[] arr = key.split(":");
                key = arr[0];
                keyValue = arr[1];
            }

            //用解析的键值对和url里的所有参数匹配
            for (Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                //如果匹配到了参数就开启
                if ((k.equals(key) || k.endsWith("." + key))
                        && ((keyValue != null && keyValue.equals(v)) || (keyValue == null && ConfigUtils.isNotEmpty(v)))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get extension's instance. Return <code>null</code> if extension is not found or is not initialized. Pls. note
     * that this method will not trigger extension load.
     * <p>
     * In order to trigger extension load, call {@link #getExtension(String)} instead.
     *
     * @see #getExtension(String)
     */
    @SuppressWarnings("unchecked")
    public T getLoadedExtension(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        Holder<Object> holder = getOrCreateHolder(name);
        return (T) holder.get();
    }

    /**
     * 获取或创建dubbo扩展类的Holder
     * @param name
     * @return
     */
    private Holder<Object> getOrCreateHolder(String name) {
        //从缓存中获取
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            //缓存中没有，创建dubbo扩展类的Holder，并放入缓存（这里的holder是空的！！用的时候要判断）
            cachedInstances.putIfAbsent(name, new Holder<>());
            //再从缓存中获取
            holder = cachedInstances.get(name);
        }
        return holder;
    }

    /**
     * Return the list of extensions which are already loaded.
     * <p>
     * Usually {@link #getSupportedExtensions()} should be called in order to get all extensions.
     *
     * @see #getSupportedExtensions()
     */
    public Set<String> getLoadedExtensions() {
        return Collections.unmodifiableSet(new TreeSet<>(cachedInstances.keySet()));
    }

    public List<T> getLoadedExtensionInstances() {
        List<T> instances = new ArrayList<>();
        cachedInstances.values().forEach(holder -> instances.add((T) holder.get()));
        return instances;
    }

    public Object getLoadedAdaptiveExtensionInstances() {
        return cachedAdaptiveInstance.get();
    }

//    public T getPrioritizedExtensionInstance() {
//        Set<String> supported = getSupportedExtensions();
//
//        Set<T> instances = new HashSet<>();
//        Set<T> prioritized = new HashSet<>();
//        for (String s : supported) {
//
//        }
//
//    }

    /**
     * 用指定的名字获取dubbo扩展类，如果没有找到就抛出异常
     * Find the extension with the given name. If the specified name is not found, then {@link IllegalStateException}
     * will be thrown.
     */
    @SuppressWarnings("unchecked")
    public T getExtension(String name) {
        //检查是否有给定的名字
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        //如果是默认名
        if ("true".equals(name)) {
            //获取默认dubbo扩展类
            return getDefaultExtension();
        }
        //获取或创建dubbo扩展类的Holder
        final Holder<Object> holder = getOrCreateHolder(name);
        //如果Holder里拿不到具体的dubbo扩展类，说明是刚初始化的
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                //创建一个具体的dubbo扩展类，放在holder里
                if (instance == null) {
                    //TODO  2
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * Get the extension by specified name if found, or {@link #getDefaultExtension() returns the default one}
     *
     * @param name the name of extension
     * @return non-null
     */
    public T getOrDefaultExtension(String name) {
        return containsExtension(name)  ? getExtension(name) : getDefaultExtension();
    }

    /**
     //* 获取dubbo 默认扩展类
     * Return default extension, return <code>null</code> if it's not configured.
     */
    public T getDefaultExtension() {
        getExtensionClasses();
        //如果没配置默认扩展类或者默认扩展类是默认值 返回空，
        if (StringUtils.isBlank(cachedDefaultName) || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    /**
     * 根据名字检查类是否存在
     * @param name
     * @return
     */
    public boolean hasExtension(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        Class<?> c = this.getExtensionClass(name);
        return c != null;
    }

    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> clazzes = getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<>(clazzes.keySet()));
    }

    public Set<T> getSupportedExtensionInstances() {
        Set<T> instances = new HashSet<>();
        Set<String> supportedExtensions = getSupportedExtensions();
        if (CollectionUtils.isNotEmpty(supportedExtensions)) {
            for (String name : supportedExtensions) {
                instances.add(getExtension(name));
            }
        }
        return instances;
    }

    /**
     * Return default extension name, return <code>null</code> if not configured.
     */
    public String getDefaultExtensionName() {
        getExtensionClasses();
        return cachedDefaultName;
    }

    /**
     * Register new extension via API
     *
     * @param name  extension name
     * @param clazz extension class
     * @throws IllegalStateException when extension with the same name has already been registered.
     */
    public void addExtension(String name, Class<?> clazz) {
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + " doesn't implement the Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + " can't be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                        name + " already exists (Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
        } else {
            if (cachedAdaptiveClass != null) {
                throw new IllegalStateException("Adaptive Extension already exists (Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
        }
    }

    /**
     * Replace the existing extension via API
     *
     * @param name  extension name
     * @param clazz extension class
     * @throws IllegalStateException when extension to be placed doesn't exist
     * @deprecated not recommended any longer, and use only when test
     */
    @Deprecated
    public void replaceExtension(String name, Class<?> clazz) {
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + " doesn't implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + " can't be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (!cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                        name + " doesn't exist (Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
            cachedInstances.remove(name);
        } else {
            if (cachedAdaptiveClass == null) {
                throw new IllegalStateException("Adaptive Extension doesn't exist (Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
            cachedAdaptiveInstance.set(null);
        }
    }

    /**
     * 获取自适应的扩展类
     * @return
     */
    @SuppressWarnings("unchecked")
    public T getAdaptiveExtension() {
        //从自适应扩展类实例缓存里获取
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) {
            //如果创建扩展类实例缓存发生了异常就抛出
            if (createAdaptiveInstanceError != null) {
                throw new IllegalStateException("Failed to create adaptive instance: " +
                        createAdaptiveInstanceError.toString(),
                        createAdaptiveInstanceError);
            }

            //锁住自适应扩展类实例缓存，开始创建自适应扩展类实例
            synchronized (cachedAdaptiveInstance) {
                instance = cachedAdaptiveInstance.get();
                if (instance == null) {
                    try {
                        //生成代码并编译成class
                        instance = createAdaptiveExtension();
                        //缓存自适应扩展类实例
                        cachedAdaptiveInstance.set(instance);
                    } catch (Throwable t) {
                        //记录编译扩展类实例时发生的异常
                        createAdaptiveInstanceError = t;
                        throw new IllegalStateException("Failed to create adaptive instance: " + t.toString(), t);
                    }
                }
            }
        }

        return (T) instance;
    }

    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);


        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }

            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(StringUtils.toString(entry.getValue()));
        }
        return new IllegalStateException(buf.toString());
    }

    /**
     * 创建一个具体的dubbo扩展类
     * @param name
     * @return
     */
    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        //从所有的dubbo扩展类缓存里获取
        Class<?> clazz = getExtensionClasses().get(name);
        //如果找不到对应dubbo扩展类就要抛出异常
        if (clazz == null) {
            throw findException(name);
        }
        try {
            //获取具体dubbo扩展类的实现
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                //如果还没有具体的实现类，就创建一个新实例并放入缓存
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                //再获取具体dubbo扩展类的实现
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            //查看有没有对象工厂，如果有就用工厂创建新的对象，用来填充自动注入属性
            injectExtension(instance);
            //检查dubbo扩展类包装类缓存
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (CollectionUtils.isNotEmpty(wrapperClasses)) {
                for (Class<?> wrapperClass : wrapperClasses) {
                    //匹配所有的类，确定这是不是一个dubbo扩展类的包装类，构造器异常会忽略
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            //检查是不是需要初始化dubbo扩展类
            initExtension(instance);
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance (name: " + name + ", class: " +
                    type + ") couldn't be instantiated: " + t.getMessage(), t);
        }
    }

    private boolean containsExtension(String name) {
        return getExtensionClasses().containsKey(name);
    }

    /**
     * 根据指定的实例选择工厂来创建对象，如果没有工厂就使用指定的实例,并进行自动注入
     * @param instance
     * @return
     */
    private T injectExtension(T instance) {
        //检查dubbo自适应扩展类有没有工厂
        if (objectFactory == null) {
            return instance;
        }

        try {
            //遍历dubbo自适应扩展类所有方法
            for (Method method : instance.getClass().getMethods()) {
                //不是set方法就跳过
                if (!isSetter(method)) {
                    continue;
                }
                /**
                 * 检查有没有禁止注入
                 * Check {@link DisableInject} to see if we need auto injection for this property
                 */
                if (method.getAnnotation(DisableInject.class) != null) {
                    //不用注入，跳过
                    continue;
                }

                //看set方法的参数类型
                Class<?> pt = method.getParameterTypes()[0];

                //如果是原始类型就跳过
                if (ReflectUtils.isPrimitives(pt)) {
                    continue;
                }

                try {
                    //从set方法的方法名里解析属性名
                    String property = getSetterProperty(method);
                    //获取自适应的扩展类
                    Object object = objectFactory.getExtension(pt, property);
                    if (object != null) {
                        method.invoke(instance, object);
                    }
                } catch (Exception e) {
                    logger.error("Failed to inject via method " + method.getName()
                            + " of interface " + type.getName() + ": " + e.getMessage(), e);
                }

            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return instance;
    }

    /**
     * 检查是不是需要初始化dubbo扩展类
     * @param instance
     */
    private void initExtension(T instance) {
        //如果是初始化组件
        if (instance instanceof Lifecycle) {
            //初始化dubbo扩展类
            Lifecycle lifecycle = (Lifecycle) instance;
            lifecycle.initialize();
        }
    }

    /**
     * 从set方法的方法名里解析属性名
     * get properties name for setter, for instance: setVersion, return "version"
     * <p>
     * return "", if setter name with length less than 3
     */
    private String getSetterProperty(Method method) {
        return method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";
    }

    /**
     * return true if and only if:
     * <p>
     * 1, public
     * <p>
     * 2, name starts with "set"
     * <p>
     * 3, only has one parameter
     */
    private boolean isSetter(Method method) {
        return method.getName().startsWith("set")
                && method.getParameterTypes().length == 1
                && Modifier.isPublic(method.getModifiers());
    }

    private Class<?> getExtensionClass(String name) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Extension name == null");
        }
        return getExtensionClasses().get(name);
    }

    /**
     * 获取dubbo扩展实现类集合
     * @return
     */
    private Map<String, Class<?>> getExtensionClasses() {
        //从缓存中获取
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            //没有缓存就加锁，然后初始化
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    //初始化加载dubbo扩展类
                    classes = loadExtensionClasses();
                    //缓存dubbo扩展类
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 初始化加载dubbo扩展类
     * synchronized in getExtensionClasses
     * */
    private Map<String, Class<?>> loadExtensionClasses() {
        //取出默认的扩展名
        cacheDefaultExtensionName();

        Map<String, Class<?>> extensionClasses = new HashMap<>();
        //从内部的配置文件加载扩展类
        // internal extension load from ExtensionLoader's ClassLoader first
        loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY, type.getName(), true);
        loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"), true);

        loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName());
        loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        loadDirectory(extensionClasses, SERVICES_DIRECTORY, type.getName());
        loadDirectory(extensionClasses, SERVICES_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        return extensionClasses;
    }

    /**
     * 如果存在，则提取和缓存默认扩展名
     * extract and cache default extension name if exists
     */
    private void cacheDefaultExtensionName() {
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation == null) {
            return;
        }

        //获取默认的扩展名
        String value = defaultAnnotation.value();
        if ((value = value.trim()).length() > 0) {
            //默认的扩展名不能有多个，有的话就抛出异常
            String[] names = NAME_SEPARATOR.split(value);
            if (names.length > 1) {
                throw new IllegalStateException("More than 1 default extension name on extension " + type.getName()
                        + ": " + Arrays.toString(names));
            }
            if (names.length == 1) {
                cachedDefaultName = names[0];
            }
        }
    }

    private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir, String type) {
        loadDirectory(extensionClasses, dir, type, false);
    }

    /**
     *
     * @param extensionClasses
     * @param dir
     * @param type
     * @param extensionLoaderClassLoaderFirst 是否优先从扩展类ClassLoader里加载
     */
    private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir, String type, boolean extensionLoaderClassLoaderFirst) {
        //用文件夹名和类型拼接处完整的文件名
        String fileName = dir + type;
        try {
            Enumeration<java.net.URL> urls = null;
            //获取可用的类加载器
            ClassLoader classLoader = findClassLoader();

            // 是否优先从扩展类ClassLoader里加载
            // try to load from ExtensionLoader's ClassLoader first
            if (extensionLoaderClassLoaderFirst) {
                //使用dubbo 扩展类加载时的类加载器进行加载
                ClassLoader extensionLoaderClassLoader = ExtensionLoader.class.getClassLoader();
                //并且他不能是系统类加载器
                if (ClassLoader.getSystemClassLoader() != extensionLoaderClassLoader) {
                    //获取指定的资源
                    urls = extensionLoaderClassLoader.getResources(fileName);
                }
            }
            //如果没加载到扩展类或者还没开始加载
            if(urls == null || !urls.hasMoreElements()) {
                //就用获取的可用的类加载器里加载
                if (classLoader != null) {
                    //使用指定类加载器获取资源
                    urls = classLoader.getResources(fileName);
                //没有获取到可用的类加载器就用系统类加载器（没用，获取可用的类加载器的时候就考虑了最终使用系统类加载器返回）
                } else {
                    urls = ClassLoader.getSystemResources(fileName);
                }
            }

            //如果有扩展类
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL resourceURL = urls.nextElement();
                    //根据资源去加载扩展类
                    loadResource(extensionClasses, classLoader, resourceURL);
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading extension class (interface: " +
                    type + ", description file: " + fileName + ").", t);
        }
    }

    /**
     * 根据资源去加载扩展类
     * @param extensionClasses
     * @param classLoader
     * @param resourceURL
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, java.net.URL resourceURL) {
        try {
            //读取资源流
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceURL.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    //如果是注释就跳过去
                    final int ci = line.indexOf('#');
                    if (ci >= 0) {
                        line = line.substring(0, ci);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        try {
                            String name = null;
                            //用=来分割键值对
                            int i = line.indexOf('=');
                            if (i > 0) {
                                name = line.substring(0, i).trim();
                                line = line.substring(i + 1).trim();
                            }
                            if (line.length() > 0) {
                                //将扩展类加载到扩展类集合中
                                loadClass(extensionClasses, resourceURL, Class.forName(line, true, classLoader), name);
                            }
                        } catch (Throwable t) {
                            IllegalStateException e = new IllegalStateException("Failed to load extension class (interface: " + type + ", class line: " + line + ") in " + resourceURL + ", cause: " + t.getMessage(), t);
                            exceptions.put(line, e);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading extension class (interface: " +
                    type + ", class file: " + resourceURL + ") in " + resourceURL, t);
        }
    }

    /**
     * 将扩展类加载到扩展类集合中
     * @param extensionClasses
     * @param resourceURL
     * @param clazz
     * @param name
     * @throws NoSuchMethodException
     */
    private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, Class<?> clazz, String name) throws NoSuchMethodException {
        //如果扩展接口和指定类型没有继承和实现关系，则应该抛出异常
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error occurred when loading extension class (interface: " +
                    type + ", class line: " + clazz.getName() + "), class "
                    + clazz.getName() + " is not subtype of interface.");
        }
        //如果有Adaptive注解
        if (clazz.isAnnotationPresent(Adaptive.class)) {
            //缓存自适应实现类
            cacheAdaptiveClass(clazz);
        //如果是指定类型的包装类
        } else if (isWrapperClass(clazz)) {
            //缓存扩展类的包装类
            cacheWrapperClass(clazz);
        } else {
            //获取无参构造器
            clazz.getConstructor();
            //如果配置文件里没有指定扩展类的名字
            if (StringUtils.isEmpty(name)) {
                //获取扩展器名
                name = findAnnotationName(clazz);
                //找不到扩展器名，无法注册
                if (name.length() == 0) {
                    throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
                }
            }

            String[] names = NAME_SEPARATOR.split(name);
            if (ArrayUtils.isNotEmpty(names)) {
                //T缓存自适应实现类类名和Activate注解关系
                cacheActivateClass(clazz, names[0]);
                for (String n : names) {
                    //缓存扩展类和类名的关系
                    cacheName(clazz, n);
                    //将单个扩展类加载到扩展类集合中
                    saveInExtensionClass(extensionClasses, clazz, n);
                }
            }
        }
    }

    /**
     * 缓存扩展类和类名的关系
     * cache name
     */
    private void cacheName(Class<?> clazz, String name) {
        //查看有没有缓存扩展类和类名的关系，并在没有缓存时放置
        if (!cachedNames.containsKey(clazz)) {
            cachedNames.put(clazz, name);
        }
    }

    /**
     * 将单个扩展类加载到扩展类集合中
     * put clazz in extensionClasses
     */
    private void saveInExtensionClass(Map<String, Class<?>> extensionClasses, Class<?> clazz, String name) {
        Class<?> c = extensionClasses.get(name);
        if (c == null) {
            //将单个扩展类加载到扩展类集合中
            extensionClasses.put(name, clazz);
        //如果已经有缓存，检查类型并在类型不同时抛出异常
        } else if (c != clazz) {
            String duplicateMsg = "Duplicate extension " + type.getName() + " name " + name + " on " + c.getName() + " and " + clazz.getName();
            logger.error(duplicateMsg);
            throw new IllegalStateException(duplicateMsg);
        }
    }

    /**
     * 缓存自适应实现类类名和Activate注解关系
     * cache Activate class which is annotated with <code>Activate</code>
     * <p>
     * for compatibility, also cache class with old alibaba Activate annotation
     */
    private void cacheActivateClass(Class<?> clazz, String name) {
        Activate activate = clazz.getAnnotation(Activate.class);
        if (activate != null) {
            //缓存自适应实现类类名和Activate注解关系
            cachedActivates.put(name, activate);
        } else {
            //缓存自适应实现类类名和Activate注解关系 兼容老的注解
            // support com.alibaba.dubbo.common.extension.Activate
            com.alibaba.dubbo.common.extension.Activate oldActivate = clazz.getAnnotation(com.alibaba.dubbo.common.extension.Activate.class);
            if (oldActivate != null) {
                cachedActivates.put(name, oldActivate);
            }
        }
    }

    /**
     * 缓存自适应实现类
     * cache Adaptive class which is annotated with <code>Adaptive</code>
     */
    private void cacheAdaptiveClass(Class<?> clazz) {
        //如果没有缓存就缓存
        if (cachedAdaptiveClass == null) {
            cachedAdaptiveClass = clazz;
        //如果已经有缓存，检查是否是同一类型，如果类型不同则需要抛出异常
        } else if (!cachedAdaptiveClass.equals(clazz)) {
            throw new IllegalStateException("More than 1 adaptive class found: "
                    + cachedAdaptiveClass.getName()
                    + ", " + clazz.getName());
        }
    }

    /**
     * 缓存扩展类的包装类
     * cache wrapper class
     * <p>
     * like: ProtocolFilterWrapper, ProtocolListenerWrapper
     */
    private void cacheWrapperClass(Class<?> clazz) {
        //如果没有缓存容器就创建一个
        if (cachedWrapperClasses == null) {
            cachedWrapperClasses = new ConcurrentHashSet<>();
        }
        //缓存扩展类的包装类
        cachedWrapperClasses.add(clazz);
    }

    /**
     * 测试clazz是否是包装类
     * test if clazz is a wrapper class
     * <p>
     * 其唯一参数是给定类类型的构造函数
     * which has Constructor with given class type as its only argument
     */
    private boolean isWrapperClass(Class<?> clazz) {
        try {
            //能否用指定类型使用构造器
            clazz.getConstructor(type);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private String findAnnotationName(Class<?> clazz) {
        //优先使用Extension注解的value作为类名
        org.apache.dubbo.common.Extension extension = clazz.getAnnotation(org.apache.dubbo.common.Extension.class);
        if (extension != null) {
            return extension.value();
        }

        //其次使用类的类名-接口名
        String name = clazz.getSimpleName();
        if (name.endsWith(type.getSimpleName())) {
            name = name.substring(0, name.length() - type.getSimpleName().length());
        }
        return name.toLowerCase();
    }

    /**
     * 生成代码并编译成class
     * @return
     */
    @SuppressWarnings("unchecked")
    private T createAdaptiveExtension() {
        try {
            //生成代码并编译成class，使用无参构造创建一个实例
            //如果有指定工厂，那就用工厂进行对象创建，并进行依赖注入
            return injectExtension((T) getAdaptiveExtensionClass().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("Can't create adaptive extension " + type + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * 生成代码并编译成class
     * @return
     */
    private Class<?> getAdaptiveExtensionClass() {
        //获取dubbo扩展实现类集合
        getExtensionClasses();

        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        //生成代码并编译成class，进行缓存
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    /**
     * 生成代码并编译成class
     * @return
     */
    private Class<?> createAdaptiveExtensionClass() {
        //生成并返回类代码
        String code = new AdaptiveClassCodeGenerator(type, cachedDefaultName).generate();
        //获取合适的类加载器
        ClassLoader classLoader = findClassLoader();
        //编译器
        org.apache.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
        //进行编译
        return compiler.compile(code, classLoader);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

}
