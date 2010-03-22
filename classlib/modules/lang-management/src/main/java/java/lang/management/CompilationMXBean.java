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
 * <code>CompilationMXBean</code> is an interface used by the management
 * system to access compilation properties.
 * </p>
 * <p>
 * <code>ObjectName</code>: java.lang:type=Compilation
 * </p>
 * 
 * @since 1.5
 */
public interface CompilationMXBean {
    
    /**
     * <p>
     * The name of the compilation system.
     * </p>
     * 
     * @return The compiler's name.
     */
    String getName();

    /**
     * <p>
     * The approximate, cumulative time (in milliseconds) spent performing
     * compilation.
     * </p>
     * 
     * @return The total time spent compiling.
     * @throws UnsupportedOperationException if this is not supported.
     */
    long getTotalCompilationTime();

    /**
     * <p>
     * Indicates whether or not the JVM supports compilation time monitoring.
     * </p>
     * 
     * @return <code>true</code> if supported, otherwise <code>false</code>.
     */
    boolean isCompilationTimeMonitoringSupported();
}
