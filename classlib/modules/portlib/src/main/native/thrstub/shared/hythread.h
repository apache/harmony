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

#if !defined(HYTHREAD_H)
#define HYTHREAD_H

#if defined(__cplusplus)
extern "C"
{
#endif
#include <stddef.h>
#include "hycomp.h"
	
/** @} */
/**
 * @name Thread library access
 * @anchor ThreadAccess
 * Macros for accessing thread library
 * {
 */
	
#define THREAD_ACCESS_FROM_ENV(jniEnv)\
	VMInterface *threadPrivateVMI = VMI_GetVMIFromJNIEnv(jniEnv);\
	HyPortLibrary *privatePortLibForThread = (*threadPrivateVMI)->GetPortLibrary(threadPrivateVMI);\
	HyThreadLibrary *privateThreadLibrary = privatePortLibForThread->port_get_thread_library(privatePortLibForThread)

#define THREAD_ACCESS_FROM_JAVAVM(javaVM) \
	VMInterface *threadPrivateVMI =  VMI_GetVMIFromJavaVM(javaVM); \
	HyPortLibrary *privatePortLibForThread = (*threadPrivateVMI)->GetPortLibrary(threadPrivateVMI); \
	HyThreadLibrary *privateThreadLibrary = privatePortLibForThread->port_get_thread_library(privatePortLibForThread)

#define THREAD_ACCESS_FROM_VMI(vmi) HyPortLibrary *privatePortLibForThread = (*vmi)->GetPortLibrary(vmi); \
HyThreadLibrary *privateThreadLibrary = privatePortLibForThread->port_get_thread_library(privatePortLibForThread)

#define THREAD_ACCESS_FROM_PORT(portLib) HyThreadLibrary *privateThreadLibrary = portLib->port_get_thread_library(portLib)

#define THREAD_ACCESS_FROM_THREAD(threadLibrary) HyThreadLibrary *privateThreadLibrary = threadLibrary

#define THREADLIB privateThreadLibrary
/** @} */

typedef UDATA hythread_tls_key_t;

#define HYTHREAD_PROC VMCALL

#define HYTHREAD_MAJOR_VERSION_NUMBER  1
#define HYTHREAD_MINOR_VERSION_NUMBER  0
#define HYTHREAD_CAPABILITY_BASE  0
#define HYTHREAD_CAPABILITY_STANDARD  1
#define HYTHREAD_CAPABILITY_MASK ((U_64)(HYTHREAD_CAPABILITY_STANDARD))
#define HYTHREAD_SET_VERSION(threadLibraryVersion, capabilityMask) \
  (threadLibraryVersion)->majorVersionNumber = HYTHREAD_MAJOR_VERSION_NUMBER; \
  (threadLibraryVersion)->minorVersionNumber = HYTHREAD_MINOR_VERSION_NUMBER; \
  (threadLibraryVersion)->capabilities = (capabilityMask)
#define HYTHREAD_SET_VERSION_DEFAULT(threadLibraryVersion) \
  (threadLibraryVersion)->majorVersionNumber = HYTHREAD_MAJOR_VERSION_NUMBER; \
  (threadLibraryVersion)->minorVersionNumber = HYTHREAD_MINOR_VERSION_NUMBER; \
  (threadLibraryVersion)->capabilities = HYTHREAD_CAPABILITY_MASK


typedef int (HYTHREAD_PROC * hythread_entrypoint_t) (void *);
typedef struct HyThread *hythread_t;
typedef struct HyThreadMonitor *hythread_monitor_t;
typedef struct HySemaphore *hysem_t;

typedef struct HyThreadMonitorTracing
{
    const char *monitor_name;
    UDATA enter_count;
    UDATA slow_count;
    UDATA recursive_count;
    UDATA spin2_count;
    UDATA yield_count;
} HyThreadMonitorTracing;

typedef struct HyThreadLibraryVersion
{
  U_16 majorVersionNumber;
  U_16 minorVersionNumber;
  U_32 padding;
  U_64 capabilities;
} HyThreadLibraryVersion;

typedef struct HyThreadLibrary {
  /** threadVersion */
  struct HyThreadLibraryVersion threadVersion;

  IDATA (PVMCALL sem_destroy) (struct HyThreadLibrary * threadLibrary, hysem_t s);
  IDATA (PVMCALL sem_init) (struct HyThreadLibrary * threadLibrary, hysem_t * sp, I_32 initValue);
  IDATA (PVMCALL sem_post) (struct HyThreadLibrary * threadLibrary, hysem_t s);
  IDATA (PVMCALL sem_wait) (struct HyThreadLibrary * threadLibrary, hysem_t s);

  IDATA (PVMCALL thread_attach) (struct HyThreadLibrary * threadLibrary, hythread_t * handle);
  IDATA (PVMCALL thread_create) (struct HyThreadLibrary * threadLibrary, hythread_t * handle, UDATA stacksize, UDATA priority,
                UDATA suspend, hythread_entrypoint_t entrypoint,
                void *entryarg);
  void  (PVMCALL thread_detach) (struct HyThreadLibrary * threadLibrary, hythread_t thread);
  void  (PVMCALL NORETURN thread_exit) (struct HyThreadLibrary * threadLibrary, hythread_monitor_t monitor);

  UDATA *(PVMCALL thread_global) (struct HyThreadLibrary * threadLibrary, const char *name);

  IDATA (PVMCALL thread_monitor_destroy) (struct HyThreadLibrary * threadLibrary, hythread_monitor_t monitor);
  IDATA (PVMCALL thread_monitor_enter) (struct HyThreadLibrary * threadLibrary, hythread_monitor_t monitor);
  IDATA (PVMCALL thread_monitor_exit) (struct HyThreadLibrary * threadLibrary, hythread_monitor_t monitor);
  IDATA (PVMCALL thread_monitor_init_with_name) (struct HyThreadLibrary * threadLibrary, hythread_monitor_t * handle, UDATA flags, const char *name);
  IDATA (PVMCALL thread_monitor_notify) (struct HyThreadLibrary * threadLibrary, hythread_monitor_t monitor);
  IDATA (PVMCALL thread_monitor_notify_all) (struct HyThreadLibrary * threadLibrary, hythread_monitor_t monitor);
  IDATA (PVMCALL thread_monitor_wait) (struct HyThreadLibrary * threadLibrary, hythread_monitor_t monitor);

  hythread_t (PVMCALL thread_self) (struct HyThreadLibrary * threadLibrary);
  IDATA (PVMCALL thread_sleep) (struct HyThreadLibrary * threadLibrary, I_64 millis);

  IDATA (PVMCALL thread_tls_alloc) (struct HyThreadLibrary * threadLibrary, hythread_tls_key_t * handle);
  IDATA (PVMCALL thread_tls_free) (struct HyThreadLibrary * threadLibrary, hythread_tls_key_t key);
  void *(PVMCALL thread_tls_get) (struct HyThreadLibrary * threadLibrary, hythread_t thread, hythread_tls_key_t key);
  IDATA (PVMCALL thread_tls_set) (struct HyThreadLibrary * threadLibrary, hythread_t thread, hythread_tls_key_t key, void *value);
  /** self_handle*/
  void *self_handle;
} HyThreadLibrary;


/**
 * @name Thread library startup and shutdown functions
 * @anchor ThreadStartup
 * Create, initialize, startup and shutdow the thread library
 * @{
 */
/** Standard startup and shutdown (thread library allocated on stack or by application)  */
extern HY_CFUNC I_32 VMCALL hythread_create_library (struct HyThreadLibrary
                                                     *threadLibrary,
                                                     struct HyThreadLibraryVersion
                                                     *version, UDATA size);
extern HY_CFUNC I_32 VMCALL hythread_init_library (struct HyThreadLibrary
                                                   *threadLibrary,
                                                   struct HyThreadLibraryVersion
                                                   *version, UDATA size);
extern HY_CFUNC I_32 VMCALL hythread_shutdown_library (struct HyThreadLibrary
                                                       *threadLibrary);
extern HY_CFUNC I_32 VMCALL hythread_startup_library (struct HyThreadLibrary
                                                      *threadLibrary);
/** Thread library self allocation routines */
extern HY_CFUNC I_32 VMCALL hythread_allocate_library (struct HyThreadLibraryVersion
                                                       *expectedVersion,
                                                       struct HyThreadLibrary
                                                       **threadLibrary);

/** @} */
/**
 * @name Thread library version and compatability queries
 * @anchor ThreadVersionControl
 * Determine thread library compatability and version.
 * @{
 */
extern HY_CFUNC UDATA VMCALL hythread_getSize (struct HyThreadLibraryVersion
                                               *version);
extern HY_CFUNC I_32 VMCALL hythread_getVersion (struct HyThreadLibrary
                                                 *threadLibrary,
                                                 struct HyThreadLibraryVersion
                                                 *version);
extern HY_CFUNC I_32 VMCALL hythread_isCompatible (struct HyThreadLibraryVersion
                                                   *expectedVersion);

/** @} */
/** 
 * @name ThreadLibrary Access functions
 * Convenience helpers for accessing thread library functionality.  Users can 
 * either call functions directly via the table or by help macros.
 * @code 
 * if (0 != threadLibrary->thread_monitor_init_with_name (threadLibrary, &nls->monitor, 0, "NLS hash table"))...
 * @endcode
 * @code
 * THREAD_ACCESS_FROM_ENV(jniEnv);
 * if (0 != hythread_monitor_init_with_name (&nls->monitor, 0, "NLS hash table")) ...
 * @endcode
 * @{
 */

#if !defined(HYTHREAD_LIBRARY_DEFINE)
#define hythread_global_monitor() (*(hythread_monitor_t*)privateThreadLibrary->thread_global(privateThreadLibrary,"global_monitor"))
#define hythread_monitor_init(pMon,flags)  privateThreadLibrary->thread_monitor_init_with_name(privateThreadLibrary,pMon,flags, #pMon)

#define hysem_destroy(param1) privateThreadLibrary->sem_destroy(privateThreadLibrary,param1)
#define hysem_init(param1,param2) privateThreadLibrary->sem_init(privateThreadLibrary,param1,param2)
#define hysem_post(param1) privateThreadLibrary->sem_post(privateThreadLibrary,param1)
#define hysem_wait(param1) privateThreadLibrary->sem_wait(privateThreadLibrary,param1)

#define hythread_attach(param1) privateThreadLibrary->thread_attach(privateThreadLibrary,param1)
#define hythread_create(param1,param2,param3,param4,param5,param6) privateThreadLibrary->thread_create(privateThreadLibrary,param1,param2,param3,param4,param5,param6)
#define hythread_detach(param1) privateThreadLibrary->thread_detach(privateThreadLibrary,param1)
#define hythread_exit(param1) privateThreadLibrary->thread_exit(privateThreadLibrary,param1)

#define hythread_global(param1) privateThreadLibrary->thread_global(privateThreadLibrary,param1)

#define hythread_monitor_destroy(param1) privateThreadLibrary->thread_monitor_destroy(privateThreadLibrary,param1)
#define hythread_monitor_enter(param1) privateThreadLibrary->thread_monitor_enter(privateThreadLibrary,param1)
#define hythread_monitor_exit(param1) privateThreadLibrary->thread_monitor_exit(privateThreadLibrary,param1)
#define hythread_monitor_init_with_name(param1,param2,param3) privateThreadLibrary->thread_monitor_init_with_name(privateThreadLibrary,param1,param2,param3)
#define hythread_monitor_notify(param1) privateThreadLibrary->thread_monitor_notify(privateThreadLibrary,param1)
#define hythread_monitor_notify_all(param1) privateThreadLibrary->thread_monitor_notify_all(privateThreadLibrary,param1)
#define hythread_monitor_wait(param1) privateThreadLibrary->thread_monitor_wait(privateThreadLibrary,param1)

#define hythread_self() privateThreadLibrary->thread_self(privateThreadLibrary)
#define hythread_sleep(param1) privateThreadLibrary->thread_sleep(privateThreadLibrary,param1)

#define hythread_tls_alloc(param1) privateThreadLibrary->thread_tls_alloc(privateThreadLibrary,param1)
#define hythread_tls_free(param1) privateThreadLibrary->thread_tls_free(privateThreadLibrary,param1)
#define hythread_tls_get(param1,param2) privateThreadLibrary->thread_tls_get(privateThreadLibrary,param1,param2)
#define hythread_tls_set(param1,param2,param3) privateThreadLibrary->thread_tls_set(privateThreadLibrary,param1,param2,param3)

#endif /*  !HYTHREAD_LIBRARY_DEFINE */
#if defined(__cplusplus)
}
#endif

#endif /* HYTHREAD_H */
