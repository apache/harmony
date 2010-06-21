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

#include "EMInterface.h"
#include "Log.h"
#include "methodtable.h"
#include "inliner.h"
#include "irmanager.h"
#include "FlowGraph.h"
#include "Inst.h"
#include "Dominator.h"
#include "Loop.h"
#include "simplifier.h"
#include "JavaByteCodeParser.h"
#include "StaticProfiler.h"
#include "optimizer.h"
#include "deadcodeeliminator.h"
#include "VMMagic.h"

namespace Jitrino {

#define MAX_INLINE_GROWTH_FACTOR 170
#define MIN_INLINE_STOP 50
#define MIN_BENEFIT_THRESHOLD 200
#define INLINE_LARGE_THRESHOLD 70


#define MAX_INLINE_GROWTH_FACTOR_PROF 500
#define MIN_INLINE_STOP_PROF 100
// no negative profile benefit for nodes with freq >= 1/10 of entry freq
#define MIN_BENEFIT_THRESHOLD_PROF (MIN_BENEFIT_THRESHOLD / 10) 
#define INLINE_LARGE_THRESHOLD_PROF 150


#define CALL_COST 1

#define INLINE_SMALL_THRESHOLD 12
#define INLINE_SMALL_BONUS 300
#define INLINE_MEDIUM_THRESHOLD 50
#define INLINE_MEDIUM_BONUS 200
#define INLINE_LARGE_PENALTY 500
#define INLINE_LOOP_BONUS 200
#define INLINE_LEAF_BONUS 0
#define INLINE_SYNCH_BONUS 50
#define INLINE_RECURSION_PENALTY 300
#define INLINE_EXACT_ARG_BONUS 0
#define INLINE_EXACT_ALL_BONUS 0
#define INLINE_SKIP_EXCEPTION_PATH false

DEFINE_SESSION_ACTION(InlinePass, inline, "Method Inlining");

Inliner::Inliner(SessionAction* argSource, MemoryManager& mm, IRManager& irm, 
                 bool doProfileOnly, bool _usePriorityQueue, const char* inlinePipeline) 
    : _tmpMM(mm), _toplevelIRM(irm), 
      _typeManager(irm.getTypeManager()), _instFactory(irm.getInstFactory()),
      _opndManager(irm.getOpndManager()),
      _hasProfileInfo(irm.getFlowGraph().hasEdgeProfile()),
      _inlineCandidates(mm), _initByteSize(irm.getMethodDesc().getByteCodeSize()), 
      _currentByteSize(irm.getMethodDesc().getByteCodeSize()), 
      _inlineTree(new (mm) InlineNode(irm, 0, 0)),
      translatorAction(NULL), 
      usePriorityQueue(_usePriorityQueue), inlinerPipelineName(inlinePipeline),
      connectEarly(true), isPseudoThrowInserted(false)
{
    
    const char* translatorName = argSource->getStringArg("translatorActionName", "translator");
    translatorAction = (TranslatorAction*)PMF::getAction(argSource->getPipeline(), translatorName);
    assert(translatorAction);

    _doProfileOnlyInlining = doProfileOnly;
    _useInliningTranslator = !doProfileOnly;
    
    _maxInlineGrowthFactor = ((double)argSource->getIntArg("growth_factor", doProfileOnly ? MAX_INLINE_GROWTH_FACTOR_PROF : MAX_INLINE_GROWTH_FACTOR)) / 100;
    _minInlineStop = argSource->getIntArg("min_stop", doProfileOnly ? MIN_INLINE_STOP_PROF : MIN_INLINE_STOP);
    _minBenefitThreshold = argSource->getIntArg("min_benefit_threshold", doProfileOnly ? MIN_BENEFIT_THRESHOLD_PROF : MIN_BENEFIT_THRESHOLD);
    
    _inlineSmallMaxByteSize = argSource->getIntArg("inline_small_method_max_size", INLINE_SMALL_THRESHOLD);
    _inlineSmallBonus = argSource->getIntArg("inline_small_method_bonus", INLINE_SMALL_BONUS);
    
    _inlineMediumMaxByteSize = argSource->getIntArg("medium_method_max_size", INLINE_MEDIUM_THRESHOLD);
    _inlineMediumBonus = argSource->getIntArg("medium_method_bonus", INLINE_MEDIUM_BONUS);
    
    _inlineLargeMinByteSize = argSource->getIntArg("large_method_min_size", doProfileOnly ? INLINE_LARGE_THRESHOLD_PROF : INLINE_LARGE_THRESHOLD);
    _inlineLargePenalty = argSource->getIntArg("large_method_penalty", INLINE_LARGE_PENALTY);

    _inlineLoopBonus = argSource->getIntArg("loop_bonus", INLINE_LOOP_BONUS);
    _inlineLeafBonus = argSource->getIntArg("leaf_bonus", INLINE_LEAF_BONUS);
    _inlineSynchBonus = argSource->getIntArg("synch_bonus", INLINE_SYNCH_BONUS);
    _inlineRecursionPenalty = argSource->getIntArg("recursion_penalty", INLINE_RECURSION_PENALTY);
    _inlineExactArgBonus = argSource->getIntArg("exact_single_parameter_bonus", INLINE_EXACT_ARG_BONUS);
    _inlineExactAllBonus = argSource->getIntArg("exact_all_parameter_bonus", INLINE_EXACT_ALL_BONUS);

    _inlineMaxNodeThreshold = irm.getOptimizerFlags().hir_node_threshold * irm.getOptimizerFlags().inline_node_quota / 100;

    _inlineSkipExceptionPath = argSource->getBoolArg("skip_exception_path", INLINE_SKIP_EXCEPTION_PATH);
#if defined  (_EM64T_) || defined (_IPF_)
    _inlineSkipApiMagicMethods  = false;
#else
    _inlineSkipApiMagicMethods = argSource->getBoolArg("skip_api_magics", true);
#endif 

    const char* skipMethods = argSource->getStringArg("skip_methods", NULL);
    _inlineSkipMethodTable = NULL;
    if(skipMethods != NULL || _inlineSkipApiMagicMethods) {
        _inlineSkipMethodTable = new (_tmpMM) Method_Table(_tmpMM, skipMethods, "SKIP_METHODS", false);
        if (_inlineSkipApiMagicMethods) {
#if defined (_IPF_)
//TODO: IA32 helpers should work on EM64T too -> TODO test
#else
            //is_accepted will return 'true' for these methods by skip table-> no inlining will be done
            Method_Table::Decision des = Method_Table::mt_accepted; 
#ifndef  _EM64T_ // not tested
            _inlineSkipMethodTable->add_method_record("java/lang/Integer", "numberOfLeadingZeros", "(I)I", des, false);
            _inlineSkipMethodTable->add_method_record("java/lang/Integer", "numberOfTrailingZeros", "(I)I", des, false);
            _inlineSkipMethodTable->add_method_record("java/lang/Long", "numberOfLeadingZeros", "(J)I", des, false);
            _inlineSkipMethodTable->add_method_record("java/lang/Long", "numberOfTrailingZeros", "(J)I", des, false);
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "sqrt", "(D)D", des, false); 
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "sin", "(D)D", des, false); 
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "cos", "(D)D", des, false); 
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "abs", "(F)F", des, false);            
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "abs", "(D)D", des, false); 
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "tan", "(D)D", des, false);           
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "atan", "(D)D", des, false);  
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "atan2", "(DD)D", des, false);  
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "asin", "(D)D", des, false); 
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "acos", "(D)D", des, false);                         
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "log", "(D)D", des, false);           
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "log10", "(D)D", des, false);           
            _inlineSkipMethodTable->add_method_record("java/lang/Math", "log1p", "(D)D", des, false);           
