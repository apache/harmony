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
 * @author Pavel Rebriy
 */
#define LOG_DOMAIN "jvmti.step"
#include "cxxlog.h"

#include "open/bytecodes.h"
#include "open/vm_method_access.h"

#include "port_crash_handler.h"
#include "jvmti.h"
#include "jthread.h"
#include "Class.h"
#include "vm_log.h"
#include "jvmti_utils.h"
#include "jvmti_internal.h"
#include "jit_intf_cpp.h"
#include "stack_iterator.h"
#include "interpreter.h"
#include "cci.h"
#include "jvmti_break_intf.h"
#include "jni_utils.h"

#define NOT_IMPLEMENTED LDIE(51, "Not implemented")

static inline short
jvmti_GetHalfWordValue( const unsigned char *bytecode,
                        unsigned location)
{
    short result = (short)( (bytecode[location] << 8)|(bytecode[location + 1]) );
    return result;
} // jvmti_GetHalfWordValue

static inline int
jvmti_GetWordValue( const unsigned char *bytecode,
                    unsigned location)
{
    int result = (int)( (bytecode[location    ] << 24)|(bytecode[location + 1] << 16)
                       |(bytecode[location + 2] << 8) |(bytecode[location + 3]      ) );
    return result;
} // jvmti_GetWordValue

NativeCodePtr static get_ip_for_invoke_call_ip(VM_thread* thread,
    unsigned location, unsigned next_location)
{
#if (defined _IA32_) || (defined _EM64T_)
    ASSERT_NO_INTERPRETER;

    // create stack iterator from native
    StackIterator* si = si_create_from_native( thread );
    si_transfer_all_preserved_registers(si);
    assert(si_is_native(si));
    // get java frame
    si_goto_previous(si);
    assert(!si_is_native(si));
    // find correct ip in java frame
    NativeCodePtr ip = si_get_ip(si);

    Method *method = si_get_method(si);
    assert(method);

    CodeChunkInfo *cci = si_get_code_chunk_info(si);
    JIT *jit = cci->get_jit();
    si_free(si);

    NativeCodePtr next_ip;
    OpenExeJpdaError UNREF result = jit->get_native_location_for_bc(method,
        (uint16)next_location, &next_ip);
    assert(result == EXE_ERROR_NONE);
    assert(ip < next_ip);

    VMBreakPoints *vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;
    VMBreakPoint *bp = vm_brpt->find_breakpoint(ip);

    InstructionDisassembler disasm;
    if (bp)
        disasm = *bp->disasm;
    else
        disasm = ip;

    // Iterate over this bytecode instructions until we reach an
    // indirect call in this bytecode which should be the
    // invikevirtual or invokeinterface call
    NativeCodePtr call_ip = NULL;
    do
    {
        if (disasm.get_type() == InstructionDisassembler::INDIRECT_CALL)
            call_ip = ip;

        ip = (NativeCodePtr)((POINTER_SIZE_INT)ip + disasm.get_length_with_prefix());

        // Another thread could have instrumented this location for
        // prediction of invokevirtual or invokeinterface, so it is
        // necessary to check that location may be instrumented
        if (port_is_breakpoint_set(ip))
        {
            bp = vm_brpt->find_breakpoint(ip);
            assert(bp);
            disasm = *bp->disasm;
        }
        else
            disasm = ip;
    }
    while (ip < next_ip);

    // ip now points to the call instruction which actually invokes a
    // virtual method. We're going to set a sythetic breakpoint there
    // and allow execution up until that point to get the virtual
    // table address and offset inside of it to determine exactly
    // which method is going to invoked in runtime.
    //
    // In case call_ip is NULL, it means that we've already inside of
    // the calling of the method, but the method is not yet ready to
    // be executed, e.g. it is being resolved or compiled at the
    // time. In this case, just setting single step state inside of
    // the target thread will enabled stepping of this method
    // automatically.
    TRACE2("jvmti.break.ss", "Predicting VIRTUAL type breakpoint on address: " << call_ip);

    return call_ip;
#else
    NOT_IMPLEMENTED;
    return 0;
#endif
} // jvmti_get_invoked_virtual_method

