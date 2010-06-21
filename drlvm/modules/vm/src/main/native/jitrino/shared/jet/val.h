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
 * @brief Val class declaration.
 */

#if !defined(__VAL_H_INCLUDED__)
#define __VAL_H_INCLUDED__

#include "enc.h"

namespace Jitrino {
namespace Jet {

/**
 * @defgroup VA_ Attributes of Val items.
 */
/**
 * @brief 'Non Zero' - an items tested to be non-zero/non-null.
 */
#define VA_NZ           (0x00000001)
/**
 * @brief An item was tested against negative-size and is either positive or 
 *        zero.
 */
#define VA_NOT_NEG      (0x00000002)
/**
 * @brief Item was marked in GC map.
 */
#define VA_MARKED       (0x00000004)
/**
 * @brief Item has its copy in the memory, no need to sync it back.
 *
 * For example, if the item was just loaded from memory to register - no 
 * need to store it back, e.g. to outlive CALL. Surely, this attribute 
 * must be dropped on a defining operation.
 *
 * Currently unused.
 */
#define VA_FRESH        (0x00000008)
/// @} // ~VA_


/**
 * @brief Class Val represents various value items that may exist during
 *        compilation - immediate constants, memory references or values
 *        in registers.
 *
 * Val acts similar to Encoder's Opnd, but may carry extended information, 
 * that Opnd can't handle (e.g. attributes or float point and double items
 * - both immediate-s and static constants in memory), and also has 
 * extended set of operations over it.
 */
class Val {
public:
    /**
     * Initializes the instance with \e dummy values.
     *
     * That is the instance gets #opnd_mem kind, but the type is jvoid,
     * and the base register gets meaningless value ((AR)NOTHING).
     */
    Val()
    {
        clear();
    }
    Val(jtype jt)
    {
        clear(); m_jt = jt;
    }
    Val(jtype jt, AR ar)
    {
        clear(); m_kind = opnd_reg;  m_jt = jt; m_reg = ar;
    }
    Val(int ival)
    {
        clear(); m_kind = opnd_imm; m_jt = i32; m_lval = ival;
        m_surviveCalls = true;
    }
    Val(jlong lval)
    {
        clear(); m_kind = opnd_imm; m_jt = i64; m_lval = lval;
        m_surviveCalls = true;
    }
    Val(float fval, const void* caddr = NULL)
    {
        clear();
        m_kind = opnd_imm; m_jt = flt32; m_fval = fval; m_caddr = caddr;
        m_surviveCalls = true;
    }
    Val(double dval, const void* caddr = NULL)
    {
        clear();
        m_kind = opnd_imm; m_jt = dbl64; m_dval = dval; m_caddr = caddr;
        m_surviveCalls = true;
    }
    Val(jtype jt, const void* p)
    {
        clear();
        m_kind = opnd_imm; assert(jt==jobj || jt==iplatf); m_jt = jt; m_pval = p;
    }
    Val(AR base, int disp, AR index = ar_x, unsigned scale=0)
    {
        clear();
        m_kind = opnd_mem; m_jt = jvoid;
        m_base = base; m_index = index;
        m_scale = scale; m_disp = disp;
    }
    Val(jtype jt, AR base, int disp, AR index = ar_x, unsigned scale=0)
    {
        clear();
        m_kind = opnd_mem; m_jt = jt;
        m_base = base; m_index = index;
        m_scale = scale; m_disp = disp;
    }
#ifdef _IA32_
    /**
     * Special-purpose ctor, only exists on IA-32.
     * 
     * Packs \c disp address into displacement.
     */
    Val(jtype jt, AR base, void* disp)
    {
        clear();
        m_kind = opnd_mem; m_jt = jt;
        m_base = base; m_index = ar_x;
        m_scale = 0; m_disp = (int)disp;
    }
#endif
    Val(const Opnd& op) {
        clear();
        m_jt = op.jt();
        m_kind = op.kind();
        if (is_reg()) {
            m_reg = op.reg();
        }
        else if (is_mem()) {
            m_base = op.base();
            m_disp = op.disp();
            m_index = op.index();
            m_scale = op.scale();
        }
        else if (m_jt <= i32) {
            m_lval = op.ival();
            m_surviveCalls = true;
        }
        else if (m_jt == jobj) {
            m_pval = (const void*)op.lval();
        }
        else {
            assert(m_jt == i64);
            m_lval = op.lval();
        }
    }
    /**
     * Returns kind of the Val.
     */
    OpndKind kind(void) const { return m_kind; }
    /**
     * Returns type of the Val.
     */
    jtype jt(void) const { return m_jt; }
    /**
     * Returns type of the Val.
     */
    jtype type(void) const { return m_jt; }
    /**
     * Tests whether this Val was not initialized to a valid value (e.g. 
     * created via Val() ctor).
     */
    bool is_dummy(void) const
    {
        return is_mem() && base() == (AR)NOTHING;
    }
    bool is_reg(void) const { return kind() == opnd_reg; }
    bool is_mem(void) const { return kind() == opnd_mem; }
    bool is_imm(void) const { return kind() == opnd_imm; }
    bool uses(AR ar) const
    {
        if (is_mem()) return base() == ar || index() == ar;
        if (is_reg()) return reg() == ar;
        return false;
    }
    //
    bool operator==(const Val& that) const
    {
        if (kind() != that.kind()) return false;
        if (is_reg()) return reg() == that.reg();
        if (is_mem()) {
            // no test for jt()
            return base() == that.base() && 
                   disp() == that.disp() &&
                   index() == that.index() &&
                   scale() == that.scale();
        }
        assert(is_imm());
        if (jt() != that.jt()) return false;
        return m_lval == that.m_lval;
    }
    
