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
 * @author Viacheslav G. Rybalov
 */
#include "Method.h"
#include "PacketParser.h"

using namespace jdwp;
using namespace Method;

void
Method::LineTableHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jvmtiError err;
    jclass refType = m_cmdParser->command.ReadReferenceTypeID(jni);
    jmethodID methodID = m_cmdParser->command.ReadMethodID(jni);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(refType, &classSignature, 0));
        JvmtiAutoFree afs(classSignature);
        char* methodName = 0;
        char* methodSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, &methodSignature, 0));
        JvmtiAutoFree afmn(methodName);
        JvmtiAutoFree afms(methodSignature);
        JDWP_TRACE_DATA("LineTable: received: methodName=" << methodName
            << ", methodSignature=" << JDWP_CHECK_NULL(methodSignature)
            << ", classSignature=" << JDWP_CHECK_NULL(classSignature));
    }
#endif

    jboolean isNative;
    JVMTI_TRACE(err, GetJvmtiEnv()->IsMethodNative(methodID, &isNative));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }

    if (isNative == JNI_TRUE) {
        JDWP_TRACE_DATA("LineTable: native method");
        throw AgentException(JDWP_ERROR_NATIVE_METHOD);
    }
    
    jlocation start_location;
    jlocation end_location;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodLocation(methodID,
        &start_location, &end_location));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }    
    jint entry_count = 0;
    jvmtiLineNumberEntry* table = 0;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetLineNumberTable(methodID,
        &entry_count, &table));
    JvmtiAutoFree afv(table);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY ||
        err == JVMTI_ERROR_ABSENT_INFORMATION)
    {
        JDWP_TRACE_DATA("LineTable: send: tableStart=" << start_location
            << ", tableEnd=" << end_location 
            << ", entry_count=0 (no info)"); 

        m_cmdParser->reply.WriteLong(start_location);
        m_cmdParser->reply.WriteLong(end_location);
        m_cmdParser->reply.WriteInt(0);
        
    } else if (err == JVMTI_ERROR_NONE) {

        JDWP_TRACE_DATA("LineTable: send: tableStart=" << start_location
            << ", tableEnd=" << end_location 
            << ", entry_count=" << entry_count);

        m_cmdParser->reply.WriteLong(start_location);
        m_cmdParser->reply.WriteLong(end_location);
        m_cmdParser->reply.WriteInt(entry_count);

        for (int i = 0; i < entry_count; i++) {
            JDWP_TRACE_DATA("LineTable: send: entry#=" << i 
                << ", lineCodeIndex=" << table[i].start_location 
                << ", lineCodeNumber=" << table[i].line_number);
            m_cmdParser->reply.WriteLong(table[i].start_location);
            m_cmdParser->reply.WriteInt(table[i].line_number);
              
        }
    } else {
        throw AgentException(err);
    }
}

void
Method::VariableTableHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jclass refType = m_cmdParser->command.ReadReferenceTypeID(jni);
    jmethodID methodID = m_cmdParser->command.ReadMethodID(jni);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(refType, &classSignature, 0));
        JvmtiAutoFree afcs(classSignature);
        char* methodName = 0;
        char* methodSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, &methodSignature, 0));
        JvmtiAutoFree afmn(methodName);
        JvmtiAutoFree afms(methodSignature);
        JDWP_TRACE_DATA("VariableTable: received: methodName=" << methodName
            << ", methodSignature=" << JDWP_CHECK_NULL(methodSignature)
            << ", classSignature=" << JDWP_CHECK_NULL(classSignature));
    }
#endif

    jboolean isNative;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->IsMethodNative(methodID, &isNative));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }

    if (isNative == JNI_TRUE) {
        JDWP_TRACE_DATA("VariableTable: native method");
        throw AgentException(JDWP_ERROR_NATIVE_METHOD);
    }

    jint size;

    JVMTI_TRACE(err, GetJvmtiEnv()->GetArgumentsSize(methodID, &size));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
    m_cmdParser->reply.WriteInt(size);

    jint entry_count;
    jvmtiLocalVariableEntry* table = 0;

    JVMTI_TRACE(err, GetJvmtiEnv()->GetLocalVariableTable(methodID,
        &entry_count, &table));
    JvmtiAutoFree afv(table);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
    
#ifndef NDEBUG    
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jlocation start_location;
        jlocation end_location;
        GetJvmtiEnv()->GetMethodLocation(methodID,
            &start_location, &end_location);
        JDWP_TRACE_DATA("VariableTable: methodStart=" << start_location
            << ", methodEnd=" << end_location << ", entry_count=" << entry_count);
    }
#endif
    
    JDWP_TRACE_DATA("VariableTable: send: argSize=" << size 
        << ", entry_count=" << entry_count); 
    m_cmdParser->reply.WriteInt(entry_count);

    for (int i = 0; i < entry_count; i++) {

        JDWP_TRACE_DATA("VariableTable: send: entry#=" << i 
            << ", codeIndex=" << table[i].start_location 
            << ", name=" << table[i].name 
            << ", signature=" << table[i].signature 
            << ", length=" << table[i].length 
            << ", slot=" << table[i].slot);

        m_cmdParser->reply.WriteLong(table[i].start_location);
        m_cmdParser->reply.WriteString(table[i].name);
        m_cmdParser->reply.WriteString(table[i].signature);
        m_cmdParser->reply.WriteInt(table[i].length);
        m_cmdParser->reply.WriteInt(table[i].slot);

        JVMTI_TRACE(err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].name)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
        JVMTI_TRACE(err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].signature)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
        JVMTI_TRACE(err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].generic_signature)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
    }
}

