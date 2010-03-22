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
#include "logparams.h"
#include "logger.h"
#include "hyport.h"

const char* LogParams::release() {
    HyPortLibrary *portlib = log_get_portlib();
    if (portlib) {
        messageId = (char*) portlib->nls_lookup_message(portlib,
            HYNLS_DO_NOT_PRINT_MESSAGE_TAG | HYNLS_DO_NOT_APPEND_NEWLINE,
            prefix, message_number, def_messageId);
        messageId = portlib->buf_write_text(portlib,
            (const char *)messageId, (IDATA) strlen(messageId));
    } else {
        messageId = def_messageId;
    }

    int i = 0;
    while(messageId[i] != '\0') {
        if (messageId[i] == '{' && messageId[i + 1] >= '0' &&
            messageId[i + 1] <= '9' && messageId[i + 2] == '}') {
                int arg = messageId[i + 1] - '0';
                result_string += values[arg];
                i += 3;
        } else {
            result_string += messageId[i];
            i++;
        }
    }
    if (portlib) {
        portlib->mem_free_memory(portlib, (void*)messageId);
    }
    return (const char*)result_string.c_str();
}

