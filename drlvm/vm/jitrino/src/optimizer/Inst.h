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
 *
 */


#ifndef _INST_H_
#define _INST_H_

#include "MemoryManager.h"
#include "Stl.h"
#include "VMInterface.h"
#include "Dlink.h"
#include "Type.h"
#include "Opcode.h"
#include "Opnd.h"
#include "Log.h"
#include "ControlFlowGraph.h"

#include "open/types.h"

#include <assert.h>
#include <stdio.h>
#include <iostream>

namespace Jitrino {

class   OpndRenameTable;
class   IRBuilder;
class   IRManager;
class   InstFactory;
class   PiCondition;
class   DominatorTree;

//
// forward declarations of the different instruction formats
//
class   Inst;
class   LabelInst;
class   DispatchLabelInst;
class   CatchLabelInst;
class   MethodEntryInst;
class   MethodMarkerInst;
class   TypeInst;
class   VarAccessInst;
class   FieldAccessInst;
class   BranchInst;
class   SwitchInst;
class   ConstInst;
class   MethodInst;
class   TokenInst;
class   LinkingExcInst;
class   CallInst;
class   JitHelperCallInst;
class   VMHelperCallInst;
class   PhiInst;
class   MultiSrcInst;
class   MethodCallInst;
class   TauPiInst;

//
// visitor pattern for the different instruction formats
//
class InstFormatVisitor {
public:
    virtual ~InstFormatVisitor() {}
    virtual void accept(Inst*) = 0;
    virtual void accept(BranchInst*) = 0;
    virtual void accept(CallInst*) = 0;
    virtual void accept(CatchLabelInst*) = 0;
    virtual void accept(ConstInst*) = 0;
    virtual void accept(DispatchLabelInst*) = 0;
    virtual void accept(JitHelperCallInst*) = 0;
    virtual void accept(VMHelperCallInst*) = 0;
    virtual void accept(FieldAccessInst*) = 0;
    virtual void accept(LabelInst*) = 0;
    virtual void accept(MethodCallInst*) = 0;
    virtual void accept(MethodEntryInst*) = 0;
    virtual void accept(MethodInst*) = 0;
    virtual void accept(MethodMarkerInst*) = 0;
    virtual void accept(MultiSrcInst*) = 0;
    virtual void accept(PhiInst*) = 0;
    virtual void accept(TauPiInst*) = 0;
    virtual void accept(SwitchInst*) = 0;
    virtual void accept(TokenInst*) = 0;
    virtual void accept(TypeInst*) = 0;
    virtual void accept(VarAccessInst*) = 0;
};

//
// Base Instruction class.
//
#define MAX_INST_SRCS 2

class Inst : public CFGInst {
public:
    virtual ~Inst() {}

    Inst* getNextInst() const {return (Inst*)next();}
    Inst* getPrevInst() const {return (Inst*)prev();}

    bool isHeaderCriticalInst() const {return getOpcode() == Op_Catch || isLabel();}

    Type::Tag    getType() const {
        return operation.getType();
    }

    Opcode      getOpcode() const   {
        return operation.getOpcode();
    }

    Modifier    getModifier() const {
        return operation.getModifier();
    }

    Operation getOperation() const {return operation;}

    U_32 getId() const {return id;}

    PersistentInstructionId getPersistentInstructionId() const {return pid; }

    void   setPersistentInstructionId(PersistentInstructionId id) {pid = id; }

    Opnd*  getDst() const {return dst;}

    void setType(Type::Tag newType) {
        operation.setType(newType);
    }

    void setModifier(Modifier newMod) {
        operation.setModifier(newMod);
    }

    void setDst(Opnd* newDst) {
        if (dst && dst->isNull() == false && dst->isVarOpnd() == false && (dst->getInst() == this))
            dst->setInst(0);
        dst = newDst;
        if (dst && dst->isNull() == false && dst->isVarOpnd() == false)
            dst->setInst(this);
    }

    U_32  getNumSrcOperands() const    {return numSrcs;}

    Opnd*   getSrc(U_32 srcIndex) const {
        assert(srcIndex < numSrcs);
        if (srcIndex >= MAX_INST_SRCS)
            return getSrcExtended(srcIndex);
        return srcs[srcIndex];
    }

    void  setSrc(U_32 srcIndex, Opnd* src) {
        assert(srcIndex < numSrcs);
        if (srcIndex >= MAX_INST_SRCS)
            setSrcExtended(srcIndex, src);
        else
            srcs[srcIndex] = src;
    }

    virtual void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    //
    // for down-casting to a derived Inst class
    // we de-virtualize these so the C++ compiler can do CSE on isFoo() in most uses
    //
    BranchInst* asBranchInst() const {
        if (isBranch()) return (BranchInst*)this; else return NULL;
    }
    CallInst* asCallInst() const {
        if (isCall()) return (CallInst*)this; else return NULL;
    }
    CatchLabelInst* asCatchLabelInst() const {
        if (isCatchLabel()) return (CatchLabelInst*)this; else return NULL;
    }
    ConstInst* asConstInst() const {
        if (isConst()) return (ConstInst*)this; else return NULL;
    }
    DispatchLabelInst* asDispatchLabelInst() const {
        if (isDispatchLabel()) return (DispatchLabelInst*)this; else return NULL;
    }
    FieldAccessInst* asFieldAccessInst() const {
        if (isFieldAccess()) return (FieldAccessInst*)this; else return NULL;
    }
    JitHelperCallInst* asJitHelperCallInst() const {
        if (isJitHelperCallInst()) return (JitHelperCallInst*) this; else return NULL;
    }
    VMHelperCallInst* asVMHelperCallInst() const {
        if (isVMHelperCallInst()) return (VMHelperCallInst*) this; else return NULL;
    }
    LabelInst* asLabelInst() const {
        if (isLabel()) return (LabelInst*)this; else return NULL;
    }
    MethodCallInst* asMethodCallInst() const {
        if (isMethodCall()) return (MethodCallInst*)this; else return NULL;
    }
    MethodEntryInst* asMethodEntryInst() const {
        if (isMethodEntry()) return (MethodEntryInst*)this; else return NULL;
    }
    MethodInst* asMethodInst() const {
        if (isMethod()) return (MethodInst*)this; else return NULL;
    }
    MethodMarkerInst* asMethodMarkerInst() const {
        if (isMethodMarker()) return(MethodMarkerInst*)this; else return NULL;
    }
    MultiSrcInst* asMultiSrcInst() const {
        if (isMultiSrcInst()) return (MultiSrcInst*)this; else return NULL;
    }
    PhiInst* asPhiInst() const { 
        if (isPhi()) return (PhiInst*)this; else return NULL;
    }
    TauPiInst *asTauPiInst() const {
        if (isTauPi()) return (TauPiInst*)this; else return NULL;
    }
    SwitchInst* asSwitchInst() const {
        if (isSwitch()) return (SwitchInst *)this; else return NULL;
    }
    TokenInst* asTokenInst() const {
        if (isToken()) return (TokenInst*)this; else return NULL;
    }
    TypeInst* asTypeInst() const {
        if (isType()) return (TypeInst*) this; else return NULL; 
    }
    VarAccessInst* asVarAccessInst() const {
        if (isVarAccess()) return (VarAccessInst*) this; else return NULL;
    }

    void        print(::std::ostream&) const;
    bool        isSigned() const {
        return operation.isSigned();
    }

    virtual bool isBranch() const { return false; };
    virtual bool isCall() const { return false; };
    virtual bool isCatchLabel() const { return false; };
    virtual bool isConst() const { return false; };
    virtual bool isDispatchLabel() const { return false; };
    virtual bool isFieldAccess() const { return false; };
    virtual bool isJitHelperCallInst() const { return false; };
    virtual bool isVMHelperCallInst() const { return false; };
    virtual bool isMethodCall() const { return false; };
    virtual bool isMethodEntry() const { return false; };
    virtual bool isMethod() const { return false; };
    virtual bool isMethodMarker() const { return false; };
    virtual bool isMultiSrcInst() const { return false; };
    bool isPhi() const { return (getOpcode() == Op_Phi); };
    bool isTauPi() const { return (getOpcode() == Op_TauPi); };
    bool isSwitch() const { return (getOpcode() == Op_Switch); };
    virtual bool isToken() const { return false; };
    virtual bool isType() const { return false; };
    virtual bool isVarAccess() const { return false; };

    bool isThrow() const ;
    bool isReturn() const     {return getOpcode() == Op_Return;    }
    bool isLdVar() const      {return getOpcode() == Op_LdVar;     }
    bool isLdVarAddr() const  {return getOpcode() == Op_LdVarAddr; }
    bool isStVar() const      {return getOpcode() == Op_StVar;     }
    bool isJSR() const        {return getOpcode() == Op_JSR;       }
    bool isRet() const        {return getOpcode() == Op_Ret;       }

    bool isUnconditionalBranch() const {return getOpcode() == Op_Jump;}
    bool isConditionalBranch() const  {return getOpcode() == Op_Branch;}

