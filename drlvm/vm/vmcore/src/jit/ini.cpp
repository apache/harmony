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
#define LOG_DOMAIN "vm.core"
#include "cxxlog.h"
#include <assert.h>
#include "ini.h"
#include "environment.h"
#include "exceptions.h"
#include "open/em_vm.h"
#include "jvmti_break_intf.h"
#include "stack_iterator.h"

static bool is_in_jni_method()
{
    StackIterator *si = si_create_from_native();
    bool in_jni = si_is_native(si) && NULL != si_get_method(si);

    si_free(si);
    return in_jni;
}

void
vm_execute_java_method_array(jmethodID method, jvalue *result, jvalue *args) {
    // TODO: select jit which compiled the method
    assert(!hythread_is_suspend_enabled());
    assert(!exn_raised());
    //FIXME integration
    //DEBUG_PUSH_LOCK(JAVA_CODE_PSEUDO_LOCK);
    assert(NULL != VM_Global_State::loader_env);
    assert(NULL != VM_Global_State::loader_env->em_interface);
    assert(NULL != VM_Global_State::loader_env->em_interface->ExecuteMethod);

    // Start single stepping a new Java method
    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;
    if(ti->isEnabled() && ti->is_single_step_enabled()) {
        // Start single stepping a new Java method
        jvmti_thread_t jvmti_thread = jthread_self_jvmti();
        assert(jvmti_thread);
        jvmti_set_single_step_breakpoints_for_method(ti, jvmti_thread, (Method*)method);
    }

    VM_Global_State::loader_env->em_interface->ExecuteMethod(method, result, args);
    //DEBUG_POP_LOCK(JAVA_CODE_PSEUDO_LOCK);

    // Return from native code. It is necessary to set up a breakpoint
    // in the method which called us
    if (ti->isEnabled() && ti->is_single_step_enabled())
    {
        VM_thread *vm_thread = p_TLS_vmthread;
        jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;
        LMAutoUnlock lock(ti->vm_brpt->get_lock());
        if (NULL != jvmti_thread->ss_state)
        {
            // Start single stepping a new Java method
            jvmti_remove_single_step_breakpoints(ti, jvmti_thread);

            jvmti_StepLocation *method_return;
            unsigned locations_number;
            jvmtiError errorCode = jvmti_get_next_bytecodes_from_native(
                vm_thread, &method_return, &locations_number, is_in_jni_method());
            assert (JVMTI_ERROR_NONE == errorCode);

            jvmti_set_single_step_breakpoints(ti, jvmti_thread, method_return, locations_number);
        }
    }
}
