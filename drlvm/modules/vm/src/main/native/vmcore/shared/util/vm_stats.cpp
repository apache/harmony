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
#ifdef VM_STATS

#define LOG_DOMAIN "vm.stats"
#include "cxxlog.h"

#include "vtable.h"
#include "environment.h"
#include "open/vm_util.h"
#include "jit_runtime_support.h"
#include "simplehashtable.h"
#include "mem_alloc.h"
#include "classloader.h"

#include "vm_stats.h"
#include "GlobalClassLoaderIterator.h"

bool vm_print_total_stats = false;
int vm_print_total_stats_level = 0;

VM_Statistics::VM_Statistics()
    : rt_function_requests(56),
      rt_function_calls(56)
{
    num_exceptions                          = 0;
    num_exceptions_caught_same_frame        = 0;
    num_exceptions_dead_object              = 0;
    num_exceptions_object_not_created       = 0;
    num_native_methods                      = 0;
    num_java_methods                        = 0;
    num_fill_in_stack_trace                 = 0;
    num_unwind_java_frames_gc               = 0;
    num_unwind_native_frames_gc             = 0;
    num_unwind_java_frames_non_gc           = 0;
    num_unwind_native_frames_all            = 0;
    max_stack_trace                         = 0;
    total_stack_trace_depth                 = 0;

    num_type_checks = 0;
    num_type_checks_equal_type = 0;
    num_type_checks_fast_decision = 0;
    num_type_checks_super_is_array = 0;
    num_type_checks_super_is_interface = 0;
    num_type_checks_super_is_too_deep = 0;

    num_instanceof = 0;
    num_instanceof_equal_type = 0;
    num_instanceof_fast_decision = 0;
    num_instanceof_null = 0;

    num_checkcast = 0;
    num_checkcast_equal_type = 0;
    num_checkcast_fast_decision = 0;
    num_checkcast_null = 0;

    num_aastore = 0;
    num_aastore_equal_type = 0;
    num_aastore_fast_decision = 0;
    num_aastore_null = 0;
    num_aastore_object_array = 0;

    num_aastore_test = 0;
    num_aastore_test_equal_type = 0;
    num_aastore_test_fast_decision = 0;
    num_aastore_test_null = 0;
    num_aastore_test_object_array = 0;

    num_optimistic_depth_success            = 0;
    num_optimistic_depth_failure            = 0;
    num_method_lookup_cache_hit             = 0;
    num_method_lookup_cache_miss            = 0;

   
    num_invokeinterface_calls               = 0;
    num_invokeinterface_calls_size_1        = 0;
    num_invokeinterface_calls_searched_1    = 0;
    num_invokeinterface_calls_size_2        = 0;
    num_invokeinterface_calls_searched_2    = 0;
    num_invokeinterface_calls_size_many     = 0;
    num_invokeinterface_calls_searched_many = 0;
    invokeinterface_calls_size_max          = 0;
    invokeinterface_calls_searched_max      = 0;
    num_instantiate_cp_string_fast          = 0;
    num_instantiate_cp_string_fast_returned_interned        = 0;
    num_instantiate_cp_string_fast_success_long_path        = 0;
    num_instantiate_cp_string_slow          = 0;
    num_class_alloc_new_object_or_null      = 0;
    num_class_alloc_new_object              = 0;
    num_newarray                            = 0;
    num_newarray_or_null                    = 0;
    num_multianewarray                      = 0;
    num_newarray_char                       = 0;
    num_newarray_float                      = 0;
    num_newarray_double                     = 0;
    num_newarray_boolean                    = 0;
    num_newarray_byte                       = 0;
    num_newarray_short                      = 0;
    num_newarray_int                        = 0;
    num_newarray_long                       = 0;
    num_array_index_throw                   = 0;
    num_is_class_initialized                = 0;
    num_get_addr_of_vm_last_java_frame     = 0;

    num_f2i                                 = 0;
    num_f2l                                 = 0;
    num_d2i                                 = 0;
    num_d2l                                 = 0;

    num_arraycopy_byte                      = 0;
    num_arraycopy_char                      = 0;
    num_arraycopy_bool                      = 0;
    num_arraycopy_short                     = 0;
    num_arraycopy_int                       = 0;
    num_arraycopy_long                      = 0;
    num_arraycopy_float                     = 0;
    num_arraycopy_double                    = 0;
    num_arraycopy_object                    = 0;
    num_arraycopy_object_same_type          = 0;
    num_arraycopy_object_different_type     = 0;

    num_char_arraycopies                    = 0;
    num_same_array_char_arraycopies         = 0;
    num_zero_src_offset_char_arraycopies    = 0;
    num_zero_dst_offset_char_arraycopies    = 0;
    num_aligned_char_arraycopies            = 0;
    total_char_arraycopy_length             = 0;
    memset(char_arraycopy_count, 0, sizeof(char_arraycopy_count));    
    num_fast_char_arraycopies               = 0;
    total_fast_char_arraycopy_uint64_copies = 0;
    memset(char_arraycopy_uint64_copies, 0, sizeof(char_arraycopy_uint64_copies));    

#ifdef _DEBUG
    num_lazy_monitor_enter                  = 0;
    num_lazy_monitor_exit                   = 0;
#endif

    num_monitor_enter                       = 0;
    num_monitor_enter_null_check            = 0;
    num_monitor_enter_is_null               = 0;
    num_monitor_enter_fastcall              = 0;

    num_monitor_exit                        = 0;
    num_monitor_exit_null_check             = 0;
    num_monitor_exit_is_null                = 0;
    num_monitor_exit_fastestcall            = 0;
    num_monitor_exit_fastcall               = 0;
    num_monitor_exit_unowned_object         = 0;
    num_monitor_exit_decr_rec_count         = 0;
    num_monitor_exit_very_slow_path         = 0;

    num_monitor_enters_with_zero_headers    = 0;
    num_monitor_enters_with_nonzero_headers = 0;

    num_monitor_enter_wait           = 0;
    num_sleep_monitor_enter          = 0;
    num_sleep_monitor_exit           = 0;
    num_sleep_notify_all             = 0;
    num_sleep_notify                 = 0;
    num_sleep_interrupt_the_wait     = 0;
    num_sleep_wait                   = 0;
    num_wait_WaitForSingleObject     = 0;
    num_sleep_hashcode               = 0;
    num_sleep_monitor_ownership      = 0; 
    num_sleep_java_thread_yield      = 0;
    num_local_jni_handles            = 0;

    num_free_local_called = 0;
    num_free_local_called_free = 0;
    num_jni_handles_freed = 0;
    num_jni_handles_wasted_refs = 0;
    jni_stub_bytes = 0;

    num_thread_enable_suspend                = 0;
    num_thread_disable_suspend               = 0;

    num_convert_null_m2u             = 0;
    num_convert_null_u2m             = 0;

    lockres_enter = 0;
    lockres_exit = 0;
    lockres_enter_nonnull = 0;
    lockres_exit_nonnull = 0;
    lockres_enter_static = 0;
    lockres_exit_static = 0;
    lockres_enter_C = 0;
    lockres_exit_C = 0;
    lockres_fastest_enter = 0;
    lockres_fastest_exit = 0;
    lockres_enter_anon_reserved = 0;
    lockres_unreserves = 0;
    lockres_rollbacks = 0;
    lockres_slow_reserved_enter = 0;
    lockres_slow_reserved_exit = 0;
    lockres_unreserved_enter = 0;
    lockres_unreserved_exit = 0;
    
    num_compileme_generated = 0;
    num_compileme_used = 0;
    number_memoryblock_allocations = 0;
    total_memory_allocated = 0;
    total_memory_used = 0;
    number_memorymanager_created = 0;

    num_statics_allocations = 0;
    num_nonempty_statics_allocations = 0;
    num_vtable_allocations = 0;
    num_hot_statics_allocations = 0;
    num_hot_vtable_allocations = 0;

    total_statics_bytes = 0;
    total_vtable_bytes = 0;
    total_hot_statics_bytes = 0;
    total_hot_vtable_bytes = 0;

    apr_pool_create(&vm_stats_pool, 0);    
} //VM_Statistics::VM_Statistics

