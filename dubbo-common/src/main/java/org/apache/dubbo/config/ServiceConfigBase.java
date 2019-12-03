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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;

/**
 * 服务配置
 * ServiceConfig
 *
 * @export
 */
public abstract class ServiceConfigBase<T> extends AbstractServiceConfig {

    private static final long serialVersionUID = 3033787999037024738L;

    /**
     * 暴露服务的接口名称
     * The interface name of the exported service
     */
    protected String interfaceName;

    /**
     * 暴露服务的接口类
     * The interface class of the exported service
     */
    protected Class<?> interfaceClass;

    /**
     * 接口实现的引用
     * The reference of the interface implementation
     */
    protected T ref;

    /**
     * 服务名称
     * The service name
     */
    protected String path;

    /**
     * 提供程序配置
     * The provider configuration
     */
    protected ProviderConfig provider;

    /**
     * 提供者ID
     * The providerIds
     */
    protected String providerIds;

    /**
     * 是否为泛化服务
     * whether it is a GenericService
     */
    protected volatile String generic;

    /**
     * 与服务级别相关的数据
     */
    protected ServiceMetadata serviceMetadata;

    public ServiceConfigBase() {
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
    }

    public ServiceConfigBase(Service service) {
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
        appendAnnotation(Service.class, service);
        setMethods(MethodConfig.constructMethodConfig(service.methods()));
    }

    public void exported() {

    }

    @Deprecated
    private static List<ProtocolConfig> convertProviderToProtocol(List<ProviderConfig> providers) {
        if (CollectionUtils.isEmpty(providers)) {
            return null;
        }
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>(providers.size());
        for (ProviderConfig provider : providers) {
            protocols.add(convertProviderToProtocol(provider));
        }
        return protocols;
    }

    @Deprecated
    private static List<ProviderConfig> convertProtocolToProvider(List<ProtocolConfig> protocols) {
        if (CollectionUtils.isEmpty(protocols)) {
            return null;
        }
        List<ProviderConfig> providers = new ArrayList<ProviderConfig>(protocols.size());
        for (ProtocolConfig provider : protocols) {
            providers.add(convertProtocolToProvider(provider));
        }
        return providers;
    }

    @Deprecated
    private static ProtocolConfig convertProviderToProtocol(ProviderConfig provider) {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName(provider.getProtocol().getName());
        protocol.setServer(provider.getServer());
        protocol.setClient(provider.getClient());
        protocol.setCodec(provider.getCodec());
        protocol.setHost(provider.getHost());
        protocol.setPort(provider.getPort());
        protocol.setPath(provider.getPath());
        protocol.setPayload(provider.getPayload());
        protocol.setThreads(provider.getThreads());
        protocol.setParameters(provider.getParameters());
        return protocol;
    }

    @Deprecated
    private static ProviderConfig convertProtocolToProvider(ProtocolConfig protocol) {
        ProviderConfig provider = new ProviderConfig();
        provider.setProtocol(protocol);
        provider.setServer(protocol.getServer());
        provider.setClient(protocol.getClient());
        provider.setCodec(protocol.getCodec());
        provider.setHost(protocol.getHost());
        provider.setPort(protocol.getPort());
        provider.setPath(protocol.getPath());
        provider.setPayload(protocol.getPayload());
        provider.setThreads(protocol.getThreads());
        provider.setParameters(protocol.getParameters());
        return provider;
    }

    public boolean shouldExport() {
        //根据配置决定是否暴露服务，默认需要暴露
        Boolean export = getExport();
        // default value is true
        return export == null ? true : export;
    }

    @Override
    public Boolean getExport() {
        return (export == null && provider != null) ? provider.getExport() : export;
    }

    public boolean shouldDelay() {
        Integer delay = getDelay();
        return delay != null && delay > 0;
    }

    @Override
    public Integer getDelay() {
        return (delay == null && provider != null) ? provider.getDelay() : delay;
    }

    /**
     * 检查引用的服务
     */
    public void checkRef() {
        // 引用不应为空
        // reference should not be null, and is the implementation of the given interface
        if (ref == null) {
            throw new IllegalStateException("ref not allow null!");
        }
        // 引用是给定接口的实现
        if (!interfaceClass.isInstance(ref)) {
            throw new IllegalStateException("The class "
                    + ref.getClass().getName() + " unimplemented interface "
                    + interfaceClass + "!");
        }
    }

