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
package org.apache.dubbo.config.utils;

import com.sun.xml.internal.bind.v2.TODO;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.monitor.MonitorFactory;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.remoting.Codec;
import org.apache.dubbo.remoting.Dispatcher;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.exchange.Exchanger;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.rpc.ExporterListener;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.InvokerListener;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.support.MockInvoker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.FILE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PASSWORD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOVE_VALUE_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_SECONDS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.USERNAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.utils.UrlUtils.isServiceDiscoveryRegistryType;
import static org.apache.dubbo.config.Constants.ARCHITECTURE;
import static org.apache.dubbo.config.Constants.CONTEXTPATH_KEY;
import static org.apache.dubbo.config.Constants.DUBBO_IP_TO_REGISTRY;
import static org.apache.dubbo.config.Constants.ENVIRONMENT;
import static org.apache.dubbo.config.Constants.LAYER_KEY;
import static org.apache.dubbo.config.Constants.LISTENER_KEY;
import static org.apache.dubbo.config.Constants.NAME;
import static org.apache.dubbo.config.Constants.ORGANIZATION;
import static org.apache.dubbo.config.Constants.OWNER;
import static org.apache.dubbo.config.Constants.STATUS_KEY;
import static org.apache.dubbo.monitor.Constants.LOGSTAT_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.registry.Constants.REGISTER_KEY;
import static org.apache.dubbo.registry.Constants.SUBSCRIBE_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;
import static org.apache.dubbo.remoting.Constants.CODEC_KEY;
import static org.apache.dubbo.remoting.Constants.DISPATCHER_KEY;
import static org.apache.dubbo.remoting.Constants.EXCHANGER_KEY;
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.remoting.Constants.TELNET;
import static org.apache.dubbo.remoting.Constants.TRANSPORTER_KEY;
import static org.apache.dubbo.rpc.Constants.FAIL_PREFIX;
import static org.apache.dubbo.rpc.Constants.FORCE_PREFIX;
import static org.apache.dubbo.rpc.Constants.LOCAL_KEY;
import static org.apache.dubbo.rpc.Constants.MOCK_KEY;
import static org.apache.dubbo.rpc.Constants.PROXY_KEY;
import static org.apache.dubbo.rpc.Constants.RETURN_PREFIX;
import static org.apache.dubbo.rpc.Constants.THROW_PREFIX;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

