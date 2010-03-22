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

#include "insertpi.h"
#include "walkers.h"
#include "abcdbounds.h"
#include "constantfolder.h"

namespace Jitrino {

static ComparisonModifier
negateComparison(ComparisonModifier mod)
{
    switch (mod) {
    case Cmp_GT: return Cmp_GTE;
    case Cmp_GT_Un: return Cmp_GTE_Un;
    case Cmp_GTE: return Cmp_GT;
    case Cmp_GTE_Un: return Cmp_GT_Un;
    case Cmp_EQ: return Cmp_EQ;
    case Cmp_NE_Un: return Cmp_NE_Un;
    default:
        assert(0); return mod;
    }
}

static const char *
printableComparison(ComparisonModifier mod)
{
    switch (mod) {
    case Cmp_GT: return "Cmp_GT";
    case Cmp_GT_Un: return "Cmp_GT_Un";
    case Cmp_GTE: return "Cmp_GTE";
    case Cmp_GTE_Un: return "Cmp_GTE_Un";
    case Cmp_EQ: return "Cmp_EQ";
    case Cmp_NE_Un: return "Cmp_NE_Un";
    default:
        assert(0); return "";
    }
}

static Type::Tag
unsignType(Type::Tag typetag)
{
    switch (typetag) {
    case Type::IntPtr: return Type::UIntPtr;
    case Type::Int8: return Type::UInt8;
    case Type::Int16: return Type::UInt16;
    case Type::Int32: return Type::UInt32;
    case Type::Int64: return Type::UInt64;
    default:
        assert(0); return typetag;
    }
}

// a DomWalker, to be applied in pre-order
class InsertPiWalker {
public:
    InsertPiWalker(InsertPi* insert_pi) : _insertPi(insert_pi) {};

    void applyToDominatorNode(DominatorNode *domNode) 
    { 
        _insertPi->insertPiToNode(domNode->getNode()); 
    }

    // is called before a node and its children are processed
    void enterScope() {};

