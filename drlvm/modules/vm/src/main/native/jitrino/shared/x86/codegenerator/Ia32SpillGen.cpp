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
#include "XTimer.h"
#include "Counter.h"
#include "Stl.h"
#ifdef PLATFORM_POSIX
#include <limits.h>
#endif

#ifdef _DEBUG_SPILLGEN
#include "Ia32SpillGenDbg.h"
#endif //#ifdef _DEBUG_SPILLGEN


namespace Jitrino
{
namespace Ia32
{

static const char* help = 
"  regs=ALL\n"
"  regs=<comma-separated list of available registers, e.g EAX,ECX,XMM0,>\n"
;


struct SpillGen : public SessionAction
{

//  Inter-blocks data
//  -----------------

    typedef U_32 RegMask;
    typedef StlVector<Opnd*> Opnds;

    MemoryManager mm;           // this is private MemoryManager, not irm.getMemoryManager()

    // Table of all available registers sets.
    // Each set (GPReg, FPReg, XMMReg and so on) is represented by the corresponding 
    // Constraint object.
    struct Registers : public StlVector<Constraint>
    {
        Registers (MemoryManager& mm)       :StlVector<Constraint>(mm) {}

        void  parse (const char*);

        // clear mask fields in all elements of the table
        void clearMasks ();

        void merge (const Registers&);

        // register the new constraint (register) in the table.
        // if table doesn't contain constraint of the specified kind, it will be ignored
        // (add = false) or new table entry for the constraint will be created (add = true).
        int merge (const Constraint&, bool add = false);

        // returns table index of the constraint of the specified kind or -1
        int getIndex (const Constraint&) const;

        // returns pointer to the table element of the specified kind
        Constraint* contains (const Constraint&);

        int indexes[IRMaxRegKinds];
    };
    Registers registers;

    size_t  opndcount;          // initial count of operands
    size_t  emitted;            // extra instructions count
    size_t  fails;              // if != 0, then SpillGen should be aborted

    // mapping operand -> its memory location
    // every operand that was splitted to memory is registered in this table
    typedef ::std::pair<const Opnd*, Opnd*> TwoOpnds;
    typedef StlVector<TwoOpnds> MemOpnds;
    MemOpnds memopnds;


//  Block-specific data
//  -------------------

    enum 
    {
        RoleDef  = 1,
        RoleUse  = 2,
        RoleCond = 4,   // if (RoleCond == true) RoleUse = true; 
        RoleEnd  = 8
    };
    typedef char OpRole;

    // extension of 'Inst' structure with some specific data
    struct Instx
    {
        Instx ()                : nbopnds(0), evicts(0) {}

        Inst*   inst;               // pointer to the corresponding instruction
        size_t  nbopnds;            // max number of operands in instruction
        OpRole* oproles;            // array of nbopnds size
        Opnds*  evicts;             // can be null, can contain null pointers 
        RegMask regusage[IRMaxRegKinds];// masks of all registers that are busy at this instruction
        RegMask regpress[IRMaxRegKinds];
    };

    typedef StlVector<Instx> Instxs;
    Instxs  instxs;

    // created for all problem (needed to be allocated) operands    

    struct Op
    {
        Instx* instx;
        OpRole oprole;
    };


    struct Opline
    {
        Opline ()               : ops(0) {}

        void clear (MemoryManager&);
        void addOp (Instx*, OpRole);

    //  iterator methods, work with the current instruction only
        void start ();
        void  forw ();
        void  back (const Instx*);
        bool    go () const     {return op != 0;}
        bool isDef () const     {return op->instx == instx && (op->oprole & RoleDef) != 0;}
        bool isUse () const     {return op->instx == instx && (op->oprole & RoleUse) != 0;}
        bool isEnd () const     {return op->instx == instx && (op->oprole & RoleEnd) != 0;}
        bool isProc() const     {return op->instx == instx;}

    //  work with the specified instruction
        bool isDef (const Inst*) const;
        bool isUse (const Inst*) const;


        bool at_start,          // 'true' if this operand lives at the BB entry
            at_exit,            // 'true' if this operand lives at the BB exit
            catched;            // 'true' if this operand lives at entry to dispatch node
        int    idx;             // -1 or index of the corresponding constraint in 'registers' array
        Opnd*  opnd;            // original form of operand
        Opnd*  opnd_mem;        // 0 or memory form of the operand (cached copy of the value from 'memopnds' array)
        Instx*  save_instx;     // 0 or point where instruction to save operand should be inserted
        bool    save_before;    // true if save must be before instruction (false is the default)
        Opnd*   save_opnd;      // which operand should be saved
        RegMask save_regmsk;
        int     save_regidx;
        bool    save_changed;

        typedef StlVector<Op> Ops;
        Ops* ops;               // array of all instructions that change/use value of the operand

    //  current instruction
        Op*    op;
        Instx* instx;

        static bool smaller (const Opline& x, const Opline& y)      {return x.weight < y.weight;}

        Constraint initial;
        int weight;
    };


    typedef StlVector<Opline> Oplines;
    Oplines actives;
    StlVector<Opline*> actsmap;

    struct  Evict;
    typedef StlVector<Evict> Evicts;

    // current context 
    Node* bblock;       // the basic block being processed
    BitSet* lives_start,    // liveness info for start and end of the block
           * lives_exit;    //
    BitSet* lives_catch;    // 0 or mask of operands live at the corresponding dispatch node

    bool   evicts_known;

//  Methods
//  -------


    SpillGen ()                     : mm("SpillGen")
                                    , registers(mm), memopnds(mm)
                                    , instxs(mm), actives(mm), actsmap(mm)
                                    , lives_start(0), lives_exit(0)
                                    {}

    U_32 getNeedInfo () const     {return NeedInfo_LivenessInfo;}
    U_32 getSideEffects () const  {return SideEffect_InvalidatesLivenessInfo;}

    void runImpl();
    bool verify(bool force=false);

