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

#ifndef __INLINE_INFO_H__
#define __INLINE_INFO_H__

#include <vector>
#include "open/types.h"
#include "open/rt_types.h"
#include "lock_manager.h"

struct Method;

/**
 * Information about methods inlined to a given method.
 * Instance of this class holds a collection of Entry objects.
 */
class InlineInfo
{
public:
    /**
     * Creates InlineInfo instance.
     */
    InlineInfo();

    /**
     * Adds information about inlined method.
     * @param[in] method - method which is inlined
     * @param[in] codeSize - size of inlined code block
     * @param[in] codeAddr - size of inlined code block
     * @param[in] mapLength - number of AddrLocation elements in addrLocationMap
     * @param[in] addrLocationMap - native addresses to bytecode locations
     *       correspondence table
     */
    void add(Method* method, U_32 codeSize, void* codeAddr, U_32 mapLength, 
            AddrLocation* addrLocationMap);

    /**
     * Sends JVMTI_EVENT_COMPILED_METHOD_LOAD event for every inline method 
     * recorded in this InlineInfo object.
     * @param[in] method - outer method this InlineInfo object belogs to.
     */
    void send_compiled_method_load_event(Method *method);

private:
    /**
     * Describes one inlined method code block.
     */
    struct Entry
    {
        Method* method;
        U_32 codeSize;
        void* codeAddr;
        U_32 mapLength;
        AddrLocation* addrLocationMap;
    };

    typedef std::vector<Entry>::iterator iterator;

    std::vector<Entry> _entries;
    Lock_Manager _lock;
};

#endif
