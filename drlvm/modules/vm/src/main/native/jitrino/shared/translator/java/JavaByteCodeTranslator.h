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
 *
 */

#ifndef _JAVABYTECODETRANSLATOR_H_
#define _JAVABYTECODETRANSLATOR_H_

#include "JavaTranslator.h"
#include "IRBuilder.h"
#include "JavaByteCodeParser.h"
#include "JavaLabelPrepass.h"
#include "JavaFlowGraphBuilder.h"

#include "VMInterface.h"

namespace Jitrino {

class Opnd;

class JavaByteCodeTranslator : public JavaByteCodeParserCallback {
public:
    virtual ~JavaByteCodeTranslator() {
    }

    // version for non-inlined methods
    JavaByteCodeTranslator(CompilationInterface& compilationInterface,
                    MemoryManager&,
                    IRBuilder&,
                    ByteCodeParser&,
                    MethodDesc& methodToCompile,
                    TypeManager& typeManager,
                    JavaFlowGraphBuilder& cfg);
    
    

    void offset(U_32 offset);
    void offset_done(U_32 offset);
    void checkStack();
    // called before parsing starts
    virtual void parseInit() {
        if (Log::isEnabled()) Log::out() << ::std::endl << "================= TRANSLATOR STARTED =================" << ::std::endl << ::std::endl;
    }
    // called after parsing ends, but not if an error occurs
    virtual void parseDone();
    // called when an error occurs during the byte code parsing
    void parseError();
    Opnd* getResultOpnd() {    // for inlined methods only
        return resultOpnd;
    }
    // generates the argument loading code
    void genArgLoads();
    // generated argument loading for inlined methods
    void genArgLoads(U_32 numActualArgs,Opnd** actualArgs);

    void nop();
    void aconst_null();
    void iconst(I_32 val);
    void lconst(int64 val);
    void fconst(float val);
    void dconst(double val);
    void bipush(I_8 val);
    void sipush(int16 val);
    void ldc(U_32 constPoolIndex);
    void ldc2(U_32 constPoolIndex);
    void iload(uint16 varIndex);
    void lload(uint16 varIndex);
    void fload(uint16 varIndex);
    void dload(uint16 varIndex);
    void aload(uint16 varIndex);
    void iaload();
    void laload();
    void faload();
    void daload();
    void aaload();
    void baload();
    void caload();
    void saload();
    void istore(uint16 varIndex,U_32 off);
    void lstore(uint16 varIndex,U_32 off);
    void fstore(uint16 varIndex,U_32 off);
    void dstore(uint16 varIndex,U_32 off);
    void astore(uint16 varIndex,U_32 off);
    void iastore();
    void lastore();
    void fastore();
    void dastore();
    void aastore();
    void bastore();
    void castore();
    void sastore();
    void pop();
    void pop2();
    void dup();
    void dup_x1();
    void dup_x2();
    void dup2();
    void dup2_x1();
    void dup2_x2();
    void swap();
    void iadd();
    void ladd();
    void fadd();
    void dadd();
    void isub();
    void lsub();
    void fsub();
    void dsub();
    void imul();
    void lmul();
    void fmul();
    void dmul();
    void idiv();
    void ldiv();
    void fdiv();
    void ddiv();
    void irem();
    void lrem();
    void frem();
    void drem();
    void ineg();
    void lneg();
    void fneg();
    void dneg();
    void ishl();
    void lshl();
    void ishr();
    void lshr();
    void iushr();
    void lushr();
    void iand();
    void land();
    void ior();
    void lor();
    void ixor();
    void lxor();
    void iinc(uint16 varIndex,I_32 amount);
    void i2l();
    void i2f();
    void i2d();
    void l2i();
    void l2f();
    void l2d();
    void f2i();
    void f2l();
    void f2d();
    void d2i();
    void d2l();
    void d2f();
    void i2b();
    void i2c();
    void i2s();
    void lcmp();
    void fcmpl();
    void fcmpg();
    void dcmpl();
    void dcmpg();
    void ifeq(U_32 targetOffset,U_32 nextOffset);
    void ifne(U_32 targetOffset,U_32 nextOffset);
    void iflt(U_32 targetOffset,U_32 nextOffset);
    void ifge(U_32 targetOffset,U_32 nextOffset);
    void ifgt(U_32 targetOffset,U_32 nextOffset);
    void ifle(U_32 targetOffset,U_32 nextOffset);
    void if_icmpeq(U_32 targetOffset,U_32 nextOffset);
    void if_icmpne(U_32 targetOffset,U_32 nextOffset);
    void if_icmplt(U_32 targetOffset,U_32 nextOffset);
    void if_icmpge(U_32 targetOffset,U_32 nextOffset);
    void if_icmpgt(U_32 targetOffset,U_32 nextOffset);
    void if_icmple(U_32 targetOffset,U_32 nextOffset);
    void if_acmpeq(U_32 targetOffset,U_32 nextOffset);
    void if_acmpne(U_32 targetOffset,U_32 nextOffset);
    void goto_(U_32 targetOffset,U_32 nextOffset);
    void jsr(U_32 offset, U_32 nextOffset);
    void ret(uint16 varIndex, const U_8* byteCodes);
    void tableswitch(JavaSwitchTargetsIter*);
    void lookupswitch(JavaLookupSwitchTargetsIter*);
    void ireturn(U_32 off);
    void lreturn(U_32 off);
    void freturn(U_32 off);
    void dreturn(U_32 off);
    void areturn(U_32 off);
    void return_(U_32 off);
    void getstatic(U_32 constPoolIndex);
    void putstatic(U_32 constPoolIndex);
    void getfield(U_32 constPoolIndex);
    void putfield(U_32 constPoolIndex);
    void invokevirtual(U_32 constPoolIndex);
    void invokespecial(U_32 constPoolIndex);
    void invokestatic(U_32 constPoolIndex);
    void invokeinterface(U_32 constPoolIndex,U_32 count);
    void new_(U_32 constPoolIndex);
    void newarray(U_8 type);
    void anewarray(U_32 constPoolIndex);
    void arraylength();
    void athrow();
    void checkcast(U_32 constPoolIndex);
    int  instanceof(const U_8* bcp, U_32 constPoolIndex, U_32 off) ;
    void monitorenter();
    void monitorexit();
    void multianewarray(U_32 constPoolIndex,U_8 dimensions);
    void ifnull(U_32 targetOffset,U_32 nextOffset);
    void ifnonnull(U_32 targetOffset,U_32 nextOffset);
private:

