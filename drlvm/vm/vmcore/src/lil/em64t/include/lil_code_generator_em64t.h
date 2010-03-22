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

/**
 * Stack frame layout created by LIL CG on EM64T
 *  
 *    |--------------------------|
 *    | Extra inputs             |
 *    |--------------------------| <--- previouse stack frame bottom
 *    | Return ip                |
 *    |==========================| <--- current stack frame top
 *    | M2N frame | callee saved |
 *    |--------------------------|
 *    | GR inputs save area      |
 *    |--------------------------|
 *    | FR inputs save area      |
 *    |--------------------------|
 *    | Dynamicly allocated area |
 *    | (includes stack padding) |
 *    |--------------------------|
 *    | Extra outputs            |
 *    |==========================| <--- current stack frame bottom
 *
 * Note:
 *     EM64T architecture requires stack frame bottom address
 *     to be aligned on 16 byte boundary (rsp % 16 == 0)
 *
 * Register usage:
 *    r12-r15 are used for lil local variables (l0-l3)
 *    r10-r11 are used for lil standard places (sp0-sp1)
 */

#ifndef _LIL_CODE_GENERATOR_EM64T_
#define _LIL_CODE_GENERATOR_EM64T_

#include "lil.h"
#include "lil_code_generator.h"
#include "encoder.h"


/**
 * rounds up an integer value to the closest multiple of 8
 */
inline unsigned align_8(unsigned n) {
    return (n + 0x7) & ~0x7;
}

/**
 * rounds up an integer value to the closest multiple of 16
 */
inline unsigned align_16(unsigned n) {
    return (n + 0xF) & ~0xF;
}

/**
* an enum indicating a variable's location: in a register class or on the
* stack (no LIL variable is ever on the heap!)
*/
enum LcgEM64TLocKind {
    LLK_Gr, // 64-bit general purpose registers
    LLK_Fr, // 128-bit xmm registers
    LLK_GStk, // memory stack which holds gp value
    LLK_FStk // memory stack which holds fp value
};

class LcgEM64TContext: public LilInstructionVisitor {

public:
#ifdef _WIN64
    // maximum number of GR reserved for returns
    static const unsigned MAX_GR_RETURNS = 1;
    // maximum number of GR reserved for outputs/inputs
    static const unsigned MAX_GR_OUTPUTS = 4;    
    // maximum number of locals that can be placed in GR
    static const unsigned MAX_GR_LOCALS = 8;
    // maximum number of stand places
    static const unsigned MAX_STD_PLACES = 2;

    // maximum number of FR reserved for returns
    static const unsigned MAX_FR_RETURNS = 1;
    // maximum number of FR reserved for outputs/inputs
    static const unsigned MAX_FR_OUTPUTS = 4;
    // maximum number of temporary XMM registers
    static const unsigned MAX_FR_LOCALS = 10;
    // maximum number of temporary XMM registers
    static const unsigned MAX_FR_TEMPORARY = 2;
#else
    // maximum number of GR reserved for returns
    static const unsigned MAX_GR_RETURNS = 2;
    // maximum number of GR reserved for outputs/inputs
    static const unsigned MAX_GR_OUTPUTS = 6;    
    // maximum number of locals that can be placed in GR
    static const unsigned MAX_GR_LOCALS = 6;
    // maximum number of stand places
    static const unsigned MAX_STD_PLACES = 2;

    // maximum number of FR reserved for returns
    static const unsigned MAX_FR_RETURNS = 2;
    // maximum number of FR reserved for outputs/inputs
    static const unsigned MAX_FR_OUTPUTS = 8;
    // maximum number of temporary XMM registers
    static const unsigned MAX_FR_LOCALS = 8;
    // maximum number of temporary XMM registers
    static const unsigned MAX_FR_TEMPORARY = 0;
#endif

    // size of GR in bytes
    // TODO: Think about using GR_STACK_SIZE
    static const unsigned GR_SIZE = 8;
    // size of FR in bytes
    // TODO: Think about using FR_STACK_SIZE
    static const unsigned FR_SIZE = 8;

    // offsets for the REG_MAP array
    static const unsigned STD_PLACES_OFFSET = 0;
    static const unsigned GR_LOCALS_OFFSET = STD_PLACES_OFFSET + MAX_STD_PLACES;
    static const unsigned GR_OUTPUTS_OFFSET = GR_LOCALS_OFFSET + MAX_GR_LOCALS;
    static const unsigned GR_RETURNS_OFFSET = GR_OUTPUTS_OFFSET + MAX_GR_OUTPUTS;
    static const unsigned RSP_OFFSET = GR_RETURNS_OFFSET + MAX_GR_RETURNS;

