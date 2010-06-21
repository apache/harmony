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

/**
 * @author Intel, Mikhail Y. Fursov
 *
 */

#ifndef _JITRINO_H_
#define _JITRINO_H_

#include "VMInterface.h"
#include "Stl.h"

namespace Jitrino {

// assert which works even in release mode
// prints message, something about line and file, and hard-exits
#define jitrino_assert(e) { if (!(e)) { crash("Assertion failed at %s:%d", __FILE__ ,__LINE__); } }

void crash (const char* fmt, ...);

class MemoryManager;
class RuntimeInterface;
class ProfilingInterface;

class JITInstanceContext;
typedef StlVector<JITInstanceContext*> JITInstances;

class Jitrino {
public:
    static void crash (const char* msg);
    static bool Init(JIT_Handle jit, const char* name);
    static void DeInit(JIT_Handle jit);
    static bool  CompileMethod(CompilationContext* compilationContext);
    static RuntimeInterface* getRuntimeInterface() {return runtimeInterface;}
    static MemoryManager& getGlobalMM() { return *global_mm; }

    enum Backend {
        CG_IPF,
        CG_IA32,
    };

    struct Flags {
        bool skip;
        Backend codegen;
        bool time;
    };
    // Global Jitrino Flags (are set on initialization, not modified at runtime)
    static struct Flags flags;
    static JITInstanceContext* getJITInstanceContext(JIT_Handle jitHandle);
    static void killJITInstanceContext(JITInstanceContext* jit);
    
    static int  getCompilationRecursionLevel();
    static void incCompilationRecursionLevel();
    static void decCompilationRecursionLevel();

private:
    static MemoryManager *global_mm; 
    static RuntimeInterface* runtimeInterface;
    
    static JITInstances* jitInstances;
};


} //namespace Jitrino 

#endif // _JITRINO_H_
