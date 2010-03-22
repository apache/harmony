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
 * @file
 * SocketTransport_pd.h
 *
 * The given header file includes platform depended declarations and definitions 
 * for types, constants, include statements and functions for the Win32 platform.
 */

#ifndef _SOCKETTRANSPORT_PD_H
#define _SOCKETTRANSPORT_PD_H

#if defined(_WINSOCKAPI_) && !defined(_WINSOCK2API_)
#undef _WINSOCKAPI_
#endif

#include <Winsock2.h>
#include <Ws2tcpip.h>

#include "jdwpTransport.h"
#include "vmi.h"
#include "hyport.h"
#include "LastTransportError.h"
#include "SocketTransport.h"
#include "jni.h"

typedef int socklen_t;

const int SOCKETWOULDBLOCK = WSAEWOULDBLOCK;
const int SOCKET_ERROR_EINTR = WSAEINTR;

/**
 * Returns the error status for the last failed operation. 
 */
static inline int
GetLastErrorStatus()
{
    return WSAGetLastError();
} //GetLastErrorStatus()
#endif // _SOCKETTRANSPORT_PD_Hi
