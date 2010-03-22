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

#include "irmanager.h"
#include "Dominator.h"
#include "classic_abcd.h"
#include "classic_abcd_solver.h"
#include "opndmap.h"

#include "Stl.h"
#include "Log.h"
#include "open/types.h"
#include "Inst.h"
#include "walkers.h"
#include "PMFAction.h"
#include "constantfolder.h"

#include <assert.h>
#include <iostream>
#include <algorithm>

namespace Jitrino {

// eliminating Array Bounds Check on Demand
DEFINE_SESSION_ACTION(CLASSIC_ABCDPass, classic_abcd, "Classic ABCD: eliminating Array Bounds Check on Demand");

void
CLASSIC_ABCDPass::_run(IRManager &irm) {
    OptPass::splitCriticalEdges(irm);
    OptPass::computeDominators(irm);
    ClassicAbcd classic_abcd(this, irm, irm.getNestedMemoryManager(), 
                             *irm.getDominatorTree());
    classic_abcd.runPass();
}
//------------------------------------------------------------------------------

class IOpndProxy : public IOpnd
{
public:
    IOpndProxy(Opnd* opnd);

    IOpndProxy(I_32 c, U_32 id);

    virtual void printName(std::ostream& os) const
    {
        if ( _opnd ) {
            _opnd->print(os);
        }else{
            os << "_c" << getID() << "(const=" << getConstant() << ")";
        }
    }

    Opnd* getOrg() const { return _opnd; }

    static U_32 getProxyIdByOpnd(Opnd* opnd);
private:
    Opnd* _opnd;

    /* ids of PiOpnd, SsaOpnd, VarOpnd may alias their IDs,
     * encoding all in one ID with unaliasing
     */
    static const U_32 min_var_opnd = 0;
    static const U_32 min_ssa_opnd = MAX_UINT32 / 4;
    static const U_32 min_pi_opnd = (min_ssa_opnd) * 2;
    static const U_32 min_const_opnd = (min_ssa_opnd) * 3;
};
//------------------------------------------------------------------------------

// for debugging purposes
void printIOpnd(IOpnd* opnd)
{
    opnd->printName(std::cout);
    std::cout << std::endl;
}

bool inInt32(int64 c) {
    return (int64)(I_32)c == c;
}

bool inInt32Type(Type t) {
    return (t.tag == Type::Int8) || 
         (t.tag == Type::Int16) || 
         (t.tag == Type::Int32);
}

IOpndProxy::IOpndProxy(Opnd* opnd) : 
    IOpnd(0/* id */, 
          opnd->getInst()->isPhi() /* is_phi */, 
          ConstantFolder::isConstant(opnd) /* is_constant */),
    _opnd(opnd)
{
    setID(getProxyIdByOpnd(_opnd));
    if ( isConstant() ) {
        ConstInst* c_inst = _opnd->getInst()->asConstInst();
        assert(c_inst);
        int64 value = c_inst->getValue().i8;
        if ( inInt32Type(c_inst->getType()) ) {
            value = c_inst->getValue().i4;
        }else if ( c_inst->getType() != Type::Int64 ) {
            setUnconstrained(true);
            return;
        }
        if ( inInt32(value) ) {
            setConstant((I_32)value);
        }else{
            setUnconstrained(true);
        }
    }
}
//------------------------------------------------------------------------------

IOpndProxy::IOpndProxy(I_32 c, U_32 id) : 
    IOpnd(0, false /* is_phi */, true /* is_constant */),
    _opnd(NULL)
{
    setID(min_const_opnd + id);
    setConstant(c);
}

U_32 IOpndProxy::getProxyIdByOpnd(Opnd* opnd)
{
    U_32 id = opnd->getId();
    if ( opnd->isVarOpnd() ) {
        id += min_var_opnd;
    }else if ( opnd->isPiOpnd() ) { 
        // note: PiOpnd inherits from SsaOpnd, check PiOpnd first
        id += min_pi_opnd;
    }else if ( opnd->isSsaOpnd() ) {
        id += min_ssa_opnd;
    }else {
        assert(0);
    }
    return id;
}
//------------------------------------------------------------------------------

class BuildInequalityGraphWalker {
public:
    BuildInequalityGraphWalker(InequalityGraph* igraph) :
        _igraph(igraph), _const_id_counter(1 /*reserve 0 for solver*/),
        _map_opnd_to_pi_inst(igraph->getMemoryManager())
    {}

