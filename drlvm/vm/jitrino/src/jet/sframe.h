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
 * @brief Declaration of StackFrame class.
 */

#if !defined(__SFRAME_H_INCLUDED__)
#define __SFRAME_H_INCLUDED__

#include "enc.h"
#include "csig.h"

namespace Jitrino {
namespace Jet {

/**
 * @brief Controls layout of the native stack frame.
 * 
 * The class StackFrame holds all the arithmetics behind layout of a native 
 * stack frame.
 *
 * Below is the stack layout, used for methods compiled by Jitrino.JET.
 *
<table>
<tr align="center">
    <td>Method/name</td><td>Purpose</td>
</tr>
<tr>
    <td></td><td></td><
</tr>


<tr>
    <td>input arg ...</td><td></td>
</tr>
<tr align="center" bgcolor="silver"><td colspan=2>fixed area</td></tr>
<tr>
    <td>StackFrame::ret()</td>
    <td><=#bp<br>Also, #sp points here at the entrance.</td>
</tr>
<tr>
    <td>
        StackFrame::spill() - beginning of the area<br>
        StackFrame::spill(AR) - address for the particular register
    </td>
    <td>The spill area used to store callee-save register. 
    Also, scratch registers may be saved here during runtime (see 
    CodeGen::gen_call_vm_restore()).
    </td>
</tr>
<tr>
    <td>StackFrame::thiz()</td>
    <td>Saved copy of \c this, when necessary. Size=STACK_SLOT_SIZE.</td>
</tr>

<tr>
    <td>StackFrame::scratch()</td>
    <td>Scratch area, used for temporary storage. Size=8 bytes</td>
</tr>

<tr>
    <td>StackFrame::dbg_scratch()</td>
    <td>Temporary area for debugging utilities needs. E.g. an #sp is stored
    here when method compiled with DBG_CHECK_STACK. Size=STACK_SLOT_SIZE.
    </td>
</tr>

<tr>
    <td>StackFrame::info_gc_regs()</td>
    <td>GC map for GR registers. Size=4 bytes.</td>
</tr>

<tr>
    <td>StackFrame::info_gc_stack_depth()</td>
    <td>GC stack depath of operand stack. Size=4 bytes.</td>
</tr>

<tr align="center" bgcolor="silver"><td colspan=2>variable size area</td></tr>

<tr>
    <td>StackFrame::info_gc_locals()</td>
    <td>GC map for local variables. Size=words(num_locals)</td>
</tr>

<tr>
    <td>StackFrame::info_gc_args()</td>
    <td>GC map for input arguments. Size=words(num_in_slots)</td>
</tr>

<tr>
    <td>StackFrame::info_gc_stack()</td>
    <td>GC map for operand stack. Size=words(max_stack)</td>
</tr>

<tr>
    <td>StackFrame::local(unsigned)</td>
    <td>Array of local varibales of the method. 
    Size=STACK_SLOT_SIZE*num_locals.</td>
</tr>

<tr>
    <td>
    StackFrame::stack_bot()<br>StackFrame::stack_slot(unsigned)<br>
    StackFrame::stack_max()
    </td>
    <td>Operand stack of the method. StackFrame::stack_bot() points to 
    the very beginning of the stack. StackFrame::stack_slot(unsigned) 
    address a particular slot in the stack, and  StackFrame::stack_max 
    points to the very last possible slot.
    Size=STACK_SLOT_SIZE*max_stack.</td>
</tr>

<tr>
    <td>StackFrame::unused()</td>
    <td>Points to the first unused memory address (of STACK_SLOT_SIZE
    size) beyond the frame.<br>
    Note that though the memory is named 'unused' it may still belog to the
    frame - for example, if the frame is aligned according to calling 
    convention.
    </td>
</tr>

</table>

 *
 *
 *
 * @todo On Intel 64 the fixed size of the frame is quite big - the
 * number of registers is much bigger than on IA-32 and each register is
 * 8 bytes wide - so the spill area is long. As result, the length of 
 * the fixed-size area does not fit into +/-127. This makes generated code 
 * to use addressing with 32 bit displacements [rbp+I_32] which make code 
 * bigger. If we point base pointer (in the method's prolog) not to the 
 * beginning of the frame but, say, into the middle between 
 * locals and stack, then we'll be able to address most of them with 8bit 
 * displacement. Seems the best place to correct the offset would be 
 * CodeGen::voff(), where all the offsets come through.
 */
class StackFrame {
public:
    /**
     * @brief Initializes an instance of StackFrame.
     *
     * @param _num_locals - number of slots occupied by local variables in 
     *        Java method's frame
     * @param _max_stack - max depth of Java operand stack
     * @param _in_slots - number of slots occupied by input args in Java 
     *        on Java operand stack
     * @see #init
     */
    StackFrame(unsigned _num_locals, unsigned _max_stack, unsigned _in_slots)
    {
        m_num_locals = _num_locals;
        m_max_stack = _max_stack;
        m_in_slots = _in_slots;
    }

    /**
     * @brief Noop.
     */
    StackFrame() {}

