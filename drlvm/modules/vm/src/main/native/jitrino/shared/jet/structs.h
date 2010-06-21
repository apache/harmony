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
 * @brief Various simple structures for CodeGen and Compiler needs. 
 *
 * Mostly structures that defer better classification are collected here.
 */

#if !defined(__BBSTATE_H_INCLUDED__)
#define __BBSTATE_H_INCLUDED__


#include "jdefs.h"
#include "jframe.h"
//#include "open/vm.h"
#include "jit_import.h"
#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"

namespace Jitrino {
namespace Jet {

/**
 * @brief Reads out a 4-byte integer stored in the Java's format - 'most
 *        significant bytes first'.
 * @param pbc - a buffer to read from
 * @return integer
 */
inline int bc_int(const unsigned char * pbc)
{
    return (int)(((*(pbc+0))<<24) | ((*(pbc+1))<<16) | 
                ((*(pbc+2))<<8) | ((*(pbc+3))<<0));
}

/**
 * @brief Simple smart pointer.
 *
 * A trivial smart pointer implementation for dynamically allocated arrays.
 * Allocated memory is deallocated in destructor.
 *
 * Does not support neither copying, nor resizing of array. Used to avoid 
 * head ache about controlling raw pointers of allocated memory. 
 *
 * Used for arrays and includes bounds check in debug build - no bound 
 * checks performed in release one.
 */
template<class SomeType> class SmartPtr {
public:
    /**
     * @brief Initializes empty object.
     */
    SmartPtr(void)
    {
        m_data = NULL;
    }
    /**
     * @brief Allocates memory for \c count elements of SomeType.
     *
     * May zero the allocated memory if \c clear parameter is \c true.
     */
    void alloc(unsigned count, bool clear = false)
    {
        m_data = new SomeType[count];
        m_count = count;
        if (clear) {
            memset(m_data, 0, bytes());
        }
    }
    /**
     * @brief Returns number of elements in the allocates array.
     */
    unsigned count(void) const
    {
        return m_count;
    }

    /**
     * @brief Provides random access to an item by index.
     *
     * Bounds check is preformed only in debug build.
     */
    SomeType& operator[](unsigned idx)
    {
        assert(idx<m_count);
        return m_data[idx];
    }

    /**
     * @brief Provides a direct access to the allocated array.
     */
    SomeType* data(void)
    {
        return m_data;
    }

    /**
     * @brief Returns size of allocated memory, in bytes.
     */
    unsigned bytes(void)
    {
        return sizeof(SomeType)*m_count;
    }

    /**
     * @brief Fress allocated resources.
     */
    ~SmartPtr()
    {
        delete[] m_data;
    }
private:
    /**
     * @brief Pointer to the allocated memory.
     */
    SomeType *  m_data;
    /**
     * @brief Number of elements in the allocated array.
     */
    unsigned    m_count;
    /**
     * @brief Disallow copying.
     */
    SmartPtr(const SmartPtr&);
    /**
    * @brief Disallow copying.
    */
    SmartPtr& operator=(const SmartPtr&);
};



/**
 * @brief Decoded byte code instruction.
 */
struct JInst {
    unsigned id;
    /**
     * @brief Number of references on this instruction.
     *
     * The number of references is number of incoming control flow edges.
     * Regular instructions have \c ref_count==1, basic block leaders may 
     * have \c ref_count > 1. \c ref_count == 0 means the instruction is 
     * dead code.
     */
    unsigned ref_count;
    /**
     * @brief Program counter of this instruction.
     */
    unsigned        pc;
    /**
     * @brief Program counter of next instruction.
     */
    unsigned        next;
    /**
     * @brief Instruction's opcode.
     */
    JavaByteCodes   opcode;
    /**
     * @brief Instruction's flags.
     */
    unsigned        flags;
    /**
     * @brief Value of the first instrution's operand, if applicable.
     * 
     * @note If instruction has no operands, the value of 'op0' is undefined.
     */
    int             op0;
    
    /**
     * @brief Value of the second instrution's operand, if applicable.
     *
     * @note If instruction has no second operands, the value of 'op1' is
     *       undefined.
     */
    int             op1;
    
    /**
     * @brief Address of data (the padding bytes skipped) for the switch
     *        instructions.
     *
     * @note For instructions other than switch, the value is undefined.
     */
    const unsigned char*    data;

