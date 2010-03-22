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

package org.apache.harmony.lang.management;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LoggingMXBean;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

/**
 * Support methods for org.apache.harmony.lang.management classes.
 * 
 */
public class ManagementUtils {

    private static Map<String, MBeanInfo> infoMap = buildInfoMap();

    private static CompositeType MEMORYUSAGE_COMPOSITETYPE;

    private static CompositeType MEMORYNOTIFICATIONINFO_COMPOSITETYPE;

    private static CompositeType THREADINFO_COMPOSITETYPE;

    private static CompositeType STACKTRACEELEMENT_COMPOSITETYPE;

    /**
     * System property setting used to decide if non-fatal exceptions should be
     * written out to console.
     */
    public static final boolean VERBOSE_MODE = checkVerboseProperty();

    /**
     * @return the singleton <code>ClassLoadingMXBean</code> instance.
     */
    public static ClassLoadingMXBeanImpl getClassLoadingBean() {
        return ClassLoadingMXBeanImpl.getInstance();
    }

    /**
     * @return boolean indication of whether or not the system property
     *         <code>org.apache.harmony.lang.management.verbose</code> has been set.
     */
    private static boolean checkVerboseProperty() {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return System.getProperty("org.apache.harmony.lang.management.verbose") != null;
            }// end method run
        });
    }

    /**
     * Convenenience method to return the {@link MBeanInfo} object that
     * corresponds to the specified <code>MXBean</code> type.
     * 
     * @param name
     *            the fully qualified name of an <code>MXBean</code>
     * @return if <code>name</code> has the value of a known
     *         <code>MXBean</code> type then returns the
     *         <code>MBeanInfo</code> meta data for that type. If
     *         <code>name</code> is not the name of a known
     *         <code>MXBean</code> kind then returns <code>null</code>.
     */
    static MBeanInfo getMBeanInfo(String name) {
        return infoMap.get(name);
    }

    /**
     * Builds a <code>Map</code> of all the {@link MBeanInfo} instances for
     * each of the platform beans. The map is keyed off the name of each bean
     * type.
     * 
     * @return a <code>Map</code> of all the platform beans'
     *         <code>MBeanInfo</code> instances.
     */
    private static Map<String, MBeanInfo> buildInfoMap() {
        HashMap<String, MBeanInfo> map = new HashMap<String, MBeanInfo>();
        addClassLoadingInfo(map);
        addCompilationBeanInfo(map);
        addLoggingBeanInfo(map);
        addMemoryManagerBeanInfo(map);
        addGarbageCollectorBeanInfo(map);
        addMemoryBeanInfo(map);
        addMemoryPoolBeanInfo(map);
        addOperatingSystemBeanInfo(map);
        addRuntimeBeanInfo(map);
        addThreadBeanInfo(map);
        return map;
    }

    /**
     * Creates the metadata for the {@link java.lang.management.ThreadMXBean}.
     * For this type of platform bean the metadata covers :
     * <ul>
     * <li>12 attributes
     * <li>0 constructors
     * <li>8 operations
     * <li>0 notifications
     * </ul>
     * 
     * @param map
     */
    private static void addThreadBeanInfo(HashMap<String, MBeanInfo> map) {
        // Attributes
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[12];
        attributes[0] = new MBeanAttributeInfo("AllThreadIds", "[J",
                "AllThreadIds", true, false, false);

        attributes[1] = new MBeanAttributeInfo("CurrentThreadCpuTime",
                Long.TYPE.getName(), "CurrentThreadCpuTime", true, false, false);

        attributes[2] = new MBeanAttributeInfo("CurrentThreadUserTime",
                Long.TYPE.getName(), "CurrentThreaduserTime", true, false,
                false);

        attributes[3] = new MBeanAttributeInfo("DaemonThreadCount",
                Integer.TYPE.getName(), "DaemonThreadCount", true, false, false);

        attributes[4] = new MBeanAttributeInfo("PeakThreadCount", Integer.TYPE
                .getName(), "PeakThreadCount", true, false, false);

        attributes[5] = new MBeanAttributeInfo("ThreadCount", Integer.TYPE
                .getName(), "ThreadCount", true, false, false);

        attributes[6] = new MBeanAttributeInfo("TotalStartedThreadCount",
                Long.TYPE.getName(), "TotalStartedThreadCount", true, false,
                false);

        attributes[7] = new MBeanAttributeInfo("CurrentThreadCpuTimeSupported",
                Boolean.TYPE.getName(), "CurrentThreadCpuTimeSupported", true,
                false, true);

        attributes[8] = new MBeanAttributeInfo(
                "ThreadContentionMonitoringEnabled", Boolean.TYPE.getName(),
                "ThreadContentionMonitoringEnabled", true, true, true);

        attributes[9] = new MBeanAttributeInfo(
                "ThreadContentionMonitoringSupported", Boolean.TYPE.getName(),
                "ThreadContentionMonitoringSupported", true, false, true);

        attributes[10] = new MBeanAttributeInfo("ThreadCpuTimeEnabled",
                Boolean.TYPE.getName(), "ThreadCpuTimeEnabled", true, true,
                true);

        attributes[11] = new MBeanAttributeInfo("ThreadCpuTimeSupported",
                Boolean.TYPE.getName(), "ThreadCpuTimeSupported", true, false,
                true);

        // Operations
        MBeanOperationInfo[] operations = new MBeanOperationInfo[8];
        MBeanParameterInfo[] nullParams = new MBeanParameterInfo[0];

        operations[0] = new MBeanOperationInfo("findMonitorDeadlockedThreads",
                "findMonitorDeadlockedThreads", nullParams, "[J",
                MBeanOperationInfo.ACTION_INFO);

        {
            MBeanParameterInfo[] params = new MBeanParameterInfo[1];
            params[0] = new MBeanParameterInfo("id", Long.TYPE.getName(), "id");
            operations[1] = new MBeanOperationInfo("getThreadCpuTime",
                    "getThreadCpuTime", params, Long.TYPE.getName(),
                    MBeanOperationInfo.ACTION_INFO);
        }

        {
            MBeanParameterInfo[] params = new MBeanParameterInfo[1];
            params[0] = new MBeanParameterInfo("id", Long.TYPE.getName(), "id");
            operations[2] = new MBeanOperationInfo("getThreadInfo",
                    "getThreadInfo", params, CompositeData.class.getName(),
                    MBeanOperationInfo.ACTION_INFO);
        }

        {
            MBeanParameterInfo[] params = new MBeanParameterInfo[1];
            params[0] = new MBeanParameterInfo("ids", "[J", "ids");
            operations[3] = new MBeanOperationInfo("getThreadInfo",
                    "getThreadInfo", params, "[L"
                            + CompositeData.class.getName() + ";",
                    MBeanOperationInfo.ACTION_INFO);
        }

        {
            MBeanParameterInfo[] params = new MBeanParameterInfo[2];
            params[0] = new MBeanParameterInfo("ids", "[J", "ids");
            params[1] = new MBeanParameterInfo("maxDepth", Integer.TYPE
                    .getName(), "maxDepth");
            operations[4] = new MBeanOperationInfo("getThreadInfo",
                    "getThreadInfo", params, "[L"
                            + CompositeData.class.getName() + ";",
                    MBeanOperationInfo.ACTION_INFO);
        }

        {
            MBeanParameterInfo[] params = new MBeanParameterInfo[2];
            params[0] = new MBeanParameterInfo("id", Long.TYPE.getName(), "id");
            params[1] = new MBeanParameterInfo("maxDepth", Integer.TYPE
                    .getName(), "maxDepth");
            operations[5] = new MBeanOperationInfo("getThreadInfo",
                    "getThreadInfo", params, CompositeData.class.getName(),
                    MBeanOperationInfo.ACTION_INFO);
        }

        {
            MBeanParameterInfo[] params = new MBeanParameterInfo[1];
            params[0] = new MBeanParameterInfo("id", Long.TYPE.getName(), "id");
            operations[6] = new MBeanOperationInfo("getThreadUserTime",
                    "getThreadUserTime", params, Long.TYPE.getName(),
                    MBeanOperationInfo.ACTION_INFO);
        }

        operations[7] = new MBeanOperationInfo("resetPeakThreadCount",
                "resetPeakThreadCount", nullParams, Void.TYPE.getName(),
                MBeanOperationInfo.ACTION_INFO);

        map.put(ThreadMXBean.class.getName(), new MBeanInfo(
                ThreadMXBeanImpl.class.getName(), ThreadMXBeanImpl.class
                        .getName(), attributes, null, operations, null));
    }

    /**
     * Creates the metadata for the {@link java.lang.management.RuntimeMXBean}.
     * For this type of platform bean the metadata covers :
     * <ul>
     * <li>16 attributes
     * <li>0 constructors
     * <li>0 operations
     * <li>0 notifications
     * </ul>
     * 
     * @param map
     */
    private static void addRuntimeBeanInfo(HashMap<String, MBeanInfo> map) {
        // Attributes
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[16];
        attributes[0] = new MBeanAttributeInfo("BootClassPath", String.class
                .getName(), "BootClassPath", true, false, false);
        attributes[1] = new MBeanAttributeInfo("ClassPath", String.class
                .getName(), "ClassPath", true, false, false);
        attributes[2] = new MBeanAttributeInfo("InputArguments",
                "[Ljava.lang.String;", "InputArguments", true, false, false);
        attributes[3] = new MBeanAttributeInfo("LibraryPath", String.class
                .getName(), "LibraryPath", true, false, false);
        attributes[4] = new MBeanAttributeInfo("ManagementSpecVersion",
                String.class.getName(), "ManagementSpecVersion", true, false,
                false);
        attributes[5] = new MBeanAttributeInfo("Name", String.class.getName(),
                "Name", true, false, false);
        attributes[6] = new MBeanAttributeInfo("SpecName", String.class
                .getName(), "SpecName", true, false, false);
        attributes[7] = new MBeanAttributeInfo("SpecVendor", String.class
                .getName(), "SpecVendor", true, false, false);
        attributes[8] = new MBeanAttributeInfo("SpecVersion", String.class
                .getName(), "SpecVersion", true, false, false);
        attributes[9] = new MBeanAttributeInfo("StartTime",
                Long.TYPE.getName(), "StartTime", true, false, false);
        attributes[10] = new MBeanAttributeInfo("SystemProperties",
                TabularData.class.getName(), "SystemProperties", true, false,
                false);
        attributes[11] = new MBeanAttributeInfo("Uptime", Long.TYPE.getName(),
                "Uptime", true, false, false);
        attributes[12] = new MBeanAttributeInfo("VmName", String.class
                .getName(), "VmName", true, false, false);
        attributes[13] = new MBeanAttributeInfo("VmVendor", String.class
                .getName(), "VmVendor", true, false, false);
        attributes[14] = new MBeanAttributeInfo("VmVersion", String.class
                .getName(), "VmVersion", true, false, false);
        attributes[15] = new MBeanAttributeInfo("BootClassPathSupported",
                Boolean.TYPE.getName(), "BootClassPathSupported", true, false,
                true);
        map.put(RuntimeMXBean.class.getName(), new MBeanInfo(
                RuntimeMXBeanImpl.class.getName(), RuntimeMXBeanImpl.class
                        .getName(), attributes, null, null, null));
    }

    /**
     * Creates the metadata for the
     * {@link java.lang.management.OperatingSystemMXBean}. For this type of
     * platform bean the metadata covers :
     * <ul>
     * <li>4 attributes
     * <li>0 constructors
     * <li>0 operations
     * <li>0 notifications
     * </ul>
     * 
     * @param map
     */
    private static void addOperatingSystemBeanInfo(
            HashMap<String, MBeanInfo> map) {
        // Attributes
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[4];
        // Standard attributes...
        attributes[0] = new MBeanAttributeInfo("Arch", String.class.getName(),
                "Arch", true, false, false);
        attributes[1] = new MBeanAttributeInfo("AvailableProcessors",
                Integer.TYPE.getName(), "AvailableProcessors", true, false,
                false);
        attributes[2] = new MBeanAttributeInfo("Name", String.class.getName(),
                "Name", true, false, false);
        attributes[3] = new MBeanAttributeInfo("Version", String.class
                .getName(), "Version", true, false, false);

        // No operations

        // No notifications

        MBeanInfo mbeanInfo = new MBeanInfo(OperatingSystemMXBeanImpl.class
                .getName(), OperatingSystemMXBeanImpl.class.getName(),
                attributes, null, null, null);

        map.put(OperatingSystemMXBean.class.getName(), mbeanInfo);
    }

    /**
     * Creates the metadata for the
     * {@link java.lang.management.MemoryPoolMXBean}. For this type of platform
     * bean the metadata covers :
     * <ul>
     * <li>15 attributes
     * <li>0 constructors
     * <li>1 operations
     * <li>0 notifications
     * </ul>
     * 
     * @param map
     */
    private static void addMemoryPoolBeanInfo(HashMap<String, MBeanInfo> map) {
        // Attributes
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[15];
        attributes[0] = new MBeanAttributeInfo("CollectionUsage",
                CompositeData.class.getName(), "CollectionUsage", true, false,
                false);
        attributes[1] = new MBeanAttributeInfo("CollectionUsageThreshold",
                Long.TYPE.getName(), "CollectionUsageThreshold", true, true,
                false);
        attributes[2] = new MBeanAttributeInfo("CollectionUsageThresholdCount",
                Long.TYPE.getName(), "CollectionUsageThresholdCount", true,
                false, false);
        attributes[3] = new MBeanAttributeInfo("MemoryManagerNames",
                "[Ljava.lang.String;", "MemoryManagerNames", true, false, false);
        attributes[4] = new MBeanAttributeInfo("Name", String.class.getName(),
                "Name", true, false, false);
        attributes[5] = new MBeanAttributeInfo("PeakUsage", CompositeData.class
                .getName(), "PeakUsage", true, false, false);
        attributes[6] = new MBeanAttributeInfo("Type", String.class.getName(),
                "Type", true, false, false);
        attributes[7] = new MBeanAttributeInfo("Usage", CompositeData.class
                .getName(), "Usage", true, false, false);
        attributes[8] = new MBeanAttributeInfo("UsageThreshold", Long.TYPE
                .getName(), "UsageThreshold", true, true, false);
        attributes[9] = new MBeanAttributeInfo("UsageThresholdCount", Long.TYPE
                .getName(), "UsageThresholdCount", true, false, false);
        attributes[10] = new MBeanAttributeInfo(
                "CollectionUsageThresholdExceeded", Boolean.TYPE.getName(),
                "CollectionUsageThresholdExceeded", true, false, true);
        attributes[11] = new MBeanAttributeInfo(
                "CollectionUsageThresholdSupported", Boolean.TYPE.getName(),
                "CollectionUsageThresholdSupported", true, false, true);
        attributes[12] = new MBeanAttributeInfo("UsageThresholdExceeded",
                Boolean.TYPE.getName(), "UsageThresholdExceeded", true, false,
                true);
        attributes[13] = new MBeanAttributeInfo("UsageThresholdSupported",
                Boolean.TYPE.getName(), "UsageThresholdSupported", true, false,
                true);
        attributes[14] = new MBeanAttributeInfo("Valid",
                Boolean.TYPE.getName(), "Valid", true, false, true);

        // Operations
        MBeanOperationInfo[] operations = new MBeanOperationInfo[1];
        MBeanParameterInfo[] params = new MBeanParameterInfo[0];
        operations[0] = new MBeanOperationInfo("resetPeakUsage",
                "resetPeakUsage", params, Void.TYPE.getName(),
                MBeanOperationInfo.ACTION_INFO);

        MBeanInfo mbeanInfo = new MBeanInfo(MemoryPoolMXBeanImpl.class
                .getName(), MemoryPoolMXBeanImpl.class.getName(), attributes,
                null, operations, null);
        map.put(MemoryPoolMXBean.class.getName(), mbeanInfo);
    }

    /**
     * Creates the metadata for the {@link java.lang.management.MemoryMXBean}.
     * For this type of platform bean the metadata covers :
     * <ul>
     * <li>4 attributes
     * <li>0 constructors
     * <li>1 operations
     * <li>1 notification
     * </ul>
     * 
     * @param map
     */
    private static void addMemoryBeanInfo(HashMap<String, MBeanInfo> map) {
        // Attributes
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[4];
        attributes[0] = new MBeanAttributeInfo("HeapMemoryUsage",
                CompositeData.class.getName(), "HeapMemoryUsage", true, false,
                false);
        attributes[1] = new MBeanAttributeInfo("NonHeapMemoryUsage",
                CompositeData.class.getName(), "NonHeapMemoryUsage", true,
                false, false);
        attributes[2] = new MBeanAttributeInfo(
                "ObjectPendingFinalizationCount", Integer.TYPE.getName(),
                "ObjectPendingFinalizationCount", true, false, false);
        attributes[3] = new MBeanAttributeInfo("Verbose", Boolean.TYPE
                .getName(), "Verbose", true, true, true);

        // Operations
        MBeanOperationInfo[] operations = new MBeanOperationInfo[1];
        MBeanParameterInfo[] params = new MBeanParameterInfo[0];
        operations[0] = new MBeanOperationInfo("gc", "gc", params, Void.TYPE
                .getName(), MBeanOperationInfo.ACTION_INFO);

        // Notifications
        MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[1];
        String[] notifTypes = new String[2];
        notifTypes[0] = MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED;
        notifTypes[1] = MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED;
        notifications[0] = new MBeanNotificationInfo(notifTypes,
                javax.management.Notification.class.getName(),
                "Memory Notification");

        MBeanInfo mbeanInfo = new MBeanInfo(MemoryMXBeanImpl.class.getName(),
                MemoryMXBeanImpl.class.getName(), attributes, null, operations,
                notifications);
        map.put(MemoryMXBean.class.getName(), mbeanInfo);
    }

    /**
     * Creates the metadata for the
     * {@link java.lang.management.MemoryManagerMXBean}. For this type of
     * platform bean the metadata covers :
     * <ul>
     * <li>3 attribute
     * <li>0 constructors
     * <li>0 operations
     * <li>0 notifications
     * </ul>
     * 
     * @param map
     */
    private static void addMemoryManagerBeanInfo(HashMap<String, MBeanInfo> map) {
        // Attributes
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[3];
        attributes[0] = new MBeanAttributeInfo("MemoryPoolNames",
                "[Ljava.lang.String;", "MemoryPoolNames", true, false, false);
        attributes[1] = new MBeanAttributeInfo("Name", String.class.getName(),
                "Name", true, false, false);
        attributes[2] = new MBeanAttributeInfo("Valid", Boolean.TYPE.getName(),
                "Valid", true, false, true);

        map.put(MemoryManagerMXBean.class.getName(), new MBeanInfo(
                MemoryManagerMXBeanImpl.class.getName(),
                MemoryManagerMXBeanImpl.class.getName(), attributes, null,
                null, null));
    }

    /**
     * Creates the metadata for the {@link java.util.logging.LoggingMXBean}.
     * For this type of platform bean the metadata covers :
     * <ul>
     * <li>1 attribute
     * <li>0 constructors
     * <li>3 operations
     * <li>0 notifications
     * </ul>
     * 
     * @param result
     */
    private static void addLoggingBeanInfo(HashMap<String, MBeanInfo> result) {
        // Attributes
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[1];
        attributes[0] = new MBeanAttributeInfo("LoggerNames",
                "[Ljava.lang.String;", "LoggerNames", true, false, false);

        // Operations
        MBeanOperationInfo[] operations = new MBeanOperationInfo[3];

        {
            MBeanParameterInfo[] params = new MBeanParameterInfo[1];
            params[0] = new MBeanParameterInfo("loggerName", String.class
                    .getName(), "loggerName");
            operations[0] = new MBeanOperationInfo("getLoggerLevel",
                    "getLoggerLevel", params, String.class.getName(),
                    MBeanOperationInfo.ACTION_INFO);
        }

        {
            MBeanParameterInfo[] params = new MBeanParameterInfo[1];
            params[0] = new MBeanParameterInfo("loggerName", String.class
                    .getName(), "loggerName");
            operations[1] = new MBeanOperationInfo("getParentLoggerName",
                    "getParentLoggerName", params, String.class.getName(),
                    MBeanOperationInfo.ACTION_INFO);
        }

        {
            MBeanParameterInfo[] params = new MBeanParameterInfo[2];
            params[0] = new MBeanParameterInfo("loggerName", String.class
                    .getName(), "loggerName");
            params[1] = new MBeanParameterInfo("levelName", String.class
                    .getName(), "levelName");
            operations[2] = new MBeanOperationInfo("setLoggerLevel",
                    "setLoggerLevel", params, Void.TYPE.getName(),
                    MBeanOperationInfo.ACTION_INFO);
        }
        result.put(LoggingMXBean.class.getName(), new MBeanInfo(
                LoggingMXBeanImpl.class.getName(), LoggingMXBeanImpl.class
                        .getName(), attributes, null, operations, null));
    }

    /**
     * Creates the metadata for the {@link GarbageCollectorMXBean}. For this
     * type of platform bean the metadata covers :
     * <ul>
     * <li>5 attributes
     * <li>0 constructors
     * <li>0 operations
     * <li>0 notifications
     * </ul>
     * 
     * @param map
     */
    private static void addGarbageCollectorBeanInfo(
            HashMap<String, MBeanInfo> map) {
        // Note that GarbageCollectorMXBean extends MemoryManagerMXBean.
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[5];

        // Standard attributes...
        attributes[0] = new MBeanAttributeInfo("MemoryPoolNames",
                "[Ljava.lang.String;", "MemoryPoolNames", true, false, false);
        attributes[1] = new MBeanAttributeInfo("Name", String.class.getName(),
                "Name", true, false, false);
        attributes[2] = new MBeanAttributeInfo("Valid", Boolean.TYPE.getName(),
                "Valid", true, false, true);
        attributes[3] = new MBeanAttributeInfo("CollectionCount", Long.TYPE
                .getName(), "CollectionCount", true, false, false);
        attributes[4] = new MBeanAttributeInfo("CollectionTime", Long.TYPE
                .getName(), "CollectionTime", true, false, false);

        MBeanInfo mbeanInfo = new MBeanInfo(GarbageCollectorMXBeanImpl.class
                .getName(), GarbageCollectorMXBeanImpl.class.getName(),
                attributes, null, null, null);
        map.put(GarbageCollectorMXBean.class.getName(), mbeanInfo);
    }

    /**
     * Creates the metadata for the
     * {@link java.lang.management.CompilationMXBean}. For this type of
     * platform bean the metadata covers :
     * <ul>
     * <li>3 attributes
     * <li>0 constructors
     * <li>0 operations
     * <li>0 notifications
     * </ul>
     * 
     * @param result
     */
    private static void addCompilationBeanInfo(HashMap<String, MBeanInfo> result) {
        // Attributes
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[3];
        attributes[0] = new MBeanAttributeInfo("Name", String.class.getName(),
                "Name", true, false, false);
        attributes[1] = new MBeanAttributeInfo("TotalCompilationTime",
                Long.TYPE.getName(), "TotalCompilationTime", true, false, false);
        attributes[2] = new MBeanAttributeInfo(
                "CompilationTimeMonitoringSupported", Boolean.TYPE.getName(),
                "CompilationTimeMonitoringSupported", true, false, true);
        result.put(CompilationMXBean.class.getName(), new MBeanInfo(
                CompilationMXBeanImpl.class.getName(),
                CompilationMXBeanImpl.class.getName(), attributes, null, null,
                null));
    }

    /**
     * Creates the metadata for the {@link ClassLoadingMXBean}bean. For this
     * type of platform bean the metadata covers :
     * <ul>
     * <li>4 attributes
     * <li>0 constructors
     * <li>0 operations
     * <li>0 notifications
     * </ul>
     * 
     * @param result
     */
    private static void addClassLoadingInfo(HashMap<String, MBeanInfo> result) {
        // Attributes
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[4];
        attributes[0] = new MBeanAttributeInfo("LoadedClassCount", Integer.TYPE
                .getName(), "LoadedClassCount", true, false, false);
        attributes[1] = new MBeanAttributeInfo("TotalLoadedClassCount",
                Long.TYPE.getName(), "TotalLoadedClassCount", true, false,
                false);
        attributes[2] = new MBeanAttributeInfo("UnloadedClassCount", Long.TYPE
                .getName(), "UnloadedClassCount", true, false, false);
        attributes[3] = new MBeanAttributeInfo("Verbose", Boolean.TYPE
                .getName(), "Verbose", true, true, true);
        result.put(ClassLoadingMXBean.class.getName(), new MBeanInfo(
                ClassLoadingMXBeanImpl.class.getName(),
                ClassLoadingMXBeanImpl.class.getName(), attributes, null, null,
                null));
    }

    /**
     * @return the singleton <code>MemoryMXBean</code> instance.
     */
    public static MemoryMXBeanImpl getMemoryBean() {
        return MemoryMXBeanImpl.getInstance();
    }

    /**
     * @return the singleton <code>ThreadMXBean</code> instance.
     */
    public static ThreadMXBeanImpl getThreadBean() {
        return ThreadMXBeanImpl.getInstance();
    }

    /**
     * @return the singleton <code>RuntimeMXBean</code> instance.
     */
    public static RuntimeMXBeanImpl getRuntimeBean() {
        return RuntimeMXBeanImpl.getInstance();
    }

    /**
     * @return the singleton <code>RuntimeMXBean</code> instance.
     */
    public static OperatingSystemMXBeanImpl getOperatingSystemBean() {
        return OperatingSystemMXBeanImpl.getInstance();
    }

    /**
     * @return the singleton <code>CompilationMXBean</code> if available.
     */
    public static CompilationMXBeanImpl getCompliationBean() {
        return CompilationMXBeanImpl.getInstance();
    }

    /**
     * @return the singleton <code>LoggingMXBean</code> instance.
     */
    public static LoggingMXBeanImpl getLoggingBean() {
        return LoggingMXBeanImpl.getInstance();
    }

    /**
     * Returns a list of all of the instances of {@link MemoryManagerMXBean}in
     * this virtual machine. Owing to the dynamic nature of this kind of
     * <code>MXBean</code>, it is possible that instances may be created or
     * destroyed between the invocation and return of this method.
     * 
     * @return a list of all known <code>MemoryManagerMXBean</code> s in this
     *         virtual machine.
     */
    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return new LinkedList<MemoryManagerMXBean>(getMemoryBean()
                .getMemoryManagerMXBeans());
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
        List<MemoryPoolMXBean> result = new LinkedList<MemoryPoolMXBean>();
        Iterator<MemoryManagerMXBean> iter = getMemoryManagerMXBeans()
                .iterator();
        while (iter.hasNext()) {
            MemoryManagerMXBeanImpl b = (MemoryManagerMXBeanImpl) iter.next();
            result.addAll(b.getMemoryPoolMXBeans());
        }
        return result;
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
        List<GarbageCollectorMXBean> result = new LinkedList<GarbageCollectorMXBean>();
        Iterator<MemoryManagerMXBean> iter = getMemoryBean()
                .getMemoryManagerMXBeans().iterator();
        while (iter.hasNext()) {
            MemoryManagerMXBean b = iter.next();
            if (b instanceof GarbageCollectorMXBean) {
                result.add((GarbageCollectorMXBean) b);
            }
        }
        return result;
    }

    /**
     * Throws an {@link IllegalArgumentException}if the {@link CompositeData}
     * argument <code>cd</code> contains attributes that are not of the exact
     * types specified in the <code>expectedTypes</code> argument. The
     * attribute types of <code>cd</code> must also match the order of types
     * in <code>expectedTypes</code>.
     * 
     * @param cd
     *            a <code>CompositeData</code> object
     * @param expectedNames
     *            an array of expected attribute names
     * @param expectedTypes
     *            an array of type names
     */
    public static void verifyFieldTypes(CompositeData cd,
            String[] expectedNames, String[] expectedTypes) {
        Object[] allVals = cd.getAll(expectedNames);
        // Check that the number of elements match
        if (allVals.length != expectedTypes.length) {
            throw new IllegalArgumentException(
                    "CompositeData does not contain the expected number of attributes.");
        }

        // Type of corresponding elements must be the same
        for (int i = 0; i < allVals.length; i++) {
            String expectedType = expectedTypes[i];
            Object actualVal = allVals[i];
            // It is permissible that a value in the CompositeData object is
            // null in which case we cannot test its type. Move on.
            if (actualVal == null) {
                continue;
            }
            String actualType = actualVal.getClass().getName();
            if (!actualType.equals(expectedType)) {
                // Handle CompositeData and CompositeDataSupport
                if (expectedType.equals(CompositeData.class.getName())) {
                    if (allVals[i] instanceof CompositeData) {
                        continue;
                    }
                }
                throw new IllegalArgumentException(
                        "CompositeData contains an attribute not of expected type. "
                                + "Expected " + expectedType + ", found "
                                + actualType);
            }
        }// end for
    }

    /**
     * Throws an {@link IllegalArgumentException}if the {@link CompositeData}
     * argument <code>cd</code> does not have any of the attributes named in
     * the <code>expected</code> array of strings.
     * 
     * @param cd
     *            a <code>CompositeData</code> object
     * @param expected
     *            an array of attribute names expected in <code>cd</code>.
     */
    public static void verifyFieldNames(CompositeData cd, String[] expected) {
        for (int i = 0; i < expected.length; i++) {
            if (!cd.containsKey(expected[i])) {
                throw new IllegalArgumentException(
                        "CompositeData object does not contain expected key : " //$NON-NLS-1$
                                + expected[i]);
            }
        }// end for all elements in expected
    }

    /**
     * Throws an {@link IllegalArgumentException}if the {@link CompositeData}
     * argument <code>cd</code> has less number of attributes specified in
     * <code>minSize</code>.
     * 
     * @param cd
     *            a <code>CompositeData</code> object
     * @param i
     *            the number of expected attributes in <code>cd</code>
     */
    public static void verifyFieldNumber(CompositeData cd, int minSize) {
        if (cd.values().size() < minSize) {
            throw new IllegalArgumentException(
                    "CompositeData object does not have the expected number of attributes"); //$NON-NLS-1$
        }
    }

    /**
     * @param usage
     *            a {@link MemoryUsage}object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>usage</code> object.
     */
    public static CompositeData toMemoryUsageCompositeData(MemoryUsage usage) {
        // Bail out early on null input.
        if (usage == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "init", "used", "committed", "max" };
        Object[] values = { Long.valueOf(usage.getInit()),
                Long.valueOf(usage.getUsed()),
                Long.valueOf(usage.getCommitted()),
                Long.valueOf(usage.getMax()) };
        CompositeType cType = getMemoryUsageCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @return an instance of {@link CompositeType}for the {@link MemoryUsage}
     *         class.
     */
    public static CompositeType getMemoryUsageCompositeType() {
        if (MEMORYUSAGE_COMPOSITETYPE == null) {
            String[] typeNames = { "init", "used", "committed", "max" };
            String[] typeDescs = { "init", "used", "committed", "max" };
            OpenType[] typeTypes = { SimpleType.LONG, SimpleType.LONG,
                    SimpleType.LONG, SimpleType.LONG };
            try {
                MEMORYUSAGE_COMPOSITETYPE = new CompositeType(MemoryUsage.class
                        .getName(), MemoryUsage.class.getName(), typeNames,
                        typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return MEMORYUSAGE_COMPOSITETYPE;
    }

    /**
     * @param info
     *            a {@link java.lang.management.MemoryNotificationInfo}object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>info</code> object.
     */
    public static CompositeData toMemoryNotificationInfoCompositeData(
            MemoryNotificationInfo info) {
        // Bail out early on null input.
        if (info == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "poolName", "usage", "count" };
        Object[] values = { info.getPoolName(),
                toMemoryUsageCompositeData(info.getUsage()),
                Long.valueOf(info.getCount()) };
        CompositeType cType = getMemoryNotificationInfoCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @return an instance of {@link CompositeType}for the
     *         {@link MemoryNotificationInfo}class.
     */
    private static CompositeType getMemoryNotificationInfoCompositeType() {
        if (MEMORYNOTIFICATIONINFO_COMPOSITETYPE == null) {
            String[] typeNames = { "poolName", "usage", "count" };
            String[] typeDescs = { "poolName", "usage", "count" };
            OpenType[] typeTypes = { SimpleType.STRING,
                    getMemoryUsageCompositeType(), SimpleType.LONG };
            try {
                MEMORYNOTIFICATIONINFO_COMPOSITETYPE = new CompositeType(
                        MemoryNotificationInfo.class.getName(),
                        MemoryNotificationInfo.class.getName(), typeNames,
                        typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return MEMORYNOTIFICATIONINFO_COMPOSITETYPE;
    }

    /**
     * @param info
     *            a {@link ThreadInfo}object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>info</code> object.
     */
    public static CompositeData toThreadInfoCompositeData(ThreadInfo info) {
        // Bail out early on null input.
        if (info == null) {
            return null;
        }

        CompositeData result = null;
        StackTraceElement[] st = info.getStackTrace();
        CompositeData[] stArray = new CompositeData[st.length];
        for (int i = 0; i < st.length; i++) {
            stArray[i] = toStackTraceElementCompositeData(st[i]);
        }// end for

        String[] names = { "threadId", "threadName", "threadState",
                "suspended", "inNative", "blockedCount", "blockedTime",
                "waitedCount", "waitedTime", "lockName", "lockOwnerId",
                "lockOwnerName", "stackTrace" };
        Object[] values = {
                Long.valueOf(info.getThreadId()),
                info.getThreadName(),
                info.getThreadState().name(),
                Boolean.valueOf(info.isSuspended()),
                Boolean.valueOf(info.isInNative()),
                Long.valueOf(info.getBlockedCount()),
                Long.valueOf(info.getBlockedTime()),
                Long.valueOf(info.getWaitedCount()),
                Long.valueOf(info.getWaitedTime()),
                info.getLockName(),
                Long.valueOf(info.getLockOwnerId()),
                info.getLockOwnerName(), 
                stArray };
        CompositeType cType = getThreadInfoCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @param element
     *            a {@link StackTraceElement}object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>element</code> object.
     */
    public static CompositeData toStackTraceElementCompositeData(
            StackTraceElement element) {
        // Bail out early on null input.
        if (element == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "className", "methodName", "fileName", "lineNumber",
                "nativeMethod" };

        // A file name of null is permissable
        String fileName = element.getFileName();

        Object[] values = { element.getClassName(),
                element.getMethodName(), 
                fileName,
                Integer.valueOf(element.getLineNumber()),
                Boolean.valueOf(element.isNativeMethod()) };
        CompositeType cType = getStackTraceElementCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @return an instance of {@link CompositeType}for the {@link ThreadInfo}
     *         class.
     */
    private static CompositeType getThreadInfoCompositeType() {
        if (THREADINFO_COMPOSITETYPE == null) {
            try {
                String[] typeNames = { "threadId", "threadName", "threadState",
                        "suspended", "inNative", "blockedCount", "blockedTime",
                        "waitedCount", "waitedTime", "lockName", "lockOwnerId",
                        "lockOwnerName", "stackTrace" };
                String[] typeDescs = { "threadId", "threadName", "threadState",
                        "suspended", "inNative", "blockedCount", "blockedTime",
                        "waitedCount", "waitedTime", "lockName", "lockOwnerId",
                        "lockOwnerName", "stackTrace" };
                OpenType[] typeTypes = { SimpleType.LONG, SimpleType.STRING,
                        SimpleType.STRING, SimpleType.BOOLEAN,
                        SimpleType.BOOLEAN, SimpleType.LONG, SimpleType.LONG,
                        SimpleType.LONG, SimpleType.LONG, SimpleType.STRING,
                        SimpleType.LONG, SimpleType.STRING,
                        new ArrayType(1, getStackTraceElementCompositeType()) };
                THREADINFO_COMPOSITETYPE = new CompositeType(ThreadInfo.class
                        .getName(), ThreadInfo.class.getName(), typeNames,
                        typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return THREADINFO_COMPOSITETYPE;
    }

    /**
     * @return an instance of {@link CompositeType}for the
     *         {@link StackTraceElement}class.
     */
    private static CompositeType getStackTraceElementCompositeType() {
        if (STACKTRACEELEMENT_COMPOSITETYPE == null) {
            String[] typeNames = { "className", "methodName", "fileName",
                    "lineNumber", "nativeMethod" };
            String[] typeDescs = { "className", "methodName", "fileName",
                    "lineNumber", "nativeMethod" };
            OpenType[] typeTypes = { SimpleType.STRING, SimpleType.STRING,
                    SimpleType.STRING, SimpleType.INTEGER, SimpleType.BOOLEAN };
            try {
                STACKTRACEELEMENT_COMPOSITETYPE = new CompositeType(
                        StackTraceElement.class.getName(),
                        StackTraceElement.class.getName(), typeNames,
                        typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return STACKTRACEELEMENT_COMPOSITETYPE;
    }

    /**
     * Convenience method to converts an array of <code>String</code> to a
     * <code>List&lt;String&gt;</code>.
     * 
     * @param data
     *            an array of <code>String</code>
     * @return a new <code>List&lt;String&gt;</code>
     */
    public static List<String> convertStringArrayToList(String[] data) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < data.length; i++) {
            result.add(data[i]);
        }// end for
        return result;
    }

    /**
     * Receives an instance of a {@link TabularData}whose data is wrapping a
     * <code>Map</code> and returns a new instance of <code>Map</code>
     * containing the input information.
     * 
     * @param data
     *            an instance of <code>TabularData</code> that may be mapped
     *            to a <code>Map</code>.
     * @return a new {@link Map}containing the information originally wrapped
     *         in the <code>data</code> input.
     * @throws IllegalArgumentException
     *             if <code>data</code> has a <code>CompositeType</code>
     *             that does not contain exactly two items (i.e. a key and a
     *             value).
     */
    @SuppressWarnings("unchecked")
    public static Object convertTabularDataToMap(TabularData data) {
        // Bail out early on null input.
        if (data == null) {
            return null;
        }

        Map<Object, Object> result = new HashMap<Object, Object>();
        Set<String> cdKeySet = data.getTabularType().getRowType().keySet();
        // The key set for the CompositeData instances comprising each row
        // must contain only two elements.
        if (cdKeySet.size() != 2) {
            throw new IllegalArgumentException(
                    "TabularData's row type is not a CompositeType with two items.");
        }
        String[] keysArray = new String[2];
        int count = 0;
        Iterator<String> keysIt = cdKeySet.iterator();
        while (keysIt.hasNext()) {
            keysArray[count++] = keysIt.next();
        }// end while

        Collection<CompositeData> rows = data.values();
        Iterator<CompositeData> rowIterator = rows.iterator();
        while (rowIterator.hasNext()) {
            CompositeData rowCD = rowIterator.next();
            result.put(rowCD.get(keysArray[0]), rowCD.get(keysArray[1]));
        }// end while a row to process
        return result;
    }

    /**
     * Return a new instance of type <code>T</code> from the supplied
     * {@link CompositeData} object whose type maps to <code>T</code>.
     * 
     * @param <T>
     *            the type of object wrapped by the <code>CompositeData</code>.
     * @param data
     *            an instance of <code>CompositeData</code> that maps to an
     *            instance of <code>T</code>
     * @param realClass
     *            the {@link Class} object for type <code>T</code>
     * @return a new instance of <code>T</code>
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertFromCompositeData(CompositeData data,
            Class<T> realClass) throws SecurityException,
            NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        // Bail out early on null input.
        if (data == null) {
            return null;
        }

        // See if the realClass has a static for method that takes a
        // CompositeData and returns a new instance of T.
        Method forMethod = realClass.getMethod("from",
                new Class[] { CompositeData.class });
        return (T) forMethod.invoke(null, data);
    }

    /**
     * Receive data of the type specified in <code>openClass</code> and return
     * it in an instance of the type specified in <code>realClass</code>.
     * 
     * @param <T>
     * 
     * @param data
     *            an instance of the type named <code>openTypeName</code>
     * @param openClass
     * @param realClass
     * @return a new instance of the type <code>realTypeName</code> containing
     *         all the state in the input <code>data</code> object.
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws SecurityException
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertFromOpenType(Object data, Class<?> openClass,
            Class<T> realClass) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, SecurityException,
            IllegalArgumentException, NoSuchMethodException,
            InvocationTargetException {
        // Bail out early on null input.
        if (data == null) {
            return null;
        }

        T result = null;

        if (openClass.isArray() && realClass.isArray()) {
            Class openElementClass = openClass.getComponentType();
            Class<?> realElementClass = realClass.getComponentType();

            Object[] dataArray = (Object[]) data;
            result = (T) Array.newInstance(realElementClass, dataArray.length);
            for (int i = 0; i < Array.getLength(result); i++) {
                Array.set(result, i, convertFromOpenType(dataArray[i],
                        openElementClass, realElementClass));
            }// end for
        } else if (openClass.equals(CompositeData.class)) {
            result = ManagementUtils.convertFromCompositeData(
                    (CompositeData) data, realClass);
        } else if (openClass.equals(TabularData.class)) {
            if (realClass.equals(Map.class)) {
                result = (T) ManagementUtils
                        .convertTabularDataToMap((TabularData) data);
            }
        } else if (openClass.equals(String[].class)) {
            if (realClass.equals(List.class)) {
                result = (T) ManagementUtils
                        .convertStringArrayToList((String[]) data);
            }
        } else if (openClass.equals(String.class)) {
            if (realClass.equals(MemoryType.class)) {
                result = (T) ManagementUtils
                        .convertStringToMemoryType((String) data);
            }
        }
        return result;
    }

    /**
     * Convenience method that receives a string representation of a
     * <code>MemoryType</code> instance and returns the actual
     * <code>MemoryType</code> that corresponds to that value.
     * 
     * @param data
     *            a string
     * @return if <code>data</code> can be used to obtain an instance of
     *         <code>MemoryType</code> then a <code>MemoryType</code>,
     *         otherwise <code>null</code>.
     */
    private static MemoryType convertStringToMemoryType(String data) {
        MemoryType result = null;
        try {
            result = MemoryType.valueOf(data);
        } catch (IllegalArgumentException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * Convenience method to convert an object, <code>data</code> from its
     * Java type <code>realClass</code> to the specified open MBean type
     * <code>openClass</code>.
     * 
     * @param <T>
     *            the open MBean class
     * @param data
     *            the object to be converted
     * @param openClass
     *            the open MBean class
     * @param realClass
     *            the real Java type of <code>data</code>
     * @return a new instance of type <code>openClass</code>
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertToOpenType(Object data, Class<T> openClass,
            Class<?> realClass) {
        // Bail out early on null input.
        if (data == null) {
            return null;
        }

        T result = null;

        if (openClass.isArray() && realClass.isArray()) {
            Class<?> openElementClass = openClass.getComponentType();
            Class<?> realElementClass = realClass.getComponentType();

            Object[] dataArray = (Object[]) data;
            result = (T) Array.newInstance(openElementClass, dataArray.length);
            for (int i = 0; i < Array.getLength(result); i++) {
                Array.set(result, i, convertToOpenType(dataArray[i],
                        openElementClass, realElementClass));
            }// end for
        } else if (openClass.equals(CompositeData.class)) {
            if (realClass.equals(ThreadInfo.class)) {
                result = (T) ManagementUtils
                        .toThreadInfoCompositeData((ThreadInfo) data);
            } else if (realClass.equals(MemoryUsage.class)) {
                result = (T) ManagementUtils
                        .toMemoryUsageCompositeData((MemoryUsage) data);
            }
        } else if (openClass.equals(TabularData.class)) {
            if (realClass.equals(Map.class)) {
                result = (T) ManagementUtils
                        .toSystemPropertiesTabularData((Map) data);
            }
        } else if (openClass.equals(String[].class)) {
            if (realClass.equals(List.class)) {
                result = (T) ManagementUtils.convertListToArray((List) data,
                        openClass, openClass.getComponentType());
            }
        } else if (openClass.equals(String.class)) {
            if (realClass.isEnum()) {
                result = (T) ((Enum) data).name();
            }
        }
        return result;
    }

    /**
     * Convenience method to convert a {@link List} instance to an instance of
     * an array. The element type of the returned array will be of the same type
     * as the <code>List</code> component values.
     * 
     * @param <T>
     *            the array type named <code>arrayType</code>
     * @param <E>
     *            the type of the elements in the array,
     *            <code>elementType</code>
     * @param list
     *            the <code>List</code> to be converted
     * @param arrayType
     *            the array type
     * @param elementType
     *            the type of the array's elements
     * @return a new instance of <code>arrayType</code> initialised with the
     *         data stored in <code>list</code>
     */
    @SuppressWarnings("unchecked")
    private static <T, E> T convertListToArray(List<E> list,
            Class<T> arrayType, Class<E> elementType) {
        T result = (T) Array.newInstance(elementType, list.size());
        Iterator<E> it = list.iterator();
        int count = 0;
        while (it.hasNext()) {
            E element = it.next();
            Array.set(result, count++, element);
        }
        return result;
    }

    /**
     * @param propsMap
     *            a <code>Map&lt;String, String%gt;</code> of the system
     *            properties.
     * @return the system properties (e.g. as obtained from
     *         {@link RuntimeMXBean#getSystemProperties()}) wrapped in a
     *         {@link TabularData}.
     */
    public static TabularData toSystemPropertiesTabularData(
            Map<String, String> propsMap) {
        // Bail out early on null input.
        if (propsMap == null) {
            return null;
        }

        TabularData result = null;
        try {
            // Obtain the row type for the TabularType
            String[] rtItemNames = { "key", "value" };
            String[] rtItemDescs = { "key", "value" };
            OpenType[] rtItemTypes = { SimpleType.STRING, SimpleType.STRING };

            CompositeType rowType = new CompositeType(propsMap.getClass()
                    .getName(), propsMap.getClass().getName(), rtItemNames,
                    rtItemDescs, rtItemTypes);

            // Obtain the required TabularType
            TabularType sysPropsType = new TabularType(propsMap.getClass()
                    .getName(), propsMap.getClass().getName(), rowType,
                    new String[] { "key" });

            // Create an empty TabularData
            result = new TabularDataSupport(sysPropsType);

            // Take each entry out of the input propsMap, put it into a new
            // instance of CompositeData and put into the TabularType
            Set<Map.Entry<String,String>> entrySet = propsMap.entrySet();
            for (Iterator<Map.Entry<String, String>> iter = entrySet.iterator(); iter.hasNext();) {
                Map.Entry<String,String> entry = iter.next();
                result.put(new CompositeDataSupport(rowType, rtItemNames,
                        new String[] { entry.getKey(), entry.getValue() }));
            }// end for
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            result = null;
        }
        return result;
    }

    /**
     * Convenience method that sets out to return the {@link Class}object for
     * the specified type named <code>name</code>. Unlike the
     * {@link Class#forName(java.lang.String)}method, this will work even for
     * primitive types.
     * 
     * @param name
     *            the name of a Java type
     * @return the <code>Class</code> object for the type <code>name</code>
     * @throws ClassNotFoundException
     *             if <code>name</code> does not correspond to any known type
     *             (including primitive types).
     */
    public static Class<?> getClassMaybePrimitive(String name)
            throws ClassNotFoundException {
        Class<?> result = null;

        try {
            result = Class.forName(name);
        } catch (ClassNotFoundException e) {
            if (name.equals(boolean.class.getName())) {
                result = boolean.class;
            } else if (name.equals(char.class.getName())) {
                result = char.class;
            } else if (name.equals(byte.class.getName())) {
                result = byte.class;
            } else if (name.equals(short.class.getName())) {
                result = short.class;
            } else if (name.equals(int.class.getName())) {
                result = int.class;
            } else if (name.equals(long.class.getName())) {
                result = long.class;
            } else if (name.equals(float.class.getName())) {
                result = float.class;
            } else if (name.equals(double.class.getName())) {
                result = double.class;
            } else if (name.equals(void.class.getName())) {
                result = void.class;
            } else {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
                // Rethrow the original ClassNotFoundException
                throw e;
            }// end else
        }// end catch
        return result;
    }

    /**
     * Convenience method to determine if the <code>wrapper</code>
     * <code>Class</code>
     * object is really the wrapper class for the
     * <code>primitive</code> <code>Class</code> object.
     * 
     * @param wrapper
     * @param primitive
     * @return <code>true</code> if the <code>wrapper</code> class is the
     *         wrapper class for <code>primitive</code>. Otherwise
     *         <code>false</code>.
     */
    public static boolean isWrapperClass(Class<? extends Object> wrapper,
            Class<?> primitive) {
        boolean result = true;
        if (primitive.equals(boolean.class) && !wrapper.equals(Boolean.class)) {
            result = false;
        } else if (primitive.equals(char.class)
                && !wrapper.equals(Character.class)) {
            result = false;
        } else if (primitive.equals(byte.class) && !wrapper.equals(Byte.class)) {
            result = false;
        } else if (primitive.equals(short.class)
                && !wrapper.equals(Short.class)) {
            result = false;
        } else if (primitive.equals(int.class)
                && !wrapper.equals(Integer.class)) {
            result = false;
        } else if (primitive.equals(long.class) && !wrapper.equals(Long.class)) {
            result = false;
        } else if (primitive.equals(float.class)
                && !wrapper.equals(Float.class)) {
            result = false;
        } else if (primitive.equals(double.class)
                && !wrapper.equals(Double.class)) {
            result = false;
        }

        return result;
    }

    /**
     * Convenience method that returns a boolean indication of whether or not
     * concrete instances of the the supplied interface type
     * <code>mxbeanInterface</code> should also be implementors of the
     * interface <code>javax.management.NotificationEmitter</code>.
     * 
     * @param <T>
     * @param mxbeanInterface
     * @return <code>true</code> if instances of type
     *         <code>mxbeanInterface</code> should also implement
     *         <code>javax.management.NotificationEmitter</code>. Otherwise,
     *         <code>false</code>.
     */
    public static <T> boolean isANotificationEmitter(Class<T> mxbeanInterface) {
        boolean result = false;
        MBeanInfo info = getMBeanInfo(mxbeanInterface.getName());
        if (info != null) {
            MBeanNotificationInfo[] notifications = info.getNotifications();
            if ((notifications != null) && (notifications.length > 0)) {
                result = true;
            }
        }
        return result;
    }
}
