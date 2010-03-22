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
* @author Alexey V. Varlamov
*/  

#include "port_timer.h"

#include <time.h>
#include <windows.h>


#undef LOG_DOMAIN
#define LOG_DOMAIN "port.timer"
#include "clog.h"

static LARGE_INTEGER frequency;

static BOOL initNanoTime() {
    if (QueryPerformanceFrequency(&frequency)) {
        return TRUE;
    } else {
        CTRACE(("QueryPerformanceFrequency failed: %u", GetLastError())); 
        return FALSE;
    }
}

APR_DECLARE(apr_nanotimer_t) port_nanotimer() 
{
    static BOOL hires_supported;
    static BOOL init = FALSE;
    if (!init) {
        hires_supported = initNanoTime();
    }
    if(hires_supported){
        LARGE_INTEGER count;
        if (QueryPerformanceCounter(&count)) {
            return (apr_nanotimer_t)((double)count.QuadPart / frequency.QuadPart * 1E9);
        } else {
            CTRACE(("QueryPerformanceCounter failed: %u", GetLastError())); 
        }
    }
    return (apr_nanotimer_t)(GetTickCount() * 1E6);
}
