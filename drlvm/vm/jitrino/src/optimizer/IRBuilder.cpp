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
 * @author Intel, Pavel A. Ozhdikhin
 */

#include "MemoryManager.h"
#include "IRBuilder.h"
#include "Inst.h"
#include "CSEHash.h"
#include "Log.h"
#include "irmanager.h"
#include "CompilationContext.h"

namespace Jitrino {


#if defined(_MSC_VER) && !defined (__ICL) && !defined (__GNUC__)
#pragma warning(disable : 4355)
#endif

static const char* help = \
    "  expansion flags:\n"\
    "    expandMemAddrs[={ON|off}]\n"\
    "    expandElemAddrs[={ON|off}]\n"\
    "    expandCallAddrs[={on|OFF}]\n"\
    "    expandVirtualCallAddrs[={ON|off}]\n"\
    "    expandNullChecks[={ON|off}]\n"\
    "    expandElemTypeChecks[={ON|off}]\n"\
    "  translation-time optimizations:\n"\
    "    doCSE[={ON|off}]\n"\
    "    doSimplify[={ON|off}]\n"\
    "    suppressCheckBounds[={on|OFF}] - omit all bounds checks\n"\
    "    insertMethodLabels[={on|OFF}]\n"\
    "    compressedReferences[={on|OFF}] - force compressed references\n";


static ActionFactory<IRBuilder, IRBuilderAction> _irbuilder(IRBUILDER_ACTION_NAME, help);


class IRBuilderSimplifier : public Simplifier {
public:
    IRBuilderSimplifier(IRBuilder& irb) 
        : Simplifier(*irb.getIRManager(), false), irBuilder(irb) 
    {}
protected:
    // numeric
    virtual Inst* genAdd(Type* type, Modifier mod, Opnd* src1, Opnd* src2){
        return irBuilder.genAdd(type, mod, src1, src2)->getInst();
    }
    virtual Inst* genSub(Type* type, Modifier mod, Opnd* src1, Opnd* src2) {
        return irBuilder.genSub(type, mod, src1, src2)->getInst();
    }
    virtual Inst* genNeg(Type* type, Opnd* src) {
        return irBuilder.genNeg(type, src)->getInst();
    }
    virtual Inst* genMul(Type* type, Modifier mod, Opnd* src1, Opnd* src2){
        return irBuilder.genMul(type, mod, src1, src2)->getInst();
    }
    virtual Inst* genMulHi(Type* type, Modifier mod, Opnd* src1, Opnd* src2){
        return irBuilder.genMulHi(type, mod, src1, src2)->getInst();
    }
    virtual Inst* genMin(Type* type, Opnd* src1, Opnd* src2){
        return irBuilder.genMin(type, src1, src2)->getInst();
    }
    virtual Inst* genMax(Type* type, Opnd* src1, Opnd* src2){
        return irBuilder.genMax(type, src1, src2)->getInst();
    }
    virtual Inst* genAbs(Type* type, Opnd* src1){
        return irBuilder.genAbs(type, src1)->getInst();
    }
    // bitwise
    virtual Inst* genAnd(Type* type, Opnd* src1, Opnd* src2){
        return irBuilder.genAnd(type, src1, src2)->getInst();
    }
    virtual Inst* genOr(Type* type, Opnd* src1, Opnd* src2){
        return irBuilder.genOr(type, src1, src2)->getInst();
    } 
    virtual Inst* genXor(Type* type, Opnd* src1, Opnd* src2){
        return irBuilder.genXor(type, src1, src2)->getInst();
    }
    virtual Inst* genNot(Type* type, Opnd* src1){
        return irBuilder.genNot(type, src1)->getInst();
    }
    virtual Inst* genSelect(Type* type, Opnd* src1, Opnd* src2, Opnd* src3){
        return irBuilder.genSelect(type, src1, src2, src3)->getInst();
    }
    // conversion
    virtual Inst* genConv(Type* dstType, Type::Tag toType, Modifier ovfMod, Opnd* src){
        return irBuilder.genConv(dstType, toType, ovfMod, src)->getInst();
    }

    virtual Inst* genConvZE(Type* dstType, Type::Tag toType, Modifier ovfMod, Opnd* src){
        return irBuilder.genConvZE(dstType, toType, ovfMod, src)->getInst();
    }
    
    virtual Inst* genConvUnmanaged(Type* dstType, Type::Tag toType, Modifier ovfMod, Opnd* src){
        return irBuilder.genConvUnmanaged(dstType, toType, ovfMod, src)->getInst();
    }
    
    // shifts
    virtual Inst* genShladd(Type* type, Opnd* src1, Opnd* src2, Opnd *src3){
        return irBuilder.genShladd(type, src1, src2, src3)->getInst();
    }
    virtual Inst* genShl(Type* type, Modifier smmod, Opnd* src1, Opnd* src2){
        return irBuilder.genShl(type, smmod, src1, src2)->getInst();
    }
    virtual Inst* genShr(Type* type, Modifier mods, Opnd* src1, Opnd* src2){
        return irBuilder.genShr(type, mods, src1, src2)->getInst();
    }
    // comparison
    virtual Inst* genCmp(Type* type, Type::Tag insttype, ComparisonModifier mod, Opnd* src1, Opnd* src2){
        return irBuilder.genCmp(type, insttype, mod, src1, src2)->getInst();
    }
    // control flow
    virtual void genJump(LabelInst* label) {
        irBuilder.genJump(label);
    }
    virtual void genBranch(Type::Tag instType, ComparisonModifier mod, LabelInst* label, Opnd* src1, Opnd* src2) {
        irBuilder.genBranch(instType, mod, label, src1, src2);
    }
    virtual void genBranch(Type::Tag instType, ComparisonModifier mod,  LabelInst* label, Opnd* src1) {
        irBuilder.genBranch(instType, mod, label, src1);
    }
    virtual Inst* genDirectCall(MethodDesc* methodDesc,Type* returnType,Opnd* tauNullCheckedFirstArg,
        Opnd* tauTypesChecked,U_32 numArgs,Opnd* args[])
    {
        irBuilder.genDirectCall(methodDesc, returnType, tauNullCheckedFirstArg, tauTypesChecked, numArgs, args);
        return (Inst*)irBuilder.getCurrentLabel()->getNode()->getLastInst();
    }
    // load, store & mov
    virtual Inst* genLdConstant(I_32 val) {
        return irBuilder.genLdConstant(val)->getInst();
    }
    virtual Inst* genLdConstant(int64 val) {
        return irBuilder.genLdConstant(val)->getInst();
    }
    virtual Inst* genLdConstant(float val) {
        return irBuilder.genLdConstant(val)->getInst();
    }
    virtual Inst* genLdConstant(double val) {
        return irBuilder.genLdConstant(val)->getInst();
    }
    virtual Inst* genLdConstant(Type *type, ConstInst::ConstValue val) {
        return irBuilder.genLdConstant(type, val)->getInst();
    }
    virtual Inst* genTauLdInd(Modifier mod, Type* dstType, Type::Tag ldType, Opnd* src,
        Opnd *tauNonNullBase, Opnd *tauAddressInRange) 
    {
            return irBuilder.genTauLdInd(mod, dstType, ldType, src, tauNonNullBase, tauAddressInRange)->getInst();
    }
    
    virtual Inst* genLdRef(Modifier mod, Type* dstType, U_32 token, MethodDesc *enclosingMethod) {
        return irBuilder.genLdRef(mod, dstType, token, enclosingMethod)->getInst();
    }
    virtual Inst* genLdFunAddrSlot(MethodDesc* methodDesc) {
        return irBuilder.genLdFunAddrSlot(methodDesc)->getInst();
    }
    virtual Inst* genGetVTableAddr(ObjectType* type) {
        return irBuilder.genGetVTable(type)->getInst();
    }
    // compressed references
    virtual Inst* genCompressRef(Opnd *uncompref){
        return irBuilder.genCompressRef(uncompref)->getInst();
    }
    virtual Inst* genUncompressRef(Opnd *compref){
        return irBuilder.genUncompressRef(compref)->getInst();
    }
    virtual Inst *genLdFieldOffsetPlusHeapbase(FieldDesc* fd) {
        return irBuilder.genLdFieldOffsetPlusHeapbase(fd)->getInst();
    }
    virtual Inst *genLdArrayBaseOffsetPlusHeapbase(Type *elemType) {
        return irBuilder.genLdArrayBaseOffsetPlusHeapbase(elemType)->getInst();
    }
    virtual Inst *genLdArrayLenOffsetPlusHeapbase(Type *elemType) {
        return irBuilder.genLdArrayLenOffsetPlusHeapbase(elemType)->getInst();
    }
    virtual Inst *genAddOffsetPlusHeapbase(Type *ptrType, Opnd *compRef, Opnd *offsetPlusHeapbase) {
        return irBuilder.genAddOffsetPlusHeapbase(ptrType, compRef, offsetPlusHeapbase)->getInst();
    }
    virtual Inst *genTauSafe() {
        return irBuilder.genTauSafe()->getInst();
    }
    virtual Inst *genTauMethodSafe() {
        return irBuilder.genTauMethodSafe()->getInst();
    }
    virtual Inst *genTauUnsafe() {
        return irBuilder.genTauUnsafe()->getInst();
    }
    virtual Inst* genTauStaticCast(Opnd *src, Opnd *tauCheckedCast, Type *castType) {
        return irBuilder.genTauStaticCast(src, tauCheckedCast, castType)->getInst();
    }
    virtual Inst* genTauHasType(Opnd *src, Type *castType) {
        return irBuilder.genTauHasType(src, castType)->getInst();
    }
    virtual Inst* genTauHasExactType(Opnd *src, Type *castType) {
        return irBuilder.genTauHasExactType(src, castType)->getInst();
    }
    virtual Inst* genTauIsNonNull(Opnd *src) {
        return irBuilder.genTauIsNonNull(src)->getInst();
    }
    // helper for store simplification, builds/finds simpler src, possibly
    // modifies typetag or store modifier. 
    virtual Opnd* simplifyStoreSrc(Opnd *src, Type::Tag &typetag, Modifier &mod, bool compressRef) {
        return 0;
    }
    virtual void  foldBranch(BranchInst* br, bool isTaken) {
        assert(0);
    }
    virtual void  foldSwitch(SwitchInst* sw, U_32 index) {
        assert(0);
    }
    virtual void  eliminateCheck(Inst* checkInst, bool alwaysThrows) {
        assert(0);
    }    
    virtual void genThrowSystemException(CompilationInterface::SystemExceptionId id) {
        irBuilder.genThrowSystemException(id);
    }
private:
    IRBuilder&  irBuilder;
};
    
void IRBuilderAction::init() {
    readFlags();
}

void IRBuilderAction::readFlags() {
    // IRBuilder expansion flags
    //
    irBuilderFlags.expandMemAddrs         = getBoolArg("expandMemAddrs", true);
    irBuilderFlags.expandElemAddrs        = getBoolArg("expandElemAddrs", true);
    irBuilderFlags.expandCallAddrs        = getBoolArg("expandCallAddrs", false);
    irBuilderFlags.expandVirtualCallAddrs = getBoolArg("expandVirtualCallAddrs", true);
    irBuilderFlags.expandNullChecks       = getBoolArg("expandNullChecks", true);
    irBuilderFlags.expandElemTypeChecks   = getBoolArg("expandElemTypeChecks", true);
    
    //
    // IRBuilder translation-time optimizations
    //
    irBuilderFlags.doCSE                  = getBoolArg("doCSE", true);
    irBuilderFlags.doSimplify             = getBoolArg("doSimplify", true);

    irBuilderFlags.suppressCheckBounds    = getBoolArg("suppressCheckBounds", false);

    irBuilderFlags.insertMethodLabels     = getBoolArg("insertMethodLabels", true);
    irBuilderFlags.compressedReferences   = getBoolArg("compressedReferences", false);

    irBuilderFlags.genMinMaxAbs           = getBoolArg("genMinMaxAbs", false);
    irBuilderFlags.genFMinMaxAbs          = getBoolArg("genFMinMaxAbs", false);

    irBuilderFlags.useNewTypeSystem       = getBoolArg("useNewTypeSystem", false);
}


    

IRBuilder::IRBuilder() :
irManager(NULL),
opndManager(NULL),
typeManager(NULL),
instFactory(NULL),
flowGraph(NULL),
translatorFlags(NULL),
currentLabel(NULL),
cseHashTable(NULL),
simplifier(NULL),
tauMethodSafeOpnd(NULL),
offset(0)
{
    
}

void IRBuilder::init(IRManager* irm, TranslatorFlags* traFlags, MemoryManager& tmpMM) {
    IRBuilderAction* myAction = (IRBuilderAction*)getAction();
    irBuilderFlags = myAction->getFlags(); //copy of flags

    irManager=irm;
    opndManager=&irm->getOpndManager();
    typeManager = &irm->getTypeManager();
    instFactory = &irm->getInstFactory();
    flowGraph = &irm->getFlowGraph();
    translatorFlags = traFlags;
    MemoryManager& mm = irm->getMemoryManager();
    cseHashTable = new (tmpMM) CSEHashTable(mm);
    
    simplifier  = new (mm) IRBuilderSimplifier(*this);
    
    CompilationInterface* ci = getCompilationContext()->getVMCompilationInterface();
    irBuilderFlags.insertWriteBarriers    = ci->needWriteBarriers();
    irBuilderFlags.compressedReferences   = irBuilderFlags.compressedReferences || VMInterface::areReferencesCompressed();
}


void IRBuilder::invalid() {
    Log::out() << " !!!! ---- IRBuilder::invalid ---- !!!! " << ::std::endl;
    assert(0);
}

void IRBuilder::updateCurrentLabelBcOffset() {
    assert(currentLabel!=NULL);
    if (currentLabel->getBCOffset()==ILLEGAL_BC_MAPPING_VALUE) {
        assert(currentLabel->getNode() == NULL || currentLabel->getNode()->isEmpty());
        currentLabel->setBCOffset((uint16)offset);
    }
}


Inst* IRBuilder::appendInst(Inst* inst) {
    updateCurrentLabelBcOffset();
    assert(currentLabel->getBCOffset()!=ILLEGAL_BC_MAPPING_VALUE);

    inst->setBCOffset((uint16)offset);
    Node* node = currentLabel->getNode();

    node->appendInst(inst);
    if(Log::isEnabled()) {
        inst->print(Log::out());
        Log::out() << std::endl;
        Log::out().flush();
    }
    return inst;
}

void IRBuilder::killCSE() {
    cseHashTable->kill();
}

void IRBuilder::genLabel(LabelInst* labelInst) {
    cseHashTable->kill();
    currentLabel = labelInst;
    updateCurrentLabelBcOffset();

    if(Log::isEnabled()) {
        currentLabel->print(Log::out());
        Log::out() << std::endl;
        Log::out().flush();
    }
}

void IRBuilder::genFallThroughLabel(LabelInst* labelInst) {
    currentLabel = labelInst;
    updateCurrentLabelBcOffset();

    if(Log::isEnabled()) {
        currentLabel->print(Log::out());
        Log::out() << std::endl;
        Log::out().flush();
    }
}

LabelInst* IRBuilder::createLabel() {
    currentLabel = (LabelInst*)instFactory->makeLabel();
    updateCurrentLabelBcOffset();
    return currentLabel;
}

void IRBuilder::createLabels(U_32 numLabels, LabelInst** labels) {
    for (U_32 i=0; i<numLabels; i++) {
        labels[i] = (LabelInst*)instFactory->makeLabel();
    }
}

LabelInst* IRBuilder::genMethodEntryLabel(MethodDesc* methodDesc) {
    currentLabel = instFactory->makeMethodEntryLabel(methodDesc);
    currentLabel->setBCOffset(0);

    if(Log::isEnabled()) {
        currentLabel->print(Log::out());
        Log::out() << std::endl;
        Log::out().flush();
    }
    return currentLabel;
}

// compute instructions
Opnd*
IRBuilder::genAdd(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    Operation operation(Op_Add, dstType->tag, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src1, src2);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyAdd(dstType, mod, src1, src2);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        Inst *newi = instFactory->makeAdd(mod, dst, src1, src2);
        appendInst(newi);
    }
    insertHash(hashcode, src1, src2, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genMul(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    Operation operation(Op_Mul, dstType->tag, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src1, src2);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyMul(dstType, mod, src1, src2);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeMul(mod, dst, src1, src2));
    }
    insertHash(hashcode, src1, src2, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genSub(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    Operation operation(Op_Sub, dstType->tag, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src1, src2);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifySub(dstType, mod, src1, src2);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeSub(mod, dst, src1, src2));
    }
    insertHash(hashcode, src1, src2, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genDiv(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    Opnd *tauDivOk = 0;
    if(src2->getType()->isInteger())
        tauDivOk = genTauCheckZero(src2);
    else
        tauDivOk = genTauSafe(); // safe by construction
    Operation operation(Op_TauDiv, dstType->tag, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src1, src2); // tauDivOk is not needed in hash
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyTauDiv(dstType, mod, src1, src2, tauDivOk);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeTauDiv(mod, dst, src1, src2, tauDivOk));
    }
    insertHash(hashcode, src1, src2, dst->getInst()); // tauDivOk is not needed in hash
    return dst;
}