void
Method::BytecodesHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jclass refType = m_cmdParser->command.ReadReferenceTypeID(jni);
    jmethodID methodID = m_cmdParser->command.ReadMethodID(jni);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(refType, &classSignature, 0));
        JvmtiAutoFree afcs(classSignature);
        char* methodName = 0;
        char* methodSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, &methodSignature, 0));
        JvmtiAutoFree afmn(methodName);
        JvmtiAutoFree afms(methodSignature);
        JDWP_TRACE_DATA("Bytecodes: received: methodName=" << methodName
            << ", methodSignature=" << JDWP_CHECK_NULL(methodSignature)
            << ", classSignature=" << JDWP_CHECK_NULL(classSignature));
    }
#endif
    jint bytecode_count;
    unsigned char* bytecodes = 0;

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetBytecodes(methodID,
        &bytecode_count, &bytecodes));
    JvmtiAutoFree afv(bytecodes);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }

    JDWP_TRACE_DATA("Bytecodes: send: bytecode_count=" << bytecode_count);
    m_cmdParser->reply.WriteByteArray(reinterpret_cast<jbyte*>(bytecodes), bytecode_count);
}

void
Method::IsObsoleteHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jclass refType = m_cmdParser->command.ReadReferenceTypeID(jni);
    jmethodID methodID = m_cmdParser->command.ReadMethodID(jni);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(refType, &classSignature, 0));
        JvmtiAutoFree afcs(classSignature);
        char* methodName = 0;
        char* methodSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, &methodSignature, 0));
        JvmtiAutoFree afmn(methodName);
        JvmtiAutoFree afms(methodSignature);
        JDWP_TRACE_DATA("IsObsolete: received: methodName=" << methodName
            << ", methodSignature=" << JDWP_CHECK_NULL(methodSignature)
            << ", classSignature=" << JDWP_CHECK_NULL(classSignature));
    }
#endif
    jboolean is_obsolete = JNI_FALSE;

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->IsMethodObsolete(methodID, &is_obsolete));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }

    JDWP_TRACE_DATA("IsObsolete: send: is_obsolete=" << is_obsolete);
    m_cmdParser->reply.WriteBoolean(is_obsolete);
}

void
Method::VariableTableWithGenericHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jclass refType = m_cmdParser->command.ReadReferenceTypeID(jni);
    jmethodID methodID = m_cmdParser->command.ReadMethodID(jni);
#ifndef NDEBUG    
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* classSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(refType, &classSignature, 0));
        JvmtiAutoFree afcs(classSignature);      
        char* methodName = 0;
        char* methodSignature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, &methodSignature, 0));
        JvmtiAutoFree afmn(methodName);
        JvmtiAutoFree afms(methodSignature);
        JDWP_TRACE_DATA("VariableTableWithGeneric: received: methodName=" << methodName
            << ", methodSignature=" << JDWP_CHECK_NULL(methodSignature)
            << ", classSignature=" << JDWP_CHECK_NULL(classSignature));
    }
#endif
    jint size;

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetArgumentsSize(methodID, &size));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
    m_cmdParser->reply.WriteInt(size);

    jint entry_count;
    jvmtiLocalVariableEntry* table = 0;

    JVMTI_TRACE(err, GetJvmtiEnv()->GetLocalVariableTable(methodID,
        &entry_count, &table));
    JvmtiAutoFree afv(table);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
    
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jlocation start_location;
        jlocation end_location;
        GetJvmtiEnv()->GetMethodLocation(methodID,
            &start_location, &end_location);
        JDWP_TRACE_DATA("VariableTableWithGeneric: methodStart="
            << start_location << ", methodEnd=" 
            << end_location << ", entry_count=" << entry_count);
    } 
#endif
    
    JDWP_TRACE_DATA("VariableTableWithGeneric: send: argSize=" << size 
        << ", entry_count=" << entry_count); 
    m_cmdParser->reply.WriteInt(entry_count);

    for (int i = 0; i < entry_count; i++) {

        JDWP_TRACE_DATA("VariableTableWithGeneric: send: entry#=" << i 
            << ", codeIndex=" << table[i].start_location 
            << ", name=" << table[i].name 
            << ", signature=" << table[i].signature 
            << ", length=" << table[i].length 
            << ", slot=" << table[i].slot);        
        
        m_cmdParser->reply.WriteLong(table[i].start_location);
        m_cmdParser->reply.WriteString(table[i].name);
        m_cmdParser->reply.WriteString(table[i].signature);
        m_cmdParser->reply.WriteString(table[i].generic_signature);
        m_cmdParser->reply.WriteInt(table[i].length);
        m_cmdParser->reply.WriteInt(table[i].slot);

        JVMTI_TRACE(err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].name)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
        JVMTI_TRACE(err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].signature)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
        JVMTI_TRACE(err, GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(table[i].generic_signature)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
    }
}
