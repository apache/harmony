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

#include <memory.h>
#include <string.h>
#include <stdio.h>

#define LOG_DOMAIN "ncai.modules"
#include "cxxlog.h"
#include "environment.h"
#include "natives_support.h"
#include "port_modules.h"
#include "open/hythread_ext.h"
#include "ncai_utils.h"
#include "ncai_direct.h"
#include "ncai_internal.h"


static ncaiError ncai_get_all_modules(ncaiModule* list, int* retCount);
static bool is_module_in_list(NCAIEnv* ncai_env, ncaiModule module);
static ncaiError ncai_get_module_info(ncaiModule module, ncaiModuleInfo *info_ptr);
static ncaiError ncai_transform_modules_to_array(ncaiModule, ncaiModule**, int);
static void ncai_clean_dead_modules_from_global_list(ncaiModule*);
static void ncai_set_global_list_modules_dead(ncaiModule modules);
static void ncai_mark_JNI_modules(ncaiModule modules);
static ncaiError ncai_alloc_module(ncaiModule* dest);
static void ncai_dealloc_module(ncaiModule module);


#ifdef PLATFORM_POSIX
#include <strings.h>
#define strcmp_case strcasecmp
#else // Win
#define strcmp_case _stricmp
#endif


ncaiError JNICALL
ncaiGetAllLoadedModules (ncaiEnv *env,
        jint *count_ptr,
        ncaiModule **modules_ptr)
{
    TRACE2("ncai.modules", "GetAllLoadedModules called");

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    ncaiError res =
        ncai_get_all_loaded_modules(env, count_ptr, modules_ptr);

    TRACE2("ncai.modules", "GetAllLoadedModules returned");
    return res;
}

