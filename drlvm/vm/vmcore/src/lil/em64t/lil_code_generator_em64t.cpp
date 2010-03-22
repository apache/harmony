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
 * @author Evgueni Brevnov
 */  

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include "open/types.h"
#include "tl/memory_pool.h"

#include "encoder.h"

#include "lil.h"
#include "lil_code_generator.h"
#include "lil_code_generator_em64t.h"
#include "lil_code_generator_utils.h"
#include "m2n.h"
#include "m2n_em64t_internal.h"

#ifndef NDEBUG
#include "dump.h"
#endif

LcgEM64TContext::LcgEM64TContext(LilCodeStub * stub, tl::MemoryPool & m):
    cs(stub), mem(m), iter(cs, true) {

    /*    1) PRE-INITIALIZATION    */ 
    n_inputs = 0;
    n_gr_inputs = 0;
    n_fr_inputs = 0;

    n_outputs = 0;
    n_gr_outputs = 0;
    n_fr_outputs = 0;

    m_tmp_grs_used = 0;
    m_tmp_xmms_used = 0;

    stk_input_gr_save_size = 0;
    stk_input_fr_save_size = 0;
    stk_output_size = 0;
    stk_alloc_size = 0;

    m2n_handles = false;
    does_normal_calls = false;
    does_tail_calls = false;
    calls_unmanaged_code = false;
    has_m2n = false;
    save_inputs = false;

    /*    2) SCAN THE CODE STUB FOR THE INFORMATION    */

    while (!iter.at_end()) {
        lil_visit_instruction(iter.get_current(), this);
        iter.goto_next();
    }

    /*    3) INITIALIZE INPUTS ACCORDING TO THE ENTRY SIGNATURE    */
    if (lil_sig_is_arbitrary(lil_cs_get_sig(stub))) {
        n_gr_inputs = MAX_GR_OUTPUTS;
        n_fr_inputs = MAX_FR_OUTPUTS;
        n_inputs = n_gr_inputs + n_fr_outputs;
        if (does_normal_calls) {
            save_inputs = true;
        }
    } else {
        n_inputs = lil_sig_get_num_args(lil_cs_get_sig(stub));
        // TODO: check if it makes sense to move to separate function
        for (unsigned i = 0; i < n_inputs; i++) {
            if (is_fp_type(lil_sig_get_arg_type(lil_cs_get_sig(stub), i))) {
                n_fr_inputs++;
            } else {
                n_gr_inputs++;
            }
        }
        n_gr_inputs = n_gr_inputs > MAX_GR_OUTPUTS ? MAX_GR_OUTPUTS : n_gr_inputs;
        n_fr_inputs = n_fr_inputs > MAX_FR_OUTPUTS ? MAX_FR_OUTPUTS : n_fr_inputs;
    }

    /*    4) INITILIZE STACK INFORMATION    */

    if (has_m2n) {
        stk_m2n_size = (unsigned)(m2n_get_size() - 2*sizeof(void*));
    } else {
        // preserve space for callee-saves registers
        stk_m2n_size = lil_cs_get_max_locals(stub) * GR_SIZE;
    }

    // stack size needed for saving GR & FR inputs
    if (must_save_inputs()) {
        // TODO: check if we need alignment here
        stk_input_gr_save_size = n_gr_inputs * GR_SIZE;
        stk_input_fr_save_size = n_fr_inputs * FR_SIZE;
    }

    // determine the size of the stack frame
    stk_size =
        stk_m2n_size +              // memory for m2n frame
        stk_input_gr_save_size +    // GR input spill space
        stk_input_fr_save_size +    // FR input spill space
        stk_alloc_size +            // memory for dynamic allocation        
        stk_output_size;            // outputs on the stack
    
    // allign stack
    if (stk_size % 16 == 0) {
        stk_size += 8;
        stk_alloc_size +=8;
    }
}

/**
 * Implementation notes:
 *     1) Current implementation doesn't correctly processes back branches when
 *        input arguments are accessed
 */
class LcgEM64TCodeGen: public LilInstructionVisitor {

    /**
     * Maximum possible size of code generated for a single LIL instruction.
     *
     * Speculatevely calculated as a sequence of code that used all available 
     * registers (e.g. potential prolog or epilog) * 2. The real maximum sizes 
     * are far this speculation (about 120 bytes), so I hardly believe we will 
     * ever reach this theoretical limit even with deep changes in the LIL codegen.
     * As the buffer size is predicted basing on this high estimation, we're safe 
     * in the terms of buffer overruns in current codeged.
     */
    static const unsigned   MAX_LIL_INSTRUCTION_CODE_LENGTH = 512*2;
    
    static const unsigned   MAX_DEC_SIZE = 20; // maximum number of bytes required for string representation of int64
    
    char                    * buf_beg; // pointer to the beginning position of the generated code
    char                    * buf;     // pointer to the current position of the generated code

    LilCguLabelAddresses    labels; // a set of defined labels and theirs addresses

    LilCodeStub             * cs;
    LcgEM64TContext         & context;
    tl::MemoryPool          & mem;
    LilInstructionIterator  iter;
        
    LilInstructionContext   * ic;   // visit functions can always assume that inst points to the 
    LilInstruction          * inst; // current instruction and ic points to the current context

    const LcgEM64TLoc       * rsp_loc; // location denoting rsp register
    bool                    take_inputs_from_stack; // true if inputs were preserved on the stack
                                                    // and should be taken from that location
    unsigned                current_alloc;  // keeps track of memory allocation

private:

    /*    Inner Classes    */

    class Tmp_GR_Opnd: public R_Opnd {

    private:
	/// Ref to compilation context
	LcgEM64TContext& m_context;
	/// Returns *reference* to the counter of currently allocated GR registers
	unsigned & get_num_used_reg(void) {
            return m_context.get_tmp_grs_used();
	}

    public:
        Tmp_GR_Opnd(LcgEM64TContext & context, LilInstructionContext * ic): R_Opnd(n_reg), m_context(context) {
            // next temporary register is allocated from unused scratched
            // registers in the following order:
            // ret, std0, std1, out0, out1, out2, out3, out4, out5
            unsigned cur_tmp_reg = 0;
            if (lil_ic_get_ret_type(ic) == LT_Void) {
                if (cur_tmp_reg == get_num_used_reg()) {
                    _reg_no = context.get_reg_from_map(LcgEM64TContext::GR_RETURNS_OFFSET).reg_no(); // should be rax
                    ++get_num_used_reg();
                    return;
                } else {
                    ++cur_tmp_reg;
                }
            }
            for (unsigned i = lil_ic_get_num_std_places(ic); i < LcgEM64TContext::MAX_STD_PLACES; i++) {
                if (cur_tmp_reg == get_num_used_reg()) {
                    _reg_no = context.get_reg_from_map(LcgEM64TContext::STD_PLACES_OFFSET + i).reg_no();
                    ++get_num_used_reg();
                    return;
                } else {
                    ++cur_tmp_reg;
                }
            }
            for (unsigned i = context.get_num_gr_inputs(); i < LcgEM64TContext::MAX_GR_OUTPUTS; i++) {
                if (cur_tmp_reg == get_num_used_reg()) {
                    _reg_no = context.get_reg_from_map(LcgEM64TContext::GR_OUTPUTS_OFFSET + i).reg_no();
                    ++get_num_used_reg();
                    return;
                } else {
                    ++cur_tmp_reg;
                }
            }
            DIE(("LIL INTERNAL ERROR: Not enough temporary registers"));
        }

        virtual ~Tmp_GR_Opnd() {
            --get_num_used_reg();
        }
    };

    class Tmp_FR_Opnd: public XMM_Opnd {

    private:
	/// Ref to compilation context
	LcgEM64TContext& m_context;
	/// Returns *reference* to the counter of currently allocated XMM registers
	unsigned & get_num_used_reg(void) {
	    return m_context.get_tmp_xmms_used();
	}

    public:
        Tmp_FR_Opnd(LcgEM64TContext& context, LilInstructionContext * ic): XMM_Opnd(0), m_context(context) {
            // next temporary register is allocated from unused scratched
            // registers in the following order:
            // xmm8, xmm9, ... xmm15
            ASSERT(get_num_used_reg() < LcgEM64TContext::MAX_FR_TEMPORARY + LcgEM64TContext:: MAX_FR_LOCALS,
                ("LIL INTERNAL ERROR: Not enough temporary registers"));
            m_idx = LcgEM64TContext::get_xmm_reg_from_map(
                LcgEM64TContext::FR_TEMPORARY_OFFSET + get_num_used_reg()).get_idx();
            ++get_num_used_reg();
        }

        virtual ~Tmp_FR_Opnd() {
            --get_num_used_reg();
        }
    };

    /*    Helper functions */
    
