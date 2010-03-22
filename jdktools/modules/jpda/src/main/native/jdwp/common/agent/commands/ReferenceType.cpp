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
 * @author Anatoly F. Bondarenko
 */
#include "ReferenceType.h"
#include "PacketParser.h"
#include "ClassManager.h"
#include <cstring>

using namespace jdwp;
using namespace ReferenceType;

//------------------------------------------------------------------------------
//SignatureFileHandler(1)----------------------------------------------------------

void
ReferenceType::SignatureHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
    JDWP_TRACE_DATA("Signature: received: refTypeID=" << jvmClass);

    char* classSignature = 0;
    char* classGenericSignature = 0;
    char** genericSignaturePtr = 0;

    if ( m_withGeneric ) {
        genericSignaturePtr = &classGenericSignature;
    }

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass,
        &classSignature, genericSignaturePtr));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS
        throw AgentException(err);
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
    JDWP_TRACE_DATA("Signature: send: classSignature=" << JDWP_CHECK_NULL(classSignature) 
        << ", classGenericSignature=" << JDWP_CHECK_NULL(classGenericSignature));

} // SignatureHandler::Execute()

//------------------------------------------------------------------------------
//ClassLoaderHandler(2)----------------------------------------------------------

void
ReferenceType::ClassLoaderHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("ClassLoader: received: refTypeID=" << jvmClass
            << ", classSignature=" << JDWP_CHECK_NULL(signature));
    }
#endif

    jobject jvmClassLoader;

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetClassLoader(jvmClass, &jvmClassLoader));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
    }
    // if GetClassLoader() returns NULL value for jvmClassLoader
    // consider it as the class loader for the jvmClass is the system class
    // loader and write in reply NULL value which will be mapped
    // to JDWP_OBJECT_ID_NULL value.

    m_cmdParser->reply.WriteObjectID(jni, jvmClassLoader);
    JDWP_TRACE_DATA("ClassLoader: send: classLoaderID=" << jvmClassLoader);  

} // ClassLoaderHandler::Execute()

//------------------------------------------------------------------------------
//ModifiersHandler(3)----------------------------------------------------------

void
ReferenceType::ModifiersHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("Modifiers: received: refTypeID=" << jvmClass
            << ", classSignature=" << JDWP_CHECK_NULL(signature));
    }
#endif
    jint jvmClassModifiers;

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetClassModifiers(jvmClass,
        &jvmClassModifiers));
    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
    }

    m_cmdParser->reply.WriteInt(jvmClassModifiers);
    JDWP_TRACE_DATA("Modifiers: send: modBits=" << hex << jvmClassModifiers); 

} // ModifiersHandler::Execute()

//------------------------------------------------------------------------------
//FieldsHandler(4,14)----------------------------------------------------------

void
ReferenceType::FieldsHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("Fields: received: refTypeID=" << jvmClass
            << ", classSignature=" << JDWP_CHECK_NULL(signature));
    }
#endif
  
    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    jint fieldsCount = 0;
    jfieldID* fields = 0;
    jvmtiError err;
    JVMTI_TRACE(err, jvmti->GetClassFields(jvmClass, &fieldsCount, &fields));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_CLASS_NOT_PREPARED, JVMTI_ERROR_INVALID_CLASS
        // JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
    }
    JvmtiAutoFree autoFreeFields(fields);

    m_cmdParser->reply.WriteInt(fieldsCount);
    JDWP_TRACE_DATA("Fields: fieldCount=" << fieldsCount);
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

        JVMTI_TRACE(err, jvmti->GetFieldName(jvmClass, jvmFieldID, &fieldName,
            &fieldSignature, genericSignaturePtr));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID
            throw AgentException(err);
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
        JVMTI_TRACE(err, jvmti->GetFieldModifiers(jvmClass, jvmFieldID,
            &fieldModifiers));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID,
            // JVMTI_ERROR_NULL_POINTER
            throw AgentException(err);
        }

        jint fieldSyntheticFlag = 0xf0000000;
        jboolean isFieldSynthetic;
        JVMTI_TRACE(err, jvmti->IsFieldSynthetic(jvmClass, jvmFieldID,
            &isFieldSynthetic));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY,JVMTI_ERROR_INVALID_CLASS,
            // JVMTI_ERROR_INVALID_FIELDID, JVMTI_ERROR_NULL_POINTER
            if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY) {
                fieldSyntheticFlag = 0;
            } else {
                throw AgentException(err);
            }
        } else {
            if ( ! isFieldSynthetic ) {
                fieldSyntheticFlag = 0;
            }
        }

        fieldModifiers = fieldModifiers | fieldSyntheticFlag;
        m_cmdParser->reply.WriteInt(fieldModifiers);
        JDWP_TRACE_DATA("Fields: send: field#=" << i 
            << ", fieldsName=" << JDWP_CHECK_NULL(fieldName) 
            << ", fieldSignature=" << JDWP_CHECK_NULL(fieldSignature) 
            << ", genericSignature=" << JDWP_CHECK_NULL(genericSignature) 
            << ", fieldModifiers=" << hex << fieldModifiers);         

     } // for (int i = 0; i < fieldsCount; i++)

} // FieldsHandler::Execute()

