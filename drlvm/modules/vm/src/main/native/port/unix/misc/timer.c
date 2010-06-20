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

#include <sys/time.h>
#include "port_timer.h"

static apr_nanotimer_t initNanoTime() {
    struct timeval tv;
    struct timezone tz;

    gettimeofday(&tv, &tz);
    return (apr_nanotimer_t)tv.tv_sec;
}


APR_DECLARE(apr_nanotimer_t) port_nanotimer() 
{
    static apr_nanotimer_t init_sec = 0;
    struct timeval tv;
    struct timezone tz;
    if (!init_sec) {
        init_sec = initNanoTime();
    }

    gettimeofday(&tv, &tz);

    return (apr_nanotimer_t)
        (((apr_nanotimer_t)tv.tv_sec - init_sec) * 1E9 + (apr_nanotimer_t)tv.tv_usec * 1E3);
}
