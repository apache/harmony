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
#ifndef _VM_CLASS_INFO_H
#define _VM_CLASS_INFO_H

#include "open/types.h"
/**
 * @file
 * Part of Class Support interface related to inquiring class data.
 * These functions do not have any side effect, such as  
 * classloading or resolving of constant pool entries. 
 * So they are safe for using out of execution context, 
 * e.g. for asynchronous compilation.
 */

#ifdef __cplusplus
extern "C" {
#endif


/**
 * @return A UTF8 representation of a string declared in a class.
 *
 * The <code>idx</code> parameter is interpreted as a constant pool 
 * index for JVM.
 * This method is generally only for JIT internal use,
 * e.g. printing a string pool constant in a bytecode disassembler.
 * The resulting const char* should of course not be inserted into
 * the jitted code.
 */
DECLARE_OPEN(const char *, class_cp_get_const_string, (Class_Handle ch, U_16 idx));

/**
* @return The data type for field in constant pool entry.
* 
* The <code>idx</code> parameter is interpreted as a constant pool index 
* for JVM.
*/
DECLARE_OPEN(VM_Data_Type, class_cp_get_field_type, (Class_Handle src_class, U_16 idx));


DECLARE_OPEN(Method_Handle, class_lookup_method_recursively,
                    (Class_Handle clss,
                    const char *name,
                    const char *descr));

DECLARE_OPEN(U_16, class_number_fields, (Class_Handle ch));
DECLARE_OPEN(Field_Handle, class_get_field, (Class_Handle ch, U_16 idx));

DECLARE_OPEN(Field_Handle, class_get_field_by_name, (Class_Handle ch, const char* name));
DECLARE_OPEN(Method_Handle, class_get_method_by_name, (Class_Handle ch, const char* name));

DECLARE_OPEN(const char *, class_get_source_file_name, (Class_Handle cl));

/**
* @return The name of the class.
*/
DECLARE_OPEN(const char *, class_get_name, (Class_Handle ch));

/**
* @return The name of the package containing the class.
*/
DECLARE_OPEN(const char *, class_get_package_name, (Class_Handle ch));

/**
* The super class of the current class. 
* @return <code>NULL</code> for the system object class, i.e.
*         <code>class_get_super_class</code>(vm_get_java_lang_object_class()) == NULL
*/
DECLARE_OPEN(Class_Handle, class_get_super_class, (Class_Handle ch));

/** 
* @return <code>TRUE</code> if class <code>s</code> is assignment 
* compatible with class <code>t</code>.
*/ 
DECLARE_OPEN(BOOLEAN, class_is_instanceof, (Class_Handle s, Class_Handle t));

/**
* @return <code>TRUE</code> if the class is already fully initialized.
*/
DECLARE_OPEN(BOOLEAN, class_is_initialized, (Class_Handle ch));

/**
* @return <code>TRUE</code> if the class represents an enum. 
*         For Java 1.4 always returns <code>FALSE</code>.
*/
DECLARE_OPEN(BOOLEAN, class_is_enum, (Class_Handle ch));

/**
* @return <code>TRUE</code> if the class represents a primitive type (int, float, etc.)
*/
DECLARE_OPEN(BOOLEAN, class_is_primitive, (Class_Handle ch));

/** 
* @return <code>TRUE</code> is the class is an array.
*/
DECLARE_OPEN(BOOLEAN, class_is_array, (Class_Handle ch));

/**
* @return <code>TRUE</code> if this is an array of primitives.
*/
DECLARE_OPEN(BOOLEAN, class_is_non_ref_array, (Class_Handle ch));

DECLARE_OPEN(BOOLEAN, class_is_final, (Class_Handle ch));
DECLARE_OPEN(BOOLEAN, class_is_abstract, (Class_Handle ch));
DECLARE_OPEN(BOOLEAN, class_is_interface, (Class_Handle ch));

/**
* @return <code>TRUE</code> if the class is likely to be used as an exception object.
*         This is a hint only. If the result is <code>FALSE</code>, the class may still
*         be used for exceptions but it is less likely.
*/
DECLARE_OPEN(BOOLEAN, class_is_throwable, (Class_Handle ch));
/**
* @return <code>TRUE</code> if the class has a non-trivial finalizer.
*/
DECLARE_OPEN(BOOLEAN, class_is_finalizable, (Class_Handle ch));

#ifdef __cplusplus
}
#endif

#endif // _VM_CLASS_INFO_H
