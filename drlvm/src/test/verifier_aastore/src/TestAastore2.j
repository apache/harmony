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
.class public TestAastore2
.super java/lang/Object
.field public testField [LTestInterface;

.method public <init>()V
   aload_0
   invokespecial java/lang/Object/<init>()V
   return
.end method

.method public test()V
   .limit stack 3
   .limit locals 2

   aload_0
   getfield TestAastore2/testField [LTestInterface;
   astore_1

   sipush 1
   sipush 1
   multianewarray [[LTestAastore; 2
   sipush 0

   ; target of a aastore instruction not assignment compatible
   ; with the class type specified in the instruction
   aload_1
   aastore

  return
.end method
