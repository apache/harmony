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

package java.util;


import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.oti.vm.VM;

/**
 * ResourceBundle is an abstract class which is the superclass of classes which
 * provide locale specific resources. A bundle contains a number of named
 * resources, where the names are Strings. A bundle may have a parent bundle,
 * when a resource is not found in a bundle, the parent bundle is searched for
 * the resource.
 * 
 * @see Properties
 * @see PropertyResourceBundle
 */
abstract public class ResourceBundle {
	
	/**
	 * The parent of this ResourceBundle.
	 */
	protected ResourceBundle parent;

	private Locale locale;

	static class MissingBundle extends ResourceBundle {
		public Enumeration getKeys() {
			return null;
		}

		public Object handleGetObject(String name) {
			return null;
		}
	}

	private static ResourceBundle MISSING = new MissingBundle();

	private static ResourceBundle MISSINGBASE = new MissingBundle();

	private static WeakHashMap cache = new WeakHashMap();

	/**
	 * Constructs a new instance of this class.
	 * 
	 */
	public ResourceBundle() {
		/*empty*/
	}

	/**
	 * Finds the named resource bundle for the default locale.
	 * 
	 * @param bundleName
	 *            the name of the resource bundle
	 * @return ResourceBundle
	 * 
	 * @exception MissingResourceException
	 *                when the resource bundle cannot be found
	 */
	public static final ResourceBundle getBundle(String bundleName)
			throws MissingResourceException {
		return getBundleImpl(bundleName, Locale.getDefault(), VM
				.callerClassLoader());
	}

	/**
	 * Finds the named resource bundle for the specified locale.
	 * 
	 * @param bundleName
	 *            the name of the resource bundle
	 * @param locale
	 *            the locale
	 * @return ResourceBundle
	 * 
	 * @exception MissingResourceException
	 *                when the resource bundle cannot be found
	 */
	public static final ResourceBundle getBundle(String bundleName,
			Locale locale) {
		return getBundleImpl(bundleName, locale, VM.callerClassLoader());
	}

	/**
	 * Finds the named resource bundle for the specified locale.
	 * 
	 * @param bundleName
	 *            the name of the resource bundle
	 * @param locale
	 *            the locale
	 * @param loader
	 *            the ClassLoader to use
	 * @return ResourceBundle
	 * 
	 * @exception MissingResourceException
	 *                when the resource bundle cannot be found
	 */
	public static ResourceBundle getBundle(String bundleName, Locale locale,
			ClassLoader loader) throws MissingResourceException {
		if (loader == null)
			throw new NullPointerException();
		if (bundleName != null) {
			ResourceBundle bundle;
			if (!locale.equals(Locale.getDefault()))
				if ((bundle = handleGetBundle(bundleName, "_" + locale, false, //$NON-NLS-1$
						loader)) != null)
					return bundle;
			if ((bundle = handleGetBundle(bundleName,
					"_" + Locale.getDefault(), true, loader)) != null) //$NON-NLS-1$
				return bundle;
			throw new MissingResourceException(null, bundleName, ""); //$NON-NLS-1$
		} else
			throw new NullPointerException();
	}

	private static ResourceBundle getBundleImpl(String bundleName,
			Locale locale, ClassLoader loader) throws MissingResourceException {
		if (bundleName != null) {
			ResourceBundle bundle;
			if (!locale.equals(Locale.getDefault())) {
				String localeName = locale.toString();
				if (localeName.length() > 0)
					localeName = "_" + localeName; //$NON-NLS-1$
				if ((bundle = handleGetBundle(bundleName, localeName, false,
						loader)) != null)
					return bundle;
			}
			String localeName = Locale.getDefault().toString();
			if (localeName.length() > 0)
				localeName = "_" + localeName; //$NON-NLS-1$
			if ((bundle = handleGetBundle(bundleName, localeName, true, loader)) != null)
				return bundle;
			throw new MissingResourceException(null, bundleName, ""); //$NON-NLS-1$
		} else
			throw new NullPointerException();
	}

	/**
	 * Answers the names of the resources contained in this ResourceBundle.
	 * 
	 * @return an Enumeration of the resource names
	 */
	public abstract Enumeration getKeys();

	/**
	 * Gets the Locale of this ResourceBundle.
	 * 
	 * @return the Locale of this ResourceBundle
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Answers the named resource from this ResourceBundle.
	 * 
	 * @param key
	 *            the name of the resource
	 * @return the resource object
	 * 
	 * @exception MissingResourceException
	 *                when the resource is not found
	 */
	public final Object getObject(String key) {
		ResourceBundle last, theParent = this;
		do {
			Object result = theParent.handleGetObject(key);
			if (result != null)
				return result;
			last = theParent;
			theParent = theParent.parent;
		} while (theParent != null);
		throw new MissingResourceException(null, last.getClass().getName(), key);
	}

