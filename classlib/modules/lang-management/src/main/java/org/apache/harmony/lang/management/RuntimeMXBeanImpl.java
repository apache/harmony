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
import java.lang.management.RuntimeMXBean;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Runtime type for {@link java.lang.management.RuntimeMXBean}
 * 
 * @since 1.5
 */
public final class RuntimeMXBeanImpl extends DynamicMXBeanImpl implements
		RuntimeMXBean {

	private static RuntimeMXBeanImpl instance = new RuntimeMXBeanImpl();

	/**
	 * Constructor intentionally private to prevent instantiation by others.
	 * Sets the metadata for this bean.
	 */
	private RuntimeMXBeanImpl() {
		setMBeanInfo(ManagementUtils
				.getMBeanInfo(RuntimeMXBean.class.getName()));
	}

	/**
	 * Singleton accessor method.
	 * 
	 * @return the <code>RuntimeMXBeanImpl</code> singleton.
	 */
	static RuntimeMXBeanImpl getInstance() {
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getBootClassPath()
	 */
	public String getBootClassPath() {
		if (!isBootClassPathSupported()) {
			throw new UnsupportedOperationException(
					"VM does not support boot classpath.");
		}

		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkPermission(new ManagementPermission("monitor"));
		}

		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return System.getProperty("sun.boot.class.path");
			}// end method run
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getClassPath()
	 */
	public String getClassPath() {
        return System.getProperty("java.class.path");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getLibraryPath()
	 */
	public String getLibraryPath() {
        return System.getProperty("java.library.path");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getManagementSpecVersion()
	 */
	public String getManagementSpecVersion() {
		return "1.0";
	}

	/**
	 * @return the name of this running virtual machine.
	 * @see #getName()
	 */
	private native String getNameImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getName()
	 */
	public String getName() {
		return this.getNameImpl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getSpecName()
	 */
	public String getSpecName() {
        return System.getProperty("java.vm.specification.name");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getSpecVendor()
	 */
	public String getSpecVendor() {
        return System.getProperty("java.vm.specification.vendor");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getSpecVersion()
	 */
	public String getSpecVersion() {
        return System.getProperty("java.vm.specification.version");
	}

	/**
	 * @return the virtual machine start time in milliseconds.
	 * @see #getStartTime()
	 */
	private native long getStartTimeImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getStartTime()
	 */
	public long getStartTime() {
		return this.getStartTimeImpl();
	}

	/**
	 * @return the number of milliseconds the virtual machine has been running.
	 * @see #getUptime()
	 */
	private native long getUptimeImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getUptime()
	 */
	public long getUptime() {
		return this.getUptimeImpl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getVmName()
	 */
	public String getVmName() {
        return System.getProperty("java.vm.name");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getVmVendor()
	 */
	public String getVmVendor() {
        return System.getProperty("java.vm.vendor");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getVmVersion()
	 */
	public String getVmVersion() {
        return System.getProperty("java.vm.version");
	}

	/**
	 * @return <code>true</code> if supported, <code>false</code> otherwise.
	 * @see #isBootClassPathSupported()
	 */
	private native boolean isBootClassPathSupportedImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#isBootClassPathSupported()
	 */
	public boolean isBootClassPathSupported() {
		return this.isBootClassPathSupportedImpl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getInputArguments()
	 */
	public List<String> getInputArguments() {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkPermission(new ManagementPermission("monitor"));
		}
        
        // TODO : Retrieve the input args from the VM
        return new ArrayList<String>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getSystemProperties()
	 */
	public Map<String, String> getSystemProperties() {
		Map<String, String> result = new HashMap<String, String>();
        Properties props = System.getProperties();
		Enumeration<?> propNames = props.propertyNames();
		while (propNames.hasMoreElements()) {
			String propName = (String) propNames.nextElement();
			String propValue = props.getProperty(propName);
			result.put(propName, propValue);
		}// end while
		return result;
	}
}

