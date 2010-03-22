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
#include <assert.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_org_apache_harmony_drlvm_tests_regression_h3404_H3404_mmm
  (JNIEnv *env, jclass that, jint depth)
{
    if (depth > 0)
    {
        jmethodID mid = env->GetStaticMethodID(that, "mmm", "(I)V");
        assert(mid);
        env->CallStaticVoidMethod(that, mid, depth - 1);
    }
    else
    {
        jclass ecl = env->FindClass("org/apache/harmony/drlvm/tests/regression/h3404/MyException");
        assert(ecl);
        env->ThrowNew(ecl, "Crash me");
    }
}

#ifdef __cplusplus
}
#endif
