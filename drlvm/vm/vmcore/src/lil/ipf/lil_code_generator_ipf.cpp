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

#include <assert.h>
#include <stdio.h>

#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include "lil.h"
#include "lil_code_generator.h"
#include "lil_code_generator_ipf.h"
#include "lil_code_generator_utils.h"
#include "m2n.h"
#include "m2n_ipf_internal.h"
#include "tl/memory_pool.h"
#include "vm_ipf.h"
#include "vm_threads.h"
#include "stub_code_utils.h"
#include "open/vm_util.h"
#include "environment.h"
#include "port_malloc.h"

#ifndef NDEBUG
#include "dump.h"
#endif
/************************
 * Where everything is
 * (up = higher addresses / reg numbers)
 * Stacked registers:
 *
 *      |----------------------------|
 *      | outputs                    |
 *      |----------------------------|
 *      | ar.pfs, b0, sp (if needed) |
 *      |----------------------------|
 *      | locals                     |
 *      |----------------------------|
 *      | m2n frame (if needed)      |
 *      |----------------------------|
 *      | inputs (8 spaces)          |
 * r32  |----------------------------|
 * 
 * Other registers:
 *   - standard places go into r17-r18
 *   - return values go into r8 or f8
 *   - fp arguments go into f8-f15
 *   - fp locals go into scratch registers if the stub makes no ordinary calls, otherwise onto the memory stack
 *   - r3 is used for add-long-immediate
 *   - r26-31 are used as temporaries in arithmetic operations
 *
 * Memory stack frame:
 *
 *              |------------------------|
 *              | extra inputs           |
 * prev sp+16   |========================|
 *              | Local FR area          |
 *              |------------------------|
 *              | FR input save area     |
 *              |------------------------|
 *              | dyn alloc              |
 *              |------------------------|
 *              | extra saves area       |
 *              |------------------------|
 *              | extra outputs          |
 *       sp+16  |------------------------|
 *              | scratch                |
 *           sp |========================|
 *
 * All pieces of the stack are 16-byte aligned!
 *
 * If the stub executes calls other than tailcall and call.noret,
 * inputs residing in FRs are moved to memory, to avoid being overwritten
 * by FR outputs and return values and/or being used as scratch in the
 * callee.  In addition local floating point variables are put on the stack
 * rather than in scratch registers.
 *
 * Things that are saved right after entry / restored right before ret:
 *   - ar.pfs (by the alloc instruction), in the stacked regs
 *   - b0  (only if the stub executes calls), in the stacked regs
 *
 * Things that are saved right before a call and restored right after a call
 * (except for call.noret):
 *   - sp
 *   - gp, if needed
 */


// maximum number of locals.  Increase it if needed, but not more than 80 or so.
#define MAX_LOCALS 10

// maximum number of stand places.  Probably upper limit here is around 16.
#define MAX_STD_PLACES 2

// rounds up an integer value to the closest multiple of 16
static inline unsigned align_16(unsigned n) {
    return n = (n + 0xF) & ~0xF;
}



// an enum indicating a variable's location: in a register class or on the
// stack (no LIL variable is ever on the heap!)
enum LcgIpfLocKind {
    LLK_Gr, LLK_Fr, LLK_Stk, LLK_Stk16
};


// location of a LIL variable
struct LcgIpfLoc {
    void *operator new(size_t sz, tl::MemoryPool& m) {
        return m.alloc(sz);
    }
    void operator delete (void *p, tl::MemoryPool& m) { }

    LcgIpfLocKind kind;
    int addr;  // register number or SP-relative offset

    LcgIpfLoc(LcgIpfLocKind k, int a): kind(k), addr(a)
    {}

    bool operator==(const LcgIpfLoc& loc) {
        return (kind == loc.kind && addr == loc.addr);
    }

    bool operator!=(const LcgIpfLoc& loc) {
        return (kind != loc.kind || addr != loc.addr);
    }

private:
    LcgIpfLoc(LcgIpfLoc &);  // disable copying
    LcgIpfLoc &operator=(LcgIpfLoc &); // disable copying
};  // struct LcgIpfLoc


// This class holds all relevant information needed in compilation,
// such as the mapping of variables to locations and the mapping of
// labels to ints.  Some ofthis information is gathered during a prepass.
class LcgIpfContext: public LilInstructionVisitor {
private:

    LilCodeStub *cs; // the code stub
    LilSig *entry_sig;  // the stub's entry signature
    bool is_arbitrary;  // true if the stub's entry signature is "arbitrary"
    tl::MemoryPool &mem; // a memory manager

    unsigned n_labels;  // number of labels
    LilLabel *labels;   // a label's index is its label id

    // number of targets used by support functions, such as pop_m2n
    unsigned extra_targets;


    unsigned n_inputs;  // total number of inputs
    unsigned n_gr_inputs;  // total number of GRs reserved for inputs
    unsigned n_fr_inputs;  // number of inputs placed in FRs

    unsigned first_local;  // location of the first local variable
    unsigned n_locals;  // maximum number of locals
    unsigned n_fr_locals;  // maximum number of locals that may contain FP values
    bool     uses_local_fr[MAX_LOCALS];  // true if preserved fp f(16+index) is used by a local
    unsigned local_fr_offset[MAX_LOCALS]; // offsets for local variable from start of local fr area

    unsigned first_output; // location of the start of the output space
    unsigned n_outputs;    // maximum number of outputs
    unsigned n_gr_outputs;  // total number of GRs reserved for outputs
    unsigned n_fr_outputs;  // maximum number of outputs placed in FRs

    unsigned first_gr_save;  // start of region where ar.pfs, b0, etc. are saved
    unsigned n_gr_saves;  // number of GRs reserved for saving stuff

    unsigned stk_output_size;  // bytes needed for outgoing params on the stack
    unsigned stk_input_save_size;    // size reserved for saving FR inputs
    unsigned stk_local_fr_size;  // size reserved for local FRs
    unsigned stk_extra_saves_size;  // bytes for m2n_save_all

    unsigned n_std_places;  // number of standard places
    unsigned stk_alloc_size;    // size of allocatable memory on the stack

    // total size of the memory stack frame (in bytes)
    unsigned stk_size;

    bool does_normal_calls;  // true if the stub contains "normal" calls
    bool does_tail_calls;    // true if the stub contains tail calls
    bool calls_unmanaged_code;  // true if the stub calls calls code with a calling convention other than managed
    bool has_m2n;            // true if the stub contains push_m2n/pop_m2n instructions
    bool uses_inputs;   // true if inputs are ever accessed
    bool needs_alloc;   // true if an alloc instruction is needed at the start

    // an iterator used during pre-pass; it has to be stored here because
    // it must be accessible from the visitor functions
    LilInstructionIterator iter;

public:

    void *operator new(size_t sz, tl::MemoryPool& m) {
        return m.alloc(sz);
    }
        void operator delete (void *p, tl::MemoryPool& m) { }
 
    LcgIpfContext(LilCodeStub *cs, tl::MemoryPool &m):
        cs(cs),
        mem(m),
        iter(cs, true)
    {
        // initialize some members
        entry_sig = lil_cs_get_sig(cs);
        is_arbitrary = lil_sig_is_arbitrary(entry_sig);
        n_labels = 0;
        extra_targets = 0;
        labels = (LilLabel *) mem.alloc(lil_cs_get_num_instructions(cs) * sizeof (LilLabel));

        n_locals = 0;

        for (unsigned i=0;  i < MAX_LOCALS;  i++) {
            uses_local_fr[i] = false;
        }

        n_outputs = 0;
        n_fr_outputs = 0;

        n_std_places = 0;

        stk_extra_saves_size = 0;
        stk_alloc_size = 0;
        does_normal_calls = false;
        calls_unmanaged_code = false;
        does_tail_calls = false;
        has_m2n = false;
        uses_inputs = false;

        // get some info from the entry signature
        if (is_arbitrary) {
            n_inputs = 8;
            n_gr_inputs = 8;
            n_fr_inputs = 8;
        }
        else {
            n_inputs = lil_sig_get_num_args(entry_sig);
            n_gr_inputs = (n_inputs > 8) ? 8 : n_inputs;
            n_fr_inputs = 0;
            for (unsigned i=0; i<n_inputs; i++) {
                LilType t = lil_sig_get_arg_type(entry_sig, i);
                if (t == LT_F4 || t == LT_F8)
                    n_fr_inputs++;
            }
        }
 
        // scan the code stub for more information
        while (!iter.at_end()) {
            check_locals(iter.get_context());
            lil_visit_instruction(iter.get_current(), this);
            iter.goto_next();
        }

        // decide if an alloc instruction is needed
        needs_alloc = (has_m2n || does_normal_calls || n_outputs > 0 ||  n_locals > 0);

        // determine how many save spots are needed in stacked GRs
        // ( if an M2N frame is present, registers will be saved in there)
        if (has_m2n || !needs_alloc)
            n_gr_saves = 0;
        else if (does_normal_calls)
            n_gr_saves = 4;  // save ar.pfs, b0, gp, sp
        else
            n_gr_saves = 1;  // save ar.pfs only


        // decide where stacked register regions should start
        if (has_m2n)
            first_local = m2n_get_last_m2n_reg() + 1;
        else
            first_local = 32 + n_gr_inputs;

        first_gr_save = first_local + n_locals;
        first_output = first_gr_save + n_gr_saves;

        // stack size needed for outputs (16-byte aligned)
        if (n_outputs <= 8) {
            n_gr_outputs = n_outputs;
            stk_output_size = 0;
        }
        else {
            n_gr_outputs = 8;
            stk_output_size = align_16((n_outputs-8)*8);
        }

        // 16-byte align allocatable space
        stk_alloc_size = align_16(stk_alloc_size);

        // stack size needed for saving FR inputs
        if (does_normal_calls)
            stk_input_save_size = n_fr_inputs * 16;
        else
            stk_input_save_size = 0;

        // stack size needed for local FRs
        stk_local_fr_size = 0;
        if (does_normal_calls) {
            for(unsigned i=0; i<MAX_LOCALS; i++) {
                local_fr_offset[i] = stk_local_fr_size;
                stk_local_fr_size += 16;
            }
        }

        // determine the size of the stack frame
        stk_size =
            stk_local_fr_size + // local FR space
            stk_input_save_size +    // FR input spill space
            stk_alloc_size +   // memory for dynamic allocation
            stk_extra_saves_size + // memory for m2n_save_all
            stk_output_size;   // outputs on the stack
    }  // LcgIpfContext (constructor)

