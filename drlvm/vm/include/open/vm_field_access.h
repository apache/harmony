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
 * @author Intel, Pavel Pervov
 */
#ifndef _VM_FIELD_ACCESS_H
#define _VM_FIELD_ACCESS_H

#include "common.h"
#include "hycomp.h"
#include "types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file
 * Part of Class Support interface related to retrieving different
 * properties of fields contained in class
 */

/**
 * Returns the name of the field.
 *
 * @param field - the field handle
 *
 * @return The name of the field.
 */
DECLARE_OPEN(const char*, field_get_name, (Field_Handle field));

/**
 * Returns the field <i>descriptor</i>.
 * 
 * The descriptor is a string representation of the field types as
 * defined by the JVM specification.
 *
 * @param field - the field handle
 *
 * @return The field descriptor.
 */
DECLARE_OPEN(const char*, field_get_descriptor, (Field_Handle field));

/**
 * Returns the class that defined the given field.
 *
 * @param field - the field handle
 *
 * @return The class that defined the field.
 */
DECLARE_OPEN(Class_Handle, field_get_class, (Field_Handle field));

/**
 * Returns the address of the given static field.
 *
 * @param field - the field handle
 *
 * @return The address of the static field.
 */
DECLARE_OPEN(void*, field_get_address, (Field_Handle field));

/**
 * Returns the offset to the given instance field.
 *
 * @param field - the field handle
 *
 * @return The offset to the instance field.
 */
DECLARE_OPEN(unsigned, field_get_offset, (Field_Handle field));

/**
 * Returns the type info that represents the type of the field.
 *
 * @param field - the field handle
 *
 * @return Type information.
 */
DECLARE_OPEN(Type_Info_Handle, field_get_type_info, (Field_Handle field));

/**
 * Returns the class that represents the type of the field.
 *
 * @param field - the field handle
 *
 * @return the class that represents the type of the field.
 */
DECLARE_OPEN(Class_Handle, field_get_class_of_field_type, (Field_Handle field));

/**
 *  Checks whether the field is final.
 *
 * @param field - the field handle
 *
 * @return <code>TRUE</code> if the field is final.
 *
 * #note Extended
 */
DECLARE_OPEN(BOOLEAN, field_is_final, (Field_Handle field));

/**
 *  Checks whether the field is static.
 *
 * @param field - the field handle
 *
 * @return <code>TRUE</code> if the field is static; otherwise, <code>FALSE</code>. 
 *
 * @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, field_is_static, (Field_Handle field));

/**
 *  Checks whether the field is private.
 *
 * @param field - the field handle
 *
 * @return <code>TRUE</code> if the field is private; otherwise, <code>FALSE</code>. 
 *
 * @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, field_is_private, (Field_Handle field));

/**
 *  Checks whether the field is protected.
 *
 * @param field - the field handle
 *
 * @return <code>TRUE</code> if the field is protected; otherwise, <code>FALSE</code>. 
 *
 * @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, field_is_protected, (Field_Handle field));

/**
 *  Checks whether the field is public.
 *
 * @param field - the field handle
 *
 * @return <code>TRUE</code> if the field is public; otherwise, <code>FALSE</code>. 
 *
 * @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, field_is_public, (Field_Handle field));

/**
 *  Checks whether the field is volatile.
 *
 * @param field - the field handle
 *
 * @return <code>TRUE</code> if the field is volatile; otherwise, <code>FALSE</code>. 
 *
 * @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, field_is_volatile, (Field_Handle field));

/**
 *  Checks whether the field is reference field.
 *
 * @param field - the field handle
 *
 * @return <code>TRUE</code> if the field is reference.
 *
 * #note Extended
 */
DECLARE_OPEN(BOOLEAN, field_is_reference, (Field_Handle field));

/**
 *  Checks whether the field is literal.
 *
 * @param field - the field handle
 *
 * @return <code>TRUE</code> if the field is literal.
 */
DECLARE_OPEN(BOOLEAN, field_is_literal, (Field_Handle field));

/**
 *  Checks whether the field is injected.
 *
 * @param field - the field handle
 *
 * @return <code>TRUE</code> if the field is injected; otherwise, <code>FALSE</code>. 
 */
DECLARE_OPEN(BOOLEAN, field_is_injected, (Field_Handle field));

/**
 * @return <code>TRUE</code> if the field is a magic type field
 *
 * This function doesn't cause resolution of the class of the field.
 */
DECLARE_OPEN(BOOLEAN, field_is_magic, (Field_Handle fh));

/**
 * @return The address and bit mask, for the flag which determine whether field
 *         access event should be sent. JIT may use the following expression to
 *         determine if specified field access should be tracked:
 *         ( **address & *mask != 0 )
 *
 * @param field         - handle of the field
 * @param[out] address  - pointer to the address of the byte which contains the flag
 * @param[out] mask     - pointer to the bit mask of the flag
 */
DECLARE_OPEN(void, field_get_track_access_flag, (Field_Handle field, char** address, char* mask));

/**
 * @return the address and bit mask, for the flag which determine whether field
 *         modification event should be sent. JIT may use the following expression to
 *         determine if specified field modification should be tracked:
 *         ( **address & *mask != 0 )
 *
 * @param field         - handle of the field
 * @param[out] address  - pointer to the address of the byte which contains the flag
 * @param[out] mask     - pointer to the bit mask of the flag
 */
DECLARE_OPEN(void, field_get_track_modification_flag, (Field_Handle field, char** address, char* mask));

}

#endif // _VM_FIELD_ACCESS_H
