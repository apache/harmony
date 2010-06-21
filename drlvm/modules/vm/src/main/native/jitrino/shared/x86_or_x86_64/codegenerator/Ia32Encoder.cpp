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

#include "Ia32Encoder.h"
#include "Ia32Inst.h"
#include <signal.h>
#include "mkernel.h"
#include "enc_prvt.h"
#include "PlatformDependant.h"

namespace Jitrino {
namespace Ia32 {

#ifdef _EM64T_
    //FIXME64: for adapter needs
    static bool is_ptr_type(const Type* typ) {
        switch(typ->tag) {
        case Type::TypedReference:
        case Type::SystemObject:
        case Type::SystemString:
        case Type::NullObject:
        case Type::Array:
        case Type::Object:
        case Type::BoxedValue:
        case Type::UnmanagedPtr:
        case Type::ManagedPtr:
        case Type::MethodPtr:
        case Type::VTablePtr:
        case Type::VTablePtrObj:
        case Type::ITablePtrObj:
            return true;
        default:
            break;
        }
        return false;
    }
#endif  // _EM64T_

static InstPrefix getInstPrefixFromSReg(RegName reg) {
    if (reg == RegName_Null) {
        return InstPrefix_Null;
    }
    if (reg == RegName_FS) {
        return InstPrefix_FS;
    }
    if (reg == RegName_GS) {
        return InstPrefix_GS;
    }
    if (reg == RegName_DS) {
        return InstPrefix_DS;
    }
    if (reg == RegName_ES) {
        return InstPrefix_ES;
    }
    if (reg == RegName_CS) {
        return InstPrefix_CS;
    }
    assert(false);
    return InstPrefix_Null;
}


const Encoder::OpcodeGroup Encoder::dummyOpcodeGroup;

const Encoder::MemOpndConstraints Encoder::memOpndConstraints[16]= {
    {{
    Constraint(OpndKind_GPReg, OpndSize_32), 
    Constraint(OpndKind_GPReg, OpndSize_32),
    Constraint(OpndKind_Imm, OpndSize_32),
    Constraint(OpndKind_Imm, OpndSize_32) }}, 
    // others contain null constraints, to be fixed later
};

Constraint Encoder::getAllRegs(OpndKind regKind)
{
    switch(regKind) {
    case OpndKind_GPReg:
        return Constraint(OpndKind_GPReg, 
                          Constraint::getDefaultSize(OpndKind_GPReg), 0xff);
    case OpndKind_XMMReg:
        return Constraint(OpndKind_XMMReg,
                          Constraint::getDefaultSize(OpndKind_XMMReg), 0xff);
    case OpndKind_FPReg:
        return Constraint(OpndKind_FPReg,
                          Constraint::getDefaultSize(OpndKind_FPReg), 0x1);
    case OpndKind_StatusReg:
        return Constraint(OpndKind_StatusReg,
                        Constraint::getDefaultSize(OpndKind_StatusReg), 0x1);
    default: 
        break;
    }
    return Constraint();
}

U_32 Encoder::getMnemonicProperties(Mnemonic mn)
{
    return getMnemonicProperties(*getMnemonicDesc(mn));
};

U_32 Encoder::getMnemonicProperties(const MnemonicDesc& mdesc)
{
    return (mdesc.flags & MF_CONDITIONAL ? Inst::Properties_Conditional:0) | 
            (mdesc.flags & MF_SYMMETRIC ? Inst::Properties_Symmetric : 0) |
            (mdesc.flags & MF_SAME_ARG_NO_USE? Inst::Properties_PureDef: 0);
;
};

//_________________________________________________________________________________________________
bool Encoder::matches(Constraint co, Constraint ci, U_32 opndRoles,
                      bool allowAliases)
{
    return co.isNull() || !(ci&co).isNull() || 
        (allowAliases && !(ci.getAliasConstraint(co.getSize())&co).isNull());
}

//_________________________________________________________________________________________________
const Encoder::OpcodeGroup * 
Encoder::findOpcodeGroup(const FindInfo& fi)
{
    Mnemonic m = fi.mnemonic;
    assert(m != Mnemonic_Null && m < Mnemonic_Count);
    const OpcodeGroupsHolder& mi = getOpcodeGroups()[m];
    // first, find better matching for already assigned operands
    for (U_32 i=0; i<mi.count; i++) {
        const OpcodeGroup* og=mi.opgroups+i;
        if (matches(og, fi, false)) {
            return og;
        }
    }

    // now find any matching suitable for the type constraint (initial)
    for (U_32 i=0; i<mi.count; i++) {
        const OpcodeGroup* og=mi.opgroups+i;
        if (matches(og, fi, true)) {
            return og;
        }
    }
    return NULL;
}

//_________________________________________________________________________________________________
bool Encoder::matches(const OpcodeGroup* og, const FindInfo& fi,
                      bool any)
{
    if (fi.isExtended) {
        if (fi.defOpndCount != og->opndRoles.defCount || 
            fi.opndCount != og->opndRoles.defCount+og->opndRoles.useCount) {
            return false;
        }
    }
    else {
        if (fi.opndCount != og->opndRoles.count) {
            return false;
        }
    }
    for (U_32 i = 0, n = fi.opndCount; i < n; i++) {
        U_32 idx = fi.isExtended ? og->extendedToNativeMap[i] : i;
        Constraint co=fi.opndConstraints[idx];
        if (any) {
            co = Constraint(OpndKind_Any, co.getSize());
        }
        if (!isOpndAllowed(og, i, co, fi.isExtended, any))
            return false;
    }
    return true;
}

//_________________________________________________________________________________________________
bool 
Encoder::isOpndAllowed(const Encoder::OpcodeGroup * og, U_32 i, Constraint co, bool isExtended, bool any)
{
        U_32 idx = isExtended ? og->extendedToNativeMap[i] : i;
        assert(idx<IRMaxNativeOpnds);

        Constraint ci=og->opndConstraints[idx];

        if (!matches(co, ci, Encoder::getOpndRoles(og->opndRoles,idx), any && (!(Encoder::getOpndRoles(og->opndRoles,idx)&Inst::OpndRole_Def) || getBaseConditionMnemonic(og->mnemonic) == Mnemonic_SETcc))) {
            return false;
        }
        return true;
}

//_________________________________________________________________________________________________
U_8* Encoder::emit(U_8* stream, const Inst * inst)
{
    Mnemonic mnemonic=inst->getMnemonic();
    #define OPND EncoderBase::Operand
    #define OPNDS EncoderBase::Operands
    EncoderBase::Operands args;

    Opnd * const * opnds = inst->getOpnds();
    const U_32 * roles = inst->getOpndRoles();

    for( int idx=0, n=inst->getOpndCount(); idx<n; idx++ ) { 
        if (!(roles[idx] & Inst::OpndRole_Explicit)) continue;
        const Opnd * p = opnds[idx];
        Constraint c = p->getConstraint(Opnd::ConstraintKind_Location);

        OpndSize sz = inst->opcodeGroup->opndConstraints[args.count()].getSize();
    
        switch( c.getKind() ) {
        case OpndKind_Imm:
            args.add(EncoderBase::Operand(sz, p->getImmValue()));
            break;
        case OpndKind_Mem:
            {
                RegName segReg = p->getSegReg();
                if (segReg == RegName_FS) {
                    stream = (U_8*)EncoderBase::prefix((char*)stream,
                                                         InstPrefix_FS);
                }

                const Opnd * pbase = 
                               p->getMemOpndSubOpnd(MemOpndSubOpndKind_Base);
                const Opnd * pindex =
                              p->getMemOpndSubOpnd(MemOpndSubOpndKind_Index);
                const Opnd * pscale =
                              p->getMemOpndSubOpnd(MemOpndSubOpndKind_Scale);
                const Opnd * pdisp =
                       p->getMemOpndSubOpnd(MemOpndSubOpndKind_Displacement);

                int disp = pdisp ? (int)pdisp->getImmValue() : 0 ;

                if (p->getMemOpndKind() == MemOpndKind_StackAutoLayout) {
                    disp += inst->getStackDepth();
                    if (Mnemonic_POP == mnemonic) {
                        disp -= STACK_SLOT_SIZE;
                    }
                }
                RegName baseReg = pbase == NULL ? RegName_Null : pbase->getRegName();
                RegName indexReg = pindex == NULL ? RegName_Null : pindex->getRegName();
#ifdef _EM64T_
                // FIXME64 adapter: all PTR types go as 64 bits
                // this is a porting quick workaround, should be fixed
                assert(pdisp == NULL || fit32(disp));
                sz = is_ptr_type(p->getType()) ? OpndSize_64 : sz;
#endif
                EncoderBase::Operand o(sz, 
                    baseReg,
                    indexReg,
                    NULL == pscale ? 0 : (unsigned char)pscale->getImmValue(),
                    disp);
                args.add( o );
                // Emit prefix here - so it relates to the real instruction
                // emitted alter, rather that to the hidden MOV inserted above
                InstPrefix instPrefix = getInstPrefixFromSReg(p->getSegReg());
                if (instPrefix != InstPrefix_Null) {
                    stream = (U_8*)EncoderBase::prefix((char*)stream, instPrefix);
                }
            }
            break;
        default:
            {
            assert(RegName_Null != p->getRegName());
            RegName opndReg = p->getRegName();
            // find an aliased register, with the same index and kind, but
            // with different size
            RegName reg = Constraint::getAliasRegName(opndReg, sz);
            assert(reg != RegName_Null);
            args.add(EncoderBase::Operand(reg));
            }
            break;
        }
    }

    //emit inst prefix
    if (inst->getPrefix()!=InstPrefix_Null) {
        stream = (U_8*)EncoderBase::prefix((char*)stream, inst->getPrefix());
    }
    return (U_8*)EncoderBase::encode((char*)stream, mnemonic, args);
}

static Mutex opcodeGroupsLock;

const Encoder::OpcodeGroupsHolder * Encoder::getOpcodeGroups(void)
{
    static OpcodeGroupsHolder opcodeGroups[Mnemonic_Count];
    static volatile bool initialized = false;
    if (!initialized) {
        opcodeGroupsLock.lock();
        if (!initialized) {
            memset(opcodeGroups, 0, sizeof opcodeGroups);
            initOpcodeGroups(opcodeGroups);
            initialized = true;
        }
        opcodeGroupsLock.unlock();
    }
    return opcodeGroups;
}

void Encoder::initOpcodeGroups(OpcodeGroupsHolder * table)
{
    for (unsigned mn = Mnemonic_Null; mn < Mnemonic_Count; mn++) {
        buildOGHolder(table+ mn, mnemonics[mn], opcodes[mn]);
    }
}


bool canBeIncluded(const Encoder::OpcodeGroup& og, Encoder::OpcodeDescription& od) 
{
    unsigned notEq= 0;
    unsigned oths= 0;

    for (unsigned j=0; j<og.opndRoles.count; j++) {
        
        if(!((Constraint)og.opndConstraints[j]).canBeMergedWith(od.opndConstraints[j]))
            return false;

        Constraint ogConstr = ((Constraint)og.opndConstraints[j]).intersectWith(Constraint((OpndKind)(OpndKind_Reg|OpndKind_Imm),OpndSize_Any,0xFFFF));
        Constraint odConstr = ((Constraint)od.opndConstraints[j]).intersectWith(Constraint((OpndKind)(OpndKind_Reg|OpndKind_Imm),OpndSize_Any,0xFFFF));

        Constraint::CompareResult compareResult = ogConstr.compare(odConstr);

        if(compareResult==Constraint::CompareResult_NotEqual)
            notEq++;
        else if (compareResult!=Constraint::CompareResult_Equal)
            oths++;

        if(notEq>1 || (notEq==1 && oths > 0))
            return false;
    }
    return true;
}

void Encoder::buildOGHolder(OpcodeGroupsHolder * mitem, 
                          const MnemonicDesc& mdesc,
                          const OpcodeDesc * opcodes)
{
    // must be called once for each item
    assert(mitem->count == 0);

    /*
    An idea is to split a set of opcodeDescriptions into several groups
    basing on the following criteria:
        1) the same operand properties (like DU_U)
        2) for wich min and max operand constraints for all operands can be 
           described by a single Constraint item per operand
        The consequences are:
        - corresponding operands in a group are of the same size
        - corresponding operands in a group can be assigned to the same 
          register kind
    */
    for (unsigned i=0; !opcodes[i].last; i++) {
        const OpcodeDesc* pOpcodeDesc = opcodes + i;
        if (pOpcodeDesc->platf == OpcodeInfo::decoder) {
            continue;
        }
        //
        // fill out an OpcodeDesc for each given opcode
        //
        OpcodeDescription od;
        initOD(od, pOpcodeDesc);
        //
        // try to find a group for the opcodeDesc in already filled groups
        //
        unsigned j=0;
        for ( ; j<mitem->count; j++) {
            OpcodeGroup& og = mitem->opgroups[j];
            // check (1) from above
            if( og.opndRoles.roles != od.opndRoles.roles ) continue;
            // check (2) from above
            //

            if (canBeIncluded(og, od)) {
                for (U_32 j=0; j<og.opndRoles.count; j++) {
                    og.opndConstraints[j].unionWith(od.opndConstraints[j]);
                }
                break;
            }
        }
        if (j != mitem->count) {
            continue;
        } else {
            // no appropriate group found, creating a new one.
            assert(mitem->count < COUNTOF(mitem->opgroups));
            OpcodeGroup& og = mitem->opgroups[mitem->count];
            initOG(og, mdesc);
            og.opndRoles = od.opndRoles;
            for (U_32 j=0; j<og.opndRoles.count; j++) {
                og.opndConstraints[j].unionWith(od.opndConstraints[j]);
            }
            ++mitem->count;
        }
    }

    for (unsigned i=0; i<mitem->count; i++) {
        finalizeOG(mitem->opgroups[i]);
    }

}

void Encoder::initOD(OpcodeDescription& od, const OpcodeDesc * opcode)
{
    od.opndRoles = opcode->roles;
    // .. fill the operands infos
    for (unsigned k=0; k<od.opndRoles.count; k++) {
        if (opcode->opnds[k].reg != RegName_Null) {
            // exact Register specified
            od.opndConstraints[k] = Constraint(opcode->opnds[k].reg);
        }
        else {
            od.opndConstraints[k] = Constraint(opcode->opnds[k].kind, 
                opcode->opnds[k].size == OpndSize_Null ? 
                                        OpndSize_Any : opcode->opnds[k].size);
        }
    }
}

void Encoder::initOG(OpcodeGroup& og, const MnemonicDesc& mdesc)
{
    og.mnemonic = mdesc.mn;
    og.properties = getMnemonicProperties(mdesc);

    const OpndRolesDescription NOOPNDS = {0, 0, 0, 0 };
    og.opndRoles = NOOPNDS;

    og.implicitOpndRegNames[0] = RegName_Null;
    og.implicitOpndRegNames[1] = RegName_Null;
    og.implicitOpndRegNames[2] = RegName_Null;

    if ((mdesc.flags & MF_AFFECTS_FLAGS) && (mdesc.flags & MF_USES_FLAGS)) {
        static const OpndRolesDescription _DU = {1, 1, 1, 
                                                OpndRole_Def|OpndRole_Use};
        og.implicitOpndRoles = _DU;
        og.implicitOpndRegNames[0] = RegName_EFLAGS;
    }
    else if (mdesc.flags & MF_AFFECTS_FLAGS) {
        static const OpndRolesDescription _D = {1, 1, 0, OpndRole_Def};
        og.implicitOpndRoles = _D;
        og.implicitOpndRegNames[0] = RegName_EFLAGS;
    }
    else if (mdesc.flags & MF_USES_FLAGS) {
        static const OpndRolesDescription _U = {1, 0, 1, OpndRole_Use};
        og.implicitOpndRoles = _U;
        og.implicitOpndRegNames[0] = RegName_EFLAGS;
    }

    og.printMnemonic = mdesc.name;
}


void Encoder::finalizeOG(OpcodeGroup& og) {

    U_32 etmIdx = 0;
    for (U_32 i = 0; i < og.opndRoles.count; i++) {
        if (getOpndRoles(og.opndRoles, i) & Inst::OpndRole_Def){
            og.extendedToNativeMap[etmIdx++] = i;
        }
        }
    assert(etmIdx == og.opndRoles.defCount);
    for (U_32 i = 0; i < og.opndRoles.count; i++) {
        if (getOpndRoles(og.opndRoles, i) & Inst::OpndRole_Use) {
            og.extendedToNativeMap[etmIdx++] = i;
    }
        }
    assert(etmIdx == (U_32)(og.opndRoles.defCount + og.opndRoles.useCount));

    U_32 memOpnds = 0;
    for (U_32 i = 0; i < og.opndRoles.count; i++) {
        if (og.opndConstraints[i].getKind() & OpndKind_Mem)
            memOpnds++;
    }
    if (memOpnds > 1)
        og.properties |= Inst::Properties_MemoryOpndConditional;
}

}}; //namespace Ia32
