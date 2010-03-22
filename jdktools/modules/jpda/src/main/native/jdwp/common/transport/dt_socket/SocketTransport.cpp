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
// SocketTransport.cpp
//

/**
 * This is implementation of JDWP Agent TCP/IP Socket transport.
 * Main module.
 */

#include "SocketTransport_pd.h"

/**
 * This function sets into internalEnv struct message and status code of last transport error
 */
static void 
SetLastTranError(jdwpTransportEnv* env, const char* messagePtr, int errorStatus)
{
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    if (ienv->lastError != 0) {
        ienv->lastError->insertError(messagePtr, errorStatus);
    } else {
        ienv->lastError = new(ienv->alloc, ienv->free) LastTransportError(messagePtr, errorStatus, ienv->alloc, ienv->free);
    }
    return;
} // SetLastTranError

/**
 * This function sets into internalEnv struct prefix message for last transport error
 */
static void 
SetLastTranErrorMessagePrefix(jdwpTransportEnv* env, const char* messagePrefix)
{
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    if (ienv->lastError != 0) {
        ienv->lastError->addErrorMessagePrefix(messagePrefix);
    }
    return;
} // SetLastTranErrorMessagePrefix


/**
 * The timeout used for invocation of select function in SelectRead and SelectSend methods
 */
static const jint cycle = 1000; // wait cycle in milliseconds 

/**
 * This function is used to determine the read status of socket (in terms of select function).
 * The function avoids absolutely blocking select
 */
static jdwpTransportError 
SelectRead(jdwpTransportEnv* env, SOCKET sckt, jlong deadline = 0) {

    jlong currentTimeout = cycle;
    while ((deadline == 0) || ((currentTimeout = (deadline - GetTickCount())) > 0)) {
        currentTimeout = currentTimeout < cycle ? currentTimeout : cycle;
        TIMEVAL tv = {(long)(currentTimeout / 1000), (long)(currentTimeout % 1000)};
        fd_set fdread;
        FD_ZERO(&fdread);
        FD_SET(sckt, &fdread);

        int ret = select((int)sckt + 1, &fdread, NULL, NULL, &tv);
        if (ret == SOCKET_ERROR) {
            int err = GetLastErrorStatus();
            // ignore signal interruption
            if (err != SOCKET_ERROR_EINTR) {
                SetLastTranError(env, "socket error", err);
                return JDWPTRANSPORT_ERROR_IO_ERROR;
            }
        }
        if ((ret > 0) && (FD_ISSET(sckt, &fdread))) {
            return JDWPTRANSPORT_ERROR_NONE; //timeout is not occurred
        }
    }
    SetLastTranError(env, "timeout occurred", 0);
    return JDWPTRANSPORT_ERROR_TIMEOUT; //timeout occurred
} // SelectRead

/**
 * This function is used to determine the send status of socket (in terms of select function).
 * The function avoids absolutely blocking select
 */
static jdwpTransportError 
SelectSend(jdwpTransportEnv* env, SOCKET sckt, jlong deadline = 0) {

    jlong currentTimeout = cycle;
    while ((deadline == 0) || ((currentTimeout = (deadline - GetTickCount())) > 0)) {
        currentTimeout = currentTimeout < cycle ? currentTimeout : cycle;
        TIMEVAL tv = {(long)(currentTimeout / 1000), (long)(currentTimeout % 1000)};
        fd_set fdwrite;
        FD_ZERO(&fdwrite);
        FD_SET(sckt, &fdwrite);

        int ret = select((int)sckt + 1, NULL, &fdwrite, NULL, &tv);
        if (ret == SOCKET_ERROR) {
            int err = GetLastErrorStatus();
            // ignore signal interruption
            if (err != SOCKET_ERROR_EINTR) {
                SetLastTranError(env, "socket error", err);
                return JDWPTRANSPORT_ERROR_IO_ERROR;
            }
        }
        if ((ret > 0) && (FD_ISSET(sckt, &fdwrite))) {
            return JDWPTRANSPORT_ERROR_NONE; //timeout is not occurred
        }
    }
    SetLastTranError(env, "timeout occurred", 0);
    return JDWPTRANSPORT_ERROR_TIMEOUT; //timeout occurred
} // SelectRead

/**
 * This function sends data on a connected socket
 */