VM_Statistics::~VM_Statistics() {
   apr_pool_destroy(vm_stats_pool);
}

VM_Statistics & VM_Statistics::get_vm_stats() {
    static VM_Statistics vm_stats = VM_Statistics();
    return vm_stats;
}

static void print_classes()
{
    if (NULL == VM_Global_State::loader_env->bootstrap_class_loader)
    {
        printf("Bootstrap class loader is NULL\n");
        return;
    }

    ClassTable* ct = VM_Global_State::loader_env->
        bootstrap_class_loader->GetLoadedClasses();
    ClassTable::iterator it;

    bool first_time = true;
    for (it = ct->begin(); it != ct->end(); it++)
    {
        Class *c = it->second;
        if(c->get_times_thrown()) {
            if(first_time) {
                first_time = false;
                printf("\nFollowing exceptions were thrown:\n");
            }
            printf("%11" FMT64 "u :::: %s\n", c->get_times_thrown(), c->get_name()->bytes);
        }
    }
    if(!first_time) {
        printf("\n");
    }

    first_time = true;
    for (it = ct->begin(); it != ct->end(); it++)
    {
        Class *c = it->second;
        if(c->get_times_init_checked() != 0) {
            if(first_time) {
                first_time = false;
                printf("Following classes were checked for init state:\n");
            }
            printf("%11" FMT64 "u :::: %s\n", c->get_times_init_checked(), c->get_name()->bytes);
        }
    }
    if(!first_time) {
        printf("\n");
    }

    first_time = true;
    for (it = ct->begin(); it != ct->end(); it++)
    {
        Class *c = it->second;
        if(c->get_times_instanceof_slow_path_taken() != 0) {
            if(first_time) {
                first_time = false;
                printf("Following classes were used in the instanceof test:\n");
            }
            printf("%11" FMT64 "u :::: %-50s : [intf=%d, depth=%2d]\n",
                c->get_times_instanceof_slow_path_taken(), c->get_name()->bytes,
                (c->is_interface() ? 1 : 0), c->get_depth());
        }
    }
    if(!first_time) {
        printf("\n");
    }

    uint64 total_bytes_allocated = 0;
    for (it = ct->begin(); it != ct->end(); it++)
    {
        Class *c = it->second;
        total_bytes_allocated += c->get_total_bytes_allocated();
    }
    first_time = true;
    for (it = ct->begin(); it != ct->end(); it++)
    {
        Class *c = it->second;
        if(c->get_times_allocated() != 0) {
            if(first_time) {
                first_time = false;
                printf("Number of instances [and # from Class.newInstance],\nand total allocated bytes for the following classes:\n");
            }
            uint64 nb = c->get_total_bytes_allocated();

#if defined (__INTEL_COMPILER) 
#pragma warning( push )
#pragma warning (disable:1683) // to get rid of remark #1683: explicit conversion of a 64-bit integral type to a smaller integral type
#endif
            unsigned percent = unsigned((nb * 100)/total_bytes_allocated);

#if defined (__INTEL_COMPILER)
#pragma warning( pop )
#endif
            if (false && nb > 1000000) {
                uint64 nk = (nb/1024);
                if (c->is_array()) {
                    printf(" %60s  %10" FMT64 "u        = %10" FMT64 "uK %3u%%\n",
                        c->get_name()->bytes, c->get_times_allocated(),
                        nk, percent);
                } else {
                    printf(" %60s  %10" FMT64 "u * %4u = %10" FMT64 "uK %3u%%\n",
                        c->get_name()->bytes, c->get_times_allocated(),
                        c->get_instance_data_size(), nk, percent);
                }
            } else {
                if (c->is_array()) {
                    printf("%10" FMT64 "u :           : %10" FMT64 "u :%-60s:%2u%%\n",
                        c->get_times_allocated(),
                        nb, c->get_name()->bytes, percent);
                } else {
                    printf("%10" FMT64 "u * %4u = : %10" FMT64 "u :%-60s:%2u%%\n",
                        c->get_times_allocated(),
                        c->get_instance_data_size(), nb, c->get_name()->bytes, percent);
                }
            }
        }
    }
    if(!first_time) {
        printf(":::                                  ----------\n");
        if (false && total_bytes_allocated > 1000000) {
            uint64 total_allocated_kbytes = (total_bytes_allocated/1024);
            printf("                                                                                           %15" FMT64 "uK\n", total_allocated_kbytes);
        } else {
            printf(":::                             %15" FMT64 "u\n", total_bytes_allocated);
        }
        printf("\n");
    }

    uint64 total_pad_bytes_allocated = 0;
    first_time = true;
    for (it = ct->begin(); it != ct->end(); it++)
    {
        Class *c = it->second;
        // Only print out those classes with padding bytes that also have instances allocated.
        if((c->get_total_padding_bytes() != 0) && (c->get_times_allocated() != 0)) {
            if(first_time) {
                first_time = false;
                if (VM_Global_State::loader_env->compact_fields) {
                    printf("Field alignment bytes and total allocated alignment bytes for the following classes:\n");
                } else {
                    printf("Field padding and alignment bytes and the total such bytes for the following classes:\n");
                }
            }
            if (c->is_array()) {
                printf("%11u :::: %s\n", (unsigned)c->get_total_padding_bytes(), c->get_name()->bytes);
            } else {
                uint64 bytes_allocated = c->get_times_allocated()*(uint64)c->get_total_padding_bytes();
                total_pad_bytes_allocated += bytes_allocated;
                printf("%11u : %11" FMT64 "u ::: %s\n", c->get_total_padding_bytes(),
                    bytes_allocated, c->get_name()->bytes);
            }
        }
    }
    if(!first_time) {
        printf(":              ----------\n");
        printf(":         %15" FMT64 "u\n", total_pad_bytes_allocated);
        printf("\n");
    }
} //print_classes



