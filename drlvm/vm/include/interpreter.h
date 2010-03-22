/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
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
 * @file
 * The interpreter description.
 *
 * The interpreter component executes the bytecode and is used in the VM
 * interchangeably with the JIT compiler. In the current implementation,
 * the interpreter is mainly used to simplify debugging. The interpreter also
 * enables VM portability, since most of its code is platform-independent.*/
#ifndef _INTERPRETER_H_
#define _INTERPRETER_H_

#include "Class.h"
#include "stack_trace.h"
#include "interpreter_exports.h"

/** Returns <code>TRUE</code> if the interpreter is enabled.
 *
 * @return <code>TRUE</code> on success.*/
extern bool interpreter_enabled();

/** If the interpreter is enabled, aborts the execution.*/
#define ASSERT_NO_INTERPRETER assert(!interpreter_enabled());

#endif
