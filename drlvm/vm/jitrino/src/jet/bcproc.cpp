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
 * @author Alexander V. Astapchuk
 */
#include "compiler.h"
#include "trace.h"
#include "stats.h"

#include "open/vm_class_info.h"
#include "open/vm_class_loading.h"
#include "jit_import.h"
//#include "jit_intf.h"
#include "open/vm_ee.h"

/**
 * @file
 * @brief Mostly a huge switch(OPCODE), separated to several groups.
 */

namespace Jitrino {
namespace Jet {


void Compiler::handle_inst(void)
{
    // is it last instruction in basic block ?
    //const bool last = m_bbinfo->last_pc == jinst.pc;
    const JInst& jinst = m_insts[m_pc];
    unsigned bc_size = m_infoBlock.get_bc_size();
    bool lastInBB = jinst.next>=bc_size || (m_insts[jinst.next].flags & OPF_STARTS_BB);
    
    if (is_set(DBG_CHECK_STACK)) {
        gen_dbg_check_stack(true);
    }

    // First test if this is a magic. If not, then proceed with regular
    // code gen.
    if (!gen_magic()) {
        const InstrDesc& idesc = instrs[jinst.opcode];
        switch (idesc.ik) {
            case ik_a:
                handle_ik_a(jinst);
                break;
            case ik_cf:
                handle_ik_cf(jinst);
                break;
            case ik_cnv:
                handle_ik_cnv(jinst);
                break;
            case ik_ls:
                handle_ik_ls(jinst);
                break;
            case ik_meth:
                handle_ik_meth(jinst);
                break;
            case ik_obj:
                handle_ik_obj(jinst);
                break;
            case ik_stack:
                handle_ik_stack(jinst);
                break;
            case ik_throw:
                gen_athrow();
                break;
            default:
                assert(jinst.opcode == OPCODE_NOP);
                break;
        } // ~switch(opcodegroup)
    } else {  // if (!gen_magic()) {
        // no op. Just check stack (if applicable) and do mem manipulations
    }

    if (is_set(DBG_CHECK_STACK)) {
        gen_dbg_check_stack(false);
    }

    if (g_jvmtiMode) {
        // Do not allow values to cross instruction boundaries
        // on a temporary registers
        vpark();
        // We must have GC info at every bytecode instruction
        // to support possible enumeration at a breakpoint 
        gen_gc_stack(-1, false);
    }

    const bool has_fall_through = !jinst.is_set(OPF_DEAD_END);
    if (lastInBB && has_fall_through && jinst.get_num_targets() == 0) {
        gen_bb_leave(jinst.next);
    }
}

void Compiler::handle_ik_a(const JInst& jinst) {

    if (jinst.opcode == OPCODE_IINC) {
        gen_iinc(jinst.op0, jinst.op1);
        return;
    }

    switch(jinst.opcode) {
    case OPCODE_LCMP:
        gen_x_cmp(jinst.opcode, i64);
        return;
    case OPCODE_FCMPL:
    case OPCODE_FCMPG:
        gen_x_cmp(jinst.opcode, flt32);
        return;
    case OPCODE_DCMPL:
    case OPCODE_DCMPG:
        gen_x_cmp(jinst.opcode, dbl64);
        return;
    default:    break;
    }

    jtype opnd = jvoid;
    JavaByteCodes inst = jinst.opcode;

    switch(jinst.opcode) {
    case OPCODE_IADD: opnd = i32; inst = OPCODE_IADD; break;
    case OPCODE_LADD: opnd = i64; inst = OPCODE_IADD; break;
    case OPCODE_FADD: opnd = flt32; inst = OPCODE_IADD; break;
    case OPCODE_DADD: opnd = dbl64; inst = OPCODE_IADD; break;

    case OPCODE_ISUB: opnd = i32; inst = OPCODE_ISUB; break;
    case OPCODE_LSUB: opnd = i64; inst = OPCODE_ISUB; break;
    case OPCODE_FSUB: opnd = flt32; inst = OPCODE_ISUB; break;
    case OPCODE_DSUB: opnd = dbl64; inst = OPCODE_ISUB; break;

    case OPCODE_IMUL: opnd = i32; inst = OPCODE_IMUL; break;
    case OPCODE_LMUL: opnd = i64; inst = OPCODE_IMUL; break;
    case OPCODE_FMUL: opnd = flt32; inst = OPCODE_IMUL; break;
    case OPCODE_DMUL: opnd = dbl64; inst = OPCODE_IMUL; break;

    case OPCODE_IDIV: opnd = i32; inst = OPCODE_IDIV; break;
    case OPCODE_LDIV: opnd = i64; inst = OPCODE_IDIV; break;
    case OPCODE_FDIV: opnd = flt32; inst = OPCODE_IDIV; break;
    case OPCODE_DDIV: opnd = dbl64; inst = OPCODE_IDIV; break;

    case OPCODE_IREM: opnd = i32; inst = OPCODE_IREM; break;
    case OPCODE_LREM: opnd = i64; inst = OPCODE_IREM; break;
    case OPCODE_FREM: opnd = flt32; inst = OPCODE_IREM; break;
    case OPCODE_DREM: opnd = dbl64; inst = OPCODE_IREM; break;

    case OPCODE_INEG: opnd = i32; inst = OPCODE_INEG; break;
    case OPCODE_LNEG: opnd = i64; inst = OPCODE_INEG; break;
    case OPCODE_FNEG: opnd = flt32; inst = OPCODE_INEG; break;
    case OPCODE_DNEG: opnd = dbl64; inst = OPCODE_INEG; break;

    case OPCODE_ISHL: opnd = i32; inst = OPCODE_ISHL; break;
    case OPCODE_LSHL: opnd = i64; inst = OPCODE_ISHL; break;

    case OPCODE_ISHR: opnd = i32; inst = OPCODE_ISHR; break;
    case OPCODE_LSHR: opnd = i64; inst = OPCODE_ISHR; break;

    case OPCODE_IUSHR: opnd = i32; inst = OPCODE_IUSHR; break;
    case OPCODE_LUSHR: opnd = i64; inst = OPCODE_IUSHR; break;

    case OPCODE_IAND: opnd = i32; inst = OPCODE_IAND; break;
    case OPCODE_LAND: opnd = i64; inst = OPCODE_IAND; break;

    case OPCODE_IOR: opnd = i32; inst = OPCODE_IOR; break;
    case OPCODE_LOR: opnd = i64; inst = OPCODE_IOR; break;

    case OPCODE_IXOR: opnd = i32; inst = OPCODE_IXOR; break;
    case OPCODE_LXOR: opnd = i64; inst = OPCODE_IXOR; break;
    default: assert(false); break;
    }

    if ((inst == OPCODE_IDIV || inst == OPCODE_IREM ) && 
        (opnd == i32 || opnd == i64)) {
        gen_check_div_by_zero(opnd, 0);
    }
    gen_a(inst, opnd);
}

void Compiler::handle_ik_cf(const JInst& jinst) {
    switch(jinst.opcode) {
    case OPCODE_IFNULL:
    case OPCODE_IFNONNULL:
    case OPCODE_IFEQ:
    case OPCODE_IFNE:
    case OPCODE_IFLT:
    case OPCODE_IFGE:
    case OPCODE_IFGT:
    case OPCODE_IFLE:
        gen_if(jinst.opcode, jinst.get_target(0));
        break;
    case OPCODE_IF_ACMPEQ:
    case OPCODE_IF_ACMPNE:
    case OPCODE_IF_ICMPEQ:
    case OPCODE_IF_ICMPNE:
    case OPCODE_IF_ICMPLT:
    case OPCODE_IF_ICMPGE:
    case OPCODE_IF_ICMPGT:
    case OPCODE_IF_ICMPLE:
        gen_if_icmp(jinst.opcode, jinst.get_target(0));
        break;
    case OPCODE_GOTO:
    case OPCODE_GOTO_W:
        gen_goto(jinst.get_target(0));
        break;
    case OPCODE_JSR:
    case OPCODE_JSR_W:
        gen_jsr(jinst.get_target(0));
        break;
    case OPCODE_RET:
        gen_ret(jinst.op0);
        break;
    case OPCODE_TABLESWITCH:
    case OPCODE_LOOKUPSWITCH:
        gen_switch(jinst);
        break;
    default: assert(false); break;
    }
}


void Compiler::handle_ik_cnv(const JInst& jinst)
{
    jtype from, to;
    switch(jinst.opcode) {
    case OPCODE_I2B:    from = i32; to = i8;  break;
    case OPCODE_I2C:    from = i32; to = u16; break;
    case OPCODE_I2S:    from = i32; to = i16; break;
    case OPCODE_I2L:    from = i32; to = i64;   break;
    case OPCODE_I2F:    from = i32; to = flt32; break;
    case OPCODE_I2D:    from = i32; to = dbl64; break;

    case OPCODE_L2I:    from = i64; to = i32;   break;
    case OPCODE_L2F:    from = i64; to = flt32; break;
    case OPCODE_L2D:    from = i64; to = dbl64; break;

    case OPCODE_F2I:    from = flt32; to = i32;   break;
    case OPCODE_F2L:    from = flt32; to = i64;   break;
    case OPCODE_F2D:    from = flt32; to = dbl64; break;

    case OPCODE_D2I:    from = dbl64; to = i32;   break;
    case OPCODE_D2L:    from = dbl64; to = i64;   break;
    case OPCODE_D2F:    from = dbl64; to = flt32; break;

    default: assert(false); from = to = jvoid; break;
    }// ~switch opcode
    gen_cnv(from, to);
}

void Compiler::handle_ik_ls(const JInst& jinst) {
    switch(jinst.opcode) {
    case OPCODE_ICONST_M1:  gen_push((int)-1); break;
    case OPCODE_ICONST_0:   gen_push((int)0);   break;
    case OPCODE_ICONST_1:   gen_push((int)1);   break;
    case OPCODE_ICONST_2:   gen_push((int)2);   break;
    case OPCODE_ICONST_3:   gen_push((int)3);   break;
    case OPCODE_ICONST_4:   gen_push((int)4);   break;
    case OPCODE_ICONST_5:   gen_push((int)5);   break;

    case OPCODE_LCONST_0:   gen_push((jlong)0); break;
    case OPCODE_LCONST_1:   gen_push((jlong)1); break;

    case OPCODE_FCONST_0:   gen_push(flt32, &g_fconst_0); break;
    case OPCODE_FCONST_1:   gen_push(flt32, &g_fconst_1); break;
    case OPCODE_FCONST_2:   gen_push(flt32, &g_fconst_2); break;

    case OPCODE_DCONST_0:   gen_push(dbl64, &g_dconst_0); break;
    case OPCODE_DCONST_1:   gen_push(dbl64, &g_dconst_1); break;

    case OPCODE_LDC:
    case OPCODE_LDC_W:
    case OPCODE_LDC2_W:
        gen_ldc();
        break;
    case OPCODE_SIPUSH:
        gen_push((int)(short)(unsigned short)jinst.op0);
        break;
    case OPCODE_BIPUSH:
        gen_push((int)(char)(unsigned char)jinst.op0);
        break;

    case OPCODE_ASTORE:
        assert(m_jframe->top() == jobj || m_jframe->top() == jretAddr);
        gen_st(m_jframe->top(), jinst.op0);
        break;
    case OPCODE_ISTORE:     gen_st(i32, jinst.op0); break;
    case OPCODE_LSTORE:     gen_st(i64, jinst.op0); break;

    case OPCODE_FSTORE:     gen_st(flt32, jinst.op0); break;

    case OPCODE_DSTORE:     gen_st(dbl64, jinst.op0); break;

    case OPCODE_ISTORE_0:
    case OPCODE_ISTORE_1:
    case OPCODE_ISTORE_2:
    case OPCODE_ISTORE_3:
        gen_st(i32, jinst.opcode-OPCODE_ISTORE_0);
        break;
    case OPCODE_LSTORE_0:
    case OPCODE_LSTORE_1:
    case OPCODE_LSTORE_2:
    case OPCODE_LSTORE_3:
        gen_st(i64, jinst.opcode-OPCODE_LSTORE_0);
        break;
    case OPCODE_FSTORE_0:
    case OPCODE_FSTORE_1:
    case OPCODE_FSTORE_2:
    case OPCODE_FSTORE_3:
        gen_st(flt32, jinst.opcode-OPCODE_FSTORE_0);
        break;
    case OPCODE_DSTORE_0:
    case OPCODE_DSTORE_1:
    case OPCODE_DSTORE_2:
    case OPCODE_DSTORE_3:
        gen_st(dbl64, jinst.opcode-OPCODE_DSTORE_0);
        break;
    case OPCODE_ASTORE_0:
    case OPCODE_ASTORE_1:
    case OPCODE_ASTORE_2:
    case OPCODE_ASTORE_3:
        assert(m_jframe->top() == jobj || m_jframe->top() == jretAddr);
        gen_st(m_jframe->top(), jinst.opcode-OPCODE_ASTORE_0);
        break;

    case OPCODE_ILOAD:      gen_ld(i32, jinst.op0);   break;
    case OPCODE_LLOAD:      gen_ld(i64, jinst.op0);   break;

    case OPCODE_FLOAD:      gen_ld(flt32, jinst.op0); break;

    case OPCODE_DLOAD:      gen_ld(dbl64, jinst.op0); break;
    case OPCODE_ALOAD:      gen_ld(jobj, jinst.op0);  break;

    case OPCODE_ILOAD_0:
    case OPCODE_ILOAD_1:
    case OPCODE_ILOAD_2:
    case OPCODE_ILOAD_3:
        gen_ld(i32, jinst.opcode-OPCODE_ILOAD_0);
        break;
    case OPCODE_LLOAD_0:
    case OPCODE_LLOAD_1:
    case OPCODE_LLOAD_2:
    case OPCODE_LLOAD_3:
        gen_ld(i64, jinst.opcode-OPCODE_LLOAD_0);
        break;
    case OPCODE_FLOAD_0:
    case OPCODE_FLOAD_1:
    case OPCODE_FLOAD_2:
    case OPCODE_FLOAD_3:
        gen_ld(flt32, jinst.opcode-OPCODE_FLOAD_0);
        break;
    case OPCODE_DLOAD_0:
    case OPCODE_DLOAD_1:
    case OPCODE_DLOAD_2:
    case OPCODE_DLOAD_3:
        gen_ld(dbl64, jinst.opcode-OPCODE_DLOAD_0);
        break;
    case OPCODE_ALOAD_0:
    case OPCODE_ALOAD_1:
    case OPCODE_ALOAD_2:
    case OPCODE_ALOAD_3:
        gen_ld(jobj, jinst.opcode-OPCODE_ALOAD_0);
        break;
    case OPCODE_ACONST_NULL:
        gen_push(jobj, NULL_REF);
        break;
    default:    assert(false); break;
    }
}

void Compiler::handle_ik_meth(const JInst& jinst) {
    if (jinst.opcode == OPCODE_INVOKESTATIC || 
        jinst.opcode == OPCODE_INVOKESPECIAL ||
        jinst.opcode == OPCODE_INVOKEVIRTUAL || 
        jinst.opcode == OPCODE_INVOKEINTERFACE) {
        
       
        JavaByteCodes opkod = jinst.opcode;
        ::std::vector<jtype> args;
        jtype retType;
        bool is_static = opkod == OPCODE_INVOKESTATIC;
        get_args_info(is_static, jinst.op0, args, &retType);
        
        Method_Handle meth = NULL;
        unsigned short cpIndex = (unsigned short)jinst.op0;
        bool lazy = m_lazy_resolution;
        bool resolve = !lazy || class_cp_is_entry_resolved(m_klass, cpIndex);
        if (!resolve) {
            assert(lazy);
            gen_invoke(opkod, NULL, cpIndex, args, retType);
            return;
        }
        if (opkod == OPCODE_INVOKESTATIC) {
            meth = resolve_static_method(m_compileHandle, m_klass,
                                            jinst.op0);
            if (meth != NULL) {
                Class_Handle klass = method_get_class(meth);
                if (!class_is_initialized(klass)) {
                    gen_call_vm(ci_helper_o, rt_helper_init_class, 0, klass);
                }
            }
        }
        else if (opkod == OPCODE_INVOKEVIRTUAL) {
            meth = resolve_virtual_method(m_compileHandle, m_klass,
                                            jinst.op0);
        }
        else if (opkod == OPCODE_INVOKEINTERFACE) {
            // BUG and HACK - all in one:
            // An 'org/eclipse/ui/keys/KeyStroke::hashCode' (e3.0) does
            // invokeinterface on java/util/SortedSet::hashCode(), but the
            // entry get resolved into the 'java/lang/Object::hashCode' !
            // later: for eclipse 3.1.1 the same problem happens with 
            // org/eclipse/jdt/internal/core/JavaProject::equals
            // which tries to resolve 
            //  'org/eclipse/core/resources/IProject::equals (Ljava/lang/Object;)Z'
            meth = resolve_interface_method(m_compileHandle, m_klass, jinst.op0);
            //
            //*** workaround here:
            if (meth != NULL && 
                !class_is_interface(method_get_class(meth))) {
                opkod = OPCODE_INVOKEVIRTUAL;
            }
        }
        else {
            assert(opkod == OPCODE_INVOKESPECIAL);
            meth = resolve_special_method(m_compileHandle, m_klass, jinst.op0);
        }
        // if class to call to is available, but method is not found in the class
        // meth here will be equal to NULL and lazy resolution call will be
        // generated in gen_invoke
        gen_invoke(opkod, meth, cpIndex, args, retType);
        return;
    }
    switch(jinst.opcode) {
    case OPCODE_IRETURN: {
        SYNC_FIRST(static const CallSig cs(CCONV_MANAGED, i32));
        gen_return(cs);
        break;
    }
    case OPCODE_LRETURN: {
        SYNC_FIRST(static const CallSig cs(CCONV_MANAGED, i64));
        gen_return(cs);
        break;
    }
    case OPCODE_FRETURN: {
        SYNC_FIRST(static const CallSig cs(CCONV_MANAGED, flt32));
        gen_return(cs);
        break;
    }
    case OPCODE_DRETURN: {
        SYNC_FIRST(static const CallSig cs(CCONV_MANAGED, dbl64));
        gen_return(cs);
        break;
    }
    case OPCODE_ARETURN: {
        SYNC_FIRST(static const CallSig cs(CCONV_MANAGED, jobj));
        gen_return(cs);
        break;
    }
    case OPCODE_RETURN: {
        SYNC_FIRST(static const CallSig cs(CCONV_MANAGED, jvoid));
        gen_return(cs);   
        break;
    }
    default: assert(false);   break;
    };
}

void Compiler::handle_ik_obj(const JInst& jinst) {
    jtype jt = jvoid;
    bool store = false;
    switch(jinst.opcode) {
    case OPCODE_IASTORE:    store = true;
    case OPCODE_IALOAD:     jt = i32;       break;
    case OPCODE_LASTORE:    store = true;
    case OPCODE_LALOAD:     jt = i64;       break;
    case OPCODE_FASTORE:    store = true;
    case OPCODE_FALOAD:     jt = flt32;     break;
    case OPCODE_DASTORE:    store = true;
    case OPCODE_DALOAD:     jt = dbl64;     break;
    case OPCODE_AASTORE:    store = true;
    case OPCODE_AALOAD:     jt = jobj;      break;
    case OPCODE_BASTORE:    store = true;
    case OPCODE_BALOAD:     jt = i8;        break;
    case OPCODE_CASTORE:    store = true;
    case OPCODE_CALOAD:     jt = u16;       break;
    case OPCODE_SASTORE:    store = true;
    case OPCODE_SALOAD:     jt = i16;       break;
    default: break;
    }
    if (jt != jvoid) {
        // that was indeed *aload/*astore
        if (store) {
            gen_arr_store(jt);
        }
        else {
            gen_arr_load(jt);
        }
        return;
    }

    switch(jinst.opcode) {
    case OPCODE_NEW:
        gen_new(m_klass, (unsigned short)jinst.op0);
        break;
    case OPCODE_PUTSTATIC:
    case OPCODE_GETSTATIC:
    case OPCODE_PUTFIELD:
    case OPCODE_GETFIELD:
        gen_field_op(jinst.opcode, m_klass, (unsigned short)jinst.op0);
        break;
    case OPCODE_ARRAYLENGTH:
        gen_array_length();
        break;
    case OPCODE_ANEWARRAY:
        gen_new_array(m_klass, (unsigned short)jinst.op0);
        break;
    case OPCODE_NEWARRAY:
        {
        VM_Data_Type atype;
        switch(jinst.op0) {
        case 4: atype = VM_DATA_TYPE_BOOLEAN; break;
        case 5: atype = VM_DATA_TYPE_CHAR; break;
        case 6: atype = VM_DATA_TYPE_F4; break;
        case 7: atype = VM_DATA_TYPE_F8; break;
        case 8: atype = VM_DATA_TYPE_INT8; break;
        case 9: atype = VM_DATA_TYPE_INT16; break;
        case 10: atype = VM_DATA_TYPE_INT32; break;
        case 11: atype = VM_DATA_TYPE_INT64; break;
        default:    assert(false); atype = VM_DATA_TYPE_INVALID; break;
        }
        Class_Handle elem_class = class_get_class_of_primitive_type(atype);
        Class_Handle array_class = class_get_array_of_class(elem_class);
        Allocation_Handle ah = class_get_allocation_handle(array_class);
        gen_new_array(ah);
        }
        break;
    case OPCODE_MULTIANEWARRAY:
        gen_multianewarray(m_klass, (unsigned short)jinst.op0, jinst.op1);
        break;
    case OPCODE_MONITORENTER:
    case OPCODE_MONITOREXIT:
        gen_monitor_ee();
        break;
    case OPCODE_CHECKCAST:
    case OPCODE_INSTANCEOF:
        gen_instanceof_cast(jinst.opcode, m_klass, (unsigned short)jinst.op0);
        break;
    default: assert(false); break;
    }
}

void Compiler::handle_ik_stack(const JInst& jinst) {
    switch(jinst.opcode) {
    case OPCODE_POP:
        gen_pop(m_jframe->top());
        break;
    case OPCODE_POP2:
        gen_pop2();
        break;
    case OPCODE_DUP:
    case OPCODE_DUP_X1:
    case OPCODE_DUP_X2:
    case OPCODE_DUP2:
    case OPCODE_DUP2_X1:
    case OPCODE_DUP2_X2:
    case OPCODE_SWAP:
        gen_dup(jinst.opcode);
        break;
    default: assert(false); break;
    }
}

}
} // ~namespace Jitrino::Jet


