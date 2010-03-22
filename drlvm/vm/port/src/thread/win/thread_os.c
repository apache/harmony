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


#include "port_atomic.h"
#include "port_malloc.h"
#include "port_thread.h"
#include "port_thread_internal.h"


/* Forward declarations */
static int suspend_init_lock();
static port_thread_info_t* init_susres_list_item();
static port_thread_info_t* suspend_add_thread(osthread_t thread);
static void suspend_remove_thread(osthread_t thread);
static port_thread_info_t* suspend_find_thread(osthread_t thread);


typedef unsigned (__stdcall *beginthread_func_t)(void*);

typedef struct
{
    port_threadfunc_t   fun;
    void*               arg;
    size_t              stack_size;
} thread_start_struct_t;

static unsigned __stdcall thread_start_func(void* arg)
{
    int err, result;
    port_tls_data_t* tlsdata;
    thread_start_struct_t* ptr = (thread_start_struct_t*)arg;
    port_threadfunc_t fun = ptr->fun;
    size_t stack_size = ptr->stack_size;
    arg = ptr->arg;
    STD_FREE(ptr);

    if (port_shared_data)
    {
        tlsdata = (port_tls_data_t*)STD_ALLOCA(sizeof(port_tls_data_t));
        err = port_thread_attach_local(tlsdata, FALSE, FALSE, stack_size);
        assert(err == 0);
    }

    result = (int)fun(arg);

    port_thread_detach();

    return result;
}


int port_thread_create(/* out */osthread_t* phandle, size_t stacksize, int priority,
        port_threadfunc_t func, void *data)
{
    uintptr_t handle;
    thread_start_struct_t* startstr;
    int res;

    if (!port_shared_data)
    {
        res = init_port_shared_data();
        /* assert(res); */
        /* It's OK to have an error here when Port shared library
           is not available yet; only signals/crash handling will
           not be available for the thread */
        /* return res; */
    }

    if (!func)
        return -1;

    startstr =
        (thread_start_struct_t*)STD_MALLOC(sizeof(thread_start_struct_t));

    if (!startstr)
        return -1;

    if (stacksize != 0)
    {
        if (port_shared_data)
        {
            size_t min_stacksize =
                /* Let's get alt stack size for normal stack and add guard page size */
                ((2*port_shared_data->guard_stack_size + port_shared_data->guard_page_size)
                /* Roung up to alt stack size */
                    + port_shared_data->guard_stack_size - 1) & ~(port_shared_data->guard_stack_size - 1);

            if (stacksize < min_stacksize)
                stacksize = min_stacksize;
        }
    }

    startstr->fun = func;
    startstr->arg = data;
    startstr->stack_size = stacksize;

    handle = _beginthreadex(NULL, (int)stacksize, thread_start_func,
                            startstr, STACK_SIZE_PARAM_IS_A_RESERVATION, NULL);

    if (handle != (uintptr_t)-1L)
    {
        *phandle = (HANDLE)handle;

        if (priority)
            SetThreadPriority(*phandle, priority);

        return 0;
    }

    STD_FREE(startstr);
    return res;
}

static int set_guard_page(port_tls_data_t* tlsdata, Boolean set)
{
    DWORD oldProtect;
    void* guard_addr;
    size_t guard_size;

    if (!tlsdata)
        tlsdata = get_private_tls_data();

    if (!tlsdata)
        return -1;

    if (!tlsdata->guard_page_addr)
        return 0;

    if ((set && tlsdata->guard_page_set) ||
        (!set && !tlsdata->guard_page_set))
        return 0; // Already in needed state

#ifdef _EM64T_
    /* Windows x86_64 protects both guard page and guard stack area
       specified by SetThreadStackGuarantee() with PAGE_GUARD flag */
    guard_addr = tlsdata->guard_stack_addr;
    guard_size = tlsdata->guard_stack_size + tlsdata->guard_page_size;
#else
    guard_addr = tlsdata->guard_page_addr;
    guard_size = tlsdata->guard_page_size;
#endif

    if (set)
    {
        if ((size_t)&set - PSD->mem_protect_size
                < (size_t)tlsdata->guard_page_addr + tlsdata->guard_page_size)
            return -1;

        if (!VirtualProtect(guard_addr, guard_size,
                            PAGE_GUARD | PAGE_READWRITE, &oldProtect))
            // should be successful always
            return -1;
    }
    else
    {
        if ((size_t)&set < (size_t)tlsdata->guard_page_addr + tlsdata->guard_page_size)
            return -1;

        if (!VirtualProtect(guard_addr, guard_size,
                            PAGE_READWRITE, &oldProtect))
            // should be successful always
            return -1;
    }

    tlsdata->guard_page_set = set;
    return 0;
}

