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

#ifndef _PORT_SYSENCODING_H_
#define _PORT_SYSENCODING_H_

/**
 * @defgroup port_sysencoding Convert system messages to UTF8
 * @ingroup port_apr
 * @{
 */

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Returns the length of buffer needed to hold NULL terminated UTF8
 * string converted from system encoded message.
 *
 * @param system_message - pointer to NULL terminated string in system encoding
 * @return - number of bytes needed to hold the converted string including final
 * zero byte
 */
int port_get_utf8_converted_system_message_length(char *system_message);

/**
 * Converts system message from system encoding to UTF8
 *
 * @param[out] converted_message - pointer to buffer to hold converted message
 * @param buffer_size - number of bytes in the converted_message buffer
 * @param system_message - pointer to NULL terminated string in system encoding
 */
void port_convert_system_error_message_to_utf8(char *converted_message,
    int buffer_size,
    char *system_message);

#ifdef __cplusplus
}
#endif

#endif // _PORT_SYSENCODING_H_
