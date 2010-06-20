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


#include <unistd.h>
#include <pthread.h>
#include "port_modules.h"
#include "native_unwind.h"

#if defined(FREEBSD)
#include <pthread_np.h>
#endif

#if defined(MACOSX)
#include <crt_externs.h>
#define environ (*_NSGetEnviron())
#else
extern char** environ;
#endif

bool native_is_in_code(UnwindContext* context, void* ip)
{
    if (!ip)
        return false;

    for (native_module_t* module = context->modules; module; module = module->next)
    {
        for (size_t i = 0; i < module->seg_count; i++)
        {
            char* base = (char*)module->segments[i].base;

            if (ip >= base &&
                ip < (base + module->segments[i].size))
                return true;
        }
    }

    return false;
}

bool native_get_stack_range(UnwindContext* context, Registers* regs, native_segment_t* seg)
{
    int err;
    pthread_attr_t pthread_attr;

    pthread_t thread = pthread_self();
    void* sp = regs->get_sp();

    if (pthread_attr_init(&pthread_attr) != 0)
        return false;

#if defined(FREEBSD)
    err = pthread_attr_get_np(thread, &pthread_attr);
#else
    err = pthread_getattr_np(thread, &pthread_attr);
#endif

    if (err != 0)
        return false;

    if (pthread_attr_getstack(&pthread_attr, &seg->base, &seg->size))
        return false;

    pthread_attr_destroy(&pthread_attr);

    if ((size_t)sp < (size_t)seg->base)
    {
        size_t page_size = (size_t)sysconf(_SC_PAGE_SIZE);
        size_t base = (size_t)sp & ~(page_size - 1);
        seg->size += (size_t)seg->base - base;
        seg->base = (void*)base;
    }

    return true;

/*    for (native_module_t* module = context->modules; module; module = module->next)
    {
        for (size_t i = 0; i < module->seg_count; i++)
        {
            char* base = (char*)module->segments[i].base;

            if (sp >= base &&
                sp < (base + module->segments[i].size))
            {
                *seg = module->segments[i];
                return true;
            }
        }
    }

    return false;
*/
}
