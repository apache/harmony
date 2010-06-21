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
#include "Ia32ConstraintsResolver.h"
#include "XTimer.h"
#include "Counter.h"
#include "Stl.h"

#ifdef _DEBUG_WEBMAKER
#include <iostream>
#include <iomanip>
#ifdef _MSC_VER
#pragma warning(disable : 4505)   //unreferenced local function has been removed
#endif //#ifdef _MSC_VER
#endif //#ifdef _DEBUG_WEBMAKER


using namespace std;

namespace Jitrino
{

namespace Ia32
{


struct WebMaker : public SessionAction
{
    MemoryManager mm;       // this is private MemoryManager, not irm.getMemoryManager()

    struct OpDef
    {
        int globid;         // -1 for local definition or uniquue id (starting from 0)
        Inst* defp;         // defining instruction
        unsigned linkx;
        bool visited;

        typedef StlVector<Inst*> Instps;
        Instps* useps;      // for local operands always 0
    };

    struct Opndx
    {
        Opndx (MemoryManager& mm)           :opdefs(mm), globdefsp(0), cbbp(0), webscount(0) {}

        typedef StlVector<OpDef> OpDefs;
        OpDefs opdefs;      // array of all definitions

        BitSet* globdefsp;  // bitmask of all global definitions or 0

        const Node* cbbp;
        OpDef* copdefp;
        int webscount;
        Opnd* newopndp;
    };

    typedef StlVector <Opndx*> Opndxs;
    Opndxs opndxs;


    struct Nodex
    {
        Nodex (MemoryManager& mm, unsigned s) :globentrys(mm, s), 
                                               globdefsp (0), 
                                               globkillsp(0),
                                               globexitsp(0) {}

        BitSet globentrys;  // all global definitions available at the block entry
        BitSet* globdefsp,  // can be 0
              * globkillsp, // can be 0
              * globexitsp; // 0 if globdefsp = globkillsp = 0 (globentrys must be used instead)
    };

    typedef StlVector <Nodex*> Nodexs;
    Nodexs nodexs;

    /*const*/ unsigned opandcount;
    /*const*/ unsigned nodecount;
    unsigned splitcount;
    unsigned globcount;


    WebMaker ()                     :mm("WebMaker"), opndxs(mm), nodexs(mm) {}

    U_32 getNeedInfo () const     {return NeedInfo_LivenessInfo;}
    U_32 getSideEffects () const  {return splitcount == 0 ? 0 : SideEffect_InvalidatesLivenessInfo;}

    void runImpl();
    void calculateConstraints();

    void phase1();
    void phase2();
    void phase3();
    void phase4();
    void linkDef(Opndx::OpDefs&, OpDef*, OpDef*);
    BitSet* bitsetp (BitSet*&);
    Opnd* splitOpnd (const Opnd*);
};


static ActionFactory<WebMaker> _webmaker("webmaker");

static Counter<int> count_splitted("ia32:webmaker:splitted", 0);


//========================================================================================
// Internal debug helpers
//========================================================================================


using std::endl;
using std::ostream;

#ifdef _DEBUG_WEBMAKER

struct Sep
{
    Sep ()      :first(true) {}

    bool first;
};

static ostream& operator << (ostream&, Sep&);

static ostream& operator << (ostream&, const Inst&);

static ostream& operator << (ostream&, const Opnd&);

static ostream& operator << (ostream&, /*const*/ BitSet*);

#define DBGOUT(s) log(LogStream::DBG).out() << s

#else

#define DBGOUT(s) 

#endif


//========================================================================================
//  Inst::Opnds
//========================================================================================

class InstOpnds
{
public:

    InstOpnds (const Inst* inst, U_32 roles = Inst::OpndRole_All, bool forw = true);

    bool hasMore () const           {return opnd != 0;}
    void next ()                    {move();}
    Opnd*  getOpnd () const         {return opnd;}
    U_32 getRole () const         {return role;}

protected:

    bool move ();

    const Inst*  inst;
    const U_32 roles;
    const unsigned main_count;

    unsigned state,
             main_idx,
              sub_idx;

