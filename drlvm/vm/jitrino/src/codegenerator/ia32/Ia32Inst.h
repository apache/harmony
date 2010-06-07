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
 * @author Vyacheslav P. Shakin
 */

#ifndef _IA32_INST_H_
#define _IA32_INST_H_

#include "open/types.h"
#include "Stl.h"
#include "MemoryManager.h"
#include "Type.h"
#include "CodeGenIntfc.h"
#include "MemoryAttribute.h"
#include "ControlFlowGraph.h"

#include "Ia32Encoder.h"
#include "Ia32CallingConvention.h"
namespace Jitrino
{
namespace Ia32{

//=========================================================================================================
class CodeEmitter;
class IRManager;
class Opnd;
class Inst;
class BasicBlock;
class I8Lowerer;
class ConstantAreaItem;

//=========================================================================================================
//      class Opnd
//=========================================================================================================
/** 
class Opnd represents an operand in the LIR.

Instructions contain pointers to Opnd instances for their operands.
For example the following LIR pseudo-code:
    I0: mov t1, t0
    I1: add t1, t3

instructions I0 and I1 will contain pointers (at index 0) to the same Opnd instance with id==1

Physical locations where an operand can be placed is controlled
by an array of 3 constraints: 
        ConstraintKind_Initial,
        ConstraintKind_Calculated,
        ConstraintKind_Location,

When an operand is created it is assigned ConstraintKind_Initial for the rest of its live
At opnd creation time ConstraintKind_Calculated is also set to ConstraintKind_Initial
When the operand is assigned to a specific physical location (Imm, Reg, Mem), its Location constraint
is set appropriately (representing the assigned location). 

The Calculated constraint is calculated during special passes 
taking into account all instruction irregularities
The Initial constraint always contains the Calculated constraint and 
the Calculated constraint contains the Location one. 

*/

class Opnd: public CG_OpndHandle
{
public:

    /** enum DefScope defines properties of the set of definitions of an operand  */
    enum DefScope{
        DefScope_Null=0,
        /** The operand has single definition */
        DefScope_Temporary,
        /** The operand has multiple defs all within one basic block (occurs after conversion into 2-operand form) */
        DefScope_SemiTemporary,
        /** The operand has merging defs */
        DefScope_Variable,
    };

    /** enum ConstraintKind is used to indicate a particular constraint of an operand */
    enum ConstraintKind{
        /** An additional constraint assigned during Opnd creation */
        ConstraintKind_Initial=0,
        /** A constraint calculated in the constraint resolver from instruction properties */
        ConstraintKind_Calculated=1,
        /** A constraint defining assigned physical location of an operand */
        ConstraintKind_Location=2,
        /** The current constraint. If an operand is not assigned with physical storage,
        getConstraint will return the Calculated constraint, 
        otherwise, it will return the Location constraint
        */
        ConstraintKind_Current
    };

    enum MemOpndAlignment{
        MemOpndAlignment_Any    = 0,
        MemOpndAlignment_4      = 4,
        MemOpndAlignment_8      = 8,
        MemOpndAlignment_16     = 16
    };
    
    //-------------------------------------------------------------------------
    /** class RuntimeInfo contains information allowing CG to determine operand value from the current runtime information
    Initially added to support AOT compiler the class is used to annotate operands with runtime info
    */
    class RuntimeInfo
    {
    public:
        enum Kind{
            Kind_Null=0,
            /** The value of the operand is [0]->ObjectType::getAllocationHandle() */
            Kind_AllocationHandle,      
            /** The value of the operand is [0]->NamedType::getRuntimeIdentifier() */
            Kind_TypeRuntimeId,     
            /** The value of the operand is [0]->NamedType::getRuntimeIdentifier() */
            Kind_MethodRuntimeId,       
            /** The value of the operand is [1], but the information can be used to serialize/deserialize 
            this value: [0] - Type * - the containing class, [1] - string token */
            Kind_StringDescription, 
            /** The value of the operand is [0]->ObjectType::getObjectSize() */
            Kind_Size,                  
            /** The value of the operand is compilationInterface->getRuntimeHelperAddress([0]) */
            Kind_HelperAddress,
            /** The value of the operand is irManager.getInternalHelperInfo((const char*)[0]).pfn */
            Kind_InternalHelperAddress,
            /** The value of the operand is the address where the interned version of the string is stored*/
            Kind_StringAddress,
            /** The value of the operand is [0]->FieldDesc::getAddress() */
            Kind_StaticFieldAddress,
            /** The value of the operand is [0]->FieldDesc::getOffset() */
            Kind_FieldOffset,
            /** The value of the operand is compilationInterface.getVTableOffset(), zero args */
            Kind_VTableAddrOffset,
            /** The value of the operand is [0]->ObjectType::getVTable() */
            Kind_VTableConstantAddr,
            /** The value of the operand is [0]->MethodDesc::getOffset() */
            Kind_MethodVtableSlotOffset,
            /** The value of the operand is [0]->MethodDesc::getIndirectAddress() */
            Kind_MethodIndirectAddr,
            /** The value of the operand is *[0]->MethodDesc::getIndirectAddress() */
            Kind_MethodDirectAddr,
    
            /** The value of the operand is address of constant pool item  ((ConstantPoolItem*)[0])->getAddress() */
            Kind_ConstantAreaItem=0x80,

            /** The value of the operand is a pointer to the EM_ProfileAccessInterface */
            Kind_EM_ProfileAccessInterface,
            /** The value of the operand is Method_Profile_Handle for the value profile of the compiled method */
            Kind_Method_Value_Profile_Handle,

            /** more ... */
        };

        /** Constructs a RuntimeInfo instance of RuntimeInfo::Type t and initialize it with given values */
        RuntimeInfo(RuntimeInfo::Kind k, void * value0, void * value1=0, void * value2=0, void * value3=0, U_32 addOffset=0)
            :kind(k), additionalOffset(addOffset)
            { value[0]=value0; value[1]=value1; value[2]=value2; value[3]=value3; }

        /** Returns the the value at index i */
        void * getValue(U_32 i)const{ assert(i<sizeof(value)/sizeof(value[0])); return value[i]; }

        U_32 getAdditionalOffset()const{ return additionalOffset; }

        /** Returns the kind of the info */
        RuntimeInfo::Kind getKind()const { return kind; }
    private:
        RuntimeInfo::Kind kind;
        void * value[4];
        U_32 additionalOffset;
    };

public:

    /** Returns the ID of the operand */
    U_32      getId()const{ return id; }

    /** 
     * Returns the ID of the operand assigned at its creation.
     * The ID of an operand returned by getId maybe changed by IRManager::packOpnds.
     * The original ID returned by getFirstID is used in logging and IR dumps
     * for convenience.
    */
    U_32      getFirstId()const{ return firstId; }

