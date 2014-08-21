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
package org.glowroot.plugin.groovy;

import java.net.URL;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import groovy.lang.GroovyShell;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.Containers;
import org.glowroot.container.AppUnderTest;
import org.glowroot.container.Container;
import org.glowroot.container.TraceMarker;
import org.glowroot.container.trace.Trace;
import org.glowroot.container.trace.TraceEntry;
import org.glowroot.container.trace.TraceMetric;

import static org.assertj.core.api.Assertions.assertThat;

public class GroovyPluginTest {

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
    public void testParseAndLoadWithMetrics() throws Exception {
        // given
        // when
        container.executeAppUnderTest(ExecuteGroovy.class);
        // then
        Trace trace = container.getTraceService().getLastTrace();
        trace.getRootMetric().getNestedMetrics();
        assertThat(trace.getRootMetric().getNestedMetrics()).hasSize(1);
        // ordering is by total desc, so not fixed (though root metric will be first since it
        // encompasses all other timings)
        assertThat(trace.getRootMetric().getName()).isEqualTo("mock trace marker");
        assertThat(trace.getRootMetric().isActive()).isFalse();
        for (TraceMetric metric : trace.getRootMetric().getNestedMetrics()) {
            assertThat(metric.isActive()).isFalse();
        }
        assertThat(trace.getRootMetric().getNestedMetricNames())
                .containsOnly("groovy parse");
    }

    @Test
    public void testParseAndLoadWithTraceEntries() throws Exception {
        // given
        // TODO add properties?
        // container.getConfigService().setPluginProperty(PLUGIN_ID, "traceEntryForLoad", true);
        // container.getConfigService().setPluginProperty(PLUGIN_ID, "traceEntryForParse", true);
        // when
        container.executeAppUnderTest(ExecuteGroovy.class);
        // then
        Trace trace = container.getTraceService().getLastTrace();
        List<TraceEntry> entries = container.getTraceService().getEntries(trace.getId());
        assertThat(entries).hasSize(2);
        TraceEntry groovyParseEntry = entries.get(1);
        assertThat(groovyParseEntry.getMessage().getText()).isEqualTo(
                "groovy parse: hello.groovy, shouldCacheSource=false");
    }

    public static class ExecuteGroovy implements AppUnderTest, TraceMarker {
        @Override
        public void executeApp() throws Exception {
            traceMarker();
        }
        @Override
        public void traceMarker() throws Exception {
            GroovyShell shell = new GroovyShell();
            URL url = Resources.getResource("hello.groovy");
            String scriptText = Resources.toString(url, Charsets.UTF_8);
            shell.parse(scriptText, "hello.groovy");
        }
    }
}
