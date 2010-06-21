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
 * @brief CodeGen class and related datas declaration.
 */
 
#if !defined(__CG_H_INCLUDED__)
#define __CG_H_INCLUDED__

#include "enc.h"
#include "sconsts.h"

#include "structs.h"
#include "sframe.h"
#include "csig.h"
#include "mib.h"

#include "../shared/MemoryManager.h"
#include "../main/PMF.h"
#include "../main/JITInstanceContext.h"
#include "../main/CompilationContext.h"

#include <map>
using std::map;

namespace Jitrino {
namespace Jet {

/**
 * CallSig for a VM helper that takes 1 argument of (#jobj).
 */
extern const CallSig ci_helper_o;
/**
 * CallSig for a VM helper that takes no arguments.
 */
extern const CallSig ci_helper_v;
/**
 * CallSig for a VM helper that takes 2 args: (#jobj, #i32).
 */
extern const CallSig ci_helper_oi;
/**
 * CallSig for VM helper THROW_LINKAGE_ERROR.
 */
extern const CallSig ci_helper_linkerr;

/**
 * Flag indicates that a patch item represents a table for 
 * LOOKUPSWITCH/TABLESWITCH. 
 */
#define DATA_SWITCH_TABLE (0x00010000)

extern void check_arg_has_doc(const char* key);


/**
* @brief Counts number of slots occupied by the specified set #jtype items.
* @param args - array of #jtype
* @return number of slots occupied by the specified set of #jtype items
*/
inline unsigned count_slots(const ::std::vector<jtype>& args)
{
    unsigned slots = 0;
    for (unsigned i=0; i<args.size(); i++) {
        slots  += (args[i] == dbl64 || args[i] == i64 ? 2 : 1);
    };
    return slots;
}


/**
* @brief Counts number of slots occupied by the specified set #jtype items.
* @param num - number of items in the \c args array
* @param args - array of #jtype
* @return number of slots occupied by the specified set of #jtype items
*/
inline unsigned count_slots(unsigned num, const jtype * args)
{
    unsigned slots = 0;
    for (unsigned i=0; i<num; i++) {
        slots += args[i] == dbl64 || args[i] == i64 ? 2 : 1;
    }
    return slots;
}

/**
 * The structure contains information about counter for patching profiling counter after 
 * profile is ready
 */
struct ProfileCounterInfo  {
   //This field contains composite info on counter size (first byte) and offset (last 3 bytes)
   U_32 offsetInfo;
   //Link to the basic block to calculate counter's offset after code layout
    BBInfo* bb;
    ProfileCounterInfo() : offsetInfo(0), bb(NULL){}

    static U_32 getInstSize(U_32 offsetInfo) { return offsetInfo >> 24;}
    static U_32 getInstOffset(U_32 offsetInfo) { return offsetInfo & 0x00FFFFFF;}
    static U_32 createOffsetInfo(U_32 instSize, U_32 instOffset) {
        assert(instSize<0xFF && instOffset<0xFFFFFF); 
        return (instSize<<24) | (instOffset);
    }
};

/**
 * Map contains patching information for all the counters in current method
 */    
typedef  std::vector<ProfileCounterInfo> ProfileCounterInfos;



/** the class is used by codegen internally to keep field information */
class FieldOpInfo {
public:
    FieldOpInfo(Field_Handle f, Class_Handle h, unsigned short i, JavaByteCodes oc) 
        : fld(f), enclClass(h), cpIndex(i), opcode(oc){}

    bool isGet() const {return opcode == OPCODE_GETFIELD || opcode == OPCODE_GETSTATIC;}
    bool isPut() const {return !isGet();}
    bool isStatic() const {return opcode == OPCODE_GETSTATIC || opcode == OPCODE_PUTSTATIC;}


    Field_Handle    fld;
    Class_Handle    enclClass;
    unsigned short  cpIndex;
    JavaByteCodes   opcode;
};

/**
 * Code generation routines for most of byte code instructions.
 *
 * The class also contains set of methods for <b>local register 
 * allocation</b>. The method that perform local register allocation if 
 * valloc(jtype jt).
 * 
 */
class CodeGen : public StaticConsts, public Encoder, public MethInfo {
protected:
    CodeGen(void)
    {
        m_pmfPipeline = NULL;
        m_pmf = NULL;
        m_lazy_resolution = true;
        m_compileHandle = NULL;
    }

