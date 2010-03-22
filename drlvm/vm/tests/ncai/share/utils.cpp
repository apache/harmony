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
 * @author Valentin Al. Sitnick
 *
 */

#include "utils.h"

typedef struct
{
    jvmtiError  code;
    const char* text;

} jvmti_error_text;

static jvmti_error_text jvmti_errors[] =
{
    {JVMTI_ERROR_NONE, "JVMTI_ERROR_NONE"},
    {JVMTI_ERROR_NULL_POINTER, "JVMTI_ERROR_NULL_POINTER"},
    {JVMTI_ERROR_OUT_OF_MEMORY, "JVMTI_ERROR_OUT_OF_MEMORY"},
    {JVMTI_ERROR_ACCESS_DENIED, "JVMTI_ERROR_ACCESS_DENIED"},
    {JVMTI_ERROR_UNATTACHED_THREAD, "JVMTI_ERROR_UNATTACHED_THREAD"},
    {JVMTI_ERROR_INVALID_ENVIRONMENT, "JVMTI_ERROR_INVALID_ENVIRONMENT"},
    {JVMTI_ERROR_WRONG_PHASE, "JVMTI_ERROR_WRONG_PHASE"},
    {JVMTI_ERROR_INTERNAL, "JVMTI_ERROR_INTERNAL"},
    {JVMTI_ERROR_NOT_AVAILABLE, "JVMTI_ERROR_NOT_AVAILABLE"},
    {JVMTI_ERROR_MUST_POSSESS_CAPABILITY, "JVMTI_ERROR_MUST_POSSESS_CAPABILITY"},
};

const char* get_jvmti_eror_text(jvmtiError code)
{
    for (unsigned int i = 0; i < sizeof(jvmti_errors) / sizeof(jvmti_errors[0]); i++)
    {
        if (jvmti_errors[i].code == code)
            return jvmti_errors[i].text;
    }

    return "Unknown jvmti error";
}

/* *********************************************************************** */

/*
 * Function for outputting names of current states of thread as a string.
 */
void thread_state_output_as_string(long state)
{
    bool pipe = false;
    if (state & JVMTI_THREAD_STATE_ALIVE )
    {
        fprintf(stderr, " _ALIVE ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_TERMINATED )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _TERMINATED ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_RUNNABLE )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _RUNNABLE ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _BLOCKED_ON_MONITOR_ENTER ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_WAITING )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _WAITING ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_WAITING_INDEFINITELY )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _WAITING_INDEFINITELY ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _WAITING_WITH_TIMEOUT ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_SLEEPING )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _SLEEPING ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_IN_OBJECT_WAIT )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _IN_OBJECT_WAIT ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_PARKED )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _PARKED ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_SUSPENDED )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _SUSPENDED ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_INTERRUPTED )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _INTERRUPTED ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_IN_NATIVE )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _IN_NATIVE ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_VENDOR_1 )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _VENDOR_1 ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_VENDOR_2 )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _VENDOR_2 ");
        pipe = true;
    }

    if (state & JVMTI_THREAD_STATE_VENDOR_3 )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _VENDOR_3 ");
        pipe = true;
    }
    return;
}

/*
 * Function for outputting names of current statuses of class as a string.
 */
void class_status_output_as_string(long status)
{
    bool pipe = false;
    if ( status & JVMTI_CLASS_STATUS_VERIFIED )
    {
        fprintf(stderr, " _VERIFIED ");
        pipe = true;
    }

    if ( status & JVMTI_CLASS_STATUS_PREPARED )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _PREPARED ");
        pipe = true;
    }

    if ( status & JVMTI_CLASS_STATUS_INITIALIZED )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _INITIALIZED ");
        pipe = true;
    }

    if ( status & JVMTI_CLASS_STATUS_ERROR )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _ERROR ");
        pipe = true;
    }

    if ( status & JVMTI_CLASS_STATUS_ARRAY )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _ARRAY ");
        pipe = true;
    }

    if ( status & JVMTI_CLASS_STATUS_PRIMITIVE )
    {
        if (pipe) fprintf(stderr, "|");
        fprintf(stderr, " _PRIMITIVE ");
        pipe = true;
    }

    return;
}

/*
 * Function for comparing of 2 capabilities structure and output result
 * of this operation.
 */
