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
  * @brief Declaration of Compiler class.
  */
 
#if !defined(__COMPILER_H_INCLUDED__)
#define __COMPILER_H_INCLUDED__


#include "jframe.h"
#include "rt.h"
#include "cg.h"

#include "open/rt_types.h"

#include <assert.h>
#include <vector>
#include <map>
#include <string>
#include <stack>
#include <bitset>


namespace Jitrino {
namespace Jet {

/**
 * @brief The name says it all.
 * 
 * The constant used to preallocate memory in code stream to avoid 
 * multiple reallocation during code gen stage.
 * 
 * @todo Was calculated for IA-32 long time ago, may require tuning for 
 * IA-64 and Intel-64.
 */
#define NATIVE_CODE_SIZE_2_BC_SIZE_RATIO        (10)
#define NATIVE_STACK_SIZE_2_THROW_SYN_EXC       (2)

/**
 * The class represents a JIT compiler for the Java bytecode under DRLVM
 * environment.
 */
class Compiler : public CodeGen {
public:
    Compiler(JIT_Handle jh)
    {
        m_hjit = jh;
        m_bEmulation = false;
        hasSOEHandlers = false;
    }
    /**
     * @brief Main compilation routine.
     */
    JIT_Result compile(Compile_Handle ch, Method_Handle method, 
                       const OpenMethodExecutionParams& params);
    /**
     * @brief Adds a flag to default compilation flags.
     * @see JMF_ 
     */
    static void addDefaultFlag(unsigned flag)
    {
        defaultFlags |= flag;
    }
    /**
     * @brief Removes a flag from default compilation flags.
     * @see JMF_ 
     */
    static void deleteDefaultFlag(unsigned flag)
    {
        defaultFlags &= ~flag;
    }
    /**
     * If not NOTHING, then only methods with compilation id more or equal 
     * to this id are compiled.
     */
    static unsigned g_acceptStartID;
    /**
     * If not NOTHING, then only methods with compilation id less or equal 
     * to this id are compiled.
     */
    static unsigned g_acceptEndID;
    /**
     * If not NOTHING, then methods with compilation id more or equal 
     * to this id are rejected (JIT_FAILURE returned) without compilation.
     */
    static unsigned g_rejectStartID;
    /**
     * If not NOTHING, then methods with compilation id less or equal 
     * to this id are rejected (JIT_FAILURE returned) without compilation.
     */
    static unsigned g_rejectEndID;
    
    /**
     * @brief Dumps out the basic blocks structure.
     * For debugging only.
     */
    void dbg_dump_bbs(void);
    /**
     * @brief Dumps out disassembled piece of code.
     * For debugging only.
     */
    void dbg_dump_code(const char * code, unsigned length, const char * name);
    /**
     * @brief Dumps out disassembled the whole code of the method, mixed with
     *        appropriate bytecode.
     * For debugging only.
     *
     * The code must be already generated and available for VM - 
     * \b method_get_code_block_jit is used to obtain the code.
     */
    void dbg_dump_code_bc(const char * code, unsigned codeLen);
    /**
     * @brief Converts the JInst into the human-readable string. 
     * For debugging only.
     *
     * @param jinst - instruction to be presented as string.
     * @param show_names - if \b true, then the string contains symbolic 
     *        names, otherwise only constant pool indexes.
     */
    ::std::string toStr(const JInst& jinst, bool show_names);
    /**
     * @brief Prints out a string 'compilation started'.
     * For debugging only.
     * @see DBG_TRACE_SUMM
     */
    void dbg_trace_comp_start(void);
    /**
     * @brief Prints out a string 'compilation finished', with a reason why 
     *        compilation failed, if \c success is \b false.
     * For debugging only.
     * @see DBG_TRACE_SUMM
     */
    void dbg_trace_comp_end(bool success, const char * reason);
    /**
     * @brief Generates code to ensure stack integrity at the beginning 
     *        of a basic block at runtime.
     *
     * This is used to ensure stack integrity after branches which can not be
     * controlled by #gen_dbg_check_stack.
     *
     * @note For debug checks only.
     */
    void gen_dbg_check_bb_stack(void);
    /**
     * @brief If not #NOTHING, then software breakpoint inserted before the
     * code at the specified PC of the generated code.
     *
     * For debugging only.
     */
    unsigned dbg_break_pc;
    
private:
    /**
     * @brief Default flags for compilation.
     * @see JMF_ 
     */
    static unsigned defaultFlags;

    /**
     * @brief Decodes bytecode instructions, finds basic blocks and 
     * collects local vars usage info.
     */
    void comp_parse_bytecode(void);
    
