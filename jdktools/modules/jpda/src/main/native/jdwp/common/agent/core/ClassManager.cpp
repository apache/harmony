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
#include "jni.h"

#include "ClassManager.h"
#include "Log.h"

#include <string.h>

using namespace jdwp;

ClassManager::ClassManager()
{
    m_classClass = 0;
    m_threadClass = 0;
    m_threadGroupClass = 0;
    m_stringClass = 0;
    m_classLoaderClass = 0;
    m_OOMEClass = 0;
    m_systemClass = 0;
}

int ClassManager::Init(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Init(%p)", jni));
    
    m_stringClass = jni->FindClass("java/lang/String");
    if (m_stringClass == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Class not found: java.lang.String"));
        AgentException ex(JDWP_ERROR_INTERNAL);
	    JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INTERNAL;
    }
    m_stringClass = static_cast<jclass>(jni->NewGlobalRef(m_stringClass));

    m_classClass = jni->FindClass("java/lang/Class");
    if (m_classClass == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Class not found: java.lang.Class"));
        AgentException ex(JDWP_ERROR_INTERNAL);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INTERNAL;
    }
    m_classClass = static_cast<jclass>(jni->NewGlobalRef(m_classClass));

    m_threadClass = jni->FindClass("java/lang/Thread");
    if (m_threadClass == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Class not found: java.lang.Thread"));
    	AgentException ex(JDWP_ERROR_INTERNAL);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INTERNAL;
    }
    m_threadClass = static_cast<jclass>(jni->NewGlobalRef(m_threadClass));

    m_threadGroupClass = jni->FindClass("java/lang/ThreadGroup");
    if (m_threadGroupClass == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Class not found: java.lang.ThreadGroup"));
        AgentException ex(JDWP_ERROR_INTERNAL);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INTERNAL;
    }
    m_threadGroupClass =
        static_cast<jclass>(jni->NewGlobalRef(m_threadGroupClass));

    m_classLoaderClass = jni->FindClass("java/lang/ClassLoader");
    if (m_classLoaderClass== 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Class not found: java.lang.ClassLoader"));
        AgentException ex(JDWP_ERROR_INTERNAL);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INTERNAL;
    }
    m_classLoaderClass =
        static_cast<jclass>(jni->NewGlobalRef(m_classLoaderClass));

    m_OOMEClass = jni->FindClass("java/lang/OutOfMemoryError");
    if (m_OOMEClass == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Class not found: java.lang.OutOfMemoryError"));
        AgentException ex(JDWP_ERROR_INTERNAL);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INTERNAL;
    }
    m_OOMEClass = static_cast<jclass>(jni->NewGlobalRef(m_OOMEClass));

    m_systemClass = jni->FindClass("java/lang/System");
    if (m_systemClass == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Class not found: java.lang.System"));
        AgentException ex(JDWP_ERROR_INTERNAL);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INTERNAL;
    }
    m_systemClass = static_cast<jclass>(jni->NewGlobalRef(m_systemClass));

    return JDWP_ERROR_NONE;
}

void ClassManager::Clean(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Clean(%p)", jni));

    /* FIXME - Workaround for shutdown crashes
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
        jni->DeleteGlobalRef(m_systemClass);*/
}

int ClassManager::CheckOnException(JNIEnv *jni) const
{
    if (jni->ExceptionOccurred()) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "An exception occurred:"));
        jni->ExceptionDescribe();
        jni->ExceptionClear();
        return JDWP_ERROR_INTERNAL;
    }
    return JDWP_ERROR_NONE;
}

// returnValue must be freed via GetMemoryManager().Free()
// returnValue = 0, if there is no property with that name
char* ClassManager::GetProperty(JNIEnv *jni, const char *str) const
{
    jmethodID mid = jni->GetStaticMethodID(m_systemClass,
        "getProperty", "(Ljava/lang/String;)Ljava/lang/String;");
    if (mid == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Method not found: java.lang.System.getProperty(String)"));
        return 0;
    }

    jstring key = jni->NewStringUTF(str);
    int ret = CheckOnException(jni);
    if (ret != JDWP_ERROR_NONE) return 0;

    jstring value = static_cast<jstring>
        (jni->CallStaticObjectMethod(m_systemClass, mid, key));
    ret = CheckOnException(jni);
    if (ret != JDWP_ERROR_NONE) return 0;

    char *returnValue = 0;
    if (value != 0) {
        jsize len = jni->GetStringUTFLength(value);
        returnValue = reinterpret_cast<char*>
            (AgentBase::GetMemoryManager().Allocate(len + 1 JDWP_FILE_LINE));
        jni->GetStringUTFRegion(value, 0, jni->GetStringLength(value),
            returnValue);
        returnValue[len] = '\0';
    }

    return returnValue;
}

