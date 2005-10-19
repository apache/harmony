/*!
 * @file linkage.c
 *
 * @brief Late binding linkages between classes.
 *
 * This logic should be called each and every time a class is
 * loaded or unloaded.  When a new class is loaded, existing
 * classes may have unresolved linkages to them that need to
 * be filled in to point to the new class.  Likewise, when a
 * class is unloaded, the remaining classes need to have the
 * linkages removed.  In this manner, the class linkage status
 * will stay current.
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
ARCH_SOURCE_COPYRIGHT_APACHE(linkage, c,
"$URL$",
"$Id$");

#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "attribute.h"
#include "exit.h"
#include "field.h"
#include "gc.h" 
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h"
#include "method.h"
#include "native.h"
#include "util.h"


/*!
 * @brief Resolve class table and object table linkages for a class
 * file, typically one just loaded by
 * @link #class_static_new() class_static_new()@endlink
 *
 *
 * @param  clsidx   Class table index to class to resolve against
 *                  all loaded classes.
 *
 * @param find_registerNatives When @link #rtrue rtrue@endlink, will
 *                  return the ordinal for
 *                  @link #JVMCFG_JLOBJECT_NMO_REGISTER 
                    JVMCFG_JLOBJECT_NMO_REGISTER@endlink and
 *                  @link #JVMCFG_JLOBJECT_NMO_UNREGISTER 
                    JVMCFG_JLOBJECT_NMO_UNREGISTER@endlink
 *                  as well as the other ordinals.  Once JVM
 *                  initialization is complete, this should always
 *                  be @link #rfalse rfalse@endlink because all
 *                  future classes should @e never have local ordinals.
 *
 *
 * @returns @link #rtrue rtrue@endlink if class was completely
 *          resolved, @link #rfalse rfalse@endlink otherwise.
 *
 */

