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
 * @brief Encoder class and related definitions.
 */
 
#if !defined(__ENC_H_INCLUDED__)
#define __ENC_H_INCLUDED__

#include "jdefs.h"
#include <assert.h>
#include <stdarg.h>

#include <bitset>
#include <string>
#include <map>
#include <algorithm>

using std::string;
using std::bitset;
using std::map;
using std::max;

/**
 * @brief Size of stack slot affected by a regular PUSH/POP instruction.
 *
 * Normally equals to the size of general-purpose register of the platform.
 */
#define STACK_SLOT_SIZE (sizeof(void*))

/**
 * @brief Rounds given \c bytes to the integer number of stack slots.
 */
#define STACK_SIZE(bytes) \
                    ((bytes + (STACK_SLOT_SIZE-1)) & ~(STACK_SLOT_SIZE-1))

namespace Jitrino {
namespace Jet {

class CallSig;
class Encoder;
/**
 * @brief A signed integer type, with the same size as a pointer.
 */
typedef POINTER_SIZE_SINT int_ptr;
typedef POINTER_SIZE_INT uint_ptr;

/**
 * @brief A dynamically grown byte array.
 * 
 * Class CodeStream represents a dynamically growing byte array, which 
 * always provides buffer of at least minimal guaranteed size (which is 
 * #BUFFER_ZONE).
 *
 * The usage is as follows:
 * @code
 *      CodeStream cs;
 *      cs.init(INITIALLY_EXPECTED_CODE_SIZE);
 *      unsigned p_start = cs.ipoff();
 *      char * p = cs.ip();
 *      memcpy(p, otherP, someSize_less_than_BUFFER_ZONE);
 *      cs.ip(p + someSize_less_than_BUFFER_ZONE);
 *      ...
 *      unsigned next_p = cs.ipoff();
 * @endcode
 */
class CodeStream {
public:
    CodeStream()    { m_buf = NULL; m_size = 0; }
    ~CodeStream()   { if (m_buf) { free(m_buf); } };

    /**
     * @brief Performs initial memory allocation. 
     *
     * The memory size allocated is 'bytes' and then grow when necessary.
     */
    void init(unsigned bytes)
    {
        resize(bytes);
    }


    /**
     * @brief Returns address of a next available for writing byte.
     *
     * The address returned is guaranteed to contain at least #BUFFER_ZONE 
     * bytes. 
     * The returned address is valid only until the next call to #ip(void) 
     * where a memory reallocation can be triggered, and thus should not be stored between 
     * such calls. Use ipoff() instead which is consistent during the lifetime of 
     * CodeStream object.
     */
    char *  ip(void)
    {
        return m_buf + m_size;
    }

    char * ip(unsigned ipoff)
    {
        return data() + ipoff;
    }

    /**
     * Sets current address. This must be an address of a next available byte.
     */
    void    ip(char * _ip)
    {
        assert((U_32)(_ip - m_buf) == (uint64)(_ip - m_buf));
        m_size = (U_32)(_ip - m_buf);
        assert(m_size < total_size);
        // Need to be done here, and not in ip(void).
        // Otherwise, the following usage template:
        //  patch(pid, m_codeStream.data() + br_ipoff, ip()) 
        // may fail, if ip(void) triggers reallocation
        if ((total_size - m_size) < BUFFER_ZONE) {
            resize(total_size + 
                   max((unsigned)BUFFER_ZONE, total_size*GROW_RATE/100));
        }
    }

    /**
     * Returns an offset the next available byte in the stream.
     */
    unsigned    ipoff(void) const
    {
        return m_size;
    }

    /**
     * Returns the size used in the stream.
     */
    unsigned    size(void) const
    {
        return m_size;
    }

    /**
     * Provides a direct access to internal buffer. Never use more than 
     * size() bytes.
     */
    char *  data(void) const
    {
        return m_buf;
    }

    /**
     * The minimum guaranteed size of the buffer returned by ip().
     * This is also a minimal size of the buffer which triggers 
     * reallocation of a bigger memory buf. '16' here is max size of the 
     * native instruction (at least on IA32/EM64T). 3 was chosen empirically.
     */
    enum { BUFFER_ZONE = 16*3 };
private:
    /**
     * Perform the [re-]allocation of a memory.
     * The previously filled memory (if any) is copied into the newly allocated buffer.
     */
    void resize(unsigned how_much)
    {
        total_size = max(how_much, (unsigned)BUFFER_ZONE);
        m_buf = (char*)realloc(m_buf, total_size);
    }

    /**
     * A pointer to the allocated buffer.
     */
    char *      m_buf;

    /**
     * A size of the buffer allocated.
     */
    unsigned    total_size;

    /**
     * A size of memory currently in use.
     */
    unsigned    m_size;