    // returns the code stub's entry signature
    LilSig *get_entry_sig() {
        return entry_sig;
    }  // get_entry_sig

    // true if the entry sig is arbitrary
    bool entry_sig_is_arbitrary() {
        return is_arbitrary;
    } // entry_sig_is_arbitrary

    // returns the location of the first input GR
    unsigned get_first_input_gr() {
        return 32;
    }  // get_first_input_gr

    // returns the location of the first output GR
    unsigned get_first_output_gr() {
        return first_output;
    }  // get_first_output_gr

    // returns the number of targets used up by this stub, including
    // targets used by labels and targets used by auxiliary functions, such as pop_m2n
    unsigned get_num_targets() {
        return n_labels + extra_targets;
    }  // get_num_labels

    // returns the id of a label
    unsigned get_label_id(LilLabel label) {
        for (unsigned i=0; i<n_labels; i++) {
            if (labels[i] && strcmp(label, labels[i])==0)
                return i;
        }

        DIE(("Label not found"));
        return 0;
    }  // get_label_id

    // returns an unused target id
    // (for use by auxiliary functions, not intended for LIL labels!)
    unsigned get_unused_target() {
        assert(extra_targets > 0);  // haven't run out of targets yet!
        --extra_targets;
        return n_labels + extra_targets;
    }

    // returns the number of incoming arguments
    unsigned get_num_inputs() {
        return n_inputs;
    }  // get_num_input

    // returns the number of incoming arguments stored in FRs
    unsigned get_num_fr_inputs() {
        return n_fr_inputs;
    }  // get_num_fr_inputs

    // returns the size of the input space in the stacked GRs
    unsigned get_input_space_size() {
        return n_gr_inputs;
    }  // get_input_space_size

    // returns the size of the local space in the stacked GRs
    // (this includes local vars, the M2N frame, and the save regs)
    unsigned get_local_space_size() {
        return first_output - (32 + n_gr_inputs);
    }  // get_local_space_size

    // returns the size of the output space in the stacked GRs
    unsigned get_output_space_size() {
        return n_gr_outputs;
    }  // get_output_space_size

    // returns true if there are floating-point inputs in registers f8-f15 that must be moved to registers f16-f23
    bool must_move_fr_inputs() {
        return (does_normal_calls && n_fr_inputs > 0);
    }  // must_move_fr_inputs

    // returns true if ar.ccv should be set to zero before a call to managed
    // code.  This is the case if the stub or any function it calls is
    // non-managed (i.e. it has a platform, jni, or stdcall cc)
    bool must_reset_ccv() {
        LilCc stub_cc = lil_sig_get_cc(entry_sig);
        return !(stub_cc==LCC_Managed || stub_cc==LCC_Rth) || calls_unmanaged_code;
    }  // must_reset_ccv

    // returns the location of the i'th input
    const LcgIpfLoc* get_input(unsigned n) {
        // function should not be called if signature is arbitrary
        assert(!is_arbitrary);

        LilType tp = lil_sig_get_arg_type(entry_sig, n);

        // if n >= 8, input is on stack
        if (n >= 8) {
            return new(mem) LcgIpfLoc(LLK_Stk, 16 + stk_size + (n-8)*8);
        }

        if (tp != LT_F4 && tp != LT_F8) {
            return new(mem) LcgIpfLoc(LLK_Gr, 32+n);
        }

        // parameter is fp; its location depends on how many fp parameters
        // come before it.
        unsigned fp_param_cnt = 0;
        for (unsigned i=0; i<n; i++) {
            LilType t = lil_sig_get_arg_type(entry_sig, i);
            if (t == LT_F4 || t == LT_F8)
                fp_param_cnt++;
        }

        if (must_move_fr_inputs()) {
            // inputs have been moved to memory
            return new(mem) LcgIpfLoc(LLK_Stk16, get_input_save_offset() + 16 * fp_param_cnt);
        }
        else {
            return new(mem) LcgIpfLoc(LLK_Fr, 8+fp_param_cnt);
        }
    }  // get_input


    // returns the location of the i'th output
    const LcgIpfLoc* get_output(unsigned n, LilSig *out_sig) {
        LilType tp = lil_sig_get_arg_type(out_sig, n);

        // if n >= 8, output is in stack
        if (n >= 8) {
            return new(mem) LcgIpfLoc(LLK_Stk, 16 + (n-8)*8);
        }

        if (tp != LT_F4 && tp != LT_F8) {
            return new(mem) LcgIpfLoc(LLK_Gr, first_output + n);
        }

        // parameter is fp; its location depends on how many fp parameters
        // come before it.
        unsigned fp_param_cnt = 0;
        for (unsigned i=0; i<n; i++) {
            LilType t = lil_sig_get_arg_type(out_sig, i);
            if (t == LT_F4 || t == LT_F8)
                fp_param_cnt++;
        }
        return new(mem) LcgIpfLoc(LLK_Fr, 8+fp_param_cnt);
    }  // get_output

    // returns the location of the i'th int local
    const LcgIpfLoc* get_gr_local(unsigned n) {
        return new(mem) LcgIpfLoc(LLK_Gr, first_local+n);
    }  // get_gr_local

    // returns the location of the i'th fp local
    const LcgIpfLoc* get_fr_local(unsigned n)
    {
        if (does_normal_calls)
            return new(mem) LcgIpfLoc(LLK_Stk16, get_local_fr_offset()+local_fr_offset[n]);
        else
            return new(mem) LcgIpfLoc(LLK_Fr, (n<8 ? 9+n : 32+n));
    }  // get_fr_local

    // returns the GR number of the i'th standard place
    unsigned get_std_place(unsigned n) {
        return 17+n;
    }  // get_std_place

    // returns the location of a LIL variable
    // (input, output, local, std place, or return value)
    // inst: the current instruction
    // ic: the current instruction context
    // is_lval: true if the variable is used as an l-value
    const LcgIpfLoc* get_var_loc(LilVariable *var,
                                 LilInstruction *inst,
                                 LilInstructionContext *ic,
                                 bool is_lval) {
        switch (lil_variable_get_kind(var)) {
        case LVK_In:
            return get_input(lil_variable_get_index(var));
        case LVK_StdPlace:
            return new(mem) LcgIpfLoc(LLK_Gr, get_std_place(lil_variable_get_index(var)));
        case LVK_Out:
        {
            LilSig *out_sig = lil_ic_get_out_sig(ic);
            assert(out_sig != NULL);
            return get_output(lil_variable_get_index(var), out_sig);
        }
        case LVK_Local:
        {
            unsigned index = lil_variable_get_index(var);
            LilType t;
            if (is_lval)
                t = lil_instruction_get_dest_type(cs, inst, ic);
            else
                t = lil_ic_get_local_type(ic, index);
            if (t == LT_F4 || t == LT_F8)
                return get_fr_local(lil_variable_get_index(var));
            else
                return get_gr_local(lil_variable_get_index(var));
        }
        case LVK_Ret:
        {
            LilType t;
            if (is_lval)
                t = lil_instruction_get_dest_type(cs, inst, ic);
            else
                t = lil_ic_get_ret_type(ic);
            if (t == LT_F4 || t == LT_F8)
                return new(mem) LcgIpfLoc(LLK_Fr, 8);
            return new(mem) LcgIpfLoc(LLK_Gr, 8);
        }

        default:
            DIE(("Unklown variable kind"));
            return new(mem) LcgIpfLoc(LLK_Gr, 0);  // should never be reached
        }
    }  // get_var_loc


    // returns the GR number where the result of the alloc instruction
    // (old ar.pfs) should be saved
    const LcgIpfLoc* get_pfs_save_gr() {
        if (!needs_alloc)
            return NULL;  // doesn't need saving
        if (has_m2n)
            return new(mem) LcgIpfLoc(LLK_Stk, m2n_get_pfs_save_reg());
        return new(mem) LcgIpfLoc(LLK_Stk, first_gr_save);
    }  // get_pfs_save_gr

    // returns the GR number where b0 should be saved
    // (0 if b0 does not need saving)
    const LcgIpfLoc* get_return_save_gr() {
        if (!has_m2n && !does_normal_calls)
            return NULL;  // b0 does not need saving

        if (has_m2n)
            return new(mem) LcgIpfLoc(LLK_Gr, m2n_get_return_save_reg());
        else
            return new(mem) LcgIpfLoc(LLK_Gr, first_gr_save + 1);
    }  // get_return_save_gr

    // returns the GR number where gp should be saved
    const LcgIpfLoc* get_gp_save_gr() {
        if (has_m2n)
            return new(mem) LcgIpfLoc(LLK_Gr, m2n_get_gp_save_reg());
        else if (!does_normal_calls)
            return NULL;  // no need to save gp
        return new(mem) LcgIpfLoc(LLK_Gr, first_gr_save + 2);
    }  // get_gp_save_gr

    // returns the size of the stack frame
    unsigned get_stk_size() {
        return stk_size;
    }  // get_stk_size

    // returns the sp-relative offset of the start of the input register save space
    unsigned get_input_save_offset() {
        return 16 + stk_output_size + stk_extra_saves_size + stk_alloc_size;
    }  // get_input_save_offset

    // returns the sp-relative offset of the start of the local FR space
    unsigned get_local_fr_offset() {
        return 16 + stk_output_size + stk_extra_saves_size + stk_alloc_size + stk_input_save_size;
    }  // get_input_save_offset

