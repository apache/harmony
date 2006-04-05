/*!
 * @file classutil.c
 *
 * @brief Utility and glue functions for
 * @link jvm/src/class.c class.c@endlink
 * and @c @b java.lang.Class
 *
 *
 * @internal Due to the fact that the implementation of the Java class
 *           and the supporting rclass structure is deeply embedded in
 *           the core of the development of this software, this file
 *           has some contents that come and go during development.
 *           Some functions get staged here before deciding where they
 *           @e really go; some are interim functions for debugging,
 *           some were glue that eventually went away.  Be careful to
 *           remove prototypes to such functions from the appropriate
 *           header file.
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
ARCH_SOURCE_COPYRIGHT_APACHE(classutil, c,
"$URL$",
"$Id$");


#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h"
#include "nts.h"
#include "utf.h"


/*!
 * @name Examine class indices for object hierarchy relationships.
 *
 *
 * If not already present, all classes examined will be loaded here
 * when their constant_pool[] or interfaced[] information is needed.
 *
 * @note <em>There is little runtime check performed</em> to check
 *       whether the input class indices represent classes, interfaces,
 *       or arrays.  Most of this must be done by the invoking function.
 *       For numerous examples,
 *       see @link #opcode_run() opcode_run()@endlink.
 *
 *
 * @param clsidx1  Class table index of a class to compare against the
 *                 second one, namely @c @b clsidx2 .
 *
 * @param clsidx2  Class table index of class to compare @c @b clsidx1
 *                 against.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Examine whether or not one class is a subclass of another
 *
 * This effectively asks the OO hierarchy question, "Is @c @b clsidx1
 * a @c @b clsidx2 ?" For an @link #rtrue rtrue@endlink comparison,
 * class @c @b clsidx1 must be either the same class as class
 * @c @b clsidx2 or a subclass of it.  In other words, the true
 * comparison affirms "@c @b clsidx1 is a @c @b clsidx2 ."
 *
 *
 * @returns @link #rtrue rtrue@endlink if @c @b clsidx1 is the same
 *          class or a subclass of @c @b clsidx2 .
 *          If either input parameter is
 *          @link #jvm_class_index_null jvm_class_index_null@endlink,
 *          then the result is @link #rfalse rfalse@endlink.
 *          If both input parameters are identical, then result
 *          is @link #rtrue rtrue@endlink.
 *
 */
rboolean classutil_class_is_a(jvm_class_index clsidx1,
                              jvm_class_index clsidx2)
{
    /* Disallow comparison with null class */
    if ((jvm_class_index_null == clsidx1) ||
        (jvm_class_index_null == clsidx2))
    {
        return(rfalse);
    }

    /* Comparison is true if both are the same class */
    if (clsidx1 == clsidx2)
    {
        return(rtrue);
    }

    /*
     * Scan for superclasses of @c @b clsidx1
     */
    jvm_class_index clsidxCLS = clsidx1;

    /*
     * Will not fail on entering loop, only possible after
     * first time through it because of @c @b clsidx1 test above.
     */
    while (jvm_class_index_null != clsidxCLS)
    {
        ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidxCLS)->pcfs;

        /*
         * If found top of hierarchy, namely java.lang.Object, then
         * search failed.
         */
        if (CONSTANT_CP_DEFAULT_INDEX == pcfs->super_class)
        {
            break;
        }

        /*
         * @internal Notice that if this request loads a class, it
         *           will also load @e all of its superclasses.
         *           Therefore, if it is a superclass that matches,
         *           no loading will occur later, but that previously
         *           loaded superclass will be found and returned
         *           immediately.
         *
         */
        clsidxCLS =
            class_load_from_cp_entry_utf(
                                 pcfs->constant_pool[pcfs->super_class],
                                         rfalse,
                                         (jint *) rnull);

        if (clsidx2 == clsidxCLS)
        {
            return(rtrue);
        }
    }

    /* Superclass of @c @b clsidx1 not found to be @c @b clsidx2 */
    return(rfalse);

} /* END of classutil_class_is_a() */


