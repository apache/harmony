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
import java.lang.management.ManagementPermission;

/**
 * Runtime type for {@link ClassLoadingMXBean}.
 * <p>
 * There is only ever one instance of this class in a virtual machine.
 * </p>
 * 
 * @since 1.5
 */
public final class ClassLoadingMXBeanImpl extends DynamicMXBeanImpl implements
        ClassLoadingMXBean {

    private static ClassLoadingMXBeanImpl instance = new ClassLoadingMXBeanImpl();

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean.
     */
    private ClassLoadingMXBeanImpl() {
        setMBeanInfo(ManagementUtils.getMBeanInfo(ClassLoadingMXBean.class
                .getName()));
    }

    /**
     * Singleton accessor method.
     * 
     * @return the <code>ClassLoadingMXBeanImpl</code> singleton.
     */
    static ClassLoadingMXBeanImpl getInstance() {
        return instance;
    }

    /**
     * @return the number of loaded classes
     * @see #getLoadedClassCount()
     */
    private native int getLoadedClassCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ClassLoadingMXBean#getLoadedClassCount()
     */
    public int getLoadedClassCount() {
        return this.getLoadedClassCountImpl();
    }

    /**
     * @return the total number of classes that have been loaded
     * @see #getTotalLoadedClassCount()
     */
    private native long getTotalLoadedClassCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ClassLoadingMXBean#getTotalLoadedClassCount()
     */
    public long getTotalLoadedClassCount() {
        return this.getTotalLoadedClassCountImpl();
    }

    /**
     * @return the total number of unloaded classes
     * @see #getUnloadedClassCount()
     */
    private native long getUnloadedClassCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ClassLoadingMXBean#getUnloadedClassCount()
     */
    public long getUnloadedClassCount() {
        return this.getUnloadedClassCountImpl();
    }

    /**
     * @return true if running in verbose mode
     * @see #isVerbose()
     */
    private native boolean isVerboseImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ClassLoadingMXBean#isVerbose()
     */
    public boolean isVerbose() {
        return this.isVerboseImpl();
    }

    /**
     * @param value
     *            true to put the class loading system into verbose mode, false
     *            to take the class loading system out of verbose mode.
     * @see #setVerbose(boolean)
     */
    private native void setVerboseImpl(boolean value);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ClassLoadingMXBean#setVerbose(boolean)
     */
    public void setVerbose(boolean value) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.setVerboseImpl(value);
    }
}