static jdwpTransportError
SendData(jdwpTransportEnv* env, SOCKET sckt, const char* data, int dataLength, jlong deadline = 0)
{
    long left = dataLength;
    long off = 0;
    int ret;

    while (left > 0) {
        jdwpTransportError err = SelectSend(env, sckt, deadline);
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return err;
        }
        ret = send(sckt, (data + off), left, 0);
        if (ret == SOCKET_ERROR) {
            int err = GetLastErrorStatus();
            // ignore signal interruption
            if (err != SOCKET_ERROR_EINTR) {
                SetLastTranError(env, "socket error", err);
                return JDWPTRANSPORT_ERROR_IO_ERROR;
            }
        }
        left -= ret;
        off += ret;
    } //while
    return JDWPTRANSPORT_ERROR_NONE;
} //SendData

/**
 * This function receives data from a connected socket
 */
static jdwpTransportError
ReceiveData(jdwpTransportEnv* env, SOCKET sckt, char* buffer, int dataLength, jlong deadline = 0, int* readByte = 0)
{
    long left = dataLength;
    long off = 0;
    int ret;

    if (readByte != 0) {
        *readByte = 0;
    }

    while (left > 0) {
        jdwpTransportError err = SelectRead(env, sckt, deadline);
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return err;
        }
        ret = recv(sckt, (buffer + off), left, 0);
        if (ret == SOCKET_ERROR) {
            int err = GetLastErrorStatus();
            // ignore signal interruption
            if (err != SOCKET_ERROR_EINTR) {
                SetLastTranError(env, "data receiving failed", err);
                return JDWPTRANSPORT_ERROR_IO_ERROR;
            }
        }
        if (ret == 0) {
            SetLastTranError(env, "premature EOF", 0);
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
        left -= ret;
        off += ret;
        if (readByte != 0) {
            *readByte = off;
        }
    } //while
    return JDWPTRANSPORT_ERROR_NONE;
} // ReceiveData

/**
 * This function enable/disables socket blocking mode 
 */
static bool 
SetSocketBlockingMode(jdwpTransportEnv* env, SOCKET sckt, bool isBlocked)
{
    unsigned long ul = isBlocked ? 0 : 1;
    if (ioctlsocket(sckt, FIONBIO, &ul) == SOCKET_ERROR) {
        SetLastTranError(env, "socket error", GetLastErrorStatus());
        return false;
    }
    return true;
} // SetSocketBlockingMode()

/**
 * This function performes handshake procedure
 */
static jdwpTransportError 
CheckHandshaking(jdwpTransportEnv* env, SOCKET sckt, jlong handshakeTimeout)
{
    const char* handshakeString = "JDWP-Handshake";
    char receivedString[14]; //length of "JDWP-Handshake"

    jlong deadline = (handshakeTimeout == 0) ? 0 : (jlong)GetTickCount() + handshakeTimeout;

    jdwpTransportError err;
    err = SendData(env, sckt, handshakeString, (int)strlen(handshakeString), deadline);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        SetLastTranErrorMessagePrefix(env, "'JDWP-Handshake' sending error: ");
        return err;
    }

    err = ReceiveData(env, sckt, receivedString, (int)strlen(handshakeString), deadline);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        SetLastTranErrorMessagePrefix(env, "'JDWP-Handshake' receiving error: ");
        return err;
    }

    if (memcmp(receivedString, handshakeString, 14) != 0) {
        SetLastTranError(env, "handshake error, 'JDWP-Handshake' is not received", 0);
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    return JDWPTRANSPORT_ERROR_NONE;
}// CheckHandshaking

/**
 * This function decodes address and populates sockaddr_in structure
 */