    /**
     * Estimates maximum possible code size for the current stub.
     */
    unsigned estimate_code_size(void) {
	unsigned num_insts = lil_cs_get_num_instructions(cs);
	return num_insts*MAX_LIL_INSTRUCTION_CODE_LENGTH;
    }

    /**
     * returns the location of the n'th int local
     */
    const LcgEM64TLoc * get_gp_local(const unsigned n) const {
        assert(n < LcgEM64TContext::MAX_GR_LOCALS);
        return new(mem) LcgEM64TLoc(LLK_Gr, LcgEM64TContext::GR_LOCALS_OFFSET + n);
    }

    // returns the location of the n'th fp local
    const LcgEM64TLoc * get_fp_local(const unsigned n) const {
        // DO NOT SUPPORT FP LOCALS
        LDIE(44, "Not supported");
        return NULL;
        /*
        assert(n < context.get_num_fr_locals());
        if (does_normal_calls) {
            return new(mem) LcgEM64TLoc(LLK_FStk, context.get_local_fr_offset() + n * FR_SIZE);
        else {
            return new(mem) LcgEM64TLoc(LLK_Fr, LcgEM64TContext::FR_LOCALS_OFFSET + n);
        }
        */
    }

    // returns the location of the n'th standard place
    const LcgEM64TLoc * get_std_place(const unsigned n) const {
        assert(n < LcgEM64TContext::MAX_STD_PLACES);
        return new(mem) LcgEM64TLoc(LLK_Gr, LcgEM64TContext::STD_PLACES_OFFSET + n);
    }

    /**
     * returns number of bytes required for the given type on the stack
     */
    unsigned get_num_bytes_on_stack(LilType t) const {
        switch (t) {
        case LT_Void:
            return 0;
        case LT_G1:
        case LT_G2:
        case LT_G4:
        case LT_F4:
            return 4;
        case LT_Ref:
        case LT_PInt:
        case LT_G8:
        case LT_F8:
            return 8;
        default:
            DIE(("Unknown LIL type"));
        }
        return 0;
    }

    // returns location of the n'th input
    // location of the n'th input can vary depending on the current instruction context
    // in case there was a call then return input saved on the memory stack
    const LcgEM64TLoc * get_input(const unsigned n) const {
        assert(n < context.get_num_inputs());
#ifndef NDEBUG
        if (take_inputs_from_stack) {
            assert(context.must_save_inputs());
        }
#endif
        LilType t;
        unsigned gp_param_cnt = 0;
#ifdef _WIN64
#define fp_param_cnt gp_param_cnt
#else
        unsigned fp_param_cnt = 0;
#endif
        for (unsigned i = 0; i < n; i++) {
            t = lil_sig_get_arg_type(lil_cs_get_sig(cs), i);
            if (context.is_fp_type(t)) {
                ++fp_param_cnt;
            } else {
                ++gp_param_cnt;
            }
        }

        // type of n'th input
        t = lil_sig_get_arg_type(lil_cs_get_sig(cs), n);

        if (context.is_fp_type(t)) {
            if (fp_param_cnt < LcgEM64TContext::MAX_FR_OUTPUTS) {
                if (take_inputs_from_stack) {
                    // compute rsp-relative offset of the bottom of FR save area
                    I_32 offset = context.get_input_fr_save_offset() +
                        fp_param_cnt * LcgEM64TContext::FR_SIZE;
                    return new(mem) LcgEM64TLoc(LLK_FStk, offset);
                } else {
                    return new(mem) LcgEM64TLoc(LLK_Fr,
                        LcgEM64TContext::FR_OUTPUTS_OFFSET + fp_param_cnt);
                }
            } else {
                unsigned gp_on_stack = gp_param_cnt > LcgEM64TContext::MAX_GR_OUTPUTS
                    ? (gp_param_cnt - LcgEM64TContext::MAX_GR_OUTPUTS)
                    * LcgEM64TContext::GR_SIZE : 0;
                unsigned fp_on_stack = fp_param_cnt > LcgEM64TContext::MAX_FR_OUTPUTS
                    ? (fp_param_cnt - LcgEM64TContext::MAX_FR_OUTPUTS)
                    * LcgEM64TContext::FR_SIZE : 0;
                // compute rsp-relative offset of the top of the activation frame
                I_32 offset = context.get_stk_size();
                // skip rip
                offset += LcgEM64TContext::GR_SIZE;
                // skip size allocated for preceding inputs
                offset += gp_on_stack;
#ifndef _WIN64
                offset += fp_on_stack;
#endif
                return new(mem) LcgEM64TLoc(LLK_FStk, offset);
            }
        } else { // if (context.is_fp_type(t))
            if (gp_param_cnt < LcgEM64TContext::MAX_GR_OUTPUTS) {
                if (take_inputs_from_stack) {
                    // compute rsp-relative offset of the bottom of GR save area
                    I_32 offset = context.get_input_gr_save_offset() +
                        gp_param_cnt * LcgEM64TContext::GR_SIZE;
                    return new(mem) LcgEM64TLoc(LLK_GStk, offset);
                } else {
                    return new(mem) LcgEM64TLoc(LLK_Gr,
                        LcgEM64TContext::GR_OUTPUTS_OFFSET + gp_param_cnt);
                }
            } else {
                unsigned gp_on_stack = gp_param_cnt > LcgEM64TContext::MAX_GR_OUTPUTS
                    ? (gp_param_cnt - LcgEM64TContext::MAX_GR_OUTPUTS)
                    * LcgEM64TContext::GR_SIZE : 0;
                unsigned fp_on_stack = fp_param_cnt > LcgEM64TContext::MAX_FR_OUTPUTS
                    ? (fp_param_cnt - LcgEM64TContext::MAX_FR_OUTPUTS)
                    * LcgEM64TContext::FR_SIZE : 0;
                // compute rsp-relative offset of the top of the activation frame
                I_32 offset = context.get_stk_size();
                // skip rip
                offset += LcgEM64TContext::GR_SIZE;
                // skip size allocated for preceding inputs
                offset += gp_on_stack;
#ifndef _WIN64
                offset += fp_on_stack;
#endif
                return new(mem) LcgEM64TLoc(LLK_GStk, offset);
            }
        }
    }


    // returns the location of the n'th output
    const LcgEM64TLoc * get_output(const unsigned n, LilSig * out_sig) const {
        assert(n <= context.get_num_outputs());

        LilType t;
        unsigned gp_param_cnt = 0;
#ifdef _WIN64
#define fp_param_cnt gp_param_cnt
#else
        unsigned fp_param_cnt = 0;
#endif
        for (unsigned i = 0; i < n; i++) {
            t = lil_sig_get_arg_type(out_sig, i);
            if (context.is_fp_type(t)) {
                ++fp_param_cnt;
            } else {
                ++gp_param_cnt;
            }
        }

        // type of n'th output
        t = lil_sig_get_arg_type(out_sig, n);

        if (context.is_fp_type(t)) {
            if (fp_param_cnt < LcgEM64TContext::MAX_FR_OUTPUTS) {
                return new(mem) LcgEM64TLoc(LLK_Fr,
                    LcgEM64TContext::FR_OUTPUTS_OFFSET + fp_param_cnt);
            } else {
                unsigned gp_on_stack = gp_param_cnt > LcgEM64TContext::MAX_GR_OUTPUTS
                    ? (gp_param_cnt - LcgEM64TContext::MAX_GR_OUTPUTS)
                    * LcgEM64TContext::GR_SIZE : 0;
                unsigned fp_on_stack = fp_param_cnt > LcgEM64TContext::MAX_FR_OUTPUTS
                    ? (fp_param_cnt - LcgEM64TContext::MAX_FR_OUTPUTS)
                    * LcgEM64TContext::FR_SIZE : 0;
                I_32 offset = gp_on_stack;
#ifndef _WIN64
                offset += fp_on_stack;
#endif
                return new(mem) LcgEM64TLoc(LLK_FStk, offset);
            }
        } else {
            if (gp_param_cnt < LcgEM64TContext::MAX_GR_OUTPUTS) {
                return new(mem) LcgEM64TLoc(LLK_Gr,
                    LcgEM64TContext::GR_OUTPUTS_OFFSET + gp_param_cnt);
            } else {
                unsigned gp_on_stack = gp_param_cnt > LcgEM64TContext::MAX_GR_OUTPUTS
                    ? (gp_param_cnt - LcgEM64TContext::MAX_GR_OUTPUTS)
                    * LcgEM64TContext::GR_SIZE : 0;
                unsigned fp_on_stack = fp_param_cnt > LcgEM64TContext::MAX_FR_OUTPUTS
                    ? (fp_param_cnt - LcgEM64TContext::MAX_FR_OUTPUTS)
                    * LcgEM64TContext::FR_SIZE : 0;
                I_32 offset = gp_on_stack;
#ifndef _WIN64
                offset += fp_on_stack;
#endif
                return new(mem) LcgEM64TLoc(LLK_GStk, offset);
            }
        }
    }

