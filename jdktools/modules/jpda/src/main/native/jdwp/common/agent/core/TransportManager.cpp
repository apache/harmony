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

#ifndef USING_VMI
#define USING_VMI

#include "TransportManager.h"
#include "ExceptionManager.h"

#include <string.h>

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
    m_isCleaned = false;
} //TransportManager::TransportManager()

TransportManager::~TransportManager()
{
#if defined(WIN32) || defined(WIN64)
    if (m_sendMonitor !=0) {
        delete m_sendMonitor;
        m_sendMonitor = 0;
    }
#endif
    if (m_address != 0) {
        GetMemoryManager().Free(m_address JDWP_FILE_LINE);
    }
    if (m_loadedLib != 0) {
	    PORT_ACCESS_FROM_JAVAVM(GetJavaVM()); 
        jdwpTransport_UnLoad_Type UnloadFunc = 0;
		UDATA ret = hysl_lookup_name(m_loadedLib, (const char*) unLoadDecFuncName, (UDATA*) &UnloadFunc, "VL");
        if (ret != 0) {
	        ret = hysl_lookup_name(m_loadedLib, "jdwpTransport_UnLoad", (UDATA*) &UnloadFunc, "VL");
        }
        if ((UnloadFunc != 0) && (m_env != 0)) {
            (UnloadFunc) (&m_env); 
		}
		hysl_close_shared_library (m_loadedLib);
    }
} //TransportManager::~TransportManager()

int 
TransportManager::Init(const char* transportName, 
                const char* libPath)
{
    JDWP_CHECK_NULL(transportName);
    JDWP_CHECK_NULL(libPath);
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Init(%s,%s)", transportName, libPath));

    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Init: transport=%s, libPath=%s", transportName, libPath));

    JDWP_ASSERT(m_loadedLib == 0);
    PORT_ACCESS_FROM_JAVAVM(GetJavaVM());
    m_isConnected = false;
#if defined(WIN32) || defined(WIN64)
    m_sendMonitor = new AgentMonitor("_jdwp_send_waitMonitor");
#endif
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
        hystr_printf(privatePortLibrary, m_lastErrorMessage, (U_32)length, "Loading of %s failed", transportName);
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, m_lastErrorMessage));
        AgentException ex(JDWP_ERROR_TRANSPORT_LOAD,
						   JDWPTRANSPORT_ERROR_NONE, m_lastErrorMessage);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_TRANSPORT_LOAD;
    }

    jdwpTransport_OnLoad_t transportOnLoad;
    UDATA ret = hysl_lookup_name(m_loadedLib, (char*) onLoadDecFuncName, (UDATA*) &transportOnLoad, "ILLIL");
    if (ret != 0) {
	ret = hysl_lookup_name(m_loadedLib, "jdwpTransport_OnLoad", (UDATA*) &transportOnLoad, "ILLIL");
    }
    if (transportOnLoad == 0) {
        if (m_lastErrorMessage != 0) {
            GetMemoryManager().Free(m_lastErrorMessage JDWP_FILE_LINE);
        }
        size_t length = strlen(" function not found in ") + strlen(transportName)
            + strlen(onLoadDecFuncName) + 1;
        m_lastErrorMessage = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        hystr_printf(privatePortLibrary, m_lastErrorMessage, (U_32)length, "%s function not found in %s", onLoadDecFuncName, transportName);
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, m_lastErrorMessage));
        AgentException ex(JDWP_ERROR_TRANSPORT_INIT,
						   JDWPTRANSPORT_ERROR_NONE, m_lastErrorMessage);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_TRANSPORT_INIT;
    }
 
    jint res = (*transportOnLoad)(GetJavaVM(), &callback, JDWPTRANSPORT_VERSION_1_0, &m_env);
    if (res == JNI_ENOMEM) {
        if (m_lastErrorMessage != 0) {
            GetMemoryManager().Free(m_lastErrorMessage JDWP_FILE_LINE);
        }
        size_t length = strlen("Out of memory");
        m_lastErrorMessage = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        hystr_printf(privatePortLibrary, m_lastErrorMessage, (U_32)length, "Out of memeory");
        AgentException ex(JDWP_ERROR_OUT_OF_MEMORY);
    	JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_OUT_OF_MEMORY;
    } else if (res != JNI_OK) {
        if (m_lastErrorMessage != 0) {
            GetMemoryManager().Free(m_lastErrorMessage JDWP_FILE_LINE);
        }
        size_t length = strlen("Invoking of  failed") + strlen(onLoadDecFuncName) + 1;
        m_lastErrorMessage = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        hystr_printf(privatePortLibrary, m_lastErrorMessage, (U_32)length, "Invoking of %s failed", onLoadDecFuncName);
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, m_lastErrorMessage));
        AgentException ex(JDWP_ERROR_TRANSPORT_INIT,
						   JDWPTRANSPORT_ERROR_NONE, m_lastErrorMessage);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_TRANSPORT_INIT;
    }
    if (m_env == 0) {
        if (m_lastErrorMessage != 0) {
            GetMemoryManager().Free(m_lastErrorMessage JDWP_FILE_LINE);
        }
        size_t length = strlen("Transport provided invalid environment");
        m_lastErrorMessage = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        hystr_printf(privatePortLibrary, m_lastErrorMessage, (U_32)length, "Transport provided invalid environment");
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, m_lastErrorMessage));
        AgentException ex(JDWP_ERROR_TRANSPORT_INIT,
						   JDWPTRANSPORT_ERROR_NONE, m_lastErrorMessage);
    	JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_TRANSPORT_INIT;
    }

    return JDWP_ERROR_NONE;
} // TransportManager::Init()

