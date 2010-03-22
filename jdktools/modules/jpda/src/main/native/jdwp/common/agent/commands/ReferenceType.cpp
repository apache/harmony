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
#include "ReferenceType.h"
#include "PacketParser.h"
#include "ClassManager.h"
#include "CallBacks.h"
#include "ExceptionManager.h"

#include <ctype.h>
#include <string.h>

using namespace jdwp;
using namespace ReferenceType;
using namespace CallBacks;

//------------------------------------------------------------------------------
//SignatureFileHandler(1)----------------------------------------------------------

int
ReferenceType::SignatureHandler::Execute(JNIEnv *jni)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
    JDWP_CHECK_NOT_NULL(jvmClass);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Signature: received: refTypeID=%p", jvmClass));

    char* classSignature = 0;
    char* classGenericSignature = 0;
    char** genericSignaturePtr = 0;

    if ( m_withGeneric ) {
        genericSignaturePtr = &classGenericSignature;
    }

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass,
        &classSignature, genericSignaturePtr));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    JvmtiAutoFree autoFreeSignature(classSignature);
    JvmtiAutoFree autoFreeGenericSignature(classGenericSignature);

    m_cmdParser->reply.WriteString(classSignature);
    if ( m_withGeneric ) {
        if (classGenericSignature != 0) {
            m_cmdParser->reply.WriteString(classGenericSignature);
        } else {
            m_cmdParser->reply.WriteString("");
        }
    }
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Signature: send: classSignature=%s, classGenericSignature=%s", JDWP_CHECK_NULL(classSignature), JDWP_CHECK_NULL(classGenericSignature)));

    return JDWP_ERROR_NONE;
} // SignatureHandler::Execute()

//------------------------------------------------------------------------------
//ClassLoaderHandler(2)----------------------------------------------------------

int
ReferenceType::ClassLoaderHandler::Execute(JNIEnv *jni) 
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassLoader: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif

    jobject jvmClassLoader;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassLoader(jvmClass, &jvmClassLoader));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    // if GetClassLoader() returns NULL value for jvmClassLoader
    // consider it as the class loader for the jvmClass is the system class
    // loader and write in reply NULL value which will be mapped
    // to JDWP_OBJECT_ID_NULL value.

    m_cmdParser->reply.WriteObjectID(jni, jvmClassLoader);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassLoader: send: classLoaderID=%p", jvmClassLoader)); 

    return JDWP_ERROR_NONE;
} // ClassLoaderHandler::Execute()

//------------------------------------------------------------------------------
//ModifiersHandler(3)----------------------------------------------------------

int
ReferenceType::ModifiersHandler::Execute(JNIEnv *jni) 
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Modifiers: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
    jint jvmClassModifiers;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassModifiers(jvmClass,
        &jvmClassModifiers));
    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    m_cmdParser->reply.WriteInt(jvmClassModifiers);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Modifiers: send: modBits=%x", jvmClassModifiers));

    return JDWP_ERROR_NONE;
} // ModifiersHandler::Execute()

//------------------------------------------------------------------------------
//FieldsHandler(4,14)----------------------------------------------------------

