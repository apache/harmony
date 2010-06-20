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
#define LOG_DOMAIN "vm.methods"
#include "cxxlog.h"

#include "environment.h"
#include <assert.h>
#include "lock_manager.h"
#include "nogc.h"
#include "vm_stats.h"
#include "cci.h"
#include "class_member.h"

#include "method_lookup.h"
#include "port_threadunsafe.h"

#define EIP_CACHE_SIZE 1024
#define EIP_ALIGNMENT     4



Method_Lookup_Table::Method_Lookup_Table()
{
    _next_free_entry = 0;
    _capacity        = 0;
    _table           = 0;
    _cache           = (Method_Code **)STD_MALLOC(EIP_CACHE_SIZE *
        sizeof(Method_Code *));
    assert (_cache);
    memset(_cache, 0, (EIP_CACHE_SIZE * sizeof(Method_Code *)));
    reallocate(511);
    port_mutex_create(&lock, APR_THREAD_MUTEX_NESTED);
} //Method_Lookup_Table::Method_Lookup_Table



Method_Lookup_Table::~Method_Lookup_Table()
{
    if (_table != NULL) {
        STD_FREE((void*)_table);
    }
    if (_cache != NULL) {
        STD_FREE((void*)_cache);
    }
    port_mutex_destroy(&lock);
} //Method_Lookup_Table::~Method_Lookup_Table



void Method_Lookup_Table::reallocate(unsigned new_capacity)
{
    Method_Code **new_table = (Method_Code **)STD_MALLOC(new_capacity *
        sizeof(Method_Code *));
    assert(new_table != NULL);
    assert(_next_free_entry <= _capacity);
    assert(_next_free_entry < new_capacity);
    memcpy(new_table, _table, (_next_free_entry * sizeof(Method_Code *)));
    if (_table != NULL) {
        STD_FREE(_table);
    }
    _table    = new_table;
    _capacity = new_capacity;
} //Method_Lookup_Table::reallocate



void Method_Lookup_Table::add(Method_Code *m)
{
    table_lock();

    void *code_block_addr = m->code_addr;

    // If the table is full, allocate more memory.
    if (_next_free_entry >= _capacity) {
        reallocate(2 * _capacity);
    }

    // Figure out the index idx where the new entry should go
    unsigned idx = find_index(code_block_addr);
    // Shift entries starting at idx one slot to the right, then insert the new entry at idx
    for (unsigned i = _next_free_entry;  i > idx;  i--) {
        _table[i] = _table[i-1];
    }
    _table[idx] = m;
    _next_free_entry++;

    table_unlock();
} //Method_Lookup_Table::add

#define USE_METHOD_LOOKUP_CACHE

Boolean Method_Lookup_Table::remove(void *addr)
{
    if (addr == NULL) {
        return FALSE;
    }

#ifdef USE_METHOD_LOOKUP_CACHE
    // First remove from cache.  
    for (unsigned i = 0; i < EIP_CACHE_SIZE; i++){
        if (_cache[i]){
            void *guess_start = _cache[i]->code_addr;
            void *guess_end   = ((char *)_cache[i]->code_addr) +
                _cache[i]->size;
            if ((addr >= guess_start) && (addr < guess_end)) {
                _cache[i] = NULL;
            }
        }
    }
#endif //USE_METHOD_LOOKUP_CACHE

    table_lock();

    unsigned L = 0, R = _next_free_entry;
    while (L < R) {
        unsigned M = (L + R) / 2;
        Method_Code *m = _table[M];
        void  *code_block_addr = m->code_addr;
        size_t code_block_size = m->size;
        void  *code_end_addr   = (void *)((char *)code_block_addr + code_block_size);

        if (addr < code_block_addr) {
            R = M;
        } else if (addr >= code_end_addr) {
            // Should this be (addr >= code_end_addr)?
            L = M + 1;
        } else {
            // Shift entries starting at idx one slot to the right, then insert the new entry at idx
            for (unsigned i = M;  i <  (_next_free_entry - 1);  i++) {
                _table[i] = _table[i+1];
            }
            _next_free_entry--;

            table_unlock();
            return TRUE;
        }
    }

    table_unlock();
    return FALSE;
} //Method_Lookup_Table::remove


void Method_Lookup_Table::append_unlocked(Method_Code *m)
{
    void  *code_block_addr = m->code_addr;
    size_t code_block_size = m->size;
    void  *code_end_addr   = (void *)((char *)code_block_addr + code_block_size);

    // If the table is full, allocate more memory.
    if (_next_free_entry >= _capacity) {
        reallocate(2 * _capacity);
    }

    if (_next_free_entry > 0) {
        unsigned last_entry = (_next_free_entry - 1);
        Method_Code *last = _table[last_entry];
        void  *last_code_addr = last->code_addr;
        size_t last_size      = last->size;
        void  *last_end_addr  = (void *)((char *)last_code_addr + last_size);
        if (code_block_addr < last_end_addr) {
            printf("Method_Lookup_Table::append_unlocked: New entry [%p..%p] is before last table entry [%p..%p]\n",
                   code_block_addr, code_end_addr, last_code_addr, last_end_addr);
            DIE(("New entry is before last table entry")); 
        }
    }

    _table[_next_free_entry] = m;
    _next_free_entry++;
} //Method_Lookup_Table::append_unlocked



