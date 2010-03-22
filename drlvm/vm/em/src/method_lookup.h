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
#ifndef _METHOD_LOOKUP_H_
#define _METHOD_LOOKUP_H_

#include "open/em.h"
#include "open/hythread_ext.h"
#include "port_mutex.h"

class Method_Lookup_Table;

class Method_Code
{
private:
friend class Method_Lookup_Table;

    Method_Code(Method_Handle mh, void *ca, size_t s, void *d) :
        method(mh),
        code_addr(ca),
        size(s),
        data(d)
    {
    }

    Method_Handle method;
    void *code_addr;
    size_t size;
    void *data;
};

class Method_Lookup_Table
{
public:
    Method_Lookup_Table();
    ~Method_Lookup_Table();

    void add(Method_Handle method_handle, void *code_addr,
        size_t size, void *data);
    Method_Handle find(void *ip, Boolean is_ip_past, void **code_addr, size_t *size,
        void **data);
    Boolean remove(void *code_addr);

private:
    void add(Method_Code *m);
    Method_Code *find(void *addr, Boolean is_ip_past);

    unsigned       size()           { return _next_free_entry; }
    Method_Code    *get(unsigned i);

    // Resembles add, but appends the new entry m at the end of the table. The new entry must have a starting address above all entries
    // in the table. This method does not acquire p_meth_addr_table_lock, so insertion must be protected by another lock or scheme.
    void           append_unlocked(Method_Code *m);

    void           unload_all();

#ifdef _DEBUG
    void           dump();
    void           verify();
#endif //_DEBUG

#ifdef VM_STATS
    void           print_stats();
#endif
    void table_lock()
    {
        UNREF UDATA r = port_mutex_lock(&lock);
        assert(TM_ERROR_NONE == r);
    }

    void table_unlock()
    {
        UNREF UDATA r = port_mutex_unlock(&lock);
        assert(TM_ERROR_NONE == r);
    }

    // An iterator for methods compiled by the specific JIT.
    Method_Code *get_first_method_jit(JIT_Handle *jit);
    Method_Code *get_next_method_jit(Method_Code *prev_info);

    // An iterator for all methods regardless of which JIT compiled them.
    Method_Code *get_first_code_info();
    Method_Code *get_next_code_info(Method_Code *prev_info);

    Method_Code *find_deadlock_free(void *addr);
    void           reallocate(unsigned new_capacity);
    unsigned       find_index(void *addr);

    unsigned        _capacity;
    unsigned        _next_free_entry;
    Method_Code **_table;
    Method_Code **_cache;
    osmutex_t lock;
}; //class Method_Lookup_Table


#endif //_METHOD_LOOKUP_H_
