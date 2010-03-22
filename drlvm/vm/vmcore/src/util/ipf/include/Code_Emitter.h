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
#ifndef _MERCED_CODE_EMITTER_H
#define _MERCED_CODE_EMITTER_H

#include "open/types.h"
#include "Emitter_IR.h"
#include "merced.h"
#include "bit_vector.h"
#include "vm_java_support.h"

typedef Encoder_Instr_IR   Instr_IR;
typedef Encoder_Bundle_IR  Bundle_IR;
typedef Encoder_Unscheduled_Instr_IR Unsch_Instr_IR;


// constants

#define  ENC_WBUF_LEN 21     // length of a working buffer
#define  ENC_N_FAST_REG 64   // # of regs for fast dependence checking
#define  ENC_GL_N_SLOTS (ENC_WBUF_LEN * ENC_N_SLOTS) // total number of slots in a work. buf
// max possible number of empty slots
#define  ENC_MAX_EMPTY_SLOTS (ENC_WBUF_LEN * (ENC_N_SLOTS -1))

// A state for controlling whether two instructions should be scheduled in
// the same bundle
enum Coupled_Instr_State {
    ENC_single_instr,         // next instr is normal
    ENC_first_coupled_instr,  // next instr is 1st one out of two to be scheduled in the same bundle
    ENC_second_coupled_instr  // next instr is 2nd one out of two to be scheduled in the same bundle
};




////////////////////////////////////////////////////// begin brl patching

class MCE_brl_patch {
    MCE_brl_patch *next;
    uint64 target;
    uint64 offset;
public:
    MCE_brl_patch(uint64 br_target, uint64 br_offset);
    void set_next(MCE_brl_patch *next_patch);
    uint64 get_target();
    uint64 get_offset();
    MCE_brl_patch *get_next();
};



class MCE_brl_patch_list {
    MCE_brl_patch *patches;
public:
    MCE_brl_patch_list();
    void add_patch(MCE_brl_patch *patch);
    MCE_brl_patch *get_patch_list()
    {
        return patches;
    }
};

//////////////////////////////////////////////////////// end brl patching



//////////////////////////////////////////////////////////////////////////
//                    class Merced_Code_Emitter
//////////////////////////////////////////////////////////////////////////

class Merced_Code_Emitter {
public:
    Merced_Code_Emitter (tl::MemoryPool& m, unsigned byteCodeSize, unsigned nTargets);
    virtual ~Merced_Code_Emitter();
    
    void *operator new (size_t sz, tl::MemoryPool& m) { return m.alloc(sz); }    
    void operator delete (void *p, tl::MemoryPool& m) { }
    
    // flush working buffer
    void flush_buffer() {
        assert (coupled_instr_state == ENC_single_instr);
        emit_all();
    }

    // get the number of instruction bytes that have been emitted

    unsigned get_size();


    // copy the instructions into buffer; buffer must be large
    // enough to hold all instructions
    
    void copy(char *buffer);

    // estimate memory size based on some heuristics
    // ToDo...
    static unsigned estimate_mem_size(unsigned byteCodeSize);
    

    // Fix the code buffer address to be divisible by 16.
    // Required by the Merced disassembler.
    
    static char * fix_code_buffer_address(char const * buf) {
        uint64 extra=0x10 - (uint64)buf;
        return (char *) ((uint64)buf + (0xf & extra));
    } 
    
    void set_bytecode_addr (unsigned bytecode_addr)
    { curr_bc_addr = bytecode_addr; }

    // compute a check sum for the generated code
    uint64 code_check_sum();

    // Memory dependency functions

    void set_mem_type_to_field_handle (void * field_handle, bool may_throw_exc=true)
    { assert(known_mem_type);
      curr_is_mem_access = true;
      curr_mem_type = ENC_MT_field_handle;
      curr_mem_value = (uint64)field_handle;
      curr_exc = may_throw_exc;
    }

    void set_mem_type_to_sp_offset (unsigned sp_offset)
    { assert(known_mem_type);
      curr_is_mem_access = true;
      curr_mem_type = ENC_MT_sp_offset;
      curr_mem_value = sp_offset;
      curr_exc = false;
    }

    void set_mem_type_to_array_element_type (Java_Type type, bool may_throw_exc=true)
    { assert(known_mem_type);
      curr_is_mem_access = true; 
      curr_mem_type = ENC_MT_array_element;
      curr_mem_value = type;
      curr_exc = may_throw_exc;
    }

    void set_mem_type_to_method (bool may_throw_exc=true)
    { assert(known_mem_type);
      curr_is_mem_access = true;
      curr_mem_type=ENC_MT_vt_entry;
      curr_mem_value = (unsigned)-1;
      curr_exc = may_throw_exc;
    }

    void set_mem_type_to_switch ()
    { assert(known_mem_type);
      curr_is_mem_access = true;
      curr_mem_type = ENC_MT_switch_entry;
      curr_mem_value = (unsigned)-1;
      curr_exc = false;
    }

    void set_mem_type_to_array_length (bool may_throw_exc=true)
    { assert(known_mem_type);
      curr_is_mem_access = true;
      curr_mem_type = ENC_MT_array_entry;
      curr_mem_value = (unsigned)-1;
      curr_exc = may_throw_exc;
    }

    void set_mem_type_to_object_vt (bool may_throw_exc=true)
    { assert(known_mem_type);
      curr_is_mem_access = true;
      curr_mem_type = ENC_MT_object_vt;
      curr_mem_value = (unsigned)-1;
      curr_exc = may_throw_exc;
    }

    void set_mem_type_to_vt_class (bool may_throw_exc=false)
    { assert(known_mem_type);
      curr_is_mem_access = true;
      curr_mem_type = ENC_MT_vt_class;
      curr_mem_value = (unsigned)-1;
      curr_exc = may_throw_exc;
    }

    void set_mem_type_to_quick_thread (bool may_throw_exc=false)
    { assert(known_mem_type);
      curr_is_mem_access = true;
      curr_mem_type = ENC_MT_quick_thread;
      curr_mem_value = (unsigned)-1;
      curr_exc = may_throw_exc;
    }

    // Next two instructions should be scehduled in the same bundle
    void next_two_instr_are_coupled(bool independent=true) {
        assert(coupled_instr_state == ENC_single_instr);
        coupled_instr_state = ENC_first_coupled_instr;
        curr_instr_couple_is_unordered = independent;
    }

    // Code patching

    // Next instruction is a symbolic target
    void set_target(unsigned target_id) {
        assert(target_id < n_targets);
        assert(target_offset[target_id] == ENC_NOT_A_TARGET);
        if (next_instr_is_target) 
            target_offset[target_id]=next_instr_target_id;
        next_instr_is_target=true;
        next_instr_target_id=target_id;
        flush_buffer();
    }

    // Set a switch target
    void set_switch_target(unsigned target_id, uint64 * abs_address_entry) {
        assert(target_id < n_targets);
        patches=new(mem_pool) Switch_Patch(patches,target_id,abs_address_entry);
    }

    // Get an offset of a target
    uint64 get_target_offset(unsigned target_id) {
        assert (target_offset_is_set[target_id]);
        return target_offset[target_id];
    }

    // Switch between two modes of operation:
    //  - each memory location should have a known type and value
    //  - memory location type is unknown; any two locations can be aliased
    void memory_type_is_known()   { known_mem_type=true;}
    void memory_type_is_unknown() { known_mem_type=false;}

    // Switch between modes:
    //   - exchange instructions to get better compaction
    //   - do not exchange instructions
    void allow_instruction_exchange() { exch_instr = true;}
    void disallow_instruction_exchange() {exch_instr = false;}

    /////////////////////////////////
    // emit an instruction
    /////////////////////////////////

    void ipf_nop (EM_Syllable_Type tv, unsigned imm21=0)
      { encoder->ipf_nop(tv, imm21);
        LDIE(51, "Not implemented");  //ToDo: nop assumes several form depending on which slot
    }

    void ipf_add (unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_add(dest, src1, src2, pred);
        _gen_an_IR_1i_2ii(curr_bc_addr, ST_a, pred, dest,src1, src2); }

    void ipf_sub (unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_sub(dest, src1, src2, pred);
        _gen_an_IR_1i_2ii(curr_bc_addr, ST_a, pred, dest,/**/src1, src2); }

    void ipf_addp4 (unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_addp4(dest, src1, src2, pred);
        _gen_an_IR_1i_2ii(curr_bc_addr, ST_a, pred, dest,/**/src1, src2); }

    void ipf_and (unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_and(dest, src1, src2, pred);
        _gen_an_IR_1i_2ii(curr_bc_addr, ST_a, pred, dest,/**/src1, src2); }
    
    void ipf_or (unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_or(dest, src1, src2, pred);
        _gen_an_IR_1i_2ii(curr_bc_addr, ST_a, pred, dest,/**/src1, src2); }
    
    void ipf_xor (unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_xor(dest, src1, src2, pred);
        _gen_an_IR_1i_2ii(curr_bc_addr, ST_a, pred, dest,/**/src1, src2); }
    
    void ipf_shladd (unsigned dest, unsigned src1, int count, unsigned src2, unsigned pred=0)
      { encoder->ipf_shladd(dest, src1, count, src2, pred);
        _gen_an_IR_1i_2ii(curr_bc_addr, ST_a, pred, dest,/**/src1, src2); }
    
    void ipf_subi (unsigned dest, int imm, unsigned src, unsigned pred=0)
      { encoder->ipf_subi(dest, imm, src, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_a, pred, dest,/**/src); }
    
    void ipf_andi (unsigned dest, int imm8, unsigned src, unsigned pred=0)
      { encoder->ipf_andi(dest, imm8, src, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_a, pred, dest,/**/src); }
    
