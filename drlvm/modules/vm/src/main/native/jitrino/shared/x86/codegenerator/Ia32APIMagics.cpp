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
 * @author Intel, Mikhail Y. Fursov
 */

#include "Ia32Inst.h"
#include "Ia32IRManager.h"


//#define ENABLE_GC_RT_CHECKS

namespace Jitrino {
namespace  Ia32 {

class APIMagicsHandlerSession: public SessionAction {
    void runImpl();
    U_32 getNeedInfo()const{ return 0; }
    U_32 getSideEffects()const{ return 0; }
    bool isIRDumpEnabled(){ return true; }
};

static ActionFactory<APIMagicsHandlerSession> _api_magics("api_magic");

static Opnd* getCallDst(CallInst* callInst) {
    Inst::Opnds defs(callInst, Inst::OpndRole_InstLevel | Inst::OpndRole_Def | Inst::OpndRole_Explicit);
    U_32 idx = defs.begin();
    return callInst->getOpnd(idx);
}
static Opnd* getCallSrc(CallInst* callInst, U_32 n) {
    Inst::Opnds uses(callInst, Inst::OpndRole_InstLevel | Inst::OpndRole_Use | Inst::OpndRole_Explicit);
    U_32 idx  = uses.begin(); //the first use is call addr
    for (U_32 i=0; i<=n; i++) {
        idx = uses.next(idx);
    }
    return  callInst->getOpnd(idx);
}

class APIMagicHandler {
public:
    APIMagicHandler(IRManager* _irm, CallInst* _inst, MethodDesc* _md)
    : irm(_irm), callInst(_inst), md(_md), typeManager(irm->getTypeManager()) {
        cfg = irm->getFlowGraph();
    }
    virtual ~APIMagicHandler(){};

    virtual void run()=0;
protected:

    void   convertIntToInt(Opnd* dst, Opnd* src, Node* node);
    Opnd*  addElemIndexWithLEA(Opnd* array, Opnd* index, RegName dstRegName, Node* node);
    Opnd*   getOpnd(Opnd* arg);

    IRManager* irm;
    CallInst* callInst;
    MethodDesc*  md;
    ControlFlowGraph* cfg;
    TypeManager& typeManager;
};

#define DECLARE_HELPER_INLINER(name)\
class name : public APIMagicHandler {\
public:\
    name (IRManager* irm, CallInst* inst, MethodDesc* md)\
    : APIMagicHandler(irm, inst, md){}\
    \
    virtual void run();\
};\

enum Math_function {SIN, COS, TAN, ASIN, ACOS, ATAN, ATAN2, LOG, LOG10, LOG1P, ABS, SQRT};\
 
#define DECLARE_HELPER_INLINER_MATH(name)\
class name : public APIMagicHandler {\
public:\
    enum Math_function func;\
    Mnemonic mnemonic;\
    name (IRManager* irm, CallInst* inst, MethodDesc* md, enum Math_function f, Mnemonic mn = Mnemonic_NULL)\
    : APIMagicHandler(irm, inst, md){name::func = f; name::mnemonic = mn;}\
    \
    virtual void run();\
}\


DECLARE_HELPER_INLINER(Integer_numberOfLeadingZeros_Handler_x_I_x_I);
DECLARE_HELPER_INLINER(Integer_numberOfTrailingZeros_Handler_x_I_x_I);
DECLARE_HELPER_INLINER(Long_numberOfLeadingZeros_Handler_x_J_x_I);
DECLARE_HELPER_INLINER(Long_numberOfTrailingZeros_Handler_x_J_x_I);

DECLARE_HELPER_INLINER_MATH(Math_Handler_x_D_x_D);

DECLARE_HELPER_INLINER(System_arraycopyDirect_Handler);
DECLARE_HELPER_INLINER(System_arraycopyReverse_Handler);
DECLARE_HELPER_INLINER(String_compareTo_Handler_x_String_x_I);
DECLARE_HELPER_INLINER(String_regionMatches_Handler_x_I_x_String_x_I_x_I_x_Z);
DECLARE_HELPER_INLINER(String_indexOf_Handler_x_String_x_I_x_I);
DECLARE_HELPER_INLINER(Float_floatToRawIntBits_x_F_x_I);
DECLARE_HELPER_INLINER(Float_intBitsToFloat_x_I_x_F);

void APIMagicsHandlerSession::runImpl() {
    CompilationContext* cc = getCompilationContext();
    MemoryManager tmpMM("Inline API methods");
#ifndef _EM64T_
    bool mathAsMagic = getBoolArg("magic_math", true);
#endif
    //finding all api magic calls
    IRManager* irm = cc->getLIRManager();
    ControlFlowGraph* fg = irm->getFlowGraph();
    StlVector<APIMagicHandler*> handlers(tmpMM);
    const Nodes& nodes = fg->getNodesPostOrder();//process checking only reachable nodes.
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode()) {
            for (Inst* inst = (Inst*)node->getFirstInst(); inst!=NULL; inst = inst->getNextInst()) {
                if (!inst->hasKind(Inst::Kind_CallInst)) {
                    continue;
                }
                if ( ((CallInst*)inst)->isDirect() ) {
                    CallInst* callInst = (CallInst*)inst;
                    Opnd * targetOpnd=callInst->getOpnd(callInst->getTargetOpndIndex());
                    assert(targetOpnd->isPlacedIn(OpndKind_Imm));
                    Opnd::RuntimeInfo * ri=targetOpnd->getRuntimeInfo();
                    if( !ri ) { 
                        continue; 
                    };
                    if( ri->getKind() == Opnd::RuntimeInfo::Kind_MethodDirectAddr ){
#ifndef _EM64T_
                        MethodDesc * md = (MethodDesc*)ri->getValue(0);
                        const char* className = md->getParentType()->getName();
                        const char* methodName = md->getName();
                        const char* signature = md->getSignatureString();
                        if (!strcmp(className, "java/lang/Integer")) {
                            if (!strcmp(methodName, "numberOfLeadingZeros") && !strcmp(signature, "(I)I")) {
                                handlers.push_back(new (tmpMM) Integer_numberOfLeadingZeros_Handler_x_I_x_I(irm, callInst, md));
                            } else if (!strcmp(methodName, "numberOfTrailingZeros") && !strcmp(signature, "(I)I")) {
                                handlers.push_back(new (tmpMM) Integer_numberOfTrailingZeros_Handler_x_I_x_I(irm, callInst, md));
                            }
                        } else if (!strcmp(className, "java/lang/Long")) {
                            if (!strcmp(methodName, "numberOfLeadingZeros") && !strcmp(signature, "(J)I")) {
                                handlers.push_back(new (tmpMM) Long_numberOfLeadingZeros_Handler_x_J_x_I(irm, callInst, md));
                            } else if (!strcmp(methodName, "numberOfTrailingZeros") && !strcmp(signature, "(J)I")) {
                                handlers.push_back(new (tmpMM) Long_numberOfTrailingZeros_Handler_x_J_x_I(irm, callInst, md));
                            }
                        } else if (!strcmp(className, "java/lang/Float")) {
                            if (!strcmp(methodName, "floatToRawIntBits") && !strcmp(signature, "(F)I")) {
                                handlers.push_back(new (tmpMM) Float_floatToRawIntBits_x_F_x_I(irm, callInst, md));
                            } else if (!strcmp(methodName, "intBitsToFloat") && !strcmp(signature, "(I)F")) {
                                handlers.push_back(new (tmpMM) Float_intBitsToFloat_x_I_x_F(irm, callInst, md));
                            }
                        } else if (mathAsMagic && !strcmp(className, "java/lang/Math")) {
                            if (!strcmp(signature, "(D)D")) { 
                                if (!strcmp(methodName, "sqrt")) {                                   
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, SQRT, Mnemonic_FSQRT)); 
                                } 
                                if (!strcmp(methodName, "sin")) {                                    
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, SIN, Mnemonic_FSIN)); 
                                }      
                                if (!strcmp(methodName, "cos")) {                                    
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, COS, Mnemonic_FCOS)); 
                                } 
                                if (!strcmp(methodName, "abs")) {                                    
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, ABS, Mnemonic_FABS)); 
                                }
                                if (!strcmp(methodName, "tan")) {                                    
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, TAN, Mnemonic_FPTAN)); 
                                } 
                                if (!strcmp(methodName, "log")) {                                    
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, LOG, Mnemonic_FLDLN2)); 
                                }       
                                if (!strcmp(methodName, "log10")) {                                   
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, LOG10, Mnemonic_FLDLG2)); 
                                }
                                if (!strcmp(methodName, "log1p")) {                                   
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, LOG1P, Mnemonic_FLDLN2)); 
                                }       
                                if (!strcmp(methodName, "atan")) {                                    
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, ATAN)); 
                                }
                                if (!strcmp(methodName, "asin")) {                                    
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, ASIN)); 
                                }
                                if (!strcmp(methodName, "acos")) {                                    
                                       handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, ACOS)); 
                                }
                            } else if (!strcmp(signature, "(F)F") && !strcmp(methodName, "abs")) {
                                handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, ABS, Mnemonic_FABS));
                            } else if (!strcmp(signature, "(DD)D") && !strcmp(methodName, "atan2")) {
                                handlers.push_back(new (tmpMM) Math_Handler_x_D_x_D(irm, callInst, md, ATAN2)); 
                            }
                        }
