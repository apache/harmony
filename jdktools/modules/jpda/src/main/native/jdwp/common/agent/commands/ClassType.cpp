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
#include "ClassType.h"

#include "PacketParser.h"
#include "ClassManager.h"
#include "ThreadManager.h"
#include "ExceptionManager.h"

#include <string.h>

using namespace jdwp;
using namespace ClassType;

//-----------------------------------------------------------------------------
// SuperClass -----------------------------------------------------------------

int 
ClassType::SuperClassHandler::Execute(JNIEnv *jni)
{
    //INVALID_CLASS or INVALID_OBJECT can be thrown below
    jclass clazz = m_cmdParser->command.ReadReferenceTypeID(jni);
    JDWP_CHECK_NOT_NULL(clazz);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SuperClass: received: classID=%p", clazz));

    // get superclass
    jclass superClazz = jni->GetSuperclass(clazz);
    
    // superClazz is null for java.lang.Object or for an interface. That is ok

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(superClazz, &signature, 0));
        JvmtiAutoFree afs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SuperClass: send: superClassID=%p, classSignature=%s", superClazz, JDWP_CHECK_NULL(signature)));
    }
#endif

    // write super class to the reply
    m_cmdParser->reply.WriteReferenceTypeID(jni, superClazz);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
// SetValues -----------------------------------------------------------------

int 
ClassType::SetValuesHandler::Execute(JNIEnv *jni) 
{
    //INVALID_CLASS or INVALID_OBJECT can be thrown below
    jclass clazz = m_cmdParser->command.ReadReferenceTypeID(jni);
    JDWP_CHECK_NOT_NULL(clazz);

    jint values = m_cmdParser->command.ReadInt();
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: received: classID=%p, values=%d", clazz, values));

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    // check for CLASS_NOT_PREPARED
    jint status;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassStatus(clazz, &status));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    // jint const JVMTI_CLASS_STATUS_PREPARED = 0x2 ;
    if ( (status & 0x2) == 0 ) {
        AgentException e(JDWP_ERROR_CLASS_NOT_PREPARED);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_CLASS_NOT_PREPARED;
    }

    //Repeats values times:
    for (int i=0; i<values; i++) {
        jfieldID fieldID = m_cmdParser->command.ReadFieldID(jni);

        // check that given field belongs to passed jvmClass
        // taking into account inheritance
        jvmtiError err;
        jclass declaringClass;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFieldDeclaringClass(clazz, fieldID,
            &declaringClass));

        if (err != JVMTI_ERROR_NONE) {
            AgentException e(err);
			JDWP_SET_EXCEPTION(e);
            return err;
        }

        if ( jni->IsAssignableFrom(clazz, declaringClass) == JNI_FALSE ) {
            // given field does not belong to passed jvmClass
            AgentException e(JDWP_ERROR_INVALID_FIELDID);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INVALID_FIELDID;
        }

        // Check non-static field
        jint fieldModifiers;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFieldModifiers(clazz, fieldID,
            &fieldModifiers));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID,
            // JVMTI_ERROR_NULL_POINTER
            AgentException e(err);
			JDWP_SET_EXCEPTION(e);
            return err;
        }

        if ( (fieldModifiers & 0x0008) == 0 ) { // ACC_STATIC_FLAG = 0x0008;
            // given field is not static
            AgentException e(JDWP_ERROR_INVALID_FIELDID);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INVALID_FIELDID;
        }

        //Do not check field for final - for compatibility with jdwp
        // SetStatic<Type>Field() JNI functions do not change final field value

        // pointers for jwmti returned parameters
        char* p_name = 0;
        char* p_signature = 0;

        // try to obtain the field's name and signature
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFieldName(clazz, fieldID,
            &p_name, &p_signature, 0));

        // create auto-free pointers for the following objects
        JvmtiAutoFree af1(p_name);
        JvmtiAutoFree af2(p_signature);

        if (err != JVMTI_ERROR_NONE) {
            // can be one of jvmti universal errors or
            // JVMTI_ERROR_INVALID_CLASS or JVMTI_ERROR_INVALID_FIELDID
            AgentException e(err);
			JDWP_SET_EXCEPTION(e);
            return err;
        }

        jdwpTag tag = GetClassManager().GetJdwpTagFromSignature(p_signature);
        if (tag == JDWP_TAG_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: unknown field signature: %s", JDWP_CHECK_NULL(p_signature)));
            AgentException e(JDWP_ERROR_INTERNAL);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INTERNAL;
        }

        jvalue value = m_cmdParser->command.ReadUntaggedValue(jni, tag);
        // Can be: InternalErrorException, OutOfMemoryException,
        // JDWP_ERROR_INVALID_OBJECT
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set: value#=%d, fieldID=%p, fieldModifiers=%d, fieldName=%s, fieldSignature=%s, tag=%d",
                        i, fieldID, fieldModifiers, JDWP_CHECK_NULL(p_name), JDWP_CHECK_NULL(p_signature), tag));
        
        switch (tag) {
        case JDWP_TAG_BOOLEAN:
            jni->SetStaticBooleanField(clazz, fieldID, value.z);
            break;
        case JDWP_TAG_BYTE:
            jni->SetStaticByteField(clazz, fieldID, value.b);
            break;
        case JDWP_TAG_CHAR:
            jni->SetStaticCharField(clazz, fieldID, value.c);
            break;
        case JDWP_TAG_SHORT:
            jni->SetStaticShortField(clazz, fieldID, value.s);
            break;
        case JDWP_TAG_INT:
            jni->SetStaticIntField(clazz, fieldID, value.i);
            break;
        case JDWP_TAG_LONG:
            jni->SetStaticLongField(clazz, fieldID, value.j);
            break;
        case JDWP_TAG_FLOAT:
            jni->SetStaticFloatField(clazz, fieldID, value.f);
            break;
        case JDWP_TAG_DOUBLE:
            jni->SetStaticDoubleField(clazz, fieldID, value.d);
            break;
        case JDWP_TAG_OBJECT:
        case JDWP_TAG_ARRAY:
            if (!GetClassManager().IsObjectValueFitsFieldType(
                    jni, value.l, p_signature) ) {
                AgentException e(JDWP_ERROR_INVALID_OBJECT);
				JDWP_SET_EXCEPTION(e);
                return JDWP_ERROR_INVALID_OBJECT;
            }
            jni->SetStaticObjectField(clazz, fieldID, value.l);
            break;
        default:
            // should not reach here
            break;
        }

    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
