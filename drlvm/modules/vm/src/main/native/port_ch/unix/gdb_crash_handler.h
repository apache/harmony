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
 * @author Intel, Evgueni Brevnov
 */  

#ifndef _CRASH_HANDLER_H
#define _CRASH_HANDLER_H

#include "open/platform_types.h"

/**
 * \file
 * Provides definition needed to install gdb crash handler.
 */

/**
 * Initializes the static state needed for gdb crash handler.
 *
 * @return <code>true</code> on success or <code>false</code> on failure
 */
bool init_gdb_crash_handler();

/**
 * Initializes the static state needed for gdb crash handler.
 */
void cleanup_gdb_crash_handler();

/**
 * Invokes gdb.
 *
 * @return true on success or false on failure
 */
bool gdb_crash_handler(Registers* regs);

#endif // _CRASH_HANDLER_H
