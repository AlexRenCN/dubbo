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
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INVOKER_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;

/**
 * 抽象接口配置类
 * AbstractDefaultConfig
 *
 * @export
 */
public abstract class AbstractInterfaceConfig extends AbstractMethodConfig {

    private static final long serialVersionUID = -1559314110797223229L;

    /**
     * 服务接口的本地实现类名
     * Local impl class name for the service interface
     */
    protected String local;

    /**
     * 服务接口的本地父类名
     * Local stub class name for the service interface
     */
    protected String stub;

    /**
     * 服务监控器
     * Service monitor
     */
    protected MonitorConfig monitor;

    /**
     * 生成动态代理的策略，现在有两种策略可以选择：jdk 或者 javassist
     * Strategies for generating dynamic agents，there are two strategies can be choosed: jdk and javassist
     */
    protected String proxy;

    /**
     * 群集类型
     * Cluster type
     */
    protected String cluster;

    /**
     * 当服务提供者公开服务或服务使用者使用远程服务时使用的过滤器，
     * The {@code Filter} when the provider side exposed a service or the customer side references a remote service used,
     * 如果有多个，可以用逗号分隔
     * if there are more than one, you can use commas to separate them
     */
    protected String filter;

    /**
     * 当服务提供者公开服务或服务使用者使用远程服务时使用的监听器，
     * The Listener when the provider side exposes a service or the customer side references a remote service used
     * 如果有多个，可以用逗号分隔
     * if there are more than one, you can use commas to separate them
     */
    protected String listener;

    /**
     * 服务所属者
     * The owner of the service providers
     */
    protected String owner;

    /**
     * 连接限制，0表示共享连接，否则它指定当前服务的连接
     * Connection limits, 0 means shared connection, otherwise it defines the connections delegated to the current service
     */
    protected Integer connections;

    /**
     * 服务提供者层
     * The layer of service providers
     */
    protected String layer;

    /**
     * 应用信息
     * The application info
     */
    protected ApplicationConfig application;

    /**
     * 模块信息
     * The module info
     */
    protected ModuleConfig module;

    /**
     * 注册中心
     * Registry centers
     */
    protected List<RegistryConfig> registries;

    /**
     * 方法配置
     * The method configuration
     */
    private List<MethodConfig> methods;

    /**
     * 注册中心id
     */
    protected String registryIds;

    /**
     * 链接事件
     */
    // connection events
    protected String onconnect;

    /**
     * 断开连接事件
     * Disconnection events
     */
    protected String ondisconnect;

    /**
     * Metrics服务监控中心配置
     * The metrics configuration
     */
    protected MetricsConfig metrics;
    /**
     * 元数据中心配置
     */
    protected MetadataReportConfig metadataReportConfig;

    /**
     * 配置中心配置
     */
    protected ConfigCenterConfig configCenter;

    /**
     * 回调限制
     */
    // callback limits
    private Integer callbacks;
    /**
     * 引用/暴露服务的作用域，如果是本地的，则表示仅在当前JVM中搜索。
     */
    // the scope for referring/exporting a service, if it's local, it means searching in current JVM only.
    private String scope;

    /**
     * 标签
     */
    protected String tag;

    /**
     * 引用服务的url
     * The url of the reference service
     */
    protected final List<URL> urls = new ArrayList<URL>();

    public List<URL> getExportedUrls() {
        return urls;
    }

    public URL toUrl() {
        return urls.isEmpty() ? null : urls.iterator().next();
    }

    public List<URL> toUrls() {
        return urls;
    }

    /**
     * 检查注册表配置是否存在，然后将它转换成RegistryConfig对象
     * Check whether the registry config is exists, and then conversion it to {@link RegistryConfig}
     */
    public void checkRegistry() {
        //转换注册中心配置并登记注册中心
        convertRegistryIdsToRegistries();

        for (RegistryConfig registryConfig : registries) {
            //对每个注册中心进行校验（是否指定了ip地址），如果没有指定则抛出异常
            if (!registryConfig.isValid()) {
                throw new IllegalStateException("No registry config found or it's not a valid config! " +
                        "The registry config is: " + registryConfig);
            }
        }
    }