#endif
                    } else if( ri->getKind() == Opnd::RuntimeInfo::Kind_InternalHelperAddress ) {
                        if( strcmp((char*)ri->getValue(0),"memory_copy_direct")==0 ) {
                            if(getBoolArg("System_arraycopy_as_magic", true)) {
                                handlers.push_back(new (tmpMM) System_arraycopyDirect_Handler(irm, callInst, NULL));
                            } else { assert(0); }
                        } else if( strcmp((char*)ri->getValue(0),"memory_copy_reverse")==0 ) {
                            if(getBoolArg("System_arraycopy_as_magic", true)) {
                                handlers.push_back(new (tmpMM) System_arraycopyReverse_Handler(irm, callInst, NULL));
                            } else { assert(0); }
                        } else if( strcmp((char*)ri->getValue(0),"String_compareTo")==0 ) {
                            if(getBoolArg("String_compareTo_as_magic", true))
                                handlers.push_back(new (tmpMM) String_compareTo_Handler_x_String_x_I(irm, callInst, NULL));
                        } else if( strcmp((char*)ri->getValue(0),"String_regionMatches")==0 ) {
                            if(getBoolArg("String_regionMatches_as_magic", true))
                                handlers.push_back(new (tmpMM) String_regionMatches_Handler_x_I_x_String_x_I_x_I_x_Z(irm, callInst, NULL));
                        } else if( strcmp((char*)ri->getValue(0),"String_indexOf")==0 ) {
                            if(getBoolArg("String_indexOf_as_magic", true))
                                handlers.push_back(new (tmpMM) String_indexOf_Handler_x_String_x_I_x_I(irm, callInst, NULL));
                        }
                    }
                }
            }
        }
    }

    //running all handlers
    for (StlVector<APIMagicHandler*>::const_iterator it = handlers.begin(), end = handlers.end(); it!=end; ++it) {
        APIMagicHandler* handler = *it;
        handler->run();
    }
    if (handlers.size() > 0) {
        irm->invalidateLivenessInfo();
    }
}


void Integer_numberOfLeadingZeros_Handler_x_I_x_I::run() {
    //mov r2,-1
    //bsr r1,arg
    //cmovz r1,r2
    //return 31 - r1;
    Type * i32Type =irm->getTypeFromTag(Type::Int32);
    Opnd* r1 = irm->newOpnd(i32Type);
    Opnd* r2 = irm->newOpnd(i32Type);
    Opnd* arg = getCallSrc(callInst, 0);
    Opnd* res = getCallDst(callInst);

    
    irm->newCopyPseudoInst(Mnemonic_MOV, r2, irm->newImmOpnd(i32Type, -1))->insertBefore(callInst);
    irm->newInstEx(Mnemonic_BSR, 1, r1, arg)->insertBefore(callInst);
    irm->newInstEx(Mnemonic_CMOVZ, 1, r1, r1, r2)->insertBefore(callInst);
    irm->newInstEx(Mnemonic_SUB, 1, res, irm->newImmOpnd(i32Type, 31), r1)->insertBefore(callInst);

    callInst->unlink();
}