    // offsets for the XMM_REG_MAP array
    static const unsigned FR_OUTPUTS_OFFSET = 0;
    static const unsigned FR_RETURNS_OFFSET = FR_OUTPUTS_OFFSET + MAX_FR_OUTPUTS;
    static const unsigned FR_TEMPORARY_OFFSET = FR_RETURNS_OFFSET + MAX_FR_RETURNS;
    static const unsigned FR_LOCALS_OFFSET = FR_TEMPORARY_OFFSET + MAX_FR_TEMPORARY;

private:

    LilCodeStub             * cs;   // the code stub
    tl::MemoryPool          & mem;  // a memory manager
    LilInstructionIterator  iter;   // instruction iterator

    unsigned n_inputs;     // total number of inputs
    unsigned n_gr_inputs;  // total number of GRs reserved for inputs
#ifdef _WIN64
    // Windows x64 has 4 slots for both integer and float inputs
#define n_fr_inputs n_gr_inputs
#else
    unsigned n_fr_inputs;  // total number of FRs reserved for inputs
#endif

    unsigned n_outputs;    // total number of outputs
    unsigned n_gr_outputs; // total number of GRs reserved for outputs
#ifdef _WIN64
    // Windows x64 has 4 slots for both integer and float inputs
#define n_fr_outputs n_gr_outputs
#else
    unsigned n_fr_outputs; // total number of FRs reserved for outputs
#endif

    /// Number of GR registers currently allocated for temporary needs.
    unsigned m_tmp_grs_used;
    /// Number of XMM registers currently allocated for temporary needs.
    unsigned m_tmp_xmms_used;

    unsigned stk_m2n_size;              // size reserved for the m2n frame
    unsigned stk_input_gr_save_size;    // size reserved for saving GR inputs
    unsigned stk_input_fr_save_size;    // size reserved for saving FR inputs
    unsigned stk_alloc_size;            // size of allocatable memory on the stack
    unsigned stk_output_size;           // bytes needed for outgoing params on the stack
    unsigned stk_size;                  // total size of the memory stack frame (in bytes)

    Method_Handle m2n_method;   // method handle of the m2n frame
    frame_type m2n_frame_type;  // m2n frame type 

    bool m2n_handles;       // true if m2n contains local handles
    bool does_normal_calls; // true if the stub contains "normal" calls
    bool does_tail_calls;   // true if the stub contains tail calls
    bool calls_unmanaged_code;  // true if the stub calls calls code with a calling convention other than managed
    bool has_m2n;           // true if the stub contains push_m2n/pop_m2n instructions
    bool save_inputs;       // true if inputs are accessed after a normal call

public:


    LcgEM64TContext(LilCodeStub * stub, tl::MemoryPool & m);

#ifdef _WIN64
     /**
      * returns general purpose register associated with given index
      * this association is used across whole lil code generator
      */
    static const R_Opnd & get_reg_from_map(unsigned index) {
        static const R_Opnd * REG_MAP[] = {
            // std places (scratched)
            &r10_opnd, &r11_opnd,
            // GR locals (calee-saved)
            &r12_opnd, &r13_opnd, &r14_opnd, &r15_opnd,
            &rdi_opnd, &rsi_opnd, &rbp_opnd, &rbx_opnd,
            // gr inputs/outputs (scratched)
            &rcx_opnd, &rdx_opnd, &r8_opnd, &r9_opnd,
            // gr returns (scratched)
            &rax_opnd,
            // rsp
            &rsp_opnd
        };
        return *REG_MAP[index];
    }

    /**
     * returns xmm register associated with given index
     * this association is used across whole lil code generator
     */
    static const XMM_Opnd & get_xmm_reg_from_map(unsigned index) {
        static const XMM_Opnd * XMM_REG_MAP[] = {
            // fr inputs/outputs (scratched)
            &xmm0_opnd, &xmm1_opnd, &xmm2_opnd, &xmm3_opnd,
            // fr returns (scratched)
            &xmm0_opnd,
            // temporary xmm registers (scratched)
            &xmm4_opnd, &xmm5_opnd,
            // locals xmm registers
            &xmm6_opnd, &xmm7_opnd, &xmm8_opnd, &xmm9_opnd,
            &xmm10_opnd, &xmm11_opnd, &xmm12_opnd, &xmm13_opnd,
            &xmm14_opnd, &xmm15_opnd
        };
        return *XMM_REG_MAP[index];
    }

