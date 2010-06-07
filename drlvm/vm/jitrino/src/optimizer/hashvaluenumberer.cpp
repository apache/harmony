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
 *
 */

#include "hashvaluenumberer.h"
#include "deadcodeeliminator.h"
#include "simplifier.h"
#include "CSEHash.h"
#include "opndmap.h"
#include "irmanager.h"
#include "FlowGraph.h"
#include "Dominator.h"
#include "Loop.h"
#include "Inst.h"
#include "Stl.h"
#include "MemoryManager.h"
#include "Log.h"
#include "constantfolder.h"
#include "optimizer.h"
#include "walkers.h"
#include "memoryopt.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(HashValueNumberingPass,hvn,"Hash Value Numbering (CSE)")

void
HashValueNumberingPass::_run(IRManager& irm) {
    computeDominators(irm);
    DominatorTree* dominatorTree = irm.getDominatorTree();
    assert(dominatorTree && dominatorTree->isValid());

    //create loops mapping to be able to fix infinite loops after the optimization
    MemoryManager tmpMM("HashValueNumberingPass::_run");
    InfiniteLoopsInfo loopsMapping(tmpMM);
    DeadCodeEliminator::createLoop2DispatchMapping(irm.getFlowGraph(), loopsMapping);

    //do the optimizations
    HashValueNumberer valueNumberer(irm, *dominatorTree);
    valueNumberer.doValueNumbering();

    //fix infinite loops if found
    DeadCodeEliminator::fixInfiniteLoops(irm, loopsMapping);
}

class SparseCseMap : public SparseScopedMap<CSEHashKey, Inst *> {
public:
    SparseCseMap(MemoryManager& mm) :
        SparseScopedMap<CSEHashKey, Inst *>(mm) {};
    SparseCseMap(size_t n, MemoryManager& mm, OptimizerFlags optimizerFlags) :
        SparseScopedMap<CSEHashKey, Inst *>(n, mm, 
                                            optimizerFlags.hash_init_factor,
                                            optimizerFlags.hash_resize_factor,
                                            optimizerFlags.hash_resize_to) {};
};

class InstValueNumberer : public InstOptimizer {
public:
    virtual ~InstValueNumberer() {
    }

    void enterScope() {
        if(Log::isEnabled()) {
            Log::out() << "Entering VN scope" << ::std::endl;
        }
        cseHashTable.enter_scope();
        if (constantTable)
            constantTable->enter_scope();
    };
    void exitScope() {
        if (constantTable)
            constantTable->exit_scope();
        cseHashTable.exit_scope();
        if(Log::isEnabled()) {
            Log::out() << "Exiting VN scope" << ::std::endl;
        }
    };
    
    InstValueNumberer(MemoryManager& memoryManager0, 
                      IRManager& irm,
                      DominatorTree &domTree0,
                      MemoryOpt *mopt,
                      bool cse_final0,
                      ControlFlowGraph &fg0,
                      bool is_scoped)
        : memoryManager(memoryManager0),
          cseHashTable(fg0.getNodes().size() * 
                       irm.getOptimizerFlags().hash_node_tmp_factor,
                       memoryManager, irm.getOptimizerFlags()),
          constantTable(0),
          irManager(irm),
          tauUnsafe(0),
          tauPoint(0),
          domTree(domTree0),
          memOpt(mopt),
          cse_final(cse_final0),
          fg(fg0)
    {
        const OptimizerFlags& optimizerFlags = irm.getOptimizerFlags();
        if (is_scoped && optimizerFlags.hvn_constants) {
            constantTable 
                = new (memoryManager) SparseOpndMap(fg0.getNodes().size() * optimizerFlags.hash_node_constant_factor,
                                                    memoryManager,
                                                    1, 4, 7);
        }
    }
    
    virtual Inst* optimizeInst(Inst* inst) {
        // first copy propagates all sources of the instruction
        DeadCodeEliminator::copyPropagate(inst);
        // then, if we are using constant propagation based on branches, try it
        if (constantTable) {
            U_32 numSrcs = inst->getNumSrcOperands();
            for (U_32 i=0; i<numSrcs; ++i) {
                Opnd *thisOpnd = inst->getSrc(i);
                Opnd *foundOpnd = constantTable->lookup(thisOpnd);
                if (foundOpnd) {
                    inst->setSrc(i, foundOpnd);
                }
            }
        }
        // then hash
        return dispatch(inst);
    }
    Inst* caseAdd(Inst* inst)               { return hashIfNoException(inst); }
    Inst* caseSub(Inst* inst)               { return hashIfNoException(inst); }  
    Inst* caseMul(Inst* inst)               { return hashIfNoException(inst); }
    Inst* caseTauDiv(Inst* inst)            { return hashInst(inst, getKey(inst->getOperation(),
                                                                           inst->getSrc(0)->getId(),
                                                                           inst->getSrc(1)->getId())); }
    Inst* caseTauRem(Inst* inst)            { return hashInst(inst, getKey(inst->getOperation(),
                                                                           inst->getSrc(0)->getId(),
                                                                           inst->getSrc(1)->getId())); }
    Inst* caseNeg(Inst* inst)               { return hashInst(inst); }
    Inst* caseMulHi(Inst* inst)             { return hashInst(inst); }
    Inst* caseMin(Inst* inst)               { return hashInst(inst); }
    Inst* caseMax(Inst* inst)               { return hashInst(inst); }
    Inst* caseAbs(Inst* inst)               { return hashInst(inst); }
    
    // Bitwise  
    Inst* caseAnd(Inst* inst)               { return hashInst(inst); }
    Inst* caseOr(Inst* inst)                { return hashInst(inst); }
    Inst* caseXor(Inst* inst)               { return hashInst(inst); }
    Inst* caseNot(Inst* inst)               { return hashInst(inst); }
    
    // selection
    Inst* caseSelect(Inst* inst)            { return hashInst(inst); }
    
    // conversion
    Inst* caseConv(Inst* inst)              { return hashIfNoException(inst); }
    Inst* caseConvZE(Inst* inst)            { return hashIfNoException(inst); }
    Inst* caseConvUnmanaged(Inst* inst)     { return caseDefault(inst); }
    
    // shifts
    Inst* caseShladd(Inst* inst)            { return hashInst(inst); }
    Inst* caseShl(Inst* inst)               { return hashInst(inst); }
    Inst* caseShr(Inst* inst)               { return hashInst(inst); }
    
    // comparison
    Inst* caseCmp(Inst* inst)               { return hashInst(inst); }
    Inst* caseCmp3(Inst* inst)              { return hashInst(inst); }
    
    // control flow
    Inst* caseBranch(BranchInst* inst) { 
        Inst *res = lookupInst(inst);
        if(Log::isEnabled()) {
            Log::out() << "caseBranch ";
            inst->print(Log::out());
            Log::out() << " yields ";
            if (res) { 
                res->print(Log::out());
            } else {
                Log::out() << "NULL";
            }
            Log::out() << ::std::endl;
        }
        return res;
    }
    
    Inst* caseJump(BranchInst* inst) { return caseDefault(inst); }
    
    Inst* caseSwitch(SwitchInst* inst) { return caseDefault(inst); }
    
    Inst* caseDirectCall(MethodCallInst* inst) { return caseDefault(inst); }
    
    Inst* caseTauVirtualCall(MethodCallInst* inst) { return caseDefault(inst); }
    
    Inst* caseIndirectCall(CallInst* inst) { return caseDefault(inst); }
    
    Inst* caseIndirectMemoryCall(CallInst* inst) { return caseDefault(inst); }
    
    Inst* caseJitHelperCall(JitHelperCallInst* inst) {return caseDefault(inst);}

    Inst* caseVMHelperCall(VMHelperCallInst* inst) {return caseDefault(inst);}

    Inst* caseReturn(Inst* inst) { return caseDefault(inst); }
    
    Inst* caseCatch(Inst* inst) { return caseDefault(inst); }
    
    Inst* caseThrow(Inst* inst) { return caseDefault(inst); }

    Inst* casePseudoThrow(Inst* inst) { return caseDefault(inst); }

    Inst* caseThrowSystemException(Inst* inst) { return caseDefault(inst); }
    
    Inst* caseThrowLinkingException(Inst* inst) { return caseDefault(inst); }
    
    Inst* caseRethrow(Inst* inst) { return caseDefault(inst); }
    
    Inst* caseLeave(Inst* inst) { return caseDefault(inst); }
    
    Inst* caseJSR(Inst* inst) { return caseDefault(inst); }
    
    Inst* caseRet(Inst* inst) { return caseDefault(inst); }
    
    Inst* caseSaveRet(Inst* inst) { return caseDefault(inst); }
    
    // load, store & move
    virtual Inst*
    caseCopy(Inst* inst) { return caseDefault(inst); }
    
    virtual Inst*
    caseDefArg(Inst* inst) { return caseDefault(inst); }
    
    // load of constants
    Inst* caseLdConstant(ConstInst* inst)   { return hashInst(inst); }
    Inst* caseLdNull(ConstInst* inst)       { return hashInst(inst); }
    Inst* caseLdRef(TokenInst* inst)        { return hashInst(inst); }
    
    // variable access
    Inst* caseLdVar(Inst* inst) {
        SsaVarOpnd* ssaVarOpnd = inst->getSrc(0)->asSsaVarOpnd();
        if (ssaVarOpnd == NULL)
            return inst;
        //
        // eliminate redundant ldvars of the same ssa var
        //
        return hashInst(inst);
    }
    Inst* caseLdVarAddr(Inst* inst)                 { return caseDefault(inst); }
    