    /**
     * A rate how to increase the already allocated buffer, in percent.
     * The default value 25 means that the buffer will grow by 25% each allocation:
     * i.e. if the first size passed to init() was 32, the allocations will be: 
     * 32 ; 40 (=32+32*0.25) ; 50 (=40+50*0.25) etc.
     */
    static const unsigned   GROW_RATE = 25;
};

/**
 * @brief Tests whether specified #jtype is too big to fit into a single 
 *        register on the current platform.
 * 
 * The only case currently is i64 on IA32.
 *
 * This is a characteristics of the platform, so it's placed in enc.h.
 */
inline bool is_big(jtype jt)
{
#if defined(_EM64T_) || defined(_IPF_)
    return false;
#else
    return jt==i64;
#endif
}

/**
 * @brief Returns a #jtype used to move an item of specified type (or part 
 *        of the item - in case of big type).
 *
 * The only case currently is that jtmov() returns i32 for jt=i64.
 */
inline jtype jtmov(jtype jt)
{
    return is_big(jt) ? i32 : jt;
}

/**
 * @brief true if current platform is IA32.
 *
 * Some presumptions are made about IA32 platform: there are no comressed 
 * references on it (not directly related to Encoder), an address fits into
 * 32 bits and fits into displacement of complex address form.
 */
inline bool is_ia32(void)
{
#if defined(_IA32_)
    return true;
#else
    return false;
#endif
}

/**
 * @brief Tests whether the specified value fits into 32 bit value.
 */
inline bool fits32(jlong val)
{
    return (INT_MIN <= val) && (val <= INT_MAX);
}


/**
 * @brief Tests whether the specified address fits into 32 bit value.
 *
 * Always true on IA32. Not always true on 64 bit platforms.
 */
inline bool fits32(const void* addr)
{
#ifdef _IA32_
    return true;
#else
    return fits32((jlong)(int_ptr)addr);
#endif
}

/**
 * @brief 
 * 
 * AR stands for Abstract Register.
 *
 * Every register may be uniquely identified either by its unique index 
 * (common index) or by a combination of type and index (which is another - 
 * type - index).
 *
 * The common index is unique for each register, and lies in the range of
 * (ar_idx(ar_x);ar_num), exclusive.
 *
 * The type index is unique only within the given group of registers and 
 * lies in the range of [gr_idx(gr0); gr_idx(gr0+gr_total-1)] for gr 
 * registers and [fr_idx(fr0); fr_idx(fr0+fr_total-1)], inclusive.
 * 
 */
enum AR {
    ar_x, fr_x = ar_x, gr_x = ar_x,
    //
    // General-purpose registers
    //
    // EAX, EBX, ECX, EDX, ESI, EDI, 
    gr0, gr1, gr2, gr3, gr4, gr5, 
#ifdef _EM64T_
    // R8, R9, R10, R11, R12, R13, R14, R15
    gr6, gr7, gr8, gr9, gr10, gr11, gr12, gr13, 
#endif
    bp, sp,
    //
    // Float-point registers
    //
    fr0, fr1, fr2, fr3, fr4, fr5, fr6, fr7, 
#ifdef _EM64T_
    fr8, fr9, fr10, fr11, fr12, fr13, fr14, fr15, 
#endif
    //
    // Specials
    //
    fp0, // top FPU stacked register
    //
#ifdef _EM64T_
    gr_num=15,      /// not including sp
    gr_total = 16,  /// including sp
    fr_num=16,
#else
    gr_num=7,       /// not including sp
    gr_total = 8,   /// including sp
    fr_num=8,
#endif
    fr_total=fr_num,
    ar_total = fr_total + gr_total, ar_num = ar_total,
};

/**
 * @brief Returns true if the register is float-point register.
 */
inline bool is_f(AR ar)
{
    return (fr0 <= ar && ar < (fr0+fr_total));
}

/**
 * @brief Returns true if the register is float-point register.
 */
inline bool is_fr(AR ar)
{
    return is_f(ar);
}

/**
 * @brief Returns true if the register is general-purpose register.
 */
inline bool is_gr(AR ar)
{
    return (gr0 <= ar && ar < (gr0+gr_total));
}

/**
 * @brief Constructs AR from with the given index (common index).
 */
inline AR _ar(unsigned idx)
{
    assert(idx<ar_total);
    return (AR)(gr0+idx);
}

/**
 * @brief Constructs AR from the given jtype and register index (type index).
 */
inline AR _ar(jtype jt, unsigned idx)
{
    if (is_f(jt)) {
        assert(idx<fr_total);
        return (AR)(fr0+idx);
    }
    assert(idx<gr_total);
    return (AR)(gr0+idx);
}

/**
 * @brief Contructs 'gr' register with the given type index.
 */
inline AR _gr(unsigned idx)
{
    return _ar(jobj, idx);
}

/**
 * @brief Contructs 'fr' register with the given type index.
 */
inline AR _fr(unsigned idx)
{
    return _ar(dbl64, idx);
}

/**
 * @brief Returns type index of the given 'fr' register.
 */
inline unsigned fr_idx(AR fr)
{
    assert(is_f(fr));
    return fr-fr0;
}

/**
 * @brief Returns type index of the given 'gr' register.
 */
inline unsigned gr_idx(AR gr)
{
    assert(!is_f(gr));
    return gr-gr0;
}

/**
 * @brief Returns common index of the given register.
 */
inline unsigned ar_idx(AR ar)
{
    assert(ar-gr0 < ar_total);
    return ar-gr0;
}

/**
 * @brief Extracts type index of the given register.
 */
inline unsigned type_idx(AR ar)
{
    return is_f(ar) ? fr_idx(ar) : gr_idx(ar);
}

/**
 * @brief Arithmetic and logical unit's operations supported by Encoder.
 */
enum ALU {
    alu_add, alu_sub, alu_mul, alu_div, alu_rem, alu_or, alu_xor, alu_and, 
    alu_cmp, alu_test, alu_shl, alu_shr, alu_sar,
    /// total number of ALU operations
    alu_count
};

/**
 * @brief Condition codes used for Encoder::br().
 * @see Encoder::br
 */
enum COND {
    // signed
    ge, le, gt, lt, eq, z=eq, ne, nz=ne, 
    /// unsigned
    ae, be, above, below,
    // 
    cond_none
};

/**
 * @brief Condition branches hints.
 * @see Encoder::br
 */
enum HINT {
    taken, not_taken, hint_none
};

/**
 * @brief Returns number of callee-save registers.
 *
 * The presumption used: the set of callee-save registers is constant
 * across a platform and does not depend on calling convention used.
 */
unsigned gen_num_calle_save(void);
/**
 * @brief Returns i-th float-point register for register-based calling 
 *        conventions.
 *
 * The presumption used: the set of registers is constant across a platform
 * and does not depend on calling convention used.
 * If we'll have to implement calling conventions with different sets of
 * registers (i.e. fastcall6 & fastcall4) then this presumption need to 
 * be revisited.
 */
AR get_cconv_fr(unsigned i, unsigned pos_in_args);
/**
 * @brief Returns i-th general-purpose register for register-based calling 
 *        conventions.
 * @see get_cconv_fr
 */
AR get_cconv_gr(unsigned i, unsigned pos_in_args);

/**
 * @brief Kind of operand.
 */
enum OpndKind { opnd_imm, opnd_mem, opnd_reg };

/**
 * @brief Represents an operand the Encoder works with.
 *
 * The Opnd can represent either immediate integer constant, or a register
 * operand, or a memory operand with complex address form [base+index*scale+
 * displacement].
 * 
 * Once created, instances of Opnd class are immutable. E.g. to change the 
 * type of an Opnd instance, one has to create new Opnd with either 
 * as_type() call, or with Opnd(jtype, const Opnd&).
 *
 */
class Opnd {
public:
    /**
     * @brief Constructs operand with 
     *        kind=#opnd_imm, type == #i32, ival() == 0.
     */
    Opnd() { clear(); }
    
