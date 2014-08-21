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
package org.glowroot.plugin.tomcat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ning.http.client.AsyncHttpClient;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.jasper.servlet.JspServlet;
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

import static org.assertj.core.api.Assertions.assertThat;

public class TomcatPluginTest {

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
    public void testJspCompile() throws Exception {
        // given
        // when
        container.executeAppUnderTest(MyServlet.class);
        // then
        Trace trace = container.getTraceService().getLastTrace();
        List<TraceEntry> entries = container.getTraceService().getEntries(trace.getId());
        assertThat(entries).hasSize(2);
        assertThat(entries.get(1).getMessage().getText()).isEqualTo("tomcat compile:"
                + " /WEB-INF/jsps/test.jsp");
    }

    @SuppressWarnings("serial")
    public static class MyServlet extends TomcatContainer implements AppUnderTest {

        @Override
        protected void serviceInternal(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            req.getRequestDispatcher("/WEB-INF/jsps/test.jsp").forward(req, resp);
        }

        @Override
        public void executeApp() throws Exception {
            startTomcat();
            AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
            asyncHttpClient.prepareGet("http://localhost:8081/hello").execute().get();
            asyncHttpClient.close();
            stopTomcat();
        }
    }

    @SuppressWarnings("serial")
    private abstract static class TomcatContainer extends HttpServlet {

        private Context context;
        private Tomcat tomcat;

        protected abstract void serviceInternal(final HttpServletRequest req,
                HttpServletResponse resp) throws ServletException, IOException;

        @Override
        protected void service(final HttpServletRequest req, final HttpServletResponse resp)
                throws ServletException, IOException {

            TraceMarker traceMarker = new TraceMarker() {
                @Override
                public void traceMarker() throws Exception {
                    serviceInternal(req, resp);
                }
            };
            try {
                traceMarker.traceMarker();
            } catch (ServletException e) {
                throw e;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        protected void startTomcat() throws LifecycleException {
            tomcat = new Tomcat();
            tomcat.setPort(8081);
            context = tomcat.addContext("", new File("src/test/resources").getAbsolutePath());

            WebappLoader webappLoader = new WebappLoader(TomcatContainer.class.getClassLoader());
            // this is needed in order to override 'system' class loader, unfortunately, setting the
            // parent and setting delegate = true is not enough
            webappLoader.setLoaderClass("org.glowroot.plugin.tomcat.TomcatClassLoader");
            context.setLoader(webappLoader);

            Tomcat.addServlet(context, "hello", this);
            context.addServletMapping("/hello", "hello");

            Tomcat.addServlet(context, "jsp", new JspServlet());
            context.addServletMapping("*.jsp", "jsp");

            tomcat.start();
        }

        protected void stopTomcat() throws LifecycleException {
            tomcat.stop();
            tomcat.destroy();
        }
    }
}
