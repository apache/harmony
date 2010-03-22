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
// TransportManager.cpp
//

#include <string.h>

#include "TransportManager.h"

using namespace jdwp;

static void* 
Alloc(jint numBytes) 
{
    return AgentBase::GetMemoryManager().AllocateNoThrow(numBytes JDWP_FILE_LINE);
}

static void 
Free(void *buffer) 
{
    AgentBase::GetMemoryManager().Free(buffer JDWP_FILE_LINE);
}

static jdwpTransportCallback callback = {Alloc, Free};

typedef void (JNICALL *jdwpTransport_UnLoad_Type)(jdwpTransportEnv** env);

TransportManager::TransportManager() : AgentBase()
{
    m_transportName = 0;
    m_connectTimeout = 0;
    m_handshakeTimeout = 0;
    m_ConnectionPrepared = false;
    m_address = 0;
    m_loadedLib = 0;
    m_env = 0;
    m_isServer = true;
    m_lastErrorMessage = 0;
    m_isConnected = false;
} //TransportManager::TransportManager()

TransportManager::~TransportManager()
{
    if (m_address != 0) {
        GetMemoryManager().Free(m_address JDWP_FILE_LINE);
    }
    if (m_loadedLib != 0) {
        jdwpTransport_UnLoad_Type UnloadFunc = reinterpret_cast<jdwpTransport_UnLoad_Type>
                (GetProcAddress(m_loadedLib, unLoadDecFuncName)); 
        if ((UnloadFunc != 0) && (m_env != 0)) {
            (UnloadFunc) (&m_env); 
        }
        FreeLibrary(m_loadedLib); 
    }
} //TransportManager::~TransportManager()

void 
TransportManager::Init(const char* transportName, 
                const char* libPath) throw(TransportException)
{
    JDWP_TRACE_ENTRY("Init(" << JDWP_CHECK_NULL(transportName) << ',' << JDWP_CHECK_NULL(libPath) << ')');

    JDWP_TRACE_PROG("Init: transport=" << JDWP_CHECK_NULL(transportName )
        << ", libPath=" << JDWP_CHECK_NULL(libPath));

    JDWP_ASSERT(m_loadedLib == 0);
    m_isConnected = false;

    m_transportName = transportName;
    const char* begin = libPath;
    do {
        const char* end = strchr(begin, pathSeparator);
        if (end != 0) {
            char* dirName = static_cast<char *>(GetMemoryManager().Allocate(end - begin + 1 JDWP_FILE_LINE));
            AgentAutoFree afv(dirName JDWP_FILE_LINE);
            strncpy(dirName , begin, end - begin);
            dirName[end - begin] = 0;
            if (strlen(dirName) != 0) {
                m_loadedLib = LoadTransport(dirName, transportName);
            }
            if (*(end + 1) != 0) {
                begin = end + 1;
            } else {
                break;
            }
        } else {
            m_loadedLib = LoadTransport(begin, transportName);
            break;
        }
    } while (m_loadedLib == 0);
    if (m_loadedLib == 0) {
        m_loadedLib = LoadTransport(0, transportName);
    }
    if (m_loadedLib == 0) {
        if (m_lastErrorMessage != 0) {
            GetMemoryManager().Free(m_lastErrorMessage JDWP_FILE_LINE);
        }
        size_t length = strlen("Loading of  failed") + strlen(transportName) + 1;
        m_lastErrorMessage = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        sprintf(m_lastErrorMessage, "Loading of %s failed", transportName);
        JDWP_ERROR("Loading of " << transportName << " failed");
        throw TransportException(JDWP_ERROR_TRANSPORT_LOAD, JDWPTRANSPORT_ERROR_NONE, m_lastErrorMessage);
    }

    jdwpTransport_OnLoad_t transportOnLoad = reinterpret_cast<jdwpTransport_OnLoad_t>
            (GetProcAddress(m_loadedLib, onLoadDecFuncName));
    if (transportOnLoad == 0) {
        if (m_lastErrorMessage != 0) {
            GetMemoryManager().Free(m_lastErrorMessage JDWP_FILE_LINE);
        }
        size_t length = strlen(" function not found in ") + strlen(transportName)
            + strlen(onLoadDecFuncName) + 1;
        m_lastErrorMessage = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        sprintf(m_lastErrorMessage, "%s function not found in %s", onLoadDecFuncName, transportName);
        JDWP_ERROR(onLoadDecFuncName << " function not found in " << transportName);
        throw TransportException(JDWP_ERROR_TRANSPORT_INIT, JDWPTRANSPORT_ERROR_NONE, m_lastErrorMessage);
    }
    jint res = (*transportOnLoad)(GetJavaVM(), &callback, JDWPTRANSPORT_VERSION_1_0, &m_env);
    if (res == JNI_ENOMEM) {
        if (m_lastErrorMessage != 0) {
            GetMemoryManager().Free(m_lastErrorMessage JDWP_FILE_LINE);
        }
        size_t length = strlen("Out of memory");
        m_lastErrorMessage = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        sprintf(m_lastErrorMessage, "Out of memeory");
        throw TransportException(JDWP_ERROR_OUT_OF_MEMORY);
    } else if (res != JNI_OK) {
        if (m_lastErrorMessage != 0) {
            GetMemoryManager().Free(m_lastErrorMessage JDWP_FILE_LINE);
        }
        size_t length = strlen("Invoking of  failed") + strlen(onLoadDecFuncName) + 1;
        m_lastErrorMessage = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        sprintf(m_lastErrorMessage, "Invoking of %s failed", onLoadDecFuncName);
        JDWP_ERROR("Invoking of " << onLoadDecFuncName << " failed");
        throw TransportException(JDWP_ERROR_TRANSPORT_INIT, JDWPTRANSPORT_ERROR_NONE, m_lastErrorMessage);
    }
    if (m_env == 0) {
        if (m_lastErrorMessage != 0) {
            GetMemoryManager().Free(m_lastErrorMessage JDWP_FILE_LINE);
        }
        size_t length = strlen("Transport provided invalid environment");
        m_lastErrorMessage = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        sprintf(m_lastErrorMessage, "Transport provided invalid environment");
        JDWP_ERROR("Transport provided invalid environment");
        throw TransportException(JDWP_ERROR_TRANSPORT_INIT, JDWPTRANSPORT_ERROR_NONE, m_lastErrorMessage);
    }

} // TransportManager::Init()

