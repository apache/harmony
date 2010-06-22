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
* @author Yuri Kashnikov
*/

#include "PMFAction.h"
#include "Ia32Inst.h"
#include "Ia32IRManager.h"
#include "methodtable.h"


#define PRAGMA_UNINTERRUPTIBLE "org/vmmagic/pragma/Uninterruptible"

namespace Jitrino {
namespace Ia32{

        

class LightJNIAction: public Action {
public:
    LightJNIAction():_myMM("LightJNIAction"){}
    void init();
    Method_Table* getOFTable() { return lightJNIMethodTable;}
protected:
    MemoryManager _myMM;
    Method_Table* lightJNIMethodTable;    
};



class LightJNISession : public SessionAction { 
protected: 
    void runImpl(); 
    U_32 getSideEffects()const{ return 0; }
    const char* getName() { return "light_jni"; } 
    bool isInOptimizationTable(Method_Table* table, MethodDesc* mmd);
    void derefIfObject(CallInst* callInst, Opnd* origRetOpnd);
    
    CompilationContext* cc;
    IRManager* lirm;
    ControlFlowGraph* fg;

};

ActionFactory<LightJNISession, LightJNIAction> light_jni("light_jni");

void LightJNIAction::init() 
{
    const char* lightJNIMethods = getStringArg("light_jni_methods", NULL);
    lightJNIMethodTable = new (_myMM) Method_Table(_myMM, lightJNIMethods, "LIGHTJNI_METHODS", false);
    Method_Table::Decision des = Method_Table::mt_accepted;
        
    
    // java/lang/Math
    lightJNIMethodTable->add_method_record("java/lang/Math","cos","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","asin","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","acos","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","atan","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","atan2","(DD)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","cbrt","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","ceil","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","cos","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","cosh","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","exp","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","expm1","(D)D", des, false);

    lightJNIMethodTable->add_method_record("java/lang/Math","floor","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","hypot","(DD)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","IEEEremainder","(DD)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","log","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","log10","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","log1p","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","pow","(DD)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","rint","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","sin","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","sinh","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","sqrt","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","tan","(D)D", des, false);
    lightJNIMethodTable->add_method_record("java/lang/Math","tanh","(D)D", des, false);

}


bool LightJNISession::isInOptimizationTable(Method_Table* table, MethodDesc * mmd)
{
    
    const char* className = mmd->getParentType()->getName();
    const char* methodName = mmd->getName();
    const char* sigName = mmd->getSignatureString();
    bool optResult = table->accept_this_method(className,methodName, sigName);
    return optResult;
}


void LightJNISession::derefIfObject(CallInst* callInst, Opnd* origRetOpnd)
{
    Node* node = callInst->getNode();
    Type* origRetType = origRetOpnd->getType();
    Opnd* retOpnd = callInst->getOpnd(0);
    if (callInst != node->getLastInst()) {
        fg->splitNodeAtInstruction(callInst, true, true, NULL);
    }
   
    node = callInst->getNode();
    Node* nextNode = node->getUnconditionalEdgeTarget();    
    assert(nextNode!=NULL);
    fg->removeEdge(node->getUnconditionalEdge());

    Node* zeroObject = fg->createBlockNode();                                
    Node* nonZeroObject = fg->createBlockNode();

    //TODO: EM64T!
    Inst* cmpZeroInst = lirm->newInst(Mnemonic_CMP, retOpnd, lirm->newImmOpnd(retOpnd->getType(), 0));
    Inst* jumpZeroInst = lirm->newBranchInst(Mnemonic_JE, zeroObject, nonZeroObject);
    // cmp eax, 0 ; eax = retOpnd
    // je @zeroObject
    cmpZeroInst->insertAfter(callInst);
    jumpZeroInst->insertAfter(cmpZeroInst);

    // @zeroObject:
    // mov eax,(NullObject)0 ; eax = retOpnd
    Inst* movResZero = lirm->newInst(Mnemonic_MOV, origRetOpnd, lirm->newImmOpnd(lirm->getTypeManager().getNullObjectType(), 0));
    zeroObject->appendInst(movResZero);                                

    // @nonZeroObject:
    // mov eax, [eax] ; eax = retOpnd
    Inst* movResRes = lirm->newInst(Mnemonic_MOV, origRetOpnd, lirm->newMemOpndAutoKind(origRetType, retOpnd));
    nonZeroObject->appendInst(movResRes);

    // link nodes
    fg->addEdge(node, nonZeroObject, 0.9);
    fg->addEdge(node, zeroObject, 0.1);
    fg->addEdge(zeroObject, nextNode);
    fg->addEdge(nonZeroObject, nextNode);
}

void LightJNISession::runImpl() {
    LightJNIAction* action = (LightJNIAction*)getAction();
    Method_Table* tmpT = action->getOFTable();
    cc = getCompilationContext();
    MemoryManager tmpMM("tmp_lightjni_session");
    lirm = cc->getLIRManager();
    fg = lirm->getFlowGraph();
    NamedType* uninterruptiblePragma = NULL;
    TypeManager& tm = lirm->getTypeManager();

    // @Uninterruptible pragma
    uninterruptiblePragma = lirm->getCompilationInterface().findClassUsingBootstrapClassloader(PRAGMA_UNINTERRUPTIBLE);

    StlVector<CallInst*> callsToOptimize(tmpMM);
    const Nodes& nodes = fg->getNodesPostOrder();//process checking only reachable nodes.
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode()) {
            for (Inst* inst = (Inst*)node->getFirstInst(); inst!=NULL; inst = inst->getNextInst()) {
                if (!inst->hasKind(Inst::Kind_CallInst) || !((CallInst*)inst)->isDirect()) {
                    continue;
                }

                CallInst* callInst = (CallInst*)inst;
                Opnd * targetOpnd=callInst->getOpnd(callInst->getTargetOpndIndex());

                assert(targetOpnd->isPlacedIn(OpndKind_Imm));
                Opnd::RuntimeInfo * ri=targetOpnd->getRuntimeInfo();
                if( !ri || ri->getKind() != Opnd::RuntimeInfo::Kind_MethodDirectAddr) { 
                    continue; 
                };

                MethodDesc * md = (MethodDesc*)ri->getValue(0);

                if (!md->isNative()) {
                    continue;
                }
                if (!isInOptimizationTable(tmpT, md)) {
                     if (uninterruptiblePragma==NULL || !md->hasAnnotation(uninterruptiblePragma)){
                        continue;
                     }
                }
                callsToOptimize.push_back(callInst);
            }
        }
    }


