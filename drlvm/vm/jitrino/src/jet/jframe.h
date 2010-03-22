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
#if !defined(__JFRAME_H_INCLUDED__)
#define __JFRAME_H_INCLUDED__

/**
 * @file
 * @brief JFrame class declaration.
 */
 
#include "jdefs.h"
#include <vector>
#include <assert.h>
#include "val.h"

namespace Jitrino {
namespace Jet {

/**
 * @brief Emulates Java method's frame (operand stack and local variables)
 *        and its operations.
 *
 * @todo Seems not much reasons to have it as separate class. Move all the 
 *       data into BBState and drop off the JFrame?
 */
class JFrame {
public:
    /**
     * @brief No op.
     */
    JFrame()
    {
        m_stack = m_vars = NULL;
        max_stack = num_locals = 0;
        m_top = -1;
    };
    ~JFrame()
    {
        if (m_stack) {
            delete[] m_stack;
        }
        if (m_vars) {
            delete[] m_vars;
        }
    }
    JFrame(const JFrame& that)
    {
        m_stack = m_vars = NULL;
        max_stack = num_locals = 0;
        m_top = -1;
        *this = that;
    }
public:
    JFrame& operator=(const JFrame& that)
    {
        assert(that.m_stack != NULL);
        assert(that.m_vars != NULL);
        if (m_stack == NULL) {
            max_stack = that.max_stack;
            num_locals = that.num_locals;
            m_stack = new Val[max_stack+1];
            m_vars = new Val[num_locals];
        }
        else {
            assert(max_stack == that.max_stack);
            assert(num_locals== that.num_locals);
        }
        memcpy(m_stack, that.m_stack, sizeof(Val)*(max_stack+1));
        memcpy(m_vars, that.m_vars, sizeof(Val)*(num_locals));
        m_top = that.m_top;
        return *this;
    }
    /**
     * @brief Initializes empty JFrame with the given max stack depth, 
     *        slots allocated for local variables and registers allocated
     *        for all this.
     */
    void    init(unsigned stack_max, unsigned var_slots)
    {
        assert(m_stack == NULL && m_vars == NULL);

        max_stack = stack_max;
        num_locals = var_slots;
        m_stack = new Val[max_stack+1];
        m_vars = new Val[num_locals];
        m_top = -1;
    }
    /**
     * @brief Converts slot index into depth.
     */
    unsigned    slot2depth(unsigned slot) const
    {
        assert(slot<size());
        return size()-slot-1;
    }
    /**
     * @brief Converts depth into slot index.
     */
    unsigned    depth2slot(unsigned depth) const
    {
        assert(depth<size());
        return size()-depth-1;
    }
    /**
     * @brief Returns current stack depth.
     * @return current stack depth
     */
    unsigned    size(void) const
    {
        return m_top+1;
    }
    
    /**
     * @brief Returns max stack size.
     */
    unsigned max_size(void) const
    {
        return max_stack;
    }
    
    /**
     * @brief Returns number of local variables.
     */
    unsigned    num_vars(void) const
    {
        return num_locals; //m_vars.size();
    }
    
    /**
     * @brief Returns type of a slot at the given depth.
     */
    jtype top(unsigned depth = 0) const
    {
        assert(depth<size());
        return m_stack[size()-depth-1].type();
    }
    /**
     * @brief Returns slot at the given depth.
     */
    Val&   dip(unsigned depth)
    {
        assert(depth<size());
        return m_stack[size()-depth-1];
    }
    /**
     * @brief Pushes the given jtype into the stack.
     */
    void    push(jtype jt);
    /**
     * @brief Pops out an item from the stack.
     *
     * The given \c jt must be same as the item on top of the stack.
     */
    void    pop(jtype jt);
    /**
     * @brief Pops 2 slots out of the stack - either a single wide item 
     *        (#dbl64, #i64) or 2 one-slot items (#i32, #jobj, etc).
     */
    void    pop2(void);
    /**
     * @brief Pops out items specified by the array.
     *
     * Item of args[args.size()-1] is taken away from the top, and so on.
     */
    void    pop_n(const ::std::vector<jtype>& args);
    /**
     * @brief Returns stack slot at the given \b position (not depth).
     */
    Val&    at(unsigned slot)
    {
        assert(slot<size());
        return m_stack[slot];
    }
    
    /**
     * @brief Returns local variable at the given position.
     */
    Val&    var(unsigned slot)
    {
        assert(slot<num_vars()); //m_vars.size());
        return m_vars[slot];
    }
private:
    /**
     * @brief Current stack pointer. -1 if stack is empty.
     */
    int     m_top;
    /**
     * @brief Operand stack data.
     */
    Val*    m_stack;
    /**
     * @brief Local variables data.
     */
    Val*    m_vars;
    unsigned max_stack;
    unsigned num_locals;
};

}}; // ~namespace Jitrino::Jet

#endif      // ~__JFRAME_H_INCLUDED__
