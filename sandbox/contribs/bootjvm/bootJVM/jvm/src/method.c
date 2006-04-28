/*!
 * @file method.c
 *
 * @brief Manipulate ClassFile methods.
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
ARCH_SOURCE_COPYRIGHT_APACHE(method, c,
"$URL$",
"$Id$");


#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "exit.h"
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h"
#include "method.h"
#include "nts.h"
#include "opcode.h"
#include "utf.h"


/*!
 * @brief Locate the method_info index for a normal method in a class
 * using a @c @b constant_pool entry to the name and description of
 * the method.
 *
 *
 * @param  clsidx            Class index of class whose method is to be
 *                           located.
 *
 * @param  mthname           UTF8 @c @b constant_pool entry of name of
 *                           method in class.
 *
 * @param  mthdesc           UTF8 @c @b constant_pool entry of
 *                           description of method parameters and
 *                           return type.
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
jvm_method_index method_find_by_cp_entry(jvm_class_index    clsidx,
                                         cp_info_mem_align *mthname,
                                         cp_info_mem_align *mthdesc)
{
    ARCH_FUNCTION_NAME(method_find_by_cp_entry);

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
    ARCH_FUNCTION_NAME(method_find_by_prchar);

    cp_info_mem_align *pcip_mthname = nts_prchar2utf(mthname);
    cp_info_mem_align *pcip_mthdesc = nts_prchar2utf(mthdesc);

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
 * @param mthdescidx  Class file @c @b constant_pool index of method
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
    ARCH_FUNCTION_NAME(method_return_type);

    cp_info_mem_align  *pcpma;
    CONSTANT_Utf8_info *pcpma_Utf8;

    pcpma=CLASS_OBJECT_LINKAGE(clsidx)->pcfs->constant_pool[mthdescidx];
    pcpma_Utf8 = PTR_THIS_CP_Utf8(pcpma);

    u2 idx;
           /* Last char will be result:/ - 1/ except 'Lsome/class;' */
    for(idx = 0; idx < pcpma_Utf8->length - 1; idx++)
    {
        /* Scan for parm list closure, next char is return type */
        if (METHOD_CHAR_CLOSE_PARM == pcpma_Utf8->bytes[idx])
        {
            switch (pcpma_Utf8->bytes[idx + 1])
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
                    return((jvm_basetype) pcpma_Utf8->bytes[idx + 1]);
                default:
                    /* Found something, but it was invalid */
                    return((jvm_basetype) LOCAL_BASETYPE_ERROR);
            }
        }
    }

    /*!
     * @todo  HARMONY-6-jvm-method.c-1 Should this throw a
     *        @b VerifyError instead? Is it better to let
     *        caller do this?  Same for above @c @b default case.
     */

    /* Error, something else found */
    return((jvm_basetype) LOCAL_BASETYPE_ERROR);

} /* END of method_return_type() */


/*!
 * @brief Calculate size of method parameter blocks from descriptor.
 *
 * This size is represented in terms of @link #jint jint@endlink words
 * of JVM stack space.  This information is typically used when
 * creating a stack frame for a virtual method call to compare against
 * that method's local variable requirements, which must be equal or
 * greater than the parameter block requirements.  (Otherwise a
 * @b VerifyError occurs.)
 *
 * @param clsidx       Class table index of method to examine.
 *
 * @param mthdescidx   Class file @c @b constant_pool index of method
 *                     descriptor to examine.  This entry must be a
 *                     CONSTANT_Utf8_info string containing the
 *                     descriptor of an unqualified method name.
 *
 *
 * @returns Number of @link #jint jint@endlink words of JVM stack space
 *          required for parameters to method @c @b mthdescidx.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_VERIFYERROR
 *         @link #JVMCLASS_JAVA_LANG_VERIFYERROR
 *         if a malformed descriptor is parsed@endlink.
 *
 */
