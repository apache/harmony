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
#ifndef _VM_CORE_TYPES_H_
#define _VM_CORE_TYPES_H_

#include "open/types.h"

// This file names all types commonly used in the VM.
// It defines those types that have no other logical place, other types are defined in header files appropriate to them.

struct String;
struct Class;
class Class_Member;
struct Field;
struct Method;
struct ClassLoader;
class  BootstrapClassLoader;
class  UserDefinedClassLoader;
class  ClassTable;
class  CodeChunkInfo;
class  GcFrame;
struct Intfc_Table;
struct LilCodeStub;
struct M2nFrame;
struct ManagedObject;
class  NativeObjectHandles;
struct VM_thread;
class  Package;
class  Package_Table;
class  Properties;
struct StackIterator;
struct TypeDesc;

typedef void         (*GenericFunctionPointer)();
typedef LilCodeStub* (*NativeStubOverride)(LilCodeStub*, Method_Handle);


#endif //!_VM_CORE_TYPES_H_