    /**
     * returns the location of a LIL operand
     * (input, output, local, std place, or return value)
     * current instruction and instruction context is used
     * is_lvalue: true if the variable is used as an l-value
     */
    const LcgEM64TLoc * get_op_loc(LilOperand * operand, bool is_lvalue) const {
        assert(!lil_operand_is_immed(operand));
        return get_var_loc(lil_operand_get_variable(operand), is_lvalue);
    }

    /**
     * returns the location of a LIL variable
     * (input, output, local, std place, or return value)
     * current instruction and instruction context is used
     * is_lvalue: true if the variable is used as an l-value
     */
    const LcgEM64TLoc * get_var_loc(LilVariable * var, bool is_lvalue) const {
        unsigned index = lil_variable_get_index(var);
        switch (lil_variable_get_kind(var)) {
        case LVK_In:
            return get_input(index);
        case LVK_StdPlace:
            return get_std_place(index);
        case LVK_Out: {
            LilSig * out_sig = lil_ic_get_out_sig(ic);
            assert(out_sig != NULL);
            return get_output(index, out_sig);
        }
        case LVK_Local: {
            //LilType t = is_lvalue
            //    ? lil_instruction_get_dest_type(cs, inst, ic)
            //    : lil_ic_get_local_type(ic, index);
            //return context.is_fp_type(t) ? get_fp_local(index) : get_gp_local(index);
            // no support for fp locals
            return get_gp_local(index);
        }
        case LVK_Ret: {
            //LilType t = is_lvalue
            //    ? lil_instruction_get_dest_type(cs, inst, ic)
            //    : lil_ic_get_ret_type(ic);
            LilType t = lil_ic_get_ret_type(ic);
            return context.is_fp_type(t)
                ? new(mem) LcgEM64TLoc(LLK_Fr, LcgEM64TContext::FR_RETURNS_OFFSET)
                : new(mem) LcgEM64TLoc(LLK_Gr, LcgEM64TContext::GR_RETURNS_OFFSET);
        }
        default:
            DIE(("Unknown variable kind"));
        }
        return NULL;  // should never be reached
    }

    inline int64 get_imm_value(LilOperand * op) const {
        assert(lil_operand_is_immed(op));
        return lil_operand_get_immed(op);
    }

    inline const Imm_Opnd & get_imm_opnd(LilOperand * op, Opnd_Size sz = n_size) const {
        return get_imm_opnd(get_imm_value(op), sz);
    }
    
    inline const Imm_Opnd & get_imm_opnd(int64 value, Opnd_Size sz = n_size) const {
        void * const mem_ptr = mem.alloc(sizeof(Imm_Opnd));
        if (sz == n_size) {
            return *(new(mem_ptr) Imm_Opnd(fit32(value) ? size_32 : size_64, value));
        }
        return *(new(mem_ptr) Imm_Opnd(sz, value));
    }
    
    inline const R_Opnd & get_r_opnd(const LcgEM64TLoc * loc) const {
        assert(loc->kind == LLK_Gr);
        return context.get_reg_from_map((unsigned)loc->addr);
    }

    inline const XMM_Opnd & get_xmm_r_opnd(const LcgEM64TLoc * loc) const {
        assert(loc->kind == LLK_Fr);
        return context.get_xmm_reg_from_map((unsigned)loc->addr);
    }

    inline const M_Opnd & get_m_opnd(const LcgEM64TLoc * loc) const {
        assert(loc->kind == LLK_GStk || loc->kind == LLK_FStk);
        void * const mem_ptr = mem.alloc(sizeof(M_Base_Opnd));
        return *(new(mem_ptr) M_Base_Opnd(rsp_reg, (I_32)loc->addr));
    }

    inline const RM_Opnd & get_rm_opnd(const LcgEM64TLoc * loc) const {
        assert(loc->kind == LLK_Gr || loc->kind == LLK_GStk);
        if (loc->kind == LLK_Gr) {
            return get_r_opnd(loc);
        }
        //return M_Base_Opnd(rsp_reg, loc->addr);
        return get_m_opnd(loc);
    }

    inline void adjust_stack_pointer(I_32 offset) {
        if (offset != 0) {
            buf = alu(buf, add_opc, rsp_opnd, get_imm_opnd(offset), size_64);
        }
    }

    // move integer immediate to GR or memory stack
    void move_imm(const LcgEM64TLoc * dest, int64 imm_val) {
        assert(dest->kind == LLK_Gr || dest->kind == LLK_GStk);
        buf = mov(buf, get_rm_opnd(dest), get_imm_opnd(imm_val), size_64);
    }

    // move between two register or stack locations
    void move_rm(const LcgEM64TLoc* dest, const LcgEM64TLoc* src) {
        if (*dest == *src) {
            return; // nothing to be done
        }
        if (dest->kind == LLK_Gr || dest->kind == LLK_GStk) {
            if (src->kind == LLK_Gr || src->kind == LLK_GStk) {
                if (dest->kind == LLK_Gr) {
                    buf = mov(buf, get_r_opnd(dest), get_rm_opnd(src), size_64);
                    return;
                }
                // dest->kind != LLK_Gr
                if (src->kind == LLK_Gr) {
                    buf = mov(buf, get_m_opnd(dest), get_r_opnd(src), size_64);
                    return;
                }
                // src->kind != LLK_Gr
                // use temporary register
                Tmp_GR_Opnd tmp_reg(context, ic);
                buf = mov(buf, tmp_reg, get_m_opnd(src), size_64);
                buf = mov(buf, get_m_opnd(dest), tmp_reg, size_64);
            } else { // src->kind == LLK_Fr || src->kind == LLK_FStk
                // src->kind == LLK_Fr supported only
                assert(src->kind == LLK_Fr);
                buf = movq(buf, get_rm_opnd(dest), get_xmm_r_opnd(src));
                return;
            }
        } else { // dest->kind == LLK_Fr || dest->kind == LLK_FStk
            if (src->kind == LLK_Fr || src->kind == LLK_FStk) {
                if (dest->kind == LLK_Fr) {
                    if (src->kind == LLK_Fr) {
                        buf = sse_mov(buf, get_xmm_r_opnd(dest), get_xmm_r_opnd(src), true);
                    } else {
                        buf = sse_mov(buf, get_xmm_r_opnd(dest), get_m_opnd(src), true);
                    }
                    return;
                }
                // dest->kind != LLK_Gr
                if (src->kind == LLK_Fr) {
                    buf = sse_mov(buf, get_m_opnd(dest), get_xmm_r_opnd(src), true);
                    return;
                }
                // src->kind != LLK_Gr
                // use temporary register
                Tmp_FR_Opnd tmp_reg(context, ic);
                buf = sse_mov(buf, tmp_reg, get_m_opnd(src), true);
                buf = sse_mov(buf, get_m_opnd(dest), tmp_reg, true);
            } else { // src->kind == LLK_Gr || src->kind == LLK_GStk
                // dest->kind == LLK_Fr supported only
                assert(dest->kind == LLK_Fr);
                buf = movq(buf, get_xmm_r_opnd(dest), get_rm_opnd(src));
                return;
            }
        }
    }

    void shift_op_imm_rm(const LcgEM64TLoc * dest, I_32 imm_val, const LcgEM64TLoc * src) {
        assert(dest->kind == LLK_Gr || dest->kind == LLK_GStk);
        assert(src->kind == LLK_Gr || src->kind == LLK_GStk);
        // this code sequence can be optimized if dest is located in memory
        move_imm(dest, imm_val);
        bin_op_rm_rm(LO_Shl, dest, dest, src);
    }

    void shift_op_rm_imm(const LcgEM64TLoc * dest, const LcgEM64TLoc * src, I_32 imm_val) {
        const Imm_Opnd & imm = get_imm_opnd(imm_val);
        if (src->kind == LLK_Gr) {
        if (dest->kind == LLK_Gr) {
            buf = mov(buf, get_r_opnd(dest), get_r_opnd(src), size_64);
            } else {
                buf = mov(buf, get_m_opnd(dest), get_r_opnd(src), size_64);
            }
            buf = shift(buf, shl_opc, get_rm_opnd(dest), imm);
            return;
        }
        // src->kind != LLK_Gr
        if (dest->kind == LLK_Gr) {
            buf = mov(buf, get_r_opnd(dest), get_m_opnd(src), size_64);
            buf = shift(buf, shl_opc, get_r_opnd(dest), imm, size_64);
            return;
        }
        // dest->kind != LLK_Gr
        Tmp_GR_Opnd tmp_reg(context, ic);
        buf = mov(buf, tmp_reg, get_m_opnd(src), size_64);
        buf = shift(buf, shl_opc, tmp_reg, imm);
        buf = mov(buf, get_m_opnd(dest), tmp_reg);
    }

