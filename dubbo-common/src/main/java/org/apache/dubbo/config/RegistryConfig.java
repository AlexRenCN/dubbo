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
import org.apache.dubbo.config.support.Parameter;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.EXTRA_KEYS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.config.Constants.REGISTRIES_SUFFIX;

/**
 * 注册中心配置
 * RegistryConfig
 *
 * @export
 */
public class RegistryConfig extends AbstractConfig {

    public static final String NO_AVAILABLE = "N/A";
    private static final long serialVersionUID = 5508512956753757169L;

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
     * 影响通信量在注册中心之间的分布方式，在订阅多个注册中心时很有用，可用选项：
     * Affects how traffic distributes among registries, useful when subscribing multiple registries, available options:
     * 一。区域感知，特定类型的流量总是根据流量的来源流向一个注册表。
     * 1. zone-aware, a certain type of traffic always goes to one Registry according to where the traffic is originated.
     */
    private String cluster;

    /**
     * 区域
     * 注册表所属的区域，通常用于隔离流量
     * The region where the registry belongs, usually used to isolate traffics
     */
    private String zone;

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

    public RegistryConfig() {
    }

    public RegistryConfig(String address) {
        setAddress(address);
    }

    public RegistryConfig(String address, String protocol) {
        setAddress(address);
        setProtocol(protocol);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
        this.updateIdIfAbsent(protocol);
    }

    @Parameter(excluded = true)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        if (address != null) {
            try {
                URL url = URL.valueOf(address);
                setUsername(url.getUsername());
                setPassword(url.getPassword());
                updateIdIfAbsent(url.getProtocol());
                updateProtocolIfAbsent(url.getProtocol());
                updatePortIfAbsent(url.getPort());
                updateParameters(url.getParameters());
            } catch (Exception ignored) {
            }
        }
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return wait
     * @see org.apache.dubbo.config.ProviderConfig#getWait()
     * @deprecated
     */
    @Deprecated
    public Integer getWait() {
        return wait;
    }

    /**
     * @param wait
     * @see org.apache.dubbo.config.ProviderConfig#setWait(Integer)
     * @deprecated
     */
    @Deprecated
    public void setWait(Integer wait) {
        this.wait = wait;
        if (wait != null && wait > 0) {
            System.setProperty(SHUTDOWN_WAIT_KEY, String.valueOf(wait));
        }
    }

    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    /**
     * @return transport
     * @see #getTransporter()
     * @deprecated
     */
    @Deprecated
    @Parameter(excluded = true)
    public String getTransport() {
        return getTransporter();
    }

    /**
     * @param transport
     * @see #setTransporter(String)
     * @deprecated
     */
    @Deprecated
    public void setTransport(String transport) {
        setTransporter(transport);
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        /*if(transporter != null && transporter.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(transporter)){
            throw new IllegalStateException("No such transporter type : " + transporter);
        }*/
        this.transporter = transporter;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        /*if(server != null && server.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(server)){
            throw new IllegalStateException("No such server type : " + server);
        }*/
        this.server = server;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        /*if(client != null && client.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(client)){
            throw new IllegalStateException("No such client type : " + client);
        }*/
        this.client = client;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getSession() {
        return session;
    }

    public void setSession(Integer session) {
        this.session = session;
    }

    public Boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Boolean isRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    public Boolean isSubscribe() {
        return subscribe;
    }

    public void setSubscribe(Boolean subscribe) {
        this.subscribe = subscribe;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void updateParameters(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return;
        }
        if (this.parameters == null) {
            this.parameters = parameters;
        } else {
            this.parameters.putAll(parameters);
        }
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getSimplified() {
        return simplified;
    }

    public void setSimplified(Boolean simplified) {
        this.simplified = simplified;
    }

    @Parameter(key = EXTRA_KEYS_KEY)
    public String getExtraKeys() {
        return extraKeys;
    }

    public void setExtraKeys(String extraKeys) {
        this.extraKeys = extraKeys;
    }

    @Parameter(excluded = true)
    public Boolean getUseAsConfigCenter() {
        return useAsConfigCenter;
    }

    public void setUseAsConfigCenter(Boolean useAsConfigCenter) {
        this.useAsConfigCenter = useAsConfigCenter;
    }

    @Parameter(excluded = true)
    public Boolean getUseAsMetadataCenter() {
        return useAsMetadataCenter;
    }

    public void setUseAsMetadataCenter(Boolean useAsMetadataCenter) {
        this.useAsMetadataCenter = useAsMetadataCenter;
    }

    public String getAccepts() {
        return accepts;
    }

    public void setAccepts(String accepts) {
        this.accepts = accepts;
    }

    public Boolean getPreferred() {
        return preferred;
    }

    public void setPreferred(Boolean preferred) {
        this.preferred = preferred;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    @Override
    public void refresh() {
        super.refresh();
        if (StringUtils.isNotEmpty(this.getId())) {
            this.setPrefix(REGISTRIES_SUFFIX);
            super.refresh();
        }
    }

    @Override
    @Parameter(excluded = true)
    public boolean isValid() {
        //空协议将默认为“dubbo”
        // empty protocol will default to 'dubbo'
        return !StringUtils.isEmpty(address);
    }

    protected void updatePortIfAbsent(Integer value) {
        if (value != null && value > 0 && port == null) {
            this.port = value;
        }
    }

    protected void updateProtocolIfAbsent(String value) {
        if (StringUtils.isNotEmpty(value) && StringUtils.isEmpty(protocol)) {
            this.protocol = value;
        }
    }
}
