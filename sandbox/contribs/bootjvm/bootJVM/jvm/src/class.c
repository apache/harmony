/*!
 * @file class.c
 *
 * @brief Create and manage real machine Java class data structures.
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
ARCH_COPYRIGHT_APACHE(class, c, "$URL$ $Id$");


#include <string.h>

#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "classfile.h"
#include "classpath.h"
#include "exit.h"
#include "field.h"
#include "gc.h"
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h"
#include "method.h"
#include "nts.h"
#include "utf.h"
#include "util.h"


/*!
 * @brief Set up an empty class in a given class table slot.
 *
 * The @b clsidx of JVMCFG_NULL_CLASS has special
 * properties in that it can ALWAYS be allocated and
 * is NEVER garbage collected!  Part of the purpose
 * for this is the JVMCFG_NULL_CLASS is of value zero,
 * which is widely used throughout the code as a special
 * value.  This this slot is not available for @e anything
 * else.
 * 
 *
 * @param  clsidx   Class table index of slot to set up.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid class_static_setup(jvm_class_index clsidx)
{
    /*
     * Declare slot in use, but not initialized.
     * (Redundant for most situations where
     * class_allocate_slot() was called, but needed
     * for initializing classes like JVMCFG_NULL_CLASS
     * with an absolute slot number that was not
     * searched for by the allocator.)
     */
    CLASS(clsidx).status = CLASS_STATUS_INUSE | CLASS_STATUS_NULL;

    /*
     * Start out with no array allocation and no array dimensions
     */
    CLASS(clsidx).arraydims = LOCAL_CONSTANT_NO_ARRAY_DIMS;
    CLASS(clsidx).arraylength = (jint *) rnull;
    CLASS(clsidx).lower_dim_array = jvm_class_index_null;

    /* Start out with no object hash */
    CLASS(clsidx).class_objhash = jvm_object_hash_null;

    /* Start out with no class static field lookups */
    CLASS(clsidx).num_class_static_field_lookups = 0;
    CLASS(clsidx).class_static_field_lookup = (jvm_class_index *) rnull;
    CLASS(clsidx).class_static_field_data = (jvalue *) rnull;

    /* Start out with no object instance field lookups */
    CLASS(clsidx).num_object_instance_field_lookups = 0;
    CLASS(clsidx).object_instance_field_lookup =
                                              (jvm_field_index *) rnull;

    /*
     * Report which @c @b java.lang.ClassLoader initiated
     * and defined this class.  The bootstrap ClassLoader always
     * reports @link #jvm_class_index_null jvm_class_index_null@endlink
     * for both of these items.
     */
    CLASS(clsidx).initiating_ClassLoader = jvm_class_index_null;
    CLASS(clsidx).defining_ClassLoader   = jvm_class_index_null;

    /*
     * Garbage collection @e initialization is performed by
     * @link #GC_CLASS_NEW GC_CLASS_NEW()@endlink.
     *
     * Garbage collection @e finalization is performed by
     * @link #GC_CLASS_DELETE GC_CLASS_DELETE()@endlink.
     */
    CLASS(clsidx).pgarbage = (rvoid *) rnull;

    /*
     * Do not set up GC_CLASS_NEW() unless there is
     * a real class with the possibility of real fields.
     */

    return;

} /* END of class_static_setup() */


/*!
 * @brief Initialize the class area of the JVM model
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *       @returns @link #rvoid rvoid@endlink
 *
 */
rvoid class_init()
{
    class_static_setup(jvm_class_index_null);

    pjvm->class_allocate_last = jvm_class_index_null;

    /* Declare this module initialized */
    jvm_class_initialized = rtrue;

    return;

} /* END of class_init() */


/*!
 * @brief Locate and reserve an unused class table slot for a new class.
 *
 *
 * @param  tryagain   If @link #rtrue rtrue@endlink, run garbage
 *                    collection @e once if no empty slots are
 *                    available so as to try and free up something.
 *                    Typically, invoke as
 *                    @link #rtrue rtrue@endlink, and
 *                    let recursion call it with
 *                    @link #rfalse rfalse@endlink.
 *
 *
 * @returns Class table index of an empty slot.  Throw error if no
 *          slots.
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no slots are available.@endlink.
 *
 */
static jvm_class_index class_allocate_slot(rboolean tryagain)
{
    /* Search for a free class table slot */
    jvm_class_index clsidx =
        (JVMCFG_MAX_CLASSES == (1 + pjvm->class_allocate_last))
        ? JVMCFG_FIRST_CLASS
        : 1 + pjvm->class_allocate_last;

    /* Count allocated slots in all slots are full */
    jvm_class_index count = 0;

    while(rtrue)
    {
        if (CLASS(clsidx).status & CLASS_STATUS_INUSE)
        {
            /* Point to next slot, wrap around at end */
            clsidx++;

            if (clsidx == JVMCFG_MAX_CLASSES - 1)
            {
                clsidx = JVMCFG_FIRST_CLASS;
            }

            /* Limit high value to end of table */
            if (pjvm->class_allocate_last == JVMCFG_MAX_CLASSES - 1)
            {
                pjvm->class_allocate_last = JVMCFG_FIRST_CLASS - 1;
            }

            /* Count this attempt and keep looking */
            count++;

            if (count == (JVMCFG_MAX_CLASSES - JVMCFG_FIRST_CLASS))
            {
                /* Try again (with rfalse) if requested */
                if (rtrue == tryagain)
                {
                    GC_RUN(rtrue);  /* Try to free up some space */

                    /* Now try to locate a free slot */

                    /* WARNING!!! Recursive call-- but only 1 deep */
                    return(class_allocate_slot(rfalse));
                }

                /* No more slots, cannot continue */
                exit_throw_exception(EXIT_JVM_CLASS,
                                   JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR);
/*NOTREACHED*/
            }
        }

        /* Declare slot in use, but not initialized */
        CLASS(clsidx).status = CLASS_STATUS_INUSE | CLASS_STATUS_NULL;

        /* Report where this allocation was performed */
        pjvm->class_allocate_last = clsidx;

        return(clsidx);
    }
/*NOTREACHED*/
    return(jvm_class_index_null); /* Satisfy compiler */

} /* END of class_allocate_slot() */


/*!
 * @brief Load a class into ad class table slot and load up its
 * associated class definition object.
 *
 * Create a Java class itself (and NOT a @c @b new instance
 * of a class!).  The following three mutially exclusive variations
 * are available using the @b special_cls modifier:
 *
 * <ul>
 * <li>(1) @b CLASS_STATUS_EMPTY:     Normal class, no special
 *                                    treatment.
 * </li>
 *
 * <li>(2) @b CLASS_STATUS_ARRAY:     Treat class instance creation
 *                                    as a dimension of an array
 *                                    instead of as a normal class
 *                                    load.  The recursion will
 *                                    eventually load its base
 *                                    class and superclasses.
 *                                    When allocating an array
 *                                    class, DO NOT EVER INVOKE
 *                                    THIS FUNCTION FOR A PRIMATIVE
 *                                    ARRAY!
 * </li>
 *
 * <li>(3) @b CLASS_STATUS_PRIMATIVE: Treat class instance creation
 *                                    as loading a primative
 *                                    pseudo-class for use by
 *                                    @c @b java.lang.Class.
 *                                    A related Classfile structure
 *                                    will be generated for this
 *                                    pseudo-class.
 * </li></ul>
 *
 * No verification of @b special_cls is performed, only these values
 * are assumed.
 *
 * Use a simple circular slot allocation mechanism to report where
 * most recent class was allocated.  The search for the next slot
 * will begin from here and go all the way around the array to this
 * same slot.  If not successful, throw error, but do @e not return.
 *
 *
 * @param   special_cls Bit-mapped request for various special
 *                      considerations for class construction.  If not
 *                      needed, use @b CLASS_STATUS_EMPTY.  If used, the
 *                      values are:
 *
 * <ul><li>
 *                      @b CLASS_STATUS_ARRAY create new array class
 *                                            instead of class instance
 * </li>
 *
 * <li>
 *                      @b CLASS_STATUS_PRIMATIVE create special class
 *                                                instance of a Java
 *                                                primative for use by
 *                                                @c @b java.lang.Class
 * </li>
 * </ul>
 *
 * @param    pcfs       Pointer to ClassFile area which contains this
 *                      class, @link #rnull rnull@endlink for
 *                      @b CLASS_STATUS_PRIMATIVE requests.
 *
 * @param    arraydims  Number of array dimensions for this class,
 *                        or zero if not an array class.
 *
 * @param  arraylength  Array of length @b arraydims containing the
 *                        length of array in each of those dimensions.
 *                        E.g., @b arraydims is 4 for new X[7][3][9][2]
 *                        so this parameter will be a 4-element array
 *                        containing the numbers {7, 3, 9, 2}
 *
 * @param lower_dim_array Class index of this array class' next
 *                        lower dimension, e.g. if this is a 3-dim
 *                        array @c @b [[[L then need index
 *                        it as @c @b [[L .
 *
 *
 * @returns   Class index value of allocation.  Throw error if no slots.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no slots are available.@endlink.
 *
 */

