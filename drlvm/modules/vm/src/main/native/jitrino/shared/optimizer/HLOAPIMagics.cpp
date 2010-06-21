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
 * @author Intel, George A. Timoshenko
 */

#include "HLOAPIMagics.h"
#include "Opcode.h"
#include "PMF.h"
#include "VMInterface.h"

namespace Jitrino {

bool arraycopyOptimizable(Inst* arraycopyCall, bool needWriteBarriers) {
    //
    //  an ArrayStoreException is thrown and the destination is not modified: 
    //  
    //  - The src argument refers to an object that is not an array. 
    //  - The dest argument refers to an object that is not an array. 
    //  - The src argument and dest argument refer to arrays whose component types are different primitive types. 
    //  - The src argument refers to an array with a primitive component type and the dest argument
    //    refers to an array with a reference component type. 
    //  - The src argument refers to an array with a reference component type and the dest argument
    //    refers to an array with a primitive component type. 
    //
    Opnd * src = arraycopyCall->getSrc(2);
    Type * srcType = src->getType();
    Opnd * dst = arraycopyCall->getSrc(4);
    Type * dstType = dst->getType();
    assert(srcType->isObject() &&
           arraycopyCall->getSrc(3)->getType()->isInt4() && // 1 - srcPos
           dstType->isObject() &&
           arraycopyCall->getSrc(5)->getType()->isInt4() && // 3 - dstPos
           arraycopyCall->getSrc(6)->getType()->isInt4());  // 4 - length

    bool srcIsArray = srcType->isArray() && !srcType->isUnresolvedType();
    bool dstIsArray = dstType->isArray() && !dstType->isUnresolvedType();

    bool isOptimizable = true;

    if ( srcIsArray && dstIsArray )  {
        // these are arrays        

        ArrayType* srcAsArrayType = srcType->asArrayType();
        ArrayType* dstAsArrayType = dstType->asArrayType();
        bool srcIsArrOfPrimitive = srcIsArray && VMInterface::isArrayOfPrimitiveElements(srcAsArrayType->getVMTypeHandle());
        bool dstIsArrOfPrimitive = dstIsArray && VMInterface::isArrayOfPrimitiveElements(dstAsArrayType->getVMTypeHandle());

        // are these primitive or reference arrays?
        if ( srcIsArrOfPrimitive && dstIsArrOfPrimitive ) {
            // both arrays are primitive

            // if we are dealing with different primitive type arrays, reject optimization
            // TODO: is that really necessary?
            isOptimizable = (srcType == dstType); 

        } else if ( srcIsArrOfPrimitive ^ dstIsArrOfPrimitive ) {
            // arrays are mixed primitive and reference types
            // reject optimization
            isOptimizable = false;

        } else {
            // both arrays are reference

            // if write barriers are enabled, reject optimization
            // if not, check the types
            if ( needWriteBarriers ) { 
                isOptimizable = false;
            } else {
                // Here is some inaccuracy. If src is a subclass of dst there is no ASE for sure.
                // If it is not, we should check the assignability of each element being copied.
                // To avoid this we just reject the inlining of System::arraycopy call in this case.
                NamedType* srcElemType = srcAsArrayType->getElementType();
                NamedType* dstElemType = dstAsArrayType->getElementType();
                isOptimizable = (srcElemType->getVMTypeHandle() == dstElemType->getVMTypeHandle());
            }

        }

    } else {
        // source or destination are not arrays
        isOptimizable = false;
    }

    return isOptimizable;
}


void
System_arraycopy_HLO_Handler::run()
{
    InstFactory&        instFactory = builder->getInstFactory();
    ControlFlowGraph&   cfg         = builder->getControlFlowGraph();

    Node* firstNode = callInst->getNode();
    Edge* outEdge = firstNode->getOutEdge(Edge::Kind_Unconditional);
    Node* exitNode = outEdge->getTargetNode();
    Node* dispatch = firstNode->getExceptionEdgeTarget();
    assert(dispatch);
    callInst->unlink();
    cfg.removeEdge(outEdge);

    builder->setCurrentBCOffset(callInst->getBCOffset());
    builder->setCurrentNode(firstNode);

    // the fist two are tau operands
    Opnd * src = callInst->getSrc(2);
    Opnd * srcPos = callInst->getSrc(3);
    Type * srcPosType = srcPos->getType();
    Opnd * dst = callInst->getSrc(4);
    Opnd * dstPos = callInst->getSrc(5);
    Type * dstPosType = dstPos->getType();
    Opnd * len = callInst->getSrc(6);

    //
    //  Generate exception condition checks:
    //      chknull src
    //      chknull dst
    //      cmpbr srcPos < 0, boundsException
    //      cmpbr dstPos < 0, boundsException
    //      cmpbr len < 0, boundsException
    //      srcEnd = add srcPos, len
    //      srcLen = src.length
    //      cmpbr srcEnd > srcLen, boundsException
    //      dstEnd = add dstPos, len
    //      dstLen = dst.length
    //      cmpbr dstEnd > dstLen, boundsException
    //  Skip trivial:
    //      cmpbr (src == dst) && (dstPos == srcPos), Exit
    //
    //  Choose a direction:
    //      cmpbr (dstPos > srcPos), Reverse
    //
    //  Direct:
    //      JitHelperCall id=ArrayCopyDirect
    //      goto Exit
    //  Reverse:
    //      srcPos = srcPos + len - 1
    //      dstPos = dstPos + len - 1
    //      JitHelperCall id=ArrayCopyReverse
    //      goto Exit
    //
    //  boundsException:
    //      chkbounds -1, src
    //  Exit:
    //

    // Referenced nodes creation
    LabelInst * reverseCopying = (LabelInst*)instFactory.makeLabel();
    LabelInst * boundsException = (LabelInst*)instFactory.makeLabel();
    LabelInst * Exit = (LabelInst*)exitNode->getLabelInst();

    Node* BExcNode = cfg.createBlockNode(boundsException);
    Node* RevCopyNode = cfg.createBlockNode(reverseCopying);

    //Other nodes creation and filling
    Opnd* tauSrcNullChecked = builder->genTauCheckNull(src);

    // node
    builder->genFallthroughNode(dispatch);
    Opnd* tauDstNullChecked = builder->genTauCheckNull(dst);

    // node
    builder->genFallthroughNode();
    builder->genEdgeFromCurrent(BExcNode, 0);

    Opnd* tauNullCheckedRefArgs = builder->genTauAnd(tauSrcNullChecked, tauDstNullChecked);

    Type * intType = builder->getTypeManager().getInt32Type();
    Type::Tag intTag = intType->tag;

    Opnd * zero = builder->genLdConstant(0);
    builder->appendInst(instFactory.makeBranch(Cmp_GT,intTag,zero,srcPos,boundsException));

    // node
    builder->genFallthroughNode();
    builder->genEdgeFromCurrent(BExcNode, 0);
    builder->appendInst(instFactory.makeBranch(Cmp_GT,intTag,zero,dstPos,boundsException));

    // node
    builder->genFallthroughNode();
    builder->genEdgeFromCurrent(BExcNode, 0);
    builder->appendInst(instFactory.makeBranch(Cmp_GT,intTag,zero,len,boundsException));

    Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No);

