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
 * @author Gregory Shimansky
 */  
#ifndef _JVMTI_INTERNAL_H_
#define _JVMTI_INTERNAL_H_

#include "jvmti_utils.h"
#include "vm_threads.h"
#include "jit_export_jpda.h"
#include <apr_dso.h>
#include <apr_strings.h>
#include "cxxlog.h"
#include "lock_manager.h"
#include "jvmti_dasm.h"

//using namespace du_ti;

typedef jint (JNICALL *f_Agent_OnLoad)
    (JavaVM *vm, char *options, void *reserved);

typedef void (JNICALL *f_Agent_OnUnLoad)
    (JavaVM *vm);

struct Agent
{
    const char* agentName;
    jint agent_id;
    jboolean dynamic_agent;
    apr_dso_handle_t* agentLib;
    apr_pool_t* pool;
    f_Agent_OnLoad Agent_OnLoad_func;
    f_Agent_OnUnLoad Agent_OnUnLoad_func;
    Agent* next;
    Agent(const char *name):
        agentLib(NULL),
        Agent_OnLoad_func(NULL),
        Agent_OnUnLoad_func(NULL),
        next(NULL){
        apr_pool_create(&pool, 0);
        agentName = apr_pstrdup(pool, name);
    }
    
    ~Agent() {
        apr_pool_destroy(pool);
    }
};

struct TIEnvList
{
    TIEnv *env;
    TIEnvList *next;
};

struct jvmti_frame_pop_listener
{
    jint depth;
    TIEnv *env;
    jvmti_frame_pop_listener *next;
};

struct jvmti_StepLocation
{
    struct Method* method;
    NativeCodePtr native_location;
    unsigned location;
    bool no_event;
};

struct JVMTISingleStepState
{
    VMBreakInterface* predicted_breakpoints;
};

/*
 * Type which will describe one watched field
 */
class Watch {
public:
    TIEnvList *envs;
    jfieldID field;
    Watch *next;

    Watch() : envs(NULL), field(0), next(NULL) {}

    void add_env(TIEnvList *el)
    {
        // FIXME: linked list modification without synchronization
        el->next = envs;
        envs = el;
    }

    TIEnvList *find_env(TIEnv *env)
    {
        for(TIEnvList *el = envs; NULL != el; el = el->next)
            if (el->env == env)
                return el;
        return NULL;
    }

    void remove_env(TIEnvList *el)
    {
        assert(envs);

        // FIXME: linked list modification without synchronization
        if (envs == el)
        {
            envs = envs->next;
            _deallocate((unsigned char *)el);
            return;
        }

        for (TIEnvList *p_el = envs->next; NULL != p_el->next; p_el = p_el->next)
            if (p_el->next == el)
            {
                p_el->next = el->next;
                _deallocate((unsigned char *)el);
                return;
            }

        DIE(("Can't find the element"));
    }
};

typedef struct Class Class;
class VMBreakPoints;
struct VMBreakPoint;

/*
 * JVMTI state of the VM
 */
class DebugUtilsTI {
    public:
        jint agent_counter;
        Lock_Manager TIenvs_lock;
        VMBreakPoints* vm_brpt;
        hythread_tls_key_t TL_ti_report; //thread local TI flag

        // TI event thread data
        vm_thread_t event_thread;
        hycond_t event_cond;
        int event_cond_initialized;

        DebugUtilsTI();

        ~DebugUtilsTI();
        jint Init(JavaVM *vm);
        void Shutdown(JavaVM *vm);
        void setExecutionMode(Global_Env *p_env);
        int getVersion(char* version);
        void addAgent(const char*); // add agent name (string)
        Agent *getAgents();
        void setAgents(Agent *agent);
        bool isEnabled();
        void addEventSubscriber(jvmtiEvent event_type);
        void removeEventSubscriber(jvmtiEvent event_type);
        bool hasSubscribersForEvent(jvmtiEvent event_type);
        bool shouldReportEvent(jvmtiEvent event_type);
        void setEnabled();
        void setDisabled();

