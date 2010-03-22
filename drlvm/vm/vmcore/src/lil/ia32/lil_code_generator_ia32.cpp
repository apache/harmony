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

#include "lil.h"
#include "lil_code_generator.h"
#include "lil_code_generator_ia32.h"
#include "lil_code_generator_utils.h"
#include "m2n.h"
#include "m2n_ia32_internal.h"
#include "vm_threads.h"
#include "encoder.h"
#include "jit_runtime_support_common.h"

// Strategy:
//   Up to 2 standard places
//   Up to 4 32-bit quantities can be used as locals
//     (i.e. 4 integer locals, or 2 int and 1 long local, or 2 long locals)
//   In, m2n, alloc, and out are on stack
//   Standard places in edx, ecx
//   Returns go where the calling convention requires
//   Locals in ebp, ebx, esi, and edi.

// Things that need fixing:
//   * Arguments are 4 bytes and return always in eax
//   * Temp register is eax, which might stomp on return
//   * Base must be register for st
//   * Index in addresses must be register

// Register allocation:
//
// Stack:
//   (hi) |-------------------------|
//        | args                    |
//        |----------+--------------|
//        |          | return ip    |
//        | m2n (or) |--------------|
//        |          | callee saves |
//        |----------+--------------|
//        | alloced memory          |
//        |-------------------------|
//        | out                     |
//   (lo) |-------------------------|
//
// Return value is in eax, eax/edx, or top of floating point stack depending upon type.
// sp0 is in edx, sp1 is in ecx
//   Locals use ebp, ebx, esi, edi, ecx, and edx, in that order
//     (one of they are 32-bit, 2 if they are 64 bit
//      for example: if l0 is 32 bit, l1 64 bit, and l2 32 bit, they'll use:
//      l0 -> ebp, l1 -> ebx:esi, l2 -> edi)        

LilCodeGeneratorIa32::LilCodeGeneratorIa32()
    : LilCodeGenerator()
{
}

///////////////////////////////////////
// Prepass information

enum LcgIa32OpLocType { LOLT_Stack, LOLT_Reg, LOLT_Immed, LOLT_Tofs };

struct LcgIa32OpLoc {
    LcgIa32OpLocType t;
    union {
        unsigned v;  // Offset for stack, value for immediate
        struct {
            R_Opnd* r1;
            R_Opnd* r2;
        } r; // For register
    } u;
};

// Represents information about the address part of a ld, st, inc, or cas
struct LcgIa32Addr {
    LcgIa32OpLoc base_loc, index_loc;
    bool has_base, has_index;
    R_Opnd* base_reg;
    R_Opnd* index_reg;
    M_Opnd* addr;
};

enum LcgIa32SpecialOp { LSO_None, LSO_Lea };

enum LcgIa32ConGen { LCG_Cmp, LCG_Test, LCG_IsZeroG8 };

struct LcgIa32InstInfo {
    unsigned size_after;
    LilType t;
    LcgIa32OpLoc loc1, loc2, loc3;
    R_Opnd* r1;
    R_Opnd* r2;
    bool mov_to_dst;
    unsigned temp_register;
    union {
        struct {
            bool mov_to_r;
            LcgIa32SpecialOp special;
        } asgn;
        struct {
            LcgIa32ConGen cg;
            bool immed, stack, swap, invert;
            ConditionCode cc;
        } jc;
        unsigned out;
        LcgIa32Addr address;  // used by ld, st, inc
        unsigned pop_m2n;
    } u;
};

struct LcgIa32PrePassInfo {
    unsigned num_is;
    unsigned num_callee_saves;  // How many of the callee saves are used (order is same as m2n frame)
    bool short_jumps;
    size_t size;
    LcgIa32InstInfo* is;
};

// Push arguments left to right or right to left
enum LilArgOrder { LAO_L2r, LAO_R2l };

struct LcgIa32CcInfo {
    LilArgOrder arg_order;
    bool callee_pop;
};

struct LcgIa32Context {
    LcgIa32PrePassInfo* info;
    LilInstructionContext* ctxt;
    LcgIa32CcInfo entry_cc;
    LilSig* entry_sig;
    LcgIa32CcInfo out_cc;
};


///////////////////////////////////////
// Stack layout stuff

static void cc_to_cc_info(LcgIa32CcInfo* info, LilCc cc)
{
    switch (cc) {
    case LCC_Managed:
        info->arg_order = LAO_L2r;
        info->callee_pop = true;
        break;
#ifdef PLATFORM_POSIX
    case LCC_Jni:
#endif
    case LCC_Platform:
        info->arg_order = LAO_R2l;
        info->callee_pop = false;
        break;
#ifndef PLATFORM_POSIX
    case LCC_Jni:
#endif
    case LCC_Rth:
    case LCC_StdCall:
        info->arg_order = LAO_R2l;
        info->callee_pop = true;
        break;
    default: DIE(("Unknown calling convention"));
    }
}

static bool type_in_two_regs(LilType t)
{
    switch (t) {
    case LT_G1:
    case LT_G2:
    case LT_G4:
    case LT_F4:
    case LT_Ref:
    case LT_PInt:
        return false;
    case LT_G8:
    case LT_F8:
        return true;
    default: DIE(("Unexpected LIL type")); for(;;);
    }
}

static unsigned type_number_regs(LilType t)
{
    return (t==LT_Void ? 0 : type_in_two_regs(t) ? 2 : 1);
}

static unsigned type_number_return_registers(LilType t)
{
    switch (t) {
    case LT_Void:
    case LT_F4:
    case LT_F8:
        return 0;
    default:
        return (type_in_two_regs(t) ? 2 : 1);
    }
}

static unsigned type_size_on_stack(LilType t)
{
    switch (t) {
    case LT_G1:
    case LT_G2:
    case LT_G4:
    case LT_F4:
    case LT_Ref:
    case LT_PInt:
        return 4;
    case LT_G8:
    case LT_F8:
        return 8;
    default: DIE(("Unexpected LIL type")); for(;;);
    }
}

static unsigned sig_size_on_stack(LilSig* s)
{
    unsigned size = 0;
    for(unsigned i=0; i<lil_sig_get_num_args(s); i++)
        size += type_size_on_stack(lil_sig_get_arg_type(s, i));
    return size;
}

static unsigned offset_in_sig(LcgIa32CcInfo* cc, LilSig* s, unsigned idx)
{
    unsigned offset = 0, i;
    if (cc->arg_order==LAO_R2l)
        for(i=0; i<idx; i++)
            offset += type_size_on_stack(lil_sig_get_arg_type(s, i));
    else
        for(i=lil_sig_get_num_args(s)-1; i>idx; i--)
            offset += type_size_on_stack(lil_sig_get_arg_type(s, i));
    return offset;
}

static int m2n_base(LcgIa32Context* c)
{
    LilSig* s = lil_ic_get_out_sig(c->ctxt);
    unsigned out = (s ? sig_size_on_stack(s) : 0);
    return lil_ic_get_amt_alloced(c->ctxt) + out;
}

static int in_base(LcgIa32Context* c)
{
    unsigned saved = (lil_ic_get_m2n_state(c->ctxt)!=LMS_NoM2n ? m2n_sizeof_m2n_frame : (1+c->info->num_callee_saves)*4);
    return m2n_base(c) + saved;
}

