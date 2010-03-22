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

// TransportManager_pd.cpp
//

/**
 * This header file includes platform depended definitions for types, 
 * constants, include statements and functions for Linux platform.
 */

#include <unistd.h>
#include <errno.h>
#include <string.h>
#include "TransportManager_pd.h"
#include "TransportManager.h"

using namespace jdwp;

const char* TransportManager::onLoadDecFuncName = "jdwpTransport_OnLoad";
const char* TransportManager::unLoadDecFuncName = "jdwpTransport_UnLoad";
const char TransportManager::pathSeparator = ':';

void TransportManager::StartDebugger(const char* command, int extra_argc, const char* extra_argv[]) throw(AgentException)
{
    JDWP_TRACE_ENTRY("StartDebugger(" << JDWP_CHECK_NULL(command) << ',' << extra_argc << ',' << extra_argv << ')');

    // allocate array for parsed arguments

    int max_argc = 250 + extra_argc;
    char** argv = static_cast<char**>(GetMemoryManager().Allocate((sizeof(char*) * (max_argc + 1)) JDWP_FILE_LINE));
    AgentAutoFree afva(argv JDWP_FILE_LINE);

    // parse command line
    int argc = 0;        
    int cmd_len = strlen(command);
    char* cmd = static_cast<char*>(GetMemoryManager().Allocate((cmd_len + 1) JDWP_FILE_LINE));
    AgentAutoFree afv(cmd JDWP_FILE_LINE);

    if (command != 0 && cmd_len > 0) {
        JDWP_TRACE_PROG("StartDebugger: parse: cmd=" << JDWP_CHECK_NULL(command));

        const char *arg = command, *p;
        char *arg1 = cmd, *s;

        for (; *arg != 0;) {

            // skip initial spaces
            while (isspace(*arg)) arg++;

            // parse non-spaced or quoted argument 
            for (p = arg, s = arg1; ; p++) {
                // check for quote
                if (*p == '\"' || *p == '\'') {
                     char quote = *(p++);
                     // skip all chars until terminating quote or null
                     for (; *p != '\0'; p++) {
                         // check for terminating quote
                         if (*p == quote) {
                             p++;
                             break;
                         }
                         // preserve escaped quote
                         if (*p == '\\' && *(p + 1) == quote) 
                             p++;
                         // store current char
                         *(s++) = *p;
                     }
                }
                // check for ending separator
                if (*p == '\0' || isspace(*p)) {
                     *(s++) = '\0';
                     if (argc >= max_argc) {
                        JDWP_ERROR("Too many arguments for launching debugger proccess: " << argc);
                        throw AgentException(JDWP_ERROR_INTERNAL);
                     }
                     argv[argc++] = arg1;
                     JDWP_TRACE_PROG("StartDebugger: launch: arg[" << argc << "]=" << JDWP_CHECK_NULL(arg1));
                     arg = p;
                     arg1 = s;
                     break;
                }
                // skip escaped quote
                if (*p == '\\' && (*(p + 1) == '\"' || *(p + 1) == '\'')) 
                    p++;
                // store current char
                *(s++) = *p;
            }
        }
    }

    // add extra arguments

    int i;
    for (i = 0; i < extra_argc; i++) {
         if (extra_argv[i] != 0) {
             JDWP_TRACE_PROG("StartDebugger: launch: arg[" << argc << "]=" << JDWP_CHECK_NULL(extra_argv[i]));
             argv[argc++] = const_cast<char*>(extra_argv[i]);
         }
    }
    argv[argc] = 0;
    
    // launch debugger process

    int pid = fork();
    if (pid == -1) {
        JDWP_ERROR("Failed to fork debugger process: error=" << errno);
        throw AgentException(JDWP_ERROR_INTERNAL);
    } else if (pid == 0) {
        // execute debugger in child process
        execv(argv[0], argv);
        // returned here only in case of error, terminate child process
        JDWP_DIE("Failed to execute debugger process: error=" << errno);
    } else {
        JDWP_TRACE_PROG("StartDebugger: launched: pid=" << pid);
    }
}

ProcPtr jdwp::GetProcAddress(LoadedLibraryHandler libHandler, const char* procName)
{
    JDWP_TRACE_ENTRY("GetProcAddress(" << libHandler << ',' << JDWP_CHECK_NULL(procName) << ')');

    dlerror();
    ProcPtr res = (ProcPtr)dlsym(libHandler, procName);
    const char* errorMessage = dlerror();
    if (errorMessage) {
        JDWP_TRACE_PROG("FreeLibrary: getting library entry failed (error: " << errorMessage << ")");
    }
    return res;
}

bool jdwp::FreeLibrary(LoadedLibraryHandler libHandler)
{
    JDWP_TRACE_ENTRY("FreeLibrary(" << libHandler << ')');

    dlerror();
    if (dlclose(libHandler) != 0) {
        JDWP_TRACE_PROG("FreeLibrary: closing library failed (error: " << dlerror() << ")");
        return false;
    }
    return true;
}

LoadedLibraryHandler TransportManager::LoadTransport(const char* dirName, const char* transportName)
{
    JDWP_TRACE_ENTRY("LoadTransport(" << JDWP_CHECK_NULL(dirName) << ',' << JDWP_CHECK_NULL(transportName) << ')');

    JDWP_ASSERT(transportName != 0);
    dlerror();
    char* transportFullName = 0;
    if (dirName == 0) {
        size_t length = strlen(transportName) + 7;
        transportFullName = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        sprintf(transportFullName, "lib%s.so", transportName);
    } else {
        size_t length = strlen(dirName) + strlen(transportName) + 8;
        transportFullName = static_cast<char *>(GetMemoryManager().Allocate(length JDWP_FILE_LINE));
        sprintf(transportFullName, "%s/lib%s.so", dirName, transportName);
    }
    AgentAutoFree afv(transportFullName JDWP_FILE_LINE);
    LoadedLibraryHandler res = dlopen(transportFullName, RTLD_LAZY);
    if (res == 0) {
        JDWP_TRACE_PROG("LoadTransport: loading library " << transportFullName << " failed (error: " << dlerror() << ")");
    } else {
        JDWP_TRACE_PROG("LoadTransport: transport library " << transportFullName << " loaded");
    }
    return res;
}
