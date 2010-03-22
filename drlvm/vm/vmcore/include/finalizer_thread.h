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

#ifndef _FINALIZER_THREAD_H_
#define _FINALIZER_THREAD_H_

#include <assert.h>
#include <apr_atomic.h>
#include "jni_types.h"
#include "port_sysinfo.h"
#include "open/hythread_ext.h"
#include "open/types.h"
#include "open/gc.h"
#include "thread_manager.h"


#define FINALIZER_THREAD_PRIORITY (HYTHREAD_PRIORITY_NORMAL + 2)

#define MUTATOR_BLOCK_THRESHOLD_BITS 6
#define MUTATOR_BLOCK_THRESHOLD ((1 << MUTATOR_BLOCK_THRESHOLD_BITS) - 3)
#define MUTATOR_RESUME_THRESHOLD_BITS 8
#define MUTATOR_RESUME_THRESHOLD ((1 << MUTATOR_RESUME_THRESHOLD_BITS) - 3)

#define FIN_MAX_WAIT_TIME_BITS 7
#define FIN_MAX_WAIT_TIME (1 << FIN_MAX_WAIT_TIME_BITS)


typedef struct Fin_Thread_Info {
    hysem_t pending_sem;            // finalizer pending event
    hysem_t attached_sem;           // fin thread attached event
    
    /* Using pair of cond and mutex rather than sem is because the waiting thread num is not constant */
    hycond_t end_cond;              // finalization end condition variable
    osmutex_t end_mutex;            // finalization end mutex
    
    /* Using pair of cond and mutex rather than sem is because the waiting thread num is not constant */
    hycond_t mutator_block_cond;    // mutator block condition variable for heavy finalizable obj load
    osmutex_t mutator_block_mutex;  // mutator block mutex for heavy finalizable obj load
    
    vm_thread_t *thread_ids;
    volatile unsigned int thread_num;
    
    volatile Boolean shutdown;
    volatile Boolean on_exit;
    volatile unsigned int working_thread_num;
    volatile unsigned int end_waiting_num;      // thread num waiting for finalization end
}Fin_Thread_Info;


extern Boolean get_finalizer_shutdown_flag();
extern Boolean get_finalizer_on_exit_flag();
extern void finalizer_threads_init(JavaVM *java_vm, JNIEnv* jni_env);
extern void finalizer_shutdown(Boolean start_finalization_on_exit);
extern void activate_finalizer_threads(Boolean wait);

inline void native_sync_run_finalization(void)
{ activate_finalizer_threads(TRUE); };


extern void vm_heavy_finalizer_resume_mutator(void);

extern unsigned int cpu_num_bits;

inline void sched_heavy_finalizer(unsigned int finalizable_obj_num)
{
    unsigned int block_threshold = MUTATOR_BLOCK_THRESHOLD << cpu_num_bits;
    if(finalizable_obj_num >= block_threshold && !(finalizable_obj_num % block_threshold))
        gc_set_mutator_block_flag();
}

inline void sched_heavy_finalizer_in_finalization(unsigned int finalizable_obj_num, unsigned int finalized_obj_num)
{
    sched_heavy_finalizer(finalizable_obj_num);
    unsigned int resume_threshold = MUTATOR_RESUME_THRESHOLD;// << cpu_num_bits;
    if(finalized_obj_num >= resume_threshold && !(finalized_obj_num % resume_threshold))
        vm_heavy_finalizer_resume_mutator();
}

#endif // _FINALIZER_THREAD_H_