    /**
    * @brief Compilation parameters.
    */    
    OpenMethodExecutionParams compilation_params;

public:
    /**
     * @brief Generates code for pushing int constant on operand stack.
     */
    void gen_push(int);
    /**
     * @brief Generates code for pushing jlong constant on operand stack.
     */
    void gen_push(jlong);
    /**
     * @brief Generates code which reads a constant value of type 'jt' from 
     *        the address specified by 'p' and pushes it onto the stack.
     */
    void gen_push(jtype jt, const void * p);
    /**
     * @brief Generates code which pops out a value of type 'jt' from the 
     *        operand stack and stores it at the address specified by 'p'.
     */
    void gen_pop(jtype jt);
    /**
     * @brief Generates code to perform POP2 bytecode instruction.
     */
    void gen_pop2(void);
    /**
     * @brief Generates various DUP_  operations.
     */
    void gen_dup(JavaByteCodes opc);
    /**
     * @brief Generates xSTORE operations.
     */
    void gen_st(jtype jt, unsigned idx);
    /**
     * @brief Generates xLOAD operations.
     */
    void gen_ld(jtype jt, unsigned idx);
    /**
     * @brief Generates LDC operation.
     */
    void gen_ldc(void);
    /**
     * @brief Generates PUTFIELD/GETFIELD/PUTSTATIC/GETSTATIC operations.
     *
     */
    void gen_field_op(JavaByteCodes opcode,  Class_Handle enclClass, unsigned short cpIndex);
    
    
    /**
    * @brief Generates modification watchpoints if VM need it.
    *
    * @param jt - field type.
    * @param fld - field handle.
    */
    void gen_modification_watchpoint(JavaByteCodes opcode, jtype jt, Field_Handle fld);

    /**
    * @brief Generates access watchpoints if VM need it.
    *
    * @param jt - field type.
    * @param fld - field handle.
    */
    void gen_access_watchpoint(JavaByteCodes opcode, jtype jt, Field_Handle fld);

    /**
    * @brief Restore all scratch registers and operand stack state
    *
    * @param saveBB - pointer to operand stack state.
    */
    void pop_all_state(BBState* saveBB);
    /**
    * @brief Save all scratch registers and operand stack state
    *
    * @param saveBB - pointer to operand stack state.
    */
    void push_all_state(BBState* saveBB);

    /**
     * @brief Generates code for INVOKE instructions.
     */
    void gen_invoke(JavaByteCodes opcod, Method_Handle meth, unsigned short cpIndex,
                    const ::std::vector<jtype>& args, jtype retType);

    /**
     * @brief Generates IINC operation.
     * @param idx - index of local variable.
     * @param value - value to add.
     */
    void gen_iinc(unsigned idx, int value);
    /**
     * @brief Generates arithmetic operations.
     *
     * @note \c op argument always specified as integer operation (for 
     *       example IADD, INEG), even if \c jt specifies float-point or long
     *       type.
     *
     * @param op - operation to perform.
     * @param jt - types used in the arithmetic operation.
     */
    void gen_a(JavaByteCodes op, jtype jt);
    /**
     * @brief Helper function for #gen_a, generates #i32 arithmetics.
     */
    bool gen_a_i32(JavaByteCodes op);
    /**
     * @brief Helper function for #gen_a, generates #flt32 and #dbl64 
     *        arithmetics.
     */
    bool gen_a_f(JavaByteCodes op, jtype jt);
    /**
     * @brief Helper function for #gen_a, may implement some 
     *        platform-dependent operations.
     */
    bool gen_a_platf(JavaByteCodes op, jtype jt);
    /**
     * @brief Performs generic operations which do not depend on type.
     */
    bool gen_a_generic(JavaByteCodes op, jtype jt);

    /**
     * @brief Generates conversion code.
     */
    void gen_cnv(jtype from, jtype to);

    /**
     * @brief Generates various CMP operations.
     */
    void gen_x_cmp(JavaByteCodes op, jtype jt);
    
    /**
     * @brief Generates code for NEW instruction.
     */
    void gen_new(Class_Handle enclClass, unsigned short cpIndex);
    /**
     * @brief Generates ANEWARRAY, NEWARRAY.
     */
    void gen_new_array(Allocation_Handle ah);
    void gen_new_array(Class_Handle enclClass, unsigned cpIndex);
    /**
     * @brief Generates MULTIANEWARRAY.
     */
    void gen_multianewarray(Class_Handle enclClass, unsigned short cpIndex, unsigned num_dims);
    /**
     * @brief Generates code for INSTANCEOF or CHECKCAST operations.
     * @param chk - if \b true, generates CHECKCAST, INSTANCEOF otherwise.
     * @param klass - Class_Handle of the class to cast to.
     */
    void gen_instanceof_cast(JavaByteCodes opcode, Class_Handle enclClass, unsigned short cpIndex);
    /**
     * @brief Generates code for MONITOREXTERN and MONITOREXIT.
     */
    void gen_monitor_ee(void);
    /**
     * @brief Generates code for ATHROW.
     */
    void gen_athrow(void);
    
    /**
     * @brief Update BBState and pushes return value on operand stack,
     * as if the code just executed a call that returned the given value.
     * 
     * The item location is set according to the calling convention on 
     * which registers to use to return value of the given type.
     *
     * On IA-32 (where the float/double are returned through FPU) the 
     * item is spilled (code is generated to do so) into memory first.
     * @param cs - calling signature descrubing the method to push return
     *             value for.
     */
    void gen_save_ret(const CallSig& cs);
    
    /**
     * @brief Generates code to call one of the throw_ helpers.
     *
     * @note The method only synchronize local vars into the memory.
     */
    void gen_call_throw(const CallSig& cs, void * target, unsigned idx, ...);
    