    ComparisonModifier getComparisonModifier() const {
        return operation.getComparisonModifier();
    }
    void setComparisonModifier(ComparisonModifier newmod) {
        operation.setComparisonModifier(newmod);
    }
    SignedModifier getSignedModifier() const {
        return operation.getSignedModifier();
    }
    void setSignedModifier(SignedModifier newmod) {
        operation.setSignedModifier(newmod);
    }
    OverflowModifier getOverflowModifier() const {
        return operation.getOverflowModifier();
    }
    void setOverflowModifier(OverflowModifier newmod) {
        operation.setOverflowModifier(newmod);
    }
    ShiftMaskModifier getShiftMaskModifier() const {
        return operation.getShiftMaskModifier();
    }
    void setShiftMaskModifier(ShiftMaskModifier mod) {
        operation.setShiftMaskModifier(mod);
    }
    StrictModifier getStrictModifier() const {
        return operation.getStrictModifier();
    }
    void setStrictModifier(StrictModifier mod) {
        operation.setStrictModifier(mod);
    }
    DefArgModifier getDefArgModifier() const {
        return operation.getDefArgModifier();
    }
    void setDefArgModifier(DefArgModifier mod) {
        operation.setDefArgModifier(mod);
    }
    StoreModifier getStoreModifier() const {
        return operation.getStoreModifier();
    }
    void setStoreModifier(StoreModifier mod) {
        operation.setStoreModifier(mod);
    }
    ExceptionModifier getExceptionModifier() const {
        return operation.getExceptionModifier();
    }
    void setExceptionModifier(ExceptionModifier mod) {
        operation.setExceptionModifier(mod);
    }
    AutoCompressModifier getAutoCompressModifier() const {
        return operation.getAutoCompressModifier();
    }
    void setAutoCompressModifier(AutoCompressModifier mod) {
        operation.setAutoCompressModifier(mod);
    }
    SpeculativeModifier getSpeculativeModifier() const {
        return operation.getSpeculativeModifier();
    }
    void setSpeculativeModifier(SpeculativeModifier mod) {
        operation.setSpeculativeModifier(mod);
    }
    ThrowModifier getThrowModifier() const {
        return operation.getThrowModifier();
    }
    void setThrowModifier(ThrowModifier mod) {
        operation.setThrowModifier(mod);
    }
    NewModifier1 getNewModifier1() const {
        return operation.getNewModifier1();
    }
    void setNewModifier1(NewModifier1 mod) {
        operation.setNewModifier1(mod);
    }
    NewModifier2 getNewModifier2() const {
        return operation.getNewModifier2();
    }
    void setNewModifier2(NewModifier2 mod) {
        operation.setNewModifier2(mod);
    }


protected:
    //
    // Constructors
    //
    Inst(Opcode op, Modifier modifier, Type::Tag, Opnd* dst);
    Inst(Opcode op, Modifier modifier, Type::Tag, Opnd* dst, Opnd* src);
    Inst(Opcode op, Modifier modifier, Type::Tag, Opnd* dst, Opnd* src1, Opnd* src2);
    Inst(Opcode op, Modifier modifier, Type::Tag, Opnd* dst, Opnd* src1, Opnd* src2, Opnd *src3);
    Inst(Opcode op, Modifier modifier, Type::Tag, Opnd* dst, U_32 nSrcs);
    //
    // fields
    //
    Operation operation;
    U_32  numSrcs;
    Opnd*   srcs[MAX_INST_SRCS];
    Opnd*   dst;
    PersistentInstructionId pid;
    U_32  id;
    
    
    // called from CFG to detect BB->BB block edges
    virtual Edge::Kind getEdgeKind(const Edge* edge) const;
    
    virtual void removeRedundantBranch();

    //
    // protected accessor methods that deriving classes should override
    //
    virtual Opnd* getSrcExtended(U_32 srcIndex) const;
    virtual void  setSrcExtended(U_32 srcIndex, Opnd* src) {assert(0);}
    virtual void  handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;
    void printFormatString(::std::ostream&, const char*) const;
};

class LabelInst : public Inst {
public:
    U_32    getLabelId() const            {return labelId;}
    void      setLabelId(U_32 id_)    {labelId = id_;}
    bool      isLabel() const               {return true;}
    virtual bool isDispatchLabel() const    {return false;}
    virtual bool isCatchLabel() const       {return false;}
    void      setState(void *stat)     {state = stat;} 
    void *    getState()               {return state;}
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    virtual void printId(::std::ostream&) const;
protected:
    LabelInst(U_32 id)
             : Inst(Op_Label, Modifier(), Type::Void, OpndManager::getNullOpnd()),
               labelId(id), state(NULL) {}
    LabelInst(Opcode opc, U_32 id)
             : Inst(opc, Modifier(), Type::Void, OpndManager::getNullOpnd()),
               labelId(id), state(NULL) {}
    virtual void handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;
    U_32   labelId;
    
    void*    state;     
};

class DispatchLabelInst: public LabelInst {
public:
    bool    isDispatchLabel() const {return true;}
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    void printId(::std::ostream&) const;
protected:
    friend class InstFactory;
    DispatchLabelInst(U_32 labelId) : LabelInst(labelId) {}
    virtual void handlePrintEscape(::std::ostream&, char code) const;
};

class CatchLabelInst: public LabelInst {
public:
    bool      isCatchLabel() const     {return true; }
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    U_32    getOrder() const         {return order;}
    Type*     getExceptionType() const {return exception;}
    void printId(::std::ostream&) const;
protected:
    CatchLabelInst(U_32 id, U_32 ord, Type *except)
              : LabelInst(id), order(ord), exception(except) {}
    virtual void handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;
    U_32   order;
    Type     *exception;
};

class MethodEntryInst : public LabelInst {
public:
    MethodDesc* getMethodDesc() {return methodDesc;}
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool isMethodEntry() const { return true; }
protected:
    MethodEntryInst(U_32 id, MethodDesc* md)
        : LabelInst(Op_MethodEntry, id), methodDesc(md){}
    virtual void handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;
    MethodDesc* methodDesc;
};

class MethodMarkerInst : public Inst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool isMethodMarker() const { return true; }
    enum Kind {Entry, Exit};
    bool isMethodEntryMarker() { return kind == Entry; }
    bool isMethodExitMarker() { return kind == Exit; }
    MethodDesc *getMethodDesc() { return methodDesc; }

    void removeOpnd() {
        setSrc(0, OpndManager::getNullOpnd());
        if (numSrcs) numSrcs--;
    }

protected:
    virtual void handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;

    MethodMarkerInst(Kind k, MethodDesc* md) :
            Inst(k==Entry? Op_MethodEntry : Op_MethodEnd, Modifier(), Type::Void, 
            OpndManager::getNullOpnd()), kind(k), methodDesc(md), retOpnd(NULL) {}

    MethodMarkerInst(Kind k, MethodDesc* md, Opnd *resOpnd) :
            Inst(k==Entry? Op_MethodEntry : Op_MethodEnd, Modifier(), Type::Void, 
            OpndManager::getNullOpnd()), kind(k), methodDesc(md), retOpnd(resOpnd) {}

    MethodMarkerInst(Kind k, MethodDesc* md, Opnd *obj, Opnd *resOpnd) :
    Inst(k==Entry? Op_MethodEntry : Op_MethodEnd, Modifier(), Type::Void, OpndManager::getNullOpnd(),
            obj), kind(k), methodDesc(md), retOpnd(resOpnd) { 
        assert(obj && !obj->isNull()); 

    }
    Kind        kind;
    MethodDesc* methodDesc;
    Opnd* retOpnd;
};

//
// indicate exception information
//

class BranchInst : public Inst {
public:
    bool       isBranch() const        {return true;}
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}

    void       replaceTargetLabel(LabelInst* target) {targetLabel = target;}
    LabelInst* getTargetLabel() const      {return targetLabel;}
    void       swapTargets(LabelInst *target);
    Edge       *getTakenEdge(U_32 condition);
protected:
    virtual void handlePrintEscape(::std::ostream&, char code) const;
    virtual Edge::Kind getEdgeKind(const Edge* edge) const;
    virtual void updateControlTransferInst(Node* oldTarget, Node* newTarget); 
private:
    friend class InstFactory;
    BranchInst(Opcode op, LabelInst* target)
        : Inst(op, Modifier(), Type::Void, OpndManager::getNullOpnd()), targetLabel(target) {};
    BranchInst(Opcode op, Opnd* src, LabelInst* target)
        : Inst(op, Modifier(), Type::Boolean, OpndManager::getNullOpnd(), src), 
          targetLabel(target) {}
    BranchInst(Opcode op,
               ComparisonModifier mod,
               Type::Tag type,
               Opnd* src,
               LabelInst* target)
        : Inst(op, mod, type, OpndManager::getNullOpnd(), src), targetLabel(target) {}
    BranchInst(Opcode op,
               ComparisonModifier mod,
               Type::Tag type,
               Opnd* src1,
               Opnd* src2,
               LabelInst* target)
        : Inst(op, mod, type, OpndManager::getNullOpnd(), src1, src2), targetLabel(target) {}
    LabelInst*  targetLabel;
};

