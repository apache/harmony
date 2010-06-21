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
 * @brief CallSig class and related constants declaration.
 */

#if !defined(__CSIG_H_INCLUDED__)
#define __CSIG_H_INCLUDED__

#include "enc.h"
#include <vector>
using std::vector;

namespace Jitrino {
namespace Jet {

/**
 * @defgroup JET_CCONV Calling conventions description.
 * @{
 */


/**
 * @brief Order of parameters is left-to-right.
 */
#define CCONV_L2R                   (0x00000001)

/**
 * @brief Caller must restore stack (pop out arguments).
 */
#define CCONV_CALLER_POPS           (0x00000002)

/**
 * @brief When entering a function, obey the (sp)%%4 == 0 rule.
 */
#define CCONV_STACK_ALIGN4          (0x00000004)

/**
 * @brief When entering a function, obey the (sp+8)%%16 == 0 rule (Intel 64
 *        convention).
 */
#define CCONV_STACK_ALIGN_HALF16    (0x00000008)

/**
 * @brief When entering a function, obey the (sp)%%16 == 0 rule.
 */
#define CCONV_STACK_ALIGN16         (0x00000010)

/**
 * @brief Mask to extract stack alignment form calling convention.
 */
#define CCONV_STACK_ALIGN_MASK      (CCONV_STACK_ALIGN4 | CCONV_STACK_ALIGN_HALF16 | CCONV_STACK_ALIGN16)

/**
 * @brief All args go though memory.
 */
#define CCONV_MEM                   (0x00000020)

/**
 * @brief Use FPU register stack to return floating point, xmm0 otherwise.
 */
#define CCONV_RETURN_FP_THROUGH_FPU (0x00000040)

/**
 * @brief IA-32's stdcall convention.
 */
#define CCONV_STDCALL_IA32     (CCONV_MEM | CCONV_RETURN_FP_THROUGH_FPU)

/**
 * @brief IA-32's cdecl convention.
 */
#define CCONV_CDECL_IA32       (CCONV_CALLER_POPS | CCONV_MEM | CCONV_RETURN_FP_THROUGH_FPU)

#ifdef _EM64T_
    /**
     * @brief EM64T calling convention.
     */
    #define CCONV_EM64T     (CCONV_STACK_ALIGN_HALF16 | CCONV_CALLER_POPS)
    /**
     * @brief On IA-32 it's CCONV_CDECL_IA32, on EM64T it's CCONV_EM64T.
     */
    #define CCONV_STDCALL   CCONV_EM64T
    /**
     * @brief On IA-32 it's CCONV_CDECL_IA32, on EM64T it's CCONV_EM64T.
     */
    #define CCONV_CDECL     CCONV_EM64T
    #define CCONV_PLATFORM  CCONV_EM64T
	#ifdef _WIN32
		/// A nubmer of FR registers dedicated to pass float-point arguments.
		#define MAX_FR_ARGS (4)
	#else
		#define MAX_FR_ARGS (8)
	#endif
#else
    #define CCONV_STDCALL   CCONV_STDCALL_IA32
    #define CCONV_CDECL     CCONV_CDECL_IA32
    #define CCONV_PLATFORM  CCONV_CDECL
	#define MAX_FR_ARGS (0)
#endif

/**
 * @brief IA-32's DRLVM's convention for managed code.
 */
#define CCONV_MANAGED_IA32     (CCONV_L2R | CCONV_MEM | CCONV_STACK_ALIGN16)
/**
 * @brief A special case - VM's helper MULTIANEWARRAY always has cdecl-like
 *        convention.
 */
#define CCONV_MULTIANEWARRAY    CCONV_CDECL_IA32

#ifdef _EM64T_
    /**
     * @brief On IA-32 it's CCONV_MANAGED_IA32, on EM64T it's CCONV_EM64T.
     */
    #define CCONV_MANAGED   CCONV_EM64T
#else
    #define CCONV_MANAGED   CCONV_MANAGED_IA32
#endif

#define CCONV_HELPERS       CCONV_STDCALL

///@}   // ~JET_CCONV


/**
 * @brief Describes method's signature - number of arguments, their types, 
 *        calling convention, etc.
 *
 * A CallSig object which describes a function with stdcall calling 
 * convention which has 2 argument - an integer and float:
 *
 * @code
 *  CallSig ssig(CCONV_STDCALL, i32, flt32)
 * @endcode
 *
 * Arguments are always numbered and referred to in left-to-right order:
 * @code
 *  foo(arg 0, arg 1, arg 2)
    CallSig csig(cc, arg0 type, arg1 type, arg2 type);
    csig.get(2) refers to arg 2.
 * @endcode
 *
 * An offsets of arguments are calculated relative to #sp with a presumption
 * that stack frame for argument passing is prepared fist. I.e. the expected
 * call preparation sequence would be 
 * @code
 *  CallSig csig();
 *  sub sp, csig.size()
 *  for i=0; i<csig.count(); i++
 *      mov [sp+csig.off(i)], arg#i
 * @endcode
 * 
 * Until #CCONV_L2R specified, the offsets are calculated as if the arguments 
 * were 'push-ed' to the stack from right-to-left. 
 * 
 * In other words, by default the 0th argument is on the top of the stack.
 *
 * With #CCONV_L2R, the least argument is on the top of the stack.
 *
 * The current implementation implies the presumption that a set of 
 * calle-save registers is the same across all calling conventions.
 */
class CallSig {
public:
    /**
     * @brief No-op. 
     *
     * @note This ctor leaves CallSig object in unpredictable state. 
     *       The #init() method \b must be called before any usage.
     */
    CallSig(void)
    {
        m_cc = (unsigned)~0;
    }
    
