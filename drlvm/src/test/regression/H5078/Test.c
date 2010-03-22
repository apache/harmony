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

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_drlvm_tests_regression_h5078_Test_funcd
  (JNIEnv *jenv, jobject this, jdouble d1, jdouble d2, jdouble d3, jdouble d4, jdouble d5, jdouble d6, jdouble d7, jdouble d8, jdouble d9)
{
    printf("%f %f %f %f %f %f %f %f %f\n", d1, d2, d3, d4, d5, d6, d7, d8, d9);
    return (d1 == 0.01 &&
            d2 == 0.02 &&
            d3 == 0.03 &&
            d4 == 0.04 &&
            d5 == 0.05 &&
            d6 == 0.06 &&
            d7 == 0.07 &&
            d8 == 0.08 &&
            d9 == 0.09) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_apache_harmony_drlvm_tests_regression_h5078_Test_funcf
  (JNIEnv *jenv, jobject this, jfloat f1, jfloat f2, jfloat f3, jfloat f4, jfloat f5, jfloat f6, jfloat f7, jfloat f8, jfloat f9)
{
    printf("%f %f %f %f %f %f %f %f %f\n", f1, f2, f3, f4, f5, f6, f7, f8, f9);
    return (f1 == 0.1f &&
            f2 == 0.2f &&
            f3 == 0.3f &&
            f4 == 0.4f &&
            f5 == 0.5f &&
            f6 == 0.6f &&
            f7 == 0.7f &&
            f8 == 0.8f &&
            f9 == 0.9f) ? JNI_TRUE : JNI_FALSE;
}