    /** Returns the type of the operand */
    Type * getType()const{ return type; }
    void setType(Type* newType) {type = newType; }

    /** Returns the constraint of the specified kind sk */
    Constraint getConstraint(ConstraintKind ck) const
    {
        if (ck==ConstraintKind_Current)
            return constraints[ConstraintKind_Location].isNull()?constraints[ConstraintKind_Calculated]:constraints[ConstraintKind_Location];
        return constraints[ck];
    }

    /** 
     * Returns the constraint of the specified kind sk and adjusts the 
     * results to the specified size.
     */
    Constraint getConstraint(ConstraintKind ck, OpndSize size) const
    {   Constraint c=getConstraint(ck); return size==OpndSize_Any?c:c.getAliasConstraint(size); }

    /** Returns true if the operand CAN BE assigned to a location defined by constraint
    */
    bool canBePlacedIn(Constraint c)const
        { return !(getConstraint(ConstraintKind_Calculated, c.getSize())&c).isNull(); }
    inline bool canBePlacedIn(OpndKind opndKind)const
    { return ((U_32)opndKind & constraints[ConstraintKind_Calculated].getKind()) != 0; }

    /** Returns the physical register assigned to the operand */
    RegName getRegName()const{ return isPlacedIn(OpndKind_Reg)?regName:RegName_Null; }

    /** Returns the immediate value assigned to the operand */
    int64               getImmValue()const{ return isPlacedIn(OpndKind_Imm)?immValue:0; }

    /** Returns a sub-operand of a memory operand or NULL   */
    Opnd *  getMemOpndSubOpnd(MemOpndSubOpndKind so)const
    { assert(memOpndKind != MemOpndKind_Null); return memOpndSubOpnds[so]; }
    Opnd * const * getMemOpndSubOpnds()const { return memOpndSubOpnds; }

    /** Returns the memory kind of the operand 
    if it has been assigned to a memory location or Null otherwise */
    MemOpndKind getMemOpndKind()const{ return memOpndKind; }

    void setMemOpndKind(MemOpndKind k){ memOpndKind=k; }

    /**
     * Returns alignment for the operand. It makes sense to query
     * alignment for memory operands that have stack auto layout kind only.
     * For all other operands MemOpndAlignment_Any will be returned.
     */
    MemOpndAlignment getMemOpndAlignment() {
        return memOpndAlignment;
    }
    
    /**
     * Sets desirable memory operand alignment.
     */
    void setMemOpndAlignment(MemOpndAlignment alignment) {
        // It makes sense to specify alignment for memory operands
        // which have stack auto layout kind only.
        memOpndAlignment = alignment;
    }
    
    /** 
     * Returns true if the operand IS assigned to a location defined by constraint.
     * The constraint can be either explicitly created or implicitly created from RegName values.
    */
    inline bool isPlacedIn(Constraint c)const
    { Constraint cl=getConstraint(ConstraintKind_Location, c.getSize()); return !cl.isNull() && c.contains(cl); }

    /** 
     * Returns true if the operand IS assigned to a location defined by opndKind.
     * This is an "optimized" version of the isPlacedIn method for OpndKind args.
     */
    inline bool isPlacedIn(OpndKind opndKind)const
    { return ( (U_32)opndKind & constraints[ConstraintKind_Location].getKind() ) != 0; }

    /** Returns true if the operand IS assigned to any location. */
    bool hasAssignedPhysicalLocation()const
    { return !constraints[ConstraintKind_Location].isNull(); }

    /** Returns the size of the operand. */
    OpndSize getSize()const
    { OpndSize sz=constraints[ConstraintKind_Initial].getSize(); assert(sz!=OpndSize_Null && sz!=OpndSize_Any); return sz; }

    /** Returns true if the operand can be allocated on register kind described by constraint c */
    bool isAllocationCandidate(Constraint c)const
    { return canBePlacedIn((OpndKind)c.getKind()) && constraints[ConstraintKind_Location].isNull(); }

    /** Returns the RuntimeInfo associated with the operand or NULL */
    RuntimeInfo * getRuntimeInfo()const 
    { return runtimeInfo; }

    /** Associates a RuntimeInfo with the operand */
    void setRuntimeInfo(RuntimeInfo * ri)
    { assert(isPlacedIn(OpndKind_Imm)); runtimeInfo=ri; }

    /** Assigns immediate value to the operand */
    void assignImmValue(int64 v);

    /** Assigns physical register to the operand */
    void assignRegName(RegName r);

    /** Assigns a memory location to the operand */
    void assignMemLocation(MemOpndKind k, Opnd * _base, Opnd * _index=0, Opnd * _scale=0, Opnd * _displacement=0);

    /** Changes sub-operands of a memory opnd (should be already assigned to memory location) */ 
    void setMemOpndSubOpnd(MemOpndSubOpndKind so, Opnd * opnd);

    /** Returns sub-operand constraint for the sub-operand defined by so
    according to the Current operand constraints
    */
    Constraint getMemOpndSubOpndConstraint(MemOpndSubOpndKind so)
    { return Encoder::getMemOpndSubOpndConstraint(Constraint(), so); }

    /** Returns the definition scope of the operand */
    DefScope getDefScope()const{ return defScope; }
    
    /** 
     * Returns the number of occurrences of the operand in LIR.
     * The value may take into account basic block execution count (profile information).
     * Non-zero result means the operand is used in LIR and zero means it is not.
     * 
     * The refCount value is calculated during IRManager::calculateOpndStatistics.
     */
    U_32 getRefCount()const{ return refCount; }

    /** Assigns the Calculated constrain to the operand. */
    void setCalculatedConstraint(Constraint c);

    /** 
     * Return the defining inst for operands with a single definition. 
     * If the operand has multiple definitions the method returns 0.
     *
     * The definingInst value is normally calculated during IRManager::calculateOpndStatistics.
     */
    Inst * getDefiningInst()const{ return definingInst; }

    /** 
     * Assigns the defining inst for operands with a single definition. 
     */
    void setDefiningInst(Inst * inst);

    /** 
     * Returns true if the operand is used in liveness analysis.
     * For example, immediate values and heap operands do not participate
     * in liveness analysis.
     */
    bool isSubjectForLivenessAnalysis()const
    {
        return (memOpndKind&(MemOpndKind_StackManualLayout|MemOpndKind_ConstantArea|MemOpndKind_Heap|MemOpndKind_LEA))==0 && !isPlacedIn(OpndKind_Imm);
    }

    /** Returns the segment register used with the operand (memory). */
        RegName getSegReg() const     { return segReg; }
    