    /**
     * @brief Clones the given Opnd, but with different type.
     * @see as_type
     */
    Opnd(jtype jt, const Opnd& that)
    {
        *this = that; m_jt = jt;
    }
    
    /**
     * @brief Constructs immediate operand of the given type and 
     *        initializes immediate field with the given value.
     * 
     * The width of any_val is wide enough to fit any necessary value - 
     * a pointer, #dbl64 or #i64.
     */
    Opnd(jtype jt, int_ptr any_val)
    {
        clear(); m_jt = jt; m_kind = opnd_imm; m_lval = any_val;
    }
    
    /**
     * @brief Constructs register operand.
     */
    Opnd(jtype jt, AR ar)
    {
        clear(); m_jt = jt; m_kind = opnd_reg; m_reg = ar;
    }
    
    /**
     * @brief Constructs register operand with a type of max possible width.
     * 
     * That is #jobj for GR registers and #dbl64 for FR registers.
     */
    Opnd(AR ar)
    {
        clear(); m_jt = is_f(ar) ? dbl64 : jobj; m_kind = opnd_reg; 
        m_reg = ar;
    }
    
    /**
     * @brief Constructs #i32 immediate operand.
     */
    Opnd(int ival)
    {
        clear(); m_jt = i32; m_lval = ival;
    }
    
    /**
     * @brief Constructs i32 immediate operand.
     */
    Opnd(unsigned ival)
    {
        clear(); m_jt = i32; m_lval = ival;
    }
    
#ifdef POINTER64
    /**
     * @brief Constructs #i64 immediate operand.
     *
     * @note Using Opnd(int_ptr) on 32-bit architecture leads to ambiguity
     * with Opnd(int), so Opnd(int_ptr) is under #ifdef.
     */
    Opnd(int_ptr lval)
    {
        clear(); m_jt = iplatf; m_lval = lval;
    }
    
    /**
     * @brief Constructs i64 immediate operand.
     *
     * @note Using Opnd(uint_ptr) on 32-bit architecture leads to ambiguity
     * with Opnd(unsigned), so Opnd(uint_ptr) is under #ifdef.
     */
    Opnd(uint_ptr lval)
    {
        clear(); m_jt = iplatf; m_lval = lval;
    }
#endif
    
    /**
     * @brief Constructs memory operand with no type (jvoid).
     */
    Opnd(AR base, int disp, AR index = ar_x, unsigned scale=0)
    {
        clear();
        m_kind = opnd_mem; m_jt = jvoid;
        m_base = base; m_index = index;
        m_scale = scale; m_disp = disp;
    }
    
    /**
     * @brief Constructs memory operand.
     */
    Opnd(jtype jt, AR base, int disp, AR index = ar_x, unsigned scale=0)
    {
        clear();
        m_kind = opnd_mem; m_jt = jt;
        m_base = base; m_index = index;
        m_scale = scale; m_disp = disp;
    }
#ifdef _IA32_
    /**
     * @brief Constructs memory operand, the given pointer is stored as 
     *        displacement.
     * @note IA-32 only.
     */
    Opnd(jtype jt, AR base, const void* disp)
    {
        clear();
        m_kind = opnd_mem; m_jt = jt;
        m_base = base; m_index = ar_x;
        m_scale = 0; m_disp = (int)disp;
    }
#endif 

    /**
     * @brief Returns kind of this operand.
     */
    OpndKind kind(void) const
    {
        return m_kind;
    }
    
    /**
     * @brief Returns type of this operand.
     */
    jtype jt(void) const
    {
        return m_jt;
    }
    /**
     * @brief Tests whether this operand is register operand.
     */
    bool is_reg(void) const { return kind() == opnd_reg; }
    /**
     * @brief Tests whether this operand is memory operand.
     */
    bool is_mem(void) const { return kind() == opnd_mem; }
    /**
     * @brief Tests whether this operand is immediate operand.
     */
    bool is_imm(void) const { return kind() == opnd_imm; }
    