static jdwpTransportError
DecodeAddress(jdwpTransportEnv* env, const char *address, struct sockaddr_in *sa, bool isServer) 
{
    memset(sa, 0, sizeof(struct sockaddr_in));
    sa->sin_family = AF_INET;

    if ((address == 0) || (*address == 0)) {  //empty address
        sa->sin_addr.s_addr = isServer ? htonl(INADDR_ANY) : inet_addr("127.0.0.1");
        sa->sin_port = 0;
        return JDWPTRANSPORT_ERROR_NONE;
    }

    const char* colon = strchr(address, ':');
    if (colon == 0) {  //address is like "port"
        sa->sin_port = htons((u_short)atoi(address));
        sa->sin_addr.s_addr = isServer ? htonl(INADDR_ANY) : inet_addr("127.0.0.1");
    } else { //address is like "host:port"
        sa->sin_port = htons((u_short)atoi(colon + 1));

        char *hostName = (char*)(((internalEnv*)env->functions->reserved1)
            ->alloc)((jint)(colon - address + 1));
        if (hostName == 0) {
            SetLastTranError(env, "out of memory", 0);
            return JDWPTRANSPORT_ERROR_OUT_OF_MEMORY;
        }
        memcpy(hostName, address, colon - address);
        hostName[colon - address] = '\0';
        sa->sin_addr.s_addr = inet_addr(hostName);
        if (sa->sin_addr.s_addr == INADDR_NONE) {
            struct hostent *host = gethostbyname(hostName);
            if (host == 0) {
                SetLastTranError(env, "unable to resolve host name", 0);
                (((internalEnv*)env->functions->reserved1)->free)(hostName);
                return JDWPTRANSPORT_ERROR_IO_ERROR;
            }
            memcpy(&(sa->sin_addr), host->h_addr_list[0], host->h_length);
        } //if
        (((internalEnv*)env->functions->reserved1)->free)(hostName);
    } //if
    return JDWPTRANSPORT_ERROR_NONE;
} //DecodeAddress

/**
 * This function implements jdwpTransportEnv::GetCapabilities
 */