int
ReferenceType::FieldsHandler::Execute(JNIEnv *jni) 
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Fields: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
  
    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    jint fieldsCount = 0;
    jfieldID* fields = 0;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassFields(jvmClass, &fieldsCount, &fields));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_CLASS_NOT_PREPARED, JVMTI_ERROR_INVALID_CLASS
        // JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    JvmtiAutoFree autoFreeFields(fields);

    m_cmdParser->reply.WriteInt(fieldsCount);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Fields: fieldCount=%d", fieldsCount));
    for (int i = 0; i < fieldsCount; i++) {
        jfieldID jvmFieldID = fields[i];
        m_cmdParser->reply.WriteFieldID(jni, jvmFieldID);

        char* fieldName = 0;
        char* fieldSignature = 0;
        char* genericSignature = 0;
        char** genericSignaturePtr = 0;
        if ( m_withGeneric ) {
            genericSignaturePtr = &genericSignature;
        }

        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFieldName(jvmClass, jvmFieldID, &fieldName,
            &fieldSignature, genericSignaturePtr));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }

        JvmtiAutoFree autoFreeFieldName(fieldName);
        JvmtiAutoFree autoFreeFieldSignature(fieldSignature);
        JvmtiAutoFree autoFreeGenericSignature(genericSignature);

        m_cmdParser->reply.WriteString(fieldName);
    
        m_cmdParser->reply.WriteString(fieldSignature);

        if ( m_withGeneric ) {
            if (genericSignature != 0) {
                m_cmdParser->reply.WriteString(genericSignature);
            } else {
                m_cmdParser->reply.WriteString("");
            }
        }
       
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

        jint fieldSyntheticFlag = 0xf0000000;
        jboolean isFieldSynthetic;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->IsFieldSynthetic(jvmClass, jvmFieldID,
            &isFieldSynthetic));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY,JVMTI_ERROR_INVALID_CLASS,
            // JVMTI_ERROR_INVALID_FIELDID, JVMTI_ERROR_NULL_POINTER
            if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY) {
                fieldSyntheticFlag = 0;
            } else {
                AgentException e(err);
		        JDWP_SET_EXCEPTION(e);
                return err;
            }
        } else {
            if ( ! isFieldSynthetic ) {
                fieldSyntheticFlag = 0;
            }
        }

        fieldModifiers = fieldModifiers | fieldSyntheticFlag;
        m_cmdParser->reply.WriteInt(fieldModifiers);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Fields: send: field#=%d, fieldsName=%s, fieldSignature=%s, genericSignature=%s, fieldModifiers=%x",
                        i, JDWP_CHECK_NULL(fieldName), JDWP_CHECK_NULL(fieldSignature), JDWP_CHECK_NULL(genericSignature), fieldModifiers));

     } // for (int i = 0; i < fieldsCount; i++)

    return JDWP_ERROR_NONE;
} // FieldsHandler::Execute()

//------------------------------------------------------------------------------
//MethodsHandler(5,15)----------------------------------------------------------

int
ReferenceType::MethodsHandler::Execute(JNIEnv *jni) 
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Methods: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    jint methodsCount = 0;
    jmethodID* methods = 0;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassMethods(jvmClass, &methodsCount, &methods));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_CLASS_NOT_PREPARED, JVMTI_ERROR_INVALID_CLASS,
        // JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    JvmtiAutoFree autoFreeFields(methods);

    m_cmdParser->reply.WriteInt(methodsCount);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Methods: methodsCount=%d", methodsCount));

    for (int i = 0; i < methodsCount; i++) {
        jmethodID jvmMethodID = methods[i];
        m_cmdParser->reply.WriteMethodID(jni, jvmMethodID);

        char* methodName = 0;
        char* methodSignature = 0;
        char* genericSignature = 0;
        char** genericSignaturePtr = 0;
        if ( m_withGeneric ) {
            genericSignaturePtr = &genericSignature;
        }

        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetMethodName(jvmMethodID, &methodName,
            &methodSignature, genericSignaturePtr));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_METHODID
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }
        JvmtiAutoFree autoFreeFieldName(methodName);
        JvmtiAutoFree autoFreeMethodSignature(methodSignature);
        JvmtiAutoFree autoFreeGenericSignature(genericSignature);

        m_cmdParser->reply.WriteString(methodName);
    
        m_cmdParser->reply.WriteString(methodSignature);

        if ( m_withGeneric ) {
            if (genericSignature != 0) {
                m_cmdParser->reply.WriteString(genericSignature);
            } else {
                m_cmdParser->reply.WriteString("");
            }
        }

        jint methodModifiers;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetMethodModifiers(jvmMethodID,
            &methodModifiers));
        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_METHODID, JVMTI_ERROR_NULL_POINTER
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }

        jint methodSyntheticFlag = 0xf0000000;
        jboolean isMethodSynthetic;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->IsMethodSynthetic(jvmMethodID,
            &isMethodSynthetic));

        if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY) {
            methodSyntheticFlag = 0;
        } else {
            if (err != JVMTI_ERROR_NONE) {
               // Can be: JVMTI_ERROR_INVALID_METHODID, JVMTI_ERROR_NULL_POINTER
                AgentException e(err);
		        JDWP_SET_EXCEPTION(e);
                return err;
            }
            if ( ! isMethodSynthetic ) {
                methodSyntheticFlag = 0;
            }
        }

        methodModifiers = methodModifiers | methodSyntheticFlag;
        m_cmdParser->reply.WriteInt(methodModifiers);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Methods: send: method#=%d, methodName=%s, methodSignature=%s, genericSignature=%s, methodModifiers=%x",
                        i, JDWP_CHECK_NULL(methodName), JDWP_CHECK_NULL(methodSignature), JDWP_CHECK_NULL(genericSignature), methodModifiers));

    } // for (int i = 0; i < methodsCount; i++)

    return JDWP_ERROR_NONE;
} // MethodsHandler::Execute()

