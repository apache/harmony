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

/* Definitions of wrapper functions in thread library function table */

#ifndef THREAD_CLASSLIB_DEFS_H
#define THREAD_CLASSLIB_DEFS_H

#include "hythread.h"

#if defined(PLATFORM_POSIX)
#define THREXPORT
#else  // !PLATFORM_POSIX
#define THREXPORT __declspec(dllexport)
#endif 

IDATA VMCALL hysem_destroy_cl(HyThreadLibrary *threadLibraryFuncs, hysem_t s) 
{
    return hysem_destroy(s);
}

IDATA VMCALL hysem_init_cl(HyThreadLibrary *threadLibraryFuncs, hysem_t * sp, I_32 initValue) 
{
    /* Just return 0 - hysem_init does not exist in DRLVM */
    return 0;
}

IDATA VMCALL hysem_post_cl(HyThreadLibrary *threadLibraryFuncs, hysem_t s)
{
    return hysem_post(s);
}

IDATA VMCALL hysem_wait_cl(HyThreadLibrary *threadLibraryFuncs, hysem_t s)
{
    return hysem_wait(s);
}

IDATA VMCALL hythread_attach_cl(HyThreadLibrary *threadLibraryFuncs, hythread_t * handle)
{
    return hythread_attach(handle);
}

IDATA VMCALL hythread_create_cl(HyThreadLibrary *threadLibraryFuncs, hythread_t * handle, 
                         UDATA stacksize, UDATA priority,
                         UDATA suspend, hythread_entrypoint_t entrypoint,
                         void *entryarg)
{
    return hythread_create(handle, stacksize, priority, suspend, entrypoint, entryarg);
}

void VMCALL hythread_detach_cl(HyThreadLibrary *threadLibraryFuncs, hythread_t thread)
{
    hythread_detach(thread);
}

void VMCALL hythread_exit_cl(HyThreadLibrary *threadLibraryFuncs, hythread_monitor_t monitor)
{
    hythread_exit(monitor);
}


UDATA * VMCALL hythread_global_cl(HyThreadLibrary *threadLibraryFuncs, const char *name) 
{
    return hythread_global(name);
}

IDATA VMCALL hythread_monitor_destroy_cl(HyThreadLibrary *threadLibraryFuncs, hythread_monitor_t monitor)
{
    return hythread_monitor_destroy(monitor);
}

IDATA VMCALL hythread_monitor_enter_cl(HyThreadLibrary *threadLibraryFuncs, hythread_monitor_t monitor) 
{
    return hythread_monitor_enter(monitor);
}

IDATA VMCALL hythread_monitor_exit_cl(HyThreadLibrary *threadLibraryFuncs, hythread_monitor_t monitor) 
{
    return hythread_monitor_exit(monitor);
}

IDATA VMCALL hythread_monitor_init_with_name_cl(HyThreadLibrary *threadLibraryFuncs, hythread_monitor_t * handle, UDATA flags, const char *name) 
{
    return hythread_monitor_init_with_name(handle, flags, name);
}

IDATA VMCALL hythread_monitor_notify_cl(HyThreadLibrary *threadLibraryFuncs, hythread_monitor_t monitor) 
{
    return hythread_monitor_notify(monitor);
}

IDATA VMCALL hythread_monitor_notify_all_cl(HyThreadLibrary *threadLibraryFuncs, hythread_monitor_t monitor) 
{
    return hythread_monitor_notify_all(monitor);
}

IDATA VMCALL hythread_monitor_wait_cl(HyThreadLibrary *threadLibraryFuncs, hythread_monitor_t monitor) 
{
    return hythread_monitor_wait(monitor);
}

hythread_t VMCALL hythread_self_cl(HyThreadLibrary *threadLibraryFuncs)
{
    return hythread_self();
}

IDATA VMCALL hythread_sleep_cl(HyThreadLibrary *threadLibraryFuncs, I_64 millis)
{
    return hythread_sleep(millis);
}

IDATA VMCALL hythread_tls_alloc_cl(HyThreadLibrary *threadLibraryFuncs, hythread_tls_key_t * handle)
{
    return hythread_tls_alloc(handle);
}

IDATA VMCALL hythread_tls_free_cl(HyThreadLibrary *threadLibraryFuncs, hythread_tls_key_t key)
{
    return hythread_tls_free(key);
}

void * VMCALL hythread_tls_get_cl(HyThreadLibrary *threadLibraryFuncs, hythread_t thread, hythread_tls_key_t key)
{
    return hythread_tls_get(thread, key);
}

IDATA VMCALL hythread_tls_set_cl(HyThreadLibrary *threadLibraryFuncs, hythread_t thread, hythread_tls_key_t key, void *value)
{
    return hythread_tls_set(thread, key, value);
}


static HyThreadLibrary MasterThreadLibraryTable = {
	{HYTHREAD_MAJOR_VERSION_NUMBER, HYTHREAD_MINOR_VERSION_NUMBER, 0, HYTHREAD_CAPABILITY_MASK},
	hysem_destroy_cl,
	hysem_init_cl,
	hysem_post_cl,
	hysem_wait_cl,
	hythread_attach_cl,
	hythread_create_cl,
	hythread_detach_cl,
	hythread_exit_cl,
	hythread_global_cl,
	hythread_monitor_destroy_cl, 
	hythread_monitor_enter_cl,
	hythread_monitor_exit_cl,
	hythread_monitor_init_with_name_cl,
	hythread_monitor_notify_cl,
	hythread_monitor_notify_all_cl,
	hythread_monitor_wait_cl,
	hythread_self_cl,
	hythread_sleep_cl,
	hythread_tls_alloc_cl,
	hythread_tls_free_cl,
	hythread_tls_get_cl,
	hythread_tls_set_cl,
	NULL,
};

#endif