    U_32 role;
    Opnd* opnd;
    Opnd* main_opnd;
};


InstOpnds::InstOpnds (const Inst* i, U_32 r, bool forw)
:inst(i), roles(r), main_count(i->getOpndCount())
{
    if (main_count == 0)
    {
        state = 6;
        opnd = 0;
    }
    else if (forw)
    {
        main_idx = 0;
        state = 0;
        move();
    }
    else
    {
        main_idx = main_count;
        state = 3;
        move();
    }
}


bool InstOpnds::move ()
{
    opnd = 0;

    do
        switch (state)
        {
        //  forward iteration

            case 0:     // main operands
                opnd = inst->getOpnd(main_idx);
                role = inst->getOpndRoles(main_idx);
                if (++main_idx == main_count)
                {
                    main_idx = 0;
                    state = 1;
                }
                return true;

            case 1:     // find next memory operand
                for (;; ++main_idx)
                {
                    if (main_idx == main_count)
                    {
                        state = 6;
                        return false;
                    }

                    main_opnd = inst->getOpnd(main_idx);
                    if (main_opnd->getMemOpndKind() != MemOpndKind_Null)
                        break;
                }

                sub_idx = 0;
                state = 2;
                // fall to case 2

            case 2:     // sub operands
                opnd = main_opnd->getMemOpndSubOpnd((MemOpndSubOpndKind)sub_idx);
                role = Inst::OpndRole_OpndLevel | Inst::OpndRole_Use;
                if (++sub_idx == 4)
                {
                    ++main_idx;
                    state = 1;
                }
                break;

        //  backward iteration

            case 3:     // find prev memory operand
                for (;;)
                {
                    if (main_idx == 0)
                    {
                        main_idx = main_count;
                        state = 5;
                        goto S5;
                    }

                    main_opnd = inst->getOpnd(--main_idx);
                    if (main_opnd->getMemOpndKind() != MemOpndKind_Null)
                        break;
                }

                sub_idx = 4;
                state = 4;
                // fall to case 4

            case 4:     // sub operands
                opnd = main_opnd->getMemOpndSubOpnd((MemOpndSubOpndKind)--sub_idx);
                role = Inst::OpndRole_OpndLevel | Inst::OpndRole_Use;
                if (sub_idx == 0)
                    state = 3;
                break;

            case 5:     // main operands
S5:             opnd = inst->getOpnd(--main_idx);
                role = inst->getOpndRoles(main_idx);
                if (main_idx == 0)
                    state = 6;
                return true;

            case 6:
                return false;
        }
    while (opnd == 0 /*TBD: check roles here */);

    return true;
}


//========================================================================================
//  WebMaker implementation
//========================================================================================


void WebMaker::runImpl()
{
    opandcount = irManager->getOpndCount();
    nodecount = irManager->getFlowGraph()->getMaxNodeId();

    splitcount = 0;

    phase1();

//TBD   Local definitions also can be splitted!

    if (globcount != 0)
    {
        phase2();
        phase3();
        phase4();

        if (splitcount != 0 && getBoolArg("calc", false))
            calculateConstraints();
    }

    count_splitted += splitcount;
    DBGOUT("splitcount=" << splitcount << "/" << count_splitted << endl;)
}


void WebMaker::calculateConstraints ()
{
    DBGOUT("Calculating constraints" << endl;)

    irManager->calculateLivenessInfo();
    ConstraintsResolverImpl impl(*irManager, true);
    impl.run();
}


void WebMaker::phase1()
{
    opndxs.resize(opandcount);
    for (unsigned i = 0; i != opandcount; ++i)
    {
        Opndx* opndxp = 0;
        if (!irManager->getOpnd(i)->hasAssignedPhysicalLocation())
            opndxp = new (mm) Opndx(mm);
        opndxs[i] = opndxp;
    }

    BitSet lives(mm, opandcount);
    globcount = 0;

    const Nodes& postOrder = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_iterator it = postOrder.begin(), end = postOrder.end(); it!=end; ++it) 
    {
        Node* nodep = *it;
        
        if (nodep->isBlockNode())
        {
            for (Inst* instp = (Inst*)nodep->getFirstInst(); instp!=NULL; instp = instp->getNextInst()) {
                const U_32 iprops = instp->getProperties();
                Inst::Opnds defs(instp, Inst::OpndRole_AllDefs);
                unsigned itx = 0;
                for (Inst::Opnds::iterator it = defs.begin(); it != defs.end(); it = defs.next(it), ++itx)
                //for (InstOpnds inops(instp, Inst::OpndRole_All, true); inops.hasMore(); inops.next())
                {
                    Opnd* opndp = instp->getOpnd(it);
                    //Opnd* opndp = inops.getOpnd();
                    Opndx* opndxp = opndxs.at(opndp->getId());
                    if (opndxp != 0)
                    {
                        const U_32 oprole = const_cast<const Inst*>(instp)->getOpndRoles(itx);
                        //const U_32 oprole = inops.getRole();
                        const bool isdef = ((oprole & Inst::OpndRole_UseDef) == Inst::OpndRole_Def)
                                        && ((iprops & Inst::Properties_Conditional) == 0 );
                        if (isdef)
                        {// register the new definition
                            opndxp->opdefs.push_back(OpDef());
                            OpDef& opdef = opndxp->opdefs.back();
                            opdef.visited = false;
                            opdef.linkx = (unsigned int)(&opdef - &opndxp->opdefs.front());
                            opdef.globid = -1;
                            opdef.defp   = instp;
                            opdef.useps  = 0;
                        }
                    }
                }
            }

        //  Mark all global definitions

            irManager->getLiveAtExit(nodep, lives);
            BitSet::IterB bsk(lives);
            for (int i = bsk.getNext(); i != -1; i = bsk.getNext())
            {
                Opnd* opndp = irManager->getOpnd(i);
                Opndx*& opndxp = opndxs.at(opndp->getId());
                if (opndxp != 0 && !opndxp->opdefs.empty())
                {
                    OpDef& opdef = opndxp->opdefs.back();
                    if (opdef.defp->getNode() == nodep)
                    {
                        opdef.globid = (int)globcount++;
                        bitsetp(opndxp->globdefsp)->setBit(opdef.globid, true);
                    }
                }
            }
        }
    }

#ifdef _DEBUG_WEBMAKER
/*
    dbgout << "--- phase1 ---" << endl;
    for (size_t i = 0; i != opandcount; ++i)
        if (opndxs[i] != 0)
        {
            dbgout << " O#" << irManager->getOpnd(i)->getFirstId() << " (#" << i << ")" << endl;
            Opndx::OpDefs opdefs = opndxs[i]->opdefs;
            for (Opndx::OpDefs::iterator it = opdefs.begin(); it != opdefs.end(); ++it)
            {
                OpDef& opdef = *it;
                dbgout << "  linkx#" << opdef.linkx << " globid#" << opdef.globid 
                    << " opdef B#" << opdef.defp->getBasicBlock()->getId() << " " << *opdef.defp << endl;
            }
        }
    dbgout << "--- opandcount:" << opandcount << " globcount:" << globcount << endl;
*/
#endif
}


void WebMaker::phase2()
{
    nodexs.resize(nodecount);
    for (size_t n = 0; n != nodecount; ++n)
        nodexs[n] = new (mm) Nodex(mm, globcount);

    Opndx* opndxp;
    for (size_t i = 0; i != opandcount; ++i)
        if ((opndxp = opndxs[i]) != 0  && !opndxp->opdefs.empty())
        {
            Opndx::OpDefs::iterator it = opndxp->opdefs.begin(),
                                   end = opndxp->opdefs.end();
            for (; it != end; ++it)
            {
                OpDef& opdef = *it;
                Nodex* nodexp = nodexs.at(opdef.defp->getNode()->getId());

                if (opdef.globid != -1)
                    bitsetp(nodexp->globdefsp)->setBit(opdef.globid, true);

                if (opndxp->globdefsp != 0)
                    bitsetp(nodexp->globkillsp)->unionWith(*bitsetp(opndxp->globdefsp));
            }

        //  prepare for pass3
            opndxp->cbbp = 0;
            opndxp->copdefp = 0;
        }
    
    BitSet wrkbs(mm, (U_32)globcount);
    bool   wrkbsvalid;
    size_t passnb = 0;
    const Nodes& postOrder = irManager->getFlowGraph()->getNodesPostOrder();
    for (bool changes = true; changes; ++passnb)
    {
        changes = false;
        
        for (Nodes::const_reverse_iterator it = postOrder.rbegin(),end = postOrder.rend(); it!=end; ++it) 
        {
            Node* nodep = *it;
            Nodex* nodexp = nodexs[nodep->getId()];

            wrkbsvalid = false;

            const Edges& edges = nodep->getInEdges();
            size_t edgecount = 0;
            for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite, ++edgecount) {
                Edge* edgep = *ite;
                Node*  predp  = edgep->getSourceNode();
                Nodex* predxp = nodexs[predp->getId()];

                BitSet* predbsp = predxp->globexitsp;
                if (predbsp == 0)
                    predbsp = &predxp->globentrys;

                if (edgecount == 0) 
                {
                    wrkbs.copyFrom(*predbsp);
                    wrkbsvalid = true;
                }
                else 
                    wrkbs.unionWith(*predbsp);
            }

            if (passnb > 0 && (!wrkbsvalid || nodexp->globentrys.isEqual(wrkbs)))
                continue;

            if (!wrkbsvalid)
                wrkbs.clear();

            if (!nodexp->globentrys.isEqual(wrkbs))
            {
                nodexp->globentrys.copyFrom(wrkbs);
                changes = true;
            }

            if (nodexp->globkillsp != 0 && nodexp->globdefsp != 0)
            {
                if (nodexp->globkillsp != 0)
                    wrkbs.subtract(*nodexp->globkillsp);

                if (nodexp->globdefsp != 0)
                    wrkbs.unionWith(*nodexp->globdefsp);

                if (!bitsetp(nodexp->globexitsp)->isEqual(wrkbs))
                {
                    nodexp->globexitsp->copyFrom(wrkbs);
                    changes = true;
                }
            }
        }
    }

#ifdef _DEBUG_WEBMAKER
/*
    dbgout << "--- total passes:" << passnb << endl;
    for (size_t n = 0; n != nodecount; ++n)
    {
        Nodex* nodexp = nodexs[n];
        dbgout <<"  node#" << n << endl;
        dbgout <<"    entry " << &nodexp->globentrys << endl;
        if (nodexp->globexitsp != 0)
            dbgout <<"    exit  " << nodexp->globexitsp << endl;
    }
*/
#endif
}


