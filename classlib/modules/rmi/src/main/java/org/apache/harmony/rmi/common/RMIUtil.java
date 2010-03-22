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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
package org.apache.harmony.rmi.common;

import java.io.IOException;

import java.lang.reflect.Method;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Utility class for RMI implementation.
 *
 * This class cannot be instantiated.
 *
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public final class RMIUtil {

    /**
     * This class cannot be instantiated.
     */
    private RMIUtil() {}

    /**
     * Returns wrapping Object class for specified primitive class.
     *
     * @param   cls
     *          Class to wrap.
     *
     * @return  Wrapping Object class for <code>cls</code>, if <code>cls</code>
     *          is Object class itself (e. g. <code>Vector</code>),
     *          <code>cls</code> itself is returned, for primitive types
     *          (e. g. <code>int</code>) the respective wrapping Object class
     *          is returned (in case of <code>int</code>, {@link Integer}).
     */
    public static Class getWrappingClass(Class cls) {
        if (cls == boolean.class) {
            return Boolean.class;
        } else if (cls == char.class) {
            return Character.class;
        } else if (cls == byte.class) {
            return Byte.class;
        } else if (cls == short.class) {
            return Short.class;
        } else if (cls == int.class) {
            return Integer.class;
        } else if (cls == long.class) {
            return Long.class;
        } else if (cls == float.class) {
            return Float.class;
        } else if (cls == double.class) {
            return Double.class;
        } else if (cls == void.class) {
            return Void.class;
        } else { // Object type.
            return cls;
        }
    }

    /**
     * Returns package name for the class.
     *
     * @param   cls
     *          Class to get package name for.
     *
     * @return  Package name of the class,
     *          or <code>null</code> if class does not belong to a package.
     */
    public static String getPackageName(Class cls) {
        if (cls.isArray() || cls.isPrimitive()) {
            return null;
        }
        String name = cls.getName();
        int index = name.lastIndexOf('.');

        return ((index > 0) ? name.substring(0, index) : null);
    }

    /**
     * Returns canonical name for the class, e. g. full class name with
     * package name, with <code>[]</code> appended to the end for array types.
     * Handles local classes correctly.
     *
     * @param   cls
     *          Class to get canonical name for.
     *
     * @return  Canonical name of the class.
     *
     * @todo    Remove completely for Java 5.0 in favor of
     *          <code>Class.getCanonicalName()</code>.
     */
    public static String getCanonicalName(Class cls) {
        if (cls.isArray()) {
            // Use recursion to create name for array class.
            return (getCanonicalName(cls.getComponentType()) + "[]"); //$NON-NLS-1$
        }
        Class declaring = cls.getDeclaringClass();

        if (declaring != null) {
            // Use recursion to create name for local classes.
            return (getCanonicalName(declaring) + '.' + getSimpleName(cls));
        }
        return cls.getName();
    }

    /**
     * Returns short canonical name for the class, e. g. short class name
     * without package name (but with '.' symbols for local classes),
     * with <code>[]</code> appended to the end for array types.
     *
     * @param   cls
     *          Class to get short canonical name for.
     *
     * @return  Short canonical name of the class.
     */
    public static String getShortCanonicalName(Class cls) {
        if (cls.isArray()) {
            // Use recursion to create name for array class.
            return (getShortCanonicalName(cls.getComponentType()) + "[]"); //$NON-NLS-1$
        }

        // The last dot in full name separates class name from package name.
        int index = cls.getName().lastIndexOf('.');

        // Canonical name uses dots to separate local class names.
        String name = getCanonicalName(cls);

        return ((index > 0) ? name.substring(index + 1) : name);
    }

    /**
     * Returns short name for the class, e. g. short class name without
     * package name (but with '$' symbols for local classes),
     * with <code>[]</code> appended to the end for array types.
     *
     * @param   cls
     *          Class to get short name for.
     *
     * @return  Short name of the class.
     */
    public static String getShortName(Class cls) {
        if (cls.isArray()) {
            // Use recursion to create name for array class.
            return (getShortName(cls.getComponentType()) + "[]"); //$NON-NLS-1$
        }
        String name = cls.getName();
        int index = name.lastIndexOf('.');

        return ((index > 0) ? name.substring(index + 1) : name);
    }

    /**
     * Returns simple name for the class, e. g. short class name without
     * package name or declaring class name, with <code>[]</code> appended
     * to the end for array types.
     *
     * @param   cls
     *          Class to get simple name for.
     *
     * @return  Simple name of the class.
     *
     * @todo    Remove completely for Java 5.0 in favor of
     *          <code>Class.getSimpleName()</code>.
     */
    public static String getSimpleName(Class cls) {
        if (cls.isArray()) {
            // Use recursion to create name for array class.
            return (getSimpleName(cls.getComponentType()) + "[]"); //$NON-NLS-1$
        }
        String name = cls.getName();
        Class declaring = cls.getDeclaringClass();

        if (declaring != null) {
            // Use substringing to extract simple name of a local class.
            return (name.substring(declaring.getName().length() + 1));
        }
        int index = name.lastIndexOf('.');

        return ((index > 0) ? name.substring(index + 1) : name);
    }

    /**
     * Returns system name for the class, e. g.
     * <code>I</code> for <code>int</code>,
     * <code>[[B</code> for <code>boolean[][]</code>,
     * <code>[Ljava/lang/String;</code> for <code>String[]</code>.
     *
     * @param   cls
     *          Class to get system name for.
     *
     * @return  System name of the class.
     */
    public static String getSystemName(Class cls) {
        if (cls == boolean.class) {
            return "Z"; //$NON-NLS-1$
        } else if (cls == char.class) {
            return "C"; //$NON-NLS-1$
        } else if (cls == byte.class) {
            return "B"; //$NON-NLS-1$
        } else if (cls == short.class) {
            return "S"; //$NON-NLS-1$
        } else if (cls == int.class) {
            return "I"; //$NON-NLS-1$
        } else if (cls == long.class) {
            return "J"; //$NON-NLS-1$
        } else if (cls == float.class) {
            return "F"; //$NON-NLS-1$
        } else if (cls == double.class) {
            return "D"; //$NON-NLS-1$
        } else if (cls == void.class) {
            return "V"; //$NON-NLS-1$
        } else { // Object type.
            String className = cls.getName().replace('.', '/');

            // Add reference to non-array reference types.
            return (cls.isArray() ? className : ('L' + className + ';'));
        }
    }

    /**
     * Returns method descriptor as specified in section 4.3.3
     * of Virtual Machine Specification.
     *
     * @param   method
     *          Method to return descriptor for.
     *
     * @return  Method descriptor.
     */
    public static String getMethodDescriptor(Method method) {
        StringBuilder buffer = new StringBuilder().append('(');
        Class[] parameters = method.getParameterTypes();

        for (int i = 0; i < parameters.length; i++) {
            buffer.append(getSystemName(parameters[i]));
        }
        buffer.append(')').append(getSystemName(method.getReturnType()));

        return buffer.toString();
    }

    /**
     * Returns extended method descriptor,
     * i. e. method name appended with method descriptor
     * as specified in section 4.3.3
     * of Virtual Machine Specification.
     *
     * @param   method
     *          Method to return extended descriptor for.
     *
     * @return  Extended method descriptor.
     */
    public static String getExtendedMethodDescriptor(Method method) {
        return (method.getName() + getMethodDescriptor(method));
    }

    /**
     * Returns basic method signature (method name and full parameters
     * class names) for the method.
     *
     * @param   method
     *          Method to get basic signature for.
     *
     * @return  Basic method signature (method name and full parameters
     *          class names) for the method. For example, for this particular
     *          method the long signature will be: <code>
     *          "getBasicMethodSignature(java.lang.reflect.Method)"</code>.
     */
    public static String getBasicMethodSignature(Method method) {
        // Start with method name.
        StringBuilder buffer = new StringBuilder()
                .append(method.getName()).append('(');
        Class[] parameters = method.getParameterTypes();

        // Append names of parameter types.
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                buffer.append(", "); //$NON-NLS-1$
            }
            buffer.append(getCanonicalName(parameters[i]));
        }
        return buffer.append(')').toString();
    }

    /**
     * Returns long method signature (return type, method name and full
     * parameters class names) for the method.
     *
     * @param   method
     *          Method to get long signature for.
     *
     * @return  Long method signature (return type, method name and full
     *          parameters class names) for the method. For example, for this
     *          particular method the long signature will be: <code>
     *          "java.lang.String
     *          getLongMethodSignature(java.lang.reflect.Method)"</code>.
     */
    public static String getLongMethodSignature(Method method) {
        StringBuilder suffix = new StringBuilder();
        Class cls = method.getReturnType();

        // Create signature suffix for array types.
        while (cls.isArray()) {
            suffix.append("[]"); //$NON-NLS-1$
            cls = cls.getComponentType();
        }
        return (getCanonicalName(cls) + ' '
                + getBasicMethodSignature(method) + suffix);
    }

    /**
     * Returns short method signature (without return type,
     * declaring class name or parameters package names) for the method.
     *
     * @param   method
     *          Method to get short signature for.
     *
     * @return  Short method signature (without return type,
     *          declaring class name or parameters package names)
     *          for the method. For example, for this particular method
     *          the short signature will be:
     *          <code>"getShortMethodSignature(Method)"</code>.
     */
    public static String getShortMethodSignature(Method method) {
        // Start with method name.
        StringBuilder buffer = new StringBuilder(method.getName() + '(');
        Class[] parameters = method.getParameterTypes();

        // Append short names of parameter types.
        for (int i = 0; i < parameters.length; i++) {
            buffer.append(((i > 0) ? ", " : "") //$NON-NLS-1$ //$NON-NLS-2$
                    + getShortCanonicalName(parameters[i]));
        }
        return buffer.append(')').toString();
    }

    /**
     * Validates remote interface.
     * Particularly, checks that all methods throw {@link RemoteException}.
     *
     * @param   iface
     *          Interface to validate.
     *
     * @return  <code>true</code> if the specified class is a valid remote
     *          interface, <code>false</code> if the specified class is not
     *          a remote interface.
     *
     * @throws  IllegalArgumentException
     *          If specified class is not an interface or if it implements
     *          {@link java.rmi.Remote} but is not a valid remote interface.
     */
    public static boolean checkRemoteInterface(Class iface)
            throws IllegalArgumentException {
        if (!iface.isInterface()) {
            // This is not an interface.
            // rmi.45={0} is not an interface
            throw new IllegalArgumentException(Messages.getString("rmi.45", //$NON-NLS-1$
                    iface.getName()));
        }

        if (!Remote.class.isAssignableFrom(iface)) {
            // This is not a remote interface.
            return false;
        }

        // Extract all methods from the specified interface.
        Method methods[] = iface.getMethods();

        methods:
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            // Extract thrown exceptions list from a particular method.
            Iterator j = Arrays.asList(method.getExceptionTypes()).iterator();

            while (j.hasNext()) {
                // Search for exception that extends RemoteException.
                if (((Class) j.next()).isAssignableFrom(RemoteException.class))
                    continue methods;
            }
            // rmi.46={0} is not a valid remote interface: method {1} must throw java.rmi.RemoteException
            throw new IllegalArgumentException(Messages.getString(
                    "rmi.46", iface.getName(), getBasicMethodSignature(method))); //$NON-NLS-1$
        }
        return true;
    }

    /**
     * Returns the list of implemented remote interfaces for the specified
     * class.
     *
     * @param   cls
     *          Class to return list of remote interfaces for.
     *
     * @return  Array of remote interfaces implemented by the specified class.
     *          May be empty if the specified class is not a remote class
     *          or if <code>cls</code> is <code>null</code>.
     *
     * @throws  IllegalArgumentException
     *          If class implements any invalid remote interfaces.
     */
    public static Class[] getRemoteInterfaces(Class cls)
            throws IllegalArgumentException {
        List interfaces = new LinkedList();

        for (; cls != null; cls = cls.getSuperclass()) {
            // Get the list of interfaces the class implements.
            Class[] interfacesArray = cls.getInterfaces();

            // Walk through all interfaces the class implements.
            for (int i = 0; i < interfacesArray.length; i++) {
                Class iface = interfacesArray[i];

                // Ignore duplicates and non-Remote interfaces.
                if (!interfaces.contains(iface)
                        && checkRemoteInterface(iface)) {
                    // Add this interface to the interfaces table.
                    interfaces.add(iface);
                }
            }
        }
        return (Class[]) interfaces.toArray(new Class[interfaces.size()]);
    }

    /**
     * Returns the string representation of the list of remote interfaces
     * implemented by the specified class.
     *
     * @param   cls
     *          Class to find remote interfaces for.
     *
     * @return  List of remote interfaces for the specified class.
     *
     * @throws  IllegalArgumentException
     *          If some error occurred while creating the list.
     */
    public static String[] getRemoteInterfacesNames(Class cls)
            throws IllegalArgumentException {
        Class[] interfaces = getRemoteInterfaces(cls);

        if ((interfaces == null) || (interfaces.length == 0)) {
            return new String[0];
        }
        String[] interfStr = new String[interfaces.length];

        for (int i = 0; i < interfaces.length; ++i) {
            interfStr[i] = interfaces[i].getName();
        }
        return interfStr;
    }

    /**
     * Returns a map containing all remote methods of the specified class
     * (i. e. all methods contained in {@link Remote} interfaces implemented
     * by the class). Hashes of methods are keys in this map.
     *
     * @param   cls
     *          Class to list remote methods for.
     *
     * @return  Map containing all the remote methods of the specified class
     *          and having method hashes as keys.
     *
     * @throws  RMIHashException
     *          If error occurred while calculating method hash.
     */
    public static Map getRemoteMethods(Class cls) throws RMIHashException {
        Map map = new HashMap();

        for (; cls != null; cls = cls.getSuperclass()) {
            Class[] interf = cls.getInterfaces();

            for (int i = 0; i < interf.length; ++i) {
                if (!Remote.class.isAssignableFrom(interf[i])) {
                    continue;
                }
                Method[] m = interf[i].getMethods();

                for (int j = 0; j < m.length; ++j) {
                    // Calculate the hash for the method.
                    long hash = RMIHash.getMethodHash(m[j]);
                    map.put(new Long(hash), m[j]);
                }
            }
        }
        return map;
    }

    /**
     * Finds the superclass of the specified class that directly implements
     * remote interface(s).
     *
     * @param   cls
     *          Class to check for remote superclass.
     *
     * @return  The class found.
     *
     * @throws  IllegalArgumentException
     *          If the specified class is not remote.
     */
    public static Class getRemoteClass(Class cls)
            throws IllegalArgumentException {
        for (; cls != null; cls = cls.getSuperclass()) {
            Class[] interfaces = cls.getInterfaces();

            for (int i = 0; i < interfaces.length; ++i) {
                if (Remote.class.isAssignableFrom(interfaces[i])) {
                    return cls;
                }
            }
        }
        // rmi.47=The specified class is not remote
        throw new IllegalArgumentException(Messages.getString("rmi.47")); //$NON-NLS-1$
    }

    /**
     * Returns <code>true</code> if the specified hostName is a local host
     * and <code>false</code> otherwise.
     *
     * @param   hostName
     *          The name of the host to check.
     *
     * @return  <code>true</code> if the specified hostName is a local host
     *          and <code>false</code> otherwise.
     *
     * @throws  UnknownHostException
     *          If the specified host name could not be resolved.
     */
    public static boolean isLocalHost(final String hostName)
            throws UnknownHostException {
        if (hostName == null) {
            return true;
        }

        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws UnknownHostException, IOException {
                    // Resolve the host name.
                    InetAddress hostAddr = InetAddress.getByName(hostName);

                    // Check if this address is really local.
                    ServerSocket ss = new ServerSocket(0, 1, hostAddr);

                    try {
                        ss.close();
                    } catch (IOException ioe) {
                        // Ignoring.
                    }
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception ex = pae.getException();

            if (ex instanceof UnknownHostException) {
                throw (UnknownHostException) ex;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <code>true</code> if the first specified class loader
     * is a parent class loader (or equal) to the second specified class loader
     * and <code>false</code> otherwise.
     *
     * @param   cl1
     *          First class loader.
     *
     * @param   cl2
     *          Second class loader.
     *
     * @return  <code>true</code> if the first class loader is a parent
     *          (or equal) to the second class loader.
     */
    public static boolean isParentLoader(ClassLoader cl1, ClassLoader cl2) {
        if (cl1 == null) {
            // cl1 is a bootstrap or system class loader.
            return true;
        }

        for (; cl2 != null; cl2 = cl2.getParent()) {
            if (cl1 == cl2) {
                return true;
            }
        }
        return false;
    }
}
