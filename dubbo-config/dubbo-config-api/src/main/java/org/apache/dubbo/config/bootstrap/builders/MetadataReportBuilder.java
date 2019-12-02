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

import org.apache.dubbo.config.MetadataReportConfig;

import java.util.Map;

/**
 * This is a builder for build {@link MetadataReportConfig}.
 *
 * @since 2.7
 */
public class MetadataReportBuilder extends AbstractBuilder<MetadataReportConfig, MetadataReportBuilder> {

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

    public static MetadataReportBuilder newBuilder() {
        return new MetadataReportBuilder();
    }

    public MetadataReportBuilder address(String address) {
        this.address = address;
        return getThis();
    }

    public MetadataReportBuilder username(String username) {
        this.username = username;
        return getThis();
    }

    public MetadataReportBuilder password(String password) {
        this.password = password;
        return getThis();
    }

    public MetadataReportBuilder timeout(Integer timeout) {
        this.timeout = timeout;
        return getThis();
    }

    public MetadataReportBuilder group(String group) {
        this.group = group;
        return getThis();
    }

    public MetadataReportBuilder appendParameters(Map<String, String> appendParameters) {
        this.parameters = appendParameters(this.parameters, appendParameters);
        return getThis();
    }

    public MetadataReportBuilder appendParameter(String key, String value) {
        this.parameters = appendParameter(this.parameters, key, value);
        return getThis();
    }

    public MetadataReportBuilder retryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
        return getThis();
    }

    public MetadataReportBuilder retryPeriod(Integer retryPeriod) {
        this.retryPeriod = retryPeriod;
        return getThis();
    }

    public MetadataReportBuilder cycleReport(Boolean cycleReport) {
        this.cycleReport = cycleReport;
        return getThis();
    }

    public MetadataReportBuilder syncReport(Boolean syncReport) {
        this.syncReport = syncReport;
        return getThis();
    }

    public MetadataReportConfig build() {
        MetadataReportConfig metadataReport = new MetadataReportConfig();
        super.build(metadataReport);

        metadataReport.setAddress(address);
        metadataReport.setUsername(username);
        metadataReport.setPassword(password);
        metadataReport.setTimeout(timeout);
        metadataReport.setGroup(group);
        metadataReport.setParameters(parameters);
        metadataReport.setRetryTimes(retryTimes);
        metadataReport.setRetryPeriod(retryPeriod);
        metadataReport.setCycleReport(cycleReport);
        metadataReport.setSyncReport(syncReport);

        return metadataReport;
    }

    @Override
    protected MetadataReportBuilder getThis() {
        return this;
    }
}