public class ConfigValidationUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConfigValidationUtils.class);
    /**
     * The maximum length of a <b>parameter's value</b>
     */
    private static final int MAX_LENGTH = 200;

    /**
     * The maximum length of a <b>path</b>
     */
    private static final int MAX_PATH_LENGTH = 200;

    /**
     * The rule qualification for <b>name</b>
     */
    private static final Pattern PATTERN_NAME = Pattern.compile("[\\-._0-9a-zA-Z]+");

    /**
     * The rule qualification for <b>multiply name</b>
     */
    private static final Pattern PATTERN_MULTI_NAME = Pattern.compile("[,\\-._0-9a-zA-Z]+");

    /**
     * The rule qualification for <b>method names</b>
     */
    private static final Pattern PATTERN_METHOD_NAME = Pattern.compile("[a-zA-Z][0-9a-zA-Z]*");

    /**
     * The rule qualification for <b>path</b>
     */
    private static final Pattern PATTERN_PATH = Pattern.compile("[/\\-$._0-9a-zA-Z]+");

    /**
     * The pattern matches a value who has a symbol
     */
    private static final Pattern PATTERN_NAME_HAS_SYMBOL = Pattern.compile("[:*,\\s/\\-._0-9a-zA-Z]+");

    /**
     * The pattern matches a property key
     */
    private static final Pattern PATTERN_KEY = Pattern.compile("[*,\\-._0-9a-zA-Z]+");


    /**
     * 解析注册中心的URL
     * @param interfaceConfig
     * @param provider
     * @return
     */
    public static List<URL> loadRegistries(AbstractInterfaceConfig interfaceConfig, boolean provider) {
        // check && override if necessary
        List<URL> registryList = new ArrayList<URL>();
        //获取应用信息
        ApplicationConfig application = interfaceConfig.getApplication();
        //获取注册中心信息
        List<RegistryConfig> registries = interfaceConfig.getRegistries();
        //如果指定了注册中心，进行循环检查
        if (CollectionUtils.isNotEmpty(registries)) {
            for (RegistryConfig config : registries) {
                String address = config.getAddress();
                //注册中心的地址没有配置就使用0.0.0.0作为默认地址
                if (StringUtils.isEmpty(address)) {
                    address = ANYHOST_VALUE;
                }
                //如果有可用的地址
                if (!RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)) {
                    Map<String, String> map = new HashMap<String, String>();
                    AbstractConfig.appendParameters(map, application);
                    AbstractConfig.appendParameters(map, config);
                    map.put(PATH_KEY, RegistryService.class.getName());
                    //添加运行时参数
                    AbstractInterfaceConfig.appendRuntimeParameters(map);
                    //如果没有指定协议，就使用默认的dubbo协议
                    if (!map.containsKey(PROTOCOL_KEY)) {
                        map.put(PROTOCOL_KEY, DUBBO_PROTOCOL);
                    }
                    //将注册中心的地址解析成URL，并在之后循环进行更进一步的填充
                    List<URL> urls = UrlUtils.parseURLs(address, map);

                    for (URL url : urls) {

                        url = URLBuilder.from(url)
                                .addParameter(REGISTRY_KEY, url.getProtocol())
                                .setProtocol(extractRegistryType(url))
                                .build();
                        if ((provider && url.getParameter(REGISTER_KEY, true))
                                || (!provider && url.getParameter(SUBSCRIBE_KEY, true))) {
                            registryList.add(url);
                        }
                    }
                }
            }
        }
        return registryList;
    }

    /**
     * 获取监控中心地址
     * @param interfaceConfig
     * @param registryURL
     * @return
     */
    public static URL loadMonitor(AbstractInterfaceConfig interfaceConfig, URL registryURL) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(INTERFACE_KEY, MonitorService.class.getName());
        //添加运行期参数
        AbstractInterfaceConfig.appendRuntimeParameters(map);
        //获取dubbo注册IP
        //set ip
        String hostToRegistry = ConfigUtils.getSystemProperty(DUBBO_IP_TO_REGISTRY);
        if (StringUtils.isEmpty(hostToRegistry)) {
            //从本地网卡中查找第一个有效IP
            hostToRegistry = NetUtils.getLocalHost();
        } else if (NetUtils.isInvalidLocalHost(hostToRegistry)) {
            //如果是本地IP就报错
            throw new IllegalArgumentException("Specified invalid registry ip from property:" +
                    DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        }
        map.put(REGISTER_IP_KEY, hostToRegistry);

        //获取接口配置里的监控中心配置
        MonitorConfig monitor = interfaceConfig.getMonitor();
        //获取接口配置里的应用配置
        ApplicationConfig application = interfaceConfig.getApplication();
        AbstractConfig.appendParameters(map, monitor);
        AbstractConfig.appendParameters(map, application);
        //获取监控中心的IP
        String address = monitor.getAddress();
        //获取系统配置监控中心的IP
        String sysaddress = System.getProperty("dubbo.monitor.address");
        if (sysaddress != null && sysaddress.length() > 0) {
            //如果系统级别指定了监控中心的IP就进行覆盖
            address = sysaddress;
        }
        //如果找到了监控中心的地址
        if (ConfigUtils.isNotEmpty(address)) {
            //如果没有指定协议，先选择协议
            if (!map.containsKey(PROTOCOL_KEY)) {
                if (getExtensionLoader(MonitorFactory.class).hasExtension(LOGSTAT_PROTOCOL)) {
                    map.put(PROTOCOL_KEY, LOGSTAT_PROTOCOL);
                } else {
                    map.put(PROTOCOL_KEY, DUBBO_PROTOCOL);
                }
            }
            //转换成URL对象
            return UrlUtils.parseURL(address, map);
        //如果没有找到监控中心的地址，这可能是一个注册中心或者服务发现中心，从中获取监控中心
        } else if ((REGISTRY_PROTOCOL.equals(monitor.getProtocol()) || SERVICE_REGISTRY_PROTOCOL.equals(monitor.getProtocol()))
                && registryURL != null) {
            //转换成URL对象
            return URLBuilder.from(registryURL)
                    .setProtocol(DUBBO_PROTOCOL)
                    .addParameter(PROTOCOL_KEY, monitor.getProtocol())
                    .addParameterAndEncoded(REFER_KEY, StringUtils.toQueryString(map))
                    .build();
        }
        return null;
    }

    /**
     * 合法性检查和本地mock操作的建立，操作可以是带有简单操作的字符串，也可以是{@link Class}实现特定函数的类名
     * Legitimacy check and setup of local simulated operations. The operations can be a string with Simple operation or
     * a classname whose {@link Class} implements a particular function
     *
     * @param interfaceClass for provider side, it is the {@link Class} of the service that will be exported; for consumer
     *                       side, it is the {@link Class} of the remote service interface that will be referenced
     */
    public static void checkMock(Class<?> interfaceClass, AbstractInterfaceConfig config) {
        //获取mock类
        String mock = config.getMock();
        //没配置mock类就直接返回
        if (ConfigUtils.isEmpty(mock)) {
            return;
        }

        //标准化mock字符串内容
        String normalizedMock = MockInvoker.normalizeMock(mock);
        //如果是返回固定值类型
        if (normalizedMock.startsWith(RETURN_PREFIX)) {
            //从mock语句中获取 返回的真实值
            normalizedMock = normalizedMock.substring(RETURN_PREFIX.length()).trim();
            try {
                //检查模拟值是否合法，如果是非法的，抛出异常
                //Check whether the mock value is legal, if it is illegal, throw exception
                MockInvoker.parseMockValue(normalizedMock);
            } catch (Exception e) {
                throw new IllegalStateException("Illegal mock return in <dubbo:service/reference ... " +
                        "mock=\"" + mock + "\" />");
            }
        //如果是抛出异常
        } else if (normalizedMock.startsWith(THROW_PREFIX)) {
            //从mock语句中获取 返回需要抛出的异常
            normalizedMock = normalizedMock.substring(THROW_PREFIX.length()).trim();
            if (ConfigUtils.isNotEmpty(normalizedMock)) {
                try {
                    //检查模拟值是否合法
                    //Check whether the mock value is legal
                    MockInvoker.getThrowable(normalizedMock);
                } catch (Exception e) {
                    throw new IllegalStateException("Illegal mock throw in <dubbo:service/reference ... " +
                            "mock=\"" + mock + "\" />");
                }
            }
        } else {
            //检查mock类是否是interfaceClass的实现，以及它是否具有默认构造函数
            //Check whether the mock class is a implementation of the interfaceClass, and if it has a default constructor
            MockInvoker.getMockObject(normalizedMock, interfaceClass);
        }
    }

    /**
     * 检查抽象接口配置和接口配置的合法性
     * @param config
     */
    public static void validateAbstractInterfaceConfig(AbstractInterfaceConfig config) {
        //先检查接口的抽象配置
        //检查是否配置了接口本地实现类名
        checkName(LOCAL_KEY, config.getLocal());
        //检查是否配置了接口父类名
        checkName("stub", config.getStub());
        //检查是否配置了接口所属者
        checkMultiName("owner", config.getOwner());

        //检查是否配置了接口代理类生成方式
        checkExtension(ProxyFactory.class, PROXY_KEY, config.getProxy());
        //检查是否配置了接口集群
        checkExtension(Cluster.class, CLUSTER_KEY, config.getCluster());
        //检查是否配置了接口调用或者发布的过滤器
        checkMultiExtension(Filter.class, FILE_KEY, config.getFilter());
        //检查是否配置了接口调用或者发布的监听器
        checkMultiExtension(InvokerListener.class, LISTENER_KEY, config.getListener());
        //检查是否配置了接口所在的层
        checkNameHasSymbol(LAYER_KEY, config.getLayer());

        //再检查每一个接口的配置
        List<MethodConfig> methods = config.getMethods();
        if (CollectionUtils.isNotEmpty(methods)) {
            //检查单独接口的配置合法性
            methods.forEach(ConfigValidationUtils::validateMethodConfig);
        }
    }

    /**
     * 检查服务配置
     * @param config
     */
    public static void validateServiceConfig(ServiceConfig config) {
        //检查有没有配置服务版本
        checkKey(VERSION_KEY, config.getVersion());
        //检查有没有配置服务组
        checkKey(GROUP_KEY, config.getGroup());
        //检查有没有配置服务token
        checkName(TOKEN_KEY, config.getToken());
        //检查有没有配置服务地址
        checkPathName(PATH_KEY, config.getPath());
        //检查监听器
        checkMultiExtension(ExporterListener.class, "listener", config.getListener());
        //检查抽象接口配置和所有接口配置
        validateAbstractInterfaceConfig(config);

        //如果指定了注册中心，则检查所有注册中心配置
        List<RegistryConfig> registries = config.getRegistries();
        if (registries != null) {
            for (RegistryConfig registry : registries) {
                //检查单个注册中心配置
                validateRegistryConfig(registry);
            }
        }

        //如果指定了服务提供者协议，则检查所有的服务提供者协议配置
        List<ProtocolConfig> protocols = config.getProtocols();
        if (protocols != null) {
            for (ProtocolConfig protocol : protocols) {
                //检查单个服务提供者协议配置
                validateProtocolConfig(protocol);
            }
        }

        //如果指定了服务提供者，则检查服务提供者配置
        ProviderConfig providerConfig = config.getProvider();
        if (providerConfig != null) {
            validateProviderConfig(providerConfig);
        }
    }

    /**
     * 检查引用配置
     * @param config
     */
    public static void validateReferenceConfig(ReferenceConfig config) {
        //检查服务引用的监听器
        checkMultiExtension(InvokerListener.class, "listener", config.getListener());
        //检查服务引用的版本号
        checkKey(VERSION_KEY, config.getVersion());
        //检查服务引用的组
        checkKey(GROUP_KEY, config.getGroup());
        //检查服务引用的客户端
        checkName(CLIENT_KEY, config.getClient());

        //检查抽象接口配置和接口配置的合法性
        validateAbstractInterfaceConfig(config);

        //如果配置了注册中心
        List<RegistryConfig> registries = config.getRegistries();
        if (registries != null) {
            for (RegistryConfig registry : registries) {
                //检查注册中心配置
                validateRegistryConfig(registry);
            }
        }

        //检查服务引用配置（其实啥也没做）
        ConsumerConfig consumerConfig = config.getConsumer();
        if (consumerConfig != null) {
            validateConsumerConfig(consumerConfig);
        }
    }

    public static void validateConfigCenterConfig(ConfigCenterConfig config) {
        if (config != null) {
            checkParameterName(config.getParameters());
        }
    }

    public static void validateApplicationConfig(ApplicationConfig config) {
        if (config == null) {
            return;
        }

        if (!config.isValid()) {
            throw new IllegalStateException("No application config found or it's not a valid config! " +
                    "Please add <dubbo:application name=\"...\" /> to your spring config.");
        }

        // backward compatibility
        String wait = ConfigUtils.getProperty(SHUTDOWN_WAIT_KEY);
        if (wait != null && wait.trim().length() > 0) {
            System.setProperty(SHUTDOWN_WAIT_KEY, wait.trim());
        } else {
            wait = ConfigUtils.getProperty(SHUTDOWN_WAIT_SECONDS_KEY);
            if (wait != null && wait.trim().length() > 0) {
                System.setProperty(SHUTDOWN_WAIT_SECONDS_KEY, wait.trim());
            }
        }

        checkName(NAME, config.getName());
        checkMultiName(OWNER, config.getOwner());
        checkName(ORGANIZATION, config.getOrganization());
        checkName(ARCHITECTURE, config.getArchitecture());
        checkName(ENVIRONMENT, config.getEnvironment());
        checkParameterName(config.getParameters());
    }

    public static void validateModuleConfig(ModuleConfig config) {
        if (config != null) {
            checkName(NAME, config.getName());
            checkName(OWNER, config.getOwner());
            checkName(ORGANIZATION, config.getOrganization());
        }
    }

    public static void validateMetadataConfig(MetadataReportConfig metadataReportConfig) {
        if (metadataReportConfig == null) {
            return;
        }
    }

    public static void validateMetricsConfig(MetricsConfig metricsConfig) {
        if (metricsConfig == null) {
            return;
        }
    }

    public static void validateSslConfig(SslConfig sslConfig) {
        if (sslConfig == null) {
            return;
        }
    }

    public static void validateMonitorConfig(MonitorConfig config) {
        if (config != null) {
            if (!config.isValid()) {
                logger.info("There's no valid monitor config found, if you want to open monitor statistics for Dubbo, " +
                        "please make sure your monitor is configured properly.");
            }

            checkParameterName(config.getParameters());
        }
    }

    /**
     * 检查服务提供者协议配置
     * @param config
     */
    public static void validateProtocolConfig(ProtocolConfig config) {
        if (config != null) {
            //检查服务提供者的协议名
            String name = config.getName();
            checkName("name", name);
            //检查服务提供者的ip地址
            checkName(HOST_KEY, config.getHost());
            //检查服务提供者的上下文路径
            checkPathName("contextpath", config.getContextpath());


            //FIXME？？？？这是写了个啥
            //如果是dubbo协议，检查协议编解码器
            if (DUBBO_PROTOCOL.equals(name)) {
                checkMultiExtension(Codec.class, CODEC_KEY, config.getCodec());
            }
            //如果是dubbo协议，检查协议序列化器
            if (DUBBO_PROTOCOL.equals(name)) {
                checkMultiExtension(Serialization.class, SERIALIZATION_KEY, config.getSerialization());
            }
            //如果是dubbo协议，检查实现类
            if (DUBBO_PROTOCOL.equals(name)) {
                checkMultiExtension(Transporter.class, SERVER_KEY, config.getServer());
            }
            //如果是dubbo协议，检查客户端实现
            if (DUBBO_PROTOCOL.equals(name)) {
                checkMultiExtension(Transporter.class, CLIENT_KEY, config.getClient());
            }
            //检查服务提供者的支持telnet命令
            checkMultiExtension(TelnetHandler.class, TELNET, config.getTelnet());
            //检查服务提供者的状态
            checkMultiExtension(StatusChecker.class, "status", config.getStatus());
            //检查服务提供者的转换器
            checkExtension(Transporter.class, TRANSPORTER_KEY, config.getTransporter());
            //检查服务提供者的交换机
            checkExtension(Exchanger.class, EXCHANGER_KEY, config.getExchanger());
            //检查服务提供者的线程调度器
            checkExtension(Dispatcher.class, DISPATCHER_KEY, config.getDispatcher());
            //检查服务提供者的线程调度器
            checkExtension(Dispatcher.class, "dispather", config.getDispather());
            //检查服务提供者的线程池
            checkExtension(ThreadPool.class, THREADPOOL_KEY, config.getThreadpool());
        }
    }

    /**
     * 检查服务提供者配置
     * @param config
     */
    public static void validateProviderConfig(ProviderConfig config) {
        //检查服务提供者的上下文
        checkPathName(CONTEXTPATH_KEY, config.getContextpath());
        //检查服务提供者的线程池
        checkExtension(ThreadPool.class, THREADPOOL_KEY, config.getThreadpool());
        //检查服务提供者的支持telnet命令
        checkMultiExtension(TelnetHandler.class, TELNET, config.getTelnet());
        //检查服务提供者的状态
        checkMultiExtension(StatusChecker.class, STATUS_KEY, config.getStatus());
        //检查服务提供者的转换器
        checkExtension(Transporter.class, TRANSPORTER_KEY, config.getTransporter());
        //检查服务提供者的交换机
        checkExtension(Exchanger.class, EXCHANGER_KEY, config.getExchanger());
    }

    //FIXME 可以优化
    public static void validateConsumerConfig(ConsumerConfig config) {
        if (config == null) {
            return;
        }
    }

    /**
     * 检查注册中心配置
     * @param config
     */
    public static void validateRegistryConfig(RegistryConfig config) {
        //检查是否设定了配置中心的协议
        checkName(PROTOCOL_KEY, config.getProtocol());
        //检查是否设定了配置中心的用户名
        checkName(USERNAME_KEY, config.getUsername());
        //检查是否设定了配置中心的密码
        checkLength(PASSWORD_KEY, config.getPassword());
        //检查是否设定了配置中心的动态列表文件
        checkPathLength(FILE_KEY, config.getFile());
        //检查是否设定了配置中心的网络传输类型
        checkName(TRANSPORTER_KEY, config.getTransporter());
        //检查是否设定了配置中心的服务端
        checkName(SERVER_KEY, config.getServer());
        //检查是否设定了配置中心的客户端
        checkName(CLIENT_KEY, config.getClient());
        //检查是否设定了配置中心的自定义参数
        checkParameterName(config.getParameters());
    }

    /**
     * 检查方法配置的合法性
     * @param config
     */
    public static void validateMethodConfig(MethodConfig config) {
        //检查是否配置了接口的负载
        checkExtension(LoadBalance.class, LOADBALANCE_KEY, config.getLoadbalance());
        //检查是否配置了接口的参数名
        checkParameterName(config.getParameters());
        //检查是否配置了接口的方法名
        checkMethodName("name", config.getName());
        //检查是否配置了接口的mock方法
        String mock = config.getMock();
        if (StringUtils.isNotEmpty(mock)) {
            //如果是简单mock，检查mock语句的长度
            if (mock.startsWith(RETURN_PREFIX) || mock.startsWith(THROW_PREFIX + " ")) {
                checkLength(MOCK_KEY, mock);
            //模板模式
            } else if (mock.startsWith(FAIL_PREFIX) || mock.startsWith(FORCE_PREFIX)) {
                checkNameHasSymbol(MOCK_KEY, mock);
            //其他模式
            } else {
                checkName(MOCK_KEY, mock);
            }
        }
    }

    private static String extractRegistryType(URL url) {
        return isServiceDiscoveryRegistryType(url) ? SERVICE_REGISTRY_PROTOCOL : REGISTRY_PROTOCOL;
    }

    public static void checkExtension(Class<?> type, String property, String value) {
        checkName(property, value);
        if (StringUtils.isNotEmpty(value)
                && !ExtensionLoader.getExtensionLoader(type).hasExtension(value)) {
            throw new IllegalStateException("No such extension " + value + " for " + property + "/" + type.getName());
        }
    }

    /**
     * 检查是否有扩展器的名字是"value"（如果有需要特殊处理）
     * Check whether there is a <code>Extension</code> who's name (property) is <code>value</code> (special treatment is
     * required)
     *
     * @param type     The Extension type
     * @param property The extension key
     * @param value    The Extension name
     */
    public static void checkMultiExtension(Class<?> type, String property, String value) {
        //检查有没有配置listener监听器
        checkMultiName(property, value);
        //如果配置了监听器
        if (StringUtils.isNotEmpty(value)) {
            //用","进行分割，循环处理监听器
            String[] values = value.split("\\s*[,]+\\s*");
            for (String v : values) {
                //如果是"-"开头就去掉这个开头
                if (v.startsWith(REMOVE_VALUE_PREFIX)) {
                    v = v.substring(1);
                }
                //如果是default就跳过处理
                if (DEFAULT_KEY.equals(v)) {
                    continue;
                }
                //检查是否能够反射到这个监听器，无法获取则抛出异常
                if (!ExtensionLoader.getExtensionLoader(type).hasExtension(v)) {
                    throw new IllegalStateException("No such extension " + v + " for " + property + "/" + type.getName());
                }
            }
        }
    }

    public static void checkLength(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, null);
    }

    public static void checkPathLength(String property, String value) {
        checkProperty(property, value, MAX_PATH_LENGTH, null);
    }

    public static void checkName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME);
    }

    public static void checkNameHasSymbol(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME_HAS_SYMBOL);
    }

    public static void checkKey(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_KEY);
    }

    public static void checkMultiName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_MULTI_NAME);
    }

    public static void checkPathName(String property, String value) {
        checkProperty(property, value, MAX_PATH_LENGTH, PATTERN_PATH);
    }

    public static void checkMethodName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_METHOD_NAME);
    }

    public static void checkParameterName(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return;
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            checkNameHasSymbol(entry.getKey(), entry.getValue());
        }
    }

    public static void checkProperty(String property, String value, int maxlength, Pattern pattern) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (value.length() > maxlength) {
            throw new IllegalStateException("Invalid " + property + "=\"" + value + "\" is longer than " + maxlength);
        }
        if (pattern != null) {
            Matcher matcher = pattern.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalStateException("Invalid " + property + "=\"" + value + "\" contains illegal " +
                        "character, only digit, letter, '-', '_' or '.' is legal.");
            }
        }
    }

}