    /**
     * an association between register number and index in the REG_MAP array
     */
    static unsigned get_index_in_map(const Reg_No reg) {
        static const unsigned INDEX_MAP[] = {
            //  rax_reg,                rbx_reg,            rcx_reg,
            GR_RETURNS_OFFSET, GR_LOCALS_OFFSET + 7, GR_OUTPUTS_OFFSET + 0,
            //  rdx_reg,                rdi_reg,            rsi_reg,
            GR_OUTPUTS_OFFSET + 1, GR_LOCALS_OFFSET + 4, GR_LOCALS_OFFSET + 5,
            //  rsp_reg,            rbp_reg,                r8_reg,
            RSP_OFFSET, GR_LOCALS_OFFSET + 6, GR_OUTPUTS_OFFSET + 2,
            //  r9_reg,                 r10_reg,            r11_reg,
            GR_OUTPUTS_OFFSET + 3, STD_PLACES_OFFSET, STD_PLACES_OFFSET + 1,
            //  r12_reg,                r13_reg,            r14_reg,
            GR_LOCALS_OFFSET, GR_LOCALS_OFFSET + 1, GR_LOCALS_OFFSET + 2,
            //  r15_reg,                xmm0_reg,           xmm1_reg,
            GR_LOCALS_OFFSET + 3, FR_OUTPUTS_OFFSET, FR_OUTPUTS_OFFSET + 1,
            //  xmm2_reg,               xmm3_reg,           xmm4_reg,
            FR_OUTPUTS_OFFSET + 2, FR_OUTPUTS_OFFSET + 3, FR_TEMPORARY_OFFSET,
            //  xmm5_reg,               xmm6_reg,           xmm7_reg,
            FR_TEMPORARY_OFFSET + 1, FR_LOCALS_OFFSET, FR_LOCALS_OFFSET + 1,
            //  xmm8_reg,               xmm9_reg,           xmm10_reg,
            FR_LOCALS_OFFSET + 2, FR_LOCALS_OFFSET + 3, FR_LOCALS_OFFSET + 4,
            //  xmm11_reg,              xmm12_reg,          xmm13_reg,
            FR_LOCALS_OFFSET + 5, FR_LOCALS_OFFSET + 6, FR_LOCALS_OFFSET + 7,
            //  xmm14_reg,              xmm15_reg
            FR_LOCALS_OFFSET + 8, FR_LOCALS_OFFSET + 9
        };
        return INDEX_MAP[reg];
    }
#else
    static const R_Opnd & get_reg_from_map(unsigned index) {
        static const R_Opnd * REG_MAP[] = {
            // std places (scratched)
            &r10_opnd, &r11_opnd,
            // GR locals (calee-saved)
            &r12_opnd, &r13_opnd, &r14_opnd, &r15_opnd, &rbp_opnd, &rbx_opnd,
            // gr inputs/outputs (scratched)
            &rdi_opnd, &rsi_opnd, &rdx_opnd, &rcx_opnd, &r8_opnd, &r9_opnd,
            // gr returns (scratched)
            &rax_opnd, &rdx_opnd,
            // rsp
            &rsp_opnd
        };
        return *REG_MAP[index];
    }

    /**
     * returns xmm register associated with given index
     * this association is used across whole lil code generator
     */
    static const XMM_Opnd & get_xmm_reg_from_map(unsigned index) {
        static const XMM_Opnd * XMM_REG_MAP[] = {
            // fr inputs/outputs (scratched)
            &xmm0_opnd, &xmm1_opnd, &xmm2_opnd, &xmm3_opnd,
            &xmm4_opnd, &xmm5_opnd, &xmm6_opnd, &xmm7_opnd,
            // fr returns (scratched)
            &xmm0_opnd, &xmm1_opnd,
            // temporary xmm registers (scratched)
            &xmm8_opnd, &xmm9_opnd, &xmm10_opnd, &xmm11_opnd,
            &xmm12_opnd, &xmm13_opnd, &xmm14_opnd, &xmm15_opnd
        };
        return *XMM_REG_MAP[index];
    }

