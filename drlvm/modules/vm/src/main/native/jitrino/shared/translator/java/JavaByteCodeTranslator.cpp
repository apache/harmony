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

#include "Stl.h"
#include "JavaByteCodeTranslator.h"
#include "JavaTranslator.h"
#include "Log.h"
#include "ExceptionInfo.h"
#include "simplifier.h"
#include "methodtable.h"
#include "open/bytecodes.h"
#include "irmanager.h"
#include "Jitrino.h"
#include "EMInterface.h"
#include "inliner.h"
#include "VMMagic.h"

#include <assert.h>
#include <stdio.h>

namespace Jitrino {

static bool matchType(const char* candidate, const char* typeName) {
    if (candidate[0]=='L') {
        size_t typeLen = strlen(typeName);
        size_t candLen = strlen(candidate);
        bool res = typeLen+2 == candLen && !strncmp(typeName, candidate+1, typeLen);
        return res;
    }
    return !strcmp(typeName, candidate);
}

static Type* convertVMMagicType2HIR(TypeManager& tm, const char* name) {
    assert(VMMagicUtils::isVMMagicClass(name));

    if (matchType(name, "org/vmmagic/unboxed/Address")
        //TODO: ObjectReference must have a ManagedPointer/Object type
        || matchType(name, "org/vmmagic/unboxed/ObjectReference"))
    {
        return tm.getUnmanagedPtrType(tm.getInt8Type());
    } else if (matchType(name, "org/vmmagic/unboxed/Word")
        || matchType(name, "org/vmmagic/unboxed/Offset")
        || matchType(name, "org/vmmagic/unboxed/Extent"))
    {
        return tm.getUIntPtrType();
    } else if (matchType(name, "org/vmmagic/unboxed/WordArray")
        || matchType(name, "org/vmmagic/unboxed/OffsetArray")
        || matchType(name, "org/vmmagic/unboxed/ExtentArray") 
        || matchType(name, "org/vmmagic/unboxed/AddressArray") 
        || matchType(name, "org/vmmagic/unboxed/ObjectReferenceArray")) 
    {
#ifdef _EM64T_
        return tm.getArrayType(tm.getInt64Type(), false);
#else 
        return tm.getArrayType(tm.getInt32Type(), false);
#endif
    }
    assert(0);
    return NULL;
}

static Type* convertVMMagicType2HIR(TypeManager& tm, Type* type) {
    if (!type->isObject() || !type->isNamedType() || type->isSystemObject()) {
        return type;
    }
    const char* name = type->getName();    
    return convertVMMagicType2HIR(tm, name);
}

//vm helpers support

static bool isVMHelperClass(const char* name) {
#ifdef _IPF_
    return false;//natives are not tested on IPF.
#else
    bool res =  !strcmp(name, VMHELPER_TYPE_NAME);
    return res;
#endif
}


//-----------------------------------------------------------------------------
// JavaByteCodeTranslator constructors
//-----------------------------------------------------------------------------
// version for non-inlined methods

JavaByteCodeTranslator::JavaByteCodeTranslator(CompilationInterface& ci,
                                 MemoryManager& mm,
                                 IRBuilder& irb,
                                 ByteCodeParser& bcp,
                                 MethodDesc& methodDesc,
                                 TypeManager& typeManager,
                                 JavaFlowGraphBuilder& cfg)
    : 
      memManager(mm), 
      compilationInterface(ci), 
      methodToCompile(methodDesc), 
      parser(bcp), 
      typeManager(*irb.getTypeManager()), 
      irBuilder(irb),
      translationFlags(*irb.getTranslatorFlags()),
      cfgBuilder(cfg),
      // CHECK ? for static sync methods must ensure at least one slot on stack for monitor enter/exit code
      opndStack(mm,methodDesc.getMaxStack()+1),
      returnOpnd(NULL),
      returnNode(NULL),
      inliningExceptionInfo(NULL),
      prepass(memManager,
              typeManager,
              irBuilder.getInstFactory()->getMemManager(),
              methodDesc,
              ci,
              NULL),
      lockAddr(NULL), 
      oldLockValue(NULL),
      jsrEntryMap(NULL),
      retOffsets(mm),
      jsrEntryOffsets(mm)
{
    initJsrEntryMap();
    // create a prolog block 
    cfgBuilder.genBlock(irBuilder.genMethodEntryLabel(&methodDesc));
    initLocalVars();
    initArgs();
    CompilationContext* cc = CompilationContext::getCurrentContext();
    InliningContext* ic  = cc->getInliningContext();
    //
    // load actual parameters into formal parameters
    //
    for (U_32 i=0,j=0; i<numArgs; i++,j++) {
        //
        // for Java this is the same as a local var!
        //
        Opnd *arg;
        if (i == 0 && methodToCompile.isStatic() == false) {
            //
            // for non-inlined, non-static methods, 'this' pointer should have non-null property set
            //
            arg = irBuilder.genArgDef(NonNullThisArg,argTypes[i]);
        } else {
            Type* type = argTypes[i];
            if (ic!=NULL) {
                assert(ic->getNumArgs() == numArgs);
                Type* newType = ic->getArgTypes()[i];
                if (newType->isObject()) {
                    assert(newType->isObject() == type->isObject());
                    assert(newType->isNullObject() || newType->isUnresolvedType()  || type->isUnresolvedType()
                        || newType->asObjectType()->isSubClassOf(type->asObjectType()) || newType->isSystemObject());
                    type = newType;
                } else {
                    //TODO: numX->numY auto conversion not tested
                }
            } 
            if (VMMagicUtils::isVMMagicClass(type->getName())) {
                type = convertVMMagicType2HIR(typeManager, type);
            }
            arg = irBuilder.genArgDef(DefArgNoModifier,type);
        }
        JavaLabelPrepass::JavaVarType javaType = JavaLabelPrepass::getJavaType(argTypes[i]);
        VarOpnd *var = getVarOpndStVar(javaType,j,arg);
        //
        // longs & doubles take up 2 slots
        //
        if (javaType==JavaLabelPrepass::L || javaType==JavaLabelPrepass::D) 
            j++;
        if (var != NULL)
            irBuilder.genStVar(var,arg);
        stateInfo->stack[j].vars = new (memManager) SlotVar(prepass.getVarInc(0, j));
    }
    // check for synchronized methods
    if (methodToCompile.isSynchronized()) {
        if (methodToCompile.isStatic()) {
            //irBuilder.genTypeMonitorEnter(methodToCompile.getParentType());
            Opnd* classObjectOpnd = irBuilder.genGetClassObj((ObjectType*) methodToCompile.getParentType());
            pushOpnd(classObjectOpnd);
            genMethodMonitorEnter();
        } else {
            genLdVar(0,JavaLabelPrepass::A);
            genMethodMonitorEnter();
        }
    }

    if(!prepass.allExceptionTypesResolved()) {
        unsigned problemToken = prepass.getProblemTypeToken();
        assert(problemToken != MAX_UINT32);
        linkingException(problemToken,OPCODE_CHECKCAST); // CHECKCAST is suitable here
        noNeedToParse = true;
    }
}


//-----------------------------------------------------------------------------
// initialization helpers
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::initJsrEntryMap()
{
    MemoryManager& ir_mem_manager = irBuilder.getIRManager()->getMemoryManager();
    jsrEntryMap = new (ir_mem_manager) JsrEntryInstToRetInstMap(ir_mem_manager);
}

void 
JavaByteCodeTranslator::initLocalVars() {
    //
    // perform label prepass in this method
    //
    parser.parse(&prepass);
    prepass.parseDone();
    prepassVisited = prepass.bytecodevisited;
    moreThanOneReturn = false;
    jumpToTheEnd      = false;
    lastInstructionWasABranch = false;

    // compute number of labels
    numLabels = prepass.getNumLabels();
    labels = new (memManager) LabelInst*[numLabels+1];
    irBuilder.createLabels(numLabels,labels);

    nextLabel = 0;
    resultOpnd = NULL;

    numVars = methodToCompile.getNumVars();
    numStackVars = prepass.getStateTable()->getMaxStackOverflow()-numVars;
    stateInfo = &prepass.stateInfo;
    for (U_32 k=0; k < numVars+numStackVars; k++) {
        struct StateInfo::SlotInfo *slot = &stateInfo->stack[k];
        slot->type = NULL;
        slot->slotFlags = 0;
        slot->vars = NULL;
    }
    stateInfo->stackDepth = numVars;
    javaTypeMap[JavaLabelPrepass::I]   = typeManager.getInt32Type();
    javaTypeMap[JavaLabelPrepass::L]   = typeManager.getInt64Type();
    javaTypeMap[JavaLabelPrepass::F]   = typeManager.getSingleType();
    javaTypeMap[JavaLabelPrepass::D]   = typeManager.getDoubleType();
    javaTypeMap[JavaLabelPrepass::A]   = typeManager.getSystemObjectType();
    javaTypeMap[JavaLabelPrepass::RET] = typeManager.getIntPtrType();
    prepass.createMultipleDefVarOpnds(&irBuilder);

}

void 
JavaByteCodeTranslator::initArgs() {
    // incoming argument and return value information
    numArgs = methodToCompile.getNumParams();
    retType = methodToCompile.getReturnType();
    argTypes = new (memManager) Type*[numArgs];
    args = new (memManager) Opnd*[numArgs];
    for (uint16 i=0; i<numArgs; i++) {
        Type* argType = methodToCompile.getParamType(i);
        assert(!(typeManager.isLazyResolutionMode() && argType==NULL));
        // argType == NULL if it fails to be resolved. Respective exception
        // will be thrown at the point of usage
        argTypes[i] = argType != NULL ? argType : typeManager.getNullObjectType();
    }
}

//-----------------------------------------------------------------------------
// variable management helpers
//-----------------------------------------------------------------------------
// returns either VarOpnd or Opnd. If returning is operand, do not generate the LdVar, only
// push the operand
//
Opnd* 
JavaByteCodeTranslator::getVarOpndLdVar(JavaLabelPrepass::JavaVarType javaType,U_32 index) {
    if (index >= numVars+numStackVars)
        // error: invalid local variable id
        invalid();
    struct StateInfo::SlotInfo slot = stateInfo->stack[index];
    assert(slot.vars);
    Opnd* var = slot.vars->getVarIncarnation()->getOrCreateOpnd(&irBuilder);
    return var;
}

// returns either VarOpnd or null. If null, does not generate the StVar

VarOpnd* 
JavaByteCodeTranslator::getVarOpndStVar(JavaLabelPrepass::JavaVarType javaType,
                                        U_32 index, 
                                        Opnd* opnd) {
    if (index >= numVars+numStackVars)
        // error: invalid local variable id
        invalid();
    VariableIncarnation* varInc = prepass.getVarInc(currentOffset, index);
    Opnd* var = NULL;
    StateInfo::SlotInfo* slot = NULL;
    slot = &stateInfo->stack[index];
    if(varInc) {
        slot->vars = new (memManager) SlotVar(varInc);
        var = varInc->getOpnd();
    }

    if (var) {
        assert(var->isVarOpnd());
        return var->asVarOpnd();
    } else {
        slot->type = typeManager.toInternalType(opnd->getType());
        slot->slotFlags = 0;
        if (isNonNullOpnd(opnd))
            StateInfo::setNonNull(slot);
        if (isExactTypeOpnd(opnd))
            StateInfo::setExactType(slot);
        varInc->setTmpOpnd(opnd);
        return NULL;
    }
}

//-----------------------------------------------------------------------------
// operand stack manipulation helpers
//-----------------------------------------------------------------------------
void    
JavaByteCodeTranslator::pushOpnd(Opnd* opnd) {
    assert(opnd->getInst());
    opndStack.push(opnd);
}

Opnd*    
JavaByteCodeTranslator::topOpnd() {
    return opndStack.top();
}

Opnd*    
JavaByteCodeTranslator::popOpnd() {
    Opnd *top = opndStack.pop();
    setStackOpndAliveOpnd(top,true);
    return top;
}

Opnd*    
JavaByteCodeTranslator::popOpndStVar() {
    return opndStack.pop();
}

//
// Called at the end of each basic block to empty out the operand stack
//
void 
JavaByteCodeTranslator::checkStack() {
    int numElems = opndStack.getNumElems();
    for (int i = numElems-1; i >= 0; i--) {
        Opnd* opnd = popOpndStVar();
        JavaLabelPrepass::JavaVarType javaType = 
            JavaLabelPrepass::getJavaType(opnd->getType());

        VarOpnd* var = getVarOpndStVar(javaType,(uint16)(numVars+i),opnd);
        // simple optimization
        if(var != NULL) {
            Inst* srcInst = opnd->getInst();
            assert(srcInst);
            if ((srcInst->getOpcode() != Op_LdVar) || 
                (srcInst->getSrc(0)->getId() != var->getId())) {
                irBuilder.genStVar(var,opnd);
                setStackOpndAliveOpnd(opnd,true);
            } else {
            }
        }
    }
}
//-----------------------------------------------------------------------------
// constant pool resolution helpers
//-----------------------------------------------------------------------------

const char*
JavaByteCodeTranslator::methodSignatureString(U_32 cpIndex) {
    return compilationInterface.getSignatureString(&methodToCompile,cpIndex);
}

U_32 
JavaByteCodeTranslator::labelId(U_32 offset) {
    U_32 labelId = prepass.getLabelId(offset);
    if (labelId == (U_32) -1)
        jitrino_assert(0);
    return labelId;
}

//-----------------------------------------------------------------------------
// misc JavaByteCodeParserCallback methods
//-----------------------------------------------------------------------------

// called when invalid byte code is encountered
void 
JavaByteCodeTranslator::invalid() {
    jitrino_assert(0);
}

// called when an error occurs during the byte code parsing
void 
JavaByteCodeTranslator::parseError() {
    jitrino_assert(0);
}

void 
JavaByteCodeTranslator::offset(U_32 offset) {

    // set bc offset in ir builder
    irBuilder.setBcOffset(offset);
    if (prepass.isLabel(offset) == false)
        return;
    if (prepassVisited && prepassVisited->getBit(offset) == false) {
        getNextLabelId(); // skip this DEAD byte code
        return;
    }

    // finish the previous basic block, if any work was required
    if (!lastInstructionWasABranch) {
        checkStack();
    }
    lastInstructionWasABranch = false;

    // start with the current basic block
    StateInfo* state = prepass.stateTable->getStateInfo(offset);
    stateInfo->flags = state->flags;
    stateInfo->stackDepth = state->stackDepth;
    stateInfo->exceptionInfo = state->exceptionInfo;
    for(unsigned i=0; i<state->stackDepth; ++i)
        stateInfo->stack[i] = state->stack[i];
    assert(stateInfo != NULL);
    Type* handlerExceptionType = NULL;
    U_32 lblId = getNextLabelId();
    LabelInst* labelInst = getLabel(lblId);

    ::std::vector<LabelInst*> catchLabels;

    bool isCatchHandler = false;
    for (ExceptionInfo* exceptionInfo = stateInfo->getExceptionInfo();
         exceptionInfo != NULL;
         exceptionInfo = exceptionInfo->getNextExceptionInfoAtOffset()) {
        if (exceptionInfo->isCatchBlock()) {
            CatchBlock* catchBlock = (CatchBlock*)exceptionInfo;
            CatchHandler *first = ((CatchBlock*)exceptionInfo)->getHandlers();
            if (Log::isEnabled()) {
                Log::out() << "TRY REGION " << (int)exceptionInfo->getBeginOffset() 
                    << " " << (int)exceptionInfo->getEndOffset() << ::std::endl;
                for (; first != NULL; first = first->getNextHandler()) {
                    Log::out() << " handler " << (int)first->getBeginOffset() << ::std::endl;
                }
            }
            if (catchBlock->getLabelInst() == NULL) {
                Node *dispatchNode = cfgBuilder.createDispatchNode();
                catchBlock->setLabelInst((LabelInst*)dispatchNode->getFirstInst());
                ((LabelInst*)dispatchNode->getFirstInst())->setState(catchBlock);
            }
            if (labelInst->getState() == NULL) 
                labelInst->setState(catchBlock);
            if(Log::isEnabled()) {
                Log::out() << "LABEL "; labelInst->print(Log::out()); Log::out() << labelInst->getState();
                Log::out() << "CATCH ";catchBlock->getLabelInst()->print(Log::out()); Log::out() << ::std::endl;
            }
        } else if (exceptionInfo->isCatchHandler()) {
            // catch handler block
            isCatchHandler = true;
            CatchHandler* handler = (CatchHandler*)exceptionInfo;
            if (Log::isEnabled()) Log::out() << "CATCH REGION " << (int)exceptionInfo->getBeginOffset() 
                << " " << (int)exceptionInfo->getEndOffset() << ::std::endl;
            handlerExceptionType = (handlerExceptionType == NULL) ?
                handler->getExceptionType() :
                typeManager.getCommonObjectType((ObjectType*) handlerExceptionType, (ObjectType*) handler->getExceptionType());
            LabelInst *oldLabel = labelInst;
            labelInst = irBuilder.getInstFactory()->makeCatchLabel(
                                            handler->getExceptionOrder(),
                                            handler->getExceptionType());
            labelInst->setBCOffset((uint16)exceptionInfo->getBeginOffset());
            catchLabels.push_back(labelInst);
            labelInst->setState(oldLabel->getState());
            exceptionInfo->setLabelInst(labelInst);
            if(Log::isEnabled()) {
                Log::out() << "LABEL "; labelInst->print(Log::out()); Log::out() << labelInst->getState();
                Log::out() << "CATCH "; handler->getLabelInst()->print(Log::out()); Log::out() << ::std::endl;
            }
        } else {jitrino_assert(0);}    // only catch blocks should occur in Java
    }
    // generate the label instruction
    if(!catchLabels.empty()) {
        for(::std::vector<LabelInst*>::iterator iter = catchLabels.begin(); iter != catchLabels.end(); ++iter) {
            LabelInst* catchLabel = *iter;
            irBuilder.genLabel(catchLabel);
            cfgBuilder.genBlock(catchLabel);
        }
        LabelInst* handlerLabel= getLabel(lblId);
        assert(!handlerLabel->isCatchLabel());
        handlerLabel->setState(labelInst->getState());
        irBuilder.genLabel(handlerLabel);
        cfgBuilder.genBlock(handlerLabel);
    } else {
        if (stateInfo->isFallThroughLabel()) {
            irBuilder.genFallThroughLabel(labelInst);
        } else { 
            irBuilder.genLabel(labelInst);
            // empty out the stack operand
            opndStack.makeEmpty();
        }
        cfgBuilder.genBlock(labelInst);
    }

    if (isCatchHandler) {
        // for catch handler blocks, generate the catch instruction
        assert(stateInfo->isCatchLabel());
        assert(1 == stateInfo->stackDepth - numVars);
        assert(stateInfo->stack[numVars].type == handlerExceptionType);
        pushOpnd(irBuilder.genCatch(stateInfo->stack[numVars].type));
    } else {
        //
        // Load var operands where current basic block begins
        //
        for (U_32 k=numVars; k < (U_32)stateInfo->stackDepth; k++) {
            if(Log::isEnabled()) {
                Log::out() << "STACK ";StateInfo::print(stateInfo->stack[k], Log::out());Log::out() << ::std::endl;
            }
            genLdVar(k,prepass.getJavaType(stateInfo->stack[k].type));
        }
    }
    if (stateInfo->isSubroutineEntry()) {
        pushOpnd(irBuilder.genSaveRet());
    }
}

void 
JavaByteCodeTranslator::offset_done(U_32 offset) {
    if (prepass.isSubroutineEntry(offset) ) {
        jsrEntryOffsets[offset] = irBuilder.getLastGeneratedInst();
    }
}

//
// called when byte code parsing is done
//
void 
JavaByteCodeTranslator::parseDone() 
{
    OffsetToInstMap::const_iterator ret_i, ret_e;
    for (ret_i = retOffsets.begin(), ret_e = retOffsets.end(); ret_i != ret_e; ++ret_i) {
        U_32 ret_offset = ret_i->first;
        Inst* ret_inst = ret_i->second;
        JavaLabelPrepass::RetToSubEntryMap* ret_to_entry_map = prepass.getRetToSubEntryMapPtr();
        JavaLabelPrepass::RetToSubEntryMap::const_iterator sub_i = ret_to_entry_map->find(ret_offset);
        //
        // jsr target should be found for each ret inst
        //
        assert(sub_i != ret_to_entry_map->end());
        U_32 entry_offset = sub_i->second;
        OffsetToInstMap::const_iterator entry_inst_i = jsrEntryOffsets.find(entry_offset);
        assert(entry_inst_i != jsrEntryOffsets.end());
        Inst* entry_inst = entry_inst_i->second;
        jsrEntryMap->insert(std::make_pair(entry_inst, ret_inst));
    }
    irBuilder.getIRManager()->setJsrEntryMap(jsrEntryMap);
    if (Log::isEnabled()) Log::out() << ::std::endl << "================= TRANSLATOR IS FINISHED =================" << ::std::endl << ::std::endl;
}

//-----------------------------------------------------------------------------
// byte code callbacks
//-----------------------------------------------------------------------------

void JavaByteCodeTranslator::nop() {}

//-----------------------------------------------------------------------------
// load constant byte codes
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::aconst_null()     {
    pushOpnd(irBuilder.genLdNull());
}
void 
JavaByteCodeTranslator::iconst(I_32 val) {
    pushOpnd(irBuilder.genLdConstant(val));
}
void 
JavaByteCodeTranslator::lconst(int64 val) {
    pushOpnd(irBuilder.genLdConstant(val));
}
void 
JavaByteCodeTranslator::fconst(float val) {
    pushOpnd(irBuilder.genLdConstant(val));
}
void 
JavaByteCodeTranslator::dconst(double val){
    pushOpnd(irBuilder.genLdConstant(val));
}
void 
JavaByteCodeTranslator::bipush(I_8 val)  {
    pushOpnd(irBuilder.genLdConstant((I_32)val));
}
void 
JavaByteCodeTranslator::sipush(int16 val) {
    pushOpnd(irBuilder.genLdConstant((I_32)val));
}
void 
JavaByteCodeTranslator::ldc(U_32 constPoolIndex) {
    // load 32-bit quantity or string from constant pool
    Type* constantType = 
        compilationInterface.getConstantType(&methodToCompile,constPoolIndex);
    Opnd* opnd = NULL;
    if (constantType->isSystemString()) {
        opnd = irBuilder.genLdRef(&methodToCompile,constPoolIndex,constantType);
    } else if (constantType->isSystemClass()) {
        NamedType *literalType = compilationInterface.getNamedType(methodToCompile.getParentHandle(), constPoolIndex);
        if (!typeManager.isLazyResolutionMode() && literalType->isUnresolvedType()) {
            linkingException(constPoolIndex, OPCODE_LDC);
        }
        opnd = irBuilder.genLdRef(&methodToCompile,constPoolIndex,constantType);
    } else {
        const void* constantAddress =
           compilationInterface.getConstantValue(&methodToCompile,constPoolIndex);
        if (constantType->isInt4()) {
            I_32 value = *(I_32*)constantAddress;
            opnd = irBuilder.genLdConstant(value);
        } else if (constantType->isSingle()) {
            float value = *(float*)constantAddress;
            opnd = irBuilder.genLdConstant((float)value);
        } else {
            // Invalid type!
            jitrino_assert(0);
        }
    }
    pushOpnd(opnd);
}

void 
JavaByteCodeTranslator::ldc2(U_32 constPoolIndex) {
    // load 64-bit quantity from constant pool
    Type* constantType = 
        compilationInterface.getConstantType(&methodToCompile,constPoolIndex);

    const void* constantAddress =
        compilationInterface.getConstantValue(&methodToCompile,constPoolIndex);
    Opnd *opnd = NULL; 
    if (constantType->isInt8()) {
        int64 value = *(int64*)constantAddress;
        opnd = irBuilder.genLdConstant((int64)value);
    } else if (constantType->isDouble()) {
        double value = *(double*)constantAddress;
        opnd = irBuilder.genLdConstant((double)value);
    } else {
        // Invalid type!
        jitrino_assert(0);
    }
    pushOpnd(opnd);
}

//-----------------------------------------------------------------------------
// variable access byte codes
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::iload(uint16 varIndex) {
    genLdVar(varIndex,JavaLabelPrepass::I);
}
void 
JavaByteCodeTranslator::lload(uint16 varIndex) {
    genLdVar(varIndex,JavaLabelPrepass::L);
}
void 
JavaByteCodeTranslator::fload(uint16 varIndex) {
    genLdVar(varIndex,JavaLabelPrepass::F);
}
void 
JavaByteCodeTranslator::dload(uint16 varIndex) {
    genLdVar(varIndex,JavaLabelPrepass::D);
}
void 
JavaByteCodeTranslator::aload(uint16 varIndex) {
    genLdVar(varIndex,JavaLabelPrepass::A);
}
void 
JavaByteCodeTranslator::istore(uint16 varIndex,U_32 off) {
    genStVar(varIndex,JavaLabelPrepass::I);
}
void 
JavaByteCodeTranslator::lstore(uint16 varIndex,U_32 off) {
    genStVar(varIndex,JavaLabelPrepass::L);
}
void 
JavaByteCodeTranslator::fstore(uint16 varIndex,U_32 off) {
    genStVar(varIndex,JavaLabelPrepass::F);
}
void 
JavaByteCodeTranslator::dstore(uint16 varIndex,U_32 off) {
    genStVar(varIndex,JavaLabelPrepass::D);
}
void 
JavaByteCodeTranslator::astore(uint16 varIndex,U_32 off) {
    genTypeStVar(varIndex);
}
//-----------------------------------------------------------------------------
// field access byte codes
//-----------------------------------------------------------------------------
Type* 
JavaByteCodeTranslator::getFieldType(FieldDesc* field, U_32 constPoolIndex) {
    Type* fieldType = field->getFieldType();
    if (!fieldType) {
        // some problem with fieldType class handle. Let's try the constant_pool.
        // (For example if the field type class is deleted, the field is beeing resolved successfully
        // but field->getFieldType() returns NULL in this case)
        fieldType = compilationInterface.getFieldType(methodToCompile.getParentHandle(),constPoolIndex);
    }
    return fieldType;
}


void 
JavaByteCodeTranslator::getstatic(U_32 constPoolIndex) {
    FieldDesc *field = compilationInterface.getStaticField(methodToCompile.getParentHandle(), constPoolIndex, false);
    if (field && field->isStatic()) {
        bool fieldValueInlined = false;
        Type* fieldType = field->getFieldType();
        assert(fieldType);
        bool fieldIsMagic = VMMagicUtils::isVMMagicClass(fieldType->getName());
        if (fieldIsMagic) {
            fieldType = convertVMMagicType2HIR(typeManager, fieldType);
        }
        if (field->isInitOnly() && !field->getParentType()->needsInitialization()) {
            //the final static field of the initialized class
            if (field->getFieldType()->isNumeric() || field->getFieldType()->isBoolean() || fieldIsMagic) {
                Opnd* constVal = NULL;
                void* fieldAddr = field->getAddress();
                switch(fieldType->tag) {
                    case Type::Int8 :   constVal=irBuilder.genLdConstant(*(I_8*)fieldAddr);break;
                    case Type::Int16:   constVal=irBuilder.genLdConstant(*(int16*)fieldAddr);break;
                    case Type::Char :   constVal=irBuilder.genLdConstant(*(uint16*)fieldAddr);break;
                    case Type::Int32:   constVal=irBuilder.genLdConstant(*(I_32*)fieldAddr);break;
                    case Type::Int64:   constVal=irBuilder.genLdConstant(*(int64*)fieldAddr);break;
                    case Type::Single:  constVal=irBuilder.genLdConstant(*(float*)fieldAddr);break;
                    case Type::Double:  constVal=irBuilder.genLdConstant(*(double*)fieldAddr);break;
                    case Type::Boolean: constVal=irBuilder.genLdConstant(*(bool*)fieldAddr);break;
                    case Type::UnmanagedPtr:  assert(fieldIsMagic); 
#ifdef _IA32_
                            constVal=irBuilder.genLdConstant(*(I_32*)fieldAddr);
#else
                            assert(sizeof(void*)==8);
                            constVal=irBuilder.genLdConstant(*(int64*)fieldAddr);
#endif
                            break;
                    default: assert(0); //??
                }
                if (constVal != NULL) {
                    pushOpnd(constVal);
                    fieldValueInlined = true;
                }
            }
        } 
        if (!fieldValueInlined){
            pushOpnd(irBuilder.genLdStatic(fieldType, field));
        }
    } else {
        //field is not resolved or not static
        if (!typeManager.isLazyResolutionMode()) {
            // generate helper call for throwing respective exception
            linkingException(constPoolIndex, OPCODE_GETSTATIC);
        }
        const char* fieldTypeName = CompilationInterface::getFieldSignature(methodToCompile.getParentHandle(), constPoolIndex);
        bool fieldIsMagic = VMMagicUtils::isVMMagicClass(fieldTypeName);
        Type* fieldType = fieldIsMagic ? convertVMMagicType2HIR(typeManager, fieldTypeName) 
                                       : compilationInterface.getFieldType(methodToCompile.getParentHandle(), constPoolIndex);
        Opnd* res = irBuilder.genLdStaticWithResolve(fieldType, methodToCompile.getParentType()->asObjectType(), constPoolIndex);
        pushOpnd(res);
    }
}

void 
JavaByteCodeTranslator::putstatic(U_32 constPoolIndex) {
    FieldDesc *field = compilationInterface.getStaticField(methodToCompile.getParentHandle(), constPoolIndex, true);
    if (field && field->isStatic()) {
        Type* fieldType = getFieldType(field,constPoolIndex);
        assert(fieldType);
        bool fieldIsMagic = VMMagicUtils::isVMMagicClass(fieldType->getName());
        if (fieldIsMagic) {
            fieldType = convertVMMagicType2HIR(typeManager, fieldType);
        }
        irBuilder.genStStatic(fieldType,field,popOpnd());
    } else {
        //field is not resolved or not static
        if (!typeManager.isLazyResolutionMode()) {
            // generate helper call for throwing respective exception
            linkingException(constPoolIndex, OPCODE_PUTSTATIC);
        }
        const char* fieldTypeName = CompilationInterface::getFieldSignature(methodToCompile.getParentHandle(), constPoolIndex);
        bool fieldIsMagic = VMMagicUtils::isVMMagicClass(fieldTypeName);
        Type* fieldType = fieldIsMagic ? convertVMMagicType2HIR(typeManager, fieldTypeName) 
                                       : compilationInterface.getFieldType(methodToCompile.getParentHandle(), constPoolIndex);
        Opnd* value = popOpnd();
        irBuilder.genStStaticWithResolve(fieldType, methodToCompile.getParentType()->asObjectType(), constPoolIndex, value);
    }
}

void 
JavaByteCodeTranslator::getfield(U_32 constPoolIndex) {
    FieldDesc *field = compilationInterface.getNonStaticField(methodToCompile.getParentHandle(), constPoolIndex, false);
    if (field && !field->isStatic()) {
        Type* fieldType = getFieldType(field, constPoolIndex);
        if (VMMagicUtils::isVMMagicClass(fieldType->getName())) {
            fieldType = convertVMMagicType2HIR(typeManager, fieldType);
        }
        pushOpnd(irBuilder.genLdField(fieldType,popOpnd(),field));
    } else {
        if (!typeManager.isLazyResolutionMode()) {
            // generate helper call for throwing respective exception
            linkingException(constPoolIndex, OPCODE_GETFIELD);
        }
        Type* fieldType = compilationInterface.getFieldType(methodToCompile.getParentHandle(), constPoolIndex);
        if (VMMagicUtils::isVMMagicClass(fieldType->getName())) {
            fieldType = convertVMMagicType2HIR(typeManager, fieldType);
        }
        Opnd* base = popOpnd();
        Opnd* res = irBuilder.genLdFieldWithResolve(fieldType, base, methodToCompile.getParentType()->asObjectType(), constPoolIndex);
        pushOpnd(res);
    }
}

void 
JavaByteCodeTranslator::putfield(U_32 constPoolIndex) {
    FieldDesc *field = compilationInterface.getNonStaticField(methodToCompile.getParentHandle(), constPoolIndex, true);
    if (field && !field->isStatic()) {
        Type* fieldType = getFieldType(field,constPoolIndex);
        assert(fieldType);
        if (VMMagicUtils::isVMMagicClass(fieldType->getName())) {
            fieldType = convertVMMagicType2HIR(typeManager, fieldType);
        }

        Opnd* value = popOpnd();
        Opnd* ref = popOpnd();
        irBuilder.genStField(fieldType,ref,field,value);
    } else {
        if (!typeManager.isLazyResolutionMode()) {
            // generate helper call for throwing respective exception
            linkingException(constPoolIndex, OPCODE_PUTFIELD);
        }
        Type* type = compilationInterface.getFieldType(methodToCompile.getParentHandle(), constPoolIndex);
        Opnd* value = popOpnd();
        Opnd* base = popOpnd();
        irBuilder.genStFieldWithResolve(type, base, methodToCompile.getParentType()->asObjectType(), constPoolIndex, value);
    }
}
//-----------------------------------------------------------------------------
// array access byte codes
//-----------------------------------------------------------------------------
void JavaByteCodeTranslator::iaload() {
    genArrayLoad(typeManager.getInt32Type());
}
void JavaByteCodeTranslator::laload() {
    genArrayLoad(typeManager.getInt64Type());
}
void JavaByteCodeTranslator::faload() {
    genArrayLoad(typeManager.getSingleType());
}
void JavaByteCodeTranslator::daload() {
    genArrayLoad(typeManager.getDoubleType());
}
void JavaByteCodeTranslator::aaload() {
    genTypeArrayLoad();
}
void JavaByteCodeTranslator::baload() {
    genArrayLoad(typeManager.getInt8Type());
}
void JavaByteCodeTranslator::caload() {
    genArrayLoad(typeManager.getCharType());
}
void JavaByteCodeTranslator::saload() {
    genArrayLoad(typeManager.getInt16Type());
}
void JavaByteCodeTranslator::iastore() {
    genArrayStore(typeManager.getInt32Type());
}
void JavaByteCodeTranslator::lastore() {
    genArrayStore(typeManager.getInt64Type());
}
void JavaByteCodeTranslator::fastore() {
    genArrayStore(typeManager.getSingleType());
}
void JavaByteCodeTranslator::dastore() {
    genArrayStore(typeManager.getDoubleType());
}
void JavaByteCodeTranslator::aastore() {
    genTypeArrayStore();
}
void JavaByteCodeTranslator::bastore() {
    genArrayStore(typeManager.getInt8Type());
}
void JavaByteCodeTranslator::castore() {
    genArrayStore(typeManager.getCharType());
}
void JavaByteCodeTranslator::sastore() {
    genArrayStore(typeManager.getInt16Type());
}
//-----------------------------------------------------------------------------
// stack manipulation byte codes (pops, dups & exchanges)
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::pop() {popOpnd();}

bool 
isCategory2(Opnd* opnd) {
    return (opnd->getType()->isInt8() || opnd->getType()->isDouble());
}

void 
JavaByteCodeTranslator::pop2() {
    Opnd* opnd = popOpnd();
    if (isCategory2(opnd))
        return;
    popOpnd();
}

void 
JavaByteCodeTranslator::dup() {
    pushOpnd(topOpnd());
}

void 
JavaByteCodeTranslator::dup_x1() {
    Opnd* opnd1 = popOpnd();
    Opnd* opnd2 = popOpnd();
    pushOpnd(opnd1);
    pushOpnd(opnd2);
    pushOpnd(opnd1);
}

void 
JavaByteCodeTranslator::dup_x2() {
    Opnd* opnd1 = popOpnd();
    Opnd* opnd2 = popOpnd();
    if (isCategory2(opnd2)) {
        pushOpnd(opnd1);
        pushOpnd(opnd2);
        pushOpnd(opnd1);
        return;
    }
    Opnd* opnd3 = popOpnd();
    pushOpnd(opnd1);
    pushOpnd(opnd3);
    pushOpnd(opnd2);
    pushOpnd(opnd1);
}

void 
JavaByteCodeTranslator::dup2() {
    Opnd* opnd1 = popOpnd();
    if (isCategory2(opnd1)) {
        pushOpnd(opnd1);
        pushOpnd(opnd1);
        return;
    }
    Opnd* opnd2 = popOpnd();
    pushOpnd(opnd2);
    pushOpnd(opnd1);
    pushOpnd(opnd2);
    pushOpnd(opnd1);
}

void 
JavaByteCodeTranslator::dup2_x1() {
    Opnd* opnd1 = popOpnd();
    Opnd* opnd2 = popOpnd();
    if (isCategory2(opnd1)) {
        // opnd1 is a category 2 instruction
        pushOpnd(opnd1);
        pushOpnd(opnd2);
        pushOpnd(opnd1);
    } else {
        // opnd1 is a category 1 instruction
        Opnd* opnd3 = popOpnd();
        pushOpnd(opnd2);
        pushOpnd(opnd1);
        pushOpnd(opnd3);
        pushOpnd(opnd2);
        pushOpnd(opnd1);
    }
}

void 
JavaByteCodeTranslator::dup2_x2() {
    Opnd* opnd1 = popOpnd();
    Opnd* opnd2 = popOpnd();
    if (isCategory2(opnd1)) {
        // opnd1 is category 2
        if (isCategory2(opnd2)) {
            pushOpnd(opnd1);
            pushOpnd(opnd2);
            pushOpnd(opnd1);
        } else {
            // opnd2 is category 1
            Opnd* opnd3 = popOpnd();
            assert(isCategory2(opnd3) == false);
            pushOpnd(opnd1);
            pushOpnd(opnd3);
            pushOpnd(opnd2);
            pushOpnd(opnd1);
        }
    } else {
        assert(isCategory2(opnd2) == false);
        // both opnd1 & opnd2 are category 1
        Opnd* opnd3 = popOpnd();
        if (isCategory2(opnd3)) {
            pushOpnd(opnd2);
            pushOpnd(opnd1);
            pushOpnd(opnd3);
            pushOpnd(opnd2);
            pushOpnd(opnd1);
        } else {
            // opnd1, opnd2, opnd3 all are category 1
            Opnd* opnd4 = popOpnd();
            assert(isCategory2(opnd4) == false);
            pushOpnd(opnd2);
            pushOpnd(opnd1);
            pushOpnd(opnd4);
            pushOpnd(opnd3);
            pushOpnd(opnd2);
            pushOpnd(opnd1);
        }
    }
}

void 
JavaByteCodeTranslator::swap() {
    Opnd* opnd1 = popOpnd();
    Opnd* opnd2 = popOpnd();
    pushOpnd(opnd1);
    pushOpnd(opnd2);
}
//-----------------------------------------------------------------------------
// Arithmetic and logical operation byte codes
//-----------------------------------------------------------------------------
void JavaByteCodeTranslator::iadd() {genAdd(typeManager.getInt32Type());}
void JavaByteCodeTranslator::ladd() {genAdd(typeManager.getInt64Type());}
void JavaByteCodeTranslator::fadd() {genFPAdd(typeManager.getSingleType());}
void JavaByteCodeTranslator::dadd() {genFPAdd(typeManager.getDoubleType());}
void JavaByteCodeTranslator::isub() {genSub(typeManager.getInt32Type());}
void JavaByteCodeTranslator::lsub() {genSub(typeManager.getInt64Type());}
void JavaByteCodeTranslator::fsub() {genFPSub(typeManager.getSingleType());}
void JavaByteCodeTranslator::dsub() {genFPSub(typeManager.getDoubleType());}
void JavaByteCodeTranslator::imul() {genMul(typeManager.getInt32Type());}
void JavaByteCodeTranslator::lmul() {genMul(typeManager.getInt64Type());}
void JavaByteCodeTranslator::fmul() {genFPMul(typeManager.getSingleType());}
void JavaByteCodeTranslator::dmul() {genFPMul(typeManager.getDoubleType());}
void JavaByteCodeTranslator::idiv() {genDiv(typeManager.getInt32Type());}
void JavaByteCodeTranslator::ldiv() {genDiv(typeManager.getInt64Type());}
void JavaByteCodeTranslator::fdiv() {genFPDiv(typeManager.getSingleType());}
void JavaByteCodeTranslator::ddiv() {genFPDiv(typeManager.getDoubleType());}
void JavaByteCodeTranslator::irem() {genRem(typeManager.getInt32Type());}
void JavaByteCodeTranslator::lrem() {genRem(typeManager.getInt64Type());}
void JavaByteCodeTranslator::frem() {genFPRem(typeManager.getSingleType());}
void JavaByteCodeTranslator::drem() {genFPRem(typeManager.getDoubleType());}
void JavaByteCodeTranslator::ineg() {genNeg(typeManager.getInt32Type());}
void JavaByteCodeTranslator::lneg() {genNeg(typeManager.getInt64Type());}
void JavaByteCodeTranslator::fneg() {genNeg(typeManager.getSingleType());}
void JavaByteCodeTranslator::dneg() {genNeg(typeManager.getDoubleType());}
void JavaByteCodeTranslator::iand() {genAnd(typeManager.getInt32Type());}
void JavaByteCodeTranslator::land() {genAnd(typeManager.getInt64Type());}
void JavaByteCodeTranslator::ior()  {genOr(typeManager.getInt32Type());}
void JavaByteCodeTranslator::lor()  {genOr(typeManager.getInt64Type());}
void JavaByteCodeTranslator::ixor() {genXor(typeManager.getInt32Type());}
void JavaByteCodeTranslator::lxor() {genXor(typeManager.getInt64Type());}
void 
JavaByteCodeTranslator::ishl() {
    genShl(typeManager.getInt32Type(), ShiftMask_Masked);
}
void 
JavaByteCodeTranslator::lshl() {
    genShl(typeManager.getInt64Type(), ShiftMask_Masked);
}
void 
JavaByteCodeTranslator::ishr() {
    genShr(typeManager.getInt32Type(),SignedOp, ShiftMask_Masked);
}
void 
JavaByteCodeTranslator::lshr() {
    genShr(typeManager.getInt64Type(),SignedOp, ShiftMask_Masked);
}
void 
JavaByteCodeTranslator::iushr(){
    genShr(typeManager.getInt32Type(),UnsignedOp, ShiftMask_Masked);
}
void 
JavaByteCodeTranslator::lushr(){
    genShr(typeManager.getInt64Type(),UnsignedOp, ShiftMask_Masked);
}
void 
JavaByteCodeTranslator::iinc(uint16 varIndex,I_32 amount) {
    VarOpnd* varOpnd = (VarOpnd*)getVarOpndLdVar(JavaLabelPrepass::I,varIndex);
    assert(varOpnd->isVarOpnd());
    Opnd* src1 = irBuilder.genLdVar(typeManager.getInt32Type(),varOpnd);
    Opnd* src2 = irBuilder.genLdConstant((I_32)amount);
    Opnd* result = irBuilder.genAdd(typeManager.getInt32Type(),Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                    src1,src2);
    irBuilder.genStVar(varOpnd,result);
}

//-----------------------------------------------------------------------------
// conversion byte codes
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::i2l() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getInt64Type(),Type::Int64,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::i2f() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getSingleType(),Type::Single,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::i2d() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getDoubleType(),Type::Double,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::l2i() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getInt32Type(),Type::Int32,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::l2f() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getSingleType(),Type::Single,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::l2d() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getDoubleType(),Type::Double,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::f2i() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getInt32Type(),Type::Int32,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::f2l() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getInt64Type(),Type::Int64,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::f2d() {
    Opnd*    src = popOpnd();
    Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never);
    if (methodToCompile.isStrict())
        mod = mod | Modifier(Strict_Yes);
    else 
        mod = mod | Modifier(Strict_No);
    pushOpnd(irBuilder.genConv(typeManager.getDoubleType(),Type::Double,mod,src));
}