    /**
     * @brief Initializes fields of this StackFrame instance.
     *
     * @param _num_locals - number of slots occupied by local variables in 
     *        Java method's frame
     * @param _max_stack - max depth of Java operand stack
     * @param _in_slots - number of slots occupied by input args in Java 
     *        on Java operand stack
     */
    void init(unsigned _num_locals, unsigned _max_stack, unsigned _in_slots)
    {
        m_num_locals = _num_locals;
        m_max_stack = _max_stack;
        m_in_slots = _in_slots;
    }
public:
    /**
     * @brief Returns number of slots occupied by local variables.
     *
     * @return number of slots occupied by local variables in Java method's 
     *         frame
     */
    unsigned get_num_locals(void) const
    {
        return m_num_locals;
    }
    
    /**
     * @brief Returns max stack depth of Java method's operand stack
     * @return max stack depth
     */
    unsigned get_max_stack(void) const
    {
        return m_max_stack;
    }
    
    /**
     * @brief Returns number of slots occupied by input args, in Java 
     *        method's operand stack
     * @return number of slots occupied by input args
     */
    unsigned get_in_slots(void) const
    {
        return m_in_slots;
    }
public:

    int inargs(void) const {
        return ret() + STACK_SLOT_SIZE;
    }
    
    int ret(void) const
    {
        return 0;
    }

    /**
     * @brief Returns size of the stack frame.
     * 
     * The size is aligned if \link #CCONV_MANAGED managed calling 
     * convention \endlink assumes so.
     * 
     * @return size of the stack frame
     */
    unsigned size(void) const
    {
        unsigned sz = -unused();
        unsigned alignment = (CCONV_MANAGED & CCONV_STACK_ALIGN_HALF16) ? CCONV_STACK_ALIGN16
            : CCONV_MANAGED & CCONV_STACK_ALIGN_MASK;
        alignment = (alignment == 0) ? CCONV_STACK_ALIGN4 : alignment; 
        return ((sz + (alignment - 1)) & ~(alignment - 1));
    }

    //
    // First, a fixed area - the same size for all methods...
    //
    
    int spill(void) const
    {
        return ret()-(int)(gr_num*STACK_SLOT_SIZE + fr_num*8);
    }

    int spill(AR ar) const
    {
        if (is_gr(ar)) {
            return spill() + STACK_SLOT_SIZE*gr_idx(ar);
        }
        assert(is_fr(ar));
        return spill() + STACK_SLOT_SIZE*gr_num + 8*fr_idx(ar);
    }

    int thiz(void) const
    {
        return spill() - STACK_SLOT_SIZE;
    }

    int scratch(void) const
    {
        return thiz() - 128/8;
    }

    int dbg_scratch(void) const
    {
        return scratch() - STACK_SLOT_SIZE;
    }
    //
    // Static area for JVMTI needs
    //

    // An area to preserve all the registers for JVMTI's PopFrame
    int jvmti_regs_spill_area(void) const
    {
        return dbg_scratch() - Encoder::get_all_regs_size();
    }
    int jvmti_register_spill_offset(AR ar) const
    {
        if (is_gr(ar)) {
            return jvmti_regs_spill_area() + STACK_SLOT_SIZE*gr_idx(ar);
        }
        assert(is_fr(ar));
        return jvmti_regs_spill_area() + STACK_SLOT_SIZE*gr_num + 8*fr_idx(ar);
    }
    // ~Static JVMTI
    //
    int info_gc_regs(void) const
    {
        assert(words(gen_num_calle_save()) == 1);
        //return dbg_scratch() - sizeof(unsigned);
        return jvmti_regs_spill_area();
    }
    
    /**
     * @brief Returns offset of stack depth in the GC info.
     */
    int info_gc_stack_depth(void) const
    {
        return info_gc_regs()-(int)sizeof(int);
    }
    //
    // ... second, an area which depends on locals count and stack size.
    //
    int info_gc_locals(void) const
    {
        return info_gc_stack_depth() - sizeof(int)*words(m_num_locals);
    }

    int info_gc_args(void) const
    {
        return info_gc_locals() - sizeof(int)*words(m_in_slots);
    }
    
    int info_gc_stack(void) const
    {
        return info_gc_args() - sizeof(int)*words(m_max_stack);
    }

    int local(unsigned i) const
    {
        assert(i<m_num_locals || (i==0)); //XXX comment out
        return info_gc_stack() - (m_num_locals - i)*STACK_SLOT_SIZE;
    }
    
    int stack_bot(void) const
    {
        return local(0) - STACK_SLOT_SIZE;
    }
    
    int stack_slot(unsigned slot) const
    {
        assert(slot<m_max_stack);
        return stack_bot() - slot*STACK_SLOT_SIZE;
    }
    
    int stack_max(void) const
    {
        return stack_bot() - (m_max_stack - 1)*STACK_SLOT_SIZE;
    }
    
    int unused(void) const
    {
        return stack_max() - STACK_SLOT_SIZE;
    }
    //
private:
    unsigned m_num_locals;
    unsigned m_max_stack;
    unsigned m_in_slots;
};


}}; // ~namespace Jitrino::Jet

#endif      // ~__SFRAME_H_INCLUDED__