   size_t pass0 ();
   size_t pass1 ();
 RegMask  lookPreffered (Opline&);
    bool  tryRegister (Opline&, Constraint, RegMask);
    bool  tryMemory   (Opline&, Constraint);
    bool  tryEvict    (Opline&, Constraint);
    bool  tryRepair   (Opline&, Constraint);
    bool  simplify    (Inst*, Opnd*);
  RegMask usedRegs  (const Instx*, int idx, bool isuse) const;
  RegMask callRegs  (const Instx*, int idx) const;
    int   update      (const Inst*, const Opnd*, Constraint&) const;
    void  assignReg   (Opline&, Instx* begx, Instx* endx, RegName);
    void  assignMem   (Opline&, Instx* begx, Instx* endx);
    void  saveOpnd    (Opline&, Instx*, Opnd*);
    void  saveOpnd    (Opline&, Instx*, Opnd*, RegMask, int, bool);
    bool  loadOpnd    (Opline&, Instx*, Opnd*);
    void  loadOpndMem (Opline&);
    Opnd* opndMem     (Opline&);
    Opnd* opndMem     (const Opnd*);
    Opnd* opndReg     (const Opnd*, RegName) const;
    void  emitPushPop (bool before, Inst*, Opnd* opnd, bool push);
    Inst* emitMove    (bool before, Inst*, Opnd* dst, Opnd* src);
    RegName findFree  (RegMask usable, int idx, Instx* begx);
    RegName findEvict (RegMask usable, int idx, Instx* begx, Instx*& endx);
    void    setupEvicts ();
    Evict*  pickEvict (Evicts&);
    bool    isEvict   (const Opnd*,  const Instx*) const;
    void    killEvict (const Opnd*, Instx*) const;
    RegMask getRegMaskConstr (const Opnd*, Constraint) const;

};


static ActionFactory<SpillGen> _spillgen("spillgen", help);



#ifdef _DEBUG_SPILLGEN
#ifdef _MSC_VER
#pragma warning(disable : 4505)   //unreferenced local function has been removed
#else
#endif //#ifdef _MSC_VER
#include "Ia32SpillGenDbgHead.h"
#else
#define DBGOUT(s)
#endif //#ifdef _DEBUG_SPILLGEN




//  Utility class for (zero-terminated) strings parsing
//
class Tokens
{
public:

    Tokens (const char* s)          :src(s) {;}

    void init (const char* s)       {src = s;}
    bool scan ();
    bool isWord () const            {return isw;}
    const char* lex () const        {return buff;}

protected:

    const char* src;
    char* dst;
    char  buff[64];
    bool  isw;
};


//  Return 'false' if end of parsing string is reached and 'true' otherwise.
//      isWord() will specify was it word or separator (one character).
//      lex()    will return zero-terminated lexem string scanned (for word or separator).
//
bool Tokens::scan ()
{
    while (isspace(*src))
        src++;

    if (*src == 0)
        return false;

    dst = buff;
    isw = isalnum(*src) != 0;
    if (!isw)
        *dst++ = *src++;
    else
        while (isalnum(*src))
        {
            assert(dst != &buff[sizeof(buff)-1]);
            *dst++ = *src++;
        }

    *dst = 0;
    return true;
}


const int MAXREGS  = IRMaxRegNamesSameKind*IRMaxRegKinds;


//  Temporary structure to describe operand that can be evicted.
//  Used to choose the best operand to evict.
//
struct SpillGen::Evict
{
    Opnd*  opnd;
    Instx* begx, * endx;
    int weight;

    bool operator < (const Evict& x) const  {return weight < x.weight;}
};


////    Misc helpers
/////   ----------------------


static size_t bitCount (SpillGen::RegMask mk)
{
    size_t count = 0;
    while (mk != 0)
    {
        if ((mk & 1) != 0)
            ++count;
        mk >>= 1;
    }
    return count;
}


static void merge (Constraint& c, SpillGen::RegMask mk)
{
    c.setMask(c.getMask() | mk);
}


//  Parse input parameters (registers available) and build table of the regsiters
//  available for allocalion ('registers').
//
void SpillGen::Registers::parse (const char* params)
{
    if (params == 0 || strcmp(params, "ALL") == 0)
    {

#ifdef _EM64T_
        push_back(Constraint(RegName_R8)
                 |Constraint(RegName_RAX)
                 |Constraint(RegName_RCX)
                 |Constraint(RegName_RBX)
                 |Constraint(RegName_RDX)
                 |Constraint(RegName_RSI)
                 |Constraint(RegName_RDI)
                 |Constraint(RegName_R9)
                 |Constraint(RegName_R10)
                 |Constraint(RegName_R11)
                 |Constraint(RegName_R12));
#else
        push_back(Constraint(RegName_EAX)
                 |Constraint(RegName_ECX)
                 |Constraint(RegName_EDX)
                 |Constraint(RegName_EBX)
                 |Constraint(RegName_ESI)
                 |Constraint(RegName_EDI)
                 |Constraint(RegName_EBP));
#endif

        push_back(Constraint(RegName_XMM1)
                 |Constraint(RegName_XMM0)
                 |Constraint(RegName_XMM2)
                 |Constraint(RegName_XMM3)
                 |Constraint(RegName_XMM4)
                 |Constraint(RegName_XMM5)
                 |Constraint(RegName_XMM6)
                 |Constraint(RegName_XMM7));

        push_back(Constraint(RegName_FP0));
    }
    else
    {
        Constraint c;
        for (Tokens t(params); t.scan(); )
            if (t.isWord())
            {
                RegName r = getRegName(t.lex());
                if (r != RegName_Null)
                    c = Constraint(r);

                merge(c, true);
            }
    }

    assert(!empty());

    for (size_t i = 0; i != IRMaxRegKinds; ++i)
        indexes[i] = -1;

    for (size_t i = 0; i != size(); ++i)
        indexes[operator[](i).getKind()] = (int)i;
}


void SpillGen::Registers::clearMasks ()
{
    for (size_t i = 0; i != size(); ++i)
        operator[](i).setMask(0);
}


void SpillGen::Registers::merge (const SpillGen::Registers& x)
{
    for (size_t i = 0; i != size(); ++i)
    {
        Constraint& r = operator[](i);
        r.setMask(r.getMask() | x[i].getMask());
    }
}


int SpillGen::Registers::merge (const Constraint& c, bool add)
{
    if (c.getMask() != 0)
    {
        for (size_t i = 0; i != size(); ++i)
        {
            Constraint& r = operator[](i);
            if (r.getKind() == c.getKind())
            {
                r.setMask(r.getMask() | c.getMask());
                return (int)i;
            }
        }

        if (add)
            push_back(c);
    }

    return -1;
}


int SpillGen::Registers::getIndex (const Constraint& c) const
{
    return indexes[c.getKind() & OpndKind_Reg];
}


Constraint* SpillGen::Registers::contains (const Constraint& c) 
{
    for (size_t i = 0; i != size(); ++i)
    {
        Constraint& r = operator[](i);
        if (r.getKind() == c.getKind() && (r.getMask() & c.getMask()) != 0)
            return &r;
    }

    return 0;
}


void SpillGen::Opline::clear (MemoryManager& mm)
{
    if (ops == 0)
        ops = new (mm) Ops(mm);
    else
        ops->clear();

    opnd_mem = 0;
    save_instx = 0;
}


void SpillGen::Opline::addOp (Instx* instx, OpRole oprole)
{
    if (ops->empty() || ops->back().instx != instx)
    {
        Op tmp;
        tmp.instx  = instx;
        tmp.oprole = oprole;
        ops->push_back(tmp);
    }
    else
    {
        ops->back().oprole  |= oprole;
    }

    op = 0;
}


void SpillGen::Opline::start ()
{
    assert(!ops->empty());
    op    = &ops->back();
    instx = op->instx;
}


void SpillGen::Opline::forw ()
{
    assert(op != 0);
    assert(!ops->empty());
    if (op == &ops->front())
    {
        op = 0;
        return;
    }

    assert(instx != ops->front().instx);
    ++instx;

    Op* opx = op - 1;
    if (opx->instx == instx)
        op = opx;
}


void SpillGen::Opline::back (const Instx* x)
{
    if (op == 0)
    {
        assert(!ops->empty());
        op = &ops->front();
        instx = op->instx;
    }

    while (instx != x)
    {
        assert(instx != ops->back().instx);
        if (op->instx == instx)
            ++op;

        --instx;
    }
}


static bool operator == (const SpillGen::Op& op, const Inst* i) 
{
    return op.instx->inst == i;
}


bool SpillGen::Opline::isDef (const Inst* i) const
{
    Ops::iterator end, ptr = find(ops->begin(), end = ops->end(), i);
    return ptr != end && (ptr->oprole & RoleDef) != 0;
}


bool SpillGen::Opline::isUse (const Inst* i) const
{
    Ops::iterator end, ptr = find(ops->begin(), end = ops->end(), i);
    return ptr != end && (ptr->oprole & RoleUse) != 0;
}


/////   SpillGen
/////   ----------------------


void SpillGen::runImpl()    
{
#ifdef _DEBUG_SPILLGEN
    onConstruct(*this);
#endif

    const bool* spill_flag = static_cast<const bool*>(getIRManager().getInfo("SpillGen"));
    if (spill_flag != 0 && !*spill_flag)
    {
        DBGOUT("SpillGen skipped" << endl;);
        return;
    }

    registers.parse(getArg("regs"));
    DBGOUT("parameters: " << registers << endl;)
    irManager->resetOpndConstraints();

    fails   = 0;
    emitted = 0;

    opndcount = irManager->getOpndCount();
    lives_exit = new (mm) BitSet(mm, (U_32)opndcount); 

    actsmap.resize(opndcount);
    for (size_t i = 0; i < opndcount; ++i)
        actsmap[i] = 0;

    const Nodes& nodes = irManager->getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode())
        {
            bblock = node;
            if (bblock->isEmpty()) {
                continue;
            }

            lives_start = irManager->getLiveAtEntry(bblock);
            lives_exit->resize((U_32)opndcount);
            irManager->getLiveAtExit(bblock, *lives_exit);

            size_t bfails = 0;

            for (;;)
            {
                if (pass0() == 0)
                    break;

#ifdef _DEBUG_SPILLGEN
                if (Log::isEnabled())
                    outInstxs(Log::out(), *this);
#endif

                size_t x;
                if ((x = pass1()) == 0 || x == bfails)
                {
                    bfails = x;

                    if (x != 0 && Log::isEnabled())
                        Log::out() << "SpillGen cannot assign operand(s)" << std::endl;

                    break;
                }
                bfails = x;

                DBGOUT(endl << "reiterating BB#"<< bblock->getId() << endl;)

                if (opndcount != irManager->getOpndCount())
                {
                    size_t oldcount = opndcount;
                    opndcount = irManager->getOpndCount();
                    lives_exit->resize((U_32)opndcount);
                    irManager->fixLivenessInfo();

                    actsmap.resize(opndcount);
                    for (; oldcount < opndcount; ++oldcount)
                        actsmap[oldcount] = 0;
                }
            }

            fails += bfails;
        }
    }