    /**
     * @brief Helper function for comp_parse_bytecode().
     * 
     * Creates a BBInfo record for the given \c pc or returns an one.
     */
    BBInfo& comp_create_bb(unsigned pc);
    
    /**
     * @brief Performs global register allocation.
     */
    void comp_alloc_regs(void);

    /**
     * @brief Generates code for a given basic block.
     * 
     * When pc=0, also generates prolog code.
     *
     * If \c jsr_lead is not NOTHING, which means that this basic block
     * is part of JSR subroutine, then the final state of the stack on a RET
     * instruction will be stored for this jsr_lead and then reused if there
     * are several JSR instructions point to the same (jsr_lead) block.
     *
     * @param pc - program counter of the basic block
     */
    void comp_gen_code_bb(unsigned pc);
    /**
     * @param pc - program counter of the basic block
     * @param parentPC - program counter of basic block which is 
     * predecessor of \c pc block. This is the block the BBState is 
     * inherited from.
     * @param jsr_lead - PC of the beginning of the JSR block, we're 
     *        currently in, or #NOTHING we're not in JSR block.
     */
    bool comp_gen_insts(unsigned pc, unsigned parentPC, unsigned jsr_lead); 
    
    /**
     * @brief Performs layout of the native code, so it become same as byte 
     *        code layout.
     * @param prolog_ipoff - offset of prolog code m_codeStream/
     * @param prolog_size - size of prolog code, in bytes.
     */
    void comp_layout_code(unsigned prolog_ipoff, unsigned prolog_size);

    /**
    * @brief Resolves method's exception handlers.
    * 
    * @note Does call resolve_class() and thus must not be used when the 
    *       lock protecting method's data is locked.
    * @return \c true if resolution was successful, \c false otherwise
    */
    bool comp_resolve_ehandlers(void);
    
    /**
     * @brief Registers method's exception handlers.
     *
     * @note Does \b not call resolve_class() and may be used when a lock 
     * protecting method's data is locked.
     */
    void comp_set_ehandlers(void);
    /**
     * @brief Fills out a native addresses of an exception handler and 
     *        a block it protects.
     * @param[in,out] hi - info about handler to process. 
     */
    bool comp_hi_to_native(HandlerInfo& hi);

    /**
     * @brief Performs code patching.
     * 
     * The code patching is to finalize addresses for relative-addressing 
     * instructions like ÎMP, and for instructions which refer to a data 
     * allocated after the instructions generated (indirect JMPs for 
     * LOOKUP/TABLE-SWITCH).
     */
    void comp_patch_code(void);
    /**
     * @brief Fetches out and decodes a bytecode instruction starting 
     *        from the given 'pc'.
     * @return PC of the next instruction, or NOTHING if end of byte code 
     *         reached.
     */
    unsigned fetch(unsigned pc, JInst& jinst);
    /**
     * @brief Invokes appropriate handle_ik_ method to generate native code.
     */
    void handle_inst(void);
    /** @brief a helper method for #handle_inst() */
    void handle_ik_meth(const JInst& jinst);
    /** @brief a helper method for #handle_inst() */
    void handle_ik_obj(const JInst& jinst);
    /** @brief a helper method for #handle_inst() */
    void handle_ik_stack(const JInst& jinst);
    /** @brief a helper method for #handle_inst() */
    void handle_ik_a(const JInst& jinst);
    /** @brief a helper method for #handle_inst() */
    void handle_ik_cf(const JInst& jinst);
    /** @brief a helper method for #handle_inst() */
    void handle_ik_cnv(const JInst& jinst);
    /** @brief a helper method for #handle_inst() */
    void handle_ik_ls(const JInst& jinst);

    //
    // CodeGeneration stuff. Most of these functions are like CodeGen's 
    // ones, but they deal with basic blocks, so it's better to place 
    // them here in Compiler.
    //
    /**
     * @brief Generates method's prolog code.
     */
    void gen_prolog(void);
    /**
     * @brief Generates method's epilogue (on RETURN instructions) code.
     */
    void gen_return(const CallSig& cs);
    