//
// for CLI: inserts a CheckDivOpnds before the divide
Opnd*
IRBuilder::genCliDiv(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);

    Operation operation(Op_TauDiv, dstType->tag, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src1, src2); // tauDivOk is not needed in hash
    if (dst) return dst;

    Opnd *tauDivOk = 0;
    if(src2->getType()->isInteger()) {
        if (mod.getSignedModifier() == SignedOp) {
            // for CLI: if signed, insert a CheckDivOpnds before the divide
            tauDivOk = genTauCheckDivOpnds(src1, src2);
        } else {
            // if unsigned, still need a zero check
            tauDivOk = genTauCheckZero(src2);
        }
    } else {
        tauDivOk = genTauSafe(); // safe by construction
    }
    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyTauDiv(dstType, mod, src1, src2, tauDivOk);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeTauDiv(mod, dst, src1, src2, tauDivOk));
    }
    insertHash(hashcode, src1, src2, dst->getInst()); // tauDivOk is not needed in hash
    return dst;
}

Opnd*
IRBuilder::genRem(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);

    Operation operation(Op_TauRem, dstType->tag, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src1, src2); // tauDivOk is not needed in hash
    if (dst) return dst;


    Opnd *tauDivOk = 0;
    if(src2->getType()->isInteger())
        tauDivOk = genTauCheckZero(src2);
    else
        tauDivOk = genTauSafe(); // safe by construction

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyTauRem(dstType, mod, src1, src2, tauDivOk);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeTauRem(mod, dst, src1, src2, tauDivOk));
    }
    insertHash(hashcode, src1, src2, dst->getInst()); // tauDivOk is not needed in hash
    return dst;
}

//
// for CLI: inserts a CheckDivOpnds before the divide
//
Opnd*
IRBuilder::genCliRem(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);

    Operation operation(Op_TauRem, dstType->tag, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src1, src2); // tauDivOk is not needed in hash
    if (dst) return dst;


    Opnd *tauDivOk = 0;
    if (src2->getType()->isInteger())
        if (mod.getSignedModifier() == SignedOp)
            // for CLI: if signed, insert a CheckDivOpnds before the divide
            tauDivOk = genTauCheckDivOpnds(src1, src2);
        else
            // if unsigned, still need zero check
            tauDivOk = genTauCheckZero(src2);
    else
        tauDivOk = genTauSafe(); // safe by construction

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyTauRem(dstType, mod, src1, src2, tauDivOk);
    }

    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeTauRem(mod, dst, src1, src2, tauDivOk));
    }
    insertHash(hashcode, src1, src2, dst->getInst()); // tauDivOk is not needed in hash
    return dst;
}

Opnd*
IRBuilder::genNeg(Type* dstType, Opnd* src) {
    src = propagateCopy(src);
    Operation operation(Op_Neg, dstType->tag, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyNeg(dstType, src);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeNeg(dst, src));
    }
    insertHash(Op_Neg, src, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genMulHi(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    Operation operation(Op_MulHi, dstType->tag, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src1, src2);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyMulHi(dstType, mod, src1, src2);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeMulHi(mod, dst, src1, src2));
    }
    insertHash(hashcode, src1, src2, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genMin(Type* dstType, Opnd* src1, Opnd* src2) {
    if (irBuilderFlags.genMinMaxAbs &&
        (!dstType->isFloatingPoint() || irBuilderFlags.genFMinMaxAbs)) {

        src1 = propagateCopy(src1);
        src2 = propagateCopy(src2);

        Operation operation(Op_Min, dstType->tag, Modifier());
        U_32 hashcode = operation.encodeForHashing();
        Opnd* dst = lookupHash(hashcode, src1, src2);
        if (dst) return dst;
        
        if (irBuilderFlags.doSimplify) {
            dst = simplifier->simplifyMin(dstType, src1, src2);
        }
        if (!dst) {
            dst = createOpnd(dstType);
            appendInst(instFactory->makeMin(dst, src1, src2));
        }
        insertHash(hashcode, src1, src2, dst->getInst());
        return dst;
    } else {
        // hand-build it

        Type* cmpDstType = typeManager->getInt32Type();
        Type::Tag typeTag = dstType->tag;
        switch (typeTag) {
        case Type::Int32:
        case Type::Int64:
            {
                Opnd *cmpRes = genCmp(cmpDstType, typeTag,
                                      Cmp_GT, src2, src1);
                Opnd *res = genSelect(dstType, cmpRes, 
                                      src1, src2);
                return res;
            }
        case Type::Single:
        case Type::Double:
            {
                // check for NaN
                Opnd *src1IsNaN = genCmp(cmpDstType, typeTag,
                                         Cmp_NE_Un, 
                                         src1, src1);
                Opnd * zero = ((typeTag == Type::Single)
                               ? genLdConstant((float)0)
                               : genLdConstant((double)0));
                // we may have [+-]0.0, which can't be distinguished by cmp
                    Opnd *cmp2aRes = genCmp(cmpDstType, typeTag, 
                                            Cmp_EQ, src1, zero);
                    Opnd *cmp2bRes = genCmp(cmpDstType, typeTag, 
                                            Cmp_EQ, src2, zero);
                    Opnd *bothAreZero = genAnd(cmpDstType,
                                               cmp2aRes, cmp2bRes);
                    // but this expression apparently gets correct min
                    Opnd *minOfZeros =
                        genNeg(dstType,
                               genSub(dstType,
                                      Modifier(Overflow_None)|
                                      Modifier(Exception_Never)|Modifier(Strict_No),
                                      genNeg(dstType, src1),
                                      src2));
                    // otherwise, we can just use a simple min expression
                    Opnd *cmpRes = genCmp(cmpDstType, typeTag,
                                          Cmp_GT, src2, src1);
                    Opnd *simpleMin = genSelect(dstType, cmpRes, 
                                                          src1, src2);
                    Opnd *res =
                        genSelect(dstType,
                                  src1IsNaN,
                                  src1,
                                  genSelect(dstType,
                                            bothAreZero,
                                            minOfZeros,
                                            simpleMin));
                    return res;
            }
        default:
            break;
        }
        assert(0);
        return 0;
    }
}

Opnd*
IRBuilder::genMax(Type* dstType, Opnd* src1, Opnd* src2) {
    if (irBuilderFlags.genMinMaxAbs &&
        (!dstType->isFloatingPoint() || irBuilderFlags.genFMinMaxAbs)) {
        src1 = propagateCopy(src1);
        src2 = propagateCopy(src2);
        Operation operation(Op_Max, dstType->tag, Modifier());
        U_32 hashcode = operation.encodeForHashing();
        Opnd* dst = lookupHash(hashcode, src1, src2);
        if (dst) return dst;
        
        if (irBuilderFlags.doSimplify) {
            dst = simplifier->simplifyMax(dstType, src1, src2);
        }
        if (!dst) {
            dst = createOpnd(dstType);
            appendInst(instFactory->makeMax(dst, src1, src2));
        }
        insertHash(hashcode, src1, src2, dst->getInst());
        return dst;
    } else {
        Type::Tag typeTag = dstType->tag;
        Type* cmpDstType = typeManager->getInt32Type();
        switch (typeTag) {
        case Type::Int32:
        case Type::Int64:
            {
                Opnd *cmpRes = genCmp(cmpDstType, typeTag, Cmp_GT, src1, src2);
                Opnd *res = genSelect(dstType, cmpRes, src1, src2);
                return res;
            }
        case Type::Single:
        case Type::Double:
            {
                // check for NaN
                Opnd *src1IsNaN = genCmp(cmpDstType, typeTag,
                                         Cmp_NE_Un, src1, src1);
                Opnd * zero = ((typeTag == Type::Single)
                               ? genLdConstant((float)0)
                               : genLdConstant((double)0));
                // we may have [+-]0.0, which can't be distinguished by cmp
                Opnd *cmp2aRes = genCmp(cmpDstType, typeTag, Cmp_EQ, src1, zero);
                Opnd *cmp2bRes = genCmp(cmpDstType, typeTag, Cmp_EQ, src2, zero);
                Opnd *bothAreZero = genAnd(cmpDstType, cmp2aRes, cmp2bRes);
                // but this expression apparently gets correct max
                Opnd *maxOfZeros = genSub(dstType, 
                                          Modifier(Overflow_None)|
                                          Modifier(Exception_Never)|Modifier(Strict_No),
                                          src1,
                                          genNeg(dstType, src2));
                Opnd *cmpRes = genCmp(cmpDstType, typeTag,
                                      Cmp_GT, src1, src2);
                Opnd *simpleMin = genSelect(dstType, cmpRes, 
                                            src1, src2);
                Opnd *res = genSelect(dstType,
                                      src1IsNaN,
                                      src1,
                                      genSelect(dstType,
                                                bothAreZero,
                                                maxOfZeros,
                                                simpleMin));
                
                return res;
            }
        default:
            break;
        }
        assert(0);
        return 0;
    }
}

Opnd*
IRBuilder::genAbs(Type* dstType, Opnd* src1) {
    if (irBuilderFlags.genMinMaxAbs &&
        (!dstType->isFloatingPoint() || irBuilderFlags.genFMinMaxAbs)) {

        src1 = propagateCopy(src1);
        Operation operation(Op_Abs, dstType->tag, Modifier());
        U_32 hashcode = operation.encodeForHashing();
        Opnd* dst = lookupHash(hashcode, src1);
        if (dst) return dst;
        
        if (irBuilderFlags.doSimplify) {
            dst = simplifier->simplifyAbs(dstType, src1);
        }
        if (!dst) {
            dst = createOpnd(dstType);
            appendInst(instFactory->makeAbs(dst, src1));
        }
        insertHash(hashcode, src1, dst->getInst());
        return dst;
    } else {
        // hand-build it

        Type::Tag typeTag = src1->getType()->tag;
        Type* cmpDstType = typeManager->getInt32Type();
        switch (typeTag) {
        case Type::Int32:
        case Type::Int64:
            {
                Opnd *zero = ((typeTag == Type::Int32)
                              ? genLdConstant((I_32)0)
                              : genLdConstant((int64)0));
                Opnd *cmpRes = genCmp(cmpDstType, typeTag, 
                                                Cmp_GT, zero, src1);
                Opnd *negSrc = genNeg(dstType, src1);
                Opnd *res = genSelect(dstType, cmpRes, negSrc, src1);
                return res;
            }
        case Type::Single:
        case Type::Double:
            {
                Opnd *zero = ((typeTag == Type::Single)
                              ? genLdConstant((float)0)
                              : genLdConstant((double)0));
                Opnd *cmpRes = genCmp(cmpDstType, typeTag, 
                                                Cmp_GTE, zero, src1);
                Opnd *negSrc = genSub(dstType,
                                                Modifier(Overflow_None)|
                                                Modifier(Exception_Never)|Modifier(Strict_No),
                                                zero,
                                                src1);
                Opnd *res = genSelect(dstType, cmpRes, negSrc, src1);
                return res;
            }
        default:
            break;
        }
        assert(0);
        return 0;
    }
}

// bitwise instructions
Opnd*
IRBuilder::genAnd(Type* dstType, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    Opnd* dst = lookupHash(Op_And, src1, src2);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyAnd(dstType, src1, src2);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeAnd(dst, src1, src2));
    }
    insertHash(Op_And, src1, src2, dst->getInst());
    return dst;
}
Opnd*
IRBuilder::genOr(Type* dstType, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    Opnd* dst = lookupHash(Op_Or, src1, src2);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyOr(dstType, src1, src2);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeOr(dst, src1, src2));
    }
    insertHash(Op_Or, src1, src2, dst->getInst());
    return dst;
}
Opnd*
IRBuilder::genXor(Type* dstType, Opnd* src1, Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    Opnd* dst = lookupHash(Op_Xor, src1, src2);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyXor(dstType, src1, src2);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeXor(dst, src1, src2));
    }
    insertHash(Op_Xor, src1, src2, dst->getInst());
    return dst;
}
Opnd*
IRBuilder::genNot(Type* dstType, Opnd* src) {
    src = propagateCopy(src);
    Opnd* dst = lookupHash(Op_Not, src);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyNot(dstType, src);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeNot(dst, src));
    }
    insertHash(Op_Not, src, dst->getInst());
    return dst;
}

// selection
Opnd*
IRBuilder::genSelect(Type* dstType, Opnd* src1, Opnd* src2, Opnd* src3) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    src3 = propagateCopy(src3);
    Opnd* dst = lookupHash(Op_Select, src1, src2, src3);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifySelect(dstType, src1, src2, src3);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeSelect(dst, src1, src2, src3));
    }
    insertHash(Op_Select, src1, src2, src3, dst->getInst());
    return dst;
}