#endif
            if(argSource->getBoolArg("System_arraycopy_as_magic",true)) {
                _inlineSkipMethodTable->add_method_record("java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", des, false);
            }
            if(argSource->getBoolArg("String_compareTo_as_magic",true)) {
                _inlineSkipMethodTable->add_method_record("java/lang/String", "compareTo", "(Ljava/lang/String;)I", des, false);
            }
            if(argSource->getBoolArg("String_regionMatches_as_magic",true)) {
                _inlineSkipMethodTable->add_method_record("java/lang/String", "regionMatches", "(ILjava/lang/String;II)Z", des, false);
            }
	    if(argSource->getBoolArg("String_indexOf_as_magic",true)) {
                _inlineSkipMethodTable->add_method_record("java/lang/String", "indexOf", "(Ljava/lang/String;I)I", des, false);
            }
#endif 
        }
    }

    const char* bonusMethods = argSource->getStringArg("bonus_methods", NULL);
    _inlineBonusMethodTable = NULL;
    if(bonusMethods!=NULL) {
        _inlineBonusMethodTable = new (_tmpMM) Method_Table(_tmpMM, bonusMethods, "BONUS_METHODS", false);
    }

    _usesOptimisticBalancedSync = argSource->getBoolArg("sync_optimistic", false) ? argSource->getBoolArg("sync_optcatch", true) : false;
}

I_32 
Inliner::computeInlineBenefit(Node* node, MethodDesc& methodDesc, InlineNode* parentInlineNode, U_32 loopDepth) 
{
    I_32 benefit = 0;

    if (Log::isEnabled()) {
        Log::out() << "Computing Inline benefit for "
                               << methodDesc.getParentType()->getName()
                               << "." << methodDesc.getName() << ::std::endl;
    }
    if (_inlineBonusMethodTable!=NULL && _inlineBonusMethodTable->accept_this_method(methodDesc)) {
        benefit+=1000;
        if (Log::isEnabled()) {
            Log::out() << "Method is in bonus table benefit+=1000"<<std::endl;
        }
    }

    //
    // Size impact
    //
    U_32 size = methodDesc.getByteCodeSize();
    Log::out() << "  size is " << (int) size << ::std::endl;
    if(size < _inlineSmallMaxByteSize) {
        // Large bonus for smallest methods
        benefit += _inlineSmallBonus;
        Log::out() << "  isSmall, benefit now = " << benefit << ::std::endl;
    } else if(size < _inlineMediumMaxByteSize) {
        // Small bonus for somewhat small methods
        benefit += _inlineMediumBonus;
        Log::out() << "  isMedium, benefit now = " << benefit << ::std::endl;
    } else if(size > _inlineLargeMinByteSize) {
        // Penalty for large methods
        benefit -= _inlineLargePenalty * (loopDepth+1); 
        Log::out() << "  isLarge, benefit now = " << benefit << ::std::endl;
    }
    benefit -= size;
    Log::out() << "  Subtracting size, benefit now = " << benefit << ::std::endl;

    //
    // Loop depth impact - add bonus for deep call sites
    //
    benefit += _inlineLoopBonus*loopDepth;
    Log::out() << "  Loop Depth is " << (int) loopDepth
                           << ", benefit now = " << benefit << ::std::endl;

    //
    // Synch bonus - may introduce synch removal opportunities
    //
    if(methodDesc.isSynchronized()){
        benefit += _inlineSynchBonus;
        Log::out() << "  Method is synchronized, benefit now = " << benefit << ::std::endl;
    }
    //
    // Recursion penalty - discourage recursive inlining
    //
    for(; parentInlineNode != NULL; parentInlineNode = parentInlineNode->getParent()) {
        if(&methodDesc == &(parentInlineNode->getIRManager().getMethodDesc())) {
            benefit -= _inlineRecursionPenalty;
            Log::out() << "  Subtracted one recursion level, benefit now = " << benefit << ::std::endl;
        }
    }
    
    //
    // Leaf bonus - try to inline leaf methods
    //
    if(isLeafMethod(methodDesc)) {
        benefit += _inlineLeafBonus;
        Log::out() << "  Added leaf bonus, benefit now = " << benefit << ::std::endl;
    }

    //
    // Exact argument bonus - may introduce specialization opportunities
    //
    Inst* last = (Inst*)node->getLastInst();
    if(last->getOpcode() == Op_DirectCall) {
        MethodCallInst* call = last->asMethodCallInst();
        assert(call != NULL);
        bool exact = true;
        // first 2 opnds are taus; skip them
        assert(call->getNumSrcOperands() >= 2);
        assert(call->getSrc(0)->getType()->tag == Type::Tau);
        assert(call->getSrc(1)->getType()->tag == Type::Tau);
        for(U_32 i = 2; i < call->getNumSrcOperands(); ++i) {
            Opnd* arg = call->getSrc(i);
            assert(arg->getType()->tag != Type::Tau);
            if(arg->getInst()->isConst()) {
                benefit += _inlineExactArgBonus;
                Log::out() << "  Src " << (int) i
                                       << " is const, benefit now = " << benefit << ::std::endl;
            } else if(arg->getType()->isObject() && Simplifier::isExactType(arg)) {
                benefit += _inlineExactArgBonus;
                Log::out() << "  Src " << (int) i
                                       << " is exacttype, benefit now = " << benefit << ::std::endl;
            }  else {
                exact = false;
                Log::out() << "  Src " << (int) i
                                       << " is inexact, benefit now = " << benefit << ::std::endl;
            }
        }
        if(call->getNumSrcOperands() > 2 && exact) {
            benefit += _inlineExactAllBonus;
            Log::out() << "  Added allexact bonus, benefit now = " << benefit << ::std::endl;
        }
    }        
    
    //
    // Use profile information if available
    //
    if(_doProfileOnlyInlining && _toplevelIRM.getFlowGraph().hasEdgeProfile()) {
        double heatThreshold = _toplevelIRM.getHeatThreshold();
        double nodeCount = node->getExecCount();
        double scale =   nodeCount / heatThreshold;
        if(scale > 100)
            scale = 100;
        // Remove any loop bonus as this is already accounted for in block count
        benefit -= _inlineLoopBonus*loopDepth;
        // Scale by call site 'hotness'.
        benefit = (U_32) ((double) benefit * scale);

        Log::out() << "  HeatThreshold=" << heatThreshold
                               << ", nodeCount=" << nodeCount
                               << ", scale=" << scale
                               << "; benefit now = " << benefit
                               << ::std::endl;
    }
    return benefit;
}

