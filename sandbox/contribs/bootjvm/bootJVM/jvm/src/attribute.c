/*!
 * @file attribute.c
 *
 * @brief Manipulate ClassFile attributes.
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
ARCH_COPYRIGHT_APACHE(attribute, c, "$URL$ $Id$");


#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "jvm.h"
#include "linkage.h"
#include "utf.h"


/*!
 * @brief Locate an attribute by constant_pool entry for a field,
 * method, or class.  The attribute pointer passed in is valid for
 * all three types.
 *
 *
 * @param  pcfs     Pointer to ClassFile area
 *
 * @param  acount   Attribute count from field, method, or class.
 *
 * @param  patr     Attribute array for a field, method, or class.
 *
 * @param  atrname  UTF8 constant_pool entry of name of attribute in
 *                    field, method, or class.
 *
 *
 * @returns Attribute table index ofthie attribute or
 *        @link #jvm_attribute_index_bad jvm_attribute_index_bad@endlink
 *          if not found.
 *
 */
static jvm_attribute_index attribute_name_common_find(
                               ClassFile           *pcfs,
                               u2                   acount,
                               attribute_info_dup **patr,
                               cp_info_dup         *atrname)
{
    /* Search for match of attribute array against requested name */
    jvm_attribute_index atridx;

    for (atridx = 0; atridx < acount; atridx++)
    {
        if (0 == utf_pcfs_strcmp(PTR_THIS_CP_Utf8(atrname),
                                 pcfs,
                                 atridx))
        {
            return(atridx);
        }
    }

    /* Not found */
    return(jvm_attribute_index_bad);

} /* END of attribute_name_common_find() */


/*!
 * @brief Locate an attribute by enumeration for a field, method,
 * or class.
 *
 * The attribute pointer passed in is valid for all three types.
 *
 *
 * @param  pcfs     Pointer to ClassFile area
 *
 * @param  acount   Attribute count from field, method, or class.
 *
 * @param  patr     Attribute array for a field, method, or class.
 *
 * @param  atrenum  Attribute enumeration (from
 *                    @link jvm/src/classfile.h classfile.h@endlink)
 *                    for attribute of field, method, or class to locate
 *                    (e.g. LOCAL_CODE_ATTRIBUTE).
 *
 *
 * @returns Attribute table index ofthie attribute or
 *        @link #jvm_attribute_index_bad jvm_attribute_index_bad@endlink
 *          if not found.
 *
 */
static jvm_attribute_index attribute_enum_common_find(
                               ClassFile                 *pcfs,
                               u2                         acount,
                               attribute_info_dup       **patr,
                               classfile_attribute_enum   atrenum)
{
    /* Search for match of attribute array against requested enum */
    jvm_attribute_index atridx;

    for (atridx = 0; atridx < acount; atridx++)
    {
        if (atrenum ==
            cfattrib_atr2enum(pcfs,
                              patr[atridx]->ai.attribute_name_index))
        {
            return(atridx);
        }
    }

    /* Not found */
    return(jvm_attribute_index_bad);

} /* END of attribute_enum_common_find() */


/*!
 * @brief Locate by constant_pool entry the attribute_info index for an
 * attribute in a field attribute area.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             searched for an attribute.
 *
 * @param  fldidx            Field index of field to search.
 *
 * @param  atrname           UTF8 constant_pool entry of name of
 *                             attribute name to locate.
 *
 *
 * @returns attribute table index of this attribute in field, or
 *        @link #jvm_attribute_index_bad jvm_attribute_index_bad@endlink
 *         if not found.
 *
 */
jvm_attribute_index
    attribute_find_in_field_by_cp_entry(jvm_class_index  clsidx,
                                        jvm_field_index  fldidx,
                                        cp_info_dup     *atrname)
{
    /* Prohibit invalid class parameter */
    if (jvm_class_index_null == clsidx)
    {
        return(jvm_attribute_index_bad);
    }

    /* Point to class structure, then look for attribute */
    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    /* Prohibit invalid field parameter */
    if (pcfs->fields_count <= fldidx)
    {
        return(jvm_attribute_index_bad);
    }

    return(
        attribute_name_common_find(pcfs,
                                 pcfs->fields[fldidx]->attributes_count,
                                   pcfs->fields[fldidx]->attributes,
                                   atrname));

} /* END of attribute_find_in_field_by_cp_entry() */


/*!
 * @brief Locate by enumeration the attribute_info index for an
 * attribute in a field attribute area.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             searched for an attribute.
 *
 * @param  fldidx            Field index of field to search.
 *
 * @param  atrenum           @link #classfile_attribute_enum
                             LOCAL_xxxx_ATTRIBUTE@endlink enumeration of
 *                           attribute to locate.
 *
 * @returns attribute table index of this attribute in field, or
 *        @link #jvm_attribute_index_bad jvm_attribute_index_bad@endlink
 *          if not found.
 *
 */
jvm_attribute_index
    attribute_find_in_field_by_enum(jvm_class_index          clsidx,
                                    jvm_field_index          fldidx,
                                    classfile_attribute_enum atrenum)
{
    /* Prohibit invalid class parameter */
    if (jvm_class_index_null == clsidx)
    {
        return(jvm_attribute_index_bad);
    }

    /* Point to class structure, then look for attribute */
    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    /* Prohibit invalid field parameter */
    if (pcfs->fields_count <= fldidx)
    {
        return(jvm_attribute_index_bad);
    }

    return(attribute_enum_common_find(pcfs,
                                 pcfs->fields[fldidx]->attributes_count,
                                      pcfs->fields[fldidx]->attributes,
                                      atrenum));

} /* END of attribute_find_in_field_by_enum() */


