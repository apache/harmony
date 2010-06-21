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
 * @author Sergey L. Ivashin
 */

#include "Ia32IRManager.h"
#include "Ia32RegAllocCheck.h"
#include "Interval.h"
#include "Stl.h"
#include "Log.h"

#ifdef _DEBUG__REGALLOC2
#include "Ia32RegAllocWrite.h"
#include <iostream>
#include <iomanip>
#endif

#define _SKIP_CATCHED

namespace Jitrino
{

namespace Ia32
{


//========================================================================================
// class Ia32RegAlloc2
//========================================================================================

/**
 *  This class attempts to assign register for any operand (found in LIR) that can be 
 *  allocated in register.
 *
 *  Set of registers available for allocation is specified by input arguments and saved 
 *  in 'constrs' class member. All operands that cannot be allocated in the registers 
 *  available are simply ignored.
 *
 *  So this allocator should be called for each set of the registers available (GPReg, XMM, 
 *  FP) independently.
 *
 *  It is not guaranteed that all operands which can be assigned will be assigned. 
 *  Therefore, the companion class (SpillGen) must be used after this allocator.
 *
 *  The main idea behind the allocator is well-known bin packing algorithm, but this 
 *  implementation is completely independent of any published algorithms.
 */

static const char* help = 
"  regs=ALL_GP\n"
"  regs=ALL_XMM\n"
"  regs=<comma-separated list of available registers, e.g EAX,ECX,XMM0,>\n"
;


struct RegAlloc2 : public SessionAction
{
    typedef U_32 RegMask;     // used to represent set of registers
    typedef size_t Instnb;      // used to describe instruction number

    typedef Interval::Span Span;        // interval of instructions not crossed basic block boundary
    struct Register;            // holds all operands assigned to this register

    // class RegAlloc2::Opand
    struct Opand : public Interval
    {
        Opnd* opnd;             // 0 for fictious (preassigned) operand or pointer to LIR
        Register* assigned;     // 0 or register assigned
        
        size_t length;          // total length of all intervals
        double weight;          // weight coeff (taken from LIR)

        Opand* parentOpand;     // another opand which is a source in a copy instruction defining this opand, hint for coalescing
        bool   ignore;

        Opand (MemoryManager& mm)                           :Interval(mm) {}

        void update ();

        //static bool smaller (const RegAlloc2::Opand*& x, const RegAlloc2::Opand*& y)
        //{   return x->end - x->beg < y->end - y->beg;   }

        static bool lighter (const RegAlloc2::Opand* x, const RegAlloc2::Opand* y)
        {   return x->weight < y->weight;   }

    };

    MemoryManager mm;           // this is private MemoryManager, not irm.getMemoryManager()
    size_t opandcount;          // total count of operands in LIR
    Constraint constrs;         // initial constraints (registers available)

    typedef StlVector<Register*> Registers;
    Registers registers;

    typedef StlVector<Opand*> Opands;
    Opands opandmap;            // mapping Ia32Opnd.id -> Opand*
    size_t candidateCount;

    void buildRegs ();
    void buildOpands ();
    void allocateRegs ();
    Register* findReg (RegMask) const;


    RegAlloc2 ()                    :mm("RegAlloc2"), registers(mm), opandmap(mm) {}

    U_32 getNeedInfo () const     {return NeedInfo_LivenessInfo;}
    U_32 getSideEffects () const  {return 0;}

    void runImpl();
    bool verify(bool force=false);
};


static ActionFactory<RegAlloc2> _bp_regalloc("bp_regalloc", help);


static Counter<size_t> count_spilled("ia32:regalloc2:spilled", 0),
                       count_assigned("ia32:regalloc2:assigned", 0);


#ifdef _DEBUG_REGALLOC
struct Dbgout : public  ::std::ofstream
{
    Dbgout (const char* s)          {open(s);}
    ~Dbgout ()                      {close();}
};

static Dbgout dbgout("regalloc2.txt");
#define DBGOUT(x) dbgout << x;

#else

#define DBGOUT(x) 

#endif //#ifdef _DEBUG_REGALLOC

//========================================================================================
// class RegAlloc2::Register
//========================================================================================

struct RegAlloc2::Register
{
    RegMask regmask;        // mask to identify this register (search key)
    RegName regname;
    Constraint constraint;  // corresponding to this register

