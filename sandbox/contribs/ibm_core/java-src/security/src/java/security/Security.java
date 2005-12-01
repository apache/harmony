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


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * For access to security providers and properties.
 */
public final class Security {
	// List of alternating key-value pairs which represent the
	// default content of the securityProperties collection.
	private static final String[] defaultProperties = { "package.access",
			"com.ibm.oti.", "policy.provider",
			"com.ibm.oti.util.DefaultPolicy", "security.provider.1",
			"com.ibm.oti.security.provider.OTI" };

	// Security Properties.
	private static final Properties securityProperties = loadSecurityProperties();

	// Providers sorted by priority.
	private static final Vector providersByPriority = new Vector();

	// Associations between name and Provider.
	private static final Hashtable providersByName = new Hashtable(20);

	// have the providers been loaded and initialized?
	private static boolean providersLoaded = false;

	/**
	 * Constructs a new instance of this class.
	 * 
	 */
	private Security() {
		super();
	}

	/**
	 * Answers the value of the security property named by the argument.
	 * 
	 * 
	 * @param key
	 *            String The property name
	 * @return String The property value
	 * 
	 * @exception SecurityException
	 *                If there is a SecurityManager installed and it will not
	 *                allow the property to be fetched from the current access
	 *                control context.
	 */
	public static String getProperty(String key) {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(new SecurityPermission("getProperty."
					+ key));
		return securityProperties.getProperty(key);
	}

	/**
	 * Sets a given security property.
	 * 
	 * 
	 * @param key
	 *            String The property name.
	 * @param datum
	 *            String The property value.
	 * @exception SecurityException
	 *                If there is a SecurityManager installed and it will not
	 *                allow the property to be set from the current access
	 *                control context.
	 */
	public static void setProperty(String key, String datum) {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(new SecurityPermission("setProperty."
					+ key));
		securityProperties.put(key, datum);
	}

	/**
	 * Adds the extra provider to the collection of providers.
	 * @param provider 
	 * 
	 * @return int The priority/position of the provider added.
	 * @exception SecurityException
	 *                If there is a SecurityManager installed and it denies
	 *                adding a new provider.
	 */
	public static int addProvider(Provider provider) {
		// must load providers before getting size
		if (!providersLoaded)
			loadSecurityProviders();
		synchronized (providersByPriority) {
			return insertProviderAt(provider, providersByPriority.size() + 1);
		}
	}

	/**
	 * Answers a Provider for a given name.
	 * 
	 * 
	 * @param providerName
	 *            java.lang.String Name of the provider we are trying to find.
	 * @return Provider if a provider was found, or <code>null</code> if there
	 *         was no such provider.
	 */
	public static Provider getProvider(String providerName) {
		if (!providersLoaded)
			loadSecurityProviders();
		return (Provider) providersByName.get(providerName);
	}

	/**
	 * Answers a collection of installed providers.
	 * 
	 * 
	 * @return Provider[] a collection of <code>provider</code> installed.
	 */
	public static Provider[] getProviders() {
		if (!providersLoaded)
			loadSecurityProviders();
		synchronized (providersByPriority) {
			Provider[] result = new Provider[providersByPriority.size()];
			providersByPriority.copyInto(result);
			return result;
		}
	}

	/**
	 * Constant value for criteria
	 */
	private final static int CRYPTO_SERVICE = 0;

	private final static int ALGORITHM_OR_TYPE = 1;

	private final static int ATTRIBUTE_NAME = 2;

	private final static int ATTRIBUTE_VALUE = 3;

	/**
	 * Check attribute value
	 * 
	 * 
	 * @param attrName
	 *            String attribute name
	 * @param attrValue
	 *            String attribute value
	 * @param sourceValue
	 *            String provider supports this attribute value
	 * 
	 * @return boolean true, if condition for attribute is true
	 * 
	 */
	private static boolean checkAttribute(String attrName, String attrValue,
			String sourceValue) {
		if (attrValue.length() == 0)
			return true;

		try {
			int v1 = Integer.parseInt(sourceValue);
			int v2 = Integer.parseInt(attrValue);
			return v1 >= v2;
		} catch (NumberFormatException e) {
		}
		return sourceValue.equals(attrValue);
	}