    // Loads:
    Inst* caseTauLdInd(Inst* inst) {
        bool is_final = false;
        bool is_volatile = false;
        
        {
            Inst *srcInst = inst->getSrc(0)->getInst();
            if ((srcInst->getOpcode() == Op_LdFieldAddr) ||
                (srcInst->getOpcode() == Op_LdStaticAddr)) {
                FieldAccessInst *faInst = srcInst->asFieldAccessInst();
                FieldDesc *fd = faInst->getFieldDesc();
                is_volatile = fd->isVolatile();
                if (fd->isInitOnly()) {
                    is_final = true;
                    // first check for System stream final fields which vary
                    NamedType *td = fd->getParentType();
                    if (strncmp(td->getName(),"java/lang/System",20)==0) {
                        const char *fdname = fd->getName();
                        if ((strncmp(fdname,"in",5)==0) ||
                            (strncmp(fdname,"out",5)==0) ||
                            (strncmp(fdname,"err",5)==0)) {
                            is_final = false;
                        }
                    }
                }
            }
        }
        if ((cse_final && is_final) ||
            (memOpt && !is_volatile)) {
            Inst *res = lookupInst(inst);
            Operation op = inst->getOperation();
            if (Log::isEnabled()) {
                Log::out() << "Ldind looking for "
                           << (int) op.encodeForHashing()
                           << ", "
                           << (int) inst->getSrc(0)->getId()
                           << ::std::endl;
            }
            if (res != inst) {
                if (Log::isEnabled()) {
                    Log::out() << "found hash" << ::std::endl;
                }
                if (res->getOpcode() == Op_TauLdInd) {
                    if ((!memOpt) || memOpt->hasSameReachingDefs(res, inst) ||
                        memOpt->hasDefReachesUse(res, inst)) {
                        if (memOpt) memOpt->remMemInst(inst);
                        return res;
                    }
                } else if (res->getOpcode() == Op_TauStInd ||res->getOpcode() == Op_TauStRef) {
                    if ((!memOpt) || memOpt->hasDefReachesUse(res, inst)) {
                        if (memOpt) memOpt->remMemInst(inst);
                        return res->getSrc(0)->getInst();
                    }
                }
            }
            // if previous cases fail,
            {
                if (Log::isEnabled()) {
                    Log::out() << "not found" << ::std::endl;
                }
                Type::Tag tag = inst->getType();
                Type::Tag otherTag = Type::Void;
                switch (tag) {
                case Type::Int8: otherTag = Type::UInt8; break;
                case Type::Int16: otherTag = Type::UInt16; break;
                case Type::Int32: otherTag = Type::UInt32; break;
                case Type::Int64: otherTag = Type::UInt64; break;
                case Type::UInt8: otherTag = Type::Int8; break;
                case Type::UInt16: otherTag = Type::Int16; break;
                case Type::UInt32: otherTag = Type::Int32; break;
                case Type::UInt64: otherTag = Type::Int64; break;
                default:
                    break;
                }
                if (otherTag != Type::Void) {
                    // check for store with other sign.
                    Operation newop = inst->getOperation();
                    newop.setType(otherTag);
                    CSEHashKey key = getKey(newop, inst->getSrc(0)->getId());
                    if (Log::isEnabled()) {
                        Log::out() << "Ldind looking for changed-sign form "
                                   << (int) newop.encodeForHashing()
                                   << ::std::endl;
                    }
                    Inst *res = lookupInst(inst, key);
                    if (res != inst) {
                        if (Log::isEnabled()) {
                            Log::out() << "found hash" << ::std::endl;
                        }
                        Opnd *dataOpnd = 0;
                        if (res->getOpcode() == Op_TauLdInd) {
                            // actually should not happen
                            if ((!memOpt) || memOpt->hasSameReachingDefs(res, inst) ||
                                memOpt->hasDefReachesUse(res, inst)) {
                                if (memOpt) memOpt->remMemInst(inst);
                                dataOpnd = res->getDst();
                            }
                        } else if (res->getOpcode() == Op_TauStInd ||res->getOpcode() == Op_TauStRef) {
                            if ((!memOpt) || memOpt->hasDefReachesUse(res, inst)) {
                                if (memOpt) memOpt->remMemInst(inst);
                                dataOpnd = res->getSrc(0);
                            }
                        }
                        if (dataOpnd) {
                            Type *dstType = inst->getDst()->getType();
                            Opnd *newDst = irManager.getOpndManager().createSsaTmpOpnd(dstType);
                            Inst *convInst = 
                                irManager.getInstFactory().makeConv(Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                                                    dstType->tag,
                                                                    newDst,
                                                                    dataOpnd);
                            if (Log::isEnabled()) {
                                Log::out() << "Ldind replaced by convInst ";
                                convInst->print(Log::out());
                                Log::out() << ::std::endl;
                            }
                            convInst->insertAfter(inst);
                            return convInst;
                        }
                    }
                    if (Log::isEnabled()) {
                        Log::out() << "not found" << ::std::endl;
                    }
                }
                return setHashToInst(inst);
            }
        } else {
            return caseDefault(inst);
        }
    }

    Inst* caseTauLdField(FieldAccessInst *inst) {
        FieldDesc* fd = inst->getFieldDesc();
        bool is_final = false;
        if (fd->isInitOnly()) {
            is_final = true;
            // first check for System stream final fields which vary
            NamedType *td = fd->getParentType();
            if (strncmp(td->getName(),"java/lang/System",20)==0) {
                const char *fdname = fd->getName();
                if ((strncmp(fdname,"in",5)==0) ||
                    (strncmp(fdname,"out",5)==0) ||
                    (strncmp(fdname,"err",5)==0)) {
                    is_final = false;
                }
            }
        };
        if ((cse_final && is_final) ||
            (memOpt && !fd->isVolatile())) {
            Inst *res = hashInst(inst);
            if (res != inst) {
                if (res->getOpcode() == Op_TauLdField) {
                    if ((!memOpt) || memOpt->hasSameReachingDefs(inst, res)) {
                        if (memOpt) memOpt->remMemInst(inst);
                        return res;
                    }
                } else if (res->getOpcode() == Op_TauStField) {
                    if ((!memOpt) || memOpt->hasDefReachesUse(res, inst)) {
                        if (memOpt) memOpt->remMemInst(inst);
                        return res->getSrc(0)->getInst();
                    }
                }
            }
            return hashInst(inst);
        } else {
            return caseDefault(inst);
        }
    };
    Inst* caseLdStatic(FieldAccessInst *inst) {
        FieldDesc* fd = inst->getFieldDesc();
        bool is_final = false;
        if (fd->isInitOnly()) {
            is_final = true;
            // first check for System stream final fields which vary
            NamedType *td = fd->getParentType();
            if (strncmp(td->getName(),"java/lang/System",20)==0) {
                const char *fdname = fd->getName();
                if ((strncmp(fdname,"in",5)==0) ||
                    (strncmp(fdname,"out",5)==0) ||
                    (strncmp(fdname,"err",5)==0)) {
                    is_final = false;
                }
            }
        };
        if ((cse_final && is_final) ||
            (memOpt && !fd->isVolatile())) {
            Inst *res = hashInst(inst);
            if (res != inst) {
                if (res->getOpcode() == Op_LdStatic) {
                    if ((!memOpt) || memOpt->hasSameReachingDefs(inst, res)) {
                        if (memOpt) memOpt->remMemInst(inst);
                        return res;
                    }
                } else if (res->getOpcode() == Op_TauStStatic) {
                    if ((!memOpt) || memOpt->hasDefReachesUse(res, inst)) {
                        if (memOpt) memOpt->remMemInst(inst);
                        return res->getSrc(0)->getInst();
                    }
                }
            }
            return hashInst(inst);
        } else {
            return caseDefault(inst);
        }
    };
    Inst* caseTauLdElem(TypeInst *inst) {
        if (memOpt) {
            Inst *res = hashInst(inst);
            if (res != inst) {
                if (res->getOpcode() == Op_TauLdElem) {
                    if ((!memOpt) || memOpt->hasSameReachingDefs(inst, res)) {
                        if (memOpt) memOpt->remMemInst(inst);
                        return res;
                    }
                } else if (res->getOpcode() == Op_TauStElem) {
                    if ((!memOpt) || memOpt->hasDefReachesUse(res, inst)) {
                        if (memOpt) memOpt->remMemInst(inst);
                        return res->getSrc(0)->getInst();
                    }
                }
            }
            return hashInst(inst);
        } else {
            return caseDefault(inst);
        }
    };
    // address loads
    Inst* caseLdFieldAddr(FieldAccessInst* inst)    { return hashInst(inst); }
    Inst* caseLdStaticAddr(FieldAccessInst* inst)   { return hashInst(inst); }
    Inst* caseLdElemAddr(TypeInst* inst)            { return hashInst(inst); }
    Inst* caseTauLdVTableAddr(Inst* inst) { 
        return hashInst(inst, getKey(inst->getOperation(),
                                     inst->getSrc(0)->getId()));
    }
    Inst* caseTauLdIntfcVTableAddr(TypeInst* inst) { 
        return hashInst(inst, getKey(inst->getOperation(),
                                     inst->getSrc(0)->getId(),
                                     inst->getTypeInfo()->getId()));
    }
    Inst* caseTauLdVirtFunAddr(MethodInst* inst) { 
        return hashInst(inst, getKey(inst->getOperation(),
                                     inst->getSrc(0)->getId(),
                                     inst->getMethodDesc()->getId()));
    }
    Inst* caseTauLdVirtFunAddrSlot(MethodInst* inst) { 
        return hashInst(inst, getKey(inst->getOperation(),
                                     inst->getSrc(0)->getId(),
                                     inst->getMethodDesc()->getId()));
    }
    Inst* caseLdFunAddr(MethodInst* inst)           { return hashInst(inst); }
    Inst* caseLdFunAddrSlot(MethodInst* inst)       { return hashInst(inst); }
    Inst* caseGetVTableAddr(TypeInst* inst)         { return hashInst(inst); }
    Inst* caseGetClassObj(TypeInst* inst)           { return hashInst(inst); }

    // array access
    Inst* caseTauArrayLen(Inst* inst) { 
        return hashInst(inst, getKey(inst->getOperation(),
                                     inst->getSrc(0)->getId()));
    }
    Inst* caseLdArrayBaseAddr(Inst* inst)           { return hashInst(inst); }
    Inst* caseAddScaledIndex(Inst* inst)            { return hashInst(inst); }

    // Stores:
    Inst* caseStVar(Inst* inst) { return caseDefault(inst); }
    Inst* caseTauStInd(Inst* inst) { 
        bool is_final = false;
        bool is_volatile = false;
        {
            Inst *ptrInst = inst->getSrc(1)->getInst();
            if ((ptrInst->getOpcode() == Op_LdFieldAddr) ||
                (ptrInst->getOpcode() == Op_LdStaticAddr)) {
                FieldAccessInst *faInst = ptrInst->asFieldAccessInst();
                FieldDesc *fd = faInst->getFieldDesc();
                is_volatile = fd->isVolatile();
                if (fd->isInitOnly()) {
                    is_final = true;
                    // first check for System stream final fields which vary
                    NamedType *td = fd->getParentType();
                    if (strncmp(td->getName(),"java/lang/System",20)==0) {
                        const char *fdname = fd->getName();
                        if ((strncmp(fdname,"in",5)==0) ||
                            (strncmp(fdname,"out",5)==0) ||
                            (strncmp(fdname,"err",5)==0)) {
                            is_final = false;
                        }
                    }
                }
            }
        }
        if ((cse_final && is_final) || (memOpt && !is_volatile)) {
            Opnd *addrOp = inst->getSrc(1);
            Type::Tag typetag = inst->getType();
            Operation op(Op_TauLdInd, typetag);
            if (Log::isEnabled()) {
                Log::out() << "Stind hashing Ldind : "
                           << (int) op.encodeForHashing()
                           << ", "
                           << (int) addrOp->getId()
                           << ::std::endl;
            }
            Modifier m(Modifier(inst->getAutoCompressModifier()) | Modifier(Speculative_No));
            setHashToInst(inst, getKey(Operation(Op_TauLdInd, inst->getType(), m),
                                       addrOp->getId()));
            return inst;
        } else {
            return caseDefault(inst);
        }
    };
    Inst* caseTauStField(Inst* inst) {
        FieldAccessInst *finst = inst->asFieldAccessInst();
        assert(finst);
        FieldDesc *fieldDesc = finst->getFieldDesc();
        bool is_final = false;
        bool is_volatile = fieldDesc->isVolatile();
        {
            if (fieldDesc->isInitOnly()) {
                is_final = true;
                // first check for System stream final fields which vary
                NamedType *td = fieldDesc->getParentType();
                if (strncmp(td->getName(),"java/lang/System",20)==0) {
                    const char *fdname = fieldDesc->getName();
                    if ((strncmp(fdname,"in",5)==0) ||
                        (strncmp(fdname,"out",5)==0) ||
                        (strncmp(fdname,"err",5)==0)) {
                        is_final = false;
                    }
                }
            }
        }
        if ((cse_final && is_final) || (memOpt && !is_volatile)) {
            Opnd *baseOp = finst->getSrc(1);
            setHashToInst(inst, getKey(Operation(Op_TauLdField, inst->getType(),
                                                 inst->getAutoCompressModifier()),
                                       baseOp->getId(),
                                       fieldDesc->getId()));
            return inst;
        } else {
            return caseDefault(inst);
        }
    }
    Inst* caseTauStElem(Inst* inst) {
        if (memOpt) {
            Opnd *arrayOp = inst->getSrc(1);
            Opnd *indexOp = inst->getSrc(2);
            Type *baseType = arrayOp->getType();
            assert(baseType->isArrayType());
            Type *elemType = ((ArrayType*)baseType)->getElementType();
            Type::Tag instType = inst->getType();
            assert(elemType->tag == instType);
            setHashToInst(inst, getKey(Operation(Op_TauLdElem, instType,
                                                 inst->getAutoCompressModifier()),
                                       arrayOp->getId(),
                                       indexOp->getId(),
                                       elemType->getId()));
            return inst;
        } else {
            return caseDefault(inst);
        }
    }
    Inst* caseTauStStatic(Inst* inst) {
        FieldAccessInst *finst = inst->asFieldAccessInst();
        assert(finst);
        FieldDesc *fieldDesc = finst->getFieldDesc();
        bool is_final = false;
        if (fieldDesc->isInitOnly()) {
            // first check for System stream final fields which vary
            NamedType *td = fieldDesc->getParentType();
            if (strncmp(td->getName(),"java/lang/System",20)==0) {
                const char *fdname = fieldDesc->getName();
                if ((strncmp(fdname,"in",5)==0) ||
                    (strncmp(fdname,"out",5)==0) ||
                    (strncmp(fdname,"err",5)==0)) {
                    return caseDefault(inst);
                }
            }
            is_final = true;
        };
        if ((cse_final && is_final) || 
            (memOpt && !fieldDesc->isVolatile())) {

            setHashToInst(inst, getKey(Operation(Op_LdStatic, inst->getType(),
                                                 inst->getAutoCompressModifier()),
                                       fieldDesc->getId()));
            return inst;
        } else {
            return caseDefault(inst);
        }
    }
    Inst* caseTauStRef(Inst* inst) {
        bool is_final = false;
        bool is_volatile = false;
        {
            Inst *ptrInst = inst->getSrc(1)->getInst();
            if ((ptrInst->getOpcode() == Op_LdFieldAddr) ) {
                FieldAccessInst *faInst = ptrInst->asFieldAccessInst();
                FieldDesc *fd = faInst->getFieldDesc();
                is_volatile = fd->isVolatile();
                if (fd->isInitOnly()) {
                    is_final = true;
                    // first check for System stream final fields which vary
                    NamedType *td = fd->getParentType();
                    if (strncmp(td->getName(),"java/lang/System",20)==0) {
                        const char *fdname = fd->getName();
                        if ((strncmp(fdname,"in",5)==0) ||
                            (strncmp(fdname,"out",5)==0) ||
                            (strncmp(fdname,"err",5)==0)) {
                            is_final = false;
                        }
                    }
                }
            }
        }
        if ((cse_final && is_final) || (memOpt && !is_volatile)) {
            Opnd *addrOp = inst->getSrc(1);
            Type::Tag typetag = inst->getType();
            Operation op(Op_TauLdInd, typetag);
            if (Log::isEnabled()) {
                Log::out() << "StRef hashing Ldind : "
                           << (int) op.encodeForHashing()
                           << ", "
                           << (int) addrOp->getId()
                           << ::std::endl;
            }
            Modifier m(Modifier(inst->getAutoCompressModifier()) | Modifier(Speculative_No));
            setHashToInst(inst, getKey(Operation(Op_TauLdInd, inst->getType(), m),
                                       addrOp->getId()));
            return inst;
        } else {
            return caseDefault(inst);
        }

    }