/*!
 * @brief Locate by constant_pool entry the attribute_info index for
 * an attribute in a method attribute area.
 *
 *
 * @param  clsidx            Class index of class whose method is to be
 *                             searched for an attribute.
 *
 * @param  mthidx            Method index of method to search.
 *
 * @param  atrname           UTF8 constant_pool entry of name of
 *                             attribute name to locate.
 *
 * @returns attribute table index of this attribute in method, or
 *        @link #jvm_attribute_index_bad jvm_attribute_index_bad@endlink
 *          if not found.
 *
 */
jvm_attribute_index
    attribute_find_in_method_by_cp_entry(jvm_class_index   clsidx,
                                         jvm_method_index  mthidx,
                                         cp_info_dup      *atrname)
{
    /* Prohibit invalid class parameter */
    if (jvm_class_index_null == clsidx)
    {
        return(jvm_attribute_index_bad);
    }

    /* Point to class structure, then look for attribute */
    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    /* Prohibit invalid method parameter */
    if (pcfs->methods_count <= mthidx)
    {
        return(jvm_attribute_index_bad);
    }

    return(
        attribute_name_common_find(pcfs,
                                pcfs->methods[mthidx]->attributes_count,
                                   pcfs->methods[mthidx]->attributes,
                                   atrname));

} /* END of attribute_find_in_method_by_cp_entry() */


/*!
 * @brief Locate by enumeration the attribute_info index for an
 * attribute in a method attribute area.
 *
 *
 * @param  clsidx            Class index of class whose method is to be
 *                             searched for an attribute.
 *
 * @param  mthidx            Method index of method to search.
 *
 * @param  atrenum           @link #classfile_attribute_enum
                             LOCAL_xxxx_ATTRIBUTE@endlink enumeration of
 *                           attribute to locate.
 *
 * @returns attribute table index of this attribute in method, or
 *        @link #jvm_attribute_index_bad jvm_attribute_index_bad@endlink
 *          if not found.
 *
 */
jvm_attribute_index
    attribute_find_in_method_by_enum(jvm_class_index          clsidx,
                                     jvm_method_index         mthidx,
                                     classfile_attribute_enum atrenum)
{
    /* Prohibit invalid class parameter */
    if (jvm_class_index_null == clsidx)
    {
        return(jvm_attribute_index_bad);
    }

    /* Point to class structure, then look for attribute */
    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    /* Prohibit invalid method parameter */
    if (pcfs->methods_count <= mthidx)
    {
        return(jvm_attribute_index_bad);
    }

    return(attribute_enum_common_find(pcfs,
                                pcfs->methods[mthidx]->attributes_count,
                                      pcfs->methods[mthidx]->attributes,
                                      atrenum));

} /* END of attribute_find_in_method_by_enum() */


/*!
 * @brief Locate by constant_pool entry the attribute_info index for
 * an attribute in a class attribute area.
 *
 *
 * @param  clsidx            Class index of class be searched for an
 *                             attribute.
 *
 * @param  atrname           UTF8 constant_pool entry of name of
 *                             attribute name to locate.
 *
 * @returns attribute table index of this attribute in class, or
 *        @link #jvm_attribute_index_bad jvm_attribute_index_bad@endlink
 *          if not found.
 *
 */
jvm_attribute_index
    attribute_find_in_class_by_cp_entry(jvm_class_index  clsidx,
                                        cp_info_dup     *atrname)
{
    /* Prohibit invalid class parameter */
    if (jvm_class_index_null == clsidx)
    {
        return(jvm_attribute_index_bad);
    }

    /* Point to class structure, then look for attribute */
    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    return(
        attribute_name_common_find(pcfs,
                                   pcfs->attributes_count,
                                   pcfs->attributes,
                                   atrname));

} /* END of attribute_find_in_method_by_cp_entry() */


/*!
 * @brief Locate by enumeration the attribute_info index for
 * an attribute in a class attribute area.
 *
 *
 * @param  clsidx            Class index of class whose method is to be
 *                             searched for an attribute.
 *
 * @param  atrenum           @link #classfile_attribute_enum
                             LOCAL_xxxx_ATTRIBUTE@endlink enumeration of
 *                           attribute to locate.
 *
 * @returns attribute table index of this attribute in class, or
 *        @link #jvm_attribute_index_bad jvm_attribute_index_bad@endlink
 *          if not found.
 *
 */
jvm_attribute_index
    attribute_find_in_class_by_enum(jvm_class_index          clsidx,
                                    classfile_attribute_enum atrenum)
{
    /* Prohibit invalid class parameter */
    if (jvm_class_index_null == clsidx)
    {
        return(jvm_attribute_index_bad);
    }

    /* Point to class structure, then look for attribute */
    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    return(attribute_enum_common_find(pcfs,
                                      pcfs->attributes_count,
                                      pcfs->attributes,
                                      atrenum));

} /* END of attribute_find_in_class_by_enum() */


/* EOF */
