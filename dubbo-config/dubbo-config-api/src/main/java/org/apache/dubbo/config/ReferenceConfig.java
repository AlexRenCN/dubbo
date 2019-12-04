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
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.event.ReferenceConfigDestroyedEvent;
import org.apache.dubbo.config.event.ReferenceConfigInitializedEvent;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.event.Event;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.ClusterUtils;
import org.apache.dubbo.rpc.cluster.support.registry.ZoneAwareCluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROXY_CLASS_REF;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SEMICOLON_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidLocalHost;
import static org.apache.dubbo.config.Constants.DUBBO_IP_TO_REGISTRY;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

/**
 * 请避免将此课程用于任何新的应用程序
 * Please avoid using this class for any new application,
 * 使用ReferenceConfigBase代替此类
 * use {@link ReferenceConfigBase} instead.
 */
public class ReferenceConfig<T> extends ReferenceConfigBase<T> {

    public static final Logger logger = LoggerFactory.getLogger(ReferenceConfig.class);

    /**
     * The {@link Protocol} implementation with adaptive functionality,it will be different in different scenarios.
     * A particular {@link Protocol} implementation is determined by the protocol attribute in the {@link URL}.
     * For example:
     *
     * <li>when the url is registry://224.5.6.7:1234/org.apache.dubbo.registry.RegistryService?application=dubbo-sample,
     * then the protocol is <b>RegistryProtocol</b></li>
     *
     * <li>when the url is dubbo://224.5.6.7:1234/org.apache.dubbo.config.api.DemoService?application=dubbo-sample, then
     * the protocol is <b>DubboProtocol</b></li>
     * <p>
     * Actually，when the {@link ExtensionLoader} init the {@link Protocol} instants,it will automatically wraps two
     * layers, and eventually will get a <b>ProtocolFilterWrapper</b> or <b>ProtocolListenerWrapper</b>
     */
    private static final Protocol REF_PROTOCOL = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    /**
     * The {@link Cluster}'s implementation with adaptive functionality, and actually it will get a {@link Cluster}'s
     * specific implementation who is wrapped with <b>MockClusterInvoker</b>
     */
    private static final Cluster CLUSTER = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();

    /**
     * A {@link ProxyFactory} implementation that will generate a reference service's proxy,the JavassistProxyFactory is
     * its default implementation
     */
    private static final ProxyFactory PROXY_FACTORY = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    /**
     * 远程引用的接口
     * The interface proxy reference
     */
    private transient volatile T ref;

    /**
     * 引用服务的调用程序
     * The invoker of the reference service
     */
    private transient volatile Invoker<?> invoker;

    /**
     * 是否已经初始化
     * The flag whether the ReferenceConfig has been initialized
     */
    private transient volatile boolean initialized;

    /**
     * whether this ReferenceConfig has been destroyed
     */
    private transient volatile boolean destroyed;

    private DubboBootstrap bootstrap;

    public ReferenceConfig() {
    }

    public ReferenceConfig(Reference reference) {
        super(reference);
    }

    public synchronized T get() {
        //如果已经销毁服务引用，则抛出异常
        if (destroyed) {
            throw new IllegalStateException("The invoker of ReferenceConfig(" + url + ") has already destroyed!");
        }
        //保证单例，在没有引用服务的情况下进行初始化
        if (ref == null) {
            init();
        }
        return ref;
    }

    /**
     * 销毁接口引用
     */
    public synchronized void destroy() {
        //如果没有服务被引用就直接退出
        if (ref == null) {
            return;
        }
        //如果已经销毁直接退出
        if (destroyed) {
            return;
        }
        //标记为已经销毁
        destroyed = true;
        try {
            //销毁节点
            invoker.destroy();
        } catch (Throwable t) {
            logger.warn("Unexpected error occured when destroy invoker of ReferenceConfig(" + url + ").", t);
        }
        //将执行器和引用指向空
        invoker = null;
        ref = null;

        //发送一个引用销毁的事件
        // dispatch a ReferenceConfigDestroyedEvent since 2.7.4
        dispatch(new ReferenceConfigDestroyedEvent(this));
    }

