#ifndef _object_h_included_
#define _object_h_included_

/*!
 * @file object.h
 *
 * @brief Definition of the @c @b java.lang.Object structure in
 * this real machine implementation.
 *
 * When initializing an object, the following activities happen in
 * the object array:
 *
 * <b>(1)</b> Its @link #jvm_object_hash jvm_object_hash@endlink is
 *            used to address an empty slot in the object storage area.
 *
 * <b>(2)</b> Its @link #ACC_PUBLIC ACC_xxx@endlink flags from the
 *            class file are stored into
 *            @link robject#access_flags access_flags@endlink.
 *
 * <b>(3)</b> Its @link #robject.mlock_thridx mlock_thridx@endlink
 *            is cleared, that is, no thread holds its monitor lock.
 *
 * <b>(4)</b> Since it is not a
 *            @link #jvm_object_hash_null jvm_object_hash_null@endlink
 *            value, its
 *            @link robject#status status@endlink is set to
 *            @link #OBJECT_STATUS_INUSE OBJECT_STATUS_INUSE@endlink.
 *            All other bits are clear.
 *
 * <b>(5)</b> Declare to garbage collection that this object is now
 *            referenced.
 *
 * <b>(6)</b> The data value(s) are stored in the array
 *            @link robject#object_instance_field_data
              object_instance_field_data@endlink according to their
 *            various types.  Since this array is a (jvalue), fields
 *            of @e any @b mixed type may be stored here.  For example,
 *            An (int) is stored in
 *            @link robject#object_instance_field_data
              object_instance_field_data@endlink.@link
              jvalue#_jint _jint@endlink), while an object reference is
 *            stored in @link robject#object_instance_field_data
              object_instance_field_data@endlink.@link
              jvalue#_jobjhash _jobjhash@endlink.  Notice that a
 *            @c @b java.lang.Integer is @em not an (int), and
 *            has its several fields stored in this array as their
 *            normal types.
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

ARCH_HEADER_COPYRIGHT_APACHE(object, h,
"$URL$",
"$Id$");


/*
 * Macros for addressing objects
 */

/*!
 * @def OBJECT
 * @brief Access structures of object table at certain index.
 *
 * The object table, being an array of slots, provides space for
 * one object instance per slot.  This macro references one of
 * them using the @p @b objhash index.
 *
 * @param objhash  Object table index into the global
 *                 @link #rjvm.object rjvm.object[]@endlink array (via
 *                 @link #pjvm pjvm->object[]@endlink).
 * 
 * 
 * @returns pointer to a object slot
 * 
 */
#define OBJECT(objhash) pjvm->object[objhash]

/*!
 * @brief Type definition for references to Java classes, enums,
 * interfaces.
 *
 * This information points to the major structures
 * for purposes of cross-referencing objects to ClassFile structures,
 * objects to/from classes, and objects to/from threads.
 *
 * The macro CLASS_OBJECT_LINKAGE() is designed to access this structure
 * given any class table index.
 *
 * The macro OBJECT_CLASS_LINKAGE() is designed to access this structure
 * given any object hash.
 *
 * The macro THREAD_OBJECT_LINKAGE() is designed to access this
 * structure given any thread table index.
 *
 * The macro OBJECT_THREAD_LINKAGE() is designed to access this
 * structure given any object hash.
 *
 *
 * @internal The JVM spec compliance of much code depends
 *           code depends on packed structures, especially
 *           in the class file arena, yet the use of global
 *           project-wide -fpack-struct (GCC version of
 *           structure packing option) conflicts with issues
 *           such as portability.  Since the largest structures
 *           of the code are not inherently portably, they
 *           are packed here with a pragma and the compiler
 *           is free to not pack anything else.
 *
 */
#pragma pack(1)

typedef struct
{
    ClassFile      *pcfs;      /**< Class file storage area */

    jvm_class_index clsidx;    /**< JVM class table class defn index */

    jvm_thread_index thridx;   /**< JVM thread table class defn index,
                                *   meaninful only if this is a
                                *   @c @b java.lang.Thread
                                *   object, else @link
                                *   #jvm_thread_index_null
                                    jvm_thread_index_null@endlink
                                */
} jvm_table_linkage;

/*!
 * @internal Remove effects of packing pragma on other code.
 *
 */
#pragma pack()


/*!
 * @brief General object slot definition.
 *
 *
 * @internal The JVM spec compliance of much code depends
 *           code depends on packed structures, especially
 *           in the class file arena, yet the use of global
 *           project-wide -fpack-struct (GCC version of
 *           structure packing option) conflicts with issues
 *           such as portability.  Since the largest structures
 *           of the code are not inherently portably, they
 *           are packed here with a pragma and the compiler
 *           is free to not pack anything else.
 *
 */
