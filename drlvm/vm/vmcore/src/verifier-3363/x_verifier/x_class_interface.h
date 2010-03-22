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
#ifndef __X_CLASS_INTERFACE_H__
#define __X_CLASS_INTERFACE_H__

#include "open/types.h"
#include "open/common.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Returns constant pool index of CONSTANT_Class for the given class name
 * If such entry is not present in the constant pool, the function adds this entry
 *
 * @param k_class   - the class handle
 * @param name      - name of the class to return constant pool index for
 *
 * @return constant pool index of the CONSTANT_Class entry with a given
 * <code>name</code>
 */
DECLARE_OPEN(unsigned short, class_cp_get_class_entry, (Class_Handle k_class, const char* name));

/**
 * Removes given exception handler (handlers with greater indexes shift)
 *
 * @param method    - method to remove exception handler from
 * @param idx       - index of exception handler in exception handlers array
 */
DECLARE_OPEN(void, method_remove_exc_handler, (Method_Handle method, unsigned short idx));

/**
 * Modifies particular exception handler information.
 *
 * @param method    - method in which exception handler must be updated
 * @param idx       - index of exception handler to update
 * @param start_pc  - new start pc
 * @param end_pc    - new end pc
 * @param handler_pc - pc of the beginning of the exception handler
 * @param handler_cp_index - index in the constant pool of the class
 *                           of the handled exception
 *
 * @note Usually it is only needed to modify start_pc and end_pc
 */
DECLARE_OPEN(void, method_modify_exc_handler_info,
    (Method_Handle method, unsigned short idx,
     unsigned short start_pc, unsigned short end_pc,
     unsigned short handler_pc, unsigned short handler_cp_index));

#ifdef __cplusplus
}
#endif

#endif
