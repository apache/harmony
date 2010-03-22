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


#include "open/platform_types.h"
#include "open/hythread_ext.h"
#include "port_mutex.h"
#include "port_thread_internal.h"


volatile port_shared_data_t* port_shared_data = NULL;

static port_shared_data_t* g_port_shared_data = NULL;
static port_shared_data_t g_port_shared_data_struct;


#define MEM_PROTECT_SIZE 0x100

#ifdef _EM64T_
#define GUARD_STACK_SIZE (64*1024)
#else /* IA-32 */
#define GUARD_STACK_SIZE (find_guard_page_size())
#endif


static size_t find_guard_page_size()
{
    SYSTEM_INFO system_info;
    GetSystemInfo(&system_info);
    return system_info.dwPageSize;
}

static int init_psd_structure(port_shared_data_t* data)
{
    DWORD key;

    if ((key = TlsAlloc()) == TLS_OUT_OF_INDEXES)
        return -1;

    data->tls_key = key;
    InitializeCriticalSection(&data->crit_section);
    data->foreign_stack_size = 0;
    data->guard_page_size = find_guard_page_size();
    data->guard_stack_size = GUARD_STACK_SIZE;
    data->mem_protect_size = MEM_PROTECT_SIZE;
    return 0;
}

__declspec(dllexport) int port_init_shared_data(volatile port_shared_data_t** p_psd)
{
    int err = 0;
static CRITICAL_SECTION struct_lock;
static int lock_initialized = 0;

    if (*p_psd)
        return 0;

    if (lock_initialized == 0)
    { /* Probable race condition because of using static flag */
        InitializeCriticalSection(&struct_lock);
        lock_initialized = 1;
    }

    EnterCriticalSection(&struct_lock);

    /* If another thread had filled the pointer */
    if (*p_psd)
    {
        LeaveCriticalSection(&struct_lock);
        return 0;
    }

    /* If structure is already initialized */
    if (!g_port_shared_data)
    {
        err = init_psd_structure(&g_port_shared_data_struct);

        if (err == 0)
            g_port_shared_data = &g_port_shared_data_struct;
    }

    if (g_port_shared_data)
        *p_psd = g_port_shared_data;

    LeaveCriticalSection(&struct_lock);
    return err;
}


int init_port_shared_data()
{
    if (port_shared_data)
        return 0;

    return port_init_shared_data(&port_shared_data);
}
