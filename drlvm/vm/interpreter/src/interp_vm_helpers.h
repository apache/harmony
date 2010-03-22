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
 * Defines helper interfaces used by the interpreter.
 *
 * The current DRLVM implementation describes the following helper interfaces:
 * <ul>
 * <li>resolving handling functionality</li>
 * <li>exceptions handling functionality</li>
 * </ul>
 */

#include "vm_core_types.h"
#include "Class.h"

/**
 * Sets the thread-local exception with the name <code>exc</code>.
 *
 * @param[in] exc - the exception name
 */
void interp_throw_exception(const char* exc);

/**
 * Sets the thread-local exception with the names <code>exc</code> and
 * <code>message</code>.
 *
 * @param[in] exc     - the exception name
 * @param[in] message - the message
 */
void interp_throw_exception(const char* exc, const char *message);

/**
 * Looks up the implementation for the native method.
 *
 * @param[in] method - the searching native-method structure
 * @return The pointer to find the native function.
 */
GenericFunctionPointer interp_find_native(Method_Handle method);

/**
 * Resolves the class in the <code>clazz</code> constant pool with the index
 * <code>classId</code>. Throws an exception if a resolution error occurs.
 *
 * @param[in] clazz   - the class which constant pool contains the reference to
 *                      the target class
 * @param[in] classId - the constant-pool index
 * @return A resolved class, if a resolution attempt succeeds.
 */
Class* interp_resolve_class(Class *clazz, int classId);

/**
 * Resolves the class suitable for a new operation in the <code>clazz</code>
 * constant pool with the index <code>classId</code>. Throws an exception if
 * a resolution error occurs.
 *
 * @param[in] clazz   - the class which constant pool contains the reference to
 *                      the target class
 * @param[in] classId - the constant-pool index
 * @return A resolved class, if a resolution attempt succeeds.
 */
Class* interp_resolve_class_new(Class *clazz, int classId);

/**
 * Resolves the static field in the <code>clazz</code> constant pool with the
 * index <code>fieldId</code>. Throws an exception if a resolution error occurs.
 *
 * @param[in] clazz    - the class which constant pool contains the reference
 *                       to the target field
 * @param[in] fieldId  - the constant-pool index
 * @param[in] putfield - location where to put/get the field
 * @return A resolved field, if a resolution attempt succeeds.
 */
Field* interp_resolve_static_field(Class *clazz, int fieldId, bool putfield);

/**
 * Resolves the nonstatic field in the <code>clazz</code> constant pool with the
 * index <code>fieldId</code>. Throws an exception if a resolution error occurs.
 *
 * @param[in] clazz    - the class which constant pool contains the reference
 *                       to the target field
 * @param[in] fieldId  - the constant-pool index
 * @param[in] putfield - location where to put/get thee field
 * @return A resolved field, if a resolution attempt succeeds.
 */
Field* interp_resolve_nonstatic_field(Class *clazz, int fieldId, bool putfield);

/**
 * Resolves the virtual method in the <code>clazz</code> constant pool with the
 * index <code>methodId</code>. Throws an exception if a resolution error occurs.
 *
 * @param[in] clazz       - the class which constant pool contains the reference
 *                          to the method
 * @param[in] methodId    - the constant-pool index
 * @return A resolved method, if a resolution attempt succeeds.
 */
Method* interp_resolve_virtual_method(Class *clazz, int methodId);

/**
 * Resolves the interface method in the <code>clazz</code> constant pool with
 * the index <code>methodId</code>. Throws an exception if a resolution error
 * occurs.
 *
 * @param[in] clazz    - the class which constant pool contains the reference
 *                       to the method
 * @param[in] methodId - the constant-pool index
 * @return A resolved method, if a resolution attempt succeeds.*/
Method* interp_resolve_interface_method(Class *clazz, int methodId);

/**
 * Resolves the static method in the <code>clazz</code> constant pool with the
 * index <code>methodId</code>. Throws an exception if a resolution error
 * occurs.
 *
 * @param[in] clazz    - the class which constant pool contains the reference
 *                       to the method
 * @param[in] methodId - the constant-pool index
 * @return A resolved method, if a resolution attempt succeeds.
 */
Method* interp_resolve_static_method(Class *clazz, int methodId);

/**
 * Resolves the special method in the <code>clazz</code> constant pool with the
 * index <code>methodId</code>. Throws an exception if a resolution error occurs.
 *
 * @param[in] clazz    - the class which constant pool contains the reference
 *                       to the method
 * @param[in] methodId - the constant pool index
 * @return A resolved method, if a resolution attempt succeeds.*/
Method* interp_resolve_special_method(Class *clazz, int methodId);

/**
 * Resolves the class array for specified <code>objClass</code>.
 *
 * @param[in] objClass - specified <code>objClass</code>
 * @return A resolved array if a resolution attempt succeeds.
 */
Class* interp_class_get_array_of_class(Class *objClass);