    /**
     * @brief Returns number of targets for a branch or switch instruction.
     *
     * For branches it's always 1. 
     * For switches the default target is not included in the value.
     * For other instructions returns 0.
     */
    unsigned get_num_targets(void) const
    {
        if (is_branch())   { return 1; };
        if (opcode == OPCODE_TABLESWITCH) {
            return high() - low() + 1;
        }
        if (opcode != OPCODE_LOOKUPSWITCH) {
            return 0;
        }
        // 4 here is the number of bytecode bytes between the values - comes
        // from the JVM Spec
        return bc_int(data + 4);    // +4 - skip the 'defaultbyte*4'
    }

    /**
     * @brief Returns a PC value for the target 'i'.
     *
     * Must be only invoked for the branch or switch instructions and 'i' 
     * must be less or equal to gen_num_targets().
     * For other cases the behavior is unpredictable.
     */
    unsigned get_target(unsigned i) const
    {
        assert(i < get_num_targets());
        if (is_branch()) {
            return (unsigned short)(((unsigned short)pc) + 
                                    ((unsigned short)op0));
        }
        if (opcode == OPCODE_TABLESWITCH) {
            // '4+4+4' - skip defaultbyte, lowbyte and hightbyte
            const int * poffsets = (int*)(data + 4 + 4 + 4);
            return pc + bc_int((unsigned char*)(poffsets + i));
        }
        assert(opcode == OPCODE_LOOKUPSWITCH);
        // '4+4' - skip defaultbyte and npairs
        const int * ppairs = (int*)(data+4+4);
        return pc + bc_int((unsigned char*)(ppairs + i*2 + 1));
    }
    /**
     * @brief Returns default target for switch instructions.
     * 
     * Must only be invoked for TABLESWITCH or LOOKUPSWITCH instructions.
     */
    unsigned get_def_target(void) const
    {
        assert(opcode == OPCODE_TABLESWITCH || opcode == OPCODE_LOOKUPSWITCH);
        unsigned offset = bc_int(data);
        return (unsigned short)(((unsigned short)pc)+((unsigned short)offset));
    }
    
    /**
     * @brief Returns size of data (in bytes) occupied by LOOKUPSWITCH or 
     *        TABLESWITCH datas.
     *
     * Must only be invoked for TABLESWITCH or LOOKUPSWITCH instructions.
     */
    unsigned get_data_len(void) const
    {
        if (opcode == OPCODE_TABLESWITCH) {
            // 4*3 = defaultbyte,lowbyte,highbyte ; +jmp offsets
            return 4*3 + 4*(high()-low()+1);
        }
        assert(opcode == OPCODE_LOOKUPSWITCH);
        // defaultbyte + npairs + (4*2)*npairs
        return (4 + 4 + 4*2 * get_num_targets());
    }
    
    /**
     * @brief Returns minimum value of TABLESWITCH instruction.
     *
     * Must only be invoked for TABLESWITCH instructions.
     */
    int low(void) const
    {
        assert(opcode == OPCODE_TABLESWITCH);
        // 4 here is the number of bytecode bytes between the values - comes
        // from the JVM Spec
        return bc_int(data + 4);
    }
    
    /**
     * @brief Returns maximum value of TABLESWITCH instruction.
     *
     * Must only be invoked for TABLESWITCH instructions.
     */
    int high(void) const
    {
        assert(opcode == OPCODE_TABLESWITCH);
        // 4 here is the number of bytecode bytes between the values - comes
        // from the JVM Spec
        return bc_int(data + 4+4);
    }
    
    /**
     * @brief Returns Nth key of LOOKUPSWITCH instruction.
     *
     * Must only be invoked for LOOKUPSWITCH instructions.
     */
    int key(unsigned i) const
    {
        assert(opcode == OPCODE_LOOKUPSWITCH);
        // skip defaultbyte(+4), npairs(+4) and 'i' pairs
        return bc_int(data+4+4 + i*2*4);
    }
    
    /**
     * @brief Tests whether the instruction is branch - either conditional 
     *        or not. JSR-s are also considered branches, but not SWITCH
     *        instructions.
     */
    bool is_branch(void) const
    {
        return (OPCODE_IFEQ <= opcode && opcode <= OPCODE_JSR) ||
               (OPCODE_IFNULL <= opcode && opcode <= OPCODE_JSR_W);
    }
    /**
     * @brief Tests whether instruction is either TABLESWITCH or 
     *        LOOKUPSWITCH.
     */
    bool is_switch(void) const
    {
        return (opcode == OPCODE_TABLESWITCH || 
                opcode == OPCODE_LOOKUPSWITCH);
    }
    /**
     * @brief Tests whether instruction is JSR[_W].
     */
    bool is_jsr(void) const
    {
        return opcode == OPCODE_JSR || opcode == OPCODE_JSR_W;
    }
    
