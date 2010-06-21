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
 * @author Intel, Konstantin M. Anisimov, Igor V. Chebykin
 *
 */

#include "CodeGenIntfc.h"
#include "IpfEmitter.h"
#include "IpfOpndManager.h"
#include "IpfIrPrinter.h"
#include "IpfVerifier.h"
#include <iomanip>

namespace Jitrino {
namespace IPF {

//============================================================================//
const BundleDescription Bundle::BundleDesc[TEMPLATES_COUNT] = {
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_I) | IT_SLOT2(IT_I), 0x0, 0x00 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_I) | IT_SLOT2(IT_I), 0x4, 0x01 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_I) | IT_SLOT2(IT_I), 0x2, 0x02 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_I) | IT_SLOT2(IT_I), 0x6, 0x03 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_L) | IT_SLOT2(IT_X), 0x0, 0x04 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_L) | IT_SLOT2(IT_X), 0x4, 0x05 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_M) | IT_SLOT2(IT_I), 0x0, 0x08 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_M) | IT_SLOT2(IT_I), 0x4, 0x09 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_M) | IT_SLOT2(IT_I), 0x1, 0x0a },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_M) | IT_SLOT2(IT_I), 0x5, 0x0b },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_F) | IT_SLOT2(IT_I), 0x0, 0x0c },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_F) | IT_SLOT2(IT_I), 0x4, 0x0d },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_M) | IT_SLOT2(IT_F), 0x0, 0x0e },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_M) | IT_SLOT2(IT_F), 0x4, 0x0f },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_I) | IT_SLOT2(IT_B), 0x0, 0x10 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_I) | IT_SLOT2(IT_B), 0x4, 0x11 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_B) | IT_SLOT2(IT_B), 0x0, 0x12 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_B) | IT_SLOT2(IT_B), 0x4, 0x13 },
    { IT_SLOT0(IT_B) | IT_SLOT1(IT_B) | IT_SLOT2(IT_B), 0x0, 0x16 },
    { IT_SLOT0(IT_B) | IT_SLOT1(IT_B) | IT_SLOT2(IT_B), 0x4, 0x17 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_M) | IT_SLOT2(IT_B), 0x0, 0x18 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_M) | IT_SLOT2(IT_B), 0x4, 0x19 },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_F) | IT_SLOT2(IT_B), 0x0, 0x1c },
    { IT_SLOT0(IT_M) | IT_SLOT1(IT_F) | IT_SLOT2(IT_B), 0x4, 0x1d }
};

//============================================================================//

const char Emitter::Itanium2_DualIssueBundles[30][30] = {
    {1, 1, 1, 1, 0, 0, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MII, 0x00
    {1, 1, 1, 1, 0, 0, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MII, 0x01
    {1, 1, 1, 1, 0, 0, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MII, 0x02
    {1, 1, 1, 1, 0, 0, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MII, 0x03
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MLX, 0x04
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MLX, 0x05
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MMI, 0x08
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MMI, 0x09
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MMI, 0x0a
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MMI, 0x0b
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MFI, 0x0c
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MFI, 0x0d
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MMF, 0x0e
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1}, // MMF, 0x0f
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 0, 0, 1, 1, -1, -1, 1, 1}, // MIB, 0x10
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 0, 0, 1, 1, -1, -1, 1, 1}, // MIB, 0x11
    {0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, -1, -1, 0, 0}, // MBB, 0x12
    {0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, -1, -1, 0, 0}, // MBB, 0x13
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, -1, -1, 0, 0}, // BBB, 0x16
    {0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, -1, -1, 0, 0}, // BBB, 0x17
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 0, 0, 1, 1, -1, -1, 1, 1}, // MMB, 0x18
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 0, 0, 1, 1, -1, -1, 1, 1}, // MMB, 0x19
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 0, 0, 1, 1, -1, -1, 1, 1}, // MFB, 0x1c
    {1, 1, 1, 1, 1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 0, 0, 1, 1, -1, -1, 1, 1}  // MFB, 0x1d
};

//============================================================================//

void Emitter::checkForDualIssueBundles() {
    EmitterBb *bb;
    Bundle *bundle1, *bundle2;
    U_32 tmpl1, tmpl2;
    bool header_printed = false;

    for (int bbindex=0 ; bbindex<(int)bbs->size() ; bbindex++) {
        bb = bbs->at(bbindex);
        BundleVector & bundles = *(bb->bundles);
        for (int bi=0, bii=bundles.size()-1 ; bi<bii ; ) {
            bundle1 = bundles[bi];
            bundle2 = bundles[bi+1];
            tmpl1 = bundle1->getTmpl();
            tmpl2 = bundle2->getTmpl();
            if (!bundle1->hasStop() && Itanium2_DualIssueBundles[tmpl1][tmpl2]==0) {
                if (!header_printed) {
                    header_printed = true;
                    clog << "CHECK_DUAL_ISSUE: Method:"
                        << " bundles=" << codesize/IPF_BUNDLE_SIZE
                        << "; SIG="
                        << compilationinterface.getMethodToCompile()->getParentType()->getName()
                        << "." << compilationinterface.getMethodToCompile()->getName()
                        << compilationinterface.getMethodToCompile()->getSignatureString()
                        << "\n";
                }
                clog << "CHECK_DUAL_ISSUE: bbindex=" << bbindex
                    << ", node_id=" << bb->node->getId()
                    << "; bi=" << bi
                    << "; tmpl1=0x" << hex << tmpl1
                    << "; tmpl2=0x" << tmpl2 << dec
                    << "\n";

                bi += 1;
                continue;
            } else if (Itanium2_DualIssueBundles[tmpl1][tmpl2] == -1) {
                if (!header_printed) {
                    header_printed = true;
                    clog << "Method:"
                        << " bundles=" << codesize/IPF_BUNDLE_SIZE
                        << "; SIG="
                        << compilationinterface.getMethodToCompile()->getParentType()->getName()
                        << "." << compilationinterface.getMethodToCompile()->getName()
                        << compilationinterface.getMethodToCompile()->getSignatureString()
                        << "\n";
                }
                clog << "BAD templates: " << tmpl1 << ", " << tmpl2
                    << "\n";
            }

            if (bundle2->hasStop()) {
                bi += 1;
                continue;
            } else {
                bi += 2;
                continue;
            }
        }
    }

}

//============================================================================//

