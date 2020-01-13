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
package org.apache.dubbo.rpc.protocol.http;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.http.HttpBinder;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProxyProtocol;

import com.googlecode.jsonrpc4j.HttpException;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.spring.JsonProxyFactoryBean;
import org.springframework.remoting.RemoteAccessException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpProtocol extends AbstractProxyProtocol {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";

    /**
     * HTTP服务器  json-rpc服务端
     */
    private final Map<String, JsonRpcServer> skeletonMap = new ConcurrentHashMap<>();

    private HttpBinder httpBinder;

    public HttpProtocol() {
        super(HttpException.class, JsonRpcClientException.class);
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    /**
     * 默认服务端口80
     * @return
     */
    @Override
    public int getDefaultPort() {
        return 80;
    }

    /**
     * Http处理器
     */
    private class InternalHandler implements HttpHandler {

        /**
         * 是否跨域
         */
        private boolean cors;

        public InternalHandler(boolean cors) {
            this.cors = cors;
        }

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws ServletException {
            //获取URL
            String uri = request.getRequestURI();
            //根据URL从缓存中获取服务器
            JsonRpcServer skeleton = skeletonMap.get(uri);
            //跨域处理
            if (cors) {
                response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
                response.setHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST");
                response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS_HEADER, "*");
            }
            //OPTIONS：
            //1.获取服务器支持的HTTP请求方法
            //2.用来检查服务器的性能
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(200);
            } else if ("POST".equalsIgnoreCase(request.getMethod())) {

                //设置ip端口
                RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
                try {
                    //处理Http请求
                    skeleton.handle(request.getInputStream(), response.getOutputStream());
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            } else {
                response.setStatus(500);
            }
        }

    }

    @Override
    protected <T> Runnable doExport(final T impl, Class<T> type, URL url) throws RpcException {
        //获取服务器IP端口
        String addr = getAddr(url);
        //检查服务器缓存
        ProtocolServer protocolServer = serverMap.get(addr);
        if (protocolServer == null) {
            //没有缓存就创建一个并放到缓存里，使用Http协议的请求处理器
            RemotingServer remotingServer = httpBinder.bind(url, new InternalHandler(url.getParameter("cors", false)));
            serverMap.put(addr, new ProxyProtocolServer(remotingServer));
        }
        //处理接口的URL地址
        final String path = url.getAbsolutePath();
        //构建服务器
        JsonRpcServer skeleton = new JsonRpcServer(impl, type);
        //缓存服务器
        skeletonMap.put(path, skeleton);
        //返回取消暴露服务器的Runnable函数
        return () -> skeletonMap.remove(path);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T doRefer(final Class<T> serviceType, URL url) throws RpcException {
        JsonProxyFactoryBean jsonProxyFactoryBean = new JsonProxyFactoryBean();
        jsonProxyFactoryBean.setServiceUrl(url.setProtocol("http").toIdentityString());
        jsonProxyFactoryBean.setServiceInterface(serviceType);

        jsonProxyFactoryBean.afterPropertiesSet();
        return (T) jsonProxyFactoryBean.getObject();
    }

    /**
     * 将异常类型转换成Dubbo异常编码
     * @param e
     * @return
     */
    protected int getErrorCode(Throwable e) {
        if (e instanceof RemoteAccessException) {
            e = e.getCause();
        }
        if (e != null) {
            Class<?> cls = e.getClass();
            //请求超时
            if (SocketTimeoutException.class.equals(cls)) {
                return RpcException.TIMEOUT_EXCEPTION;
            //网络超时
            } else if (IOException.class.isAssignableFrom(cls)) {
                return RpcException.NETWORK_EXCEPTION;
            //类型不匹配，序列化异常
            } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                return RpcException.SERIALIZATION_EXCEPTION;
            }
        }
        return super.getErrorCode(e);
    }

    @Override
    public void destroy() {
        super.destroy();
        for (String key : new ArrayList<>(serverMap.keySet())) {
            ProtocolServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close jsonrpc server " + server.getUrl());
                    }
                    server.close();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }

}