void
jvmti_SingleStepLocation( VM_thread* thread,
                          Method *method,
                          unsigned bytecode_index,
                          jvmti_StepLocation **next_step,
                          unsigned *count)
{
    assert(next_step);
    assert(count);
    ASSERT_NO_INTERPRETER;

    // get method bytecode array and code length
    const unsigned char *bytecode = method->get_byte_code_addr();
    unsigned len = method->get_byte_code_size();
    unsigned location = bytecode_index;
    assert(location < len);

    // initialize step location count
    *count = 0;

    // parse bytecode
    jvmtiError error;
    bool is_wide = false;
    int offset;
    do {

        switch( bytecode[location] )
        {
        // wide instruction
        case OPCODE_WIDE:           /* 0xc4 */
            assert( !is_wide );
            location++;
            is_wide = true;
            continue;

        // if instructions
        case OPCODE_IFEQ:           /* 0x99 + s2 */
        case OPCODE_IFNE:           /* 0x9a + s2 */
        case OPCODE_IFLT:           /* 0x9b + s2 */
        case OPCODE_IFGE:           /* 0x9c + s2 */
        case OPCODE_IFGT:           /* 0x9d + s2 */
        case OPCODE_IFLE:           /* 0x9e + s2 */

        case OPCODE_IF_ICMPEQ:      /* 0x9f + s2 */
        case OPCODE_IF_ICMPNE:      /* 0xa0 + s2 */
        case OPCODE_IF_ICMPLT:      /* 0xa1 + s2 */
        case OPCODE_IF_ICMPGE:      /* 0xa2 + s2 */
        case OPCODE_IF_ICMPGT:      /* 0xa3 + s2 */
        case OPCODE_IF_ICMPLE:      /* 0xa4 + s2 */

        case OPCODE_IF_ACMPEQ:      /* 0xa5 + s2 */
        case OPCODE_IF_ACMPNE:      /* 0xa6 + s2 */

        case OPCODE_IFNULL:         /* 0xc6 + s2 */
        case OPCODE_IFNONNULL:      /* 0xc7 + s2 */
            assert( !is_wide );
            offset = (int)location + jvmti_GetHalfWordValue( bytecode, location + 1 );
            location += 3;
            *count = 2;
            error = _allocate( sizeof(jvmti_StepLocation) * 2, (unsigned char**)next_step );
            assert( error == JVMTI_ERROR_NONE );
            (*next_step)[0].method = method;
            (*next_step)[0].location = location;
            (*next_step)[0].native_location = NULL;
            (*next_step)[0].no_event = false;
            (*next_step)[1].method = method;
            (*next_step)[1].location = offset;
            (*next_step)[1].native_location = NULL;
            (*next_step)[1].no_event = false;
            break;

        // goto instructions
        case OPCODE_GOTO:           /* 0xa7 + s2 */
        case OPCODE_JSR:            /* 0xa8 + s2 */
            assert( !is_wide );
            offset = (int)location + jvmti_GetHalfWordValue( bytecode, location + 1 );
            *count = 1;
            error = _allocate( sizeof(jvmti_StepLocation), (unsigned char**)next_step );
            assert( error == JVMTI_ERROR_NONE );
            (*next_step)->method = method;
            (*next_step)->location = offset;
            (*next_step)->native_location = NULL;
            (*next_step)->no_event = false;
            break;
        case OPCODE_GOTO_W:         /* 0xc8 + s4 */
        case OPCODE_JSR_W:          /* 0xc9 + s4 */
            assert( !is_wide );
            offset = (int)location + jvmti_GetWordValue( bytecode, location + 1 );
            *count = 1;
            error = _allocate( sizeof(jvmti_StepLocation), (unsigned char**)next_step );
            assert( error == JVMTI_ERROR_NONE );
            (*next_step)->method = method;
            (*next_step)->location = offset;
            (*next_step)->native_location = NULL;
            (*next_step)->no_event = false;
            break;

        // tableswitch instruction
        case OPCODE_TABLESWITCH:    /* 0xaa + pad + s4 * (3 + N) */
            assert( !is_wide );
            location = (location + 4)&(~0x3U);
            {
                int low = jvmti_GetWordValue( bytecode, location + 4 );
                int high = jvmti_GetWordValue( bytecode, location + 8 );
                int number = high - low + 2;

                *count = number;
                error = _allocate( sizeof(jvmti_StepLocation) * number, (unsigned char**)next_step );
                assert( error == JVMTI_ERROR_NONE );
                (*next_step)[0].method = method;
                (*next_step)[0].location = (int)bytecode_index
                    + jvmti_GetWordValue( bytecode, location );
                (*next_step)[0].native_location = NULL;
                (*next_step)[0].no_event = false;
                location += 12;
                for( int index = 1; index < number; index++, location += 4 ) {
                    (*next_step)[index].method = method;
                    (*next_step)[index].location = (int)bytecode_index
                        + jvmti_GetWordValue( bytecode, location );
                    (*next_step)[index].native_location = NULL;
                    (*next_step)[index].no_event = false;
                }
            }
            break;

        // lookupswitch instruction
        case OPCODE_LOOKUPSWITCH:   /* 0xab + pad + s4 * 2 * (N + 1) */
            assert( !is_wide );
            location = (location + 4)&(~0x3U);
            {
                int number = jvmti_GetWordValue( bytecode, location + 4 ) + 1;

                *count = number;
                error = _allocate( sizeof(jvmti_StepLocation) * number, (unsigned char**)next_step );
                assert( error == JVMTI_ERROR_NONE );
                (*next_step)[0].method = method;
                (*next_step)[0].location = (int)bytecode_index
                    + jvmti_GetWordValue( bytecode, location );
                (*next_step)[0].native_location = NULL;
                (*next_step)[0].no_event = false;
                location += 12;
                for( int index = 1; index < number; index++, location += 8 ) {
                    (*next_step)[index].method = method;
                    (*next_step)[index].location = (int)
                        + jvmti_GetWordValue( bytecode, location );
                    (*next_step)[index].native_location = NULL;
                    (*next_step)[index].no_event = false;
                }
            }
            break;

        // athrow and invokeinterface instruction
        case OPCODE_ATHROW:         /* 0xbf */
            assert( !is_wide );
            break;
        case OPCODE_INVOKEINTERFACE:/* 0xb9 + u2 + u1 + u1 */
            assert( !is_wide );
            {
                NativeCodePtr ip = get_ip_for_invoke_call_ip(thread, location,
                    location + 5);
                if (NULL != ip)
                {
                    error = _allocate(sizeof(jvmti_StepLocation),
                        (unsigned char**)next_step );
                    assert(error == JVMTI_ERROR_NONE);
                    *count = 1;
                    (*next_step)->method = method;
                    (*next_step)->location = location;
                    (*next_step)->native_location = ip;
                    (*next_step)->no_event = true;
                }
            }
            break;

        // return instructions
        case OPCODE_IRETURN:        /* 0xac */
        case OPCODE_LRETURN:        /* 0xad */
        case OPCODE_FRETURN:        /* 0xae */
        case OPCODE_DRETURN:        /* 0xaf */
        case OPCODE_ARETURN:        /* 0xb0 */
        case OPCODE_RETURN:         /* 0xb1 */
            assert( !is_wide );
            {
                error = jvmti_get_next_bytecodes_from_native( 
                    thread, next_step, count, true );
                assert( error == JVMTI_ERROR_NONE );
            }
            break;

        // invokes instruction
        case OPCODE_INVOKESPECIAL:  /* 0xb7 + u2 */
        case OPCODE_INVOKESTATIC:   /* 0xb8 + u2 */
            assert( !is_wide );
            {
                unsigned short index = jvmti_GetHalfWordValue( bytecode, location + 1 );
                Class *klass = method_get_class( method );

                if (klass->get_constant_pool().is_entry_resolved(index))
                {
                    Method *invoke_method = klass->get_constant_pool().get_ref_method(index); 
                    if(!method_is_native(invoke_method)) {
                        *count = 1;
                        error = _allocate( sizeof(jvmti_StepLocation), (unsigned char**)next_step );
                        assert( error == JVMTI_ERROR_NONE );
                        (*next_step)->method = invoke_method;
                        (*next_step)->location = 0;
                        (*next_step)->native_location = NULL;
                        (*next_step)->no_event = false;
                    }
                }
            }
            break;

        // invokevirtual instruction
        case OPCODE_INVOKEVIRTUAL:  /* 0xb6 + u2 */
            assert( !is_wide );
            {
                NativeCodePtr ip = get_ip_for_invoke_call_ip(thread, location,
                    location + 3);
                if (NULL != ip)
                {
                    error = _allocate(sizeof(jvmti_StepLocation),
                        (unsigned char**)next_step );
                    assert(error == JVMTI_ERROR_NONE);
                    *count = 1;
                    (*next_step)->method = method;
                    (*next_step)->location = location;
                    (*next_step)->native_location = ip;
                    (*next_step)->no_event = true;
                }
            }
            break;

        case OPCODE_MULTIANEWARRAY: /* 0xc5 + u2 + u1 */
            assert( !is_wide );
            location++;

        case OPCODE_IINC:           /* 0x84 + u1|u2 + s1|s2 */
            if( is_wide ) {
                location += 2;
                is_wide = false;
            }

        case OPCODE_SIPUSH:         /* 0x11 + s2 */
        case OPCODE_LDC_W:          /* 0x13 + u2 */
        case OPCODE_LDC2_W:         /* 0x14 + u2 */
        case OPCODE_GETSTATIC:      /* 0xb2 + u2 */
        case OPCODE_PUTSTATIC:      /* 0xb3 + u2 */
        case OPCODE_GETFIELD:       /* 0xb4 + u2 */
        case OPCODE_PUTFIELD:       /* 0xb5 + u2 */
        case OPCODE_NEW:            /* 0xbb + u2 */
        case OPCODE_ANEWARRAY:      /* 0xbd + u2 */
        case OPCODE_CHECKCAST:      /* 0xc0 + u2 */
        case OPCODE_INSTANCEOF:     /* 0xc1 + u2 */
            assert( !is_wide );
            location++;

        case OPCODE_ILOAD:          /* 0x15 + u1|u2 */
        case OPCODE_LLOAD:          /* 0x16 + u1|u2 */
        case OPCODE_FLOAD:          /* 0x17 + u1|u2 */
        case OPCODE_DLOAD:          /* 0x18 + u1|u2 */
        case OPCODE_ALOAD:          /* 0x19 + u1|u2 */
        case OPCODE_ISTORE:         /* 0x36 + u1|u2 */
        case OPCODE_LSTORE:         /* 0x37 + u1|u2 */
        case OPCODE_FSTORE:         /* 0x38 + u1|u2 */
        case OPCODE_DSTORE:         /* 0x39 + u1|u2 */
        case OPCODE_ASTORE:         /* 0x3a + u1|u2 */
            if( is_wide ) {
                location++;
                is_wide = false;
            }

        case OPCODE_BIPUSH:         /* 0x10 + s1 */
        case OPCODE_LDC:            /* 0x12 + u1 */
        case OPCODE_NEWARRAY:       /* 0xbc + u1 */
            assert( !is_wide );
            location++;

        default:
            assert( !is_wide );
            assert( bytecode[bytecode_index] < OPCODE_COUNT );
            assert( bytecode[bytecode_index] != _OPCODE_UNDEFINED );

            location++;
            *count = 1;
            error = _allocate( sizeof(jvmti_StepLocation), (unsigned char**)next_step );
            assert( error == JVMTI_ERROR_NONE );
            (*next_step)->method = method;
            (*next_step)->location = location;
            (*next_step)->native_location = NULL;
            (*next_step)->no_event = false;
            break;

        // ret instruction
        case OPCODE_RET:            /* 0xa9 + u1|u2  */
            // FIXME - need to obtain return address from stack.
            LDIE(25, "SingleStepLocation: not implemented ret instruction");
            break;
        }
        break;
    } while( true );

    for( unsigned index = 0; index < *count; index++ ) {
        TRACE2( "jvmti.break.ss", "Step: " << method
            << " :" << bytecode_index << "\n      -> "
            << ((*next_step)[index].method)
            << " :" << (*next_step)[index].location << " :"
            << (*next_step)[index].native_location << ", event: "
            << (*next_step)[index].no_event );
    }

    return;
} // jvmti_SingleStepLocation