rboolean linkage_resolve_class(jvm_class_index clsidx,
                               rboolean        find_registerNatives)
{
    ARCH_FUNCTION_NAME(linkage_resolve_class);

    if (jvm_class_index_null == clsidx)
    {
        /*!
         * @link #rtrue rtrue@endlink/@link #rfalse rfalse@endlink
         * irrelevant here
         */
        return(rfalse);
    }

    /* If class is not present, cannot resolve linkages */
    if (!(CLASS_STATUS_INUSE & CLASS(clsidx).status))
    {
        return(rfalse);
    }

    /* If class is fully linked, there is no need to do it again */
    if (CLASS_STATUS_LINKED & CLASS(clsidx).status)
    {
        return(rtrue);
    }

    /*
     * Try to find @e one instance of a class not yet loaded.  If found,
     * then this class cannot be linked to it, so set
     * @link #rfalse rfalse@endlink.  This result will be stored in the
     * </b><code>CLASS(clsidx)->status</code></b>
     * as the @link #CLASS_STATUS_LINKED CLASS_STATUS_LINKED@endlink
     * bit, either @link #rtrue rtrue@endlink or
     * @link #rfalse rfalse@endlink.
     */
    rboolean class_fully_linked = rtrue;

    /*
     * Scan through @c @b constant_pool, resolving any missing items
     * that might now be available.
     */

    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    jvm_constant_pool_index cpidx;
    for (cpidx = CONSTANT_CP_START_INDEX;
         cpidx < pcfs->constant_pool_count + CONSTANT_CP_START_INDEX -1;
         cpidx++)
    {
        jvm_class_index        clsidxFIND;
        jvm_field_index        fldidxFIND;
        jvm_field_lookup_index fluidxFIND;

        jvm_method_index       mthidxFIND;
        jvm_attribute_index    atridxFIND;

        cp_info_dup *pcpd = pcfs->constant_pool[cpidx];

        CONSTANT_Class_info              *pcpd_Class;
        CONSTANT_Fieldref_info           *pcpd_Fieldref;
        CONSTANT_Methodref_info          *pcpd_Methodref;
        CONSTANT_InterfaceMethodref_info *pcpd_InterfaceMethodref;
        CONSTANT_NameAndType_info        *pcpd_NameAndType;

        cp_info_dup *clsname;

        switch (CP_TAG(pcfs, cpidx))
        {
            case CONSTANT_Class:

                pcpd_Class = PTR_THIS_CP_Class(pcpd);

                /* Only resolve this CP slot if it is not yet done. */
                if (jvm_class_index_null ==
                    pcpd_Class->LOCAL_Class_binding.clsidxJVM)
                {
                    clsname =
                        pcfs->constant_pool[pcpd_Class->name_index];

                    clsidxFIND = class_find_by_cp_entry(clsname);

                    if (jvm_class_index_null == clsidxFIND)
                    {
                        /* Class not yet loaded in memory */
                        class_fully_linked = rfalse;
                    }
                    else
                    {
                        PTR_THIS_CP_Class(pcpd)
                           ->LOCAL_Class_binding.clsidxJVM = clsidxFIND;

                        /* Add reference unless it references itself */
                        if (clsidx != clsidxFIND)
                        {
                            (rvoid) GC_CLASS_MKREF_FROM_CLASS(
                                        clsidx,
                                        clsidxFIND);
                        }
                    }
                }
                break;

            case CONSTANT_Fieldref:

                pcpd_Fieldref = PTR_THIS_CP_Fieldref(pcpd);

                /* Only resolve this CP slot if it is not yet done. */
                if (jvm_class_index_null ==
                    pcpd_Fieldref->LOCAL_Fieldref_binding.clsidxJVM)
                {
                    pcpd_Class =
                        PTR_THIS_CP_Class(pcfs->constant_pool
                                         [pcpd_Fieldref->class_index]);

                    clsname =
                        pcfs->constant_pool[pcpd_Class->name_index];

                    clsidxFIND = class_find_by_cp_entry(clsname);

                    pcpd_Fieldref
                        ->LOCAL_Fieldref_binding.clsidxJVM = clsidxFIND;

                    if (jvm_class_index_null == clsidxFIND)
                    {
                        /* Class not yet loaded in memory */
                        class_fully_linked = rfalse;
                    }
                    else
                    {
                        pcpd_NameAndType =
                            PTR_THIS_CP_NameAndType(
                                pcfs->constant_pool
                                  [pcpd_Fieldref->name_and_type_index]);

                        fldidxFIND =
                            field_find_by_cp_entry(
                                clsidxFIND,
                                pcfs->constant_pool
                                    [pcpd_NameAndType->name_index],
                                pcfs->constant_pool
                                  [pcpd_NameAndType->descriptor_index]);

                        if (jvm_field_index_bad == fldidxFIND)
                        {
                            /* Field not found in class-- fatal error */
                            exit_throw_exception(EXIT_JVM_CLASS,
                                   JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR);
/*NOTREACHED*/
#if 0
                            pcpd_Fieldref
                                ->LOCAL_Fieldref_binding.fluidxJVM =
                                    jvm_field_lookup_index_bad;

                            pcpd_Fieldref
                                ->LOCAL_Fieldref_binding.oiflagJVM =
                                    rneither_true_nor_false;

                            pcpd_Fieldref
                                ->LOCAL_Fieldref_binding.jvaluetypeJVM =
                                    LOCAL_BASETYPE_ERROR;

                            /* No point processing ACC_STATIC bit */
                            continue;
#endif
                        }

                        /*
                         * Now check if ACC_STATIC or not and store
                         * the proper type of field lookup index.
                         */
                        if (FIELD(clsidxFIND, fldidxFIND)
                              ->access_flags & ACC_STATIC)
                        {
                            fluidxFIND =
            field_index_get_class_static_lookup(clsidxFIND, fldidxFIND);

                        }
                        else
                        {
                            fluidxFIND =
         field_index_get_object_instance_lookup(clsidxFIND, fldidxFIND);

                        }

                        pcpd_Fieldref
                            ->LOCAL_Fieldref_binding.fluidxJVM =
                                fluidxFIND;

                        if (jvm_field_lookup_index_bad == fluidxFIND)
                        {
                            /* Field not found in class-- fatal error */
                            exit_throw_exception(EXIT_JVM_CLASS,
                                   JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR);
/*NOTREACHED*/

                            pcpd_Fieldref
                                ->LOCAL_Fieldref_binding.oiflagJVM =
                                    rneither_true_nor_false;

                            pcpd_Fieldref
                                ->LOCAL_Fieldref_binding.jvaluetypeJVM =
                                    LOCAL_BASETYPE_ERROR;

                            /*
                             * Don't have valid @b oiflagJVM or
                             * @b jvaluetypeJVM result
                             */
                            continue;
                        }

                        pcpd_Fieldref
                            ->LOCAL_Fieldref_binding.oiflagJVM =
                                (FIELD(clsidxFIND, fldidxFIND)
                                   ->access_flags & ACC_STATIC)
                                ? rfalse
                                : rtrue;

                        cp_info_dup *pfldesc_dup =
                           CLASS_OBJECT_LINKAGE(clsidxFIND)
                             ->pcfs
                               ->constant_pool
                                 [FIELD(clsidxFIND, fldidxFIND)
                                    ->descriptor_index];

                        CONSTANT_Utf8_info *pfldesc =
                                          PTR_THIS_CP_Utf8(pfldesc_dup);

                        /*!
                         * @todo HARMONY-6-jvm-linkage.c-1 What needs
                         *       to happen when base type is
                         *       BASETYPE_ARRAY or BASETYPE_ERROR
                         *       or BASETYPE_VOID?
                         */
                        pcpd_Fieldref
                            ->LOCAL_Fieldref_binding.jvaluetypeJVM =

                                pfldesc->bytes[0];

                        /* Add reference unless it references itself */
                        if (clsidx != clsidxFIND)
                        {
                            (rvoid) GC_CLASS_MKREF_FROM_CLASS(
                                        clsidx,
                                        clsidxFIND);
                        }
                    }
                }
                break;

            case CONSTANT_Methodref:

                pcpd_Methodref = PTR_THIS_CP_Methodref(pcpd);

                /* Only resolve this CP slot if it is not yet done. */
                if (jvm_class_index_null ==
                    pcpd_Methodref->LOCAL_Methodref_binding.clsidxJVM)
                {
                    pcpd_Class =
                        PTR_THIS_CP_Class(pcfs->constant_pool
                                         [pcpd_Methodref->class_index]);

                    clsname =
                        pcfs->constant_pool[pcpd_Class->name_index];

                    clsidxFIND = class_find_by_cp_entry(clsname);

                    pcpd_Methodref
                        ->LOCAL_Methodref_binding.clsidxJVM =clsidxFIND;

                    if (jvm_class_index_null == clsidxFIND)
                    {
                        /* Class not yet loaded in memory */
                        class_fully_linked = rfalse;
                    }
                    else
                    {
                        pcpd_NameAndType =
                            PTR_THIS_CP_NameAndType(
                                pcfs->constant_pool
                                 [pcpd_Methodref->name_and_type_index]);

                        mthidxFIND =
                            method_find_by_cp_entry(
                                clsidxFIND,
                                pcfs->constant_pool
                                    [pcpd_NameAndType->name_index],
                                pcfs->constant_pool
                                  [pcpd_NameAndType->descriptor_index]);

                        pcpd_Methodref
                            ->LOCAL_Methodref_binding.mthidxJVM =
                                mthidxFIND;

                        if (jvm_method_index_bad == mthidxFIND)
                        {
                            /* Method not found in class-- fatal error*/
                            exit_throw_exception(EXIT_JVM_CLASS,
                                  JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
                        }

                        /*
                         * Now check if Code_attribute is present
                         * and store its index.  First check if native
                         * and insert native method ordinal if so.
                         */

                        if (ACC_NATIVE &
                            METHOD(clsidxFIND,mthidxFIND)->access_flags)
                        {
                            atridxFIND = jvm_attribute_index_native;

                            pcpd_Methodref
                              ->LOCAL_Methodref_binding
                                  .nmordJVM =
                                native_locate_local_method(
                                        pcfs,
                                        pcpd_Class->name_index,
                                        pcpd_NameAndType->name_index,
                                        pcpd_NameAndType
                                          ->descriptor_index,
                                        find_registerNatives);
                        }
                        else
                        {
                            atridxFIND =
                                attribute_find_in_method_by_enum(
                                    clsidxFIND,
                                    mthidxFIND,
                                    LOCAL_CODE_ATTRIBUTE);
                        }

                        pcpd_Methodref
                            ->LOCAL_Methodref_binding.codeatridxJVM =
                                atridxFIND;

                        if (jvm_attribute_index_bad == atridxFIND)
                        {
                            /* Code not found in method-- fatal error */
                            exit_throw_exception(EXIT_JVM_ATTRIBUTE,
                                  JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
                        }

                        /*
                         * Now check if Exceptions attribute is
                         * present and store its index.
                         */

                        atridxFIND =
                            attribute_find_in_method_by_enum(
                                clsidxFIND,
                                mthidxFIND,
                                LOCAL_EXCEPTIONS_ATTRIBUTE);

                        pcpd_Methodref
                            ->LOCAL_Methodref_binding.excpatridxJVM =
                                atridxFIND;

                        if (jvm_attribute_index_bad == atridxFIND)
                        {
                            /* It is OKAY to not have exceptions */
                        }
 
                        /* Add reference unless it references itself */
                        if (clsidx != clsidxFIND)
                        {
                            (rvoid) GC_CLASS_MKREF_FROM_CLASS(
                                        clsidx,
                                        clsidxFIND);
                        }
                    }
                }
                break;

            case CONSTANT_InterfaceMethodref:

                pcpd_InterfaceMethodref =
                    PTR_THIS_CP_InterfaceMethodref(pcpd);

                /* Only resolve this CP slot if it is not yet done. */
                if (jvm_class_index_null ==
                    pcpd_InterfaceMethodref
                        ->LOCAL_InterfaceMethodref_binding.clsidxJVM)
                {
                    pcpd_Class =
                        PTR_THIS_CP_Class(pcfs->constant_pool
                                [pcpd_InterfaceMethodref->class_index]);
                    clsname =
                        pcfs->constant_pool[pcpd_Class->name_index];

                    clsidxFIND = class_find_by_cp_entry(clsname);

                    pcpd_InterfaceMethodref
                        ->LOCAL_InterfaceMethodref_binding.clsidxJVM =
                            clsidxFIND;

                    if (jvm_class_index_null == clsidxFIND)
                    {
                        /* Class not yet loaded in memory */
                        class_fully_linked = rfalse;
                    }
                    else
                    {
                        pcpd_NameAndType =
                            PTR_THIS_CP_NameAndType(
                                pcfs->constant_pool
                                [pcpd_InterfaceMethodref
                                  ->name_and_type_index]);

                        mthidxFIND =
                            method_find_by_cp_entry(
                                clsidxFIND,
                                pcfs->constant_pool
                                    [pcpd_NameAndType->name_index],
                                pcfs->constant_pool
                                  [pcpd_NameAndType->descriptor_index]);

                        pcpd_InterfaceMethodref
                            ->LOCAL_InterfaceMethodref_binding
                              .mthidxJVM =
                                  mthidxFIND;

                        if (jvm_method_index_bad == mthidxFIND)
                        {
                            /* Method not found in class-- fatal error*/
                            exit_throw_exception(EXIT_JVM_CLASS,
                                  JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
                        }

                        /*
                         * Now check if Code_attribute is present
                         * and store its index.  First check if native
                         * and insert native method ordinal if so.
                         */

                        if (ACC_NATIVE &
                            METHOD(clsidxFIND,mthidxFIND)->access_flags)
                        {
                            atridxFIND = jvm_attribute_index_native;

                            /*!
                             * @todo HARMONY-6-jvm-linkage.c-2 Should
                             *       this instance permit use
                             *       of @b find_registerNatives
                             *       since interaces are not a part
                             *       of the JVM startup, just a few
                             *       foundational classes?  Should
                             *       it just be
                             *       @link #rfalse rfalse@endlink
                             *       instead?
                             */
                            pcpd_InterfaceMethodref
                              ->LOCAL_InterfaceMethodref_binding
                                  .nmordJVM =
                                native_locate_local_method(
                                        pcfs,
                                        pcpd_Class->name_index,
                                        pcpd_NameAndType->name_index,
                                        pcpd_NameAndType
                                          ->descriptor_index,
                                        find_registerNatives);
                        }
                        else
                        {
                            atridxFIND =
                                attribute_find_in_method_by_enum(
                                    clsidxFIND,
                                    mthidxFIND,
                                    LOCAL_CODE_ATTRIBUTE);
                        }

                        pcpd_InterfaceMethodref
                            ->LOCAL_InterfaceMethodref_binding
                              .codeatridxJVM =
                                  atridxFIND;

                        if (jvm_attribute_index_bad == atridxFIND)
                        {
                            /* Code not found in method-- fatal error */
                            exit_throw_exception(EXIT_JVM_ATTRIBUTE,
                                  JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
                        }

                        /*
                         * Now check if Exceptions attribute is
                         * present and store its index.
                         */

                        atridxFIND =
                            attribute_find_in_method_by_enum(
                                clsidxFIND,
                                mthidxFIND,
                                LOCAL_EXCEPTIONS_ATTRIBUTE);

                        pcpd_InterfaceMethodref
                            ->LOCAL_InterfaceMethodref_binding
                              .excpatridxJVM =
                                  atridxFIND;

                        if (jvm_attribute_index_bad == atridxFIND)
                        {
                            /* It is OKAY to not have exceptions */
                        }

                        /* Add reference unless it references itself */
                        if (clsidx != clsidxFIND)
                        {
                            (rvoid) GC_CLASS_MKREF_FROM_CLASS(
                                        clsidx,
                                        clsidxFIND);
                        }
                    }
                }
                break;

            case CONSTANT_String:

            case CONSTANT_Integer:

            case CONSTANT_Float:

            case CONSTANT_Long:

            case CONSTANT_Double:

            case CONSTANT_NameAndType:

            case CONSTANT_Utf8:

                /*
                 * There is no late binding associated
                 * directly with these tags.
                 */

                break;

            default:
                GENERIC_FAILURE1_VALUE(rtrue,
                                       DMLNORM,
                                       arch_function_name,
                                       "Invalid CP tag %d",
                                       CP_TAG(pcfs, cpidx),
                                       rfalse,
                                       rnull,
                                       rnull);

        } /* switch ... */

    } /* for (cpidx) */

    /* If this class linked to existing classes, it is fully linked */
    if (rtrue == class_fully_linked)
    {
        CLASS(clsidx).status |= CLASS_STATUS_LINKED;
    } 

    cfmsgs_show_constant_pool(CLASS_OBJECT_LINKAGE(clsidx)->pcfs);

    return(class_fully_linked);

} /* END of linkage_resolve_class() */


/*!
 * @brief Unresolve class table and object table linkages for a
 * class file that is getting ready to be unloaded.
 *
 *
 * @param  clsidx   Class table index to class to unresolve against
 *                  all loaded classes.
 *
 *
 * @returns @link #rtrue rtrue@endlink if class linkages were
 *          unresolved, @link #rfalse rfalse@endlink otherwise.
 *
 */

rboolean linkage_unresolve_class(jvm_class_index clsidx)
{
    ARCH_FUNCTION_NAME(linkage_unresolve_class);

    if (jvm_class_index_null == clsidx)
    {
        /*
         * @link #rtrue rtrue@endlink/@link #rfalse rfalse@endlink
         * irrelevant here
         */
        return(rfalse);
    }

    /* If class is not present, cannot unresolve linkages */
    if (!(CLASS_STATUS_INUSE & CLASS(clsidx).status))
    {
        return(rfalse);
    }


    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    /*
     * Scan through constant_pool, resolving any missing items
     * that might now be available.
     */
    jvm_constant_pool_index cpidx;
    for (cpidx = CONSTANT_CP_START_INDEX;
         cpidx < pcfs->constant_pool_count + CONSTANT_CP_START_INDEX -1;
         cpidx++)
    {
        cp_info_dup *pcpd = pcfs->constant_pool[cpidx];

        CONSTANT_Class_info              *pcpd_Class;
        CONSTANT_Fieldref_info           *pcpd_Fieldref;
        CONSTANT_Methodref_info          *pcpd_Methodref;
        CONSTANT_InterfaceMethodref_info *pcpd_InterfaceMethodref;

        switch (CP_TAG(pcfs, cpidx))
        {
            case CONSTANT_Class:

                pcpd_Class = PTR_THIS_CP_Class(pcpd);

                /* Only unresolve this CP slot if it was resolved */
                if (jvm_class_index_null !=
                    pcpd_Class->LOCAL_Class_binding.clsidxJVM)
                {
                    /* Remove reference unless it references itself */
                    if (clsidx !=
                        pcpd_Class->LOCAL_Class_binding.clsidxJVM)
                    {
                        (rvoid) GC_CLASS_RMREF_FROM_CLASS(
                                    clsidx,
                                    pcpd_Class
                                      ->LOCAL_Class_binding
                                        .clsidxJVM);
                    }

                    pcpd_Class->LOCAL_Class_binding.clsidxJVM =
                        jvm_class_index_null;
                }
                break;

            case CONSTANT_Fieldref:

                pcpd_Fieldref = PTR_THIS_CP_Fieldref(pcpd);

                /* Only unresolve this CP slot if it was resolved */
                if (jvm_class_index_null !=
                    pcpd_Fieldref->LOCAL_Fieldref_binding.clsidxJVM)
                {
                    /* Remove reference unless it references itself */
                    if (clsidx != 
                        pcpd_Fieldref->LOCAL_Fieldref_binding.clsidxJVM)
                    {
                        (rvoid) GC_CLASS_RMREF_FROM_CLASS(
                                    clsidx,
                                    pcpd_Fieldref
                                      ->LOCAL_Fieldref_binding
                                        .clsidxJVM);
                    }

                    pcpd_Fieldref
                        ->LOCAL_Fieldref_binding.clsidxJVM =
                            jvm_class_index_null;

                    pcpd_Fieldref
                        ->LOCAL_Fieldref_binding.fluidxJVM =
                            jvm_field_lookup_index_bad;

                    pcpd_Fieldref
                        ->LOCAL_Fieldref_binding.oiflagJVM =
                            rneither_true_nor_false;

                    pcpd_Fieldref
                        ->LOCAL_Fieldref_binding.jvaluetypeJVM =
                            LOCAL_BASETYPE_ERROR;
                }
                break;

            case CONSTANT_Methodref:

                pcpd_Methodref = PTR_THIS_CP_Methodref(pcpd);

                /* Only unresolve this CP slot if it was resolved */
                if (jvm_class_index_null !=
                    pcpd_Methodref->LOCAL_Methodref_binding.clsidxJVM)
                {
                    /* Remove reference unless it references itself */
                    if (clsidx != 
                      pcpd_Methodref->LOCAL_Methodref_binding.clsidxJVM)
                    {
                        (rvoid) GC_CLASS_RMREF_FROM_CLASS(
                                    clsidx,
                                    pcpd_Methodref
                                      ->LOCAL_Methodref_binding
                                        .clsidxJVM);
                    }

                    pcpd_Methodref
                        ->LOCAL_Methodref_binding.clsidxJVM =
                            jvm_class_index_null;

                    pcpd_Methodref
                        ->LOCAL_Methodref_binding.mthidxJVM =
                            jvm_method_index_bad;

                    pcpd_Methodref
                        ->LOCAL_Methodref_binding.codeatridxJVM =
                            jvm_attribute_index_bad;

                    pcpd_Methodref
                        ->LOCAL_Methodref_binding.excpatridxJVM =
                            jvm_attribute_index_bad;

                    pcpd_Methodref
                        ->LOCAL_Methodref_binding.nmordJVM =
                            jvm_native_method_ordinal_null;
                }
                break;

            case CONSTANT_InterfaceMethodref:

                pcpd_InterfaceMethodref =
                    PTR_THIS_CP_InterfaceMethodref(pcpd);

                /* Only unresolve this CP slot if it was resolved */
                if (jvm_class_index_null !=
                    pcpd_InterfaceMethodref
                        ->LOCAL_InterfaceMethodref_binding.clsidxJVM)
                {
                    /* Remove reference unless it references itself */
                    if (clsidx != 
                        pcpd_InterfaceMethodref
                          ->LOCAL_InterfaceMethodref_binding.clsidxJVM)
                    {
                        (rvoid) GC_CLASS_RMREF_FROM_CLASS(
                                    clsidx,
                                    pcpd_InterfaceMethodref
                                      ->LOCAL_InterfaceMethodref_binding
                                        .clsidxJVM);
                    }

                    pcpd_InterfaceMethodref
                        ->LOCAL_InterfaceMethodref_binding.clsidxJVM =
                            jvm_class_index_null;

                    pcpd_InterfaceMethodref
                        ->LOCAL_InterfaceMethodref_binding.mthidxJVM =
                            jvm_method_index_bad;

                    pcpd_InterfaceMethodref
                       ->LOCAL_InterfaceMethodref_binding.codeatridxJVM=
                            jvm_attribute_index_bad;

                    pcpd_InterfaceMethodref
                       ->LOCAL_InterfaceMethodref_binding.excpatridxJVM=
                            jvm_attribute_index_bad;

                    pcpd_InterfaceMethodref
                       ->LOCAL_InterfaceMethodref_binding.nmordJVM=
                            jvm_native_method_ordinal_null;
                }
                break;

            case CONSTANT_String:

            case CONSTANT_Integer:

            case CONSTANT_Float:

            case CONSTANT_Long:

            case CONSTANT_Double:

            case CONSTANT_NameAndType:

            case CONSTANT_Utf8:

                /*
                 * There is no late binding associated
                 * directly with these tags.
                 */

                break;

            default:
                GENERIC_FAILURE1_VALUE(rtrue,
                                       DMLNORM,
                                       arch_function_name,
                                       "Invalid CP tag %d",
                                       CP_TAG(pcfs, cpidx),
                                       rfalse,
                                       rnull,
                                       rnull);

        } /* switch ... */

    } /* for (cpidx) */

    /* This class is no longer fully linked */
    CLASS(clsidx).status &= ~CLASS_STATUS_LINKED;

    return(rtrue);

} /* END of linkage_unresolve_class() */


/* EOF */