    // subtract op where the first op is immediate
    // (allowed only for intefer values)
    void sub_op_imm_rm(const LcgEM64TLoc * dest, I_32 imm_val, const LcgEM64TLoc * src) {
        assert(dest->kind == LLK_Gr || dest->kind == LLK_GStk);
        assert(src->kind == LLK_Gr || src->kind == LLK_GStk);
        // TODO: this code sequence can be optimized if dest is located in memory
        alu_op_rm_imm(add_opc, dest, src, -imm_val);
    }

    void alu_op_rm_imm(const ALU_Opcode alu_opc, const LcgEM64TLoc * dest,
                       const LcgEM64TLoc * src, I_32 imm_val) {
        assert(alu_opc < n_alu);
        assert(dest->kind == LLK_Gr || dest->kind == LLK_GStk);
        assert(src->kind == LLK_Gr || src->kind == LLK_GStk);
        const Imm_Opnd & imm = get_imm_opnd(imm_val);
        if (*dest == *src) {
            buf = alu(buf, alu_opc, get_rm_opnd(dest), imm, size_64);
            return;
        }
        // dest != src
        if (dest->kind == LLK_Gr) {
            buf = mov(buf, get_r_opnd(dest), get_rm_opnd(src), size_64);
            buf = alu(buf, alu_opc, get_r_opnd(dest), imm, size_64);
            return;
        }
        // dest->kind != LLK_Gr
        if (src->kind == LLK_Gr) {
            buf = mov(buf, get_m_opnd(dest), get_r_opnd(src), size_64);
            buf = alu(buf, alu_opc, get_m_opnd(dest), imm, size_64);
            return;
        }
        // src->kind == LLK_Gr
        Tmp_GR_Opnd tmp_reg(context, ic);
        buf = mov(buf, tmp_reg, get_m_opnd(src), size_64);
        buf = alu(buf, alu_opc, tmp_reg, imm, size_64);
        buf = mov(buf, get_m_opnd(dest), tmp_reg, size_64);
    }

    void alu_op_rm_rm(const ALU_Opcode alu_opc, const LcgEM64TLoc * dest,
                      const LcgEM64TLoc * src1, const LcgEM64TLoc * src2) {
        assert(dest->kind == LLK_Gr || dest->kind == LLK_GStk);
        assert(src1->kind == LLK_Gr || src1->kind == LLK_GStk);
        assert(src2->kind == LLK_Gr || src2->kind == LLK_GStk);

        if (*dest == *src1) {
            if (dest->kind == LLK_Gr) {
                buf = alu(buf, alu_opc, get_r_opnd(dest), get_rm_opnd(src2));
                return;
            }
            // dest->kind != LLK_Gr
            if (src2->kind == LLK_Gr) {
                buf = alu(buf, alu_opc, get_m_opnd(dest), get_r_opnd(src2));
                return;
            }
            // src2->kind != LLK_Gr
            Tmp_GR_Opnd tmp_reg(context, ic);
            buf = mov(buf, tmp_reg, get_m_opnd(src2), size_64);
            buf = alu(buf, alu_opc, get_m_opnd(dest), tmp_reg);
            return;
        }
        // dest != src1
        if (dest->kind == LLK_Gr) {
            buf = mov(buf, get_r_opnd(dest), get_rm_opnd(src1), size_64);
            buf = alu(buf, alu_opc, get_r_opnd(dest), get_rm_opnd(src2));
            return;
        }
        // dest->kind != LLK_Gr
        Tmp_GR_Opnd tmp_reg(context, ic);
        buf = mov(buf, tmp_reg, get_rm_opnd(src1), size_64);
        buf = alu(buf, alu_opc, tmp_reg, get_rm_opnd(src2));
        buf = mov(buf, get_m_opnd(dest), tmp_reg, size_64);
    }

    // where the second operand is immediate (allowed only for integer values!)
    void bin_op_rm_imm(LilOperation o, const LcgEM64TLoc* dest,
                       const LcgEM64TLoc* src, I_32 imm_val) {
        assert(dest->kind == LLK_Gr || dest->kind == LLK_GStk);
        assert(src->kind == LLK_Gr || src->kind == LLK_GStk);

        switch (o) {
        case LO_Add:
            return alu_op_rm_imm(add_opc, dest, src, imm_val);
        case LO_Sub:
            return alu_op_rm_imm(sub_opc, dest, src, imm_val);
        case LO_And:
            return alu_op_rm_imm(and_opc, dest, src, imm_val);
        case LO_SgMul: {
            const Imm_Opnd & imm = get_imm_opnd(imm_val);
            if (dest->kind == LLK_Gr) {
                if (*dest == *src) {
                    buf = imul(buf, get_r_opnd(dest), imm);
                } else {
                    buf = imul(buf, get_r_opnd(dest), get_rm_opnd(src), imm);
                }
                return;
            }
            // dest->kind != LLK_Gr
            Tmp_GR_Opnd tmp_reg(context, ic);
            buf = mov(buf, tmp_reg, get_rm_opnd(src), size_64);
            buf = imul(buf, tmp_reg, imm);
            buf = mov(buf, get_m_opnd(dest), tmp_reg, size_64);
            break;
        }
        case LO_Shl:
            shift_op_rm_imm(dest, src, imm_val);
            break;
        default:
            DIE(("Unexpected operation"));
        }
    }

    // binary arithmetic operations without immediates
    // (allowed only for integer values!)
    void bin_op_rm_rm(LilOperation o, const LcgEM64TLoc * dest,
                      const LcgEM64TLoc * src1, const LcgEM64TLoc * src2) {
        assert(dest->kind == LLK_Gr || dest->kind == LLK_GStk);
        assert(src1->kind == LLK_Gr || src1->kind == LLK_GStk);
        assert(src2->kind == LLK_Gr || src2->kind == LLK_GStk);

        switch (o) {
        case LO_Add:
            return alu_op_rm_rm(add_opc, dest, src1, src2);
        case LO_Sub:
            return alu_op_rm_rm(sub_opc, dest, src1, src2);
        case LO_And:
            return alu_op_rm_rm(and_opc, dest, src1, src2);
        case LO_SgMul: {
            if (dest->kind == LLK_Gr) {
                if (dest != src1) {
                    buf = mov(buf, get_r_opnd(dest), get_rm_opnd(src1), size_64);
                }
                buf = imul(buf, get_r_opnd(dest), get_rm_opnd(src2));
                return;
            }
            // dest->kind != LLK_Gr
            Tmp_GR_Opnd tmp_reg(context, ic);
            buf = mov(buf, tmp_reg, get_rm_opnd(src1), size_64);
            buf = imul(buf, tmp_reg, get_rm_opnd(src2));
            buf = mov(buf, get_m_opnd(dest), tmp_reg, size_64);
            break;
        }
        case LO_Shl: {
            move_rm(dest, src1);
            const Tmp_GR_Opnd tmp_reg(context, ic);
            const R_Opnd * src2_reg;
            if (src2->kind == LLK_Gr) {
                src2_reg = &get_r_opnd(src2);
            } else {
                src2_reg = &tmp_reg;
                buf = mov(buf, tmp_reg, get_m_opnd(src2), size_64);
            }
            buf = shift(buf, shl_opc, get_rm_opnd(dest), *src2_reg);
            break;
        }
        default:
            DIE(("Unexpected operation"));  // control should never reach this point
        }
    }

    // unary operation without immediates
    void un_op_rm(LilOperation o, const LcgEM64TLoc * dest, const LcgEM64TLoc * src) {
        assert(dest->kind == LLK_Gr || dest->kind == LLK_GStk);
        assert(src->kind == LLK_Gr || src->kind == LLK_GStk);

        const Tmp_GR_Opnd tmp_reg(context, ic);
        const R_Opnd & dest_reg =
            (dest->kind == LLK_Gr ? get_r_opnd(dest) : (const R_Opnd &)tmp_reg);

        switch (o) {
        case LO_Neg:
            buf = mov(buf, dest_reg, get_rm_opnd(src), size_64);
            buf = neg(buf, dest_reg, size_64);
            break;
        case LO_Not:
            buf = mov(buf, dest_reg, get_rm_opnd(src), size_64);
            buf = _not(buf, dest_reg, size_64);
            break;
        case LO_Sx1:
            buf = movsx(buf, dest_reg, get_rm_opnd(src), size_8);
            break;
        case LO_Sx2:
            buf = movsx(buf, dest_reg, get_rm_opnd(src), size_16);
            break;
        case LO_Sx4:
            buf = movsx(buf, dest_reg, get_rm_opnd(src), size_32);
            break;
        case LO_Zx1:
            buf = movzx(buf, dest_reg, get_rm_opnd(src), size_8);
            break;
        case LO_Zx2:
            buf = movzx(buf, dest_reg, get_rm_opnd(src), size_16);
            break;
        case LO_Zx4:
            // movzx r64, r/m32 is not available on em64t
            // mov r32, r/m32 should zero out upper bytes
            buf = mov(buf, dest_reg, get_rm_opnd(src), size_32);
            break;
        default:
            DIE(("Unexpected operation"));  // control should never reach this point
        }

        if (dest->kind != LLK_Gr) {
            buf = mov(buf, get_m_opnd(dest), tmp_reg, size_64);
        }
    }

