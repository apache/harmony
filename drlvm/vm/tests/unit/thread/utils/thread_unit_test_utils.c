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

#include <assert.h>
#include "jni.h"
#include "testframe.h"
#include "thread_unit_test_utils.h"
#include "jthread.h"
#include "open/hythread.h"
#include "open/hythread_ext.h"
#include "ti_thread.h"
#include "thread_manager.h"

/*
 * Utilities for thread manager unit tests 
 */

tested_thread_sturct_t dummy_tts_struct;
tested_thread_sturct_t * dummy_tts = &dummy_tts_struct;
tested_thread_sturct_t tested_threads[MAX_TESTED_THREAD_NUMBER];
JavaVM * GLOBAL_VM = NULL;

void sleep_a_click(void){
    hythread_sleep(CLICK_TIME_MSEC);
}

jthread new_jobject_thread(JNIEnv * jni_env) {
    const char * name = "<init>";
    const char * sig = "()V";
    jmethodID constructor = NULL;
    jclass thread_class;
    
    thread_class = (*jni_env)->FindClass(jni_env, "java/lang/Thread");
    constructor = (*jni_env)->GetMethodID(jni_env, thread_class, name, sig);
    return (*jni_env)->NewObject(jni_env, thread_class, constructor);
}

jobject new_jobject_thread_death(JNIEnv * jni_env) {
    const char * name = "<init>";
    const char * sig = "()V";
    jmethodID constructor = NULL;
    jclass thread_death_class;
    
    thread_death_class = (*jni_env)->FindClass(jni_env, "java/lang/ThreadDeath");
    constructor = (*jni_env)->GetMethodID(jni_env, thread_death_class, name, sig);
    return (*jni_env)->NewObject(jni_env, thread_death_class, constructor);
}

jthread new_jobject()
{
    _jobject *jobj;
    _jjobject *jjobj;

    jobj = (_jobject *)calloc(1, sizeof(_jobject));
    jjobj = (_jjobject *)calloc(1, sizeof(_jjobject));
    assert(jobj);
    assert(jjobj);
    jobj->object = jjobj;
    jobj->object->data = NULL;
    jobj->object->daemon = 0;
    jobj->object->name = NULL;
    return jobj;
}

void delete_jobject(jobject obj){
}

void test_java_thread_setup(int argc, char *argv[]) {
    create_java_vm_func CreateJavaVM;
    JavaVMInitArgs args;
    JNIEnv * jni_env;
    int i;

    CreateJavaVM = test_get_java_vm_ptr();
    assert(CreateJavaVM);

    args.version = JNI_VERSION_1_2;
    args.nOptions = argc;
    args.options = (JavaVMOption *) malloc(args.nOptions * sizeof(JavaVMOption));
    args.options[0].optionString = "-Djava.class.path=.";
    for (i = 1; i < argc; i++) {
        args.options[i].optionString = argv[i];
        args.options[i].extraInfo = NULL;
    }

    log_debug("test_java_thread_init()");

    hythread_sleep(1000);
    CreateJavaVM(&GLOBAL_VM, &jni_env, &args);
}

void test_java_thread_teardown(void) {

    IDATA status;

    log_debug("test_java_thread_shutdown()");
    //hythread_detach(NULL);

    // status = tm_shutdown(); ??????????????? fix me: 
    // second testcase don't work after tm_shutdown() call
    status = TM_ERROR_NONE; 
        
    /*
     * not required, if there are running threads tm will exit with
     * error TM_ERROR_RUNNING_THREADS
     */
    if(!(status == TM_ERROR_NONE || status == TM_ERROR_RUNNING_THREADS)){
        log_error("test_java_thread_shutdown() FAILED: %d", status);
    }
}

void tested_threads_init(int mode){
        
    tested_thread_sturct_t *tts;
    jobject monitor;
    jrawMonitorID raw_monitor;
    JNIEnv * jni_env;
    IDATA status; 
    int i;
    
    jni_env = jthread_get_JNI_env(jthread_self());
        
    if (mode != TTS_INIT_DIFFERENT_MONITORS){
        monitor = new_jobject();
        status = jthread_monitor_init(monitor);
        tf_assert_same_v(status, TM_ERROR_NONE);
    }
    status = jthread_raw_monitor_create(&raw_monitor);
    tf_assert_same_v(status, TM_ERROR_NONE);

    reset_tested_thread_iterator(&tts);
    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++){
        tts = &tested_threads[i];
        tts->my_index = i;
        tts->java_thread = new_jobject_thread(jni_env);
        tts->native_thread = NULL;
        tts->jvmti_start_proc_arg = &tts->jvmti_start_proc_arg;
        hysem_create(&tts->started, 0, 1);
        hysem_create(&tts->running, 0, 1);
        hysem_create(&tts->stop_request, 0, 1);
        hysem_create(&tts->ended, 0, 1);
        tts->phase = TT_PHASE_NONE;
        if (mode == TTS_INIT_DIFFERENT_MONITORS){
            monitor = new_jobject();
            status = jthread_monitor_init(monitor);
            tf_assert_same_v(status, TM_ERROR_NONE);
        }
        tts->monitor = monitor;
        tts->raw_monitor = raw_monitor;
        tts->excn = NULL;
    }
}

