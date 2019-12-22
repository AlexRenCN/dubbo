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
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.event.ServiceConfigExportedEvent;
import org.apache.dubbo.config.event.ServiceConfigUnexportedEvent;
import org.apache.dubbo.config.invoker.DelegateProviderMetaDataInvoker;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.event.Event;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.ConfiguratorFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_IP_TO_BIND;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;
import static org.apache.dubbo.common.utils.NetUtils.getAvailablePort;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidPort;
import static org.apache.dubbo.config.Constants.DUBBO_IP_TO_REGISTRY;
import static org.apache.dubbo.config.Constants.DUBBO_PORT_TO_BIND;
import static org.apache.dubbo.config.Constants.DUBBO_PORT_TO_REGISTRY;
import static org.apache.dubbo.config.Constants.MULTICAST;
import static org.apache.dubbo.config.Constants.SCOPE_NONE;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.Constants.PROXY_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;

/**
 * 服务提供者暴露服务配置
 * @param <T>
 */
public class ServiceConfig<T> extends ServiceConfigBase<T> {

    public static final Logger logger = LoggerFactory.getLogger(ServiceConfig.class);

    /**
     * 一个随机端口缓存，没有指定端口的不同协议有不同的随机端口
     * A random port cache, the different protocols who has no port specified have different random port
     */
    private static final Map<String, Integer> RANDOM_PORT_MAP = new HashMap<String, Integer>();

    /**
     * 延迟曝光服务的线程池
     * A delayed exposure service timer
     */
    private static final ScheduledExecutorService DELAY_EXPORT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboServiceDelayExporter", true));

    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    /**
     * A {@link ProxyFactory} implementation that will generate a exported service proxy,the JavassistProxyFactory is its
     * default implementation
     */
    private static final ProxyFactory PROXY_FACTORY = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    /**
     * 是否已经注册
     * Whether the provider has been exported
     */
    private transient volatile boolean exported;

    /**
     * 服务是否未注册标志，如果调用未注册的方法，则该值为true
     * The flag whether a service has unexported ,if the method unexported is invoked, the value is true
     */
    private transient volatile boolean unexported;

    /**
     * dubbo上下文
     */
    private DubboBootstrap bootstrap;

    /**
     * 暴露的服务
     * The exported services
     */
    private final List<Exporter<?>> exporters = new ArrayList<Exporter<?>>();

    public ServiceConfig() {
    }

    public ServiceConfig(Service service) {
        super(service);
    }

    @Parameter(excluded = true)
    public boolean isExported() {
        return exported;
    }

    @Parameter(excluded = true)
    public boolean isUnexported() {
        return unexported;
    }

    public void unexport() {
        if (!exported) {
            return;
        }
        if (unexported) {
            return;
        }
        if (!exporters.isEmpty()) {
            for (Exporter<?> exporter : exporters) {
                try {
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn("Unexpected error occured when unexport " + exporter, t);
                }
            }
            exporters.clear();
        }
        unexported = true;

        // dispatch a ServiceConfigUnExportedEvent since 2.7.4
        dispatch(new ServiceConfigUnexportedEvent(this));
    }

    /**
     * TODO 服务提供者暴露服务的入口代码
     */
    public synchronized void export() {
        //如果不需要暴露服务则直接返回
        if (!shouldExport()) {
            return;
        }

        //如果还没有加载dubbo上下文，就在此加载并初始化
        if (bootstrap == null) {
            bootstrap = DubboBootstrap.getInstance();
            bootstrap.init();
        }

        //检查和更新配置
        checkAndUpdateSubConfigs();

        //初始化服务的元数据
        //init serviceMetadata
        //填充版本号
        serviceMetadata.setVersion(version);
        //填充所属组
        serviceMetadata.setGroup(group);
        //填充默认组
        serviceMetadata.setDefaultGroup(group);
        //填充接口类型
        serviceMetadata.setServiceType(getInterfaceClass());
        //填充接口名
        serviceMetadata.setServiceInterfaceName(getInterface());
        //填充接口应用实现
        serviceMetadata.setTarget(getRef());

        //如果需要延迟注册，使用线程池注册一个线程用来注册
        if (shouldDelay()) {
            DELAY_EXPORT_EXECUTOR.schedule(this::doExport, getDelay(), TimeUnit.MILLISECONDS);
        } else {
            //直接注册
            doExport();
        }
    }

