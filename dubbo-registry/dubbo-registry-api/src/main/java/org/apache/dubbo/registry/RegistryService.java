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
package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;

import java.util.List;

/**
 * 注册中心
 * RegistryService. (SPI, Prototype, ThreadSafe)
 *
 * @see org.apache.dubbo.registry.Registry
 * @see org.apache.dubbo.registry.RegistryFactory#getRegistry(URL)
 */
public interface RegistryService {

    /**
     * 注册数据，比如：服务提供者地址，服务消费者地址，路由规则，重写规则或其他数据
     * Register data, such as : provider service, consumer address, route rule, override rule and other data.
     * <p>
     * 注册需要遵循以下规则
     * Registering is required to support the contract:<br>
     * 一。如果URL设置check=false参数。当注册失败时，不会在后台引发和重试异常。否则，将引发异常。
     * 1. When the URL sets the check=false parameter. When the registration fails, the exception is not thrown and retried in the background. Otherwise, the exception will be thrown.<br>
     * 二。当URL设置dynamic = false参数时，需要持久化存储，否则，当注册者有异常退出时，它应该自动删除。
     * 2. When URL sets the dynamic=false parameter, it needs to be stored persistently, otherwise, it should be deleted automatically when the registrant has an abnormal exit.<br>
     * 三。当URL设置category=routers时，表示已分类存储，默认类别为providers，数据可以由classified部分通知。
     * 3. When the URL sets category=routers, it means classified storage, the default category is providers, and the data can be notified by the classified section. <br>
     * 四。当注册表重新启动时，网络抖动，数据不能丢失，包括自动删除虚线中的数据。
     * 4. When the registry is restarted, network jitter, data can not be lost, including automatically deleting data from the broken line.<br>
     * 五。允许URL具有相同的URL但不同的参数共存，它们不能相互覆盖。
     * 5. Allow URLs which have the same URL but different parameters to coexist,they can't cover each other.<br>
     *
     * @param url  Registration information , is not allowed to be empty, e.g: dubbo://10.20.153.10/org.apache.dubbo.foo.BarService?version=1.0.0&application=kylin
     */
    void register(URL url);

    /**
     * 注销
     * Unregister
     * <p>
     * 注销需要遵循以下规则
     * Unregistering is required to support the contract:<br>
     * 一。如果是dynamic=false的持久存储数据，如果找不到注册数据，则抛出IllegalStateException，否则忽略它。
     * 1. If it is the persistent stored data of dynamic=false, the registration data can not be found, then the IllegalStateException is thrown, otherwise it is ignored.<br>
     * 二。根据完整的url匹配注销。
     * 2. Unregister according to the full url match.<br>
     *
     * @param url Registration information , is not allowed to be empty, e.g: dubbo://10.20.153.10/org.apache.dubbo.foo.BarService?version=1.0.0&application=kylin
     */
    void unregister(URL url);

    /**
     * 订阅
     * 订阅符合条件的注册数据，并在更改注册数据时自动推送。
     * Subscribe to eligible registered data and automatically push when the registered data is changed.
     * <p>
     * 订阅需要遵循以下规则
     * Subscribing need to support contracts:<br>
     * 一。当URL设置check=false参数时。注册失败时，不会在后台引发和重试异常。
     * 1. When the URL sets the check=false parameter. When the registration fails, the exception is not thrown and retried in the background. <br>
     * 二。当URL设置category=routers时，它只通知指定的分类数据。多个分类用逗号分隔，并允许星号匹配，这表示订阅了所有分类数据。
     * 2. When URL sets category=routers, it only notifies the specified classification data. Multiple classifications are separated by commas, and allows asterisk to match, which indicates that all categorical data are subscribed.<br>
     * 三。运行接口，组，版本、类分级作为查询条件，例如：interface=org.apache.dubbo.foo.BarService&version=1.0.0
     * 3. Allow interface, group, version, and classifier as a conditional query, e.g.: interface=org.apache.dubbo.foo.BarService&version=1.0.0<br>
     * 四。查询条件允许星号匹配，订阅所有接口所有包的所有版本，例如：interface=*&group=*&version=*&classifier=*
     * 4. And the query conditions allow the asterisk to be matched, subscribe to all versions of all the packets of all interfaces, e.g. :interface=*&group=*&version=*&classifier=*<br>
     * 五。重新启动注册表和网络抖动时，需要自动恢复订阅请求。
     * 5. When the registry is restarted and network jitter, it is necessary to automatically restore the subscription request.<br>
     * 六。允许URL具有相同的URL但不同的参数共存，它们不能相互覆盖。
     * 6. Allow URLs which have the same URL but different parameters to coexist,they can't cover each other.<br>
     * 七。当第一个通知完成并返回时，必须阻止订阅过程。
     * 7. The subscription process must be blocked, when the first notice is finished and then returned.<br>
     *
     * @param url      Subscription condition, not allowed to be empty, e.g. consumer://10.20.153.10/org.apache.dubbo.foo.BarService?version=1.0.0&application=kylin
     * @param listener A listener of the change event, not allowed to be empty
     */
    void subscribe(URL url, NotifyListener listener);

    /**
     * 取消订阅
     * Unsubscribe
     * <p>
     * 取消订阅需要遵循以下规则
     * Unsubscribing is required to support the contract:<br>
     * 一。如果没有订阅，直接忽略它
     * 1. If don't subscribe, ignore it directly.<br>
     * 二。按完整URL匹配取消订阅。
     * 2. Unsubscribe by full URL match.<br>
     *
     * @param url      Subscription condition, not allowed to be empty, e.g. consumer://10.20.153.10/org.apache.dubbo.foo.BarService?version=1.0.0&application=kylin
     * @param listener A listener of the change event, not allowed to be empty
     */
    void unsubscribe(URL url, NotifyListener listener);

    /**
     * 查询符合条件的注册数据。对应于订阅的push模式，这是pull模式，只返回一个结果。
     * Query the registered data that matches the conditions. Corresponding to the push mode of the subscription, this is the pull mode and returns only one result.
     *
     * @param url Query condition, is not allowed to be empty, e.g. consumer://10.20.153.10/org.apache.dubbo.foo.BarService?version=1.0.0&application=kylin
     * @return The registered information list, which may be empty, the meaning is the same as the parameters of {@link org.apache.dubbo.registry.NotifyListener#notify(List<URL>)}.
     * @see org.apache.dubbo.registry.NotifyListener#notify(List)
     */
    List<URL> lookup(URL url);

}