// Conversion
Opnd*
IRBuilder::genConv(Type* dstType,
                   Type::Tag toType,
                   Modifier ovfMod,
                   Opnd* src) 
{
    src = propagateCopy(src);
    Operation operation(Op_Conv, toType, ovfMod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src->getId());
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyConv(dstType, toType, ovfMod, src);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        Inst* inst = instFactory->makeConv(ovfMod, toType, dst, src);
        appendInst(inst);
    }
    insertHash(hashcode, src->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genConvUnmanaged(Type* dstType,
                   Type::Tag toType,
                   Modifier ovfMod,
                   Opnd* src) 
{
    assert((dstType->isUnmanagedPtr() && src->getType()->isObject()) 
            || (dstType->isObject() && src->getType()->isUnmanagedPtr()));
    src = propagateCopy(src);
    Operation operation(Op_ConvUnmanaged, toType, ovfMod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src->getId());
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyConvUnmanaged(dstType, toType, ovfMod, src);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        Inst* inst = instFactory->makeConvUnmanaged(ovfMod, toType, dst, src);
        appendInst(inst);
    }
    
    insertHash(hashcode, src->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genConvZE(Type* dstType,
                   Type::Tag toType,
                   Modifier ovfMod,
                   Opnd* src) 
{
    assert(src->getType()->isInteger() && (dstType->isInteger() || dstType->isUnmanagedPtr()));
    src = propagateCopy(src);
    Operation operation(Op_ConvZE, toType, ovfMod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src->getId());
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyConvZE(dstType, toType, ovfMod, src);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        Inst* inst = instFactory->makeConvZE(ovfMod, toType, dst, src);
        appendInst(inst);
    }
    insertHash(hashcode, src->getId(), dst->getInst());
    return dst;
}

// Shift
Opnd*
IRBuilder::genShladd(Type* dstType,
                     Opnd* value,
                     Opnd* shiftAmount,
                     Opnd* addTo) {
    value = propagateCopy(value);
    shiftAmount = propagateCopy(shiftAmount);
    addTo = propagateCopy(addTo);
    Opnd* dst = lookupHash(Op_Shladd, value, shiftAmount, addTo);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyShladd(dstType, value, shiftAmount, addTo);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeShladd(dst, value, shiftAmount, addTo));
    }
    insertHash(Op_Shladd, value, shiftAmount, addTo, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genShl(Type* dstType,
                  Modifier mod,
                  Opnd* value,
                  Opnd* shiftAmount) {
    value = propagateCopy(value);
    shiftAmount = propagateCopy(shiftAmount);

    Operation operation(Op_Shladd, dstType->tag, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, value, shiftAmount);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyShl(dstType, mod, value, shiftAmount);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeShl(mod, dst, value, shiftAmount));
    }
    insertHash(hashcode, value, shiftAmount, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genShr(Type* dstType,
                  Modifier mods,
                  Opnd* value,
                  Opnd* shiftAmount) {
    value = propagateCopy(value);
    shiftAmount = propagateCopy(shiftAmount);

    Operation operation(Op_Shr, dstType->tag, mods);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, value, shiftAmount);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyShr(dstType, mods, value, shiftAmount);
    }
    if (!dst) {
        dst = createOpnd(dstType);
        appendInst(instFactory->makeShr(mods, dst, value, shiftAmount));
    }
    insertHash(hashcode, value, shiftAmount, dst->getInst());
    return dst;
}

// Comparison
Opnd*
IRBuilder::genCmp(Type* dstType,
                  Type::Tag instType, // source type for inst
                  ComparisonModifier mod,
                  Opnd* src1,
                  Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);

    Operation operation(Op_Cmp, instType, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src1, src2);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyCmp(dstType, instType, mod, src1, src2);
    }
    if (!dst) {
        // result of comparison is always a 32-bit int
        dst = createOpnd(dstType);
        Inst *i = instFactory->makeCmp(mod, instType, dst, src1, src2);
        appendInst(i);
    }
    insertHash(hashcode, src1, src2, dst->getInst());
    return dst;
}

// 3-way Java-like Comparison
//   effect is (src1 cmp src2) ? 1 : (src2 cmp src1) ? -1 : 0
// For Float or Double args, if Cmp_GT_Un, then second compare is Cmp_GT,
// and vice versa, so that
//     Cmp3(...,Cmp_GT_Un,src1,src2) -> 1 if src1 or src2 is NaN
//     Cmp3(...,Cmp_GT,src1,src2) -> -1 if src1 or src2 is NaN
Opnd*
IRBuilder::genCmp3(Type* dstType,
                   Type::Tag instType, // source type for inst
                   ComparisonModifier mod,
                   Opnd* src1,
                   Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    Operation operation(Op_Cmp3, instType, mod);
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src1, src2);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyCmp3(dstType, instType, mod, 
                                      src1, src2);
    }
    if (!dst) {
        // result of comparison is always a 32-bit int
        dst = createOpnd(dstType);
        Inst* i = instFactory->makeCmp3(mod, instType, dst, src1, src2);
        appendInst(i);
    }
    insertHash(hashcode, src1, src2, dst->getInst());
    return dst;
}

// Control flow
void
IRBuilder::genBranch(Type::Tag instType,
                     ComparisonModifier mod,
                     LabelInst* label,
                     Opnd* src1,
                     Opnd* src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);
    if (mod > Cmp_GTE_Un)
        // bad modifier
        invalid();
    if (irBuilderFlags.doSimplify) {
        if (simplifier->simplifyBranch(instType, mod, label, src1, src2)) {
            // simplified branch was emitted;
            return;
        }
    }
    appendInst(instFactory->makeBranch(mod, instType, src1, src2, label));
}

void
IRBuilder::genBranch(Type::Tag instType,
                     ComparisonModifier mod,
                     LabelInst* label,
                     Opnd* src) {
    src = propagateCopy(src);
    if (mod < Cmp_Zero)
        // bad modifier
        invalid();
    if (irBuilderFlags.doSimplify) {
        if (simplifier->simplifyBranch(instType, mod, label, src)) {
            // simplified branch was emitted;
            return;
        }
    }
    appendInst(instFactory->makeBranch(mod, instType, src, label));
}


void
IRBuilder::genJump(LabelInst* label) {
    appendInst(instFactory->makeJump(label));
}

void
IRBuilder::genJSR(LabelInst* label) {
    appendInst(instFactory->makeJSR(label));
}

void
IRBuilder::genSwitch(U_32 nLabels,
                     LabelInst* labelInsts[],
                     LabelInst* defaultLabel,
                     Opnd* src) {
    src = propagateCopy(src);
    appendInst(instFactory->makeSwitch(src, nLabels, labelInsts, defaultLabel));
}

void
IRBuilder::genThrow(ThrowModifier mod, Opnd* exceptionObj) {
    exceptionObj = propagateCopy(exceptionObj);
    appendInst(instFactory->makeThrow(mod, exceptionObj));
}

void
IRBuilder::genPseudoThrow() {
    appendInst(instFactory->makePseudoThrow());
}

void
IRBuilder::genThrowSystemException(CompilationInterface::SystemExceptionId id) {
    appendInst(instFactory->makeThrowSystemException(id));
}

void
IRBuilder::genThrowLinkingException(Class_Handle encClass, U_32 CPIndex, U_32 operation) {
    appendInst(instFactory->makeThrowLinkingException(encClass, CPIndex, operation));
}

Opnd*
IRBuilder::genCatch(Type* exceptionType) {
    Opnd* dst = createOpnd(exceptionType);
    appendInst(instFactory->makeCatch(dst));
    return dst;
}

Opnd*
IRBuilder::genSaveRet() {
    Opnd *dst = createOpnd(typeManager->getIntPtrType());
    appendInst(instFactory->makeSaveRet(dst));
    return dst;
}



void
IRBuilder::genPrefetch(Opnd *addr) {
    appendInst(instFactory->makePrefetch(propagateCopy(addr)));
}

Opnd* IRBuilder::createTypeOpnd(ObjectType* type) {
    Opnd* res = NULL;
    POINTER_SIZE_SINT val = (POINTER_SIZE_SINT)type->getRuntimeIdentifier();
    res = genLdConstant(val);
    return res;
}
// Calls

Opnd* IRBuilder::genIndirectCallWithResolve(Type* returnType,
                                Opnd* tauNullCheckedFirstArg,
                                Opnd* tauTypesChecked,
                                U_32 numArgs,
                                Opnd* args[],
                                ObjectType* ch,
                                JavaByteCodes bc,
                                U_32 cpIndex,
                                MethodSignature* sig
                                )
{
    assert(!returnType->isNullObject());
    Opnd* callAddrOpnd = lookupHash(Op_VMHelperCall, bc, cpIndex, numArgs>0?args[0]->getId() : 0);
    if (tauTypesChecked == NULL) {
        tauTypesChecked = genTauUnsafe(); 
    }

    if (callAddrOpnd == NULL) {
        VM_RT_SUPPORT vmHelperId = VM_RT_UNKNOWN;
        MemoryManager& mm = irManager->getMemoryManager();
        Opnd* clsOpnd = createTypeOpnd(ch);
        Opnd* idxOpnd = genLdConstant((int)cpIndex);
        U_32 numHelperArgs = 0;
        Opnd** helperArgs = new(mm)Opnd*[3];
        helperArgs[0] = clsOpnd;
        helperArgs[1] = idxOpnd;
        helperArgs[2] = NULL;
        switch(bc) {
        case OPCODE_INVOKESTATIC:
            vmHelperId = VM_RT_GET_INVOKESTATIC_ADDR_WITHRESOLVE;
            numHelperArgs = 2;
            break;
        case OPCODE_INVOKEVIRTUAL:
            vmHelperId = VM_RT_GET_INVOKEVIRTUAL_ADDR_WITHRESOLVE;
            helperArgs[2] = args[0];
            numHelperArgs = 3;
            break;
        case OPCODE_INVOKESPECIAL:
            vmHelperId = VM_RT_GET_INVOKE_SPECIAL_ADDR_WITHRESOLVE;
            numHelperArgs = 2;
            break;
        case OPCODE_INVOKEINTERFACE:
            vmHelperId = VM_RT_GET_INVOKEINTERFACE_ADDR_WITHRESOLVE;
            helperArgs[2] = args[0];
            numHelperArgs = 3;
            break;
        default: assert(0);
        }
        callAddrOpnd = genVMHelperCall(vmHelperId, typeManager->getUnresolvedMethodPtrType(ch, cpIndex, sig), numHelperArgs, helperArgs);
        insertHash(Op_VMHelperCall, bc, cpIndex, numArgs>0?args[0]->getId() : 0, callAddrOpnd->getInst());
    }

    return genIndirectMemoryCall(returnType, callAddrOpnd,  tauNullCheckedFirstArg, tauTypesChecked, numArgs, args); 
}


Opnd*
IRBuilder::genDirectCall(MethodDesc* methodDesc,
                         Type* returnType,
                         Opnd* tauNullCheckedFirstArg,
                         Opnd* tauTypesChecked,
                         U_32 numArgs,
                         Opnd* args[])
{
    if (!tauNullCheckedFirstArg)
        tauNullCheckedFirstArg = genTauUnsafe();
    else
        tauNullCheckedFirstArg = propagateCopy(tauNullCheckedFirstArg);
    if (!tauTypesChecked)
        tauTypesChecked = genTauUnsafe(); 
    else
        tauTypesChecked = propagateCopy(tauTypesChecked);

    if (irBuilderFlags.expandCallAddrs) {
        return genIndirectMemoryCall(returnType, genLdFunAddrSlot(methodDesc), 
                                     tauNullCheckedFirstArg, tauTypesChecked,
                                     numArgs, args); 
    }
    for (U_32 i=0; i<numArgs; i++) {
        args[i] = propagateCopy(args[i]);
    }
    Opnd* dst = createOpnd(returnType);
    appendInst(instFactory->makeDirectCall(dst, tauNullCheckedFirstArg, tauTypesChecked,
                                          numArgs, args, methodDesc));

    // Note that type initialization should be made available for this type
    // and all its ancestor types.
    return dst;
}

Opnd*
IRBuilder::genTauVirtualCall(MethodDesc* methodDesc,
                             Type* returnType,
                             Opnd* tauNullCheckedFirstArg,
                             Opnd* tauTypesChecked,
                             U_32 numArgs,
                             Opnd* args[])
{
    if(!methodDesc->isVirtual())
        // Must de-virtualize - no vtable
        return genDirectCall(methodDesc, returnType,
                             tauNullCheckedFirstArg, tauTypesChecked, 
                             numArgs, args);
    for (U_32 i=0; i<numArgs; i++) {
        args[i] = propagateCopy(args[i]);
    }
    // callvirt can throw a null pointer exception
    if (!tauNullCheckedFirstArg || 
        (tauNullCheckedFirstArg->getInst()->getOpcode() == Op_TauUnsafe)) {
        // if no null check yet, do one
        tauNullCheckedFirstArg = genTauCheckNull(args[0]);
    } else {
        tauNullCheckedFirstArg = propagateCopy(tauNullCheckedFirstArg);
    }
    if (!tauTypesChecked || (tauTypesChecked->getInst()->getOpcode() == Op_TauUnsafe)) {
        // if no type check available yet
        tauTypesChecked = genTauHasTypeWithConv(&args[0], methodDesc->getParentType());
    } else {
        tauTypesChecked = propagateCopy(tauTypesChecked);
    }
    if (irBuilderFlags.doSimplify) {
        Opnd *dst = simplifier->simplifyTauVirtualCall(methodDesc,
                                                            returnType,
                                                            tauNullCheckedFirstArg,
                                                            tauTypesChecked,
                                                            numArgs,
                                                            args);
        if (dst) return dst;
    }
    
    if (irBuilderFlags.expandVirtualCallAddrs) {
        return genIndirectMemoryCall(returnType, 
                                     genTauLdVirtFunAddrSlot(args[0], 
                                                             tauNullCheckedFirstArg,
                                                             methodDesc), 
                                     tauNullCheckedFirstArg,
                                     tauTypesChecked,
                                     numArgs, args);
    }
    Opnd *dst = createOpnd(returnType);
    appendInst(instFactory->makeTauVirtualCall(dst, tauNullCheckedFirstArg,
                                              tauTypesChecked, numArgs, args, methodDesc));
    return dst;
}

Opnd*
IRBuilder::genJitHelperCall(JitHelperCallId helperId,
                            Type* returnType,
                            U_32 numArgs,
                            Opnd*  args[]) {
    for (U_32 i=0; i<numArgs; i++) {
        args[i] = propagateCopy(args[i]);
    }
    Opnd * dst = createOpnd(returnType);
    appendInst(instFactory->makeJitHelperCall(dst, helperId, NULL, NULL, numArgs, args));
    return dst;
}

Opnd*
IRBuilder::genJitHelperCall(JitHelperCallId helperId,
                            Type* returnType,
                            Opnd* tauNullCheckedRefArgs,
                            Opnd* tauTypesChecked,
                            U_32 numArgs,
                            Opnd*  args[]) {
    for (U_32 i=0; i<numArgs; i++) {
        args[i] = propagateCopy(args[i]);
    }
    Opnd * dst = createOpnd(returnType);
    appendInst(instFactory->makeJitHelperCall(dst, helperId, tauNullCheckedRefArgs, tauTypesChecked, numArgs, args));
    return dst;
}

