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

#include <stdlib.h>
#include "lock_manager.h"
#include "open/types.h"
#include "open/vm_type_access.h"
#include "open/vm_method_access.h"

#include "Class.h"
#include "type.h"
#include "environment.h"
#include "stack_iterator.h"
#include "m2n.h"
#include "../m2n_ia32_internal.h"
#include "exceptions.h"
#include "exceptions_jit.h"
#include "jit_intf.h"
#include "jit_intf_cpp.h"
#include "jit_runtime_support.h"
#include "type.h"

#include "encoder.h"

#include "object_layout.h"
#include "nogc.h"

#include "open/gc.h"
 
#include "open/vm_util.h"
#include "vm_threads.h"
#include "ini.h"
#include "type.h"

#include "compile.h"
#include "lil.h"
#include "lil_code_generator.h"

#include "vm_stats.h"
#include "dump.h"

void compile_flush_generated_code_block(U_8*, size_t) {
    // Nothing to do on IA32
}

void compile_flush_generated_code() {
    // Nothing to do on IA32
}

static U_32* get_arg_word(unsigned num_arg_words, unsigned word) {
    return m2n_get_args(m2n_get_last_frame())+num_arg_words-word-1;
}

void compile_protect_arguments(Method_Handle method, GcFrame* gc) {
    assert(!hythread_is_suspend_enabled());
    Method_Signature_Handle msh = method_get_signature(method);

    if (msh == NULL) {
        return;
    }

    assert(msh);
    unsigned num_args = method_args_get_number(msh);
    unsigned num_arg_words = ((Method*)method)->get_num_arg_slots();

    unsigned cur_word = 0;
    for(unsigned i=0; i<num_args; i++) {
        Type_Info_Handle tih = method_args_get_type_info(msh, i);
        bool is_magic = false;  //wjw, MMTk support
        const String *str = tih->get_type_name();

        switch (type_info_get_type(tih)) {
        case VM_DATA_TYPE_INT64:
        case VM_DATA_TYPE_UINT64:
        case VM_DATA_TYPE_F8:
            cur_word += 2;
            break;
        case VM_DATA_TYPE_INT8:
        case VM_DATA_TYPE_UINT8:
        case VM_DATA_TYPE_INT16:
        case VM_DATA_TYPE_UINT16:
        case VM_DATA_TYPE_INT32:
        case VM_DATA_TYPE_UINT32:
        case VM_DATA_TYPE_INTPTR:
        case VM_DATA_TYPE_UINTPTR:
        case VM_DATA_TYPE_F4:
        case VM_DATA_TYPE_BOOLEAN:
        case VM_DATA_TYPE_CHAR:
        case VM_DATA_TYPE_UP:
            cur_word++;
            break;
        case VM_DATA_TYPE_CLASS:
        case VM_DATA_TYPE_ARRAY:
            if (str) {   //wjw MMTk support
                if (strncmp(str->bytes, "org/vmmagic/unboxed/Address", str->len) == 0 ) 
                     is_magic = true;
                if (strncmp(str->bytes, "org/vmmagic/unboxed/Extent",  str->len) == 0 ) 
                     is_magic = true;
                if (strncmp(str->bytes, "org/vmmagic/unboxed/Offset",  str->len) == 0 ) 
                     is_magic = true;
                if (strncmp(str->bytes, "org/vmmagic/unboxed/Word",    str->len) == 0 ) 
                     is_magic = true;
                if (strncmp(str->bytes, "org/vmmagic/unboxed/ObjectReference",    str->len) == 0 ) 
                     is_magic = true;
            }
            if (is_magic == false)
            gc->add_object((ManagedObject**)get_arg_word(num_arg_words, cur_word));
            cur_word++;
            break;
        case VM_DATA_TYPE_MP:
            gc->add_managed_pointer((ManagedPointer*)get_arg_word(num_arg_words, cur_word));
            break;
        case VM_DATA_TYPE_VALUE:
            {
                // This should never cause loading
                Class_Handle UNUSED c = type_info_get_class(tih);
                assert(c);
                LDIE(30, "This functionality is not currently supported");
                break;
            }
        default:
            DIE(("Unexpected data type: %d", type_info_get_type(tih)));
        }
    }
}