class SwitchInst : public Inst {
public:
    LabelInst* getTarget(U_32 i) {
        assert(i < numTargets);
        return targetInsts[i];
    }
    LabelInst** getTargets() { return targetInsts; }
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool isSwitch() const {return true;}
    LabelInst* getDefaultTarget() const {return defaultTargetInst;}
    U_32     getNumTargets() {return numTargets;}
    void       replaceTargetLabel(U_32 i, LabelInst* target) {
        assert(i < numTargets);
        targetInsts[i] = target;
    }
    void       replaceDefaultTargetLabel(LabelInst* target) { defaultTargetInst = target; }

protected:
    virtual void handlePrintEscape(::std::ostream&, char code) const;
    virtual Edge::Kind getEdgeKind(const Edge* edge) const;
    virtual void updateControlTransferInst(Node* oldTarget, Node* newTarget); 
private:
    friend class InstFactory;
    SwitchInst(Opnd* src, LabelInst** targets, U_32 nTargets, LabelInst* defTarget)
        : Inst(Op_Switch, Modifier(), Type::Void, OpndManager::getNullOpnd(), src),
          targetInsts(targets), defaultTargetInst(defTarget), numTargets(nTargets) {
    }
    LabelInst** targetInsts;
    LabelInst*  defaultTargetInst;
    U_32      numTargets;
};

class ConstInst : public Inst {
public:
    union ConstValue {
        void*    i;    // I  (can be NULL for ldnull)
        I_32    i4;   // I4
        int64    i8;   // I8
        float    s;    // Single
        double   d;    // Double
        struct {
            I_32 dword1;
            I_32 dword2;
        };
        ConstValue() {dword1=dword2=0;}
    };
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    virtual bool isConst() const {return true;}
    ConstValue    getValue()    {return value;}
protected:
    virtual void handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;
    ConstInst(Opnd* d, ConstValue cv)
        : Inst(Op_LdConstant, Modifier(), d->getType()->tag, d) { value = cv; }
    ConstInst(Opnd* d, I_32 i4)
        : Inst(Op_LdConstant, Modifier(), Type::Int32, d)  {value.i4 = i4;}
    ConstInst(Opnd* d, int64 i8)
        : Inst(Op_LdConstant, Modifier(), Type::Int64, d)  {value.i8 = i8;}
    ConstInst(Opnd* d, float fs)
        : Inst(Op_LdConstant, Modifier(), Type::Single, d) {value.s = fs;}
    ConstInst(Opnd* d, double fd)
        : Inst(Op_LdConstant, Modifier(), Type::Double, d)  {value.d = fd;}
    ConstInst(Opnd* d, void* ptr)
        : Inst(Op_LdConstant, Modifier(), d->getType()->tag, d)  {value.i = ptr;}
    ConstInst(Opnd* d)
        : Inst(Op_LdConstant, Modifier(), Type::NullObject, d) {value.i=NULL;}
    ConstValue value;
};

class TokenInst : public Inst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    U_32 getToken() const   {return token;}
    MethodDesc*    getEnclosingMethod()     {return enclosingMethod;}
    bool isToken() const                    {return true;}
protected:
    virtual void handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;
    TokenInst(Opcode opc, Modifier mod, Type::Tag type, Opnd* d, U_32 t, MethodDesc* encMethod)
        : Inst(opc, mod, type, d), token(t), enclosingMethod(encMethod) {}

    U_32      token;
    MethodDesc* enclosingMethod;
};

// for Throw_LinkingException
class LinkingExcInst : public Inst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    Class_Handle     getEnclosingClass()  {return enclosingClass;}
    U_32           getCPIndex() const   {return cp_index;}
    U_32           getOperation() const {return operation;}
private:
    friend class InstFactory;
    LinkingExcInst(Opcode opc, Modifier mod, Type::Tag type, Opnd* d,
                   Class_Handle encClass, U_32 cp_ndx, U_32 _operation)
        : Inst(opc, mod, type, d),
          enclosingClass(encClass), cp_index(cp_ndx), operation(_operation) {}

    Class_Handle     enclosingClass;
    U_32           cp_index;
    U_32           operation;
};

// for ldvar, ldvara & stvar instructions
class VarAccessInst : public Inst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool     isVarAccess() const {return true;}
    VarOpnd* getVar() {
        if (isStVar())
            return (dst->isVarOpnd()) ? (VarOpnd*)dst : NULL;
        else
            return (getSrc(0)->isVarOpnd()) ? (VarOpnd*)getSrc(0) : NULL;
    }
    VarOpnd* getBaseVar() {
        if (isStVar()) {
            if (dst->isVarOpnd())
                return dst->asVarOpnd();
            else if (dst->isSsaVarOpnd())
                return (dst->asSsaVarOpnd()->getVar());
            else
                return 0;
        } else {
            Opnd *src0 = getSrc(0);
            if (src0->isVarOpnd())
                return src0->asVarOpnd();
            else if (src0->isSsaVarOpnd())
                return (src0->asSsaVarOpnd()->getVar());
            else
                return 0;
        }
    }
    VarAccessInst*  getNextVarAccessInst()    {return nextVarAccessInst;}
protected:
    virtual void     handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;
    // ldvar & ldvara:
    VarAccessInst(Opcode op, Type::Tag type, Opnd* dst, VarOpnd* var)
                 : Inst(op, Modifier(), type, dst, var) {
        nextVarAccessInst = var->getVarAccessInsts();
        var->addVarAccessInst(this);
    }
    // stvar:
    VarAccessInst(Opcode op, Type::Tag type, VarOpnd* var, Opnd* src)
                 : Inst(op, Modifier(), type, var, src){
        nextVarAccessInst = var->getVarAccessInsts();
        var->addVarAccessInst(this);
    }
    // SsaVar forms
    // ldvar & ldvara:
    VarAccessInst(Opcode op, Type::Tag type, Opnd* dst, SsaVarOpnd* var)
                 : Inst(op, Modifier(), type, dst, var) {
        VarOpnd *orgvar = var->getVar();
        nextVarAccessInst = orgvar->getVarAccessInsts();
        orgvar->addVarAccessInst(this);
    }
    // stvar:
    VarAccessInst(Opcode op, Type::Tag type, SsaVarOpnd* var, Opnd* src)
        : Inst(op, Modifier(), type, var, src){
        VarOpnd *orgvar = var->getVar();
        nextVarAccessInst = orgvar->getVarAccessInsts();
        orgvar->addVarAccessInst(this);
    }
    VarAccessInst*   nextVarAccessInst;
};

class MultiSrcInst : public Inst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool isMultiSrcInst() const { return true; }
protected:
    MultiSrcInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst)
        : Inst(op, mod, ty, dst), extendedSrcs(0), extendedSrcSpace(0) {}
    MultiSrcInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst, Opnd* src)
        : Inst(op, mod, ty, dst, src), extendedSrcs(0), extendedSrcSpace(0) {}
    MultiSrcInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst, Opnd* src1, Opnd* src2)
        : Inst(op, mod, ty, dst, src1, src2), extendedSrcs(0), extendedSrcSpace(0) {};
    MultiSrcInst(Opcode op,
                 Modifier mod,
                 Type::Tag ty,
                 Opnd* dst,
                 U_32 nSrcs_,
                 Opnd** srcs_)
        : Inst(op, mod, ty, dst, nSrcs_),
          extendedSrcs(srcs_), extendedSrcSpace(nSrcs_)
    {
        switch (nSrcs_) {
        default:    
        case 2:     srcs[1] = srcs_[1];
        case 1:     srcs[0] = srcs_[0];
        case 0:     break;
        }
        for (U_32 i=2; i < nSrcs_; i++) {
            srcs_[i-MAX_INST_SRCS] = srcs_[i];
            srcs_[i] = 0;
        }
    }
private:
    friend class InstFactory;
    Opnd* getSrcExtended(U_32 srcIndex) const {
        assert(srcIndex < (extendedSrcSpace + MAX_INST_SRCS));
        return extendedSrcs[srcIndex - MAX_INST_SRCS];
    }
    void  setSrcExtended(U_32 srcIndex, Opnd* src) {
        assert(srcIndex < (extendedSrcSpace + MAX_INST_SRCS));
        extendedSrcs[srcIndex - MAX_INST_SRCS] = src;
    }
    Opnd**    extendedSrcs;
    U_32    extendedSrcSpace;

    void initSrcs(U_32 nSrcs_, Opnd**srcs_) {
        switch (nSrcs_) {
        default:    
        case 2:     srcs[1] = srcs_[1];
        case 1:     srcs[0] = srcs_[0];
        case 0:     break;
        }
        for (U_32 i=2; i < nSrcs_; i++) {
            srcs_[i-MAX_INST_SRCS] = srcs_[i];
            extendedSrcs = srcs_ + MAX_INST_SRCS;
        }
    }
public:
    void  setNumSrcs(U_32 nSrcs) {
        assert(nSrcs <= numSrcs);
        numSrcs = nSrcs;
    }
};

class TypeInst : public MultiSrcInst {
public:
    Type*    getTypeInfo() {return type;}
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool isType() const {return true; }
protected:
    TypeInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst, Type* td)
        : MultiSrcInst(op, mod, ty, dst), type(td) {}
    TypeInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst,
             Opnd* src, Type* td)
        : MultiSrcInst(op, mod, ty, dst, src), type(td) {}
    TypeInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst,
             Opnd* src1, Opnd* src2, Type* td)
        : MultiSrcInst(op, mod, ty, dst, src1, src2), type(td) {}
    TypeInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst,
             U_32 nArgs, Opnd** args_, Type* td)
        : MultiSrcInst(op, mod, ty, dst, nArgs, args_), type(td){}
    virtual void handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;
    Type*    type;
};

// for LoadField, LoadFieldAddr, LoadStatic, LoadStaticAddr, StoreStatic, StoreField
class FieldAccessInst : public MultiSrcInst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    FieldDesc* getFieldDesc()    {return fieldDesc;}
    bool isFieldAccess() const {return true;}