    /**
     * @brief Tests two operands for equality.
     * 
     * For memory operands, types of operands are ignored (so it only tests
     * whether two operands refer to the same memory location).
     *
     * For immediate operands, types are taken into account (that means 
     * that zero of i64 type will \b not be equal to zero of i32 type).
     */
    bool operator==(const Opnd& that) const
    {
        if (kind() != that.kind()) return false;
        if (is_reg()) return reg() == that.reg();
        if (is_mem()) {
            // no test for jt() - it's intentional
            return base() == that.base() && 
                   disp() == that.disp() &&
                   index() == that.index() &&
                   scale() == that.scale();
        }
        assert(is_imm());
        if (jt() != that.jt()) return false;
        return m_lval == that.m_lval;
    }
    
    /**
     * @brief Operation reversed to operator==.
     */
    bool operator!=(const Opnd& that) const { return !(*this==that); }
    
    /**
     * @brief Returns AR for register operand, or #ar_x for operands of 
     *        other kinds.
     */
    AR reg(void) const { return m_kind == opnd_reg ? m_reg : ar_x; }
    /**
     * Returns integer value for immediate operand, or 0 for operands of 
     * other kinds.
     */
    int    ival(void) const { return  is_imm() ? (int)m_lval : 0; }
    /**
     * Returns long value for immediate operand, or 0 for 
     *        operands of other kinds.
     */
    int_ptr lval(void) const { return is_imm() ? m_lval : 0; }
    
    /**
     * Returns base register for memory operand, or ar_x for operands of 
     * other kinds.
     */
    AR base(void) const { return m_kind == opnd_mem ? m_base : ar_x; }
    
    /**
     * Returns index register for memory operand, or ar_x for operands of 
     * other kinds.
     */
    AR index(void) const { return m_kind == opnd_mem ? m_index : ar_x; }
    /**
     * Returns displacement of complex address form for memory operand, or 
     * 0 for operands of other kinds.
     */
    int disp(void) const { return m_kind == opnd_mem ? m_disp : 0; }
    /**
     * Returns scale of complex address form for memory operand, or 0 for 
     * operands of other kinds.
     */
    unsigned scale(void) const { return m_kind == opnd_mem ? m_scale : 0; }
    /**
     * Returns Opnd which only differs from this Opnd by the type.
     * @see Opnd(jtype, const Opnd&)
     */
    Opnd as_type(jtype jt) const
    {
        if (m_jt == jt) {
            return *this;
        }
        Opnd res(*this);
        res.m_jt = jt;
        return res;
    }
private:
    /**
     * Initializes Opnd instance with default values.
     */
    void clear(void)
    {
        m_kind = opnd_imm;
        m_jt = i32;
        m_base = m_index = ar_x;
        m_disp = 0;
        m_lval = 0;
        m_scale = 0;
    }
    /**
     * Kind of operand.
     */
    OpndKind    m_kind;
    /**
     * Type of operand.
     */
    jtype       m_jt;
    union {
        /**
         * AR for register operand.
         */
        AR          m_reg;
        /**
         * Displacement for memory operand.
         */
        int         m_disp;
        /**
         * Integer or long value of immediate operand.
         */
        int_ptr     m_lval;
    };
    /**
     * Base register for memory operand.
     */
    AR          m_base;
    /**
     * Index register for memory operand.
     */
    AR          m_index;
    /**
     * Scale for memory operand.
     */
    unsigned    m_scale;
};

/**
 * @brief Generation of code for an abstract CPU.
 *
 * Class Encoder used to generate CPU instructions in a CPU-independent
 * manner.
 *
 * The Encoder's function set represents an abstract CPU which has 2 sets 
 * of registers - general-purpose (named GP or GR) and float-point (named 
 * FP or FR), has memory and memory stack.
 *
 * The Encoder designed to hide specialties of underlying platform as much
 * as possible, so most of characteristics are the same: 
 *  - FR reg may hold both #dbl64 and #flt32
 *  - FR operations may have either memory or FR reg as second operand, but 
 *  not immediate
 *  - GR reg is wide enough to carry I_32
 *  - GR reg is wide enough to carry a memory address
 *  - a memory may be addressed using complex address form cosists of 
 *      base and index registers, displacement and a scale for index. The 
 *      scale may be any of the following: 1, 2, 4, 8.
 *
 * Though some differences still exist:
 *  - GR reg may \b not be wide enough to fit #i64 type (is_big(i64)==true)
 *  - An arbitrary address may not fit into displacement field of complex
 *  address form. If is_ia32()==true, then an address always fits into 
 *  displacement.
 *
 * Special emulations performed for the following cases:
 *  - (Intel 64) mov [mem64], imm64 -
 *      the operation is generated as 2 moves of imm32
 *  - (IA-32) operations that involve 8bit access to EBP, ESI or EDI - 
 *      in this case, the sequence of XCHG reg, reg; operation ; XCHG is 
 *      generated.
 *  - (all) 'PUSH fr' is emulated as 
 *  'sub sp, num_of_slots_for(dbl64) ; mov [sp], fr'. 'POP fr' is emulated 
 *  the same way.
 *  - (IA-32) Only 'mov/ld fp0, mem' and 'mov/st mem, fp0' are 
 *  allowed. In this case, FST/FLD instructions are generated. \b NOTE: 
 *  this simulation is only done in #fld and #fst methods, you can \b not
 *  do #mov with fp0. This limitation is intentional, to remove 
 *  additional check and branch from the hot exectuion path in #mov.
 * 
 * call() operation is made indirect only (trough a GR register). This is 
 * done intentionally, to reduce differences in code generation between 
 * platforms - on IA-32 we alway can do relative CALL, though on Intel 64
 * the possibility depends on whether the distance between CALL instruction
 * and its target fits into 2Gb. As the code is first generated into 
 * internal buffer, and then copied to its final location, the distance 
 * also changes and this may complicate the code generation routine.
 * In contrast, the <code>movp(gr, target); call(gr)</code> sequence works
 * the same way on all platforms.
 *
 * The code is generated into internal buffer represented by CodeStream 
 * object. 
 *
 * The Encoder also have support for \b patching of generated code. 
 *
 * Patching is a process of changing some part of instruction after it has
 * been generated. Normally, this is used to finalize addresses that are 
 * not yet known at the time of code generation (for example, for a forward
 * jump).
 * 
 * The following instructions support patching: branches (br(COND) and 
 * loading address into GR register (#movp).
 * 
 * When the such instruction is generated, then a special \e patch \e record
 * is stored in the Encoder internally. The patch record contains some info 
 * about the instruction - its length, offset, type (data or branch - 
 * below), whether patching was done for this instruction and so on.
 * 
 * Both methods accept additional user-defined arguments. In no way they 
 * are interpreted by the Encoder itself, just associated with the 
 * instruction to be patched. In CodeGen the arguments are used to store 
 * basic block and instruction's PC.
 * 
 * The method void patch(unsigned pid, void* inst_addr, void* data) 
 * performs the patching. 
 * 
 * \c pid is 'patch id' returned by appropriate #br() or #movp() call. This
 * is also the offset of the instruction in the internal Encoder's buffer.
 * 
 * \c inst_addr is the address of instruction to patch and \c data is the 
 * data to be stored into instruction.
 *
 * In many cases, \c inst_addr points to the instruction in the internal
 * Encoder's buffer, so the short version of patch(unsigned, void*) method
 * exists.
 * 
 * There are 2 kinds of patches - \e data and \e branch. The data patch 
 * is used with instruction that operate with data addresses, e.g. 
 * <code>mov gr, addr</code>. Branch patch applicable to br() instructions,
 * with the presumption that all branches are relative ones.
 *
 * The key difference is that when patching the \e data, address is stored
 * as-is, wihtout modification. When patching a branch, then the offset
 * between \c inst_addr and \c data (interpreted as address of target) 
 * is calculated and the offset is stored into instruction.
 *
 * @todo FPU-only support, without SSE to work on P3-s. The basic idea 
 *       is to emulate 'mov fr, fr'  using FXCH and 'mov fr, mem' and 
 *       'mov mem, fr' using FLD, FST and FXCH.
 * 
 * @todo IPF support. The basic idea is to hide one or two registers from 
 * application and use them in Encoder internally to emulate complex address 
 * form and other operations that are not natively support in IPF's 
 * instruction set.
 */
class Encoder {
public:
    /**
     * No op.
     */
    Encoder() {
        m_trace = false;
    }
    