    /**
     * an association between register number and index in the REG_MAP array
     */
    static unsigned get_index_in_map(const Reg_No reg) {
        static const unsigned INDEX_MAP[] = {
            //  rax_reg,                rbx_reg,            rcx_reg,
            GR_RETURNS_OFFSET, GR_LOCALS_OFFSET + 5, GR_OUTPUTS_OFFSET + 3,
            //  rdx_reg,                rdi_reg,            rsi_reg,
            GR_OUTPUTS_OFFSET + 2, GR_OUTPUTS_OFFSET, GR_OUTPUTS_OFFSET + 1,
            //  rsp_reg,            rbp_reg,                r8_reg,
            RSP_OFFSET, GR_LOCALS_OFFSET + 4, GR_OUTPUTS_OFFSET + 4,
            //  r9_reg,                 r10_reg,            r11_reg,
            GR_OUTPUTS_OFFSET + 5, STD_PLACES_OFFSET, STD_PLACES_OFFSET + 1,
            //  r12_reg,                r13_reg,            r14_reg,
            GR_LOCALS_OFFSET, GR_LOCALS_OFFSET + 1, GR_LOCALS_OFFSET + 2,
            //  r15_reg,                xmm0_reg,           xmm1_reg,
            GR_LOCALS_OFFSET + 3, FR_OUTPUTS_OFFSET, FR_OUTPUTS_OFFSET + 1,
            //  xmm2_reg,               xmm3_reg,           xmm4_reg,
            FR_OUTPUTS_OFFSET + 2, FR_OUTPUTS_OFFSET + 3, FR_OUTPUTS_OFFSET + 4,
            //  xmm5_reg,               xmm6_reg,   x       mm7_reg,
            FR_OUTPUTS_OFFSET + 5, FR_OUTPUTS_OFFSET + 6, FR_OUTPUTS_OFFSET + 7,
            //  xmm8_reg,               xmm9_reg,           xmm10_reg,
            FR_TEMPORARY_OFFSET, FR_TEMPORARY_OFFSET + 1, FR_TEMPORARY_OFFSET + 2,
            //  xmm11_reg,              xmm12_reg,          xmm13_reg,
            FR_TEMPORARY_OFFSET + 3, FR_TEMPORARY_OFFSET + 4, FR_TEMPORARY_OFFSET + 5,
            //  xmm14_reg,              xmm15_reg
            FR_TEMPORARY_OFFSET + 6, FR_TEMPORARY_OFFSET + 7
        };
        return INDEX_MAP[reg];
    }
#endif        
    void * operator new(size_t sz, tl::MemoryPool & m) {
        return m.alloc(sz);
    }

    void operator delete (void  * p, tl::MemoryPool & m) {}

    /**
     * returns the number of incoming arguments
     */
    unsigned get_num_inputs() const {
        return n_inputs;
    }

    /**
     * returns the number of incoming arguments stored in GRs
     */
    unsigned get_num_gr_inputs() const {
        assert(n_gr_inputs <= MAX_GR_OUTPUTS);
        return n_gr_inputs;
    }

    /**
     * Returns *reference* to the number of temporary GR registers currently allocated.
     */
    unsigned& get_tmp_grs_used(void) {
	return m_tmp_grs_used;
    }

    /**
     * Returns *reference* to the number of temporary XMM registers currently allocated.
     */
    unsigned& get_tmp_xmms_used(void) {
        return m_tmp_xmms_used;
    }
    
    /**
     * returns the number of incoming arguments stored in FRs
     */
    unsigned get_num_fr_inputs() const {
        assert(n_fr_inputs <= MAX_FR_OUTPUTS);
        return n_fr_inputs;
    }

    unsigned get_num_outputs() const {
        return n_outputs;
    }

    unsigned get_num_gr_outputs() const {
        assert(n_gr_outputs <= MAX_GR_OUTPUTS);
        return n_gr_outputs;
    }

    unsigned get_num_fr_outputs() const {
        assert(n_fr_outputs <= MAX_FR_OUTPUTS);
        return n_fr_outputs;
    }

    /**
     * returns true if m2n is required on the activation frame
     */
    bool has_m2n_frame() const {
        return has_m2n;
    }

    /**
     * returns true if we need to reserve space on the stack to save inputs
     */
    bool must_save_inputs() const {
        return save_inputs;
    }

    /**
     * true if type represents floating point value
     */
    bool is_fp_type(LilType t) const {
        return t == LT_F4 || t == LT_F8;
    }

    /**
     * method which corresponds to the m2n frame
     */
    Method_Handle get_m2n_method() const {
        return m2n_method;
    }

    /**
     * m2n frame type
     */
    frame_type get_m2n_frame_type() const {
        return m2n_frame_type;
    }

