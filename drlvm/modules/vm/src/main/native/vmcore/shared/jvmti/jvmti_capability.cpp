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
/*
 * JVMTI capability API
 */

#include "port_mutex.h"
#include "jvmti_direct.h"
#include "jvmti_utils.h"
#include "jvmti_tags.h"
#include "cxxlog.h"
#include "suspend_checker.h"
#include "environment.h"
#include "interpreter_exports.h"

static const jvmtiCapabilities jvmti_supported_interpreter_capabilities =
{
    1, // can_tag_objects
    1, // can_generate_field_modification_events
    1, // can_generate_field_access_events
    1, // can_get_bytecodes
    1, // can_get_synthetic_attribute
    1, // can_get_owned_monitor_info
    1, // can_get_current_contended_monitor
    1, // can_get_monitor_info
    1, // can_pop_frame
    0, // can_redefine_classes
    1, // can_signal_thread
    1, // can_get_source_file_name
    1, // can_get_line_numbers
    1, // can_get_source_debug_extension
    1, // can_access_local_variables
    0, // can_maintain_original_method_order
    1, // can_generate_single_step_events
    1, // can_generate_exception_events
    1, // can_generate_frame_pop_events
    1, // can_generate_breakpoint_events
    1, // can_suspend
    0, // can_redefine_any_class
    1, // can_get_current_thread_cpu_time
    1, // can_get_thread_cpu_time
    1, // can_generate_method_entry_events
    1, // can_generate_method_exit_events
    1, // can_generate_all_class_hook_events
    1, // can_generate_compiled_method_load_events
    1, // can_generate_monitor_events
    1, // can_generate_vm_object_alloc_events
    1, // can_generate_native_method_bind_events
    1, // can_generate_garbage_collection_events
    1  // can_generate_object_free_events
};

#if (defined _EM64T_) || (defined _IPF_)

static const jvmtiCapabilities jvmti_supported_jit_capabilities =
{
    1, // can_tag_objects
    1, // can_generate_field_modification_events
    1, // can_generate_field_access_events
    1, // can_get_bytecodes
    1, // can_get_synthetic_attribute
    1, // can_get_owned_monitor_info
    1, // can_get_current_contended_monitor
    1, // can_get_monitor_info
    1, // can_pop_frame
    0, // can_redefine_classes
    1, // can_signal_thread
    1, // can_get_source_file_name
    1, // can_get_line_numbers
    1, // can_get_source_debug_extension
    1, // can_access_local_variables
    0, // can_maintain_original_method_order
    1, // can_generate_single_step_events
    1, // can_generate_exception_events
    1, // can_generate_frame_pop_events
    1, // can_generate_breakpoint_events
    1, // can_suspend
    0, // can_redefine_any_class
    1, // can_get_current_thread_cpu_time
    1, // can_get_thread_cpu_time
    1, // can_generate_method_entry_events
    1, // can_generate_method_exit_events
    1, // can_generate_all_class_hook_events
    1, // can_generate_compiled_method_load_events
    1, // can_generate_monitor_events
    1, // can_generate_vm_object_alloc_events
    1, // can_generate_native_method_bind_events
    1, // can_generate_garbage_collection_events
    1  // can_generate_object_free_events
};

#else

static const jvmtiCapabilities jvmti_supported_jit_capabilities =
{
    1, // can_tag_objects
    1, // can_generate_field_modification_events
    1, // can_generate_field_access_events
    1, // can_get_bytecodes
    1, // can_get_synthetic_attribute
    1, // can_get_owned_monitor_info
    1, // can_get_current_contended_monitor
    1, // can_get_monitor_info
    1, // can_pop_frame
    0, // can_redefine_classes
    1, // can_signal_thread
    1, // can_get_source_file_name
    1, // can_get_line_numbers
    1, // can_get_source_debug_extension
    1, // can_access_local_variables
    0, // can_maintain_original_method_order
    1, // can_generate_single_step_events
    1, // can_generate_exception_events
    1, // can_generate_frame_pop_events
    1, // can_generate_breakpoint_events
    1, // can_suspend
    0, // can_redefine_any_class
    1, // can_get_current_thread_cpu_time
    1, // can_get_thread_cpu_time
    1, // can_generate_method_entry_events
    1, // can_generate_method_exit_events
    1, // can_generate_all_class_hook_events
    1, // can_generate_compiled_method_load_events
    1, // can_generate_monitor_events
    1, // can_generate_vm_object_alloc_events
    1, // can_generate_native_method_bind_events
    1, // can_generate_garbage_collection_events
    1  // can_generate_object_free_events
};