    /**
     * Tests whether tracing enabled for this Encoder instance.
     * @note Only valid when JIT_TRACE macro is defined. Otherwise always
     *       returns false.
     * @see JIT_TRACE
     * @see JET_PROTO
     */
    bool is_trace_on(void) const 
    {
#ifdef JIT_TRACE
        return m_trace;
#else
        return false;
#endif
    }
    /**
     * Tests whether the AR is callee-save.
     */
    static bool is_callee_save(AR ar)
    {
        return isCalleeSave[ar_idx(ar)];
    }
    
    /**
     * Generates MOV operation.
     */
    void mov(const Opnd& op0, const Opnd& op1)
    {
        if (is_trace_on()) {
            trace(string("mov")+"("+to_str(op0.jt())+")", 
                to_str(op0), to_str(op1));
        }
        mov_impl(op0, op1);
    }
    /**
     * Generates load of constant address into GR register.
     * @see movp(AR, unsigned, unsigned)
     */
    void movp(AR op0, const void *op1)
    {
        assert(op0 != ar_x);
        assert(is_gr(op0));
        if (is_trace_on()) { 
            trace("movP", to_str(op0), to_str(op1));
        }
        movp_impl(op0, op1);
    }
    /**
     * Generates load of an address into GR register, for further patching.
     * @param gr - register to load
     * @param udata - user data (not interpreted by Encoder)
     * @param ubase - user data (not interpreted by Encoder)
     * @see movp(AR, const void*)
     */
    unsigned movp(AR gr, unsigned udata, unsigned ubase);
    /**
     * Generates load of an address specified by mem argument into the reg 
     * argument.
     * @note \c reg must be register and \c mem can only be memory operand.
     */
    void lea(const Opnd& reg, const Opnd& mem);
    /**
     * Generates sign extension of I_8 from op1 into op0.
     */
    void sx1(const Opnd& op0, const Opnd& op1);
    /**
     * Generates sign extension of int16 from op1 into op0.
     */
    void sx2(const Opnd& op0, const Opnd& op1);
    /**
     * Generates sign extension op1 into op0.
     */
    void sx(const Opnd& op0, const Opnd& op1);
    /**
     * Generates zero extension of U_8 from op1 into op0.
     */
    void zx1(const Opnd& op0, const Opnd& op1);
    /**
     * Generates zero extension of uint16 from op1 into op0.
     */
    void zx2(const Opnd& op0, const Opnd& op1);
    /**
     * Generates ALU operation.
     */
    void alu(ALU alu, const Opnd& op0, const Opnd& op1)
    {
        if (is_trace_on()) {
            trace(to_str(alu), to_str(op0), to_str(op1));
        }
        alu_impl(alu, op0, op1);
    }

   /**
   * Generates n-byte long NOP instruction.
   */
    void nop(U_32 n) {
        if (is_trace_on()) {
            trace(string("nop"), to_str((int)n), string());
        }
        nop_impl(n);
    }
    