    /**
     * returns true if m2n contains local handles
     */
    bool m2n_has_handles() const {
        return m2n_handles;
    }

    // returns the offset of the start of the m2n frame
    unsigned get_m2n_offset() const {
        return get_input_gr_save_offset() + stk_input_gr_save_size;
    }

    // returns the offset of the start of the gr input register save space
    unsigned get_input_gr_save_offset() const {
        return get_input_fr_save_offset() + stk_input_fr_save_size;
    }

    // returns the offset of the start of the fr input register save space
    unsigned get_input_fr_save_offset() const {
        return get_alloc_start_offset() + stk_alloc_size;
    }

    // returns the offset of the first "allocatable" byte
    unsigned get_alloc_start_offset() const {
        return get_output_offset() + stk_output_size;
    }

    // returns the offset of the start of the m2n frame
    unsigned get_output_offset() const {
        return 0;
    }

    // size reserved for saving GR inputs
    unsigned get_stk_input_gr_save_size() const {
        return stk_input_gr_save_size;
    }

    // size reserved for saving FR inputs
    unsigned get_stk_input_fr_save_size() const {
        return stk_input_fr_save_size;
    }

    // size of allocatable memory on the stack
    unsigned get_stk_alloc_size() const {
        return stk_alloc_size;
    }

    // size reserved for the m2n frame
    unsigned get_stk_m2n_size() const {
        return stk_m2n_size;
    }

    // bytes needed for outgoing params on the stack
    unsigned get_stk_output_size() const {
        return stk_output_size;
    }

    // returns the size of the stack frame
    unsigned get_stk_size() const {
        return stk_size;
    }

private:

    /*    Helper functions, used by visitor functions    */

    // gather info from variable
    void check_variable(LilVariable * var, bool lvalue) {
        switch (lil_variable_get_kind(var)) {
            case LVK_In:
                // it's illegal to redefine inputs
                assert(!lvalue);
                // arbitrary stubs should not access inputs
                assert(!lil_sig_is_arbitrary(lil_cs_get_sig(cs)));
                // check if we use inputs after normal call
                if (does_normal_calls) {
                    save_inputs = true;
                }
                break;
            case LVK_Out:
                if (lvalue) {
                    save_inputs = true;
                }
                break;
            default:;
        }
    }

    // gather info from operand
    void check_operand(LilOperand * o, bool lvalue) {
        if (o != NULL && !lil_operand_is_immed(o)) {
            check_variable(lil_operand_get_variable(o), lvalue);
        }
    }



    //**************************
    // visitor functions

    void label(LilLabel label) {
        // nothing to do here
    }

    void locals(unsigned num) {
        assert(num <= MAX_GR_LOCALS);
    }

    void std_places(unsigned num) {
        assert(num <= MAX_STD_PLACES);
    }

    void alloc(LilVariable * var, unsigned alloc_space) {
        stk_alloc_size += align_8(alloc_space);
    }

    void asgn(LilVariable * var, LilOperation operation, LilOperand * op1, LilOperand * op2) {
        check_variable(var, true);
        check_operand(op1, false);
        if (lil_operation_is_binary(operation)) {
            check_operand(op2, false);
        }
    }

    void ts(LilVariable * var) {
        does_normal_calls = true;
        check_variable(var, true);
    }


    void handles(LilOperand * op) {
        check_operand(op, true);
    }

    void ld(LilType t, LilVariable * dst, LilVariable * base, unsigned scale,
            LilVariable * index, POINTER_SIZE_SINT offset, LilAcqRel, LilLdX) {
        check_variable(dst, true);
        if (base != NULL) {
            check_variable(base, false);
        }
        if (index != NULL) {
            check_variable(index, false);
        }
    }

    void st(LilType t, LilVariable * base, unsigned scale, LilVariable * index,
            POINTER_SIZE_SINT offset, LilAcqRel, LilOperand * src) {
        if (base != NULL) {
            check_variable(base, false);
        }
        if (index != NULL) {
            check_variable(index, false);
        }
        check_operand(src, false);
    }

    void inc(LilType t, LilVariable * base, unsigned scale, LilVariable * index,
             POINTER_SIZE_SINT offset, LilAcqRel) {
        if (base != NULL) {
            check_variable(base, false);
        }
        if (index != NULL) {
            check_variable(index, false);
        }
    }

