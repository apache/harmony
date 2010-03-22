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

#ifndef _HLOAPIMAGICS_H_
#define _HLOAPIMAGICS_H_

#include "optpass.h"
#include "Inst.h"
#include "irmanager.h"

namespace Jitrino {
                    
class HLOAPIMagicIRBuilder {
public:
    HLOAPIMagicIRBuilder(IRManager* irmanager, MemoryManager& _mm, bool _compRefs)
    : irm(irmanager),
      instFactory(irm->getInstFactory()),
      opndManager(irm->getOpndManager()),
      typeManager(irm->getTypeManager()),
      cfg(irm->getFlowGraph()),
      mm(_mm),
      compRefs(_compRefs),
      currentNode(NULL),
      currentBCOffset(ILLEGAL_BC_MAPPING_VALUE)
    {}

public:
    IRManager*          getIRManager()          {return irm;}
    InstFactory&        getInstFactory()        {return instFactory;}
    OpndManager&        getOpndManager()        {return opndManager;}
    TypeManager&        getTypeManager()        {return typeManager;}
    ControlFlowGraph&   getControlFlowGraph()   {return cfg;}
    MemoryManager&      getMemoryManager()      {return mm;}

    // Flow Graph building
    Node* getCurrentNode() {return currentNode;}
    void  setCurrentNode(Node* node) {currentNode = node;}
    uint16 getCurrentBCOffset() {return currentBCOffset;}
    void   setCurrentBCOffset(uint16 offset) {currentBCOffset = offset;}

    void  genEdgeFromCurrent(Node* target, double edgeProb = 1.0) {cfg.addEdge(currentNode, target, edgeProb);}
    void  genEdgeToCurrent(Node* source) {cfg.addEdge(source,currentNode);}

    Node* genNodeAfter(Node* node, LabelInst* label, Node* dispatch=NULL);
    Node* genNodeAfterCurrent(LabelInst* label, Node* dispatch=NULL);
    Node* genFallthroughNode(Node* dispatch=NULL);

    // IR building
    void  appendInst(Inst* inst);
    void  genCopy(Opnd* trgt, Opnd* src);
    Opnd* genLdField(FieldDesc* fieldDesc, Opnd* base, Opnd* tauBaseNonNull, Opnd* tauAddressInRange);
    Opnd* createOpnd(Type* type);
    VarOpnd* createVarOpnd(Type* type, bool isPinned);
    SsaVarOpnd* createSsaVarOpnd(VarOpnd* var);
    void  genStVar(SsaVarOpnd* var, Opnd* src);
    Opnd* genTauCheckNull(Opnd* base);
    Opnd* genAnd(Type* dstType, Opnd* src1, Opnd* src2);
    Opnd* genTauAnd(Opnd* src1, Opnd* src2);
    Opnd* genAdd(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2);
    Opnd* genSub(Type* dstType, Modifier mod, Opnd* src1, Opnd* src2);
    Opnd* genLdConstant(I_32 val);
    Opnd* genArrayLen(Type* dstType, Type::Tag type, Opnd* array, Opnd* tauNonNull);
    Opnd* genTauArrayLen(Type* dstType, Type::Tag type, Opnd* array,
                         Opnd* tauNullChecked, Opnd *tauTypeChecked);
    Opnd* genCmp3(Type* dstType, Type::Tag instType, // source type for inst
                  ComparisonModifier mod, Opnd* src1, Opnd* src2);
    Opnd* genCmp(Type* dstType, Type::Tag instType, // source type for inst
                 ComparisonModifier mod, Opnd* src1, Opnd* src2);
    Opnd* genTauSafe();
    Opnd* genTauCheckBounds(Opnd* array, Opnd* index, Opnd *tauNullChecked);
    Opnd* genTauCheckBounds(Opnd* ub, Opnd *index);
    Opnd* genTauHasType(Opnd *src, Type *castType);

private:
    IRManager*          irm;
    InstFactory&        instFactory;
    OpndManager&        opndManager;
    TypeManager&        typeManager;
    ControlFlowGraph&   cfg;
    MemoryManager&      mm;
    bool                compRefs;
    Node*               currentNode;
    uint16              currentBCOffset;
};

class HLOAPIMagicHandler {
public:
    HLOAPIMagicHandler(MethodCallInst* inst)
    : callInst(inst)
    {}
    virtual ~HLOAPIMagicHandler(){}

