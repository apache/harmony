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


#include "open/hythread_ext.h" // for correct Windows.h inclusion
#include "port_modules.h"
#include "native_unwind.h"


bool native_is_in_code(UnwindContext* context, void* ip)
{
    if (!ip)
        return false;

    MEMORY_BASIC_INFORMATION mem_info;

    if (VirtualQuery(ip, &mem_info, sizeof(mem_info)) == 0)
        return false;

    if (mem_info.State != MEM_COMMIT)
        return false;

    return ((mem_info.Protect & (PAGE_EXECUTE | PAGE_EXECUTE_READ |
                    PAGE_EXECUTE_READWRITE | PAGE_EXECUTE_WRITECOPY)) != 0);
}

bool native_get_stack_range(UnwindContext* context, Registers* regs, native_segment_t* seg)
{
    MEMORY_BASIC_INFORMATION mem_info;

    if (VirtualQuery(regs->get_sp(), &mem_info, sizeof(mem_info)) == 0)
        return false;

    if (mem_info.State != MEM_COMMIT)
        return false;

    seg->base = mem_info.BaseAddress;
    seg->size = mem_info.RegionSize;
    return true;
}
