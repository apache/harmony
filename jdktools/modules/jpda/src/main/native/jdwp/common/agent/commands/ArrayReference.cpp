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
#include "ArrayReference.h"
#include "PacketParser.h"
#include "ClassManager.h"
#include "ExceptionManager.h"


#include <string.h>

using namespace jdwp;
using namespace ArrayReference;

int
ArrayReference::LengthHandler::Execute(JNIEnv *jni) 
{
    jarray arrayObject = (jarray)m_cmdParser->command.ReadArrayID(jni);
    if (arrayObject == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Length: null array: arrayID=%p", arrayObject));
        AgentException e(JDWP_ERROR_INVALID_OBJECT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_OBJECT;
    }
    jclass arrObjClass = jni->GetObjectClass(arrayObject);
#ifndef NDEBUG 
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(arrObjClass, &signature, 0));
        JvmtiAutoFree afs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Length: arrayID=%p, classSignature=%s", arrayObject, JDWP_CHECK_NULL(signature)));
    }
#endif
    JDWP_ASSERT(arrObjClass != 0);
    jboolean is_array_class;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->IsArrayClass(arrObjClass, &is_array_class));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    if (is_array_class != JNI_TRUE) {
        AgentException e(JDWP_ERROR_INVALID_ARRAY);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_ARRAY;
    }

    jsize length = jni->GetArrayLength(arrayObject);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Length: send: length=%d", length));
    m_cmdParser->reply.WriteInt(length);

    return JDWP_ERROR_NONE;
}

