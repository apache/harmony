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
 * This is implementation of JDWP Agent TCP/IP Socket transport.
 * Main module.
 */

#ifndef USING_VMI
#define USING_VMI
#endif

#if defined(ZOS)
#define _XOPEN_SOURCE  500
#include <unistd.h>
#endif

#include <stdlib.h>
#include <string.h>

#include "SocketTransport_pd.h"
#include "hysocket.h"
#include "hysock.h"

#define READ_BUFFER_SIZE 32
#define WRITE_BUFFER_SIZE 64

static jbyte read_buffer[READ_BUFFER_SIZE];
static char write_buffer[WRITE_BUFFER_SIZE];

/**
 * Returns the error status for the last failed operation. 
 */
static int
GetLastErrorStatus(jdwpTransportEnv* env)
{
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    PORT_ACCESS_FROM_JAVAVM(ienv->jvm);
    return hyerror_last_error_number();
} // GetLastErrorStatus

/**
 * Retrieves the number of milliseconds, substitute for the corresponding Win32 
 * function.
 */
static long 
GetTickCount(jdwpTransportEnv* env)
{
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    PORT_ACCESS_FROM_JAVAVM(ienv->jvm);
    return (long)hytime_current_time_millis();
} // GetTickCount

/**
 * Initializes critical section lock objects.
 */
static inline void
InitializeCriticalSections(jdwpTransportEnv* env)
{
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    JavaVM *vm = ienv->jvm;
#ifdef HY_NO_THR
    THREAD_ACCESS_FROM_JAVAVM(vm);
#endif /* HY_NO_THR */
    hythread_attach(NULL);

    UDATA flags = 0;
    if (hythread_monitor_init(&(ienv->readLock), 1) != 0) {
        printf("initial error\n");
    }

    if (hythread_monitor_init(&(ienv->sendLock), 1) != 0) {
        printf("initial error\n");
    }
    
} //InitializeCriticalSections()

/**
 * Releases all resources used by critical-section lock objects.
 */
static inline void
DeleteCriticalSections(jdwpTransportEnv* env)
{
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    JavaVM *vm = ienv->jvm;
#ifdef HY_NO_THR
    THREAD_ACCESS_FROM_JAVAVM(vm);
#endif /* HY_NO_THR */

    hythread_attach(NULL);
    hythread_monitor_destroy(ienv->readLock);
    hythread_monitor_destroy(ienv->sendLock);
} //DeleteCriticalSections()

/**
 * Waits for ownership of the send critical-section object.
 */
static inline void
EnterCriticalSendSection(jdwpTransportEnv* env)
{
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    JavaVM *vm = ienv->jvm;
#ifdef HY_NO_THR
    THREAD_ACCESS_FROM_JAVAVM(vm);
#endif /* HY_NO_THR */

    hythread_attach(NULL);
    hythread_monitor_enter(ienv->sendLock);
} //EnterCriticalSendSection()

/**
 * Waits for ownership of the read critical-section object.
 */
static inline void
EnterCriticalReadSection(jdwpTransportEnv* env)
{
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    JavaVM *vm = ienv->jvm;
#ifdef HY_NO_THR
    THREAD_ACCESS_FROM_JAVAVM(vm);
#endif /* HY_NO_THR */

    hythread_attach(NULL);
    hythread_monitor_enter(ienv->readLock);
} //EnterCriticalReadSection()

/**
 * Releases ownership of the read critical-section object.
 */
static inline void
LeaveCriticalReadSection(jdwpTransportEnv* env)
{
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    JavaVM *vm = ienv->jvm;
#ifdef HY_NO_THR
    THREAD_ACCESS_FROM_JAVAVM(vm);
#endif /* HY_NO_THR */

    hythread_attach(NULL);
    hythread_monitor_exit(ienv->readLock);
} //LeaveCriticalReadSection()

/**
 * Releases ownership of the send critical-section object.
 */
