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
 * @author Intel, Evgueni Brevnov, Ivan Volosyuk
 */  


#ifndef _LIL_CODE_GENERATOR_H_
#define _LIL_CODE_GENERATOR_H_

#include "lil.h"
#include "vm_core_types.h"
#include "environment.h"
#include "mem_alloc.h"

// This is an abstract base case for LIL code generators
// Subclasses compile LIL into native code for a particular
// architecture

// Note that this is a compiler algorithm abstraction not
// a compilation process abstraction.  Subclasses should
// not store state about any particular compilation, only
// options that configure the compilation.

class LilCodeGenerator {
public:
    // Return the code generator for the current platform
    static LilCodeGenerator* get_platform();

    // Compile LIL code stub to native code and return it
    // The stub_name is for vtune support
    // Dump an ascii version of the compiled stub to stdout if dump_stub
    // If cs_stats is nonnull add the number of bytes of the compiled code to *cs_stats
    NativeCodePtr compile(LilCodeStub* cs, PoolManager* code_pool = 
        VM_Global_State::loader_env->GlobalCodeMemoryManager);

protected:
    LilCodeGenerator();

    // allocates a chunk of memory for a LIL stub; the user-provided function
    // compile_main() should call this function instead of allocating memory
    // directly.
    NativeCodePtr allocate_memory(size_t, PoolManager*);

    // generates compiled code for a LIL stub, and returns its address.  The
    // size of the compiled stub is placed in stub_size.  Called by the
    // compile() function to do most of the work.
    //
    // Each subclass of LilCodeGenerator should provide a platform-dependent
    // implementation of compile_main().  The memory area that holds the
    // compiled code should be allocated by calling allocate_memory().
    virtual NativeCodePtr compile_main(LilCodeStub* cs, size_t* stub_size, PoolManager* code_pool) = 0;
};

#endif // _LIL_CODE_GENERATOR_H_
