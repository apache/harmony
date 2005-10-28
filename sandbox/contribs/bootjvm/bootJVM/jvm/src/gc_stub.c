/*!
 * @file gc_stub.c
 *
 * @brief JVM @b stub garbage collector,
 * performs role of @c @b System.gc() .
 *
 * The logic of these structures and functions is empty, pending
 * a memory allocation and garbage collection design for the project.
 *
 * This is the first of hopefully a number of garbage collection
 * schemes.  Others should be named @b gc_somename.c and should
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
ARCH_SOURCE_COPYRIGHT_APACHE(gc_stub, c,
"$URL$",
"$Id$");

#if defined(CONFIG_GC_TYPE_STUB) || defined(CONFIG_COMPILE_ALL_OPTIONS)


#include "jvmcfg.h"
#include "classfile.h"
#include "jvm.h"

/*!
 * @name Garbage collection accounting areas
 *
 * @brief Type definition for references to garbage collection
 * in several major JVM structure areas, namely classes,
 * objects, and stack frames.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @struct gc_class_stub
 *
 * @brief Type definition for references to garbage collection
 * in Java classes.
 */

typedef struct
{
    rint dummy; /*!< place holder for implementation details. */

} gc_class_stub;


/*!
 * @struct gc_object_stub
 *
 * @brief Type definition for references to garbage collection
 * in Java objects.
 */

typedef struct
{
    rint dummy; /*!< place holder for implementation details. */

} gc_object_stub;


/*!
 * @struct gc_stack_stub
 *
 * @brief Type definition for references to garbage collection
 * in Java stacks.
 */

typedef struct
{
    rint dummy; /*!< place holder for implementation details. */

} gc_stack_stub;

/*@} */ /* End of grouped definitions */


/*!
 * @brief Initialize garbage collection
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid gc_init_stub()
{
    ARCH_FUNCTION_NAME(gc_init_stub);

    /* Nothing to do in this model */

    return;

} /* END of gc_init_stub() */


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

rvoid gc_run_stub(rboolean rmref)
{
    ARCH_FUNCTION_NAME(gc_run_stub);

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
            /*!
             * @todo  HARMONY-6-jvm-gc_stub.c-1 Write the class
             *        GC algorithm
             */
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
            /*!
             * @todo  HARMONY-6-jvm-gc_stub.c-2 Write the object
             * GC algorithm
             */
            continue;
        }

    } /* for objhash */

    /* Done with this pass */
    return;

} /* END of gc_run_stub() */


/*!
 * @brief Start up garbage collection for a class.
 *
 * Initialize garbage collection for a new class static
 * instance, as set up in class_static_new().  The reverse of
 * gc_class_delete_stub().
 *
 *
 * @param  clsidxNEW   Class table index of new class static instance.
 *
 *
 * @returns @link #rtrue rtrue@endlink if garbage
 *          collection was initialized for this class static instance,
 *          otherwise @link #rfalse rfalse@endlink.
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
rboolean gc_class_new_stub(jvm_class_index clsidxNEW)
{
    ARCH_FUNCTION_NAME(gc_class_new_stub);

    return(rfalse);

} /* END of gc_class_new_stub() */


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
rboolean gc_class_reload_stub(jvm_class_index clsidxOLD,
                              jvm_class_index clsidxNEW)
{
    ARCH_FUNCTION_NAME(gc_class_reload_stub);

    return(rfalse);

} /* END of gc_class_reload_stub() */


/*!
 * @brief Add a class static reference to a class.
 *
 * Mark class as having another class static instance reference to it,
 * namely a @b lower_dim_array or @b initiating_ClassLoader or
 * @b defining_ClasslLoader The reverse of
 * gc_class_rmref_from_class_stub().
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
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR 
           @link #JVMCLASS_JAVA_LANG_INTERNALERROR
           if null target class index@endlink.
 *
 */
rboolean gc_class_mkref_from_class_stub(jvm_class_index clsidxFROM,
                                        jvm_class_index clsidxTO)
{
    ARCH_FUNCTION_NAME(gc_class_mkref_from_class_stub);

    return(rfalse);

} /* END of gc_class_mkref_from_class_stub() */


/*!
 * @brief Add an object instance reference to a class.
 *
 * Mark class as having another class object instance reference to it,
 * namely where OBJECT_STATUS_CLASS is set for that object.  (Usually
 * marked only one time over the life of the class.)
 * The reverse of gc_class_rmref_from_object_stub().
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
rboolean gc_class_mkref_from_object_stub(jvm_object_hash objhashFROM,
                                         jvm_class_index  clsidxTO)
{
    ARCH_FUNCTION_NAME(gc_class_mkref_from_object_stub);

    return(rfalse);

} /* END of gc_class_mkref_from_object_stub() */


