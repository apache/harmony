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
 * @author Anatoly F. Bondarenko, Viacheslav G. Rybalov
 */
#include <string.h>
#include "ObjectReference.h"
#include "PacketParser.h"
#include "ObjectManager.h"

using namespace jdwp;
using namespace ObjectReference;

//------------------------------------------------------------------------------
//ReferenceTypeHandler(1)-------------------------------------------------------

void
ObjectReference::ReferenceTypeHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jobject jvmObject = m_cmdParser->command.ReadObjectID(jni);
    // Can be: InternalErrorException, OutOfMemoryException, JDWP_ERROR_INVALID_OBJECT
    jclass jvmClass = jni->GetObjectClass(jvmObject); 

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afs(signature);
        JDWP_TRACE_DATA("ReferenceType: received: objectID=" << jvmObject 
            << ", classSignature=" << JDWP_CHECK_NULL(signature));
    }
#endif

    jboolean isArrayClass;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->IsArrayClass(jvmClass, &isArrayClass));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
    }

    jbyte refTypeTag = JDWP_TYPE_TAG_CLASS;
    if ( isArrayClass ) {
        refTypeTag = JDWP_TYPE_TAG_ARRAY;
    }

    m_cmdParser->reply.WriteByte(refTypeTag);
    m_cmdParser->reply.WriteReferenceTypeID(jni, jvmClass);
    JDWP_TRACE_DATA("ReferenceType: send: refTypeTag=" << refTypeTag 
        << ", refTypeID=" << jvmClass);

} // ReferenceTypeHandler::Execute()

//------------------------------------------------------------------------------
//GetValuesHandler(2)-----------------------------------------------------------

void
ObjectReference::GetValuesHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jobject jvmObject = m_cmdParser->command.ReadObjectID(jni);
    // Can be: InternalErrorException, OutOfMemoryException, JDWP_ERROR_INVALID_OBJECT

    jclass jvmClass = jni->GetObjectClass(jvmObject); 
    jint fieldsNumber = m_cmdParser->command.ReadInt();
    // Can be: InternalErrorException

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("GetValues: received: objectID=" << jvmObject 
            << ", classSignature=" << JDWP_CHECK_NULL(signature)
            << ", fields=" << fieldsNumber);
    }