int 
TransportManager::PrepareConnection(const char* address, bool isServer, 
                jlong connectTimeout, jlong handshakeTimeout)
{
    JDWP_CHECK_NULL(address);
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "PrepareConnection(%s,%s,%lld,%lld)", address, (isServer?"TRUE":"FALSE"), connectTimeout, handshakeTimeout));
    
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "PrepareConnection: address=%s isServer=%s connectTimeout=%lld handshakeTimeout=%lld",
                    address, (isServer?"TRUE":"FALSE"), connectTimeout, handshakeTimeout));

    JDWP_ASSERT((m_loadedLib != 0) && (!m_ConnectionPrepared));

    m_lastErrorMessage = 0;

    m_connectTimeout = connectTimeout;
    m_handshakeTimeout = handshakeTimeout;
    m_isServer = isServer;

    JDWPTransportCapabilities capabilities;
    jdwpTransportError err = m_env->GetCapabilities(&capabilities);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return CheckReturnStatus(err);
    }

	// only print error message when all of handshake, accept and attach timeout can not be used.
    if ((handshakeTimeout != 0) && (!capabilities.can_timeout_handshake) && (connectTimeout != 0)) {
        if ((isServer && (!capabilities.can_timeout_accept)) || ((!isServer) && (!capabilities.can_timeout_attach))) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Warning: transport does not support timeouts"));
        }
    }

    if (isServer) {
        err = m_env->StartListening(address, &m_address); 
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return CheckReturnStatus(err);
        }

        JDWP_TRACE(LOG_RELEASE, (LOG_SIMPLE_FL, "Listening for transport %s at address: %s", m_transportName, m_address));
        JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "PrepareConnection: listening on %s", m_address));
    } else {
        m_address = static_cast<char *>(GetMemoryManager().Allocate(strlen(address) + 1 JDWP_FILE_LINE));
        strcpy(m_address, address);
    }

    m_ConnectionPrepared = true;

    return JDWP_ERROR_NONE;
} // TransportManager::PrepareConnection()