static void
jvmti_setup_jit_single_step(DebugUtilsTI *ti, Method* m, jlocation location)
{
    jvmti_thread_t jvmti_thread = jthread_self_jvmti();
    jvmti_StepLocation *locations;
    unsigned locations_count;

    // lock breakpoints
    VMBreakPoints* vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_brpt->get_lock());

    jvmti_SingleStepLocation(p_TLS_vmthread, m, (unsigned)location,
                            &locations, &locations_count);

    jvmti_remove_single_step_breakpoints(ti, jvmti_thread);

    jvmti_set_single_step_breakpoints(ti, jvmti_thread, locations, locations_count);
}

void
jvmti_set_single_step_breakpoints_for_method(DebugUtilsTI *ti,
                                             jvmti_thread_t jvmti_thread,
                                             Method *method)
{
    if (ti->isEnabled() && ti->is_single_step_enabled())
    {
        if (method->is_native()) // Must not single step through native methods
            return;

        LMAutoUnlock lock(ti->vm_brpt->get_lock());
        if (NULL != jvmti_thread->ss_state)
        {
            jvmti_remove_single_step_breakpoints(ti, jvmti_thread);

            jvmti_StepLocation method_start = {method, NULL, 0, false};
            jvmti_set_single_step_breakpoints(ti, jvmti_thread, &method_start, 1);
        }
    }
}