    // generate instructions that calculate a LIL address
    const M_Opnd & get_effective_addr(LilVariable * base, unsigned scale,
                                           LilVariable * index, int64 offset,
                                           const R_Opnd & tmp_reg) {
        void * const M_Index_Opnd_mem = mem.alloc(sizeof(M_Index_Opnd));
        // handle special case
        if (base == NULL && (index == NULL || scale == 0)) {
            buf = mov(buf, tmp_reg, Imm_Opnd(size_64, offset));
            return *(new(M_Index_Opnd_mem) M_Index_Opnd(tmp_reg.reg_no(), n_reg, 0, 0));
        }

        // initialize locations
        const bool is_offset32 = fit32(offset);
        const LcgEM64TLoc * base_loc = base != NULL ? get_var_loc(base, false) : NULL;
        const LcgEM64TLoc * index_loc = index != NULL && scale != 0 ? get_var_loc(index, false) : NULL;
        const bool is_base_in_mem = base_loc != NULL && base_loc->kind == LLK_GStk;
        const bool is_index_in_mem = index_loc != NULL && index_loc->kind == LLK_GStk;

        // check if there is no need to use temporary register
        if (is_offset32 && !is_base_in_mem && !is_index_in_mem) {
                return *(new(M_Index_Opnd_mem) M_Index_Opnd(
                    base_loc != NULL ? get_r_opnd(base_loc).reg_no() : n_reg,
                    index_loc != NULL ?  get_r_opnd(index_loc).reg_no() : n_reg,
                    (U_32)offset, scale));
        }

        // check if it's enough to use only one temporary register
        if (is_offset32 && (is_base_in_mem || is_index_in_mem)) {
            if (is_base_in_mem) {
                buf = mov(buf, tmp_reg, get_m_opnd(base_loc), size_64);
                return *(new(M_Index_Opnd_mem) M_Index_Opnd(tmp_reg.reg_no(),
                    index_loc != NULL ?  get_r_opnd(index_loc).reg_no() : n_reg,
                    (U_32)offset, scale));
            }
            if (is_index_in_mem) {
                buf = mov(buf, tmp_reg, get_m_opnd(index_loc), size_64);
                return *(new(M_Index_Opnd_mem) M_Index_Opnd(
                    base_loc != NULL ? get_r_opnd(base_loc).reg_no() : n_reg,
                    tmp_reg.reg_no(), (U_32)offset, scale));
            }
            DIE(("Should never reach this point"));
        }

        // need to perform manual calculation of the effective address
        const LcgEM64TLoc * ret_loc =
            new(mem) LcgEM64TLoc(LLK_Gr, LcgEM64TContext::get_index_in_map(tmp_reg.reg_no()));
        if (index_loc != NULL) {
            I_32 shift = scale / 4 + 1;
            bin_op_rm_imm(LO_Shl, ret_loc, index_loc, shift);
            if (base_loc != NULL) {
                bin_op_rm_rm(LO_Add, ret_loc, ret_loc, base_loc);
            }
            if (!is_offset32) {
                Tmp_GR_Opnd tmp_reg2(context, ic);
                const LcgEM64TLoc * tmp_loc =
                    new(mem) LcgEM64TLoc(LLK_Gr, LcgEM64TContext::get_index_in_map(tmp_reg2.reg_no()));
                move_imm(tmp_loc, offset);
                bin_op_rm_rm(LO_Add, ret_loc, ret_loc, tmp_loc);
                offset = 0;
            }
        } else if (base_loc != NULL) {
            // index_loc is NULL
            if (is_offset32) {
                move_rm(ret_loc, base_loc);
            } else {
                move_imm(ret_loc, offset);
                bin_op_rm_rm(LO_Add, ret_loc, ret_loc, base_loc);
                offset = 0;
            }

        } else {
            assert(!is_offset32);
            move_imm(ret_loc, offset);
            offset = 0;
        }
        return *(new(M_Index_Opnd_mem) M_Base_Opnd(get_r_opnd(ret_loc).reg_no(), (U_32)offset));
    }

    // sets up stack frame
    void prolog() {        
        // push callee-saves registers on the stack
        if (lil_cs_get_max_locals(cs) > 0) {
            assert(context.get_stk_size() != 0);
            for (unsigned i = 0; i < lil_cs_get_max_locals(cs); i++) {
                const LcgEM64TLoc * loc = get_gp_local(i);
                assert(loc->kind == LLK_Gr);
                buf = push(buf, get_r_opnd(loc), size_64);
            }
        }
        adjust_stack_pointer(lil_cs_get_max_locals(cs) * LcgEM64TContext::GR_SIZE -
            context.get_stk_size());
    }

    // unsets the stack frame
    void epilog() {
        adjust_stack_pointer(context.get_stk_size() -
            lil_cs_get_max_locals(cs) * LcgEM64TContext::GR_SIZE);

        if (lil_cs_get_max_locals(cs) > 0) {
            assert(context.get_stk_size() != 0);
            // pop callee-saves registers from the stack
            for (int i = lil_cs_get_max_locals(cs) - 1; i >= 0; i--) {
                const LcgEM64TLoc * loc = get_gp_local(i);
                assert(loc->kind == LLK_Gr);
                buf = pop(buf, get_r_opnd(loc), size_64);
            }
        }
    }

    // moves GR & FR inputs into the stack
    void move_inputs() {
        if (!context.must_save_inputs()) {
            return;
        }  

        // compute the rsp-relative offset of the top of the GR save area
        I_32 offset = (I_32)context.get_input_gr_save_offset() +
            context.get_stk_input_gr_save_size();
        // store GR inputs to the computed locations
        for (int i = context.get_num_gr_inputs() - 1; i >= 0; i--) {
            const R_Opnd & r_opnd =
                LcgEM64TContext::get_reg_from_map(LcgEM64TContext::GR_OUTPUTS_OFFSET + i);
            offset -= LcgEM64TContext::GR_SIZE;
            const M_Opnd dest(rsp_reg, offset);
            buf = mov(buf, dest, r_opnd);
        }
        // compute the rsp-relative offset of the top of the FR save area
        offset = (I_32)context.get_input_fr_save_offset() +
            context.get_stk_input_fr_save_size();
        // store FR inputs to the computed locations
        for (int i = context.get_num_fr_inputs() - 1; i >= 0; i--) {
            const XMM_Opnd & r_opnd =
                LcgEM64TContext::get_xmm_reg_from_map(LcgEM64TContext::FR_OUTPUTS_OFFSET + i);
            offset -= LcgEM64TContext::FR_SIZE;
            const M_Opnd dest(rsp_reg, offset);
            buf = sse_mov(buf, dest, r_opnd, true);
        }
    }

    // moves GR & FR inputs from the stack back to the registers
    void unmove_inputs() {
        if (!context.must_save_inputs()) {
            // inputs should be already in place
            return;
        }
        // compute the rsp-relative offset of the bottom of the FR save area
        I_32 offset = (I_32)context.get_input_fr_save_offset();
        // restore FR inputs
        for (unsigned i = 0; i < context.get_num_fr_inputs(); i++) {
            const XMM_Opnd & r_opnd =
                LcgEM64TContext::get_xmm_reg_from_map(LcgEM64TContext::FR_OUTPUTS_OFFSET + i);
            const M_Opnd src(rsp_reg, offset);
            buf = sse_mov(buf, r_opnd, src, true);
            offset += LcgEM64TContext::FR_SIZE;
        }
        // compute the rsp-relative offset of the bottom of the GR save area
        offset = (I_32)context.get_input_gr_save_offset();
        // restore GR inputs
        for (unsigned i = 0; i < context.get_num_gr_inputs(); i++) {
            const R_Opnd & r_opnd =
                LcgEM64TContext::get_reg_from_map(LcgEM64TContext::GR_OUTPUTS_OFFSET + i);
            const M_Opnd src(rsp_reg, offset);
            buf = mov(buf, r_opnd, src, size_64);
            offset += LcgEM64TContext::GR_SIZE;
        }
    }

