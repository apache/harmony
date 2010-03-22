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

import java.lang.management.ManagementPermission;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Runtime type for {@link java.lang.management.MemoryPoolMXBean}
 * 
 * @since 1.5
 */
public final class MemoryPoolMXBeanImpl extends DynamicMXBeanImpl implements
        MemoryPoolMXBean {
    private final String name;

    /**
     * IMPORTANT: bean identifier for use by VM
     */
    @SuppressWarnings("unused")
    private final int id;

    private final MemoryType type;

    private final MemoryMXBeanImpl memBean;

    /**
     * Sets the metadata for this bean.
     * 
     * @param name
     * @param type
     * @param id
     * @param memBean
     */
    MemoryPoolMXBeanImpl(String name, MemoryType type, int id,
            MemoryMXBeanImpl memBean) {
        this.name = name;
        this.type = type;
        this.id = id;
        this.memBean = memBean;
        setMBeanInfo(ManagementUtils
                .getMBeanInfo(java.lang.management.MemoryPoolMXBean.class
                        .getName()));
        if (isUsageThresholdSupported()
                || isCollectionUsageThresholdSupported()) {
            MemoryNotificationThread t = new MemoryNotificationThread(memBean,
                    this, id);
            t.setDaemon(true);
            t.setName("MemoryPoolMXBean notification dispatcher");
            t.setPriority(Thread.NORM_PRIORITY + 1);
            t.start();
        }
    }

    /**
     * @return a {@link MemoryUsage}object that may be interrogated by the
     *         caller to determine the details of the memory usage. Returns
     *         <code>null</code> if the virtual machine does not support this
     *         method.
     * @see #getCollectionUsage()
     */
    private native MemoryUsage getCollectionUsageImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#getCollectionUsage()
     */
    public MemoryUsage getCollectionUsage() {
        return this.getCollectionUsageImpl();
    }

    /**
     * @return the collection usage threshold in bytes. The default value as set
     *         by the virtual machine will be zero.
     * @see #getCollectionUsageThreshold()
     */
    private native long getCollectionUsageThresholdImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#getCollectionUsageThreshold()
     */
    public long getCollectionUsageThreshold() {
        if (!isCollectionUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                    "VM does not support collection usage threshold.");
        }
        return this.getCollectionUsageThresholdImpl();
    }

    /**
     * @return a count of the number of times that the collection usage
     *         threshold has been surpassed.
     * @see #getCollectionUsageThresholdCount()
     */
    private native long getCollectionUsageThresholdCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#getCollectionUsageThresholdCount()
     */
    public long getCollectionUsageThresholdCount() {
        if (!isCollectionUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                    "VM does not support collection usage threshold.");
        }
        return this.getCollectionUsageThresholdCountImpl();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#getMemoryManagerNames()
     */
    public String[] getMemoryManagerNames() {
        /* get the memory managers and check which of them manage this pool */
        Iterator<MemoryManagerMXBean> iter = memBean.getMemoryManagerMXBeans()
                .iterator();
        List<String> result = new LinkedList<String>();
        while (iter.hasNext()) {
            MemoryManagerMXBean bean = iter.next();
            String[] managedPools = bean.getMemoryPoolNames();
            for (int i = 0; i < managedPools.length; i++) {
                if (managedPools[i].equals(name)) {
                    result.add(bean.getName());
                    break;
                }
            }
        }
        return result.toArray(new String[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return a {@link MemoryUsage}which can be interrogated by the caller to
     *         determine details of the peak memory usage. A <code>null</code>
     *         value will be returned if the memory pool no longer exists (and
     *         the pool is therefore considered to be invalid).
     * @see #getPeakUsage()
     */
    private native MemoryUsage getPeakUsageImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#getPeakUsage()
     */
    public MemoryUsage getPeakUsage() {
        return this.getPeakUsageImpl();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#getType()
     */
    public MemoryType getType() {
        return this.type;
    }

    /**
     * @return an instance of {@link MemoryUsage}that can be interrogated by
     *         the caller to determine details on the pool's current memory
     *         usage. A <code>null</code> value will be returned if the memory
     *         pool no longer exists (in which case it is considered to be
     *         invalid).
     * @see #getUsage()
     */
    private native MemoryUsage getUsageImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#getUsage()
     */
    public MemoryUsage getUsage() {
        return this.getUsageImpl();
    }

    /**
     * @return the usage threshold in bytes. The default value as set by the
     *         virtual machine depends on the platform the virtual machine is
     *         running on. will be zero.
     * @see #getUsageThreshold()
     */
    private native long getUsageThresholdImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#getUsageThreshold()
     */
    public long getUsageThreshold() {
        if (!isUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                    "VM does not support usage threshold.");
        }
        return this.getUsageThresholdImpl();
    }

    /**
     * @return a count of the number of times that the usage threshold has been
     *         surpassed.
     * @see #getUsageThresholdCount()
     */
    private native long getUsageThresholdCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#getUsageThresholdCount()
     */
    public long getUsageThresholdCount() {
        if (!isUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                    "VM does not support usage threshold.");
        }
        return this.getUsageThresholdCountImpl();
    }

    /**
     * @return <code>true</code> if the collection usage threshold was
     *         surpassed after the latest garbage collection run, otherwise
     *         <code>false</code>.
     * @see #isCollectionUsageThresholdExceeded()
     */
    private native boolean isCollectionUsageThresholdExceededImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#isCollectionUsageThresholdExceeded()
     */
    public boolean isCollectionUsageThresholdExceeded() {
        if (!isCollectionUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                    "VM does not support collection usage threshold.");
        }
        return this.isCollectionUsageThresholdExceededImpl();
    }

    /**
     * @return <code>true</code> if supported, <code>false</code> otherwise.
     * @see #isCollectionUsageThresholdSupported()
     */
    private native boolean isCollectionUsageThresholdSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#isCollectionUsageThresholdSupported()
     */
    public boolean isCollectionUsageThresholdSupported() {
        return this.isCollectionUsageThresholdSupportedImpl();
    }

    /**
     * @return <code>true</code> if the usage threshold has been surpassed,
     *         otherwise <code>false</code>.
     * @see #isUsageThresholdExceeded()
     */
    private native boolean isUsageThresholdExceededImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#isUsageThresholdExceeded()
     */
    public boolean isUsageThresholdExceeded() {
        if (!isUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                    "VM does not support usage threshold.");
        }
        return this.isUsageThresholdExceededImpl();
    }

    /**
     * @return <code>true</code> if supported, <code>false</code> otherwise.
     * @see #isUsageThresholdSupported()
     */
    private native boolean isUsageThresholdSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#isUsageThresholdSupported()
     */
    public boolean isUsageThresholdSupported() {
        return this.isUsageThresholdSupportedImpl();
    }

    /**
     * @return <code>true</code> if the memory pool has not been removed by
     *         the virtual machine, <code>false</code> otherwise.
     * @see #isValid()
     */
    private native boolean isValidImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#isValid()
     */
    public boolean isValid() {
        return this.isValidImpl();
    }

    /**
     * @see #resetPeakUsage()
     */
    private native void resetPeakUsageImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#resetPeakUsage()
     */
    public void resetPeakUsage() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.resetPeakUsageImpl();
    }

    /**
     * @param threshold
     *            the size of the new collection usage threshold expressed in
     *            bytes.
     * @see #setCollectionUsageThreshold(long)
     */
    private native void setCollectionUsageThresholdImpl(long threshold);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#setCollectionUsageThreshold(long)
     */
    public void setCollectionUsageThreshold(long threshold) {
        if (!isCollectionUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                    "VM does not support collection usage threshold.");
        }

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }

        if (threshold < 0) {
            throw new IllegalArgumentException(
                    "Collection usage threshold cannot be negative.");
        }

        if (exceedsMaxPoolSize(threshold)) {
            throw new IllegalArgumentException(
                    "Collection usage threshold cannot exceed maximum amount of memory for pool.");
        }
        this.setCollectionUsageThresholdImpl(threshold);
    }

    /**
     * @param threshold
     *            the size of the new usage threshold expressed in bytes.
     * @see #setUsageThreshold(long)
     */
    private native void setUsageThresholdImpl(long threshold);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryPoolMXBean#setUsageThreshold(long)
     */
    public void setUsageThreshold(long threshold) {
        if (!isUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                    "VM does not support usage threshold.");
        }

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }

        if (threshold < 0) {
            throw new IllegalArgumentException(
                    "Usage threshold cannot be negative.");
        }

        if (exceedsMaxPoolSize(threshold)) {
            throw new IllegalArgumentException(
                    "Usage threshold cannot exceed maximum amount of memory for pool.");
        }
        this.setUsageThresholdImpl(threshold);
    }

    /**
     * @param value
     * @return <code>true</code> if <code>value</code> is greater than the
     *         maximum size of the corresponding memory pool. <code>false</code>
     *         if <code>value</code> does not exceed the maximum memory pool
     *         size or else no memory pool maximum size has been defined.
     */
    private boolean exceedsMaxPoolSize(long value) {
        MemoryUsage m = getUsage();
        return (m.getMax() != -1 && m.getMax() < value);
    }
}
