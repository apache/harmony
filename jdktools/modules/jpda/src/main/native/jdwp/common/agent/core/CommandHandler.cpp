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
 * @author Vitaly A. Provodin, Viacheslav G. Rybalov
 */

#include <string.h>

#include "CommandHandler.h"
#include "PacketParser.h"
#include "ThreadManager.h"
#include "EventDispatcher.h"

using namespace jdwp;

//-----------------------------------------------------------------------------

void CommandHandler::ComposeError(const AgentException &e)
{
    m_cmdParser->reply.SetError(e.ErrCode());
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

void SyncCommandHandler::Run(JNIEnv *jni_env, CommandParser *cmd) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Sync::Run(" << jni_env << ',' << cmd << ')');

    m_cmdParser = cmd;
    try
    {
        Execute(jni_env);
    }
    catch (const AgentException& e)
    {
        ComposeError(e);
    }
    
    if (cmd->reply.IsPacketInitialized())
    {
        cmd->WriteReply(jni_env);
    }
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

AsyncCommandHandler::~AsyncCommandHandler()
{
    if (m_cmdParser != 0)
        delete m_cmdParser;
}

//-----------------------------------------------------------------------------

const char* AsyncCommandHandler::GetThreadName() {
    return "_jdwp_AsyncCommandHandler";
}

//-----------------------------------------------------------------------------

void AsyncCommandHandler::Run(JNIEnv *jni_env, CommandParser *cmd) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Async::Run(" << jni_env << ',' << cmd << ')');

    m_cmdParser = new CommandParser();
    cmd->MoveData(jni_env, m_cmdParser);
    try
    {
        GetThreadManager().RunAgentThread(jni_env, StartExecution, this,
            JVMTI_THREAD_MAX_PRIORITY, GetThreadName());
    }
    catch (const AgentException& e)
    {
        JDWP_ASSERT(e.ErrCode() != JDWP_ERROR_NULL_POINTER);
        JDWP_ASSERT(e.ErrCode() != JDWP_ERROR_INVALID_PRIORITY);

        throw e;
    }
}

//-----------------------------------------------------------------------------

void JNICALL
AsyncCommandHandler::StartExecution(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg)
{
    JDWP_TRACE_ENTRY("Async::StartExecution(" << jvmti_env << ',' << jni_env << ',' << arg << ')');

    AsyncCommandHandler *handler = reinterpret_cast<AsyncCommandHandler *>(arg);

    try 
    {
        handler->Execute(jni_env);
    }
    catch (const AgentException &e)
    {
        handler->ComposeError(e);
    }

    try {
        if (handler->m_cmdParser->reply.IsPacketInitialized())
        {
            JDWP_TRACE_CMD("send reply");
            handler->m_cmdParser->WriteReply(jni_env);
        }

        JDWP_TRACE_CMD("Removing command handler: "
            << handler->m_cmdParser->command.GetCommandSet() << "/"
            << handler->m_cmdParser->command.GetCommand());

        handler->Destroy();
    
    } catch (const AgentException &e) {
        // cannot report error in async thread, just print warning message
        JDWP_INFO("JDWP error in asynchronous command: " << e.what() << " [" << e.ErrCode() << "]");
    }
}

//-----------------------------------------------------------------------------

SpecialAsyncCommandHandler::SpecialAsyncCommandHandler()
{
//    m_monitor = new AgentMonitor("SpecialAsyncCommandHandler monitor");
    m_isInvoked = false;
    m_isReleased = false;
}

SpecialAsyncCommandHandler::~SpecialAsyncCommandHandler()
{
}

void SpecialAsyncCommandHandler::ExecuteDeferredInvoke(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY("Async::ExecuteDeferredInvoke(" << jni << ')');
    ExecuteDeferredFunc(jni);
}

void SpecialAsyncCommandHandler::WaitDeferredInvocation(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY("Async::WaitDeferredInvocation(" << jni << ')');

    GetThreadManager().RegisterInvokeHandler(jni, this);
    GetEventDispatcher().PostInvokeSuspend(jni, this);
}

//-----------------------------------------------------------------------------

/**
 * Counts number of arguments in in the method signature.
 */
jint SpecialAsyncCommandHandler::getArgsNumber(char* sig)
{
    JDWP_TRACE_ENTRY("Async::getArgsNumber(" << JDWP_CHECK_NULL(sig) << ')');

    if (sig == 0) return 0;

    jint argsCount = 0;
    const size_t len = strlen(sig);
    for (size_t i = 1; i < len && sig[i] != ')'; i++) {
        while (i < len && sig[i] == '[') i++;
        if (sig[i] == 'L') {
            while (i < len && sig[i] != ';' && sig[i] != ')') i++;
        }
        argsCount++;
    }
    JDWP_TRACE_DATA("getArgsNumber: sig=" << sig << ", args=" << argsCount);

    return argsCount;
}

/**
 * Extracts jdwpTag letter for given argument index in the method signature.
 */
jdwpTag SpecialAsyncCommandHandler::getTag(jint index, char* sig)
{
    JDWP_TRACE_ENTRY("Async::getArgsNumber(" << index << ',' << JDWP_CHECK_NULL(sig) << ')');

    if (sig == 0) return JDWP_TAG_NONE;

    const size_t len = strlen(sig);
    size_t i;
    for (i = 1; index > 0 && i < len && sig[i] != ')'; i++) {
        while (i < len && sig[i] == '[') i++;
        if (sig[i] == 'L') {
            while (i < len && sig[i] != ';' && sig[i] != ')') i++;
        }
        index--;
    }

    return (index == 0) ? static_cast<jdwpTag>(sig[i]) : JDWP_TAG_NONE;
}

/**
 * Extracts class name for given argument index in the method signature.
 * Type signature for this argument should start with 'L' or '[' tag.
 *
 * @return extracted class name in 'name' argument or JNI_FALSE if any error occured
 */
bool SpecialAsyncCommandHandler::getClassNameArg(jint index, char* sig, char* name)
{
    JDWP_TRACE_ENTRY("Async::getArgsNumber(" << index << ',' << JDWP_CHECK_NULL(sig) << ')');

    if (sig == 0) return false;

    // skip previous arguments

    const size_t len = strlen(sig);
    size_t i;
    for (i = 1; index > 0 && i < len && sig[i] != ')'; i++) {
        while (i < len && sig[i] == '[') i++;
        if (sig[i] == 'L') {
            while (i < len && sig[i] != ';' && sig[i] != ')') i++;
        }
        index--;
    }

    if (index > 0) return false;

    // extract type name for the next argument

    bool isArrayType = false;
    size_t j = 0;

    if (sig[i] == '[') {
        // copy all starting '[' chars for array type
        isArrayType = true;
        for (; i < len && sig[i] == '['; i++) {
            name[j++] = sig[i];
        }
    }

    if (sig[i] == 'L') {
        // copy class name until ';'
        if (!isArrayType) {
            i++; // skip starting 'L' for not array type
        }
        for (; i < len && sig[i] != ';'; i++) {
            name[j++] = sig[i];
        }
        if (isArrayType) {
            name[j++] = sig[i]; // add trailing ';' for array type
        }
    } else if (isArrayType) {
        // copy single char tag for primitive array type
        name[j++] = sig[i];
    } else {
        // not a class or array type
        return false;
    }
        
    name[j] = '\0';
    return true;
}

/**
 * Checks that type of the argument value matches declared type for given argument index 
 * in the method signature.
 *
 * @return JNI_FALSE in case of any mismatch or error
 */
jboolean
SpecialAsyncCommandHandler::IsArgValid(JNIEnv *jni, jint index,
                                       jdwpTaggedValue value, char* sig)
                                       throw(AgentException)
{
    JDWP_TRACE_ENTRY("IsArgValid(" << jni << ',' << index 
        << ',' << (int)value.tag << ',' << JDWP_CHECK_NULL(sig) << ')');

    jdwpTag argTag = getTag(index, sig);

    JDWP_TRACE_DATA("IsArgValid: index=" << index << ", value.tag=" << value.tag << ", argTag=" << argTag);

    switch (value.tag) {
        case JDWP_TAG_BOOLEAN:
        case JDWP_TAG_BYTE:
        case JDWP_TAG_CHAR:
        case JDWP_TAG_SHORT:
        case JDWP_TAG_INT:
        case JDWP_TAG_LONG:
        case JDWP_TAG_FLOAT:
        case JDWP_TAG_DOUBLE:
            if (value.tag != argTag) {
                JDWP_TRACE_DATA("IsArgValid: mismatched primitive type tag: index=" << index << ", value.tag=" << value.tag << ", argTag=" << argTag);
                return JNI_FALSE;
            } else {
                return JNI_TRUE;
            }
        case JDWP_TAG_ARRAY:
            if ('[' != argTag) {
                JDWP_TRACE_DATA("IsArgValid: mismatched array type tag: index=" << index << ", value.tag=" << value.tag << ", argTag=" << argTag);
                return JNI_FALSE;
            }
            break;
        case JDWP_TAG_OBJECT:
        case JDWP_TAG_STRING:
        case JDWP_TAG_THREAD:
        case JDWP_TAG_THREAD_GROUP:
        case JDWP_TAG_CLASS_LOADER:
        case JDWP_TAG_CLASS_OBJECT:
            if ('L' != argTag) {
                JDWP_TRACE_DATA("IsArgValid: mismatched reference type tag: index=" << index << ", value.tag=" << value.tag << ", argTag=" << argTag);
                return JNI_FALSE;
            }
            break;
        default: 
            JDWP_TRACE_DATA("IsArgValid: unknown value type tag: index=" << index << ", value.tag=" << value.tag << ", argTag=" << argTag);
            return JNI_FALSE;
    }
    char* name = reinterpret_cast<char*>(GetMemoryManager().Allocate(strlen(sig) JDWP_FILE_LINE));
    AgentAutoFree afv(name JDWP_FILE_LINE);
    if (!getClassNameArg(index, sig, name)) {
        JDWP_TRACE_DATA("IsArgValid: bad class name: index=" << index << ", class=" << JDWP_CHECK_NULL(name));
        return JNI_FALSE;
    }
    jclass cls = jni->FindClass(name);
    if (jni->ExceptionCheck() == JNI_TRUE) {
        jni->ExceptionClear();
        JDWP_TRACE_DATA("IsArgValid: unknown class name: index=" << index << ", class=" << JDWP_CHECK_NULL(name));
        return JNI_FALSE;
    }
    if (!jni->IsInstanceOf(value.value.l, cls)) {
        JDWP_TRACE_DATA("IsArgValid: unmatched class: index=" << index << ", class=" << JDWP_CHECK_NULL(name));
        return JNI_FALSE;
    }
    JDWP_TRACE_DATA("IsArgValid: matched class: index=" << index << ", class=" << JDWP_CHECK_NULL(name));
    return JNI_TRUE;
}
