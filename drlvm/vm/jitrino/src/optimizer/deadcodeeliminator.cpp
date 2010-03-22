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

#include "deadcodeeliminator.h"
#include "irmanager.h"
#include "Inst.h"
#include "Stl.h"
#include "BitSet.h"
#include "MemoryManager.h"
#include "Log.h"
#include "constantfolder.h"
#include "optimizer.h"
#include "XTimer.h"
#include "PMFAction.h"
#include "Dominator.h"
#include "FlowGraph.h"

namespace Jitrino {

// Timer.h includes windows.h, which has a broken #def of min:
#undef min

DEFINE_SESSION_ACTION(DeadCodeEliminationPass, dce, "Dead Code Elimination")

void
DeadCodeEliminationPass::_run(IRManager& irm){
    DeadCodeEliminator dce(irm);
    {
        //create loops mapping to be able to fix infinite loops after the optimization
        MemoryManager tmpMM("HashValueNumberingPass::_run");
        InfiniteLoopsInfo loopsMapping(tmpMM);
        DeadCodeEliminator::createLoop2DispatchMapping(irm.getFlowGraph(), loopsMapping);

        //perform optimization
        dce.eliminateDeadCode(false);

        //fix infinite loops if found
        DeadCodeEliminator::fixInfiniteLoops(irm, loopsMapping);
    }
}

DEFINE_SESSION_ACTION(UnreachableCodeEliminationPass, uce, "Unreachable Code Elimination");

void 
UnreachableCodeEliminationPass::_run(IRManager& irm) {
    DeadCodeEliminator dce(irm);
    {
        dce.eliminateUnreachableCode();
    }
    bool fixup_ssa = irm.getOptimizerFlags().fixup_ssa;
    if (irm.getInSsa() && fixup_ssa) {
        OptPass::fixupSsa(irm);
    }
}

DEFINE_SESSION_ACTION(ExtraPseudoThrowRemovalPass, rept, "Removal of Extra PseudoThrow Instructions");

void 
ExtraPseudoThrowRemovalPass::_run(IRManager& irm) {
    DeadCodeEliminator dce(irm);
    dce.removeExtraPseudoThrow();
}


//Empty Node Removal
class PurgeEmptyNodesPass: public SessionAction {
public:
    void run();    
};
ActionFactory<PurgeEmptyNodesPass> _purge("purge");

void
PurgeEmptyNodesPass::run() {
    IRManager& irm = *getCompilationContext()->getHIRManager();
    irm.getFlowGraph().purgeEmptyNodes(false);
}

//
// Returns true if an instruction is a non-essential instruction
// Essential instructions are exception throwing instructions, calls,
// returns, stores, and branches.  Once we have control dependence 
// information branches are non-essential.
//
static bool
isNonEssential(Inst* inst) {
    Operation operation = inst->getOperation();
    if (operation.isNonEssential()) {
        return true;
    } else {
        Opcode opCode = inst->getOpcode();
        if (opCode == Op_StVar) {
            // StVar is non-essential if the variable to which it stores is in SSA form
            return inst->getDst()->isSsaVarOpnd();
        } else if (opCode == Op_NewObj) {
            Type* dstType = inst->getDst()->getType();
            assert(!dstType->isUnresolvedObject());
            // Objects that have finalizers must not be swept
            return !dstType->asNamedType()->isFinalizable();
        }
        return false;
    }
}

static U_8
getBitWidth(Type::Tag tag)
{
    switch (tag) {
    case Type::Void: return 0;
    case Type::Boolean: return 1;
    case Type::Char: return 16;
    case Type::Int8: return 8;
    case Type::Int16: return 16;
    case Type::Int32: return 32;
    case Type::Int64: return 64;
    case Type::UInt8: return 8;
    case Type::UInt16: return 16;
    case Type::UInt32: return 32;
    case Type::UInt64: return 64;

    default: return 0xff; // uses all;
    }        
}

// returns the number of bits of the given opnd_num which is used,
// given that dst_bits of the dst are used
static U_8
usesBitsOfOpnd(U_8 dst_bits, Inst* inst, U_32 opnd_num) {
    U_8 opwidth = getBitWidth(inst->getType());
    Opnd *opnd = inst->getSrc(opnd_num);
    U_8 opndwidth = getBitWidth(opnd->getType()->tag);
    U_8 res = opndwidth;
    switch (inst->getOpcode()) {
    case Op_Add:   case Op_Mul:    case Op_Sub:
        res = ::std::min(dst_bits, opwidth); break;
    case Op_TauDiv:   case Op_TauRem:    
        res = dst_bits; break;
    case Op_Neg:   
        res = ::std::min(dst_bits, opwidth); break;
    case Op_MulHi:
        res = opwidth; break;
    case Op_Min:   case Op_Max:   case Op_Abs:
    case Op_And:   case Op_Or:    case Op_Xor:    case Op_Not:
        res = ::std::min(dst_bits, opwidth); break;
    case Op_Select:
        if (opnd_num == 0) res = opndwidth;
        else res = dst_bits;
        break;
    case Op_Conv:
        res = ::std::min(dst_bits, opwidth); break;
    case Op_Shladd:  
        {
            switch (opnd_num) {
            case 0:
                {
                    Opnd *shiftAmount = inst->getSrc(1);
                    I_32 shiftby;
                    bool isconst = 
                        ConstantFolder::isConstant(shiftAmount->getInst(), 
                                                   shiftby);
                    if( !(isconst && (shiftby > 0)) ) assert(0);
                    res = ::std::min(dst_bits, opwidth)-(U_8)shiftby;
                    break;
                }
            case 1:
                {
                    res = opndwidth;
                    break;
                }
            case 2:
                {
                    res = ::std::min(dst_bits, opwidth);
                    break;
                }
            default:
                assert(0);
            }
            break;
        }        
    case Op_Shl:
        {
            switch (opnd_num) {
            case 0:
                {
                    Opnd *shiftAmount = inst->getSrc(1);
                    I_32 shiftby;
                    bool isconst 
                        = ConstantFolder::isConstant(shiftAmount->getInst(), 
                                                     shiftby);
                    if (isconst && shiftby > 0) {
                        res = ::std::min(dst_bits, opwidth)-(U_8)shiftby;
                        break;
                    }
                }
            case 1:
                {
                    res = opndwidth;
                }
                break;
            default:
                assert(0);
            };
            break;
        }
    case Op_Shr:   
        res = opndwidth; break;
    case Op_Cmp:    case Op_Cmp3:
    case Op_Copy:
        res = ::std::min(dst_bits, opwidth); break;
    case Op_LdVar:
        res = dst_bits; break;
    case Op_Phi:
        res = dst_bits; break;
    case Op_StVar: 
        if (inst->getDst()->isSsaVarOpnd()) {
            res = dst_bits;
        } else {
            res = opndwidth; // can't track value accurately, be conservative
        }
        break;
    case Op_TauStElem:
    case Op_TauStRef: 
        {
            if (opnd_num == 0) { // value to be stored, trim to opwidth
                res = opwidth;
            }
        }
        break;
    case Op_TauStInd:
    case Op_TauStField: 
        {
            if (opnd_num == 0) { // value to be stored, trim to opwidth
                res = opwidth;
            }
        }
        break;
    case Op_TauStStatic:
        {
            if (opnd_num == 0) { // value to be stored, trim to opwidth
                res = opwidth;
            }
        }
        break;
    default:
        {
            res = opndwidth;
        }
    }
    U_8 res1 = ::std::min(res, opndwidth);
    return res1;
}

DeadCodeEliminator::DeadCodeEliminator(IRManager& irm) 
: irManager(irm), flowGraph(irm.getFlowGraph()), returnOpnd(irm.getReturnOpnd()) 
{
    preserveCriticalEdges = irManager.getOptimizerFlags().preserve_critical_edges;
}

Opnd*
DeadCodeEliminator::findDefiningTemp(Opnd* var) {
    SsaVarOpnd* ssaVarOpnd = var->asSsaVarOpnd();
    if(ssaVarOpnd == NULL) {
        Log::out() << "Nothing found: Not SSA" << ::std::endl;
        return NULL; // not an SsaVarOpnd
    }
    Inst* inst = ssaVarOpnd->getInst();
    assert(inst->getNode());
    if(inst->getOpcode() == Op_StVar) {
        // propagate src of copy recursively
        copyPropagate(inst);
        if(Log::isEnabled()) {
            Log::out() << "Found: ";
            Inst* def = inst->getSrc(0)->getInst();
            if(def != NULL) {
                def->print(Log::out());
            }
            else { 
                Log::out() << "Dangling operand "; inst->getSrc(0)->print(Log::out()); 
            }
            Log::out() << ::std::endl;
        }
        return inst->getSrc(0);
    } else if(inst->getOpcode() == Op_Phi) {

        Opnd* tmp = NULL;
        U_32 n = inst->getNumSrcOperands();
        for(U_32 j=0; j < n; ++j) {
            Opnd* src = inst->getSrc(j);
            Inst* srcInst = src->getInst();
            assert(srcInst->getNode());
            if(srcInst == NULL || srcInst->getOpcode() != Op_StVar) {
                tmp = NULL;
                break;
            }
            assert(srcInst->getOpcode() == Op_StVar);
            if(tmp == NULL) {
                assert(j == 0);
                tmp = srcInst->getSrc(0);
            } else if(tmp != srcInst->getSrc(0)) {
                tmp = NULL;
                break;
            }
        }

        return tmp;
    }
    Log::out() << "Nothing found: Not stVar or phi instruction" << ::std::endl;
    return NULL;
}


Opnd*
DeadCodeEliminator::copyPropagate(Opnd* opnd) {
    SsaOpnd* src = opnd->asSsaOpnd();
    if (src == NULL)
        return opnd;   // src was not a SsaOpnd so it cannot be propagated
    Inst* srcInst = src->getInst();
    assert(srcInst->getNode());
    if (srcInst == NULL)
        return opnd;
    if (srcInst->getOpcode() == Op_Copy) {
        // propagate src of copy recursively
        copyPropagate(srcInst);
        return srcInst->getSrc(0);
    } else if (srcInst->getOpcode() == Op_LdVar) {
        // propagate src of LdVar if src SsaVarOpnd comes from a StVar
        Opnd* tmp = findDefiningTemp(srcInst->getSrc(0));
        if(tmp != NULL) {
            return tmp;
        } else {
            return opnd;
        }
    } else if (srcInst->getOpcode() == Op_Phi) {
        // propagate src of degenerate phi (phi with only one arg).
        if(srcInst->getNumSrcOperands() != 1)
            return opnd;
        // propagate src of copy recursively
        copyPropagate(srcInst);
        // reset inst's source to propagates src
        return srcInst->getSrc(0);
    }
    return opnd;
}

void
DeadCodeEliminator::copyPropagate(Inst* inst) {
    U_32 numSrcs = inst->getNumSrcOperands();
    for (U_32 i=0; i<numSrcs; i++) {
        Opnd* opnd = inst->getSrc(i);
        Opnd* propagated = copyPropagate(opnd);
        if (opnd != propagated) {
            if (Log::isEnabled()) {
                Log::out() << "    Operand "; opnd->print(Log::out());
                Log::out() << " replaced by "; propagated->print(Log::out());
                Log::out() << std::endl;
            }
            inst->setSrc(i, propagated);
        }
    }
}

//
// marks an instruction as live and add its sources to the work set
//
static void
markLiveInst1(Inst* inst,
              InstDeque& workSet,
              BitSet& usefulInstSet,
              BitSet& usefulVarSet,
              U_32 minInstId,
              U_32 maxInstId) {
    //
    // add instruction's sources to the work list
    //
    assert(inst);
    assert(inst->getNode());

    U_8 dstWidth = 255;

    if (Log::isEnabled()) {
        Log::out() << "Found dstwidth of " << (int) dstWidth
                   << " for inst ";
        inst->print(Log::out());
        Log::out() << ::std::endl;
    }

    for (U_32 i=0; i<inst->getNumSrcOperands(); i++) {
        Opnd* src = inst->getSrc(i);
        // only follow ssa use-def links
        SsaOpnd* srcSsaOpnd = src->asSsaOpnd();
        if (srcSsaOpnd != NULL) {
            // use of an ssa operand
            Inst* def = srcSsaOpnd->getInst();
            if(def == NULL)
                continue;
            assert(def != NULL);

            assert(def->getNode());
            U_8 opndWidth = 255;
            U_32 defId = def->getId();

            if (usefulInstSet.setBit(defId-minInstId, true)) {
                // was already marked as live, check old width.
                U_8 oldOpndWidth = 255;

                if (Log::isEnabled()) {
                    Log::out() << "Found dstwidth of " << (int) oldOpndWidth
                               << " for inst ";
                    def->print(Log::out());
                    Log::out() << ::std::endl;
                }

                if (opndWidth > oldOpndWidth) {
                    if (Log::isEnabled()) {
                        Log::out() << "Setting dstwidth to " << (int) opndWidth
                                   << " for inst ";
                        def->print(Log::out());
                        Log::out() << ::std::endl;
                    }
                    workSet.pushFront(def);                    
                }
            } else {
                if (Log::isEnabled()) {
                    Log::out() << "Setting dstwidth to " << (int) opndWidth
                               << " for inst ";
                    def->print(Log::out());
                    Log::out() << ::std::endl;
                }
                workSet.pushFront(def);
            }
            // mark ssavaropnds as not dead
            SsaVarOpnd* ssaVarOpnd = src->asSsaVarOpnd();
            if (ssaVarOpnd != NULL) {
                // mark var as useful
                usefulVarSet.setBit(ssaVarOpnd->getVar()->getId(),true);
            }
        } else {
            VarOpnd* srcVarOpnd = src->asVarOpnd();
            if (srcVarOpnd != NULL) {
                // use of a var opnd (ldvar or ldvara)
                // mark var as useful
                usefulVarSet.setBit(srcVarOpnd->getId(),true);
            }
        }
    }
}

//
// Deletes instructions that are not marked as useful
//
void
DeadCodeEliminator::sweepInst1(Node* node, 
                               Inst* inst,
                               BitSet& usefulInstSet,
                               BitSet& usefulVarSet,
                               U_32 minInstId,
                               U_32 maxInstId,
                               bool canRemoveStvars) {
    assert(inst);
    assert(inst->getNode());
    U_32 instId = inst->getId();
    if (inst->isMethodMarker()) {
        // don't remove a method marker, but remove opcode if it is dead
        if (inst->getNumSrcOperands() > 0) {
            Opnd *opnd = inst->getSrc(0);
            if (opnd && !opnd->isNull()) {
                Inst *srcInst = opnd->getInst();
                assert(srcInst);
                assert(srcInst->getNode());
                U_32 srcId = srcInst->getId();
                // instructions are only added during analysis for
                // live instructions, so if it's not in the map, 
                // assume it is live
                if ((minInstId <= srcId) &&
                    (srcId < maxInstId) &&
                    (usefulInstSet.getBit(srcId-minInstId) == false)) {
                    MethodMarkerInst *mmi = inst->asMethodMarkerInst();
                    // Method marker could contain retOpnd
                    if (!mmi->getMethodDesc()->isStatic()) {
                    mmi->removeOpnd();
                }
            }
        }            
        }            
    } else if ((minInstId <= instId) && (instId < maxInstId) &&
               usefulInstSet.getBit(instId-minInstId) == false) {
        // inst is a useless instruction
        if (Log::isEnabled()) {
            Log::out() << "Useless inst found, removing: ";
            inst->print(Log::out());
            Log::out() << ::std::endl;
            Log::out() << " instId=" << (int)instId;
            Log::out() << ", minInstId=" << (int)minInstId;
            Log::out() << ", maxInstId=" << (int)maxInstId;
            Log::out() << ::std::endl;
        }
        inst->unlink();
        if (inst->isBranch()) {
            assert(0);
        } else if (inst->isSwitch()) {
            assert(0);
        } else if (inst->getOperation().canThrow()) {
            flowGraph.removeEdge(node->getExceptionEdge());
        }
        return;
    }
    //
    // delete stores to useless variables
    //
    if (canRemoveStvars && inst->getOpcode() == Op_StVar) {
        VarOpnd* dstVarOpnd = inst->getDst()->asVarOpnd();
        if (dstVarOpnd == NULL)
            return;
        if (usefulVarSet.getBit(dstVarOpnd->getId()) == false) {
            //
            // store to a useless variable
            //
            if (Log::isEnabled()) {
                Log::out() << "Removing store to useless var: ";
                inst->print(Log::out());
                Log::out() << ::std::endl;
            }
            inst->unlink();
            return;
        }       
    }
}

//
// marks an instruction as live and add its sources to the work set
//
static void
markLiveInst(Inst* inst,
             InstDeque& workSet,
             BitSet& usefulInstSet,
             BitSet& usefulVarSet,
             U_8 *usedInstWidth,
             U_32 minInstId,
             U_32 maxInstId) {
    //
    // add instruction's sources to the work list
    //
    assert(inst);
    assert(inst->getNode());
    U_32 instId = inst->getId();
    assert(usedInstWidth);
    assert((instId >= minInstId) && (instId < maxInstId));
    U_8 dstWidth = usedInstWidth[instId-minInstId];

    if (Log::isEnabled()) {
        Log::out() << "Found dstwidth of " << (int) dstWidth
                   << " for inst ";
        inst->print(Log::out());
        Log::out() << ::std::endl;
    }

    for (U_32 i=0; i<inst->getNumSrcOperands(); i++) {
        Opnd* src = inst->getSrc(i);
        // only follow ssa use-def links
        SsaOpnd* srcSsaOpnd = src->asSsaOpnd();
        if (srcSsaOpnd != NULL) {
            // use of an ssa operand
            Inst* def = srcSsaOpnd->getInst();
            if(def == NULL)
                continue;
            assert(def != NULL);

            U_8 opndWidth = usesBitsOfOpnd(dstWidth, inst, i);
            assert(def);
            assert(def->getNode());
            U_32 defId = def->getId();

            if (!((minInstId <= defId) && (defId < maxInstId))) {
                // this instruction is out of the region, skip it.
                
                if (Log::isEnabled()) {
                    Log::out() << "Skipping outside-region inst ";
                    def->print(Log::out());
                    Log::out() << ::std::endl;
                }
            } else if (usefulInstSet.setBit(defId-minInstId, true)) {
                // was already marked as live, check old width.
                U_8 oldOpndWidth = usedInstWidth[defId-minInstId];

                if (Log::isEnabled()) {
                    Log::out() << "Found dstwidth of " << (int) oldOpndWidth
                               << " for inst ";
                    def->print(Log::out());
                    Log::out() << ::std::endl;
                }

                if (opndWidth > oldOpndWidth) {
                    if (Log::isEnabled()) {
                        Log::out() << "Setting dstwidth to " << (int) opndWidth
                                   << " for inst ";
                        def->print(Log::out());
                        Log::out() << ::std::endl;
                    }
                    usedInstWidth[defId-minInstId] = opndWidth;
                    workSet.pushFront(def);                    
                }
            } else {
                if (Log::isEnabled()) {
                    Log::out() << "Setting dstwidth to " << (int) opndWidth
                               << " for inst ";
                    def->print(Log::out());
                    Log::out() << ::std::endl;
                }
                usedInstWidth[defId-minInstId] = opndWidth;
                workSet.pushFront(def);
            }
            // mark ssavaropnds as not dead
            SsaVarOpnd* ssaVarOpnd = src->asSsaVarOpnd();
            if (ssaVarOpnd != NULL) {
                // mark var as useful
                usefulVarSet.setBit(ssaVarOpnd->getVar()->getId(),true);
            }
        } else {
            VarOpnd* srcVarOpnd = src->asVarOpnd();
            if (srcVarOpnd != NULL) {
                // use of a var opnd (ldvar or ldvara)
                // mark var as useful
                usefulVarSet.setBit(srcVarOpnd->getId(),true);
            }
        }
    }
}

//
// Deletes instructions that are not marked as useful
//
void
DeadCodeEliminator::sweepInst(Node* node, 
                              Inst* inst,
                              BitSet& usefulInstSet,
                              BitSet& usefulVarSet,
                              U_8 *usedInstWidth,
                              U_32 minInstId,
                              U_32 maxInstId,
                              bool canRemoveStvars) {
    assert(usedInstWidth);
    assert(inst);
    assert(inst->getNode());
    U_32 instId = inst->getId();
    if (inst->isMethodMarker()) {
        // don't remove a method marker, but remove opcode if it is dead
        if (inst->getNumSrcOperands() > 0) {
            Opnd *opnd = inst->getSrc(0);
            if (opnd && !opnd->isNull()) {
                Inst *srcInst = opnd->getInst();
                assert(srcInst);
                assert(srcInst->getNode());
                U_32 srcId = srcInst->getId();
                // instructions are only added during analysis for
                // live instructions, so if it's not in the map, 
                // assume it is live
                if ((minInstId <= srcId) && (srcId < maxInstId) &&
                    (usefulInstSet.getBit(srcId-minInstId) == false)) {
                    MethodMarkerInst *mmi = inst->asMethodMarkerInst();
                    // Method marker could contain retOpnd which should be preserved
                    if (!mmi->getMethodDesc()->isStatic()) {
                    mmi->removeOpnd();
                }
            }
        }            
        }            
    } else if ((minInstId <= instId) && (instId < maxInstId) &&
               usefulInstSet.getBit(instId-minInstId) == false) {
        // inst is a useless instruction
        if (Log::isEnabled()) {
            Log::out() << "Useless inst found, removing: ";
            inst->print(Log::out());
            Log::out() << ::std::endl;
            Log::out() << " instId=" << (int)instId;
            Log::out() << ", minInstId=" << (int)minInstId;
            Log::out() << ", maxInstId=" << (int)maxInstId;
            Log::out() << ::std::endl;
        }
        inst->unlink();
        if (inst->isBranch()) {
            assert(0);
        } else if (inst->isSwitch()) {
            assert(0);
        } else if (inst->getOperation().canThrow()) {
            flowGraph.removeEdge(node->getExceptionEdge());
        }
        return;
    } else {
        switch (inst->getOpcode()) {
        case Op_Conv:
            {
                Type::Tag instType = inst->getType();
                
                if ((!Type::isInteger(instType)) ||
                    ((inst->getOverflowModifier() != Overflow_None) &&
                     (inst->getExceptionModifier() != Exception_Never))) 
                    break;

                Type *srcType = inst->getSrc(0)->getType();
                if (!srcType->isInteger())
                    break;
        
                U_8 convSize = getBitWidth(instType);
                U_8 srcSize = getBitWidth(srcType->tag);
                U_8 dstSize = getBitWidth(inst->getDst()->getType()->tag);

                if (dstSize != srcSize) {
                    // it's not just a sign-extension, we can't remove it
                    break;
                }

                assert((minInstId <= instId) && (instId < maxInstId));
                U_8 usedDstSize = usedInstWidth[instId-minInstId];

                if (Log::isEnabled()) {
                    Log::out() << "Found dstwidth of " << (int) usedDstSize
                               << " for inst ";
                    inst->print(Log::out());
                    Log::out() << ::std::endl;
                }
                
                if ((srcSize >= convSize) &&
                    (usedDstSize <= convSize)) {
                    // convert to a copy.
                    if (Log::isEnabled()) {
                        Log::out() << "Eliminating redundant conv ";
                        inst->print(Log::out());
                        Log::out() << ::std::endl;
                    }
                    InstFactory &factory = irManager.getInstFactory();
                    Inst *copyInst = factory.makeCopy(inst->getDst(), inst->getSrc(0));
                    copyInst->insertAfter(inst);
                    inst->unlink();
                    // copyInst doesn't appear in bitmaps, but we won't walk
                    // over it, so it is only tested for a MethodMarker Inst 
                    // operand above, and we are careful there
                }
            }
            break;
        default:
            break;
        }
    }
    //
    // delete stores to useless variables
    //
    if (canRemoveStvars && inst->getOpcode() == Op_StVar) {
        VarOpnd* dstVarOpnd = inst->getDst()->asVarOpnd();
        if (dstVarOpnd == NULL)
            return;
        if (usefulVarSet.getBit(dstVarOpnd->getId()) == false) {
            //
            // store to a useless variable
            //
            if (Log::isEnabled()) {
                Log::out() << "Removing store to useless var: ";
                inst->print(Log::out());
                Log::out() << ::std::endl;
            }
            inst->unlink();
            return;
        }       
    }
}

//
// Eliminate unreachable code
//
bool
DeadCodeEliminator::eliminateUnreachableCode() {
    MemoryManager memManager("DeadCodeEliminator::eliminateUnreachableCode");

    // Purge unreachable nodes from CFG.
    Nodes unreachableNodes(memManager);
    irManager.getFlowGraph().purgeUnreachableNodes(unreachableNodes);   

    // If no unreachable nodes, return
    if(unreachableNodes.empty())
        return false;

    // Clear the set of unreachable definitions.
    StlHashSet<U_32> unreachableDefSet(memManager);

    StlVector<Node*>::iterator niter;
    for(niter = unreachableNodes.begin(); niter != unreachableNodes.end(); ++niter) {
        Node* node = *niter;

        // Find unreachable defs.  Note, only SsaVarOpnds can be used 
        // in later reachable nodes.
        Inst* headInst = (Inst*)node->getFirstInst();
        assert(headInst->getNode());
        for (Inst* inst = headInst->getNextInst();inst!=NULL;inst=inst->getNextInst()) {
            assert(inst->getNode());
            SsaVarOpnd* dst = inst->getDst()->asSsaVarOpnd();
            if(dst != NULL) {
                assert(inst->getOpcode() == Op_StVar || inst->getOpcode() == Op_Phi);
                unreachableDefSet.insert(dst->getId());
            }
        }
    }

    // If no unreachable defs to cleanup, just return.
    if(unreachableDefSet.empty())
        return true;


    // Cleanup up phi instructions.
    const Nodes& nodes = flowGraph.getNodes();
    Nodes::const_iterator niter2;
    for(niter2 = nodes.begin(); niter2 != nodes.end(); ++niter2) {
        Node* node = *niter2;
        Inst* headInst = (Inst*)node->getFirstInst();
        assert(headInst->getNode());
        for (Inst* inst = headInst->getNextInst();inst!=NULL;inst=inst->getNextInst()) {
            assert(inst->getNode());
            if(inst->getOpcode() == Op_Phi) {
#ifndef NDEBUG
                SsaVarOpnd* dst = inst->getDst()->asSsaVarOpnd();
                assert(dst!=NULL);
#endif
                U_32 numSrc = inst->getNumSrcOperands();
                U_32 numKill = 0;
                for(U_32 i = 0; i < numSrc; ++i) {
                    SsaVarOpnd* src = inst->getSrc(i)->asSsaVarOpnd();
                    assert(src != NULL);
                    if(unreachableDefSet.find(src->getId()) != unreachableDefSet.end()) {
                        // Purge this operand.
                        ++numKill;
                    } else {
                        // Shift down over purged operands.
                        assert(numKill <= i);
                        if(numKill > 0)
                            inst->setSrc(i-numKill, src);
                    }
                }
                if(numKill > 0)
                    ((PhiInst*) inst)->setNumSrcs(numSrc-numKill);
            }
        }
    }
    return true;
}

static CountTime dcePhase1Timer("opt::dce::phase1");
static CountTime dcePhase2Timer("opt::dce::phase2");
static CountTime dcePhase3Timer("opt::dce::phase3");
static CountTime dcePhase4Timer("opt::dce::phase4");
static CountTime dcePhase5Timer("opt::dce::phase5");
static CountTime dcePhase6Timer("opt::dce::phase6");

//
// Performs dead code elimination
//
void
DeadCodeEliminator::eliminateDeadCode(bool keepEmptyNodes) {
    // user should call eliminateUnreachableCode() first

    U_32 minInstId = irManager.getMinimumInstId();
    U_32 maxInstId = irManager.getInstFactory().getNumInsts();
    U_32 numInsts = maxInstId - minInstId;
    U_32 numOpnds = irManager.getOpndManager().getNumVarOpnds();
    MemoryManager memManager("DeadCodeEliminator::eliminateDeadCode");
    InstDeque workSet(memManager);
    Nodes nodes(memManager);
    flowGraph.getNodesPostOrder(nodes);
    Nodes::reverse_iterator niter;

    // set of useful instructions & variables; initially everything is useless
    BitSet usefulInstSet(memManager,numInsts+1);
    BitSet usefulVarSet(memManager,numOpnds+1);
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    U_8 *usedInstWidth = optimizerFlags.dce2 ? new (memManager) U_8[numInsts] : 0;

    {
        AutoTimer t(dcePhase1Timer); 
        
    //
    // first propagate copies and initialize the work list with 
    // essential instructions
    //
    if (usedInstWidth) {
        for (niter = nodes.rbegin(); niter != nodes.rend(); ++niter) {
            Node* node = *niter;
            Inst* headInst = (Inst*)node->getFirstInst();
            assert(headInst->getNode());
            for (Inst* inst = headInst->getNextInst();inst!=NULL;inst=inst->getNextInst()) {
                assert(inst->getNode());
                // copy propagate all sources of this instruction
                copyPropagate(inst);
                // For inlined methods, the last instruction of the epilog copies the return value out.
                if (isNonEssential(inst) == false || (returnOpnd != NULL && returnOpnd == inst->getDst())) {
                    // add essential instruction to work list
                    assert(inst != NULL);
                    workSet.pushBack(inst);

                    assert(inst);
                    assert(inst->getNode());
                    U_32 instId = inst->getId();
                    usefulInstSet.setBit(instId-minInstId, true);
                    if (usedInstWidth) {
                        U_8 usedWidth = getBitWidth(inst->getType());
                        if (Log::isEnabled()) {
                            Log::out() << "Setting dstwidth to " << (int) usedWidth
                                       << " for inst ";
                            inst->print(Log::out());
                            Log::out() << ::std::endl;
                        }
                        assert((minInstId <= instId) && (instId < maxInstId));
                        usedInstWidth[instId-minInstId]= usedWidth;
                    }
                }
            }
        }
    } else {
        for (niter = nodes.rbegin(); niter != nodes.rend(); ++niter) {
            Node* node = *niter;
            Inst* headInst = (Inst*)node->getFirstInst();
            assert(headInst->getNode());
            for (Inst* inst = headInst->getNextInst();inst!=NULL;inst=inst->getNextInst()) {
                assert(inst->getNode());
                // copy propagate all sources of this instruction
                copyPropagate(inst);
                // For inlined methods, the last instruction of the epilog copies the return value out.
                if (isNonEssential(inst) == false || (returnOpnd != NULL && returnOpnd == inst->getDst())) {
                    // add essential instruction to work list
                    assert(inst != NULL);
                    workSet.pushBack(inst);

                    assert(inst);
                    assert(inst->getNode());
                    U_32 instId = inst->getId();
                    usefulInstSet.setBit(instId-minInstId, true);
                }
            }
        }
    }
    }

    {
        AutoTimer t(dcePhase2Timer); 

    //
    // Iteratively mark all useful instructions by computing slices
    //
    if (usedInstWidth) {
        while (!workSet.isEmpty()) {
            markLiveInst(workSet.popFront(),workSet,usefulInstSet,usefulVarSet,
                         usedInstWidth, minInstId, maxInstId);
        }
    } else {
        while (!workSet.isEmpty()) {
            markLiveInst1(workSet.popFront(),workSet,
                          usefulInstSet, usefulVarSet, minInstId, maxInstId);
        }
    }
    }

    {
        const bool canRemoveStVars = (irManager.getParent() == NULL); // we can remove a useless var
        AutoTimer t(dcePhase3Timer); 
        //
        // Now cleanup all the dead code
        //
        if (usedInstWidth) {
            for (niter = nodes.rbegin(); niter != nodes.rend(); ++niter) {
                Node* node = *niter;
                Inst* headInst = (Inst*)node->getFirstInst();
                assert(headInst->getNode());
                for (Inst* inst = headInst->getNextInst(); inst != NULL; ) {
                    assert(inst->getNode());
                    Inst *nextInst = inst->getNextInst();
                    sweepInst(node, inst,usefulInstSet,usefulVarSet,usedInstWidth,minInstId,maxInstId,canRemoveStVars);
                    inst = nextInst;
                }
            }
        } else {
            for (niter = nodes.rbegin(); niter != nodes.rend(); ++niter) {
                Node* node = *niter;
                Inst* headInst = (Inst*)node->getFirstInst();
                assert(headInst->getNode());
                for (Inst* inst = headInst->getNextInst(); inst != NULL; ) {
                    assert(inst->getNode());
                    Inst *nextInst = inst->getNextInst();
                    sweepInst1(node, inst,usefulInstSet,usefulVarSet,minInstId,maxInstId,canRemoveStVars);
                    inst = nextInst;
                }
            }
        }
    }

    {
        const bool canUnlink = (irManager.getParent() == NULL); // we can unlink an unused var

        AutoTimer t(dcePhase4Timer); 
        //
        // delete dead variables
        //
        OpndManager &opndManager = irManager.getOpndManager();
        VarOpnd *varOpnd0 = opndManager.getVarOpnds();
        if (varOpnd0) {
            VarOpnd *varOpnd = varOpnd0;
            // first check whether head needs to be deleted
            while (varOpnd == varOpnd0) {
                VarOpnd *nextVar = varOpnd->getNextVarOpnd();
                if (nextVar == varOpnd) nextVar = 0; // this should make us stop
                if (usefulVarSet.getBit(varOpnd->getId()) == true){
                    // live variable
                    varOpnd->setDeadFlag(false);  // mark as live
                } else {
                    // dead variable
                    varOpnd->setDeadFlag(true); // mark as dead
                    
                    if (canUnlink) {
                        if (Log::isEnabled()) {
                            Log::out() << "removing dead VarOpnd ";
                            varOpnd->print(Log::out());
                            Log::out() << ::std::endl;
                        }
                        opndManager.deleteVar(varOpnd); // remove from list
                    }
                    
                    // get the new head; this should usually be nextVar, but may be 0.
                    varOpnd0 = opndManager.getVarOpnds();
                    if (!nextVar && !varOpnd0) nextVar = varOpnd; // if it is 0, make sure we still stop
                }
                varOpnd = nextVar;
            };
            // we may have deleted all vars already, be careful here.
            if (varOpnd0) {
                varOpnd = varOpnd0->getNextVarOpnd();
                while (varOpnd != varOpnd0) {
                    VarOpnd *nextVar = varOpnd->getNextVarOpnd();
                    if (usefulVarSet.getBit(varOpnd->getId()) == true){
                        // live variable
                        varOpnd->setDeadFlag(false);  // mark as live
                    } else {
                        // dead variable

                        varOpnd->setDeadFlag(true); // mark as dead
                        if (canUnlink) {
                            if (Log::isEnabled()) {
                                Log::out() << "removing dead VarOpnd ";
                                varOpnd->print(Log::out());
                                Log::out() << ::std::endl;
                            }
                            opndManager.deleteVar(varOpnd); // remove from list
                        }
                    }
                    varOpnd = nextVar;
                }
            }
        }
    }

    {
        AutoTimer t(dcePhase6Timer); 
        flowGraph.mergeAdjacentNodes();       
    }
    {
        AutoTimer t(dcePhase5Timer); 
        if (!keepEmptyNodes) {
            //
            // Purge empty nodes.
            //
            flowGraph.purgeEmptyNodes(preserveCriticalEdges);
        }
    }

}

//
// Leave one PseudoThrow instruction only for those loops which
// do not contain other dispatch edges exiting the loop.
//
void
DeadCodeEliminator::removeExtraPseudoThrow() {
    MemoryManager memManager("DeadCodeEliminator::removeExtraPseudoThrow");

    OptPass::computeLoops(irManager);
    LoopTree* loopTree = irManager.getLoopTree();
    assert(loopTree && loopTree->isValid());

    if (Log::isLogEnabled(LogStream::DOTDUMP)) {
        OptPass::printDotFile(irManager, Log::getStageId(), "rept", "after_loop_tree");
        OptPass::printHIR(irManager);
    }

    // Nodes containing essential PseudoThrow instructions
    BitSet essentialNodes(memManager, flowGraph.getMaxNodeId());
    
    if (loopTree->hasLoops()) {
        LoopNode* loopNode = ((LoopNode*)loopTree->getRoot())->getChild();
        markEssentialPseudoThrows(loopNode, essentialNodes);
    }

    const Nodes& cfgNodes = flowGraph.getNodes();
    if (Log::isEnabled()) {
        Log::out() << "Removing useless PseudoThrow instructions:" << std::endl;
    }
    for (Nodes::const_iterator it = cfgNodes.begin(), end = cfgNodes.end(); it!=end; ++it) {
        Node* node = *it;
        Inst* lastInst = (Inst*)node->getLastInst();
        if (!essentialNodes.getBit(node->getId()) && (lastInst->getOpcode() == Op_PseudoThrow)) {
            if (Log::isEnabled()) {
                Log::out() << "  Removing instruction: ";
                lastInst->print(Log::out());
                Log::out() << std::endl;
            }
            lastInst->unlink();
            Edge* dispatchEdge = node->getExceptionEdge();
            assert(dispatchEdge != NULL);
            flowGraph.removeEdge(dispatchEdge);
        }
    }
    if (Log::isEnabled()) {
        Log::out() << "Done." << std::endl;
    }
    eliminateUnreachableCode();
}

void
DeadCodeEliminator::markEssentialPseudoThrows(LoopNode* loopNode, BitSet& essentialNodes) {
    LoopNode* childNode = loopNode->getChild();
    if (childNode != NULL)
        markEssentialPseudoThrows(childNode, essentialNodes);
    LoopNode* siblingNode = loopNode->getSiblings();
    if (siblingNode != NULL)
        markEssentialPseudoThrows(siblingNode, essentialNodes);
    if (Log::isEnabled()) {
        Log::out() << "Analyzing loop nodes with the loop header ID: "
            << loopNode->getHeader()->getId() << std::endl;
    }
    Node* mbEssentialNode = NULL;
    const Nodes& loopNodes = loopNode->getNodesInLoop();
    for (Nodes::const_iterator it = loopNodes.begin(), end = loopNodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (Log::isEnabled()) {
            Log::out() << "Analyzing Node ID: " << node->getId() << std::endl;
        }
        if (!node->isBlockNode()) continue;
        Edge* exceptionEdge = node->getExceptionEdge();
        while (exceptionEdge != NULL) {
            Node* targetNode = exceptionEdge->getTargetNode();
            if (Log::isEnabled()) {
                Log::out() << "  inspecting dispatch edge to node: " << targetNode->getId() << std::endl;
            }
            if (!loopNode->inLoop(targetNode)) break;
            exceptionEdge = targetNode->getExceptionEdge();
        }
        if (exceptionEdge != NULL) {
            if (Log::isEnabled()) {
                Log::out() << " Loop exit exception edge detected: ";
            }
            if (((Inst*)node->getLastInst())->getOpcode() == Op_PseudoThrow) {
                if (essentialNodes.getBit(node->getId())) {
                    // There is an essential PseudoThrow instruction in this loop
                    if (Log::isEnabled()) {
                        Log::out() << " essential PseudoThrow inst" << std::endl;
                    }
                    return;
                } else {
                    // A candidate to essential nodes
                    if (Log::isEnabled()) {
                        Log::out() << " essential candidate" << std::endl;
                    }
                    mbEssentialNode = node;
                }
            } else {
                // No essential PseudoThrow insts in this loop
                if (Log::isEnabled()) {
                    Log::out() << " PseudoThrow killer inst" << std::endl;
                }
                return;
            }
        }
    }
    if (mbEssentialNode != NULL) {
        essentialNodes.setBit(mbEssentialNode->getId());
        if (Log::isEnabled()) {
            Log::out() << "Found essential PseudoThrow in node ID: "
                << mbEssentialNode->getId() << std::endl;
        }
    }
    return;
}

void DeadCodeEliminator::createLoop2DispatchMapping(ControlFlowGraph& cfg, InfiniteLoopsInfo& info) {
    LoopTree* lt = cfg.getLoopTree();
    if (!lt->isValid()) {
        lt->rebuild(false, true);
    }
    info.hasLoops = lt->hasLoops();
    if (!info.hasLoops) {
        return;
    }

    //algorithm: 
    //for every loop find ANY node in a loop with dispatch edge that leads out of the loop
    //add the dispatch node to the mapping. Add nothing if dispatch is unwind
    //ANY dispatch is suitable to be used for infinite loop, because the point of interruption of infinite loop is undefined.
    const Nodes& nodes = cfg.getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (lt->getLoopDepth(node) == 0) {
            continue;
        }
        Edge* exceptionEdge = node->getExceptionEdge();
        if (exceptionEdge == NULL) {
            continue;
        }
        Node* dispatch = exceptionEdge->getTargetNode();
        Node* loopHead = lt->getLoopHeader(node, false);
        while (dispatch && lt->getLoopHeader(dispatch, false) == loopHead) {
            dispatch = dispatch->getExceptionEdgeTarget();
        }
        if (dispatch) {
            info.map[loopHead] = dispatch;
        }
    }
}

