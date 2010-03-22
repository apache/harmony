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

#ifndef _PORT_SYSINFO_H_
#define _PORT_SYSINFO_H_

/**
* @file
* System information routines
*
*/

#include "open/types.h"
#include "port_general.h"
#include <apr_pools.h>

#ifdef __cplusplus
extern "C" {
#endif
/**
 * @defgroup port_sysinfo System information routines
 * @ingroup port_apr
 * @{
 */

/**
 * Determines the absolute path of the executing process.
 * @param[out] self_name - the pointer to the requested path string
 * @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
 * @note The value returned can be freed by <code>STD_FREE</code> macro.
 */
APR_DECLARE(apr_status_t) port_executable_name(char** self_name);

/**
* Returns the number of processors in the system.
*/
APR_DECLARE(int) port_CPUs_number(void);

/**
* Returns the name of CPU architecture.
*/
APR_DECLARE(const char *) port_CPU_architecture(void);

/**
* Provides the name and version of the host operating system.
* @param[out] os_name - the pointer to the OS name string
* @param[out] os_ver  - the pointer to the OS version string
* @param pool         - a pool to allocate return buffers
* @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
*/
APR_DECLARE(apr_status_t) port_OS_name_version(char** os_name, char** os_ver, 
                                   apr_pool_t* pool);

/**
* Returns the name of the account under which the current process is executed.
* @param[out] account - the pointer to the requested name string
* @param pool         - a pool to allocate the return buffer
* @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
*/
APR_DECLARE(apr_status_t) port_user_name(char** account,
                             apr_pool_t* pool);

/**
* Returns the home path of the account under which the process is executed.
* @param[out] path - the pointer to the requested path string
* @param pool      - a pool to allocate return buffer
* @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
*/
APR_DECLARE(apr_status_t) port_user_home(char** path,
                             apr_pool_t* pool);

/**
 * Returns the name of current system time zone. Time zone names are defined 
 * in the <i>tz</i> database, see ftp://elsie.nci.nih.gov/pub/.
 * @param[out] tzname - the pointer to the name string
 * @param pool        - a pool to allocate return buffer
 * @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
 */
APR_DECLARE(apr_status_t) port_user_timezone(char** tzname,
                                             apr_pool_t* pool);

/** @} */


#ifdef __cplusplus
}
#endif
#endif //_PORT_SYSINFO_H_
