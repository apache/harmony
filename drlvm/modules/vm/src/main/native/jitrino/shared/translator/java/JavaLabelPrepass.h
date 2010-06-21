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

#ifndef _JAVALABELPREPASS_H_
#define _JAVALABELPREPASS_H_

#include "Log.h"
#include "Stl.h"
#include "open/types.h"
#include "VMInterface.h"
#include "BitSet.h"
#include "JavaByteCodeParser.h"
#include "Type.h"
#include "Opnd.h"
#include "ExceptionInfo.h"

namespace Jitrino {

class StateTable;

class VariableIncarnation : private Dlink {
public:
    VariableIncarnation(U_32 offset, Type*);
    void setMultipleDefs();
    Type* getDeclaredType();
    void setDeclaredType(Type*);

    // Link two chains of var incarnations and assign them a specified common type
    static void linkAndMergeIncarnations(VariableIncarnation*, VariableIncarnation*, Type*, TypeManager*);
    // Link two chains of var incarnations and assign them a common type
    static void linkAndMergeIncarnations(VariableIncarnation*, VariableIncarnation*, TypeManager*);
    // Link two chains of var incarnations
    static void linkIncarnations(VariableIncarnation*, VariableIncarnation*);
    // Merge a chain of var incarnations to assign them a specified common type
    void mergeIncarnations(Type*, TypeManager*);
    // Merge a chain of var incarnations to assign them a new type common for all incarnations
    void mergeIncarnations(TypeManager*);
    // Set common type for a chain of incarnations
    void setCommonType(Type*);
    void print(::std::ostream& out);

    Opnd* getOpnd();
    Opnd* getOrCreateOpnd(IRBuilder*);
    void createMultipleDefVarOpnd(IRBuilder*);
    void setTmpOpnd(Opnd*);
protected:
    void createVarOpnd(IRBuilder*);
private:
    friend class SlotVar;
    I_32   definingOffset;  // offset where the def was found, -1 if multiple defs
    Type*   declaredType;
    Opnd*   opnd;
};

class SlotVar : private Dlink {
public:
    SlotVar(VariableIncarnation* varInc): var(varInc), linkOffset(0) {
        _prev = _next = NULL;
    }
    SlotVar(SlotVar* sv, MemoryManager& mm) {
        assert(sv);
        var = sv->getVarIncarnation();
        linkOffset = sv->getLinkOffset();
        _prev = _next = NULL;
        for (sv = (SlotVar*)sv->_next; sv; sv = (SlotVar*)sv->_next) {
            addVarIncarnations(sv, mm, sv->getLinkOffset());
        }
    }
    // Add var incarnations from SlotVar to the list.
    // Return true if var incarnation has been added.
    bool addVarIncarnations(SlotVar* var, MemoryManager& mm, U_32 linkOffset);
    VariableIncarnation* getVarIncarnation() {return var;}
    void mergeVarIncarnations(TypeManager* tm);
    U_32 getLinkOffset() {return linkOffset;}
    void print(::std::ostream& out);
private:
    VariableIncarnation* var;
    U_32 linkOffset;
};


class StateInfo {
public:
    StateInfo():  flags(0), stackDepth(0), stack(NULL), exceptionInfo(NULL) {}

    ExceptionInfo *getExceptionInfo()       { return exceptionInfo; }
    void           setCatchLabel()          { flags |= 1;           }
    void           setSubroutineEntry()     { flags |= 2;           }
    void           setFallThroughLabel()    { flags |= 4;           }
    void           clearFallThroughLabel()  { flags &=~4;           }
    void           setVisited()             { flags |= 8;           }
    bool           isCatchLabel()           { return (flags & 1) != 0;     }
    bool           isSubroutineEntry()      { return (flags & 2) != 0;     }
    bool           isFallThroughLabel()     { return (flags & 4) != 0;     }
    bool           isVisited()              { return (flags & 8) != 0;     }

    //     Catch-blocks should be listed in the same order as the 
    //     corresponding exception table entries were listed in byte-code. (according to VM spec)
    void  addExceptionInfo(CatchBlock* info); 
    void  addCatchHandler(CatchHandler* info); 

    struct SlotInfo {
        Type *type;
        U_32 varNumber;
        uint16 slotFlags;
        SlotVar *vars;
        U_32 jsrLabelOffset;
        SlotInfo() : type(NULL), varNumber(0), slotFlags(0), vars(NULL), jsrLabelOffset(0){}
        void setVarNumber(U_32 n) { varNumber = n;slotFlags |= VarNumberIsSet; }
    };

    // Push type to modelled operand stack
    SlotInfo& push(Type *type);

