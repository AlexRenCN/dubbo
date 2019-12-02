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

import org.apache.dubbo.config.ProtocolConfig;

import java.util.Map;

/**
 * This is a builder for build {@link ProtocolConfig}.
 *
 * @since 2.7
 */
public class ProtocolBuilder extends AbstractBuilder<ProtocolConfig, ProtocolBuilder> {
    /**
     * 协议名称
     * Protocol name
     */
    private String name;

    /**
     * 服务ip地址（当有多个网卡可用时）
     * Service ip address (when there are multiple network cards available)
     */
    private String host;

    /**
     * 服务端口
     * Service port
     */
    private Integer port;

    /**
     * 上下文路径
     * Context path
     */
    private String contextpath;

    /**
     * 线程池
     * Thread pool
     */
    private String threadpool;

    /**
     * 线程池核心线程数量
     * Thread pool core thread size
     */
    private Integer corethreads;

    /**
     * 线程池线程线程数量（固定线程）
     * Thread pool size (fixed size)
     */
    private Integer threads;

    /**
     * IO线程池线程数量（固定线程）
     * IO thread pool size (fixed size)
     */
    private Integer iothreads;

    /**
     * 线程池的队列长度
     * Thread pool's queue length
     */
    private Integer queues;

    /**
     * 最大可接受连接数
     * Max acceptable connections
     */
    private Integer accepts;

    /**
     * 协议编解码器
     * Protocol codec
     */
    private String codec;

    /**
     * 协议序列化器
     * Serialization
     */
    private String serialization;

    /**
     * 协议编码
     * Charset
     */
    private String charset;

    /**
     * 有效载荷最大长度
     * Payload max length
     */
    private Integer payload;

    /**
     * 缓冲区大小
     * Buffer size
     */
    private Integer buffer;

    /**
     * 心跳间隔
     * Heartbeat interval
     */
    private Integer heartbeat;

    /**
     * 日志级别
     * Access log
     */
    private String accesslog;

    /**
     * 转换器
     * Transfort
     */
    private String transporter;

    /**
     * 信息如何交换
     * How information is exchanged
     */
    private String exchanger;

    /**
     * 线程调度器
     * Thread dispatch mode
     */
    private String dispatcher;

    /**
     * 网络
     * Networker
     */
    private String networker;

    /**
     * 实现类
     * Sever impl
     */
    private String server;

    /**
     * 客户端实现
     * Client impl
     */
    private String client;

    /**
     * 支持的telnet命令，用逗号分隔。
     * Supported telnet commands, separated with comma.
     */
    private String telnet;

    /**
     * 命令行提示
     * Command line prompt
     */
    private String prompt;

    /**
     * 状态检查
     * Status check
     */
    private String status;

    /**
     * 是否注册
     * Whether to register
     */
    private Boolean register;

    /**
     * 是否为持久连接
     * whether it is a persistent connection
     */
    //TODO add this to provider config
    private Boolean keepAlive;

    /**
     * 优化器
     */
    // TODO add this to provider config
    private String optimizer;

    /**
     * 扩展
     * The extension
     */
    private String extension;

    /**
     * 自定义参数
     * The customized parameters
     */
    private Map<String, String> parameters;

    /**
     * 是否默认
     * If it's default
     */
    private Boolean isDefault;

    public static ProtocolBuilder newBuilder() {
        return new ProtocolBuilder();
    }

    public ProtocolBuilder id(String id) {
        return super.id(id);
    }

    public ProtocolBuilder name(String name) {
        this.name = name;
        return getThis();
    }

    public ProtocolBuilder host(String host) {
        this.host = host;
        return getThis();
    }

    public ProtocolBuilder port(Integer port) {
        this.port = port;
        return getThis();
    }

    public ProtocolBuilder contextpath(String contextpath) {
        this.contextpath = contextpath;
        return getThis();
    }

    /**
     * @param path
     * @return ProtocolBuilder
     * @see ProtocolBuilder#contextpath(String)
     */
    @Deprecated
    public ProtocolBuilder path(String path) {
        this.contextpath = path;
        return getThis();
    }

    public ProtocolBuilder threadpool(String threadpool) {
        this.threadpool = threadpool;
        return getThis();
    }

    public ProtocolBuilder corethreads(Integer corethreads) {
        this.corethreads = corethreads;
        return getThis();
    }

    public ProtocolBuilder threads(Integer threads) {
        this.threads = threads;
        return getThis();
    }

    public ProtocolBuilder iothreads(Integer iothreads) {
        this.iothreads = iothreads;
        return getThis();
    }