//------------------------------------------------------------------------------
//MethodsHandler(5,15)----------------------------------------------------------

void
ReferenceType::MethodsHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("Methods: received: refTypeID=" << jvmClass
            << ", classSignature=" << JDWP_CHECK_NULL(signature));  
    }
#endif
    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    jint methodsCount = 0;
    jmethodID* methods = 0;
    jvmtiError err;
    JVMTI_TRACE(err, jvmti->GetClassMethods(jvmClass, &methodsCount, &methods));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_CLASS_NOT_PREPARED, JVMTI_ERROR_INVALID_CLASS,
        // JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
    }
    JvmtiAutoFree autoFreeFields(methods);

    m_cmdParser->reply.WriteInt(methodsCount);
    JDWP_TRACE_DATA("Methods: methodCount=" << methodsCount);

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

        JVMTI_TRACE(err, jvmti->GetMethodName(jvmMethodID, &methodName,
            &methodSignature, genericSignaturePtr));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_METHODID
            throw AgentException(err);
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
        JVMTI_TRACE(err, jvmti->GetMethodModifiers(jvmMethodID,
            &methodModifiers));
        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_METHODID, JVMTI_ERROR_NULL_POINTER
            throw AgentException(err);
        }

        jint methodSyntheticFlag = 0xf0000000;
        jboolean isMethodSynthetic;
        JVMTI_TRACE(err, jvmti->IsMethodSynthetic(jvmMethodID,
            &isMethodSynthetic));

        if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY) {
            methodSyntheticFlag = 0;
        } else {
            if (err != JVMTI_ERROR_NONE) {
               // Can be: JVMTI_ERROR_INVALID_METHODID, JVMTI_ERROR_NULL_POINTER
                throw AgentException(err);
            }
            if ( ! isMethodSynthetic ) {
                methodSyntheticFlag = 0;
            }
        }

        methodModifiers = methodModifiers | methodSyntheticFlag;
        m_cmdParser->reply.WriteInt(methodModifiers);
        JDWP_TRACE_DATA("Methods: send: method#="<< i 
            << ", methodName=" << JDWP_CHECK_NULL(methodName) 
            << ", methodSignature=" << JDWP_CHECK_NULL(methodSignature) 
            << ", genericSignature=" << JDWP_CHECK_NULL(genericSignature) 
            << ", methodModifiers=" << hex << methodModifiers);         

    } // for (int i = 0; i < methodsCount; i++)

} // MethodsHandler::Execute()

//------------------------------------------------------------------------------
//GetValuesHandler(6)-----------------------------------------------------------

