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


#ifndef _LIL_CODE_GENERATOR_UTILS_H_
#define _LIL_CODE_GENERATOR_UTILS_H_

#include "lil.h"
#include "vm_core_types.h"

// This module provides some common utilities for generating native code from LIL code stubs

//*** The following is for managing label to address translation and back patching

// Patch types have to be the union of the various cases of the various architectures,
// so unfortunately are somewhat platform specific.
typedef enum LilCguPatchType {
    LPT_Rel8, LPT_Rel32, LPT_Abs32
} LilCguPatchType;

typedef struct LilCguPatch {
    char            * addr;
    LilCguPatchType type;
    LilCguPatch     * next;
} LilCguPatch;

typedef struct LilCguLabelAddress {
    LilLabel            l;
    char                * addr;
    bool                base_relative;
    LilCguPatch         * patches;
    LilCguLabelAddress  * next;
}LilCguLabelAddress;


// This class mantains a mapping between labels and addresses, and ensures the references to
// labels eventually point to those labels.
// Clients should call define_label to set a label's address, and patch_to_label to ensure references
// correct point to a label; the class will make sure that a reference points to its label once both
// functions have been called.
class LilCguLabelAddresses {
public:
    // Create a label to address mapping with back patching
    LilCguLabelAddresses(tl::MemoryPool*, char * b);
    //
    void change_base(char * new_base);
    // Label l should have addres code
    void define_label(LilLabel l, void * code, bool base_relative);
    // The contents of address patch_address should point to label l according to patch_type
    void add_patch_to_label(LilLabel l, void * patch_address,  LilCguPatchType patch_type);

private:
    void add_new_label_adress(LilLabel l, void * code, bool base_relative);
    // Apply all patches associated with the current label
    void apply_patches(LilCguLabelAddress * label_adress);
    // Actually apply a patch
    void apply_patch(LilCguLabelAddress * label_adress, LilCguPatch * patch);

    LilCguLabelAddress  * first;
    tl::MemoryPool      * my_mem;
    char                * base;
};

#endif // _LIL_CODE_GENERATOR_UTILS_H_