int port_thread_restore_guard_page()
{
    return set_guard_page(NULL, TRUE);
}

int port_thread_clear_guard_page()
{
    return set_guard_page(NULL, FALSE);
}

void port_thread_postpone_guard_page()
{
    port_tls_data_t* tlsdata = get_private_tls_data();

    if (!tlsdata || !tlsdata->guard_page_addr)
        return;

    tlsdata->restore_guard_page = FALSE;
}

void* port_thread_get_stack_address()
{
    port_tls_data_t* tlsdata = get_private_tls_data();
    return tlsdata ? tlsdata->stack_addr : NULL;
}

size_t port_thread_get_stack_size()
{
    port_tls_data_t* tlsdata = get_private_tls_data();
    return tlsdata ? tlsdata->stack_size : 0;
}

size_t port_thread_get_effective_stack_size()
{
    port_tls_data_t* tlsdata = get_private_tls_data();

    if (!tlsdata)
        return 0;

    if (!tlsdata->guard_page_addr || !tlsdata->guard_page_set)
        return tlsdata->stack_size
               - PSD->guard_page_size - PSD->mem_protect_size;

    return tlsdata->stack_size - 2*PSD->guard_page_size
           - PSD->guard_stack_size - PSD->mem_protect_size;
}

static int setup_stack(port_tls_data_t* tlsdata)
{
#ifdef _EM64T_
    ULONG guard_stack_size_param;
#endif

    if (!port_shared_data)
        return -1;

#ifdef _EM64T_
    /* this code in future should be used on both platforms x86-32 and x86-64 */
    guard_stack_size_param = (ULONG)PSD->guard_stack_size;

    if (!SetThreadStackGuarantee(&guard_stack_size_param))
        /* should be successful always */
        return -1;
#endif

    if ((size_t)&tlsdata - PSD->mem_protect_size
            < (size_t)tlsdata->guard_page_addr + tlsdata->guard_page_size)
        return -1;

    tlsdata->guard_page_set = TRUE; // GUARD_PAGE is set by default
    return 0;
}

static int find_stack_addr_size(void** paddr, size_t* psize)
{
    size_t reg_size;
    MEMORY_BASIC_INFORMATION memory_information;

    VirtualQuery(&memory_information, &memory_information, sizeof(memory_information));
    reg_size = memory_information.RegionSize;
    *paddr = (void*)((size_t)memory_information.BaseAddress + reg_size);
    *psize = (size_t)*paddr - (size_t)memory_information.AllocationBase;
    return 0;
}

static int init_stack(port_tls_data_t* tlsdata, size_t stack_size, Boolean temp)
{
    int err;
    size_t stack_begin;

    if (!port_shared_data)
        return -1;

    err = find_stack_addr_size(&tlsdata->stack_addr, &tlsdata->stack_size);
    if (err != 0) return err;

    if (stack_size)
        tlsdata->stack_size = stack_size;

    tlsdata->guard_page_size = PSD->guard_page_size;
    tlsdata->guard_stack_size = PSD->guard_stack_size;
    tlsdata->mem_protect_size = PSD->mem_protect_size;

    if (temp)
        return 0;

    stack_begin = (size_t)tlsdata->stack_addr - tlsdata->stack_size;
    tlsdata->guard_stack_addr = (void*)(stack_begin + tlsdata->guard_page_size);
    tlsdata->guard_page_addr =
        (void*)((size_t)tlsdata->guard_stack_addr + tlsdata->guard_stack_size);

    return setup_stack(tlsdata);
}

int port_thread_attach_local(port_tls_data_t* tlsdata, Boolean temp,
                                    Boolean foreign, size_t stack_size)
{
    int res;

    memset(tlsdata, 0, sizeof(port_tls_data_t));

    tlsdata->foreign = foreign;
    res = init_stack(tlsdata, stack_size, temp);
    if (res != 0) return res;

    return set_private_tls_data(tlsdata);
}

int port_thread_attach()
{
    int res;
    port_tls_data_t* tlsdata;

    if (!port_shared_data && (res = init_port_shared_data()) != 0)
        return res;

    if (get_private_tls_data())
        return 0;

    tlsdata = (port_tls_data_t*)STD_MALLOC(sizeof(port_tls_data_t));

    if (!tlsdata) return ENOMEM;

    res = port_thread_attach_local(tlsdata, FALSE, TRUE, 0);

    if (res != 0)
        STD_FREE(tlsdata);

    return res;
}

int port_thread_detach_temporary()
{
    port_tls_data_t* tlsdata = get_private_tls_data();

    if (!tlsdata || tlsdata->guard_page_addr)
        return -1;

    return set_private_tls_data(NULL);
}