int cmp_caps_and_output_results(jvmtiCapabilities c1,
        jvmtiCapabilities c2, const char* name1, const char* name2)
{
    int diff = 0;

    if (c1.can_tag_objects
            != c2.can_tag_objects)
    {
        fprintf(stderr, "\t\tDIFF: can_tag_objects %d in %s  and %d in %s\n",
                c1.can_tag_objects, name1, c2.can_tag_objects, name2);
        diff++;
    }
    if (c1.can_generate_field_modification_events
            != c2.can_generate_field_modification_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_field_modification_events %d in %s  and %d in %s\n",
                c1.can_generate_field_modification_events, name1, c2.can_generate_field_modification_events, name2);
        diff++;
    }
    if (c1.can_generate_field_access_events
            != c2.can_generate_field_access_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_field_access_events %d in %s  and %d in %s\n",
                c1.can_generate_field_access_events, name1, c2.can_generate_field_access_events, name2);
        diff++;
    }
    if (c1.can_get_bytecodes
            != c2.can_get_bytecodes)
    {
        fprintf(stderr, "\t\tDIFF: can_get_bytecodes %d in %s  and %d in %s\n",
                c1.can_get_bytecodes, name1, c2.can_get_bytecodes, name2);
        diff++;
    }
    if (c1.can_get_synthetic_attribute
            != c2.can_get_synthetic_attribute)
    {
        fprintf(stderr, "\t\tDIFF: can_get_synthetic_attribute in %d in %s  and %d in %s\n",
                c1.can_get_synthetic_attribute, name1, c2.can_get_synthetic_attribute, name2);
        diff++;
    }
    if (c1.can_get_owned_monitor_info
            != c2.can_get_owned_monitor_info)
    {
        fprintf(stderr, "\t\tDIFF: can_get_owned_monitor_info in %d in %s  and %d in %s\n",
                c1.can_get_owned_monitor_info, name1, c2.can_get_owned_monitor_info, name2);
        diff++;
    }
    if (c1.can_get_current_contended_monitor
            != c2.can_get_current_contended_monitor)
    {
        fprintf(stderr, "\t\tDIFF: can_get_current_contended_monitor %d in %s  and %d in %s\n",
                c1.can_get_current_contended_monitor, name1, c2.can_get_current_contended_monitor, name2);
        diff++;
    }
    if (c1.can_get_monitor_info
            != c2.can_get_monitor_info)
    {
        fprintf(stderr, "\t\tDIFF: can_get_monitor_info %d in %s  and %d in %s\n",
                c1.can_get_monitor_info, name1, c2.can_get_monitor_info, name2);
        diff++;
    }
    if (c1.can_pop_frame
            != c2.can_pop_frame)
    {
        fprintf(stderr, "\t\tDIFF: can_pop_frame %d in %s  and %d in %s\n",
                c1.can_pop_frame, name1, c2.can_pop_frame, name2);
        diff++;
    }
    if (c1.can_redefine_classes
            != c2.can_redefine_classes)
    {
        fprintf(stderr, "\t\tDIFF: can_redefine_classes %d in %s  and %d in %s\n",
                c1.can_redefine_classes, name1, c2.can_redefine_classes, name2);
        diff++;
    }
    if (c1.can_signal_thread
            != c2.can_signal_thread)
    {
        fprintf(stderr, "\t\tDIFF: can_signal_thread %d in %s  and %d in %s\n",
                c1.can_signal_thread, name1, c2.can_signal_thread, name2);
        diff++;
    }
    if (c1.can_get_source_file_name
            != c2.can_get_source_file_name)
    {
        fprintf(stderr, "\t\tDIFF: can_get_source_file_name %d in %s  and %d in %s\n",
                c1.can_get_source_file_name, name1, c2.can_get_source_file_name, name2);
        diff++;
    }
    if (c1.can_get_line_numbers
            != c2.can_get_line_numbers)
    {
        fprintf(stderr, "\t\tDIFF: can_get_line_numbers %d in %s  and %d in %s\n",
                c1.can_get_line_numbers, name1, c2.can_get_line_numbers, name2);
        diff++;
    }
    if (c1.can_get_source_debug_extension
            != c2.can_get_source_debug_extension)
    {
        fprintf(stderr, "\t\tDIFF: can_get_source_debug_extension %d in %s  and %d in %s\n",
                c1.can_get_source_debug_extension, name1, c2.can_get_source_debug_extension, name2);
        diff++;
    }
    if (c1.can_access_local_variables
            != c2.can_access_local_variables)
    {
        fprintf(stderr, "\t\tDIFF: can_access_local_variables %d in %s  and %d in %s\n",
                c1.can_access_local_variables, name1, c2.can_access_local_variables, name2);
        diff++;
    }
    if (c1.can_maintain_original_method_order
            != c2.can_maintain_original_method_order)
    {
        fprintf(stderr, "\t\tDIFF: can_maintain_original_method_order %d in %s  and %d in %s\n",
                c1.can_maintain_original_method_order, name1, c2.can_maintain_original_method_order, name2);
        diff++;
    }
    if (c1.can_generate_single_step_events
            != c2.can_generate_single_step_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_single_step_events %d in %s  and %d in %s\n",
                c1.can_generate_single_step_events, name1, c2.can_generate_single_step_events, name2);
        diff++;
    }
    if (c1.can_generate_exception_events
            != c2.can_generate_exception_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_exception_events %d in %s  and %d in %s\n",
                c1.can_generate_exception_events, name1, c2.can_generate_exception_events, name2);
        diff++;
    }
    if (c1.can_generate_frame_pop_events
            != c2.can_generate_frame_pop_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_frame_pop_events %d in %s  and %d in %s\n",
                c1.can_generate_frame_pop_events, name1, c2.can_generate_frame_pop_events, name2);
        diff++;
    }
    if (c1.can_generate_breakpoint_events
            != c2.can_generate_breakpoint_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_breakpoint_events %d in %s  and %d in %s\n",
                c1.can_generate_breakpoint_events, name1, c2.can_generate_breakpoint_events, name2);
        diff++;
    }
    if (c1.can_suspend
            != c2.can_suspend)
    {
        fprintf(stderr, "\t\tDIFF: can_suspend %d in %s  and %d in %s\n",
                c1.can_suspend, name1, c2.can_suspend, name2);
        diff++;
    }
    if (c1.can_redefine_any_class
            != c2.can_redefine_any_class)
    {
        fprintf(stderr, "\t\tDIFF: can_redefine_any_class %d in %s  and %d in %s\n",
                c1.can_redefine_any_class, name1, c2.can_redefine_any_class, name2);
        diff++;
    }
    if (c1.can_get_current_thread_cpu_time
            != c2.can_get_current_thread_cpu_time)
    {
        fprintf(stderr, "\t\tDIFF: can_tag_objects %d in %s  and %d in %s\n",
                c1.can_tag_objects, name1, c2.can_tag_objects, name2);
        diff++;
    }
    if (c1.can_get_thread_cpu_time
            != c2.can_get_thread_cpu_time)
    {
        fprintf(stderr, "\t\tDIFF: can_tag_objects %d in %s  and %d in %s\n",
                c1.can_tag_objects, name1, c2.can_tag_objects, name2);
        diff++;
    }
    if (c1.can_generate_method_entry_events
            != c2.can_generate_method_entry_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_method_entry_events %d in %s  and %d in %s\n",
                c1.can_generate_method_entry_events, name1, c2.can_generate_method_entry_events, name2);
        diff++;
    }
    if (c1.can_generate_method_exit_events
            != c2.can_generate_method_exit_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_method_exit_events %d in %s  and %d in %s\n",
                c1.can_generate_method_exit_events, name1, c2.can_generate_method_exit_events, name2);
        diff++;
    }
    if (c1.can_generate_all_class_hook_events
            != c2.can_generate_all_class_hook_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_all_class_hook_events %d in %s  and %d in %s\n",
                c1.can_generate_all_class_hook_events, name1, c2.can_generate_all_class_hook_events, name2);
        diff++;
    }
    if (c1.can_generate_compiled_method_load_events
            != c2.can_generate_compiled_method_load_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_compiled_method_load_events %d in %s  and %d in %s\n",
                c1.can_generate_compiled_method_load_events, name1, c2.can_generate_compiled_method_load_events, name2);
        diff++;
    }
    if (c1.can_generate_monitor_events
            != c2.can_generate_monitor_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_monitor_events %d in %s  and %d in %s\n",
                c1.can_generate_monitor_events, name1, c2.can_generate_monitor_events, name2);
        diff++;
    }
    if (c1.can_generate_vm_object_alloc_events
            != c2.can_generate_vm_object_alloc_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_vm_object_alloc_events %d in %s  and %d in %s\n",
                c1.can_generate_vm_object_alloc_events, name1, c2.can_generate_vm_object_alloc_events, name2);
        diff++;
    }
    if (c1.can_generate_native_method_bind_events
            != c2.can_generate_native_method_bind_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_native_method_bind_events %d in %s  and %d in %s\n",
                c1.can_generate_native_method_bind_events, name1, c2.can_generate_native_method_bind_events, name2);
        diff++;
    }
    if (c1.can_generate_garbage_collection_events
            != c2.can_generate_garbage_collection_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_garbage_collection_events %d in %s  and %d in %s\n",
                c1.can_generate_garbage_collection_events, name1, c2.can_generate_garbage_collection_events, name2);
        diff++;
    }
    if (c1.can_generate_object_free_events
            != c2.can_generate_object_free_events)
    {
        fprintf(stderr, "\t\tDIFF: can_generate_object_free_events %d in %s  and %d in %s\n",
                c1.can_generate_object_free_events, name1, c2.can_generate_object_free_events, name2);
        diff++;
    }

    if (0 != diff)
    {
        fprintf(stderr, "\t\tDIFF: capabilities %s and %s has %d differences\n", name1, name2, diff);
    }
    else
    {
        fprintf(stderr, "\t\tDIFF: capabilities %s and %s are identical\n", name1, name2);
    }

    return diff;
}

