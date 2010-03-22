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

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/wait.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <assert.h>
#include <ctype.h>

#include "port_dso.h"
#include "port_malloc.h"
#include "stack_dump.h"

#if defined(MACOSX)
#include <crt_externs.h>
#define environ (*_NSGetEnviron())
#else
extern char** environ;
#endif

static char* g_curdir = NULL;
static char* g_cmdline = NULL;


static inline native_segment_t* sd_find_segment(native_module_t* module, void* ip)
{
    for (size_t i = 0; i < module->seg_count; i++)
    {
        if (module->segments[i].base <= ip &&
            (char*)module->segments[i].base + module->segments[i].size > ip)
            return &module->segments[i];
    }

    assert(0);
    return NULL;
}

void sd_get_c_method_info(CFunInfo* info, native_module_t* module, void* ip)
{
    *info->name = 0;
    *info->filename = 0;
    info->line = -1;

    if (!module || !module->filename)
        return;

    POINTER_SIZE_INT offset = (POINTER_SIZE_INT)ip;

    if (strstr(module->filename, PORT_DSO_EXT) != NULL) // Shared object
    { // IP for addr2line should be an offset within shared library
        native_segment_t* seg = sd_find_segment(module, ip);
        offset -= (POINTER_SIZE_INT)seg->base;
    }

    int po[2];
    pipe(po);

    char ip_str[20];
    sprintf(ip_str, "0x%"PI_FMT"x\n", offset);

    if (!fork())
    {
        close(po[0]);
        dup2(po[1], 1);
        execlp("addr2line", "addr2line", "-f", "-s", "-e", module->filename, "-C", ip_str, NULL);
        //fprintf(stderr, "Warning: Cannot run addr2line. No symbolic information will be available\n");
        printf("??\n??:0\n"); // Emulate addr2line output
        exit(-1);
    }
    else
    {
        close(po[1]);
        char buf[sizeof(info->name) + sizeof(info->filename)];
        int status;
        wait(&status);
        int count = read(po[0], buf, sizeof(buf) - 1);
        close(po[0]);

        if (count < 0)
        {
            fprintf(stderr, "read() failed during addr2line execution\n");
            return;
        }

        while (isspace(buf[count-1]))
            count--;

        buf[count] = '\0';
        int i = 0;

        for (; i < count; i++)
        {
            if (buf[i] == '\n')
            { // Function name is limited by '\n'
                buf[i] = '\0';
                strncpy(info->name, buf, sizeof(info->name));
                break;
            }
        }

        if (i == count)
            return;

        char* fn = buf + i + 1;

        for (; i < count && buf[i] != ':'; i++); // File name and line number are separated by ':'

        if (i == count)
            return;

        buf[i] = '\0';
        strncpy(info->filename, fn, sizeof(info->filename));

        info->line = atoi(buf + i + 1); // Line number

        if (info->line == 0)
            info->line = -1;
    }
}

void sd_init_crash_handler()
{
    // Get current directory
    char buf[PATH_MAX + 1];
    char* cwd = getcwd(buf, sizeof(buf));

    if (cwd)
    {
        cwd = (char*)STD_MALLOC(strlen(cwd) + 1);
        g_curdir = cwd;
        if (cwd)
            strcpy(cwd, buf);
    }

    // Get command line
    sprintf(buf, "/proc/%d/cmdline", getpid());
    int file = open(buf, O_RDONLY);

    if (file > 0)
    {
        size_t size = 0;
        char rdbuf[256];
        ssize_t rd;
        do
        {
            rd = read(file, rdbuf, sizeof(rdbuf));
            size += (rd > 0) ? rd : 0;
        } while (rd == sizeof(rdbuf));

        if (size)
        {
            char* cmd = (char*)STD_MALLOC(size + 1);
            g_cmdline = cmd;
            if (cmd)
            {
                cmd[size] = '\0';
                lseek(file, 0, SEEK_SET);
                read(file, cmd, size);
            }
        }
        close(file);
    }
}

void sd_cleanup_crash_handler()
{
    STD_FREE(g_curdir);
    STD_FREE(g_cmdline);
}

void sd_print_cmdline_cwd()
{
    fprintf(stderr, "\nCommand line:\n");
    for (const char* ptr = g_cmdline; *ptr; ptr += strlen(ptr) + 1)
        fprintf(stderr, "%s ", ptr);
    fprintf(stderr, "\n");

    fprintf(stderr, "\nWorking directory:\n%s\n", g_curdir ? g_curdir : "'null'");
}

void sd_print_environment()
{
    fprintf(stderr, "\nEnvironment variables:\n");
    for (char** env = environ; *env; ++env)
        fprintf(stderr, "%s\n", *env);
}