static inline void
LeaveCriticalSendSection(jdwpTransportEnv* env)
{
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    JavaVM *vm = ienv->jvm;
#ifdef HY_NO_THR
    THREAD_ACCESS_FROM_JAVAVM(vm);
#endif /* HY_NO_THR */

    hythread_attach(NULL);
    hythread_monitor_exit(ienv->sendLock);
} //LeaveCriticalSendSection()


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
        ienv->lastError = new(ienv->alloc, ienv->free) LastTransportError(ienv->jvm, messagePtr, errorStatus, ienv->alloc, ienv->free);
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
static const jint select_timeout = 5000; // wait cycle in milliseconds 

/**
 * This function enable/disables socket blocking mode 
 */
static bool 
SetSocketBlockingMode(jdwpTransportEnv* env, hysocket_t sckt, bool isBlocked)
{
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);

    jint ret = hysock_set_nonblocking(sckt, isBlocked ? FALSE : TRUE);
    if (ret != 0){
	SetLastTranError(env, "socket error", GetLastErrorStatus(env));
        return false;
    }
    return true;

} // SetSocketBlockingMode()

/**
 * This function is used to determine the read status of socket (in terms of select function).
 * The function avoids absolutely blocking select
 */
static jdwpTransportError 
SelectRead(jdwpTransportEnv* env, hysocket_t sckt, jlong deadline = 0) {
    internalEnv* ienv = (internalEnv*)env->functions->reserved1;
    PORT_ACCESS_FROM_JAVAVM(ienv->jvm);

    if (deadline < 0)
        return JDWPTRANSPORT_ERROR_NONE;

    // default to 5s but don't allow anything less than 1s
    deadline =
      deadline == 0 ? select_timeout : (deadline < 1000 ? 1000 : deadline);
    jint ret = hysock_select_read(sckt, (I_32) deadline / 1000 , (I_32) deadline % 1000, FALSE);
    if (ret == 1){
        return JDWPTRANSPORT_ERROR_NONE; //timeout is not occurred
    }
    if (ret != HYPORT_ERROR_SOCKET_TIMEOUT){
        SetLastTranError(env, "socket error", ret);

        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }
    SetLastTranError(env, "timeout occurred", 0);
    return JDWPTRANSPORT_ERROR_TIMEOUT; //timeout occurred
} // SelectRead

/**
 * This function is used to determine the send status of socket (in terms of select function).
 * The function avoids absolutely blocking select
 */
static jdwpTransportError 
SelectSend(jdwpTransportEnv* env, hysocket_t sckt, jlong deadline = 0) {
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);

    hyfdset_struct hyfdSet;
    deadline = deadline == 0 ? 20000 : deadline;
    
    I_32 secTime = (long)(deadline / 1000);
    I_32 uTime = (long)(deadline % 1000);

    hytimeval_struct timeval;

    hysock_fdset_zero(&hyfdSet);
    hysock_fdset_set(sckt,&hyfdSet);

    int ret = hysock_timeval_init(secTime,uTime,&timeval);

    ret =  hysock_select(hysock_fdset_size(sckt),NULL,&hyfdSet,NULL,&timeval);

    if (ret > 0){
        return JDWPTRANSPORT_ERROR_NONE; //timeout has not occurred
    }
    if (ret != HYPORT_ERROR_SOCKET_TIMEOUT){
    	 SetLastTranError(env, "socket error", ret);
         return JDWPTRANSPORT_ERROR_IO_ERROR;
    }
    SetLastTranError(env, "timeout occurred", 0);
    return JDWPTRANSPORT_ERROR_TIMEOUT; //timeout occurred
} // SelectSend

/**
 * This function sends data on a connected socket
 */
static jdwpTransportError
SendData(jdwpTransportEnv* env, hysocket_t sckt, const char* data, int dataLength, jlong deadline = 0)
{
    long left = dataLength;
    long off = 0;
    int ret;

    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);

    // Check if block
    while (left > 0){
#ifndef ZOS	    
        jdwpTransportError err = SelectSend(env, sckt, deadline);
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            return err;
        }
#endif
#ifdef ZOS
	if (!SetSocketBlockingMode(env, sckt, true)) {
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
#endif	
	ret = hysock_write (sckt, (U_8 *)data+off, left, HYSOCK_NOFLAGS);
#ifdef ZOS
	if (!SetSocketBlockingMode(env, sckt, false)) {
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
#endif	
	if (ret < 0){
                SetLastTranError(env, "socket error", ret);
                return JDWPTRANSPORT_ERROR_IO_ERROR; 
	}
	left -= ret;
	off += ret;
    }    
    return JDWPTRANSPORT_ERROR_NONE;
} //SendData