void 
TransportManager::PrepareConnection(const char* address, bool isServer, 
                jlong connectTimeout, jlong handshakeTimeout) throw(TransportException)
{
    JDWP_TRACE_ENTRY("PrepareConnection(" << JDWP_CHECK_NULL(address) << ',' << isServer 
        << ',' << connectTimeout << ',' << handshakeTimeout << ')');
    
    JDWP_TRACE_PROG("PrepareConnection: address=" << JDWP_CHECK_NULL(address) 
        << " connectTimeout=" << connectTimeout 
        << " handshakeTimeout=" << handshakeTimeout 
        << " isServer=" << isServer);

    JDWP_ASSERT((m_loadedLib != 0) && (!m_ConnectionPrepared));

    m_lastErrorMessage = 0;

    m_connectTimeout = connectTimeout;
    m_handshakeTimeout = handshakeTimeout;
    m_isServer = isServer;

    JDWPTransportCapabilities capabilities;
    jdwpTransportError err = m_env->GetCapabilities(&capabilities);
    CheckReturnStatus(err);
    if ((connectTimeout != 0) && isServer && (!capabilities.can_timeout_accept)) {
        JDWP_INFO("Warning: transport does not support accept timeout");
    }
    if ((connectTimeout != 0) && (!isServer) && (!capabilities.can_timeout_attach)) {
        JDWP_INFO("Warning: transport does not support attach timeout");
    }
    if ((handshakeTimeout != 0) && (!capabilities.can_timeout_handshake)) {
        JDWP_INFO("Warning: transport does not support handshake timeout");
    }

    if (isServer) {
        err = m_env->StartListening(address, &m_address); 
        CheckReturnStatus(err);
        JDWP_INFO("transport is listening on " << m_address);
        JDWP_TRACE_PROG("PrepareConnection: listening on " << m_address);
    } else {
        m_address = static_cast<char *>(GetMemoryManager().Allocate(strlen(address) + 1 JDWP_FILE_LINE));
        strcpy(m_address, address);
    }

    m_ConnectionPrepared = true;

} // TransportManager::PrepareConnection()

void 
TransportManager::Connect() throw(TransportException)
{
    if (m_isConnected) {
        return;
    }

    JDWP_TRACE_PROG("Connect: isServer=" << m_isServer);
    JDWP_ASSERT(m_ConnectionPrepared);
    jdwpTransportError err;
    if (m_isServer) {
        err = m_env->Accept(m_connectTimeout, m_handshakeTimeout);
        CheckReturnStatus(err);
    } else {
        err = m_env->Attach(m_address, m_connectTimeout, m_handshakeTimeout);
        CheckReturnStatus(err);
    }
    m_isConnected = true;
    JDWP_TRACE_PROG("Connect: connection established");
} // TransportManager::Connect()