void Float_floatToRawIntBits_x_F_x_I::run() {

    Opnd* arg = getCallSrc(callInst, 0);
    Opnd* res = getCallDst(callInst);

    irm->newCopyPseudoInst(Mnemonic_MOV, res, arg)->insertBefore(callInst);
    callInst->unlink();
}

void Float_intBitsToFloat_x_I_x_F::run() {

    Opnd* arg = getCallSrc(callInst, 0);
    Opnd* res = getCallDst(callInst);

    irm->newCopyPseudoInst(Mnemonic_MOV, res, arg)->insertBefore(callInst);
    callInst->unlink();
}

void Math_Handler_x_D_x_D::run() {
     Opnd* arg = getCallSrc(callInst, 0);
     Opnd* res = getCallDst(callInst);
     
     Opnd* fp0 = getOpnd(arg);        
     Node* node = callInst->getNode();
     switch (func) {
         case SQRT: case SIN: case COS: case ABS: case TAN:  
             node->appendInst(irm->newInst(Mnemonic_FLD, fp0, arg));                
             node->appendInst(irm->newInst(mnemonic, fp0));             
             if (func == TAN) {
                 node->appendInst(irm->newInst(Mnemonic_FSTP, res, fp0));    
             }                                                                        
             break;
         case LOG: case LOG10: case LOG1P:
             node->appendInst(irm->newInst(mnemonic, fp0));
             node->appendInst(irm->newInst(Mnemonic_FLD, fp0, arg));
             if (func == LOG1P) {
                 node->appendInst(irm->newInst(Mnemonic_FLD1, fp0));
                 node->appendInst(irm->newInst(Mnemonic_FADDP, fp0));          
             }                 
             node->appendInst(irm->newInst(Mnemonic_FYL2X, fp0));        
             break;
         case ATAN:
             node->appendInst(irm->newInst(Mnemonic_FLD, fp0, arg));
             node->appendInst(irm->newInst(Mnemonic_FLD1, fp0));
             node->appendInst(irm->newInst(Mnemonic_FPATAN, fp0));
             break;
         case ATAN2:
             node->appendInst(irm->newInst(Mnemonic_FLD, fp0, arg));
             node->appendInst(irm->newInst(Mnemonic_FLD, fp0, getCallSrc(callInst, 1)));
             node->appendInst(irm->newInst(Mnemonic_FPATAN, fp0));        
             break;
         case ASIN: case ACOS:
             node->appendInst(irm->newInst(Mnemonic_FLD, fp0, arg));         
             node->appendInst(irm->newInst(Mnemonic_FLD1, fp0));                 
             node->appendInst(irm->newInst(Mnemonic_FLD, fp0, arg));
             node->appendInst(irm->newInst(Mnemonic_FLD, fp0, arg));                     
             node->appendInst(irm->newInst(Mnemonic_FMULP, fp0));        
             node->appendInst(irm->newInst(Mnemonic_FSUBP, fp0));         
             node->appendInst(irm->newInst(Mnemonic_FSQRT, fp0)); 
             if (func == ACOS) {
                 node->appendInst(irm->newInst(Mnemonic_FXCH, fp0));     
             }
             node->appendInst(irm->newInst(Mnemonic_FPATAN, fp0));        
             break;
         default: assert(0);
     } 
     node->appendInst(irm->newInst(Mnemonic_FSTP, res, fp0));
     callInst->unlink();
}                                      


Opnd* APIMagicHandler::getOpnd(Opnd* arg) {
     if (arg->getSize() == OpndSize_64) {
         Opnd * fp0Op64 = irm->newOpnd(irm->getTypeManager().getDoubleType(), RegName_FP0D);        
         fp0Op64->assignRegName(RegName_FP0D);
         return fp0Op64;
     } else {
         Opnd * fp0Op32 = irm->newOpnd(irm->getTypeManager().getSingleType(), RegName_FP0S);        
         fp0Op32->assignRegName(RegName_FP0S);        
         return fp0Op32;
        
     }
}                                                                                                                

void Integer_numberOfTrailingZeros_Handler_x_I_x_I::run() {
    //mov r2,32
    //bsf r1,arg
    //cmovz r1,r2
    //return r1
    Type * i32Type =irm->getTypeFromTag(Type::Int32);
    Opnd* r1 = irm->newOpnd(i32Type);
    Opnd* r2 = irm->newOpnd(i32Type);
    Opnd* arg = getCallSrc(callInst, 0);
    Opnd* res = getCallDst(callInst);

    irm->newCopyPseudoInst(Mnemonic_MOV, r2, irm->newImmOpnd(i32Type, 32))->insertBefore(callInst);
    irm->newInstEx(Mnemonic_BSF, 1, r1, arg)->insertBefore(callInst);
    irm->newInstEx(Mnemonic_CMOVZ, 1, r1, r1, r2)->insertBefore(callInst);
    irm->newCopyPseudoInst(Mnemonic_MOV, res, r1)->insertBefore(callInst);

    callInst->unlink();
}

void Long_numberOfLeadingZeros_Handler_x_J_x_I::run() {
#ifdef _EM64T_
    return;
#else
//  bsr r1,hi
//  jz high_part_is_zero 
//high_part_is_not_zero:
//  return 31-r1
//high_part_is_zero:
//  mov r2,-1
//  bsr r1,lw
//  cmovz r1, r2
//  return 63 - r1;

    
    Type * i32Type =irm->getTypeFromTag(Type::Int32);
    Opnd* r1 = irm->newOpnd(i32Type);
    Opnd* r2 = irm->newOpnd(i32Type);
    Opnd* lwOpnd = getCallSrc(callInst, 0);
    Opnd* hiOpnd = getCallSrc(callInst, 1);
    Opnd* res = getCallDst(callInst);
    
    if (callInst!=callInst->getNode()->getLastInst()) {
        cfg->splitNodeAtInstruction(callInst, true, true, NULL);
    }
    Node* node = callInst->getNode();
    Node* nextNode = node->getUnconditionalEdgeTarget();
    assert(nextNode!=NULL);
    cfg->removeEdge(node->getUnconditionalEdge());
    callInst->unlink();

    Node* hiZeroNode = cfg->createBlockNode();
    Node* hiNotZeroNode = cfg->createBlockNode();
    
    //node
    node->appendInst(irm->newInstEx(Mnemonic_BSR, 1, r1, hiOpnd));
    node->appendInst(irm->newBranchInst(Mnemonic_JZ, hiZeroNode, hiNotZeroNode));
    
    
    //high_part_is_not_zero
    hiNotZeroNode->appendInst(irm->newInstEx(Mnemonic_SUB, 1, res, irm->newImmOpnd(i32Type, 31), r1));
    
    //high_part_is_zero
    hiZeroNode->appendInst(irm->newCopyPseudoInst(Mnemonic_MOV, r2, irm->newImmOpnd(i32Type, -1)));
    hiZeroNode->appendInst(irm->newInstEx(Mnemonic_BSR, 1, r1, lwOpnd));
    hiZeroNode->appendInst(irm->newInstEx(Mnemonic_CMOVZ, 1, r1, r1, r2));
    hiZeroNode->appendInst(irm->newInstEx(Mnemonic_SUB, 1, res, irm->newImmOpnd(i32Type, 63), r1));


    cfg->addEdge(node, hiZeroNode, 0.3);
    cfg->addEdge(node, hiNotZeroNode, 0.7);
    cfg->addEdge(hiZeroNode, nextNode);
    cfg->addEdge(hiNotZeroNode, nextNode);

#endif
}