    void cas(LilType t, LilVariable * base, unsigned scale, LilVariable * index,
             POINTER_SIZE_SINT offset, LilAcqRel, LilOperand * cmp, LilOperand * src, LilLabel) {
        if (base != NULL) {
            check_variable(base, false);
        }
        if (index != NULL) {
            check_variable(index, false);
        }
        check_operand(cmp, false);
        check_operand(src, false);
    }

    void j(LilLabel) {
        // nothing to do
    }

    void jc(LilPredicate p, LilOperand* o1, LilOperand* o2, LilLabel) {
        check_operand(o1, false);
        if (lil_predicate_is_binary(p)) {
            check_operand(o2, false);
        }
    }

    void out(LilSig* sig) {
        // make sure there is enough space for this command's outputs
        unsigned outs_cnt = lil_sig_get_num_args(sig);
        n_outputs = outs_cnt > n_outputs ? outs_cnt : n_outputs;

        // reserve enough GR & FR outputs
        unsigned gp_out_cnt = 0;
#ifdef _WIN64
#       define fp_out_cnt gp_out_cnt
#else
        unsigned fp_out_cnt = 0;
#endif
        for (unsigned i = 0;  i < lil_sig_get_num_args(sig);  i++) {
            LilType t = lil_sig_get_arg_type(sig, i);
            if (is_fp_type(t)) {
                fp_out_cnt++;
            } else {
                gp_out_cnt++;
            }
        }

        if (n_gr_outputs < gp_out_cnt) {
            if (gp_out_cnt <= MAX_GR_OUTPUTS) {
                n_gr_outputs = gp_out_cnt;
            } else {
                n_gr_outputs = MAX_GR_OUTPUTS;
                stk_output_size += (gp_out_cnt - n_gr_outputs) * GR_SIZE;
            }
        }

        if (n_fr_outputs < fp_out_cnt) {
            if (fp_out_cnt <= MAX_FR_OUTPUTS) {
                n_fr_outputs = fp_out_cnt;
            } else {
                n_fr_outputs = MAX_FR_OUTPUTS;
                stk_output_size += (fp_out_cnt - n_fr_outputs) * FR_SIZE;
            }
        }
    }

    void in2out(LilSig * sig) {
        assert(!lil_sig_is_arbitrary(lil_cs_get_sig(cs)));
        // check if we need to save inputs
        if (does_normal_calls) {
            save_inputs = true;
        }
        out(sig);
    }

    void call(LilOperand* o, LilCallKind k) {
        check_operand(o, false);
        if (k == LCK_Call) {
            does_normal_calls = true;
        } else if (k == LCK_TailCall) {
            // no need to reserve extra outputs, like in in2out
            // since tailcall is implemented differently
            does_tail_calls = true;
        }
    }

    void ret() {
        // nothing to do
    }

    void push_m2n(Method_Handle method, frame_type current_frame_type, bool handles) {
        m2n_method = method;
        m2n_frame_type = current_frame_type;
        m2n_handles = handles;
        has_m2n = true;  // remember that this stub requires an m2n frame
        does_normal_calls = true;
    }

    void m2n_save_all() {
    }

    void pop_m2n() {
        bool handles = lil_ic_get_m2n_state(iter.get_context()) == LMS_Handles;
        if (handles) {
            // it will execute a call
            does_normal_calls = true;
        }
    }

    void print(char *, LilOperand *) {
    }
};

/**
* keeps location of a LIL variable
*/
class LcgEM64TLoc {

public:
    LcgEM64TLocKind kind;
    int64 addr;  // register number or SP-relative offset

    LcgEM64TLoc(LcgEM64TLocKind k, int64 a): kind(k), addr(a) {}

    bool operator==(const LcgEM64TLoc & loc) const {
        return (kind == loc.kind && addr == loc.addr);
    }
    
    bool operator!=(const LcgEM64TLoc & loc)  {
        return (kind != loc.kind || addr != loc.addr);
    }

    void * operator new(size_t sz, tl::MemoryPool & m) {
        return m.alloc(sz);
    }
    
    void operator delete (void * p, tl::MemoryPool & m) {}
    
private:
    LcgEM64TLoc(LcgEM64TLoc &);  // disable copying
    LcgEM64TLoc & operator=(LcgEM64TLoc &); // disable copying
};

class LilCodeGeneratorEM64T : public LilCodeGenerator {

 public:
    LilCodeGeneratorEM64T();

 protected:
    NativeCodePtr compile_main(LilCodeStub* , size_t*, PoolManager*);
};

#endif // _LIL_CODE_GENERATOR_EM64T_