static void print_methods()
{
    printf("--------- begin native method execution counts (total and slow-path):\n");
    GlobalClassLoaderIterator clIterator;
    ClassLoader *cl;
    for(cl = clIterator.first(); cl; cl = clIterator.next()) {
        ClassTable* ct = cl->GetLoadedClasses();
        ClassTable::iterator it;
        for (it = ct->begin(); it != ct->end(); it++)
        {
            Class *c = it->second;
            int n_methods = c->get_number_of_methods();
            for(int i = 0; i < n_methods; i++) {
                Method* m = c->get_method(i);
                if (m->is_fake_method()) {
                    continue;   // ignore fake methods
                }
                if(m->num_accesses) {
                    printf("%11" FMT64 "u : %11" FMT64 "u ::: %s.%s%s\n",
                        m->num_accesses,
                        m->num_slow_accesses,
                        c->get_name()->bytes,
                        m->get_name()->bytes,
                        m->get_descriptor()->bytes);
                }
            }
        }
    }
    printf("--------- end method execution counts:\n\n");
} //print_methods



static void print_array_distribution(const char *caption, uint64 *array)
{
    bool print_distributions = false;
    int decade_high, min_size_decade, max_size_decade, i;
    int64 sum;

    for (i = 0;  i < 31;  i++) {
        if (array[i] > 0) {
            print_distributions = true;
            break;
        }
    }

    if (print_distributions) {
        printf("\n%s:\n", caption);
        printf("  Up to size :      Count : Percentage\n");
        min_size_decade = max_size_decade = 0;
        int64 total = 0;
        for (i = 0;  i < 31;  i++) {
            total = total + (int64)(array[i]);
        }
        for (i = 0;  i < 31;  i++) {
            if (array[i] > 0) {
                min_size_decade = i;
                break;
            }
        }
        for (i = 31;  i >= 0;  i--) {
            if (array[i] > 0) {
                max_size_decade = i;
                break;
            }
        }
        sum = 0;
        for (i = min_size_decade;  i <= max_size_decade;  i++) {
            double percent;
            decade_high = (1 << (i+1)) - 1;
            sum += array[i];
            if (total > 0) {
                percent = (sum * 100.0)/total;
            } else {
                percent = 0;
            }
            printf("  %10d : %10" FMT64 "u : %8d\n",
                   decade_high, array[i], (int)(percent + 0.5));
        }
        printf("\n");
    }
} // print_array_distribution



