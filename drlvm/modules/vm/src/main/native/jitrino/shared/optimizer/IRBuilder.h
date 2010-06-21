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

#ifndef _IRBUILDER_H_
#define _IRBUILDER_H_

#include "open/bytecodes.h"

#include "MemoryManager.h"
#include "Opcode.h"
#include "Inst.h"
#include "CSEHash.h"
#include "simplifier.h"
#include "IRBuilderFlags.h"
#include "PMFAction.h"


#include <iostream>

namespace Jitrino {

class PiCondition;
class VectorHandler;
class MapHandler;
class CompilationContext;
class SessionAction;
struct TranslatorFlags;


#define IRBUILDER_ACTION_NAME "irbuilder"

class IRBuilderAction : public Action {
public:
    void init();
    const IRBuilderFlags& getFlags() const {return irBuilderFlags;}
private:
    void readFlags();
    
    IRBuilderFlags    irBuilderFlags;
};


class IRBuilder : public SessionAction {
public:
    IRBuilder();
    void init(IRManager* irm, TranslatorFlags* traFlags, MemoryManager& tmpMM);
    
    //this session can't be placed into PMF path
    void run(){assert(0);}

    IRManager*          getIRManager()   const  { return irManager;}
    InstFactory*        getInstFactory() const  {return instFactory;}
    TypeManager*        getTypeManager() const  {return typeManager;}
    OpndManager*        getOpndManager() const  {return opndManager;}
    ControlFlowGraph*   getFlowGraph() const  {return flowGraph;}
    TranslatorFlags*    getTranslatorFlags() const {return translatorFlags;}

    // 
    // used to map bytecode offsets to instructions by JavaByteCodeTranslator
    Inst* getLastGeneratedInst();

    // TRANSLATOR GEN
    // gen methods used in translators (front ends)
    // if we had better compilers, we might separate these out into abstract methods 
    // in an interface superclass
    // note that some of these are also used by the _Simplifier methods below
    
    // arithmetic instructions
    Opnd* genAdd(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2); // TR //SI
    Opnd* genMul(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2);//TR //SI
    Opnd* genSub(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2);//TR //SI
    Opnd* genCliDiv(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2); // TR
    Opnd* genDiv(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2); //TR
    Opnd* genCliRem(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2); // TR
    Opnd* genRem(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2);//TR
    Opnd* genNeg(Type* dstType, Opnd* src);//TR //SI
    Opnd* genMulHi(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2); //SI
    Opnd* genMin(Type* dstType, Opnd* src1, Opnd* src2); //SI
    Opnd* genMax(Type* dstType, Opnd* src1, Opnd* src2); //SI
    Opnd* genAbs(Type* dstType, Opnd* src1); //SI
    // bitwise instructions
    Opnd* genAnd(Type* dstType, Opnd* src1, Opnd* src2); // TR //SI
    Opnd* genOr(Type* dstType, Opnd* src1, Opnd* src2);//TR //SI
    Opnd* genXor(Type* dstType, Opnd* src1, Opnd* src2);//TR //SI
    Opnd* genNot(Type* dstType, Opnd* src);//TR //SI
    // Conversion
    Opnd* genConv(Type* dstType, Type::Tag toType, Modifier ovfMod, Opnd* src); //TR //SI
    Opnd* genConvZE(Type* dstType, Type::Tag toType, Modifier ovfMod, Opnd* src); //TR //SI
    Opnd* genConvUnmanaged(Type* dstType, Type::Tag toType, Modifier ovfMod, Opnd* src); //TR //SI
    // Shift
    Opnd* genShl(Type* dstType, Modifier mod, Opnd* value, Opnd* shiftAmount);//TR //SI
    Opnd* genShr(Type* dstType, Modifier mods, Opnd* value, Opnd* shiftAmount);//TR //SI
    // Comparison
    Opnd* genCmp(Type* dstType, Type::Tag srcType, ComparisonModifier mod, Opnd* src1, Opnd* src2); // TR //SI
    // 3-way comparison, used to represent Java tests:
    //    (s1 cmp s2) ? 1 : (s2 cmp s1) : -1 : 0
    Opnd*      genCmp3(Type* dstType, Type::Tag srcType, ComparisonModifier mod, Opnd* src1, Opnd* src2); // TR
    void  genBranch(Type::Tag instType, ComparisonModifier mod, LabelInst* label, Opnd* src1, Opnd* src2); // TR //SI
    void  genBranch(Type::Tag instType, ComparisonModifier mod, LabelInst* label, Opnd* src); // TR //SI
    void  genJump(LabelInst* label); //TR
    void  genSwitch(U_32 numLabels, LabelInst* label[], LabelInst* defaultLabel, Opnd* src);//TR

        
    // Calls
    // If the call does not return a value, then the returned Opnd* is the
    // null Opnd*.
    Opnd* genIndirectCallWithResolve(Type* returnType,
                                    Opnd* tauNullCheckedFirstArg,
                                    Opnd* tauTypesChecked,
                                    U_32 numArgs,
                                    Opnd* args[],
                                    ObjectType* ch,
                                    JavaByteCodes bc,
                                    U_32 cpIndex,
                                    MethodSignature* sig
                                    );
    
