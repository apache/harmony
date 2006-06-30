#ifndef _attribute_h_included_
#define _attribute_h_included_

/*!
 * @file attribute.h
 *
 * @brief Attribute management functions for the JVM.
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

ARCH_HEADER_COPYRIGHT_APACHE(attribute, h,
"$URL$",
"$Id$");

#include "classfile.h"

/*!
 * @name Attribute single deferencing support.
 *
 * @brief Conveniently reference an
 * @link Code_attribute Xxx_attribute@endlink contained in
 * a <b><code>attribute_info_mem_align *paima</code></b>, namely, with
 * single pointer indirection.
 *
 * This is a counterpart for cfattrib_unload_attribute() where
 * no indirection is needed.  Notice that
 * @link #PTR_ATR_CODE_AI() PTR_ATR_XXX_AI()@endlink references
 * a <b><code>attribute_info_mem_align **ppaima</code></b>, while this
 * macro references a
 * <b><code>attribute_info_mem_align *paima</code></b>.
 *
 */
/*@{ */ /* Begin grouped definitions */

#define ATR_CONSTANTVALUE_AI(paima) \
                                ((ConstantValue_attribute *) &paima->ai)
#define ATR_CODE_AI(paima)               ((Code_attribute *) &paima->ai)
#define ATR_EXCEPTIONS_AI(paima)   ((Exceptions_attribute *) &paima->ai)
#define ATR_INNERCLASSES_AI(paima) \
                                 ((InnerClasses_attribute *) &paima->ai)
#define ATR_ENCLOSINGMETHOD_AI(paima) \
                              ((EnclosingMethod_attribute *) &paima->ai)
#define ATR_SIGNATURE_AI(paima)     ((Signature_attribute *) &paima->ai)
#define ATR_SOURCEFILE_AI(paima)   ((SourceFile_attribute *) &paima->ai)
#define ATR_LINENUMBERTABLE_AI(ppaima) \
                              ((LineNumberTable_attribute *) &paima->ai)
#define ATR_LOCALVARIABLETABLE_AI(paima) \
                           ((LocalVariableTable_attribute *) &paima->ai)
#define ATR_LOCALVARIABLETYPETABLE_AI(paima) \
                       ((LocalVariableTypeTable_attribute *) &paima->ai)
#define ATR_RUNTIMEVISIBLEANNOTATIONS_AI(paima) \
                    ((RuntimeVisibleAnnotations_attribute *) &paima->ai)
#define ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(paima) \
                  ((RuntimeInvisibleAnnotations_attribute *) &paima->ai)
#define ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(paima) \
           ((RuntimeVisibleParameterAnnotations_attribute *) &paima->ai)
#define ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(paima) \
         ((RuntimeInvisibleParameterAnnotations_attribute *) &paima->ai)
#define ATR_ANNOTATIONDEFAULT_AI(paima) \
                            ((AnnotationDefault_attribute *) &paima->ai)
#define ATR_ANNOTATIONDEFAULTMEMALIGN_AI(paima) \
                  ((AnnotationDefault_attribute_mem_align *) &paima->ai)
/*@} */ /* End of grouped definitions */

/*!
 * @name Attribute double deferencing support.
 *
 * @brief Conveniently reference the XXX_attribute contained in
 * an indirect <b><code>attribute_info_mem_align **ppaima</code></b>,
 * namely, with double pointer indirection.
 *
 * After putting in the 4-byte access alignment changes,
 * it became obvious that what was once (*atrptr)->member
 * had become quite cumbersome.  Therefore, in order to
 * simplify access through (attribute_info_mem_align *)->ai.member
 * constructions, including appropriate casting, the following
 * macros were added to take care of the most common usage.
 * The few other references are unchanged, and the code is
 * easier to understand.
 *
 * Notice that @link #ATR_CODE_AI() ATR_XXX_AI()@endlink
 * references a <b><code>attribute_info_mem_align *paima</code></b>,
 * while  @link #PTR_ATR_CODE_AI() PTR_ATR_XXX_AI()@endlink
 * references a <b><code>attribute_info_mem_align **ppaima</code></b>.
 */