    /** Assigns the segment register to be used with the operand (memory). */
        void    setSegReg(RegName sr) { segReg = sr; }

protected:
    bool replaceMemOpndSubOpnd(Opnd * opndOld, Opnd * opndNew);
    bool replaceMemOpndSubOpnds(Opnd * const * opndMap);
    /**
     * 'Normalizes' memory sub opnds. That is ensures that an immediate is 
     * placed at the displacement, and a register gets placed ad the base.
     */
    void normalizeMemSubOpnds(void);

    void addRefCount(U_32& index, U_32 blockExecCount);

#ifdef _DEBUG
    void checkConstraints();
#else
    void checkConstraints(){}
#endif
private:

    //-------------------------------------------------------------------------
    Opnd(U_32 _id, Type * t, Constraint c)
        :id(_id), firstId(_id), type(t), 
        defScope(DefScope_Null), definingInst(NULL), refCount(0),
        segReg(RegName_Null), memOpndKind(MemOpndKind_Null),
        memOpndAlignment(MemOpndAlignment_Any),
        immValue(0), runtimeInfo(NULL) {
            constraints[ConstraintKind_Initial]=constraints[ConstraintKind_Calculated]=c;
        }

    //-------------------------------------------------------------------------
    U_32          id;
    U_32          firstId;
    Type     *      type;
    Constraint      constraints[ConstraintKind_Current];

    DefScope        defScope;
    Inst *          definingInst;
    U_32          refCount;
    RegName         segReg;

    MemOpndKind         memOpndKind;
    // Defines alignment for memory oprands that have stack auto layout kind.
    MemOpndAlignment    memOpndAlignment;

    union{
        RegName     regName;
        struct{
            int64           immValue;
            RuntimeInfo *   runtimeInfo;
        };
        Opnd *      memOpndSubOpnds[MemOpndSubOpndKind_Count];
    };

    //-------------------------------------------------------------------------
    friend class IRManager;
    friend class Inst;
};

typedef Opnd *      POpnd;
typedef StlVector<Opnd*> OpndVector;

//=========================================================================================================
//   class Inst
//=========================================================================================================
/**

class Inst represents an instruction of the LIR.

Each instruction contains an array of all its operands (pointers to Opnd instances).

Each instruction contains a pointer to the basic block it is attached to.

Each instruction has an ID unique in the method it belongs to.

Each instruction can be assigned a sequential index using IRManager::indexInsts() to order instruction
in a particular order.

Inst provides Opnds collection allowing it to iterate over all the operands the instruction 
uses or defines explicitly or implicitly.

Inst also provides an interface to instruction-level operand constraints defined by ISA. 
*/

class Inst: public CFGInst 
{

public:

    //---------------------------------------------------------------
    /** enum Kind represents dynamic type info of Inst and descendants.
    This enumeration is hierarchical and is used in getKind and hasKind Inst methods.
    */
    enum Kind
    {
        Kind_Inst = 0x7fffffff,
        Kind_PseudoInst                         = 0x7ff00000,
            Kind_MethodEntryPseudoInst          = 0x00100000,
            Kind_MethodEndPseudoInst            = 0x00200000,
            Kind_EmptyPseudoInst                = 0x00400000,
            Kind_CMPXCHG8BPseudoInst            = 0x00800000,
            Kind_CopyPseudoInst                 = 0x01000000,
            Kind_I8PseudoInst                   = 0x02000000,
            Kind_GCInfoPseudoInst               = 0x04000000,
            Kind_SystemExceptionCheckPseudoInst = 0x08000000,
            Kind_CatchPseudoInst                = 0x10000000,
            Kind_AliasPseudoInst                = 0x20000000,
            Kind_EntryPointPseudoInst           = 0x40000000,
        Kind_ControlTransferInst                    = 0x0000fff0,
            Kind_LocalControlTransferInst           = 0x000003F0,
            Kind_JmpInst                            = 0x00000200,
            Kind_BranchInst                         = 0x000001C0,
            Kind_SwitchInst                         = 0x00000030,
            Kind_InterProceduralControlTransferInst = 0x0000fc00,
                Kind_CallInst                       = 0x0000f000,
                Kind_RetInst                        = 0x00000c00,
    };

    /** Misc properties of an instruction */
    enum Properties{
        /** The operation of the instruction is commutative regarding its uses */
        Properties_Symmetric=0x1,
        /** The instruction is conditional (e.g. SETCC) */
        Properties_Conditional=0x2,
        Properties_PureDef=0x4,
        /** 
         * Memory operands of the instruction are conditional.
         * This means different operands of the instructions can be assigned to memory
         * but only one of them at the same time. This is a wide-known constraint of IA32 ISA.
         */
        Properties_MemoryOpndConditional=0x8,
    };

    
    /** 
     * Enum OpndRole defines the role of an operand in an instruction.
     * The structure of the enumeration is filter-like allowing to combine its values with '|'.
     * 
     * It is important to notice that the role mask consists of two parts
     * covered by OpndRole_FromEncoder and OpndRole_ForIterator.
     * When used as filter values from each part are virtually combined by "and"
     * meaning that an operand must satisfy filters from both parts.
     * 
     * For example OpndRole_Use|OpndRole_Def|OpndRole_Explicit
     * will filter explicit operand which are both uses and defs while 
     * OpndRole_Use|OpndRole_Explicit will filter only explicit operands which are uses.
    */
    enum OpndRole
    {
        OpndRole_Null=0,
        /** Instruction uses this operand. */
        OpndRole_Use=0x1,
        /** Instruction defines this operand. */
        OpndRole_Def=0x2,
        /** Both uses and defs. */
        OpndRole_UseDef=OpndRole_Use|OpndRole_Def,

        /** Roles retrieved from Encoder. */
        OpndRole_FromEncoder=OpndRole_UseDef,

        /** 
         * Explicit operand, defined by ISA, must be explicitly provided during inst creation.
        */
        OpndRole_Explicit=0x10,

        /** 
         * Auxilary operand, not defined by ISA, 
         * e.g. return values, arguments of a call instruction, and so on.
        */ 
        OpndRole_Auxilary=0x20,

        /** 
          * Implicit operand, defined by ISA, must not be explicitly provided during inst creation.
          * These operands are assigned with a physical location from the moment of creation,
          * cannot be replaced in instructions, and fully defined by the semantics of an instruction.
          * Examples are: EFLAG.
          * 
          * ESP could also be implicit operands of PUSH/POP 
          * but this is not done in the current implementation.
          * 
        */
        OpndRole_Implicit=0x40,

        /** 
         * Operands contained directly in the instruction 
         * (as oppsed to sub-operands of memory InstLevel operands).
        */
        OpndRole_InstLevel=OpndRole_Explicit|OpndRole_Auxilary|OpndRole_Implicit,

