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
#include "Method.h"
#include "PacketParser.h"
#include "ExceptionManager.h"


using namespace jdwp;
using namespace Method;

int
Method::LineTableHandler::Execute(JNIEnv *jni) 
{
    jvmtiError err;
    jclass refType = m_cmdParser->command.ReadReferenceTypeID(jni);
    jmethodID methodID = m_cmdParser->command.ReadMethodID(jni);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(refType, &classSignature, 0));
        JvmtiAutoFree afs(classSignature);
        char* methodName = 0;
        char* methodSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, &methodSignature, 0));
        JvmtiAutoFree afmn(methodName);
        JvmtiAutoFree afms(methodSignature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "LineTable: received: methodName=%s, methodSignature=%s, classSignature=%s", 
                        methodName, JDWP_CHECK_NULL(methodSignature), JDWP_CHECK_NULL(classSignature)));
    }
#endif

    jboolean isNative;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->IsMethodNative(methodID, &isNative));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    if (isNative == JNI_TRUE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "LineTable: native method"));
        AgentException e(JDWP_ERROR_NATIVE_METHOD);
        JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_NATIVE_METHOD;
    }
    
    jlocation start_location;
    jlocation end_location;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodLocation(methodID,
        &start_location, &end_location));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }    
    jint entry_count = 0;
    jvmtiLineNumberEntry* table = 0;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetLineNumberTable(methodID,
        &entry_count, &table));
    JvmtiAutoFree afv(table);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY ||
        err == JVMTI_ERROR_ABSENT_INFORMATION)
    {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "LineTable: send: tableStart=%lld, tableEnd=%lld, entry_count=0 (no info)", start_location, end_location));

        m_cmdParser->reply.WriteLong(start_location);
        m_cmdParser->reply.WriteLong(end_location);
        m_cmdParser->reply.WriteInt(0);
        
    } else if (err == JVMTI_ERROR_NONE) {

        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "LineTable: send: tableStart=%lld, tableEnd=%lld, entry_count=%d", start_location, end_location, entry_count));

        m_cmdParser->reply.WriteLong(start_location);
        m_cmdParser->reply.WriteLong(end_location);
        m_cmdParser->reply.WriteInt(entry_count);

        for (int i = 0; i < entry_count; i++) {
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "LineTable: send: entry#=%d, lineCodeIndex=%lld, lineCodeNumber=%d", 
                            i, table[i].start_location, table[i].line_number));
            m_cmdParser->reply.WriteLong(table[i].start_location);
            m_cmdParser->reply.WriteInt(table[i].line_number);
              
        }
    } else {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    return JDWP_ERROR_NONE;
}

int
Method::VariableTableHandler::Execute(JNIEnv *jni) 
{
    jclass refType = m_cmdParser->command.ReadReferenceTypeID(jni);
    jmethodID methodID = m_cmdParser->command.ReadMethodID(jni);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(refType, &classSignature, 0));
        JvmtiAutoFree afcs(classSignature);
        char* methodName = 0;
        char* methodSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, &methodSignature, 0));
        JvmtiAutoFree afmn(methodName);
        JvmtiAutoFree afms(methodSignature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "VariableTable: received: methodName=%s, methodSignature=%s, classSignature=%s", 
                        methodName, JDWP_CHECK_NULL(methodSignature), JDWP_CHECK_NULL(classSignature)));
    }
#endif

    jboolean isNative;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->IsMethodNative(methodID, &isNative));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    if (isNative == JNI_TRUE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "VariableTable: native method"));
        AgentException e(JDWP_ERROR_NATIVE_METHOD);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_NATIVE_METHOD;
    }

    jint size;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetArgumentsSize(methodID, &size));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    m_cmdParser->reply.WriteInt(size);

    jint entry_count;
    jvmtiLocalVariableEntry* table = 0;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetLocalVariableTable(methodID,
        &entry_count, &table));
    JvmtiAutoFree afv(table);
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    
#ifndef NDEBUG    
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jlocation start_location;
        jlocation end_location;
        GetJvmtiEnv()->GetMethodLocation(methodID,
            &start_location, &end_location);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "VariableTable: methodStart=%lld, methodEnd=%lld, entry_count=%d", start_location, end_location, entry_count));
    }
#endif
    
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "VariableTable: send: argSize=%d, entry_count=%d", size, entry_count));
    m_cmdParser->reply.WriteInt(entry_count);

    for (int i = 0; i < entry_count; i++) {

        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "VariableTable: send: entry#=%d, codeIndex=%lld, name=%s, signature=%s, length=%d, slot=%d",
                        i, table[i].start_location, table[i].name, table[i].signature, table[i].length, table[i].slot));

        m_cmdParser->reply.WriteLong(table[i].start_location);
        m_cmdParser->reply.WriteString(table[i].name);
        m_cmdParser->reply.WriteString(table[i].signature);
        m_cmdParser->reply.WriteInt(table[i].length);
        m_cmdParser->reply.WriteInt(table[i].slot);

        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].name)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].signature)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].generic_signature)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
    }

    return JDWP_ERROR_NONE;
}

