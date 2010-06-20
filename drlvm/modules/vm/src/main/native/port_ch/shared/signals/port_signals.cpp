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


#include "port_crash_handler.h"
#include "port_memaccess.h"
#include "signals_internal.h"


int port_set_breakpoint(void* addr, unsigned char* prev)
{
    if (!addr || !prev)
        return -1;

    unsigned char buf;
    unsigned char instr = INSTRUMENTATION_BYTE;

    int err = port_read_memory(addr, 1, &buf);
    if (err != 0) return err;

    if (buf == instr)
        return -1;

    err = port_write_memory(addr, 1, &instr);
    if (err != 0) return err;

    *prev = buf;
    return 0;
}

int port_clear_breakpoint(void* addr, unsigned char prev)
{
    if (!addr)
        return -1;

    unsigned char buf;
    unsigned char instr = INSTRUMENTATION_BYTE;

    int err = port_read_memory(addr, 1, &buf);
    if (err != 0) return err;

    if (buf != instr)
        return -1;

    return port_write_memory(addr, 1, &prev);
}

Boolean port_is_breakpoint_set(void* addr)
{
    unsigned char byte;

    if (port_read_memory(addr, 1, &byte) != 0)
        return FALSE;

    return (byte == INSTRUMENTATION_BYTE);
}