class JavaByteCodeLeafSearchCallback : public JavaByteCodeParserCallback {
public:
    JavaByteCodeLeafSearchCallback() {
        leaf = true;
    }
    void parseInit() {}
    // called after parsing ends, but not if an error occurs
    void parseDone() {}
    // called when an error occurs parsing the byte codes
    void parseError() {}
    bool isLeaf() const { return leaf; }
protected:
    bool leaf;

    void offset(U_32 offset) {};
    void offset_done(U_32 offset) {};
    void nop()  {}
    void aconst_null()  {}
    void iconst(I_32)  {}
    void lconst(int64)  {}
    void fconst(float)  {}
    void dconst(double)  {}
    void bipush(I_8)  {}
    void sipush(int16)  {}
    void ldc(U_32)  {}
    void ldc2(U_32)  {}
    void iload(uint16 varIndex)  {}
    void lload(uint16 varIndex)  {}
    void fload(uint16 varIndex)  {}
    void dload(uint16 varIndex)  {}
    void aload(uint16 varIndex)  {}
    void iaload()  {}
    void laload()  {}
    void faload()  {}
    void daload()  {}
    void aaload()  {}
    void baload()  {}
    void caload()  {}
    void saload()  {}
    void istore(uint16 varIndex,U_32 off)  {}
    void lstore(uint16 varIndex,U_32 off)  {}
    void fstore(uint16 varIndex,U_32 off)  {}
    void dstore(uint16 varIndex,U_32 off)  {}
    void astore(uint16 varIndex,U_32 off)  {}
    void iastore()  {}
    void lastore()  {}
    void fastore()  {}
    void dastore()  {}
    void aastore()  {}
    void bastore()  {}
    void castore()  {}
    void sastore()  {}
    void pop()  {}
    void pop2()  {}
    void dup()  {}
    void dup_x1()  {}
    void dup_x2()  {}
    void dup2()  {}
    void dup2_x1()  {}
    void dup2_x2()  {}
    void swap()  {}
    void iadd()  {}
    void ladd()  {}
    void fadd()  {}
    void dadd()  {}
    void isub()  {}
    void lsub()  {}
    void fsub()  {}
    void dsub()  {}
    void imul()  {}
    void lmul()  {}
    void fmul()  {}
    void dmul()  {}
    void idiv()  {}
    void ldiv()  {}
    void fdiv()  {}
    void ddiv()  {}
    void irem()  {}
    void lrem()  {}
    void frem()  {}
    void drem()  {}
    void ineg()  {}
    void lneg()  {}
    void fneg()  {}
    void dneg()  {}
    void ishl()  {}
    void lshl()  {}
    void ishr()  {}
    void lshr()  {}
    void iushr()  {}
    void lushr()  {}
    void iand()  {}
    void land()  {}
    void ior()  {}
    void lor()  {}
    void ixor()  {}
    void lxor()  {}
    void iinc(uint16 varIndex,I_32 amount)  {}
    void i2l()  {}
    void i2f()  {}
    void i2d()  {}
    void l2i()  {}
    void l2f()  {}
    void l2d()  {}
    void f2i()  {}
    void f2l()  {}
    void f2d()  {}
    void d2i()  {}
    void d2l()  {}
    void d2f()  {}
    void i2b()  {}
    void i2c()  {}
    void i2s()  {}
    void lcmp()  {}
    void fcmpl()  {}
    void fcmpg()  {}
    void dcmpl()  {}
    void dcmpg()  {}
    void ifeq(U_32 targetOffset,U_32 nextOffset)  {}
    void ifne(U_32 targetOffset,U_32 nextOffset)  {}
    void iflt(U_32 targetOffset,U_32 nextOffset)  {}
    void ifge(U_32 targetOffset,U_32 nextOffset)  {}
    void ifgt(U_32 targetOffset,U_32 nextOffset)  {}
    void ifle(U_32 targetOffset,U_32 nextOffset)  {}
    void if_icmpeq(U_32 targetOffset,U_32 nextOffset)  {}
    void if_icmpne(U_32 targetOffset,U_32 nextOffset)  {}
    void if_icmplt(U_32 targetOffset,U_32 nextOffset)  {}
    void if_icmpge(U_32 targetOffset,U_32 nextOffset)  {}
    void if_icmpgt(U_32 targetOffset,U_32 nextOffset)  {}
    void if_icmple(U_32 targetOffset,U_32 nextOffset)  {}
    void if_acmpeq(U_32 targetOffset,U_32 nextOffset)  {}
    void if_acmpne(U_32 targetOffset,U_32 nextOffset)  {}
    void goto_(U_32 targetOffset,U_32 nextOffset)  {}
    void jsr(U_32 offset, U_32 nextOffset)  {}
    void ret(uint16 varIndex,const U_8* byteCodes)  {}
    void tableswitch(JavaSwitchTargetsIter*)  {}
    void lookupswitch(JavaLookupSwitchTargetsIter*)  {}
    void ireturn(U_32 off)  {}
    void lreturn(U_32 off)  {}
    void freturn(U_32 off)  {}
    void dreturn(U_32 off)  {}
    void areturn(U_32 off)  {}
    void return_(U_32 off)  {}
    void getstatic(U_32 constPoolIndex)  {}
    void putstatic(U_32 constPoolIndex)  {}
    void getfield(U_32 constPoolIndex)  {}
    void putfield(U_32 constPoolIndex)  {}
    void invokevirtual(U_32 constPoolIndex)  { leaf = false; }
    void invokespecial(U_32 constPoolIndex)  { leaf = false; }
    void invokestatic(U_32 constPoolIndex)  { leaf = false; }
    void invokeinterface(U_32 constPoolIndex,U_32 count)  { leaf = false; }
    void new_(U_32 constPoolIndex)  {}
    void newarray(U_8 type)  {}
    void anewarray(U_32 constPoolIndex)  {}
    void arraylength()  {}
    void athrow()  {}
    void checkcast(U_32 constPoolIndex)  {}
    int  instanceof(const U_8* bcp, U_32 constPoolIndex, U_32 off)  {return 3;}
    void monitorenter()  {}
    void monitorexit()  {}
    void multianewarray(U_32 constPoolIndex,U_8 dimensions)  {}
    void ifnull(U_32 targetOffset,U_32 nextOffset)  {}
    void ifnonnull(U_32 targetOffset,U_32 nextOffset) {}
};