    /**
    * Performs bitwise NOT operation.
    */
    void bitwise_not(const Opnd& op0) {
        if (is_trace_on()) {
            trace(string("not"), to_str(op0), to_str(""));
        }
        not_impl(op0);
    }

    /**
    * Generates CMOVxx operation.
    */
    void cmovcc(COND cond, const Opnd& op0, const Opnd& op1)
    {
        if (is_trace_on()) {
            trace(string("cmov:")+ to_str(cond), to_str(op0), to_str(op1));
        }
        cmovcc_impl(cond, op0, op1);
    }

    /**
    * Generates CMPXCHG operation.
    */
    void cmpxchg(bool lockPrefix, AR addrBaseReg, AR newReg, AR oldReg)
    {
        if (is_trace_on()) {
            trace(string("cmpxchg:")+ (lockPrefix ? "(locked) ":"") + to_str(addrBaseReg), to_str(newReg), to_str(oldReg));
        }
        cmpxchg_impl(lockPrefix, addrBaseReg, newReg, oldReg);
    }

    /**
    * Generates write for 64-bit volatile value
    */
    void volatile64_set(Opnd& where, AR hi_part, AR lo_part)
    {
        if (is_trace_on()) {
            trace(string("volatile64_set:") + to_str(where), to_str(hi_part), to_str(lo_part));
        }
        volatile64_op_impl(where, hi_part, lo_part, true);
    }

    /**
    * Generates read for 64-bit volatile value
    */
    void volatile64_get(Opnd& where, AR hi_part, AR lo_part)
    {
        if (is_trace_on()) {
            trace(string("volatile64_get:") + to_str(where), to_str(hi_part), to_str(lo_part));
        }
        volatile64_op_impl(where, hi_part, lo_part, false);
    }

    /**
     * Generates ALU operation between two registers.
     *
     * The registers are used as \c jt type.
     */
    void alu(jtype jt, ALU op, AR op0, AR op1)
    {
        alu(op, Opnd(jt, op0), Opnd(jt, op1));
    }
    
    /**
     * Loads from memory into the specified register.
     *
     * Just a wrapper around mov().
     * @note On IA32 fp0 loads are threated in a special way.
     */
    void ld(jtype jt, AR ar, AR base, int disp=0, AR index = ar_x, 
            unsigned scale=0)
    {
        if (is_f(jt)) {
            fld(jt, ar, base, disp, index, scale);
        }
        else {
            mov(Opnd(jt, ar), Opnd(jt, base, disp, index, scale));
        }
    }
    /**
     * Stores from the specified register into memory .
     * Just a wrapper around mov().
     */
    void st(jtype jt, AR ar, AR base, int disp=0, AR index = gr_x, 
            unsigned scale=0)
    {
        if (is_f(jt)) {
            fst(jt, ar, base, disp, index, scale);
        }
        else {
            mov(Opnd(jt, base, disp, index, scale), Opnd(jt, ar));
        }
    }
    /**
     * Loads from memory into the specified FR register.
     *
     * Just a wrapper around mov().
     * @note On IA32 fp0 loads are threated in a special way.
     */
    void fld(jtype jt, AR ar, AR base, int disp=0, AR index = ar_x, 
             unsigned scale=0);
    /**
     * Stores from the specified FR register into memory .
     *
     * Just a wrapper around mov().
     * @note On IA32 fp0 stores are threated in a special way.
     */
    void fst(jtype jt, AR ar, AR base, int disp=0, AR index = gr_x, 
             unsigned scale=0);
    /**
     * Loads 8bit from memory into GR register.
     */
    void ld1(AR ar, AR base, int disp=0, AR ridx = ar_x, unsigned scale=0)
    {
        ld(i8, ar, base, disp, ridx, scale);
    }
    /**
     * Loads 16bit from memory into GR register.
     */
    void ld2(AR ar, AR base, int disp=0, AR ridx = ar_x, unsigned scale=0)
    {
        ld(i16, ar, base, disp, ridx, scale);
    }
    /**
     * Loads 32bit from memory into a register.
     */
    void ld4(AR ar, AR base, int disp=0, AR ridx = ar_x, unsigned scale=0)
    {
        ld(is_fr(ar) ? flt32 : i32, ar, base, disp, ridx, scale);
    }
    /**
     * Stores 8bit from GR register into memory.
     */
    void st1(AR ar, AR base, int disp=0, AR ridx = ar_x, unsigned scale=0)
    {
        st(i8, ar, base, disp, ridx, scale);
    }
    /**
     * Stores 16bit from GR register into memory.
     */
    void st2(AR ar, AR base, int disp=0, AR ridx = ar_x, unsigned scale=0)
    {
        st(i16, ar, base, disp, ridx, scale);
    }
    /**
     * Stores 32bit from GR or FR register into memory.
     */
    void st4(AR ar, AR base, int disp=0, AR ridx = ar_x, unsigned scale=0)
    {
        st(is_fr(ar) ? flt32 : i32, ar, base, disp, ridx, scale);
    }
    /**
     * Pushes the value onto stack.
     * @note Push of FR registers is emulated (sub sp, n ; mov [sp], fr).
     * @return Number of bytes spent from the stack - the number 
     * subtracted from #sp .
     */
    int push(const Opnd& op0);
    /**
     * Pops out the value from stack.
     * @note Pop of FR registers is emulated (mov fr, [sp] ; add sp, n).
     * @return Number of bytes popped from the stack - the number added to
     *         #sp.
     */
    int pop(const Opnd& op0);
    
    /**
     * Returns number of bytes needed to store all the registers.
     */
    static unsigned get_all_regs_size(void)
    {
        return gr_num*STACK_SLOT_SIZE + fr_num*8;
    }

