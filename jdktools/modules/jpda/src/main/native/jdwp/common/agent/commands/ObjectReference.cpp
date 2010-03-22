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
#include "ObjectReference.h"
#include "PacketParser.h"
#include "ObjectManager.h"
#include "CallBacks.h"
#include "ExceptionManager.h"

#include <string.h>

using namespace jdwp;
using namespace ObjectReference;
using namespace CallBacks;

//------------------------------------------------------------------------------
//ReferenceTypeHandler(1)-------------------------------------------------------

int
ObjectReference::ReferenceTypeHandler::Execute(JNIEnv *jni) 
{
    jobject jvmObject = m_cmdParser->command.ReadObjectID(jni);
    if (NULL == jvmObject) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReferenceType: ReadObjectID() returned NULL"));
        AgentException aex = GetExceptionManager().GetLastException();
        jdwpError err = aex.ErrCode();
        JDWP_SET_EXCEPTION(aex);
        return err;
    }
    // Can be: InternalErrorException, OutOfMemoryException, JDWP_ERROR_INVALID_OBJECT
    jclass jvmClass = jni->GetObjectClass(jvmObject); 

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReferenceType: received: objectID=%p, classSignature=%s", jvmObject, JDWP_CHECK_NULL(signature)));
    }
#endif

    jboolean isArrayClass;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->IsArrayClass(jvmClass, &isArrayClass));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    jbyte refTypeTag = JDWP_TYPE_TAG_CLASS;
    if ( isArrayClass ) {
        refTypeTag = JDWP_TYPE_TAG_ARRAY;
    }

    m_cmdParser->reply.WriteByte(refTypeTag);
    m_cmdParser->reply.WriteReferenceTypeID(jni, jvmClass);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReferenceType: send: refTypeTag=%d, refTypeID=%p", refTypeTag, jvmClass));

    return JDWP_ERROR_NONE;
} // ReferenceTypeHandler::Execute()

//------------------------------------------------------------------------------
//GetValuesHandler(2)-----------------------------------------------------------

