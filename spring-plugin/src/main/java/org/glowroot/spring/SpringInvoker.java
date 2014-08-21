/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.spring;

import java.lang.reflect.Method;

import org.glowroot.api.Logger;
import org.glowroot.api.LoggerFactory;

public class SpringInvoker {

    private static final Logger logger = LoggerFactory.getLogger(SpringInvoker.class);

    private final Method isAopProxyMethod;

    public SpringInvoker(Class<?> cls) {
        Class<?> aopUtilsClass = null;
        try {
            aopUtilsClass = Class.forName("org.springframework.aop.support.AopUtils", false,
                    cls.getClassLoader());
        } catch (ClassNotFoundException e) {
            logger.warn(e.getMessage(), e);
        }
        isAopProxyMethod = getMethod(aopUtilsClass, "isAopProxy", Object.class);
    }

    public boolean isAopProxy(Object object) {
        if (isAopProxyMethod == null) {
            return false;
        }
        try {
            Boolean value = (Boolean) isAopProxyMethod.invoke(null, object);
            if (value == null) {
                logger.warn("method unexpectedly returned null: AopUtils.isAopProxy()");
                return false;
            }
            return value;
        } catch (Throwable t) {
            logger.warn("error calling AopUtils.isAopProxy()", t);
            return false;
        }
    }

    private static Method getMethod(Class<?> cls, String methodName, Class<?>... parameterTypes) {
        if (cls == null) {
            return null;
        }
        try {
            return cls.getMethod(methodName, parameterTypes);
        } catch (SecurityException e) {
            logger.warn(e.getMessage(), e);
            return null;
        } catch (NoSuchMethodException e) {
            logger.warn(e.getMessage(), e);
            return null;
        }
    }
}