    void ipf_ori (unsigned dest, int imm8, unsigned src, unsigned pred=0)
      { encoder->ipf_ori(dest, imm8, src, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_a, pred, dest,/**/src); }
    
    void ipf_xori (unsigned dest, int imm, unsigned src, unsigned pred=0)
      { encoder->ipf_xori(dest, imm, src, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_a, pred, dest,/**/src); }
    
    void ipf_adds (unsigned dest, int imm14, unsigned src, unsigned pred=0)
      { encoder->ipf_adds(dest, imm14, src, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_a, pred, dest,/**/src); }
    
    void ipf_addp4i (unsigned dest, int imm14, unsigned src, unsigned pred=0)
      { encoder->ipf_addp4i(dest, imm14, src, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_a, pred, dest,/**/src); }
    
    void ipf_addl (unsigned dest, int imm22, unsigned src, unsigned pred=0)
      { encoder->ipf_addl(dest, imm22, src, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_a, pred, dest,/**/src); }
    
    void ipf_cmp (Int_Comp_Rel cr, Compare_Extension cx, unsigned p1, unsigned p2, unsigned r2, unsigned r3, bool cmp4=false, unsigned pred=0)
      { encoder->ipf_cmp(cr, cx, p1, p2, r2, r3, cmp4, pred);
        _gen_an_IR_2pp_2ii(curr_bc_addr, ST_a, pred, cx, p1, p2,/**/r2, r3); }

    void ipf_cmpz (Int_Comp_Rel cr, Compare_Extension cx, unsigned p1, unsigned p2, unsigned r3, bool cmp4=false, unsigned pred=0)
      { encoder->ipf_cmpz(cr, cx, p1, p2, r3, cmp4, pred);
        _gen_an_IR_2pp_1i(curr_bc_addr, ST_a, pred, cx, p1, p2,/**/r3); }
    
    void ipf_cmpi (Int_Comp_Rel cr, Compare_Extension cx, unsigned p1, unsigned p2, int imm, unsigned r3, bool cmp4=false, unsigned pred=0)
      { encoder->ipf_cmpi(cr, cx, p1, p2,imm, r3, cmp4, pred);
        _gen_an_IR_2pp_1i(curr_bc_addr, ST_a, pred, cx, p1, p2,/**/r3); }
    
    void ipf_movl (unsigned dest, unsigned upper_32, unsigned lower_32, unsigned pred=0)
      { encoder->ipf_movl(dest, upper_32, lower_32, pred);
        _gen_an_IR_1i_0(curr_bc_addr, ST_il, pred, dest/**/); }

    void ipf_movi64 (unsigned dest, uint64 imm64, unsigned pred=0)
     { encoder->ipf_movi64(dest, imm64, pred);
        _gen_an_IR_1i_0(curr_bc_addr, ST_il, pred, dest/**/); }

     void ipf_brl_call(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b1, uint64 imm64, unsigned pred=0)
     {
         encoder->ipf_brl_call(ph, wh, dh, b1, imm64, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_il, pred);
        encode_write_breg(b1,ir);
        encode_write_areg(AR_bsp,ir);
        encode_write_areg(AR_pfs,ir);
        encode_read_areg(AR_bsp,ir);
        encode_read_areg(AR_ec,ir);
        ir.special_instr=ENC_SI_brcall;
        schedule_an_IR(ir);
        emit_all();
        brl_patches->add_patch(new MCE_brl_patch(imm64, curr_offset - 16));
     } //ipf_brl_call

     void ipf_brl_cond(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, uint64 imm64, unsigned pred=0)
     {
       encoder->ipf_brl_cond(ph, wh, dh, imm64, pred);
       Encoder_Unscheduled_Instr_IR ir;
       _init_ir(ir, curr_bc_addr, ST_il, pred);
       schedule_an_IR(ir);
       brl_patches->add_patch(new MCE_brl_patch(imm64, curr_offset - 16));
     } //ipf_brl_call

     void ipf_movl_label (unsigned dest, unsigned target_id, unsigned pred=0)
      { encoder->ipf_movl(dest, 0 /* target_id */, pred);
        needs_patching=true;
        patch_target_id=target_id;
        _gen_an_IR_1i_0(curr_bc_addr, ST_il, pred, dest/**/); }

     void ipf_extr (unsigned dest, unsigned src, int pos6, int len6, unsigned pred=0)
      { encoder->ipf_extr(dest, src, pos6, len6, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_i, pred, dest,/**/src); }
    
    void ipf_extru (unsigned dest, unsigned src, int pos6, int len6, unsigned pred=0)
      { encoder->ipf_extru(dest, src, pos6,len6, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_i, pred, dest,/**/src); }
    
    void ipf_depz (unsigned dest, unsigned src, int pos6, int len6, unsigned pred=0)
      { encoder->ipf_depz(dest, src, pos6, len6, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_i, pred, dest,/**/src); }
    
    void ipf_depiz (unsigned dest, int imm8, int pos6, int len6, unsigned pred=0)
      { encoder->ipf_depiz(dest, imm8, pos6, len6, pred);
        _gen_an_IR_1i_0(curr_bc_addr, ST_i, pred, dest); }
    
    void ipf_depi (unsigned dest, int imm1, unsigned src, int pos6, int len6, unsigned pred=0)
      { encoder->ipf_depi(dest, imm1, src, pos6, len6, pred);
        _gen_an_IR_1i_1i(curr_bc_addr, ST_i, pred, dest,/**/src); }
    
    void ipf_dep (unsigned dest, unsigned r2, unsigned r3, int pos6, int len4, unsigned pred=0)
      { encoder->ipf_dep(dest, r2, r3, pos6, len4, pred);
        _gen_an_IR_1i_2ii(curr_bc_addr, ST_i, pred, dest,/**/r2, r3); }
    
