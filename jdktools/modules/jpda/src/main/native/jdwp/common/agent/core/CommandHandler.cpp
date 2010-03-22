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
#include "CommandHandler.h"
#include "PacketParser.h"
#include "ThreadManager.h"
#include "ClassManager.h"
#include "EventDispatcher.h"
#include "ExceptionManager.h"

using namespace jdwp;

//-----------------------------------------------------------------------------

void CommandHandler::ComposeError(const AgentException &e)
{
    m_cmdParser->reply.SetError(e.ErrCode());
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

int SyncCommandHandler::Run(JNIEnv *jni_env, CommandParser *cmd)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Sync::Run(%p,%p)", jni_env, cmd));
    static int count = 0;
    int ret = 0;

    if (count == 0) {
        GetJniEnv()->PushLocalFrame(100);
    }

    m_cmdParser = cmd;
    ret = Execute(jni_env);
    if (ret != JDWP_ERROR_NONE) {
        AgentException aex = GetExceptionManager().GetLastException();
        ComposeError(aex);
    }
    
    if (cmd->reply.IsPacketInitialized())
    {
        ret = cmd->WriteReply(jni_env);
        JDWP_CHECK_RETURN(ret);

    }

    count++;
    if (count >= 30) {
        GetJniEnv()->PopLocalFrame(NULL);
        count = 0;
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

WorkerThread* AsyncCommandHandler::worker = 0;

static bool isWorkerInitialized = false;

WorkerThread::WorkerThread(JNIEnv* jni) {
    m_head = 0;
    m_tail = 0;
    m_requestListMonitor = new AgentMonitor("_jdwp_CommandHandler_requestListMonitor");

    m_agentThread = GetThreadManager().RunAgentThread(jni, StartExecution, NULL,
						      JVMTI_THREAD_MAX_PRIORITY, "_jdwp_AsyncCommandHandler_Worker");
}

WorkerThread::~WorkerThread() {

}

void JNICALL
WorkerThread::StartExecution(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg)
{
    AsyncCommandHandler::StartExecution(jvmti_env, jni_env, arg);
}

void WorkerThread::AddRequest(AsyncCommandHandler* handler) {
    MonitorAutoLock lock(m_requestListMonitor JDWP_FILE_LINE);
    HandlerNode* node = new HandlerNode();
    node->m_next = 0;
    node->m_handler = handler;
    if (m_tail != 0) {
	m_tail->m_next = node;
    }
    m_tail = node;
    if (m_head == 0) {
	m_head = m_tail;
    }
    m_requestListMonitor->NotifyAll();
}

AsyncCommandHandler* WorkerThread::RemoveRequest() {
    MonitorAutoLock lock(m_requestListMonitor JDWP_FILE_LINE);

    while (m_head == 0) {
	m_requestListMonitor->Wait();
    }

    HandlerNode* node = m_head;
    AsyncCommandHandler* handler = m_head->m_handler;
    if (m_head == m_tail) {
	m_tail = 0;
    }

    m_head = m_head->m_next;

    delete node;

    return handler;
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

int AsyncCommandHandler::Run(JNIEnv *jni_env, CommandParser *cmd)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Async::Run(%p,%p)", jni_env, cmd));

    m_cmdParser = new CommandParser();
    cmd->MoveData(jni_env, m_cmdParser);

    if (worker == 0) {
        worker = new WorkerThread(jni_env);
        isWorkerInitialized = true;
    }

    worker->AddRequest(this);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

void JNICALL
AsyncCommandHandler::StartExecution(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Async::StartExecution(%p,%p,%p)", jvmti_env, jni_env, arg));

    static int count = 0;
    int ret = 0;

    while (true) {
        if (!isWorkerInitialized) {
            continue;
        }
	AsyncCommandHandler* handler = worker->RemoveRequest();

    if (count == 0) {
        GetJniEnv()->PushLocalFrame(100);
    }

    ret = handler->Execute(jni_env);
    if (ret != JDWP_ERROR_NONE) {
        AgentException aex = GetExceptionManager().GetLastException();
        handler->ComposeError(aex);
    }

    if (handler->m_cmdParser->reply.IsPacketInitialized())
    {
        JDWP_TRACE(LOG_RELEASE, (LOG_CMD_FL, "send reply"));
        ret = handler->m_cmdParser->WriteReply(jni_env);
        if (ret != JDWP_ERROR_NONE) {
            // cannot report error in async thread, just print warning message
            AgentException aex = GetExceptionManager().GetLastException();
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in asynchronous command: %s", aex.GetExceptionMessage(jni_env)));
        }
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_CMD_FL, "Removing command handler: %d/%d",
           handler->m_cmdParser->command.GetCommandSet(),
           handler->m_cmdParser->command.GetCommand()));

    count++;
    if (count >= 30) {
        GetJniEnv()->PopLocalFrame(NULL);
        count = 0;
    }

    }
}

//-----------------------------------------------------------------------------
void SpecialAsyncCommandHandler::Destroy() {
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
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Async::ExecuteDeferredInvoke(%p)", jni));
    ExecuteDeferredFunc(jni);
}

int SpecialAsyncCommandHandler::WaitDeferredInvocation(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Async::WaitDeferredInvocation(%p)", jni));

    int ret = GetThreadManager().RegisterInvokeHandler(jni, this);
    JDWP_CHECK_RETURN(ret);
    ret = GetEventDispatcher().PostInvokeSuspend(jni, this);
    return ret;
}

//-----------------------------------------------------------------------------

/**
 * Counts number of arguments in in the method signature.
 */
jint SpecialAsyncCommandHandler::getArgsNumber(char* sig)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Async::getArgsNumber(%s)", JDWP_CHECK_NULL(sig)));

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
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "getArgsNumber: sig=%s, args=%d", sig, argsCount));

    return argsCount;
}