int
Method::BytecodesHandler::Execute(JNIEnv *jni) 
{
    jclass refType = m_cmdParser->command.ReadReferenceTypeID(jni);
    jmethodID methodID = m_cmdParser->command.ReadMethodID(jni);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(refType, &classSignature, 0));
        JvmtiAutoFree afcs(classSignature);
        char* methodName = 0;
        char* methodSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, &methodSignature, 0));
        JvmtiAutoFree afmn(methodName);
        JvmtiAutoFree afms(methodSignature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Bytecodes: received: methodName=%s, methodSignature=%s, classSignature=%s", 
                        methodName, JDWP_CHECK_NULL(methodSignature), JDWP_CHECK_NULL(classSignature)));
    }
#endif
    jint bytecode_count;
    unsigned char* bytecodes = 0;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetBytecodes(methodID,
        &bytecode_count, &bytecodes));
    JvmtiAutoFree afv(bytecodes);
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Bytecodes: send: bytecode_count=%d", bytecode_count));
    m_cmdParser->reply.WriteByteArray(reinterpret_cast<jbyte*>(bytecodes), bytecode_count);

    return JDWP_ERROR_NONE;
}

int
Method::IsObsoleteHandler::Execute(JNIEnv *jni) 
{
    jclass refType = m_cmdParser->command.ReadReferenceTypeID(jni);
    jmethodID methodID = m_cmdParser->command.ReadMethodID(jni);

    // when the methodID is 0, it means the method is obsolete.
    if (0 == methodID) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsObsolete: send: is_obsolete=TRUE"));
        m_cmdParser->reply.WriteBoolean(JNI_TRUE);
        return JDWP_ERROR_NONE;
	}

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(refType, &classSignature, 0));
        JvmtiAutoFree afcs(classSignature);
        char* methodName = 0;
        char* methodSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, &methodSignature, 0));
        JvmtiAutoFree afmn(methodName);
        JvmtiAutoFree afms(methodSignature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsObsolete: received: methodName=%s, methodSignature=%s, classSignature=%s", 
                        methodName, JDWP_CHECK_NULL(methodSignature), JDWP_CHECK_NULL(classSignature)));
    }
#endif
    jboolean is_obsolete = JNI_FALSE;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->IsMethodObsolete(methodID, &is_obsolete));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsObsolete: send: is_obsolete=%s", (is_obsolete ? "TRUE" : "FALSE")));
    m_cmdParser->reply.WriteBoolean(is_obsolete);

    return JDWP_ERROR_NONE;
}

int
Method::VariableTableWithGenericHandler::Execute(JNIEnv *jni)
{
    jclass refType = m_cmdParser->command.ReadReferenceTypeID(jni);
    jmethodID methodID = m_cmdParser->command.ReadMethodID(jni);
#ifndef NDEBUG    
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(refType, &classSignature, 0));
        JvmtiAutoFree afcs(classSignature);      
        char* methodName = 0;
        char* methodSignature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, &methodSignature, 0));
        JvmtiAutoFree afmn(methodName);
        JvmtiAutoFree afms(methodSignature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "VariableTableWithGeneric: received: methodName=%s, methodSignature=%s, classSignature=%s", 
                        methodName, JDWP_CHECK_NULL(methodSignature), JDWP_CHECK_NULL(classSignature)));
    }
#endif
    jint size;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetArgumentsSize(methodID, &size));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    m_cmdParser->reply.WriteInt(size);

    jint entry_count;
    jvmtiLocalVariableEntry* table = 0;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetLocalVariableTable(methodID,
        &entry_count, &table));
    JvmtiAutoFree afv(table);
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jlocation start_location;
        jlocation end_location;
        GetJvmtiEnv()->GetMethodLocation(methodID,
            &start_location, &end_location);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "VariableTableWithGeneric: methodStart=%lld, methodEnd=%lld, entry_count=%d",
                        start_location, end_location, entry_count));
    } 
#endif
    
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "VariableTableWithGeneric: send: argSize=%d, entry_count=%d", size, entry_count));
    m_cmdParser->reply.WriteInt(entry_count);

    for (int i = 0; i < entry_count; i++) {

        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "VariableTableWithGeneric: send: entry#=%d, codeIndex=%lld, name=%s, signature=%s, length=%d, slot=%d",
                        i, table[i].start_location, table[i].name, table[i].signature, table[i].length, table[i].slot));
        
        m_cmdParser->reply.WriteLong(table[i].start_location);
        m_cmdParser->reply.WriteString(table[i].name);
        m_cmdParser->reply.WriteString(table[i].signature);
        m_cmdParser->reply.WriteString(table[i].generic_signature);
        m_cmdParser->reply.WriteInt(table[i].length);
        m_cmdParser->reply.WriteInt(table[i].slot);

        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].name)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].signature)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].generic_signature)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
    }

    return JDWP_ERROR_NONE;
}