Bundle::Bundle(Cfg& cfg, U_32 itmp, Inst *i0, Inst *i1, Inst *i2) {
    indxtmpl = itmp;   // !!! THis is not template, but index in Bundle::BundleDesc[] !!!

    MemoryManager& mm          = cfg.getMM();
    OpndManager*   opndManager = cfg.getOpndManager();

    // slot 0
    if( i0==NULL ) {
        slot[0] = new(mm) Inst(mm, INST_NOP, PR(0), IMM(INST_BREAKPOINT_IMM_VALUE));
    } else {
        slot[0] = i0;
    }

    // slot 1
    if( i1==NULL ) {
        slot[1] = new(mm) Inst(mm, INST_NOP, PR(0), IMM(INST_BREAKPOINT_IMM_VALUE));
    } else {
        slot[1] = i1;
    }

    // slot 2
    if( i2==NULL ) {
        slot[2] = new(mm) Inst(mm, INST_NOP, PR(0), IMM(INST_BREAKPOINT_IMM_VALUE));
    } else {
        slot[2] = i2;
    }
}

//============================================================================//
void Bundle::emitBundleGeneral(void *whereToEmit) {
    uint64 * p  = (uint64 *)whereToEmit;
    uint64   s;

    p[0] = 0;
    p[1] = 0;

    p[0]  = getTmpl();

    s = getSlotBits(0);
    p[0] |= s << 5;

    s = getSlotBits(1);
    p[0] |= s << 46;
    p[1]  = s >> 18;

    s = getSlotBits(2);
    p[1] |= s << 23;
}

void Bundle::emitBundleBranch(void *whereToEmit, int * bundletarget) {
    uint64 * p  = (uint64 *)whereToEmit;
    uint64   s;
    U_32   itmp = getTmpl();

    p[0] = 0;
    p[1] = 0;

    p[0]  = itmp;

    if (itmp==0x16 || itmp==0x17) {
        s = getSlotBitsBranch(0, bundletarget[0]);
    } else {
        s = getSlotBits(0);
    }
    p[0] |= s << 5;

    if (itmp>=0x12 && itmp<=0x17) {
        s = getSlotBitsBranch(1, bundletarget[1]);
    } else {
        s = getSlotBits(1);
    }
    p[0] |= s << 46;
    p[1]  = s >> 18;

    if (itmp>=0x10) {
        s = getSlotBitsBranch(2, bundletarget[2]);
    } else {
        s = getSlotBits(2);
    }
    p[1] |= s << 23;
}

void Bundle::emitBundleExtended(void *whereToEmit) {
    uint64 * p  = (uint64 *)whereToEmit;
    uint64   s;
    uint64   s12[2];

    p[0] = 0;
    p[1] = 0;

    p[0]  = getTmpl();

    s = getSlotBits(0);
    p[0] |= s << 5;

    getSlotBitsExtended(s12, whereToEmit);
    p[0] |= s12[0] << 46;
    p[1]  = s12[0] >> 18;
    p[1] |= s12[1] << 23;
}

uint64 Bundle::getSlotBits(int slotindex) {
    return Encoder::getInstBits(Emitter::getExecUnitType(indxtmpl, slotindex)
            , slot[slotindex]);
}
uint64 Bundle::getSlotBitsBranch(int slotindex, int target) {
    return Encoder::getInstBitsBranch(Emitter::getExecUnitType(indxtmpl, slotindex)
            , slot[slotindex], target);
}
uint64 * Bundle::getSlotBitsExtended(uint64 *slots12, void *whereToEmit) {
    return Encoder::getInstBitsExtended(slot[1], slots12, whereToEmit);
}

//============================================================================//
void Bundle::print() {
    Inst * inst = getSlot(0);
    U_32 tmpl = getTmpl();

    IPF_LOG << "(" << tmpl << ")\n";
    IPF_LOG << IrPrinter::toString(inst);
    IPF_LOG << ( tmpl==0x0A || tmpl==0x0B ? " ;;" : "")
        << "\n";
    inst = getSlot(1);
    IPF_LOG << IrPrinter::toString(inst);
    IPF_LOG << ( tmpl==0x02 || tmpl==0x03 ? " ;;" : "")
        << "\n";
    inst = getSlot(2);
    IPF_LOG << IrPrinter::toString(inst);
    IPF_LOG << ( tmpl%2==1 ? " ;;" : "")
        << "\n";
}

//============================================================================//
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#ifndef _REENTRANT
#define _REENTRANT
#endif
#include <signal.h>
#include <errno.h>
#include <ucontext.h>

void sighandler(int sn, siginfo_t *si, void *_sc) {
    struct sigaction signal_action;
    struct ucontext * signal_ucontext;
    int saved_errno = errno;
    
    if (sn==SIGILL && si->si_code==ILL_BREAK && si->si_imm==INST_BREAKPOINT_IMM_VALUE) {
        signal_ucontext = (struct ucontext *)_sc;
    
        if ( (signal_ucontext->_u._mc.sc_ip & 0x03)==2 ) {
            signal_ucontext->_u._mc.sc_ip = (signal_ucontext->_u._mc.sc_ip & ~0x03) + 0x10;
        } else {
            signal_ucontext->_u._mc.sc_ip++;
        }
        //printf("-- sighandler() for signal %d, si_code %d, si_imm %x\n", sn, si->si_code, si->si_imm);
    
        signal_action.sa_flags = SA_SIGINFO;
        signal_action.sa_sigaction = sighandler;
        if (sigaction(SIGILL, &signal_action, NULL)) {
            printf("Sigaction returned error = %d\n", errno);
        }
    }
    
    errno = saved_errno;
    return;
}


//============================================================================//

EmitterBb::EmitterBb(Cfg & cfg, CompilationInterface & compilationinterface
        , BbNode  * node_, bool _break4cafe, bool _nop4cafe) :
    node(node_),
    insts(node->getInsts())
{
    MemoryManager& mm=cfg.getMM();
    OpndManager * opndManager = cfg.getOpndManager();
    Opnd * p0 = opndManager->getP0();

    isize=insts.size();
    stops=new(mm) vectorbool(isize);
    wregs=new(mm) vectorregs(isize);
    rregs=new(mm) vectorregs(isize);
    bundles=new(mm) BundleVector(cfg);
    consts=new(mm) vectorconst;
    bsize=0;

    if (!_break4cafe) {
        if (_nop4cafe) {
            bundles->addBundle(0x01
                , new(mm) Inst(mm, INST_NOP, p0, IMM(INST_BREAKPOINT_IMM_VALUE))
                , new(mm) Inst(mm, INST_NOP, p0, IMM(INST_BREAKPOINT_IMM_VALUE))
                , new(mm) Inst(mm, INST_NOP, p0, IMM(INST_BREAKPOINT_IMM_VALUE)));
        }
    } else {
        struct sigaction signal_action;
        
        signal_action.sa_flags = SA_SIGINFO;
        signal_action.sa_sigaction = sighandler;
        if (sigaction(SIGILL, &signal_action, NULL)) {
            printf("Sigaction returned error = %d\n", errno);
        }

        bundles->addBundle(0x01
            , new(mm) Inst(mm, INST_NOP, p0, IMM(INST_BREAKPOINT_IMM_VALUE))
            , new(mm) Inst(mm, INST_BREAK, p0, IMM(INST_BREAKPOINT_IMM_VALUE))
            , new(mm) Inst(mm, INST_NOP, p0, IMM(INST_BREAKPOINT_IMM_VALUE)));
    }

};

