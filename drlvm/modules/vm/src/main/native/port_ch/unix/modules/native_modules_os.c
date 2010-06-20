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

#include <limits.h>
#include <stdio.h>
#include <memory.h>
#include <sys/types.h>
#include <unistd.h>
#include "open/types.h"
#include "port_malloc.h"
#include "port_modules.h"
#include "port_modules_unix.h"


typedef struct _raw_module raw_module;

// Structure to accumulate several segments for the same module
struct _raw_module
{
    void*               start;
    void*               end;
    Boolean             acc_r;
    Boolean             acc_x;
    char*               name;
    raw_module*         next;
};

void native_clear_raw_list(raw_module*);
raw_module* native_add_raw_segment(raw_module*, void*, void*, char, char);
native_module_t* native_fill_module(raw_module*, size_t);


void native_clear_raw_list(raw_module* list)
{
    raw_module* cur;

    if (list->name)
        STD_FREE(list->name);

    cur = list->next;

    while (cur)
    {
        raw_module* next = cur->next;
        STD_FREE(cur);
        cur = next;
    }
}

raw_module* native_add_raw_segment(raw_module* last,
                                    void* start, void* end,
                                    char acc_r, char acc_x)
{
    if (last->next == NULL)
    {
        last->next = (raw_module*)STD_MALLOC(sizeof(raw_module));
        if (last->next == NULL)
            return NULL;

        last->next->name = NULL;
        last->next->next = NULL;
    }

    last = last->next;

    last->start = start;
    last->end = end;
    last->acc_r = (acc_r == 'r');
    last->acc_x = (acc_x == 'x');

    return last;
}

native_module_t* native_fill_module(raw_module* rawModule, size_t count)
{
    size_t i;

    native_module_t* module =
        (native_module_t*)STD_MALLOC(sizeof(native_module_t) + sizeof(native_segment_t)*(count - 1));

    if (module == NULL)
        return NULL;

    module->seg_count = count;
    module->filename = rawModule->name;
    rawModule->name = NULL;
    module->next = NULL;

    for (i = 0; i < count; i++)
    {
        if (rawModule->acc_x)
            module->segments[i].type = SEGMENT_TYPE_CODE;
        else if (rawModule->acc_r)
            module->segments[i].type = SEGMENT_TYPE_DATA;
        else
            module->segments[i].type = SEGMENT_TYPE_UNKNOWN;

        module->segments[i].base = rawModule->start;
        module->segments[i].size =
            (size_t)((POINTER_SIZE_INT)rawModule->end -
                        (POINTER_SIZE_INT)rawModule->start);

        rawModule = rawModule->next;
    }

    return module;
}

Boolean port_get_all_modules(native_module_t** list_ptr, int* count_ptr)
{
    FILE* file;

    POINTER_SIZE_INT start, end;
    char acc_r, acc_x;
    char filename[PATH_MAX];
    raw_module module; // First raw module
    raw_module* lastseg = &module; // Address of last filled segment
    size_t segment_count;
    int module_count;
    native_module_t** cur_next_ptr;

    if (list_ptr == NULL || count_ptr == NULL)
        return FALSE;

    file = port_modules_procmap_open(getpid());
    if (!file)
        return FALSE;

    segment_count = 0;
    module_count = 0;
    cur_next_ptr = list_ptr;
    module.name = NULL;
    module.next = NULL;
    *list_ptr = NULL;

    while (!feof(file))
    {
        int res = port_modules_procmap_readline(file, &start, &end, &acc_r, &acc_x, filename);

        if (res < 0)
            break;

        if (res < 4)
            continue;

        if (res < 5)
            *filename = 0;

        if (module.name == NULL || // First module, or single memory region
            !(*filename)        || // Single memory region
            strcmp(module.name, filename) != 0) // Next module
        {
            if (segment_count) // Add previous module
            {
                native_module_t* filled =
                    native_fill_module(&module, segment_count);

                if (!filled)
                {
                    native_clear_raw_list(&module);
                    port_clear_modules(list_ptr);
                    fclose(file);
                    return FALSE;
                }

                *cur_next_ptr = filled;
                cur_next_ptr = &filled->next;
            }

            if (*filename)
            {
                module.name = (char*)STD_MALLOC(strlen(filename) + 1);
                if (module.name == NULL)
                {
                    native_clear_raw_list(&module);
                    port_clear_modules(list_ptr);
                    fclose(file);
                    return FALSE;
                }

                strcpy(module.name, filename);
            }
            else
                module.name = NULL;

            // Store new module information
            module.start = (void*)start;
            module.end =  (void*)end;
            module.acc_r = (acc_r == 'r');
            module.acc_x = (acc_x == 'x');
            module.next = NULL;
            ++module_count;

            lastseg = &module;
            segment_count = 1; 
        }
        else
        {
            lastseg = native_add_raw_segment(lastseg,
                                (void*)start, (void*)end, acc_r, acc_x);

            if (lastseg == NULL)
            {
                native_clear_raw_list(&module);
                port_clear_modules(list_ptr);
                fclose(file);
                return FALSE;
            }

            ++segment_count;
        }
    }

    if (segment_count) // To process the last module
    {
        native_module_t* filled = native_fill_module(&module, segment_count);

        if (!filled)
        {
            native_clear_raw_list(&module);
            port_clear_modules(list_ptr);
            fclose(file);
            return FALSE;
        }

        *cur_next_ptr = filled;
    }

    native_clear_raw_list(&module);
    fclose(file);

    *count_ptr = module_count;
    return TRUE;
}