void
ReferenceType::GetValuesHandler::Execute(JNIEnv *jni) throw (AgentException)
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
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("GetValues: received: refTypeID=" << jvmClass
            << ", classSignature=" << JDWP_CHECK_NULL(signature) 
            << ", fields=" << fieldsNumber);
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
        JVMTI_TRACE(err, jvmti->GetFieldDeclaringClass(jvmClass, jvmFieldID,
            &declaringClass));

        if (err != JVMTI_ERROR_NONE) {
            throw AgentException(err);
        }

        if ( jni->IsAssignableFrom(jvmClass, declaringClass) == JNI_FALSE ) {
            // given field does not belong to passed jvmClass
            throw AgentException(JDWP_ERROR_INVALID_FIELDID);
        }

        jint fieldModifiers;
        JVMTI_TRACE(err, jvmti->GetFieldModifiers(jvmClass, jvmFieldID,
            &fieldModifiers));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_INVALID_FIELDID,
            // JVMTI_ERROR_NULL_POINTER
            throw AgentException(err);
        }

        if ( (fieldModifiers & 0x0008) == 0 ) { // ACC_STATIC_FLAG = 0x0008;
            // given field is not static
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
                JDWP_TRACE_DATA("GetValues: unknown field signature: " 
                    << JDWP_CHECK_NULL(fieldSignature));
                throw InternalErrorException();
        }
        
        m_cmdParser->reply.WriteValue(jni, fieldValueTag, fieldValue);
        JDWP_TRACE_DATA("GetValues: send: field#=" << i 
            << ", fieldName=" << JDWP_CHECK_NULL(fieldName) 
            << ", fieldSignature=" << JDWP_CHECK_NULL(fieldSignature) 
            << ", fieldValueTag=" << fieldValueTag);         

    } // for (int i = 0; i < fieldsNumber; i++) {

} // GetValuesHandler::Execute()

//------------------------------------------------------------------------------
//SourceFileHandler(7)----------------------------------------------------------

void
ReferenceType::SourceFileHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("SourceFile: received: refTypeID=" << jvmClass
            << ", classSignature=" << JDWP_CHECK_NULL(signature));
    }
#endif
    char* sourceFileName = 0;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetSourceFileName(jvmClass,
        &sourceFileName));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_ABSENT_INFORMATION,
        // JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
    }
    JvmtiAutoFree autoFreeFieldName(sourceFileName);

    m_cmdParser->reply.WriteString(sourceFileName);
    JDWP_TRACE_DATA("SourceFile: send: sourceFile=" << JDWP_CHECK_NULL(sourceFileName));

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

void
ReferenceType::NestedTypesHandler::Execute(JNIEnv *jni)
        throw (AgentException)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("NestedTypes: received: refTypeID=" << jvmClass
            << ", classSignature=" << JDWP_CHECK_NULL(signature));
    }
#endif
    char* jvmClassSignature = 0;

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    jvmtiError err;
    JVMTI_TRACE(err, jvmti->GetClassSignature(jvmClass, &jvmClassSignature, 0));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS
        throw AgentException(err);
    }
    JvmtiAutoFree autoFreeSignature(jvmClassSignature);
    size_t jvmClassSignatureLength = strlen(jvmClassSignature);

    jint allClassesCount = 0;
    jclass* allClasses = 0;
    JVMTI_TRACE(err, jvmti->GetLoadedClasses(&allClassesCount, &allClasses));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
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

        JVMTI_TRACE(err, jvmti->GetClassSignature(klass, &klassSignature, 0));

        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_INVALID_CLASS
            throw AgentException(err);
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
        char* firstCharPtr = strchr(klassSignature, nestedClassSign);
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
    JDWP_TRACE_DATA("NestedTypes: nestedTypes=" << nestedTypesCount);
    for (int nestedClassesIndex = 0; nestedClassesIndex < nestedTypesCount; nestedClassesIndex++) {
        jclass nestedClass = allClasses[nestedClassesIndex];

        jdwpTypeTag refTypeTag = JDWP_TYPE_TAG_CLASS;
        if (GetClassManager().IsInterfaceType(nestedClass) == JNI_TRUE ) {
            // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
            refTypeTag = JDWP_TYPE_TAG_INTERFACE;
        }
        m_cmdParser->reply.WriteByte(refTypeTag);
        m_cmdParser->reply.WriteReferenceTypeID(jni, nestedClass);
        // can be: OutOfMemoryException, InternalErrorException,
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiError err;
            char* signature = 0;
            JVMTI_TRACE(err, jvmti->GetClassSignature(nestedClass, &signature, 0));
            JvmtiAutoFree afcs(signature);
            JDWP_TRACE_DATA("NestedTypes: send: nestedClass#" << nestedClassesIndex
                << ", typeTag=" << refTypeTag
                << ", nestedClassID=" << nestedClass
                << ", signature=" << JDWP_CHECK_NULL(signature));
        }