    static Counter<size_t> count_emits("ia32:spillgen:emits", 0);
    count_emits += emitted;

#ifdef _DEBUG_SPILLGEN
    DBGOUT(endl << "Emitted movs :" << emitted << endl);

    if (fails)
        DBGOUT(endl << "FAILS: " << fails << endl);
#endif

    assert(fails == 0);

    if (fails != 0)
        Jitrino::crash("SpillGen failed");

#ifdef _DEBUG_SPILLGEN
    onDestruct(*this);
#endif
}


bool SpillGen::verify (bool force)
{
    bool failed = false;

    if (force || getVerificationLevel() >= 1)
    {
        irManager->packOpnds();
        irManager->updateLivenessInfo();

        if (!RegAllocCheck(*irManager).run(true))
            failed = true;

        if (!SessionAction::verify(true))
            failed = true;
    }

    return !failed;
}   


//  Preprocessing phase.
//  Scans all instructions in the basic block and create 'Instx' extension structure for
//  every instruction with required information.
//  Identifies all operands that needed to be allocated (problem operands) and fill 'Opline'
//  structure for each.
//
size_t SpillGen::pass0 ()   
{
    static CountTime pass0Timer("ia32::spillgen:pass0");
    AutoTimer tm(pass0Timer);

    actives.resize(0);

    evicts_known = false;

    const size_t regkinds = registers.size();
    assert(regkinds <= IRMaxRegKinds);

    instxs.resize(bblock->getInstCount() + 1);

    instxs[0].inst = 0;
    if (instxs[0].evicts == 0)
        instxs[0].evicts = new (mm) Opnds(mm);
    else
        instxs[0].evicts->resize(0);

    Instxs::iterator instxp = instxs.end() -  1;

    for (size_t i = 0; i < regkinds; ++i)
        instxp->regusage [i] = 0;

#ifdef _DEBUG_SPILLGEN
    DBGOUT(endl << "BB#"<< bblock->getId());
    if (!instxs.empty())
        DBGOUT (" [" << *(Inst*)bblock->getFirstInst() << " - " << *(Inst*)bblock->getLastInst() << "] ");
    DBGOUT(instxs.size() - 1 << endl);
#endif

//  calculate registers used at the block exit

    BitSet lives_next = *lives_exit;
    BitSet::IterB ib(*lives_exit);
    for (int i = ib.getNext(); i != -1; i = ib.getNext())
    {
        Constraint loc = irManager->getOpnd(i)->getConstraint(Opnd::ConstraintKind_Location, OpndSize_Default);
        int idx = registers.getIndex(loc);
        if (!loc.isNull() && idx != -1)
        {// this operand is already assigned to some location/register
            instxp->regusage[idx] |= loc.getMask();
        }
    }

//  iterate over instructions towards the top of the block

    for (Inst* inst = (Inst*)bblock->getLastInst(); inst != 0; inst = inst->getPrevInst())
    {
    //  Prepare processing of the next instruction

        Instxs::iterator instxq = instxp - 1;
        for (size_t i = 0; i < regkinds; ++i)
            instxq->regusage [i] = instxp->regusage[i];

    //  Process current instruction

        instxp->inst = inst;

        size_t nbopnds = inst->getOpndCount(Inst::OpndRole_All);
        if (instxp->nbopnds < nbopnds)
            instxp->oproles = new (mm) OpRole[instxp->nbopnds = (nbopnds + 8) & ~7];

        for (size_t i = 0;  i < regkinds; ++i)
            instxp->regpress[i] = 0;

        Opnd* opnd;
        size_t itx = 0;
        Inst::Opnds opnds(inst, Inst::OpndRole_All);
        for (Inst::Opnds::iterator it = opnds.begin(); it != opnds.end(); it = opnds.next(it), ++itx)
        {
            opnd = inst->getOpnd(it);

            U_32 roles = inst->getOpndRoles(it);

            OpRole oprole = 0;

            if ((roles & Inst::OpndRole_Def) != 0)
                oprole |= RoleDef;

            if ((roles & Inst::OpndRole_Use) != 0 || (inst->getProperties() & Inst::Properties_Conditional) != 0)
                oprole |= RoleUse;

            if ((inst->getProperties() & Inst::Properties_Conditional) != 0)
                oprole |= RoleCond;

            if (!lives_next.getBit(opnd->getId()))
                oprole |= RoleEnd;

            assert(itx < instxp->nbopnds);
            instxp->oproles[itx] = oprole;

            Constraint loc = opnd->getConstraint(Opnd::ConstraintKind_Location, OpndSize_Default);
            if (!loc.isNull())
            {// this operand is already assigned to some location/register
                int idx;
                if ((idx = registers.getIndex(loc)) != -1)
                {
                    RegMask msk = loc.getMask();
                    assert(msk != 0);
                    RegMask was = instxp->regusage[idx];
                    instxq->regusage [idx] |= msk;
                    if ((oprole & RoleUse) != 0)
                    {
                    }
                    else if ((oprole & (RoleDef | RoleCond)) == RoleDef)
                    {
                        instxq->regusage[idx] &= ~msk;
                        if ((was & msk) == 0)
                        {
                            DBGOUT("dead def " << *inst << " " << *opnd << endl;)
                            instxp->regusage [idx] |= msk;
                        }
                    }
                }
            }
            else
            {// this operand is not allocated yet
                unsigned mask = (1<<inst->getOpndCount())-1;
                loc = ((Inst*)inst)->getConstraint(it, mask, OpndSize_Default);
                int idx = registers.getIndex(loc);
                Opline* opline = actsmap[opnd->getId()];
                if (opline == 0) 
                {// Is this is a problem operand?
                    if (!(loc & Constraint(OpndKind_Memory)).isNull() || idx != -1)
                    {// operand can be assigned to memory or register
                        actives.resize(actives.size() + 1);
                        opline = &actives.back();
                        actsmap[opnd->getId()] = opline;

                        opline->clear(mm);
                        opline->opnd = opnd;
                        opline->at_start = lives_start->getBit(opnd->getId());
                        opline->at_exit  = lives_exit->getBit(opnd->getId());
                        opline->initial  = loc;
                    }
                    else
                    {
                        DBGOUT("Cannot assign " << *opnd << " @ " << *opline->instx->inst << endl;)
                    }
                }
                else
                {
                    opline->initial.intersectWith(loc);
                }

            //  this operand is tracked
                if (opline != 0)
                {
                    opline->addOp(&*instxp, oprole);

                    if ((loc & Constraint(OpndKind_Memory)).isNull())
                    {// Operand cannot assigned to memory
                        assert(idx != -1);
                        if (bitCount(loc.getMask()) == 1)
                            instxp->regpress[idx] |= loc.getMask();
                    }
                }
            }

            if (inst->isLiveRangeEnd(it)) 
                lives_next.setBit(opnd->getId(), false);
            else if (inst->isLiveRangeStart(it)) 
                lives_next.setBit(opnd->getId(), true);
        }

        instxp = instxq;
    }

    assert(instxp == instxs.begin());
    return actives.size();
}



