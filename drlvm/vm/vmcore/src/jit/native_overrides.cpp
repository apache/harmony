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

#include "vtable.h"
#include "Class.h"
#include "environment.h"
#include "lil.h"
#include "native_overrides.h"
#include "nogc.h"
#include "object_generic.h"
#include "object_layout.h"
#include "open/types.h"
#include "open/vm_util.h"
#include "open/vm_ee.h"
#include "open/vm.h"

// *** This is for readInternal override
#include "jni_utils.h"
#include "vm_arrays.h"
#include "open/vm_util.h"
#include <fcntl.h>
#ifdef PLATFORM_NT
#include <io.h>
#include <direct.h>
#elif defined(PLATFORM_POSIX)
#include <unistd.h>
#endif

#include <errno.h>

// *** This is for currenttimemillis override
#include "platform_core_natives.h"

// *** This is for the newInstance override
#include "exceptions.h"

#include "dump.h"

int readinternal_override_lil(JNIEnv *jenv,
                              Java_java_io_FileInputStream * UNREF pThis,
                              int fd,
                              Vector_Handle pArrayOfByte,
                              int  offset,
                              int len)
{
    // Read into byte array "count" bytes starting at index "offset"
    
    // Check if we have been passed a null pointer
    if (!pArrayOfByte) {
        throw_exception_from_jni(jenv, "java/lang/NullPointerException", 
            "Null pointer passed to read()");
        return 0;
    }
    
    int array_len = jenv->GetArrayLength((jbyteArray)(&pArrayOfByte));
    if (len < 0 || offset < 0 || offset + len > array_len) {
        throw_exception_from_jni(jenv, "java/lang/ArrayIndexOutOfBoundsException", 
            "Index check failed");
        return 0;
    }
    
    // Get the array reference.
    void *bufp = get_vector_element_address_int8(pArrayOfByte, offset);
    int ret = read (fd, bufp, len);
    
    if (ret == 0) {
        return -1;
    }
    
    // No "interrupted" check for now.
    if (ret == -1) {
        // Throw IOException since read failed somehow.. use strerror(errno)
        throw_exception_from_jni(jenv, "java/io/IOException", 0);
        return 0;
    }
    assert(ret >= 0);
    
    return ret;
} // readinternal_override_lil

LilCodeStub* nso_readinternal(LilCodeStub* cs, Method_Handle)
{
    cs = lil_parse_onto_end(cs, "out platform:pint,ref,g4,ref,g4,g4:g4; o0=%0i;",
        p_TLS_vmthread->jni_env);
    cs = lil_parse_onto_end(cs, "o1=i0; o2=i1; o3=i2; o4=i3; o5=i4;");
    cs = lil_parse_onto_end(cs, "call %0i; ret;", readinternal_override_lil);
    assert(cs);
    return cs;
} // nso_readinternal

LilCodeStub* nso_system_currenttimemillis(LilCodeStub* cs, Method_Handle)
{
    cs = lil_parse_onto_end(cs, "out platform::g8;");
    cs = lil_parse_onto_end(cs, "call %0i; ret;", get_current_time);
    assert(cs);
    return cs;
} // nso_system_currenttimemillis