    /**
     * @brief Generates code to call non-VM helper.
     *
     * The method does not update GC info.
     */
    void gen_call_novm(const CallSig& cs, void * target, unsigned idx, ...);
    /**
     * @brief Generates code to call a VM helper.
     *
     * The method updates GC info and synchronizes both stack and local vars.
     */
    void gen_call_vm(const CallSig& cs, void * target, unsigned idx, ...);

    /**
     * @brief Generates code to call a VM helper and then restores the 
     *        state of local variable, stack and registers.
     *
     * The method updates GC info and synchronizes both stack and local vars.
     */
    void gen_call_vm_restore(bool exc, const CallSig& cs, void * target, 
                             unsigned idx, ...);
    
    /**
     * @brief Generates code to take arguments from Java operand stack and 
     *        put them to the native stack/registers to prepare a call.
     *
     * @note The method does rlock(cs), so caller must runlock(cs).
     *
     * @param pop - if \b true, the arguments are popped out from the operand 
     *        stack. Otherwise, the operand stack is left intact.
     * @param cs - CallSig describing args
     * @param idx - an index of arguments to start from
     * @param cnt - how many arguments to process. -1 means 'till the end'
     */
    unsigned gen_stack_to_args(bool pop, const CallSig& cs, unsigned idx, 
                               int cnt=-1);

    /**
     * @brief Generates code to prepare arguments for a call.
     *
     * @note The method does \b not rlock(cs).
     * @note All possible conflicts of \c parg registers and registers
     *       used in \c cs, must be resolved before gen_args call.
     */
    void gen_args(const CallSig& cs, unsigned idx, const Val * parg0 = NULL, 
                  const Val * parg1 = NULL, const Val * parg2 = NULL, 
                  const Val * parg3 = NULL, const Val * parg4 = NULL,
                  const Val * parg5 = NULL, const Val * parg6 = NULL);


    /**
     * @brief Generates ARRAYLENGTH instruction.
     */
    void gen_array_length(void);

    /**
     * @brief Generates ALOAD instruction.
     *
     * Also invokes gen_check_bounds() and gen_check_null().
     */
    void gen_arr_load(jtype jt);
    /**
     * @brief Generates ASTORE instruction.
     *
     * Also invokes gen_check_bounds() and gen_check_null().
     */
    void gen_arr_store(jtype jt, bool helperOk = true);
    /**
     * @brief Generates code to check bounds for array access.
     * @param aref_depth - depth (in the operand stack) of the array's object
     *        reference
     * @param index_depth - depth (in the operand stack) of the index to be
     *        used for array's access.
     */
    void gen_check_bounds(unsigned aref_depth, unsigned index_depth);
    /**
     * @brief Generates code to check whether the object ref at the given 
     *        depth in the operand stack is \c null.
     * @param stack_depth_of_ref - depth in the operand stack of the object 
     *        reference to test against \c null.
     */
    void gen_check_null(unsigned stack_depth_of_ref);
    /**
     * @brief Generates code to check whether the object apecified by the 
     *        \c Val is not \b NULL.
     * @param val - the value to check for \b NULL
     * @param hw_ok - whether it's ok to eliminate explicit NPE check in 
     * favor of hardware NPE check.
     */
    void gen_check_null(Val& val, bool hw_ok);
    /**
     * @brief Generates code to check whether an item used in division 
     *        operation is zero.
     * @param jt - type of division operation (#i64 or #i32) to be performed.
     * @param stack_depth_of_divizor - depth in the operand stack of the 
     *        divisor.
     */
    void gen_check_div_by_zero(jtype jt, unsigned stack_depth_of_divizor);
    
    /**
     * @brief Generates a code which prepares GC info for operand stack - 
     *        stack depth and GC mask.
     *
     * @param depth - if \b -1, then current stack depth is taken,
     *        otherwise, the specified depth is stored.
     * @param trackIt - if \b true, then the GC info is also reflected in 
     *        the current BB's state (BBState::stack_depth, 
     *        BBState::stack_mask).
     * @see BBState
     */
    void gen_gc_stack(int depth=-1, bool trackIt=false);
    /**
     * @brief Generates code which either marks or clears mark on a local 
     *        variable to reflect whether it holds an object or not 
     *        (runtime GC info).
     */
    void gen_gc_mark_local(jtype jt, unsigned idx);
    
    /**
     * @brief Generates GC safe point code which facilitate thread 
     * suspension on back branches.
     */
    void gen_gc_safe_point(void);

    /**
     * @brief Inserts a break point.
     *
     * For debugging only.
     */
    void gen_brk(void);

    /**
     * @brief Generates code to ensure stack integrity at runtime.
     * @note For debug checks only.
     * @param start - \b true if check start to be generated, false otherwise
     */
    void gen_dbg_check_stack(bool start);
    
    /**
     * @brief Generates code to output string during runtime.
     *
     * The generated code preserves general-purpose registers.
     * The string is formatted before the code generation, then 
     * \link #dbg_rt get printed \endlink during runtime.
     *
     * For debugging only.
     */
    void gen_dbg_rt(bool save_regs, const char * fmt, ...);