//------------------------------------------------------------------------------
//GetValuesHandler(6)-----------------------------------------------------------

int
ReferenceType::GetValuesHandler::Execute(JNIEnv *jni) 
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT

    jint fieldsNumber = m_cmdParser->command.ReadInt();
    // Can be: InternalErrorException
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: received: refTypeID=%p, classSignature=%s, fields=%d",
                        jvmClass, JDWP_CHECK_NULL(signature), fieldsNumber));
    }
#endif

    m_cmdParser->reply.WriteInt(fieldsNumber);
    // The number of values returned, always equal to the number of values to get.

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    for (int i = 0; i < fieldsNumber; i++) {
        jfieldID jvmFieldID = m_cmdParser->command.ReadFieldID(jni);
        // Can be: InternalErrorException, OutOfMemoryException

        // check that given field belongs to passed jvmClass
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
            // given field does not belong to passed jvmClass
			AgentException e(JDWP_ERROR_INVALID_FIELDID);
		    JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INVALID_FIELDID;
        }

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

        if ( (fieldModifiers & 0x0008) == 0 ) { // ACC_STATIC_FLAG = 0x0008;
            // given field is not static
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

        jvalue fieldValue;
        jdwpTag fieldValueTag;
        jobject jobj = NULL;
        switch ( fieldSignature[0] ) {
            case 'Z':
                fieldValueTag = JDWP_TAG_BOOLEAN;
                fieldValue.z = jni->GetStaticBooleanField(jvmClass, jvmFieldID);
                break;
            case 'B':
                fieldValueTag = JDWP_TAG_BYTE;
                fieldValue.b = jni->GetStaticByteField(jvmClass, jvmFieldID);
                break;
            case 'C':
                fieldValueTag = JDWP_TAG_CHAR;
                fieldValue.c = jni->GetStaticCharField(jvmClass, jvmFieldID);
                break;
            case 'S':
                fieldValueTag = JDWP_TAG_SHORT;
                fieldValue.s = jni->GetStaticShortField(jvmClass, jvmFieldID);
                break;
            case 'I':
                fieldValueTag = JDWP_TAG_INT;
                fieldValue.i = jni->GetStaticIntField(jvmClass, jvmFieldID);
                break;
            case 'J':
                fieldValueTag = JDWP_TAG_LONG;
                fieldValue.j = jni->GetStaticLongField(jvmClass, jvmFieldID);
                break;
            case 'F':
                fieldValueTag = JDWP_TAG_FLOAT;
                fieldValue.f = jni->GetStaticFloatField(jvmClass, jvmFieldID);
                break;
            case 'D':
                fieldValueTag = JDWP_TAG_DOUBLE;
                fieldValue.d = jni->GetStaticDoubleField(jvmClass, jvmFieldID);
                break;
            case 'L':
            case '[':
                jobj = jni->GetStaticObjectField(jvmClass, jvmFieldID);
                fieldValueTag = AgentBase::GetClassManager().GetJdwpTag(jni, jobj);
                fieldValue.l = jobj;
                break;
            default:
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: unknown field signature: %s", JDWP_CHECK_NULL(fieldSignature)));
                AgentException e(JDWP_ERROR_INTERNAL);
				JDWP_SET_EXCEPTION(e);
                return JDWP_ERROR_INTERNAL;
        }
        
        m_cmdParser->reply.WriteValue(jni, fieldValueTag, fieldValue);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: send: field#=%d, fieldName=%s, fieldSignature=%s, fieldValueTag=%d",
                        i, JDWP_CHECK_NULL(fieldName), JDWP_CHECK_NULL(fieldSignature), fieldValueTag));

    } // for (int i = 0; i < fieldsNumber; i++) {

    return JDWP_ERROR_NONE;
} // GetValuesHandler::Execute()

