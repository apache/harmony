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

#include <stdio.h>

#include "open/platform_types.h"
#include "open/hythread_ext.h"
#include "port_modules.h"
#include "port_filepath.h"
#include "crash_dump.h"


static const char* cd_get_region_access_info(MEMORY_BASIC_INFORMATION* pinfo)
{
    if ((pinfo->State & MEM_COMMIT) == 0)
        return "not committed";

    if ((pinfo->Protect & PAGE_GUARD) != 0)
        return "guard page occured";

    if ((pinfo->Protect & (PAGE_EXECUTE | PAGE_EXECUTE_READ |
                          PAGE_EXECUTE_READWRITE | PAGE_EXECUTE_WRITECOPY |
                          PAGE_READWRITE | PAGE_READONLY)) != 0)
        return "";

    if ((pinfo->Protect & (PAGE_READWRITE | PAGE_READONLY)) != 0)
        return "without execution rights";

    return "without read rights";;
}

void cd_parse_module_info(native_module_t* module, void* ip)
{
    fprintf(stderr, "\nCrashed module:\n");

    if (module)
    {
        native_segment_t* segment = module->segments;

        assert(module->filename);

        if (!module->filename)
        { // We should not reach this code
            fprintf(stderr, "Unknown memory region 0x%"W_PI_FMT":0x%"W_PI_FMT"%s\n",
                    (size_t)segment->base, (size_t)segment->base + segment->size,
                    (segment->type == SEGMENT_TYPE_CODE) ? "" : " without execution rights");
            return;
        }

        // Common shared module
        const char* short_name = port_filepath_basename(module->filename);
        const char* module_type = cd_get_module_type(short_name);
        fprintf(stderr, "%s\n(%s)\n", module->filename, module_type);
        return;
    }

    // module == NULL
    size_t start_addr, end_addr, region_size;
    MEMORY_BASIC_INFORMATION mem_info;

    VirtualQuery(ip, &mem_info, sizeof(mem_info));
    start_addr = (size_t)mem_info.BaseAddress;
    region_size = (size_t)mem_info.RegionSize;
    end_addr = start_addr + region_size;

    fprintf(stderr, "Memory region 0x%"W_PI_FMT":0x%"W_PI_FMT" %s\n",
                start_addr, end_addr, cd_get_region_access_info(&mem_info));
}
