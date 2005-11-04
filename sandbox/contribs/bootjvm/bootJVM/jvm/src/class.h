#ifndef _class_h_included_
#define _class_h_included_

/*!
 * @file class.h
 *
 * @brief Definition of the @c @b java.lang.Class structure in
 * this real machine implementation.
 *
 * The definition of a class index is found here, as is the
 * definition of a class.  When initializing an class,
 * the following activities happen in the class array:
 *
 * <b>(1)</b> Its class index is used to address an empty slot in
 *            the class storage area.
 *
 * <b>(2)</b> Since it is not a
              @link #jvm_class_index_null jvm_class_index_null@endlink
 *            value, its
 *            @link rclass#status status@endlink is set to
 *            @link #CLASS_STATUS_INUSE CLASS_STATUS_INUSE@endlink,
 *            with an @c @b |
              @link #OBJECT_STATUS_ARRAY OBJECT_STATUS_ARRAY@endlink
 *            if it is an array type.  All other bits are clear.
 *
 * <b>(3)</b> Allocate an object table slot that references this class
 *            table slot.  This a class definition can be references
 *            using an object hash.
 *
 * <b>(4)</b> Recursively define array class types.
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

ARCH_HEADER_COPYRIGHT_APACHE(class, h,
"$URL$",
"$Id$");


#include "object.h"

/*!
 * @def CLASS
 * @brief Access structures of class table at certain index.
 *
 * The class table, being an array of slots, provides space for
 * one class definition per slot.  This macro references one of
 * them using the @p @b clsidx index.
 *
 * @param clsidx  Class table index into the global
 *                @link #rjvm.class rjvm.class[]@endlink array (via
 *                @link #pjvm pjvm->class[]@endlink).
 * 
 * @returns pointer to a class slot
 * 
 */
#define CLASS(clsidx) pjvm->class[clsidx]

/*!
 * @brief General class slot definition.
 */
typedef struct
{
    rushort status;      /*!< Runtime status of class, bitwise */


/*!
 * @name Class status bits
 *
 * @brief Bitwise status bits for the status of a class slot.
 *
 * These class status bits have direct
 * @link #OBJECT_STATUS_EMPTY OBJECT_STATUS_xxx@endlink equivalents and
 * a few @link #THREAD_STATUS_EMPTY THREAD_STATUS_xxx@endlink
 * equivalents also. There are no overloaded bit positions between them
 * (for ease of diagnostics).
 */

/*@{ */ /* Begin grouped definitions */

/****** First 3 bits same for class, object, and thread ***********/
#define CLASS_STATUS_EMPTY    0x0000 /**< This slot is available for
                                          use*/
#define CLASS_STATUS_INUSE    0x0001 /**< This slot contains a class */
#define CLASS_STATUS_NULL     0x0002 /**< NULL class slot.
                                      * <em>Exactly on</em> exists
                                      * in normal use, any else besides
                                      * the @link #JVMCFG_NULL_CLASS
                                        JVMCFG_NULL_CLASS@endlink
                                      * is a class slot now
                                      * being initialized. */
/******************************************************************/

/****** Next 3 bits same for class and object *********************/
#define CLASS_STATUS_GCREQ    0x0004 /**< Class may be garbage
                                          collected. */
#define CLASS_STATUS_ARRAY    0x0008 /**< Class is an array type where
                                          @p @b arraydims contains
                                          number of dimensions */
#define CLASS_STATUS_REFERENCE 0x0010 /**< Class static variable is a
                                       * reference.  This is the
                                       * @e same definition as for
                                       * @link #OBJECT_STATUS_REFERENCE
                                         OBJECT_STATUS_REFERENCE@endlink
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
#define CLASS_STATUS_0020     0x0020 /**< not used */
#define CLASS_STATUS_0040     0x0040 /**< not used */
#define CLASS_STATUS_PRIMATIVE 0x0080 /**< Primative for
                                      *   @c @b java.lang.Class */
#define CLASS_STATUS_0100     0x0100 /**< not used */
#define CLASS_STATUS_0200     0x0200 /**< not used */
#define CLASS_STATUS_LINKED   0x0400 /**< Class linkages completed */
#define CLASS_STATUS_DOCLINIT 0x0800 /**< Class loaded,
                                      * needs @c @b \<clinit\> */
#define CLASS_STATUS_CLINIT   0x1000 /**< Class loaded, initialized, 
                                      * and ready to allocate
                                      * instances */
/******************************************************************/

/****** Last 3 bits not used by class or object *******************/
#define CLASS_STATUS_2000     0x2000 /**< not used */
#define CLASS_STATUS_4000     0x4000 /**< not used */
#define CLASS_STATUS_8000     0x8000 /**< not used */
/******************************************************************/

/*@} */ /* End of grouped definitions */

    u1 unused1;          /**< Not used, keep 2-byte alignment */

    jvm_array_dim arraydims; /**< Number of array dimensions,
                          * meaningful only when @link
                          #CLASS_STATUS_ARRAY CLASS_STATUS_ARRAY@endlink
                          * is set */


    jint *arraylength;   /**<  Array of length @p @b arraydims
                          * containing the length of array in each of
                          * those dimensions.  E.g., @b arraydims is
                          * 4 for new X[7][3][9][2] so this parameter
                          * will be a 4-element array containing the
                          * numbers {7, 3, 9, 2} */

    jvm_class_index  lower_dim_array; /**< Class table index of
                          * version of this class with one fewer array
                          * dimensions, meaningful only when @link
                          #CLASS_STATUS_ARRAY CLASS_STATUS_ARRAY@endlink
                          * is set. */

    jvm_object_hash  class_objhash; /**< Object table hash used to
                                         find this slot */

    u2 num_class_static_field_lookups; /**< size of
                          * @p @b class_static_field_lookup
                          * field lookup array[] for class static fields
                          */

    jvm_field_index *class_static_field_lookup; /**< field lookup
                          * array[] for class static fields.
                          * Indexed by @link #jvm_field_lookup_index
                            jvm_field_lookup_index@endlink
                          */

    jvalue *class_static_field_data; /**< field lookup array[] for
                          * class static fields.
                          * Indexed by @link #jvm_field_lookup_index
                            jvm_field_lookup_index@endlink
                          */



    u2 num_object_instance_field_lookups; /**< size of
                          * @p @b object_instance_field_lookup
                          * field lookup array[] for object instance
                          * fields */

    jvm_field_index *object_instance_field_lookup; /**< field lookup
                          * array[] for object instance fields.
                          * The jvalue array[] for each object may
                          * be found in @link
                            #robject.object_instance_field_data
                             robject.object_instance_field_data@endlink
                          * instead of here in order to have unique
                          * values for each and every object.  (This is
                          * over and against the class static field
                          * array[] here in this structure.)
                          * Indexed by @link #jvm_field_lookup_index
                            jvm_field_lookup_index@endlink
                          */


    jvm_class_index initiating_ClassLoader; /**<Object table hash
                          * of initiating @c @b ClassLoader */

    jvm_class_index defining_ClassLoader; /**< Object table hash
                          * of defining @c @b ClassLoader */

    rvoid          *pgarbage; /**< Garbage collection profile of
                          * this class.  An @link #rvoid rvoid@endlink
                          * pointer is used here to avoid linking this
                          * structure to any particular GC
                          * implementation.
                          */

} rclass;

