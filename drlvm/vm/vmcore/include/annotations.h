/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/** 
 * @author Alexey V. Varlamov
 */  
/**
 * @file
 * @brief Declaration of main resolution routines for Java annotations.
 */

#ifndef _ANNOTATIONS_H_
#define _ANNOTATIONS_H_

#include "jni_types.h"
#include "vm_core_types.h"

struct AnnotationTable;
struct Annotation;
struct AnnotationValue;

/**
 * Returns common array of visible and invisible annotations.
 * @note invisible annotations are present only if -Xinvisible command line flag is set
 * An annotation which cannot be resolved (i.e. it's type cannot be loaded) 
 * is silently ignored, per JSR-175.
 * Returns zero-sized array if there are no (resolved) annotations.
 * May raise an exception, in this case returns null.
 *
 * @param jenv - JNI interface 
 * @param table - annotations to resolve
 * @param inv_table - invisible annotations to resolve
 * @param clss - an "owner" class, which is annotated or whose member is annotated
 * @return - array of resolved annotations or null
 */
jobjectArray get_annotations(JNIEnv* jenv, AnnotationTable* table, AnnotationTable *inv_table, Class* clss);

/**
 * Returns resolved annotation or null if resolution failed.
 * If the cause parameter is not null, resolution error is assigned to it for upstream processing;
 * otherwise the error is raised.
 * In case of errors other than resolving failure, the error is raised, 
 * null is returned and cause is unchanged
 *
 * @param jenv - JNI interface 
 * @param antn - the annotation to be resolved
 * @param clss - an "owner" class, which is annotated or whose member is annotated
 * @param cause - out-parameter to return resolution failure (if any)
 * @return - resolved annotation or null if resolution failed
 */
jobject resolve_annotation(JNIEnv* jenv, Annotation* antn, Class* clss, jthrowable* cause = NULL);

/**
 * Returns resolved annotation value or null if resolution failed.
 * In case of a resolution failure, the error is assigned to the "cause" parameter 
 * for upstream processing.
 * In case of errors other than resolving failure, the error is raised, 
 * null is returned and cause is unchanged
 *
 * @param jenv - JNI interface pointer
 * @param clss - an "owner" class, which is annotated or whose member is annotated
 * @param antn - the value to be resolved
 * @param antn_clss - annotation type
 * @param name - the name of an annotation member which holds the value
 * @param cause - out-parameter to return resolution failure (if any)
 * @return - resolved annotation value or null if resolution failed
 */
jobject resolve_annotation_value(JNIEnv* jenv, Class* clss, AnnotationValue& antn, Class* antn_clss, 
                                 String* name, jthrowable* cause);

#endif // !_ANNOTATIONS_H_
