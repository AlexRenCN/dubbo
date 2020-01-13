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

package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.Parameters;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;

/**
 * 请求协议抽象类
 * AbstractProxyProtocol
 */
public abstract class AbstractProxyProtocol extends AbstractProtocol {

    /**
     * 需要抛出的调用异常
     */
    private final List<Class<?>> rpcExceptions = new CopyOnWriteArrayList<Class<?>>();

    protected ProxyFactory proxyFactory;

    public AbstractProxyProtocol() {
    }

    public AbstractProxyProtocol(Class<?>... exceptions) {
        for (Class<?> exception : exceptions) {
            addRpcException(exception);
        }
    }

    public void addRpcException(Class<?> exception) {
        this.rpcExceptions.add(exception);
    }

    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
        //获取服务URL
        final String uri = serviceKey(invoker.getUrl());
        Exporter<T> exporter = (Exporter<T>) exporterMap.get(uri);
        if (exporter != null) {
            //检查URL是否重写过
            // When modifying the configuration through override, you need to re-expose the newly modified service.
            if (Objects.equals(exporter.getInvoker().getUrl(), invoker.getUrl())) {
                //从缓存里取出，直接返回
                return exporter;
            }
        }
        //如法从缓存里取出，使用代理工厂、接口、URL为参数创建一个新的Exporter
        final Runnable runnable = doExport(proxyFactory.getProxy(invoker, true), invoker.getInterface(), invoker.getUrl());
        exporter = new AbstractExporter<T>(invoker) {
            @Override
            public void unexport() {
                super.unexport();
                //从缓存中移除
                exporterMap.remove(uri);
                if (runnable != null) {
                    try {
                        //再暴露出去
                        runnable.run();
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }
            }
        };
        //放到缓存里
        exporterMap.put(uri, exporter);
        return exporter;
    }

    @Override
    protected <T> Invoker<T> protocolBindingRefer(final Class<T> type, final URL url) throws RpcException {
        //获取Invoker
        final Invoker<T> target = proxyFactory.getInvoker(doRefer(type, url), type, url);
        Invoker<T> invoker = new AbstractInvoker<T>(type, url) {
            @Override
            protected Result doInvoke(Invocation invocation) throws Throwable {
                try {
                    Result result = target.invoke(invocation);
                    // FIXME result is an AsyncRpcResult instance.
                    Throwable e = result.getException();
                    //检查远程调用的异常能否与本地指定异常匹配
                    if (e != null) {
                        for (Class<?> rpcException : rpcExceptions) {
                            if (rpcException.isAssignableFrom(e.getClass())) {
                                throw getRpcException(type, url, invocation, e);
                            }
                        }
                    }
                    return result;
                } catch (RpcException e) {
                    if (e.getCode() == RpcException.UNKNOWN_EXCEPTION) {
                        e.setCode(getErrorCode(e.getCause()));
                    }
                    throw e;
                } catch (Throwable e) {
                    throw getRpcException(type, url, invocation, e);
                }
            }
        };
        //添加到服务引用的缓存中
        invokers.add(invoker);
        return invoker;
    }

    protected RpcException getRpcException(Class<?> type, URL url, Invocation invocation, Throwable e) {
        RpcException re = new RpcException("Failed to invoke remote service: " + type + ", method: "
                + invocation.getMethodName() + ", cause: " + e.getMessage(), e);
        re.setCode(getErrorCode(e));
        return re;
    }

    /**
     * 获取服务器地址
     * @param url
     * @return
     */
    protected String getAddr(URL url) {
        //获取绑定ip
        String bindIp = url.getParameter(Constants.BIND_IP_KEY, url.getHost());
        if (url.getParameter(ANYHOST_KEY, false)) {
            bindIp = ANYHOST_VALUE;
        }
        //追加端口
        return NetUtils.getIpByHost(bindIp) + ":" + url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
    }

    protected int getErrorCode(Throwable e) {
        return RpcException.UNKNOWN_EXCEPTION;
    }

    protected abstract <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException;

    protected abstract <T> T doRefer(Class<T> type, URL url) throws RpcException;

    protected class ProxyProtocolServer implements ProtocolServer {

        private RemotingServer server;
        private String address;

        public ProxyProtocolServer(RemotingServer server) {
            this.server = server;
        }

        @Override
        public RemotingServer getRemotingServer() {
            return server;
        }

        @Override
        public String getAddress() {
            return StringUtils.isNotEmpty(address) ? address : server.getUrl().getAddress();
        }

        @Override
        public void setAddress(String address) {
            this.address = address;
        }

        @Override
        public URL getUrl() {
            return server.getUrl();
        }

        @Override
        public void close() {
            server.close();
        }
    }

    protected abstract class RemotingServerAdapter implements RemotingServer {

        public abstract Object getDelegateServer();

        /**
         * @return
         */
        @Override
        public boolean isBound() {
            return false;
        }

        @Override
        public Collection<Channel> getChannels() {
            return null;
        }

        @Override
        public Channel getChannel(InetSocketAddress remoteAddress) {
            return null;
        }

        @Override
        public void reset(Parameters parameters) {

        }

        @Override
        public void reset(URL url) {

        }

        @Override
        public URL getUrl() {
            return null;
        }

        @Override
        public ChannelHandler getChannelHandler() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public void send(Object message) throws RemotingException {

        }

        @Override
        public void send(Object message, boolean sent) throws RemotingException {

        }

        @Override
        public void close() {

        }

        @Override
        public void close(int timeout) {

        }

        @Override
        public void startClose() {

        }

        @Override
        public boolean isClosed() {
            return false;
        }
    }


}