bool
Inliner::isLeafMethod(MethodDesc& methodDesc) {
    U_32 size = methodDesc.getByteCodeSize();
    const U_8* bytecodes = methodDesc.getByteCodes();
    ByteCodeParser parser(bytecodes,size);
    JavaByteCodeLeafSearchCallback leafTester;
    parser.parse(&leafTester);
    return leafTester.isLeaf();
}

void
Inliner::reset()
{
    _hasProfileInfo = true; // _irm.getFlowGraph().hasEdgeProfile();
    _inlineCandidates.clear();
}

bool
Inliner::canInlineFrom(MethodDesc& methodDesc)
{
    // 
    // Test if calls in this method should be inlined
    //
    bool doInline = !methodDesc.isClassInitializer() && !methodDesc.getParentType()->isLikelyExceptionType();
    Log::out() << "Can inline from " << methodDesc.getParentType()->getName() << "." << methodDesc.getName() << " == " << doInline << ::std::endl;
    return doInline;
}

bool
Inliner::canInlineInto(MethodDesc& methodDesc)
{
    // 
    // Test if a call to this method should be inlined
    //
    bool doSkip = (_inlineSkipMethodTable == NULL) ? false : _inlineSkipMethodTable->accept_this_method(methodDesc);
    if(doSkip)
        Log::out() << "Skipping inlining of " << methodDesc.getParentType()->getName() << "." << methodDesc.getName() << ::std::endl;    
    bool doInline = !doSkip && !methodDesc.isNative() && !methodDesc.isNoInlining() && !methodDesc.getParentType()->isLikelyExceptionType();
    Log::out() << "Can inline this " << methodDesc.getParentType()->getName() << "." << methodDesc.getName() << " == " << doInline << ::std::endl;
    return doInline;
}

