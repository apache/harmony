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
 * @author Intel, Pavel A. Ozhdikhin
 */

#ifndef CODESELECTORS_H_
#define CODESELECTORS_H_


#if defined(_IPF_)
    #include "IpfCodeGenerator.h"
#else
    #include "ia32/Ia32CodeGenerator.h"
#endif

#include "irmanager.h"

namespace Jitrino {

class _VarCodeSelector : public VarCodeSelector {
public:
    _VarCodeSelector(VarOpnd* opnds, U_32* varMap, GCBasePointerMap& gcMap) 
        : varOpnds(opnds), varIdMap(varMap), gcMap(gcMap)
    {}

    void genCode(Callback& callback);
    U_32 getNumVarOpnds();

private:
    VarOpnd* varOpnds;
    U_32*  varIdMap;
    GCBasePointerMap& gcMap;
};

class _BlockCodeSelector : public BlockCodeSelector {
public:
    _BlockCodeSelector(MemoryManager& mm, IRManager& irmanager, Node* b,CG_OpndHandle** map,
        U_32* varMap, bool sinkConstants0, bool sinkConstantsOne0) 
        : irmanager(irmanager), memManager(mm), opndToCGInstMap(map), 
        localOpndToCGInstMap(mm), varIdMap(varMap), block(b),
        sinkConstants(sinkConstants0), sinkConstantsOne(sinkConstantsOne0), argCount(0)
    {}
    
    virtual ~_BlockCodeSelector() {};

    // maps type and overflow modifier to a ArithmeticOp::Types
    ArithmeticOp::Types mapToArithmOpType(Inst* inst);

    //  maps type and overflow modifier to a RefArithmeticOp::Type
    RefArithmeticOp::Types mapToRefArithmOpType(Inst* inst, Opnd *src);

    //  checks if instruction has an overflow modifier
    bool    isOverflow(Inst* inst);

    //  checks if instruction has an exception modifier that can never except
    bool    isExceptionNever(Inst* inst);

    //  checks if instruction is unsigned
    bool    isUnsigned(Inst *inst);

    // checks if shift instruction needs shift mask
    bool    isShiftMask(Inst *inst);

    DivOp::Types mapToDivOpType(Inst* inst);

    MulHiOp::Types mapToMulHiOpType(Inst * inst);
    
    NegOp::Types mapToNegOpType(Inst* inst);

    //  Maps instruction type to IntegerOp::Types
    IntegerOp::Types mapToIntegerOpType(Inst* inst);

    //  maps type to CompareOp::Types
    CompareOp::Types mapToCompareOpType(Inst* inst);

    //  maps type to CompareZeroOp::Types
    CompareZeroOp::Types mapToCompareZeroOpType(Inst *inst);

    //  Maps compare inst to the CompareOp::Operator
    CompareOp::Operators mapToComparisonOp(Inst* inst);

    //  Maps instruction to ConvertToFpOp::Types
     ConvertToFpOp::Types mapToFpConvertOpType(Inst *inst);

    //  Maps instruction to ConvertToIntOp::Types
    ConvertToIntOp::Types mapToIntConvertOpType(Inst *inst);

    //  Maps instruction to ConvertToIntOp::OverflowMod
    ConvertToIntOp::OverflowMod mapToIntConvertOvfMod(Inst *inst);

    JitHelperCallOp::Id convertJitHelperId(JitHelperCallId callId);

    CG_OpndHandle ** genCallArgs(Inst * call, U_32 arg0Pos);

    CG_OpndHandle ** genCallArgs(Opnd *extraArg, Inst * call, U_32 arg0Pos);

    void genInstCode(InstructionCallback& instructionCallback, Inst *inst, bool genConsts);
    
    void genCode(InstructionCallback& instructionCallback);

private:
    CG_OpndHandle*    getCGInst(Opnd* opnd);
    void setLocalCGInst(CG_OpndHandle* inst, Opnd* opnd);
    void clearLocalCGInsts();
    void setCGInst(CG_OpndHandle* inst,Opnd* opnd);
    U_32 getVarHandle(VarOpnd *var);

    IRManager&              irmanager;
    MemoryManager&          memManager;
    CG_OpndHandle**         opndToCGInstMap;
    StlMap<U_32, CG_OpndHandle*> localOpndToCGInstMap;
    U_32*                 varIdMap;
    Node*                   block;
    InstructionCallback*    callback;
    bool                    sinkConstants;
    bool                    sinkConstantsOne;
    U_32                  argCount;
};

class _CFGCodeSelector : public CFGCodeSelector {
public:
    _CFGCodeSelector(MemoryManager& mm, IRManager& irmanager, ControlFlowGraph* fg,CG_OpndHandle** map,
                     U_32 *varMap, bool sinkConstants0, bool sinkConstantsOne0)
        : irmanager(irmanager), opndToCGInstMap(map), varIdMap(varMap), 
          flowGraph(fg), numNodes(0), memManager(mm), sinkConstants(sinkConstants0),
          sinkConstantsOne(sinkConstantsOne0)
    {
        flowGraph->orderNodes();
        numNodes = flowGraph->getNodeCount();
    }
    void genCode(Callback& callback); 
    U_32 getNumNodes() {return numNodes;}
private:
    IRManager&         irmanager;
    CG_OpndHandle**    opndToCGInstMap;
    U_32*            varIdMap;
    ControlFlowGraph*  flowGraph;
    U_32            numNodes;
    MemoryManager&    memManager;
    bool              sinkConstants;
    bool              sinkConstantsOne;
};

class _MethodCodeSelector : public MethodCodeSelector {
public:
    _MethodCodeSelector(IRManager& irmanager,
                        MethodDesc *desc,
                        VarOpnd* opnds,
                        ControlFlowGraph* fg,
                        OpndManager& opndManager,
                        bool sinkConstants0,
                        bool sinkConstantsOne0) 
        : irmanager(irmanager), varOpnds(opnds), flowGraph(fg), methodDesc(desc), sinkConstants(sinkConstants0), sinkConstantsOne(sinkConstantsOne0) {
        numOpnds = opndManager.getNumSsaOpnds();
        numArgs = opndManager.getNumArgs();
        numVars = opndManager.getNumVarOpnds();
    }
    void selectCode(Callback& callback);
private:
    IRManager&  irmanager;
    U_32      numOpnds;
    U_32      numArgs;
    U_32      numVars;
    VarOpnd*    varOpnds;
    ControlFlowGraph*  flowGraph;
    MethodDesc* methodDesc;
    bool        sinkConstants;
    bool        sinkConstantsOne;
};

} //namespace Jitrino 

#endif /*CODESELECTORS_H_*/
