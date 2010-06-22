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
 * @author Vyacheslav P. Shakin
 */

#include "Ia32IRManager.h"
//#include "Ia32Printer.h"

namespace Jitrino
{
namespace Ia32{

//========================================================================================
// class Ia32ConstraintsResolver
//========================================================================================
/**
 *  class Ia32ConstraintsResolver performs resolution of operand constraints
 *  and assigns calculated constraints (Opnd::ConstraintKind_Calculated) to operands.
 *  The resulting calculated constraints of operands determine allowable physical location
 *  for the operand.
 *  
 *  This transformer allows to insert operands into instructions before it 
 *  regardless instruction constraints except that Initial constraints of explicit 
 *  instruction operands must have non-null intersections with corresponding constraints 
 *  of at least one opcode group of the instruction.
 *  
 *  ConstraintResolver analyzes instruction constraints and splits operands when necessary.
 * 
 *  This transformer ensures that 
 *  1)  All instruction constraints for EntryPoints, CALLs and RETs
 *      are set appropriately (IRManager::applyCallingConventions())
 *  2)  All operands has non-null calculated constraints 
 *  3)  All operands fits into instructions they are used in (in terms of instruction constraints)
 *      For example:
 *          Original code piece:
 *              I38: (AD:s65:double) =CopyPseudoInst (AU:t1:double) 
 *              I32: MULSD .s65.:double,.t2:double 
 *              I33: RET t66(0):int16 (AU:s65:double) 
 *          
 *              RET imposes constraint on s65 requiring to place it into FP0 register 
 *              (its FP0D alias in this particular case)
 *              MULSD imposes constraint on s65 requiring to place it into XMM register 
 *                      
 *          After the pass: 
 *              I38: (AD:s65:double) =CopyPseudoInst (AU:t1:double) 
 *              I32: MULSD .s65.:double,.t2:double 
 *              I46: (AD:t75:double) =CopyPseudoInst (AU:s65:double) 
 *              I33: RET t66(20):int16 (AU:t75:double) 
 *          
 *              Thus, ConstraintResolver inserted I46 splitting s65 to s65 and t75
 *              s65 is assigned with Mem|XMM calculated constraint and t75 
 *              is assigned with FP0D calculated calculated constraint
 *
 *  4)  If the live range of an operand crosses a call site and the operand is not redefined 
 *      in the call site, the calculated constraint of the operand is narrowed the callee-save regs 
 *      or memory (stack)
 *      
 *  5)  If the operand (referred as original operand here) is live at entry of a catch handler 
 *      then necessary operand splitting is performed as close as possible to the instruction 
 *      which caused the splitting and original operand is used before and after the instruction. 
 *      
 *  The main principle of the algorithm is anding of instruction constraints into
 *  operand calculated constraints and splitting operands to ensure that the calculated constraint
 *  is not null
 *
 *  This transformer must be inserted before register allocator which relies on 
 *  calculated operand constraints. 
 *
 *  The implementation of this transformer is located in the ConstraintResolverImpl class. 
 *
 */


//========================================================================================
// class ConstraintsResolverImpl
//========================================================================================

/**
 *  class Ia32ConstraintsResolverImpl is an implementation of simple constraint resolution algorithm
 *  The algorithm takes one-pass over CFG.
 *
 *  The algorithm works as follows: 
 *      
 *  1)  Creates an array of basic blocks and orders by bb->getExecCount() 
 *      in createBasicBlockArray().
 *      Thus, the algorithm handles hottest basic blocks first and constraints are assigned to operands first 
 *      from the most frequently used instructions
 *
 *  2)  Collects a bit vector of all operands live at entries of all dispatch node entries
 *      in calculateLiveAtDispatchBlockEntries()
 *
 *  3)  For all operands:
 *      - If an operand has already been assigned to some location 
 *        (its location constraint is not null) the calculated constraint is set to 
 *        the location constraint
 *           
 *      - If an operand is live at entry of a dispatch node 
 *        the calculated constraint is set to the constraint 
 *        preserving operand values during exception throwing
 *        This constraint is returned by getDispatchEntryConstraint
 *        In fact this is the constriant for the DRL calling convention
 *
 *      This is done in calculateStartupOpndConstraints()
 *      Originally all calculateed constraints are equial to Initial constraints
 *
 *  4)  Walks through all basic blocks collected and arranged at step 1
 *      in resolveConstraints()
 *
 *      The opndReplaceWorkset array of operand replacements is maintained 
 *      (indexed by from-operand id). 
 *
 *      This is the array of current replacement for operands
 *      and is reset for each basic block (local within basic blocks) 
 *
 *      This array is filled as a result of operand splitting and indicates
 *      which operand must be used instead of original ones for all the instructions
 *      above the one caused splitting
 *      
 *      4.1) Walks throw all instruction of a basic block in backward order 
 *          in resolveConstraints(BasicBlock * bb) 
 *          4.1.1) resolves constraints for each instruction 
 *              in resolveConstraints(Inst * inst);
 *          
 *              To do this already collected calculated constraint of 
 *              either original operand or its current replacement is anded 
 *              with instruction constraint for this operand occurence and
 *              if the result is null, new operand is created and substituted instead
 *                              
 *              4.1.1.1) All def operands of the isntruction are traversed
 *                  and operand splitting is performed after the instruction (when necessary)
 *                  def&use cases are also handled during this step 
 *              4.1.1.2) If the instruction is CALL, all hovering operands of 
 *                  the isntruction are traversed.
 *
 *                  Hovering operands are operands which are live across a call site and are not
 *                  redefined in the call site
 *                  This step ensures operands are saved in callee-save regs or memory 
 *                  and takes into account whether an operand is live at dispatch node entries
 *
 *                  Operand splitting is performed before the instruction (when necessary)
 *              4.1.1.3) All use operands of the instruction are traversed
 *                  and operand splitting is performed before the instruction (when necessary)
 *              
 *      The current implementation doesn't deal properly with conditional memory constraints.
 *      I.e. it doesn't resolve properly things like ADD m, m when both operands are already
 *      assigned.
 * 
 *      For more details please refer to ConstraintsResolverImpl source code
 */

class ConstraintsResolverImpl
{
public:
    ConstraintsResolverImpl(IRManager &irm, bool _second = false)
        :irManager(irm), 
        memoryManager("ConstraintsResolverImpl"),
        basicBlocks(memoryManager, 0), originalOpndCount(0),
        liveOpnds(memoryManager,0),
        liveAtDispatchBlockEntry(memoryManager,0),
        needsOriginalOpnd(memoryManager,0),
        hoveringOpnds(memoryManager,0),
        opndReplaceWorkset(memoryManager,0),
        opndUsage(memoryManager,0),

