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

import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Runtime type for {@link MemoryManagerMXBean}
 * 
 * @since 1.5
 */
public class MemoryManagerMXBeanImpl extends DynamicMXBeanImpl implements
        MemoryManagerMXBean {
    protected final String name;

    protected final int id;

    private List<MemoryPoolMXBean> managedPoolList;

    /**
     * Sets the metadata for this bean.
     * 
     * @param name
     * @param id
     * @param memBean
     */
    MemoryManagerMXBeanImpl(String name, int id, MemoryMXBeanImpl memBean) {
        this.name = name;
        this.id = id;
        initializeInfo();
        managedPoolList = new LinkedList<MemoryPoolMXBean>();
        createMemoryPools(id, memBean);
    }

    /**
     * 
     */
    protected void initializeInfo() {
        setMBeanInfo(ManagementUtils.getMBeanInfo(MemoryManagerMXBean.class
                .getName()));
    }

    /**
     * Instantiate the MemoryPoolMXBeans representing the memory managed by this
     * manager, and store them in the managedPoolList.
     * 
     * @param managerID
     * @param memBean
     */
    private native void createMemoryPools(int managerID,
            MemoryMXBeanImpl memBean);

    /**
     * A helper method called from within the native
     * {@link #createMemoryPools(int, MemoryMXBeanImpl)} method to construct new
     * MemoryPoolMXBeans representing memory managed by this manager and add
     * them to the {@link #managedPoolList}.
     * 
     * @param name
     *            the name of the corresponding memory pool
     * @param isHeap
     *            boolean indication of the memory pool type. <code>true</code>
     *            indicates that the memory is heap memory while
     *            <code>false</code> indicates non-heap memory
     * @param internalID
     *            numerical identifier associated with the memory pool for the
     *            benefit of the VM
     * @param memBean
     *            the {@link MemoryMXBeanImpl} that will send event
     *            notifications related to this memory pool
     */
    @SuppressWarnings("unused")
    // IMPORTANT: for use by VM
    private void createMemoryPoolHelper(String name, boolean isHeap,
            int internalID, MemoryMXBeanImpl memBean) {
        managedPoolList.add(new MemoryPoolMXBeanImpl(name,
                isHeap ? MemoryType.HEAP : MemoryType.NON_HEAP, internalID,
                memBean));
    }

    /**
     * Retrieves the list of memory pool beans managed by this manager.
     * 
     * @return the list of <code>MemoryPoolMXBean</code> instances
     */
    List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return managedPoolList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryManagerMXBean#getMemoryPoolNames()
     */
    public String[] getMemoryPoolNames() {
        String[] names = new String[managedPoolList.size()];
        int idx = 0;
        Iterator<MemoryPoolMXBean> iter = managedPoolList.iterator();
        while (iter.hasNext()) {
            names[idx++] = iter.next().getName();
        }
        return names;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryManagerMXBean#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return <code>true</code> if the memory manager is still valid in the
     *         virtual machine ; otherwise <code>false</code>.
     * @see #isValid()
     */
    private native boolean isValidImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryManagerMXBean#isValid()
     */
    public boolean isValid() {
        return this.isValidImpl();
    }
}