    // node
    builder->genFallthroughNode();
    builder->genEdgeFromCurrent(BExcNode, 0);

    Opnd * srcLen = builder->genArrayLen(intType, intTag, src, tauSrcNullChecked);
    Opnd * srcEnd = builder->genAdd(intType,mod,srcPos,len);
    builder->appendInst(instFactory.makeBranch(Cmp_GT,intTag,srcEnd,srcLen,boundsException));
    
    // node
    builder->genFallthroughNode();
    builder->genEdgeFromCurrent(BExcNode, 0);

    Opnd * dstEnd = builder->genAdd(intType,mod,dstPos,len);
    Opnd * dstLen = builder->genArrayLen(intType, intTag, dst, tauDstNullChecked);
    builder->appendInst(instFactory.makeBranch(Cmp_GT,intTag,dstEnd,dstLen,boundsException));

    // node
    builder->genFallthroughNode();
    builder->genEdgeFromCurrent(exitNode, 0);

    // The case of same arrays and same positions
    Opnd * diff = builder->genCmp3(intType,intTag,Cmp_GT,dstPos,srcPos);
    Opnd * sameArrays = builder->genCmp(intType,Type::IntPtr,Cmp_EQ,src,dst);
    Opnd * zeroDiff = builder->genCmp(intType,intTag,Cmp_EQ,diff,zero);
    Opnd * nothingToCopy = builder->genAnd(intType,sameArrays,zeroDiff);
    builder->appendInst(instFactory.makeBranch(Cmp_NonZero,intTag,nothingToCopy,Exit));