/**
 * Extracts jdwpTag letter for given argument index in the method signature.
 */
jdwpTag SpecialAsyncCommandHandler::getTag(jint index, char* sig)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Async::getArgsNumber(%d,%s)", index, JDWP_CHECK_NULL(sig)));

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
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Async::getArgsNumber(%d,%s)", index, JDWP_CHECK_NULL(sig)));

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
SpecialAsyncCommandHandler::IsArgValid(JNIEnv *jni, jclass klass, jint index,
                                       jdwpTaggedValue value, char* sig)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "IsArgValid(%p,%d,%d,%s)", jni, index, (int)value.tag, JDWP_CHECK_NULL(sig)));

    jdwpTag argTag = getTag(index, sig);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsArgValid: index=%d, value.tag=%d, argTag=%d", index, value.tag, argTag));

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
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsArgValid: mismatched primitive type tag: index=%d, value.tag=%d, argTag=%d", index, value.tag, argTag));
                return JNI_FALSE;
            } else {
                return JNI_TRUE;
            }
        case JDWP_TAG_ARRAY:
            if ('[' != argTag) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsArgValid: mismatched array type tag: index=%d, value.tag=%d, argTag=%d", index, value.tag, argTag));
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
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsArgValid: mismatched reference type tag: index=%d, value.tag=%d, argTag=%d", index, value.tag, argTag));
                return JNI_FALSE;
            }
            break;
        default: 
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsArgValid: unknown value type tag: index=%d, value.tag=%d, argTag=%d", index, value.tag, argTag));
            return JNI_FALSE;
    }
    char* name = reinterpret_cast<char*>(GetMemoryManager().Allocate(strlen(sig) JDWP_FILE_LINE));
    AgentAutoFree afv(name JDWP_FILE_LINE);
    if (!getClassNameArg(index, sig, name)) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsArgValid: bad class name: index=%d, class=%s", index, JDWP_CHECK_NULL(name)));
        return JNI_FALSE;
    }
    // Since jni->FindClass method can't find the required class due to classloader restrications.
    // The class in method signautre should be found by the same classloader, which loaded the
    // class of the method.
    jclass cls = FindClass(klass, name);
    if (cls == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsArgValid: unknown class name: index=%d, class=%s", index, JDWP_CHECK_NULL(name)));
        return JNI_FALSE;
    }
    if (!jni->IsInstanceOf(value.value.l, cls)) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsArgValid: unmatched class: index=%d, class=%s", index, JDWP_CHECK_NULL(name)));
        return JNI_FALSE;
    }
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "IsArgValid: matched class: index=%d, class=%s", index, JDWP_CHECK_NULL(name)));
    return JNI_TRUE;
}

/**
 * Retrieve a class object from a fully-qualified name, or 0 if the class cannot be found.
 *
 * @return a class object from a fully-qualified name, or 0 if the class cannot be found. 
 */
jclass SpecialAsyncCommandHandler::FindClass(jclass klass, char *name)
{	
    if(name == 0) {
       return 0;
    }
    int len = strlen(name);
    char* signature = (char*)GetMemoryManager().Allocate(len + 1 JDWP_FILE_LINE);
    // replace '/' to '.'
    for (int i = 0; i < len; ++i) {
	if (name[i] == '/') {
	    signature[i] = '.';
	} else {
	    signature[i] = name[i];
	}
    }
    signature[len] = 0;

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    jvmtiError err;
    jobject classLoader;

    JVMTI_TRACE(LOG_RELEASE, err, jvmti->GetClassLoader(klass, &classLoader));

    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error calling GetClassLoader()"));
        return 0;
    }

    jclass cls = AgentBase::GetClassManager().GetClassForName(AgentBase::GetJniEnv(), signature, classLoader);
    GetMemoryManager().Free(signature JDWP_FILE_LINE);
    return cls;
}

