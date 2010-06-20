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
 * @author Pavel Pervov
 */  

#include <assert.h>
#include <apr_atomic.h>
#include <apr_pools.h>
#include <apr_hash.h>
#include <apr_time.h>

#include "open/hythread.h"
#include "open/vm_util.h"
#include "open/gc.h"

#include "platform_lowlevel.h"
#include "String_Pool.h"
#include "environment.h"
#include "port_barriers.h"
#include "vm_strings.h"
#include "vm_stats.h"
#include "ini.h"
#include "exceptions.h"
#include "port_threadunsafe.h"

#define LOG_DOMIAN "vm.strings"
#include "cxxlog.h"

// apr_atomic_casptr should result in an .acq on IPF.
void String_Pool::lock_pool () {
    // Spin until lock is m_free.
    while (apr_atomic_casptr(
        (volatile void **)&string_pool_lock, (void *)1, (void *)0) != 0) {
        hythread_yield();
    }
}

// Release lock. string_pool_lock is volatile which results in a st.rel on IPF
void String_Pool::unlock_pool () {
    assert (string_pool_lock != 0);
    string_pool_lock = 0;
} //String_Pool::unlock_pool

String_Pool::Entry::Entry(const char * s, size_t len, Entry *n) : next(n) {
// This constructor can be run very early on during VM execution--even before main is entered.
// This is before we have had a chance to process any command line arguments. So, initialize the 
// interned Java_lang_String reference to NULL in a way that will work whether references are compressed or not.
    str.intern.raw_ref = NULL;
    str.len = (unsigned)len;
    assert(strlen(s) >= len);
    memcpy(str.bytes, s, len);
    str.bytes[len] = '\0';
} //String_Pool::Entry::Entry


String_Pool::String_Pool(size_t sp_size) {
    string_pool_size = sp_size;
    size_t size = sizeof(Entry*) * sp_size;
    table = (Entry **)memory_pool.alloc(size);
    memset(table, 0, size);
    
    head_interned = (Interned_Strings *)memory_pool.alloc(sizeof(Interned_Strings));
    memset(head_interned, 0, sizeof(Interned_Strings));
    current_interned = head_interned;

    index_interned = (Interned_Strings_Index *)memory_pool.alloc(sizeof(Interned_Strings_Index));

    string_pool_lock = 0;
#ifdef VM_STATS
    string_stat = apr_hash_make(VM_Statistics::get_vm_stats().vm_stats_pool);
    num_ambiguity = 0;
#endif
} //String_Pool::String_Pool

inline bool String_Pool::has_line_end(POINTER_SIZE_INT val) {
    return (val ^ ~(val + BIT_MASK)) & ~BIT_MASK;
}

void String_Pool::hash_it(const char * s, size_t* len, POINTER_SIZE_INT * hash) {
    POINTER_SIZE_INT h1 = 0;
    POINTER_SIZE_INT h2 = 0;
    const char * p_val = s;

    // to avoid access violation exception in a while(true) cycle below we need
    // to be sure that the input string is aligned on the pointer size boundary
    if (((POINTER_SIZE_INT)s & (sizeof(POINTER_SIZE_INT) - 1)) != 0) {
        *len = strlen(s);
        *hash = hash_it_unaligned(s, *len);
        return;
    }

    while(true) {
        POINTER_SIZE_INT val = *(POINTER_SIZE_INT *)p_val;
        if (has_line_end(val)) {
            for (unsigned i = 0; i < sizeof(POINTER_SIZE_INT); i++) {
                if (p_val[i] != '\0') {
                    h2 += p_val[i];
                } else {
                    // line end found
                    *len = p_val - s + i;
                    goto done;
                }
            }
            // false signal !!!
            h2 = 0;
        }
        h1 += val;
        p_val += sizeof(POINTER_SIZE_INT);
    }
done:
    // check that length was computed correctly
    assert(strlen(s) == *len);
    *hash = h1 - h2;
}

POINTER_SIZE_INT String_Pool::hash_it(const char * s, size_t len) {

#ifdef _IPF_
    // aligned loading is critical for _IPF_
    if (((POINTER_SIZE_INT)s & (sizeof(POINTER_SIZE_INT) - 1)) != 0) {
        return hash_it_unaligned(s, len);
    }
#endif

    POINTER_SIZE_INT h1 = 0, h2 = 0;
    const unsigned parts = (unsigned)(len / sizeof(POINTER_SIZE_INT));
    
    for (unsigned i = 0; i < parts; i++) {
        h1 += *((POINTER_SIZE_INT *)s + i);
    }

    for (unsigned j = parts * sizeof(POINTER_SIZE_INT); j < len; j++) {
        h2 += s[j];
    }
    
    return h1 - h2;
}

