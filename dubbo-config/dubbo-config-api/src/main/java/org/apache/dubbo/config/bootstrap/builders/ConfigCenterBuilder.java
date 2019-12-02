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

import org.apache.dubbo.config.ConfigCenterConfig;

import java.util.Map;

/**
 * This is a builder for build {@link ConfigCenterConfig}.
 *
 * @since 2.7
 */
public class ConfigCenterBuilder extends AbstractBuilder<ConfigCenterConfig, ConfigCenterBuilder> {
    /**
     * 协议
     */
    private String protocol;
    /**
     * 地址
     */
    private String address;
    /**
     * 集群
     */
    private String cluster;
    /**
     * 命名空间
     */
    private String namespace = "dubbo";
    /**
     * 配置中心组
     */
    private String group = "dubbo";
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 超时时间
     */
    private Long timeout = 3000L;
    /**
     * 配置是不是最高优先级
     */
    private Boolean highestPriority = true;
    /**
     * 是否检查链接
     */
    private Boolean check = true;
    /**
     * 全局配置文件名
     */
    private String configFile = "dubbo.properties";
    /**
     * 应用配置文件名
     */
    private String appConfigFile;
    /**
     * 自定义参数
     */
    private Map<String, String> parameters;

    public ConfigCenterBuilder protocol(String protocol) {
        this.protocol = protocol;
        return getThis();
    }

	public ConfigCenterBuilder address(String address) {
        this.address = address;
        return getThis();
    }

    public ConfigCenterBuilder cluster(String cluster) {
        this.cluster = cluster;
        return getThis();
    }

    public ConfigCenterBuilder namespace(String namespace) {
        this.namespace = namespace;
        return getThis();
    }

    public ConfigCenterBuilder group(String group) {
        this.group = group;
        return getThis();
    }

    public ConfigCenterBuilder username(String username) {
        this.username = username;
        return getThis();
    }

    public ConfigCenterBuilder password(String password) {
        this.password = password;
        return getThis();
    }

    public ConfigCenterBuilder timeout(Long timeout) {
        this.timeout = timeout;
        return getThis();
    }

    public ConfigCenterBuilder highestPriority(Boolean highestPriority) {
        this.highestPriority = highestPriority;
        return getThis();
    }

    public ConfigCenterBuilder check(Boolean check) {
        this.check = check;
        return getThis();
    }

    public ConfigCenterBuilder configFile(String configFile) {
        this.configFile = configFile;
        return getThis();
    }

    public ConfigCenterBuilder appConfigFile(String appConfigFile) {
        this.appConfigFile = appConfigFile;
        return getThis();
    }

    public ConfigCenterBuilder appendParameters(Map<String, String> appendParameters) {
        this.parameters = appendParameters(this.parameters, appendParameters);
        return getThis();
    }

    public ConfigCenterBuilder appendParameter(String key, String value) {
        this.parameters = appendParameter(this.parameters, key, value);
        return getThis();
    }

    public ConfigCenterConfig build() {
        ConfigCenterConfig configCenter = new ConfigCenterConfig();
        super.build(configCenter);

        configCenter.setProtocol(protocol);
        configCenter.setAddress(address);
        configCenter.setCluster(cluster);
        configCenter.setNamespace(namespace);
        configCenter.setGroup(group);
        configCenter.setUsername(username);
        configCenter.setPassword(password);
        configCenter.setTimeout(timeout);
        configCenter.setHighestPriority(highestPriority);
        configCenter.setCheck(check);
        configCenter.setConfigFile(configFile);
        configCenter.setAppConfigFile(appConfigFile);
        configCenter.setParameters(parameters);

        return configCenter;
    }

    @Override
    protected ConfigCenterBuilder getThis() {
        return this;
    }
}