//------------------------------------------------------------------------------
//SourceFileHandler(7)----------------------------------------------------------

int
ReferenceType::SourceFileHandler::Execute(JNIEnv *jni) 
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SourceFile: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
    char* sourceFileName = 0;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetSourceFileName(jvmClass,
        &sourceFileName));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_ABSENT_INFORMATION,
        // JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    JvmtiAutoFree autoFreeFieldName(sourceFileName);

    m_cmdParser->reply.WriteString(sourceFileName);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SourceFile: send: sourceFile=%s", JDWP_CHECK_NULL(sourceFileName)));

    return JDWP_ERROR_NONE;
} // SourceFileHandler::Execute()

//------------------------------------------------------------------------------
//NestedTypesHandler(8)---------------------------------------------------------

// For reference:
// 1. JDWP spec:
// ReferenceType Command Set (2), NestedTypes Command (8):
// Returns the classes and interfaces directly nested within this type.Types 
// further nested within those types are not included
// 2.
// The Java Language Specification, CHAPTER 8, Classes:
// A nested class is any class whose declaration occurs within the body of
// another class or interface.
// CHAPTER 9, Interfaces:
// A nested interface is any interface whose declaration occurs within the body
// of another class or interface

int
ReferenceType::NestedTypesHandler::Execute(JNIEnv *jni)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "NestedTypes: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
    char* jvmClassSignature = 0;

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassSignature(jvmClass, &jvmClassSignature, 0));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    JvmtiAutoFree autoFreeSignature(jvmClassSignature);
    size_t jvmClassSignatureLength = strlen(jvmClassSignature);

    jint allClassesCount = 0;
    jclass* allClasses = 0;

    AgentBase::GetJniEnv()->PushLocalFrame(100);

    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetLoadedClasses(&allClassesCount, &allClasses));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
        JDWP_SET_EXCEPTION(e);
        return err;
    }
    JvmtiAutoFree autoFreeAllClasses(allClasses);

    /* Since JVMTI doesn't support getting nested classes so here 
     * following algorithm is used:
     * searching among allClasses for classes nested directly in given jvmClass.
     * The criterion is:
     * <nested_class_signature> = 
     * <jvmClassSignature> + '$' + <nested_class_name>
     * but not
     * <jvmClassSignature> + '$' + <nested_class_name> + '$' + ...
     * since according to JDWP specificstion 
     * "Types further nested within those types are not included",
     * and not
     * <jvmClassSignature> + '$' + <digit>
     * since it is anonymous class and reference JDWP implementation
     * doesn't return anonymous classes.
     * But there is one nuance: given algorithm does not take into 
     * account that class name of not-nested class can contain '$' symbol,
     * that is not forbidden by the Java Language Specification.
     * In this case such class with name containing '$' symbol may be returned 
     * by ReferenceType.NestedTypes command too, although this class is not
     * nested class.
    */
    const char nestedClassSign = '$';
    jint nestedTypesCount = 0;
    for (int allClassesIndex = 0; allClassesIndex < allClassesCount; allClassesIndex++) {
        jclass klass = allClasses[allClassesIndex];
        char* klassSignature = 0;

        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassSignature(klass, &klassSignature, 0));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS
            AgentException e(err);
    	    JDWP_SET_EXCEPTION(e);
            return err;
        }
        JvmtiAutoFree autoFreeKlassSignature(klassSignature);

        size_t klassSignatureLength = strlen(klassSignature);
        if ( jvmClassSignatureLength+2 > klassSignatureLength ) {
            // <nested_class_signature> = 
            // <jvmClassSignature> + '$' + <at_least_1_symbol> 
            continue;
        }

        if ( strncmp(klassSignature, jvmClassSignature, jvmClassSignatureLength-1)
                != 0 ) {
            continue;
        }
	// note:
        char* firstCharPtr = strchr((klassSignature + jvmClassSignatureLength - 1), nestedClassSign);
        if ( firstCharPtr == NULL ) {
            // klass is not nested in jvmClass
            continue;
        }
        char* lastCharPtr = strrchr(klassSignature, nestedClassSign);
        if ( firstCharPtr != lastCharPtr ) {
            // klass is nested in jvmClass but NOT directly
            continue;
        }
        firstCharPtr++;
        if ( isdigit(*firstCharPtr) ) {
            // it is anonymous class  - ignore it
            continue;
        }
        // klass is directly nested in jvmClass - it is desired nested class
        allClasses[nestedTypesCount] = klass;
        nestedTypesCount++;
    }

    // form reply data for all found out classes nested directly in given jvmClass
    m_cmdParser->reply.WriteInt(nestedTypesCount);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "NestedTypes: nestedTypes=%d", nestedTypesCount));
    for (int nestedClassesIndex = 0; nestedClassesIndex < nestedTypesCount; nestedClassesIndex++) {
        jclass nestedClass = allClasses[nestedClassesIndex];

        jdwpTypeTag refTypeTag = JDWP_TYPE_TAG_CLASS;
        if (GetClassManager().IsInterfaceType(nestedClass) == JNI_TRUE ) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
            refTypeTag = JDWP_TYPE_TAG_INTERFACE;
        }
        m_cmdParser->reply.WriteByte((jbyte)refTypeTag);
        m_cmdParser->reply.WriteReferenceTypeID(jni, nestedClass);
        // can be: OutOfMemoryException, InternalErrorException,
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiError err;
            char* signature = 0;
            JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassSignature(nestedClass, &signature, 0));
            JvmtiAutoFree afcs(signature);
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "NestedTypes: send: nestedClass#%d, typeTag=%d, nestedClassID=%p, signature=%s",
                            nestedClassesIndex, refTypeTag, nestedClass, JDWP_CHECK_NULL(signature)));
        }