    Opnd* genDirectCall(MethodDesc* methodDesc, // TR //SI
                        Type* returnType,
                        Opnd* tauNullCheckedFirstArg, // 0 for unsafe
                        Opnd* tauTypesChecked,        // 0 to let IRBuilder find taus
                        U_32 numArgs,
                        Opnd* args[]);
   
   Opnd* genTauVirtualCall(MethodDesc* methodDesc,//TR
                                  Type* returnType,
                                  Opnd* tauNullCheckedFirstArg, // 0 to let IRBuilder add check
                                  Opnd* tauTypesChecked,        // 0 to let IRBuilder find it
                                  U_32 numArgs,
                                  Opnd* args[]);

    Opnd* genIndirectCall(      Type* returnType, //TR
                                  Opnd* funAddr,
                                  Opnd* tauNullCheckedFirstArg, // 0 for unsafe
                                  Opnd* tauTypesChecked,        // 0 to let IRBuilder find it
                                 U_32 numArgs,
                                  Opnd* args[]);

    Opnd*  genJitHelperCall(JitHelperCallId helperId,
                            Type* returnType,
                            U_32 numArgs,
                            Opnd*  args[]);
    Opnd*  genJitHelperCall(JitHelperCallId helperId,
                            Type* returnType,
                            Opnd* tauNullCheckedRefArgs,
                            Opnd* tauTypesChecked,
                            U_32 numArgs,
                            Opnd*  args[]);

    Opnd*  genVMHelperCall(VM_RT_SUPPORT helperId,
                           Type* returnType,
                           U_32 numArgs,
                           Opnd*  args[]);

    
    void       genReturn(Opnd* src, Type* retType);//TR
    void       genReturn();//TR
    Opnd*      genCatch(Type* exceptionType); // TR
    void       genThrow(ThrowModifier mod, Opnd* exceptionObj);//TR
    void       genPseudoThrow();//TR
    void       genThrowSystemException(CompilationInterface::SystemExceptionId);//SI
    void       genThrowLinkingException(Class_Handle encClass, U_32 CPIndex, U_32 operation);//SI
    void       genJSR(LabelInst* label); //TR
    void       genRet(Opnd *src);//TR
    Opnd*      genSaveRet();//TR
    // load, store & move
    Opnd* genLdConstant(I_32 val); //TR //SI
    Opnd* genLdConstant(int64 val); //TR //SI
    Opnd* genLdConstant(float val); //TR //SI
    Opnd* genLdConstant(double val); //TR //SI
    Opnd* genLdConstant(Type *ptrtype, ConstInst::ConstValue val);//TR //SI
    Opnd*      genLdFloatConstant(float val);//TR
    Opnd*      genLdFloatConstant(double val);//TR
    Opnd*      genLdNull();//TR

