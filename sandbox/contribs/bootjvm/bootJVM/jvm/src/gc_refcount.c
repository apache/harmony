/*!
 * @file gc_refcount.c
 *
 * @brief JVM @b refcount garbage collector,
 * performs role of @c @b System.gc() .
 *
 * The logic of these structures and functions is a simple counting
 * scheme that tracks only the @e number of references to an object,
 * the object's GC area holding that number.  It also holds a
 * @link #rtrue rtrue@endlink/@link #rfalse rfalse@endlink for local
 * variables as to whether or not a local is a reference that will
 * need to be counted (down) when that stack frame goes away.
 *
 * This is the second of hopefully a number of garbage collection
 * schemes.  Others should be named @b gc_somename.c  and should
 * be configured into @link ./config.sh ./config.sh@endlink.
 *
 * The common header file @link jvm/src/gc.h gc.h@endlink defines
 * the prototypes for all garbage collection implementations by way
 * of the @link #CONFIG_GC_TYPE_STUB CONFIG_GC_TYPE_xxx@endlink
 * symbol definitions.
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
ARCH_SOURCE_COPYRIGHT_APACHE(gc_refcount, c,
"$URL$",
"$Id$");

#if defined(CONFIG_GC_TYPE_REFCOUNT) || defined(CONFIG_COMPILE_ALL_OPTIONS)


#include "jvmcfg.h"
#include "classfile.h"
#include "exit.h"
#include "heap.h"
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h"

/*!
 * @name Garbage collection accounting areas
 *
 * @brief Type definition for references to garbage collection
 * in several major JVM structure areas, namely classes,
 * objects, and stack frames.
 *
 * Notice the @e exact same definition for:
 *
 *     @link #CLASS_STATUS_REFERENCE CLASS_STATUS_REFERENCE@endlink,
 *     @link #OBJECT_STATUS_REFERENCE OBJECT_STATUS_REFERENCE@endlink,
 *     and
 *     @link #LOCAL_STATUS_REFERENCE LOCAL_STATUS_REFERENCE@endlink
 *
 * These are made to be the @e same across all three categories
 * of GC items.
 * @link #OBJECT_STATUS_REFERENCE OBJECT_STATUS_REFERENCE@endlink
 * is defined to be the same as
 * the value in @link src/object.h object.h@endlink and the the
 * other two are defined to be the same as this one.  The compiler
 * will let you if someone changes something so these two definitions
 * are not identical.  Notice that the status byte is @b rbyte, so the
 * definition must be of byte scope.  The compiler will, of course,
 * let you know if you violate this rule.
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @struct gc_class_refcount
 *
 * @brief Type definition for references to garbage collection in
 * Java classes
 *
 */

typedef struct
{
    /* Count number of references to this class instance */
    jint refcount;

    /* Garbage collect status of each class static variable */
    rbyte *status;

} gc_class_refcount;


/*!
 * @struct gc_object_refcount
 *
 * @brief Type definition for references to garbage collection in
 * Java objects
 *
 */

typedef struct
{
    /* Count number of references to this object instance */
    jint refcount;

    /* Garbage collect status of each class static variable */
    rbyte *status;

} gc_object_refcount;


/*!
 * @struct gc_stack_refcount
 *
 * @brief Type definition for references to garbage collection in
 * Java stack frames
 *
 */

typedef struct
{
    jint  num_local_variables;
    rbyte status[1];   /* Array of size status[num_locals] */

} gc_stack_refcount;

/*@} */ /* End of grouped definitions */


/*!
 * @name GC naming shortcut macros
 *
 * @brief Map the GC accounting area @link #rvoid (rvoid *)@endlink
 * onto a @link #gc_stack_refcount (gc_stack_refcount *)@endlink.
 *
 *
 * @param clsidx   Class index of a class slot to reference
 *
 */
/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Reference a class slot garbage collection accounting area
 *
 */
#define CLASS_GARBAGE(clsidx) \
    ((gc_class_refcount *)   CLASS(clsidx).pgarbage)

/*!
 * @brief Reference an object slot garbage collection accounting area
 *
 *
 * @returns @link #gc_class_refcount (gc_class_refcount *)@endlink
 *          version of @link #rvoid rvoid@endlink GC pointer
 *          @link #robject.pgarbage robject.pgarbage@endlink
 *
 */
#define OBJECT_GARBAGE(objhash) \
    ((gc_object_refcount *) OBJECT(objhash).pgarbage)

/*!
 * @brief Reference a stack frame garbage collection accounting area
 *
 *
 * @returns @link #gc_stack_refcount (gc_stack_refcount *)@endlink
 *          version of @link #rvoid rvoid@endlink GC pointer
 *          @link #GET_GC() GET_GC()@endlink
 *
 */
#define GET_STACK_GARBAGE(thridx) \
    ((gc_stack_refcount *)   GET_GC(thridx))

/*@} */ /* End of grouped definitions */


/*!
 * @brief Initialize garbage collection
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *       @returns @link #rvoid rvoid@endlink
 *
 */