    // node
    builder->genFallthroughNode();
    builder->genEdgeFromCurrent(RevCopyNode,0);

    Opnd* tauTypesChecked = builder->genTauSafe();

    // Choosing direction
    Opnd * dstIsGreater = builder->genCmp(intType,intTag,Cmp_GT,diff,zero);
    Opnd * reverseCopy = builder->genAnd(intType,sameArrays,dstIsGreater);
    builder->appendInst(instFactory.makeBranch(Cmp_GT,intTag,reverseCopy,zero,reverseCopying));

    // Direct Copying
    builder->genFallthroughNode();
    builder->genEdgeFromCurrent(exitNode,0);

    Opnd* directArgs [5];
    directArgs[0] = src;
    directArgs[1] = srcPos;
    directArgs[2] = dst;
    directArgs[3] = dstPos;
    directArgs[4] = len;
    builder->appendInst(instFactory.makeJitHelperCall(OpndManager::getNullOpnd(), ArrayCopyDirect,
                                                      tauNullCheckedRefArgs, tauTypesChecked,
                                                      5, directArgs));

    // Reverse Copying
    builder->setCurrentNode(RevCopyNode);
    builder->genEdgeFromCurrent(exitNode);

    Opnd* one = builder->genLdConstant(1);
    Opnd* lastSrcIdx = builder->genSub(srcPosType,mod,srcEnd,one);
    Opnd* lastDstIdx = builder->genSub(dstPosType,mod,dstEnd,one);

    Opnd* reverseArgs [5];
    reverseArgs[0] = src;
    reverseArgs[1] = lastSrcIdx;  // srcPos+len-1
    reverseArgs[2] = dst;
    reverseArgs[3] = lastDstIdx;  // dstPos+len-1
    reverseArgs[4] = len;
    builder->appendInst(instFactory.makeJitHelperCall(OpndManager::getNullOpnd(), ArrayCopyReverse,
                                                      tauNullCheckedRefArgs, tauTypesChecked,
                                                      5, reverseArgs));

    // Bounds Exception
    builder->setCurrentNode(BExcNode);
    builder->genEdgeFromCurrent(exitNode);
    builder->genEdgeFromCurrent(dispatch, 0);
    Opnd* minusone = builder->genLdConstant(-1);
    builder->genTauCheckBounds(src,minusone,tauSrcNullChecked);
}

