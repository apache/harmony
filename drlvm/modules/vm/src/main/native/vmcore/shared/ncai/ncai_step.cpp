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

#define LOG_DOMAIN "ncai.step"
#include "cxxlog.h"
#include "open/vm_method_access.h"
#include "suspend_checker.h"
#include "jvmti_break_intf.h"
#include "classloader.h"

#include "open/ncai_thread.h"
#include "ncai_utils.h"
#include "ncai_direct.h"
#include "ncai_internal.h"


static void ncai_send_single_step_events(DebugUtilsTI* ti,
                    ncaiThread thread, NativeCodePtr addr);
// Clean up postponed SS breakpoints list
static void clear_pp_breakpoints(NCAISingleStepState* sss);
// Check if instruction should not be reported in STEP_OUT mode (RET instruction)
static bool ncai_do_not_report_step_out(NCAISingleStepState* sss);


// Callback function for NCAI breakpoint processing
static bool ncai_process_single_step_event(TIEnv *ti_env,
                        const VMBreakPoint* bp, const POINTER_SIZE_INT data)
{
    TRACE2("ncai.step", "SINGLE STEP BREAKPOINT occured, address = " << bp->addr);
    assert(ti_env == NULL);
    assert(bp);
    assert(bp->addr);

    VM_thread* vm_thread = p_TLS_vmthread;
    assert(vm_thread);
    if (!vm_thread)
        return false;

    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

    // This check works for current thread only
    if (jvmti_thread->flag_ncai_handler) // Recursion
        return true;

    jvmti_thread->flag_ncai_handler = true;

    DebugUtilsTI* ti = VM_Global_State::loader_env->TI;
    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;
    VMBreakPoints* vm_brpt = ti->vm_brpt;
    NativeCodePtr addr = bp->addr;

    vm_brpt->lock();
    NCAISingleStepState* sss = jvmti_thread->ncai_ss;

    if (!sss)
    {
        vm_brpt->unlock();
        jvmti_thread->flag_ncai_handler = false;
        return false;
    }

    ncaiStepMode step_mode = ncai_get_thread_ss_mode(jvmti_thread);
    bool suspend_enabled = true;

    vm_brpt->unlock();

    if (!ncai->step_enabled)
    {
        TRACE2("ncai.step", "SINGLE STEP skipped due to disabling");
    }
    else if (step_mode == NCAI_STEP_OUT && ncai_do_not_report_step_out(sss))
    {
        TRACE2("ncai.step", "SINGLE STEP skipped due to unwanted instruction for NCAI_STEP_OUT");
    }
    else if (step_mode == NCAI_STEP_OFF)
    {
        TRACE2("ncai.step", "SINGLE STEP skipped due to NCAI_STEP_OFF");
    }
    else
    {
        ncaiModuleKind type = ncai_get_target_address_type(addr);

        if (type == NCAI_MODULE_JNI_LIBRARY)
        {
            suspend_enabled = hythread_is_suspend_enabled();

            if (!suspend_enabled)
                hythread_suspend_enable();

            hythread_t hythread = hythread_self();
            ncaiThread thread = reinterpret_cast<ncaiThread>(hythread);
            assert(vm_thread == jthread_get_vm_thread(hythread));

            ncai_send_single_step_events(ti, thread, addr);
        }
    }

    // Set breakpoints on instructions after the current one
    ncai_setup_single_step(ncai, bp, jvmti_thread);

    if (!suspend_enabled)
        hythread_suspend_disable();

    jvmti_thread->flag_ncai_handler = false;
    return true;
}

static inline bool ncai_do_not_report_step_out(NCAISingleStepState* sss)
{
    return !sss->flag_out;
}

