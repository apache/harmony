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
#define LOG_DOMAIN "vm.optimize"
#include "cxxlog.h"

#include "open/types.h"
#include "open/vm_ee.h"
#include "environment.h"
#include "encoder.h"
#include "open/vm_util.h"
#include "compile.h"
#include "vtable.h"
// *** This is for readInternal override
#include "jni_utils.h"
#include "vm_arrays.h"

extern int readinternal_override_lil(JNIEnv *jenv, Java_java_io_FileInputStream *pThis, int fd, Vector_Handle pArrayOfByte, int  offset, int len);

// *** This is for currenttimemillis override
#include "platform_core_natives.h"

static unsigned java_array_of_char_body_offset()
{
    return vector_first_element_offset(VM_DATA_TYPE_CHAR);
}

static unsigned java_array_of_char_length_offset()
{
    return vector_length_offset();
}

static bool enable_fast_char_arraycopy()
{
    return !(REFS_IS_COMPRESSED_MODE);
}

static ManagedObject *
get_class_ptr(ManagedObject *obj)
{
    return *(obj->vt()->clss->get_class_handle());
}

// ****** Begin overrides
// ****** identityHashCode
#include "object_generic.h"
unsigned native_hashcode_fastpath_size(Method * UNREF m)
{
    return 20;
}
void gen_native_hashcode(Emitter_Handle h, Method *m)
{
    unsigned UNUSED stub_length = native_hashcode_fastpath_size(m);
    char *s = *(char **)h;
    char * UNUSED stub = s;

    s = push(s,  ebp_opnd);
    s = push(s,  ebx_opnd);
    s = push(s,  M_Base_Opnd(esp_reg, 12));
    s = call(s, (char*)generic_hashcode);
    s = alu(s, add_opc,  esp_opnd,  Imm_Opnd(4));
    s = pop(s,  ebx_opnd);
    s = pop(s,  ebp_opnd);
    s = ret(s,  Imm_Opnd(4));

    assert((int)stub_length >= (s - stub));
    *(char **)h = s;
}

// ****** newInstance
// *** This is for the newInstance override
//#include "exceptions.h"
unsigned native_newinstance_fastpath_size(Method * UNREF m)
{
    return 100;
}
void gen_native_newinstance(Emitter_Handle h, Method *m)
{
    unsigned UNUSED stub_length = native_newinstance_fastpath_size(m);
    char *s = *(char **)h;
    char * UNUSED stub = s;

    Global_Env *env = VM_Global_State::loader_env;
    assert(env->vm_class_offset != 0);
    unsigned current_offset = 1;
    unsigned limit_offset = 1;
    
    if(gc_supports_frontier_allocation(&current_offset, &limit_offset)){
        s = push(s,  ebp_opnd); // entry 0:managed:ref:ref
        s = push(s,  ebx_opnd);
        s = push(s,  esi_opnd);
        s = push(s,  edi_opnd);
        
        // Get Class_Handle from java.lang.Class object
        s = mov(s,  eax_opnd,  M_Base_Opnd(esp_reg, 20)) ; // ld l0,[i0+offset] (fetch i0)
        s = mov(s,  ebp_opnd,  M_Base_Opnd(eax_reg, (int)env->vm_class_offset)) ; // ld l0,i0+offset (add offset, ld val)
        
        // Determine if this class supports fast allocation
        size_t offset_is_fast_allocation_possible = env->Void_Class->get_offset_of_fast_allocation_flag();
        s = movzx(s,  ebx_opnd,  M_Base_Opnd(ebp_reg, offset_is_fast_allocation_possible), size_8);
        s = test(s,  ebx_opnd,  ebx_opnd); // jc l1=0,fallback
        s = branch8(s, Condition_Z,  Imm_Opnd(size_8, 50));

        // Class supports fast allocation, now use frontier allocation technique
        size_t offset_gc_local           = (U_8*)&(p_TLS_vmthread->_gc_private_information) - (U_8*)p_TLS_vmthread;
        size_t offset_allocation_handle  = env->Void_Class->get_offset_of_allocation_handle();
        size_t offset_instance_data_size = env->Void_Class->get_offset_of_instance_data_size();
        current_offset += (unsigned) offset_gc_local;
        limit_offset += (unsigned) offset_gc_local;

        // Get frontier into r, limit into l2, instance size into l3
#ifndef PLATFORM_POSIX
        *s++ = (char)0x64;
        *s++ = (char)0xa1;
        *s++ = (char)0x14;
        *s++ = (char)0x00;
        *s++ = (char)0x00;
        *s++ = (char)0x00;

        // hack: the above sequence moves fs to eax, but want ebx
        s = mov(s,  ebx_opnd,  eax_opnd);
#else
        // DO NOT RUN THIS OVERRIDE ON LINUX (it will not work)
        LDIE(45, "Not supported on this platform");
#endif // !PLATFORM_POSIX

        s = mov(s,  eax_opnd,  M_Base_Opnd(ebx_reg, current_offset)); // ld r,[l1+current_offset]
        s = mov(s,  esi_opnd,  M_Base_Opnd(ebx_reg, limit_offset)); // ld l2,[l1+limit_offset]
        s = mov(s,  edi_opnd,  M_Base_Opnd(ebp_reg, offset_instance_data_size)); // ld l3,[l0+offset_instance_data_size]

        // Compute new frontier
        s = alu(s, add_opc,  edi_opnd,  eax_opnd); // l3=l3+r

        // Is it within limit?
        s = alu(s, cmp_opc,  esi_opnd,  edi_opnd); // jc l2<l3,fallback; esi=l2, edi=l3
        s = branch32(s, Condition_L,  Imm_Opnd(20)) ;

        // Yes, store new frontier, initialise vtable
        s = mov(s,  M_Base_Opnd(ebx_reg, current_offset),  edi_opnd); // st [l1+current_offset],l3
        s = mov(s,  edi_opnd,  M_Base_Opnd(ebp_reg, offset_allocation_handle)); // ld l3,[l0+offset_alloc_handle]
        s = mov(s,  esi_opnd,  eax_opnd); // l2=r
        s = mov(s,  M_Base_Opnd(esi_reg, 0),  edi_opnd); // st [l2],l3

        // Done
        s = pop(s,  edi_opnd);
        s = pop(s,  esi_opnd);
        s = pop(s,  ebx_opnd);
        s = pop(s,  ebp_opnd);
        s = ret(s,  Imm_Opnd(4));

        // Fallback: need to restore values if we end up here
        s = pop(s,  edi_opnd);
        s = pop(s,  esi_opnd);
        s = pop(s,  ebx_opnd);
        s = pop(s,  ebp_opnd);
    }
    
    assert((int)stub_length >= (s - stub));
    *(char **)h = s;
}