    Opnd*      genLdRef(MethodDesc* enclosingMethod, U_32 stringToken, Type* type);//TR
    Opnd*      genLdVar(Type* dstType, VarOpnd* var);//TR
    Opnd*      genLdVarAddr(VarOpnd* var);//TR
    Opnd*      genLdInd(Type*, Opnd* ptr); // for use by front-ends, but not simplifier//TR
    Opnd*      genLdField(Type*, Opnd* base, FieldDesc* fieldDesc); //TR
    Opnd*      genLdStatic(Type*, FieldDesc* fieldDesc);//TR
    Opnd*      genLdElem(Type* elemType, Opnd* array, Opnd* index); //TR
    Opnd*      genLdElem(Type* elemType, Opnd* array, Opnd* index,
                         Opnd* tauNullCheck, Opnd* tauAddressInRange);
    Opnd*      genLdFieldAddr(Type* fieldType, Opnd* base, FieldDesc* fieldDesc); //TR
    Opnd*      genLdFieldAddrWithResolve(Type* fieldType, Opnd* base, ObjectType* enclClass, U_32 cpIndex, bool putfield); //TR
    Opnd*      genLdStaticAddr(Type* fieldType, FieldDesc* fieldDesc);//TR
    Opnd*      genLdStaticAddrWithResolve(Type* fieldType, ObjectType* enclClass, U_32 cpIndex, bool putfield);//TR
    Opnd*      genLdElemAddr(Type* elemType, Opnd* array, Opnd* index);//TR
    Opnd*      genLdVirtFunAddr(Opnd* base, MethodDesc* methodDesc);//TR
    Opnd*      genLdFunAddr(MethodDesc* methodDesc);//TR
    Opnd*      genArrayLen(Type* dstType, Type::Tag type, Opnd* array); // TR

    Opnd*      genLdFieldWithResolve(Type*, Opnd* base, ObjectType* enclClass, U_32 cpIdx); //TR
    Opnd*      genLdStaticWithResolve(Type*, ObjectType* enclClass, U_32 cpIdx);//TR

    // store instructions
    void       genStVar(VarOpnd* var, Opnd* src);//TR
    void       genStField(Type*, Opnd* base, FieldDesc* fieldDesc, Opnd* src);//TR
    void       genStStatic(Type*, FieldDesc* fieldDesc, Opnd* src);//TR
    void       genStElem(Type*, Opnd* array, Opnd* index, Opnd* src);//TR
    void       genStElem(Type*, Opnd* array, Opnd* index, Opnd* src,
                         Opnd* tauNullCheck, Opnd* tauBaseTypeCheck, Opnd* tauAddressInRange);
    void       genStInd(Type*, Opnd* ptr, Opnd* src);//TR

    void       genStFieldWithResolve(Type*, Opnd* base, ObjectType* enclClass, U_32 cpIdx, Opnd* src);//TR
    void       genStStaticWithResolve(Type*, ObjectType* enclClass, U_32 cpIdx, Opnd* src);//TR

