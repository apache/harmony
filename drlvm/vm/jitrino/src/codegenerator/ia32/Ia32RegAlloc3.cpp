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
 * @author Intel, Sergey L. Ivashin
 */

#include "Ia32IRManager.h"
#include "Ia32RegAllocCheck.h"
#include "Stl.h"
#include "Log.h"
#include "Ia32Printer.h"
#include <algorithm>
#include <iostream>
#include <iomanip>
#include <sstream>
#include <stdio.h>

#ifdef _DEBUG_REGALLOC3
#ifdef _MSC_VER
#pragma warning(disable : 4505)   //unreferenced local function has been removed
#endif //#ifdef _MSC_VER
#endif //#ifdef _DEBUG_REGALLOC3


using namespace std;

#define _SKIP_CATCHED

namespace Jitrino
{

namespace Ia32
{


//========================================================================================
// class Ia32RegAlloc3
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
 */

struct RegAlloc3 : public SessionAction
{
    MemoryManager mm;           // this is private MemoryManager, not irm.getMemoryManager()

    unsigned flag_SORT;
    bool flag_NEIGBH;
    bool flag_COALESCE;

    unsigned coalesceCount;

    class BoolMatrix;
    typedef U_32 RegMask;     // used to represent set of registers

    static void merge (Constraint& c, RegMask mk)   {c.setMask(c.getMask() | mk);}

    // Table of all available registers sets.
    // Each set (GPReg, FPReg, XMMReg and so on) is represented by the corresponding 
    // Constraint object.
    struct Registers : public StlVector<Constraint>
    {
        Registers (MemoryManager& mm)       :StlVector<Constraint>(mm) {}

        void  parse (const char*);

        // register the new constraint (register) in the table.
        // if table doesn't contain constraint of the specified kind, it will be ignored
        // (add = false) or new table entry for the constraint will be created (add = true).
        int merge (const Constraint&, bool add = false);

        // returns table index of the constraint of the specified kind or -1
        int index (const Constraint&) const;

        int indexes[IRMaxRegKinds];
    };
    Registers registers;

    struct Oprole
    {
        Inst* inst;
        U_32 role;
    };
    typedef StlVector<Oprole> Oproles;

    typedef StlList<int> Indexes;

    struct Opndx
    {
        Indexes* adjacents,
               * hiddens;

        Indexes* neighbs;

        Opnd*    opnd;
        Oproles* oproles;

        int ridx;       // index in Registers of register assigned/will be assigned
        RegMask alloc,  // 0 or mask of the register assigned
                avail;  // if not assigned, then mask of the registers available (defined by calculated constraint)
        unsigned nbavails;  // number of the registers available for this operand ( =bitCount(avail) )
        double  spillcost;
        bool    spill;  // operand selected for spilling
        bool    ignore; // this operand was coalesced so its entry in the graph is invalid
    };

    //  Operand's graph to be colored
    struct Graph : public StlVector<Opndx>
    {
        Graph (MemoryManager& m)            : StlVector<Opndx>(m) {}

        void connect (int x1, int x2) const;
        int  disconnect (int x) const;
        void reconnect  (int x) const;
        void moveNodes (Indexes& from, Indexes& to, int x) const;
    };
    Graph graph;

    size_t graphsize,  // total size of graph (operands + registers)
             xregbase;   // index of first register in graph

    StlVector<int> nstack;


    RegAlloc3 ()                    : mm("RegAlloc3"), registers(mm), graph(mm), nstack(mm) {}

    U_32 getNeedInfo () const     {return NeedInfo_LivenessInfo;}
    U_32 getSideEffects () const  {return coalesceCount == 0 ? 0 : SideEffect_InvalidatesLivenessInfo;}

    void runImpl();
    void SpillGen ();
    bool verify (bool force=false);

    bool buildGraph ();
    void processInst (Inst*, BitSet&, int* opandmap, BoolMatrix&, double excount);
    void showGraph ();
    void lookLives (Opnd*, BitSet&, int* opandmap, BoolMatrix&);
    int  findNode  (Opnd*) const;
    bool coalescing (int* opandmap, BoolMatrix& matrix);
    void coalesce   (int* opandmap, BoolMatrix& matrix, int x0, int x1);
    int  duplicates (Indexes* list, BoolMatrix& matrix, int x0, int x1);
    void pruneGraph ();
    bool shouldPrune (const Opndx&) const;
    RegAlloc3::RegMask occupiedReg (OpndSize, OpndSize, RegAlloc3::RegMask);
    bool assignRegs ();
    bool assignReg  (Opndx&);
    void spillRegs  ();
    int  spillReg   (Opndx&);
    int update (const Inst*, const Opnd*, Constraint&) const;
};


static ActionFactory<RegAlloc3> _cg_regalloc("cg_regalloc");


static Counter<size_t> count_spilled("ia32:regalloc3:spilled", 0),
                       count_assigned("ia32:regalloc3:assigned", 0),
                       count_coalesced("ia32:regalloc3:coalesced", 0);


//========================================================================================
// Internal debug helpers
//========================================================================================


using std::endl;
using std::ostream;

#ifdef _DEBUG_REGALLOC3

struct Sep
{
    Sep ()      :first(true) {}

