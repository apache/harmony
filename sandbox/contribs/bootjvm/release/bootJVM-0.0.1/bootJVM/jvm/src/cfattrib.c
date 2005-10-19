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


/* #include <string.h> */

#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "util.h"
#include "utf.h"


/*!
 * @brief Convenient shorthand for attribute string comparison
 *
 *
 * @param string  Attribute string for comparison against ClassFile
 *                attribute_info value.
 *
 *
 * @returns integer from utf_prchar_pcfs_strcmp()
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
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_SIGNATURE_ATTRIBUTE))
    {
        return(LOCAL_SIGNATURE_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_SYNTHETIC_ATTRIBUTE))
    {
        return(LOCAL_SYNTHETIC_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_SOURCEFILE_ATTRIBUTE))
    {
        return(LOCAL_SOURCEFILE_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_LINENUMBERTABLE_ATTRIBUTE))
    {
        return(LOCAL_LINENUMBERTABLE_ATTRIBUTE);
    }
    else
    if (0 == CMP_ATTRIBUTE(CONSTANT_UTF8_LOCALVARIABLETABLE_ATTRIBUTE))
    {
        return(LOCAL_LOCALVARIABLETABLE_ATTRIBUTE);
    }
    else
    if (0 ==
          CMP_ATTRIBUTE(CONSTANT_UTF8_LOCALVARIABLETYPETABLE_ATTRIBUTE))
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
 * @name Code_attribute deferencing support.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Conveniently reference the Code_attribute contained in
 * an indirect <b><code>attribute_info_dup **dst</code></b>.
 *
 * After putting in the 4-byte access alignment changes,
 * it became obvious that what was once (*dst)->member
 * had become quite cumbersome.  Therefore, in order to
 * simplify access through (attribute_info_dup *)->ai.member
 * constructions, including appropriate casting, the following
 * macro is offered to take care of the most common usage.
 * The few other references are unchanged, and the code is
 * easier to understand.
 *
 * Notice that @link #DST_AI() DST_AI()@endlink references
 * a <b><code>attribute_info_dup *dst</code></b>, while this
 * macro references a <b><code>attribute_info_dup **dst</code></b>.
 */
#define PTR_DST_AI(dst) ((Code_attribute *) &(*dst)->ai)

/*!
 * @brief Conveniently reference the Code_attribute contained in
 * a <b><code>attribute_info_dup *dst</code></b>, namely, with less
 * pointer indirection.
 *
 * This is a counterpart for cfattrib_unloadattribute() where
 * no indirection is needed.  Notice that
 * @link #PTR_DST_AI() PTR_DST_AI()@endlink references
 * a <b><code>attribute_info_dup **dst</code></b>, while this
 * macro references a <b><code>attribute_info_dup *dst</code></b>.
 *
 */
#define     DST_AI(dst) ((Code_attribute *) &dst->ai)

/*@} */ /* End of grouped definitions */


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
 * @param  dst       Pointer to a attribute_info_dup[] address telling
 *                   where in the heap this attribute will be
 *                   copied from the source area.
 *
 * @param  src       Pointer to an attribute in class file image.
 *                   This data will be stored in the heap at
 *                   location @c @b *dst .
 *
 *
 * @returns Point to first byte past this attribute,
 *          or @link #rnull rnull@endlink if parsing problem.
 *          If there is a problem, @b dst will contain a valid
 *          heap pointer only if there is a valid
 *          @link attribute_info.attribute_name_index
            attribute_name_index@endlink, else it will also
 *          be @link #rnull rnull@endlink.
 *
 */

