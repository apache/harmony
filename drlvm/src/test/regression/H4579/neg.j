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
.class public org/apache/harmony/drlvm/tests/regression/h4579/neg
.super java.security.SecureClassLoader
.method public <init>()V
   aload_0
   invokespecial java.security.SecureClassLoader/<init>()V
   return
.end method

;
; super class (SecureClassLoader) is loaded by different (bootstrap) classloader than the current class
; super of the super class (ClassLoader) has protected method
; try to invoke it with invokevirtual
;
;
.method public static test()V
   ; obtain some classloader
   ldc	"org.apache.harmony.drlvm.tests.regression.h4579.neg"
   invokestatic	java/lang/Class/forName(Ljava/lang/String;)Ljava/lang/Class;

   invokevirtual java/lang/Class/getClassLoader()Ljava/lang/ClassLoader;

   ; try to invoke its protected method
   invokevirtual java/lang/ClassLoader/getPackages()[Ljava/lang/Package;
   return
.end method


.method public static main([Ljava/lang/String;)V
   invokestatic	org/apache/harmony/drlvm/tests/regression/h4579/neg/test()V
   return
.end method


