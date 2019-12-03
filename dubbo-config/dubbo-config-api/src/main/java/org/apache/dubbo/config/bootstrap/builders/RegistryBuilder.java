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

import org.apache.dubbo.config.RegistryConfig;

import java.util.Map;

/**
 * This is a builder for build {@link RegistryConfig}.
 *
 * @since 2.7
 */
public class RegistryBuilder extends AbstractBuilder<RegistryConfig, RegistryBuilder> {
    /**
     * 注册中心地址
     * Register center address
     */
    private String address;

    /**
     * 注册中心用户名
     * Username to login register center
     */
    private String username;

    /**
     * 注册中心密码
     * Password to login register center
     */
    private String password;

    /**
     * 注册中心端口
     * Default port for register center
     */
    private Integer port;

    /**
     * 注册中心协议
     * Protocol for register center
     */
    private String protocol;

    /**
     * 网络传输类型
     * Network transmission type
     */
    private String transporter;

    /**
     * 服务端
     */
    private String server;

    /**
     * 客户端
     */
    private String client;

    /**
     * 集群
     */
    private String cluster;

    /**
     * 服务注册所在的组
     * The group the services registry in
     */
    private String group;

    /**
     * 版本号
     */
    private String version;

    /**
     * 注册中心的请求超时时间（毫秒）
     * Request timeout in milliseconds for register center
     */
    private Integer timeout;

    /**
     * 注册中心的会话超时时间（毫秒）
     * Session timeout in milliseconds for register center
     */
    private Integer session;

    /**
     * 用于保存注册中心动态列表的文件
     * File for saving register center dynamic list
     */
    private String file;

    /**
     * 停机前等待时间
     * Wait time before stop
     */
    private Integer wait;

    /**
     * 启动时是否检查注册中心是否可用
     * Whether to check if register center is available when boot up
     */
    private Boolean check;

    /**
     * 是否允许动态服务在注册中心注册
     * Whether to allow dynamic service to register on the register center
     */
    private Boolean dynamic;

    /**
     * 是否在注册中心登记服务
     * Whether to export service on the register center
     */
    private Boolean register;

    /**
     * 是否允许在注册中心订阅服务
     * Whether allow to subscribe service on the register center
     */
    private Boolean subscribe;

    /**
     * 自定义参数
     * The customized parameters
     */
    private Map<String, String> parameters;

    /**
     * 是否默认
     * Whether it's default
     */
    private Boolean isDefault;

    /**
     * 简化注册表。对提供者和使用者都有用
     * Simple the registry. both useful for provider and consumer
     *
     * @since 2.7.0
     */
    private Boolean simplified;
    /**
     * 简化注册表后，应单独添加一些参数。只为提供者使用。
     * After simplify the registry, should add some paramter individually. just for provider.
     * <p>
     * such as: extra-keys = A,b,c,d
     *
     * @since 2.7.0
     */
    private String extraKeys;

    /**
     * 地址是否作为配置中心
     * the address work as config center or not
     */
    private Boolean useAsConfigCenter;

    /**
     * 地址是否作为远程元数据中心
     * the address work as remote metadata center or not
     */
    private Boolean useAsMetadataCenter;

    /**
     * 此注册表接受的rpc协议列表，例如，“dubbo，rest”
     * list of rpc protocols accepted by this registry, for example, "dubbo,rest"
     */
    private String accepts;

    /**
     * 首选注册表
     * 如果设置为true，则始终首先使用此注册表，这在订阅多个注册表时很有用
     * Always use this registry first if set to true, useful when subscribe to multiple registries
     */
    private Boolean preferred;

    /**
     * 注册表权重
     * 影响注册表之间的流量分布，在订阅多个注册表时很有用。
     * 仅当未指定首选注册表时才生效。
     * Affects traffic distribution among registries, useful when subscribe to multiple registries
     * Take effect only when no preferred registry is specified.
     */
    private Integer weight;

    public static RegistryBuilder newBuilder() {
        return new RegistryBuilder();
    }

    public RegistryBuilder id(String id) {
        return super.id(id);
    }

    public RegistryBuilder address(String address) {
        this.address = address;
        return getThis();
    }

    public RegistryBuilder username(String username) {
        this.username = username;
        return getThis();
    }

    public RegistryBuilder password(String password) {
        this.password = password;
        return getThis();
    }