POINTER_SIZE_INT String_Pool::hash_it_unaligned(const char * s, size_t len) {
    POINTER_SIZE_INT h1 = 0, h2 = 0;
    const size_t parts = len / sizeof(POINTER_SIZE_INT);

#ifdef _IPF_ /* 64 bit and little endian */
    POINTER_SIZE_INT val, val0, val1, val2, val3, val4, val5, val6, val7;
#endif

    // ATTENTION! we got here with unaligned s!

    for (size_t i = 0; i < parts; i++) {
#ifdef _IPF_ /* 64 bit and little endian */
        val0 = (POINTER_SIZE_INT)(unsigned char)(s[i * 8 + 0]);
        val1 = ((POINTER_SIZE_INT)(unsigned char)(s[i * 8 + 1])) << 8;
        val2 = ((POINTER_SIZE_INT)(unsigned char)(s[i * 8 + 2])) << 16;
        val3 = ((POINTER_SIZE_INT)(unsigned char)(s[i * 8 + 3])) << 24;
        val4 = ((POINTER_SIZE_INT)(unsigned char)(s[i * 8 + 4])) << 32;
        val5 = ((POINTER_SIZE_INT)(unsigned char)(s[i * 8 + 5])) << 40;
        val6 = ((POINTER_SIZE_INT)(unsigned char)(s[i * 8 + 6])) << 48;
        val7 = ((POINTER_SIZE_INT)(unsigned char)(s[i * 8 + 7])) << 56;
        val = val0 + val1 + val2 + val3 + val4 + val5 + val6 + val7;
        h1 += val;
#else /* also unaligned load */
        h1 += *((POINTER_SIZE_INT *)s + i);
#endif
    }

    for (size_t j = parts * sizeof(POINTER_SIZE_INT); j < len; j++) {
        h2 += s[j];
    }
    
    return h1 - h2;
}

String * String_Pool::lookup(const char *s, size_t len, POINTER_SIZE_INT raw_hash) {
#ifdef VM_STATS
    // we need a lock here since apr_palloc & apr_hash_set is single threaded
    LMAutoUnlock auto_lock(&VM_Statistics::get_vm_stats().vm_stats_lock);
    String_Stat * key_stats =
        (String_Stat *)apr_hash_get(string_stat, s, len);
    if (key_stats == NULL) {
        key_stats = (String_Stat *)
            apr_palloc(VM_Statistics::get_vm_stats().vm_stats_pool, sizeof(String_Stat));
        memset(key_stats, 0, sizeof(String_Stat));
        char * str = (char *)apr_palloc(VM_Statistics::get_vm_stats().vm_stats_pool, len + 1);
        memcpy(str, s, len);
        str[len] = '\0';
        apr_hash_set(string_stat, str, len, key_stats);
        key_stats->raw_hash = raw_hash;
    }
    assert(key_stats->raw_hash == raw_hash);
    ++key_stats->num_lookup;
#endif

    int hash = (int)(raw_hash % string_pool_size);

    // search bucket for string, no lock
    for (Entry *e = table[hash]; e != NULL; e = e->next) {
        if (e->str.len == len && memcmp(s, e->str.bytes, len) == 0) {
            // found string in table
            return  &e->str;
        }
#ifdef VM_STATS
        ++key_stats->num_lookup_collision;
#endif
    }

    lock_pool();

    // search bucket for string, strict variant with locking to avoid
    // duplication
    Entry **last_entry = &table[hash];
    Entry * cur_entry = *last_entry;
    while (cur_entry) {    
        if (cur_entry->str.len == len && memcmp(s, cur_entry->str.bytes, len) == 0) {
            // found string in table
            unlock_pool();
            return  &cur_entry->str;
        }
        last_entry = &(cur_entry->next);
        cur_entry = cur_entry->next;
    }

#ifdef VM_STATS
    if (table[hash]) {
        // there is already an element with the same hash
        num_ambiguity++;
    }
#endif

    // string not in table; insert a new string entry into string pool
    //
    // compute size of Entry record
    // add one to str_len for '\0'
    // subtract STRING_PADDING already in Entry
    size_t entry_size = sizeof(Entry) + len + 1 - STRING_PADDING;
    
    /* Synchronized via String_Pool lock */
    void * mem = memory_pool.alloc(entry_size);
    
    // We need ordering of writes here as we use the collection without lock.
    // Entry's next pointer should be updated before we update head reference.
    cur_entry = new(mem) Entry(s, len, 0);
    port_write_barrier();
    *last_entry = cur_entry;

    unlock_pool();
    return &cur_entry->str;
}

String * String_Pool::lookup(const char *s) {
    POINTER_SIZE_INT hash;
    size_t len;
    
    hash_it(s, &len, &hash);
    return lookup(s, len, hash);
} //String_Pool::lookup