///////////////////////////////////////
// Operand conversion

// do a pre-prepass to determine the number of registers needed for locals
// this also checks that there are always enough regs for locals,
// std places, and the return
static unsigned get_num_regs_for_locals(LilCodeStub *cs)
{
    LilInstructionIterator iter(cs, true);
    unsigned max_regs = 0;

    while (!iter.at_end()) {
        LilInstructionContext *ic = iter.get_context();
        unsigned n_locals = lil_ic_get_num_locals(ic);
        unsigned n_regs = 0;
        for (unsigned i=0; i<n_locals; i++) {
            LilType t = lil_ic_get_local_type(ic, i);
            n_regs += type_number_regs(t);
        }
        if (n_regs > max_regs)
            max_regs = n_regs;

        LilType rt = lil_ic_get_ret_type(ic);
        unsigned UNUSED n_ret_regs = type_number_return_registers(rt);

        // check that locals, std_places, and return fit
        assert(lil_ic_get_num_std_places(ic) + n_regs + n_ret_regs <= 7);
        assert(lil_ic_get_num_std_places(ic)==0 || n_ret_regs<2);
        iter.goto_next();
    }

    assert(max_regs <= 6);
    return max_regs;
}

// Return the number of bytes of an instruction where loc is the primary operand
static unsigned size_loc(LcgIa32OpLoc* loc)
{
    switch (loc->t) {
    case LOLT_Reg: return 2;
    case LOLT_Stack: return (loc->u.v<124 ? 4 : 7); // 124 is used to work correctly for two words on the stack
    case LOLT_Immed: return 5;
    case LOLT_Tofs: return 8;
    default: DIE(("Unexpected type")); for(;;);
    }
}

// Return the register for the ith local
static R_Opnd* get_local_reg(unsigned i) {
    switch (i) {
    case 0:
        return &ebp_opnd;
    case 1:
        return &ebx_opnd;
    case 2:
        return &esi_opnd;
    case 3:
        return &edi_opnd;
    case 4:
        return &ecx_opnd;
    case 5:
        return &edx_opnd;
    default:
        DIE(("Unexpected index"));
        return NULL;
    }
}

static R_Opnd* get_temp_register(LcgIa32Context* c, unsigned num)
{
    LilType rt = lil_ic_get_ret_type(c->ctxt);
    unsigned n_ret_regs = type_number_return_registers(rt);
    if (n_ret_regs==0)
        if (num==0)
            return &eax_opnd;
        else
            num--;
    unsigned n_std_places = lil_ic_get_num_std_places(c->ctxt);
    unsigned n_locals = 0;
    for (unsigned i=0; i<lil_ic_get_num_locals(c->ctxt); i++) {
        LilType t = lil_ic_get_local_type(c->ctxt, i);
        n_locals += type_number_regs(t);
    }
    if (n_ret_regs<=1 && n_std_places==0 && n_locals<=5)
        if (num==0)
            return &edx_opnd;
        else
            num--;
    if (n_std_places<=1 && n_locals<=4)
        if (num==0)
            return &ecx_opnd;
        else
            num--;
    if (n_std_places==0 && n_locals+num<=3)
        return get_local_reg(3-num);
    DIE(("All the possible cases are supposed to be already covered"));
    return NULL;
}

static void variable_to_location(LcgIa32OpLoc* loc, LcgIa32Context* c, LilVariable* v, LilType t)
{
    bool two = type_in_two_regs(t);
    switch (lil_variable_get_kind(v)) {
    case LVK_In:
        loc->t = LOLT_Stack;
        loc->u.v = in_base(c) + offset_in_sig(&c->entry_cc, c->entry_sig, lil_variable_get_index(v));
        break;
    case LVK_StdPlace:
        assert(!two);
        loc->t = LOLT_Reg;
        switch (lil_variable_get_index(v)) {
        case 0: loc->u.r.r1 = &edx_opnd; break;
        case 1: loc->u.r.r1 = &ecx_opnd; break;
        default: DIE(("Unexpected index"));
        }
        break;
    case LVK_Out:
        loc->t = LOLT_Stack;
        loc->u.v = offset_in_sig(&c->out_cc, lil_ic_get_out_sig(c->ctxt), lil_variable_get_index(v));
        break;
    case LVK_Local:
    {
        loc->t = LOLT_Reg;
        unsigned index = lil_variable_get_index(v);
        // see how many regs are taken up by locals before this one
        unsigned n_regs = 0;
        for (unsigned i=0; i<index; i++) {
            LilType t = lil_ic_get_local_type(c->ctxt, i);
            n_regs += (t == LT_G8 || t == LT_F8) ? 2 : 1;
        }
        loc->u.r.r1 = get_local_reg(n_regs);
        if (two)
            loc->u.r.r2 = get_local_reg(n_regs+1);
        break;
    }
    case LVK_Ret:
        if (t==LT_F4 || t==LT_F8) {
            loc->t = LOLT_Tofs;
        } else {
            loc->t = LOLT_Reg;
            loc->u.r.r1 = &eax_opnd;
            loc->u.r.r2 = &edx_opnd;
        }
        break;
    default: DIE(("Unknown kind"));
    }
}

static void operand_to_location(LcgIa32OpLoc* loc, LcgIa32Context* c, LilOperand* o, LilType t)
{
    if (lil_operand_is_immed(o)) {
        loc->t = LOLT_Immed;
        loc->u.v = lil_operand_get_immed(o);
    } else {
        variable_to_location(loc, c, lil_operand_get_variable(o), t);
    }
}

void convert_addr(tl::MemoryPool* mem, LcgIa32Context* ctxt, LcgIa32Addr* out, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, unsigned* tmp_reg)
{
    out->has_base = (base!=NULL);
    out->has_index = (index!=NULL);
    out->base_reg = NULL;
    out->index_reg = NULL;

    if (base) {
        variable_to_location(&out->base_loc, ctxt, base, LT_PInt);
        if (out->base_loc.t==LOLT_Reg) {
            out->base_reg = out->base_loc.u.r.r1;
        } else {
            out->base_reg = get_temp_register(ctxt, *tmp_reg);
            ++*tmp_reg;
        }
    }

    if (index) {
        assert(base);
        variable_to_location(&out->index_loc, ctxt, index, LT_PInt);
        if (out->base_loc.t==LOLT_Reg) {
            out->index_reg = out->index_loc.u.r.r1;
        } else {
            out->index_reg = get_temp_register(ctxt, *tmp_reg);
            ++*tmp_reg;
        }
    }

    void * const mem_ptr = mem->alloc(sizeof(M_Index_Opnd));
    if (base)
        if (index)
            out->addr = new(mem_ptr) M_Index_Opnd(out->base_reg->reg_no(), out->index_reg->reg_no(), offset, scale);
        else
            out->addr = new(mem_ptr) M_Index_Opnd(out->base_reg->reg_no(), n_reg, offset, 0);
    else
        out->addr = new(mem_ptr) M_Index_Opnd(n_reg, n_reg, offset, 0);
}

unsigned size_addr(LcgIa32Addr* addr)
{
    unsigned size = 5 + 1;  // +1 is for the opcode
    if (addr->has_base && addr->base_loc.t != LOLT_Reg)
        size += size_loc(&addr->base_loc);
    if (addr->has_index && addr->index_loc.t != LOLT_Reg)
        size += size_loc(&addr->index_loc);
    return size;
}