void print_capabilities(jvmtiCapabilities c, const char* name)
{
    fprintf(stderr, "\t\t%s.can_tag_objects = %d\n",
            name, c.can_tag_objects);
    fprintf(stderr, "\t\t%s.can_generate_field_modification_events = %d\n",
            name, c.can_generate_field_modification_events);

    fprintf(stderr, "\t\t%s.can_generate_field_access_events = %d\n",
            name, c.can_generate_field_access_events);

    fprintf(stderr, "\t\t%s.can_get_bytecodes = %d\n",
            name, c.can_get_bytecodes);

    fprintf(stderr, "\t\t%s.can_get_synthetic_attribute = %d\n",
            name, c.can_get_synthetic_attribute);

    fprintf(stderr, "\t\t%s.can_get_owned_monitor_info = %d\n",
            name, c.can_get_owned_monitor_info);

    fprintf(stderr, "\t\t%s.can_get_current_contended_monitor = %d\n",
            name, c.can_get_current_contended_monitor);

    fprintf(stderr, "\t\t%s.can_get_monitor_info = %d\n",
            name, c.can_get_monitor_info);

    fprintf(stderr, "\t\t%s.can_pop_frame = %d\n",
            name, c.can_pop_frame);

    fprintf(stderr, "\t\t%s.can_redefine_classes = %d\n",
            name, c.can_redefine_classes);

    fprintf(stderr, "\t\t%s.can_signal_thread = %d\n",
            name, c.can_signal_thread);

    fprintf(stderr, "\t\t%s.can_get_source_file_name = %d\n",
            name, c.can_get_source_file_name);

    fprintf(stderr, "\t\t%s.can_get_line_numbers = %d\n",
            name, c.can_get_line_numbers);

    fprintf(stderr, "\t\t%s.can_get_source_debug_extension = %d\n",
            name, c.can_get_source_debug_extension);

    fprintf(stderr, "\t\t%s.can_access_local_variables = %d\n",
            name, c.can_access_local_variables);

    fprintf(stderr, "\t\t%s.can_maintain_original_method_order = %d\n",
            name, c.can_maintain_original_method_order);

    fprintf(stderr, "\t\t%s.can_generate_single_step_events = %d\n",
            name, c.can_generate_single_step_events);

    fprintf(stderr, "\t\t%s.can_generate_exception_events = %d\n",
            name, c.can_generate_exception_events);

    fprintf(stderr, "\t\t%s.can_generate_frame_pop_events = %d\n",
            name, c.can_generate_frame_pop_events);

    fprintf(stderr, "\t\t%s.can_generate_breakpoint_events = %d\n",
            name, c.can_generate_breakpoint_events);

    fprintf(stderr, "\t\t%s.can_suspend = %d\n",
            name, c.can_suspend);

    fprintf(stderr, "\t\t%s.can_redefine_any_class = %d\n",
            name, c.can_redefine_any_class);

    fprintf(stderr, "\t\t%s.can_tag_objects = %d\n",
            name, c.can_tag_objects);

    fprintf(stderr, "\t\t%s.can_tag_objects = %d\n",
            name, c.can_tag_objects);

    fprintf(stderr, "\t\t%s.can_generate_method_entry_events = %d\n",
            name, c.can_generate_method_entry_events);

    fprintf(stderr, "\t\t%s.can_generate_method_exit_events = %d\n",
            name, c.can_generate_method_exit_events);

    fprintf(stderr, "\t\t%s.can_generate_all_class_hook_events = %d\n",
            name, c.can_generate_all_class_hook_events);

    fprintf(stderr, "\t\t%s.can_generate_compiled_method_load_events = %d\n",
            name, c.can_generate_compiled_method_load_events);

    fprintf(stderr, "\t\t%s.can_generate_monitor_events = %d\n",
            name, c.can_generate_monitor_events);

    fprintf(stderr, "\t\t%s.can_generate_vm_object_alloc_events = %d\n",
            name, c.can_generate_vm_object_alloc_events);

    fprintf(stderr, "\t\t%s.can_generate_native_method_bind_events = %d\n",
            name, c.can_generate_native_method_bind_events);

    fprintf(stderr, "\t\t%s.can_generate_garbage_collection_events = %d\n",
            name, c.can_generate_garbage_collection_events);

    fprintf(stderr, "\t\t%s.can_generate_object_free_events = %d\n",
            name, c.can_generate_object_free_events);
}