// InvokeMethod ---------------------------------------------------------------

const char* ClassType::InvokeMethodHandler::GetThreadName() {
    return "_jdwp_ClassType_InvokeMethod";
}

int 
ClassType::InvokeMethodHandler::Execute(JNIEnv *jni)
{
    m_clazz = m_cmdParser->command.ReadReferenceTypeID(jni);
    JDWP_CHECK_NOT_NULL(m_clazz);
    m_thread = m_cmdParser->command.ReadThreadID(jni);
    JDWP_CHECK_NOT_NULL(m_thread);

    m_methodID = m_cmdParser->command.ReadMethodID(jni);
    jint arguments = m_cmdParser->command.ReadInt();

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InvokeMethod: received: refTypeID=%p, threadID=%p, methodID=%p, arguments=%d", 
                    m_clazz, m_thread, m_methodID, arguments));

    if (AgentBase::GetClassManager().IsClass(jni, m_clazz) != JNI_TRUE) {
        AgentException e(JDWP_ERROR_INVALID_CLASS);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_CLASS;
    }

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();
    jvmtiError err;

    // check that given method belongs to passed class
    // taking into account inheritance
    jclass declaringClass;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetMethodDeclaringClass(m_methodID, &declaringClass));

    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    if ( jni->IsAssignableFrom(m_clazz, declaringClass) == JNI_FALSE ) {
        // given method does not belong to passed class
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InvokeMethod: given method does not belong to passed class"));
        AgentException e(JDWP_ERROR_INVALID_METHODID);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_METHODID;
    }

    // check that given method is static method
    jint methodModifiers;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetMethodModifiers(m_methodID, &methodModifiers));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    if ( (methodModifiers & 0x0008) == 0 ) { // ACC_STATIC_FLAG = 0x0008;
        // given method is not static
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InvokeMethod: given method is not static"));
        AgentException e(JDWP_ERROR_INVALID_METHODID);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_METHODID;
    }

    char* signature = 0;
    char* name = 0;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetMethodName(m_methodID,
        &name, &signature, 0 /*&generic signature*/));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    
    JvmtiAutoFree afv2(signature);
    JvmtiAutoFree afv3(name);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(m_clazz, &classSignature, 0));
        JvmtiAutoFree afs(classSignature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InvokeMethod: call: method=%s, sig=%s, class=%s, thread=%p", 
                        JDWP_CHECK_NULL(name), JDWP_CHECK_NULL(signature), JDWP_CHECK_NULL(classSignature), m_thread));
    }