#pragma pack(1)


typedef struct
{
    rushort status;      /**< Runtime status of object, bitwise */

/*!
 * @name Object status bits
 *
 * @brief Bitwise status bits for the status of an object slot.
 *
 * These object status bits have direct
 * @link #CLASS_STATUS_EMPTY CLASS_STATUS_xxx@endlink equivalents and a
 * few @link #THREAD_STATUS_EMPTY THREAD_STATUS_xxx@endlink equivalents
 * also.  There are no overloaded bit positions between them
 * (for ease of diagnostics).
 */

/*@{ */ /* Begin grouped definitions */

/****** First 3 bits same for class, object, and thread ***********/
#define OBJECT_STATUS_EMPTY  0x0000 /**< This slot is available
                                         for use */
#define OBJECT_STATUS_INUSE  0x0001 /**< This slot contains an object */
#define OBJECT_STATUS_NULL   0x0002 /**< NULL object (only 1 exists in
                                     * normal use, any else besides the
                                     * JVMCFG_NULL_OBJECT is an object
                                     * slot now being initialized.) */
/******************************************************************/

/****** Next 3 bits same for class and object *********************/
#define OBJECT_STATUS_GCREQ  0x0004 /**< Object may be garbage
                                         collected */
#define OBJECT_STATUS_ARRAY  0x0008 /**< Object is an array instead of
                                         an object instance*/
#define OBJECT_STATUS_REFERENCE 0x0010 /**< Object instance variable is
                                        * a reference.  This is the
                                        * @e same definition as for
                                        * @link #CLASS_STATUS_REFERENCE
                                          CLASS_STATUS_REFERENCE@endlink
                                        *  and for
                                        * @link #LOCAL_STATUS_REFERENCE
                                         LOCAL_STATUS_REFERENCE@endlink,
                                        * the local variable reference
                                        * bit for variables on the stack
                                        * frame, where the GC algorithm
                                        * implements it.
                                        */
/******************************************************************/

/****** Next 9 bits unique between class and object ***************/
#define OBJECT_STATUS_SUBARRAY 0x0020/**< This is a subset of an array,
                                         that is, of smaller dimension*/
#define OBJECT_STATUS_MLOCK  0x0040 /**< Object monitor locked by
                                         @link #robject.mlock_thridx
                                         mlock_thridx@endlink */
#define OBJECT_STATUS_0080   0x0080 /**< not used */
#define OBJECT_STATUS_CLASS  0x0100 /**< This object is a class
                                         definition instead of an
                                         object instance */
#define OBJECT_STATUS_THREAD 0x0200 /**< Object is a
                                         @c @b java/Lang/Thread */
#define OBJECT_STATUS_STRING 0x0400 /**< Object is a
                                         @c @b java/Lang/String */
#define OBJECT_STATUS_0800   0x0800 /**< not used */
#define OBJECT_STATUS_1000   0x1000 /**< not used */
/******************************************************************/

/****** Last 3 bits not used by class or object *******************/
#define OBJECT_STATUS_2000   0x2000 /**< not used */
#define OBJECT_STATUS_4000   0x4000 /**< not used */
#define OBJECT_STATUS_8000   0x8000 /**< not used */
/******************************************************************/