void Long_numberOfTrailingZeros_Handler_x_J_x_I::run() {
#ifdef _EM64T_
    return;
#else

//    bsf r1,lw
//    jz low_part_is_zero 
//low_part_is_not_zero:
//    return r1;
//low_part_is_zero:
//    bsf r1,hi
//    jz zero
//not_zero;
//    return 32 + r1;
//zero:
//    return 64;

    Type * i32Type =irm->getTypeFromTag(Type::Int32);
    Opnd* r1 = irm->newOpnd(i32Type);
    Opnd* lwOpnd = getCallSrc(callInst, 0);
    Opnd* hiOpnd = getCallSrc(callInst, 1);
    Opnd* res = getCallDst(callInst);

    if (callInst!=callInst->getNode()->getLastInst()) {
        cfg->splitNodeAtInstruction(callInst, true, true, NULL);
    }
    Node* node = callInst->getNode();
    Node* nextNode = node->getUnconditionalEdgeTarget();
    assert(nextNode!=NULL);
    cfg->removeEdge(node->getUnconditionalEdge());
    callInst->unlink();

    Node* lowZeroNode = cfg->createBlockNode();
    Node* lowNotZeroNode = cfg->createBlockNode();
    Node* notZeroNode = cfg->createBlockNode();
    Node* zeroNode = cfg->createBlockNode();
    
    //node:
    node->appendInst(irm->newInstEx(Mnemonic_BSF, 1, r1, lwOpnd));
    node->appendInst(irm->newBranchInst(Mnemonic_JZ, lowZeroNode, lowNotZeroNode));

    //low_part_is_not_zero:
    lowNotZeroNode->appendInst(irm->newCopyPseudoInst(Mnemonic_MOV, res, r1));

    //low_part_is_zero:
    lowZeroNode->appendInst(irm->newInstEx(Mnemonic_BSF, 1, r1, hiOpnd)); 
    lowZeroNode->appendInst(irm->newBranchInst(Mnemonic_JZ, zeroNode, notZeroNode));    

    //not zero:
    notZeroNode->appendInst(irm->newInstEx(Mnemonic_ADD, 1, res, r1, irm->newImmOpnd(i32Type, 32)));

    //zero:
    zeroNode->appendInst(irm->newCopyPseudoInst(Mnemonic_MOV, res, irm->newImmOpnd(i32Type, 64)));

    cfg->addEdge(node, lowNotZeroNode, 0.7);
    cfg->addEdge(node, lowZeroNode, 0.3);
    cfg->addEdge(lowNotZeroNode, nextNode);
    cfg->addEdge(lowZeroNode, zeroNode, 0.1);
    cfg->addEdge(lowZeroNode, notZeroNode, 0.9);
    cfg->addEdge(notZeroNode, nextNode);
    cfg->addEdge(zeroNode, nextNode);

#endif
}

void System_arraycopyDirect_Handler::run()
{
    Node* currNode = callInst->getNode();
    if (callInst!=currNode->getLastInst()) {
        cfg->splitNodeAtInstruction(callInst, true, true, NULL);
    }
    UNUSED Node* nextNode = currNode->getUnconditionalEdgeTarget();
    assert(nextNode!=NULL);
    assert( currNode->getOutEdge(Edge::Kind_Dispatch) == NULL);

    callInst->unlink();

#ifdef _EM64T_
    RegName counterRegName = RegName_RCX;
    RegName srcAddrRegName = RegName_RSI;
    RegName dstAddrRegName = RegName_RDI;
#else
    RegName counterRegName = RegName_ECX;
    RegName srcAddrRegName = RegName_ESI;
    RegName dstAddrRegName = RegName_EDI;
#endif
    
    Opnd* src = getCallSrc(callInst, 0);
    Opnd* srcPos = getCallSrc(callInst, 1);
    Opnd* dst = getCallSrc(callInst, 2);
    Opnd* dstPos = getCallSrc(callInst, 3);
    Opnd* counterVal = getCallSrc(callInst, 4);

    // prepare counter
    Opnd* counter = irm->newRegOpnd(typeManager.getIntPtrType(),counterRegName);
    convertIntToInt(counter, counterVal, currNode);

    // prepare src/dst positions
    Opnd* srcAddr = addElemIndexWithLEA(src,srcPos,srcAddrRegName,currNode);
    Opnd* dstAddr = addElemIndexWithLEA(dst,dstPos,dstAddrRegName,currNode);

    Opnd* one = irm->newImmOpnd(typeManager.getInt8Type(), 1);

    Mnemonic mn = Mnemonic_NULL;
    Type* elemType = src->getType()->asArrayType()->getElementType();
    OpndSize typeSize = IRManager::getTypeSize(elemType->tag);
    switch(typeSize) {
        case OpndSize_8:   mn = Mnemonic_MOVS8; break;
        case OpndSize_16:  mn = Mnemonic_MOVS16; break;
        case OpndSize_32:  mn = Mnemonic_MOVS32; break;
        case OpndSize_64:
            {
                /**
                 * FIXME 
                 * Currently JIT erroneously supposes that compressed mode is always on.
                 * So if type is object, it is actually compressed (32-bit sized).
                 * But IRManager::getTypeSize() "correctly" returns OpndSize_64.
                 */
                if (!elemType->isObject()) {
                    currNode->appendInst(irm->newInstEx(Mnemonic_SHL, 1, counter, counter, one));
                }
                mn = Mnemonic_MOVS32;
            }
            break;
        default: assert(0); mn = Mnemonic_MOVS32; break;
    }

    Inst* copyInst = irm->newInst(mn,dstAddr,srcAddr,counter);
    copyInst->setPrefix(InstPrefix_REP);
    currNode->appendInst(copyInst);
}

