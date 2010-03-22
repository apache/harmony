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
#ifndef _STRING_POOL_H_
#define _STRING_POOL_H_

#include <string.h>
#include <apr_hash.h>

#include "tl/memory_pool.h"
#include "object_layout.h"

#define STRING_PADDING sizeof(void*)

struct String
{
#ifdef _DEBUG
    unsigned id; // id for debugging
#endif
    // 20030507 Ref to the interned string used by java.lang.String.intern().
    // It is compressed when compressing refs.
    unsigned len;
    union {
        // raw reference to interned string if not compressing references
        ManagedObject   * raw_ref;
        // equivalent compressed reference.
        U_32          compressed_ref;
    } intern;
    char bytes[STRING_PADDING];
};

class String_Pool {
public:
    String_Pool(size_t st_size);

    // lookup string in string table & insert if not found
    String * lookup(const char *str);
    String * lookup(const char *str, size_t len);

    // Iterators for GC
    String * get_first_string_intern();

    String * get_next_string_intern();

    // intern the string
    ManagedObject * intern(String *);

    // The GC enumeration code needs to lock and unlock the 
    // pool in order to enumerate it in a thread safe manner.
    void lock_pool();
    void unlock_pool();

#ifdef VM_STATS
    friend class VM_Statistics;
public:
    apr_hash_t * string_stat;
    unsigned num_ambiguity;
#endif

private:
    enum { INTERNED_STRING_ARRAY_SIZE = 32768 };
    size_t string_pool_size;

    class Entry {
    public:
        Entry   * next;
        String  str;
        Entry(const char *s, size_t len, Entry *n);
        /**
         * Memory is already allocated for this object.
         */
        void *operator new(size_t UNREF sz, void * mem) { return mem; }
        void operator delete(void * UNREF mem, void * UNREF mem1) {}
    };

    struct Interned_Strings {
        Interned_Strings    * next;
        // this field is used for synchronization purposes
        volatile unsigned   free_slot;
        String              * elem[INTERNED_STRING_ARRAY_SIZE];
    };

    struct Interned_Strings_Index {
        Interned_Strings    * current;
        unsigned            index;
    };

    // bit mask to test for end of line character
    static const POINTER_SIZE_INT BIT_MASK =
#ifdef POINTER64
        0x7efefefefefefeffL;
#else
        0x7efefeffL;
#endif

    bool has_line_end(POINTER_SIZE_INT val);
    void hash_it(const char * str, size_t* len, POINTER_SIZE_INT * hash);
    POINTER_SIZE_INT hash_it(const char * str, size_t len);
    String * lookup(const char *str, size_t len, POINTER_SIZE_INT hash);
    void register_interned_string(String * str);
    POINTER_SIZE_INT hash_it_unaligned(const char * str, size_t len);
    // memory pool
    tl::MemoryPool      memory_pool;    
    // table of string entries
    Entry               ** table;
    // linked list of arrays of interned strings
    Interned_Strings    * head_interned;
    // current list element
    volatile Interned_Strings   * current_interned;
    // iterator to go through interned strings
    // NOTE: only one thread can iterate through at once
    // Moreover the iteration should be done in stop_the_world phase since no
    // synchronization between String_Pool::intern() & String_Pool::get_next_string_intern()
    Interned_Strings_Index      * index_interned;

    // In order to make this code thread safe a light weight lock is used.
    // The protocol is simple and the lock is local to this string pool.
    volatile POINTER_SIZE_INT string_pool_lock; // 1 is locked, 0 is unlocked

};

#endif // _STRING_POOL_H_
