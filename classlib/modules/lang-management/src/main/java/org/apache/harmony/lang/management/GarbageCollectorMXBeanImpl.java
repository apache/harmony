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

import java.lang.management.GarbageCollectorMXBean;


/**
 * Runtime type for {@link java.lang.management.GarbageCollectorMXBean}.
 * 
 * @since 1.5
 */
public final class GarbageCollectorMXBeanImpl extends MemoryManagerMXBeanImpl
        implements GarbageCollectorMXBean {

    /**
     * @param name The name of this collector
     * @param id An internal id number representing this collector
     * @param memBean The memory bean that receives notification events from pools managed by this collector
     */
    GarbageCollectorMXBeanImpl(String name, int id, MemoryMXBeanImpl memBean ) {
        super(name, id, memBean);
    }

    /**
     * Sets the metadata for this bean.
     */
    protected void initializeInfo() {
        setMBeanInfo(ManagementUtils
                .getMBeanInfo(java.lang.management.GarbageCollectorMXBean.class
                        .getName()));
    }

    /**
     * @return the total number of garbage collections that have been carried
     *         out by the associated garbage collector.
     * @see #getCollectionCount()
     */
    private native long getCollectionCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.GarbageCollectorMXBean#getCollectionCount()
     */
    public long getCollectionCount() {
        return this.getCollectionCountImpl();
    }

    /**
     * @return the number of milliseconds that have been spent in performing
     *         garbage collection. This is a cumulative figure.
     * @see #getCollectionTime()
     */
    private native long getCollectionTimeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.GarbageCollectorMXBean#getCollectionTime()
     */
    public long getCollectionTime() {
        return this.getCollectionTimeImpl();
    }
}