/*@} */ /* End of grouped definitions */

    jvm_basetype arraybasetype; /**< Base element type of array,
                                 * meaningful only when @link
                                   #OBJECT_STATUS_ARRAY
                                   OBJECT_STATUS_ARRAY@endlink is set
                                 */

    jvm_array_dim arraydims;    /** Num dimensions for array,
                                 * meaningful only when @link
                                   #OBJECT_STATUS_ARRAY
                                   OBJECT_STATUS_ARRAY@endlink is set
                                 */

    /*!
     * @todo HARMONY-6-jvm-object.h-1 Is @c @b arraylength necessary?
     */
    jint arraylength;           /**< First dimension of an array of
                                 * length @c @b arraydims as passed in
                                 * to object_instance_new(), which
                                 * contains the length of array in each
                                 * of those dimensions.
                                 * Meaningful only when when @link
                                   #OBJECT_STATUS_ARRAY
                                   OBJECT_STATUS_ARRAY@endlink is set
                                 */

    rvoid *arraydata;           /**< Data for an arbitrary array.  For
                                 *  @link #arraydims arraydims@endlink
                                 *  of 0, it is not used,for
                                 *  @link #arraydims arraydims@endlink
                                 *  of 1, contains data for
                 <b><code>((basetype) array)[arraylength[0]]</code></b>.
                                  For @link #arraydims arraydims@endlink
                                 *  greater than 1, contains object
                                 *  reference array to next lower
                                 *  dimension,
    <b><code>((jvm_object_hash *) &arraydata)[arraylength[0]]</code></b>
                                 */

    jint mlock_count;           /**< Number of times the object monitor
                                     was locked */

    jvm_thread_index  mlock_thridx; /**< This thread holds monitor lock,
                                 *  meaningful only when @link
                                   #OBJECT_STATUS_MLOCK
                                   OBJECT_STATUS_MLOCK@endlink is set
                                 */

    jvm_object_hash objhash_superclass; /**< Instance of
                                 * of this object's superclass, namely
                                 * the @c @b super object.
                                 * @link #JVMCFG_NULL_OBJECT
                                   JVMCFG_NULL_OBJECT@endlink indicates
                                 * that the parent class is a
                                 * @c @b java.lang.Object
                                 */

    jvm_access_flags access_flags; /**< Holds class file
                                 * @link #ACC_PUBLIC ACC_xxx@endlink
                                 * values
                                 */

    jvm_table_linkage table_linkage; /**< Connect related instances of
                                 * ClassFile, rclass, robject,  and
                                 * rthread structures.  Used intensively
                                 * all over the code, this table is
                                 * found in each and every robject and
                                 * is the central structure in the
                                 * linkage macros of
                                 * @link jvm/src/linkage.h
                                 * linkage.h@endlink .
                                 */

    jvalue *object_instance_field_data; /**< Object instance data
                                 * data contents array[].  The
                                 * associated field lookup table is
                                 * located in its rclass table entry
                                 * @link
                                   #rclass.object_instance_field_lookup
                             rclass.object_instance_field_lookup@endlink

                                 * since these lookups are the same for
                                 * every instance object of this class.
                                 * Not meaningful when @link
                                   #OBJECT_STATUS_ARRAY
                                   OBJECT_STATUS_ARRAY@endlink
                                 * is set.  Instead, see
                                 * @link #arraydata arraydata@endlink
                                 */

    rvoid          *pgarbage; /**< Garbage collection profile of
                                 * this class.  An
                                 * @link #rvoid rvoid@endlink pointer
                                 * is used here to avoid linking this
                                 * structure to any particular GC
                                 * implementation.
                                 */

} robject;

/*!
 * @internal Remove effects of packing pragma on other code.
 *
 */
#pragma pack()

#ifndef LOCAL_STATUS_REFERENCE
/*!
 * @brief Reserved symbol for garbage collection of
 * local variables, distinguishing primatives from reference types.
 *
 * When garbage collecting local variables on the stack,
 * there must be a distinction drawn between local variables that
 * are primative data types and those that area reference types.
 * This symbol is reserved for that purpose.  If implemented, it
 * will @e always be found in the @b gc_XXX.c and @b gc_XXX.h source
 * files and will @e never be found anywhere else.  However, it is
 * also mentioned in the documentation outside of GC modules;
 * therefore, it is documented here for completeness.
 *
 * The actual value and implementation of this symbol is dependent
 * on the GC system and the constraints given elsewhere in the
 * documentation.  The value given here is a dummy value.  Its
 * actual value must be declared previous to the inclusion in a
 * source file of this header file.
 *
 */
#define LOCAL_STATUS_REFERENCE OBJECT_STATUS_REFERENCE

#endif

/* Prototypes for functions in 'object.c' */

extern rvoid object_init(rvoid);
extern rvoid object_shutdown(rvoid);
extern jvm_object_hash object_utf8_string_lookup(CONSTANT_Utf8_info *
                                                                string);
extern rvoid object_new_setup(jvm_object_hash objhash);
extern jvm_object_hash object_instance_new(rushort         special_obj,
                                           ClassFile       *pcfs,
                                           jvm_class_index  clsidx,
                                           jvm_array_dim    arraydims,
                                           jint            *arraylength,
                                           rboolean         run_init_,
                                           jvm_thread_index thridx,
                                           CONSTANT_Utf8_info *string);
extern rvoid object_instance_finalize(jvm_object_hash objhash,
                                      jvm_thread_index thridx);
extern jvm_object_hash object_instance_delete(jvm_object_hash objhash,
                                              rboolean        rmref);


/* Prototypes for functions in 'objectutil.c' */

extern rboolean objectutil_synchronize(jvm_object_hash  objhashthis,
                                       jvm_thread_index thridx);

extern rvoid objectutil_unsynchronize(jvm_object_hash  objhashthis,
                                      jvm_thread_index thridx);

extern rboolean objectutil_release(jvm_object_hash  objhashthis,
                                   jvm_thread_index thridx);
#endif /* _object_h_included_ */

/* EOF */