    // returns the sp-relative offset of the first "allocatable" byte
    unsigned get_alloc_start_offset() {
        return 16 + stk_output_size + stk_extra_saves_size;
    }  // get_alloc_start_offset

    unsigned get_extra_saves_start_offset() {
        return 16 + stk_output_size;
    }

    bool has_push_m2n() {
        return has_m2n;
    }

private:
    //*****************
    // helper functions, used by visitor functions

    // gather info from variable
    void check_variable(LilVariable *var) {
        if (lil_variable_get_kind(var) == LVK_In)
            uses_inputs = true;
    }  // check_variable

    // gather info from operand
    void check_operand(LilOperand *o) {
        if (o != NULL && !lil_operand_is_immed(o))
            check_variable(lil_operand_get_variable(o));
    }  // check_operand

    // check a LIL context for local usage
    void check_locals(LilInstructionContext *ic) {
        unsigned nl = lil_ic_get_num_locals(ic);
        assert(nl <= MAX_LOCALS);
        if (n_locals < nl)
            n_locals = nl;
        for (unsigned i=0;  i < nl;  i++) {
            LilType t = lil_ic_get_local_type(ic, i);
            if (t == LT_F4 || t == LT_F8) {
                uses_local_fr[i] = true;
            }
        }
    }  // check_locals


    //**************************
    // visitor functions

    void label(LilLabel label) {
        labels[n_labels++] = label;
    }  // label

    void locals(unsigned l) {
        // nothing to do; locals are taken care of in check_locals()
    }  // locals

    void std_places(unsigned sp) {
        assert(sp<=MAX_STD_PLACES);
        if (sp > n_std_places)
            n_std_places = sp;
    }  // std_places

    void alloc(LilVariable* var, unsigned alloc_space) {
        alloc_space = (alloc_space + 0x7) & ~0x7;
        stk_alloc_size += alloc_space;
    }  // alloc

    void asgn(LilVariable* var, enum LilOperation operation, LilOperand* op1, LilOperand* op2) {
        check_variable(var);
        check_operand(op1);
        if (lil_operation_is_binary(operation))
            check_operand(op2);
    }  // asgn

    void ts(LilVariable*var) {
        check_variable(var);
    }  // ts

    void handles(LilOperand* op) {
        check_operand(op);
    }  // handles

    void ld(LilType t, LilVariable* dst, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel, LilLdX) {
        check_variable(dst);
        if (base != NULL)
            check_variable(base);
        if (index != NULL)
            check_variable(index);
    }  // ld

    void st(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel, LilOperand* src) {
        if (base != NULL)
            check_variable(base);
        if (index != NULL)
            check_variable(index);
        check_operand(src);
    }  // st

    void inc(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel) {
        if (base != NULL)
            check_variable(base);
        if (index != NULL)
            check_variable(index);
    }  // inc

    void cas(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel,
             LilOperand* cmp, LilOperand* src, LilLabel)
    {
        if (base) check_variable(base);
        if (index) check_variable(index);
        check_operand(cmp);
        check_operand(src);
    }

    void j(LilLabel) {
        // nothing to do
    }  // j

    void jc(LilPredicate p, LilOperand* o1, LilOperand* o2, LilLabel)
    {
        check_operand(o1);
        if (lil_predicate_is_binary(p))
            check_operand(o2);
    }  // jc

    void out(LilSig* sig) {
        // make sure there is enough space for this command's outputs
        unsigned outs = lil_sig_get_num_args(sig);
        if (outs > n_outputs)
            n_outputs = outs;

        // reserve enough FR outputs
        unsigned n_fo = 0;
        for (unsigned i=0;  i<lil_sig_get_num_args(sig);  i++) {
            LilType t = lil_sig_get_arg_type(sig, i);
            if (t == LT_F4 || t == LT_F8)
                n_fo ++;
        }
        if (n_fo > n_fr_outputs)
            n_fr_outputs = n_fo;

        // check if this refers to a call to unmanaged code
        LilCc cc = lil_sig_get_cc(sig);
        if (cc != LCC_Managed && cc != LCC_Rth)
            calls_unmanaged_code = true;
    }  // out

    void in2out(LilSig* sig) {
        // reserve enough outputs and FR outputs
        if (n_inputs > n_outputs)
            n_outputs = n_inputs;
        if (n_fr_inputs > n_fr_outputs)
            n_fr_outputs = n_fr_inputs;
        // in2out implies that inputs are being accessed
        uses_inputs = true;

        // check if this refers to a call to unmanaged code
        LilCc cc =lil_sig_get_cc(sig);
        if (cc != LCC_Managed && cc != LCC_Rth)
            calls_unmanaged_code = true;

    }  // in2out

    void call(LilOperand* o, LilCallKind k) {
        check_operand(o);
        if (k == LCK_Call)
            does_normal_calls = true;
        else if (k == LCK_TailCall) {
            does_tail_calls = true;
            // no need to reserve extra outputs, like in in2out, since tailcall is implemented differently
        }
    }  // call

    void ret() {
        // nothing to do
    }  // ret

    void push_m2n(Method_Handle method, frame_type current_frame_type, bool handles) {
        has_m2n = true;  // remember that this stub requires an m2n frame
    }  // push_m2n

    void m2n_save_all()
    {
        stk_extra_saves_size = M2N_EXTRA_SAVES_SPACE;
    }  // m2n_save_all

    void pop_m2n() {
        // if handles are present, pop_m2n may be using an extra target
        bool handles = lil_ic_get_m2n_state(iter.get_context()) == LMS_Handles;
        if (handles) {
            // this pop_m2n will use an extra target
            extra_targets++;
            // it will also execute a call
            does_normal_calls = true;
        }
    }

    void print(char *, LilOperand *) {
        // this is a no-op if debugging is off
#ifdef STUB_DEBUG
        // reserve at least 2 outputs; remember that a call will be made
        if (n_outputs < 2)
            n_outputs = 2;
        does_normal_calls = true;
#endif  // STUB_DEBUG
    }  // print

    //visitor functions - end
    //***********************

};  //class LcgIpfContext


// the following class is a LIL instruction visitor used by
// LilCodeGeneratorIpf to do most of the work.
// After the constructor exits, all the necessary code is in the
// code emitter.
class LcgIpfCodeGen: public LilInstructionVisitor {

    // the following variables are here because visitor functions need to
    // access them in addition to their arguments
    LilCodeStub *cs;
    Merced_Code_Emitter& emitter;
    LcgIpfContext& context;
    tl::MemoryPool& mem;
    LilInstructionIterator iter;
    LilInstructionContext *ic;
    LilInstruction *inst;
    // visit functions can always assume that inst points to the current instruction and ic points to the current context

    unsigned current_alloc;  // keeps track of memory allocation

    LilSig *cur_out_sig;  // keeps track of the current out signature

    // some useful constants
    static const LcgIpfLoc* gp;
    static const LcgIpfLoc* sp;
    static const LcgIpfLoc* r0;
    static const LcgIpfLoc* tp;  // thread pointer
    static const LcgIpfLoc* tmp_op1;  // used for temp storage of operands
    static const LcgIpfLoc* tmp_op2;  // used for temp storage of operands
    static const LcgIpfLoc* tmp_res;  // used for temp storage of results
    static const LcgIpfLoc* tmp_f;      // used for temp storage of FPs
    static const LcgIpfLoc* tmp_addr1;  // used for temp storage of addresses
    static const LcgIpfLoc* tmp_addr2;  // used for temp storage of addresses
    static const LcgIpfLoc* tmp_addr3;  // used for temp storage of addresses
    static const LcgIpfLoc* tmp_addl; // used as a temporary op of addl
    static const unsigned tmp_pred; // used for the jc instruction
    static const unsigned tmp_br;    // used for indirect calls

private:
    /*******************
     * helper functions
     */

    // performs an immediate addition; may use adds, addl, or movl and add,
    // depending on the immediate operand's size
    void add_imm(const LcgIpfLoc *dest, const LcgIpfLoc *src, int64 imm) {
        assert(src->kind == LLK_Gr && (dest->kind==LLK_Stk || dest->kind==LLK_Gr));

        unsigned dst_reg = (dest->kind==LLK_Stk ? tmp_res->addr : dest->addr);

        if (imm >= -0x2000 && imm < 0x2000) {
            // imm fits into 14 bits; use adds
            emitter.ipf_adds(dst_reg, (int) imm, src->addr, current_predicate);
        }
        else if (imm >= -0x200000 && imm < 0x200000) {
            // imm fits into 22 bits; use addl
            emitter.ipf_addl(dst_reg, (int) imm, src->addr, current_predicate);
        }
        else {
            // have to use movl
            unsigned lower_32 = (unsigned) imm;
            unsigned upper_32 = (unsigned) (imm >> 32);
            if (src->addr==0) {
                emitter.ipf_movl(dst_reg, upper_32, lower_32, current_predicate);
            } else {
                emitter.ipf_movl(tmp_addl->addr, upper_32, lower_32, current_predicate);
                emitter.ipf_add(dst_reg, src->addr, tmp_addl->addr, current_predicate);
            }
        }

        if (dest->kind==LLK_Stk)
            move(dest, tmp_res);
    }

    void int_mul(unsigned dest_reg, unsigned src1_reg, unsigned src2_reg)
    {
        unsigned ftmp1 = 7, ftmp2 = 8, ftmp3 = 9;
        emitter.ipf_setf(freg_sig, ftmp1, src1_reg, current_predicate);
        emitter.ipf_setf(freg_sig, ftmp2, src2_reg, current_predicate);
        emitter.ipf_xma(ftmp3, ftmp1, ftmp2, 0, l_form, current_predicate);
        emitter.ipf_getf(freg_sig, dest_reg, ftmp3);
    }