void WebMaker::phase3()
{
    const Nodes& postOrder  = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_iterator it = postOrder.begin(), end = postOrder.end(); it!=end; ++it) 
    {
        Node* nodep = *it;
        if (nodep->isBlockNode())
        {
            const BitSet*  globentryp = &nodexs.at(nodep->getId())->globentrys;
            
            for (Inst* instp = (Inst*)nodep->getFirstInst(); instp != 0; instp = instp->getNextInst()) 
            {
                const U_32 iprops = instp->getProperties();

                for (InstOpnds inops(instp, Inst::OpndRole_All, false); inops.hasMore(); inops.next())
                {
                    Opnd* opndp = inops.getOpnd();
                    Opndx* opndxp = opndxs.at(opndp->getId());
                    if (opndxp != 0)
                    {
                        OpDef* opdefp = 0;
                        const U_32 oprole = inops.getRole();
                        const bool isdef = ((oprole & Inst::OpndRole_UseDef) == Inst::OpndRole_Def)
                                        && ((iprops & Inst::Properties_Conditional) == 0 );
                        DBGOUT(" B#" << instp->getBasicBlock()->getId() << " " << *instp;)
                        DBGOUT(" " << *opndp << (isdef ? " DEF" : "") << endl;)

                        if (isdef) 
                        {// opand definition here
                            Opndx::OpDefs::iterator it = opndxp->opdefs.begin(),
                                                    end = opndxp->opdefs.end();
                            for (; it != end && it->defp != instp; ++it)
                                ;

                            assert(it != end);
                            opdefp = &*it;
                            opndxp->copdefp = opdefp;   
                            opndxp->cbbp = nodep;
                        }
                        else
                        {// opand usage here
                            if (opndxp->cbbp != nodep)
                            {// it must be usage of global definition
                                OpDef* lastdefp = 0;
                                Opndx::OpDefs::iterator it = opndxp->opdefs.begin(),
                                                        end = opndxp->opdefs.end();
                                for (; it != end; ++it)
                                    if (it->globid != -1 && globentryp->getBit(it->globid))
                                    {
                                        opdefp = &*it;
                                        opndxp->copdefp = opdefp;
                                        opndxp->cbbp = nodep;

                                        if (lastdefp != 0)
                                            linkDef(opndxp->opdefs, lastdefp, opdefp);
                                        lastdefp = opdefp;
                                    }
                                assert(lastdefp != 0);
                            }
                            else
                            {// it can be usage of global or local definition
                                opdefp = opndxp->copdefp;
                            }
                        }
                        assert(opdefp != 0);

                        if (opdefp->globid == -1)
                        {// local operand
                            if (isdef)
                            {
                                ++opndxp->webscount;
                                if (opndxp->webscount > 1)
                                {
                                    opndxp->newopndp = splitOpnd(opndp);
                                    DBGOUT("**new local web found " << *opndp << " -> " << *opndxp->newopndp << endl;)
                                }
                            }

                            if (opndxp->webscount > 1 && opdefp->defp->getNode() == nodep)
                            {
                                instp->replaceOpnd(opndp, opndxp->newopndp, isdef ? Inst::OpndRole_AllDefs : Inst::OpndRole_AllUses);
                                DBGOUT(" replace B#" << instp->getBasicBlock()->getId() << " " << *instp << endl;)
                            }
                        }
                        else
                        {// global oprand
                            if (opdefp->useps == 0)
                                opdefp->useps = new (mm) OpDef::Instps(mm);
                            if (!isdef)
                                opdefp->useps->push_back(instp);
                        }
                    }
                }
            }
        }
    }
}