    Opand  preassigned;     // fictious operand to describe assignments by code selector

    typedef StlList<Opand*> Opands;
    Opands assigned;        // all operands assigned to this register by allocator

    size_t length;          // total length of intervals of all operands

    Instnb beg, end;        // start point of first interval and end point of last interval of all assigned operands

    Register (MemoryManager& mm)                        :preassigned(mm), assigned(mm) {}

    bool canBeAssigned (const Opand*, int&) const;
    void assign (Opand*);
};


//========================================================================================
// Internal debug helpers
//========================================================================================

using ::std::endl;
using ::std::ostream;


#ifdef _DEBUG_REGALLOC
static ostream& operator << (ostream& os, const RegAlloc2::Opand& x)
{
    os << "Opand{";

    if (x.opnd != 0)
        os << "#" << x.opnd->getFirstId()<<"("<<x.opnd->getId()<<")";
    else
        os << "~";

    os << " W:" << x.weight << " L:" << x.length;
    os << " beg:" << x.beg << " end:" << x.end << " " << x.spans ;

    if (x.assigned != 0)
        os << " in " << getRegNameString(x.assigned->regname);

    return os << "}";
}

static ostream& operator << (ostream& os, const RegAlloc2::Register& x)
{
    return os << "Reg{" << getRegNameString(x.regname) << " L:" << x.length << " beg:" << x.beg << " end:" << x.end << "}";
}
#endif


//  Helper function for update()
//
static bool less (const Interval::Span& x, const Interval::Span& y) 
{
    return x.beg < y.end; // to order properly spans like [124,130] [124,124] although there should not be such things
}

//  Complete liveness calculation - sort intervals and set endpoints
//
void RegAlloc2::Opand::update ()
{
    if ( opnd != NULL ){  // pre-assigned operands are not candidates anyway, no need to update their weight
        Constraint c = opnd->getConstraint(Opnd::ConstraintKind_Calculated, OpndSize_Default);
        weight *= (17 - countOnes(c.getMask())) * ( (c & Constraint(OpndKind_Mem, c.getSize())).isNull() ? 2 : 1);
    }
    if (spans.size() != 0)
    {
        Span * pspans = &spans.front();
        sort(spans.begin(), spans.end(), less);
        beg = spans.front().beg,
        end = spans.back().end;
        length=0;
        for (int i=0, n=(int)spans.size(); i<n; i++){
            assert(pspans[i].beg != 0);
            length += pspans[i].end - pspans[i].beg; // no +1 as this is the number of instructions "hovered" by this opand
#ifdef _DEBUG
            assert(pspans[i].beg <= pspans[i].end);
            if ( i < n-1 ){
                assert(pspans[i].end < pspans[i+1].beg);
            }
#endif
        }
        assert( opnd == NULL || beg > 0);
        assert(beg <= end);
    }
    else
    {
        //assert(0);
        ignore = true;
    }
}

//========================================================================================
// class Ia32RegAlloc2::Register
//========================================================================================


//  Test if this register can be assigned to the operand 'x'
//
bool RegAlloc2::Register::canBeAssigned (const RegAlloc2::Opand* x, int& adj) const
{
    DBGOUT("    try " << *this;)

    if (!x->opnd->canBePlacedIn(constraint))
    {
        DBGOUT(" -constraint" << endl;)
        return false;
    }

    if (preassigned.conflict(x, adj))
    {
        DBGOUT(" -preassigned" << endl;)
        return false;
    }

    adj = 0;

    int d;
    if ( (d = int(x->end - beg)) < 0 ){
        if (d==-1)
            adj=1;
        DBGOUT(" -free (below lower bound)!" << endl;)
        return true;
    }
    if ( (d = int(x->beg - end)) > 0 ){
        if (d==1)
            adj=1;
        DBGOUT(" -free (above upper bound)!" << endl;)
        return true;
    }
        
    for (Opands::const_iterator i = assigned.begin(); i != assigned.end(); ++i)
        if ((*i)->conflict(x, adj))
        {
            DBGOUT(" -conflict " << **i << endl;)
            return false;
        }

    DBGOUT(" -free (full check)!" << endl;)
    return true;
}


//  Assign opand 'x' to this register
//
void RegAlloc2::Register::assign (RegAlloc2::Opand* x) 
{
    assigned.push_back(x);
    x->assigned = this;
    if (x->beg<beg)
        beg=x->beg;
    if (x->end>end)
        end=x->end;
    length += x->length;
}

//========================================================================================
// class Ia32RegAlloc2
//========================================================================================


void RegAlloc2::runImpl()
{
#ifdef _DEBUG_REGALLOC
    MethodDesc& md=irManager->getMethodDesc();
    const char * methodName = md.getName();
    const char * methodTypeName = md.getParentType()->getName();
    const char * methodSignature= md.getSignatureString();
    dbgout << "Constructed <" << methodTypeName << "::" << methodName << methodSignature << ">" << endl;
#endif

    irManager->fixEdgeProfile();

    const char* parameters = getArg("regs");
    assert(parameters);
    if (strcmp(parameters, "ALL_GP") == 0)

#ifdef _EM64T_
        constrs = Constraint(RegName_R8)
                 |Constraint(RegName_RAX)
                 |Constraint(RegName_RDX)
                 |Constraint(RegName_RCX)
                 |Constraint(RegName_RBX)
                 |Constraint(RegName_RSI)
                 |Constraint(RegName_RDI)
                 |Constraint(RegName_RBP)
                 |Constraint(RegName_R9)
                 |Constraint(RegName_R10)
                 |Constraint(RegName_R11)
                 |Constraint(RegName_R12);
#else
        constrs = Constraint(RegName_EAX)
                 |Constraint(RegName_ECX)
                 |Constraint(RegName_EDX)
                 |Constraint(RegName_EBX)
                 |Constraint(RegName_ESI)
                 |Constraint(RegName_EDI)
                 |Constraint(RegName_EBP);
#endif

    else if (strcmp(parameters, "ALL_XMM") == 0)
        constrs = Constraint(RegName_XMM1)
                 |Constraint(RegName_XMM0)
                 |Constraint(RegName_XMM2)
                 |Constraint(RegName_XMM3)
                 |Constraint(RegName_XMM4)
                 |Constraint(RegName_XMM5)
                 |Constraint(RegName_XMM6)
                 |Constraint(RegName_XMM7);

    else
    {
        constrs = Constraint(parameters);
        constrs.intersectWith(Constraint(OpndKind_Reg, Constraint::getDefaultSize(constrs.getKind())));
    }
    assert(!constrs.isNull());

    buildRegs();

    buildOpands();

    if (candidateCount == 0)
        return;

    allocateRegs();

#ifdef _DEBUG_REGALLOC
        dbgout << endl << "Result" << endl;

        int count1 = 0, count2 = 0;

        Opand* opand;
        for (size_t i = 0; i != opandmap.size(); ++i)
            if ((opand = opandmap[i]) != 0 && opand->opnd != 0)
            {
                dbgout << "  " << i << ") " << *opand << endl;
                if (opand->assigned != 0)
                {
                    dbgout << "    +assigned " << *opand->assigned << endl;
                    ++count1;
                }
                else
                {
                    dbgout << "    -spilled " << endl;
                    ++count2;
                }
            }

        dbgout << endl << "Registers" << endl;
        for (Registers::const_iterator it = registers.begin(); it != registers.end(); ++it)
        {
            Register& r = **it;
            dbgout << r;
    
            if (!r.preassigned.spans.empty())
                dbgout << " preassigned:" << r.preassigned.spans;
    
            if (!r.assigned.empty())
            {
                dbgout << " assigned:";
                for (Register::Opands::const_iterator kt = r.assigned.begin(); kt != r.assigned.end(); ++kt)
                    dbgout << (*kt)->spans;
            }
            dbgout << endl;
        }

        dbgout << endl << "Assigned: " << count1 << "  spilled: " << count2 << endl;

    dbgout << "Destructed" << endl;
#endif
}

bool RegAlloc2::verify (bool force)
{   
    bool failed=false;
    if (force||getVerificationLevel()>=2){
        RegAllocCheck chk(*irManager);
        if (!chk.run(false))
            failed=true;
        if (!SessionAction::verify(force))
            failed=true;
    }
    return !failed;
}   

//  Initialize list of all available registers
//
void RegAlloc2::buildRegs ()    
{
    DBGOUT(endl << "buildRegs" << endl;)

    OpndKind k = (OpndKind)constrs.getKind(); 
    OpndSize s = constrs.getSize();

    registers.clear();
    RegMask mask = constrs.getMask();
    for (RegMask m = 1, x = 0; mask != 0; m <<= 1, ++x)
        if ((mask & m))
        {   
            mask ^= m;
            Register* r = new (mm) Register(mm);
            r->regmask = m,
            r->regname = getRegName(k, s, x);
            r->constraint = Constraint(k, s, m);
            r->length = 0;
            r->beg=(Instnb)UINT_MAX;
            r->end=(Instnb)0;

            Opand& op = r->preassigned;
            op.opnd   = 0;  // because this is a fictious operand
            op.length = 0;
            op.weight = 0;
            op.assigned = r;
            op.parentOpand = 0;
            registers.push_back(r);
        }
}


RegAlloc2::Register* RegAlloc2::findReg (RegMask mask) const
{
    for (Registers::const_iterator it = registers.begin(); it != registers.end(); ++it)
        if ((*it)->regmask == mask)
            return *it;

    return 0;
}


void RegAlloc2::buildOpands ()  
{
    DBGOUT(endl << "buildOpands" << endl;)

    opandcount = irManager->getOpndCount();
    candidateCount = 0;
    int registerPressure = 0;
    opandmap.clear();
    opandmap.reserve(opandcount);

    for (U_32 i = 0; i != opandcount; ++i)
    {
        Opnd*  opnd  = irManager->getOpnd(i);
        Opand* opand = 0;

        if (opnd->isPlacedIn(constrs))
        {
            Register* r = findReg(getRegMask(opnd->getRegName()));
            if (r != 0)
                opand = &r->preassigned;
        }

        else if (opnd->isAllocationCandidate(constrs))
        {
            opand = new (mm) Opand(mm);
            opand->opnd = opnd;
            opand->length = 0;
            opand->weight = 0;
            opand->assigned = 0;
            opand->parentOpand = 0;
            opand->ignore = false;
            ++candidateCount;
        }

        opandmap.push_back(opand);
    }
    if (candidateCount == 0)
        return;

    //irManager->indexInsts();

    BitSet lives(mm, (U_32)opandcount);

    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode())
        {
            double execCount = node->getExecCount() / irManager->getFlowGraph()->getEntryNode()->getExecCount();
            assert(execCount > 0);

            const Inst*  inst  = (const Inst*)node->getLastInst();
            if (inst == 0)
                continue;

            U_32 instIndex=inst->getIndex();
            Opand * opand;

        //  start with the operands at the block bottom
            irManager->getLiveAtExit(node, lives);
            BitSet::IterB ib(lives);
            for (int x = ib.getNext(); x != -1; x = ib.getNext())
                if ( (opand=opandmap[x]) != 0 ){
                    if (opand->startOrExtend(instIndex + 1))
                        ++registerPressure;
                }

        //  iterate over instructions towards the top of the block

            for (; inst != 0; inst = inst->getPrevInst())
            {
                instIndex=inst->getIndex();
                const Opnd* opnd;
                Opand* definedInCopyOpand = 0;
                Inst::Opnds opnds(inst, Inst::OpndRole_All);
                for (Inst::Opnds::iterator it = opnds.begin(); it != opnds.end(); it = opnds.next(it)){
                    opnd = opnds.getOpnd(it);
                    if ( (opand=opandmap[opnd->getId()]) != 0 )
                    {
                        opand->weight += (execCount * (registerPressure > (int)registers.size() ? 4 : 1));

                        if (inst->isLiveRangeEnd(it)){
                            opand->stop(instIndex + 1);
                            --registerPressure;
                            if (inst->getMnemonic() == Mnemonic_MOV)
                                definedInCopyOpand = opand;
                        }else{
                            if (opand->startOrExtend(instIndex)){
                                ++registerPressure;
                                if ( definedInCopyOpand != 0 && inst->getMnemonic() == Mnemonic_MOV && definedInCopyOpand->parentOpand == 0){
                                    definedInCopyOpand->parentOpand = opand;
                                }
                            }
                        }
                    }
                    if (registerPressure<0) // if there were dead defs
                        registerPressure=0;
                }
            }

        //  process operands at the top of the block
            inst = (Inst*)node->getFirstInst();
            instIndex=inst->getIndex();
            BitSet* tmp = irManager->getLiveAtEntry(node);

            ib.init(*tmp);
            for (int x = ib.getNext(); x != -1; x = ib.getNext())
                if ( (opand=opandmap[x]) != 0 ){
                    opand->stop(instIndex);
                    --registerPressure;
                }

            if (registerPressure<0) // TODO: why?
                registerPressure=0;
        }

#ifdef _SKIP_CATCHED
        else if (node->isDispatchNode())
        {
            BitSet* tmp = irManager->getLiveAtEntry(node);
            BitSet::IterB ib(*tmp);
            Opand* opand;
            for (int x = ib.getNext(); x != -1; x = ib.getNext())
                if ( (opand=opandmap[x]) != 0 )
                {
                    opand->ignore = true;
                    DBGOUT("catched " << *opand << endl;)
                }
        }
#endif
    }

//  for each operand, sort all its intervals (spans)