void System_arraycopyReverse_Handler::run()
{
    Node* currNode = callInst->getNode();
    irm->newInst(Mnemonic_PUSHFD)->insertBefore(callInst);
    irm->newInst(Mnemonic_STD)->insertBefore(callInst);
    System_arraycopyDirect_Handler directHandler(irm, callInst, NULL);
    directHandler.run();

    currNode->appendInst(irm->newInst(Mnemonic_POPFD));
}

void String_compareTo_Handler_x_String_x_I::run() {
    //mov ds:esi, this
    //mov es:edi, src
    //mov ecx, min(this.count, src.count)
    //repne cmpw
    //if ZF == 0 (one of strings is a prefix)
    //  return this.count - src.count
    //else
    //  return [ds:esi-2] - [es:edi-2]

    Node* callInstNode = callInst->getNode();
    Node* nextNode = callInstNode->getUnconditionalEdgeTarget();
    assert(nextNode!=NULL);
    cfg->removeEdge(callInstNode->getUnconditionalEdge());

    // arguments of the call are already prepared by respective HLO pass
    // they are not the strings but 'value' arrays
    Opnd* thisArr = getCallSrc(callInst, 0);
    Opnd* thisIdx = getCallSrc(callInst, 1);
    Opnd* thisLen = getCallSrc(callInst, 2);
    Opnd* trgtArr = getCallSrc(callInst, 3);
    Opnd* trgtIdx = getCallSrc(callInst, 4);
    Opnd* trgtLen = getCallSrc(callInst, 5);
    Opnd* valForCounter = getCallSrc(callInst, 6);
    Opnd* res = getCallDst(callInst);

#ifdef _EM64T_
    RegName counterRegName = RegName_RCX;
    RegName thisAddrRegName = RegName_RSI;
    RegName trgtAddrRegName = RegName_RDI;
#else
    RegName counterRegName = RegName_ECX;
    RegName thisAddrRegName = RegName_ESI;
    RegName trgtAddrRegName = RegName_EDI;
#endif
    Type*   counterType = typeManager.getIntPtrType();

    Node* counterIsZeroNode = irm->getFlowGraph()->createBlockNode();
    // if counter is zero jump to counterIsZeroNode immediately
    callInstNode->appendInst(irm->newInst(Mnemonic_TEST, valForCounter, valForCounter));
    BranchInst* br = irm->newBranchInst(Mnemonic_JZ, NULL, NULL);
    callInstNode->appendInst(br);
    Node* node = irm->getFlowGraph()->createBlockNode();
    br->setTrueTarget(counterIsZeroNode);
    br->setFalseTarget(node);
    irm->getFlowGraph()->addEdge(counterIsZeroNode, nextNode, 1);
    irm->getFlowGraph()->addEdge(callInstNode, counterIsZeroNode, 0.05);
    irm->getFlowGraph()->addEdge(callInstNode, node, 0.95);

    // prepare counter
    Opnd* counter = irm->newRegOpnd(counterType,counterRegName);
    convertIntToInt(counter, valForCounter, node);

    // prepare this/trgt positions
    Opnd* thisAddrReg = addElemIndexWithLEA(thisArr,thisIdx,thisAddrRegName,node);
    Opnd* trgtAddrReg = addElemIndexWithLEA(trgtArr,trgtIdx,trgtAddrRegName,node);

    Inst* compareInst = irm->newInst(Mnemonic_CMPSW,thisAddrReg,trgtAddrReg,counter);
    compareInst->setPrefix(InstPrefix_REPZ);
    node->appendInst(compareInst);

    // counter is 0 means the same as last comparison leaves zero at ZF
    br = irm->newBranchInst(Mnemonic_JZ, NULL, NULL);
    node->appendInst(br);

    Node* differentStringsNode = irm->getFlowGraph()->createBlockNode();
    br->setTrueTarget(counterIsZeroNode);
    br->setFalseTarget(differentStringsNode);
    irm->getFlowGraph()->addEdge(node, counterIsZeroNode, 0.5);
    irm->getFlowGraph()->addEdge(node, differentStringsNode, 0.5);
    irm->getFlowGraph()->addEdge(differentStringsNode, nextNode, 1);

    // counter is zero
    counterIsZeroNode->appendInst(irm->newInstEx(Mnemonic_SUB, 1, res, thisLen, trgtLen));

    // strings are different
    Opnd* minustwo = irm->newImmOpnd(counterType,-2);
    Type* charType = typeManager.getCharType();
    Opnd* thisChar = irm->newMemOpnd(charType, thisAddrReg, NULL, NULL, minustwo);
    Opnd* trgtChar = irm->newMemOpnd(charType, trgtAddrReg, NULL, NULL, minustwo);
    Type* intType = res->getType();
    Opnd* thisInt = irm->newOpnd(intType);
    Opnd* trgtInt = irm->newOpnd(intType);
    differentStringsNode->appendInst(irm->newInstEx(Mnemonic_MOVZX, 1, thisInt, thisChar));
    differentStringsNode->appendInst(irm->newInstEx(Mnemonic_MOVZX, 1, trgtInt, trgtChar));
    differentStringsNode->appendInst(irm->newInstEx(Mnemonic_SUB, 1, res, thisInt, trgtInt));

    callInst->unlink();
}