void WebMaker::phase4()
{
    Opndx* opndxp;
    for (unsigned i = 0; i != opandcount; ++i)
        if ((opndxp = opndxs[i]) != 0  && !opndxp->opdefs.empty())
        {
            Opnd* opndp = irManager->getOpnd(i);
            Opndx::OpDefs& opdefs = opndxp->opdefs;

            for (unsigned itx = 0; itx != opdefs.size(); ++itx)
            {
                OpDef* opdefp = &opdefs[itx];
                if (opdefp->globid != -1 && !opdefp->visited)
                {
                    Opnd* newopndp = 0;
                    ++opndxp->webscount;
                    if (opndxp->webscount > 1)
                    {
                        newopndp = splitOpnd(opndp);
                        DBGOUT("**new global web found " << *opndp << " -> " << *newopndp<< endl;)
                    }

                    while (!opdefp->visited)
                    {
                        if (newopndp != 0)
                        {
                            DBGOUT(" defp " << *opdefp->defp << endl;)
                            opdefp->defp->replaceOpnd(opndp, newopndp, Inst::OpndRole_AllDefs);
                            DBGOUT(" replace B#" << opdefp->defp->getBasicBlock()->getId() << " " << *opdefp->defp << endl;)

                            OpDef::Instps::iterator it = opdefp->useps->begin(),
                                                    end = opdefp->useps->end();
                            for (; it != end; ++it)
                            {
                                (*it)->replaceOpnd(opndp, newopndp, Inst::OpndRole_AllUses);
                                DBGOUT(" replace B#" << (*it)->getBasicBlock()->getId() << " " << **it << endl;)
                            }
                        }

                        opdefp->visited = true;
                        opdefp = &opdefs.at(opdefp->linkx);
                        assert(opdefp->globid != -1);
                    }
                }
            }
        }
}