    /**
     * @brief Initializes CallSig object with the given arg types.
     */
    CallSig(unsigned cc, jtype ret = jvoid, 
            jtype arg0=jvoid, jtype arg1=jvoid, jtype arg2=jvoid,
            jtype arg3=jvoid, jtype arg4=jvoid, jtype arg5=jvoid)
    {
        m_cc = cc;
        m_ret_jt = ret;
        if (arg0 != jvoid)  { m_args.push_back(arg0); }
        if (arg1 != jvoid)  { m_args.push_back(arg1); }
        if (arg2 != jvoid)  { m_args.push_back(arg2); }
        if (arg3 != jvoid)  { m_args.push_back(arg3); }
        if (arg4 != jvoid)  { m_args.push_back(arg4); }
        if (arg5 != jvoid)  { m_args.push_back(arg5); }
        // No jvoid type argument can be presented.
        assert(find(m_args.begin(), m_args.end(), jvoid) == m_args.end());
        init();
    }
     
#if 0
    /**
     * Seems there is no need in such ctor. All the existing usage 
     * cases are covered by CallSig(cc, jtype*N = jvoid)
     */
    CallSig(unsigned cc, unsigned num, ...)
    {
        va_list valist;
        va_start(valist, num);
        m_args.resize(num);
        assert(sizeof(jtype)==sizeof(int));
        for (unsigned i=0; i<num; i++) {
            jtype jt = (jtype)va_arg(valist, int);
            assert(i8<=jt && jt<num_jtypes);
            m_args[i] = jt;
        }
        m_cc = cc;
        init();
    }
#endif
    /**
     * @brief Constructs and initializes CallSig object with the given 
     *        calling convention and list of args types.
     */
    CallSig(unsigned cc, const jtype ret, const vector<jtype>& args)
    {
        m_ret_jt = ret;
        init(cc, args);
    }
    
    /**
     * @brief Initializes CallSig object with the given calling convention 
     *        and list of args types.
     */
    void init(unsigned cc, const vector<jtype>& args)
    {
        m_args = args;
        m_cc = cc;
        init();
    }
    
    /**
     * @brief Returns used calling convention.
     */
    unsigned cc(void) const
    {
        return m_cc;
    }
    
    /**
     * @brief Returns true if caller must restore the stack.
     * @see CCONV_CALLER_POPS
     * @see CCONV_CDECL
     */
    bool caller_pops(void) const
    {
        return (m_cc & CCONV_CALLER_POPS);
    }
    
    /**
     * @brief Returns size (in bytes) of padding area to achieve proper alignment.  
     */
    unsigned alignment() const {
        return m_alignment;
    }
    
