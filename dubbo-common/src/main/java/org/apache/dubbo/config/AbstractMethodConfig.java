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

import org.apache.dubbo.config.support.Parameter;

import java.util.Map;

/**
 * 方法级别配置的抽象类
 * AbstractMethodConfig
 *
 * @export
 */
public abstract class AbstractMethodConfig extends AbstractConfig {

    private static final long serialVersionUID = 1L;

    /**
     * 远程调用的超时时间（毫秒）
     * The timeout for remote invocation in milliseconds
     */
    protected Integer timeout;

    /**
     * 重试次数
     * The retry times
     */
    protected Integer retries;

    /**
     * 最大并发调用数
     * max concurrent invocations
     */
    protected Integer actives;

    /**
     * 负载均衡
     * The load balance
     */
    protected String loadbalance;

    /**
     * 是否异步
     * Whether to async
     * 注意：这是一种不可靠的异步机制，它忽略返回值而且不阻塞线程。
     * note that: it is an unreliable asynchronism that ignores return values and does not block threads.
     */
    protected Boolean async;

    /**
     * 是否确认异步发送
     * Whether to ack async-sent
     */
    protected Boolean sent;

    /**
     * 降级mock类
     * 当服务无法执行时调用的模拟类的名称
     * The name of mock class which gets called when a service fails to execute
     *
     * 注意：mock在服务提供者端不支持,当远程服务调用后发生非业务异常时执行模拟
     * note that: the mock doesn't support on the provider side，and the mock is executed when a non-business exception
     * occurs after a remote service call
     */
    protected String mock;

    /**
     * 请求合并
     * Merger
     */
    protected String merger;

    /**
     * 使用请求参数作为键缓存返回结果，可以使用以下选项：lru、threadlocal、jcache等。
     * Cache the return result with the call parameter as key, the following options are available: lru, threadlocal,
     * jcache, etc.
     */
    protected String cache;

    /**
     * 是否启用了JSR303标准注释验证，如果启用了，方法参数上的注释将被验证
     * Whether JSR303 standard annotation validation is enabled or not, if enabled, annotations on method parameters will
     * be validated
     */
    protected String validation;

    /**
     * 自定义参数
     * The customized parameters
     */
    protected Map<String, String> parameters;

    /**
     * 并行调用服务器数量，同事请求多个服务器，只取最先返回的结果
     * Forks for forking cluster
     */
    protected Integer forks;

    public Integer getForks() {
        return forks;
    }

    public void setForks(Integer forks) {
        this.forks = forks;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    public Boolean isAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public Integer getActives() {
        return actives;
    }

    public void setActives(Integer actives) {
        this.actives = actives;
    }

    public Boolean getSent() {
        return sent;
    }

    public void setSent(Boolean sent) {
        this.sent = sent;
    }

    @Parameter(escaped = true)
    public String getMock() {
        return mock;
    }

    public void setMock(String mock) {
        if (mock == null) {
            return;
        }
        this.mock = mock;
    }

    public void setMock(Boolean mock) {
        if (mock == null) {
            setMock((String) null);
        } else {
            setMock(mock.toString());
        }
    }

    public String getMerger() {
        return merger;
    }

    public void setMerger(String merger) {
        this.merger = merger;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

}
