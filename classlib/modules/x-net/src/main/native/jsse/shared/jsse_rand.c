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

#include "jsse_rand.h"

#include <stdio.h>
#include "jni.h"
#include "openssl/rand.h"

void randSeed(const void *buf, int num);
int  randBytes(unsigned char *buf, int num);
void randCleanup(void);
void randAdd(const void *buf, int num, double entropy);
int randPseudoBytes(unsigned char *buf, int num);
int randStatus(void);

JavaVM *javaVM;

RAND_METHOD *getRandMethod(JavaVM *jvm) {
    RAND_METHOD *randMethod = malloc(sizeof(RAND_METHOD));
    randMethod->seed = &randSeed;
    randMethod->bytes = &randBytes;
    randMethod->cleanup = &randCleanup;
    randMethod->add = &randAdd;
    randMethod->pseudorand = &randPseudoBytes;
    randMethod->status = &randStatus;

    javaVM = jvm;

    return randMethod;
}

void randSeed(const void *buf, int num) {
    printf("randSeed with num=%d and javaVM=%p\n", num, javaVM);

    //(*javaVM)->GetEnv(javaVM, (void**)&env, JNI_VERSION_1_4);

    return;
}

int randBytes(unsigned char *buf, int num) {
    int i;
    printf("randBytes with num=%d and javaVM=%p\n", num, javaVM);
    for (i=0; i<num; i++) {
        buf[i] = 1;
    }
    return 1;
}

void randCleanup() {
    printf("randCleanup and javaVM=%p\n", javaVM);
    return;
}

void randAdd(const void *buf, int num, double entropy) {
    printf("randAdd with num=%d and entropy=%f and javaVM=%p\n", num, entropy, javaVM);

    return;
}

int  randPseudoBytes(unsigned char *buf, int num) {
    int i;
    printf("randPseudoBytes with num=%d and javaVM=%p\n", num, javaVM);
    for (i=0; i<num; i++) {
        buf[i] = 1;
    }
    return 1;
}

int randStatus() {
    printf("randStatus and javaVM=%p\n", javaVM);
    return 0;
}