#endif
    }

} // NestedTypesHandler::Execute()

//------------------------------------------------------------------------------
//StatusHandler(9)--------------------------------------------------------------

void
ReferenceType::StatusHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jint status;

    jclass klass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(klass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("Status: received: refTypeID=" << klass
            << ", classSignature=" << JDWP_CHECK_NULL(signature));
    }
#endif

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetClassStatus(klass, &status));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
    }

    if (status == JVMTI_CLASS_STATUS_ARRAY) {
       status = 0;
    } else {
        if ( status == JVMTI_CLASS_STATUS_PRIMITIVE ) {
            status = 0;
        }
    }
    m_cmdParser->reply.WriteInt(status);
    JDWP_TRACE_DATA("Status: send: status=" << status);

} // StatusHandler::Execute()

//------------------------------------------------------------------------------
//InterfacesHandler(10)---------------------------------------------------------

void
ReferenceType::InterfacesHandler::Execute(JNIEnv *jni)
        throw (AgentException)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("Interfaces: received: refTypeID=" << jvmClass
            << ", classSignature=" << JDWP_CHECK_NULL(signature));
    }
#endif
    jint interfacesCount = 0;
    jclass* interfaces;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetImplementedInterfaces(jvmClass,
        &interfacesCount, &interfaces));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_CLASS_NOT_PREPARED, JVMTI_ERROR_INVALID_CLASS,
        // JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
    }
    JvmtiAutoFree autoFreeInterfaces(interfaces);

    m_cmdParser->reply.WriteInt(interfacesCount);

    JDWP_TRACE_DATA("Interfaces: interfaces=" << interfacesCount);
    for (int i = 0; i < interfacesCount; i++) {
        m_cmdParser->reply.WriteReferenceTypeID(jni, interfaces[i]);
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiError err;
            char* signature = 0;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(interfaces[i], &signature, 0));
            JvmtiAutoFree afcs(signature);
            JDWP_TRACE_DATA("Interfaces: interface#" << i
                << ", interfaceID=" << interfaces[i]
                << ", classSignature=" << JDWP_CHECK_NULL(signature));
        }
#endif
    }

} // InterfacesHandler::Execute()

//------------------------------------------------------------------------------
//ClassObjectHandler(11)--------------------------------------------------------

void
ReferenceType::ClassObjectHandler::Execute(JNIEnv *jni)
        throw (AgentException)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("ClassObject: refTypeID=" << jvmClass
            << ", classSignature=" << JDWP_CHECK_NULL(signature))
    }
#endif
    m_cmdParser->reply.WriteObjectID(jni, jvmClass);
    JDWP_TRACE_DATA("ClassObject: send: objectID=" << jvmClass);
    

} // ClassObjectHandler::Execute()

//------------------------------------------------------------------------------
//SourceDebugExtensionHandler(12)-----------------------------------------------

void
ReferenceType::SourceDebugExtensionHandler::Execute(JNIEnv *jni)
        throw (AgentException)
{
    jclass jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
    // Can be: InternalErrorException, OutOfMemoryException,
    // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
        JvmtiAutoFree afcs(signature);
        JDWP_TRACE_DATA("SourceDebugExtension: received: refTypeID=" << jvmClass
            << ", classSignature=" << JDWP_CHECK_NULL(signature));
    }
#endif
    char* sourceDebugExtension = 0;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetSourceDebugExtension(jvmClass,
        &sourceDebugExtension));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY,JVMTI_ERROR_ABSENT_INFORMATION,
        // JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        throw AgentException(err);
    }
    JvmtiAutoFree autoFreeDebugExtension(sourceDebugExtension);

    m_cmdParser->reply.WriteString(sourceDebugExtension);
    JDWP_TRACE_DATA("SourceDebugExtension: send: sourceDebugExtension=" 
        << JDWP_CHECK_NULL(sourceDebugExtension));

} // SourceDebugExtensionHandler::Execute()

//------------------------------------------------------------------------------