/*@{ */ /* Begin grouped definitions */
#define PTR_ATR_CONSTANTVALUE_AI(ppaima) \
                            ((ConstantValue_attribute *) &(*ppaima)->ai)
#define PTR_ATR_CODE_AI(ppaima)      ((Code_attribute *) &(*ppaima)->ai)
#define PTR_ATR_EXCEPTIONS_AI(ppaima) \
                                ((Exceptions_attribute *)&(*ppaima)->ai)
#define PTR_ATR_INNERCLASSES_AI(ppaima) \
                              ((InnerClasses_attribute *)&(*ppaima)->ai)
#define PTR_ATR_ENCLOSINGMETHOD_AI(ppaima) \
                           ((EnclosingMethod_attribute *)&(*ppaima)->ai)
#define PTR_ATR_SIGNATURE_AI(ppaima) \
                                 ((Signature_attribute *)&(*ppaima)->ai)
#define PTR_ATR_SOURCEFILE_AI(ppaima) \
                                ((SourceFile_attribute *)&(*ppaima)->ai)
#define PTR_ATR_LINENUMBERTABLE_AI(ppaima) \
                           ((LineNumberTable_attribute *)&(*ppaima)->ai)
#define PTR_ATR_LOCALVARIABLETABLE_AI(ppaima) \
                        ((LocalVariableTable_attribute *)&(*ppaima)->ai)
#define PTR_ATR_LOCALVARIABLETYPETABLE_AI(ppaima) \
                    ((LocalVariableTypeTable_attribute *)&(*ppaima)->ai)
#define PTR_ATR_RUNTIMEVISIBLEANNOTATIONS_AI(ppaima) \
                 ((RuntimeVisibleAnnotations_attribute *)&(*ppaima)->ai)
#define PTR_ATR_RUNTIMEINVISIBLEANNOTATIONS_AI(ppaima) \
               ((RuntimeInvisibleAnnotations_attribute *)&(*ppaima)->ai)
#define PTR_ATR_RUNTIMEVISIBLEPARAMETERANNOTATIONS_AI(ppaima) \
        ((RuntimeVisibleParameterAnnotations_attribute *)&(*ppaima)->ai)
#define PTR_ATR_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_AI(ppaima) \
      ((RuntimeInvisibleParameterAnnotations_attribute *)&(*ppaima)->ai)
#define PTR_ATR_ANNOTATIONDEFAULT_AI(ppaima) \
                         ((AnnotationDefault_attribute *)&(*ppaima)->ai)
#define PTR_ATR_ANNOTATIONDEFAULTMEMALIGN_AI(ppaima) \
               ((AnnotationDefault_attribute_mem_align *)&(*ppaima)->ai)

/*@} */ /* End of grouped definitions */


extern jvm_attribute_index attribute_find_in_field_by_cp_entry(
                                   jvm_class_index    clsidx,
                                   jvm_field_index    fldidx,
                                   cp_info_mem_align *atrname);

extern jvm_attribute_index attribute_find_in_field_by_enum(
                                   jvm_class_index  clsidx,
                                   jvm_field_index  fldidx,
                                   rint             atrenum);

extern jvm_attribute_index attribute_find_in_method_by_cp_entry(
                                   jvm_class_index    clsidx,
                                   jvm_method_index   mthidx,
                                   cp_info_mem_align *atrname);

extern jvm_attribute_index attribute_find_in_method_by_enum(
                                   jvm_class_index  clsidx,
                                   jvm_method_index mthidx,
                                   rint             atrenum);

extern jvm_attribute_index attribute_find_in_class_by_cp_entry(
                                   jvm_class_index    clsidx,
                                   cp_info_mem_align *atrname);

extern jvm_attribute_index attribute_find_in_class_by_enum(
                                   jvm_class_index  clsidx,
                                   rint             atrenum);

#endif /* _attribute_h_included_ */

/* EOF */