//void tested_threads_shutdown(void){
//      
//      tested_thread_sturct_t *tts;
//      int i;
//      
//      reset_tested_thread_iterator(&tts);
//      for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++){
//              tts = &tested_threads[i];
//        delete_jobject(tts->java_thread);
//        delete_JNIEnv(tts->jni_env);
//      }
//}

tested_thread_sturct_t *get_tts(int tts_index){
    
    if (tts_index >= 0 && tts_index < MAX_TESTED_THREAD_NUMBER){
        return &tested_threads[tts_index];
    }
    return (void *)NULL;
}

int next_tested_thread(tested_thread_sturct_t **tts_ptr){

    tested_thread_sturct_t *tts = *tts_ptr;
    int tts_index;

    if (! tts){
        tts = &tested_threads[0];
    } else {
        tts_index = tts->my_index;
        if (tts_index >= 0 && tts_index < MAX_TESTED_THREAD_NUMBER - 1){
            tts = &tested_threads[tts_index + 1];
        } else {
            tts = NULL;
        }
    }
    *tts_ptr = tts;
    return tts != NULL;
}

int prev_tested_thread(tested_thread_sturct_t **tts_ptr){
    
    tested_thread_sturct_t *tts = *tts_ptr;
    int tts_index;

    if (! tts){
        tts = &tested_threads[MAX_TESTED_THREAD_NUMBER - 1];
    } else {
        tts_index = tts->my_index;
        if (tts_index > 0 && tts_index < MAX_TESTED_THREAD_NUMBER){
            tts = &tested_threads[tts_index - 1];
        } else {
            tts = NULL;
        }
    }
    *tts_ptr = tts;
    return tts != NULL;
}

void reset_tested_thread_iterator(tested_thread_sturct_t **tts){
    *tts = (void *)NULL;
}

void tested_threads_run_common(jvmtiStartFunction run_method_param){
    tested_thread_sturct_t *tts;
    JNIEnv * jni_env = jthread_get_JNI_env(jthread_self());

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tts->attrs.proc = run_method_param;
        tts->attrs.arg = tts;
        tf_assert_same_v(jthread_create_with_function(jni_env, tts->java_thread, &tts->attrs), TM_ERROR_NONE);
        tested_thread_wait_started(tts);
        tts->native_thread = jthread_get_native_thread(tts->java_thread);
    }
}

void tested_threads_run(jvmtiStartFunction run_method_param){

    tested_threads_init(TTS_INIT_COMMON_MONITOR);
    tested_threads_run_common(run_method_param);
}

void tested_threads_run_with_different_monitors(jvmtiStartFunction run_method_param){

    tested_threads_init(TTS_INIT_DIFFERENT_MONITORS);
    tested_threads_run_common(run_method_param);
}

void tested_os_threads_run(hythread_entrypoint_t run_method_param){

    tested_thread_sturct_t *tts;
    IDATA status;

    tested_threads_init(TTS_INIT_COMMON_MONITOR);
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        // Create thread
        tts->native_thread = (hythread_t)calloc(1, sizeof(struct VM_thread));
        tf_assert_v(tts->native_thread != NULL);
        tts->native_thread->java_status = TM_STATUS_ALLOCATED;
        status = hythread_create_ex(tts->native_thread,
            NULL, 0, 0, NULL, run_method_param, tts);
        tf_assert_v(status == TM_ERROR_NONE);
        tested_thread_wait_started(tts);
    }
}

void tested_thread_started(tested_thread_sturct_t * tts) {
    hysem_set(tts->started, 1);
}

void tested_thread_ended(tested_thread_sturct_t * tts) {
    hysem_set(tts->ended, 1);
}

void tested_thread_send_stop_request(tested_thread_sturct_t * tts) {
    hysem_set(tts->stop_request, 1);    
}

void tested_thread_wait_for_stop_request(tested_thread_sturct_t * tts) {
    IDATA status;
    do {
        hysem_set(tts->running, 1);
        status = hysem_wait_timed(tts->stop_request, SLEEP_TIME, 0);
    } while (status == TM_ERROR_TIMEOUT);
}

IDATA tested_thread_wait_for_stop_request_timed(tested_thread_sturct_t * tts, I_64 sleep_time) {
    hysem_set(tts->running, 1);
    return hysem_wait_timed(tts->stop_request, sleep_time, 0);
}

int tested_threads_stop() {

    tested_thread_sturct_t *tts;
    
    reset_tested_thread_iterator(&tts);
    while (next_tested_thread(&tts)) {
        tested_thread_send_stop_request(tts);
    }
    while (next_tested_thread(&tts)) {
        tested_thread_wait_ended(tts);
    }
    return TEST_PASSED;
}

int tested_threads_destroy() {

    tested_thread_sturct_t *tts;
    
    reset_tested_thread_iterator(&tts);
    while (next_tested_thread(&tts)) {
        tested_thread_send_stop_request(tts);
    }
    reset_tested_thread_iterator(&tts);
    while (next_tested_thread(&tts)) {
        tested_thread_wait_dead(tts);
    }

    return TEST_PASSED;
}