void
Inliner::connectRegion(InlineNode* inlineNode) {
    if(inlineNode == _inlineTree.getRoot())
        // This is the top level graph.
        return;
    
    
    IRManager &inlinedIRM = inlineNode->getIRManager();
    ControlFlowGraph &inlinedFlowGraph = inlinedIRM.getFlowGraph();
    Inst *callInst = inlineNode->getCallInst();
    MethodDesc &methodDesc = inlinedIRM.getMethodDesc();
    
    // Mark inlined region
    Opnd *obj = 0;
    if (methodDesc.isClassInitializer()) {
        assert(callInst->getNumSrcOperands() >= 3);
        Opnd *obj = callInst->getSrc(2); // this parameter for DirectCall
        if (obj && !obj->isNull()) obj = 0;
    }
    
    
    //set up inlinee border markers
    {
        Node* entry = inlinedFlowGraph.getEntryNode();
        //replace MethodEntryInst with simple label -> MethodMarkerInsts will be used to mark inlinee borders
        entry->getFirstInst()->unlink(); 
        entry->prependInst(_instFactory.makeLabel());

        uint16 callerOffset = inlineNode->getCallInst()->getBCOffset();
        assert(callerOffset!=ILLEGAL_BC_MAPPING_VALUE);
        Inst* entryMarker =  obj ? _instFactory.makeMethodMarker(MethodMarkerInst::Entry, &methodDesc, obj)
            : _instFactory.makeMethodMarker(MethodMarkerInst::Entry, &methodDesc);
        entryMarker->setBCOffset(callerOffset);
        entry->prependInst(entryMarker);
        Node* retNode = inlinedFlowGraph.getReturnNode();
        if (retNode) {
            Inst* exitMarker =  obj ? _instFactory.makeMethodMarker(MethodMarkerInst::Exit, &methodDesc, obj)
                : _instFactory.makeMethodMarker(MethodMarkerInst::Exit,  &methodDesc);
            retNode->appendInst(exitMarker);
        }

        Node* unwindNode = inlinedFlowGraph.getUnwindNode();
        if (unwindNode) {
            Inst* exitMarker =  obj ? _instFactory.makeMethodMarker(MethodMarkerInst::Exit, &methodDesc, obj)
                : _instFactory.makeMethodMarker(MethodMarkerInst::Exit,  &methodDesc);
            unwindNode->appendInst(exitMarker);
        }
    }


    // Fuse callsite arguments with incoming parameters
    U_32 numArgs = callInst->getNumSrcOperands() - 2; // omit taus
    Opnd *tauNullChecked = callInst->getSrc(0);
    Opnd *tauTypesChecked = callInst->getSrc(1);
    assert(tauNullChecked->getType()->tag == Type::Tau);
    assert(tauTypesChecked->getType()->tag == Type::Tau);

    // Mark tauNullChecked def inst as nonremovable
    // tauSafe can be here. this is not our case.
    Inst* tauNullCheckInst = tauNullChecked->getInst();
    if(tauNullCheckInst->getOpcode() == Op_TauCheckNull) {
        tauNullCheckInst->setDefArgModifier(NonNullThisArg);
    }

    Node* entry = inlinedFlowGraph.getEntryNode();
    Node* retNode = inlinedFlowGraph.getReturnNode();
    Inst* first = (Inst*)entry->getFirstInst();
    Inst* inst;
    Opnd* thisPtr = 0;
    U_32 j;
    for(j = 0, inst = first->getNextInst(); j < numArgs && inst != NULL;) {
        switch (inst->getOpcode()) {
        case Op_DefArg:
            {
                Opnd* dst = inst->getDst(); 
                Opnd* src = callInst->getSrc(j+2); // omit taus
                Inst* copy = NULL;
                // todo: make translators isMagicClass and convertMagic2HIRType methods public
                // use these methods here
                if (dst->getType()->isUnmanagedPtr()==src->getType()->isUnmanagedPtr()) {
                    copy = _instFactory.makeCopy(dst, src);
                } else {
                    Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No);
                    copy = _instFactory.makeConvUnmanaged(mod, dst->getType()->tag, dst, src);
                }
                copy->insertBefore(inst);
                inst->unlink();
                inst = copy;
                ++j;
                if (!thisPtr) {
                    src = thisPtr;
                }
            }
            break;
        case Op_TauHasType:
            {
                // substitute tau parameter for hasType test of parameter
                Opnd* src = inst->getSrc(0);
                if (tauTypesChecked->getInst()->getOpcode() != Op_TauUnsafe) {
                    // it's worth substituting the tau
                    for (U_32 i=0; i<numArgs; ++i) {
                        if (src == callInst->getSrc(i+2)) {
                            // it is a parameter
                            Opnd* dst = inst->getDst();
                            Inst* copy = _instFactory.makeCopy(dst, tauTypesChecked);
                            copy->insertBefore(inst);
                            inst->unlink();
                            inst = copy;
                        }
                    }
                }
            }
            break;
        case Op_TauIsNonNull:
            {
                // substitute tau parameter for isNonNull test of this parameter
                Opnd* src = inst->getSrc(0);
                if (src == thisPtr) {
                    Opnd* dst = inst->getDst();
                    Inst* copy = _instFactory.makeCopy(dst, tauNullChecked);
                    copy->insertBefore(inst);
                    inst->unlink();
                    inst = copy;
                }
            }
            break;
        case Op_TauHasExactType:
        default:
            break;
        }
        inst = inst->getNextInst();
    }
    assert(j == numArgs);
    
    //
    // Convert returns in inlined region into stvar/ldvar
    //
    if(retNode != NULL) {
        Opnd* dst = callInst->getDst();
        VarOpnd* retVar = NULL;
        if(!dst->isNull())
            retVar = _opndManager.createVarOpnd(dst->getType(), false);
        
        // Iterate return sites
        assert(retNode != NULL);
        bool isSsa = inlinedIRM.getInSsa();
        Opnd** phiArgs = isSsa ? new (_toplevelIRM.getMemoryManager()) Opnd*[retNode->getInDegree()] : NULL;
        U_32 phiCount = 0;
        const Edges& inEdges = retNode->getInEdges();
        Edges::const_iterator eiter;
        for(eiter = inEdges.begin(); eiter != inEdges.end(); ++eiter) {
            Node* retSite = (*eiter)->getSourceNode();
            Inst* ret = (Inst*)retSite->getLastInst();
            assert(ret->getOpcode() == Op_Return);
            if(retVar != NULL) {
                Opnd* tmp = ret->getSrc(0);
                assert(tmp != NULL);
                if(isSsa) {
                    SsaVarOpnd* ssaVar = _opndManager.createSsaVarOpnd(retVar);
                    phiArgs[phiCount++] = ssaVar;
                    retSite->appendInst(_instFactory.makeStVar(ssaVar, tmp));
                } else {
                    retSite->appendInst(_instFactory.makeStVar(retVar, tmp));
                }
            }
            ret->unlink();
        }
        if(retVar != NULL) {
            // Insert phi and ldVar after call site
            Node* joinNode = inlinedFlowGraph.splitReturnNode(_instFactory.makeLabel());
            joinNode->setExecCount(retNode->getExecCount());
            joinNode->findTargetEdge(retNode)->setEdgeProb(1.0);
            if(isSsa) {
                SsaVarOpnd* ssaVar = _opndManager.createSsaVarOpnd(retVar);
                joinNode->appendInst(_instFactory.makePhi(ssaVar, phiCount, phiArgs));
                joinNode->appendInst(_instFactory.makeLdVar(dst, ssaVar));
            } else {
                joinNode->appendInst(_instFactory.makeLdVar(dst, retVar));
            }

            // Set return operand.
            assert(inlinedIRM.getReturnOpnd() == NULL);
            inlinedIRM.setReturnOpnd(dst);
        }
    }
    
    //
    // Insert explicit catch_all/monitor_exit/rethrow at exception exits for a synchronized inlined region
    //
    if(methodDesc.isSynchronized() && inlinedIRM.getFlowGraph().getUnwindNode()) {
        // Add monitor exit to unwind.
        Node* unwind = inlinedFlowGraph.getUnwindNode();
        uint16 monExitBCMap = 0; //this monexit is not present in bytecode. Need some artificial but valid value.
        // Test if an exception exit exists
        if(unwind->getInDegree() > 0) {
            Node* dispatch = inlinedFlowGraph.createDispatchNode(_instFactory.makeLabel());
            
            // Insert catch all
            Opnd* ex = _opndManager.createSsaTmpOpnd(_typeManager.getSystemObjectType());
            Inst* catchInst = inlinedIRM.getInstFactory().makeCatchLabel(0, ex->getType());
            catchInst->setBCOffset(0);
            Node* handler = inlinedFlowGraph.createBlockNode(catchInst);
            handler->appendInst(_instFactory.makeCatch(ex));
            if(methodDesc.isStatic()) {
                // Release class monitor
                Inst* monExit = _instFactory.makeTypeMonitorExit(methodDesc.getParentType());
                monExit->setBCOffset(monExitBCMap);
                handler->appendInst(monExit);
            } else {
                // Release object monitor
                assert(callInst->getNumSrcOperands() > 2);
                Opnd* obj = callInst->getSrc(2); // object is arg 2;
                const TranslatorFlags& traFlags = translatorAction->getFlags();
                if (!traFlags.ignoreSync && !traFlags.syncAsEnterFence) {
                    if (_usesOptimisticBalancedSync) {
                        // We may need to insert an optimistically balanced monitorexit.
                        // Note that we shouldn't have an un-optimistic balanced monitorexit,
                        // since there is at least one edge to UNWIND; this edge should have
                        // prevented balancing for a synchronized method, which would have
                        // a missing monitorexit on the exception path.
                        
                        Node *aRetNode = retNode;
                        bool done = false;
                        int count = 0;
                        // We should have an optbalmonexit on a return path
                        while (!done && aRetNode != NULL && !aRetNode->getInEdges().empty()) {
                            const Edges& retEdges = aRetNode->getInEdges();
                            if (!retEdges.empty()) {
                                Edge *anEdgeToReturn = *retEdges.begin();
                                Node *aMonexitNode = anEdgeToReturn->getSourceNode();
                                Inst *lastInst = (Inst*)aMonexitNode->getLastInst();
                                Inst *firstInst = (Inst*)aMonexitNode->getFirstInst();
                                while (lastInst != firstInst) {
                                    if (lastInst->getOpcode() == Op_OptimisticBalancedMonitorExit) {
                                        Opnd *srcObj = lastInst->getSrc(0);
                                        Opnd *lockAddr = lastInst->getSrc(1);
                                        Opnd *enterDst = lastInst->getSrc(2);
                                        
                                        Inst* monExit = _instFactory.makeOptimisticBalancedMonitorExit(srcObj,lockAddr,enterDst);
                                        monExit->setBCOffset(monExitBCMap);
                                        handler->appendInst(monExit);
                                        done = true;
                                        break;
                                    }
                                    lastInst = lastInst->getPrevInst();
                                }
                                if (!done) {
                                    // we may have empty nodes or something, so try a predecessor.
                                    assert(count < 10); // try to bound this search
                                    count += 1;
                                    // we have to search further.
                                    aRetNode = aMonexitNode;
                                    assert(aRetNode);
                                    assert(!aRetNode->getInEdges().empty());
                                }
                            } else {
                                assert(0);
                            }
                        }
                        assert(done);
                    } else {
                        Opnd* tauSafe = _opndManager.createSsaTmpOpnd(_typeManager.getTauType());
                        handler->appendInst(_instFactory.makeTauSafe(tauSafe)); // monenter success guarantees non-null
                        Inst* monExit = _instFactory.makeTauMonitorExit(obj, tauSafe);
                        monExit->setBCOffset(monExitBCMap);
                        handler->appendInst(monExit);
                    }
                }
            }
            
            // Insert rethrow
            Node* rethrow = inlinedFlowGraph.createBlockNode(_instFactory.makeLabel());
            Inst* rethrowInst = _instFactory.makeThrow(Throw_NoModifier, ex);
            rethrowInst->setBCOffset(monExitBCMap);
            rethrow->appendInst(rethrowInst);
            
            // Redirect exception exits to monitor_exit
            while (!unwind->getInEdges().empty()) {
                Edge* edge = unwind->getInEdges().front();
                inlinedFlowGraph.replaceEdgeTarget(edge, dispatch);
            }
            inlinedFlowGraph.addEdge(dispatch, handler);
            inlinedFlowGraph.addEdge(handler, rethrow);
            inlinedFlowGraph.addEdge(handler, unwind);
            inlinedFlowGraph.addEdge(rethrow, unwind);
        }
    }
    
    //
    // Add type initialization if necessary
    //
    if ((methodDesc.isStatic() || methodDesc.isInstanceInitializer()) &&
        methodDesc.getParentType()->needsInitialization()) {
            // initialize type for static methods
            Inst* initType = _instFactory.makeInitType(methodDesc.getParentType());
            initType->setBCOffset(callInst->getBCOffset());
            entry->prependInst(initType); //inittype is placed before methodEntryMarker -> it's a part of caller method in stacktrace
            inlinedFlowGraph.splitNodeAtInstruction(initType, true, false, _instFactory.makeLabel());
            //here we need to create new unwind node, because old one contains method-exit-marker instruction.
            //Init-type is a part of caller method -> it must be out of the range of method marker insts.
            Node* oldUnwind = inlinedFlowGraph.getUnwindNode();
            Node* newUnwind = inlinedFlowGraph.createDispatchNode(_instFactory.makeLabel());
            inlinedFlowGraph.addEdge(entry, newUnwind);
            inlinedFlowGraph.addEdge(newUnwind, inlinedFlowGraph.getExitNode());
            inlinedFlowGraph.setUnwindNode(newUnwind);
            if (oldUnwind!=NULL) {
                assert(oldUnwind->getOutDegree() == 1);
                inlinedFlowGraph.replaceEdgeTarget(oldUnwind->getExceptionEdge(), newUnwind, true);
            }
        }
}

