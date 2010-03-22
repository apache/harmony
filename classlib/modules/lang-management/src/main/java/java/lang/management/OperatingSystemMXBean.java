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
 * <code>OperatingSystemMXBean</code> is an interface used by the management
 * system to access operating system properties.
 * </p>
 * <p>
 * <code>ObjectName</code>: java.lang:type=OperatingSystem
 * </p>
 * 
 * @since 1.5
 */
public interface OperatingSystemMXBean {
    /**
     * <p>
     * The operating system's architecture; equivalent to
     * <code>System.getProperty("os.arch")</code>.
     * </p>
     * 
     * @return The underlying architecture of the operating system.
     * @throws SecurityException if the
     *         {@link SecurityManager#checkPropertyAccess(String)} doesn't
     *         allow access.
     */
    String getArch();

    /**
     * <p>
     * The number of processors available to the JVM; equivalent to
     * {@link Runtime#availableProcessors()}.
     * </p>
     * 
     * @return The number of available processors; guaranteed to be at least one.
     */
    int getAvailableProcessors();

    /**
     * <p>
     * The name of the operating system; equivalent to
     * <code>System.getProperty("os.name")</code>.
     * </p>
     * 
     * @return The name of the operating system.
     * @throws SecurityException if the
     *         {@link SecurityManager#checkPropertyAccess(String)} doesn't
     *         allow access.
     */
    String getName();

    /**
     * <p>
     * The operating system's version; equivalent to
     * <code>System.getProperty("os.version")</code>.
     * </p>
     * 
     * @return The version of the operating system.
     * @throws SecurityException if the
     *         {@link SecurityManager#checkPropertyAccess(String)} doesn't
     *         allow access.
     */
    String getVersion();
}