void patch_code_with_threads_suspended(U_8* UNREF code_block, U_8* UNREF new_code, size_t UNREF size) {
    LDIE(46, "Not supported on IA32 currently");
}

// Convert a reference on the stack, if null, from a managed null
// (represented by heap_base) to an unmanaged one (NULL/0). Uses %eax.
char * gen_convert_managed_to_unmanaged_null_ia32(char * ss,
                                                  unsigned stack_pointer_offset) {
#ifdef REFS_RUNTIME_OR_COMPRESSED
    REFS_RUNTIME_SWITCH_IF
        ss = mov(ss,  eax_opnd,  M_Base_Opnd(esp_reg, stack_pointer_offset));
        ss = alu(ss, cmp_opc,  eax_opnd,  Imm_Opnd((I_32)VM_Global_State::loader_env->heap_base) );
        ss = branch8(ss, Condition_NE,  Imm_Opnd(size_8, 0));  // not null, branch around the mov 0
        char *backpatch_address__not_managed_null = ((char *)ss) - 1;
        ss = mov(ss,  M_Base_Opnd(esp_reg, stack_pointer_offset),  Imm_Opnd(0));
        signed offset = (signed)ss - (signed)backpatch_address__not_managed_null - 1;
        *backpatch_address__not_managed_null = (char)offset;
    REFS_RUNTIME_SWITCH_ENDIF
#endif // REFS_RUNTIME_OR_COMPRESSED
    return ss;
}


/*    BEGIN COMPILE-ME STUBS    */

static NativeCodePtr compile_get_compile_me_generic() {
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    const int STUB_SIZE = 15 + m2n_push_m2n_size(false, 0) +
        m2n_pop_m2n_size(false, 0, 0, 1);
    char * stub = (char *) malloc_fixed_code_for_jit(STUB_SIZE,
        DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);
    addr = stub;
#ifndef NDEBUG
    memset(stub, 0xcc /*int 3*/, STUB_SIZE);
#endif
    // push m2n to the stack
    stub = m2n_gen_push_m2n(stub, NULL, FRAME_COMPILATION, false, 0);
    // ecx register should contain correct Mehod_Handle
    stub = push(stub, ecx_opnd);
    // compile the method
    stub = call(stub, (char *)&compile_me);
    // remove ecx from the stack
    stub = pop(stub, ecx_opnd);
    // pop m2n from the stack
    stub = m2n_gen_pop_m2n(stub, false, 0, 0, 1);
    // transfer control to the compiled code
    stub = jump(stub, eax_opnd);
    
    assert(stub - (char *)addr <= STUB_SIZE);

    compile_add_dynamic_generated_code_chunk("compile_me_generic", false, addr, STUB_SIZE);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("compile_me_generic", addr, STUB_SIZE);
    }

    DUMP_STUB(addr, "compileme_generic", stub - (char *)addr);

    return addr;
}