int
ObjectReference::GetValuesHandler::Execute(JNIEnv *jni) 
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
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: received: objectID=%p, classSignature=%s, fields=%d", 
                        jvmObject, JDWP_CHECK_NULL(signature), fieldsNumber));
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
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFieldDeclaringClass(jvmClass, jvmFieldID,
            &declaringClass));

        if (err != JVMTI_ERROR_NONE) {
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }

        if ( jni->IsAssignableFrom(jvmClass, declaringClass) == JNI_FALSE ) {
            // given field does not belong to class of passed jobject
            AgentException e(JDWP_ERROR_INVALID_FIELDID);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INVALID_FIELDID;
        }

        char* fieldName = 0;
        char* fieldSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFieldName(jvmClass, jvmFieldID,
            &fieldName, &fieldSignature, 0));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }
        JvmtiAutoFree autoFreeFieldName(fieldName);
        JvmtiAutoFree autoFreeFieldSignature(fieldSignature);

        // Check if given field is static
        jint fieldModifiers;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFieldModifiers(jvmClass, jvmFieldID,
            &fieldModifiers));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID,
            // JVMTI_ERROR_NULL_POINTER
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
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
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get static: field#=%d, fieldID=%%p, value=(boolean)%d",
                                i, jvmFieldID, fieldValue.z));
            } else {
                fieldValue.z = jni->GetBooleanField(jvmObject, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get instance: field#=%d, fieldID=%p, value=(boolean)%d",
                                i, jvmFieldID, fieldValue.z));
            }
            break;
        case 'B':
            fieldValueTag = JDWP_TAG_BYTE;
            if ( isFieldStatic ) {
                fieldValue.b = jni->GetStaticByteField(jvmClass, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get static: field#=%d, fieldID=%p, value=(byte)%d",
                                i, jvmFieldID, fieldValue.b));
            } else {
                fieldValue.b = jni->GetByteField(jvmObject, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get instance: field#=%d, fieldID=%p, value=(byte)%d",
                                i, jvmFieldID, fieldValue.b));
            }
            break;
        case 'C':
            fieldValueTag = JDWP_TAG_CHAR;
            if ( isFieldStatic ) {
                fieldValue.c = jni->GetStaticCharField(jvmClass, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get static: field#=%d, fieldID=%p, value=(char)%d",
                                i, jvmFieldID, fieldValue.c));
            } else {
                fieldValue.c = jni->GetCharField(jvmObject, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get instance: field#=%d, fieldID=%p, value=(char)%d",
                                i, jvmFieldID, fieldValue.c));
            }
            break;
        case 'S':
            fieldValueTag = JDWP_TAG_SHORT;
            if ( isFieldStatic ) {
                fieldValue.s = jni->GetStaticShortField(jvmClass, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get static: field#=%d, fieldID=%p, value=(short)%d",
                                i, jvmFieldID, fieldValue.s));
            } else {
                fieldValue.s = jni->GetShortField(jvmObject, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get instance: field#=%d, fieldID=%p, value=(short)%d",
                                i, jvmFieldID, fieldValue.s));
            }
            break;
        case 'I':
            fieldValueTag = JDWP_TAG_INT;
            if ( isFieldStatic ) {
                fieldValue.i = jni->GetStaticIntField(jvmClass, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get static: field#=%d, fieldID=%p, value=(int)%d",
                                i, jvmFieldID, fieldValue.i));
            } else {
                fieldValue.i = jni->GetIntField(jvmObject, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get instance: field#=%d, fieldID=%p, value=(int)%d",
                                i, jvmFieldID, fieldValue.i));
            }
            break;
        case 'J':
            fieldValueTag = JDWP_TAG_LONG;
            if ( isFieldStatic ) {
                fieldValue.j = jni->GetStaticLongField(jvmClass, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get static: field#=%d, fieldID=%p, value=(long)%lld",
                                i, jvmFieldID, fieldValue.j));
            } else {
                fieldValue.j = jni->GetLongField(jvmObject, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get instance: field#=%d, fieldID=%p, value=(long)%lld",
                                i, jvmFieldID, fieldValue.j));
            }
            break;
        case 'F':
            fieldValueTag = JDWP_TAG_FLOAT;
            if ( isFieldStatic ) {
                fieldValue.f = jni->GetStaticFloatField(jvmClass, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get static: field#=%d, fieldID=%p, value=(float)%f",
                                i, jvmFieldID, fieldValue.f));
            } else {
                fieldValue.f = jni->GetFloatField(jvmObject, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get instance: field#=%d, fieldID=%p, value=(float)%f",
                                i, jvmFieldID, fieldValue.f));
            }
            break;
        case 'D':
            fieldValueTag = JDWP_TAG_DOUBLE;
            if ( isFieldStatic ) {
                fieldValue.d = jni->GetStaticDoubleField(jvmClass, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get static: field#=%d, fieldID=%p, value=(double)%Lf",
                                i, jvmFieldID, fieldValue.d));
            } else {
                fieldValue.d = jni->GetDoubleField(jvmObject, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get instance: field#=%d, fieldID=%p, value=(double)%Lf",
                                i, jvmFieldID, fieldValue.d));
            }
            break;
        case 'L':
        case '[':
            if ( isFieldStatic ) {
                fieldValue.l = jni->GetStaticObjectField(jvmClass, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get static: field#=%d, fieldID=%p, value=(object)%p",
                                i, jvmFieldID, fieldValue.l));
            } else {
                fieldValue.l = jni->GetObjectField(jvmObject, jvmFieldID);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: get instance: field#=%d, fieldID=%p, value=(object)%p",
                                i, jvmFieldID, fieldValue.l));
            }
            fieldValueTag = AgentBase::GetClassManager().GetJdwpTag(jni, fieldValue.l);
            break;
        default:
            // should not reach here
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: bad field signature: field#=%d, fieldID=%p, signature=%s",
                            i, jvmFieldID, fieldSignature));
            AgentException e(JDWP_ERROR_INTERNAL);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INTERNAL;
        }

        m_cmdParser->reply.WriteValue(jni, fieldValueTag, fieldValue);
       
    } // for (int i = 0; i < fieldsNumber; i++) {

    return JDWP_ERROR_NONE;
} // GetValuesHandler::Execute()

//------------------------------------------------------------------------------
//SetValuesHandler(3)-----------------------------------------------------------