#endif

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    m_cmdParser->reply.WriteInt(fieldsNumber);
    // The number of values returned, always equal to the number of values to get.

    for (int i = 0; i < fieldsNumber; i++) {
        jfieldID jvmFieldID = m_cmdParser->command.ReadFieldID(jni);
        // Can be: InternalErrorException, OutOfMemoryException

        // check that given field belongs to class of passed jobject (jvmClass)
        // taking into account inheritance
        jvmtiError err;
        jclass declaringClass;
        JVMTI_TRACE(err, jvmti->GetFieldDeclaringClass(jvmClass, jvmFieldID,
            &declaringClass));

        if (err != JVMTI_ERROR_NONE) {
            throw AgentException(err);
        }

        if ( jni->IsAssignableFrom(jvmClass, declaringClass) == JNI_FALSE ) {
            // given field does not belong to class of passed jobject
            throw AgentException(JDWP_ERROR_INVALID_FIELDID);
        }

        char* fieldName = 0;
        char* fieldSignature = 0;
        JVMTI_TRACE(err, jvmti->GetFieldName(jvmClass, jvmFieldID,
            &fieldName, &fieldSignature, 0));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID
            throw AgentException(err);
        }
        JvmtiAutoFree autoFreeFieldName(fieldName);
        JvmtiAutoFree autoFreeFieldSignature(fieldSignature);

        // Check if given field is static
        jint fieldModifiers;
        JVMTI_TRACE(err, jvmti->GetFieldModifiers(jvmClass, jvmFieldID,
            &fieldModifiers));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID,
            // JVMTI_ERROR_NULL_POINTER
            throw AgentException(err);
        }

        jboolean isFieldStatic = JNI_FALSE;
        if ( (fieldModifiers & ACC_STATIC) != 0 ) { // ACC_STATIC_FLAG = 0x0008;
            // given field is static
            isFieldStatic = JNI_TRUE;
        }

        jvalue fieldValue;
        jdwpTag fieldValueTag;
        switch ( fieldSignature[0] ) {
        case 'Z':
            fieldValueTag = JDWP_TAG_BOOLEAN;
            if ( isFieldStatic ) {
                fieldValue.z = jni->GetStaticBooleanField(jvmClass, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(boolean)" << fieldValue.z);
            } else {
                fieldValue.z = jni->GetBooleanField(jvmObject, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(boolean)" << fieldValue.z);
            }
            break;
        case 'B':
            fieldValueTag = JDWP_TAG_BYTE;
            if ( isFieldStatic ) {
                fieldValue.b = jni->GetStaticByteField(jvmClass, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(byte)" << fieldValue.b);
            } else {
                fieldValue.b = jni->GetByteField(jvmObject, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(byte)" << fieldValue.b);
            }
            break;
        case 'C':
            fieldValueTag = JDWP_TAG_CHAR;
            if ( isFieldStatic ) {
                fieldValue.c = jni->GetStaticCharField(jvmClass, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(char)" << fieldValue.c);
            } else {
                fieldValue.c = jni->GetCharField(jvmObject, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(char)" << fieldValue.c);
            }
            break;
        case 'S':
            fieldValueTag = JDWP_TAG_SHORT;
            if ( isFieldStatic ) {
                fieldValue.s = jni->GetStaticShortField(jvmClass, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(short)" << fieldValue.s);
            } else {
                fieldValue.s = jni->GetShortField(jvmObject, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(short)" << fieldValue.s);
            }
            break;
        case 'I':
            fieldValueTag = JDWP_TAG_INT;
            if ( isFieldStatic ) {
                fieldValue.i = jni->GetStaticIntField(jvmClass, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(int)" << fieldValue.i);
            } else {
                fieldValue.i = jni->GetIntField(jvmObject, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(int)" << fieldValue.i);
            }
            break;
        case 'J':
            fieldValueTag = JDWP_TAG_LONG;
            if ( isFieldStatic ) {
                fieldValue.j = jni->GetStaticLongField(jvmClass, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(long)" << fieldValue.l);
            } else {
                fieldValue.j = jni->GetLongField(jvmObject, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(long)" << fieldValue.l);
            }
            break;
        case 'F':
            fieldValueTag = JDWP_TAG_FLOAT;
            if ( isFieldStatic ) {
                fieldValue.f = jni->GetStaticFloatField(jvmClass, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(float)" << fieldValue.f);
            } else {
                fieldValue.f = jni->GetFloatField(jvmObject, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(float)" << fieldValue.f);
            }
            break;
        case 'D':
            fieldValueTag = JDWP_TAG_DOUBLE;
            if ( isFieldStatic ) {
                fieldValue.d = jni->GetStaticDoubleField(jvmClass, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(double)" << fieldValue.d);
            } else {
                fieldValue.d = jni->GetDoubleField(jvmObject, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(double)" << fieldValue.d);
            }
            break;
        case 'L':
        case '[':
            if ( isFieldStatic ) {
                fieldValue.l = jni->GetStaticObjectField(jvmClass, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(object)" << fieldValue.l);
            } else {
                fieldValue.l = jni->GetObjectField(jvmObject, jvmFieldID);
                JDWP_TRACE_DATA("GetValues: get instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(double)" << fieldValue.l);
            }
            fieldValueTag = AgentBase::GetClassManager().GetJdwpTag(jni, fieldValue.l);
            break;
        default:
            // should not reach here
            JDWP_TRACE_DATA("GetValues: bad field signature: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", signature: " << fieldSignature);
            throw InternalErrorException();
        }

        m_cmdParser->reply.WriteValue(jni, fieldValueTag, fieldValue);
       
    } // for (int i = 0; i < fieldsNumber; i++) {

} // GetValuesHandler::Execute()

//------------------------------------------------------------------------------
//SetValuesHandler(3)-----------------------------------------------------------


void
ObjectReference::SetValuesHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jobject jvmObject = m_cmdParser->command.ReadObjectID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_OBJECT
   
    jclass jvmClass = jni->GetObjectClass(jvmObject); 
    jint fieldsNumber = m_cmdParser->command.ReadInt();
    // Can be: InternalErrorException

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afs(signature);
        JDWP_TRACE_DATA("SetValues: received: objectID=" << jvmObject 
            << ", classSignature=" << JDWP_CHECK_NULL(signature)
            << ", fields=" << fieldsNumber);
    }
#endif

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();
    for (int i = 0; i < fieldsNumber; i++) {
        jfieldID jvmFieldID = m_cmdParser->command.ReadFieldID(jni);
        // Can be: InternalErrorException, OutOfMemoryException

        // check that given field belongs to class of passed jobject (jvmClass)
        // taking into account inheritance
        jvmtiError err;
        jclass declaringClass;
        JVMTI_TRACE(err, jvmti->GetFieldDeclaringClass(jvmClass, jvmFieldID,
            &declaringClass));

        if (err != JVMTI_ERROR_NONE) {
            throw AgentException(err);
        }

        if ( jni->IsAssignableFrom(jvmClass, declaringClass) == JNI_FALSE ) {
            // given field does not belong to class of passed jobject
            throw AgentException(JDWP_ERROR_INVALID_FIELDID);
        }

        char* fieldName = 0;
        char* fieldSignature = 0;
        JVMTI_TRACE(err, jvmti->GetFieldName(jvmClass, jvmFieldID,
            &fieldName, &fieldSignature, 0));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID
            throw AgentException(err);
        }
        JvmtiAutoFree autoFreeFieldName(fieldName);
        JvmtiAutoFree autoFreeFieldSignature(fieldSignature);

        // Check if given field is static
        jint fieldModifiers;
        JVMTI_TRACE(err, jvmti->GetFieldModifiers(jvmClass, jvmFieldID,
            &fieldModifiers));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID,
            // JVMTI_ERROR_NULL_POINTER
            throw AgentException(err);
        }

        jboolean isFieldStatic = JNI_FALSE;
        if ( (fieldModifiers & ACC_STATIC) != 0 ) { // ACC_STATIC_FLAG = 0x0008;
            // given field is static
            isFieldStatic = JNI_TRUE;
        }

        jdwpTag fieldValueTag 
            = AgentBase::GetClassManager().GetJdwpTagFromSignature(fieldSignature);

        jvalue fieldValue 
            = m_cmdParser->command.ReadUntaggedValue(jni, fieldValueTag);
        // Can be: InternalErrorException, OutOfMemoryException,
        // JDWP_ERROR_INVALID_OBJECT

        switch ( fieldValueTag ) {
        case JDWP_TAG_BOOLEAN:
            if ( isFieldStatic ) {
                JDWP_TRACE_DATA("SetValues: set static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(boolean)" << fieldValue.z);
                jni->SetStaticBooleanField(jvmClass, jvmFieldID, fieldValue.z);
            } else {
                JDWP_TRACE_DATA("SetValues: set instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(boolean)" << fieldValue.z);
                jni->SetBooleanField(jvmObject, jvmFieldID, fieldValue.z);
            }
            break;
        case JDWP_TAG_BYTE:
            if ( isFieldStatic ) {
                JDWP_TRACE_DATA("SetValues: set static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(byte)" << fieldValue.b);
                jni->SetStaticByteField(jvmClass, jvmFieldID, fieldValue.b);
            } else {
                JDWP_TRACE_DATA("SetValues: set instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(byte)" << fieldValue.b);
                jni->SetByteField(jvmObject, jvmFieldID, fieldValue.b);
            }
            break;
        case JDWP_TAG_CHAR:
            if ( isFieldStatic ) {
                JDWP_TRACE_DATA("SetValues: set static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(char)" << fieldValue.c);
                jni->SetStaticCharField(jvmClass, jvmFieldID, fieldValue.c);
            } else {
                JDWP_TRACE_DATA("SetValues: set instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(char)" << fieldValue.c);
                jni->SetCharField(jvmObject, jvmFieldID, fieldValue.c);
            }
            break;
        case JDWP_TAG_SHORT:
            if ( isFieldStatic ) {
                JDWP_TRACE_DATA("SetValues: set static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(short)" << fieldValue.s);
                jni->SetStaticShortField(jvmClass, jvmFieldID, fieldValue.s);
            } else {
                JDWP_TRACE_DATA("SetValues: set instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(short)" << fieldValue.s);
                jni->SetShortField(jvmObject, jvmFieldID, fieldValue.s);
            }
            break;
        case JDWP_TAG_INT:
            if ( isFieldStatic ) {
                JDWP_TRACE_DATA("SetValues: set static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(int)" << fieldValue.i);
                jni->SetStaticIntField(jvmClass, jvmFieldID, fieldValue.i);
            } else {
                JDWP_TRACE_DATA("SetValues: set instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(int)" << fieldValue.i);
                jni->SetIntField(jvmObject, jvmFieldID, fieldValue.i);
            }
            break;
        case JDWP_TAG_LONG:
            if ( isFieldStatic ) {
                JDWP_TRACE_DATA("SetValues: set static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(long)" << fieldValue.j);
                jni->SetStaticLongField(jvmClass, jvmFieldID, fieldValue.j);
            } else {
                JDWP_TRACE_DATA("SetValues: set instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(long)" << fieldValue.j);
                jni->SetLongField(jvmObject, jvmFieldID, fieldValue.j);
            }
            break;
        case JDWP_TAG_FLOAT:
            if ( isFieldStatic ) {
                JDWP_TRACE_DATA("SetValues: set static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(float)" << fieldValue.f);
                jni->SetStaticFloatField(jvmClass, jvmFieldID, fieldValue.f);
            } else {
                JDWP_TRACE_DATA("SetValues: set instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(float)" << fieldValue.f);
                jni->SetFloatField(jvmObject, jvmFieldID, fieldValue.f);
            }
            break;
        case JDWP_TAG_DOUBLE:
            if ( isFieldStatic ) {
                JDWP_TRACE_DATA("SetValues: set static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(double)" << fieldValue.d);
                jni->SetStaticDoubleField(jvmClass, jvmFieldID, fieldValue.d);
            } else {
                JDWP_TRACE_DATA("SetValues: set instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(double)" << fieldValue.d);
                jni->SetDoubleField(jvmObject, jvmFieldID, fieldValue.d);
            }
            break;
        case JDWP_TAG_OBJECT:
        case JDWP_TAG_ARRAY:
            if ( ! AgentBase::GetClassManager().IsObjectValueFitsFieldType
                    (jni, fieldValue.l, fieldSignature) ) {
                JDWP_TRACE_DATA("SetValues: bad object type: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", signature=" << fieldSignature
                    << ", value=(object)" << fieldValue.l);
                throw AgentException(JDWP_ERROR_INVALID_OBJECT);
            }

            if ( isFieldStatic ) {
                JDWP_TRACE_DATA("SetValues: set static: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(object)" << fieldValue.l);
                jni->SetStaticObjectField(jvmClass, jvmFieldID, fieldValue.l);
            } else {
                JDWP_TRACE_DATA("SetValues: set instance: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", value=(object)" << fieldValue.l);
                jni->SetObjectField(jvmObject, jvmFieldID, fieldValue.l);
            }
            break;
        default:
            // should not reach here
            JDWP_TRACE_DATA("SetValues: bad field signature: field#=" << i
                    << ", fieldID=" << jvmFieldID
                    << ", signature: " << fieldSignature);
            throw InternalErrorException();
        }

    } // for (int i = 0; i < fieldsNumber; i++) {

} // SetValuesHandler::Execute()

//------------------------------------------------------------------------------
//MonitorInfoHandler(5)---------------------------------------------------------

void
ObjectReference::MonitorInfoHandler::Execute(JNIEnv *jni)
    throw (AgentException)
{
    jobject jvmObject = m_cmdParser->command.ReadObjectID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_OBJECT
    JDWP_TRACE_DATA("MonitorInfo: received: objectID=" << jvmObject);

    jvmtiMonitorUsage monitorInfo;

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetObjectMonitorUsage(jvmObject,
        &monitorInfo));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_INVALID_OBJECT,
        // JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
    }

    JvmtiAutoFree autoFreeWaiters(monitorInfo.waiters);
    JvmtiAutoFree autoFreeNotifyWaiters(monitorInfo.notify_waiters);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiThreadInfo info;
        info.name = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(monitorInfo.owner, &info));
        JvmtiAutoFree jafInfoName(info.name);

        JDWP_TRACE_DATA("MonitorInfo: send: ownerID=" << monitorInfo.owner 
            << ", name=" << JDWP_CHECK_NULL(info.name)
            << ", entry_count=" << monitorInfo.entry_count 
            << ", waiter_count=" << monitorInfo.waiter_count);
    }
#endif
    
    m_cmdParser->reply.WriteObjectID(jni, monitorInfo.owner);
    // jthread - the thread object owning this monitor, or NULL if unused 

    m_cmdParser->reply.WriteInt(monitorInfo.entry_count);
    // The number of times the owning thread has entered the monitor  

    m_cmdParser->reply.WriteInt(monitorInfo.waiter_count);
    // The number of threads waiting to own this monitor   

    for (int i = 0; i < monitorInfo.waiter_count; i++) {

#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiThreadInfo info;
            info.name = 0;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(monitorInfo.waiters[i], &info));
            JvmtiAutoFree jafInfoName(info.name);
    
            JDWP_TRACE_DATA("MonitorInfo: waiter#=" << i 
                << ", threadID=" << monitorInfo.waiters[i]
                << ", name=" << JDWP_CHECK_NULL(info.name));
        }
#endif
        
        m_cmdParser->reply.WriteObjectID(jni, monitorInfo.waiters[i]);
        // jthread - the thread object waiting this monitor 
    }

} // MonitorInfoHandler::Execute()

//------------------------------------------------------------------------------
//DisableCollectionHandler(7)---------------------------------------------------

void
ObjectReference::DisableCollectionHandler::Execute(JNIEnv *jni)
    throw (AgentException)
{
    ObjectID objectID = m_cmdParser->command.ReadRawObjectID();
    // Can be: InternalErrorException
    JDWP_TRACE_DATA("DisableCollection: received: objectID=" << objectID);

    GetObjectManager().DisableCollection(jni, objectID);
    // Can be: JDWP_ERROR_INVALID_OBJECT, OutOfMemoryException
    JDWP_TRACE_DATA("DisableCollection: disableCollection");

} // DisableCollectionHandler::Execute()

//------------------------------------------------------------------------------
//EnableCollectionHandler(8)----------------------------------------------------

void
ObjectReference::EnableCollectionHandler::Execute(JNIEnv *jni)
    throw (AgentException)
{
    ObjectID objectID = m_cmdParser->command.ReadRawObjectID();
    // Can be: InternalErrorException
    JDWP_TRACE_DATA("EnableCollection: received: objectID=" << objectID);

    GetObjectManager().EnableCollection(jni, objectID);
    // Can be: OutOfMemoryException
    JDWP_TRACE_DATA("EnableCollection: enableCollection");

} // EnableCollectionHandler::Execute()

//------------------------------------------------------------------------------
//IsCollectedHandler(9)---------------------------------------------------

void
ObjectReference::IsCollectedHandler::Execute(JNIEnv *jni)
    throw (AgentException)
{
    ObjectID objectID = m_cmdParser->command.ReadRawObjectID();
    // Can be: InternalErrorException
    JDWP_TRACE_DATA("IsCollected: received: objectID=" << objectID);

    jboolean isCollected = GetObjectManager().IsCollected(jni, objectID);
    // Can be: JDWP_ERROR_INVALID_OBJECT

    m_cmdParser->reply.WriteBoolean(isCollected);
    // Can be: OutOfMemoryException
    JDWP_TRACE_DATA("IsCollected: send: isCollected=" << isCollected);

} // IsCollectedHandler::Execute()

//------------------------------------------------------------------------------
//InvokeMethodHandler(6)---------------------------------------------------

const char* ObjectReference::InvokeMethodHandler::GetThreadName() {
    return "_jdwp_ObjectReference_InvokeMethodHandler";
}

void 
ObjectReference::InvokeMethodHandler::Execute(JNIEnv *jni) throw(AgentException) 
{
    m_object = m_cmdParser->command.ReadObjectID(jni);
    m_thread = m_cmdParser->command.ReadThreadID(jni);
    m_clazz = m_cmdParser->command.ReadReferenceTypeID(jni);
    m_methodID = m_cmdParser->command.ReadMethodID(jni);
    int arguments = m_cmdParser->command.ReadInt();

    JDWP_TRACE_DATA("InvokeMethod: received: "
        << "objectID=" << m_object 
        << ", classID=" << m_clazz 
        << ", threadID=" << m_thread 
        << ", methodID=" << m_methodID
        << ", arguments=" << arguments);

    if (AgentBase::GetClassManager().IsClass(jni, m_clazz) != JNI_TRUE) {
        throw AgentException(JDWP_ERROR_INVALID_CLASS);
    }
    
    char* signature = 0;
    char* name = 0;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodName(m_methodID,
        &name, &signature, 0 /*&generic signature*/));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
    JvmtiAutoFree afv2(signature);
    JvmtiAutoFree afv3(name);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(m_clazz, &classSignature, 0));
        JvmtiAutoFree afs(classSignature);
        jvmtiThreadInfo threadInfo;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(m_thread, &threadInfo));
        JvmtiAutoFree aftn(threadInfo.name);
        JDWP_TRACE_DATA("InvokeMethod: call: method=" << JDWP_CHECK_NULL(name)
            << ", sig=" << JDWP_CHECK_NULL(signature)
            << ", class=" << JDWP_CHECK_NULL(classSignature) 
            << ", thread=" << JDWP_CHECK_NULL(threadInfo.name));
    }
#endif

    JDWP_ASSERT(signature[0] == '(');
    JDWP_ASSERT(strlen(signature) >= 3);
    JDWP_ASSERT(signature + strlen(signature) >= strchr(signature, ')'));

    int methodArguments = getArgsNumber(signature);

    if (arguments != methodArguments) {
        throw AgentException(JDWP_ERROR_ILLEGAL_ARGUMENT);
    }
    if (arguments != 0) {
        m_methodValues = 
            reinterpret_cast<jvalue*>(AgentBase::GetMemoryManager().Allocate(sizeof(jvalue) * arguments JDWP_FILE_LINE));
    } else {
        m_methodValues = 0;
    }
    AgentAutoFree afv1(m_methodValues JDWP_FILE_LINE);

    m_returnValue.tag = static_cast<jdwpTag>(*(strchr(signature, ')') + 1));
    for (int i = 0; i < arguments; i++) {
        jdwpTaggedValue tValue = m_cmdParser->command.ReadValue(jni);
        if (IsArgValid(jni, i, tValue, signature) != JNI_TRUE) {
            JDWP_TRACE_DATA("InvokeMethod: bad argument " << i << ": sig=" << signature);
            throw AgentException(JDWP_ERROR_TYPE_MISMATCH);
        }
        m_methodValues[i] = tValue.value;
    }
    m_invokeOptions = m_cmdParser->command.ReadInt();

    m_returnError = JDWP_ERROR_NONE;
    m_returnException = 0;

    WaitDeferredInvocation(jni);

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
        throw AgentException(m_returnError);
    }
    
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(m_clazz, &classSignature, 0));
        JvmtiAutoFree afs(classSignature);
        JDWP_LOG("InvokeMethod: return: method=" << JDWP_CHECK_NULL(name)
            << ", sig=" << JDWP_CHECK_NULL(signature)
            << ", class=" << JDWP_CHECK_NULL(classSignature)
            << ", thread=" << m_thread
            << ", returnValueTag=" << m_returnValue.tag
            << ", returnException=" << m_returnException);
    }
#endif

}