	/**
	 * Parser for filter
	 * 
	 * 
	 * @param filter
	 *            String the criteria for selecting providers
	 * 
	 * @return String[] A list of filter's element for selecting providers
	 *         {CRYPTO_SERVICE,ALGORITHM_OR_TYPE,ATTRIBUTE_NAME,ATTRIBUTE_VALUE}
	 */
	private static String[] parserOfFilter(String filter) {
		// criterion
		String[] filters = { null, null, null, null };
		char[] ch = { '.', ' ', ':' };
		// parser
		int begin = 0;
		int end = 0;
		int length = filter.length();
		int i = 0;
		for (i = 0; (i < ch.length) && (begin < length) && (end != -1); i++) {
			end = filter.indexOf(ch[i], begin);
			if (end == -1) {
				filters[i] = new String(filter.substring(begin, length));
				break;
			}
			filters[i] = new String(filter.substring(begin, end));
			begin = end + 1;
			while (begin < length && filter.charAt(begin) == ' ')
				begin++;
		}
		if (end != -1)
			filters[i] = new String(filter.substring(begin, length));
		return filters;
	}

	private static Provider[] getProvidersUsingFilters(Provider[] providers,
			String[] filters, int typeOfFilter) {

		int numberOfProviders = providers.length;

		if (numberOfProviders > 0) {

			String bufKey = filters[CRYPTO_SERVICE] + "."
					+ filters[ALGORITHM_OR_TYPE];
			String algAlias = "Alg.Alias." + bufKey;

			boolean isNotFound = true;

			for (int i = 0; i < providers.length; i++) {
				isNotFound = true;

				String key = bufKey;

				String alias = providers[i].getProperty(algAlias);
				if (alias != null) {
					key = filters[CRYPTO_SERVICE] + "." + alias;
				}

				String property = providers[i].getProperty(key);
				if (property != null) {
					if (typeOfFilter == 1) {
						isNotFound = false;
					} else if (typeOfFilter == 2) {
						key = key + ' ' + filters[ATTRIBUTE_NAME];

						String value = providers[i].getProperty(key);

						if (value != null) {
							if (checkAttribute(filters[ATTRIBUTE_NAME],
									filters[ATTRIBUTE_VALUE], value))
								isNotFound = false;
						}
					}
				}

				if (isNotFound) {
					providers[i] = null;
					numberOfProviders--;
				}
			}

			if (numberOfProviders > 0) {
				Provider[] filterResult = new Provider[numberOfProviders];
				int j = 0;
				for (int i = 0; i < providers.length; i++) {
					if (providers[i] != null) {
						filterResult[j++] = providers[i];
					}
				}
				providers = filterResult;
			} else {
				providers = null;
			}
		} else {
			providers = null;
		}
		return providers;
	}

	/**
	 * Returns the collection of providers which meet the user supplied string
	 * filter.
	 * 
	 * @param filter
	 *            case-insensitive filter
	 * @return the providers which meet the user supplied string filter
	 *         <code>filter</code>. A <code>null</code> value signifies
	 *         that none of the installed providers meets the filter
	 *         specification
	 * @exception InvalidParameterException
	 *                if an unusable filter is supplied
	 */
	public static Provider[] getProviders(String filter) {
		Provider[] providers = getProviders();
		if (providers.length == 0)
			return null;
		String[] filters = parserOfFilter(filter);
		int typeOfFilter = 0;
		if (filters[CRYPTO_SERVICE] != null
				&& filters[ALGORITHM_OR_TYPE] != null) {
			if (filters[ATTRIBUTE_NAME] != null
					|| filters[ATTRIBUTE_VALUE] != null) {
				if (filters[ATTRIBUTE_NAME] != null
						&& filters[ATTRIBUTE_VALUE] != null) {
					if (filters[ATTRIBUTE_NAME].length() > 0) {
						typeOfFilter = 2;
					} else if (filters[ATTRIBUTE_VALUE].length() == 0) {
						typeOfFilter = 1;
					}
				}
			} else {
				if (filters[ALGORITHM_OR_TYPE].length() > 0)
					typeOfFilter = 1;
			}
		}
		if (typeOfFilter == 0)
			throw new InvalidParameterException(com.ibm.oti.util.Msg
					.getString("K01a6"));
		return getProvidersUsingFilters(providers, filters, typeOfFilter);
	}