int
ObjectReference::SetValuesHandler::Execute(JNIEnv *jni) 
{
    jdwpError jdwpErr;
    jobject jvmObject = m_cmdParser->command.ReadObjectID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_OBJECT
    JDWP_CHECK_ERROR_CODE(jdwpErr);
   
    jclass jvmClass = jni->GetObjectClass(jvmObject); 
    jint fieldsNumber = m_cmdParser->command.ReadInt();
    // Can be: InternalErrorException

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: received: objectID=%d, classSignature=%s, fields=%d",
                        jvmObject, JDWP_CHECK_NULL(signature), fieldsNumber));
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
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFieldDeclaringClass(jvmClass, jvmFieldID,
            &declaringClass));

        if (err != JVMTI_ERROR_NONE) {
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }

        if ( jni->IsAssignableFrom(jvmClass, declaringClass) == JNI_FALSE ) {
            // given field does not belong to class of passed jobject
            AgentException e(JDWP_ERROR_INVALID_FIELDID);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INVALID_FIELDID;
        }

        char* fieldName = 0;
        char* fieldSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFieldName(jvmClass, jvmFieldID,
            &fieldName, &fieldSignature, 0));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }
        JvmtiAutoFree autoFreeFieldName(fieldName);
        JvmtiAutoFree autoFreeFieldSignature(fieldSignature);

        // Check if given field is static
        jint fieldModifiers;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFieldModifiers(jvmClass, jvmFieldID,
            &fieldModifiers));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID,
            // JVMTI_ERROR_NULL_POINTER
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
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
        JDWP_CHECK_ERROR_CODE(jdwpErr);

        switch ( fieldValueTag ) {
        case JDWP_TAG_BOOLEAN:
            if ( isFieldStatic ) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set static: field#=%d, fieldID=%p, value=(boolean)%d",
                                i, jvmFieldID, fieldValue.z));
                jni->SetStaticBooleanField(jvmClass, jvmFieldID, fieldValue.z);
            } else {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set instance: field#=%d, fieldID=%p, value=(boolean)%d",
                                i, jvmFieldID, fieldValue.z));
                jni->SetBooleanField(jvmObject, jvmFieldID, fieldValue.z);
            }
            break;
        case JDWP_TAG_BYTE:
            if ( isFieldStatic ) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set static: field#=%d, fieldID=%p, value=(byte)%d",
                                i, jvmFieldID, fieldValue.b));
                jni->SetStaticByteField(jvmClass, jvmFieldID, fieldValue.b);
            } else {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set instance: field#=%d, fieldID=%p, value=(byte)%d",
                                i, jvmFieldID, fieldValue.b));
                jni->SetByteField(jvmObject, jvmFieldID, fieldValue.b);
            }
            break;
        case JDWP_TAG_CHAR:
            if ( isFieldStatic ) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set static: field#=%d, fieldID=%p, value=(char)%d",
                                i, jvmFieldID, fieldValue.c));
                jni->SetStaticCharField(jvmClass, jvmFieldID, fieldValue.c);
            } else {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set instance: field#=%d, fieldID=%p, value=(char)%d",
                                i, jvmFieldID, fieldValue.c));
                jni->SetCharField(jvmObject, jvmFieldID, fieldValue.c);
            }
            break;
        case JDWP_TAG_SHORT:
            if ( isFieldStatic ) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set static: field#=%d, fieldID=%p, value=(short)%d",
                                i, jvmFieldID, fieldValue.s));
                jni->SetStaticShortField(jvmClass, jvmFieldID, fieldValue.s);
            } else {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set instance: field#=%d, fieldID=%p, value=(short)%d",
                                i, jvmFieldID, fieldValue.s));
                jni->SetShortField(jvmObject, jvmFieldID, fieldValue.s);
            }
            break;
        case JDWP_TAG_INT:
            if ( isFieldStatic ) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set static: field#=%d, fieldID=%p, value=(int)%d",
                                i, jvmFieldID, fieldValue.i));
                jni->SetStaticIntField(jvmClass, jvmFieldID, fieldValue.i);
            } else {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set instance: field#=%d, fieldID=%p, value=(int)%d",
                                i, jvmFieldID, fieldValue.i));
                jni->SetIntField(jvmObject, jvmFieldID, fieldValue.i);
            }
            break;
        case JDWP_TAG_LONG:
            if ( isFieldStatic ) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set static: field#=%d, fieldID=%p, value=(long)%lld",
                                i, jvmFieldID, fieldValue.j));
                jni->SetStaticLongField(jvmClass, jvmFieldID, fieldValue.j);
            } else {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set instance: field#=%d, fieldID=%p, value=(long)%lld",
                                i, jvmFieldID, fieldValue.j));
                jni->SetLongField(jvmObject, jvmFieldID, fieldValue.j);
            }
            break;
        case JDWP_TAG_FLOAT:
            if ( isFieldStatic ) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set static: field#=%d, fieldID=%p, value=(float)%f",
                                i, jvmFieldID, fieldValue.f));
                jni->SetStaticFloatField(jvmClass, jvmFieldID, fieldValue.f);
            } else {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set instance: field#=%d, fieldID=%p, value=(float)%f",
                                i, jvmFieldID, fieldValue.f));
                jni->SetFloatField(jvmObject, jvmFieldID, fieldValue.f);
            }
            break;
        case JDWP_TAG_DOUBLE:
            if ( isFieldStatic ) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set static: field#=%d, fieldID=%p, value=(double)%Lf",
                                i, jvmFieldID, fieldValue.d));
                jni->SetStaticDoubleField(jvmClass, jvmFieldID, fieldValue.d);
            } else {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set instance: field#=%d, fieldID=%p, value=(double)%Lf",
                                i, jvmFieldID, fieldValue.d));
                jni->SetDoubleField(jvmObject, jvmFieldID, fieldValue.d);
            }
            break;
        case JDWP_TAG_OBJECT:
        case JDWP_TAG_ARRAY:
            if ( ! AgentBase::GetClassManager().IsObjectValueFitsFieldType
                    (jni, fieldValue.l, fieldSignature) ) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: bad object type: field#=%d, fieldID=%p, fieldSignature=%s, value(object)=%p",
                                i, jvmFieldID, fieldSignature, fieldValue.l));
                AgentException e(JDWP_ERROR_INVALID_OBJECT);
				JDWP_SET_EXCEPTION(e);
                return JDWP_ERROR_INVALID_OBJECT;
            }

            if ( isFieldStatic ) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set static: field#=%d, fieldID=%p, value=(object)%p",
                                i, jvmFieldID, fieldValue.l));
                jni->SetStaticObjectField(jvmClass, jvmFieldID, fieldValue.l);
            } else {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: set instance: field#=%d, fieldID=%p, value=(object)%p",
                                i, jvmFieldID, fieldValue.l));
                jni->SetObjectField(jvmObject, jvmFieldID, fieldValue.l);
            }
            break;
        default:
            // should not reach here
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: bad field signature: field#=%d, fieldID=%p, fieldSignature=%s",
                                i, jvmFieldID, fieldSignature));
            AgentException e(JDWP_ERROR_INTERNAL);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INTERNAL;
        }

    } // for (int i = 0; i < fieldsNumber; i++) {

    return JDWP_ERROR_NONE;
} // SetValuesHandler::Execute()