    public RegistryBuilder port(Integer port) {
        this.port = port;
        return getThis();
    }

    public RegistryBuilder protocol(String protocol) {
        this.protocol = protocol;
        return getThis();
    }

    public RegistryBuilder transporter(String transporter) {
        this.transporter = transporter;
        return getThis();
    }

    /**
     * @param transport
     * @see #transporter(String)
     * @deprecated
     */
    @Deprecated
    public RegistryBuilder transport(String transport) {
        this.transporter = transport;
        return getThis();
    }

    public RegistryBuilder server(String server) {
        this.server = server;
        return getThis();
    }

    public RegistryBuilder client(String client) {
        this.client = client;
        return getThis();
    }

    public RegistryBuilder cluster(String cluster) {
        this.cluster = cluster;
        return getThis();
    }

    public RegistryBuilder group(String group) {
        this.group = group;
        return getThis();
    }

    public RegistryBuilder version(String version) {
        this.version = version;
        return getThis();
    }

    public RegistryBuilder timeout(Integer timeout) {
        this.timeout = timeout;
        return getThis();
    }

    public RegistryBuilder session(Integer session) {
        this.session = session;
        return getThis();
    }

    public RegistryBuilder file(String file) {
        this.file = file;
        return getThis();
    }

    /**
     * @param wait
     * @see ProviderBuilder#wait(Integer)
     * @deprecated
     */
    @Deprecated
    public RegistryBuilder wait(Integer wait) {
        this.wait = wait;
        return getThis();
    }

    public RegistryBuilder isCheck(Boolean check) {
        this.check = check;
        return getThis();
    }

    public RegistryBuilder isDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
        return getThis();
    }

    public RegistryBuilder register(Boolean register) {
        this.register = register;
        return getThis();
    }

    public RegistryBuilder subscribe(Boolean subscribe) {
        this.subscribe = subscribe;
        return getThis();
    }

    public RegistryBuilder appendParameter(String key, String value) {
        this.parameters = appendParameter(parameters, key, value);
        return getThis();
    }

    public RegistryBuilder appendParameters(Map<String, String> appendParameters) {
        this.parameters = appendParameters(parameters, appendParameters);
        return getThis();
    }

    public RegistryBuilder isDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return getThis();
    }

    public RegistryBuilder simplified(Boolean simplified) {
        this.simplified = simplified;
        return getThis();
    }

    public RegistryBuilder extraKeys(String extraKeys) {
        this.extraKeys = extraKeys;
        return getThis();
    }

    public RegistryBuilder useAsConfigCenter(Boolean useAsConfigCenter) {
        this.useAsConfigCenter = useAsConfigCenter;
        return getThis();
    }

    public RegistryBuilder useAsMetadataCenter(Boolean useAsMetadataCenter) {
        this.useAsMetadataCenter = useAsMetadataCenter;
        return getThis();
    }

    public RegistryBuilder preferred(Boolean preferred) {
        this.preferred = preferred;
        return getThis();
    }

    public RegistryBuilder accepts(String accepts) {
        this.accepts = accepts;
        return getThis();
    }

    public RegistryBuilder weight(Integer weight) {
        this.weight = weight;
        return getThis();
    }

    public RegistryConfig build() {
        RegistryConfig registry = new RegistryConfig();
        super.build(registry);

        registry.setAddress(address);
        registry.setCheck(check);
        registry.setClient(client);
        registry.setCluster(cluster);
        registry.setDefault(isDefault);
        registry.setDynamic(dynamic);
        registry.setExtraKeys(extraKeys);
        registry.setFile(file);
        registry.setGroup(group);
        registry.setParameters(parameters);
        registry.setPassword(password);
        registry.setPort(port);
        registry.setProtocol(protocol);
        registry.setRegister(register);
        registry.setServer(server);
        registry.setSession(session);
        registry.setSimplified(simplified);
        registry.setSubscribe(subscribe);
        registry.setTimeout(timeout);
        registry.setTransporter(transporter);
        registry.setUsername(username);
        registry.setVersion(version);
        registry.setWait(wait);
        registry.setUseAsConfigCenter(useAsConfigCenter);
        registry.setUseAsMetadataCenter(useAsMetadataCenter);
        registry.setAccepts(accepts);
        registry.setPreferred(preferred);
        registry.setWeight(weight);

        return registry;
    }

    @Override
    protected RegistryBuilder getThis() {
        return this;
    }
}
