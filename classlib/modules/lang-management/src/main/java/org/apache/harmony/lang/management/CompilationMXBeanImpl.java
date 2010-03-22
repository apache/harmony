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

import java.lang.management.CompilationMXBean;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Runtime type for {@link CompilationMXBean}
 * 
 * @since 1.5
 */
public final class CompilationMXBeanImpl extends DynamicMXBeanImpl implements
        CompilationMXBean {

    private static CompilationMXBeanImpl instance = createInstance();

    /**
     * Conditionally returns the singleton instance of this type of MXBean.
     * 
     * @return if the virtual machine has a compilation system, returns a new
     *         instance of <code>CompilationMXBean</code>, otherwise returns
     *         <code>null</code>.
     */
    private static CompilationMXBeanImpl createInstance() {
        CompilationMXBeanImpl result = null;

        if (isJITEnabled()) {
            result = new CompilationMXBeanImpl();
        }
        return result;
    }

	/**
	 * Query whether the VM is running with a JIT compiler enabled.
	 * 
	 * @return true if a JIT is enabled, false otherwise
	 */
	private static native boolean isJITEnabled();
	
    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean. 
     */
    private CompilationMXBeanImpl() {
        setMBeanInfo(ManagementUtils.getMBeanInfo(CompilationMXBean.class
                .getName()));
    }

    /**
     * Singleton accessor method.
     * 
     * @return the <code>ClassLoadingMXBeanImpl</code> singleton.
     */
    static CompilationMXBeanImpl getInstance() {
        return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.CompilationMXBean#getName()
     */
    public String getName() {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("java.compiler");
            }// end method run
        });
    }

    /**
     * @return the compilation time in milliseconds
     * @see #getTotalCompilationTime()
     */
    private native long getTotalCompilationTimeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.CompilationMXBean#getTotalCompilationTime()
     */
    public long getTotalCompilationTime() {
        if (!isCompilationTimeMonitoringSupported()) {
            throw new UnsupportedOperationException(
                    "VM does not support monitoring of compilation time.");
        }
        return this.getTotalCompilationTimeImpl();
    }

    /**
     * @return <code>true</code> if compilation timing is supported, otherwise
     *         <code>false</code>.
     * @see #isCompilationTimeMonitoringSupported()
     */
    private native boolean isCompilationTimeMonitoringSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.CompilationMXBean#isCompilationTimeMonitoringSupported()
     */
    public boolean isCompilationTimeMonitoringSupported() {
        return this.isCompilationTimeMonitoringSupportedImpl();
    }
}