/*!
 * @brief Examine whether or not a class implements an interface
 *
 * For an @link #rtrue rtrue@endlink comparison, the class @c @b clsidx1
 * must implement the interface defined by class @c @b clsidx2 .
 *
 *
 * @returns @link #rtrue rtrue@endlink if @c @b clsidx1 implements
 *          the interface defined in class index @c @b clsidx2 .
 *          If either input parameter is
 *          @link #jvm_class_index_null jvm_class_index_null@endlink,
 *          then the result is @link #rfalse rfalse@endlink.
 *          If both input parameters are identical, then result
 *          is @link #rtrue rtrue@endlink.
 *
 */
rboolean classutil_class_implements_interface(jvm_class_index clsidx1,
                                              jvm_class_index clsidx2)
{
    ClassFile *pcfs1, *pcfs2, *pcfs3;
    u2         ifidx;

    /* Disallow comparison with null class */
    if ((jvm_class_index_null == clsidx1) ||
        (jvm_class_index_null == clsidx2))
    {
        return(rfalse);
    }

    /*
     * Cannot be true if comparator class index IS an interface
     * and if comparend class index is NOT an interface.
     */
    pcfs1 = CLASS_OBJECT_LINKAGE(clsidx1)->pcfs;
    pcfs2 = CLASS_OBJECT_LINKAGE(clsidx2)->pcfs;

    if ( (pcfs1->access_flags & ACC_INTERFACE) ||
        (!(pcfs2->access_flags & ACC_INTERFACE)))
    {
        return(rfalse);
    }

    /*
     * Comparison is false if both are the same class, i.e. there is
     * no interface involved.
     */
    if (clsidx1 == clsidx2)
    {
        return(rfalse);
    }

    /*
     * Scan for superclasses of @c @b clsidx2 .
     * If no interfaces, then this loop is skipped.
     */
    for (ifidx = 0; ifidx < pcfs1->interfaces_count; ifidx++)
    {
        jvm_class_index clsidxINTFC =
                class_load_from_cp_entry_utf(
                                            pcfs1->constant_pool[ifidx],
                                             rfalse,
                                             (jint *) rnull);

        /* Check if this class index matches requested interface */
        if (clsidx2 == clsidxINTFC)
        {
            return(rtrue);
        }

        pcfs3 = CLASS_OBJECT_LINKAGE(clsidxINTFC)->pcfs;

        /*
         * If found top of hierarchy, namely java.lang.Object class
         * or highest-level interface, then search cannot succeed.
         */
        if (CONSTANT_CP_DEFAULT_INDEX == pcfs3->super_class)
        {
            break;
        }

        /* Look at superclass of this one */
        clsidxINTFC =
            class_load_from_cp_entry_utf(
                                         pcfs3->constant_pool
                                           [pcfs3->super_class],
                                         rfalse,
                                         (jint *) rnull);

        /*
         * Check if super-hierarchy matches.
         *
         * WARNING:  RECURSIVE CALL!!! Will go as deep as hierarchy.
         */
        if (rtrue == classutil_class_is_superinterface_of(clsidx2,
                                                          clsidxINTFC))
        {
            return(rtrue);
        }
    }

    /* Class of @c @b clsidx1 not found to implement @c @b clsidx2 */
    return(rfalse);

} /* END of classutil_class_implements_interface() */


/*!
 * @brief Examine whether or not a class is a superinterface of another
 * class or interface.
 *
 * For an @link #rtrue rtrue@endlink comparison, the class index
 * @c @b clsidx1 must be a valid superinterface of that defined
 * by class @c @b clsidx2 .
 *
 *
 * @returns @link #rtrue rtrue@endlink if @c @b clsidx1 is a valid
 *          superinterface of class @c @b clsidx2 .
 *          If either input parameter is
 *          @link #jvm_class_index_null jvm_class_index_null@endlink,
 *          then the result is @link #rfalse rfalse@endlink.
 *          If both input parameters are identical, then result
 *          is @link #rtrue rtrue@endlink.
 *
 */