void WebMaker::linkDef(Opndx::OpDefs& opdefs, OpDef* lastdefp, OpDef* opdefp)
{
    for (OpDef* p = lastdefp;;)
    {
        if (p == opdefp)
            return;
        if ((p = &opdefs.at(p->linkx)) == lastdefp)
            break;
    }

    unsigned wx = lastdefp->linkx;
    lastdefp->linkx = opdefp->linkx;
    opdefp->linkx = wx;
}


BitSet* WebMaker::bitsetp (BitSet*& bsp)
{
    if (bsp == 0)
        bsp = new (mm) BitSet(mm, globcount);
    else
        bsp->resize(globcount);
    return bsp;
}


Opnd* WebMaker::splitOpnd (const Opnd* op)
{
    Opnd* np = irManager->newOpnd(op->getType(), op->getConstraint(Opnd::ConstraintKind_Initial));
    ++splitcount;
    return np;
}


//========================================================================================
//  Output formatters
//========================================================================================


#ifdef _DEBUG_WEBMAKER

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


static ostream& operator << (ostream& os, /*const*/ BitSet* bs)
{
    if (bs != 0)
    {
        os << "{";
        Sep s;
        BitSet::IterB bsk(*bs);
        for (int i = bsk.getNext(); i != -1; i = bsk.getNext())
            os << s << i;
        os << "}";
    }
    else
        os << "null";

    return os;
}

#endif //#ifdef _DEBUG_WEBMAKER

} //namespace Ia32
} //namespace Jitrino