// ****** readInternal
unsigned native_readinternal_fastpath_size(Method * UNREF m)
{
    return 100;
}
void gen_native_readinternal(Emitter_Handle h, Method *m)
{
    unsigned UNUSED stub_length = native_readinternal_fastpath_size(m);
    char *s = *(char **)h;
    char * UNUSED stub = s;

    s = alu(s, sub_opc,  esp_opnd,  Imm_Opnd(24));
    s = mov(s,  M_Base_Opnd(esp_reg, 0),  Imm_Opnd((int)p_TLS_vmthread->jni_env)) ; // o0=jni_native_intf
    s = mov(s,  eax_opnd,  M_Base_Opnd(esp_reg, 44)) ; // o1=i0
    s = mov(s,  M_Base_Opnd(esp_reg, 4),  eax_opnd) ;
    s = mov(s,  eax_opnd,  M_Base_Opnd(esp_reg, 40)) ; // o2=i1
    s = mov(s,  M_Base_Opnd(esp_reg, 8),  eax_opnd) ;
    s = mov(s,  eax_opnd,  M_Base_Opnd(esp_reg, 36)) ; // o3=i2
    s = mov(s,  M_Base_Opnd(esp_reg, 12),  eax_opnd) ;
    s = mov(s,  eax_opnd,  M_Base_Opnd(esp_reg, 32)) ; // o4=i3
    s = mov(s,  M_Base_Opnd(esp_reg, 16),  eax_opnd) ;
    s = mov(s,  eax_opnd,  M_Base_Opnd(esp_reg, 28)) ; // o5=i4
    s = mov(s,  M_Base_Opnd(esp_reg, 20),  eax_opnd) ;
    s = call(s, (char*)readinternal_override_lil);
    s = alu(s, add_opc,  esp_opnd,  Imm_Opnd(24));
    s = ret(s,  Imm_Opnd(20));
    
    assert((int)stub_length >= (s - stub));
    *(char **)h = s;
}

// ****** get current time millis
unsigned native_getccurrenttime_fastpath_size(Method * UNREF m)
{
    return 20;
}
void gen_native_system_currenttimemillis(Emitter_Handle h, Method *m)
{
    unsigned UNUSED stub_length = native_getccurrenttime_fastpath_size(m);
    char *s = *(char **)h;
    char * UNUSED stub = s;

    s = push(s,  esi_opnd);
    s = call(s, (char*)get_current_time);
    s = pop(s,  esi_opnd);
    s = ret(s);


    assert((int)stub_length >= (s - stub));
    *(char **)h = s;
}