    void ipf_br (Branch_Type btype, Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned target_id, unsigned pred=0)
      { assert(btype == br_cond || btype == br_cloop);
        if (btype == br_cloop)
            assert(pred == 0);
        encoder->ipf_br(btype, ph, wh, dh, 0 /* target_id */, pred);
        needs_patching=true;
        patch_target_id=target_id;
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, (btype == br_cloop ? ST_bl : ST_b), pred);
        if (btype == br_cloop)
        {
            encode_write_areg(AR_lc, ir); 
            encode_read_areg(AR_lc, ir);
        }
        schedule_an_IR(ir);
    }
    
    void ipf_brcall (Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b1, unsigned target25, unsigned pred=0)
      { // No relative address calls in JIT or VM
        DIE(("No relative address calls in JIT or VM"));}

    void ipf_bri (Branch_Type btype, Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b2, unsigned pred=0)
      { assert(btype == br_cond);
        encoder->ipf_bri(btype, ph, wh, dh, b2, pred);
        _gen_an_IR_0_1b(curr_bc_addr, ST_b, pred,/**/b2); }
    
    void ipf_brret (Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b2, unsigned pred=0)
    {   encoder->ipf_brret(ph, wh, dh, b2, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_b, pred);
        encode_write_areg(AR_bsp,ir); 
        encode_write_areg(AR_ec,ir);
        encode_read_breg(b2,ir); 
        encode_read_areg(AR_bsp,ir);
        encode_read_areg(AR_pfs,ir);
        schedule_an_IR(ir);
    }
    
    void ipf_bricall (Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b1, unsigned b2, unsigned pred=0)
      { encoder->ipf_bricall(ph, wh, dh, b1, b2, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_b, pred);
        encode_write_breg(b1,ir);
        encode_write_areg(AR_bsp,ir);
        encode_write_areg(AR_pfs,ir);
        encode_read_breg(b2,ir);
        encode_read_areg(AR_bsp,ir);
        encode_read_areg(AR_ec,ir);
        ir.special_instr=ENC_SI_brcall;
        schedule_an_IR(ir);
        emit_all();
      }
        
    void ipf_ld (Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned pred=0)
      { encoder->ipf_ld(size, flag, hint, dest, addrreg, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_m, pred);
        encode_write_ireg(dest,ir);
        encode_read_ireg(addrreg,ir);
        if (flag == mem_ld_fill)
            encode_read_areg(AR_unat,ir);
        schedule_an_IR(ir);
        reset_mem_type();
    } 
    
    void ipf_ld_inc_reg (Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_reg, unsigned pred=0)
      { encoder->ipf_ld_inc_reg(size, flag, hint, dest, addrreg, inc_reg, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir,curr_bc_addr, ST_m,pred);
        encode_write_ireg(dest,ir);
        encode_write_ireg(addrreg,ir);
        encode_read_ireg(addrreg,ir);
        encode_read_ireg(inc_reg,ir);
        if (flag == mem_ld_fill)
            encode_read_areg(AR_unat,ir);
        schedule_an_IR(ir);
        reset_mem_type();
    }
    
    void ipf_ld_inc_imm (Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_imm, unsigned pred=0)
      { encoder->ipf_ld_inc_imm(size, flag, hint, dest, addrreg, inc_imm, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir,curr_bc_addr, ST_m,pred);
        encode_write_ireg(dest,ir);
        encode_write_ireg(addrreg,ir);
        encode_read_ireg(addrreg,ir);
        if (flag == mem_ld_fill)
            encode_read_areg(AR_unat,ir);
        schedule_an_IR(ir);
        reset_mem_type();
    }
    
    void ipf_st (Int_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned pred=0)
      { encoder->ipf_st(size, flag, hint, addrreg, src, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir,curr_bc_addr, ST_m,pred);
        encode_read_ireg(addrreg,ir);
        encode_read_ireg(src,ir);
        if (flag == mem_st_spill)
            encode_write_areg(AR_unat,ir);
        schedule_an_IR(ir);
        reset_mem_type();
    }
    
    void ipf_st_inc_imm (Int_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned inc_imm, unsigned pred=0)
      { encoder->ipf_st_inc_imm(size, flag, hint, addrreg, src, inc_imm, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir,curr_bc_addr, ST_m,pred);
        encode_write_ireg(addrreg,ir);
        encode_read_ireg(addrreg,ir);
        encode_read_ireg(src,ir);
        if (flag == mem_st_spill)
            encode_write_areg(AR_unat,ir);
        schedule_an_IR(ir);
        reset_mem_type();
    }
    
    void ipf_ldf (Float_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned pred=0)
      { encoder->ipf_ldf(size, flag, hint, dest, addrreg, pred);
        _gen_an_IR_1f_1i(curr_bc_addr, ST_m, pred, dest,/**/addrreg,true);
        reset_mem_type();
    }
     
    void ipf_ldf_inc_reg (Float_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_reg, unsigned pred=0)
      { encoder->ipf_ldf_inc_reg(size, flag, hint, dest, addrreg, inc_reg, pred);
        _gen_an_IR_2fi_2ii(curr_bc_addr, ST_m, pred, dest, addrreg,/**/addrreg, inc_reg,true);
        reset_mem_type();
    }
     
    
    void ipf_ldf_inc_imm (Float_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_imm, unsigned pred=0)
      { encoder->ipf_ldf_inc_imm(size, flag, hint, dest, addrreg, inc_imm, pred);
        _gen_an_IR_2fi_1i(curr_bc_addr, ST_m, pred, dest, addrreg,/**/addrreg,true);
        reset_mem_type();
    }
    
    void ipf_stf (Float_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned pred=0)
      { encoder->ipf_stf(size, flag, hint, addrreg, src, pred);
        _gen_an_IR_0_2fi(curr_bc_addr, ST_m, pred,/**/src, addrreg,true);
        reset_mem_type();
    }
    
    void ipf_stf_inc_imm (Float_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned inc_imm, unsigned pred=0)
      { encoder->ipf_stf_inc_imm(size, flag, hint, addrreg, src, inc_imm, pred);
        _gen_an_IR_1i_2fi(curr_bc_addr, ST_m, pred, addrreg,/**/src, addrreg,true);
        reset_mem_type();
    }

    
    void ipf_mov (unsigned dest, unsigned src, unsigned pred=0)
      {  ipf_adds(dest, 0, src, pred); }    // pseudo-op
    
    void ipf_movi (unsigned dest, int imm22, unsigned pred=0)
      {  ipf_addl(dest, imm22, 0, pred); }  // pseudo-op
    
    void ipf_neg (unsigned dest, unsigned src, unsigned pred=0)
      {  ipf_subi(dest, 0, src, pred); }    // pseudo-op
    
    void ipf_sxt (Sxt_Size size, unsigned dest, unsigned src, unsigned pred=0)
      { encoder->ipf_sxt(size, dest, src, pred);
         _gen_an_IR_1i_1i(curr_bc_addr, ST_i, pred, dest,/**/src); }
    
    void ipf_zxt (Sxt_Size size, unsigned dest, unsigned src, unsigned pred=0)
      { encoder->ipf_zxt(size, dest, src, pred);
         _gen_an_IR_1i_1i(curr_bc_addr, ST_i, pred, dest,/**/src); }
    
    void ipf_shl (unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_shl(dest, src1, src2, pred);
         _gen_an_IR_1i_2ii(curr_bc_addr, ST_i, pred, dest,/**/src1, src2); }
    
    void ipf_shr (unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_shr(dest, src1, src2, pred);
         _gen_an_IR_1i_2ii(curr_bc_addr, ST_i, pred, dest,/**/src1, src2); }
    
    void ipf_shru (unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_shru(dest, src1, src2, pred);
         _gen_an_IR_1i_2ii(curr_bc_addr, ST_i, pred, dest,/**/src1, src2); }

    void ipf_shli (unsigned dest, unsigned src1, int count, unsigned pred=0)
      { ipf_depz(dest, src1, count, 64-count, pred); }  // pseudo-op

    void ipf_shri (unsigned dest, unsigned src1, int count, unsigned pred=0)
      { ipf_extr(dest, src1, count, 64-count, pred); }  // pseudo-op

    void ipf_shrui (unsigned dest, unsigned src1, int count, unsigned pred=0)
      { ipf_extru(dest, src1, count, 64-count, pred); } // pseudo-op

    void ipf_setf (FReg_Convert form, unsigned fdest, unsigned src, unsigned pred=0)
      { encoder->ipf_setf(form, fdest, src, pred);
         _gen_an_IR_1f_1i(curr_bc_addr, ST_m, pred, fdest,/**/src); }
    
    void ipf_getf (FReg_Convert form, unsigned dest, unsigned fsrc, unsigned pred=0)
      { encoder->ipf_getf(form, dest, fsrc, pred);
         _gen_an_IR_1i_1f(curr_bc_addr, ST_m, pred, dest,/**/fsrc); }
    
    void ipf_fma (Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned src3, unsigned pred=0)
      { encoder->ipf_fma(pc, sf, dest, src1, src2, src3, pred);
         _gen_an_IR_1f_3fff(curr_bc_addr, ST_f, pred, dest,/**/src1, src2, src3); }

    void ipf_fnma (Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned src3, unsigned pred=0)
      { encoder->ipf_fnma(pc, sf, dest, src1, src2, src3, pred);
         _gen_an_IR_1f_3fff(curr_bc_addr, ST_f, pred, dest,/**/src1, src2, src3); }

    void ipf_fms (Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned src3, unsigned pred=0)
      { encoder->ipf_fms(pc, sf, dest, src1, src2, src3, pred);
         _gen_an_IR_1f_3fff(curr_bc_addr, ST_f, pred, dest,/**/src1, src2, src3); }

    void ipf_frcpa (Float_Status_Field sf, unsigned dest, unsigned p2, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_frcpa(sf, dest, p2, src1, src2, pred);
         _gen_an_IR_2fp_2ff(curr_bc_addr, ST_f, pred, dest, p2, /**/src1, src2); }

    void ipf_fadd (Float_Precision pc, unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      {  ipf_fma(pc, sf0, dest, src1, 1, src2, pred); }  // pseudo-op

    void ipf_fsub (Float_Precision pc, unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      {  ipf_fms(pc, sf0, dest, src1, 1, src2, pred); }  // pseudo-op

    void ipf_fmul (Float_Precision pc, unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      {  ipf_fma(pc, sf0, dest, src1, src2, 0, pred); }  // pseudo-op

    void ipf_fnorm (Float_Precision pc, unsigned dest, unsigned src, unsigned pred=0)
      {  ipf_fma(pc, sf0, dest, src, 1, 0, pred); }      // pseudo-op
    
    void ipf_fmerge (Float_Merge fm, unsigned dest, unsigned src1, unsigned src2, unsigned pred=0)
      { encoder->ipf_fmerge(fm, dest, src1, src2, pred);
         _gen_an_IR_1f_2ff(curr_bc_addr, ST_f, pred, dest,/**/src1, src2); }

    void ipf_fcmp (Float_Comp_Rel cr, Compare_Extension cx, unsigned p1, unsigned p2, unsigned f2, unsigned f3, unsigned pred=0)
      { encoder->ipf_fcmp(cr, cx, p1, p2, f2, f3, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_f, pred);
        encode_write_preg(p1,ir);
        encode_write_preg(p2,ir);
        encode_read_freg(f2,ir);
        encode_read_freg(f3,ir);
        ir.special_instr = cmp_ext_to_special_instr[cx];
        schedule_an_IR(ir);
    }

    void ipf_fclass (Compare_Extension cx, unsigned p1, unsigned p2, unsigned f2, unsigned fclass9, unsigned pred=0)
      { encoder->ipf_fclass(cx, p1, p2, f2, fclass9, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_f, pred);
        encode_write_preg(p1,ir);
        encode_write_preg(p2,ir);
        encode_read_freg(f2,ir);
        ir.special_instr = cmp_ext_to_special_instr[cx];
        schedule_an_IR(ir);
    }

    void ipf_fcvt_fx (FFix_Convert fc, Float_Status_Field sf, unsigned dest, unsigned src, unsigned pred=0)
      { encoder->ipf_fcvt_fx(fc, sf, dest, src, pred);
         _gen_an_IR_1f_1f(curr_bc_addr, ST_f, pred, dest,/**/src); }

    void ipf_fcvt_xf (unsigned dest, unsigned src, unsigned pred=0)
      { encoder->ipf_fcvt_xf(dest, src, pred);
         _gen_an_IR_1f_1f(curr_bc_addr, ST_f, pred, dest,/**/src); }

    void ipf_fmov (unsigned dest, unsigned src, unsigned pred=0)
      {  ipf_fmerge(fmerge_s, dest, src, src, pred); }  // pseudo-op

    void ipf_fneg (unsigned dest, unsigned src, unsigned pred=0)
      {  ipf_fmerge(fmerge_ns, dest, src, src, pred); } // pseudo-op

    void ipf_alloc (unsigned dest, unsigned i, unsigned l, unsigned o, unsigned r)
      { flush_buffer();
        encoder->ipf_alloc(dest, i, l, o, r);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_ma, 0);
        encode_write_ireg(dest,ir);
        encode_write_areg(AR_pfs,ir);
        encode_write_areg(AR_bspstore,ir);
        encode_write_areg(AR_rnat,ir);
        encode_read_areg(AR_bspstore,ir);
        encode_read_areg(AR_pfs,ir);
        encode_read_areg(AR_rnat,ir);
        encode_read_areg(AR_rsc,ir);
        schedule_an_IR(ir);
      }

    void ipf_mtbr_michal (unsigned bdest, unsigned src, bool ret=false, unsigned offset=0, unsigned pred=0)
      { encoder->ipf_mtbr(bdest, src, brp_none, ret, offset, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_i, pred);
        encode_write_breg(bdest,ir);
        encode_read_ireg(src,ir);
        ir.special_instr=ENC_SI_mtbr;
        schedule_an_IR(ir);}

    void ipf_mtbr (unsigned bdest, unsigned src, unsigned pred=0)
      { encoder->ipf_mtbr(bdest, src, brp_none, false, 0, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_i, pred);
        encode_write_breg(bdest,ir);
        encode_read_ireg(src,ir);
        ir.special_instr=ENC_SI_mtbr;
        schedule_an_IR(ir);}

    void ipf_mfbr (unsigned dest, unsigned bsrc, unsigned pred=0)
      { encoder->ipf_mfbr(dest, bsrc, pred);
         _gen_an_IR_1i_1b(curr_bc_addr, ST_i, pred, dest,/**/bsrc); }

    void ipf_mtap (EM_Application_Register adest, unsigned src, unsigned pred=0)
      { // Dependencies between mt AR_fpsr and floating point instructions
        // are not implemented.
        assert (adest != AR_fpsr);
        EM_Syllable_Type syl_type=ST_m;
        switch (adest) {
        case AR_pfs:
        case AR_lc:
        case AR_ec:
            syl_type=ST_i;
            break;
        default:break;
        }
        encoder->ipf_mtap(adest, src, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, syl_type, pred);
        encode_write_areg(adest,ir);
        encode_read_ireg(src,ir);
        schedule_an_IR(ir);
    }

    void ipf_mfap (unsigned dest, EM_Application_Register asrc, unsigned pred=0)
      { // Dependencies between mf AR_fpsr and floating point instructions
        // are not implemented.
        assert (asrc != AR_fpsr);
        EM_Syllable_Type syl_type=ST_m;
        switch (asrc) {
        case AR_pfs:
        case AR_lc:
        case AR_ec:
            syl_type=ST_i;
            break;
        default: break;
        }
        encoder->ipf_mfap(dest, asrc, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, syl_type, pred);
        encode_write_ireg(dest,ir);
        encode_read_areg(asrc,ir);
        schedule_an_IR(ir);
    }

    void ipf_movip (unsigned dest, unsigned pred=0)
      { encoder->ipf_movip(dest, pred);
        _gen_an_IR_1i_ip(curr_bc_addr, ST_i, pred, dest/**/); }

    void ipf_xma(unsigned dest, unsigned src1, unsigned src2, unsigned src3, Xla_Flag flag, unsigned pred=0)
      { encoder->ipf_xma(dest, src1, src2, src3, flag, pred);
        _gen_an_IR_1f_3fff(curr_bc_addr, ST_f, pred, dest,/**/src1, src2, src3); }

    void ipf_cmpxchg(Int_Mem_Size size, Cmpxchg_Flag flag, Mem_Hint hint, unsigned dest, unsigned r3, unsigned r2, unsigned pred=0)
      { encoder->ipf_cmpxchg(size, flag, hint, dest, r3, r2, pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_m, pred);
        encode_write_ireg(dest,ir);
        encode_read_ireg(r3,ir);
        encode_read_ireg(r2,ir);
        encode_read_areg(AR_ccv,ir);
        schedule_an_IR(ir);
    }



    void ipf_mtpr(unsigned src1, unsigned mask17 = 0x1ffff, unsigned pred=0)
    {
        encoder->ipf_mtpr(src1, mask17, pred);
        _gen_an_IR_allpp_1i (curr_bc_addr, ST_i, pred, src1);
    } //ipf_mtpr



    void ipf_mfpr(unsigned dest, unsigned pred=0)
    {
        encoder->ipf_mfpr(dest, pred);
        _gen_an_IR_1i_allpp (curr_bc_addr, ST_i, pred, dest);
    } //ipf_mfpr



    // (? 20021205) To do: ensure that this the last instruction in an
    // instruction group.
    void ipf_cover()
    { 
        encoder->ipf_cover();
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_bl, 0);
        ir.special_instr = ENC_SI_end_igroup;
        schedule_an_IR(ir);
   } //ipf_cover


    // (? 20021205) To do: ensure that this the first instruction in an
    // instruction group.
    void ipf_flushrs()
    { 
        encoder->ipf_flushrs();
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_m, 0);
        ir.special_instr = ENC_SI_start_igroup;
        schedule_an_IR(ir);
    } //ipf_flushrs


    void ipf_mf(unsigned pred=0)
    {
        encoder->ipf_mf(pred);
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, curr_bc_addr, ST_m, 0);
        schedule_an_IR(ir);
    }


