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

#ifndef IPFEMITTER_H_
#define IPFEMITTER_H_

#include <vector>
#include <bitset>
#include "Type.h"
#include "IpfEncoder.h"
#include "IpfCfg.h"
#include "IpfCodeSelector.h"

using namespace std;

namespace Jitrino {
namespace IPF {

//========================================================================================//
// This file is based on following docs:
//   Intel Itanium Architecture
//   Software Developers Manual
//   Volume 1: Application Architecture
//   Volume 2: System Architecture
//   Volume 3: Instruction Set Reference
//   Revision 2.1
//   October 2002
//========================================================================================//

//
//  Constants that describe cache on Itanium2
//

//
//  Level 1 cache
//
#define L1I_CACHE_LINE_SIZE 64       
#define L1I_CACHE_BANK_SIZE 16384
#define L1D_CACHE_LINE_SIZE 64       
#define L1D_CACHE_BANK_SIZE 16384

//
//  Level 2 cache
//
#define L2_CACHE_LINE_SIZE 128
#define L2_CACHE_BANK_SIZE 262144


#define PR(n)  (opndManager->newRegOpnd(OPND_P_REG, DATA_P, n))
#define GR(n)  (opndManager->newRegOpnd(OPND_G_REG, DATA_I64, n))
#define FR(n)  (opndManager->newRegOpnd(OPND_F_REG, DATA_F, n))
#define BR(n)  (opndManager->newRegOpnd(OPND_B_REG, DATA_I64, n))
#define IMM(n) (opndManager->newImm(n))

#define IPF_SLOTS_COUNT 3
#define IT_SLOT0(v) (0xFF & v)
#define IT_SLOT1(v) ((0xFF & v) << 8)
#define IT_SLOT2(v) ((0xFF & v) << 16)

#define TEMPLATES_COUNT    24                      // count of valid templates
#define IPF_CODE_ALIGNMENT 32                      // 2-bundles alignment

typedef vector<bool>   vectorbool;
typedef bitset<128>    bitset128;
typedef bitset<64>     bitset64;
typedef bitset<8>      bitset8;
typedef vector<Opnd *> vectorconst;

//============================================================================//
/*
 * Bundle description
 * slots = slot_0_type | (slot_1_type << 8) | (slot_2_type << 16)
 *         type of slot is InstructionType enum, but IT_I...IT_X only
 * stops = 1 in bit pos corresponds stop after those slot
 *         id est, 0x1 - stop after 0 slot, 0x6 - stops after 1 and 2 slots, ...
 * tmpl  = template field encoding
 */
struct BundleDescription {
    U_32 slots;
    U_32 stops;
    U_32 tmpl;
};

//============================================================================//
class RegistersBitset {
    public:
      void reset() {
        GR.reset();
        FR.reset();
        PR.reset();
        BR.reset();
      }
      
      bool any() {
        return GR.any() || FR.any() || PR.any() || BR.any();
      }
      
      bitset128 GR;
      bitset128 FR;
      bitset64  PR;
      bitset8   BR;
};

typedef vector<RegistersBitset *> vectorregs;

//============================================================================//
// Bundle
//============================================================================//
class Bundle {
  public:
    Bundle(Cfg& cfg, U_32 tmpl, Inst *, Inst *, Inst *);
    
    Inst   * getSlot(int si) { return slot[si]; };
    U_32   getTmplIndex() { return indxtmpl; };
    U_32   getTmpl() { return BundleDesc[indxtmpl].tmpl; };
    bool     hasStop() { U_32 t = getTmpl(); 
                         if (t%2==1 || t==0x2 || t==0xa) return true; return false; };
    void     emitBundleGeneral(void *);
    void     emitBundleExtended(void *);
    void     emitBundleBranch(void *, int *);
    void     print();
    uint64   getSlotBits(int);
    uint64   getSlotBitsBranch(int, int);
    uint64 * getSlotBitsExtended(uint64 *, void *);

    static const BundleDescription BundleDesc[TEMPLATES_COUNT];

  protected:
    U_32  indxtmpl;
    Inst   *slot[IPF_SLOTS_COUNT];
};

class BundleVector : public vector<Bundle*> {
    MemoryManager& mm;
    Cfg&           cfg;
  public:
    BundleVector(Cfg& cfg): mm(cfg.getMM()), cfg(cfg) { 
        branches=new(mm) vector<int>; 
    };
    
    void addBundle(U_32 itmpl, Inst *i0, Inst *i1, Inst *i2) {
        push_back(new(mm) Bundle(cfg, itmpl, i0, i1, i2));
        if(Encoder::isBranchInst(i0) || Encoder::isBranchInst(i1) || Encoder::isBranchInst(i2)) {
            branches->push_back(size() - 1);
        }
    }
    
    vector<int> * branches; // indexes of bundles which contain br, brl, ...
    
};

class EmitterBb {
  public:
    EmitterBb(Cfg & cfg_, CompilationInterface & compilationinterface_
            , BbNode  * node_, bool _break4cafe=false, bool _nop4cafe=false);
    
    BbNode       * node;
    InstVector   & insts;
    long           isize;
    vectorbool   * stops;
    vectorregs   * wregs;
    vectorregs   * rregs;
    BundleVector * bundles;
    vectorconst  * consts;
    bool           istarget;
    
    long           bsize;
    char         * codeoff;   // offset in full code block
    long           codesize;  // size of this bb code block
};

typedef vector<EmitterBb *> vectorbb;

//============================================================================//
// Encoder
//============================================================================//

class Emitter {
  public:
    Emitter(Cfg & cfg_, CompilationInterface & compilationinterface_, bool _break4cafe=false);

    bool emit();
    void printInsts(char *);
    void printInsts(int);
    void printBundles(char *);
    void printBundles(int);
    void printCodeBlock(char *);
    void printDisasm(char * cap);
    void registerDirectCall(Inst *, uint64);

    static InstructionType getExecUnitType(int, int);
    static const char Itanium2_DualIssueBundles[30][30];
    
  protected:
    static void getTmpl(int, BundleDescription &, Inst *, Inst *, Inst *, bool, bool, bool);
    static int  findTmpl(const BundleDescription &);
    static void getWriteDpndBitset(Inst *, RegistersBitset *);
    static void getReadDpndBitset(Inst *, RegistersBitset *);
    static bool tricking(InstVector & insts, MemoryManager& mm, Cfg& cfg);
    static int  removeUselessInst(Cfg &, CompilationInterface &);
    static int  removeIgnoreTypeInst(Cfg &, CompilationInterface &);
    bool    parsing();
    bool    parsing(int);
    bool    stopping();
    bool    stopping(int);
    bool    bundling();
    bool    bundling(int);
    bool    emitData();
    bool    emitCode();
    bool    fixSwitchTables();
    void    checkForDualIssueBundles();
    bool    isBranchBundle(Bundle *, char *, char *, int *);
    bool    isExtendedBundle(Bundle *bundle) { if (bundle->getTmpl()>=0x4 && bundle->getTmpl()<=0x5) return true; return false; };
    int     getBbNodeIndex(BbNode * node);
    char  * getBbNodeOff(BbNode * node);

    MemoryManager& mm;
    Cfg      & cfg;
    CompilationInterface & compilationinterface;
    vectorbb * bbs;
    char *     dataoff;
    long       datasize;  // full size of data block
    char *     codeoff;
    long       codesize;  // full size of code block (summ of all bb codesize)
};

} // IPF
} // Jitrino

#endif /*IPFEMITTER_H_*/