static void jvmti_start_single_step_in_virtual_method(DebugUtilsTI *ti, const VMBreakPoint* bp,
    const POINTER_SIZE_INT data, const unsigned char bytecode)
{
#if (defined _IA32_) || (defined _EM64T_)
    VM_thread *vm_thread = p_TLS_vmthread;
    assert(vm_thread);
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;
    assert(jvmti_thread);
    Registers regs = *(Registers*)(jvmti_thread->jvmti_saved_exception_registers);
    // This is a virtual breakpoint set exactly on the call
    // instruction for the virtual method. In this place it is
    // possible to determine the target method in runtime
    bool UNREF virtual_flag = (bool)data;
    assert(virtual_flag == true);

    InstructionDisassembler *disasm = bp->disasm;

    InstructionDisassembler::Type UNREF type = disasm->get_type();
    assert(type == InstructionDisassembler::RELATIVE_CALL ||
        type == InstructionDisassembler::INDIRECT_CALL);

    Global_Env * vm_env = jni_get_vm_env(vm_thread->jni_env);
    const InstructionDisassembler::Opnd& op = disasm->get_opnd(0);
    Method *method;

    // Disassembler points to the last CALL instruction inside of
    // invoke* bytecode. We assume that this CALL is a call to the
    // method. If target address can be found in methods lookup table,
    // then it is the breakpoint is set on its beginning. Otherwise
    // this is method resolution or compilation stub. Nothing shold be
    // done. Breakpoint will be inserted once the method is compiled.
    NativeCodePtr ip = disasm->get_target_address_from_context(&regs);
    Method_Handle mh = vm_env->em_interface->LookupCodeChunk(ip, FALSE,
        NULL, NULL, NULL);
    if (NULL != mh)
    {
        // Compiled method, set bytecode in it
        method = reinterpret_cast<Method *>(mh);
        assert(method->get_state() == Method::ST_Compiled);
    }
    else
        // Method is not compiled, nothing to do
        method = NULL;

#if 0
    // Gregory -
    // Here is commented the old implementation of single step prediction
    // for invokevirtual and invokeinterface bytecodes. Don't delete this
    // code, it may be still be used some day because it contains some
    // optimizations that don't require lookup in methods table
    if (bytecode == OPCODE_INVOKEVIRTUAL)
    {
        assert(op.kind == InstructionDisassembler::Kind_Mem ||
            op.kind == InstructionDisassembler::Kind_Reg);
        if (op.kind == InstructionDisassembler::Kind_Mem)
        {
            // Invokevirtual uses indirect call from VTable. The base
            // address is in the register, offset is in displacement *
            // scale.
            VTable* vtable = (VTable*)disasm->get_reg_value(op.base, &regs);
            assert(vtable);
            // For x86 based architectures offset cannot be longer than 32
            // bits, so unsigned is ok here
            unsigned offset = (unsigned)((POINTER_SIZE_INT)disasm->get_reg_value(op.index, &regs) *
                op.scale + op.disp);
            method = class_get_method_from_vt_offset(vtable, offset);
        }
        else if (op.kind == InstructionDisassembler::Kind_Reg)
        {
            // Call through a register, it has to point to the address of a compiled method
            NativeCodePtr ip = disasm->get_target_address_from_context(&regs);
            CodeChunkInfo *cci = vm_env->vm_methods->find(ip);
            if (NULL != cci)
            {
                // Compiled method, set bytecode in it
                method = cci->get_method();
                assert(method->get_state() == Method::ST_Compiled);
            }
            else
                // Method is not compiled, nothing to do
                method = NULL;
        }
    }
    else if (bytecode == OPCODE_INVOKEINTERFACE)
    {
        // This is invokeinterface bytecode which uses register
        // call so we need to search through all methods for this
        // one to find it, no way to get vtable and offset in it
        NativeCodePtr ip = disasm->get_target_address_from_context(&regs);
        CodeChunkInfo *cci = vm_env->vm_methods->find(ip);
        if (cci)
            method = cci->get_method();
        else
        {
            // This is an uncompiled interface method. We don't
            // know its address and don't know its handle. To get
            // the handle we need to parse LIL stub generated in
            // compile_gen_compile_me.
            InstructionDisassembler stub_disasm(ip);
#ifdef VM_STATS
            // In case of VM_STATS first instruction should be
            // skipped because it is a stats increment
            ip = (NativeCodePtr)((POINTER_SIZE_INT)ip + stub_disasm.get_length_with_prefix());
            stub_disasm = ip;
#endif
            // Now IP points on mov(stub, ecx_opnd, Imm_Opnd((I_32)method));
            // where method is the method handle. Need to get its
            // address from instruction, it is an immd operand in mov
            assert(stub_disasm.get_operands_count() == 1);

            const InstructionDisassembler::Opnd& stub_op = stub_disasm.get_opnd(0);
            assert(stub_op.kind == InstructionDisassembler::Kind_Imm);
            method = (Method *)((POINTER_SIZE_INT)stub_op.imm);
        }
    }
#endif // #if 0

    TRACE2("jvmti.break.ss", "Removing VIRTUAL single step breakpoint: " << bp->addr);

    // The determined method is the one which is called by
    // invokevirtual or invokeinterface bytecodes. It should be
    // started to be single stepped from the beginning
    if (NULL != method)
        jvmti_set_single_step_breakpoints_for_method(ti, jvmti_thread, method);
#else
    NOT_IMPLEMENTED;
#endif
}

