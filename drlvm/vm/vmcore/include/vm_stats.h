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
#ifndef _VM_STATS_H_
#define _VM_STATS_H_

#ifdef VM_STATS

#include <apr_hash.h>
#include <apr_pools.h>
#include <apr_time.h>

#include "open/types.h"
#include "simplehashtable.h"
#include "lock_manager.h"

typedef struct String_Stat {
    static unsigned int num_embiguity;
    unsigned int num_lookup;
    unsigned int num_lookup_collision;
    POINTER_SIZE_INT raw_hash;
    bool is_interned;    
} String_Stat;

class VM_Statistics
{
public:
    uint64 num_exceptions;
    uint64 num_exceptions_caught_same_frame;
    uint64 num_exceptions_object_not_created;
    uint64 num_exceptions_dead_object;
    uint64 num_array_index_throw;
    uint64 num_native_methods;
    uint64 num_java_methods;
    uint64 num_fill_in_stack_trace;
    uint64 num_unwind_java_frames_gc;
    uint64 num_unwind_native_frames_gc;
    uint64 num_unwind_java_frames_non_gc;
    uint64 num_unwind_native_frames_all;
    uint64 max_stack_trace;
    uint64 total_stack_trace_depth;

    uint64 num_type_checks;
    uint64 num_type_checks_equal_type;
    uint64 num_type_checks_fast_decision;
    uint64 num_type_checks_super_is_array;
    uint64 num_type_checks_super_is_interface;
    uint64 num_type_checks_super_is_too_deep;

    uint64 num_instanceof;
    uint64 num_instanceof_equal_type;
    uint64 num_instanceof_fast_decision;
    uint64 num_instanceof_null;

    uint64 num_checkcast;
    uint64 num_checkcast_equal_type;
    uint64 num_checkcast_fast_decision;
    uint64 num_checkcast_null;

    uint64 num_aastore;
    uint64 num_aastore_equal_type;
    uint64 num_aastore_fast_decision;
    uint64 num_aastore_null;
    uint64 num_aastore_object_array;

    uint64 num_aastore_test;
    uint64 num_aastore_test_equal_type;
    uint64 num_aastore_test_fast_decision;
    uint64 num_aastore_test_null;
    uint64 num_aastore_test_object_array;

    uint64 num_optimistic_depth_success;
    uint64 num_optimistic_depth_failure;
    uint64 num_method_lookup_cache_hit;
    uint64 num_method_lookup_cache_miss;

    uint64 num_invokeinterface_calls;
    uint64 num_invokeinterface_calls_size_1;
    uint64 num_invokeinterface_calls_searched_1;
    uint64 num_invokeinterface_calls_size_2;
    uint64 num_invokeinterface_calls_searched_2;
    uint64 num_invokeinterface_calls_size_many;
    uint64 num_invokeinterface_calls_searched_many;
    uint64 invokeinterface_calls_size_max;
    uint64 invokeinterface_calls_searched_max;

    uint64 num_instantiate_cp_string_fast;
    uint64 num_instantiate_cp_string_fast_returned_interned;
    uint64 num_instantiate_cp_string_fast_success_long_path;
    uint64 num_instantiate_cp_string_slow;

    uint64 num_class_alloc_new_object_or_null;
    uint64 num_class_alloc_new_object;

    uint64 num_anewarray;
    uint64 num_multianewarray;
    uint64 num_newarray;
    uint64 num_newarray_or_null;
    uint64 num_newarray_char;
    uint64 num_newarray_float;
    uint64 num_newarray_double;
    uint64 num_newarray_boolean;
    uint64 num_newarray_byte;
    uint64 num_newarray_short;
    uint64 num_newarray_int;
    uint64 num_newarray_long;

    uint64 num_is_class_initialized;

    uint64 num_get_addr_of_vm_last_java_frame;

    uint64 num_f2i;
    uint64 num_f2l;
    uint64 num_d2i;
    uint64 num_d2l;

    uint64 num_arraycopy_byte;
    uint64 num_arraycopy_char;
    uint64 num_arraycopy_bool;
    uint64 num_arraycopy_short;
    uint64 num_arraycopy_int;
    uint64 num_arraycopy_long;
    uint64 num_arraycopy_float;
    uint64 num_arraycopy_double;
    uint64 num_arraycopy_object;
    uint64 num_arraycopy_object_same_type;
    uint64 num_arraycopy_object_different_type;