jvm_class_index class_static_new(rushort          special_cls,
                                 ClassFile       *pcfs,
                                 jvm_array_dim    arraydims,
                                 jint            *arraylength,
                                 jvm_class_index  lower_dim_array)
{
    jvm_class_index clsidx;

    /* Locate an empty slot */
    clsidx = class_allocate_slot(rtrue);

    /* Abort if no more class table slots */
    if(jvm_class_index_null == clsidx)
    {
        return(jvm_class_index_null);
    }

    /* Initialize class structures */
    class_static_setup(clsidx);



    /*
     * Set up empty class static field lookup table
     * and object instance lookup table now
     * instead of later so that GC_CLASS_NEW()
     * and GC_OBJECT_NEW() know how many fields
     * to anticipate for primative and array classes,
     * namely zero.
     */
    CLASS(clsidx).num_class_static_field_lookups = 0;
    CLASS(clsidx).num_object_instance_field_lookups = 0;


    /*
     * Start GC references on new and valid class.
     * Unlike for objects, this must be done @e immediately
     * so that object_instance_new() can add references to
     * it from @link table_linkage#clsidx table_linkage.clsidx@endlink
     * and write to an initialized structure.
     *
     *     GC_CLASS_NEW(clsidx);
     *
     * ... but not until after num_XXX_field_lookups() is known.
     *
     */


    /* Check for special class instances, then normal ones */
    jvm_object_hash objhash;
    if (CLASS_STATUS_PRIMATIVE & special_cls)
    {
        /* Start GC tracking for class */
        (rvoid) GC_CLASS_NEW(clsidx);

        /*
         * Allocate a primative class
         */

        objhash = object_instance_new(OBJECT_STATUS_EMPTY,
                                      pcfs,
                                      clsidx,
                                      arraydims,
                                      arraylength,
                                      rfalse,
                                      jvm_thread_index_null);

        /*
         * Mark as a primative class, @c @b \<clinit\> done
         * (not applicable)
         */
        CLASS(clsidx).status |= CLASS_STATUS_PRIMATIVE |
                                CLASS_STATUS_CLINIT;
    }
    else
    if (special_cls & CLASS_STATUS_ARRAY)
    {
        /* Start GC tracking for class */
        (rvoid) GC_CLASS_NEW(clsidx);

        /*
         * Allocate an array class-- but treat object as a class object
         * lest it try to initialize the array dimensions, which is
         * @e only something that an array @e instance object can do.
         */

        objhash = object_instance_new(OBJECT_STATUS_CLASS,
                                      pcfs,
                                      clsidx,
                                      arraydims,
                                      arraylength,
                                      rfalse,
                                      jvm_thread_index_null);

        CLASS(clsidx).status |= CLASS_STATUS_ARRAY;

        CLASS(clsidx).arraydims         = arraydims;
        CLASS(clsidx).arraylength       = arraylength;
        CLASS(clsidx).lower_dim_array   = lower_dim_array;

        /*! @todo   Where is this mkref's GC_CLASS_RMREF() ??? */
        (rvoid) GC_CLASS_MKREF_FROM_CLASS(clsidx,
                                         CLASS(clsidx).lower_dim_array);
    }
    else
    {
        /*
         * Allocate a normal class
         */
        CLASS(clsidx).num_class_static_field_lookups =
            class_get_num_static_fields(pcfs);
        CLASS(clsidx).class_static_field_lookup =
            class_get_static_field_lookups(pcfs);

        CLASS(clsidx).num_object_instance_field_lookups =
            class_get_num_object_instance_fields(pcfs);
        CLASS(clsidx).object_instance_field_lookup =
            class_get_object_instance_field_lookups(pcfs);

        /*
         * Start GC tracking for class, now that actual
         * number and type of fields is known
         */
        (rvoid) GC_CLASS_NEW(clsidx);


        objhash = object_instance_new(OBJECT_STATUS_EMPTY,
                                      pcfs,
                                      clsidx,
                                      arraydims,
                                      arraylength,
                                      rfalse,
                                      jvm_thread_index_null);

        /* Delay loading field data until after @b objhash is known */
        CLASS(clsidx).class_static_field_data =
            class_get_static_field_data(clsidx, pcfs);
    }

    /*
     * Declare this class instance as being
     * referenced by a class object
     */
    CLASS(clsidx).class_objhash = objhash;
    (rvoid) GC_OBJECT_MKREF_FROM_CLASS(clsidx, objhash);

    /* normal class definition */
    CLASS(clsidx).status &= ~CLASS_STATUS_NULL;

    /*
     * Declare this class as being referenced, but not here.
     * The calling function must perform this task.
     */
    /* GC_CLASS_MKREF(clsidx); */

    return(clsidx);

} /* END of class_static_new() */


/*!
 * @brief Reload a class in the class table after
 * @c @b java.lang.String has become available.
 *
 * This process does @e not re-read the class file, only redoes
 * class initialization.  The @b class_objhash does not get deleted.
 *
 *
 * @param    clsidxOLD  Class table index of slot to tear down.
 *
 * @returns   New class index of rebuilt class slot.  Throw error if
 *             no slots.
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no slots are available.@endlink.
 *
 *
 * @todo  This function needs more testing.  Also, is it @e really
 *        needed in the implementation?
 */