    bool operator!=(const Val& that) const
    {
        return !(*this==that);
    }
    /**
     * Converts the Val into Opnd.
     */
    Opnd as_opnd(void) const
    {
        return as_opnd(jt());
    }
    /**
     * Converts the Val into Opnd, and sets its type to \c jt.
     */
    Opnd as_opnd(jtype jt) const
    {
        if (is_mem()) {
            return Opnd(jt, base(), disp(), index(), scale());
        }
        if (is_reg()) {
            return Opnd(jt, reg());
        }
        return Opnd(jt, (int_ptr)lval());
    }
    //
    AR      reg(void) const { return m_kind == opnd_reg ? m_reg : ar_x; }
    //
    int     ival(void) const { return is_imm() ? (int)m_lval : 0; }
    jlong   lval(void) const { return is_imm() ? m_lval : 0; }
    //
    float   fval(void) const { return is_imm() ? m_fval : 0; }
    double  dval(void) const { return is_imm() ? m_dval : 0; }
    const void* pval(void) const { return is_imm() ? m_pval : NULL; }
    const void* caddr(void) const { return m_caddr; }
    /**
     * Sets current Val's property \b survive_calls.
     * @return reference to this Val instance
     * @todo Make the property as attribute ?
     */
    Val&   long_live(void) { m_surviveCalls = true; return *this; };
    /**
     * Sets a property whether this Val may outlive calls without any 
     * special preparation (e.g. static fiedls).
     */
    void    set_survive_calls(bool s = true) { m_surviveCalls = s; };
    /**
     * Tests whether this Val may outlive calls without any special 
     * preparation (e.g. static fiedls).
     */
    bool    get_survive_calls(void) const { return m_surviveCalls; };
    /**
     * Tests whether this Val may outlive calls without any special 
     * preparation (e.g. static fiedls).
     */
    bool    survive_calls(void) const { return m_surviveCalls; };
    /**
    * Converts Val into operand.
    */
    void    to_opnd(Opnd& op) {

            m_kind = op.kind();
            if (op.is_mem()) {
                m_base = op.base();
                m_disp = op.disp();
                m_index = op.index();
                m_scale = op.scale();
            } else if (op.is_reg()){
                m_reg = op.reg();
            } else if (op.jt() <= i32) {
                m_lval = op.ival();
                m_surviveCalls = true;
            } else if (op.jt() == jobj) {
                m_pval = (const void*)op.lval();
            } else if (op.jt() == flt32) {
                m_fval = (float)op.ival();
            } else if (op.jt() == dbl64) {
                m_dval = (double)op.lval();
            }  else {
                assert(op.jt() == i64);
                m_lval = op.lval();
            }
        }
    /**
    * Converts Val into operand.
    */
    void    to_val(Val& value) {
            m_jt = value.jt();
            m_kind = value.kind();
            m_surviveCalls = value.get_survive_calls();
            m_caddr = value.caddr();
            m_attrs = value.attrs();

            if (value.is_mem()) {
                m_base = value.base();
                m_disp = value.disp();
                m_index = value.index();
                m_scale = value.scale();
            } else if (value.is_reg()){
                m_reg = value.reg();
            } else if (value.jt() <= i32) {
                m_lval = value.ival();
            } else if (value.jt() == jobj) {
                m_pval = value.pval();
            } else if (value.jt() == flt32) {
                m_fval = value.fval();
            } else if (value.jt() == dbl64) {
                m_dval = value.dval();
            }  else {
                assert(value.jt() == i64);
                m_lval = value.lval();
            }
        }
    /**
    * Converts Val into memory reference.
    */
    void    to_mem(AR base, int disp, AR index = ar_x, unsigned scale=0)
    {
        m_surviveCalls = false;
        m_kind = opnd_mem;
        m_base = base; m_index = index;
        m_scale = scale; m_disp = disp;
    }
    /**
     * Converts Val into register reference.
     */
    void    to_reg(AR ar)
    {
        m_surviveCalls = false;
        m_kind = opnd_reg;
        m_reg = ar;
    }
    /**
     * Returns \link VA_ attributes \endlink.
     */
    unsigned attrs(void) const { return m_attrs; }
    /**
     * Assigns \link VA_ attributes \endlink to this Val.
     */
    void attrs(unsigned attrs)
    {
        m_attrs = attrs;
    }
    /**
     * Adds the specified \link VA_ attributes \endlink to this Val.
     */
    void set(unsigned attr)
    {
        m_attrs |= attr;
    }
    /**
     * Removes the specified \link VA_ attributes \endlink to this Val.
     */
    void clr(unsigned attrs)
    {
        m_attrs &= ~attrs;
    }
    /**
     * Tests whether this Val has at least one given attribute.
     */
    bool has(unsigned mask) const
    {
        return 0 != (m_attrs&mask);
    }
    AR base(void) const { return m_kind == opnd_mem ? m_base : ar_x; }
    AR index(void) const { return m_kind == opnd_mem ? m_index : ar_x; }
    int disp(void) const { return m_kind == opnd_mem ? m_disp : 0; }
    unsigned scale(void) const { return m_kind == opnd_mem ? m_scale : 0; }
private:
    void clear(void)
    {
        m_kind = opnd_mem;
        m_jt = jvoid;
        // As we are initializing Val to be mem, then set m_base
        // to something meaningless, to avoid confusions with real 
        // memory operands which has only displacement.
        m_base = (AR)NOTHING;
        m_index = ar_x;
        m_lval = 0;
        m_scale = m_disp = 0;
        //
        m_caddr = NULL;
        m_attrs = 0;
        m_surviveCalls = false;
    }
    OpndKind    m_kind;
    jtype       m_jt;
    union {
        AR          m_reg;
        int         m_disp;
        jlong       m_lval;
        double      m_dval;
        float       m_fval;
        const void* m_pval;
    };
    AR          m_base;
    AR          m_index;
    unsigned    m_scale;
    //
    const void* m_caddr;
    unsigned    m_attrs;
    bool        m_surviveCalls;
};



}}; // ~namespace Jitrino::Jet

#endif      // ~__VAL_H_INCLUDED__


