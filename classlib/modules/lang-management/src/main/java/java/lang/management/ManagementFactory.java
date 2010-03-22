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

package java.lang.management;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerPermission;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;

import org.apache.harmony.lang.management.ManagementUtils;
import org.apache.harmony.lang.management.MemoryManagerMXBeanImpl;
import org.apache.harmony.lang.management.OpenTypeMappingIHandler;


/**
 * <p>
 * The factory for retrieving all managed object for the JVM as well as the
 * JVM's MBean server.
 * </p>
 * 
 * @since 1.5
 */
public class ManagementFactory {
    /**
     * <p>
     * The String value of the {@link ObjectName} for {@link ClassLoadingMXBean}.
     * </p>
     */
    public static final String CLASS_LOADING_MXBEAN_NAME = "java.lang:type=ClassLoading";

    /**
     * <p>
     * The String value of the {@link ObjectName} for {@link CompilationMXBean}.
     * </p>
     */
    public static final String COMPILATION_MXBEAN_NAME = "java.lang:type=Compilation";

    /**
     * <p>
     * The String value of the {@link ObjectName} for
     * {@link GarbageCollectorMXBean}.
     * </p>
     */
    public static final String GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE = "java.lang:type=GarbageCollector";

    /**
     * <p>
     * The String value of the {@link ObjectName} for
     * {@link MemoryManagerMXBean}.
     * </p>
     */
    public static final String MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE = "java.lang:type=MemoryManager";

    /**
     * <p>
     * The String value of the {@link ObjectName} for {@link MemoryMXBean}.
     * </p>
     */
    public static final String MEMORY_MXBEAN_NAME = "java.lang:type=Memory";

    /**
     * <p>
     * The String value of the {@link ObjectName} for {@link MemoryPoolMXBean}.
     * </p>
     */
    public static final String MEMORY_POOL_MXBEAN_DOMAIN_TYPE = "java.lang:type=MemoryPool";

    /**
     * <p>
     * The String value of the {@link ObjectName} for
     * {@link OperatingSystemMXBean}.
     * </p>
     */
    public static final String OPERATING_SYSTEM_MXBEAN_NAME = "java.lang:type=OperatingSystem";

    /**
     * <p>
     * The String value of the {@link ObjectName} for {@link RuntimeMXBean}.
     * </p>
     */
    public static final String RUNTIME_MXBEAN_NAME = "java.lang:type=Runtime";

    /**
     * <p>
     * The String value of the {@link ObjectName} for {@link ThreadMXBean}.
     * </p>
     */
    public static final String THREAD_MXBEAN_NAME = "java.lang:type=Threading";

    /**
     * Reference to the MBean server. In addition to playing the role of an
     * MBean registry, the MBean server also provides a way for management
     * systems to find and utilise registered MBeans.
     */
    private static MBeanServer platformServer;

    private static Map<String, String> interfaceNameLookupTable;

    private static Set<String> multiInstanceBeanNames;

    static {
        interfaceNameLookupTable = new HashMap<String, String>();
        // Public API types
        interfaceNameLookupTable.put("java.lang.management.ClassLoadingMXBean",
                CLASS_LOADING_MXBEAN_NAME);
        interfaceNameLookupTable.put("java.lang.management.MemoryMXBean",
                MEMORY_MXBEAN_NAME);
        interfaceNameLookupTable.put("java.lang.management.ThreadMXBean",
                THREAD_MXBEAN_NAME);
        interfaceNameLookupTable.put("java.lang.management.RuntimeMXBean",
                RUNTIME_MXBEAN_NAME);
        interfaceNameLookupTable.put(
                "java.lang.management.OperatingSystemMXBean",
                OPERATING_SYSTEM_MXBEAN_NAME);
        interfaceNameLookupTable.put("java.lang.management.CompilationMXBean",
                COMPILATION_MXBEAN_NAME);
        interfaceNameLookupTable.put(
                "java.lang.management.GarbageCollectorMXBean",
                GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE);
        interfaceNameLookupTable.put(
                "java.lang.management.MemoryManagerMXBean",
                MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE);
        interfaceNameLookupTable.put("java.lang.management.MemoryPoolMXBean",
                MEMORY_POOL_MXBEAN_DOMAIN_TYPE);

        multiInstanceBeanNames = new HashSet<String>();
        multiInstanceBeanNames
                .add("java.lang.management.GarbageCollectorMXBean");
        multiInstanceBeanNames.add("java.lang.management.MemoryManagerMXBean");
        multiInstanceBeanNames.add("java.lang.management.MemoryPoolMXBean");
    }

