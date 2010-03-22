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
#include "open/types.h"
#include "port_malloc.h"
#include "port_modules.h"


native_module_t* port_find_module(native_module_t* modules, void* code_ptr)
{
    native_module_t* module;
    size_t s;

    for (module = modules; NULL != module; module = module->next)
    {
        for (s = 0; s < module->seg_count; s++)
        {
            char* base = module->segments[s].base;
            size_t size = module->segments[s].size;

            if ((char*)code_ptr >= base && (char*)code_ptr < base + size)
                return module;
        }
    }

    // no matching module
    return NULL;
}

void port_dump_modules(native_module_t* modules, FILE *out)
{
    native_module_t* module;
    size_t i;

    for (module = modules; module; module = module->next)
    {
        if (!module->filename)
            continue;

        fprintf(out, "%s:\n", module->filename);

        for (i = 0; i < module->seg_count; i++)
        {
            size_t base = (size_t)module->segments[i].base;

            fprintf(out, "\t0x%"W_PI_FMT":0x%"W_PI_FMT"\n",
                    base, base + module->segments[i].size);
        }
    }
}

void port_clear_modules(native_module_t** list_ptr)
{
    native_module_t* cur = *list_ptr;

    while (cur)
    {
        native_module_t* next = cur->next;

        if (cur->filename)
            STD_FREE(cur->filename);

        STD_FREE(cur);
        cur = next;
    }

    *list_ptr = NULL;
}