    bool first;
};

static ostream& operator << (ostream&, Sep&);

static ostream& operator << (ostream&, const Inst&);

static ostream& operator << (ostream&, const Opnd&);

static ostream& operator << (ostream&, Constraint);

static ostream& operator << (ostream&, const RegAlloc3::Registers&);

struct RegMasks
{
    RegMasks (Constraint x, RegAlloc3::RegMask mk)  : c(x) {c.setMask(mk);}

    Constraint c;
};

static ostream& operator << (ostream&, RegMasks);

static ostream& outRegMasks (ostream&, RegAlloc3::RegMask*, const RegAlloc3::Registers&);

static ostream& operator << (ostream&, const RegAlloc3::Opndx&);

static ostream& operator << (ostream&, const RegAlloc3::Graph&);

#define DBGOUT(s) log(LogStream::DBG).out() << s

#else

#define DBGOUT(s) 

#endif


//========================================================================================
//  Utility
//========================================================================================


static int bitCount (RegAlloc3::RegMask mk)
{
    int count = 0;
    while (mk != 0)
    {
        if ((mk & 1) != 0)
            ++count;
        mk >>= 1;
    }
    return count;
}


static int bitNumber (RegAlloc3::RegMask mk)
{
    assert(mk != 0);

    int number = 0;
    while (mk != 1)
    {
        ++number;
        mk >>= 1;
    }
    return number;
}


static RegAlloc3::RegMask findHighest (RegAlloc3::RegMask mk)
{
    assert(mk != 0);

    RegAlloc3::RegMask high = 1,
                       highest = (RegAlloc3::RegMask)~1;

    while ((mk & highest) != 0)
    {
        high <<= 1,
        highest <<= 1;
    }

    return high;
}


//========================================================================================
//  Tokens - Utility class for (zero-terminated) strings parsing
//========================================================================================


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


//========================================================================================
//  BoolMatrix - Symmetric boolean matrix
//========================================================================================


class RegAlloc3::BoolMatrix
{
public:

    BoolMatrix (MemoryManager&, size_t);

    void clear ();
    void clear (int i, int j)           {at((unsigned)i, (unsigned)j); *ptr &= ~msk;}
    void set   (int i, int j)           {at((unsigned)i, (unsigned)j); *ptr |= msk;}
    bool test  (int i, int j)           {at((unsigned)i, (unsigned)j); return (*ptr & msk) != 0;}

private:

    void at (unsigned i, unsigned j)
    {
        assert((size_t)i < dim && (size_t)j < dim);

        const unsigned bitn = (i < j) ? j*(j-1)/2 + i
                                      : i*(i-1)/2 + j;

        msk = (char)(1 << (bitn & 7));
        ptr = base + (bitn >> 3);
    }

    size_t dim, dims;
    char* base;