void VM_Statistics::print_rt_function_stats()
{
    int num_entries, i;

    printf("\nJIT runtime support functions requested:\n");
    num_entries = rt_function_requests.size();
    if (num_entries > 0)
    {
        PairEntry **pair_array = (PairEntry **)STD_MALLOC(num_entries * sizeof(PairEntry *));
        dump(rt_function_requests, pair_array, num_entries, /*threshold*/ 1);
        // Sort by increasing number of requests.
        quick_sort(pair_array, 0, num_entries-1);
        for (i = 0;  i < num_entries;  i++) {
            VM_RT_SUPPORT fn_number = (VM_RT_SUPPORT)((POINTER_SIZE_INT)(pair_array[i]->key));       
            const char *fn_name = vm_helper_get_name(fn_number);
            printf("%11d :::: %s\n", pair_array[i]->value, fn_name);
        }
        STD_FREE(pair_array);
        printf("\n");
    }

    num_entries = rt_function_calls.size();
    if (num_entries > 0) {
        printf("\nJIT runtime support functions called:\n");
        PairEntry **pair_array = (PairEntry **)STD_MALLOC(num_entries * sizeof(PairEntry *));
        dump(rt_function_calls, pair_array, num_entries, /*threshold*/ 1);
        // Sort by increasing number of calls.
        quick_sort(pair_array, 0, num_entries-1);
        for (i = 0;  i < num_entries;  i++) {
            VM_RT_SUPPORT fn_number = (VM_RT_SUPPORT)((POINTER_SIZE_INT)(pair_array[i]->key));       
            const char *fn_name = vm_helper_get_name(fn_number);
            printf("%11d :::: %s\n", pair_array[i]->value, fn_name);
        }
        printf("\n");
    }
} //print_rt_function_stats

void VM_Statistics::print_string_pool_stats() {
    printf("\nBegin: Virtual Machine String Pool Statistics\n");
    printf("\tname\ttotal lookups\ttotal collisions\n");
    unsigned long num_lookup_total = 0;
    unsigned long num_lookup_collision_total = 0;
    String_Pool & string_pool = VM_Global_State::loader_env->string_pool;
    for (apr_hash_index_t * index =
        apr_hash_first(vm_stats_pool, string_pool.string_stat);
        index != NULL; index = apr_hash_next(index)) {
            char * key;
            apr_ssize_t key_len;
            String_Stat * key_stats;
            
            apr_hash_this(index, (const void **)&key, &key_len, (void **)&key_stats);

            num_lookup_total += key_stats->num_lookup;
            num_lookup_collision_total += key_stats->num_lookup_collision;
            
            bool is_interned = false;
            String * string = string_pool.lookup(key, key_len);
            if (REFS_IS_COMPRESSED_MODE) {
                if (string->intern.compressed_ref) {
                    is_interned = true;
                }
            } else {
                if (string->intern.raw_ref) {
                    is_interned = true;
                }
            }

            char * str = key;
            // replace '\n' literal with space
            while(str = strchr(str, '\n')) {
                *str = ' ';
                ++str;
            }
            printf("%s#%i#%i#%s\n", key, key_stats->num_lookup,
                key_stats->num_lookup_collision, is_interned == 0 ? "uninterned" : "interned");
    }
    int num_elements = apr_hash_count(string_pool.string_stat);
    float hash_quality = ((float)(num_elements) - string_pool.num_ambiguity ) / num_elements;

    printf("Total lookups/lookup collisions/conflicting elements/hash quality:#%i#%i#%i#%f\n",
        num_lookup_total, num_lookup_collision_total, string_pool.num_ambiguity, hash_quality);
    printf("\n End: Virtual Machine String Pool Statistics\n");
}

