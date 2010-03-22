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


#include "org_apache_harmony_drlvm_VMHelperFastPath.h"

#include "Class.h"
#include "vtable.h"
#include "open/vm.h"
#include <assert.h>


JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelperFastPath_getVtableIntfTypeOffset(JNIEnv *e, jclass c, jint i)
{
#if defined (_IPF_)
    assert(0);
    return 0;
#else
    assert(i>=0 && i<=2);
    //NOTE: use (jint)(POINTER_SIZE_INT) double cast here to avoid warnings
    if (i==0) return (jint)(POINTER_SIZE_INT)&((VTable*)0)->intfc_class_0;
    if (i==1) return (jint)(POINTER_SIZE_INT)&((VTable*)0)->intfc_class_1;
    return (jint)(POINTER_SIZE_INT)&((VTable*)0)->intfc_class_2;
#endif
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelperFastPath_getVtableIntfTableOffset(JNIEnv *e, jclass c, jint i)
{
#if defined (_IPF_)
    assert(0);
    return 0;
#else

    assert(i>=0 && i<=2);
    if (i==0) return (jint)(POINTER_SIZE_INT)&((VTable*)0)->intfc_table_0;
    if (i==1) return (jint)(POINTER_SIZE_INT)&((VTable*)0)->intfc_table_1;
    return (jint)(POINTER_SIZE_INT)&((VTable*)0)->intfc_table_2;
    return 0;
#endif
}


JNIEXPORT jint JNICALL Java_org_apache_harmony_drlvm_VMHelperFastPath_getVtableSuperclassesOffset(JNIEnv *e, jclass c)
{
    return static_cast<jint>(reinterpret_cast<jlong>(&((VTable*)0)->superclasses));
}