int 
TransportManager::Connect()
{
    if (m_isConnected) {
        return JDWP_ERROR_NONE;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Connect: isServer=%s", (m_isServer?"TRUE":"FALSE")));
    JDWP_ASSERT(m_ConnectionPrepared);
    jdwpTransportError err;
    if (m_isServer) {
        if (strcmp("dt_shmem", m_transportName)) {
            err = m_env->Accept(m_connectTimeout, m_handshakeTimeout);
            if (err != JDWPTRANSPORT_ERROR_NONE) {
                return CheckReturnStatus(err);
            }
        } else {
            /*
             * Work around: because Accept can't be interrupted by StopListening
             * when use "dt_shmem", we use loop instead of blocking at Accept
             */
            jlong timeout = 100;
            jlong total = m_connectTimeout;
            while (m_connectTimeout == 0 || total > 0) {
                if (m_isCleaned) {
                    AgentException ex(JDWP_ERROR_TRANSPORT_INIT, JDWPTRANSPORT_ERROR_NONE, "Connection failed");
                    JDWP_SET_EXCEPTION(ex);
                    return JDWP_ERROR_TRANSPORT_INIT;
                }

                err = m_env->Accept(timeout, m_handshakeTimeout);

                if (err == JDWPTRANSPORT_ERROR_NONE) {
                    break;
                }

                if (err != JDWPTRANSPORT_ERROR_TIMEOUT) {
                    return CheckReturnStatus(err);
                }

                total -= timeout;
            }
            if (err != JDWPTRANSPORT_ERROR_NONE) {
                return CheckReturnStatus(err);
            }
        }
    } else {
        err = m_env->Attach(m_address, m_connectTimeout, m_handshakeTimeout);
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return CheckReturnStatus(err);
        }
    }
    m_isConnected = true;
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Connect: connection established"));
    return JDWP_ERROR_NONE;
} // TransportManager::Connect()

int 
TransportManager::Launch(const char* command)
{
    JDWP_CHECK_NULL(command);
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Launch: %s", command));
    JDWP_ASSERT(m_ConnectionPrepared);
    const char* extra_argv[2];
    extra_argv[0] = m_transportName;
    extra_argv[1] = m_address;
    int ret = StartDebugger(command, 2, extra_argv);
    JDWP_CHECK_RETURN(ret);
    ret = Connect();
    return ret;
} // TransportManager::Launch()

int 
TransportManager::Read(jdwpPacket* packet)
{
    JDWP_ASSERT(m_ConnectionPrepared);
    JDWP_TRACE(LOG_RELEASE, (LOG_PACKET_FL, "read packet"));
    jdwpTransportError err = m_env->ReadPacket(packet);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return CheckReturnStatus(err);
    }
    TracePacket("rcvt", packet);
    return JDWP_ERROR_NONE;
} // TransportManager::Read()

int 
TransportManager::Write(const jdwpPacket *packet)
{
    JDWP_ASSERT(m_ConnectionPrepared);
    JDWP_TRACE(LOG_RELEASE, (LOG_PACKET_FL, "send packet"));
    jdwpTransportError err;
#if defined(WIN32) || defined(WIN64)
    // work around: prevent different threads from sending data at the same time.
    // This enables Harmony jdwp to work with RI dt_shmem.dll
    {
    MonitorAutoLock lock(m_sendMonitor JDWP_FILE_LINE);
#endif
    err = m_env->WritePacket(packet);
#if defined(WIN32) || defined(WIN64)
    }
#endif
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return CheckReturnStatus(err);
    }
    TracePacket("sent", packet);
    return JDWP_ERROR_NONE;
} // TransportManager::Write()

int 
TransportManager::Reset()
{
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Reset: close connection"));
    if(m_env != 0) {
        JDWP_ASSERT(m_ConnectionPrepared);
        jdwpTransportError err = m_env->Close();
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return CheckReturnStatus(err);
        }
    }
    m_isConnected = false;
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Reset: connection closed"));
    return JDWP_ERROR_NONE;
} // TransportManager::Reset()


