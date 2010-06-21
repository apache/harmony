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

#ifndef _INLINER_H_
#define _INLINER_H_

#include "StlPriorityQueue.h"
#include "Tree.h"
#include "irmanager.h"

namespace Jitrino {

class MemoryManager;
class IRManager;
class TypeManager;
class InstFactory;
class OpndManager;
class Node;
class MethodInst;
class Inst;
class Opnd;
class FlowGraph;
class CompilationInterface;
class MethodDesc;
class DominatorTree;
class DominatorNode;
class LoopTree;
class Method_Table;

class InliningContext {
public:
    InliningContext(U_32 _nArgs, Type** _argTypes) : nArgs(_nArgs), argTypes(_argTypes) {}
    U_32 getNumArgs() const {return nArgs;}
    Type** getArgTypes() const {return argTypes;}
private:
    U_32 nArgs;
    Type** argTypes;
};

class InlineNode : public TreeNode {
public:
    InlineNode(IRManager& irm, Inst *callInst, Node *callNode, bool forced = false)
        : _irm(irm), _callInst(callInst), _callNode(callNode) {}
    InlineNode* getChild()    {return (InlineNode*) child;}
    InlineNode* getSiblings() {return (InlineNode*) siblings;}
    InlineNode* getParent()   {return (InlineNode*) parent;}
    IRManager&  getIRManager()      { return _irm; }
    Inst*       getCallInst() { return _callInst; }
    Node*       getCallNode() { return _callNode; }
    void print(::std::ostream& os);
    void printTag(::std::ostream& os);
private:
    IRManager&  _irm;
    Inst*       _callInst;
    Node*       _callNode;
};

class InlineTree : public Tree {
public:
    InlineTree(InlineNode* r) {
        root = r;
    }
    InlineNode *getRoot() { return (InlineNode*)root; }

    U_32 computeCheckSum() { return computeCheckSum(getRoot()); }
private:
    U_32 computeCheckSum(InlineNode* node);
};

class Inliner
{
public:
    Inliner(SessionAction* argSource, MemoryManager& mm, IRManager& irm,  
        bool doProfileOnly, bool usePriorityQueue, const char* inlinerPipelineName);

    // Inline this method into the current CFG and process it for further
    // inline candidates.  If the argument is the top level CFG, only processing
    // occurs.
    void inlineRegion(InlineNode* inlineNode);

    // Connect input and return operands of the region to the top-level method.  Do not yet splice.
    void connectRegion(InlineNode* inlineNode);

    void compileAndConnectRegion(InlineNode* inlineNode, CompilationContext& inlineCC);

    // Searches the flowgraph for the next inline candidate method.
    InlineNode* getNextRegionToInline(CompilationContext& inlineCC);

    InlineTree& getInlineTree() { return _inlineTree; } 

    void reset();

    InlineNode* createInlineNode(CompilationContext& inlineCC, MethodCallInst* call);

    static double getProfileMethodCount(CompilationInterface& compileIntf, MethodDesc& methodDesc); 
    
    static void runInlinerPipeline(CompilationContext& inlineCC, const char* pipeName);

    /**runs inliner for a specified top level call. 
     * If call==NULL runs inliner for all calls in a method: priority queue is used to find methods to inline
     */
    void runInliner(MethodCallInst* call);

    void setConnectEarly(bool early) {connectEarly = early;}

    /** Inlines all methods annotated with @Inline. 
        Does not run any pipeline for inlined methods
    */
    static void processInlinePragmas(IRManager& irm);

private:

    class CallSite {
    public:
        CallSite(I_32 benefit, Node* callNode, InlineNode* inlineNode) : benefit(benefit), callNode(callNode), inlineNode(inlineNode) {}

        I_32 benefit;
        Node* callNode;
        InlineNode* inlineNode;
    };

    class CallSiteCompare {
    public:
        bool operator()(const CallSite& site1, const CallSite& site2) { return site1.benefit < site2.benefit; }
    };

    void scaleBlockCounts(Node* callSite, IRManager& inlinedIRM);
    void processRegion(InlineNode *inlineNode, DominatorTree* dtree, LoopTree* ltree);
    void processDominatorNode(InlineNode *inlineNode, DominatorNode* dtree, LoopTree* ltree);

    void runTranslatorSession(CompilationContext& inlineCC);

    // True if this method should be processed for further inlining.  I.e., 
    // can we inline the calls in this method?
    bool canInlineFrom(MethodDesc& methodDesc);

    // True if this method may be inlined into a calling method.
    bool canInlineInto(MethodDesc& methodDesc);

    bool isLeafMethod(MethodDesc& methodDesc);

    I_32 computeInlineBenefit(Node* node, MethodDesc& methodDesc, InlineNode* parentInlineNode, U_32 loopDepth);

    MemoryManager& _tmpMM;
    IRManager& _toplevelIRM;
    TypeManager& _typeManager;
    InstFactory& _instFactory;
    OpndManager& _opndManager;

    bool _hasProfileInfo;
    
    StlPriorityQueue<CallSite, StlVector<CallSite>, CallSiteCompare> _inlineCandidates;
    U_32 _initByteSize;
    U_32 _currentByteSize;

    InlineTree _inlineTree;

    bool _doProfileOnlyInlining;
    bool _useInliningTranslator;

    double _maxInlineGrowthFactor;
    U_32 _minInlineStop;
    I_32 _minBenefitThreshold;
    
    U_32 _inlineSmallMaxByteSize;
    I_32 _inlineSmallBonus;
    
    U_32 _inlineMediumMaxByteSize;
    I_32 _inlineMediumBonus;

    U_32 _inlineLargeMinByteSize;
    I_32 _inlineLargePenalty;

    I_32 _inlineLoopBonus;
    I_32 _inlineLeafBonus;
    I_32 _inlineSynchBonus;
    I_32 _inlineRecursionPenalty;
    I_32 _inlineExactArgBonus;
    I_32 _inlineExactAllBonus;

    U_32 _inlineMaxNodeThreshold;

    bool _inlineSkipExceptionPath;
    bool _inlineSkipApiMagicMethods;
    Method_Table* _inlineSkipMethodTable;
    Method_Table* _inlineBonusMethodTable;

    bool _usesOptimisticBalancedSync;
    bool isBCmapRequired;
    void* bc2HIRMapHandler;
    TranslatorAction* translatorAction;
    bool usePriorityQueue;
    const char* inlinerPipelineName;
    bool connectEarly;
    bool isPseudoThrowInserted;
};


} //namespace Jitrino 

#endif // _INLINER_H_
