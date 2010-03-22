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
 * @author Ilya Berezhniuk
 */
#ifndef _NCAI_INTERNAL_H_
#define _NCAI_INTERNAL_H_

#include "ncai.h"
#include "open/ncai_thread.h"
#include "ncai_direct.h"
#include "vm_threads.h"



#ifdef __cplusplus
extern "C" {
#endif

typedef struct _NcaiRegisterTableItem
{
    const char* name;       // Register name
    jint        size;       // Register size in bytes
    unsigned    offset;     // Register offset in NcaiRegisters structure

} NcaiRegisterTableItem;

// Register table is specific for target processor architecture
extern NcaiRegisterTableItem g_ncai_reg_table[];


void clean_all_modules(ncaiModule* pmodules);
void ncai_library_load_callback(const char* name);
void ncai_library_unload_callback(const char* name);


// These functions are differ for various architectures
size_t ncai_get_reg_table_size();
bool ncai_get_register_value(hythread_t thread, jint reg_number, void* buf_ptr);
bool ncai_set_register_value(hythread_t thread, jint reg_number, void* buf_ptr);
void* ncai_get_instruction_pointer(hythread_t thread);

// These functions are differ for various operating systems
bool ncai_get_generic_registers(hythread_t handle, Registers* regs);
char* ncai_parse_module_name(char* filepath);


struct VMBreakPoint;

// Callback function for NCAI breakpoint processing
bool ncai_process_breakpoint_event(TIEnv *env, const VMBreakPoint* bp,
                                    const POINTER_SIZE_INT data);


/*
 * Global NCAI data
 */
class GlobalNCAI
{
public:
    bool    enabled; // Is NCAI enabled

    bool step_enabled; // Is Single Stepping enabled
    ncaiStepMode step_mode; // Global step mode

    Lock_Manager mod_lock;
    ncaiModule modules;

public:
    GlobalNCAI();
    ~GlobalNCAI();

    static bool isEnabled();

}; /* end of GlobalNCAI */


struct st_pending_ss;

struct NCAISingleStepState
{
    // Predicted breakpoints
    VMBreakInterface* breakpoints;
    // Postponed breakpoints
    st_pending_ss* pplist;
    // Step mode: OFF/INTO/OVER/OUT
    bool use_local_mode;
    ncaiStepMode step_mode;
    // Report STEP_OUT event if set
    bool flag_out;
};

// Allocate thread-local structures for single step processing
void ncai_check_alloc_ss_data(jvmti_thread_t jvmti_thread);

// Functions to enable/disable single stepping if needed
ncaiError ncai_start_single_step(NCAIEnv* env);
ncaiError ncai_stop_single_step(NCAIEnv* env);
bool ncai_start_thread_single_step(NCAIEnv* env, hythread_t thread);
void ncai_stop_thread_single_step(jvmti_thread_t jvmti_thread);

// Must be called under breakpoint lock
ncaiStepMode ncai_get_thread_ss_mode(jvmti_thread_t jvmti_thread);

// Returns type of specified adress
ncaiModuleKind ncai_get_target_address_type(void* addr);
// Instrument predicted locations for single step
void ncai_setup_single_step(GlobalNCAI* ncai,
            const VMBreakPoint* bp, jvmti_thread_t jvmti_thread);
// Instrument predicted HWE handlers
void ncai_setup_signal_step(jvmti_thread_t jvmti_thread, NativeCodePtr addr);

// Function to send Signal events
void ncai_process_signal_event(NativeCodePtr addr,
        jint code, bool is_internal, bool* p_handled);

// Method Entry/Exit events processing
void ncai_report_method_entry(jmethodID method);
void ncai_report_method_exit(jmethodID method, jboolean exc_popped, jvalue ret_val);
void ncai_step_native_method_entry(Method* m);
void ncai_step_native_method_exit(Method* m);

// Allows to get modules without providing NCAI environment
ncaiError ncai_get_all_loaded_modules(ncaiEnv *env,
    jint *count_ptr, ncaiModule **modules_ptr);


#ifdef __cplusplus
}
#endif
#endif /* _NCAI_INTERNAL_H_ */
