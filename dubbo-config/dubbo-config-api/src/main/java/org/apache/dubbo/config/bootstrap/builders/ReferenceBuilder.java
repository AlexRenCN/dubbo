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

import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ReferenceConfigBase;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a builder for build {@link ReferenceConfigBase}.
 *
 * @since 2.7
 */
public class ReferenceBuilder<T> extends AbstractReferenceBuilder<ReferenceConfig, ReferenceBuilder<T>> {
    /**
     * 引用服务的接口名称
     * The interface name of the reference service
     */
    private String interfaceName;

    /**
     * 引用服务的接口类
     * The interface class of the reference service
     */
    private Class<?> interfaceClass;

    /**
     * 客户端类型
     * client type
     */
    private String client;

    /**
     * 点对点调用的URL
     * The url for peer-to-peer invocation
     */
    private String url;

    /**
     * 方法配置
     * The method configs
     */
    private List<MethodConfig> methods;

    /**
     * 使用者配置（默认）
     * The consumer config (default)
     */
    private ConsumerConfig consumer;

    /**
     * 只调用指定协议的服务提供程序，而忽略其他协议。
     * Only the service provider of the specified protocol is invoked, and other protocols are ignored.
     */
    private String protocol;

    public static ReferenceBuilder newBuilder() {
        return new ReferenceBuilder();
    }

    public ReferenceBuilder<T> id(String id) {
        return super.id(id);
    }

    public ReferenceBuilder<T> interfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
        return getThis();
    }

    public ReferenceBuilder<T> interfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return getThis();
    }

    public ReferenceBuilder<T> client(String client) {
        this.client = client;
        return getThis();
    }

    public ReferenceBuilder<T> url(String url) {
        this.url = url;
        return getThis();
    }

    public ReferenceBuilder<T> addMethods(List<MethodConfig> methods) {
        if (this.methods == null) {
            this.methods = new ArrayList<>();
        }
        this.methods.addAll(methods);
        return getThis();
    }

    public ReferenceBuilder<T> addMethod(MethodConfig method) {
        if (this.methods == null) {
            this.methods = new ArrayList<>();
        }
        this.methods.add(method);
        return getThis();
    }

    public ReferenceBuilder<T> consumer(ConsumerConfig consumer) {
        this.consumer = consumer;
        return getThis();
    }

    public ReferenceBuilder<T> protocol(String protocol) {
        this.protocol = protocol;
        return getThis();
    }

    public ReferenceConfig<T> build() {
        ReferenceConfig<T> reference = new ReferenceConfig<>();
        super.build(reference);

        reference.setInterface(interfaceName);
        if (interfaceClass != null) {
            reference.setInterface(interfaceClass);
        }
        reference.setClient(client);
        reference.setUrl(url);
        reference.setMethods(methods);
        reference.setConsumer(consumer);
        reference.setProtocol(protocol);

        return reference;
    }

    @Override
    protected ReferenceBuilder<T> getThis() {
        return this;
    }
}
