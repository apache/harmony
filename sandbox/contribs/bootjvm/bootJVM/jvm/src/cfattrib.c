/*!
 * @file cfattrib.c
 *
 * @brief Implementation of <em>The Java Virtual Machine Specification,
 * version 2, Chapter 4, the Class File Format</em>.
 *
 * This file contains the attribute manipulation functions.  All
 * other work is performed in
 * @link jvm/src/classfile.c classfile.c@endlink.
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
ARCH_SOURCE_COPYRIGHT_APACHE(cfattrib, c,
"$URL$",
"$Id$");


#include <string.h>
#include "jvmcfg.h"
#include "attribute.h"
#include "cfmacros.h"
#include "classfile.h"
#include "util.h"
#include "utf.h"


/*!
 * @brief Compare string to expected valuue of attribute name index.
 *
 *
 * @param string  Attribute string for comparison against ClassFile
 *                attribute_info value.
 *
 *
 * @returns jbyte from utf_prchar_pcfs_strcmp()
 *
 */
#define CMP_ATTRIBUTE(string) \
    utf_prchar_pcfs_strcmp(string, pcfs, attribute_name_index)


/*!
 * @name Attribute test utilities.
 *
 *
 * @param  pcfs                  ClassFile structure containing
 *                               attribute @b xxx.
 *
 * @param  attribute_name_index  Constant pool index of @b xxx
 *                               attribute in this class file structure.
 *
 *
 * @todo HARMONY-6-jvm-cfattrib-1 Both in this group of functions and
 *       probably numerous places around the code, the
 *       @link #u2 u2@endlink parameters that reference
 *       @c @b constant_pool entries should be changed to
 *       become type @link #jvm_constant_pool_index
         jvm_constant_pool_index@endlink instead as a more accurate
 *       reflection of their purpose.  This type was a later addition
 *       to the code and is therefore not reflected in the earlier
 *       portions that were written.
 *
 */


/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Map UTF8 string names for attributes in a @c @b constant_pool
 * to LOCAL_xxx_ATTRIBUTE constants for the purpose of switch(int)
 * instead of switch("string") code constructions.
 *
 *
 * @returns enumeration constant from classfile_attribute_enum
 *
 */

