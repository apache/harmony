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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @file
 * ClassManager.h
 *
 * Provides access to certain standard Java classes.
 */

#ifndef _CLASS_MANAGER_H_
#define _CLASS_MANAGER_H_

#include "AgentBase.h"
#include "ExceptionManager.h"

namespace jdwp {

    /**
     * The class provides access to certain standard Java classes.
     */
    class ClassManager : public AgentBase {

    public:

        /**
         * A constructor.
         */
        ClassManager();

        /**
         * A destructor.
         */
        ~ClassManager() {}

        /**
         * Initializes the class manager.
         * Creates instances of the standard java classes: 
         * <code>java.lang.String</code>, <code>java.lang.Class</code>,
         * <code>java.lang.Thread</code>, <code>java.lang.ThreadGroup</code>,
         * <code>java.lang.ClassLoader</code>, <code>java.lang.OutOfMemoryError</code> 
         * and <code>java.lang.System</code>.
         *
         * @param jni - the JNI interface pointer
         */
        int Init(JNIEnv *jni);

        /**
         * Cleanups the class manager.
         *
         * @param jni - the JNI interface pointer
         */
        void Clean(JNIEnv *jni);

        /**
         * Resets the class manager.
         *
         * @param jni - the JNI interface pointer
         */
        void Reset(JNIEnv *jni) { }

        /**
         * Gets an instance of the <code>java.lang.Class</code> class.
         *
         * @return Returns  the <code>java.lang.Class</code> jclass.
         */
        jclass GetClassClass() const {
            return m_classClass;
        }

        /**
         * Gets an instance of the <code>java.lang.Thread</code> class.
         *
         * @return Returns the <code>java.lang.Thread</code> jclass.
         */
        jclass GetThreadClass() const {
            return m_threadClass;
        }

        /**
         * Gets an instance of the <code>java.lang.ThreadGroup</code> class.
         *
         * @return Returns the <code>java.lang.ThreadGroup</code> jclass.
         */
        jclass GetThreadGroupClass() const {
            return m_threadGroupClass;
        }

        /**
         * Gets an instance of the <code>java.lang.String</code> class.
         *
         * @return Returns the <code>java.lang.String</code> jclass.
         */
        jclass GetStringClass() const {
            return m_stringClass;
        }

        /**
         * Gets an instance of  the <code>java.lang.ClassLoader</code> class.
         * 
         * @return Returns the <code>java.lang.ClassLoader</code> jclass.
         */
        jclass GetClassLoaderClass() const {
            return m_classLoaderClass;
        }

        /**
         * Gets an instance of the <code>java.lang.OutOfMemoryError</code> class.
         *
         * @return Returns the <code>java.lang.OutOfMemoryError</code> jclass.
         */
        jclass GetOOMEClass() const {
            return m_OOMEClass;
        }

        /**
         * Gets an instance of the <code>java.lang.System</code> class.
         *
         * @return Returns the <code>java.lang.System</code> jclass.
         */
        jclass GetSystemClass() const {
            return m_systemClass;
        }

        /**
         * Checks whether an exception has occurred in the target VM
         * and whether VM has translated it to <code>AgentException</code>.
         *
         * @param jni - the JNI interface pointer
         */
        int CheckOnException(JNIEnv *jni) const;

        /**
         * Returns system property from the Java class <code>java.lang.System</code> 
         * using the <code>getProperty</code> method.
         *
         * @param jni - the JNI interface pointer
         * @param str - the name of the property.
         *
         * Free the returned value via <code>GetMemoryManager().Free().</code>
         * If no property exists with that name, the returned value is 0.
         *
         * @return Returns - the Java system property.
         */
        char* GetProperty(JNIEnv *jni, const char *str) const;

        /**
         * Returns the class name corresponding to the given signature.
         *
         * @param signature - the class signature.
         *
         * Free the returned value via <code>GetMemoryManager().Free().</code>
         *
         * @return Returns the class name corresponding to the given signature.
         */
        char* GetClassName(const char *signature) const;

        /**
         * Gets the Java class corresponding to the given name.
         *
         * @param jni    - the JNI interface pointer
         * @param name   - the class name
         * @param loader - the class loader
         *
         * @return Returns the Java class for the given name.
         */
        jclass GetClassForName(JNIEnv *jni, const char *name, jobject loader) const;

        /**
         * Checks whether the given object is the instance of the 
         * <code>java.lang.Class</code> class.
         *
         * @param jni    - the JNI interface pointer
         * @param object - the Java object
         *
         * @return <code>TRUE</code> if the object is the instance of 
         *         <code>java.lang.Class</code>, otherwise <code>FALSE</code>.
         */
        jboolean IsClass(JNIEnv *jni, jobject object) const {
            return jni->IsInstanceOf(object, m_classClass);
        }