//////////////////////////////////////////////////////////////////////////
// Pre Pass

class LcgIa32IntrPrePass : public LilInstructionVisitor {
public:
    LcgIa32IntrPrePass(tl::MemoryPool* _mem, LilCodeStub* _cs, LcgIa32PrePassInfo* _info)
        : mem(_mem), cs(_cs), info(_info), ii(NULL), rets(0), m2n_ops(0), js(0), jcs(0), tailcalls(0)
    {
        ctxt.info = info;
        ctxt.ctxt = NULL;
        ctxt.entry_sig = lil_cs_get_sig(cs);
        cc_to_cc_info(&ctxt.entry_cc, lil_sig_get_cc(ctxt.entry_sig));
    }

    void update_context(LilInstructionContext* c)
    {
        if (c && lil_ic_get_out_sig(c))
            cc_to_cc_info(&ctxt.out_cc, lil_sig_get_cc(lil_ic_get_out_sig(c)));
        ctxt.ctxt = c;
        if (ii)
            ii++;
        else
            ii = info->is;
        ii->temp_register = 0;
    }

    void label(LilLabel UNREF l) { }
    void locals(unsigned) { }
    void std_places(unsigned) { }

    void alloc(LilVariable* dst, unsigned amt)
    {
        variable_to_location(&ii->loc1, &ctxt, dst, LT_PInt);
        info->size += (amt<=124 ? 3 : 6) + size_loc(&ii->loc1);
    }

    void prepass_dst(LilVariable* dst, LilType t)
    {
        variable_to_location(&ii->loc1, &ctxt, dst, t);
        if (ii->loc1.t==LOLT_Reg) {
            ii->r1 = ii->loc1.u.r.r1;
            ii->r2 = ii->loc1.u.r.r2;
            ii->mov_to_dst = false;
        } else {
            ii->r1 = get_temp_register(&ctxt, ii->temp_register++);
            if (type_in_two_regs(t)) {
                ii->r2 = get_temp_register(&ctxt, ii->temp_register++);
            }
            ii->mov_to_dst = true;
        }
    }

    void asgn(LilVariable* dst, enum LilOperation op, LilOperand* o1, LilOperand* o2)
    {
        // Strategy:
        //   mov o1 -> r
        //   do op on r
        //   mov r -> dst
        // However:
        //   This doesn't work if r==o2, so assert on this for now
        //   For s/zx1/2: can get o1 sign/zero extended into r with a movsz/movzx instruction, so skip mov o1->r
        //   For mov stack<-immed: can do this directly as the op
        //   For add of register & immed: can use lea to get this into r without mov o1->r

        assert(!lil_operation_is_binary(op) || lil_operand_is_immed(o2) || !lil_variable_is_equal(dst, lil_operand_get_variable(o2)));

        if (lil_operation_is_binary(op) && lil_operand_is_immed(o1))
            ii->t = lil_ic_get_type(cs, ctxt.ctxt, o2);
        else
            ii->t = lil_ic_get_type(cs, ctxt.ctxt, o1);
        unsigned num_regs = type_number_regs(ii->t);

        assert(op==LO_Mov || !(ii->t==LT_F4 || ii->t==LT_F8 || ii->t==LT_G8));

        operand_to_location(&ii->loc2, &ctxt, o1, ii->t);
        if (lil_operation_is_binary(op)) {
            operand_to_location(&ii->loc3, &ctxt, o2, ii->t);
        }

        // Determine what r should be and whether the final move is needed
        // This may be overriden below in the case of a move
        prepass_dst(dst, ii->t);

        // Special cases
        if ((op==LO_Add || op==LO_Sub) && ii->loc2.t==LOLT_Reg && ii->loc3.t==LOLT_Immed) {
            // In this case use a lea instruction
            ii->u.asgn.special = LSO_Lea;
            ii->u.asgn.mov_to_r = false;
            int n = (op==LO_Add ? ii->loc3.u.v : -(int)ii->loc3.u.v);
            info->size += (n ? -128<=n && n<128 ? 3 : 6 : 2);
        } else {
            // General case
            ii->u.asgn.special = LSO_None;
            switch (op) {
            case LO_Sx4:
            case LO_Zx4:
                // Treat sx4 & zx4 as a move
            case LO_Mov:
                // Treat immed->stack, the others are done by the movs o1->r->dst
                if (ii->loc1.t==LOLT_Stack && ii->loc2.t==LOLT_Immed) {
                    ii->u.asgn.mov_to_r = false;
                    ii->mov_to_dst = false;
                    info->size += (size_loc(&ii->loc1)+4)*num_regs;
                } else if ((ii->loc1.t==LOLT_Stack || ii->loc1.t==LOLT_Tofs) && ii->loc2.t==LOLT_Reg) {
                    // In this case change r to be o1 then r->dst will do the move
                    ii->r1 = ii->loc2.u.r.r1;
                    ii->r2 = ii->loc2.u.r.r2;
                    ii->u.asgn.mov_to_r = false;
                } else {
                    ii->u.asgn.mov_to_r = (ii->loc2.t!=LOLT_Reg || ii->loc2.u.r.r1!=ii->r1);
                }
                break;
            case LO_SgMul:
                info->size++;
                // Fall through
            case LO_Add:
            case LO_Sub:
            case LO_Shl:
            case LO_And:
                ii->u.asgn.mov_to_r = (ii->loc2.t!=LOLT_Reg || ii->loc2.u.r.r1!=ii->r1);
                info->size += size_loc(&ii->loc3);
                break;
            case LO_Neg:
                ii->u.asgn.mov_to_r = (ii->loc2.t!=LOLT_Reg || ii->loc2.u.r.r1!=ii->r1);
                info->size += 2;
                break;
            case LO_Not:
                DIE(("Unexpected operation"));
            case LO_Sx1:
            case LO_Sx2:
            case LO_Zx1:
            case LO_Zx2:
                ii->u.asgn.mov_to_r = false;
                info->size += 1+size_loc(&ii->loc2);
                break;
            default: DIE(("Unknown operation"));
            }
        }
        if (ii->u.asgn.mov_to_r) info->size += size_loc(&ii->loc2)*num_regs;
        if (ii->mov_to_dst) info->size += size_loc(&ii->loc1)*num_regs;
    }

    void ts(LilVariable* dst)
    {
        prepass_dst(dst, LT_PInt);
        info->size += m2n_ts_to_register_size() + (ii->mov_to_dst ? size_loc(&ii->loc1) : 0);
    }

    void handles(LilOperand* o)
    {
        operand_to_location(&ii->loc2, &ctxt, o, LT_PInt);
        info->size += (ii->loc2.t!=LOLT_Reg ? size_loc(&ii->loc2) : 0);
        info->size += m2n_set_local_handles_size(m2n_base(&ctxt));
    }