    /**
     * @brief Generates code to throw specified tipe of exception.
     *
     * The generated code which throws exception of soecified type.
     * Also it can updates GC info and synchronizes both stack and local vars.
     */
    void gen_throw(Class_Handle exnClass, bool restore);
    
    /**
     * The opcode may be one of AASTORE, PUTFIELD or PUTSTATIC.
     *
     * For AASTORE \c fieldHandle must be \b NULL.
     * For AASTORE \c fieldSlotAddress is not used
     */
    void gen_write_barrier(JavaByteCodes opcode, Field_Handle fieldHandle, Opnd fieldSlotAddress);


    /**
     * @brief Prints out an argument's (or return value's) type and value.
     *
     * The method is invoked during runtime from the compiled code.
     * 
     * The output goes through dbg_rt.
     * @param val - value of the argument/return value
     * @param idx - index of argument or -1 if return value is printed
     * @param jt - type of the argument/return value
     */
    static void __stdcall dbg_trace_arg(void * val, int idx, jtype jt) stdcall__;
    /**
     * @brief CallSig object for dbg_trace_arg().
     */
    static const CallSig cs_trace_arg;
    /**
     * @brief Converts given \c s into human-readable string.
     *
     * Used for debugging output.
     *
     * @param s - Val to be converted.
     * @param is_stack - \b true if \c s represents operand stack item.
     */
    ::std::string toStr2(const Val& s, bool is_stack) const;
    /**
     * @brief Prints out BBState.
     * @param name - a name to print out before the dump, to identify the 
     *        dump in the trace log.
     * @param pState - BBState to dump.
     */
    void    dbg_dump_state(const char * name, BBState * pState);
    /**
     * @brief Enforces memory check.
     * 
     * Implementation is platform dependent. 
     * 
     * Currently, the check is performed only on Windows in debug build,
     * using CRT routines.
     */
    void    dbg_check_mem(void);
    /**
     * @brief Generates move operation from \c src to \c dst.
     * 
     * The types of both \c src and \c dst must be the same and \c dst must
     * not be immediate. If both items reside in the memory, then a 
     * temporary register is allocated to perform the movement.
     *
     * The method does not transfer attributes.
     *
     * @param src - source operand.
     * @param dst - destination operand.
     */
    void    do_mov(const Val& dst, const Val& src, bool skipTypeCheck=false);
    /**
     * @brief Returns a stack item at the given depth.
     * 
     * @param depth - depth of stack item to return.
     * @param toReg - if \b true, then ensures that the stack item resides
     * on a register.
     */
    Val&    vstack(unsigned depth, bool toReg = false);
    /**
     * @brief Returns a local variable item.
     *
     * The local variable's slot at the given \c idx must be either 
     * unknown (of #jvoid type) or must be of the same type \c jt.
     * 
     * If operation being generted is defining operation, then vvar_def().
     * must be called before vlocal() and \c willDef must be set to \b true.
     * 
     * If the variable has globally allocated register, but is currently 
     * spilled out, then the code to upload it back from the memory to 
     * register is generated. If \c willDef == \b true, no upload code
     * is generated.
     * 
     * Also, if \c willDef == \b true, then the method ensures that any 
     * references to the variable in operand stack are 'unreferenced' -
     * that is the value of variable is read and then written into 
     * operand stack memory cells. 
     *
     * @note As a side effect of this function some items on operand stack
     * may change their location.
     *
     * @param jt - type of the variable.
     * @param idx - index of the variable.
     * @param willDef - whether the next operation with the variable
     * will be defining operation.
     * @see vvar_def
     */
    Val&    vlocal(jtype jt, unsigned idx, bool willDef = false);
    /**
     * @brief Ensures the local variable slot has the proper type.
     *
     * If the \c idx slot is occupied by an register of other type (e.g. 
     * FSTORE followed by ISTORE), then the register is freed, and the 
     * slot get the proper type assigned to it.
     */
    void    vvar_def(jtype jt, unsigned idx);
    /**
     * @brief Returns offset of the given local variable in the stack frame.
     *
     * May not be equal to m_stack.local(idx) as some local variables that 
     * come as input args, are not copied into stack frame but are used 
     * at the same location as the input args are.
     *
     * The returned value reflect current stack depth (if applicable).
     */
    int     vlocal_off(unsigned idx) const;
    /**
     * @brief Returns offset of the operand stack item at the given depth 
     * in method's stack frame.
     *
     * The returned value reflect current stack depth (if applicable).
     */
    int     vstack_off(unsigned depth) const;
    /**
     * @brief Corrects an offset in the method's stack frame according 
     * to the current stack depth.
     *
     * The method is intended for use with sp-based stack frames.
     */
    int     voff(int off) const;
    /**
     * @brief Verifies data integrity in current BBState. For debugging 
     * purposes only.
     */
    void    vcheck(void);
    /**
     * @brief Allocates temporary scratch register of the given type.
     *
     * The allocation is based on reference count for a register, register 
     * locks and 'last_used' fields in the BBState.
     *
     * The 'last used' idiom allows to spread operations across several 
     * registers. When a code is about to allocate temporary register, then 
     * the allocation routine tries to allocate a register \b other than one 
     * allocated before. This reduces dependencies between operations and 
     * increases parallelism chances.
     * 
     * If the allocation routine failed to find an unused register of 
     * proper type, then it generates spill code. A register to spill is 
     * chosen on reference count for the register. The reference count is
     * current number of usages of the register in both operand stack and 
     * local variables array. 
     * 
     * A non-locked register with the lowest number of references is 
     * subject to spill out - the code is generated.
     *
     * The reference count is not calculated in valloc() itself, but used
     * from BBState data (rref()).
     *
     * This presumes that usage of registers in mimic operand stack and 
     * local variables is tracked properly. Debug method vcheck() ensures 
     * the proper reference count.
     *
     * @note As a side effect of this function some non-locked items of 
     * operand stack or local variables may change its kind from register 
     * to memory.
     */
    AR      valloc(jtype jt);

