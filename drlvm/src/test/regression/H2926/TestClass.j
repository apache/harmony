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
.class public org/apache/harmony/drlvm/tests/regression/h2926/TestClass
.super java/lang/Object

.method public <init>()V
   .limit stack 1
   .limit locals 1
   aload_0
   invokenonvirtual java/lang/Object/<init>()V
   return
.end method

.method public f1()V
   .limit stack 2
   .limit locals 2
   .catch org/apache/harmony/drlvm/tests/regression/h2926/MyException from L1 to L2 using L2
L1:
   new org/apache/harmony/drlvm/tests/regression/h2926/MyException
   dup
   invokespecial org/apache/harmony/drlvm/tests/regression/h2926/MyException/<init>()V
   astore_1
   aload_1
   athrow
L2:
   astore_1
   return
.end method

.method public f2()V
   .limit stack 1
   .limit locals 2
   .catch java/lang/NullPointerException from L1 to L2 using L3
L1:
   aconst_null
   astore_1
   aload_1
   invokevirtual java/lang/String/toString()Ljava/lang/String;
   astore_1
L2:
   goto exit
L3:
   astore_1
exit:
   return
.end method

.method public f3()V
   .limit stack 1
   .limit locals 2
   .catch org/apache/harmony/drlvm/tests/regression/h2926/MyException from L1 to L2 using L3
L1:
   aload_0
   invokespecial org/apache/harmony/drlvm/tests/regression/h2926/TestClass/f5()V
L2:
   goto exit
L3:
   astore_1
exit:
   return
.end method

.method public f4()V
   .limit stack 1
   .limit locals 2
   .catch java/lang/NullPointerException from L1 to L2 using L3
L1:
   aload_0
   invokespecial org/apache/harmony/drlvm/tests/regression/h2926/TestClass/f6()V
L2:
   goto exit
L3:
   astore_1
exit:
   return
.end method

.method public f5()V
   .limit stack 2
   .limit locals 2
   .throws org/apache/harmony/drlvm/tests/regression/h2926/MyException
   new org/apache/harmony/drlvm/tests/regression/h2926/MyException
   dup
   invokespecial org/apache/harmony/drlvm/tests/regression/h2926/MyException/<init>()V
   astore_1
   aload_1
   athrow
.end method

.method public f6()V
   .limit stack 1
   .limit locals 2
   aconst_null
   astore_1
   aload_1
   invokevirtual java/lang/String/toString()Ljava.lang.String;
   astore_1
   return
.end method