NativeCodePtr compile_gen_compile_me(Method_Handle method) {
    const int STUB_SIZE = 16;
#ifdef VM_STATS
    ++VM_Statistics::get_vm_stats().num_compileme_generated;
#endif
    char * stub = (char *) method_get_class(method)->code_alloc(STUB_SIZE, DEFAULT_CODE_ALIGNMENT, CAA_Allocate);
    NativeCodePtr addr = stub; 
#ifndef NDEBUG
    memset(stub, 0xcc /*int 3*/, STUB_SIZE);
#endif

#ifdef VM_STATS
    stub = inc(stub, M_Base_Opnd(n_reg, (I_32)&VM_Statistics::get_vm_stats().num_compileme_used));
#endif
    stub = mov(stub, ecx_opnd, Imm_Opnd((I_32)method));
    stub = jump(stub, (char *)compile_get_compile_me_generic());
    assert(stub - (char *)addr <= STUB_SIZE);

    char* name;
    const char * c = method->get_class()->get_name()->bytes;
    const char * m = method->get_name()->bytes;
    const char * d = method->get_descriptor()->bytes;
    size_t sz = strlen(c) + strlen(m) + strlen(d) + 12;
    name = (char*)STD_MALLOC(sz);
    sprintf(name, "compileme.%s.%s%s", c, m, d);
    compile_add_dynamic_generated_code_chunk(name, true, addr, STUB_SIZE);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event(name, addr, STUB_SIZE);
    }

#ifndef NDEBUG
    static unsigned done = 0;
    // dump first 10 compileme stubs
    if (dump_stubs && ++done <= 10) {
        char * buf;
        const char * c = method->get_class()->get_name()->bytes;
        const char * m = method->get_name()->bytes;
        const char * d = method->get_descriptor()->bytes;
        size_t sz = strlen(c)+strlen(m)+strlen(d)+12;
        buf = (char *)STD_MALLOC(sz);
        sprintf(buf, "compileme.%s.%s%s", c, m, d);
        assert(strlen(buf) < sz);
        DUMP_STUB(addr, buf, stub - (char *)addr);
        STD_FREE(buf);
    }
#endif
    return addr;
} //compile_gen_compile_me
 
/*    END COMPILE-ME STUBS    */

void gen_native_hashcode(Emitter_Handle h, Method *m);
unsigned native_hashcode_fastpath_size(Method *m);
void gen_native_system_currenttimemillis(Emitter_Handle h, Method *m);
unsigned native_getccurrenttime_fastpath_size(Method *m);
void gen_native_readinternal(Emitter_Handle h, Method *m);
unsigned native_readinternal_fastpath_size(Method *m);
void gen_native_newinstance(Emitter_Handle h, Method *m);
unsigned native_newinstance_fastpath_size(Method *m);
// ****** 20031009 above are additions to bring original on par with LIL
void gen_native_getclass_fastpath(Emitter_Handle h, Method *m);
unsigned native_getclass_fastpath_size(Method *m);

void gen_native_arraycopy_fastpath(Emitter_Handle h, Method *m);
unsigned native_arraycopy_fastpath_size(Method *m);

static Stub_Override_Entry _stub_override_entries_base[] = {
    {"java/lang/VMSystem", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", gen_native_arraycopy_fastpath, native_arraycopy_fastpath_size},
    {"java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", gen_native_arraycopy_fastpath, native_arraycopy_fastpath_size},
    {"java/lang/Object", "getClass", "()Ljava/lang/Class;", gen_native_getclass_fastpath, native_getclass_fastpath_size},
    // ****** 20031009 below are additions to bring baseline on par with LIL
    {"java/lang/System", "currentTimeMillis", "()J", gen_native_system_currenttimemillis, native_getccurrenttime_fastpath_size},
    {"java/io/FileInputStream", "readInternal", "(I[BII)I", gen_native_readinternal, native_readinternal_fastpath_size},
#ifndef PLATFORM_POSIX
    // because of threading, this override will not work on Linux!
    {"java/lang/Class", "newInstance", "()Ljava/lang/Object;", gen_native_newinstance, native_newinstance_fastpath_size},
#endif
    {"java/lang/VMSystem", "identityHashCode", "(Ljava/lang/Object;)I", gen_native_hashcode, native_hashcode_fastpath_size}
};

Stub_Override_Entry *stub_override_entries = &(_stub_override_entries_base[0]);

int sizeof_stub_override_entries = sizeof(_stub_override_entries_base) / sizeof(_stub_override_entries_base[0]);


