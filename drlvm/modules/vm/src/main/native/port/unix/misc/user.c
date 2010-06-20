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

#include <sys/types.h>
#include <pwd.h>
#include <unistd.h>
#include <errno.h>
#include "port_sysinfo.h"
#include <apr_strings.h>

APR_DECLARE(apr_status_t) port_user_name(char** account,
							 apr_pool_t* pool){

	struct passwd *user_info;
	errno = 0;
	user_info = getpwuid(getuid());
	if (errno != 0) {
		return apr_get_os_error();
	}
	*account = apr_pstrdup(pool, user_info->pw_name);
	return APR_SUCCESS;
}

APR_DECLARE(apr_status_t) port_user_home(char** path,
							 apr_pool_t* pool){

	struct passwd *user_info;
	errno = 0;
	user_info = getpwuid(getuid());
	if (errno != 0) {
		return apr_get_os_error();
	}
	*path = apr_pstrdup(pool, user_info->pw_dir);
	return APR_SUCCESS;
}