static void ncai_send_single_step_events(DebugUtilsTI* ti,
                    ncaiThread thread, NativeCodePtr addr)
{
    TIEnv* env = ti->getEnvironments();
    TIEnv* next_env;

    while (NULL != env)
    {
        next_env = env->next;
        NCAIEnv* ncai_env = env->ncai_env;

        if (NULL == ncai_env)
        {
            env = next_env;
            continue;
        }

        ncaiStep func =
            (ncaiStep)ncai_env->get_event_callback(NCAI_EVENT_STEP);

        if (NULL != func)
        {
            if (ncai_env->global_events[NCAI_EVENT_STEP - NCAI_MIN_EVENT_TYPE_VAL])
            {
                TRACE2("ncai.step", "Calling global SingleStep callback for address = " << addr);

                func((ncaiEnv*)ncai_env, thread, (void*)addr);

                TRACE2("ncai.step", "Finished global SingleStep callback for address = " << addr);

                env = next_env;
                continue;
            }

            ncaiEventThread* next_et;
            ncaiEventThread* first_et =
                ncai_env->event_threads[NCAI_EVENT_STEP - NCAI_MIN_EVENT_TYPE_VAL];

            for (ncaiEventThread* et = first_et; NULL != et; et = next_et)
            {
                next_et = et->next;

                if (et->thread == thread)
                {
                    TRACE2("ncai.step", "Calling local SingleStep callback for address = " << addr);

                    func((ncaiEnv*)ncai_env, thread, (void*)addr);

                    TRACE2("ncai.step", "Finished local SingleStep callback for address = " << addr);
                }

                et = next_et;
            }
        }

        env = next_env;
    }
}

ncaiModuleKind ncai_get_target_address_type(void* addr)
{
    jint count;
    ncaiModule *modules;

    ncaiError err = ncai_get_all_loaded_modules(NULL, &count, &modules);
    assert(err == NCAI_ERROR_NONE);

    ncaiModuleKind kind = NCAI_MODULE_OTHER;
    bool found = false;

    for (jint mod_index = 0; mod_index < count; mod_index++)
    {
        ncaiModuleInfo* info = modules[mod_index]->info;

        for (size_t num = 0; !found && num < info->segment_count; num++)
        {
            ncaiSegmentInfo* seg = info->segments + num;

            if (seg->base_address <= addr &&
                (char*)seg->base_address + seg->size > addr)
            {
                found = true;
                kind = info->kind;
                break;
            }
        }
    }

    ncai_free(modules);
    return kind;
}

static bool step_suspend_thread(NCAIEnv* env, hythread_t thread)
{
    if (thread == hythread_self())
        return true;

    if (!jthread_get_vm_thread(thread))
        return false; // will be enabled in THREAD_START event

    return (ncaiSuspendThread((ncaiEnv*)env, (ncaiThread)thread) == NCAI_ERROR_NONE);
}

static void step_resume_thread(NCAIEnv* env, hythread_t thread)
{
    if (thread == hythread_self())
        return;

    if (!jthread_get_vm_thread(thread))
        return;

    ncaiResumeThread((ncaiEnv*)env, (ncaiThread)thread);
}

bool ncai_start_thread_single_step(NCAIEnv* env, hythread_t thread)
{
    vm_thread_t vm_thread = jthread_get_vm_thread(thread);

    if (!vm_thread)
        return false;

    // Suspend thread
    if (!step_suspend_thread(env, thread))
        return false;

    VMBreakPoints* vm_brpt = env->ti_env->vm->vm_env->TI->vm_brpt;
    VMLocalBreak* local_bp = vm_brpt->find_thread_local_break(vm_thread);

    if (!local_bp)
    {
        step_resume_thread(env, thread);
        return false;
    }

    VMBreakPoint* bp = local_bp->local_bp;
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

    ncai_check_alloc_ss_data(jvmti_thread);

    if (jvmti_thread->ncai_ss->breakpoints)
        jvmti_thread->ncai_ss->breakpoints->remove_all_reference();
    else
    {
        jvmti_thread->ncai_ss->breakpoints =
            vm_brpt->new_intf(NULL, ncai_process_single_step_event,
            PRIORITY_NCAI_STEP_BREAKPOINT, false);
        assert(jvmti_thread->ncai_ss->breakpoints);
    }

    jvmti_thread->ncai_ss->flag_out = false;
    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;
    ncai_setup_single_step(ncai, bp, jvmti_thread);

    // Resume thread
    step_resume_thread(env, thread);
    return true;
}

ncaiError ncai_start_single_step(NCAIEnv* env)
{
    TRACE2("ncai.step", "ncai_start_single_step called");
    SuspendEnabledChecker sec;

    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;

    if (ncai->step_enabled) // single step is enabled already
        return NCAI_ERROR_NONE;

    ncaiThread* threads;
    jint thread_count;

    ncaiError err = ncaiGetAllThreads((ncaiEnv*)env, &thread_count, &threads);

    if (err != NCAI_ERROR_NONE)
        return err;

    assert(thread_count > 0);

    VMBreakPoints* vm_brpt = env->ti_env->vm->vm_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_brpt->get_lock());

    ncai->step_enabled = true;

    for (jint i = 0; i < thread_count; i++)
    {
        ncai_start_thread_single_step(env, (hythread_t)threads[i]);
    }

    return NCAI_ERROR_NONE;
}

