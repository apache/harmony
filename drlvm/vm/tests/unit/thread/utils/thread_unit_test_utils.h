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

#include <stdlib.h>
#include "jvmti_types.h"
#include "open/hycomp.h"
#include "jthread.h"
#include "thread_manager.h"

// Tested Thread Phases
#define TT_PHASE_NONE 0
#define TT_PHASE_DEAD 1
#define TT_PHASE_OK 2
#define TT_PHASE_ERROR 3
#define TT_PHASE_SLEEPING 4
#define TT_PHASE_WAITING 5
#define TT_PHASE_IN_CRITICAL_SECTON 7
#define TT_PHASE_WAITING_ON_MONITOR 8
#define TT_PHASE_WAITING_ON_WAIT 9
#define TT_PHASE_WAITING_ON_JOIN 10
#define TT_PHASE_RUNNING 11
#define TT_PHASE_PARKED 12
#define TT_PHASE_ATTACHED 13
#define TT_PHASE_ATTACHED_TWICE 14
#define TT_PHASE_STEP_1 15
#define TT_PHASE_DETACHED 16
#define TT_PHASE_INTERRUPTED 17
#define TT_PHASE_ANY 18

#define TTS_INIT_COMMON_MONITOR 0
#define TTS_INIT_DIFFERENT_MONITORS 1

#define MAX_TESTED_THREAD_NUMBER 5
#define MAX_TIME_TO_WAIT 600000
#define MAX_OWNED_MONITORS_NMB 2
#define SLEEP_TIME 100
#define CLICK_TIME_MSEC 10

extern JavaVM * GLOBAL_VM;

typedef struct _jjobject{
    void *data;
    jboolean daemon;
    char *name;
    int lockword;
}_jjobject;

typedef struct _jobject{
    _jjobject *object;
}_jobject;

typedef struct {
    int my_index;
    jthread java_thread;
    hythread_t native_thread;
    jobject monitor;
    jrawMonitorID raw_monitor;
    void * jvmti_start_proc_arg;
    hysem_t started;
    hysem_t running;
    hysem_t stop_request;
    hysem_t ended;
    int phase;
    jint peak_count;
    struct jthread_start_proc_data attrs;
    jclass excn;
} tested_thread_sturct_t;

void sleep_a_click(void);
void test_java_thread_setup(int argc, char *argv[]);
void test_java_thread_teardown(void);
void tested_threads_init(int mode);
void tested_threads_run(jvmtiStartFunction run_method_param);
void tested_threads_run_common(jvmtiStartFunction run_method_param);
void tested_threads_run_with_different_monitors(jvmtiStartFunction run_method_param);
void tested_threads_run_with_jvmti_start_proc(jvmtiStartFunction jvmti_start_proc);
void tested_os_threads_run(hythread_entrypoint_t run_method_param);

int tested_threads_destroy();
int tested_threads_stop();

tested_thread_sturct_t *get_tts(int tts_index);
int next_tested_thread(tested_thread_sturct_t **tts);
int prev_tested_thread(tested_thread_sturct_t **tts);
void reset_tested_thread_iterator(tested_thread_sturct_t ** tts);

#define check_tested_thread_phase(tts, phase) if (check_phase(tts, phase) != TEST_PASSED) return TEST_FAILED;
#define check_tested_thread_structures(tts) if (check_structure(tts) != TEST_PASSED) return TEST_FAILED;
int check_structure(tested_thread_sturct_t *tts);
int check_phase(tested_thread_sturct_t *tts, int phase);

void tested_thread_started(tested_thread_sturct_t * tts);
void tested_thread_ended(tested_thread_sturct_t * tts);
void tested_thread_send_stop_request(tested_thread_sturct_t * tts);
void tested_thread_wait_for_stop_request(tested_thread_sturct_t * tts);
IDATA tested_thread_wait_for_stop_request_timed(tested_thread_sturct_t * tts, I_64 sleep_time);

void tested_thread_wait_started(tested_thread_sturct_t *tts);
void tested_thread_wait_running(tested_thread_sturct_t *tts);
void tested_thread_wait_ended(tested_thread_sturct_t *tts);
void tested_thread_wait_dead(tested_thread_sturct_t *tts);
void test_thread_join(hythread_t native_thread, int index);

int compare_threads(jthread *threads, int thread_nmb, int compare_from_end);
int compare_pointer_sets(void ** set_a, void ** set_b, int nmb);
int check_exception(jobject excn);
void set_phase(tested_thread_sturct_t *tts, int phase);
void JNICALL default_run_for_test(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *arg);
jthread new_jobject_thread(JNIEnv * jni_env);
jobject new_jobject_thread_death(JNIEnv * jni_env);
jthread new_jobject();
void delete_jobject(jobject obj);

typedef jint (JNICALL *create_java_vm_func)(JavaVM **vm, JNIEnv **env,
    JavaVMInitArgs *args);
create_java_vm_func test_get_java_vm_ptr(void);