void 
JavaByteCodeTranslator::d2i() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getInt32Type(),Type::Int32,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::d2l() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getInt64Type(),Type::Int64,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::d2f() {
    Opnd*    src = popOpnd();
    Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never);
    if (methodToCompile.isStrict())
        mod  = mod | Modifier(Strict_Yes);
    else
        mod = mod | Modifier(Strict_No);
    pushOpnd(irBuilder.genConv(typeManager.getSingleType(),Type::Single,mod,src));
}

void 
JavaByteCodeTranslator::i2b() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getInt32Type(),Type::Int8,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::i2c() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getInt32Type(),Type::UInt16,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}

void 
JavaByteCodeTranslator::i2s() {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genConv(typeManager.getInt32Type(),Type::Int16,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src));
}
//-----------------------------------------------------------------------------
// comparison byte codes
//-----------------------------------------------------------------------------
//
void JavaByteCodeTranslator::lcmp()  {genThreeWayCmp(Type::Int64,Cmp_GT);}
void JavaByteCodeTranslator::fcmpl() {genThreeWayCmp(Type::Single,Cmp_GT);}
void JavaByteCodeTranslator::fcmpg() {genThreeWayCmp(Type::Single,Cmp_GT_Un);}
void JavaByteCodeTranslator::dcmpl() {genThreeWayCmp(Type::Double,Cmp_GT);}
void JavaByteCodeTranslator::dcmpg() {genThreeWayCmp(Type::Double,Cmp_GT_Un);}

