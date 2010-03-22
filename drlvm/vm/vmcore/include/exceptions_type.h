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
 * @author Pavel Afremov
 */

// This describes the core VM interface to exception manipulation, throwing, and catching

#ifndef _INTERFACE_EXCEPTIONS_TYPE_H_
#define _INTERFACE_EXCEPTIONS_TYPE_H_

#include "open/types.h"

#ifdef __cplusplus
extern "C" {
#endif

struct Class;
struct Exception {
    struct ManagedObject* exc_object;
    struct Class* exc_class;
    const char* exc_message;
    struct ManagedObject* exc_cause;
};

#ifdef __cplusplus
}
#endif
#endif // !_INTERFACE_EXCEPTIONS_TYPE_H_