//  Allocation phase.
//  Allocate all problem operands for the current block.
//
size_t SpillGen::pass1 ()   
{
    static CountTime pass1Timer("ia32::spillgen:pass1");
    AutoTimer tm(pass1Timer);

    size_t fails = 0;

    if (actives.size() > 1)
    {
        for (Oplines::iterator it = actives.begin(); it != actives.end(); ++it)
        {
            Opline& opline = *it;
            opline.weight = (int)bitCount(opline.initial.getMask());
        }
        sort(actives.begin(), actives.end(), Opline::smaller);
    }

    lives_catch = 0;
    Node* node = bblock->getExceptionEdgeTarget();
    if (node != NULL) 
        if ((lives_catch = irManager->getLiveAtEntry(node))->isEmpty()) 
            lives_catch = 0;

    for (Oplines::reverse_iterator it = actives.rbegin(); it != actives.rend(); ++it)
    {
        Opline& opline = *it;
        if (actsmap[opline.opnd->getId()] == 0)
            continue;

        actsmap[opline.opnd->getId()] = 0;

#ifdef _DEBUG_SPILLGEN
        opruns.incr(opline.ops->size());
#endif
        opline.opnd_mem = 0;
        opline.save_instx = 0;
        opline.save_changed = false;
        opline.start();

    //  Is this operand lives at the corresponding Dispatch node entry ?
        opline.catched = (lives_catch == 0) ? false : lives_catch->getBit(opline.opnd->getId());

        DBGOUT(opline;)

    //  Begin-block processing

        RegMask prefreg = 0;

    //  Is this operand lives at entry of this node ?
        if (opline.at_start)
            saveOpnd(opline, opline.instx, opndMem(opline));
        else
            prefreg = lookPreffered(opline);

    //  Process instructions that are using the operand

        while (opline.go()) 
            if (opline.isProc())
            {
                Constraint c(opline.opnd->getConstraint(Opnd::ConstraintKind_Initial));
                update(opline.instx->inst, opline.opnd, c);
                opline.idx = registers.getIndex(c);

                if (!tryRegister(opline, c, prefreg))
                    if (!tryMemory(opline, c))
                        if (!tryEvict(opline, c))
                            if (!tryRepair(opline, c))
                            {// Cannot assign operand, so let it remains in the original form
                                DBGOUT("Cannot assign " << opline.opnd << " @ " << *opline.instx->inst << endl;)
                                ++fails;

                                if (simplify(opline.instx->inst, opline.opnd))
                                {
                                    loadOpndMem(opline);
                                    break;
                                }

                                loadOpnd(opline, opline.instx, opline.opnd);

                                if (opline.isDef())
                                    opline.save_changed = true;

                                saveOpnd(opline, opline.instx, opline.opnd);

                                opline.forw();
                            }
            }
            else
            {
                opline.forw();
            }

    //  End-block processing

        //assert(opline.instx == opline.ops->front().instx);
        if (opline.at_exit)
            loadOpndMem(opline);
    }
    return fails;
}


SpillGen::RegMask SpillGen::lookPreffered (Opline& opline)
{
    Inst* inst = opline.instx->inst;
    if (inst->getMnemonic() != Mnemonic_MOV || inst->getOpnd(0) != opline.opnd)
        return 0;

    if ((opline.instx->oproles[1] & RoleEnd) == 0)
        return 0;

    Opnd* opnd1 = inst->getOpnd(1);

    RegMask msk;
    Constraint loc = opnd1->getConstraint(Opnd::ConstraintKind_Location, OpndSize_Default);
    if (!loc.isNull() && registers.getIndex(loc) != -1)
        msk = loc.getMask();
    else
        return 0;

    DBGOUT("COALESCE at " << *inst << " " << *opnd1 << endl;)
    return msk;
}
                                