    // checks
    Opnd*      genTauCheckNull(Opnd* base);
    Opnd*      genTauCheckBounds(Opnd* array, Opnd* index, Opnd *tauNullChecked);
    Opnd*      genCheckFinite(Type* dstType, Opnd* src); // TR
    // allocation
    Opnd*      genNewObj(Type* type);//TR
    Opnd*      genNewObjWithResolve(ObjectType* enclClass, U_32 cpIndex);//TR
    Opnd*      genNewArray(NamedType* elemType, Opnd* numElems);//TR
    Opnd*      genNewArrayWithResolve(NamedType* elemType, Opnd* numElems, ObjectType* enclClass, U_32 cpIndex);//TR
    Opnd*      genMultianewarray(NamedType* arrayType, U_32 dimensions, Opnd** numElems);//TR
    Opnd*      genMultianewarrayWithResolve(NamedType* arrayType, ObjectType* enclClass, U_32 cpIndex, U_32 dimensions, Opnd** numElems);//TR
    //Opnd*      genMultianewarrayWithResolve(NamedType* arrayType, U_32 dimensions, Opnd** numElems);//TR
    // sync
    void       genMonitorEnter(Opnd* src); // also inserts nullcheck of src//TR
    void       genMonitorExit(Opnd* src);  // also inserts nullcheck of src//TR
    // typemonitors
    void       genTypeMonitorEnter(Type *type);//TR
    void       genTypeMonitorExit(Type *type);//TR
    // lowered parts of monitor enter/exit;
    //   these assume src is already checked and is not null
    Opnd*      genLdLockAddr(Type *dstType, Opnd *obj);    // result is ref:int16//TR
    Opnd*      genBalancedMonitorEnter(Type *dstType, Opnd* src, Opnd *lockAddr); // result is I_32 // TR
    void       genBalancedMonitorExit(Opnd* src, Opnd *lockAddr, Opnd *oldValue); // TR
    // type checking
    // CastException (succeeds if argument is null, returns casted object)
    Opnd*      genCast(Opnd* src, Type* type); // TR
    Opnd*      genCastWithResolve(Opnd* src, Type* type, ObjectType* enclClass, U_32 cpIndex); // TR
    // returns trueResult if src is an instance of type, 0 otherwise
    Opnd*      genAsType(Opnd* src, Type* type); // TR
    // returns 1 if src is not null and an instance of type, 0 otherwise
    Opnd*      genInstanceOf(Opnd* src, Type* type); //TR
    Opnd*      genInstanceOfWithResolve(Opnd* src, ObjectType* enclClass, U_32 cpIndex); //TR
    void       genInitType(NamedType* type); //TR
    // labels
    void       genLabel(LabelInst* labelInst); //TR
    void       genFallThroughLabel(LabelInst* labelInst); //TR
    // method entry/exit
    LabelInst* genMethodEntryLabel(MethodDesc* methodDesc);//TR
    
    void       genTauTypeCompare(Opnd *arg0, MethodDesc* methodDesc, LabelInst *target,
                                 Opnd *tauNullChecked);//TR

    Opnd*      genArgCoercion(Type* argType, Opnd* actualArg); // TR
    // actual parameter and variable definitions
    Opnd*      genArgDef(Modifier, Type*); // TR
    VarOpnd*   genVarDef(Type*, bool isPinned);//TR
    // Phi-node instruction
    Opnd*      genPhi(U_32 numArgs, Opnd* args[]);//TR

    // label manipulation for translators
    LabelInst* createLabel();
    // this should really be genBlocks
    void       createLabels(U_32 numLabels, LabelInst** labels);
    void       killCSE();
    LabelInst* getCurrentLabel() {return currentLabel;}


    // SIMPLIFIER GEN
    // gen methods for use in _Simplifier below, also may be used elsewhere
    // selection
    Opnd* genSelect(Type* dstType, Opnd* src1, Opnd* src2, Opnd *src3); //SI
    // Shift
    Opnd* genShladd(Type* dstType, Opnd* value, Opnd* shiftAmount, Opnd* addto); //SI
    // Control flow
    // load, store & move
    Opnd* genLdRef(Modifier mod, Type *dstType,//TR //SI
                      U_32 token, MethodDesc *enclosingMethod); // for simplifier use
    Opnd* genTauLdInd(Modifier mod, Type *dstType, Type::Tag ldType, Opnd *ptr, //SI
                      Opnd *tauNonNullBase, Opnd *tauAddressInRange); // for simplifier use
    Opnd* genLdFunAddrSlot(MethodDesc* methodDesc); //SI
    Opnd* genGetVTable(ObjectType* type); //SI
    Opnd* genGetClassObj(ObjectType* type);
    // compressed reference instructions
    Opnd* genUncompressRef(Opnd *compref); //SI
    Opnd* genCompressRef(Opnd *uncompref); //SI
    Opnd* genLdFieldOffsetPlusHeapbase(FieldDesc* fieldDesc); //SI
    Opnd* genLdArrayBaseOffsetPlusHeapbase(Type *elemType); //SI
    Opnd* genLdArrayLenOffsetPlusHeapbase(Type *elemType); //SI
    Opnd* genAddOffsetPlusHeapbase(Type *ptrType, Opnd* compref, Opnd* offset); //SI

