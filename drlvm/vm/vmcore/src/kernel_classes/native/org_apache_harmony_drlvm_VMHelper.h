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

#include <jni.h>


/* Header for class org.apache.harmony.drlvm.VMHelperFastPath */

#ifndef _ORG_APACHE_HARMONY_DRLVM_VMHELPER_H
#define _ORG_APACHE_HARMONY_DRLVM_VMHELPER_H

#ifdef __cplusplus
extern "C" {
#endif


JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelper_getPointerTypeSize
  (JNIEnv *, jclass);


JNIEXPORT jboolean JNICALL Java_org_apache_harmony_drlvm_VMHelper_isCompressedRefsMode
  (JNIEnv *, jclass);

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_drlvm_VMHelper_isCompressedVTableMode
(JNIEnv *, jclass);


JNIEXPORT jlong JNICALL Java_org_apache_harmony_drlvm_VMHelper_getCompressedModeVTableBaseOffset
  (JNIEnv *, jclass);


JNIEXPORT jlong JNICALL Java_org_apache_harmony_drlvm_VMHelper_getCompressedModeObjectBaseOffset
  (JNIEnv *, jclass);


JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelper_getObjectVtableOffset
  (JNIEnv *, jclass);


JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelper_getClassJLCHanldeOffset
  (JNIEnv *, jclass);

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelper_getVtableClassOffset
  (JNIEnv *, jclass);


#ifdef __cplusplus
}
#endif

#endif /* _ORG_APACHE_HARMONY_DRLVM_VMHELPER_H */