void ncai_stop_thread_single_step(jvmti_thread_t jvmti_thread)
{
    VMBreakPoints* vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;
    NCAISingleStepState* sss = jvmti_thread->ncai_ss;
    assert(sss);

    if (jvmti_thread->ncai_ss->breakpoints)
    {
        jvmti_thread->ncai_ss->breakpoints->remove_all_reference();
        vm_brpt->release_intf(jvmti_thread->ncai_ss->breakpoints);
    }

    clear_pp_breakpoints(jvmti_thread->ncai_ss);
    ncai_free(jvmti_thread->ncai_ss);
    jvmti_thread->ncai_ss = NULL;
}

ncaiError ncai_stop_single_step(NCAIEnv* env)
{
    TRACE2("ncai.step", "ncai_stop_single_step called");
    SuspendEnabledChecker sec;

    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;

    if (!ncai->step_enabled) // single step is disabled already
        return NCAI_ERROR_NONE;

    ncaiThread* threads;
    jint thread_count;

    ncaiError err = ncaiGetAllThreads((ncaiEnv*)env, &thread_count, &threads);

    if (err != NCAI_ERROR_NONE)
        return err;

    assert(thread_count > 0);

    VMBreakPoints* vm_brpt = env->ti_env->vm->vm_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_brpt->get_lock());

    ncai->step_enabled = false;

    for (jint i = 0; i < thread_count; i++)
    {
        hythread_t hy_thread = (hythread_t)threads[i];
        vm_thread_t vm_thread = jthread_get_vm_thread(hy_thread);

        if (!vm_thread)
            continue;

        jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

        if (!jvmti_thread->ncai_ss)
            continue;

        // Suspend thread
        if (!step_suspend_thread(env, hy_thread))
            continue;

        ncai_stop_thread_single_step(jvmti_thread);

        // Resume thread
        step_resume_thread(env, hy_thread);
    }

    return NCAI_ERROR_NONE;
}

void ncai_check_alloc_ss_data(jvmti_thread_t jvmti_thread)
{
    if (jvmti_thread->ncai_ss)
        return;

    jvmti_thread->ncai_ss =
        (NCAISingleStepState*)ncai_alloc(sizeof(NCAISingleStepState));
    assert(jvmti_thread->ncai_ss);

    jvmti_thread->ncai_ss->breakpoints = NULL;
    jvmti_thread->ncai_ss->pplist = NULL;
    jvmti_thread->ncai_ss->step_mode = NCAI_STEP_INTO;
    jvmti_thread->ncai_ss->use_local_mode = true;
    jvmti_thread->ncai_ss->flag_out = false;
}

// Infrastructure to store postponed single step breakpoints
// which are used to continue single stepping after returning to JNI code from
// VM or managed code

struct st_pending_ss
{
    st_pending_ss*  next;
    Method*         m;
    size_t          count;
    void*           addrs[1];
};


static void add_pp_breakpoints(Method* m, bool is_dummy)
{
    TRACE2("ncai.step.pp", "Storing postponed SS breakpoints entering method = " <<
         m << ", is_dummy = " << (is_dummy ? "true" : "false"));

    VM_thread* vm_thread = p_TLS_vmthread;
    assert(vm_thread);

    NCAISingleStepState* sss = vm_thread->jvmti_thread.ncai_ss;
    assert(sss && sss->breakpoints);

    size_t count = 0;

    VMBreakPointRef* ref;

    if (!is_dummy)
    {
        for (ref = sss->breakpoints->get_reference();
             ref; ref = sss->breakpoints->get_next_reference(ref))
        {
            assert(ref->bp->addr);
            count++;
        }
    }

    st_pending_ss* pp =
        (st_pending_ss*)STD_MALLOC(sizeof(st_pending_ss) + (count-1)*sizeof(void*));

    pp->count = count;
    pp->m = m;
    pp->next = sss->pplist;
    sss->pplist = pp;

    if (is_dummy)
        return;

    count = 0;
    for (ref = sss->breakpoints->get_reference();
         ref; ref = sss->breakpoints->get_next_reference(ref))
    {
        pp->addrs[count++] = ref->bp->addr;
    }
}