    // binary arithmetic operations without immediates
    // (allowed only for integer values!)
    void bin_op(LilOperation o, const LcgIpfLoc* dest, const LcgIpfLoc* src1, const LcgIpfLoc* src2)
    {
        assert((dest->kind == LLK_Gr || dest->kind == LLK_Stk) &&
               (src1->kind == LLK_Gr || src1->kind == LLK_Stk) &&
               (src2->kind == LLK_Gr || src2->kind == LLK_Stk));

        unsigned dest_reg = (dest->kind == LLK_Stk) ? tmp_res->addr : dest->addr;
        unsigned src1_reg = (src1->kind == LLK_Stk) ? tmp_op1->addr : src1->addr;
        unsigned src2_reg = (src2->kind == LLK_Stk) ? tmp_op2->addr : src2->addr;

        if (src1->kind == LLK_Stk) {
            move(tmp_op1, src1);  // load src1 into tmp_op1
        }
        if (src2->kind == LLK_Stk) {
            move(tmp_op2, src2);  // load src2 into tmp_op2
        }

        switch (o) {
        case LO_Add:
            emitter.ipf_add(dest_reg, src1_reg, src2_reg, current_predicate);
            break;
        case LO_Sub:
            emitter.ipf_sub(dest_reg, src1_reg, src2_reg, current_predicate);
            break;
        case LO_SgMul:
            int_mul(dest_reg, src1_reg, src2_reg);
            break;
        case LO_Shl:
            emitter.ipf_shl(dest_reg, src1_reg, src2_reg, current_predicate);
            break;
        default:
            DIE(("Unexpected operation"));  // control should never reach this point
        }

        if (dest->kind == LLK_Stk) {
            // store tmp_res back to dest
            move(dest, tmp_res);
        }
    }  // bin_op


    // binary op where the second op is immediate
    // (allowed only for integer values!)
    void bin_op_imm(LilOperation o, const LcgIpfLoc* dest, const LcgIpfLoc* src, POINTER_SIZE_INT imm_val) {
        int64 imm = (int64) imm_val;  // convert to signed
        assert(dest->kind != LLK_Fr && src->kind != LLK_Stk16);
        const LcgIpfLoc* dest_reg = (dest->kind == LLK_Stk) ? tmp_res : dest;
        const LcgIpfLoc* src_reg = (src->kind == LLK_Stk) ? tmp_op1 : src;

        if (src_reg != src) {
            move(src_reg, src);  // load src into tmp_op1
        }

        if (o == LO_Shl) {
            if (imm > 63) {
                emitter.ipf_add(dest_reg->addr, 0, 0, current_predicate);  // dest_reg = 0
            }
            else {
                emitter.ipf_shli(dest_reg->addr, src_reg->addr, (int) imm, current_predicate);
            }
        }
        else if (o == LO_Sub) {
            add_imm(dest_reg, src_reg, -imm);
        }
        else if (o == LO_Add) {
            add_imm(dest_reg, src_reg, imm);
        }
        else if (o == LO_SgMul) {
            // This is not as optimised as it could be
            // We could move the immediate directly into a floating point register
            unsigned lower_32 = (unsigned) imm;
            unsigned upper_32 = (unsigned) (imm >> 32);
            emitter.ipf_movl(tmp_addl->addr, upper_32, lower_32, current_predicate);
            int_mul(dest_reg->addr, tmp_addl->addr, src_reg->addr);
        }
        else if (o == LO_And) {
            if (imm>=0x80 && imm<0x80) {
                emitter.ipf_and(dest_reg->addr, src_reg->addr, (unsigned)imm, current_predicate);
            } else {
                unsigned lower_32 = (unsigned) imm;
                unsigned upper_32 = (unsigned) (imm >> 32);
                emitter.ipf_movl(tmp_addl->addr, upper_32, lower_32, current_predicate);
                emitter.ipf_and(dest_reg->addr, tmp_addl->addr, src_reg->addr, current_predicate);
            }
        } else {
            DIE(("Unexpected operation"));  // invalid value in o
        }

        if (dest_reg != dest) {
            //move tmp_res back to dest
            move(dest, dest_reg);
        }
    }  // bin_op_imm

    // subtract op where the first op is immediate
    // (allowed only for intefer values)
    void sub_op_imm(const LcgIpfLoc* dest, POINTER_SIZE_INT imm_val, const LcgIpfLoc* src)
    {
        int64 imm = (int64)imm_val;
        assert(dest->kind != LLK_Fr && src->kind != LLK_Stk16);
        const LcgIpfLoc* dest_reg = (dest->kind == LLK_Stk) ? tmp_res : dest;
        const LcgIpfLoc* src_reg = (src->kind == LLK_Stk) ? tmp_op1 : src;

        if (src_reg!=src)
            move(src_reg, src);

        if (imm>=-0x80 && imm<0x80) {
            emitter.ipf_subi(dest_reg->addr, (unsigned)imm, src_reg->addr, current_predicate);
        } else {
            move_imm(tmp_res, imm);
            emitter.ipf_sub(dest_reg->addr, tmp_res->addr, src_reg->addr, current_predicate);
        }

        if (dest_reg!=dest)
            move(dest, dest_reg);
    }

    // unary operation without immediates
    void un_op(LilOperation o, const LcgIpfLoc* dest, const LcgIpfLoc* src) {
        assert(dest->kind != LLK_Fr && src->kind != LLK_Fr);
        unsigned dest_reg = (dest->kind == LLK_Stk) ? tmp_res->addr : dest->addr;
        unsigned src_reg = (src->kind == LLK_Stk) ? tmp_op1->addr : src->addr;

        if (src->kind == LLK_Stk) {
            move(tmp_op1, src);  // load src into tmp_op1
        }

        switch (o) {
        case LO_Neg:
            emitter.ipf_sub(dest_reg, 0, src_reg, current_predicate);
            break;
        case LO_Not:
            emitter.ipf_xori(dest_reg, 0xFF, src_reg, current_predicate);
            break;
        case LO_Sx1:
            emitter.ipf_sxt(sxt_size_1, dest_reg, src_reg, current_predicate);
            break;
        case LO_Sx2:
            emitter.ipf_sxt(sxt_size_2, dest_reg, src_reg, current_predicate);
            break;
        case LO_Sx4:
            emitter.ipf_sxt(sxt_size_4, dest_reg, src_reg, current_predicate);
            break;
        case LO_Zx1:
            emitter.ipf_zxt(sxt_size_1, dest_reg, src_reg, current_predicate);
            break;
        case LO_Zx2:
            emitter.ipf_zxt(sxt_size_2, dest_reg, src_reg, current_predicate);
            break;
        case LO_Zx4:
            emitter.ipf_zxt(sxt_size_4, dest_reg, src_reg, current_predicate);
            break;
        default:
            DIE(("Unexpected operation"));  // control should never reach this point
        }

        if (dest->kind == LLK_Stk) {
            //move tmp_res back to dest
            move(dest, tmp_res);
        }
    }  // un_op


    // move between two register or stack locations
    void move(const LcgIpfLoc* dest, const LcgIpfLoc* src) {
        if (dest->kind == LLK_Stk) {
            // put sp + (dest offset) in tmp_addr1
            add_imm(tmp_addr1, sp, dest->addr);

            if (src->kind == LLK_Stk) {
                // put sp + (src offset) in tmp_addr2
                add_imm(tmp_addr2, sp, src->addr);
                // load  src into tmp_op1
                emitter.ipf_ld(int_mem_size_8, mem_ld_fill, mem_none, tmp_op1->addr, tmp_addr2->addr, current_predicate);
                // store tmp_op1 into dest
                emitter.ipf_st(int_mem_size_8, mem_st_spill, mem_none, tmp_addr1->addr, tmp_op1->addr, current_predicate);
            }
            else if (src->kind == LLK_Stk16) {
                // put sp + (src offset) in tmp_addr2
                add_imm(tmp_addr2, sp, src->addr);
                // load (128-bit) src into tmp_f
                emitter.ipf_ldf(float_mem_size_d, mem_ld_fill, mem_none, tmp_f->addr, tmp_addr2->addr, current_predicate);
                // store (64-bit) tmp_f into dest
                emitter.ipf_stf(float_mem_size_d, mem_st_none, mem_none, tmp_addr1->addr, tmp_f->addr, current_predicate);
            }
            else if (src->kind == LLK_Gr) {
                // store src into dest
                emitter.ipf_st(int_mem_size_8, mem_st_spill, mem_none, tmp_addr1->addr, src->addr, current_predicate);
            }
            else if (src->kind == LLK_Fr) {
                // store src into (64-bit) dest
                emitter.ipf_stf(float_mem_size_d, mem_st_none, mem_none, tmp_addr1->addr, src->addr, current_predicate);
            }
        }
        else if (dest->kind == LLK_Stk16) {
            // put sp + (dest offset) in tmp_addr1
            add_imm(tmp_addr1, sp, dest->addr);

            if (src->kind == LLK_Stk) {
                // put sp + (src offset) in tmp_addr2
                add_imm(tmp_addr2, sp, src->addr);
                // load (64-bit) src into tmp_f
                emitter.ipf_ldf(float_mem_size_d, mem_ld_none, mem_none, tmp_f->addr, tmp_addr2->addr, current_predicate);
                // store tmp_f into (128-bit) dest
                emitter.ipf_stf(float_mem_size_d, mem_st_spill, mem_none, tmp_addr1->addr, tmp_f->addr, current_predicate);
            }
            else if (src->kind == LLK_Stk16) {
                // put sp + (src offset) in tmp_addr2
                add_imm(tmp_addr2, sp, src->addr);
                // load (128-bit) src into tmp_f
                emitter.ipf_ldf(float_mem_size_d, mem_ld_fill, mem_none, tmp_f->addr, tmp_addr2->addr, current_predicate);
                // store tmp_f into (128-bit) dest
                emitter.ipf_stf(float_mem_size_d, mem_st_spill, mem_none, tmp_addr1->addr, tmp_f->addr, current_predicate);
            }
            else if (src->kind == LLK_Fr) {
                // store src into (128-bit) dest
                emitter.ipf_stf(float_mem_size_d, mem_st_spill, mem_none, tmp_addr1->addr, src->addr, current_predicate);
            }
            else {
                LDIE(73, "Unexpected kind");  // src shouldn't be a GR!
            }
        }
        else if (dest->kind == LLK_Gr) {
            if (src->kind == LLK_Stk) {
                // put sp + (src offset) in tmp_addr2
                add_imm(tmp_addr2, sp, src->addr);
                // load src into dest
                emitter.ipf_ld(int_mem_size_8, mem_ld_fill, mem_none, dest->addr, tmp_addr2->addr, current_predicate);
            }
            else if (src->kind == LLK_Gr) {
                // move src into dest
                emitter.ipf_mov(dest->addr, src->addr, current_predicate);
            }
            else {
                LDIE(73, "Unexpected kind");  // src->kind shouldn't be LLK_Fr or LLK_Stk16
            }
        }
        else if (dest->kind == LLK_Fr)  {
            if (src->kind == LLK_Stk) {
                // put sp + (src offset) in tmp_addr2
                add_imm(tmp_addr2, sp, src->addr);
                // load (64-bit) src into dest
                emitter.ipf_ldf(float_mem_size_d, mem_ld_none, mem_none, dest->addr, tmp_addr2->addr, current_predicate);
            }
            else if (src->kind == LLK_Stk16) {
                // put sp + (src offset) in tmp_addr2
                add_imm(tmp_addr2, sp, src->addr);
                // load (128-bit) src into dest
                emitter.ipf_ldf(float_mem_size_d, mem_ld_fill, mem_none, dest->addr, tmp_addr2->addr, current_predicate);
            }
            else if (src->kind == LLK_Fr) {
                // move src into dest
                emitter.ipf_fmov(dest->addr, src->addr, current_predicate);
            }
            else {
                LDIE(73, "Unexpected kind");  // src should not be GR!
            }
        }
        else {
            DIE(("Unknown kind"));  // dest->kind has illegal value!
        }
    }  // move