//  Attempt to allocate a register for the operand.
//
bool SpillGen::tryRegister (Opline& opline, Constraint c, RegMask prefreg)
{
    if (opline.idx == -1)
        return false;

    Constraint cx = c.getAliasConstraint(OpndSize_Default) & registers[opline.idx];
    Constraint cr((OpndKind)cx.getKind(), c.getSize(), cx.getMask());

//  handle first instruction of the interval

    RegMask mk = cr.getMask() & ~usedRegs(opline.instx, opline.idx, opline.isUse());
    if (mk == 0)
    {
        DBGOUT("   -No reg [" << *opline.instx->inst << "]" << c << endl;)
        return false;
    }

    RegMask mkpost = ~(RegMask)0;
    if (!opline.isDef())
        mkpost = callRegs(opline.instx, opline.idx);

//  handle second and all others instructions

    Instx* begx = opline.instx;
    Instx* endx = begx;
    int count = 0;
    for (opline.forw(); opline.go(); opline.forw())
    {
        RegMask tmp = mk & mkpost;
        if (opline.isEnd())
            tmp &= ~opline.instx->regusage[opline.idx-1];
        else
            tmp &= ~opline.instx->regusage[opline.idx];

        if (tmp == 0)
            break;

        int cnt = 0;
        if (opline.isProc())
        {
            if (opline.isDef() && !opline.isUse())
                break;

            if ((cnt = update(opline.instx->inst, opline.opnd, cr)) != 0)
            {
                tmp &= cr.getMask();
                if (tmp == 0)
                    break;
                endx = opline.instx;
            }
        }

        mk = tmp;
        mkpost = callRegs(opline.instx, opline.idx);
        count += cnt;
    }

    DBGOUT("   -reg [" << *begx->inst << " - " << *endx->inst 
        << "] avail:" << RegMasks(cx, mk) << " count:" << count << endl;)

    if (count == 0)
    {
        assert(begx == endx);
        Constraint cm = c & OpndKind_Memory;
        if (!cm.isNull())
        {
            DBGOUT("   -mem [" << *begx->inst << "]" << endl;)
            assignMem(opline, begx, begx);
            return true;
        }
    }

    if ((mk & prefreg) != 0)
    {
        mk &= prefreg;
        static Counter<size_t> count_preffer("ia32:spillgen:preffered", 0);
        count_preffer++;
    }
    RegName rn = findFree(mk, opline.idx, begx);
    assignReg(opline, begx, endx, rn);
    return true;
}


//  Attempt to assign operand to memory.
//
bool SpillGen::tryMemory (Opline& opline, Constraint c)
{
    c.intersectWith(OpndKind_Memory);
    if (c.isNull())
    {
        DBGOUT("   -No mem [" << *opline.instx->inst << "]" << endl;)
        return false;
    }

    DBGOUT("   -mem [" << *opline.instx->inst << "]" << endl;)
    assignMem(opline, opline.instx, opline.instx);
    opline.forw();
    return true;
}


//  Attempt to find other operand that can be temporary saved to memory and use it's register
//  for the operand processed.
//
bool SpillGen::tryEvict (Opline& opline, Constraint c)
{
    if (opline.idx == -1)
        return false;

    Constraint cx = c.getAliasConstraint(OpndSize_Default) & registers[opline.idx];
    Constraint cr((OpndKind)cx.getKind(), c.getSize(), cx.getMask());

    Instx* begx = opline.instx;
    Instx* endx = begx;

    for (opline.forw(); opline.go(); opline.forw())
    {
        Constraint cx = cr;
        if (update(opline.instx->inst, opline.opnd, cx))
            if (cx.getMask() == 0)
                break;

        cr = cx;
        endx = opline.instx;
    }

    RegName rn = findEvict(cr.getMask(), opline.idx, begx, endx);
    if (rn == RegName_Null)
    {
        DBGOUT("   -No evict [" << *begx->inst << " - " << *endx->inst << "]" << endl;)
        opline.back(begx);
        return false;
    }

    DBGOUT("   -evict [" << *begx->inst << " - " << *endx->inst 
           << "] " << getRegNameString(rn) << endl;)

    static Counter<size_t> count_evicts("ia32:spillgen:evicts", 0);
    count_evicts++;

    assignReg(opline, begx, endx, rn);

    opline.back(endx);
    opline.forw();
    return true;
}


//  Attempt to change allocation that was previously made, i.e. reassign register that 
//  already used in the instruction for other operand because the processed operand 
//  requires already assigned register.
//
bool SpillGen::tryRepair (Opline& opline, Constraint c)
{
    if (opline.idx == -1)
        return false;

    Constraint ca = c.getAliasConstraint(OpndSize_Default) & registers[opline.idx];
    Constraint cr((OpndKind)ca.getKind(), c.getSize(), ca.getMask());

    Inst* inst = opline.instx->inst;

    RegMask mk = cr.getMask();

    Opnd* ox = 0, *ox_out = 0;
    RegName rx = RegName_Null;
    bool need_load = false,
         need_save = false;

    size_t itx = 0;
    Inst::Opnds opnds(inst, Inst::OpndRole_All);
    for (Inst::Opnds::iterator it = opnds.begin(); it != opnds.end(); it = opnds.next(it), ++itx)
    {
        ox = inst->getOpnd(it);
        RegMask oxMask = ox->getConstraint(Opnd::ConstraintKind_Initial).getMask();

        RegMask rm = getRegMaskConstr(ox, cr);
        if ((mk & rm) != 0)
        {
            OpRole oprole = opline.instx->oproles[itx];
            need_load = (oprole & RoleUse) != 0;
            need_save = (oprole & RoleEnd) == 0;

            unsigned mask = (1<<inst->getOpndCount())-1;
            Constraint cx = ((Inst*)inst)->getConstraint(it, mask, OpndSize_Default);
            RegMask used = usedRegs(opline.instx, opline.idx, need_load);
            RegMask usable = (cx  & registers[opline.idx]).getMask() & ~mk & oxMask;
            if ((usable & ~used) != 0)
            {
                rx = findFree(usable & ~used, opline.idx, opline.instx);
                ox_out = opndReg(ox, rx);
                break;
            }
            else if (!(cx & OpndKind_Memory).isNull())
            {
                ox_out = opndMem(ox);
                break;
            }
            else
            {
                rx = findEvict(usable, opline.idx, opline.instx, opline.instx);
                if (rx != RegName_Null)
                {
                    ox_out = opndReg(ox, rx);
                    break;
                }
            }
        }
    }

    if (ox_out == 0)
        return false;

//  relocate operand 'ox' to new register 'rx'

    DBGOUT("   -repair [" << *inst << "] " << *opline.opnd 
           << " " << *ox << " " << *ox_out << endl;)

    static Counter<size_t> count_repairs("ia32:spillgen:repairs", 0);
    count_repairs++;

    if (need_load)
        emitMove(true,  inst, ox_out, ox);
    if (need_save)
        emitMove(false, inst, ox, ox_out);

    inst->replaceOpnd(ox, ox_out);
    if (rx != RegName_Null /*&& need_save*/)
        opline.instx->regusage[opline.idx] |= getRegMask(rx);

//  assign the requsted operand to the register which was previosly assigned to ox

    assignReg(opline, opline.instx, opline.instx, ox->getRegName());
    opline.forw();
    return true;
}