//============================================================================//
Emitter::Emitter(Cfg & cfg_, CompilationInterface & compilationinterface_, bool _break4cafe) :
        mm(cfg_.getMM()),
        cfg(cfg_),
        compilationinterface(compilationinterface_)
    {

    removeUselessInst(cfg, compilationinterface);
    removeIgnoreTypeInst(cfg, compilationinterface);

    (new(mm) IpfVerifier(cfg, compilationinterface))->verifyMethod();

    bbs = new(mm) vectorbb;
    BbNode  * node = (BbNode *)cfg.getEnterNode();
    EmitterBb * bbdesc;
    bool break4cafe = _break4cafe;
    bool nop4cafe = false;

    do {
        bbdesc = new(mm) EmitterBb(cfg, compilationinterface, node, break4cafe, nop4cafe);
        bbs->push_back(bbdesc);
        break4cafe = false;
        nop4cafe = false;
    } while( (node = node->getLayoutSucc()) != NULL );

};

//============================================================================//
int  Emitter::removeUselessInst(Cfg & cfg, CompilationInterface & compilationinterface) {
    BbNode  * node = (BbNode *)cfg.getEnterNode();
    int methoduseless = 0;
    int methodafter = 0;

    do {
        InstVector &insts = node->getInsts();
        int i;

        for( i=0 ; i < (int)insts.size() ; ) {
            Inst *inst = insts[i];
            InstCode icode = inst->getInstCode();
            OpndVector &opnds = inst->getOpnds();

            if (icode==INST_MOV) {
                if (inst->getNumOpnd()==3) {
                    if (((opnds[1]->isGReg() && opnds[2]->isGReg()) || (opnds[1]->isFReg() && opnds[2]->isFReg()))
                            && opnds[1]->getValue()==opnds[2]->getValue()) {
                        IPF_LOG << "USELESS: " << IrPrinter::toString(inst) << "\n";
                        insts.erase(insts.begin() + i);
                        methoduseless++;
                        continue;
                    }
                }
            }
            if (icode==INST_ADD || icode==INST_ADDS || icode==INST_ADDL) {
                if (inst->getNumOpnd()==4) {
                    if (opnds[2]->getValue()==0
                            && opnds[1]->getValue()==opnds[3]->getValue()) {
                        IPF_LOG << "USELESS: " << IrPrinter::toString(inst) << "\n";
                        insts.erase(insts.begin() + i);
                        methoduseless++;
                        continue;
                    }
                }
            }

            i++;
        }
        methodafter += i;
    } while( (node = node->getLayoutSucc()) != NULL );

    /* statistical data */
    if (0 && (methoduseless > 0)) {
        static int alluseless = 0;
        static int allafter = 0;
        alluseless += methoduseless;
        allafter += methodafter;
        if (methoduseless > 0) {
            IPF_LOG << "USELESS: method: " << methoduseless
                << "(" << (((float)methoduseless)/(methoduseless + methodafter))*100 << "%) instructions\n";
            IPF_LOG << "USELESS: all: " << alluseless
                << "(" << (((float)alluseless)/(alluseless + allafter))*100 << "%) instructions\n";
        }
    }

    if (methoduseless > 0) {
        IPF_LOG << "USELESS: removed " << methoduseless
            << "(" << (((float)methoduseless)/(methoduseless + methodafter) * 100) << "%) instructions\n";
    }
    return methoduseless;
}

//============================================================================//
int  Emitter::removeIgnoreTypeInst(Cfg & cfg, CompilationInterface & compilationinterface) {
    BbNode  * node = (BbNode *)cfg.getEnterNode();
    int ignoredcc = 0, i = 0;
    do {
        InstVector &insts = node->getInsts();

        for( i=0 ; i < (int)insts.size() ; ) {
            Inst *inst = insts[i];

            if (Encoder::isIgnoreInst(inst)) {
                IPF_LOG << "IGNORED: " << IrPrinter::toString(inst) << "\n";
                insts.erase(insts.begin() + i);
                continue;
            }
            i++;
        }
    } while( (node = node->getLayoutSucc()) != NULL );

    if (ignoredcc>0 && i>0) {
        IPF_LOG << "IGNORED: removed " << ignoredcc
            << "(" << (((float)ignoredcc)/(ignoredcc + i) * 100) << "%) instructions\n";
    }
    return ignoredcc;
}

//============================================================================//
InstructionType Emitter::getExecUnitType(int templateindex, int slotindex) {
    return (InstructionType)((Bundle::BundleDesc[templateindex].slots >> (slotindex * 8)) & 0xFF);
}

//============================================================================//
void Emitter::getTmpl(int bbindex, BundleDescription & tmp, Inst *i0, Inst *i1, Inst *i2, bool s0, bool s1, bool s2) {
    if(i0!=NULL) {
        tmp.slots = IT_SLOT0(Encoder::getInstType(i0));
    } else {
        tmp.slots = IT_SLOT0(IT_ANY);
    }
    tmp.stops = (s0 ? 1 : 0);

    if(i1!=NULL) {
        tmp.slots |= IT_SLOT1(Encoder::getInstType(i1));
    } else {
        tmp.slots |= IT_SLOT1(IT_ANY);
    }
    tmp.stops |= (s1 ? 2 : 0);

    if( i2!=NULL ) {
        tmp.slots |= IT_SLOT2(Encoder::getInstType(i2));
    } else {
        tmp.slots |= IT_SLOT2(IT_ANY);
    }
    tmp.stops |= (s2 ? 4 : 0);
}

int Emitter::findTmpl(const BundleDescription & tmp) {
    for( int j=0 ; j<TEMPLATES_COUNT ; j++ ) {
        if( (Bundle::BundleDesc[j].slots & tmp.slots)==Bundle::BundleDesc[j].slots
                && (Bundle::BundleDesc[j].stops == tmp.stops) ) {
            return j;
        }
    }
    return -1;
}

//============================================================================//
void Emitter::getWriteDpndBitset(Inst * inst, RegistersBitset * regs)
{
    OpndVector & opnds = inst->getOpnds();
    if(opnds.size() <= 1) return;

    U_32 dstcount=inst->getNumDst();
    Opnd *opnd;

    for( U_32 i=0 ; i<dstcount ; i++ ) {
        opnd = opnds[i+1];
        switch (opnd->getOpndKind()) {
        case OPND_G_REG:
            regs->GR.set(opnd->getValue());
            break;
        case OPND_F_REG:
            regs->FR.set(opnd->getValue());
            break;
        case OPND_P_REG:
            regs->PR.set(opnd->getValue());
            break;
        case OPND_B_REG:
            regs->BR.set(opnd->getValue());
            break;
        default:
            break;
        }
    }
}