#endif

// 1 means that corresponding capability can be enabled
// on JVMTI_PHASE_LIVE
static const jvmtiCapabilities jvmti_enable_on_live_flags =
{
    0, // can_tag_objects
    0, // can_generate_field_modification_events
    0, // can_generate_field_access_events
    1, // can_get_bytecodes
    1, // can_get_synthetic_attribute
    1, // can_get_owned_monitor_info
    1, // can_get_current_contended_monitor
    1, // can_get_monitor_info
    1, // can_pop_frame
    0, // can_redefine_classes
    1, // can_signal_thread
    1, // can_get_source_file_name
    1, // can_get_line_numbers
    1, // can_get_source_debug_extension
    0, // can_access_local_variables
    0, // can_maintain_original_method_order
    1, // can_generate_single_step_events
    0, // can_generate_exception_events
    0, // can_generate_frame_pop_events
    0, // can_generate_breakpoint_events
    1, // can_suspend
    0, // can_redefine_any_class
    1, // can_get_current_thread_cpu_time
    1, // can_get_thread_cpu_time
    0, // can_generate_method_entry_events
    0, // can_generate_method_exit_events
    1, // can_generate_all_class_hook_events
    1, // can_generate_compiled_method_load_events
    0, // can_generate_monitor_events
    1, // can_generate_vm_object_alloc_events
    1, // can_generate_native_method_bind_events
    1, // can_generate_garbage_collection_events
    1  // can_generate_object_free_events
};

// Implementation for jvmtiGetPotentialCapabilities()
void static get_available_caps(jvmtiEnv* env,
                               jvmtiPhase phase,
                               jvmtiCapabilities* capabilities_ptr)
{
    TIEnv *ti_env = reinterpret_cast<TIEnv *>(env);

    if (JVMTI_PHASE_ONLOAD == phase)
        *capabilities_ptr = interpreter_enabled() ?
            jvmti_supported_interpreter_capabilities : jvmti_supported_jit_capabilities;
    else
    {
        // Add all capabilities from supported on live phase to already posessed capabilities
        unsigned char* puchar_ptr = (unsigned char*)capabilities_ptr;
        unsigned char* enable_ptr = (unsigned char*)&jvmti_enable_on_live_flags;

        *capabilities_ptr = ti_env->posessed_capabilities;

        for (int i = 0; i < int(sizeof(jvmtiCapabilities)); i++)
            puchar_ptr[i] |= enable_ptr[i]; 
    }

    DebugUtilsTI* ti = VM_Global_State::loader_env->TI;

    // can_tag_objects capability can only be possessed by one environment.
    // The feature should be globally enabled in OnLoad phase. If not, it
    // can't be possesed in live phase.
    // Thus can_tag_objects is available IF
    // ( we're in the OnLoad phase OR the feature is already enabled ) AND
    // no other environment possesses this capability.
    if ( (JVMTI_PHASE_ONLOAD == phase || ManagedObject::_tag_pointer) && 
        (!ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_TAG_OBJECTS) ||
            ti_env->posessed_capabilities.can_tag_objects) )
        capabilities_ptr->can_tag_objects = 1;
    else
        capabilities_ptr->can_tag_objects = 0;
}