        /** Sub-operand of an instruction memory operand (base, index, scale, displacement). */
        OpndRole_MemOpndSubOpnd=0x80,
        /** Sub-operands of the InstLevel operands. */
        OpndRole_OpndLevel=OpndRole_MemOpndSubOpnd,

        /** Operands which can be set or replaced directly in an instruction. */
        OpndRole_Changeable=OpndRole_Explicit|OpndRole_Auxilary|OpndRole_MemOpndSubOpnd,

        OpndRole_ForIterator=OpndRole_InstLevel|OpndRole_OpndLevel,

        OpndRole_All=0xff,
        OpndRole_AllDefs=OpndRole_ForIterator|OpndRole_Def,
        OpndRole_AllUses=OpndRole_ForIterator|OpndRole_Use,
    };

    /** enum Form represents the form of an instruction 
    Inst can be in either Extended ("3-address") form or Native ("2-address") form.
    */
    enum Form
    {
        /** Instructions's operands are in the "2-address" native form, e.g. add t0, t1. */
        Form_Native,
        /** Instructions's operands are in the "3-address" extended form, e.g. t2=add t0, t1. */
        Form_Extended,
    };

    //---------------------------------------------------------------
public:

    /** Returns the next inst in a double-linked list. */
    Inst* getNextInst() const {return (Inst*)next();}
    /** Returns the previous inst in a double-linked list. */
    Inst* getPrevInst() const {return (Inst*)prev();}

    /** Returns the kind of the instruction representing its class. */
    Kind getKind()const{ return kind; }
    /** Returns true if the instruction is of kind (class) k or its subclass. */
    bool hasKind(Kind k)const{ return (kind&k)==kind; }

    /** Returns the id of the instruction. */
    U_32      getId()const{ return id; }

    /** Returns the current form of the instruction. */
    Form        getForm()const{ return (Form)form; }

    /** 
     * Returns the stack depth at the instruction.
     * This information is calculated in IRManager::calculateStackDepth().
     */
    U_32      getStackDepth() const { return stackDepth; }

    /** Returns the mnemonic of the instruction. */
    Mnemonic    getMnemonic()const{ return mnemonic; }

    /** Returns the prefix of the instruction. */
    InstPrefix getPrefix()const{ return prefix; }

    /** Sets the prefix of the instruction. */
    void setPrefix(InstPrefix newPrefix){prefix = newPrefix;}

    /** Returns opcode group description associated with this instruction. */
    const Encoder::OpcodeGroup* getOpcodeGroup()const
    { assert(opcodeGroup); return opcodeGroup; }


    /** Shortcut: returns the properties of the instruction (bit-mask of Properties). */
    U_32 getProperties()const
    { return properties; }

    bool getPureDefProperty() const;

    /** Returns the sequential index of the instruction after ordering via IRManager::indexInsts. */
    U_32      getIndex()const{ return index; }

    /** 
     * Returns the number of InstLevel operands in the instruction. 
     * This is an optimized version of getOpndCount(OpndRole_InstLevel|OpndRole_UseDef).
    */
    U_32      getOpndCount()const{ return opndCount; }

    /** 
     * Returns the number of operands in the instruction satisfying the given mask. 
     * Please refer to documentation of enum OpndRole for description of usage of opnd roles as filters. 
     */
    U_32      getOpndCount(U_32 roles)const
    { return 
        roles == (OpndRole_InstLevel|OpndRole_UseDef) ? opndCount:
        roles == (OpndRole_InstLevel|OpndRole_Def) ? defOpndCount:
        roles == (OpndRole_InstLevel|OpndRole_Use) && (Form)form == Form_Extended ? opndCount - defOpndCount:
        countOpnds(roles);
    }

    /** 
     * Returns the operand at the given index. 
     * The indexing space is sparce and common for all operands including both InstLevel and OpndLevel.
     * 
     * The indexing space is organized as follows:
     * - All InstLevel operands
     * - OpndLevel operands for each InstLevel operand, 
     *   organized in 4 slots for each InstLevel operand. 
     *   If an InstLevel operand is not a memory operand the 
     *   corresponding OpndLevel slots will contain undefined value.
     * For example, for MOV op0, op1 ([op10(base)+op11(disp)]) the virtual array of operands will appear as
     * { op0, op1, ?, ?, ?, ?, op10, ?, ?, op11 }.
     *
     * All defs in an instruction go before uses in the indexing space.
     *
     * Simple loops from 0 to getOpndCount() can be used to access InstLevel operands only.
     * Inst::Opnds::iterator can also be used as index in this method and should be used to iterate
     * over all operands including OpndLevel.
    */
    Opnd * getOpnd(U_32 index)const
    {   
        Opnd * const * opnds = getOpnds();
        if (index < opndCount)
            return opnds[index];
        return opnds[(index - opndCount) / 4]->getMemOpndSubOpnd((MemOpndSubOpndKind)((index - opndCount) & 3));
    }

    /** 
     * Returns a mask describing operand roles (|-ed from OpndRole values).
     *
     * The indexing space is common for all operands including OpndLevel
     * (please see comments to Opnd * getOpnd(U_32 index)const). 
    */
    U_32  getOpndRoles(U_32 index) const
    { return index < opndCount ? getOpndRoles()[index] : OpndRole_OpndLevel|OpndRole_Use;   }

    /** Returns a const array of all InstLevel operands. */
    Opnd * const * getOpnds()const              { return opnds; }

    /** Returns a const array of InstLevel operand roles. */
    const U_32 * getOpndRoles()const          
    { U_32 aoc = allocatedOpndCount; return (const U_32*)(opnds + aoc); }

    /** Returns a const array of InstLevel operand constraints. */
    const Constraint * getConstraints()const    
    { U_32 aoc = allocatedOpndCount; return (const Constraint*)((const U_32*)(opnds + aoc) + aoc); }

    /** 
     * Returns the constraint imposed by the instruction for the operand at idx.
     * 
     * The indexing space is common for all operands including OpndLevel
     * (please see comments to Opnd * getOpnd(U_32 index)const). 
     * 
     * @param idx - operand index to get the constraint for.
     * 
     * @param memOpndMask - the mask of the operands which are checked if 
     * they are assigned to memory when determining conditional constraints. 
     * memOpndMask is indexed in the standard operand indexing space, the same as idx.
     * The method takes into account only those memOpndMask bits which correspond to 
     * Explicit operands and not the operand defined by idx.
     * 
     * @param size - if this parameter is not OpndSize_Null the resulted constraint 
     * is adjusted to the requested size.
    */
    Constraint getConstraint(U_32 idx, U_32 memOpndMask, OpndSize size = OpndSize_Null) const;