/**
 * This function receives data from a connected socket
 */
static jdwpTransportError
ReceiveData(jdwpTransportEnv* env, hysocket_t sckt, U_8 * buffer, int dataLength, jlong deadline = 0, int* readByte = 0)
{
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);

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

	if (!SetSocketBlockingMode(env, sckt, true)) {
            return JDWPTRANSPORT_ERROR_IO_ERROR;
    }
 	ret = hysock_read(sckt, (U_8 *) (buffer + off), left, HYSOCK_NOFLAGS);
	if (!SetSocketBlockingMode(env, sckt, false)) {
            return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

        if (ret < 0) {
            SetLastTranError(env, "data receiving failed", ret);
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
        if (ret == 0) {
            SetLastTranError(env, "premature EOF", HYSOCK_NOFLAGS);
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
 * This function performes handshake procedure
 */
static jdwpTransportError 
CheckHandshaking(jdwpTransportEnv* env, hysocket_t sckt, jlong handshakeTimeout)
{
    const char* handshakeString = "JDWP-Handshake";
    U_8 receivedString[14]; //length of "JDWP-Handshake"
    jlong timeout = handshakeTimeout == 0 ? 30000 : handshakeTimeout;

    jdwpTransportError err;
    err = SendData(env, sckt, handshakeString, (int)strlen(handshakeString), handshakeTimeout);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        SetLastTranErrorMessagePrefix(env, "'JDWP-Handshake' sending error: ");
        return err;
    }
 
    err = ReceiveData(env, sckt, receivedString, (int)strlen(handshakeString),
                      timeout);
 
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
DecodeAddress(jdwpTransportEnv* env, const char *address, hysockaddr_t sa, bool isServer) 
{
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);
    const char * localhost = "127.0.0.1";
    const char * anyhost = "0.0.0.0";

    if ((address == 0) || (*address == 0)) {  //empty address
        hysock_sockaddr(sa,  isServer ? anyhost : localhost, 0);
        return JDWPTRANSPORT_ERROR_NONE;
    }

    char *finalAddress = (char*)(((internalEnv*)env->functions->reserved1)->alloc)((jint)strlen(address)+1);
    if (finalAddress == 0) {
        SetLastTranError(env, "out of memory", 0);
        return JDWPTRANSPORT_ERROR_OUT_OF_MEMORY;
    }
    memcpy(finalAddress, address, strlen(address)+1);

#ifdef ZOS
    __atoe(finalAddress);
#endif

#ifdef ZOS
// Ensure that the ':' is in EBCDIC on zOS platforms
#pragma convlit(suspend)
#endif
    const char* colon = strchr(finalAddress, ':');
#ifdef ZOS
#pragma convlit(resume)
#endif
    if (colon == 0) {  //address is like "port"
        hysock_sockaddr(sa,  isServer ? anyhost : localhost,  hysock_htons((U_16)atoi(finalAddress)));
    } else { //address is like "host:port"
        char *hostName = (char*)(((internalEnv*)env->functions->reserved1)
            ->alloc)((jint)(colon - finalAddress + 1));
        if (hostName == 0) {
            SetLastTranError(env, "out of memory", 0);
            (((internalEnv*)env->functions->reserved1)->free)(finalAddress);
            return JDWPTRANSPORT_ERROR_OUT_OF_MEMORY;
        }
        memcpy(hostName, address, colon - finalAddress); // Use address here, not finalAddress, as we want to keep the string ASCII on zOS
        hostName[colon - finalAddress] = '\0';
        int ret = hysock_sockaddr(sa,  hostName, hysock_htons((U_16)atoi(colon + 1)));
        if (ret != 0){
                SetLastTranError(env, "unable to resolve host name", 0);
                (((internalEnv*)env->functions->reserved1)->free)(hostName);
                (((internalEnv*)env->functions->reserved1)->free)(finalAddress);
                return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
        (((internalEnv*)env->functions->reserved1)->free)(hostName);
    } //if
    (((internalEnv*)env->functions->reserved1)->free)(finalAddress);

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
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);
    hysocket_t envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket == NULL) {
        return JDWPTRANSPORT_ERROR_NONE;
    }

    ((internalEnv*)env->functions->reserved1)->envClientSocket = NULL;
    if (hysock_socketIsValid(envClientSocket)==0){
        return JDWPTRANSPORT_ERROR_NONE;
    }

    int err;
    err = hysock_shutdown_input(envClientSocket);
    if (err == 0){
	 err = hysock_shutdown_output(envClientSocket);
    }
    
    err = hysock_close(&envClientSocket);
    if (err != 0) {
        SetLastTranError(env, "close socket failed", GetLastErrorStatus(env));
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }
    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_Close

/**
 * This function sets socket options SO_REUSEADDR and TCP_NODELAY
 */
static bool 
SetSocketOptions(jdwpTransportEnv* env, hysocket_t sckt) 
{
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);

    BOOLEAN isOn = TRUE;

#ifndef ZOS
    if (hysock_setopt_bool(sckt, HY_SOL_SOCKET, HY_SO_REUSEADDR, &isOn) != 0){
        SetLastTranError(env, "setsockopt(SO_REUSEADDR) failed", GetLastErrorStatus(env));
        return false;
    }
    if (hysock_setopt_bool(sckt, HY_IPPROTO_TCP, HY_TCP_NODELAY,  &isOn) != 0) {
        SetLastTranError(env, "setsockopt(TCPNODELAY) failed", GetLastErrorStatus(env));
        return false;
    }
#endif

    return true;
} // SetSocketOptions()

/**
 * This function implements jdwpTransportEnv::Attach
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_Attach(jdwpTransportEnv* env, const char* address,
        jlong attachTimeout, jlong handshakeTimeout)
{
    internalEnv *ienv = (internalEnv*)env->functions->reserved1;
    PORT_ACCESS_FROM_JAVAVM(ienv->jvm);

    hysocket_t clientSocket;  
    hysockaddr_struct serverSockAddr;  
                                   
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

    hysocket_t envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket != NULL) {
        SetLastTranError(env, "there is already an open connection to the debugger", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    hysocket_t envServerSocket = ((internalEnv*)env->functions->reserved1)->envServerSocket;
    if (envServerSocket != NULL) {
        SetLastTranError(env, "transport is currently in listen mode", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    jdwpTransportError res = DecodeAddress(env, address, &serverSockAddr, false);
    if (res != JDWPTRANSPORT_ERROR_NONE) {
        return res;
    }

    int ret = hysock_socket(&clientSocket, HYSOCK_AFINET, HYSOCK_STREAM, HYSOCK_DEFPROTOCOL);
	   // socket(AF_INET, SOCK_STREAM, 0);
    if (ret != 0) {
        SetLastTranError(env, "unable to create socket", GetLastErrorStatus(env));
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }
    
    if (!SetSocketOptions(env, clientSocket)) {
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    if (attachTimeout == 0) {
        if (!SetSocketBlockingMode(env, clientSocket, true)) {
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
        int err = hysock_connect(clientSocket, &serverSockAddr);

	if (err != 0 ) {
            SetLastTranError(env, "connection failed", GetLastErrorStatus(env));
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
        int err = hysock_connect(clientSocket, &serverSockAddr);
        if (err != 0) {
            //HYPORT_ERROR_SOCKET_EINPROGRESS will be returned when the non-blocking socket would wait for estabish
            if (err != HYPORT_ERROR_SOCKET_WOULDBLOCK && err != HYPORT_ERROR_SOCKET_EINPROGRESS) {
                SetLastTranError(env, "connection failed", GetLastErrorStatus(env));
                return JDWPTRANSPORT_ERROR_IO_ERROR;
            } else {
                int ret = SelectSend(env, clientSocket, attachTimeout);
		if (ret != JDWPTRANSPORT_ERROR_NONE){
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
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);

    hysocket_t envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket != NULL) {
        SetLastTranError(env, "there is already an open connection to the debugger", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    hysocket_t envServerSocket = ((internalEnv*)env->functions->reserved1)->envServerSocket;
    if (envServerSocket != NULL) {
        SetLastTranError(env, "transport is currently in listen mode", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    jdwpTransportError res;
    hysockaddr_struct serverSockAddr;
    res = DecodeAddress(env, address, &serverSockAddr, true);
    if (res != JDWPTRANSPORT_ERROR_NONE) {
        return res;
    }

    hysocket_t serverSocket;
    int ret = hysock_socket(&serverSocket, HYSOCK_AFINET, HYSOCK_STREAM, HYSOCK_DEFPROTOCOL); //socket(AF_INET, SOCK_STREAM, 0);
    if (ret != 0) {
        SetLastTranError(env, "unable to create socket", GetLastErrorStatus(env));
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    if (!SetSocketOptions(env, serverSocket)) {
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    int err;

    err = hysock_bind (serverSocket, &serverSockAddr);
    // bind(serverSocket, (struct sockaddr *)&serverSockAddr, sizeof(serverSockAddr));
    if (err != 0 ) {
        SetLastTranError(env, "binding to port failed", GetLastErrorStatus(env));
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    err = hysock_listen(serverSocket, 100);
    if (err != 0) {
        SetLastTranError(env, "listen start failed", GetLastErrorStatus(env));
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    if (!SetSocketBlockingMode(env, serverSocket, false)) {
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    ((internalEnv*)env->functions->reserved1)->envServerSocket = serverSocket;

    err = hysock_getsockname(serverSocket, &serverSockAddr);
    if (err != 0) {
        SetLastTranError(env, "socket error", GetLastErrorStatus(env));
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    char* retAddress = 0;

    retAddress = (char*)(((internalEnv*)env->functions->reserved1)->alloc)(6 + 1); 
    if (retAddress == 0) {
        SetLastTranError(env, "out of memory", 0);
        return JDWPTRANSPORT_ERROR_OUT_OF_MEMORY;
    }
    // print server port
    hystr_printf(privatePortLibrary, retAddress, 7, "%d",hysock_ntohs(hysock_sockaddr_port(&serverSockAddr)));

    *actualAddress = retAddress;

    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_StartListening

/**
 * This function implements jdwpTransportEnv::StopListening
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_StopListening(jdwpTransportEnv* env)
{
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);

    hysocket_t envServerSocket = ((internalEnv*)env->functions->reserved1)->envServerSocket;
    if (envServerSocket == NULL) {
        return JDWPTRANSPORT_ERROR_NONE;
    }

    int err = hysock_close(&envServerSocket);
    ((internalEnv*)env->functions->reserved1)->envServerSocket = NULL;
    
    if (err != 0) {
        SetLastTranError(env, "close socket failed", GetLastErrorStatus(env));
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    return JDWPTRANSPORT_ERROR_NONE;
} //TCPIPSocketTran_StopListening

/**
 * This function implements jdwpTransportEnv::Accept
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_Accept(jdwpTransportEnv* env, jlong acceptTimeout,
        jlong handshakeTimeout)
{
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);

    if (acceptTimeout < 0) {
        SetLastTranError(env, "acceptTimeout timeout is negative", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    if (handshakeTimeout < 0) {
        SetLastTranError(env, "handshakeTimeout timeout is negative", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    hysocket_t envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket != NULL) {
        SetLastTranError(env, "there is already an open connection to the debugger", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    hysocket_t envServerSocket = ((internalEnv*)env->functions->reserved1)->envServerSocket;
    if (envServerSocket == NULL) {
        SetLastTranError(env, "transport is not currently in listen mode", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    hysockaddr_struct serverSockAddr;

    hysocket_t clientSocket;
    U_8 nlocalAddrBytes[HYSOCK_INADDR_LEN];
    I_32 ret = hysock_sockaddr_init6(&serverSockAddr, nlocalAddrBytes, HYSOCK_INADDR_LEN, HYADDR_FAMILY_AFINET4, hysock_sockaddr_port(&serverSockAddr), 0, 0, envServerSocket);


#ifndef WIN32
    // workaround for some platform cannot close a socket blocking on accept
    do{
        ret = hysock_select_read(envServerSocket, 1, 0, TRUE);
        envServerSocket = ((internalEnv*)env->functions->reserved1)->envServerSocket;
    } while (ret == HYPORT_ERROR_SOCKET_TIMEOUT && envServerSocket != NULL);
    if (ret != 1){
        SetLastTranError(env, "socket accept failed or closed", GetLastErrorStatus(env));
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }
    if (envServerSocket == NULL) {
        SetLastTranError(env, "Server socket has been closed", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE;
    }
#endif

    SetSocketBlockingMode(env, envServerSocket, true);
    ret = hysock_accept(envServerSocket, &serverSockAddr, &clientSocket);
    SetSocketBlockingMode(env, envServerSocket, false);

    if (ret != 0) {
        SetLastTranError(env, "socket accept failed", GetLastErrorStatus(env));
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    if (!SetSocketBlockingMode(env, clientSocket, false)) {
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    EnterCriticalSendSection(env);
    EnterCriticalReadSection(env);
    ((internalEnv*)env->functions->reserved1)->envClientSocket = clientSocket;
    jdwpTransportError err = CheckHandshaking(env, clientSocket, (long)handshakeTimeout);
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
    hysocket_t envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket == NULL) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
} //TCPIPSocketTran_IsOpen

/**
 * This function read packet
 */
static jdwpTransportError
ReadPacket(jdwpTransportEnv* env, hysocket_t envClientSocket, jdwpPacket* packet)
{
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);

    jdwpTransportError err;
    int length;
    int readBytes = 0;
    err = ReceiveData(env, envClientSocket, (U_8 *)&length, sizeof(jint), -1, &readBytes);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        if (readBytes == 0) {
            packet->type.cmd.len = 0;
            return JDWPTRANSPORT_ERROR_NONE;
        }
        return err;
    }
    packet->type.cmd.len = (jint)hysock_ntohl(length);

    bool isUseBuffer = false;
    jbyte* buffer = 0;
    if (packet->type.cmd.len - 4 > READ_BUFFER_SIZE) {
        buffer = (jbyte*)(((internalEnv*)env->functions->reserved1)->alloc)(packet->type.cmd.len - 4);
        isUseBuffer = false;
    } else {
        buffer = read_buffer;
        isUseBuffer = true;
    }

    err = ReceiveData(env, envClientSocket, (U_8 *)buffer, packet->type.cmd.len - 4);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return err;
    }

    int offset = 0;

    int id;
    memcpy(&id, buffer + offset, sizeof(jint));
    offset += sizeof(jint);
    packet->type.cmd.id = (jint)hysock_ntohl(id);

    memcpy(&(packet->type.cmd.flags), buffer + offset, sizeof(jbyte));
    offset += sizeof(jbyte);

    if (packet->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) {
        int errorCode;
	memcpy(&errorCode, buffer + offset, sizeof(jshort));
	offset += sizeof(jshort);
        packet->type.reply.errorCode = (jshort)hysock_ntohs((U_16)errorCode);
    } else {
	memcpy(&(packet->type.cmd.cmdSet), buffer + offset, sizeof(jbyte));
	offset += sizeof(jbyte);

	memcpy(&(packet->type.cmd.cmd), buffer + offset, sizeof(jbyte));
	offset += sizeof(jbyte);
    } //if

    int dataLength = packet->type.cmd.len - 11;
    if (dataLength < 0) {
        SetLastTranError(env, "invalid packet length received", 0);
        if (!isUseBuffer) {
            (((internalEnv*)env->functions->reserved1)->free)(buffer);
        }
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    } else if (dataLength == 0) {
        packet->type.cmd.data = 0;
    } else {
        packet->type.cmd.data = (jbyte*)(((internalEnv*)env->functions->reserved1)->alloc)(dataLength);
        if (packet->type.cmd.data == 0) {
            SetLastTranError(env, "out of memory", 0);
        if (!isUseBuffer) {
            (((internalEnv*)env->functions->reserved1)->free)(buffer);
        }
            return JDWPTRANSPORT_ERROR_OUT_OF_MEMORY;
        }

	memcpy(packet->type.cmd.data, buffer + offset, dataLength);
	offset += dataLength;
    } //if

    if (!isUseBuffer) {
        (((internalEnv*)env->functions->reserved1)->free)(buffer);
    }
    return JDWPTRANSPORT_ERROR_NONE;
}