/*!
 * @brief Remove a class static reference from a class.
 *
 * Unmark class as having a class static instance reference to it.
 * The reverse of gc_class_mkref_from_class_stub().
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
rboolean gc_class_rmref_from_class_stub(jvm_class_index clsidxFROM,
                                        jvm_class_index clsidxTO)
{
    ARCH_FUNCTION_NAME(gc_class_rmref_from_class_stub);

    return(rfalse);

} /* END of gc_class_rmref_from_class_stub() */


/*!
 * @brief Remove an object instance reference from a class.
 *
 * Unmark class as having a class object reference to it, namely
 * where OBJECT_STATUS_CLASS is set for that object.  (Usually marked
 * only one time over the life of the class, so this will effectively
 * mark the class itself as being ready for garbage collection.)
 * The reverse of gc_class_mkref_stub().
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
rboolean gc_class_rmref_from_object_stub(jvm_object_hash objhashFROM,
                                         jvm_class_index clsidxTO)
{
    ARCH_FUNCTION_NAME(gc_class_rmref_from_object_stub);

    return(rfalse);

} /* END of gc_class_rmref_from_object_stub() */


/*!
 * @brief Add a class static field reference to a class.
 *
 * Mark class static field as being a reference type (typically
 * after loading the class).  The reverse of
 * gc_class_field_rmref_stub().
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
rboolean gc_class_field_mkref_stub(jvm_class_index        clsidxTO,
                                   jvm_field_lookup_index csflidxTO)
{
    ARCH_FUNCTION_NAME(gc_class_firld_mkref_stub);

    return(rfalse);

} /* END of gc_class_field_mkref_stub() */


/*!
 * @brief Remove a class static field reference from a class.
 *
 * Mark class static field as @e not being a reference type (typically
 * before unloading the class.  The reverse of
 * gc_class_field_mkref_stub().
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
rboolean gc_class_field_rmref_stub(jvm_class_index clsidxTO,
                                   jvm_field_lookup_index csflidxTO)
{
    ARCH_FUNCTION_NAME(gc_class_field_rmref_stub);

    return(rfalse);

} /* END of gc_class_field_rmref_stub() */


/*!
 * @brief Garbage collect a class static instance.
 *
 * Finalize garbage collection for a class static instance that will
 * no longer be used, as set up in class_static_delete().  If there
 * are any outstanding references to this class, those must first
 * be removed, at which time gc_run_stub() will perform the
 * finalization instead.  The reverse of gc_class_new_stub().
 *
 * @note Since this function is the reverse of gc_class_new_stub(),
 *       the @link #rclass.pgarbage rclass.pgarbage@endlink pointer must
 *       be freed by @link #HEAP_DATA_FREE() HEAP_DATA_FREE@endlink.
 *
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
rboolean gc_class_delete_stub(jvm_class_index clsidxOLD,
                              rboolean        delete_class)
{
    ARCH_FUNCTION_NAME(gc_class_delete_stub);

    return(rfalse);

} /* END of gc_class_delete_stub() */


/*!
 * @brief Start up garbage collection for an object.
 *
 * Initialize garbage collection for a new object instance, as
 * set up in object_new().  The reverse of gc_object_delete_stub().
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
rboolean gc_object_new_stub(jvm_object_hash objhashNEW)
{
    ARCH_FUNCTION_NAME(gc_object_new_stub);

    return(rfalse);

} /* END of gc_object_new_stub() */


/*!
 * @brief Add a class static reference to an object.
 *
 * Mark object as having another class static instance reference to it.
 * The reverse of gc_object_rmref_from_class_stub().
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
rboolean gc_object_mkref_from_class_stub(jvm_class_index clsidxFROM,
                                         jvm_object_hash objhashTO)
{
    ARCH_FUNCTION_NAME(gc_object_mkref_from_class_stub);

    return(rfalse);

} /* END of gc_object_mkref_from_class_stub() */


/*!
 * @brief Add an object instance reference to an object.
 *
 * Mark object as having another object instance reference to it.
 * The reverse of gc_object_rmref_from_object_stub().
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
rboolean gc_object_mkref_from_object_stub(jvm_object_hash objhashFROM,
                                          jvm_object_hash objhashTO)
{
    ARCH_FUNCTION_NAME(gc_object_mkref_from_object_stub);

    return(rfalse);

} /* END of gc_object_mkref_from_object_stub() */


/*!
 * @brief Remove a class static reference from an object.
 *
 * Unmark object as having a class static instance reference to it.
 * The reverse of gc_object_mkref_from_class_stub().
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
rboolean gc_object_rmref_from_class_stub(jvm_class_index clsidxFROM,
                                         jvm_object_hash objhashTO)
{
    ARCH_FUNCTION_NAME(gc_object_rmref_from_class_stub);

    return(rfalse);

} /* END of gc_object_rmref_from_class_stub() */


/*!
 * @brief Remove an object instance reference from an object.
 *
 * Unmark object as having an object instance reference to it.
 * The reverse of gc_object_mkref_from_object_stub().
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
rboolean gc_object_rmref_from_object_stub(jvm_object_hash objhashFROM,
                                          jvm_object_hash objhashTO)
{
    ARCH_FUNCTION_NAME(gc_object_rmref_from_object_stub);

    return(rfalse);

} /* END of gc_object_rmref_from_object_stub() */