    Opnd_Size type_to_opnd_size(LilType t) const {
        switch (t) {
        case LT_G1:
            return size_8;
        case LT_G2:
            return size_16;
        case LT_G4:
            return size_32;
        case LT_G8:
        case LT_PInt:
        case LT_Ref:
            return size_64;
        default:
            return n_size;
        }
    }

public:

    /**
     * constructor
     */
    LcgEM64TCodeGen(LilCodeStub * cs, LcgEM64TContext & c, tl::MemoryPool & m):
        buf_beg(NULL), buf(NULL),
        labels(&m, buf_beg), cs(cs), context(c), mem(m), iter(cs, true), ic(NULL), inst(NULL),        
        rsp_loc(new(m) LcgEM64TLoc(LLK_Gr, LcgEM64TContext::RSP_OFFSET)), take_inputs_from_stack(false),
        current_alloc(0) {

	unsigned max_code_size = estimate_code_size();
	
	buf = buf_beg = (char *)m.alloc(max_code_size);
	char* buf_end = buf_beg + max_code_size;
	
	static unsigned max_code = 0;
	
        // emit entry code
	char* i_start = buf;
	
	// the size of code for prolog accounted in estimate_code_size
	// as the code for 'entry' instruction.
	
        prolog();
        move_inputs();
	
	// debug code: check the estimate	
        char* i_end = buf;
	unsigned i_len = (unsigned)(i_end - i_start);
	if (i_len > MAX_LIL_INSTRUCTION_CODE_LENGTH) {
	    // the MAX_LIL_INSTRUCTION_CODE_LENGTH was underestimated.
	    // most likely will not cause problems in real life, though still requires correction.
	    assert(false);
	}
	    
        // go through the instructions
        while (!iter.at_end()) {
	    char* i_start = buf;
	    
            ic = iter.get_context();
            inst = iter.get_current();
            lil_visit_instruction(inst, this);
            iter.goto_next();
	    
	    // debug code: see above for the rationale
	    char* i_end = buf;
	    unsigned i_len = (unsigned)(i_end - i_start);
	    if (i_len > MAX_LIL_INSTRUCTION_CODE_LENGTH) {
		assert(false);
	    }
        }
	
	if (buf>buf_end) {
	    /// Ugh. Buffer overrun.
	    /// Must be accompanied with previous assert()-s on MAX_LIL_INSTRUCTION_CODE_LENGTH?
	    assert(false);
	}
    }

    /**
     * memory allocator
     */
    void *operator new(size_t sz, tl::MemoryPool & m) {
        return m.alloc(sz);
    }

    /**
     * returns actual size of the generated code
     */
    size_t get_size() const {
        return buf - buf_beg;
    }

    /**
     * returns the beginning of the code
     */
    NativeCodePtr copy_stub(NativeCodePtr base) const {
        memcpy((void *)base, buf_beg, get_size());
        //labels.change_base((char *)base);
        return base;
    }


    /*    Visitor Functions    */

    void label(LilLabel lab) {
        labels.define_label(lab, buf, true);
    }

    void locals(unsigned num) {
        // nothing to be done here;
    }

    void std_places(unsigned num) {
        // nothing to be done here;
    }

    void alloc(LilVariable * var, unsigned sz) {
        I_32 alloc_offset = context.get_alloc_start_offset() + current_alloc;
        // the actual size allocated will always be a multiple of 8
        current_alloc += align_8(sz);
        // var = sp + alloc_offset
        bin_op_rm_imm(LO_Add, get_var_loc(var, true), rsp_loc, alloc_offset);
    }

    void asgn(LilVariable * dest, enum LilOperation o, LilOperand * op1, LilOperand * op2) {
        if (dest->tag == LVK_Out) {
            // since inputs and outputs occupies same registers we need to take inputs from the stack
            take_inputs_from_stack = true;
        }
        const LcgEM64TLoc * dest_loc = get_var_loc(dest, true);
        if (o == LO_Mov) {
            if (lil_operand_is_immed(op1)) {
                move_imm(dest_loc, get_imm_value(op1));
            } else {
                const LcgEM64TLoc * src_loc = get_op_loc(op1, false);
                move_rm(dest_loc, src_loc);
            }
            return;
        }
        // check if this is binary operation
        if (lil_operation_is_binary(o)) {
            if (lil_operand_is_immed(op1) && lil_operand_is_immed(op2)) {
                // type-convert to get signed types of same length
                assert(fit32(get_imm_value(op1)));
                I_32 op1_imm = (I_32)get_imm_value(op1);
                assert(fit32(get_imm_value(op2)));
                I_32 op2_imm = (I_32)get_imm_value(op2); 
                I_32 result = 0;
                switch (o) {
                case LO_Add:
                    result = op1_imm + op2_imm;
                    break;
                case LO_Sub:
                    result = op1_imm - op2_imm;
                    break;
                case LO_SgMul:
                    result = op1_imm * op2_imm;
                    break;
                case LO_Shl:
                    result = op1_imm << op2_imm;
                    break;
                default:
                    DIE(("Unexpected operation"));   // control should never reach this point
                }
                move_imm(dest_loc, result);
            } else if (lil_operand_is_immed(op1)) {
                assert(fit32(get_imm_value(op1)));
                const I_32 op1_imm = (I_32)get_imm_value(op1);
                const LcgEM64TLoc * op2_loc = get_op_loc(op2, false);
                switch (o) {
                case LO_Add:
                    bin_op_rm_imm(LO_Add, dest_loc, op2_loc, op1_imm);
                    break;
                case LO_Sub:
                    sub_op_imm_rm(dest_loc, op1_imm, op2_loc);
                    break;
                case LO_SgMul:
                    bin_op_rm_imm(LO_SgMul, dest_loc, op2_loc, op1_imm);
                case LO_Shl:
                    shift_op_imm_rm(dest_loc, op1_imm, op2_loc);
                    break;
                default:
                    DIE(("Unexpected operation"));  // control should never reach this point
                }
            } else if (lil_operand_is_immed(op2)) {
                const LcgEM64TLoc*  op1_loc = get_op_loc(op1, false);
                assert(fit32(get_imm_value(op2)));
                const I_32 op2_imm = (I_32)get_imm_value(op2);
                bin_op_rm_imm(o, dest_loc, op1_loc, op2_imm);
            } else {  // both operands non-immediate
                const LcgEM64TLoc * src1_loc = get_op_loc(op1, false);
                const LcgEM64TLoc * src2_loc = get_op_loc(op2, false);
                bin_op_rm_rm(o, dest_loc, src1_loc, src2_loc);
            }
        } else { // unary operation
            if (lil_operand_is_immed(op1)) {
                assert(fit32(get_imm_value(op1)));
                I_32 imm = (I_32)get_imm_value(op1);
                I_32 result = 0;
                switch (o) {
                case LO_Neg:
                    result = -imm;
                    break;
                case LO_Not:
                    result = ~imm;
                    break;
                case LO_Sx1:
                    result = (I_32) (I_8) imm;
                    break;
                case LO_Sx2:
                    result = (I_32) (int16) imm;
                    break;
                case LO_Sx4:
                    result = (I_32) (I_32) imm;
                    break;
                case LO_Zx1:
                    result = (I_32) (uint64) (U_8) imm;
                    break;
                case LO_Zx2:
                    result = (I_32) (uint64) (uint16) imm;
                    break;
                case LO_Zx4:
                    result = (I_32) (uint64) (U_32) imm;
                    break;
                default:
                    DIE(("Unexpected operation"));  // control should never reach this point
                }
                move_imm(dest_loc, result);
            } else { // non-immediate operand
                const LcgEM64TLoc * op1_loc = get_op_loc(op1, false);
                un_op_rm(o, dest_loc, op1_loc);
            }
        }
    }

    // TODO: think over if it makes sense to preserve a register for VM_Tread pointer
    void ts(LilVariable * var) {
        const LcgEM64TLoc * var_loc = get_var_loc(var, true);
        assert(var_loc->kind == LLK_Gr || var_loc->kind == LLK_GStk);

        if (var_loc->kind == LLK_Gr) {
            buf = m2n_gen_ts_to_register(buf, &get_r_opnd(var_loc),
                lil_ic_get_num_locals(ic), lil_cs_get_max_locals(cs),
                lil_ic_get_num_std_places(ic), lil_ic_get_ret_type(ic) == LT_Void ? 0 : 1);
        } else {
            const Tmp_GR_Opnd tmp(context, ic);
            buf = m2n_gen_ts_to_register(buf, &tmp,
                lil_ic_get_num_locals(ic), lil_cs_get_max_locals(cs),
                lil_ic_get_num_std_places(ic), lil_ic_get_ret_type(ic) == LT_Void ? 0 : 1);
            buf = mov(buf, get_m_opnd(var_loc), tmp, size_64);
        }

        take_inputs_from_stack = true;
    }
        