    /** 
     * Returns true if the position at idx starts or extends the operand live range 
     * (simplistically its a use of the operand).
    */
    bool isLiveRangeStart(U_32 idx)const
    { return (getOpndRoles(idx) & Inst::OpndRole_Use) != 0 && getOpnd(idx)->isSubjectForLivenessAnalysis() && !getPureDefProperty(); }


    /** 
     * Returns true if the position at idx ends the operand live range 
     * (simplistically its a pure def of the operand).
     */
    bool isLiveRangeEnd(U_32 idx)const
    { return ((getOpndRoles(idx) & Inst::OpndRole_UseDef) == Inst::OpndRole_Def && (getProperties() & Inst::Properties_Conditional)==0) || ((getOpndRoles(idx) & Inst::OpndRole_Def)!=0 && getPureDefProperty()); }


    /** 
     * Sets opnd in the instruction at idx.
     * 
     * The indexing space is common for all operands including OpndLevel
     * (please see comments to Opnd * getOpnd(U_32 index)const). 
    */
    void setOpnd(U_32 idx, Opnd * opnd);


    /** 
     * Inserts opnd into the instruction at idx.
     * 
     * Works only for InstLevel operands. The total number of operands cannot 
     * exceed pre-allocated capacity.
     */
    void insertOpnd(U_32 idx, Opnd * opnd, U_32 opndRoles);

    /** Replaces all occurences of opndOld with roles matching opndRoleMask to opndNew */
    bool replaceOpnd(Opnd * opndOld, Opnd * opndNew, U_32 opndRoleMask=OpndRole_All);

    /** 
     * Replaces all occurences of operands with roles matching opndRoleMask
     * which has an entry in opndMap to the operand from that entry.
     *
     * The opndMap map is organized as an array indexed by from-operand ID which contains to-operands.
     * The number of entries in the array must be no less than the value returned by IRManager::getOpndCount()
    */
    bool replaceOpnds(Opnd * const * opndMap, U_32 opndRoleMask=OpndRole_All);

    /** Returns true if the instruction has side effect not described by its operands */
    virtual bool hasSideEffect()const
    { 
        Mnemonic m = getMnemonic();
        if(m==Mnemonic_MOVS8  ||
           m==Mnemonic_MOVS16 ||
           m==Mnemonic_MOVS32 ||
           m==Mnemonic_MOVS64 ||
           m==Mnemonic_STOS   ||
           m==Mnemonic_STD    ||
           m==Mnemonic_CLD    ||
           m==Mnemonic_POPFD  ||
           m==Mnemonic_PUSHFD ||
           m==Mnemonic_POP    ||
           m==Mnemonic_PUSH   ||
           m==Mnemonic_FSTP   ||
           m==Mnemonic_FST    ||
           m==Mnemonic_FIST   ||
           m==Mnemonic_FLD    ||
           m==Mnemonic_FILD   || 
           m==Mnemonic_FLDLN2 ||
           m==Mnemonic_FLDLG2 ||
           m==Mnemonic_FLD1   )
        {
            return true;
        }
        return false;
    }

    /* Checks that inst is valid*/
    virtual void verify() const { assert(node!=NULL);}

    /** Emits (encodes) the instruction into stream */
    U_8 * emit(U_8* stream);

    void initFindInfo(Encoder::FindInfo& fi, Opnd::ConstraintKind opndConstraintKind)const;

    /** Shortcut to get the next instruction in an Inst list */
    Inst * getNext()const{ return (Inst*)_next; }
    /** Shortcut to get the prev instruction in an Inst list */
    Inst * getPrev()const{ return (Inst*)_prev; }
    
    /** Swaps inst's operands at idx0 and idx1  */
    void swapOperands(U_32 idx0, U_32 idx1);

    /** 
     * Changes instruction form to native
     * and makes all necessary changes in instruction operands
    */  
    virtual void makeNative(IRManager * irManager);

    /** 
     * Changes condition for a conditional instruction (SETcc, MOVcc, Jcc)
     * Conditional instructions have Properies_Conditional.
    */
    void changeInstCondition(ConditionMnemonic cc, IRManager * irManager);

    /* Reverses condition of a conditional inst and updates its opcode group appropriately. */
    virtual void reverse(IRManager * irManager);

    /* Returns true if the condition of a conditional inst can be reversed. */
    virtual bool canReverse()const
    { return getProperties()&&Properties_Conditional; }

    /** Sets the offset of native code for this instruction. */
    void            setCodeOffset(U_32 offset) {codeOffset = offset;}
    /** Returns the offset of native code for this instruction. */
    U_32          getCodeOffset()const    {   return codeOffset;  }
    /** Returns the size of native code for this instruction. */
    U_32          getCodeSize()const  {   return codeSize;    }

    /** Returns the pointer to the native code for this instruction. */
    void *          getCodeStartAddr()const;

    /** Returns the basic block this inst is inserted into or null. */
    BasicBlock* getBasicBlock() const
    {
        assert(node == NULL || node->isBlockNode());
        return (BasicBlock*)node;
    }

    /** 
     * Returns the kind of edge according to the kind of the inst.
     * Called by CFG to detect BB->BB block edges.
     * ControlTransferInst descendants override this method.
     */
    virtual Edge::Kind getEdgeKind(const Edge* edge) const;

    class Opnds;
protected:
    //---------------------------------------------------------------

    static void* operator new(size_t sz, MemoryManager& mm, U_32 opndCount); 

    static inline void operator delete(void * p, MemoryManager& mm, U_32 opndCount) {}
    static inline void operator delete(void * p) {}

    Inst(Mnemonic m, U_32 _id, Form f)
        :kind(Kind_Inst), id(_id), mnemonic(m), prefix(InstPrefix_Null),
        form(f), reservedFlags(0), codeSize(0), properties(0), reservedFlags2(0),
        opcodeGroup(0), index(0), codeOffset(0), 
        /*allocatedOpndCount(0), */defOpndCount(0), opndCount(0), stackDepth(0)/*, opnds(0)*/ 
        
        // WARN! commented opnds are assigned in overloaded 'new' operator before the constructor
        // : void* Inst::operator new(size_t sz, MemoryManager& mm, U_32 opndCount)
    {}
    virtual ~Inst(){};

    static U_32 getOpndChunkSize(U_32 opndCount){ return opndCount * (sizeof(Opnd*) + sizeof(U_32) * 2); }

    /** sets the size of native code for this instruction */
    void            setCodeSize(U_32 size) {codeSize = size;}

