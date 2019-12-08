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
package org.apache.dubbo.rpc;

import java.util.Map;
import java.util.stream.Stream;

/**
 * TODO 【dubbo域】持有调用过程中的参数、方法签名、附加参数，属于会话域
 * 调用过程（调用接口，原型模式，线程不安全的）
 * Invocation. (API, Prototype, NonThreadSafe)
 *
 * @serial Don't change the class name and package name.
 * @see org.apache.dubbo.rpc.Invoker#invoke(Invocation)
 * @see org.apache.dubbo.rpc.RpcInvocation
 */
public interface Invocation {

    String getTargetServiceUniqueName();

    /**
     * 获取方法名
     * get method name.
     *
     * @return method name.
     * @serial
     */
    String getMethodName();


    /**
     * 获取接口名
     * get the interface name
     * @return
     */
    String getServiceName();

    /**
     * 获取参数类型
     * get parameter types.
     *
     * @return parameter types.
     * @serial
     */
    Class<?>[] getParameterTypes();

    /**
     * 获取参数的签名，用字符串数组表示。
     * get parameter's signature, string representation of parameter types.
     *
     * @return parameter's signature
     */
    default String[] getCompatibleParamSignatures() {
        return Stream.of(getParameterTypes())
                .map(Class::getName)
                .toArray(String[]::new);
    }

    /**
     * 获取参数
     * get arguments.
     *
     * @return arguments.
     * @serial
     */
    Object[] getArguments();

    /**
     * 获取附加值
     * get attachments.
     *
     * @return attachments.
     * @serial
     */
    Map<String, Object> getAttachments();

    void setAttachment(String key, Object value);

    void setAttachmentIfAbsent(String key, Object value);

    /**
     * 根据指定的键获取附加值
     * get attachment by key.
     *
     * @return attachment value.
     * @serial
     */
    Object getAttachment(String key);

    /**
     * 根据指定的键获取附加值，如果找不到就使用默认值
     * get attachment by key with default value.
     *
     * @return attachment value.
     * @serial
     */
    Object getAttachment(String key, Object defaultValue);

    /**
     * 获取当前上下文的调用者
     * get the invoker in current context.
     *
     * @return invoker.
     * @transient
     */
    Invoker<?> getInvoker();

    Object put(Object key, Object value);

    Object get(Object key);

    Map<Object, Object> getAttributes();
}