static void set_pp_breakpoints(Method* m)
{
    VM_thread* vm_thread = p_TLS_vmthread;
    assert(vm_thread);

    NCAISingleStepState* sss = vm_thread->jvmti_thread.ncai_ss;
    assert(sss && sss->breakpoints);

    TRACE2("ncai.step.pp", "Re-enabling postponed SS breakpoints exitting method = " <<
            m << ", is_dummy = " << ((sss->pplist->count == 0) ? "true" : "false"));

    if (!sss->pplist) // SS was enabled later than MethodEntry has occured
        return;

    assert(sss->pplist->m == m); // There should be exactly the same record

    st_pending_ss* ptr = sss->pplist;

    if (sss->pplist->count != 0) // Is not a dummy record
    {
        // Check that we've returned to non-JNI code from JNI method
        for (VMBreakPointRef* ref = sss->breakpoints->get_reference();
             ref; ref = sss->breakpoints->get_next_reference(ref))
        {
            assert(ref->bp->addr);
            ncaiModuleKind type = ncai_get_target_address_type(ref->bp->addr);
            assert(type != NCAI_MODULE_JNI_LIBRARY);
        }

        sss->breakpoints->remove_all_reference();
    }

    for (size_t i = 0; i < ptr->count; i++)
    { // Setting up postponed breakpoints
        sss->breakpoints->add_reference(ptr->addrs[i], 0);
    }

    sss->pplist = ptr->next;
    STD_FREE(ptr);
}

static void clear_pp_breakpoints(NCAISingleStepState* sss)
{
    assert(sss);

    if (!sss->pplist) // There are no postponed breakpoints
        return;

    while (sss->pplist)
    {
        st_pending_ss* ptr = sss->pplist;
        sss->pplist = ptr->next;
        STD_FREE(ptr);
    }
}


void ncai_step_native_method_entry(Method* m)
{
    TRACE2("ncai.step", "Setting up SS breakpoint to "
        << "metod entry for method: " << (void*)m);

    assert(m->is_native());

    VM_thread* vm_thread = p_TLS_vmthread;

    if (!vm_thread)
        return;

    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

    ncaiStepMode step_mode = ncai_get_thread_ss_mode(jvmti_thread);
    if (step_mode != NCAI_STEP_INTO && step_mode != NCAI_STEP_OFF)
        return;

    VMBreakPoints* vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;

    vm_brpt->lock();
    NCAISingleStepState* sss = jvmti_thread->ncai_ss;

    if (!sss || !sss->breakpoints)
    {
        vm_brpt->unlock();
        return;
    }

    Class_Handle klass = method_get_class((Method_Handle)m);
    ClassLoader* loader = klass->get_class_loader();
    GenericFunctionPointer func = loader->LookupNative((Method_Handle)m);
    assert(func);

    ncaiModuleKind type = ncai_get_target_address_type((void*)func);

    if (type == NCAI_MODULE_VM_INTERNAL)
    {
        TRACE2("ncai.step", "Skipping, method is VM_INTERNAL");

        add_pp_breakpoints(m, true);
        vm_brpt->unlock();
        return;
    }

    add_pp_breakpoints(m, false);

    TRACE2("ncai.step", "Method entry address: " << (void*)func);
    sss->breakpoints->remove_all_reference();
    sss->breakpoints->add_reference((void*)func, 0);
    vm_brpt->unlock();
}

void ncai_step_native_method_exit(Method* m)
{
    assert(m->is_native());

    VM_thread* vm_thread = p_TLS_vmthread;

    if (!vm_thread)
        return;

    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

    ncaiStepMode step_mode = ncai_get_thread_ss_mode(jvmti_thread);
    if (step_mode != NCAI_STEP_INTO && step_mode != NCAI_STEP_OFF)
        return;

    VMBreakPoints* vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;

    vm_brpt->lock();
    NCAISingleStepState* sss = jvmti_thread->ncai_ss;

    if (!sss || !sss->breakpoints)
    {
        vm_brpt->unlock();
        return;
    }

    set_pp_breakpoints(m);
    vm_brpt->unlock();
}