/*
 * Function for making ZERO capabilities
 */
void make_caps_zero(jvmtiCapabilities* c1)
{
    memset(c1, 0, sizeof(jvmtiCapabilities));

    return;
}

/*
 * Function for making SET capabilities
 */
void make_caps_set_all(jvmtiCapabilities* c1)
{
    memset(c1, 0xff, sizeof(jvmtiCapabilities));

    return;
}

/*
 * Check Phase
 *     1. special phase parameter is
 *         0 - any phases (no check)
 *         1 - ONLOAD only
 *         2 - ONLOAD & LIVE
 *         3 - START & LIVE
 *         4 - LIVE only
 */

bool check_phase_debug(jvmtiEnv* jvmti_env, int phase_param, bool is_debug)
{
    jvmtiPhase phase;
    jvmtiError result = jvmti_env->GetPhase(&phase);

    if (is_debug)
    {
        fprintf(stderr, "\tnative: GetPhase result = %d (must be zero) \n", result);
        fprintf(stderr, "\tnative: current phase is %d (must be 4 (LIVE-phase)) \n", phase);
    }

    if (result != JVMTI_ERROR_NONE) return false;

    if (phase_param)
    {
        if (((phase_param == 1) && (phase == JVMTI_PHASE_ONLOAD)) ||
            ((phase_param == 2) && (phase == JVMTI_PHASE_ONLOAD) && (phase == JVMTI_PHASE_LIVE)) ||
            ((phase_param == 3) && (phase == JVMTI_PHASE_START) && (phase == JVMTI_PHASE_LIVE)) ||
            ((phase_param == 4) && (phase == JVMTI_PHASE_LIVE))) return true;
        else
            return false;
    }

    return true;
}