void 
TransportManager::Clean()
{
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Clean: close connection and stop listening"));
    if (m_env != 0) {
        m_env->Close();
        m_env->StopListening();
    }

    m_isCleaned = true;
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Clean: connection closed and listening stopped"));
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
TransportManager::GetLastTransportError()
{
    char* lastErrorMessage = 0;
    if (m_lastErrorMessage != 0) {
        lastErrorMessage = m_lastErrorMessage;
        m_lastErrorMessage = 0;
    } else {
        JDWP_ASSERT(m_env != 0);
        m_env->GetLastError(&lastErrorMessage);
    }
    JDWP_CHECK_NULL(lastErrorMessage);
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "GetLastTransportError: %s", lastErrorMessage));
    return lastErrorMessage;
} // TransportManager::GetLastTransportError()

/**
 * Returns JDWP_ERROR_NONE if there is no error, an error value 
 * otherwise 
 */
int
TransportManager::CheckReturnStatus(jdwpTransportError err)
{
    if (err == JDWPTRANSPORT_ERROR_OUT_OF_MEMORY) {
        AgentException ex(JDWP_ERROR_OUT_OF_MEMORY, JDWPTRANSPORT_ERROR_OUT_OF_MEMORY);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_OUT_OF_MEMORY;
    }
    char* lastErrorMessage = GetLastTransportError();
    AgentException ex(JDWP_ERROR_TRANSPORT_INIT, err, lastErrorMessage);
    JDWP_SET_EXCEPTION(ex);
    return JDWP_ERROR_TRANSPORT_INIT;
} // TransportManager::CheckReturnStatus()

inline void 
TransportManager::TracePacket(const char* message, const jdwpPacket* packet) 
{ 
    if (packet->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) { 
        JDWP_TRACE(LOG_RELEASE, (LOG_PACKET_FL, "%s length=%d id=%d flag=REPLY errorCode=%d",
                          message, packet->type.cmd.len, packet->type.cmd.id, (short)(packet->type.reply.errorCode)));
    } else { 
        JDWP_TRACE(LOG_RELEASE, (LOG_PACKET_FL, "%s length=%d id=%d flag=NONE cmdSet=%d cmd=%d",
                          message, packet->type.cmd.len, packet->type.cmd.id, (int)(packet->type.cmd.cmdSet), (int)(packet->type.cmd.cmd)));

    } 
} // TransportManager::TracePacket()

LoadedLibraryHandler TransportManager::LoadTransport(const char* dirName, const char* transportName)
{
    PORT_ACCESS_FROM_JAVAVM(GetJavaVM());

    JDWP_CHECK_NULL(dirName); JDWP_CHECK_NULL(transportName);
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "LoadTransport(%s,%s)", dirName, transportName));

    JDWP_ASSERT(transportName != 0);
    char* transportFullName = 0;

#ifdef WIN32
    if (dirName == 0) {
        size_t length = strlen(transportName) + 5;
        transportFullName = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        hystr_printf(privatePortLibrary, transportFullName, (U_32)length, "%s.dll", transportName);
    } else {
        size_t length = strlen(dirName) + strlen(transportName) + 6;
        transportFullName = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        hystr_printf(privatePortLibrary, transportFullName, (U_32)length, "%s\\%s.dll", dirName, transportName);
    }
#else
    if (dirName == 0) {
        size_t length = strlen(transportName) + 7;
        transportFullName = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        hystr_printf(privatePortLibrary, transportFullName, length, "lib%s.so", transportName);
    } else {
        size_t length = strlen(dirName) + strlen(transportName) + 8;
        transportFullName = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        hystr_printf(privatePortLibrary, transportFullName, length, "%s/lib%s.so", dirName, transportName);
    }
#endif

    UDATA res;
    UDATA ret = hysl_open_shared_library(transportFullName,&res, FALSE);

    if (ret != 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "LoadTransport: loading library %s failed: %s)", transportFullName, hyerror_last_error_message()));
        res = 0;
    } else {
        JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "LoadTransport: transport library %s loaded", transportFullName));
    }
    return (LoadedLibraryHandler)res;
}

#endif