void
String_compareTo_HLO_Handler::run()
{
    IRManager*          irm         = builder->getIRManager();
    InstFactory&        instFactory = builder->getInstFactory();
    ControlFlowGraph&   cfg         = builder->getControlFlowGraph();

    Node* firstNode = callInst->getNode();
    Node* lastNode = cfg.splitNodeAtInstruction(callInst, true, true, instFactory.makeLabel());
    Node* dispatch = firstNode->getExceptionEdgeTarget();
    assert(dispatch);
    callInst->unlink();
    cfg.removeEdge(firstNode->findEdge(true, lastNode));

    builder->setCurrentBCOffset(callInst->getBCOffset());
    
    // the fist two are tau operands
    Opnd* dst     = callInst->getDst();
    Opnd* thisStr = callInst->getSrc(2);
    Opnd* trgtStr = callInst->getSrc(3);
    
    Class_Handle string = (Class_Handle)VMInterface::getSystemStringVMTypeHandle();
    FieldDesc* fieldCountDesc = irm->getCompilationInterface().getFieldByName(string,"count");
    assert(fieldCountDesc);
    FieldDesc* fieldValueDesc = irm->getCompilationInterface().getFieldByName(string,"value");
    assert(fieldValueDesc);
    // this field is optional
    FieldDesc* offsetDesc = irm->getCompilationInterface().getFieldByName(string,"offset");
    Type* fieldType = fieldCountDesc->getFieldType();
    Type::Tag fieldTag = fieldType->tag;

    // gen at the end of first node
    builder->setCurrentNode(firstNode);
    Opnd *tauThisNullChecked = builder->genTauCheckNull(thisStr);

    // node
    builder->genFallthroughNode(dispatch);
    Opnd *tauThisInRange = builder->genTauHasType(thisStr, fieldCountDesc->getParentType());
    Opnd* thisLength = builder->genLdField(fieldCountDesc, thisStr, tauThisNullChecked, tauThisInRange);
    Opnd *tauTrgtNullChecked = builder->genTauCheckNull(trgtStr);

    // node
    Node* branch = builder->genFallthroughNode();
    Opnd *tauTrgtInRange = builder->genTauHasType(trgtStr, fieldCountDesc->getParentType());
    Opnd* trgtLength = builder->genLdField(fieldCountDesc, trgtStr, tauTrgtNullChecked, tauTrgtInRange);

    LabelInst * ThisIsLonger = (LabelInst*)instFactory.makeLabel();
    builder->appendInst(instFactory.makeBranch(Cmp_GT,fieldTag,thisLength,trgtLength,ThisIsLonger));

    VarOpnd* counterVar = builder->createVarOpnd(fieldType,false);

    // node (trgt is longer here)
    builder->genFallthroughNode();
    SsaVarOpnd* thisLengthVar = builder->createSsaVarOpnd(counterVar);
    builder->genStVar(thisLengthVar,thisLength);
    builder->genEdgeFromCurrent(lastNode); // jump to merge

    // node (this is longer here)
    builder->genNodeAfter(branch,ThisIsLonger);
    SsaVarOpnd* trgtLengthVar = builder->createSsaVarOpnd(counterVar);
    builder->genStVar(trgtLengthVar,trgtLength);
    builder->genEdgeFromCurrent(lastNode); // jump to merge

    // last node (merge after counter definition)
    builder->setCurrentNode(lastNode);
    // gather counter value
    Opnd* phiArgs[] = {thisLengthVar,trgtLengthVar};
    SsaVarOpnd* var = builder->createSsaVarOpnd(counterVar);
    lastNode->appendInst(instFactory.makePhi(var,2,phiArgs));
    Opnd* counter = builder->createOpnd(fieldType);
    lastNode->appendInst(instFactory.makeLdVar(counter,var));
    Opnd* thisStart = builder->genLdConstant(0);
    Opnd* trgtStart = builder->genLdConstant(0);
    if(offsetDesc) {
        Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No);
        Opnd* thisOffset = builder->genLdField(offsetDesc, thisStr, tauThisNullChecked, tauThisInRange);
        Opnd* trgtOffset = builder->genLdField(offsetDesc, trgtStr, tauTrgtNullChecked, tauTrgtInRange);
        thisStart = builder->genAdd(fieldType, mod, thisOffset, thisStart);
        trgtStart = builder->genAdd(fieldType, mod, trgtOffset, trgtStart);
    }
    Opnd* thisValue = builder->genLdField(fieldValueDesc, thisStr, tauThisNullChecked, tauThisInRange);
    Opnd* trgtValue = builder->genLdField(fieldValueDesc, trgtStr, tauTrgtNullChecked, tauTrgtInRange);
    Opnd* opnds[] = {thisValue,thisStart,thisLength,trgtValue,trgtStart,trgtLength,counter};

    // This helper call will be processed in Ia32ApiMagics pass
    builder->appendInst(instFactory.makeJitHelperCall(dst, StringCompareTo, NULL, NULL, 7, opnds));

    cfg.orderNodes(true);
}

