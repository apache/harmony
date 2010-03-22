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

#include <stdlib.h>
#include <unistd.h>
#include <sys/utsname.h>
#include <limits.h>
#include <errno.h>
#include "port_sysinfo.h"
#include <apr_strings.h>

APR_DECLARE(int) port_CPUs_number(void) {
	return (int)sysconf(_SC_NPROCESSORS_CONF);
}

/**
* Returns OS name and version.
*/
APR_DECLARE(apr_status_t) port_OS_name_version(char** os_name, char** os_ver, 
								   apr_pool_t* pool){

	struct utsname sys_info;
	int ret = uname(&sys_info);
	if (-1 == ret) {
		return apr_get_os_error();
	}
	*os_name = apr_pstrdup(pool, sys_info.sysname);
	*os_ver = apr_pstrdup(pool, sys_info.release);

	return APR_SUCCESS;
}

APR_DECLARE(const char *) port_CPU_architecture(void){
#if defined(_IPF_)
	return "ia64";
#elif defined (_EM64T_)
    return "x86_64";
#else
    return "x86";
#endif
}
