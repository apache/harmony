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
 * @author Alexander Astapchuk
 */
/**
 * @file
 * @brief Implementation of CodeGen routines for fields, statics and array
 *        accesses.
 */
 
#include "cg.h"
#include <open/vm.h>
#include <open/vm_class_info.h>
#include <open/vm_field_access.h>
#include <open/vm_ee.h>

#include "trace.h"
#include "VMMagic.h"

namespace Jitrino {
namespace Jet {

void CodeGen::gen_array_length(void)
{
    Opnd arr = vstack(0, true).as_opnd();
    rlock(arr);
    gen_check_null(0);
    vpop();
    vpush(Val(i32, arr.reg(), rt_array_length_offset));
    runlock(arr);
    // the array length is known to be non-negative, marking it as such
    vstack(0, VA_NZ);
}

void CodeGen::gen_arr_load(jtype jt)
{
    // stack: [.., aref, idx]
    // If index is not immediate, then force it to register
    Val& idx = vstack(0, !vis_imm(0));
    assert(idx.is_imm() || idx.is_reg());
    rlock(idx);
    // Force reference on register
    Val& arr = vstack(1, true);
    rlock(arr);
    // Runtime checks
    gen_check_null(1);
    gen_check_bounds(1, 0);
    
    vpop();
    vpop();
    // For compressed refs, the size is always 4
    unsigned size = jt==jobj && g_refs_squeeze ? 4 : jtypes[jt].size;
    AR base = arr.reg();
    int disp = jtypes[jt].rt_offset +
               (idx.is_imm() ? size*idx.ival() : 0);
    
    AR index = idx.is_imm() ? ar_x : idx.reg();
    unsigned scale = idx.is_imm() ? 0 : size;
    Opnd elem(jt, base, disp, index, scale);

    if (!is_ia32() && jt==jobj && g_refs_squeeze) {
        AR gr_base = valloc(jobj);
        rlock(gr_base);
        AR gr_ref = valloc(jobj);
        rlock(gr_ref);
        
        Opnd where32(i32, elem.base(), elem.disp(), 
                          elem.index(), elem.scale());
        mov(Opnd(i32, gr_ref), where32);
        //sx(Opnd(jobj, gr_ref), where32);
        movp(gr_base, OBJ_BASE);
        Opnd obj(jobj, gr_ref);
        alu(alu_add, obj, Opnd(jobj, gr_base));
        //
        runlock(gr_ref);
        runlock(gr_base);
        //
        runlock(arr);
        runlock(idx);
        vpush(obj);
    }
    else if (is_big(jt)) {
        AR ar_lo = valloc(jt);
        Opnd lo(jt, ar_lo);
        rlock(lo);
        
        do_mov(lo, elem);
        runlock(idx);
        
        AR ar_hi = valloc(jt);
        Opnd hi(jt, ar_hi);
        rlock(hi);
        Opnd elem_hi(jt, base, disp+4, index, scale);
        do_mov(hi, elem_hi);
        runlock(arr);
        vpush2(lo, hi);
        runlock(lo);
        runlock(hi);
    }
    else {
        jtype jtm = jt < i32 ? i32 : jt;
        runlock(idx);
        AR ar = valloc(jtm);
        Opnd val(jtm, ar);
        rlock(val);
        if (jt == i8)       { sx1(val, elem); }
        else if (jt == i16) { sx2(val, elem); }
        else if (jt == u16) { zx2(val, elem); }
        else                { do_mov(val, elem); }
        runlock(val);
        runlock(arr);
        vpush(val);
    }
}

void CodeGen::gen_arr_store(jtype jt, bool helperOk)
{
    vunref(jt);
    // stack: [.., aref, idx, val]
    if (jt == jobj && helperOk) {
        gen_write_barrier(m_curr_inst->opcode, NULL, Opnd(0));
        SYNC_FIRST(static const CallSig cs_aastore(CCONV_HELPERS, jvoid, jobj, i32, jobj));
        unsigned stackFix = gen_stack_to_args(true, cs_aastore, 0);
        gen_call_vm(cs_aastore, rt_helper_aastore, 3);
        if (stackFix != 0) {
            alu(alu_sub, sp, stackFix);
        }
        runlock(cs_aastore);
        return;
    }
    unsigned idx_depth = is_wide(jt) ? 2 : 1;
    unsigned ref_depth = idx_depth + 1;
    
    // Force reference on register
    const Val& arr = vstack(ref_depth, true);
    rlock(arr);
    // If index is not immediate, then force it to register
    const Val& idx = vstack(idx_depth, !vis_imm(idx_depth));
    assert(idx.is_imm() || idx.is_reg());
    rlock(idx);
    //
    //
    gen_check_null(ref_depth);
    gen_check_bounds(ref_depth, ref_depth-1);
    
    // Where to store
    AR base = arr.reg();
    int disp = jtypes[jt].rt_offset +
               (idx.is_imm() ? jtypes[jt].size*idx.ival() : 0);
    AR index = idx.is_imm() ? ar_x : idx.reg();
    unsigned scale = idx.is_imm() ? 0 : jtypes[jt].size;

    Opnd where(jt, base, disp, index, scale);
    // If we need to perform a narrowing convertion, then have the 
    // item on a register.
    const Val& val = vstack(0, vis_mem(0));
    rlock(val);
    if (is_big(jt)) {
        do_mov(where, val);
        runlock(idx);
        runlock(val);
        Opnd where_hi(jt, base, disp+4, index, scale);
        Val& val_hi = vstack(1);
        rlock(val_hi);
        do_mov(where_hi, val_hi);
        runlock(val_hi);
    }
    else if (jt<i32) {
        do_mov(where, val.as_opnd(jt));
        runlock(idx);
        runlock(val);
    }
    else {
        runlock(idx);
        runlock(val);
        do_mov(where, val);
    }
    
    runlock(arr);
    //
    vpop();
    vpop();
    vpop();
}


void CodeGen::gen_field_op(JavaByteCodes opcode,  Class_Handle enclClass, unsigned short cpIndex) {
    FieldOpInfo fieldOp(NULL, enclClass, cpIndex, opcode); 

    bool needJVMTI = compilation_params.exe_notify_field_modification 
                    || compilation_params.exe_notify_field_access;
    bool lazy = m_lazy_resolution && !needJVMTI; // JVMTI field access helpers are not ready for lazy resolution mode
    bool resolve = !lazy || class_cp_is_entry_resolved(enclClass, cpIndex);
    if (resolve) {
        if (!fieldOp.isStatic()) {
            fieldOp.fld = resolve_nonstatic_field(m_compileHandle, enclClass, cpIndex, fieldOp.isPut());
        } else {
            Field_Handle fld = resolve_static_field(m_compileHandle, enclClass,  cpIndex, fieldOp.isPut());
            if (fld && !field_is_static(fld)) {
                fld = NULL;
            }
            if (fld != NULL) {
                Class_Handle klass = field_get_class(fld);
                assert(klass);
                if (klass != m_klass && !class_is_initialized(klass)) {
                    gen_call_vm(ci_helper_o, rt_helper_init_class, 0, klass);
                }
                fieldOp.fld = fld;
            }
        }
        if(fieldOp.fld == NULL && !lazy) {  //in lazy resolution mode exception will be thrown from lazy resolution helper
            //TODO: we can avoid this check and use lazy resolution code path in this case!
            gen_call_throw(ci_helper_linkerr, rt_helper_throw_linking_exc, 0, enclClass, cpIndex, opcode);
        }
    }
    do_field_op(fieldOp);
}

Opnd CodeGen::get_field_addr(const FieldOpInfo& fieldOp, jtype jt) {
    Opnd where;
    if (!fieldOp.isStatic()) { //generate check null
        unsigned ref_depth = fieldOp.isPut() ? (is_wide(jt) ? 2 : 1) : 0;
        gen_check_null(ref_depth);
        if (fieldOp.fld) { //field is resolved -> offset is available
            unsigned fld_offset = field_get_offset(fieldOp.fld);
            Val& ref = vstack(ref_depth, true);
            where = Opnd(jt, ref.reg(), fld_offset);
        }  else { //field is not resolved -> generate code to request offset
            SYNC_FIRST(static const CallSig cs_get_offset(CCONV_HELPERS, iplatf, iplatf, i32, i32));
            gen_call_vm(cs_get_offset, rt_helper_field_get_offset_withresolve, 0, fieldOp.enclClass, fieldOp.cpIndex, fieldOp.isPut());
            AR gr_ret = cs_get_offset.ret_reg(0);
            rlock(gr_ret);
            Val& ref = vstack(ref_depth, true);
            runlock(gr_ret);
            alu(alu_add, gr_ret, ref.as_opnd());
            where = Opnd(jt, gr_ret, 0);
        }
    } else {
        if (fieldOp.fld) { //field is resolved -> address is available
            char * fld_addr = (char*)field_get_address(fieldOp.fld);
            where = vaddr(jt, fld_addr);
        }  else { //field is not resolved -> generate code to request address
            SYNC_FIRST(static const CallSig cs_get_addr(CCONV_HELPERS, iplatf, iplatf, i32, i32));
            gen_call_vm(cs_get_addr, rt_helper_field_get_address_withresolve, 0, fieldOp.enclClass, fieldOp.cpIndex, fieldOp.isPut());
            AR gr_ret = cs_get_addr.ret_reg(0);
            where = Opnd(jt, gr_ret, 0);
        }
    }
    return where;
}

void CodeGen::do_field_op(const FieldOpInfo& fieldOp)
{
    jtype jt = to_jtype(class_cp_get_field_type(fieldOp.enclClass, fieldOp.cpIndex));
    
    const char* fieldDescName = class_cp_get_entry_descriptor(fieldOp.enclClass, fieldOp.cpIndex);
    bool fieldIsMagic = VMMagicUtils::isVMMagicClass(fieldDescName);
    if (fieldIsMagic) {
        jt = iplatf;
    }

    if (fieldOp.isPut() && compilation_params.exe_notify_field_modification && !fieldIsMagic)  {
        gen_modification_watchpoint(fieldOp.opcode, jt, fieldOp.fld);
    }
    if (fieldOp.isGet() && compilation_params.exe_notify_field_access && !fieldIsMagic) {
        gen_access_watchpoint(fieldOp.opcode, jt, fieldOp.fld);
    }
    if (fieldOp.isPut() && ! fieldIsMagic) {
        Opnd where = get_field_addr(fieldOp, jt);
        gen_write_barrier(fieldOp.opcode, fieldOp.fld, where);
    }

    Opnd where = get_field_addr(fieldOp, jt);
    rlock(where);
    

    //generate get/put op

    if (fieldOp.isGet()) {

        if (!fieldOp.isStatic()) {
            // pop out ref
            vpop();
        }
        if ( !is_ia32() && (jt == jobj || fieldIsMagic) ) {
            if (fieldIsMagic || !g_refs_squeeze) {
                AR gr_ref = valloc(jobj);
                rlock(gr_ref);
                Opnd obj(jobj, gr_ref);
                mov(Opnd(jobj, gr_ref), where);
                runlock(gr_ref);
                vpush(obj);
            } else {
                assert(!is_ia32());
                AR gr_base = valloc(jobj);
                rlock(gr_base);
                AR gr_ref = valloc(jobj);
                rlock(gr_ref);

                Opnd where32(i32, where.base(), where.disp(), 
                              where.index(), where.scale());
                mov(Opnd(i32, gr_ref), where32);
                movp(gr_base, OBJ_BASE);
                Opnd obj(jobj, gr_ref);
                alu(alu_add, obj, Opnd(jobj, gr_base));
                //
                runlock(gr_ref);
                runlock(gr_base);
                //
                vpush(obj);
            }
        }
        else if (jt<i32) {
            AR gr = valloc(i32);
            Opnd reg(i32, gr);
            //
            if (jt == i8)       { sx1(reg, where); }
            else if (jt == i16) { sx2(reg, where); }
            else if (jt == u16) { zx2(reg, where); }
            //
            vpush(Val(i32, gr));
        }
        else {
            if (is_big(jt)){
                // if in lazy resolution mode the field may be not resolved
                // it is pessimistically considered as a volatile one.
                if ( (!fieldOp.fld) || field_is_volatile(fieldOp.fld) ) {
                    Opnd hi_part(jt, valloc(jt));
                    rlock(hi_part.reg());
                    Opnd lo_part(jt, valloc(jt));
                    volatile64_get(where, hi_part.reg(), lo_part.reg());
                    vpush2(lo_part, hi_part);
                    runlock(hi_part.reg());
                } else {
                    Opnd where_hi(jt, where.base(), where.disp()+4, 
                                      where.index(), where.scale());
                    vpush2(where, where_hi);
                }
            }
            else {
                vpush(where);
            }
        }
        runlock(where);
        return;
    } // if (get)
    
    vunref(jt);

    if (!is_ia32() && g_refs_squeeze && jt == jobj && vis_imm(0)) {
        const Val& s = m_jframe->dip(0);
        unsigned ref = (unsigned)(int_ptr)((const char*)s.pval() - OBJ_BASE);
        Opnd where32(i32, where.base(), where.disp(), 
                          where.index(), where.scale());
        mov(where32, Opnd(ref));
    }
    else if ( !is_ia32() && (jt == jobj || fieldIsMagic) ) {
        // have the reference on a register
        Val& s0 = vstack(0, true);
        rlock(s0);
        if (fieldIsMagic || !g_refs_squeeze) {
            mov(where, s0.as_opnd());
        } else {
            assert(!is_ia32());
            // compress the reference
            AR tmp = valloc(jobj);
            void * inv_base = (void*)-(int_ptr)OBJ_BASE;
            movp(tmp, inv_base);
            alu(alu_add, Opnd(jobj, tmp), s0.as_opnd());
            // store the resulting I_32
            Opnd where32(i32, where.base(), where.disp(), 
                          where.index(), where.scale());
            mov(where32, Opnd(jobj, tmp)); //s0.as_opnd(i32));
        }
        runlock(s0);
    }
    else if (jt<i32) {
        // No need to unref() - we just can't have jt()<i32 on the stack 
        Val& val = vstack(0, vis_mem(0));
        assert(val.jt() == i32);
        do_mov(where, val.as_opnd(jt));
    }
    else {
        vunref(jt, where);
        // if in lazy resolution mode the field may be not resolved
        // it is pessimistically considered as a volatile one.
        if (is_big(jt) &&
           ((!fieldOp.fld) || field_is_volatile(fieldOp.fld))) {
            Opnd val_lo = vstack(0, true).as_opnd();
            rlock(val_lo.reg());
            Opnd val_hi = vstack(1, true).as_opnd();
            volatile64_set(where, val_hi.reg(), val_lo.reg());
            runlock(val_lo.reg());
        } else {
            Val& val = vstack(0, vis_mem(0));
            do_mov(where, val, fieldIsMagic);
            if (is_big(jt)) {
                Opnd where_hi(jt, where.base(), where.disp()+4, 
                                  where.index(), where.scale());
                vunref(jt, where_hi);
                Opnd val_hi = vstack(1, vis_mem(1)).as_opnd();
                do_mov(where_hi, val_hi);
            }
        }
    }
    
    runlock(where);

    
    vpop(); // pop out value
    if (!fieldOp.isStatic()) {
        vpop(); // pop out ref
    }
}


}}; // ~namespace Jitrino::Jet
