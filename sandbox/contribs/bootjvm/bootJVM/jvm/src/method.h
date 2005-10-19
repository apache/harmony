#ifndef _method_h_included_
#define _method_h_included_

/*!
 * @file method.h
 *
 * @brief Method management functions for the JVM.
 *
 *
 * @section Control
 *
 * \$URL$ \$Id$
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
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

ARCH_COPYRIGHT_APACHE(method, h, "$URL$ $Id$");


/*!
 * @def METHOD
 * @brief Access structures of a class' method table at certain index.
 *
 * Each class has a table of methods, divided into virtual methods
 * @e within the Java code and native methods referenced @e from the
 * Java code and implemented in some outside object library.
 * They may be distinquished by the @link #ACC_NATIVE ACC_NATIVE@endlink
 * status bit in @link #rclass.status rclass.status@endlink
 * This macro references one of them using the @p @b clsidx index for
 * the class and @p @b mthidx for the method table entry in that class.
 *
 * @param clsidx  Class table index into the global
 * @link #rjvm.class rjvm.class[]@endlink array (via
 * @link #pjvm pjvm->class[]@endlink).
 * 
 * @param mthidx  Index into method table for this class.
 * 
 * @returns pointer to a method table entry
 * 
 */
#define METHOD(clsidx, mthidx) \
    (CLASS_OBJECT_LINKAGE(clsidx)->pcfs->methods[mthidx])


/* Prototypes for functions in 'method.c' */

extern
    jvm_method_index method_find_by_cp_entry(jvm_class_index  clsidx,
                                             cp_info_dup    *mthname,
                                             cp_info_dup    *mthdesc);

extern jvm_method_index method_find_by_prchar(jvm_class_index  clsidx,
                                              rchar           *mthname,
                                              rchar           *mthdesc);

extern jvm_basetype method_return_type(jvm_class_index      clsidx,
                                    jvm_constant_pool_index mthdescidx);

#endif /* _method_h_included_ */

/* EOF */