//------------------------------------------------------------------------------
//MonitorInfoHandler(5)---------------------------------------------------------

int
ObjectReference::MonitorInfoHandler::Execute(JNIEnv *jni)
{
    jobject jvmObject = m_cmdParser->command.ReadObjectID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_OBJECT
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "MonitorInfo: received: objectID=%p", jvmObject));

    jvmtiMonitorUsage monitorInfo;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetObjectMonitorUsage(jvmObject,
        &monitorInfo));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_INVALID_OBJECT,
        // JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    JvmtiAutoFree autoFreeWaiters(monitorInfo.waiters);
    JvmtiAutoFree autoFreeNotifyWaiters(monitorInfo.notify_waiters);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiThreadInfo info;
        info.name = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(monitorInfo.owner, &info));
        JvmtiAutoFree jafInfoName(info.name);

        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "MonitorInfo: send: ownerID=%d, name=%s, entry_count=%d, waiter_count=%d",
                        monitorInfo.owner, JDWP_CHECK_NULL(info.name), monitorInfo.entry_count, monitorInfo.waiter_count));
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
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(monitorInfo.waiters[i], &info));
            JvmtiAutoFree jafInfoName(info.name);
    
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "MonitorInfo: waiter#=%d, threadID=%p, name=%s",
                            i, monitorInfo.waiters[i], JDWP_CHECK_NULL(info.name)));
        }