    void handles(LilOperand * op) {
        if (lil_operand_is_immed(op)) {
            buf = m2n_gen_set_local_handles_imm(buf,
                context.get_m2n_offset(), &get_imm_opnd(op));
        } else {
            const LcgEM64TLoc * loc = get_op_loc(op, false);
            if (loc->kind == LLK_Gr) {
                buf = m2n_gen_set_local_handles_r(buf,
                    context.get_m2n_offset(), &get_r_opnd(loc));
            } else {
                const Tmp_GR_Opnd tmp(context, ic);
                buf = m2n_gen_set_local_handles_r(buf,
                    context.get_m2n_offset(), &tmp);
                buf = mov(buf, get_m_opnd(loc), tmp, size_64);
            }
        }
    }

    void ld(LilType t, LilVariable * dest, LilVariable * base, unsigned scale,
            LilVariable * index, long long int offset, LilAcqRel acqrel, LilLdX ext) {

        assert(t != LT_F4 && t != LT_F8 && t != LT_Void);
        assert(acqrel == LAR_Acquire || acqrel == LAR_None);

        const Tmp_GR_Opnd tmp_reg1(context, ic);
        const Tmp_GR_Opnd * tmp_reg2 = NULL;

        // calculate the address
        const M_Opnd & addr = get_effective_addr(base, scale, index, offset, tmp_reg1);
        const LcgEM64TLoc * dest_loc = get_var_loc(dest, true);
        assert(dest_loc->kind == LLK_Gr || dest_loc->kind == LLK_GStk);
        void * const mem_ptr = mem.alloc(sizeof(Tmp_GR_Opnd));
        const R_Opnd * dest_reg = dest_loc->kind == LLK_Gr ? &get_r_opnd(dest_loc) :
            (tmp_reg2 = new(mem_ptr) Tmp_GR_Opnd(context, ic));

        Opnd_Size size = n_size;
        switch(t) {
        case LT_G1:
            size = size_8;
            break;
        case LT_G2:
            size = size_16;
            break;
        case LT_G4:
            size = size_32;
            break;
        case LT_G8:
        case LT_Ref:
        case LT_PInt:
            buf = mov(buf, *dest_reg, addr, size_64);
            goto move_to_destination;
        default:
            DIE(("Unexpected LIL type"));  // invalid value in type
        }

        assert(size != n_size);

        if (ext == LLX_Zero) {
            // movzx r64, r/m32 is not available on em64t
            // mov r32, r/m32 should zero out upper bytes
            if (size == size_32) {
                buf = mov(buf, *dest_reg, addr, size);
            } else {
                buf = movzx(buf, *dest_reg, addr, size);
            }
        } else {
            buf = movsx(buf, *dest_reg, addr, size);
        }
move_to_destination:
        if (dest_loc->kind != LLK_Gr) {
            buf = mov(buf, get_m_opnd(dest_loc), *dest_reg, size_64);
            delete dest_reg;
        }
    }

    void st(LilType t, LilVariable * base, unsigned scale, LilVariable * index,
            long long int offset, LilAcqRel acqrel, LilOperand * src) {
        assert(t != LT_F4 && t != LT_F8 && t != LT_Void);
        assert(acqrel == LAR_Release || acqrel == LAR_None);

        const Tmp_GR_Opnd tmp_reg1(context, ic);
        // calculate the address
        const M_Opnd & addr = get_effective_addr(base, scale, index, offset, tmp_reg1);

        if (lil_operand_is_immed(src)) {
            buf = mov(buf, addr, get_imm_opnd(src), type_to_opnd_size(t));
        } else {
            const LcgEM64TLoc * src_loc = get_op_loc(src, false);
            if (src_loc->kind == LLK_Gr) {
                buf = mov(buf, addr, get_r_opnd(src_loc), type_to_opnd_size(t));
            } else {
                const Tmp_GR_Opnd tmp_reg2(context, ic);
                buf = mov(buf, tmp_reg2, get_m_opnd(src_loc), type_to_opnd_size(t));
                buf = mov(buf, addr, tmp_reg2, type_to_opnd_size(t));
            }
        }
    }  // st

    void inc(LilType t, LilVariable* base, unsigned scale, LilVariable* index,
             long long int offset, LilAcqRel acqrel) {
        assert(acqrel == LAR_None);

        Tmp_GR_Opnd tmp_reg(context, ic);
        // calculate the address
        const M_Opnd & addr = get_effective_addr(base, scale, index, offset, tmp_reg);
        buf = ::inc(buf, addr, type_to_opnd_size(t));

    }

    void cas(LilType t, LilVariable * base, unsigned scale,
             LilVariable * index, long long int offset, LilAcqRel acqrel,
             LilOperand * cmp, LilOperand * src, LilLabel label) {
        // this is either rax register itself or keeps value of rax
        const Tmp_GR_Opnd tmp_reg(context, ic);

        const LcgEM64TLoc * cmp_loc = lil_operand_is_immed(cmp) ? NULL : get_op_loc(cmp, false);
        const LcgEM64TLoc * src_loc = lil_operand_is_immed(src) ? NULL : get_op_loc(src, false);

        bool is_rax_used = tmp_reg.reg_no() != rax_reg;
        bool is_rax_used_by_cmp = (cmp_loc != NULL && cmp_loc->kind == LLK_Gr
            && get_r_opnd(cmp_loc).reg_no() == rax_reg);
        if (is_rax_used && !is_rax_used_by_cmp) {
            // need to preserve rax value
            buf = mov(buf, tmp_reg, rax_opnd, size_64);
        }

        if (!is_rax_used_by_cmp) {
            if (cmp_loc == NULL) { // cmp is immediate
                buf = mov(buf, rax_opnd, get_imm_opnd(cmp), type_to_opnd_size(t));
            } else {
                buf = mov(buf, rax_opnd, get_rm_opnd(cmp_loc), type_to_opnd_size(t));
            }
        }

        const Tmp_GR_Opnd tmp_reg1(context, ic);
        // calculate the address
        const M_Opnd & addr = get_effective_addr(base, scale, index, offset, tmp_reg1);

        void * const mem_ptr = mem.alloc(sizeof(Tmp_GR_Opnd));
        const R_Opnd * src_reg = NULL;
        const R_Opnd * src_reg2 = NULL;
        if (src_loc == NULL) { // src is immediate value
            src_reg = new(mem_ptr) Tmp_GR_Opnd(context, ic);
            buf = mov(buf, *src_reg, get_imm_opnd(src), type_to_opnd_size(t));
        } else if (src_loc->kind == LLK_GStk) {
            src_reg = new(mem_ptr) Tmp_GR_Opnd(context, ic);
            buf = mov(buf, *src_reg, get_m_opnd(src_loc), type_to_opnd_size(t));
        } else if (is_rax_used && get_r_opnd(src_loc).reg_no() == rax_reg) {
            // value of rax was saved in tmp_reg
            src_reg2 = &tmp_reg;
        } else {
            src_reg2 = &get_r_opnd(src_loc);
        }
        buf = prefix(buf, lock_prefix);
        buf = cmpxchg(buf, addr, src_reg != NULL ? *src_reg : *src_reg2,
            type_to_opnd_size(t));       
        buf = branch32(buf, Condition_NE, get_imm_opnd((int64)0, size_32));
        labels.add_patch_to_label(label, buf - 4, LPT_Rel32);

        delete src_reg;

        if (is_rax_used && src_reg2->reg_no() != rax_reg) {
            // move preserved value back to rax
            buf = mov(buf, rax_reg, tmp_reg, size_64);
        }
    }

    void j(LilLabel lab) {
        buf = jump32(buf, get_imm_opnd((int64)0, size_32));
        labels.add_patch_to_label(lab, buf - 4, LPT_Rel32);
    }

