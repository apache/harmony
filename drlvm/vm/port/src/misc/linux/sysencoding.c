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
#include <string.h>

int port_get_utf8_converted_system_message_length(char *system_message)
{
    return strlen(system_message) + 1;
}

void port_convert_system_error_message_to_utf8(char *converted_message,
    int buffer_size,
    char *system_message)
{
    strncpy(converted_message, system_message, buffer_size - 1);
    converted_message[buffer_size - 1] = '\0';
}
