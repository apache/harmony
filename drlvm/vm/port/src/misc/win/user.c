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

#include "port_sysinfo.h"

#include <stdio.h>
#include <windows.h>
#include <lmcons.h>
#include <userenv.h>

APR_DECLARE(apr_status_t) port_user_name(char** account,
							 apr_pool_t* pool){

	DWORD len = (UNLEN + 1)*2; /*XXX result in TCHARs */
	char* buf = apr_palloc(pool, len); 
	if (!GetUserName(buf, &len)) {
		return apr_get_os_error();
	}
	*account = buf;
	return APR_SUCCESS;
}

APR_DECLARE(apr_status_t) port_user_home(char** path,
							 apr_pool_t* pool){

	DWORD len = _MAX_PATH*2; /*XXX result in TCHARs */
	char* buf = apr_palloc(pool, len);
    HANDLE token;
    if (OpenProcessToken(GetCurrentProcess(), READ_CONTROL | TOKEN_READ, &token) 
		&& GetUserProfileDirectory(token, buf, &len)
		&& CloseHandle(token)) {
			*path = buf;
            return APR_SUCCESS;
        }
	
	return apr_get_os_error();
}

