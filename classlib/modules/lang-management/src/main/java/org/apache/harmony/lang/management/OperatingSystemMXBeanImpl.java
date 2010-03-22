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

import java.lang.management.OperatingSystemMXBean;

/**
 * Runtime type for {@link java.lang.management.OperatingSystemMXBean}.
 * 
 * @since 1.5
 */
public final class OperatingSystemMXBeanImpl extends DynamicMXBeanImpl
        implements OperatingSystemMXBean {

    private static OperatingSystemMXBeanImpl instance = new OperatingSystemMXBeanImpl();

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean.
     */
    private OperatingSystemMXBeanImpl() {
        setMBeanInfo(ManagementUtils
                .getMBeanInfo(java.lang.management.OperatingSystemMXBean.class
                        .getName()));
    }

    /**
     * Singleton accessor method.
     * 
     * @return the <code>OperatingSystemMXBeanImpl</code> singleton.
     */
    static OperatingSystemMXBeanImpl getInstance() {
        return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getArch()
     */
    public String getArch() {
        return System.getProperty("os.arch");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getAvailableProcessors()
     */
    public int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getName()
     */
    public String getName() {
        return System.getProperty("os.name");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.OperatingSystemMXBean#getVersion()
     */
    public String getVersion() {
        return System.getProperty("os.version");
    }
}