ncaiError JNICALL
ncaiGetModuleInfo (ncaiEnv *env,
        ncaiModule module,
        ncaiModuleInfo *info_ptr)
{
    TRACE2("ncai.modules", "GetModuleInfo called");

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (module == NULL || info_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    if (module->info == NULL)
        return NCAI_ERROR_ILLEGAL_ARGUMENT;

    LMAutoUnlock aulock(((NCAIEnv*)env)->env_lock);

    if (!is_module_in_list((NCAIEnv*)env, module))
    {
        TRACE2("ncai.modules", "GetModuleInfo: module was not found in previously obtained list");
        return NCAI_ERROR_INVALID_MODULE;
    }

    ncaiError err = ncai_get_module_info(module, info_ptr);

    TRACE2("ncai.modules", "GetModuleInfo returned");
    return err;
}

///////////////////////
//Functions for internal use
///////////////////////

ncaiError
ncai_get_all_loaded_modules(ncaiEnv *env,
    jint *count_ptr, ncaiModule **modules_ptr)
{
    if (modules_ptr == NULL || count_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    NCAIEnv* ncai_env = (NCAIEnv*)env;
    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;
    jint count = 0;
    ncaiError res;

    Lock_Manager* plock = env ? ncai_env->env_lock : &ncai->mod_lock;
    ncaiModule* pmodules = env ? &ncai_env->modules : &ncai->modules;

    LMAutoUnlock aulock(plock);

    //gets the list of all modules from OS
    res = ncai_get_all_modules(pmodules, &count);
    if (res != NCAI_ERROR_NONE)
        return res;

    ncai_transform_modules_to_array(*pmodules, modules_ptr, count);
    *count_ptr = count;

    return NCAI_ERROR_NONE;
}

///////////////////////
//Helper functions
///////////////////////

static bool is_module_in_list(NCAIEnv* ncai_env, ncaiModule module)
{
    assert(ncai_env);
    ncaiModule modules = ncai_env->modules;

    for (ncaiModule cur = modules; cur; cur = cur->next)
    {
        if (cur == module)
            return true;
    }

    return false;
}

static ncaiError ncai_get_module_info(ncaiModule module, ncaiModuleInfo *info_ptr)
{
    ncaiModuleInfo mod_info;
    ncaiSegmentInfo* seg_info = NULL;
    seg_info = (ncaiSegmentInfo*)ncai_alloc(sizeof(ncaiSegmentInfo)*
                                        (module->info->segment_count));
    if (seg_info == NULL)
        return NCAI_ERROR_OUT_OF_MEMORY;

    for (size_t i = 0; i < module->info->segment_count; i++)
    {
        seg_info[i] = module->info->segments[i];
    }

    mod_info.kind = module->info->kind;
    mod_info.name = strdup(module->info->name);
    assert(mod_info.name);
    mod_info.filename = strdup(module->info->filename);
    assert(mod_info.filename);
    mod_info.segment_count = module->info->segment_count;
    mod_info.segments = seg_info;

    *info_ptr = mod_info;

    return NCAI_ERROR_NONE;
}

static void ncai_identify_module_is_VM(ncaiModule module)
{
    const char* vm_modules[] = {"java", "vmcore", "harmonyvm", "em", "interpreter",
        "gc_gen", "gc_gen_uncomp", "gc_cc", "vmi", "encoder", "jitrino", "hythr"};

    for (size_t i = 0; i < sizeof(vm_modules)/sizeof(vm_modules[0]); i++)
    {
        if(strcmp_case(module->info->name, vm_modules[i]) == 0)
        {
            module->info->kind = NCAI_MODULE_VM_INTERNAL;
            return;
        }
    }
}

static void ncai_set_global_list_modules_dead(ncaiModule modules)
{
    for( ; modules != NULL; modules = modules->next)
    {
        modules->isAlive = false;
    }
}

static ncaiError ncai_transform_modules_to_array(ncaiModule list, ncaiModule** retArray, int count)
{
    ncaiModule* array;

    array = (ncaiModule*) ncai_alloc(sizeof(ncaiModule) * count);

    if (array == NULL)
        return NCAI_ERROR_OUT_OF_MEMORY;

    int i = 0;
    for (ncaiModule current = list;
         current != NULL; i++, current = current->next)
    {
        array[i] = current;
    }

    *retArray = array;
    return NCAI_ERROR_NONE;
}

static void ncai_clean_dead_modules_from_global_list(ncaiModule* modules)
{
    // Address of 'next' in previous list item or address of list root
    // Is a place where current->next must be placed when removing item
    ncaiModule* plast_next = modules;

    for (ncaiModule current = *modules; current != NULL;)
    {
        if (!current->isAlive)
        {
            *plast_next = current->next;
            ncaiModule next = current->next;
            ncai_dealloc_module(current);
            current = next;
        }
        else
        {
            plast_next = &current->next;
            current = current->next;
        }
    }
}


static void ncai_mark_JNI_modules(ncaiModule modules)
{
// natives_is_library_loaded_slow is used instead of natives_is_library_loaded
// because natives_support module still receives both full and relative paths
// FIXME: This is workaround, short file names without paths are compared
    for ( ; modules != NULL; modules = modules->next)
    {
        if (modules->info->kind == NCAI_MODULE_OTHER &&
            natives_is_library_loaded_slow(modules->info->filename))
        {
            modules->info->kind = NCAI_MODULE_JNI_LIBRARY;
        }
    }
}

static bool ncai_is_same_module(ncaiModule ncai_mod, native_module_t* module)
{
    if (ncai_mod->info->segment_count != module->seg_count)
        return false;

    if (strcmp(ncai_mod->info->filename, module->filename) != 0)
        return false;

    for (size_t i = 0; i < module->seg_count; i++)
    {
        if (ncai_mod->info->segments[i].base_address != module->segments[i].base ||
            ncai_mod->info->segments[i].size != module->segments[i].size)
        {
            return false;
        }
    }

    return true;
}

static ncaiModule ncai_find_module_in_global_list(ncaiModule modules,
                native_module_t* module)
{
    for (ncaiModule current = modules;
         current != NULL;
         current = current->next)
    {
        if (ncai_is_same_module(current, module))
            return current;
    }

    return NULL;
}

static ncaiError ncai_fill_module(ncaiModule* modulePtr, native_module_t* src)
{
    ncaiError error;
    ncaiModule module;

    error = ncai_alloc_module(&module);
    if ( error != NCAI_ERROR_NONE)
        return error;

    module->info->segments =
        (ncaiSegmentInfo*)ncai_alloc(sizeof(ncaiSegmentInfo)*src->seg_count);

    if (module->info->segments == NULL)
    {
        ncai_dealloc_module(module);
        return NCAI_ERROR_OUT_OF_MEMORY;
    }

    module->info->kind = NCAI_MODULE_OTHER;
    module->info->segment_count = src->seg_count;

    size_t pathsize = strlen(src->filename) + 1;
    module->info->filename = (char*)ncai_alloc(pathsize);
    if (module->info->filename == NULL)
    {
        ncai_dealloc_module(module);
        return NCAI_ERROR_OUT_OF_MEMORY;
    }

    memcpy(module->info->filename, src->filename, pathsize);

    module->info->name = ncai_parse_module_name(module->info->filename);

    if (module->info->name == NULL)
    {
        ncai_dealloc_module(module);
        return NCAI_ERROR_OUT_OF_MEMORY;
    }

    for (size_t i = 0; i < src->seg_count; i++)
    {
        if (src->segments[i].type == SEGMENT_TYPE_CODE)
            module->info->segments[i].kind = NCAI_SEGMENT_CODE;
        else if (src->segments[i].type == SEGMENT_TYPE_DATA)
            module->info->segments[i].kind = NCAI_SEGMENT_DATA;
        else
            module->info->segments[i].kind = NCAI_SEGMENT_UNKNOWN;

        module->info->segments[i].base_address = src->segments[i].base;
        module->info->segments[i].size = src->segments[i].size;
    }

    module->isAlive = true;

    *modulePtr = module;
    return NCAI_ERROR_NONE;
}

static ncaiError ncai_add_module(ncaiModule* list, native_module_t* module)
{
    ncaiError error = NCAI_ERROR_NONE;

    ncaiModule found =
        ncai_find_module_in_global_list(*list, module);

    if (found)
    {
        found->isAlive = true;
    }
    else
    {
        ncaiModule ncai_module;
        error = ncai_fill_module(&ncai_module, module);

        if (error == NCAI_ERROR_NONE)
        {
            ncai_module->next = *list;
            *list = ncai_module;
            ncai_identify_module_is_VM(ncai_module);
        }
    }

    return error;
}

static ncaiError ncai_get_all_modules(ncaiModule* list, int* retCount)
{
    native_module_t* modules = NULL;
    int mod_count;

    ncai_set_global_list_modules_dead(*list);

    bool result = port_get_all_modules(&modules, &mod_count);

    if (!result)
        return NCAI_ERROR_INTERNAL;

    for (native_module_t* cur = modules; cur; cur = cur->next)
    {
        if (!cur->filename)
            continue;

        ncaiError error = ncai_add_module(list, cur);

        if (error != NCAI_ERROR_NONE)
        {
            port_clear_modules(&modules);
            return error;
        }
    }

    port_clear_modules(&modules);
    ncai_clean_dead_modules_from_global_list(list);
    ncai_mark_JNI_modules(*list);

    if (retCount)
        *retCount = mod_count;

    return NCAI_ERROR_NONE;
}


//allocates the memory for module, except for segments - those must be allocated, when the segments information is known.
static ncaiError ncai_alloc_module(ncaiModule* dest)
{
    assert(dest);

    ncaiModule module = (ncaiModule)ncai_alloc(sizeof(_ncaiModule));

    if (module == NULL)
        return NCAI_ERROR_OUT_OF_MEMORY;

    module->next = NULL;
    module->info = (ncaiModuleInfo*)ncai_alloc(sizeof(ncaiModuleInfo));

    if (module->info == NULL)
    {
        ncai_free(module);
        return NCAI_ERROR_OUT_OF_MEMORY;
    }

    *dest = module;
    return NCAI_ERROR_NONE;
}

static void ncai_dealloc_module(ncaiModule module)
{
    assert(module);

    if (module->info->name)
        ncai_free(module->info->name);

    if (module->info->filename)
        ncai_free(module->info->filename);

    if (module->info->segments)
        ncai_free(module->info->segments);

    if (module->info)
        ncai_free(module->info);

    ncai_free(module);
}

void clean_all_modules(ncaiModule* pmodules)
{
    while (*pmodules)
    {
        ncaiModule current = *pmodules;
        *pmodules = current->next;
        ncai_dealloc_module(current);
    }
}

// Functions are in natives_support.cpp
void lowercase_buf(char* name);
char* short_name(const char* name, char* buf);

// Returns true if short names are equal
static bool
compare_modules_by_short_name(ncaiModule module, const char* sh_name)
{
    char buf[PORT_PATH_MAX + 1];

    if (short_name(module->info->filename, buf) == NULL)
        return false; // Error case

    return (strcmp(buf, sh_name) == 0);
}

// Searches provided module list for a specified name
// Must be called under modules lock
static ncaiModule find_module_by_name(ncaiModule modules, const char* name)
{
    char name_buf[PORT_PATH_MAX + 1];

    if (short_name(name, name_buf) == NULL)
        return NULL;

    for(ncaiModule module = modules; module; module = module->next)
    {
        ncaiModuleInfo* info = module->info;

        if (compare_modules_by_short_name(module, name_buf))
            return module;
    }

    return NULL;
}

static bool is_same_modules(ncaiModule mod1, ncaiModule mod2)
{
    assert(mod1 && mod2);

    ncaiModuleInfo* info1 = mod1->info;
    ncaiModuleInfo* info2 = mod2->info;

    if (info1->segment_count != info2->segment_count)
        return false;

    for (size_t i = 0; i < info1->segment_count; i++)
    {
        if (info1->segments[i].base_address != info2->segments[i].base_address ||
            info1->segments[i].size != info2->segments[i].size)
        {
            return false;
        }
    }

    return (strcmp(info1->filename, info2->filename) == 0);
}

static ncaiModule
ncai_find_module_in_list(ncaiModule modules, ncaiModule module)
{
    for (ncaiModule current = modules;
         current != NULL;
         current = current->next)
    {
        if (is_same_modules(current, module))
            return current;
    }

    return NULL;
}

static void
find_init_module_record(NCAIEnv* env, ncaiModule module, ncaiModule* found)
{
    if (*found)
        return;

    ncaiModule* pmodules = &env->modules;

    *found = ncai_find_module_in_list(*pmodules, module);

    if (*found == NULL)
    {
        ncaiError err = ncai_get_all_modules(pmodules, NULL);
        assert(err == NCAI_ERROR_NONE);

        *found = ncai_find_module_in_list(*pmodules, module);
        assert(*found);
    }
}

typedef void (JNICALL * ncaiModLU)
        (ncaiEnv *env, ncaiThread thread, ncaiModule module);

static void report_loaded_unloaded_module(ncaiModule module, bool loaded)
{
    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;

    hythread_t hythread = hythread_self();
    ncaiThread thread = reinterpret_cast<ncaiThread>(hythread);

    bool suspend_enabled = hythread_is_suspend_enabled();

    if (!suspend_enabled)
        hythread_suspend_enable();

    TIEnv *ti_env = ti->getEnvironments();
    TIEnv *next_ti_env;

    const char* trace_text = loaded ? "ModuleLoad" : "ModuleUnload";

    while (NULL != ti_env)
    {
        next_ti_env = ti_env->next;

        NCAIEnv* env = ti_env->ncai_env;

        if (NULL == env)
        {
            ti_env = next_ti_env;
            continue;
        }

        ncaiModuleLoad func_l =
            (ncaiModuleLoad)env->get_event_callback(NCAI_EVENT_MODULE_LOAD);
        ncaiModuleLoad func_u =
            (ncaiModuleUnload)env->get_event_callback(NCAI_EVENT_MODULE_UNLOAD);

        ncaiModule env_module = NULL;
        ncaiModLU func = loaded ? (ncaiModLU)func_l : (ncaiModLU)func_u;
        ncaiEventKind event =
            loaded ? NCAI_EVENT_MODULE_LOAD : NCAI_EVENT_MODULE_UNLOAD;

        if (NULL != func)
        {
            if (env->global_events[event - NCAI_MIN_EVENT_TYPE_VAL])
            {
                TRACE2("ncai.modules",
                    "Calling global " << trace_text << " callback for module "
                    << module->info->name);

                find_init_module_record(env, module, &env_module);
                func((ncaiEnv*)env, thread, env_module);

                TRACE2("ncai.modules",
                    "Finished global " << trace_text << " callback for module "
                    << module->info->name);

                ti_env = next_ti_env;
                continue;
            }

            ncaiEventThread* next_et;
            ncaiEventThread* first_et =
                env->event_threads[event - NCAI_MIN_EVENT_TYPE_VAL];

            for (ncaiEventThread* et = first_et; NULL != et; et = next_et)
            {
                next_et = et->next;

                if (et->thread == thread)
                {
                    TRACE2("ncai.modules",
                        "Calling local " << trace_text << " callback for module "
                        << module->info->name);

                    find_init_module_record(env, module, &env_module);
                    func((ncaiEnv*)env, thread, env_module);

                    TRACE2("ncai.modules",
                        "Finished local " << trace_text << " callback for module "
                        << module->info->name);
                }
                et = next_et;
            }
        }
        ti_env = next_ti_env;
    }

    if (!suspend_enabled)
        hythread_suspend_disable();
}

void ncai_library_load_callback(const char* name)
{
    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;

    if (!ncai->isEnabled())
        return;

    LMAutoUnlock lock(&ncai->mod_lock);

    ncaiError err = ncai_get_all_modules(&ncai->modules, NULL);
    assert(err == NCAI_ERROR_NONE);

    ncaiModule module = find_module_by_name(ncai->modules, name);
    assert(module);

    report_loaded_unloaded_module(module, true);
}

void ncai_library_unload_callback(const char* name)
{
    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;

    if (!ncai->isEnabled())
        return;

    LMAutoUnlock lock(&ncai->mod_lock);

    ncaiError err = ncai_get_all_modules(&ncai->modules, NULL);
    assert(err == NCAI_ERROR_NONE);

    ncaiModule module = find_module_by_name(ncai->modules, name);
    assert(module);

    report_loaded_unloaded_module(module, false);
}