// Callback function for JVMTI single step processing
static bool jvmti_process_jit_single_step_event(TIEnv* UNREF unused_env,
    const VMBreakPoint* bp, const POINTER_SIZE_INT data)
{
    assert(bp);

    TRACE2("jvmti.break.ss", "SingleStep occured: "
        << bp->method
        << " :" << bp->location << " :" << bp->addr);

    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;
    if (!ti->isEnabled() || ti->getPhase() != JVMTI_PHASE_LIVE)
        return false;

    ti->vm_brpt->lock();
    jvmti_thread_t jvmti_thread = jthread_self_jvmti();
    assert(jvmti_thread);
    JVMTISingleStepState* sss = jvmti_thread->ss_state;

    if (!sss || !ti->is_single_step_enabled()) {
        ti->vm_brpt->unlock();
        return false;
    }
    ti->vm_brpt->unlock();

    jlocation location = bp->location;
    jmethodID method = bp->method;
    Method* m = (Method*)method;
    NativeCodePtr addr = bp->addr;
    assert(addr);

    if ((bool)data)
    {
        const unsigned char *bytecode = reinterpret_cast<Method *>(method)->get_byte_code_addr();
        const unsigned char bc = bytecode[location];
        assert(bc == OPCODE_INVOKEINTERFACE || bc == OPCODE_INVOKEVIRTUAL);
        jvmti_start_single_step_in_virtual_method(ti, bp, data, bc);
        return true;
    }

    hythread_t h_thread = hythread_self();
    jthread j_thread = jthread_get_java_thread(h_thread);
    ObjectHandle hThread = oh_allocate_local_handle();
    hThread->object = (Java_java_lang_Thread *)j_thread->object;
    tmn_suspend_enable();

    JNIEnv *jni_env = p_TLS_vmthread->jni_env;
    TIEnv *env = ti->getEnvironments();
    TIEnv *next_env;

    while (NULL != env)
    {
        next_env = env->next;

        jvmtiEventSingleStep func =
            (jvmtiEventSingleStep)env->get_event_callback(JVMTI_EVENT_SINGLE_STEP);

        if (NULL != func)
        {
            if (env->global_events[JVMTI_EVENT_SINGLE_STEP - JVMTI_MIN_EVENT_TYPE_VAL])
            {
                TRACE2("jvmti.break.ss",
                    "Calling JIT global SingleStep breakpoint callback: "
                    << method << " :" << location << " :" << addr);

                // fire global event
                func((jvmtiEnv*)env, jni_env, (jthread)hThread, method, location);

                TRACE2("jvmti.break.ss",
                    "Finished JIT global SingleStep breakpoint callback: "
                    << method << " :" << location << " :" << addr);

                env = next_env;
                continue;
            }

            TIEventThread* next_et;
            bool found = false;
            // fire local events
            for (TIEventThread* et = env->event_threads[JVMTI_EVENT_SINGLE_STEP - JVMTI_MIN_EVENT_TYPE_VAL];
                 et != NULL; et = next_et)
            {
                next_et = et->next;

                if (et->thread == hythread_self())
                {
                    TRACE2("jvmti.break.ss",
                        "Calling JIT local SingleStep breakpoint callback: "
                        << method << " :" << location << " :" << addr);

                    found = true;

                    func((jvmtiEnv*)env, jni_env,
                        (jthread)hThread, method, location);

                    TRACE2("jvmti.break.ss",
                        "Finished JIT local SingleStep breakpoint callback: "
                        << method << " :" << location << " :" << addr);
                }
            }

            env = next_env;
        }
    }

    // Set breakpoints on bytecodes after the current one
    if (ti->is_single_step_enabled())
        jvmti_setup_jit_single_step(ti, m, location);

    oh_discard_local_handle(hThread);
    hythread_exception_safe_point();
    tmn_suspend_disable();

    return true;
}

void jvmti_set_single_step_breakpoints(DebugUtilsTI *ti, jvmti_thread_t jvmti_thread,
    jvmti_StepLocation *locations, unsigned locations_number)
{
    // Function is always executed under global TI breakpoints lock
    ASSERT_NO_INTERPRETER;

    JVMTISingleStepState *ss_state = jvmti_thread->ss_state;
    if( NULL == ss_state ) {
        // no need predict next step due to single step is off
        return;
    }

    if (NULL == ss_state->predicted_breakpoints)
    {
        // Create SS breakpoints list
        // Single Step must be processed earlier then Breakpoints
        ss_state->predicted_breakpoints =
            ti->vm_brpt->new_intf( NULL, jvmti_process_jit_single_step_event,
                PRIORITY_SINGLE_STEP_BREAKPOINT, false);
        assert(ss_state->predicted_breakpoints);
    }

    for (unsigned iii = 0; iii < locations_number; iii++)
    {
        TRACE2("jvmti.break.ss", "Set single step breakpoint: "
            << locations[iii].method
            << " :" << locations[iii].location
            << " :" << locations[iii].native_location);

        VMBreakPointRef* ref =
            ss_state->predicted_breakpoints->add_reference(
                (jmethodID)locations[iii].method,
                locations[iii].location,
                locations[iii].native_location,
                (POINTER_SIZE_INT)locations[iii].no_event);
        assert(ref);
    }
}