static jdwpTransportError JNICALL
TCPIPSocketTran_GetCapabilities(jdwpTransportEnv* env, 
        JDWPTransportCapabilities* capabilitiesPtr) 
{
    memset(capabilitiesPtr, 0, sizeof(JDWPTransportCapabilities));
    capabilitiesPtr->can_timeout_attach = 1;
    capabilitiesPtr->can_timeout_accept = 1;
    capabilitiesPtr->can_timeout_handshake = 1;

    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_GetCapabilities

/**
 * This function implements jdwpTransportEnv::Close
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_Close(jdwpTransportEnv* env)
{
    SOCKET envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket == INVALID_SOCKET) {
        return JDWPTRANSPORT_ERROR_NONE;
    }

    ((internalEnv*)env->functions->reserved1)->envClientSocket = INVALID_SOCKET;

    int err;
    err = shutdown(envClientSocket, SD_BOTH);
    if (err == SOCKET_ERROR) {
        SetLastTranError(env, "close socket failed", GetLastErrorStatus());
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    err = closesocket(envClientSocket);
    if (err == SOCKET_ERROR) {
        SetLastTranError(env, "close socket failed", GetLastErrorStatus());
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_Close

/**
 * This function sets socket options SO_REUSEADDR and TCP_NODELAY
 */
static bool 
SetSocketOptions(jdwpTransportEnv* env, SOCKET sckt) 
{
    BOOL isOn = TRUE;
    if (setsockopt(sckt, SOL_SOCKET, SO_REUSEADDR, (const char*)&isOn, sizeof(isOn)) == SOCKET_ERROR) {                                                              
        SetLastTranError(env, "setsockopt(SO_REUSEADDR) failed", GetLastErrorStatus());
        return false;
    }
    if (setsockopt(sckt, IPPROTO_TCP, TCP_NODELAY, (const char*)&isOn, sizeof(isOn)) == SOCKET_ERROR) {
        SetLastTranError(env, "setsockopt(TCPNODELAY) failed", GetLastErrorStatus());
        return false;
    }
    return true;
} // SetSocketOptions()

/**
 * This function implements jdwpTransportEnv::Attach
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_Attach(jdwpTransportEnv* env, const char* address,
        jlong attachTimeout, jlong handshakeTimeout)
{
    if ((address == 0) || (*address == 0)) {
        SetLastTranError(env, "address is missing", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    if (attachTimeout < 0) {
        SetLastTranError(env, "attachTimeout timeout is negative", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    if (handshakeTimeout < 0) {
        SetLastTranError(env, "handshakeTimeout timeout is negative", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    SOCKET envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket != INVALID_SOCKET) {
        SetLastTranError(env, "there is already an open connection to the debugger", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    SOCKET envServerSocket = ((internalEnv*)env->functions->reserved1)->envServerSocket;
    if (envServerSocket != INVALID_SOCKET) {
        SetLastTranError(env, "transport is currently in listen mode", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    struct sockaddr_in serverSockAddr;
    jdwpTransportError res = DecodeAddress(env, address, &serverSockAddr, false);
    if (res != JDWPTRANSPORT_ERROR_NONE) {
        return res;
    }

    SOCKET clientSocket = socket(AF_INET, SOCK_STREAM, 0);
    if (clientSocket == INVALID_SOCKET) {
        SetLastTranError(env, "unable to create socket", GetLastErrorStatus());
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }
    
    if (!SetSocketOptions(env, clientSocket)) {
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    if (attachTimeout == 0) {
        if (!SetSocketBlockingMode(env, clientSocket, true)) {
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
        int err = connect(clientSocket, (struct sockaddr *)&serverSockAddr, sizeof(serverSockAddr));
        if (err == SOCKET_ERROR) {
            SetLastTranError(env, "connection failed", GetLastErrorStatus());
            SetSocketBlockingMode(env, clientSocket, false);
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
        if (!SetSocketBlockingMode(env, clientSocket, false)) {
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
    } else {
        if (!SetSocketBlockingMode(env, clientSocket, false)) {
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
        int err = connect(clientSocket, (struct sockaddr *)&serverSockAddr, sizeof(serverSockAddr));
        if (err == SOCKET_ERROR) {
            if (GetLastErrorStatus() != SOCKETWOULDBLOCK) {
                SetLastTranError(env, "connection failed", GetLastErrorStatus());
                return JDWPTRANSPORT_ERROR_IO_ERROR;
            } else {  
                fd_set fdwrite;
                FD_ZERO(&fdwrite);
                FD_SET(clientSocket, &fdwrite);
                TIMEVAL tv = {(long)(attachTimeout / 1000), (long)(attachTimeout % 1000)};

                int ret = select((int)clientSocket + 1, NULL, &fdwrite, NULL, &tv);
                if (ret == SOCKET_ERROR) {
                    SetLastTranError(env, "socket error", GetLastErrorStatus());
                    return JDWPTRANSPORT_ERROR_IO_ERROR;
                }
                if ((ret != 1) || !(FD_ISSET(clientSocket, &fdwrite))) {
                    SetLastTranError(env, "timeout occurred", 0);
                    return JDWPTRANSPORT_ERROR_IO_ERROR;
                }
            }
        }
    }

    EnterCriticalSendSection(env);
    EnterCriticalReadSection(env);
    ((internalEnv*)env->functions->reserved1)->envClientSocket = clientSocket;
    res = CheckHandshaking(env, clientSocket, (long)handshakeTimeout);
    LeaveCriticalReadSection(env);
    LeaveCriticalSendSection(env);
    if (res != JDWPTRANSPORT_ERROR_NONE) {
        TCPIPSocketTran_Close(env);
        return res;
    }

    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_Attach

/**
 * This function implements jdwpTransportEnv::StartListening
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_StartListening(jdwpTransportEnv* env, const char* address, 
        char** actualAddress)
{
    SOCKET envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket != INVALID_SOCKET) {
        SetLastTranError(env, "there is already an open connection to the debugger", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    SOCKET envServerSocket = ((internalEnv*)env->functions->reserved1)->envServerSocket;
    if (envServerSocket != INVALID_SOCKET) {
        SetLastTranError(env, "transport is currently in listen mode", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    jdwpTransportError res;
    struct sockaddr_in serverSockAddr;
    res = DecodeAddress(env, address, &serverSockAddr, true);
    if (res != JDWPTRANSPORT_ERROR_NONE) {
        return res;
    }

    SOCKET serverSocket = socket(AF_INET, SOCK_STREAM, 0);
    if (serverSocket == INVALID_SOCKET) {
        SetLastTranError(env, "unable to create socket", GetLastErrorStatus());
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    if (!SetSocketOptions(env, serverSocket)) {
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    int err;

    err = bind(serverSocket, (struct sockaddr *)&serverSockAddr, sizeof(serverSockAddr));
    if (err == SOCKET_ERROR) {
        SetLastTranError(env, "binding to port failed", GetLastErrorStatus());
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    err = listen(serverSocket, SOMAXCONN);
    if (err == SOCKET_ERROR) {
        SetLastTranError(env, "listen start failed", GetLastErrorStatus());
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    if (!SetSocketBlockingMode(env, serverSocket, false)) {
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    ((internalEnv*)env->functions->reserved1)->envServerSocket = serverSocket;

    socklen_t len = sizeof(serverSockAddr);
    err = getsockname(serverSocket, (struct sockaddr *)&serverSockAddr, &len);
    if (err == SOCKET_ERROR) {
        SetLastTranError(env, "socket error", GetLastErrorStatus());
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    char* retAddress = 0;

    // RI always returns only port number in listening mode
/*
    char portName[6];
    sprintf(portName, "%d", ntohs(serverSockAddr.sin_port)); //instead of itoa()

    char hostName[NI_MAXHOST];
    if (getnameinfo((struct sockaddr *)&serverSockAddr, len, hostName, sizeof(hostName), NULL, 0, 0)) {
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }
    if (strcmp(hostName, "0.0.0.0") == 0) {
        gethostname(hostName, sizeof(hostName));
    }
    retAddress = (char*)(((internalEnv*)env->functions->reserved1)
        ->alloc)((jint)(strlen(hostName) + strlen(portName) + 2)); 
    if (retAddress == 0) {
        SetLastTranError(env, "out of memory", 0);
        return JDWPTRANSPORT_ERROR_OUT_OF_MEMORY;
    }
    sprintf(retAddress, "%s:%s", hostName, portName);
*/
    retAddress = (char*)(((internalEnv*)env->functions->reserved1)->alloc)(6 + 1); 
    if (retAddress == 0) {
        SetLastTranError(env, "out of memory", 0);
        return JDWPTRANSPORT_ERROR_OUT_OF_MEMORY;
    }
    sprintf(retAddress, "%d", ntohs(serverSockAddr.sin_port));

    *actualAddress = retAddress;

    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_StartListening

/**
 * This function implements jdwpTransportEnv::StopListening
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_StopListening(jdwpTransportEnv* env)
{
    SOCKET envServerSocket = ((internalEnv*)env->functions->reserved1)->envServerSocket;
    if (envServerSocket == INVALID_SOCKET) {
        return JDWPTRANSPORT_ERROR_NONE;
    }

    if (closesocket(envServerSocket) == SOCKET_ERROR) {
        SetLastTranError(env, "close socket failed", GetLastErrorStatus());
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    ((internalEnv*)env->functions->reserved1)->envServerSocket = INVALID_SOCKET;

    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_StopListening

/**
 * This function implements jdwpTransportEnv::Accept
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_Accept(jdwpTransportEnv* env, jlong acceptTimeout,
        jlong handshakeTimeout)
{
    if (acceptTimeout < 0) {
        SetLastTranError(env, "acceptTimeout timeout is negative", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    if (handshakeTimeout < 0) {
        SetLastTranError(env, "handshakeTimeout timeout is negative", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    SOCKET envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket != INVALID_SOCKET) {
        SetLastTranError(env, "there is already an open connection to the debugger", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    SOCKET envServerSocket = ((internalEnv*)env->functions->reserved1)->envServerSocket;
    if (envServerSocket == INVALID_SOCKET) {
        SetLastTranError(env, "transport is not currently in listen mode", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    struct sockaddr serverSockAddr;
    socklen_t len = sizeof(serverSockAddr);
    int res = getsockname(envServerSocket, &serverSockAddr, &len);
    if (res == SOCKET_ERROR) {
        SetLastTranError(env, "connection failed", GetLastErrorStatus());
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    jlong deadline = (acceptTimeout == 0) ? 0 : (jlong)GetTickCount() + acceptTimeout;
    jdwpTransportError err = SelectRead(env, envServerSocket, deadline);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return err;
    }

    SOCKET clientSocket = accept(envServerSocket, &serverSockAddr, &len);
    if (clientSocket == INVALID_SOCKET) {
        SetLastTranError(env, "socket accept failed", GetLastErrorStatus());
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    if (!SetSocketBlockingMode(env, clientSocket, false)) {
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    EnterCriticalSendSection(env);
    EnterCriticalReadSection(env);
    ((internalEnv*)env->functions->reserved1)->envClientSocket = clientSocket;

    err = CheckHandshaking(env, clientSocket, (long)handshakeTimeout);
    LeaveCriticalReadSection(env);
    LeaveCriticalSendSection(env);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        TCPIPSocketTran_Close(env);
        return err;
    }

    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_Accept

/**
 * This function implements jdwpTransportEnv::IsOpen
 */
static jboolean JNICALL 
TCPIPSocketTran_IsOpen(jdwpTransportEnv* env)
{
    SOCKET envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket == INVALID_SOCKET) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
} //TCPIPSocketTran_IsOpen