int
ArrayReference::GetValuesHandler::Execute(JNIEnv *jni) 
{
    jarray arrayObject = m_cmdParser->command.ReadArrayID(jni);
    jint firstIndex = m_cmdParser->command.ReadInt();
    jint length = m_cmdParser->command.ReadInt();

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: received: arrayID=%p, firstIndex=%d, length=%d", arrayObject, firstIndex, length));

    if (NULL == arrayObject) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: ReadArrayID() returned NULL"));
        AgentException aex = GetExceptionManager().GetLastException();
        jdwpError err = aex.ErrCode();
        JDWP_SET_EXCEPTION(aex);
        return err;
    }

    jclass arrObjClass = jni->GetObjectClass(arrayObject);

    char* signature = 0;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(arrObjClass,
        &signature, 0));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    JvmtiAutoFree afv1(signature);
    if(signature[0] != '[') {
        AgentException e(JDWP_ERROR_INVALID_ARRAY);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_ARRAY;
    }
    jsize arrLength = jni->GetArrayLength(arrayObject);
    JDWP_ASSERT(arrLength >= 0);
    if ( (firstIndex < 0) || (firstIndex >= arrLength) ) {
        AgentException e(JDWP_ERROR_INVALID_INDEX);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_INDEX;
    }
    if ( length == -1 ) {
        length = arrLength - firstIndex;
    }
    if ( (length < 0 ) || (firstIndex + length > arrLength) ) {
        AgentException e(JDWP_ERROR_INVALID_LENGTH);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_LENGTH;
    }
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: values=%d, signature=%s", length, JDWP_CHECK_NULL(signature)));
    jvalue value;
    ClassManager& classManager = AgentBase::GetClassManager();
    switch (signature[1]) {
        case 'Z': {
            m_cmdParser->reply.WriteByte(JDWP_TAG_BOOLEAN);
            m_cmdParser->reply.WriteInt(length);
            if ( length == 0 ) {
                return JDWP_ERROR_NONE;
            }
            jboolean* bufferArray = reinterpret_cast<jboolean*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jboolean)*length JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            jni->GetBooleanArrayRegion(static_cast<jbooleanArray>(arrayObject), firstIndex, length, bufferArray);
            classManager.CheckOnException(jni);
                        
            for (int i = 0; i < length; i++) {
                value.z = bufferArray[i];
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: index=%d, value=(boolean)%d", i, value.z));
                m_cmdParser->reply.WriteUntaggedValue(jni, JDWP_TAG_BOOLEAN, value);
            }
            return JDWP_ERROR_NONE;
        }
        case 'B': {
            m_cmdParser->reply.WriteByte(JDWP_TAG_BYTE);
            m_cmdParser->reply.WriteInt(length);
            if ( length == 0 ) {
                return JDWP_ERROR_NONE;
            }
            jbyte* bufferArray = reinterpret_cast<jbyte*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jbyte)*length JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            jni->GetByteArrayRegion(static_cast<jbyteArray>(arrayObject), firstIndex, length, bufferArray);
            classManager.CheckOnException(jni);
                        
            for (int i = 0; i < length; i++) {
                value.b = bufferArray[i];
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: index=%d, value=(byte)%d", i, value.b));
                m_cmdParser->reply.WriteUntaggedValue(jni, JDWP_TAG_BYTE, value);
            }
            return JDWP_ERROR_NONE;
        }
        case 'C': {
            m_cmdParser->reply.WriteByte(JDWP_TAG_CHAR);
            m_cmdParser->reply.WriteInt(length);
            if ( length == 0 ) {
                return JDWP_ERROR_NONE;
            }
            jchar* bufferArray = reinterpret_cast<jchar*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jchar)*length JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            jni->GetCharArrayRegion(static_cast<jcharArray>(arrayObject), firstIndex, length, bufferArray);
            classManager.CheckOnException(jni);
                        
            for (int i = 0; i < length; i++) {
                value.c = bufferArray[i];
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: index=%d, value=(char)%d", i, value.c));
                m_cmdParser->reply.WriteUntaggedValue(jni, JDWP_TAG_CHAR, value);
            }
            return JDWP_ERROR_NONE;
        }
        case 'S': {
            m_cmdParser->reply.WriteByte(JDWP_TAG_SHORT);
            m_cmdParser->reply.WriteInt(length);
            if ( length == 0 ) {
                return JDWP_ERROR_NONE;
            }
            jshort* bufferArray = reinterpret_cast<jshort*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jshort)*length JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            jni->GetShortArrayRegion(static_cast<jshortArray>(arrayObject), firstIndex, length, bufferArray);
            classManager.CheckOnException(jni);
                        
            for (int i = 0; i < length; i++) {
                value.s = bufferArray[i];
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: index=%d, value=(short)%d", i, value.s));
                m_cmdParser->reply.WriteUntaggedValue(jni, JDWP_TAG_SHORT, value);
            }
            return JDWP_ERROR_NONE;
        }
        case 'I': {
            m_cmdParser->reply.WriteByte(JDWP_TAG_INT);
            m_cmdParser->reply.WriteInt(length);
            if ( length == 0 ) {
                return JDWP_ERROR_NONE;
            }
            jint* bufferArray = reinterpret_cast<jint*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jint)*length JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            jni->GetIntArrayRegion(static_cast<jintArray>(arrayObject), firstIndex, length, bufferArray);
            classManager.CheckOnException(jni);
                        
            for (int i = 0; i < length; i++) {
                value.i = bufferArray[i];
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: index=%d, value=(int)%d", i, value.i));
                m_cmdParser->reply.WriteUntaggedValue(jni, JDWP_TAG_INT, value);
            }
            return JDWP_ERROR_NONE;
        }
        case 'J': {
            m_cmdParser->reply.WriteByte(JDWP_TAG_LONG);
            m_cmdParser->reply.WriteInt(length);
            if ( length == 0 ) {
                return JDWP_ERROR_NONE;
            }
            jlong* bufferArray = reinterpret_cast<jlong*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jlong)*length JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            jni->GetLongArrayRegion(static_cast<jlongArray>(arrayObject), firstIndex, length, bufferArray);
            classManager.CheckOnException(jni);
                       
            for (int i = 0; i < length; i++) {
                value.j = bufferArray[i];
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: index=%d, value=(long)%lld", i, value.j));
                m_cmdParser->reply.WriteUntaggedValue(jni, JDWP_TAG_LONG, value);
            }
            return JDWP_ERROR_NONE;
        }
        case 'F': {
            m_cmdParser->reply.WriteByte(JDWP_TAG_FLOAT);
            m_cmdParser->reply.WriteInt(length);
            if ( length == 0 ) {
                return JDWP_ERROR_NONE;
            }
            jfloat* bufferArray = reinterpret_cast<jfloat*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jfloat)*length JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            jni->GetFloatArrayRegion(static_cast<jfloatArray>(arrayObject), firstIndex, length, bufferArray);
            classManager.CheckOnException(jni);
                        
            for (int i = 0; i < length; i++) {
                value.f = bufferArray[i];
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: index=%d, value=(float)%f", i, value.f));
                m_cmdParser->reply.WriteUntaggedValue(jni, JDWP_TAG_FLOAT, value);
            }
            return JDWP_ERROR_NONE;
        }
        case 'D': {
            m_cmdParser->reply.WriteByte(JDWP_TAG_DOUBLE);
            m_cmdParser->reply.WriteInt(length);
            if ( length == 0 ) {
                return JDWP_ERROR_NONE;
            }
            jdouble* bufferArray = reinterpret_cast<jdouble*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jdouble)*length JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            jni->GetDoubleArrayRegion(static_cast<jdoubleArray>(arrayObject), firstIndex, length, bufferArray);
            classManager.CheckOnException(jni);
                        
            for (int i = 0; i < length; i++) {
                value.d = bufferArray[i];
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: index=%d, value=(double)%Lf", i, value.d));
                m_cmdParser->reply.WriteUntaggedValue(jni, JDWP_TAG_DOUBLE, value);
            }
            return JDWP_ERROR_NONE;
        }
        case '[':
            m_cmdParser->reply.WriteByte(JDWP_TAG_ARRAY);
            break;
        case 'L':
            m_cmdParser->reply.WriteByte(JDWP_TAG_OBJECT);
            break;
        default:
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: bad type signature: %s", JDWP_CHECK_NULL(signature)));
            AgentException e(JDWP_ERROR_INVALID_ARRAY);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INVALID_ARRAY;
    }
    m_cmdParser->reply.WriteInt(length);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: length=%d", length));
    for (int i = 0; i < length; i++) {
        jobject objArrayElement = jni->GetObjectArrayElement(static_cast<jobjectArray>(arrayObject), firstIndex + i);
        classManager.CheckOnException(jni);

        jdwpTag tag = classManager.GetJdwpTag(jni, objArrayElement);

        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: index=%d, tag=%d, value=(object)%p", i, tag, objArrayElement));

        m_cmdParser->reply.WriteByte((jbyte)tag);
        m_cmdParser->reply.WriteObjectID(jni, objArrayElement);
    }

    return JDWP_ERROR_NONE;
}

