#ifndef _field_h_included_
#define _field_h_included_

/*!
 * @file field.h
 *
 * @brief Field management functions for the JVM.
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

ARCH_HEADER_COPYRIGHT_APACHE(field, h,
"$URL$",
"$Id$");


#include "jvalue.h"


/*!
 * @def FIELD
 * @brief Access structures of a class' field table at certain index.
 *
 * Each class has a table of fields, divided into class static fields
 * and object instance fields.  This macro references one of
 * them using the @b clsidx index for the class and @b fldidx for 
 * the field table entry in that class.
 *
 * @param clsidx  Class table index into the global
 *                @link #rjvm.class rjvm.class[]@endlink array (via
 *                @link #pjvm pjvm->class[]@endlink).
 * 
 * @param fldidx  Index into method table for this class.
 * 
 * @returns pointer to a field table entry
 * 
 */
#define FIELD(clsidx, fldidx) \
    (CLASS_OBJECT_LINKAGE(clsidx)->pcfs->fields[fldidx])


/* Prototypes for functions in 'field.h' */

extern jvm_field_index field_find_by_cp_entry(jvm_class_index   clsidx,
                                             cp_info_mem_align *fldname,
                                             cp_info_mem_align *flddesc);

extern rboolean field_index_is_class_static(jvm_class_index clsidx,
                                            jvm_field_index fldidx);

extern rboolean field_name_is_class_static(jvm_class_index    clsidx,
                                           cp_info_mem_align *fldname,
                                           cp_info_mem_align *flddesc);

extern rboolean field_index_is_object_instance(jvm_class_index clsidx,
                                               jvm_field_index fldidx);

extern rboolean field_name_is_object_instance(jvm_class_index  clsidx,
                                            cp_info_mem_align *fldname,
                                            cp_info_mem_align *flddesc);

extern jvm_field_lookup_index
    field_index_get_class_static_lookup(jvm_class_index clsidx,
                                        jvm_field_index fldidx);

extern jvm_field_lookup_index
    field_name_get_class_static_lookup(jvm_class_index    clsidx,
                                       cp_info_mem_align *fldname,
                                       cp_info_mem_align *flddesc);

extern jvm_field_lookup_index
    field_index_get_object_instance_lookup(jvm_class_index clsidx,
                                           jvm_field_index fldidx);

extern jvm_field_lookup_index
    field_name_get_object_instance_lookup(jvm_class_index    clsidx,
                                          cp_info_mem_align *fldname,
                                          cp_info_mem_align *flddesc);

extern jvalue *field_index_get_class_static_pjvalue(
                   jvm_class_index  clsidx,
                   jvm_field_index fldidx);

extern jvalue *field_name_get_class_static_pjvalue(
                   jvm_class_index    clsidx,
                   cp_info_mem_align *fldname,
                   cp_info_mem_align *flddesc);

extern jvalue *field_index_get_object_instance_pjvalue(
                   jvm_object_hash objhash,
                   jvm_field_index fldidx);

extern jvalue *field_name_get_object_instance_pjvalue(
                   jvm_object_hash    objhash,
                   cp_info_mem_align *fldname,
                   cp_info_mem_align *flddesc);

extern jvm_field_index field_index_put_class_static_pjvalue(
                   jvm_class_index  clsidx,
                   jvm_field_index  fldidx,
                   jvalue          *_jvalue);

extern jvm_field_index field_name_put_class_static_pjvalue(
                   jvm_class_index  clsidx,
                   cp_info_mem_align *fldname,
                   cp_info_mem_align *flddesc,
                   jvalue            *_jvalue);

extern jvm_field_index field_index_put_object_instance_pjvalue(
                   jvm_object_hash  objhash,
                   jvm_field_index  fldidx,
                   jvalue          *_jvalue);

extern jvm_field_index field_name_put_object_instance_pjvalue(
                   jvm_object_hash    objhash,
                   cp_info_mem_align *fldname,
                   cp_info_mem_align *flddesc,
                   jvalue            *_jvalue);

#endif /* _field_h_included_ */

/* EOF */