    typedef StlMap<U_32, Inst*> OffsetToInstMap;

    //
    // helper methods for generating code
    //
    Opnd**  popArgs(U_32 numArgs);
    // for invoke emulation if resolution fails
    void    pseudoInvoke(const char* mdesc);
    void    genCallWithResolve(JavaByteCodes bc, unsigned cpIndex);
    void    invalid();    // called when invalid IR is encountered
    void    genLdVar(U_32 varIndex,JavaLabelPrepass::JavaVarType javaType);
    void    genStVar(U_32 varIndex,JavaLabelPrepass::JavaVarType javaType);
    void    genTypeStVar(uint16 varIndex);
    void    genReturn(JavaLabelPrepass::JavaVarType javaType,U_32 off);
    void    genReturn(U_32 off);
    void    genAdd(Type* dstType);
    void    genSub(Type* dstType);
    void    genMul(Type* dstType);
    void    genDiv(Type* dstType);
    void    genRem(Type* dstType);
    void    genNeg(Type* dstType);
    void    genFPAdd(Type* dstType);
    void    genFPSub(Type* dstType);
    void    genFPMul(Type* dstType);
    void    genFPDiv(Type* dstType);
    void    genFPRem(Type* dstType);
    void    genAnd(Type* dstType);
    void    genOr(Type* dstType);
    void    genXor(Type* dstType);
    void    genArrayLoad(Type* type);
    void    genTypeArrayLoad();
    void    genArrayStore(Type* type);
    void    genTypeArrayStore();
    void    genShl(Type* type, ShiftMaskModifier mod);
    void    genShr(Type* type, SignedModifier mod1, ShiftMaskModifier mod2);
    void    genIf1(ComparisonModifier,I_32 targetOffset,I_32 nextOffset);
    void    genIf1Commute(ComparisonModifier,I_32 targetOffset,I_32 nextOffset);
    void    genIf2(ComparisonModifier,I_32 targetOffset,I_32 nextOffset);
    void    genIf2Commute(ComparisonModifier mod,I_32 targetOffset,I_32 nextOffset);
    void    genIfNull(ComparisonModifier,I_32 targetOffset,I_32 nextOffset);
    void    genThreeWayCmp(Type::Tag cmpType,ComparisonModifier src1ToSrc2); // Src2toSrc1 must be same
    //
    // LinkingException throw
    // 
    void linkingException(U_32 constPoolIndex, U_32 operation);
    //
    // helper methods for inlining, call and return sequences
    //
    bool    needsReturnLabel(U_32 off);
    void    genInvokeStatic(MethodDesc * methodDesc,U_32 numArgs,Opnd ** srcOpnds,Type * returnType);
    bool    genVMMagic(const char* mname, U_32 numArgs,Opnd ** srcOpnds,Type * returnType);
    bool    genVMHelper(const char* mname, U_32 numArgs,Opnd ** srcOpnds,Type * returnType);
    