bool check_phase_and_method_debug(jvmtiEnv* jvmti_env, jmethodID method, int phase_param,
        const char* exp_name, bool is_debug)
{
    char* name;
    char* signature;
    char* generic;

    if (!check_phase_debug(jvmti_env, phase_param, is_debug))
        return false;

    jvmtiError result = jvmti_env->GetMethodName(method, &name, &signature, &generic);

    if (is_debug)
    {
        fprintf(stderr, "\tnative: GetMethodName result = %d (must be zero) \n", result);
        fprintf(stderr, "\tnative: method name is %s \n", name);
        fprintf(stderr, "\tnative: signature name is %s \n", signature);
        fprintf(stderr, "\tnative: generic name is %s \n", generic);
        fflush(stderr);
    }

    if (result != JVMTI_ERROR_NONE) return false;

    if (strcmp(name, exp_name))
        return false;
    else
        return true;
}

bool check_phase_and_thread_debug(jvmtiEnv* jvmti_env, jthread thread, int phase_param,
        const char* exp_name, bool is_debug)
{
    jvmtiThreadInfo tinfo;

    if (!check_phase_debug(jvmti_env, phase_param, is_debug))
        return false;

    jvmtiError result = jvmti_env->GetThreadInfo(thread, &tinfo);
    if (is_debug)
    {
        fprintf(stderr, "\tnative: GetThreadInfo result = %d (must be zero) \n", result);
        fprintf(stderr, "\tnative: current thread name is %s (must be zero) \n", tinfo.name);
    }

    if (strcmp(tinfo.name, exp_name) || (result != JVMTI_ERROR_NONE))
        return false;
    else
        return true;
}
/*
jthread create_not_alive_thread(JNIEnv* jni_env, jvmtiEnv* jvmti_env)
{
    jclass clazz;
    jmethodID mid;
    jthread thread;

    fprintf(stderr, "\tnative: JNI: funcs start\n");

    clazz = jni_env->FindClass("java/lang/Thread");
    if (!clazz) return NULL;
    fprintf(stderr, "\tnative: JNI: FindClass - Ok\n");

    mid = jni_env->GetMethodID(clazz, "<init>", "()V");
    if (!mid) return NULL;
    fprintf(stderr, "\tnative: JNI: GetMethodID - Ok\n");

    thread = jni_env->NewObject(clazz, mid, "native_agent_thread");
    if (!thread) return NULL;
    fprintf(stderr, "\tnative: JNI: NewObject - Ok\n");

    return thread;
}
*/
bool is_needed_field_found(jvmtiEnv* jvmti_env, const char* filename, const char* fieldname,
        jclass* myclass, jfieldID* myfield, bool is_debug)
{

    jint class_count;
    jclass* classes;
    jclass my_class = NULL;
    char* source_name;
    jint field_count;
    jfieldID* fields;
    jfieldID my_field = NULL;

    jvmtiError result = jvmti_env->GetLoadedClasses(&class_count, &classes);
    fprintf(stderr, "\tnative: GetLoadedClasses result = %d (must be zero) \n", result);
    fprintf(stderr, "\tnative: class_count is %d \n", class_count);
    fprintf(stderr, "\tnative: classes is %p \n", classes);
    fflush(stderr);

    if ( result != JVMTI_ERROR_NONE ) return false;

    for (int i = 0; i < class_count; i++)
    {
        result = jvmti_env->GetSourceFileName(classes[i], &source_name);
        if (is_debug)
        {
            fprintf(stderr, "\tnative: GetSourceFileName result = %d (must be zero) \n", result);
            fprintf(stderr, "\tnative: source_name is %s \n", source_name);
            fprintf(stderr, "\tnative: classes is %p \n", classes);
            fflush(stderr);
        }

        if (result != JVMTI_ERROR_NONE) continue;

        if (!strcmp(source_name, filename))
        {
            fprintf(stderr, "\tnative: GetSourceFileName result = %d (must be zero) \n", result);
            fprintf(stderr, "\tnative: source_name is %s \n", source_name);
            fprintf(stderr, "\tnative: classes is %p \n", classes);
            fflush(stderr);

            my_class = classes[i];
            break;
        }
    }

    result = jvmti_env->GetClassFields(my_class, &field_count, &fields);
    fprintf(stderr, "\tnative: GetClassFields result = %d (must be zero) \n", result);
    fprintf(stderr, "\tnative: fields_ptr is %p \n", fields);
    fprintf(stderr, "\tnative: field_count_ptr is %d \n", field_count);
    fprintf(stderr, "\tnative: my_class is %p \n", my_class);
    fflush(stderr);

    if ( result != JVMTI_ERROR_NONE ) return false;

    for (int j = 0; j < field_count; j++)
    {
        char* name;
        char* signature;
        char* generic;

        result = jvmti_env->GetFieldName(my_class, fields[j], &name, &signature, &generic);
        if (is_debug)
        {
            fprintf(stderr, "\tnative: GetFieldName result = %d (must be zero) \n", result);
            fprintf(stderr, "\tnative: classes is %p \n", my_class);
            fprintf(stderr, "\tnative: fields_ptr[%d] is %p \n", j, fields[j]);
            fprintf(stderr, "\tnative: field name is %s \n", name);
            fprintf(stderr, "\tnative: field signature is %s \n", signature);
            fprintf(stderr, "\tnative: field generic is %s \n", generic);
            fflush(stderr);
        }
        if ( result != JVMTI_ERROR_NONE ) continue;

        if (!strcmp(name, fieldname))
        {
            fprintf(stderr, "\tnative: GetFieldName result = %d (must be zero) \n", result);
            fprintf(stderr, "\tnative: classes is %p \n", my_class);
            fprintf(stderr, "\tnative: fields_ptr[%d] is %p \n", j, fields[j]);
            fprintf(stderr, "\tnative: field name is %s \n", name);
            fprintf(stderr, "\tnative: field signature is %s \n", signature);
            fprintf(stderr, "\tnative: field generic is %s \n", generic);
            fflush(stderr);

            my_field = fields[j];
            break;
        }
    }

    if (!my_class || !my_field) return false;

    *myclass = my_class;
    *myfield = my_field;

    return true;
}

