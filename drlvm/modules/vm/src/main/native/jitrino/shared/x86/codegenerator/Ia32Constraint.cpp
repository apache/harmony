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

#include "Ia32Constraint.h"
namespace Jitrino
{
namespace Ia32{

///////////////////////////////////////////////////////////////////////////////////////////////

const Constraint Constraint::nullConstraint;


//=================================================================================================
// class Constraint
//=================================================================================================
const char * Constraint::parse(const char * str)
{
    Constraint szc, kc;
    for (U_32 i=0, j=0; i<0x100 && *str;){
        const char * tokenEnd=str; char token[0x100];
        for (j=0; j<0x100 && *tokenEnd && (isalpha(*tokenEnd)||isdigit(*tokenEnd)); tokenEnd++, j++)
            token[j]=*tokenEnd;
        token[j]=0;

        if (tokenEnd>str){

            RegName r=getRegName(token);
            if (r!=RegName_Null){
                if (!kc.canBeMergedWith(r))
                    return NULL;
                kc.unionWith(r);
            }else{
                OpndKind k=getOpndKind(token);
                if (k!=OpndKind_Null){
                    if (!kc.canBeMergedWith(k))
                        return NULL;
                    kc.unionWith(k);
                }else{
                    OpndSize sz=getOpndSize(token);
                    if (sz!=OpndSize_Null){
                        Constraint c(OpndKind_Any, sz);
                        if (!szc.canBeMergedWith(c))
                            return NULL;
                        szc=c;
                    }else
                        return NULL;
                }
            }
            str=tokenEnd;
        }else
            str++;
    }
    if (szc.isNull())
        szc=Constraint(OpndKind_Any, OpndSize_Any);
    if (kc.isNull())
        kc=Constraint(OpndKind_Any);
    *this=szc&kc;
    return str;
}

//_________________________________________________________________________________________________
OpndSize Constraint::getDefaultSize(U_32 k)
{
    OpndKind regKind=(OpndKind)(k & OpndKind_Reg);
    if (regKind){
        switch(regKind){
            case OpndKind_SReg:         return OpndSize_16;
            case OpndKind_FPReg:        return OpndSize_80;
            case OpndKind_XMMReg:       return OpndSize_128;
#ifdef _EM64T_
            case OpndKind_GPReg:        return OpndSize_64;
#else
            case OpndKind_GPReg:        return OpndSize_32;
#endif
            case OpndKind_StatusReg:    return OpndSize_32;
            default:                    return OpndSize_Any;
        }
    }
    return OpndSize_Any;
}

//_________________________________________________________________________________________________
Constraint Constraint::getAliasConstraint(OpndSize s, U_32 offset)const
{
    OpndSize sz=(OpndSize)size;
    if (s==OpndSize_Default){
        s=getDefaultSize(kind);
        if (s==OpndSize_Any)
            s=sz;
    }
    if (sz==s || s==OpndSize_Null || sz==OpndSize_Null)
        return *this;
    if (sz>s)
        return Constraint();

    U_32 newKind=kind, newMask=0;
    U_32 newRegKind=newKind & OpndKind_Reg;
    OpndSize maxSubregisterSize =
#ifdef _EM64T_
                                    OpndSize_32;
#else
                                    OpndSize_16;
#endif

    if (newRegKind == OpndKind_GPReg || ( (newRegKind & OpndKind_GPReg) && sz <= maxSubregisterSize) ){
#ifndef _EM64T_
        if (sz==OpndSize_8 && (s==OpndSize_16 || s==OpndSize_32))
            newMask=((mask>>4)|mask)&0xf;
        else if (sz==OpndSize_16)
            newMask=mask;
#else   // all registers on EM64T have respective subregisters
        newMask=mask;
#endif
    }else if (newRegKind==OpndKind_FPReg || newRegKind==OpndKind_XMMReg){
        newMask=mask;
    }
    if (newMask==0) newKind&=~OpndKind_Reg;
    return newKind==OpndKind_Null?Constraint():Constraint( (OpndKind)newKind, s, newMask);
}

//_________________________________________________________________________________________________
RegName Constraint::getAliasRegName(RegName regName, OpndSize sz, U_32 offset)
{
    if (regName==RegName_Null)
        return RegName_Null;
    OpndSize s=getRegSize(regName);
    if (sz==OpndSize_Any){
        sz=getDefaultSize(getRegKind(regName));
        if (sz==OpndSize_Any)
            sz=s;
    }
    if (sz==s)
        return regName;
    if (sz>s)
        return RegName_Null;
    
    OpndKind regKind=getRegKind(regName);

    if (regKind==OpndKind_GPReg){
#ifndef _EM64T_
        if (sz==OpndSize_8 && (s==OpndSize_16 || s==OpndSize_32)){
            U_32 idx=getRegIndex(regName);
            if (idx>4)
                return RegName_Null;
            return getRegName(regKind, sz, idx);
        }else if (sz==OpndSize_16) {
            return getRegName(regKind, sz, getRegIndex(regName));
        } 
#else
        return getRegName(regKind, sz, getRegIndex(regName));
#endif
    }else if (regKind==OpndKind_FPReg){
        return getRegName(regKind, sz, getRegIndex(regName));
    }else if (regKind==OpndKind_XMMReg){
        return getRegName(regKind, sz, getRegIndex(regName));
    }
    
    return RegName_Null;
}

//_________________________________________________________________________________________________
RegName Constraint::getAliasRegName(RegName regName, U_32 offset)const
{
    RegName rn=getAliasRegName(regName, (OpndSize)size, offset);
    return contains(rn)?rn:RegName_Null;
}


}}; // namespace Ia32

