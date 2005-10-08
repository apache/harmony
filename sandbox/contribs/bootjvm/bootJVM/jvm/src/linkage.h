#ifndef _linkage_h_included_
#define _linkage_h_included_

/*!
 * @file linkage.h
 *
 * @brief Runtime linkages between major data structures.
 *
 * Of particular interest are the linkages between thread, class,
 * and object areas.
 *
 * Several useful macros are defined here.  They are used to associate
 * an object instance with its ClassFile structure, its class
 * definition, and its thread definition, where applicable.
 *
 * The macro CLASS_OBJECT_LINKAGE() associates a class definition with
 * its object instance.  The macro OBJECT_CLASS_LINKAGE() associates
 * an object instance with its class definition.
 *
 * The macro THREAD_OBJECT_LINKAGE() associates a thread definition with
 * its object instance.  The macro OBJECT_THREAD_LINKAGE() associates
 * an object instance with its thread definition.  This functionality
 * is @e only meaningful when the object is a
 * <b><code>java.lang.Thread</code></b>.
 *
 * The information stored in that object table entry points to both
 * the ClassFile storage for that class and to the class table entry
 * for that class.
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/linkage.h $ \$Id: linkage.h 0 09/28/2005 dlydick $
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
 * @version \$LastChangedRevision: 0 $
 *
 * @date \$LastChangedDate: 09/28/2005 $
 *
 * @author \$LastChangedBy: dlydick $
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

ARCH_COPYRIGHT_APACHE(linkage, h, "$URL: https://svn.apache.org/path/name/linkage.h $ $Id: linkage.h 0 09/28/2005 dlydick $");


/*!
 * @def CLASS_OBJECT_LINKAGE()
 *
 * @brief Retrieve class information about a class from its
 * class object.
 *
 * @param  clsidx     Class table index of class definition for which
 *                    to locate its class definition
 *                   @link #jvm_table_linkage jvm_table_linkage@endlink.
 *
 *
 * @returns (jvm_table_linkage *) to object table class linkage entry
 *
 */
#define CLASS_OBJECT_LINKAGE(clsidx) \
                (&OBJECT(CLASS(clsidx).class_objhash).table_linkage)

/*!
 * @def OBJECT_CLASS_LINKAGE()
 *
 * @brief Retrieve class information about an object from its
 * class.
 *
 * @param  objhash    Object hash of object for which to locate its
 *                    class table entry.
 *
 * @returns (jvm_table_linkage *) to object table class linkage entry
 *
 */

#define OBJECT_CLASS_LINKAGE(objhash) (&OBJECT(objhash).table_linkage)


/* Now the thread definitions: */

/*!
 * @def VERIFY_THREAD_LINKAGE()
 *
 * @brief Verify that a thread slot is in use, is not a null thread,
 * and contains a valid, non-null, thread object hash that is
 * definitely marked as a thread object
 *
 * @param  thridx     Thread table index of thread to verify its
 *                    thread table entry.
 *
 *
 * @returns @link #rtrue rtrue@endlink if this thread index indicates
 * a valid thread table slot, otherwise @link #rfalse rfalse@endlink.
 *
 */
#define VERIFY_THREAD_LINKAGE(thridx)                            \
   (((thridx != jvm_thread_index_null) &&                        \
     (THREAD(thridx).status & THREAD_STATUS_INUSE) &&            \
     (!(THREAD(thridx).status & THREAD_STATUS_NULL)) &&          \
     (THREAD(thridx).thread_objhash != jvm_object_hash_null)&&   \
     (OBJECT(THREAD(thridx).thread_objhash).status &             \
                                        OBJECT_STATUS_INUSE) &&  \
     (!(OBJECT(THREAD(thridx).thread_objhash).status &           \
                                        OBJECT_STATUS_NULL)) &&  \
     (OBJECT(THREAD(thridx).thread_objhash).status &             \
                                        OBJECT_STATUS_THREAD))   \
    ? rtrue                                                      \
    : rfalse)