    bool    genMinMax(MethodDesc * methodDesc,U_32 numArgs,Opnd ** srcOpnds, Type * returnType);
    void    newFallthroughBlock();

    
    // 
    // initialization
    //
    void    initJsrEntryMap();
    void    initArgs();
    void    initLocalVars();

    //
    // labels and control flow
    //
    U_32            labelId(U_32 offset);
    LabelInst* getLabel(U_32 labelId) {
        assert(labelId < numLabels);
        LabelInst *label = labels[labelId];
        return label;
    }
    void       setLabel(U_32 labelId, LabelInst *label) {
        labels[labelId] = label;
    }
    LabelInst* getNextLabel() {
        return labels[nextLabel];
    }
    U_32    getNextLabelId() {
        assert(nextLabel < numLabels);
        return nextLabel++;
    }
    //
    // operand stack manipulation
    //
    Opnd*            topOpnd();
    Opnd*            popOpnd();
    Opnd*            popOpndStVar();
    void             pushOpnd(Opnd* opnd);
    //
    // field, method, and type resolution
    //
    Type*            getFieldType(FieldDesc*, U_32 constPoolIndex);
    const char*      methodSignatureString(U_32 cpIndex);
    //
    //
    // locals access
    //
    Opnd*            getVarOpndLdVar(JavaLabelPrepass::JavaVarType javaType,U_32 index);
    VarOpnd*         getVarOpndStVar(JavaLabelPrepass::JavaVarType javaType,U_32 index,Opnd *opnd);
    bool             needsReturnLabel();
    //
    //  synchronization
    //
    void             genMethodMonitorEnter();
    void             genMethodMonitorExit();
    
    void             applyMaskToTop(I_32 mask);

    // Method checks if following bytecodes are array initializers for newly created array.
    // If they are then substitute array initializers with jit helper array copy instruction.
    // Returns the length of bytecodes converted by this routine.
    U_32 checkForArrayInitializer(Opnd* arrayOpnd, const U_8* byteCodes, U_32 offset, const U_32 byteCodeLength);
    // Obtain the next numeric value from the bytecode in array initialization sequence
    // Returns number of bytes read from the byteCodes array.
    U_32 getNumericValue(const U_8* byteCodes, U_32 offset, const U_32 byteCodeLength, uint64& value);

    //
    // private fields
    //
    MemoryManager&             memManager;

    CompilationInterface&    compilationInterface;
    //
    // references to other compiler objects
    //
    MethodDesc&              methodToCompile;
    ByteCodeParser&          parser;
    TypeManager&             typeManager;
    IRBuilder&               irBuilder;
    TranslatorFlags   translationFlags;
    JavaFlowGraphBuilder&   cfgBuilder;
    //
    // byte code parsing state
    //
    OpndStack           opndStack;          // operand stack
    Opnd**              locVarPropValues;   // value propagation for variables
    Type**              varState;           //  variable state (in terms of type)
    bool                lastInstructionWasABranch;    // self explanatory
    bool                moreThanOneReturn;  // true if there is more than one return
    bool                jumpToTheEnd;       // insert an extra jump to the exit node on return
    //
    // used for IR inlining
    //
    Opnd**              returnOpnd;         // if non-null, used this operand
    Node **          returnNode;         // returns the node where the return is located
    //
    // method state
    //
    ExceptionInfo*      inliningExceptionInfo; // instruction where inlining begins
    Node*            inliningNodeBegin;  // used by inlining of synchronized methods
    U_32              numLabels;          // number of labels in this method
    U_32              numVars;            // number of variables in this method
    U_32              numStackVars;       // number of non-empty stack locations in this method
    U_32              numArgs;            // number of arguments in this method
    Type**              argTypes;           // types for each argument
    Opnd**              args;               // argument opnds
    Opnd*               resultOpnd;         // used for inlining only
    Type*               retType;            // return type of method
    U_32              nextLabel;
    LabelInst**         labels;
    Type*               javaTypeMap[JavaLabelPrepass::NumJavaVarTypes];
    JavaLabelPrepass    prepass;
    StateInfo           *stateInfo;
    U_32              firstVarRef;
    U_32              numberVarRef;
    // Synchronization
    Opnd*               lockAddr;
    Opnd*               oldLockValue;
    
    //
    // mapping: 
    //   [ subroutine entry stvar inst ] -> [ ret inst ]
    //   taken from parent if translating inlined method
    // this mapping should be provided but not used since
    //   inlining translators update parent maps
    //
    JsrEntryInstToRetInstMap* jsrEntryMap;

    //
    // mapping to bytecode offsets:
    // * 'ret' instructions
    // * subroutine entries (=='jsr' targets)
    //
    OffsetToInstMap retOffsets, jsrEntryOffsets;
};

} //namespace Jitrino 

#endif //  _JAVABYTECODETRANSLATOR_H_
