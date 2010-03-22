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

#include "port_malloc.h"
#include "port_sysinfo.h"

#include <stdio.h>
#include <stdlib.h>
#include <windows.h>

APR_DECLARE(int) port_CPUs_number() {
	SYSTEM_INFO sys_info;
	typedef void (WINAPI *PTR_GETNATIVESYSTEM_INFO)(LPSYSTEM_INFO);
	static PTR_GETNATIVESYSTEM_INFO pTrGetNativeSystemInfo = NULL;
    if (!pTrGetNativeSystemInfo) {
        HMODULE h = GetModuleHandleA("kernel32.dll");
       /* 
        * Use GetNativeSystemInfo if available in kernel 
        * It provides more accurate info in WOW64 mode
        */
        pTrGetNativeSystemInfo = (PTR_GETNATIVESYSTEM_INFO) GetProcAddress(h, "GetNativeSystemInfo");
    }
    if(pTrGetNativeSystemInfo != NULL) {
		pTrGetNativeSystemInfo(&sys_info);
    } else {
		GetSystemInfo(&sys_info);
    }
	return sys_info.dwNumberOfProcessors;
}

APR_DECLARE(apr_status_t) port_OS_name_version(char** os_name, char** os_ver, 
								   apr_pool_t* pool){
	
	char* name_buf = NULL;
	char* ver_buf;
	OSVERSIONINFO vi;
	vi.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);
	if (!GetVersionEx(&vi))
		return apr_get_os_error();
	
	switch (vi.dwPlatformId) 
	{
	case VER_PLATFORM_WIN32_NT: /*Windows NT, Windows 2000, Windows XP, 
								or Windows Server 2003 family.*/
		switch (vi.dwMajorVersion)
		{
		case 3:
		case 4:
			name_buf = "Windows NT";
			break;
		case 5:
			switch (vi.dwMinorVersion)
			{
			case 0: name_buf = "Windows 2000"; break;
			case 1: name_buf = "Windows XP"; break;
			case 2: name_buf = "Windows Server 2003"; break;
			}
			break;
		}
		break;
	case VER_PLATFORM_WIN32_WINDOWS: /*Windows 95, Windows 98, or Windows Me.*/
		if (4 == vi.dwMajorVersion) {
			switch (vi.dwMinorVersion)
			{
			case 0: name_buf = "Windows 95"; break;
			case 10: name_buf = "Windows 98"; break;
			case 90: name_buf = "Windows Me"; break;
			}
		}
		break;
	case VER_PLATFORM_WIN32s: /* Win32s on Windows 3.1. */
		name_buf = "Win32s";
		break;
	}
	if (!name_buf) name_buf = "Windows";

	ver_buf = apr_palloc(pool, 20);
	sprintf(ver_buf, "%d.%d", vi.dwMajorVersion, vi.dwMinorVersion);
	
	*os_name = name_buf;
	*os_ver = ver_buf;
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