    /**
     * 初始化引用接口
     */
    public synchronized void init() {
        //如果已经初始化直接退出
        if (initialized) {
            return;
        }

        //如果dubbo上下文没哟初始化则进行初始化
        if (bootstrap == null) {
            bootstrap = DubboBootstrap.getInstance();
            bootstrap.init();
        }

        //检查和在必要时更新基础配置
        checkAndUpdateSubConfigs();

        //初始化服务元数据：版本号、组、默认组、引用接口类和类名、秘钥
        //init serivceMetadata
        serviceMetadata.setVersion(version);
        serviceMetadata.setGroup(group);
        serviceMetadata.setDefaultGroup(group);
        serviceMetadata.setServiceType(getActualInterface());
        serviceMetadata.setServiceInterfaceName(interfaceName);
        // TODO, uncomment this line once service key is unified
        serviceMetadata.setServiceKey(URL.buildKey(interfaceName, group, version));

        //引用接口的父类合法性检查
        checkStubAndLocal(interfaceClass);
        //引用接口的mock类合法性检查
        ConfigValidationUtils.checkMock(interfaceClass, this);

        Map<String, String> map = new HashMap<String, String>();
        map.put(SIDE_KEY, CONSUMER_SIDE);

        //添加运行期数据
        ReferenceConfigBase.appendRuntimeParameters(map);
        if (!ProtocolUtils.isGeneric(generic)) {
            //添加版本号
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put(REVISION_KEY, revision);
            }

            //添加引用的方法，没有指定引用方法就指定为*
            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if (methods.length == 0) {
                logger.warn("No method found in service interface " + interfaceClass.getName());
                map.put(METHODS_KEY, ANY_VALUE);
            } else {
                map.put(METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), COMMA_SEPARATOR));
            }
        }
        map.put(INTERFACE_KEY, interfaceName);
        //metrics监控中心
        AbstractConfig.appendParameters(map, metrics);
        //应用信息配置
        AbstractConfig.appendParameters(map, application);
        //模块中心配置
        AbstractConfig.appendParameters(map, module);
        // remove 'default.' prefix for configs from ConsumerConfig
        // appendParameters(map, consumer, Constants.DEFAULT_KEY);
        //消费者客户端
        AbstractConfig.appendParameters(map, consumer);
        //服务引用配置
        AbstractConfig.appendParameters(map, this);
        Map<String, Object> attributes = null;
        if (CollectionUtils.isNotEmpty(getMethods())) {
            attributes = new HashMap<>();
            //循环处理每个方法
            for (MethodConfig methodConfig : getMethods()) {
                AbstractConfig.appendParameters(map, methodConfig, methodConfig.getName());
                String retryKey = methodConfig.getName() + ".retry";
                //如果指定了重试次数，但是也指定了不重试
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        //将重试次数覆盖为0
                        map.put(methodConfig.getName() + ".retries", "0");
                    }
                }
                //尝试将方法配置转换为异步方法配置
                ConsumerModel.AsyncMethodInfo asyncMethodInfo = AbstractConfig.convertMethodConfig2AsyncInfo(methodConfig);
                //如果转换成功，就记录方法异步调用的拦截器
                if (asyncMethodInfo != null) {
//                    consumerModel.getMethodModel(methodConfig.getName()).addAttribute(ASYNC_KEY, asyncMethodInfo);
                    attributes.put(methodConfig.getName(), asyncMethodInfo);
                }
            }
        }

        //从系统环境、系统属性中获取需要注册的IP
        String hostToRegistry = ConfigUtils.getSystemProperty(DUBBO_IP_TO_REGISTRY);
        if (StringUtils.isEmpty(hostToRegistry)) {
            //从本地网卡中查找第一个有效IP
            hostToRegistry = NetUtils.getLocalHost();
        } else if (isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" + DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        }
        map.put(REGISTER_IP_KEY, hostToRegistry);

        //将以上信息作为服务元数据的附带信息
        serviceMetadata.getAttachments().putAll(map);

        //获取服务仓库
        ServiceRepository repository = ApplicationModel.getServiceRepository();
        //包装引用的接口和接口元数据
        ServiceDescriptor serviceDescriptor = repository.registerService(interfaceClass);
        //在服务仓库中登记引入的接口
        repository.registerConsumer(
                serviceMetadata.getServiceKey(),
                attributes,
                serviceDescriptor,
                this,
                null,
                serviceMetadata);

        //创建引用服务的代理
        ref = createProxy(map);

        //记录引用服务
        serviceMetadata.setTarget(ref);
        serviceMetadata.addAttribute(PROXY_CLASS_REF, ref);
        //记录已经注册的消费者
        repository.lookupReferredService(serviceMetadata.getServiceKey()).setProxyObject(ref);

        //标记已经创建完成
        initialized = true;

        //发一个引用服务的事件出去
        // dispatch a ReferenceConfigInitializedEvent since 2.7.4
        dispatch(new ReferenceConfigInitializedEvent(this, invoker));
    }

    /**
     * 创建引用接口对象的代理
     * @param map
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
    private T createProxy(Map<String, String> map) {
        //是否应该从jvm里引用
        if (shouldJvmRefer(map)) {
            URL url = new URL(LOCAL_PROTOCOL, LOCALHOST_VALUE, 0, interfaceClass.getName()).addParameters(map);
            //引用远程服务
            invoker = REF_PROTOCOL.refer(interfaceClass, url);
            if (logger.isInfoEnabled()) {
                logger.info("Using injvm service " + interfaceClass.getName());
            }
        } else {
            //情况引用服务的URL集合，重新处理一遍
            urls.clear();
            //用户指定的URL，可以是点对点地址，也可以是注册中心的地址。
            if (url != null && url.length() > 0) { // user specified URL, could be peer-to-peer address, or register center's address.
                String[] us = SEMICOLON_SPLIT_PATTERN.split(url);
                if (us != null && us.length > 0) {
                    //循环处理URL，进行进一步填充
                    for (String u : us) {
                        URL url = URL.valueOf(u);
                        if (StringUtils.isEmpty(url.getPath())) {
                            url = url.setPath(interfaceName);
                        }
                        //如果是注册中心的地址
                        if (UrlUtils.isRegistry(url)) {
                            //追加参数
                            urls.add(url.addParameterAndEncoded(REFER_KEY, StringUtils.toQueryString(map)));
                        } else {
                            urls.add(ClusterUtils.mergeUrl(url, map));
                        }
                    }
                }
            } else { // assemble URL from register center's configuration
                // if protocols not injvm checkRegistry
                //没有指定的URL，从注册中心里获取（不能是jvm内部内存）
                if (!LOCAL_PROTOCOL.equalsIgnoreCase(getProtocol())) {
                    //检查注册中心的配置
                    checkRegistry();
                    //解析注册中心的URL
                    List<URL> us = ConfigValidationUtils.loadRegistries(this, false);
                    if (CollectionUtils.isNotEmpty(us)) {
                        for (URL u : us) {
                            //获取监控中心的URL地址
                            URL monitorUrl = ConfigValidationUtils.loadMonitor(this, u);
                            if (monitorUrl != null) {
                                map.put(MONITOR_KEY, URL.encode(monitorUrl.toFullString()));
                            }
                            //注册URL
                            urls.add(u.addParameterAndEncoded(REFER_KEY, StringUtils.toQueryString(map)));
                        }
                    }
                    //如果没指定URL，在注册中心上也获取不到，则需要抛出异常了
                    if (urls.isEmpty()) {
                        throw new IllegalStateException("No such any registry to reference " + interfaceName + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <dubbo:registry address=\"...\" /> to your spring config.");
                    }
                }
            }

            //如果只有一个URL，进行单次注册
            if (urls.size() == 1) {
                invoker = REF_PROTOCOL.refer(interfaceClass, urls.get(0));
            } else {
                //进行循环注册
                List<Invoker<?>> invokers = new ArrayList<Invoker<?>>();
                URL registryURL = null;
                for (URL url : urls) {
                    //添加一个URL注册器
                    invokers.add(REF_PROTOCOL.refer(interfaceClass, url));
                    //如果是注册中心，使用上一个URL
                    if (UrlUtils.isRegistry(url)) {
                        registryURL = url; // use last registry url
                    }
                }
                //注册URL不能为空
                if (registryURL != null) { // registry url is available
                    //对于多订阅方案，默认情况下使用“区域优先”策略
                    // for multi-subscription scenario, use 'zone-aware' policy by default
                    URL u = registryURL.addParameterIfAbsent(CLUSTER_KEY, ZoneAwareCluster.NAME);
                    //调用程序包装关系如下：ZoneAwareClusterInvoker（StaticDirectory）->FailoverClusterInvoker（RegistryDirectory，路由发生在这里）->invoker
                    // The invoker wrap relation would be like: ZoneAwareClusterInvoker(StaticDirectory) -> FailoverClusterInvoker(RegistryDirectory, routing happens here) -> Invoker
                    invoker = CLUSTER.join(new StaticDirectory(u, invokers));
                } else { // not a registry url, must be direct invoke.
                    //不是注册表url，必须是直接调用。
                    invoker = CLUSTER.join(new StaticDirectory(invokers));
                }
            }
        }

        //如果需要检查而且无法获取引用程序，需要抛异常
        if (shouldCheck() && !invoker.isAvailable()) {
            throw new IllegalStateException("Failed to check the status of the service "
                    + interfaceName
                    + ". No provider available for the service "
                    + (group == null ? "" : group + "/")
                    + interfaceName +
                    (version == null ? "" : ":" + version)
                    + " from the url "
                    + invoker.getUrl()
                    + " to the consumer "
                    + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion());
        }
        if (logger.isInfoEnabled()) {
            logger.info("Refer dubbo service " + interfaceClass.getName() + " from url " + invoker.getUrl());
        }
        /**
         * 2.7.0之后 注册服务元数据信息
         * @since 2.7.0
         * ServiceData Store
         */
        String metadata = map.get(METADATA_KEY);
        WritableMetadataService metadataService = WritableMetadataService.getExtension(metadata == null ? DEFAULT_METADATA_STORAGE_TYPE : metadata);
        if (metadataService != null) {
            URL consumerURL = new URL(CONSUMER_PROTOCOL, map.remove(REGISTER_IP_KEY), 0, map.get(INTERFACE_KEY), map);
            metadataService.publishServiceDefinition(consumerURL);
        }
        // 创建服务代理
        // create service proxy
        return (T) PROXY_FACTORY.getProxy(invoker);
    }

    /**
     * 这个方法应该在创建这个类的实例之后调用，在使用其他配置模块中的任何属性之前。
     * This method should be called right after the creation of this class's instance, before any property in other config modules is used.
     * 检查每个配置模块是否正确创建，并在必要时重写其属性。
     * Check each config modules are created properly and override their properties if necessary.
     */
    public void checkAndUpdateSubConfigs() {
        //引用接口名不可以为空
        if (StringUtils.isEmpty(interfaceName)) {
            throw new IllegalStateException("<dubbo:reference interface=\"\" /> interface not allow null!");
        }
        //填充为空的配置
        completeCompoundConfigs();
        //获取消费者的全局配置
        // get consumer's global configuration
        //检查和初始化消费者
        checkDefault();
        //刷新配置，在必要时重写配置的值
        this.refresh();
        //如果没有使用泛化服务，但是消费者协议里指定了泛化服务，就填充一下
        if (getGeneric() == null && getConsumer() != null) {
            setGeneric(getConsumer().getGeneric());
        }
        //如果是泛化服务
        if (ProtocolUtils.isGeneric(generic)) {
            //指定接口类是泛化服务接口
            interfaceClass = GenericService.class;
        } else {
            try {
                //检查引用的类是否能被加载到
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
            } catch (ClassNotFoundException e) {
                //如果找不到引用的类需要抛出异常
                throw new IllegalStateException(e.getMessage(), e);
            }
            //检查远程服务接口和方法是否符合Dubbo的要求
            checkInterfaceAndMethods(interfaceClass, getMethods());
        }
        //解析映射文件
        resolveFile();
        //检查引用服务的配置是否合法
        ConfigValidationUtils.validateReferenceConfig(this);
        //追加URL参数
        appendParameters();
    }


    /**
     * 是否应该从JVM中引用
     * 找出应该从配置中引用相同JVM中的服务。默认行为是true
     * Figure out should refer the service in the same JVM from configurations. The default behavior is true
     * 一。如果指定了injvm，则使用它
     * 1. if injvm is specified, then use it
     * 2。如果指定了一个url，那么假设它是一个远程调用
     * 2. then if a url is specified, then assume it's a remote call
     * 三。否则，请检查范围参数
     * 3. otherwise, check scope parameter
     * 四。如果未指定作用域，但目标服务是在同一个JVM中提供的，然后倾向于进行本地调用，这是默认行为
     * 4. if scope is not specified but the target service is provided in the same JVM, then prefer to make the local
     * call, which is the default behavior
     */
    protected boolean shouldJvmRefer(Map<String, String> map) {
        URL tmpUrl = new URL("temp", "localhost", 0, map);
        boolean isJvmRefer;
        //一。是否是当前jvm引用
        if (isInjvm() == null) {
            //2。如果指定了一个url，那么假设它是一个远程调用
            // if a url is specified, don't do local reference
            if (url != null && url.length() > 0) {
                isJvmRefer = false;
            } else {
                //四。如果未指定作用域，但目标服务是在同一个JVM中提供的，然后倾向于进行本地调用，这是默认行为
                // by default, reference local service if there is
                isJvmRefer = InjvmProtocol.getInjvmProtocol().isInjvmRefer(tmpUrl);
            }
        } else {
            isJvmRefer = isInjvm();
        }
        return isJvmRefer;
    }

    /**
     * Dispatch an {@link Event event}
     *
     * @param event an {@link Event event}
     * @since 2.7.5
     */
    protected void dispatch(Event event) {
        EventDispatcher.getDefaultExtension().dispatch(event);
    }

    public DubboBootstrap getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(DubboBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @SuppressWarnings("unused")
    private final Object finalizerGuardian = new Object() {
        @Override
        protected void finalize() throws Throwable {
            super.finalize();

            if (!ReferenceConfig.this.destroyed) {
                logger.warn("ReferenceConfig(" + url + ") is not DESTROYED when FINALIZE");

                /* don't destroy for now
                try {
                    ReferenceConfig.this.destroy();
                } catch (Throwable t) {
                        logger.warn("Unexpected err when destroy invoker of ReferenceConfig(" + url + ") in finalize method!", t);
                }
                */
            }
        }
    };

    /**
     * 追加URL参数
     */
    public void appendParameters() {
        //获取需要追加的参数
        URL appendParametersUrl = URL.valueOf("appendParameters://");
        List<AppendParametersComponent> appendParametersComponents = ExtensionLoader.getExtensionLoader(AppendParametersComponent.class).getActivateExtension(appendParametersUrl, (String[]) null);
        appendParametersComponents.forEach(component -> component.appendReferParameters(this));
    }

    // just for test
    Invoker<?> getInvoker() {
        return invoker;
    }
}