    // RECURSIVE GEN
    Opnd*      genIndirectMemoryCall(Type* returnType,
                                     Opnd* funAddr,
                                     Opnd* tauNullCheckedFirstArg, // 0 to let IRBuilder add check
                                     Opnd* tauTypesChecked,        // 0 to let IRBuilder find it
                                     U_32 numArgs,
                                     Opnd* args[]);

    Opnd*      genLdElemAddrNoChecks(Type *elemType, Opnd* array, Opnd* index);
    Opnd*      genTauLdVirtFunAddrSlot(Opnd* base, Opnd* tauOk, MethodDesc* methodDesc);
    Opnd*      genLdVTable(Opnd* base, Type* type);
    Opnd*      genTauLdVTable(Opnd* base, Opnd *tauNullChecked, Type* type);
    Opnd*      genLdArrayBaseAddr(Type* elemType, Opnd* array);
    Opnd*      genAddScaledIndex(Opnd* ptr, Opnd* index);
    void       genTauStRef(Type*, Opnd *objectbase, Opnd* ptr, Opnd* value,
                           Opnd *tauBaseNonNull, Opnd *tauAddressInRange,
                           Opnd *tauELemTypeChecked);
    void       genTauStInd(Type*, Opnd* ptr, Opnd* src,
                           Opnd *tauBaseNonNull, Opnd *tauAddressInRange,
                           Opnd *tauElemTypeChecked);
    // checks
    Opnd*      genTauCheckZero(Opnd* base);
    Opnd*      genTauCheckDivOpnds(Opnd* num, Opnd* denom);
    Opnd*      genTauCheckElemType(Opnd* array, Opnd* src, Opnd *tauNullChecked,
                                   Opnd* tauIsArray);
    Opnd*      genTauCheckBounds(Opnd* ub, Opnd* index);
    Opnd*      genTauArrayLen(Type* dstType, Type::Tag type, Opnd* array,
                              Opnd *tauNullChecked, Opnd *tauBaseTypeChecked);
    Opnd*      genTauBalancedMonitorEnter(Type *dstType, Opnd* src, Opnd *lockAddr,
                                          Opnd *tauNullChecked); // result is I_32
    Opnd*      genTauCheckFinite(Opnd* src);
    // tau operations
    Opnd*      genTauSafe();
    Opnd*      genTauMethodSafe();
    Opnd*      genTauUnsafe();
    Opnd*      genTauCheckCast(Opnd* src, Opnd *tauNullChecked, Type* type);
    Opnd*      genTauStaticCast(Opnd *src, Opnd *tauCheckedCast, Type *castType);
    Opnd*      genTauHasType(Opnd *src, Type *hasType);
    //generates tauhastype + copy conversion of src has unresolved type
    //stores result of conversion in *src
    Opnd*      genTauHasTypeWithConv(Opnd **src, Type *hasType);
    Opnd*      genTauHasExactType(Opnd *src, Type *hasType);
    Opnd*      genTauIsNonNull(Opnd *src);
    Opnd*      genTauAnd(Opnd *src1, Opnd *src2);