//============================================================================//
void Emitter::getReadDpndBitset(Inst * inst, RegistersBitset * regs)
{
    OpndVector & opnds = inst->getOpnds();
    if(opnds.size() <= 1) return;

    U_32 dstcount=inst->getNumDst();
    U_32 opndcount=inst->getNumOpnd();
    Opnd *opnd;

    for( U_32 i=1+dstcount ; i<opndcount ; i++ ) {
        opnd = opnds[i];
        switch (opnd->getOpndKind()) {
        case OPND_G_REG:
            regs->GR.set(opnd->getValue());
            break;
        case OPND_F_REG:
            regs->FR.set(opnd->getValue());
            break;
        case OPND_P_REG:
            regs->PR.set(opnd->getValue());
            break;
        case OPND_B_REG:
            regs->BR.set(opnd->getValue());
            break;
        default:
            break;
        }
    }
    int p=opnds[0]->getValue();
    if( p ) {
        regs->PR.set(p);
    }
}

//============================================================================//
bool Emitter::tricking(InstVector & insts, MemoryManager& mm, Cfg& cfg) {
    RegistersBitset * regs;
    Inst *inst;

    for( int i=0 ; i < (int)insts.size() ; i++ ) {
        inst = insts[i];
        // regs masks
        regs = new(mm) RegistersBitset();
        getWriteDpndBitset(inst, regs);
        if (regs->GR.test(33)) {
            OpndManager * opndManager = cfg.getOpndManager();
            insts.insert(insts.begin() + i
                , new(mm) Inst(mm, INST_BREAK, opndManager->getP0(), opndManager->newImm(INST_BREAKPOINT_IMM_VALUE)));
            i++;
        }
    }
    return true;
}

//============================================================================//
bool Emitter::parsing() {
    bool ret = true;

    datasize = 0;
    for (int bbindex=0, bbssize=(int)bbs->size() ; bbindex<bbssize ; bbindex++) {
        ret &= parsing(bbindex);
    }

    return ret;
}

bool Emitter::parsing(int bbindex) {
    EmitterBb * bb = bbs->at(bbindex);
    InstVector & insts = bb->insts;
    vectorregs * wr = bb->wregs;
    vectorregs * rr = bb->rregs;
    Inst *inst;
    RegistersBitset * regs;
    U_32 dstcount;
    U_32 opndcount;
    Opnd         * opnd;
    vectorconst  * consts = bb->consts;
    Constant     * constant;

    for( int i=0 ; i < (int)insts.size() ; i++ ) {
        inst = insts[i];
        InstCode instCode = inst->getInstCode();
        OpndVector & opnds = inst->getOpnds();

        if (instCode==INST_BR13 || instCode==INST_BRL13) {
            instCode = (instCode == INST_BR13 ? INST_BR : INST_BRL);
        }

        // if inst is branch and target is DATA_NODE_REF
        // set istarget flag for target bb
        if (instCode==INST_BR || instCode==INST_BRL) {
            int is13 = (instCode==INST_BRL13 || instCode==INST_BR13 ? 1 : 0);  // must be 1 or 0
            opnd = opnds[is13 + 1];
            if (opnd->getDataKind() == DATA_NODE_REF)  {
                int targetind = getBbNodeIndex(((NodeRef*) opnd)->getNode());
                bbs->at(targetind)->istarget=true;
            }
        }

        // regs masks
        regs = new(mm) RegistersBitset();
        getWriteDpndBitset(inst, regs);
        wr->at(i) = regs;

        regs = new(mm) RegistersBitset();
        getReadDpndBitset(inst, regs);
        rr->at(i) = regs;

        // constants
        dstcount=inst->getNumDst();
        opndcount=inst->getNumOpnd();

        for( U_32 j=1 ; j<opndcount ; j++ ) {
            opnd = opnds[j];
            if (opnd->isImm()) {
                if ( opnd->getDataKind() == DATA_CONST_REF) {
                    constant = ((ConstantRef *)opnd)->getConstant();
                    consts->push_back(opnd);
                    datasize += constant->getSize();
                } else if ( opnd->getDataKind() == DATA_SWITCH_REF) {
                    constant = ((ConstantRef *)opnd)->getConstant();
                    consts->push_back(opnd);
                    datasize += ((SwitchConstant *)constant)->getSize();
                }
            }
        }
    }

    return true;
}
//============================================================================//
bool Emitter::stopping() {
    bool ret = true;

    for (int bbindex=0 ; bbindex<(int)bbs->size() ; bbindex++) {
        ret &= stopping(bbindex);
    }

    return ret;
}

bool Emitter::stopping(int bbindex) {
    EmitterBb * bb = bbs->at(bbindex);
    InstVector & insts = bb->insts;
    long sizebb = insts.size();
    if (sizebb==0) return true;

    vectorbool * stops = bb->stops;
    vectorregs * wr = bb->wregs;
    vectorregs * rr = bb->rregs;
    Inst *inst1, *inst2;
    InstCode icode1, icode2;
    RegistersBitset * regs1w, * regs2w, * regs2r;
    long start, stop, hardstop;

    for( start=0, stop=sizebb ; start < (sizebb - 1) ; ) {
        hardstop = -1;
        for (long i = start; i < (stop-1) ; i++) {
            inst1 = insts[i];
            icode1 = inst1->getInstCode();
            if (icode1==INST_BRL || icode1==INST_BRL13) {
                hardstop=i;
                break;
            } else if (icode1 == INST_ALLOC) {
                if (i>0) stops->at(i-1) = true;
            }
            if (Encoder::isIgnoreInst(inst1)) continue;
            if (icode1 >= INST_ST_FIRST && icode1 <= INST_ST_LAST) continue;

            regs1w = wr->at(i);
            if(!regs1w->any()) continue;

            for (long j = i + 1; j < stop; j++) {
                inst2 = insts[j];
                icode2 = inst2->getInstCode();
                if (icode2 == INST_ALLOC) {
                    if (j>0) stops->at(j-1) = true;
                }
                if (Encoder::isIgnoreInst(inst2)) continue;

                regs2w = wr->at(j);
                regs2r = rr->at(j);
                if(!regs2w->any() && !regs2r->any()) continue;

                if (Encoder::isBranchInst(inst2)) {
                    if( (Encoder::getInstType(icode1) & IT_F)
                            && (regs1w->PR & (regs2w->PR | regs2r->PR)).any() ) {
                        stop=j;
                        break;
                    }
                    continue;
                }

                if( (regs1w->GR & (regs2w->GR | regs2r->GR)).any()
                        || (regs1w->FR & (regs2w->FR | regs2r->FR)).any()
                        || (regs1w->PR & (regs2w->PR | regs2r->PR)).any()
                        || (regs1w->BR & (regs2w->BR | regs2r->BR)).any() ) {
                    stop=j;
                    break;
                }
            }
        }
        if (hardstop>=0) {
            stops->at(hardstop) = true;
            start=hardstop+1;
            stop=sizebb;
        } else if( stop==sizebb ) {
            start++;
        } else {
            stops->at(stop-1) = true;
            start=stop;
            stop=sizebb;
        }
    }
    stops->at(stops->size()-1) = true;

    return true;
}