#endif
    }

    AgentBase::GetJniEnv()->PopLocalFrame(NULL);

    return JDWP_ERROR_NONE;
} // NestedTypesHandler::Execute()

//------------------------------------------------------------------------------
//StatusHandler(9)--------------------------------------------------------------

int
ReferenceType::StatusHandler::Execute(JNIEnv *jni) 
{
    jint status;

    jclass klass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(klass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Status: received: refTypeID=%p, classSignature=%s", klass, JDWP_CHECK_NULL(signature)));
    }
#endif

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassStatus(klass, &status));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    if (status == JVMTI_CLASS_STATUS_ARRAY) {
       status = 0;
    } else {
        if ( status == JVMTI_CLASS_STATUS_PRIMITIVE ) {
            status = 0;
        }
    }
    m_cmdParser->reply.WriteInt(status);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Status: send: status=%d", status));

    return JDWP_ERROR_NONE;
} // StatusHandler::Execute()

//------------------------------------------------------------------------------
//InterfacesHandler(10)---------------------------------------------------------

int
ReferenceType::InterfacesHandler::Execute(JNIEnv *jni)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Interfaces: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
    jint interfacesCount = 0;
    jclass* interfaces;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetImplementedInterfaces(jvmClass,
        &interfacesCount, &interfaces));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_CLASS_NOT_PREPARED, JVMTI_ERROR_INVALID_CLASS,
        // JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    JvmtiAutoFree autoFreeInterfaces(interfaces);

    m_cmdParser->reply.WriteInt(interfacesCount);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Interfaces: interfaces=%d", interfacesCount));
    for (int i = 0; i < interfacesCount; i++) {
        m_cmdParser->reply.WriteReferenceTypeID(jni, interfaces[i]);
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiError err;
            char* signature = 0;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(interfaces[i], &signature, 0));
            JvmtiAutoFree afcs(signature);
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Interfaces: interface#%d, interfaceID=%p, classSignature=%s",
                            i, interfaces[i], JDWP_CHECK_NULL(signature)));
        }
