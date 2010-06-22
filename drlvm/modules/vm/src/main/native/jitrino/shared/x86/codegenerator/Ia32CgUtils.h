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

#if !defined(__IA32_CGUTILS_INCLUDED__)
#define __IA32_CGUTILS_INCLUDED__

#include "Ia32IRManager.h"

namespace Jitrino {
namespace Ia32 {

class IRManagerHolder {
public:
    IRManagerHolder()
    {
        m_irManager = NULL;
    }
    
    void setIRManager(IRManager* irm)
    {
        m_irManager = irm;
    }
    IRManager* getIRManager(void)
    {
        return m_irManager;
    }
protected:
    IRManager*  m_irManager;
};

/**
 * A mix-in that provides various Opnd manipulations IRManager.
 */
class OpndUtils : virtual protected IRManagerHolder {
public:
    OpndUtils()
    {
        m_opndIntZero = NULL;
        m_opndDoubleZero = NULL;
        m_opndFloatZero = NULL;
    }
    
    /**
     * Tests whether the Opnd has only one def.
     */
    static bool isSingleDef(const Opnd* opnd);
    
    /**
     * Tests whether the given operand is placed on (any) register and 
     * optionally test against the specified RegName (\c what).
     */
    static bool isReg(const Opnd* op, RegName what = RegName_Null);
    
    /**
     * Tests whether the given operand is placed on XMM register and
     * optionally test against the specified RegName (\c what).
     */
    static bool isXmmReg(const Opnd* op, RegName what = RegName_Null);
    
    /**
     * Tests whether the given operand is placed on memory.
     */
    static bool isMem(const Opnd* op);
    
    /**
     * Tests whether the operand is immediate.
     * 
     * @note The method only returns \n true for immediate-s without 
     * RuntimeInfo. This is because the real value of immediate with 
     * RuntimeInfo is unknown until very last - they get resolved only 
     * in CodeEmitter.
     */
    static bool isImm(const Opnd* op);
    
    /**
     * Tests whether the Opnd is immediate and has the provided value.
     * 
     * @note See note at isImm(const Opnd* op).
     */
    static bool isImm(const Opnd* op, int iVal);
    
    /**
     * Tests whether the Opnd is immediate of the zero value.
     * 
     * No more than a named shortcut for isImm(op, 0).
     */
    static bool isZeroImm(const Opnd* op);
    
    /**
     * Tests whether the Opnd is Type::Int8 immediate.
     * 
     * @note See note at isImm(const Opnd* op).
     */
    static bool isImm8(const Opnd* op);
    
    /**
     * Tests whether the Opnd is Type::Int32 immediate.
     * 
     * @note See note at isImm(const Opnd* op).
     */
    static bool isImm32(const Opnd* op);
    
    /**
     * Tests whether the Opnd is an immediate (note: of \b any type)
     * and whether it is equal to the provided value.
     * 
     * @note See note at isImm(const Opnd* op).
     */
    static bool isFPConst(const Opnd* op, double d);
    
    /**
     * Tests whether the Opnd is an immediate (note: of \b any type)
     * and whether it is equal to the provided value.
     * 
     * @note See note at isImm(const Opnd* op).
     */
    static bool isFPConst(const Opnd* op, float f);
    
    /**
     * Tests whether the Opnd is a constant area item.
     */
    static bool isConstAreaItem(const Opnd* op);
    
    /**
     * Extracts address of constant area item.
     *
     * @return NULL if \c op is not a constant area item.
     */
    static const void* extractAddrOfConst(const Opnd* op);
    
    /**
     * Extracts integer constant from constant area item.
     *
     * @note The \c op must be the constant item.
     */
    static int    extractIntConst(const Opnd* op);
    
    /**
     * Extracts double constant from constant area item.
     *
     * @note The \c op must be the constant item.
     */
    static double extractDoubleConst(const Opnd* op);
    
    /**
     * Extracts float constant from constant area item.
     *
     * @note The \c op must be the constant item.
     */
    static float  extractFloatConst(const Opnd* op);
    
    /**
     * Tests whether the provided Opnd is immediate and its value may be 
     * placed in a single byte.
     */
    static bool fitsImm8(const Opnd* op);
    
