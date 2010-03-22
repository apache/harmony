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
 * @author Li-Gang Wang, 2006/11/15
 */

#ifndef _REF_ENQUEUE_THREAD_H_
#define _REF_ENQUEUE_THREAD_H_

#include <assert.h>
#include "jni_types.h"
#include "open/hythread_ext.h"
#include "open/types.h"


#define REF_ENQUEUE_THREAD_PRIORITY HYTHREAD_PRIORITY_USER_MAX
#define REF_ENQUEUE_THREAD_NUM 1


typedef struct Ref_Enqueue_Thread_Info {
    hysem_t pending_sem;    // weakref pending event
    hysem_t attached_sem;   // ref enqueue thread attached event
    
    /* Using pair of cond and mutex rather than sem is because the waiting thread num is not constant */
    hycond_t end_cond;      // ref enqueue end condition variable
    osmutex_t end_mutex;    // ref enqueue end mutex
    
    Boolean shutdown;
    volatile unsigned int thread_num;
    volatile unsigned int end_waiting_num;  // thread num waiting for finalization end
}Ref_Enqueue_Thread_Info;


extern void ref_enqueue_thread_init(JavaVM *java_vm, JNIEnv* jni_env);
extern void ref_enqueue_shutdown(void);
extern void activate_ref_enqueue_thread(Boolean wait);

inline void native_sync_enqueue_references(void)
{ activate_ref_enqueue_thread(TRUE); };

#endif // _REF_ENQUEUE_THREAD_H_