#endif
    }

    return JDWP_ERROR_NONE;
} // InterfacesHandler::Execute()

//------------------------------------------------------------------------------
//ClassObjectHandler(11)--------------------------------------------------------

int
ReferenceType::ClassObjectHandler::Execute(JNIEnv *jni)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassObject: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
    m_cmdParser->reply.WriteObjectID(jni, jvmClass);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassObject: send: objectID=%p", jvmClass));
    
    return JDWP_ERROR_NONE;
} // ClassObjectHandler::Execute()

//------------------------------------------------------------------------------
//SourceDebugExtensionHandler(12)-----------------------------------------------

int
ReferenceType::SourceDebugExtensionHandler::Execute(JNIEnv *jni)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SourceDebugExtension: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
    char* sourceDebugExtension = 0;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetSourceDebugExtension(jvmClass,
        &sourceDebugExtension));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY,JVMTI_ERROR_ABSENT_INFORMATION,
        // JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    JvmtiAutoFree autoFreeDebugExtension(sourceDebugExtension);

    m_cmdParser->reply.WriteString(sourceDebugExtension);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SourceDebugExtension: send: sourceDebugExtension=%s", JDWP_CHECK_NULL(sourceDebugExtension)));

    return JDWP_ERROR_NONE;
} // SourceDebugExtensionHandler::Execute()

// New commands for Java 6

//------------------------------------------------------------------------------
// InstancesHandler(16)-----------------------------------------------