    // checks 
    Inst* caseTauCheckBounds(Inst* inst)               { return lookupInst(inst); }
    Inst* caseTauCheckLowerBound(Inst* inst)           { return lookupInst(inst); }
    Inst* caseTauCheckUpperBound(Inst* inst)           { return lookupInst(inst); }
    Inst* caseTauCheckNull(Inst* inst){ 
        Inst *found = findIsNonNull(inst->getSrc(0));
        if (found)
            return found;
        return lookupInst(inst); 
    }
    Inst* caseTauCheckZero(Inst* inst)                 { return lookupInst(inst); }
    Inst* caseTauCheckDivOpnds(Inst* inst)             { return lookupInst(inst); }
    Inst* caseTauCheckElemType(Inst* inst) { 
        // skip tau operand when hashing
        CSEHashKey key = getKey(inst->getOperation(), 
                                inst->getSrc(0)->getId(),
                                inst->getSrc(1)->getId());
        return lookupInst(inst, key);
    }
    Inst* caseTauCheckFinite(Inst* inst)               { return lookupInst(inst); }

    // alloc

    virtual Inst*
    caseNewObj(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseNewArray(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseNewMultiArray(Inst* inst) { return caseDefault(inst); }

    // sync

    virtual Inst*
    caseTauMonitorEnter(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseTauMonitorExit(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseTypeMonitorEnter(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseTypeMonitorExit(Inst* inst) { return caseDefault(inst); }

    Inst* caseLdLockAddr(Inst* inst)                { return hashInst(inst); }

    virtual Inst*
    caseIncRecCount(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseTauBalancedMonitorEnter(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseBalancedMonitorExit(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseTauOptimisticBalancedMonitorEnter(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseOptimisticBalancedMonitorExit(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseMonitorEnterFence(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseMonitorExitFence(Inst* inst) { return caseDefault(inst); }

    // type checking
    Inst* caseTauStaticCast(TypeInst* inst) { 
        CSEHashKey key = getKey(inst->getOperation(), 
                                inst->getSrc(0)->getId(),
                                inst->getTypeInfo()->getId());
        return hashInst(inst, key);
    }
    Inst *findIsNonNull(Opnd *srcOp) {
        CSEHashKey nullCheckKey = getKey(Op_TauCheckNull,
                                         srcOp->getId());
        Inst *nullCheck = lookupHash(nullCheckKey);
        if (nullCheck) {
            return nullCheck;
        }
        CSEHashKey isNonNullKey = getKey(Operation(Op_TauIsNonNull,
                                                   srcOp->getType()->tag,
                                                   Modifier()),
                                         srcOp->getId());
        Inst *isNonNull = lookupHash(isNonNullKey);
        if (isNonNull) {
            return isNonNull;
        }
        Inst *srcOpInst = srcOp->getInst();
        Opcode srcOpOpcode = srcOpInst->getOpcode();
        if (srcOpOpcode == Op_TauStaticCast) {
            Opnd *castSrcOp = srcOpInst->getSrc(0);
            return findIsNonNull(castSrcOp);
        }
        return 0;
    }
    Inst* caseTauCast(TypeInst* inst) {
        CSEHashKey key = getKey(inst->getOperation(), 
                                inst->getSrc(0)->getId(),
                                inst->getTypeInfo()->getId());
        Inst *res = lookupInst(inst, key);
        if (res) return res;
        // try to get a better nullcheck operand.
        Inst *nullChecked = findIsNonNull(inst->getSrc(0));
        if (nullChecked) {
            inst->setSrc(1, nullChecked->getDst());
        }
        return inst;
    }
    Inst* caseSizeof(TypeInst* inst) { 
        return hashInst(inst);
    }
    Inst* caseTauAsType(TypeInst* inst) { 
        CSEHashKey key = getKey(inst->getOperation(), 
                                inst->getSrc(0)->getId(),
                                inst->getTypeInfo()->getId());
        Inst *res = hashInst(inst, key);
        if (res != inst)
            return res;
        // try to get a better nullcheck operand if available.
        Inst *nullChecked = findIsNonNull(inst->getSrc(0));
        if (nullChecked) {
            inst->setSrc(1, nullChecked->getDst());
        }
        return inst;
    }
    Inst* caseTauInstanceOf(TypeInst* inst) { 
        CSEHashKey key = getKey(inst->getOperation(), 
                                inst->getSrc(0)->getId(),
                                inst->getTypeInfo()->getId());
        Inst *res = hashInst(inst, key);
        if (res != inst)
            return res;
        // try to get a better nullcheck opnd if available.
        Inst *nullChecked = findIsNonNull(inst->getSrc(0));
        if (nullChecked) {
            inst->setSrc(1, nullChecked->getDst());
        }
        return inst;
    }
    Inst* caseInitType(TypeInst* inst) { return hashIfNoException(inst); }

    // labels

    virtual Inst*
    caseLabel(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseCatchLabelInst(Inst* inst) { return caseDefault(inst); }

    // method entry/exit
    virtual Inst*
    caseMethodEntryLabel(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseMethodEntry(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseMethodEnd(Inst* inst) { return caseDefault(inst); }

    // source markers
    virtual Inst*
    caseMethodMarker(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    casePhi(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseTauPi(TauPiInst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseIncCounter(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    casePrefetch(Inst* inst) { return caseDefault(inst); }

    // compressed references

    Inst* caseUncompressRef(Inst* inst)  { return hashInst(inst); }

    Inst* caseCompressRef(Inst* inst)  { return hashInst(inst); }

    virtual Inst*
    caseLdFieldOffset(Inst* inst) { return hashInst(inst); }

    virtual Inst*
    caseLdFieldOffsetPlusHeapbase(Inst* inst) { return hashInst(inst); }

    virtual Inst*
    caseLdArrayBaseOffset(Inst* inst) { return hashInst(inst); }

    virtual Inst*
    caseLdArrayBaseOffsetPlusHeapbase(Inst* inst) { return hashInst(inst); }

    virtual Inst*
    caseLdArrayLenOffset(Inst* inst) { return hashInst(inst); }

    virtual Inst*
    caseLdArrayLenOffsetPlusHeapbase(Inst* inst) { return hashInst(inst); }

    virtual Inst*
    caseAddOffset(Inst* inst) { return hashInst(inst); }

    virtual Inst*
    caseAddOffsetPlusHeapbase(Inst* inst) { return hashInst(inst); }

    // new tau methods
    virtual Inst*
    caseTauPoint(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseTauEdge(Inst* inst) { return caseDefault(inst); }

    virtual Inst*
    caseTauAnd(Inst* inst) { return hashInst(inst); }

    virtual Inst*
    caseTauSafe(Inst* inst) { return hashInst(inst); }

    virtual Inst*
    caseTauUnsafe(Inst* inst) { return hashInst(inst); }

    virtual Inst*
    caseTauCheckCast(TypeInst* inst) { 
        CSEHashKey key = getKey(Operation(Op_TauCheckCast, Type::SystemObject,
                                          Modifier(Exception_Sometimes)),
                                inst->getSrc(0)->getId(),
                                inst->getTypeInfo()->getId(),
                                inst->getNode()->getExceptionEdgeTarget()->getId());
        return hashInst(inst, key);
    }

    virtual Inst*
    caseTauHasType(TypeInst* inst) { 
        CSEHashKey key = getKey(Operation(Op_TauHasType),
                                inst->getSrc(0)->getId(),
                                inst->getTypeInfo()->getId());
        return hashInst(inst, key);
    }

    virtual Inst*
    caseTauHasExactType(TypeInst* inst) { 
        CSEHashKey key = getKey(Operation(Op_TauHasExactType),
                                inst->getSrc(0)->getId(),
                                inst->getTypeInfo()->getId());
        return hashInst(inst, key);
    }

    virtual Inst*
    caseTauIsNonNull(Inst* inst) { 
        Inst *found = findIsNonNull(inst->getSrc(0));
        if (found)
            return found;
        return hashInst(inst);
    }
    
    Inst* caseIdentHC(Inst* inst) {
        return inst;
    }
    
    // default
    Inst* caseDefault(Inst* inst)                   { return inst; }
private:
    CSEHashKey getKey(Inst *inst) {
   
        if(inst->isType()) {
            return getKey(inst->asTypeInst());
        } else if(inst->isFieldAccess()) {
            return getKey(inst->asFieldAccessInst());
        } else if(inst->isConst()) {
            return getKey(inst->asConstInst());
        } else if(inst->isToken()) {
            return getKey(inst->asTokenInst());
        } else if(inst->isMethod()) {
            return getKey(inst->asMethodInst());
        } else if(inst->isBranch()) {
            return getKey(inst->asBranchInst());
        }

        // eliminate tau operands from the key
        // they will always be trailing operands
        // but: some instructions have just tau operands,
        //  (tauAnd, ldvar)
        // so if first operand is a tau, don't skip any

        U_32 numSrcs = inst->getNumSrcOperands();
        if (numSrcs > 0) {
            if (inst->getSrc(0)->getType()->tag != Type::Tau) {
                while ((numSrcs > 0) && (inst->getSrc(numSrcs-1)->getType()->tag == Type::Tau)) {
                    numSrcs -= 1;
                }
            }
        }
        switch (numSrcs) {
        case 0: return getKey(inst->getOperation());
        case 1: return getKey(inst->getOperation(),
                              inst->getSrc(0)->getId());
        case 2: return getKey(inst->getOperation(),
                              inst->getSrc(0)->getId(),
                              inst->getSrc(1)->getId());
        case 3: return getKey(inst->getOperation(),
                              inst->getSrc(0)->getId(),
                              inst->getSrc(1)->getId(), 
                              inst->getSrc(2)->getId());
        default:
            assert(0);
        }
        return getKey();
    }
    CSEHashKey getKey(FieldAccessInst *inst) {
        FieldDesc* fieldDesc = inst->getFieldDesc();
        U_32 numSrcs = inst->getNumSrcOperands();
        if (numSrcs > 0) {
            if (inst->getSrc(0)->getType()->tag != Type::Tau) {
                while ((numSrcs > 0) && (inst->getSrc(numSrcs-1)->getType()->tag == Type::Tau)) {
                    numSrcs -= 1;
                }
            }
        }
        switch (numSrcs) {
        case 0: return getKey(inst->getOperation(),
                              fieldDesc->getId());
        case 1: return getKey(inst->getOperation(),
                              inst->getSrc(0)->getId(), 
                              fieldDesc->getId());
        default: 
            assert(0);
        }
        return getKey();
    }
    CSEHashKey getKey(TypeInst* inst) {
        Type* typeInfo = inst->getTypeInfo();
        U_32 numSrcs = inst->getNumSrcOperands();
        if (numSrcs > 0) {
            if (inst->getSrc(0)->getType()->tag != Type::Tau) {
                while ((numSrcs > 0) && (inst->getSrc(numSrcs-1)->getType()->tag == Type::Tau)) {
                    numSrcs -= 1;
                }
            }
        }
        switch (numSrcs) {
        case 0: return getKey(inst->getOperation(),
                              typeInfo->getId());
        case 1: return getKey(inst->getOperation(),
                              inst->getSrc(0)->getId(),
                              typeInfo->getId());
        case 2: return getKey(inst->getOperation(),
                              inst->getSrc(0)->getId(),
                              inst->getSrc(1)->getId(),
                              typeInfo->getId());
        default:
            assert(0);
        }
        return getKey();
    }
    CSEHashKey getKey(ConstInst* inst) {
        return getKey(inst->getOperation(),
                      (U_32)inst->getValue().dword1,
                      (U_32)inst->getValue().dword2);
    }
    CSEHashKey getKey(TokenInst* inst) {
        return getKey(inst->getOperation(),
                      inst->getEnclosingMethod()->getId(),
                      inst->getToken());
    }
    CSEHashKey getKey(MethodInst* inst) {
        MethodDesc* methodDesc = inst->getMethodDesc();
        U_32 numSrcs = inst->getNumSrcOperands();
        if (numSrcs > 0) {
            if (inst->getSrc(0)->getType()->tag != Type::Tau) {
                while ((numSrcs > 0) && (inst->getSrc(numSrcs-1)->getType()->tag == Type::Tau)) {
                    numSrcs -= 1;
                }
            }
        }
        switch (numSrcs) {
        case 0: return getKey(inst->getOperation(),
                              methodDesc->getId());
        case 1: return getKey(inst->getOperation(),
                              inst->getSrc(0)->getId(),
                              methodDesc->getId());
        default:
            assert(0);
        }
        return getKey();
    }
    CSEHashKey getKey(BranchInst* inst) {
        switch (inst->getNumSrcOperands()) {
        case 1: return getKey(inst->getOperation(),
                              inst->getSrc(0)->getId());
        case 2: return getKey(inst->getOperation(),
                              inst->getSrc(0)->getId(),
                              inst->getSrc(1)->getId());
        default:
            assert(0);
        }
        return getKey();
    }
    CSEHashKey getKey() {
        return CSEHashKey();
    }
    CSEHashKey getKey(Operation operation) {
        return CSEHashKey(operation.encodeForHashing());
    }
    CSEHashKey getKey(Operation operation, U_32 srcid1) {
        return CSEHashKey(operation.encodeForHashing(), srcid1);
    }
    CSEHashKey getKey(Operation operation, U_32 srcid1, U_32 srcid2) {
        return CSEHashKey(operation.encodeForHashing(), srcid1, srcid2);
    }
    CSEHashKey getKey(Operation operation, U_32 srcid1, U_32 srcid2, U_32 srcid3) {
        return CSEHashKey(operation.encodeForHashing(), srcid1, srcid2, srcid3);
    }

    Inst* hashInst(Inst* inst) {
        return hashInst(inst, getKey(inst));
    }
    Inst* hashInst(FieldAccessInst* inst) {
        return hashInst(inst, getKey(inst));
    }
    Inst* hashInst(TypeInst* inst) {
        return hashInst(inst, getKey(inst));
    }
    Inst* hashInst(ConstInst* inst) {
        return hashInst(inst, getKey(inst));
    }
    Inst* hashInst(TokenInst* inst) {
        return hashInst(inst, getKey(inst));
    }
    Inst* hashInst(MethodInst* inst) {
        return hashInst(inst, getKey(inst));
    }
    Inst* hashInst(BranchInst* inst) {
        return hashInst(inst, getKey(inst));
    }
    Inst* hashInst(Inst* inst, CSEHashKey key) {
        if (!key.isNull()) {
            Inst* newInst = lookupHash(key);
            if (newInst != NULL) {
                return newInst;
            }
            // insert inst into the hash table
            setHashToInst(inst, key);
        }
        return inst;
    }

    Inst* setHashToInst(Inst* inst) {
        return setHashToInst(inst, getKey(inst));
    }
    Inst* setHashToInst(FieldAccessInst* inst) {
        return setHashToInst(inst, getKey(inst));
    }
    Inst* setHashToInst(TypeInst* inst) {
        return setHashToInst(inst, getKey(inst));
    }
    Inst* setHashToInst(ConstInst* inst) {
        return setHashToInst(inst, getKey(inst));
    }
    Inst* setHashToInst(TokenInst* inst) {
        return setHashToInst(inst, getKey(inst));
    }
    Inst* setHashToInst(MethodInst* inst) {
        return setHashToInst(inst, getKey(inst));
    }
    Inst* setHashToInst(BranchInst* inst) {
        return setHashToInst(inst, getKey(inst));
    }
    Inst* setHashToInst(Inst* inst, const CSEHashKey &key) {
        if (!key.isNull()) {
            if (Log::isEnabled()) {
                Log::out() << "setting hash ";
                key.print(Log::out());
                Log::out() << " to inst ";
                inst->print(Log::out());
                Log::out() << ::std::endl;
            }
            cseHashTable.insert(key,inst);
        }
        return inst;
    }
    Inst* lookupHash(const CSEHashKey &key) {
        if (!key.isNull()) {
            Inst* newInst = cseHashTable.lookup(key);
            if (newInst != NULL) {
                if (Log::isEnabled()) {
                    Log::out() << "looking for hash ";
                    key.print(Log::out());
                    Log::out() << " found inst ";
                    newInst->print(Log::out());
                    Log::out() << ::std::endl;
                }
                return newInst;
            } else {
                if (Log::isEnabled()) {
                    Log::out() << "looking for hash ";
                    key.print(Log::out());
                    Log::out() << " found NULL" << ::std::endl;
                }
                return 0;
            }
        } else
            return 0;
    }

    bool thereIsAPath(Node* start, Node* finish, Node* commonAncestor)
    {
        assert(domTree.dominates(commonAncestor,start));
        assert(domTree.dominates(commonAncestor,finish));

        const Edges &outedges = start->getOutEdges();
        typedef Edges::const_iterator EdgeIter;
        EdgeIter eLast = outedges.end();
        for (EdgeIter eIter = outedges.begin(); eIter != eLast; eIter++) {
            Edge* outEdge = *eIter;
            Node* succBlock = outEdge->getTargetNode();

            if( domTree.dominates(succBlock,start) )
                continue; // outEdge is a backedge. Skip it.

            if (finish == succBlock) {
                return true;
            } else if ( domTree.dominates(commonAncestor,succBlock) &&
                        thereIsAPath(succBlock,finish,commonAncestor) )
            {
                return true;
            }
        }        
        return false;
    }

    Inst* hashIfNoException(Inst* inst) {
        Modifier mod = inst->getModifier();
        if ((mod.hasOverflowModifier() && mod.getOverflowModifier() != Overflow_None)   ||
            (mod.hasExceptionModifier() && mod.getExceptionModifier() != Exception_Never))
        {
            Inst* optInst = lookupInst(inst);
            if (inst == optInst) {
                setHashToInst(inst, getKey(inst));
                return inst;
            } else {
                Node* block = inst->getNode();
                Node* optBlock = optInst->getNode();
                // we must ensure that optInst was successfully executed
                // if there is a way [block]->[dispatch]->...->[optBlock]
                // we should not consider optInst as successfully executed
                // so inst can not be eliminated, but it should be added
                // into hash table instead of optInst
                Node* dispatch = optBlock->getEdgeTarget(Edge::Kind_Dispatch);
                if ( dispatch && domTree.dominates(optBlock,dispatch) &&
                     thereIsAPath(dispatch,block,optBlock))
                {
                    // do not optimize, just add to hash.
                    setHashToInst(inst, getKey(inst));
                    return inst;
                } else {
                    // everything is OK. Can optimize.
                    return optInst;
                }
            }

        } else {
            // process inst as usual
            return hashInst(inst);
        }
    }

    Inst* lookupInst(Inst* inst) {
        return lookupInst(inst, getKey(inst));
    }
    Inst* lookupInst(FieldAccessInst* inst) {
        return lookupInst(inst, getKey(inst));
    }
    Inst* lookupInst(TypeInst* inst) {
        return lookupInst(inst, getKey(inst));
    }
    Inst* lookupInst(ConstInst* inst) {
        return lookupInst(inst, getKey(inst));
    }
    Inst* lookupInst(TokenInst* inst) {
        return lookupInst(inst, getKey(inst));
    }
    Inst* lookupInst(MethodInst* inst) {
        return lookupInst(inst, getKey(inst));
    }
    Inst* lookupInst(BranchInst* inst) {
        Inst *res = lookupInst(inst, getKey(inst));
        if(Log::isEnabled()) {
            Log::out() << "lookupInst(Branch ";
            inst->print(Log::out());
            Log::out() << " with hashcode "
                       << (int) inst->getOperation().encodeForHashing();
            Log::out() << " yields ";
            if (res) { 
                res->print(Log::out());
            } else {
                Log::out() << "NULL";
            }
            Log::out() << ::std::endl;
        }
        return res;
    }

    Inst* lookupInst(Inst* inst, CSEHashKey key) {
        if (!key.isNull()) {
            Inst* newInst = lookupHash(key);
            if (newInst != NULL)
                return newInst;
        }
        return inst;
    }

    // Additional routines to branch conditions
public:
    void addBranchConditions(DominatorNode* domNode);
private:
    void addInfoFromBranch(Node* targetNode, BranchInst *branchi, bool isTrueEdge);
    void addInfoFromBranchCompare(Node* targetNode, 
                                  ComparisonModifier mod,
                                  Type::Tag comparisonType,
                                  bool isTrueEdge,
                                  U_32 numSrcOperands,
                                  Opnd *src0,
                                  Opnd *src1);
    void addInfoFromPEI(Inst *pei, bool isExceptionEdge);
protected:
    MemoryManager&      memoryManager;
    SparseCseMap        cseHashTable;        // hash table for value numbering
    SparseOpndMap       *constantTable;      // hash table for constant propagation
    IRManager&          irManager;
    Inst*               tauUnsafe;
    Opnd*               tauPoint;
    DominatorTree       &domTree;
    MemoryOpt           *memOpt;
    bool                cse_final;
    ControlFlowGraph    &fg;
public:
    Inst* getTauUnsafe() {
        if (!tauUnsafe) {
            Node *head = fg.getEntryNode();
            Inst *entryLabel = (Inst*)head->getFirstInst();
            // first search for one already there
            Inst *inst = entryLabel->getNextInst();
            while (inst != NULL) {
                if (inst->getOpcode() == Op_TauUnsafe) {
                    tauUnsafe = inst;
                    return tauUnsafe;
                }
                inst = inst->getNextInst();
            }
            TypeManager &tm = irManager.getTypeManager();
            Opnd *tauOpnd = irManager.getOpndManager().createSsaTmpOpnd(tm.getTauType());
            tauUnsafe = irManager.getInstFactory().makeTauUnsafe(tauOpnd);
            // place after label and Phi instructions
            inst = entryLabel->getNextInst();
            while (inst != NULL) {
                Opcode opc = inst->getOpcode();
                if ((opc != Op_Phi) && (opc != Op_TauPi) && (opc != Op_TauPoint)
                    && (opc != Op_TauEdge))
                    break;
                inst = inst->getNextInst();
            }
            if(Log::isEnabled()) {
                Log::out() << "Inserting tauUnsafe inst ";
                tauUnsafe->print(Log::out());
                if (inst!=NULL) {
                    Log::out() << " before inst ";
                    inst->print(Log::out());
                }
                Log::out() << ::std::endl;
            }
            if (inst != NULL) {
                tauUnsafe->insertBefore(inst);
            } else {
                head->appendInst(tauUnsafe);
            }
            
        }
        return tauUnsafe;
    };

    Opnd* getBlockTauPoint(Node *block) {
        Inst *firstInst = (Inst*)block->getFirstInst();
        Inst *inst = firstInst->getNextInst();
        for (; inst != NULL; inst = inst->getNextInst()) {
            if (inst->getOpcode() == Op_TauPoint) {
                return inst->getDst();
            }
        }
        for (inst = firstInst->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
            if (inst->getOpcode() != Op_Phi) {
                break; // insert before inst.
            }
        }
        // no non-phis, insert before inst;
        TypeManager &tm = irManager.getTypeManager();
        Opnd *tauOpnd = irManager.getOpndManager().createSsaTmpOpnd(tm.getTauType());
        Inst* tauPoint = irManager.getInstFactory().makeTauPoint(tauOpnd);
        if(Log::isEnabled()) {
            Log::out() << "Inserting tauPoint ";
            tauPoint->print(Log::out());
            if (inst!=NULL) {
                Log::out() << " before inst ";
                inst->print(Log::out());
            }
            Log::out() << ::std::endl;
        }
        if (inst!=NULL) {
            tauPoint->insertBefore(inst);
        } else {
            block->appendInst(tauPoint);
        }
        return tauOpnd;
    }
    Opnd* getBlockTauEdge(Node *block) {
        Inst *firstInst = (Inst*)block->getFirstInst();
        Inst *inst = firstInst->getNextInst();
        for (; inst != NULL; inst = inst->getNextInst()) {
            if (inst->getOpcode() == Op_TauEdge) {
                return inst->getDst();
            }
        }
        for (inst = firstInst->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
            if ((inst->getOpcode() != Op_Phi) && (inst->getOpcode() != Op_TauPoint)) {
                break; // insert before inst.
            }
        }
        // no non-phis, insert before inst;
        TypeManager &tm = irManager.getTypeManager();
        Opnd *tauOpnd = irManager.getOpndManager().createSsaTmpOpnd(tm.getTauType());
        Inst* tauEdge = irManager.getInstFactory().makeTauEdge(tauOpnd);
        if(Log::isEnabled()) {
            Log::out() << "Inserting tauEdge ";
            tauEdge->print(Log::out());
            if (inst!=NULL) {
                Log::out() << " before inst ";
                inst->print(Log::out());
            }
            Log::out() << ::std::endl;
        }
        if (inst != NULL) {
            tauEdge->insertBefore(inst);
        } else {
            block->appendInst(tauEdge);
        }
        return tauOpnd;
    }
    bool allowsConstantPropagation(ComparisonModifier mod, Type::Tag comparisonType,
                                   Opnd *src0, Opnd *src1, bool isTrueEdge,
                                   Opnd **opnd, Opnd **constOpnd);
    void recordHasTypeTau(Opnd *opnd,
                          Type *type,
                          Inst *tauHasTypeInst);
};

void
InstValueNumberer::addBranchConditions(DominatorNode* domNode)
{
    DominatorNode *parent = domNode->getParent();
    if (!parent) return; // first node, no predecessors

    // check for a dominating edge
    Node *block = domNode->getNode();
    Node *idom = parent->getNode();
    Edge *domEdge = 0;
    if (idom->hasOnlyOneSuccEdge())
        return; // no conditional branch there, no info to obtain

    if (block->hasOnlyOnePredEdge()) {
        domEdge = *(block->getInEdges().begin());
    } else {
        // idom must be a predecessor,
        // and every other predecessor must be dominated by this block
        const Edges &inedges = block->getInEdges();
        typedef Edges::const_iterator EdgeIter;
        EdgeIter eLast = inedges.end();
        for (EdgeIter eIter = inedges.begin(); eIter != eLast; eIter++) {
            Edge *inEdge = *eIter;
            Node *predBlock = inEdge->getSourceNode();
            if (predBlock == idom) {
                if (domEdge)
                    return; // can't deal with more than one dominating edge
                domEdge = inEdge;
            } else if (domTree.dominates(block, predBlock)) {
                // ok.
            } else {
                // found a predecessor which is not idom, 
                // and is not dominated by this block.

                // edge condition isn't useful.
                return; 
            }
        }        
    }

    if (!domEdge) return; // only take easy info for now.
    Edge::Kind kind = domEdge->getKind();

    Node *predBlock = idom;
    bool taken = false;
    switch(kind) {
    case Edge::Kind_True:
        taken = true;
    case Edge::Kind_False:
        {
            Inst* branchi1 = (Inst*)predBlock->getLastInst();
            assert(branchi1 != NULL);
            BranchInst* branchi = branchi1->asBranchInst();
            if (branchi) {
                addInfoFromBranch(block, branchi, taken);
            } else {
                // default edge of switch, skip it
            }
        }
    break;
    case Edge::Kind_Dispatch:
        taken = true;
    case Edge::Kind_Unconditional:
        // remember the predecessor has multiple out edges, so ut must
        // be non-exception case of a PEI
        { 
            Inst* lasti = (Inst*)predBlock->getLastInst();
            assert(lasti != NULL);
            addInfoFromPEI(lasti, taken);
        }
    break;
    case Edge::Kind_Catch:
        break;
    default: break;
    }
    return;
}

// sets opnd and returns true if we can eliminate a checkZero or checkNull(opnd)
bool allowsAnyZeroElimination(ComparisonModifier mod, Type::Tag comparisonType,
                              Opnd *src0, Opnd *src1, bool isTrueEdge,
                              Opnd **opnd)
{
    bool positive = false;
    switch (mod) {
    case Cmp_EQ:
        positive = true;
    case Cmp_NE_Un:
        {
            bool canelim = false;
            ConstInst::ConstValue cv;
            Opnd *constOpnd = 0;
            Opnd *otherOpnd = 0;
            if (ConstantFolder::isConstant(src0->getInst(), cv)) {
                constOpnd = src0;
                otherOpnd = src1;
            } else if (ConstantFolder::isConstant(src1->getInst(), cv)) {
                constOpnd = src1;
                otherOpnd = src0;
            } else {
                return false;
            }
            bool notEqual = isTrueEdge ^ positive; // we know (nonconst != const)
            switch (constOpnd->getInst()->getType()) {
            case Type::Int8: case Type::Int16: case Type::Int32:
            case Type::UInt8: case Type::UInt16: case Type::UInt32:
                canelim = (notEqual ? (cv.i4 == 0) : (cv.i4 != 0)); break;
            case Type::Int64:
            case Type::UInt64:
                canelim = (notEqual ? (cv.i8 == 0) : (cv.i8 != 0)); break;
            case Type::IntPtr: 
            case Type::UIntPtr:
            case Type::ManagedPtr: case Type::UnmanagedPtr:
            case Type::SystemObject: case Type::SystemClass: case Type::SystemString:
            case Type::Array: case Type::Object: 
            case Type::BoxedValue:
            case Type::MethodPtr: case Type::VTablePtr:
            case Type::CompressedSystemObject: 
            case Type::CompressedSystemClass: 
            case Type::CompressedSystemString:
            case Type::CompressedArray: case Type::CompressedObject:
                canelim = (notEqual ? (cv.i == 0) : (cv.i != 0)); break;
            case Type::NullObject:
            case Type::CompressedNullObject:
                canelim = notEqual; break;
            default:
                break;
            }
            if (canelim) {
                *opnd = otherOpnd;
                return true;
            }
        }
        break;
    case Cmp_GT:
    case Cmp_GT_Un:
        positive = true;
    case Cmp_GTE:
    case Cmp_GTE_Un:
        if (isTrueEdge) {
            Inst *src1inst = src1->getInst();
            ConstInst::ConstValue cv;
            if (ConstantFolder::isConstant(src1inst, cv)) {
                bool canelim = false;
                switch (src1inst->getType()) {
                case Type::Int8: case Type::Int16: case Type::Int32:
                    canelim = (positive ? (cv.i4 >= 0) : (cv.i4 > 0)); break;
                case Type::Int64:
                    canelim = (positive ? (cv.i8 >= 0) : (cv.i8 > 0)); break;
                case Type::IntPtr:
                    canelim = (positive ? true : (cv.i != 0)); break;
                case Type::UInt8: case Type::UInt16: case Type::UInt32:
                    canelim = (positive ? true : (cv.i4 != 0)); break;
                case Type::UInt64:
                    canelim = (positive ? true : (cv.i8 != 0)); break;
                case Type::UIntPtr:
                    canelim = (positive ? true : (cv.i != 0)); break;
                default:
                    break;
                }
                if (canelim) {
                    *opnd = src0;
                    return true;
                }
            }
        } else {
            Inst *src0inst = src0->getInst();
            ConstInst::ConstValue cv;
            if (ConstantFolder::isConstant(src0inst, cv)) {
                bool canelim = false;
                switch (src0inst->getType()) {
                case Type::Int8: case Type::Int16: case Type::Int32:
                    canelim = (positive ? (cv.i4 <= 0) : (cv.i4 < 0)); break;
                case Type::Int64:
                    canelim = (positive ? (cv.i8 <= 0) : (cv.i8 < 0)); break;
                case Type::IntPtr:
                    canelim = (positive ? true : (cv.i != 0)); break;
                case Type::UInt8: case Type::UInt16: case Type::UInt32:
                    canelim = (positive ? true : (cv.i4 != 0)); break;
                case Type::UInt64:
                    canelim = (positive ? true : (cv.i8 != 0)); break;
                case Type::UIntPtr:
                    canelim = (positive ? true : (cv.i != 0)); break;
                default:
                    break;
                }
                if (canelim) {
                    *opnd = src1;
                    return true;
                }
            }
        }
        break;
    case Cmp_Zero:
        positive = true;
    case Cmp_NonZero:
        {
            bool canelim = false;
            switch (src0->getInst()->getType()) {
            case Type::Int8: case Type::Int16: case Type::Int32:
            case Type::UInt8: case Type::UInt16: case Type::UInt32:
            case Type::Int64:
            case Type::UInt64:
            case Type::IntPtr: 
            case Type::UIntPtr:
            case Type::ManagedPtr: case Type::UnmanagedPtr:
            case Type::SystemObject: case Type::SystemClass: case Type::SystemString:
            case Type::Array: case Type::Object: case Type::BoxedValue:
            case Type::MethodPtr: case Type::VTablePtr:
            case Type::CompressedSystemObject: 
            case Type::CompressedSystemClass: 
            case Type::CompressedSystemString:
            case Type::CompressedArray: case Type::CompressedObject:
                canelim = positive ^ isTrueEdge; break;
            case Type::NullObject: case Type::CompressedNullObject:
            default:
                break;
            }
            if (canelim) {
                *opnd = src0;
                return true;
            }
        }
        break;
    default:
        assert(0);
        break;
    }
    return false;
}

// sets opnd and returns True if it tells us something about CheckZero of an Int type
bool allowsCheckZeroElimination(ComparisonModifier mod, Type::Tag comparisonType,
                                Opnd *src0, Opnd *src1, bool isTrueEdge,
                                Opnd **opnd)
{
    if (src0->getType()->isInteger()) {
        return allowsAnyZeroElimination(mod, comparisonType, src0, src1, isTrueEdge, opnd);
    } else
        return false;
}

// sets opnd and constOpnd and returns True if it tells us something about equality of
// a nontrivial opnd with a constant opnd constOpnd
bool InstValueNumberer::allowsConstantPropagation(ComparisonModifier mod, Type::Tag comparisonType,
                                                  Opnd *src0, Opnd *src1, bool isTrueEdge,
                                                  Opnd **opnd, Opnd **constOpnd)
{
    bool equals = isTrueEdge;
    switch (mod) {
    case Cmp_NE_Un:
        if (Type::isFloatingPoint(comparisonType)) {
            // can't count on not-NE being same as EQ
            return false;
        }
        equals = !equals;
    case Cmp_EQ:
        {
            bool src0isconst = src0->getInst()->getOperation().isConstant();
            bool src1isconst = src1->getInst()->getOperation().isConstant();
            if (!(src0isconst || src1isconst)) return false;
            if (src0isconst && src1isconst) {
            }
            if (equals) {
                if (src0isconst) {
                    *opnd = src1;
                    *constOpnd = src0;
                } else {
                    assert(src1isconst);
                    *opnd = src0;
                    *constOpnd = src1;
                }
                return true;
            }
        }
        break;
    case Cmp_NonZero:
        equals = !equals;
    case Cmp_Zero:
        {
            if (equals) {
                Inst *zeroInst = lookupInst(0, getKey(Operation(Op_LdConstant, 
                                                                comparisonType, 
                                                                Modifier()),
                                                      (U_32) 0,
                                                      (U_32) 0));
                if (zeroInst) {
                    *opnd = src0;
                    *constOpnd = zeroInst->getDst();
                    return true;
                } else {
                    // we don't have a zero to work with
                    return false;
                }
            }
        }
        break;
    case Cmp_GT:
    case Cmp_GT_Un:
    case Cmp_GTE:
    case Cmp_GTE_Un:
        break;
    default:
        assert(0);
        break;
    }
    return false;
}

// sets opnd and returns True if it tells us something about CheckNull of an Pointer type
bool allowsCheckNullElimination(ComparisonModifier mod, Type::Tag comparisonType,
                                Opnd *src0, Opnd *src1, bool isTrueEdge,
                                Opnd **opnd)
{
    Type *type0 = src0->getType();
    if (type0->isPtr() || type0->isReference()) {
        return allowsAnyZeroElimination(mod, comparisonType, src0, src1, isTrueEdge, opnd);
    } else
        return false;
}

// sets opnd and returns True if it tells us something about CheckBounds of an Int type
bool allowsCheckBoundsElimination(ComparisonModifier mod, Type::Tag comparisonType,
                                  Opnd *src0, Opnd *src1, bool isTrueEdge,
                                  Opnd **opnd, Opnd **opnd2)
{
    switch (mod) {
    case Cmp_EQ:
    case Cmp_NE_Un:
    case Cmp_GT:
    case Cmp_GT_Un:
    case Cmp_GTE:
    case Cmp_GTE_Un:
    case Cmp_Zero:
    case Cmp_NonZero:
        break;
    default:
        assert(0);
        break;
    }
    return false;
}

// check for an instanceof test, simplify astype and cast based on value
//   typically looks like ceq(instanceof(), ldci#0) or cne(instanceof(),
//   ldci#0)

// sets opnd and returns True if it tells us something about a CheckCast
bool allowsCheckCastElimination(ComparisonModifier mod, Type::Tag comparisonType,
                                Opnd *src0, Opnd *src1, bool isTrueEdge,
                                Opnd **opnd, Type **type)
{
    if(!src0->getType()->isInteger())
        return false;

    bool eq = true;
    switch (mod) {
    case Cmp_NE_Un:
        eq = false;
    case Cmp_EQ:
        if(!ConstantFolder::isConstantZero(src1)) {
            if(!ConstantFolder::isConstantZero(src0))
                return false;
            else
                src0 = src1;
        }
        break;
    case Cmp_NonZero:
        eq = false;
    case Cmp_Zero:
        break;
    case Cmp_GT:
    case Cmp_GT_Un:
    case Cmp_GTE:
    case Cmp_GTE_Un:
        return false;
    default:
        assert(0);
        break;
    }
    if(isTrueEdge == eq)
        return false;

    Inst* inst = src0->getInst();
    if(inst->getOpcode() != Op_TauInstanceOf)
        return false;
    TypeInst* tinst = inst->asTypeInst();
    assert(tinst != NULL);
    *opnd = tinst->getSrc(0);
    *type = tinst->getTypeInfo();
    if(Log::isEnabled()) {
        Log::out() << "CheckCast Elim: ";
        (*opnd)->print(Log::out());
        Log::out() << ::std::endl;
    }
    return true;
}

// sets opnd and returns True if it tells us something about a CheckCast
bool allowsCheckElemTypeElimination(ComparisonModifier mod, Type::Tag comparisonType,
                                    Opnd *src0, Opnd *src1, bool isTrueEdge,
                                    Opnd **opnd, Opnd **opnd2)
{
    switch (mod) {
    case Cmp_EQ:
    case Cmp_NE_Un:
    case Cmp_GT:
    case Cmp_GT_Un:
    case Cmp_GTE:
    case Cmp_GTE_Un:
    case Cmp_Zero:
    case Cmp_NonZero:
        break;
    default:
        assert(0);
        break;
    }
    return false;
}

ComparisonModifier negateComparison(ComparisonModifier mod, bool isfloat) 
{
    if (isfloat) {
        switch (mod) {
        case Cmp_EQ: return Cmp_NE_Un;
        case Cmp_NE_Un: return Cmp_EQ;
        case Cmp_GT: return Cmp_GTE_Un;
        case Cmp_GT_Un: return Cmp_GTE;
        case Cmp_GTE: return Cmp_GT_Un;
        case Cmp_GTE_Un: return Cmp_GT;
        case Cmp_Zero: return Cmp_NonZero;
        case Cmp_NonZero: return Cmp_Zero;
        default:
            break;
        }
    } else {
        switch (mod) {
        case Cmp_EQ: return Cmp_NE_Un;
        case Cmp_NE_Un: return Cmp_EQ;
        case Cmp_GT: return Cmp_GTE;
        case Cmp_GT_Un: return Cmp_GTE_Un;
        case Cmp_GTE: return Cmp_GT;
        case Cmp_GTE_Un: return Cmp_GT_Un;
        case Cmp_Zero: return Cmp_NonZero;
        case Cmp_NonZero: return Cmp_Zero;
        default:
            break;
        }
    }
    assert(0);
    return Cmp_EQ;
}

void InstValueNumberer::addInfoFromBranch(Node* targetNode, BranchInst *branchi, bool isTrueEdge)
{
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();   
    if (!optimizerFlags.elim_checks) return;

    if (Log::isEnabled()) {
        Log::out() << "addInfoFromBranch " << (isTrueEdge ? "taken " : "notTaken ");
        branchi->print(Log::out());
        Log::out() << ::std::endl;
    }

    U_32 numSrcs = branchi->getNumSrcOperands();
    addInfoFromBranchCompare(targetNode, 
                             branchi->getComparisonModifier(),
                             branchi->getType(),
                             isTrueEdge,
                             branchi->getNumSrcOperands(),
                             branchi->getSrc(0),
                             (numSrcs==2 ? branchi->getSrc(1) : 0));
}

void InstValueNumberer::recordHasTypeTau(Opnd *opnd,
                                         Type *type,
                                         Inst *tauHasTypeInst)
{
    // make checks available
    U_32 typeId = type->getId();
    U_32 opndId = opnd->getId();

    CSEHashKey key1 = getKey(Operation(Op_TauCheckCast, Type::SystemObject,
                                       Modifier(Exception_Sometimes)),
                             opndId, typeId);
    CSEHashKey key2 = getKey(Op_TauHasType,
                             opndId, typeId);
    if (!cseHashTable.lookup(key1)) {
        setHashToInst(tauHasTypeInst, key1);
    }
    if (!cseHashTable.lookup(key2)) {
        setHashToInst(tauHasTypeInst, key2);
    }

    if (type->isObject()) {
        // make superclass casts also available
        ObjectType *objType = (ObjectType *)type;
        ObjectType *superClass = objType->getSuperType();
        while (superClass) {
            U_32 superClassId = superClass->getId();
            CSEHashKey key1 = getKey(Operation(Op_TauCheckCast, Type::SystemObject,
                                               Modifier(Exception_Sometimes)),
                                     opndId, superClassId);
            CSEHashKey key2 = getKey(Op_TauHasType,
                                     opndId, superClassId);
            if (!cseHashTable.lookup(key1)) {
                setHashToInst(tauHasTypeInst, key1);
            }
            if (!cseHashTable.lookup(key2)) {
                setHashToInst(tauHasTypeInst, key2);
            }                
            superClass = superClass->getSuperType();
        };
    }
}

void InstValueNumberer::addInfoFromBranchCompare(Node* targetNode, 
                                                 ComparisonModifier mod,
                                                 Type::Tag comparisonType,
                                                 bool isTrueEdge,
                                                 U_32 numSrcOperands,
                                                 Opnd *src0,
                                                 Opnd *src1)
{
    // add the branch
    ComparisonModifier modhere 
        = (isTrueEdge 
           ? mod 
           : negateComparison(mod, Type::isFloatingPoint(comparisonType)));
    ComparisonModifier negModhere = 
        negateComparison(modhere,
                         Type::isFloatingPoint(comparisonType));
    Operation cmpOperation(Op_Cmp, comparisonType, modhere);
    Operation negCmpOperation(Op_Cmp, comparisonType, negModhere);
    Operation branchOperation(Op_Branch, comparisonType, modhere);
    Operation negBranchOperation(Op_Branch, comparisonType, negModhere);
    switch (numSrcOperands) {
    case 1:
        {
            if (Log::isEnabled()) {
                Log::out() << "adding true comparison: ";
                switch (modhere) {
                case Cmp_Zero: Log::out() << "Cmp_Zero "; break;
                case Cmp_NonZero: Log::out() << "Cmp_NonZero "; break;
                default: assert(0); break;
                }
                src0->print(Log::out());
                Log::out() << ::std::endl;
                Log::out() << "branchOperation.hashcode() == "
                           << (int) branchOperation.encodeForHashing()
                           << ::std::endl;
                Log::out() << "cmpOperation.hashcode() == "
                           << (int) cmpOperation.encodeForHashing()
                           << ::std::endl;
            }
            Inst *tauEdge = getBlockTauEdge(targetNode)->getInst();
            setHashToInst(tauEdge, getKey(branchOperation, src0->getId()));
            setHashToInst(tauEdge, getKey(cmpOperation, src0->getId()));
            
            if (Log::isEnabled()) {
                Log::out() << "adding false comparison: ";
                switch (negModhere) {
                case Cmp_Zero: Log::out() << "Cmp_Zero "; break;
                case Cmp_NonZero: Log::out() << "Cmp_NonZero "; break;
                default: assert(0); break;
                }
                src0->print(Log::out());
                Log::out() << ::std::endl;
                Log::out() << "negBranchOperation.hashcode() == "
                           << (int) negBranchOperation.encodeForHashing()
                           << ::std::endl;
                Log::out() << "negCmpOperation.hashcode() == "
                           << (int) negCmpOperation.encodeForHashing()
                           << ::std::endl;
            }

            setHashToInst(getTauUnsafe(), getKey(negBranchOperation, src0->getId()));
            setHashToInst(getTauUnsafe(), getKey(negCmpOperation, src0->getId()));
        }
        break;
    case 2:
        {
            Opnd *posSrc0 = isTrueEdge ? src0 : src1;
            Opnd *posSrc1 = isTrueEdge ? src1 : src0;
            Opnd *negSrc0 = isTrueEdge ? src1 : src0;
            Opnd *negSrc1 = isTrueEdge ? src0 : src1;

            if (Log::isEnabled()) {
                Log::out() << "adding true comparison: ";
                switch (modhere) {
                case Cmp_EQ: Log::out() << "Cmp_EQ "; break;
                case Cmp_NE_Un: Log::out() << "Cmp_NE_Un "; break;
                case Cmp_GT: Log::out() << "Cmp_GT "; break;
                case Cmp_GT_Un: Log::out() << "Cmp_GT_Un "; break;
                case Cmp_GTE: Log::out() << "Cmp_GTE "; break;
                case Cmp_GTE_Un: Log::out() << "Cmp_GTE_Un "; break;
                default: assert(0); break;
                }
                posSrc0->print(Log::out());
                Log::out() << ", ";
                posSrc1->print(Log::out());
                Log::out() << ::std::endl;
            }

            Inst *tauEdge = getBlockTauEdge(targetNode)->getInst();
            setHashToInst(tauEdge, 
                          getKey(branchOperation, posSrc0->getId(), posSrc1->getId()));
            setHashToInst(tauEdge,
                          getKey(cmpOperation, posSrc0->getId(), posSrc1->getId()));

            if (Log::isEnabled()) {
                Log::out() << "adding false comparison: ";
                switch (negModhere) {
                case Cmp_EQ: Log::out() << "Cmp_EQ "; break;
                case Cmp_NE_Un: Log::out() << "Cmp_NE_Un "; break;
                case Cmp_GT: Log::out() << "Cmp_GT "; break;
                case Cmp_GT_Un: Log::out() << "Cmp_GT_Un "; break;
                case Cmp_GTE: Log::out() << "Cmp_GTE "; break;
                case Cmp_GTE_Un: Log::out() << "Cmp_GTE_Un "; break;
                default: assert(0); break;
                }
                posSrc0->print(Log::out());
                Log::out() << ", ";
                posSrc1->print(Log::out());
                Log::out() << ::std::endl;
            }
            
            setHashToInst(getTauUnsafe(),
                          getKey(negBranchOperation, negSrc0->getId(), 
                                 negSrc1->getId()));
            setHashToInst(getTauUnsafe(),
                          getKey(negCmpOperation, negSrc0->getId(), 
                                 negSrc1->getId()));
        }
        break;
    default:
        assert(0);
        break;
    }

    Opnd *opnd = 0;
    Opnd *opnd2 = 0;
    Type *type = 0;
    Inst *tauEdge = 0;
    if (allowsCheckZeroElimination(mod, comparisonType, src0, src1, isTrueEdge, 
                                   &opnd)) {
        assert(opnd);
        if (Log::isEnabled()) {
            Log::out() << "can eliminate checkzero of ";
            opnd->print(Log::out());
            Log::out() << ::std::endl;
        }

        if (!tauEdge) tauEdge = getBlockTauEdge(targetNode)->getInst();
        setHashToInst(tauEdge,
                      getKey(Operation(Op_TauCheckZero, opnd->getType()->tag,
                                       Modifier(Exception_Sometimes)),
                             opnd->getId()));
    }
    if (allowsCheckNullElimination(mod, comparisonType, src0, src1, isTrueEdge, 
                                   &opnd)) {
        bool repeat_it;
        do {
            repeat_it = false;

            assert(opnd);
            if (Log::isEnabled()) {
                Log::out() << "can eliminate checknull of ";
                opnd->print(Log::out());
                Log::out() << ::std::endl;
            }
            if (!tauEdge) tauEdge = getBlockTauEdge(targetNode)->getInst();
            setHashToInst(tauEdge,
                          getKey(Op_TauCheckNull,
                                 opnd->getId()));
            setHashToInst(tauEdge,
                          getKey(Operation(Op_TauIsNonNull,
                                           opnd->getType()->tag,
                                           Modifier()),
                                 opnd->getId()));

            Inst *opndInst = opnd->getInst();
            Opcode opndInstOpcode = opndInst->getOpcode();
            if (opndInstOpcode == Op_TauStaticCast) {
                // if a static cast, source is also non-null
                opnd = opndInst->getSrc(0);
                repeat_it = true;
            }
        } while (repeat_it);
    }
    if (allowsCheckBoundsElimination(mod, comparisonType, src0, src1, isTrueEdge, 
                                     &opnd, &opnd2)) {
        assert(opnd);
        assert(opnd2);
        if (Log::isEnabled()) {
            Log::out() << "can eliminate checkbounds of ";
            opnd->print(Log::out());
            Log::out() << ", ";
            opnd2->print(Log::out());
            Log::out() << ::std::endl;
        }
        if (!tauEdge) tauEdge = getBlockTauEdge(targetNode)->getInst();
        setHashToInst(tauEdge,
                      getKey(Operation(Op_TauCheckBounds, opnd->getType()->tag,
                                       Modifier(Exception_Sometimes)),
                             opnd->getId(), opnd2->getId()));
    }
    if (allowsCheckCastElimination(mod, comparisonType, src0, src1, isTrueEdge,
                                   &opnd, &type)) {
        bool repeat_it;
        do {
            repeat_it = false;
            assert(opnd);
            assert(type);
            if (Log::isEnabled()) {
                Log::out() << "can eliminate checkcast of ";
                opnd->print(Log::out());
                Log::out() << ", ";
                type->print(Log::out());
                Log::out() << ::std::endl;
            }
            if (!tauEdge) tauEdge = getBlockTauEdge(targetNode)->getInst();
            opnd2 = irManager.getOpndManager().createSsaTmpOpnd(type);
            Inst* scast = irManager.getInstFactory().makeTauStaticCast(opnd2, opnd,
                                                                       tauEdge->getDst(), type);
            if(Log::isEnabled()) {
                Log::out() << "Inserting staticCast inst ";
                scast->print(Log::out());
                Log::out() << " after tauEdge ";
                tauEdge->print(Log::out());
                Log::out() << ::std::endl;
            }
            scast->insertAfter(tauEdge);

            if (!tauEdge) tauEdge = getBlockTauEdge(targetNode)->getInst();
            // make checks available
            recordHasTypeTau(opnd, type, tauEdge);

            Inst *opndInst = opnd->getInst();
            Opcode opndInstOpcode = opndInst->getOpcode();
            if (opndInstOpcode == Op_TauStaticCast) {
                // if a static cast, source is also in the type
                opnd = opndInst->getSrc(0);
                repeat_it = true;
            }
        } while (repeat_it);
    }
    if (allowsCheckElemTypeElimination(mod, comparisonType, src0, src1, 
                                       isTrueEdge,
                                       &opnd, &opnd2)) {
        bool repeat_it;
        do {
            repeat_it = false;
            assert(opnd);
            assert(opnd2);
            if (Log::isEnabled()) {
                Log::out() << "can eliminate checkelemtype of ";
                opnd->print(Log::out());
                Log::out() << ", ";
                opnd2->print(Log::out());
                Log::out() << ::std::endl;
            }
            if (!tauEdge) tauEdge = getBlockTauEdge(targetNode)->getInst();
            setHashToInst(tauEdge,
                          getKey(Operation(Op_TauCheckElemType, 
                                           opnd->getType()->tag, 
                                           (Modifier(Exception_Sometimes))),
                                 opnd->getId(), 
                                 opnd2->getId()));

            Inst *opndInst = opnd->getInst();
            Opcode opndInstOpcode = opndInst->getOpcode();
            if (opndInstOpcode == Op_TauStaticCast) {
                // if a static cast, source is also in the type
                opnd = opndInst->getSrc(0);
                repeat_it = true;
            }
        } while (repeat_it);
    }
    if (allowsConstantPropagation(mod, comparisonType, src0, src1, isTrueEdge,
                                  &opnd, &opnd2)) {
        bool repeat_it;
        do {
            repeat_it = false;

            // opnd2 is a constant expression
            if (constantTable)
                constantTable->insert(opnd, opnd2);
            
            // vtable comparison guarantees type safety
            if (Type::isVTablePtr(comparisonType)) {
                Inst *opnd2inst = opnd2->getInst();
                assert(opnd2inst->getOpcode() == Op_GetVTableAddr);
                Inst *opnd1inst = opnd->getInst();
                if (opnd1inst->getOpcode() == Op_TauLdVTableAddr) {
                    TypeInst *opnd2typeInst = opnd2inst->asTypeInst();
                    Opnd *base = opnd1inst->getSrc(0);
                    if (!tauEdge) tauEdge = getBlockTauEdge(targetNode)->getInst();
                    Type *typeInfo = opnd2typeInst->getTypeInfo();
                    setHashToInst(tauEdge,
                                  getKey(Op_TauHasExactType,
                                         base->getId(),
                                         opnd2typeInst->getType()));
                    recordHasTypeTau(base, typeInfo, tauEdge);
                }
            }

            Inst *opndInst = opnd->getInst();
            Opcode opndInstOpcode = opndInst->getOpcode();
            if (opndInstOpcode == Op_TauStaticCast) {
                // if a static cast, source is also the constant
                opnd = opndInst->getSrc(0);
                repeat_it = true;
            }
        } while (repeat_it);
    }        
}

void InstValueNumberer::addInfoFromPEI(Inst *pei, bool isExceptionEdge)
{
    switch (pei->getOpcode()) {
    case Op_Add: case Op_Mul: case Op_Sub: case Op_Conv: case Op_ConvZE: case Op_ConvUnmanaged:
    case Op_TauCheckDivOpnds:
        break;
    case Op_DirectCall: case Op_TauVirtualCall: case Op_IndirectCall:
    case Op_IndirectMemoryCall: case Op_JitHelperCall:   case Op_InitType:
        break;
    case Op_TauMonitorExit:
        break;
    case Op_TauCheckElemType:
        if (!isExceptionEdge) {
            // skip tau operand when hashing
            CSEHashKey key = getKey(pei->getOperation(), 
                                    pei->getSrc(0)->getId(),
                                    pei->getSrc(1)->getId());
            setHashToInst(pei, key);
        }
        break;
    case Op_TauCheckLowerBound:
    case Op_TauCheckUpperBound:
    case Op_TauCheckBounds:
    case Op_TauCheckZero:
    case Op_TauCheckFinite:
        // If an exception did not occur, the result of the pei is available.
        if(!isExceptionEdge)
            setHashToInst(pei, getKey(pei));
        break;
    case Op_TauCheckNull:
    case Op_TauCheckCast:
    case Op_TauCast:
        // If an exception did not occur, the result of the pei is available.
        if(!isExceptionEdge) {
            // these may provide information about the source of a static cast
            bool repeat_it;
            Opnd *orgSrc0 = pei->getSrc(0);
            Opnd *opnd = orgSrc0;
            do {
                repeat_it = false;
                
                pei->setSrc(0, opnd);
                CSEHashKey key = getKey(pei);
                setHashToInst(pei, key);

                Inst *opndInst = opnd->getInst();
                Opcode opndInstOpcode = opndInst->getOpcode();
                if (opndInstOpcode == Op_TauStaticCast) {
                    // if a static cast, source is also non-null
                    opnd = opndInst->getSrc(0);
                    repeat_it = true;
                }
            } while (repeat_it);
            pei->setSrc(0, orgSrc0);
        }
        break;
    default:
        break;
    }
}

class ValueNumberingWalker {
    IRManager &irManager;
    ControlFlowGraph &fg;
    InstValueNumberer ivn;
    DominatorNode *domNode;
    Node *block;
    int depth;
    bool isScoped;
    bool useBranches;
    int dispatchDepth; // 0 if we are not dominated by a dispatch node,
                       // 1 + depth since dispatch node otherwise
    bool skipDispatches;
    bool cacheOpnds;
    SparseScopedMap<Opnd *, Opnd *> *opndMap;
public:
    int numInstOptimized;
    void startNode(DominatorNode *domNode0) { 
        domNode = domNode0;
        block = domNode->getNode();
        if (dispatchDepth <= 0) {
            if (skipDispatches && block->isDispatchNode()) {
                if (Log::isEnabled()) {
                    Log::out() << "Skipping dispatch node ";
                    FlowGraph::print(Log::out(), block);
                    Log::out() << " and dominated nodes";
                    Log::out() << ::std::endl;
                }    
                dispatchDepth = 1;
            } else {
                if (Log::isEnabled()) {
                    Log::out() << "Begin hashvaluenumbering of block";
                    FlowGraph::print(Log::out(), block);
                    Log::out() << ::std::endl;
                }    
                if (useBranches)
                    ivn.addBranchConditions(domNode);
            }
        }
    };
    void applyToInst(Inst *inst) { 
        if (Log::isEnabled()) {
            Log::out() << "VN examining instruction ";
            inst->print(Log::out());
            Log::out() << ::std::endl;
        }
        if (cacheOpnds) {
            // map operands to point to optimized ones
            // this must happen even for exception code, to
            // make sure that GCMed def is made visible to
            // exception code
            U_32 numSrcs = inst->getNumSrcOperands();
            for (U_32 i=0; i < numSrcs; ++i) {
                Opnd *opnd = inst->getSrc(i);
                SsaOpnd *ssaOpnd = opnd->asSsaOpnd();
                if (ssaOpnd) {
                    Opnd *mapped = opndMap->lookup(ssaOpnd);
                    if (mapped) {
                        if (Log::isEnabled()) {
                            Log::out() << "VN remapped opnd " << (int) i
                                       << " of inst " << (int) inst->getId() 
                                       << " from ";
                            ssaOpnd->print(Log::out());
                            Log::out() << " to ";
                            mapped->print(Log::out());
                            Log::out() << ::std::endl;
                        }    
                        inst->setSrc(i, mapped);
                    }
                }
            }
        }
        if (dispatchDepth > 0) return;

        Opcode instOpcode = inst->getOpcode();
        if (Log::isEnabled()) {
            Log::out() << "VN point 1" << ::std::endl;
        }
        if (!isScoped) {
            if ((inst->getDst() == 0) || (instOpcode == Op_LdVar)) {
                return;
            }
            if (!inst->getOperation().isMovable())
                return;
        } else {
        }
        if (Log::isEnabled()) {
            Log::out() << "VN point 2" << ::std::endl;
        }
        Inst* optimizedInst = ivn.optimizeInst(inst);
        Opcode optimizedOpcode = optimizedInst->getOpcode();
        if (Log::isEnabled()) {
            Log::out() << "VN point 3, optimizedInst = ";
            if (optimizedInst) {
                optimizedInst->print(Log::out());
            } else {
                Log::out() << "NULL";
            }
            Log::out() << ::std::endl;
        }
        if (optimizedInst != inst) {
            // CSE was found!
            numInstOptimized++;
            if (Log::isEnabled()) {
                Log::out() << "VN optimized instruction ";
                inst->print(Log::out());
                Log::out() << " to ";
                if (optimizedInst) {
                    optimizedInst->print(Log::out());
                } else {
                    Log::out() << "NULL";
                }
                Log::out() << ::std::endl;
            }    
            
            // do something special with branches
            BranchInst *branchi = inst->asBranchInst();
            if (branchi) {
                if (optimizedOpcode == Op_TauUnsafe) {
                    FlowGraph::foldBranch(fg, branchi, false); // not taken
                } else {
                    FlowGraph::foldBranch(fg, branchi, true); // taken
                }
                return;
            }

            Opnd* dstOpnd = inst->getDst();
            // some operations, e.g., InitType, don't produce a
            // value in a destination opnd
            if (dstOpnd->isNull() == false) {
                inst->setDst(OpndManager::getNullOpnd());
                Inst* copy = NULL;
                Opnd* srcOpnd = NULL;
                if (optimizedOpcode == Op_TauUnsafe && instOpcode == Op_Cmp){ 
                    // optimizedInst is tauUnsafe so srcOpnd for copying must be 'false'
                    copy = irManager.getInstFactory().makeLdConst(dstOpnd,(I_32)0);
                } else  if (optimizedOpcode == Op_TauEdge && dstOpnd->getType()->isNumeric()) {
                    copy = irManager.getInstFactory().makeLdConst(dstOpnd,(I_32)1);
                } else {
                    srcOpnd = optimizedInst->getDst();
                    //
                    // Note that sometimes dstOpnd could be a null operand because of
                    // instructions that do not define a new value (e.g., checkelemtype)
                    // but are simplified to instructions that do (e.g., newobj)
                    //
                    copy = irManager.getInstFactory().makeCopy(dstOpnd,srcOpnd);
                }
                
                if(Log::isEnabled()) {
                    Log::out() << "Inserting copy inst ";
                    copy->print(Log::out());
                    Log::out() << " before inst ";
                    inst->print(Log::out());
                    Log::out() << ::std::endl;
                }
                copy->insertBefore(inst);

                // note that it is important that copy is inserted BEFORE inst,
                // so that if we are caching opnds we don't remap it in the copy.
                if (cacheOpnds && srcOpnd) {
                    opndMap->insert(dstOpnd, srcOpnd);
                }
            }
            // remove the instruction
            if (inst->getOperation().canThrow()) {
                // instructions that can throw exceptions are special because 
                // they have corresponding exceptions edges in the control flow 
                // graph.  You must remove exception edges when you remove
                // a potentially exception throwing check instruction.
                if(Log::isEnabled()) {
                    Log::out() << "Removing redundant check inst ";
                    inst->print(Log::out());
                    Log::out() << ::std::endl;
                }
                FlowGraph::eliminateCheck(fg, block,inst,false);
            } else {
                if(Log::isEnabled()) {
                    Log::out() << "Removing redundant inst ";
                    inst->print(Log::out());
                    Log::out() << ::std::endl;
                }
                inst->unlink();
            }
        }
    };
    void finishNode(DominatorNode *domNode) { 
        if (dispatchDepth > 0) return;
        if (Log::isEnabled()) {
            Log::out() << "Done hashvaluenumbering of block";
            domNode->print(Log::out());
            Log::out() << ::std::endl;
        }
    };
    void enterScope() { 
        if ((isScoped && (dispatchDepth == 0)) || (depth == 0)) {
            ivn.enterScope();
            if (opndMap) opndMap->enter_scope();
            if (Log::isEnabled()) {
                Log::out() << "Entering scope" << depth << ::std::endl;
            }
        }
        depth += 1;
        if (dispatchDepth > 0) { dispatchDepth += 1; }
    };
    void exitScope() { 
        if (dispatchDepth > 0) { 
            dispatchDepth -= 1; 
        }
        depth -= 1;
        if ((isScoped && (dispatchDepth == 0)) || (depth == 0)) {
            if (Log::isEnabled()) {
                Log::out() << "Exiting scope" << depth << ::std::endl;
            }
            ivn.exitScope();
            if (opndMap) opndMap->exit_scope();
        }
        if (dispatchDepth == 1) dispatchDepth = 0;
    };
    ValueNumberingWalker(MemoryManager &mm0, IRManager &irm, 
                         ControlFlowGraph &fg0,
                         DominatorTree &domtree,
                         MemoryOpt *mopt,
                         bool isScoped0,
                         bool useBranches0,
                         bool skipDispatches0,
                         bool cacheOpnds0)
        : irManager(irm), fg(fg0),
          ivn(mm0, irm, domtree, mopt, irManager.getOptimizerFlags().cse_final && isScoped0,
              fg0, isScoped0),
          domNode(0), depth(0), isScoped(isScoped0),
          useBranches(useBranches0),
          dispatchDepth(0), skipDispatches(skipDispatches0), 
          cacheOpnds(cacheOpnds0), 
          opndMap(0), numInstOptimized(0)
    {
        if (cacheOpnds) {
            const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
            opndMap = 
                new (mm0) SparseScopedMap<Opnd *, Opnd *>(fg.getNodes().size() *
                                                          (optimizerFlags.
                                                           hash_node_tmp_factor),
                                                          mm0);
        }
    };
    ~ValueNumberingWalker() {};
};

void 
HashValueNumberer::doGlobalValueNumbering(MemoryOpt *mopt) {
    MemoryManager localMM("HashValueNumberer::doGlobalValueNumbering");

    if (Log::isEnabled()) {
        Log::out() << "Starting unscoped value numbering pass" << ::std::endl;
    }
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    ValueNumberingWalker walker(localMM, irManager, fg, dominators, mopt,
                                false, false, // un-scoped, no branches
                                !optimizerFlags.gvn_exceptions,
                                optimizerFlags.gvn_aggressive);


    if (optimizerFlags.gvn_aggressive) {
        // although we are un-scoped, we benefit from DomTree walking
        // since subexpressions will be already numbered
        
        // adapt the ScopedDomNodeInstWalker to a DomWalker
        typedef ScopedDomNodeInst2DomWalker<true, ValueNumberingWalker>
            ValueNumberingDomWalker;
        ValueNumberingDomWalker domWalker(walker);
        
        // do the walk, pre-order
        DomTreeWalk<true, ValueNumberingDomWalker>(dominators, domWalker, 
                                                   localMM);
    } else {
        // here we are un-scoped, so we use the ValueNumberingWalker as a simple 
        // forwards InstWalker and walk over nodes in arbitrary order.
        
        // adapt the NodeInstWalker to a NodeWalker
        typedef Inst2NodeWalker<true, ValueNumberingWalker>
            ValueNumberingNodeWalker;
        ValueNumberingNodeWalker nodeWalker(walker);
        
        // do the walk over nodes in arbitrary order
        NodeWalk<ValueNumberingNodeWalker>(fg, nodeWalker);
    }

    if (Log::isEnabled()) {
        Log::out() << "Finished unscoped value numbering pass" << ::std::endl;
    }
}


void 
HashValueNumberer::doValueNumbering(MemoryOpt *mopt) {
    MemoryManager localMM("HashValueNumberer::doValueNumbering");

    if (Log::isEnabled()) {
        Log::out() << "Starting scoped value numbering pass" << ::std::endl;
    }

    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    ValueNumberingWalker walker(localMM, irManager, fg, dominators, mopt,
                                true, true,  // scoped, use branches
                                !optimizerFlags.hvn_exceptions,
                                false);


    // adapt the ScopedDomNodeInstWalker to a DomWalker
    typedef ScopedDomNodeInst2DomWalker<true, ValueNumberingWalker>
        ValueNumberingDomWalker;
    ValueNumberingDomWalker domWalker(walker);
    
    // do the walk, pre-order
    DomTreeWalk<true, ValueNumberingDomWalker>(dominators, domWalker, 
                                               localMM);

    if (Log::isEnabled()) {
        Log::out() << "Finished scoped value numbering pass" << ::std::endl;
    }
}

} //namespace Jitrino 
