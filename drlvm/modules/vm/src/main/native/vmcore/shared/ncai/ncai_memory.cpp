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
 * @author Petr Ivanov
 */
#define LOG_DOMAIN "ncai.memory"
#include "cxxlog.h"
#include "port_memaccess.h"
#include "jvmti_break_intf.h"
#include "environment.h"
#include "jvmti_internal.h"
#include "ncai_direct.h"
#include "ncai_internal.h"


struct BreakListItem
{
    VMBreakPoint*   bp;
    BreakListItem*  next;
};

struct RewriteArray
{
    BreakListItem*  array;
    BreakListItem*  list;
    size_t          size;
    size_t          count;
};

static void add_rewrite_address(RewriteArray* prwa, VMBreakPoint* bp);


ncaiError JNICALL ncaiReadMemory(ncaiEnv* env,
        void* addr, size_t size, void* buf)
{
    TRACE2("ncai.memory", "ReadMemory called");

    if (!env)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    int err = port_read_memory(addr, size, buf);

    if (err != 0)
        return NCAI_ERROR_ACCESS_DENIED;

    // Restore bytes changed by JVMTI/NCAI breakpoints
    VMBreakPoints* vm_breaks = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_breaks->get_lock());

    void* end_addr = (char*)addr + size;
    jbyte* cbuf = (jbyte*)buf;

    for (VMBreakPoint* cur = vm_breaks->get_first_breakpoint(); cur;
         cur = vm_breaks->get_next_breakpoint(cur))
    {
        if (cur->addr >= addr && cur->addr < end_addr)
        {
            size_t offset = (size_t)cur->addr - (size_t)addr;
            cbuf[offset] = cur->saved_byte;
        }
    }

    return NCAI_ERROR_NONE;
}

ncaiError JNICALL ncaiWriteMemory(ncaiEnv* env,
        void* addr, size_t size, void* buf)
{
    TRACE2("ncai.memory", "WriteMemory called");

    if (!env)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    VMBreakPoints* vm_breaks = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_breaks->get_lock());

    jbyte* end_addr = (jbyte*)addr + size;
    VMBreakPoint* cur;
    size_t rewrite_count = 0;

    // Count addresses in given range
    for (cur = vm_breaks->get_first_breakpoint(); cur;
         cur = vm_breaks->get_next_breakpoint(cur))
    {
        if (cur->addr >= addr && cur->addr < end_addr)
            ++rewrite_count;
    }

    // Simple case: there are no breakpoints in specified address range
    if (rewrite_count == 0)
    {
        if (port_write_memory(addr, size, buf) == 0)
            return NCAI_ERROR_NONE;
        else
            return NCAI_ERROR_ACCESS_DENIED;
    }

    // Allocate array for address sorting
    RewriteArray rwa;
    rwa.array = (BreakListItem*)STD_MALLOC(rewrite_count*sizeof(BreakListItem));

    if (!rwa.array)
        return NCAI_ERROR_OUT_OF_MEMORY;

    memset(rwa.array, 0, rewrite_count*sizeof(BreakListItem));
    rwa.list = NULL;
    rwa.size = rewrite_count;
    rwa.count = 0;

    // Add matching addresses to sorted list
    for (cur = vm_breaks->get_first_breakpoint(); cur;
         cur = vm_breaks->get_next_breakpoint(cur))
    {
        if (cur->addr >= addr && cur->addr < end_addr)
            add_rewrite_address(&rwa, cur);
    }

    // Write memory
    size_t remain = size;
    BreakListItem* cur_bla = rwa.list;
    assert(cur_bla);
    jbyte* cbuf = (jbyte*)buf;
    jbyte* cur_addr = (jbyte*)addr;
    int err = 0;

    while (remain)
    {
        if (!cur_bla || cur_bla->bp->addr > cur_addr)
        { // Write buffer to memory
            size_t offset = (size_t)cur_addr - (size_t)addr;
            size_t chunk_size = cur_bla ? ((jbyte*)cur_bla->bp->addr - cur_addr)
                                        : (end_addr - cur_addr);
            err = port_write_memory(cur_addr, chunk_size, cbuf + offset);

            if (err != 0)
                break;

            cur_addr += chunk_size;
            remain -= chunk_size;
        }

        if (cur_bla)
        { // Write byte from buffer to 'saved_byte' field in breakpoint
            assert(cur_addr == cur_bla->bp->addr);

            size_t offset = (size_t)cur_addr - (size_t)addr;
            cur_bla->bp->saved_byte = cbuf[offset];
            cur_bla = cur_bla->next;
            ++cur_addr;
            --remain;
        }
    }

    STD_FREE(rwa.array);
    return (err == 0) ? NCAI_ERROR_NONE : NCAI_ERROR_ACCESS_DENIED;
}

// Adding sorted item into rewrite list
static void add_rewrite_address(RewriteArray* prwa, VMBreakPoint* bp)
{
    assert(prwa && prwa->array && prwa->size && bp);

    BreakListItem** pcur = &prwa->list;

    // Look for place for insertion
    while (*pcur && (*pcur)->bp->addr < bp->addr)
        pcur = &((*pcur)->next);

    BreakListItem* freeitem = prwa->array + prwa->count++;
    assert(prwa->count <= prwa->size);

    freeitem->next = *pcur;
    *pcur = freeitem;
    freeitem->bp = bp;
}