/**
 * This function read packet
 */
static jdwpTransportError
ReadPacket(jdwpTransportEnv* env, SOCKET envClientSocket, jdwpPacket* packet)
{
    jdwpTransportError err;
    int length;
    int readBytes = 0;
    err = ReceiveData(env, envClientSocket, (char *)&length, sizeof(jint), 0, &readBytes);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        if (readBytes == 0) {
            packet->type.cmd.len = 0;
            return JDWPTRANSPORT_ERROR_NONE;
        }
        return err;
    }

    packet->type.cmd.len = (jint)ntohl(length);
    
    int id;
    err = ReceiveData(env, envClientSocket, (char *)&(id), sizeof(jint));
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return err;
    }

    packet->type.cmd.id = (jint)ntohl(id);

    err = ReceiveData(env, envClientSocket, (char *)&(packet->type.cmd.flags), sizeof(jbyte));
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return err;
    }

    if (packet->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) {
        u_short errorCode;
        err = ReceiveData(env, envClientSocket, (char*)&(errorCode), sizeof(jshort));
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return err;
        }
        packet->type.reply.errorCode = (jshort)ntohs(errorCode); 
    } else {
        err = ReceiveData(env, envClientSocket, (char*)&(packet->type.cmd.cmdSet), sizeof(jbyte));
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return err;
        }
 
        err = ReceiveData(env, envClientSocket, (char*)&(packet->type.cmd.cmd), sizeof(jbyte));
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return err;
        }
    } //if

    int dataLength = packet->type.cmd.len - 11;
    if (dataLength < 0) {
        SetLastTranError(env, "invalid packet length received", 0);
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    } else if (dataLength == 0) {
        packet->type.cmd.data = 0;
    } else {
        packet->type.cmd.data = (jbyte*)(((internalEnv*)env->functions->reserved1)->alloc)(dataLength);
        if (packet->type.cmd.data == 0) {
            SetLastTranError(env, "out of memory", 0);
            return JDWPTRANSPORT_ERROR_OUT_OF_MEMORY;
        }
        err = ReceiveData(env, envClientSocket, (char *)packet->type.cmd.data, dataLength);
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            (((internalEnv*)env->functions->reserved1)->free)(packet->type.cmd.data);
            return err;
        }
    } //if
    return JDWPTRANSPORT_ERROR_NONE;
}

