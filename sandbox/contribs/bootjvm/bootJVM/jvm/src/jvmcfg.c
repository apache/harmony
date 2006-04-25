/*!
 * @file jvmcfg.c
 *
 * @brief Real machine constant types convenient for C/C++ source code.
 *
 *
 * @section Control
 *
 * \$URL$
 *
 * \$Id$
 *
 * Copyright 2005 The Apache Software Foundation
 * or its licensors, as applicable.
 *
 * Licensed under the Apache License, Version 2.0 ("the License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * @version \$LastChangedRevision$
 *
 * @date \$LastChangedDate$
 *
 * @author \$LastChangedBy$
 *
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

#include "arch.h"
ARCH_SOURCE_COPYRIGHT_APACHE(jvmcfg, c,
"$URL$",
"$Id$");


#define I_AM_JVMCFG_C /* Permit xxx_NULL_xxx definition constants */
#include "jvmcfg.h"
#include "classfile.h"


/*! @brief Real machine NULL thread index */
const jvm_thread_index jvm_thread_index_null = JVMCFG_NULL_THREAD;

/*! @brief Real machine NULL constant pool index */
const jvm_constant_pool_index jvm_constant_pool_index_null =
                                              CONSTANT_CP_DEFAULT_INDEX;

/*! @brief Real machine NULL interface table index */
const jvm_interface_index jvm_interface_index_bad =JVMCFG_BAD_INTERFACE;

/*! @brief Real machine NULL class index */
const jvm_class_index jvm_class_index_null = JVMCFG_NULL_CLASS;

/*!
 * @brief Real machine BAD method index (or interface method index)
 * in class
 */
const jvm_method_index jvm_method_index_bad    = JVMCFG_BAD_METHOD;

/*! @brief Real machine BAD field index in class */
const jvm_field_index jvm_field_index_bad = JVMCFG_BAD_FIELD;

/*! @brief Real machine BAD field lookup index in class */
const jvm_field_lookup_index jvm_field_lookup_index_bad =
                                                JVMCFG_BAD_FIELD_LOOKUP;

/*!
 * @brief Real machine BAD attribute index in class
 */
const jvm_attribute_index jvm_attribute_index_bad =JVMCFG_BAD_ATTRIBUTE;

/*!
 * @brief Real machine BAD annotation type index in annotation
 */
const jvm_annotation_type_index jvm_attribute_type_index_bad =
                                             JVMCFG_BAD_ANNOTATION_TYPE;

/*!
 * @brief Real machine BAD element value index in annotation type
 */
const jvm_element_value_pair_index jvm_element_value_pair_index_bad =
                                          JVMCFG_BAD_ELEMENT_VALUE_PAIR;

/*!
 * @brief Real machine NATIVE (method) attribute index
 */
const jvm_attribute_index jvm_attribute_index_native =
                                         JVMCFG_NATIVE_METHOD_ATTRIBUTE;

/*!
 * @brief Real machine NULL ordinal for local native method
 */
const jvm_native_method_ordinal jvm_native_method_ordinal_null =
                                               JVMCFG_JLOBJECT_NMO_NULL;

/*!
 * @brief Real machine registration ordinal for local native method
 */
const jvm_native_method_ordinal jvm_native_method_ordinal_register =
                                           JVMCFG_JLOBJECT_NMO_REGISTER;

/*!
 * @brief Real machine un-registration ordinal for local native method
 */
const jvm_native_method_ordinal jvm_native_method_ordinal_unregister =
                                         JVMCFG_JLOBJECT_NMO_UNREGISTER;

/*! @brief Real machine BAD program counter value in class */
jvm_pc_offset jvm_pc_offset_bad = CODE_CONSTRAINT_CODE_LENGTH_MAX;

/*! @brief Real machine BAD unicode string index in class */
const jvm_unicode_string_index jvm_unicode_string_index_bad =
                                              JVMCFG_BAD_UNICODE_STRING;

/*! @brief Real machine NULL object hash */
const jvm_object_hash jvm_object_hash_null = JVMCFG_NULL_OBJECT;


/* EOF */
