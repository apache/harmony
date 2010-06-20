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
 * @author Petr Ivanov
 */

#include <memory.h>
#include <string.h>
#include "ncai_utils.h"
#include "ncai_internal.h"


char* ncai_parse_module_name(char* filepath)
{
    size_t length = 0;
    char* filename = strrchr(filepath, '\\');

    if (filename)
    {
        filename = filename + 1;
        length = strlen(filename);
        size_t dot_len = length - 4;
        char* dot_pos = filename + dot_len;

        if (strcmp(dot_pos, ".dll") == 0 ||
            strcmp(dot_pos, ".exe") == 0)
        {
            length = dot_len;
        }
    }
    else
    {
        filename = filepath;
        length = strlen(filename);
    }

    char* ret = (char*)ncai_alloc(length + 1);
    if (ret)
    {
        memcpy(ret, filename, length);
        ret[length] = '\0';
    }

    return ret;
}

