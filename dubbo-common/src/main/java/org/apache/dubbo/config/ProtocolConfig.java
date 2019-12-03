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
import org.apache.dubbo.config.support.Parameter;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.config.Constants.PROTOCOLS_SUFFIX;

/**
 * 服务提供者协议配置
 * ProtocolConfig
 *
 * @export
 */
public class ProtocolConfig extends AbstractConfig {

    private static final long serialVersionUID = 6913423882496634749L;

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

    /**
     * 是否支持SSL
     */
    private Boolean sslEnabled;

    public ProtocolConfig() {
    }

    public ProtocolConfig(String name) {
        setName(name);
    }

    public ProtocolConfig(String name, int port) {
        setName(name);
        setPort(port);
    }

    @Parameter(excluded = true)
    public String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
        this.updateIdIfAbsent(name);
    }

    @Parameter(excluded = true)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Parameter(excluded = true)
    public Integer getPort() {
        return port;
    }

    public final void setPort(Integer port) {
        this.port = port;
    }

    @Deprecated
    @Parameter(excluded = true)
    public String getPath() {
        return getContextpath();
    }

    @Deprecated
    public void setPath(String path) {
        setContextpath(path);
    }

    @Parameter(excluded = true)
    public String getContextpath() {
        return contextpath;
    }

    public void setContextpath(String contextpath) {
        this.contextpath = contextpath;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
        this.threadpool = threadpool;
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

    public Integer getIothreads() {
        return iothreads;
    }

    public void setIothreads(Integer iothreads) {
        this.iothreads = iothreads;
    }

    public Integer getQueues() {
        return queues;
    }

    public void setQueues(Integer queues) {
        this.queues = queues;
    }

    public Integer getAccepts() {
        return accepts;
    }

    public void setAccepts(Integer accepts) {
        this.accepts = accepts;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Integer getPayload() {
        return payload;
    }

    public void setPayload(Integer payload) {
        this.payload = payload;
    }

    public Integer getBuffer() {
        return buffer;
    }

    public void setBuffer(Integer buffer) {
        this.buffer = buffer;
    }

    public Integer getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Integer heartbeat) {
        this.heartbeat = heartbeat;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getAccesslog() {
        return accesslog;
    }

    public void setAccesslog(String accesslog) {
        this.accesslog = accesslog;
    }

    public String getTelnet() {
        return telnet;
    }

    public void setTelnet(String telnet) {
        this.telnet = telnet;
    }

    @Parameter(escaped = true)
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean isRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        this.transporter = transporter;
    }

    public String getExchanger() {
        return exchanger;
    }

    public void setExchanger(String exchanger) {
        this.exchanger = exchanger;
    }

    /**
     * typo, switch to use {@link #getDispatcher()}
     *
     * @deprecated {@link #getDispatcher()}
     */
    @Deprecated
    @Parameter(excluded = true)
    public String getDispather() {
        return getDispatcher();
    }

    /**
     * typo, switch to use {@link #getDispatcher()}
     *
     * @deprecated {@link #setDispatcher(String)}
     */
    @Deprecated
    public void setDispather(String dispather) {
        setDispatcher(dispather);
    }

    public String getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(String dispatcher) {
        this.dispatcher = dispatcher;
    }

    public String getNetworker() {
        return networker;
    }

    public void setNetworker(String networker) {
        this.networker = networker;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(Boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getOptimizer() {
        return optimizer;
    }

    public void setOptimizer(String optimizer) {
        this.optimizer = optimizer;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public void refresh() {
        if (StringUtils.isEmpty(this.getName())) {
            this.setName(DUBBO_VERSION_KEY);
        }
        super.refresh();
        if (StringUtils.isNotEmpty(this.getId())) {
            this.setPrefix(PROTOCOLS_SUFFIX);
            super.refresh();
        }
    }

    @Override
    @Parameter(excluded = true)
    public boolean isValid() {
        return StringUtils.isNotEmpty(name);
    }
}
