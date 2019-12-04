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
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;

/**
 * 引用服务配置
 * ReferenceConfig
 *
 * @export
 */
public abstract class ReferenceConfigBase<T> extends AbstractReferenceConfig {

    private static final long serialVersionUID = -5864351140409987595L;

    /**
     * 引用服务的接口名称
     * The interface name of the reference service
     */
    protected String interfaceName;

    /**
     * 引用服务的接口类
     * The interface class of the reference service
     */
    protected Class<?> interfaceClass;

    /**
     * 客户端类型
     * client type
     */
    protected String client;

    /**
     * 点对点调用的URL
     * The url for peer-to-peer invocation
     */
    protected String url;

    /**
     * 使用者配置（默认）
     * The consumer config (default)
     */
    protected ConsumerConfig consumer;

    /**
     * 只调用指定协议的服务提供程序，而忽略其他协议。
     * Only the service provider of the specified protocol is invoked, and other protocols are ignored.
     */
    protected String protocol;

    /**
     * 与服务级别相关的数据
     */
    protected ServiceMetadata serviceMetadata;

    public ReferenceConfigBase() {
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
    }

    public ReferenceConfigBase(Reference reference) {
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
        appendAnnotation(Reference.class, reference);
        setMethods(MethodConfig.constructMethodConfig(reference.methods()));
    }

    /**
     * 是否需要检查
     * @return
     */
    public boolean shouldCheck() {
        //先获取引用配置是否检查
        Boolean shouldCheck = isCheck();
        //再获取客户端里的是否检查
        if (shouldCheck == null && getConsumer() != null) {
            shouldCheck = getConsumer().isCheck();
        }
        //默认需要检查
        if (shouldCheck == null) {
            // default true
            shouldCheck = true;
        }
        return shouldCheck;
    }

    public boolean shouldInit() {
        Boolean shouldInit = isInit();
        if (shouldInit == null && getConsumer() != null) {
            shouldInit = getConsumer().isInit();
        }
        if (shouldInit == null) {
            // default is true, spring will still init lazily by setting init's default value to false,
            // the def default setting happens in {@link ReferenceBean#afterPropertiesSet}.
            return true;
        }
        return shouldInit;
    }

    public void checkDefault() {
        //如果有消费者，返回
        if (consumer != null) {
            return;
        }
        //放置消费者
        setConsumer(ApplicationModel.getConfigManager().getDefaultConsumer().orElseGet(() -> {
            ConsumerConfig consumerConfig = new ConsumerConfig();
            consumerConfig.refresh();
            return consumerConfig;
        }));
    }

    /**
     * 查看配置为空的部分，并尝试从其他配置中获取
     */
    public void completeCompoundConfigs() {
        //从消费者配置里填充应用配置、模块配置、注册中心配置、监控中心配置
        if (consumer != null) {
            if (application == null) {
                setApplication(consumer.getApplication());
            }
            if (module == null) {
                setModule(consumer.getModule());
            }
            if (registries == null) {
                setRegistries(consumer.getRegistries());
            }
            if (monitor == null) {
                setMonitor(consumer.getMonitor());
            }
        }
        //从模块配置里填充注册中心配置、监控中心配置
        if (module != null) {
            if (registries == null) {
                setRegistries(module.getRegistries());
            }
            if (monitor == null) {
                setMonitor(module.getMonitor());
            }
        }
        //从应用配置里填充注册中心配置、监控中心配置
        if (application != null) {
            if (registries == null) {
                setRegistries(application.getRegistries());
            }
            if (monitor == null) {
                setMonitor(application.getMonitor());
            }
        }
    }

    /**
     * 获取实际需要引用的接口类
     * @return
     */
    public Class<?> getActualInterface() {
        Class actualInterface = interfaceClass;
        if (interfaceClass == GenericService.class) {
            try {
                actualInterface = Class.forName(interfaceName);
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        return actualInterface;
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (ProtocolUtils.isGeneric(getGeneric())
                || (getConsumer() != null && ProtocolUtils.isGeneric(getConsumer().getGeneric()))) {
            return GenericService.class;
        }
        try {
            if (interfaceName != null && interfaceName.length() > 0) {
                interfaceClass = Class.forName(interfaceName, true, ClassUtils.getClassLoader());
            }
        } catch (ClassNotFoundException t) {
            throw new IllegalStateException(t.getMessage(), t);
        }

        return interfaceClass;
    }

    /**
     * @param interfaceClass
     * @see #setInterface(Class)
     * @deprecated
     */
    @Deprecated
    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (StringUtils.isEmpty(id)) {
            id = interfaceName;
        }
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? null : interfaceClass.getName());
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    @Parameter(excluded = true)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ConsumerConfig getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerConfig consumer) {
        this.consumer = consumer;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }

    @Override
    @Parameter(excluded = true)
    public String getPrefix() {
        return DUBBO + ".reference." + interfaceName;
    }

    /**
     * 解析映射文件，映射文件的优先级高于<dubbo:reference>
     */
    public void resolveFile() {
        String resolve = System.getProperty(interfaceName);
        String resolveFile = null;
        if (StringUtils.isEmpty(resolve)) {
            resolveFile = System.getProperty("dubbo.resolve.file");
            //没有映射文件就用dubbo默认的
            if (StringUtils.isEmpty(resolveFile)) {
                //获取映射文件
                File userResolveFile = new File(new File(System.getProperty("user.home")), "dubbo-resolve.properties");
                //如果文件存在
                if (userResolveFile.exists()) {
                    //解析映射文件的绝对路径名
                    resolveFile = userResolveFile.getAbsolutePath();
                }
            }
            if (resolveFile != null && resolveFile.length() > 0) {
                //将映射文件读取成流，解析成配置
                Properties properties = new Properties();
                try (FileInputStream fis = new FileInputStream(new File(resolveFile))) {
                    //添加为配置
                    properties.load(fis);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to load " + resolveFile + ", cause: " + e.getMessage(), e);
                }

                //获取需要引入的接口配置
                resolve = properties.getProperty(interfaceName);
            }
        }
        //如果映射文件里有引入的接口配置，覆盖URL配置
        if (resolve != null && resolve.length() > 0) {
            url = resolve;
            if (logger.isWarnEnabled()) {
                if (resolveFile != null) {
                    logger.warn("Using default dubbo resolve file " + resolveFile + " replace " + interfaceName + "" + resolve + " to p2p invoke remote service.");
                } else {
                    logger.warn("Using -D" + interfaceName + "=" + resolve + " to p2p invoke remote service.");
                }
            }
        }
    }

    @Override
    protected void computeValidRegistryIds() {
        super.computeValidRegistryIds();
        if (StringUtils.isEmpty(getRegistryIds())) {
            if (getConsumer() != null && StringUtils.isNotEmpty(getConsumer().getRegistryIds())) {
                setRegistryIds(getConsumer().getRegistryIds());
            }
        }
    }

    @Parameter(excluded = true)
    public String getUniqueServiceName() {
        return URL.buildKey(interfaceName, group, version);
    }

    /**
     * TODO dubbo服务引用的入口
     * @return
     */
    public abstract T get();

    public abstract void destroy();


}