    /**
     * 添加运行时参数
     * @param map
     */
    public static void appendRuntimeParameters(Map<String, String> map) {
        //协议版本
        map.put(DUBBO_VERSION_KEY, Version.getProtocolVersion());
        //版本
        map.put(RELEASE_KEY, Version.getVersion());
        //时间戳
        map.put(TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            //进程PID
            map.put(PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }
    }

    /**
     * 检查远程服务接口和方法是否符合Dubbo的要求，主要检查，配置文件中的方法配置是不是包含在远程服务的接口中
     * Check whether the remote service interface and the methods meet with Dubbo's requirements.it mainly check, if the
     * methods configured in the configuration file are included in the interface of remote service
     *
     * @param interfaceClass the interface of remote service
     * @param methods        the methods configured
     */
    public void checkInterfaceAndMethods(Class<?> interfaceClass, List<MethodConfig> methods) {
        //需要检查的接口不能为null
        // interface cannot be null
        Assert.notNull(interfaceClass, new IllegalStateException("interface not allow null!"));

        //interfaceClass必须是一个接口才可以验证
        // to verify interfaceClass is an interface
        if (!interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        // 检查是否存在远程调用方法
        // check if methods exist in the remote service interface
        if (CollectionUtils.isNotEmpty(methods)) {
            for (MethodConfig methodBean : methods) {
                //填充接口信息并刷新
                methodBean.setService(interfaceClass.getName());
                methodBean.setServiceId(this.getId());
                methodBean.refresh();
                //获取方法名，用来检查方法名必填
                String methodName = methodBean.getName();
                if (StringUtils.isEmpty(methodName)) {
                    throw new IllegalStateException("<dubbo:method> name attribute is required! Please check: " +
                            "<dubbo:service interface=\"" + interfaceClass.getName() + "\" ... >" +
                            "<dubbo:method name=\"\" ... /></<dubbo:reference>");
                }

                //检查是否有匹配根据方法名不到的方法，匹配不到抛出异常
                boolean hasMethod = Arrays.stream(interfaceClass.getMethods()).anyMatch(method -> method.getName().equals(methodName));
                if (!hasMethod) {
                    throw new IllegalStateException("The interface " + interfaceClass.getName()
                            + " not found method " + methodName);
                }
            }
        }
    }



    /**
     * 父类的合法性检查，注意：本地实现将不推荐使用，并且通过父类进行替换
     * Legitimacy check of stub, note that: the local will deprecated, and replace with <code>stub</code>
     *
     * @param interfaceClass for provider side, it is the {@link Class} of the service that will be exported; for consumer
     *                       side, it is the {@link Class} of the remote service interface
     */
    public void checkStubAndLocal(Class<?> interfaceClass) {
        //如果有本地实现
        if (ConfigUtils.isNotEmpty(local)) {
            //如果是默认值（true，default），反射调用local获取class，否则使用指定名称反射
            Class<?> localClass = ConfigUtils.isDefault(local) ?
                    ReflectUtils.forName(interfaceClass.getName() + "Local") : ReflectUtils.forName(local);
            //校验本地实现
            verify(interfaceClass, localClass);
        }
        //如果有父类
        if (ConfigUtils.isNotEmpty(stub)) {
            //如果是默认值（true，default），反射调用local获取class，否则使用指定名称反射
            Class<?> localClass = ConfigUtils.isDefault(stub) ?
                    ReflectUtils.forName(interfaceClass.getName() + "Stub") : ReflectUtils.forName(stub);
            //校验父类
            verify(interfaceClass, localClass);
        }
    }

    private void verify(Class<?> interfaceClass, Class<?> localClass) {
        //如果指定的类和接口本身没有继承和实现的关系则抛出异常
        if (!interfaceClass.isAssignableFrom(localClass)) {
            throw new IllegalStateException("The local implementation class " + localClass.getName() +
                    " not implement interface " + interfaceClass.getName());
        }

        try {
            //检查本地的实现类或者父类有没有用指定接口作为参数的构造器
            //Check if the localClass a constructor with parameter who's type is interfaceClass
            ReflectUtils.findConstructor(localClass, interfaceClass);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No such constructor \"public " + localClass.getSimpleName() +
                    "(" + interfaceClass.getName() + ")\" in local implementation class " + localClass.getName());
        }
    }

    private void convertRegistryIdsToRegistries() {
        //处理注册中心的ID
        computeValidRegistryIds();
        if (StringUtils.isEmpty(registryIds)) {
            //如果注册中心ID不存在，也找不到已经注册过的注册中心
            if (CollectionUtils.isEmpty(registries)) {
                //获取默认注册中心
                List<RegistryConfig> registryConfigs = ApplicationModel.getConfigManager().getDefaultRegistries();
                //如果默认注册中心都没有配置，则使用dubbo默认配置中心
                if (registryConfigs.isEmpty()) {
                    registryConfigs = new ArrayList<>();
                    RegistryConfig registryConfig = new RegistryConfig();
                    registryConfig.refresh();
                    registryConfigs.add(registryConfig);
                }
                //将默认的或者dubbo默认的注册中心进行登记
                setRegistries(registryConfigs);
            }
        } else {
            //如果指定了注册中心的ID，就使用分隔符进行配置值的分割，确定指定了几个注册中心
            String[] ids = COMMA_SPLIT_PATTERN.split(registryIds);
            //在现有的注册中心基础上追加新的注册中心
            List<RegistryConfig> tmpRegistries = CollectionUtils.isNotEmpty(registries) ? registries : new ArrayList<>();
            Arrays.stream(ids).forEach(id -> {
                //如果在已经登记的注册中心里找不到这个id
                if (tmpRegistries.stream().noneMatch(reg -> reg.getId().equals(id))) {
                    //看一下配置管理器里里是否能找到这个注册中心
                    Optional<RegistryConfig> globalRegistry = ApplicationModel.getConfigManager().getRegistry(id);
                    if (globalRegistry.isPresent()) {
                        //如果存在则直接登记
                        tmpRegistries.add(globalRegistry.get());
                    } else {
                        //不存在则创建一个新的实例用来登记
                        RegistryConfig registryConfig = new RegistryConfig();
                        registryConfig.setId(id);
                        registryConfig.refresh();
                        tmpRegistries.add(registryConfig);
                    }
                }
            });
            //如果最终注册中心的数量大于指定的数量，则抛出异常
            if (tmpRegistries.size() > ids.length) {
                throw new IllegalStateException("Too much registries found, the registries assigned to this service " +
                        "are :" + registryIds + ", but got " + tmpRegistries.size() + " registries!");
            }

            //登记所有的注册中心
            setRegistries(tmpRegistries);
        }

    }

    protected void computeValidRegistryIds() {
        //查看注册中心ID，
        if (StringUtils.isEmpty(getRegistryIds())) {
            //如果有应用信息，并且应用信息里指定了注册中心id，则覆盖成应用信息里指定的ID
            if (getApplication() != null && StringUtils.isNotEmpty(getApplication().getRegistryIds())) {
                setRegistryIds(getApplication().getRegistryIds());
            }
        }
    }

    /**
     * @return local
     * @deprecated Replace to <code>getStub()</code>
     */
    @Deprecated
    public String getLocal() {
        return local;
    }

    /**
     * @param local
     * @deprecated Replace to <code>setStub(Boolean)</code>
     */
    @Deprecated
    public void setLocal(Boolean local) {
        if (local == null) {
            setLocal((String) null);
        } else {
            setLocal(local.toString());
        }
    }

    /**
     * @param local
     * @deprecated Replace to <code>setStub(String)</code>
     */
    @Deprecated
    public void setLocal(String local) {
        this.local = local;
    }

    public String getStub() {
        return stub;
    }

    public void setStub(Boolean stub) {
        if (stub == null) {
            setStub((String) null);
        } else {
            setStub(stub.toString());
        }
    }

    public void setStub(String stub) {
        this.stub = stub;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {

        this.proxy = proxy;
    }

    public Integer getConnections() {
        return connections;
    }

    public void setConnections(Integer connections) {
        this.connections = connections;
    }

    @Parameter(key = REFERENCE_FILTER_KEY, append = true)
    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Parameter(key = INVOKER_LISTENER_KEY, append = true)
    public String getListener() {
        return listener;
    }

    public void setListener(String listener) {
        this.listener = listener;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public ApplicationConfig getApplication() {
        if (application != null) {
            return application;
        }
        return ApplicationModel.getConfigManager().getApplicationOrElseThrow();
    }

    @Deprecated
    public void setApplication(ApplicationConfig application) {
        this.application = application;
        if (application != null) {
            ConfigManager configManager = ApplicationModel.getConfigManager();
            configManager.getApplication().orElseGet(() -> {
                configManager.setApplication(application);
                return application;
            });
        }
    }

    public ModuleConfig getModule() {
        if (module != null) {
            return module;
        }
        return ApplicationModel.getConfigManager().getModule().orElse(null);
    }

    @Deprecated
    public void setModule(ModuleConfig module) {
        this.module = module;
        if (module != null) {
            ConfigManager configManager = ApplicationModel.getConfigManager();
            configManager.getModule().orElseGet(() -> {
                configManager.setModule(module);
                return module;
            });
        }
    }

    public RegistryConfig getRegistry() {
        return CollectionUtils.isEmpty(registries) ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = new ArrayList<RegistryConfig>(1);
        registries.add(registry);
        setRegistries(registries);
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @SuppressWarnings({"unchecked"})
    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>) registries;
    }

    @Parameter(excluded = true)
    public String getRegistryIds() {
        return registryIds;
    }

    public void setRegistryIds(String registryIds) {
        this.registryIds = registryIds;
    }


    public List<MethodConfig> getMethods() {
        return methods;
    }

    // ======== Deprecated ========

    @SuppressWarnings("unchecked")
    public void setMethods(List<? extends MethodConfig> methods) {
        this.methods = (List<MethodConfig>) methods;
    }


    public MonitorConfig getMonitor() {
        if (monitor != null) {
            return monitor;
        }
        return ApplicationModel.getConfigManager().getMonitor().orElse(null);
    }

    @Deprecated
    public void setMonitor(String monitor) {
        setMonitor(new MonitorConfig(monitor));
    }

    @Deprecated
    public void setMonitor(MonitorConfig monitor) {
        this.monitor = monitor;
        if (monitor != null) {
            ConfigManager configManager = ApplicationModel.getConfigManager();
            configManager.getMonitor().orElseGet(() -> {
                configManager.setMonitor(monitor);
                return monitor;
            });
        }
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Deprecated
    public ConfigCenterConfig getConfigCenter() {
        if (configCenter != null) {
            return configCenter;
        }
        Collection<ConfigCenterConfig> configCenterConfigs = ApplicationModel.getConfigManager().getConfigCenters();
        if (CollectionUtils.isNotEmpty(configCenterConfigs)) {
            return configCenterConfigs.iterator().next();
        }
        return null;
    }

    @Deprecated
    public void setConfigCenter(ConfigCenterConfig configCenter) {
        this.configCenter = configCenter;
        if (configCenter != null) {
            ConfigManager configManager = ApplicationModel.getConfigManager();
            Collection<ConfigCenterConfig> configs = configManager.getConfigCenters();
            if (CollectionUtils.isEmpty(configs)
                    || configs.stream().noneMatch(existed -> existed.equals(configCenter))) {
                configManager.addConfigCenter(configCenter);
            }
        }
    }

    public Integer getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(Integer callbacks) {
        this.callbacks = callbacks;
    }

    public String getOnconnect() {
        return onconnect;
    }

    public void setOnconnect(String onconnect) {
        this.onconnect = onconnect;
    }

    public String getOndisconnect() {
        return ondisconnect;
    }

    public void setOndisconnect(String ondisconnect) {
        this.ondisconnect = ondisconnect;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Deprecated
    public MetadataReportConfig getMetadataReportConfig() {
        if (metadataReportConfig != null) {
            return metadataReportConfig;
        }
        Collection<MetadataReportConfig> metadataReportConfigs = ApplicationModel.getConfigManager().getMetadataConfigs();
        if (CollectionUtils.isNotEmpty(metadataReportConfigs)) {
            return metadataReportConfigs.iterator().next();
        }
        return null;
    }

    @Deprecated
    public void setMetadataReportConfig(MetadataReportConfig metadataReportConfig) {
        this.metadataReportConfig = metadataReportConfig;
        if (metadataReportConfig != null) {
            ConfigManager configManager = ApplicationModel.getConfigManager();
            Collection<MetadataReportConfig> configs = configManager.getMetadataConfigs();
            if (CollectionUtils.isEmpty(configs)
                    || configs.stream().noneMatch(existed -> existed.equals(metadataReportConfig))) {
                configManager.addMetadataReport(metadataReportConfig);
            }
        }
    }

    @Deprecated
    public MetricsConfig getMetrics() {
        if (metrics != null) {
            return metrics;
        }
        return ApplicationModel.getConfigManager().getMetrics().orElse(null);
    }

    @Deprecated
    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
        if (metrics != null) {
            ConfigManager configManager = ApplicationModel.getConfigManager();
            configManager.getMetrics().orElseGet(() -> {
                configManager.setMetrics(metrics);
                return metrics;
            });
        }
    }

    @Parameter(key = TAG_KEY, useKeyAsProperty = false)
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public SslConfig getSslConfig() {
        return ApplicationModel.getConfigManager().getSsl().orElse(null);
    }
}