    /**
     * @brief Ensures operand stack item at the given \c depth resides in 
     * the memory.
     */
    void    vswap(unsigned depth);
    /**
     * @brief Synchronizes all scratch registers into memory.
     *
     * If \c doStack is \b false, then only processes local variables.
     *
     * @param doStack - \c true to synchronize operand stack items as well
     */
    void    vpark(bool doStack = true);
    /**
     * @brief Synchronizes given register into memory.
     *
     * If \c doStack is \b false, then only processes local variables.
     * 
     * @param ar - register to be synchronized
     * @param doStack - \c true to synchronize operand stack items as well
     */
    void    vpark(AR ar, bool doStack = true);
    
    /**
     * @brief Tests whether operand stack item at the given depth is 
     * immediate.
     */
    bool    vis_imm(unsigned depth)
    {
        return m_jframe->dip(depth).is_imm();
    }
    /**
     * @brief Tests whether operand stack item at the given depth resides
     * in memory.
     */
    bool    vis_mem(unsigned depth)
    {
        return m_jframe->dip(depth).is_mem();
    }
    /**
     * @brief Tests whether operand stack item at the given depth resides 
     * in register.
     */
    bool    vis_reg(unsigned depth)
    {
        return m_jframe->dip(depth).is_reg();
    }
    /**
     * @brief One or 2 (for wide types) empty Val-s on the operand stack.
     * @note In contrast to vpush(const Val&) accepts wide types.
     */
    void    vpush(jtype jt);
    /**
     * @brief Pushes the given Val into mimic operand stack.
     *
     * Increments reference counts for registers involved.
     */
    void    vpush(const Val& op);
    /**
     * @brief Pushes the given Val into mimic operand stack.
     *
     * Increments reference counts for registers involved.
     */
    void    vpush2(const Val& op_lo, const Val& op_hi);
    /**
     * @brief Pops out 2 parts of \link is_big() big \endlink type from 
     * mimic operand stack.
     *
     * Decrements reference counts for involved registers.
     */
    void    vpop2(Val* pop_low, Val* pop_hi);
    /**
     * @brief Pops out an item from mimic operand stack.
     *
     * Decrements reference counts for involved registers.
     */
    void    vpop(void);
    /**
     * @brief Marks that the local variable \c idx resides in the given
     * register (\c s).
     * @param jt - type of local variable
     * @param idx - index of local variable
     * @param s - must represent a register of proper type, to be assigned 
     * as current variable's location
     */
    void    vassign(jtype jt, unsigned idx, const Val& s);
    /**
     * @brief Tests whether the memory reference in \c s points to an 
     * operand stack item in the stack frame.
     */
    bool    vis_stack(const Val& s) const;
    /**
     * @brief Tests whether the operand stack at the given \c depth 
     * is memory <b>and is located in the operand stack area in the stack 
     * frame</b>.
     * In other words, it's memory and represents neither a local variable, 
     * nor a field, nor any other value stored in memory, but exactly
     * the operand stack element.
     */
    bool    vis_stack(unsigned depth) const
    {
        return vis_stack(m_jframe->dip(depth));
    }

