/**
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

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.management.ManagementService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.Containers;
import org.glowroot.container.AppUnderTest;
import org.glowroot.container.Container;

import static org.assertj.core.api.Assertions.assertThat;

public class EhcachePluginTest {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        container = Containers.getSharedContainer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
    }

    @After
    public void afterEachTest() throws Exception {
        container.checkAndReset();
    }

    @Test
    public void testStatisticsRegistered() throws Exception {
        // given
        // when
        container.executeAppUnderTest(CreateCache.class);
        // then
        ObjectName objectName = ObjectName.getInstance("glowroot:plugin=ehcache,"
                + "type=CacheStatistics,CacheManager=testmanager,name=testcache");
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        long size = (Long) platformMBeanServer.getAttribute(objectName, "MemoryStoreObjectCount");
        assertThat(size).isEqualTo(1);
        long hits = (Long) platformMBeanServer.getAttribute(objectName, "CacheHits");
        assertThat(hits).isEqualTo(2);
    }

    public static class CreateCache implements AppUnderTest {

        @Override
        public void executeApp() throws Exception {
            CacheManager cacheManager = CacheManager.newInstance();
            Element element = new Element("abc", "xyz");
            cacheManager.getCache("testcache").put(element);
            cacheManager.getCache("testcache").get("abc");
            cacheManager.getCache("testcache").get("abc");
            ManagementService.registerMBeans(cacheManager,
                    ManagementFactory.getPlatformMBeanServer(), true, true, true, true, true);
        }
    }
}