/**
 * This function implements jdwpTransportEnv::ReadPacket
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_ReadPacket(jdwpTransportEnv* env, jdwpPacket* packet)
{
    if (packet == 0) {
        SetLastTranError(env, "packet is 0", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    SOCKET envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket == INVALID_SOCKET) {
        SetLastTranError(env, "there isn't an open connection to a debugger", 0);
        LeaveCriticalReadSection(env);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    EnterCriticalReadSection(env);
    jdwpTransportError err = ReadPacket(env, envClientSocket, packet);
    LeaveCriticalReadSection(env);
    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_ReadPacket

/**
 * This function implements jdwpTransportEnv::WritePacket
 */
static jdwpTransportError 
WritePacket(jdwpTransportEnv* env, SOCKET envClientSocket, const jdwpPacket* packet)
{
    int packetLength = packet->type.cmd.len;
    if (packetLength < 11) {
        SetLastTranError(env, "invalid packet length", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    char* data = (char*)packet->type.cmd.data;
    if ((packetLength > 11) && (data == 0)) {
        SetLastTranError(env, "packet length is greater than 11 but the packet data field is 0", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    int dataLength = packetLength - 11;
    packetLength = htonl(packetLength);

    jdwpTransportError err;
    err = SendData(env, envClientSocket, (char*)&packetLength, sizeof(jint));
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return err;
    }

    int id = htonl(packet->type.cmd.id);

    err = SendData(env, envClientSocket, (char*)&id, sizeof(jint));
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return err;
    }

    err = SendData(env, envClientSocket, (char*)&(packet->type.cmd.flags), sizeof(jbyte));
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return err;
    }

    if (packet->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) {
        u_short errorCode = htons(packet->type.reply.errorCode);
        err = SendData(env, envClientSocket, (char*)&errorCode, sizeof(jshort));
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return err;
        }
    } else {
        err = SendData(env, envClientSocket, (char*)&(packet->type.cmd.cmdSet), sizeof(jbyte));
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return err;
        }
        err = SendData(env, envClientSocket, (char*)&(packet->type.cmd.cmd), sizeof(jbyte));
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return err;
        }
    } //if
    
    if (data != 0) {
        err = SendData(env, envClientSocket, data, dataLength);
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return err;
        }
    } //if
    return JDWPTRANSPORT_ERROR_NONE;
}

