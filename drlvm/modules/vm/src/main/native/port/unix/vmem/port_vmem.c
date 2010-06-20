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

#include <stdio.h>
#include <sys/mman.h>
#include <unistd.h>
#include <errno.h>
#include <stdlib.h>
#include <limits.h>
#include "port_vmem.h"

#ifdef __cplusplus
extern "C" {
#endif

struct port_vmem_t {
	apr_pool_t *pool;
	void* start;
	size_t size;
	size_t page;
	unsigned int protection;
};

static int convertProtectionBits(unsigned int mode){
 	int bits = 0;
 	if (mode & PORT_VMEM_MODE_READ) {
 		bits |= PROT_READ;
 	}
 	if (mode & PORT_VMEM_MODE_WRITE) {
 		bits |= PROT_WRITE;
 	}
 	if (mode & PORT_VMEM_MODE_EXECUTE) {
 		bits |= PROT_EXEC;
 	}

    return bits;
}

APR_DECLARE(apr_status_t) port_vmem_reserve(port_vmem_t **block, void **address, 
        size_t size, unsigned int mode, 
        size_t page, apr_pool_t *pool) {

    void *start = 0;
    if (PORT_VMEM_PAGESIZE_DEFAULT == page || PORT_VMEM_PAGESIZE_LARGE == page) {
        page = port_vmem_page_sizes()[0];
    }

    int protection = convertProtectionBits(mode);

    errno = 0;
    size = (size + page - 1) & ~(page - 1); /* Align */

#ifdef MAP_ANONYMOUS
    start = mmap(*address, size, protection, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
#elif defined(MAP_ANON)
    start = mmap(*address, size, protection, MAP_PRIVATE | MAP_ANON, -1, 0);
#else
    int fd = open("/dev/zero", O_RDONLY);
    start = mmap(*address, size, protection, MAP_PRIVATE, fd, 0);
    close(fd);
#endif

    if (MAP_FAILED == start) {
        return apr_get_os_error();
    }

    *block = apr_palloc(pool, sizeof(port_vmem_t));
    (*block)->pool = pool;
    (*block)->start = start;
    (*block)->size = size;
    (*block)->page = page;
    (*block)->protection = protection;

    *address = start;

    return APR_SUCCESS;
}

APR_DECLARE(apr_status_t) port_vmem_commit(void **address, size_t amount, 
										  port_vmem_t *block){
	size_t page = block->page;
	void *aligned = (void *)(((long)*address + page-1) & ~(page-1));
	errno = 0;
	if(mprotect(aligned, amount, block->protection)) {
		return apr_get_os_error();
	}
	*address = aligned;

	return APR_SUCCESS;
}


APR_DECLARE(apr_status_t) port_vmem_decommit(void *address, size_t amount, 
											port_vmem_t *block){
	return APR_SUCCESS;
}


APR_DECLARE(apr_status_t) port_vmem_release(/*void *address, size_t amount,*/ 
										   port_vmem_t *block){
	munmap(block->start, block->size);
	return APR_SUCCESS;
}

APR_DECLARE(size_t *) port_vmem_page_sizes() {

	static size_t page_sizes[2];
	if (!page_sizes[0]) {
		page_sizes[1] = 0;
		page_sizes[0] = sysconf(_SC_PAGE_SIZE);
		if (!page_sizes[0]) {
			page_sizes[0] = 4*1024;
		}
	}
	return page_sizes;
}

APR_DECLARE(size_t) port_vmem_used_size(){
    // TODO: Update this method when/if new common memory manager will be created 
    return port_vmem_committed_size();
}

APR_DECLARE(size_t) port_vmem_reserved_size(){
    // TODO: Update this method when/if new common memory manager will be created
    return port_vmem_committed_size();
}

#ifdef FREEBSD
APR_DECLARE(size_t) port_vmem_committed_size(){
    return 0; /* TOFIX: Implement */
}
APR_DECLARE(size_t) port_vmem_max_size(){
    return 0; /* TOFIX: Implement */
}
#else
APR_DECLARE(size_t) port_vmem_committed_size(){
    char* buf = (char*) malloc(PATH_MAX + 1);
    size_t vmem = 0;
    pid_t my_pid = getpid();

    sprintf(buf, "/proc/%d/statm", my_pid);
    FILE* file = fopen(buf, "r");
    if (!file) {
        goto cleanup;
    }
    size_t size = 0;
    ssize_t len = getline(&buf, &size, file);
    fclose(file);
    if (len == -1) {
        goto cleanup;
    }
    sscanf(buf, "%lu", &vmem);

cleanup:
    free(buf);
    return vmem * port_vmem_page_sizes()[0];
}

APR_DECLARE(size_t) port_vmem_max_size(){
    char* buf = (char*) malloc(PATH_MAX + 1);
    int pid, ppid, pgrp, session, tty_nr, tpgid, exit_signal, processor;
    char comm[PATH_MAX];
    char state;
    unsigned long flags, minflt, cminflt, majflt, cmajflt, utime, stime,
        starttime, vsize, rlim = 0, startcode, endcode, startstack, kstkesp,
        kstkeip, signal, blocked, sigignore, sigcatch, wchan, nswap, cnswap;
    long cutime, cstime, priority, nice, unused, itrealvalue, rss;

    pid_t my_pid = getpid();
    sprintf(buf, "/proc/%d/stat", my_pid);
    FILE* file = fopen(buf, "r");
    if (!file) {
        goto cleanup;
    }
    size_t size = 0;
    ssize_t len = getline(&buf, &size, file);
    fclose(file);
    if (len == -1) {
        goto cleanup;
    }

    sscanf(buf, "%d %s %c %d %d %d %d %d %lu %lu %lu %lu "
        "%lu %lu %lu %ld %ld %ld %ld %ld %ld %lu %lu %ld %lu %lu %lu "
        "%lu %lu %lu %lu %lu %lu %lu %lu %lu %lu %d %d",
        &pid, &comm, &state, &ppid, &pgrp, &session, &tty_nr, &tpgid, &flags,
        &minflt, &cminflt, &majflt, &cmajflt, &utime, &stime, &cutime, &cstime,
        &priority, &nice, &unused, &itrealvalue, &starttime, &vsize, &rss, &rlim,
        &startcode, &endcode, &startstack, &kstkesp, &kstkeip, &signal, &blocked,
        &sigignore, &sigcatch, &wchan, &nswap, &cnswap, &exit_signal, &processor);

cleanup:
    free(buf);
    return rlim;
}
#endif

APR_DECLARE(apr_status_t) port_vmem_allocate(void **addr, size_t size, unsigned int mode)
{
    void *start = NULL;
    int protection = convertProtectionBits(mode);

#ifdef MAP_ANONYMOUS
    start = mmap(*addr, size, protection, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
#elif defined(MAP_ANON)
    start = mmap(*addr, size, protection, MAP_PRIVATE | MAP_ANON, -1, 0);
#else
    int fd = open("/dev/zero", O_RDONLY);
    start = mmap(*addr, size, protection, MAP_PRIVATE, fd, 0);
    close(fd);
#endif

    if (MAP_FAILED == start)
        return apr_get_os_error();

    *addr = start;
    return APR_SUCCESS;
}

APR_DECLARE(apr_status_t) port_vmem_free(void *addr, size_t size)
{
	munmap(addr, size);
	return APR_SUCCESS;
}

#ifdef __cplusplus
}
#endif
