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
 * @author Ilya Berezhniuk
 */
#include <sys/mman.h>
#include <limits.h>
#include <unistd.h>

#include "port_general.h"
#include "port_barriers.h"
#include "port_malloc.h"

#include "signals_internal.h"
#include "port_memaccess.h"
#include "port_thread_internal.h"


extern "C" void port_memcpy_asm(void* dst, const void* src, size_t size,
                                void** prestart_addr, void* memcpyaddr);


static int memcpy_internal(void* dst, const void* src, size_t size, int from)
{
    int res;
    port_tls_data_t* tlsdata = get_private_tls_data();

    if (!tlsdata)
    {
        tlsdata = (port_tls_data_t*)STD_ALLOCA(sizeof(port_tls_data_t));

        res = port_thread_attach_local(tlsdata, TRUE, TRUE, 0);

        if (res != 0)
            return res;
    }

    tlsdata->violation_flag = 1;

    void* memcpyaddr = (void*)&memcpy;
    port_memcpy_asm(dst, src, size, &tlsdata->restart_address, memcpyaddr);

    if (tlsdata->violation_flag == 1)
    {
        tlsdata->violation_flag = 0;
        port_thread_detach_temporary();
        return 0;
    }

    // Try changing memory access
    size_t page_size = (size_t)sysconf(_SC_PAGE_SIZE);

    const void* addr = from ? src : dst;

    POINTER_SIZE_INT start =
        (POINTER_SIZE_INT)addr & ~(page_size - 1);
    POINTER_SIZE_INT pastend =
        ((POINTER_SIZE_INT)addr + size + page_size - 1) & ~(page_size - 1);

    int result = mprotect((void*)start, pastend - start,
                            PROT_READ | PROT_WRITE | PROT_EXEC);

    if (result == EAGAIN)
    {
        timespec delay = {0, 10};
        nanosleep(&delay, NULL);
        result = mprotect((void*)start, pastend - start,
                            PROT_READ | PROT_WRITE | PROT_EXEC);
    }

    if (result != 0)
    {
        tlsdata->violation_flag = 0;
        port_thread_detach_temporary();
        return -1;
    }

    tlsdata->violation_flag = 1;

    port_memcpy_asm(dst, src, size, &tlsdata->restart_address, memcpyaddr);

    res = (tlsdata->violation_flag == 1) ? 0 : -1;
    tlsdata->violation_flag = 0;
    port_thread_detach_temporary();
    return res;
}


int port_read_memory(void* addr, size_t size, void* buf)
{
    if (!buf || !addr)
        return -1;

    if (size == 0)
        return 0;

    return memcpy_internal(buf, addr, size, 1);
}

int port_write_memory(void* addr, size_t size, void* buf)
{
    if (!buf || !addr)
        return -1;

    if (size == 0)
        return 0;

    int result = memcpy_internal(addr, buf, size, 0);

    if (result == 0)
    {
        port_rw_barrier();
    }

    return result;
}