bool SpillGen::simplify (Inst* inst, Opnd* opnd)
{
    Opnd* opndm;
    if (inst->hasKind(Inst::Kind_LocalControlTransferInst) && inst->getOpndCount() == 1)
    {
        opndm = inst->getOpnd(0);
        Opnd* opndx = irManager->newMemOpnd(opndm->getType(), 
                                           MemOpndKind_StackAutoLayout, 
                                           irManager->getRegOpnd(STACK_REG), 
                                           0);
        DBGOUT("simplify jump " << *opndm << " -> mem " << *opndx << endl;);
        emitMove(true, inst,opndx, opndm);
        inst->replaceOpnd(opndm, opndx);

        for (int so = 0; so != MemOpndSubOpndKind_Count; ++so)
        {
            Opnd* sub = opndm->getMemOpndSubOpnd(static_cast<MemOpndSubOpndKind>(so));
            if (sub != 0)
                actsmap[sub->getId()] = 0;
        }
        return true;
    }

    else if (inst->hasKind(Inst::Kind_CallInst) && (opndm = inst->getOpnd(0)) == opnd)
    {
        Opnd* opndx = irManager->newMemOpnd(opndm->getType(), 
                                           MemOpndKind_StackAutoLayout, 
                                           irManager->getRegOpnd(STACK_REG), 
                                           0);
        DBGOUT("simplify call " << *opndm << " -> mem " << *opndx << endl;);
        emitMove(true, inst,opndx, opndm);
        inst->replaceOpnd(opndm, opndx);

        for (int so = 0; so != MemOpndSubOpndKind_Count; ++so)
        {
            Opnd* sub = opndm->getMemOpndSubOpnd(static_cast<MemOpndSubOpndKind>(so));
            if (sub != 0)
                actsmap[sub->getId()] = 0;
        }
        return true;
    }

    return false;
}


//  Return mask of all free registers at the instruction specified.
//  If register will define value, set isuse=false.
//  If regsiter will load value defined in some other instruction, set isuse=true
//
SpillGen::RegMask SpillGen::usedRegs (const Instx* instx, int idx, bool isuse) const
{
    RegMask used = instx->regusage[idx];
    if (isuse)
        used |= (used ^ (instx-1)->regusage[idx]);

    return used;
}


SpillGen::RegMask SpillGen::callRegs (const Instx* instx, int idx) const
{
    if (instx->inst->getMnemonic() == Mnemonic_CALL)
    {
        OpndKind k = static_cast<OpndKind>(registers[idx].getKind());
        Constraint ci = static_cast<CallInst*>(instx->inst)->getCalleeSaveRegs(k);
        return ci.getMask();
    }

    return ~(RegMask)0;
}


//  If currently handled operand is referenced by current instruction, then evaluate
//  constraint of the operand imposed by this instruction and return 'true'.
//  Otherwise, do nothing and return false.
//
int SpillGen::update (const Inst* inst, const Opnd* opnd, Constraint& constr) const
{
    int count = 0;
    Inst::Opnds opnds(inst, Inst::OpndRole_All);
    for (Inst::Opnds::iterator it = opnds.begin(); it != opnds.end(); it = opnds.next(it))
        if ( inst->getOpnd(it) == opnd)
        {
        //  if operand occures more than once then conservatively take 
        //  the strong constraint for the operand
            Constraint c;
            c = ((Inst*)inst)->getConstraint(it, count?0xFFFFFFFF:0x7FFFFFFF, constr.getSize());
            if (constr.isNull())
                constr = c;
            else
                constr.intersectWith(c);

            count++;    
        }
    return count;
}


//  Split operand and assign register 'rn' to it in the interval specified 
//  ('begx' - 'endx', inclusive)
//
void  SpillGen::assignReg (Opline& opline, Instx* begx, Instx* endx, RegName rn)
{
    assert(rn != RegName_Null);
    RegMask mk = getRegMask(rn);
    Opnd* opnd_reg = opndReg(opline.opnd, rn);

    int reguses = 0;

    DBGOUT("assignReg " << *opline.opnd << " [" << *begx->inst 
           << " - " << *endx->inst << "] to " << *opnd_reg << endl;)

    if (loadOpnd(opline, begx, opnd_reg))
        (begx-1)->regusage[opline.idx] |= mk;

    bool has_def = false;
    Instx* savex = endx;

    for (Instx* ptrx = begx; ptrx <= endx; ++ptrx)
    {
        if (opline.save_changed && opline.catched && ptrx->inst->hasKind(Inst::Kind_CallInst))
        {
            emitMove(true, ptrx->inst, opndMem(opline), opnd_reg);
            opline.save_changed = false;
        }

        if (ptrx->inst->replaceOpnd(opline.opnd, opnd_reg))
        {// the operand is used in the instruction
            ++reguses;
            if (opline.isDef(ptrx->inst))
            {
                has_def = true;
                opline.save_changed = true;
                savex = ptrx;
            }
        }
        else
        {// the operand is not used in the instruction, so it can be evicted
            if (ptrx->evicts != 0)
                ptrx->evicts->push_back(opnd_reg);
        }

    //  special processing for dead-definition (begx == endx && has_def)
        if (ptrx != endx || (begx == endx && has_def))
            ptrx->regusage[opline.idx] |= mk;
    }

    bool before = false;
    if (savex->inst->getMnemonic() == Mnemonic_CALL && !opline.isDef(savex->inst))
    {
        Constraint ci = static_cast<CallInst*>(savex->inst)->getCalleeSaveRegs(getRegKind(rn));
        before = (ci.getMask() & mk) == 0;
    }
    saveOpnd(opline, savex, opnd_reg, before ? 0 : mk, opline.idx, before);

#ifdef _DEBUG_SPILLGEN
    assigns.incr(reguses);
#endif
}


//  Split operand and assign memory location to it in the interval specified 
//  ('begx' - 'endx', inclusive)
//
void  SpillGen::assignMem (Opline& opline, Instx* begx, Instx* endx)
{
    opndMem(opline);

    DBGOUT("assignMem " << *opline.opnd << " [" << *begx->inst 
           << " - " << *endx->inst << "] to " << *opline.opnd_mem << endl;)

    loadOpnd(opline, begx, opline.opnd_mem);

    if (opline.opnd != opline.opnd_mem)
        for (; begx <= endx; ++begx)
        {
            begx->inst->replaceOpnd(opline.opnd, opline.opnd_mem);
        }

    saveOpnd(opline, endx, opline.opnd_mem);
    opline.save_changed = false;
}


//  Lazy save 'opnd' to memory at the point 'instx'
//
void  SpillGen::saveOpnd (Opline& opline, Instx* instx, Opnd* opnd)
{
    opline.save_instx  = instx;
    opline.save_before = false;
    opline.save_opnd   = opnd;
    opline.save_regmsk = 0;
    opline.save_regidx = -1;
}