void String_regionMatches_Handler_x_I_x_String_x_I_x_I_x_Z::run() {
    //mov ds:esi, this
    //mov es:edi, src
    //mov ecx, counter
    //repne cmpw
    //if ZF == 0 (one of strings is a prefix)
    //  return this.count - src.count
    //else
    //  return [ds:esi-2] - [es:edi-2]

    Node* node = callInst->getNode();
    Node* nextNode = NULL;

    if(callInst == node->getLastInst()) {
        nextNode = node->getUnconditionalEdgeTarget();
        assert(nextNode!=NULL);
    } else {
        nextNode = irm->getFlowGraph()->splitNodeAtInstruction(callInst, true, true, NULL);
    }
    cfg->removeEdge(node->getUnconditionalEdge());

    // arguments of the call are already prepared by respective HLO pass
    // they are not the strings but 'value' arrays
    Opnd* thisArr = getCallSrc(callInst, 0);
    Opnd* thisIdx = getCallSrc(callInst, 1);
    Opnd* trgtArr = getCallSrc(callInst, 2);
    Opnd* trgtIdx = getCallSrc(callInst, 3);
    Opnd* valForCounter = getCallSrc(callInst, 4);
    Opnd* res = getCallDst(callInst);

#ifdef _EM64T_
    RegName counterRegName = RegName_RCX;
    RegName thisAddrRegName = RegName_RSI;
    RegName trgtAddrRegName = RegName_RDI;
#else
    RegName counterRegName = RegName_ECX;
    RegName thisAddrRegName = RegName_ESI;
    RegName trgtAddrRegName = RegName_EDI;
#endif
    Type*   counterType = typeManager.getIntPtrType();

    // prepare counter
    Opnd* counter = irm->newRegOpnd(counterType,counterRegName);
    convertIntToInt(counter, valForCounter, node);

    // prepare this/trgt positions
    Opnd* thisAddrReg = addElemIndexWithLEA(thisArr,thisIdx,thisAddrRegName,node);
    Opnd* trgtAddrReg = addElemIndexWithLEA(trgtArr,trgtIdx,trgtAddrRegName,node);

    Inst* compareInst = irm->newInst(Mnemonic_CMPSW,thisAddrReg,trgtAddrReg,counter);
    compareInst->setPrefix(InstPrefix_REPZ);
    node->appendInst(compareInst);

    // counter is 0 means the same as last comparison leaves zero at ZF
    BranchInst* br = irm->newBranchInst(Mnemonic_JZ, NULL, NULL);
    node->appendInst(br);

    Node* sameRegionsNode = irm->getFlowGraph()->createBlockNode();
    Node* diffRegionsNode = irm->getFlowGraph()->createBlockNode();
    br->setTrueTarget(sameRegionsNode);
    br->setFalseTarget(diffRegionsNode);
    irm->getFlowGraph()->addEdge(node, sameRegionsNode, 0.5);
    irm->getFlowGraph()->addEdge(sameRegionsNode, nextNode, 1);
    irm->getFlowGraph()->addEdge(node, diffRegionsNode, 0.5);
    irm->getFlowGraph()->addEdge(diffRegionsNode, nextNode, 1);

    // regions are equal
    Opnd* one = irm->newImmOpnd(res->getType(),1);
    sameRegionsNode->appendInst(irm->newInst(Mnemonic_MOV, res, one));

    // regions are different
    Opnd* zero = irm->newImmOpnd(res->getType(),0);
    diffRegionsNode->appendInst(irm->newInst(Mnemonic_MOV, res, zero));

    callInst->unlink();
}