/*
 * Get Potential Capabilities
 *
 * Returns via capabilities_ptr the JVMTI features that can
 * potentially be possessed by this environment at this time.
 *
 * @note REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetPotentialCapabilities(jvmtiEnv* env,
                              jvmtiCapabilities* capabilities_ptr)
{
    TRACE2("jvmti.capability", "GetPotentialCapabilities called");
    SuspendEnabledChecker sec;

    // Check given env & current phase.
    jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL == capabilities_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    jvmtiPhase phase;
    jvmtiError errorCode = jvmtiGetPhase(env, &phase);

    if (errorCode != JVMTI_ERROR_NONE)
        return errorCode;

    get_available_caps(env, phase, capabilities_ptr);

    return JVMTI_ERROR_NONE;
}

/*
 * Add Capabilities
 *
 * Set new capabilities by adding the capabilities pointed to by
 * capabilities_ptr. All previous capabilities are retained.
 * Typically this function is used in the OnLoad function.
 *
 * @note REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiAddCapabilities(jvmtiEnv* env,
                     const jvmtiCapabilities* capabilities_ptr)
{
    TRACE2("jvmti.capability", "AddCapabilities called");
    SuspendEnabledChecker sec;

    // Check given env & current phase.
    jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL == capabilities_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    jvmtiPhase phase;
    jvmtiError errorCode = jvmtiGetPhase(env, &phase);

    if (errorCode != JVMTI_ERROR_NONE)
        return errorCode;

    // make a copy of already possessed caps
    TIEnv *ti_env = reinterpret_cast<TIEnv *>(env);
    jvmtiCapabilities possessed_caps = ti_env->posessed_capabilities;
    
    jvmtiCapabilities available_caps;
    get_available_caps(env, phase, &available_caps);

    unsigned char* p_requested = (unsigned char*) capabilities_ptr;
    unsigned char* p_available = (unsigned char*) &available_caps;
    unsigned char* p_possessed = (unsigned char*) &possessed_caps;

    DebugUtilsTI *ti = ti_env->vm->vm_env->TI;

    // Allow to turn on any capabilities that are listed in potential capabilities
    for (int i = 0; i < int(sizeof(jvmtiCapabilities)); i++)
    {
        unsigned char adding_new = p_requested[i] & ~p_possessed[i];

        if (adding_new & ~p_available[i])
            return JVMTI_ERROR_NOT_AVAILABLE;

        p_possessed[i] |= adding_new;
    }

    // Add new capabilities after checking was done
    ti_env->posessed_capabilities = possessed_caps;

    // Update global capabilities
    if (capabilities_ptr->can_generate_method_entry_events)
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_METHOD_ENTRY);

    if (capabilities_ptr->can_generate_method_exit_events)
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_METHOD_EXIT);

    if (capabilities_ptr->can_generate_frame_pop_events)
    {
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_METHOD_EXIT);
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_FRAME_POP_NOTIFICATION);
    }

    if (capabilities_ptr->can_generate_single_step_events)
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_SINGLE_STEP);

    if (capabilities_ptr->can_generate_exception_events)
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_EXCEPTION_EVENT);

    if (capabilities_ptr->can_generate_field_access_events)
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_FIELD_ACCESS_EVENT);

    if (capabilities_ptr->can_generate_field_modification_events)
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_FIELD_MODIFICATION_EVENT);

    if (capabilities_ptr->can_pop_frame)
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_POP_FRAME);

    if (capabilities_ptr->can_generate_monitor_events ||
        capabilities_ptr->can_get_owned_monitor_info)
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_MONITOR_EVENTS);

    if (capabilities_ptr->can_tag_objects) {
        ti->set_global_capability(DebugUtilsTI::TI_GC_ENABLE_TAG_OBJECTS);

        // this flag could be set only once and mustn't be reset by
        // RelinquishCapabilities.
        ManagedObject::_tag_pointer = true;
    }

    return JVMTI_ERROR_NONE;
} // jvmtiAddCapabilities

/*
 * Relinquish Capabilities
 *
 * Remove the capabilities pointed to by capabilities_ptr.
 * Some implementations may allow only one environment to have
 * capability (see the capability introduction).
 *
 * @note REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiRelinquishCapabilities(jvmtiEnv* env,
                            const jvmtiCapabilities* capabilities_ptr)
{
    TRACE2("jvmti.capability", "RelinquishCapabilities called");
    SuspendEnabledChecker sec;

    // Check given env & current phase.
    jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL == capabilities_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    TIEnv *ti_env = reinterpret_cast<TIEnv *>(env);
    unsigned char* p_posessed = (unsigned char*)&ti_env->posessed_capabilities;
    unsigned char* puchar_ptr = (unsigned char*)capabilities_ptr;

    jvmtiCapabilities removed_caps;
    unsigned char* removed_ptr = (unsigned char*)&removed_caps;

    // Remove all bits set in capabilities_ptr
    // FIXME: disable corresponding parts of VM according to removed capabilities
    for (int i = 0; i < int(sizeof(jvmtiCapabilities)); i++)
    {
        removed_ptr[i] = (p_posessed[i] & puchar_ptr[i]);
        p_posessed[i] &= ~removed_ptr[i];
    }

    DebugUtilsTI* ti = ti_env->vm->vm_env->TI;
    ti->TIenvs_lock._lock();
    ti_env = ti->getEnvironments();

    while (NULL != ti_env)
    {
        TIEnv* next_env = ti_env->next;
        unsigned char* p_posessed = (unsigned char*)&ti_env->posessed_capabilities;

        // clear 'removed_caps' capabilities that posessed in any environment
        for (int i = 0; i < int(sizeof(jvmtiCapabilities)); i++)
            removed_ptr[i] &= ~p_posessed[i];

        ti_env = next_env;
    }

    ti->TIenvs_lock._unlock();
    
    // Now removed_ptr contains capabilities removed from all environments
    if (removed_caps.can_generate_method_entry_events)
        ti->reset_global_capability(DebugUtilsTI::TI_GC_ENABLE_METHOD_ENTRY);

    if (removed_caps.can_generate_method_exit_events)
    {
        ti->reset_global_capability(DebugUtilsTI::TI_GC_ENABLE_METHOD_EXIT);
        ti->reset_global_capability(DebugUtilsTI::TI_GC_ENABLE_FRAME_POP_NOTIFICATION);
    }

    if (removed_caps.can_generate_frame_pop_events)
        ti->reset_global_capability(DebugUtilsTI::TI_GC_ENABLE_FRAME_POP_NOTIFICATION);

    if (removed_caps.can_generate_single_step_events)
        ti->reset_global_capability(DebugUtilsTI::TI_GC_ENABLE_SINGLE_STEP);

    if (removed_caps.can_generate_exception_events)
        ti->reset_global_capability(DebugUtilsTI::TI_GC_ENABLE_EXCEPTION_EVENT);

    if (removed_caps.can_generate_field_access_events)
        ti->reset_global_capability(DebugUtilsTI::TI_GC_ENABLE_FIELD_ACCESS_EVENT);

    if (removed_caps.can_generate_field_modification_events)
        ti->reset_global_capability(DebugUtilsTI::TI_GC_ENABLE_FIELD_MODIFICATION_EVENT);

    if (removed_caps.can_pop_frame)
        ti->reset_global_capability(DebugUtilsTI::TI_GC_ENABLE_POP_FRAME);

    if (removed_caps.can_tag_objects) {
        // clear tags on relinquishing can_tag_objects capability
        ti_env = reinterpret_cast<TIEnv *>(env);
        port_mutex_lock(&ti_env->environment_data_lock);
        if (ti_env->tags) {
            ti_env->tags->clear();
            delete ti_env->tags;
            ti_env->tags = NULL;
        }
        port_mutex_unlock(&ti_env->environment_data_lock);

        ti->reset_global_capability(DebugUtilsTI::TI_GC_ENABLE_TAG_OBJECTS);
    }

    // relinquishing following capabilities will not revert VM operation mode
    // back to optimized, so we do not reset global capabilities
    //
    //     TI_GC_ENABLE_MONITOR_EVENTS

    return JVMTI_ERROR_NONE;
} // jvmtiRelinquishCapabilities

/*
 * Get Capabilities
 *
 * Returns via capabilities_ptr the optional JVMTI features which
 * this environment currently possesses. An environment does not
 * possess a capability unless it has been successfully added with
 * AddCapabilities. An environment only loses possession of a
 * capability if it has been relinquished with
 * RelinquishCapabilities. Thus, this function returns the net
 * result of the AddCapabilities and RelinquishCapabilities calls
 * which have been made.
 *
 * @note REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetCapabilities(jvmtiEnv* env,
                     jvmtiCapabilities* capabilities_ptr)
{
    TRACE2("jvmti.capability", "GetCapabilities called");
    SuspendEnabledChecker sec;

    // Can be called from any phase
    // Check only given env.
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    if (NULL == capabilities_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    TIEnv *ti_env = reinterpret_cast<TIEnv *>(env);

    *capabilities_ptr = ti_env->posessed_capabilities;

    return JVMTI_ERROR_NONE;
} // jvmtiGetCapabilities

