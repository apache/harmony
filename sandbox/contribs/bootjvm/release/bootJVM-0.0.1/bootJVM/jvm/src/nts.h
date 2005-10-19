#ifndef _nts_h_defined_
#define _nts_h_defined_

/*!
 * @file nts.h
 *
 * @brief Manipulate null-terminated (@link #rchar rchar@endlink)
 * character strings.
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

ARCH_HEADER_COPYRIGHT_APACHE(nts, h,
"$URL$",
"$Id$");

/* Prototypes for functions in 'nts.c' */

extern cp_info_dup *nts_prchar2utf(rchar *inbfr);

extern jshort nts_prchar2unicode(rchar *inbfr, jchar *outbfr);

extern cp_info_dup *nts_prchar2utf_classname(rchar        *inbfr,
                                             jvm_array_dim arraydims);

extern jvm_array_dim nts_get_prchar_arraydims(rchar *inbfr);

extern rboolean nts_prchar_isarray(rchar *inbfr);

extern rboolean nts_prchar_isprimativeformatted(rchar *src);

extern rboolean nts_prchar_isclassformatted(rchar *src);

extern rchar *nts_prchar2prchar_unformatted_classname(rchar *inbfr);

#endif /* _nts_h_defined_ */


/* EOF */