    // is called after node and children are processed
    void exitScope() {};

private:
    InsertPi* _insertPi;
};
//------------------------------------------------------------------------------

// Add a Pi node in the node if it is after each test 
// which tells something about a variable
//
// WARNING: Pi var live ranges may overlap the original var live ranges
// since we don't bother to add Phi nodes and rename subsequent uses of var.
void InsertPi::insertPi()
{
    InsertPiWalker insert_pi_walker(this);

    // dom-pre-order traversal
    DomTreeWalk<true, InsertPiWalker>(_domTree, insert_pi_walker, _mm);
    renamePiVariables();

    if (Log::isEnabled()) {
        Log::out() << "IR after Pi insertion" << std::endl;
        FlowGraph::printHIR(Log::out(), _irManager.getFlowGraph(), 
                            _irManager.getMethodDesc());
        FlowGraph::printDotFile(_irManager.getFlowGraph(), 
                                _irManager.getMethodDesc(), "withpi");
    }
}
//------------------------------------------------------------------------------

// add Pi in the node iff after a test which tells something about the var
void InsertPi::insertPiToNode(Node* block)
{
    Edge *dom_edge = 0;

    // see if there is a predecessor block idom such that 
    //  (1) idom dominates this one
    //  (2) this block dominates all other predecessors
    //  (3) idom has multiple out-edges
    //  (4) idom has only 1 edge to this node

    // (1a) if a predecessor dominates it must be idom
    Node *idom = _domTree.getIdom(block);

    // (3) must exist and have multiple out-edges
    if ((idom == NULL) || (idom->hasOnlyOneSuccEdge())) {
        return;
    }

    if (Log::isEnabled()) {
        Log::out() << "Checking block " << (int)block->getId() << " with idom "
                   << (int) idom->getId() << std::endl;
    }

    if (block->hasOnlyOnePredEdge()) {
        // must be from idom -- (1b)
        // satisfies (2) trivially
        dom_edge = *(block->getInEdges().begin());
    } else { 
        // check (1b) and (2)
        const Edges &inedges = block->getInEdges();
        typedef Edges::const_iterator EdgeIter;
        EdgeIter e_last = inedges.end();
        for (EdgeIter e_iter = inedges.begin(); e_iter != e_last; e_iter++) {
            Edge *in_edge = *e_iter;
            Node *pred_block = in_edge->getSourceNode();
            if (pred_block == idom) {
                // (1b) found idom
                if (dom_edge) {
                    // failed (4): idom found on more than one incoming edge
                    return;
                }
                dom_edge = in_edge;
            } else if (! _domTree.dominates(block, pred_block)) {
                // failed (2)
                return;
            }
        }
    }

    if (dom_edge) { 
        Edge *in_edge = dom_edge;
        Node *pred_block = idom;
        if (Log::isEnabled()) {
            Log::out() << "Checking branch for " << (int)block->getId() 
                       << " with idom "
                       << (int) idom->getId() << std::endl;
        }
        if (!pred_block->hasOnlyOneSuccEdge()) {
            Edge::Kind kind = in_edge->getKind();
            switch (kind) {
            case Edge::Kind_True:
            case Edge::Kind_False:
                {
                    Inst* branchi1 = (Inst*)pred_block->getLastInst();
                    assert(branchi1 != NULL);
                    BranchInst* branchi = branchi1->asBranchInst();
                    if (branchi && branchi->isConditionalBranch()) {
                        insertPiForBranch(block, branchi, kind);
                    } else {
                        return;
                    }
                }
                break;

            case Edge::Kind_Dispatch: 
                return;

            case Edge::Kind_Unconditional:
                // Previous block must have a PEI
                // since it had multiple out-edges.
                // This is the unexceptional condition.
                { 
                    Inst* lasti = (Inst*)pred_block->getLastInst();
                    assert(lasti != NULL);
                    insertPiForUnexceptionalPEI(block, lasti);
                }
                // We could look for a bounds check in predecessor.

                // But: since now all useful PEIs have explicit results,
                // they imply a Pi-like action.
                break;
            case Edge::Kind_Catch:
                break;
            default:
            break;
            }
        }
    }
}
//------------------------------------------------------------------------------

void InsertPi::insertPiForUnexceptionalPEI(Node *block, Inst *lasti)
{
    switch (lasti->getOpcode()) {
    case Op_TauCheckBounds:
        {
            // the number of newarray elements must be >= 0.
            assert(lasti->getNumSrcOperands() == 2);
            Opnd *idxOp = lasti->getSrc(1);
            Opnd *boundsOp = lasti->getSrc(0);

            if (Log::isEnabled()) {
                Log::out() << "Adding info about CheckBounds instruction ";
                lasti->print(Log::out());
                Log::out() << std::endl;
            }
            Type::Tag typetag = idxOp->getType()->tag;
            PiBound lb(typetag, int64(0));
            PiBound ub(typetag, 1, VarBound(boundsOp),int64(-1));
            PiCondition bounds0(lb, ub);
            Opnd *tauOpnd = lasti->getDst(); // use the checkbounds tau
            insertPiForOpndAndAliases(block, idxOp, bounds0, tauOpnd);

            PiBound idxBound(typetag, 1, VarBound(idxOp), int64(1));
            PiCondition bounds1(idxBound, PiBound(typetag, false));
            insertPiForOpndAndAliases(block, boundsOp, bounds1, tauOpnd);
        }
        break;
    case Op_NewArray:
        {
            // the number of newarray elements must be in [0, MAXINT32]
            assert(lasti->getNumSrcOperands() == 1);
            Opnd *numElemOpnd = lasti->getSrc(0);
            if (Log::isEnabled()) {
                Log::out() << "Adding info about NewArray instruction ";
                lasti->print(Log::out());
                Log::out() << std::endl;
            }
            Opnd *tauOpnd = getBlockTauEdge(block); // need to use a TauEdge
            PiCondition bounds0(PiBound(numElemOpnd->getType()->tag, 
                                        int64(0)),
                                PiBound(numElemOpnd->getType()->tag, 
                                        int64(0x7fffffff)));
            insertPiForOpndAndAliases(block, numElemOpnd, bounds0, tauOpnd);
        }
        break;
    case Op_NewMultiArray:
        {
            // the number of newarray dimensions must be >= 1.
            U_32 numOpnds = lasti->getNumSrcOperands();
            assert(numOpnds >= 1);
            StlSet<Opnd *> done(_mm);
            if (Log::isEnabled()) {
                Log::out() << "Adding info about NewMultiArray instruction ";
                lasti->print(Log::out());
                Log::out() << std::endl;
            }
            Opnd *tauOpnd = 0;
            // the number of newarray elements must be in [0, MAXINT32]
            for (U_32 opndNum = 0; opndNum < numOpnds; opndNum++) {
                Opnd *thisOpnd = lasti->getSrc(opndNum);
                if (!done.has(thisOpnd)) {
                    done.insert(thisOpnd);
                    PiCondition bounds0(PiBound(thisOpnd->getType()->tag, 
                                                int64(0)),
                                        PiBound(thisOpnd->getType()->tag, 
                                                int64(0x7fffffff)));
                    if ( !tauOpnd ) {
                        tauOpnd = getBlockTauEdge(block); // must use a tauEdge
                    }
                    insertPiForOpndAndAliases(block, thisOpnd, bounds0, tauOpnd);
                }
            }
        }
        break;
    default:
        break;
    }    
}
//------------------------------------------------------------------------------

SsaTmpOpnd* InsertPi::getBlockTauEdge(Node *block) {
    if ((_lastTauEdgeBlock == block) && _blockTauEdge) return _blockTauEdge;
    Inst *firstInst = (Inst*)block->getFirstInst();
    Inst *inst = firstInst->getNextInst();
    for (; inst != NULL; inst = inst->getNextInst()) {
        if (inst->getOpcode() == Op_TauEdge) {
            _blockTauEdge = inst->getDst()->asSsaTmpOpnd();
            assert(_blockTauEdge);
            _lastTauEdgeBlock = block;
            return _blockTauEdge;
        }
    }
    for (inst = firstInst->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
        if ((inst->getOpcode() != Op_Phi) && (inst->getOpcode() != Op_TauPoint)) {
            break; // insert before inst.
        }
    }
    // no non-phis, insert before inst;
    TypeManager &tm = _irManager.getTypeManager();
    SsaTmpOpnd *tauOpnd = _irManager.getOpndManager().createSsaTmpOpnd(tm.getTauType());
    Inst* tauEdge = _irManager.getInstFactory().makeTauEdge(tauOpnd);
    if(Log::isEnabled()) {
        Log::out() << "Inserting tauEdge inst ";
        tauEdge->print(Log::out());
        if (inst!=NULL) {
            Log::out() << " before inst ";
            inst->print(Log::out()); 
        }
        Log::out() << std::endl;
    }
    if (inst != NULL) {
        tauEdge->insertBefore(inst);
    }  else {
        block->appendInst(tauEdge);
    }
    _blockTauEdge = tauOpnd;
    _lastTauEdgeBlock = block;
    return tauOpnd;
}
//------------------------------------------------------------------------------

// Insert Pi Nodes for any variables occurring in the branch test
//
// Since we're examining the test anyway, let's figure out the conditions
// here, too, so we don't have to duplicate any code.  Note that this 
// condition may already be in terms of Pi variables from the predecessor 
// block, since 
//   -- predecessor dominates this block
//   -- we are traversing blocks in a dominator-tree preorder
// so we must have already visited the predecessor.
//
// We also must add the new Pi variable to our map.
// 
void InsertPi::insertPiForBranch(Node* block, 
                                 BranchInst* branchi, 
                                 Edge::Kind kind) // True or False only
{
    Type::Tag instTypeTag = branchi->getType();
    if (!Type::isInteger(instTypeTag))
        return; 
    ComparisonModifier mod = branchi->getComparisonModifier();
    if (branchi->getNumSrcOperands() == 1) {
        Opnd *op0 = branchi->getSrc(0);
        PiCondition zeroBounds(PiBound(instTypeTag, (int64)0), 
                               PiBound(instTypeTag, (int64)0));
        switch (mod) {
        case Cmp_Zero:
            insertPiForComparison(block,
                                       Cmp_EQ,
                                       zeroBounds,
                                       op0,
                                       false,
                                       // negate if false edge
                                       (kind == Edge::Kind_False)); 
            break;
        case Cmp_NonZero:
            insertPiForComparison(block,
                                       Cmp_EQ, // use EQ
                                       zeroBounds,
                                       op0,
                                       false, 
                                       // but negate if true edge
                                       (kind == Edge::Kind_True));
            break;
    default:
        break;
        }
    } else {
        Opnd *op0 = branchi->getSrc(0);
        Opnd *op1 = branchi->getSrc(1);
        assert(branchi->getNumSrcOperands() == 2);
        PiCondition bounds0(op0->getType()->tag, op0);
        PiCondition bounds1(op1->getType()->tag, op1);
        if (!bounds0.isUnknown()) {
            insertPiForComparison(block,
                                       mod,
                                       bounds0,
                                       op1,
                                       false,
                                       // negate for false edge
                                       (kind == Edge::Kind_False));
        }
        if (!bounds1.isUnknown()) {
            insertPiForComparison(block,
                                       mod,
                                       bounds1,
                                       op0,
                                       true,
                                       // negate for false edge
                                       (kind == Edge::Kind_False));
        }
    }
}
//------------------------------------------------------------------------------

void InsertPi::insertPiForComparison(Node* block,
                                     ComparisonModifier mod,
                                     const PiCondition &bounds,
                                     Opnd* op,
                                     bool swap_operands,
                                     bool negate_comparison)
{
    if (Log::isEnabled()) {
        Log::out() << "insertPiForComparison(..., ";
        Log::out() << printableComparison(mod);
        Log::out() << ", ";
        bounds.print(Log::out());
        Log::out() << ", ";
        op->print(Log::out());
        Log::out() << ", ";
        Log::out() << (swap_operands ? "true" : "false");
        Log::out() << ", ";
        Log::out() << (negate_comparison ? "true" : "false");
        Log::out() << ")" << std::endl;

        // Print last inst from all prev blocks.
        Edges::const_iterator it = block->getInEdges().begin(),
            end = block->getInEdges().end();
        for(; it != end; ++it) {
            Edge* edge = (*it);
            Log::out() << "pred inst---> ";
            ((Inst*)edge->getSourceNode()->getLastInst())->print(Log::out());
            Log::out() << std::endl;
        }
    }

    PiCondition bounds0 = bounds;
    // add a Pi node for immediate value.
    if (negate_comparison) {
        mod = negateComparison(mod);
        swap_operands = !swap_operands;
        if (Log::isEnabled()) {
            Log::out() << "insertPiForComparison: negating comparison to " ;
            Log::out() << printableComparison(mod);
            Log::out() << std::endl;
        }
    }
    switch (mod) {
    case Cmp_EQ:
        if (!negate_comparison)
            insertPiForOpndAndAliases(block, op, bounds0, NULL);
        else {
            if (Log::isEnabled()) {
                Log::out() << "insertPiForComparison: cannot represent ! Cmp_EQ" << std::endl;
            }
        }
        // we can't represent the other case
        break;
    case Cmp_NE_Un:
        if (negate_comparison)
            insertPiForOpndAndAliases(block, op, bounds0, NULL);
        else {
            if (Log::isEnabled()) {
                Log::out() << "insertPiForComparison: cannot represent Cmp_NE_Un" << std::endl;
            }
        }
        // we can't represent the other case
        break;
    case Cmp_GT_Un:
        if (swap_operands) { // op > bounds, only a lower bound on op
            Type::Tag optag = op->getType()->tag;
            if (!Type::isUnsignedInteger(optag)) {
                // 1 is a lower bound on int op
                PiCondition oneBounds(PiBound(optag, (int64)1), 
                                      PiBound(optag, (int64)1));
                PiCondition oneLowerBound(oneBounds.only_lower_bound());
                insertPiForOpndAndAliases(block, op, oneLowerBound, NULL);
            } else {
                // we can be more precise for an unsigned op
                bounds0 = bounds0.cast(unsignType(bounds0.getType()));
                PiCondition bounds1a(bounds0.only_lower_bound());
                PiCondition bounds1(bounds1a.add((int64)1));
                if (! bounds1.getLb().isUnknown())
                    insertPiForOpndAndAliases(block, op, bounds1, NULL);
                else {
                    if (Log::isEnabled()) {
                        Log::out() << "insertPiForComparison(1): bounds1 LB is Unknown;\n\tbounds is ";
                        bounds.print(Log::out());
                        Log::out() << "\n\tbounds0 is ";
                        bounds0.print(Log::out());
                        Log::out() << "\n\tbounds1a is ";
                        bounds1a.print(Log::out());
                        Log::out() << "\n\tbounds1 is ";
                        bounds1.print(Log::out());
                        Log::out() << std::endl;
                    }
                }
            }
        } else { // bounds > op, only an upper bound on op
            Type::Tag optag = op->getType()->tag;
            if (Type::isUnsignedInteger(optag)) {
                // for an unsigned upper bound, we're ok
                bounds0 = bounds0.cast(unsignType(bounds0.getType()));
                PiCondition bounds1(bounds0.only_upper_bound().add((int64)-1));
                if (! bounds1.getUb().isUnknown())
                    insertPiForOpndAndAliases(block, op, bounds1, NULL);
                else {
                    if (Log::isEnabled()) {
                        Log::out() << "insertPiForComparison(2): bounds1 LB is Unknown;\n\tbounds is ";
                        bounds.print(Log::out());
                        Log::out() << "\n\tbounds0 is ";
                        bounds0.print(Log::out());
                        Log::out() << "\n\tbounds1 is ";
                        bounds1.print(Log::out());
                        Log::out() << std::endl;
                    }
                }
            } else {
                // otherwise, we know nothing unless bound is a small constant
                PiCondition bounds1(bounds0.only_upper_bound().add((int64)-1));
                if (bounds0.getUb().isConstant()) {
                    int64 ubConst = bounds1.getUb().getConst();
                    if (((optag == Type::Int32) &&
                         ((ubConst&0xffffffff) <= 0x7ffffff) && 
                         ((ubConst&0xffffffff) >= 0)) ||
                        ((optag == Type::Int64) &&
                         ((ubConst <= 0x7ffffff) && 
                          (ubConst >= 0)))) {
                        insertPiForOpndAndAliases(block, op, bounds1, NULL);
                    } else {
                        if (Log::isEnabled()) {
                            Log::out() << "insertPiForComparison(2): bounds1 LB is Unknown;\n\tbounds is ";
                            bounds.print(Log::out());
                            Log::out() << "\n\tbounds0 is ";
                            bounds0.print(Log::out());
                            Log::out() << "\n\tbounds1 is ";
                            bounds1.print(Log::out());
                            Log::out() << std::endl;
                        }
                    }
                }
            }
        }
        break;
    case Cmp_GT:
        if (swap_operands) { // op > bounds, only a lower bound on op
            PiCondition bounds1a(bounds0.only_lower_bound());
            PiCondition bounds1(bounds1a.add((int64)1));
            if (! bounds1.getLb().isUnknown())
                insertPiForOpndAndAliases(block, op, bounds1, NULL);
            else {
                if (Log::isEnabled()) {
                    Log::out() << "insertPiForComparison(1): bounds1 LB is Unknown;\n\tbounds is ";
                    bounds.print(Log::out());
                    Log::out() << "\n\tbounds0 is ";
                    bounds0.print(Log::out());
                    Log::out() << "\n\tbounds1a is ";
                    bounds1a.print(Log::out());
                    Log::out() << "\n\tbounds1 is ";
                    bounds1.print(Log::out());
                    Log::out() << std::endl;
                }
            }
        } else { // bounds > op, only an upper bound on op
            PiCondition bounds1(bounds0.only_upper_bound().add((int64)-1));
            if (! bounds1.getUb().isUnknown())
                insertPiForOpndAndAliases(block, op, bounds1, NULL);
            else {
                if (Log::isEnabled()) {
                    Log::out() << "insertPiForComparison(2): bounds1 LB is Unknown;\n\tbounds is ";
                    bounds.print(Log::out());
                    Log::out() << "\n\tbounds0 is ";
                    bounds0.print(Log::out());
                    Log::out() << "\n\tbounds1 is ";
                    bounds1.print(Log::out());
                    Log::out() << std::endl;
                }
            }
        }
        break;
    case Cmp_GTE_Un:
        if (swap_operands) { // op >= bounds, only lower bound on op
            Type::Tag optag = op->getType()->tag;
            if (!Type::isUnsignedInteger(optag)) {
                // 0 is a lower bound on an int op
                PiCondition zeroBounds(PiBound(optag, (int64)0), 
                                       PiBound(optag, (int64)0));
                PiCondition zeroLowerBound(zeroBounds.only_lower_bound());
                insertPiForOpndAndAliases(block, op, zeroLowerBound, NULL);
            } else {
                // we can be more precise for an unsigned op lb
                bounds0 = bounds0.cast(unsignType(bounds0.getType()));
                if (! bounds0.getLb().isUnknown()) {
                    insertPiForOpndAndAliases(block, op, 
                                                  bounds0.only_lower_bound(), NULL);
                } else {
                    if (Log::isEnabled()) {
                        Log::out() << "insertPiForComparison(3): bounds0 LB is Unknown;\n\tbounds is ";
                        bounds.print(Log::out());
                        Log::out() << "\n\tbounds0 is ";
                        bounds0.print(Log::out());
                        Log::out() << std::endl;
                    }
                }
            }
        } else { // bounds >= op, only upper bound on op
            Type::Tag optag = op->getType()->tag;
            if (Type::isUnsignedInteger(optag)) {
                // unsigned ub on unsigned op 
                bounds0 = bounds0.cast(unsignType(bounds0.getType()));
                if (! bounds0.getUb().isUnknown())
                    insertPiForOpndAndAliases(block, op, 
                                                  bounds0.only_upper_bound(), NULL);
                else {
                    if (Log::isEnabled()) {
                        Log::out() << "insertPiForComparison(4): bounds0 UB is Unknown;\n\tbounds is ";
                        bounds.print(Log::out());
                        Log::out() << "\n\tbounds0 is ";
                        bounds0.print(Log::out());
                        Log::out() << std::endl;
                    }
                }
            } else {
                // otherwise, we know nothing unless bound is a small constant
                if (bounds0.getUb().isConstant()) {
                    int64 ubConst = bounds0.getUb().getConst();
                    if (((optag == Type::Int32) &&
                         ((ubConst&0xffffffff) <= 0x7ffffff) && 
                         ((ubConst&0xffffffff) >= 0)) ||
                        ((optag == Type::Int64) &&
                         ((ubConst <= 0x7ffffff) && 
                          (ubConst >= 0)))) {
                        insertPiForOpndAndAliases(block, op, bounds0, NULL);
                    } else {
                        if (Log::isEnabled()) {
                            Log::out() << "insertPiForComparison(2): bounds0 LB is Unknown;\n\tbounds is ";
                            bounds.print(Log::out());
                            Log::out() << "\n\tbounds0 is ";
                            bounds0.print(Log::out());
                            Log::out() << std::endl;
                        }
                    }
                }
            }
        }
        break;
    case Cmp_GTE:
        if (swap_operands) { // op >= bounds, only lower bound on op
            if (! bounds0.getLb().isUnknown()) {
                insertPiForOpndAndAliases(block, op, 
                                              bounds0.only_lower_bound(), NULL);
            } else {
                if (Log::isEnabled()) {
                    Log::out() << "insertPiForComparison(3): bounds0 LB is Unknown;\n\tbounds is ";
                    bounds.print(Log::out());
                    Log::out() << "\n\tbounds0 is ";
                    bounds0.print(Log::out());
                    Log::out() << std::endl;
                }
            }
        } else { // bounds >= op, only upper bound on op
            if (! bounds0.getUb().isUnknown())
                insertPiForOpndAndAliases(block, op, 
                                              bounds0.only_upper_bound(), NULL);
            else {
                if (Log::isEnabled()) {
                    Log::out() << "insertPiForComparison(4): bounds0 UB is Unknown;\n\tbounds is ";
                    bounds.print(Log::out());
                    Log::out() << "\n\tbounds0 is ";
                    bounds0.print(Log::out());
                    Log::out() << std::endl;
                }
            }
        }
        break;
    case Cmp_Zero:
    case Cmp_NonZero:
    case Cmp_Mask:
        assert(0);
        break;
    default:
    assert(false);
    break;
    }
}
//------------------------------------------------------------------------------

void InsertPi::insertPiForOpnd(Node *block, 
                               Opnd *org, 
                               const PiCondition &cond,
                               Opnd *tauOpnd)
{
    if (ConstantFolder::isConstant(org)) {
        if (Log::isEnabled()) {
            Log::out() << "Skipping Pi Node for opnd ";
            org->print(Log::out());
            Log::out() << " under condition ";
            cond.print(Log::out());
            Log::out() << " since it is constant" << std::endl;
        }
    } else {
        PiOpnd *piOpnd = _irManager.getOpndManager().createPiOpnd(org);
        Inst *headInst = (Inst*)block->getFirstInst();
        PiCondition *condPtr = new (_irManager.getMemoryManager()) PiCondition(cond);
        if (tauOpnd == 0)
            tauOpnd = getBlockTauEdge(block);
        Inst *newInst = _irManager.getInstFactory().makeTauPi(piOpnd, org, tauOpnd, condPtr);
        Inst *place = headInst->getNextInst();
        while (place != NULL) {
            Opcode opc = place->getOpcode();
            if ((opc != Op_Phi) && (opc != Op_TauPoint) && (opc != Op_TauEdge))
                break;
            place = place->getNextInst();
        }
        if (Log::isEnabled()) {
            Log::out() << "Inserting Pi Node for opnd ";
            org->print(Log::out());
            Log::out() << " under condition ";
            cond.print(Log::out());
            if (place!=NULL) {
                Log::out() << " just before inst ";
                place->print(Log::out());
            }
            Log::out() << std::endl;
        }
        if (place != NULL) {
            newInst->insertBefore(place);
        } else {
            block->appendInst(newInst);
        }
    }
}
//------------------------------------------------------------------------------

// dereferencing through Pis, 0 if not constant.
Opnd* InsertPi::getConstantOpnd(Opnd *opnd)
{
    return ConstantFolder::isConstant(opnd) ? opnd : NULL;
}
//------------------------------------------------------------------------------

bool InsertPi::getAliases(Opnd *opnd, AbcdAliases *aliases, int64 addend)
{
    Inst *inst = opnd->getInst();
    switch (inst->getOpcode()) {
    case Op_TauPi:
        return getAliases(inst->getSrc(0), aliases, addend);

    case Op_Add:
        {
            Opnd *op0 = inst->getSrc(0);
            Opnd *op1 = inst->getSrc(1);
            Opnd *constOpnd0 = getConstantOpnd(op0);
            Opnd *constOpnd1 = getConstantOpnd(op1);
            if ((constOpnd0 || constOpnd1) &&
                (inst->getType() == Type::Int32)) {
                // I assume we've done folding first
                assert(!(constOpnd0 && constOpnd1));
                if (constOpnd1) {
                    // swap the operands;
                    constOpnd0 = constOpnd1;
                    op1 = op0;
                }
                // now constOpnd0 should be constant
                // op1 is the non-constant operand

                Inst *inst0 = constOpnd0->getInst();
                assert(inst0);
                ConstInst *cinst0 = inst0->asConstInst();
                assert(cinst0);
                ConstInst::ConstValue cv = cinst0->getValue();
                I_32 c = cv.i4;
                int64 sumc = c + addend;
                if (add_overflowed<int64>(sumc, c, addend)) {
                    return false;
                } else {
                    VarBound vb(op1);
                    aliases->theSet.insert(PiBound(inst->getType(), 1, vb, sumc));
                    getAliases(op1, aliases, sumc);
                    return true;
                }
            }
        }
        break;
    case Op_Sub:
        {
            Opnd *constOpnd = getConstantOpnd(inst->getSrc(1));
            if (constOpnd && (inst->getType() == Type::Int32)) {
                Opnd *op1 = constOpnd;
                Inst *inst1 = op1->getInst();
                assert(inst1);
                ConstInst *cinst1 = inst1->asConstInst();
                assert(cinst1);
                ConstInst::ConstValue cv = cinst1->getValue();
                int64 c = cv.i4;
                int64 negc = -c;
                int64 subres = addend + negc;
                if (neg_overflowed<int64>(negc, c) ||
                    add_overflowed<int64>(subres, addend, negc)) {
                    return false;
                } else {
                    VarBound vb(op1);
                    aliases->theSet.insert(PiBound(inst->getType(), 1, vb, subres));
                    getAliases(op1, aliases, subres);
                    return true;
                }
            }
        }
        break;
    case Op_Copy:
        assert(0); // do copy propagation first
        break;
    case Op_TauCheckZero:
        return false;
    default:
        break;
    }
    return false;
}
//------------------------------------------------------------------------------

// checks for aliases of opnd, inserts them.
void InsertPi::insertPiForOpndAndAliases(Node *block, 
                                         Opnd *org, 
                                         const PiCondition &cond,
                                         Opnd *tauOpnd)
{
    const PiBound &lb = cond.getLb();
    const PiBound &ub = cond.getUb();

    if (_useAliases) {
        if (Log::isEnabled()) {
            Log::out() << "Inserting Pi Node for opnd ";
            org->print(Log::out());
            Log::out() << " and its aliases";
            Log::out() << " under condition ";
            cond.print(Log::out());
            Log::out() << std::endl;
        }
        AbcdAliases aliases(_mm);
        // check for aliases
        insertPiForOpnd(block, org, cond, tauOpnd);
        if (getAliases(org, &aliases, 0)) {
            if (Log::isEnabled()) {
                Log::out() << "Has aliases ";
                AbcdAliasesSet::iterator iter = aliases.theSet.begin();
                AbcdAliasesSet::iterator end = aliases.theSet.end();
                for ( ; iter != end; iter++) {
                    PiBound alias = *iter;
                    alias.print(Log::out());
                    Log::out() << " ";
                }
                Log::out() << std::endl;
            }
            AbcdAliasesSet::iterator iter = aliases.theSet.begin();
            AbcdAliasesSet::iterator end = aliases.theSet.end();
            for ( ; iter != end; iter++) {
                PiBound alias = *iter; 
                PiBound inverted = alias.invert(org); // org - c
                // plug-in lb and ub into inverted, yields bounds:
                //   [ lb - c, ub - c ]
                PiCondition renamedCondition(PiBound(inverted, org, lb),
                                             PiBound(inverted, org, ub));
                insertPiForOpnd(block, alias.getVar().the_var, 
                                    renamedCondition, tauOpnd);
            }
        }
    } else {
        insertPiForOpnd(block, org, cond, tauOpnd);
    }
}
//------------------------------------------------------------------------------

// a DomWalker, to be applied forwards/preorder
class RenamePiWalker {
public:

