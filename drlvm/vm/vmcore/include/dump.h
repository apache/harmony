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
* @author Evgueni Brevnov
*/  

#ifndef _DUMP_H_
#define _DUMP_H_

#include <stddef.h>

#ifndef NDEBUG
    extern bool dump_stubs;

    #define DUMP_STUB(addr, name, len) \
    if (dump_stubs) \
         dump(((char *)addr), (name), (len));
#else
    #define DUMP_STUB(addr, name, len)
#endif

int dump(const char * code, const char * name, size_t length);

#endif // _DUMP_H_