Opnd*
IRBuilder::genVMHelperCall(VM_RT_SUPPORT helperId,
                            Type* returnType,
                            U_32 numArgs,
                            Opnd*  args[]) {
    for (U_32 i=0; i<numArgs; i++) {
        args[i] = propagateCopy(args[i]);
    }
    Opnd * dst = createOpnd(returnType);
    appendInst(instFactory->makeVMHelperCall(dst, helperId, numArgs, args));
    return dst;
}


void
IRBuilder::genTauTypeCompare(Opnd *arg0, MethodDesc *methodDesc, LabelInst *target,
                             Opnd *tauNullChecked) {
    arg0 = propagateCopy(arg0);     // null check now is in genLdVTable()
    Type* type = methodDesc->getParentType();
    assert(type->isObject());
    // Note that we use the methodDesc's type to obtain the vtable which contains the pointer
    // to the method.  This may be an interface vtable.  genLdVTable figures out which.
    Opnd* vtableThis = genLdVTable(arg0, type);
    Opnd* vtableClass = createOpnd(typeManager->getVTablePtrType(type));
    appendInst(instFactory->makeGetVTableAddr(vtableClass, (ObjectType*)type));

    genBranch(Type::VTablePtr, Cmp_EQ, target, vtableThis, vtableClass);
}

Opnd*
IRBuilder::genIndirectCall(Type* returnType,
                           Opnd* funAddr,
                           Opnd* tauNullCheckedFirstArg,
                           Opnd* tauTypesChecked,
                           U_32 numArgs,
                           Opnd* args[])
{
    for (U_32 i=0; i<numArgs; i++) {
        args[i] = propagateCopy(args[i]);
    }
    Opnd* dst = createOpnd(returnType);

    if (!tauNullCheckedFirstArg)
        tauNullCheckedFirstArg = genTauUnsafe();
    else 
        tauNullCheckedFirstArg = propagateCopy(tauNullCheckedFirstArg);
    if (!tauTypesChecked)
        tauTypesChecked = genTauUnsafe(); 
    else
        tauTypesChecked = propagateCopy(tauTypesChecked);

    appendInst(instFactory->makeIndirectCall(dst, funAddr, tauNullCheckedFirstArg, tauTypesChecked,
                                            numArgs, args));
    return dst;
}

Opnd*
IRBuilder::genIndirectMemoryCall(Type* returnType,
                                 Opnd* funAddr,
                                 Opnd* tauNullCheckedFirstArg,
                                 Opnd* tauTypesChecked,
                                 U_32 numArgs,
                                 Opnd* args[])
{
    for (U_32 i=0; i<numArgs; i++) {
        args[i] = propagateCopy(args[i]);
    }

    if (!tauNullCheckedFirstArg)
        tauNullCheckedFirstArg = genTauUnsafe();
    else 
        tauNullCheckedFirstArg = propagateCopy(tauNullCheckedFirstArg);
    if (!tauTypesChecked)
        tauTypesChecked = genTauUnsafe(); 
    else
        tauTypesChecked = propagateCopy(tauTypesChecked);

    Opnd* dst = createOpnd(returnType);
    appendInst(instFactory->makeIndirectMemoryCall(dst, funAddr, tauNullCheckedFirstArg, 
                                                  tauTypesChecked, numArgs, args));
    return dst;
}

void
IRBuilder::genReturn(Opnd* src, Type* retType) {
    src = propagateCopy(src);
    if(Log::isEnabled()) {
        Type* srcType = src->getType();
        bool convOk = retType == srcType;
        convOk = convOk || (retType->isObject() && (srcType->isObject() || srcType->isUnmanagedPtr()));
        if (!convOk){
            assert(!typeManager->isLazyResolutionMode());
            Log::out() << "ERROR   !!!!  IRBuilder: unimplemented: ret typecheck !!!\n";
        }
    }
    appendInst(instFactory->makeReturn(src));
}

void
IRBuilder::genReturn() {
    appendInst(instFactory->makeReturn());
}

void
IRBuilder::genRet(Opnd* src) {
    appendInst(instFactory->makeRet(src));
}

// Move instruction
Opnd*
IRBuilder::genCopy(Opnd* src) {
    src = propagateCopy(src);
    Opnd* dst = createOpnd(src->getType());
    appendInst(instFactory->makeCopy(dst, src));
    return dst;
}

Opnd*
IRBuilder::genArgCoercion(Type* argType, Opnd* actualArg) {
    actualArg = propagateCopy(actualArg);
    if (actualArg->getType() == argType)
        return actualArg;
    return actualArg;
}

// actual parameter and variable definitions
Opnd*
IRBuilder::genArgDef(Modifier mod, Type* type) {
    Opnd* dst = opndManager->createArgOpnd(type);
    appendInst(instFactory->makeDefArg(mod, dst));
    DefArgModifier defMod = mod.getDefArgModifier();
    switch (defMod) {
    case NonNullThisArg:
        genTauIsNonNull(dst);
        break;
    case SpecializedToExactType:
        genTauHasExactType(dst, type);
        break;
    case DefArgBothModifiers:
        genTauIsNonNull(dst);
        genTauHasExactType(dst, type);
        break;
    case DefArgNoModifier:
        break;
    default:
        assert(0);
    }
    genTauHasType(dst, type);
    return dst;
}

VarOpnd*
IRBuilder::genVarDef(Type* type, bool isPinned) {
    return opndManager->createVarOpnd(type, isPinned);
}

// Phi-node instruction
Opnd*
IRBuilder::genPhi(U_32 numArgs, Opnd* args[]) {
    for (U_32 i=0; i<numArgs; i++) {
        args[i] = propagateCopy(args[i]);
    }
    Opnd* dst = createOpnd(args[0]->getType());
    appendInst(instFactory->makePhi(dst, numArgs, args));
    return dst;
}

// Pi-node instruction (splits live range for bounds analysis)
Opnd*
IRBuilder::genTauPi(Opnd *src, Opnd *tau, PiCondition *cond) {
    src = propagateCopy(src);
    tau = propagateCopy(tau);
    PiOpnd* dst = createPiOpnd(src);
    appendInst(instFactory->makeTauPi(dst, src, tau, cond));
    return dst;
}