        /**
         * Checks whether the given object is the instance of the 
         * <code>java.lang.Thread</code> class.
         *
         * @param jni    - the JNI interface pointer
         * @param object - the Java object
         *
         * @return <code>TRUE</code> if the object is the instance of 
         *         <code>java.lang.Thread</code>, otherwise <code>FALSE</code>.
         */
        jboolean IsThread(JNIEnv *jni, jobject object) const {
            return jni->IsInstanceOf(object, m_threadClass);
        }

        /**
         * Checks whether the given object is the instance of the 
         * <code>java.lang.ThreadGroup</code> class.
         *
         * @param jni    - the JNI interface pointer
         * @param object - the Java object
         *
         * @return <code>TRUE</code> if the object is the instance of 
         *         <code>java.lang.ThreadGroup</code>, otherwise <code>FALSE</code>.
         */
        jboolean IsThreadGroup(JNIEnv *jni, jobject object) const {
            return jni->IsInstanceOf(object, m_threadGroupClass);
        }

        /**
         * Checks whether the given object is the instance of the 
         * <code>java.lang.String</code> class.
         *
         * @param jni    - the JNI interface pointer
         * @param object - the Java object
         *
         * @return <code>TRUE</code> if the object is the instance of 
         *         <code>java.lang.String</code>, otherwise <code>FALSE</code>.
         */
        jboolean IsString(JNIEnv *jni, jobject object) const {
            return jni->IsInstanceOf(object, m_stringClass);
        }

        /**
         * Checks whether the given object is the instance of the 
         * <code>java.lang.ClassLoader</code> class.
         *
         * @param jni    - the JNI interface pointer
         * @param object - the Java object
         *
         * @return <code>TRUE<code> if the object is the instance of 
         *         <code>java.lang.ClassLoader</code>, otherwise <code>FALSE</code>.
         */
        jboolean IsClassLoader(JNIEnv *jni, jobject object) const {
            return jni->IsInstanceOf(object, m_classLoaderClass);
        }

        /**
         * Checks whether the given object is the Java array.
         *
         * @param jni    - the JNI interface pointer
         * @param object - the Java object
         *
         * @return <code>TRUE</code> if the object is the array, 
         *         otherwise <code>FALSE</code>.
         */
        jboolean IsArray(JNIEnv *jni, jobject object) const
           ;

        /**
         * Gets the JDWP tag of the given Java object.
         *
         * @param jni    - the JNI interface pointer
         * @param object - the Java object
         *
         * @return Returns the JDWP tag of the Java object.
         */
        jdwpTag GetJdwpTag(JNIEnv *jni, jobject object) const
           ;

        /** 
         * Gets jdwpTag indicated by the specified signature.
         *
         * @param signature - the JNI-style signature for a certain component
         *                    encoding the type information according to the 
         *                    JNI documentation
         *
         * @return Returns the JDWP tag corresponding to the passed signature,
         *         such as JDWP_TAG_BYTE, JDWP_TAG_OBJECT and so on, or 
         *         JDWP_TAG_NONE, if the passed signature is incorrect.
         */
        jdwpTag GetJdwpTagFromSignature(const char* signature) const;

        /**
         * Checks whether the given class is the Java array.
         *
         * @param klass - the Java class
         *
         * @return <code>TRUE</code> if the given class is the array, 
         *         otherwise <code>FALSE</code>.
         */
        jboolean IsArrayType(jclass klass) const;

        /**
         * Checks whether the given class is the Java interface.
         *
         * @param klass - the Java class
         *
         * @return <code>TRUE</code> if the given class is the interface, 
         *         otherwise <code>FALSE</code>.
         */
        jboolean IsInterfaceType(jclass klass) const;

        /**
         * Gets the JDWP type tag of the given Java class.
         *
         * @param klass - the Java class
         *
         * @return Returns the JDWP type tag of the Java class.
         */
        jdwpTypeTag GetJdwpTypeTag(jclass klass) const;

        /**
         * Checks the given object value.
         *
         * @param jni            - the JNI interface pointer
         * @param objectValue    - the Java object
         * @param fieldSignature - the signature of the field type
         *
         * @return <code>TRUE</code> if the given value fits the field type.
         */
        jboolean IsObjectValueFitsFieldType(JNIEnv *jni, jobject objectValue, const char* fieldSignature)
            const;

    private:

        jclass m_classClass;
        jclass m_threadClass;
        jclass m_threadGroupClass;
        jclass m_stringClass;
        jclass m_classLoaderClass;
        jclass m_OOMEClass;
        jclass m_systemClass;

    };

}

#endif // _CLASS_MANAGER_H_
