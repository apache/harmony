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

/**
 * @file
 * SocketTransport.h
 *
 * Internal data for the jdwp transport environment.
 * The given TCP/IP Socket transport supports multiple environments, 
 * so the struct is allocated for each environment.
 */

#ifndef _SOCKETTRANSPORT_H
#define _SOCKETTRANSPORT_H

struct internalEnv {
    JavaVM *jvm;                    // the JNI invocation interface, provided 
                                    // by the agent 
    void* (*alloc)(jint numBytes);  // the function allocating an area of memory, 
                                    // provided by the agent 
    void (*free)(void *buffer);     // the function deallocating an area of memory, 
                                    // provided by the agent
    SOCKET envClientSocket;         // the client socket, INVALID_SOCKET if closed
    SOCKET envServerSocket;         // the server socket, INVALID_SOCKET if closed
    LastTransportError* lastError;  // last errors
    CriticalSection readLock;       // the critical-section lock object for socket
                                    // read operations
    CriticalSection sendLock;       // the critical-section lock object for socket
                                    // send operations
};

#endif // _SOCKETTRANSPORT_H

