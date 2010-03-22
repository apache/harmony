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

#if !defined(thrdsup_h)
#define thrdsup_h

/* windows.h defined UDATA.  Ignore its definition */
#define UDATA UDATA_win32_
#include <windows.h>
#undef UDATA			/* this is safe because our UDATA is a typedef, not a macro */
#include <process.h>
#include "hymutex.h"

/* ostypes */
typedef HANDLE OSTHREAD;
typedef DWORD TLSKEY;
typedef HANDLE COND;

#define WRAPPER_TYPE void _cdecl

typedef void *WRAPPER_ARG;
#define WRAPPER_RETURN() return
typedef HANDLE OSSEMAPHORE;

#include "hycomp.h"
/* this file might be included from a directory other than thread. Use a pseudo-absolute path to make 
 * sure that we find thrtypes.h in the right place. Only IBMC needs this.
 */
#include "thrtypes.h"
void initialize_thread_priority PROTOTYPE ((hythread_t thread));
IDATA init_thread_library PROTOTYPE ((void));
extern const int priority_map[];
extern struct HyThreadLibrary default_library;
extern BOOL (WINAPI * f_yield) (void);

/* Unused ID variable. */
extern DWORD unusedThreadID;
/* COND_DESTROY */
#define COND_DESTROY(cond) CloseHandle(cond)
/* TLS_GET */
#define TLS_GET(key) (TlsGetValue(key))
/* TLS_ALLOC */
#define TLS_ALLOC(key) ((key = TlsAlloc()) == 0xFFFFFFFF)
/* TLS_SET */
#define TLS_SET(key, value) (TlsSetValue(key, value))
/* THREAD_SELF */
#define THREAD_SELF() (GetCurrentThread())
/* THREAD_YIELD */
#if !defined(_WIN32_WINNT)
#define _WIN32_WINNT 0x0400
#endif
#define THREAD_YIELD() (f_yield())

/* THREAD_CREATE */

#define THREAD_CREATE(thread, stacksize, priority, entrypoint, entryarg)	\
	(((thread)->handle = (HANDLE)_beginthread((entrypoint), (stacksize), (entryarg))) != (HANDLE)(-1) && \
		hythread_set_priority((thread), (priority)) == 0)

/* COND_NOTIFY_ALL */
#define COND_NOTIFY_ALL(cond) SetEvent(cond)
/* COND_WAIT_IF_TIMEDOUT */
/* NOTE: the calling thread must already own mutex */
#define ADJUST_TIMEOUT(millis, nanos) (((nanos) && ((millis) != ((IDATA) (((UDATA)-1) >> 1)))) ? ((millis) + 1) : (millis))
#define COND_WAIT_IF_TIMEDOUT(cond, mutex, millis, nanos) 		\
	do {																								\
		DWORD starttime_ = GetTickCount(); \
		IDATA initialtimeout_, timeout_, rc_;			\
		initialtimeout_ = timeout_ = ADJUST_TIMEOUT(millis, nanos);														\
		while (1) {						\
			ResetEvent((cond));																			\
			MUTEX_EXIT(mutex);																		\
			rc_ = WaitForSingleObject((cond), timeout_);	\
			MUTEX_ENTER(mutex);				\
			if (rc_ == WAIT_TIMEOUT)

#define COND_WAIT_TIMED_LOOP()						\
			timeout_ = initialtimeout_ - (GetTickCount() - starttime_);	\
			if (timeout_ < 0) { timeout_ = 0; } \
		}	} while(0)

/* COND_WAIT */
/* NOTE: the calling thread must already own mutex */
#define COND_WAIT(cond, mutex) \
	do { \
		ResetEvent((cond));	\
		MUTEX_EXIT(mutex);	\
		WaitForSingleObject((cond), INFINITE);	\
		MUTEX_ENTER(mutex);

#define COND_WAIT_LOOP()	} while(1)

/* COND_INIT */
#define COND_INIT(cond) ((cond = CreateEvent(NULL, TRUE, FALSE, NULL)) != NULL)

/* TLS_DESTROY */
#define TLS_DESTROY(key) (TlsFree(key))

/* THREAD_CANCEL */
#define THREAD_CANCEL(thread) (TerminateThread(thread, (DWORD)-1)&&WaitForSingleObject(thread,INFINITE))

/* THREAD_EXIT */
#define THREAD_EXIT() _endthread()

/* THREAD_DETACH */
#define THREAD_DETACH(thread)	/* no need to do anything */

/* THREAD_SET_PRIORITY */
#define THREAD_SET_PRIORITY(thread, priority) (!SetThreadPriority((thread), (priority)))

/* SEM_CREATE */
/* Arbitrary maximum count */
#define SEM_CREATE(inval) CreateSemaphore(NULL,inval,2028,NULL)

/* SEM_INIT */
#define SEM_INIT(sm,pshrd,inval)  (sm != NULL) ? 0: -1

/* SEM_DESTROY */
#define SEM_DESTROY(sm)  CloseHandle(sm)

/* SEM_FREE */
#define SEM_FREE(s)

/* SEM_POST */
#define SEM_POST(sm)  (ReleaseSemaphore((sm),1,NULL) ? 0 : -1)

/* SEM_WAIT */
#define SEM_WAIT(sm)  ((WaitForSingleObject((sm), INFINITE) == WAIT_FAILED) ? -1 : 0)

/* SEM_TRYWAIT */
#define SEM_TRYWAIT(sm)  WaitForSingleObject(sm, 0)

/* SEM_GETVALUE */
#define SEM_GETVALUE(sm)
#if !defined(HYVM_STATIC_LINKAGE)
#define init_thread_library() (0)
#endif

#endif /* thrdsup_h */