    char  msk;
    char* ptr;
};


RegAlloc3::BoolMatrix::BoolMatrix (MemoryManager& mm, size_t d)
{
    assert(d > 0);
    dim = d;
    dims = (dim*(dim - 1)) >> 4; // /16
    base = new (mm) char[dims];
    clear();
}


void RegAlloc3::BoolMatrix::clear ()                        
{
    memset(base, 0, dims);
}


//========================================================================================
//  Registers implementation
//========================================================================================


//  Parse input parameters (registers available) and build table of the regsiters
//  available for allocalion ('registers').
//
void RegAlloc3::Registers::parse (const char* params)
{
    if (params == 0 || strcmp(params, "ALL") == 0)
    {
#ifdef _EM64T_
        push_back(Constraint(RegName_RAX)
                 |Constraint(RegName_RCX)
                 |Constraint(RegName_RDX)
                 |Constraint(RegName_RBX)
                 |Constraint(RegName_RSI)
                 |Constraint(RegName_RDI)
                 |Constraint(RegName_RBP)
                 |Constraint(RegName_R8)
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
        push_back(Constraint(RegName_XMM0)
                 |Constraint(RegName_XMM1)
                 |Constraint(RegName_XMM2)
                 |Constraint(RegName_XMM3)
                 |Constraint(RegName_XMM4)
                 |Constraint(RegName_XMM5)
                 |Constraint(RegName_XMM6)
                 |Constraint(RegName_XMM7));
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

    for (unsigned i = 0; i != IRMaxRegKinds; ++i)
        indexes[i] = -1;

    for (unsigned i = 0; i != size(); ++i)
        indexes[operator[](i).getKind()] = (int)i;
}


int RegAlloc3::Registers::merge (const Constraint& c, bool add)
{
    if (c.getMask() != 0)
    {
        for (unsigned i = 0; i != size(); ++i)
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


int RegAlloc3::Registers::index (const Constraint& c) const
{
    return indexes[c.getKind() & OpndKind_Reg];
}


//========================================================================================
//  Graph implementation
//========================================================================================


void RegAlloc3::Graph::connect (int x1, int x2) const
{
    at(x1).adjacents->push_back(x2);
    at(x2).adjacents->push_back(x1);
}


int RegAlloc3::Graph::disconnect (int x) const
{
//  Node to be disconnected
    const Opndx& opndx = at(x);
    if (opndx.adjacents->empty())
        return 0;

    int disc = 0;

    for (Indexes::iterator k = opndx.adjacents->begin(); k != opndx.adjacents->end(); ++k)
    {
    //  this node is adjacent to the node to be disconnected
        const Opndx& adjopndx = at(*k);
        if (!adjopndx.adjacents->empty())
        {
            moveNodes(*adjopndx.adjacents, *adjopndx.hiddens, x);
            if (adjopndx.adjacents->empty())
                disc++;
        }
    }

    opndx.hiddens->splice(opndx.hiddens->begin(), *opndx.adjacents);

    return ++disc;
}


void RegAlloc3::Graph::reconnect (int x) const
{
//  Node to be reconnected
    const Opndx& opndx = at(x);

    for (Indexes::iterator k = opndx.hiddens->begin(); k != opndx.hiddens->end(); ++k)
    {
    //  this node was adjacent to the node to be reconnected
        const Opndx& adjopndx = at(*k);
        moveNodes(*adjopndx.hiddens, *adjopndx.adjacents, x);
    }

    opndx.adjacents->splice(opndx.adjacents->begin(), *opndx.hiddens);
}


void RegAlloc3::Graph::moveNodes (Indexes& from, Indexes& to, int x) const
{
    Indexes::iterator i;
    while ((i = find(from.begin(), from.end(), x)) != from.end())
        to.splice(to.begin(), from, i);
}


//========================================================================================
//  RegAlloc3 implementation
//========================================================================================


void RegAlloc3::runImpl ()
{
    getIRManager().fixEdgeProfile();

    registers.parse(getArg("regs"));
    DBGOUT("parameters: " << registers << endl;)

    getArg("SORT", flag_SORT = 2);
    getArg("NEIGBH", flag_NEIGBH = false);
    getArg("COALESCE", flag_COALESCE = true);

    coalesceCount = 0;

    DBGOUT(endl << "passnb 1" << endl;)
    if (buildGraph())
    {
        pruneGraph();
        if (!assignRegs())
        {
            bool flag_SPILL;
            getArg("SPILL", flag_SPILL = false);
            if (flag_SPILL)
            {
                spillRegs();
                getIRManager().calculateLivenessInfo();

                DBGOUT(endl << "passnb 2" << endl;)
                buildGraph();
                pruneGraph();
                assignRegs();
            }
        }
    }

    count_coalesced += coalesceCount;
    
    SpillGen();
}


bool RegAlloc3::verify (bool force)
{   
    bool failed = false;
    if (force || getVerificationLevel() >=2 )
    {
        RegAllocCheck chk(getIRManager());
        if (!chk.run(false))
            failed = true;
        if (!SessionAction::verify(force))
            failed = true;
    }

    return !failed;
}   


void RegAlloc3::SpillGen ()   
{
/***
    bool runSpillGen = false;    

    for (unsigned i = 0, opandcount = getIRManager().getOpndCount(); i != opandcount; ++i)
    {
        Opnd* opnd  = getIRManager().getOpnd(i);
        if (opnd->getConstraint(Opnd::ConstraintKind_Location, OpndSize_Default).isNull())
            if (opnd->getConstraint(Opnd::ConstraintKind_Calculated, OpndSize_Default).getKind() == OpndKind_Memory)
            {
                opnd->assignMemLocation(MemOpndKind_StackAutoLayout, getIRManager().getRegOpnd(STACK_REG), 0);
                DBGOUT("assigned to mem " << *opnd << endl;)
            }
            else
                runSpillGen = true;
    }

    bool* spill_flag = new (getIRManager().getMemoryManager()) bool(runSpillGen);
    getIRManager().setInfo("SpillGen", spill_flag);
    DBGOUT("runSpillGen:" << runSpillGen << endl;)
***/
}


bool RegAlloc3::buildGraph ()   
{
    static CountTime buildGraphTimer("ia32::RegAlloc3::buildGraph");
    AutoTimer tm(buildGraphTimer);

    const unsigned opandcount = getIRManager().getOpndCount();
    graph.resize(0);
    graph.reserve(opandcount);

    Opndx opndx;

//  Scan all the operands available and see if operand is already assigned
//  or need to be assigned

    int* opandmap = new (mm) int[opandcount];

    for (unsigned i = 0; i != opandcount; ++i)
    {
        int mapto = -1;

        Opnd* opnd  = getIRManager().getOpnd(i);
        opndx.opnd   = opnd;
        opndx.spill  = false;
        opndx.ignore = false;

        int ridx;
        Constraint loc = opnd->getConstraint(Opnd::ConstraintKind_Location, OpndSize_Default);
        if (loc.isNull())
        {// this operand is not allocated yet
            loc = opnd->getConstraint(Opnd::ConstraintKind_Calculated, OpndSize_Default);
            if ((ridx = registers.index(loc)) != -1)
            {// operand should be assigned to register
                opndx.ridx  = ridx;
                opndx.alloc = 0;
                opndx.avail = loc.getMask() & registers[ridx].getMask();
                opndx.nbavails = bitCount(opndx.avail);
                assert(opndx.nbavails != 0);
                opndx.spillcost = 1;
                mapto = (int)graph.size();
                graph.push_back(opndx);
            }
        }

        opandmap[i] = mapto;
    }

    if ((graphsize = graph.size()) == 0)
        return false;

//  Create graph node for each register available

    xregbase = graph.size();    // graph index of the first register available
    graph.reserve(graph.size() + registers.size());

    opndx.opnd = 0;
    opndx.spill  = false;
    opndx.ignore = false;
    opndx.ridx  = 0;
    for (Registers::iterator it = registers.begin(), end = registers.end(); it != end; ++it)
    {
        for (RegMask msk = it->getMask(), mk = 1; msk != 0; mk <<= 1)
            if ((msk & mk) != 0)
            {
                msk ^= mk;
                opndx.alloc = mk;
                graph.push_back(opndx);
            }

        ++opndx.ridx;
    }

    graphsize = graph.size();
    BoolMatrix matrix(mm, graphsize);

    for (Graph::iterator i = graph.begin(); i != graph.end(); ++i)
    {
        i->adjacents = new (mm) Indexes(mm);
        i->hiddens   = new (mm) Indexes(mm);
        i->oproles   = new (mm) Oproles(mm);
        i->neighbs   = 0;
    }

//  Iterate over all instructions in CFG and calculate which operands
//  live simultaneously (result stored in matrix)

    BitSet lives(mm, opandcount);

    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) 
    {
        Node* node = *it;
        if (node->isBlockNode())
        {
            Inst* inst = (Inst*)node->getLastInst();
            if (inst == 0)
                continue;

            double excount = node->getExecCount() / irManager->getFlowGraph()->getEntryNode()->getExecCount();
            assert(excount > 0);

        //  start with the operands at the block bottom
            getIRManager().getLiveAtExit(node, lives);

        //  iterate over instructions towards the top of the block
            for (;;)
            {
                processInst(inst, lives, opandmap, matrix, excount);

                if (inst->getPrevInst() == 0)
                    break;

                getIRManager().updateLiveness(inst, lives);
                inst = inst->getPrevInst();
            }
        }
#ifdef _SKIP_CATCHED
        else if (node->isDispatchNode())
        {
            BitSet* tmp = irManager->getLiveAtEntry(node);
            BitSet::IterB ib(*tmp);
            int i;
            for (int x = ib.getNext(); x != -1; x = ib.getNext())
                if ((i = opandmap[x]) != -1)
                {
                    Opndx& opndx = graph.at(i);
                    opndx.ignore = true;
                    DBGOUT("catched " << opndx << endl;)
                }
        }
#endif
    }

//  Detect and ignore not-used operands (e.g. child of coalesced operands)

    for (unsigned x = 0; x < xregbase; ++x)
        if (graph[x].oproles->empty())
            graph[x].ignore = true;

//  Connect nodes that represent simultaneously live operands

    for (unsigned x1 = 1; x1 < graphsize; ++x1)
        for (unsigned x2 = 0; x2 < x1; ++x2)
            if (matrix.test(x1, x2))
                graph.connect(x1, x2);

    showGraph();

//  Do iterative coalescing

    if (flag_COALESCE)
        while (coalescing(opandmap, matrix))
            /*nothing*/;

    showGraph();
    return true;
}


void RegAlloc3::processInst (Inst* inst, BitSet& lives, int* opandmap, BoolMatrix& matrix, double excount)
{
    int defx = -1;
    Oprole oprole;
    Inst::Opnds opnds(inst, Inst::OpndRole_All);
    for (Inst::Opnds::iterator it = opnds.begin(); it != opnds.end(); it = opnds.next(it))
    {
        U_32 role = inst->getOpndRoles(it);
        Opnd*  opnd = inst->getOpnd(it);

    //  For each operand def, look at the all live operand
        if (role & Inst::OpndRole_Def)
            lookLives(opnd, lives, opandmap, matrix);

        int i = opnd->getId();
        int x = opandmap[i];
        if (x != -1)
        {
            Opndx& opndx = graph.at(x);

            if (opndx.oproles->empty() || opndx.oproles->back().inst != inst)
            {
                oprole.inst = inst;
                oprole.role = role;
                opndx.oproles->push_back(oprole);
            }
            else
                opndx.oproles->back().role |= role;

            if (role & Inst::OpndRole_Def)
            {
                defx = x;
            }
            else if (flag_NEIGBH && defx != -1 && !lives.getBit(i))
            {
                Opndx& defopndx = graph.at(defx);
                if (defopndx.neighbs == 0)
                    defopndx.neighbs = new (mm) Indexes(mm);
                defopndx.neighbs->push_back(x);

                if (opndx.neighbs == 0)
                    opndx.neighbs = new (mm) Indexes(mm);
                opndx.neighbs->push_back(defx);
            }

            opndx.spillcost += excount;
        }
    }
}


//  Look for operands live at defining point 
//
void RegAlloc3::lookLives (Opnd* opnd, BitSet& lives, int* opandmap, BoolMatrix& matrix)
{
    int i = opnd->getId();
    int x;
    if ((x = opandmap[i]) == -1 && (x = findNode(opnd)) == -1)
        return;

    BitSet::IterB bsk(lives);
    int k, y;
    for (k = bsk.getNext(); k != -1; k = bsk.getNext())
        if (k != i)
            if ((y = opandmap[k]) != -1 || (y = findNode(irManager->getOpnd(k))) != -1)
                matrix.set(x, y);
}


int RegAlloc3::findNode (Opnd* opnd) const
{
    Constraint loc = opnd->getConstraint(Opnd::ConstraintKind_Location);
    if (!loc.isNull())
    {
        int ridx = registers.index(loc);
        RegMask msk = loc.getMask();

        for (size_t x = xregbase; x != graph.size(); ++x)
        {
            const Opndx& opndx = graph[x];
            assert(opndx.alloc != 0);
            if (opndx.ridx == ridx && opndx.alloc == msk)
                return (int)x;
        }
    }

    return -1;
}


void RegAlloc3::showGraph ()
{
#ifdef _DEBUG_REGALLOC3
    log(LogStream::DBG) << "--- graph" << endl;
    for (unsigned x = 0; x != graphsize; ++x)
    {
        const Opndx& opndx = graph.at(x);
        log(LogStream::DBG).out()
            << "(" << x << ") "
            << (opndx.ignore ? "IGNORE " : "");

            if (opndx.opnd != 0)
                log(LogStream::DBG).out() << *opndx.opnd;
            else
                log(LogStream::DBG).out() << "REG";

        log(LogStream::DBG).out()
            << " ridx:" << opndx.ridx 
            << " avail:" << hex << opndx.avail << " alloc:" << opndx.alloc << dec 
            //<< " nbavails:" << opndx.nbavails
            << " spillcost:" << opndx.spillcost;

        if (!opndx.adjacents->empty())
        {
            Sep s;
            log(LogStream::DBG) << " adjacents{";
            for (RegAlloc3::Indexes::const_iterator i = opndx.adjacents->begin(); i != opndx.adjacents->end(); ++i)
                log(LogStream::DBG).out() << s << *i;
            log(LogStream::DBG) << "}";
        }

        if (opndx.neighbs != 0)
        {
            Sep s;
            log(LogStream::DBG) << " neighbs{";
            for (RegAlloc3::Indexes::const_iterator i = opndx.neighbs->begin(); i != opndx.neighbs->end(); ++i)
                log(LogStream::DBG).out() << s << *i;
            log(LogStream::DBG) << "}";
        }

        log(LogStream::DBG) << endl;
    }
    log(LogStream::DBG) << "---------" << endl;
#endif
}


bool RegAlloc3::coalescing (int* opandmap, BoolMatrix& matrix)
{
    //static CountTime coalescingTimer("ia32::RegAlloc3::coalescing");
    //AutoTimer tm(coalescingTimer);

    int x0, x1;

    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) 
    {
        Node* node = *it;
        if (node->isBlockNode())
            for (const Inst*  inst = (Inst*)node->getLastInst(); inst != 0; inst = inst->getPrevInst())
                if (inst->getMnemonic() == Mnemonic_MOV)
                    if ((x0 = opandmap[inst->getOpnd(0)->getId()]) != -1 &&
                        (x1 = opandmap[inst->getOpnd(1)->getId()]) != -1 &&
                        x0 != x1 && !matrix.test(x0, x1))
                    {
                        Opndx& opndx0 = graph.at(x0),
                             & opndx1 = graph.at(x1);

                        RegMask  avail = opndx0.avail & opndx1.avail;
                        unsigned nbavails = bitCount(avail);

                        if (opndx0.ridx != opndx1.ridx || nbavails == 0)
                            continue;

                        //if (opndx0.opnd->getSize() != opndx1.opnd->getSize())
                        //    continue;

                        Type* t0 = opndx0.opnd->getType(),
                            * t1 = opndx1.opnd->getType();

                        //if ((t0->isManagedPtr() || t0->isObject()) != (t1->isManagedPtr() || t1->isObject()))
                        //    continue;

                        if (!Type::mayAlias(&irManager->getTypeManager(), t0, t1))
                            continue;

                        DBGOUT("coalesce candidates (" << x0 << ") & (" << x1 << ") " << *inst << endl;)

                        size_t   xdegree = 0, // estimated degree of the coalesced node
                                 xcount  = 0; // number of neighbours with degree >= k

                        xdegree = opndx0.adjacents->size() + opndx1.adjacents->size() 
                                  - duplicates(opndx1.adjacents, matrix, x0, x1);

                        for (Indexes::iterator ptr = opndx0.adjacents->begin(), end = opndx0.adjacents->end(); ptr != end; ++ptr)
                        {
                            Indexes* ixs = graph.at(*ptr).adjacents;
                            size_t ndegree = ixs->size() - duplicates(ixs, matrix, x0, x1);
                            if (ndegree >= nbavails)
                                if (++xcount >= nbavails)
                                    break;
                        }
                        for (Indexes::iterator ptr = opndx1.adjacents->begin(), end = opndx1.adjacents->end(); ptr != end; ++ptr)
                            if (!matrix.test(*ptr, x0))
                            {
                                Indexes* ixs = graph.at(*ptr).adjacents;
                                size_t ndegree = ixs->size();
                                if (ndegree >= nbavails)
                                    if (++xcount >= nbavails)
                                        break;
                            }

                        DBGOUT("xdegree:" << xdegree << " xcount:" << xcount << endl;)

                        if (xcount >= nbavails || xdegree >= nbavails)
                            continue;

                        coalesce (opandmap, matrix, x0, x1);
                        return true;
                    }
    }

    return false;
}


//  Coalesce graph nodes (x0) and (x1) and the corresponding operands.
//  Node (x1) not to be used anymore, (x0) must be used instead.
//  Note that (x1) remains in the graph (must be ignored)
//
void RegAlloc3::coalesce (int* opandmap, BoolMatrix& matrix, int x0, int x1)
{
    DBGOUT("*coalescing (" << x0 << ") with (" << x1 << ")" << endl;)

    Opndx& opndx0 = graph.at(x0),
         & opndx1 = graph.at(x1);

    opndx0.avail &= opndx1.avail;
    opndx0.nbavails = bitCount(opndx0.avail);

    opndx1.ignore = true;

    Opnd* opnd0 = opndx0.opnd,
        * opnd1 = opndx1.opnd;

    opandmap[opnd1->getId()] = x0;

    Oproles::iterator it  = opndx1.oproles->begin(),
                      end = opndx1.oproles->end();
    for (; it != end; ++it)
        it->inst->replaceOpnd(opnd1, opnd0);

    opndx0.oproles->reserve(opndx0.oproles->size() + opndx1.oproles->size());
    opndx0.oproles->insert(opndx0.oproles->end(), opndx1.oproles->begin(), opndx1.oproles->end());
    opndx1.oproles->clear();

    Indexes tmp(mm);    // list of trash indexes

    for (Indexes::iterator ptr = opndx1.adjacents->begin(), end = opndx1.adjacents->end(); ptr != end;)
    {
        Indexes::iterator ptr_next = ptr;
        ++ptr_next;

        int x = *ptr;
        assert(matrix.test(x, x1));
        matrix.clear(x, x1);

        Opndx& opndx = graph.at(x);
        Indexes::iterator ptrx = find(opndx.adjacents->begin(), opndx.adjacents->end(), x1);
        assert(ptrx != opndx.adjacents->end());

        if (matrix.test(x, x0))
        {// disconnect (x1 - x)
            tmp.splice(tmp.end(), *opndx1.adjacents, ptr);
            tmp.splice(tmp.end(), *opndx.adjacents, ptrx);
        }
        else
        {// connect (x0 - x)
            matrix.set(x, x0);
            opndx0.adjacents->splice(opndx0.adjacents->end(), *opndx1.adjacents, ptr);  // x0 -> x
            *ptrx = x0;                                                                 // x  -> x0
        }

        ptr = ptr_next;
    }

    assert(opndx1.adjacents->empty());

    ++coalesceCount;
}


int RegAlloc3::duplicates (RegAlloc3::Indexes* list, RegAlloc3::BoolMatrix& matrix, int x0, int x1)
{
    int count = 0;

    for (RegAlloc3::Indexes::iterator ptr = list->begin(), end = list->end(); ptr != end; ++ptr)
        if (*ptr != x0 && *ptr != x1)
            if (matrix.test(*ptr, x0) && matrix.test(*ptr, x1))
                ++count;

    return count;
}


struct sortRule1
{
    const RegAlloc3::Graph& graph;
    const unsigned int rule;

    sortRule1 (const RegAlloc3::Graph& g, unsigned int r)   :graph(g), rule(r) {}

    bool operator () (int x1, int x2)
    {
        const RegAlloc3::Opndx& opndx1 = graph.at(x1),
                              & opndx2 = graph.at(x2);

        return rule == 1 ? opndx1.spillcost > opndx2.spillcost
                         : opndx1.spillcost < opndx2.spillcost;
    }
};


void RegAlloc3::pruneGraph ()
{
    static CountTime pruneGraphTimer("ia32::RegAlloc3::pruneGraph");
    AutoTimer tm(pruneGraphTimer);

    DBGOUT(endl << "pruneGraph"<< endl;)

//  Calculate number of nodes that should be pruned off the graph
    int nbnodes = 0;
    for (unsigned i = 0; i != graphsize; ++i)
        if (shouldPrune(graph.at(i)))
            nbnodes++;

    StlVector<int> tmp(mm);

    nstack.reserve(nbnodes);
    while (nbnodes > 0)
    {
    //  Apply degree < R rule

        if (flag_SORT == 0)

            for (bool succ = false; !succ;)
            {
                succ = true;
                for (unsigned i = 0; i != graphsize; ++i)
                {
                    Opndx& opndx = graph.at(i);
                    if (shouldPrune(opndx))
                    {
                        const size_t n = opndx.adjacents->size();
                        if (n != 0 && n < opndx.nbavails)
                        {
                            nbnodes -= graph.disconnect(i);
                            nstack.push_back(i);
                            succ = false;
                            //DBGOUT(" rule#1 (" << i << ")" << endl;)
                        }
                    }
                }
            }

        else

            for (bool succ = false; !succ;)
            {
                succ = true;

                tmp.resize(0);

                for (unsigned i = 0; i != graphsize; ++i)
                {
                    Opndx& opndx = graph.at(i);
                    if (shouldPrune(opndx))
                    {
                        const size_t n = opndx.adjacents->size();
                        if (n != 0 && n < opndx.nbavails)
                            tmp.push_back(i);
                    }
                }

                if (tmp.size() != 0)
                {
                    if (tmp.size() > 1)
                        sort(tmp.begin(), tmp.end(), sortRule1(graph, flag_SORT));

                    for (StlVector<int>::iterator it = tmp.begin(); it != tmp.end(); ++it)
                    {
                        nbnodes -= graph.disconnect(*it);
                        nstack.push_back(*it);
                    }

                    succ = false;
                }
            }

    //  Apply degree >= R rule

        if (nbnodes > 0)
        {
            int x = -1, n;
            double cost = 0, w;

        //  Find some node to disconnect 
            for (unsigned i = 0; i != graphsize; ++i)
            {
                Opndx& opndx = graph.at(i);
                if (shouldPrune(opndx))
                    if ((n = (int)opndx.adjacents->size()) != 0)
                    {
                        w = opndx.spillcost/(double)n;
                        if (x == -1 || w < cost)
                            cost = w,
                            x    = i;
                    }
            }

            assert(x != -1);
            if (x != -1)
            {
                nbnodes -= graph.disconnect(x);
                nstack.push_back(x);
            }
        }
    }
}


bool RegAlloc3::shouldPrune (const Opndx& opndx) const
{
    return !opndx.ignore && opndx.alloc == 0 && !opndx.adjacents->empty();
}


bool RegAlloc3::assignRegs ()
{
    static CountTime assignRegsTimer("ia32::RegAlloc3::assignRegs");
    AutoTimer tm(assignRegsTimer);

    DBGOUT("assignRegs" << endl;)

    int spilled = 0;

    while (!nstack.empty())
    {
        int x = nstack.back();
        nstack.pop_back();

        Opndx& opndx = graph.at(x);
        graph.reconnect(x);
        if (opndx.alloc == 0)
        {
            DBGOUT("(" << x << ")" << endl;)
            opndx.spill = !assignReg(opndx);
        }
    }

    for (unsigned x = 0; x != graphsize; ++x)
    {
        Opndx& opndx = graph.at(x);
        if (opndx.alloc == 0 && !opndx.spill && !opndx.ignore)
        {
            DBGOUT("(" << x << ")" << endl;)
            opndx.spill = !assignReg(opndx);
        }

        if (opndx.spill)
            ++spilled;
    }

    DBGOUT("spilled " << spilled << " operands" << endl;)

    return spilled == 0;
}

RegAlloc3::RegMask RegAlloc3::occupiedReg (OpndSize tgtSize, OpndSize adjSize, RegAlloc3::RegMask adjMask) {
#if !defined(_EM64T_)    
    if (!((tgtSize != adjSize) && ((tgtSize == OpndSize_8) || (adjSize == OpndSize_8))))
#endif
        return adjMask;

    RegMask val = adjMask;
    if (tgtSize == OpndSize_8) {
        if (adjMask <= 8) //for AH, CH, DH, BH
            val |= adjMask<<4;
    } else if (adjSize == OpndSize_8) {
        if (adjMask >= 16) //for AH, CH, DH, BH
            val >>= 4;
    }
    return val;
}


bool RegAlloc3::assignReg (Opndx& opndx)
{
    RegMask alloc = 0;

    assert(!opndx.ignore);
    for (Indexes::iterator i = opndx.adjacents->begin(); i != opndx.adjacents->end(); ++i)
    {
        Opndx& opndz = graph.at(*i);
        if (opndz.ridx == opndx.ridx)
        {
            if (opndz.opnd != NULL) //for operand nodes
                alloc |= occupiedReg(opndx.opnd->getSize(), opndz.opnd->getSize(), opndz.alloc);
            else //for color nodes
                alloc |= occupiedReg(opndx.opnd->getSize(), OpndSize_32, opndz.alloc);
        }
    }

    if ((alloc = opndx.avail & ~alloc) == 0)
    {
        DBGOUT("  assign " << *opndx.opnd << " failed" << endl;)
        return false;
    }
    else
    {
        if (opndx.neighbs != 0)
        {
            RegMask neighbs = 0;
            for (Indexes::iterator i = opndx.neighbs->begin(); i != opndx.neighbs->end(); ++i)
            {
                Opndx& neigbx = graph.at(*i);
                if (neigbx.ridx == opndx.ridx)
                    neighbs |= neigbx.alloc;
            }

            if ((neighbs & alloc) != 0 && neighbs != alloc)
            {
                DBGOUT("  !alloc:" << std::hex << alloc << " * neighbs:"  << neighbs << " =" << (alloc & neighbs) << std::dec << endl);
                alloc &= neighbs;
            }
        }

        opndx.alloc = findHighest(alloc);
        opndx.opnd->assignRegName(getRegName((OpndKind)registers[opndx.ridx].getKind(), 
                                             opndx.opnd->getSize(), 
                                             bitNumber(opndx.alloc)));

        ++count_assigned;
        DBGOUT("  assigned " << *opndx.opnd << endl;)
        return true;
    }
}


void RegAlloc3::spillRegs ()
{
    DBGOUT("spillRegs" << endl;)

    int inserted = 0;

    for (unsigned x = 0; x != graphsize; ++x)
    {
        Opndx& opndx = graph.at(x);
        if (opndx.spill)
            inserted += spillReg(opndx);
    }

    DBGOUT("inserted " << inserted << " operands" << endl;)
}


int RegAlloc3::spillReg (Opndx& opndx)
{
    Opnd* opnd = opndx.opnd;
    const Constraint initial = opnd->getConstraint(Opnd::ConstraintKind_Initial);

    if ((initial.getKind() & OpndKind_Memory) == 0)
    {
        DBGOUT("  spilling " << *opndx.opnd << " failed" << endl;)
        return 0;
    }

    DBGOUT("  spilling " << *opndx.opnd << endl;)
    opnd->setCalculatedConstraint(initial);
    opnd->assignMemLocation(MemOpndKind_StackAutoLayout, irManager->getRegOpnd(STACK_REG), 0);
    if (initial.getKind() == OpndKind_FPReg
        || initial.getKind() == OpndKind_XMMReg) {
        opnd->setMemOpndAlignment(Opnd::MemOpndAlignment_16);
    }

    int inserted = 0;

    for (Oproles::iterator ptr = opndx.oproles->begin(), end = opndx.oproles->end(); ptr != end; ++ptr)
    {
        Opnd* opndnew = getIRManager().newOpnd(opnd->getType(), initial);
        Inst* inst = ptr->inst;
        Inst* instnew = 0;
        bool replaced = false;
        if (ptr->role & Inst::OpndRole_Use)
        {
            instnew = getIRManager().newCopyPseudoInst(Mnemonic_MOV, opndnew, opnd);
            instnew->insertBefore(inst);
            replaced = inst->replaceOpnd(opnd, opndnew);
            assert(replaced);
            DBGOUT("    before " << *inst << " inserted " << *instnew << " MOV " << *opndnew << ", " << *opnd << endl;)
        }

        if (ptr->role & Inst::OpndRole_Def)
        {
            assert(!inst->hasKind(Inst::Kind_LocalControlTransferInst));  
            if (!replaced)
                replaced = inst->replaceOpnd(opnd, opndnew);
            assert(replaced);
            instnew = getIRManager().newCopyPseudoInst(Mnemonic_MOV, opnd, opndnew);
            instnew->insertAfter(inst);
            DBGOUT("    after  " << *inst << " inserted " << *instnew << " MOV " << *opnd << ", " << *opndnew << endl;)
        }

        Constraint c = initial;
        update(instnew, opndnew, c);
        update(inst,    opndnew, c);
        opndnew->setCalculatedConstraint(c);

        ++inserted;
    }

    ++count_spilled;
    return inserted;
}


//  If currently handled operand is referenced by current instruction, then evaluate
//  constraint of the operand imposed by this instruction and return 'true'.
//  Otherwise, do nothing and return false.
//
int RegAlloc3::update (const Inst* inst, const Opnd* opnd, Constraint& constr) const
{
    int count = 0;
    Inst::Opnds opnds(inst, Inst::OpndRole_All);
    for (Inst::Opnds::iterator it = opnds.begin(); it != opnds.end(); it = opnds.next(it))
        if ( inst->getOpnd(it) == opnd)
        {
            Constraint c = inst->getConstraint(it, 0, constr.getSize());
            if (constr.isNull())
                constr = c;
            else
                constr.intersectWith(c);

            count++;    
        }
    return count;
}


//========================================================================================
//  Output formatters
//========================================================================================


#ifdef _DEBUG_REGALLOC3

static ostream& operator << (ostream& os, Sep& x)
{
    if (x.first)
        x.first = false;
    else
        os << ",";
    return os;
}

static ostream& operator << (ostream& os, const Inst& x)
{
    return os << "I#" << x.getId();
}


static ostream& operator << (ostream& os, const Opnd& x)
{
    os << "O#" << x.getFirstId();
    RegName rn = x.getRegName();
    if (rn != RegName_Null)
        os << "<" << getRegNameString(rn) << ">";
    if (x.isPlacedIn(OpndKind_Memory))
        os << "<mem>";
    return os;
}


static ostream& operator << (ostream& os, Constraint c)
{
    IRPrinter::printConstraint(os, c); 
    return os;
}


static ostream& operator << (ostream& os, const RegAlloc3::Registers& x)
{
    Sep s;;
    os << "{";
    for (RegAlloc3::Registers::const_iterator it = x.begin(); it != x.end(); ++it)
        os << s << *it;
    return os << "}";
}


static ostream& operator << (ostream& os, RegMasks x)
{
    return os << x.c;
}


static ostream& outRegMasks (ostream& os, RegAlloc3::RegMask* x, const RegAlloc3::Registers& registers)
{
    Sep s;;
    os << "{";
    for (unsigned rk = 0; rk != registers.size(); ++rk)
    {
        RegAlloc3::RegMask msk = x[rk];

        for (unsigned rx = 0; msk != 0; ++rx, msk >>= 1)
            if ((msk & 1) != 0)
            {
                RegName reg = getRegName((OpndKind)registers[rk].getKind(), registers[rk].getSize(), rx);
                os<< s << getRegNameString(reg);
            }
    }
    return os << "}";
}


static ostream& operator << (ostream& os, const RegAlloc3::Opndx& opndx)
{
    RegName reg;
    if ((reg = opndx.opnd->getRegName()) != RegName_Null)
        os << " <" << getRegNameString(reg) << ">";

    if (!opndx.adjacents->empty())
    {
        Sep s;
        os << " adjacents{";
        for (RegAlloc3::Indexes::const_iterator i = opndx.adjacents->begin(); i != opndx.adjacents->end(); ++i)
            os << s << *i;
        os << "}";
    }

    if (!opndx.hiddens->empty())
    {
        Sep s;
        os << " hiddens{";
        for (RegAlloc3::Indexes::const_iterator i = opndx.hiddens->begin(); i != opndx.hiddens->end(); ++i)
            os << s << *i;
        os << "}";
    }

    return os;
}


static ostream& operator << (ostream& os, const RegAlloc3::Graph& graph)
{
    int x = 0;
    for (RegAlloc3::Graph::const_iterator i = graph.begin(); i != graph.end(); ++i)
        os << x++ << ") " << *i << endl;

    return os;
}


#endif //#ifdef _DEBUG_REGALLOC3

} //namespace Ia32
} //namespace Jitrino