protected:
    virtual void handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;
    FieldAccessInst(Opcode op,
                    Modifier mod,
                    Type::Tag type,
                    Opnd* dst,
                    FieldDesc* fd)
        : MultiSrcInst(op, mod, type, dst), fieldDesc(fd) {}
    FieldAccessInst(Opcode op,
                    Modifier mod,
                    Type::Tag type,
                    Opnd* dst,
                    Opnd* src,
                    FieldDesc* fd)
        : MultiSrcInst(op, mod, type, dst, src), fieldDesc(fd) {}
    FieldAccessInst(Opcode op,
                    Modifier mod,
                    Type::Tag type,
                    Opnd* dst,
                    Opnd* src1,
                    Opnd* src2,
                    FieldDesc* fd)
        : MultiSrcInst(op, mod, type, dst, src1, src2), fieldDesc(fd) {}
    FieldAccessInst(Opcode op,
                    Modifier mod,
                    Type::Tag type,
                    Opnd* dst,
                    U_32 numSrcs,
                    Opnd** srcs,
                    FieldDesc* fd)
        : MultiSrcInst(op, mod, type, dst, numSrcs, srcs), fieldDesc(fd) {}
    FieldDesc* fieldDesc;
};

class MethodInst : public MultiSrcInst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool isMethod() const        {return true;}
    MethodDesc* getMethodDesc()    {return methodDesc;}
    void        setMethodDesc(MethodDesc* desc) {methodDesc = desc;}
protected:
    virtual void handlePrintEscape(::std::ostream&, char code) const;
    friend class InstFactory;
    MethodInst(Opcode op, Modifier mod, Type::Tag type, Opnd* dst, MethodDesc* md)
        : MultiSrcInst(op, mod, type, dst), methodDesc(md) {}
    MethodInst(Opcode op,
               Modifier mod,
               Type::Tag type,
               Opnd* dst,
               U_32 nArgs,
               Opnd ** srcs,
               MethodDesc* md)
        : MultiSrcInst(op, mod, type, dst, nArgs, srcs), methodDesc(md) {}
    MethodInst(Opcode op,
               Modifier mod,
               Type::Tag type,
               Opnd* dst,
               Opnd* base,
               MethodDesc* md)
        : MultiSrcInst(op, mod, type, dst, base), methodDesc(md) {}
    MethodInst(Opcode op,
               Modifier mod,
               Type::Tag type,
               Opnd* dst,
               Opnd* base,
               Opnd* extra,
               MethodDesc* md)
        : MultiSrcInst(op, mod, type, dst, base, extra), methodDesc(md) {}
private:

    MethodDesc* methodDesc;
};

class MethodCallInst : public MethodInst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool isMethodCall() const {return true;}

private:
    friend class InstFactory;
    MethodCallInst(Opcode op, Modifier mod,
                   Type::Tag type,
                   Opnd* dst,
                   U_32 nArgs,
                   Opnd** args_,
                   MethodDesc* md,
                   MemoryManager& mem_mgr)
        : MethodInst(op, mod, type, dst, nArgs, args_, md){}
};

// for call instructions
class CallInst : public Inst {
public:
    bool isCall() const {return true; }
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    Opnd*   getFunPtr()             {return srcs[0];}
    U_32  getNumArgs() const      {return getNumSrcOperands()-1;}
    Opnd*   getArg(U_32 argIndex) {return getSrc(argIndex+1);}
    Opnd**  getArgs()               {args[0] = srcs[1]; return args;}
private:
    friend class InstFactory;

    CallInst(Opcode op, Modifier mod,
             Type::Tag type,
             Opnd* dst,
             Opnd* ptr,
             U_32 nArgs,
             Opnd** args_,
             MemoryManager& mem_mgr)
        : Inst(op, mod, type, dst, nArgs+1)
    {
        args = args_;
        switch (nArgs) {
        default:    
        case 1:     srcs[1] = args_[0];
        case 0:     srcs[0] = ptr;
        }
    }
    Opnd* getSrcExtended(U_32 srcIndex) const {
        assert(srcIndex != 1);
        return args[srcIndex - 1];
    }
    void  setSrcExtended(U_32 srcIndex, Opnd* src) {
        assert(srcIndex != 1);
        args[srcIndex - 1] = src;
    }
    Opnd**    args;
};

// JIT helper calls
class JitHelperCallInst : public Inst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool isJitHelperCallInst() const { return true; }
    JitHelperCallId getJitHelperId() const {return jitHelperId;}
private:
    virtual void handlePrintEscape(::std::ostream&, char code) const;
    friend class InstFactory;
    JitHelperCallInst(Opcode op,
                      Modifier mod,
                      Type::Tag type,
                      Opnd* dst,
                      U_32 nArgs,
                      Opnd** args_,
                      JitHelperCallId id) : Inst(op, mod, type, dst, nArgs),
                                            jitHelperId(id) {
        args = NULL;
        switch (nArgs) {
        default:    args = args_ + MAX_INST_SRCS;
        case 2:     srcs[1] = args_[1];
        case 1:     srcs[0] = args_[0];
        case 0:     break;
        }
    }
    Opnd* getSrcExtended(U_32 srcIndex) const {
        return args[srcIndex - MAX_INST_SRCS];
    }
    void  setSrcExtended(U_32 srcIndex, Opnd* src) {
        args[srcIndex - MAX_INST_SRCS] = src;
    }
    Opnd**    args;
    JitHelperCallId jitHelperId;
};

// VM helper calls
class VMHelperCallInst : public Inst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool isVMHelperCallInst() const { return true; }
    VM_RT_SUPPORT getVMHelperId() const {return vmHelperId;}
    bool isThrowLazy() const {return vmHelperId == VM_RT_THROW_LAZY;}
private:
    virtual void handlePrintEscape(::std::ostream&, char code) const;
    friend class InstFactory;
    VMHelperCallInst(Opcode op,
                     Modifier mod,
                     Type::Tag type,
                     Opnd* dst,
                     U_32 nArgs,
                     Opnd** args_,
                     VM_RT_SUPPORT id) 
                     : Inst(op, mod, type, dst, nArgs), vmHelperId(id)
    {
        args = args_;
        switch (nArgs) {
        default:
        case 2:     srcs[1] = args_[1];
        case 1:     srcs[0] = args_[0];
        case 0:     break;
        }
    }
    Opnd* getSrcExtended(U_32 srcIndex) const {
        return args[srcIndex];
    }
    void  setSrcExtended(U_32 srcIndex, Opnd* src) {
        args[srcIndex] = src;
    }
    Opnd**    args;
    VM_RT_SUPPORT vmHelperId;
};


// phi instructions
class PhiInst : public MultiSrcInst {
public:
    void visit(InstFormatVisitor& visitor)  {visitor.accept(this);}
    bool isMultiSrcInst() const { return false; }
    bool isPhi() const { return true; }
private:
    friend class InstFactory;
    PhiInst(Type::Tag type, Opnd* dst, U_32 nArgs, Opnd** args_ )
        : MultiSrcInst(Op_Phi, Modifier(), type, dst, nArgs, args_)
    {
    }
    bool isHeaderCriticalInst() const {return true;}
};

// pi instructions
class TauPiInst : public Inst {
public:
    void visit(InstFormatVisitor& visitor) { visitor.accept(this);}
    bool isTauPi() const { return true; }
    const PiCondition *getCond() const { return cond; };
protected:
    virtual void handlePrintEscape(::std::ostream&, char code) const;
private:
    friend class InstFactory;
    friend class InsertPi; // needs to update the cond below;
    TauPiInst(Type::Tag type, Opnd* dst, Opnd* src, Opnd *tau, PiCondition *cond0)
        : Inst(Op_TauPi, Modifier(), type, dst, src, tau),
          cond(cond0)
    {
    }
    PiCondition *cond;
};

