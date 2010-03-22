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

#if !defined(hymutex_h)
#define hymutex_h

/* windows.h defined UDATA.  Ignore its definition */
#define UDATA UDATA_win32_
#include <windows.h>
#undef UDATA                    /* this is safe because our UDATA is a typedef, not a macro */
typedef CRITICAL_SECTION MUTEX;

/* MUTEX_INIT */
#define MUTEX_INIT(mutex) (InitializeCriticalSection(&(mutex)), 1)

/* MUTEX_DESTROY */
#define MUTEX_DESTROY(mutex) DeleteCriticalSection(&(mutex))

/* MUTEX_ENTER */
#define MUTEX_ENTER(mutex) EnterCriticalSection(&(mutex))

/*
 *  MUTEX_TRY_ENTER 
 *  returns 0 on success
 *  Beware: you may not have support for TryEnterCriticalSection 
 */
#define MUTEX_TRY_ENTER(mutex) (!(TryEnterCriticalSection(&(mutex))))

/* MUTEX_EXIT */
#define MUTEX_EXIT(mutex) LeaveCriticalSection(&(mutex))

#endif /* hymutex_h */
