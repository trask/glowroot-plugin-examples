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
package org.glowroot.plugin.ehcache;

import java.lang.reflect.Method;

import javax.management.MBeanServer;

import org.glowroot.api.Logger;
import org.glowroot.api.LoggerFactory;

public class EhcacheInvoker {

    private static final Logger logger = LoggerFactory.getLogger(EhcacheInvoker.class);

    private final Method registerMBeansMethod;
    private final Method setStatisticsEnabledMethod;

    public EhcacheInvoker(Class<?> cls) {
        Class<?> cacheManagerClass = null;
        Class<?> managementServiceClass = null;
        Class<?> cacheClass = null;
        try {
            cacheManagerClass =
                    Class.forName("net.sf.ehcache.CacheManager", false, cls.getClassLoader());
            managementServiceClass = Class.forName("net.sf.ehcache.management.ManagementService",
                    false, cls.getClassLoader());
            cacheClass = Class.forName("net.sf.ehcache.Cache", false, cls.getClassLoader());
        } catch (ClassNotFoundException e) {
            logger.warn(e.getMessage(), e);
        }
        registerMBeansMethod = getMethod(managementServiceClass, "registerMBeans",
                cacheManagerClass, MBeanServer.class, boolean.class, boolean.class, boolean.class,
                boolean.class, boolean.class);
        setStatisticsEnabledMethod =
                getOptionalMethod(cacheClass, "setStatisticsEnabled", boolean.class);
    }

    void registerMBeans(Object cacheManager, MBeanServer mbeanServer,
            boolean registerCacheManager, boolean registerCaches,
            boolean registerCacheConfigurations, boolean registerCacheStatistics,
            boolean registerCacheStores) {
        if (registerMBeansMethod == null) {
            return;
        }
        try {
            registerMBeansMethod.invoke(null, cacheManager, mbeanServer,
                    registerCacheManager, registerCaches, registerCacheConfigurations,
                    registerCacheStatistics, registerCacheStores);
        } catch (Throwable t) {
            logger.warn("error calling ManagementService.registerMBeans()", t);
        }
    }

    void setStatisticsEnabled(Object cache, boolean enabled) {
        if (setStatisticsEnabledMethod == null) {
            return;
        }
        try {
            setStatisticsEnabledMethod.invoke(cache, enabled);
        } catch (Throwable t) {
            logger.warn("error calling Cache.setStatisticsEnabled()", t);
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

    private static Method getOptionalMethod(Class<?> cls, String methodName,
            Class<?>... parameterTypes) {
        if (cls == null) {
            return null;
        }
        try {
            return cls.getMethod(methodName, parameterTypes);
        } catch (SecurityException e) {
            logger.warn(e.getMessage(), e);
            return null;
        } catch (NoSuchMethodException e) {
            // don't log for optional method
            return null;
        }
    }
}