void
String_regionMatches_HLO_Handler::run()
{
    IRManager*          irm         = builder->getIRManager();
    InstFactory&        instFactory = builder->getInstFactory();
    ControlFlowGraph&   cfg         = builder->getControlFlowGraph();

    Node* firstNode = callInst->getNode();
    Node* lastNode = cfg.splitNodeAtInstruction(callInst, true, true, instFactory.makeLabel());
    Node* dispatch = firstNode->getExceptionEdgeTarget();
    assert(dispatch);
    callInst->unlink();
    cfg.removeEdge(firstNode->findEdge(true, lastNode));

    builder->setCurrentBCOffset(callInst->getBCOffset());
    
    // the fist two are tau operands
    Opnd* dst       = callInst->getDst();
    Opnd* thisStr   = callInst->getSrc(2);
    Opnd* thisStart = callInst->getSrc(3);
    Opnd* trgtStr   = callInst->getSrc(4);
    Opnd* trgtStart = callInst->getSrc(5);
    Opnd* counter   = callInst->getSrc(6);

    Class_Handle string = (Class_Handle)VMInterface::getSystemStringVMTypeHandle();
    FieldDesc* fieldCountDesc = irm->getCompilationInterface().getFieldByName(string,"count");
    assert(fieldCountDesc);
    FieldDesc* fieldValueDesc = irm->getCompilationInterface().getFieldByName(string,"value");
    assert(fieldValueDesc);
    // this field is optional
    FieldDesc* offsetDesc = irm->getCompilationInterface().getFieldByName(string,"offset");

    Type* fieldType = fieldCountDesc->getFieldType();
    Type::Tag fieldTag = fieldType->tag;

    // gen at the end of first node
    builder->setCurrentNode(firstNode);
    Opnd *tauThisNullChecked = builder->genTauCheckNull(thisStr);

    // node
    builder->genFallthroughNode(dispatch);
    Opnd *tauThisInRange = builder->genTauHasType(thisStr, fieldCountDesc->getParentType());
    Opnd *tauTrgtNullChecked = builder->genTauCheckNull(trgtStr);

    LabelInst * FalseResult = (LabelInst*)instFactory.makeLabel();
    Node* returnFalse = cfg.createBlockNode(FalseResult);

    // node
    builder->genFallthroughNode();
    Opnd* zero = builder->genLdConstant(0);
    Opnd *tauTrgtInRange = builder->genTauHasType(trgtStr, fieldCountDesc->getParentType());
    builder->appendInst(instFactory.makeBranch(Cmp_GT,fieldTag,zero,trgtStart,FalseResult));
    builder->genEdgeFromCurrent(returnFalse);

    // node
    builder->genFallthroughNode();
    builder->appendInst(instFactory.makeBranch(Cmp_GT,fieldTag,zero,thisStart,FalseResult));
    builder->genEdgeFromCurrent(returnFalse);

    Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No);

    // node
    builder->genFallthroughNode();
    Opnd* trgtLength = builder->genLdField(fieldCountDesc, trgtStr, tauTrgtNullChecked, tauTrgtInRange);
    Opnd* trgtDiff = builder->genSub(fieldType, mod,trgtLength,trgtStart);
    builder->appendInst(instFactory.makeBranch(Cmp_GT,fieldTag,counter,trgtDiff,FalseResult));
    builder->genEdgeFromCurrent(returnFalse);

    // node
    builder->genFallthroughNode();
    Opnd* thisLength = builder->genLdField(fieldCountDesc, thisStr, tauThisNullChecked, tauThisInRange);
    Opnd* thisDiff = builder->genSub(fieldType, mod,thisLength,thisStart);
    builder->appendInst(instFactory.makeBranch(Cmp_GT,fieldTag,counter,thisDiff,FalseResult));
    builder->genEdgeFromCurrent(returnFalse);

    LabelInst * TrueResult = (LabelInst*)instFactory.makeLabel();
    Node* returnTrue = cfg.createBlockNode(TrueResult);

    // node
    builder->genFallthroughNode();
    builder->appendInst(instFactory.makeBranch(Cmp_GTE,fieldTag,zero,counter,TrueResult));
    builder->genEdgeFromCurrent(returnTrue);

    // node
    builder->genFallthroughNode();
    if(offsetDesc) {
        Opnd* thisOffset = builder->genLdField(offsetDesc, thisStr, tauThisNullChecked, tauThisInRange);
        Opnd* trgtOffset = builder->genLdField(offsetDesc, trgtStr, tauTrgtNullChecked, tauTrgtInRange);
        thisStart = builder->genAdd(fieldType, mod, thisOffset, thisStart);
        trgtStart = builder->genAdd(fieldType, mod, trgtOffset, trgtStart);
    }
    Opnd* thisValue = builder->genLdField(fieldValueDesc, thisStr, tauThisNullChecked, tauThisInRange);
    Opnd* trgtValue = builder->genLdField(fieldValueDesc, trgtStr, tauTrgtNullChecked, tauTrgtInRange);
    Opnd* opnds[] = {thisValue,thisStart,trgtValue,trgtStart,counter};

    // This helper call will be processed in Ia32ApiMagics pass
    VarOpnd* resultVar = builder->createVarOpnd(dst->getType(),false);
    SsaVarOpnd* resVar = builder->createSsaVarOpnd(resultVar);
    Opnd* res = builder->createOpnd(dst->getType());
    builder->appendInst(instFactory.makeJitHelperCall(res, StringRegionMatches, NULL, NULL, 5, opnds));
    builder->genStVar(resVar,res);
    builder->genEdgeFromCurrent(lastNode);

    // returnFalse
    builder->setCurrentNode(returnFalse);
    Opnd* resFalse  = builder->genLdConstant(0);
    SsaVarOpnd* resFalseVar = builder->createSsaVarOpnd(resultVar);
    builder->genStVar(resFalseVar,resFalse);
    builder->genEdgeFromCurrent(lastNode);

    // returnTrue
    builder->setCurrentNode(returnTrue);
    Opnd* resTrue  = builder->genLdConstant(1);
    SsaVarOpnd* resTrueVar = builder->createSsaVarOpnd(resultVar);
    builder->genStVar(resTrueVar,resTrue);
    builder->genEdgeFromCurrent(lastNode);

    // lastNode
    Opnd* phiArgs[] = {resVar,resFalseVar,resTrueVar};
    SsaVarOpnd* var = builder->createSsaVarOpnd(resultVar);
    lastNode->appendInst(instFactory.makePhi(var,3,phiArgs));
    lastNode->appendInst(instFactory.makeLdVar(dst,var));

    cfg.orderNodes(true);
}