    /**
     * @brief Returns \b true if the local variable is not stored in the 
     * method's stack frame, but rather a stack slot of input argument is 
     * reused.
     */
    bool    vis_arg(unsigned local_idx) const;
    /**
     * @brief If vis_arg(local_idx) is \b true, the method returns 
     * index of input argument that corresponds to this local variable.
     *
     * Must only be called for local variables which really reuse input 
     * argument stack space (that is, vis_arg()==\b true).
     */
    unsigned vget_arg(unsigned local_idx) const;
    /**
     * @brief Tests whether the memory reference in \c s points to a local
     * variable. An index of the variable (>=0) is returned, or -1 if 
     * the memory does not point to local variable.
     *
     * This method takes vis_arg() into account - if the variable reuses
     * input argument space, then the address is checked against that 
     * argument's address.
     */
    int     vvar_idx(const Val& s) const;
    /**
     * @brief Wraps given address into operand.
     * 
     * The method checks whether the address fits into displacement part
     * of complex address form (which is always true for IA-32). If so, 
     * then the operand with no base is returned.
     *
     * Otherwise, GR register allocated, the code is generated to load 
     * address into the register and the operand with the register as 
     * base and zero displacement returned.
     */    
    Opnd    vaddr(jtype jt, const void* p)
    {
        const char * addr = (const char*)p;
        // If we're going to address more that 4 bytes, ensure the next 
        // item will also fit into displacement.
        if (fits32(addr) && fits32(addr+4)) {
            return Opnd(jt, ar_x, (int)(int_ptr)addr);
        }
        AR ar = valloc(jobj);
        movp(ar, addr);
        return Opnd(jt, ar, 0);
    }
    /**
     * @brief Return <i>static type</i> of the local variable.
     *
     * Static type is the type which is only used to access the variable.
     *
     * For example, if a variable is only used for ASTORE and ALOAD, then 
     * the static type is #jobj. Otherwise (e.g. FSTORE/ASTORE mix), static
     * type is #jvoid.
     */
    jtype   vtype(unsigned idx);
    /**
     * Returns a register allocated for the given local variable, ar_x if
     * none.
     */
    AR      vreg(jtype jt, unsigned idx);
    /**
     * @brief Dereferences all items of the given type on operand stack.
     *
     * Dereference here means the following:
     *
     * When mimic operand stack operations, CodeGen enforces lazy scheme -
     * e.g. for xLOAD operations, the reference on local variable is
     * stored onto mimic stack. 
     * 
     * However, when we perform a defining operation for a local variable
     * we must ensure that operand stack items contain the value of the 
     * variable before the defining operation.
     *
     * The method generates code to reload such postponed references from
     * the local variable(s) into proper operand stack memory slot(s).
     */
    void    vunref(jtype jt);
    /**
     * Dereferences all items on the operand stack the refer to the given
     * Opnd (either register or memory location).
     *
     * @see vunref(jtype jt) on dereference description.
     */
    void    vunref(jtype jt, const Val& op, unsigned skipDepth = NOTHING);
    
    /**
     * @brief Locks the given \c ar.
     * 
     * Increases number of locks for the specified \c ar.
     *
     * Lock on an AR prevents the register from being \link valloc(jtype) 
     * allocated for temporary needs \endlink.
     * 
     * The lock may be acquired several times, but must be released the 
     * same number of times.
     */
    void rlock(AR ar)
    {
        if (ar != ar_x && ar != (AR)NOTHING) {
            assert(ar_idx(ar)<ar_num);
            ++m_bbstate->m_regs[ar_idx(ar)].locks;
        }
    }
    