    void ld(LilType t, LilVariable* dst, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel, LilLdX)
    { 
        // for the moment, can't load floats
        assert(t != LT_F4 && t != LT_F8 && t != LT_Void);
        prepass_dst(dst, t);
        convert_addr(mem, &ctxt, &ii->u.address, base, scale, index, offset, &ii->temp_register);
        info->size += (t==LT_G8 ? 2 : 1) * size_addr(&ii->u.address);
        if (t == LT_G1 || t == LT_G2) {
            // MOVZX has a 2-byte opcode!
            info->size++;
        }            
        if (ii->mov_to_dst) info->size += (t==LT_G8 ? 2 : 1) * size_loc(&ii->loc1);
    }

    void st(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel, LilOperand* src)
    { 
        assert(t==LT_PInt || t==LT_G4 || t==LT_Ref || t==LT_G2);
        convert_addr(mem, &ctxt, &ii->u.address, base, scale, index, offset, &ii->temp_register);
        info->size += size_addr(&ii->u.address);
        operand_to_location(&ii->loc3, &ctxt, src, t);
        if (ii->loc3.t==LOLT_Immed) info->size += 4;
        if (ii->loc3.t==LOLT_Stack) info->size += size_loc(&ii->loc3);
        if (t==LT_G2) info->size++; // operand size prefix
    }

    void inc(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel)
    {
        assert(t==LT_PInt || t==LT_G4 || t==LT_Ref);
        convert_addr(mem, &ctxt, &ii->u.address, base, scale, index, offset, &ii->temp_register);
        info->size += size_addr(&ii->u.address);
    }

    void cas(LilType t, LilVariable* base, unsigned scale, LilVariable* index, POINTER_SIZE_SINT offset, LilAcqRel,
             LilOperand* cmp, LilOperand* src, LilLabel)
    {
        assert(t==LT_PInt || t==LT_G4 || t==LT_Ref || t==LT_G2);
        // Make sure that ret is not in eax
        assert(type_number_return_registers(lil_ic_get_ret_type(ctxt.ctxt))==0);
        // We need eax for compare, so start the address temp registers after that
        ii->temp_register = 1;
        convert_addr(mem, &ctxt, &ii->u.address, base, scale, index, offset, &ii->temp_register);
        info->size += size_addr(&ii->u.address);
        operand_to_location(&ii->loc2, &ctxt, src, t);
        assert(ii->loc2.t==LOLT_Reg); // Otherwise we have to move it to & from a register
        operand_to_location(&ii->loc3, &ctxt, cmp, t);
        info->size += size_loc(&ii->loc3);
        if (t==LT_G2) info->size++; // operand size prefix
        info->size += 8; // lock cmpxhcg has a 2-byte opcode plus a prefix, plus a long condition jump
    }

    void j(LilLabel)
    {
        info->size += 2;
        js++;
    }

    void jc(enum LilPredicate p, LilOperand* o1, LilOperand* o2, LilLabel)
    {
        LilType t = lil_ic_get_type(cs, ctxt.ctxt, o1);
        assert(t==LT_G4 || t==LT_Ref || t==LT_PInt || t==LT_G1 || t==LT_G2 || (t==LT_G8 && (p==LP_IsZero || p==LP_IsNonzero)));
        bool invert = false;

        operand_to_location(&ii->loc1, &ctxt, o1, t);
        if (lil_predicate_is_binary(p)) {
            operand_to_location(&ii->loc2, &ctxt, o2, t);
            ii->u.jc.cg = LCG_Cmp;
            ii->u.jc.swap = false;
            if (ii->loc1.t==LOLT_Immed) {
                assert(ii->loc2.t!=LOLT_Immed);
                ii->u.jc.immed = true;
                ii->u.jc.stack = (ii->loc2.t==LOLT_Stack);
                ii->u.jc.swap = invert = true;
            } else if (ii->loc2.t==LOLT_Immed) {
                ii->u.jc.immed = true;
                ii->u.jc.stack = (ii->loc1.t==LOLT_Stack);
            } else if (ii->loc1.t==LOLT_Stack) {
                if (ii->loc2.t==LOLT_Stack) {
                    DIE(("Unexpected type"));
                } else {
                    ii->u.jc.immed = false;
                    ii->u.jc.stack = true;
                }
            } else if (ii->loc2.t==LOLT_Stack) {
                ii->u.jc.immed = false;
                ii->u.jc.stack = true;
                ii->u.jc.swap = invert = true;
            } else {
                ii->u.jc.immed = false;
                ii->u.jc.stack = false;
            }
            info->size += 2 + (ii->u.jc.stack ? 4 : 0) + (ii->u.jc.immed ? 4 : 0);
        } else  if (t==LT_G8) {
            ii->u.jc.cg = LCG_IsZeroG8;
            info->size += 2*size_loc(&ii->loc1);
        } else {
            ii->u.jc.cg = LCG_Test;
            if (ii->loc1.t!=LOLT_Reg) info->size += size_loc(&ii->loc1);
            info->size += 2;
        };
        switch (p) {
        case LP_IsZero: ii->u.jc.cc=Condition_Z; break;
        case LP_IsNonzero: ii->u.jc.cc=Condition_NZ; break;
        case LP_Eq: ii->u.jc.cc=Condition_Z; break;
        case LP_Ne: ii->u.jc.cc=Condition_NZ; break;
        case LP_Le: ii->u.jc.cc=Condition_LE; break;
        case LP_Lt: ii->u.jc.cc=Condition_L; break;
        case LP_Ule: ii->u.jc.cc=Condition_BE; break;
        case LP_Ult: ii->u.jc.cc=Condition_B; break;
        default: DIE(("Unknown predicate"));
        }
        if (invert) {
            switch (ii->u.jc.cc) {
            case Condition_L: ii->u.jc.cc = Condition_NL; break;
            case Condition_LE: ii->u.jc.cc = Condition_NLE; break;
            case Condition_B: ii->u.jc.cc = Condition_NB; break;
            case Condition_BE: ii->u.jc.cc = Condition_NBE; break;
            default:;
            }
        }
        info->size += 2;
        jcs++;
    }

    void out(LilSig* s)
    {
        ii->u.out = sig_size_on_stack(s);
        if (ii->u.out)
            info->size += 2 + (ii->u.out<128 ? 1 : 4);
    }

    void in2out(LilSig* s)
    {
        unsigned num = sig_size_on_stack(s);
        unsigned npushs = num/4;
        if (2*num+in_base(&ctxt) < 128)
            info->size += 4*npushs;
        else
            info->size += 7*npushs;
    }

    void call(LilOperand* o, LilCallKind k)
    {
        operand_to_location(&ii->loc1, &ctxt, o, LT_PInt);
        switch (k) {
        case LCK_Call:{
            info->size += size_loc(&ii->loc1);
            if (!ctxt.out_cc.callee_pop) {
                unsigned num = sig_size_on_stack(lil_ic_get_out_sig(ctxt.ctxt));
                if (num) info->size += (num<128 ? 3 : 6);
            }
            break;}
        case LCK_CallNoRet:
            info->size += size_loc(&ii->loc1);
            break;
        case LCK_TailCall:
            tailcalls++;
            info->size += size_loc(&ii->loc1);
            break;
        default: DIE(("Unexpected call kind"));
        }
    }

    void ret()
    {
        if (lil_ic_get_out_sig(ctxt.ctxt) || lil_ic_get_amt_alloced(ctxt.ctxt))
            info->size += 6;
        rets++;
    }

    void push_m2n(Method_Handle UNREF method, frame_type UNREF current_frame_type, bool handles)
    {
        m2n_ops++;
        info->size += m2n_push_m2n_size(handles, 4);
    }

