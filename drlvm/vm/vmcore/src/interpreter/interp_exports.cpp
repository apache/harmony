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
 * @author Ivan Volosyuk
 */  
#include <stdlib.h>
#include "open/vm_properties.h"
#include "mon_enter_exit.h"
#include "interpreter.h"
#include "interpreter_exports.h"
#include "cxxlog.h"

char const * * opcodeNames = 0;
#define JVMTI_NYI 0

static bool interp_enabled = false;
Interpreter interpreter;

VMEXPORT Interpreter *interpreter_table() {
    interp_enabled = true;
    return &interpreter;
}

bool interpreter_enabled(void) {
    static bool inited = false;
    static bool val;
    if (!inited) {
        val = interp_enabled && 
            vm_property_get_boolean("vm.use_interpreter", FALSE, VM_PROPERTIES);
        inited = true;
        INFO2("init", "Use interpreter = " << val);
    }
    return val;
}