    void jc(LilPredicate p, LilOperand * op1, LilOperand * op2, LilLabel label) {
        // compute the condition
        ConditionCode cc = Condition_O;

        switch (p) {
        case LP_Eq:
        case LP_IsZero:
            cc = Condition_E;
            break;
        case LP_Ne:
        case LP_IsNonzero:
            cc = Condition_NE;
            break;
        case LP_Le:
            cc = Condition_LE;
            break;
        case LP_Ule:
            cc = Condition_BE;
            break;
        case LP_Lt:
            cc = Condition_L;
            break;
        case LP_Ult:
            cc = Condition_B;
            break;
        default:
            DIE(("Unknown predicate"));
        }

        void * const mem_ptr = mem.alloc(sizeof(Tmp_GR_Opnd));
        const RM_Opnd * src1 = NULL;
        const Tmp_GR_Opnd * tmp_reg = NULL;
        if (!lil_predicate_is_binary(p)) {
            op2 = (LilOperand *)mem.alloc(sizeof(LilOperand));
            op2->is_immed = true;
            op2->val.imm = 0;
        }
        if (lil_operand_is_immed(op1) && lil_operand_is_immed(op2)) {
            tmp_reg = new(mem_ptr) Tmp_GR_Opnd(context, ic);
            src1 = tmp_reg;
            buf = mov(buf, *tmp_reg, get_imm_opnd(op1), size_64);
        } else {
            if (lil_operand_is_immed(op1)) {
                assert(!lil_operand_is_immed(op2));
                LilOperand * tmp_op = op1;
                op1 = op2;
                op2 = tmp_op;
                switch (cc) {
                case Condition_LE:
                    cc = Condition_G;
                    break;
                case Condition_L:
                    cc = Condition_GE;
                    break;
                default:;
                }
            }
            assert(!lil_operand_is_immed(op1));
            src1 = &get_rm_opnd(get_op_loc(op1, false));
        }
        // src1 is set here (not an immediate)
        if (lil_operand_is_immed(op2)) {
            int64 imm = lil_operand_get_immed(op2);
            if (fit32(imm)) {
                buf = alu(buf, cmp_opc, *src1, get_imm_opnd(imm, size_32), size_64);
            } else {
                // use temporary register
                Tmp_GR_Opnd src2_reg(context, ic);
                buf = mov(buf, src2_reg, get_imm_opnd(imm, size_64), size_64);
                if (src1->is_reg()) {
                    buf = alu(buf, cmp_opc, *(R_Opnd *)src1, src2_reg);
                } else {
                    buf = alu(buf, cmp_opc, *(M_Opnd *)src1, src2_reg);
                }
            }
        } else {
            // second operand is not an immediate value
            const LcgEM64TLoc * src2_loc = get_op_loc(op2, false);
            if (src1->is_reg()) {
                buf = alu(buf, cmp_opc, *(R_Opnd *)src1, get_rm_opnd(src2_loc));
            } else if (src2_loc->kind == LLK_Gr) {
                buf = alu(buf, cmp_opc, *(M_Opnd *)src1, get_r_opnd(src2_loc));
            } else {
                // src1 is in memory as well as src2
                const Tmp_GR_Opnd src2_reg(context, ic);
                buf = mov(buf, src2_reg, get_m_opnd(src2_loc), size_64);
                buf = alu(buf, cmp_opc, *((M_Opnd *)src1), src2_reg);
            }
        }
        delete tmp_reg;

        // the actual branch
        buf = branch32(buf, cc, get_imm_opnd((int64)0, size_32));
        labels.add_patch_to_label(label, buf - 4, LPT_Rel32);
    }

    void out(LilSig * sig) {
        // nothing else to do; space has been reserved already
    }

    /**
     * implements the copying of incoming to outgoing args
     */
    void in2out(LilSig * sig) {
        assert(!lil_sig_is_arbitrary(lil_cs_get_sig(cs)));

        for (unsigned i = 0; i < context.get_num_inputs(); i++) {
            const LcgEM64TLoc * in_loc = get_input(i);
            const LcgEM64TLoc * out_loc = get_output(i, sig);
            move_rm(out_loc, in_loc);
        }
    }

    void call(LilOperand * target, LilCallKind kind) {
        switch (kind) {
        case LCK_TailCall: {
            // restore input FR & GR
            unmove_inputs();
            // unwind current stack frame
            epilog();
            // jump (instead of calling)
            if (lil_operand_is_immed(target)) {
                // check if we can perform relative call
                int64 target_value = lil_operand_get_immed(target);
                /*

                // TODO: relative addressing isn't supported now
                // need to compute code size before emitting

                int64 offset = target_value - (int64)buf;
                // sub 5 bytes for this instruction
                if (fit32(offset)) {
                    // make a relative call
                    buf = jump32(buf, get_imm_opnd((int64)0, size_32));
                    // label name is equal to address
                    char * label = (char *)mem.alloc(MAX_DEC_SIZE);
                    assert(sizeof(long) == sizeof(POINTER_SIZE_INT));
                    sprintf(label, "%" FMT64 "d", target_value);
                    // offset should be patched when the stub is copied to other place
                    labels.define_label(label, (void *)(int64 *)target_value, false);
                    labels.add_patch_to_label(label, buf - 4, LPT_Rel32);
                } else {
                */
                    // make absolute jump
                    buf = mov(buf, rax_opnd, get_imm_opnd(target_value, size_64), size_64);
                    buf = ::jump(buf, rax_opnd, size_64);
                //}
            } else {
                const LcgEM64TLoc * loc = get_op_loc(target, false);
                buf = jump(buf, get_rm_opnd(loc), size_64);
            }
            break;
        }
        case LCK_Call:
        case LCK_CallNoRet: {
#ifdef _WIN64
            buf = alu(buf, add_opc, rsp_opnd, Imm_Opnd(-SHADOW), size_64);
#endif
            if (lil_operand_is_immed(target)) {
                // check if we can perform relative call
                int64 target_value = lil_operand_get_immed(target);

                /*

                // TODO: relative addressing isn't supported now
                // need to compute code size before emitting

                int64 offset = target_value - (int64)buf;
                // sub 5 bytes for this instruction
                if (fit32(offset)) {
                    // make a relative call
                    buf = ::call(buf, get_imm_opnd((int64)0, size_32));
                    // label name is equal to address
                    char * label = (char *)mem.alloc(MAX_DEC_SIZE);
                    assert(sizeof(long) == sizeof(POINTER_SIZE_INT));
                    sprintf(label, "%" FMT64 "d", target_value);
                    // offset should be patched when the stub is copied to other place
                    labels.define_label(label, (void *)(int64 *)target_value, false);
                    labels.add_patch_to_label(label, buf - 4, LPT_Rel32);
                } else {
                */
                    // make absolute call
                    buf = mov(buf, rax_opnd, get_imm_opnd(target_value, size_64), size_64);
                    buf = ::call(buf, rax_opnd, size_64);
                //}
            } else {
                const LcgEM64TLoc * loc = get_op_loc(target, false);
                buf = ::call(buf, get_rm_opnd(loc), size_64);
            }
#ifdef _WIN64
            buf = alu(buf, add_opc, rsp_opnd, Imm_Opnd(SHADOW), size_64);
#endif
            take_inputs_from_stack = true;
            break;
        }
        default:
            DIE(("Unknown kind"));
        }
    }

    void ret() {
        // unwind current stack frame
        epilog();
        buf = ::ret(buf);
    }

    // guarantee to keep inputs, locals & standard places 
    // input registers should be preserved outside if required
    void push_m2n(Method_Handle method, frame_type current_frame_type, bool handles) {
        take_inputs_from_stack = true;

        // rsp-relative offset of the top of the m2n frame
        I_32 offset = context.get_m2n_offset() + context.get_stk_m2n_size();

        buf = m2n_gen_push_m2n(buf, method, current_frame_type, handles,
            lil_cs_get_max_locals(cs), lil_ic_get_num_std_places(ic), offset);
    }

    void m2n_save_all() {
    }

    void pop_m2n() {
        buf = m2n_gen_pop_m2n(buf, context.m2n_has_handles(), lil_cs_get_max_locals(cs),
            // TODO: FIXME: need to define proper return registers to be preserved
            context.get_m2n_offset(), 1);
        // after m2n_gen_pop_m2n rsp points to the last callee-saves register
    }

    void print(char * str, LilOperand * o) {
        // this is a no-op if debugging is off
/*
#ifdef STUB_DEBUG
        unsigned print_reg;
        if (lil_operand_is_immed(o)) {
            // dummy operand; print r0
            print_reg = 0;
        } else {
            LilVariable *var = lil_operand_get_variable(o);
            const LcgEM64TLoc* var_loc =
                get_var_loc(var, inst, ic, false);
            assert(var_loc->kind == LLK_Gr);
            print_reg = var_loc->addr;
        }
        emit_print_reg(emitter, str, print_reg, context.get_num_inputs(),
                       context.get_first_output_gr(), false);
#endif  // STUB_DEBUG
*/
    }
};

LilCodeGeneratorEM64T::LilCodeGeneratorEM64T(): LilCodeGenerator() {}

NativeCodePtr LilCodeGeneratorEM64T::compile_main(LilCodeStub * cs, size_t * stub_size, PoolManager* code_pool) {
    // start a memory manager
    tl::MemoryPool m;
    // get context
    LcgEM64TContext * context = new(m) LcgEM64TContext(cs, m);
    // generate code
    LcgEM64TCodeGen codegen(cs, *context, m);
    // copy generated code to the destination
    *stub_size = codegen.get_size();
    NativeCodePtr buffer = allocate_memory(*stub_size, code_pool);
    codegen.copy_stub(buffer);

    return buffer;
}

GenericFunctionPointer lil_npc_to_fp(NativeCodePtr ncp) {
    return (GenericFunctionPointer)ncp;
}
