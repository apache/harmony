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
/**
 * @author Evgueni Brevnov
 */

package java.lang.reflect;

import org.apache.harmony.lang.reflect.ReflectAccessor;

/**
 * @com.intel.drl.spec_ref 
 */
class ReflectExporter implements ReflectAccessor {

    public <T> Constructor<T> copyConstructor(Constructor<T> c) {
        return new Constructor<T>(c);
    }

    public Field copyField(Field f) {
        return new Field(f);
    }

    public Method copyMethod(Method m) {
        return new Method(m);
    }
    
    public Method[] mergePublicMethods(Method[] declared, 
            Method[] superPublic, Method[][] intf, int estimate) {
        Method[] store = new Method[estimate];
        int size = 0;
        for (Method m : declared) {
            if (Modifier.isPublic(m.getModifiers())) {
                store[size++] = m;
            }
        }
        if (superPublic != null) {
            nextSuper: for (Method m : superPublic) {
                for (int i = 0; i < size; i++) {
                    if (m.getName() == store[i].getName() 
                            && m.getSignature() == store[i].getSignature()) {
                        continue nextSuper;
                    }
                }
                store[size++] = m;
            }
        }
        if (intf != null) {
            for (Method[] mi : intf) {
                nextIntf: for (Method m : mi) {
                    for (int i = 0; i < size; i++) {
                        if (m.getName() == store[i].getName() 
                                && m.getSignature() == store[i].getSignature()) {
                            continue nextIntf;
                        }
                    }
                    store[size++] = m;                
                }
            }
        }
        Method[] real = new Method[size];
        System.arraycopy(store, 0, real, 0, size);

        return real;
    }


    public void checkMemberAccess(Class callerClass, Class declaringClass,
                                  Class runtimeClass, int memberModifiers)
        throws IllegalAccessException {
        if (!allowAccess(callerClass, declaringClass, runtimeClass, memberModifiers)) {
            throw new IllegalAccessException("A member of the \""
                + declaringClass + "\" with \""
                + Modifier.toString(memberModifiers)
                + "\" modifiers can not be accessed from the \"" + callerClass
                + "\"");
        }
    }

    /*
     * NON EXPORTED 
     */
    
    private boolean allowAccess(Class<?> callerClass, Class<?> declaringClass,
                                Class<?> runtimeClass, int memberModifiers) {
        // it is allways safe to access members from the class declared in the
        // same top level class as the declaring class        
        if (hasSameTopLevelClass(declaringClass, callerClass)) {
            return true;
        }
        // no way to access private methods at this point
        if (Modifier.isPrivate(memberModifiers)) {
            return false;
        }
        // check access to public members
        if (Modifier.isPublic(memberModifiers)) {
            // fast check
            if (allowClassAccess(declaringClass, callerClass)) {
                return true;
            }
            // full inspection of the hierarchy 
            if (runtimeClass != declaringClass) {
                do {
                    if (allowClassAccess(runtimeClass, callerClass) ||
                        hasSameTopLevelClass(runtimeClass, callerClass)) {
                        return true;
                    }            
                } while ((runtimeClass = runtimeClass.getSuperclass()) != declaringClass);
            }
            return  false;
        }

        // this check should cover package private access
        if (hasSamePackage(declaringClass, callerClass) &&
            allowClassAccess(declaringClass, callerClass)) {
            return true;
        }

        // check access to protected members through hierarchy
        if (Modifier.isProtected(memberModifiers)) {            
            Class<?> outerClass = callerClass;
            // scan from the caller to the top level class
            do {
                // find closest enclouser class which extends declaring class
                while (!declaringClass.isAssignableFrom(outerClass)) {
                    outerClass = outerClass.getDeclaringClass();
                    if (outerClass == null) {
                        return false;
                    }
                }
                // provide access if and only if outer is subclass of runtime class
                if (outerClass.isAssignableFrom(runtimeClass)) {
                    return true;
                }
                outerClass = outerClass.getDeclaringClass();
            } while (outerClass != null);
        }
        return false;
    }

    private boolean allowClassAccess(Class<?> callee, Class<?> caller) {
        if (callee == null || callee == caller) {
            return true;
        }
        int modifiers = callee.getModifiers();
	    Class calleeDeclaringClass = callee.getDeclaringClass();
	    if (calleeDeclaringClass == null) {
	        // the callee is either a top level class or a local class
            if (Modifier.isPublic(modifiers) || hasSamePackage(callee, caller)) {
	            return true;
	        }
	        // other top level classes and local classes 
            return false;
	    }
	    // here callee is a member class
   	    return allowAccess(caller, calleeDeclaringClass, calleeDeclaringClass, modifiers);
    }

    private boolean hasSameTopLevelClass(Class<?> class1, Class<?> class2) {
        Class topClass;
        while ( (topClass = class1.getEnclosingClass()) != null) {
            class1 = topClass;
        }
        while ( (topClass = class2.getEnclosingClass()) != null) {
            class2 = topClass;
        }
        return class1 == class2;
    }

    private boolean hasSamePackage(Class<?> class1, Class<?> class2) {
        if (class1.getClassLoader() != class2.getClassLoader()) {
            return false;
        }
        final String pkg1 = class1.getName();
        final String pkg2 = class2.getName();
        int i1 = pkg1.lastIndexOf('.');
        int i2 = pkg2.lastIndexOf('.');
        // in the case of default packages i1 == i2 == -1 
        return i1 == i2 ? pkg1.regionMatches(0, pkg2, 0, i1) : false;
    }
}
