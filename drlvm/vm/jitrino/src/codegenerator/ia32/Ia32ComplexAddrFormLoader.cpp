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
 * @author Nikolay A. Sidelnikov
 */


#include "Ia32CodeGenerator.h"
#include "Ia32CFG.h"
#include "Ia32IRManager.h"
#include "Ia32Inst.h"
#include "open/types.h"
#include "Stl.h"
#include "MemoryManager.h"
#include "Type.h"

namespace Jitrino
{
namespace Ia32 {

struct SubOpndsTable {
    Opnd * baseOp;
    Opnd * indexOp;
    Opnd * scaleOp;
    Opnd * dispOp;
    Opnd * baseCand1;
    Opnd * baseCand2;
    Opnd * suspOp;

    SubOpndsTable(Opnd * s, Opnd * disp) : baseOp(NULL), indexOp(NULL), scaleOp(NULL), dispOp(disp), baseCand1(NULL), baseCand2(NULL), suspOp(s) {}
};

class ComplexAddrFormLoader : public SessionAction {
    void runImpl();
protected:
    //fill complex address form
    bool findAddressComputation(Opnd * memOp);
    bool checkIsScale(Inst * inst);
    void walkThroughOpnds(SubOpndsTable& table);
private:
    U_32 refCountThreshold;
};

static ActionFactory<ComplexAddrFormLoader> _cafl("cafl");

void
ComplexAddrFormLoader::runImpl() {
    refCountThreshold = getIntArg("threshold", 4);
    if(refCountThreshold < 2) {
        refCountThreshold = 2;
        assert(0);
    }

    StlMap<Opnd *, bool> memOpnds(irManager->getMemoryManager());
    U_32 opndCount = irManager->getOpndCount();
    irManager->calculateOpndStatistics();
    for (U_32 i = 0; i < opndCount; i++) {
        Opnd * opnd = irManager->getOpnd(i);
        if(opnd->isPlacedIn(OpndKind_Mem)) {
            Opnd * baseOp = opnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Base);
            Opnd * indexOp = opnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Index);
            if (baseOp && (!indexOp)) {
                StlMap<Opnd *, bool>::iterator it = memOpnds.find(baseOp);
                if(it == memOpnds.end() || it->second) {
                    memOpnds[baseOp]=findAddressComputation(opnd);
                }
            }
        }
    }
}

bool
ComplexAddrFormLoader::findAddressComputation(Opnd * memOp) {

    
    Opnd * disp = memOp->getMemOpndSubOpnd(MemOpndSubOpndKind_Displacement);
    Opnd * base = memOp->getMemOpndSubOpnd(MemOpndSubOpndKind_Base);
    
    Inst * inst = base->getDefiningInst();
    if (!inst)
        return true;

    SubOpndsTable table(base, disp);
    walkThroughOpnds(table);
    if(!table.baseOp)
        table.baseOp = table.suspOp;
    
    if(base->getRefCount() > refCountThreshold) {
        if (table.indexOp) {
            Inst* newIns = irManager->newInst(Mnemonic_LEA, base, irManager->newMemOpnd(irManager->getTypeManager().getUnmanagedPtrType(memOp->getType()), table.baseOp, table.indexOp, table.scaleOp, table.dispOp));
            newIns->insertAfter(inst);
            return false;
        }
    } else {
        if (table.baseOp) {
            Opnd* origOp = memOp->getMemOpndSubOpnd(MemOpndSubOpndKind_Base);
            Opnd* replacementOp = table.baseOp;
            if (origOp->getType()->isUnmanagedPtr() && replacementOp->getType()->isInteger()) {
                replacementOp->setType(origOp->getType());
            }
            memOp->setMemOpndSubOpnd(MemOpndSubOpndKind_Base, replacementOp);
        } 
        if (table.indexOp) {
            memOp->setMemOpndSubOpnd(MemOpndSubOpndKind_Index, table.indexOp);
            if (table.scaleOp) {
                memOp->setMemOpndSubOpnd(MemOpndSubOpndKind_Scale, table.scaleOp);
            } else {
                assert(0);
            }
        }
        if (table.dispOp) {
                memOp->setMemOpndSubOpnd(MemOpndSubOpndKind_Displacement, table.dispOp);

        }
    }
    return true;
}//end ComplexAddrFormLoader::findAddressComputation