// returnValue must be freed via GetMemoryManager().Free()
char* ClassManager::GetClassName(const char *signature) const
{
    if (signature == 0)
        return 0;

    const size_t len = strlen(signature);
    char *returnValue = reinterpret_cast<char*>
        (AgentBase::GetMemoryManager().Allocate(len + 1 JDWP_FILE_LINE));

    if (0 == returnValue) {
        return 0;
    }

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
    const char *name, jobject loader) const
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "GetClassForName(%p,%s,%p)", jni, name, loader));

    jmethodID mid = jni->GetStaticMethodID(m_classClass, "forName",
        "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");
    int ret = CheckOnException(jni);
    if (ret != JDWP_ERROR_NONE) return 0;
    if (mid == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Method not found: java.lang.Class.forName(String,boolean,ClassLoader)"));
        return 0;
    }

    jstring clsName = jni->NewStringUTF(name);
    ret = CheckOnException(jni);
    if (ret != JDWP_ERROR_NONE) return 0;

    jclass cls = static_cast<jclass>
        (jni->CallStaticObjectMethod(m_classClass, mid, clsName, JNI_TRUE, loader));
    ret = CheckOnException(jni);
    if (ret != JDWP_ERROR_NONE) return 0;

    return cls;
}

jboolean ClassManager::IsArray(JNIEnv *jni, jobject object) const
   
{
    jboolean isArray;
    jclass cls = jni->GetObjectClass(object);
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->IsArrayClass(cls, &isArray));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error %d returned calling IsArrayClass()", err));
        return JNI_FALSE;
    }
    return isArray;
}

jdwpTag ClassManager::GetJdwpTag(JNIEnv *jni, jobject object) const
   
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
{
    jboolean flag;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->IsArrayClass(klass, &flag));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error %d returned calling IsArrayClass()", err));
        return JNI_FALSE;
    }

    return flag;
}

jboolean ClassManager::IsInterfaceType(jclass klass) const
{
    jboolean flag;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->IsInterface(klass, &flag));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error %d returned calling IsInterface()", err));
        return JNI_FALSE;
    }

    return flag;
}

jdwpTypeTag ClassManager::GetJdwpTypeTag(jclass klass) const
{
    if ( IsInterfaceType(klass) == JNI_TRUE )
        return JDWP_TYPE_TAG_INTERFACE;

    else if ( IsArrayType(klass) )
        return JDWP_TYPE_TAG_ARRAY;

    return JDWP_TYPE_TAG_CLASS;
}

jboolean ClassManager::IsObjectValueFitsFieldType
    (JNIEnv *jni, jobject objectValue, const char* fieldSignature) const 
{
    if ( objectValue == 0 ) {
        return JNI_TRUE;
    }

    jint classCount = 0;
    jclass* classes = 0;

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    AgentBase::GetJniEnv()->PushLocalFrame(100);

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetLoadedClasses(&classCount, &classes));
    JvmtiAutoFree classesAutoFree(classes);

    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error %d returned calling GetLoadedClasses()", err));
        return JNI_FALSE;
    }

    int i;
    jclass fieldTypeClass = 0;
    for (i = 0; i < classCount; i++) {
        char* classSignature = 0;

        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassSignature(classes[i], &classSignature, 0));
        JvmtiAutoFree classSignatureAutoFree(classSignature);

        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error %d returned calling GetClassSignature()", err));
            return JNI_FALSE;
        }

        if ( strcmp(fieldSignature, classSignature) == 0 ) {
            fieldTypeClass = classes[i];
            break;
        }

    }
    if ( fieldTypeClass == 0 ) {
    	JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Field type class unexpectedly null"));
        return JNI_FALSE;
    }

    jboolean rtValue = jni->IsInstanceOf(objectValue, fieldTypeClass);
    AgentBase::GetJniEnv()->PopLocalFrame(NULL);
    return rtValue;
}