    Opnd ** getOpnds()              { return opnds; }
    U_32 * getOpndRoles()         
    { U_32 aoc = allocatedOpndCount; return (U_32*)(opnds + aoc); }
    Constraint * getConstraints()   
    { U_32 aoc = allocatedOpndCount; return (Constraint*)((const U_32*)(opnds + aoc) + aoc); }
    static U_32 getExplicitOpndIndexFromOpndRoles(U_32 roles)
    { return roles>>16; }


    U_32 countOpnds(U_32 roles)const;

    void setConstraint(U_32 idx, Constraint c)
    { assert( (getOpndRoles()[idx] & OpndRole_Explicit) == 0 ); getConstraints()[idx] = c; }

    void fixOpndsForOpcodeGroup(IRManager * irManager);
    void assignOpcodeGroup(IRManager * irManager);

    void setStackDepth(const U_32 sd) { stackDepth = sd; }


    //---------------------------------------------------------------
    Kind                                        kind;
    U_32                                      id;
    Mnemonic                                    mnemonic;
    InstPrefix                                  prefix;

    U_32                                      form:1;
    U_32                                      reservedFlags:7;
    U_32                                      codeSize:8;
    U_32                                      properties:8;
    U_32                                      reservedFlags2:8;

    Encoder::OpcodeGroup                *   opcodeGroup;

    U_32                                      index;
    U_32                                      codeOffset;

    U_32                                      allocatedOpndCount:16;
    U_32                                      defOpndCount:16;
    U_32                                      opndCount:16;
    U_32                                      stackDepth:16;

    Opnd **                                     opnds;

    //---------------------------------------------------------------
    friend class    IRManager;
    friend class    Encoder;
    friend class    CallingConventionClient;
    friend class    I8Lowerer;
};


//=========================================================================================================
//   Inst virtual operand collection for iteration
//=========================================================================================================

class Inst::Opnds
{
public:
    typedef U_32 iterator;
    inline Opnds(const Inst * inst, U_32 r)
    {
        rolesToCheck = 0;
        roles = NULL;

        opnds = inst->getOpnds();

        if (r & Inst::OpndRole_InstLevel) {
            startIndex = 0;
            if (r & Inst::OpndRole_Use){
                endIndex = instEndIndex = inst->opndCount; 
                if (r & Inst::OpndRole_OpndLevel)
                    endIndex += endIndex<<2;
            }else if (r & Inst::OpndRole_Def) 
                endIndex = instEndIndex = inst->defOpndCount;
            else
                endIndex = instEndIndex = 0;
        
            if ((r & Inst::OpndRole_InstLevel) != Inst::OpndRole_InstLevel ||
                (r & Inst::OpndRole_UseDef) == Inst::OpndRole_Use )
            {
                roles = inst->getOpndRoles();
                rolesToCheck = r;
                startIndex = next(startIndex - 1);
            }
        }else{
            instEndIndex = inst->opndCount;
            endIndex = instEndIndex + (instEndIndex<<2);
            startIndex = skipNulls(instEndIndex);
        }
    }

    inline iterator begin()const{   return startIndex;  }
    inline iterator end()const{ return endIndex;    }

    inline iterator next(iterator index)const
    {   
        ++index; 
        if (index < instEndIndex){
            if (roles == NULL)
                return index;
            U_32 r = rolesToCheck;
            do { 
                U_32 ri = roles[index] & r;
                if ( (ri & Inst::OpndRole_ForIterator) && (ri & Inst::OpndRole_FromEncoder) ) return index; 
            }while (++index < instEndIndex);
        }
        return skipNulls(index);
    }

    U_32 skipNulls(iterator index)const
    {
        while (index < endIndex){
            U_32 diffIndex = index - instEndIndex;
            Opnd * instOpnd = opnds[diffIndex / 4];
            U_32 subIndex = diffIndex & 3;
            if (subIndex == 0 && instOpnd->getMemOpndKind()==MemOpndKind_Null)
                index += 4;
            else if (instOpnd->getMemOpndSubOpnd((MemOpndSubOpndKind)(subIndex))==NULL) index++;
            else break;
        }
        return index;
    }

    U_32 fill( Opnd ** opnds )const;

    Opnd * getOpnd(iterator index)const
    { 
        if (index < instEndIndex)
            return opnds[index];
        else {
            return opnds[(index - instEndIndex) / 4]->getMemOpndSubOpnd((MemOpndSubOpndKind)((index - instEndIndex) & 3));
        }
    }

    U_32 startIndex, endIndex, instEndIndex;
    Opnd * const * opnds; const U_32 * roles;
    U_32 rolesToCheck;

};


//=========================================================================================================
//   class CMPXCHG8BPseudoInst
//=========================================================================================================
/**
    Class CMPXCHG8BPseudoInst represents ...
*/
class CMPXCHG8BPseudoInst: public Inst
{
protected:
    CMPXCHG8BPseudoInst(int id)  
        : Inst(Mnemonic_NULL, id, Inst::Form_Extended)
    {kind=Kind_CMPXCHG8BPseudoInst;}

    friend class    IRManager;
};

//=========================================================================================================
//   class AliasPseudoInst
//=========================================================================================================
/**
    Class AliasPseudoInst represents ...
*/
class AliasPseudoInst: public Inst
{
protected:
    AliasPseudoInst(int id)  
        : Inst(Mnemonic_NULL, id, Inst::Form_Extended), offset(EmptyUint32)
    {kind=Kind_AliasPseudoInst;}

    U_32 offset;

    friend class    IRManager;
};

//=========================================================================================================
//   class CatchPseudoInst
//=========================================================================================================
/**
    Class CatchPseudoInst represents ...
*/
class CatchPseudoInst: public Inst
{
protected:
    CatchPseudoInst(int id)  
        : Inst(Mnemonic_NULL, id, Inst::Form_Extended)
    {kind=Kind_CatchPseudoInst;}

    virtual bool hasSideEffect()const{ return true; }
    virtual bool isHeaderCriticalInst() const {return true;}
    friend class    IRManager;
};

//=========================================================================================================
//   class GCInfoPseudoInst
//=========================================================================================================
/**
Class GCInfoPseudoInst adds uses of managed pointers bases to CFG
All opnds of this inst are bases that must be live in a place in CFG this inst is located.
staticMPtrs offsets contains resolved static offsets of managed pointers
*/

class GCInfoPseudoInst: public Inst {
    friend class    IRManager;

protected:
    GCInfoPseudoInst(IRManager * irm, int id);
    virtual bool hasSideEffect()const{ return true; }

public:   
    StlVector<I_32> offsets;
    const char* desc;
};

//=========================================================================================================
//   class SystemExceptionCheckInst
//=========================================================================================================

class SystemExceptionCheckPseudoInst: public Inst
{
public:
    CompilationInterface::SystemExceptionId getExceptionId()const{ return exceptionId; }
    bool checksThisOfInlinedMethod() const { return checksThis; }
protected:
    SystemExceptionCheckPseudoInst(CompilationInterface::SystemExceptionId eid, int id, bool chkThis)
        : Inst(Mnemonic_CALL, id, Inst::Form_Extended), exceptionId(eid), checksThis(chkThis)
    {kind=Kind_SystemExceptionCheckPseudoInst;}

