#ifndef _utf_h_defined_
#define _utf_h_defined_

/*!
 * @file utf.h
 *
 * @brief Manipulate UTF-8 CONSTANT_Utf8_info character strings.
 *
 * There are three character string types in this program:
 * null-terminated @link #rchar (rchar)@endlink strings
 * @e ala 'C' language, UTF-8
 * @link #CONSTANT_Utf8_info (CONSTANT_Utf8_info)@endlink strings,
 * and Unicode @link #jchar (jchar)[]@endlink strings.
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


/* Prototypes for functions in 'utf.c' */

ARCH_HEADER_COPYRIGHT_APACHE(utf, h,
"$URL$",
"$Id$");

extern jshort utf_utf2unicode(CONSTANT_Utf8_info *utf_inbfr,
                              jchar              *outbfr);

extern rchar *utf_utf2prchar(CONSTANT_Utf8_info *src);

extern rchar *utf_utf2prchar_classname(CONSTANT_Utf8_info *src);

extern jbyte utf_utf_strcmp(CONSTANT_Utf8_info *s1,
                            CONSTANT_Utf8_info *s2);

extern jbyte utf_prchar_pcfs_strcmp(rchar                   *s1,
                                    ClassFile               *pcfs,
                                    jvm_constant_pool_index  cpidx2);

extern jbyte utf_pcfs_strcmp(CONSTANT_Utf8_info      *s1,
                             ClassFile               *pcfs,
                             jvm_constant_pool_index  cpidx2);

extern jbyte utf_prchar_classname_strcmp(rchar                  *s1,
                                         ClassFile              *pcfs,
                                        jvm_constant_pool_index cpidx2);

extern jbyte utf_classname_strcmp(CONSTANT_Utf8_info      *s1,
                                  ClassFile               *pcfs2,
                                  jvm_constant_pool_index  cpidx2);

extern jvm_array_dim utf_get_utf_arraydims(CONSTANT_Utf8_info *inbfr);

extern rboolean utf_utf_isarray(CONSTANT_Utf8_info *inbfr);

extern rboolean utf_utf_isclassformatted(CONSTANT_Utf8_info *src);

extern cp_info_dup
                 *utf_utf2utf_unformatted_classname(cp_info_dup *inbfr);

#endif /* _utf_h_defined_ */


/* EOF */