jvm_class_index class_reload(jvm_class_index clsidxOLD)
{

    if (CLASS(clsidxOLD).status & CLASS_STATUS_INUSE)
    {
        /* Locate an empty slot */
        jvm_class_index clsidxNEW = class_allocate_slot(rtrue);

        /*
         * Save image of class slot to be copied into new slot
         */
        rclass *dupslot = HEAP_GET_METHOD(sizeof(rclass), rfalse);

        memcpy(dupslot, &CLASS(clsidxOLD), sizeof(rclass));


        /* Mark old slot as partially initialized */
        CLASS(clsidxOLD).status |= CLASS_STATUS_NULL;

        /* Should @e always be true */
        if (jvm_object_hash_null != CLASS(clsidxOLD).class_objhash)
        {
            /* Replace old class slot ref with new class slot ref.  */
            OBJECT(dupslot->class_objhash).table_linkage.clsidx =
                                                              clsidxNEW;

            /* Make SURE there is at least 1 class object reference */
            (rvoid) GC_OBJECT_MKREF_FROM_CLASS(
                        clsidxOLD,
                        dupslot->class_objhash);
            (rvoid) GC_CLASS_MKREF_FROM_CLASS(
                        clsidxOLD,
                        dupslot->lower_dim_array);
            (rvoid) GC_CLASS_MKREF_FROM_OBJECT(
                        dupslot->class_objhash,
                        OBJECT(dupslot->class_objhash)
                          .table_linkage
                          .clsidx);

            /* Unwind real class object reference */
            (rvoid) GC_OBJECT_RMREF_FROM_CLASS(
                        clsidxOLD,
                        CLASS(clsidxOLD).class_objhash);
            (rvoid) GC_CLASS_RMREF_FROM_CLASS(
                        clsidxOLD,
                        CLASS(clsidxOLD).lower_dim_array);
            (rvoid) GC_CLASS_RMREF_FROM_OBJECT(
                        CLASS(clsidxOLD).class_objhash,
                        OBJECT(CLASS(clsidxOLD).class_objhash)
                          .table_linkage
                          .clsidx);

            /* DO NOT DO THIS-- use same object, incl. class file
             * object_instance_delete(CLASS(clsidxOLD)
             *                                      .class_objhash);
             */

            CLASS(clsidxOLD).class_objhash = jvm_object_hash_null;
        }

        /* Unhook class object's class ref, then all unhook class refs*/
        (rvoid) GC_CLASS_RMREF_FROM_CLASS(
                    clsidxOLD,
                    CLASS(clsidxOLD).lower_dim_array);
        (rvoid) GC_CLASS_RMREF_FROM_CLASS(jvm_class_index_null,
                                          clsidxOLD);
        (rvoid) linkage_unresolve_class(clsidxOLD);

        /*
         * Finalize garbage collection status of this class instance,
         * but DO NOT do class_static_delete()!
         */
        (rvoid) GC_CLASS_DELETE(clsidxOLD, rfalse);

        /*
         * Wipe out old structures, see similar in class_static_delete()
         */
        CLASS(clsidxOLD).class_static_field_lookup =
                                              (jvm_class_index *) rnull;
        CLASS(clsidxOLD).num_class_static_field_lookups = 0;

        CLASS(clsidxOLD).object_instance_field_lookup =
                                              (jvm_class_index *) rnull;
        CLASS(clsidxOLD).num_object_instance_field_lookups = 0;


        CLASS(clsidxOLD).status = CLASS_STATUS_EMPTY;

        /*
         * Copy slot image into new slot
         */
        dupslot->status |= CLASS_STATUS_NULL; /* Partial init */

        memcpy(&CLASS(clsidxNEW), dupslot, sizeof(rclass));

        /* Reverse of first pair of mkref/rmref */
        (rvoid) GC_OBJECT_MKREF_FROM_CLASS(
                    clsidxNEW,
                    CLASS(clsidxNEW).class_objhash);
        (rvoid) GC_CLASS_MKREF_FROM_CLASS(
                    clsidxNEW,
                    CLASS(clsidxNEW).lower_dim_array);
        (rvoid) GC_CLASS_MKREF_FROM_OBJECT(
                    CLASS(clsidxNEW).class_objhash,
                    OBJECT(CLASS(clsidxNEW).class_objhash)
                      .table_linkage
                      .clsidx);

        (rvoid) GC_OBJECT_RMREF_FROM_CLASS(
                    clsidxOLD,
                    dupslot->class_objhash);
        (rvoid) GC_CLASS_RMREF_FROM_CLASS(
                    clsidxOLD,
                    dupslot->lower_dim_array);
        (rvoid) GC_CLASS_RMREF_FROM_OBJECT(
                    dupslot->class_objhash,
                    OBJECT(dupslot->class_objhash)
                      .table_linkage
                      .clsidx);

            /* Unwind real class object reference */
        HEAP_FREE_METHOD(&dupslot);

        return(clsidxNEW);
    }
    else
    {
        /* Error-- slot was already free */
        return(jvm_class_index_null);
    }

} /* END of class_reload() */


/*!
 * @brief Un-reserve a slot from the class table.
 *
 * This is the reverse of the process of class_static_new() above.
 *  Only tear down the heap allocations and mark the slot as empty.
 * Leave the rest of the data in place for post-mortem.  When the
 * slot gets allocated again, any zeroing out of values will just
 * get overwritten again, so don't bother.
 *
 * @todo  Make @e sure all objects of this class type have been
 *        destroyed @e before destroying this class itself!
 *
 * @todo  This function may be used to declutter the class table when
 *        a class has not been used for some period of time (including
 *        any static methods and fields, watch out for static final
 *        constants), so as to free up its slot for other purposes.
 *
 *
 * @param    clsidx   Class index value of allocation.
 *
 * @param    rmref    @link #rtrue rtrue@endlink if @e class references
 *                    should be removed, which is NOT SO during JVM
 *                    shutdown.  Regardless of this value, @e object
 *                    references are always removed.
 *
 *
 * @returns   Same class index as input if slot was freed, else
 *             @link #jvm_class_index_null jvm_class_index_null@endlink
 *             if slot was already free.
 *
 */

jvm_class_index class_static_delete(jvm_class_index clsidx,
                                    rboolean        rmref)
{
    if (CLASS(clsidx).status & CLASS_STATUS_INUSE)
    {
        /*!
         * @todo Determine what to do, if anything, when rfalse is
         *       returned from linkage_unresolve_class().  Is the
         *       class slot unusable?  Should class_static_delete()
         *       proceed?
         */
        (rvoid) linkage_unresolve_class(clsidx);

        /*!
         * @todo  Is there anything equivalent to calling
         *        @c @b java.lang.Object.finalize() for an object
         *        that must be invoked before unloading a class?
         */

        if (jvm_object_hash_null != CLASS(clsidx).class_objhash)
        {
            (rvoid) GC_OBJECT_RMREF_FROM_CLASS(
                        clsidx,
                        CLASS(clsidx).class_objhash);

            object_instance_finalize(CLASS(clsidx).class_objhash,
                                     JVMCFG_GC_THREAD);

            (rvoid) object_instance_delete(
                        CLASS(clsidx).class_objhash,
                        rtrue);

            /* CLASS(clsidx).class_objhash = jvm_object_hash_null;*/
        }

        /* Finalize garbage collection status of this class instance */
        if (rtrue == rmref)
        {
            (rvoid) GC_CLASS_RMREF_FROM_CLASS(clsidx,
                                         CLASS(clsidx).lower_dim_array);
        }
        (rvoid) GC_CLASS_DELETE(clsidx, rfalse);

        if (rnull != CLASS(clsidx).class_static_field_data)
        {
            /* Remove class static field reference markings */
            u2 ncsfl = CLASS(clsidx).num_class_static_field_lookups;
            jvm_field_lookup_index csflidx;
            for (csflidx = 0; csflidx < ncsfl; csflidx++)
            {
                (rvoid) GC_CLASS_FIELD_RMREF(clsidx, csflidx);
            }

            HEAP_FREE_DATA(CLASS(clsidx).class_static_field_data);
        }

        if (rnull != CLASS(clsidx).class_static_field_lookup)
        {
            HEAP_FREE_DATA(CLASS(clsidx).class_static_field_lookup);
        }
        /* CLASS(clsidx).class_static_field_lookup =
                                           (jvm_class_index *) rnull; */
        /* CLASS(clsidx).num_class_static_field_lookups = 0; */

        if (rnull != CLASS(clsidx).object_instance_field_lookup)
        {
            HEAP_FREE_DATA(CLASS(clsidx).object_instance_field_lookup);
        }
        /* CLASS(clsidx).object_instance_field_lookup =
                                           (jvm_class_index *) rnull; */
        /* CLASS(clsidx).num_object_instance_field_lookups = 0; */


        /* CLASS(clsidx).status                 = CLASS_STATUS_EMPTY; */

        CLASS(clsidx).status &= ~CLASS_STATUS_INUSE;

        return(clsidx);
    }
    else
    {
        /* Error-- slot was already free */
        return(jvm_class_index_null);
    }

} /* END of class_static_delete() */


/*!
 * @brief Scan class table using CP table entry for presence of a class
 * of specific name and number of dimensions.
 *
 *
 * @param  clsname    UTF8 string pointer to a class
 *                    name, possibly with array dimensions:
 *                    @c @b [[Lsome/class/path/SomeClassname;
 *                    has two array dimensions.  Can also
 *                    accept classes that are null-terminated
 *                    strings @e without class formatting.
 *                    Such strings @e may start with @c @b L
 *                    but will @e not end with @c @b ; .
 *
 *
 * @returns its index in pjvm->class if present, else
 *         @link #jvm_class_index_null jvm_class_index_null@endlink.
 *
 */