    // Obtain top slot of modelled operand stack
    SlotInfo& top();

    // remove all slots containing returnAddress for RET instruction with jsrNexOffset == offset
    void cleanFinallyInfo(U_32 offset);

    /* flags */
    enum {
        VarNumberIsSet = 0x01,
        IsNonNull      = 0x02,
        IsExactType    = 0x04,
        ChangeState    = 0x08,
        StackOpndAlive = 0x10,  // the following to get rid of phi nodes in the translator
        StackOpndSaved = 0x20   // the following to get rid of phi nodes in the translator
    };

    static bool isVarNumberSet(struct SlotInfo s) { return (s.slotFlags & VarNumberIsSet) != 0; }
    static bool isNonNull(struct SlotInfo s)      { return (s.slotFlags & IsNonNull)      != 0; }
    static bool isExactType(struct SlotInfo s)    { return (s.slotFlags & IsExactType)    != 0; }
    static bool changeState(struct SlotInfo s)    { return (s.slotFlags & ChangeState)    != 0; }
    static void setNonNull(struct SlotInfo *s)     { s->slotFlags |= IsNonNull;      }
    static void setExactType(struct SlotInfo *s)   { s->slotFlags |= IsExactType;    }
    static void clearExactType(struct SlotInfo *s) { s->slotFlags &= ~IsExactType;   }
    static void setChangeState(struct SlotInfo *s) { s->slotFlags |= ChangeState;    }
    static void print(SlotInfo& s, ::std::ostream& os) {
        Log::out() << "\ttype: ";
        if (s.type == NULL)
            os << "NULL";
        else
            s.type->print(os);
        if (isVarNumberSet(s)) os << " ->[" <<(int)s.varNumber<< "],";
        if (isNonNull(s))      os << ",nn";
        if (isExactType(s))    os << ",ex";
        if (changeState(s))    os << ",cs";
        //Log::out() << ::std::endl;
        Log::out() << "\tvar: ";
        if (s.vars) {
            s.vars->print(Log::out());
        } else {
            Log::out() << "NULL";
        }
    }
    // add flags as needed
    friend class JavaLabelPrepass;
    friend class JavaByteCodeTranslator;
    unsigned        flags;
    unsigned        stackDepth;
    struct SlotInfo*  stack;
    ExceptionInfo *exceptionInfo;
};


class JavaLabelPrepass : public JavaByteCodeParserCallback {
public:
    typedef StlList<CatchBlock*> ExceptionTable;

    virtual ~JavaLabelPrepass() {
    }

    // for non-inlined methods the actual operands are null
    JavaLabelPrepass(MemoryManager& mm,
                     TypeManager& tm, 
                     MemoryManager& irManager,
                     MethodDesc&  md,
                     CompilationInterface& ci,
                     Opnd** actualArgs);    // NULL for non-inlined methods

    bool    isLabel(U_32 offset)            { return labels->getBit(offset); }
    bool    isSubroutineEntry(U_32 offset)  { return subroutines->getBit(offset); }
    bool    getHasJsrLabels()                 { return hasJsrLabels;}
    U_32  getNumLabels()                    { return numLabels;}
    U_32  getNumVars()                      { return numVars;}
    U_32  getLabelId(U_32 offset);
    void    print_loc_vars(U_32 offset, U_32 index);
    //
    // exception info
    //

    enum JavaVarType {
         None = 0, A = 1, I = 2, L = 3, F = 4, D = 5, RET = 6, // JSR return address
         NumJavaVarTypes = 7
    };

    ExceptionTable& getExceptionTable() {return exceptionTable;}

    ///////////////////////////////////////////////////////////////////////////
    // Java Byte code parser callbacks
    ///////////////////////////////////////////////////////////////////////////

    // called before each byte code to indicate the next byte code's offset
    void offset(U_32 offset);

    // called after each byte code offset is worked out
    void offset_done(U_32 offset) {}

    // called when an error occurs during the byte codes parsing
    void parseError();

    // called to initialize parsing
    void parseInit() {
        if (Log::isEnabled()) Log::out() << ::std::endl << "================= PREPASS STARTED =================" << ::std::endl << ::std::endl;
    }

    // called to indicate end of parsing
    void parseDone();

    // Variable information
    VariableIncarnation* getVarInc(U_32 offset, U_32 index);
    VariableIncarnation* getOrCreateVarInc(U_32 offset, U_32 index, Type* type);
    void                 createMultipleDefVarOpnds(IRBuilder*);