//-----------------------------------------------------------------------------
// control transfer byte codes
//-----------------------------------------------------------------------------
void JavaByteCodeTranslator::ifeq(U_32 targetOffset,U_32 nextOffset) {
    genIf1(Cmp_EQ,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::ifne(U_32 targetOffset,U_32 nextOffset) {
    genIf1(Cmp_NE_Un,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::iflt(U_32 targetOffset,U_32 nextOffset) {
    genIf1Commute(Cmp_GT,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::ifge(U_32 targetOffset,U_32 nextOffset) {
    genIf1(Cmp_GTE,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::ifgt(U_32 targetOffset,U_32 nextOffset) {
    genIf1(Cmp_GT,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::ifle(U_32 targetOffset,U_32 nextOffset) {
    genIf1Commute(Cmp_GTE,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::ifnull(U_32 targetOffset,U_32 nextOffset) {
    genIfNull(Cmp_Zero,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::ifnonnull(U_32 targetOffset,U_32 nextOffset) {
    genIfNull(Cmp_NonZero,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::if_icmpeq(U_32 targetOffset,U_32 nextOffset) {
    genIf2(Cmp_EQ,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::if_icmpne(U_32 targetOffset,U_32 nextOffset) {
    genIf2(Cmp_NE_Un,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::if_icmplt(U_32 targetOffset,U_32 nextOffset) {
    genIf2Commute(Cmp_GT,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::if_icmpge(U_32 targetOffset,U_32 nextOffset) {
    genIf2(Cmp_GTE,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::if_icmpgt(U_32 targetOffset,U_32 nextOffset) {
    genIf2(Cmp_GT,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::if_icmple(U_32 targetOffset,U_32 nextOffset) {
    genIf2Commute(Cmp_GTE,targetOffset,nextOffset);
}
void JavaByteCodeTranslator::if_acmpeq(U_32 targetOffset,U_32 nextOffset) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    if (targetOffset == nextOffset)
        return;
    if (targetOffset < nextOffset) {
        irBuilder.genPseudoThrow();
    }
    lastInstructionWasABranch = true;
    checkStack();
    LabelInst *target = getLabel(labelId(targetOffset));
    irBuilder.genBranch(Type::Object,Cmp_EQ,target,src1,src2);
}

void 
JavaByteCodeTranslator::if_acmpne(U_32 targetOffset,U_32 nextOffset) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    if (targetOffset == nextOffset)
        return;
    if (targetOffset < nextOffset) {
        irBuilder.genPseudoThrow();
    }
    lastInstructionWasABranch = true;
    checkStack();
    LabelInst *target = getLabel(labelId(targetOffset));
    irBuilder.genBranch(Type::Object,Cmp_NE_Un,target,src1,src2);
}

void 
JavaByteCodeTranslator::goto_(U_32 targetOffset,U_32 nextOffset) {
    if (targetOffset == nextOffset)
        return;
    if (targetOffset < nextOffset) {
        irBuilder.genPseudoThrow();
    }
    lastInstructionWasABranch = true;
    checkStack();
    U_32 lid = labelId(targetOffset);
    LabelInst *target = getLabel(lid);
    irBuilder.genJump(target);
}
//-----------------------------------------------------------------------------
// jsr & ret byte codes for finally statements
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::jsr(U_32 targetOffset, U_32 nextOffset) {
    if (targetOffset < nextOffset) {
        irBuilder.genPseudoThrow();
    }
    lastInstructionWasABranch = true;
    checkStack();
    irBuilder.genJSR(getLabel(labelId(targetOffset)));
}

void 
JavaByteCodeTranslator::ret(uint16 varIndex, const U_8* byteCodes) {
    lastInstructionWasABranch = true;
    checkStack();
    irBuilder.genRet(getVarOpndLdVar(JavaLabelPrepass::RET,varIndex));

    retOffsets[currentOffset] = irBuilder.getLastGeneratedInst();
}
//-----------------------------------------------------------------------------
// multy-way branch byte codes (switches)
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::tableswitch(JavaSwitchTargetsIter* iter) {
    Opnd* opnd = popOpnd();
    lastInstructionWasABranch = true;
    checkStack();
    // subtract the lower bound
    Opnd* bias = irBuilder.genLdConstant((I_32)iter->getLowValue());
    Opnd* dst  = irBuilder.genSub(bias->getType(),Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),opnd,bias);
    LabelInst**    labels = new (memManager) LabelInst*[iter->getNumTargets()];
    for (U_32 i=0; iter->hasNext(); i++) {
        labels[i] = getLabel(labelId(iter->getNextTarget()));
    }
    LabelInst * defaultLabel = getLabel(labelId(iter->getDefaultTarget()));
    irBuilder.genSwitch(iter->getNumTargets(),labels,defaultLabel,dst);
}

void 
JavaByteCodeTranslator::lookupswitch(JavaLookupSwitchTargetsIter* iter) {
    Opnd* opnd = popOpnd();
    lastInstructionWasABranch = true;
    checkStack();
    // generate a sequence of branches
    while (iter->hasNext()) {
        U_32 key;
        U_32 offset = iter->getNextTarget(&key);
        // load the key
        Opnd* value = irBuilder.genLdConstant((I_32)key);
        LabelInst *target = getLabel(labelId(offset));
        irBuilder.genBranch(Type::Int32,Cmp_EQ,target,opnd,value);
        // break the basic block
        LabelInst *label = irBuilder.createLabel();
        cfgBuilder.genBlockAfterCurrent(label);
    }
    // generate a jump to the default label
    LabelInst *defaultLabel = getLabel(labelId(iter->getDefaultTarget()));
    irBuilder.genJump(defaultLabel);
}
//-----------------------------------------------------------------------------
// method return byte codes
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::ireturn(U_32 off) {
    genReturn(JavaLabelPrepass::I,off);
    LabelInst *label = irBuilder.createLabel();
    cfgBuilder.genBlockAfterCurrent(label);
}
void 
JavaByteCodeTranslator::lreturn(U_32 off) {
    genReturn(JavaLabelPrepass::L,off);
    LabelInst *label = irBuilder.createLabel();
    cfgBuilder.genBlockAfterCurrent(label);
}
void 
JavaByteCodeTranslator::freturn(U_32 off) {
    genReturn(JavaLabelPrepass::F,off);
    LabelInst *label = irBuilder.createLabel();
    cfgBuilder.genBlockAfterCurrent(label);
}
void 
JavaByteCodeTranslator::dreturn(U_32 off) {
    genReturn(JavaLabelPrepass::D,off);
    LabelInst *label = irBuilder.createLabel();
    cfgBuilder.genBlockAfterCurrent(label);
}
void 
JavaByteCodeTranslator::areturn(U_32 off) {
    genReturn(JavaLabelPrepass::A,off);
    LabelInst *label = irBuilder.createLabel();
    cfgBuilder.genBlockAfterCurrent(label);
}
void 
JavaByteCodeTranslator::return_(U_32 off) {
    genReturn(off);
    LabelInst *label = irBuilder.createLabel();
    cfgBuilder.genBlockAfterCurrent(label);
}
//-----------------------------------------------------------------------------
// LinkingException throw
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::linkingException(U_32 constPoolIndex, U_32 operation) {
    Class_Handle enclosingDrlVMClass = methodToCompile.getParentHandle();
    irBuilder.genThrowLinkingException(enclosingDrlVMClass, constPoolIndex, operation);
}
//-----------------------------------------------------------------------------
// for invoke emulation if resolution fails
//-----------------------------------------------------------------------------
void JavaByteCodeTranslator::pseudoInvoke(const char* methodSig) 
{
    U_32 numArgs = JavaLabelPrepass::getNumArgsBySignature(methodSig); 

    // pop numArgs items
    while (numArgs--) {
        popOpnd();
    }
    // recognize and push respective returnType
    Type* retType = JavaLabelPrepass::getRetTypeBySignature(compilationInterface, methodToCompile.getParentHandle(), methodSig);
    assert(retType);

    // push NULL as a returned object
    if (retType->tag != Type::Void) {
        pushOpnd(irBuilder.genLdNull());
    }
}

//-----------------------------------------------------------------------------
// method invocation byte codes
//-----------------------------------------------------------------------------
Opnd** 
JavaByteCodeTranslator::popArgs(U_32 numArgs) {
    // pop source operands
    Opnd** srcOpnds = new (memManager) Opnd*[numArgs];
    for (int i=numArgs-1; i>=0; i--) 
        srcOpnds[i] = popOpnd();
    return srcOpnds;
}

//Helper class used to initialize unresolved method ptrs
class JavaMethodSignature : public MethodSignature {
public:
    JavaMethodSignature(MemoryManager& mm, CompilationInterface ci, bool instanceMethod, Class_Handle cl, const char* sigStr){
        signatureStr = sigStr;
        nParams = JavaLabelPrepass::getNumArgsBySignature(sigStr) + (instanceMethod ? 1 : 0);
        const char* sigSuffix = sigStr;
        if (nParams > 0) {
            paramTypes = new (mm) Type*[nParams];
            U_32 i = 0;
            if (instanceMethod) {
                paramTypes[0] = ci.getTypeManager().getUnresolvedObjectType();
                i++;
            }
            sigSuffix++;
            for (; i < nParams; i++) {
                U_32 len = 0;
                Type* type = JavaLabelPrepass::getTypeByDescriptorString(ci, cl, sigSuffix, len);
                assert(type!=NULL);
                paramTypes[i] = type;
                sigSuffix+=len;
            }
        } else {
            paramTypes = NULL;
        }
        retType = JavaLabelPrepass::getRetTypeBySignature(ci, cl, sigSuffix);
    }
    
    virtual ~JavaMethodSignature(){};
    virtual U_32 getNumParams() const { return nParams;}
    virtual Type** getParamTypes() const { return paramTypes;}
    virtual Type* getRetType() const {return retType;}
    virtual const char* getSignatureString() const {return signatureStr;}
private:
    U_32 nParams;
    Type** paramTypes;
    Type* retType;
    const char* signatureStr;

};

void JavaByteCodeTranslator::genCallWithResolve(JavaByteCodes bc, unsigned cpIndex) {
    assert(bc == OPCODE_INVOKESPECIAL || bc == OPCODE_INVOKESTATIC || bc == OPCODE_INVOKEVIRTUAL || bc == OPCODE_INVOKEINTERFACE);
    bool isStatic = bc == OPCODE_INVOKESTATIC;
    

    ObjectType* enclosingClass = methodToCompile.getParentType()->asObjectType();
    assert(enclosingClass!=NULL);
    const char* methodSig = methodSignatureString(cpIndex);
    assert(methodSig);
    JavaMethodSignature* sig = new (memManager) JavaMethodSignature(irBuilder.getIRManager()->getMemoryManager(), 
        compilationInterface, !isStatic, (Class_Handle)enclosingClass->getVMTypeHandle(), methodSig);
    U_32 numArgs = sig->getNumParams();
    assert(numArgs > 0 || isStatic);

    Opnd** args = popArgs(numArgs);
    Type* returnType = sig->getRetType();
    

    if (bc != OPCODE_INVOKEINTERFACE) {
        const char* kname = CompilationInterface::getMethodClassName(methodToCompile.getParentHandle(), cpIndex);
        const char* mname = CompilationInterface::getMethodName(methodToCompile.getParentHandle(), cpIndex);
        if (VMMagicUtils::isVMMagicClass(kname)) {
            assert(bc == OPCODE_INVOKESTATIC || bc == OPCODE_INVOKEVIRTUAL);
            UNUSED bool res = genVMMagic(mname, numArgs, args, returnType);    
            assert(res);
            return;
        } else if (isVMHelperClass(kname)) {
            assert(bc == OPCODE_INVOKESTATIC);
            bool res = genVMHelper(mname, numArgs, args, returnType);
            if (res) { //method is not a registered vmhelper name
                return;
            }
        }
    }

    Opnd* tauNullCheckedFirstArg = bc == OPCODE_INVOKESTATIC ? irBuilder.genTauSafe() :irBuilder.genTauCheckNull(args[0]);
    Opnd* tauTypesChecked = NULL;// let IRBuilder handle types

    Opnd* dst = irBuilder.genIndirectCallWithResolve(returnType, tauNullCheckedFirstArg, tauTypesChecked, 
                                numArgs, args, enclosingClass, bc, cpIndex, sig);
    if (returnType->tag != Type::Void) {
        pushOpnd(dst);
    }
}

void 
JavaByteCodeTranslator::invokevirtual(U_32 constPoolIndex) {
    MethodDesc* methodDesc = compilationInterface.getVirtualMethod(methodToCompile.getParentHandle(), constPoolIndex);
    if (!methodDesc) {
        if (!typeManager.isLazyResolutionMode()) {
            linkingException(constPoolIndex, OPCODE_INVOKEVIRTUAL);
        }
        genCallWithResolve(OPCODE_INVOKEVIRTUAL, constPoolIndex);
        return;
    }
    jitrino_assert(methodDesc);
    U_32 numArgs = methodDesc->getNumParams();
    Opnd** srcOpnds = popArgs(numArgs);
    Type* returnType = methodDesc->getReturnType();

    const char* className = methodDesc->getParentType()->getName();
    if (VMMagicUtils::isVMMagicClass(className)) {
        UNUSED bool res = genVMMagic(methodDesc->getName(), numArgs, srcOpnds, returnType);
        assert(res);
        return;
    }

    // callvirt can throw a null pointer exception
    Opnd *tauNullChecked = irBuilder.genTauCheckNull(srcOpnds[0]);
    Opnd* thisOpnd = srcOpnds[0];
    if (methodDesc->getParentType() != thisOpnd->getType()) {
        if(Log::isEnabled()) {
            Log::out()<<"CHECKVIRTUAL "; thisOpnd->printWithType(Log::out()); Log::out() << " : ";
            methodDesc->getParentType()->print(Log::out());
            Log::out() <<"."<<methodDesc->getName()<<" "<< (int)methodDesc->getByteCodeSize()<< ::std::endl;
        }

        Type* type = thisOpnd->getType();
        if (!type->isNullObject() && !type->isUnresolvedType() && !type->isInterface()) {
            // needs to refine the method descriptor before doing any optimization
            MethodDesc *overriding = compilationInterface.getOverridingMethod(
                                     (NamedType*)type,methodDesc);
            if (overriding && overriding != methodDesc) {
                methodDesc = overriding;
            }
        }
    }

    if (returnType==NULL) {
        // This means that it was not resolved successfully but it can be resolved later
        // inside the callee (with some "magic" custom class loader for example)
        // Or respective exception will be thrown there (in the callee) at the attempt to create (new)
        // an object of unresolved type
        const char* methodSig_string = methodSignatureString(constPoolIndex);
        returnType = JavaLabelPrepass::getRetTypeBySignature(compilationInterface, methodToCompile.getParentHandle(), methodSig_string);
    }

    Opnd* dst = irBuilder.genTauVirtualCall(methodDesc,returnType,
                                            tauNullChecked, 
                                            0, // let IRBuilder handle types
                                            numArgs,
                                            srcOpnds);
    // push the return type
    if (returnType->tag != Type::Void)
        pushOpnd(dst);
}

void 
JavaByteCodeTranslator::invokespecial(U_32 constPoolIndex) {
    MethodDesc* methodDesc = compilationInterface.getSpecialMethod(methodToCompile.getParentHandle(), constPoolIndex);
    if (!methodDesc) {
        if (!typeManager.isLazyResolutionMode()) {
            linkingException(constPoolIndex, OPCODE_INVOKESPECIAL);
        }
        genCallWithResolve(OPCODE_INVOKESPECIAL, constPoolIndex);
        return;
    }
    jitrino_assert(methodDesc);
    U_32 numArgs = methodDesc->getNumParams();
    Opnd** srcOpnds = popArgs(numArgs);
    Type* returnType = methodDesc->getReturnType();
    // invokespecial can throw a null pointer exception
    Opnd *tauNullChecked = irBuilder.genTauCheckNull(srcOpnds[0]);
    Opnd* dst;
    
    if (returnType == NULL) {
        // This means that it was not resolved successfully but it can be resolved later
        // inside the callee (with some "magic" custom class loader for example)
        // Or respective exception will be thrown there (in the callee) at the attempt to create (new)
        // an object of unresolved type
        returnType = typeManager.getNullObjectType();
    }
    dst = irBuilder.genDirectCall(methodDesc,
        returnType,
        tauNullChecked, 
        0, // let IRBuilder check types
        numArgs,
        srcOpnds);

    // push the return type
    if (returnType->tag != Type::Void) {
        pushOpnd(dst);
    }

}

void 
JavaByteCodeTranslator::invokestatic(U_32 constPoolIndex) {
    MethodDesc* methodDesc = compilationInterface.getStaticMethod(methodToCompile.getParentHandle(), constPoolIndex);
    if (!methodDesc) {
        if (!typeManager.isLazyResolutionMode()) {
            linkingException(constPoolIndex, OPCODE_INVOKESTATIC);
        }
        genCallWithResolve(OPCODE_INVOKESTATIC, constPoolIndex);
        return;
    }

    jitrino_assert(methodDesc);
    U_32 numArgs = methodDesc->getNumParams();
    Opnd** srcOpnds = popArgs(numArgs);
    Type *returnType = methodDesc->getReturnType();
    if (returnType == NULL) {
        // This means that it was not resolved successfully but it can be resolved later
        // inside the callee (with some "magic" custom class loader for example)
        // Or respective exception will be thrown there (in the callee) at the attempt to create (new)
        // an object of unresolved type
        returnType = typeManager.getNullObjectType();
    }
    //
    //  Try some optimizations for Min, Max, Abs...
    //
    if (translationFlags.genMinMaxAbs == true &&
        genMinMax(methodDesc,numArgs,srcOpnds,returnType)) {
        return;
    } else
        genInvokeStatic(methodDesc,numArgs,srcOpnds,returnType);
}

void 
JavaByteCodeTranslator::invokeinterface(U_32 constPoolIndex,U_32 count) {
    MethodDesc* methodDesc = compilationInterface.getInterfaceMethod(methodToCompile.getParentHandle(), constPoolIndex);
    if (!methodDesc) {
        if (!typeManager.isLazyResolutionMode()) {
            linkingException(constPoolIndex, OPCODE_INVOKEINTERFACE);
        }
        genCallWithResolve(OPCODE_INVOKEINTERFACE, constPoolIndex);
        return;
    }
    jitrino_assert(methodDesc);
    U_32 numArgs = methodDesc->getNumParams();
    Opnd** srcOpnds = popArgs(numArgs);
    Type* returnType = methodDesc->getReturnType();
    // callintf can throw a null pointer exception
    Opnd *tauNullChecked = irBuilder.genTauCheckNull(srcOpnds[0]);
    Opnd* thisOpnd = srcOpnds[0];
    Opnd* dst;
    if (methodDesc->getParentType() != thisOpnd->getType()) {
        Type * type = thisOpnd->getType();
        if (!type->isNullObject() && !type->isUnresolvedObject() && !type->isInterface()) {
            // need to refine the method descriptor before doing any optimization
            MethodDesc *overriding = compilationInterface.getOverridingMethod(
                                  (NamedType*)type,methodDesc);
            if (overriding && overriding != methodDesc && !overriding->getParentType()->isInterface()) {
                methodDesc = overriding;
            }
        }
    }

    if (returnType == NULL) {
        // This means that it was not resolved successfully but it can be resolved later
        // inside the callee (with some "magic" custom class loader for example)
        // Or respective exception will be thrown there (in the callee) at the attempt to create (new)
        // an object of unresolved type
        const char* methodSig_string = methodSignatureString(constPoolIndex);
        returnType = JavaLabelPrepass::getRetTypeBySignature(compilationInterface, methodToCompile.getParentHandle(), methodSig_string);
    }
    dst = irBuilder.genTauVirtualCall(methodDesc,
        returnType,
        tauNullChecked, 
        0, // let IRBuilder handle types
        numArgs,
        srcOpnds);
    // push the return type
    if (returnType->tag != Type::Void)
        pushOpnd(dst);
}
//-----------------------------------------------------------------------------
// object allocation byte codes
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::new_(U_32 constPoolIndex) {
    NamedType* type = compilationInterface.getNamedType(methodToCompile.getParentHandle(), constPoolIndex, ResolveNewCheck_DoCheck);
    jitrino_assert(type);
    if (type->isUnresolvedObject()) {
        if (!typeManager.isLazyResolutionMode()) {
            linkingException(constPoolIndex, OPCODE_NEW);
        }
        pushOpnd(irBuilder.genNewObjWithResolve(methodToCompile.getParentType()->asObjectType(), constPoolIndex));
    } else {
#ifdef _DEBUG
        const char* typeName = type->getName();
        assert(!VMMagicUtils::isVMMagicClass(typeName));
#endif
        pushOpnd(irBuilder.genNewObj(type));
    }
}
void 
JavaByteCodeTranslator::newarray(U_8 atype) {
    NamedType* type = NULL;
    switch (atype) {
    case 4:    // boolean
        type = typeManager.getBooleanType(); break;
    case 5: // char
        type = typeManager.getCharType(); break;
    case 6: // float
        type = typeManager.getSingleType(); break;
    case 7: // double
        type = typeManager.getDoubleType(); break;
    case 8: // byte
        type = typeManager.getInt8Type(); break;
    case 9: // short
        type = typeManager.getInt16Type(); break;
    case 10: // int
        type = typeManager.getInt32Type(); break;
    case 11: // long
        type = typeManager.getInt64Type(); break;
    default: jitrino_assert(0);
    }
    Opnd* arrayOpnd = irBuilder.genNewArray(type,popOpnd());
    pushOpnd(arrayOpnd);
    if (translationFlags.optArrayInit) {
        const U_8* byteCodes = parser.getByteCodes();
        const U_32 byteCodeLength = parser.getByteCodeLength();
        U_32 offset = currentOffset + 2/*newarray length*/;
        U_32 length = checkForArrayInitializer(arrayOpnd, byteCodes, offset, byteCodeLength);
        currentOffset += length;
    }
}

void 
JavaByteCodeTranslator::anewarray(U_32 constPoolIndex) {
    NamedType* type = compilationInterface.getNamedType(methodToCompile.getParentHandle(), constPoolIndex);
    Opnd* sizeOpnd = popOpnd();
    if (type->isUnresolvedType()) {
        if (!typeManager.isLazyResolutionMode()) {
            linkingException(constPoolIndex, OPCODE_ANEWARRAY);
        }
        //res type can be an array of multi array with uninitialized dimensions.
        pushOpnd(irBuilder.genNewArrayWithResolve(type, sizeOpnd, methodToCompile.getParentType()->asObjectType(), constPoolIndex));
    } else {
        pushOpnd(irBuilder.genNewArray(type,sizeOpnd));
    }
}

void 
JavaByteCodeTranslator::multianewarray(U_32 constPoolIndex,U_8 dimensions) {
    NamedType* arraytype = compilationInterface.getNamedType(methodToCompile.getParentHandle(), constPoolIndex);
    assert(arraytype->isArray());
    jitrino_assert(dimensions > 0);
    Opnd** countOpnds = new (memManager) Opnd*[dimensions];
    // pop the sizes
    for (int i = dimensions - 1; i >= 0; i--) {
        countOpnds[i] = popOpnd();
    }
    if (arraytype->isUnresolvedType()) {
        if (!typeManager.isLazyResolutionMode()) {
            linkingException(constPoolIndex, OPCODE_MULTIANEWARRAY);
        }
        pushOpnd(irBuilder.genMultianewarrayWithResolve(
            arraytype, methodToCompile.getParentType()->asObjectType(),constPoolIndex, dimensions,countOpnds
            ));
    } else {
        pushOpnd(irBuilder.genMultianewarray(arraytype,dimensions,countOpnds));
    }
}

void 
JavaByteCodeTranslator::arraylength() {
    Type::Tag arrayLenType = Type::Int32;
    pushOpnd(irBuilder.genArrayLen(typeManager.getInt32Type(),arrayLenType,popOpnd()));
}

void 
JavaByteCodeTranslator::athrow() {
    lastInstructionWasABranch = true;
    irBuilder.genThrow(Throw_NoModifier, popOpnd());
    LabelInst *label = irBuilder.createLabel();
    cfgBuilder.genBlockAfterCurrent(label);
}

//-----------------------------------------------------------------------------
// type checking byte codes
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::checkcast(U_32 constPoolIndex) {
    NamedType *type = compilationInterface.getNamedType(methodToCompile.getParentHandle(), constPoolIndex);
    Opnd* objOpnd = popOpnd();
    if (type->isUnresolvedType()) {
        if (!typeManager.isLazyResolutionMode()) {
            linkingException(constPoolIndex, OPCODE_CHECKCAST);
        }
        pushOpnd(irBuilder.genCastWithResolve(objOpnd, type, methodToCompile.getParentType()->asObjectType(), constPoolIndex));
    } else {
        pushOpnd(irBuilder.genCast(objOpnd, type));
    }
}

int  
JavaByteCodeTranslator::instanceof(const U_8* bcp, U_32 constPoolIndex, U_32 off)   {
    NamedType *type = compilationInterface.getNamedType(methodToCompile.getParentHandle(), constPoolIndex);
    Opnd* src = popOpnd();
    Type* srcType = src->getType();
    Opnd* res = NULL;

    if (type->isUnresolvedType()) {
        if (!typeManager.isLazyResolutionMode()) {
            linkingException(constPoolIndex, OPCODE_INSTANCEOF);
        }
        res = irBuilder.genInstanceOfWithResolve(src, methodToCompile.getParentType()->asObjectType(), constPoolIndex);
    } else if( !srcType->isUnresolvedType() 
        && !srcType->isInterface() 
        && !Simplifier::isExactType(src) 
        && ((ObjectType*)type)->isFinalClass() )
    {
        // if target type is final just compare VTables
        // This can not be done by Simplifier as it can not generate branches
        // (srcType->isExactType() case will be simplified by Simplifier)

        Type* intPtrType = typeManager.getIntPtrType();

        LabelInst* ObjIsNullLabel = irBuilder.createLabel();
        LabelInst* Exit = irBuilder.createLabel();
        VarOpnd* resVar = irBuilder.genVarDef(intPtrType, false);

        newFallthroughBlock();

        Opnd * nullObj = irBuilder.genLdNull();
        irBuilder.genBranch(Type::IntPtr, Cmp_EQ, ObjIsNullLabel, nullObj, src);        

        // src is not null here
        newFallthroughBlock();
        Opnd* srcIsSafe = irBuilder.genTauSafe();
        Opnd* dynamicVTable = irBuilder.genTauLdVTable(src, srcIsSafe, srcType);
        Opnd* staticVTable = irBuilder.genGetVTable((ObjectType*) type);
        irBuilder.genStVar(resVar, irBuilder.genCmp(intPtrType,Type::IntPtr,Cmp_EQ,staticVTable,dynamicVTable));
        irBuilder.genJump(Exit);

        // src is null, instanceOf returns 0
        irBuilder.genLabel(ObjIsNullLabel);
        cfgBuilder.genBlockAfterCurrent(ObjIsNullLabel);
        Opnd * zero = irBuilder.genLdConstant((I_32)0);
        irBuilder.genStVar(resVar, zero);
        irBuilder.genJump(Exit);

        irBuilder.genLabel(Exit);
        cfgBuilder.genBlockAfterCurrent(Exit);
        res = irBuilder.genLdVar(intPtrType,resVar);
    } else {
        res = irBuilder.genInstanceOf(src,type);
    }

    assert(res);
    pushOpnd(res);
    return 3;
}

//
// synchronization
//
void 
JavaByteCodeTranslator::monitorenter() {
   if (translationFlags.ignoreSync) 
        popOpnd();
   else if (translationFlags.syncAsEnterFence)
        irBuilder.genMonitorEnterFence(popOpnd());
   else
        irBuilder.genMonitorEnter(popOpnd());
}

void 
JavaByteCodeTranslator::monitorexit() {
    if (translationFlags.ignoreSync || translationFlags.syncAsEnterFence)
        popOpnd();
    else
        irBuilder.genMonitorExit(popOpnd());
}

//-----------------------------------------------------------------------------
// variable access helpers
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::genLdVar(U_32 varIndex,JavaLabelPrepass::JavaVarType javaType) {
    Opnd *var = getVarOpndLdVar(javaType,varIndex);
    if (VMMagicUtils::isVMMagicClass(var->getType()->getName())) {
        var->setType(convertVMMagicType2HIR(typeManager, var->getType()));
    }
    Opnd *opnd;
    if (var->isVarOpnd()) {
        opnd = irBuilder.genLdVar(var->getType(),(VarOpnd*)var);
    } else {
        opnd = var;
    }
    pushOpnd(opnd);
}

void 
JavaByteCodeTranslator::genTypeStVar(uint16 varIndex) {
    Opnd *src = popOpnd();
    JavaLabelPrepass::JavaVarType javaType;
    if (src->getType() == typeManager.getIntPtrType())
        javaType = JavaLabelPrepass::RET;
    else
        javaType = JavaLabelPrepass::A;
    VarOpnd *var = getVarOpndStVar(javaType,varIndex,src);
    if (var != NULL) {
        irBuilder.genStVar(var,src);
    }
}

void 
JavaByteCodeTranslator::genStVar(U_32 varIndex,JavaLabelPrepass::JavaVarType javaType) {
    Opnd *src = popOpnd();
    VarOpnd *var = getVarOpndStVar(javaType,varIndex,src);
    if (var != NULL)
        irBuilder.genStVar(var,src);
}

//-----------------------------------------------------------------------------
// method return helpers
//-----------------------------------------------------------------------------
bool 
JavaByteCodeTranslator::needsReturnLabel(U_32 off) {
    if (!moreThanOneReturn && methodToCompile.getByteCodeSize()-1 != off) {
        if (!jumpToTheEnd) {
           // allocate one more label
           labels[numLabels++] = (LabelInst*)irBuilder.getInstFactory()->makeLabel();
        }
        jumpToTheEnd      = true;
        moreThanOneReturn = true;
    }
    return moreThanOneReturn;
}

// for non-void returns
void 
JavaByteCodeTranslator::genReturn(JavaLabelPrepass::JavaVarType javaType, U_32 off) {
    Opnd *ret = popOpndStVar();
    if (methodToCompile.isSynchronized()) {
        // Create a new block to break exception region.  The monexit exception should
        // go to unwind.
        cfgBuilder.genBlock(irBuilder.createLabel());
        if (methodToCompile.isStatic()) {
            //irBuilder.genTypeMonitorExit(methodToCompile.getParentType());
            Opnd* classObjectOpnd = irBuilder.genGetClassObj((ObjectType*) methodToCompile.getParentType());
            pushOpnd(classObjectOpnd);
            genMethodMonitorExit();
        } else {
            genLdVar(0,JavaLabelPrepass::A);
            genMethodMonitorExit();
        }
    }
    irBuilder.genReturn(ret,javaTypeMap[javaType]);
    opndStack.makeEmpty();
}

// for void returns
void
JavaByteCodeTranslator::genReturn(U_32 off) {
    if (methodToCompile.isSynchronized()) {
        // Create a new block to break exception region. The monexit exception should
        // go to unwind.
        cfgBuilder.genBlock(irBuilder.createLabel());
        if (methodToCompile.isStatic()) {
           // irBuilder.genTypeMonitorExit(methodToCompile.getParentType());
            Opnd* classObjectOpnd = irBuilder.genGetClassObj((ObjectType*) methodToCompile.getParentType());
            pushOpnd(classObjectOpnd);
            genMethodMonitorExit();
        } else {
            genLdVar(0,JavaLabelPrepass::A);
            genMethodMonitorExit();
        }
    }
    irBuilder.genReturn();
    // some manually written test case can leave non empty opnd stack after return
    opndStack.makeEmpty();
}

//-----------------------------------------------------------------------------
// arithmetic & logical helpers
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::genAdd(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    pushOpnd(irBuilder.genAdd(dstType,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src1,src2));
}

void 
JavaByteCodeTranslator::genSub(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    pushOpnd(irBuilder.genSub(dstType,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src1,src2));
}

void 
JavaByteCodeTranslator::genMul(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    pushOpnd(irBuilder.genMul(dstType,Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),src1,src2));
}

void 
JavaByteCodeTranslator::genDiv(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    pushOpnd(irBuilder.genDiv(dstType,Modifier(SignedOp)|Modifier(Strict_No),src1,src2));
}

void 
JavaByteCodeTranslator::genRem(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    pushOpnd(irBuilder.genRem(dstType,Modifier(SignedOp)|Modifier(Strict_No),src1,src2));
}

void 
JavaByteCodeTranslator::genNeg(Type* dstType) {
    Opnd*    src = popOpnd();
    pushOpnd(irBuilder.genNeg(dstType,src));
}

void 
JavaByteCodeTranslator::genFPAdd(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never);
    if (methodToCompile.isStrict())
        mod = mod | Modifier(Strict_Yes);
    else
        mod = mod | Modifier(Strict_No);
    pushOpnd(irBuilder.genAdd(dstType,mod,src1,src2));
}

void 
JavaByteCodeTranslator::genFPSub(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never);
    if (methodToCompile.isStrict())
        mod = mod | Modifier(Strict_Yes);
    else
        mod = mod | Modifier(Strict_No);
    pushOpnd(irBuilder.genSub(dstType,mod,src1,src2));
}

void 
JavaByteCodeTranslator::genFPMul(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never);
    if (methodToCompile.isStrict())
        mod  = mod | Modifier(Strict_Yes);
    else
        mod  = mod | Modifier(Strict_No);
    pushOpnd(irBuilder.genMul(dstType,mod,src1,src2));
}

void 
JavaByteCodeTranslator::genFPDiv(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    Modifier mod = SignedOp;
    if (methodToCompile.isStrict())
        mod = mod | Modifier(Strict_Yes);
    else
        mod = mod | Modifier(Strict_No);
    pushOpnd(irBuilder.genDiv(dstType,mod,src1,src2));
}

void 
JavaByteCodeTranslator::genFPRem(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    Modifier mod = SignedOp;
    if (methodToCompile.isStrict())
        mod = mod | Modifier(Strict_Yes);
    else
        mod = mod | Modifier(Strict_No);
    pushOpnd(irBuilder.genRem(dstType,mod,src1,src2));
}

void 
JavaByteCodeTranslator::genAnd(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    pushOpnd(irBuilder.genAnd(dstType,src1,src2));
}

void 
JavaByteCodeTranslator::genOr(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    pushOpnd(irBuilder.genOr(dstType,src1,src2));
}

void 
JavaByteCodeTranslator::genXor(Type* dstType) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    pushOpnd(irBuilder.genXor(dstType,src1,src2));
}

void 
JavaByteCodeTranslator::genShl(Type* type, ShiftMaskModifier mod) {
    Opnd*    shiftAmount = popOpnd();
    Opnd*    value = popOpnd(); 
    pushOpnd(irBuilder.genShl(type,mod,value,shiftAmount));
}

void 
JavaByteCodeTranslator::genShr(Type* type, SignedModifier mod1,
                               ShiftMaskModifier mod2) {
    Opnd*    shiftAmount = popOpnd();
    Opnd*    value = popOpnd(); 
    pushOpnd(irBuilder.genShr(type,Modifier(mod1)|Modifier(mod2), value, shiftAmount));
}

//-----------------------------------------------------------------------------
// array access helpers
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::genArrayLoad(Type* type) {
    Opnd* index = popOpnd();
    Opnd* base = popOpnd();
    pushOpnd(irBuilder.genLdElem(type,base,index));
}

void 
JavaByteCodeTranslator::genTypeArrayLoad() {
    Opnd* index = popOpnd();
    Opnd* base = popOpnd();
    Type *type = base->getType();
    if (!type->isArrayType()) {
        if (type->isNullObject()) {
            irBuilder.genThrowSystemException(CompilationInterface::Exception_NullPointer);
            pushOpnd(irBuilder.genLdNull());
            return;
        }
        if (Log::isEnabled()) {
            Log::out() << "Array type is ";
            type->print(Log::out()); Log::out() << ::std::endl;
            stateInfo->stack[5].type->print(Log::out()); Log::out() << ::std::endl;
            Log::out() << "CONFLICT IN ARRAY ACCESS\n";
        }
        type = typeManager.getSystemObjectType();
    } else
        type = ((ArrayType*)type)->getElementType();
    pushOpnd(irBuilder.genLdElem(type,base,index));
}

void 
JavaByteCodeTranslator::genArrayStore(Type* type) {
    Opnd*    value = popOpnd();
    Opnd*    index = popOpnd();
    Opnd*    base = popOpnd();
    irBuilder.genStElem(type,base,index,value);
}

void 
JavaByteCodeTranslator::genTypeArrayStore() {
    Opnd*    value = popOpnd();
    Opnd*    index = popOpnd();
    Opnd*    base = popOpnd();
    Type *type = base->getType();
    if (!type->isArrayType()) {
        if (type->isNullObject()) {
            irBuilder.genThrowSystemException(CompilationInterface::Exception_NullPointer);
            return;
        }
        type = typeManager.getSystemObjectType();
        Log::out() << "CONFLICT IN ARRAY ACCESS\n";
    } else
        type = ((ArrayType*)type)->getElementType();
    irBuilder.genStElem(type,base,index,value);
}

//-----------------------------------------------------------------------------
// control transfer helpers
//-----------------------------------------------------------------------------
void 
JavaByteCodeTranslator::genIf1(ComparisonModifier mod,
                               I_32 targetOffset,
                               I_32 nextOffset) {
    Opnd*    src1 = popOpnd();
    if (targetOffset == nextOffset)
        return;
    if (targetOffset < nextOffset) {
        irBuilder.genPseudoThrow();
    }
    lastInstructionWasABranch = true;
    checkStack();
    LabelInst *target = getLabel(labelId(targetOffset));
    Opnd*    src2 = irBuilder.genLdConstant((I_32)0);
    irBuilder.genBranch(Type::Int32,mod,target,src1,src2);
}

void 
JavaByteCodeTranslator::genIf1Commute(ComparisonModifier mod,
                                      I_32 targetOffset,
                                      I_32 nextOffset) {
    Opnd*    src1 = popOpnd();
    if (targetOffset == nextOffset)
        return;
    if (targetOffset < nextOffset) {
        irBuilder.genPseudoThrow();
    }
    lastInstructionWasABranch = true;
    checkStack();
    LabelInst *target = getLabel(labelId(targetOffset));
    Opnd*    src2 = irBuilder.genLdConstant((I_32)0);
    irBuilder.genBranch(Type::Int32,mod,target,src2,src1);
}

void 
JavaByteCodeTranslator::genIf2(ComparisonModifier mod,
                               I_32 targetOffset,
                               I_32 nextOffset) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    if (targetOffset == nextOffset)
        return;
    if (targetOffset < nextOffset) {
        irBuilder.genPseudoThrow();
    }
    lastInstructionWasABranch = true;
    checkStack();
    LabelInst *target = getLabel(labelId(targetOffset));
    irBuilder.genBranch(Type::Int32,mod,target,src1,src2);
}

void 
JavaByteCodeTranslator::genIf2Commute(ComparisonModifier mod,
                                      I_32 targetOffset,
                                      I_32 nextOffset) {
    Opnd*    src2 = popOpnd();
    Opnd*    src1 = popOpnd();
    if (targetOffset == nextOffset)
        return;
    if (targetOffset < nextOffset) {
        irBuilder.genPseudoThrow();
    }
    lastInstructionWasABranch = true;
    checkStack();
    LabelInst *target = getLabel(labelId(targetOffset));
    irBuilder.genBranch(Type::Int32,mod,target,src2,src1);
}

void 
JavaByteCodeTranslator::genIfNull(ComparisonModifier mod,
                                  I_32 targetOffset,
                                  I_32 nextOffset) {
    Opnd*    src1 = popOpnd();
    if (targetOffset == nextOffset)
        return;
    if (targetOffset < nextOffset) {
        irBuilder.genPseudoThrow();
    }
    lastInstructionWasABranch = true;
    checkStack();
    LabelInst *target = getLabel(labelId(targetOffset));

    if (src1->getType() == typeManager.getNullObjectType()) {
        if (mod == Cmp_Zero)
            irBuilder.genJump(target);
        return;
    }
    irBuilder.genBranch(Type::SystemObject,mod,target,src1);
}

void 
JavaByteCodeTranslator::genThreeWayCmp(Type::Tag cmpType,
                                       ComparisonModifier src1ToSrc2) {
    Opnd* src2 = popOpnd();
    Opnd* src1 = popOpnd();
    Type* dstType = typeManager.getInt32Type();
    pushOpnd(irBuilder.genCmp3(dstType,cmpType,src1ToSrc2,src1,src2));
}

//-----------------------------------------------------------------------------
// method calls helpers
//-----------------------------------------------------------------------------

void 
JavaByteCodeTranslator::genInvokeStatic(MethodDesc * methodDesc,
                                        U_32       numArgs,
                                        Opnd **      srcOpnds,
                                        Type *       returnType) {
    Opnd *dst;

    const char* kname = methodDesc->getParentType()->getName(); 
    const char* mname = methodDesc->getName(); 
    if (VMMagicUtils::isVMMagicClass(kname)) {
        UNUSED bool res = genVMMagic(mname, numArgs, srcOpnds, returnType);    
        assert(res);
        return;
    } else if (isVMHelperClass(kname) && !methodDesc->isNative()) {
        bool res = genVMHelper(mname, numArgs, srcOpnds, returnType);
        if (res) {
            return;
        }
    }
    Opnd *tauNullChecked = irBuilder.genTauSafe(); // always safe, is a static method call
    Type* resType = returnType;
    if (VMMagicUtils::isVMMagicClass(resType->getName())) {
        resType = convertVMMagicType2HIR(typeManager, resType);
    }
    dst = irBuilder.genDirectCall(methodDesc, 
                        resType,
                        tauNullChecked,
                        0, // let IRBuilder check types
                        numArgs,
                        srcOpnds);
    if (returnType->tag != Type::Void)
        pushOpnd(dst);
}

void
JavaByteCodeTranslator::newFallthroughBlock() {
    LabelInst * labelInst = irBuilder.createLabel();
    irBuilder.genFallThroughLabel(labelInst);
    cfgBuilder.genBlockAfterCurrent(labelInst);
}

bool
JavaByteCodeTranslator::genMinMax(MethodDesc * methodDesc, 
                                  U_32       numArgs,
                                  Opnd **      srcOpnds,
                                  Type *       returnType) {

    const char *className = methodDesc->getParentType()->getName();
    if (strcmp(className, "java/lang/Math") == 0) {
        const char *methodName = methodDesc->getName();
        //
        //  Check for certain math methods and inline them
        // 
        if (strcmp(methodName, "min") == 0) {
            assert(numArgs == 2);
            Opnd *src0 = srcOpnds[0];
            Opnd *src1 = srcOpnds[1];
            Type *type = src0->getType();
            assert(type == src1->getType());

            IRManager& irm = *irBuilder.getIRManager();
            MethodDesc& md = irm.getMethodDesc();
            if (Log::isEnabled()) {
                Log::out() << "Saw call to java/lang/Math::min from "
                           << md.getParentType()->getName()
                           << "::"
                           << md.getName()
                           << ::std::endl;
            }
            Opnd *res = irBuilder.genMin(type, src0, src1);
            if (res) {
                pushOpnd(res);
                return true;
            }
        } else if (strcmp(methodName, "max") == 0) {
            assert(numArgs == 2);
            Opnd *src0 = srcOpnds[0];
            Opnd *src1 = srcOpnds[1];
            Type *type = src0->getType();
            assert(type == src1->getType());
            
            IRManager& irm = *irBuilder.getIRManager();
            MethodDesc& md = irm.getMethodDesc();
            if (Log::isEnabled()) {
                Log::out() << "Saw call to java/lang/Math::max from "
                           << md.getParentType()->getName()
                           << "::"
                           << md.getName()
                           << ::std::endl;
            }
            
            Opnd *res = irBuilder.genMax(type, src0, src1);
            if (res) {
                pushOpnd(res);
                return true;
            }
            
        } else if (strcmp(methodName, "abs") == 0) {
            assert(numArgs == 1);
            Opnd *src0 = srcOpnds[0];
            Type *type = src0->getType();
            
            IRManager& irm = *irBuilder.getIRManager();
            MethodDesc& md = irm.getMethodDesc();
            if (Log::isEnabled()) {
                Log::out() << "Saw call to java/lang/Math::abs from "
                           << md.getParentType()->getName()
                           << "::"
                           << md.getName()
                           << ::std::endl;
            }
            
            Opnd *res = irBuilder.genAbs(type, src0);
            if (res) {
                pushOpnd(res);
                return true;
            }
        } else {
            return false;
        }
    }
    return false;
}

//------------------------------------------------
//  synchronization helpers
//------------------------------------------------

void
JavaByteCodeTranslator::genMethodMonitorEnter() {
    if (translationFlags.ignoreSync) {
        popOpnd();
        return;
    }
    if (translationFlags.syncAsEnterFence) {
        irBuilder.genMonitorEnterFence(popOpnd());
    } 
    else if (! translationFlags.onlyBalancedSync) {
        irBuilder.genMonitorEnter(popOpnd());
    }
    else {
        assert(lockAddr == NULL && oldLockValue == NULL);
        Opnd * obj = popOpnd();
        Type * lockType = typeManager.getUInt16Type();
        Type * lockAddrType = typeManager.getManagedPtrType(lockType);
        Type * oldValueType = typeManager.getInt32Type();
        lockAddr = irBuilder.genLdLockAddr(lockAddrType,obj);
        oldLockValue = irBuilder.genBalancedMonitorEnter(oldValueType,obj,lockAddr);
    }
}

void
JavaByteCodeTranslator::genMethodMonitorExit() {
    if (translationFlags.ignoreSync || translationFlags.syncAsEnterFence) {
        popOpnd();
        return;
    }
    
    if (! translationFlags.onlyBalancedSync) {
        irBuilder.genMonitorExit(popOpnd());
    }
    else {
        assert(lockAddr != NULL && oldLockValue != NULL);
        irBuilder.genBalancedMonitorExit(popOpnd(),lockAddr,oldLockValue);
    }
}

U_32 JavaByteCodeTranslator::checkForArrayInitializer(Opnd* arrayOpnd, const U_8* byteCodes, U_32 offset, const U_32 byteCodeLength)
{
    assert(offset < byteCodeLength);
    const U_32 MIN_NUMBER_OF_INIT_ELEMS = 2;

    const U_8 BYTE_JAVA_SIZE    = 1;
    const U_8 SHORT_JAVA_SIZE   = 2;
    const U_8 INT_JAVA_SIZE     = 4;
    const U_8 LONG_JAVA_SIZE    = 8;

    // Skip short array initializers.
    // Average length of an array element initializer is 4.
    if ((byteCodeLength - offset)/4 < MIN_NUMBER_OF_INIT_ELEMS) return 0;

    // Size of the array elements
    U_8 elem_size = 0;
    // Number of initialized array elements
    U_32 elems = 0;

    ArrayType* arrayType = arrayOpnd->getType()->asArrayType();
    assert(arrayType);
    Type* elemType = arrayType->getElementType();
    if (elemType->isBoolean() || elemType->isInt1()) {
        elem_size = BYTE_JAVA_SIZE;
    } else if (elemType->isInt2() || elemType->isChar()) {
        elem_size = SHORT_JAVA_SIZE;
    } else if (elemType->isInt4() || elemType->isSingle()) {
        elem_size = INT_JAVA_SIZE;
    } else if (elemType->isInt8() || elemType->isDouble()) {
        elem_size = LONG_JAVA_SIZE;
    } else {
        assert(0);
    }

    ::std::vector<uint64> array_data;

    // Current offset.
    U_32 off = offset;
    U_32 predoff = offset;
    // Array element indexes
    uint64 oldIndex = 0;
    uint64 newIndex = 0;
    // Array element value
    uint64 value = 0;

    bool exitScan = false;
    U_32 tmpOff = 0;

    while (byteCodes[off++] == 0x59/*dup*/) {
        if (off >= byteCodeLength) break;

        // Get array element index
        tmpOff = getNumericValue(byteCodes, off, byteCodeLength, newIndex);
        if (!tmpOff || ((off += tmpOff) >= byteCodeLength)) break;
        if (newIndex != (oldIndex++)) break;

        // Get array element value
        tmpOff = getNumericValue(byteCodes, off, byteCodeLength, value);
        if (!tmpOff || ((off += tmpOff) >= byteCodeLength)) break;

        // Store array element
        switch (byteCodes[off++]) {
            case 0x4f:        // iastore
                assert(elem_size == INT_JAVA_SIZE);
                array_data.push_back((uint64)value);
                break;
            case 0x50:        // lastore
                assert(elem_size == LONG_JAVA_SIZE);
                array_data.push_back((uint64)value);
                break;
            case 0x51:        // fastore
                assert(elem_size == INT_JAVA_SIZE);
                array_data.push_back((uint64)value);
                break;
            case 0x52:        // dastore
                assert(elem_size == LONG_JAVA_SIZE);
                array_data.push_back((uint64)value);
                break;
            case 0x54:        // bastore
                assert(elem_size == BYTE_JAVA_SIZE);
                array_data.push_back((uint64)value);
                break;
            case 0x55:        // castore
                assert(elem_size == SHORT_JAVA_SIZE);
                array_data.push_back((uint64)value);
                break;
            case 0x56:        // sastore
                assert(elem_size == SHORT_JAVA_SIZE);
                array_data.push_back((uint64)value);
                break;
            default:
                exitScan = true;
                break;
        }
        if (exitScan || (off >= byteCodeLength)) break;
        predoff = off;
        elems++;
    }/*end_while*/

    if (elems < MIN_NUMBER_OF_INIT_ELEMS) return 0;

    const U_32 data_size = elems* elem_size;
    U_8* init_array_data = new U_8[data_size];

    for (U_32 i = 0; i < elems; i++) {
        switch (elem_size) {
            case BYTE_JAVA_SIZE:
                init_array_data[i] = (U_8)(array_data[i]);
                break;
            case SHORT_JAVA_SIZE:
                *((uint16*)(init_array_data + (i * SHORT_JAVA_SIZE))) = (uint16)(array_data[i]);
                break;
            case INT_JAVA_SIZE:
                *((U_32*)(init_array_data + (i * INT_JAVA_SIZE))) = (U_32)(array_data[i]);
                break;
            case LONG_JAVA_SIZE:
                *((uint64*)(init_array_data + (i * LONG_JAVA_SIZE))) = (uint64)(array_data[i]);
                break;
           default:
                assert(0);
        }
    }

    Type* returnType = typeManager.getVoidType();
    Opnd* arrayDataOpnd = irBuilder.genLdConstant((POINTER_SIZE_SINT)init_array_data);
    Opnd* arrayElemsOffset = irBuilder.genLdConstant((I_32)(arrayType->getArrayElemOffset()));
    Opnd* elemsOpnd = irBuilder.genLdConstant((I_32)data_size);

    const U_32 numArgs = 4;
    Opnd* args[numArgs] = {arrayOpnd, arrayElemsOffset, arrayDataOpnd, elemsOpnd};
    irBuilder.genJitHelperCall(InitializeArray, returnType, numArgs, args);


    return predoff - offset;
}

U_32 JavaByteCodeTranslator::getNumericValue(const U_8* byteCodes, U_32 offset, const U_32 byteCodeLength, uint64& value) {
    assert(offset < byteCodeLength);
    U_32 off = offset;
    switch (byteCodes[off++]) {
        case 0x02:        // iconst_m1
            value = (uint64)(-1);
            break;
        case 0x03:        // iconst_0
        case 0x09:        // lconst_0
            value = 0;
            break;
        case 0x04:        // iconst_1
        case 0x0a:        // lconst_1
            value = 1;
            break;
        case 0x05:        // iconst_2
            value = 2;
            break;
        case 0x06:        // iconst_3
            value = 3;
            break;
        case 0x07:        // iconst_4
            value = 4;
            break;
        case 0x08:        // iconst_5
            value = 5;
            break;
        case 0x0b:        // fconst_0
            {
                float val = 0.0f;
                value = (uint64)(*((U_32*)(&val)));
            }
            break;
        case 0x0c:        // fconst_1
            {
                float val = 1.0f;
                value = (uint64)(*((U_32*)(&val)));
            }
            break;
        case 0x0d:        // fconst_2
            {
                float val = 2.0f;
                value = (uint64)(*((U_32*)(&val)));
            }
            break;
        case 0x0e:        // dconst_0
            {
                double val = 0.0;
                value = *((uint64*)(&val));
            }
            break;
        case 0x0f:        // dconst_1
            {
                double val = 1.0;
                value = *((uint64*)(&val));
            }
            break;
        case 0x10:        // bipush
            if (off >= byteCodeLength) return 0;
            value = (uint64)si8(byteCodes + (off++));
            break;
        case 0x11:        // sipush
            if ((off + 1) >= byteCodeLength) return 0;
            value = (uint64)si16(byteCodes + off);
            off += 2;
            break;
        case 0x12:        // ldc
            {
                if (off >= byteCodeLength) return 0;
                U_32 constPoolIndex = su8(byteCodes + (off++));
                // load 32-bit quantity from constant pool
                Type* constantType = compilationInterface.getConstantType(&methodToCompile,constPoolIndex);
                if ( !(constantType->isInt4() || constantType->isSingle()) ) {
                    // only integer and floating-point types 
                    //     are implemented for streamed array loads
                    return 0;
                }
                const void* constantAddress =
                    compilationInterface.getConstantValue(&methodToCompile,constPoolIndex);
                value = *(U_32*)constantAddress;
            }
            break;
        case 0x13:        // ldc_w
            {
                if ((off + 1) >= byteCodeLength) return 0;
                U_32 constPoolIndex = su16(byteCodes + off);
                // load 32-bit quantity from constant pool
                Type* constantType = compilationInterface.getConstantType(&methodToCompile,constPoolIndex);
                if ( !(constantType->isInt4() || constantType->isSingle()) ) {
                    // only integer and floating-point types 
                    //     are implemented for streamed array loads
                    return 0;
                }
                const void* constantAddress =
                    compilationInterface.getConstantValue(&methodToCompile,constPoolIndex);
                value = *(U_32*)constantAddress;
            }
            off += 2;
            break;
        case 0x14:        // ldc2_w
            {
                if ((off + 1) >= byteCodeLength) return 0;
                U_32 constPoolIndex = su16(byteCodes + off);
                // load 64-bit quantity from constant pool
                Type* constantType = compilationInterface.getConstantType(&methodToCompile,constPoolIndex);
                if ( !(constantType->isInt8() || constantType->isDouble()) ) {
                    // only integer and floating-point types 
                    //     are implemented for streamed array loads
                    return 0;
                }
                const void* constantAddress =
                    compilationInterface.getConstantValue(&methodToCompile,constPoolIndex);
                value = *(uint64*)constantAddress;
            }
            off += 2;
            break;
        default:
            return 0;
    }
    return off - offset;
}

bool JavaByteCodeTranslator::genVMMagic(const char* mname, U_32 numArgs, Opnd **srcOpnds, Type *magicRetType) {
    Type* resType = convertVMMagicType2HIR(typeManager, magicRetType);
    Type* cmpResType = typeManager.getInt32Type();
    Opnd* tauSafe = irBuilder.genTauSafe();
    Opnd* arg0 = numArgs > 0 ? srcOpnds[0]: NULL;
    Opnd* arg1 = numArgs > 1 ? srcOpnds[1]: NULL;
    Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No);

    
    // max, one, zero
    POINTER_SIZE_INT theConst = 0;
    bool loadConst = false;
    if (!strcmp(mname, "max"))          { loadConst = true; theConst = ~(POINTER_SIZE_INT)0;}
    else if (!strcmp(mname, "one"))     { loadConst = true; theConst =  1;}
    else if (!strcmp(mname, "zero"))    { loadConst = true; theConst =  0;}
    else if (!strcmp(mname, "nullReference")) { loadConst = true; theConst =  0;}
    if (loadConst) {
        ConstInst::ConstValue v;
#ifdef _EM64T_
        v.i8 = theConst;
#else
        v.i4 = theConst;        
#endif
        Opnd* res = irBuilder.genLdConstant(typeManager.getUIntPtrType(), v);

        if (resType->isPtr()) {
            res = irBuilder.genConv(resType, resType->tag, mod, res);
        }
        pushOpnd(res);
        return true;
    }

    //
    // prefetch
    //
    if (!strcmp(mname, "prefetch"))
    {
        Opnd* prefetchingAddress = arg0;
        irBuilder.genPrefetch(prefetchingAddress);
        return true;
    }

    //
    // fromXXX, toXXX - static creation from something
    //
    if (!strcmp(mname, "fromLong") 
        || !strcmp(mname, "fromIntSignExtend") 
        || !strcmp(mname, "fromIntZeroExtend")
        || !strcmp(mname, "fromObject")  
        || !strcmp(mname, "toAddress") 
        || !strcmp(mname, "toObjectReference")
        || !strcmp(mname, "toInt")
        || !strcmp(mname, "toLong")
        || !strcmp(mname, "toObjectRef")
        || !strcmp(mname, "toWord")
        || !strcmp(mname, "toAddress")
        || !strcmp(mname, "toObject")
        || !strcmp(mname, "toExtent")
        || !strcmp(mname, "toOffset"))
    {
        assert(numArgs == 1);
        Type* srcType = arg0->getType();
        if (resType == srcType) {
            pushOpnd(irBuilder.genCopy(arg0));
            return true;
        }
        Opnd* res = NULL;
        
        if ((srcType->isObject() && resType->isUnmanagedPtr())
            || (resType->isObject() && srcType->isUnmanagedPtr())) 
        {
            res = irBuilder.genConvUnmanaged(resType, resType->tag, mod, arg0);
        } else if (!strcmp(mname, "fromIntZeroExtend")) {
            res = irBuilder.genConvZE(resType, resType->tag, mod, arg0);
        } else {
            res = irBuilder.genConv(resType, resType->tag, mod, arg0);
        }
        pushOpnd(res);
        return true;
    }

    //
    // is<Smth> one arg testing
    //
    bool isOp = false;
    if (!strcmp(mname, "isZero")) { isOp = true; theConst = 0; }
    else if (!strcmp(mname, "isMax")) { isOp = true; theConst = ~(POINTER_SIZE_INT)0; }
    else if (!strcmp(mname, "isNull")) { isOp = true; theConst = 0; }
    if (isOp) {
        assert(numArgs == 1);
        Opnd* res = irBuilder.genCmp(cmpResType, arg0->getType()->tag, Cmp_EQ, arg0, irBuilder.genLdConstant((POINTER_SIZE_SINT)theConst));
        pushOpnd(res);
        return true;
    }


    //
    // EQ, GE, GT, LE, LT, sXX - 2 args compare
    //
    ComparisonModifier cm = Cmp_Mask;
    bool commuteOpnds=false;
    if (!strcmp(mname, "EQ"))         { cm = Cmp_EQ; }
    else if (!strcmp(mname, "equals")){ cm = Cmp_EQ; }
    else if (!strcmp(mname, "NE"))    { cm = Cmp_NE_Un; }
    else if (!strcmp(mname, "GE"))    { cm = Cmp_GTE_Un;}
    else if (!strcmp(mname, "GT"))    { cm = Cmp_GT_Un; }
    else if (!strcmp(mname, "LE"))    { cm = Cmp_GTE_Un; commuteOpnds = true;}
    else if (!strcmp(mname, "LT"))    { cm = Cmp_GT_Un;  commuteOpnds = true;}
    else if (!strcmp(mname, "sGE"))   { cm = Cmp_GTE;}
    else if (!strcmp(mname, "sGT"))   { cm = Cmp_GT; }
    else if (!strcmp(mname, "sLE"))   { cm = Cmp_GTE; commuteOpnds = true;}
    else if (!strcmp(mname, "sLT"))   { cm = Cmp_GT;  commuteOpnds = true;}

    if (cm!=Cmp_Mask) {
        assert(numArgs == 2);
        assert(arg0->getType() == arg1->getType());
        Opnd* op0 = commuteOpnds ? arg1 : arg0;
        Opnd* op1 = commuteOpnds ? arg0 : arg1;
        Opnd* res = irBuilder.genCmp(cmpResType, arg0->getType()->tag, cm, op0, op1);
        pushOpnd(res);
        return true;
    }

   
    //
    // plus, minus, xor, or, and ... etc - 1,2 args arithmetics
    //
    if (!strcmp(mname, "plus")) { 
        assert(numArgs==2); 
        if (resType->isPtr()) {
            pushOpnd(irBuilder.genAddScaledIndex(arg0, arg1)); 
        } else {
            pushOpnd(irBuilder.genAdd(resType, mod, arg0, arg1)); 
        }
        return true;
    }
    if (!strcmp(mname, "minus")){ 
        assert(numArgs==2); 
        if (resType->isPtr()) {
            Type* negType = arg1->getType()->isUIntPtr() ? typeManager.getIntPtrType() : arg1->getType();
            Opnd* negArg1 = irBuilder.genNeg(negType, arg1);
            pushOpnd(irBuilder.genAddScaledIndex(arg0, negArg1)); 
        } else {
            pushOpnd(irBuilder.genSub(resType, mod, arg0, arg1)); 
        }
        return true;
    }
    if (!strcmp(mname, "or"))   { assert(numArgs==2); pushOpnd(irBuilder.genOr (resType, arg0, arg1)); return true;}
    if (!strcmp(mname, "xor"))  { assert(numArgs==2); pushOpnd(irBuilder.genXor(resType, arg0, arg1)); return true;}
    if (!strcmp(mname, "and"))  { assert(numArgs==2); pushOpnd(irBuilder.genAnd(resType, arg0, arg1)); return true;}
    if (!strcmp(mname, "not"))  { assert(numArgs==1); pushOpnd(irBuilder.genNot(resType, arg0)); return true;}
    if (!strcmp(mname, "diff")) { assert(numArgs==2); pushOpnd(irBuilder.genSub(resType, mod, arg0, arg1)); return true;}

    
    //
    // shifts
    //
    Modifier shMod(ShiftMask_Masked);
    if (!strcmp(mname, "lsh"))      {assert(numArgs==2); pushOpnd(irBuilder.genShl(resType, shMod|SignedOp, arg0, arg1));  return true;}
    else if (!strcmp(mname, "rsha")){assert(numArgs==2); pushOpnd(irBuilder.genShr(resType, shMod|SignedOp, arg0, arg1)); return true;}
    else if (!strcmp(mname, "rshl")){assert(numArgs==2); pushOpnd(irBuilder.genShr(resType, shMod |UnsignedOp, arg0, arg1)); return true;}

    
    //
    // loadXYZ.. prepareXYZ..
    //
    if (!strcmp(mname, "loadObjectReference")
        || !strcmp(mname, "loadAddress")
        || !strcmp(mname, "loadWord")
        || !strcmp(mname, "loadByte")
        || !strcmp(mname, "loadChar")
        || !strcmp(mname, "loadDouble")
        || !strcmp(mname, "loadFloat")
        || !strcmp(mname, "loadInt")
        || !strcmp(mname, "loadLong")
        || !strcmp(mname, "loadShort")
        || !strcmp(mname, "prepareWord")
        || !strcmp(mname, "prepareObjectReference")
        || !strcmp(mname, "prepareAddress")
        || !strcmp(mname, "prepareInt"))
    {
        assert(numArgs == 1 || numArgs == 2);
        Opnd* effectiveAddress = arg0;
        if (numArgs == 2) {//load by offset
            effectiveAddress = irBuilder.genAddScaledIndex(arg0, arg1);
        }
        Opnd* res = irBuilder.genTauLdInd(AutoCompress_No, resType, resType->tag, effectiveAddress, tauSafe, tauSafe);
        pushOpnd(res);
        return true;
    }

    //
    // store(XYZ)
    //
    if (!strcmp(mname, "store")) {
        assert(numArgs==2 || numArgs == 3);
        Opnd* effectiveAddress = arg0;
        if (numArgs == 3) { // store by offset
            effectiveAddress = irBuilder.genAddScaledIndex(arg0, srcOpnds[2]);
        }
        irBuilder.genTauStInd(arg1->getType(), effectiveAddress, arg1, tauSafe, tauSafe, tauSafe);
        return true;
    }

    if (!strcmp(mname, "attempt")) {
        assert(numArgs == 3 || numArgs == 4);
        Opnd* effectiveAddress = arg0;
        if (numArgs == 4) { // offset opnd
            effectiveAddress = irBuilder.genAddScaledIndex(arg0, srcOpnds[3]);
        }
        Opnd* opnds[3] = {effectiveAddress, arg1, srcOpnds[2]};
        Opnd* res = irBuilder.genJitHelperCall(LockedCompareAndExchange, resType, 3, opnds);
        pushOpnd(res);
        return true;
    }

    //
    //Arrays
    //
    if (!strcmp(mname, "create")) { assert(numArgs==1); pushOpnd(irBuilder.genNewArray(resType->asNamedType(),arg0)); return true;} 
    if (!strcmp(mname, "set")) {
        assert(numArgs == 3);
        Opnd* arg2 = srcOpnds[2];
        Type* opType = convertVMMagicType2HIR(typeManager, arg2->getType());
        irBuilder.genStElem(opType, arg0, arg1, arg2, tauSafe, tauSafe, tauSafe); 
        return true;
    }
    if (!strcmp(mname, "get")) {
        assert(numArgs == 2);
        Opnd* res = irBuilder.genLdElem(resType, arg0, arg1, tauSafe, tauSafe);
        pushOpnd(res);
        return true;
    }
    if (!strcmp(mname, "length")) {    
        pushOpnd(irBuilder.genArrayLen(typeManager.getInt32Type(), Type::Int32, arg0));
        return true;
    }

    return false;
}

bool JavaByteCodeTranslator::genVMHelper(const char* mname, U_32 numArgs, Opnd **srcOpnds, Type *returnType) {
    Type* resType = VMMagicUtils::isVMMagicClass(returnType->getName()) ? convertVMMagicType2HIR(typeManager, returnType) : returnType;

//VMHelper methods

    if (!strcmp(mname,"getTlsBaseAddress")) {
        assert(numArgs == 0);
        Opnd* res = irBuilder.genVMHelperCall(VM_RT_GC_GET_TLS_BASE, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"newResolvedUsingAllocHandleAndSize")) {
        assert(numArgs == 2);
        Opnd* res = irBuilder.genVMHelperCall(VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"newVectorUsingAllocHandle")) {
        assert(numArgs == 2);
        Opnd* res = irBuilder.genVMHelperCall(VM_RT_NEW_VECTOR_USING_VTABLE, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"monitorEnter")) {
        assert(numArgs == 1);
        irBuilder.genVMHelperCall(VM_RT_MONITOR_ENTER, resType, numArgs, srcOpnds);
        return true;
    }

    if (!strcmp(mname,"monitorExit")) {
        assert(numArgs == 1);
        irBuilder.genVMHelperCall(VM_RT_MONITOR_EXIT, resType, numArgs, srcOpnds);
        return true;
    }

    if (!strcmp(mname,"writeBarrier")) {
        assert(numArgs == 3);
        irBuilder.genVMHelperCall(VM_RT_GC_HEAP_WRITE_REF, resType, numArgs, srcOpnds);
        return true;
    }

    if (!strcmp(mname, "memset0"))
    {
	assert(numArgs == 2);
        irBuilder.genJitHelperCall(Memset0, resType, numArgs, srcOpnds);
        return true;
    }

    if (!strcmp(mname, "prefetch"))
    {
        assert(numArgs == 3);
        irBuilder.genJitHelperCall(Prefetch, resType, numArgs, srcOpnds);
        return true;
    }

    if (!strcmp(mname,"getInterfaceVTable")) {
        assert(numArgs == 2);
        Opnd* res = irBuilder.genVMHelperCall(VM_RT_GET_INTERFACE_VTABLE_VER0, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"checkCast")) {
        assert(numArgs == 2);
        irBuilder.genVMHelperCall(VM_RT_CHECKCAST, resType, numArgs, srcOpnds);
        return true;
    }

    if (!strcmp(mname,"instanceOf")) {
        assert(numArgs == 2);
        Opnd* res = irBuilder.genVMHelperCall(VM_RT_INSTANCEOF, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }


    //no VMHelpers exist for these magics -> use internal JIT helpers

    if (!strcmp(mname,"isArray")) {
        assert(numArgs == 1 && srcOpnds[0]->getType()->isUnmanagedPtr());
        Opnd* res = irBuilder.genJitHelperCall(ClassIsArray, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"getAllocationHandle")) {
        assert(numArgs == 1 && srcOpnds[0]->getType()->isUnmanagedPtr());
        Opnd* res = irBuilder.genJitHelperCall(ClassGetAllocationHandle, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"getTypeSize")) {
        assert(numArgs == 1 && srcOpnds[0]->getType()->isUnmanagedPtr());
        Opnd* res = irBuilder.genJitHelperCall(ClassGetTypeSize, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"getArrayElemSize")) {
        assert(numArgs == 1 && srcOpnds[0]->getType()->isUnmanagedPtr());
        Opnd* res = irBuilder.genJitHelperCall(ClassGetArrayElemSize, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"isInterface")) {
        assert(numArgs == 1 && srcOpnds[0]->getType()->isUnmanagedPtr());
        Opnd* res = irBuilder.genJitHelperCall(ClassIsInterface, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"isFinal")) {
        assert(numArgs == 1 && srcOpnds[0]->getType()->isUnmanagedPtr());
        Opnd* res = irBuilder.genJitHelperCall(ClassIsFinal, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"getArrayClass")) {
        assert(numArgs == 1 && srcOpnds[0]->getType()->isUnmanagedPtr());
        Opnd* res = irBuilder.genJitHelperCall(ClassGetArrayClass, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"isFinalizable")) {
        assert(numArgs == 1 && srcOpnds[0]->getType()->isUnmanagedPtr());
        Opnd* res = irBuilder.genJitHelperCall(ClassIsFinalizable, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"getFastTypeCheckDepth")) {
        assert(numArgs == 1 && srcOpnds[0]->getType()->isUnmanagedPtr());
        Opnd* res = irBuilder.genJitHelperCall(ClassGetFastCheckDepth, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"isVMMagicPackageSupported")) {
        assert(numArgs == 0);
        ObjectType* base = compilationInterface.findClassUsingBootstrapClassloader(VMHELPER_TYPE_NAME);
        int ready = base!=NULL && !base->needsInitialization() ? 1 : 0;
        Opnd* res = irBuilder.genLdConstant((I_32)ready);
        pushOpnd(res);
        return true;
    }

    if (!strcmp(mname,"getHashcode")) {
        assert(numArgs == 1);
        Opnd* res = irBuilder.genVMHelperCall(VM_RT_GET_IDENTITY_HASHCODE, resType, numArgs, srcOpnds);
        pushOpnd(res);
        return true;
    }
    
    return false;
}

} //namespace Jitrino 
