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
 
#include <open/vm_gc.h>
#include <jni.h>
#include "open/vm_util.h"
#include "environment.h"
#include "../thread/gc_thread.h"
#include "../gen/gen.h"
#include "java_support.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     org_apache_harmony_drlvm_gc_gen_GCHelper
 * Method:    TLSFreeOffset
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_TLSGCOffset(JNIEnv *e, jclass c)
{
    return (jint)tls_gc_offset;
}

JNIEXPORT jobject JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getNosBoundary(JNIEnv *e, jclass c)
{
    return (jobject)nos_boundary;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getGenMode(JNIEnv *e, jclass c)
{
    return (jboolean)gc_is_gen_mode();
}

JNIEXPORT void JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_helperCallback(JNIEnv *e, jclass c)
{
    java_helper_inlined = TRUE;

    POINTER_SIZE_INT obj = *(POINTER_SIZE_INT*)c;
    /* a trick to get the GCHelper_class j.l.c in order to manipulate its 
       fields in GC native code */ 
    Class_Handle *vm_class_ptr = (Class_Handle *)(obj + VM_Global_State::loader_env->vm_class_offset);
    GCHelper_clss = *vm_class_ptr;
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getZeroingSize(JNIEnv *e, jclass c)
{
#if defined(ALLOC_ZEROING) && defined(ALLOC_PREFETCH)
    return (jint)ZEROING_SIZE;
#else
    return (jint)0;
#endif
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getPrefetchDist(JNIEnv *e, jclass c)
{
#if defined(ALLOC_ZEROING) && defined(ALLOC_PREFETCH)
    return (jint)PREFETCH_DISTANCE;
#else
    return (jint)0;
#endif
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getPrefetchStride(JNIEnv *e, jclass c)
{
#if defined(ALLOC_ZEROING) && defined(ALLOC_PREFETCH)
    return (jint)PREFETCH_STRIDE;
#else
    return (jint)0;
#endif
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_isPrefetchEnabled(JNIEnv *, jclass) 
{
#if defined(ALLOC_ZEROING) && defined(ALLOC_PREFETCH)
   return (jboolean) PREFETCH_ENABLED;
#else
    return (jboolean)JNI_FALSE;
#endif
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getTlaFreeOffset(JNIEnv *, jclass) 
{
    return (jint)((POINTER_SIZE_INT) &(((Allocator*)0)->free));
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getTlaCeilingOffset(JNIEnv *, jclass) 
{
    return (jint)((POINTER_SIZE_INT) &(((Allocator*)0)->ceiling));
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getTlaEndOffset(JNIEnv *, jclass) 
{
    return (jint)((POINTER_SIZE_INT) &(((Allocator*)0)->end));
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getGCObjectAlignment(JNIEnv *, jclass) 
{
   return (jint) GC_OBJECT_ALIGNMENT;
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getLargeObjectSize(JNIEnv *, jclass) 
{
   return (jint) GC_LOS_OBJ_SIZE_THRESHOLD;
}

#define OFFSET(structure, member)  ((int)(POINTER_SIZE_INT) &((structure *)0)->member)

JNIEXPORT jlong JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getVTBase(JNIEnv *e, jclass c) 
{
  return (jlong)vtable_base;
}
JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getArrayElemSizeOffsetInGCVT(JNIEnv *e, jclass c) 
{
  return (jint)OFFSET(GC_VTable_Info,array_elem_size);
}
JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getArrayFirstElemOffsetInGCVT(JNIEnv *e, jclass c) 
{
  return (jint)OFFSET(GC_VTable_Info,array_first_elem_offset);
}
JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_gc_1gen_GCHelper_getGCAllocatedSizeOffsetInGCVT(JNIEnv *e, jclass c) 
{
  return (jint)OFFSET(GC_VTable_Info,gc_allocated_size);
}

#ifdef __cplusplus
}
#endif