private: 
    // Methods to encode registers for register dependency checks

    // Return an integer with a register bit set according to a fast scheme.
    // If need to switch to a slow scheme return 0.

    uint64 encode_a_fast_reg(U_8* reg_ptr) {
        U_8 b=*reg_ptr;
        if (b & 0x80)  {  // reg is not in the map
            if (n_fast_reg >= ENC_N_FAST_REG)
                return 0;
            b=*reg_ptr=n_fast_reg++;
        }
        return (uint64)0x1 << b;
    }

    // encode a write to a register
    void encode_write_reg(U_8* reg_ptr, Encoder_Instr_IR &ir) {
#ifndef ENC_SLOW_REG_DEP
        if (fast_reg_dep_check) {
            uint64 u=encode_a_fast_reg(reg_ptr);
            if (u)
                ir.written_regs.fast |= u;
            else {
                switch_ir_to_slow_reg_dep_check(ir, &curr_rd_reg_vector, &curr_wr_reg_vector);
                switch_to_slow_reg_dep_check();
                curr_wr_reg_vector.set((unsigned) (reg_ptr-reg_map));
            }
        }
        else 
#endif
            curr_wr_reg_vector.set((unsigned) (reg_ptr-reg_map));
    }

    // encode a read from a register
    void encode_read_reg(U_8* reg_ptr, Encoder_Instr_IR &ir) {
#ifndef ENC_SLOW_REG_DEP
        if (fast_reg_dep_check) {
            uint64 u=encode_a_fast_reg(reg_ptr);
            if (u)
                ir.read_regs.fast |= u;
            else {
                switch_ir_to_slow_reg_dep_check(ir, &curr_rd_reg_vector, &curr_wr_reg_vector);
                curr_rd_reg_vector.set((unsigned) (reg_ptr-reg_map));
                switch_to_slow_reg_dep_check();
            }
        }
        else
#endif
            curr_rd_reg_vector.set((unsigned) (reg_ptr-reg_map));
    }
           

    void encode_write_ireg(unsigned ireg, Encoder_Instr_IR &ir) {
        encode_write_reg(ireg_map+ireg,ir);
    }
    void encode_read_ireg(unsigned ireg, Encoder_Instr_IR &ir) {
        encode_read_reg(ireg_map+ireg,ir);
    }
    void encode_write_freg(unsigned freg, Encoder_Instr_IR &ir) {
        encode_write_reg(freg_map+freg,ir);
    }
    void encode_read_freg(unsigned freg, Encoder_Instr_IR &ir) {
        encode_read_reg(freg_map+freg,ir);
    }
    void encode_write_breg(unsigned breg, Encoder_Instr_IR &ir) {
        encode_write_reg(breg_map+breg,ir);
    }
    void encode_read_breg(unsigned breg, Encoder_Instr_IR &ir) {
        encode_read_reg(breg_map+breg,ir);
    }
    void encode_write_preg(unsigned preg, Encoder_Instr_IR &ir) {
        if (preg > 0)  // 3.4
            encode_write_reg(preg_map+preg,ir);
    }
    void encode_read_preg(unsigned preg, Encoder_Instr_IR &ir) {
        if (preg >0)   // 3.4
            encode_read_reg(preg_map+preg,ir);
    }
    void encode_write_areg(unsigned areg, Encoder_Instr_IR &ir) {
        encode_write_reg(areg_map+areg,ir);
    }
    void encode_read_areg(unsigned areg, Encoder_Instr_IR &ir) {
        encode_read_reg(areg_map+areg,ir);
    }
    void switch_to_slow_reg_dep_check();
    void switch_ir_to_slow_reg_dep_check(Instr_IR &prev_ir,Enc_All_Reg_BV* read_regs, Enc_All_Reg_BV* written_regs);
    void start_slow_dep();
    
