/*!
 * @file method.c
 *
 * @brief Manipulate ClassFile methods.
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

#include "arch.h"
ARCH_COPYRIGHT_APACHE(method, c, "$URL$ $Id$");


#include <stdlib.h>
#include <strings.h>

#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "exit.h"
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h"
#include "nts.h"
#include "utf.h"


/*!
 * @brief Locate the method_info index for a normal method in a class
 * using a constant_pool entry to the name and description of
 * the method.
 *
 *
 * @param  clsidx            Class index of class whose method is to be
 *                             located.
 *
 * @param  mthname           UTF8 constant_pool entry of name of method
 *                             in class.
 *
 * @param  mthdesc           UTF8 constant_pool entry of description of
 *                             method parameters and return type.
 *
 *
 * @returns method table index of this method in class or
 *          @link #jvm_method_index_bad jvm_method_index_bad@endlink
 *          if not found.
 *
 *
 * Throws: nothing.  Let caller throw an error like
 *         JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR if it is
 *         useful at that point.  The purpose of this
 *         function is simply to locate the method, not
 *         make a value judgment on the meaning of the
 *         search result.
 *
 */
jvm_method_index method_find_by_cp_entry(jvm_class_index  clsidx,
                                         cp_info_dup     *mthname,
                                         cp_info_dup     *mthdesc)
{
    /* Prohibit invalid parameter */
    if (jvm_class_index_null == clsidx)
    {
        exit_throw_exception(EXIT_JVM_METHOD,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
    }

    /* Prohibit non-UTF8 CP entries from being parsed */
    CONSTANT_Utf8_info *pmthdata = PTR_THIS_CP_Utf8(mthname);
    if (CONSTANT_Utf8 != pmthdata->tag)
    {
        return(jvm_method_index_bad);
    }

    pmthdata = PTR_THIS_CP_Utf8(mthdesc);
    if (CONSTANT_Utf8 != pmthdata->tag)
    {
        return(jvm_method_index_bad);
    }

    /* Point to class structure, then look for method  */
    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    u2 mcount = pcfs->methods_count;
    jvm_method_index mthidx;
    for (mthidx = 0; mthidx < mcount; mthidx++)
    {
        /* Report a match of method name and description when found */
        if ((0 == utf_pcfs_strcmp(PTR_THIS_CP_Utf8(mthname),
                                  pcfs,
                                  pcfs->methods[mthidx]->name_index))
            &&
            (0 == utf_pcfs_strcmp(PTR_THIS_CP_Utf8(mthdesc),
                                  pcfs,
                                  pcfs
                                    ->methods[mthidx]
                                      ->descriptor_index)))
        {
            return(mthidx);
        }
    }

    /* Not found */
    return(jvm_method_index_bad);

} /* END of method_find_by_cp_entry() */


/*!
 * @brief Retrieve by <b><code>(rchar *)</code></b> name a method
 * index to a method in a class.
 *
 *
 * @param  clsidx            Class index of class whose method is to be
 *                             located.
 *
 * @param  mthname           Null-terminated string of name of method
 *                             in class.
 *
 * @param  mthdesc           Null-terminated string of description of
 *                             method parameters and return type.
 *
 *
 * @returns method index of located method in class, otherwise
 *          @link #jvm_method_index_bad jvm_method_index_bad@endlink.
 *
 */
jvm_method_index
    method_find_by_prchar(jvm_class_index  clsidx,
                          rchar           *mthname,
                          rchar           *mthdesc)
{
    cp_info_dup *pcip_mthname = nts_prchar2utf(mthname);
    cp_info_dup *pcip_mthdesc = nts_prchar2utf(mthdesc);

    jvm_method_index rc =
        method_find_by_cp_entry(clsidx, pcip_mthname, pcip_mthdesc);

    HEAP_FREE_DATA(pcip_mthname);
    HEAP_FREE_DATA(pcip_mthdesc);

    return(rc);

} /* END of method_find_by_prchar() */

/*!
 * @brief Extract method return type from descriptor
 *
 *
 * @param clsidx      Class table index of method to examine.
 *
 * @param mthdescidx  Class file constant_pool index of method
 *                    descriptor to examine.  This entry must be a
 *                    CONSTANT_Utf8_info string containing the
 *                    descriptor of an unqualified method name.
 *
 *
 * @returns Primative base type of method return value or
 *          @link #LOCAL_BASETYPE_ERROR LOCAL_BASETYPE_ERROR@endlink
 *          if not found.
 *
 */
jvm_basetype method_return_type(jvm_class_index         clsidx,
                                jvm_constant_pool_index mthdescidx)
{
    cp_info_dup        *pcpd;
    CONSTANT_Utf8_info *pcpd_Utf8;

    pcpd =CLASS_OBJECT_LINKAGE(clsidx)->pcfs->constant_pool[mthdescidx];
    pcpd_Utf8 = PTR_THIS_CP_Utf8(pcpd);

    u2 idx;
           /* Last char will be result:/ - 1/ except 'Lsome/class;' */
    for(idx = 0; idx < pcpd_Utf8->length - 1; idx++)
    {
        /* Scan for parm list closure, next char is return type */
        if (METHOD_CHAR_CLOSE_PARM == pcpd_Utf8->bytes[idx])
        {
            switch (pcpd_Utf8->bytes[idx + 1])
            {
                case BASETYPE_CHAR_B:
                case BASETYPE_CHAR_C:
                case BASETYPE_CHAR_D:
                case BASETYPE_CHAR_F:
                case BASETYPE_CHAR_I:
                case BASETYPE_CHAR_J:
                case BASETYPE_CHAR_L:
                case BASETYPE_CHAR_S:
                case BASETYPE_CHAR_Z:
                case BASETYPE_CHAR_ARRAY:
                case METHOD_CHAR_VOID:
                    return((jvm_basetype) pcpd_Utf8->bytes[idx + 1]);
            }
        }
    }

    /*!
     * @todo  Should this throw a @b VerifyError instead?
     *        Is it better to let caller do this?
     */

    /* Error, something else found */
    return((jvm_basetype) LOCAL_BASETYPE_ERROR);

} /* END of method_return_type() */


/* EOF */
