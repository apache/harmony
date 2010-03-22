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
 * This header file includes platform depended definitions for types, 
 * constants, include statements and functions for Win32 platform.
 */

// Windows Header Files:
#include <windows.h>

#include "TransportManager.h"
#include "ExceptionManager.h"
#include <process.h>

using namespace jdwp;

#ifdef _WIN64
    // for 64-bit Windows platform
    const char* TransportManager::onLoadDecFuncName = "jdwpTransport_OnLoad";
    const char* TransportManager::unLoadDecFuncName = "jdwpTransport_UnLoad";
#else
    // for 32-bit Windows platform
    const char* TransportManager::onLoadDecFuncName = "_jdwpTransport_OnLoad@16";
    const char* TransportManager::unLoadDecFuncName = "_jdwpTransport_UnLoad@4";
#endif // _WIN64

const char TransportManager::pathSeparator = ';';

int TransportManager::StartDebugger(const char* command, int extra_argc, const char* extra_argv[])
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "StartDebugger(%s,%d,%p)", JDWP_CHECK_NULL(command), extra_argc, extra_argv));

    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "StartDebugger: transport=%s", JDWP_CHECK_NULL(extra_argv[0])));
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "StartDebugger: address=%s", JDWP_CHECK_NULL(extra_argv[1])));

    // append extra arguments to command line

    int cmd_len = (int)strlen(command);
    int extra_len = 0;
    int i;

    for (i = 0; i < extra_argc; i++) {
         if (extra_argv[i] != 0) {
             extra_len += (int)strlen(extra_argv[i]) + 1;
         }
    }

    char* cmd = static_cast<char*>(GetMemoryManager().Allocate((cmd_len + extra_len + 1) JDWP_FILE_LINE));
    AgentAutoFree afv(cmd JDWP_FILE_LINE);
    strcpy(cmd, command);

    for (i = 0; i < extra_argc; i++) {
         if (extra_argv[i] != 0) {
             strcat(cmd, " ");
             strcat(cmd, extra_argv[i]);
         }
    }

    // launch debugger process

    STARTUPINFO si;
    PROCESS_INFORMATION pi;

    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    ZeroMemory(&pi, sizeof(pi));

    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "StartDebugger: launch: cmd=%s", JDWP_CHECK_NULL(cmd)));

    if(!CreateProcess(NULL, const_cast<LPSTR>(cmd), NULL, NULL, 
            TRUE, NULL, NULL, NULL, &si, &pi)) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Failed to launch debugger process: error=%d", GetLastError()));
        AgentException ex = AgentException(JDWP_ERROR_INTERNAL);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INTERNAL;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "StartDebugger: launched: pid=%d", pi.dwProcessId));

    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);

    return JDWP_ERROR_NONE;
}


