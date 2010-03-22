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

#ifndef _IA32_CONSTRAINT_H_
#define _IA32_CONSTRAINT_H_

#include "open/types.h"
#include "Ia32IRConstants.h"

namespace Jitrino
{
namespace Ia32{
/**
class Constraint represents operand constraints and is used to 
describe Ia32 irregularities

The constraint framework is similar to a lattice Null < other constraints < Any
with partial order set by the contains method

The constraint contains 4 fields:
    OpndKind kind, OpndSize size, and register mask

Each constraint instance is exactly 4-bytes long making it possible to pass 
it directly by value.

*/

class Constraint
{
public:
    //----------------------------------------------------------------------
    /** enum CompareResult represent results of Constraint::compare
    
    The values are self-documenting
    */
    enum CompareResult
    {
        CompareResult_Equal=0,
        CompareResult_LeftContainsRight=1,
        CompareResult_RightContainsLeft=-1,
        CompareResult_NotEqual=2
    };

    //----------------------------------------------------------------------
    /** Creates a Null constraint */
    Constraint()
    { fullValue = 0;}

    /** Creates a constraint of the specified OpndKind
    This allows for passing of OpndKind values wherever Constraint is expected

    The size of the constraint is set to OpndSize_Any

    For OpndKind_Reg and sub-kinds initializes the mask field to 0xffff
    */
    Constraint(OpndKind k)
        :mask(OpndKind_Reg&k?0xffff:0), size(OpndSize_Any), kind(k)
    { assert(k!=OpndKind_Null); }

    /** Creates a constraint of the specified OpndKind k and OpndSize s

    Both k and s cannot be _Null.

    If k contains OpndKind_Reg the constructor initializes the mask field to 0xffff
    */
    Constraint(OpndKind k, OpndSize s)
        :mask(OpndKind_Reg&k?0xffff:0), size(s), kind(k)
    { assert(k!=OpndKind_Null && size!=OpndSize_Null); }

    /** Creates a constraint of the specified OpndKind, OpndSize, and register mask m

    Both k and s cannot be _Null, and k must contain be OpndKind_Reg if mask is not null

    For OpndKind_Reg and sub-kinds initializes the mask field to 0xffff
    */
    Constraint(OpndKind k, OpndSize s, U_32 m)
        :mask(m), size(s), kind(k)
    {   assert(k!=OpndKind_Null && size!=OpndSize_Null); assert(mask==0||(OpndKind_Reg&k)!=0);  }

    /** Creates a constraint corresponding to the specified physical register RegName
    This allows for passing of RegName values wherever Constraint is expected

    The size of the constraint is initialized to the size for the specified RegName
    returned by getRegSize(OpndKind)

    The mask field of the constraint is initialized to the mask corresponding to the specified register
    */
    Constraint(RegName reg)
        :mask(getRegMask(reg)), size(getRegSize(reg)), kind(getRegKind(reg)) {}

    
    Constraint(const char * str){ fullValue = 0; str=parse(str); assert(str!=NULL); }
        
    const char * parse(const char * str);

    //----------------------------------------------------------------------
    /** returns the kind field of the constraint */
    U_32 getKind()const{ return kind; }
    /** returns the size field of the constraint */
    OpndSize getSize()const{ return (OpndSize)size; }
    /** returns the mask field of the constraint */
    U_32 getMask()const{ return mask; }

    /** sets the mask field of the constraint */
    void setMask(U_32 m){ mask=m; }

    /** resets the constraint to the Null value */
    void makeNull(){ fullValue = 0; }

    /** resets the constraint to the Any value */
    void makeAny()
        { fullValue = OpndKind_Any|OpndSize_Any|0xffff; }

    /** Convenience operator |
    
    Creates a copy of 'this' constraint, unions it with c, and returns the result
    */
    Constraint operator|(Constraint c)const
        { Constraint l=*this;   l.unionWith(c); return l; }

    /** Convenience operator &
    
    Creates a copy of 'this' constraint, intersects it with c, and returns the result
    */
    Constraint operator&(Constraint c)const
        { Constraint l=*this;   l.intersectWith(c); return l; }

    /** Returns true if the constraint is Null (its kind is Null) */
    bool isNull()const{ return kind==OpndKind_Null; }

    /** Returns the default size for the OpndKind combination k */
    static OpndSize getDefaultSize(U_32 k);

    /** Returns true if 'this' can be merged (via unionWith) with c 
    
    Sizes must be equal, and kinds cannot designate different reg kinds (like GPReg vs XMMReg)

    Note: Null constraints are mergeable with any constraints
    */
    bool        canBeMergedWith(Constraint c)
    {
        U_32 filter = (U_32)OpndKind_Reg << 24;
        U_32 thisTmp = fullValue & filter, cTmp = c.fullValue & filter, rTmp = thisTmp & cTmp; 
        if (rTmp!=thisTmp && rTmp!=cTmp)
            return false;
        filter = (U_32)OpndSize_Any << 16;
        thisTmp = fullValue & filter, cTmp = c.fullValue & filter, rTmp = thisTmp & cTmp; 
        return rTmp==thisTmp || rTmp==cTmp;
    }

    /** Unions 'this' with c  (|-like)
    
    this must be mergeable with c (canBeMergedWith(c) must return true)

    */
    Constraint& unionWith(Constraint c)
        {   assert(canBeMergedWith(c)); fullValue |= c.fullValue;  return *this;  }

    /** Intersects 'this' constraint with c  (&-like) */
    Constraint& intersectWith(Constraint c)
    {
        fullValue &= c.fullValue;
        if (size==0 || kind==0) fullValue=0; else if (mask==0) kind&=~OpndKind_Reg;
        return *this;
    }

    /** returns true if 'this' constrains contains c */
    bool contains(Constraint c)const{ return (*this&c)==c; }

    /** returns true if 'this' constraint exactly equals to c */
    bool operator==(Constraint c)const
        { return fullValue == c.fullValue; }
    bool operator!=(Constraint c)const
        { return fullValue != c.fullValue; }

    /** Determines the relationship of between 'this' constraint and c
    
    The semantics of the operation and possible result values are defined by the CompareResult enumeration
    */
    CompareResult compare(Constraint c)const
        { return *this==c?CompareResult_Equal:contains(c)?CompareResult_LeftContainsRight:c.contains(*this)?CompareResult_RightContainsLeft:CompareResult_NotEqual; }

    /** Returns the constraint for an outer aliased operand
    s must be greater than or equal to the constraint's size
    */
    Constraint getAliasConstraint(OpndSize s, U_32 offset=0)const;


    /** Returns the regname for an aliased regname
    In some sense this operations is reverse to getAliasConstraint.
    e.g. for eax it can return eax, ax, ah, al, depending on the constraint
    The constraint's size must be less than or equal to regName's size
    */
    static RegName      getAliasRegName(RegName regName, OpndSize s, U_32 offset=0);
    RegName     getAliasRegName(RegName regName, U_32 offset=0)const;

    //----------------------------------------------------------------------
private:
    union{
        struct {
            U_32  mask:16;
            U_32  size:8;
            U_32  kind:8;
        };
        U_32  fullValue;
    };
    
    friend struct DefaultConstraintInitializer;

    const static Constraint nullConstraint;
};


}; // namespace Ia32
}
#endif