        bool needCreateEventThread();
        void enableEventThreadCreation();
        void disableEventThreadCreation();

        bool shouldReportLocally();
        void doNotReportLocally();
        void reportLocally();

        jvmtiPhase getPhase()
        {
            return phase;
        }

        void nextPhase(jvmtiPhase phase)
        {
            this->phase = phase;
        }

        void addEnvironment(TIEnv *env)
        {
            // assert(TIenvs_lock._lock_or_null());

            env->next = p_TIenvs;
            p_TIenvs = env;
        }

        void removeEnvironment(TIEnv *env)
        {
            TIEnv *e = p_TIenvs;

            if (NULL == e)
                return;

            // assert(TIenvs_lock._lock_or_null());

            if (e == env)
            {
                p_TIenvs = env->next;
                return;
            }

            while (NULL != e->next)
            {
                if (e->next == env)
                {
                    e->next = env->next;
                    return;
                }
                e = e->next;
            }
        }

        TIEnv *getEnvironments(void)
        {
            // assert(TIenvs_lock._lock_or_null());

            return p_TIenvs;
        }

        void enumerate();

        // Watched fields' support

        Watch** get_access_watch_list()
        {
            return &access_watch_list;
        }

        Watch** get_modification_watch_list()
        {
            return &modification_watch_list;
        }

        Watch *find_watch(Watch** p_watch_list, jfieldID f)
        {
            for (Watch *w = *p_watch_list; NULL != w; w = w->next)
                if (w->field == f)
                    return w;

            return NULL;
        }

        void add_watch(Watch** p_watch_list, Watch *w)
        {
            // FIXME: linked list modification without synchronization
            w->next = *p_watch_list;
            *p_watch_list = w;
        }

        void remove_watch(Watch** p_watch_list, Watch *w)
        {
            assert(*p_watch_list);

            if (w == *p_watch_list)
            {
                *p_watch_list = w->next;
                _deallocate((unsigned char *)w);
                return;
            }

            // FIXME: linked list modification without synchronization
            for (Watch *p_w = *p_watch_list; NULL != p_w->next; p_w = p_w->next)
                if (p_w->next == w)
                {
                    p_w->next = w->next;
                    _deallocate((unsigned char *)w);
                    return;
                }

            DIE(("Can't find the watch"));
        }

        void SetPendingNotifyLoadClass( Class *klass );
        void SetPendingNotifyPrepareClass( Class *klass );
        unsigned GetNumberPendingNotifyLoadClass();
        unsigned GetNumberPendingNotifyPrepareClass();
        Class * GetPendingNotifyLoadClass( unsigned number );
        Class * GetPendingNotifyPrepareClass( unsigned number );
        void ReleaseNotifyLists();

        enum GlobalCapabilities {
            TI_GC_ENABLE_METHOD_ENTRY             = 0x01,
            TI_GC_ENABLE_METHOD_EXIT              = 0x02,
            TI_GC_ENABLE_FRAME_POP_NOTIFICATION   = 0x04,
            TI_GC_ENABLE_SINGLE_STEP              = 0x08,
            TI_GC_ENABLE_EXCEPTION_EVENT          = 0x10,
            TI_GC_ENABLE_FIELD_ACCESS_EVENT       = 0x20,
            TI_GC_ENABLE_FIELD_MODIFICATION_EVENT = 0x40,
            TI_GC_ENABLE_POP_FRAME                = 0x80,
            TI_GC_ENABLE_TAG_OBJECTS              = 0x100,
            TI_GC_ENABLE_MONITOR_EVENTS           = 0x200,
        };

        void set_global_capability(GlobalCapabilities ti_gc)
        {
            global_capabilities |= ti_gc;
        }

        void reset_global_capability(GlobalCapabilities ti_gc)
        {
            global_capabilities &= ~ti_gc;
        }

        unsigned get_global_capability(GlobalCapabilities ti_gc)
        {
            return global_capabilities & ti_gc;
        }