rboolean classutil_class_is_superinterface_of(jvm_class_index clsidx1,
                                              jvm_class_index clsidx2)
{
    ClassFile *pcfs1, *pcfs2, *pcfs3;
    u2         ifidx;

    /* Disallow comparison with null class */
    if ((jvm_class_index_null == clsidx1) ||
        (jvm_class_index_null == clsidx2))
    {
        return(rfalse);
    }

    /* Can only be true if comparator class index is an interface */
    pcfs1 = CLASS_OBJECT_LINKAGE(clsidx1)->pcfs;
    if (!(pcfs1->access_flags & ACC_INTERFACE))
    {
        return(rfalse);
    }

    /* Comparison is true if both are the same class */
    if (clsidx1 == clsidx2)
    {
        return(rtrue);
    }

    /*
     * Scan for superinterfaces of @c @b clsidx2 .
     *
     * If starting at top of hierarchy, namely java.lang.Object class
     * or highest-level interface, then search cannot succeed.
     */
    pcfs2 = CLASS_OBJECT_LINKAGE(clsidx2)->pcfs;

    if (CONSTANT_CP_DEFAULT_INDEX == pcfs2->super_class)
    {
        return(rfalse);
    }

    /* If no interfaces, then this loop is skipped */
    for (ifidx = 0; ifidx < pcfs2->interfaces_count; ifidx++)
    {
        jvm_class_index clsidxINTFC =
            class_load_from_cp_entry_utf(pcfs2->constant_pool[ifidx],
                                         rfalse,
                                         (jint *) rnull);

        /* Check if this class index matches requested interface */
        if (clsidx1 == clsidxINTFC)
        {
            return(rtrue);
        }

        pcfs3 = CLASS_OBJECT_LINKAGE(clsidxINTFC)->pcfs;

        /*
         * If found top of hierarchy, namely java.lang.Object class
         * or highest-level interface, then search cannot succeed.
         */
        if (CONSTANT_CP_DEFAULT_INDEX == pcfs3->super_class)
        {
            break;
        }

        /* Look at superclass of this one */
        clsidxINTFC =
            class_load_from_cp_entry_utf(
                           pcfs3->constant_pool[pcfs3->super_class],
                                         rfalse,
                                         (jint *) rnull);

        /*
         * Check if super-hierarchy matches.
         *
         * WARNING:  RECURSIVE CALL!!! Will go as deep as hierarchy.
         */
        if (rtrue == classutil_class_is_superinterface_of(clsidx1,
                                                          clsidxINTFC))
        {
            return(rtrue);
        }
    }

    /* Superclass of @c @b clsidx2 not found to be @c @b clsidx1 */
    return(rfalse);

} /* END of classutil_class_is_superinterface_of() */


/*!
 * @brief Examine whether or not a class may be accessed by another
 *
 * For an @link #rtrue rtrue@endlink comparison, the class @c @b clsidx1
 * must either be public or must be in the same runtime package
 * as @c @b clsidx2, as defined in the JVM spec section 5.4.4.
 *
 *
 * @returns @link #rtrue rtrue@endlink if @c @b clsidx1 is either
 *          @link #ACC_PUBLIC ACC_PUBLIC@endlink or must be
 *          in the same runtime package as @c @b cldidx2 or is
 *          actually the same class index as @c @b clsidx2 .
 *
 */