    public ProtocolBuilder queues(Integer queues) {
        this.queues = queues;
        return getThis();
    }

    public ProtocolBuilder accepts(Integer accepts) {
        this.accepts = accepts;
        return getThis();
    }

    public ProtocolBuilder codec(String codec) {
        this.codec = codec;
        return getThis();
    }

    public ProtocolBuilder serialization(String serialization) {
        this.serialization = serialization;
        return getThis();
    }

    public ProtocolBuilder charset(String charset) {
        this.charset = charset;
        return getThis();
    }

    public ProtocolBuilder payload(Integer payload) {
        this.payload = payload;
        return getThis();
    }

    public ProtocolBuilder buffer(Integer buffer) {
        this.buffer = buffer;
        return getThis();
    }

    public ProtocolBuilder heartbeat(Integer heartbeat) {
        this.heartbeat = heartbeat;
        return getThis();
    }

    public ProtocolBuilder accesslog(String accesslog) {
        this.accesslog = accesslog;
        return getThis();
    }

    public ProtocolBuilder transporter(String transporter) {
        this.transporter = transporter;
        return getThis();
    }

    public ProtocolBuilder exchanger(String exchanger) {
        this.exchanger = exchanger;
        return getThis();
    }

    public ProtocolBuilder dispatcher(String dispatcher) {
        this.dispatcher = dispatcher;
        return getThis();
    }

    /**
     * @param dispather
     * @return ProtocolBuilder
     * @see ProtocolBuilder#dispatcher(String)
     */
    @Deprecated
    public ProtocolBuilder dispather(String dispather) {
        this.dispatcher = dispather;
        return getThis();
    }

    public ProtocolBuilder networker(String networker) {
        this.networker = networker;
        return getThis();
    }

    public ProtocolBuilder server(String server) {
        this.server = server;
        return getThis();
    }

    public ProtocolBuilder client(String client) {
        this.client = client;
        return getThis();
    }

    public ProtocolBuilder telnet(String telnet) {
        this.telnet = telnet;
        return getThis();
    }

    public ProtocolBuilder prompt(String prompt) {
        this.prompt = prompt;
        return getThis();
    }

    public ProtocolBuilder status(String status) {
        this.status = status;
        return getThis();
    }

    public ProtocolBuilder register(Boolean register) {
        this.register = register;
        return getThis();
    }

    public ProtocolBuilder keepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
        return getThis();
    }

    public ProtocolBuilder optimizer(String optimizer) {
        this.optimizer = optimizer;
        return getThis();
    }

    public ProtocolBuilder extension(String extension) {
        this.extension = extension;
        return getThis();
    }

    public ProtocolBuilder appendParameter(String key, String value) {
        this.parameters = appendParameter(parameters, key, value);
        return getThis();
    }

    public ProtocolBuilder appendParameters(Map<String, String> appendParameters) {
        this.parameters = appendParameters(parameters, appendParameters);
        return getThis();
    }

    public ProtocolBuilder isDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return getThis();
    }

    public ProtocolConfig build() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        super.build(protocolConfig);

        protocolConfig.setAccepts(accepts);
        protocolConfig.setAccesslog(accesslog);
        protocolConfig.setBuffer(buffer);
        protocolConfig.setCharset(charset);
        protocolConfig.setClient(client);
        protocolConfig.setCodec(codec);
        protocolConfig.setContextpath(contextpath);
        protocolConfig.setCorethreads(corethreads);
        protocolConfig.setDefault(isDefault);
        protocolConfig.setDispatcher(dispatcher);
        protocolConfig.setExchanger(exchanger);
        protocolConfig.setExtension(extension);
        protocolConfig.setHeartbeat(heartbeat);
        protocolConfig.setHost(host);
        protocolConfig.setIothreads(iothreads);
        protocolConfig.setKeepAlive(keepAlive);
        protocolConfig.setName(name);
        protocolConfig.setNetworker(networker);
        protocolConfig.setOptimizer(optimizer);
        protocolConfig.setParameters(parameters);
        protocolConfig.setPayload(payload);
        protocolConfig.setPort(port);
        protocolConfig.setPrompt(prompt);
        protocolConfig.setQueues(queues);
        protocolConfig.setRegister(register);
        protocolConfig.setSerialization(serialization);
        protocolConfig.setServer(server);
        protocolConfig.setStatus(status);
        protocolConfig.setTelnet(telnet);
        protocolConfig.setThreadpool(threadpool);
        protocolConfig.setThreads(threads);
        protocolConfig.setTransporter(transporter);

        return protocolConfig;
    }

    @Override
    protected ProtocolBuilder getThis() {
        return this;
    }
}