/* Prototypes for functions in 'class.c' */
extern rvoid           class_init(rvoid);
extern rvoid           class_shutdown_1(rvoid);
extern rvoid           class_shutdown_2(rvoid);
extern jvm_class_index class_static_new(rushort          status_req,
                                        ClassFile       *pcfs,
                                        jvm_array_dim    arraydims,
                                        jint            *arraylength,
                                       jvm_class_index lower_dim_array);
extern jvm_class_index class_reload(jvm_class_index clsidxOLD);
extern jvm_class_index class_static_delete(jvm_class_index clsidx,
                                           rboolean        rmref);
extern jvm_class_index class_find_by_cp_entry(cp_info_dup *clsname);
extern jvm_class_index class_find_by_prchar(rchar *clsname);
extern jvm_class_index class_load_primative(u1 basetype);
extern jvm_class_index class_load_from_prchar(rchar    *clsname,
                                              rboolean
                                                   find_registerNatives,
                                              jint     *arraylength);
extern jvm_class_index class_load_from_cp_entry_utf(
                                             cp_info_dup *clsname,
                                             rboolean
                                                   find_registerNatives,
                                             jint        *arraylength);

extern jvm_class_index class_load_resolve_clinit(
                                          rchar        *clsname,
                                       jvm_thread_index thridx,
                                          rboolean      usesystemthread,
                                         rboolean find_registerNatives);

extern u2               class_get_num_static_fields(ClassFile *pcfs);
extern jvm_class_index *class_get_static_field_lookups(
                                              ClassFile *pcfs);
extern jvalue          *class_get_static_field_data(
                                        jvm_class_index  clsidx,
                                              ClassFile *pcfs);

extern u2               class_get_num_object_instance_fields(
                                                       ClassFile *pcfs);
extern jvm_class_index *class_get_object_instance_field_lookups(
                                                       ClassFile *pcfs);
extern jvalue          *class_get_object_instance_field_data(
                                        jvm_class_index  clsidx,
                                        jvm_object_hash  objhash,
                                              ClassFile *pcfs);


/* Prototypes for functions in 'classutil.c' */

extern rboolean classutil_subclass_of(jvm_class_index clsidx1,
                                      jvm_class_index clsidx2);

extern rboolean classutil_implements_interface(jvm_class_index clsidx1,
                                               jvm_class_index clsidx2);

extern rboolean classutil_superinterface_of(jvm_class_index clsidx1,
                                            jvm_class_index clsidx2);

extern rboolean
      classutil_interface_implemented_by_arrays(jvm_class_index clsidx);

#endif /* _class_h_included_ */


/* EOF */