int
ReferenceType::InstancesHandler::Execute(JNIEnv *jni)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Instances: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
    jint maxInstances = m_cmdParser->command.ReadInt();
    if(maxInstances < 0) {
        AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_ILLEGAL_ARGUMENT;		
    }

    jvmtiHeapCallbacks hcbs;
    memset(&hcbs, 0, sizeof(hcbs));
    hcbs.heap_iteration_callback = NULL;
    hcbs.heap_reference_callback = &HeapReferenceCallback;
    hcbs.primitive_field_callback = NULL;
    hcbs.array_primitive_value_callback = NULL;
    hcbs.string_primitive_value_callback = NULL;

    jvmtiError err;
    //This tag is used to mark instance that is reachable for garbage collection purpose
    const jlong tag_value = 0xfffff;

    //It initiates a traversal over the objects that are directly and indirectly reachable from the heap roots.
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->FollowReferences(0, jvmClass,  NULL,
         &hcbs, &tag_value));
     if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_INVALID_CLASS
        // JVMTI_ERROR_INVALID_OBJECT, JVMTI_ERROR_NULL_POINTER 
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
     }

     const jlong tags[1] = {tag_value};
     jint reachableInstancesNum = 0;
     jobject * pResultObjects = 0;
     // Return the instances that have been marked expectd tag_value tag.
     JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetObjectsWithTags(1, tags, &reachableInstancesNum,
            &pResultObjects, NULL));
     JvmtiAutoFree afResultObjects(pResultObjects);

     if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_ILLEGAL_ARGUMENT 
        // JVMTI_ERROR_ILLEGAL_ARGUMENT, JVMTI_ERROR_NULL_POINTER  
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
     }

     jint returnInstancesNum;
     //If maxInstances is zero, all instances are returned.
     if(0 == maxInstances) {
        returnInstancesNum = reachableInstancesNum;
     }
     else if(maxInstances < reachableInstancesNum) {
        returnInstancesNum = maxInstances;
     }
     else {
        returnInstancesNum = reachableInstancesNum;
     }

     // Compose reply package
     m_cmdParser->reply.WriteInt(returnInstancesNum);
     JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Instances: return instances number: %d", returnInstancesNum));

     for(int i = 0; i < returnInstancesNum; i++) {
          m_cmdParser->reply.WriteTaggedObjectID(jni, pResultObjects[i]);
          JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetTag(pResultObjects[i], 0));
          jni->DeleteLocalRef(pResultObjects[i]);
          if (err != JVMTI_ERROR_NONE) {
              AgentException e(err);
              JDWP_SET_EXCEPTION(e);
              return err;
          }
     }
     JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Instances: tagged-objectID returned."));

     // Tags for those instances which are not returned should also be set back to 0
	 if(returnInstancesNum < reachableInstancesNum) {
	 	for(int i = returnInstancesNum; i < reachableInstancesNum; i++) {
          JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetTag(pResultObjects[i], 0));
          jni->DeleteLocalRef(pResultObjects[i]);
          if (err != JVMTI_ERROR_NONE) {
              AgentException e(err);
              JDWP_SET_EXCEPTION(e);
              return err;
          }
        }
	 }

     return JDWP_ERROR_NONE;
}

//------------------------------------------------------------------------------
// ClassFileVersionHandler(17)-----------------------------------------------

int
ReferenceType::ClassFileVersionHandler::Execute(JNIEnv *jni)
{
     jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassFileVersion: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
    
    jint minorVersion = -1;
    jint majorVersion = -1;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassVersionNumbers(jvmClass, &minorVersion, &majorVersion));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_ABSENT_INFORMATION, JVMTI_ERROR_INVALID_CLASS, 
        // JVMTI_ERROR_NULL_POINTER
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    m_cmdParser->reply.WriteInt(majorVersion);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassFileVersion: send: majorVersion=%d", majorVersion));
     
    m_cmdParser->reply.WriteInt(minorVersion);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassFileVersion: send: minorVersion=%d", minorVersion));

    return JDWP_ERROR_NONE;
}

//------------------------------------------------------------------------------
// ConstantPoolHandler(18)-----------------------------------------------

int
ReferenceType::ConstantPoolHandler::Execute(JNIEnv *jni)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ConstantPool: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
    }
#endif
    jvmtiError err;
    jint count = 0;
    jint bytes = 0;
    unsigned char* cpbytes = 0;
    // Return the raw bytes of the constant pool 
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetConstantPool(jvmClass, &count, &bytes, &cpbytes));
    JvmtiAutoFree afCpbytes(cpbytes);

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_ABSENT_INFORMATION
        // JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_OBJECT
        // JVMTI_ERROR_NULL_POINTER 
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
     }

    m_cmdParser->reply.WriteInt(count);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ConstantPool: send: count=%d", count));

    m_cmdParser->reply.WriteInt(bytes);
     JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ConstantPool: send: bytes=%d", bytes));

    for (int i = 0; i < bytes; i++) {
        m_cmdParser->reply.WriteByte(cpbytes[i]);
    }

    return JDWP_ERROR_NONE;
}
