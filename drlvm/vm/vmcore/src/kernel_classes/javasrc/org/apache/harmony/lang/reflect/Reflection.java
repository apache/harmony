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
package org.apache.harmony.lang.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author Evgueni Brevnov, Roman S. Bushmanov
 */
public class Reflection {
    
    private  static ReflectAccessor reflectAccessor;
    
    public static void setReflectAccessor(ReflectAccessor accessor) {
        reflectAccessor = accessor;
    }
    
    public static <T> Constructor<T> copyConstructor(Constructor<T> c) {
        return reflectAccessor.copyConstructor(c);
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T>[] copyConstructors(Constructor<T>[] cs) {
        Constructor<T>[] ret = new Constructor[cs.length];
        for (int i = 0; i < cs.length; i++) {
            ret[i] = reflectAccessor.copyConstructor(cs[i]);
        }
        return ret;
    }
    
    public static Field copyField(Field f) {
        return reflectAccessor.copyField(f);
     }

    public static Field[] copyFields(Field[] fs) {
        Field[] ret = new Field[fs.length];
        for (int i = 0; i < fs.length; i++) {
            ret[i] = reflectAccessor.copyField(fs[i]);
        }
        return ret;
    }
    
    public static Method copyMethod(Method m) {
        return reflectAccessor.copyMethod(m);
    }
    
    public static Method[] copyMethods(Method[] ms) {
        Method[] ret = new Method[ms.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = reflectAccessor.copyMethod(ms[i]);
        }
        return ret;
    }
    
    public static void checkMemberAccess(Class callerClass,
                                         Class declarinClass,
                                         Class runtimeClass,
                                         int memberModifiers)
        throws IllegalAccessException {
        reflectAccessor.checkMemberAccess(callerClass, declarinClass,
                                          runtimeClass, memberModifiers);
    }
    
    public static Method[] mergePublicMethods(Method[] declared, 
            Method[] superPublic, Method[][] intf, int estimate) {
        return reflectAccessor.mergePublicMethods(declared, superPublic, intf, estimate);
    }
}