        callSplitThresholdForNoRegs((unsigned)-1),  // always
        callSplitThresholdFor1Reg(1),               // for very cold code
        callSplitThresholdFor4Regs(1),              // for very cold code
        defSplitThresholdForNoRegs(0),              // never
        defSplitThresholdFor1Reg(0),                // never
        defSplitThresholdFor4Regs(0),               // never
        useSplitThresholdForNoRegs(0),              // never
        useSplitThresholdFor1Reg(0),                // never
        useSplitThresholdFor4Regs(0),               // never

        second(_second)
    {
    }
    
    void run();

private:
    /** Get the priority of a the node for sorting in createBasicBlockArray */
    double getBasicBlockPriority(Node * node);
    /** Fills the basicBlocks array and orders it according to block exec count (hottest first) */ 
    void createBasicBlockArray();
    /** Fills the liveAtDispatchBlockEntry bit set with operands live at dispatch node entries */ 
    void calculateLiveAtDispatchBlockEntries();
    /** Pre-sets calculated constraints for each operand */ 
    void calculateStartupOpndConstraints();
    /** Scans basicBlocks array and calls resolveConstraints(BasicBlock * bb) for each entry */
    void resolveConstraints();
    /** Traverses instructions of bb and calls resolveConstraints(Inst *) 
     * for each inst 
     */
    void resolveConstraints(Node* bb);
    /**
     Main logic of constraint resolution for each instrution
     */
    void resolveConstraintsWithOG(Inst * inst);

    /** returns constraint describing call-safe locations for opnd in CallInst inst */
    Constraint getCalleeSaveConstraint(Inst * inst, Opnd * opnd);
    /** returns constraint describing safe locations for operands live at dispatch node entries */
    Constraint getDispatchEntryConstraint(Opnd * opnd);

    static bool constraintIsWorse(Constraint cnew, Constraint cold, unsigned normedBBExecCount, 
        unsigned splitThresholdForNoRegs, unsigned splitThresholdFor1Reg, unsigned splitThresholdFor4Regs
        );
   


    /** Reference to IRManager */ 
    IRManager&                  irManager;

    /** Private memory manager for this algorithm */ 
    MemoryManager               memoryManager;

    /** Array of basic blocks to be handled */ 
    Nodes                       basicBlocks;

    /** result of irManager.getOpndCount before the pass */ 
    U_32                      originalOpndCount;

    /** Current live set, updated as usual for each instruction in resolveConstraints(Inst*) */ 
    BitSet                      liveOpnds;

    /** Bit set of operands live at dispatch node entries */ 
    BitSet                      liveAtDispatchBlockEntry;

    /** Bit set of operands for which original operand should be used wherever possible.
     *  Currently this is only for operands which are live at dispatch block entries.
     */ 
    BitSet                      needsOriginalOpnd;

    /** Temporary bit set of hovering operands (live across call sites)
     *  Is initialized and used only during resolveConstraints(Inst*)
     */ 
    BitSet                      hoveringOpnds;

    /** An array of current substitutions for operands
     *  Is filled as a result of operand splitting.
     *  Reset for each basic blocks (all replacements are local within basic blocks)
     */
    StlVector<Opnd*>            opndReplaceWorkset;


    StlVector<U_32>           opndUsage;

    unsigned callSplitThresholdForNoRegs;
    unsigned callSplitThresholdFor1Reg;
    unsigned callSplitThresholdFor4Regs;

    unsigned defSplitThresholdForNoRegs;
    unsigned defSplitThresholdFor1Reg;
    unsigned defSplitThresholdFor4Regs;

    unsigned useSplitThresholdForNoRegs;
    unsigned useSplitThresholdFor1Reg;
    unsigned useSplitThresholdFor4Regs;

    bool second;

    friend class ConstraintsResolver;
};

}}; //namespace Ia32

