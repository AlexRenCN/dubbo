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
package org.apache.dubbo.common.config;

import com.sun.xml.internal.bind.v2.TODO;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.context.FrameworkExt;
import org.apache.dubbo.common.context.LifecycleAdapter;
import org.apache.dubbo.common.extension.DisableInject;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Environment extends LifecycleAdapter implements FrameworkExt {
    public static final String NAME = "environment";

    /**
     * 配置文件配置
     */
    private Map<String, PropertiesConfiguration> propertiesConfigs = new ConcurrentHashMap<>();
    /**
     * 系统配置
     */
    private Map<String, SystemConfiguration> systemConfigs = new ConcurrentHashMap<>();
    /**
     * 系统环境配置
     */
    private Map<String, EnvironmentConfiguration> environmentConfigs = new ConcurrentHashMap<>();
    private Map<String, InmemoryConfiguration> externalConfigs = new ConcurrentHashMap<>();
    private Map<String, InmemoryConfiguration> appExternalConfigs = new ConcurrentHashMap<>();

    private Map<String, String> externalConfigurationMap = new HashMap<>();
    private Map<String, String> appExternalConfigurationMap = new HashMap<>();

    /**
     * 配置中心的配置是否优先使用
     */
    private boolean configCenterFirst = true;

    private DynamicConfiguration dynamicConfiguration;

    @Override
    public void initialize() throws IllegalStateException {
        ConfigManager configManager = ApplicationModel.getConfigManager();
        Optional<Collection<ConfigCenterConfig>> defaultConfigs = configManager.getDefaultConfigCenter();
        defaultConfigs.ifPresent(configs -> {
            for (ConfigCenterConfig config : configs) {
                this.setExternalConfigMap(config.getExternalConfiguration());
                this.setAppExternalConfigMap(config.getAppExternalConfiguration());
            }
        });
    }

    public PropertiesConfiguration getPropertiesConfig(String prefix, String id) {
        return propertiesConfigs.computeIfAbsent(toKey(prefix, id), k -> new PropertiesConfiguration(prefix, id));
    }

    /**
     * 获取系统配置
     * @param prefix
     * @param id
     * @return
     */
    public SystemConfiguration getSystemConfig(String prefix, String id) {
        return systemConfigs.computeIfAbsent(toKey(prefix, id), k -> new SystemConfiguration(prefix, id));
    }

    public InmemoryConfiguration getExternalConfig(String prefix, String id) {
        return externalConfigs.computeIfAbsent(toKey(prefix, id), k -> {
            InmemoryConfiguration configuration = new InmemoryConfiguration(prefix, id);
            configuration.setProperties(externalConfigurationMap);
            return configuration;
        });
    }

    public InmemoryConfiguration getAppExternalConfig(String prefix, String id) {
        return appExternalConfigs.computeIfAbsent(toKey(prefix, id), k -> {
            InmemoryConfiguration configuration = new InmemoryConfiguration(prefix, id);
            configuration.setProperties(appExternalConfigurationMap);
            return configuration;
        });
    }

    public EnvironmentConfiguration getEnvironmentConfig(String prefix, String id) {
        return environmentConfigs.computeIfAbsent(toKey(prefix, id), k -> new EnvironmentConfiguration(prefix, id));
    }

    @DisableInject
    public void setExternalConfigMap(Map<String, String> externalConfiguration) {
        if (externalConfiguration != null) {
            this.externalConfigurationMap = externalConfiguration;
        }
    }

    @DisableInject
    public void setAppExternalConfigMap(Map<String, String> appExternalConfiguration) {
        if (appExternalConfiguration != null) {
            this.appExternalConfigurationMap = appExternalConfiguration;
        }
    }

    public Map<String, String> getExternalConfigurationMap() {
        return externalConfigurationMap;
    }

    public Map<String, String> getAppExternalConfigurationMap() {
        return appExternalConfigurationMap;
    }

    public void updateExternalConfigurationMap(Map<String, String> externalMap) {
        this.externalConfigurationMap.putAll(externalMap);
    }

    public void updateAppExternalConfigurationMap(Map<String, String> externalMap) {
        this.appExternalConfigurationMap.putAll(externalMap);
    }

    /**
     * 为每次调用创建新的实例，因为它只能在启动时调用，我认为潜在的成本没有什么大不了的。
     * Create new instance for each call, since it will be called only at startup, I think there's no big deal of the potential cost.
     * 否则，如果使用缓存，我们应该确保每个配置都有一个唯一的id，这是很难保证的，因为它在用户端，
     * Otherwise, if use cache, we should make sure each Config has a unique id which is difficult to guarantee because is on the user's side,
     * 尤其是当涉及到ServiceConfig和ReferenceConfig时。
     * especially when it comes to ServiceConfig and ReferenceConfig.
     *
     * @param prefix
     * @param id
     * @return
     */
    public CompositeConfiguration getConfiguration(String prefix, String id) {
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        // 配置中心具有最高优先级
        // Config center has the highest priority
        // 系统配置
        compositeConfiguration.addConfiguration(this.getSystemConfig(prefix, id));
        // 系统环境配置
        compositeConfiguration.addConfiguration(this.getEnvironmentConfig(prefix, id));
        // 应用配置
        compositeConfiguration.addConfiguration(this.getAppExternalConfig(prefix, id));
        // 内存配置
        compositeConfiguration.addConfiguration(this.getExternalConfig(prefix, id));
        // 配置文件配置
        compositeConfiguration.addConfiguration(this.getPropertiesConfig(prefix, id));
        return compositeConfiguration;
    }

    public Configuration getConfiguration() {
        return getConfiguration(null, null);
    }

    private static String toKey(String prefix, String id) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(prefix)) {
            sb.append(prefix);
        }
        if (StringUtils.isNotEmpty(id)) {
            sb.append(id);
        }

        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '.') {
            sb.append(".");
        }

        if (sb.length() > 0) {
            return sb.toString();
        }
        return CommonConstants.DUBBO;
    }

    public boolean isConfigCenterFirst() {
        return configCenterFirst;
    }

    @DisableInject
    public void setConfigCenterFirst(boolean configCenterFirst) {
        this.configCenterFirst = configCenterFirst;
    }

    public Optional<DynamicConfiguration> getDynamicConfiguration() {
        return Optional.ofNullable(dynamicConfiguration);
    }

    @DisableInject
    public void setDynamicConfiguration(DynamicConfiguration dynamicConfiguration) {
        this.dynamicConfiguration = dynamicConfiguration;
    }

    @Override
    public void destroy() throws IllegalStateException {
        clearExternalConfigs();
        clearAppExternalConfigs();
    }

    // For test
    public void clearExternalConfigs() {
        this.externalConfigs.clear();
        this.externalConfigurationMap.clear();
    }

    // For test
    public void clearAppExternalConfigs() {
        this.appExternalConfigs.clear();
        this.appExternalConfigurationMap.clear();
    }
}