LilCodeStub* nso_newinstance(LilCodeStub* cs, Method_Handle)
{
    Global_Env *env = VM_Global_State::loader_env;
    assert(env->vm_class_offset != 0);
    unsigned current_offset = 1;
    unsigned limit_offset = 1;
    
    if(gc_supports_frontier_allocation(&current_offset, &limit_offset)){
        cs = lil_parse_onto_end(cs, "locals 4;");

        // Get Class_Handle from java.lang.Class object
        cs = lil_parse_onto_end(cs, "ld l0,[i0+%0i:pint];", (POINTER_SIZE_INT)env->vm_class_offset);
        
        // Determine if this class supports fast allocation
        size_t offset_is_fast_allocation_possible = env->Void_Class->get_offset_of_fast_allocation_flag();
        cs = lil_parse_onto_end(cs, "ld l1,[l0+%0i:g1];", offset_is_fast_allocation_possible);
        cs = lil_parse_onto_end(cs, "jc l1=0,fallback;");

        // Class supports fast allocation, now use frontier allocation technique
        size_t offset_gc_local           = (U_8*)&(p_TLS_vmthread->_gc_private_information) - (U_8*)p_TLS_vmthread;
        size_t offset_allocation_handle  = env->Void_Class->get_offset_of_allocation_handle();
        size_t offset_instance_data_size = env->Void_Class->get_offset_of_instance_data_size();
        current_offset += (unsigned) offset_gc_local;
        limit_offset += (unsigned) offset_gc_local;

        // Get frontier into r, limit into l2, instance size into l3
        cs = lil_parse_onto_end(cs, "l1=ts;");
        cs = lil_parse_onto_end(cs, "ld r,[l1+%0i:ref];", (POINTER_SIZE_INT)current_offset);
        cs = lil_parse_onto_end(cs, "ld l2,[l1+%0i:pint];", (POINTER_SIZE_INT)limit_offset);
        cs = lil_parse_onto_end(cs, "ld l3,[l0+%0i:g4];", offset_instance_data_size);

        // Compute new frontier
        cs = lil_parse_onto_end(cs, "l3=l3:pint+r:pint;");
        // Is it within limit?
        cs = lil_parse_onto_end(cs, "jc l2<l3,fallback;");

        // Yes, store new frontier, initialize vtable
        cs = lil_parse_onto_end(cs, "st [l1+%0i:pint],l3;", (POINTER_SIZE_INT)current_offset);
        cs = lil_parse_onto_end(cs, "ld l3,[l0+%0i:pint];", offset_allocation_handle);
        cs = lil_parse_onto_end(cs, "l2=r; st [l2+0:pint],l3;");

        // Done
        cs = lil_parse_onto_end(cs, "ret; :fallback;");
    }
    
    assert(cs);
    return cs;

} // nso_newinstance

static ManagedObject* get_class_ptr(ManagedObject* obj)
{
    return *(obj->vt()->clss->get_class_handle());
}

LilCodeStub* nso_get_class(LilCodeStub* cs, Method_Handle)
{
    cs = lil_parse_onto_end(cs, "in2out platform:ref; call %0i; jc r=0,null; ret; :null;", get_class_ptr);
    assert(cs);
    return cs;
}

LilCodeStub* nso_array_copy(LilCodeStub* cs, Method_Handle)
{
    ArrayCopyResult (*p_array_copy)(ManagedObject* src, I_32 src_off, ManagedObject* dst, I_32 dst_off, I_32 count);
    p_array_copy = array_copy;
    cs = lil_parse_onto_end(cs,
        "in2out platform:g4;"
        "call %0i;"
        "jc r!=%1i,bad;"
        "ret;"
        ":bad;",
        p_array_copy, (POINTER_SIZE_INT)ACR_Okay);
    return cs;
}

// The following override is only for IA32

#ifdef _IA32_

#include "encoder.h"

#define MOV_PAIR 8