    CompilationInterface::SystemExceptionId exceptionId;

    virtual bool hasSideEffect()const{ return false; }
    bool checksThis;
    friend class    IRManager;
};


//=========================================================================================================
//   class ControlTransferInst
//=========================================================================================================
/** class ControlTransferInst is a base class for all intructions which trasfers control:
branches, calls, rets
*/
class ControlTransferInst: public Inst
{

public:
    /** Sub-type: returns true if the instruction is a direct ConstrolTransferInst instance 
    (direct branch or direct call), which means that its operand is immediate */
    virtual bool isDirect()const
    { return getOpndCount()>0 && getOpnd(getTargetOpndIndex())->isPlacedIn(OpndKind_Imm); }

    U_32 getTargetOpndIndex()const{ return getOpndCount(OpndRole_InstLevel|OpndRole_Def); }

    virtual bool hasSideEffect()const { return true; }
protected:
    ControlTransferInst(Mnemonic mnemonic,  int id)  
        : Inst(mnemonic, id, Form_Native){kind=Kind_ControlTransferInst;}

    friend class    IRManager;
};

//=========================================================================================================
//   class BranchInst
//=========================================================================================================
/** class BranchInst is used for all branching (conditional control transfers) instructions 
*/
class BranchInst: public ControlTransferInst
{
    friend class    IRManager;
public:

    /** Returns the basic block this branch transfers control to*/
    Node* getTrueTarget() const {return trueTarget;}
    void  setTrueTarget(Node* node) {trueTarget = node;}
    Node* getFalseTarget() const {return falseTarget;}
    void  setFalseTarget(Node* node) { falseTarget = node;}

    /* Reverses direct branch condition and updates target&fallthrough edges
       WARN: does not affect layout.
     */
    void reverse(IRManager * irManager);

    /* Returns true if the direct branch can be reverted, i.e. it is direct and is conditional */
    bool canReverse()const;
    
    virtual void verify() const;

protected:
    BranchInst(Mnemonic mnemonic,  int id)  
        : ControlTransferInst(mnemonic, id) ,
        trueTarget(NULL), falseTarget(NULL)
    {
        kind=Kind_BranchInst; 
    }
    // called by CFG to detect BB->BB block edges
    virtual Edge::Kind getEdgeKind(const Edge* edge) const;

    // called from CFG when edge target is replaced
    virtual void updateControlTransferInst(Node* oldTarget, Node* newTarget);
    
    // called from CFG when 2 blocks are merging and one of  the branches is redundant.
    virtual void removeRedundantBranch();

    Node* trueTarget;
    Node* falseTarget;
};

//=========================================================================================================
//   class JumpInst
//=========================================================================================================
/** class JumpInst is used for unconditional jumps
*/
class JumpInst: public ControlTransferInst {
    friend class IRManager;
protected:
    JumpInst(int id) : ControlTransferInst(Mnemonic_JMP, id) { kind = Kind_JmpInst;}
    virtual void verify() const;
};

//=========================================================================================================
//   class SwitchInst
//=========================================================================================================
/** class SwitchInst is used for unconditional indirect jumps that use table mapping for targets
*/
class SwitchInst : public ControlTransferInst
{
    friend class    IRManager;
public:

    /** Returns the basic block for index i */
    U_32 getNumTargets() const;
    Node* getTarget(U_32 i)const;
    /** Sets the basic block for index i */
    void setTarget(U_32 i, Node* bb);

    Opnd * getTableAddress() const;
    ConstantAreaItem * getConstantAreaItem() const;
    
    virtual void verify() const;
protected:
    SwitchInst(Mnemonic mnemonic,  int id, Opnd * addr = 0) : 
            ControlTransferInst(mnemonic, id), tableAddr(addr)
        {
            kind=Kind_SwitchInst; 
            tableAddrRI = addr->getRuntimeInfo();
            assert(tableAddrRI->getKind()==Opnd::RuntimeInfo::Kind_ConstantAreaItem);
        }


    // called by CFG to detect BB->BB block edges
    virtual Edge::Kind getEdgeKind(const Edge* edge) const;
    // called from CFG when edge target is replaced
    virtual void updateControlTransferInst(Node* oldTarget, Node* newTarget);
    // called from CFG when 2 blocks are merging and one of  the branches is redundant.
    virtual void removeRedundantBranch();

    void replaceTarget(Node* bbFrom, Node* bbTo);

private:
    Opnd * tableAddr;
    //keep original opnd runtime info.
    //if orig opnd is spilled by spillgen and it's replacement has no runtime info
    Opnd::RuntimeInfo* tableAddrRI;
    
};

//=========================================================================================================
//   class CallingConventionClient
//=========================================================================================================
class CallingConventionClient
{
public:
    struct StackOpndInfo
    {
        U_32  opndIndex;
        U_32  offset;
        bool operator<(const StackOpndInfo& r)const{ return offset < r.offset; }
    };

    CallingConventionClient(MemoryManager& mm, const CallingConvention * cc)
        :callingConvention(cc), defInfos(mm), useInfos(mm), defStackOpndInfos(mm), useStackOpndInfos(mm), defArgStackDepth(0), useArgStackDepth(0){}

    const StlVector<CallingConvention::OpndInfo> & getInfos(Inst::OpndRole role)const
    { return (role & Inst::OpndRole_UseDef)==Inst::OpndRole_Def?defInfos:useInfos; }
    const StlVector<StackOpndInfo> & getStackOpndInfos(Inst::OpndRole role)const
    { return (role & Inst::OpndRole_UseDef)==Inst::OpndRole_Def?defStackOpndInfos:useStackOpndInfos; }

    void pushInfo(Inst::OpndRole role, Type::Tag typeTag)
    {
        CallingConvention::OpndInfo info;
        info.typeTag=(U_32)typeTag; info.slotCount=0;
        StlVector<CallingConvention::OpndInfo> & infos = getInfos(role);
        infos.push_back(info);
    }

    void finalizeInfos(Inst::OpndRole role, CallingConvention::ArgKind argKind);
    void layoutAuxilaryOpnds(Inst::OpndRole role, OpndKind kindForStackArgs);

    const CallingConvention *   getCallingConvention()const
    { assert(callingConvention!=NULL); return callingConvention; }