    //
    // operand stack manipulation (to keep track of state only !)
    //
    StateInfo::SlotInfo& topType();
    StateInfo::SlotInfo& popType();
    void                    popAndCheck(Type *type);
    void                    popAndCheck(JavaVarType type);
    void                    pushType(StateInfo::SlotInfo& slot);
    void                    pushType(Type *type, U_32 varNumber);
    void                    pushType(Type *type);
    bool isCategory2(StateInfo::SlotInfo& slot) { return slot.type == int64Type || slot.type == doubleType; }

    //
    bool        allExceptionTypesResolved() {return problemTypeToken == MAX_UINT32;}
    unsigned    getProblemTypeToken() {return problemTypeToken;}

    // cut and paste from Java_Translator.cpp
    // field, method, and type resolution
    //
    const char*             methodSignatureString(U_32 cpIndex);
    StateInfo*              getStateInfo()  { return &stateInfo; }
    StateTable*             getStateTable() { return stateTable; }

    static JavaVarType getJavaType(Type *type) {
        assert(type);
        switch(type->tag) {
        case Type::Boolean:  case Type::Char:
        case Type::Int8:     case Type::Int16:     case Type::Int32:
            return I;
        case Type::Int64:
            return L;
        case Type::Single:
            return F;
        case Type::Double:
            return D;
        case Type::Array:           
        case Type::Object:
        case Type::NullObject:
        case Type::UnresolvedObject:
        case Type::SystemString:
        case Type::SystemObject:
        case Type::SystemClass:
        case Type::CompressedArray:           
        case Type::CompressedObject:
        case Type::CompressedNullObject:
        case Type::CompressedUnresolvedObject:
        case Type::CompressedSystemString:
        case Type::CompressedSystemObject:
            return A;
        case Type::IntPtr: // reserved for JSR
            return RET;
        default:
            ::std::cerr << "UNKNOWN "; type->print(::std::cerr); ::std::cerr << ::std::endl;
            assert(0);
            return None;
        }
    }

    // 
    // helper functions
    //
    void genReturn    (Type *type);
    void genLoad      (Type *type, U_32 index);
    void genTypeLoad  (U_32 index);
    void genStore     (Type *type, U_32 index, U_32 offset);
    void genTypeStore (U_32 index, U_32 offset);
    void genArrayLoad (Type *type);
    void genTypeArrayLoad();
    void genArrayStore(Type *type);
    void genTypeArrayStore();
    void genBinary    (Type *type);
    void genUnary     (Type *type);
    void genShift     (Type *type);
    void genConv      (Type *from, Type *to);
    void genCompare   (Type *type);
    void invoke       (MethodDesc *mdesc);
    void pseudoInvoke (const char* mdesc);
    static U_32  getNumArgsBySignature(const char* methodSig);
    static Type*   getRetTypeBySignature(CompilationInterface& ci, Class_Handle enclClass, const char* methodSig);
    static Type*   getTypeByDescriptorString(CompilationInterface& ci, Class_Handle enclClass, const char* descriptorString, U_32& len);