    public Optional<String> getContextPath(ProtocolConfig protocolConfig) {
        String contextPath = protocolConfig.getContextpath();
        //如果服务提供者协议配置没有提供上下文并且提供了服务提供者，对上下文进行覆盖
        if (StringUtils.isEmpty(contextPath) && provider != null) {
            contextPath = provider.getContextpath();
        }
        return Optional.ofNullable(contextPath);
    }

    protected Class getServiceClass(T ref) {
        return ref.getClass();
    }

    /**
     * 进一步填充本jvm的配置
     */
    public void completeCompoundConfigs() {
        //如果这是一个服务提供者
        if (provider != null) {
            //没有设置应用配置，从服务提供者配置中获取应用配置
            if (application == null) {
                setApplication(provider.getApplication());
            }
            //没有设置模块配置，从服务提供者配置中获取模块配置
            if (module == null) {
                setModule(provider.getModule());
            }
            //没有设置注册中心配置，从服务提供者配置中获取注册中心配置
            if (registries == null) {
                setRegistries(provider.getRegistries());
            }
            //没有设置监控中心配置，从服务提供者配置中获取监控中心配置
            if (monitor == null) {
                setMonitor(provider.getMonitor());
            }
            //没有设置协议配置，从服务提供者配置中获取协议配置
            if (protocols == null) {
                setProtocols(provider.getProtocols());
            }
            //没有设置配置中心配置，从服务提供者配置中获取配置中心配置
            if (configCenter == null) {
                setConfigCenter(provider.getConfigCenter());
            }
        }
        //如果已经指定了模块配置
        if (module != null) {
            //没有设置注册中心配置，从模块配置中获取注册中心配置
            if (registries == null) {
                setRegistries(module.getRegistries());
            }
            //没有设置监控中心配置，从模块配置中获取监控中心配置
            if (monitor == null) {
                setMonitor(module.getMonitor());
            }
        }
        //如果已经指定了应用配置
        if (application != null) {
            //没有设置注册中心配置，从应用配置中获取注册中心配置
            if (registries == null) {
                setRegistries(application.getRegistries());
            }
            //没有设置监控中心配置，从应用配置中获取监控中心配置
            if (monitor == null) {
                setMonitor(application.getMonitor());
            }
        }
    }

    /**
     * 检查是否存在默认的服务提供者配置并决定是否创建
     */
    public void checkDefault() {
        createProviderIfAbsent();
    }

    /**
     * 检查是否存在默认的服务提供者配置并决定是否创建
     */
    private void createProviderIfAbsent() {
        //如果不是服务提供者则返回
        if (provider != null) {
            return;
        }
        setProvider(
                //从配置管理中获取，获取不到则初始化一个
                ApplicationModel.getConfigManager()
                        .getDefaultProvider()
                        .orElseGet(() -> {
                            ProviderConfig providerConfig = new ProviderConfig();
                            providerConfig.refresh();
                            return providerConfig;
                        })
        );
    }

    /**
     * 检查协议
     */
    public void checkProtocol() {
        //如果没有指定协议但是指定了服务提供者的配置
        if (CollectionUtils.isEmpty(protocols) && provider != null) {
            //从服务提供者里获取指定的协议
            setProtocols(provider.getProtocols());
        }
        //等级或更新所有的协议
        convertProtocolIdsToProtocols();
    }