    void m2n_save_all()
    {
        // This is a nop on IA32
    }

    void pop_m2n()
    {
        m2n_ops++;
        unsigned num = m2n_base(&ctxt);
        switch (lil_ic_get_ret_type(ctxt.ctxt)) {
        case LT_Void:
        case LT_F4:
        case LT_F8:
            ii->u.pop_m2n = 0;
            break;
        case LT_G8:
            ii->u.pop_m2n = 2;
            break;
        case LT_G1:
        case LT_G2:
        case LT_G4:
        case LT_Ref:
        case LT_PInt:
            ii->u.pop_m2n = 1;
            break;
        default: DIE(("Unknown LIL type"));
        }
        info->size += m2n_pop_m2n_size(lil_ic_get_m2n_state(ctxt.ctxt)==LMS_Handles, 4, num, ii->u.pop_m2n);
    }

    void print(char *, LilOperand *) {
        // not implemented on ia32
    }

    void after_inst()
    {
        ii->size_after = info->size;
    }

    void finalise_size()
    {
        LcgIa32CcInfo entry_cc_info;
        cc_to_cc_info(&entry_cc_info, lil_sig_get_cc(lil_cs_get_sig(cs)));
        unsigned num = sig_size_on_stack(lil_cs_get_sig(cs));
        unsigned ret_size = (entry_cc_info.callee_pop && num ? 3 : 1);
        ret_size += info->num_callee_saves;  // need this many pop insts at every ret
        info->size += ret_size*rets;
        info->size += info->num_callee_saves*tailcalls; // tailcalls need the same as rets
        info->size += (4-info->num_callee_saves)*m2n_ops; // this many push/pops at push_m2n and pop_m2n
        info->size += info->num_callee_saves; // this many pushes at the start
        info->short_jumps = (info->size < 128);
        if (!info->short_jumps) info->size += 3*js+4*jcs;
    }

private:
    tl::MemoryPool* mem;
    LilCodeStub* cs;
    LcgIa32PrePassInfo* info;
    LcgIa32Context ctxt;
    LcgIa32InstInfo* ii;
    unsigned rets, m2n_ops, js, jcs, tailcalls;
};

static size_t pre_pass(LilCodeStub* cs, tl::MemoryPool* mem, LcgIa32PrePassInfo** data)
{
    LcgIa32PrePassInfo* info = (LcgIa32PrePassInfo*)mem->alloc(sizeof(LcgIa32PrePassInfo));
    info->num_is = lil_cs_get_num_instructions(cs);
    info->size = 0;
    info->num_callee_saves = get_num_regs_for_locals(cs);
    info->is = (LcgIa32InstInfo*)mem->alloc(info->num_is*sizeof(LcgIa32InstInfo));

    LilInstructionIterator iter(cs, true);
    LcgIa32IntrPrePass ppv(mem, cs, info);
    while(!iter.at_end()) {
        LilInstruction* i = iter.get_current();
        ppv.update_context(iter.get_context());
        lil_visit_instruction(i, &ppv);
        ppv.after_inst();
        iter.goto_next();
    }

    ppv.finalise_size();

    *data = info;
    return info->size;
}

//////////////////////////////////////////////////////////////////////////
// Movement

static R_Opnd* move_location_to_a_register(char** buf, LcgIa32Context* c, LcgIa32OpLoc* loc, unsigned* temp_reg)
{
    switch (loc->t) {
    case LOLT_Reg:
        return loc->u.r.r1;
        // break; // remark #111: statement is unreachable
    case LOLT_Stack:{
        R_Opnd* tmp = get_temp_register(c, *temp_reg);
        ++*temp_reg;
        *buf = mov(*buf, *tmp, M_Base_Opnd(esp_reg, loc->u.v));
        return tmp;}
    case LOLT_Immed: {
        R_Opnd* tmp = get_temp_register(c, *temp_reg);
        ++*temp_reg;
        *buf = mov(*buf, *tmp, Imm_Opnd(loc->u.v));
        return tmp;}
    default: DIE(("Unknown type")); for(;;);
    }
}

static void move_location_to_register(char** buf, R_Opnd* reg1, R_Opnd* reg2, LcgIa32OpLoc* loc, LilType t)
{
    bool two = type_in_two_regs(t);
    switch (loc->t) {
    case LOLT_Reg:
        if (loc->u.r.r1==reg1) return;
        *buf = mov(*buf, *reg1, *(loc->u.r.r1));
        if (two) *buf = mov(*buf, *reg2, *(loc->u.r.r2));
        break;
    case LOLT_Stack:{
        *buf = mov(*buf, *reg1, M_Base_Opnd(esp_reg, loc->u.v));
        if (two) {
            *buf = mov(*buf, *reg2, M_Base_Opnd(esp_reg, loc->u.v+4));
        }
        break;}
    case LOLT_Immed: {
        *buf = mov(*buf, *reg1, Imm_Opnd(loc->u.v));
        if (two) {
            *buf = mov(*buf, *reg2, Imm_Opnd(0));
        }
        break;}
    case LOLT_Tofs:{
        *buf = alu(*buf, sub_opc, esp_opnd, Imm_Opnd(t==LT_F4 ? 4 : 8));
        *buf = fst(*buf, M_Base_Opnd(esp_reg, 0), (t==LT_F8), 1);
        *buf = pop(*buf, *reg1);
        if (t==LT_F8) *buf = pop(*buf, *reg2);
        break;}
    default: DIE(("Unknown type"));
    }
}

static void move_register_to_location(char** buf, LcgIa32OpLoc* loc, R_Opnd* reg1, R_Opnd* reg2, LilType t)
{
    bool two = type_in_two_regs(t);
    switch (loc->t) {
    case LOLT_Reg:
        *buf = mov(*buf, *(loc->u.r.r1), *reg1);
        if (two) *buf = mov(*buf, *(loc->u.r.r2), *reg2);
        break;
    case LOLT_Stack:{
        *buf = mov(*buf, M_Base_Opnd(esp_reg, loc->u.v), *reg1);
        if (two) {
            *buf = mov(*buf, M_Base_Opnd(esp_reg, loc->u.v+4), *reg2);
        }
        break;}
    case LOLT_Tofs:{
        if (t==LT_F8) *buf = push(*buf, *reg2);
        *buf = push(*buf, *reg1);
        *buf = fld(*buf, M_Base_Opnd(esp_reg, 0), (t==LT_F8));
        *buf = alu(*buf, add_opc, esp_opnd, Imm_Opnd(t==LT_F4 ? 4 : 8));
        break;}
    default: DIE(("Unknown type"));
    }
}

static char* addr_emit_moves(char* buf, LcgIa32Addr* addr)
{
    if (addr->has_base && addr->base_loc.t!=LOLT_Reg)
        move_location_to_register(&buf, addr->base_reg, NULL, &addr->base_loc, LT_PInt);
    if (addr->has_index && addr->index_loc.t!=LOLT_Reg)
        move_location_to_register(&buf, addr->index_reg, NULL, &addr->index_loc, LT_PInt);
    return buf;
}

