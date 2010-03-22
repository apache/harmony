/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#include <string.h>
#include "open/hythread_ext.h" // for correct Windows.h inclusion
#include "port_malloc.h"
#include "stack_dump.h"

#ifndef NO_DBGHELP
#include <dbghelp.h>
#pragma comment(linker, "/defaultlib:dbghelp.lib")
#endif


static char* g_curdir = NULL;
static char* g_cmdline = NULL;
static char* g_environ = NULL;

#ifndef NO_DBGHELP
typedef BOOL (WINAPI *SymFromAddr_type)
   (IN  HANDLE              hProcess,
    IN  DWORD64             Address,
    OUT PDWORD64            Displacement,
    IN OUT PSYMBOL_INFO     Symbol    );
typedef BOOL (WINAPI *SymGetLineFromAddr64_type)
   (IN  HANDLE                  hProcess,
    IN  DWORD64                 qwAddr,
    OUT PDWORD                  pdwDisplacement,
    OUT PIMAGEHLP_LINE64        Line64);

typedef BOOL (WINAPI *SymGetLineFromAddr_type)
   (IN  HANDLE                hProcess,
    IN  DWORD                 dwAddr,
    OUT PDWORD                pdwDisplacement,
    OUT PIMAGEHLP_LINE        Line    );

typedef BOOL (WINAPI *MiniDumpWriteDump_type)
   (HANDLE          hProcess,
    DWORD           ProcessId,
    HANDLE          hFile,
    MINIDUMP_TYPE   DumpType,
    PMINIDUMP_EXCEPTION_INFORMATION   ExceptionParam,
    PMINIDUMP_USER_STREAM_INFORMATION UserStreamParam,
    PMINIDUMP_CALLBACK_INFORMATION    CallbackParam);

static SymFromAddr_type g_SymFromAddr = NULL;
static SymGetLineFromAddr64_type g_SymGetLineFromAddr64 = NULL;
static SymGetLineFromAddr_type g_SymGetLineFromAddr = NULL;
#endif // #ifndef NO_DBGHELP



void create_minidump(LPEXCEPTION_POINTERS exp)
{
#ifndef NO_DBGHELP
    MINIDUMP_EXCEPTION_INFORMATION mei = {GetCurrentThreadId(), exp, TRUE};
    MiniDumpWriteDump_type mdwd = NULL;

    HMODULE hdbghelp = ::LoadLibrary("dbghelp");

    if (hdbghelp)
        mdwd = (MiniDumpWriteDump_type)::GetProcAddress(hdbghelp, "MiniDumpWriteDump");

    if (!mdwd)
    {
        fprintf(stderr, "\nFailed to open DbgHelp library\n");
        fflush(stderr);
        return;
    }

    char filename[24];
    sprintf(filename, "minidump_%d.dmp", GetCurrentProcessId());

    HANDLE file = CreateFile(filename, GENERIC_WRITE, 0, 0,
                                CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, 0);

    if (file == INVALID_HANDLE_VALUE)
    {
        fprintf(stderr, "\nFailed to create minidump file: %s\n", filename);
        fflush(stderr);
        return;
    }

    SymInitialize(GetCurrentProcess(), NULL, TRUE);

    BOOL res = mdwd(GetCurrentProcess(), GetCurrentProcessId(),
                                file, MiniDumpNormal, &mei, 0, 0);

    if (!res)
    {
        fprintf(stderr, "\nFailed to create minidump\n");
        fflush(stderr);
    }
    else
    {
        char dir[_MAX_PATH];
        GetCurrentDirectory(_MAX_PATH, dir);
        fprintf(stderr, "\nMinidump is generated:\n%s\\%s\n", dir, filename);
        fflush(stderr);
    }

    CloseHandle(file);
#endif // #ifndef NO_DBGHELP
}