    private void convertProtocolIdsToProtocols() {
        // 通过服务提供者配置进一步填充协议
        computeValidProtocolIds();
        //如果还是没有发现指定的协议
        if (StringUtils.isEmpty(protocolIds)) {
            if (CollectionUtils.isEmpty(protocols)) {
                //使用默认的协议
                List<ProtocolConfig> protocolConfigs = ApplicationModel.getConfigManager().getDefaultProtocols();
                //如果默认的协议都没有
                if (protocolConfigs.isEmpty()) {
                    //通过dubbo创建一个默认的协议对象并初始化
                    protocolConfigs = new ArrayList<>(1);
                    ProtocolConfig protocolConfig = new ProtocolConfig();
                    protocolConfig.refresh();
                    protocolConfigs.add(protocolConfig);
                }
                setProtocols(protocolConfigs);
            }
        } else {
            //将指定的协议通过分隔符分开，进行逐个处理
            String[] arr = COMMA_SPLIT_PATTERN.split(protocolIds);
            List<ProtocolConfig> tmpProtocols = CollectionUtils.isNotEmpty(protocols) ? protocols : new ArrayList<>();
            Arrays.stream(arr).forEach(id -> {
                //如果该协议还没有被注册
                if (tmpProtocols.stream().noneMatch(prot -> prot.getId().equals(id))) {
                    //从配置管理器里获取对应的协议配置
                    Optional<ProtocolConfig> globalProtocol = ApplicationModel.getConfigManager().getProtocol(id);
                    if (globalProtocol.isPresent()) {
                        //存在就直接登记
                        tmpProtocols.add(globalProtocol.get());
                    } else {
                        //不存在就创建一个进行登记
                        ProtocolConfig protocolConfig = new ProtocolConfig();
                        protocolConfig.setId(id);
                        protocolConfig.refresh();
                        tmpProtocols.add(protocolConfig);
                    }
                }
            });
            //如果最终注册协议的数量大于指定的数量，则抛出异常
            if (tmpProtocols.size() > arr.length) {
                throw new IllegalStateException("Too much protocols found, the protocols comply to this service are :" + protocolIds + " but got " + protocols
                        .size() + " registries!");
            }
            //注册所有的协议
            setProtocols(tmpProtocols);
        }
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (ref instanceof GenericService) {
            return GenericService.class;
        }
        try {
            if (interfaceName != null && interfaceName.length() > 0) {
                this.interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
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
    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? null : interfaceClass.getName());
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (StringUtils.isEmpty(id)) {
            id = interfaceName;
        }
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }

    @Parameter(excluded = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ProviderConfig getProvider() {
        return provider;
    }

    public void setProvider(ProviderConfig provider) {
        ApplicationModel.getConfigManager().addProvider(provider);
        this.provider = provider;
    }

    @Parameter(excluded = true)
    public String getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(String providerIds) {
        this.providerIds = providerIds;
    }

    public String getGeneric() {
        return generic;
    }

    public void setGeneric(String generic) {
        if (StringUtils.isEmpty(generic)) {
            return;
        }
        if (ProtocolUtils.isValidGenericValue(generic)) {
            this.generic = generic;
        } else {
            throw new IllegalArgumentException("Unsupported generic type " + generic);
        }
    }

    @Override
    public void setMock(Boolean mock) {
        throw new IllegalArgumentException("mock doesn't support on provider side");
    }

    @Override
    public void setMock(String mock) {
        throw new IllegalArgumentException("mock doesn't support on provider side");
    }

    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }

    /**
     * @deprecated Replace to getProtocols()
     */
    @Deprecated
    public List<ProviderConfig> getProviders() {
        return convertProtocolToProvider(protocols);
    }

    /**
     * @deprecated Replace to setProtocols()
     */
    @Deprecated
    public void setProviders(List<ProviderConfig> providers) {
        this.protocols = convertProviderToProtocol(providers);
    }

    @Override
    @Parameter(excluded = true)
    public String getPrefix() {
        return DUBBO + ".service." + interfaceName;
    }

    /**
     * 通过接口名、组、版本号拼接URL地址
     * @return
     */
    @Parameter(excluded = true)
    public String getUniqueServiceName() {
        return URL.buildKey(interfaceName, group, version);
    }

    private void computeValidProtocolIds() {
        //如果服务没有指定协议
        if (StringUtils.isEmpty(getProtocolIds())) {
            // 从提供者配置中进行获取并覆盖空值
            if (getProvider() != null && StringUtils.isNotEmpty(getProvider().getProtocolIds())) {
                setProtocolIds(getProvider().getProtocolIds());
            }
        }
    }

    @Override
    protected void computeValidRegistryIds() {
        super.computeValidRegistryIds();
        if (StringUtils.isEmpty(getRegistryIds())) {
            if (getProvider() != null && StringUtils.isNotEmpty(getProvider().getRegistryIds())) {
                setRegistryIds(getProvider().getRegistryIds());
            }
        }
    }

    public abstract void export();

    public abstract void unexport();

    public abstract boolean isExported();

    public abstract boolean isUnexported();

}