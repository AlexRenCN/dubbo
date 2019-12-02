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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.support.Parameter;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.PROPERTIES_CHAR_SEPERATOR;

/**
 * 元数据报告配置
 * MetadataReportConfig
 *
 * @export
 */
public class MetadataReportConfig extends AbstractConfig {

    private static final long serialVersionUID = 55233L;
    /**
     * the value is : metadata-report
     */
    private static final String PREFIX_TAG = StringUtils.camelToSplitName(
            MetadataReportConfig.class.getSimpleName().substring(0, MetadataReportConfig.class.getSimpleName().length() - 6), PROPERTIES_CHAR_SEPERATOR);

    /**
     * 注册中心地址
     */
    // Register center address
    private String address;

    /**
     * 注册中心用户名
     */
    // Username to login register center
    private String username;

    /**
     * 注册中心密码
     */
    // Password to login register center
    private String password;

    /**
     * 注册中心请求超时时间（毫秒）
     */
    // Request timeout in milliseconds for register center
    private Integer timeout;

    /**
     * 元数据所在的组。与注册中心相同
     * The group the metadata in . It is the same as registry
     */
    private String group;

    /**
     * 自定义参数
     */
    // Customized parameters
    private Map<String, String> parameters;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 重试间隔
     */
    private Integer retryPeriod;
    /**
     * 默认情况下元数据存储将每天重复存储完整的元数据。
     * By default the metadatastore will store full metadata repeatly every day .
     */
    private Boolean cycleReport;

    /**
     * 同步报告，默认异步
     * Sync report, default async
     */
    private Boolean syncReport;

    /**
     * 集群
     * cluster
     */
    private Boolean cluster;

    public MetadataReportConfig() {
    }

    public MetadataReportConfig(String address) {
        setAddress(address);
    }

    public URL toUrl() {
        String address = this.getAddress();
        if (StringUtils.isEmpty(address)) {
            return null;
        }
        Map<String, String> map = new HashMap<String, String>();
        appendParameters(map, this);
        if (!StringUtils.isEmpty(address)) {
            URL url = URL.valueOf(address);
            map.put("metadata", url.getProtocol());
            return new URL("metadata", url.getUsername(), url.getPassword(), url.getHost(),
                    url.getPort(), url.getPath(), map);
        }
        throw new IllegalArgumentException("The address of metadata report is invalid.");
    }

    @Parameter(excluded = true)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Parameter(key = "retry-times")
    public Integer getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    @Parameter(key = "retry-period")
    public Integer getRetryPeriod() {
        return retryPeriod;
    }

    public void setRetryPeriod(Integer retryPeriod) {
        this.retryPeriod = retryPeriod;
    }

    @Parameter(key = "cycle-report")
    public Boolean getCycleReport() {
        return cycleReport;
    }

    public void setCycleReport(Boolean cycleReport) {
        this.cycleReport = cycleReport;
    }

    @Parameter(key = "sync-report")
    public Boolean getSyncReport() {
        return syncReport;
    }

    public void setSyncReport(Boolean syncReport) {
        this.syncReport = syncReport;
    }

    @Override
    @Parameter(excluded = true)
    public String getPrefix() {
        return StringUtils.isNotEmpty(prefix) ? prefix : (DUBBO + "." + PREFIX_TAG);
    }

    @Override
    @Parameter(excluded = true)
    public boolean isValid() {
        return StringUtils.isNotEmpty(address);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Boolean getCluster() {
        return cluster;
    }

    public void setCluster(Boolean cluster) {
        this.cluster = cluster;
    }
}