    uint64 num_char_arraycopies;
    uint64 num_same_array_char_arraycopies;
    uint64 num_zero_src_offset_char_arraycopies;
    uint64 num_zero_dst_offset_char_arraycopies;
    uint64 num_aligned_char_arraycopies;
    uint64 total_char_arraycopy_length;
    uint64 char_arraycopy_count[32];               // distribution of arraycopy lengths
    uint64 num_fast_char_arraycopies;
    uint64 total_fast_char_arraycopy_uint64_copies;
    uint64 char_arraycopy_uint64_copies[32];       // distribution of number of fast arraycopy uint64 copies

    uint64 num_lazy_monitor_enter;
    uint64 num_lazy_monitor_exit;

    uint64 num_monitor_enter;
    uint64 num_monitor_enter_null_check;
    uint64 num_monitor_enter_is_null;
    uint64 num_monitor_enter_fastcall;

    uint64 num_monitor_exit;
    uint64 num_monitor_exit_null_check;
    uint64 num_monitor_exit_is_null;
    uint64 num_monitor_exit_unowned_object;
    uint64 num_monitor_exit_fastestcall;
    uint64 num_monitor_exit_fastcall;
    uint64 num_monitor_exit_decr_rec_count;
    uint64 num_monitor_exit_very_slow_path;

    uint64 num_monitor_enter_wait;
    uint64 num_sleep_monitor_enter;
    uint64 num_sleep_monitor_exit;
    uint64 num_sleep_notify_all;
    uint64 num_sleep_notify;
    uint64 num_sleep_interrupt_the_wait;
    uint64 num_sleep_wait;
    uint64 num_sleep_java_thread_yield;
    uint64 num_wait_WaitForSingleObject;
    uint64 num_sleep_hashcode;
    uint64 num_sleep_monitor_ownership;

    uint64 num_monitor_enters_with_zero_headers;
    uint64 num_monitor_enters_with_nonzero_headers;

    uint64 num_local_jni_handles;

    uint64 num_free_local_called;
    uint64 num_free_local_called_free;
    uint64 num_jni_handles_freed;
    uint64 num_jni_handles_wasted_refs;
    uint64 jni_stub_bytes;

    uint64 num_thread_enable_suspend;
    uint64 num_thread_disable_suspend;

    uint64 num_convert_null_m2u;
    uint64 num_convert_null_u2m;

    uint64 lockres_enter;
    uint64 lockres_exit;
    uint64 lockres_enter_nonnull;
    uint64 lockres_exit_nonnull;
    uint64 lockres_enter_static;
    uint64 lockres_exit_static;
    uint64 lockres_enter_C;
    uint64 lockres_exit_C;
    uint64 lockres_fastest_enter; // already reserved by me but unlocked
    uint64 lockres_fastest_exit; // reserved by me and locked once
    uint64 lockres_enter_anon_reserved; // anonymously reserved upon acquire
    uint64 lockres_unreserves; // the lock had to be unreserved
    uint64 lockres_rollbacks; // the reserving thread was in an unsafe region
    uint64 lockres_slow_reserved_enter; // the lock was already reserved by me, but no fast path
    uint64 lockres_slow_reserved_exit; // the lock was already reserved by me, but no fast path
    uint64 lockres_unreserved_enter; // the lock was unreserved
    uint64 lockres_unreserved_exit; // the lock was unreserved

    uint64 codemgr_total_code_pool_size;
    uint64 codemgr_total_code_allocated;
    uint64 codemgr_total_data_pool_size;
    uint64 codemgr_total_data_allocated;

    uint64 num_compileme_generated;
    uint64 num_compileme_used;

    // Total number of allocations and total number of bytes for class-related
    // data structures. This includes any rounding added to make each item
    // aligned (current alignment is to the next 16 byte boundary).
    uint64 num_statics_allocations;
    uint64 num_nonempty_statics_allocations;
    uint64 num_vtable_allocations;
    uint64 num_hot_statics_allocations;
    uint64 num_hot_vtable_allocations;

    uint64 total_statics_bytes;
    uint64 total_vtable_bytes;
    uint64 total_hot_statics_bytes;
    uint64 total_hot_vtable_bytes;

    SimpleHashtable rt_function_requests;
    SimpleHashtable rt_function_calls;

    Lock_Manager vm_stats_lock;
    apr_pool_t * vm_stats_pool;

    // JIT and stub pools statistics
    uint64 number_memoryblock_allocations;
    uint64 total_memory_allocated;
    uint64 total_memory_used;
    uint64 number_memorymanager_created;

    ~VM_Statistics();

    static VM_Statistics & get_vm_stats();

    void print();

private:
    // get_vm_stats should be used to get instance
    VM_Statistics();

    void print_rt_function_stats();
    void print_string_pool_stats();

}; //VM_Statistics

extern bool vm_print_total_stats;
extern int vm_print_total_stats_level;

#define vm_stats_inc(stat_var) stat_var++

#endif //VM_STATS
#endif //_VM_STATS_H_
