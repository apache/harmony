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
.class public org/apache/harmony/drlvm/tests/regression/h3446/SubSubTest
.super junit/framework/TestCase
.method public <init>()V
    aload_0
    invokespecial junit/framework/TestCase/<init>()V
    return
.end method

; 
; Subroutine is called from the other subroutine and 
; from the top level code. 
; 
.method public test()V 
   .limit stack 1 
   .limit locals 2 

   jsr LabelSub 
   jsr LabelSubSub 
   return 
LabelSub: 
   astore 1 
   jsr LabelSubSub 
   ret 1 
LabelSubSub: 
   astore 0 
   ret 0 
.end method 

