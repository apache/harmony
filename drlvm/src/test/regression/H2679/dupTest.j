;
;  Licensed to the Apache Software Foundation (ASF) under one or more
;  contributor license agreements.  See the NOTICE file distributed with
;  this work for additional information regarding copyright ownership.
;  The ASF licenses this file to You under the Apache License, Version 2.0
;  (the "License"); you may not use this file except in compliance with
;  the License.  You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
;  Unless required by applicable law or agreed to in writing, software
;  distributed under the License is distributed on an "AS IS" BASIS,
;  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;  See the License for the specific language governing permissions and
;  limitations under the License.
;
.class public org/apache/harmony/drlvm/tests/regression/h2679/DupTest
.super junit/framework/TestCase

.method public <init>()V
    .limit stack 1
    .limit locals 1
    aload_0
    invokespecial junit/framework/TestCase/<init>()V
    return
.end method

.method public testFloat()V
    .limit stack 4
    .limit locals 1
    fconst_1
    ldc 1.0f
    dup
    getstatic java/lang/System/err Ljava/io/PrintStream;
    swap
    invokevirtual java/io/PrintStream/println(F)V
    fconst_0
    invokestatic junit/framework/Assert/assertEquals(FFF)V
    return
.end method

.method public testDouble()V
    .limit stack 8
    .limit locals 1
    dconst_1
    dconst_1
    dup2
    getstatic java/lang/System/err Ljava/io/PrintStream;
    dup_x2
    pop
    invokevirtual java/io/PrintStream/println(D)V
    dconst_0
    invokestatic junit/framework/Assert/assertEquals(DDD)V
    return
.end method