jvm_class_index class_find_by_cp_entry(cp_info_dup *clsname)
{
    jvm_class_index clsidx;

    jvm_array_dim arraydims =
        utf_get_utf_arraydims(PTR_THIS_CP_Utf8(clsname));

    for (clsidx = JVMCFG_FIRST_CLASS;
         clsidx < JVMCFG_MAX_CLASSES;
         clsidx++)
    {
        if ( (CLASS_STATUS_INUSE  & CLASS(clsidx).status) &&
            !(CLASS_STATUS_NULL   & CLASS(clsidx).status))
        {
            ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;
            u2 cpidx = pcfs->this_class;

            /*
             * Check if this slot's class name matches input request.
             * Must match BOTH @b arraydims and string name.
             */
            if (arraydims != CLASS(clsidx).arraydims)
            {
                continue;
            }

            if (0 == utf_classname_strcmp(
                         PTR_THIS_CP_Utf8(clsname),
                         pcfs,
                         cpidx))
            {
                return(clsidx);
            }
        }
    }

    /* Return NULL if entry not found */
    return(jvm_class_index_null);

} /* END of class_find_by_cp_entry() */


/*!
 * @brief Retrieve by (rchar *) name a class index to a class.
 *
 *
 * @param  clsname           Null-terminated string of name of class.
 *
 *
 * @returns class index of located class, otherwise
 *          @link #jvm_class_index_null jvm_class_index_null@endlink.
 *
 */
jvm_class_index class_find_by_prchar(rchar *clsname)
{
    cp_info_dup *pcip_clsname = nts_prchar2utf(clsname);

    jvm_method_index rc =
        class_find_by_cp_entry(pcip_clsname);

    HEAP_FREE_DATA(pcip_clsname);

    return(rc);

} /* END of class_find_by_prchar() */


/*!
 * @brief Load primative classes for @c @b java.lang.Class
 *
 * Load primative classes as opposed to a real class
 * @c @b LSome/Package/Name/SomeClassName; .
 * These will be known by their primative types: @c @b I
 * for "integer", @c @b S for "short", etc.
 *
 * Classes loaded with this method will typically @e never have any
 * references to them except from their class object created
 * within class_static_new(), yet are completely valid and are used
 * intensively by class type processing.  Because they never have
 * references @e removed, they will never be marked for garbage
 * collection, either. In this way, they are permanently available
 * to the JVM for class type processing.
 *
 * @param  basetype   One of the primative base types BASETYPE_CHAR_x
 *
 *
 * @returns class table index to loaded primative [pseudo-]class,
 *         ready for use.  Throw error if could not load.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no slots are available.@endlink.
 *
 */
jvm_class_index class_load_primative(u1 basetype)
{
    ClassFile *pcfs = classfile_allocate_primative(basetype);

    return(class_static_new(CLASS_STATUS_PRIMATIVE,
                            pcfs,
                            LOCAL_CONSTANT_NO_ARRAY_DIMS,
                            (jint *) rnull,
                            jvm_class_index_null));

} /* END of class_load_primative() */


/*!
 * @name DEFAULT SYSTEM CLASS LOADER:
 *
 * @brief perform the duties of @c @b java.lang.ClassLoader

 * Using either null-terminated strings or UTF strings from
 * @link #CONSTANT_Utf8_info CONSTANT_Utf8_info@endlink or
 * @link #CONSTANT_Class_info CONSTANT_Class_info@endlink
 * constant_pool entries, the following three functions function
 * as the system default class loader that can be invoked three
 * different ways.  It searches @b CLASSPATH, reads in a class
 * file, parses its contents, and loads it into the class table
 * and object table, and performs static initialization.
 *
 * Notice that the array formatting @e must already be included in
 * @b clsname (if this is an array class), and that the @b arraylength
 * array must contain the same number of integers as described
 * in @b clsname.  No checking is done for this in this function.
 *
 * @attention This function performs the task of the method
 *
 * @verbatim
     ClassLoader.defineClass(String name, byte[] b, int off, int len)
   @endverbatim
 *
 * but combines the operands effectively into,
 *
 * @verbatim
       ClassLoader.defineClass(String name)
   @endverbatim
 *
 *   where @b name is a null-terminated string passed as the only parm,
 *   and @b b is the byte array read in by classfile_readclassfile()
 *   or classfile_readjarfile(), and @b off is zero, and @b len being
 *   discovered by classfile_read_XXXfile().
 *
 * @todo  Spec section 5.3.4 <b>Loading Constraints</b> has not been
 *        considered in this implementation.  It needs to be looked at.
 *
 * @todo  Convert this function from recursive (easy to write) into
 *        iterative (easier to run and maintain).
 *
 *
 * @param  clsname   This parameter may be one of three styles:
 *
 * <ul>
 * <li> for class_load_from_cp_entry_utf() only: a
 *                   CONSTANT_Utf8_info constant_pool entry string
 *                   containing unqualified class name:
 *
 *                   @c @b Lsome/class/path/SomeClassname;
 *
 *                   with possible array specification:
 *
 *                   @c @b [[[Lsome/class/path/SomeClassname;
 *
 *                   This example has three array dimensions.
 * </li>
 *
 * <li> for class_load_from_prchar() only: a null-terminated string
 *                   containing unqualified class name.
 *                   See above for further comments.
 * </li>
 *
 * <li> for class_load_from_cp_entry_class() only: a
 *                   CONSTANT_Class_info constant_pool entry containing
 *                   constant_pool entry to unqualified class name.
 *                   See above for further comments.
 * </li>
 * </ul>
 *
 * @param find_registerNatives When @link #rtrue rtrue@endlink,
 *                   will return the ordinal for
 *                   @link #JVMCFG_JLOBJECT_NMO_REGISTER 
                            JVMCFG_JLOBJECT_NMO_REGISTER@endlink and
 *                   @link #JVMCFG_JLOBJECT_NMO_UNREGISTER 
                            JVMCFG_JLOBJECT_NMO_UNREGISTER@endlink
 *                   as well as the other ordinals.  Once JVM
 *                   initialization is complete, this should always
 *                   be @link #rfalse rfalse@endlink because all future
 *                   classes should @e never have local ordinals.
 *
 * @param arraylength Array of number of elements in @e each array
 *                    dimension.  If not needed, set to
 *                    @link #rnull rnull@endlink.
 *                    Notice that @b clsname will determine how many
 *                    array dimensions are needed.  In the above
 *                    example of 3 dimensions, an array of 3 numbers
 *                    must be passed here.  If the declaration was,
 *                    <b><code>new SomeClassname[4][7][8]</code></b>,
 *                    then the array {4, 7, 8} must be passed here.
 *
 *
 * @returns class table index to loaded class, ready for use, or
 *          @link #jvm_class_index_null jvm_class_index_null@endlink
 *          if could not load.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no slots are available.@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR
 *         @link #JVMCLASS_JAVA_LANG_INTERNALERROR
 *         with bad clsname constant_pool entry type@endlink.
 *         Meaninful only for
 *         @link #class_load_from_cp_entry_utf()
                  class_load_from_cp_entry_utf()@endlink
 *
 */
/*         and @link #@class_load_from_cp_entry_class()
                   class_load_from_cp_entry_class()@endlink only. */

/*@{ */ /* Begin grouped definitions */

