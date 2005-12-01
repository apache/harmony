/* Copyright 2001, 2005 The Apache Software Foundation or its licensors, as applicable
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

package java.lang.reflect;


import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.ibm.oti.lang.reflect.ProxyClassFile;
import com.ibm.oti.util.Msg;

/**
 * This class provides methods to creating dynamic proxy classes and instances.
 * 
 * @see java.lang.reflect.InvocationHandler
 */
public class Proxy implements java.io.Serializable {

	static final long serialVersionUID = -2222568056686623797L;

	// maps class loaders to created classes by interface names
	private static Map loaderCache = new WeakHashMap();

	// to find previously created types
	private static Map proxyCache = new WeakHashMap();

	private static int NextClassNameIndex = 0;

	protected InvocationHandler h;

	private Proxy() {
	}

	protected Proxy(InvocationHandler h) {
		this.h = h;
	}

	/**
	 * Return the dynamically build class for the given interfaces, build a new
	 * one when necessary. The order of the interfaces is important.
	 * 
	 * The interfaces must be visible from the supplied class loader; no
	 * duplicates are permitted. All non-public interfaces must be defined in
	 * the same package.
	 * 
	 * @param loader
	 *            the class loader that will define the proxy class.
	 * @param interfaces
	 *            an array of <code>Class</code> objects, each one identifying
	 *            an interface that the new proxy must implement
	 * @return a proxy class that implements all of the interfaces referred to
	 *         in the contents of <code>interfaces</code>.
	 * @exception IllegalArgumentException
	 * @exception NullPointerException
	 *                if either <code>interfaces</code> or any of its elements
	 *                are <code>null</code>.
	 */
	public static Class getProxyClass(ClassLoader loader, Class[] interfaces)
			throws IllegalArgumentException {
		// check that interfaces are a valid array of visible interfaces
		if (interfaces == null)
			throw new NullPointerException();
		String commonPackageName = null;
		for (int i = 0, length = interfaces.length; i < length; i++) {
			Class next = interfaces[i];
			if (next == null)
				throw new NullPointerException();
			String name = next.getName();
			if (!next.isInterface())
				throw new IllegalArgumentException(Msg.getString("K00ed", name));
			if (loader != next.getClassLoader()) {
				try {
					if (next != Class.forName(name, false, loader))
						throw new IllegalArgumentException(Msg.getString(
								"K00ee", name));
				} catch (ClassNotFoundException ex) {
					throw new IllegalArgumentException(Msg.getString("K00ee",
							name));
				}
			}
			for (int j = i + 1; j < length; j++)
				if (next == interfaces[j])
					throw new IllegalArgumentException(Msg.getString("K00ef",
							name));
			if (!Modifier.isPublic(next.getModifiers())) {
				int last = name.lastIndexOf('.');
				String p = last == -1 ? "" : name.substring(0, last);
				if (commonPackageName == null)
					commonPackageName = p;
				else if (!commonPackageName.equals(p))
					throw new IllegalArgumentException(Msg.getString("K00f0"));
			}
		}

		// search cache for matching proxy class using the class loader
		synchronized (loaderCache) {
			Map interfaceCache = (Map) loaderCache.get(loader);
			if (interfaceCache == null)
				loaderCache.put(loader, (interfaceCache = new HashMap()));

			String interfaceKey = "";
			if (interfaces.length == 1) {
				interfaceKey = interfaces[0].getName();
			} else {
				StringBuffer names = new StringBuffer();
				for (int i = 0, length = interfaces.length; i < length; i++) {
					names.append(interfaces[i].getName());
					names.append(' ');
				}
				interfaceKey = names.toString();
			}

			Class newClass;
			WeakReference ref = (WeakReference) interfaceCache
					.get(interfaceKey);
			if (ref == null) {
				String nextClassName = "$Proxy" + NextClassNameIndex++;
				if (commonPackageName != null)
					nextClassName = commonPackageName + "." + nextClassName;
				byte[] classFileBytes = ProxyClassFile.generateBytes(
						nextClassName, interfaces);
				if (loader == null)
					loader = ClassLoader.getSystemClassLoader();
				newClass = defineClassImpl(loader, nextClassName.replace('.',
						'/'), classFileBytes);
				// Need a weak reference to the class so it can
				// be unloaded if the class loader is discarded
				interfaceCache.put(interfaceKey, new WeakReference(newClass));
				synchronized (proxyCache) {
					proxyCache.put(newClass, ""); // the value is unused
				}
			} else {
				newClass = (Class) ref.get();
			}
			return newClass;
		}
	}

	/**
	 * Return an instance of the dynamically build class for the given
	 * interfaces that forwards methods to the specified invocation handler.
	 * 
	 * The interfaces must be visible from the supplied class loader; no
	 * duplicates are permitted. All non-public interfaces must be defined in
	 * the same package.
	 * 
	 * @param loader
	 *            the class loader that will define the proxy class.
	 * @param interfaces
	 *            the list of interfaces to implement.
	 * @param h
	 *            the invocation handler for the forwarded methods.
	 * @return a new proxy object that delegates to the handler <code>h</code>
	 * @exception IllegalArgumentException
	 * @exception NullPointerException
	 *                if the interfaces or any of its elements are null.
	 */
	public static Object newProxyInstance(ClassLoader loader,
			Class[] interfaces, InvocationHandler h)
			throws IllegalArgumentException {
		if (h != null) {
			try {
				return getProxyClass(loader, interfaces).getConstructor(
						new Class[] { InvocationHandler.class }).newInstance(
						new Object[] { h });
			} catch (NoSuchMethodException ex) {
				throw new InternalError(ex.toString());
			} catch (IllegalAccessException ex) {
				throw new InternalError(ex.toString());
			} catch (InstantiationException ex) {
				throw new InternalError(ex.toString());
			} catch (InvocationTargetException ex) {
				throw new InternalError(ex.getTargetException().toString());
			}
		}
		throw new NullPointerException();
	}

	/**
	 * Return whether the supplied class is a dynamically generated proxy class.
	 * 
	 * @param cl
	 *            the class.
	 * @return true if the class is a proxy class and false otherwise.
	 * @exception NullPointerException
	 *                if the class is null.
	 */
	public static boolean isProxyClass(Class cl) {
		if (cl != null) {
			synchronized (proxyCache) {
				return proxyCache.containsKey(cl);
			}
		}
		throw new NullPointerException();
	}

	/**
	 * Return the proxy instance's invocation handler.
	 * 
	 * @param proxy
	 *            the proxy instance.
	 * @return the proxy's invocation handler object
	 * @exception IllegalArgumentException
	 *                if the supplied <code>proxy</code> is not a proxy
	 *                object.
	 */
	public static InvocationHandler getInvocationHandler(Object proxy)
			throws IllegalArgumentException {
		if (isProxyClass(proxy.getClass()))
			return ((Proxy) proxy).h;

		throw new IllegalArgumentException(Msg.getString("K00f1"));
	}

	private static native Class defineClassImpl(ClassLoader classLoader,
			String className, byte[] classFileBytes);

}
