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
#include "assertion_registry.h"

void Assertion_Registry::add_class(const Global_Env* genv, const char* name, unsigned len, bool value) {
    Assertion_Record* rec = (Assertion_Record*)
        apr_palloc(genv->mem_pool, sizeof(Assertion_Record) + len * sizeof(char));
    rec->status = value;
    rec->len = len;
    strncpy(rec->name, name, len);
    rec->name[len] = '\0';
    rec->next = classes;
    classes = rec;
}

void Assertion_Registry::add_package(const Global_Env* genv, const char* name, unsigned len, bool value) {
    Assertion_Record* rec = (Assertion_Record*)
        apr_palloc(genv->mem_pool, sizeof(Assertion_Record) + len * sizeof(char));
    rec->status = value;
    rec->len = len;
    strncpy(rec->name, name, len);
    rec->name[len] = '\0';
    rec->next = packages;
    packages = rec;
}

Assertion_Status Assertion_Registry::get_class_status(const char* name) const {
    for (Assertion_Record* it = classes; it != NULL; it = it->next) {
        if (strcmp(name, it->name) == 0) {
            return it->status ? ASRT_ENABLED : ASRT_DISABLED;
        }
    }
    return ASRT_UNSPECIFIED;
}

Assertion_Status Assertion_Registry::get_package_status(const char* name) const {
    unsigned best_len = 0;
    Assertion_Status status = ASRT_UNSPECIFIED;
    for (Assertion_Record* it = packages; it != NULL; it = it->next) {
        unsigned len = it->len;
        if (len == 0 && !strchr(name, '.')) {
            //have a match for default package
            return it->status ? ASRT_ENABLED : ASRT_DISABLED;
        }
        if ((strncmp(name, it->name, len) == 0) && ('.' == name[len])) {
            if (len > best_len) {
                best_len = len;
                status = it->status ? ASRT_ENABLED : ASRT_DISABLED;
            }
        }
    }
    return status;
}
