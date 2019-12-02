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

import org.apache.dubbo.common.utils.StringUtils;

/**
 * 消费者默认配置
 * The service consumer default configuration
 *
 * @export
 */
public class ConsumerConfig extends AbstractReferenceConfig {

    private static final long serialVersionUID = 2827274711143680600L;

    /**
     * 是否使用默认协议
     * Whether to use the default protocol
     */
    private Boolean isDefault;

    /**
     * 网络框架客户端使用：netty, mina, etc.
     * Networking framework client uses: netty, mina, etc.
     */
    private String client;

    /**
     * 消费者线程池类型：cached, fixed, limit, eager
     * Consumer thread pool type: cached, fixed, limit, eager
     */
    private String threadpool;

    /**
     * 消费者线程池核心线程
     * Consumer threadpool core thread size
     */
    private Integer corethreads;

    /**
     * 消费者线程池最大线程
     * Consumer threadpool thread size
     */
    private Integer threads;

    /**
     * 消费者线程池队列长度
     * Consumer threadpool queue size
     */
    private Integer queues;

    /**
     * TCP长连接个数
     * 默认情况下，在使用者进程和提供者进程之间共享TCP长连接通信。
     * By default, a TCP long-connection communication is shared between the consumer process and the provider process.
     * 此属性可以设置为共享多个TCP长连接通信。注意，只有dubbo协议生效。
     * This property can be set to share multiple TCP long-connection communications. Note that only the dubbo protocol takes effect.
     */
    private Integer shareconnections;

    @Override
    public void setTimeout(Integer timeout) {
        super.setTimeout(timeout);
        String rmiTimeout = System.getProperty("sun.rmi.transport.tcp.responseTimeout");
        if (timeout != null && timeout > 0
                && (StringUtils.isEmpty(rmiTimeout))) {
            System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(timeout));
        }
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
        this.threadpool = threadpool;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getCorethreads() {
        return corethreads;
    }

    public void setCorethreads(Integer corethreads) {
        this.corethreads = corethreads;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getQueues() {
        return queues;
    }

    public void setQueues(Integer queues) {
        this.queues = queues;
    }

    public Integer getShareconnections() {
        return shareconnections;
    }

    public void setShareconnections(Integer shareconnections) {
        this.shareconnections = shareconnections;
    }
}
