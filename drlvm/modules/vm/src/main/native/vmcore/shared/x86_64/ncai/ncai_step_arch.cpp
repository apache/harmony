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

#define LOG_DOMAIN "ncai.step"
#include "cxxlog.h"
#include "jvmti_break_intf.h"

#include "ncai_utils.h"
#include "ncai_direct.h"
#include "ncai_internal.h"


void ncai_setup_single_step(GlobalNCAI* ncai,
            const VMBreakPoint* bp, jvmti_thread_t jvmti_thread)
{
    TRACE2("ncai.step", "Setup predicted single step breakpoints: "
        << "not implemented for em64t");
}

void ncai_setup_signal_step(jvmti_thread_t jvmti_thread, NativeCodePtr addr)
{
    TRACE2("ncai.step", "Setting up single step in exception handler: "
        << "not implemented for em64t");
}
