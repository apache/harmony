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
#include "mon_enter_exit.h"
#include "interpreter.h"
#include "interpreter_imports.h"
#include "jit_intf.h"
#include "vm_threads.h"

VMEXPORT void vm_monitor_enter_wrapper(ManagedObject *obj) {
    vm_monitor_enter(obj);
}

VMEXPORT void vm_monitor_exit_wrapper(ManagedObject *obj) {
    vm_monitor_exit(obj);
}

VMEXPORT void class_throw_linking_error_for_interpreter(Class_Handle ch,
        unsigned index, unsigned opcode) {
    class_throw_linking_error(ch, index, opcode);
}

VMEXPORT JNIEnv * get_jni_native_intf() {
    return p_TLS_vmthread->jni_env;
}