#endif

    JDWP_ASSERT(signature[0] == '(');
    JDWP_ASSERT(strlen(signature) >= 3);
    JDWP_ASSERT(signature + strlen(signature) >= strchr(signature, ')'));

    int methodArguments = getArgsNumber(signature);

    if (arguments != methodArguments) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InvokeMethod: arguments != methodArguments"));
        AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_ILLEGAL_ARGUMENT;
    }
    if (arguments != 0) {
        m_methodValues = 
            reinterpret_cast<jvalue*>(AgentBase::GetMemoryManager().Allocate(sizeof(jvalue) * arguments JDWP_FILE_LINE));
    } else {
        m_methodValues = 0;
    }
    AgentAutoFree afv1(m_methodValues JDWP_FILE_LINE);

    m_returnValue.tag = static_cast<jdwpTag>(*(strchr(signature, ')') + 1));
    for (jint i = 0; i < arguments; i++) {
        jdwpTaggedValue tValue = m_cmdParser->command.ReadValue(jni);
        if (IsArgValid(jni, m_clazz, i, tValue, signature) != JNI_TRUE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InvokeMethod: bad argument %d: sig=%s", i, signature));
            AgentException e(JDWP_ERROR_TYPE_MISMATCH);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_TYPE_MISMATCH;
        }
        m_methodValues[i] = tValue.value;
    }
    m_invokeOptions = m_cmdParser->command.ReadInt();

    m_returnError = JDWP_ERROR_NONE;
    m_returnException = 0;

    int ret = WaitDeferredInvocation(jni);
    JDWP_CHECK_RETURN(ret);

    if (m_returnError == JDWP_ERROR_NONE) {
        m_cmdParser->reply.WriteValue(jni, m_returnValue.tag, m_returnValue.value);
        m_cmdParser->reply.WriteTaggedObjectID(jni, m_returnException);
    }
    
    switch (m_returnValue.tag) {
    case JDWP_TAG_OBJECT:
    case JDWP_TAG_ARRAY:
    case JDWP_TAG_STRING:
    case JDWP_TAG_THREAD:
    case JDWP_TAG_THREAD_GROUP:
    case JDWP_TAG_CLASS_LOADER:
    case JDWP_TAG_CLASS_OBJECT:
        if (m_returnValue.value.l != 0) {
            jni->DeleteGlobalRef(m_returnValue.value.l);
        }
        break;
    default:
        break;
    }
    if (m_returnException != 0) {
        jni->DeleteGlobalRef(m_returnException);
    }

    if (m_returnError != JDWP_ERROR_NONE) {
        AgentException e(m_returnError);
		JDWP_SET_EXCEPTION(e);
        return m_returnError;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(m_clazz, &classSignature, 0));
        JvmtiAutoFree afs(classSignature);
        JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "InvokeMethod: return: method=%s, sig=%s, class=%s, thread=%p, returnValueTag=%d, returnException=%p", 
                 JDWP_CHECK_NULL(name), JDWP_CHECK_NULL(signature), JDWP_CHECK_NULL(classSignature), m_thread, m_returnValue.tag, m_returnException));
    }
#endif

    return JDWP_ERROR_NONE;
}