int port_thread_detach()
{
    port_tls_data_t* tlsdata;
    int res;

    if (!port_shared_data && (res = init_port_shared_data()) != 0)
        return res;

    tlsdata = get_private_tls_data();

    if (!tlsdata)
        return 0;

    if (port_thread_detach_temporary() == 0)
        return 0;

    if (tlsdata->foreign)
        STD_FREE(tlsdata);

    return set_private_tls_data(NULL);
}

int port_thread_set_priority(osthread_t os_thread, int priority)
{
    if (SetThreadPriority(os_thread, (int)priority)) {
        return 0;
    } else {
        return GetLastError();
    }
}

osthread_t port_thread_current()
{
    HANDLE hproc = GetCurrentProcess();
    HANDLE hthread = GetCurrentThread();
    if (!DuplicateHandle(hproc, hthread,
                         hproc, &hthread, 0, FALSE,
                         DUPLICATE_SAME_ACCESS)) {
        return NULL;
    }
    return hthread;
}

int port_thread_free_handle(osthread_t os_thread)
{
    BOOL r = CloseHandle(os_thread);
    return !r;
}

int port_thread_join(osthread_t os_thread)
{
    int error = 0;
    DWORD r;
    r = WaitForSingleObject(os_thread, INFINITE);
    if (r == WAIT_OBJECT_0 || r == WAIT_ABANDONED)
        r = 0;
    else
        r = GetLastError();
    CloseHandle(os_thread);
    return r;
}

int port_thread_cancel(osthread_t os_thread)
{
    port_thread_info_t* pinfo;
    int status = TM_ERROR_NONE;

    if (!suspend_init_lock())
        return TM_ERROR_INTERNAL;

    pinfo = suspend_find_thread(os_thread);

    if (pinfo)
        suspend_remove_thread(os_thread);

    if (!TerminateThread(os_thread, 0))
        status = (int)GetLastError();

    LeaveCriticalSection(&PSD->crit_section);
    return status;
}

void port_thread_exit(int status)
{
    ExitThread(status);
}

int port_get_thread_times(osthread_t os_thread, int64* pkernel, int64* puser)
{
    FILETIME creation_time;
    FILETIME exit_time;
    FILETIME kernel_time;
    FILETIME user_time;
    int r;

    r = GetThreadTimes(os_thread,
            &creation_time, &exit_time, &kernel_time, &user_time);

    if (r) {
        // according to MSDN, time is counted in 100 ns units, so we need to multiply by 100
        *pkernel = 100 *
            (((int64)kernel_time.dwHighDateTime << 32)
             | kernel_time.dwLowDateTime);
        *puser = 100 *
            (((int64)user_time.dwHighDateTime << 32)
             | user_time.dwLowDateTime);
        return 0;
    } else
        return GetLastError();
}

void port_thread_yield_other(osthread_t os_thread)
{
    port_thread_info_t* pinfo;

    /*
     * Synchronization is needed to avoid cyclic (mutual) suspension problem.
     * Accordingly to MSDN, it is possible on multiprocessor box that
     * 2 threads suspend each other and become deadlocked.
     */
    if (!suspend_init_lock()) // Initializes and enters a critical section
        return;

    pinfo = suspend_find_thread(os_thread);

    if (pinfo && pinfo->suspend_count > 0) {
        LeaveCriticalSection(&PSD->crit_section);
        return;
    }

    if (SuspendThread(os_thread) != -1) {
        /* suspended successfully, so resume it back. */
        ResumeThread(os_thread);
    }

    LeaveCriticalSection(&PSD->crit_section);
}


int port_thread_suspend(osthread_t thread)
{
    port_thread_info_t* pinfo;
    DWORD old_count;

    if (!thread)
        return TM_ERROR_NULL_POINTER;

    if (!suspend_init_lock())
        return TM_ERROR_INTERNAL;

    pinfo = suspend_find_thread(thread);

    if (!pinfo)
        pinfo = suspend_add_thread(thread);

    if (!pinfo)
    {
        LeaveCriticalSection(&PSD->crit_section);
        return TM_ERROR_OUT_OF_MEMORY;
    }

    if (pinfo->suspend_count > 0)
    {
        ++pinfo->suspend_count;
        LeaveCriticalSection(&PSD->crit_section);
        return TM_ERROR_NONE;
    }

    old_count = SuspendThread(thread);

    if (old_count == (DWORD)-1)
    {
        int status = (int)GetLastError();
        LeaveCriticalSection(&PSD->crit_section);
        return status;
    }

    ++pinfo->suspend_count;
    LeaveCriticalSection(&PSD->crit_section);
    return TM_ERROR_NONE;
}