bool 
ComplexAddrFormLoader::checkIsScale(Inst * inst) {
    Opnd * opnd = inst->getOpnd(3);
    if(opnd->isPlacedIn(OpndKind_Imm)) {
        switch(opnd->getImmValue()) {
            case 1:
            case 2:
            case 4:
            case 8: 
                return true;
            default:
                return false;
        }
    }
    return false;
}

void
ComplexAddrFormLoader::walkThroughOpnds(SubOpndsTable& table) {
    Opnd * opnd;
    if (table.baseCand1)
        opnd = table.baseCand1;
    else if(table.baseCand2)
        opnd = table.baseCand2;
    else
        opnd = table.suspOp;
    
    Inst * instUp = opnd->getDefiningInst();

    for(;instUp!=NULL && instUp->getMnemonic() == Mnemonic_MOV;instUp = instUp->getOpnd(1)->getDefiningInst());
    if(!instUp) {
        if(!table.baseOp && !opnd->isPlacedIn(OpndKind_Mem)) {
            table.baseOp = opnd;
            if (table.baseCand1) {
                table.baseCand1 = NULL;
                walkThroughOpnds(table);
            }
        } else if (table.baseOp) {
            table.baseOp = table.suspOp;
            table.baseCand1 = NULL;
            table.baseCand2 = NULL;
//            table.dispOp = NULL;
        }
        return;
    } 

    U_32 defCount = instUp->getOpndCount(Inst::OpndRole_InstLevel|Inst::OpndRole_Def);
    if(instUp->getMnemonic()==Mnemonic_ADD) {
        Opnd * src1 = instUp->getOpnd(defCount);
        Opnd * src2 = instUp->getOpnd(defCount+1);
        if(src1->isPlacedIn(OpndKind_Mem) || (src2->isPlacedIn(OpndKind_Mem))) {
            table.baseOp = table.suspOp;
            return;
        } else if(src2->isPlacedIn(OpndKind_Imm)) {
            irManager->resolveRuntimeInfo(src2);
#ifdef _EM64T_
            if((src2->getImmValue() > (int64)0x7FFFFFFF) || (src2->getImmValue() < -((int64)0x10000000))) {
                table.baseOp = table.suspOp;
                return;
            }
#endif      

            if (table.baseCand1) {
                table.baseCand1 = NULL;
                table.baseOp = src1;
            } else if (table.baseCand2) {
                if(!table.baseOp)
                    table.baseOp = src1;
                else {
                    table.baseOp = table.suspOp;
                    return;
                }
            } else {
                table.suspOp = src1;
            }

            if(table.dispOp) {
                irManager->resolveRuntimeInfo(table.dispOp);
                table.dispOp = irManager->newImmOpnd(table.dispOp->getType(), table.dispOp->getImmValue() + src2->getImmValue());
                return;
            } else {
                table.dispOp = src2;            
            }
            walkThroughOpnds(table);
        }else if(table.baseCand1) {
            assert(!table.baseOp);
            table.baseOp = table.baseCand1;
            table.baseCand1 = NULL;
            walkThroughOpnds(table);
        }else if(table.baseCand2) {
            assert(table.baseOp);
            table.baseOp = table.suspOp;
        }else if(!table.baseOp) {
            table.baseCand1 = src1;
            table.baseCand2 = src2;
            walkThroughOpnds(table);
        } else {
            table.baseOp = table.suspOp;
        }
    } else if(instUp->getMnemonic()==Mnemonic_IMUL && checkIsScale(instUp)) {
        table.indexOp = instUp->getOpnd(defCount);
        table.scaleOp =  instUp->getOpnd(defCount+1);
        if(table.baseCand1) {
            table.baseCand1 = NULL;
            table.suspOp = table.baseCand2;
            table.baseCand2 = NULL;
            walkThroughOpnds(table);
        } 
    } else {
        table.baseOp = table.suspOp;
    }
}
} //end namespace Ia32
}