static NativeCodePtr fast_array_copy()
{
    static NativeCodePtr addr = NULL;
    if (addr) return addr;
    unsigned stub_size = 0x87;
    char* stub = (char *) malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);
    char* s = stub;

    s = push(s,  esi_opnd);
    s = push(s,  edi_opnd);

    // Stack layout:
    //   +28 address of first src element to copy from
    //   +24 src offset (not needed)
    //   +20 address of first dst element to copy to
    //   +16 dst offset (not needed)
    //   +12 length
    //   +08 return ip
    //   +04 saved edi
    //   +00 saved esi

    // Fetch src, dst, and length into esi, edi, and ecx
    M_Base_Opnd m1(esp_reg, 28);
    M_Base_Opnd m2(esp_reg, 20);
    M_Base_Opnd m3(esp_reg, 12);
    s = mov(s,  esi_opnd,  m1);
    s = mov(s,  edi_opnd,  m2);
    s = mov(s,  ecx_opnd,  m3);
    s = mov(s,  edx_opnd,  ecx_opnd);

    // Compute the number of words to copy, if none jump to code to copy last 2-bytes
    // Note that ecx is never zero, the override ensures this
    Imm_Opnd i1(1);
    s = shift(s, shr_opc,  ecx_opnd,  i1);
    Imm_Opnd i0(size_8, 0);
    s = branch8(s, Condition_Z,  i0);
    char* patch1 = s-1;

    // Is copy less than MOV_PAIR words?
    Imm_Opnd imp(MOV_PAIR);
    s = alu(s, cmp_opc,  ecx_opnd,  imp);
    s = branch8(s, Condition_A,  i0);
    char* patch2 = s-1;

    // Yes, jump into a sequence of move words
    s = mov(s,  eax_opnd,  imp);
    s = alu(s, sub_opc,  eax_opnd,  ecx_opnd);
    s = alu(s, add_opc,  eax_opnd,  eax_opnd);
    s = alu(s, add_opc,  eax_opnd,  eax_opnd);
    s = alu(s, add_opc,  eax_opnd,  eax_opnd);
    Imm_Opnd i_seq_start((int)POINTER_SIZE_INT(s)+7);
    s = alu(s, add_opc,  eax_opnd,  i_seq_start) ;
    s = jump(s,  eax_opnd) ;
    for(unsigned j = 0 ; j<MOV_PAIR ; j++) {
        unsigned offset = (MOV_PAIR-j-1)*4;
        M_Base_Opnd m4(esi_reg, offset);
        M_Base_Opnd m5(edi_reg, offset);
        s = mov(s,  eax_opnd,  m4);
        s = mov(s,  m5,  eax_opnd);
        if (j < MOV_PAIR - 1)
            s = mov(s,  ebp_opnd,  ebp_opnd);  // 2-byte nop sequence
    }

    // Adjust esi and edi
    M_Index_Opnd m6(esi_reg, ecx_reg, 0, 2);
    M_Index_Opnd m7(edi_reg, ecx_reg, 0, 2);
    s = lea(s,  esi_opnd,  m6);
    s = lea(s,  edi_opnd,  m7);
    s = jump8(s,  i0);
    char* patch3 = s-1;

    // No, do a rep MOVSD
    assert(s-patch2-1 < 128);
    *patch2 = (char)(s-patch2-1);
    *s++ = (char)0xF3 ;
    *s++ = (char)0xA5 ;

    // In either case, now determine if there is an additional 2-bytes to copy
    assert(s-patch3-1 < 128);
    *patch3 = (char)(s-patch3-1);
    s = test(s,  edx_opnd,  i1);
    s = branch8(s, Condition_Z,  i0);
    char* patch4 = s-1;

    // Do last 2-byte copy
    assert(s-patch1-1 < 128);
    *patch1 = (char)(s-patch1-1);
    M_Base_Opnd m8(esi_reg, 0);
    M_Base_Opnd m9(edi_reg, 0);
    s = mov(s,  eax_opnd,  m8, size_16) ;
    s = mov(s,  m9,  eax_opnd, size_16) ;

    // Done
    assert(s-patch4-1 < 128);
    *patch4 = (char)(s-patch4-1);
    s = pop(s,  edi_opnd);
    s = pop(s,  esi_opnd);
    Imm_Opnd i20(20);
    s = ret(s,  i20);

    addr = (NativeCodePtr)stub;
    assert(stub_size >= (unsigned)(s-stub));

    DUMP_STUB(stub, "stub.fast_array_copy", s - stub);

    return addr;
}

LilCodeStub* nso_char_array_copy(LilCodeStub* cs, Method_Handle)
{
    Global_Env *env = VM_Global_State::loader_env;
    POINTER_SIZE_INT length_offset = (POINTER_SIZE_INT)vector_length_offset();
    POINTER_SIZE_INT body_offset =
        (POINTER_SIZE_INT)vector_first_element_offset(VM_DATA_TYPE_CHAR);
    NativeCodePtr custom_stub = fast_array_copy();

    // Check all array copy error conditions and that both arrays are [C
    cs = lil_parse_onto_end(cs,
        "jc i4=0,ret;"                          // if length==0 return
        "locals 2;"
        "l0=i0;"
        "jc l0=%0i,slowpath;"                     // src nonnull
        "l1=i2;"
        "jc l1=%1i,slowpath;"                     // dst nonnull
        "ld l0,[l0+%2i:pint];"
        "jc l0!=%3i,slowpath;"                  // src vtable==[C vtable
        "ld l1,[l1+%4i:pint];"
        "jc l0!=l1,slowpath;"                   // src vtable==dst vtable
        "jc i4<0:g4,slowpath;"                     // length>0
        "l1 = i1;"
        "jc l1<0:g4,slowpath;"                     // src offset>=0
        "ld l0,[i0+%5i:pint];"
        "l1 = l1+i4;"
        "jc l0:g4<l1,slowpath;"                    // src offset + length <= src length
        "l1 = i3;"
        "jc l1<0:g4,slowpath;"                     // dst offset>=0
        "ld l0,[i2+%6i:pint];"
        "l1 = l1+i4;"
        "jc l0:g4<l1,slowpath;",                   // dst offset + length <= dst length
        VM_Global_State::loader_env->managed_null,
        VM_Global_State::loader_env->managed_null,
        (POINTER_SIZE_INT)object_get_vtable_offset(),
        env->ArrayOfChar_Class->get_vtable(),
        (POINTER_SIZE_INT)object_get_vtable_offset(),
        length_offset, length_offset);
    assert(cs);

    // At this point array copy will happen, now decide how to do it
    cs = lil_parse_onto_end(cs,
        "l0 = i1:pint<<1;"                           // l0 = first element of src to copy
        "l0 = l0+i0:pint;"
        "l0 = l0+%0i;"
        "l1 = i0;"
        "jc l1!=i2,nonoverlap;"
        "l1 = i3:pint<<1;"                           // l1 = first element of dst to copy to
        "l1 = l1+i2:pint;"
        "l1 = l1+%1i;"
        "out platform:pint,pint,pint:void;"     // copy might overlap, call memmove
        "o0 = l1;"
        "o1 = l0;"
        "o2 = i4:pint<<1;"
        "locals 0;"
        "call %2i;"
        ":ret;"
        "ret;"
        ":nonoverlap;"                          // copy will not overlap, call custom stub
        "i0 = l0:ref;"
        "l1 = i3:pint<<1;"                           // l1 = first element of dst to copy to
        "l1 = l1+i2:pint;"
        "l1 = l1+%3i;"
        "i2 = l1:ref;"
        "tailcall %4i;"
        ":slowpath;"                            // slowpath, fall through to JNI stub, locals 0 is to satisfy LIL validity checker
        "locals 0;",
        body_offset, body_offset, memmove, body_offset, custom_stub);
    assert(cs);

    return cs;
}

