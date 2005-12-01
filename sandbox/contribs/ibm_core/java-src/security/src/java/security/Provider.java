/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.security;


import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;


/**
 * Common superclass for providers of Java Security API implementations. Such
 * implementations may provide a number of services like Message digest, key
 * generation, certificate support, secure random number generation, etc.
 * 
 * @see MessageDigest
 * @see java.security.cert.CertificateFactory
 * @see SecureRandom
 */
public abstract class Provider extends java.util.Properties {

	static final long serialVersionUID = -4298000515446427739L; 

	private String name; // Name of the provider.

	private String info; // Generic description about what is being provided.

	private double version; // Version number for the services being provided.

	/**
	 * Constructs a new Provider with the given name, version and info.
	 * 
	 * @param name
	 *            name for this provider
	 * @param version
	 *            version number for the services being provided
	 * @param info
	 *            generic description of the services being provided
	 */
	protected Provider(String name, double version, String info) {
		super();
		this.name = name;
		this.version = version;
		this.info = info;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#clear()
	 */
	public synchronized void clear() {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkSecurityAccess("clearProviderProperties." + name);
		super.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet() {
		return Collections.unmodifiableSet(super.entrySet());
	}

	/**
	 * Returns the generic information about the services being provided.
	 * 
	 * 
	 * 
	 * @return String generic description of the services being provided
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * Returns the name of this provider.
	 * 
	 * 
	 * 
	 * @return String name of the provider
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the version number for teh services being provided
	 * 
	 * 
	 * 
	 * @return double version number for the services being provided
	 */
	public double getVersion() {
		return version;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#keySet()
	 */
	public Set keySet() {
		return Collections.unmodifiableSet(super.keySet());
	}

	/**
	 * Loads properties from the specified InputStream. The properties are of
	 * the form <code>key=value</code>, one property per line.
	 * 
	 * @param in
	 *            the input stream
	 * @throws IOException 
	 */
	public synchronized void load(InputStream in) throws IOException {
		super.load(in);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Dictionary#put(java.lang.Object, java.lang.Object)
	 */
	public synchronized Object put(Object key, Object value) {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkSecurityAccess("putProviderProperty." + name);
		return super.put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public synchronized void putAll(Map map) {
		super.putAll(map);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Dictionary#remove(java.lang.Object)
	 */
	public synchronized Object remove(Object key) {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkSecurityAccess("removeProviderProperty." + name);
		return super.remove(key);
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * 
	 * @return a printable representation for the receiver.
	 */
	public String toString() {
		return "Provider : " + name + " at version " + version; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		return Collections.unmodifiableCollection(super.values());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public String getProperty(String key, String defaultValue) {
		String result = getProperty(key);

		if (result == null) {
			return defaultValue;
		}

		return defaultValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Properties#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		int index = key.indexOf('.');
		if (index == -1) {
			return null;
		}

		String prefix = key.substring(0, index + 1);
		String algorithm = key.substring(index + 1, key.length());

		return lookupProperty(prefix, algorithm);
	}

	String lookupProperty(String property) {

		// Use the Properties.getProperty to determine if property is present
		// in its original form
		String result = super.getProperty(property);
		if (result != null)
			return result;

		// Look for the property in a case-insensitive fashion
		String upper = property.toUpperCase();
		Enumeration keyEnum = keys();
		while (keyEnum.hasMoreElements()) {
			String key = (String) keyEnum.nextElement();
			if (key.toUpperCase().equals(upper))
				return getProperty(key);
		}

		// Property was not found
		return null;
	}

	String lookupProperty(String prefix, String key) {
		String property = prefix + key;
		String result = lookupProperty(property);
		if (result != null)
			return result;
		result = lookupProperty("Alg.Alias." + property);
		if (result != null)
			return lookupProperty(prefix + result);
		return null;
	}
}
