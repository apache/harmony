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
#ifndef __VER_UTILS_H_
#define __VER_UTILS_H_

#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include "open/types.h"

// convenience types
typedef unsigned short Address;

static const int BUFSIZE = 100;

    //TODO:
#define tc_free(ptr)                free(ptr)
#define tc_realloc(ptr, sz)         realloc(ptr, sz)
#define tc_malloc(sz)               malloc(sz)
#define tc_calloc(sz1, sz2)         calloc(sz1, sz2)
#define tc_memcpy(ptr1, ptr2, sz)   vf_memcpy(ptr1, ptr2, sz)
#define tc_memset(ptr, i1, i2)      vf_memset(ptr, i1, i2)

//TODO: delegate to compiler
inline void *vf_memcpy(void *dest, const void *src, size_t count) {
    char *d = (char *)dest;
    const char *s = (const char *)src;
    for (; count; count--) {
        *d++ = *s++;
    }
    return dest;
}

//TODO: delegate to compiler
inline void *vf_memset(void *dest, int val, size_t count) {
    char *d = (char *)dest;
    char v = (char) val;
    for (; count; count--) {
        *d++ = v;
    }
    return dest;
}

/**
* Structure of hash entry.
*/
struct vf_HashEntry_t {
    const char *key;            // hash entry key
    int key_size;               // hash entry key size
    union {                     // hash entry data
        unsigned data_index;    // when it's an index
        void* data_ptr;         // when it's data
    };
    vf_HashEntry_t *next;       // next hash entry
};

template<typename T>
class Stack {
protected:
    int max_depth;
    T* stack;
    int depth;

public:
    Stack() :
      max_depth(0), stack(0), depth(0)
      {}


      ~Stack() {
          tc_free(stack);
      }

      void push(T value) {
          if( depth == max_depth ) {
              assert(sizeof(T) < 4096);
              max_depth += 4096/sizeof(T);
              stack = (T*) tc_realloc(stack, sizeof(T) * max_depth);
          }

          stack[depth++] = value;
      }

      T pop() {
          assert(depth > 0);
          return stack[--depth];
      }

      bool is_empty() {
          return !depth;
      }

      void init() {
          depth = 0;
      }
};

template<typename T>
class FastStack : Stack<T> {
public:
    FastStack() : fdepth(0) 
    {}

    void push(T value) {
        if( fdepth < BUFSIZE ) {
            buffer[fdepth++] = value;
        } else {
            Stack<T>::push(value);
        }
    }

    T pop() {
        assert(fdepth > 0);
        return Stack<T>::is_empty() ? buffer[--fdepth] : Stack<T>::pop();
    }

    bool is_empty() {
        return !fdepth;
    }

    void init() {
        fdepth = 0;
        Stack<T>::init();
    }

    int get_depth() {
        return fdepth < BUFSIZE ? fdepth : BUFSIZE + Stack<T>::depth;
    }

    T at(int d) {
        assert(d < get_depth());
        return d < BUFSIZE ? buffer[d] : Stack<T>::stack[d - BUFSIZE];
    }

    int instack(T other) {
        int i;
        for( i = 0; i < fdepth; i++ ) {
            if( buffer[i] == other ) return true;
        }
        for( i = 0; i < Stack<T>::depth; i++ ) {
            if( Stack<T>::stack[i] == other ) return true;
        }
        return false;
    }

private:
    int fdepth;
    T buffer[BUFSIZE];
};

class MarkableStack : public FastStack<Address> {
    // contains the following entries:
    // <address, mark> no mask means zero mark

    // <non-zero address, 0> is pushed as {address}
    // <0, 0> is pushed as {0, 0}
    // <any address, non-zero mark> is pushed as {address, mark, 0}

public:
    void xPop(Address *addr, short *mark) {
        *addr = pop();
        *mark = (*addr) ? 0 : pop();

        if( *mark ) {
            *addr = pop();
        }
    }

    void xPush(Address value) {
        if( value ) { 
            push(value);
        } else {
            push(0);
            push(0);
        }
    }

    void xPush(Address addr, short m) {
        push(addr);
        push(m);
        push(0);
    }
};

struct MemoryPageHead {
    MemoryPageHead *next;
    size_t size;

    MemoryPageHead *get_next(size_t min_size, size_t max_size) {
        assert(this);
        MemoryPageHead *ret = this;
        while ( ret->next && ret->next->size < min_size ) {
            ret = ret->next;
        }

        return ret->next ? ret->next : (ret->next = create_next(max_size));
    }

    MemoryPageHead *create_next(size_t max_size) {
        MemoryPageHead *ret =(MemoryPageHead*)tc_malloc(max_size + sizeof(MemoryPageHead));
        ret->size = max_size;
        ret->next = 0;
        return ret;
    }
};

static const int STATICSZ = 2000;

class Memory {
    MemoryPageHead *static_page;
    MemoryPageHead *current_page;

    U_8           static_mem[STATICSZ + sizeof (MemoryPageHead) ];

    size_t  page_size;
    size_t  used;
public:
    Memory() 
    {
        //in 90% of cases no memory allocation will be required
        static_page = (MemoryPageHead *)&static_mem;
        static_page->next = 0;
        static_page->size = STATICSZ;

        init();
    }

    ~Memory() {
        current_page = static_page->next;
        while (current_page) {
            MemoryPageHead *next = current_page->next;
            tc_free(current_page);
            current_page = next;
        }
    }

    void init() {
        used = 0;
        current_page = static_page;
        page_size = current_page->size;
    }

