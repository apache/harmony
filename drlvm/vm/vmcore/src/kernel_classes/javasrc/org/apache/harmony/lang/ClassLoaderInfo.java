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
package org.apache.harmony.lang;

import java.util.ArrayList;

/**
 * @author Evgueni Brevnov, Roman S. Bushmanov
 */
public class ClassLoaderInfo {

    private static final ClassLoader[] systemLoaders;
    
    static {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        ClassLoader parentClassLoader = systemClassLoader.getParent();
        if (parentClassLoader == null) {
            systemLoaders = new ClassLoader[] { systemClassLoader };
        } else {
            ArrayList<ClassLoader> loaders = new ArrayList<ClassLoader>();
            loaders.add(systemClassLoader);
            do {
                loaders.add(parentClassLoader);
                parentClassLoader = parentClassLoader.getParent();
            } while (parentClassLoader != null);
            systemLoaders = (ClassLoader[])loaders
                .toArray(new ClassLoader[loaders.size()]);
        }
    }
    
    /**
     * Answers if the specified class loader is the system class loader or on of
     * its ancestors.
     * 
     * @param classLoader class loader to test
     * @return true if the specified class loader is the system class loader or
     *         on of its ancestors, false otherwise. This method returns true if
     *         null is passed.
     */
    public static boolean isSystemClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            return true;
        }
        for (int i = 0; i < systemLoaders.length; i++) {
            if (classLoader == systemLoaders[i]) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Answers if the specified class was defined by the system class loader or
     * on of its ancestors. The behaviour is undefined if clazz is equal to null.
     * 
     * @param clazz class which class loader should be tested
     * @return true if the specified class was defined by the system class
     *         loader or on of its ancestors, false otherwise. This method
     *         returns true if defining class loader is null.
     */
    public static boolean hasSystemClassLoader(Class<?> clazz) {
        for (int i = 0; i < systemLoaders.length; i++) {
            ClassLoader loader = clazz.getClassLoader();
            if (loader == null || loader == systemLoaders[i]) {
                return true;
            }
        }
        return false;
        
    }
}
