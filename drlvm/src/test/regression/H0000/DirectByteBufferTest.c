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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

JNIEXPORT jstring JNICALL Java_org_apache_harmony_drlvm_tests_regression_h0000_DirectByteBufferTest_tryDirectBuffer
  (JNIEnv *, jclass);

JNIEXPORT jstring JNICALL Java_org_apache_harmony_drlvm_tests_regression_h0000_DirectByteBufferTest_checkSameDirectStorage
(JNIEnv *, jclass, jobject, jobject);


JNIEXPORT jstring JNICALL Java_org_apache_harmony_drlvm_tests_regression_h0000_DirectByteBufferTest_tryDirectBuffer
  (JNIEnv *jenv, jclass unused)
{
    char* error = (char*)calloc(256, 1);
    const jlong BUF_SIZE = 100;
    void* buf = malloc(BUF_SIZE);
    jobject jbuf = (*jenv)->NewDirectByteBuffer(jenv, buf, BUF_SIZE);
    void* addr = (*jenv)->GetDirectBufferAddress(jenv, jbuf);
    jlong size = (*jenv)->GetDirectBufferCapacity(jenv, jbuf);
    jstring jstr;
    if (jbuf) {
        if (addr != buf) {
            sprintf(error, "invalid buffer address: expected %p but was %p\n", buf, addr);
        } 
        if (size != BUF_SIZE) {
            sprintf(error + strlen(error), 
                "invalid buffer capacity: expected %d but was %d\n", BUF_SIZE, size);
        }
    } else {
        // access to direct buffers not supported
        if (addr != NULL | size != -1) {
            sprintf(error, "inconsistent NIO support:\n" 
                "NewDirectByteBuffer() returned NULL;\n"
                "GetDirectBufferAddress() returned %p\n"
                "GetDirectBufferCapacity() returned %d\n", addr, size);
        } else {
            sprintf(error, "no JNI NIO support\n");
        }
    }

    jstr = strlen(error) ? (*jenv)->NewStringUTF(jenv, error) : NULL;
    free(buf);
    free(error);

    return jstr;
}

JNIEXPORT jstring JNICALL Java_org_apache_harmony_drlvm_tests_regression_h0000_DirectByteBufferTest_checkSameDirectStorage
(JNIEnv *jenv, jclass unused, jobject jbuf1, jobject jbuf2)
{
    char* error = (char*)calloc(256, 1);
    void* addr1 = (*jenv)->GetDirectBufferAddress(jenv, jbuf1);
    void* addr2 = (*jenv)->GetDirectBufferAddress(jenv, jbuf2);
    jlong size1 = (*jenv)->GetDirectBufferCapacity(jenv, jbuf1);
    jlong size2 = (*jenv)->GetDirectBufferCapacity(jenv, jbuf2);
    jstring jstr;
    if (addr1 != addr2) {
        sprintf(error, "buffer address is not the same: expected %p but was %p\n", addr1, addr2);
    }
    if (size1 != size2) {
        sprintf(error + strlen(error), 
            "buffer capacity is not the same: expected %d but was %d\n", size1, size2);
    }

    jstr = strlen(error) ? (*jenv)->NewStringUTF(jenv, error) : NULL;
    free(error);

    return jstr;
}