rvoid gc_init_refcount()
{
    /* Nothing to do in this model */

    return;

} /* END of gc_init_refcount() */


/*!
 * @brief Review collection status of all objects and clean them up.
 *
 * Scan through object table for objects that need
 * be deallocated and free up those resources for
 * reuse by the JVM.
 *
 *
 * @param rmref  @link #rtrue rtrue@endlink when class and object
 *               references are to be removed during processing.
 *               This is @link #rfalse rfalse@endlink during
 *               JVM shutdown.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

rvoid gc_run_refcount(rboolean rmref)
{
    jvm_class_index clsidx;

    for (clsidx = jvm_class_index_null;
         clsidx < JVMCFG_MAX_CLASSES;
         clsidx++)
    {
        rushort status = CLASS(clsidx).status;

        /* Skip free object table slots */
        if (!(CLASS_STATUS_INUSE & status))
        {
            continue;
        }

        /* Skip null slots (the JVMCFG_NULL_CLASS and any slot that
           is currently being intialized. */
        if (CLASS_STATUS_NULL & status)
        {
            continue;
        }

        /* Look only at slots marked as ready for garbage collection */
        if (CLASS_STATUS_GCREQ & status)
        {
            class_static_delete(clsidx, rmref);
            continue;
        }

    } /* for clsidx */

    jvm_object_hash objhash;

    for (objhash = jvm_object_hash_null;
         objhash < JVMCFG_MAX_OBJECTS;
         objhash++)
    {
        rushort status = OBJECT(objhash).status;

        /* Skip free object table slots */
        if (!(OBJECT_STATUS_INUSE & status))
        {
            continue;
        }

        /* Skip null slots (the JVMCFG_NULL_OBJECT and any slot that
           is currently being intialized). */
        if (OBJECT_STATUS_NULL & status)
        {
            continue;
        }

        /* Look only at slots marked as ready for garbage collection */
        if (OBJECT_STATUS_GCREQ & status)
        {
            object_instance_finalize(objhash, JVMCFG_GC_THREAD);
            (rvoid) object_instance_delete(objhash, rmref);
            continue;
        }

    } /* for objhash */

    /* Done with this pass */
    return;

} /* END of gc_run_refcount() */


/*!
 * @brief Start up garbage collection for a class.
 *
 * Initialize garbage collection for a new class static
 * instance, as set up in class_static_new().  The reverse of
 * gc_class_delete_refcount().
 *
 *
 * @param  clsidxNEW   Class table index of new class static instance.
 *
 *
 * @returns @link #rtrue rtrue@endlink if garbage
 *          collection was initialized for this class static instance,
 *          otherwise @link #rfalse false@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null class index or if a garbage collection
           accounting area has already been
           allocated for this class@endlink.
 *
 *
 */
