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
.class public org/apache/harmony/drlvm/tests/regression/h3225/J9CompatibleJsrTest
.super junit/framework/TestCase
.method public <init>()V
    aload_0
    invokespecial junit/framework/TestCase/<init>()V
    return
.end method

;
; Launches testcases which check subroutine verification.
;
.method public static main([Ljava/lang/String;)V
    .limit stack 1
    .limit locals 1

    ldc "org.apache.harmony.drlvm.tests.regression.h3225.J9CompatibleJsrTest"
    invokestatic java/lang/Class/forName(Ljava/lang/String;)Ljava/lang.Class;
    invokestatic junit/textui/TestRunner/run(Ljava/lang/Class;)V

    return
.end method

;
; A subroutine is called from two different subroutines. RI produce
; a shameful error message in this case.
;
.method public testNestedSubs()V
    .limit stack 3
    .limit locals 2

    jsr LabelSub1
    jsr LabelSub2
    return

LabelSub1:
    astore 1
    jsr LabelSub
    ret 1

LabelSub2:
    astore 1
    jsr LabelSub
    ret 1

LabelSub:
    astore 0
    ret 0

.end method

;
; A subroutine splits execution and calls another subroutine from
; different branches.
;
.method public testSubSplit()V
    .limit stack 1
    .limit locals 2

    jsr LabelSub
    return

LabelSub:
    astore 1
    aconst_null
    ifnull Label1
    jsr LabelSub1
Label1:
    jsr LabelSub1
Label2:
    jsr LabelSub2
    aconst_null
    ifnonnull Label2
    jsr LabelSub2
    goto Label4
Label3:
    jsr LabelSub3
    ret 1
Label4:
    aconst_null
    ifnull Label3
    jsr LabelSub3
    aconst_null
    ifnull Label3
    return

LabelSub1:
    astore 0
    ret 0

LabelSub2:
    astore 0
    ret 0

LabelSub3:
    astore 0
    ret 0

.end method

;
; A nested subroutine is preceded with two calls to another subroutine.
;
.method public testNestedPreceded()V
    .limit stack 4
    .limit locals 1

    iconst_0
    jsr LabelSub
    return

LabelSub:
    swap
    jsr LabelSub1
    jsr LabelSub1
    jsr LabelSub2
    swap
    astore 0
    ret 0

LabelSub1:
    swap
    iconst_1
    iadd
    swap
    astore 0
    ret 0

LabelSub2:
    swap
    iconst_1
    iadd
    swap
    astore 0
    ret 0

.end method