/*!
 * @def THREAD_OBJECT_LINKAGE()
 *
 * @brief Retrieve object information about a thread from its
 * thread object instance.
 *
 * @param  thridx     Thread table index of thread definition for which
 *                    to locate its class definition
 *                   @link #jvm_table_linkage jvm_table_linkage@endlink.
 *
 *
 * @returns (jvm_table_linkage *) to object table class linkage entry
 *
 *
 * @note  This macro will return @link #rnull rnull@endlink if
 *        the object hash in this slot is a
 *        @link #jvm_thread_index_null jvm_thread_index_null@endlink
 *        reference.
 *        
 */
#define THREAD_OBJECT_LINKAGE(thridx)                         \
    ((THREAD(thridx).thread_objhash != jvm_object_hash_null)  \
     ? (&OBJECT(THREAD(thridx).thread_objhash).table_linkage) \
     : (jvm_table_linkage *) rnull)


/*!
 * @def OBJECT_THREAD_LINKAGE()
 *
 * @brief Retrieve thread information about an object from its
 * thread entry.
 *
 *
 * @param  objhash    Object hash of object for which to locate its
 *                    thread table entry.
 *
 *
 * @returns (jvm_table_linkage *) to object table class linkage entry of
 *                               <b><code>java.lang.Thread</code></b>
 *                               class object.
 *
 * @note  This macro will return @link #rnull rnull@endlink if the
 * object in this slot is not a <b><code>java.lang.Thread</code></b>
 * object and one which indexes a valid thread.
 *
 */
#define OBJECT_THREAD_LINKAGE(objhash)                                 \
    (((OBJECT(objhash).status & OBJECT_STATUS_INUSE) &&                \
      (OBJECT(objhash).status & OBJECT_STATUS_THREAD) &&               \
      (jvm_thread_index_null != OBJECT(objhash).table_linkage.thridx)) \
     ? (&OBJECT(objhash).table_linkage)                                \
     : ((jvm_table_linkage *) rnull))


/*!
 * @def VERIFY_OBJECT_THREAD_LINKAGE()
 *
 * @brief Verify that a thread slot is in use, is not a null thread,
 * and contains a valid thread object hash.
 *
 * @param  objhash    Object hash of object for which to verify its
 *                    thread linkage and its thread table entry.
 *
 *
 * @returns @link #rtrue rtrue@endlink if this object hash indicates
 * a valid thread linkage and a thread table slot, otherwise
 * @link #rfalse rfalse@endlink.
 *
 */
#define VERIFY_OBJECT_THREAD_LINKAGE(objhash)   \
   ((rnull != (OBJECT_THREAD_LINKAGE(objhash))) \
    ? rtrue                                     \
    : rfalse)


/* Finally the object self-referential definitions: */

/*!
 * @def OBJECT_OBJECT_LINKAGE()
 *
 * @brief Retrieve object information about an object from itself.
 *
 * @param  objhash    Object hash of object for which
 *                    to locate its class definition
 *                   @link #jvm_table_linkage jvm_table_linkage@endlink.
 *
 *
 * @returns (jvm_table_linkage *) to object table class linkage entry
 *
 *
 * @note  This macro will return @link #rnull rnull@endlink if the
 *        object hash in this slot is a
 *        @link #jvm_object_hash_null jvm_object_hash_null@endlink
 *        reference.
 *
 */

#define OBJECT_OBJECT_LINKAGE(objhash)             \
    ((OBJECT(objhash).status & OBJECT_STATUS_INUSE) \
     ? (&OBJECT(objhash).table_linkage)             \
     : (jvm_table_linkage *) rnull)


/* Prototypes for functions in 'linkage.c' */
extern rboolean linkage_resolve_class(jvm_class_index clsidx,
                                      rboolean    find_registerNatives);

extern rboolean linkage_unresolve_class(jvm_class_index clsidx);

#endif /* _linkage_h_included_ */


/* EOF */