class InstFactory {
public:
    MemoryManager& getMemManager() {return memManager;}
    Inst*    clone(Inst* inst, OpndManager& opndManager, OpndRenameTable *table);
    // Numeric compute
    Inst*    makeAdd(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeSub(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeMul(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeTauDiv(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2, Opnd *tauCheckedOpnds);
    Inst*    makeTauRem(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2, Opnd *tauCheckedOpnds);
    Inst*    makeNeg(Opnd* dst, Opnd* src);
    Inst*    makeMulHi(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeMin(Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeMax(Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeAbs(Opnd* dst, Opnd* src1);
    // Bitwise
    Inst*    makeAnd(Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeOr(Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeXor(Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeNot(Opnd* dst, Opnd* src);
    // Selection
    Inst*    makeSelect(Opnd* dst, Opnd* src1, Opnd* src2, Opnd* src3);
    // conversion
    Inst*    makeConv(Modifier mod, Type::Tag toType, Opnd* dst, Opnd* src);
    Inst*    makeConvZE(Modifier mod, Type::Tag toType, Opnd* dst, Opnd* src);
    Inst*    makeConvUnmanaged(Modifier mod, Type::Tag toType, Opnd* dst, Opnd* src);
    // shifts
    Inst*    makeShladd(Opnd* dst, Opnd* value, Opnd* shiftAmount, Opnd *addTo); // shiftAmount must be constant
    Inst*    makeShl(Modifier mod, Opnd* dst, Opnd* value, Opnd* shiftAmount);
    Inst*    makeShr(Modifier mod1, Opnd* dst, Opnd* value, Opnd* shiftAmount);
    // comparison
    Inst*    makeCmp(ComparisonModifier mod, Type::Tag type, Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeCmp3(ComparisonModifier mod, Type::Tag type, Opnd* dst, Opnd* src1, Opnd* src2);
    // Control flow
    Inst*    makeBranch(ComparisonModifier mod, Type::Tag, Opnd* src1, Opnd* src2, LabelInst* labelInst);
    Inst*    makeBranch(ComparisonModifier mod, Type::Tag, Opnd* src, LabelInst* labelInst);
    Inst*    makeJump(LabelInst* labelInst);
    Inst*    makeSwitch(Opnd* src, U_32 nLabels, LabelInst** labelInsts, LabelInst* defaultLabel);
    Inst*    makeDirectCall(Opnd* dst, 
                            Opnd* tauNullChecked, Opnd* tauTypesChecked,
                            U_32 numArgs, Opnd** args, 
                            MethodDesc*);
    Inst*    makeTauVirtualCall(Opnd* dst, 
                                Opnd* tauNullChecked, Opnd *tauTypesChecked,
                                U_32 numArgs, Opnd** args, 
                                MethodDesc*);
    Inst*    makeIndirectCall(Opnd* dst, Opnd* funPtr,
                              Opnd* tauNullCheckedFirstArg, Opnd *tauTypesChecked, 
                              U_32 numArgs, Opnd** args);
    Inst*    makeIndirectMemoryCall(Opnd* dst, Opnd* funPtr, 
                                    Opnd *tauNullCheckedFirstArg, 
                                    Opnd *tauTypesChecked,
                                    U_32 numArgs, Opnd** args);
    Inst*    makeJitHelperCall(Opnd* dst, JitHelperCallId id, 
                               Opnd* tauNullChecked, Opnd* tauTypesChecked, 
                               U_32 numArgs, Opnd** args);
    Inst*    makeVMHelperCall(Opnd* dst, VM_RT_SUPPORT id, U_32 numArgs,
                               Opnd** args);
    
    Inst*    makeIdentHC(Opnd* dst, Opnd* src);

    Inst*    makeReturn(Opnd* src);
    Inst*    makeReturn();    // void return type
    Inst*    makeCatch(Opnd* dst);
    Inst*    makeCatchLabel(U_32 labelId, U_32 exceptionOrder, Type* exceptionType);
    CatchLabelInst*    makeCatchLabel(U_32 exceptionOrder, Type* exceptionType);
    Inst*    makeThrow(ThrowModifier mod, Opnd* exceptionObj);
    Inst*    makePseudoThrow();
    Inst*    makeThrowSystemException(CompilationInterface::SystemExceptionId exceptionId);
    Inst*    makeThrowLinkingException(Class_Handle encClass, U_32 CPIndex, U_32 operation);
    Inst*    makeLeave(LabelInst* labelInst);
    Inst*    makeJSR(LabelInst* labelInst);
    Inst*    makeRet(Opnd *src);       // JSR-RET pair
    Inst*    makeSaveRet(Opnd *dst);   // JSR-RET pair
    // load, store, & mov
    Inst*    makeCopy(Opnd* dst, Opnd* src);
    Inst*    makeDefArg(Modifier, Opnd* arg);
    Inst*    makeLdConst(Opnd* dst, I_32 val);
    Inst*    makeLdConst(Opnd* dst, int64 val);
    Inst*    makeLdConst(Opnd* dst, float val);
    Inst*    makeLdConst(Opnd* dst, double val);
    Inst*    makeLdConst(Opnd* dst, ConstInst::ConstValue val);
    Inst*    makeLdNull(Opnd* dst);
    Inst*    makeLdRef(Modifier mod, Opnd* dst, MethodDesc* enclosingMethod, U_32 token);
    Inst*    makeLdVar(Opnd* dst, VarOpnd* var);
    Inst*    makeLdVar(Opnd* dst, SsaVarOpnd* var);
    Inst*    makeLdVarAddr(Opnd* dst, VarOpnd* var);
    Inst*    makeTauLdInd(Modifier, Type::Tag type, Opnd* dst, Opnd* ptr, 
                          Opnd *tauNonNullBase, Opnd *tauAddressInRange);
    Inst*    makeTauLdField(Modifier, Type* type, Opnd* dst, Opnd* base,
                            Opnd *tauNonNullBase, Opnd *tauObjectTypeChecked, 
                            FieldDesc*);
    Inst*    makeLdStatic(Modifier, Type* type, Opnd* dst, FieldDesc*);
    Inst*    makeTauLdElem(Modifier, Type* type, Opnd* dst, Opnd* array, Opnd* index,
                           Opnd *tauNonNullBase, Opnd *tauAddressInRange);
    Inst*    makeLdFieldAddr(Opnd* dst, Opnd* base, FieldDesc*);
    Inst*    makeLdStaticAddr(Opnd* dst, FieldDesc*);
    Inst*    makeLdElemAddr(Type* type, Opnd* dst, Opnd* array, Opnd* index);
    Inst*    makeTauLdVTableAddr(Opnd* dst, Opnd* base, Opnd *tauBaseNonNull);
    Inst*    makeTauLdIntfcVTableAddr(Opnd* dst, Opnd* base, Type* vtableType);
    Inst*    makeTauLdVirtFunAddr(Opnd* dst, Opnd* vtable, 
                                  Opnd *tauVtableHasDesc,
                                  MethodDesc*);
    Inst*    makeTauLdVirtFunAddrSlot(Opnd* dst, Opnd* vtable,
                                      Opnd *tauVtableHasDesc, MethodDesc*);
    Inst*    makeLdFunAddr(Opnd* dst, MethodDesc*);
    Inst*    makeLdFunAddrSlot(Opnd* dst, MethodDesc*);
    Inst*    makeGetVTableAddr(Opnd* dst, ObjectType *type);
    Inst*    makeGetClassObj(Opnd* dst, ObjectType *type);
    Inst*    makeTauArrayLen(Opnd* dst, Type::Tag type, Opnd* base, Opnd *tauBaseNonNull,
                             Opnd *tauBaseIsArray);
    Inst*    makeLdArrayBaseAddr(Type* type, Opnd* dst, Opnd* array);
    Inst*    makeAddScaledIndex(Opnd* dst, Opnd* base, Opnd* index);
    Inst*    makeStVar(VarOpnd* var, Opnd* src);
    Inst*    makeStVar(SsaVarOpnd* var, Opnd* src);
    Inst*    makeTauStInd(Modifier, Type::Tag, Opnd* src, Opnd* ptr, Opnd *tauNonNullBase,
                          Opnd *tauAddressInRange, Opnd *tauElemTypeChecked);
    Inst*    makeTauStField(Modifier, Type::Tag type, Opnd* src, Opnd* base,
                            Opnd *tauNonNullBase, Opnd *tauObjectHasField,
                            Opnd *tauFieldTypeChecked,
                            FieldDesc*);
    Inst*    makeTauStElem(Modifier, Type::Tag, Opnd* src, Opnd* array, Opnd* index,
                           Opnd *tauNonNullBase, Opnd *tauAddressInRange, 
                           Opnd *tauElemTypeChecked);
    Inst*    makeTauStStatic(Modifier, Type::Tag type, Opnd* src, 
                             Opnd *tauFieldTypeChecked, FieldDesc*);
    Inst*    makeTauStRef(Modifier, Type::Tag, Opnd* src, Opnd* pointer, Opnd* objectbase,
                       Opnd *tauNonNullBase, Opnd *tauAddressInRange, Opnd *tauElemTypeChecked);
    // checks
    Inst*    makeTauCheckBounds(Opnd *dst, Opnd* arrayLen, Opnd* index);
    Inst*    makeTauCheckLowerBound(Opnd *dst, Opnd* lb, Opnd* idx); // tests (lb <= idx);
    Inst*    makeTauCheckUpperBound(Opnd *dst, Opnd* idx, Opnd* ub); // tests (idx < ub);
    Inst*    makeTauCheckNull(Opnd* dst, Opnd* base);
    Inst*    makeTauCheckZero(Opnd* dst, Opnd* src);
    Inst*    makeTauCheckDivOpnds(Opnd* dst, Opnd* src1, Opnd* src2);
    Inst*    makeTauCheckElemType(Opnd* dst, Opnd* array, Opnd* src, Opnd *tauNullChecked,
                                  Opnd *tauIsArray);
    Inst*    makeTauCheckFinite(Opnd* dst, Opnd* src);
    // alloc
    Inst*    makeNewObj(Opnd* dst, Type* type);
    Inst*    makeNewArray(Opnd* dst, Opnd* numElems, Type* elemType);
    Inst*    makeNewMultiArray(Opnd* dst, U_32 dimensions, Opnd** numElems, Type* elemType);
    // sync
    Inst*    makeTauMonitorEnter(Opnd* src, Opnd *tauSrcNonNull);
    Inst*    makeTauMonitorExit(Opnd* src, Opnd *tauSrcNonNull);
    Inst*    makeTypeMonitorEnter(Type *type);
    Inst*    makeTypeMonitorExit(Type *type);
    // lowered parts of monitor enter/exit
    Inst*    makeLdLockAddr(Opnd *dst, Opnd *obj);   // result is ref:int16
    Inst*    makeIncRecCount(Opnd *obj, Opnd *oldValue);
    Inst*    makeTauBalancedMonitorEnter(Opnd* dst, Opnd *src, Opnd *lockAddr,
                                         Opnd *tauSrcNonNull); // result is I_32
    Inst*    makeBalancedMonitorExit(Opnd* src, Opnd *lockAddr, Opnd *enterDst);
    Inst*    makeTauOptimisticBalancedMonitorEnter(Opnd* dst, Opnd *src, Opnd *lockAddr,
                                                   Opnd *tauSrcNonNull); // result is I_32
    Inst*    makeOptimisticBalancedMonitorExit(Opnd* src, Opnd *lockAddr, Opnd *enterDst);
    Inst*    makeMonitorEnterFence(Opnd* src);  // elided MonitorEnter, just enforce memory model
    Inst*    makeMonitorExitFence(Opnd* src);   // elided MonitorExit, just enforce memory model
    // type checking
    Inst*    makeTauStaticCast(Opnd* dst, Opnd* src, Opnd *tauCastChecked, 
                               Type* type);
    Inst*    makeTauCast(Opnd* dst, Opnd* src, Opnd *tauNullChecked, Type* type);
    Inst*    makeTauAsType(Opnd* dst, Opnd* src, Opnd *tauNullChecked, Type* type);
    Inst*    makeTauInstanceOf(Opnd* dst, Opnd* src, Opnd *tauCheckedNull, Type* type);
    Inst*    makeInitType(Type* type);
    // labels
    LabelInst*  makeLabel();
    // method entry/exit
    LabelInst*    makeMethodEntryLabel(MethodDesc* methodDesc);
    Inst*    makeMethodMarker(MethodMarkerInst::Kind kind, MethodDesc* methodDesc, 
            Opnd *obj, Opnd *retOpnd);
    Inst*    makeMethodMarker(MethodMarkerInst::Kind kind, MethodDesc* methodDesc, Opnd *retOpnd);
    Inst*    makeMethodMarker(MethodMarkerInst::Kind kind, MethodDesc* methodDesc);


    // SSA
    Inst*    makePhi(Opnd* dst, U_32 numOpnds, Opnd** opnds); // array is copied
    Inst*    makeTauPi(Opnd* dst, Opnd* src, Opnd *tau, PiCondition *cond);

    // profile counter increment
    Inst*    makeIncCounter(U_32 val);
    Inst*    makePrefetch(Opnd* addr); // prefetch

    // compressed references
    Inst*    makeUncompressRef(Opnd* dst, Opnd* compref);
    Inst*    makeCompressRef(Opnd* dst, Opnd* uncompref);
    Inst*    makeLdFieldOffset(Opnd* dst, FieldDesc *fieldDesc);
    Inst*    makeLdFieldOffsetPlusHeapbase(Opnd* dst, FieldDesc *fieldDesc);
    Inst*    makeLdArrayBaseOffset(Opnd* dst, Type *elemType);
    Inst*    makeLdArrayBaseOffsetPlusHeapbase(Opnd* dst, Type *elemType);
    Inst*    makeLdArrayLenOffset(Opnd* dst, Type *elemType);
    Inst*    makeLdArrayLenOffsetPlusHeapbase(Opnd* dst, Type *elemType);
    Inst*    makeAddOffset(Opnd* dst, Opnd* ref, Opnd* offset);
    Inst*    makeAddOffsetPlusHeapbase(Opnd* dst, Opnd* ref, Opnd* offset);

    // new tau methods
    Inst*    makeTauPoint(Opnd *dst);
    Inst*    makeTauEdge(Opnd *dst);
    Inst*    makeTauAnd(Opnd *dst, U_32 numOpnds, Opnd** opnds); // array is copied
    Inst*    makeTauUnsafe(Opnd *dst);
    Inst*    makeTauSafe(Opnd *dst);
    Inst*    makeTauCheckCast(Opnd *taudst, Opnd* src, Opnd* tauCheckedNull, Type* type);
    Inst*    makeTauHasType(Opnd *taudst, Opnd* src, Type* type);
    Inst*    makeTauHasExactType(Opnd *taudst, Opnd* src, Type* type);
    Inst*    makeTauIsNonNull(Opnd *taudst, Opnd* src);
    
    //
    //
    //
    U_32   createLabelNumber()     {return maxNumLabels++; }
    U_32   getMaxNumLabels()       {return maxNumLabels;   }
    U_32   getNumInsts()           {return numInsts;       }
    
private:
    U_32   maxNumLabels;        // number of labels generated
    U_32   numInsts;            // number of instructions generated
    MemoryManager& memManager;

    //
    // Private constructor
    //
    // We only allow IRManager to create an InstFactory.
    //
    friend class IRManager;
    InstFactory(MemoryManager& mm, MethodDesc & md);

    //
    // private helpers for making different types of instructions
    //
    Opnd**  copyOpnds(Opnd** srcs, U_32 numSrcs);
    Opnd**  copyOpnds(Opnd* src1, Opnd** srcs, U_32 numSrcs);
    Opnd**  copyOpnds(Opnd* src1, Opnd* src2, Opnd** srcs, U_32 numSrcs);

    //
    // methods for copying instructions
    //
    friend class CloneVisitor;
    Inst*               makeClone(Inst*, OpndManager&, OpndRenameTable&);
    LabelInst*          makeClone(LabelInst*, OpndManager&, OpndRenameTable&);
    DispatchLabelInst*  makeClone(DispatchLabelInst*, OpndManager&, OpndRenameTable&);
    CatchLabelInst*     makeClone(CatchLabelInst*, OpndManager&, OpndRenameTable&);
    MethodEntryInst*    makeClone(MethodEntryInst*, OpndManager&, OpndRenameTable&);
    MethodMarkerInst*   makeClone(MethodMarkerInst*, OpndManager&, OpndRenameTable&);
    BranchInst*         makeClone(BranchInst*, OpndManager&, OpndRenameTable&);
    SwitchInst*         makeClone(SwitchInst*, OpndManager&, OpndRenameTable&);
    ConstInst*          makeClone(ConstInst*, OpndManager&, OpndRenameTable&);
    TokenInst*          makeClone(TokenInst*, OpndManager&, OpndRenameTable&);
    LinkingExcInst*     makeClone(LinkingExcInst*, OpndManager&, OpndRenameTable&);
    VarAccessInst*      makeClone(VarAccessInst*, OpndManager&, OpndRenameTable&);
    TypeInst*           makeClone(TypeInst*, OpndManager&, OpndRenameTable&);
    FieldAccessInst*    makeClone(FieldAccessInst*, OpndManager&, OpndRenameTable&);
    MethodInst*         makeClone(MethodInst*, OpndManager&, OpndRenameTable&);
    MethodCallInst*     makeClone(MethodCallInst*, OpndManager&, OpndRenameTable&);
    CallInst*           makeClone(CallInst*, OpndManager&, OpndRenameTable&);
    JitHelperCallInst*  makeClone(JitHelperCallInst*, OpndManager&, OpndRenameTable&);
    VMHelperCallInst*   makeClone(VMHelperCallInst*, OpndManager&, OpndRenameTable&);
    PhiInst*            makeClone(PhiInst*, OpndManager&, OpndRenameTable&);
    MultiSrcInst*       makeClone(MultiSrcInst*, OpndManager&, OpndRenameTable&);
public:
    Inst* makeInst(Opcode op, Modifier mod, Type::Tag, Opnd* dst);
    Inst* makeInst(Opcode op, Modifier mod, Type::Tag, Opnd* dst, Opnd* src);
    Inst* makeInst(Opcode op,
                   Modifier mod,
                   Type::Tag,
                   Opnd* dst,
                   Opnd* src1,
                   Opnd* src2);
    // add a new source at end of sources
    void appendSrc(MultiSrcInst*, Opnd *newSrc);

private:
    // makes a copy of a LabelInst
    LabelInst*          makeLabelInst(U_32 labelId);
    LabelInst*          makeLabelInst(Opcode opc, U_32 labelId);
    DispatchLabelInst*  makeDispatchLabelInst(U_32 labelId);
    CatchLabelInst*     makeCatchLabelInst(U_32 lableId,
                                       U_32 ord,
                                       Type *exceptionType);
    MethodEntryInst*    makeMethodEntryInst(U_32 labelId, MethodDesc*) ;
    MethodMarkerInst*   makeMethodMarkerInst(MethodMarkerInst::Kind, MethodDesc*, 
            Opnd *obj, Opnd *retOpnd);
    MethodMarkerInst*   makeMethodMarkerInst(MethodMarkerInst::Kind, MethodDesc*, Opnd *retOpnd);
    MethodMarkerInst*   makeMethodMarkerInst(MethodMarkerInst::Kind, MethodDesc*);

    BranchInst* makeBranchInst(Opcode, LabelInst* target);
    BranchInst* makeBranchInst(Opcode, Opnd *src, LabelInst* target);
    BranchInst* makeBranchInst(Opcode, ComparisonModifier mod, 
                               Type::Tag, Opnd* src, LabelInst* target);
    BranchInst* makeBranchInst(Opcode,
                               ComparisonModifier mod,
                               Type::Tag,
                               Opnd* src1,
                               Opnd* src2,
                               LabelInst* target);
    SwitchInst* makeSwitchInst(Opnd* src,
                               LabelInst** targets,
                               U_32 nTargets,
                               LabelInst* defTarget);
    ConstInst* makeConstInst(Opnd* dst, I_32 i4);
    ConstInst* makeConstInst(Opnd* dst, int64 i8) ;
    ConstInst* makeConstInst(Opnd* dst, float fs);
    ConstInst* makeConstInst(Opnd* dst, double fd) ;
    ConstInst* makeConstInst(Opnd* dst, ConstInst::ConstValue cv);
    ConstInst* makeConstInst(Opnd* dst);
    //
    // fix parameter names!
    //
    TokenInst* makeTokenInst(Opcode opc, Modifier mod, Type::Tag, Opnd* dst, U_32 t, MethodDesc* encMethod);
    LinkingExcInst* makeLinkingExcInst(Opcode opc, Modifier mod, Type::Tag type, Opnd* dst,
                                       Class_Handle encClass, U_32 CPIndex, U_32 operation);
    VarAccessInst* makeVarAccessInst(Opcode, Type::Tag, Opnd* dst, VarOpnd* var);
    VarAccessInst* makeVarAccessInst(Opcode, Type::Tag, VarOpnd* var, Opnd* src);
    VarAccessInst* makeVarAccessInst(Opcode, Type::Tag, Opnd* dst,
                                     SsaVarOpnd* var);
    VarAccessInst* makeVarAccessInst(Opcode, Type::Tag, SsaVarOpnd* var,
                                     Opnd* src);
    TypeInst* makeTypeInst(Opcode, Modifier mod, Type::Tag, Opnd* dst, Type*);
    TypeInst* makeTypeInst(Opcode, Modifier mod, Type::Tag, Opnd* dst, Opnd* src, Type*);
    TypeInst* makeTypeInst(Opcode,
                           Modifier mod,
                           Type::Tag,
                           Opnd* dst,
                           Opnd* src1,
                           Opnd* src2,
                           Type* td);
    TypeInst* makeTypeInst(Opcode,
                           Modifier mod,
                           Type::Tag,
                           Opnd* dst,
                           Opnd* src1,
                           Opnd* src2,
                           Opnd* src3,
                           Type* td);
    TypeInst* makeTypeInst(Opcode,
                           Modifier mod,
                           Type::Tag,
                           Opnd* dst,
                           Opnd* src1,
                           Opnd* src2,
                           Opnd* src3,
                           Opnd* src4,
                           Type* td);
    TypeInst* makeTypeInst(Opcode,
                           Modifier mod,
                           Type::Tag,
                           Opnd* dst,
                           U_32 nArgs,
                           Opnd** args,
                           Type*);
    FieldAccessInst* makeFieldAccessInst(Opcode, Modifier mod, Type::Tag, Opnd* dst, FieldDesc*);
    FieldAccessInst* makeFieldAccessInst(Opcode, Modifier mod, Type::Tag, Opnd* dst, Opnd* src, FieldDesc*);
    FieldAccessInst* makeFieldAccessInst(Opcode,
                                         Modifier mod,
                                         Type::Tag type,
                                         Opnd* dst,
                                         Opnd* src1,
                                         Opnd* src2,
                                         FieldDesc* fd);
    FieldAccessInst* makeFieldAccessInst(Opcode,
                                         Modifier mod,
                                         Type::Tag type,
                                         Opnd* dst,
                                         U_32 nSrcs,
                                         Opnd** srcs,
                                         FieldDesc* fd);
    MethodInst* makeMethodInst(Opcode, Modifier mod, Type::Tag type, Opnd* dst, MethodDesc* md);
    MethodInst* makeMethodInst(Opcode,
                               Modifier mod,
                               Type::Tag,
                               Opnd* dst,
                               U_32 nArgs,
                               MethodDesc*);
    MethodInst* makeMethodInst(Opcode,
                               Modifier mod,
                               Type::Tag type,
                               Opnd* dst,
                               Opnd* base,
                               MethodDesc*);
    MethodInst* makeMethodInst(Opcode,
                               Modifier mod,
                               Type::Tag type,
                               Opnd* dst,
                               Opnd* base,
                               Opnd* tauVtableHasMethod,
                               MethodDesc*);
    MethodInst* makeMethodInst(Opcode,
                               Modifier mod,
                               Type::Tag,
                               Opnd* dst,
                               U_32 nArgs,
                               Opnd** args_,
                               MethodDesc*);
    MethodCallInst* makeMethodCallInst(Opcode,
                                       Modifier mod,
                                       Type::Tag,
                                       Opnd* dst,
                                       U_32 nArgs,
                                       Opnd** args_,
                                       MethodDesc*);
    CallInst* makeCallInst(Opcode op, Modifier mod,
                           Type::Tag,
                           Opnd* dst,
                           Opnd* ptr,
                           U_32 nArgs,
                           Opnd** args);
    JitHelperCallInst* makeJitHelperCallInst(Opcode op, 
                                             Modifier mod,
                                             Type::Tag,
                                             Opnd* dst,
                                             U_32 nArgs,
                                             Opnd** args_,
                                             JitHelperCallId id);
    VMHelperCallInst* makeVMHelperCallInst(Opcode op, 
                                           Modifier mod,
                                           Type::Tag,
                                           Opnd* dst,
                                           U_32 nArgs,
                                           Opnd** args_,
                                           VM_RT_SUPPORT id);


    PhiInst* makePhiInst(Type::Tag type, Opnd* dst, U_32 nArgs, Opnd** args_);

    MultiSrcInst* makeMultiSrcInst(Opcode, Modifier mod, Type::Tag, Opnd* dst);
    MultiSrcInst* makeMultiSrcInst(Opcode, Modifier mod, Type::Tag, Opnd* dst, Opnd* src);
    MultiSrcInst* makeMultiSrcInst(Opcode, Modifier mod, Type::Tag, Opnd* dst, Opnd* src1, Opnd* src2);
    MultiSrcInst* makeMultiSrcInst(Opcode, Modifier mod, Type::Tag, Opnd* dst, Opnd* src1, Opnd* src2, Opnd* src3);
    MultiSrcInst* makeMultiSrcInst(Opcode, Modifier mod, Type::Tag, Opnd* dst, Opnd* src1, Opnd* src2, Opnd* src3, Opnd* src4);
    MultiSrcInst* makeMultiSrcInst(Opcode, Modifier mod, Type::Tag, Opnd* dst, U_32 nSrcs, Opnd** srcs);
};

//
// Basic container class for instructions
//
class InstDeque : StlDeque<Inst*> {
public:
    InstDeque(MemoryManager& mm) : StlDeque<Inst*>(mm) {}
    bool  isEmpty() const       {return empty();}
    Inst* popFront()            {Inst* inst=front();pop_front();return inst;}
    Inst* popBack()             {Inst* inst=back();pop_back();return inst;}
    void  pushFront(Inst* inst) {push_front(inst);}
    void  pushBack(Inst* inst)  {push_back(inst);}
};


class InstOptimizer {
public:
    virtual ~InstOptimizer() {}
    // returns 0 or an optimized version of inst;
    virtual Inst*
    optimizeInst(Inst* inst) {
        return dispatch(inst);
    }
    // Numeric compute
    virtual Inst*
    caseAdd(Inst* inst)=0; //              {return caseDefault(inst);}

    virtual Inst*
    caseMul(Inst* inst)=0; //                 {return caseDefault(inst);}

    virtual Inst*
    caseSub(Inst* inst)=0; //                 {return caseDefault(inst);}

    virtual Inst*
    caseTauDiv(Inst* inst)=0;//              {return caseDefault(inst);}

    virtual Inst*
    caseTauRem(Inst* inst)=0; //                 {return caseDefault(inst);}

    virtual Inst*
    caseNeg(Inst* inst)=0; //                 {return caseDefault(inst);}

    virtual Inst*
    caseMulHi(Inst* inst)=0;//               {return caseDefault(inst);}

    virtual Inst*
    caseMin(Inst* inst)=0;//                 {return caseDefault(inst);}

    virtual Inst*
    caseMax(Inst* inst)=0;//                 {return caseDefault(inst);}

    virtual Inst*
    caseAbs(Inst* inst)=0;//                 {return caseDefault(inst);}

    // Bitwise
    virtual Inst*
    caseAnd(Inst* inst)=0;//                 {return caseDefault(inst);}

    virtual Inst*
    caseOr(Inst* inst)=0;//                  {return caseDefault(inst);}

    virtual Inst*
    caseXor(Inst* inst)=0;//                 {return caseDefault(inst);}

    virtual Inst*
    caseNot(Inst* inst)=0;//                 {return caseDefault(inst);}

    // selection
    virtual Inst*
    caseSelect(Inst* inst)=0;//              {return caseDefault(inst);}

    // conversion
    virtual Inst*
    caseConv(Inst* inst)=0;//                {return caseDefault(inst);}

    // conversion
    virtual Inst*
    caseConvZE(Inst* inst)=0;//                {return caseDefault(inst);}

    // conversion
    virtual Inst*
    caseConvUnmanaged(Inst* inst)=0;//                {return caseDefault(inst);}

    // shifts
    virtual Inst*
    caseShladd(Inst* inst)=0;//                 {return caseDefault(inst);}

    virtual Inst*
    caseShl(Inst* inst)=0;//                 {return caseDefault(inst);}

    virtual Inst*
    caseShr(Inst* inst)=0;//                 {return caseDefault(inst);}

    // comparison
    virtual Inst*
    caseCmp(Inst* inst)=0;//                {return caseDefault(inst);}

    virtual Inst*
    caseCmp3(Inst* inst)=0;//               {return caseDefault(inst);}

    // Control flow
    virtual Inst*
    caseBranch(BranchInst* inst)=0;//       {return caseDefault(inst);}

    virtual Inst*
    caseJump(BranchInst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseSwitch(SwitchInst* inst)=0;//       {return caseDefault(inst);}

    virtual Inst*
    caseDirectCall(MethodCallInst* inst)=0;//  {return caseDefault(inst);}

    virtual Inst*
    caseTauVirtualCall(MethodCallInst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseIndirectCall(CallInst* inst)=0;//   {return caseDefault(inst);}

    virtual Inst*
    caseIndirectMemoryCall(CallInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseJitHelperCall(JitHelperCallInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseVMHelperCall(VMHelperCallInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseReturn(Inst* inst)=0;//             {return caseDefault(inst);}

    virtual Inst*
    caseCatch(Inst* inst)=0;//              {return caseDefault(inst);}

    virtual Inst*
    caseThrow(Inst* inst)=0;//              {return caseDefault(inst);}

    virtual Inst*
    casePseudoThrow(Inst* inst)=0;//        {return caseDefault(inst);}

    virtual Inst*
    caseThrowSystemException(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseThrowLinkingException(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseLeave(Inst* inst)=0;//              {return caseDefault(inst);}

    virtual Inst*
    caseJSR(Inst* inst)=0;//          {return caseDefault(inst);}

    virtual Inst*
    caseRet(Inst* inst)=0;//                {return caseDefault(inst);}

    virtual Inst*
    caseSaveRet(Inst* inst)=0;//            {return caseDefault(inst);}

    // load, store & mov
    virtual Inst*
    caseCopy(Inst* inst)=0;//               {return caseDefault(inst);}

    virtual Inst*
    caseDefArg(Inst* inst)=0;//             {return caseDefault(inst);}

    virtual Inst*
    caseLdConstant(ConstInst* inst)=0;//    {return caseDefault(inst);}

    virtual Inst*
    caseLdNull(ConstInst* inst)=0;//        {return caseDefault(inst);}

    virtual Inst*
    caseLdRef(TokenInst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseLdVar(Inst* inst)=0;//              {return caseDefault(inst);}

    virtual Inst*
    caseLdVarAddr(Inst* inst)=0;//          {return caseDefault(inst);}

    virtual Inst*
    caseTauLdInd(Inst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauLdField(FieldAccessInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseLdStatic(FieldAccessInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseTauLdElem(TypeInst* inst)=0;//             {return caseDefault(inst);}

    virtual Inst*
    caseLdFieldAddr(FieldAccessInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseLdStaticAddr(FieldAccessInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseLdElemAddr(TypeInst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauLdVTableAddr(Inst* inst)=0;//       {return caseDefault(inst);}

    virtual Inst*
    caseTauLdIntfcVTableAddr(TypeInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseTauLdVirtFunAddr(MethodInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseTauLdVirtFunAddrSlot(MethodInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseLdFunAddr(MethodInst* inst)=0;//    {return caseDefault(inst);}

    virtual Inst*
    caseLdFunAddrSlot(MethodInst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseGetVTableAddr(TypeInst* inst)=0;//      {return caseDefault(inst);}

    virtual Inst*
    caseGetClassObj(TypeInst* inst)=0;//      {return caseDefault(inst);}

    virtual Inst*
    caseTauArrayLen(Inst* inst)=0;//           {return caseDefault(inst);}

    virtual Inst*
    caseLdArrayBaseAddr(Inst* inst)=0;//    {return caseDefault(inst);}

    virtual Inst*
    caseAddScaledIndex(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseStVar(Inst* inst)=0;//              {return caseDefault(inst);}

    virtual Inst*
    caseTauStInd(Inst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauStField(Inst* inst)=0;//            {return caseDefault(inst);}

    virtual Inst*
    caseTauStElem(Inst* inst)=0;//             {return caseDefault(inst);}

    virtual Inst*
    caseTauStStatic(Inst* inst)=0;//           {return caseDefault(inst);}

    virtual Inst*
    caseTauStRef(Inst* inst)=0;//              {return caseDefault(inst);}

    // checks
    virtual Inst*
    caseTauCheckBounds(Inst* inst)=0;//        {return caseDefault(inst);}

    virtual Inst*
    caseTauCheckLowerBound(Inst* inst)=0;//      {return caseDefault(inst);}

    virtual Inst*
    caseTauCheckUpperBound(Inst* inst)=0;//      {return caseDefault(inst);}

    virtual Inst*
    caseTauCheckNull(Inst* inst)=0;//          {return caseDefault(inst);}

    virtual Inst*
    caseTauCheckZero(Inst* inst)=0;//          {return caseDefault(inst);}

    virtual Inst*
    caseTauCheckDivOpnds(Inst* inst)=0;//      {return caseDefault(inst);}
    
    virtual Inst*
    caseTauCheckElemType(Inst* inst)=0;//      {return caseDefault(inst);}

    virtual Inst*
    caseTauCheckFinite(Inst* inst)=0;//         {return caseDefault(inst);}

    // alloc

    virtual Inst*
    caseNewObj(Inst* inst)=0;//             {return caseDefault(inst);}

    virtual Inst*
    caseNewArray(Inst* inst)=0;//           {return caseDefault(inst);}

    virtual Inst*
    caseNewMultiArray(Inst* inst)=0;//      {return caseDefault(inst);}

    // sync

    virtual Inst*
    caseTauMonitorEnter(Inst* inst)=0;//       {return caseDefault(inst);}

    virtual Inst*
    caseTauMonitorExit(Inst* inst)=0;//        {return caseDefault(inst);}

    virtual Inst*
    caseTypeMonitorEnter(Inst* inst)=0;//   {return caseDefault(inst);}

    virtual Inst*
    caseTypeMonitorExit(Inst* inst)=0;//    {return caseDefault(inst);}

    virtual Inst*
    caseLdLockAddr(Inst* inst)=0;//       {return caseDefault(inst);}

    virtual Inst*
    caseIncRecCount(Inst* inst)=0;//       {return caseDefault(inst);}

    virtual Inst*
    caseTauBalancedMonitorEnter(Inst* inst)=0;//       {return caseDefault(inst);}

    virtual Inst*
    caseBalancedMonitorExit(Inst* inst)=0;//        {return caseDefault(inst);}

    virtual Inst*
    caseTauOptimisticBalancedMonitorEnter(Inst* inst)=0;//{return caseDefault(inst);}

    virtual Inst*
    caseOptimisticBalancedMonitorExit(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseMonitorEnterFence(Inst* inst)=0;//       {return caseDefault(inst);}

    virtual Inst*
    caseMonitorExitFence(Inst* inst)=0;//        {return caseDefault(inst);}

    // type checking
    virtual Inst*
    caseTauStaticCast(TypeInst* inst)=0;//     {return caseDefault(inst);}

    virtual Inst*
    caseTauCast(TypeInst* inst)=0;//           {return caseDefault(inst);}

    virtual Inst*
    caseSizeof(TypeInst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauAsType(TypeInst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauInstanceOf(TypeInst* inst)=0;//     {return caseDefault(inst);}

    virtual Inst*
    caseInitType(TypeInst* inst)=0;//       {return caseDefault(inst);}

    // labels

    virtual Inst*
    caseLabel(Inst* inst)=0;//              {return caseDefault(inst);}

    // type checking
    virtual Inst*
    caseCatchLabelInst(Inst* inst)=0;//     {return caseDefault(inst);}

    // method entry/exit
    virtual Inst*
    caseMethodEntryLabel(Inst* inst)=0;//   {return caseDefault(inst);}

    virtual Inst*
    caseMethodEntry(Inst* inst)=0;//        {return caseDefault(inst);}

    virtual Inst*
    caseMethodEnd(Inst* inst)=0;//          {return caseDefault(inst);}

    // source markers
    virtual Inst*
    caseMethodMarker(Inst* inst)=0;//       {return caseDefault(inst);}

    virtual Inst*
    casePhi(Inst* inst)=0;//                {return caseDefault(inst);}

    virtual Inst*
    caseTauPi(TauPiInst* inst)=0;//                 {return caseDefault(inst);}

    virtual Inst*
    caseIncCounter(Inst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    casePrefetch(Inst* inst)=0;//         {return caseDefault(inst);}

    // compressed references

    virtual Inst*
    caseUncompressRef(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseCompressRef(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseLdFieldOffset(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseLdFieldOffsetPlusHeapbase(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseLdArrayBaseOffset(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseLdArrayBaseOffsetPlusHeapbase(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseLdArrayLenOffset(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseLdArrayLenOffsetPlusHeapbase(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseAddOffset(Inst* inst)=0;// {return caseDefault(inst);}

    virtual Inst*
    caseAddOffsetPlusHeapbase(Inst* inst)=0;// {return caseDefault(inst);}

    // new tau methods
    virtual Inst*
    caseTauPoint(Inst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauEdge(Inst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauAnd(Inst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauSafe(Inst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauUnsafe(Inst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauCheckCast(TypeInst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauHasType(TypeInst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauHasExactType(TypeInst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseTauIsNonNull(Inst* inst)=0;//         {return caseDefault(inst);}

    virtual Inst*
    caseIdentHC(Inst* inst)=0;//         {return caseDefault(inst);}
    
    virtual Inst*
    caseDefault(Inst* inst)=0;//            {return NULL;}

protected:
    Inst* dispatch(Inst*);

};

} //namespace Jitrino 

#endif // _INST_H_







