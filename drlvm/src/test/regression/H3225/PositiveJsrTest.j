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
.class public org/apache/harmony/drlvm/tests/regression/h3225/PositiveJsrTest
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

    ldc "org.apache.harmony.drlvm.tests.regression.h3225.PositiveJsrTest"
    invokestatic java/lang/Class/forName(Ljava/lang/String;)Ljava/lang.Class;
    invokestatic junit/textui/TestRunner/run(Ljava/lang/Class;)V

    return
.end method

;
; Minimal number of locals is one since
; one passes a class reference.
;
.method public testMinimalLimits()V
    .limit stack 0
    .limit locals 1

    return
.end method

;
; A subroutine call can be the last instruction,
; when the subroutine doesn't return.
;
.method public testLastJsr()V
    .limit stack 1
    .limit locals 1

    goto LabelEndMethod
LabelReturn:
    return
LabelEndMethod:
    jsr LabelReturn
.end method

;
; Calls merge execution into common return instruction.
;
.method public testCommonReturn()V 
    .limit stack 1 
    .limit locals 1

    aconst_null
    ifnull LabelCodeBranch
    jsr LabelSub1

LabelCodeBranch:
    jsr LabelSub2

LabelSub1:
    astore 0
    goto LabelCommonPart

LabelSub2:
    astore 0
    goto LabelCommonPart

LabelCommonPart:
    return
    
.end method

;
; Multiple calls to one subroutine.
;
.method public testMultipleCalls()V
    .limit stack 1
    .limit locals 1

    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    return
LabelSub:
    astore 0
    ret 0
.end method

;
; A subroutine is called from another subroutine twice.
;
.method public testNestedSubs()V
    .limit stack 1
    .limit locals 2

    jsr LabelSub
    jsr LabelSub
    return

LabelSub:
    astore 0
    jsr LabelSubSub
    ret 0

LabelSubSub:
    astore 1
    ret 1

.end method

;
; A subroutine is called from the exception handler of
; the other subroutine.
;
.method public testCallFromHandler()V
    .limit stack 1
    .limit locals 2

    jsr LabelSub
    jsr LabelSub
    return

LabelSub:
    astore 0
    jsr LabelSubSub
LabelStartHandler:
    jsr LabelSubSub
LabelEndHandler:
    ret 0

LabelSubSub:
    astore 1
    ret 1

LabelHandler:
    pop
    jsr LabelSubSub
    return

.catch all from LabelStartHandler to LabelEndHandler using LabelHandler
.end method


;
; Subroutine contains different branches.
;
.method public testBranches()V
    .limit stack 1
    .limit locals 2

    aconst_null
    astore_0
    jsr LabelSub
    aload_0
    pop
    iconst_0
    istore_0
    jsr LabelSub
    iload_0
    return

LabelSub:
    astore 1
LabelBranch:
    aconst_null
    ifnonnull LabelBranch
    aconst_null
    ifnull LabelRet
    goto LabelBranch
LabelRet:
    ret 1

.end method

;
; A subroutine graph contains several unreachable nodes.
;
.method public testUnreachableNodes()V
    .limit stack 1
    .limit locals 1

    return
LabelBackward:
    aconst_null
    ifnull LabelForward
    aconst_null
    ifnull LabelBackward
LabelForward:
    aconst_null
    ifnull LabelBackward
    jsr LabelBackward
.end method

;
; A subroutine is called from another subroutine nine times.
;
.method public testNineNestedSubs()V
    .limit stack 3
    .limit locals 1

    iconst_0
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    jsr LabelSub
    bipush 81
    swap
    invokestatic org/apache/harmony/drlvm/tests/regression/h3225/PositiveJsrTest/assertEquals(II)V
    return

LabelSub:
    swap
    jsr LabelSubSub
    jsr LabelSubSub
    jsr LabelSubSub
    jsr LabelSubSub
    jsr LabelSubSub
    jsr LabelSubSub
    jsr LabelSubSub
    jsr LabelSubSub
    jsr LabelSubSub
    swap
    astore 0
    ret 0

LabelSubSub:
    astore 0
    iconst_1
    iadd
    ret 0

.end method

;
; Calls one subroutine after another in the subroutine context.
;
.method public testSubAfterSub()V
    .limit stack 1
    .limit locals 2

    jsr LabelSub1
    return

LabelSub1:
    astore 0
    jsr LabelSub2
    jsr LabelSub2
    jsr LabelSub3
    jsr LabelSub3
    ret 0

LabelSub2:
    astore 1
    ret 1

LabelSub3:
    astore 1
    ret 1

.end method

;
; An exception range ends at the end of the method.
;
.method public testWideExceptionRange()V
    .limit stack 1
    .limit locals 1

    jsr LabelSub
    ldc "Constant"
LabelStart:
    return

LabelSub:
    astore 0
    ret 0
LabelEnd:
.catch all from LabelSub to LabelEnd using LabelStart

.end method

