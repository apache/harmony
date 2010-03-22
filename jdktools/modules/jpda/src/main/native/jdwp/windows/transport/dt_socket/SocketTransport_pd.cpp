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
// SocketTransport_pd.cpp
//

#include "SocketTransport_pd.h"

/**
 * This function the optional entry point into a dynamic-link library (DLL).
 * It invokes WSAStartup() and WSACleanup() functions.
 */
BOOL APIENTRY 
DllMain(HANDLE hModule, DWORD  ul_reason_for_call, LPVOID lpReserved)
{
    WSADATA wsStartData;
    if (ul_reason_for_call == DLL_PROCESS_ATTACH) {
        WSAStartup(MAKEWORD(1,1), &wsStartData);
    } else if (ul_reason_for_call == DLL_PROCESS_DETACH) {
        WSACleanup();
    }
    return TRUE;
} //DllMain
