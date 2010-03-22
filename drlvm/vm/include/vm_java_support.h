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

#ifndef _JAVA_SUPPORT_INTF_H_
#define _JAVA_SUPPORT_INTF_H_


#include "open/types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
* (? 20030317) These defines are deprecated.
* Use <code>VM_Data_Type</code> in all new code.
*/
#define Java_Type           VM_Data_Type
#define JAVA_TYPE_BYTE      VM_DATA_TYPE_INT8
#define JAVA_TYPE_CHAR      VM_DATA_TYPE_CHAR
#define JAVA_TYPE_DOUBLE    VM_DATA_TYPE_F8
#define JAVA_TYPE_FLOAT     VM_DATA_TYPE_F4
#define JAVA_TYPE_INT       VM_DATA_TYPE_INT32
#define JAVA_TYPE_LONG      VM_DATA_TYPE_INT64
#define JAVA_TYPE_SHORT     VM_DATA_TYPE_INT16
#define JAVA_TYPE_BOOLEAN   VM_DATA_TYPE_BOOLEAN
#define JAVA_TYPE_CLASS     VM_DATA_TYPE_CLASS
#define JAVA_TYPE_ARRAY     VM_DATA_TYPE_ARRAY
#define JAVA_TYPE_VOID      VM_DATA_TYPE_VOID
#define JAVA_TYPE_STRING    VM_DATA_TYPE_STRING
#define JAVA_TYPE_INVALID   VM_DATA_TYPE_INVALID
#define JAVA_TYPE_END       VM_DATA_TYPE_END


VMEXPORT Java_Type    field_get_type(Field_Handle f);
VMEXPORT Java_Type    method_get_return_type(Method_Handle m);


typedef const void *Arg_List_Iterator;       // Java only
VMEXPORT Arg_List_Iterator  method_get_argument_list(Method_Handle m);
VMEXPORT Java_Type          curr_arg(Arg_List_Iterator iter);
VMEXPORT Class_Handle       get_curr_arg_class(Arg_List_Iterator iter,
                                               Method_Handle m);
VMEXPORT Arg_List_Iterator  advance_arg_iterator(Arg_List_Iterator iter);

VMEXPORT unsigned     class_number_implements(Class_Handle ch);
VMEXPORT Class_Handle class_get_implements(Class_Handle ch, unsigned idx);

/**
* @return <code>TRUE</code> if this a Java method. Every Java JIT must call this
*         function before compiling a method and return <code>JIT_FAILURE</code> if
*         <code>method_is_java</code> returned <code>FALSE</code>.
*/ 
VMEXPORT Boolean method_is_java(Method_Handle mh);
VMEXPORT unsigned     field_get_flags(Field_Handle f);
VMEXPORT unsigned     class_get_flags(Class_Handle cl);

void
class_throw_linking_error(Class_Handle ch, unsigned cp_index, unsigned opcode);


#ifdef __cplusplus
}
#endif

#endif //_JAVA_SUPPORT_INTF_H_