    // remaining instructions

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
    void iload(uint16 varIndex) ;
    void lload(uint16 varIndex) ;
    void fload(uint16 varIndex) ;
    void dload(uint16 varIndex) ;
    void aload(uint16 varIndex) ;
    void iaload() ;
    void laload() ;
    void faload() ;
    void daload() ;
    void aaload() ;
    void baload() ;
    void caload() ;
    void saload() ;
    void istore(uint16 varIndex, U_32 off) ;
    void lstore(uint16 varIndex, U_32 off) ;
    void fstore(uint16 varIndex, U_32 off) ;
    void dstore(uint16 varIndex, U_32 off) ;
    void astore(uint16 varIndex, U_32 off) ;
    void iastore() ;
    void lastore() ;
    void fastore() ;
    void dastore() ;
    void aastore() ;
    void bastore() ;
    void castore() ;
    void sastore() ;
    void pop() ;
    void pop2() ;
    void dup() ;
    void dup_x1() ;
    void dup_x2() ;
    void dup2() ;
    void dup2_x1() ;
    void dup2_x2() ;
    void swap() ;
    void iadd() ;
    void ladd() ;
    void fadd() ;
    void dadd() ;
    void isub() ;
    void lsub() ;
    void fsub() ;
    void dsub() ;
    void imul() ;
    void lmul() ;
    void fmul() ;
    void dmul() ;
    void idiv() ;
    void ldiv() ;
    void fdiv() ;
    void ddiv() ;
    void irem() ;
    void lrem() ;
    void frem() ;
    void drem() ;
    void ineg() ;
    void lneg() ;
    void fneg() ;
    void dneg() ;
    void ishl() ;
    void lshl() ;
    void ishr() ;
    void lshr() ;
    void iushr() ;
    void lushr() ;
    void iand() ;
    void land() ;
    void ior() ;
    void lor() ;
    void ixor() ;
    void lxor() ;
    void iinc(uint16 varIndex,I_32 amount) ;
    void i2l() ;
    void i2f() ;
    void i2d() ;
    void l2i() ;
    void l2f() ;
    void l2d() ;
    void f2i() ;
    void f2l() ;
    void f2d() ;
    void d2i() ;
    void d2l() ;
    void d2f() ;
    void i2b() ;
    void i2c() ;
    void i2s() ;
    void lcmp() ;
    void fcmpl() ;
    void fcmpg() ;
    void dcmpl() ;
    void dcmpg() ;
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
    void incrementReturn();
    void ireturn(U_32 off);
    void lreturn(U_32 off);
    void freturn(U_32 off);
    void dreturn(U_32 off);
    void areturn(U_32 off);
    void return_(U_32 off);
    void getstatic(U_32 constPoolIndex) ;
    void putstatic(U_32 constPoolIndex) ;
    void getfield(U_32 constPoolIndex) ;
    void putfield(U_32 constPoolIndex) ;
    void invokevirtual(U_32 constPoolIndex) ;
    void invokespecial(U_32 constPoolIndex) ;
    void invokestatic(U_32 constPoolIndex) ;
    void invokeinterface(U_32 constPoolIndex,U_32 count) ;
    void new_(U_32 constPoolIndex) ;
    void newarray(U_8 type) ;
    void anewarray(U_32 constPoolIndex) ;
    void arraylength() ;
    void athrow() ;
    void checkcast(U_32 constPoolIndex) ;
    int  instanceof(const U_8* bcp, U_32 constPoolIndex, U_32 off) ;
    void monitorenter() ;
    void monitorexit() ;
    void multianewarray(U_32 constPoolIndex,U_8 dimensions) ;
    void ifnull(U_32 targetOffset,U_32 nextOffset);
    void ifnonnull(U_32 targetOffset,U_32 nextOffset);
    void pushCatchLabel(U_32 offset) {
        labelStack->push((U_8*)methodDesc.getByteCodes()+offset);
    }
    void pushRestart(U_32 offset) {
        labelStack->push((U_8*)methodDesc.getByteCodes()+offset);
    }
private:
    friend class JavaExceptionParser;
    friend struct CatchOffsetVisitor;
    friend class JavaByteCodeTranslator;

    typedef StlMultiMap<U_32, U_32> JsrEntryToJsrNextMap;
    typedef std::pair<JsrEntryToJsrNextMap::const_iterator, JsrEntryToJsrNextMap::const_iterator> JsrEntriesMapCIterRange;
    typedef StlMap<U_32, U_32> RetToSubEntryMap;

    // compilation environment
    MemoryManager&    memManager;
    TypeManager&    typeManager;
    MethodDesc&     methodDesc;
    CompilationInterface& compilationInterface;
    // simulates the stack operation
    int             blockNumber;
    StateInfo       stateInfo;
    StateTable*     stateTable;
    // information about variables
    StlHashMap<U_32,VariableIncarnation*> localVars;
    // basic label info
    bool            nextIsLabel;
    BitSet*         labels;
    BitSet*         subroutines;
    U_32*         labelOffsets;    // array containing offsets of labels
    U_32          numLabels;
    U_32          numVars; 
    bool            isFallThruLabel;
    // exception info
    U_32          numCatchHandlers;
    // Java JSR
    bool            hasJsrLabels;
    //
    // mapping [Subroutine entry offset] -> [JSR next offset (=offset of instruction that immediately follows the JSR) ]
    //
    JsrEntryToJsrNextMap jsrEntriesMap;
    //
    // mapping [RET offset] -> [Soubroutine entry offset (target of JSR inst)]
    //
    RetToSubEntryMap retToSubEntryMap;

    // helpers
    Type           *int32Type, *int64Type, *singleType, *doubleType;
    ExceptionTable exceptionTable;

    // if an exception type can not be resolved, its token is being kept here
    unsigned       problemTypeToken;

    // private helper methods
    void setLabel(U_32 offset);
    void setSubroutineEntry(U_32 offset) { subroutines->setBit(offset,true); }
    void checkTargetForRestart(U_32 target);
    void propagateStateInfo(U_32 offset, bool isFallthru);
    void setJsrLabel(U_32 offset);
    void setStackVars();
    void propagateLocalVarToHandlers(U_32 varIndex);
    RetToSubEntryMap* getRetToSubEntryMapPtr() { return &retToSubEntryMap; }
};



class StateTable 
{
public:
    virtual ~StateTable() {
    }