void  SpillGen::saveOpnd (Opline& opline, Instx* instx, Opnd* opnd, RegMask regmsk, int regidx, bool before)
{
    opline.save_instx  = instx;
    opline.save_before = before;
    opline.save_opnd   = opnd;
    opline.save_regmsk = regmsk;
    opline.save_regidx = regidx;
}


//  Load operand from memory to 'opnd' at the poinr 'instx'
//
bool  SpillGen::loadOpnd (Opline& opline, Instx* instx, Opnd* opnd)
{
    if (opline.save_instx == 0 || !opline.isUse(instx->inst))
        return false;

    if (opline.save_opnd->hasAssignedPhysicalLocation())
    {
        opndMem(opline);
        if (opline.save_changed)
        {
            emitMove(opline.save_before, 
                     opline.save_instx->inst, 
                     opline.opnd_mem, 
                     opline.save_opnd);
            if (opline.save_regmsk != 0)
                opline.save_instx->regusage[opline.save_regidx] |= opline.save_regmsk;
            opline.save_changed = false;
        }
        emitMove(true,  instx->inst, opnd, opline.opnd_mem);
    }
    else
    {
        if (opnd->hasAssignedPhysicalLocation())
        {// Assigned <- UnAssigned
            emitMove(true,  instx->inst, opnd, opline.save_opnd);
        }
        else
        {// UnAssigned <- UnAssigned
            assert(opnd == opline.save_opnd);
            opline.save_opnd  = opnd;
            opline.save_instx = instx;
        }
    }
    return true;
}


//  Save operand to memory
//
void SpillGen::loadOpndMem (Opline& opline)
{
    if (opline.save_instx == 0)
        return;

    if (opline.save_opnd->hasAssignedPhysicalLocation())
    {
        if (opline.save_changed)
        {
            emitMove(opline.save_before, 
                     opline.save_instx->inst, 
                     opndMem(opline), 
                     opline.save_opnd);
            if (opline.save_regmsk != 0)
                opline.save_instx->regusage[opline.save_regidx] |= opline.save_regmsk;
            opline.save_changed = false;
        }
    }
}


Opnd* SpillGen::opndMem (Opline& opline)
{
    if (opline.opnd_mem == 0)
            opline.opnd_mem = opndMem(opline.opnd);

    return opline.opnd_mem;
}


static bool operator == (const SpillGen::TwoOpnds& p, const Opnd* o)
{
    return p.first == o;
}


Opnd* SpillGen::opndMem (const Opnd* opnd)
{
    Opnd* opnd_mem;

    MemOpnds::iterator ptr, end;
    if ((ptr = find(memopnds.begin(), end = memopnds.end(), opnd)) == end)
    {
        opnd_mem = irManager->newMemOpnd(opnd->getType(), 
                                        MemOpndKind_StackAutoLayout, 
                                        irManager->getRegOpnd(STACK_REG), 
                                        0);
        memopnds.push_back(TwoOpnds(opnd, opnd_mem)); 
#ifdef _DEBUG_SPILLGEN
        DBGOUT("split " << *opnd << " -> mem " << *opnd_mem << endl;)
        //if (dbgraw != 0)
        //    dbgraw->split(opnd, opnd_mem);
#endif
    }
    else
    {
        opnd_mem = ptr->second;
    }

    return opnd_mem;
}


Opnd* SpillGen::opndReg (const Opnd* opnd, RegName rn) const
{
    Opnd* opnd_reg = irManager->newRegOpnd(opnd->getType(), getRegName(getRegKind(rn), opnd->getSize(), getRegIndex(rn)));
#ifdef _DEBUG_SPILLGEN
    DBGOUT("split " << *opnd << " -> reg " << *opnd_reg << endl;)
    //if (dbgraw != 0)
    //    dbgraw->split(opnd, opnd_reg);
#endif
    return opnd_reg;
}


void  SpillGen::emitPushPop  (bool before, Inst* inst, Opnd* opnd, bool push)
{
    if (push)
        emitMove(before, inst, opndMem(opnd), opnd);
    else
        emitMove(before, inst, opnd, opndMem(opnd));
}


//  Emit 'mov' instruction before/after the instruction specified
//
Inst* SpillGen::emitMove  (bool before, Inst* inst, Opnd* dst, Opnd* src)
{
    if (dst == src)
        return 0;

    Inst* mov = irManager->newCopyPseudoInst(Mnemonic_MOV, dst, src);
    if (before) {
        mov->insertBefore(inst);
    } else
    {
        // ensure we don't append after any branch instructions
        // for example, after the last instruction in basic block
        assert(!inst->hasKind(Inst::Kind_LocalControlTransferInst));  
        mov->insertAfter(inst);
    }

    DBGOUT((before?"before":" after") << " " << *inst << "  " 
           << *mov << "  MOV " << *dst << " " << *src << endl;)
    emitted++;

    return mov;
}

RegName SpillGen::findFree (RegMask usable, int idx, Instx* instx)
{
    Constraint c = registers[idx];
    usable &= c.getMask();  // search only from available registers

    Instx* begx = instxs.empty()? (Instx*)NULL:&*instxs.begin(); 
    Instx* endx = begx + instxs.size() ;
    ++begx;
    Instx* ptrx;
    size_t xbest = INT_MAX,
           xbest_free  = INT_MAX,
           xlgth_free  = 0,
           xbest_press = INT_MAX,
           xlgth_press = 0;

    size_t x, found;
    RegMask msk;
    for (x = 0, found = 0, msk = 1; usable != 0; ++x, msk <<= 1) {
        if ((usable & msk) != 0)
        {
            usable ^= msk, ++found;
            if (usable == 0 && found == 1)
            {// this is the only register available - no need to choose the best
                xbest = x;
                break;
            }

            size_t lgth  = 0;
            bool   press = (instx->regpress[idx] & msk) != 0;

            for (ptrx = instx-1; ptrx >= begx && (ptrx->regusage[idx] & msk) == 0; --ptrx)
            {
                ++lgth;
                press |= (ptrx->regpress[idx] & msk) != 0;
            }
            for (ptrx = instx+1; ptrx <  endx && (ptrx->regusage[idx] & msk) == 0; ++ptrx)
            {
                ++lgth;
                press |= (ptrx->regpress[idx] & msk) != 0;
            }
            if (press)
            {
                if (xbest_press == INT_MAX || xlgth_press >= lgth)
                    xbest_press = x,
                    xlgth_press = lgth;
            }
            else
            {
                if (xbest_free == INT_MAX || xlgth_free >= lgth)
                    xbest_free = x,
                    xlgth_free = lgth;
            }
        }
    }
    if (xbest == INT_MAX) {
        xbest = xbest_free != INT_MAX ? xbest_free : xbest_press;
    }

    return xbest == INT_MAX ? 
            RegName_Null : 
            getRegName(static_cast<OpndKind>(c.getKind()), c.getSize(), (int)xbest);
}


