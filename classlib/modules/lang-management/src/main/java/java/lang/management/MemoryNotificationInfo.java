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

import javax.management.openmbean.CompositeData;

import org.apache.harmony.lang.management.ManagementUtils;

/**
 * <p>
 * Memory notification information.
 * </p>
 * 
 * @since 1.5
 */
public class MemoryNotificationInfo {

    private final String poolName;

    private final MemoryUsage usage;

    private final long count;

    public static final String MEMORY_COLLECTION_THRESHOLD_EXCEEDED = "java.management.memory.collection.threshold.exceeded";

    public static final String MEMORY_THRESHOLD_EXCEEDED = "java.management.memory.threshold.exceeded";

    /**
     * <p>
     * Constructs a MemoryNotificationInfo object from the CompositeData passed.
     * </p>
     * 
     * @param cd The CompositeDate object to retrieve data from.
     * @return A MemoryNotificationInfo instance or {@code null} if {@code cd} is {@code null}
     * @throws IllegalArgumentException if <code>cd</code> does not contain
     *         MemoryUsage data.
     */
    public static MemoryNotificationInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        ManagementUtils.verifyFieldNumber(cd, 3);
        String[] attributeNames = { "poolName", "usage", "count" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        ManagementUtils.verifyFieldNames(cd, attributeNames);
        String[] attributeTypes = { "java.lang.String",
                "javax.management.openmbean.CompositeData", "java.lang.Long", }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        ManagementUtils.verifyFieldTypes(cd, attributeNames, attributeTypes);

        return new MemoryNotificationInfo(cd);
    }

    /**
     * <p>
     * Constructs a MemoryNotificationInfo instance.
     * </p>
     * 
     * @param poolName The memory pool name.
     * @param usage The memory usage snapshot.
     * @param count The threshold crossing count.
     * 
     * @throws NullPointerException if {@code poolName} or {@code usage} is {@code null}
     */
    public MemoryNotificationInfo(String poolName, MemoryUsage usage, long count) {
        super();
        if (poolName == null) {
            throw new NullPointerException("pooName is null"); //$NON-NLS-1$
        }
        if (usage == null) {
            throw new NullPointerException("usage is null"); //$NON-NLS-1$
        }
        
        this.poolName = poolName;
        this.usage = usage;
        this.count = count;
    }

    private MemoryNotificationInfo(CompositeData cd) {
        final Object poolName = cd.get("poolName"); //$NON-NLS-1$
        if (poolName == null) {
            throw new IllegalArgumentException(
                    "Attribute poolName has null value"); //$NON-NLS-1$
        }
        this.poolName = (String) poolName;
        this.usage = MemoryUsage.from((CompositeData) cd.get("usage")); //$NON-NLS-1$
        this.count = ((Long) cd.get("count")).longValue(); //$NON-NLS-1$
    }

    public String getPoolName() {
        return poolName;
    }

    public MemoryUsage getUsage() {
        return usage;
    }

    public long getCount() {
        return count;
    }
}