void VM_Statistics::print()
{
    Global_Env *env = VM_Global_State::loader_env;
    if(!vm_print_total_stats)
        return;

    printf("\n==== begin VM statistics\n");

    print_classes();

    if(vm_print_total_stats_level > 2) {
        print_methods();
    }

    // Gregory -
    // Code moved to EM, no longer accessible through standard interface
    //    env->vm_methods->print_stats();

    printf("%11" FMT64 "u ::::Number of native methods\n", num_native_methods);
    printf("%11" FMT64 "u ::::Number of Java methods\n",   num_java_methods);

    printf("%11" FMT64 "u ::::Total exceptions thrown\n",       num_exceptions);
    printf("%11" FMT64 "u ::::  exc obj was dead\n",            num_exceptions_dead_object);
    printf("%11" FMT64 "u ::::  exc obj wasn't created\n",      num_exceptions_object_not_created);
    printf("%11" FMT64 "u ::::  caught in the same frame\n",    num_exceptions_caught_same_frame);
    printf("%11" FMT64 "u ::::  calls to array_index_throw\n",  num_array_index_throw);

    printf("%11" FMT64 "u ::::Number fillInStackTrace\n", num_fill_in_stack_trace);
    printf("%11" FMT64 "u ::::Max stack trace depth\n", max_stack_trace);
    if (num_fill_in_stack_trace != 0)
        printf("%11.2f ::::Avg stack trace depth\n", (double)(int64)total_stack_trace_depth / (double)(int64)num_fill_in_stack_trace);
    printf("%11" FMT64 "u ::::Unwinds (GC)  java frames\n", num_unwind_java_frames_gc);
    printf("%11" FMT64 "u ::::            native frames\n", num_unwind_native_frames_gc);
    printf("%11" FMT64 "u ::::                    total\n", num_unwind_java_frames_gc + num_unwind_native_frames_gc);
    printf("%11" FMT64 "u ::::Unwinds (non-GC)  java frames\n", num_unwind_java_frames_non_gc);
    printf("%11" FMT64 "u ::::                native frames\n", (num_unwind_native_frames_all - num_unwind_native_frames_gc));
    printf("%11" FMT64 "u ::::                        total\n", num_unwind_java_frames_non_gc + (num_unwind_native_frames_all - num_unwind_native_frames_gc));

    printf("%11" FMT64 "u :::: Optimistic depth success\n", num_optimistic_depth_success);
    printf("%11" FMT64 "u ::::                  failure\n", num_optimistic_depth_failure);
    printf("%11" FMT64 "u ::::Method lookup cache hit\n", num_method_lookup_cache_hit);
    printf("%11" FMT64 "u ::::                   miss\n", num_method_lookup_cache_miss);

    printf("%11" FMT64 "u ::::Type checks\n",                       num_type_checks);
    printf("%11" FMT64 "u ::::   Equal types\n",                    num_type_checks_equal_type);
    printf("%11" FMT64 "u ::::   Fast type check\n",                num_type_checks_fast_decision);
    printf("%11" FMT64 "u ::::   Superclass is array type\n",       num_type_checks_super_is_array);
    printf("%11" FMT64 "u ::::   Superclass is interface type\n",   num_type_checks_super_is_interface);
    printf("%11" FMT64 "u ::::   Superclass depth >%d\n",           num_type_checks_super_is_too_deep, vm_max_fast_instanceof_depth());

    printf("%11" FMT64 "u ::::Instanceof calls\n", num_instanceof);
    printf("%11" FMT64 "u ::::   Equal types\n", num_instanceof_equal_type);
    printf("%11" FMT64 "u ::::   Fast type check\n", num_instanceof_fast_decision);
    printf("%11" FMT64 "u ::::   Null object\n", num_instanceof_null);

    printf("%11" FMT64 "u ::::Checkcast calls\n", num_checkcast);
    printf("%11" FMT64 "u ::::   Equal types\n", num_checkcast_equal_type);
    printf("%11" FMT64 "u ::::   Fast type check\n", num_checkcast_fast_decision);
    printf("%11" FMT64 "u ::::   Null object\n", num_checkcast_null);

    printf("%11" FMT64 "u ::::Aastore calls\n", num_aastore);
    printf("%11" FMT64 "u ::::   Equal types\n", num_aastore_equal_type);
    printf("%11" FMT64 "u ::::   Fast type check\n", num_aastore_fast_decision);
    printf("%11" FMT64 "u ::::   Null object\n", num_aastore_null);
    printf("%11" FMT64 "u ::::   Into Object[] array\n", num_aastore_object_array);

    printf("%11" FMT64 "u ::::Aastore_test calls\n", num_aastore_test);
    printf("%11" FMT64 "u ::::   Equal types\n", num_aastore_test_equal_type);
    printf("%11" FMT64 "u ::::   Fast type check\n", num_aastore_test_fast_decision);
    printf("%11" FMT64 "u ::::   Null object\n", num_aastore_test_null);
    printf("%11" FMT64 "u ::::   Into Object[] array\n", num_aastore_test_object_array);

    printf("%11" FMT64 "u ::::Number of invokeinterface\n", num_invokeinterface_calls);
    printf("%11" FMT64 "u ::::                 max size\n", invokeinterface_calls_size_max);
    printf("%11" FMT64 "u ::::               max search\n", invokeinterface_calls_searched_max);
    printf("%11" FMT64 "u ::::               num size 1\n", num_invokeinterface_calls_size_1);
    printf("%11" FMT64 "u ::::             num search 1\n", num_invokeinterface_calls_searched_1);
    printf("%11" FMT64 "u ::::               num size 2\n", num_invokeinterface_calls_size_2);
    printf("%11" FMT64 "u ::::             num search 2\n", num_invokeinterface_calls_searched_2);
    printf("%11" FMT64 "u ::::               num size +\n", num_invokeinterface_calls_size_many);
    printf("%11" FMT64 "u ::::             num search +\n", num_invokeinterface_calls_searched_many);

    printf("%11" FMT64 "u ::::# instantiate_cp_string_fast\n", num_instantiate_cp_string_fast);
    printf("%11" FMT64 "u ::::           returned interned\n", num_instantiate_cp_string_fast_returned_interned);
    printf("%11" FMT64 "u ::::           success long path\n", num_instantiate_cp_string_fast_success_long_path);
    printf("%11" FMT64 "u ::::# instantiate_cp_string_slow\n", num_instantiate_cp_string_slow);

    printf("%11" FMT64 "u ::::# clss_alloc_new_object_or_nul\n", num_class_alloc_new_object_or_null);
    printf("%11" FMT64 "u ::::        class_alloc_new_object\n", num_class_alloc_new_object);

    printf("%11" FMT64 "u ::::Number of calls to anewarray\n", num_anewarray);
    printf("%11" FMT64 "u ::::Number calls to multianewarray\n", num_multianewarray);
    printf("%11" FMT64 "u ::::Number of calls to newarray\n", num_newarray);
    printf("%11" FMT64 "u ::::Number of calls to newarray fastpath\n", num_newarray_or_null);
    if(num_newarray_boolean)
        printf("%11" FMT64 "u ::::                    boolean\n", num_newarray_boolean);
    if(num_newarray_byte)
        printf("%11" FMT64 "u ::::                       byte\n", num_newarray_byte);
    if(num_newarray_char)
        printf("%11" FMT64 "u ::::                       char\n", num_newarray_char);
    if(num_newarray_short)
        printf("%11" FMT64 "u ::::                      short\n", num_newarray_short);
    if(num_newarray_int)
        printf("%11" FMT64 "u ::::                        int\n", num_newarray_int);
    if(num_newarray_long)
        printf("%11" FMT64 "u ::::                       long\n", num_newarray_long);
    if(num_newarray_float)
        printf("%11" FMT64 "u ::::                      float\n", num_newarray_float);
    if(num_newarray_double)
        printf("%11" FMT64 "u ::::                     double\n", num_newarray_double);

    printf("%11" FMT64 "u ::::# checks if class initialized\n", num_is_class_initialized);

    printf("%11" FMT64 "u ::::# get ljf addr\n", num_get_addr_of_vm_last_java_frame);
    printf("%11" FMT64 "u ::::# local JNI handles\n", num_local_jni_handles);

    printf("%11" FMT64 "u ::::Number of f2i\n", num_f2i);
    printf("%11" FMT64 "u ::::          f2l\n", num_f2l);
    printf("%11" FMT64 "u ::::          d2i\n", num_d2i);
    printf("%11" FMT64 "u ::::          d2l\n", num_d2l);

    printf("\n");
    printf("%11" FMT64 "u ::::Number of arraycopy(byte)\n", num_arraycopy_byte);
    printf("%11" FMT64 "u ::::Number of arraycopy(char)\n", num_arraycopy_char);
    printf("%11" FMT64 "u ::::Number of arraycopy(bool)\n", num_arraycopy_bool);
    printf("%11" FMT64 "u ::::Number of arraycopy(short)\n", num_arraycopy_short);
    printf("%11" FMT64 "u ::::Number of arraycopy(int)\n", num_arraycopy_int);
    printf("%11" FMT64 "u ::::Number of arraycopy(long)\n", num_arraycopy_long);
    printf("%11" FMT64 "u ::::Number of arraycopy(float)\n", num_arraycopy_float);
    printf("%11" FMT64 "u ::::Number of arraycopy(double)\n", num_arraycopy_double);
    printf("%11" FMT64 "u ::::Number of arraycopy(object)\n", num_arraycopy_object);
    printf("%11" FMT64 "u ::::    same element types\n", num_arraycopy_object_same_type);
    printf("%11" FMT64 "u ::::    different element types\n", num_arraycopy_object_different_type);
    printf("\n");

    printf("%11" FMT64 "u ::::Number of char arraycopies\n",     num_char_arraycopies);
    printf("%11" FMT64 "u ::::    same array copies\n",          num_same_array_char_arraycopies);
    printf("%11" FMT64 "u ::::    zero src offset copies\n",     num_zero_src_offset_char_arraycopies);
    printf("%11" FMT64 "u ::::    zero dst offset copies\n",     num_zero_dst_offset_char_arraycopies);
    printf("%11" FMT64 "u ::::    both 8 byte aligned copies\n", num_aligned_char_arraycopies);
    printf("%11" FMT64 "u ::::    fast uint64 copies\n",         num_fast_char_arraycopies);
    if (num_char_arraycopies > 0) {
        printf("%11" FMT64 "u ::::       average copy length in Chars\n", (total_char_arraycopy_length / num_char_arraycopies));
        printf("%11" FMT64 "u ::::       average copy length in bytes\n", 2*(total_char_arraycopy_length / num_char_arraycopies));
    }
    if (total_fast_char_arraycopy_uint64_copies > 0) {
        printf("%11" FMT64 "u ::::       average fast uint64 copies\n",   (total_fast_char_arraycopy_uint64_copies / num_fast_char_arraycopies));
    }
    if (num_char_arraycopies > 0) {
        print_array_distribution("Char arraycopy lengths", char_arraycopy_count);
    }
    if (total_fast_char_arraycopy_uint64_copies > 0) {
        print_array_distribution("Fast char arraycopy uint64 copies", char_arraycopy_uint64_copies);
    }
    printf("\n");

#ifdef _DEBUG
    printf("%11" FMT64 "u ::::Number of lazy monenter\n", num_lazy_monitor_enter);
    printf("%11" FMT64 "u ::::Number of lazy monexit\n", num_lazy_monitor_exit);
#endif
    // 20030114 New monitor enter/exit statistics
    printf("%11" FMT64 "u ::::Number of monenter\n", num_monitor_enter);
    printf("%11" FMT64 "u ::::          monenter (null check)\n", num_monitor_enter_null_check);
    printf("%11" FMT64 "u ::::          monenter (is null)\n", num_monitor_enter_is_null);
    printf("%11" FMT64 "u ::::          monenter (fastcall)\n", num_monitor_enter_fastcall);

    printf("\n");
    printf("%11" FMT64 "u ::::          monexit\n", num_monitor_exit);
    printf("%11" FMT64 "u ::::          monenter (null check)\n", num_monitor_exit_null_check);
    printf("%11" FMT64 "u ::::          monexit (is null)\n", num_monitor_exit_is_null);
    printf("%11" FMT64 "u ::::          monexit (unowned obj)\n", num_monitor_exit_unowned_object); 
    printf("%11" FMT64 "u ::::          monexit (fastestcall)\n", num_monitor_exit_fastestcall);
    printf("%11" FMT64 "u ::::          monexit (fastcall)\n", num_monitor_exit_fastcall);
    printf("%11" FMT64 "u ::::          monexit (decr rec ct)\n", num_monitor_exit_decr_rec_count);
    printf("%11" FMT64 "u ::::          monexit (very slow)\n", num_monitor_exit_very_slow_path);

    printf("%11" FMT64 "u ::::Number of monenter\n", num_monitor_enter);
    printf("%11" FMT64 "u ::::Number of monenter waits\n", num_monitor_enter_wait);
    printf("%11" FMT64 "u ::::Number of monenter sleeps\n", num_sleep_monitor_enter);
    printf("%11" FMT64 "u ::::Number of monexit sleeps\n", num_sleep_monitor_exit);
    printf("%11" FMT64 "u ::::Number of NotifyAll sleeps\n", num_sleep_notify_all);
    printf("%11" FMT64 "u ::::Number of Sleep sleeps\n", num_sleep_notify);
    printf("%11" FMT64 "u ::::Number of interrupt wait sleeps\n", num_sleep_interrupt_the_wait);
    printf("%11" FMT64 "u ::::Number of wait sleeps\n", num_sleep_wait);
    printf("%11" FMT64 "u ::::Number of Java yield sleeps\n", num_sleep_java_thread_yield);
    printf("%11" FMT64 "u ::::Number of wait for object\n", num_wait_WaitForSingleObject);
    printf("%11" FMT64 "u ::::Number of hashcode sleeps\n", num_sleep_hashcode);
    printf("%11" FMT64 "u ::::Number of mon owner sleeps\n", num_sleep_monitor_ownership);

    // Print total number of allocations and total number of bytes
    // for class-related data structures.
    printf("\nAllocations of storage for statics:\n");
    printf("%11d ::::number allocated\n", num_statics_allocations);
    printf("%11d ::::number nonempty allocated\n", num_nonempty_statics_allocations);
    printf("%11d ::::bytes allocated\n", total_statics_bytes);
    fflush(stdout);

    printf("\nAllocations of storage for vtables:\n");
    printf("%11d ::::number allocated\n", num_vtable_allocations);
    printf("%11d ::::bytes allocated\n", total_vtable_bytes);

    printf("\n");
    printf("%11" FMT64 "u ::::# times free_local_handle_2 was called\n", num_free_local_called);
    printf("%11" FMT64 "u ::::# times free_local_handle_2 was called and freed something\n", num_free_local_called_free);
    printf("%11" FMT64 "u ::::# jni handles freed\n", num_jni_handles_freed);
    printf("%11" FMT64 "u ::::# jni refs wasted\n", num_jni_handles_wasted_refs);
    printf("\n");
    printf("%11" FMT64 "u ::::JNI stub bytes allocated\n", jni_stub_bytes);
    printf("%11" FMT64 "u ::::# calls to thread_enable_suspend\n", num_thread_enable_suspend);
    printf("%11" FMT64 "u ::::# calls to thread_disable_suspend\n", num_thread_disable_suspend);
    printf("%11" FMT64 "u ::::# managed to unmanaged null conversions attempted\n", num_convert_null_m2u);
    printf("%11" FMT64 "u ::::# unmanaged to managed null conversions attempted\n", num_convert_null_u2m);

    printf("\n");
    printf("%11" FMT64 "u ::::lockres_enter\n", lockres_enter);
    printf("%11" FMT64 "u ::::lockres_exit\n", lockres_exit);
    printf("%11" FMT64 "u ::::lockres_enter_nonnull\n", lockres_enter_nonnull);
    printf("%11" FMT64 "u ::::lockres_exit_nonnull\n", lockres_exit_nonnull);
    printf("%11" FMT64 "u ::::lockres_enter_static\n", lockres_enter_static);
    printf("%11" FMT64 "u ::::lockres_exit_static\n", lockres_exit_static);
    printf("%11" FMT64 "u ::::lockres_enter_C\n", lockres_enter_C);
    printf("%11" FMT64 "u ::::lockres_exit_C\n", lockres_exit_C);
    printf("%11" FMT64 "u ::::lockres_fastest_enter\n", lockres_fastest_enter);
    printf("%11" FMT64 "u ::::lockres_fastest_exit\n", lockres_fastest_exit);
    printf("%11" FMT64 "u ::::lockres_enter_anon_reserved\n", lockres_enter_anon_reserved);
    printf("%11" FMT64 "u ::::lockres_unreserves\n", lockres_unreserves);
    printf("%11" FMT64 "u ::::lockres_rollbacks\n", lockres_rollbacks);
    printf("%11" FMT64 "u ::::lockres_slow_reserved_enter\n", lockres_slow_reserved_enter);
    printf("%11" FMT64 "u ::::lockres_slow_reserved_exit\n", lockres_slow_reserved_exit);
    printf("%11" FMT64 "u ::::lockres_unreserved_enter\n", lockres_unreserved_enter);
    printf("%11" FMT64 "u ::::lockres_unreserved_exit\n", lockres_unreserved_exit);

    printf("\n");
    printf("%11" FMT64 "u ::::num_compileme_generated\n", num_compileme_generated);
    printf("%11" FMT64 "u ::::num_compileme_used\n", num_compileme_used);

    printf("\n");
    printf("Use_large_pages = %s\n", (VM_Global_State::loader_env->use_large_pages? "yes" : "no"));
    printf("%11" FMT64 "u ::::number_memoryblock_allocations\n",     number_memoryblock_allocations);
    printf("%11" FMT64 "u ::::total_memory_allocated\n",     total_memory_allocated);
    printf("%11" FMT64 "u ::::total_memory_used\n",     total_memory_used);
    printf("%11" FMT64 "u ::::number_memorymanager_created\n",     number_memorymanager_created);

    print_rt_function_stats();
    print_string_pool_stats();
    printf("==== end VM statistics\n");
    fflush(stdout);
} //VM_Statistics::print

#endif //VM_STATS
