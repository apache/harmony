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
 * @author Alexander Astapchuk
 */
/**
 * @file
 * @brief MethodInfoBlock implementation.
 */
 
#include "mib.h"

namespace Jitrino {
namespace Jet {

MethodInfoBlock::MethodInfoBlock(void)
{
    m_data = NULL;
    //
    rt_inst_addrs = NULL;
    //
    rt_header = &m_header;
    rt_header->code_start = NULL;
    num_profiler_counters = 0;
    profiler_counters_map= NULL;

}

void MethodInfoBlock::init(unsigned bc_size, unsigned stack_max, 
                           unsigned num_locals, unsigned in_slots,
                           unsigned flags)
{
    memset(rt_header, 0, sizeof(*rt_header));
    rt_header->magic = MAGIC;
    //
    rt_header->m_bc_size = bc_size;
    rt_header->m_num_locals = num_locals;
    rt_header->m_max_stack_depth = stack_max;
    rt_header->m_in_slots = in_slots;
    rt_header->m_flags = flags;

    // All the values must be initialized *before* get_bcmap_size() !
    unsigned bcmap_size = get_bcmap_size();
    m_data = new char[bcmap_size];
    memset(m_data, 0, bcmap_size);
    rt_inst_addrs = (const char**)m_data;
}

void MethodInfoBlock::save(char * to)
{
    memset(to, 0, get_total_size());
    memcpy(to, rt_header, get_hdr_size());
    memcpy(to +  get_hdr_size(), m_data, get_bcmap_size());
    rt_inst_addrs = (const char**)(to + get_hdr_size());

   //store information about profiling counters for the method in MethodInfoBlock
    U_32* countersInfo = (U_32*)(to +  get_hdr_size() + get_bcmap_size());
    countersInfo[0]=num_profiler_counters;
    if (num_profiler_counters > 0) {
        memcpy(countersInfo+1, profiler_counters_map, num_profiler_counters * sizeof(U_32));
    }
}

const char * MethodInfoBlock::get_ip(unsigned pc) const
{
    assert(pc < rt_header->m_bc_size);
    return (char*)rt_inst_addrs[pc];
}

unsigned MethodInfoBlock::get_pc(const char * ip) const
{
    const char ** data = rt_inst_addrs;
    //
    // Binary search performed, in its classical form - with 'low' 
    // and 'high'pointers, and plus additional steps specific to the nature 
    // of the data stored.
    // The array where the search is performed, may (and normally does) have
    // repetitive values: i.e. [ABBBCDEEFFF..]. Each region with the same 
    // value relate to the same byte code instruction.
    
    int l = 0;
    int max_idx = (int)rt_header->m_bc_size-1;
    int r = max_idx;
    int m = 0;
    const char * val = NULL;

    assert(rt_header->code_start <= ip);
    assert(ip < rt_header->code_start + rt_header->m_code_len);

    //
    // Step 1.
    // Find first element which is above or equal to the given IP.
    //
    while (l<=r) {
        m = (r+l)/2;
        val = *(data+m);
        assert(val != NULL);
        if (ip<val) {
            r = m-1;
        }
        else if(ip>val) {
            l = m+1;
        }
        else {
            break;
        }
    }
    
    //here, 'val'  is '*(data+m)'
    
    // Step 2.
    // If we found an item which is less or equal than key, then this step
    // is omitted.
    // If an item greater than key found, we need small shift to the previous
    // item: 
    // [ABBB..] if we find any 'B', which is > key, then we need to step back
    // to 'A'.
    //
    if (val > ip) {
        // Find very first item of the same IP value (very first 'B' in the 
        // example) - this is the beginning of the bytecode instruction 
        while (m && val == *(data+m-1)) {
            --m;
        }
        // here, 'm' points to the first 'B', and 'val' has its value ...
        if (m) {
            --m;
        }
        // ... and here 'm' points to last 'A'
        val = *(data+m);
    }
    
    // Step 3.
    // Find very first item in the range - this is the start of the bytecode 
    // instruction
    while (m && val == *(data+m-1)) {
        --m;
    }
    return m;
}


}}; // ~namespace Jitrino::Jet