jint func_for_Agent_OnLoad_JVMTI(JavaVM *vm, char *options, void *reserved, Callbacks* callbacks,
        jvmtiEvent* events, jint size, const char* test_case_name, bool is_debug, jvmtiEnv** jvmti)
{
    jvmtiEnv* jvmti_env;
    jvmtiError result;
    jvmtiCapabilities pc;
    jvmtiEventCallbacks cb = {NULL};

    //warning fix
    int w_fix = sizeof(options);
    w_fix += sizeof(reserved);
    w_fix += sizeof (is_debug);
    //

    /* Getting of jvmti environment */
    if (vm->GetEnv((void**)&jvmti_env, JVMTI_VERSION_1_0) != JNI_OK)
    {
        fprintf(stderr, "Failure of initialization of JVMTI environment");
        return JNI_ERR;
    }

    *jvmti = jvmti_env;

    result = jvmti_env->GetPotentialCapabilities(&pc);
    if (result != JVMTI_ERROR_NONE)
    {
        fprintf(stderr, "\n\tERROR during the getting of potential capabilities");
        fprintf(stderr, "\n\tERROR: test interupted");
        fprintf(stderr, "\n\tnative: GetPotentialCapabilities error_code is %d",
                result);
        return JNI_ERR;
    }

    bool mentry = false, mexit = false, fpopnot = false;

    for (jint mee_ind = 0; mee_ind < size; mee_ind++)
    {
        if (events[mee_ind] == JVMTI_EVENT_METHOD_ENTRY)
            mentry = true;
        if (events[mee_ind] == JVMTI_EVENT_METHOD_EXIT)
            mexit = true;
        if (events[mee_ind] == JVMTI_EVENT_FRAME_POP)
            fpopnot = true;
    }

    if (!mentry)
        pc.can_generate_method_entry_events = 0;
    if (!mexit)
        pc.can_generate_method_exit_events = 0;
    if (!fpopnot)
        pc.can_generate_frame_pop_events = 0;

    result = jvmti_env->AddCapabilities(&pc);
    if (result != JVMTI_ERROR_NONE)
    {
        fprintf(stderr, "\n\tERROR during the adding of all possible capabilities");
        fprintf(stderr, "\n\tERROR: test interupted");
        fprintf(stderr, "\n\tnative: AddCapabilities error_code is %d",
                result);
        return JNI_ERR;
    }

    memset(&cb, 0, sizeof(cb));

    cb.SingleStep              = callbacks->cbSingleStep;
    cb.Breakpoint              = callbacks->cbBreakpoint;
    cb.FieldAccess             = callbacks->cbFieldAccess;
    cb.FieldModification       = callbacks->cbFieldModification;
    cb.FramePop                = callbacks->cbFramePop;
    cb.MethodEntry             = callbacks->cbMethodEntry;
    cb.MethodExit              = callbacks->cbMethodExit;
    cb.NativeMethodBind        = callbacks->cbNativeMethodBind;
    cb.Exception               = callbacks->cbException;
    cb.ExceptionCatch          = callbacks->cbExceptionCatch;
    cb.ThreadStart             = callbacks->cbThreadStart;
    cb.ThreadEnd               = callbacks->cbThreadEnd;
    cb.ClassLoad               = callbacks->cbClassLoad;
    cb.ClassPrepare            = callbacks->cbClassPrepare;
    cb.ClassFileLoadHook       = callbacks->cbClassFileLoadHook;
    cb.VMStart                 = callbacks->cbVMStart;
    cb.VMInit                  = callbacks->cbVMInit;
    cb.VMDeath                 = callbacks->cbVMDeath;
    cb.CompiledMethodLoad      = callbacks->cbCompiledMethodLoad;
    cb.CompiledMethodUnload    = callbacks->cbCompiledMethodUnload;
    cb.DynamicCodeGenerated    = callbacks->cbDynamicCodeGenerated;
    cb.DataDumpRequest         = callbacks->cbDataDumpRequest;
    cb.MonitorContendedEnter   = callbacks->cbMonitorContendedEnter;
    cb.MonitorContendedEntered = callbacks->cbMonitorContendedEntered;
    cb.MonitorWait             = callbacks->cbMonitorWait;
    cb.MonitorWaited           = callbacks->cbMonitorWaited;
    cb.VMObjectAlloc           = callbacks->cbVMObjectAlloc;
    cb.ObjectFree              = callbacks->cbObjectFree;
    cb.GarbageCollectionStart  = callbacks->cbGarbageCollectionStart;
    cb.GarbageCollectionFinish = callbacks->cbGarbageCollectionFinish;

    for (int j = 0; j < size; j++)
    {
        result = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, events[j], NULL);

        if (result == JVMTI_ERROR_NONE) continue;

        fprintf(stderr, "\n\tERROR during the adding of SetEventNotificationMode");
        fprintf(stderr, "\n\tERROR: test interupted");
        fprintf(stderr, "\n\tnative: SetEventNotificationMode error_code is %d",
                result);
        return JNI_ERR;
    }

    result = jvmti_env->SetEventCallbacks(&cb, sizeof(cb));

    if (result != JVMTI_ERROR_NONE)
    {
        fprintf(stderr, "\n\tERROR during the adding of SetEventCallbacks");
        fprintf(stderr, "\n\tERROR: test interupted");
        fprintf(stderr, "\n\tnative: SetEventCallbacks error_code is %d",
                result);
        return JNI_ERR;
    }

    fprintf(stderr, "\n----------------------------------------------------");
    fprintf(stderr, "\ntest");
    fprintf(stderr, " %s ", test_case_name);
    fprintf(stderr, "is started\n{\n");
    fflush(stderr);

    return JNI_OK;
}