void jvmti_remove_single_step_breakpoints(DebugUtilsTI *ti, jvmti_thread_t jvmti_thread)
{
    // Function is always executed under global TI breakpoints lock
    JVMTISingleStepState *ss_state = jvmti_thread->ss_state;

    TRACE2("jvmti.break.ss", "Remove single step breakpoints, intf: "
        << (ss_state ? ss_state->predicted_breakpoints : NULL) );

    if (ss_state && ss_state->predicted_breakpoints)
        ss_state->predicted_breakpoints->remove_all_reference();
}

jvmtiError jvmti_get_next_bytecodes_from_native(VM_thread *thread,
    jvmti_StepLocation **next_step,
    unsigned *count,
    bool invoked_frame)
{
#if (defined _IA32_) || (defined _EM64T_)
    ASSERT_NO_INTERPRETER;
    VMBreakPoints *vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;

    *count = 0;
    // create stack iterator, current stack frame should be native
    StackIterator *si = si_create_from_native(thread);
    si_transfer_all_preserved_registers(si);
    assert(si_is_native(si));
    // get previous stack frame, it should be java frame
    si_goto_previous(si);

    if (si_is_past_end(si))
    {
        si_free(si);
        return JVMTI_ERROR_NONE;
    }

    if (si_is_native(si))
    {
        // We are called from native code, nothing to be done
        // here. The bytecode which called us will be instrumented
        // when we return back to java in vm_execute_java_method_array
        return JVMTI_ERROR_NONE;
    }

    if( invoked_frame ) {
        // get previous stack frame
        si_goto_previous(si);
    }
    if (!si_is_native(si)) {
        // stack frame is java frame, get frame method and location
        uint16 bc = 0;
        CodeChunkInfo *cci = si_get_code_chunk_info(si);
        Method *func = cci->get_method();
        NativeCodePtr ip = si_get_ip(si);
        JIT *jit = cci->get_jit();
        OpenExeJpdaError UNREF result =
                    jit->get_bc_location_for_native(func, ip, &bc);
        assert(result == EXE_ERROR_NONE);
        TRACE2( "jvmti.break.ss", "SingleStep method IP: " << ip );

        // In case stack iterator points to invoke (in invoked_frame)
        // case the IP may point to an instruction after a call, but
        // still on the invoke* bytecode. It can be found out by
        // iterating through instructions inside of the same
        // bytecode. If we find a call in it, then we're on a correct
        // bytecode, if not, we're on a tail of an invoke*
        // instruction. It is necessary to move one bytecode ahead in
        // this case.
        if (invoked_frame)
        {
            // Determine if the found bytecode if of an invoke type
            const unsigned char *bytecode = func->get_byte_code_addr();
            uint16 next_location = 0;

            switch (bytecode[bc])
            {
            case OPCODE_INVOKEINTERFACE: /* 0xb9 + u2 + u1 + u1 */
                next_location = bc + 5;
                break;
            case OPCODE_INVOKESPECIAL:   /* 0xb7 + u2 */
            case OPCODE_INVOKESTATIC:    /* 0xb8 + u2 */
            case OPCODE_INVOKEVIRTUAL:   /* 0xb6 + u2 */
                next_location = bc + 3;
                break;
            }

            NativeCodePtr ip2 = ip;
            // Yes this is an invoke type bytecode
            if (next_location)
            {
                NativeCodePtr next_ip;
                OpenExeJpdaError UNREF result = jit->get_native_location_for_bc(func,
                    next_location, &next_ip);
                assert(result == EXE_ERROR_NONE);
                assert(ip2 < next_ip);

                VMBreakPoint *bp = vm_brpt->find_breakpoint(ip2);

                InstructionDisassembler disasm;
                if (bp)
                    disasm = *bp->disasm;
                else
                    disasm = ip2;

                NativeCodePtr call_ip = NULL;
                do
                {
                    // Bytecode may be either invokevirtual or
                    // invokeinterface which generate indirect calls or
                    // invokestatic or invokespecial which generate
                    // relative calls
                    if (disasm.get_type() == InstructionDisassembler::INDIRECT_CALL ||
                        disasm.get_type() == InstructionDisassembler::RELATIVE_CALL)
                        call_ip = ip2;

                    ip2 = (NativeCodePtr)((POINTER_SIZE_INT)ip2 + disasm.get_length_with_prefix());

                    // Another thread could have instrumented this location for
                    // prediction of invokevirtual or invokeinterface, so it is
                    // necessary to check that location may be instrumented
                    bp = vm_brpt->find_breakpoint(ip2);
                    if (bp)
                        disasm = *bp->disasm;
                    else
                        disasm = ip2;
                }
                while (ip2 < next_ip);

                // We've found no call instruction in this
                // bytecode. This means we're standing on the tail of
                // invoke. Need to shift to the next bytecode
                if (NULL == call_ip)
                {
                    TRACE2("jvmti.break.ss", "SingleStep IP shifted in prediction to: "
                        << func << " :" << next_location << " :" << ip2);
                    bc = next_location;
                    ip = ip2;
                }
            }
            // No this is not an invoke type bytecode, so the IP
            // points to a normal bytecode after invoke. No need to
            // shift to the next one.

            // set step location structure
            *count = 1;
            jvmtiError error = _allocate( sizeof(jvmti_StepLocation), (unsigned char**)next_step );
            if( error != JVMTI_ERROR_NONE ) {
                si_free(si);
                return error;
            }
            (*next_step)->method = func;
            // IP in stack iterator points to a bytecode next after the one
            // which caused call of the method. So next location is the 'bc' which
            // IP points to.
            (*next_step)->location = bc;
            (*next_step)->native_location = ip;
            (*next_step)->no_event = false;
        }
        else
        {
            // Find next bytecode after the one we're currently
            // standing on
            jvmti_SingleStepLocation(thread, func, bc, next_step, count);
        }
    }
    si_free(si);
#else
    NOT_IMPLEMENTED;
#endif
    return JVMTI_ERROR_NONE;
} // jvmti_get_next_bytecodes_from_native