void 
ClassType::InvokeMethodHandler::ExecuteDeferredFunc(JNIEnv *jni)
{
    JDWP_ASSERT(m_returnValue.tag != 0);
    JDWP_ASSERT(m_clazz != 0);
    JDWP_ASSERT(m_methodID != 0);
    JDWP_ASSERT(jni != 0);
    switch (m_returnValue.tag) {
    case JDWP_TAG_BOOLEAN:
        m_returnValue.value.z = jni->CallStaticBooleanMethodA(m_clazz, m_methodID, m_methodValues);
        break;
    case JDWP_TAG_BYTE:
        m_returnValue.value.b = jni->CallStaticByteMethodA(m_clazz, m_methodID, m_methodValues);
        break;
    case JDWP_TAG_CHAR:
        m_returnValue.value.c = jni->CallStaticCharMethodA(m_clazz, m_methodID, m_methodValues);
        break;
    case JDWP_TAG_SHORT:
        m_returnValue.value.s = jni->CallStaticShortMethodA(m_clazz, m_methodID, m_methodValues);
        break;
    case JDWP_TAG_INT:
        m_returnValue.value.i = jni->CallStaticIntMethodA(m_clazz, m_methodID, m_methodValues);
        break;
    case JDWP_TAG_LONG:
        m_returnValue.value.j = jni->CallStaticLongMethodA(m_clazz, m_methodID, m_methodValues);
        break;
    case JDWP_TAG_FLOAT:
        m_returnValue.value.f = jni->CallStaticFloatMethodA(m_clazz, m_methodID, m_methodValues);
        break;
    case JDWP_TAG_DOUBLE:
        m_returnValue.value.d = jni->CallStaticDoubleMethodA(m_clazz, m_methodID, m_methodValues);
        break;
    case JDWP_TAG_VOID:
        jni->CallStaticVoidMethodA(m_clazz, m_methodID, m_methodValues);
        break;
    case JDWP_TAG_ARRAY:
    case JDWP_TAG_OBJECT: {
        m_returnValue.value.l =
            jni->CallStaticObjectMethodA(m_clazz, m_methodID, m_methodValues);
        if (m_returnValue.value.l != 0) {
            m_returnValue.value.l = jni->NewGlobalRef(m_returnValue.value.l);
            if (m_returnValue.value.l == 0) {
                m_returnError = JDWP_ERROR_OUT_OF_MEMORY;
            }
        }
        m_returnValue.tag = GetClassManager().GetJdwpTag(jni, m_returnValue.value.l);
        break;
    }
    default:
        m_returnError = JDWP_ERROR_INVALID_TAG;
        return;
    }
    m_returnException = jni->ExceptionOccurred();
    if (m_returnException != 0) {
        jni->ExceptionClear();
        m_returnException =
            static_cast<jthrowable>(jni->NewGlobalRef(m_returnException));
        if (m_returnException == 0) {
            m_returnError = JDWP_ERROR_OUT_OF_MEMORY;
        }
    }
}

//-----------------------------------------------------------------------------
// NewInstance ----------------------------------------------------------------

const char* ClassType::NewInstanceHandler::GetThreadName() {
    return "_jdwp_ClassType_NewInstanceHandler";
}

int 
ClassType::NewInstanceHandler::Execute(JNIEnv *jni) 
{
    m_clazz = m_cmdParser->command.ReadReferenceTypeID(jni);
    JDWP_CHECK_NOT_NULL(m_clazz);

    if (AgentBase::GetClassManager().IsClass(jni, m_clazz) != JNI_TRUE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "NewInstance: not a class: refTypeID=%p", m_clazz));
        AgentException e(JDWP_ERROR_INVALID_CLASS);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_CLASS;
    }

    m_thread = m_cmdParser->command.ReadThreadID(jni);
    JDWP_CHECK_NOT_NULL(m_thread);
    m_methodID = m_cmdParser->command.ReadMethodID(jni);
    int passedArguments = m_cmdParser->command.ReadInt();

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "NewInstance: received: refTypeID=%p, threadID=%p, methodID=%p, arguments=%d", 
                    m_clazz, m_thread, m_methodID, passedArguments));

    char* signature = 0;
    char* name = 0;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(m_methodID,
        &name, &signature, 0 /*&generic signature*/));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    
    JvmtiAutoFree afv2(signature);
    JvmtiAutoFree afv3(name);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(m_clazz, &classSignature, 0));
        JvmtiAutoFree afs(classSignature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "NewInstance: call method=%s, sig=%s, class=%s, thread=%p",
                        JDWP_CHECK_NULL(name), JDWP_CHECK_NULL(signature), JDWP_CHECK_NULL(classSignature), m_thread));
    }