void
String_indexOf_HLO_Handler::run()
{
    IRManager*          irm         = builder->getIRManager();
    InstFactory&        instFactory = builder->getInstFactory();
    ControlFlowGraph&   cfg         = builder->getControlFlowGraph();

    Node* firstNode = callInst->getNode();
    Node* lastNode = cfg.splitNodeAtInstruction(callInst, true, true, instFactory.makeLabel());
    Node* dispatch = firstNode->getExceptionEdgeTarget();
    assert(dispatch);
    callInst->unlink();
    cfg.removeEdge(firstNode->findEdge(true, lastNode));

    builder->setCurrentBCOffset(callInst->getBCOffset());
    
    // the fist two are tau operands
    Opnd* dst     = callInst->getDst();
    Opnd* thisStr = callInst->getSrc(2);
    Opnd* trgtStr = callInst->getSrc(3);
    Opnd* start = callInst->getSrc(4);
    
    Class_Handle string = (Class_Handle)VMInterface::getSystemStringVMTypeHandle();
    FieldDesc* fieldCountDesc = irm->getCompilationInterface().getFieldByName(string,"count");
    assert(fieldCountDesc);
    FieldDesc* fieldValueDesc = irm->getCompilationInterface().getFieldByName(string,"value");
    assert(fieldValueDesc);
    FieldDesc* offsetDesc = irm->getCompilationInterface().getFieldByName(string,"offset");
    assert(offsetDesc);

    // gen at the end of first node
    builder->setCurrentNode(firstNode);
    Opnd *tauThisNullChecked = builder->genTauCheckNull(thisStr);

    // node
    builder->genFallthroughNode(dispatch);
    Opnd *tauThisInRange = builder->genTauHasType(thisStr, fieldCountDesc->getParentType());

    Opnd *tauTrgtNullChecked = builder->genTauCheckNull(trgtStr);

    // node
    builder->genFallthroughNode();
    Opnd* imm128 = builder->genLdConstant(128);
    Opnd* imm64 = builder->genLdConstant(64);
    Opnd *tauTrgtInRange = builder->genTauHasType(trgtStr, fieldCountDesc->getParentType());

    // node
    builder->genFallthroughNode(dispatch);

    // prefetch String objects
    Opnd * voidDst = builder->createOpnd(irm->getTypeManager().getVoidType());
    Opnd* prefetchThis[] = {thisStr, imm128, imm64};
    builder->appendInst(instFactory.makeJitHelperCall(voidDst, Prefetch, NULL, NULL, 3, prefetchThis));

    // node
    builder->genFallthroughNode(dispatch);
    
    Opnd* prefetchTrgt[] = {trgtStr, imm128, imm64};
    builder->appendInst(instFactory.makeJitHelperCall(voidDst, Prefetch, NULL, NULL, 3, prefetchTrgt));

    Opnd* thisLength = builder->genLdField(fieldCountDesc, thisStr, tauThisNullChecked, tauThisInRange);
    Opnd* trgtLength = builder->genLdField(fieldCountDesc, trgtStr, tauTrgtNullChecked, tauTrgtInRange);
    
    Opnd* thisOffset = builder->genLdField(offsetDesc, thisStr, tauThisNullChecked, tauThisInRange);
    Opnd* trgtOffset = builder->genLdField(offsetDesc, trgtStr, tauTrgtNullChecked, tauTrgtInRange);

    Opnd* thisValue = builder->genLdField(fieldValueDesc, thisStr, tauThisNullChecked, tauThisInRange);
    Opnd* trgtValue = builder->genLdField(fieldValueDesc, trgtStr, tauTrgtNullChecked, tauTrgtInRange);

    // node
    builder->genFallthroughNode(dispatch);

    // prefetch character arrays
    Opnd* prefetchThisValue[] = {thisValue, imm128, imm64};
    builder->appendInst(instFactory.makeJitHelperCall(voidDst, Prefetch, NULL, NULL, 3, prefetchThisValue));

    // node
    builder->genFallthroughNode(dispatch);
    
    Opnd* prefetchTrgtValue[] = {trgtValue, imm128, imm64};
    builder->appendInst(instFactory.makeJitHelperCall(voidDst, Prefetch, NULL, NULL, 3, prefetchTrgtValue));

    // node
    builder->genFallthroughNode(dispatch);

    Opnd* opnds[] = {thisValue, thisOffset, thisLength, trgtValue, trgtOffset, trgtLength, start};
    // This helper call will be processed in Ia32ApiMagics pass
    builder->appendInst(instFactory.makeJitHelperCall(dst, StringIndexOf, NULL, NULL, 7, opnds));

    builder->genEdgeFromCurrent(lastNode);

    cfg.orderNodes(true);
}

