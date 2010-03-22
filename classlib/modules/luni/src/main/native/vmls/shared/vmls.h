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

#ifndef vmls_h
#define vmls_h

void   initializeVMLocalStorage PROTOTYPE((JavaVM * vm));
void   freeVMLocalStorage PROTOTYPE((JavaVM * vm));
UDATA  JNICALL HyVMLSAllocKeys PROTOTYPE((JNIEnv * env, UDATA * pInitCount, ...));
void * JNICALL HyVMLSSet PROTOTYPE((JNIEnv * env, void ** pKey, void * value));
void * JNICALL HyVMLSGet PROTOTYPE((JNIEnv * env, void * key));
void   JNICALL HyVMLSFreeKeys PROTOTYPE((JNIEnv * env, UDATA * pInitCount, ...));

#endif     /* vmls_h */