// load instructions
Opnd*
IRBuilder::genLdConstant(I_32 val) {
    Operation operation(Op_LdConstant, Type::Int32, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, (U_32) val);
    if (dst) return dst;
    dst = createOpnd(typeManager->getInt32Type());
    appendInst(instFactory->makeLdConst(dst, val));
    insertHash(hashcode, (U_32) val, dst->getInst());
    return dst;
}
Opnd*
IRBuilder::genLdConstant(int64 val) {
    Operation operation(Op_LdConstant, Type::Int64, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, (U_32) (val >> 32), (U_32) (val & 0xffffffff));
    if (dst) return dst;
    dst = createOpnd(typeManager->getInt64Type());
    appendInst(instFactory->makeLdConst(dst, val));
    insertHash(hashcode, (U_32) (val >> 32), (U_32) (val & 0xffffffff), dst->getInst());
    return dst;
}
Opnd* IRBuilder::genLdConstant(float val) {
    ConstInst::ConstValue cv;
    cv.s = val;
    U_32 word1 = cv.dword1;
    U_32 word2 = cv.dword2;
    Operation operation(Op_LdConstant, Type::Single, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, word1, word2);
    if (dst) return dst;
    dst = createOpnd(typeManager->getSingleType());
    appendInst(instFactory->makeLdConst(dst, val));
    insertHash(hashcode, word1, word2, dst->getInst());
    return dst;
}
Opnd*
IRBuilder::genLdConstant(double val) {
    ConstInst::ConstValue cv;
    cv.d = val;
    U_32 word1 = cv.dword1;
    U_32 word2 = cv.dword2;
    Operation operation(Op_LdConstant, Type::Double, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, word1, word2);
    if (dst) return dst;
    dst = createOpnd(typeManager->getDoubleType());
    appendInst(instFactory->makeLdConst(dst, val));
    insertHash(hashcode, word1, word2, dst->getInst());
    return dst;
}
Opnd*
IRBuilder::genLdConstant(Type *ptrtype, ConstInst::ConstValue val) {
    U_32 word1 = val.dword1;
    U_32 word2 = val.dword2;
    Operation operation(Op_LdConstant, ptrtype->tag, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, word1, word2);
    if (dst) return dst;
    dst = createOpnd(ptrtype);
    appendInst(instFactory->makeLdConst(dst, val));
    insertHash(hashcode, word1, word2, dst->getInst());
    return dst;
}
Opnd*
IRBuilder::genLdFloatConstant(double val) {
    ConstInst::ConstValue cv;
    cv.d = val;
    U_32 word1 = cv.dword1;
    U_32 word2 = cv.dword2;
    Operation operation(Op_LdConstant, Type::Float, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, word1, word2);
    if (dst) return dst;
    dst = createOpnd(typeManager->getFloatType());
    appendInst(instFactory->makeLdConst(dst, val));
    insertHash(hashcode, word1, word2, dst->getInst());
    return dst;
}
Opnd*
IRBuilder::genLdFloatConstant(float val) {
    ConstInst::ConstValue cv;
    cv.dword1 = 0;
    cv.dword2 = 0;
    cv.s = val;
    U_32 word1 = cv.dword1;
    U_32 word2 = cv.dword2;
    Operation operation(Op_LdConstant, Type::Float, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, word1, word2);
    if (dst) return dst;
    dst = createOpnd(typeManager->getFloatType());
    appendInst(instFactory->makeLdConst(dst, val));
    insertHash(hashcode, word1, word2, dst->getInst());
    return dst;
}
Opnd*
IRBuilder::genLdNull() {
    Operation operation(Op_LdConstant, Type::NullObject, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode);
    if (dst) return dst;
    dst = createOpnd(typeManager->getNullObjectType());
    appendInst(instFactory->makeLdNull(dst));
    insertHash(hashcode, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdRef(MethodDesc* enclosingMethod, U_32 stringToken, Type* type) {
    bool uncompress = irBuilderFlags.compressedReferences;

    Modifier mod = uncompress ? AutoCompress_Yes : AutoCompress_No;
    Opnd* dst = createOpnd(type);

    appendInst(instFactory->makeLdRef(mod, dst, enclosingMethod, stringToken));
    return dst;
}

Opnd*
IRBuilder::genLdVar(Type* dstType, VarOpnd* var) {
    if (!var->isAddrTaken()) {
        Opnd *dst = lookupHash(Op_LdVar, var);
        if (dst) return dst;

        dst = createOpnd(dstType);
        appendInst(instFactory->makeLdVar(dst, var));
        insertHash(Op_LdVar, var, dst->getInst());
        return dst;
    } else {
        Opnd *dst = createOpnd(dstType);
        appendInst(instFactory->makeLdVar(dst, var));
        return dst;
    }
}

Opnd*
IRBuilder::genLdVarAddr(VarOpnd* var) {
    Opnd* dst = lookupHash(Op_LdVarAddr, var);
    if (dst) return dst;


    var->setAddrTaken();
    dst = createOpnd(typeManager->getManagedPtrType(var->getType()));
    appendInst(instFactory->makeLdVarAddr(dst, var));
    insertHash(Op_LdVarAddr, var, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdInd(Type* type, Opnd *ptr)
{
    ptr = propagateCopy(ptr);
    Opnd *tauUnsafe = genTauUnsafe();
    bool uncompress = false;
    if (irBuilderFlags.compressedReferences && type->isObject()) {
        assert(!type->isCompressedReference());
        uncompress = true;
    }
    Modifier mod = uncompress ? AutoCompress_Yes : AutoCompress_No;
    Opnd *dst = genTauLdInd(mod, type, type->tag, ptr,
                            tauUnsafe, tauUnsafe);
    return dst;
}

Opnd*
IRBuilder::genTauLdInd(Modifier mod, Type* type, Type::Tag ldType, Opnd* ptr, 
                       Opnd *tauBaseNonNull, Opnd *tauAddressInRange) {
    ptr = propagateCopy(ptr);
    tauBaseNonNull = propagateCopy(tauBaseNonNull);
    tauAddressInRange = propagateCopy(tauAddressInRange);
    Opnd* dst = createOpnd(type);
    appendInst(instFactory->makeTauLdInd(mod, ldType, dst, ptr, 
                                        tauBaseNonNull, tauAddressInRange));
    return dst;
}

Opnd*
IRBuilder::genLdRef(Modifier mod, Type* type, 
                    U_32 token, MethodDesc *enclosingMethod)
{
    Opnd* dst = createOpnd(type);
    appendInst(instFactory->makeLdRef(mod, dst, enclosingMethod, token));
    return dst;
}

Opnd*
IRBuilder::genLdField(Type* type, Opnd* base, FieldDesc* fieldDesc) {
    assert(!fieldDesc->isStatic());
    base = propagateCopy(base);
    Opnd *tauNullCheck = genTauCheckNull(base);
    Opnd *tauAddressInRange = 
        genTauHasType(base, fieldDesc->getParentType());
    
    bool uncompress = false;
    if (irBuilderFlags.compressedReferences && type->isObject()) {
        assert(!type->isCompressedReference());
        uncompress = true;
    }
    Modifier mod = uncompress ? AutoCompress_Yes : AutoCompress_No;
    if (irBuilderFlags.expandMemAddrs) {
        return genTauLdInd(mod, type, type->tag, 
                           genLdFieldAddr(type, base, fieldDesc), 
                           tauNullCheck, tauAddressInRange);
    }

    Opnd* dst = createOpnd(type);
    appendInst(instFactory->makeTauLdField(mod, type, dst, base, 
                                          tauNullCheck, tauAddressInRange, 
                                          fieldDesc));
    return dst;
}

Opnd*
IRBuilder::genLdFieldWithResolve(Type* type, Opnd* base, ObjectType* enclClass, U_32 cpIndex) {
    base = propagateCopy(base);
    Opnd *tauNullCheck = genTauCheckNull(base);
    Opnd *tauAddressInRange = genTauSafe();

    bool uncompress = false;
    if (irBuilderFlags.compressedReferences && type->isObject()) {
        assert(!type->isCompressedReference());
        uncompress = true;
    }
    Modifier mod = uncompress ? AutoCompress_Yes : AutoCompress_No;
    assert(irBuilderFlags.expandMemAddrs);

    Opnd* addr = genLdFieldAddrWithResolve(type, base, enclClass, cpIndex, false);
    return genTauLdInd(mod, type, type->tag, addr,  tauNullCheck, tauAddressInRange);
}

void
IRBuilder::genInitType(NamedType* type) {
    if (!type->needsInitialization()) {
        return;
    }
    MethodDesc * m = irManager->getCompilationInterface().getMethodToCompile();
    NamedType* classType = m->getParentType();
    if (type == classType) {
        return;
    }
    Opnd* opnd = lookupHash(Op_InitType, type->getId());
    if (opnd) return; // no need to re-initialize

    insertHash(Op_InitType, type->getId(),  appendInst(instFactory->makeInitType(type)));
}

Opnd*
IRBuilder::genLdStaticWithResolve(Type* type, ObjectType* enclClass, U_32 cpIdx) {
    bool uncompress = false;
    if (irBuilderFlags.compressedReferences && type->isObject()) {
        assert(!type->isCompressedReference());
        uncompress = true;
    }
    Modifier mod = uncompress ? AutoCompress_Yes : AutoCompress_No;

    Opnd *tauOk = genTauSafe(); // static field, always safe
    Opnd* addrOpnd = genLdStaticAddrWithResolve(type, enclClass, cpIdx, false);
    return genTauLdInd(mod, type, type->tag, addrOpnd, tauOk, tauOk);
}

Opnd*
IRBuilder::genLdStatic(Type* type, FieldDesc* fieldDesc) {
    bool uncompress = false;
    if (irBuilderFlags.compressedReferences && type->isObject()) {
        assert(!type->isCompressedReference());
        uncompress = true;
    }
    Modifier mod = uncompress ? AutoCompress_Yes : AutoCompress_No;

    genInitType(fieldDesc->getParentType());
    if (irBuilderFlags.expandMemAddrs) {
        Opnd *tauOk = genTauSafe(); // static field, always safe
        return genTauLdInd(mod, type, type->tag, genLdStaticAddr(type, fieldDesc),
                           tauOk, tauOk);
    }

    Opnd* dst = createOpnd(type);
    appendInst(instFactory->makeLdStatic(mod, type, dst, fieldDesc));
    return dst;
}


Opnd*
IRBuilder::genLdElem(Type* type, Opnd* array, Opnd* index, Opnd* tauNullChecked, Opnd* tauAddressInRange) {

    assert(tauNullChecked);
    assert(tauAddressInRange);

    array = propagateCopy(array);
    index = propagateCopy(index);

    bool uncompress = false;
    if (irBuilderFlags.compressedReferences && type->isObject()) {
        assert(!type->isCompressedReference());
        uncompress = true;
    }
    Modifier mod = uncompress ? AutoCompress_Yes : AutoCompress_No;

    if (irBuilderFlags.expandMemAddrs) {
        return genTauLdInd(mod, type, type->tag, 
                           genLdElemAddrNoChecks(type, array, index),
                           tauNullChecked, tauAddressInRange);
    }
    Opnd* dst = createOpnd(type);
    appendInst(instFactory->makeTauLdElem(mod, type, dst, array, index,
                                         tauNullChecked, tauAddressInRange));
    return dst;
}

Opnd*
IRBuilder::genLdElem(Type* type, Opnd* array, Opnd* index) {

    array = propagateCopy(array);
    index = propagateCopy(index);

    Opnd *tauNullChecked = genTauCheckNull(array);
    Opnd *tauBoundsChecked = genTauCheckBounds(array, index, tauNullChecked);
    Opnd *tauBaseTypeChecked = genTauHasType(array, array->getType());
    Opnd *tauAddressInRange = genTauAnd(tauBoundsChecked, tauBaseTypeChecked);
    
    return genLdElem(type,array,index,tauNullChecked,tauAddressInRange);
}

Opnd*
IRBuilder::genLdFieldAddr(Type* type, Opnd* base, FieldDesc* fieldDesc) {
    assert(!fieldDesc->isStatic());

    base = propagateCopy(base);

    genTauCheckNull(base);
    
    Opnd* dst = lookupHash(Op_LdFieldAddr, base->getId(), fieldDesc->getId());
    if (dst) return dst;

    if (base->getType()->isIntPtr()) {
        // unmanaged pointer
        dst = createOpnd(typeManager->getIntPtrType());
    } else if (irBuilderFlags.compressedReferences && type->isObject()) {
        // until VM type system is upgraded,
        // fieldDesc type will have uncompressed ref type;
        // compress it
        assert(!type->isCompressedReference());
        Type *compressedType = typeManager->compressType(type);
        dst = createOpnd(typeManager->getManagedPtrType(compressedType));
    } else {
        dst = createOpnd(typeManager->getManagedPtrType(type));
    }
    appendInst(instFactory->makeLdFieldAddr(dst, base, fieldDesc));
    insertHash(Op_LdFieldAddr, base->getId(), fieldDesc->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdFieldAddrWithResolve(Type* type, Opnd* base, ObjectType* enclClass, U_32 cpIndex, bool putfield) {
    base = propagateCopy(base);
    genTauCheckNull(base);

    //1. loading field offset
    JavaByteCodes opcode = putfield? OPCODE_PUTFIELD : OPCODE_GETFIELD;
    Opnd* dst = lookupHash(Op_VMHelperCall, opcode, base->getId(), cpIndex);
    if (dst) return dst;

    if (irBuilderFlags.compressedReferences && type->isObject()) {
        // until VM type system is upgraded,
        // fieldDesc type will have uncompressed ref type;
        // compress it
        assert(!type->isCompressedReference());
        Type *compressedType = typeManager->compressType(type);
        dst = createOpnd(typeManager->getManagedPtrType(compressedType));
    } else {
        dst = createOpnd(typeManager->getManagedPtrType(type));
    }
    Opnd** args = new (irManager->getMemoryManager()) Opnd*[3];
    args[0] = createTypeOpnd(enclClass);
    args[1] = genLdConstant((int)cpIndex);
    args[2] = genLdConstant((int)putfield?1:0);
    Opnd* offsetOpnd = genVMHelperCall(VM_RT_GET_NONSTATIC_FIELD_OFFSET_WITHRESOLVE, 
                                    typeManager->getIntPtrType(), 3, args);
    insertHash(Op_VMHelperCall, opcode, base->getId(), cpIndex, dst->getInst());

    //2. adding the offset to object opnd -> getting the address of the field
    appendInst(instFactory->makeAddOffset(dst, base, offsetOpnd));
    return dst;
}

Opnd*
IRBuilder::genLdStaticAddr(Type* type, FieldDesc* fieldDesc) {
    genInitType(fieldDesc->getParentType());

    Opnd* dst = lookupHash(Op_LdStaticAddr, fieldDesc->getId());
    if (dst) return dst;

    if (irBuilderFlags.compressedReferences && type->isObject()) {
        // until VM type system is upgraded,
        // fieldDesc type will have uncompressed ref type;
        // compress it
        assert(!type->isCompressedReference());
        Type *compressedType = typeManager->compressType(type);
        dst = createOpnd(typeManager->getManagedPtrType(compressedType));
    } else {
        dst = createOpnd(typeManager->getManagedPtrType(type));
    }
    appendInst(instFactory->makeLdStaticAddr(dst, fieldDesc));
    insertHash(Op_LdStaticAddr, fieldDesc->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdStaticAddrWithResolve(Type* type, ObjectType* enclClass, U_32 cpIndex, bool putfield) {
    JavaByteCodes opcode = putfield ? OPCODE_PUTSTATIC : OPCODE_GETSTATIC;
    Opnd* dst = lookupHash(Op_VMHelperCall, opcode, cpIndex);
    if (dst) return dst;

    if (irBuilderFlags.compressedReferences && type->isObject()) {
        // until VM type system is upgraded,
        // fieldDesc type will have uncompressed ref type;
        // compress it
        assert(!type->isCompressedReference());
        Type *compressedType = typeManager->compressType(type);
        dst = createOpnd(typeManager->getManagedPtrType(compressedType));
    } else {
        dst = createOpnd(typeManager->getManagedPtrType(type));
    }
    Opnd** args = new (irManager->getMemoryManager()) Opnd*[3];
    args[0] = createTypeOpnd(enclClass);
    args[1] = genLdConstant((int)cpIndex);
    args[2] = genLdConstant((int)putfield?1:0);
    appendInst(instFactory->makeVMHelperCall(dst, VM_RT_GET_STATIC_FIELD_ADDR_WITHRESOLVE, 3, args));
    insertHash(Op_VMHelperCall, OPCODE_GETSTATIC, cpIndex, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdElemAddr(Type* elemType, Opnd* array, Opnd* index) {
    // null and bounds checks
    index = propagateCopy(index);
    array = propagateCopy(array);
    Opnd *tauNullChecked = genTauCheckNull(array);
    genTauCheckBounds(array, index, tauNullChecked);
    return genLdElemAddrNoChecks(elemType, array, index);
}

Opnd*
IRBuilder::genLdElemAddrNoChecks(Type* elemType, Opnd* array, Opnd* index) {
    Opnd* dst;
    if (irBuilderFlags.expandElemAddrs) {
        //
        // boundscheck array, index
        // ldarraybase array --> base
        // addindex    base, index --> dst
        //
        return genAddScaledIndex(genLdArrayBaseAddr(elemType, array), index);
    } else {
        //
        // Op_LdElemAddr
        //
        dst = lookupHash(Op_LdElemAddr, array, index);
        if (dst) return dst;

        if (irBuilderFlags.compressedReferences && elemType->isObject()) {
            // until VM type system is upgraded,
            // fieldDesc type will have uncompressed ref type;
            // compress it
            assert(!elemType->isCompressedReference());
            Type *compressedType = typeManager->compressType(elemType);
            dst = createOpnd(typeManager->getManagedPtrType(compressedType));
        } else {
            dst = createOpnd(typeManager->getManagedPtrType(elemType));
        }
        appendInst(instFactory->makeLdElemAddr(elemType, dst, array, index));
        insertHash(Op_LdElemAddr, array, index, dst->getInst());
    }

    return dst;
}

Opnd*
IRBuilder::genLdFunAddr(MethodDesc* methodDesc) {
    Opnd* dst = lookupHash(Op_LdFunAddr, methodDesc->getId());
    if (dst) return dst;

    dst = createOpnd(typeManager->getMethodPtrType(methodDesc));
    appendInst(instFactory->makeLdFunAddr(dst, methodDesc));
    insertHash(Op_LdFunAddr, methodDesc->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdFunAddrSlot(MethodDesc* methodDesc) {
    Opnd* dst = lookupHash(Op_LdFunAddrSlot, methodDesc->getId());
    if (dst) return dst;


    dst = createOpnd(typeManager->getMethodPtrType(methodDesc));
    appendInst(instFactory->makeLdFunAddrSlot(dst, methodDesc));
    insertHash(Op_LdFunAddrSlot, methodDesc->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdVTable(Opnd* base, Type* type) {
    base = propagateCopy(base);
    Opnd *tauNullChecked = genTauCheckNull(base);

    return genTauLdVTable(base, tauNullChecked, type);
}

Opnd*
IRBuilder::genTauLdVTable(Opnd* base, Opnd *tauNullChecked, Type* type) {
    base = propagateCopy(base);

    SsaOpnd* obj = base->asSsaOpnd();
    assert(obj);
    Opnd* dst = NULL;
    if (type->isInterface()) {
        dst = lookupHash(Op_TauLdIntfcVTableAddr, base->getId(), type->getId());
        if (dst) return dst;

        if (irBuilderFlags.useNewTypeSystem) {
            NamedType* iType = type->asNamedType();
            assert(iType);
            dst = createOpnd(typeManager->getITablePtrObjType(obj, iType));
        } else {
            dst = createOpnd(typeManager->getVTablePtrType(type));
        }
        appendInst(instFactory->makeTauLdIntfcVTableAddr(dst, base, type));
        insertHash(Op_TauLdIntfcVTableAddr, base->getId(), type->getId(),
                   dst->getInst());
    } else if (type->isClass()) {
        dst = lookupHash(Op_TauLdVTableAddr, base);
        if (dst) return dst;

        if (irBuilderFlags.useNewTypeSystem) {
            dst = createOpnd(typeManager->getVTablePtrObjType(obj));
        } else {
            dst = createOpnd(typeManager->getVTablePtrType(base->getType()));
        }
        appendInst(instFactory->makeTauLdVTableAddr(dst, base, tauNullChecked));
        insertHash(Op_TauLdVTableAddr, base, dst->getInst());
    } else {
        assert(0); // shouldn't happen
    }
    return dst;
}

Opnd*
IRBuilder::genGetVTable(ObjectType* type) {
    assert(type->isClass() && (!type->isAbstract() || type->isArray()));
    Opnd* dst = lookupHash(Op_GetVTableAddr, type->getId());
    if (dst) return dst;

    dst = createOpnd(typeManager->getVTablePtrType(type));
    appendInst(instFactory->makeGetVTableAddr(dst, type));
    insertHash(Op_GetVTableAddr, type->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genGetClassObj(ObjectType* type) {
    assert(type->isClass());
    Opnd* dst = lookupHash(Op_GetClassObj, type->getId());
    if (dst) return dst;

    Type* dstType = irManager->getTypeManager().getSystemClassType();
    dst = createOpnd(dstType);
    appendInst(instFactory->makeGetClassObj(dst, type));
    insertHash(Op_GetClassObj, type->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdVirtFunAddr(Opnd* base, MethodDesc* methodDesc) {
    base = propagateCopy(base);
    Opnd* dst = lookupHash(Op_TauLdVirtFunAddr, base->getId(), 
                           methodDesc->getId());
    if (dst) return dst;

    Opnd *tauNullChecked = genTauCheckNull(base);
    Type *methodType = methodDesc->getParentType();
    Opnd* vtableOpnd = genTauLdVTable(base, tauNullChecked, methodType);
    Opnd *tauVtableHasMethod = genTauHasType(base, methodType);

    dst = createOpnd(typeManager->getMethodPtrType(methodDesc));
    appendInst(instFactory->makeTauLdVirtFunAddr(dst, vtableOpnd, 
                                                tauVtableHasMethod,
                                                methodDesc));
    insertHash(Op_TauLdVirtFunAddr, vtableOpnd->getId(), methodDesc->getId(), 
               dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genTauLdVirtFunAddrSlot(Opnd* base, Opnd *tauOk, MethodDesc* methodDesc) {
    base = propagateCopy(base);
    Opnd* dst = lookupHash(Op_TauLdVirtFunAddrSlot, base->getId(), 
                           methodDesc->getId());
    if (dst) return dst;

    Opnd *tauNullChecked = genTauCheckNull(base);
    Opnd* vtableOpnd = genTauLdVTable(base, tauNullChecked, methodDesc->getParentType());
    Opnd *tauVtableHasMethod = tauOk;

    if (irBuilderFlags.useNewTypeSystem) {
        SsaOpnd* obj = base->asSsaOpnd();
        assert(obj);
        dst = createOpnd(typeManager->getMethodPtrObjType(obj, methodDesc));
    } else {
        dst = createOpnd(typeManager->getMethodPtrType(methodDesc));
    }
    appendInst(instFactory->makeTauLdVirtFunAddrSlot(dst, vtableOpnd, 
                                                    tauVtableHasMethod,
                                                    methodDesc));
    insertHash(Op_TauLdVirtFunAddrSlot, vtableOpnd->getId(), 
               methodDesc->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genArrayLen(Type* dstType, Type::Tag type, Opnd* array) {
    array = propagateCopy(array);
    
    Opnd* dst = lookupHash(Op_TauArrayLen, array->getId());
    if (dst) return dst;
    
    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyTauArrayLen(dstType, type, array);
        if (dst) return dst;
    }

    Opnd *tauNonNull = genTauCheckNull(array);
    Opnd *tauIsArray = genTauHasType(array, array->getType());
    
    return genTauArrayLen(dstType, type, array, tauNonNull, tauIsArray);
}

Opnd*
IRBuilder::genTauArrayLen(Type* dstType, Type::Tag type, Opnd* array,
                          Opnd* tauNullChecked, Opnd *tauTypeChecked) {
    array = propagateCopy(array);

    Opnd* dst = lookupHash(Op_TauArrayLen, array->getId());
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyTauArrayLen(dstType, type, array, tauNullChecked,
                                             tauTypeChecked);
        if (dst) return dst;
    }
    dst = createOpnd(dstType);
    appendInst(instFactory->makeTauArrayLen(dst, type, array, tauNullChecked,
                                           tauTypeChecked));
    insertHash(Op_TauArrayLen, array->getId(), dst->getInst());
    
    return dst;
}

Opnd*
IRBuilder::genLdArrayBaseAddr(Type* elemType, Opnd* array) {
    array = propagateCopy(array);

    Opnd* dst = lookupHash(Op_LdArrayBaseAddr, array);
    if (dst) return dst;

    if (irBuilderFlags.useNewTypeSystem) {
        SsaOpnd* arrayVal = array->asSsaOpnd();
        assert(arrayVal);
        Type* baseType = typeManager->getArrayBaseType(arrayVal);
        dst = createOpnd(baseType);
    } else {
        if (irBuilderFlags.compressedReferences && elemType->isObject()) {
            // until VM type system is upgraded,
            // fieldDesc type will have uncompressed ref type;
            // compress it
            assert(!elemType->isCompressedReference());
            Type *compressedType = typeManager->compressType(elemType);
            dst = createOpnd(typeManager->getManagedPtrType(compressedType));
        } else {        
            dst = createOpnd(typeManager->getManagedPtrType(elemType));
        }
    }
    appendInst(instFactory->makeLdArrayBaseAddr(elemType, dst, array));
    insertHash(Op_LdArrayBaseAddr, array, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genAddScaledIndex(Opnd* ptr, Opnd* index) {
    ptr = propagateCopy(ptr);
    index = propagateCopy(index);
    Opnd* dst = lookupHash(Op_AddScaledIndex, ptr, index);
    if (dst) return dst;

    if (irBuilderFlags.useNewTypeSystem) {
        PtrType* ptrType = ptr->getType()->asPtrType();
        assert(ptrType);
        SsaOpnd* indexVar = index->asSsaOpnd();
        assert(indexVar);
        Type* dstType = typeManager->getArrayIndexType(ptrType->getArrayName(), indexVar);
        dst = createOpnd(dstType);
    } else {
        dst = createOpnd(ptr->getType());
    }

    appendInst(instFactory->makeAddScaledIndex(dst, ptr, index));
    insertHash(Op_AddScaledIndex, ptr, index, dst->getInst());
    return dst;
}


Opnd*
IRBuilder::genUncompressRef(Opnd *compref)
{
    compref = propagateCopy(compref);
    Opnd* dst = lookupHash(Op_UncompressRef, compref);
    if (dst) return dst;

    Type *comprefType = compref->getType();
    assert(comprefType->isCompressedReference());

    dst = createOpnd(typeManager->uncompressType(comprefType));
    appendInst(instFactory->makeUncompressRef(dst, compref));
    insertHash(Op_UncompressRef, compref, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genCompressRef(Opnd *uncompref)
{
    Type *uncomprefType = uncompref->getType();

    uncompref = propagateCopy(uncompref);
    Opnd* dst = lookupHash(Op_CompressRef, uncompref);
    if (dst) return dst;

    uncomprefType = uncompref->getType();
    assert(uncomprefType->isReference() && !uncomprefType->isCompressedReference());
    
    dst = createOpnd(typeManager->compressType(uncomprefType));
    appendInst(instFactory->makeCompressRef(dst, uncompref));
    insertHash(Op_CompressRef, uncompref, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdFieldOffset(FieldDesc* fieldDesc)
{
    assert(!fieldDesc->isStatic());

    Opnd *dst = lookupHash(Op_LdFieldOffset, fieldDesc->getId());
    if (dst) return dst;


    dst = createOpnd(typeManager->getOffsetType());
    appendInst(instFactory->makeLdFieldOffset(dst, fieldDesc));
    insertHash(Op_LdFieldOffset, fieldDesc->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdFieldOffsetPlusHeapbase(FieldDesc* fieldDesc)
{
    assert(!fieldDesc->isStatic());

    Opnd *dst = lookupHash(Op_LdFieldOffsetPlusHeapbase, fieldDesc->getId());
    if (dst) return dst;


    dst = createOpnd(typeManager->getOffsetPlusHeapbaseType());
    appendInst(instFactory->makeLdFieldOffsetPlusHeapbase(dst, fieldDesc));
    insertHash(Op_LdFieldOffsetPlusHeapbase, fieldDesc->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdArrayBaseOffset(Type *elemType)
{
    Opnd* dst = lookupHash(Op_LdArrayBaseOffset, elemType->getId());
    if (dst) return dst;

    dst = createOpnd(typeManager->getOffsetType());
    appendInst(instFactory->makeLdArrayBaseOffset(dst, elemType));
    insertHash(Op_LdArrayBaseOffset, elemType->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdArrayBaseOffsetPlusHeapbase(Type *elemType)
{
    Opnd* dst = lookupHash(Op_LdArrayBaseOffsetPlusHeapbase, elemType->getId());
    if (dst) return dst;

    dst = createOpnd(typeManager->getOffsetPlusHeapbaseType());
    appendInst(instFactory->makeLdArrayBaseOffsetPlusHeapbase(dst, elemType));
    insertHash(Op_LdArrayBaseOffsetPlusHeapbase, elemType->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdArrayLenOffset(Type *elemType)
{
    Opnd* dst = lookupHash(Op_LdArrayLenOffset, elemType->getId());
    if (dst) return dst;

    dst = createOpnd(typeManager->getOffsetType());
    appendInst(instFactory->makeLdArrayLenOffset(dst, elemType));
    insertHash(Op_LdArrayLenOffset, elemType->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genLdArrayLenOffsetPlusHeapbase(Type *elemType)
{
    Opnd* dst = lookupHash(Op_LdArrayLenOffsetPlusHeapbase, elemType->getId());
    if (dst) return dst;

    dst = createOpnd(typeManager->getOffsetPlusHeapbaseType());
    appendInst(instFactory->makeLdArrayLenOffsetPlusHeapbase(dst, elemType));
    insertHash(Op_LdArrayLenOffsetPlusHeapbase, elemType->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genAddOffset(Type *ptrType, Opnd* ref, Opnd* offset)
{
    ref = propagateCopy(ref);
    offset = propagateCopy(offset);
    Opnd* dst = lookupHash(Op_AddOffset, ref, offset);
    if (dst) return dst;


    assert(!ref->getType()->isCompressedReference());
    assert(offset->getType()->isOffset());

    dst = createOpnd(ptrType);
    appendInst(instFactory->makeAddOffset(dst, ref, offset));
    insertHash(Op_AddOffset, ref, offset, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genAddOffsetPlusHeapbase(Type *ptrType, Opnd* compref, Opnd* offset)
{
    compref = propagateCopy(compref);
    offset = propagateCopy(offset);
    Opnd* dst = lookupHash(Op_AddOffsetPlusHeapbase, compref, offset);
    if (dst) return dst;


    assert(compref->getType()->isCompressedReference());
    assert(offset->getType()->isOffsetPlusHeapbase());
    
    dst = createOpnd(ptrType);
    appendInst(instFactory->makeAddOffsetPlusHeapbase(dst, compref, offset));
    insertHash(Op_AddOffsetPlusHeapbase, compref, offset, dst->getInst());
    return dst;
}

// store instructions
void
IRBuilder::genStVar(VarOpnd* var, Opnd* src) {
    src = propagateCopy(src);
    appendInst(instFactory->makeStVar(var, src));
    if (irBuilderFlags.doCSE) {
        insertHash(Op_LdVar, var->getId(), src->getInst());
    }
}

void
IRBuilder::genStInd(Type* type,
                    Opnd* ptr,
                    Opnd* src) {
    ptr = propagateCopy(ptr);
    src = propagateCopy(src);

    Type *ptrType = ptr->getType();
    assert(ptrType->isPtr() || ptrType->isIntPtr());
    Type *fieldType = ((PtrType *)ptrType)->getPointedToType();
    bool compress = (fieldType->isCompressedReference() &&
                     !type->isCompressedReference());
    Modifier compressMod = Modifier(compress ? AutoCompress_Yes
                                    : AutoCompress_No);
    
    Opnd *tauUnsafe = genTauUnsafe();

    if (irBuilderFlags.insertWriteBarriers) {
        appendInst(instFactory->makeTauStInd((Modifier(Store_WriteBarrier)|
                                             compressMod),
                                            type->tag, src, ptr, 
                                            tauUnsafe, tauUnsafe, tauUnsafe));
    } else {
        appendInst(instFactory->makeTauStInd((Modifier(Store_NoWriteBarrier)|
                                             compressMod),
                                            type->tag, src, ptr,
                                            tauUnsafe, tauUnsafe, tauUnsafe));
    }
}

void
IRBuilder::genTauStInd(Type* type,
                       Opnd* ptr,
                       Opnd* src,
                       Opnd* tauBaseNonNull, 
                       Opnd *tauAddressInRange,
                       Opnd* tauElemTypeChecked) {
    ptr = propagateCopy(ptr);
    src = propagateCopy(src);

    Type *ptrType = ptr->getType();
    assert(ptrType->isPtr() || ptrType->isIntPtr());
    Type *fieldType = ((PtrType *)ptrType)->getPointedToType();
    if (fieldType->isArrayElement()) {
        fieldType = fieldType->getNonValueSupertype();
    }
    bool compress = (fieldType->isCompressedReference() &&
                     !type->isCompressedReference());
    Modifier compressMod = Modifier(compress ? AutoCompress_Yes
                                    : AutoCompress_No);
    
    if (irBuilderFlags.insertWriteBarriers) {
        appendInst(instFactory->makeTauStInd((Modifier(Store_WriteBarrier)|
                                             compressMod),
                                            type->tag, src, ptr, 
                                            tauBaseNonNull, tauAddressInRange, tauElemTypeChecked));
    } else {
        appendInst(instFactory->makeTauStInd((Modifier(Store_NoWriteBarrier)|
                                             compressMod),
                                            type->tag, src, ptr,
                                            tauBaseNonNull, tauAddressInRange, tauElemTypeChecked));
    }
}

void
IRBuilder::genTauStRef(Type* type, Opnd *objectbase, Opnd* ptr, Opnd* src,
                       Opnd *tauBaseNonNull, Opnd *tauAddressInRange,
                       Opnd *tauElemTypeChecked) {
    objectbase = propagateCopy(objectbase);
    ptr = propagateCopy(ptr);
    src = propagateCopy(src);
    tauBaseNonNull = propagateCopy(tauBaseNonNull);
    tauAddressInRange = propagateCopy(tauAddressInRange);
    tauElemTypeChecked = propagateCopy(tauElemTypeChecked);

    Type *ptrType = ptr->getType();
    assert(ptrType->isPtr());
    Type *fieldType = ((PtrType *)ptrType)->getPointedToType();
    if (fieldType->isArrayElement()) {
        fieldType = fieldType->getNonValueSupertype();
    }
    bool compress = (fieldType->isCompressedReference() &&
                     !type->isCompressedReference());
    Modifier compressMod = Modifier(compress ? AutoCompress_Yes
                                    : AutoCompress_No);
    if (irBuilderFlags.insertWriteBarriers) {
        appendInst(instFactory->makeTauStRef((Modifier(Store_WriteBarrier)|
                                             compressMod),
                                            type->tag, src, ptr, objectbase,
                                            tauBaseNonNull, tauAddressInRange, 
                                            tauElemTypeChecked));
    } else {
        appendInst(instFactory->makeTauStRef((Modifier(Store_NoWriteBarrier)|
                                             compressMod),
                                            type->tag, src, ptr, objectbase,
                                            tauBaseNonNull, tauAddressInRange, 
                                            tauElemTypeChecked));
    }
}

void
IRBuilder::genStField(Type* type, Opnd* base, FieldDesc* fieldDesc,Opnd* src) {
    assert (!fieldDesc->isStatic());

    base = propagateCopy(base);
    src = propagateCopy(src);
    Opnd *tauBaseNonNull = genTauCheckNull(base);
    Opnd *tauBaseTypeIsOk = genTauHasType(base, fieldDesc->getParentType());
//    Type *fieldType = fieldDesc->getFieldType();
    Opnd *tauStoredTypeIsOk = (type->isObject()
                               ? genTauHasType(src, type)
                               : genTauSafe()); // safe, not an object
    if (irBuilderFlags.expandMemAddrs) { // do not expand ldField of stack values
        Opnd *ptr = genLdFieldAddr(type, base, fieldDesc);
        if (irBuilderFlags.insertWriteBarriers && src->getType()->isObject()) {
            genTauStRef(type, base, ptr, src, 
                        tauBaseNonNull, 
                        tauBaseTypeIsOk,
                        tauStoredTypeIsOk); 
        } else {
            genTauStInd(type, ptr, src, 
                        tauBaseNonNull, 
                        tauBaseTypeIsOk,
                        tauStoredTypeIsOk);
        }
    } else {
        if (irBuilderFlags.insertWriteBarriers &&
            base->getType()->isValue()==false) {
            appendInst(instFactory->makeTauStField((Modifier(Store_WriteBarrier)|
                                                   Modifier(AutoCompress_Yes)), 
                                                  type->tag, src, base,
                                                  tauBaseNonNull, 
                                                  tauBaseTypeIsOk,
                                                  tauStoredTypeIsOk,
                                                  fieldDesc));
        } else {
            appendInst(instFactory->makeTauStField((Modifier(Store_NoWriteBarrier)|
                                                   Modifier(AutoCompress_Yes)), 
                                                  type->tag, src, base,
                                                  tauBaseNonNull, 
                                                  tauBaseTypeIsOk,
                                                  tauStoredTypeIsOk,
                                                  fieldDesc));
        }
    }
}


void       
IRBuilder::genStFieldWithResolve(Type* type, Opnd* base, ObjectType* enclClass, U_32 cpIdx, Opnd* src) {
    base = propagateCopy(base);
    src = propagateCopy(src);
    Opnd *tauBaseNonNull = genTauCheckNull(base);
    Opnd *tauBaseTypeIsOk = genTauSafe();
    Opnd *tauStoredTypeIsOk = (type->isObject() ? genTauHasType(src, type) : genTauSafe()); 
    assert(irBuilderFlags.expandMemAddrs);
    Opnd *ptr = genLdFieldAddrWithResolve(type, base, enclClass, cpIdx, true);
    if (irBuilderFlags.insertWriteBarriers && src->getType()->isObject()) {
        genTauStRef(type, base, ptr, src, 
            tauBaseNonNull, 
            tauBaseTypeIsOk,
            tauStoredTypeIsOk); 
    } else {
        genTauStInd(type, ptr, src, 
            tauBaseNonNull, 
            tauBaseTypeIsOk,
            tauStoredTypeIsOk);
    }
}


void       
IRBuilder::genStStaticWithResolve(Type* type, ObjectType* enclClass, U_32 cpIdx, Opnd* src) {
    src = propagateCopy(src);
    Opnd *tauOk = genTauSafe(); // address is always ok
    Opnd *tauTypeIsOk = type->isObject() ? genTauHasType(src, type) : genTauSafe();
    assert(irBuilderFlags.expandMemAddrs);
    Opnd* addr = genLdStaticAddrWithResolve(type, enclClass, cpIdx, true);
    genTauStInd(type, addr, src, tauOk,  tauOk, tauTypeIsOk);
    return;
}

void
IRBuilder::genStStatic(Type* type, FieldDesc* fieldDesc, Opnd* src) {
    src = propagateCopy(src);
    genInitType(fieldDesc->getParentType());
    Opnd *tauOk = genTauSafe(); // address is always ok
//    Type *fieldType = fieldDesc->getFieldType();
    Opnd *tauTypeIsOk = (type->isObject() 
                         ? genTauHasType(src, type)
                         : genTauSafe()); // safe, not an object
    if (irBuilderFlags.expandMemAddrs) {
        genTauStInd(type, genLdStaticAddr(type, fieldDesc), src,
                    tauOk, 
                    tauOk,
                    tauTypeIsOk // safety may depend on a type check
                    );
        return;
    }
    if (irBuilderFlags.insertWriteBarriers) {
        appendInst(instFactory->makeTauStStatic((Modifier(Store_WriteBarrier)|
                                                Modifier(AutoCompress_Yes)),
                                               type->tag, src,
                                               tauTypeIsOk,
                                               fieldDesc));
    } else {
        appendInst(instFactory->makeTauStStatic((Modifier(Store_NoWriteBarrier)|
                                                Modifier(AutoCompress_Yes)), 
                                               type->tag, src,
                                               tauTypeIsOk,
                                               fieldDesc));
    }
}

void
IRBuilder::genStElem(Type* elemType,
                     Opnd* array,
                     Opnd* index,
                     Opnd* src,
                     Opnd* tauNullChecked,
                     Opnd* tauBaseTypeChecked,
                     Opnd* tauAddressInRange) {
    array = propagateCopy(array);
    src = propagateCopy(src);
    index = propagateCopy(index);
    
    Opnd *tauElemTypeChecked = NULL;
    if (elemType->isObject()) {
        tauElemTypeChecked = genTauCheckElemType(array, src, tauNullChecked,
                                                 tauBaseTypeChecked);
    } else {
        tauElemTypeChecked = genTauSafe(); // src type is ok if non-object
    }
    if (irBuilderFlags.expandMemAddrs) {
        Opnd *ptr = NULL;
        if (tauNullChecked && tauAddressInRange) {
            ptr = genLdElemAddrNoChecks(elemType, array, index);
        } else {
            ptr = genLdElemAddr(elemType, array, index);
        }
        if (irBuilderFlags.insertWriteBarriers && elemType->isObject()) {
            genTauStRef(elemType, array, ptr, src, tauNullChecked, tauAddressInRange,
                        tauElemTypeChecked);
        } else {
            genTauStInd(elemType, ptr, src, tauNullChecked, tauAddressInRange,
                        tauElemTypeChecked);
        }
    } else {
        if (irBuilderFlags.insertWriteBarriers) {
            appendInst(instFactory->makeTauStElem((Modifier(Store_WriteBarrier)|
                                                  Modifier(AutoCompress_Yes)), 
                                                 elemType->tag, src, array, index,
                                                 tauNullChecked, 
                                                 tauAddressInRange,
                                                 tauElemTypeChecked));
        } else {
            appendInst(instFactory->makeTauStElem((Modifier(Store_NoWriteBarrier)|
                                                  Modifier(AutoCompress_Yes)), 
                                                 elemType->tag, src, array, index,
                                                 tauNullChecked, 
                                                 tauAddressInRange,
                                                 tauElemTypeChecked));
        }
    }
}

void
IRBuilder::genStElem(Type* elemType,
                     Opnd* array,
                     Opnd* index,
                     Opnd* src) {

    array = propagateCopy(array);
    src = propagateCopy(src);
    index = propagateCopy(index);

    Opnd *tauNullChecked = NULL;
    Opnd *tauAddressInRange = NULL;
    Opnd *tauBaseTypeChecked = NULL;

    // prepare checks
    tauNullChecked = genTauCheckNull(array);
    tauBaseTypeChecked = genTauHasType(array, array->getType());
    Opnd *tauBoundsChecked = genTauCheckBounds(array, index, tauNullChecked);
    tauAddressInRange = genTauAnd(tauBaseTypeChecked, tauBoundsChecked);

    genStElem(elemType,array,index,src,tauNullChecked,tauBaseTypeChecked,tauAddressInRange);
}


Opnd*
IRBuilder::genNewObj(Type* type) {
    assert(type->isNamedType());
    Opnd* dst = createOpnd(type);
    //FIXME class initialization must be done before allocating new object
    appendInst(instFactory->makeNewObj(dst, type));
    genInitType(type->asNamedType());
    return dst;
}

Opnd*
IRBuilder::genNewObjWithResolve(ObjectType* enclClass, U_32 cpIndex) {
    Opnd* clsOpnd = createTypeOpnd(enclClass);
    Opnd* idxOpnd = genLdConstant((int)cpIndex);
    Opnd** args = new (irManager->getMemoryManager()) Opnd*[2];
    args[0]=clsOpnd;
    args[1]=idxOpnd;
    Opnd* res = genVMHelperCall(VM_RT_NEWOBJ_WITHRESOLVE, typeManager->getUnresolvedObjectType(), 2, args);
    return res;
}

Opnd*
IRBuilder::genNewArrayWithResolve(NamedType* elemType, Opnd* numElems, ObjectType* enclClass, U_32 cpIndex) {
    numElems = propagateCopy(numElems);
    Opnd* clsOpnd = createTypeOpnd(enclClass);
    Opnd* idxOpnd = genLdConstant((int)cpIndex);
    Opnd** args = new (irManager->getMemoryManager()) Opnd*[3];
    args[0]=clsOpnd;
    args[1]=idxOpnd;
    args[2]=numElems;
    Type* resType = typeManager->getArrayType(elemType);
    Opnd* res = genVMHelperCall(VM_RT_NEWARRAY_WITHRESOLVE, resType, 3, args);
    return res;
}

Opnd*
IRBuilder::genNewArray(NamedType* elemType, Opnd* numElems) {
    numElems = propagateCopy(numElems);
    Opnd* dst = createOpnd(typeManager->getArrayType(elemType));
    appendInst(instFactory->makeNewArray(dst, numElems, elemType));
    return dst;
}

Opnd*
IRBuilder::genMultianewarray(NamedType* arrayType,
                             U_32 dimensions,
                             Opnd** numElems) {
    NamedType* elemType = arrayType;
    // create an array of arrays type
    for (U_32 i=0; i<dimensions; i++) {
        elemType = ((ArrayType*)elemType)->getElementType();
    }
    Opnd* dst = createOpnd(arrayType);
    appendInst(instFactory->makeNewMultiArray(dst, dimensions, numElems, elemType));
    return dst;
}

Opnd*
IRBuilder::genMultianewarrayWithResolve(NamedType* arrayType,
                                        ObjectType* enclClass, 
                                        U_32 cpIndex,
                                        U_32 dimensions,
                                        Opnd** numElems) 
{
    Opnd* enclClsOpnd = createTypeOpnd(enclClass);
    Opnd* idxOpnd = genLdConstant((int)cpIndex);
    Opnd** args = new (irManager->getMemoryManager()) Opnd*[2];
    args[0] = enclClsOpnd;
    args[1] = idxOpnd;
    Opnd* clsOpnd = genVMHelperCall(VM_RT_INITIALIZE_CLASS_WITHRESOLVE, 
                                    typeManager->getUnmanagedPtrType(typeManager->getInt8Type()),
                                    2, args);
    
    size_t nArgs2 = 2+dimensions;
    Opnd** args2  = new (irManager->getMemoryManager()) Opnd*[nArgs2];
    args2[0]=clsOpnd;
    args2[1]=genLdConstant((int)dimensions);
    // create an array of arrays type
    for (U_32 i=0; i<dimensions; i++) {
        args2[i+2]=numElems[dimensions-1-i];
    }
    Opnd* dst = genVMHelperCall(VM_RT_MULTIANEWARRAY_RESOLVED, arrayType, (U_32)nArgs2, args2);
    return dst;
}


void
IRBuilder::genMonitorEnter(Opnd* src) {
    src = propagateCopy(src);
    Opnd *tauNullChecked = genTauCheckNull(src);
    appendInst(instFactory->makeTauMonitorEnter(src, tauNullChecked));
}

void
IRBuilder::genMonitorExit(Opnd* src) {
    src = propagateCopy(src);
    Opnd *tauNullChecked = genTauCheckNull(src);
    appendInst(instFactory->makeTauMonitorExit(src, tauNullChecked));
}

Opnd*
IRBuilder::genLdLockAddr(Type* dstType, Opnd* obj) {
    obj = propagateCopy(obj);
    Opnd* dst = lookupHash(Op_LdLockAddr, obj);
    if (dst) return dst;

    dst = createOpnd(dstType);
    appendInst(instFactory->makeLdLockAddr(dst, obj));
    insertHash(Op_LdLockAddr, obj, dst->getInst());
    return dst;
}               

void
IRBuilder::genIncRecCount(Opnd* obj, Opnd *oldLock) {
    obj = propagateCopy(obj);
    oldLock = propagateCopy(oldLock);
    appendInst(instFactory->makeLdLockAddr(obj, oldLock));
}               


Opnd*
IRBuilder::genBalancedMonitorEnter(Type* dstType, Opnd* src, Opnd *lockAddr) {
    // src should already have been checked for null
    src = propagateCopy(src);
    lockAddr = propagateCopy(lockAddr);
    Opnd *tauNullChecked = genTauCheckNull(src);
    return genTauBalancedMonitorEnter(dstType, src, lockAddr, tauNullChecked);
}

Opnd*
IRBuilder::genTauBalancedMonitorEnter(Type* dstType, Opnd* src, Opnd *lockAddr,
                                      Opnd* tauNullChecked) {
    // src should already have been checked for null
    src = propagateCopy(src);
    lockAddr = propagateCopy(lockAddr);
    Opnd *dst = createOpnd(dstType);
    appendInst(instFactory->makeTauBalancedMonitorEnter(dst, src, lockAddr,
                                                       tauNullChecked));
    return dst;
}

void
IRBuilder::genBalancedMonitorExit(Opnd* src, Opnd *lockAddr, Opnd *oldValue) {
    // src should already have been checked for null
    src = propagateCopy(src);
    appendInst(instFactory->makeBalancedMonitorExit(src, lockAddr, oldValue));
}

Opnd*
IRBuilder::genTauOptimisticBalancedMonitorEnter(Type* dstType, Opnd* src, 
                                                Opnd *lockAddr,
                                                Opnd *tauNullChecked) {
    // src should already have been checked for null
    src = propagateCopy(src);
    lockAddr = propagateCopy(lockAddr);
    Opnd *dst = createOpnd(dstType);
    appendInst(instFactory->makeTauOptimisticBalancedMonitorEnter(dst, src, 
                                                                 lockAddr,
                                                                 tauNullChecked));
    return dst;
}

void
IRBuilder::genMonitorEnterFence(Opnd* src) {
    src = propagateCopy(src);
    appendInst(instFactory->makeMonitorEnterFence(src));
}

void
IRBuilder::genMonitorExitFence(Opnd* src) {
    src = propagateCopy(src);
    appendInst(instFactory->makeMonitorExitFence(src));
}

void
IRBuilder::genTypeMonitorEnter(Type* type) {
    appendInst(instFactory->makeTypeMonitorEnter(type));
}

void
IRBuilder::genTypeMonitorExit(Type* type) {
    appendInst(instFactory->makeTypeMonitorExit(type));
}


// type checking
// CastException (succeeds if argument is null, returns casted object)
Opnd*
IRBuilder::genCast(Opnd* src, Type* castType) {
    src = propagateCopy(src);
    Opnd* dst = lookupHash(Op_TauCast, src->getId(), castType->getId());
    if (dst) return dst;

    Opnd *tauCheckedCast = lookupHash(Op_TauCheckCast, src->getId(), castType->getId());
    if (!tauCheckedCast) {
        Opnd *tauNullChecked = lookupHash(Op_TauCheckNull, src->getId());
        if (!tauNullChecked) {
            tauNullChecked = genTauUnsafe();
        }

        tauCheckedCast = genTauCheckCast(src, tauNullChecked, castType);
    }
    
    dst = genTauStaticCast(src, tauCheckedCast, castType);
    insertHash(Op_TauCast, src->getId(), castType->getId(), dst->getInst());
    return dst;
}

// type checking
// CastException (succeeds if argument is null, returns casted object)
Opnd*
IRBuilder::genCastWithResolve(Opnd* src, Type* type, ObjectType* enclClass, U_32 cpIndex) {
    src = propagateCopy(src);
    Opnd* dst = lookupHash(Op_VMHelperCall, OPCODE_CHECKCAST, src->getId(), cpIndex);
    if (dst) return dst;

    Opnd** args = new (irManager->getMemoryManager()) Opnd*[3];
    args[0] = createTypeOpnd(enclClass);
    args[1] = genLdConstant((int)cpIndex);
    args[2] = src;
    dst = genVMHelperCall(VM_RT_CHECKCAST_WITHRESOLVE, type, 3, args);
    insertHash(Op_VMHelperCall, OPCODE_CHECKCAST, src->getId(), cpIndex, dst->getInst());
    return dst;
}


Opnd*
IRBuilder::genTauCheckCast(Opnd* src, Opnd *tauNullChecked, Type* castType) {
    src = propagateCopy(src);
    tauNullChecked = propagateCopy(tauNullChecked);
    Opnd* dst = lookupHash(Op_TauCheckCast, src->getId(), castType->getId());
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        bool alwaysThrows = false;
        dst = simplifier->simplifyTauCheckCast(src, tauNullChecked, castType, alwaysThrows);
        if (dst) {
            return dst;
        }
    }
    
    dst = createOpnd(typeManager->getTauType());
    appendInst(instFactory->makeTauCheckCast(dst, src, tauNullChecked, castType));
    insertHash(Op_TauCheckCast, src->getId(), castType->getId(), dst->getInst());
    return dst;
}

// returns src if src is an instance of type, NULL otherwise
Opnd*
IRBuilder::genAsType(Opnd* src, Type* type) {
    if (type->isUserValue()) {
        assert(0);    
    }
    src = propagateCopy(src);

    Opnd* tauCheckedNull = genTauUnsafe();

    Opnd* dst = lookupHash(Op_TauAsType, src->getId(), tauCheckedNull->getId(), type->getId());
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyTauAsType(src, tauCheckedNull, type);
        if (dst) return dst;
    }
    dst = createOpnd(type);
    appendInst(instFactory->makeTauAsType(dst, src, tauCheckedNull, type));
    insertHash(Op_TauAsType, src->getId(), tauCheckedNull->getId(), type->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genInstanceOf(Opnd* src, Type* type) {
    src = propagateCopy(src);

    Opnd *tauNullChecked = genTauUnsafe();

    Opnd* dst = lookupHash(Op_TauInstanceOf, src->getId(), 
                           tauNullChecked->getId(), type->getId());
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        dst = simplifier->simplifyTauInstanceOf(src, tauNullChecked, type);
        if (dst) return dst;
    }

    dst = createOpnd(typeManager->getInt32Type());
    appendInst(instFactory->makeTauInstanceOf(dst, src, tauNullChecked, type));
    insertHash(Op_TauInstanceOf, src->getId(), type->getId(), 
               tauNullChecked->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genInstanceOfWithResolve(Opnd* src, ObjectType* enclClass, U_32 cpIndex) {
    src = propagateCopy(src);

    Opnd* dst = lookupHash(Op_VMHelperCall, OPCODE_INSTANCEOF, src->getId(), cpIndex);
    if (dst) {
        return dst;
    }

    Opnd** args = new (irManager->getMemoryManager()) Opnd*[3];
    args[0] = createTypeOpnd(enclClass);
    args[1] = genLdConstant((int)cpIndex);
    args[2] = src;
    dst = genVMHelperCall(VM_RT_INSTANCEOF_WITHRESOLVE, typeManager->getInt32Type(), 3, args);
    insertHash(Op_VMHelperCall, OPCODE_INSTANCEOF, src->getId(), cpIndex,  dst->getInst());
    return dst;
}


//-----------------------------------------------------------------------------
//
// Private helper methods for generating instructions
//
//-----------------------------------------------------------------------------
Opnd*
IRBuilder::propagateCopy(Opnd* opnd) {
    return simplifier->propagateCopy(opnd);
}

Opnd*    IRBuilder::createOpnd(Type* type) {
    if (type->tag == Type::Void)
        return OpndManager::getNullOpnd();
    return opndManager->createSsaTmpOpnd(type);
}

PiOpnd*    IRBuilder::createPiOpnd(Opnd *org) {
    return opndManager->createPiOpnd(org);
}

Opnd* IRBuilder::genTauCheckNull(Opnd* base) {
    base = propagateCopy(base);
    if (! irBuilderFlags.expandNullChecks) {
        assert(0); // not expanding them is not compatible with taus
        return base;
    }

    Opnd* res = lookupHash(Op_TauCheckNull, base);
    if (res) return res;

    // Not advisable to turn off simplification of checknull because
    // IRBuilder calls genTauCheckNull redundantly many times
    bool alwaysThrows = false;
    res = simplifier->simplifyTauCheckNull(base, alwaysThrows);
    if (res && (res->getInst()->getOpcode() != Op_TauUnsafe)) return res;
    Opnd* dst = createOpnd(typeManager->getTauType());
    Inst *inst = appendInst(instFactory->makeTauCheckNull(dst, base));
    insertHash(Op_TauCheckNull, base, inst);

    // We can make the type init for the base object available here
    Type* baseType = base->getType();
    insertHash(Op_InitType, baseType->getId(), inst);
    return dst;
}

Opnd* IRBuilder::genTauCheckZero(Opnd* src) {
    src = propagateCopy(src);


    Opnd* res = lookupHash(Op_TauCheckZero, src);
    if (res) return res;

    bool alwaysThrows = false;
    res = simplifier->simplifyTauCheckZero(src, alwaysThrows);
    if (res && (res->getInst()->getOpcode() != Op_TauUnsafe)) return res;

    Opnd* dst = createOpnd(typeManager->getTauType());
    appendInst(instFactory->makeTauCheckZero(dst, src));
    insertHash(Op_TauCheckZero, src, dst->getInst());
    return dst;
}

Opnd *IRBuilder::genTauCheckDivOpnds(Opnd* src1, Opnd *src2) {
    src1 = propagateCopy(src1);
    src2 = propagateCopy(src2);


    Opnd* res = lookupHash(Op_TauCheckDivOpnds, src1, src2);
    if (res) return res;

    bool alwaysThrows = false;
    res = simplifier->simplifyTauCheckDivOpnds(src1, src2, alwaysThrows);
    if (res && (res->getInst()->getOpcode() != Op_TauUnsafe)) return res;

    Opnd* dst = createOpnd(typeManager->getTauType());
    Inst *inst = appendInst(instFactory->makeTauCheckDivOpnds(dst, src1, src2));
    insertHash(Op_TauCheckDivOpnds, src1, src2, inst);
    return dst;
}

Opnd *IRBuilder::genTauCheckBounds(Opnd* array, Opnd* index, Opnd *tauNullChecked) {
    // just to allow limit studies, omit all bounds checks 
    // if command-line flag is given
    if (irBuilderFlags.suppressCheckBounds)
        return genTauUnsafe(); 
    
    array = propagateCopy(array);
    index = propagateCopy(index);

    // we also hash operation with array as the opnd
    Opnd* res = lookupHash(Op_TauCheckBounds, array, index);
    if (res) return res;

    Opnd *tauArrayTypeChecked = genTauHasType(array, array->getType());
    Opnd* arrayLen = genTauArrayLen(typeManager->getInt32Type(), Type::Int32, array, 
                                    tauNullChecked, tauArrayTypeChecked);

    Opnd* dst = genTauCheckBounds(arrayLen, index);
    // we also hash operation with array as the opnd
    insertHash(Op_TauCheckBounds, array, index, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genTauCheckElemType(Opnd* array, Opnd* src, Opnd *tauNullChecked,
                               Opnd *tauIsArray) {
    if (! irBuilderFlags.expandElemTypeChecks)
        return genTauUnsafe();

    array = propagateCopy(array);
    src = propagateCopy(src);
    Opnd* res = lookupHash(Op_TauCheckElemType, array, src);
    if (res) return res;

    if (irBuilderFlags.doSimplify) {
        bool alwaysThrows = false;
        res = simplifier->simplifyTauCheckElemType(array, src, alwaysThrows);
        if (res && (res->getInst()->getOpcode() != Op_TauUnsafe)) return res;
    }
    Opnd* dst = createOpnd(typeManager->getTauType());
    Inst* inst = appendInst(instFactory->makeTauCheckElemType(dst, array, src, 
                                                             tauNullChecked,
                                                             tauIsArray));
    insertHash(Op_TauCheckElemType, array, src, inst);
    return dst;
}

Opnd *
IRBuilder::genTauCheckBounds(Opnd* ub, Opnd *index) {
    index = propagateCopy(index);
    ub = propagateCopy(ub);

    Opnd* dst = lookupHash(Op_TauCheckBounds, ub, index);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        bool alwaysThrows = false;
        dst = simplifier->simplifyTauCheckBounds(ub, index, alwaysThrows);
    }

    if (!(dst && (dst->getInst()->getOpcode() != Op_TauUnsafe))) {
        // need to create one
        dst = createOpnd(typeManager->getTauType());
        appendInst(instFactory->makeTauCheckBounds(dst, ub, index));
    }
    insertHash(Op_TauCheckBounds, ub, index, dst->getInst());
    return dst;
}

Opnd *
IRBuilder::genCheckFinite(Type *dstType, Opnd* src) {
    assert(dstType == src->getType());
    (void) genTauCheckFinite(src);
    return src;
}

Opnd *
IRBuilder::genTauCheckFinite(Opnd* src) {
    src = propagateCopy(src);
    Opnd* dst = lookupHash(Op_TauCheckFinite, src);
    if (dst) return dst;

    if (irBuilderFlags.doSimplify) {
        bool alwaysThrows = false;
        dst = simplifier->simplifyTauCheckFinite(src, alwaysThrows);
        if (dst && (dst->getInst()->getOpcode() != Op_TauUnsafe)) return dst;
    }
    
    dst = createOpnd(typeManager->getTauType());
    appendInst(instFactory->makeTauCheckFinite(dst, src));
    insertHash(Op_TauCheckFinite, src, dst->getInst());
    return dst;
}

//-----------------------------------------------------------------------------
//
// Methods for CSE hashing
//
//-----------------------------------------------------------------------------
Opnd* IRBuilder::lookupHash(U_32 opc) {
    if (! irBuilderFlags.doCSE)
        return NULL;
    Inst* inst = cseHashTable->lookup(opc);
    if (inst) 
        return inst->getDst();
    else 
        return NULL;
}

Opnd* IRBuilder::lookupHash(U_32 opc, U_32 op) {
    if (! irBuilderFlags.doCSE)
        return NULL;
    Inst* inst =  cseHashTable->lookup(opc, op);
    if (inst)
        return inst->getDst();
    else
        return NULL;
}

Opnd* IRBuilder::lookupHash(U_32 opc, U_32 op1, U_32 op2) {
    if (! irBuilderFlags.doCSE)
        return NULL;
    Inst* inst = cseHashTable->lookup(opc, op1, op2);
    if (inst)
        return inst->getDst();
    else
        return NULL;
}

Opnd* IRBuilder::lookupHash(U_32 opc, U_32 op1, U_32 op2, U_32 op3) {
    if (! irBuilderFlags.doCSE)
        return NULL;
    Inst* inst = cseHashTable->lookup(opc, op1, op2, op3);
    if (inst)
        return inst->getDst();
    else
        return NULL;
}

void IRBuilder::insertHash(U_32 opc, Inst* inst) {
    if (! irBuilderFlags.doCSE)
        return;
    cseHashTable->insert(opc, inst);
}

void IRBuilder::insertHash(U_32 opc, U_32 op1, Inst* inst) {
    if (! irBuilderFlags.doCSE)
        return;
    cseHashTable->insert(opc, op1, inst);
}

void IRBuilder::insertHash(U_32 opc, U_32 op1, U_32 op2, Inst* inst) {
    if (! irBuilderFlags.doCSE)
        return;
    cseHashTable->insert(opc, op1, op2, inst);
}

void IRBuilder::insertHash(U_32 opc, U_32 op1, U_32 op2, U_32 op3,
                           Inst* inst) {
    if (! irBuilderFlags.doCSE)
        return;
    cseHashTable->insert(opc, op1, op2, op3, inst);
}

// tau instructions
Opnd*
IRBuilder::genTauSafe() {
    Opnd* dst = lookupHash(Op_TauSafe);
    if (dst) return dst;

    dst = createOpnd(typeManager->getTauType());
    appendInst(instFactory->makeTauSafe(dst));

    insertHash(Op_TauSafe, dst->getInst());
    return dst;
}

// tau instructions
Opnd*
IRBuilder::genTauMethodSafe() {
    Opnd* dst = tauMethodSafeOpnd;
    if (dst) return dst;
    
    dst = createOpnd(typeManager->getTauType());
    Inst *inst = instFactory->makeTauPoint(dst);

    Node *head = flowGraph->getEntryNode();
    Inst *entryLabel = (Inst*)head->getFirstInst();
    // first search for one already there
    Inst *where = entryLabel->getNextInst();
    while (where != NULL) {
        if (where->getOpcode() != Op_DefArg) {
            break;
        }
        where = where->getNextInst();
    }
    // insert before where
    inst->insertBefore(where);

    tauMethodSafeOpnd = dst;
    return dst;
}

Opnd*
IRBuilder::genTauUnsafe() {
    Operation operation(Op_TauUnsafe, Type::Tau, Modifier());
    Opnd* dst = lookupHash(Op_TauUnsafe);
    if (dst) return dst;
    
    dst = createOpnd(typeManager->getTauType());
    appendInst(instFactory->makeTauUnsafe(dst));

    insertHash(Op_TauUnsafe, dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genTauStaticCast(Opnd *src, Opnd *tauCheckedCast, Type *castType) {
    Operation operation(Op_TauStaticCast, castType->tag, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src->getId(), tauCheckedCast->getId(), castType->getId());
    if (dst) return dst;
    
    dst = createOpnd(castType);
    appendInst(instFactory->makeTauStaticCast(dst, src, tauCheckedCast, castType));
    
    insertHash(hashcode, src->getId(), tauCheckedCast->getId(), castType->getId(), dst->getInst());
    Operation hasTypeOperation(Op_TauHasType, castType->tag, Modifier());
    U_32 hasTypeHashcode = hasTypeOperation.encodeForHashing();
    insertHash(hasTypeHashcode, src->getId(), castType->getId(), tauCheckedCast->getId(),
               dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genTauHasType(Opnd *src, Type *castType) {
    Operation operation(Op_TauHasType, castType->tag, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src->getId(), castType->getId());
    if (dst) return dst;
    
    dst = createOpnd(typeManager->getTauType());
    appendInst(instFactory->makeTauHasType(dst, src, castType));
    
    insertHash(hashcode, src->getId(), castType->getId(), dst->getInst());
    return dst;
}

Opnd* IRBuilder::genTauHasTypeWithConv(Opnd **srcPtr, Type *hasType) {
    Opnd* src = srcPtr[0];
    Opnd* res = genTauHasType(src, hasType);
    Type* srcType = src->getType();
    if (srcType->isUnresolvedType()) {
        //change opnd type in case if unresolved
        Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No);
        Opnd* newSrc = genConv(hasType, hasType->tag, mod,  src);
        srcPtr[0] = newSrc;
    }
    return res;
}

Opnd*
IRBuilder::genTauHasExactType(Opnd *src, Type *castType) {
    Operation operation(Op_TauHasExactType, castType->tag, Modifier());
    U_32 hashcode = operation.encodeForHashing();
    Opnd* dst = lookupHash(hashcode, src->getId(), castType->getId());
    if (dst) return dst;
    
    dst = createOpnd(typeManager->getTauType());
    appendInst(instFactory->makeTauHasExactType(dst, src, castType));
    
    insertHash(hashcode, src->getId(), castType->getId(), dst->getInst());
    return dst;
}

Opnd*
IRBuilder::genTauIsNonNull(Opnd *src) {
    U_32 hashcode = Op_TauCheckNull;
    Opnd* dst = lookupHash(hashcode, src->getId());
    if (dst) return dst;
    
    dst = createOpnd(typeManager->getTauType());
    appendInst(instFactory->makeTauIsNonNull(dst, src));
    
    insertHash(hashcode, src->getId(), dst->getInst());

    // We can also make the type init for the base object available here
    Type* baseType = src->getType();
    insertHash(Op_InitType, baseType->getId(), dst->getInst());

    return dst;
}


Opnd*
IRBuilder::genTauAnd(Opnd *src1, Opnd *src2) {
    if (src1->getId() > src2->getId()) {
        Opnd *tmp = src1;
        src1 = src2;
        src2 = tmp;
    }
    Opnd* dst = lookupHash(Op_TauAnd, src1, src2);
    if (dst) return dst;

    dst = createOpnd(typeManager->getTauType());
    Opnd* srcs[2] = { src1, src2 };
    appendInst(instFactory->makeTauAnd(dst, 2, srcs));
    
    insertHash(Op_TauAnd, src1->getId(), src2->getId(), dst->getInst());
    return dst;
}


Inst* IRBuilder::getLastGeneratedInst() {
    return (Inst*)currentLabel->getNode()->getLastInst();
}

} //namespace Jitrino 