//============================================================================//
bool Emitter::bundling() {
    EmitterBb * bb;
    char * off=0;
    bool ret = true;

    for (int bbindex=0 ; bbindex<(int)bbs->size() ; bbindex++) {
        ret &= bundling(bbindex);
        bb = bbs->at(bbindex);
        bb->bsize = bb->bundles->size();
        bb->codeoff = off;
        bb->codesize = bb->bsize * IPF_BUNDLE_SIZE;
        // align bb if istarget==true
        if (false && bb->istarget) { // switch off aligning
            uint16 bytes4align = (bb->codesize % IPF_CODE_ALIGNMENT) == 0
                    ? 0
                    : IPF_CODE_ALIGNMENT - (bb->codesize % IPF_CODE_ALIGNMENT);
            if (bytes4align>0 && (bytes4align%IPF_BUNDLE_SIZE)==0) {
                bb->codesize += bytes4align;
                uint16 bundles4align = bytes4align/IPF_BUNDLE_SIZE;
                OpndManager * opndManager = cfg.getOpndManager();
                Opnd * p0 = opndManager->getP0();
                for (uint16 i=0 ; i<bundles4align ; i++) {
                    bb->bundles->addBundle(0x00
                        , new(mm) Inst(mm, INST_NOP, p0, IMM(INST_BREAKPOINT_IMM_VALUE))
                        , new(mm) Inst(mm, INST_NOP, p0, IMM(INST_BREAKPOINT_IMM_VALUE))
                        , new(mm) Inst(mm, INST_NOP, p0, IMM(INST_BREAKPOINT_IMM_VALUE)));
                }

            }
        }
        off = (char *)off + bb->codesize;
    }
    codesize = (long)((char *)off);

    return ret;
}
bool Emitter::bundling(int bbindex) {
    EmitterBb * bb = bbs->at(bbindex);
    InstVector & insts = bb->insts;
    long   sizebb = insts.size();
    if (sizebb==0) return true;

    vectorbool * stops = bb->stops;
    BundleVector * bundles = bb->bundles;
    BundleDescription tmp = {0,0,0};
    int   iTmpl=-1;
    Inst *inst0=NULL, *inst1=NULL, *inst2=NULL, *inst3=NULL;
    bool  stop0=false, stop1=false, stop2=false, stop3=false;
    bool  stop0_f=false, stop1_f=false, stop2_f=false, stop3_f=false; // inst is first in group
    bool  stop0_l=false, stop1_l=false, stop2_l=false; // inst is last in group
    int   it0=0, it1=0, it2=0, it3=0; // insts type
    long  i0, i1, i2, i3;

    for ( i0=0 ; i0 < sizebb ; ) {
        inst0=Encoder::resolvePseudo(cfg, insts[i0]);
        if (Encoder::isIgnoreInst(inst0)) { i0++; continue; }
        stop0=stops->at(i0);

        for ( i1=i0+1 ; i1<sizebb ; ) {
            inst1=Encoder::resolvePseudo(cfg, insts[i1]);
            if (Encoder::isIgnoreInst(inst1)) { i1++; continue; }
            stop1=stops->at(i1);
            break;
        }
        if ( i1 >= sizebb ) { inst1=NULL; }

        for ( i2=i1+1 ; i2<sizebb ; ) {
            inst2=Encoder::resolvePseudo(cfg, insts[i2]);
            if (Encoder::isIgnoreInst(inst2)) { i2++; continue; }
            stop2=stops->at(i2);
            break;
        }
        if ( i2 >= sizebb ) { inst2=NULL; }

        for ( i3=i2+1 ; i3<sizebb ; ) {
            inst3=Encoder::resolvePseudo(cfg, insts[i3]);
            if (Encoder::isIgnoreInst(inst3)) { i3++; continue; }
            stop3=stops->at(i3);
            break;
        }
        if ( i3 >= sizebb ) { inst3=NULL; }

        it0 = Encoder::getInstType(inst0);
        it1 = Encoder::getInstType(inst1);
        it2 = Encoder::getInstType(inst2);
        it3 = Encoder::getInstType(inst3);

        stop0_l = (it0 & IT_GL)==IT_GL;
        stop1_l = (it1 & IT_GL)==IT_GL;
        stop2_l = (it2 & IT_GL)==IT_GL;
        stop0_f = (it0 & IT_GF)==IT_GF;
        stop1_f = (it1 & IT_GF)==IT_GF;
        stop2_f = (it2 & IT_GF)==IT_GF;
        stop3_f = (it3 & IT_GF)==IT_GF;

        /*********************************************
         * Special case for br.call
         * br.ret will return to next bundle, so
         * all instructions after br.call must be nop
         *********************************************/
        if (inst0->getInstCode()==INST_BR || inst0->getInstCode()==INST_BR13) {
            CompVector & cmpls0 = inst0->getComps();
            if (cmpls0.size()>0 && cmpls0[0]==CMPLT_BTYPE_CALL) {
                inst1=NULL;
                inst2=NULL;
            }
        } else if (inst1!=NULL && (inst1->getInstCode()==INST_BR || inst1->getInstCode()==INST_BR13)) {
            CompVector & cmpls1 = inst1->getComps();
            if (cmpls1.size()>0 && cmpls1[0]==CMPLT_BTYPE_CALL) {
                inst2=NULL;
            }
        }

        /*********************************************
         * Special case for inst0==LX type (brl,...)
         *********************************************/
        if ( Encoder::getInstType(inst0->getInstCode()) & IT_L ) {
            inst1=NULL;
            inst2=NULL;
            getTmpl(bbindex, tmp, NULL, inst0, NULL
                , false, false, stop0 || stop0_l || stop1_f);
            iTmpl=findTmpl(tmp);
            if( iTmpl>=0 ) {
                bundles->addBundle(iTmpl, NULL, inst0, NULL);
                i0 += 1;
                continue;
            }
            IPF_ERR << "BUNDLING ERROR: CAN'T FIND TEMPLATE !\n";
            IPF_ERR << "BUNDLING ERROR: " << IrPrinter::toString(inst0) << "\n";
            IPF_ASSERT(0);
        }

        /********************************
         * Special case for inst1==LX type (brl,...)
         ********************************/
        if (inst1!=NULL && (Encoder::getInstType(inst1->getInstCode()) & IT_L)) {
            inst2=NULL;
            getTmpl(bbindex, tmp, inst0, inst1, NULL
                , stop0 || stop0_l || stop1_f, false, stop1 || stop1_l || stop2_f);
            iTmpl=findTmpl(tmp);
            if( iTmpl>=0 ) {
                bundles->addBundle(iTmpl, inst0, inst1, NULL);
                i0 = i1 + 1;
                continue;
            }
            inst1=NULL;
        }

        /********************************
         * inst1!=NULL && inst2!=NULL
         ********************************/
        if (inst1!=NULL && inst2!=NULL) {
            getTmpl(bbindex, tmp, inst0, inst1, inst2
                , stop0 || stop0_l || stop1_f, stop1 || stop1_l || stop2_f, stop2 || stop2_l || stop3_f);
            iTmpl=findTmpl(tmp);
            if( iTmpl>=0 ) {
                bundles->addBundle(iTmpl, inst0, inst1, inst2);
                i0 = i2 + 1;
                continue;
            }
        }

        /********************************
         * inst2==NULL && inst1!=LX type (brl,...)
         ********************************/
        if (inst1!=NULL) {
            getTmpl(bbindex, tmp, inst0, inst1, NULL
                , stop0 || stop0_l || stop1_f, stop1 || stop1_l || stop2_f, false);
            iTmpl=findTmpl(tmp);
            if( iTmpl>=0 ) {
                bundles->addBundle(iTmpl, inst0, inst1, NULL);
                i0 = i1 + 1;
                continue;
            }
            getTmpl(bbindex, tmp, inst0, inst1, NULL
                , stop0 || stop0_l || stop1_f, false, stop1 || stop1_l || stop2_f);
            iTmpl=findTmpl(tmp);
            if( iTmpl>=0 ) {
                bundles->addBundle(iTmpl, inst0, inst1, NULL);
                i0 = i1 + 1;
                continue;
            }

            getTmpl(bbindex, tmp, inst0, NULL, inst1
                , stop0 || stop0_l || stop1_f, false, stop1 || stop1_l || stop2_f);
            iTmpl=findTmpl(tmp);
            if( iTmpl>=0 ) {
                bundles->addBundle(iTmpl, inst0, NULL, inst1);
                i0 = i1 + 1;
                continue;
            }
            getTmpl(bbindex, tmp, inst0, NULL, inst1
                , false, stop0 || stop0_l || stop1_f, stop1 || stop1_l || stop2_f);
            iTmpl=findTmpl(tmp);
            if( iTmpl>=0 ) {
                bundles->addBundle(iTmpl, inst0, NULL, inst1);
                i0 = i1 + 1;
                continue;
            }

            getTmpl(bbindex, tmp, NULL, inst0, inst1
                , false, stop0 || stop0_l || stop1_f, stop1 || stop1_l || stop2_f);
            iTmpl=findTmpl(tmp);
            if( iTmpl>=0 ) {
                bundles->addBundle(iTmpl, NULL,inst0, inst1);
                i0 = i1 + 1;
                continue;
            }
        }

        /********************************
         * inst1==NULL
         ********************************/
        getTmpl(bbindex, tmp, inst0, NULL, NULL
            , stop0 || stop0_l || stop1_f, false, false);
        iTmpl=findTmpl(tmp);
        if( iTmpl>=0 ) {
            bundles->addBundle(iTmpl, inst0, NULL, NULL);
            i0 += 1;
            continue;
        }
        getTmpl(bbindex, tmp, inst0, NULL, NULL
            , false, stop0 || stop0_l || stop1_f, false);
        iTmpl=findTmpl(tmp);
        if( iTmpl>=0 ) {
            bundles->addBundle(iTmpl, inst0, NULL, NULL);
            i0 += 1;
            continue;
        }
        getTmpl(bbindex, tmp, inst0, NULL, NULL
            , false, false, stop0 || stop0_l || stop1_f);
        iTmpl=findTmpl(tmp);
        if( iTmpl>=0 ) {
            bundles->addBundle(iTmpl, inst0, NULL, NULL);
            i0 += 1;
            continue;
        }

        getTmpl(bbindex, tmp, NULL, inst0, NULL
            , false, stop0 || stop0_l || stop1_f, false);
        iTmpl=findTmpl(tmp);
        if( iTmpl>=0 ) {
            bundles->addBundle(iTmpl, NULL, inst0, NULL);
            i0 += 1;
            continue;
        }
        getTmpl(bbindex, tmp, NULL, inst0, NULL
            , false, false, stop0 || stop0_l || stop1_f);
        iTmpl=findTmpl(tmp);
        if( iTmpl>=0 ) {
            bundles->addBundle(iTmpl, NULL, inst0, NULL);
            i0 += 1;
            continue;
        }

        getTmpl(bbindex, tmp, NULL, NULL, inst0
            , false, false, stop0 || stop0_l || stop1_f);
        iTmpl=findTmpl(tmp);
        if( iTmpl>=0 ) {
            bundles->addBundle(iTmpl, NULL, NULL, inst0);
            i0 += 1;
            continue;
        }

        IPF_ERR << "ERROR: CAN'T FIND TEMPLATE !!!\n";
        IPF_ASSERT(0);

        i0++;
    }

    return true;
}