void DeadCodeEliminator::fixInfiniteLoops(IRManager& irm, const InfiniteLoopsInfo& info) {
    if (!info.hasLoops) {
        return;
    }
    //Find infinite loops and use mapping provided to add pseudoThrows.
    //To find infinite loops use dominator trees: 
    //    LoopHead == dominates on incoming edge source. 
    //    Infinite loop == LoopHead with no paths to Exit node: when Exit node does not postdominate on LoopHead
    if (Log::isEnabled()) {
        Log::out()<<"Performing infinite loops check "<<std::endl;
    }

    ControlFlowGraph& cfg = irm.getFlowGraph();
    MemoryManager tmpMM("fixInfiniteLoops");
    OptPass::computeDominators(irm);
    DominatorTree* dom = irm.getDominatorTree();
    DominatorBuilder db;
    DominatorTree* postDom = db.computeDominators(tmpMM, &cfg, true, true);

    Node* exitNode = cfg.getExitNode();
    Nodes infiniteLoopHeads(tmpMM);

    //searching for infinite loops
    Nodes nodes(tmpMM);
    cfg.getNodes(nodes);
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (postDom->dominates(exitNode, node)) { //the node is reachable from Exit node
            continue;
        }
        //ok, this is dead code or endless loop -> check if this is a loop
        const Edges& inEdges = node->getInEdges();
        for (Edges::const_iterator eit = inEdges.begin(), eend = inEdges.end(); eit !=eend; eit++) {
            Edge* edge = *eit;
            Node* srcNode = edge->getSourceNode();
            if (dom->dominates(node, srcNode)) { //ok, this is loop head
                infiniteLoopHeads.push_back(node);
                break;
            }
        }
    }
    
    //fixing infinite loops
    for (Nodes::const_iterator it = infiniteLoopHeads.begin(), end = infiniteLoopHeads.end(); it!=end; ++it) {
        Node* node = *it;
        Node* dispatch = NULL;
        StlMap<Node*, Node*>::const_iterator mit = info.map.find(node);
        if (mit!=info.map.end()) {
            dispatch = mit->second;
        } else {
            dispatch = cfg.getUnwindNode();
            if (dispatch == NULL) {
                dispatch = cfg.createDispatchNode(irm.getInstFactory().makeLabel());
                cfg.addEdge(dispatch, exitNode);
                cfg.setUnwindNode(dispatch);
            }
        }
        assert(dispatch!=NULL);
        cfg.splitNodeAtInstruction(node->getFirstInst(), true, false, irm.getInstFactory().makeLabel());
        node->appendInst(irm.getInstFactory().makePseudoThrow());
        cfg.addEdge(node, dispatch);
        if (Log::isEnabled()) {
            Log::out() <<"  Found infinite loop: node:";FlowGraph::print(Log::out(), node); Log::out()<<std::endl;
            Log::out() <<"      connecting loop to :";FlowGraph::print(Log::out(), dispatch); Log::out()<<std::endl;
        }
    }
    
    if (Log::isEnabled()) {
        Log::out()<<"Infinite loops check finished."<<std::endl;
    }

}


} //namespace Jitrino 
