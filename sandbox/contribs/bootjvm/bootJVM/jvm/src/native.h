#ifndef _native_h_included_
#define _native_h_included_

/*!
 * @file native.h
 *
 * @brief Local native method interface between JNI and JVM.
 *
 * Native methods that are implemented @e within the JVM may circumvent
 * the full-blown JNI interface by calling these functions.
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

ARCH_HEADER_COPYRIGHT_APACHE(native, h,
"$URL$",
"$Id$");

/* Prototypes for functions in 'native.c' */

extern rvoid native_run_method(jvm_thread_index          thridx,
                               jvm_class_index           clsidx,
                               jvm_native_method_ordinal nmord,
                               jvm_constant_pool_index   mthnameidx,
                               jvm_constant_pool_index   mthdescidx,
                               jvm_access_flags          access_flags,
                               jvm_virtual_opcode        opcode,
                               rboolean                  isinitmethod);

extern jvm_native_method_ordinal native_locate_local_method(
                                ClassFile               *pcfs,
                                jvm_constant_pool_index  clsnameidx,
                                jvm_constant_pool_index  mthnameidx,
                                jvm_constant_pool_index  mthdescidx,
                                rboolean          find_registerNatives);

extern rint native_jlString_critical_field_value;
extern rint native_jlString_critical_field_length;
extern rint native_jlString_critical_num_fields;

#endif /* _native_h_included_ */

/* EOF */