void String_indexOf_Handler_x_String_x_I_x_I::run() {

    Node* callInstNode = callInst->getNode();
    Node* nextNode = NULL;

    if(callInst == callInstNode->getLastInst()) {
        nextNode = callInstNode->getUnconditionalEdgeTarget();
        assert(nextNode!=NULL);
    } else {
        nextNode = irm->getFlowGraph()->splitNodeAtInstruction(callInst, true, true, NULL);
    }
    cfg->removeEdge(callInstNode->getUnconditionalEdge());

    // arguments of the call are already prepared by respective HLO pass
    // they are not the strings but 'value' arrays
    Opnd* thisArr = getCallSrc(callInst, 0);
    Opnd* thisOffset = getCallSrc(callInst, 1);
    Opnd* thisLen = getCallSrc(callInst, 2);
    Opnd* trgtArr = getCallSrc(callInst, 3);
    Opnd* trgtOffset = getCallSrc(callInst, 4);
    Opnd* trgtLen = getCallSrc(callInst, 5);
    Opnd* start = getCallSrc(callInst, 6);
    Opnd* res = getCallDst(callInst);

#ifdef _EM64T_
    Type*   counterType = irm->getTypeManager().getInt64Type();
    Constraint regConstr(OpndKind_GPReg, OpndSize_64);
#else
    Type*   counterType = irm->getTypeManager().getInt32Type();
    Constraint regConstr(OpndKind_GPReg, OpndSize_32);
#endif
    Constraint reg16Constr(OpndKind_GPReg, OpndSize_16);

    Opnd* zero = irm->newImmOpnd(counterType, 0);

    Node* mainNode = irm->getFlowGraph()->createBlockNode();
    
    Node* mainLoop = irm->getFlowGraph()->createBlockNode();
    Node* mainLoop2 = irm->getFlowGraph()->createBlockNode();
    Node* mainLoop3 = irm->getFlowGraph()->createBlockNode();
    Node* nestedLoop = irm->getFlowGraph()->createBlockNode();
    Node* nestedLoop2 = irm->getFlowGraph()->createBlockNode();
    Node* nestedLoop3 = irm->getFlowGraph()->createBlockNode();
    Node* mainLoopEnd = irm->getFlowGraph()->createBlockNode();

    Node* returnStart = irm->getFlowGraph()->createBlockNode();
    Node* returnIndex = irm->getFlowGraph()->createBlockNode();
    Node* returnMinusOne = irm->getFlowGraph()->createBlockNode();

    irm->getFlowGraph()->addEdge(mainNode, mainLoop);

    irm->getFlowGraph()->addEdge(mainLoop, mainLoop2, 0.9);
    irm->getFlowGraph()->addEdge(mainLoop, returnMinusOne, 0.1);
    irm->getFlowGraph()->addEdge(mainLoop2, mainLoop3, 0.1);
    irm->getFlowGraph()->addEdge(mainLoop2, mainLoopEnd, 0.9);
    irm->getFlowGraph()->addEdge(mainLoop3, nestedLoop);
    irm->getFlowGraph()->addEdge(nestedLoop, returnIndex, 0.1);
    irm->getFlowGraph()->addEdge(nestedLoop, nestedLoop2, 0.9);
    irm->getFlowGraph()->addEdge(nestedLoop2, mainLoopEnd, 0.8);
    irm->getFlowGraph()->addEdge(nestedLoop2, nestedLoop3, 0.2);
    irm->getFlowGraph()->addEdge(nestedLoop3, nestedLoop);
    irm->getFlowGraph()->addEdge(mainLoopEnd, mainLoop);
    
    irm->getFlowGraph()->addEdge(returnStart, nextNode);
    irm->getFlowGraph()->addEdge(returnMinusOne, nextNode);
    irm->getFlowGraph()->addEdge(returnIndex, nextNode);

    Opnd* subLen = irm->newOpnd(counterType, regConstr);

    bool startIsZero = true;
    if ( !(start->isPlacedIn(OpndKind_Imm) && start->getImmValue() == 0) )
        startIsZero = false;

    if ( !start->isPlacedIn(OpndKind_Imm) )
    {
        Node* startLessThanZero = irm->getFlowGraph()->createBlockNode();
        Node* subLenCheck = irm->getFlowGraph()->createBlockNode();
        Node* startCheck = irm->getFlowGraph()->createBlockNode();

        irm->getFlowGraph()->addEdge(callInstNode, startLessThanZero, 0);
        irm->getFlowGraph()->addEdge(callInstNode, subLenCheck, 1);
        irm->getFlowGraph()->addEdge(startLessThanZero, subLenCheck);
        irm->getFlowGraph()->addEdge(subLenCheck, startCheck, 0);
        irm->getFlowGraph()->addEdge(subLenCheck, mainNode, 1);
        irm->getFlowGraph()->addEdge(startCheck, returnMinusOne, 0);
        irm->getFlowGraph()->addEdge(startCheck, returnStart, 1);

        callInstNode->appendInst(irm->newInst(Mnemonic_CMP, start, zero)); // cmp start, 0
        callInstNode->appendInst(irm->newBranchInst(Mnemonic_JL, startLessThanZero, subLenCheck)); // jl startLessThanZero

        startLessThanZero->appendInst(irm->newInst(Mnemonic_MOV, start, zero)); // mov start, 0

        // saving subString.length on register
        subLenCheck->appendInst(irm->newCopyPseudoInst(Mnemonic_MOV, subLen, trgtLen)); // mov subLen, subString.count
        subLenCheck->appendInst(irm->newInst(Mnemonic_CMP, subLen, zero)); // cmp subLen, 0
        subLenCheck->appendInst(irm->newBranchInst(Mnemonic_JE, startCheck, mainNode)); // je startCheck

        startCheck->appendInst(irm->newInst(Mnemonic_CMP, start, thisLen)); // cmp start, this.count
        startCheck->appendInst(irm->newBranchInst(Mnemonic_JG, returnMinusOne, returnStart)); // jg returnMinusOne
    }
    else // removing unnecessary checks
    {
        int64 val = start->getImmValue();
        if (val <0)
        {
            start = zero;
            val = 0;
        }

        // saving subString.length on register
        callInstNode->appendInst(irm->newCopyPseudoInst(Mnemonic_MOV, subLen, trgtLen)); // mov subLen, subString.count
        callInstNode->appendInst(irm->newInst(Mnemonic_CMP, subLen, zero)); // cmp subLen, 0

        if (val != 0)
        {
            Node* startCheck = irm->getFlowGraph()->createBlockNode();
            
            irm->getFlowGraph()->addEdge(callInstNode, mainNode, 1);
            irm->getFlowGraph()->addEdge(callInstNode, startCheck, 0);
            irm->getFlowGraph()->addEdge(startCheck, returnMinusOne, 0);
            irm->getFlowGraph()->addEdge(startCheck, returnStart, 1);

            callInstNode->appendInst(irm->newBranchInst(Mnemonic_JE, startCheck, mainNode)); // je startCheck

            startCheck->appendInst(irm->newInst(Mnemonic_CMP, thisLen, start)); // cmp this.count, start
            startCheck->appendInst(irm->newBranchInst(Mnemonic_JL, returnMinusOne, returnStart)); // jl returnMinusOne
        }
        else
        {
            irm->getFlowGraph()->addEdge(callInstNode, mainNode, 1);
            irm->getFlowGraph()->addEdge(callInstNode, returnStart, 0);
            callInstNode->appendInst(irm->newBranchInst(Mnemonic_JE, returnStart, mainNode)); // je returnStart
        }
    }

    // prepare this position
    Opnd* offset = irm->newOpnd(counterType, regConstr);
    mainNode->appendInst(irm->newCopyPseudoInst(Mnemonic_MOV, offset, thisOffset)); // mov offset, this.offset
    if (!startIsZero)
        mainNode->appendInst(irm->newInst(Mnemonic_ADD, offset, start)); // add offset, start
    Opnd* thisAddrReg = addElemIndexWithLEA(thisArr, offset, RegName_Null, mainNode); // lea edi, [this.value + offset*sizeof(char) + 12]

    // prepare trgt position
    Opnd* trgtAddrReg = addElemIndexWithLEA(trgtArr, trgtOffset, RegName_Null, mainNode);  // lea esi, [subString.value + subString.offset*sizeof(char) + 12]
    
    // lastIndex = this.count - subString.count - start
    Opnd* lastIndex = irm->newOpnd(counterType, regConstr);
    mainNode->appendInst(irm->newCopyPseudoInst(Mnemonic_MOV, lastIndex, thisLen)); // mov lastIndex, this.count
    mainNode->appendInst(irm->newInst(Mnemonic_SUB, lastIndex, subLen)); // sub lastIndex, subLen
    if (!startIsZero)
        mainNode->appendInst(irm->newInst(Mnemonic_SUB, lastIndex, start)); // sub lastIndex, start
   
    //save subString's first char
    Opnd* firstChar = irm->newOpnd(irm->getTypeManager().getCharType(), reg16Constr);
    mainNode->appendInst(irm->newInst(Mnemonic_MOV, firstChar, irm->newMemOpnd(irm->getTypeManager().getCharType(), trgtAddrReg, 0, 0, 0))); // mov firstChar, word ptr [esi]

     // preparing main loop iterator
    Opnd* mainLoopIter = irm->newOpnd(counterType, regConstr);
    mainNode->appendInst(irm->newInst(Mnemonic_MOV, mainLoopIter, zero)); // mov mainLoopIter, 0

    //*****************************************************************************************************

    // main loop
    mainLoop->appendInst(irm->newInst(Mnemonic_CMP, mainLoopIter, lastIndex)); // cmp mainLoopIter, lastIndex
    mainLoop->appendInst(irm->newBranchInst(Mnemonic_JG, returnMinusOne, mainLoop2)); // jg returnMinusOne

    Opnd* currentChar = irm->newMemOpnd(irm->getTypeManager().getCharType(), thisAddrReg, 0, 0, 0);
    mainLoop2->appendInst(irm->newInst(Mnemonic_CMP, currentChar, firstChar)); // cmp word ptr [edi], firstChar
    mainLoop2->appendInst(irm->newBranchInst(Mnemonic_JNE, mainLoopEnd, mainLoop3)); // jne mainLoopEnd

    // preparing nested loop iterator
    Opnd* nestedLoopIter = irm->newOpnd(counterType, regConstr);
    mainLoop3->appendInst(irm->newInst(Mnemonic_MOV, nestedLoopIter, irm->newImmOpnd(counterType, 1))); // mov nestedLoopIter, 1

    nestedLoop->appendInst(irm->newInst(Mnemonic_CMP, nestedLoopIter, subLen)); // cmp nestedLoopIter, subLen
    nestedLoop->appendInst(irm->newBranchInst(Mnemonic_JGE, returnIndex, nestedLoop2)); // jge returnIndex

    Opnd* tmp = irm->newRegOpnd(irm->getTypeManager().getCharType(), RegName_DX);
    nestedLoop2->appendInst(irm->newInst(Mnemonic_MOV, tmp, irm->newMemOpnd(irm->getTypeManager().getCharType(), thisAddrReg, nestedLoopIter, irm->newImmOpnd(counterType, 2), 0))); // mov tmp, [edi + 2*nestedLoopIter]
    nestedLoop2->appendInst(irm->newInst(Mnemonic_CMP, tmp, irm->newMemOpnd(irm->getTypeManager().getCharType(), trgtAddrReg, nestedLoopIter, irm->newImmOpnd(counterType, 2), 0))); // cmp tmp, [esi + 2*nestedLoopIter]
    nestedLoop2->appendInst(irm->newBranchInst(Mnemonic_JNE, mainLoopEnd, nestedLoop3)); // jne mainLoopEnd
    
    nestedLoop3->appendInst(irm->newInst(Mnemonic_ADD, nestedLoopIter, irm->newImmOpnd(counterType, 1))); // add nestedLoopIter, 1
    
    mainLoopEnd->appendInst(irm->newInst(Mnemonic_ADD, mainLoopIter, irm->newImmOpnd(counterType, 1))); // add mainLoopIter, 1
    mainLoopEnd->appendInst(irm->newInst(Mnemonic_ADD, thisAddrReg, irm->newImmOpnd(counterType, 2))); // add edi, 2

    returnMinusOne->appendInst(irm->newInst(Mnemonic_MOV, res, irm->newImmOpnd(res->getType(), -1)));
    
    returnStart->appendInst(irm->newInst(Mnemonic_MOV, res, start));

    returnIndex->appendInst(irm->newInst(Mnemonic_MOV, res, mainLoopIter));

    if (!startIsZero)
        returnIndex->appendInst(irm->newInst(Mnemonic_ADD, res, start));

    callInst->unlink();
}