    void *malloc(size_t sz) {
        size_t need_on_page = used + sz;

        if( need_on_page > page_size ) {
            //create next page

            //define new page size - some heuristic formula. subject to change
            size_t desired_size = need_on_page + need_on_page/2 + 128;

            //allocating next page
            current_page = current_page->get_next(sz, desired_size);
            if( !static_page ) {
                static_page = current_page;
            }
            used = 0;
            page_size = current_page->size;
        }

        void *ret = (U_8*)current_page + sizeof(MemoryPageHead) + used;
        used += sz;
        return ret;
    }

    void *calloc(size_t sz) {
        void *ret = malloc(sz);
        tc_memset(ret, 0, sz);
        return ret;
    }

    void dealloc_last(void* ptr, size_t sz) {
        assert( ((U_8*)ptr) + sz == (U_8*)current_page + sizeof(MemoryPageHead) + used );
        used -= sz;
    }
};

static const unsigned HASH_SIZE = 128;   ///< hash table size
static const unsigned HASH_MASK = 127;   ///< hash table mask to avoid division

/**
* Verifier hash table structure.
*/
struct vf_Hash {
public:
    /**
    * Hash table constructor.
    * @note Function allocates memory for hash pool and hash table.
    */
    vf_Hash() 
    {
        memoryPool.init();
        m_hash = (vf_HashEntry_t**)memoryPool.calloc(HASH_SIZE * sizeof(vf_HashEntry_t*));
        assert((0xFFFFFFFF & HASH_MASK) + 1 == HASH_SIZE );
    } // vf_Hash::vf_Hash


    /**
    * Function looks up hash entry which is identical to given hash key.
    * @param key - given hash key
    * @return Hash entry which is identical to given hash key.
    * @see vf_HashEntry_t
    */
    vf_HashEntry_t * Lookup( const char *key ) {
        assert( key );
        int length = (int)strlen(key);

        unsigned hash_index = HashFunc( key, length );

        vf_HashEntry_t *hash_entry = m_hash[hash_index];
        while( hash_entry != NULL ) {
            if( CheckKey( hash_entry, key, length ) ) {
                return hash_entry;
            }
            hash_entry = hash_entry->next;
        }
        return NULL;
    } // vf_Hash::Lookup( key )

    /**
     * Creates a hash entry which is identical to a given hash key.
     * @param key - given hash key
     * @param length - length for the key
     * @return Hash entry which are identical to given hash key.
     * @see vf_HashEntry_t
     * @note Created hash key and hash entry is allocated into hash memory pool.
    */
    vf_HashEntry_t * NewHashEntry( const char *key, int length ) {
        // lookup type in hash
        assert( key );
        unsigned hash_index = HashFunc( key, length );

        vf_HashEntry_t *hash_entry = m_hash[hash_index];
        while( hash_entry != NULL ) {
            if( CheckKey( hash_entry, key, length ) ) {
                return hash_entry;
            }
            hash_entry = hash_entry->next;
        }

        if( !hash_entry ) {
            // create key string
            char *hash_key = (char*)memoryPool.malloc( (length & (~3)) + 4);
            tc_memcpy( hash_key, key, length );
            hash_key[length] = 0;

            hash_entry = (vf_HashEntry_t*)memoryPool.malloc(sizeof(vf_HashEntry_t));
            hash_entry->key = hash_key;
            hash_entry->key_size = length;
            hash_entry->next = m_hash[hash_index];

            hash_entry->data_ptr = 0;
            hash_entry->data_index = 0;

            m_hash[hash_index] = hash_entry;
        }

        return hash_entry;
    } // vf_Hash::NewHashEntry( key, length )

    /**
    * Function creates hash entry which is identical to given hash key.
    * @param key - given hash key
    * @return Hash entry which are identical to given hash key.
    * @see vf_HashEntry_t
    * @note Created hash key and hash entry is allocated into hash memory pool.
    */
    vf_HashEntry_t * NewHashEntry( const char *key) {
        return NewHashEntry(key, (int)strlen(key));
    } // vf_Hash::NewHashEntry( key )

private:
    Memory memoryPool;
    vf_HashEntry_t **m_hash;    ///< hash table

    /**
    * Function checks key identity.
    * @param hash_entry - checked hash entry
    * @param key        - checked key
    * @return If keys are identical function returns <code>true</code>,
    *         else returns <code>false</code>.
    * @see vf_HashEntry_t
    */
    int CheckKey( vf_HashEntry_t *hash_entry, const char *key, int length) {
        if( hash_entry->key_size != length ) return false;

        const char* h_key = hash_entry->key; 
        int idx = 0;

        for( ; idx < length - 3; idx += 4 ) {
            if( *((U_32*) (key+idx) ) != *((U_32*) (h_key+idx) ) ) return false;
        }

        for( ; idx < length; idx++) {
            if( *(key+idx) != *(h_key+idx) ) return false;
        }

        return true;
    }

    /**
    * Hash function.
    * @param key - key for hash function
    * @return Hash index relevant to key.
    */
    unsigned HashFunc( const char *key, int length ) {
        unsigned result = 0;

        int idx = 0;

        for( ; idx < length - 3; idx += 4 ) {
            result += *((U_32*) (key+idx) );
        }

        for( ; idx < length; idx++) {
            result += *(key+idx);
        }

        U_8 *bres = (U_8*) &result;

        return (bres[0] + bres[1] + bres[2] + bres[3]) & HASH_MASK;
    } // vf_Hash::HashFunc( key )

}; // struct vf_Hash

// external declarations for error reporting functions
class vf_Context_Base;
struct vf_TypeConstraint;

void vf_create_error_message(Method_Handle method, vf_Context_Base context, char** msg);
void vf_create_error_message(Class_Handle klass, vf_TypeConstraint* constraint, char** msg);

#endif
