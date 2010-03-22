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
 * for types, constants, include statements and functions for the Linux platform.
 */

#ifndef _SOCKETTRANSPORT_PD_H
#define _SOCKETTRANSPORT_PD_H


#include "LastTransportError.h"
#include "vmi.h"
#include "hythread.h"
#include "hysock.h"
#include "hyport.h"

#include "jni.h"

//typedef pthread_mutex_t CriticalSection;
typedef int SOCKET;
//typedef pthread_t ThreadId_t;

#include "jdwpTransport.h"
#include "LastTransportError.h"
#include "SocketTransport.h"

typedef timeval TIMEVAL;
typedef int BOOL;

const int SOCKET_ERROR = -1;

#endif //_SOCKETTRANSPORT_PD_H