jvm_class_index
    class_load_from_cp_entry_utf(cp_info_dup *clsname,
                                 rboolean     find_registerNatives,
                                 jint        *arraylength)
{
    /* Disallow all but UTF8 constant_pool entries */
    if (CONSTANT_Utf8 != PTR_THIS_CP_Utf8(clsname)->tag)
    {
        /* Somebody goofed */
        exit_throw_exception(EXIT_JVM_INTERNAL,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* 
     * Be @e VERY conservative about local var allocation
     * due to upcoming deep recursion.
     */
    jvm_class_index rc;

    /*
     * Ignore this request if class already loaded, but distinguish
     * between array and non-array classes
     */
    rc = class_find_by_cp_entry(clsname);

    /* If class already loaded, then do nothing */
    if (jvm_class_index_null != rc)
    {
        return(rc);
    }

    /* Inquire if this is an array class */
    jvm_array_dim arraydims =
        utf_get_utf_arraydims(PTR_THIS_CP_Utf8(clsname));

    /*
     * If an array class, load array class of 1 lower dimension,
     * or non-array version if this is array dimension 1.
     */
    if (LOCAL_CONSTANT_NO_ARRAY_DIMS < arraydims)
    {
        rchar *pnextdim = utf_utf2prchar(PTR_THIS_CP_Utf8(clsname));

        /*
         * WARNING! RECURSIVE CALL! This call goes 1 level per
         *          array dimension.
         */
        rc = class_load_from_prchar(&pnextdim[1 * sizeof(u1)],
                                    find_registerNatives,
                                    (rnull == arraylength)
                                        ? (jint *) rnull
                                        : &arraylength[1]);

        HEAP_FREE_DATA(pnextdim);
    }

    /*
     * Be conservative about number of local variables above
     * due to possibility of deep recursion (avert stack overflow).
     * Now that recursion is over, can allocate locals more freely.
     */
    {
        /* Use name more descriptive of new role.  Known non-zero. */
        jvm_class_index lower_dim_clsidx = rc;

        /*
         * For an array, locate class file structure of non-array
         * version and mark class as being an array class (through
         * agency of class_static_new(,,arraydims,)).  If not an
         * array class, load the class file.  This will be
         * known to array classes as the "non-array version" of
         * the class.  All array class file pointers will point
         * to this class file structure.
         */
        u1              *pcfd;
        ClassFile       *pcfs;
        rchar           *pcname;
        jvm_class_index  clsidx;

        if (LOCAL_CONSTANT_NO_ARRAY_DIMS < arraydims)
        {
            /* Locate non-array version of class in class table */
            jvm_class_index next_clsidx = lower_dim_clsidx;

            while(jvm_class_index_null !=
                  CLASS(next_clsidx).lower_dim_array)
            {
                next_clsidx =
                    CLASS(CLASS_OBJECT_LINKAGE(next_clsidx)->clsidx)
                        .lower_dim_array;
            }

            /* Having traversed the indices, found non-array version */
            ClassFile *pcfs = CLASS_OBJECT_LINKAGE(next_clsidx)->pcfs;

            /* Load into class and object tables */
            clsidx =
                class_static_new(((LOCAL_CONSTANT_NO_ARRAY_DIMS ==
                                   arraydims)
                                      ? CLASS_STATUS_EMPTY
                                      : CLASS_STATUS_ARRAY),
                                  pcfs,
                                  arraydims,
                                  arraylength,
                                  lower_dim_clsidx);

        } /* if arraydims */
        else
        {
            cp_info_dup *pnofmt =
                             utf_utf2utf_unformatted_classname(clsname);

            pcname = classpath_get_from_cp_entry_utf(pnofmt);

            HEAP_FREE_DATA(pnofmt);

            GENERIC_FAILURE1_THROWERROR((rnull == pcname),
                                        DMLNORM,
                                        "class_load_from_prchar",
                                        "Cannot locate class %s",
                                        clsname,
                                        EXIT_JVM_CLASS,
                               JVMCLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR,
                                        pcname,
                                        rnull);

            /*
             * Check if JAR file or class file and read in the class
             * file data stream as a simple binary image.
             */
            if (rtrue == classpath_isjar(pcname))
            {
                pcfd = classfile_readjarfile(pcname);
            }
            else
            {
                pcfd = classfile_readclassfile(pcname);
            }

            GENERIC_FAILURE1_THROWERROR((rnull == pcfd),
                                        DMLNORM,
                                        "class_load_from_prchar",
                                        "Cannot read file %s",
                                        pcname,
                                        EXIT_JVM_CLASS,
                                   JVMCLASS_JAVA_LANG_CLASSFORMATERROR,
                                        pcfd,
                                        pcname);

            /*! @todo  Throw @b UnsupportedClassVersionError */

            /* Parse out the class file input stream */
            pcfs = classfile_loadclassdata(pcfd);

            /* Load into class and object tables */
            clsidx = class_static_new(CLASS_STATUS_EMPTY,
                                      pcfs,
                                      LOCAL_CONSTANT_NO_ARRAY_DIMS,
                                      (jint *) rnull,
                                      jvm_class_index_null);

            /* Don't need file image any more */
            HEAP_FREE_DATA(pcfd);

            GENERIC_FAILURE1_THROWERROR((rnull == pcfs),
                                        DMLNORM,
                                        "class_load_from_prchar",
                                        "Invalid class file %s",
                                        pcname,
                                        EXIT_JVM_CLASS,
                                   JVMCLASS_JAVA_LANG_CLASSFORMATERROR,
                                        pcname,
                                        rnull);

            /* Don't need absolute path name any more, either */
            HEAP_FREE_DATA(pcname);

            /*!
             * @internal Load superclass
             *
             * @todo  (The @c @b \<clinit\> procedure is run when the
             *        JVM virtual machine execution engine moves
             *        a new class from the START state into the
             *        RUNNABLE state.)
             */
            if (jvm_class_index_null != pcfs->super_class)
            {
                CONSTANT_Utf8_info *name = PTR_CP1_CLASS_NAME(pcfs,
                                                     pcfs->super_class);

                rchar *super_name = utf_utf2prchar_classname(name);

                /*!
                 * @internal WATCH OUT!  RECURSIVE CALL!  This will
                 * recurse until super_class is a
                 * @c @b java.lang.Object, where
                 * @link ClassFile.super_class
                   ClassFile.super_class@endlink is 0 (per JVM spec.
                 * Throw error if could not load superclass.
                 * Don't care about its class index, as
                 * that is also available in other places.
                 *
                 * @todo Make @e sure that this superclass and all of
                 *       its superclasses are not only loaded, but also
                 *       linked and have @c @b \<clinit\> run also.
                 *
                 */
                (rvoid) class_load_from_prchar(super_name,
                                               find_registerNatives,
                                               (jint *) rnull);

                /*
                 * If error above, neither HEAP_FREE_DATA(super_name)
                 * nor classfile_unloadclassdata(pcfs) will be called.
                 */
            }

            /* Mark as needing @c @b \<clinit\> in JVM class startup */
            CLASS(clsidx).status |= CLASS_STATUS_DOCLINIT;

        } /* if arraydims else */

        /* Make a pass at resolving constant_pool linkages */
        (rvoid) linkage_resolve_class(clsidx, find_registerNatives);

        /*
         * Class is ready for JVM execution.
         */
        return(clsidx);

    } /* local block */

} /* END of class_load_from_cp_entry_utf() */


jvm_class_index class_load_from_prchar(rchar    *clsname,
                                       rboolean  find_registerNatives,
                                       jint     *arraylength)
{
    cp_info_dup *cp_clsname = nts_prchar2utf(clsname);

    jvm_class_index clsidx =
        class_load_from_cp_entry_utf(cp_clsname,
                                     find_registerNatives,
                                     arraylength);

    HEAP_FREE_DATA(cp_clsname);

    return(clsidx);

} /* END of class_load_from_prchar() */


#if 0
/* Can use if want to add @b pcfs to parm list or some such */
jvm_class_index
    class_load_from_cp_entry_class(cp_info_dup *clsname,
                                   rboolean     find_registerNatives,
                                   jint        *arraylength)
{
    /* Disallow all but CLASS constant_pool entries */
    if (CONSTANT_Class != PTR_THIS_CP_Class(clsname)->tag)
    {
        /* Somebody goofed */
        exit_throw_exception(EXIT_JVM_INTERNAL,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    rchar *prchar_clsname =
        utf_utf2prchar(
            PTR_THIS_CP_Utf8(
                &pcfs->constant_pool
                 [PTR_THIS_CP_Class(clsname)->name_index]));

    jvm_class_index clsidx = class_load_from_prchar(prchar_clsname,
                                                   find_registerNatives,
                                                    arraylength);

    HEAP_FREE_DATA(prchar_clsname);

    return(clsidx);

} /* END of class_load_from_cp_entry_class() */
#endif

/*@} */ /* End of grouped definitions */


/*!
 * @brief All-purpose class loading, load, resolve, @c @b \<clinit\> ,
 * each step only run if needed.
 *
 * Load a class into the JVM, resolve its linkages, and run its
 * @c @b \<clinit\> method, if any, typically on the system thread.
 * All three steps are optional and will be invoked @e only if needed.
 * Therefore it is possible that @e none of the steps will be run.
 * However, if @e any step needs to performed, that step will be run.
 *
 *
 * @param  clsname           Null-terminated string of unformatted
 *                           class name.(Formatting is okay, but don't
 *                           pass an array class)
 *
 * @param  thridx            If a non-null thread index is passed in,
 *                           use the current state of that thread to
 *                           load the class and run its
 *                           @c @b \<clinit\> .
 *                           If null thread index is passed in, use
 *                           the @b usesystemthread parameter to
 *                           further clarify which thread to load up
 *                           with class @c @b \<clinit\> .
 *
 * @param  usesystemthread   Load onto system thread when
 *                           @link #rtrue rtrue@endlink, use
 *                           an available thread otherwise.
 *
 * @param find_registerNatives When @link #rtrue rtrue@endlink,
 *                      will return the ordinal for
 *                      @link #JVMCFG_JLOBJECT_NMO_REGISTER 
                               JVMCFG_JLOBJECT_NMO_REGISTER@endlink and
 *                      @link #JVMCFG_JLOBJECT_NMO_UNREGISTER 
                               JVMCFG_JLOBJECT_NMO_UNREGISTER@endlink
 *                      as well as the other ordinals.  Once JVM
 *                      initialization is complete, this should always
 *                      be @link #rfalse rfalse@endlink because all
 *                      future classes should @e never have local
 *                      ordinals.
 *
 *
 * @returns class index of loaded class, whether or not a
 *          @c @b \<clinit\> method was available.  Throw
 *          error if not slots available.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no slots are available.@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR
 *         @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if invalid clsidx@endlink
 *
 * @throws JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR
 *         @link #JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR
           if invalid field linked by class@endlink
 *
 * @throws JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR
 *         @link #JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR
           if invalid method linked by class@endlink
 *
 * @todo  Any JAR file passed in here that has a @c @b [ in its
 *        first one or more characters may be interpreted as an array
 *        class.  I have not tested this, and it is an oddball,
 *        but somebody needs to make sure that either, (a) such
 *        a name is @e never passed in, which is preferred due to
 *        standard industry file naming practices, and/or (b) that
 *        no array processing happens in the lower levels.
 *
 */


jvm_class_index class_load_resolve_clinit(rchar        *clsname,
                                      jvm_thread_index  thridx,
                                          rboolean      usesystemthread,
                                          rboolean find_registerNatives)
{
    jvm_class_index clsidx = class_load_from_prchar(clsname,
                                                   find_registerNatives,
                                                    (jint *) rnull);

    /*
     * Make another pass at class linkage (if completely linked,
     * request is ignored).
     */
    (rvoid) linkage_resolve_class(clsidx, find_registerNatives);

    /*
     * If @c @b \<clinit\> has been run due to previous class load,
     * done
     */
    if (CLASS_STATUS_CLINIT & CLASS(clsidx).status)
    {
        return(clsidx);
    }

    /*
     * Need to explicitly run @c @b \<clinit\> since not
     * in main JVM loop
     */
    jvm_method_index mthidx =
        method_find_by_prchar(clsidx,
                              LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR,
                           LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR_PARMS);
    if (jvm_method_index_bad == mthidx)
    {
        /*
         * Return valid class index even if no @c @b \<clinit\>
         * was available
         */
        return(clsidx);
    }
    else
    {
        jvm_thread_index thridxLOAD;

        if (jvm_thread_index_null != thridx)
        {
            thridxLOAD = thridx;

            /*!
             * @internal see similar logic for loading a new stack
             * frame and PC in thread_new_common()
             *
             */

            ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

            jvm_attribute_index codeatridx =
                pcfs
                  ->methods[mthidx]
                    ->LOCAL_method_binding
                      .codeatridxJVM;

            if (jvm_attribute_index_bad == codeatridx)
            {
                /*!
                 * @todo Currently, return valid class index even
                 * if no @c @b \<clinit\> was available.  Is this
                 * correct? Or should it throw a @b VerifyError since
                 * a method was declared, yet had no code area?  Take
                 * the easy way out for now, evaluate and maybe fix
                 * later.
                 */
                return(clsidx);
            }

            jvm_attribute_index excpatridx =
                pcfs
                  ->methods[mthidx]
                    ->LOCAL_method_binding
                      .excpatridxJVM;

            Code_attribute *pca =
                (Code_attribute *)
                &pcfs->methods[mthidx]->attributes[codeatridx]->ai;

            /* Check for stack overflow if this frame is loaded */
            if (JVMCFG_MAX_SP <= GET_SP(thridx) +
                                 JVMREG_STACK_MIN_FRAME_HEIGHT +
                                 JVMREG_STACK_PC_HEIGHT +
                                 pca->max_stack +
                                 pca->max_locals)
            {
                exit_throw_exception(EXIT_JVM_CLASS,
                                   JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR);
/*NOTLOADED*/
            }

            /*
             * Save old stack frame for @b RETURN from @c b \<clinit\>
             */
            PUSH_FRAME(thridx, pca->max_locals);

            /* Load PC of @c @b \<clinit\> method */
            PUT_PC_IMMEDIATE(thridx,
                             clsidx,
                             mthidx,
                             codeatridx,
                             excpatridx,
                             CODE_CONSTRAINT_START_PC);
        }
        else
        {
            /*
             * Class is now known to be available,
             * so start it on the system thread.
             *
             * There is no old stack frame to save for @b RETURN
             * from @c @b \<clinit\> because this a new thread.  The
             * starting stack frame is set up by thread_class_load()
             * and returning to it will @b COMPLETE the thread.
             */
            thridxLOAD =
                thread_class_load(clsname,
                                  LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR,
                            LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR_PARMS,
                                  THREAD_PRIORITY_MAX,
                                  rfalse,
                                  usesystemthread,
                                  find_registerNatives);
        }

        jvm_manual_thread_run(thridxLOAD,
                              ((rtrue == usesystemthread) ||
                               (jvm_thread_index_null == thridx))
                                ? rtrue /* Die if not prev @b RUN */
                                : rfalse,
                              clsname,
                              LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR,
                           LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR_PARMS);

        /* Class @c @b \<clinit\> has been completed (manually) */
        CLASS(clsidx).status &= ~CLASS_STATUS_DOCLINIT;
        CLASS(clsidx).status |= CLASS_STATUS_CLINIT;


        /*Return class index of class loaded,resolved,and ready to use*/
        return(clsidx);
    }

} /* END of class_load_resolve_clinit() */


/*!
 * @brief Retrieve data from a @link #CONSTANT_Class_info
   CONSTANT_xxx_info@endlink structure and insert it
 * into a slot in the field data table.  Also implements table 4.6.
 *
 *
 * @param   clsidx        Class table index of slot having class loaded
 *
 * @param   pcfs          Pointer to ClassFile area
 *
 * @param   fldidx        Class file field info table index
 *
 * @param   field_data    Field data tbl in class tbl entry(on the heap)
 *
 * @param   fluidx        Field lookup index into field_data table
 *
 * @param  staticmark     If @link #rtrue rtrue@endlink, this
 *                        invocation is for a class static field lookup
 *                        table, so call
 *                        GC_CLASS_FIELD_MKREF(clsidx, fluidx) for
 *                        reference types (namely CONSTANT_String
 *                        types).  When @link #rfalse rfalse@endlink,
 *                        call instead
 *                        GC_OBJECT_FIELD_MKREF(objhash, fluidx) for
 *                        reference types.  It is for this @e specific
 *                        purpose that @b objhash is supplied below:
 *
 * @param  objhash        If @b staticmark is
 *                        @link #rfalse rfalse@endlink,
 *                        this is the object
 *                        hash of the field lookup table whose reference
 *                        types should be GC marked, otherwise ignored.
 *                        See @b staticmark above for details.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
static
   rvoid class_get_constant_field_attribute(jvm_class_index  clsidx,
                                            ClassFile       *pcfs,
                                            jvm_field_index  fldidx,
                                            jvalue          *field_data,
                                      jvm_field_lookup_index fluidx,
                                            rboolean         staticmark,
                                            jvm_object_hash  objhash)
{
    /* Check for an initial value for each field */
     jvm_attribute_index atridx;
     for (atridx = 0;
          atridx < pcfs->fields[fldidx]->attributes_count;
          atridx++)
     {
         rulong *prl8;
         rdouble *prd8;

         u4 *pu4, *pu4h, *pu4l;
         jint vali;
         jlong vall;
         jfloat valf;
         jdouble vald;

         jvalue  val;

         jvm_constant_pool_index constantvalue_index;

         switch (cfattrib_atr2enum(pcfs,
                                   pcfs->fields[fldidx]
                                         ->attributes[atridx]
                                           ->ai.attribute_name_index))
         {
             case LOCAL_SIGNATURE_ATTRIBUTE:
                 /*! @todo  Need to recognize signatures */
                 break;

             case LOCAL_CONSTANTVALUE_ATTRIBUTE:
                 constantvalue_index = 
                     ((ConstantValue_attribute *)
                      &pcfs->fields[fldidx]->attributes[atridx]->ai)
                     ->constantvalue_index;

                 /* Spec table 4.6 implementation */
                 switch(CP_TAG(pcfs, constantvalue_index))
                 {
                     case CONSTANT_Long:
                         pu4h = &PTR_CP_ENTRY_TYPE(
                                     CONSTANT_Long_info,
                                     pcfs,
                                     constantvalue_index)->high_bytes;

                         pu4l = &PTR_CP_ENTRY_TYPE(
                                     CONSTANT_Long_info,
                                     pcfs,
                                     constantvalue_index)->low_bytes;

                         /*
                          * if WORDSIZE/32/64 mismatches -m32/-m64,
                          * the <code>JBITS * sizeof(u4)<code>
                          * calculation @e will cause a runtime-visible
                          * compiler warning!
                          */
/*
 *                       vall = (jlong)
 *                          ((((julong) *pu4h) << (JBITS * sizeof(u4)))|
 *                           ((julong) *pu4l));
 */

/*! @todo  Above logic works, 64-bit logic below needs testing: */
                         /* LS word always follows MS word */
                         prl8 = (rulong *) pu4h;
                         vall = (jlong) GETRL8(prl8);

                         val._jlong = vall;
                         break;

                     case CONSTANT_Float:
                         pu4 = &PTR_CP_ENTRY_TYPE(
                                    CONSTANT_Float_info,
                                    pcfs,
                                    constantvalue_index)
                                ->bytes;

                         valf = (jfloat) (jint) *pu4;

                         val._jfloat = valf;
                         break;

                     case CONSTANT_Double:
                         pu4h = &PTR_CP_ENTRY_TYPE(
                                     CONSTANT_Long_info,
                                     pcfs,
                                     constantvalue_index)->high_bytes;
                         pu4l = &PTR_CP_ENTRY_TYPE(
                                     CONSTANT_Long_info,
                                     pcfs,
                                     constantvalue_index)->low_bytes;

                         /*
                          * if WORDSIZE/32/64 mismatches -m32/-m64,
                          * the <code>JBITS * sizeof(u4)<code>
                          * calculation @e will cause a runtime-visible
                          * compiler warning!
                          */
/*
 *                       vald = (jdouble)
 *                          ((((julong) *pu4h) << (JBITS * sizeof(u4)))|
 *                           ((julong) *pu4l));
 */

/*! @todo  Above logic works, 64-bit logic below needs testing: */
                         /* LS word always follows MS word */
                         prd8 = (rdouble *) pu4h;
                         vald = (jdouble) GETRL8((rulong *) prd8);


                         val._jdouble = vald;
                         break;

                     case CONSTANT_Integer:
                         pu4 = &PTR_CP_ENTRY_TYPE(
                                    CONSTANT_Integer_info,
                                    pcfs,
                                    constantvalue_index)
                                ->bytes;

                         vali = (jint) *pu4;

                         /*
                          * Casting to shorter types, namely
                          * (jshort), (jchar), (jbyte), and
                          * (jboolean), is done during field
                          * access, not here.
                          */
                         val._jint = vali;

                         break;
    

                     case CONSTANT_String:
                         /*!
                          * @todo Load up this string into a
                          * @c @b java.lang.String using the
                          * source from
                          *
                          * <b><code> pcfs->constant_pool
                                         [PTR_CP_ENTRY_TYPE(
                                              CONSTANT_String_info,
                                              pcfs,
                                              constantvalue_index)
                                          ->string_index]</code></b>
                          *
                          * But do not store directly into,
                          *
                          * <b><code>val._jstring = ... </code></b>
                          *
                          * Instead, store the resulting object
                          * hash from the algorithm shown in
                          * @link #jvm_init() jvm_init()@endlink
                          * where the @link #main() main()@endlink
                          * parameter @c @b argv[] array is loaded
                          * into the Java edition of the same.
                          * The pseudocode for this operation is
                          * shown there.
                          *
                          * <em>DO NOT</em> do this until the class
                          * initialization is complete for
                          * @c @b java.lang.String or the results
                          * may be arbitrary or even fatal.  A
                          * well-formed class library will not
                          * attempt such an operation.
                          *
                          */


                         /*
                          * DO NOT do GC_OBJECT_MKREF(val._jstring)
                          * since it is part of this class
                          * definition, that is, self-referential.
                          */



                         /*
                          * Since strings are constant, this is the
                          * only opportunity to mark this field.
                          */
                         if (rtrue == staticmark)
                         {
                             GC_CLASS_FIELD_MKREF(clsidx, fluidx);
                         }
                         else
                         {
                             /*
                              * Rearranged higher level logic to
                              * make sure that @b objhash was valid
                              * by the time this call occurs here:
                              */
                             GC_OBJECT_FIELD_MKREF(objhash, fluidx);
                         }
                         break;

                 } /* switch constantvalue_index */


                 /* Copy constant value into result array */
                 memcpy(&field_data[fluidx++], &val, sizeof(jvalue));
                 break;

             /* Satisfy compiler that all cases are handled */
             case LOCAL_UNKNOWN_ATTRIBUTE:
             case LOCAL_CODE_ATTRIBUTE:
             case LOCAL_EXCEPTIONS_ATTRIBUTE:
             case LOCAL_INNERCLASSES_ATTRIBUTE:
             case LOCAL_ENCLOSINGMETHOD_ATTRIBUTE:
             case LOCAL_SYNTHETIC_ATTRIBUTE:
             case LOCAL_SOURCEFILE_ATTRIBUTE:
             case LOCAL_LINENUMBERTABLE_ATTRIBUTE:
             case LOCAL_LOCALVARIABLETABLE_ATTRIBUTE:
             case LOCAL_LOCALVARIABLETYPETABLE_ATTRIBUTE:
             case LOCAL_DEPRECATED_ATTRIBUTE:
             case LOCAL_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE:
             case LOCAL_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE:
             case LOCAL_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE:
             case LOCAL_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE:
             case LOCAL_ANNOTATIONDEFAULT_ATTRIBUTE:
                 break;
         } /* switch cfattrib_atr2enum() */

     } /* for atridx */

     return;

} /* END of class_get_constant_field_attribute() */


/*!
 * @brief Report number of class static fields are in a class file.
 *
 *
 * @param    pcfs  Pointer to ClassFile area
 *
 *
 * @returns  number of fields marked ACC_STATIC.
 *
 */
u2 class_get_num_static_fields(ClassFile *pcfs)
{
    jvm_field_index fldidx;
    u2 rc = 0;

    for (fldidx = 0; fldidx < pcfs->fields_count; fldidx++)
    {
        if (ACC_STATIC & pcfs->fields[fldidx]->access_flags)
        {
            rc++;
        }
    }

    return(rc);

} /* END of class_get_num_static_fields() */


/*!
 * @brief Load the field lookup table for class static fields in
 * this class.
 *
 *
 * @param    pcfs         Pointer to ClassFile area
 * 
 *
 * @returns   array of field indices in ClassFile @b fields table
 *           for all static fields in this class or
 *           @link #rnull rnull@endlink if no values
 *           to initialize or heap allocation error.  The
 *           size of the array is determined by the result
 *           of class_get_num_static_fields().
 *
 */
jvm_field_index *class_get_static_field_lookups(ClassFile *pcfs)
{
    u2 num_static_fields = class_get_num_static_fields(pcfs);

    /* Done if no static fields to initialize */
    if (0 == num_static_fields)
    {
        return((u2 *) rnull);
    }

    u2 fldidx;
    u2 fluidx = 0;
    jvm_field_index *rc =
        HEAP_GET_DATA(sizeof(jvm_field_index) * num_static_fields,
                      rfalse);

    for (fldidx = 0; fldidx < pcfs->fields_count; fldidx++)
    {
        if (ACC_STATIC & pcfs->fields[fldidx]->access_flags)
        {
            rc[fluidx++] = fldidx;
        }
    }

    return(rc);

} /* END of class_get_static_field_lookups() */


/*!
 * @brief Load the field data table for class static fields in
 * this class
 *
 *
 * @param   clsidx        Class table index of slot having class loaded
 *
 * @param  pcfs           Pointer to ClassFile area
 * 
 *
 * @returns  array of data values corresponding to each field index
 *           as returned from class_get_static_field_lookups() or
 *           @link #rnull rnull@endlink if no values to initialize
 *           or heap allocation error.
 *           The size of the array is determined by the result of
 *           class_get_num_static_fields().
 *
 */
jvalue *class_get_static_field_data(jvm_class_index  clsidx,
                                    ClassFile       *pcfs)
{
    u2 num_static_fields = class_get_num_static_fields(pcfs);

    /* Done if no static fields to initialize */
    if (0 == num_static_fields)
    {
        return((jvalue *) rnull);
    }

    u2 fldidx;
    u2 fluidx = 0;

    /*
     * Allocate heap are for data table, SET ALL VALUES TO ZERO.
     * This will accomplish the intention of clearing all primative
     * data types.
     */
    jvalue *rc =HEAP_GET_DATA(sizeof(jvalue) * num_static_fields,rtrue);

    for (fldidx = 0; fldidx < pcfs->fields_count; fldidx++)
    {
        if (ACC_STATIC & pcfs->fields[fldidx]->access_flags)
        {
            class_get_constant_field_attribute(clsidx,
                                               pcfs,
                                               fldidx,
                                               rc,
                                               fluidx,
                                               rtrue,
                                               jvm_object_hash_null);

            fluidx++;
        }
    }

    return(rc);

} /* END of class_get_static_field_data() */


/*!
 * @brief Report number of object instance fields are in a class file.
 *
 *
 * @param    pcfs       Pointer to ClassFile area
 *
 *
 * @returns  number of fields @e not marked ACC_STATIC.
 *
 */
u2 class_get_num_object_instance_fields(ClassFile *pcfs)
{
    jvm_field_index fldidx;
    u2 rc = 0;

    for (fldidx = 0; fldidx < pcfs->fields_count; fldidx++)
    {
        if (!(ACC_STATIC & pcfs->fields[fldidx]->access_flags))
        {
            rc++;
        }
    }

    return(rc);

} /* END of class_get_num_object_instance_fields() */


/*!
 * @brief Load the field lookup table for object instance fields in
 * this class
 *
 *
 * @param  pcfs           Pointer to ClassFile area
 * 
 *
 * @returns  array of field indices in ClassFile @b fields table
 *           for all object instance fields in this class or
 *           @link #rnull rnull@endlink if no values to initialize
 *           or heap allocation error.  The size of the array is
 *           determined by the result of
 *           class_get_num_object_instance_fields().
 *
 */
jvm_field_index
    *class_get_object_instance_field_lookups(ClassFile *pcfs)
{
    u2 num_instance_fields = class_get_num_object_instance_fields(pcfs);

    /* Done if no object instance fields to initialize */
    if (0 == num_instance_fields)
    {
        return((u2 *) rnull);
    }

    u2 fldidx;
    u2 fluidx = 0;
    jvm_field_index *rc =
        HEAP_GET_DATA(sizeof(jvm_field_index) * num_instance_fields,
                      rfalse);

    for (fldidx = 0; fldidx < pcfs->fields_count; fldidx++)
    {
        if (!(ACC_STATIC & pcfs->fields[fldidx]->access_flags))
        {
            rc[fluidx++] = fldidx;
        }
    }

    return(rc);

} /* END of class_get_object_instance_field_lookups() */


/*!
 * @brief Load the field data table for object instance fields in
 * this class.
 *
 * This initialization is the same for all objects of this class,
 * so this function is still naturally a part of class processing
 * instead of object processing.
 *
 *
 * @param   clsidx        Class table index of slot having class loaded
 *
 * @param   objhash       Object hash of object slot being loaded.
 *                        This is supplied @e expressly to mark
 *                        reference types (that is, CONSTANT_String)
 *                        during field loading from the class file,
 *                        otherwise not needed.
 *
 * @param   pcfs          Pointer to ClassFile area
 * 
 *
 * @returns   array of data values corresponding to each field index
 *           as returned from class_get_static_field_lookups() or
 *           @link #rnull rnull@endlink if no values to initialize
 *           or heap allocation error.  The size of the array is
 *           determined by the result of
 *           class_get_num_object_instance_fields().
 */
jvalue *class_get_object_instance_field_data(jvm_class_index  clsidx,
                                             jvm_object_hash  objhash,
                                             ClassFile       *pcfs)
{
    u2 num_object_instance_fields =
        class_get_num_object_instance_fields(pcfs);

    /* Done if no object instance fields to initialize */
    if (0 == num_object_instance_fields)
    {
        return((jvalue *) rnull);
    }

    u2 fldidx;
    u2 fluidx = 0;

    /*
     * Allocate heap are for data table, SET ALL VALUES TO ZERO.
     * This will accomplish the intention of clearing all primative
     * data types.
     */
    jvalue *rc =
       HEAP_GET_DATA(sizeof(jvalue) * num_object_instance_fields,rtrue);

    for (fldidx = 0; fldidx < pcfs->fields_count; fldidx++)
    {
        if (!(ACC_STATIC & pcfs->fields[fldidx]->access_flags))
        {
            class_get_constant_field_attribute(clsidx,
                                               pcfs,
                                               fldidx,
                                               rc,
                                               fluidx,
                                               rfalse,
                                               objhash);
            fluidx++;
        }
    }

    return(rc);

} /* END of class_get_object_instance_field_data() */


/*!
 * @brief Shut down the class area of the JVM model after JVM execution
 * in two stages.
 *
 *  In between these two stages will be object_shutdown()
 * which performs related reference cleanup from the object side:
 *
 *   Stage 1-- remove class object references and class array
 *             references.
 *
 *   object_shutdown()-- remove @e class references from @e objects
 *                       here.
 *
 *   Stage 2-- all remaining cleanup.  Notice that references are
 *             removed here @e also except during this shutdown process.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *       @returns @link #rvoid rvoid@endlink
 *
 */
rvoid class_shutdown_1()
{
    jvm_class_index clsidx;

    for (clsidx = jvm_class_index_null;
         clsidx < JVMCFG_MAX_CLASSES;
         clsidx++)
    {
        if (CLASS(clsidx).status & CLASS_STATUS_INUSE)
        {
            if (!(CLASS(clsidx).status & CLASS_STATUS_NULL))
            {
                (rvoid) GC_CLASS_RMREF_FROM_CLASS(
                            clsidx,
                            CLASS(clsidx).lower_dim_array);

                (rvoid) GC_OBJECT_RMREF_FROM_CLASS(
                            clsidx,
                            CLASS(clsidx).class_objhash);

                (rvoid) GC_CLASS_RMREF_FROM_CLASS(
                            clsidx,
                            CLASS(clsidx).initiating_ClassLoader);

                (rvoid) GC_CLASS_RMREF_FROM_CLASS(
                            clsidx,
                            CLASS(clsidx).defining_ClassLoader);
            }

            (rvoid) GC_CLASS_RMREF_FROM_CLASS(jvm_class_index_null,
                                              clsidx);
        }
    }

    /* This may result in a @e large garbage collection */
    GC_RUN(rfalse);

    return;

} /* END of class_shutdown_1() */


rvoid class_shutdown_2()
{
    jvm_class_index clsidx;

    for (clsidx = jvm_class_index_null;
         clsidx < JVMCFG_MAX_CLASSES;
         clsidx++)
    {
        if (CLASS(clsidx).status & CLASS_STATUS_INUSE)
        {
            (rvoid) class_static_delete(clsidx, rfalse);
        }
    }

    /* Declare this module uninitialized */
    jvm_class_initialized = rfalse;

    /* This may result in a @e large garbage collection */
    GC_RUN(rfalse);

    return;

} /* END of class_shutdown_2() */


/* EOF */
