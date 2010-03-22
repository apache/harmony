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
 * @author Pavel N. Vyssotski
 */
// ClassManager.cpp

#include <string.h>

#include "jni.h"

#include "ClassManager.h"
#include "Log.h"

using namespace jdwp;

ClassManager::ClassManager() throw()
{
    m_classClass = 0;
    m_threadClass = 0;
    m_threadGroupClass = 0;
    m_stringClass = 0;
    m_classLoaderClass = 0;
    m_OOMEClass = 0;
    m_systemClass = 0;
}

void ClassManager::Init(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Init(" << jni << ')');
    
    m_stringClass = jni->FindClass("java/lang/String");
    if (m_stringClass == 0) {
        JDWP_INFO("Class not found: java.lang.String");
        throw InternalErrorException();
    }
    m_stringClass = static_cast<jclass>(jni->NewGlobalRef(m_stringClass));

    m_classClass = jni->FindClass("java/lang/Class");
    if (m_classClass == 0) {
        JDWP_INFO("Class not found: java.lang.Class");
        throw InternalErrorException();
    }
    m_classClass = static_cast<jclass>(jni->NewGlobalRef(m_classClass));

    m_threadClass = jni->FindClass("java/lang/Thread");
    if (m_threadClass == 0) {
        JDWP_INFO("Class not found: java.lang.Thread");
        throw InternalErrorException();
    }
    m_threadClass = static_cast<jclass>(jni->NewGlobalRef(m_threadClass));

    m_threadGroupClass = jni->FindClass("java/lang/ThreadGroup");
    if (m_threadGroupClass == 0) {
        JDWP_INFO("Class not found: java.lang.ThreadGroup");
        throw InternalErrorException();
    }
    m_threadGroupClass =
        static_cast<jclass>(jni->NewGlobalRef(m_threadGroupClass));

    m_classLoaderClass = jni->FindClass("java/lang/ClassLoader");
    if (m_classLoaderClass== 0) {
        JDWP_INFO("Class not found: java.lang.ClassLoader");
        throw InternalErrorException();
    }
    m_classLoaderClass =
        static_cast<jclass>(jni->NewGlobalRef(m_classLoaderClass));

    m_OOMEClass = jni->FindClass("java/lang/OutOfMemoryError");
    if (m_OOMEClass == 0) {
        JDWP_INFO("Class not found: java.lang.OutOfMemoryError");
        throw InternalErrorException();
    }
    m_OOMEClass = static_cast<jclass>(jni->NewGlobalRef(m_OOMEClass));

    m_systemClass = jni->FindClass("java/lang/System");
    if (m_systemClass == 0) {
        JDWP_INFO("Class not found: java.lang.System");
        throw InternalErrorException();
    }
    m_systemClass = static_cast<jclass>(jni->NewGlobalRef(m_systemClass));
}

void ClassManager::Clean(JNIEnv *jni) throw()
{
    JDWP_TRACE_ENTRY("Clean(" << jni << ')');

    if (m_classClass != 0)
        jni->DeleteGlobalRef(m_classClass);
    if (m_threadClass != 0)
        jni->DeleteGlobalRef(m_threadClass);
    if (m_threadGroupClass != 0)
        jni->DeleteGlobalRef(m_threadGroupClass);
    if (m_stringClass != 0)
        jni->DeleteGlobalRef(m_stringClass);
    if (m_classLoaderClass != 0)
        jni->DeleteGlobalRef(m_classLoaderClass);
    if (m_OOMEClass != 0)
        jni->DeleteGlobalRef(m_OOMEClass);
    if (m_systemClass != 0)
        jni->DeleteGlobalRef(m_systemClass);
}

void ClassManager::CheckOnException(JNIEnv *jni) const throw(AgentException)
{
    jthrowable exception = jni->ExceptionOccurred();
    if (exception != 0) {
        jni->ExceptionClear();
        if (jni->IsInstanceOf(exception, m_OOMEClass) == JNI_TRUE) {
            throw OutOfMemoryException();
        } else {
            throw InternalErrorException();
        }
    }
}

// returnValue must be freed via GetMemoryManager().Free()
// returnValue = 0, if there is no property with that name
char* ClassManager::GetProperty(JNIEnv *jni, const char *str) const
    throw(AgentException)
{
    jmethodID mid = jni->GetStaticMethodID(m_systemClass,
        "getProperty", "(Ljava/lang/String;)Ljava/lang/String;");
    if (mid == 0) {
        JDWP_INFO("Method not found: java.lang.System.getProperty(String)");
        throw InternalErrorException();
    }

    jstring key = jni->NewStringUTF(str);
    CheckOnException(jni);

    jstring value = static_cast<jstring>
        (jni->CallStaticObjectMethod(m_systemClass, mid, key));
    CheckOnException(jni);

    char *returnValue = 0;
    if (value != 0) {
        jsize len = jni->GetStringUTFLength(value);
        returnValue = reinterpret_cast<char*>
            (AgentBase::GetMemoryManager().Allocate(len + 1 JDWP_FILE_LINE));
        jni->GetStringUTFRegion(value, 0, jni->GetStringLength(value),
            returnValue);
    }

    return returnValue;
}

// returnValue must be freed via GetMemoryManager().Free()
char* ClassManager::GetClassName(const char *signature) const throw(AgentException)
{
    if (signature == 0)
        return 0;

    const size_t len = strlen(signature);
    char *returnValue = reinterpret_cast<char*>
        (AgentBase::GetMemoryManager().Allocate(len + 1 JDWP_FILE_LINE));

    bool arrayFlag = (signature[0] == '[');
    size_t j = 0;
    for (size_t i = 0; i < len; i++) {
        char c = signature[i];
        if (c == '/') {
             returnValue[j++] = '.';
        } else if (c == 'L') {
             if (arrayFlag) {
                 returnValue[j++] = c;
             }
        } else if (c == ';') {
             if (arrayFlag) {
                 returnValue[j++] = c;
             }
             break;
        } else {
             returnValue[j++] = c;
        }
    }
    returnValue[j] = '\0';

    return returnValue;
}

jclass ClassManager::GetClassForName(JNIEnv *jni,
    const char *name, jobject loader) const throw(AgentException)
{
    JDWP_TRACE_ENTRY("GetClassForName(" << jni << ',' << name << ',' << loader << ')');

    jmethodID mid = jni->GetStaticMethodID(m_classClass, "forName",
        "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");
    CheckOnException(jni);
    if (mid == 0) {
        JDWP_INFO("Method not found: java.lang.Class.forName(String,boolean,ClassLoader)");
        throw InternalErrorException();
    }

    jstring clsName = jni->NewStringUTF(name);
    CheckOnException(jni);

    jclass cls = static_cast<jclass>
        (jni->CallStaticObjectMethod(m_classClass, mid, clsName, JNI_TRUE, loader));
    CheckOnException(jni);

    return cls;
}

jboolean ClassManager::IsArray(JNIEnv *jni, jobject object) const
    throw(AgentException)
{
    jboolean isArray;
    jclass cls = jni->GetObjectClass(object);
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->IsArrayClass(cls, &isArray));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
    return isArray;
}

