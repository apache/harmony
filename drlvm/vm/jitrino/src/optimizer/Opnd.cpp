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

#include <iostream>
#include "Opnd.h"
#include "Inst.h"
#include "IRBuilder.h"

namespace Jitrino {

NullOpnd    OpndManager::_nullOpnd;

//-----------------------------------------------------------------------------
// Method for printing operands.  Move to an IRPrinter file.
//-----------------------------------------------------------------------------
void    Opnd::print(::std::ostream& os) const {
    if (isGlobal()) 
        os << "g";
    else
        os << "t";
    os << (int)getId();
}

void    VarOpnd::print(::std::ostream& os) const {
    os << "v" << (int)getId();
}

void    SsaVarOpnd::print(::std::ostream& os) const {
    var->print(os);
    os << "." << (int)getId();
}

void    PiOpnd::print(::std::ostream& os) const {
    orgOpnd->print(os);
    os << "." << (int)getId();
}

void    OpndBase::printWithType(::std::ostream& os) const {
    print(os);
    os << ":";
    getType()->print(os);
}


void    VarOpnd::printWithType(::std::ostream& os) const {
    Opnd::printWithType(os);
    if (isPinnedFlag == true)
        os << " PINNED";
}


void    SsaVarOpnd::printWithType(::std::ostream& os) const {
    print(os);
    os << ":";
    getType()->print(os);
}

void    VarOpnd::addVarAccessInst(VarAccessInst* varInst) {
    if (varInst->isStVar())
        numStores++;
    else
        numLoads++;
    varAccessInsts = varInst;
}

void  OpndManager::deleteVar(VarOpnd *var) {
    // delete all instructions that ld/st this var
    VarAccessInst* inst = var->varAccessInsts;
    while (inst != NULL) {
        VarOpnd *instVar = inst->getBaseVar();
        if (!instVar || (instVar == var)) {
            if (Log::isEnabled()) {
                Log::out() << "Removing inst ";
                inst->print(Log::out());
                Log::out() << " for deleted var ";
                var->print(Log::out());
                Log::out() << ::std::endl;
            }
            inst->unlink();
        } else {
            if (Log::isEnabled()) {
                Log::out() << "Found misfiled varAccessInst ";
                inst->print(Log::out());
                Log::out() << " for deleted var ";
                var->print(Log::out());
                Log::out() << ::std::endl;
            }
            instVar->addVarAccessInst(inst);
            if (instVar->isDeadFlag) {
                // looks already deleted;
                inst->unlink();
                if (Log::isEnabled()) {
                    Log::out() << "Correct var looks already deleted, so removing inst anyway."
                               << ::std::endl;
                }
            } else {
                // don't delete it
            }
        }
        inst = inst->getNextVarAccessInst();
    }
    var->isDeadFlag = true;
    if (var == varOpnds) {
        if (var == var->nextVarInMethod) {
            varOpnds = 0;
        } else {
            varOpnds = var->nextVarInMethod;
        }
    }
    var->nextVarInMethod->prevVarInMethod = var->prevVarInMethod;
    var->prevVarInMethod->nextVarInMethod = var->nextVarInMethod;
}

Type* 
OpndManager::getOpndTypeFromLdType(Type* ldType) {
    switch (ldType->tag) {
    case Type::Boolean:  case Type::Char:
    case Type::Int8:     case Type::Int16:     case Type::Int32:
    case Type::UInt8:    case Type::UInt16:    case Type::UInt32:
        return typeManager.getInt32Type();

    case Type::IntPtr:   case Type::UIntPtr:
        return ldType;
        //return typeManager.getIntPtrType();

    case Type::Int64:    case Type::UInt64:
        return typeManager.getInt64Type();

    case Type::Single:
        return typeManager.getSingleType();
    case Type::Double:
        return typeManager.getDoubleType();
    case Type::Float:
        return typeManager.getFloatType();
        // object types
    case Type::CompressedSystemObject:
    case Type::CompressedUnresolvedObject:
    case Type::CompressedSystemClass:
    case Type::CompressedSystemString:
    case Type::CompressedArray:           case Type::CompressedObject:
    case Type::CompressedNullObject:
    case Type::SystemObject:    case Type::SystemClass:    case Type::SystemString:
    case Type::Array:           case Type::Object:
    case Type::UnresolvedObject:
    case Type::NullObject:
    case Type::Offset:  case Type::OffsetPlusHeapbase:
    case Type::VTablePtr:
    case Type::Tau:
        return ldType;
        // value and pointer types
    case Type::Value:
        if (ldType->isEnum()) {
            return getOpndTypeFromLdType(((EnumType*)ldType)->getUnderlyingType());
        }
        return ldType;
    case Type::UnmanagedPtr:
        //return typeManager.getIntPtrType();
    case Type::ManagedPtr:        // Managed ptr is valid only for ldvar or ldarg
    case Type::TypedReference:    // TypedReference is valid only for ldvar or ldarg
        return ldType;
        // function pointers
    case Type::MethodPtr:
        return ldType;
    case Type::Void:
        return typeManager.getVoidType();    // happens only for calls
    case Type::VTablePtrObj:
    case Type::ITablePtrObj:
    case Type::ArrayElementType:
        return ldType;
    default:
        assert(0);    // missed something
    }
    return typeManager.getVoidType();    // get rid of compiler warning message
}

Opnd*
OpndRenameTable::duplicate(OpndManager& opndManager, Opnd* opndToRename) {
    if (opndToRename->isSsaTmpOpnd()) {
        Opnd *newOpnd = opndManager.createSsaTmpOpnd(opndToRename->getType());
        // record the correspondence between the new operand and the old operand
        setMapping(opndToRename,newOpnd);
        return newOpnd;
    } else if (opndToRename->isSsaOpnd()) {
        if (!renameSsaOpnd) {
            assert(0);
        } else {
            VarOpnd* var = ((SsaVarOpnd*)opndToRename)->getVar();
            Opnd *newOpnd = opndManager.createSsaVarOpnd(var);
            setMapping(opndToRename,newOpnd);
            return newOpnd;
        }
    } else {
    }
    return opndToRename;
}


} //namespace Jitrino 
