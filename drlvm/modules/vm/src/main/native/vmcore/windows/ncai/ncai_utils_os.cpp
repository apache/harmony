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

#include "open/ncai_thread.h"
#include "port_thread.h"
#include "ncai_internal.h"

bool ncai_get_generic_registers(hythread_t thread, Registers* regs)
{
    if (regs == NULL)
        return false;

    CONTEXT context;
    context.ContextFlags = CONTEXT_FULL; // CONTEXT_ALL
    IDATA status = hythread_get_thread_context(thread, &context);

    if (status != TM_ERROR_NONE)
        return false;

    port_thread_context_to_regs(regs, &context);
    return true;
}