    /**
     * 检查和更新配置
     */
    private void checkAndUpdateSubConfigs() {
        // 填充本服务一些空配置，从其他配置中获取值进行对空的覆盖
        // Use default configs defined explicitly on global scope
        completeCompoundConfigs();
        // 检查是否存在默认的服务提供者配置并决定是否创建
        checkDefault();
        // 检查协议
        checkProtocol();
        // 如果协议不是本地协议
        // if protocol is not injvm checkRegistry
        if (!isOnlyInJvm()) {
            //检查注册中心
            checkRegistry();
        }
        // 检查和刷新配置
        this.refresh();

        //如果暴露的接口名为空则报错
        if (StringUtils.isEmpty(interfaceName)) {
            throw new IllegalStateException("<dubbo:service interface=\"\" /> interface not allow null!");
        }

        //如果实现是一个泛化服务（服务的消费者通过$invoke进行消费）
        if (ref instanceof GenericService) {
            //接口类型指定为泛化服务接口
            interfaceClass = GenericService.class;
            if (StringUtils.isEmpty(generic)) {
                //如果没有设置为泛化服务则设置为true
                generic = Boolean.TRUE.toString();
            }
        } else {
            try {
                //反射获取暴露服务的类型
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            //检查远程服务接口和方法是否符合Dubbo的要求
            checkInterfaceAndMethods(interfaceClass, getMethods());
            //检查引用的服务
            checkRef();
            //设置为非泛化服务
            generic = Boolean.FALSE.toString();
        }
        //如果指定了本地实现
        if (local != null) {
            //指定默认的本地实现名，从默认值local改成接口名称+Local
            if ("true".equals(local)) {
                local = interfaceName + "Local";
            }
            Class<?> localClass;
            try {
                //从当前classLoader里获取本地实现类，没有找到就抛出异常
                localClass = ClassUtils.forNameWithThreadContextClassLoader(local);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            //如果本地实现类没有实现dubbo配置里的接口则抛出异常
            if (!interfaceClass.isAssignableFrom(localClass)) {
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceName);
            }
        }
        //如果指定了父类
        if (stub != null) {
            //指定默认的父类名，从默认值local改成接口名称+Stub
            if ("true".equals(stub)) {
                stub = interfaceName + "Stub";
            }
            Class<?> stubClass;
            try {
                //从当前classLoader里获取父类，没有找到就抛出异常
                stubClass = ClassUtils.forNameWithThreadContextClassLoader(stub);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            //如果本地实现类没有继承dubbo配置里的类则抛出异常
            if (!interfaceClass.isAssignableFrom(stubClass)) {
                throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interface " + interfaceName);
            }
        }
        //检查父类和本地实现
        checkStubAndLocal(interfaceClass);
        //检查mock类是否合法
        ConfigValidationUtils.checkMock(interfaceClass, this);
        //检查服务配置和服务提供者、注册中心
        ConfigValidationUtils.validateServiceConfig(this);
        //追加参数
        appendParameters();
    }


    protected synchronized void doExport() {
        //如果已经注册，抛出异常
        if (unexported) {
            throw new IllegalStateException("The service " + interfaceClass.getName() + " has already unexported!");
        }
        //如果已经注册，直接返回
        if (exported) {
            return;
        }
        //标记已经注册
        exported = true;

        //如果没有指定path地址，则默认使用接口名
        if (StringUtils.isEmpty(path)) {
            path = interfaceName;
        }
        //将服务注册
        doExportUrls();

        //发一个注册事件
        //事件分发从2.7.4开始
        // dispatch a ServiceConfigExportedEvent since 2.7.4
        dispatch(new ServiceConfigExportedEvent(this));
    }

    /**
     * 将服务注册再一个地址上
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doExportUrls() {
        //需要注册的服务信息
        ServiceRepository repository = ApplicationModel.getServiceRepository();
        //需要注册的接口信息
        ServiceDescriptor serviceDescriptor = repository.registerService(getInterfaceClass());
        //登记成一个服务提供者
        repository.registerProvider(
                //通过接口名、组、版本号拼接URL地址
                getUniqueServiceName(),
                //实现类
                ref,
                //接口信息
                serviceDescriptor,
                //接口配置
                this,
                //接口元数据
                serviceMetadata
        );

        //解析注册中心的URL
        List<URL> registryURLs = ConfigValidationUtils.loadRegistries(this, true);

        //循环处理所有需要暴露的服务提供者协议，对每个协议进行一次全量注册
        for (ProtocolConfig protocolConfig : protocols) {
            //将服务提供者协议配置解析成协议URL
            String pathKey = URL.buildKey(getContextPath(protocolConfig)
                    .map(p -> p + "/" + path)
                    .orElse(path), group, version);
            // 如果用户指定了路径，请再次注册服务以将其映射到路径。
            // In case user specified path, register service one more time to map it to path.
            repository.registerService(pathKey, interfaceClass);
            // TODO, uncomment this line once service key is unified
            //设置服务的秘钥
            serviceMetadata.setServiceKey(pathKey);
            //对每个协议进行一次全量注册
            doExportUrlsFor1Protocol(protocolConfig, registryURLs);
        }
    }

    /**
     * 每个协议单独注册一次URL
     * @param protocolConfig
     * @param registryURLs
     */
    private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
        //获取服务提供者协议，默认是dubbo
        String name = protocolConfig.getName();
        if (StringUtils.isEmpty(name)) {
            name = DUBBO;
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put(SIDE_KEY, PROVIDER_SIDE);

        //添加运行时参数
        ServiceConfig.appendRuntimeParameters(map);
        //Metrics服务监控中心配置
        AbstractConfig.appendParameters(map, metrics);
        //应用信息配置
        AbstractConfig.appendParameters(map, application);
        //模块信息配置
        AbstractConfig.appendParameters(map, module);
        // remove 'default.' prefix for configs from ProviderConfig
        // appendParameters(map, provider, Constants.DEFAULT_KEY);
        //服务提供者配置
        AbstractConfig.appendParameters(map, provider);
        //单独的协议配置（每种协议都走一遍这个代码）
        AbstractConfig.appendParameters(map, protocolConfig);
        //服务配置
        AbstractConfig.appendParameters(map, this);
        //循环处理方法配置
        if (CollectionUtils.isNotEmpty(getMethods())) {
            for (MethodConfig method : getMethods()) {
                //追加参数
                AbstractConfig.appendParameters(map, method, method.getName());
                //获取方法是否重试
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    //如果不重试
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        //覆盖重试次数为0
                        map.put(method.getName() + ".retries", "0");
                    }
                }
                //循环处理每个方法的参数配置
                List<ArgumentConfig> arguments = method.getArguments();
                if (CollectionUtils.isNotEmpty(arguments)) {
                    for (ArgumentConfig argument : arguments) {
                        //如果有参数类型，就进行转换
                        // convert argument type
                        if (argument.getType() != null && argument.getType().length() > 0) {
                            Method[] methods = interfaceClass.getMethods();
                            //访问所有的方法
                            // visit all methods
                            if (methods != null && methods.length > 0) {
                                for (int i = 0; i < methods.length; i++) {
                                    String methodName = methods[i].getName();
                                    //以方法为目标，并获取其签名
                                    // target the method, and get its signature
                                    if (methodName.equals(method.getName())) {
                                        //获取该方法的参数类型列表
                                        Class<?>[] argtypes = methods[i].getParameterTypes();
                                        // 一个方法一个回调
                                        // one callback in the method
                                        if (argument.getIndex() != -1) {
                                            //转换参数的类型
                                            if (argtypes[argument.getIndex()].getName().equals(argument.getType())) {
                                                //追加参数
                                                AbstractConfig.appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                                            } else {
                                                throw new IllegalArgumentException("Argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                            }
                                        } else {
                                            // 一个方法多个回调
                                            // multiple callbacks in the method
                                            for (int j = 0; j < argtypes.length; j++) {
                                                //循环处理转换参数的类型
                                                Class<?> argclazz = argtypes[j];
                                                if (argclazz.getName().equals(argument.getType())) {
                                                    //追加参数
                                                    AbstractConfig.appendParameters(map, argument, method.getName() + "." + j);
                                                    if (argument.getIndex() != -1 && argument.getIndex() != j) {
                                                        throw new IllegalArgumentException("Argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        //==0的时候，追加参数
                        } else if (argument.getIndex() != -1) {
                            AbstractConfig.appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                        } else {
                            throw new IllegalArgumentException("Argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                        }

                    }
                }
            } // end of methods for
        }

        //如果是泛化服务
        if (ProtocolUtils.isGeneric(generic)) {
            map.put(GENERIC_KEY, generic);
            //支持所有的方法调用
            map.put(METHODS_KEY, ANY_VALUE);
        //如果是其他协议服务
        } else {
            //获取和填充版本号
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put(REVISION_KEY, revision);
            }

            //获取所有方法名
            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if (methods.length == 0) {
                //没有方法的话，使用日志记录，并且允许任何接口地址的调用
                logger.warn("No method found in service interface " + interfaceClass.getName());
                map.put(METHODS_KEY, ANY_VALUE);
            } else {
                //记录允许调用的方法名
                map.put(METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }
        //如果指定了token
        if (!ConfigUtils.isEmpty(token)) {
            //如果是默认token，就随机生成
            if (ConfigUtils.isDefault(token)) {
                map.put(TOKEN_KEY, UUID.randomUUID().toString());
            } else {
                //否则使用指定的token
                map.put(TOKEN_KEY, token);
            }
        }
        //初始化服务元数据信息
        //init serviceMetadata attachments
        serviceMetadata.getAttachments().putAll(map);

        //暴露服务
        // export service
        //获取在dubbo上登记的ip
        String host = findConfigedHosts(protocolConfig, registryURLs, map);
        //获取在dubbo上登记的端口
        Integer port = findConfigedPorts(protocolConfig, name, map);
        //构建URL对象
        URL url = new URL(name, host, port, getContextPath(protocolConfig).map(p -> p + "/" + path).orElse(path), map);

        //通过自定义参数添加额外参数
        // You can customize Configurator to append extra parameters
        if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                .hasExtension(url.getProtocol())) {
            url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                    .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
        }

        //获取作用域
        String scope = url.getParameter(SCOPE_KEY);
        //没有配置作用域时不暴露服务
        // don't export when none is configured
        if (!SCOPE_NONE.equalsIgnoreCase(scope)) {

            //如果配置不是远程的，则暴露服务到本地（仅当配置是远程的时暴露服务到远程）
            // export to local if the config is not remote (export to remote only when config is remote)
            if (!SCOPE_REMOTE.equalsIgnoreCase(scope)) {
                //在本地暴露服务
                exportLocal(url);
            }
            //如果配置不是本地的，则暴露服务到远程（仅当配置是本地的时暴露服务到本地）
            // export to remote if the config is not local (export to local only when config is local)
            if (!SCOPE_LOCAL.equalsIgnoreCase(scope)) {
                //遍历处理所有要注册的URL
                if (CollectionUtils.isNotEmpty(registryURLs)) {
                    for (URL registryURL : registryURLs) {
                        //if protocol is only injvm ,not register
                        // 如果是本地协议不需要注册
                        if (LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
                            continue;
                        }
                        //是否采用动态注册，动态注册的服务再注册之后需要手动的开启，下线之后也需要手动的关闭
                        url = url.addParameterIfAbsent(DYNAMIC_KEY, registryURL.getParameter(DYNAMIC_KEY));
                        //获取监控中心地址
                        URL monitorUrl = ConfigValidationUtils.loadMonitor(this, registryURL);
                        if (monitorUrl != null) {
                            //将监听中心填充给URL对象
                            url = url.addParameterAndEncoded(MONITOR_KEY, monitorUrl.toFullString());
                        }
                        if (logger.isInfoEnabled()) {
                            if (url.getParameter(REGISTER_KEY, true)) {
                                logger.info("Register dubbo service " + interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                            } else {
                                logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                            }
                        }

                        //对于服务提供者，这用于启用自定义代理来生成调用程序
                        // For providers, this is used to enable custom proxy to generate invoker
                        //获取代理策略
                        String proxy = url.getParameter(PROXY_KEY);
                        if (StringUtils.isNotEmpty(proxy)) {
                            //如果指定了代理策略就填充到URL对象中
                            registryURL = registryURL.addParameter(PROXY_KEY, proxy);
                        }

                        //构建暴露的URL调用对象
                        Invoker<?> invoker = PROXY_FACTORY.getInvoker(ref, (Class) interfaceClass, registryURL.addParameterAndEncoded(EXPORT_KEY, url.toFullString()));
                        //把URL和应用配置进行包装
                        DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);

                        //添加URL暴露工具
                        Exporter<?> exporter = protocol.export(wrapperInvoker);
                        //登记需要暴露的URL
                        exporters.add(exporter);
                    }
                } else {
                    //没有需要对外暴露的URL，比如消费者直连生产者
                    if (logger.isInfoEnabled()) {
                        logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                    }
                    //把需要暴露的接口转换成需要暴露的URL
                    Invoker<?> invoker = PROXY_FACTORY.getInvoker(ref, (Class) interfaceClass, url);
                    //把URL和应用配置进行包装
                    DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);
                    //添加URL暴露工具
                    Exporter<?> exporter = protocol.export(wrapperInvoker);
                    //登记需要暴露的URL
                    exporters.add(exporter);
                }
                /**
                 * 2.7.0之后，把服务的元数据也暴露给dubbo
                 * @since 2.7.0
                 * ServiceData Store
                 */
                //构建一个可以后期修改的服务元数据对象
                WritableMetadataService metadataService = WritableMetadataService.getExtension(url.getParameter(METADATA_KEY, DEFAULT_METADATA_STORAGE_TYPE));
                if (metadataService != null) {
                    //暴露服务元数据
                    metadataService.publishServiceDefinition(url);
                }
            }
        }
        this.urls.add(url);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    /**
     * 在JVM内部永久暴露服务
     * always export injvm
     */
    private void exportLocal(URL url) {
        //构建一个jvm内部暴露的URL
        URL local = URLBuilder.from(url)
                .setProtocol(LOCAL_PROTOCOL)
                .setHost(LOCALHOST_VALUE)
                .setPort(0)
                .build();
        //并且登记
        Exporter<?> exporter = protocol.export(
                PROXY_FACTORY.getInvoker(ref, (Class) interfaceClass, local));
        exporters.add(exporter);
        logger.info("Export dubbo service " + interfaceClass.getName() + " to local registry url : " + local);
    }

    /**
     * 确定是不是本地协议
     * Determine if it is injvm
     *
     * @return
     */
    private boolean isOnlyInJvm() {
        return getProtocols().size() == 1
                && LOCAL_PROTOCOL.equalsIgnoreCase(getProtocols().get(0).getName());
    }


    /**
     * 为服务提供者注册IP和本地绑定IP，可以单独配置。
     * Register & bind IP address for service provider, can be configured separately.
     * 配置优先级：环境变量->java系统属性->配置文件中的ip属性->etc/hosts->默认网络地址->第一个可用网络地址
     * Configuration priority: environment variables -> java system properties -> host property in config file ->
     * /etc/hosts -> default network address -> first available network address
     *
     * @param protocolConfig
     * @param registryURLs
     * @param map
     * @return
     */
    private String findConfigedHosts(ProtocolConfig protocolConfig,
                                     List<URL> registryURLs,
                                     Map<String, String> map) {
        //在系统和配置文件上是否指定了IP
        boolean anyhost = false;
        //从系统配置中获取需要绑定的ip
        String hostToBind = getValueFromConfig(protocolConfig, DUBBO_IP_TO_BIND);
        //如果获取到了IP但是是本地IP，就抛出异常
        if (hostToBind != null && hostToBind.length() > 0 && isInvalidLocalHost(hostToBind)) {
            throw new IllegalArgumentException("Specified invalid bind ip from property:" + DUBBO_IP_TO_BIND + ", value:" + hostToBind);
        }

        //如果系统配置中找不到需要绑定的IP，就继续找
        // if bind ip is not found in environment, keep looking up
        if (StringUtils.isEmpty(hostToBind)) {
            //查看协议配置中是否指定了IP
            hostToBind = protocolConfig.getHost();
            if (provider != null && StringUtils.isEmpty(hostToBind)) {
                //从服务提供者中获取指定IP
                hostToBind = provider.getHost();
            }
            //如果配置文件指定的是本地IP
            if (isInvalidLocalHost(hostToBind)) {
                anyhost = true;
                try {
                    //从网卡中找IP地址
                    logger.info("No valid ip found from environment, try to find valid host from DNS.");
                    hostToBind = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    logger.warn(e.getMessage(), e);
                }
                //如果网卡指定的是本地IP
                if (isInvalidLocalHost(hostToBind)) {
                    //检查注册中心里的ip
                    if (CollectionUtils.isNotEmpty(registryURLs)) {
                        for (URL registryURL : registryURLs) {
                            if (MULTICAST.equalsIgnoreCase(registryURL.getParameter("registry"))) {
                                // skip multicast registry since we cannot connect to it via Socket
                                continue;
                            }
                            try (Socket socket = new Socket()) {
                                SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort());
                                socket.connect(addr, 1000);
                                hostToBind = socket.getLocalAddress().getHostAddress();
                                break;
                            } catch (Exception e) {
                                logger.warn(e.getMessage(), e);
                            }
                        }
                    }
                    //最后使用本地IP
                    if (isInvalidLocalHost(hostToBind)) {
                        hostToBind = getLocalHost();
                    }
                }
            }
        }

        //记录需要绑定的IP
        map.put(BIND_IP_KEY, hostToBind);

        // 指定用于注册在dubbo中的ip不能用本地IP
        // registry ip is not used for bind ip by default
        String hostToRegistry = getValueFromConfig(protocolConfig, DUBBO_IP_TO_REGISTRY);
        if (hostToRegistry != null && hostToRegistry.length() > 0 && isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" + DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        } else if (StringUtils.isEmpty(hostToRegistry)) {
            // 用注册表中的ip作为注册再dubbo中的ip
            // bind ip is used as registry ip by default
            hostToRegistry = hostToBind;
        }

        map.put(ANYHOST_KEY, String.valueOf(anyhost));

        return hostToRegistry;
    }


    /**
     * 为提供程序注册端口和绑定端口，可以单独配置
     * Register port and bind port for the provider, can be configured separately
     * 配置优先级：环境变量->java系统属性->协议配置文件中的端口属性->协议默认端口
     * Configuration priority: environment variable -> java system properties -> port property in protocol config file
     * -> protocol default port
     *
     * @param protocolConfig
     * @param name
     * @return
     */
    private Integer findConfigedPorts(ProtocolConfig protocolConfig,
                                      String name,
                                      Map<String, String> map) {
        //绑定的端口
        Integer portToBind = null;

        //从系统配置中获取绑定端口
        // parse bind port from environment
        String port = getValueFromConfig(protocolConfig, DUBBO_PORT_TO_BIND);
        //并转换成数字类型
        portToBind = parsePort(port);

        //如果在环境中找不到绑定端口，继续查找。
        // if there's no bind port found from environment, keep looking up.
        if (portToBind == null) {
            //从服务提供者协议里获取绑定端口
            portToBind = protocolConfig.getPort();
            //从服务提供者中获取绑定端口
            if (provider != null && (portToBind == null || portToBind == 0)) {
                portToBind = provider.getPort();
            }
            //协议的默认端口
            final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
            if (portToBind == null || portToBind == 0) {
                portToBind = defaultPort;
            }
            //没有指定默认端口的协议，先从缓存中获取端口
            if (portToBind == null || portToBind <= 0) {
                portToBind = getRandomPort(name);
                if (portToBind == null || portToBind < 0) {
                    //还没有缓存，随机一个端口使用
                    portToBind = getAvailablePort(defaultPort);
                    //并且放入缓存
                    putRandomPort(name, portToBind);
                }
            }
        }

        // 保存绑定端口，以后用作url的密钥
        // save bind port, used as url's key later
        map.put(BIND_PORT_KEY, String.valueOf(portToBind));

        // 注册端口，默认情况下不用绑定端口
        // registry port, not used as bind port by default
        // 从系统配置中获取dubbo注册端口
        String portToRegistryStr = getValueFromConfig(protocolConfig, DUBBO_PORT_TO_REGISTRY);
        //检查dubbo注册端口合法性并转换成数字类型
        Integer portToRegistry = parsePort(portToRegistryStr);
        if (portToRegistry == null) {
            //没有指定注册端口就使用绑定端口
            portToRegistry = portToBind;
        }

        return portToRegistry;
    }

    /**
     * 检查端口合法性并转换成数字类型
     * @param configPort
     * @return
     */
    private Integer parsePort(String configPort) {
        Integer port = null;
        if (configPort != null && configPort.length() > 0) {
            try {
                Integer intPort = Integer.parseInt(configPort);
                if (isInvalidPort(intPort)) {
                    throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
                }
                port = intPort;
            } catch (Exception e) {
                throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
            }
        }
        return port;
    }

    /**
     * 根据协议或key从系统配置中获取对应值
     * @param protocolConfig
     * @param key
     * @return
     */
    private String getValueFromConfig(ProtocolConfig protocolConfig, String key) {
        //前缀是协议名大写+"_"
        String protocolPrefix = protocolConfig.getName().toUpperCase() + "_";
        //从系统配置中获取对应协议的端口
        String port = ConfigUtils.getSystemProperty(protocolPrefix + key);
        //如果系统协议找不到，就使用key单独找一遍
        if (StringUtils.isEmpty(port)) {
            port = ConfigUtils.getSystemProperty(key);
        }
        return port;
    }

    private Integer getRandomPort(String protocol) {
        protocol = protocol.toLowerCase();
        return RANDOM_PORT_MAP.getOrDefault(protocol, Integer.MIN_VALUE);
    }

    /**
     * 缓存协议和随机端口的关联关系
     * @param protocol
     * @param port
     */
    private void putRandomPort(String protocol, Integer port) {
        protocol = protocol.toLowerCase();
        //对每个协议缓存一次
        if (!RANDOM_PORT_MAP.containsKey(protocol)) {
            RANDOM_PORT_MAP.put(protocol, port);
            logger.warn("Use random available port(" + port + ") for protocol " + protocol);
        }
    }

    public void appendParameters() {
        URL appendParametersUrl = URL.valueOf("appendParameters://");
        List<AppendParametersComponent> appendParametersComponents = ExtensionLoader.getExtensionLoader(AppendParametersComponent.class).getActivateExtension(appendParametersUrl, (String[]) null);
        appendParametersComponents.forEach(component -> component.appendExportParameters(this));
    }

    /**
     * Dispatch an {@link Event event}
     *
     * @param event an {@link Event event}
     * @since 2.7.5
     */
    private void dispatch(Event event) {
        EventDispatcher.getDefaultExtension().dispatch(event);
    }

    public DubboBootstrap getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(DubboBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }
}