    /**
     * @brief Tests whether the given flag (or set of flags) is set.
     * @see OPF_
     */
    bool is_set(unsigned opf_flag) const
    {
        return (flags & opf_flag);
    }
    
    /**
     * @brief Tests whether instruction has single successor.
     */
    bool single_suc(void) const
    {
        // GOTO is the case as well as non-branch instructions with 
        // fall through pass. 
        return (get_num_targets()==0 && !is_set(OPF_DEAD_END)) ||
                opcode == OPCODE_GOTO || opcode == OPCODE_GOTO_W;
               
    }
};

/**
 * @brief Information about exception handler.
 */
struct HandlerInfo {
    /**
     * @brief Start PC of bytecode region protected by this exception handler
     *        (inclusive).
     */
    unsigned     start;
    /**
     * @brief End PC of bytecode region protected by this exception handler
     *        (exclusive).
     */
    unsigned     end;
    /**
     * @brief Entry point PC of exception handler.
     */
    unsigned     handler;
    /**
     * @brief Type of exception handled by this handler. 0 for 'any type'.
     */
    unsigned     type;
    /**
     * @brief Class_Handle of the exception class for this handler.
     */
    Class_Handle klass;
    /**
     * @brief IP for #start.
     */
    char *       start_ip;
    /**
     * @brief IP for #end.
     */
    char *       end_ip;
    /**
     * @brief IP for #handler.
     */
    char *       handler_ip;
};


/**
 * @brief General info about basic block
 */
struct BBInfo {
    BBInfo()
    {
        start = last_pc = next_bb = 0;
        code_size = 0;
        ehandler = false;
        jsr_target = false;
        processed = false;
    }
    /**
     * @brief Very first bytecode instruction - the basic block's leader.
     */
    unsigned    start;
    
    /**
     *@brief Last bytecode instruction which belongs to this basic block.
     */
    unsigned    last_pc;
    
    /**
     * @brief Leader of the next (layout successor) basic block.
     * 
     * Actually, the next BB's leader can be obtained by a call to 
     * Compiler::fetch(last_pc). The redundancy is intentional to avoid 
     * this additional call.
     */
    unsigned    next_bb;
    
    /**
     * An offset of the BB's code before code layout.
     * If the BB has a prolog, then ipoff points to the prolog code.
     */
    unsigned    ipoff;
    /**
     * @brief Total size of native code of the basic block.
     */
    unsigned    code_size;
    /**
     * @brief Code address after layout (the final and real address).
     */
    char *  addr;
    /**
     * \b true if the basic block servers as a target for at least one
     * JSR instruction.
     */
    bool    jsr_target;
    /**
     * \b true if this basic block is a catch handler.
     *
     * Normally this means that is has an additional small prolog
     * before a code which is generated for its first bytecode.
     */
    bool    ehandler;
    /**
     * \b true if the basic block was processed in 
     * Compiler::comp_gen_code_bb().
     *
     * Used to avoid recursion and to detect unreachable code 
     * at the stage of code layout.
     */
    bool    processed;
};

typedef map<unsigned, BBInfo>    BBMAP;

/**
 * General info and various attributes of a method.
 */
struct MethInfo
{
    /**
     * Initializes emty MethInfo.
     *
     * init() must be called before a first usage of MethInfo instance.
     */
    MethInfo()
    {
        m_method = NULL;
        m_klass = NULL;
        m_kname = m_mname = m_sig = NULL;
    }
    
    /** Initializes MethInfo structure. */
    void init(Method_Handle meth)
    {
        m_method = meth;
        m_klass = method_get_class(m_method);
    }
    
    /** Returns name of the method. */
    const char* meth_name(void) 
    {
        if (NULL==m_mname) {
            m_mname = method_get_name(m_method);
        }
        return m_mname;
    }
    
    /** Returns name of method's class. */
    const char* meth_kname(void) 
    {
        if (NULL==m_kname) {
            m_kname = class_get_name(m_klass);
        }
        return m_kname;
    }
    