    void startNode(DominatorNode *domNode);
    void applyToInst(Inst* i);
    void finishNode(DominatorNode *domNode);

    void enterScope() {}
    void exitScope() {}
private:
    // returns true if an edge to const opnd is actually added
    bool addEdgeIfConstOpnd(IOpndProxy* dst, Opnd* const_src, Opnd* src, 
                            bool negate_src);

    void addAllSrcOpndsForPhi(Inst* inst);

    // returns true if the edge is actually added
    bool addDistance(IOpndProxy* dst, IOpndProxy* src, int64 constant, 
                     bool negate);

    void addDistanceSingleProblem(IOpndProxy* to, IOpndProxy* from, int64 c, 
                                  bool lower_problem);

    void addPiEdgesSingleProblem
         (IOpndProxy* dst, bool lower_problem, const PiBound& non_inf_bound);

    IOpndProxy* findProxy(Opnd* opnd);

    IOpndProxy* addOldOrCreateOpnd(Opnd* opnd);

    InequalityGraph* _igraph;
    U_32 _const_id_counter;

    // operands are mapped to their renaming Pi instructions during the walk
    // (applyToInst), and then this mapping is used to create edges between
    // operands in InequalityGraph. This allows to link newer operands with
    // constraints (edges) rather than old ones;
    //
    // Example:
    //   if ( x.1 < y.1 ) {
    //     pi( x.1 : [undef, y.1 - 1] ) -> x.2 // inst.X
    //     pi( y.1 : [x.1 + 1, undef] ) -> y.2 // inst.Y
    //   }
    //
    // we collect the mapping:
    //     x.1 -> inst.X
    //     y.1 -> inst.Y
    // to deduce edges:
    //     upper-only edge: x.2 - y.2 <= -1 // hint: 'to' - 'from'
    //     lower-only edge: 1 <= y.2 - x.2
    // instead of the straightforward:
    //     upper-only edge: x.2 - y.1 <= -1 // hint: 'to' - 'from'
    //     lower-only edge: 1 <= y.2 - x.1
    //
    // (ideally there should only be 2 elements in the map for each basic block
    //  at most (taken from "if a < b" or such), but our InsertPi sometimes adds
    //  more)
    typedef StlMap<IOpndProxy*, TauPiInst*> OpndToPiInst2ElemMap;
    OpndToPiInst2ElemMap _map_opnd_to_pi_inst;
};
//------------------------------------------------------------------------------

IOpndProxy* BuildInequalityGraphWalker::findProxy(Opnd* opnd)
{
    assert(_igraph);
    return (IOpndProxy*) _igraph->findOpnd(IOpndProxy::getProxyIdByOpnd(opnd));
}
//------------------------------------------------------------------------------

void BuildInequalityGraphWalker::addAllSrcOpndsForPhi(Inst* inst)
{
    assert(inst->getOpcode() == Op_Phi);
    for (U_32 j = 0; j < inst->getNumSrcOperands(); j++) {
        IOpndProxy* proxy_src = addOldOrCreateOpnd(inst->getSrc(j));
        addDistance(findProxy(inst->getDst()), proxy_src, 0, false /*negate*/);
    }
}
//------------------------------------------------------------------------------

void BuildInequalityGraphWalker::startNode(DominatorNode *domNode)
{
    if ( Log::isEnabled() &&
         !_map_opnd_to_pi_inst.empty() ) {
        Log::out() << "_map_opnd_to_pi_inst before clear:" << std::endl;
        OpndToPiInst2ElemMap::const_iterator 
            it = _map_opnd_to_pi_inst.begin(),
            end = _map_opnd_to_pi_inst.end();
        for (; it != end; it++ ) {
            IOpndProxy* opnd = it->first;
            TauPiInst* inst = it->second;
            Log::out() << " opnd: ";
            opnd->printName(Log::out());
            Log::out() << " -> inst: ";
            inst->print(Log::out());
            Log::out() << std::endl;
        }
    }
    _map_opnd_to_pi_inst.clear();
}
//------------------------------------------------------------------------------

bool isIntOrLong(Type* t) {
    return t->isInt4() || t->isInt8();
}
//------------------------------------------------------------------------------

void BuildInequalityGraphWalker::applyToInst(Inst* inst)
{
    assert(inst);

    Type::Tag inst_type = inst->getType();
    if ( !Type::isInteger(inst_type) && inst_type != Type::Boolean &&
         inst_type != Type::Char ) {
        // note: some operations of unsupported type can produce operands of
        // supported (int) types, for example,
        // inst-compare-two-unmanaged-pointers, we need these operands as
        // unconstrained in the graph
        Opnd* dst = inst->getDst();
        if ( dst && !dst->isNull() &&
                (dst->getType()->isInteger() ||
                 dst->getType()->isBoolean() ) ) {
            addOldOrCreateOpnd(dst)->setUnconstrained(true);
        }
        return;
    }
    if ( inst->isUnconditionalBranch() || inst->isConditionalBranch() || 
         inst->isReturn() ) {
        return;
    }
    IOpndProxy* proxy_dst;
    Opcode opc = inst->getOpcode();
    switch ( opc ) {
        case Op_Phi:
        {
            proxy_dst = addOldOrCreateOpnd(inst->getDst());
            addAllSrcOpndsForPhi(inst);
        }
            break;
        case Op_Copy:
        case Op_LdVar:
        case Op_StVar:
        {
            proxy_dst = addOldOrCreateOpnd(inst->getDst());
            addDistance(proxy_dst, findProxy(inst->getSrc(0)), 0, 
                        false /* negate */);
        }
            break;
        case Op_Conv:
        {
            // adding conversions int<->long as zero-length edges to the
            // inequality graph. This is a small enhancement to the paper's
            // algorithm, but it is safe. The proof is as follows: if we ensure
            // that a value of type 'long' is in good bounds for array access,
            // then is is proven to be not outside bounds for type 'int'
            Opnd* src = inst->getSrc(0);
            Opnd* dst = inst->getDst();
            if ( isIntOrLong(src->getType()) &&
                 isIntOrLong(dst->getType()) ) {
                proxy_dst = addOldOrCreateOpnd(inst->getDst());
                addDistance(proxy_dst, findProxy(inst->getSrc(0)), 0, 
                            false /* negate */);
            }else{
                addOldOrCreateOpnd(inst->getDst())->setUnconstrained(true);
            }
        }
            break;
        case Op_Add:
        {
            proxy_dst = addOldOrCreateOpnd(inst->getDst());
            Opnd* src0 = inst->getSrc(0);
            Opnd* src1 = inst->getSrc(1);
            addEdgeIfConstOpnd(proxy_dst, src0, src1, false /* negate */) 
            || addEdgeIfConstOpnd(proxy_dst, src1, src0, false /* negate */);
        }
            break;
        case Op_Sub:
        {
            proxy_dst = addOldOrCreateOpnd(inst->getDst());
            addEdgeIfConstOpnd(proxy_dst, inst->getSrc(1), inst->getSrc(0),
                               true /* negate */ );
        }
            break;
        case Op_TauPi:
        {
            proxy_dst = addOldOrCreateOpnd(inst->getDst());
            IOpndProxy* src0 = findProxy(inst->getSrc(0));
            addDistance(proxy_dst, src0, 0, false /* negate */);
            _map_opnd_to_pi_inst[src0] = inst->asTauPiInst();
            if ( Log::isEnabled() ) {
                Log::out() << "mapping (src->pi inst): src: ";
                src0->printName(Log::out());
                Log::out() << " inst: ";
                inst->print(Log::out());
                Log::out() << std::endl;
            }
        }
            break;
        case Op_TauArrayLen:
        case Op_LdConstant: case Op_LdStatic: case Op_TauLdInd:
        case Op_TauLdField: case Op_TauLdElem:
        case Op_DirectCall: case Op_TauVirtualCall: case Op_IndirectCall:
        case Op_IndirectMemoryCall: case Op_JitHelperCall: case Op_VMHelperCall:
        case Op_DefArg:
            // All these load instructions may potentially produce an operand
            // that would be a parameter to a newarray, hence we need it to be
            // reachable with inequality edges. Need to keep it constrained.
            addOldOrCreateOpnd(inst->getDst());
            break;
        case Op_TauStInd: case Op_TauStElem: case Op_TauStField:
        case Op_TauStRef: case Op_TauStStatic:
            break;
        default:
            addOldOrCreateOpnd(inst->getDst())->setUnconstrained(true);
            break;
    }
}
//------------------------------------------------------------------------------

void BuildInequalityGraphWalker::finishNode(DominatorNode *domNode)
{
    OpndToPiInst2ElemMap::const_iterator it = _map_opnd_to_pi_inst.begin(),
        end = _map_opnd_to_pi_inst.end();
    for (; it != end; it++ ) {
        TauPiInst* pi_inst = it->second;
        const PiCondition* condition = pi_inst->getCond();
        const PiBound& lb = condition->getLb();
        const PiBound& ub = condition->getUb();
        IOpndProxy* dst = findProxy(pi_inst->getDst());
        assert(dst);
        /*
         * pi (src0 \in [undef,A + c] -) dst
         *      dst <= A + c <-> (dst - A) <= c
         *      edge(from:A, to:dst, c)
         *
         * pi (src0 \in [A + c,undef] -) dst
         *      (A + c) <= dst <-> (A - dst) <= -c
         *      edge(from:dst, to:A, -c)
         */
        bool lb_defined = !lb.isUndefined();
        bool ub_defined = !ub.isUndefined();
        if ( lb_defined ) {
            addPiEdgesSingleProblem(dst, true /* lower_problem */, lb);
        }
        if ( ub_defined ) {
            addPiEdgesSingleProblem(dst, false /* lower_problem */, ub);
        }
    }
}

// returns true if the edge is actually added
bool BuildInequalityGraphWalker::addDistance
     (IOpndProxy* dst, IOpndProxy* src, int64 constant, bool negate)
{
    assert(dst && src);
    // This prevention to put some edges is *not* an optimization of any kind.
    // Operands can be marked unconstrained by various reasons, for example:
    // because the constant value does not fit into I_32 (which is critical for
    // array access)
    if ( !src->isUnconstrained() ) {
        if ( !inInt32(constant) ) {
            return false;
        }
        if ( negate ) {
            constant = (-1) * constant;
        }
        _igraph->addEdge(src->getID(), dst->getID(), (I_32)constant);
        return true;
    } else {
        if ( Log::isEnabled() ) {
            Log::out() << "addDistance(): skipping edge, operand is unconstrained: ";
            src->printName(Log::out());
            Log::out() << std::endl;
        }
    }
    return false;
}
//------------------------------------------------------------------------------

void BuildInequalityGraphWalker::addDistanceSingleProblem
     (IOpndProxy* to, IOpndProxy* from, int64 c, bool lower_problem)
{
    assert(to && from);
    if ( from->isUnconstrained() || !inInt32(c) ) {
        return;
    }
    _igraph->addEdgeSingleState(from->getID(), to->getID(), (I_32)c, lower_problem);
}
//------------------------------------------------------------------------------

// returns true if an edge to const opnd is actually added
bool BuildInequalityGraphWalker::addEdgeIfConstOpnd
    (IOpndProxy* dst, Opnd* const_src, Opnd* src, bool negate_src)
{
    if ( ConstantFolder::isConstant(const_src) ) {
        IOpnd* from = findProxy(const_src);
        assert(from);
        if ( !from->isUnconstrained() ) {
            return addDistance(dst, findProxy(src), from->getConstant(), 
                               negate_src);
        }
    }
    return false;
}
//------------------------------------------------------------------------------

void BuildInequalityGraphWalker::addPiEdgesSingleProblem
     (IOpndProxy* dst, bool lower_problem, const PiBound& non_inf_bound)
{
    if ( non_inf_bound.isVarPlusConst()  ) {
        Opnd* var = non_inf_bound.getVar().the_var;
        IOpndProxy* var_proxy = findProxy(var);
        assert(var_proxy);
        if ( _map_opnd_to_pi_inst.count(var_proxy) == 0 ) {
            addDistanceSingleProblem(dst /* to */,
                                  var_proxy /* from */,
                                  non_inf_bound.getConst(),
                                  lower_problem);
        }else{
            TauPiInst* pi_inst = _map_opnd_to_pi_inst[var_proxy];
            IOpndProxy* newer_var_proxy = findProxy(pi_inst->getDst());
            assert(newer_var_proxy);
            addDistanceSingleProblem(dst /* to */,
                                  newer_var_proxy /* from */,
                                  non_inf_bound.getConst(),
                                  lower_problem);
        }
    } else if ( non_inf_bound.isConst() ) {
        MemoryManager& mm = _igraph->getMemoryManager();
        IOpndProxy* c_opnd = new (mm) 
            IOpndProxy((I_32)non_inf_bound.getConst(), _const_id_counter++);
        _igraph->addOpnd(c_opnd);
        addDistanceSingleProblem(dst /* to */, c_opnd, 0, lower_problem);
    }
}
//------------------------------------------------------------------------------

IOpndProxy* BuildInequalityGraphWalker::addOldOrCreateOpnd(Opnd* opnd)
{
    IOpndProxy* proxy = findProxy(opnd);
    if ( !proxy ) {
        MemoryManager& mm = _igraph->getMemoryManager();
        proxy = new (mm) IOpndProxy(opnd);
        _igraph->addOpnd(proxy);
        if ( Log::isEnabled() ) {
            Log::out() << "added opnd: ";
            proxy->printFullName(Log::out());
            Log::out() << std::endl;
        }
    }
    return proxy;
}

class InequalityGraphPrinter : public PrintDotFile {
public:
    InequalityGraphPrinter(InequalityGraph& graph) : _graph(graph) {}
    void printDotBody()
    {
        _graph.printDotBody(*os);
    }
private:
    InequalityGraph& _graph;
};
//------------------------------------------------------------------------------

ClassicAbcd::ClassicAbcd(SessionAction* arg_source, IRManager &ir_manager,
        MemoryManager& mem_manager, DominatorTree& dom0) :
    _irManager(ir_manager), 
    _mm(mem_manager),
    _domTree(dom0),
    _redundantChecks(mem_manager),
    _zeroIOp(NULL),
    _dump_abcd_stats(ir_manager.getOptimizerFlags().dump_abcd_stats)
{
    _runTests = arg_source->getBoolArg("run_tests", false);
    _useAliases = arg_source->getBoolArg("use_aliases", true);
    _zeroIOp = new (mem_manager) IOpndProxy(0, 0 /*using reserved ID*/);
}
//------------------------------------------------------------------------------

void ClassicAbcd::updateOrInitValue
     (InstRedundancyMap& map, Inst* inst, RedundancyType type)
{
    if ( map.count(inst) == 0 ) {
        map[inst] = type;
    }else{
        I_32 new_rtype = (I_32)map[inst];
        new_rtype |= (I_32)type;
        map[inst] = (RedundancyType)new_rtype;
    }
}
//------------------------------------------------------------------------------

void ClassicAbcd::markRedundantInstructions
     (bool upper_problem, InequalityGraph& igraph, ControlFlowGraph& cfg)
{
    ClassicAbcdSolver solver(igraph, igraph.getMemoryManager());
    igraph.setState(!upper_problem /* is_lower */);

    for (Nodes::const_iterator i = cfg.getNodes().begin(); 
            i != cfg.getNodes().end(); 
            ++i) {
        Node *curr_node = *i;

        for (Inst *curr_inst = (Inst*)curr_node->getFirstInst();
             curr_inst != NULL; curr_inst = curr_inst->getNextInst()) {

            if (curr_inst->getOpcode() == Op_TauCheckBounds) {
                assert(curr_inst->getNumSrcOperands() == 2);
                if (Log::isEnabled()) {
                    Log::out() << "Trying to eliminate CheckBounds instruction ";
                    curr_inst->print(Log::out());
                    Log::out() << std::endl;
                }

                Opnd *idxOp = curr_inst->getSrc(1);
                IOpnd *idxIOp = igraph.findOpnd(IOpndProxy::getProxyIdByOpnd(idxOp));

                IOpnd *boundsIOp = _zeroIOp;
                if ( upper_problem ) {
                    Opnd *boundsOp = curr_inst->getSrc(0);
                    boundsIOp = igraph.findOpnd(IOpndProxy::getProxyIdByOpnd(boundsOp));
                }
                bool res = solver.demandProve
                           (boundsIOp, idxIOp, upper_problem ? -1 : 0, upper_problem);
                if (res) {
                    RedundancyType rt = upper_problem ? rtUPPER_MASK : rtLOWER_MASK;
                    updateOrInitValue(_redundantChecks, curr_inst, rt);
                    if (Log::isEnabled()) {
                        Log::out() << "can eliminate";
                        if ( upper_problem ) {
                            Log::out() << " upper ";
                        }else{
                            Log::out() << " lower ";
                        }
                        Log::out() << "bound check!\n";
                    }
                }else{
                    updateOrInitValue(_redundantChecks, curr_inst, rtNONE_MASK);
                }
            }
        }
    }
}
//------------------------------------------------------------------------------

void ClassicAbcd::runPass()
{
    static bool run_once = true;
    if ( run_once && _runTests ) {
        classic_abcd_test_main();
        _runTests = false;
        run_once = false;
    }

    MethodDesc& method_desc  = _irManager.getMethodDesc();
    ControlFlowGraph& cfg    = _irManager.getFlowGraph();
    TypeManager& typeManager = _irManager.getTypeManager();
    OpndManager& opndManager = _irManager.getOpndManager();
    InstFactory& instFactory = _irManager.getInstFactory();

    if ( Log::isEnabled() ) {
        FlowGraph::printDotFile(cfg, method_desc, "before_classic_abcd");
        _domTree.printDotFile(method_desc, "before_classic_abcd.dom");
        Log::out() << "ClassicAbcd pass started" << std::endl;
    }

    MemoryManager ineq_mm("ClassicAbcd::InequalityGraph");
    InsertPi insertPi(ineq_mm, _domTree, _irManager, _useAliases);
    insertPi.insertPi();
    InequalityGraph igraph(ineq_mm);
    igraph.addOpnd(_zeroIOp);
    BuildInequalityGraphWalker igraph_walker(&igraph);
    typedef ScopedDomNodeInst2DomWalker<true, BuildInequalityGraphWalker>
        IneqBuildDomWalker;
    IneqBuildDomWalker dom_walker(igraph_walker);
    DomTreeWalk<true, IneqBuildDomWalker>(_domTree, dom_walker, ineq_mm);

    if ( Log::isEnabled() ) {
        Log::out() << "added zero opnd for solving lower bound problem: ";
        _zeroIOp->printFullName(Log::out());
        Log::out() << std::endl;
        InequalityGraphPrinter printer(igraph);
        printer.printDotFile(method_desc, "inequality.graph");
    }

    _redundantChecks.clear();
    markRedundantInstructions(true /* upper_problem */, igraph, cfg);
    markRedundantInstructions(false /* upper_problem */, igraph, cfg);

    insertPi.removePi();

    U_32 checks_eliminated = 0;

    for(InstRedundancyMap::const_iterator i = _redundantChecks.begin();
        i != _redundantChecks.end(); ++i) {
        Inst *redundant_inst = i->first;
        bool fully_redundant = (i->second == rtFULL_MASK);

        if (fully_redundant) {
            // should we check if another tau has already been placed in
            // this block, and if so reuse it?  Also, should we be using
            // taupoint or tauedge?
            Opnd *tauOp = opndManager.createSsaTmpOpnd(typeManager.getTauType());
            Inst* tau_point = instFactory.makeTauPoint(tauOp);
            tau_point->insertBefore(redundant_inst);

            if (Log::isEnabled()) {
                Log::out() << "Inserted taupoint inst ";
                tau_point->print(Log::out());
                Log::out() << " before inst ";
                redundant_inst->print(Log::out());
                Log::out() << std::endl;
            }

            Opnd* dstOp = redundant_inst->getDst();
            redundant_inst->setDst(OpndManager::getNullOpnd());
            Inst* copy = instFactory.makeCopy(dstOp, tauOp);
            copy->insertBefore(redundant_inst);
            FlowGraph::eliminateCheck(cfg, redundant_inst->getNode(), redundant_inst, false);
            checks_eliminated++;

            if (Log::isEnabled()) {
                Log::out() << "Replaced bound check with inst ";
                copy->print(Log::out());
                Log::out() << std::endl;
            }
        }
    }

    size_t checks_total = _redundantChecks.size();
    if ( _dump_abcd_stats && checks_total > 0 ) {
        std::ofstream checks_log;
        checks_log.open("bounds_checks.log", std::fstream::out | std::fstream::app);
        checks_log << "removed bounds checks of: "
            << method_desc.getParentType()->getName()
            << "." << method_desc.getName()
            << method_desc.getSignatureString()
            << " total checks: " << checks_total
            << "; eliminated: " << checks_eliminated
            << "; fraction: "
            << (double) checks_eliminated / (double) checks_total
            << std::endl;
        checks_log.close();
    }

    Log::out() << "ClassicAbcd pass finished" << std::endl;
}
//------------------------------------------------------------------------------

} //namespace Jitrino 