void sd_get_c_method_info(CFunInfo* info, native_module_t* module, void* ip)
{
    *info->name = 0;
    *info->filename = 0;
    info->line = -1;

#ifndef NO_DBGHELP

    if (!SymInitialize(GetCurrentProcess(), NULL, TRUE))
        return;

    SymSetOptions(SYMOPT_LOAD_LINES | SYMOPT_UNDNAME);

    BYTE smBuf[sizeof(SYMBOL_INFO) + SD_MNAME_LENGTH - 1];
    PSYMBOL_INFO pSymb = (PSYMBOL_INFO)smBuf;
    pSymb->SizeOfStruct = sizeof(SYMBOL_INFO);
    pSymb->MaxNameLen = SD_MNAME_LENGTH;
    DWORD64 funcDispl;

    if (g_SymFromAddr &&
        g_SymFromAddr(GetCurrentProcess(), (DWORD64)(POINTER_SIZE_INT)ip, &funcDispl, pSymb))
    {
        strcpy(info->name, pSymb->Name);
    }

    if (g_SymGetLineFromAddr64)
    {
        DWORD offset;
        IMAGEHLP_LINE64 lineinfo;
        lineinfo.SizeOfStruct = sizeof(IMAGEHLP_LINE64);

        if (g_SymGetLineFromAddr64(GetCurrentProcess(),
                                   (DWORD64)(POINTER_SIZE_INT)ip,
                                   &offset, &lineinfo))
        {
            info->line = lineinfo.LineNumber;
            strncpy(info->filename, lineinfo.FileName, sizeof(info->filename));
            return;
        }
    }

    if (g_SymGetLineFromAddr)
    {
        DWORD offset;
        IMAGEHLP_LINE lineinfo;
        lineinfo.SizeOfStruct = sizeof(IMAGEHLP_LINE);

        if (g_SymGetLineFromAddr(GetCurrentProcess(),
                                 (DWORD)(POINTER_SIZE_INT)ip,
                                 &offset, &lineinfo))
        {
            info->line = lineinfo.LineNumber;
            strncpy(info->filename, lineinfo.FileName, sizeof(info->filename));
        }
    }

#endif // #ifndef NO_DBGHELP
}

void sd_init_crash_handler()
{
#ifndef NO_DBGHELP
// Preventive initialization does not work
//        if (!SymInitialize(GetCurrentProcess(), NULL, TRUE))
//            return false;

        HMODULE hdbghelp = ::LoadLibrary("dbghelp");

        if (hdbghelp)
        {
            SymSetOptions(SYMOPT_LOAD_LINES | SYMOPT_UNDNAME);
            g_SymFromAddr = (SymFromAddr_type)::GetProcAddress(hdbghelp, "SymFromAddr");
            g_SymGetLineFromAddr64 = (SymGetLineFromAddr64_type)::GetProcAddress(hdbghelp, "SymGetLineFromAddr64");
            g_SymGetLineFromAddr = (SymGetLineFromAddr_type)::GetProcAddress(hdbghelp, "SymGetLineFromAddr");
        }
#endif // #ifndef NO_DBGHELP

    // Get current directory
    DWORD required = GetCurrentDirectory(0, NULL);
    char* ptr = (char*)STD_MALLOC(required);

    if (ptr)
    {
        GetCurrentDirectory(required, ptr);
        g_curdir = ptr;
    }

    // Get command line
    LPTSTR cmdline = GetCommandLine();
    ptr = (char*)STD_MALLOC(strlen(cmdline) + 1);
    strcpy(ptr, cmdline);
    g_cmdline = ptr;

    // Get environment
    LPVOID env_block = GetEnvironmentStrings();

    if (!env_block)
        return;

    size_t total_len = 1;
    ptr = (char*)env_block;

    while (*ptr)
    {
        total_len += strlen(ptr) + 1;
        ptr += strlen(ptr) + 1;
    }

    ptr = (char*)STD_MALLOC(total_len);

    if (ptr)
    {
        memcpy(ptr, env_block, total_len);
        g_environ = ptr;
    }

    FreeEnvironmentStrings((char*)env_block);
}

void sd_cleanup_crash_handler()
{
    STD_FREE(g_curdir);
    STD_FREE(g_cmdline);
    STD_FREE(g_environ);
}

void sd_print_cmdline_cwd()
{
    fprintf(stderr, "\nCommand line:\n%s\n", g_cmdline);
    fprintf(stderr, "\nWorking directory:\n%s\n", g_curdir ? g_curdir : "'null'");
}

void sd_print_environment()
{
    fprintf(stderr, "\nEnvironment variables:\n");

    const char* penv = (char*)g_environ;

    while (*penv)
    {
        fprintf(stderr, "%s\n", penv);
        penv += strlen(penv) + 1;
    }
}