	/**
	 * Returns the collection of providers which meet the user supplied map of
	 * string filter values.
	 * 
	 * @param filter
	 *            a {@link Map} of case-insensitive filter strings each of which
	 *            must be satisfied before a provider is included as an element
	 *            in the return value.
	 * @return the providers which meet the user supplied string filter
	 *         <code>filter</code>. A <code>null</code> value signifies
	 *         that none of the installed providers meets the filter
	 *         specification
	 * @exception InvalidParameterException
	 *                if any of the supplied filters are unusable
	 */
	public static Provider[] getProviders(Map filter) {
		Provider[] providers = getProviders();
		if (providers.length == 0)
			return null;
		if (filter == null)
			return providers;
		Set keySet = filter.keySet();
		if (keySet == null)
			return providers;
		Iterator keys = keySet.iterator();
		String key;
		String value;
		String[] filters;
		int typeOfFilter;
		while (keys.hasNext()) {
			key = (String) keys.next();
			value = (String) filter.get(key);
			filters = parserOfFilter(key);
			typeOfFilter = 0;
			if (filters[CRYPTO_SERVICE] != null
					&& filters[ALGORITHM_OR_TYPE] != null) {
				if (filters[ATTRIBUTE_NAME] != null) {
					if (filters[ATTRIBUTE_VALUE] != null) {
						throw new InvalidParameterException(
								com.ibm.oti.util.Msg.getString("K01a6"));
					}
					// <crypto_service>.<algorithm_or_type> <attribute_name>
					if (value.length() >= 0) {
						// <crypto_service>.<algorithm_or_type>
						// <attribute_name>:<attribute_value>
						typeOfFilter = 2;
						filters[ATTRIBUTE_VALUE] = value;
					}
				} else if (value.length() == 0) {
					// <crypto_service>.<algorithm_or_type>
					if (filters[ALGORITHM_OR_TYPE].length() > 0)
						typeOfFilter = 1;
				}
			}
			if (typeOfFilter == 0)
				throw new InvalidParameterException(com.ibm.oti.util.Msg
						.getString("K01a6"));
			providers = getProvidersUsingFilters(providers, filters,
					typeOfFilter);
			if (providers == null)
				return null;
		}
		return providers;
	}

	/**
	 * Adds a provider to the collection of providers, at the specified
	 * position.
	 * @param provider 
	 * @param position 
	 * 
	 * @return int The priority/position where the provider was actually added.
	 * @exception SecurityException
	 *                If there is a SecurityManager installed and it will not
	 *                allow a new provider to be installed from the current
	 *                access control context.
	 */
	public static int insertProviderAt(Provider provider, int position) {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security
					.checkSecurityAccess("insertProvider." + provider.getName());
		if (!providersLoaded)
			loadSecurityProviders();
		return insertAt(provider, position);
	}

	private static int insertAt(Provider provider, int position) {
		synchronized (providersByPriority) {
			if (providersByName.get(provider.getName()) != null)
				// Already registered, no-op and return -1
				return -1;

			// First adjust, positions are 1-based and Vector is 0-based
			position--;
			if (position < 0 || position > providersByPriority.size())
				position = providersByPriority.size();
			providersByPriority.insertElementAt(provider, position);
			providersByName.put(provider.getName(), provider);
		}
		// Now adjust again to 1-based offset
		return ++position;
	}