/*!
 * @brief Add an object instance field reference to an object.
 *
 * Mark object instance field as being a reference type (typically
 * after loading the class and instantiating an object of that
 * class type).  The reverse of gc_object_field_rmref_stub().
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
rboolean gc_object_field_mkref_stub(jvm_object_hash objhashTO,
                                    jvm_field_lookup_index oiflidxTO)
{
    ARCH_FUNCTION_NAME(gc_object_field_mkref_stub);

    return(rfalse);

} /* END of gc_object_field_mkref_stub() */


/*!
 * @brief Add an object instance field reference to an object.
 *
 * Mark object instance field as @e not being a reference type any more
 * (typically before unloading the class).  The reverse of
 * gc_object_field_rmref_stub().
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
rboolean gc_object_field_rmref_stub(jvm_object_hash objhashTO,
                                    jvm_field_lookup_index oiflidxTO)
{
    ARCH_FUNCTION_NAME(gc_object_field_rmref_stub);

    return(rfalse);

} /* END of gc_object_field_rmref_stub() */


/*!
 * @brief Garbage collect an object instance.
 *
 * Finalize garbage collection for an object instance that will no
 * longer be used, as set up in object_instance_delete().  If there
 * are any outstanding references to this class, those must first
 * be removed, at which time gc_run_stub() will perform the
 * finalization instead.  The reverse of gc_object_new_stub().
 *
 * @note Since this function is the reverse of gc_object_new_stub(), the
 *       @link #rclass.pgarbage rclass.pgarbage@endlink pointer must be
 *       freed by @link #HEAP_DATA_FREE() HEAP_DATA_FREE@endlink.
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
rboolean gc_object_delete_stub(jvm_object_hash objhashOLD)
{
    ARCH_FUNCTION_NAME(gc_object_delete_stub);

    return(rfalse);

} /* END of gc_object_delete_stub() */


/*!
 * @brief Start up garbage collection for a new Java virtual
 * method stack frame.
 *
 * Initialize garbage collection for a new stack frame for a
 * virtual Java method invocation, as set up in PUSH_GC().
 * The reverse of gc_stack_delete_stub().
 *
 *
 * @param  thridxNEW    Thread table index to thread that is setting
 *                      up a new stack frame for a method invocation.
 *
 * @param  num_locals   Number of local variables in this method.
 *
 *
 * @returns Pointer to (gc_stack)  for this object instance
 *
 *
 * @attention Due to the fact that there may be any number of garbage
 *            collection algorithms implemented for the JVM, and with
 *            the need to keep the API to the GC system constant, this
 *            return value is @b not defined to be related to
 *            any particular type of GC.  Instead it is a simple
 *            @link #rvoid rvoid@endlink pointer.
 *
 */
rvoid *gc_stack_new_stub(jvm_thread_index thridxNEW, rint num_locals)
{
    ARCH_FUNCTION_NAME(gc_stack_new_stub);

    return((rvoid *) rnull);

} /* END of gc_stack_new_stub() */


/*!
 * @brief Add local variable reference to an object.
 * 
 * Mark object as having another local variable instance reference
 * to it.  The reverse of gc_stack_rmref_stub().
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
rboolean gc_stack_mkref_from_jvm_stub(jvm_thread_index thridxFROM,
                                      jint             frmidxTO)
{
    ARCH_FUNCTION_NAME(gc_stack_mkref_from_jvm_stub);

    return(rfalse);

} /* END of gc_stack_mkref_from_jvm_stub() */


/*!
 * @brief Remove local variable reference from an object.
 *
 * Unmark object from having another local variable instance
 * reference to it.  The reverse of gc_stack_mkref_stub().
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
rboolean gc_stack_rmref_from_jvm_stub(jvm_thread_index thridxFROM,
                                      jint             frmidxTO)
{
    ARCH_FUNCTION_NAME(gc_stack_rmref_from_jvm_stub);

    return(rfalse);

} /* END of gc_stack_rmref_from_jvm_stub() */


/*!
 * @brief Garbage collect a Java virtual method stack frame.
 *
 * Finalize garbage collection for a stack frame that will no
 * longer be used by a virtual Java method, as set up in PUSH_GC().
 * This function is called from POP_GC().  It is the reverse
 * of gc_stack_new_stub().
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
rboolean gc_stack_delete_stub(jvm_thread_index    thridxOLD,
                              rvoid             **ppgcs,
                              jint               *plocal_teardown)
{
    ARCH_FUNCTION_NAME(gc_stack_delete_stub);

    return(rfalse);

} /* END of gc_stack_delete_stub() */

#endif /* CONFIG_GC_TYPE_STUB || CONFIG_OPTIONS_COMPILE_ALL */


/* EOF */