rint method_parm_size(jvm_class_index         clsidx,
                      jvm_constant_pool_index mthdescidx)
{
    ARCH_FUNCTION_NAME(method_parm_size);

    cp_info_mem_align  *pcpma;
    CONSTANT_Utf8_info *pcpma_Utf8;

    pcpma=CLASS_OBJECT_LINKAGE(clsidx)->pcfs->constant_pool[mthdescidx];
    pcpma_Utf8 = PTR_THIS_CP_Utf8(pcpma);

    rboolean find_open_paren;
    find_open_paren = rtrue;

    rboolean find_class_close;
    find_class_close = rfalse;

    rint rc = 0;

    rboolean find_array_type;
    find_array_type = rfalse;

    u2 idx;
           /* Last char will be result:/ - 1/ except 'Lsome/class;' */
    for(idx = 0; idx < pcpma_Utf8->length - 1; idx++)
    {
        /* Skip past class/path/name until closing semicolon */
        if (rtrue == find_class_close)
        {
            if (BASETYPE_CHAR_L_TERM == pcpma_Utf8->bytes[idx])
            {
                find_class_close = rfalse;
            }

            /* Go on to next character, whether or not end of class */
            continue;
        }

        /* Scan for parm list opening, next char starts parm block */
        if (rtrue == find_open_paren)
        {
            /* If open paren found, start scanning descriptor */
            if (METHOD_CHAR_OPEN_PARM == pcpma_Utf8->bytes[idx])
            {
                /*
                 * If anything precedes the open paren,
                 * something is wrong
                 */
                if (0 != idx)
                {
                    exit_throw_exception(EXIT_JVM_METHOD,
                                        JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
                }

                find_open_paren = rfalse;
            }

            continue;
        }

        /* Done if close paren, being end of descriptor */
        if (METHOD_CHAR_CLOSE_PARM == pcpma_Utf8->bytes[idx])
        {
            /* Something is profoundly wrong if any condition is true */
            if ((rtrue == find_open_paren) ||
                (rtrue == find_class_close) ||
                (rtrue == find_array_type))
            {
                exit_throw_exception(EXIT_JVM_METHOD,
                                     JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
            }

            /* Return normally if everything was parsed properly */
            return(rc);
        }

        /* Scan for parm list closure, next char is return type */
        switch (pcpma_Utf8->bytes[idx])
        {
            case BASETYPE_CHAR_B:
            case BASETYPE_CHAR_C:
            case BASETYPE_CHAR_I:
            case BASETYPE_CHAR_S:
            case BASETYPE_CHAR_Z:
                if (rtrue == find_array_type)
                {
                    rc += sizeof(jvm_object_hash) / sizeof(jint);

                    find_array_type = rfalse;
                }
                else
                {
                    rc += sizeof(jint) / sizeof(jint);
                }
                break;

            case BASETYPE_CHAR_D:
                if (rtrue == find_array_type)
                {
                    rc += sizeof(jvm_object_hash) / sizeof(jint);

                    find_array_type = rfalse;
                }
                else
                {
                    rc += sizeof(jdouble) / sizeof(jint);
                }
                break;

            case BASETYPE_CHAR_F:
                if (rtrue == find_array_type)
                {
                    rc += sizeof(jvm_object_hash) / sizeof(jint);

                    find_array_type = rfalse;
                }
                else
                {
                    rc += sizeof(jfloat) / sizeof(jint);
                }
                break;

            case BASETYPE_CHAR_J:
                if (rtrue == find_array_type)
                {
                    rc += sizeof(jvm_object_hash) / sizeof(jint);

                    find_array_type = rfalse;
                }
                else
                {
                    rc += sizeof(jlong) / sizeof(jint);
                }
                break;

            case BASETYPE_CHAR_L:
                if (rtrue == find_array_type)
                {
                    find_array_type = rfalse;

                }

                rc += sizeof(jvm_object_hash) / sizeof(jint);

                /* Start scanning for end of class name */
                find_class_close = rtrue;

                break;

            case BASETYPE_CHAR_ARRAY:

                /*
                 * Type token will be a reference.
                 * Notice that it does not matter how many
                 * array dimensions are found, the result is
                 * still going to be a reference.
                 */
                find_array_type = rtrue;

                break;

            default:
                /* No more slots, cannot continue */
                exit_throw_exception(EXIT_JVM_METHOD,
                                     JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
        }
    }

    /* Something is profoundly wrong if closing paren was not found */
    exit_throw_exception(EXIT_JVM_METHOD,
                         JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/

    return((jint) 0); /* Satisfy compiler */

} /* END of method_parm_size() */


/*!
* @name Suggest a virtual opcode implied in method name and access flags
 *
 * These functions support native_run_method() by suggesting a virtual
 * operation code that should be used when calling it in the selected
 * locations outside of the JVM inner loop, namely, when a virtual
 * operation code is not otherwise available.
 *
 * @returns The most likely candidate for JVM operation code for this
 *          method, should it have been invoked from with the JVM inner
 *          loop.
 *
 *
 * @attention The return value does @e not represent a sure thing!
 *            This is because of the potential ambiguity of the
 *            algorithm, which is still sufficient for its initial
 *            purpose of supporting
 *            @link #POP_THIS_OBJHASH() POP_THIS_OBJHASH()@endlink in
 *            @link jvm/src/native.c native.c@endlink.
 *
 *
 * @todo  HARMONY-6-jvm-method.c-2 What criteria should be used to
 *        be able to return OPCODE_B9_INVOKEINTERFACE also?  The
 *        current usage is designed for use by
 *        @link #POP_THIS_OBJHASH() POP_THIS_OBJHASH()@endlink in
 *        @link jvm/src/native.c native.c@endlink, but it could
 *        be extended if needed.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Suggest virtual opcode from null-terminated method name string
 *
 * @param mthname      Method name
 *
 * @param access_flags The @link #ACC_PUBLIC ACC_xxx@endlink access
 *                     flags for this method.
 *
 */
jvm_virtual_opcode method_implied_opcode_from_prchar(
                                        rchar            *mthname,
                                        jvm_access_flags  access_flags)
{
    if (0 == portable_strcmp(LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR,
                             mthname))
    {
        return(OPCODE_B7_INVOKESPECIAL);
    }
    else
    if (0 == portable_strcmp(CONSTANT_UTF8_INSTANCE_CONSTRUCTOR,
                             mthname))
    {
        return(OPCODE_B7_INVOKESPECIAL);
    }
    else
    if (ACC_STATIC & access_flags)
    {
        return(OPCODE_B8_INVOKESTATIC);
    }
    else
    {
        return(OPCODE_B6_INVOKEVIRTUAL);
    }

} /* END of method_implied_opcode_from_prchar() */

/*!
 * @brief Suggest virtual opcode from @c @b constant_pool entry
 *
 * @param pcfs         Pointer to ClassFile area
 *
 * @param mthnameidx   Index into @c @b constant_pool for UTF8 method
 *                     name string
 *
 * @param access_flags The @link #ACC_PUBLIC ACC_xxx@endlink access
 *                     flags for this method.
 *
 */
jvm_virtual_opcode method_implied_opcode_from_cp_entry_pcfs(
                                  ClassFile              *pcfs,
                                  jvm_constant_pool_index mthnameidx,
                                  jvm_access_flags        access_flags)
{
    CONSTANT_Utf8_info *mthname =
        PTR_THIS_CP_Utf8(pcfs->constant_pool[mthnameidx]);

    if (0 == utf_prchar_strcmp(mthname,
                               LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR))
    {
        return(OPCODE_B7_INVOKESPECIAL);
    }
    else
    if (0 == utf_prchar_strcmp(mthname,
                               CONSTANT_UTF8_INSTANCE_CONSTRUCTOR))
    {
        return(OPCODE_B7_INVOKESPECIAL);
    }
    else
    if (ACC_STATIC & access_flags)
    {
        return(OPCODE_B8_INVOKESTATIC);
    }
    else
    {
        return(OPCODE_B6_INVOKEVIRTUAL);
    }

} /* END of method_implied_opcode_from_utf() */

/*@} */ /* End of grouped definitions */

/* EOF */