//============================================================================//
bool Emitter::emitData() {
    if (datasize==0) return true;

    bool          ret = true;
    EmitterBb   * bb;
    char        * p;
    Opnd        * opnd;

    dataoff = (char *)compilationinterface.allocateDataBlock(datasize
        , L2_CACHE_LINE_SIZE);
    assert(dataoff != NULL);
    if ( dataoff == NULL ) return false;

    p = dataoff;

    for (int bbindex=0 ; bbindex<(int)bbs->size() ; bbindex++) {
        bb = bbs->at(bbindex);
        for ( int i=0, ii=bb->consts->size() ; i < ii ; i++ ) {
            opnd = bb->consts->at(i);
            if (opnd->isImm()) {
                if ( opnd->getDataKind() == DATA_CONST_REF) {
                    Constant    * c;

                    c = ((ConstantRef *)opnd)->getConstant();
                    switch (c->getDataKind()) {
                    case DATA_I64:
                        *((int64 *)p) = ((Int64Constant *)c)->getValue();
                        c->setAddress(p);
                        p += sizeof(int64);
                        break;
                    case DATA_S:
                        *((float *)p) = ((FloatConstant *)c)->getValue();
                        c->setAddress(p);
                        p += sizeof(float);
                        break;
                    case DATA_D:
                        *((double *)p) = ((DoubleConstant *)c)->getValue();
                        c->setAddress(p);
                        p += sizeof(double);
                        break;
                    default:
                        IPF_ERR << "DATA UNKNOWN\n";
                        break;
                    }
                } else if ( opnd->getDataKind() == DATA_SWITCH_REF) {
                    SwitchConstant    * c;

                    c = (SwitchConstant *)((ConstantRef *)opnd)->getConstant();
                    *((uint64 *)p) = (uint64)opnd;  // need to fill with actual
                                                    // values of node's addresses
                                                    // after code emitting!
                    c->setAddress(p);
                    p += c->getSize();
                }
            } else {
                IPF_LOG << "DATA UNKNOWN\n";
                assert(0);
            }
        }
    }

    return ret;
}