jdwpTag ClassManager::GetJdwpTag(JNIEnv *jni, jobject object) const
    throw(AgentException)
{
    if (object == 0) {
        return JDWP_TAG_OBJECT;
    } else if (IsString(jni, object) == JNI_TRUE) {
        return JDWP_TAG_STRING;
    } else if (IsThread(jni, object) == JNI_TRUE) {
        return JDWP_TAG_THREAD;
    } else if (IsThreadGroup(jni, object) == JNI_TRUE) {
        return JDWP_TAG_THREAD_GROUP;
    } else if (IsClassLoader(jni, object) == JNI_TRUE) {
        return JDWP_TAG_CLASS_LOADER;
    } else if (IsClass(jni, object) == JNI_TRUE) {
        return JDWP_TAG_CLASS_OBJECT;
    } else if (IsArray(jni, object) == JNI_TRUE) {
        return JDWP_TAG_ARRAY;
    } else {
        return JDWP_TAG_OBJECT;
    }
}

jdwpTag ClassManager::GetJdwpTagFromSignature(const char* signature) const
throw ()
{
    switch ( signature[0] ) {
        case 'Z': return JDWP_TAG_BOOLEAN;
        case 'B': return JDWP_TAG_BYTE;
        case 'C': return JDWP_TAG_CHAR;
        case 'S': return JDWP_TAG_SHORT;
        case 'I': return JDWP_TAG_INT;
        case 'J': return JDWP_TAG_LONG;
        case 'F': return JDWP_TAG_FLOAT;
        case 'D': return JDWP_TAG_DOUBLE;
        case 'L': return JDWP_TAG_OBJECT;
        case '[': return JDWP_TAG_ARRAY;
    }
    return JDWP_TAG_NONE;
} // GetJdwpTagFromSignature() 

jboolean ClassManager::IsArrayType(jclass klass) const
    throw(AgentException)
{
    jboolean flag;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->IsArrayClass(klass, &flag));

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    return flag;
}

jboolean ClassManager::IsInterfaceType(jclass klass) const
    throw(AgentException)
{
    jboolean flag;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->IsInterface(klass, &flag));

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    return flag;
}

jdwpTypeTag ClassManager::GetJdwpTypeTag(jclass klass) const
    throw (AgentException)
{
    if ( IsInterfaceType(klass) == JNI_TRUE )
        return JDWP_TYPE_TAG_INTERFACE;

    else if ( IsArrayType(klass) )
        return JDWP_TYPE_TAG_ARRAY;

    return JDWP_TYPE_TAG_CLASS;
}

jboolean ClassManager::IsObjectValueFitsFieldType
    (JNIEnv *jni, jobject objectValue, const char* fieldSignature) const 
    throw(AgentException)
{
    if ( objectValue == 0 ) {
        return true;
    }

    jint classCount = 0;
    jclass* classes = 0;

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    jvmtiError err;
    JVMTI_TRACE(err, jvmti->GetLoadedClasses(&classCount, &classes));
    JvmtiAutoFree classesAutoFree(classes);

    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }

    int i;
    jclass fieldTypeClass = 0;
    for (i = 0; i < classCount; i++) {
        char* classSignature = 0;

        JVMTI_TRACE(err, jvmti->GetClassSignature(classes[i], &classSignature, 0));
        JvmtiAutoFree classSignatureAutoFree(classSignature);

        if (err != JVMTI_ERROR_NONE) {
            throw AgentException(err);
        }

        if ( strcmp(fieldSignature, classSignature) == 0 ) {
            fieldTypeClass = classes[i];
            break;
        }
    }
    if ( fieldTypeClass == 0 ) {
        throw AgentException(JDWP_ERROR_INVALID_FIELDID);
    }
    return  jni->IsInstanceOf(objectValue, fieldTypeClass);
}