rboolean classutil_class_is_accessible_to(jvm_class_index clsidx1,
                                          jvm_class_index clsidx2)
{
    ClassFile *pcfs1 = CLASS_OBJECT_LINKAGE(clsidx1)->pcfs;

    /* Class is accessible if it is public or the same class */
    if ((clsidx1 ==  clsidx2) || (ACC_PUBLIC & pcfs1->access_flags))
    {
        return(rtrue);
    }

    /*
     * Class is accessible if it is in the same runtime package.
     * This may be discovered by comparing the first part of the
     * path names (before final path delimiter) for equality.
     */

    CONSTANT_Utf8_info *putfname1 = PTR_CP1_CLASS_NAME(pcfs1,
                                                     pcfs1->this_class);

    ClassFile *pcfs2 = CLASS_OBJECT_LINKAGE(clsidx2)->pcfs;

    CONSTANT_Utf8_info *putfname2 = PTR_CP1_CLASS_NAME(pcfs2,
                                                     pcfs2->this_class);

    /* Truncate class names from both strings, compare remainder */
    rchar *prcharclsfmt1 = utf_utf2prchar(putfname1);
    rchar *prcharclsfmt2 = utf_utf2prchar(putfname2);

    /* Strip class formatting, if any, before comparison */
    /*!
     * @todo   HARMONY-6-jvm-classutil.c-2 Is this stripping really
     *         necessary?  Or are the input parameters going to always
     *         be guaranteed to be @e only class names without
     *         any formatting?
     */
    rchar *prcharname1 =
        nts_prchar2prchar_unformatted_classname(prcharclsfmt1);
    rchar *prcharname2 =
        nts_prchar2prchar_unformatted_classname(prcharclsfmt2);

    HEAP_FREE_DATA(prcharclsfmt1);
    HEAP_FREE_DATA(prcharclsfmt2);

    /* Locate end of path component, if any */
    rchar *pnameonly1 = portable_strrchr(prcharname1,
                                     CLASSNAME_INTERNAL_DELIMITER_CHAR);
    rchar *pnameonly2 = portable_strrchr(prcharname2,
                                     CLASSNAME_INTERNAL_DELIMITER_CHAR);

    /* Check for two unpackaged classes, accessible if so */
    if ((rnull == pnameonly1) && (rnull == pnameonly2))
    {
        HEAP_FREE_DATA(prcharname1);
        HEAP_FREE_DATA(prcharname2);
        return(rtrue);
    }

    /* Not accessible if only one is unpackaged */
    if (((rnull == pnameonly1) && (rnull != pnameonly2)) ||
        ((rnull != pnameonly1) && (rnull == pnameonly2)))
    {
        HEAP_FREE_DATA(prcharname1);
        HEAP_FREE_DATA(prcharname2);
        return(rfalse);
    }

    /*
     * Both classes area packaged, so compare null-terminated
     * package name strings
     */
    *pnameonly1 = '\0';
    *pnameonly2 = '\0';

    rboolean rc = (0 == portable_strcmp(prcharname1, prcharname2))
                  ? rtrue
                  : rfalse;

    HEAP_FREE_DATA(prcharname1);
    HEAP_FREE_DATA(prcharname2);
    return(rc);

} /* END of classutil_class_is_accessible_to() */


/*!
 * @brief Examine whether or not an array class implements one of
 *        the interfaces that are valid for arrays
 *
 * For an @link #rtrue rtrue@endlink comparison, the class @c @b clsidx1
 * must be one of the interfaces defined in the JVM spec section 2.15.
 *
 *
 * @returns @link #rtrue rtrue@endlink if @c @b clsidx1 is one of
 *          the valid interfaces available to arrays.
 *          If the input parameter is
 *          @link #jvm_class_index_null jvm_class_index_null@endlink,
 *          then the result is @link #rfalse rfalse@endlink.
 *
 *
 * @todo   HARMONY-6-jvm-classutil.c-1 Need to verify that the array
 *         design can properly implement the 'clone' method.
 *
 */
rboolean
      classutil_interface_implemented_by_arrays(jvm_class_index clsidx1)
{
    /* Disallow evaluation of null class */
    if (jvm_class_index_null == clsidx1)
    {
        return(rfalse);
    }

    if (clsidx1 == 
       class_load_from_prchar(JVMCLASS_JAVA_LANG_CLONEABLE,
                              rfalse,
                              (jint *) rnull))
    {
        return(rtrue);
    }
    else
    if (clsidx1 ==
        class_load_from_prchar(JVMCLASS_JAVA_IO_SERIALIZABLE,
                               rfalse,
                               (jint *) rnull))
    {
        return(rtrue);
    }
    return(rfalse);

} /* END of classutil_interface_implemented_by_arrays() */

/*@} */ /* End of grouped definitions */


/* EOF */