static Opnd_Size type_to_opnd_size(LilType t)
{
    switch (t) {
    case LT_G2:
        return size_16;
        //break;// remark #111: statement is unreachable
    case LT_G4:
    case LT_PInt:
    case LT_Ref:
        return size_32;
        //break;// remark #111: statement is unreachable
    default:
        DIE(("Unknown LIL type")); for(;;);
    }
}

//////////////////////////////////////////////////////////////////////////
// Main Pass

class LcgIa32IntrCodeGen : public LilInstructionVisitor {
public:
    LcgIa32IntrCodeGen(tl::MemoryPool* _mem, LilCodeStub* cs, char** _buf, LcgIa32PrePassInfo* info)
        : mem(_mem), buf(_buf), la_tab(_mem, 0), ii(NULL)
    {
        ctxt.info = info;
        ctxt.ctxt = NULL;
        ctxt.entry_sig = lil_cs_get_sig(cs);
        cc_to_cc_info(&ctxt.entry_cc, lil_sig_get_cc(ctxt.entry_sig));
    }

    void update_context(LilInstructionContext* c)
    {
        if (c && lil_ic_get_out_sig(c))
            cc_to_cc_info(&ctxt.out_cc, lil_sig_get_cc(lil_ic_get_out_sig(c)));
        ctxt.ctxt = c;
        if (ii)
            ii++;
        else
            ii = ctxt.info->is;
    }

    void label(LilLabel l)
    {
        la_tab.define_label(l, *buf, false);
    }

    void locals(unsigned UNREF num)
    {
        // nothing to do; everything is taken care of by get_num_regs_for_locals
    }

    void std_places(unsigned UNREF num)
    {
        // nothing to do; everything is taken care of by get_num_regs_for_locals        
    }

    void alloc(LilVariable* UNREF dst, unsigned amt)
    {
        // Keep the stack 4-byte aligned
        Imm_Opnd imm((amt+3)&~3);
        *buf = alu(*buf, sub_opc, esp_opnd, imm);
        move_register_to_location(buf, &ii->loc1, &esp_opnd, NULL, LT_PInt);
    }

    void asgn(LilVariable* UNREF dst, LilOperation op, LilOperand* UNREF o1, LilOperand* UNREF o2)
    {
        // Move o1 to register r
        if (ii->u.asgn.mov_to_r)
            move_location_to_register(buf, ii->r1, ii->r2, &ii->loc2, ii->t);

        // Do operation
        switch (ii->u.asgn.special) {
        case LSO_None:
            ALU_Opcode opc;
            bool sign;
            Opnd_Size sz;
            switch (op) {
            case LO_Sx4:
            case LO_Zx4:
                // Treat sx4 & zx4 as a move
            case LO_Mov:
                // Treat immed->stack specially, the others are done by the movs o1->r->dst
                if (ii->loc1.t==LOLT_Stack && ii->loc2.t==LOLT_Immed) {
                    *buf = mov(*buf, M_Base_Opnd(esp_reg, ii->loc1.u.v), Imm_Opnd(ii->loc2.u.v));
                    if (type_in_two_regs(ii->t)) {
                        *buf = mov(*buf, M_Base_Opnd(esp_reg, ii->loc1.u.v+4), Imm_Opnd(0));
                    }
                }
                break;
            case LO_Add:
                opc = add_opc; goto alu;
            case LO_Sub:
                opc = sub_opc; goto alu;
            case LO_SgMul:
                switch (ii->loc3.t) {
                case LOLT_Reg:
                    *buf = imul(*buf, *(ii->r1), *(ii->loc3.u.r.r1));
                    break;
                case LOLT_Stack:{
                    *buf = imul(*buf, *(ii->r1), M_Base_Opnd(esp_reg, ii->loc3.u.v));
                    break;}
                case LOLT_Immed:{
                    *buf = imul(*buf, *(ii->r1), Imm_Opnd(ii->loc3.u.v));
                    break;}
                default: DIE(("Unexpected type"));
                }            
                break;
            case LO_Neg:
                *buf = neg(*buf, *(ii->r1));
                break;
            case LO_Shl:
                switch (ii->loc3.t) {
                case LOLT_Reg:
                case LOLT_Stack:
                    DIE(("Unexpected type"));
                case LOLT_Immed:{
                    *buf = shift(*buf, shl_opc, *(ii->r1), Imm_Opnd(ii->loc3.u.v));
                    break;}
                default: DIE(("Unexpected type"));
                }
                break;
            case LO_And:
                opc = and_opc; goto alu;
            case LO_Not:
                DIE(("Unexpected operation"));
            case LO_Sx1: sign=true; sz = size_8; goto widden;
            case LO_Sx2: sign=true; sz = size_16; goto widden;
            case LO_Zx1: sign=false; sz = size_8; goto widden;
            case LO_Zx2: sign=false; sz = size_16; goto widden;
            alu:
                switch (ii->loc3.t) {
                case LOLT_Reg:
                    *buf = alu(*buf, opc, *(ii->r1), *(ii->loc3.u.r.r1));
                    break;
                case LOLT_Stack:{
                    *buf = alu(*buf, opc, *(ii->r1), M_Base_Opnd(esp_reg, ii->loc3.u.v));
                    break;}
                case LOLT_Immed:{
                    *buf = alu(*buf, opc, *(ii->r1), Imm_Opnd(ii->loc3.u.v));
                    break;}
                default: DIE(("Unexpected type"));
                }            
                break;
            widden:
                switch (ii->loc2.t) {
                case LOLT_Reg:
                    // You cannot widen ebp, esp, esi, or edi.
                    assert(ii->loc2.u.r.r1==&eax_opnd || ii->loc2.u.r.r1==&ebx_opnd);
                    if (sign) {
                        *buf = movsx(*buf, *(ii->r1), *(ii->loc2.u.r.r1), sz);
                    } else {
                        *buf = movzx(*buf, *(ii->r1), *(ii->loc2.u.r.r1), sz);
                    }
                    break;
                case LOLT_Stack:{
                    if (sign) {
                        *buf = movsx(*buf, *(ii->r1), M_Base_Opnd(esp_reg, ii->loc2.u.v), sz);
                    } else {
                        *buf = movzx(*buf, *(ii->r1), M_Base_Opnd(esp_reg, ii->loc2.u.v), sz);
                    }
                    break;}
                default: DIE(("Unexpected type"));
                }
                break;
            default: DIE(("Unexpected operartion"));
            }
            break;
        case LSO_Lea:{
            M_Base_Opnd m(ii->loc2.u.r.r1->reg_no(), (op==LO_Add ? ii->loc3.u.v : -(int)ii->loc3.u.v));
            *buf = lea(*buf, *(ii->r1), m);
            break;}
        }

        // Move register r to dst
        if (ii->mov_to_dst)
            move_register_to_location(buf, &ii->loc1, ii->r1, ii->r2, ii->t);
    }

    void ts(LilVariable* UNREF v)
    {
        *buf = m2n_gen_ts_to_register(*buf, ii->r1);
        if (ii->mov_to_dst)
            move_register_to_location(buf, &ii->loc1, ii->r1, ii->r2, LT_PInt);
    }

    void handles(LilOperand*)
    {
        R_Opnd* r = move_location_to_a_register(buf, &ctxt, &ii->loc2, &ii->temp_register);
        *buf = m2n_gen_set_local_handles(*buf, m2n_base(&ctxt), r);
    }

