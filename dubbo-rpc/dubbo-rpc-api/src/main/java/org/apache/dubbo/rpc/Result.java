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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * (API, Prototype, NonThreadSafe)
 * TODO 【dubbo域】一个远程调用的返回结果，属于会话域
 * 一个远程调用的结果
 * An RPC {@link Result}.
 *
 * Known implementations are:
 * 1. {@link AsyncRpcResult}, it's a {@link CompletionStage} whose underlying value signifies the return value of an RPC call.
 * 2. {@link AppResponse}, it inevitably inherits {@link CompletionStage} and {@link Future}, but you should never treat AppResponse as a type of Future,
 *    instead, it is a normal concrete type.
 *
 * @serial Don't change the class name and package name.
 * @see org.apache.dubbo.rpc.Invoker#invoke(Invocation)
 * @see AppResponse
 */
public interface Result extends Serializable {

    /**
     * 获取调用结果
     * Get invoke result.
     *
     * @return result. if no result return null.
     */
    Object getValue();

    void setValue(Object value);

    /**
     * 获取异常
     * Get exception.
     *
     * @return exception. if no exception return null.
     */
    Throwable getException();

    void setException(Throwable t);

    /**
     * 是否存在异常
     * Has exception.
     *
     * @return has exception.
     */
    boolean hasException();

    /**
     * 获取调用的返回值或者异常
     * Recreate.
     * <p>
     * <code>
     * if (hasException()) {
     * throw getException();
     * } else {
     * return getValue();
     * }
     * </code>
     *
     * @return result.
     * @throws if has exception throw it.
     */
    Object recreate() throws Throwable;

    /**
     * 获取附加信息集合
     * get attachments.
     *
     * @return attachments.
     */
    Map<String, Object> getAttachments();

    /**
     * 将指定的映射添加到该实例中的现有附加信息中。
     * Add the specified map to existing attachments in this instance.
     *
     * @param map
     */
    void addAttachments(Map<String, Object> map);

    /**
     * 将指定的映射集合添加到该实例中的现有附加信息中
     * Replace the existing attachments with the specified param.
     *
     * @param map
     */
    void setAttachments(Map<String, Object> map);

    /**
     * 用指定的键获取附加信息
     * get attachment by key.
     *
     * @return attachment value.
     */
    Object getAttachment(String key);

    /**
     * 用指定的键获取附加信息，否则使用默认值
     * get attachment by key with default value.
     *
     * @return attachment value.
     */
    Object getAttachment(String key, Object defaultValue);

    void setAttachment(String key, Object value);

    /**
     * 添加可在RPC调用完成时触发的回调。
     * Add a callback which can be triggered when the RPC call finishes.
     * <p>
     * 正如方法名所暗示的，这个方法将保证在调用启动时的相同上下文下触发回调
     * Just as the method name implies, this method will guarantee the callback being triggered under the same context as when the call was started,
     * see implementation in {@link Result#whenCompleteWithContext(BiConsumer)}
     *
     * @param fn
     * @return
     */
    Result whenCompleteWithContext(BiConsumer<Result, Throwable> fn);

    <U> CompletableFuture<U> thenApply(Function<Result, ? extends U> fn);

    Result get() throws InterruptedException, ExecutionException;

    Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}