    U_32 getArgStackDepth(Inst::OpndRole role)const
    { return ((role & Inst::OpndRole_UseDef) == Inst::OpndRole_Def) ? defArgStackDepth : useArgStackDepth; }

    U_32 getArgStackDepthAlignment(Inst::OpndRole role) const
    { return ((role & Inst::OpndRole_UseDef) == Inst::OpndRole_Def) ? useArgStackDepthAlignment : useArgStackDepthAlignment; }

    void setOwnerInst(Inst * oi){ ownerInst = oi; }
protected:
    StlVector<CallingConvention::OpndInfo> & getInfos(Inst::OpndRole role)
    { return (role & Inst::OpndRole_UseDef)==Inst::OpndRole_Def?defInfos:useInfos; }
    StlVector<StackOpndInfo> & getStackOpndInfos(Inst::OpndRole role)
    { return (role & Inst::OpndRole_UseDef)==Inst::OpndRole_Def?defStackOpndInfos:useStackOpndInfos; }

    const CallingConvention *   callingConvention;

    Inst *                                      ownerInst;

    StlVector<CallingConvention::OpndInfo>      defInfos;
    StlVector<CallingConvention::OpndInfo>      useInfos;
    StlVector<StackOpndInfo>                    defStackOpndInfos;
    StlVector<StackOpndInfo>                    useStackOpndInfos;
    
    U_32 defArgStackDepth, useArgStackDepth;
    U_32 defArgStackDepthAlignment, useArgStackDepthAlignment;

};

//=========================================================================================================
//   class EntryPointPseudoInst
//=========================================================================================================
/**
    Class EntryPointPseudoInst represents an entry point for an instruction 
    and is used as definition point for all incoming arguments
*/
class EntryPointPseudoInst: public Inst
{
public:
    virtual bool isHeaderCriticalInst() const {return true;}

    Opnd * getDefArg(U_32 i)const;

    U_32 getArgStackDepth()const
    { return callingConventionClient.getArgStackDepth(Inst::OpndRole_Def); }

    CallingConventionClient& getCallingConventionClient(){ return callingConventionClient; }
    const CallingConventionClient& getCallingConventionClient()const { return callingConventionClient; }

    virtual bool hasSideEffect()const { return true; }

#ifdef _EM64T_
    Opnd * thisOpnd;
#endif
    //--------------------------------------------------------------------
protected:
    CallingConventionClient callingConventionClient;

    EntryPointPseudoInst(IRManager * irm, int id, const CallingConvention * cc);
    friend class    IRManager;
};


//=========================================================================================================
//   class CallInst
//=========================================================================================================
/** class CallInst is used for all calls instructions: CALL
*/
class CallInst: public ControlTransferInst
{
public:

    U_32 getArgStackDepth()const
    { return callingConventionClient.getArgStackDepth(Inst::OpndRole_Use); }

    U_32 getArgStackDepthAlignment() const
    { return callingConventionClient.getArgStackDepthAlignment(Inst::OpndRole_Use); }
    
    CallingConventionClient& getCallingConventionClient(){ return callingConventionClient; }
    const CallingConventionClient& getCallingConventionClient()const { return callingConventionClient; }

    Constraint getCalleeSaveRegs(OpndKind regKind=OpndKind_GPReg)const 
    {
        return callingConventionClient.getCallingConvention()->getCalleeSavedRegs(regKind);
    }

    Opnd::RuntimeInfo*  getRuntimeInfo() const {return runtimeInfo;}

protected:
    CallingConventionClient callingConventionClient;
    CallInst(IRManager * irm, int id, const CallingConvention * cc, Opnd::RuntimeInfo* targetInfo);

    //--------------------------------------------------------------------
    friend class    IRManager;
private:
    Opnd::RuntimeInfo* runtimeInfo;
};

//=========================================================================================================
//   class RetInst
//=========================================================================================================
/** class RetInst is used for ret instructions: RET, RET N
*/
class RetInst: public ControlTransferInst
{
public:
    RetInst(IRManager * irm, int id);
    friend class    IRManager;

    CallingConventionClient& getCallingConventionClient(){ return callingConventionClient; }
    const CallingConventionClient& getCallingConventionClient()const { return callingConventionClient; }

protected:
    CallingConventionClient callingConventionClient;

};

//==================================================================================
//   class EmptyPseudoInst
//==================================================================================
/** class EmptyPseudoInst is used to fill blocks which should not be considered as an empty
*/
class EmptyPseudoInst: public Inst {
public:
    void makeNative(IRManager * irManager) {}
protected:
    EmptyPseudoInst(int id): Inst(Mnemonic_NULL, id, Inst::Form_Extended) {
        kind = Kind_EmptyPseudoInst;
    }
    virtual bool hasSideEffect()const{ return true; }

friend class IRManager;
};

//==================================================================================
//   class MethodMarkerPseudoInst
//==================================================================================
/** class MethodMarkerPseudoInst is used to track inlined method boundaries
*/
class MethodMarkerPseudoInst: public Inst {
public:
    MethodDesc* getMethodDesc(){ return methDesc; }
    void makeNative(IRManager * irManager) {}
protected:
    MethodMarkerPseudoInst(MethodDesc* mDesc, int id, Kind k): Inst(Mnemonic_NULL, id, Inst::Form_Extended) {
        kind = k;
        methDesc = mDesc;
    }
    virtual bool hasSideEffect()const{ return true; }

friend class IRManager;

private:
    MethodDesc* methDesc;

};

//=========================================================================================================
//      class ConstantAreaItem
//=========================================================================================================
/**  class ConstantAreaItem

*/
class ConstantAreaItem
{
public:
    enum Kind{
        Kind_ConstantAreaItem=0xffffffff,
        Kind_ValueConstantAreaItem=0xff,
        Kind_FPSingleConstantAreaItem=0x1,
        Kind_FPDoubleConstantAreaItem=0x2,
        Kind_InternalStringConstantAreaItem=0x4,
        Kind_BinaryConstantAreaItem=0x80,
        Kind_SwitchTableConstantAreaItem=0x100,
    };

    ConstantAreaItem(Kind k, U_32 s, const void * v)
        :kind(k), size(s), value(v), address(NULL){}

    Kind                getKind()const{ return kind; }
    bool                hasKind(Kind k)const{ return (kind&k)==kind; }

    U_32              getSize()const{ return size; }
    void const *    getValue()const{ return value; }

    void *              getAddress()const { return address; }
    void                setAddress(void * addr) { address=addr; }
protected:
    const Kind          kind;
    const U_32        size;
    void const *    value;
    void *              address;
};

}}; // namespace Ia32

#endif
