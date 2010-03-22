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
#include "port_malloc.h"
#include "port_sysinfo.h"
#if defined(FREEBSD)
#define _GNU_SOURCE
#include <dlfcn.h>
extern int main (int argc, char **argv, char **envp);
#endif

APR_DECLARE(apr_status_t) port_executable_name(char** self_name) {

    char* buf;

#if defined(FREEBSD)
    Dl_info info;

    if (dladdr( (const void*)&main, &info) == 0) {
        return APR_ENOENT;
    }

    buf = (char*)STD_MALLOC(strlen(info.dli_fname) + 1);

    if (!buf)
        return APR_ENOMEM;

    strcpy(buf, info.dli_fname);
#else
    char tmpbuf[PATH_MAX + 1];

    int n = readlink("/proc/self/exe", tmpbuf, PATH_MAX);

    if (n == -1) {
        return apr_get_os_error();
    }

    tmpbuf[n] = '\0';

    buf = (char*)STD_MALLOC(n + 1);

    if (!buf)
        return APR_ENOMEM;

    strcpy(buf, tmpbuf);
#endif

    *self_name = buf;
    return APR_SUCCESS;
}