rboolean gc_class_new_refcount(jvm_class_index clsidxNEW)
{
    if (jvm_class_index_null == clsidxNEW)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* Make sure a pointer has not already been allocated */
    if (rnull != CLASS_GARBAGE(clsidxNEW))
    {
        exit_throw_exception(EXIT_GC_ALLOC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* Allocate heap , do not bother setting to zeroes */
    CLASS_GARBAGE(clsidxNEW) = HEAP_GET_DATA(sizeof(gc_class_refcount),
                                                    rfalse);

    /*
     * Set up list to mark garbage collection status of static fields.
     * Initialize the list to ZEROES-- no status items set.
     */
    u2 ncsfl = CLASS(clsidxNEW).num_class_static_field_lookups;
    CLASS_GARBAGE(clsidxNEW)->status =
        HEAP_GET_DATA(sizeof(rbyte) * ncsfl, rtrue);

    /* Start out with no references */
    CLASS_GARBAGE(clsidxNEW)->refcount = 0;

    /* Should always be false */
    if ((0 != ncsfl) && (rnull == CLASS_GARBAGE(clsidxNEW)->status))
    {
        return(rfalse);
    }

    return(rtrue);

} /* END of gc_class_new_refcount() */


/*!
 * @brief Shut down and restart garbage collection for a class.
 *
 * Reinitialize garbage collection for a reloaded class static instance,
 * as set up in class_reload().
 *
 *
 * @param  clsidxOLD   Class table index of old class static instance.
 *
 * @param  clsidxNEW   Class table index of reloaded class static
 *                     instance.
 *
 *
 * @returns @link #rtrue rtrue@endlink if garbage collection was
 *          reinitialized for this reloaded class static instance,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target class index@endlink.
 *
 */
rboolean gc_class_reload_refcount(jvm_class_index clsidxOLD,
                                  jvm_class_index clsidxNEW)
{
    if (jvm_class_index_null == clsidxOLD)
    {
        return(rfalse);
    }

    if (jvm_class_index_null == clsidxNEW)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /*
     * This particular GC algorithm has nothing to adjust since
     * class_reload() copies the GC pointer and everything else
     * is still valid in that pointed-to structure.  However,
     * other GC algorithms might need this hook.
     */

    return(rtrue);

} /* END of gc_class_reload_refcount() */


/*!
 * @brief Add a class static reference to a class.
 *
 * Mark class as having another class static instance reference to it,
 * namely a @b lower_dim_array or @b initiating_ClassLoader or
 * @b defining_ClasslLoader The reverse of
 * gc_class_rmref_from_class_refcount().
 *
 *
 * @param  clsidxFROM Class table index of source class static instance.
 *                    If @link #jvm_class_index_null
                      jvm_class_index_null@endlink, this is the class
 *                    table entry itself.
 *
 * @param  clsidxTO   Class table index of target class static instance.
 *
 *
 * @returns @link #rtrue rtrue@endlink if class static instance was
 *          marked, otherwise @link #rfalse rfalse@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target class index@endlink.
 */
rboolean gc_class_mkref_from_class_refcount(jvm_class_index clsidxFROM,
                                            jvm_class_index clsidxTO)
{
    /* @b clsidxFROM is not used in this implementation */

    if (jvm_class_index_null == clsidxTO)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    CLASS_GARBAGE(clsidxTO)->refcount++;

    return(rtrue);

} /* END of gc_class_mkref_from_class_refcount() */


/*
 * Mark class as having another class object instance reference to it,
 * namely where OBJECT_STATUS_CLASS is set for that object.  (Usually
 * marked only one time over the life of the class.)
 * The reverse of gc_class_rmref_from_object_refcount().
 *
 *
 * @param  objhashFROM Object hash of source object instance.
 *
 * @param  clsidxTO    Class table index of target class static instance
 *
 *
 * @returns @link #rtrue rtrue@endlink if class static instance was
 *          marked, otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target class index@endlink.
 *
 */
rboolean
    gc_class_mkref_from_object_refcount(jvm_object_hash objhashFROM,
                                        jvm_class_index  clsidxTO)
{
    /* @b objhashFROM is not used in this implementation */

    return(gc_class_mkref_from_class_refcount(
               jvm_class_index_null /* meaningless */,
               clsidxTO));

} /* END of gc_class_mkref_from_object_refcount() */


/*!
 * @brief Remove a class static reference from a class.
 *
 * Unmark class as having a class static instance reference to it.
 * The reverse of gc_class_mkref_from_class_refcount().
 *
 *
 * @param  clsidxFROM Class table index of source class static instance.
 *                    If @link #jvm_class_index_null
                      jvm_class_index_null@endlink, this is the class
 *                    table entry itself.
 *
 * @param  clsidxTO   Class table index of target class static instance.
 *
 *
 * @returns @link #rtrue rtrue@endlink if class static instance was
 *          unmarked, otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target class index@endlink.
 *
 */
rboolean gc_class_rmref_from_class_refcount(jvm_class_index clsidxFROM,
                                            jvm_class_index clsidxTO)
{
    /* @b clsidxFROM is not used in this implementation */

    if (jvm_class_index_null == clsidxTO)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }


    /* Limit decrement to lower bound of zero */
    if (0 < CLASS_GARBAGE(clsidxTO)->refcount)
    {
        CLASS_GARBAGE(clsidxTO)->refcount--;
    }

    /* Flag class ready for GC */
    if (0 == CLASS_GARBAGE(clsidxTO)->refcount)
    {
        CLASS(clsidxTO).status |= CLASS_STATUS_GCREQ;
    }


    return(rtrue);

} /* END of gc_class_rmref_from_class_refcount() */


/*
 * Unmark class as having a class object reference to it, namely
 * where OBJECT_STATUS_CLASS is set for that object.  (Usually marked
 * only one time over the life of the class, so this will effectively
 * mark the class itself as being ready for garbage collection.)
 * The reverse of gc_class_mkref_refcount().
 *
 *
 * @param  objhashFROM Object hash of source object instance.
 *
 * @param  clsidxTO    Class table index of target class static instance
 *
 *
 * @returns @link #rtrue rtrue@endlink if class static instance was
 *          unmarked, otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target class index@endlink.
 *
 */
rboolean
    gc_class_rmref_from_object_refcount(jvm_object_hash objhashFROM,
                                        jvm_class_index clsidxTO)
{
    /* @b objhashFROM is not used in this implementation */

    return(gc_class_rmref_from_class_refcount(
               jvm_class_index_null /* meaningless*/,
               clsidxTO));

} /* END of gc_class_rmref_from_object_refcount() */


/*!
 * @brief Add a class static field reference to a class.
 *
 * Mark class static field as being a reference type (typically
 * after loading the class).  The reverse of
 * gc_class_field_rmref_refcount().
 *
 *
 * @param  clsidxTO   Class table index of target class static instance.
 *
 * @param  csflidxTO  Class static field lookup index to field in
 *                    target class static instance.
 *
 *
 * @returns @link #rtrue rtrue@endlink if class static field was marked,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target class index@endlink.
 *
 */
rboolean gc_class_field_mkref_refcount(jvm_class_index        clsidxTO,
                                       jvm_field_lookup_index csflidxTO)
{
    if (jvm_class_index_null == clsidxTO)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* Perform range check */
    if (CLASS(clsidxTO).num_class_static_field_lookups >= csflidxTO)
    {
        return(rfalse);
    }

    jvm_object_hash objhash =
        CLASS(clsidxTO).class_static_field_data[csflidxTO]._jobjhash;

    if (jvm_object_hash_null == objhash)
    {
        return(rfalse);
    }

    /* This object instance field is a reference type */
    CLASS_GARBAGE(clsidxTO)->status[csflidxTO] |= CLASS_STATUS_REFERENCE;

    OBJECT_GARBAGE(objhash)->refcount++;

    return(rtrue);

} /* END of gc_class_field_mkref_refcount() */


/*!
 * @brief Remove a class static field reference from a class.
 *
 * Mark class static field as @e not being a reference type (typically
 * before unloading the class.  The reverse of
 * gc_class_field_mkref_refcount().
 *
 *
 * @param  clsidxTO   Class table index of target class static instance.
 *
 * @param  csflidxTO  Class static field lookup index to field in
 *                    target class.
 *
 *
 * @returns @link #rtrue rtrue@endlink if class static field was marked,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null class index@endlink.
 *
 */
rboolean gc_class_field_rmref_refcount(jvm_class_index        clsidxTO,
                                       jvm_field_lookup_index csflidxTO)
{
    if (jvm_class_index_null == clsidxTO)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* Perform range check */
    if (CLASS(clsidxTO).num_class_static_field_lookups >= csflidxTO)
    {
        return(rfalse);
    }

    jvm_object_hash objhash =
        CLASS(clsidxTO).class_static_field_data[csflidxTO]._jobjhash;

    if (jvm_object_hash_null == objhash)
    {
        return(rfalse);
    }

    /* Honor removal request only if it was marked in the first place */
    if (CLASS_GARBAGE(clsidxTO)->status[csflidxTO] &
        CLASS_STATUS_REFERENCE)
    {
        /* Limit decrement to lower bound of zero */
        if (0 < OBJECT_GARBAGE(objhash)->refcount)
        {
            OBJECT_GARBAGE(objhash)->refcount--;
        }

        /* Flag class static field ready for GC */
        if (0 == OBJECT_GARBAGE(objhash)->refcount)
        {
            CLASS(clsidxTO).status |= CLASS_STATUS_GCREQ;
        }

        return(rtrue);
    }

    return(rfalse);

} /* END of gc_class_field_rmref_refcount() */


/*!
 * @brief Garbage collect a class static instance.
 *
 * Finalize garbage collection for a class static instance that will
 * no longer be used, as set up in class_static_delete().  If there
 * are any outstanding references to this class, those must first
 * be removed, at which time gc_run_refcount() will perform the
 * finalization instead.  The reverse of gc_class_new_refcount().
 *
 * @note Since this function is the reverse of gc_class_new_refcount(),
 *       the @link #rclass.pgarbage rclass.pgarbage@endlink pointer must
 *       be freed by @link #HEAP_DATA_FREE() HEAP_DATA_FREE@endlink.
 *
 * @param  clsidxOLD     Class table index of defunct class static
 *                       instance.
 *
 * @param  delete_class  If @link #rtrue rtrue@endlink, attempt
 *                       class_static_delete() when finished with
 *                       garbage collection.
 *
 *
 * @returns @link #rtrue rtrue@endlink if garbage collection was
 *          finalized for this class static instance, otherwise
 *          @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null source source class index@endlink.
 *
 */
rboolean gc_class_delete_refcount(jvm_class_index clsidxOLD,
                                  rboolean        delete_class)
{
    if (jvm_class_index_null == clsidxOLD)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    u2 csflidx;
    for (csflidx = 0;
         csflidx < CLASS(clsidxOLD).num_class_static_field_lookups;
         csflidx++)
    {
        /* If more than zero class static fields, @b status != NULL */
        if (CLASS_GARBAGE(clsidxOLD)->status[csflidx] &
            CLASS_STATUS_REFERENCE)
        {
            (rvoid) gc_class_field_rmref_refcount(clsidxOLD, csflidx);

            /*
             * Don't have to know any other data, or whether it might
             * be an array, for only arrays and references will mark
             * this field.  Furthermore, since arrays @e are references,
             * both the @link jvalue#_jarray _jarray@endlink and
             * @link jvalue#_jobjhash _jobjhash@endlink mean the
             * same thing semantically.
             */
            CLASS(clsidxOLD)
              .class_static_field_data[csflidx]
              ._jobjhash =
                jvm_object_hash_null;
            CLASS_GARBAGE(clsidxOLD)->status[csflidx] &=
                ~CLASS_STATUS_REFERENCE;
        }
    }

    /* Should never happen */
    if (rfalse ==
        gc_class_rmref_from_class_refcount(jvm_class_index_null,
                                           clsidxOLD))
    {
        return(rfalse);
    }

    /* If gc_class_rmref_refcount() marked class for deletion,go do it*/
    if (CLASS(clsidxOLD).status & CLASS_STATUS_GCREQ)
    {
        /* Also get rid of the class GC accounting area */
        HEAP_FREE_DATA(CLASS_GARBAGE(clsidxOLD));
        CLASS_GARBAGE(clsidxOLD) = rnull;

        /* Attempt to actually delete the class itself, if requested */
        if (rtrue == delete_class)
        {
            return(class_static_delete(clsidxOLD, rfalse));
        }
    }

    /* There are still outstanding references to this class */
    return(rfalse);

} /* END of gc_class_delete_refcount() */


/*!
 * @brief Start up garbage collection for an object.
 *
 * Initialize garbage collection for a new object instance, as
 * set up in object_new().  The reverse of gc_object_delete_refcount().
 *
 *
 * @param  objhashNEW  Object table hash of new object instance.
 *
 *
 * @returns @link #rtrue rtrue@endlink if garbage collection was
 *          initialized for this object instance, otherwise
 *          @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null object hash@endlink.
 *
 */
rboolean gc_object_new_refcount(jvm_object_hash objhashNEW)
{
    if (jvm_object_hash_null == objhashNEW)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* Make sure a pointer has not already been allocated */
    if (rnull != OBJECT_GARBAGE(objhashNEW))
    {
        exit_throw_exception(EXIT_GC_ALLOC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* Allocate heap , do not bother setting to zeroes */
    OBJECT_GARBAGE(objhashNEW) =
        HEAP_GET_DATA(sizeof(gc_object_refcount), rfalse);


    /* Find class table entry for this object */
    jvm_class_index clsidx = OBJECT_OBJECT_LINKAGE(objhashNEW)->clsidx;

    /*
     * Set up list to mark garbage collection status of static fields.
     * Initialize the list to ZEROES-- no status items set.
     */
    u2 noifl = CLASS(clsidx).num_object_instance_field_lookups;
    OBJECT_GARBAGE(objhashNEW)->status =
        HEAP_GET_DATA(sizeof(rbyte) * noifl, rtrue);

    /* Start out with no references */
    OBJECT_GARBAGE(objhashNEW)->refcount = 0;

    /* Should always be false */
    if ((0 != noifl) && (rnull == OBJECT_GARBAGE(objhashNEW)->status))
    {
        return(rfalse);
    }

    return(rtrue);

} /* END of gc_object_new_refcount() */


/*!
 * @brief Add a class static reference to an object.
 *
 * Mark object as having another class static instance reference to it.
 * The reverse of gc_object_rmref_from_class_refcount().
 *
 *
 * @param  clsidxFROM Class table index of source class static instance.
 *
 * @param  objhashTO  Object table hash of target object instance
 *
 *
 * @returns @link #rtrue rtrue@endlink if object instance was marked,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target object hash@endlink.
 *
 */
rboolean gc_object_mkref_from_class_refcount(jvm_class_index clsidxFROM,
                                             jvm_object_hash objhashTO)
{
    /* @b clsidxFROM is not used in this implementation */

    if (jvm_object_hash_null == objhashTO)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    OBJECT_GARBAGE(objhashTO)->refcount++;

    return(rtrue);

} /* END of gc_object_mkref_from_class_refcount() */


/*!
 * @brief Add an object instance reference to an object.
 *
 * Mark object as having another object instance reference to it.
 * The reverse of gc_object_rmref_from_object_refcount().
 *
 *
 * @param  objhashFROM  Object table hash of source object instance.
 *                      If @link #jvm_object_hash_null
                        jvm_object_hash_null@endlink, this is the object
 *                      table entry itself.
 *
 * @param  objhashTO    Object table hash of target object instance
 *
 *
 * @returns @link #rtrue rtrue@endlink if object instance was marked,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target object hash@endlink.
 *
 */
rboolean
    gc_object_mkref_from_object_refcount(jvm_object_hash objhashFROM,
                                        jvm_object_hash objhashTO)
{
    /* @b objhashFROM is not used in this implementation */

    return(gc_object_mkref_from_class_refcount(jvm_class_index_null,
                                               objhashTO));

} /* END of gc_object_mkref_from_object_refcount() */


/*!
 * @brief Remove a class static reference from an object.
 *
 * Unmark object as having a class static instance reference to it.
 * The reverse of gc_object_mkref_from_class_refcount().
 *
 *
 * @param  clsidxFROM Class table index of source class static instance.
 *
 * @param  objhashTO  Object table hash of target object instance
 *
 *
 * @returns @link #rtrue rtrue@endlink if class static instance was
 *          unmarked, otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target object hash@endlink.
 *
 */
rboolean gc_object_rmref_from_class_refcount(jvm_class_index clsidxFROM,
                                    jvm_object_hash objhashTO)
{
    /* @b clsidxFROM is not used in this implementation */

    if (jvm_object_hash_null == objhashTO)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* Limit decrement to lower bound of zero */
    if (0 < OBJECT_GARBAGE(objhashTO)->refcount)
    {
        OBJECT_GARBAGE(objhashTO)->refcount--;
    }

    /* Flag object instance ready for GC */
    if (0 == OBJECT_GARBAGE(objhashTO)->refcount)
    {
        OBJECT(objhashTO).status |= OBJECT_STATUS_GCREQ;
    }

    return(rtrue);

} /* END of gc_object_rmref_from_class_refcount() */


/*
 * Unmark object as having an object instance reference to it.
 * The reverse of gc_object_mkref_from_object_refcount().
 *
 *
 * @param  objhashFROM  Object table hash of source object instance.
 *                      If @link #jvm_object_hash_null
                        jvm_object_hash_null@endlink, this is the object
 *                      table entry itself.
 *
 * @param  objhashTO    Object table hash of target object instance
 *
 *
 * @returns @link #rtrue rtrue@endlink if object instance was
 *          unmarked, otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target object hash@endlink.
 *
 */
rboolean
    gc_object_rmref_from_object_refcount(jvm_object_hash objhashFROM,
                                         jvm_object_hash objhashTO)
{
    /* @b objhashFROM is not used in this implementation */

    return(gc_object_rmref_from_class_refcount(jvm_class_index_null,
                                               objhashTO));

} /* END of gc_object_rmref_from_object_refcount() */


/*!
 * @brief Add an object instance field reference to an object.
 *
 * Mark object instance field as being a reference type (typically
 * after loading the class and instantiating an object of that
 * class type).  The reverse of gc_object_field_rmref_refcount().
 *
 *
 * @param  objhashTO   Object table hash of target object instance.
 *
 * @param  oiflidxTO   Object instance field lookup index to field
 *                     in target object instance.
 *
 *
 * @returns @link #rtrue rtrue@endlink if object instance field was
 *          marked, otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target object hash@endlink.
 *
 */
rboolean gc_object_field_mkref_refcount(jvm_object_hash objhashTO,
                                       jvm_field_lookup_index oiflidxTO)
{
    if (jvm_object_hash_null == objhashTO)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    jvm_class_index clsidx = OBJECT_OBJECT_LINKAGE(objhashTO)->clsidx;
    u2 noifl = CLASS(clsidx).num_object_instance_field_lookups;

    /* Perform range check */
    if (noifl >= oiflidxTO)
    {
        return(rfalse);
    }

    jvm_object_hash objhashFLD =
        OBJECT(objhashTO)
          .object_instance_field_data[oiflidxTO]
          ._jobjhash;

    if (jvm_object_hash_null == objhashFLD)
    {
        return(rfalse);
    }

    /* This object instance field is a reference type */
    OBJECT_GARBAGE(objhashTO)->status[oiflidxTO] |=
        OBJECT_STATUS_REFERENCE;

    OBJECT_GARBAGE(objhashFLD)->refcount++;

    return(rtrue);

} /* END of gc_object_field_mkref_refcount() */


/*!
 * @brief Add an object instance field reference to an object.
 *
 * Mark object instance field as @e not being a reference type any more
 * (typically before unloading the class).  The reverse of
 * gc_object_field_rmref_refcount().
 *
 *
 * @param  objhashTO    Object table hash of target object instance.
 *
 * @param  oiflidxTO    Object instance field lookup index to field
 *                      in target object instance.
 *
 *
 * @returns @link #rtrue rtrue@endlink if class static field was
 *          unmarked, otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target object hash@endlink.
 *
 */
rboolean gc_object_field_rmref_refcount(jvm_object_hash objhashTO,
                                       jvm_field_lookup_index oiflidxTO)
{
    if (jvm_object_hash_null == objhashTO)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    jvm_class_index clsidx = OBJECT_OBJECT_LINKAGE(objhashTO)->clsidx;
    u2 noifl = CLASS(clsidx).num_object_instance_field_lookups;

    /* Perform range check */
    if (noifl >= oiflidxTO)
    {
        return(rfalse);
    }

    jvm_object_hash objhashFLD =
        CLASS(clsidx).class_static_field_data[oiflidxTO]._jobjhash;

    if (jvm_object_hash_null == objhashFLD)
    {
        return(rfalse);
    }

    /* Honor removal request only if it was marked in the first place */
    if (OBJECT_GARBAGE(objhashTO)->status[oiflidxTO] &
        OBJECT_STATUS_REFERENCE)
    {
        /* Limit decrement to lower bound of zero */
        if (0 < OBJECT_GARBAGE(objhashFLD)->refcount)
        {
            OBJECT_GARBAGE(objhashFLD)->refcount--;
        }

        /* Flag object instance field ready for GC */
        if (0 == OBJECT_GARBAGE(objhashFLD)->refcount)
        {
            OBJECT_GARBAGE(objhashFLD)->status[oiflidxTO] |= 
                                                    OBJECT_STATUS_GCREQ;
        }
        return(rtrue);
    }

    return(rfalse);

} /* END of gc_object_field_rmref_refcount() */


/*!
 * @brief Garbage collect an object instance.
 *
 * Finalize garbage collection for an object instance that will no
 * longer be used, as set up in object_instance_delete().  If there
 * are any outstanding references to this class, those must first
 * be removed, at which time gc_run_refcount() will perform the
 * finalization instead.  The reverse of gc_object_new_refcount().
 *
 * @note Since this function is the reverse of gc_object_new_refcount(),
 * the @link #rclass.pgarbage rclass.pgarbage@endlink pointer must be
 * freed by @link #HEAP_DATA_FREE() HEAP_DATA_FREE@endlink.
 *
 *
 * @param  objhashOLD  Object table hash of defunct object instance.
 *
 *
 * @returns @link #rtrue rtrue@endlink if garbage collection was
 *          finalized for this object instance, otherwise
 *          @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null object hash@endlink.
 *
 */
rboolean gc_object_delete_refcount(jvm_object_hash objhashOLD)
{
    if (jvm_object_hash_null == objhashOLD)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    jvm_class_index clsidx = OBJECT_OBJECT_LINKAGE(objhashOLD)->clsidx;

    u2 oiflidx;
    for (oiflidx = 0;
         oiflidx < CLASS(clsidx).num_object_instance_field_lookups;
         oiflidx++)
    {
        /*If more than zero object instance fields,@b status != NULL */
        if (OBJECT_GARBAGE(objhashOLD)->status[oiflidx] &
            OBJECT_STATUS_REFERENCE)
        {
            (rvoid) gc_object_field_rmref_refcount(objhashOLD, oiflidx);

            /*
             * Don't have to know any other data, or whether it might
             * be an array, for only arrays and references will mark
             * this field.  Furthermore, since arrays @e are references,
             * both the @link jvalue#_jarray _jarray@endlink and
             * @link jvalue#_jobjhash _jobjhash@endlink mean the
             * same thing semantically.
             */
            OBJECT(objhashOLD)
                .object_instance_field_data[oiflidx]
                    ._jobjhash = jvm_object_hash_null;

            OBJECT_GARBAGE(objhashOLD)->status[oiflidx] &=
                ~OBJECT_STATUS_REFERENCE;
        }
    }

    /* Should never happen */
    if (rfalse == gc_object_rmref_from_object_refcount(
                      jvm_object_hash_null,
                      objhashOLD))
    {
        return(rfalse);
    }

    /*
     * If gc_object_rmref_refcount() marked object for deletion,
     * go do it
     */
    if (OBJECT(objhashOLD).status & OBJECT_STATUS_GCREQ)
    {
        /* Also get rid of the class GC accounting area */
        HEAP_FREE_DATA(CLASS_GARBAGE(objhashOLD));
        CLASS_GARBAGE(objhashOLD) = rnull;

        /* Finalize object before removing it */
        object_instance_finalize(objhashOLD, JVMCFG_GC_THREAD);
        return(object_instance_delete(objhashOLD, rfalse));
    }

    /* There are still outstanding references to this object */
    return(rfalse);

} /* END of gc_object_delete_refcount() */


/*!
 * @brief Start up garbage collection for a new Java virtual
 * method stack frame.
 *
 * Initialize garbage collection for a new stack frame for a
 * virtual Java method invocation, as set up in PUSH_GC().
 * The reverse of gc_stack_delete_refcount().
 *
 * Initializing to zeroes during heap allocation means that
 * all status bits will be turned OFF, so no local variables
 * are marked with any status until later.
 *
 *
 * @param  thridxNEW    Thread table index to thread that is setting
 *                      up a new stack frame for a method invocation.
 *
 * @param  num_locals   Number of local variables in this method.
 *
 *
 * @returns Pointer to (gc_stack) for this object instance
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null thread index@endlink.
 *
 *
 * @attention Due to the fact that there may be any number of garbage
 *            collection algorithms implemented for the JVM, and with
 *            the need to keep the API to the GC system constant, this
 *            return value is @b not defined to be related to
 *            any particular type of GC.  Instead it is a simple
 *            @link #rvoid rvoid@endlink pointer.
 *
 *
 */
rvoid *gc_stack_new_refcount(jvm_thread_index thridxNEW,rint num_locals)
{
    if (jvm_thread_index_null == thridxNEW)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* Allocate heap AND INITIALIZED TO ZEROES */
    gc_stack_refcount *rc =
        HEAP_GET_DATA(sizeof(gc_stack_refcount) -
                                 sizeof(rbyte) +
                                 (sizeof(rbyte) * num_locals),
                             rtrue);

    rc->num_local_variables = num_locals;

    return((rvoid *) rc);

} /* END of gc_stack_new_refcount() */


/*!
 * @brief Add local variable reference to an object.
 * 
 * Mark object as having another local variable instance reference
 * to it.  The reverse of gc_stack_rmref_refcount().
 *
 *
 * @param  thridxFROM Thread table index to thread that is setting
 *                    up new stack from for a method invocation.
 *
 * @param  frmidxTO   Index into current frame of local variable
 *                    that is an object reference.
 *
 *
 * @returns @link #rtrue rtrue@endlink if object instance was marked,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null thread index@endlink.
 *
 */
rboolean gc_stack_mkref_from_jvm_refcount(jvm_thread_index thridxFROM,
                                          jint             frmidxTO)
{
    if (jvm_thread_index_null == thridxFROM)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    gc_stack_refcount *pgcs = GET_GC(thridxFROM);

    /* Range check frame index */
    if (pgcs->num_local_variables <= frmidxTO)
    {
        return(rfalse);
    }

    /* Mark local var as a reference */
    pgcs->status[frmidxTO] |= LOCAL_STATUS_REFERENCE;

    return(gc_object_mkref_from_object_refcount(
               jvm_object_hash_null,
               (jvm_object_hash) GET_LOCAL_VAR(thridxFROM, frmidxTO)));

} /* END of gc_stack_mkref_from_jvm_refcount() */


/*!
 * @brief Remove local variable reference from an object.
 *
 * Unmark object from having another local variable instance
 * references to it.  The reverse of gc_stack_mkref_refcount().
 *
 *
 * @param  thridxFROM Thread table index to thread that is setting
 *                    up new stack from for a method invocation.
 *
 * @param  frmidxTO   Index into current frame of local variable
 *                    that is an object reference.
 *
 *
 * @returns @link #rtrue rtrue@endlink if object instance was marked,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null thread index@endlink.
 *
 */
rboolean gc_stack_rmref_from_jvm_refcount(jvm_thread_index thridxFROM,
                                          jint             frmidxTO)
{
    if (jvm_thread_index_null == thridxFROM)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    gc_stack_refcount *pgcs = GET_GC(thridxFROM);

    /* Range check frame index */
    if (pgcs->num_local_variables <= frmidxTO)
    {
        return(rfalse);
    }

    /* Honor removal request only if it was marked in the first place */
    if (pgcs->status[frmidxTO] & LOCAL_STATUS_REFERENCE)
    {
        return(gc_object_rmref_from_object_refcount(
                   jvm_object_hash_null,
                   (jvm_object_hash) GET_LOCAL_VAR(thridxFROM,
                                                   frmidxTO)));
    }

    /* Complain if not marked in the first place */
    return(rfalse);

} /* END of gc_stack_rmref_from_jvm_refcount() */


/*!
 * @brief Garbage collect a Java virtual method stack frame.
 *
 * Finalize garbage collection for a stack frame that will no
 * longer be used by a virtual Java method, as set up in PUSH_GC().
 * This function is called from POP_GC().  It is the reverse
 * of gc_stack_new_refcount().
 *
 *
 * @param  thridxOLD  Thread table index to thread that is tearing
 *                    down an old stack frame from a method invocation.
 *
 * @param  ppgcs      Pointer to GC stack area pointer for this frame
 *
 * @param  plocal_teardown  Pointer to local area of partially popped
 *                          frame.
 *
 *
 * @returns @link #rtrue rtrue@endlink if garbage collection was
 *          finalized for this method return, otherwise
 *          @link #rfalse rfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null thread index@endlink.
 *
 *
 * @attention Due to the fact that there may be any number of garbage
 *            collection algorithms implemented for the JVM, and with
 *            the need to keep the API to the GC system constant, the
 *            parameter @b ppgcs is @b not defined to be related to
 *            any particular type of GC.  Instead it is a simple
 *            @link #rvoid rvoid@endlink pointer.
 *
 */
rboolean gc_stack_delete_refcount(jvm_thread_index   thridxOLD,
                                  rvoid            **ppgcs,
                                  jint              *plocal_teardown)
{
    if (jvm_thread_index_null == thridxOLD)
    {
        exit_throw_exception(EXIT_JVM_GC,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    gc_stack_refcount *pgcs = *((gc_stack_refcount **) ppgcs);

    jint frmidx;
    for (frmidx = 0; frmidx < pgcs->num_local_variables; frmidx++)
    {
        if (pgcs->status[frmidx] & LOCAL_STATUS_REFERENCE)
        {
            /* WARNING:  Working with a NEGATIVE INDEX here!!! */
            (rvoid) gc_object_rmref_from_object_refcount(
                        jvm_object_hash_null,
                        (jvm_object_hash) plocal_teardown[0 - frmidx]);

            /*
             * Since stack is now being popped, and furthermore
             * since this @link gc_stack_refcount#status status@endlink array
             * will soon have HEAP_FREE_DATA() run against it, these
             * are not needed:
             */

            /* PUT_LOCAL_VAR(thridx, frmidx, jvm_object_hash_null); */
            /* pgcs->status[frmidx] &= ~LOCAL_STATUS_REFERENCE;     */
        }
    }

    HEAP_FREE_DATA(pgcs);

    return(rtrue);

} /* END of gc_stack_delete_refcount() */

#endif /* CONFIG_GC_TYPE_REFCOUNT || CONFIG_OPTIONS_COMPILE_ALL */


/* EOF */