    /**
     * Pushes either all or all scratch registers onto stack.
     * Number of bytes spent on stack is always rounded to 16.
     */
    int push_all(bool includeCalleeSave=false);
    /**
     * Pops out either all or all scratch registers from stack.
     * Number of bytes popped from stack is always rounded to 16.
     */
    int pop_all(bool includeCalleeSave=false);
    
    /**
     * Generates return instruction.
     * @param pop_bytes - how many bytes to pop out from the stack after 
     * return.
     */
    void ret(unsigned pop_bytes);
    
    /**
     * Generates indirect call instruction.
     * 
     * If calling convention assumes that caller restores stack, then it 
     * also generates code to restore stack.
     * 
     * If check_stack is \c true and calling conventions obliges stack 
     * alignment, then a code that checks this alignment is also generated.
     * trap() instruction is executed if alignment requirement not met.
     */
    void call(const Opnd& target, const CallSig& ci, 
              bool check_stack = false);
    /**
     * Generates indirect call to \c target trough the specified register.
     * May place constant arguments according to \c cs. \c idx parameter 
     * specifies which argument to start from. If all arguments are already
     * prepared, then set <code>idx = cs.count()</code>.
     *
     * If \c idx is 0 and any argument is passed via stack, then stack 
     * preparation sequence is generated (<code>sub sp, cs.size()</code>).
     *
     * If calling convention assumes that caller restores stack, then 
     * the proper instructions are generated.
     *
     * If check_stack is \c true and calling conventions obliges stack 
     * alignment, then a code that checks this alignment is also generated.
     * trap() instruction is executed if alignment requirement not met.
     */
    void call(bool check_stack, AR gr, const void * target, 
              const CallSig& cs, unsigned idx, ...);
    /**
     * Same as call(...) but takes arguments to pass from \c va_list.
     */
    void call_va(bool check_stack, AR ar, const void *target, 
                 const CallSig& cs, unsigned idx, va_list& valist);
    /**
     *
     * @todo the name may be somehow confusing with CodeGen's one, may 
     * think about renaming.
     */
    void gen_args(const CallSig& cs, AR grtmp, unsigned idx, unsigned count, ...);
    /**
     * Generates conditional or unconditional branch.
     * @param op - target operand
     * @param cond - condition for conditional branch or cond_none
     * @param hint - possible hint whether conditional branch is presumed
     * to be taken or not
     */
    void br(const Opnd& op, COND cond=cond_none, HINT hint=hint_none);
    /**
     * Generates conditional or unconditional branch for further patching.
     * @param cond - condition for conditional branch or cond_none
     * @param udata - user data (not interpreted by Encoder)
     * @param ubase - user data (not interpreted by Encoder)
     * @param hint - possible hint whenether conditional branch is presumed
     * to be taken or not
     * @return patching id (which is also ip offset of generated branch 
     * instruction)
     */
    unsigned br(COND cond, unsigned udata, unsigned ubase, 
                HINT hint=hint_none);
    /**
     * Generates software breakpoint.
     */
    void trap(void);
    
    /**
     * Triggers software breakpoint.
     * @note The method does \b not generate software break point, but 
     *       raises it in the current program instead - in platform 
     *       dependent manner. On Win it's DebugBreak() and it's 
     *       raise(SIGTRAP) on Linux.
     * @note To generate software break point use trap().
     * @see trap
     */
    static void debug(void);
    /**
     * @brief Returns current offset in the Encoder's internal buffer.
     * That is the offset where next instruction will be generated.
     */
    unsigned ipoff(void) const
    {
        return m_codeStream.ipoff();
    }
    
    /**
     * @brief Returns number of patch records registered in current Encoder.
     */
    unsigned patch_count(void) const
    {
        return (unsigned) m_patches.size();
    }
    /**
     * @brief Returns info about next patch record.
     * @param[out] ppid - patch id (which is also offset of instruction 
     * in the Encoder's internal buffer)
     * @param[out] pudata - user data 1
     * @param[out] pubase - user data 1
     * @param[out] pdone - \b true if the instruction was patched already
     * @returns \b true if the patch record is for data instruction, \b
     * false for branch instruction.
     */
    bool enum_patch_data(unsigned* ppid, unsigned* pudata, 
                         unsigned* pubase, bool* pdone)
    {
        *ppid = iter->first;
        const CodePatchItem& cpi = iter->second;
        *pudata = cpi.udata;
        *pubase = cpi.ubase;
        *pdone = cpi.done;
        return cpi.data;
    }
    /**
     * @brief Begins enumeration of patch records.
     */
    void * enum_start(void) 
    {
        iter = m_patches.begin();
        return NULL; //(void*)&i;
    }
    /**
     * @brief Returns \b true if no more items to enumrate remains.
     */
    bool enum_is_end(void *h)
    {
        return iter == m_patches.end();
    }
    
    /**
     * @brief Advances enumeration iterator on next item.
     */
    void enum_next(void * h)
    {
        assert(iter != m_patches.end());
        ++iter;
    }
    /**
     * @brief Patch the given by \c pid instruction in the Encoder's 
     * internal buffer.
     */
    void patch(unsigned pid, void * data)
    {
        // pid is also ipoff of the instruction
        void * inst_addr = ip(pid);
        patch(pid, inst_addr, data);
    }
    void patch(unsigned pid, void* inst_addr, void* data);
    /**
     * Returns a current 'ip' for underlying code stream - that is 
     * an 'ip' where the next emitted instruction will begin.
     * The ip returned is a pointer to an internal temporary code buffer.
     */
    char * ip(void)
    {
        return m_codeStream.ip();
    }
    /**
     * @brief Returns address in Encoder's internal buffer by the given
     * offset.
     */
    char * ip(unsigned ipoff)
    {
        return m_codeStream.ip(ipoff);
    }

