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
#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include "lil.h"
#include "lil_code_generator_utils.h"
#include "tl/memory_pool.h"

LilCguLabelAddresses::LilCguLabelAddresses(tl::MemoryPool* mem, char * init_base):
first(NULL), my_mem(mem), base(init_base) {}

void LilCguLabelAddresses::change_base(char * new_base) {
    LilCguLabelAddress * cur_label_addr;
    LilCguPatch * cur_patch;
    for (cur_label_addr = first; cur_label_addr != NULL; cur_label_addr = cur_label_addr->next) {
        if (cur_label_addr->base_relative) {
            // all patch addresses are sensitive to the base address
            // since label address is sensitive to the base as well
            // we don't need to patch the code. just remember new addresses
            cur_label_addr->addr += new_base - base;
            for (cur_patch = cur_label_addr->patches; cur_patch != NULL; cur_patch = cur_patch->next) {
                cur_patch->addr += new_base - base;
            }
        } else {
            //need to patch the code
            for (cur_patch = cur_label_addr->patches; cur_patch != NULL; cur_patch = cur_patch->next) {
                cur_patch->addr += new_base - base;
                apply_patch(cur_label_addr, cur_patch);
            }
        }
    }
    base = new_base;
}

// when base_relative is true it means that label address should be recalculated if base is changed
void LilCguLabelAddresses::define_label(LilLabel l, void * code, bool base_relative) {
    LilCguLabelAddress * cur = first;
    while (cur != NULL) {
        if (strcmp(cur->l, l) == 0) {
            if (cur->addr == NULL) {
                // not defined
                cur->addr = (char *)code;
                cur->base_relative = base_relative;
                apply_patches(cur);
            }
#ifndef NDEBUG
            else {
                // such label has already been defined
                // check that they are consistent
                assert(base_relative == cur->base_relative && cur->addr == code);
            }
#endif
            return;
        }
        cur = cur->next;
    }
    // need to create new label address
    add_new_label_adress(l, code, base_relative);
}

void LilCguLabelAddresses::add_patch_to_label(LilLabel l, void * patch_address, LilCguPatchType patch_type) {
    LilCguLabelAddress * cur = first;
    // try to find existing label address
    while (cur != NULL && strcmp(cur->l, l) != 0) {
        cur = cur->next;
    }

    // create new label address if not found
    if (!cur) {
        add_new_label_adress(l, NULL, false);
        cur = first;
    }

    // add new patch
    LilCguPatch * p = (LilCguPatch*)my_mem->alloc(sizeof(LilCguPatch));
    p->addr = (char *)patch_address;
    p->type = patch_type;
    p->next = cur->patches;
    cur->patches = p;

    // apply patch if label defined
    if (cur->addr != NULL) {
        apply_patch(cur, p);
    }
}

void LilCguLabelAddresses::apply_patches(LilCguLabelAddress * label_adress) {
    for(LilCguPatch * p = label_adress->patches; p != NULL; p = p->next) {
        apply_patch(label_adress, p);
    }
}

void LilCguLabelAddresses::apply_patch(LilCguLabelAddress * label_adress, LilCguPatch * patch) {
    int64 diff;
    switch (patch->type) {
    case LPT_Rel8:
        diff = (int64)((char *)label_adress->addr - ((char *)patch->addr + 1));
        assert(diff == (int64)(I_8)diff);
        *(I_8*)patch->addr = (I_8)diff;
        break;
    case LPT_Rel32:
        diff = (int64)((char *)label_adress->addr - (char *)((I_32 *)patch->addr + 1));
        assert(diff == (int64)(I_32)diff);
        *(I_32*)patch->addr = (I_32)diff;
        break;
    case LPT_Abs32:
        assert((POINTER_SIZE_INT)label_adress->addr <= 0xFFFFffff);
        *(I_32*)patch->addr = (I_32)(POINTER_SIZE_INT)label_adress->addr;
        break;
    default:
        DIE(("Unknown patch typ"));
    }
}

void LilCguLabelAddresses::add_new_label_adress(LilLabel l, void * code, bool base_relative) {
    LilCguLabelAddress * cur = (LilCguLabelAddress*)my_mem->alloc(sizeof(LilCguLabelAddress));
    cur->l = l;
    cur->addr = (char *)code;
    cur->base_relative = base_relative;
    cur->patches = NULL;
    cur->next = first;
    first = cur;
}
