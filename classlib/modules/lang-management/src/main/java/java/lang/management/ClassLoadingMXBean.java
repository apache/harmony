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

/**
 * <p>
 * <code>ClassLoadingMXBean</code> is an interface used by the management
 * system to access class loader properties.
 * </p>
 * <p>
 * <code>ObjectName</code>: java.lang:type=ClassLoading
 * </p>
 * 
 * @since 1.5
 */
public interface ClassLoadingMXBean {
    /**
     * <p>
     * The number of classes currently loaded in the JVM.
     * </p>
     * 
     * @return The number of loaded classes.
     */
    int getLoadedClassCount();

    /**
     * <p>
     * The total number of classes the JVM has loaded since startup.
     * </p>
     * 
     * @return The total number of classes loaded.
     */
    long getTotalLoadedClassCount();

    /**
     * <p>
     * The number of classes that have been unloaded in the JVM since startup.
     * </p>
     * 
     * @return The number of classes unloaded.
     */
    long getUnloadedClassCount();

    /**
     * <p>
     * Indicates whether or not the verbose output is enabled for the class
     * loading system.
     * </p>
     * 
     * @return <code>true</code> is verbose output is enabled, otherwise <code>false</code>.
     */
    boolean isVerbose();

    /**
     * <p>
     * Enables or disables the verbose output of the class loading system.
     * </p>
     * 
     * @param value <code>true</code> to enable, <code>false</code> to
     *        disable.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("control")</code>.
     */
    void setVerbose(boolean value);
}