    /**
     * Sets a current ip for the internal code buffer.
     */
    void ip(char * _ip)
    {
        m_codeStream.ip(_ip);
    }
protected:
    unsigned    m_trace;
public:
    /**
     * Initializes internal Encoder's data.
     * Must be invoked before any usage of Encoder.
     */
    static void init(void);
public:
    /**
     * Formats the given \c op into human-readable string.
     */
    static string to_str(const Opnd& op);
    /**
     * Formats the given \c ar into human-readable string.
     *
     * Callee-save registers are presented in capital letters.
     * @param ar - register to convert to string.
     * @param platf - if \c true, then a native (e.g. EAX) returned instead
     * of abstract one (e.g. gr0).
     */
    static string to_str(AR ar, bool platf = false);
    /**
     * Formats the given complex address from  into human-readable string.
     */
    static string to_str(AR base, int disp, AR index, unsigned scale);
    /**
     * Formats the given \c addr into human-readable string.
     */
    static string to_str(const void * addr);
    /**
     * Formats the given integer into human-readable string.
     */
    static string to_str(int i);
    /**
     * Formats the given ALU code into human-readable string.
     */
    static string to_str(ALU op);
    /**
     * Formats the given condition code into human-readable string.
     */
    static string to_str(COND cond);
    /**
     * Formats the given HINT into human-readable string.
     */
    static string to_str(HINT hint);
    /**
     * Formats the given jtype into human-readable string.
     */
    static string to_str(jtype jt);
protected:
    void trace(const string& func, const string& op0, const string& op1);
    /**
     * Used to beautify debugging output for complex code sequences like 
     * push_all().
     */
    string m_prefix;
    /**
     * An internal temporary buffer where the generated code is accumulated.
     * Normally not to be used directly, but instead through ip() methods calls.
     */
    CodeStream  m_codeStream;
private:
    /**
     * Patch record.
     */
    struct CodePatchItem {
        /// length
        unsigned len;
        /// data or branch instruction
        bool data;
        /// \b true if instruction was patched
        bool done;
        /// user data 1
        unsigned udata;
        /// user data 2
        unsigned ubase;
    };
    /**
     * Map of patch records.
     */
    typedef map<unsigned, CodePatchItem> PATCH_MAP;
    /**
     * Storage of patch records.
     */
    PATCH_MAP   m_patches;
    /**
     * Iterator used during enumeration of patch records.
     */
    PATCH_MAP::iterator iter;
    /**
     * Set of flags which registers are callee-save.
     */
    static bitset<ar_num> isCalleeSave;
    /**
     * Creates patch record for current ipoff().
     */
    unsigned reg_patch(bool data, unsigned udata, unsigned ubase_ipoff);
    
    /**
     * Finalizes current patch record (stores instruction length, etc).
     */
    void reg_patch_end(unsigned pid);
    
    //
    // Platform-specific implementations 
    //
    
    /// Implementation of mov().
    void mov_impl(const Opnd& op0, const Opnd& op1);
    /// Implementation of not().
    void not_impl(const Opnd& op0);
    /// Implementation of alu().
    void alu_impl(ALU op, const Opnd& op0, const Opnd& op1);
   //Implementation of nop()
    void nop_impl(U_32 n);
    /// Implementation of cmovcc().
    void cmovcc_impl(COND c, const Opnd& op0, const Opnd& op1);
    /// Implementation of cmpxchg().
    void cmpxchg_impl(bool lockPrefix, AR addrReg, AR newReg, AR oldReg);
    /// Implementation of volatile64 get and set ops().
    void volatile64_op_impl(Opnd& where, AR hi_part, AR lo_part, bool is_put);
    /// Implementation of lea().
    void lea_impl(const Opnd& reg, const Opnd& mem);
    /// Implementation of movp().
    void movp_impl(AR op0, const void *);
    /// Implementation of sx1().
    void sx1_impl(const Opnd& op0, const Opnd& op1);
    /// Implementation of sx2().
    void sx2_impl(const Opnd& op0, const Opnd& op1);
    /// Implementation of sx().
    void sx_impl(const Opnd& op0, const Opnd& op1);
    /// Implementation of zx1().
    void zx1_impl(const Opnd& op0, const Opnd& op1);
    /// Implementation of zx2().
    void zx2_impl(const Opnd& op0, const Opnd& op1);
    /// Implementation of fld().
    void fld_impl(jtype jt, AR op0, AR base, int disp, AR index, 
                  unsigned scale);
    /// Implementation of fst().
    void fst_impl(jtype jt, AR op0, AR base, int disp, AR index, 
                  unsigned scale);
    /// Implementation of push().
    int push_impl(const Opnd& op0);
    /// Implementation of pop().
    int pop_impl(const Opnd& op0);
    /// Implementation of call().
    void call_impl(const Opnd& target);
    /// Implementation of ret().
    void ret_impl(unsigned pop);
    /// Implementation of br().
    void br_impl(COND cond, HINT hint);
    /// Implementation of br().
    void br_impl(const Opnd& op, COND cond, HINT hint);
    /// Converts \c ar into platform's register name.
    static string to_str_impl(AR ar);
    /// Implementation of trap().
    void trap_impl(void);
    //
    static bool is_callee_save_impl(AR gr);
};

/**
 * Returns \b true if the \c ar is callee-save register.
 */
inline bool is_callee_save(AR ar)
{
    return Encoder::is_callee_save(ar);
}


}
}; // ~namespace Jitrino::Jet

#endif  // __ENC_H_INCLUDED__