/*
 * Universal agent function;
 */
jint func_for_Agent_OnLoad(JavaVM *vm, char *options, void *reserved, Callbacks* callbacks,
        jvmtiEvent* events, jint size, const char* test_case_name, bool is_debug)
{
    jvmtiEnv* jvmti_env;
    jvmtiError result;
    jvmtiCapabilities pc;
    jvmtiEventCallbacks cb = {NULL};

    //warning fix
    int w_fix = sizeof(options);
    w_fix += sizeof(reserved);
    w_fix += sizeof (is_debug);
    //

    /* Getting of jvmti environment */
    if (vm->GetEnv((void**)&jvmti_env, JVMTI_VERSION_1_0) != JNI_OK)
    {
        fprintf(stderr, "Failure of initialization of JVMTI environment");
        return JNI_ERR;
    }

    result = jvmti_env->GetPotentialCapabilities(&pc);
    if (result != JVMTI_ERROR_NONE)
    {
        fprintf(stderr, "\n\tERROR during the getting of potential capabilities");
        fprintf(stderr, "\n\tERROR: test interupted");
        fprintf(stderr, "\n\tnative: GetPotentialCapabilities error_code is %d",
                result);
        return JNI_ERR;
    }

    bool mentry = false, mexit = false, fpopnot = false;

    for (jint mee_ind = 0; mee_ind < size; mee_ind++)
    {
        if (events[mee_ind] == JVMTI_EVENT_METHOD_ENTRY)
            mentry = true;
        if (events[mee_ind] == JVMTI_EVENT_METHOD_EXIT)
            mexit = true;
        if (events[mee_ind] == JVMTI_EVENT_FRAME_POP)
            fpopnot = true;
    }

    if (!mentry)
        pc.can_generate_method_entry_events = 0;
    if (!mexit)
        pc.can_generate_method_exit_events = 0;
    if (!fpopnot)
        pc.can_generate_frame_pop_events = 0;

    result = jvmti_env->AddCapabilities(&pc);
    if (result != JVMTI_ERROR_NONE)
    {
        fprintf(stderr, "\n\tERROR during the adding of all possible capabilities");
        fprintf(stderr, "\n\tERROR: test interupted");
        fprintf(stderr, "\n\tnative: AddCapabilities error_code is %d",
                result);
        return JNI_ERR;
    }

    memset(&cb, 0, sizeof(cb));

    cb.SingleStep              = callbacks->cbSingleStep;
    cb.Breakpoint              = callbacks->cbBreakpoint;
    cb.FieldAccess             = callbacks->cbFieldAccess;
    cb.FieldModification       = callbacks->cbFieldModification;
    cb.FramePop                = callbacks->cbFramePop;
    cb.MethodEntry             = callbacks->cbMethodEntry;
    cb.MethodExit              = callbacks->cbMethodExit;
    cb.NativeMethodBind        = callbacks->cbNativeMethodBind;
    cb.Exception               = callbacks->cbException;
    cb.ExceptionCatch          = callbacks->cbExceptionCatch;
    cb.ThreadStart             = callbacks->cbThreadStart;
    cb.ThreadEnd               = callbacks->cbThreadEnd;
    cb.ClassLoad               = callbacks->cbClassLoad;
    cb.ClassPrepare            = callbacks->cbClassPrepare;
    cb.ClassFileLoadHook       = callbacks->cbClassFileLoadHook;
    cb.VMStart                 = callbacks->cbVMStart;
    cb.VMInit                  = callbacks->cbVMInit;
    cb.VMDeath                 = callbacks->cbVMDeath;
    cb.CompiledMethodLoad      = callbacks->cbCompiledMethodLoad;
    cb.CompiledMethodUnload    = callbacks->cbCompiledMethodUnload;
    cb.DynamicCodeGenerated    = callbacks->cbDynamicCodeGenerated;
    cb.DataDumpRequest         = callbacks->cbDataDumpRequest;
    cb.MonitorContendedEnter   = callbacks->cbMonitorContendedEnter;
    cb.MonitorContendedEntered = callbacks->cbMonitorContendedEntered;
    cb.MonitorWait             = callbacks->cbMonitorWait;
    cb.MonitorWaited           = callbacks->cbMonitorWaited;
    cb.VMObjectAlloc           = callbacks->cbVMObjectAlloc;
    cb.ObjectFree              = callbacks->cbObjectFree;
    cb.GarbageCollectionStart  = callbacks->cbGarbageCollectionStart;
    cb.GarbageCollectionFinish = callbacks->cbGarbageCollectionFinish;

    for (int j = 0; j < size; j++)
    {
        result = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, events[j], NULL);

        if (result == JVMTI_ERROR_NONE) continue;

        fprintf(stderr, "\n\tERROR during the adding of SetEventNotificationMode");
        fprintf(stderr, "\n\tERROR: test interupted");
        fprintf(stderr, "\n\tnative: SetEventNotificationMode error_code is %d",
                result);
        return JNI_ERR;
    }

    result = jvmti_env->SetEventCallbacks(&cb, sizeof(cb));

    if (result != JVMTI_ERROR_NONE)
    {
        fprintf(stderr, "\n\tERROR during the adding of SetEventCallbacks");
        fprintf(stderr, "\n\tERROR: test interupted");
        fprintf(stderr, "\n\tnative: SetEventCallbacks error_code is %d",
                result);
        return JNI_ERR;
    }

    fprintf(stderr, "\n----------------------------------------------------");
    fprintf(stderr, "\ntest");
    fprintf(stderr, " %s ", test_case_name);
    fprintf(stderr, "is started\n{\n");
    fflush(stderr);

    return JNI_OK;
}

/*
 * Universal VMDeath callback function;
 */
void func_for_callback_VMDeath(JNIEnv* jni_env, jvmtiEnv* jvmti_env,
        const char* test_case_name, bool test, bool util)
{

    //warning fix
    int w_fix = 1;
    w_fix += sizeof(jni_env);
    w_fix += sizeof(jvmti_env);
    //

    fprintf(stderr, "\n\tTest of function ");
    fprintf(stderr, "%s ", test_case_name);
    fprintf(stderr, "             : ");
    fflush(stderr);

    if (test && util)
        fprintf(stderr, " passed \n");
    else
        fprintf(stderr, " failed \n");

    fprintf(stderr, "\n} /* test ");
    fprintf(stderr, "%s ", test_case_name);
    fprintf(stderr, "is finished */ \n\n");
    fflush(stderr);

    return;
}

/* *********************************************************************** */