    // move immediate
    // (not allowed for FRs!)
    void move_imm(const LcgIpfLoc* dest, POINTER_SIZE_INT imm_val) {
        assert(dest->kind==LLK_Gr || dest->kind==LLK_Stk);
        add_imm(dest, r0, (int64)imm_val);
    }  // move_imm

    // generate instructions that compute a condition into two predicate registers
    void do_cond(LilPredicate p, LilOperand* op1, LilOperand* op2, unsigned true_pr, unsigned false_pr)
    {
        assert(current_predicate==0);

        // make sure operands are in registers
        unsigned op1_reg = 0, op2_reg = 0;  // GR numbers holding op1 and op2

        if (lil_operand_is_immed(op1)) {
            int64 imm = (int64) lil_operand_get_immed(op1);
            move_imm(tmp_op1, imm);
            op1_reg = tmp_op1->addr;
        }
        else { // op1 is a variable
            LilVariable *op1_var = lil_operand_get_variable(op1);
            const LcgIpfLoc* op1_loc =
                context.get_var_loc(op1_var, inst, ic, false);
            if (op1_loc->kind == LLK_Stk) {
                // load op1_loc into tmp_op1
                move(tmp_op1, op1_loc);
                op1_reg = tmp_op1->addr;
            }
            else if (op1_loc->kind == LLK_Gr) {
                assert(op1_loc->kind != LLK_Fr);  // comparisons available only for ints!
                op1_reg = op1_loc->addr;
            }
            else {
                DIE(("Wrong kind"));  // comparisons available only for ints!
            }
        }

        if (lil_predicate_is_binary(p)) {
            if (lil_operand_is_immed(op2)) {
                int64 imm = (int64) lil_operand_get_immed(op2);
                move_imm(tmp_op2, imm);
                op2_reg = tmp_op2->addr;
            }
            else { // op2 is a variable
                LilVariable *op2_var = lil_operand_get_variable(op2);
                const LcgIpfLoc* op2_loc =
                    context.get_var_loc(op2_var, inst, ic, false);
                if (op2_loc->kind == LLK_Stk) {
                    // load op2_loc into tmp_op2
                    move(tmp_op2, op2_loc);
                    op2_reg = tmp_op2->addr;
                }
                else if (op2_loc->kind == LLK_Gr) {
                    op2_reg = op2_loc->addr;
                }
                else {
                    DIE(("Wrong kind"));  // comparisons available only for ints!
                }
            }
        }
        else { // the predicate is unary, i.e. the 2nd op is r0
            op2_reg = 0;
        }

        // do the comparison using a cmp instruction, place result in tmp_pred
        Int_Comp_Rel emitter_p = icmp_invalid;
        switch (p) {
        case LP_IsZero:
        case LP_Eq:
            emitter_p = icmp_eq;  break;
        case LP_IsNonzero:
        case LP_Ne:
            emitter_p = icmp_ne;  break;
        case LP_Le:
            emitter_p = icmp_le;  break;
        case LP_Lt:
            emitter_p = icmp_lt;  break;
        case LP_Ule:
            emitter_p = icmp_leu; break;
        case LP_Ult:
            emitter_p = icmp_ltu; break;
        default:
            DIE(("Unknown predicate"));  // should never be reached
        }

        emitter.ipf_cmp(emitter_p, cmp_unc, true_pr, false_pr, op1_reg, op2_reg, false, 0);
    }

    // generate instructions that calculate a LIL address;
    // return the GR number where the address can be found
    // (this can be either the base register itself, or a temp register)
    const LcgIpfLoc* do_addr(LilVariable *base, unsigned scale, LilVariable *index, POINTER_SIZE_INT offset) {
        // locations for base and index (NULL if base or index doesn't exist)
        const LcgIpfLoc *bloc=NULL, *iloc=NULL;
        // shift amount, if index exists
        unsigned shift=0;

        if (base != NULL) {
            const LcgIpfLoc* orig_bloc =
                context.get_var_loc(base, inst, ic, false);
            assert(orig_bloc->kind == LLK_Gr || orig_bloc->kind == LLK_Stk);
            if (orig_bloc->kind == LLK_Stk) {
                move(tmp_addr1, orig_bloc);
                bloc = tmp_addr1;
            }
            else {
                bloc = orig_bloc;
            }
        }

        if (index != NULL && scale != 0) {
            const LcgIpfLoc* orig_iloc =
                context.get_var_loc(index, inst, ic, false);
            assert(orig_iloc->kind == LLK_Gr || orig_iloc->kind == LLK_Stk);

            // fetch index from memory if needed
            if (orig_iloc->kind == LLK_Stk) {
                move(tmp_res, orig_iloc);
                iloc = tmp_addr2;
            }
            else {
                iloc = orig_iloc;
            }
      
            // determine size of shift
            while (scale > 1) {
                scale >>= 1;
                shift++;
            }
            assert(shift >= 1 && shift <= 4);
        }        

        if (bloc) {
            if (offset) {
                add_imm(tmp_addr3, bloc, offset);
                if (iloc)
                    emitter.ipf_shladd(tmp_addr3->addr, iloc->addr, shift, tmp_addr3->addr, current_predicate);
                return tmp_addr3;
            }
            else {  // no offset
                if (iloc) {
                    emitter.ipf_shladd(tmp_addr3->addr, iloc->addr, shift, bloc->addr, current_predicate);
                    return tmp_addr3;
                }
                else // no index
                    return bloc;
            }
        }  // if (bloc)
        else { // no base
            if (offset) {
                move_imm(tmp_addr3, offset);
                if (iloc)
                    emitter.ipf_shladd(tmp_addr3->addr, iloc->addr, shift, tmp_addr3->addr, current_predicate);
            }
            else {  // no offset
                if (iloc)
                    emitter.ipf_shladd(tmp_addr3->addr, iloc->addr, shift, 0, current_predicate);
                else // no index
                    DIE(("Can't have no base, no index and no offset"));
            }
            return tmp_addr3;
        }  // else (bloc)
    }  // do_addr


    // implements the copying of incoming to outgoing args in in2out and tailcall
    void do_in_to_out()
    {
        assert(!context.entry_sig_is_arbitrary());
        unsigned n_inputs = context.get_num_inputs();
        unsigned i;
        for (i=0; i<8 && i<n_inputs;  i++) {
            const LcgIpfLoc* in_loc = context.get_input(i);
            const LcgIpfLoc* out_loc = context.get_output(i, cur_out_sig);
            if (in_loc != out_loc) {
                move(out_loc, in_loc);
            }
        }
        if (i==n_inputs) return;

        // The rest of the inputs and outputs are on the stack, so use an optimised memory copy
        const LcgIpfLoc* in_loc = context.get_input(8);
        const LcgIpfLoc* out_loc = context.get_output(8, cur_out_sig);
        assert(in_loc->kind==LLK_Stk && out_loc->kind==LLK_Stk);
        add_imm(tmp_addr1, sp, in_loc->addr);
        add_imm(tmp_addr2, sp, out_loc->addr);
        for(; i<n_inputs; i++) {
            emitter.ipf_ld_inc_imm(int_mem_size_8, mem_ld_none, mem_none, tmp_res->addr, tmp_addr1->addr, 8, current_predicate);
            emitter.ipf_st_inc_imm(int_mem_size_8, mem_st_none, mem_none, tmp_addr2->addr, tmp_res->addr, 8, current_predicate);
        }
    }  // do_in_to_out


    // sets up the stack frame
    void set_stk_frame()
    {
        unsigned stk_size = context.get_stk_size();
        if (stk_size != 0) {
            add_imm(sp, sp, -(int)stk_size);
        }
    }  // set_stk_frame

    // unsets the stack frame
    void unset_stk_frame()
    {
        unsigned stk_size = context.get_stk_size();
        if (stk_size != 0) {
            add_imm(sp, sp, (int)stk_size);
        }
    }  // unset_stk_frame