    StateTable(MemoryManager& mm,TypeManager& tm, JavaLabelPrepass& jlp, U_32 numstack, U_32 numvars) :
               memManager(mm), typeManager(tm), prepass(jlp),
               hashtable(mm), maxDepth(numvars + numstack), numVars(numvars)
               {
                    assert(sizeof(POINTER_SIZE_INT)>=sizeof(U_32));
                    assert(sizeof(U_32*)>=sizeof(U_32));
               }
    StateInfo *getStateInfo(U_32 offset) {
        return hashtable[offset];
    }
    StateInfo *createStateInfo(U_32 offset, unsigned stackDepth = MAX_UINT32) {
        StateInfo *state = hashtable[offset];
        if (state == NULL) {
            state = new (memManager) StateInfo();
            hashtable[offset] = state;
        }
        if (stackDepth != MAX_UINT32 && state->stack == NULL) {
            state->stack = new (memManager) StateInfo::SlotInfo[maxDepth];
            state->stackDepth = stackDepth;
        }
        if(Log::isEnabled()) {
            Log::out() << "CREATESTATE " <<(int)offset << " depth " << state->stackDepth << ::std::endl;
            //printState(state);
        }
        return state;
    }

    void copySlotInfo(StateInfo::SlotInfo& to, StateInfo::SlotInfo& from);
    void mergeSlots(StateInfo::SlotInfo* inSlot, StateInfo::SlotInfo* slot, U_32 offset, bool isVar);
    void rewriteSlots(StateInfo::SlotInfo* inSlot, StateInfo::SlotInfo* slot, U_32 offset, bool isVar);
    void setStackInfo(StateInfo *inState, U_32 offset, bool includeVars, bool includeStack);
    void setStateInfo(StateInfo *inState, U_32 offset, bool isFallThru, bool varsOnly = false);
    void setStateInfoFromFinally(StateInfo *inState, U_32 offset);

    void restoreStateInfo(StateInfo *stateInfo, U_32 offset) {
        if(Log::isEnabled()) {
            Log::out() << "INIT_STATE_FOR_BLOCK " <<(int)offset << " depth " << stateInfo->stackDepth << ::std::endl;
            printState(stateInfo);
        }
        StateInfo *state = hashtable[offset]; 
        assert(state != NULL && (state->stack || state->stackDepth==0));
        stateInfo->flags      = state->flags;
        stateInfo->stackDepth = state->stackDepth;
        for (unsigned i=0; i < stateInfo->stackDepth; i++)
            stateInfo->stack[i] = state->stack[i];
        stateInfo->exceptionInfo = state->exceptionInfo;
        for (ExceptionInfo *except = stateInfo->exceptionInfo; except != NULL;
             except = except->getNextExceptionInfoAtOffset()) {
            if (except->isCatchBlock() && 
                except->getBeginOffset() <= offset && offset < except->getEndOffset()) {
                CatchBlock* block = (CatchBlock*)except;
                for (CatchHandler *handler = block->getHandlers(); handler != NULL;
                     handler = handler->getNextHandler()) {
                    int cstart = handler->getBeginOffset();
                    if (Log::isEnabled()) Log::out() << "SETCATCHINFO "<<(int)cstart<<" "<<(int)numVars<< ::std::endl;
                    prepass.pushCatchLabel(cstart);
                    setStateInfo(stateInfo,cstart,false,true);
                }
            }
        }
    }

    int getMaxStackOverflow() { return maxDepth; }
    void printState(StateInfo *state) {
        if (state == NULL) return;
        struct StateInfo::SlotInfo *stack = state->stack;
        for (unsigned i=0; i < state->stackDepth; i++) {
            Log::out() << "STACK " << i << ":";
            StateInfo::print(stack[i],Log::out());
            Log::out() << ::std::endl;
        }
    }
protected:
    virtual bool keyEquals(U_32 *key1,U_32 *key2) const {
        return key1 == key2;
    }
    virtual U_32 getKeyHashCode(U_32 *key) const {
        // return hash of address bits
        return ((U_32)(POINTER_SIZE_INT)key);
    }
private:
    MemoryManager& memManager;
    TypeManager& typeManager;
    JavaLabelPrepass& prepass;
    StlHashMap<U_32, StateInfo*> hashtable;
    unsigned maxDepth;
    unsigned numVars;
};

#endif // _JAVALABELPREPASS_H_


} //namespace Jitrino 