    /**
     * @brief Returns size (in bytes) of the stack size needed to pass args
     *        that go through the stack.
     */
    unsigned size(void) const
    {
        return m_stack;
    }
    
    /**
     * @brief Returns number of arguments.
     */
    unsigned count(void) const
    {
        assert(m_data.size() == m_args.size());
        return (unsigned) m_args.size();
    }
    
    /**
     * @brief Returns appropriate AR to pass the given argument, or ar_x 
     *        if the argument comes through the stack.
     */
    AR reg(unsigned i) const
    {
        assert(i<count());
        return m_data[i]<=0 ? ar_x : (AR)m_data[i];
    }
    
    /**
     * @params i slot number. For example on IA32 ret_reg(0) is eax, ret_reg(1) is edx.
     * @returns register which holds return value, or ar_x
     *          of the value comes through the memory.
     */
    AR ret_reg(unsigned i) const
    { 
        assert(i < 2);
        return (m_ret_reg[i] <= 0) ? ar_x : (AR)m_ret_reg[i];
    }
    
    /**
     * @returns type of return value.
     */
    jtype ret_jt() const { return m_ret_jt; }
    
    /**
     * @returns Offset (in bytes) from #sp of the given argument, or -1 if 
     *          the argument is passed on register.
     */
    int off(unsigned i) const
    {
        assert(i<count());
        return m_data[i]<=0 ? -m_data[i] : -1;
    }
    
    /**
     * @returns An Opnd to refer the given method's argument.
     *
     * If the argument is passed through the stack, then memory operand 
     * with base()==sp and appropriate displacement returned. 
     *
     * If the \c offset parameter is not 0, then it's added to the argument's
     * displacement.
     *
     * If the argument is passed through a register, then register operand 
     * returned, and \c offset parameter is ignored.
     *
     * The returned Opnd object has proper type. For \link #is_big big type 
     * \endlink the \link #jtmov appropriate supported type \endlink
     * returned.
     */
    Opnd get(unsigned i, int offset = 0) const 
    {
        AR ar = reg(i);
        jtype jtm = jtmov(jt(i));
        if (ar != ar_x) {
            return Opnd(jtm, ar);
        }
        return Opnd(jtm, sp, off(i) + offset);
    }
    
    /**
     * @brief Returns type of the argument.
     */
    jtype jt(unsigned i) const
    {
        assert(i<count());
        return m_args[i];
    }
    /**
     * @brief Tests whether given AR is used to pass an argument for the 
     *        this CallSig.
     */
    bool uses(AR ar) const 
    {
        if (ar != ar_x) {
            for (unsigned i=0; i<count(); i++) {
                if (reg(i) == ar) {
                    return true;
                }
            }
        }
        return false;
    }
private:
    /**
     * @brief Initializes CallSig object.
     *
     * Counts args offsets and required stack size.
     */
    void init();
    /**
     * @brief An info about argument types.
     */
    ::std::vector<jtype>    m_args;
    /**
     * @brief An info about argument offsets (m_data[i]<0) or registers 
     *        (m_data[i]>0 => (AR)m_data[i]).
     *
     * Though offsets are stored as negative values, they are positive 
     * offsets to #sp. The sign here is used as additional flag whether the 
     * value must be interpreted as offset (sig<=0) or as a register 
     * (sign>0).
     *
     * This implies presumption that a valid AR is alwys > 0.
     */
    ::std::vector<int>      m_data;
    
    /**
     * @brief Returns size (in bytes) of padding area to achieve proper alignment.
     */  
    unsigned                m_alignment;
    /**
     * @brief Size (in bytes) of stack frame needed to pass arguments.
     *
     * ... with all the alignment properties taken into account.
     */
    unsigned                m_stack;
    /**
     * @brief \link JET_CCONV calling convention \endlink for this
     *        CallSig object.
     */
    unsigned                m_cc;
    /**
     * @brief              
     */
    int                     m_ret_reg[2];
    jtype                   m_ret_jt;
};

/**
 * @brief CallSig for stdcall function that takes no args.
 */
extern const CallSig helper_v;
extern const CallSig platform_v;


}}; // ~namespace Jitrino::Jet

#endif      // ~__CSIG_H_INCLUDED__