int check_structure(tested_thread_sturct_t *tts){

    jthread java_thread = tts->java_thread;
    vm_thread_t vm_thread;
    jvmti_thread_t jvmti_thread;
    hythread_t hythread;

    vm_thread = jthread_get_vm_thread_from_java(java_thread);
    tf_assert(vm_thread);
    hythread = (hythread_t)vm_thread;
    tf_assert_same(hythread, tts->native_thread);
    jvmti_thread = &(vm_thread->jvmti_thread);
    tf_assert(jvmti_thread);
    return TEST_PASSED;
}

int check_phase(tested_thread_sturct_t *tts, int phase) {
    
    tf_assert(tts->phase != TT_PHASE_ERROR);
    if (phase == TT_PHASE_ANY) {
        tf_assert(tts->phase != TT_PHASE_NONE);
    } else if (tts->phase != phase) {
        log_info("Expected phase %d, but got %d", phase, tts->phase);
        tf_assert_same(tts->phase, phase);
    }
    return TEST_PASSED;
}

void tested_thread_wait_started(tested_thread_sturct_t *tts) {
    int i;
     
    i = 0;
    while (hysem_wait_timed(tts->started, MAX_TIME_TO_WAIT, 0) == TM_ERROR_TIMEOUT) {
        i++;
        printf("Thread %i hasn't started for %i milliseconds", 
            tts->my_index, (i * MAX_TIME_TO_WAIT));
    }

    hysem_post(tts->started);
}

void tested_thread_wait_running(tested_thread_sturct_t *tts) {
    int i;
     
    i = 0;
    while (hysem_wait_timed(tts->running, MAX_TIME_TO_WAIT, 0) == TM_ERROR_TIMEOUT) {
        i++;
        printf("Thread %i isn't running after %i milliseconds", 
            tts->my_index, (i * MAX_TIME_TO_WAIT));
    }    
}

void tested_thread_wait_ended(tested_thread_sturct_t *tts) {
    int i;
     
    i = 0;
    while (hysem_wait_timed(tts->ended, MAX_TIME_TO_WAIT, 0) == TM_ERROR_TIMEOUT) {
        i++;
        log_info("Thread %i hasn't ended for %i milliseconds",
            tts->my_index, (i * MAX_TIME_TO_WAIT));
    }
    hysem_post(tts->ended);
}

void tested_thread_wait_dead(tested_thread_sturct_t *tts) {
    test_thread_join(tts->native_thread, tts->my_index);
}

void test_thread_join(hythread_t native_thread, int index) {
    int i = 0;
    do {
        hythread_sleep(SLEEP_TIME);
        if(!hythread_is_alive(native_thread)) {
            break;
        }
        if (!i && (i % (MAX_TIME_TO_WAIT / SLEEP_TIME)) == 0) {
            printf("Thread %i isn't dead after %i milliseconds",
                index, (++i * SLEEP_TIME));
        }
    } while(1);
}

int compare_threads(jthread *threads, int thread_nmb, int compare_from_end) {

    int i;
    int j;
    int found;
    int tested_thread_start;
    jthread java_thread;

    // Check that all thread_nmb threads are different

    //printf("----------------------------------------------- %i %i\n", thread_nmb, compare_from_end);
    //for (j = 0; j < MAX_TESTED_THREAD_NUMBER; j++){
    //      printf("[%i] %p\n",j,tested_threads[j].java_thread->object);
    //}
    //printf("\n");
    //for (i = 0; i < thread_nmb; i++){
    //      printf("!!! %p\n", (*(threads + i))->object);
    //}
    for (i = 0; i < thread_nmb - 1; i++){
        java_thread = *(threads + i);
        for (j = i + 1; j < thread_nmb; j++){
            if (*(threads + j) == java_thread){
                return TM_ERROR_INTERNAL;
            }
        }
    }

    // Check that the set of threads are equal to the set of the first 
    // or the last thread_nmb tested threads

    tested_thread_start = compare_from_end ? MAX_TESTED_THREAD_NUMBER - thread_nmb : 0;
    for (i = 0; i < thread_nmb; i++){
        java_thread = *(threads + i);
        found = 0;
        for (j = tested_thread_start; j < thread_nmb + tested_thread_start; j++){
            if (tested_threads[j].java_thread->object == java_thread->object){
                found = 1;
                break;
            }
        }
        tf_assert(found);
    }
    return TM_ERROR_NONE;
}

int compare_pointer_sets(void ** set_a, void ** set_b, int nmb){
    return TEST_FAILED;
}

int check_exception(jobject excn){

    return TM_ERROR_INTERNAL;
    //return TM_ERROR_NONE;
}

void JNICALL default_run_for_test(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *arg) {

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *)arg;
    
    tts->phase = TT_PHASE_RUNNING;
    tested_thread_started(tts);
    tested_thread_wait_for_stop_request(tts);
    tts->phase = TT_PHASE_DEAD;
    tested_thread_ended(tts);
}