        bool is_single_step_enabled(void)
        {
            return single_step_enabled && (phase == JVMTI_PHASE_LIVE);
        }

        // Single step functions
        jvmtiError jvmti_single_step_start(void);
        jvmtiError jvmti_single_step_stop(void);

        bool is_cml_report_inlined()
        {
            return cml_report_inlined;
        }

        void set_cml_report_inlined(bool value)
        {
            cml_report_inlined = value;
        }

        char *get_method_entry_flag_address()
        {
            return &method_entry_enabled_flag;
        }

        char *get_method_exit_flag_address()
        {
            return &method_exit_enabled_flag;
        }

        char get_method_entry_flag()
        {
            return method_entry_enabled_flag;
        }

        char get_method_exit_flag()
        {
            return method_exit_enabled_flag;
        }

        void set_method_entry_flag(char value)
        {
            method_entry_enabled_flag = value;
        }

        void set_method_exit_flag(char value)
        {
            method_exit_enabled_flag = value;
        }

    private:

    protected:
        friend jint JNICALL create_jvmti_environment(JavaVM *vm, void **env, jint version);
        Watch *access_watch_list;
        Watch *modification_watch_list;
        bool status;
        bool need_create_event_thread;
        Agent* agents;
        TIEnv* p_TIenvs;
        jvmtiPhase phase;
        const unsigned MAX_NOTIFY_LIST;
        Class **notifyLoadList;
        unsigned loadListNumber; 
        Class **notifyPrepareList;
        unsigned prepareListNumber;
        unsigned global_capabilities;
        bool single_step_enabled;
        bool cml_report_inlined;
        char method_entry_enabled_flag, method_exit_enabled_flag;
        unsigned event_needed[TOTAL_EVENT_TYPE_NUM];
}; /* end of class DebugUtilsTI */

jvmtiError add_event_to_thread(jvmtiEnv *env, jvmtiEvent event_type, jthread event_thread);
void remove_event_from_thread(jvmtiEnv *env, jvmtiEvent event_type, jthread event_thread);
void add_event_to_global(jvmtiEnv *env, jvmtiEvent event_type);
void remove_event_from_global(jvmtiEnv *env, jvmtiEvent event_type);
jthread getCurrentThread();

jint load_agentlib(Agent *agent, const char *str, JavaVM_Internal *vm);
jint load_agentpath(Agent *agent, const char *str, JavaVM_Internal *vm);

// Breakpoints internal functions
jvmtiError jvmti_get_next_bytecodes_from_native(VM_thread *thread,
    jvmti_StepLocation **next_step, unsigned *count, bool invoked_frame);
void jvmti_set_single_step_breakpoints(DebugUtilsTI *ti,
    jvmti_thread_t jvmti_thread, jvmti_StepLocation *locations,
    unsigned locations_number);
void jvmti_set_single_step_breakpoints_for_method(DebugUtilsTI *ti,
    jvmti_thread_t jvmti_thread, Method* method);
void jvmti_remove_single_step_breakpoints(DebugUtilsTI *ti, jvmti_thread_t jvmti_thread);

// NCAI extension
jvmtiError JNICALL jvmtiGetNCAIEnvironment(jvmtiEnv* jvmti_env, ...);

// Object check functions
Boolean is_valid_throwable_object(jthread thread);
Boolean is_valid_thread_object(jthread thread);
Boolean is_valid_thread_group_object(jthreadGroup group);
Boolean is_valid_class_object(jclass klass);

// JIT support
jvmtiError jvmti_translate_jit_error(OpenExeJpdaError error);

// Single step support
void jvmti_SingleStepLocation(VM_thread* thread, Method *method,
    unsigned location, jvmti_StepLocation **next_step, unsigned *count);

// Callback function for JVMTI breakpoint processing
bool jvmti_process_breakpoint_event(TIEnv *env, const VMBreakPoint* bp, const POINTER_SIZE_INT data);

#endif /* _JVMTI_INTERNAL_H_ */