void  APIMagicHandler::convertIntToInt(Opnd* dst, Opnd* src, Node* node) 
{
    Type* dstType = dst->getType();
    Type* srcType = src->getType();

    // this works only for equal types 
    // or Int32 into IntPtr conversion
    assert(srcType == dstType || (srcType == irm->getTypeManager().getInt32Type() &&
                                  dstType == irm->getTypeManager().getIntPtrType()));

    if(srcType != dstType) {
#ifdef _EM64T_
        node->appendInst(irm->newInstEx(Mnemonic_MOVZX, 1, dst, src));
#else
        node->appendInst(irm->newCopyPseudoInst(Mnemonic_MOV, dst, src));
#endif
    }
}

//  Compute address of the array element given 
//  address of the first element and index
//  using 'LEA' instruction

Opnd*  APIMagicHandler::addElemIndexWithLEA(Opnd* array, Opnd* index, RegName dstRegName, Node* node) 
{
    ArrayType * arrayType = array->getType()->asArrayType();
    Type * elemType = arrayType->getElementType();
    Type * dstType = irm->getManagedPtrType(elemType);
    //Opnd * dst = irm->newRegOpnd(dstType,dstRegName);
    Opnd* dst;
    if (dstRegName != RegName_Null)
        dst = irm->newRegOpnd(dstType,dstRegName);
    else
    {
        Constraint reg32Constr(OpndKind_GPReg, OpndSize_32);
        dst = irm->newOpnd(dstType,reg32Constr);
    }

//    TypeManager& typeManager = typeManager;
#ifdef _EM64T_
    Type * offType = typeManager.getInt64Type();
#else
    Type * offType = typeManager.getInt32Type();
#endif
    Type * indexType = typeManager.getIntPtrType();
        
    U_32 elemSize = 0;
    if (elemType->isReference()
        && Type::isCompressedReference(elemType->tag, irm->getCompilationInterface()) 
        && !elemType->isCompressedReference()) {
        elemSize = 4;
    } else {
        elemSize = getByteSize(irm->getTypeSize(elemType));
    }
    Opnd * elemSizeOpnd  = irm->newImmOpnd(indexType, elemSize);
    
    Opnd * indexOpnd = NULL;
    if ( index->isPlacedIn(OpndKind_Imm) && index->getImmValue() == 0 ) {
            indexOpnd = NULL;
            elemSizeOpnd = NULL;
    } else {
        indexOpnd = irm->newOpnd(indexType);
        convertIntToInt(indexOpnd,index,node);
    } 
    Opnd * arrOffset = irm->newImmOpnd(offType, arrayType->getArrayElemOffset());
    Opnd * addr = irm->newMemOpnd(dstType,(Opnd*)array, indexOpnd, elemSizeOpnd, arrOffset);
    node->appendInst(irm->newInstEx(Mnemonic_LEA, 1, dst, addr));

    return dst;
}


}} //namespace