    for (StlVector<CallInst*>::const_iterator it = callsToOptimize.begin(), end = callsToOptimize.end(); it!=end; ++it) {
        CallInst* callInst = *it;

        Opnd * targetOpnd=callInst->getOpnd(callInst->getTargetOpndIndex());
        assert(targetOpnd->isPlacedIn(OpndKind_Imm));
        Opnd::RuntimeInfo * ri=targetOpnd->getRuntimeInfo();
        assert( ri && ri->getKind() == Opnd::RuntimeInfo::Kind_MethodDirectAddr);
        MethodDesc * md = (MethodDesc*)ri->getValue(0);

//        printf("optimize %s::%s %s\n", md->getParentType()->getName(), md->getName(), md->getSignatureString());

        void* tmpPoint = md->getNativeAddress();
        Opnd* tmpZero = lirm->newImmOpnd(lirm->getTypeManager().getIntPtrType(), 0);
        U_32 tmpNumArgs = callInst->getOpndCount();
        CallInst* newCallInst = NULL;
        Opnd* origRetOpnd = callInst->getOpnd(0);
        //const char* mmthName = md->getName();
        Opnd* newRetOpnd = origRetOpnd;
        bool deref = origRetOpnd->getType()->isObject();
        if (deref) {
            newRetOpnd = lirm->newOpnd(tm.getUnmanagedPtrType(tm.getUInt8Type()));
        }
        if (md->isInstance()){
            Opnd ** tmpOpnds = new (tmpMM) Opnd*[tmpNumArgs-1];
            Opnd* newTargetOpnd = lirm->newImmOpnd(tm.getIntPtrType(), ((POINTER_SIZE_INT)tmpPoint));

            tmpOpnds[0] = tmpZero;
            for (U_32 i = 2; i<tmpNumArgs; i++) {
                tmpOpnds[i-1] = callInst->getOpnd(i);
            }
            newCallInst = lirm->newCallInst(newTargetOpnd, &CallingConvention_STDCALL, tmpNumArgs - 1, tmpOpnds, newRetOpnd);
        } else  {
            assert(md->isStatic());
            Opnd ** tmpOpnds = new (tmpMM) Opnd*[tmpNumArgs];
            Opnd* newTargetOpnd = lirm->newImmOpnd(tm.getIntPtrType(), ((POINTER_SIZE_INT)tmpPoint));
            Opnd* tmpClassName = lirm->newImmOpnd(tm.getIntPtrType(), (POINTER_SIZE_INT)(md->getParentHandle()));
            tmpOpnds[0] = tmpZero;
            tmpOpnds[1] = tmpClassName;
            for (U_32 i = 2; i<tmpNumArgs; i++){
                tmpOpnds[i] = callInst->getOpnd(i);
            }
            newCallInst = lirm->newCallInst(newTargetOpnd, &CallingConvention_STDCALL, tmpNumArgs, tmpOpnds, newRetOpnd);
        }

        newCallInst->setBCOffset(callInst->getBCOffset());
        newCallInst->insertBefore(callInst);
        callInst->unlink();

        if (deref) {
            derefIfObject(newCallInst, origRetOpnd);
        }

        if (Log::isEnabled()) {
            Log::out() << "changing for method"  << md->getParentType()->getName() << "_" << md->getName() << "_" << md->getSignatureString() << std::endl;
        }
      }
}
}} //namespace

