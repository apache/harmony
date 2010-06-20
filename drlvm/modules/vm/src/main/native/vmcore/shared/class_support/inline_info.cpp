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

#define LOG_DOMAIN "method.inline"
#include "cxxlog.h"

#include "inline_info.h"
#include "class_member.h"
#include "jvmti_direct.h"

InlineInfo::InlineInfo(): _entries(0)
{}

void InlineInfo::add(Method* method, U_32 codeSize, void* codeAddr, 
                     U_32 mapLength, AddrLocation* addrLocationMap)
{
    Entry entry = {method, codeSize, codeAddr, mapLength, addrLocationMap};
 
    LMAutoUnlock au(& _lock);

    TRACE("Adding Inlined method: " << method->get_class()->get_name()->bytes 
            << "." << method->get_name()->bytes 
            << " " << method->get_descriptor()->bytes 
            << "\taddress: " << codeAddr 
            << " [" << codeSize << "]\t" 
            << "mapLength: " << mapLength);

   _entries.push_back(entry);
} // InlineInfo::add

void InlineInfo::send_compiled_method_load_event(Method *method)
{
    LMAutoUnlock au(& _lock);

    if(jvmti_should_report_event(JVMTI_EVENT_COMPILED_METHOD_LOAD)) {
        for (iterator i = _entries.begin(); i != _entries.end(); i++) {
            Entry& e = *i;
            jvmti_send_region_compiled_method_load_event(e.method, e.codeSize,
                    e.codeAddr, e.mapLength, 
                    e.addrLocationMap, NULL);
        }
    }

} // InlineInfo::send_compiled_method_load_event