    // moves FR inputs, originally residing in f8-f15, into the stack
    void move_FR_inputs()
    {
        if (context.must_move_fr_inputs()) {
            unsigned n_fr_inputs = context.get_num_fr_inputs();
            int start_offset = context.get_input_save_offset();

            // using the same address reg in all stores will create too many dependencies;
            // alternate the address reg between tmp_addr1 and tmp_addr2 for more ILP
            add_imm(tmp_addr1, sp, start_offset);
            if (n_fr_inputs >=2)
                add_imm(tmp_addr2, sp, start_offset+16);

            for (unsigned i=0;  i < n_fr_inputs;  i++) {
                unsigned addr_reg = (i%2) ? tmp_addr2->addr : tmp_addr1->addr;
                emitter.ipf_stf_inc_imm(float_mem_size_d, mem_st_spill, mem_none,
                                         addr_reg, 8+i, 32, current_predicate);
            }
        }
    }  // move_FR_inputs

    // moves FR inputs from the stack back to f8-f15
    void unmove_FR_inputs()
    {
        if (context.must_move_fr_inputs()) {
            unsigned n_fr_inputs = context.get_num_fr_inputs();
            int start_offset = context.get_input_save_offset();

            // using the same address reg in all stores will create too many dependencies;
            // alternate the address reg between tmp_addr1 and tmp_addr2 for more ILP
            add_imm(tmp_addr1, sp, start_offset);
            if (n_fr_inputs >=2)
                add_imm(tmp_addr2, sp, start_offset+16);

            for (unsigned i=0;  i < n_fr_inputs;  i++) {
                unsigned addr_reg = (i%2) ? tmp_addr2->addr : tmp_addr1->addr;
                emitter.ipf_ldf_inc_imm(float_mem_size_d, mem_ld_fill, mem_none, 8+i, addr_reg, 32, current_predicate);
            }
        }
    }  // unmove_FR_inputs


    // emits the code that needs to go at the very beginning of the code stub
    void do_entry()
    {
        assert(current_predicate==0);

        // start with an alloc instruction
        const LcgIpfLoc* pfs_save_gr = context.get_pfs_save_gr();
        if (pfs_save_gr) {
            emitter.ipf_alloc(pfs_save_gr->addr,
                               context.get_input_space_size(),
                               context.get_local_space_size(),
                               context.get_output_space_size(),
                               0);  // no rotating regs!
        }

        // save b0
        const LcgIpfLoc* return_save_gr = context.get_return_save_gr();
        if (return_save_gr) {
            emitter.ipf_mfbr(return_save_gr->addr, BRANCH_RETURN_LINK_REG);
        }

        // save gp
        const LcgIpfLoc* gp_save_gr = context.get_gp_save_gr();
        if (gp_save_gr)
            emitter.ipf_mov(gp_save_gr->addr, GP_REG);

        set_stk_frame();
        move_FR_inputs();
    }  // do_entry



    /**********************
     * visitor functions
     */

    unsigned current_predicate;  // Predicate all instructions with this instruction (may not work for all instructions)

public:

    void ret()
    {
        unset_stk_frame();

        // restore gp
        const LcgIpfLoc* gp_save_gr = context.get_gp_save_gr();
        if (gp_save_gr)
            emitter.ipf_mov(GP_REG, gp_save_gr->addr, current_predicate);

        // restore b0
        const LcgIpfLoc* return_save_gr = context.get_return_save_gr();
        if (return_save_gr) {
            emitter.ipf_mtbr(BRANCH_RETURN_LINK_REG, return_save_gr->addr, current_predicate);
        }

        // restore ar.pfs
        const LcgIpfLoc* pfs_save_gr = context.get_pfs_save_gr();
        if (pfs_save_gr)
            emitter.ipf_mtap(AR_pfs, pfs_save_gr->addr, current_predicate);

        // if this ret is going back to LIL or managed code, reset ar.ccv
        LilCc call_conv = lil_sig_get_cc(context.get_entry_sig());
        if (context.must_reset_ccv() &&
            (call_conv == LCC_Managed ||
             call_conv == LCC_Rth)) {
            emitter.ipf_mtap(AR_ccv, 0, current_predicate);
        }

        // return
        emitter.ipf_brret(br_few, br_sptk, br_none, BRANCH_RETURN_LINK_REG, current_predicate);
    }  // ret


    void label(LilLabel lab) {
        emitter.set_target(context.get_label_id(lab));
    }  // label


    void locals(unsigned) {
        // nothing to be done here;
        // has been taken care of in the prepass
    }  // locals


    void std_places(unsigned) {
        // nothing to be done here;
        // has been taken care of in the prepass
    }  // std_places

    void alloc(LilVariable* var, unsigned sz) {
        // the actual size allocated will always be a multiple of 8
        sz = (sz + 0x7) & ~0x7;
        int alloc_offset = context.get_alloc_start_offset() + current_alloc;
        current_alloc += sz;

        // var = sp + alloc_offset
        const LcgIpfLoc* var_loc =
            context.get_var_loc(var, inst, ic, true);
        add_imm(var_loc, sp, alloc_offset);
    }  // alloc


