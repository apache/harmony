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
 * <code>MemoryPoolMXBean</code> is an interface used by the management
 * system to access memory pool properties.
 * </p>
 * <p>
 * <code>ObjectName</code> pattern: java.lang:type=MemoryPool,name=<i>pool_name</i>
 * </p>
 * 
 * @since 1.5
 */
public interface MemoryPoolMXBean {
    /**
     * <p>
     * The memory usage of the JVM's most recent attempt to recycle unused
     * objects in this pool. If this feature is not supported, then
     * <code>null</code> will be returned.
     * </p>
     * 
     * @return A {@link MemoryUsage} instance representing the most recent
     *         attempt to recycle unused objects; <code>null</code> may be
     *         returned to indicated this method is not supported.
     */
    MemoryUsage getCollectionUsage();

    /**
     * <p>
     * The collection usage threshold (in bytes) of this pool; the default is
     * zero.
     * </p>
     * 
     * @return The collection usage threshold value.
     * @throws UnsupportedOperationException if this is not supported.
     * @see #isCollectionUsageThresholdSupported()
     */
    long getCollectionUsageThreshold();

    /**
     * <p>
     * The number of times the collection usage threshold has been reached or
     * exceeded.
     * </p>
     * 
     * @return The number of times the collection usage threshold has been
     *         reached or exceeded.
     * @throws UnsupportedOperationException if this is not supported.
     * @see #isCollectionUsageThresholdSupported()
     */
    long getCollectionUsageThresholdCount();

    /**
     * <p>
     * The names of the memory managers that manage this pool; there will be at
     * least one memory manager for each pool.
     * </p>
     * 
     * @return A <code>String[]</code> of memory manager names.
     */
    String[] getMemoryManagerNames();

    /**
     * <p>
     * The name of this memory pool.
     * </p>
     * 
     * @return The name of this memory pool.
     */
    String getName();

    /**
     * <p>
     * The peak memory usage of this pool since JVM startup or since the last
     * reset of the peak.
     * </p>
     * 
     * @return A {@link MemoryUsage} instance representing the peak usage.
     * @see #resetPeakUsage()
     */
    MemoryUsage getPeakUsage();

    /**
     * <p>
     * The memory type of this pool.
     * </p>
     * 
     * @return The pool's memory type.
     */
    MemoryType getType();

    /**
     * <p>
     * The approximate, current memory usage of this pool. This method will
     * return <code>null</code> if the pool is invalid.
     * </p>
     * 
     * @return A {@link MemoryUsage} instance representing the current usage or
     *         <code>null</code> if the pool is invalid.
     */
    MemoryUsage getUsage();

    /**
     * <p>
     * The current usage threshold (in bytes) of this pool.
     * </p>
     * 
     * @return The current usage threshold.
     * @throws UnsupportedOperationException if this is not supported.
     * @see #isUsageThresholdSupported()
     */
    long getUsageThreshold();

    /**
     * <p>
     * The number of times the usage threshold has been met or exceeded.
     * </p>
     * 
     * @return The number of times the usage threshold has been met or exceeded.
     * @throws UnsupportedOperationException if this is not supported.
     * @see #isUsageThresholdSupported()
     */
    long getUsageThresholdCount();

    /**
     * <p>
     * Indicates whether or not the collection usage threshold has been met or
     * exceeded after the most recent collection.
     * </p>
     * 
     * @return <code>true</code> if the collection usage threshold was met or
     *         exceeded, otherwise <code>false</code>.
     * @throws UnsupportedOperationException if this is not supported.
     * @see #isCollectionUsageThresholdSupported()
     */
    boolean isCollectionUsageThresholdExceeded();

    /**
     * <p>
     * Indicates whether or not this pool supports collection threshold
     * monitoring.
     * </p>
     * 
     * @return <code>true</code> if supported, <code>false</code> otherwise.
     */
    boolean isCollectionUsageThresholdSupported();

    /**
     * <p>
     * Indicates whether or not the usage threshold is currently met or
     * exceeded.
     * </p>
     * 
     * @return <code>true</code> if the usage threshold is met or exceeded,
     *         otherwise <code>false</code>.
     * @throws UnsupportedOperationException if this is not supported.
     * @see #isUsageThresholdSupported()
     */
    boolean isUsageThresholdExceeded();

    /**
     * <p>
     * Indicates whether or not usage threshold monitoring is supported.
     * </p>
     * 
     * @return <code>true</code> if supported, otherwise <code>false</code>.
     */
    boolean isUsageThresholdSupported();

    /**
     * <p>
     * Indicates whether or not the pool is currently valid. A memory pool may
     * be removed by a JVM and become invalid.
     * </p>
     * 
     * @return <code>true</code> if the pool is valid, <code>false</code>
     *         otherwise.
     */
    boolean isValid();

    /**
     * <p>
     * Resets the peak memory usage to the current value.
     * </p>
     * 
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("control")</code>.
     */
    void resetPeakUsage();

    /**
     * <p>
     * Sets the collection usage threshold (in bytes) for this memory pool.
     * </p>
     * 
     * @param threshold The new, non-negative threshold.
     * @throws IllegalArgumentException if the new <code>threshold</code> is either negative
     *         or greater than the maximum memory allowed.
     * @throws UnsupportedOperationException if this is not supported.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("control")</code>.
     */
    void setCollectionUsageThreshold(long threshold);

    /**
     * <p>
     * Sets the usage threshold (in bytes) for this memory pool.
     * </p>
     * 
     * @param threshold The new, non-negative threshold.
     * @throws IllegalArgumentException if the new <code>threshold</code> is either negative
     *         or greater than the maximum memory allowed.
     * @throws UnsupportedOperationException if this is not supported.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("control")</code>.
     */
    void setUsageThreshold(long threshold);
}