    void setIRBuilder(HLOAPIMagicIRBuilder* irb) {builder = irb;}
    virtual void run() = 0;

protected:
    HLOAPIMagicIRBuilder* builder;
    MethodCallInst*       callInst;
};

#define DECLARE_HLO_MAGIC_INLINER(name)\
class name : public HLOAPIMagicHandler {\
public:\
    name (MethodCallInst* inst)\
    : HLOAPIMagicHandler(inst){}\
    \
    virtual void run();\
};\

DECLARE_HLO_MAGIC_INLINER(System_arraycopy_HLO_Handler);
DECLARE_HLO_MAGIC_INLINER(String_compareTo_HLO_Handler);
DECLARE_HLO_MAGIC_INLINER(String_regionMatches_HLO_Handler);
DECLARE_HLO_MAGIC_INLINER(String_indexOf_HLO_Handler);
DECLARE_HLO_MAGIC_INLINER(System_identityHashCode_Handler);

DEFINE_SESSION_ACTION(HLOAPIMagicSession, hlo_api_magic, "APIMagics HLO Pass")

bool arraycopyOptimizable(Inst* arraycopyCall, bool needWriteBarriers);

void
HLOAPIMagicSession::_run(IRManager& irm)
{
    MemoryManager mm("HLOAPIMagicSession mm");
 
    //finding all api magic calls
    StlVector<HLOAPIMagicHandler*> handlers(mm);
    ControlFlowGraph& fg = irm.getFlowGraph();
    const Nodes& nodes = fg.getNodesPostOrder();//process checking only reachable nodes.
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode()) {
            for (Inst* inst = (Inst*)node->getFirstInst(); inst!=NULL; inst = inst->getNextInst()) {
                if (inst->getOpcode() != Op_DirectCall) {
                    continue;
                }
                MethodCallInst* callInst = (MethodCallInst*)inst;
                MethodDesc* md = callInst->getMethodDesc();
                const char* className = md->getParentType()->getName();
                const char* methodName = md->getName();
                const char* signature = md->getSignatureString();
                if (!strcmp(className, "java/lang/System")) {
                    if (!strcmp(methodName, "arraycopy") && !strcmp(signature, "(Ljava/lang/Object;ILjava/lang/Object;II)V")) {
                        if(getBoolArg("System_arraycopy_as_magic", true) && arraycopyOptimizable(callInst, irm.getCompilationInterface().needWriteBarriers()))
                            handlers.push_back(new (mm) System_arraycopy_HLO_Handler(callInst));
                    }
                    if (!strcmp(methodName, "identityHashCode")) {
                        if (getBoolArg("getIdentityHashCode", false)) {
                            handlers.push_back(new (mm) System_identityHashCode_Handler(callInst));
                        }
                    }
                }
                if (!strcmp(className, "java/lang/String")) {
                    if (!strcmp(methodName, "compareTo") && !strcmp(signature, "(Ljava/lang/String;)I")) {
                        if(getBoolArg("String_compareTo_as_magic", true))
                            handlers.push_back(new (mm) String_compareTo_HLO_Handler(callInst));
                    } else if (!strcmp(methodName, "regionMatches") && !strcmp(signature, "(ILjava/lang/String;II)Z")) {
                        if(getBoolArg("String_regionMatches_as_magic", true))
                            handlers.push_back(new (mm) String_regionMatches_HLO_Handler(callInst));
                    } else if (!strcmp(methodName, "indexOf") && !strcmp(signature, "(Ljava/lang/String;I)I")) {
                        if(getBoolArg("String_indexOf_as_magic", false))
                            handlers.push_back(new (mm) String_indexOf_HLO_Handler(callInst));
                    }
                }
            }
        }
    }

    if(handlers.size() != 0) {
        bool compRefs = getBoolArg("compressedReferences", false);
        HLOAPIMagicIRBuilder builder = HLOAPIMagicIRBuilder(&irm, mm, compRefs);
        //running all handlers
        for (StlVector<HLOAPIMagicHandler*>::const_iterator it = handlers.begin(), end = handlers.end(); it!=end; ++it) {
            HLOAPIMagicHandler* handler = *it;
            handler->setIRBuilder(&builder);
            handler->run();
        }
    }
}






} //namespace Jitrino 

#endif // _HLOAPIMAGICS_H_
