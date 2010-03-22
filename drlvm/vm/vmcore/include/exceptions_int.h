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
 * @author Intel, Pavel Afremov
 */
#ifndef _EXCEPTIONS_INT_H_
#define _EXCEPTIONS_INT_H_

#include "open/types.h"

VMEXPORT void clear_current_thread_exception();
VMEXPORT bool check_current_thread_exception();
VMEXPORT ManagedObject* get_current_thread_exception();
VMEXPORT void set_current_thread_exception(ManagedObject* obj);

#endif // _EXCEPTIONS_INT_H_
