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
package org.apache.dubbo.registry.support;

import com.sun.xml.internal.bind.v2.TODO;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.RegistryService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

/**
 * 注册中心抽象类
 * AbstractRegistryFactory. (SPI, Singleton, ThreadSafe)
 *
 * @see org.apache.dubbo.registry.RegistryFactory
 */
public abstract class AbstractRegistryFactory implements RegistryFactory {

    // Log output
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRegistryFactory.class);

    /**
     * 注册表获取过程的锁
     */
    // The lock for the acquisition process of the registry
    private static final ReentrantLock LOCK = new ReentrantLock();

    /**
     * 注册中心URL地址，注册中心映射集合
     */
    // Registry Collection Map<RegistryAddress, Registry>
    private static final Map<String, Registry> REGISTRIES = new HashMap<>();

    /**
     * 创建注册中心
     * Get all registries
     *
     * @return all registries
     */
    public static Collection<Registry> getRegistries() {
        return Collections.unmodifiableCollection(REGISTRIES.values());
    }

    public static Registry getRegistry(String key) {
        return REGISTRIES.get(key);
    }

    /**
     * 关闭所有的注册中心
     * Close all created registries
     */
    public static void destroyAll() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Close all registries " + getRegistries());
        }
        //加锁
        // Lock up the registry shutdown process
        LOCK.lock();
        try {
            //循环关闭每一个注册中心连接
            for (Registry registry : getRegistries()) {
                try {
                    registry.destroy();
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            //情况映射的缓存
            REGISTRIES.clear();
        } finally {
            //释放锁
            // Release the lock
            LOCK.unlock();
        }
    }

    @Override
    public Registry getRegistry(URL url) {
        //构建URL
        url = URLBuilder.from(url)
                .setPath(RegistryService.class.getName())
                .addParameter(INTERFACE_KEY, RegistryService.class.getName())
                .removeParameters(EXPORT_KEY, REFER_KEY)
                .build();
        String key = url.toServiceStringWithoutResolving();
        //加锁
        // Lock the registry access process to ensure a single instance of the registry
        LOCK.lock();
        try {
            //检查有没有缓存过
            Registry registry = REGISTRIES.get(key);
            if (registry != null) {
                //从缓存里获取并返回
                return registry;
            }
            //创建一个新的注册中心连接
            //create registry by spi/ioc
            registry = createRegistry(url);
            if (registry == null) {
                throw new IllegalStateException("Can not create registry " + url);
            }
            //放到缓存里
            REGISTRIES.put(key, registry);
            return registry;
        } finally {
            //释放锁
            // Release the lock
            LOCK.unlock();
        }
    }

    protected abstract Registry createRegistry(URL url);

}