#endif

    JDWP_ASSERT(signature[0] == '(');
    JDWP_ASSERT(strlen(signature) >= 3);
    JDWP_ASSERT(signature + strlen(signature) >= strchr(signature, ')'));

    int methodArguments = getArgsNumber(signature);

    if (passedArguments != methodArguments) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "NewInstance: passedArguments != methodArguments"));
        AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_ILLEGAL_ARGUMENT;
    }
    if (passedArguments != 0) {
        m_methodValues = 
            reinterpret_cast<jvalue*>(AgentBase::GetMemoryManager().Allocate(sizeof(jvalue) * passedArguments JDWP_FILE_LINE));
    } else {
        m_methodValues = 0;
    }
    AgentAutoFree afv1(m_methodValues JDWP_FILE_LINE);

    for (int i = 0; i < passedArguments; i++) {
        jdwpTaggedValue tValue = m_cmdParser->command.ReadValue(jni);
        if (IsArgValid(jni, m_clazz, i, tValue, signature) != JNI_TRUE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "NewInstance: bad argument %d: sig=%s", i, signature));
            AgentException e(JDWP_ERROR_TYPE_MISMATCH);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_TYPE_MISMATCH;
        }
        m_methodValues[i] = tValue.value;
    }
    m_invokeOptions = m_cmdParser->command.ReadInt();

    m_returnError = JDWP_ERROR_NONE;
    m_returnException = 0;
    m_returnValue = 0;

    int ret = WaitDeferredInvocation(jni);
    JDWP_CHECK_RETURN(ret);

    if (m_returnError == JDWP_ERROR_NONE) {
        m_cmdParser->reply.WriteTaggedObjectID(jni, m_returnValue);
        m_cmdParser->reply.WriteTaggedObjectID(jni, m_returnException);
    }
    
    if (m_returnValue != 0) {
        jni->DeleteGlobalRef(m_returnValue);
    }
    if (m_returnException != 0) {
        jni->DeleteGlobalRef(m_returnException);
    }

    if (m_returnError != JDWP_ERROR_NONE) {
        AgentException e(m_returnError);
		JDWP_SET_EXCEPTION(e);
        return m_returnError;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(m_clazz, &classSignature, 0));
        JvmtiAutoFree afs(classSignature);
        JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "NewInstance: return: methodName=%s, sig=%s, class=%s, thread=%p, returnObject=%d, returnException=%p",
                 JDWP_CHECK_NULL(name), JDWP_CHECK_NULL(signature), JDWP_CHECK_NULL(classSignature), m_thread, m_returnValue, m_returnException));
    }
#endif

    return JDWP_ERROR_NONE;
}

void 
ClassType::NewInstanceHandler::ExecuteDeferredFunc(JNIEnv *jni)
{
    JDWP_ASSERT(m_clazz != 0);
    JDWP_ASSERT(m_methodID != 0);
    JDWP_ASSERT(jni != 0);

    m_returnValue = jni->NewObjectA(m_clazz, m_methodID, m_methodValues);
    if (m_returnValue != 0) {
        m_returnValue = jni->NewGlobalRef(m_returnValue);
        if (m_returnValue == 0) {
            m_returnError = JDWP_ERROR_OUT_OF_MEMORY;
        }
    }
    m_returnException = jni->ExceptionOccurred();
    if (m_returnException != 0) {
        jni->ExceptionClear();
        m_returnException =
            static_cast<jthrowable>(jni->NewGlobalRef(m_returnException));
        if (m_returnException == 0) {
            m_returnError = JDWP_ERROR_OUT_OF_MEMORY;
        }
    }
}