void 
System_identityHashCode_Handler::run() {
    InstFactory& instFactory = builder->getInstFactory();

    // the fist two are tau operands
    Opnd* dst = callInst->getDst();
    Opnd* obj = callInst->getSrc(2);    
    // Opnd * opnds[] = { obj }; 

    Node* firstNode = callInst->getNode();   
    builder->setCurrentBCOffset(callInst->getBCOffset());
    builder->setCurrentNode(firstNode);    
    
    // builder->appendInst(instFactory.makeVMHelperCall(dst, VM_RT_GET_IDENTITY_HASHCODE, 1, opnds));    
    builder->appendInst(instFactory.makeIdentHC(dst, obj));        
    callInst->unlink();       
}

Node*
HLOAPIMagicIRBuilder::genNodeAfter(Node* srcNode, LabelInst* label, Node* dispatch) {
    currentNode = cfg.createBlockNode(label);
    cfg.addEdge(srcNode, currentNode);
    if (dispatch != NULL) {
        cfg.addEdge(currentNode, dispatch, 0.001);
    }
    return currentNode;
}

Node*
HLOAPIMagicIRBuilder::genNodeAfterCurrent(LabelInst* label, Node* dispatch) {
    return genNodeAfter(currentNode,label,dispatch);
}

Node*
HLOAPIMagicIRBuilder::genFallthroughNode(Node* dispatch) {
    return genNodeAfter(currentNode,instFactory.makeLabel(),dispatch);
}

void
HLOAPIMagicIRBuilder::appendInst(Inst* inst) {
    inst->setBCOffset(currentBCOffset);
    assert(currentNode);
    currentNode->appendInst(inst);
}

void
HLOAPIMagicIRBuilder::genCopy(Opnd* trgt, Opnd* src) {
    appendInst(instFactory.makeCopy(trgt,src));
}

Opnd*
HLOAPIMagicIRBuilder::genLdField(FieldDesc* fieldDesc, Opnd* base,
                                 Opnd* tauBaseNonNull, Opnd* tauAddressInRange) {
    Type* fieldType = fieldDesc->getFieldType();
    assert(fieldType);

    Opnd* fieldAddr;
    Modifier mod;

    if (compRefs) {
        // until VM type system is upgraded,
        // fieldDesc type will have uncompressed ref type;
        // compress it
        Type *compressedType = typeManager.compressType(fieldType);
        fieldAddr = createOpnd(typeManager.getManagedPtrType(compressedType));
        mod = AutoCompress_Yes;
    } else {
        fieldAddr = createOpnd(typeManager.getManagedPtrType(fieldType));
        mod = AutoCompress_No;
    }
    appendInst(instFactory.makeLdFieldAddr(fieldAddr, base, fieldDesc));

    Opnd* fieldVal = createOpnd(fieldType);
    appendInst(instFactory.makeTauLdInd(mod, fieldType->tag, fieldVal, fieldAddr, 
                                              tauBaseNonNull, tauAddressInRange));
    return fieldVal;
}

Opnd*
HLOAPIMagicIRBuilder::createOpnd(Type* type) {
    if (type->tag == Type::Void)
        return OpndManager::getNullOpnd();
    return opndManager.createSsaTmpOpnd(type);
}

