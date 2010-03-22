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
#include "org_apache_harmony_drlvm_VMHelper.h"

#include "open/vm.h"
#include "open/vm_ee.h"
#include "open/vm_util.h"
#include "environment.h"
#include "vtable.h"
#include <assert.h>

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelper_getPointerTypeSize (JNIEnv *, jclass) {
    return (jint)sizeof(void*);
}


JNIEXPORT jboolean JNICALL Java_org_apache_harmony_drlvm_VMHelper_isCompressedRefsMode(JNIEnv *, jclass) {
    return (jboolean)REFS_IS_COMPRESSED_MODE;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_drlvm_VMHelper_isCompressedVTableMode(JNIEnv *, jclass) {
#ifdef USE_COMPRESSED_VTABLE_POINTERS
    return true;
#else
    return false;
#endif
}


/** @return vtable base offset if is in compressed-refs mode or -1*/
JNIEXPORT jlong JNICALL Java_org_apache_harmony_drlvm_VMHelper_getCompressedModeVTableBaseOffset(JNIEnv *, jclass) {
#ifdef USE_COMPRESSED_VTABLE_POINTERS
    return (jlong)vm_get_vtable_base_address();
#else
    return -1;
#endif
}


/** @return object base offset if is in compressed-refs mode or -1*/
JNIEXPORT jlong JNICALL Java_org_apache_harmony_drlvm_VMHelper_getCompressedModeObjectBaseOffset(JNIEnv *, jclass) {
    if (REFS_IS_COMPRESSED_MODE) {
        return (jlong)(POINTER_SIZE_INT)REF_MANAGED_NULL;
    } else {
        return -1;
    }
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelper_getObjectVtableOffset(JNIEnv *e, jclass c)
{
    return object_get_vtable_offset();
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelper_getClassJLCHanldeOffset(JNIEnv *e, jclass c)
{
   return (jint)Class::get_offset_of_jlc_handle();
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelper_getVtableClassOffset(JNIEnv *e, jclass c)
{
    return static_cast<jint>(reinterpret_cast<jlong>(&((VTable*)0)->clss));
}