    // UNUSED GEN
    void       genPrefetch(Opnd *addr); // prefetch
    Opnd*      genCopy(Opnd* src);
    Opnd*      genTauPi(Opnd* src, Opnd *tau, PiCondition *cond);
    // compressed reference instructions
    Opnd*      genLdFieldOffset(FieldDesc* fieldDesc);
    Opnd*      genLdArrayBaseOffset(Type *elemType);
    Opnd*      genLdArrayLenOffset(Type *elemType);
    Opnd*      genAddOffset(Type *ptrType, Opnd* ref, Opnd* offset);
    // lowered parts of monitor enter/exit;
    //   these assume src is already checked and is not null
    void       genIncRecCount(Opnd *obj, Opnd *oldLock);   // result is ref:int16
    Opnd*      genTauOptimisticBalancedMonitorEnter(Type *dstType, Opnd* src, Opnd *lockAddr,
                                                    Opnd *tauNullChecked); // result is I_32
    void       genOptimisticBalancedMonitorExit(Opnd* src, Opnd *lockAddr, Opnd *oldValue);
    void       genMonitorEnterFence(Opnd *src);
    void       genMonitorExitFence(Opnd *src);
    
private:

    void readFlagsFromCommandLine(SessionAction* argSource, const char* argPrefix);
    void updateCurrentLabelBcOffset();


private:
    //
    // private helper methods
    //
    Opnd*    propagateCopy(Opnd*);
    Inst*    appendInst(Inst*);
    Type*    getOpndTypeFromLdType(Type* ldType);
    Opnd*    createOpnd(Type*);
    PiOpnd*  createPiOpnd(Opnd *org);
    Opnd*    createTypeOpnd(ObjectType* type);
    Opnd*    lookupHash(U_32 opc);
    Opnd*    lookupHash(U_32 opc, U_32 op);
    Opnd*    lookupHash(U_32 opc, U_32 op1, U_32 op2);
    Opnd*    lookupHash(U_32 opc, U_32 op1, U_32 op2, U_32 op3);
    Opnd*    lookupHash(U_32 opc, Opnd* op) { return lookupHash(opc, op->getId()); };
    Opnd*    lookupHash(U_32 opc, Opnd* op1, Opnd* op2) { return lookupHash(opc, op1->getId(), op2->getId()); };
    Opnd*    lookupHash(U_32 opc, Opnd* op1, Opnd* op2, Opnd* op3) { return lookupHash(opc, op1->getId(), op2->getId(), op3->getId()); };
    void     insertHash(U_32 opc, Inst*);
    void     insertHash(U_32 opc, U_32 op, Inst*);
    void     insertHash(U_32 opc, U_32 op1, U_32 op2, Inst*);
    void     insertHash(U_32 opc, U_32 op1, U_32 op2, U_32 op3, Inst*);
    void     insertHash(U_32 opc, Opnd* op, Inst*i) { insertHash(opc, op->getId(), i); };
    void     insertHash(U_32 opc, Opnd* op1, Opnd* op2, Inst*i) { insertHash(opc, op1->getId(), op2->getId(), i); };
    void     insertHash(U_32 opc, Opnd* op1, Opnd* op2, Opnd* op3, Inst*i) { insertHash(opc, op1->getId(), op2->getId(), op3->getId(), i); };
    void     invalid();    // called when the builder detects invalid IR
    void     setBcOffset(U_32 bcOffset) {  offset =  bcOffset;}
    U_32   getBcOffset() const {  return offset; };

    friend class    JavaByteCodeTranslator;
    
    //
    // private fields
    //
    // references to other translation objects
    //
    IRManager*          irManager;
    OpndManager*        opndManager;        // generates operands
    TypeManager*        typeManager;        // generates types
    InstFactory*        instFactory;        // generates instructions
    ControlFlowGraph*   flowGraph;          // generates blocks
    IRBuilderFlags      irBuilderFlags;     // flags that control translation 
    TranslatorFlags*    translatorFlags;
    //
    // translation state
    //
    LabelInst*          currentLabel;       // current header label
    CSEHashTable*        cseHashTable;       // hash table for CSE
    
    //
    // simplifier to fold constants etc.
    //
    Simplifier* simplifier;
    //
    // method-safe operand
    Opnd*               tauMethodSafeOpnd;

    // current bc offset
    U_32 offset;
};

} //namespace Jitrino 

#endif // _IRBUILDER_H_
