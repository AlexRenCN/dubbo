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
package org.apache.dubbo.config.spring.util;

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static org.springframework.util.ClassUtils.getAllInterfacesForClass;
import static org.springframework.util.ClassUtils.resolveClassName;
import static org.springframework.util.StringUtils.hasText;

/**
 * Dubbbo Annotation Utilities Class
 *
 * @see org.springframework.core.annotation.AnnotationUtils
 * @since 2.5.11
 */
public class DubboAnnotationUtils {


    @Deprecated
    public static String resolveInterfaceName(Service service, Class<?> defaultInterfaceClass)
            throws IllegalStateException {

        String interfaceName;
        if (hasText(service.interfaceName())) {
            interfaceName = service.interfaceName();
        } else if (!void.class.equals(service.interfaceClass())) {
            interfaceName = service.interfaceClass().getName();
        } else if (defaultInterfaceClass.isInterface()) {
            interfaceName = defaultInterfaceClass.getName();
        } else {
            throw new IllegalStateException(
                    "The @Service undefined interfaceClass or interfaceName, and the type "
                            + defaultInterfaceClass.getName() + " is not a interface.");
        }

        return interfaceName;

    }

    /**
     * Resolve the interface name from {@link AnnotationAttributes}
     *
     * @param attributes            {@link AnnotationAttributes} instance, may be {@link Service @Service} or {@link Reference @Reference}
     * @param defaultInterfaceClass the default {@link Class class} of interface
     * @return the interface name if found
     * @throws IllegalStateException if interface name was not found
     */
    public static String resolveInterfaceName(AnnotationAttributes attributes, Class<?> defaultInterfaceClass) {
        Boolean generic = getAttribute(attributes, "generic");
        if (generic != null && generic) {
            // it's a generic reference
            String interfaceClassName = getAttribute(attributes, "interfaceName");
            Assert.hasText(interfaceClassName,
                    "@Reference interfaceName() must be present when reference a generic service!");
            return interfaceClassName;
        }
        return resolveServiceInterfaceClass(attributes, defaultInterfaceClass).getName();
    }

    /**
     *获取service接口
     * Resolve the {@link Class class} of Dubbo Service interface from the specified
     * {@link AnnotationAttributes annotation attributes} and annotated {@link Class class}.
     *
     * @param attributes            {@link AnnotationAttributes annotation attributes}
     * @param defaultInterfaceClass the annotated {@link Class class}.
     * @return the {@link Class class} of Dubbo Service interface
     * @throws IllegalArgumentException if can't resolved
     */
    public static Class<?> resolveServiceInterfaceClass(AnnotationAttributes attributes, Class<?> defaultInterfaceClass)
            throws IllegalArgumentException {

        ClassLoader classLoader = defaultInterfaceClass != null ? defaultInterfaceClass.getClassLoader() : Thread.currentThread().getContextClassLoader();

        //从service注解里获取指定的接口类interfaceClass
        Class<?> interfaceClass = getAttribute(attributes, "interfaceClass");

        //如果找不到指定了接口类，就使用默认的接口类
        if (void.class.equals(interfaceClass)) { // default or set void.class for purpose.

            interfaceClass = null;

            //获取接口类的名字
            String interfaceClassName = getAttribute(attributes, "interfaceName");

            if (hasText(interfaceClassName)) {
                //检查能否从类加载器中获取到
                if (ClassUtils.isPresent(interfaceClassName, classLoader)) {
                    //根据类名解析成类
                    interfaceClass = resolveClassName(interfaceClassName, classLoader);
                }
            }

        }

        //兜底的接口是默认接口类里获取第一个接口
        if (interfaceClass == null && defaultInterfaceClass != null) {
            // Find all interfaces from the annotated class
            // To resolve an issue : https://github.com/apache/dubbo/issues/3251
            Class<?>[] allInterfaces = getAllInterfacesForClass(defaultInterfaceClass);

            if (allInterfaces.length > 0) {
                interfaceClass = allInterfaces[0];
            }

        }

        Assert.notNull(interfaceClass,
                "@Service interfaceClass() or interfaceName() or interface class must be present!");

        Assert.isTrue(interfaceClass.isInterface(),
                "The annotated type must be an interface!");

        return interfaceClass;
    }

    @Deprecated
    public static String resolveInterfaceName(Reference reference, Class<?> defaultInterfaceClass)
            throws IllegalStateException {

        String interfaceName;
        if (!"".equals(reference.interfaceName())) {
            interfaceName = reference.interfaceName();
        } else if (!void.class.equals(reference.interfaceClass())) {
            interfaceName = reference.interfaceClass().getName();
        } else if (defaultInterfaceClass.isInterface()) {
            interfaceName = defaultInterfaceClass.getName();
        } else {
            throw new IllegalStateException(
                    "The @Reference undefined interfaceClass or interfaceName, and the type "
                            + defaultInterfaceClass.getName() + " is not a interface.");
        }

        return interfaceName;
    }
}