    /**
     * @brief Prepares BBState as it was left by gen_bb_leave().
     * @see gen_bb_leave
     */
    void gen_bb_enter(void);
    /**
     * @brief Generates code to leave current basic block.
     *
     * The leave code is to place operands on their places - stack operands
     * go to memory, globally allocated variables are loaded into registers
     * and not allocated scratch registers are freed.
     *
     * Such leave code is executed only if \c to basic block has more than 
     * one reference. Otherwise no code generated.
     * @see gen_bb_enter
     */
    void gen_bb_leave(unsigned to);
    /**
     * @brief Generates either LOOKUPSWITCH or TABLESWITCH.
     */
    void gen_switch(const JInst& jinst);
    /**
     * @brief Generates various IF_ operations.
     *
     * Also inserts back branch polling (if the \link #JMF_BBPOLLING 
     * appropriate flag\endlink set), and \link #JMF_PROF_ENTRY_BE \endlink
     * instrumentation code.
     */
    void gen_if(JavaByteCodes opcod, unsigned target);
    /**
     * @brief Generates various IF_ICMP operations.
     *
     * Also inserts back branch polling (if the \link #JMF_BBPOLLING 
     * appropriate flag\endlink set), and \link #JMF_PROF_ENTRY_BE \endlink
     * instrumentation code.
     */
    void gen_if_icmp(JavaByteCodes opcod, unsigned target);
    /**
     * @brief Generates GOTO operation.
     *
     * Also inserts back branch polling (if the \link #JMF_BBPOLLING 
     * appropriate flag\endlink set), and \link #JMF_PROF_ENTRY_BE \endlink
     * instrumentation code.
     */
    void gen_goto(unsigned target);
    /**
     * @brief Generates JSR/JSR_W operation.
     */
    void gen_jsr(unsigned target);
    /**
     * @brief Generates RET operation.
     * @param idx - index of local variable.
     */
    void gen_ret(unsigned idx);
    
    /**
     * @brief Checks current inst and generates magic if needed
     * @return  - true if current inst is magic call, false otherwise.
     */
    bool gen_magic(void);

    
    //
    // Method being compiled info
    //

    /**
     * @brief Return type of the method.
     */
    jtype   m_retType;
    /**
     * @brief A list to keep exception handlers' info.
     */
    typedef ::std::vector<HandlerInfo> HADLERS_LIST;
    
    /**
     * @brief List of infos about exception handlers.
     * 
     * The list preserves the order in which the VM returns the info about 
     * the handlers.
     */
    HADLERS_LIST    m_handlers;
    
    /**
     * @brief Map of basic blocks. A key is basic block's leader's PC.
     */
    BBMAP   m_bbs;
    /**
     * @brief Bunch of preallocated BBState-s.
     *
     * A key in the map is basic block's PC.
     */
    map<unsigned, BBState*> m_bbStates;
    /**
     * @brief Pre-calculated BBState-s at the end of JSR subroutines.
     *
     * A key in the map is JSR subroutine beginning PC.
     */
    map<unsigned, BBState*> m_jsrStates;
    
    /**
     * @brief Array of decoded instructions.
     * @todo check whether changing AoS=>SoA gives a compilation speedup.
     */
    SmartPtr<JInst>     m_insts;
    
    /**
     * @brief Only emulates compilation - do not register the generated 
     *        code in VM.
     */
    bool    m_bEmulation;
    
    /**
     * @brief Code buffer allocated by VM.
     */
    char *  m_vmCode;
    
    /// 'TRUE' if this method has catch handlers suitable for StackOverflowError
    bool    hasSOEHandlers;

    /**
     * @brief Parses method's signature at the given constant pool entry.
     *
     * @brief Parses a constant pool entry (presuming it contains a method's 
     *        signature) and fills the info about the method's arguments and
     *        return type.
     * @param is_static - must be \b true, if the signature belongs to static
     *        method (this is to add additional 'this' which is not reflected
     *        in signature for instance methods).
     * @param cp_idx - constant pool index.
     * @param args - an array to fill out. Must be empty.
     * @param retType - [out] return type of the method. Must not be NULL.
     */
    void    get_args_info(bool is_static, unsigned cp_idx, 
                          ::std::vector<jtype>& args, jtype * retType);
    /**
     * @brief Obtains an arguments and return info from the given method.
     * @param meth - method handle to get the info for.
     * @param args - an array to fill out. Must be empty.
     * @param[out] retType - return type of the method. Must not be NULL.
     */
    static void get_args_info(Method_Handle meth,
                              ::std::vector<jtype>& args, jtype * retType);
    static jtype to_jtype(VM_Data_Type vmtype)
    {
        return ::Jitrino::Jet::to_jtype(vmtype);
    }
    /**
     * @brief Converts VM_Data_Type to appropriate #jtype.
     */
    static jtype to_jtype(Type_Info_Handle th);
private:
    /**
     * @brief Initializes all global stuff in StaticConsts.
     */
    static void initStatics(void);
    /**
     * @brief Initializes profiling data.
     *
     * Adds appropriate JMF_ flags to \c pflags if necessary.
     * @param[out] pflags - flags which will be used for compilation. Must 
     * be non-NULL.
     */
    void initProfilingData(unsigned * pflags);
};


}}; // ~namespace Jitrino::Jet

#include "bcproc.inl"

#endif  // __COMPILER_H_INCLUDED__
