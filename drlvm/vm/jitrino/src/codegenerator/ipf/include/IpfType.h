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

#ifndef IPFTYPE_H_
#define IPFTYPE_H_

#include <bitset>
#include <string>
#include <sstream>
#include <algorithm>
#include <utility>
#include <iomanip>
#include "Type.h"
#include "Log.h"
#include "Stl.h"
#include "VMInterface.h"

using namespace std;

namespace Jitrino {
namespace IPF {

//========================================================================================//
// Forward declaration
//========================================================================================//

class Opnd;
class RegOpnd;
class ConstantRef;
class NodeRef;
class MethodRef;
class Constant;
class Inst;
class Node;
class BbNode;
class Edge;
class Cfg;
class QpNode;

//========================================================================================//
// Defines
//========================================================================================//

#define NUM_G_REG           128     // num of general registers
#define NUM_F_REG           128     // num of floating-point registers
#define NUM_P_REG            64     // num of predicate registers
#define NUM_B_REG             8     // num of branch registers
#define NUM_A_REG           128     // num of application registers

#define MAX_REG_ARG           8     // args number that must be located on regs
#define REG_STACK_BASE       32     // gr stack base
#define G_INARG_BASE         32     // num of register containing first general input arg (not fp)
#define F_INARG_BASE          8     // num of register containing first fp input arg (not general)
#define G_OUTARG_BASE       127     // num of register containing first general output arg (temporary) 
#define F_OUTARG_BASE         8     // num of register containing first fp output arg 

#define LOCATION_INVALID 400000     // invalid location for register and stack operands
#define S_INARG_BASE     300000     // memory stack offset containing first input arg (temporary)
#define S_LOCAL_BASE     200000     // first byte of memory stack local storage (temporary)
#define S_OUTARG_BASE    100000     // memory stack offset containing ninth output arg (temporary)
#define S_BASE              200     // memory stack opnds offset in LOCATION space (if location<S_BASE it is reg num else - stack offset)
#define S_SCRATCH_SIZE       16     // size of memory stack scratch area
#define S_ALIGNMENT          16     // alignment of memory stack size

#define RET_F_REG             8     // num of register containing general return value
#define RET_G_REG             8     // num of register containing fp return value
#define ARG_SLOT_SIZE         8     // size of memory in arg slot
#define SPILL_REG1           14     // register reserved for stack address calculation during spill/fill
#define SPILL_REG2           15     // register reserved for spill/fill

#define POS_BR_TARGET         1     // position of opnd target25 in inst (qp) br target25
#define POS_SWITCH_TABLE      2     // position of opnd switchTblAddr  in inst (qp) switch branchTgt, switchTblAddr, defTgt, fallThroughTgt
#define POS_SWITCH_DEFAULT    3     // position of opnd defTgt         in inst (qp) switch branchTgt, switchTblAddr, defTgt, fallThroughTgt
#define POS_SWITCH_THROUGH    4     // position of opnd fallThroughTgt in inst (qp) switch branchTgt, switchTblAddr, defTgt, fallThroughTgt
#define POS_CMP_P1            1     // position of opnd p1 in inst (qp) cmp p1, p2 = r2, r3
#define POS_CMP_P2            2     // position of opnd p2 in inst (qp) cmp p1, p2 = r2, r3
#define LOC_OFFSET            8     // offset of lock owner field in the synchronization header

#define AR_PFS_NUM           64     // num of AR.PFS register
#define AR_UNAT_NUM          36     // num of AR.UNAT register

#define ROOT_SET_HEADER_SIZE    4   // header size in root set info block
#define SAFE_POINT_HEADER_SIZE 12   // header size in safe points info block

#define MAX_QP_MASK 0xffffffffffffffff        // max value of predicate mask used in LiveManager

#define LOG_ON                0               // Log for Code Generator is on
#define VERIFY_ON             0               // verification for Code Generator is on
#define LOG_OUT               Log::out()
#define STAT_ON               0               // Log for statistic

#define IPF_ERROR "ERROR in file " << __FILE__ << " line " << __LINE__ << " "
#define IPF_LOG   if (LOG_ON) LOG_OUT
#define IPF_STAT  if (STAT_ON) LOG_OUT
#define IPF_ERR   cerr << IPF_ERROR 
#define IPF_ASSERT(condition) if (LOG_ON && !(condition)) { IPF_ERR << (#condition) << endl; }

extern bool ipfLogIsOn;
extern bool ipfVerifyIsOn;
extern bool ipfConstantFolding;

//========================================================================================//
// Enums
//========================================================================================//

enum EdgeKind {
    EDGE_BRANCH,            // taken branch 
    EDGE_THROUGH,           // untaken branch
    EDGE_DISPATCH,          // to dispatch or unwind node
    EDGE_EXCEPTION,         // from dispatch node to exception handler node
    EDGE_INVALID
};

//----------------------------------------------------------------------------------------//

enum NodeKind {
    NODE_BB,                // node containing instructions
    NODE_DISPATCH,          // represents data flow during exception handling process (try block)
    NODE_UNWIND,            // represents data flow during handling exception not cought
    NODE_INVALID
};

//----------------------------------------------------------------------------------------//
// OpndKind shows how Opnd::value field should be treated in encoder

enum OpndKind {
    OPND_G_REG,             // general register number
    OPND_F_REG,             // floating-point register number
    OPND_P_REG,             // predicate register number
    OPND_B_REG,             // branch register number
    OPND_A_REG,             // application register number
    OPND_IP_REG,            // instruction pointer register (Opnd::value ignored)
    OPND_UM_REG,            // user mask register (Opnd::value ignored)
    OPND_IMM,               // immediate value
    OPND_INVALID            // something wrong with jit developers
};

//----------------------------------------------------------------------------------------//
// DataKind shows how Opnd::value should be treated during compilation

enum DataKind {
    DATA_I8,                // signed 8 bit value
    DATA_U8,                // unsigned 8 bit value
    DATA_I16,               // signed 16 bit value
    DATA_U16,               // unsigned 16 bit value
    DATA_I32,               // signed 32 bit value
    DATA_U32,               // unsigned 32 bit value
    DATA_I64,               // signed 64 bit value
    DATA_U64,               // unsigned 64 bit value
    DATA_S,                 // IEEE single precision
    DATA_D,                 // IEEE double precision
    DATA_F,                 // IEEE double-extended precision
    DATA_P,                 // unsigned 8 bit value (0/1)
    DATA_B,                 // unsigned 64 bit value
    DATA_BASE,              // object reference (should be reported to GC)
    DATA_MPTR,              // field reference (should be reported to GC)
    DATA_IMM,               // imm constant (value known durind code selection)
    DATA_NODE_REF,          // imm constant (method reference - is resolved during code emission)
    DATA_METHOD_REF,        // imm constant (node reference - is resolved during code emission)
    DATA_CONST_REF,         // imm constant (memory constant pool - is resolved during code emission)
    DATA_SWITCH_REF,        // imm constant (memory constant pool - is resolved during code emission)
    DATA_INVALID
};

//----------------------------------------------------------------------------------------//

enum SearchKind {
    SEARCH_DIRECT_ORDER,    // all predecessors stay before successor
    SEARCH_POST_ORDER,      // all successors stay before predecessor
    SEARCH_LAYOUT_ORDER,    // order nodes laid out in memory
    SEARCH_UNDEF_ORDER      // invalidate current order
};

//========================================================================================//
// Typedefs
//========================================================================================//

typedef StlVector< Opnd* >              OpndVector;
typedef StlVector< RegOpnd* >           RegOpndVector;
typedef StlVector< Inst* >              InstVector;
typedef StlVector< Node* >              NodeVector;
typedef StlVector< Edge* >              EdgeVector;
typedef StlVector< U_32 >             Uint32Vector;
typedef StlList< Inst* >                InstList;
typedef StlList< Node* >                NodeList;
typedef StlList< Edge* >                EdgeList;
typedef StlSet< Opnd* >                 OpndSet;
typedef StlSet< RegOpnd* >              RegOpndSet;
typedef StlSet< Node* >                 NodeSet;
typedef StlMap< RegOpnd*, RegOpnd* >    RegOpnd2RegOpndMap;
typedef StlMap< Inst*, RegOpndSet >     Inst2RegOpndSetMap;
typedef StlMap< uint64, RegOpndSet >    Uint642RegOpndSetMap;
typedef bitset< NUM_G_REG >             RegBitSet;
typedef StlMultiMap <U_32, RegOpnd*, greater <U_32> > Int2OpndMap;

typedef NodeVector::iterator            NodeIterator;
typedef InstVector::iterator            InstIterator;
typedef OpndVector::iterator            OpndIterator;
typedef EdgeVector::iterator            EdgeIterator;
typedef OpndSet::iterator               OpndSetIterator;
typedef RegOpndSet::iterator            RegOpndSetIterator;
typedef InstList::iterator              InstListIterator;
typedef NodeList::iterator              NodeListIterator;
typedef EdgeList::iterator              EdgeListIterator;
typedef RegOpnd2RegOpndMap::iterator    RegOpnd2RegOpndMapIterator;
typedef Inst2RegOpndSetMap::iterator    Inst2RegOpndSetMapIterator;
typedef Uint642RegOpndSetMap::iterator  Uint642RegOpndSetMapIterator;

typedef NodeList                        Chain;
typedef StlList< Chain* >               ChainList;
typedef StlMultiMap< U_32, Chain*, greater < U_32 > > ChainMap;
typedef Chain::iterator                 ChainIterator;
typedef ChainList::iterator             ChainListIterator;
typedef ChainMap::iterator              ChainMapIterator;

typedef StlMultiMap <Opnd*, QpNode*>    QpMap;
typedef uint64                          QpMask;

//========================================================================================//
// IpfType
//========================================================================================//

class IpfType {
public:

    static int16    getSize(DataKind);     // opnd value size in bytes
    static bool     isReg(OpndKind);       // is opnd resides on register
    static bool     isGReg(OpndKind);      // is opnd resides on general register
    static bool     isFReg(OpndKind);      // is opnd resides on general register
    static bool     isImm(OpndKind);       // is opnd resides in imm
    static bool     isSigned(DataKind);    // is opnd value is signed
    static bool     isFloating(DataKind);  // is opnd value can be placed in fp reg
};  

} // IPF
} // Jitrino

#endif /*IPFTYPE_H_*/