/**
 * This function implements jdwpTransportEnv::ReadPacket
 */
static jdwpTransportError JNICALL 
TCPIPSocketTran_ReadPacket(jdwpTransportEnv* env, jdwpPacket* packet)
{
    PORT_ACCESS_FROM_JAVAVM(((internalEnv*)env->functions->reserved1)->jvm);
    if (packet == 0) {
        SetLastTranError(env, "packet is 0", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    hysocket_t envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket == NULL) {
        SetLastTranError(env, "there isn't an open connection to a debugger", 0);
        LeaveCriticalReadSection(env);
        return JDWPTRANSPORT_ERROR_ILLEGAL_STATE ;
    }

    EnterCriticalReadSection(env);
    jdwpTransportError err = ReadPacket(env, envClientSocket, packet);
    LeaveCriticalReadSection(env);
    return err;
} //TCPIPSocketTran_ReadPacket

/**
 * This function implements jdwpTransportEnv::WritePacket
 */
static jdwpTransportError 
WritePacket(jdwpTransportEnv* env, hysocket_t envClientSocket, const jdwpPacket* packet)
{
    JavaVM *vm = ((internalEnv*)env->functions->reserved1)->jvm;
    PORT_ACCESS_FROM_JAVAVM(vm);
    jint packetLength = packet->type.cmd.len;
    if (packetLength < 11) {
        SetLastTranError(env, "invalid packet length", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    char* data = (char*)packet->type.cmd.data;
    if ((packetLength > 11) && (data == 0)) {
        SetLastTranError(env, "packet length is greater than 11 but the packet data field is 0", 0);
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    jint dataLength = packetLength - 11;
    bool isUseBuffer = false;

    char* buffer = 0;
    if (packetLength > WRITE_BUFFER_SIZE) {
        buffer = (char*)(((internalEnv*)env->functions->reserved1)->alloc)(packet->type.cmd.len);
        isUseBuffer = false;
    } else {
        buffer = write_buffer;
        isUseBuffer = true;
    }

    int offset = 0;

    packetLength = hysock_htonl(packetLength);
    memcpy(buffer + offset, &packetLength, sizeof(jint));
    offset += sizeof(jint);

    jint id = hysock_htonl (packet->type.cmd.id);
    memcpy(buffer + offset, &id, sizeof(jint));
    offset += sizeof(jint);

    memcpy(buffer + offset, &(packet->type.cmd.flags), sizeof(jbyte));
    offset += sizeof(jbyte);

    if (packet->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) {
        U_16 errorCode = hysock_htons((U_16)packet->type.reply.errorCode);
	memcpy(buffer + offset, &errorCode, sizeof(jshort));
	offset += sizeof(jshort);
    } else {
	memcpy(buffer + offset, &(packet->type.cmd.cmdSet), sizeof(jbyte));
	offset += sizeof(jbyte);

	memcpy(buffer + offset, &(packet->type.cmd.cmd), sizeof(jbyte));
	offset += sizeof(jbyte);
    } //if
    
    if (data != 0) {
	memcpy(buffer + offset, data, dataLength);
	offset += dataLength;
    } //if

    jdwpTransportError err;
    err = SendData(env, envClientSocket, buffer, packet->type.cmd.len);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        if (!isUseBuffer) {
            (((internalEnv*)env->functions->reserved1)->free)(buffer);
        }
	return err;
    }

    if (!isUseBuffer) {
        (((internalEnv*)env->functions->reserved1)->free)(buffer);
    }
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

    hysocket_t envClientSocket = ((internalEnv*)env->functions->reserved1)->envClientSocket;
    if (envClientSocket == NULL) {
        SetLastTranError(env, "there isn't an open connection to a debugger", 0);
        //LeaveCriticalSendSection(env);
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
    iEnv->envClientSocket = NULL;
    iEnv->envServerSocket = NULL;

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


