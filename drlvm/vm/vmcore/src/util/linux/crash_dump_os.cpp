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
#include <stdlib.h>

#include "open/platform_types.h"
#include "open/hythread_ext.h"
#include "port_filepath.h"
#include "port_dso.h"
#include "port_modules.h"
#include "crash_dump.h"


static inline bool cd_is_predefined_name(const char* name)
{
    if (*name != '[')
        return false;

    return true;
//    return (!strcmp(name, "[heap]") ||
//            !strcmp(name, "[stack]") ||
//            !strcmp(name, "[vdso]"));
}

static inline native_segment_t* cd_find_segment(native_module_t* module, void* ip)
{
    for (size_t i = 0; i < module->seg_count; i++)
    {
        if (module->segments[i].base <= ip &&
            (char*)module->segments[i].base + module->segments[i].size > ip)
            return &module->segments[i];
    }

    assert(0);
    return NULL;
}

void cd_parse_module_info(native_module_t* module, void* ip)
{
    fprintf(stderr, "\nCrashed module:\n");

    if (!module)
    { // Unknown address
        fprintf(stderr, "Unknown address 0x%"W_PI_FMT"\n",
                (POINTER_SIZE_INT)ip);
        return;
    }

    native_segment_t* segment = cd_find_segment(module, ip);

    if (!module->filename)
    {
        fprintf(stderr, "Unknown memory region 0x%"W_PI_FMT":0x%"W_PI_FMT"%s\n",
                (size_t)segment->base, (size_t)segment->base + segment->size,
                (segment->type == SEGMENT_TYPE_CODE) ? "" : " without execution rights");
        return;
    }

    if (cd_is_predefined_name(module->filename))
    { // Special memory region
        fprintf(stderr, "%s memory region 0x%"W_PI_FMT":0x%"W_PI_FMT"%s\n",
                module->filename,
                (size_t)segment->base, (size_t)segment->base + segment->size,
                (segment->type == SEGMENT_TYPE_CODE) ? "" : " without execution rights");
        return;
    }

    // Common shared module
    const char* short_name = port_filepath_basename(module->filename);
    const char* module_type = cd_get_module_type(short_name);

    fprintf(stderr, "%s\n(%s)\n", module->filename, module_type);
}