void 
TransportManager::Launch(const char* command) throw(AgentException)
{
    JDWP_TRACE_PROG("Launch: " << JDWP_CHECK_NULL(command));
    JDWP_ASSERT(m_ConnectionPrepared);
    const char* extra_argv[2];
    extra_argv[0] = m_transportName;
    extra_argv[1] = m_address;
    StartDebugger(command, 2, extra_argv);
    Connect();
} // TransportManager::Launch()

void 
TransportManager::Read(jdwpPacket* packet) throw(TransportException)
{
    JDWP_ASSERT(m_ConnectionPrepared);
    JDWP_TRACE_PACKET("read packet");
    jdwpTransportError err = m_env->ReadPacket(packet);
    CheckReturnStatus(err);
    TracePacket("rcvt", packet);
} // TransportManager::Read()

void 
TransportManager::Write(const jdwpPacket *packet) throw(TransportException)
{
    JDWP_ASSERT(m_ConnectionPrepared);
    JDWP_TRACE_PACKET("send packet");
    jdwpTransportError err = m_env->WritePacket(packet);
    CheckReturnStatus(err);
    TracePacket("sent", packet);
} // TransportManager::Write()

void 
TransportManager::Reset() throw(TransportException)
{
    JDWP_TRACE_PROG("Reset: close connection");
    if(m_env != 0) {
        JDWP_ASSERT(m_ConnectionPrepared);
        jdwpTransportError err = m_env->Close();
        CheckReturnStatus(err);
    }
    m_isConnected = false;
    JDWP_TRACE_PROG("Reset: connection closed");
} // TransportManager::Reset()


void 
TransportManager::Clean() throw(TransportException)
{
    JDWP_TRACE_PROG("Clean: close connection and stop listening");
    if (m_env != 0) {
        m_env->Close();
        m_env->StopListening();
    }
    JDWP_TRACE_PROG("Clean: connection closed and listening stopped");
} // TransportManager::Clean()

bool 

TransportManager::IsOpen()
{
    if (!m_ConnectionPrepared) {
        return false;
    }
    if (m_env->IsOpen() == JNI_TRUE) {
        return true;
    } else {
        return false;
    }
} // TransportManager::IsOpen()

char* 
TransportManager::GetLastTransportError() throw(TransportException)
{
    char* lastErrorMessage = 0;
    if (m_lastErrorMessage != 0) {
        lastErrorMessage = m_lastErrorMessage;
        m_lastErrorMessage = 0;
    } else {
        JDWP_ASSERT(m_env != 0);
        m_env->GetLastError(&lastErrorMessage);
    }
    JDWP_TRACE_PROG("GetLastTransportError: " << JDWP_CHECK_NULL(lastErrorMessage));
    return lastErrorMessage;
} // TransportManager::GetLastTransportError()

void 
TransportManager::CheckReturnStatus(jdwpTransportError err) throw(TransportException)
{
    if (err == JDWPTRANSPORT_ERROR_NONE) {
        return;
    }
    if (err == JDWPTRANSPORT_ERROR_OUT_OF_MEMORY) {
        throw TransportException(JDWP_ERROR_OUT_OF_MEMORY, JDWPTRANSPORT_ERROR_OUT_OF_MEMORY);
    }
    char* lastErrorMessage = GetLastTransportError();
    // AgentBase::GetMemoryManager().Free(lastErrorMessage JDWP_FILE_LINE);
    throw TransportException(JDWP_ERROR_TRANSPORT_INIT, err, lastErrorMessage);
} // TransportManager::CheckReturnStatus()

inline void 
TransportManager::TracePacket(const char* message, const jdwpPacket* packet) 
{ 
    if (packet->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) { 
        JDWP_TRACE_PACKET(message 
                <<" length=" << packet->type.cmd.len 
                << " Id=" << packet->type.cmd.id 
                << " flag=REPLY" 
                << " errorCode=" << (short)(packet->type.reply.errorCode));
    } else { 
        JDWP_TRACE_PACKET(message 
                <<" length=" << packet->type.cmd.len 
                << " Id=" << packet->type.cmd.id 
                << " flag=NONE"
                << " cmdSet=" << (int)(packet->type.cmd.cmdSet) 
                << " cmd=" << (int)(packet->type.cmd.cmd)); 
    } 
} // TransportManager::TracePacket()
