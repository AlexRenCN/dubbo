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
package org.apache.dubbo.config.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于 Dubbo URL 的 parameters 拼接。用在配置对象的 getting 方法上
 * Parameter
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Parameter {

    /**
     * 键（别名）
     * @return
     */
    String key() default "";

    /**
     * 必填校验
     * @return
     */
    boolean required() default false;

    /**
     * 是否忽略
     * @return
     */
    boolean excluded() default false;

    /**
     * 是否转义
     * @return
     */
    boolean escaped() default false;

    /**
     * 是否是属性
     * @return
     */
    boolean attribute() default false;

    /**
     * 是否拼接默认属性
     * @return
     */
    boolean append() default false;

    /**
     * 是否用键作为属性（在key有值时有效）
     * 如果指定了键，它将在生成url时用作带注释属性的键。
     * if {@link #key()} is specified, it will be used as the key for the annotated property when generating url.
     * 默认情况下，此键还将用于检索配置值：
     * by default, this key will also be used to retrieve the config value:
     * <pre>
     * {@code
     *  class ExampleConfig {
     *      // Dubbo will try to get "dubbo.example.alias_for_item=xxx" from .properties, if you want to use the original property
     *      // "dubbo.example.item=xxx", you need to set useKeyAsProperty=false.
     *      @Parameter(key = "alias_for_item")
     *      public getItem();
     *  }
     * }
     *
     * </pre>
     */
    boolean useKeyAsProperty() default true;

}