/**
 * This function send packet
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_WritePacket(jdwpTransportEnv* env, const jdwpPacket* packet)
{

    if (packet == 0) {
        SetLastTranError(env, "packet is 0", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    SOCKET envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket == INVALID_SOCKET) {
        SetLastTranError(env, "there isn't an open connection to a debugger", 0);
        LeaveCriticalSendSection(env);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE;
    }

    EnterCriticalSendSection(env);
    jdwpTransportError err = WritePacket(env, envClientSocket, packet);
    LeaveCriticalSendSection(env);
    return err;
} //TCPIPSocketTran_WritePacket

/**
 * This function implements jdwpTransportEnv::GetLastError
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_GetLastError(jdwpTransportEnv* env, char** message)
{
    *message = ((internalEnv*)env->functions->reserved1)->lastError->GetLastErrorMessage();
    if (*message == 0) {
        return JDWPTRANSPORT_ERROR_MSG_NOT_AVAILABLE;
    }
    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_GetLastError

/**
 * This function must be called by agent when the library is loaded
 */
extern "C" JNIEXPORT jint JNICALL 
jdwpTransport_OnLoad(JavaVM *vm, jdwpTransportCallback* callback,
             jint version, jdwpTransportEnv** env)
{
    if (version != JDWPTRANSPORT_VERSION_1_0) {
        return JNI_EVERSION;
    }

    internalEnv* iEnv = (internalEnv*)callback->alloc(sizeof(internalEnv));
    if (iEnv == 0) {
        return JNI_ENOMEM;
    }
    iEnv->jvm = vm;
    iEnv->alloc = callback->alloc;
    iEnv->free = callback->free;
    iEnv->lastError = 0;
    iEnv->envClientSocket = INVALID_SOCKET;
    iEnv->envServerSocket = INVALID_SOCKET;

    jdwpTransportNativeInterface_* envTNI = (jdwpTransportNativeInterface_*)callback
        ->alloc(sizeof(jdwpTransportNativeInterface_));
    if (envTNI == 0) {
        callback->free(iEnv);
        return JNI_ENOMEM;
    }

    envTNI->GetCapabilities = &TCPIPSocketTran_GetCapabilities;
    envTNI->Attach = &TCPIPSocketTran_Attach;
    envTNI->StartListening = &TCPIPSocketTran_StartListening;
    envTNI->StopListening = &TCPIPSocketTran_StopListening;
    envTNI->Accept = &TCPIPSocketTran_Accept;
    envTNI->IsOpen = &TCPIPSocketTran_IsOpen;
    envTNI->Close = &TCPIPSocketTran_Close;
    envTNI->ReadPacket = &TCPIPSocketTran_ReadPacket;
    envTNI->WritePacket = &TCPIPSocketTran_WritePacket;
    envTNI->GetLastError = &TCPIPSocketTran_GetLastError;
    envTNI->reserved1 = iEnv;

    _jdwpTransportEnv* resEnv = (_jdwpTransportEnv*)callback
        ->alloc(sizeof(_jdwpTransportEnv));
    if (resEnv == 0) {
        callback->free(iEnv);
        callback->free(envTNI);
        return JNI_ENOMEM;
    }

    resEnv->functions = envTNI;
    *env = resEnv;

    InitializeCriticalSections(resEnv);

    return JNI_OK;
} //jdwpTransport_OnLoad

/**
 * This function may be called by agent before the library unloading.
 * The function is not defined in JDWP Transport Interface specification.
 */
extern "C" JNIEXPORT void JNICALL 
jdwpTransport_UnLoad(jdwpTransportEnv** env)
{
    DeleteCriticalSections(*env);
    TCPIPSocketTran_Close(*env);
    TCPIPSocketTran_StopListening(*env);
    void (*unLoadFree)(void *buffer) = ((internalEnv*)(*env)->functions->reserved1)->free;
    if (((internalEnv*)(*env)->functions->reserved1)->lastError != 0){
        delete (((internalEnv*)(*env)->functions->reserved1)->lastError);
    }
    unLoadFree((void*)(*env)->functions->reserved1);
    unLoadFree((void*)(*env)->functions);
    unLoadFree((void*)(*env));
} //jdwpTransport_UnLoad