unsigned Method_Lookup_Table::find_index(void *addr)
{
    unsigned L = 0, R = _next_free_entry;
    while (L < R) {
        unsigned M = (L + R) / 2;
        Method_Code *m = _table[M];
        void  *code_block_addr = m->code_addr;
        size_t code_block_size = m->size;
        void  *code_end_addr   = (void *)((char *)code_block_addr + code_block_size);
        if (addr < code_block_addr) {
            R = M;
        } else if (addr >= code_end_addr) {
            L = M + 1;
        } else {
            return M;
        }
    }
    return L;
} //Method_Lookup_Table::find_index



Method_Code *Method_Lookup_Table::find(void *addr, Boolean is_ip_past)
{
    if (addr == NULL) {
        return NULL;
    }
    if (is_ip_past) {
        addr = (U_8*)addr - 1;
    }

#ifdef USE_METHOD_LOOKUP_CACHE
    // First try the cache.  There's no need for a lock.
    unsigned cache_idx = (unsigned)(((POINTER_SIZE_INT)addr / EIP_ALIGNMENT) % EIP_CACHE_SIZE);
    Method_Code *guess = _cache[cache_idx];
    if (guess != NULL) {
        void *guess_start = guess->code_addr;
        void *guess_end   = ((char *)guess->code_addr) + guess->size;
        if ((addr >= guess_start) && (addr < guess_end)) {
#ifdef VM_STATS
            UNSAFE_REGION_START
//            VM_Statistics::get_vm_stats().num_method_lookup_cache_hit++;
            UNSAFE_REGION_END
#endif //VM_STATS
            return guess;
        }
    }
#endif //USE_METHOD_LOOKUP_CACHE
#ifdef VM_STATS
    UNSAFE_REGION_START
//    VM_Statistics::get_vm_stats().num_method_lookup_cache_miss++;
    UNSAFE_REGION_END
#endif //VM_STATS

    table_lock();

    unsigned L = 0, R = _next_free_entry;
    while (L < R) {
        unsigned M = (L + R) / 2;
        Method_Code *m = _table[M];
        void  *code_block_addr = m->code_addr;
        size_t code_block_size = m->size;
        void  *code_end_addr   = (void *)((char *)code_block_addr + code_block_size);

        if (addr < code_block_addr) {
            R = M;
        } else if (addr >= code_end_addr) {
            // Should this be (addr >= code_end_addr)?
            L = M + 1;
        } else {
#ifdef USE_METHOD_LOOKUP_CACHE
            _cache[cache_idx] = m;
#endif //USE_METHOD_LOOKUP_CACHE
            table_unlock();
            return m;
        }
    }

    table_unlock();
    return NULL;
} //Method_Lookup_Table::find



Method_Code *Method_Lookup_Table::find_deadlock_free(void *addr)
{
    bool ok = port_mutex_trylock(&lock) == TM_ERROR_NONE;             // vvv
    if (ok) {
        // We acquired the lock.  Can use the fast lookup.
        Method_Code *m = find(addr, FALSE);
        table_unlock();                 // ^^^
        return m;
    } else {
        // We failed to acquire the lock.  Use slow linear search.
        // The linear search is safe even is someone else is adding a method
        // because of the way the table is modified:
        // 1. If necessary, the table is reallocated.  This operation is
        //    atomic, so we never see a partially initialized table.
        // 2. Space is made for the new method by shifting all methods
        //    with higher addresses by 1.  The shift is done from the right,
        //    so we never lose an element in the linear search from the left.
        //    We could see the same method twice, but this is safe.
        for (unsigned i = 0;  i < _next_free_entry;  i++) {
            Method_Code *m = _table[i];
            void  *code_block_addr = m->code_addr;
            size_t code_block_size = m->size;
            void  *code_end_addr   = (void *)((char *)code_block_addr + code_block_size);
            if ((addr >= code_block_addr) && (addr <= code_end_addr)) {
                return m;
            }
        }
        return NULL;
    }
} //Method_Lookup_Table::find_deadlock_free



void Method_Lookup_Table::unload_all()
{
} //Method_Lookup_Table::unload_all