//============================================================================//
bool Emitter::fixSwitchTables() {
    if ( datasize==0 ) return true;
    if ( dataoff == NULL ) return true;

    bool          ret = true;
    EmitterBb   * bb;
    void        * p;
    Opnd        * opnd;
    SwitchConstant    * c;
    Node * node;
    Edge * edge;

    for (int bbindex=0 ; bbindex<(int)bbs->size() ; bbindex++) {
        bb = bbs->at(bbindex);
        for ( int i=0, ii=bb->consts->size() ; i < ii ; i++ ) {
            opnd = bb->consts->at(i);
            if (opnd->isImm() && opnd->getDataKind()==DATA_SWITCH_REF) {
                c = (SwitchConstant *)((ConstantRef *)opnd)->getConstant();
                p = c->getAddress();
                for (int j=0, jj=c->getChoiceCount() ; j<jj ; j++) {
                    edge = c->getEdge(j);
                    node = edge->getTarget();
                    *((uint64 *)p) = (uint64)(codeoff + (int64)getBbNodeOff((BbNode *)node));
                    p = (uint64 *)p +1;
                }
            }
        }
    }

    return ret;
}

//============================================================================//
bool Emitter::emitCode() {
    EmitterBb *bb;
    BundleVector * bundles;
    Bundle * bundle;
    int    target[3];
    char *bboff = 0;
    char *off = 0;
    Inst *inst;

    codeoff = (char *)compilationinterface.allocateCodeBlock(
            codesize, IPF_CODE_ALIGNMENT, CodeBlockHeatDefault, 0, false);
    assert(codeoff != NULL);
    IPF_LOG << endl;
    if ( codeoff ) {
        off=codeoff;
        for (int bbindex=0 ; bbindex<(int)bbs->size() ; bbindex++) {
            bb = bbs->at(bbindex);
            // sa
            bb->node->setAddress((uint64)off);
            bboff = off;
            bundles = bb->bundles;
            for(int i=0, ii=(int)bundles->size() ; i<ii ; i++ ) {
                bundle = bundles->at(i);
                for (U_32 si=0 ; si < IPF_SLOTS_COUNT ; si++) {
                    inst = bundle->getSlot(si);
                    inst->setAddr((U_32)(off - bboff) + si);
                }

                if ( isBranchBundle(bundle, codeoff, off, target) ) {
                    // bundle contains B type inst
                    bundle->emitBundleBranch(off, target);
                } else if ( isExtendedBundle(bundle) ) {
                    // bundle contains L+X type inst
                    bundle->emitBundleExtended(off);

                    inst = bundle->getSlot(1);
                    InstCode     icode  = inst->getInstCode();
                    unsigned int is13 = (icode==INST_BRL13 ? 1 : 0);  // must be 1 or 0
                    if ((icode==INST_BRL  || icode==INST_BRL13)
                            && (inst->getComps())[0]==CMPLT_BTYPE_CALL
                            && (inst->getOpnds()[is13 + 2])->getDataKind()==DATA_METHOD_REF) {
                        if (((((uint64)off) | (((uint64)0x4cafe) << 32)) & ~((uint64)0x4cafe << 32))==(uint64)off) {
                            registerDirectCall(inst, ((uint64)off) | (((uint64)0x4cafe) << 32));
                        } else {
                            IPF_ERR << "\n";
                            assert(0);
                        }
                    }
                } else {
                    bundle->emitBundleGeneral(off);
                }
                off = off + IPF_BUNDLE_SIZE;
            }
        }
        IPF_LOG << "END code emitting: "
            << compilationinterface.getMethodToCompile()->getParentType()->getName()
            << "." << compilationinterface.getMethodToCompile()->getName()
            << compilationinterface.getMethodToCompile()->getSignatureString()
            << endl;

        return true;
    }
    return false;
}

bool Emitter::isBranchBundle(Bundle * bundle, char * baseoff, char * bundleoff, int * branchtargets) {
    bool ret = false;

    if ( bundle->getTmpl() > 15 ) {
        Inst   * inst;
        Opnd   * opnd;
        InstCode     icode;
        unsigned int is13;  // must be 1 or 0

        for (int i=0 ; i<IPF_SLOTS_COUNT ; i++ ) {
            branchtargets[i] = 0;
            inst = bundle->getSlot(i);
            if(Encoder::isBranchInst(inst)) {
                icode  = inst->getInstCode();
                is13 = (icode==INST_BRL13 || icode==INST_BR13 ? 1 : 0);  // must be 1 or 0
                ret = true;
                opnd = inst->getOpnd(is13 + 1);
                if (opnd->getDataKind() == DATA_NODE_REF)  {
                    branchtargets[i] = (long)getBbNodeOff(((NodeRef*) opnd)->getNode()) - (long)(bundleoff - baseoff);
                }
            }
        }
    }
    return ret;
}

char * Emitter::getBbNodeOff(BbNode * node) {
    for (int i=0 ; i<(int)bbs->size() ; i++) {
        if (bbs->at(i)->node == node) {
            return bbs->at(i)->codeoff;
        }
    }
    return (char *)-1;
}

int Emitter::getBbNodeIndex(BbNode * node) {
    for (int i=0 ; i<(int)bbs->size() ; i++) {
        if (bbs->at(i)->node == node) {
            return i;
        }
    }
    return -1;
}

bool Emitter::emit() {
    bool ret = true;

    // 0 pass: parsing insts
    //     - clear ignored insts
    //     - define registers read/write  masks
    //     - create vector of constants
    //     - create vector of brunches
    ret &= parsing();
    if (!ret) { IPF_ERR << "Bad results for parsing\n"; return ret; }

    // 1 pass: emit data
    ret &= emitData();
    if (!ret) { IPF_ERR << "Bad results for emitData\n"; return ret; }

    // 2 pass: define stops
    ret &= stopping();
    if (!ret) { IPF_ERR << "Bad results for stopping\n"; return ret; }

    // 3 pass: bundling instructions (also saving indexes of bundles with branch insts)
    ret &= bundling();
    if (!ret) { IPF_ERR << "Bad results for bundling\n"; return ret; }

    // 4 pass: emits bundles
    ret &= emitCode();
    if (!ret) { IPF_ERR << "Bad results for emitCode\n"; return ret; }

    // 5 pass: fix switch tables
    ret &= fixSwitchTables();
    if (!ret) { IPF_ERR << "Bad results for fixSwitchTables\n"; return ret; }

    // checkForDualIssueBundles();

    if (LOG_ON) {
        printInsts(" Result of stopping() ");
        printBundles(" Result of bundling() ");
        printCodeBlock(" Result of emitting() ");
        printDisasm(" Result of emitting(disasm) ");
    }

    return ret;
}

