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
.class public org/apache/harmony/drlvm/tests/regression/h788/TestArray
.super java/lang/Object

.method public <init>()V
    aload_0
    invokenonvirtual java/lang/Object/<init>()V
    return
.end method

.method public static TestMultianewarray()V
   
   .limit stack 258
   .limit locals 3

   iconst_1 
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1
   iconst_1

   ; creating array with 255 dimentions
   ; must throw java.lang.VerifyError
   multianewarray [[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[Ljava/lang/String; 255
   astore_2

   return
.end method

