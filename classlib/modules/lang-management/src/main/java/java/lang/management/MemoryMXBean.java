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
 * <code>MemoryMXBean</code> is an interface used by the management
 * system to access memory-related properties.
 * </p>
 * <p>
 * <code>ObjectName</code>: java.lang:type=Memory
 * </p>
 * 
 * @since 1.5
 */
public interface MemoryMXBean {
    
    /**
     * <p>
     * Invokes the JVM's garbage collector; equivalent to {@link System#gc()}.
     * </p>
     */
    void gc();

    /**
     * <p>
     * The current memory usage for the heap; memory used for object allocation.
     * </p>
     * 
     * @return A {@link MemoryUsage} instance representing heap memory.
     */
    MemoryUsage getHeapMemoryUsage();

    /**
     * <p>
     * The current memory usage outside of the heap.
     * </p>
     * 
     * @return A {@link MemoryUsage} instance representing non-heap memory.
     */
    MemoryUsage getNonHeapMemoryUsage();

    /**
     * <p>
     * The approximate number of objects that are about to be finalized.
     * </p>
     * 
     * @return The number of objects about to be finalized.
     */
    int getObjectPendingFinalizationCount();

    /**
     * <p>
     * Indicates whether or not the verbose output is enabled for the memory
     * system.
     * </p>
     * 
     * @return <code>true</code> is verbose output is enabled, otherwise
     *         <code>false</code>.
     */
    boolean isVerbose();

    /**
     * <p>
     * Enables or disables the verbose output of the memory system.
     * </p>
     * 
     * @param value <code>true</code> to enable, <code>false</code> to
     *        disable.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("control")</code>.
     */
    void setVerbose(boolean value);
}