    /** Returns method's signature. */
    const char* meth_sig(void) 
    {
        if (NULL==m_sig) {
            m_sig = method_get_descriptor(m_method);
        }
        return m_sig;
    }
    
    /** Returns method fully qualified name, without signature.*/
    const char* meth_fname(void)
    {
        if (m_fname.empty()) {
            m_fname = meth_kname();
            m_fname += "::";
            m_fname += meth_name();
        }
        return m_fname.c_str();
    }
    
    /** Returns method fully qualified name, including signature.*/
    const char* meth_fname_sig(void)
    {
        if (m_fname_sig.empty()) {
            m_fname_sig = meth_fname();
            m_fname_sig += meth_sig();
        }
        return m_fname_sig.c_str();
    }
    /** Returns number of exception handlers in the method. */
    unsigned meth_num_handlers(void) const
    {
        return method_get_exc_handler_number(m_method);
    }
    
    /** Tests whether the method is synchronized. */
    bool meth_is_sync(void) const
    {
        return method_is_synchronized(m_method);
    }
    /** Tests whether the method is static. */
    bool meth_is_static(void) const
    {
        return method_is_static(m_method);
    }
    /** 
     * Tests whether the method is synchronized and not static (instance).
     */
    bool meth_is_sync_inst(void) const
    {
        return meth_is_sync() && !meth_is_static();
    }
    /** Tests whether the method is constructor. */
    bool meth_is_ctor(void)
    {
        return !strcmp(meth_name(), "<init>");
    }
    /** Tests whether the method is constructor of Exception. */
    bool meth_is_exc_ctor(void)
    {
        return class_is_throwable(m_klass) && meth_is_ctor();
    }
    
protected:
    /** Method handle. */
    Method_Handle   m_method;
    /** Class handle. */
    Class_Handle    m_klass;
private:
    /** Class name. */
    const char*     m_kname;
    /** Method name. */
    const char*     m_mname;
    /** Method signature. */
    const char*     m_sig;
    /** Method fully qualified name, without signature.*/
    string  m_fname;
    /** Method fully qualified name, including signature.*/
    string  m_fname_sig;
};

/**
 * @brief Info about current state of a register in BBState.
 */
struct RegInfo {
    /**
     * @brief Number of references.
     */
    unsigned refs;
    /**
     * @brief Number of locks.
     */
    unsigned locks;
    /**
     * @brief A value currently known to be loaded into the register.
     */
    Val      val;
};


/**
 * @brief State of a basic block during code generation.
 */
class BBState {
public:
    BBState()
    {
        clear();
        m_last_fr = fr0;
        m_last_gr = gr0;
    }
    
    void clear(void)
    {
        seen_gcpt = false;
        stack_depth = NOTHING;
        stack_mask = 0;
        stack_mask_valid = false;
    }
    /**
     * @brief Current state of mimic frame for the BB.
     */
    JFrame      jframe;
    /**
     * @brief State of registers.
     */
    RegInfo     m_regs[ar_num];
    /**
     * @brief Recently stored stack depth. 
     *
     * Used to eliminate unnecessary stack depth updates.
     */
    unsigned stack_depth;
    /**
     * @brief Recently stored GC mask for stack.
     *
     * Used to eliminate unnecessary stack GC map updates.
     *
     * Only single word of the GC mask is stored - it's enough for most 
     * applications.
     */
    unsigned stack_mask;
    /**
     * @brief Last used (allocated) GR register.
     */
    AR      m_last_gr;
    /**
     * @brief Last used (allocated) FR register.
     */
    AR      m_last_fr;
    /**
     * @brief \b true if #stack_mask contains a 'valid' (that is which was 
     *        really written).
     *
     * The #stack_depth field may contain limited set of values and thus 
     * the field itself may carry info whether it contains 'non-initialized'
     * flag (#NOTHING) or the real stack depth (any other value).
     *
     * In opposite, for the #stack_mask any value is valid combination. Thus
     * it's necessary to indicate whether the stack_mask was initialized or 
     * not. Here is this flag intended for.
     */
    unsigned    stack_mask_valid;
    /**
     * @brief 'true' if there was at least one GC point in the basic block.
     *
     * Set during the code generation and is used to reduce unnecessary back 
     * branch polling code.
     */
    unsigned    seen_gcpt;
    
};

}}; // ~namespace Jitrino::Jet2

#endif      // ~__BBSTATE_H_INCLUDED__