u1 *cfattrib_loadattribute(ClassFile           *pcfs,
                           attribute_info_dup **dst,
                           attribute_info      *src)
{
    ARCH_FUNCTION_NAME(cfattrib_loadattribute);

    attribute_info tmpatr;
    u4             tmplen;

    jbyte *pnext_src = (jbyte *) src;

    tmpatr.attribute_name_index = GETRS2(&src->attribute_name_index);
    tmpatr.attribute_length     = GETRI4(&src->attribute_length);

    cfmsgs_typemsg("cfattrib_loadattribute",
                   pcfs,
                   tmpatr.attribute_name_index);
    sysDbgMsg(DMLNORM,
              arch_function_name,
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
 
    /* Calculate total size of this (attribute_info) area in src */
    tmplen = sizeof(attribute_info)
             - sizeof(u1)
             + tmpatr.attribute_length;

    /*
     * Skip past this item's header and contents to next attribute,
     * adjusting for dummy [info] field.
     */
    pnext_src += tmplen;


    /* Calculate total size of this (attribute_info_dup) area in dst */
    tmplen = sizeof(attribute_info_dup)
             - sizeof(u1)
             + tmpatr.attribute_length;

    /* Allocate a heap location to store this attribute */
    *dst = (attribute_info_dup *) HEAP_GET_METHOD(tmplen, rfalse);

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
    (*dst)->empty[0] = FILL_INFO_DUP0;
    (*dst)->empty[1] = FILL_INFO_DUP1;
    (*dst)->ai.attribute_name_index = tmpatr.attribute_name_index;
    (*dst)->ai.attribute_length = tmpatr.attribute_length;

    classfile_attribute_enum atrenum =
        cfattrib_atr2enum(pcfs, (*dst)->ai.attribute_name_index);

    if (LOCAL_CODE_ATTRIBUTE == atrenum)
    {
        /*
         * Copy Code_attribute field-by-field as in the top-level
         * structure, allocating array[] pieces in that same manner.
         *
         * @note  You can get away with referencing
         *        @c @b src->member here because these first
         *        few members are of fixed length.  You @e still must
         *        use GETRS2() or GETRI4() because they are in a
         *        byte-stream class file, which has no guaranteed
         *        word alignment.
         */
        PTR_DST_AI(dst)->max_stack =
            GETRS2(&((Code_attribute *) src)->max_stack);

        PTR_DST_AI(dst)->max_locals =
            GETRS2(&((Code_attribute *) src)->max_locals);

        PTR_DST_AI(dst)->code_length =
            GETRI4(&((Code_attribute *) src)->code_length);

        if (0 == PTR_DST_AI(dst)->code_length)
        {
            /*
             * Possible, but not theoretically reasonable,
             * thus prohibited by spec
             */
            PTR_DST_AI(dst)->code = (u1 *) rnull;
        }
        else
        {
            PTR_DST_AI(dst)->code =
                HEAP_GET_METHOD(PTR_DST_AI(dst)->code_length *
                                    sizeof(u1),
                                rfalse);

            /* Notice this is copy TO *ptr and FROM *bfr, not *ptr! */
            portable_memcpy( PTR_DST_AI(dst)->code,
                            /* 1st var len fld in class file code area*/
                            &((Code_attribute *)  src)->code,
                           (PTR_DST_AI(dst)->code_length) * sizeof(u1));

        }

        /*
         * Load up variable fields now, use same technique as
         * elsewhere.
         *
         */
        jbyte *pabytes =(jbyte *) &((Code_attribute *) src)->code;
        pabytes += PTR_DST_AI(dst)->code_length * sizeof(u1);

        u2 *pu2;
        MAKE_PU2(pu2, pabytes);

        PTR_DST_AI(dst)->exception_table_length=GETRS2(pu2++);

        pabytes = (u1 *) pu2;

        if (0 == PTR_DST_AI(dst)->exception_table_length)
        {
            /* Possible, and theoretically reasonable */
            PTR_DST_AI(dst)->exception_table = (rvoid *) rnull;
        }
        else
        {
            PTR_DST_AI(dst)->exception_table =
              HEAP_GET_METHOD(PTR_DST_AI(dst)->exception_table_length *
                                  sizeof(exception_table_entry),
                              rfalse);

            portable_memcpy(&PTR_DST_AI(dst)->exception_table,
                            pabytes,
                            (PTR_DST_AI(dst)->exception_table_length) *
                                sizeof(exception_table_entry));

            pabytes += (PTR_DST_AI(dst)->exception_table_length) *
                             sizeof(exception_table_entry);
        }

        MAKE_PU2(pu2, pabytes);

        PTR_DST_AI(dst)->attributes_count = GETRS2(pu2++);

        pabytes = (jbyte *) pu2;

        if (0 == PTR_DST_AI(dst)->attributes_count)
        {
            PTR_DST_AI(dst)->attributes =
                (attribute_info_dup **) rnull;
        }
        else
        {
            PTR_DST_AI(dst)->attributes =
                HEAP_GET_METHOD(PTR_DST_AI(dst)->attributes_count *
                                    sizeof(attribute_info_dup *),
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
                 atridx < PTR_DST_AI(dst)->attributes_count;
                 atridx++)
            {
                /*
                 * Load an attribute and verify that it is either
                 * a valid (or an ignored) attribute, then point to
                 * next attribute in class file image.
                 */

                pabytes =
                    cfattrib_loadattribute(
                        pcfs,
                        (attribute_info_dup **)
                            &(PTR_DST_AI(dst)->attributes)[atridx],
                        (attribute_info *) pabytes);

                LOAD_SYSCALL_FAILURE_ATTRIB((rnull == pabytes),
                                            "load field attribute",
                                           *dst,
                                           PTR_DST_AI(dst)->attributes);

            } /* for (atridx) */
        }

    } /* if LOCAL_CODE_ATTRIBUTE */
    else if (LOCAL_CONSTANTVALUE_ATTRIBUTE == atrenum) 
    {
        if (0 != tmpatr.attribute_length)
        {
            portable_memcpy(&(*dst)->ai.info,
                            &src->info,
                            tmpatr.attribute_length);
        }

        /*
         *  we need to swap the index for the value
         */
        ((ConstantValue_attribute *) &(*dst)->ai)
          ->constantvalue_index =
              GETRS2(&((ConstantValue_attribute *) &(*dst)->ai)
                        ->constantvalue_index);

    } /* if LOCAL_CONSTANT_ATTRIBUTE */
    else
    {
        /*!
         * @todo HARMONY-6-jvm-cfattrib.c-2
         *       $$$ GMJ: I don't agree... things need to be byteswapped
         *       for indices and such.
         *       DL:  Geir is _absolutely_ right.  I've been living
         *            in big-endian land and forgot about this.
         */

        /*
         * See comments at top of @c @b if statment as to why
         * all other structures can be directly copied into the heap
         * area. The only other variable-length attributes are the
         * annotation and local variable type attributes, which
         * are being ignored by this implementation.
         */
        if (0 != tmpatr.attribute_length)
        {
            portable_memcpy(&(*dst)->ai.info,
                            &src->info,
                            tmpatr.attribute_length);
        }

    } /* if LOCAL_CODE_ATTRIBUTE else */


    /*!
     * @todo HARMONY-6-jvm-cfattrib.c-3 Delete this when
     *  TODO: items below are satisfied
     */
    rboolean dummy = rtrue;

    switch(atrenum)
    {
        case LOCAL_CONSTANTVALUE_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-4 Verify "ConstantValue"
             * attribute contents
             */
            dummy = rfalse;

            break;

        case LOCAL_CODE_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-5 Verify "Code" attribute
             *       contents
             */
            dummy = rfalse;

            break;

        case LOCAL_EXCEPTIONS_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-6 Verify "Exceptions"
             *       attribute  contents
             */
            dummy = rfalse;

            break;

        case LOCAL_INNERCLASSES_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-7 Verify "InnerClasses"
             *       attribute contents
             */
            dummy = rfalse;

            break;

        case LOCAL_ENCLOSINGMETHOD_ATTRIBUTE:

           /*!
             * @todo HARMONY-6-jvm-cfattrib-8 "EnclosingMethod"
             *       attribute has nothing to verify
             */
            dummy = rfalse;

            break;

        case LOCAL_SIGNATURE_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-9 "Signature" attribute 
             *       has nothing to verify
             */
            dummy = rfalse;

            break;

        case LOCAL_SYNTHETIC_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-10 "Synthetic" attribute
             *       has nothing to verify
             */
            dummy = rfalse;

            break;

        case LOCAL_SOURCEFILE_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-11 Verify "SourceFile"
             *       attribute contents
             */
            dummy = rfalse;

            break;

        case LOCAL_LINENUMBERTABLE_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-12 Verify "LineNumberTable" 
             *       attribute contents
             */
            dummy = rfalse;

            break;

        case LOCAL_LOCALVARIABLETABLE_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-13 Verify
             *       "LocalVariableTable" attribute contents
             */
            dummy = rfalse;

            break;

        case LOCAL_LOCALVARIABLETYPETABLE_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-14 Verify
             *       "LocalVariableTypeTable" attribute
                     contents
             */
            dummy = rfalse;

            break;

        case LOCAL_DEPRECATED_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-15 "Deprecated" attribute
             *       has nothing to verify
             */
            dummy = rfalse;

            break;

        case LOCAL_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-16 Verify
             *       "RuntimeVisibleAnnotations" attribute contents
             */
            dummy = rfalse;

            break;

        case LOCAL_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-17 Verify
             *       "RuntimeInvisibleAnnotations" attribute contents
             */
            dummy = rfalse;

            break;

        case LOCAL_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-18 Verify
             *       "RuntimeVisibleParameterAnnotations" contents
             */
            dummy = rfalse;

            break;

        case LOCAL_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-19 Verify
             *       "RuntimeInvisibleParameterAnnotations" contents
             */
            dummy = rfalse;

            break;

        case LOCAL_ANNOTATIONDEFAULT_ATTRIBUTE:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-20 "AnnotationDefault"
             *       attribute has nothing to verify
             */
            dummy = rfalse;

            break;

        case LOCAL_UNKNOWN_ATTRIBUTE:
        default:

            /*!
             * @todo HARMONY-6-jvm-cfattrib-21 Ignore unrecognized
             *       attribute.  There really should not be anything
             *       to do here since the return value
             *       already points to the next attribute.
             */
            dummy = rfalse;

    } /* switch(atrenum) */

    return(pnext_src);

} /* END of cfattrib_loadattribute() */


/*!
 * @brief UnLoad an attribute and free its heap area.
 *
 * @param  pcfs      Pointer to (partially) parsed ClassFile area
 *
 * @param  dst       Pointer to a attribute_info_dup allocation
 *                   where this attribute is stored in the heap
 *
 *
 * @returns @link #rvoid rvoid@endlink Whether it succeeds or fails,
 *          returning anything does not make much sense.  This is
 *          similar to @c @b free(3) not returning anything even when
 *          a bad pointer was passed in.
 *
 */

rvoid cfattrib_unloadattribute(ClassFile  *pcfs,
                               attribute_info_dup *dst)
{
    ARCH_FUNCTION_NAME(cfattrib_unloadattribute);

    /* Ignore any NULL pointers, nothing to do (should NEVER happen) */
    if ((rnull == pcfs) || (rnull == dst))
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

    if (rtrue == cfattrib_iscodeattribute(pcfs,
                                     DST_AI(dst)->attribute_name_index))

    {
        /*
         * Free Code_attribute, all allocations in reverse order.
         * (Should not make any difference, but this is how the
         * @c @b constant_pool is being freed, so just do it the same
         * way, namely, the reverse order in which allocations
         * were made.)
         */

        if (0 == DST_AI(dst)->attributes_count)
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
                 atridx < DST_AI(dst)->attributes_count;
                 atridx++)
            {
                /*
                 * Unload an attribute
                 */

                cfattrib_unloadattribute(pcfs,
                                       DST_AI(dst)->attributes[atridx]);

            } /* for (atridx) */

            HEAP_FREE_METHOD(DST_AI(dst)->attributes);
        }

        if (0 == DST_AI(dst)->exception_table_length)
        {
            /*
             * Possible, and theoretically reasonable.
             *  Nothing to do here.
             */
            ;
        }
        else
        {
            HEAP_FREE_METHOD(DST_AI(dst)->exception_table);
        }

        if (0 == DST_AI(dst)->code_length)
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
            HEAP_FREE_METHOD(DST_AI(dst)->code);
        }


        /* Finally, free the main attribute area itself */
        HEAP_FREE_METHOD(dst);

    } /* if LOCAL_CODE_ATTRIBUTE */
    else
    {
        /*
         * See comments at top of @c @b if statment as to
         * why all other structures can be directly freed from the
         * heap area. The only other variable-length attributes are
         * the annotation and local variable type attributes, which
         * are being ignored by this implementation.
         */
        HEAP_FREE_METHOD(dst);

    } /* if LOCAL_CODE_ATTRIBUTE else */


    return;

} /* END of cfattrib_unloadattribute() */


/* EOF */