    RenamePiWalker(InsertPi *insert_pi,
                   MemoryManager &localMM,
                   SparseOpndMap* &piMap0,
                   int sizeEstimate0) : 
        _insertPi(insert_pi), 
        localMemManager(localMM), 
        _piMap(piMap0),
        sizeEstimate(sizeEstimate0)
    {}

    void applyToDominatorNode(DominatorNode *domNode) 
    { 
        _insertPi->renamePiVariablesInNode(domNode->getNode()); 
    }

    void enterScope() 
    {
        if (!_piMap) _piMap = 
            new (localMemManager) SparseOpndMap(sizeEstimate,
                                                localMemManager, 1, 4, 7);
        _piMap->enter_scope(); 
    }

    void exitScope() 
    { 
        _piMap->exit_scope(); 
    }
private:
    InsertPi* _insertPi;
    MemoryManager &localMemManager;
    SparseOpndMap* &_piMap;
    int sizeEstimate;
};
//------------------------------------------------------------------------------

void InsertPi::renamePiVariables()
{
    MethodDesc &methodDesc= _irManager.getMethodDesc();
    U_32 byteCodeSize = methodDesc.getByteCodeSize();
    MemoryManager localMemManager("Abcd::renamePiNodes");

    RenamePiWalker theWalker(this, localMemManager, _piMap, byteCodeSize);
    DomTreeWalk<true, RenamePiWalker>(_domTree, theWalker, localMemManager);
}
//------------------------------------------------------------------------------

void InsertPi::renamePiVariablesInNode(Node *block)
{
    // For each variable use in the block, check for a Pi version in
    // the piTable.  Since we are visiting in preorder over dominator
    // tree dominator order, any found version will dominate this node.

    // Phase 0: remap just Pi inst source operands.
    Inst* headInst = (Inst*)block->getFirstInst();
    for (Inst* inst = headInst->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
        if (inst->getOpcode() == Op_TauPi) {
            // Replace Pi source operands with renames from the map.
            Opnd *dstOpnd = inst->getDst();
            assert(dstOpnd->isPiOpnd());
            Opnd *srcOpnd = inst->getSrc(0);
            if (_useAliases) {
                if (srcOpnd->isSsaVarOpnd()) {
                    srcOpnd = srcOpnd->asSsaVarOpnd()->getVar();
                }
            }
            // Find the end of the operand replace chain.
            // The most constrained operand is being used, the more
            // opportunities we have for optimization.
            Opnd *replaceOpnd = _piMap->lookupTillEnd(srcOpnd);
            if (replaceOpnd) {
                // Disallow remapping source operand of Pi Nodes to their
                // destination operands.
                if (Log::isEnabled()) {
                    Log::out() << "remapping src in Pi: ";
                    inst->print(Log::out());
                    Log::out() << ", replaceOpnd: ";
                    replaceOpnd->print(Log::out());
                }
                if (dstOpnd->getId() != replaceOpnd->getId()) {
                    inst->setSrc(0, replaceOpnd);
                    srcOpnd = replaceOpnd;
                    if (Log::isEnabled()) {
                        Log::out() << "... done" << std::endl;
                    }
                } else {
                    if (Log::isEnabled()) {
                        Log::out() << "... defines this opnd, cannot remap" << std::endl;
                    }
                }
            }

            // We now replaced the source. Add any Pi node destination to the map.
            _piMap->insert(srcOpnd, dstOpnd);
            if (Log::isEnabled()) {
                Log::out() << "adding remap for Pi of ";
                srcOpnd->print(Log::out());
                Log::out() << " to ";
                inst->getDst()->print(Log::out());
                Log::out() << std::endl;
            }
        }
    }

    // Phase 1: add Pi remappings, remap source operands of other instructions
    // and pi conditions.
    for (Inst* inst = headInst->getNextInst(); inst != NULL; inst = inst->getNextInst()) {

        // Remap source operands if they match a Pi definition.
        U_32 numOpnds = inst->getNumSrcOperands();
        for (U_32 i=0; i<numOpnds; i++) {
            Opnd *opnd = inst->getSrc(i);
            while (opnd->isPiOpnd()) {
                opnd = opnd->asPiOpnd()->getOrg();
            }
            // Transitively look up for deepest Pi renamings of this operand.
            // The most constrained operand is being used, the more
            // opportunities we have for optimization.
            Opnd *foundOpnd = _piMap->lookupTillEnd(opnd);
            if (foundOpnd) {
                // Remap the source operand except for Pi instruction and for
                // array argument for chkbounds instruction. The latter is
                // because (a) we do not need any facts provided by pi renaming
                // for this operand, (b) we should limit array operand renaming
                // to be able to search the proof path by exact array operand
                // id.
                if (inst->getOpcode() != Op_TauPi &&
                    (inst->getOpcode() != Op_TauCheckBounds || i != 0)) {
                    inst->setSrc(i, foundOpnd);
                }
            }
        }

        // Remap variables appearing in the condition of the Pi node.
        if (inst->getOpcode() == Op_TauPi) {
            TauPiInst *thePiInst = inst->asTauPiInst();
            assert(thePiInst);

            if (Log::isEnabled()) {
                Log::out() << "remapping condition in ";
                inst->print(Log::out());
                Log::out() << std::endl;
            }
            PiCondition *cond = thePiInst->cond;
            if (Log::isEnabled()) {
                Log::out() << "  original condition is ";
                cond->print(Log::out());
                Log::out() << std::endl;
            }
            Opnd *lbRemap = cond->getLb().getVar().the_var;
            if (lbRemap) {
                if (Log::isEnabled()) {
                    Log::out() << "  has lbRemap=";
                    lbRemap->print(Log::out());
                    Log::out() << std::endl;
                }
                if (lbRemap->isPiOpnd())
                    lbRemap = lbRemap->asPiOpnd()->getOrg();
                Opnd *lbRemapTo = _piMap->lookupTillEnd(lbRemap);
                if (lbRemapTo) {
                    if (Log::isEnabled()) {
                        Log::out() << "adding remap of lbRemap=";
                        lbRemap->print(Log::out());
                        Log::out() << " to lbRemapTo=";
                        lbRemapTo->print(Log::out());
                        Log::out() << " to condition ";
                        cond->print(Log::out());
                    }
                    PiCondition remapped(*cond, lbRemap, lbRemapTo);
                    if (Log::isEnabled()) {
                        Log::out() << " YIELDS1 ";
                        remapped.print(Log::out());
                    }
                    *cond = remapped;
                    if (Log::isEnabled()) {
                        Log::out() << " YIELDS ";
                        cond->print(Log::out());
                        Log::out() << std::endl;
                    }
                }
            }
            Opnd *ubRemap = cond->getUb().getVar().the_var;
            if (ubRemap && (lbRemap != ubRemap)) {
                if (Log::isEnabled()) {
                    Log::out() << "  has ubRemap=";
                    ubRemap->print(Log::out());
                    Log::out() << std::endl;
                }
                if (ubRemap->isPiOpnd())
                    ubRemap = ubRemap->asPiOpnd()->getOrg();
                Opnd *ubRemapTo = _piMap->lookupTillEnd(ubRemap);
                if (ubRemapTo) {
                    if (Log::isEnabled()) {
                        Log::out() << "adding remap of ubRemap=";
                        ubRemap->print(Log::out());
                        Log::out() << " to ubRemapTo=";
                        ubRemapTo->print(Log::out());
                        Log::out() << " to condition ";
                        cond->print(Log::out());
                    }
                    PiCondition remapped(*cond, ubRemap, ubRemapTo);
                    if (Log::isEnabled()) {
                        Log::out() << " YIELDS1 ";
                        remapped.print(Log::out());
                    }
                    *cond = remapped;
                    if (Log::isEnabled()) {
                        Log::out() << " YIELDS ";
                        cond->print(Log::out());
                        Log::out() << std::endl;
                    }
                }
            }
        }
    }
}
//------------------------------------------------------------------------------

// a ScopedDomNodeInstWalker, forward/preorder
class RemovePiWalker {
public:
    RemovePiWalker(InsertPi* ins) : _insertPi(ins), block(0) // forward
    {}