    /**
     * @brief Unlocks the given \c ar.
     *
     * Decreases number of locks for the specified \c ar.
     * 
     * @param ar - register to be locked
     * @param force - if \b true, then drops the lock count to zero, 
     * regardless of how many lock(ar) was invoked before.
     * @see rlock(AR ar)
     */
    void runlock(AR ar, bool force = false)
    {
        if (ar != ar_x && ar != (AR)NOTHING) {
            assert(ar_idx(ar)<ar_num);
            assert(m_bbstate->m_regs[ar_idx(ar)].locks>0);
            if (force) {
                m_bbstate->m_regs[ar_idx(ar)].locks = 0;
            }
            else {
                --m_bbstate->m_regs[ar_idx(ar)].locks;
            }
        }
    }
    /**
     * @brief Locks all the registers used in the \c cs.
     */
    void    rlock(const CallSig& cs)
    {
        for (unsigned i=0; i<cs.count(); i++) {
            AR ar = cs.reg(i);
            if (ar != ar_x) {
                rlock(ar);
            }
        }
    }
    /**
     * @brief Unlocks all the registers used in the \c cs.
     */
    void    runlock(const CallSig& cs)
    {
        for (unsigned i=0; i<cs.count(); i++) {
            AR ar = cs.reg(i);
            if (ar != ar_x) {
                runlock(ar);
            }
        }
    }
    /**
     * @brief Locks the registers in the \c s.
     *
     * If \c s refers to register, then this register is locked. 
     *
     * If \c is memory reference, then all valid registers of complex
     * address form are locked.
     *
     * For immediate operand it's no-op.
     */
    void    rlock(const Val& s)
    {
        if (s.is_reg())         { rlock(s.reg()); }
        else if (s.is_mem())    { rlock(s.base()); rlock(s.index()); }
    }
    /**
     * @brief Unlocks all registers in the \c s.
     */
    void    runlock(const Val& s)
    {
        if (s.is_reg())         { runlock(s.reg()); }
        else if (s.is_mem())    { runlock(s.base()); runlock(s.index()); }
    }
    /**
     * @brief Increments reference counts for registers in the \c s.
     */
    void    rref(const Val& s)
    {
        if (s.is_reg())         { rref(s.reg()); }
        else if (s.is_mem())    { rref(s.base()); rref(s.index()); }
    }
    /**
     * @brief Decrements reference counts for registers in the \c s.
     */
    void    rfree(const Val& s)
    {
        if (s.is_reg())         { rfree(s.reg()); }
        else if (s.is_mem())    { rfree(s.base()); rfree(s.index()); }
    }
    /**
     * @brief Increments reference counts for \c ar;
     */
    void rref(AR ar)
    {
        if (ar != ar_x && ar != (AR)NOTHING) {
            assert(ar_idx(ar)<ar_num);
            ++m_bbstate->m_regs[ar_idx(ar)].refs;
        }
    }
    /**
     * @brief Decrements reference counts for \c ar;
     */
    void rfree(AR ar)
    {
        if (ar != ar_x && ar != (AR)NOTHING) {
            assert(ar_idx(ar)<ar_num);
            assert(m_bbstate->m_regs[ar_idx(ar)].refs>0);
            --m_bbstate->m_regs[ar_idx(ar)].refs;
        }
    }
    /**
     * @brief Returns number of references for \c ar;
     */
    unsigned rrefs(AR ar) const
    {
        assert(ar_idx(ar)<ar_num);
        return m_bbstate->m_regs[ar_idx(ar)].refs;
    }
    /**
     * @brief Returns number of locks for \c ar;
     */
    unsigned rlocks(AR ar) const
    {
        assert(ar_idx(ar)<ar_num);
        return m_bbstate->m_regs[ar_idx(ar)].locks;
    }
    /**
     * @brief Tries to find out which register is currently know to hold 
     * the given value (if \c v is immediate) or a value from the specified
     * address (if \c v is memory).
     */
    AR rfind(const Val& v) const
    {
        for (unsigned i=0; i<ar_num; i++) {
            if (m_bbstate->m_regs[i].val == v) {
                return _ar(i);
            }
        }
        return ar_x;
    }
    /**
     * @brief Set's 'currently known' value for the register.
     */
    void rset(AR ar, const Val& v)
    {
        assert(ar_idx(ar)<ar_num);
        m_bbstate->m_regs[ar_idx(ar)].val = v;
    }
    /**
     * @brief Clears all registers-related info in current BBState.
     */
    void rclear(void)
    {
        for (unsigned i=0; i<ar_num; i++) {
            m_bbstate->m_regs[i].locks = 0;
            m_bbstate->m_regs[i].refs = 0;
            m_bbstate->m_regs[i].val = Val();
        }
    }
    /**
     * @brief Returns 'last used' register of the given type.
     * @see valloc
     */
    AR rlast(jtype jt) const
    {
        return is_f(jt) ? m_bbstate->m_last_fr : m_bbstate->m_last_gr;
    }
    /**
     * @brief Sets 'last used' register for the given type.
     * @see valloc
     */
    void rlast(AR ar)
    {
        assert(ar != ar_x);
        if (is_f(ar)) {
            m_bbstate->m_last_fr = ar;
        }
        else {
            assert(is_gr(ar));
            m_bbstate->m_last_gr = ar;
        }
    }

protected:
    /**
     * @brief Tests whether the specified flag is set in method's compilation
     *        flags.
     *
     * If \c flag is a set of flags, then checks whether any of the flags 
     * is set.
     * @see InfoBlock#get_flags
     */
    bool is_set(unsigned flag)
    {
        return (m_infoBlock.get_flags() & flag);
    }
    /**
     * @brief Returns command line argument for the given \c key, or \c def
     * if the \c key was not specified.
     *
     * Normally, arguments are specified as 
     *
     * '-XX:jit.\<JIT_NAME\>.arg.\<key\>=value'
     *
     * Implemented though Jitrino's PMF, refer to PMF docs for more info.
     * @note The old-fashion -Xjit options are not processed by this method.
     */
    const char* get_arg(const char* key, const char* def)
    {
#ifdef _DEBUG
        check_arg_has_doc(key);
#endif
        if (m_pmf == NULL) {
            m_pmf = &JITInstanceContext::getContextForJIT(m_hjit)->getPMF();
            m_pmfPipeline = (PMF::Pipeline*)CompilationContext::getCurrentContext()->getPipeline();
        }
        return m_pmf->getStringArg(m_pmfPipeline, key, def);
    }
    /**
     * @brief Returns a command-line argument interpreted as integer.
     */
    int get_int_arg(const char* key, int def)
    {
        const char* val = get_arg(key, (const char*)NULL);
        return val == NULL ? def : atoi(val);
    }
    /**
     * @brief Returns a string command-line argument.
     *
     * Never returns NULL, but empty constant string ("") if argument
     * was not set in through the command line.
     */
    const char* get_arg(const char* key)
    {
        const char* val = get_arg(key, (const char*)NULL);
        return val == NULL ? "" : val;
    }
    /**
     * @brief Returns a command-line argument interpreted as integer.
     * @see to_bool
     */
    bool get_bool_arg(const char* key, bool def)
    {
        const char* val = get_arg(key, (const char*)NULL);
        if (val == NULL) {
            return def;
        }
        return to_bool(val);
    }


    /**
    * @brief Do all the job for gen_field_op()
    *
    * Invokes gen_check_null() for GETFIELD and PUTFIELD.
    */
    void do_field_op(const FieldOpInfo& fieldOp);

    /**
    * @brief Returns mem-opnd that is address of the given field. Used by do_field_op
    * Invokes gen_check_null() for GETFIELD and PUTFIELD.
    */
    Opnd get_field_addr(const FieldOpInfo& fieldOp, jtype jt);