#endif // _IA_32

// NSO table item
typedef struct
{
    NativeStubOverride  fun;
    const char*       class_name;
    const char*       method_name;
    const char*       method_desc;
} NSOLocalItem;

// Local NSO table for filling up env-local lookup table
NSOLocalItem local_NSO_table[] = 
{
#ifdef _IA32_
    {nso_char_array_copy,
#else
    {nso_array_copy,
#endif
    "java/lang/System", "arraycopy", "Ljava/lang/Object;ILjava/lang/Object;II)V"
    },
    {nso_get_class,
    "java/lang/Object", "getClass", "()Ljava/lang/Class;"
    },
    {nso_readinternal,
    "java/io/FileInputStream", "readInternal", "(I[BII)I"
    },
    {nso_system_currenttimemillis,
    "java/lang/System", "currentTimeMillis", "()J"
    },
    {nso_newinstance,
    "java/lang/Class", "newInstance", "()Ljava/lang/Object;"
    },
};

// NSO table item
struct NSOTableItem
{
    NativeStubOverride  fun;
    const String*       class_name;
    const String*       method_name;
    const String*       method_desc;
};

// Initializes local NSO table for given environment
NSOTableItem* nso_init_lookup_table(String_Pool* pstrpool)
{
    assert(pstrpool);

    size_t item_count = sizeof(local_NSO_table)/sizeof(local_NSO_table[0]);

    NSOTableItem* NSOTable = (NSOTableItem*)STD_MALLOC(sizeof(NSOTableItem)*(item_count+1));
    assert(NSOTable != NULL);

    int i;

    for (i = 0; i < int(item_count); i++)
    {
        NSOTable[i].fun = local_NSO_table->fun;

        NSOTable[i].class_name =
            pstrpool->lookup(local_NSO_table->class_name);
        NSOTable[i].method_name =
            pstrpool->lookup(local_NSO_table->method_name);
        NSOTable[i].method_desc =
            pstrpool->lookup(local_NSO_table->method_desc);
    }

    NSOTable[i].fun = NULL;

    return NSOTable;
}

// Frees memory occupied by NSO table
void nso_clear_lookup_table(NSOTableItem* NSOTable)
{
    assert(NSOTable);
    STD_FREE(NSOTable);
}

// Look for NSO method
NativeStubOverride nso_find_method_override(const Global_Env* ge,
        const String* class_name, const String* name, const String* desc)
{
    NSOTableItem* envNSOTable = ge->nsoTable;

    for (unsigned i = 0; envNSOTable[i].fun; i++)
    {
        NSOTableItem* pitem = &envNSOTable[i];

        if (pitem->class_name == class_name &&
            pitem->method_name == name &&
            pitem->method_desc == desc)
        {
            return pitem->fun;
        }
    }

    return NULL;
}