RegName SpillGen::findEvict (RegMask usable, int idx, Instx* begx, Instx*& endx)
{
    Constraint c = registers[idx];
    Inst* inst = begx->inst;
    Opnd* opnd;

    RegMask used = 0;
    Inst::Opnds opnds(inst, Inst::OpndRole_All);
    for (Inst::Opnds::iterator it = opnds.begin(); it != opnds.end(); it = opnds.next(it))
        used |= getRegMaskConstr(inst->getOpnd(it), c);

    usable &= ~used;
    if (usable == 0)
        return RegName_Null;

    if (!evicts_known)
        setupEvicts();

    Evicts evicts(mm);

    for (Opnds::iterator it = begx->evicts->begin(); it != begx->evicts->end(); ++it)
        if ((opnd = *it) != 0 && (usable & getRegMaskConstr(opnd, c)) != 0)
        {
            Evict tmp;
            tmp.opnd = opnd;
            tmp.begx = begx;
            tmp.endx = begx;
            evicts.push_back(tmp);
        }

    if (evicts.empty())
        return RegName_Null;

    Evict* evict = pickEvict(evicts);
    if (evict == 0)
        return RegName_Null;

    if (evict->endx->inst->hasKind(Inst::Kind_LocalControlTransferInst)) {
        --(evict->endx);
        if ((evict->endx < evict->begx) || (evict->endx < endx))
            return RegName_Null;
    }

    if (endx > evict->endx)
        endx = evict->endx;

//  now evict the operand found

    emitPushPop(true,  evict->begx->inst, evict->opnd, true);
    emitPushPop(false, evict->endx->inst, evict->opnd, false);

//  update the 'instxs' table

    RegName rn = evict->opnd->getRegName();
    RegMask mk = getRegMask(rn);
    assert((usable & mk));

    Instx* ptrx;
    for (ptrx = evict->begx; ptrx <= evict->endx; ++ptrx)
    {
    //  mark register assigned to the operand as free
        if (ptrx != evict->endx)
            ptrx->regusage[idx] &= ~mk;

        //  clear the operand as a candidate to evict
        killEvict(evict->opnd, ptrx);
    }

    return rn;
}


void SpillGen::setupEvicts ()
{
    Opnd* opnd;
    Constraint loc;
    RegMask msk;
    int idx;

//  will be used to calculate all registers used in an instruction
    Registers used(mm);
    used = registers;

    Registers* catched = 0;

    BitSet ls = *lives_exit;
    ls.resize(irManager->getOpndCount());
    BitSet lsnext = ls;

    Instxs::iterator instxp = instxs.end() -  1;
    for (Inst* inst = (Inst*)bblock->getLastInst(); inst != 0; inst = inst->getPrevInst())
    {
        used.clearMasks();  // mask of all registers used in the current instruction

        Inst::Opnds opnds(inst, Inst::OpndRole_All);
        for (Inst::Opnds::iterator it = opnds.begin(); it != opnds.end(); it = opnds.next(it))
        {
            opnd = inst->getOpnd(it);
            loc  = opnd->getConstraint(Opnd::ConstraintKind_Location, OpndSize_Default);
            if ((msk = loc.getMask()) != 0 && (idx = registers.getIndex(loc)) != -1)
            {// this operand is already assigned to some location/register
                if (inst->isLiveRangeEnd(it)) 
                    lsnext.setBit(opnd->getId(), false);
                else if (inst->isLiveRangeStart(it)) 
                    lsnext.setBit(opnd->getId(), true);

                merge(used[idx], msk);
            }
        }
        if (inst->hasKind(Inst::Kind_CallInst) && lives_catch != 0)
        {
            if (catched == 0)
            {// construct the catched operand list - once per block
                catched = new (mm) Registers(mm);
                *catched = registers;
                catched->clearMasks();

                BitSet::IterB lives(*lives_catch);
                for (int i = lives.getNext(); i != -1; i = lives.getNext())
                {
                    opnd = irManager->getOpnd(i);
                    loc  = opnd->getConstraint(Opnd::ConstraintKind_Location, OpndSize_Default);
                    if ((msk = loc.getMask()) != 0 && (idx = registers.getIndex(loc)) != -1)
                        merge((*catched)[idx], msk);
                }
            }
            used.merge(*catched);
        }
        if (inst == instxp->inst)
        {
            if (instxp->evicts == 0)
                instxp->evicts = new (mm) Opnds(mm);
            else
                instxp->evicts->resize(0);

            BitSet::IterB lives(ls);
            for (int i = lives.getNext(); i != -1; i = lives.getNext())
            {
                opnd = irManager->getOpnd(i);
                loc  = opnd->getConstraint(Opnd::ConstraintKind_Location, OpndSize_Default);
                if (loc.getMask() != 0 && registers.contains(loc) && !used.contains(loc))
                    instxp->evicts->push_back(opnd);
            }

            --instxp;
        }

        ls = lsnext;
    }

    evicts_known = true;
}


SpillGen::Evict* SpillGen::pickEvict (Evicts& evicts)
{
    Instx* begx = instxs.empty()? (Instx*)NULL:&*instxs.begin(); 
    Instx* endx = begx + instxs.size();
    ++begx;
    Instx* ptrx;

    DBGOUT("Evicts" << endl;)
    for (Evicts::iterator it = evicts.begin(); it != evicts.end(); ++it)
    {
        Evict& evict = *it;

        for (ptrx = evict.begx-1; ptrx >= begx && isEvict(evict.opnd, ptrx); --ptrx)
            evict.begx = ptrx;

        for (ptrx = evict.endx+1; ptrx <  endx && isEvict(evict.opnd, ptrx); ++ptrx)
            evict.endx = ptrx;

        evict.weight = int(evict.endx - evict.begx);

        DBGOUT(" evict " << *evict.opnd << " [" << *evict.begx->inst 
               << " - " << *evict.endx->inst << "] w:" << evict.weight << endl;)
    }
    assert(!evicts.empty());
    return &*max_element(evicts.begin(), evicts.end());
}


bool SpillGen::isEvict (const Opnd* opnd, const Instx* ptrx) const
{
    Opnds::iterator endx = ptrx->evicts->end();
    return find(ptrx->evicts->begin(), endx, opnd) != endx;
}


void SpillGen:: killEvict (const Opnd* opnd, Instx* instx) const
{
    Opnds::iterator end = instx->evicts->end();
    Opnds::iterator  it = find(instx->evicts->begin(), end, opnd);
    if (it != end)
        *it = 0;
}


SpillGen::RegMask SpillGen::getRegMaskConstr (const Opnd* opnd, Constraint c) const
{
    return opnd->isPlacedIn(c.getAliasConstraint(OpndSize_Default)) ? 
                getRegMask(opnd->getRegName()) : 0;
}


#ifdef _DEBUG_SPILLGEN
#include "Ia32SpillGenDbgTail.h"
#endif


} //namespace Ia32

} //namespace Jitrino