#if 0
#ifdef VM_STATS
void Method_Lookup_Table::print_stats()
{
    size_t code_block_size     = 0;
    size_t jit_info_block_size = 0;
    unsigned i;
    int num;

    if (vm_print_total_stats_level > 1) {
        num = 0;
        printf("Methods throwing exceptions:\n");
        for(i = 0;  i < _next_free_entry;  i++) {
            CodeChunkInfo *m = _table[i];
            if(m->num_throws) {
                num++;
                printf("%8" FMT64 "u :::: %s.%s%s\n",
                       m->num_throws,
                       m->get_method()->get_class()->get_name()->bytes,
                       m->get_method()->get_name()->bytes,
                       m->get_method()->get_descriptor()->bytes);
            }
        }
        printf("(A total of %d methods)\n", num);

        num = 0;
        printf("Methods catching exceptions:\n");
        for(i = 0;  i < _next_free_entry;  i++) {
            CodeChunkInfo *m = _table[i];
            code_block_size     += m->_code_block_size;
            jit_info_block_size += m->_jit_info_block_size;
            if(m->num_catches) {
                num++;
                printf("%8" FMT64 "u :::: %s.%s%s\n",
                       m->num_catches,
                       m->get_method()->get_class()->get_name()->bytes,
                       m->get_method()->get_name()->bytes,
                       m->get_method()->get_descriptor()->bytes);
            }
        }
        printf("(A total of %d methods)\n", num);
    }

    if (vm_print_total_stats_level > 2) {
        num = 0;
        printf("Methods unwinds (GC):\n");
        for(i = 0;  i < _next_free_entry;  i++) {
            CodeChunkInfo *m = _table[i];
            if(m->num_unwind_java_frames_gc) {
                num++;
                printf("%8" FMT64 "u :::: %s.%s%s\n",
                       m->num_unwind_java_frames_gc,
                       m->get_method()->get_class()->get_name()->bytes,
                       m->get_method()->get_name()->bytes,
                       m->get_method()->get_descriptor()->bytes);
            }
        }
        printf("(A total of %d methods)\n", num);

        num = 0;
        printf("Methods unwinds (non-GC):\n");
        for(i = 0;  i < _next_free_entry;  i++) {
            CodeChunkInfo *m = _table[i];
            if(m->num_unwind_java_frames_non_gc) {
                num++;
                printf("%8" FMT64 "u :::: %s.%s%s\n",
                       m->num_unwind_java_frames_non_gc,
                       m->get_method()->get_class()->get_name()->bytes,
                       m->get_method()->get_name()->bytes,
                       m->get_method()->get_descriptor()->bytes);
            }
        }
        printf("(A total of %d methods)\n", num);
    }

    printf("%11d ::::Total size of code blocks\n", (unsigned) code_block_size);
    printf("%11d ::::          JIT info blocks\n", (unsigned) jit_info_block_size);
} //Method_Lookup_Table::print_stats
#endif

CodeChunkInfo *Method_Lookup_Table::get_first_method_jit(JIT *jit)
{
    for (unsigned i = 0;  i < _next_free_entry;  i++) {
        CodeChunkInfo *m = _table[i];
        if (m && m->get_jit() == jit) {
            return m;
        }
    }
    return 0;
} //Method_Lookup_Table::get_first_method_jit



CodeChunkInfo *Method_Lookup_Table::get_next_method_jit(CodeChunkInfo *prev_info)
{
    unsigned idx = find_index(prev_info->get_code_block_addr());
    JIT *jit = prev_info->get_jit();

    for (unsigned i = idx + 1;  i < _next_free_entry;  i++) {
        CodeChunkInfo *m = _table[i];
        if(m && m->get_jit() == jit) {
            return m;
        }
    }
    return 0;
} //Method_Lookup_Table::get_next_method_jit



CodeChunkInfo *Method_Lookup_Table::get_first_code_info()
{
    for (unsigned i = 0;  i < _next_free_entry;  i++) {
        CodeChunkInfo *m = _table[i];
        if (m != NULL) {
            return m;
        }
    }
    return NULL;
} //Method_Lookup_Table::get_first_code_info



CodeChunkInfo *Method_Lookup_Table::get_next_code_info(CodeChunkInfo *prev_info)
{
    unsigned idx = find_index(prev_info->get_code_block_addr());
    for (unsigned i = idx + 1;  i < _next_free_entry;  i++) {
        CodeChunkInfo *m = _table[i];
        if (m != NULL) {
            return m;
        }
    }
    return NULL;
} //Method_Lookup_Table::get_next_code_info

#endif

Method_Code *Method_Lookup_Table::get(unsigned i) { 
    if (i < _next_free_entry) {
        return _table[i];
    } else {
        return NULL;
    }
}   // Method_Lookup_Table::get


void Method_Lookup_Table::add(Method_Handle method_handle, void *code_addr,
    size_t size, void *data)
{
    Method_Code *mc = new Method_Code(method_handle, code_addr, size, data);
    add(mc);
}

Method_Handle Method_Lookup_Table::find(void *ip, Boolean is_ip_past,
    void **code_addr, size_t *size, void **data)
{
    Method_Code *mc = find(ip, is_ip_past);

    if (NULL != mc)
    {
        if (NULL != code_addr)
            *code_addr = mc->code_addr;
        if (NULL != size)
            *size = mc->size;
        if (NULL != data)
            *data = mc->data;
        return mc->method;
    }
    else
        return NULL;
}