void 
ObjectReference::InvokeMethodHandler::ExecuteDeferredFunc(JNIEnv *jni)
{
    JDWP_ASSERT(m_returnValue.tag != 0);
    JDWP_ASSERT(m_methodID != 0);
    JDWP_ASSERT(jni != 0);
    if ((m_invokeOptions & JDWP_INVOKE_NONVIRTUAL) != 0) {
        JDWP_ASSERT(m_clazz != 0);
        switch (m_returnValue.tag) {
        case JDWP_TAG_BOOLEAN:
            m_returnValue.value.z = jni->CallNonvirtualBooleanMethodA(m_object, m_clazz, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_BYTE:
            m_returnValue.value.b = jni->CallNonvirtualByteMethodA(m_object, m_clazz, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_CHAR:
            m_returnValue.value.c = jni->CallNonvirtualCharMethodA(m_object, m_clazz, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_SHORT:
            m_returnValue.value.s = jni->CallNonvirtualShortMethodA(m_object, m_clazz, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_INT:
            m_returnValue.value.i = jni->CallNonvirtualIntMethodA(m_object, m_clazz, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_LONG:
            m_returnValue.value.j = jni->CallNonvirtualLongMethodA(m_object, m_clazz, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_FLOAT:
            m_returnValue.value.f = jni->CallNonvirtualFloatMethodA(m_object, m_clazz, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_DOUBLE:
            m_returnValue.value.d = jni->CallNonvirtualDoubleMethodA(m_object, m_clazz, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_VOID:
            jni->CallNonvirtualVoidMethodA(m_object, m_clazz, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_ARRAY:
        case JDWP_TAG_OBJECT:
            m_returnValue.value.l =
                jni->CallNonvirtualObjectMethodA(m_object, m_clazz, m_methodID, m_methodValues);
            if (m_returnValue.value.l != 0) {
                m_returnValue.value.l = jni->NewGlobalRef(m_returnValue.value.l);
                if (m_returnValue.value.l == 0) {
                    m_returnError = JDWP_ERROR_OUT_OF_MEMORY;
                }
            }
            m_returnValue.tag = GetClassManager().GetJdwpTag(jni, m_returnValue.value.l);
            break;
        default:
            m_returnError = JDWP_ERROR_INVALID_TAG;
            return;
        }
    } else {
        switch (m_returnValue.tag) {
        case JDWP_TAG_BOOLEAN:
            m_returnValue.value.z = jni->CallBooleanMethodA(m_object, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_BYTE:
            m_returnValue.value.b = jni->CallByteMethodA(m_object, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_CHAR:
            m_returnValue.value.c = jni->CallCharMethodA(m_object, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_SHORT:
            m_returnValue.value.s = jni->CallShortMethodA(m_object, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_INT:
            m_returnValue.value.i = jni->CallIntMethodA(m_object, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_LONG:
            m_returnValue.value.j = jni->CallLongMethodA(m_object, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_FLOAT:
            m_returnValue.value.f = jni->CallFloatMethodA(m_object, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_DOUBLE:
            m_returnValue.value.d = jni->CallDoubleMethodA(m_object, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_VOID:
            jni->CallVoidMethodA(m_object, m_methodID, m_methodValues);
            break;
        case JDWP_TAG_ARRAY:
        case JDWP_TAG_OBJECT:
            m_returnValue.value.l =
                jni->CallObjectMethodA(m_object, m_methodID, m_methodValues);
            if (m_returnValue.value.l != 0) {
                m_returnValue.value.l = jni->NewGlobalRef(m_returnValue.value.l);
                if (m_returnValue.value.l == 0) {
                    m_returnError = JDWP_ERROR_OUT_OF_MEMORY;
                }
            }
            m_returnValue.tag = GetClassManager().GetJdwpTag(jni, m_returnValue.value.l);
            break;
        default:
            m_returnError = JDWP_ERROR_INVALID_TAG;
            return;
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