	/**
	 * Removes the provider from the collection of providers.
	 * 
	 * 
	 * @param name
	 *            String The name of the provider to remove.
	 * @exception SecurityException
	 *                If there is a SecurityManager installed and it will not
	 *                allow the provider to be removed by code in the current
	 *                access control context.
	 */
	public static void removeProvider(String name) {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkSecurityAccess("removeProvider." + name);
		if (!providersLoaded)
			loadSecurityProviders();
		synchronized (providersByPriority) {
			int foundIndex = -1;
			for (int i = 0; i < providersByPriority.size(); i++) {
				Provider p = (Provider) providersByPriority.elementAt(i);
				if (name.equals(p.getName())) {
					foundIndex = i;
					break;
				}
			}
			if (foundIndex >= 0) {
				// Found something
				providersByPriority.removeElementAt(foundIndex);
				providersByName.remove(name);
			}
		}
	}

	public static Set getAlgorithms(String serviceName) {

		Set result = new TreeSet();

		Provider[] providers = getProviders();
		for (int i = 0; i < providers.length; i++) {

			Provider p = providers[i];
			Set s = p.keySet();

			String searchName = serviceName.toLowerCase();

			for (Iterator iter = s.iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				String searchElement = element.toLowerCase();
				if (searchElement.indexOf(searchName) == 0) {
					int sepIndex = element.indexOf('.');
					String algorithm = element.substring(sepIndex + 1);
					result.add(algorithm);
				}
			}

		}

		return result;

	}

	/**
	 * Answers with the default security properties for the case where the
	 * receiver was not able to read the java.security file
	 * 
	 * 
	 * @return Properties A list of properties as defined in defaultProperties
	 */
	private static Properties loadDefaultProperties() {
		// Initialize the properties collection.
		Properties sp = new Properties();
		for (int i = 0; i < defaultProperties.length; i += 2)
			sp.put(defaultProperties[i], defaultProperties[i + 1]);
		return sp;
	}

	/**
	 * Answers with the default security properties for the case where the
	 * receiver was not able to read the java.security file
	 * 
	 * 
	 * @return Properties A list of properties as defined in defaultProperties
	 */
	private static Properties loadSecurityProperties() {
		Properties sp = null;
		String javahome = System.getProperty("java.home");
		String securityPropertiesFileName = new StringBuffer(
				javahome.length() + 30)
				.append(javahome)
				.append(
						System.getProperty("file.separator").equals("\\") ? "\\lib\\security\\java.security"
								: "/lib/security/java.security").toString();
		File securityPropertiesFile = new File(securityPropertiesFileName);
		if (securityPropertiesFile.exists()) {
			InputStream in = null;
			try {
				in = new BufferedInputStream(new FileInputStream(
						securityPropertiesFile));
				sp = new Properties();
				sp.load(in);

				// trim trailing whitespace
				Enumeration keyEnum = sp.keys();
				while (keyEnum.hasMoreElements()) {
					String key = (String) keyEnum.nextElement();
					sp.put(key, ((String) sp.get(key)).trim());
				}
			} catch (FileNotFoundException e) {
				// shouldn't happen
			} catch (IOException e) {
				sp = null;
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
					}
				}
			}
		}
		// if the java.security file load failed, use the default system ones
		if (sp == null)
			sp = loadDefaultProperties();

		return sp;
	}

	/**
	 * Loads and initializes the security providers specified in the security
	 * properties list.
	 * 
	 * 
	 */
	private static void loadSecurityProviders() {
		synchronized (providersByPriority) {
			// needed for syncronization issues
			if (providersLoaded)
				return;
			// must be set before calling addProvider()
			providersLoaded = true;
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					int providerNum = 1;
					String providerName;
					while ((providerName = getProperty("security.provider."
							+ providerNum++)) != null) {
						try {
							Class providerClass = Class.forName(providerName,
									true, ClassLoader.getSystemClassLoader());
							Provider provider = (Provider) providerClass
									.newInstance();
							insertAt(provider, providersByPriority.size() + 1);
						} catch (ClassNotFoundException cnf) {
						} catch (IllegalAccessException iae) {
						} catch (InstantiationException ie) {
						}
					}
					return null;
				}
			});
		}
	}

	/**
	 * Deprecated method which returns null.
	 * @param algorithm 
	 * @param property 
	 * @return <code>null</code>
	 *
	 * @deprecated	Use AlgorithmParameters and KeyFactory instead
	 */
	public static String getAlgorithmProperty(String algorithm, String property) {
		return null;
	}
}