    void asgn(LilVariable* dest, enum LilOperation o, LilOperand* op1, LilOperand*op2)
    {
        const LcgIpfLoc* dest_loc =
            context.get_var_loc(dest, inst, ic, true);

        if (o == LO_Mov) {
            if (lil_operand_is_immed(op1)) {
                move_imm(dest_loc, lil_operand_get_immed(op1));
            }
            else {
                const LcgIpfLoc* src_loc =
                    context.get_var_loc(lil_operand_get_variable(op1), inst, ic, false);
                move(dest_loc, src_loc);
            }
        }
        else if (lil_operation_is_binary(o)) {
            if (lil_operand_is_immed(op1) && lil_operand_is_immed(op2)) {
                // type-convert to get signed types of same length
                int64 op1_imm = (int64) lil_operand_get_immed(op1);
                int64 op2_imm = (int64) lil_operand_get_immed(op2);
                POINTER_SIZE_INT result = 0;
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
                    DIE(("Unexpected LIL operation"));   // control should never reach this point
                }
                move_imm(dest_loc, result);
            }
            else if (lil_operand_is_immed(op1)) {
                const LcgIpfLoc* src2_loc = context.get_var_loc(lil_operand_get_variable(op2), inst, ic, false);
                switch (o) {
                case LO_Add:
                    bin_op_imm(LO_Add, dest_loc, src2_loc, lil_operand_get_immed(op1));
                    break;
                case LO_Sub:
                    sub_op_imm(dest_loc, lil_operand_get_immed(op1), src2_loc);
                    break;
                case LO_SgMul:
                case LO_Shl:
                    move_imm(tmp_res, (int64) lil_operand_get_immed(op1));
                    bin_op(o, dest_loc, tmp_res, src2_loc);
                    break;
                default:
                    DIE(("Unexpected LIL operation"));  // control should never reach this point
                }
            }
            else if (lil_operand_is_immed(op2)) {
                const LcgIpfLoc* src1_loc = context.get_var_loc(lil_operand_get_variable(op1), inst, ic, false);
                bin_op_imm(o, dest_loc, src1_loc, lil_operand_get_immed(op2));
            }
            else {  // both operands non-immediate
                const LcgIpfLoc* src1_loc = context.get_var_loc(lil_operand_get_variable(op1), inst, ic, false);
                const LcgIpfLoc* src2_loc = context.get_var_loc(lil_operand_get_variable(op2), inst, ic, false);
                bin_op(o, dest_loc, src1_loc, src2_loc);
            }
        }
        else {  // unary operation
            if (lil_operand_is_immed(op1)) {
                int64 imm = (int64) lil_operand_get_immed(op1);  // typecast to get signed type
                int64 result = 0;
                switch (o) {
                case LO_Neg:
                    result = -imm;
                    break;
                case LO_Not:
                    result = ~imm;
                    break;
                case LO_Sx1:
                    result = (int64) (I_8) imm;
                    break;
                case LO_Sx2:
                    result = (int64) (int16) imm;
                    break;
                case LO_Sx4:
                    result = (int64) (I_32) imm;
                    break;
                case LO_Zx1:
                    result = (int64) (uint64) (U_8) imm;
                    break;
                case LO_Zx2:
                    result = (int64) (uint64) (uint16) imm;
                    break;
                case LO_Zx4:
                    result = (int64) (uint64) (U_32) imm;
                    break;
                default:
                    DIE(("Unexpected LIL operation"));  // control should never reach this point
                }
                move_imm(dest_loc, result);
            }
            else { // non-immediate operand
                const LcgIpfLoc* src1_loc = context.get_var_loc(lil_operand_get_variable(op1), inst, ic, false);
                un_op(o, dest_loc, src1_loc);
            }
        }
    }  // asgn


    void ts(LilVariable* var) {
        const LcgIpfLoc* var_loc =
            context.get_var_loc(var, inst, ic, true);
        move(var_loc, tp);
    }  // ts

    void handles(LilOperand* op)
    {
        assert(current_predicate==0);

        if (lil_operand_is_immed(op)) {
            m2n_gen_set_local_handles_imm(&emitter, lil_operand_get_immed(op));
        } else {
            const LcgIpfLoc* op_loc = context.get_var_loc(lil_operand_get_variable(op), inst, ic, false);
            assert(op_loc->kind == LLK_Gr);
            m2n_gen_set_local_handles(&emitter, op_loc->addr);
        }
    } // handles

    void ld(LilType t, LilVariable* dst, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel acqrel, LilLdX ext)
    {
        // form the address
        const LcgIpfLoc* addr_loc = do_addr(base, scale, index, offset);

        // decide the size of the load (1, 2, 4, or 8)
        Int_Mem_Size size = int_mem_size_1;
        Sxt_Size ext_size = sxt_size_invalid;
        bool sxt = ext==LLX_Sign;
        switch(t) {
        case LT_G1:
            size = int_mem_size_1; ext_size = sxt_size_1; break;
        case LT_G2:
            size = int_mem_size_2; ext_size = sxt_size_2; break;
        case LT_G4:
            size = int_mem_size_4; ext_size = sxt_size_4; break;
        case LT_G8:
        case LT_Ref:
        case LT_PInt:
            size = int_mem_size_8; sxt = false; break;
        case LT_F4:
        case LT_F8:
        case LT_Void:
            DIE(("Unexpected LIL type"));  // types not allowed in loads / stores
        default:
            DIE(("Unknown LIL type"));  // invalid value in type
        }

        Ld_Flag ld_flag = mem_ld_none;
        switch (acqrel) {
        case LAR_Acquire: ld_flag = mem_ld_acq; break;
        case LAR_Release: DIE(("Unexpected acqrel value")); break;
        case LAR_None: break;
        }

        const LcgIpfLoc* dst_loc = context.get_var_loc(dst, inst, ic, true);
        if (dst_loc->kind == LLK_Stk) {
            // load value into tmp_res
            emitter.ipf_ld(size, ld_flag, mem_none, tmp_res->addr, addr_loc->addr, current_predicate);
            if (sxt)
                emitter.ipf_sxt(ext_size, tmp_res->addr, tmp_res->addr, current_predicate);
            // store value into dst_loc
            move(dst_loc, tmp_res);
        }
        else if (dst_loc->kind == LLK_Gr) {
            // load value into dst_loc
            emitter.ipf_ld(size, ld_flag, mem_none, dst_loc->addr, addr_loc->addr, current_predicate);
            if (sxt)
                emitter.ipf_sxt(ext_size, dst_loc->addr, dst_loc->addr, current_predicate);
        }
        else {
            LDIE(73, "Unexpected kind");  // dst_loc shouldn't be FR or STK16
        }
    }  // ld


    void st(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel acqrel, LilOperand* src)
    {
        // move the address into tmp_addr1
        const LcgIpfLoc* addr_loc = do_addr(base, scale, index, offset);

        // decide the size of the store (1, 2, 4, or 8)
        Int_Mem_Size size = int_mem_size_1;
        switch(t) {
        case LT_G1:
            size = int_mem_size_1; break;
        case LT_G2:
            size = int_mem_size_2; break;
        case LT_G4:
            size = int_mem_size_4; break;
        case LT_G8:
        case LT_Ref:
        case LT_PInt:
            size = int_mem_size_8; break;
        case LT_F4:
        case LT_F8:
        case LT_Void:
            DIE(("Unexpected LIL type"));  // types not allowed in loads / stores
        default:
            DIE(("Unknown LIL type"));  // invalid value in type
        }

        St_Flag st_flag = mem_st_none;
        switch (acqrel) {
        case LAR_Acquire: DIE(("Unexpected value of acqrel")); break;
        case LAR_Release: st_flag = mem_st_rel; break;
        case LAR_None: break;
        }

        const LcgIpfLoc* src_loc = NULL;
        if (lil_operand_is_immed(src)) {
            // move source into temp reg
            POINTER_SIZE_INT imm = lil_operand_get_immed(src);
            if (imm == 0) {
                //just use r0
                src_loc = r0;
            }
            else {
                move_imm(tmp_op1, lil_operand_get_immed(src));
                src_loc = tmp_op1;
            }
        }
        else {
            LilVariable *src_var = lil_operand_get_variable(src);
            src_loc = context.get_var_loc(src_var, inst, ic, false);
        }
        if (src_loc->kind == LLK_Stk) {
            // load src_loc into tmp_res
            move(tmp_res, src_loc);
            // store tmp_res
            emitter.ipf_st(size, st_flag, mem_none, addr_loc->addr, tmp_res->addr, current_predicate);
        }
        else if (src_loc->kind == LLK_Gr) {
            // store src_loc
            emitter.ipf_st(size, st_flag, mem_none, addr_loc->addr, src_loc->addr, current_predicate);
        }
        else {
            LDIE(73, "Unexpected kind");  // src_loc shouldn't be FR or STK16!
        }
    }  // st


    void inc(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel acqrel)
    {
        assert(acqrel==LAR_None);

        const LcgIpfLoc* addr_loc = do_addr(base, scale, index, offset);
        // load addr1 into tmp_res
        emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none,
                        tmp_res->addr, addr_loc->addr, current_predicate);
        // inc tmp_res
        add_imm(tmp_res, tmp_res, 1);
        // store tmp_res back to addr1
        emitter.ipf_st(int_mem_size_8, mem_st_none, mem_none,
                        addr_loc->addr, tmp_res->addr, current_predicate);
    }  // inc

    void cas(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel acqrel,
             LilOperand* cmp, LilOperand* src, LilLabel label)
    {
        assert(current_predicate==0);

        // move the address into tmp_addr1
        const LcgIpfLoc* addr_loc = do_addr(base, scale, index, offset);

        // decide the size of the store (1, 2, 4, or 8)
        Int_Mem_Size size = int_mem_size_1;
        switch(t) {
        case LT_G1:
            size = int_mem_size_1; break;
        case LT_G2:
            size = int_mem_size_2; break;
        case LT_G4:
            size = int_mem_size_4; break;
        case LT_G8:
        case LT_Ref:
        case LT_PInt:
            size = int_mem_size_8; break;
        case LT_F4:
        case LT_F8:
        case LT_Void:
            DIE(("Unexpected LIL type"));  // types not allowed in loads / stores
        default:
            DIE(("Unknown LIL type"));  // invalid value in type
        }

        Cmpxchg_Flag flag = mem_cmpxchg_acq;
        switch (acqrel) {
        case LAR_Acquire: flag = mem_cmpxchg_acq; break;
        case LAR_Release: flag = mem_cmpxchg_rel; break;
        default:
            DIE(("Unexpected value of acqrel"));
        }

        const LcgIpfLoc* src_loc = NULL;
        if (lil_operand_is_immed(src)) {
            // move source into temp reg
            POINTER_SIZE_INT imm = lil_operand_get_immed(src);
            if (imm == 0) {
                //just use r0
                src_loc = r0;
            }
            else {
                move_imm(tmp_op1, lil_operand_get_immed(src));
                src_loc = tmp_op1;
            }
        }
        else {
            LilVariable *src_var = lil_operand_get_variable(src);
            src_loc = context.get_var_loc(src_var, inst, ic, false);
            if (src_loc->kind == LLK_Stk) {
                move(tmp_op1, src_loc);
                src_loc = tmp_op1;
            }
        }

        const LcgIpfLoc* cmp_loc = NULL;
        if (lil_operand_is_immed(cmp)) {
            // move source into temp reg
            POINTER_SIZE_INT imm = lil_operand_get_immed(cmp);
            if (imm == 0) {
                //just use r0
                cmp_loc = r0;
            }
            else {
                move_imm(tmp_op2, lil_operand_get_immed(cmp));
                cmp_loc = tmp_op2;
            }
        }
        else {
            LilVariable *cmp_var = lil_operand_get_variable(cmp);
            cmp_loc = context.get_var_loc(cmp_var, inst, ic, false);
            if (cmp_loc->kind == LLK_Stk) {
                move(tmp_op2, cmp_loc);
                cmp_loc = tmp_op2;
            }
        }

        emitter.ipf_mtap(AR_ccv, cmp_loc->addr, current_predicate);
        emitter.ipf_cmpxchg(size, flag, mem_none, tmp_res->addr, addr_loc->addr, src_loc->addr, current_predicate);
        emitter.ipf_mtap(AR_ccv, 0, current_predicate);
        emitter.ipf_cmp(icmp_ne, cmp_unc, tmp_pred, 0, tmp_res->addr, cmp_loc->addr, false, 0);
        unsigned label_id = context.get_label_id(label);
        emitter.ipf_br(br_cond, br_few, br_dptk, br_none, label_id, tmp_pred);
    }

    void j(LilLabel lab)
    {
        unsigned label_id = context.get_label_id(lab);
        emitter.ipf_br(br_cond, br_few, br_sptk, br_none, label_id, current_predicate);
    }  // j

    void jc(LilPredicate p, LilOperand* op1, LilOperand* op2, LilLabel label)
    {
        assert(current_predicate==0);

        // compute the condition
        do_cond(p, op1, op2, tmp_pred, 0);

        // the actual branch
        unsigned label_id = context.get_label_id(label);
        emitter.ipf_br(br_cond, br_few, br_dptk, br_none, label_id, tmp_pred);
    }  // jc

    void out(LilSig* sig)
    {
        cur_out_sig = sig;
        // nothing else to do; space has been reserved already
    }  // out

    void in2out(LilSig* sig)
    {
        assert(!context.entry_sig_is_arbitrary());
        cur_out_sig = sig;
        do_in_to_out();
    }  // in2out


    void call(LilOperand* target, LilCallKind kind)
    {
        assert(current_predicate==0);

        LilSig *out_sig = lil_ic_get_out_sig(ic);
        LilCc call_conv = (kind == LCK_TailCall) ? lil_sig_get_cc(context.get_entry_sig()) :
            lil_sig_get_cc(out_sig);

        // reset ar.ccv if this call could lead to managed or LIL code
        if (context.must_reset_ccv() &&
            (call_conv == LCC_Managed ||
             call_conv == LCC_Rth)) {
            emitter.ipf_mtap(AR_ccv, 0);
        }

        if (kind == LCK_TailCall) {
            // move FR inputs to their original place
            unmove_FR_inputs();

            // unset stack, so that old stacked arguments become visible
            unset_stk_frame();

            // restore gp
            const LcgIpfLoc* gp_save_gr = context.get_gp_save_gr();
            if (gp_save_gr)
                emitter.ipf_mov(GP_REG, gp_save_gr->addr);

            // restore b0
            const LcgIpfLoc* return_save_gr = context.get_return_save_gr();
            if (return_save_gr) {
                emitter.ipf_mtbr(BRANCH_RETURN_LINK_REG, return_save_gr->addr);
            }

            // restore ar.pfs
            const LcgIpfLoc* pfs_save_gr = context.get_pfs_save_gr();
            if (pfs_save_gr)
                emitter.ipf_mtap(AR_pfs, pfs_save_gr->addr);

            // jump (instead of calling)
            if (lil_operand_is_immed(target)) {
                void **proc_ptr = (void **) lil_operand_get_immed(target);
                emit_branch_with_gp(emitter, proc_ptr);
            }
            else {
                LilVariable *var = lil_operand_get_variable(target);
                const LcgIpfLoc* loc =
                    context.get_var_loc(var, inst, ic, false);
                unsigned call_addr_gr = 0;
                if (loc->kind == LLK_Gr) {
                    call_addr_gr = loc->addr;
                }
                else if (loc->kind == LLK_Stk) {
                    // load loc into tmp_res
                    move(tmp_res, loc);
                    call_addr_gr = tmp_res->addr;
                }
                else {
                    LDIE(73, "Unexpected kind");  // address can't be FP!
                }
                emitter.ipf_mtbr(tmp_br, call_addr_gr);
                emitter.ipf_bri(br_cond, br_many, br_sptk, br_none, tmp_br);
            }

            return;
        }  // kind == LCK_TailCall

        // kind == LCK_Call or kind == LCK_CallNoRet

        if (lil_operand_is_immed(target)) {
            void** proc_ptr = (void **) lil_operand_get_immed(target);
            void*  fn_addr = proc_ptr[0];
            void*  gp_new  = proc_ptr[1];
            void*  gp_old  = get_vm_gp_value();
            if (gp_new != gp_old) {
                // Set new gp
                emit_mov_imm_compactor(emitter, GP_REG, (uint64)gp_new);
            }
            emit_mov_imm_compactor(emitter, tmp_res->addr, (uint64)fn_addr, 0);
            if (context.has_push_m2n()) {
                emitter.ipf_mtbr(BRANCH_CALL_REG, tmp_res->addr);
                emit_mov_imm_compactor(emitter, tmp_res->addr, (uint64)m2n_gen_flush_and_call(), 0);
            }
            emitter.ipf_mtbr(tmp_br, tmp_res->addr);
            emitter.ipf_bricall(br_few, br_sptk, br_none, BRANCH_RETURN_LINK_REG, tmp_br);
            if (gp_new != gp_old) {
                // Restore gp
                const LcgIpfLoc* gp_save_gr = context.get_gp_save_gr();
                assert(gp_save_gr);
                emitter.ipf_mov(GP_REG, gp_save_gr->addr, current_predicate);
            }
        }
        else {
            LilVariable *var = lil_operand_get_variable(target);
            const LcgIpfLoc* loc =
                context.get_var_loc(var, inst, ic, false);
            unsigned call_addr_gr = 0;
            if (loc->kind == LLK_Gr) {
                call_addr_gr = loc->addr;
            }
            else if (loc->kind == LLK_Stk) {
                // load loc into tmp_res
                move(tmp_res, loc);
                call_addr_gr = tmp_res->addr;
            }
            else {
                LDIE(73, "Unexpected kind");  // address can't be FP!
            }
            if (context.has_push_m2n()) {
                emitter.ipf_mtbr(BRANCH_CALL_REG, call_addr_gr);
                emit_mov_imm_compactor(emitter, call_addr_gr, (uint64)m2n_gen_flush_and_call(), 0);
            }
            emitter.ipf_mtbr(tmp_br, call_addr_gr);
            emitter.ipf_bricall(br_few, br_sptk, br_none, BRANCH_RETURN_LINK_REG, tmp_br);
        }
    }  // call


    void push_m2n(Method_Handle method, frame_type current_frame_type, bool handles)
    {
        assert(current_predicate==0);
        m2n_gen_push_m2n(&emitter, method, current_frame_type, handles, context.get_stk_size(), 0, 0, false);
    }  // push_m2n

    void m2n_save_all()
    {
        assert(current_predicate==0);
        add_imm(sp, sp, context.get_extra_saves_start_offset()+M2N_EXTRA_SAVES_SPACE-16);
        m2n_gen_save_extra_preserved_registers(&emitter);
        add_imm(sp, sp, -(int64)(context.get_extra_saves_start_offset()-16));
    }  // m2n_save_all

    void pop_m2n()
    {
        assert(current_predicate==0);
        // see if handles are present
        bool handles = lil_ic_get_m2n_state(ic) == LMS_Handles;
        int target = -1;
        if (handles)
            // this pop_m2n will need to use an emitter target
            target = (int) context.get_unused_target();
        M2nPreserveRet pr;
        LilType rt = lil_ic_get_ret_type(ic);
        if (rt==LT_Void)
            pr = MPR_None;
        else if (rt==LT_F4 || rt==LT_F8)
            pr = MPR_Fr;
        else
            pr = MPR_Gr;
        m2n_gen_pop_m2n(&emitter, handles, pr, false, context.get_first_output_gr(), target);
    }  // pop_m2n

    void print(char *str, LilOperand *o)
    {
        assert(current_predicate==0);

        // this is a no-op if debugging is off
#ifdef STUB_DEBUG
        unsigned print_reg;
        if (lil_operand_is_immed(o)) {
            // dummy operand; print r0
            print_reg = 0;
        }
        else {
            LilVariable *var = lil_operand_get_variable(o);
            const LcgIpfLoc* var_loc =
                context.get_var_loc(var, inst, ic, false);
            assert(var_loc->kind == LLK_Gr);
            print_reg = var_loc->addr;
        }
        emit_print_reg(emitter, str, print_reg, context.get_num_inputs(),
                       context.get_first_output_gr(), false);
#endif  // STUB_DEBUG
    }  // print

