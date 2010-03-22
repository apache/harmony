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
#define LOG_DOMAIN "port.vmem"
#include "clog.h"

#include "port_vmem.h"
#include "open/platform_types.h"

#include <windows.h>
#include <psapi.h>

#ifdef __cplusplus
extern "C" {
#endif

struct port_vmem_t {
	apr_pool_t *pool;
	void* start;
	size_t size;
	DWORD protection;
	int large;
};

// To use large pages, you need to modify 'lock pages' privileges:
// go to Administrator Tools, Local Security Settings, Local Policies, User Rights Assignments, 
// and enable 'lock pages in memory' for the user.
typedef UINT (WINAPI *PGetLargePageMinimum)();

static BOOL 
SetPrivilege (
			  HANDLE hToken,          // access token handle
			  LPCTSTR lpszPrivilege,  // name of privilege to enable/disable
			  BOOL bEnablePrivilege)  // to enable or disable privilege
{
	TOKEN_PRIVILEGES tp;
	LUID luid;

	if ( !LookupPrivilegeValue( 
		NULL,            // lookup privilege on local system
		lpszPrivilege,   // privilege to lookup 
		&luid ) ) {      // receives LUID of privilege
			CTRACE(("LookupPrivilegeValue error: %u", GetLastError())); 
			return FALSE; 
		}

		tp.PrivilegeCount = 1;
		tp.Privileges[0].Luid = luid;
		if (bEnablePrivilege)
			tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
		else
			tp.Privileges[0].Attributes = 0;

		// Enable the privilege or disable all privileges.

		AdjustTokenPrivileges(
			hToken, 
			FALSE, 
			&tp, 
			sizeof(TOKEN_PRIVILEGES), 
			(PTOKEN_PRIVILEGES) NULL, 
			(PDWORD) NULL); 

		// Call GetLastError to determine whether the function succeeded.

		if (GetLastError() != ERROR_SUCCESS) { 
			CTRACE(("AdjustTokenPrivileges failed: %u", GetLastError())); 
			return FALSE; 
		}

		return TRUE;
} // SetPrivilege



// return the page minimum size, if the user has privileges to access
// large pages, otherwise 0 
static UINT adjustprivileges() {
	HMODULE h;
	HANDLE accessToken;
	PGetLargePageMinimum m_GetLargePageMinimum;

	if (!(OpenProcessToken (GetCurrentProcess (), 
		TOKEN_ALL_ACCESS, &accessToken) 
		&& SetPrivilege (accessToken, "SeLockMemoryPrivilege", TRUE))) {
			CTRACE(("Lock Page Privilege was not set."));
			return 0;
		}

		h = GetModuleHandleA("kernel32.dll");

		m_GetLargePageMinimum = (PGetLargePageMinimum) GetProcAddress(h, "GetLargePageMinimum");
		if (!m_GetLargePageMinimum) {
			CTRACE(("Cannot locate GetLargePageMinimum."));
			return 0;
		}	
		return m_GetLargePageMinimum();
} //adjustprivileges

static DWORD convertProtectionMask(unsigned int mode){
	if (mode & PORT_VMEM_MODE_EXECUTE) {
		if (mode & PORT_VMEM_MODE_READ) {
			if (mode & PORT_VMEM_MODE_WRITE) {
				return PAGE_EXECUTE_READWRITE;
			}
			return PAGE_EXECUTE_READ;
		}
		return PAGE_EXECUTE;
	}
	if (mode & PORT_VMEM_MODE_READ) {
		if (mode & PORT_VMEM_MODE_WRITE) {
			return PAGE_READWRITE;
		}
		return PAGE_READONLY;
	}
	return PAGE_NOACCESS;
}

APR_DECLARE(apr_status_t) port_vmem_reserve(port_vmem_t **block, void **address, 
										   size_t amount, unsigned int mode, 
										   size_t pageSize, apr_pool_t *pool){
	 
	void *start = 0;
	DWORD action = MEM_RESERVE;
	DWORD protection = PAGE_NOACCESS;
	int large = 0;
    size_t* ps = port_vmem_page_sizes();

    if ((pageSize == PORT_VMEM_PAGESIZE_LARGE && ps[1] != 0)
            || pageSize > ps[0]) {

		/* Using large pages on Win64 seems to require MEM_COMMIT and PAGE_READWRITE.*/
		action = MEM_COMMIT | MEM_LARGE_PAGES;
		protection = PAGE_READWRITE;
		large = 1;
	}
	start = VirtualAlloc(*address, amount, action, protection);
	if (!start) {
		return apr_get_os_error();
	}
	*block = apr_palloc(pool, sizeof(port_vmem_t));
	(*block)->pool = pool;
	(*block)->start = start;
	(*block)->size = amount;
	(*block)->protection = convertProtectionMask(mode);
	(*block)->large = large;

	*address = start;
	return APR_SUCCESS;
}

APR_DECLARE(apr_status_t) port_vmem_commit(void **address, size_t amount, 
										  port_vmem_t *block){
	void *start = 0;
	if (block->large) {
		return APR_SUCCESS;
	}
	start = VirtualAlloc(*address, amount, MEM_COMMIT, block->protection);
	if (!start) {
		return apr_get_os_error();
	}
	*address = start;
	return APR_SUCCESS;
}


APR_DECLARE(apr_status_t) port_vmem_decommit(void *address, size_t amount, 
											port_vmem_t *block){
	if (block->large) {
		return APR_SUCCESS;
	}
	if (!VirtualFree(address, amount, MEM_DECOMMIT)) {
		return apr_get_os_error();
	}

	return APR_SUCCESS;
}


APR_DECLARE(apr_status_t) port_vmem_release(/*void *address, size_t amount,*/ 
										   port_vmem_t *block){
							   
	if (!VirtualFree(block->start, 0, MEM_RELEASE)) {
		return apr_get_os_error();
	}

	return APR_SUCCESS;
}

APR_DECLARE(size_t *) port_vmem_page_sizes(){
	static size_t page_sizes[3];
	if (!page_sizes[0]) {
		SYSTEM_INFO si;
		page_sizes[2] = 0;
		page_sizes[1] = (size_t)adjustprivileges();
		GetSystemInfo(&si);
		page_sizes[0] = si.dwPageSize;
	}
	return page_sizes;
}

APR_DECLARE(size_t) port_vmem_used_size(){
    return port_vmem_committed_size();
}

APR_DECLARE(size_t) port_vmem_committed_size(){
    // TODO: should return summarized commits instead of usage.
    PROCESS_MEMORY_COUNTERS pmc;
    if ( GetProcessMemoryInfo( GetCurrentProcess(), &pmc, sizeof(pmc)) ) {
        return (pmc.QuotaNonPagedPoolUsage + pmc.QuotaPagedPoolUsage) * 1024;
    } else {
        return (size_t)0;
    }
}

APR_DECLARE(size_t) port_vmem_reserved_size(){
    // TODO: should return summarized reservaition instead of peak.
    return port_vmem_committed_size();
}

APR_DECLARE(size_t) port_vmem_max_size(){
    PERFORMANCE_INFORMATION pi;
    if ( GetPerformanceInfo( &pi, sizeof(pi)) ) {
        return (pi.CommitLimit - pi.CommitTotal) * pi.PageSize + port_vmem_committed_size();
    } else {
        return (size_t)0;
    }
}

APR_DECLARE(apr_status_t) port_vmem_allocate(void **addr, size_t size, unsigned int mode)
{
    LPVOID start = NULL;
    DWORD protection = convertProtectionMask(mode);

	start = VirtualAlloc(*addr, size, MEM_COMMIT, protection);
	if (!start) {
		return apr_get_os_error();
	}

    *addr = start;
    return APR_SUCCESS;
}

APR_DECLARE(apr_status_t) port_vmem_free(void *addr, size_t UNREF size)
{
	if (!VirtualFree(addr, 0, MEM_RELEASE)) {
		return apr_get_os_error();
	}

	return APR_SUCCESS;
}

#ifdef __cplusplus
}
#endif