    void ld(LilType t, LilVariable*, LilVariable* UNREF base, unsigned UNREF scale, LilVariable* UNREF index, POINTER_SIZE_SINT UNREF offset, LilAcqRel, LilLdX ext)
    {
        *buf = addr_emit_moves(*buf, &ii->u.address);
        switch(t) {
        case LT_G1:
        case LT_G2:
            if (ext == LLX_Sign) {
                *buf = movsx(*buf, *(ii->r1), *(ii->u.address.addr), t == LT_G1 ? size_8 : size_16);
            } else {
                *buf = movzx(*buf, *(ii->r1), *(ii->u.address.addr), t == LT_G1 ? size_8 : size_16);
            }
            if (ext!=LLX_None) t = LT_PInt;
            break;
        case LT_G4:
        case LT_PInt:
        case LT_Ref:
            *buf = mov(*buf, *(ii->r1), *(ii->u.address.addr));
            break;
        case LT_G8:
            DIE(("Unexpected type"));
            //ASSERT(0 == 1, "Need proper implementation");
            //*buf = mov(*buf, *(ii->r1), *(ii->u.address.addr));
            // Bit of a hack to change this value, but it works
            //ii->u.address.addr->disp.value += 4;
            //*buf = mov(*buf, *(ii->r2), *(ii->u.address.addr));
            break;
        default:
            DIE(("Unexpected LIL type")); // other types not allowed
        }
        if (ii->mov_to_dst) move_register_to_location(buf, &ii->loc1, ii->r1, ii->r2, t);
    }

    void st(LilType t, LilVariable* UNREF base, unsigned UNREF scale, LilVariable* UNREF index, POINTER_SIZE_SINT UNREF offset, LilAcqRel, LilOperand*)
    {
        *buf = addr_emit_moves(*buf, &ii->u.address);
        if (ii->loc3.t==LOLT_Immed) {
            Imm_Opnd imm(ii->loc3.u.v);
            *buf = mov(*buf, *(ii->u.address.addr), imm, type_to_opnd_size(t));
        } else {
            R_Opnd* r = move_location_to_a_register(buf, &ctxt, &ii->loc3, &ii->temp_register);
            *buf = mov(*buf, *(ii->u.address.addr), *r, type_to_opnd_size(t));
        }
    }

    void inc(LilType UNREF t, LilVariable* UNREF base, unsigned UNREF scale, LilVariable* UNREF index, POINTER_SIZE_SINT UNREF offset, LilAcqRel)
    {
        *buf = addr_emit_moves(*buf, &ii->u.address);
        *buf = ::inc(*buf, *(ii->u.address.addr));
    }

    void cas(LilType t, LilVariable* UNREF base, unsigned UNREF scale, LilVariable* UNREF index, POINTER_SIZE_SINT UNREF offset, LilAcqRel,
             LilOperand* UNREF cmp, LilOperand* UNREF src, LilLabel l)
    {
        *buf = addr_emit_moves(*buf, &ii->u.address);
        R_Opnd* r = move_location_to_a_register(buf, &ctxt, &ii->loc2, &ii->temp_register);
        move_location_to_register(buf, &eax_opnd, NULL, &ii->loc3, t);
        *buf = prefix(*buf, lock_prefix);
        *buf = cmpxchg(*buf, *(ii->u.address.addr), *r, type_to_opnd_size(t));       
        *buf = branch32(*buf, Condition_NZ, Imm_Opnd(-4));
        la_tab.add_patch_to_label(l, *buf-4, LPT_Rel32);
    }

    void j(LilLabel l)
    {
        if (ctxt.info->short_jumps) {
            Imm_Opnd imm(size_8, -1);
            *buf = jump8(*buf, imm);
            la_tab.add_patch_to_label(l, *buf-1, LPT_Rel8);
        } else {
            Imm_Opnd imm(size_32, -4);
            *buf = jump32(*buf, imm);
            la_tab.add_patch_to_label(l, *buf-4, LPT_Rel32);
        }
    }

    void jc(LilPredicate, LilOperand*, LilOperand*, LilLabel l)
    {
        if (ii->u.jc.cg==LCG_Cmp) {
            if (ii->u.jc.immed) {
                Imm_Opnd imm(ii->u.jc.swap ? ii->loc1.u.v : ii->loc2.u.v);
                if (ii->u.jc.stack) {
                    M_Base_Opnd m(esp_reg, (ii->u.jc.swap ? ii->loc2.u.v : ii->loc1.u.v));
                    *buf = alu(*buf, cmp_opc, m, imm);
                } else {
                    *buf = alu(*buf, cmp_opc, *(ii->u.jc.swap ? ii->loc2.u.r.r1 : ii->loc1.u.r.r1), imm);
                }
            } else {
                if (ii->u.jc.stack) {
                    M_Base_Opnd m(esp_reg, (ii->u.jc.swap ? ii->loc2.u.v : ii->loc1.u.v));
                    *buf = alu(*buf, cmp_opc, m, *(ii->u.jc.swap ? ii->loc1.u.r.r1 : ii->loc2.u.r.r1));
                } else {
                    *buf = alu(*buf, cmp_opc, *(ii->loc1.u.r.r1), *(ii->loc2.u.r.r1));
                }
            }
        } else if (ii->u.jc.cg==LCG_Test) {
            R_Opnd* reg = move_location_to_a_register(buf, &ctxt, &ii->loc1, &ii->temp_register);
            *buf = test(*buf, *reg, *reg);
        } else if (ii->u.jc.cg==LCG_IsZeroG8) {
            R_Opnd* reg = get_temp_register(&ctxt, ii->temp_register++);
            move_location_to_register(buf, reg, NULL, &ii->loc1, LT_PInt);
            switch (ii->loc1.t) {
            case LOLT_Reg:
                *buf = alu(*buf, or_opc, *reg, *(ii->loc1.u.r.r2));
                break;
            case LOLT_Stack:{
                *buf = alu(*buf, or_opc, *reg, M_Base_Opnd(esp_reg, ii->loc1.u.v+4));
                break;}
            default:
                DIE(("Unexpected type"));
            }
        }
        if (ctxt.info->short_jumps) {
            *buf = branch8(*buf, ii->u.jc.cc, Imm_Opnd(size_8, -1));
            la_tab.add_patch_to_label(l, *buf-1, LPT_Rel8);
        } else {
            *buf = branch32(*buf, ii->u.jc.cc, Imm_Opnd(size_32, -4));
            la_tab.add_patch_to_label(l, *buf-4, LPT_Rel32);
        }
    }
    
    void out(LilSig*)
    {
        if (ii->u.out) {
            *buf = alu(*buf, sub_opc, esp_opnd, Imm_Opnd(ii->u.out));
        }
    }

