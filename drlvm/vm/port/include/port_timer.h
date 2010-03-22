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
#ifndef _PORT_TIMER_H_
#define _PORT_TIMER_H_

#include "port_general.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
* @file
* High resolution timer
*/

/**
 * @defgroup port_timer High resolution timer
 * @ingroup port_apr
 * @{
 */

    /**
     * High resolution timer, in nanoseconds. 
     * The timer is not tied to system clocks, rather intended for precise 
     * measuring of elapsed time intervals.
     */
    typedef apr_int64_t apr_nanotimer_t;

    /**
    * Returns the value of the system timer with the best possible accuracy.
    * The difference between two subsequent invocations returns the number of
    * passed nanoseconds.
    */
    APR_DECLARE(apr_nanotimer_t) port_nanotimer();

/** @} */


#ifdef __cplusplus
}
#endif
#endif /*_PORT_TIMER_H_*/
