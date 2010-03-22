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
/**
 * @author Petr Ivanov, Ilya Berezhniuk
 */

#include "port_malloc.h"
#include "port_modules.h"

#include <memory.h>
#include <Windows.h>
#include <Tlhelp32.h>


static native_module_t* fill_module(MODULEENTRY32 src);

Boolean port_get_all_modules(native_module_t** list_ptr, int* count_ptr)
{
    HANDLE hModuleSnap = INVALID_HANDLE_VALUE; 
    MODULEENTRY32 module; 
    native_module_t** cur_next_ptr = list_ptr;
    int count = 0;

    hModuleSnap =
        CreateToolhelp32Snapshot(TH32CS_SNAPMODULE, GetCurrentProcessId());

    if (hModuleSnap == INVALID_HANDLE_VALUE)
        return FALSE;

    *list_ptr = NULL;

    //It is required to set the size of the structure. 
    module.dwSize = sizeof(MODULEENTRY32);
    if ( !Module32First(hModuleSnap, &module) )
    {
        CloseHandle(hModuleSnap);
        return FALSE;
    }

    do
    {
        native_module_t* filled = fill_module(module);

        if (!filled)
        {
            CloseHandle(hModuleSnap);
            port_clear_modules(list_ptr);
            return FALSE;
        }

        *cur_next_ptr = filled;
        cur_next_ptr = &filled->next;
        count++;

    } while (Module32Next(hModuleSnap, &module));

    CloseHandle(hModuleSnap);
    *count_ptr = count;

    return TRUE;
}

native_module_t* fill_module(MODULEENTRY32 src)
{
    MEMORY_BASIC_INFORMATION mem_info;
    size_t path_size = strlen(src.szExePath) + 1;

    native_module_t* module =
        (native_module_t*)STD_MALLOC(sizeof(native_module_t));

    if (module == NULL)
        return NULL;

    module->filename = (char*)STD_MALLOC(path_size);
    if (module->filename == NULL)
    {
        STD_FREE(module);
        return NULL;
    }

    memcpy(module->filename, src.szExePath, path_size);
    strlwr(module->filename);

    module->seg_count = 1;
    module->segments[0].base = src.modBaseAddr;
    module->segments[0].size = (size_t)src.modBaseSize;
    module->next = NULL;

    VirtualQuery(src.modBaseAddr, &mem_info, sizeof(mem_info));

    if ((mem_info.Protect & (PAGE_EXECUTE | PAGE_EXECUTE_READ | PAGE_EXECUTE_READWRITE | PAGE_EXECUTE_WRITECOPY)) != 0)
        module->segments[0].type = SEGMENT_TYPE_CODE;
    else if ((mem_info.Protect & (PAGE_READWRITE | PAGE_READONLY)) != 0)
        module->segments[0].type = SEGMENT_TYPE_DATA;
    else
        module->segments[0].type = SEGMENT_TYPE_UNKNOWN;

    return module;
}