// ****** 20031009 above are additions to bring original on par with LIL

unsigned native_getclass_fastpath_size(Method * UNREF m)
{
    return 20;
}


void gen_native_getclass_fastpath(Emitter_Handle h, Method *m)
{
    unsigned UNUSED stub_length = native_getclass_fastpath_size(m);
    char *s = *(char **)h;
    char * UNUSED stub = s;

    s = push(s,  M_Base_Opnd(esp_reg, 4));
    s = call(s, (char*)get_class_ptr);
    s = alu(s, add_opc,  esp_opnd,  Imm_Opnd(4)); 
    s = test(s,  eax_opnd,  eax_opnd);           
    s = branch8(s, Condition_Z,  Imm_Opnd(size_8, 3)) ; 
    s = ret(s,  Imm_Opnd(4));

    assert((int)stub_length >= (s - stub));
    *(char **)h = s;
}



// Return an upper bound on the size of the custom arraycopy routine.
unsigned native_arraycopy_fastpath_size(Method * UNREF m)
{
    if (enable_fast_char_arraycopy())
        return 350;
    return 0;
}

//MOV_PAIR must be less than 32
#define MOV_PAIR  8
void gen_native_arraycopy_fastpath(Emitter_Handle h, Method *method)
{
    if (!enable_fast_char_arraycopy())
        return;
    char *stub = *(char **)h;
    Global_Env *env = VM_Global_State::loader_env;

    assert(MOV_PAIR<32) ;
    unsigned UNUSED stub_length = native_arraycopy_fastpath_size(method);// 300 + MOV_PAIR*8; 
    char *s = stub;

    char* patch[20] ;//jmp patch
    unsigned p_c = 0 ;
    char* patch_ret[20] ;//jmp-to-ret patch
    unsigned p_c_ret =0 ;
    //get length offset of JavaArrayOf Char
    unsigned length_offset = java_array_of_char_length_offset() ;
    //
    // push ebp
    // push ebx
    // push esi
    // push edi
    s = push(s,  ebp_opnd);
    s = push(s,  ebx_opnd);
    s = push(s,  esi_opnd);
    s = push(s,  edi_opnd);

    //fast arraycopy , only handling 'C' type
    // esp  + 16        return-ip
    //      + 20        length
    //      + 24        dstoffset
    //      + 28        dst
    //      + 32        srcoffset
    //      + 36        src
    
    // Test whether first object is null.
    s = mov(s,  esi_opnd,  M_Base_Opnd(esp_reg, 36)) ;   // mov esi, [esp+36] ; src
    s = test(s,  esi_opnd,  esi_opnd) ;           // or  esi, esi
    s = branch32(s, Condition_Z,  Imm_Opnd(0x0)) ;           // !!je backup
    patch[p_c++] = s - 4 ;

    // Test whether second object is null.
    s = mov(s,  edi_opnd,  M_Base_Opnd(esp_reg, 28)) ;   // mov edi, [esp+28] ; dst
    s = test(s,  edi_opnd,  edi_opnd) ;           // or  edi, edi
    s = branch32(s, Condition_Z,  Imm_Opnd(0x0)) ;           // !!je backup
    patch[p_c++] = s - 4 ;

    // Test whether the first object is of type [C.
    s = mov(s,  ebx_opnd,  M_Base_Opnd(esi_reg, object_get_vtable_offset())) ;     // mov ebx, [esi] ; src->vt
    s = alu(s, cmp_opc,  ebx_opnd,  Imm_Opnd((int)(env->ArrayOfChar_Class->get_vtable()))); // cmp ebx, vtable_of_char_array
    s = branch32(s, Condition_NZ,  Imm_Opnd(0x0));
    patch[p_c++] =  s - 4 ;

    // Test whether the two objects have the same class.
    s = alu(s, cmp_opc,  ebx_opnd,  M_Base_Opnd(edi_reg, object_get_vtable_offset())) ;          // cmp ebx, [edi] ; 
    s = branch32(s, Condition_NZ,  Imm_Opnd(0x0)) ;           // !!je backup
    patch[p_c++] =  s - 4 ;

    // Test the array index conditions.
    s = mov(s,  ecx_opnd,  M_Base_Opnd(esp_reg, 20)) ;   // mov ecx, [esp+20] ; length
    s = alu(s, cmp_opc,  ecx_opnd,  Imm_Opnd(0)) ;                // cmp ecx, 0
    s = branch32(s, Condition_Z,  Imm_Opnd(0x0)) ;           // !!je ret
    patch_ret[p_c_ret++] = s - 4 ;
    s = branch32(s, Condition_L,  Imm_Opnd(0x0)) ;           // !!jl backup (signed)
    patch[p_c++] =  s - 4 ;
    s = mov(s,  eax_opnd,  M_Base_Opnd(esp_reg, 32)) ;   // mov eax, [esp+32] ; srcoffset
    s = test(s,  eax_opnd,  eax_opnd) ;                  // test eax, eax
    s = branch32(s, Condition_L,  Imm_Opnd(0x0)) ;           // !!jl backup
    patch[p_c++] =  s - 4 ;
    s = mov(s,  ebx_opnd,  M_Base_Opnd(esp_reg, 24)) ;   // mov ebx, [esp+24] ; dstoffset
    s = test(s,  ebx_opnd,  ebx_opnd) ;                  // test ebx, ebx
    s = branch32(s, Condition_L,  Imm_Opnd(0x0)) ;           // !!jl backup
    patch[p_c++] = s - 4 ;

    s = mov(s,  ebp_opnd,  M_Base_Opnd(esi_reg, length_offset)) ;    // mov ebp, [src+length_offset] ; src->length
    s = alu(s, add_opc,  eax_opnd,  ecx_opnd) ;                     // add eax, ecx ;srcoffset + length
    s = alu(s, cmp_opc,  eax_opnd,  ebp_opnd) ;                     // cmp eax, ebp
    s = branch32(s, Condition_A,  Imm_Opnd(0x0)) ;                      // !!ja backup
    patch[p_c++] = s - 4 ;
    s = mov(s,  ebp_opnd,  M_Base_Opnd(edi_reg, length_offset)) ;    // mov ebp, [dst+length_offset] ;dst->length
    s = alu(s, add_opc,  ebx_opnd,  ecx_opnd) ;                     // add ebx, ecx ;dstoffset + length
    s = alu(s, cmp_opc,  ebx_opnd,  ebp_opnd) ;                     // cmp ebx, ebp
    s = branch32(s, Condition_A,  Imm_Opnd(0x0)) ;                      // !!ja backup
    patch[p_c++] = s - 4 ;

    void* (*m_move)( void *, const void *, size_t );
    m_move = (void* (*)( void *, const void *, size_t ))memmove ;
    unsigned body_offset = java_array_of_char_body_offset() ;
    s = mov(s,  ebp_opnd,  edi_opnd) ;
    s = mov(s,  eax_opnd, M_Base_Opnd(esp_reg, 24)) ;
    s = lea(s,  edi_opnd,  M_Index_Opnd(edi_reg, eax_reg, body_offset, 1)); // lea edi, [edi + 2*eax + body_offset]
    s = mov(s,  eax_opnd,  M_Base_Opnd(esp_reg, 32)) ;
    s = lea(s,  esi_opnd,  M_Index_Opnd(esi_reg, eax_reg, body_offset, 1)); // lea esi, [esi + 2*eax + body_offset]

    //if they overlap, goto normal memmove .
    // if |edi-esi|<ecx, overlap. we can use eax, ebx, edx
    s = mov(s,  eax_opnd,  edi_opnd) ;
    s = mov(s,  ebx_opnd,  esi_opnd) ;
    s = mov(s,  edx_opnd,  ecx_opnd) ;
    s = alu(s, sub_opc,  eax_opnd,  ebx_opnd) ;
    s = branch8(s, Condition_G,  Imm_Opnd(size_8, 0x0)) ; //!!jg
    char* patch5 = s - 1 ;
    s = neg(s,  eax_opnd) ;
    *patch5 = (char)(s- patch5 - 1) ;
    s = alu(s, add_opc,  edx_opnd,  edx_opnd) ;
    s = alu(s, cmp_opc,  eax_opnd,  edx_opnd) ;
    s = branch8(s, Condition_A,  Imm_Opnd(size_8, 0x0)) ; //!!jg next2
    char* patch6 = s - 1 ;
    s = push(s,  edx_opnd) ;
    s = push(s,  esi_opnd) ;
    s = push(s,  edi_opnd) ;
    s = call(s, (char*)m_move) ;
    s = alu(s, add_opc,  esp_opnd,  Imm_Opnd(12)) ;
    s = jump32(s,  Imm_Opnd(0x0)) ;    //jmp next3
    char* patch7 = s - 4 ;

    *patch6 = (char)(s - patch6 - 1) ;
    s = mov(s,  ebx_opnd,  ecx_opnd) ;
    s = shift(s, shr_opc,  ecx_opnd,  Imm_Opnd(1)) ; 
    s = mov(s,  edx_opnd,  ecx_opnd) ;
    s = branch32(s, Condition_Z,  Imm_Opnd(0x0)) ;
    char* patch1 = s - 4 ;
    //if the ecx < MOV_PAIR , do some fast copy!
    s = alu(s, cmp_opc,  ecx_opnd,  Imm_Opnd(MOV_PAIR)) ;
    s = branch32(s, Condition_A,  Imm_Opnd(0x0)) ;
    char* patch3 = s - 4 ;
    s = mov(s,  eax_opnd, Imm_Opnd(MOV_PAIR)) ;
    s = alu(s, sub_opc,  eax_opnd,  ecx_opnd) ;
    s = alu(s, add_opc,  eax_opnd,  eax_opnd) ;
    s = alu(s, add_opc,  eax_opnd,  eax_opnd) ;
    s = alu(s, add_opc,  eax_opnd,  eax_opnd) ;

    s = alu(s, add_opc,  eax_opnd,  Imm_Opnd((unsigned)s+7)) ;
    s = jump(s,  eax_opnd) ;
    int j = 0 ;
    for(j = 0 ; j<MOV_PAIR ; j++){
        //mov eax, [esi+xx]
        //mov [edi+xx], eax
        s = mov(s,  eax_opnd,  M_Base_Opnd(esi_reg,(4*MOV_PAIR-4)-j*4)) ;
        s = mov(s,  M_Base_Opnd(edi_reg,(4*MOV_PAIR-4)-j*4),  eax_opnd) ;
        if (j < MOV_PAIR - 1)
        {
            s = mov(s,  ebp_opnd,  ebp_opnd);  // 2-byte nop sequence
        }
    }
    s = lea(s,  esi_opnd,  M_Index_Opnd(esi_reg, ecx_reg, 0, 2));
    s = lea(s,  edi_opnd,  M_Index_Opnd(edi_reg, ecx_reg, 0, 2));
    s = jump8(s,  Imm_Opnd(size_8, 0x0)) ;
    char* patch4 = s - 1 ;
    //
    *(unsigned*)patch3 = (unsigned)(s - patch3 - 4) ;
    *s++ = (char)0xF3 ;
    *s++ = (char)0xA5 ;// F3 A4 rep movs DWORD
    *patch4 = (char)(s - patch4 - 1) ;
    s = alu(s, add_opc,  edx_opnd,  edx_opnd) ;
    *(unsigned*)patch1 = (unsigned)(s - patch1 -4) ;
    s = alu(s, sub_opc,  ebx_opnd,  edx_opnd) ;
    s = branch8(s, Condition_Z,  Imm_Opnd(size_8, 0x0)) ;
    char* patch2 = s - 1 ;
    //edx only could be 0, 2
    //2:  mov WORD ax, [esi+1]
    //    mov WORD [edi+1], al
    s = mov(s,  eax_opnd,  M_Base_Opnd(esi_reg,0), size_16) ;
    s = mov(s,  M_Base_Opnd(edi_reg,0),  eax_opnd, size_16) ;
    //0: _next:
    *patch2 = (char)(s - patch2 - 1) ;
    s = mov(s,  edi_opnd,  ebp_opnd) ;
    *(unsigned*)patch7 = (unsigned)(s - patch7 -4) ;

    for(;p_c_ret>0 ; p_c_ret--){
        *(unsigned*)(patch_ret[p_c_ret-1]) = (unsigned)(s - patch_ret[p_c_ret-1] - 4) ;
    }
    s = pop(s,  edi_opnd);
    s = pop(s,  esi_opnd);
    s = pop(s,  ebx_opnd);
    s = pop(s,  ebp_opnd);

    s = ret(s,  Imm_Opnd(method->get_num_arg_slots() * 4));

    //Fill Patches
    for(; p_c>0 ; p_c--){
        *(unsigned*)(patch[p_c-1]) = (unsigned)(s - patch[p_c-1] -4) ;
    }

    /********************************************************************
     *generate the backup rountine
     ********************************************************************/

    // 20030325: the backup routine used to be an old copy of a native method wrapper
    //           we do not want to duplicate this code, so I have changed the backup to
    //           jump to a wrapper generated by prepare_native_method

    s = pop(s,  edi_opnd);
    s = pop(s,  esi_opnd);
    s = pop(s,  ebx_opnd);
    s = pop(s,  ebp_opnd);

    assert((int)stub_length >= (s - stub));
    *(char **)h = s;
} //jit_inline_native_array_copy_general
