/*!
 * @file field.c
 *
 * @brief Manipulate ClassFile fields.
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
ARCH_COPYRIGHT_APACHE(field, c, "$URL$ $Id$");


#include <strings.h>

#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "exit.h"
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h"
#include "utf.h"


/*!
 * @brief Locate the field_info index for a field in a class.
 *
 * This works both with object instance fields and static class
 * instance fields.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldname           UTF8 constant_pool entry of name of field
 *                             in class.
 *
 * @param  flddesc           UTF8 constant_pool entry of description of
 *                             field type.
 *
 *
 * @returns field table index of this field in class or
 *          @link #jvm_field_index_bad jvm_field_index_bad@endlink
 *          if not found.
 *
 *
 * @throws nothing.  Let caller throw an error like
 *         @link #JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR
           JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR@endlink if it is
 *         useful at that point.  The purpose of this
 *         function is simply to locate the field, not
 *         make a value judgment on the meaning of the
 *         search result.
 *
 */
jvm_field_index field_find_by_cp_entry(jvm_class_index  clsidx,
                                       cp_info_dup     *fldname,
                                       cp_info_dup     *flddesc)
{
    /* Prohibit invalid parameter */
    if (jvm_class_index_null == clsidx)
    {
        exit_throw_exception(EXIT_JVM_FIELD,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
    }

    /* Point to class structure, then look for field  */
    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    u2 fcount = pcfs->fields_count;
    jvm_field_index fldidx;
    for (fldidx = 0; fldidx < fcount; fldidx++)
    {
        /* If field name and description match, go run thread */
        if ((0 == utf_pcfs_strcmp(PTR_THIS_CP_Utf8(fldname),
                                 pcfs,
                                 pcfs->fields[fldidx]->name_index))
            &&
            (0 == utf_pcfs_strcmp(PTR_THIS_CP_Utf8(flddesc),
                                 pcfs,
                                 pcfs
                                   ->fields[fldidx]
                                     ->descriptor_index)))
        {
            return(fldidx);
        }
    }

    /* Not found */
    return(jvm_field_index_bad);

} /* END of field_find_by_cp_entry() */


/*!
 * @brief Determine if a field index is to a static class instance
 * field or not.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldidx            Field index in class of field to be located
 *
 *
 * @returns @link #rtrue rtrue@endlink if this field is a class static
 *          field, otherwise @link #rfalse rfalse@endlink.
 *
 *
 */
rboolean field_index_is_class_static(jvm_class_index clsidx,
                                     jvm_field_index fldidx)
{
    /* Prohibit invalid parameter */
    if (jvm_field_index_bad == fldidx)
    {
        return(rfalse);
    }

    jvm_field_lookup_index csflidx;
    for (csflidx= 0;
         csflidx < CLASS(clsidx).num_class_static_field_lookups;
         csflidx++)
    {
        /*
         * Report a match when field index is
         * found in static class fields table
         */
        if (fldidx ==
            (CLASS(clsidx).class_static_field_lookup)[csflidx])
        {
            return(rtrue);
        }
    }

    /* Not found */
    return(rfalse);

} /* END of field_index_is_class_static() */


/*!
 * @brief Determine if a field name/descriptor is a static class
 * instance field or not.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldname           UTF8 constant_pool entry of name of field
 *                             in class.
 *
 * @param  flddesc           UTF8 constant_pool entry of description of
 *                             field type.
 *
 * @returns @link #rtrue rtrue@endlink if this field is a class static
 *          field, otherwise @link #rfalse rfalse@endlink.
 *
 */
rboolean field_name_is_class_static(jvm_class_index  clsidx,
                                    cp_info_dup     *fldname,
                                    cp_info_dup     *flddesc)
{
    return(field_index_is_class_static(
               clsidx,
               field_find_by_cp_entry(clsidx, fldname, flddesc)));

} /* END of field_name_is_class_static() */


/*!
 * @brief Determine if a field index is to an object instance field
 * or not.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldidx            Field index in class of field to be located
 *
 *
 * @returns @link #rtrue rtrue@endlink if this field is an object
 *          instance field, otherwise @link #rfalse rfalse@endlink.
 *
 */
rboolean field_index_is_object_instance(jvm_class_index clsidx,
                                        jvm_field_index fldidx)
{
    /* Prohibit invalid parameter */
    if (jvm_field_index_bad == fldidx)
    {
        return(rfalse);
    }

    jvm_field_lookup_index oiflidx;
    for (oiflidx= 0;
         oiflidx < CLASS(clsidx).num_object_instance_field_lookups;
         oiflidx++)
    {
        /*
         * Report a match when field index is
         * found in static class fields table
         */
        if (fldidx ==
            (CLASS(clsidx).object_instance_field_lookup)[oiflidx])
        {
            return(rtrue);
        }
    }

    /* Not found */
    return(rfalse);

} /* END of field_index_is_object_instance() */


/*!
 * @brief Determine if a field name/descriptor is to an object instance
 * field or not.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldname           UTF8 constant_pool entry of name of field
 *                             in class.
 *
 * @param  flddesc           UTF8 constant_pool entry of description of
 *                             field type.
 *
 *
 * @returns @link #rtrue rtrue@endlink if this field is an object
 *          instance field, otherwise @link #rfalse rfalse@endlink.
 *
 */
rboolean field_name_is_object_instance(jvm_class_index   clsidx,
                                       cp_info_dup     *fldname,
                                       cp_info_dup     *flddesc)
{
    return(field_index_is_object_instance(
               clsidx,
               field_find_by_cp_entry(clsidx, fldname, flddesc)));

} /* END of field_name_is_object_instance() */


/*!
 * @brief Retrieve by field index a field lookup index to a static
 * class instance field.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldidx            Field index in class of field to be located
 *
 *
 * @returns class static field lookup index of located field, otherwise
 *          @link #jvm_field_index_bad jvm_field_index_bad@endlink.
 *
 */
jvm_field_lookup_index
    field_index_get_class_static_lookup(jvm_class_index  clsidx,
                                        jvm_field_index fldidx)
{
    /* Prohibit invalid parameter */
    if (jvm_field_index_bad == fldidx)
    {
        return(jvm_field_lookup_index_bad);
    }

    jvm_field_lookup_index csflidx;
    for (csflidx= 0;
         csflidx < CLASS(clsidx).num_class_static_field_lookups;
         csflidx++)
    {
        /*
         * Report a match when field index is
         * found in static class fields table
         */
        if (fldidx ==
            (CLASS(clsidx).class_static_field_lookup)[csflidx])
        {
            return(fldidx);
        }
    }

    /* Not found */
    return(jvm_field_lookup_index_bad);

} /* END of field_index_get_class_static_lookup() */


/*!
 * @brief Retrieve by name/descriptor a field lookup index to a static
 * class instance field.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldname           UTF8 constant_pool entry of name of field
 *                             in class.
 *
 * @param  flddesc           UTF8 constant_pool entry of description of
 *                             field type.
 *
 * @returns class static field lookup index of located field, otherwise
 *          @link #jvm_field_index_bad jvm_field_index_bad@endlink.
 *
 */
jvm_field_lookup_index
    field_name_get_class_static_lookup(jvm_class_index  clsidx,
                                       cp_info_dup     *fldname,
                                       cp_info_dup     *flddesc)
{
    return(field_index_get_class_static_lookup(
               clsidx,
               field_find_by_cp_entry(clsidx, fldname, flddesc)));


} /* END of field_name_get_class_static_lookup() */


/*!
 * @brief Retrieve by field index a field lookup index to an object
 * instance field.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldidx            Field index in class of field to be located
 *
 *
 * @returns object instance field lookup index of located field,
 *          otherwise
 *          @link #jvm_field_index_bad jvm_field_index_bad@endlink.
 *
 */
jvm_field_lookup_index
    field_index_get_object_instance_lookup(jvm_class_index  clsidx,
                                           jvm_field_index fldidx)
{
    /* Prohibit invalid parameter */
    if (jvm_field_index_bad == fldidx)
    {
        return(rfalse);
    }

    jvm_field_lookup_index oiflidx;
    for (oiflidx= 0;
         oiflidx < CLASS(clsidx).num_object_instance_field_lookups;
         oiflidx++)
    {
        /*
         * Report a match when field index is
         * found in static class fields table
         */
        if (fldidx ==
            (CLASS(clsidx).object_instance_field_lookup)[oiflidx])
        {
            return(rtrue);
        }
    }

    /* Not found */
    return(rfalse);

} /* END of field_index_get_object_instance_lookup() */


/*!
 * @brief Retrieve by name/descriptor a field lookup index to an object
 * instance field.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldname           UTF8 constant_pool entry of name of field
 *                             in class.
 *
 * @param  flddesc           UTF8 constant_pool entry of description of
 *                             field type.
 *
 *
 * @returns object instance field lookup index of located field,
 *          otherwise 
 *          @link #jvm_field_index_bad jvm_field_index_bad@endlink.
 *
 */
jvm_field_lookup_index
    field_name_get_object_instance_lookup(jvm_class_index  clsidx,
                                          cp_info_dup     *fldname,
                                          cp_info_dup     *flddesc)
{
    return(field_index_get_object_instance_lookup(
               clsidx,
               field_find_by_cp_entry(clsidx, fldname, flddesc)));


} /* END of field_name_get_object_instance_lookup() */


/*!
 * @brief Retrieve by field index the value of a static class instance
 * field.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldidx            Field index in class of field to be located
 *
 *
 * @returns pointer to class static field data, otherwise
 *          @link #rnull rnull@endlink.
 *
 */
jvalue *field_index_get_class_static_pjvalue(jvm_class_index  clsidx,
                                             jvm_field_index fldidx)
{
    /* Prohibit invalid parameter */
    if (jvm_field_index_bad == fldidx)
    {
        return((jvalue *) rnull);
    }

    jvm_field_lookup_index csflidx;
    for (csflidx= 0;
         csflidx < CLASS(clsidx).num_class_static_field_lookups;
         csflidx++)
    {
        /*
         * Report a match when field index is
         * found in static class fields table
         */
        if (fldidx ==
            (CLASS(clsidx).class_static_field_lookup)[csflidx])
        {
            return(&(CLASS(clsidx).class_static_field_data)[csflidx]);
        }
    }

    /* Not found */
    return((jvalue *) rnull);

} /* END of field_index_get_class_static_pjvalue() */


/*!
 * @brief Retrieve by name the value of a static class instance field.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             located.
 *
 * @param  fldname           UTF8 constant_pool entry of name of field
 *                             in class.
 *
 * @param  flddesc           UTF8 constant_pool entry of description of
 *                             field type.
 *
 *
 * @returns pointer to class static field data, otherwise
 *          @link #rnull rnull@endlink.
 *
 */
jvalue *field_name_get_class_static_pjvalue(jvm_class_index  clsidx,
                                            cp_info_dup     *fldname,
                                            cp_info_dup     *flddesc)
{
    return(field_index_get_class_static_pjvalue(
               clsidx,
               field_find_by_cp_entry(clsidx, fldname, flddesc)));


} /* END of field_name_get_class_static_pjvalue() */


/*!
 * @brief Retrieve by field index the value of an object instance field.
 *
 *
 * @param  objhash           Object hash of object whose field is to be
 *                             located.
 *
 * @param  fldidx            Field index in class of field to be located
 *
 *
 * @returns Pointer to object instance field data, otherwise
 *          @link #rnull rnull@endlink.
 *
 */
jvalue *field_index_get_object_instance_pjvalue(jvm_object_hash objhash,
                                                jvm_field_index fldidx)
{
    jvm_class_index clsidx = OBJECT_CLASS_LINKAGE(objhash)->clsidx;

    jvm_field_lookup_index oifldidx;
    for (oifldidx= 0;
         oifldidx < CLASS(clsidx).num_object_instance_field_lookups;
         oifldidx++)
    {
        /*
         * Report a match when field index is
         * found in object instance fields table
         */
        if (fldidx ==
            (CLASS(clsidx).object_instance_field_lookup)[oifldidx])
        {
            return(&OBJECT(objhash)
                        .object_instance_field_data[oifldidx]);
        }
    }

    /* Not found */
    return((jvalue *) rnull);

} /* END of field_index_get_object_instance_pjvalue() */


/*!
 * @brief Retrieve by name the value of an object instance field.
 *
 *
 * @param  objhash           Object hash of object whose field is to be
 *                             located.
 *
 * @param  fldname           UTF8 constant_pool entry of name of field
 *                             in class.
 *
 * @param  flddesc           UTF8 constant_pool entry of description of
 *                             field type.
 *
 *
 * @returns Pointer to object instance field data, otherwise
 *          @link #rnull rnull@endlink.
 *
 */
jvalue *field_name_get_object_instance_pjvalue(jvm_object_hash  objhash,
                                               cp_info_dup     *fldname,
                                               cp_info_dup     *flddesc)
{
    return(field_index_get_object_instance_pjvalue(
               objhash,
               field_find_by_cp_entry(
                   OBJECT_CLASS_LINKAGE(objhash)->clsidx,
                   fldname,
                   flddesc)));

} /* END of field_name_get_object_instance_pjvalue() */


/*!
 * @brief Store by field index the value of a static class instance
 * field.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             stored.
 *
 * @param  fldidx            Field index in class of field to be stored.
 *
 *
 * @param  _jvalue           Data to be stored.
 *
 *
 * @returns Field index of field name if this is a valid class static
 *          field, else
 *          @link #jvm_field_index_bad jvm_field_index_bad@endlink.
 *
 */
jvm_field_index
    field_index_put_class_static_pjvalue(jvm_class_index  clsidx,
                                         jvm_field_index  fldidx,
                                         jvalue          *_jvalue)
{
    /* Prohibit invalid parameter */
    if (jvm_field_index_bad == fldidx)
    {
        return(jvm_field_index_bad);
    }

    jvm_field_lookup_index csflidx;
    for (csflidx= 0;
         csflidx < CLASS(clsidx).num_class_static_field_lookups;
         csflidx++)
    {
        /*
         * Report a match when field index is
         * found in static class fields table
         */
        if (fldidx ==
            (CLASS(clsidx).class_static_field_lookup)[csflidx])
        {
            memcpy(&CLASS(clsidx).class_static_field_data[csflidx],
                   _jvalue,
                   sizeof(jvalue));
            return(fldidx);
        }
    }

    /* Not found */
    return(jvm_field_index_bad);

} /* END of field_index_put_class_static_pjvalue() */


/*!
 * @brief Store by name/descriptor the value of a static class
 * instance field.
 *
 *
 * @param  clsidx            Class index of class whose field is to be
 *                             stored.
 *
 * @param  fldname           UTF8 constant_pool entry of name of field
 *                             in class.
 *
 * @param  flddesc           UTF8 constant_pool entry of description of
 *                             field type.
 *
 * @param  _jvalue           Data to be stored.
 *
 *
 * @returns Field index of field name if this is a valid class static
 *          field, else
 *          @link #jvm_field_index_bad jvm_field_index_bad@endlink.
 *
 */
jvm_field_index
    field_name_put_class_static_pjvalue(jvm_class_index  clsidx,
                                        cp_info_dup     *fldname,
                                        cp_info_dup     *flddesc,
                                        jvalue          *_jvalue)
{
    return(field_index_put_class_static_pjvalue(
               clsidx,
               field_find_by_cp_entry(clsidx,
                                      fldname,
                                      flddesc),
               _jvalue));

} /* END of field_name_put_class_static_pjvalue() */


/*!
 * @brief Store by field index the value of an object instance field.
 *
 *
 * @param  objhash           Object hash of object whose field is to be
 *                             located.
 *
 * @param  fldidx            Field index in class of field to be stored
 *
 * @param  _jvalue           Data to be stored.
 *
 *
 * @returns Field index of field name if this is a valid object instance
 *          field, else
 *          @link #jvm_field_index_bad jvm_field_index_bad@endlink.
 *
 */
jvm_field_index
    field_index_put_object_instance_pjvalue(jvm_object_hash  objhash,
                                            jvm_field_index  fldidx,
                                            jvalue          *_jvalue)
{
    jvm_class_index clsidx = OBJECT_CLASS_LINKAGE(objhash)->clsidx;

    jvm_field_lookup_index oifldidx;
    for (oifldidx= 0;
         oifldidx < CLASS(clsidx).num_object_instance_field_lookups;
         oifldidx++)
    {
        /*
         * Report a match when field index is
         * found in object instance fields table
         */
        if (fldidx ==
            (CLASS(clsidx).object_instance_field_lookup)[oifldidx])
        {
           memcpy(&OBJECT(objhash).object_instance_field_data[oifldidx],
                   _jvalue,
                   sizeof(jvalue));
            return(fldidx);
        }
    }

    /* Not found */
    return(jvm_field_index_bad);

} /* END of field_index_put_object_instance_pjvalue() */


/*!
 * @brief Store by name/descriptor the value of an object
 * instance field.
 *
 *
 * @param  objhash           Object hash of object whose field is to be
 *                             located.
 *
 * @param  fldname           UTF8 constant_pool entry of name of field
 *                             in class.
 *
 * @param  flddesc           UTF8 constant_pool entry of description of
 *                             field type.
 *
 * @param  _jvalue           Data to be stored.
 *
 *
 * @returns Field index of field name if this is a valid object instance
 *          field, else
 *          @link #jvm_field_index_bad jvm_field_index_bad@endlink.
 *
 */
jvm_field_index
    field_name_put_object_instance_pjvalue(jvm_object_hash  objhash,
                                           cp_info_dup     *fldname,
                                           cp_info_dup     *flddesc,
                                           jvalue          *_jvalue)
{
    return(field_index_put_object_instance_pjvalue(
               objhash,
               field_find_by_cp_entry(
                   OBJECT_CLASS_LINKAGE(objhash)->clsidx,
                   fldname,
                   flddesc),
               _jvalue));

} /* END of field_name_put_object_instance_pjvalue() */


/* EOF */