jvmtiError DebugUtilsTI::jvmti_single_step_start(void)
{
    assert(hythread_is_suspend_enabled());

    hythread_iterator_t threads_iterator;

    if( single_step_enabled ) {
        // single step is already enabled
        return JVMTI_ERROR_NONE;
    }

    // Suspend all threads except current
    IDATA tm_ret = hythread_suspend_all(&threads_iterator, NULL);
    if (TM_ERROR_NONE != tm_ret)
        return JVMTI_ERROR_INTERNAL;

    // get this lock for insurance - all threads are suspended
    VMBreakPoints* vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_brpt->get_lock());

    if( single_step_enabled ) {
        // single step is already enabled
        tm_ret = hythread_resume_all(NULL);
        if (TM_ERROR_NONE != tm_ret)
            return JVMTI_ERROR_INTERNAL;
        return JVMTI_ERROR_NONE;
    }

    hythread_t ht;

    // Set single step in all threads
    while ((ht = hythread_iterator_next(&threads_iterator)) != NULL)
    {
        vm_thread_t vm_thread = jthread_get_vm_thread(ht);
        if( !vm_thread ) {
            // Skip thread that isn't started yet.
            // SingleStep state will be enabled for it in
            // jvmti_send_thread_start_end_event()
            continue;
        }
        jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;
        if( !jvmti_thread ) {
            // Skip thread that isn't started yet.
            // SingleStep state will be enabled for it in
            // jvmti_send_thread_start_end_event
            continue;
        }

        // Init single step state for the thread
        jvmtiError errorCode = _allocate(sizeof(JVMTISingleStepState),
            (unsigned char **)&jvmti_thread->ss_state);

        if (JVMTI_ERROR_NONE != errorCode)
        {
            hythread_resume_all(NULL);
            return errorCode;
        }

        jvmti_thread->ss_state->predicted_breakpoints = NULL;

        jvmti_StepLocation *locations;
        unsigned locations_number;

        errorCode = jvmti_get_next_bytecodes_from_native(
            vm_thread, &locations, &locations_number, false);

        if (JVMTI_ERROR_NONE != errorCode)
        {
            hythread_resume_all(NULL);
            return errorCode;
        }

        jvmti_set_single_step_breakpoints(this, jvmti_thread, locations, locations_number);
    }
    
    single_step_enabled = true;

    tm_ret = hythread_resume_all(NULL);
    if (TM_ERROR_NONE != tm_ret)
        return JVMTI_ERROR_INTERNAL;

    return JVMTI_ERROR_NONE;
}

jvmtiError DebugUtilsTI::jvmti_single_step_stop(void)
{
    assert(hythread_is_suspend_enabled());

    hythread_iterator_t threads_iterator;

    if( !single_step_enabled ) {
        // single step is already disabled
        return JVMTI_ERROR_NONE;
    }

    // Suspend all threads except current
    IDATA tm_ret = hythread_suspend_all(&threads_iterator, NULL);
    if (TM_ERROR_NONE != tm_ret)
        return JVMTI_ERROR_INTERNAL;

    // get this lock for insurance - all threads are suspended
    VMBreakPoints* vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_brpt->get_lock());

    if( !single_step_enabled ) {
        // single step is already disabled
        tm_ret = hythread_resume_all(NULL);
        if (TM_ERROR_NONE != tm_ret)
            return JVMTI_ERROR_INTERNAL;
        return JVMTI_ERROR_NONE;
    }

    hythread_t ht;

    // Clear single step in all threads
    while ((ht = hythread_iterator_next(&threads_iterator)) != NULL)
    {
        jvmti_thread_t jvmti_thread = jthread_get_jvmti_thread(ht);
        if( !jvmti_thread ) {
            // Skip thread that isn't started yet. No need to disable
            // SingleStep state for it
            continue;
        }

        if( jvmti_thread->ss_state ) {
            jvmti_remove_single_step_breakpoints(this, jvmti_thread);
            if( jvmti_thread->ss_state->predicted_breakpoints ) {
                vm_brpt->release_intf(jvmti_thread->ss_state->predicted_breakpoints);
            }
            _deallocate((unsigned char *)jvmti_thread->ss_state);
            jvmti_thread->ss_state = NULL;
        }
    }

    single_step_enabled = false;

    tm_ret = hythread_resume_all(NULL);
    if (TM_ERROR_NONE != tm_ret)
        return JVMTI_ERROR_INTERNAL;

    return JVMTI_ERROR_NONE;
}