String * String_Pool::lookup(const char *s, size_t len) {
    return lookup(s, len, hash_it(s, len));
} //String_Pool::lookup


String * String_Pool::get_first_string_intern() {
    index_interned->current = head_interned;
    index_interned->index = 0;
    return get_next_string_intern();
}

String * String_Pool::get_next_string_intern() {
    unsigned index = index_interned->index;
    if (index < index_interned->current->free_slot) {
        index_interned->index++;
        return index_interned->current->elem[index];
    }
    index_interned->current = index_interned->current->next;
    if (index_interned->current) {
        index_interned->index = 0;
        return get_next_string_intern();
    }
    return NULL;
}

void String_Pool::register_interned_string(String * str) {
    void * result;
    while ((result = apr_atomic_casptr(
        (volatile void **)(current_interned->elem + current_interned->free_slot),
        (void *)str,
        (void *)NULL)) != NULL) {
            hythread_yield();
    }
    assert(current_interned->free_slot < INTERNED_STRING_ARRAY_SIZE);
    if (current_interned->free_slot == INTERNED_STRING_ARRAY_SIZE - 1) {
        // this piece of code should be executed in one thread until current_interned is updated
        volatile Interned_Strings * local_current_interned = current_interned;
        Interned_Strings * new_elem = (Interned_Strings *)memory_pool.alloc(sizeof(Interned_Strings));
        memset(new_elem, 0, sizeof(Interned_Strings));
        current_interned->next = new_elem;
        port_write_barrier();
        current_interned = new_elem;
        port_write_barrier();
        local_current_interned->free_slot++;
    } else {
        UNSAFE_REGION_START
        current_interned->free_slot++;
        UNSAFE_REGION_END
    }
}

// NOTE: it is safe to call this function in multiple threads BUT
// don't iterate through interned strings while other threads do interning
ManagedObject * String_Pool::intern(String * str) {
    jobject string = oh_allocate_local_handle();
    ManagedObject* lang_string = string_create_from_utf8(str->bytes, str->len);

    if (!lang_string) { // if OutOfMemory
        return NULL;
    }

    if (exn_raised()) { //if RuntimeException or Error
        return NULL;
    }

    string->object = lang_string;
    assert(!hythread_is_suspend_enabled());

    Global_Env* env = VM_Global_State::loader_env;
    jvalue args[1];
    args[0].l = string;
    assert(env->VM_intern);
    vm_execute_java_method_array((jmethodID)env->VM_intern,
        (jvalue*)&string, args);

    if (exn_raised()) { //if RuntimeException or Error
        return NULL;
    }
    assert(string);
    assert(string->object);

    // Atomically update the string structure since some other thread might be trying to make the same update.
    // The GC won't be able to enumerate here since GC is disabled, so there are no race conditions with GC.
    REFS_RUNTIME_SWITCH_IF
#ifdef REFS_RUNTIME_OR_COMPRESSED
        COMPRESSED_REFERENCE compressed_lang_string =
            compress_reference(string->object);
        assert(is_compressed_reference(compressed_lang_string));
        U_32 result = apr_atomic_cas32(
            /*destination*/ (volatile U_32 *)&str->intern.compressed_ref, 
            /*exchange*/    compressed_lang_string,
            /*comparand*/   0);
        if (result == 0) {
            // Note the successful write of the object.
            gc_heap_write_global_slot_compressed(
                (COMPRESSED_REFERENCE *)&str->intern.compressed_ref,
                (Managed_Object_Handle)string->object);
            // add this string to interned strings
            register_interned_string(str);
        }
        // Some other thread may have beaten us to the slot.
        lang_string = (ManagedObject *)uncompress_compressed_reference(str->intern.compressed_ref);
#endif // REFS_RUNTIME_OR_COMPRESSED
    REFS_RUNTIME_SWITCH_ELSE
#ifdef REFS_RUNTIME_OR_UNCOMPRESSED
        void *result =
            (void *)apr_atomic_casptr(
            /*destination*/ (volatile void **)&str->intern.raw_ref,
            /*exchange*/    (void *)string->object,
            /*comparand*/   (void *)NULL);
        if (result == NULL) {
            // Note the successful write of the object.
            gc_heap_write_global_slot(
                (Managed_Object_Handle *)&str->intern.raw_ref,
                (Managed_Object_Handle)string->object);
            // add this string to interned strings
            register_interned_string(str);
        }
        // Some other thread may have beaten us to the slot.
        lang_string = str->intern.raw_ref;
#endif // REFS_RUNTIME_OR_UNCOMPRESSED
    REFS_RUNTIME_SWITCH_ENDIF

    oh_discard_local_handle(string);
    return lang_string;
}