VarOpnd*
HLOAPIMagicIRBuilder::createVarOpnd(Type* type, bool isPinned) {
    assert(type->tag != Type::Void);
    return opndManager.createVarOpnd(type,isPinned);
}

SsaVarOpnd*
HLOAPIMagicIRBuilder::createSsaVarOpnd(VarOpnd* var) {
    return opndManager.createSsaVarOpnd(var);
}

void
HLOAPIMagicIRBuilder::genStVar(SsaVarOpnd* var, Opnd* src) {
    appendInst(instFactory.makeStVar(var, src));
}

Opnd*
HLOAPIMagicIRBuilder::genTauCheckNull(Opnd* base)
{
    Opnd* dst = createOpnd(typeManager.getTauType());
    Inst* inst = instFactory.makeTauCheckNull(dst, base);
    appendInst(inst);
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genAnd(Type* dstType, Opnd* src1, Opnd* src2) {   
    Opnd* dst = createOpnd(dstType);
    appendInst(instFactory.makeAnd(dst, src1, src2));
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genTauAnd(Opnd *src1, Opnd *src2) {
    if (src1->getId() > src2->getId()) {
        Opnd *tmp = src1;
        src1 = src2;
        src2 = tmp;
    }
    Opnd* dst = createOpnd(typeManager.getTauType());
    Opnd* srcs[2] = { src1, src2 };
    appendInst(instFactory.makeTauAnd(dst, 2, srcs));

    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genAdd(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2) {    
    Opnd* dst = createOpnd(dstType);
    Inst *newi = instFactory.makeAdd(mod, dst, src1, src2);
    appendInst(newi);
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genSub(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2) {    
    Opnd* dst = createOpnd(dstType);
    Inst *newi = instFactory.makeSub(mod, dst, src1, src2);
    appendInst(newi);
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genLdConstant(I_32 val) {
    Opnd* dst = createOpnd(typeManager.getInt32Type());
    appendInst(instFactory.makeLdConst(dst, val));
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genArrayLen(Type* dstType, Type::Tag type, Opnd* array, Opnd* tauNonNull) {
    Opnd *tauIsArray = genTauHasType(array, array->getType());
    
    return genTauArrayLen(dstType, type, array, tauNonNull, tauIsArray);
}

Opnd*
HLOAPIMagicIRBuilder::genTauArrayLen(Type* dstType, Type::Tag type, Opnd* array,
                          Opnd* tauNullChecked, Opnd *tauTypeChecked) {
    Opnd* dst = createOpnd(dstType);
    appendInst(instFactory.makeTauArrayLen(dst, type, array, tauNullChecked,
                                           tauTypeChecked));
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genCmp3(Type* dstType,
                   Type::Tag instType, // source type for inst
                   ComparisonModifier mod,
                   Opnd* src1,
                   Opnd* src2) {
    // result of comparison is always a 32-bit int
    Opnd* dst = createOpnd(dstType);
    Inst* i = instFactory.makeCmp3(mod, instType, dst, src1, src2);
    appendInst(i);
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genCmp(Type* dstType,
                  Type::Tag instType, // source type for inst
                  ComparisonModifier mod,
                  Opnd* src1,
                  Opnd* src2) {
    // result of comparison is always a 32-bit int
    Opnd* dst = createOpnd(dstType);
    Inst *i = instFactory.makeCmp(mod, instType, dst, src1, src2);
    appendInst(i);
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genTauSafe() {
    Opnd* dst = createOpnd(typeManager.getTauType());
    appendInst(instFactory.makeTauSafe(dst));
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genTauCheckBounds(Opnd* array, Opnd* index, Opnd *tauNullChecked) {
    Opnd* tauArrayTypeChecked = genTauHasType(array, array->getType());
    Opnd* arrayLen = genTauArrayLen(typeManager.getInt32Type(), Type::Int32, array, 
                                    tauNullChecked, tauArrayTypeChecked);

    Opnd* dst = genTauCheckBounds(arrayLen, index);
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genTauCheckBounds(Opnd* ub, Opnd *index) {
    Opnd* dst = createOpnd(typeManager.getTauType());
    appendInst(instFactory.makeTauCheckBounds(dst, ub, index));
    return dst;
}

Opnd*
HLOAPIMagicIRBuilder::genTauHasType(Opnd *src, Type *castType) {
    Opnd* dst = createOpnd(typeManager.getTauType());
    appendInst(instFactory.makeTauHasType(dst, src, castType));
    return dst;
}

} //namespace Jitrino