void
Inliner::inlineRegion(InlineNode* inlineNode) {
    IRManager &inlinedIRM = inlineNode->getIRManager();
    DominatorTree* dtree = inlinedIRM.getDominatorTree();
    LoopTree* ltree = inlinedIRM.getLoopTree();
    ControlFlowGraph &inlinedFlowGraph = inlinedIRM.getFlowGraph();
    Node *callNode = inlineNode->getCallNode();
    Inst *callInst = inlineNode->getCallInst();
    MethodDesc &methodDesc = inlinedIRM.getMethodDesc();
    
    if (Log::isEnabled()) {
        Log::out()<<"inlineAndProcessRegion "<< methodDesc.getParentType()->getName() << "."<< methodDesc.getName()<<std::endl;
    }

    // Scale block counts in the inlined region for this particular call site
    if (callNode != NULL) {
        scaleBlockCounts(callNode, inlinedIRM);
    }
    
    // Update priority queue with calls in this region
    processRegion(inlineNode, dtree, ltree);
    
    // If top level flowgraph 
    if (inlineNode == _inlineTree.getRoot()) {
        Log::out() << "inlineNode is root" << ::std::endl;
        return;
    }

    // Splice region into top level flowgraph at call site
    assert(callInst->getOpcode() == Op_DirectCall);
    Log::out() << "Inlining " << methodDesc.getParentType()->getName() 
        << "." << methodDesc.getName() << ::std::endl;

    Log::out() 
        << "callee = " << methodDesc.getParentType()->getName() << "." << methodDesc.getName() 
        << ::std::endl
        << "caller = " << inlinedIRM.getParent()->getMethodDesc().getParentType()->getName() << "."
        << inlinedIRM.getParent()->getMethodDesc().getName()
        << ::std::endl;


    //
    // Splice the inlined region into the top-level flowgraph
    //
    IRManager* parent = inlinedIRM.getParent();
    assert(parent);
    
    Edge* edgeToInlined = callNode->getUnconditionalEdge();
    ControlFlowGraph& parentCFG = parent->getFlowGraph();
    parentCFG.spliceFlowGraphInline(edgeToInlined, inlinedFlowGraph);

    if (inlinedFlowGraph.getUnwindNode() == NULL) {
        // Replace original call with PseudoThrow to keep graph topology.
        callNode->appendInst(_instFactory.makePseudoThrow());
        isPseudoThrowInserted = true;
    } else {
        // Inlined graph has exception path -> remove original edge.
        Edge* exceptionEdge = callNode->getExceptionEdge(); 
        //exception edge can be NULL because the call inserted instead of the HIR inst is artificial
        if (exceptionEdge) {
            parentCFG.removeEdge(exceptionEdge);
        }
    }
    callInst->unlink();
}


void Inliner::runTranslatorSession(CompilationContext& inlineCC) {
    TranslatorSession* traSession = (TranslatorSession*)translatorAction->createSession(inlineCC.getCompilationLevelMemoryManager());
    traSession->setCompilationContext(&inlineCC);
    inlineCC.setCurrentSessionAction(traSession);
    traSession->run();
    inlineCC.setCurrentSessionAction(NULL);
}

