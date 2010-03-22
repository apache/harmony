/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.harmony.lang.reflect.support;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Serguei S. Zapreyev
 */

/**
 * Loader provides access to some of finding.
 * 
 * (This should be considered as a temporary decision. A correct approach
 * in using loader facilities should be implemented later.)
 */
public final class AuxiliaryLoader {

    public static Class<?> findClass(final String classTypeName,
            Object startPoint) throws ClassNotFoundException {
        if (classTypeName.equals("byte")) {
            return byte.class;
        } else if (classTypeName.equals("char")) {
            return char.class;
        } else if (classTypeName.equals("double")) {
            return double.class;
        } else if (classTypeName.equals("float")) {
            return float.class;
        } else if (classTypeName.equals("int")) {
            return int.class;
        } else if (classTypeName.equals("long")) {
            return long.class;
        } else if (classTypeName.equals("short")) {
            return short.class;
        } else if (classTypeName.equals("boolean")) {
            return boolean.class;
        } else if (classTypeName.equals("void")) {
            return void.class;
        }
        final ClassLoader loader = getClassLoader(startPoint);
        Class c = (Class) AccessController.doPrivileged(
                new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    Method loadClassMethod = findLoadClassMethod(
                            loader.getClass());
                    loadClassMethod.setAccessible(true);
                    return (Object) loadClassMethod.invoke((Object) loader,
                            new Object[] {
                            (Object) AuxiliaryFinder.transform(classTypeName),
                            new Boolean(false) });
                } catch (IllegalAccessException e) {
                    System.err.println("Error: AuxiliaryLoader.findClass(" +
                            classTypeName + "): " + e.toString());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    System.err.println("Error: AuxiliaryLoader.findClass(" +
                            classTypeName + "): " + e.getTargetException());
                    e.getTargetException().printStackTrace();
                } catch (Exception e) {
                    System.err.println("Error: AuxiliaryLoader.findClass(" +
                            classTypeName + "): " + e.toString());
                    e.printStackTrace();
                }
                return null;
            }
        });

        if (c == null) {
            throw new ClassNotFoundException(classTypeName);
        }
        return c;
    }

    /**
     * @param startPoint an instance of the Class, Method, Constructor or Field
     * type to start the search of a type variable declaration place.
     */
    private static ClassLoader getClassLoader(Object startPoint) {
        ClassLoader res = null;

        if (startPoint instanceof Class) {
            res = ((Class) startPoint).getClassLoader();
        } else if (startPoint instanceof Member) {
            res = ((Member) startPoint).getDeclaringClass().getClassLoader();
        } else {
            res = startPoint.getClass().getClassLoader();
        }

        if (res == null) {
            res = ClassLoader.getSystemClassLoader();
        }
        return res;
    }

    /**
     * Looks for loadClass(String name, boolean resolve) Method in the
     * specified 'loader' class (of the type ClassLoader) or its super class.
     */
    private static Method findLoadClassMethod(Class loaderClass) {
        Method res = null;
        Method[] ms = loaderClass.getDeclaredMethods();

        for (int i = 0; i < ms.length; i++) {
            if (!ms[i].getName().equals("loadClass")) {
                continue;
            }

            if (ms[i].getParameterTypes().length != 2) {
                continue;
            }

            if (!ms[i].getParameterTypes()[0].getName().equals(
                        "java.lang.String")) {
                continue;
            }

            if (!ms[i].getParameterTypes()[1].getName().equals(
                        "boolean")) {
                continue;
            }
            res = ms[i];
            break;
        }

        // no null check is required - at leas this methopd will be found in 
        // java.lang.ClassLoader
        return res != null ? res :
                findLoadClassMethod(loaderClass.getSuperclass());
    }

    public static void resolve(final Class c) {
        AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
            public Object run() {
                ClassLoader loader = getClassLoader(c);

                try {
                    Method loadClassMethod = findLoadClassMethod(
                            loader.getClass());
                    loadClassMethod.setAccessible(true);
                    loadClassMethod.invoke((Object) loader, new Object[] {
                            (Object) c.getCanonicalName(), (Object) true });
                } catch (java.lang.IllegalAccessException _) {
                } catch (java.lang.reflect.InvocationTargetException _) {
                }
                return null;
            }
        });
    }
}
