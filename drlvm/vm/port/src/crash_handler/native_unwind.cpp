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
 * @author Ilya Berezhniuk
 */

#include <string.h>
#include "port_modules.h"
#include "native_unwind.h"


//////////////////////////////////////////////////////////////////////////////
/// Helper functions

bool native_is_in_stack(UnwindContext* context, void* sp)
{
    return (sp >= context->stack.base &&
            sp < (char*)context->stack.base + context->stack.size);
}

/// Helper functions
//////////////////////////////////////////////////////////////////////////////


//////////////////////////////////////////////////////////////////////////////
//

bool port_init_unwind_context(UnwindContext* context, native_module_t* modules, Registers* regs)
{
    if (!context)
        return false;

    if (!modules)
    {
        int mod_count;
        native_module_t* mod_list = NULL;

        if (!port_get_all_modules(&mod_list, &mod_count))
            return false;

        context->clean_modules = true;
        context->modules = mod_list;
    }
    else
    {
        context->clean_modules = false;
        context->modules = modules;
    }

    if (!native_get_stack_range(context, regs, &context->stack))
    {
        if (context->clean_modules)
            port_clear_modules(&context->modules);
        return false;
    }

    return true;
}

void port_clean_unwind_context(UnwindContext* context)
{
    if (!context)
        return;

    if (context->modules && context->clean_modules)
    {
        port_clear_modules(&context->modules);
    }

    context->modules = NULL;
}

bool port_unwind_frame(UnwindContext* context, Registers* regs)
{
    if (native_is_frame_exists(context, regs))
    { // Stack frame (x86)
        return native_unwind_stack_frame(context, regs);
    }
    else
    { // Stack frame does not exist, try using heuristics
        return native_unwind_special(context, regs);
    }
}