protected:
    // Methods to create instruction IR
    virtual void _init_ir (Encoder_Unscheduled_Instr_IR &ir,
                   unsigned bytecode_addr,
                   EM_Syllable_Type syl_type,
                   unsigned pred) {
        assert(bytecode_addr==(unsigned)-1 || bytecode_addr<(1<<16));
        
        encoder->get_slot01_code_image(&ir.code_image1, &ir.code_image2);
        ir.bytecode_addr = (uint16)bytecode_addr;
        ir.syl_type = syl_type;
        ir.special_instr = ENC_SI_none;
        ir.filled();
#ifndef ENC_SLOW_REG_DEP
        if (!fast_reg_dep_check) 
#endif
        {
            ir.written_regs.slow=&curr_wr_reg_vector;
            ir.read_regs.slow=&curr_rd_reg_vector;
            curr_wr_reg_vector.reset_all();
            curr_rd_reg_vector.reset_all();
        }
        encode_read_preg(pred,ir);
        assert (!curr_is_mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        if (curr_is_mem_access) {
            ir.set_is_mem_access();
        }
        ir.mem_type = curr_mem_type;
        ir.mem_value = curr_mem_value;
        if (curr_exc)
            ir.set_may_throw_exc();
        if (next_instr_is_target) {
            ir.set_is_target();
            ir.target_id=next_instr_target_id;
            next_instr_is_target=false;
        }
        if (needs_patching) {
            ir.set_needs_patching();
            ir.patch_target_id=patch_target_id;
            needs_patching=false;
        }
    }

    virtual void copy_ir_into_slot(Encoder_Instr_IR &dest, Encoder_Unscheduled_Instr_IR & src) {
        dest.code_image1=src.code_image1;
        dest.bytecode_addr=src.bytecode_addr;
        dest.syl_type = src.syl_type;
        dest.flags = src.flags;
        dest.mem_type = src.mem_type;
        dest.mem_value = src.mem_value;
#ifndef ENC_SLOW_REG_DEP
        if (fast_reg_dep_check) {
            dest.read_regs.fast = src.read_regs.fast;
            dest.written_regs.fast = src.written_regs.fast;
        }
        else 
#endif
        {
            assert (dest.read_regs.slow && dest.written_regs.slow);
            *dest.read_regs.slow = *src.read_regs.slow;
            *dest.written_regs.slow = *src.written_regs.slow;
        }
        dest.patch_target_id = src.patch_target_id;
        dest.special_instr = src.special_instr;
    }

    virtual void copy_unscheduled_ir(Encoder_Unscheduled_Instr_IR& dest, Encoder_Unscheduled_Instr_IR& src) {
        dest.code_image1=src.code_image1;
        dest.code_image2=src.code_image2;
        dest.bytecode_addr=src.bytecode_addr;
        dest.syl_type = src.syl_type;
        dest.flags = src.flags;
        dest.mem_type = src.mem_type;
        dest.mem_value = src.mem_value;
#ifndef ENC_SLOW_REG_DEP
        if (fast_reg_dep_check) {
            dest.read_regs.fast = src.read_regs.fast;
            dest.written_regs.fast = src.written_regs.fast;
        }
        else 
#endif
        {   assert (dest.read_regs.slow != src.read_regs.slow &&
                    dest.written_regs.slow != src.written_regs.slow);
            *dest.read_regs.slow = *src.read_regs.slow;
            *dest.written_regs.slow = *src.written_regs.slow;
        }
        dest.patch_target_id = src.patch_target_id;
        dest.special_instr = src.special_instr;
        dest.target_id = src.target_id;
    }


    void _gen_an_IR_0_0 (unsigned bca, EM_Syllable_Type syl_type, unsigned pred ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1i_2ii (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned idest_reg,
                     unsigned isrc_reg1,
                     unsigned isrc_reg2,
                     bool mem_access=false) {
        assert(!mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_ireg(idest_reg,ir);
        encode_read_ireg(isrc_reg1,ir);
        encode_read_ireg(isrc_reg2,ir);
        schedule_an_IR(ir);
    }
    

    void _gen_an_IR_1i_1i (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned idest_reg,
                     unsigned isrc_reg, bool mem_access=false) {
        assert(!mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_ireg(idest_reg,ir);
        encode_read_ireg(isrc_reg,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1i_0 (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned idest_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_ireg(idest_reg,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1i_ip (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                        unsigned idest_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_ireg(idest_reg,ir);
        ir.special_instr = ENC_SI_mtip;
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1i_allpp (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned idest_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_ireg(idest_reg,ir);
        for(int pr = 0; pr < 64; pr++) {
            encode_read_preg(pr,ir);
        }
        schedule_an_IR(ir);
    }
    void _gen_an_IR_allpp_1i (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned isrc_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_read_ireg(isrc_reg,ir);
        for(int pr = 0; pr < 64; pr++) {
            encode_write_preg(pr,ir);
        }
        schedule_an_IR(ir);
    }
    void _gen_an_IR_2pp_2ii(unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     Compare_Extension cx,
                     unsigned dest_p1,
                     unsigned dest_p2,
                     unsigned isrc_reg1,
                     unsigned isrc_reg2) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_preg(dest_p1,ir);
        encode_write_preg(dest_p2,ir);
        encode_read_ireg(isrc_reg1,ir);
        encode_read_ireg(isrc_reg2,ir);
        ir.special_instr = ENC_SI_icmp | cmp_ext_to_special_instr[cx];
        schedule_an_IR(ir);
    }

    void _gen_an_IR_2pp_1i(unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     Compare_Extension cx,
                     unsigned dest_p1,
                     unsigned dest_p2,
                     unsigned isrc_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_preg(dest_p1,ir);
        encode_write_preg(dest_p2,ir);
        encode_read_ireg(isrc_reg,ir);
        ir.special_instr = ENC_SI_icmp | cmp_ext_to_special_instr[cx];
        schedule_an_IR(ir);
    }
    void _gen_an_IR_2ii_2ii (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned idest_reg1,
                     unsigned idest_reg2,
                     unsigned isrc_reg1,
                     unsigned isrc_reg2,
                     bool mem_access=false) {
        assert(!mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_ireg(idest_reg1,ir); 
        encode_write_ireg(idest_reg2,ir);
        encode_read_ireg(isrc_reg1,ir);
        encode_read_ireg(isrc_reg2,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_2ii_1i (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned idest_reg1,
                     unsigned idest_reg2,
                     unsigned isrc_reg,
                     bool mem_access=false) {    
        assert(!mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_ireg(idest_reg1,ir);
        encode_write_ireg(idest_reg2,ir);
        encode_read_ireg(isrc_reg,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_0_2ii (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned isrc_reg1,
                     unsigned isrc_reg2,
                     bool mem_access = false) {  
        assert(!mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_read_ireg(isrc_reg1,ir);
        encode_read_ireg(isrc_reg2,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_2fi_2ii (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned fdest_reg1,
                     unsigned idest_reg2,
                     unsigned isrc_reg1,
                     unsigned isrc_reg2,
                     bool mem_access=false) {
        assert(!mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_freg(fdest_reg1,ir);
        encode_write_ireg(idest_reg2,ir);
        encode_read_ireg(isrc_reg1,ir);
        encode_read_ireg(isrc_reg2,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_2fi_1i (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned fdest_reg1,
                     unsigned idest_reg2,
                     unsigned isrc_reg,
                     bool mem_access=false) {
        assert(!mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_freg(fdest_reg1,ir);
        encode_write_ireg(idest_reg2,ir);
        encode_read_ireg(isrc_reg,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_0_2fi (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned fsrc_reg1,
                     unsigned isrc_reg2,
                     bool mem_access=false) {
        assert(!mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_read_freg(fsrc_reg1,ir);
        encode_write_ireg(isrc_reg2,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1i_2fi (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned idest_reg,
                     unsigned fsrc_reg1,
                     unsigned isrc_reg2,
                     bool mem_access=false) {
        assert(!mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_ireg(idest_reg,ir);
        encode_read_freg(fsrc_reg1,ir);
        encode_read_ireg(isrc_reg2,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1f_1f (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned fdest_reg,
                     unsigned fsrc_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_freg(fdest_reg,ir);
        encode_read_freg(fsrc_reg,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1f_2ff (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned fdest_reg,
                     unsigned fsrc_reg1,
                     unsigned fsrc_reg2 ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_freg(fdest_reg,ir);
        encode_read_freg(fsrc_reg1,ir);
        encode_read_freg(fsrc_reg2,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1f_3fff (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned fdest_reg,
                     unsigned fsrc_reg1,
                     unsigned fsrc_reg2,
                     unsigned fsrc_reg3 ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_freg(fdest_reg,ir);
        encode_read_freg(fsrc_reg1,ir);
        encode_read_freg(fsrc_reg2,ir);
        encode_read_freg(fsrc_reg3,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_2fp_2ff (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned fdest_reg,
                     unsigned dest_p2,
                     unsigned fsrc_reg1,
                     unsigned fsrc_reg2 ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_freg(fdest_reg,ir);
        encode_write_preg(dest_p2,ir);
        encode_read_freg(fsrc_reg1,ir);
        encode_read_freg(fsrc_reg2,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1i_1b (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned idest_reg,
                     unsigned bsrc_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_ireg(idest_reg,ir);
        encode_read_breg(bsrc_reg,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1b_1i (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned bdest_reg,
                     unsigned isrc_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_breg(bdest_reg,ir);
        encode_read_ireg(isrc_reg,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_0_1b (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned bsrc_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
         encode_read_breg(bsrc_reg,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1b_0 (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned bdest_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_breg(bdest_reg,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1b_1b (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned bdest_reg,
                     unsigned bsrc_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_breg(bdest_reg,ir);
        encode_read_breg(bsrc_reg,ir);
        schedule_an_IR(ir);
    }
    
    void _gen_an_IR_1i_1f (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned idest_reg,
                     unsigned fsrc_reg ) {
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_ireg(idest_reg,ir);
        encode_read_freg(fsrc_reg,ir);
        schedule_an_IR(ir);
    }
    void _gen_an_IR_1f_1i (unsigned bca, EM_Syllable_Type syl_type, unsigned pred,
                     unsigned fdest_reg,
                     unsigned isrc_reg,
                     bool mem_access=false) {
        assert(!mem_access || (known_mem_type == (curr_mem_type != ENC_MT_unknown)));
        Encoder_Unscheduled_Instr_IR ir;
        _init_ir(ir, bca, syl_type, pred);
        encode_write_freg(fdest_reg,ir);
        encode_read_ireg(isrc_reg,ir);
        schedule_an_IR(ir);
    }
    
// memory management
    void * _alloc_space(size_t);
    void _free_arena(apr_memnode_t *);
    char *  _copy(char *buffer, apr_memnode_t *a);

// Code scheduling
    Bundle_IR * incr_wbuf_ptr(Bundle_IR * p) {
        if (++p==wbuf_end) p=wbuf;
        return p;
    }
    Bundle_IR * decr_wbuf_ptr(Bundle_IR * p) {
        if (p == wbuf) p=wbuf_end;
        return --p;
    }
    Bundle_IR * incr_wbuf_ptr_by(Bundle_IR * p, int k) {
        return (wbuf + ((p-wbuf)+k)%ENC_WBUF_LEN);
    }
    Instr_IR * incr_slot_ptr(Instr_IR * p) {
        if (++p == slot_end) p=slots;
        return p;
    }
    Instr_IR * decr_slot_ptr(Instr_IR * p) {
        if (p == slots) p=slot_end;
        return --p;
    }

    void new_bundle(); // start new bundle in a work buffer
    void new_bundle_with_no_emit(); // start new bundle in a work buffer assuming there's space
    virtual void emit_bundle(Bundle_IR * bundle_ir); // move one bundle from the work buffer
    void prepass_before_emit(Bundle_IR * first, Bundle_IR * last); // prepass over the bundles to be emitted
    void emit_several_bundles(Bundle_IR * first, Bundle_IR * last); // emit several bundles from the work buffer
    void emit_all();   // empty the work buffer
    void buffer_overflow();
    
    void reset_mem_type() {
       curr_mem_type = ENC_MT_unknown;
       curr_mem_value = 0;
       curr_is_mem_access = false;
       curr_exc=false;
    }
    // schedule an instr with no exchange
    void schedule_an_IR_ne (Unsch_Instr_IR &ir); 
    // schedule an instr with instr exchange
    void schedule_an_IR_ex (Unsch_Instr_IR &ir); 
    // call one of the above
    void schedule_an_IR (Encoder_Unscheduled_Instr_IR &ir);
    void place_instr_into_slot(Bundle_IR * bundle, int slot, Encoder_Unscheduled_Instr_IR& instr, unsigned need_stop);
    bool place_instr_into_bundle(Bundle_IR * bundle, Encoder_Unscheduled_Instr_IR& instr);
    bool instr_fits_into_slot(Bundle_IR * bundle, int slot, Unsch_Instr_IR& instr, unsigned& need_stop);
    bool instr_couple_fits_into_slots(Bundle_IR * bundle, unsigned slot1, unsigned slot2, 
                                      Unsch_Instr_IR& instr1, Unsch_Instr_IR & instr2,
                                      unsigned& stop_pos); 
    void place_instr_couple_into_slots(Bundle_IR * bundle, unsigned slot1, unsigned slot2,
              Unsch_Instr_IR& instr1, Unsch_Instr_IR& instr2, unsigned stop_pos) {
        if (instr1.is_target() || instr2.is_target()) {
            Unsch_Instr_IR * i1=&instr1, *i2=&instr2;
            if (slot2 < slot1) {i1=&instr2; i2=&instr1;}
            i1->set_is_target();
            i2->reset_is_target();
        }
        place_instr_into_slot(bundle,slot1,instr1, stop_pos && stop_pos == slot1);
#ifndef NDEBUG
        unsigned need_stop;
#endif
        assert(instr_fits_into_slot(bundle,slot2,instr2,need_stop));
        place_instr_into_slot(bundle,slot2,instr2, stop_pos && stop_pos == slot2);
    }
    Instr_IR * place_instr_couple_into_bundle(Bundle_IR * bundle, Unsch_Instr_IR& instr1,
                                              Unsch_Instr_IR& instr2, bool unord);
    void schedule_two_IR_ne(Unsch_Instr_IR & ir1, Unsch_Instr_IR& ir2, bool unord);
    void schedule_two_IR_ex(Unsch_Instr_IR & ir1, Unsch_Instr_IR& ir2, bool unord);

    // Patching
    void apply_patches(char * code_buffer, uint64* target_offset_tbl) {
        for (Patch * p=patches; p!=0; p=p->next())
            p->apply(code_buffer, target_offset_tbl);
    }
    void apply_brl_patches(char *code_buffer);
    void apply_brl_patch(MCE_brl_patch *patch, char *code_buffer);

private:

    void operator delete (void *) { LDIE(51, "Not implemented"); }


protected: // data
    // Memory management
    tl::MemoryPool   & mem_pool;    // memory manager for allocating arenas
    apr_allocator_t * allocator;
    apr_memnode_t   * arena;       // code arena

    // Instruction working area

    Bundle_IR wbuf[ENC_WBUF_LEN]; // a circular buffer for forming bundles
    Bundle_IR * wbuf_first;       // first instr in wbuf
    Bundle_IR * wbuf_last;        // last instr in wbuf
    Bundle_IR * const wbuf_end;   // end of wbuf
    Instr_IR  slots[ENC_GL_N_SLOTS]; // slot storage
    Instr_IR  * slot_end;         // end of slot storage
    int       last_empty_slot;    // slot number of last empty slot 
    // for exchange mode
    Instr_IR  * gl_first_empty_slot; // ptr to the first empty slot 
    Instr_IR  * empty_slots[ENC_MAX_EMPTY_SLOTS];  // an array of empty slot pointers
    Bundle_IR * empty_bdls[ENC_WBUF_LEN]; // an array of pointers to bdls with 2 empty slots

    // Code patching

    unsigned n_targets;      
    uint64 * target_offset;
    bool     next_instr_is_target;
    unsigned next_instr_target_id;
    bool     needs_patching;
    unsigned patch_target_id;
    uint64   curr_offset;
    Patch *  patches;

    MCE_brl_patch_list *brl_patches;

    // Register dependency check
    // General idea: represent a register as a bit. As long as there are
    // less than 64 registers use one uint64 for all bits(fast scheme).
    // If there are more registers - switch to a slow scheme
    // where each register is assigned a bit.

    // Fast scheme data:
    // a mapping from a register number to its bit position.
    // 8-th bit is 1 if register has not bit assigned a bit.
    U_8     reg_map[ENC_N_REG];
    U_8*    const ireg_map;    // pointers to the beginning of the reg map parts
    U_8*    const freg_map;
    U_8*    const breg_map;
    U_8*    const areg_map;
    U_8*    const preg_map;
    int    n_fast_reg;          // number of used regs with fast dependency check

    // Slow scheme data:
    Enc_All_Reg_BV  curr_wr_reg_vector, curr_rd_reg_vector;
    bool            fast_reg_dep_check;


    // Placing instruction into slot
    static const uint16 unavailable_tmplts[ST_last_type+1][2][ENC_N_SLOTS];

    // Other private data

    unsigned curr_bc_addr;      // current bytecode address
    Encoder_Memory_Type curr_mem_type;     // current memory type
    uint64   curr_mem_value;               // current memory value
    bool     curr_exc;          // next instruction might throw exception
    bool     curr_is_mem_access;     // next instruction is a memory access
    bool     wbuf_is_empty;   
    // current state in regard of placing two instr in the same bundle
    Coupled_Instr_State coupled_instr_state; 
    // current instruction couple can be scheduled in any order
    bool     curr_instr_couple_is_unordered; 
    //mapping from compare extensions to speical instructions 
    const static U_8 cmp_ext_to_special_instr[cmp_last];
#ifdef _DEBUG
    bool     emit_after_get_code_size;
    bool *   target_offset_is_set;
#endif
    // modes of operation
    bool     known_mem_type;
    bool     exch_instr;                  
    

    IPF_Encoder encoder0, *encoder;
#ifdef ENABLE_ENCODER_STATS
    void compute_n_dbl_slots();
#endif

};

////////////////////////////////////////////////////////////////////////////////////////
//                class Encoder_RefInfo
////////////////////////////////////////////////////////////////////////////////////////

// Information about registers and stack locations - reference or not, interior pointer or not,
// is offset constant or in register, what is the offset or its register number

#define ENC_reg_ref_pos(r)   (r)
#define ENC_stack_ref_pos(s) (ENC_N_GEN_REG + s)
#define ENC_RI_ref           0x0100000000000000  // it's a reference
#define ENC_RI_intr_ptr      0x0200000000000000  // it's an interior pointer
#define ENC_RI_reg           0x0400000000000000  // offset is in register/stack (0 - constant)
#define ENC_RI_base          0x0800000000000000  // it's a base for some interior pointer
#define ENC_RI_copy          0x1000000000000000  // copy info from the source (Compactor internal)
#define ENC_RI_offset_mask   0x00ffffff00000000  // mask to get an offset
#define ENC_RI_base_mask     0x00000000ffffffff  // mask to get a base
#define ENC_NOT_A_BASE       (unsigned)(-1)
#define ENC_offset(I)        (((uint64)I) << 32)

class Encoder_RefInfo {
public:
    Encoder_RefInfo(unsigned len, tl::MemoryPool& m) : _size(len) {
        _data = (uint64 *)m.alloc(_size * sizeof(uint64));
        for (unsigned i=0; i<_size; i++)
            _data[i]=0;
    }
    unsigned get_size() const { return _size;}
    void clear_all() {
        for (unsigned i=0;i<_size; i++)
            _data[i]=0;
    }
    void copy(Encoder_RefInfo * ri) {
        _size = ri->_size;
        for (unsigned i=0;i<_size; i++)
            _data[i]=ri->_data[i];
    }
    void copy(unsigned dest_pos, unsigned src_pos) { _data[dest_pos]=_data[src_pos];}
    void clear(unsigned pos) { _data[pos]=0;}
    void set(unsigned pos, uint64 val) { _data[pos]=val;}
    void add(unsigned pos, uint64 val) { _data[pos]|=val;}
    uint64 get(unsigned pos) { return _data[pos];}
    uint64 is_ref(unsigned pos) { return _data[pos] & ENC_RI_ref;}
    uint64 is_intr_ptr(unsigned pos) { 
        assert (is_ref(pos));
        return _data[pos] & ENC_RI_intr_ptr;
    }
    uint64 is_base(unsigned pos) {
        return _data[pos] & ENC_RI_base;
    }
    uint64 has_reg_offset(unsigned pos) {
        assert(is_intr_ptr(pos));
        return _data[pos] & ENC_RI_reg;
    }
    uint64 has_const_offset(unsigned pos) {
        assert(is_intr_ptr(pos));
        return !(_data[pos] & ENC_RI_reg);
    }
    unsigned get_offset(unsigned pos) {
        assert(is_intr_ptr(pos));
        return (unsigned)((_data[pos] & ENC_RI_offset_mask) >> 32);
    }
    unsigned get_base(unsigned pos) {
        assert(is_intr_ptr(pos));
        return (unsigned)(_data[pos] & ENC_RI_base_mask);
    }
protected:
    uint64   * _data;
    unsigned   _size;
};

// Class _Encoder_RefInfo contains an additional data structure
// to represent a set of interior pointers that refer to each base
// register or stack location

class _Encoder_RefInfo : public Encoder_RefInfo {
public:
    _Encoder_RefInfo(unsigned len, tl::MemoryPool& m) : Encoder_RefInfo(len,m) {
        _list = (unsigned *) m.alloc(len * sizeof(unsigned));
        for (unsigned i=0;i<len; i++)
            _list[i]=ENC_NOT_A_BASE;
    }
    void add_intr_ptr(unsigned ptr_pos, unsigned base_pos) {
        if (is_intr_ptr(ptr_pos))
            delete_intr_ptr(ptr_pos);
        assert(0 <= ptr_pos && ptr_pos < _size);
        _list[ptr_pos]=_list[base_pos];
        assert(0 <= base_pos && base_pos < _size);
        _list[base_pos]=ptr_pos;
    }
    void invalidate_intr_ptrs(unsigned base_pos) {
        unsigned i=base_pos;
        while (i != ENC_NOT_A_BASE) {
            unsigned next_i=_list[i];
            assert(is_intr_ptr(i));
            clear(i);
            assert (0 <= i && i< _size);
            _list[i]=ENC_NOT_A_BASE;
            i=next_i;
        }
    }
    void delete_intr_ptr(unsigned ptr_pos) {
        assert(is_intr_ptr(ptr_pos));
        unsigned base_pos = get_base(ptr_pos);
        unsigned i=base_pos;
        while (_list[i]!=ptr_pos) {
            assert(_list[i]!=ENC_NOT_A_BASE);
            i=_list[i];
        }
        assert (0<=i && i<_size);
        _list[i]=_list[ptr_pos];
    }
    void clear_all() {
        Encoder_RefInfo::clear_all();
        for (unsigned i=0;i<_size; i++) 
            _list[i]=ENC_NOT_A_BASE;
    }
    void clear(unsigned pos) {
        if (is_base(pos))
            invalidate_intr_ptrs(pos);
        else if (is_intr_ptr(pos))
            delete_intr_ptr(pos);
        _data[pos]=0;
    }
private:
    unsigned        * _list;
};

////////////////////////////////////////////////////////////////////////////////
//              class Merced_Code_Emitter_GC1
////////////////////////////////////////////////////////////////////////////////

// Emit code and collect GC information.
// Version 1, aka "simple"
// Collect a bit vector that indicates whether each integer register and
// stack location contains a reference. Also, let the compiler know when
// we hit a certain offset (GC point) in a generated code.

#define NOT_A_STACK_LOC          ((unsigned)-1)
#define RETURN_GR                8


class Merced_Code_Emitter_GC1 : public Merced_Code_Emitter {
public:
    Merced_Code_Emitter_GC1(tl::MemoryPool& m, unsigned byteCodeSize, unsigned nTargets,
                            unsigned maxStackLoc, uint64 gcPoint) :
          Merced_Code_Emitter(m,byteCodeSize,nTargets), 
          gc_point(gcPoint), max_stack_loc(maxStackLoc), 
          def_ref_bv_size(ENC_N_GEN_REG + maxStackLoc),
          gc_point_def_ref(def_ref_bv_size,m),
          call_site_def_ref(def_ref_bv_size,m),
          done_upto_GC(false),
          curr_cleared_refs(def_ref_bv_size,m), 
          last_call_site_def_ref(def_ref_bv_size,m)
    {
        reset_def_ref();
        gc_point_def_ref.clear_all();
        call_site_def_ref.clear_all();
#ifndef _NDEBUG
        unscheduled_call = false;
#endif
    }

    //virtual ~Merced_Code_Emitter_GC1() {};

    void *operator new (size_t sz, tl::MemoryPool& m) { return m.alloc(sz); }
    void operator delete (void *p, tl::MemoryPool& m) { }

    // check if all instructions before GC point have been issued?
    bool GC_point_is_done() { return done_upto_GC;}

    // get a reference bit vector for the GC point
    Encoder_RefInfo * get_GC_point_refs() { 
        assert(done_upto_GC); 
        return &gc_point_def_ref;
    }

    // set a target (i.e., begin a new basic block)

    void set_target(unsigned target_id) {
        Merced_Code_Emitter::set_target(target_id);
        gc_point_def_ref.clear_all();
        call_site_def_ref.clear_all();
    }

    // Emit instructions

    void ipf_add (unsigned dest, unsigned src1, unsigned src2, bool def_ref, unsigned pred=0) {
        set_def_ref(def_ref);
        curr_dest_ref = ENC_reg_ref_pos(dest);
        if (def_ref && src1 != 0) {
            curr_base = ENC_reg_ref_pos(src2);
            curr_ref_info = ENC_offset(src1) | ENC_RI_reg | ENC_RI_intr_ptr;
        }
        Merced_Code_Emitter::ipf_add(dest,src1,src2,pred);
        reset_def_ref();
    }
    void ipf_shladd (unsigned dest, unsigned src1, int count, unsigned src2, bool def_ref, unsigned pred=0) {
        assert (!def_ref);
        curr_dest_ref = ENC_reg_ref_pos(dest);
        Merced_Code_Emitter::ipf_shladd(dest,src1,count,src2,pred);
    }
    void ipf_adds (unsigned dest, int imm14, unsigned src, bool def_ref, unsigned pred=0) {
        set_def_ref(def_ref);
        curr_dest_ref = ENC_reg_ref_pos(dest);
        if (def_ref && imm14) {
            curr_base = ENC_reg_ref_pos(src);
            curr_ref_info = ENC_offset(imm14) | ENC_RI_intr_ptr;
        }
        Merced_Code_Emitter::ipf_adds(dest,imm14,src,pred);
        reset_def_ref();
    }
    void ipf_addl (unsigned dest, int imm22, unsigned src, bool def_ref, unsigned pred=0) {
        set_def_ref(def_ref);
        curr_dest_ref = ENC_reg_ref_pos(dest);
        if (def_ref && imm22) {
            curr_base = ENC_reg_ref_pos(src);
            curr_ref_info = ENC_offset(imm22) | ENC_RI_intr_ptr;
        }
        Merced_Code_Emitter::ipf_addl(dest,imm22,src,pred);
        reset_def_ref();
    }
    // Returns reference definition bit vector for the call site
    Encoder_RefInfo * ipf_bricall (Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, 
          Branch_Dealloc_Hint dh, unsigned b1, unsigned b2, bool def_ref, 
          Bit_Vector & cleared_refs, unsigned pred=0) {
        assert(!unscheduled_call);
        last_call_site_def_ref.copy(&call_site_def_ref);
        curr_dest_ref = ENC_reg_ref_pos(RETURN_GR);
        Merced_Code_Emitter::ipf_bricall(ph,wh,dh,b1,b2,pred);
#ifndef _NDEBUG
        unscheduled_call = true;
#endif
        curr_cleared_refs.copy_from(&cleared_refs);
        return &last_call_site_def_ref;
    }

    void ipf_mov (unsigned dest, unsigned src, bool def_ref, unsigned pred=0) { // pseudo-op
        ipf_adds(dest, 0, src,def_ref, pred);
    }
    void ipf_movi (unsigned dest, int imm22, bool def_ref, unsigned pred=0) { // pseudo-op
        ipf_addl(dest, imm22, 0, def_ref, pred);
    }
    void ipf_ld (Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, bool def_ref, unsigned stack_loc=NOT_A_STACK_LOC, unsigned pred=0) {
        set_def_ref(def_ref);
        assert(size!=int_mem_size_8 || curr_def_ref);
        curr_dest_ref = ENC_reg_ref_pos(dest);
        if (def_ref) {
            assert(stack_loc != NOT_A_STACK_LOC);
            curr_base = ENC_stack_ref_pos(stack_loc);
            curr_ref_info = ENC_RI_copy;
        }
        Merced_Code_Emitter::ipf_ld(size,flag,hint,dest,addrreg,pred);
        reset_def_ref();
    }
    void ipf_ld_inc_imm (Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_imm, bool def_ref, unsigned stack_loc=NOT_A_STACK_LOC, unsigned pred=0) {
        set_def_ref(def_ref);
        assert(size!=int_mem_size_8 || curr_def_ref);
        curr_dest_ref = ENC_reg_ref_pos(dest);
        if (def_ref) {
            assert(stack_loc != NOT_A_STACK_LOC);
            curr_base = ENC_stack_ref_pos(stack_loc);
            curr_ref_info = ENC_RI_copy;
        }
        Merced_Code_Emitter::ipf_ld_inc_imm(size,flag,hint,dest,addrreg,inc_imm,pred);
        reset_def_ref();
    }
    void ipf_st (Int_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src,  bool def_ref, unsigned stack_loc=NOT_A_STACK_LOC, unsigned pred=0) {
        set_def_ref(def_ref);
        assert(size!=int_mem_size_8 || curr_def_ref);
        if (stack_loc == NOT_A_STACK_LOC)
            curr_def_ref = ENC_REF_dontcare;
        else
            curr_dest_ref = ENC_stack_ref_pos(stack_loc);
        if (curr_def_ref == ENC_REF_set) {
            assert(stack_loc != NOT_A_STACK_LOC);
            curr_base = ENC_stack_ref_pos(stack_loc);
            curr_ref_info = ENC_RI_copy;
        }
        Merced_Code_Emitter::ipf_st(size,flag,hint,addrreg,src,pred);
        reset_def_ref();
    }
    void ipf_st_inc_imm (Int_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned inc_imm,  bool def_ref, unsigned stack_loc=NOT_A_STACK_LOC, unsigned pred=0) {
        set_def_ref(def_ref);
        assert(size!=int_mem_size_8 || curr_def_ref);
        if (stack_loc == NOT_A_STACK_LOC)
            curr_def_ref = ENC_REF_dontcare;
        else
            curr_dest_ref = ENC_stack_ref_pos(stack_loc);
        if (curr_def_ref == ENC_REF_set) {
            assert(stack_loc != NOT_A_STACK_LOC);
            curr_base = ENC_stack_ref_pos(stack_loc);
            curr_ref_info = ENC_RI_copy;
        }
        Merced_Code_Emitter::ipf_st_inc_imm(size,flag,hint,addrreg,src,inc_imm,pred);
        reset_def_ref();
    }
private: // functions

    void operator delete (void *) { LDIE(51, "Not implemented"); }

    virtual void emit_bundle(Bundle_IR * bundle);
    virtual void place_instr_into_slot(Bundle_IR * bundle, int slot, Unsch_Instr_IR& instr, unsigned need_stop);

    // Methods that work with Instruction IR
    virtual void _init_ir (Encoder_Unscheduled_Instr_IR &ir,
                   unsigned bytecode_addr,
                   EM_Syllable_Type syl_type,
                   unsigned pred) {
        Merced_Code_Emitter::_init_ir(ir,bytecode_addr,syl_type,pred);
        ir.def_ref = curr_def_ref;
        ir.dest_ref = curr_dest_ref;
        ir.ref_info = curr_ref_info;
        ir.base_pos = curr_base;
    }

    virtual void copy_ir_into_slot(Encoder_Instr_IR &dest, Encoder_Unscheduled_Instr_IR & src) {
        Merced_Code_Emitter::copy_ir_into_slot(dest,src);
        dest.def_ref=src.def_ref;
    }

    virtual void copy_unscheduled_ir(Encoder_Unscheduled_Instr_IR& dest, Encoder_Unscheduled_Instr_IR& src) {
        Merced_Code_Emitter::copy_unscheduled_ir(dest,src);
        dest.dest_ref = src.dest_ref;
        dest.ref_info = src.ref_info;
        dest.base_pos = src.base_pos;
    }
    // set whether instruction sets/resets reference
    void set_def_ref (bool def_ref) {
        curr_def_ref = def_ref? ENC_REF_set : ENC_REF_reset;
    }
    void reset_def_ref() {
        curr_def_ref = ENC_REF_dontcare;
        curr_base    = ENC_NOT_A_BASE;
    }

private: // data
    char              curr_def_ref;           // current defined reference
    unsigned          curr_dest_ref;          // current destination reference position in a bit vector
    unsigned          curr_base;              // base ptr position if might be interior poiter
    uint64            curr_ref_info;          // reference info for interior pointers 
    uint64            gc_point;               // code offset at which GC happend 
    unsigned          max_stack_loc;          // max size of the Java stack
    unsigned          def_ref_bv_size;        // size of the bit vector with ref info
    _Encoder_RefInfo  gc_point_def_ref;       // ref info at the GC point
    _Encoder_RefInfo  call_site_def_ref;      // ref info at call sites
    bool              done_upto_GC;           // flag - issued all instr upto GC point
    Bit_Vector        curr_cleared_refs;      // refs that need to be cleared after the call  
    Encoder_RefInfo   last_call_site_def_ref; // ref info at last call site
#ifndef _NDEBUG
    bool         unscheduled_call;     // there is a call that's not been scheduled
#endif
    
    
};


////////////////////////////////////////////////////////////////////////////////
//              class Merced_Code_Emitter_GC2
////////////////////////////////////////////////////////////////////////////////

// Emit code and collect GC information.
// Version 2, aka "Andrew's favorite"
// Collect a bit vector that indicates whether instruction sets/resets a reference.

class Merced_Code_Emitter_GC2 : public Merced_Code_Emitter {
public:
    Merced_Code_Emitter_GC2(tl::MemoryPool& m, unsigned byteCodeSize, unsigned nTargets) :
        Merced_Code_Emitter(m,byteCodeSize,nTargets),
            ref_bit_arena0(0), ref_bit_arena(0), n_ref_bit(0) {
        reset_def_ref();
        _alloc_ref_bit_arena(byteCodeSize * 2);
    }

    virtual ~Merced_Code_Emitter_GC2();

    void *operator new (size_t sz, tl::MemoryPool& m) { return m.alloc(sz); }
    void operator delete (void *p, tl::MemoryPool& m) { }

    // get number of reference bits
    unsigned get_n_ref_bit () { return n_ref_bit; }

    // copy reference bits into a buffer
    void copy_ref_bits (char *ref_bit_buffer);

    // emit instructions

    void ipf_add (unsigned dest, unsigned src1, unsigned src2, bool def_ref, unsigned pred=0) {
        set_def_ref(def_ref);
        Merced_Code_Emitter::ipf_add(dest,src1,src2,pred);
        reset_def_ref();
    }
    void ipf_shladd (unsigned dest, unsigned src1, int count, unsigned src2,bool def_ref, unsigned pred=0) {
        set_def_ref(def_ref);
        Merced_Code_Emitter::ipf_shladd(dest,src1,count,src2,pred);
        reset_def_ref();
    }
    void ipf_adds (unsigned dest, int imm14, unsigned src, bool def_ref, unsigned pred=0) {
        set_def_ref(def_ref);
        Merced_Code_Emitter::ipf_adds(dest,imm14,src,pred);
        reset_def_ref();
    }
    void ipf_addl (unsigned dest, int imm22, unsigned src, bool def_ref, unsigned pred=0) {
        set_def_ref(def_ref);
        Merced_Code_Emitter::ipf_addl(dest,imm22,src,pred);
        reset_def_ref();
    }
    void ipf_bricall (Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b1, unsigned b2, bool def_ref, unsigned pred=0) {
        set_def_ref(def_ref);
        Merced_Code_Emitter::ipf_bricall(ph,wh,dh,b1,b2,pred);
        reset_def_ref();
    }
    void ipf_mov (unsigned dest, unsigned src, bool def_ref, unsigned pred=0) { // pseudo-op
        ipf_adds(dest, 0, src, def_ref,pred);
    }
    void ipf_movi (unsigned dest, int imm22, bool def_ref, unsigned pred=0) { // pseudo-op
        ipf_addl(dest, imm22, 0, def_ref,pred);
    }
    void ipf_ld (Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, bool def_ref, unsigned pred=0) {
        if (size==int_mem_size_8)
            set_def_ref(def_ref);
        Merced_Code_Emitter::ipf_ld(size,flag,hint,dest,addrreg,pred);
        reset_def_ref();
    }
    void ipf_ld_inc_imm (Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_imm, bool def_ref, unsigned pred=0) {
        if (size==int_mem_size_8)
            set_def_ref(def_ref);
        Merced_Code_Emitter::ipf_ld_inc_imm(size,flag,hint,dest,addrreg,inc_imm,pred);
        reset_def_ref();
    }
    void ipf_st (Int_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, bool def_ref, unsigned pred=0) {
        if (size==int_mem_size_8)
            set_def_ref(def_ref);
        Merced_Code_Emitter::ipf_st(size,flag,hint,addrreg,src,pred);
        reset_def_ref();
    }
    void ipf_st_inc_imm (Int_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned inc_imm, bool def_ref, unsigned pred=0) {
        if (size==int_mem_size_8)
            set_def_ref(def_ref);
        Merced_Code_Emitter::ipf_st_inc_imm(size,flag,hint,addrreg,src,inc_imm,pred);
        reset_def_ref();
    }

private:

    void operator delete (void *) { LDIE(51, "Not implemented"); }

    // set whether instruction sets/resets reference
    void set_def_ref (bool def_ref) {
        curr_def_ref = def_ref? ENC_REF_set : ENC_REF_reset;
    }
    void reset_def_ref() {
        curr_def_ref = ENC_REF_dontcare;
    }

    // Code scheduling
    virtual void emit_bundle(Bundle_IR * bundle);

    // Memory allocation
    void _alloc_ref_bit_arena(unsigned nbytes);

    // Reference bit functions

    void encode_def_ref_bit(U_8 def_ref);

    // Methods that work with Instruction IR
    virtual void _init_ir (Encoder_Unscheduled_Instr_IR &ir,
                   unsigned bytecode_addr,
                   EM_Syllable_Type syl_type,
                   unsigned pred) {
        Merced_Code_Emitter::_init_ir(ir,bytecode_addr,syl_type,pred);
        ir.def_ref = curr_def_ref;
    }

    virtual void copy_ir_into_slot(Encoder_Instr_IR &dest, Encoder_Unscheduled_Instr_IR & src) {
        Merced_Code_Emitter::copy_ir_into_slot(dest,src);
        dest.def_ref=src.def_ref;
    }

private:  // data
    char curr_def_ref;              // current defined reference
    apr_memnode_t *ref_bit_arena0, *ref_bit_arena;
    unsigned n_ref_bit;   
};


////////////////////////////////////////////////////////////////////////////////
//                     Statistics gathering
////////////////////////////////////////////////////////////////////////////////

#endif /* _CODE_EMITTER_H */

