#ifndef _attribute_h_included_
#define _attribute_h_included_

/*!
 * @file attribute.h
 *
 * @brief Attribute management functions for the JVM.
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

ARCH_HEADER_COPYRIGHT_APACHE(attribute, h,
"$URL$",
"$Id$");


extern jvm_attribute_index attribute_find_in_field_by_cp_entry(
                                   jvm_class_index  clsidx,
                                   jvm_field_index  fldidx,
                                   cp_info_dup     *atrname);

extern jvm_attribute_index attribute_find_in_field_by_enum(
                                   jvm_class_index  clsidx,
                                   jvm_field_index  fldidx,
                                   rint             atrenum);

extern jvm_attribute_index attribute_find_in_method_by_cp_entry(
                                   jvm_class_index  clsidx,
                                   jvm_method_index mthidx,
                                   cp_info_dup     *atrname);

extern jvm_attribute_index attribute_find_in_method_by_enum(
                                   jvm_class_index  clsidx,
                                   jvm_method_index mthidx,
                                   rint             atrenum);

extern jvm_attribute_index attribute_find_in_class_by_cp_entry(
                                   jvm_class_index  clsidx,
                                   cp_info_dup     *atrname);

extern jvm_attribute_index attribute_find_in_class_by_enum(
                                   jvm_class_index  clsidx,
                                   rint             atrenum);

#endif /* _attribute_h_included_ */

/* EOF */