    void startNode(DominatorNode *domNode) { block = domNode->getNode(); };
    void applyToInst(Inst *i) { _insertPi->removePiOnInst(block, i); }
    void finishNode(DominatorNode *domNode) {}

    void enterScope() {}
    void exitScope() {}
private:
    InsertPi* _insertPi;
    Node* block;
};
//------------------------------------------------------------------------------

void InsertPi::removePi()
{
    RemovePiWalker removePiWalker(this);
    typedef ScopedDomNodeInst2DomWalker<true, RemovePiWalker> 
        RemovePiDomWalker;
    RemovePiDomWalker removePiDomWalker(removePiWalker);
    DomTreeWalk<true, RemovePiDomWalker>(_domTree, removePiDomWalker, _mm);
}
//------------------------------------------------------------------------------

void InsertPi::removePiOnInst(Node* block, Inst *inst)
{
    if ( inst->getOpcode() == Op_TauPi ) {
        inst->unlink();
    }else{
        // replace Pi operands with original ones
        U_32 num_opnds = inst->getNumSrcOperands();
        for (U_32 i = 0; i < num_opnds; i++) {
            Opnd* pi_opnd = inst->getSrc(i);
            while ( pi_opnd->isPiOpnd() ) {
                pi_opnd = pi_opnd->asPiOpnd()->getOrg();
            }
            inst->setSrc(i, pi_opnd);
        }
    }
}
//------------------------------------------------------------------------------

} //namespace Jitrino 

