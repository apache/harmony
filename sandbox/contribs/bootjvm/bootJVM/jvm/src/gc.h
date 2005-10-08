#ifndef _gc_h_included_
#define _gc_h_included_

/*!
 * @file gc.h
 *
 * @brief Garbage collection structures and API
 *
 * The logic of these structures and functions is empty, pending
 * a garbage collection design for the project.
 *
 * This common header file @link jvm/src/gc.h gc.h@endlink defines
 * the prototypes for all garbage collection implementations by way
 * of the @link #CONFIG_GC_TYPE_STUB CONFIG_GC_TYPE_xxx@endlink
 * symbol definition.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/gc.h $ \$Id: gc.h 0 09/28/2005 dlydick $
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

#include "arch.h"
ARCH_COPYRIGHT_APACHE(gc, h, "$URL: https://svn.apache.org/path/name/gc.h $ $Id: gc.h 0 09/28/2005 dlydick $");

#ifdef CONFIG_GC_TYPE_STUB
/*!
 * @name Stub garbage collection model definitions
 *
 * @brief Expand the @b GC_xxx() macros into the simple heap model as
 * implemented by @link jvm/src/gc_stub.c gc_stub.c@endlink.
 *
 * Each garbage collection algorithm will have a section just
 * like this here in this file so that these macros will expand
 * to point to the API as implemented by that algoritm.  For
 * some examples as to how this is done for other modules,
 * please refer to @link jvm/src/heap.h heap.h@endlink .
 *
 */

/*@{ */ /* Begin grouped definitions */

#define GC_INIT gc_init_stub
#define GC_RUN  gc_run_stub

#define GC_CLASS_NEW                gc_class_new_stub
#define GC_CLASS_RELOAD             gc_class_reload_stub

#define GC_CLASS_MKREF_FROM_CLASS   gc_class_mkref_from_class_stub
#define GC_CLASS_MKREF_FROM_OBJECT  gc_class_mkref_from_object_stub
#define GC_CLASS_RMREF_FROM_CLASS   gc_class_rmref_from_class_stub
#define GC_CLASS_RMREF_FROM_OBJECT  gc_class_rmref_from_object_stub
#define GC_CLASS_FIELD_MKREF        gc_class_field_mkref_stub
#define GC_CLASS_FIELD_RMREF        gc_class_field_rmref_stub
#define GC_CLASS_DELETE             gc_class_delete_stub

#define GC_OBJECT_NEW               gc_object_new_stub
#define GC_OBJECT_MKREF_FROM_CLASS  gc_object_mkref_from_class_stub
#define GC_OBJECT_MKREF_FROM_OBJECT gc_object_mkref_from_object_stub
#define GC_OBJECT_RMREF_FROM_CLASS  gc_object_rmref_from_class_stub
#define GC_OBJECT_RMREF_FROM_OBJECT gc_object_rmref_from_object_stub
#define GC_OBJECT_FIELD_MKREF       gc_object_field_mkref_stub
#define GC_OBJECT_FIELD_RMREF       gc_object_field_rmref_stub
#define GC_OBJECT_DELETE            gc_object_delete_stub

#define GC_STACK_NEW                gc_stack_new_stub
#define GC_STACK_MKREF_FROM_JVM     gc_stack_mkref_from_jvm_stub
#define GC_STACK_RMREF_FROM_JVM     gc_stack_rmref_from_jvm_stub
#define GC_STACK_DELETE             gc_stack_delete_stub

/*@} */ /* End of grouped definitions */
#endif


/* Prototypes for functions in 'gc_XXX.c' */

extern rvoid GC_INIT(rvoid);
extern rvoid GC_RUN(rboolean rmref);

extern rboolean  GC_CLASS_NEW(jvm_class_index clsidxNEW);
extern rboolean  GC_CLASS_RELOAD(jvm_class_index clsidxOLD,
                                 jvm_class_index clsidxNEW);

extern rboolean  GC_CLASS_MKREF_FROM_CLASS(jvm_class_index clsidxFROM,
                                           jvm_class_index clsidxTO);
extern rboolean  GC_CLASS_MKREF_FROM_OBJECT(jvm_object_hash objhashFROM,
                                            jvm_class_index clsidxTO);
extern rboolean  GC_CLASS_RMREF_FROM_CLASS(jvm_class_index clsidxFROM,
                                           jvm_class_index clsidxTO);
extern rboolean  GC_CLASS_RMREF_FROM_OBJECT(jvm_object_hash objhashFROM,
                                            jvm_class_index clsidxTO);
extern rboolean  GC_CLASS_FIELD_MKREF(jvm_class_index        clsidxTO,
                                      jvm_field_lookup_index csflidxTO);
extern rboolean  GC_CLASS_FIELD_RMREF(jvm_class_index        clsidxTO,
                                      jvm_field_lookup_index csflidxTO);
extern rboolean  GC_CLASS_DELETE(jvm_class_index clsidxOLD,
                                 rboolean        delete_class);

extern rboolean  GC_OBJECT_NEW(jvm_object_hash objhashNEW);
extern rboolean  GC_OBJECT_MKREF_FROM_CLASS(jvm_class_index clsidxFROM,
                                            jvm_object_hash objhashTO);
extern rboolean GC_OBJECT_MKREF_FROM_OBJECT(jvm_object_hash objhashFROM,
                                             jvm_object_hash objhashTO);
extern rboolean  GC_OBJECT_RMREF_FROM_CLASS(jvm_class_index clsidxFROM,
                                            jvm_object_hash objhashTO);
extern rboolean GC_OBJECT_RMREF_FROM_OBJECT(jvm_object_hash objhashFROM,
                                             jvm_object_hash objhashTO);
extern rboolean  GC_OBJECT_FIELD_MKREF(jvm_object_hash        objhashTO,
                                      jvm_field_lookup_index oiflidxTO);
extern rboolean  GC_OBJECT_FIELD_RMREF(jvm_object_hash        objhashTO,
                                      jvm_field_lookup_index oiflidxTO);
extern rboolean  GC_OBJECT_DELETE(jvm_object_hash objhashOLD);

extern rvoid    *GC_STACK_NEW(jvm_thread_index thridxNEW,
                              rint num_locals);
extern rboolean  GC_STACK_MKREF_FROM_JVM(jvm_thread_index thridxFROM,
                                         jint frmidxTO);
extern rboolean  GC_STACK_RMREF_FROM_JVM(jvm_thread_index thridxFROM,
                                         jint frmidxTO);
extern rboolean  GC_STACK_DELETE(jvm_thread_index   thridxOLD,
                                 rvoid            **ppgcm,
                                 jint              *plocal_teardown);

#endif /* _gc_h_included_ */

/* EOF */