#endif
        
        m_cmdParser->reply.WriteObjectID(jni, monitorInfo.waiters[i]);
        // jthread - the thread object waiting this monitor 
    }

    return JDWP_ERROR_NONE;
} // MonitorInfoHandler::Execute()

//------------------------------------------------------------------------------
//DisableCollectionHandler(7)---------------------------------------------------

int
ObjectReference::DisableCollectionHandler::Execute(JNIEnv *jni)
{
    ObjectID objectID = m_cmdParser->command.ReadRawObjectID();
    // Can be: InternalErrorException
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "DisableCollection: received: objectID=%lld", objectID));

    int ret = GetObjectManager().DisableCollection(jni, objectID);
    JDWP_CHECK_RETURN(ret);
    // Can be: JDWP_ERROR_INVALID_OBJECT, OutOfMemoryException
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "DisableCollection: disableCollection"));

    return JDWP_ERROR_NONE;
} // DisableCollectionHandler::Execute()

//------------------------------------------------------------------------------
//EnableCollectionHandler(8)----------------------------------------------------

int
ObjectReference::EnableCollectionHandler::Execute(JNIEnv *jni)
{
    ObjectID objectID = m_cmdParser->command.ReadRawObjectID();
    // Can be: InternalErrorException
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "EnableCollection: received: objectID=%lld", objectID));

    int ret = GetObjectManager().EnableCollection(jni, objectID);
    JDWP_CHECK_RETURN(ret);
    // Can be: OutOfMemoryException
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "EnableCollection: enableCollection"));

    return JDWP_ERROR_NONE;
} // EnableCollectionHandler::Execute()

//------------------------------------------------------------------------------
//IsCollectedHandler(9)---------------------------------------------------

int
ObjectReference::IsCollectedHandler::Execute(JNIEnv *jni)
{
    ObjectID objectID = m_cmdParser->command.ReadRawObjectID();
    // Can be: InternalErrorException
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsCollected: received: objectID=%lld", objectID));

    jboolean isCollected = GetObjectManager().IsCollected(jni, objectID);
    // Can be: JDWP_ERROR_INVALID_OBJECT

    m_cmdParser->reply.WriteBoolean(isCollected);
    // Can be: OutOfMemoryException
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsCollected: send: isCollected=%s", (isCollected?"TRUE":"FALSE")));

    return JDWP_ERROR_NONE;
} // IsCollectedHandler::Execute()

//------------------------------------------------------------------------------
//InvokeMethodHandler(6)---------------------------------------------------

const char* ObjectReference::InvokeMethodHandler::GetThreadName() {
    return "_jdwp_ObjectReference_InvokeMethodHandler";
}

int 
ObjectReference::InvokeMethodHandler::Execute(JNIEnv *jni) 
{
    m_object = m_cmdParser->command.ReadObjectID(jni);
    m_thread = m_cmdParser->command.ReadThreadID(jni);
    m_clazz = m_cmdParser->command.ReadReferenceTypeID(jni);
    m_methodID = m_cmdParser->command.ReadMethodID(jni);
    int arguments = m_cmdParser->command.ReadInt();

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InvokeMethod: received: objectID=%p, classID=%p, threadID=%p, methodID=%p, arguments=%d",
                    m_object, m_clazz, m_thread, m_methodID, arguments));

    if (AgentBase::GetClassManager().IsClass(jni, m_clazz) != JNI_TRUE) {
        AgentException e(JDWP_ERROR_INVALID_CLASS);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_CLASS;
    }
    
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
        jvmtiThreadInfo threadInfo;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(m_thread, &threadInfo));
        JvmtiAutoFree aftn(threadInfo.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InvokeMethod: call: method=%s, sig=%s, class=%s, thread=%s", 
                        JDWP_CHECK_NULL(name), JDWP_CHECK_NULL(signature), JDWP_CHECK_NULL(classSignature),
                        JDWP_CHECK_NULL(threadInfo.name)));
    }
