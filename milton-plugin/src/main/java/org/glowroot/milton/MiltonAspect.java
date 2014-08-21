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
package org.glowroot.milton;

import org.glowroot.api.ErrorMessage;
import org.glowroot.api.MessageSupplier;
import org.glowroot.api.MetricName;
import org.glowroot.api.PluginServices;
import org.glowroot.api.TraceEntry;
import org.glowroot.api.weaving.BindParameter;
import org.glowroot.api.weaving.BindTraveler;
import org.glowroot.api.weaving.IsEnabled;
import org.glowroot.api.weaving.OnAfter;
import org.glowroot.api.weaving.OnBefore;
import org.glowroot.api.weaving.Pointcut;

public class MiltonAspect {

    private static final PluginServices pluginServices = PluginServices.get("milton");

    @Pointcut(className = "com.datacert.apps.mattermanagement.milton.MiltonService",
            methodName = "logAndSendErrorEmail",
            methodParameterTypes = {"java.lang.String", "java.lang.Exception", ".."})
    public static class LogAndSendErrorEmailAdvice {
        private static final MetricName metricName =
                pluginServices.getMetricName(LogAndSendErrorEmailAdvice.class);
        @IsEnabled
        public static boolean isEnabled() {
            return pluginServices.isEnabled();
        }
        @OnBefore
        public static TraceEntry onBefore(@BindParameter String message) {
            pluginServices.setTransactionError(message);
            return pluginServices.startTraceEntry(
                    MessageSupplier.from("log webdav error: {}", message), metricName);
        }
        @OnAfter
        public static void onAfter(@BindParameter String message, @BindParameter Exception e,
                @BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithError(ErrorMessage.from(message, e));
        }
    }
}