InlineNode*
Inliner::getNextRegionToInline(CompilationContext& inlineCC) {
    // If in DPGO profiling mode, don't inline if profile information is not available.
    if(_doProfileOnlyInlining && !_toplevelIRM.getFlowGraph().hasEdgeProfile()) {
        return NULL;
    }
    
    Node* callNode = 0;
    InlineNode* inlineParentNode = 0;
    MethodCallInst* call = 0;
    MethodDesc* methodDesc = 0;
    U_32 newByteSize = 0;
    
    //
    // Search priority queue for next inline candidate
    //
    bool found = false;
    while(!_inlineCandidates.empty() && !found) {
        // Find new candidate.
        callNode = _inlineCandidates.top().callNode;
        inlineParentNode = _inlineCandidates.top().inlineNode;
        _inlineCandidates.pop();

        call = ((Inst*)callNode->getLastInst())->asMethodCallInst();
        assert(call != NULL);
        methodDesc = call->getMethodDesc();

        // If candidate would cause top level method to exceed size threshold, throw away.
        
        U_32 methodByteSize = methodDesc->getByteCodeSize();
        methodByteSize = (methodByteSize <= CALL_COST) ? 1 : methodByteSize-CALL_COST;
        newByteSize = _currentByteSize + methodByteSize;
        double factor = ((double) newByteSize) / ((double) _initByteSize);
        if(newByteSize < _minInlineStop || factor <= _maxInlineGrowthFactor || (methodByteSize < _inlineSmallMaxByteSize)) {
            found = true;
        } else {
            Log::out() << "Skip inlining " << methodDesc->getParentType()->getName() << "." << methodDesc->getName() << ::std::endl;
            Log::out() << "methodByteSize=" 
                                   << (int)methodByteSize
                                   << ", newByteSize = " << (int)newByteSize
                                   << ", factor=" << factor
                                   << std::endl;
        }
    }
    
    if(!found) {
        // No more candidates.  Done with inlining.
        assert(_inlineCandidates.empty());
        Log::out() << "Done inlining " << std::endl;
        return NULL;
    }
    
    // Set candidate as current inlined region
    Log::out() << "Opt:   Inline " << methodDesc->getParentType()->getName() << "." << methodDesc->getName() << 
        methodDesc->getSignatureString() << std::endl;

    // Generate flowgraph for new region
    InlineNode* inlineNode = createInlineNode(inlineCC, call);
    assert(inlineNode!=NULL);
    inlineParentNode->addChild(inlineNode);

    _currentByteSize = newByteSize;
    return inlineNode;
}

InlineNode* Inliner::createInlineNode(CompilationContext& inlineCC, MethodCallInst* call) {
    MethodDesc *methodDesc = call->getMethodDesc();
    IRManager* inlinedIRM = new (_tmpMM) IRManager(_tmpMM, _toplevelIRM, *methodDesc, NULL);
    InlineNode *inlineNode = new (_tmpMM) InlineNode(*inlinedIRM, call, call->getNode(), false);
    
    inlineCC.setHIRManager(inlinedIRM);
    
    //prepare type info
    U_32 nArgs = call->getNumSrcOperands() - 2;
    Type** types = new (_tmpMM)Type*[nArgs];
    for (U_32 i = 0; i < nArgs; i++) {
        types[i] = call->getSrc(i + 2)->getType();
    }

    InliningContext* ic = new (_tmpMM) InliningContext(nArgs, types);
    inlineCC.setInliningContext(ic);
    runTranslatorSession(inlineCC);

    return inlineNode;
}

void
Inliner::processDominatorNode(InlineNode *inlineNode, DominatorNode* dnode, LoopTree* ltree) {
    if(dnode == NULL)
        return;

    //
    // Process this node for inline candidates
    //
    Node* node = dnode->getNode();
    if(node->isBlockNode()) {
        // Search for call site
        Inst* last =(Inst*)node->getLastInst();
        if(last->getOpcode() == Op_DirectCall) {
            // Process call site
            MethodCallInst* call = last->asMethodCallInst();
            assert(call != NULL);
            MethodDesc* methodDesc = call->getMethodDesc();

            Log::out() << "Considering inlining instruction I" << (int)call->getId() << ::std::endl;
            if (usePriorityQueue && canInlineInto(*methodDesc)) {
                U_32 size = methodDesc->getByteCodeSize();
                I_32 benefit = computeInlineBenefit(node, *methodDesc, inlineNode, ltree->getLoopDepth(node));
                assert(size > 0);
                Log::out() << "Inline benefit " << methodDesc->getParentType()->getName() << "." << methodDesc->getName() << " == " << (int) benefit << ::std::endl;
                if(0 < size && benefit > _minBenefitThreshold) {
                    // Inline candidate
                    Log::out() << "Add to queue" << std::endl;
                    _inlineCandidates.push(CallSite(benefit, node, inlineNode));
                } else {
                    Log::out() << "Will not inline" << std::endl;
                }
            }
        }
    }

    //
    // Skip exception control flow unless directed
    //
    if(node->isBlockNode() || !_inlineSkipExceptionPath)
        processDominatorNode(inlineNode, dnode->getChild(), ltree);

    // 
    // Process siblings in dominator tree
    //
    processDominatorNode(inlineNode, dnode->getSiblings(), ltree);
}

void
Inliner::processRegion(InlineNode* inlineNode, DominatorTree* dtree, LoopTree* ltree) {
    if(!canInlineFrom(inlineNode->getIRManager().getMethodDesc()))
        return;

    //
    // Process region for inline candidates
    //
    processDominatorNode(inlineNode, dtree->getDominatorRoot(), ltree);
}

void 
Inliner::scaleBlockCounts(Node* callSite, IRManager& inlinedIRM) {
    Log::out() << "Scaling block counts for callsite in block "
                           << (int) callSite->getId() << ::std::endl;
    if(!_toplevelIRM.getFlowGraph().hasEdgeProfile()) {
        // No profile information to scale
        Log::out() << "No profile information to scale!" << ::std::endl;
        return;
    }

    //
    // Compute scale from call site count and inlined method count
    //
    double callFreq = callSite->getExecCount();
    ControlFlowGraph &inlinedRegion = inlinedIRM.getFlowGraph();
    double methodFreq = inlinedRegion.getEntryNode()->getExecCount();
    double scale = callFreq / ((methodFreq != 0.0) ? methodFreq : 1.0);

    Log::out() << "callFreq=" << callFreq
                           << ", methodFreq=" << methodFreq
                           << ", scale=" << scale << ::std::endl;
    //
    // Apply scale to each block in inlined region
    //
    const Nodes& nodes = inlinedRegion.getNodes();
    Nodes::const_iterator i;
    for(i = nodes.begin(); i != nodes.end(); ++i) {
        Node* node = *i;
        node->setExecCount(node->getExecCount()*scale);
    }
}


double 
Inliner::getProfileMethodCount(CompilationInterface& compileIntf, MethodDesc& methodDesc) {
    //
    // Get the entry count for a given method 
    //
    CompilationContext* cc = compileIntf.getCompilationContext();
    if ( cc->hasDynamicProfileToUse() ) {
        // Online
        double res = cc->getProfilingInterface()->getProfileMethodCount(methodDesc);
        return res;
    } else {
        return 0;
    }
}


void 
InlineNode::print(::std::ostream& os) {
    MethodDesc& _methodDesc = getIRManager().getMethodDesc();
    os << _methodDesc.getParentType()->getName() << "." << _methodDesc.getName() << _methodDesc.getSignatureString();
}

void 
InlineNode::printTag(::std::ostream& os) {
    MethodDesc& _methodDesc = getIRManager().getMethodDesc();
    os << "M" << (int) _methodDesc.getId(); 

}



U_32
InlineTree::computeCheckSum(InlineNode* node) {
    if(node == NULL)
        return 0;

    IRManager& irm = node->getIRManager();
    MethodDesc& desc = irm.getMethodDesc();
#ifdef PLATFORM_POSIX
    __gnu_cxx::hash<const char*> hash;
#else
    stdext::hash_compare<const char*> hash; 
#endif
    size_t sum = hash(desc.getParentType()->getName());
    sum += hash(desc.getName());
    sum += computeCheckSum(node->getSiblings());
    sum += computeCheckSum(node->getChild());
    return (U_32) sum;
}