    void in2out(LilSig* s)
    {
        LcgIa32CcInfo out_cc;
        cc_to_cc_info(&out_cc, lil_sig_get_cc(s));
        bool l2r = out_cc.arg_order==LAO_L2r;
        unsigned nargs = lil_sig_get_num_args(s);
        unsigned extra = 0, base = in_base(&ctxt);
        unsigned i = (l2r ? 0 : nargs);
        while ((l2r && i<nargs) || (!l2r && i>0)) {
            if (!l2r) i--;
            unsigned arg_base = extra+base+offset_in_sig(&ctxt.entry_cc, ctxt.entry_sig, i);
            switch (type_size_on_stack(lil_sig_get_arg_type(s, i))) {
            case 4:{
                *buf = push(*buf, M_Base_Opnd(esp_reg, arg_base));
                extra += 4;
                break;}
            case 8:{
                M_Base_Opnd m1(esp_reg, arg_base+4);  // +4 because of the first push
                M_Base_Opnd m2(esp_reg, arg_base+4);
                *buf = push(*buf, m2);
                *buf = push(*buf, m1);
                extra += 8;
                break;}
            default: DIE(("Unexpected type size"));
            }
            if (l2r) i++;
        }
    }

    void do_call(LcgIa32OpLoc* l)
    {
        switch (l->t) {
        case LOLT_Reg:
            *buf = ::call(*buf, *(l->u.r.r1));
            break;
        case LOLT_Stack:{
            *buf = ::call(*buf, M_Base_Opnd(esp_reg, l->u.v));
            break;}
        case LOLT_Immed:
            *buf = ::call(*buf, (char*)l->u.v);
            break;
        default: DIE(("Unexpected type"));
        }
    }

    void call(LilOperand* UNREF o, LilCallKind k)
    {
        switch (k) {
        case LCK_Call:
            // Do call
            do_call(&ii->loc1);
            // Argument pop if necessary
            if (!ctxt.out_cc.callee_pop) {
                unsigned num_bytes = sig_size_on_stack(lil_ic_get_out_sig(ctxt.ctxt));
                if (num_bytes) {
                    *buf = alu(*buf, add_opc, esp_opnd, Imm_Opnd(num_bytes));
                }
            }
            break;
        case LCK_CallNoRet:
            do_call(&ii->loc1);
            break;
        case LCK_TailCall:
            adjust_stack_for_return();
            switch (ii->loc1.t) {
            case LOLT_Reg:
                *buf = jump(*buf, *(ii->loc1.u.r.r1));
                break;
            case LOLT_Stack:{
                *buf = jump(*buf, M_Base_Opnd(esp_reg, ii->loc1.u.v));
                break;}
            case LOLT_Immed:
                *buf = jump(*buf, (char*)ii->loc1.u.v);
                break;
            default: DIE(("Unexpected type"));
            }
            break;
        default: DIE(("Unexpected call kind"));
        }
    }

    void adjust_stack_to_callee_saved()
    {
        unsigned num_bytes = m2n_base(&ctxt);
        if (num_bytes) {
            *buf = alu(*buf, add_opc, esp_opnd, Imm_Opnd(num_bytes));
        }
    }

    void adjust_stack_for_return()
    {
        assert(lil_ic_get_m2n_state(ctxt.ctxt)==LMS_NoM2n);
        adjust_stack_to_callee_saved();
        if (ctxt.info->num_callee_saves>=4) *buf = pop(*buf, edi_opnd);
        if (ctxt.info->num_callee_saves>=3) *buf = pop(*buf, esi_opnd);
        if (ctxt.info->num_callee_saves>=2) *buf = pop(*buf, ebx_opnd);
        if (ctxt.info->num_callee_saves>=1) *buf = pop(*buf, ebp_opnd);
    }

    void ret()
    {
        adjust_stack_for_return();
        unsigned sz = sig_size_on_stack(ctxt.entry_sig);
        LilCc cc = lil_sig_get_cc(ctxt.entry_sig);
        LilType type = lil_sig_get_ret_type(ctxt.entry_sig);
        
        if (cc == LCC_Managed && (type == LT_F4 || type == LT_F8)) {
            // Managed calling convention uses XMM to return floating point values on IA32.
            // So we need to copy return value from FPU stack to XMM.
            int disp = (type == LT_F8) ? -8 : -4;
            M_Opnd memloc(esp_reg, disp);
            *buf = fst(*buf, memloc, (type == LT_F8), true);
            *buf = sse_mov(*buf, xmm0_reg, memloc, (type == LT_F8));
        }
        
        if (ctxt.entry_cc.callee_pop) {
            if (cc == LCC_Managed) {
                // Managed calling convention assumes callee responsibility to 
                // handle alignment properly. Assuming that arguments were aligned, 
                // size of input arguments plus return pointer on the stack also should be aligned
                sz += sizeof(POINTER_SIZE_INT);
                sz = (sz + (MANAGED_STACK_ALIGNMENT - 1)) & ~(MANAGED_STACK_ALIGNMENT - 1);
                sz -= sizeof(POINTER_SIZE_INT);
            }
            if (sz != 0) {
                *buf = ::ret(*buf, Imm_Opnd(sz));
                return;
            }
        }
        *buf = ::ret(*buf);
    }

    void push_m2n(Method_Handle method, frame_type current_frame_type, bool handles)
    {
        adjust_stack_to_callee_saved();
        *buf = m2n_gen_push_m2n(*buf, method, current_frame_type, handles, ctxt.info->num_callee_saves);
    }

    void m2n_save_all()
    {
        // This is a nop on IA32
    }

    void pop_m2n()
    {
        *buf = m2n_gen_pop_m2n(*buf, lil_ic_get_m2n_state(ctxt.ctxt)==LMS_Handles, ctxt.info->num_callee_saves, m2n_base(&ctxt), ii->u.pop_m2n);
    }

    void print(char *, LilOperand *) {
    // not implemented on ia32
    }

    tl::MemoryPool* mem;
    char** buf;
    LilCguLabelAddresses la_tab;
    LcgIa32Context ctxt;
    LcgIa32InstInfo* ii;
};


// a helper function, called by compile() and get_stub_size()
// previously a part of the LilCodeGeneratorIa32 interface
static void main_pass(LilCodeStub* cs, tl::MemoryPool* mem, NativeCodePtr _buf,
                      LcgIa32PrePassInfo* info, size_t * stub_size)
{
    char* buf = (char*)_buf;
    LilCguLabelAddresses la_tab(mem, 0);

    if (info->num_callee_saves>=1) buf = push(buf, ebp_opnd);
    if (info->num_callee_saves>=2) buf = push(buf, ebx_opnd);
    if (info->num_callee_saves>=3) buf = push(buf, esi_opnd);
    if (info->num_callee_saves>=4) buf = push(buf, edi_opnd);

    LilInstructionIterator iter(cs, true);
    LcgIa32IntrCodeGen cgv(mem, cs, &buf, info);
    while(!iter.at_end()) {
        LilInstruction* i = iter.get_current();
        cgv.update_context(iter.get_context());
        lil_visit_instruction(i, &cgv);
        iter.goto_next();
    }

    assert((char *)_buf+info->size >= buf);
    *stub_size = buf - (char *)_buf;
}


NativeCodePtr LilCodeGeneratorIa32::compile_main(LilCodeStub* cs, size_t* stub_size, PoolManager* code_pool)
{
    LcgIa32PrePassInfo* data;
    tl::MemoryPool mem;
    size_t size = pre_pass(cs, &mem, &data);
    NativeCodePtr buf = allocate_memory(size, code_pool);
    main_pass(cs, &mem, buf, data, stub_size);
    return buf;
}

GenericFunctionPointer lil_npc_to_fp(NativeCodePtr p)
{
    return (GenericFunctionPointer)p;
}