static unsigned
jvmti_GetNextBytecodeLocation( Method *method,
                               unsigned location)
{
    assert( location < method->get_byte_code_size() );
    const unsigned char *bytecode = method->get_byte_code_addr();
    bool is_wide = false;
    do {
        switch( bytecode[location] )
        {
        case OPCODE_WIDE:           /* 0xc4 */
            assert( !is_wide );
            location++;
            is_wide = true;
            continue;

        case OPCODE_TABLESWITCH:    /* 0xaa + pad + s4 * (3 + N) */
            assert( !is_wide );
            location = (location + 4)&(~0x3U);
            {
                int low = jvmti_GetWordValue( bytecode, location + 4 );
                int high = jvmti_GetWordValue( bytecode, location + 8 );
                return location + 4 * (high - low + 4);
            }

        case OPCODE_LOOKUPSWITCH:   /* 0xab + pad + s4 * 2 * (N + 1) */
            assert( !is_wide );
            location = (location + 4)&(~0x3U);
            {
                int number = jvmti_GetWordValue( bytecode, location + 4 ) + 1;
                return location + 8 * number;
            }

        case OPCODE_IINC:           /* 0x84 + u1|u2 + s1|s2 */
            if( is_wide ) {
                return location + 5;
            } else {
                return location + 3;
            }

        case OPCODE_GOTO_W:         /* 0xc8 + s4 */
        case OPCODE_JSR_W:          /* 0xc9 + s4 */
        case OPCODE_INVOKEINTERFACE:/* 0xb9 + u2 + u1 + u1 */
            assert( !is_wide );
            return location + 5;

        case OPCODE_MULTIANEWARRAY: /* 0xc5 + u2 + u1 */
            assert( !is_wide );
            return location + 4;

        case OPCODE_ILOAD:          /* 0x15 + u1|u2 */
        case OPCODE_LLOAD:          /* 0x16 + u1|u2 */
        case OPCODE_FLOAD:          /* 0x17 + u1|u2 */
        case OPCODE_DLOAD:          /* 0x18 + u1|u2 */
        case OPCODE_ALOAD:          /* 0x19 + u1|u2 */
        case OPCODE_ISTORE:         /* 0x36 + u1|u2 */
        case OPCODE_LSTORE:         /* 0x37 + u1|u2 */
        case OPCODE_FSTORE:         /* 0x38 + u1|u2 */
        case OPCODE_DSTORE:         /* 0x39 + u1|u2 */
        case OPCODE_ASTORE:         /* 0x3a + u1|u2 */
        case OPCODE_RET:            /* 0xa9 + u1|u2  */
            if( is_wide ) {
                return location + 3;
            } else {
                return location + 2;
            }

        case OPCODE_SIPUSH:         /* 0x11 + s2 */
        case OPCODE_LDC_W:          /* 0x13 + u2 */
        case OPCODE_LDC2_W:         /* 0x14 + u2 */
        case OPCODE_IFEQ:           /* 0x99 + s2 */
        case OPCODE_IFNE:           /* 0x9a + s2 */
        case OPCODE_IFLT:           /* 0x9b + s2 */
        case OPCODE_IFGE:           /* 0x9c + s2 */
        case OPCODE_IFGT:           /* 0x9d + s2 */
        case OPCODE_IFLE:           /* 0x9e + s2 */
        case OPCODE_IF_ICMPEQ:      /* 0x9f + s2 */
        case OPCODE_IF_ICMPNE:      /* 0xa0 + s2 */
        case OPCODE_IF_ICMPLT:      /* 0xa1 + s2 */
        case OPCODE_IF_ICMPGE:      /* 0xa2 + s2 */
        case OPCODE_IF_ICMPGT:      /* 0xa3 + s2 */
        case OPCODE_IF_ICMPLE:      /* 0xa4 + s2 */
        case OPCODE_IF_ACMPEQ:      /* 0xa5 + s2 */
        case OPCODE_IF_ACMPNE:      /* 0xa6 + s2 */
        case OPCODE_GOTO:           /* 0xa7 + s2 */
        case OPCODE_GETSTATIC:      /* 0xb2 + u2 */
        case OPCODE_PUTSTATIC:      /* 0xb3 + u2 */
        case OPCODE_GETFIELD:       /* 0xb4 + u2 */
        case OPCODE_PUTFIELD:       /* 0xb5 + u2 */
        case OPCODE_INVOKEVIRTUAL:  /* 0xb6 + u2 */
        case OPCODE_INVOKESPECIAL:  /* 0xb7 + u2 */
        case OPCODE_JSR:            /* 0xa8 + s2 */
        case OPCODE_INVOKESTATIC:   /* 0xb8 + u2 */
        case OPCODE_NEW:            /* 0xbb + u2 */
        case OPCODE_ANEWARRAY:      /* 0xbd + u2 */
        case OPCODE_CHECKCAST:      /* 0xc0 + u2 */
        case OPCODE_INSTANCEOF:     /* 0xc1 + u2 */
        case OPCODE_IFNULL:         /* 0xc6 + s2 */
        case OPCODE_IFNONNULL:      /* 0xc7 + s2 */
            assert( !is_wide );
            return location + 3;

        case OPCODE_BIPUSH:         /* 0x10 + s1 */
        case OPCODE_LDC:            /* 0x12 + u1 */
        case OPCODE_NEWARRAY:       /* 0xbc + u1 */
            assert( !is_wide );
            return location + 2;

        default:
            assert( !is_wide );
            assert( bytecode[location] < OPCODE_COUNT );
            assert( bytecode[location] != _OPCODE_UNDEFINED );
            return location + 1;
        }
        break;
    } while( true );
    return 0;
} // jvmti_GetNextBytecodeLocation

void
jvmti_dump_compiled_method(Method *method)
{
    unsigned location = 0;
    unsigned bc_number = 0;

    do
    {
        OpenExeJpdaError res = EXE_ERROR_UNSUPPORTED;
        NativeCodePtr native_location = NULL;
        for (CodeChunkInfo* cci = method->get_first_JIT_specific_info(); cci; cci = cci->_next)
        {
            JIT *jit = cci->get_jit();
            res = jit->get_native_location_for_bc(method,
                location, &native_location);
            if (res == EXE_ERROR_NONE)
                break;
        }
        assert(res == EXE_ERROR_NONE);

        ECHO("bytecode " << bc_number << ": "
            << location << " = " << native_location);

        location = jvmti_GetNextBytecodeLocation(method, location);
        bc_number++;
    }
    while(location < method->get_byte_code_size());
}
