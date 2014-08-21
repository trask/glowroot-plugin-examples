package org.glowroot.plugin.ehcache;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.glowroot.api.PluginServices;
import org.glowroot.api.weaving.BindClassMeta;
import org.glowroot.api.weaving.BindReceiver;
import org.glowroot.api.weaving.IsEnabled;
import org.glowroot.api.weaving.OnAfter;
import org.glowroot.api.weaving.Pointcut;

public class EhcacheAspect {

    private static final PluginServices pluginServices = PluginServices.get("ehcache");

    // could check current value each time, but changing this property basically requires
    // restarting JVM to take effect next time CacheManagers are created, so may as well
    // make it totally require restarting JVM
    private static final boolean registerStatisticsMBeans =
            pluginServices.getBooleanProperty("registerStatisticsMBeans");

    @Pointcut(className = "net.sf.ehcache.CacheManager", methodName = "<init>",
            methodParameterTypes = {".."})
    public static class CacheManagerConstructorAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            return registerStatisticsMBeans;
        }
        @OnAfter
        public static void onAfter(@BindReceiver Object cacheManager,
                @BindClassMeta EhcacheInvoker ehcacheInvoker) {
            MBeanServer proxyMBeanServer =
                    new ProxyMBeanServer(ManagementFactory.getPlatformMBeanServer());
            ehcacheInvoker.registerMBeans(cacheManager, proxyMBeanServer, false, false, false,
                    true, false);
        }
    }

    @Pointcut(className = "net.sf.ehcache.Cache", methodName = "initialise",
            methodParameterTypes = {})
    public static class CacheConstructorAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            return registerStatisticsMBeans;
        }
        @OnAfter
        public static void onAfter(@BindReceiver Object cache,
                @BindClassMeta EhcacheInvoker ehcacheInvoker) {
            ehcacheInvoker.setStatisticsEnabled(cache, true);
        }
    }
}