#endif

    JDWP_ASSERT(signature[0] == '(');
    JDWP_ASSERT(strlen(signature) >= 3);
    JDWP_ASSERT(signature + strlen(signature) >= strchr(signature, ')'));

    int methodArguments = getArgsNumber(signature);

    if (arguments != methodArguments) {
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
    for (int i = 0; i < arguments; i++) {
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
                 JDWP_CHECK_NULL(name), JDWP_CHECK_NULL(signature), JDWP_CHECK_NULL(classSignature), m_thread,
                 m_returnValue.tag, m_returnException));
    }
#endif

    return JDWP_ERROR_NONE;
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

// New commands for Java 6
//------------------------------------------------------------------------------
//ReferringObjectsHandler(10)---------------------------------------------------

int
ObjectReference::ReferringObjectsHandler::Execute(JNIEnv *jni) 
{
    // Get objectID
    jobject jvmObject = m_cmdParser->command.ReadObjectID(jni);
    // Can be: InternalErrorException, OutOfMemoryException, JDWP_ERROR_INVALID_OBJECT

    // Get maximum number of referring objects to return.
    int maxReferrers = m_cmdParser->command.ReadInt();
    if(maxReferrers < 0) {
        AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_ILLEGAL_ARGUMENT;
    }

    // Define tag for referree object
    jlong targetObjTag = 0xefff;
    // Set tag for target object
     jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetTag(jvmObject, targetObjTag));
    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY,  JVMTI_ERROR_INVALID_OBJECT
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    
    // Define tag for referrer object
    jlong referrerObjTag = 0xdfff;
    jlong tags[2] = {targetObjTag, referrerObjTag};

    // Initial callbacks for jvmtiHeapCallbacks
    jvmtiHeapCallbacks hcbs;
    memset(&hcbs, 0, sizeof(hcbs));
    hcbs.heap_iteration_callback = NULL;
    hcbs.heap_reference_callback = &HeapReferenceCallback_ReferringObject;
    hcbs.primitive_field_callback = NULL;
    hcbs.array_primitive_value_callback = NULL;
    hcbs.string_primitive_value_callback = NULL;

    //It initiates a traversal over the objects that are directly and indirectly reachable from the heap roots.
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->FollowReferences(0, NULL,  NULL,
         &hcbs, tags));
    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_INVALID_CLASS
        // JVMTI_ERROR_INVALID_OBJECT, JVMTI_ERROR_NULL_POINTER 
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    const jlong referrerTags[1] = {referrerObjTag};
    jint referringObjectsNum = 0;
    jobject * pResultObjects = 0;
    // Return the instances that have been marked expectd tag.
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetObjectsWithTags(1, referrerTags, &referringObjectsNum,
           &pResultObjects, NULL));
    JvmtiAutoFree afResultObjects(pResultObjects);

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_ILLEGAL_ARGUMENT 
        // JVMTI_ERROR_ILLEGAL_ARGUMENT, JVMTI_ERROR_NULL_POINTER  
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    jint returnReferringObjectsNum;
    //If maxReferrers is zero, all instances are returned.
    if(0 == maxReferrers) {
        returnReferringObjectsNum = referringObjectsNum;
    }
    else if(maxReferrers < referringObjectsNum) {
        returnReferringObjectsNum = maxReferrers;
    }
    else {
        returnReferringObjectsNum = referringObjectsNum;
    }

    // Compose reply package
    m_cmdParser->reply.WriteInt(returnReferringObjectsNum);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReferringObject: return objects number: %d", returnReferringObjectsNum));

    for(int i = 0; i < returnReferringObjectsNum; i++) {
        m_cmdParser->reply.WriteTaggedObjectID(jni, pResultObjects[i]);
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetTag(pResultObjects[i], 0));
        jni->DeleteLocalRef(pResultObjects[i]);
        if (err != JVMTI_ERROR_NONE) {
            AgentException e(err);
            JDWP_SET_EXCEPTION(e);
            return err;
        }
    }
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetTag(jvmObject, 0));
    if (err != JVMTI_ERROR_NONE) {
      AgentException e(err);
      JDWP_SET_EXCEPTION(e);
      return err;
    }
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReferringObject: tagged-objectID returned."));

    return JDWP_ERROR_NONE;
} //ReferringObjectsHandler::Execute()