public:
    /******************************
     * constructors and destructors
     */
    void *operator new(size_t sz, tl::MemoryPool &m) {
        return m.alloc(sz);
    }


    LcgIpfCodeGen(LilCodeStub* cs, Merced_Code_Emitter& e, LcgIpfContext& c, tl::MemoryPool& m):
        cs(cs),
        emitter(e),
        context(c),
        mem(m),
        iter(cs, true),
        ic(NULL),
        current_alloc(0),
        cur_out_sig(NULL),
        current_predicate(0)
    {
        // emit entry code
        do_entry();

        while (!iter.at_end()) {
            ic = iter.get_context();
            inst = iter.get_current();
            lil_visit_instruction(inst, this);
            iter.goto_next();
        }
    }  // LcgIpfCodeGen (constructor)

};  // class LcgIpfCodeGen


// initialization of static members
// TODO: we have to get rid of memory pool in static area due to initialization problems
static tl::MemoryPool loc_mem;
const LcgIpfLoc* LcgIpfCodeGen::gp = new(loc_mem) LcgIpfLoc(LLK_Gr, 1);
const LcgIpfLoc* LcgIpfCodeGen::sp = new(loc_mem) LcgIpfLoc(LLK_Gr, 12);
const LcgIpfLoc* LcgIpfCodeGen::r0 = new(loc_mem) LcgIpfLoc(LLK_Gr, 0);
const LcgIpfLoc* LcgIpfCodeGen::tp = new(loc_mem) LcgIpfLoc(LLK_Gr, 4);  // thread pointer is in r4
const LcgIpfLoc* LcgIpfCodeGen::tmp_op1 = new(loc_mem) LcgIpfLoc(LLK_Gr, 31);  // r30 and r31 used as temp operand locations
const LcgIpfLoc* LcgIpfCodeGen::tmp_op2 = new(loc_mem) LcgIpfLoc(LLK_Gr, 30);
const LcgIpfLoc* LcgIpfCodeGen::tmp_res = new(loc_mem) LcgIpfLoc(LLK_Gr, 29);
const LcgIpfLoc* LcgIpfCodeGen::tmp_f = new(loc_mem) LcgIpfLoc(LLK_Fr, 6);
const LcgIpfLoc* LcgIpfCodeGen::tmp_addr1 = new(loc_mem) LcgIpfLoc(LLK_Gr, 28);
const LcgIpfLoc* LcgIpfCodeGen::tmp_addr2 = new(loc_mem) LcgIpfLoc(LLK_Gr, 27);
const LcgIpfLoc* LcgIpfCodeGen::tmp_addr3 = new(loc_mem) LcgIpfLoc(LLK_Gr, 26);
const LcgIpfLoc* LcgIpfCodeGen::tmp_addl = new(loc_mem) LcgIpfLoc(LLK_Gr, 3);
const unsigned LcgIpfCodeGen::tmp_pred = 6;  // use p6 (scratch) for any conditional jumps
const unsigned LcgIpfCodeGen::tmp_br = 6; //use BR 6 as a temporary branch address register



LilCodeGeneratorIpf::LilCodeGeneratorIpf()
    : LilCodeGenerator()
{
}

NativeCodePtr LilCodeGeneratorIpf::compile_main(LilCodeStub* cs, size_t* stub_size, PoolManager* code_pool) {

    // start a memory manager
    tl::MemoryPool m;

    // get context info and do a prepass
    LcgIpfContext context(cs, m);

    // initiate an IPF code emitter
    Merced_Code_Emitter emitter(m, 100, context.get_num_targets());
    emitter.memory_type_is_unknown();  // fix later
    emitter.disallow_instruction_exchange();

    LcgIpfCodeGen codegen(cs, emitter, context, m);

    // get the goodies from the emitter
    emitter.flush_buffer();
    *stub_size = emitter.get_size();
    NativeCodePtr buffer = allocate_memory(*stub_size, code_pool);
    emitter.copy((char*)buffer);
    flush_hw_cache((U_8*)buffer, *stub_size);
    sync_i_cache();
    
    return buffer;
}  // compile_main

GenericFunctionPointer lil_npc_to_fp(NativeCodePtr ncp)
{
    void** p = (void**)STD_MALLOC(2*sizeof(void*));
    assert(p);
    p[0] = ncp;
    p[1] = get_vm_gp_value();

    return (GenericFunctionPointer)p;
}  // lil_npc_to_fp
