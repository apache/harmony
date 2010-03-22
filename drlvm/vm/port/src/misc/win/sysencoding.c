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

#include "port_sysencoding.h"
#include <windows.h>

int port_get_utf8_converted_system_message_length(char *system_message)
{
    return WideCharToMultiByte(CP_UTF8, 0, (LPCWSTR)system_message,
        -1, NULL, 0, NULL, NULL);
}

void port_convert_system_error_message_to_utf8(char *converted_message,
    int buffer_size,
    char *system_message)
{
    WideCharToMultiByte(CP_UTF8, 0, (LPCWSTR)system_message, -1,
        (LPSTR)converted_message, buffer_size, NULL, NULL);
}