int
ArrayReference::SetValuesHandler::Execute(JNIEnv *jni) 
{
    jarray arrayObject = m_cmdParser->command.ReadArrayID(jni);
    jint firstIndex = m_cmdParser->command.ReadInt();
    jint values = m_cmdParser->command.ReadInt();

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: received: arrayID=%p, firstIndex=%d, values=%d", arrayObject, firstIndex, values));

    if (arrayObject == 0) {
        AgentException e(JDWP_ERROR_INVALID_OBJECT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_OBJECT;
    }
    if ((firstIndex < 0) || (values < 0)) {
        AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_ILLEGAL_ARGUMENT;
    }
    jclass arrObjClass = jni->GetObjectClass(arrayObject);
    JDWP_ASSERT(arrObjClass != 0);
    char* signature = 0;
    char* generic = 0;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(arrObjClass,
        &signature, &generic));
    JvmtiAutoFree afv1(signature);
    JvmtiAutoFree afv2(generic);
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    if ((signature == 0) || (strlen(signature) < 2)) {
        AgentException e(JDWP_ERROR_INVALID_OBJECT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_OBJECT;
    }
    if(signature[0] != '[') {
        AgentException e(JDWP_ERROR_INVALID_ARRAY);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_ARRAY;
    }
    jsize arrLength = jni->GetArrayLength(arrayObject);
    JDWP_ASSERT(arrLength >= 0);
    if (firstIndex + values > arrLength) {
        AgentException e(JDWP_ERROR_INVALID_LENGTH);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_LENGTH;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: values=%d, signature=%s, generic=%s", values, JDWP_CHECK_NULL(signature), JDWP_CHECK_NULL(generic)));

    jvalue value;
    switch (signature[1]) {
        case 'Z': {
            jboolean* bufferArray = reinterpret_cast<jboolean*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jboolean)*values JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            for (int i = 0; i < values; i++) {
                value = m_cmdParser->command.ReadUntaggedValue(jni, JDWP_TAG_BOOLEAN);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set: index=%d, value=(boolean)%d", i, value.z));
                bufferArray[i] = value.z;
            }
            jni->SetBooleanArrayRegion(static_cast<jbooleanArray>(arrayObject), firstIndex, values, bufferArray);
            break;
        }
        case 'B': {
            jbyte* bufferArray = reinterpret_cast<jbyte*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jbyte)*values JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
                        
            for (int i = 0; i < values; i++) {
                value = m_cmdParser->command.ReadUntaggedValue(jni, JDWP_TAG_BYTE);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set: index=%d, value=(byte)%d", i, value.b));
                bufferArray[i] = value.b;
            }
            jni->SetByteArrayRegion(static_cast<jbyteArray>(arrayObject), firstIndex, values, bufferArray);
            break;
        }
        case 'C': {
            jchar* bufferArray = reinterpret_cast<jchar*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jchar)*values JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
                        
            for (int i = 0; i < values; i++) {
                value = m_cmdParser->command.ReadUntaggedValue(jni, JDWP_TAG_CHAR);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set: index=%d, value=(char)%d", i, value.c));
                bufferArray[i] = value.c;
            }
            jni->SetCharArrayRegion(static_cast<jcharArray>(arrayObject), firstIndex, values, bufferArray);
            break;
        }
        case 'S': {
            jshort* bufferArray = reinterpret_cast<jshort*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jshort)*values JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            
            for (int i = 0; i < values; i++) {
                value = m_cmdParser->command.ReadUntaggedValue(jni, JDWP_TAG_SHORT);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set: index=%d, value=(short)%d", i, value.s));
                bufferArray[i] = value.s;
            }
            jni->SetShortArrayRegion(static_cast<jshortArray>(arrayObject), firstIndex, values, bufferArray);
            break;
        }
        case 'I': {
            jint* bufferArray = reinterpret_cast<jint*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jint)*values JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            
            for (int i = 0; i < values; i++) {
                value = m_cmdParser->command.ReadUntaggedValue(jni, JDWP_TAG_INT);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set: index=%d, value=(int)%d", i, value.i));
                bufferArray[i] = value.i;
            }
            jni->SetIntArrayRegion(static_cast<jintArray>(arrayObject), firstIndex, values, bufferArray);
            break;
        }
        case 'J': {
            jlong* bufferArray = reinterpret_cast<jlong*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jlong)*values JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            
            for (int i = 0; i < values; i++) {
                value = m_cmdParser->command.ReadUntaggedValue(jni, JDWP_TAG_LONG);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set: index=%d, value=(long)%lld", i, value.j));
                bufferArray[i] = value.j;
            }
            jni->SetLongArrayRegion(static_cast<jlongArray>(arrayObject), firstIndex, values, bufferArray);
            break;
        }
        case 'F': {
            jfloat* bufferArray = reinterpret_cast<jfloat*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jfloat)*values JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            
            for (int i = 0; i < values; i++) {
                value = m_cmdParser->command.ReadUntaggedValue(jni, JDWP_TAG_FLOAT);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set: index=%d, value=(float)%f", i, value.f));
                bufferArray[i] = value.f;
            }
            jni->SetFloatArrayRegion(static_cast<jfloatArray>(arrayObject), firstIndex, values, bufferArray);
            break;
        }
        case 'D': {
            jdouble* bufferArray = reinterpret_cast<jdouble*>(AgentBase::GetMemoryManager()
                    .Allocate(sizeof(jdouble)*values JDWP_FILE_LINE));
            AgentAutoFree scavenger(bufferArray JDWP_FILE_LINE);
            
            for (int i = 0; i < values; i++) {
                value = m_cmdParser->command.ReadUntaggedValue(jni, JDWP_TAG_DOUBLE);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set: index=%d, value=(dounle)%Lf", i, value.d));
                bufferArray[i] = value.d;
            }
            jni->SetDoubleArrayRegion(static_cast<jdoubleArray>(arrayObject), firstIndex, values, bufferArray);
            break;
        }
        case '[':
        case 'L': {
            for (int i = 0; i < values; i++) {
                value = m_cmdParser->command.ReadUntaggedValue(jni, JDWP_TAG_OBJECT);
                jobject objArrayElement = value.l;

                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set: index=%d, value=(object)%p", i, value.l));
                
                jni->SetObjectArrayElement(static_cast<jobjectArray>(arrayObject), firstIndex + i, objArrayElement);
                jthrowable ex = jni->ExceptionOccurred();
                
                // indicate error if any exception occured
                if (ex != 0) {
                    jni->ExceptionClear();
                    AgentException e(JDWP_ERROR_INVALID_ARRAY);
					JDWP_SET_EXCEPTION(e);
                    return JDWP_ERROR_INVALID_ARRAY;
                }
            }
            break;
        }
        default:
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: bad type signature: %s", JDWP_CHECK_NULL(signature)));
            AgentException e(JDWP_ERROR_INVALID_ARRAY);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INVALID_ARRAY;
    }           

    // check if exception occured
    {
        jthrowable ex = jni->ExceptionOccurred();
        
        // indicate error if any exception occured
        if (ex != 0) {
            jni->ExceptionClear();
            AgentException e(JDWP_ERROR_INVALID_ARRAY);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INVALID_ARRAY;
        }
    }

    return JDWP_ERROR_NONE;
}