    for (Opands::iterator it = opandmap.begin(); it != opandmap.end(); ++it)
        if (*it != 0)
            (*it)->update();

    for (Opands::iterator it = opandmap.begin(); it != opandmap.end(); ++it){
        Opand * opand = *it;
        if (opand != 0){
            int adj;
            if (opand->parentOpand != 0 &&
                opand->length < opand->parentOpand->length &&
                opand->length < 3 && 
                !opand->conflict(opand->parentOpand, adj)
                ){
                // can coalesce
                if (opand->weight >= opand->parentOpand->weight){
                    opand->weight = opand->parentOpand->weight; // make sure it is handled after its parent operand
                    ++opand->parentOpand->weight;
                }
            }
        }
    }

#ifdef _DEBUG_REGALLOC
        dbgout << endl << "opandmap" << endl;
        for (Opands::iterator it = opandmap.begin(); it != opandmap.end(); ++it)
        {
            dbgout << "  "; 
            if (*it != 0)
                dbgout << **it;
            else
                dbgout << "-";
            dbgout << endl;
        }
#endif
}

void RegAlloc2::allocateRegs () 
{
    DBGOUT(endl << "allocateRegs" << endl;)

    Opands opands(mm);
    opands.reserve(opandmap.size());

    Opand* nxt;
    for (Opands::const_iterator i = opandmap.begin(); i != opandmap.end(); ++i){
        if ((nxt = *i) != 0 && nxt->assigned == 0 && !nxt->ignore)
            opands.push_back(nxt);
    }

    sort(opands.begin(), opands.end(), Opand::lighter);

    for (Opands::reverse_iterator i = opands.rbegin(); i != opands.rend(); ++i)
    {
        nxt = *i;

        DBGOUT("  nxt " << *nxt << endl;)

    //  try to find free regsiter for opand 'nxt'
        Register* better = 0;
        Register* best   = 0;
        int bestadj = 0, adj=0;

        for (Registers::iterator k = registers.begin(); k != registers.end(); ++k)
            if ((*k)->canBeAssigned(nxt, adj))
            {
                DBGOUT("    found " << **k << " adj:" << adj << endl;)

                if (better == 0 || better->length < (*k)->length)
                    better = *k;

                if (adj != 0)
                    if (best == 0 || adj > bestadj)
                        best = *k, bestadj = adj;
            }

        if (best == 0)
            best = better; 

        if (best != 0)
        {
            DBGOUT("    assigned " << *best << endl;)
            best->assign(nxt);
            nxt->opnd->assignRegName(best->regname);
            ++count_assigned;
        }
        else
            ++count_spilled;
    }
}


} //namespace Ia32
} //namespace Jitrino