	/**
	 * Answers the named resource from this ResourceBundle.
	 * 
	 * @param key
	 *            the name of the resource
	 * @return the resource string
	 * 
	 * @exception MissingResourceException
	 *                when the resource is not found
	 */
	public final String getString(String key) {
		return (String) getObject(key);
	}

	/**
	 * Answers the named resource from this ResourceBundle.
	 * 
	 * @param key
	 *            the name of the resource
	 * @return the resource string array
	 * 
	 * @exception MissingResourceException
	 *                when the resource is not found
	 */
	public final String[] getStringArray(String key) {
		return (String[]) getObject(key);
	}

	private static ResourceBundle handleGetBundle(String base, String locale,
			boolean loadBase, final ClassLoader loader) {
		ResourceBundle bundle = null;
		String bundleName = base + locale;
		Object cacheKey = loader != null ? (Object) loader : (Object) "null"; //$NON-NLS-1$
		Hashtable loaderCache;
		synchronized (cache) {
			loaderCache = (Hashtable) cache.get(cacheKey);
			if (loaderCache == null) {
				loaderCache = new Hashtable(13);
				cache.put(cacheKey, loaderCache);
			}
		}
		ResourceBundle result = (ResourceBundle) loaderCache.get(bundleName);
		if (result != null) {
			if (result == MISSINGBASE)
				return null;
			if (result == MISSING) {
				if (!loadBase)
					return null;
				String extension = strip(locale);
				if (extension == null)
					return null;
				return handleGetBundle(base, extension, loadBase, loader);
			}
			return result;
		}

		try {
			Class bundleClass = Class.forName(bundleName, true, loader);
			bundle = (ResourceBundle) bundleClass.newInstance();
			bundle.setLocale(locale);
		} catch (Exception e) {
		} catch (LinkageError e) {
		}

		if (bundle == null) {
			final String fileName = bundleName.replace('.', '/');
			InputStream stream = null;
			stream = (InputStream) AccessController
					.doPrivileged(new PrivilegedAction() {
						public Object run() {
							return loader == null ? ClassLoader
									.getSystemResourceAsStream(fileName
											+ ".properties") : loader //$NON-NLS-1$
									.getResourceAsStream(fileName
											+ ".properties"); //$NON-NLS-1$
						}
					});
			if (stream != null) {
				try {
					bundle = new PropertyResourceBundle(stream);
					bundle.setLocale(locale);
					stream.close();
				} catch (IOException e) {
				}
			}
		}

		String extension = strip(locale);
		if (bundle != null) {
			if (extension != null) {
				ResourceBundle parent = handleGetBundle(base, extension, true,
						loader);
				if (parent != null)
					bundle.setParent(parent);
			}
			loaderCache.put(bundleName, bundle);
			return bundle;
		}

		if (extension != null && (loadBase || extension.length() > 0)) {
			bundle = handleGetBundle(base, extension, loadBase, loader);
			if (bundle != null) {
				loaderCache.put(bundleName, bundle);
				return bundle;
			}
		}
		loaderCache.put(bundleName, loadBase ? MISSINGBASE : MISSING);
		return null;
	}

	/**
	 * Answers the named resource from this ResourceBundle, or null if the
	 * resource is not found.
	 * 
	 * @param key
	 *            the name of the resource
	 * @return the resource object
	 */
	protected abstract Object handleGetObject(String key);

	/**
	 * Sets the parent resource bundle of this ResourceBundle. The parent is
	 * searched for resources which are not found in this resource bundle.
	 * 
	 * @param bundle
	 *            the parent resource bundle
	 */
	protected void setParent(ResourceBundle bundle) {
		parent = bundle;
	}

	private static String strip(String name) {
		int index = name.lastIndexOf('_');
		if (index != -1)
			return name.substring(0, index);
		return null;
	}

	private void setLocale(String name) {
		String language = "", country = "", variant = "";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		if (name.length() > 1) {
			int nextIndex = name.indexOf('_', 1);
			if (nextIndex == -1)
				nextIndex = name.length();
			language = name.substring(1, nextIndex);
			if (nextIndex + 1 < name.length()) {
				int index = nextIndex;
				nextIndex = name.indexOf('_', nextIndex + 1);
				if (nextIndex == -1)
					nextIndex = name.length();
				country = name.substring(index + 1, nextIndex);
				if (nextIndex + 1 < name.length())
					variant = name.substring(nextIndex + 1, name.length());
			}
		}
		locale = new Locale(language, country, variant);
	}
}