    /**
     * Tests 2 operands for equality.
     */
    static bool equals(const Opnd* a, const Opnd* b);

    /**
     * Searches for a source of an immediate value (if any) of the specified operand.
     * Returns the "defining" operand or \c NULL.
     */
    static Opnd* findImmediateSource(Opnd* opnd);

    //
    // The following are not static and require IRManager
    //
    Opnd* convertImmToImm8(Opnd* imm);
    Opnd* convertToXmmReg64(Opnd* xmmReg);
    
    Opnd* getIntZeroConst(void);
    Opnd* getDoubleZeroConst(void);
    Opnd* getFloatZeroConst(void);
    Opnd* getZeroConst(Type* type);
    
private:
    Opnd* m_opndIntZero;
    Opnd* m_opndDoubleZero;
    Opnd* m_opndFloatZero;
};

class InstUtils : virtual protected IRManagerHolder {
public:
    static void replaceInst(Inst* old, Inst* brandNewInst);
    static void replaceOpnd(Inst* inst, unsigned index, Opnd* newOpnd);
    static bool instMustHaveBCMapping(Inst* inst);
};

class SubCfgBuilderUtils : virtual protected IRManagerHolder {
public:
    SubCfgBuilderUtils()
    {
        m_subCFG = NULL;
        m_currNode = NULL;
    }
    /**
     * Creates new sub CFG and makes it current.
     */
    ControlFlowGraph* newSubGFG(bool withReturn=true, bool withUnwind=false);
    /**
     * Creates new basic block and makes it current.
     */
    BasicBlock* newBB(void);
    /**
     * Sets current node to operate on.
     */
    Node* setCurrentNode(Node* node);
    /**
     * Returns current node.
     */
    Node* getCurrentNode(void) const;
    /**
     * Returns entry node of current subCFG.
     */
    Node* getSubCfgEntryNode(void);
    /**
     * Returns return node of current subCFG.
     */
    Node* getSubCfgReturnNode(void);
    /**
     * Sets current subCFG to operate on.
     */
    ControlFlowGraph* setSubCFG(ControlFlowGraph* subCFG);
    /**
     * Returns current subCFG.
     */
    ControlFlowGraph* getSubCFG(void);
    /**
     * Replaces the given Inst with the subCFG just built, and clears 
     * current subCFG.
     *
     * Also invokes minor cleanup on CFG when \c purgeEmptyNodes == \c true.
     */
    void propagateSubCFG(Inst* inst, bool purgeEmptyNodes = true);
    /**
     * Creates an instruction with the given mnemonic and arguments and 
     * adds it to the end of current node.
     *
     * Special handling for Mnemonic_MOV: CopyPseudoInstruction 
     * is generated.
     */
    Inst* newInst(Mnemonic mn, unsigned defsCount,
        Opnd* op0 = NULL, Opnd* op1 = NULL, Opnd* op2 = NULL,
        Opnd* op3 = NULL, Opnd* op4 = NULL, Opnd* op5 = NULL);
    /**
     * Creates an instruction with the given mnemonic and arguments and 
     * adds it to the end of current node.
     *
     * Special handling for Mnemonic_MOV: CopyPseudoInstruction 
     * is generated.
     */
    Inst* newInst(Mnemonic mn, 
        Opnd* op0 = NULL, Opnd* op1 = NULL, Opnd*op2 = NULL);
    /**
     * Generates branch instruction with the given mnemonic and given target
     * nodes, adds it to the end of current node, and also adds edges 
     * from current to provided nodes.
     */
    Inst* newBranch(Mnemonic mn, Node* trueTarget, Node* falseTarget,
        double trueProbability=0.5, double falseProbability=0.5);
    /**
     * Creates an edge from the current node to the specified node.
     */
    void connectNodeTo(Node* to);
    /**
     * Creates an edge from \c from node, to the \c to node.
     */
    void connectNodes(Node* from, Node* to);
private:
    ControlFlowGraph * m_subCFG;
    Node* m_currNode;
};


}}; // ~namespace Jitrino::Ia32

#endif  // ~ifndef __IA32_CGUTILS_INCLUDED__
