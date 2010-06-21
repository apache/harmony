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

#include "Ia32CallingConvention.h"
#include "Ia32IRManager.h"

namespace Jitrino {
namespace Ia32 {

const CallingConvention * CallingConvention::str2cc(const char * cc_name) {
    if( NULL == cc_name ) { // default
        return &CallingConvention_STDCALL;
    }

    if( !strcmpi(cc_name, "stdcall") ) {
        return &CallingConvention_STDCALL;
    }

    if( !strcmpi(cc_name, "drl") ) {
        return &CallingConvention_Managed;
    }

    if( !strcmpi(cc_name, "cdecl") ) {
        return &CallingConvention_CDECL;
    }
    assert( false );
    return NULL;
}


//========================================================================================
STDCALLCallingConvention        CallingConvention_STDCALL;
CDECLCallingConvention          CallingConvention_CDECL;
ManagedCallingConvention        CallingConvention_Managed;
MultiArrayCallingConvention     CallingConvention_MultiArray;

//========================================================================================
// STDCALLCallingConvention
//========================================================================================

#ifdef _EM64T_
#ifdef _WIN64
const RegName fastCallGPRegs[4] = {RegName_RCX, RegName_RDX, RegName_R8, RegName_R9} ;
const RegName fastCallFPRegs[4] = {RegName_XMM0,RegName_XMM1,RegName_XMM2,RegName_XMM3};
#else 
const RegName fastCallGPRegs[6] = {RegName_RDI, RegName_RSI, RegName_RDX, RegName_RCX, RegName_R8, RegName_R9} ;
const RegName fastCallFPRegs[8] = {RegName_XMM0,RegName_XMM1,RegName_XMM2,RegName_XMM3,RegName_XMM4,RegName_XMM5,RegName_XMM6,RegName_XMM7};
#endif
#endif

#ifdef _IA32_
//______________________________________________________________________________________
void STDCALLCallingConventionIA32::getOpndInfo(ArgKind kind, U_32 count, OpndInfo * infos) const
{
    if (kind == ArgKind_InArg) {
        for (U_32 i=0; i<count; i++){
    
            Type::Tag typeTag=(Type::Tag)infos[i].typeTag;
            OpndSize size=IRManager::getTypeSize(typeTag);
            assert(size!=OpndSize_Null && size<=OpndSize_64);

            infos[i].slotCount=1;
            infos[i].slots[0]=RegName_Null;
            infos[i].isReg=false;
    
            if (size==OpndSize_64){
                infos[i].slotCount=2;
                infos[i].slots[1]=RegName_Null;
            }
        }
    } else {
        assert(kind == ArgKind_RetArg);
        assert(count <= 1);
        if (count == 1) {
            Type::Tag typeTag=(Type::Tag)infos[0].typeTag;
            infos[0].isReg=true;
            switch (typeTag) {
                case Type::Void:
                    infos[0].slotCount=0;
                    break;
                case Type::Float:
                case Type::Double:
                case Type::Single:
                    infos[0].slotCount=1;
                    infos[0].slots[0]=RegName_FP0;
                    break;
                default: {
                    OpndSize size=IRManager::getTypeSize(typeTag);
                    assert(size!=OpndSize_Null && size<=OpndSize_64);
    
                    infos[0].slotCount=1;
                    infos[0].slots[0]=RegName_EAX;
        
                    if (size == OpndSize_64) {
                        infos[0].slotCount=2;
                        infos[0].slots[1]=RegName_EDX;
                    }
                }
            };
        }
    }
}

#else
void STDCALLCallingConventionEM64T::getOpndInfo(ArgKind kind, U_32 count, OpndInfo * infos) const
{
    if (kind == ArgKind_InArg) {
        U_32 gpreg = 0;
#ifdef _WIN64
    #define fpreg gpreg
#else
    U_32 fpreg = 0;
#endif

        for (U_32 i=0; i<count; i++){
    
            Type::Tag typeTag=(Type::Tag)infos[i].typeTag;
            if(((typeTag>Type::Float  ||typeTag<Type::Single)  && gpreg < lengthof(fastCallGPRegs))) {
                infos[i].slotCount=1;
                infos[i].slots[0]=fastCallGPRegs[gpreg];
                infos[i].isReg=true;
                gpreg++;
            } else if(((typeTag<=Type::Float && typeTag>=Type::Single)  && fpreg < lengthof(fastCallFPRegs))) {
                infos[i].slotCount=1;
                infos[i].slots[0]=fastCallFPRegs[fpreg];
                infos[i].isReg=true;
                fpreg++;
            } else {
                infos[i].slotCount=1;
                infos[i].slots[0]=RegName_Null;
                infos[i].isReg=false;
            }
        }
    } else {
        assert(kind == ArgKind_RetArg);
        assert(count <= 1);
        if (count == 1) {
            Type::Tag typeTag=(Type::Tag)infos[0].typeTag;
            infos[0].isReg=true;
            switch (typeTag) {
                case Type::Void:
                    infos[0].slotCount=0;
                    break;
                case Type::Float:
                case Type::Double:
                case Type::Single:
                    infos[0].slotCount=1;
                    infos[0].slots[0]=RegName_XMM0;
                    break;
                default: {
                    OpndSize size=IRManager::getTypeSize(typeTag);
                    infos[0].slotCount=1;
                    infos[0].slots[0]=RegName_RAX;
                    
                    if (size == OpndSize_128) {
                        infos[0].slotCount=2;
                        infos[0].slots[1]=RegName_RDX;
                    }
                }
            };
        }
    }
}
#endif

#ifdef _IA32_
//______________________________________________________________________________________
Constraint STDCALLCallingConventionIA32::getCalleeSavedRegs(OpndKind regKind) const
{
    switch (regKind){
        case OpndKind_GPReg:
            return (Constraint(RegName_EBX)|RegName_EBP|RegName_ESI|RegName_EDI);
        default:
            return Constraint();
    }
}

#else
Constraint STDCALLCallingConventionEM64T::getCalleeSavedRegs(OpndKind regKind) const
{
    switch (regKind){
        case OpndKind_GPReg:
#ifdef _WIN64
            return (Constraint(RegName_RBX)|RegName_RBP|RegName_R12|RegName_R13|RegName_R14|RegName_R15|RegName_RSI|RegName_RDI);
#else
            return (Constraint(RegName_RBX)|RegName_RBP|RegName_R12|RegName_R13|RegName_R14|RegName_R15);
#endif
        default:
            return Constraint();
    }
}
#endif


//______________________________________________________________________________________
#ifdef _IA32_
void ManagedCallingConventionIA32::getOpndInfo(ArgKind kind, U_32 count, OpndInfo * infos) const
{
    if (kind == ArgKind_RetArg) {
        assert(count <= 1);
        if (count == 1) {
            Type::Tag typeTag = (Type::Tag)infos[0].typeTag;
            switch (typeTag) {
                case Type::Float:
                case Type::Double:
                case Type::Single:
                    infos[0].isReg = true;
                    infos[0].slotCount = 1;
                    infos[0].slots[0] = RegName_XMM0;
                    return;
                default:;
            };
        }
        
    }
    return STDCALLCallingConventionIA32::getOpndInfo(kind, count, infos);
}
#else
#endif

#ifdef _IA32_
#else
void MultiArrayCallingConventionEM64T::getOpndInfo(ArgKind kind, U_32 count, OpndInfo * infos) const
{
    if (kind == ArgKind_InArg){
        for (U_32 i = 0; i < count; i++) {    
                infos[i].slotCount = 1;
                infos[i].slots[0] = RegName_Null;
                infos[i].isReg = false;
        }
    } else {
        CDECLCallingConventionEM64T::getOpndInfo(kind, count, infos);
    }
}
#endif

};  // namespace Ia32
}