int port_thread_resume(osthread_t thread)
{
    port_thread_info_t* pinfo;
    DWORD old_count;

    if (!thread)
        return TM_ERROR_NULL_POINTER;

    if (!suspend_init_lock())
        return TM_ERROR_INTERNAL;

    pinfo = suspend_find_thread(thread);

    if (!pinfo)
    {
        LeaveCriticalSection(&PSD->crit_section);
        return TM_ERROR_UNATTACHED_THREAD;
    }

    if (pinfo->suspend_count > 1)
    {
        --pinfo->suspend_count;
        LeaveCriticalSection(&PSD->crit_section);
        return TM_ERROR_NONE;
    }

    old_count = ResumeThread(thread);

    if (old_count == (DWORD)-1)
    {
        int status = (int)GetLastError();
        LeaveCriticalSection(&PSD->crit_section);
        return status;
    }

    if (--pinfo->suspend_count == 0)
        suspend_remove_thread(thread);

    LeaveCriticalSection(&PSD->crit_section);
    return TM_ERROR_NONE;
}

int port_thread_get_suspend_count(osthread_t thread)
{
    port_thread_info_t* pinfo;
    int suspend_count;

    if (!thread)
        return -1;

    if (!suspend_init_lock())
        return -1;

    pinfo = suspend_find_thread(thread);
    suspend_count = pinfo ? pinfo->suspend_count : 0;

    LeaveCriticalSection(&PSD->crit_section);
    return suspend_count;
}

int port_thread_get_context(osthread_t thread, thread_context_t *context)
{
    port_thread_info_t* pinfo;
    CONTEXT local_context;

    if (!thread || !context)
        return TM_ERROR_NULL_POINTER;

    if (!suspend_init_lock())
        return TM_ERROR_INTERNAL;

    pinfo = suspend_find_thread(thread);

    if (!pinfo)
    {
        LeaveCriticalSection(&PSD->crit_section);
        return TM_ERROR_UNATTACHED_THREAD;
    }

#ifdef CONTEXT_ALL
    local_context.ContextFlags = CONTEXT_ALL;
#else
    local_context.ContextFlags = CONTEXT_FULL;
#endif

    if (!GetThreadContext(thread, &local_context))
    {
        int status = (int)GetLastError();
        LeaveCriticalSection(&PSD->crit_section);
        return status;
    }

    pinfo->context = local_context;
    *context = local_context;
    LeaveCriticalSection(&PSD->crit_section);
    return TM_ERROR_NONE;
}

int port_thread_set_context(osthread_t thread, thread_context_t *context)
{
    port_thread_info_t* pinfo;

    if (!thread || !context)
        return -1;

    if (!suspend_init_lock())
        return -2;

    pinfo = suspend_find_thread(thread);

    if (!pinfo)
    {
        LeaveCriticalSection(&PSD->crit_section);
        return TM_ERROR_UNATTACHED_THREAD;
    }

    if (!SetThreadContext(thread, context))
    {
        int status = (int)GetLastError();
        LeaveCriticalSection(&PSD->crit_section);
        return status;
    }

    pinfo->context = *context;
    LeaveCriticalSection(&PSD->crit_section);
    return TM_ERROR_NONE;
}


static int suspend_init_lock()
{
    static uint16 initialized = 0;

    if (!initialized)
    {
        // Critical section should be initialized only once,
        // do nothing in case someone else already initialized it.
        if (port_atomic_cas16((volatile uint16*)&initialized, 1, 0) == 0)
            InitializeCriticalSectionAndSpinCount(&PSD->crit_section, 400);
    }

    EnterCriticalSection(&PSD->crit_section);
    return 1;
}

static port_thread_info_t* init_susres_list_item()
{
    port_thread_info_t* pinfo =
        (port_thread_info_t*)malloc(sizeof(port_thread_info_t));

    if (pinfo)
        pinfo->suspend_count = 0;

    return pinfo;
}

static port_thread_info_t* suspend_add_thread(osthread_t thread)
{
    port_thread_info_t* pinfo = init_susres_list_item();

    if (!pinfo)
        return NULL;

    pinfo->thread = thread;
    pinfo->next = PSD->suspended_list;
    PSD->suspended_list = pinfo;

    return pinfo;
}

static void suspend_remove_thread(osthread_t thread)
{
    port_thread_info_t** pprev = &PSD->suspended_list;
    port_thread_info_t* pinfo;

    for (pinfo = PSD->suspended_list; pinfo; pinfo = pinfo->next)
    {
        if (pinfo->thread == thread)
            break;

        pprev = &pinfo->next;
    }

    if (pinfo)
    {
        *pprev = pinfo->next;
        free(pinfo);
    }
}

static port_thread_info_t* suspend_find_thread(osthread_t thread)
{
    port_thread_info_t* pinfo;

    for (pinfo = PSD->suspended_list; pinfo; pinfo = pinfo->next)
    {
        if (pinfo->thread == thread)
            break;
    }

    return pinfo;
}