    /**
     * PMF instance to get arguments from.
     */
    PMF *               m_pmf;
    /**
     * Current filter for PMF.
     */
    PMF::HPipeline      m_pmfPipeline;
    /**
     * @brief JIT handle.
     */    
    JIT_Handle      m_hjit;
    //*********************************************************************
    //* Instrumentation, profiling 
    //*********************************************************************
    /**
     * Generates back edge counter increment.
     * Does nothing is JMF_PROF_ENTRY_BE not set.
     */
    void gen_prof_be(void);
    
    /**
     * @brief Pointer to method's entrances counter.
     * @see JMF_PROF_ENTRY_BE
     */
    unsigned *  m_p_methentry_counter;
    /**
     * @brief Pointer to method's back branches counter.
     * @see JMF_PROF_ENTRY_BE
     */
    unsigned *  m_p_backedge_counter;
    /**
     * @brief Threshold for method entry counter which fires recompilation 
     *        (in synchronized recompilation mode).
     */
    unsigned    m_methentry_threshold;
    /**
     * @brief Threshold for back edges counter which fires recompilation 
     *        (in synchronized recompilation mode).
     */
    unsigned    m_backedge_threshold;
    /**
     * @brief Profile handle to be passed to recompilation handler (in 
     *        synchronized recompilation mode).
     */
    void*       m_profile_handle;
    /**
     * @brief Recompilation handler (in synchronized recompilation mode).
     */
    void *      m_recomp_handler_ptr;
    
    /**
     * @brief The byte code of the method being compiled.
     */
    const unsigned char* m_bc;

    JFrame *    m_jframe;
    /**
     * @brief Current basic block's info.
     * 
     * @note Only valid during code generation.
     */
    BBInfo*     m_bbinfo;
    /**
     * @brief Current basic block's state.
     * 
     * @note Only valid during code generation.
     */
    BBState *   m_bbstate;
    /**
     * @brief PC of an instruction currently processed.
     */
    unsigned    m_pc;
    /**
     * @brief Instruction currently being processed.
     */
    const JInst *   m_curr_inst;
    
    /**
     * @brief Method's info block.
     */
    MethodInfoBlock m_infoBlock;
    
    /**
     * @brief Method's native stack layout.
     */
    StackFrame      m_stack;
    
    /**
     * @brief Global register allocation.
     * 
     * m_ra[local index] contains a register globally allocated for the 
     * local variable or ar_x.
     */
    vector<AR>      m_ra;
    /**
     * 'Static' types for local variables.
     *
     * A variable has static type if it's used as this type only (e.g. only
     * ISTORE and ILOAD are performed). If a local variable slot is used
     * as more than one type (e.g. both ASTORE_0 and DSTORE_0 exist) then 
     * the static type is #jvoid.
     *
     * The size of the array is equal to the number of local variables.
     */
    vector<jtype>       m_staticTypes;
    /**
     * @brief Numbers of def operations of local variables.
     */
    vector<unsigned>    m_defs;    // [num_locals]
    /**
     * @brief Numbers of use operations of local variables.
     */
    vector<unsigned>    m_uses;    // [num_locals]
    /**
     * @brief Mapping between input arguments and local variables they 
     * map to.
     */
    vector<int>         m_argids;   // [m_argSlots];
    /**
     * @brief Number of slots occupied by input args of the method.
     */
    unsigned            m_argSlots;
    /**
     * @brief The bitset shows whether a register (by its index) was 
     * globally allocated or not.
     *
     * <code>
     * <pre>
     * AR ar = ...
     * if (m_global_rusage[ar_idx(ar)]) {
     *      printf("%s allocated globally", to_str(ar).c_str());
     *  }
     * </pre>
     * </code>
     */
    bitset<ar_num>      m_global_rusage;
    /**
     * @brief CallSig instance for method being compiled.
     */
    CallSig             m_ci;
    /**
     * @brief A GR register used to make stack frame for the compiled 
     * method.
     *
     * Currently, only #bp is allowed here. In future, it's possible to 
     * implement #sp-based stack frame.
     */
    AR              m_base;
    
    /**
     * @brief Current stack depth.
     * 
     * For sp-based frame, to track changes of depth of native stack.
     */
    unsigned        m_depth;
    /**
     * @brief Maximum value for #m_depth.
     */
    unsigned        m_max_native_stack_depth;
    /**
     * @brief Compilation id for the method being compiled.
     * 
     * It's a simple sequential counter for compilation requests came 
     * through the Jitrino.JET.
     * Technically speaking, not equal to the number of compiled methods 
     * as some methods may be rejected (seen, but not compiled).
     */
    unsigned     m_methID;

    /**
    * @brief If 'TRUE' JIT will not ask VM to resolve any unresolved types during compilation
    */    
    bool m_lazy_resolution;

    /**
    * @brief Compilation handle.
    */    
    Compile_Handle  m_compileHandle;

    ProfileCounterInfos m_profileCountersMap;

};


}}; // ~namespace Jitrino::Jet



#endif  // ~ __CG_H_INCLUDED__
