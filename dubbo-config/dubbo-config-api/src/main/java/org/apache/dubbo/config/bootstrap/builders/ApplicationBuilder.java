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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.RegistryConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.config.Constants.PRODUCTION_ENVIRONMENT;

/**
 * This is a builder for build {@link ApplicationConfig}.
 * @since 2.7
 */
public class ApplicationBuilder extends AbstractBuilder<ApplicationConfig, ApplicationBuilder> {
    /**
     * 应用名称
     */
    private String name;
    /**
     * 元数据
     */
    private String metadata;
    /**
     * 应用版本号
     */
    private String version;
    /**
     * 应用所属者
     */
    private String owner;
    /**
     * 应用所属组织
     */
    private String organization;
    /**
     * 架构层
     */
    private String architecture;
    /**
     * 环境，例如开发、测试或生产
     */
    private String environment = PRODUCTION_ENVIRONMENT;
    /**
     * Java编译器
     */
    private String compiler;
    /**
     * 日志等级
     */
    private String logger;
    /**
     * 注册中心
     */
    private List<RegistryConfig> registries;
    /**
     * 注册中心id
     */
    private String registryIds;
    /**
     * 监控中心
     */
    private MonitorConfig monitor;
    /**
     * 是否默认
     */
    private Boolean isDefault;
    /**
     * 保存线程快照的目录
     */
    private String dumpDirectory;
    /**
     * 是否启用qos（Quality of Service，服务质量）
     */
    private Boolean qosEnable;
    /**
     * 要监听服务质量的服务端口
     */
    private Integer qosPort;
    /**
     * 是否接受国外ip
     */
    private Boolean qosAcceptForeignIp;
    /**
     * 自定义参数
     */
    private Map<String, String> parameters;
    /**
     * 配置优雅停机
     */
    private String shutwait;

    public static ApplicationBuilder newBuilder() {
        return new ApplicationBuilder();
    }

    public ApplicationBuilder name(String name) {
        this.name = name;
        return getThis();
    }

    public ApplicationBuilder metadata(String metadata) {
        this.metadata = metadata;
        return getThis();
    }

    public ApplicationBuilder version(String version) {
        this.version = version;
        return getThis();
    }

    public ApplicationBuilder owner(String owner) {
        this.owner = owner;
        return getThis();
    }

    public ApplicationBuilder organization(String organization) {
        this.organization = organization;
        return getThis();
    }

    public ApplicationBuilder architecture(String architecture) {
        this.architecture = architecture;
        return getThis();
    }

    public ApplicationBuilder environment(String environment) {
        this.environment = environment;
        return getThis();
    }

    public ApplicationBuilder compiler(String compiler) {
        this.compiler = compiler;
        return getThis();
    }

    public ApplicationBuilder logger(String logger) {
        this.logger = logger;
        return getThis();
    }

    public ApplicationBuilder addRegistry(RegistryConfig registry) {
        if (this.registries == null) {
            this.registries = new ArrayList<>();
        }
        this.registries.add(registry);
        return getThis();
    }

    public ApplicationBuilder addRegistries(List<? extends RegistryConfig> registries) {
        if (this.registries == null) {
            this.registries = new ArrayList<>();
        }
        this.registries.addAll(registries);
        return getThis();
    }

    public ApplicationBuilder registryIds(String registryIds) {
        this.registryIds = registryIds;
        return getThis();
    }

    public ApplicationBuilder monitor(MonitorConfig monitor) {
        this.monitor = monitor;
        return getThis();
    }

    public ApplicationBuilder monitor(String monitor) {
        this.monitor = new MonitorConfig(monitor);
        return getThis();
    }

    public ApplicationBuilder isDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return getThis();
    }

    public ApplicationBuilder dumpDirectory(String dumpDirectory) {
        this.dumpDirectory = dumpDirectory;
        return getThis();
    }

    public ApplicationBuilder qosEnable(Boolean qosEnable) {
        this.qosEnable = qosEnable;
        return getThis();
    }

    public ApplicationBuilder qosPort(Integer qosPort) {
        this.qosPort = qosPort;
        return getThis();
    }

    public ApplicationBuilder qosAcceptForeignIp(Boolean qosAcceptForeignIp) {
        this.qosAcceptForeignIp = qosAcceptForeignIp;
        return getThis();
    }

    public ApplicationBuilder shutwait(String shutwait) {
        this.shutwait = shutwait;
        return getThis();
    }

    public ApplicationBuilder appendParameter(String key, String value) {
        this.parameters = appendParameter(parameters, key, value);
        return getThis();
    }

    public ApplicationBuilder appendParameters(Map<String, String> appendParameters) {
        this.parameters = appendParameters(parameters, appendParameters);
        return getThis();
    }

    public ApplicationConfig build() {
        ApplicationConfig config = new ApplicationConfig();
        super.build(config);

        config.setName(name);
        config.setMetadataType(metadata);
        config.setVersion(this.version);
        config.setOwner(this.owner);
        config.setOrganization(this.organization);
        config.setArchitecture(this.architecture);
        config.setEnvironment(this.environment);
        config.setCompiler(this.compiler);
        config.setLogger(this.logger);
        config.setRegistries(this.registries);
        config.setRegistryIds(this.registryIds);
        config.setMonitor(this.monitor);
        config.setDefault(this.isDefault);
        config.setDumpDirectory(this.dumpDirectory);
        config.setQosEnable(this.qosEnable);
        config.setQosPort(this.qosPort);
        config.setQosAcceptForeignIp(this.qosAcceptForeignIp);
        config.setParameters(this.parameters);
        if (!StringUtils.isEmpty(shutwait)) {
            config.setShutwait(shutwait);
        }
        return config;
    }

    @Override
    protected ApplicationBuilder getThis() {
        return this;
    }
}