void Emitter::printBundles(char * cap) {
    if (LOG_ON) {
        IPF_LOG << "-----------" << cap << "-----------------------------\n";
        for (int i=0 ; i<(int)bbs->size() ; i++) {
            printBundles(i);
        }
        IPF_LOG << "\n";
    }
}

void Emitter::printBundles(int bbindex) {
    BundleVector * bundles = bbs->at(bbindex)->bundles;
    Bundle * bndl;

    IPF_LOG << ".L" << bbs->at(bbindex)->node->getId()
        << " (0x" << hex << bbs->at(bbindex)->node->getAddress() << dec << ")\n";
    for (long i = 0, ii=bundles->size() ; i < ii ; i++) {
        bndl = bundles->at(i);
        if( bndl==NULL ) {
            IPF_LOG << i;
            IPF_LOG << "  \tNULL\n"
                      << "  \tNULL\n"
                      << "  \tNULL\n";
            IPF_LOG << "\n";
        } else {
            IPF_LOG << i;
            bndl->print();
            IPF_LOG << "\n";
        }
    }
}

void Emitter::printInsts(char *cap) {
    IPF_LOG << "\n-----------" << cap << "-----------------------------\n";
    for (int i=0 ; i<(int)bbs->size() ; i++) {
        printInsts(i);
    }
    IPF_LOG << "\n";
}

void Emitter::printInsts(int bbindex) {
    InstVector & insts = bbs->at(bbindex)->insts;
    vectorbool * stops = bbs->at(bbindex)->stops;
    Inst *inst;

    IPF_LOG << ".L" << bbs->at(bbindex)->node->getId() << "\n";
    for (U_32 i = 0, ii=insts.size() ; i < ii ; i++) {
        inst = insts[i];
        if( inst==NULL ) {
            IPF_LOG << IrPrinter::toString(inst);
        } else {
            IPF_LOG << IrPrinter::toString(inst);
            IPF_LOG << (stops->at(i) ? " ;;" : "");
        }
        IPF_LOG << "\n";
    }
}

void Emitter::printCodeBlock(char * cap) {
    char     printbuf[256];
    char   * off = codeoff;
    uint64 * p, tmpl, i0, i1, i2;

    IPF_LOG << "\n-----------" << cap << "-----------------------------\n";
    sprintf(printbuf, "%-11.11s       %-11.11s       %-11.11s       %-8.8s\n", "slot2", "slot1", "slot0", "template");
    IPF_LOG << printbuf;
    for(int i=0 ; i<codesize; i += IPF_BUNDLE_SIZE) {
        p = (uint64 *)off;

        tmpl = p[0] & 0x1F;
        i0 = (p[0] & 0x3FFFFFFFFFE0) >> 5;
        i1 = (((p[0] & ~0x1F) & ~0x3FFFFFFFFFE0) >> 46) | ((p[1] & 0x7FFFFF) << 18);
        i2 = (p[1] & ~0x7FFFFF) >> 23;

        sprintf(printbuf, "%11.11llx       %11.11llx       %11.11llx       %2.2x      \n", i2, i1, i0, (unsigned)tmpl);
        IPF_LOG << printbuf;
        off += IPF_BUNDLE_SIZE;
    }
    IPF_LOG << "---------------------------------------------------\n";
}

void Emitter::registerDirectCall(Inst * inst, uint64 data)
{
    InstCode     icode  = inst->getInstCode();
    unsigned int is13 = (icode==INST_BRL13 ? 1 : 0);  // must be 1 or 0

    assert((icode==INST_BRL || icode==INST_BRL13)
        && (inst->getComps())[0]==CMPLT_BTYPE_CALL);
    Opnd * target = (inst->getOpnds())[is13 + 2];
    assert(target->isImm() && target->getDataKind()==DATA_METHOD_REF);
    MethodDesc * method = ((MethodRef *)target)->getMethod();

    compilationinterface.setNotifyWhenMethodIsRecompiled(method, (void *)data);
    if (LOG_ON) {
        IPF_LOG << "Registered call to " << method->getParentType()->getName()
            << "." << method->getName()
            << " : data=0x" << hex << data << dec
            << " for recompiled method event" << endl;
    }
}

#include <dlfcn.h>
#include <libgen.h>

void Emitter::printDisasm(char * cap) {
    static bool load_done = false;
    static unsigned (*disasm)(const char *, char *, unsigned) = NULL;

    if (!load_done) {
        char buf[PATH_MAX+1];

        // Resolve full path to module
        string sz("");
        int len = 0;

        len = readlink("/proc/self/exe", buf, sizeof(buf)-1);
        if (len < 0) {
            buf[0]='.';
            buf[1]='/';
            buf[2]='\0';
        } else {
            buf[len] = 0;
        }
        char * slash = strrchr(buf, '/');
        if (slash) {
            *(slash) = '\0';
        }

        sz.append(buf);
        sz.append("/libdisasm.so");
        std::cout << "Loading `" << sz << "'...";

        char * path = strdup(sz.c_str());
        void * handle = dlopen(path, RTLD_NOW);
        free(path);
        disasm = (unsigned (*)(const char *, char *, unsigned))
            (handle == NULL ? NULL : dlsym(handle, "disasm"));
        load_done = true;

        if (disasm==NULL) {
            std::cout << "CAN'T LOAD" << std::endl;
            return;
        } else {
            std::cout << "OK" << std::endl;
        }
    }
    if (disasm==NULL) {
        return;
    }

    string str("");
    char buf[100];
    char * off = codeoff;

    for (unsigned i=0 ; i<codesize ; off=codeoff+i) {
        unsigned l = disasm((const char*)off, buf, sizeof(buf)-1);

        for (int bbindex=0 ; bbindex<(int)bbs->size() ; bbindex++) {
            EmitterBb *bb = bbs->at(bbindex);
            char * bboff = (char *)bb->node->getAddress();

            if (bboff==off) {
                char   bbid[64];

                sprintf(bbid, "%i (%p)", (int)bb->node->getId(), bboff);
                str.append("\n.L");
                str.append(bbid);
                break;
            }
        }
        if (buf[0]=='[') {
            str.append("\n");
            buf[5] = '\0';
            str.append(buf);
            str.append("\n      ");
            str.append(buf + 6);
            str.append("\n");
        } else {
            str.append(buf);
            str.append("\n");
        }
        i += l;
    }

    IPF_LOG << endl << "-----------" << cap << "-----------------------" << endl;
    IPF_LOG << str;
}

} // IPF
} // Jitrino
