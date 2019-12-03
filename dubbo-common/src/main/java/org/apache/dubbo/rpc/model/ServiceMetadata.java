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
package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.ServiceDescriptor;
import org.apache.dubbo.common.URL;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注意，这个类目前在Dubbo中没有用法。
 * Notice, this class currently has no usage inside Dubbo.
 *
 * 与服务级别相关的数据，如业务服务的名称、版本、类加载器、安全信息等，还带有扩展属性映射。
 * data related to service level such as name, version, classloader of business service,
 * security info, etc. Also with a AttributeMap for extension.
 */
public class ServiceMetadata extends ServiceDescriptor {

    /**
     * 默认组
     */
    private String defaultGroup;
    /**
     * 服务类
     */
    private Class<?> serviceType;

    /**
     * 代理
     */
    private Object target;

    /**
     * 附带信息
     * 将传输到远程端
     */
    /* will be transferred to remote side */
    private final Map<String, Object> attachments = new ConcurrentHashMap<String, Object>();
    /**
     * 属性集合
     * 本地使用
     */
    /* used locally*/
    private final Map<String, Object> attributeMap = new ConcurrentHashMap<String, Object>();

    public ServiceMetadata(String serviceInterfaceName, String group, String version, Class<?> serviceType) {
        this.serviceInterfaceName = serviceInterfaceName;
        this.defaultGroup = group;
        this.group = group;
        this.version = version;
        this.serviceKey = URL.buildKey(serviceInterfaceName, group, version);
        this.serviceType = serviceType;
    }

    public ServiceMetadata() {
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public Map<String, Object> getAttributeMap() {
        return attributeMap;
    }

    public Object getAttribute(String key) {
        return attributeMap.get(key);
    }

    public void addAttribute(String key, Object value) {
        this.attributeMap.put(key, value);
    }

    public void addAttachment(String key, Object value) {
        this.attributeMap.put(key, value);
    }

    public Class<?> getServiceType() {
        return serviceType;
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public void setServiceType(Class<?> serviceType) {
        this.serviceType = serviceType;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }
}