    /**
     * Private constructor ensures that this class cannot be instantiated by
     * users.
     */
    private ManagementFactory() {
        // NO OP
    }

    /**
     * Returns the singleton <code>MXBean</code> for the virtual machine's
     * class loading system.
     * 
     * @return the virtual machine's {@link ClassLoadingMXBean}
     */
    public static ClassLoadingMXBean getClassLoadingMXBean() {
        return ManagementUtils.getClassLoadingBean();
    }

    /**
     * Returns the singleton <code>MXBean</code> for the virtual machine's
     * compilation system <i>if and only if the virtual machine has a
     * compilation system enabled </i>. If no compilation exists for this
     * virtual machine, a <code>null</code> is returned.
     * 
     * @return the virtual machine's {@link CompilationMXBean}or
     *         <code>null</code> if there is no compilation system for this
     *         virtual machine.
     */
    public static CompilationMXBean getCompilationMXBean() {
        return ManagementUtils.getCompliationBean();
    }

    /**
     * Returns a list of all of the instances of {@link GarbageCollectorMXBean}
     * in this virtual machine. Owing to the dynamic nature of this kind of
     * <code>MXBean</code>, it is possible that instances may be created or
     * destroyed between the invocation and return of this method.
     * 
     * @return a list of all known <code>GarbageCollectorMXBean</code> s in
     *         this virtual machine.
     */
    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return ManagementUtils.getGarbageCollectorMXBeans();
    }

    /**
     * Returns a list of all of the instances of {@link MemoryManagerMXBean}in
     * this virtual machine. Owing to the dynamic nature of this kind of
     * <code>MXBean</code>, it is possible that instances may be created or
     * destroyed between the invocation and return of this method.
     * <p>
     * Note that the list of <code>MemoryManagerMXBean</code> instances will
     * include instances of <code>MemoryManagerMXBean</code> sub-types such as
     * <code>GarbageCollectorMXBean</code>.
     * </p>
     * 
     * @return a list of all known <code>MemoryManagerMXBean</code> s in this
     *         virtual machine.
     */
    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return ManagementUtils.getMemoryManagerMXBeans();
    }

    /**
     * Returns the singleton <code>MXBean</code> for the virtual machine's
     * memory system.
     * 
     * @return the virtual machine's {@link MemoryMXBean}
     */
    public static MemoryMXBean getMemoryMXBean() {
        return ManagementUtils.getMemoryBean();
    }

    /**
     * Returns a list of all of the instances of {@link MemoryPoolMXBean}in
     * this virtual machine. Owing to the dynamic nature of this kind of
     * <code>MXBean</code>, it is possible that instances may be created or
     * destroyed between the invocation and return of this method.
     * 
     * @return a list of all known <code>MemoryPoolMXBean</code> s in this
     *         virtual machine.
     */
    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return ManagementUtils.getMemoryPoolMXBeans();
    }

    /**
     * Returns the singleton <code>MXBean</code> for the operating system
     * which the virtual machine runs on.
     * 
     * @return the virtual machine's {@link OperatingSystemMXBean}
     */
    public static OperatingSystemMXBean getOperatingSystemMXBean() {
        return ManagementUtils.getOperatingSystemBean();
    }

    /**
     * Returns a reference to the virtual machine's platform
     * <code>MBeanServer</code>. This <code>MBeanServer</code> will have
     * all of the platform <code>MXBean</code> s registered with it including
     * any dynamic <code>MXBean</code> s (e.g. instances of
     * {@link GarbageCollectorMXBean}that may be unregistered and destroyed at
     * a later time.
     * <p>
     * In order to simplify the process of distribution and discovery of managed
     * beans it is good practice to register all managed beans (in addition to
     * the platform <code>MXBean</code>s) with this server.
     * </p>
     * <p>
     * A custom <code>MBeanServer</code> can be created by this method if the
     * System property <code>javax.management.builder.initial</code> has been
     * set with the fully qualified name of a subclass of
     * {@link javax.management.MBeanServerBuilder}.
     * </p>
     * 
     * @return the platform <code>MBeanServer</code>.
     * @throws SecurityException
     *             if there is a Java security manager in operation and the
     *             caller of this method does not have
     *             &quot;createMBeanServer&quot;
     *             <code>MBeanServerPermission</code>.
     * @see MBeanServer
     * @see javax.management.MBeanServerPermission
     */
    public static MBeanServer getPlatformMBeanServer() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new MBeanServerPermission(
                    "createMBeanServer"));
        }

        synchronized (ManagementFactory.class) {
            if (platformServer == null) {
                platformServer = MBeanServerFactory.createMBeanServer();

                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        registerPlatformBeans(platformServer);
                        return null;
                    }// end method run
                });
            }
        }// end synchronized
        return platformServer;
    }

    /**
     * Returns the singleton <code>MXBean</code> for the virtual machine's
     * runtime system.
     * 
     * @return the virtual machine's {@link RuntimeMXBean}
     */
    public static RuntimeMXBean getRuntimeMXBean() {
        return ManagementUtils.getRuntimeBean();
    }

    /**
     * Returns the singleton <code>MXBean</code> for the virtual machine's
     * threading system.
     * 
     * @return the virtual machine's {@link ThreadMXBean}
     */
    public static ThreadMXBean getThreadMXBean() {
        return ManagementUtils.getThreadBean();
    }

    /**
     * @param <T>
     * @param connection
     * @param mxbeanName
     * @param mxbeanInterface
     * @return a new proxy object representing the named <code>MXBean</code>.
     *         All subsequent method invocations on the proxy will be routed
     *         through the supplied {@link MBeanServerConnection} object.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static <T> T newPlatformMXBeanProxy(MBeanServerConnection connection,
            String mxbeanName, Class<T> mxbeanInterface) throws IOException {
        // Check that the named object implements the specified interface
        verifyNamedMXBean(mxbeanName, mxbeanInterface);

        T result = null;
        Class[] interfaces = null;
        if (ManagementUtils.isANotificationEmitter(mxbeanInterface)) {
            // Proxies of the MemoryMXBean and OperatingSystemMXBean interfaces
            // must also implement the NotificationEmitter interface.
            interfaces = new Class[] { mxbeanInterface,
                    NotificationEmitter.class };
        } else {
            interfaces = new Class[] { mxbeanInterface };
        }

        result = (T) Proxy.newProxyInstance(interfaces[0].getClassLoader(),
                interfaces, new OpenTypeMappingIHandler(connection,
                        mxbeanInterface.getName(), mxbeanName));
        return result;
    }

    /**
     * @param mxbeanName
     * @param mxbeanInterface
     */
    private static void verifyNamedMXBean(String mxbeanName,
            Class<?> mxbeanInterface) {
        String mxbeanInterfaceName = mxbeanInterface.getName();
        String expectedObjectName = interfaceNameLookupTable
                .get(mxbeanInterfaceName);
        if (multiInstanceBeanNames.contains(mxbeanInterfaceName)) {
            // partial match is good enough
            if (!mxbeanName.startsWith(expectedObjectName)) {
                throw new IllegalArgumentException(mxbeanName
                        + " is not an instance of interface "
                        + mxbeanInterfaceName);
            }
        } else {
            // exact match required
            if (!expectedObjectName.equals(mxbeanName)) {
                throw new IllegalArgumentException(mxbeanName
                        + " is not an instance of interface "
                        + mxbeanInterfaceName);
            }
        }
    }

    /**
     * Register the singleton platform MXBeans :
     * <ul>
     * <li>ClassLoadingMXBean
     * <li>MemoryMXBean
     * <li>ThreadMXBean
     * <li>RuntimeMXBean
     * <li>OperatingSystemMXBean
     * <li>CompilationMXBean ( <i>only if the VM has a compilation system </i>)
     * </ul>
     * <p>
     * This method will be called only once in the lifetime of the virtual
     * machine, at the point where the singleton platform MBean server has been
     * created.
     * 
     * @param platformServer
     *            the platform <code>MBeanServer</code> for this virtual
     *            machine.
     */
    private static void registerPlatformBeans(MBeanServer platformServer) {
        try {
            ObjectName oName = new ObjectName(CLASS_LOADING_MXBEAN_NAME);
            if (!platformServer.isRegistered(oName)) {
                platformServer.registerMBean(ManagementUtils
                        .getClassLoadingBean(), oName);
            }

            oName = new ObjectName(LogManager.LOGGING_MXBEAN_NAME);
            if (!platformServer.isRegistered(oName)) {
                platformServer.registerMBean(ManagementUtils.getLoggingBean(),
                        oName);
            }

            oName = new ObjectName(MEMORY_MXBEAN_NAME);
            if (!platformServer.isRegistered(oName)) {
                platformServer.registerMBean(ManagementUtils.getMemoryBean(),
                        oName);
            }

            oName = new ObjectName(THREAD_MXBEAN_NAME);
            if (!platformServer.isRegistered(oName)) {
                platformServer.registerMBean(ManagementUtils.getThreadBean(),
                        oName);
            }

            oName = new ObjectName(RUNTIME_MXBEAN_NAME);
            if (!platformServer.isRegistered(oName)) {
                platformServer.registerMBean(ManagementUtils.getRuntimeBean(),
                        oName);
            }

            oName = new ObjectName(OPERATING_SYSTEM_MXBEAN_NAME);
            if (!platformServer.isRegistered(oName)) {
                platformServer.registerMBean(ManagementUtils
                        .getOperatingSystemBean(), oName);
            }

            // If there is no JIT compiler available, there will be no
            // compilation MXBean.
            CompilationMXBean cBean = ManagementUtils.getCompliationBean();
            if (cBean != null) {
                oName = new ObjectName(COMPILATION_MXBEAN_NAME);
                if (!platformServer.isRegistered(oName)) {
                    platformServer.registerMBean(cBean, oName);
                }
            }// end if compilation bean is not null

            // Carry out the initial registration of Dynamic MXBeans.
            List<MemoryPoolMXBean> mpBeanList = ManagementUtils
                    .getMemoryPoolMXBeans();
            if (mpBeanList != null) {
                Iterator<MemoryPoolMXBean> mpIt = mpBeanList.iterator();
                while (mpIt.hasNext()) {
                    MemoryPoolMXBean mpBean = mpIt.next();
                    oName = new ObjectName(MEMORY_POOL_MXBEAN_DOMAIN_TYPE
                            + ",name=" + mpBean.getName());
                    if (!platformServer.isRegistered(oName)) {
                        platformServer.registerMBean(mpBean, oName);
                    }
                }// end while
            }

            List<GarbageCollectorMXBean> gcBeanList = ManagementUtils
                    .getGarbageCollectorMXBeans();
            if (gcBeanList != null) {
                Iterator<GarbageCollectorMXBean> gcIt = gcBeanList.iterator();
                while (gcIt.hasNext()) {
                    GarbageCollectorMXBean gcBean = gcIt.next();
                    oName = new ObjectName(GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE
                            + ",name=" + gcBean.getName());
                    if (!platformServer.isRegistered(oName)) {
                        platformServer.registerMBean(gcBean, oName);
                    }
                }// end while
            }

            // Careful ! The getMemoryManagerMXBeans call returns objects that
            // are memory managers (with the MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE
            // prefix in their object name) *and* garbage collectors (with the
            // GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE prefix in their object
            // name).
            // This is because garbage collector platform beans extend
            // memory manager platform beans and so qualify for inclusion in
            // the list of memory manager beans.
            List<MemoryManagerMXBean> mmBeanList = ManagementUtils
                    .getMemoryManagerMXBeans();
            if (mmBeanList != null) {
                Iterator<MemoryManagerMXBean> mmIt = mmBeanList.iterator();
                while (mmIt.hasNext()) {
                    MemoryManagerMXBean mmBean = mmIt.next();
                    // Test the bean's runtime class. Only register this bean
                    // if it's runtime type is MemoryManagerMXBeanImpl.
                    if (mmBean.getClass().equals(MemoryManagerMXBeanImpl.class)) {
                        oName = new ObjectName(
                                MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE + ",name="
                                        + mmBean.getName());
                        if (!platformServer.isRegistered(oName)) {
                            platformServer.registerMBean(mmBean, oName);
                        }
                    }
                }// end while
            }
        } catch (InstanceAlreadyExistsException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        } catch (MBeanRegistrationException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        } catch (NotCompliantMBeanException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        } catch (MalformedObjectNameException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        } catch (NullPointerException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
    }
}
