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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceConfigBase;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a builder for build {@link ServiceConfigBase}.
 *
 * @since 2.7
 */
public class ServiceBuilder<U> extends AbstractServiceBuilder<ServiceConfig, ServiceBuilder<U>> {
    /**
     * 暴露服务的接口名称
     * The interface name of the exported service
     */
    private String interfaceName;

    /**
     * 暴露服务的接口类
     * The interface class of the exported service
     */
    private Class<?> interfaceClass;

    /**
     * 接口实现的引用
     * The reference of the interface implementation
     */
    private U ref;

    /**
     * 服务名称
     * The service name
     */
    private String path;

    /**
     * 方法配置
     * The method configuration
     */
    private List<MethodConfig> methods;

    /**
     * 提供程序配置
     * The provider configuration
     */
    private ProviderConfig provider;

    /**
     * 提供者ID
     * The providerIds
     */
    private String providerIds;
    /**
     * 是否为一般服务
     * whether it is a GenericService
     */
    private String generic;

    public static ServiceBuilder newBuilder() {
        return new ServiceBuilder();
    }

    public ServiceBuilder id(String id) {
        return super.id(id);
    }

    public ServiceBuilder<U> interfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
        return getThis();
    }

    public ServiceBuilder<U> interfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return getThis();
    }

    public ServiceBuilder<U> ref(U ref) {
        this.ref = ref;
        return getThis();
    }

    public ServiceBuilder<U> path(String path) {
        this.path = path;
        return getThis();
    }

    public ServiceBuilder<U> addMethod(MethodConfig method) {
        if (this.methods == null) {
            this.methods = new ArrayList<>();
        }
        this.methods.add(method);
        return getThis();
    }

    public ServiceBuilder<U> addMethods(List<? extends MethodConfig> methods) {
        if (this.methods == null) {
            this.methods = new ArrayList<>();
        }
        this.methods.addAll(methods);
        return getThis();
    }

    public ServiceBuilder<U> provider(ProviderConfig provider) {
        this.provider = provider;
        return getThis();
    }

    public ServiceBuilder<U> providerIds(String providerIds) {
        this.providerIds = providerIds;
        return getThis();
    }

    public ServiceBuilder<U> generic(String generic) {
        this.generic = generic;
        return getThis();
    }

    @Override
    public ServiceBuilder<U> mock(String mock) {
        throw new IllegalArgumentException("mock doesn't support on provider side");
    }

    @Override
    public ServiceBuilder<U> mock(Boolean mock) {
        throw new IllegalArgumentException("mock doesn't support on provider side");
    }

    public ServiceConfig<U> build() {
        ServiceConfig<U> serviceConfig = new ServiceConfig<>();
        super.build(serviceConfig);

        serviceConfig.setInterface(interfaceName);
        serviceConfig.setInterface(interfaceClass);
        serviceConfig.setRef(ref);
        serviceConfig.setPath(path);
        serviceConfig.setMethods(methods);
        serviceConfig.setProvider(provider);
        serviceConfig.setProviderIds(providerIds);
        serviceConfig.setGeneric(generic);

        return serviceConfig;
    }

    @Override
    protected ServiceBuilder<U> getThis() {
        return this;
    }
}