classfile_attribute_enum cfattrib_atr2enum(ClassFile *pcfs,
                                           u2      attribute_name_index)
{
    ARCH_FUNCTION_NAME(cfattrib_atr2enum);

    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_CONSTANTVALUE_ATTRIBUTE))
    {
        return(LOCAL_CONSTANTVALUE_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_CODE_ATTRIBUTE))
    {
        return(LOCAL_CODE_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_EXCEPTIONS_ATTRIBUTE))
    {
        return(LOCAL_EXCEPTIONS_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_INNERCLASSES_ATTRIBUTE))
    {
        return(LOCAL_INNERCLASSES_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_ENCLOSINGMETHOD_ATTRIBUTE))
    {
        return(LOCAL_ENCLOSINGMETHOD_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_SYNTHETIC_ATTRIBUTE))
    {
        return(LOCAL_SYNTHETIC_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_SIGNATURE_ATTRIBUTE))
    {
        return(LOCAL_SIGNATURE_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_SOURCEFILE_ATTRIBUTE))
    {
        return(LOCAL_SOURCEFILE_ATTRIBUTE);
    }
    else
    if (0 ==
            CMP_ATTRIBUTE(CONSTANT_UTF8_LINENUMBERTABLE_ATTRIBUTE))
    {
        return(LOCAL_LINENUMBERTABLE_ATTRIBUTE);
    }
    else
    if (0 ==
         CMP_ATTRIBUTE(CONSTANT_UTF8_LOCALVARIABLETABLE_ATTRIBUTE))
    {
        return(LOCAL_LOCALVARIABLETABLE_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(
                        CONSTANT_UTF8_LOCALVARIABLETYPETABLE_ATTRIBUTE))
    {
        return(LOCAL_LOCALVARIABLETYPETABLE_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_DEPRECATED_ATTRIBUTE))
    {
        return(LOCAL_DEPRECATED_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(
                     CONSTANT_UTF8_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE))
    {
        return(LOCAL_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(
                   CONSTANT_UTF8_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE))
    {
        return(LOCAL_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(
            CONSTANT_UTF8_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE))
    {
        return(LOCAL_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(
          CONSTANT_UTF8_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE))
    {
      return(LOCAL_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_ANNOTATIONDEFAULT_ATTRIBUTE))
    {
        return(LOCAL_ANNOTATIONDEFAULT_ATTRIBUTE);
    }
    else
    {
        return(LOCAL_UNKNOWN_ATTRIBUTE);
    }

} /* END of cfattrib_atr2enum() */


/*!
 * @brief Short version of cfattrib_atr2enum(), but only check
 * if an index refers to a Code_attribute area.
 *
 *
 * @returns @link #rtrue rtrue@endlink if this attribute is indeed
 *          a @link #CONSTANT_UTF8_CODE_ATTRIBUTE
            CONSTANT_UTF8_CODE_ATTRIBUTE@endlink,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 */
rboolean cfattrib_iscodeattribute(ClassFile *pcfs,
                                  u2 attribute_name_index)
{
    ARCH_FUNCTION_NAME(cfattrib_iscodeattribute);

    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_CODE_ATTRIBUTE))
    {
        return(rtrue);
    }
    else
    {
        return(rfalse);
    }
} /* END of cfattrib_iscodeattribute() */

/*@} */ /* End of grouped definitions */


/*!
 * @brief Load an annotation's element_value and minimally verify
 * that it has either valid contents.
 *
 * If the element_value is valid, it will be copied
 * into its element_value_mem_align destination area.
 *
 * @param  pcfs      Pointer to (partially) parsed ClassFile area
 *
 * @param  pevpma    Pointer to an element_value_pair_mem_align[]
 *                   address telling where in the heap this
 *                   element_value will be copied from the source area.
 *
 * @param  pev       Pointer to an element_value in class file image.
 *                   This data will be stored in the heap at
 *                   location @c @b *pevpma .
 *
 *
 * @returns Point to first byte past this annotation,
 *          or @link #rnull rnull@endlink if parsing problem.
 *
 *
 * @todo HARMONY-6-jvm-cfattrib.c-40 Needs unit testing with real data.
 *
 */

u1 *cfattrib_load_elementvalue(ClassFile               *pcfs,
                               element_value_mem_align *pevpma,
                               element_value           *pev)
{
    ARCH_FUNCTION_NAME(cfattrib_load_elementvalue);

    u1     ev_tag;
    jbyte *pabytes;
    u2    *pu2;

    pabytes = (jbyte *) pev;

    /*
     * Retrieve element_value tag, then process value type
     */
    pevpma->empty[0] = FILL_ELEMENT_VALUES_MEM_ALIGN0;
    pevpma->empty[1] = FILL_ELEMENT_VALUES_MEM_ALIGN1;
    pevpma->empty[2] = FILL_ELEMENT_VALUES_MEM_ALIGN2;

    ev_tag = *pabytes++;
    pevpma->tag = ev_tag;

    /*!
     * @todo HARMONY-6-jvm-cfattrib.c-39 Should a check
     * be made for each case to match @b ev_tag with the 
     * constant_pool_index to see if they agree properly?
     */
    switch(ev_tag)
    {
        case BASETYPE_CHAR_B:
        case BASETYPE_CHAR_C:
        case BASETYPE_CHAR_D:
        case BASETYPE_CHAR_F:
        case BASETYPE_CHAR_I:
        case BASETYPE_CHAR_J:
        case BASETYPE_CHAR_S:
        case BASETYPE_CHAR_Z:
        case BASETYPE_CHAR_s:
            MAKE_PU2(pu2, pabytes);
            pevpma->_value._const_value_index = GETRS2(pu2);
            pabytes += sizeof(jvm_constant_pool_index);
            break;

        case BASETYPE_CHAR_e:
            MAKE_PU2(pu2, pabytes);
            pevpma->_value._enum_const_value.type_name_index =
                                                    GETRS2(pu2);
            pabytes += sizeof(jvm_constant_pool_index);

            MAKE_PU2(pu2, pabytes);
            pevpma->_value._enum_const_value.const_name_index =
                                                    GETRS2(pu2);
            pabytes += sizeof(jvm_constant_pool_index);
            break;

        case BASETYPE_CHAR_c:
            MAKE_PU2(pu2, pabytes);
            pevpma->_value._class_info_index = GETRS2(pu2);
            pabytes += sizeof(jvm_constant_pool_index);
            break;

        case BASETYPE_CHAR_AT:
            /* Allocate one nested annotation */
            pevpma->_value._pannotation_value =
                HEAP_GET_METHOD(1 * sizeof(annotation_mem_align),rtrue);

            /*
             * WARNING!  RECURSIVE CALL:
             *
             * Load up a nested annotation.  This may go as deep
             * as the nesting of annotation data, which will be
             * inherently finite for a valid class file, but the
             * actual depth is unknown at this point.
             */

            pabytes =
                cfattrib_load_annotation(pcfs,
                                      pevpma->_value._pannotation_value,
                                         (annotation *) pabytes);


            /* If parsing error, pass it up the line */
            if (rnull == pabytes)
            {
                return((u1 *) rnull);
            }

            break;

        case BASETYPE_CHAR_ARRAY:
            /*
             * Load an array_values[] slot, then point to
             * next array_values in class file image.
             */
            pevpma->_value._array_value.empty[0] =
                                           FILL_ARRAY_VALUES_MEM_ALIGN0;
            pevpma->_value._array_value.empty[1] =
                                           FILL_ARRAY_VALUES_MEM_ALIGN1;

            MAKE_PU2(pu2, pabytes);
            pevpma->_value._array_value.num_values = GETRS2(pu2);
            pu2++;
            pabytes = (jbyte *) pu2;

            if (0 == pevpma->_value._array_value.num_values)
            {
                /*
                 * Possible, but not theoretically reasonable,
                 * but not explicitly prohibited by spec
                 */
                pevpma->_value._array_value.pvalues =
                    (element_value_mem_align **) rnull;
            }
            else
            {
                pevpma->_value._array_value.pvalues =
                  HEAP_GET_METHOD(pevpma->_value._array_value.num_values
                                    * sizeof(element_value_mem_align *),
                                    rtrue);

                /*
                 * Load an element_value_mem_align[] slot, then point to
                 * next element_value in class file image.
                 */
                u2 evidx;
                for (evidx = 0;
                     evidx < pevpma->_value._array_value.num_values;
                     evidx++)
                {
                    pevpma->_value._array_value.pvalues[evidx] =
                        HEAP_GET_METHOD(
                            sizeof(element_value_mem_align *),
                            rtrue);
                
                    /*
                     * WARNING!  RECURSIVE CALL:
                     *
                     * Unload a nested element_value.  This may go as
                     * deep as the nesting of annotation data, which
                     * will be inherently finite for a valid class file,
                     * but the actual depth is unknown at this point.
                     */
                    pabytes = cfattrib_load_elementvalue(
                                  pcfs,
                                  pevpma
                                    ->_value
                                      ._array_value
                                        .pvalues[evidx],
                                  (element_value *) pabytes);

                    /* If parsing error, pass it up the line */
                    if (rnull == pabytes)
                    {
                        return((u1 *) rnull);
                    }
                }

            }

            break;

    } /* switch(ev_tag) */

    return((u1 *) pabytes);

} /* END of cfattrib_load_elementvalue() */

/*!
 * @brief Unload an element_value_mem_align and free its heap area.
 *
 * @param  pcfs      Pointer to (partially) parsed ClassFile area
 *
 * @param  pevma     Pointer to a element_value_mem_align allocation
 *                   where this attribute is stored in the heap
 *
 *
 * @returns @link #rvoid rvoid@endlink Whether it succeeds or fails,
 *          returning anything does not make much sense.  This is
 *          similar to @c @b free(3) not returning anything even when
 *          a bad pointer was passed in.
 *
 */

void cfattrib_unload_elementvalue(ClassFile               *pcfs,
                                  element_value_mem_align *pevma)
{
    ARCH_FUNCTION_NAME(cfattrib_unload_elementvalue);

    u2 ptridx;

    switch(pevma->tag)
    {
        case BASETYPE_CHAR_AT:
            cfattrib_unload_annotation(pcfs,
                                      pevma->_value._pannotation_value);

            HEAP_FREE_METHOD(pevma);
            break;

        case BASETYPE_CHAR_ARRAY:
            for (ptridx = 0;
                 ptridx < pevma->_value._array_value.num_values;
                 ptridx++)
            {
                /*
                 * WARNING!  RECURSIVE CALL:
                 *
                 * Unload a nested element_value_mem_align.  This may go
                 * as deep as the nesting of annotation data, which will
                 * be inherently finite for a valid class file, but the
                 * actual depth is unknown at this point.
                 */
                cfattrib_unload_elementvalue(pcfs,
                            pevma->_value._array_value.pvalues[ptridx]);
            }
            HEAP_FREE_METHOD(pevma->_value._array_value.pvalues);
            HEAP_FREE_METHOD(pevma);
            break;

        default:
            /* Nothing else has further heap allocation */
            HEAP_FREE_METHOD(pevma);
            break;

    } /* switch(ev_tag) */

    return;

} /* END of cfattrib_unload_elementvalue() */


/*!
 * @brief Load an annotation and verify that it has either valid
 * contents.
 *
 * If the annotation is valid, it will be copied
 * into its destination area.
 *
 * @param  pcfs      Pointer to (partially) parsed ClassFile area
 *
 * @param  pnma      Pointer to an annotation_mem_align[] address
 *                   telling where in the heap this annotation will be
 *                   copied from the source area.
 *
 * @param  pn        Pointer to an annotation in class file image.
 *                   This data will be stored in the heap at
 *                   location @c @b *pnma .
 *
 *
 * @returns Point to first byte past this annotation,
 *          or @link #rnull rnull@endlink if parsing problem.
 *
 *
 * @todo HARMONY-6-jvm-cfattrib.c-38 Needs unit testing with real data.
 *
 */

u1 *cfattrib_load_annotation(ClassFile            *pcfs,
                             annotation_mem_align *pnma,
                             annotation           *pn)
{
    ARCH_FUNCTION_NAME(cfattrib_load_annotation);

    jbyte *pabytes;
    u2 *pu2;

    jvm_annotation_type_index    anttidx;
    u2                           numevp;

    pabytes = (jbyte *) pn;

    MAKE_PU2(pu2, pabytes);

    anttidx = GETRS2(pu2);
    pu2++;
    pnma->type_index = anttidx;

    numevp = GETRS2(pu2);
    pu2++;
    pnma->num_element_value_pairs = numevp;

    MAKE_PU2(pu2, pabytes);

    /* Normal pointing to beginning of source area to load from */
    pabytes = (jbyte *) pu2;

    if (0 == numevp)
    {
        /*
         * Possible, but not theoretically reasonable,
         * but not explicitly prohibited by spec
         */
        pnma->element_value_pairs
            = (struct element_value_pair_mem_align_struct *) rnull;
    }
    else
    {
        rint evpbytes = numevp * sizeof(element_value_pair_mem_align);

        pnma->element_value_pairs = HEAP_GET_METHOD(evpbytes, rfalse);

        memset(pnma->element_value_pairs,
               FILL_ELEMENT_VALUES_UNION,
               evpbytes);


        element_value_pair_mem_align *evp = pnma->element_value_pairs;

        jvm_element_value_pair_index evpidx;
        for (evpidx = 0; evpidx < numevp; evpidx++, evp++)
        {
            /*
             * Load an element_value_pair_mem_align[] slot, then point
             * to next element_value_pair in class file image.
             */
            MAKE_PU2(pu2, pabytes);
            evp->element_value_index = GETRS2(pu2);
            pu2++;
            pabytes = (jbyte *) pu2;

            /*
             * WARNING!  RECURSIVE CALL:
             *
             * Load a nested element_value, which may invoke this
             * function for a nested annotation.  This may go as deep
             * as the nesting of annotation data, which will be
             * inherently finite for a valid class file, but the
             * actual depth is unknown at this point.
             */
            pabytes =
                cfattrib_load_elementvalue(pcfs,
                                           &evp->value,
                                           (element_value *) pabytes);

            /* If parsing error, pass it up the line */
            if (rnull == pabytes)
            {
                return((u1 *) rnull);
            }

        } /* for (evpidx) */

    } /* if numevep else */

    return((u1 *) pabytes);

} /* END of cfattrib_load_annotation() */

/*!
 * @brief Unload an annotation and free its heap area.
 *
 * @param  pcfs      Pointer to (partially) parsed ClassFile area
 *
 * @param  pnma      Pointer to an annotation_mem_align[] allocation
 *                   where this attribute is stored in the heap
 *
 *
 * @returns @link #rvoid rvoid@endlink Whether it succeeds or fails,
 *          returning anything does not make much sense.  This is
 *          similar to @c @b free(3) not returning anything even when
 *          a bad pointer was passed in.
 *
 */

void cfattrib_unload_annotation(ClassFile            *pcfs,
                                annotation_mem_align *pnma)
{
    ARCH_FUNCTION_NAME(cfattrib_unload_annotation);

    jvm_element_value_pair_index evpidx;
    u2                           numevp;

    element_value_pair_mem_align *evp = pnma->element_value_pairs;

    numevp = pnma->num_element_value_pairs;

    for (evpidx = 0; evpidx < numevp; evpidx++, evp++)
    {
        /*
         * WARNING!  RECURSIVE CALL:
         *
         * Unload a nested element_value, which may invoke this
         * function for a nested annotation.  This may go as deep
         * as the nesting of annotation data, which will be
         * inherently finite for a valid class file, but the
         * actual depth is unknown at this point.
         */
        cfattrib_unload_elementvalue(pcfs, &evp->value);
    }
    HEAP_FREE_METHOD(pnma->element_value_pairs);
    HEAP_FREE_METHOD(pnma);

    return;

} /* END of cfattrib_unload_annotation() */

/*!
 * @brief Load an attribute and verify that it has either valid contents
 * or is ignored as an unknown attribute.
 *
 * If the attribute index is valid at all (for either a known or even
 * an unknown but properly formed attribute), it will be copied
 * into its destination area.
 *
 * @param  pcfs      Pointer to (partially) parsed ClassFile area
 *
 * @param  ppaima    Pointer to a attribute_info_mem_align[] address
 *                   telling where in the heap this attribute will be
 *                   copied from the source area.
 *
 * @param  pai       Pointer to an attribute in class file image.
 *                   This data will be stored in the heap at
 *                   location @c @b *ppaima .
 *
 *
 * @returns Point to first byte past this attribute,
 *          or @link #rnull rnull@endlink if parsing problem.
 *          The heap area @c @b *ppaima will contain a valid
 *          heap pointer only if there is a valid
 *          @link attribute_info.attribute_name_index
            attribute_name_index@endlink, else it will also
 *          be @link #rnull rnull@endlink.
 *
 */

u1 *cfattrib_load_attribute(ClassFile                 *pcfs,
                            attribute_info_mem_align **ppaima,
                            attribute_info            *pai)
{
    ARCH_FUNCTION_NAME(cfattrib_load_attribute);

    attribute_info tmpatr;
    u4             tmplen;
    u2             attribute_name_index;

    jbyte *pabytes;
    u2 *pu2;

    u2 *patblu2;
    u2 atblidx;
    u2 atbllen;

    u2 parmtblidx;
    u2 parmtbllen;

    jbyte *pnext_ai = (jbyte *) pai;

    /*!
     * @internal The @c @b attribute_length member could be
     * tested for each incoming attribute that was of fixed
     * length, e.g. LOCAL_CONSTANTVALUE_ATTRIBUTE, but cannot
     * be checked directly if variable, e.g. LOCAL_CODE_ATTRIBUTE.
     * Therefore, check the @c @b attribute_name_index to verify
     * that its string matches the expected value, but do not
     * bother checking the length.  Thus the assumption is made
     * that if the name matches the expected string, then the
     * length will also be correct.
     */
    tmpatr.attribute_name_index = GETRS2(&pai->attribute_name_index);
    tmpatr.attribute_length     = GETRI4(&pai->attribute_length);

    /* Convenient mapping for direct support of CMP_ATTRIBUTE() macro */
    attribute_name_index = tmpatr.attribute_name_index;

    cfmsgs_typemsg((rchar *) arch_function_name,
                   pcfs,
                   tmpatr.attribute_name_index);
    sysDbgMsg(DMLNORM,
              (char *) arch_function_name,
              "len=%d",
              tmpatr.attribute_length);

    /* Range check the name index (index is unsigned, so not checked) */
    GENERIC_FAILURE_PTR(( /* (tmpatr.attribute_name_index <  0) || */
                             (tmpatr.attribute_name_index >=
                              pcfs->constant_pool_count)),
                        DMLNORM,
                        "loadClassFileStructures",
                        "attribute name index",
                        u1,
                        rnull,
                        rnull);
 
    /* Calculate total size of this (attribute_info) area in pai */
    tmplen = sizeof(attribute_info)
             - sizeof(u1)
             + tmpatr.attribute_length;

    /*
     * Skip past this item's header and contents to next attribute,
     * adjusting for dummy [info] field.
     */
    pnext_ai += tmplen;


    /*
     * Calculate total size of this (attribute_info_mem_align) area
     * in ppaima
     */
    tmplen = sizeof(attribute_info_mem_align)
             - sizeof(u1)
             + tmpatr.attribute_length;

    /* Allocate a heap location to store this attribute */
    *ppaima = (attribute_info_mem_align *) HEAP_GET_METHOD(tmplen,
                                                           rfalse);

    /*
     * Copy attribute data to heap.  The @b info item is optional,
     * depending on which attribute this is.  Copy it in directly
     * unless it is a Code_attribute, then do that specially.
     * All other variable-length attributes have the @b SINGLE
     * variable-length item at the END of the structure (except
     * annotatons and local variable type attributes, which are being
     * ignored by this implementation);  therefore, there is no need
     * to play pointer games to adjust what is stored where in those
     * attributes. Simply index that array[] item.
     *
     * Also investigate attribute type constraints using a
     * string-based @c @b switch .
     */
    (*ppaima)->empty[0] = FILL_INFO_MEM_ALIGN0;
    (*ppaima)->empty[1] = FILL_INFO_MEM_ALIGN1;
    (*ppaima)->ai.attribute_name_index = tmpatr.attribute_name_index;
    (*ppaima)->ai.attribute_length = tmpatr.attribute_length;

    classfile_attribute_enum atrenum =
        cfattrib_atr2enum(pcfs, (*ppaima)->ai.attribute_name_index);

    switch (atrenum)
    {
        case LOCAL_CONSTANTVALUE_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-26 Needs better
             * unit testing.
             */

            PTR_ATR_CONSTANTVALUE_AI(ppaima)->constantvalue_index =
                GETRS2(&((ConstantValue_attribute *) pai)
                          ->constantvalue_index);

            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(
                                 CONSTANT_UTF8_CONSTANTVALUE_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;


        case LOCAL_CODE_ATTRIBUTE:
            /*
             * Copy Code_attribute field-by-field as in the top-level
             * structure, allocating array[] pieces in that same manner.
             *
             * @note  You can get away with referencing
             *        @c @b pai->member here because these first
             *        few members are of fixed length.  You @e still
             *        must use GETRS2() or GETRI4() because they are in
             *        a byte-stream class file, which has no guaranteed
             *        word alignment.
             */
            PTR_ATR_CODE_AI(ppaima)->max_stack =
                GETRS2(&((Code_attribute *) pai)->max_stack);

            PTR_ATR_CODE_AI(ppaima)->max_locals =
                GETRS2(&((Code_attribute *) pai)->max_locals);

            PTR_ATR_CODE_AI(ppaima)->code_length =
                GETRI4(&((Code_attribute *) pai)->code_length);

            if (0 == PTR_ATR_CODE_AI(ppaima)->code_length)
            {
                /*
                 * Possible, but not theoretically reasonable,
                 * thus prohibited by spec
                 */
                PTR_ATR_CODE_AI(ppaima)->code = (u1 *) rnull;
            }
            else
            {
                /*
                 * Block copy array of (u1) data-- no endianness
                 * issues here due to single-byte data width for
                 * all items in array.
                 */
                PTR_ATR_CODE_AI(ppaima)->code =
                    HEAP_GET_METHOD(PTR_ATR_CODE_AI(ppaima)
                                      ->code_length * sizeof(u1),
                                    rfalse);

                /* Notice this is copy TO *ptr and FROM *bfr,not *ptr!*/
                portable_memcpy( PTR_ATR_CODE_AI(ppaima)->code,
                            /* 1st var len fld in class file code area*/
                            &((Code_attribute *)  pai)->code,
                           (PTR_ATR_CODE_AI(ppaima)->code_length)
                            * sizeof(u1));

            }

            pabytes  = (jbyte *) &((Code_attribute *) pai)->code;
            pabytes += PTR_ATR_CODE_AI(ppaima)->code_length *
                       sizeof(u1);

            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-23 Loading the exception
             * table was originally done with memcpy(), which worked
             * only on big-endian architectures.  This approach uses
             * the same technique as elsewhere, and does appear to
             * work properly on big-endian architectures.  It needs
             * unit testing on little-endian architectures to verify
             * proper functionality.
             */

            /*
             * Load up exception fields now, but (u2) and (u4) data
             * items need endianness conversions, so don't do memcpy()
             *
             */
            MAKE_PU2(pu2, pabytes);

            atbllen = GETRS2(pu2);
            pu2++;

            pabytes = (jbyte *) pu2;

            PTR_ATR_CODE_AI(ppaima)->exception_table_length = atbllen;
            if (0 == atbllen)
            {
                /* Possible, and theoretically reasonable */
                PTR_ATR_CODE_AI(ppaima)->exception_table =
                    (rvoid *) rnull;
            }
            else
            {
                exception_table_entry *pete;

                PTR_ATR_CODE_AI(ppaima)->exception_table =
                    HEAP_GET_METHOD(atbllen *
                                          sizeof(exception_table_entry),
                                    rfalse);

                /* Load up exception table one entry at a time */
                for (atblidx = 0; atblidx < atbllen; atblidx++)
                {
                    MAKE_PU2(pu2, pabytes);

                    pete = &(PTR_ATR_CODE_AI(ppaima)
                               ->exception_table)[atblidx];

                    pete->start_pc = GETRS2(pu2);
                    pu2++;

                    pete->end_pc = GETRS2(pu2);
                    pu2++;

                    pete->handler_pc = GETRS2(pu2);
                    pu2++;

                    pete->catch_type = GETRS2(pu2);
                    pu2++;

                    pabytes = (jbyte *) pu2;
                }
            }

            /*
             * Load up attributes area
             */
            MAKE_PU2(pu2, pabytes);

            atbllen = GETRS2(pu2);
            pu2++;

            pabytes = (jbyte *) pu2;

            PTR_ATR_CODE_AI(ppaima)->attributes_count = atbllen;

            if (0 == atbllen)
            {
                PTR_ATR_CODE_AI(ppaima)->attributes =
                    (attribute_info_mem_align **) rnull;
            }
            else
            {
                PTR_ATR_CODE_AI(ppaima)->attributes =
                    HEAP_GET_METHOD(PTR_ATR_CODE_AI(ppaima)
                                      ->attributes_count *
                                    sizeof(attribute_info_mem_align *),
                                    rtrue);


                /*
                 * WARNING!  RECURSIVE CALL:
                 *
                 * Load up each attribute in this attribute area.
                 * This should NOT recurse more than once since there
                 * can ONLY be ONE Code_attribute per method.
                 */

                jvm_attribute_index atridx;

                for (atridx = 0;
                     atridx < PTR_ATR_CODE_AI(ppaima)->attributes_count;
                     atridx++)
                {
                    /*
                     * Load an attribute and verify that it is either
                     * a valid (or an ignored) attribute, then point to
                     * next attribute in class file image.
                     *
                     *
                     * WARNING!  RECURSIVE CALL:
                     *
                     * Load up a nested attribute.  This may go as deep
                     * as the nesting of attribute data, which will be
                     * inherently finite for a valid class file, but the
                     * actual depth is unknown at this point.
                     */

                    pabytes =
                        cfattrib_load_attribute(
                            pcfs,
                            (attribute_info_mem_align **)
                                &(PTR_ATR_CODE_AI(ppaima)
                                    ->attributes)[atridx],
                            (attribute_info *) pabytes);

                    LOAD_SYSCALL_FAILURE_ATTRIB((rnull == pabytes),
                                                "load field attribute",
                                                *ppaima,
                                                PTR_ATR_CODE_AI(ppaima)
                                                  ->attributes);

                } /* for (atridx) */
            }

            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_CODE_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_EXCEPTIONS_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-24 Needs better
             * unit testing.
             */

            pabytes = (jbyte *) &((Exceptions_attribute *) pai)
                                   ->number_of_exceptions;

            MAKE_PU2(pu2, pabytes);

            atbllen = GETRS2(pu2);
            pu2++;

            pabytes = (jbyte *) pu2;

            PTR_ATR_EXCEPTIONS_AI(ppaima)->number_of_exceptions =
                atbllen;
            if (0 != atbllen)
            {
                /*
                 * Load up exception index table one entry at a time.
                 * Space has already been reserved by the
                 * @b attribute_length field above as parm to
                 * HEAP_GET_METHOD() above @c @b switch() statement.
                 */
                patblu2 = &(PTR_ATR_EXCEPTIONS_AI(ppaima)
                              ->exception_index_table)[0];
                for (atblidx = 0; atblidx < atbllen; atblidx++)
                {
                    MAKE_PU2(pu2, pabytes);

                    patblu2[atblidx] = GETRS2(pu2);
                    pu2++;

                    pabytes = (jbyte *) pu2;
                }
            }

            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_EXCEPTIONS_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_INNERCLASSES_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-25 Needs unit testing.
             */

            pabytes = (jbyte *) &((InnerClasses_attribute *) pai)
                                   ->number_of_classes;

            MAKE_PU2(pu2, pabytes);

            atbllen = GETRS2(pu2);
            pu2++;

            pabytes = (jbyte *) pu2;

            PTR_ATR_INNERCLASSES_AI(ppaima)->number_of_classes =
                atbllen;
            if (0 != atbllen)
            {
                inner_class_table_entry *picte;

                /*
                 * Load up inner classes table one entry at a time.
                 * Space has already been reserved by the
                 * @b attribute_length field above as parm to
                 * HEAP_GET_METHOD() above @c @b switch() statement.
                 */
                for (atblidx = 0; atblidx < atbllen; atblidx++)
                {
                    MAKE_PU2(pu2, pabytes);

                    picte = &(PTR_ATR_INNERCLASSES_AI(ppaima)
                                ->classes)[atblidx];

                    picte->inner_class_info_index   = GETRS2(pu2);
                    pu2++;

                    picte->outer_class_info_index   = GETRS2(pu2);
                    pu2++;

                    picte->inner_name_index         = GETRS2(pu2);
                    pu2++;

                    picte->inner_class_access_flags = GETRS2(pu2);
                    pu2++;

                    pabytes = (jbyte *) pu2;
                }
            }

            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(
                                  CONSTANT_UTF8_INNERCLASSES_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_LINENUMBERTABLE_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-30 Needs unit testing.
             */

            pabytes = (jbyte *) &((LineNumberTable_attribute *) pai)
                                   ->line_number_table_length;

            MAKE_PU2(pu2, pabytes);

            atbllen = GETRS2(pu2);
            pu2++;

            pabytes = (jbyte *) pu2;

            PTR_ATR_LINENUMBERTABLE_AI(ppaima)
              ->line_number_table_length =
                atbllen;
            if (0 != atbllen)
            {
                line_number_table_entry *plnte;

                /*
                 * Load up line number table one entry at a time.
                 * Space has already been reserved by the
                 * @b attribute_length field above as parm to
                 * HEAP_GET_METHOD() above @c @b switch() statement.
                 */
                for (atblidx = 0; atblidx < atbllen; atblidx++)
                {
                    MAKE_PU2(pu2, pabytes);

                    plnte = &(PTR_ATR_LINENUMBERTABLE_AI(ppaima)
                                ->line_number_table)[atblidx];

                    plnte->start_pc   = GETRS2(pu2);
                    pu2++;

                    plnte->line_number   = GETRS2(pu2);
                    pu2++;

                    pabytes = (jbyte *) pu2;
                }
            }

            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(
                               CONSTANT_UTF8_LINENUMBERTABLE_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_LOCALVARIABLETABLE_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-31 Needs unit testing.
             */

            pabytes = (jbyte *) &((LocalVariableTable_attribute *) pai)
                                   ->local_variable_table_length;

            MAKE_PU2(pu2, pabytes);

            atbllen = GETRS2(pu2);
            pu2++;

            pabytes = (jbyte *) pu2;

            PTR_ATR_LOCALVARIABLETABLE_AI(ppaima)
              ->local_variable_table_length = atbllen;
            if (0 != atbllen)
            {
                local_variable_table_entry *plvte;

                /*
                 * Load up local variable table one entry at a time.
                 * Space has already been reserved by the
                 * @b attribute_length field above as parm to
                 * HEAP_GET_METHOD() above @c @b switch() statement.
                 */
                for (atblidx = 0; atblidx < atbllen; atblidx++)
                {
                    MAKE_PU2(pu2, pabytes);

                    plvte = &(PTR_ATR_LOCALVARIABLETABLE_AI(ppaima)
                                ->local_variable_table)[atblidx];

                    plvte->start_pc         = GETRS2(pu2);
                    pu2++;

                    plvte->length           = GETRS2(pu2);
                    pu2++;

                    plvte->name_index       = GETRS2(pu2);
                    pu2++;

                    plvte->descriptor_index = GETRS2(pu2);
                    pu2++;

                    plvte->index            = GETRS2(pu2);
                    pu2++;

                    pabytes = (jbyte *) pu2;
                }
            }

            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(
                            CONSTANT_UTF8_LOCALVARIABLETABLE_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_LOCALVARIABLETYPETABLE_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-32 Needs unit testing.
             */

            pabytes = (jbyte *)
                      &((LocalVariableTypeTable_attribute *) pai)
                         ->local_variable_type_table_length;

            MAKE_PU2(pu2, pabytes);

            atbllen = GETRS2(pu2);
            pu2++;

            pabytes = (jbyte *) pu2;

            PTR_ATR_LOCALVARIABLETYPETABLE_AI(ppaima)
              ->local_variable_type_table_length = atbllen;
            if (0 != atbllen)
            {
                local_variable_type_table_entry *plvtte;

                /*
                 * Load up local variable table one entry at a time.
                 * Space has already been reserved by the
                 * @b attribute_length field above as parm to
                 * HEAP_GET_METHOD() above @c @b switch() statement.
                 */
                for (atblidx = 0; atblidx < atbllen; atblidx++)
                {
                    MAKE_PU2(pu2, pabytes);

                    plvtte = &(PTR_ATR_LOCALVARIABLETYPETABLE_AI(ppaima)
                                ->local_variable_type_table)[atblidx];

                    plvtte->start_pc        = GETRS2(pu2);
                    pu2++;

                    plvtte->length          = GETRS2(pu2);
                    pu2++;

                    plvtte->name_index      = GETRS2(pu2);
                    pu2++;

                    plvtte->signature_index = GETRS2(pu2);
                    pu2++;

                    plvtte->index           = GETRS2(pu2);
                    pu2++;

                    pabytes = (jbyte *) pu2;
                }
            }

            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(
                        CONSTANT_UTF8_LOCALVARIABLETYPETABLE_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_DEPRECATED_ATTRIBUTE:
            /*
             * Nothing else to process, no attribute-specific data.
             */

            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_DEPRECATED_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-33 Needs unit testing.
             */

            pabytes = (jbyte *)
                      &((RuntimeVisibleAnnotations_attribute *) pai)
                         ->num_annotations;

            MAKE_PU2(pu2, pabytes);

            atbllen = GETRS2(pu2);
            pu2++;
            PTR_ATR_RUNTIMEVISIBLEANNOTATIONS_AI(ppaima)
              ->num_annotations = atbllen;

            pabytes = (jbyte *) pu2;

            if (0 == atbllen)
            {
                PTR_ATR_RUNTIMEVISIBLEANNOTATIONS_AI(ppaima)
                  ->annotations =
                    (annotation_mem_align **) rnull;
            }
            else
            {
                PTR_ATR_RUNTIMEVISIBLEANNOTATIONS_AI(ppaima)
                  ->annotations =
                    HEAP_GET_METHOD(
                            PTR_ATR_RUNTIMEVISIBLEANNOTATIONS_AI(ppaima)
                                      ->num_annotations *
                                    sizeof(annotation_mem_align *),
                                    rtrue);

                jvm_annotation_index antidx;

                for (antidx = 0;
                     antidx <
             PTR_ATR_RUNTIMEVISIBLEANNOTATIONS_AI(ppaima)
               ->num_annotations;
                     antidx++)
                {
                    /*
                     * Load an annotation[] slot, then point to
                     * next annotation in class file image.
                     */
                    pabytes =
                        cfattrib_load_annotation(
                            pcfs,
                            /* (annotation_mem_align **) */
                           (PTR_ATR_RUNTIMEVISIBLEANNOTATIONS_AI(ppaima)
                                 ->annotations)[antidx],
                            (annotation *) pabytes);

                    LOAD_SYSCALL_FAILURE_ANNOTATION(
                        (rnull == pabytes),
                        "load runtime visible annotation",
                        *ppaima,
                        PTR_ATR_RUNTIMEVISIBLEANNOTATIONS_AI(ppaima)
                          ->annotations);
                } /* for (antidx) */
            }


            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(
                     CONSTANT_UTF8_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-34 Needs unit testing.
             */

            pabytes = (jbyte *)
                      &((RuntimeInvisibleAnnotations_attribute *) pai)
                         ->num_annotations;

            MAKE_PU2(pu2, pabytes);

            atbllen = GETRS2(pu2);
            pu2++;
            PTR_ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(ppaima)
              ->num_annotations = atbllen;

            pabytes = (jbyte *) pu2;

            if (0 == atbllen)
            {
                PTR_ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(ppaima)
                  ->annotations =
                    (annotation_mem_align **) rnull;
            }
            else
            {
                PTR_ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(ppaima)
                  ->annotations =
                    HEAP_GET_METHOD(
                          PTR_ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(ppaima)
                                 ->num_annotations *
                               sizeof(annotation_mem_align *),
                             rtrue);

                jvm_annotation_index antidx;

                for (antidx = 0;
                     antidx <
        PTR_ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(ppaima)->num_annotations;
                     antidx++)
                {
                    /*
                     * Load an annotation[] slot, then point to
                     * next annotation in class file image.
                     */
                    pabytes =
                        cfattrib_load_annotation(
                            pcfs,
                            /* (annotation_mem_align **) */
                         (PTR_ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(ppaima)
                                 ->annotations)[antidx],
                            (annotation *) pabytes);

                    LOAD_SYSCALL_FAILURE_ANNOTATION(
                        (rnull == pabytes),
                        "load runtime invisible annotation",
                        *ppaima,
                        PTR_ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(ppaima)
                          ->annotations);
                } /* for (antidx) */
            }


            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(
                   CONSTANT_UTF8_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-35 Needs unit testing.
             */

            pabytes = (jbyte *)
                 &((RuntimeVisibleParameterAnnotations_attribute *) pai)
                         ->num_parameters;

            MAKE_PU2(pu2, pabytes);

            parmtbllen = GETRS2(pu2);
            pu2++;
            PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
              ->num_parameters = parmtbllen;

            pabytes = (jbyte *) pu2;

            if (0 == parmtbllen)
            {
                PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                  ->parameter_annotations = 
                    (parameter_annotation **) rnull;
            }
            else
            {
                PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                  ->parameter_annotations = 
                    HEAP_GET_METHOD(
                   PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                        ->num_parameters *
                          sizeof(parameter_annotation *),
                      rtrue);

                for (parmtblidx = 0;
                     parmtblidx < parmtbllen;
                     parmtblidx++)
                {
                    /*
                     * Load an parameter_annotation[] slot, then point
                     * to next annotation in class file image.
                     */

                    atbllen = GETRS2(pu2);
                    pu2++;
                   PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                      ->parameter_annotations[parmtblidx]
                        ->num_annotations = atbllen;

                    pabytes = (jbyte *) pu2;

                    if (0 == atbllen)
                    {
                   PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                          ->parameter_annotations[parmtblidx]
                            ->annotations =
                            (annotation_mem_align **) rnull;
                    }
                    else
                    {
                   PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                        ->parameter_annotations[parmtblidx]
                          ->annotations =
                            HEAP_GET_METHOD(
                   PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                        ->parameter_annotations[parmtblidx]
                          ->num_annotations *
                                         sizeof(annotation_mem_align *),
                                            rtrue);

                        jvm_annotation_index antidx;

                        for (antidx = 0;
                             antidx <
                   PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                               ->parameter_annotations[parmtblidx]
                                 ->num_annotations;
                             antidx++)
                        {
                            /*
                             * Load an annotation[] slot, then point to
                             * next annotation in class file image.
                             */
                            pabytes =
                                cfattrib_load_annotation(
                                    pcfs,
                                    /* (annotation_mem_align **) */
                  (PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                                     ->parameter_annotations[parmtblidx]
                                       ->annotations)[antidx],
                                    (annotation *) pabytes);

                            LOAD_SYSCALL_FAILURE_ANNOTATION(
                                (rnull == pabytes),
                            "load runtime visible parameter annotation",
                                *ppaima,
                   PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                                  ->parameter_annotations[parmtblidx]
                                    ->annotations);
                        } /* for (antidx) */
                    }
                } /* for parmtblidx */
            }


            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(
            CONSTANT_UTF8_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-36 Needs unit testing.
             */

            pabytes = (jbyte *)
               &((RuntimeInvisibleParameterAnnotations_attribute *) pai)
                         ->num_parameters;

            MAKE_PU2(pu2, pabytes);

            parmtbllen = GETRS2(pu2);
            pu2++;
            PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
              ->num_parameters = parmtbllen;

            pabytes = (jbyte *) pu2;

            if (0 == parmtbllen)
            {
                PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                  ->parameter_annotations = 
                    (parameter_annotation **) rnull;
            }
            else
            {
                PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                  ->parameter_annotations = 
                    HEAP_GET_METHOD(
                 PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                        ->num_parameters *
                          sizeof(parameter_annotation *),
                      rtrue);

                for (parmtblidx = 0;
                     parmtblidx < parmtbllen;
                     parmtblidx++)
                {
                    /*
                     * Load an parameter_annotation[] slot, then point
                     * to next annotation in class file image.
                     */

                    atbllen = GETRS2(pu2);
                    pu2++;
                 PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                      ->parameter_annotations[parmtblidx]
                        ->num_annotations = atbllen;

                    pabytes = (jbyte *) pu2;

                    if (0 == atbllen)
                    {
                 PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                          ->parameter_annotations[parmtblidx]
                            ->annotations =
                            (annotation_mem_align **) rnull;
                    }
                    else
                    {
                 PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                        ->parameter_annotations[parmtblidx]
                          ->annotations =
                            HEAP_GET_METHOD(
                 PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                        ->parameter_annotations[parmtblidx]
                          ->num_annotations *
                                         sizeof(annotation_mem_align *),
                                            rtrue);

                        jvm_annotation_index antidx;

                        for (antidx = 0;
                             antidx <
                 PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                               ->parameter_annotations[parmtblidx]
                                 ->num_annotations;
                             antidx++)
                        {
                            /*
                             * Load an annotation[] slot, then point to
                             * next annotation in class file image.
                             */
                            pabytes =
                                cfattrib_load_annotation(
                                    pcfs,
                                    /* (annotation_mem_align **) */
                (PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                                     ->parameter_annotations[parmtblidx]
                                       ->annotations)[antidx],
                                    (annotation *) pabytes);

                            LOAD_SYSCALL_FAILURE_ANNOTATION(
                                (rnull == pabytes),
                            "load runtime visible parameter annotation",
                                *ppaima,
                 PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima)
                                  ->parameter_annotations[parmtblidx]
                                    ->annotations);
                        } /* for (antidx) */
                    }
                } /* for parmtblidx */
            }


            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(
          CONSTANT_UTF8_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE))
            {
              return(pnext_ai);
            }
            break;

        case LOCAL_ANNOTATIONDEFAULT_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-37 Needs unit testing.
             */

            pabytes = (jbyte *)
                      &((AnnotationDefault_attribute *) pai)
                         ->default_value;


            pabytes = cfattrib_load_elementvalue(pcfs,
                           &PTR_ATR_ANNOTATIONDEFAULTMEMALIGN_AI(ppaima)
                                                 ->default_value, 
                                             (element_value *) pabytes);


            /* Verify contents of @c @b attribute_name_index string */
            if (0 == CMP_ATTRIBUTE(
                             CONSTANT_UTF8_ANNOTATIONDEFAULT_ATTRIBUTE))
            {
                return(pnext_ai);
            }
            break;

        case LOCAL_UNKNOWN_ATTRIBUTE:

        default:
            /*
             * Concerning a permanent implementation,
             * and per spec section 4.7.1, all unrecognized
             * attributes must be ignored.  For completeness and
             * for ease of future attribute implementation, such
             * attribute is at least copied into memory, although
             * this could be eliminated to conserve space.
             *
             * @todo HARMONY-6-jvm-cfattrib.c-22 Evaluate whether or not
             * this is the best way to handle this case, that is,
             * should the attribute be loaded, or should the heap area
             * be freed and a null returned?  Probably load and ignore.
             */
            if (0 != tmpatr.attribute_length)
            {
                portable_memcpy(&(*ppaima)->ai.info,
                                &pai->info,
                                tmpatr.attribute_length);
            }

            /*
             * Nothing in contents of @c @b attribute_name_index
             * that needs to be verified since contents is an
             * unknown attribute.
             */
            return(pnext_ai);

    } /* switch atrenum */

    /*
     * Something went wrong here. free heap allocation and report error
     */
    HEAP_FREE_METHOD(*ppaima);
    return((u1 *) rnull);

} /* END of cfattrib_load_attribute() */


/*!
 * @brief Unload an attribute and free its heap area.
 *
 * @param  pcfs      Pointer to (partially) parsed ClassFile area
 *
 * @param  paima     Pointer to a attribute_info_mem_align allocation
 *                   where this attribute is stored in the heap
 *
 *
 * @returns @link #rvoid rvoid@endlink Whether it succeeds or fails,
 *          returning anything does not make much sense.  This is
 *          similar to @c @b free(3) not returning anything even when
 *          a bad pointer was passed in.
 *
 */

rvoid cfattrib_unload_attribute(ClassFile                *pcfs,
                                attribute_info_mem_align *paima)
{
    ARCH_FUNCTION_NAME(cfattrib_unload_attribute);

    u2  num_pointers, num_pointers2;
    int ptridx, ptridx2;

    parameter_annotation **ppa;
    annotation_mem_align **pa;

    /* Ignore any NULL pointers, nothing to do (should NEVER happen) */
    if ((rnull == pcfs) || (rnull == paima))
    {
        return;
    }

    /*
     * Free attribute data from the heap.  The @b info item is optional,
     * depending on which attribute this is.  Free it directly
     * unless it is a Code_attribute, then do that specially.
     * All other variable-length attributes have the SINGLE variable
     * length item at the END of the structure (except annotatons and
     * local variable type attributes, which are being ignored by this
     * implementation);  therefore, there is no need to play pointer
     * games to adjust what is stored where in those attributes.
     * Simply index that array[] item and free it.
     *
     * Also investigate attribute type constraints using a
     * string-based @c @b switch .
     */

    classfile_attribute_enum atrenum =
        cfattrib_atr2enum(pcfs, paima->ai.attribute_name_index);

    switch (atrenum)
    {
        case LOCAL_CODE_ATTRIBUTE:
            /*
             * Free Code_attribute, all allocations in reverse order.
             * (Should not make any difference, but this is how the
             * @c @b constant_pool is being freed, so just do it the
             * same way, namely, the reverse order in which allocations
             * were made.)
             */

            if (0 == ATR_CODE_AI(paima)->attributes_count)
            {
                /*
                 * Possible, and theoretically reasonable.
                 *  Nothing to do here.
                 */
                ;
            }
            else
            {
                /*
                 * WARNING!  RECURSIVE CALL:
                 *
                 * Load up each attribute in this attribute area.
                 * This should NOT recurse more than once since there
                 * can ONLY be ONE Code_attribute per method.
                 */
                jvm_attribute_index atridx;

                for (atridx = 0;
                     atridx < ATR_CODE_AI(paima)->attributes_count;
                     atridx++)
                {
                    /*
                     * Unload an attribute
                     *
                     *
                     * WARNING!  RECURSIVE CALL:
                     *
                     * Unload a nested attribute, which may invoke this
                     * function for a nested annotation.  This may go as
                     * deep as the nesting of attribute data, which will
                     * be inherently finite for a valid class file, but
                     * the actual depth is unknown at this point.
                     */

                    cfattrib_unload_attribute(pcfs,
                                ATR_CODE_AI(paima)->attributes[atridx]);

                } /* for (atridx) */

                HEAP_FREE_METHOD(ATR_CODE_AI(paima)->attributes);
            }

            if (0 == ATR_CODE_AI(paima)->exception_table_length)
            {
                /*
                 * Possible, and theoretically reasonable.
                 *  Nothing to do here.
                 */
                ;
            }
            else
            {
                HEAP_FREE_METHOD(ATR_CODE_AI(paima)->exception_table);
            }

            if (0 == ATR_CODE_AI(paima)->code_length)
            {
                /*
                 * Possible, but not theoretically reasonable,
                 * thus prohibited by spec.  Nothing to do here
                 * since @link Code_attribute#code code@endlink
                 * pointer is @link #rnull rnull@endlink.
                 */
                ;
            }
            else
            {
                HEAP_FREE_METHOD(ATR_CODE_AI(paima)->code);
            }


            /* Finally, free the main attribute area itself */
            HEAP_FREE_METHOD(paima);

            break;

        case LOCAL_EXCEPTIONS_ATTRIBUTE:
            HEAP_FREE_METHOD(paima);

            break;

        case LOCAL_INNERCLASSES_ATTRIBUTE:
            HEAP_FREE_METHOD(paima);
            break;

        case LOCAL_LINENUMBERTABLE_ATTRIBUTE:
            HEAP_FREE_METHOD(paima);
            break;

        case LOCAL_LOCALVARIABLETABLE_ATTRIBUTE:
            HEAP_FREE_METHOD(paima);
            break;

        case LOCAL_LOCALVARIABLETYPETABLE_ATTRIBUTE:
            HEAP_FREE_METHOD(paima);
            break;

        case LOCAL_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-41 Needs unit testing.
             */

            num_pointers =
             ATR_RUNTIMEVISIBLEANNOTATIONS_AI(paima)->num_annotations;

            for (ptridx = 0; ptridx < num_pointers; ptridx++)
            {
                cfattrib_unload_annotation(
                    pcfs,
                    ATR_RUNTIMEVISIBLEANNOTATIONS_AI(paima)
                      ->annotations[ptridx]);
            }
            HEAP_FREE_METHOD(ATR_RUNTIMEVISIBLEANNOTATIONS_AI(paima)
                               ->annotations);
            HEAP_FREE_METHOD(paima);
            break;

        case LOCAL_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-42 Needs unit testing.
             */

            num_pointers =
                ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(paima)
                  ->num_annotations;

            for (ptridx = 0; ptridx < num_pointers; ptridx++)
            {
                cfattrib_unload_annotation(
                    pcfs,
                    ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(paima)
                      ->annotations[ptridx]);
            }
            HEAP_FREE_METHOD(ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(paima)
                               ->annotations);
            HEAP_FREE_METHOD(paima);
            break;

        case LOCAL_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-43 Needs unit testing.
             */

            num_pointers =
                ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(paima)
                  ->num_parameters;

            ppa = ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(paima)
                    ->parameter_annotations;

            for (ptridx = 0; ptridx < num_pointers; ptridx++)
            {
                num_pointers2 = ppa[ptridx]->num_annotations;
                pa            = ppa[ptridx]->annotations;

                for (ptridx2 = 0; ptridx2 < num_pointers2; ptridx2++)
                {
                    cfattrib_unload_annotation(pcfs, pa[ptridx2]);
                }
                HEAP_FREE_METHOD(pa);
            }
            HEAP_FREE_METHOD(ppa);
            HEAP_FREE_METHOD(paima);
            break;

        case LOCAL_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE:
            /*!
             * @todo HARMONY-6-jvm-cfattrib.c-44 Needs unit testing.
             */

            num_pointers =
                ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(paima)
                  ->num_parameters;

            ppa = ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(paima)
                    ->parameter_annotations;

            for (ptridx = 0; ptridx < num_pointers; ptridx++)
            {
                num_pointers2 = ppa[ptridx]->num_annotations;
                pa            = ppa[ptridx]->annotations;

                for (ptridx2 = 0; ptridx2 < num_pointers2; ptridx2++)
                {
                    cfattrib_unload_annotation(pcfs, pa[ptridx2]);
                }
                HEAP_FREE_METHOD(pa);
            }
            HEAP_FREE_METHOD(ppa);
            HEAP_FREE_METHOD(paima);
            break;

        case LOCAL_ANNOTATIONDEFAULT_ATTRIBUTE:
            cfattrib_unload_elementvalue(
                pcfs,
               &ATR_ANNOTATIONDEFAULTMEMALIGN_AI(paima)->default_value);
            HEAP_FREE_METHOD(paima);
            break;

        case LOCAL_CONSTANTVALUE_ATTRIBUTE:
        case LOCAL_ENCLOSINGMETHOD_ATTRIBUTE:
        case LOCAL_SYNTHETIC_ATTRIBUTE:
        case LOCAL_SIGNATURE_ATTRIBUTE:
        case LOCAL_SOURCEFILE_ATTRIBUTE:
        case LOCAL_DEPRECATED_ATTRIBUTE:

        case LOCAL_UNKNOWN_ATTRIBUTE:
        default:
            /*
             * Only the variable-length attributes have any logic that
             * needs to be invoked for freeing up the heap areas.  The
             * constant-size attributes are simply freed.
             */
            HEAP_FREE_METHOD(paima);
            break;

    } /* switch atrenum */

    return;

} /* END of cfattrib_unload_attribute() */


/* EOF */