void Inliner::runInlinerPipeline(CompilationContext& inlineCC, const char* pipeName) {
    PMF::HPipeline p = inlineCC.getCurrentJITContext()->getPMF().getPipeline(pipeName);
    assert(p!=NULL);
    PMF::PipelineIterator pit(p);
    while (pit.next()) {
        SessionAction* sa = pit.getSessionAction();
        sa->setCompilationContext(&inlineCC);
        inlineCC.setCurrentSessionAction(sa);
        inlineCC.stageId++;
        sa->run();
        inlineCC.setCurrentSessionAction(0);
        assert(!inlineCC.isCompilationFailed() &&  !inlineCC.isCompilationFinished());
    }
}

typedef StlVector<MethodCallInst*> InlineVector;

void Inliner::runInliner(MethodCallInst* call) {
    CompilationContext* topCC = _toplevelIRM.getCompilationContext();
    CompilationInterface* ci = topCC->getVMCompilationInterface();
    InlineNode* rootRegionNode = (InlineNode*) getInlineTree().getRoot();
    bool first = true;
    do {
        InlineNode* regionNode = NULL;
        {
            CompilationContext inlineCC(topCC->getCompilationLevelMemoryManager(), ci, topCC->getCurrentJITContext());
            inlineCC.setPipeline(topCC->getPipeline());//workaround for logging issue. Pipeline must not be NULL
            if (first) {
                if (call == NULL) { //if method is null -> check all calls in top level method
                    regionNode = rootRegionNode;
                } else {
                    regionNode = createInlineNode(inlineCC, call); //if method is not null -> inline it first
                    rootRegionNode->addChild(regionNode);
                }
                first = false;
            } else {
                regionNode = getNextRegionToInline(inlineCC);
            }

            if (regionNode == NULL) {
                break;
            }
            compileAndConnectRegion(regionNode, inlineCC);
        }
        //inline current region
        inlineRegion(regionNode);
        // Limit inlining by node count. 
        if (_toplevelIRM.getFlowGraph().getNodeCount() > _inlineMaxNodeThreshold) {
            break;
        }
    } while (true);


    // Clean up phase.
    if (_toplevelIRM.getInSsa()) {
        DeadCodeEliminator dce(_toplevelIRM);
        dce.eliminateUnreachableCode();
        assert(_toplevelIRM.getInSsa());
        OptPass::fixupSsa(_toplevelIRM);
        if (isPseudoThrowInserted && _toplevelIRM.getOptimizerFlags().rept_aggressive) {
            dce.removeExtraPseudoThrow();
        }
    }
}

void Inliner::compileAndConnectRegion(InlineNode* inlineNode, CompilationContext& inlineCC) {
    MethodCallInst* call = inlineNode->getCallInst()!=NULL ? inlineNode->getCallInst()->asMethodCallInst() : NULL;
    if (call!=NULL) { // processing custom call in a method
        IRManager &regionManager = inlineNode->getIRManager();
        assert(inlineCC.getHIRManager() == &regionManager);

        // Connect region arguments to top-level flowgraph
        if(connectEarly) {
            connectRegion(inlineNode);
        }

        // Optimize inlined region before splicing
        if (inlinerPipelineName!=NULL) {
            CompilationContext* topCC = regionManager.getCompilationContext();
            inlineCC.stageId = topCC->stageId;
            Inliner::runInlinerPipeline(inlineCC, inlinerPipelineName);
            topCC->stageId = inlineCC.stageId;
        }

        // Splice into flow graph and find next region.
        if(!connectEarly) {
            connectRegion(inlineNode);
        }
        OptPass::computeDominatorsAndLoops(regionManager);
    } 
}

void Inliner::processInlinePragmas(IRManager& irm) {
    // VMMagic package should be loaded & resolved at start up.  
    NamedType* inlinePragma = irm.getCompilationInterface().findClassUsingBootstrapClassloader(PRAGMA_INLINE_TYPE_NAME);
    if (inlinePragma == NULL) {
        if (Log::isEnabled()) {
            Log::out()<<"@Inline is not resolved. Skipping @Inline check."<<std::endl;
        }
        return; //pragma inline is not resolved -> nothing to check
    }

    MemoryManager tmpMM("processInlinePragmas");
    Inliner inliner(irm.getCompilationContext()->getCurrentSessionAction(), tmpMM, irm, false, false, NULL);
    StlVector<MethodCallInst*> callsToInline(tmpMM);

    //find all methods with @Inline
    const Nodes& nodes = irm.getFlowGraph().getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        for (Inst* inst = (Inst*)node->getFirstInst(); inst!=NULL; inst = inst->getNextInst()) {
            if (!inst->isMethodCall()) {
                continue;
            }
            MethodCallInst* mci = inst->asMethodCallInst();
            MethodDesc* md = mci->getMethodDesc();
            if (md != NULL && md->hasAnnotation(inlinePragma)) {
                callsToInline.push_back(mci);
                if (Log::isEnabled()) {
                    Log::out()<<"found @Inline :";mci->print(Log::out()); Log::out()<<std::endl;
                }
            }
        }
    }

    //now inline all all @Inline methods found 
    for(StlVector<MethodCallInst*>::const_iterator it = callsToInline.begin(), end = callsToInline.end(); it!=end; ++it) {
        MethodCallInst* call = *it;
        inliner.runInliner(call);
    }
}

void InlinePass::_run(IRManager& irm) {

    computeDominatorsAndLoops(irm);

    // Set up Inliner
    bool connectEarly = getBoolArg("connect_early", true);
    const char* pipeName = getStringArg("pipeline", "inliner_pipeline");

    MemoryManager tmpMM("Inliner::tmp_mm");
    Inliner inliner(this, tmpMM, irm, irm.getFlowGraph().hasEdgeProfile(), true, pipeName);
    inliner.setConnectEarly(connectEarly);
    inliner.runInliner(NULL); //call is unspecified -> check all calls using priority queue
    
    const OptimizerFlags& optimizerFlags = irm.getOptimizerFlags();
    // Print the results to logging / dot file
    if(optimizerFlags.dumpdot) {
        inliner.getInlineTree().printDotFile(irm.getMethodDesc(), "inlinetree");
    }
    if(Log::isEnabled()) {
        Log::out()<<std::endl;
        Log::out() << indent(irm) << "Opt: Inline Tree" << std::endl;
        inliner.getInlineTree().printIndentedTree(Log::out(), "  ");
    }
   Log::out() << "Inline Checksum == " << (int) inliner.getInlineTree().computeCheckSum() << ::std::endl;
}


} //namespace Jitrino
 
