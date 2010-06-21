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

#ifndef _ABCD_BOUNDS_H
#define _ABCD_BOUNDS_H

#include <cstddef>
#include <iostream>
#include "open/types.h"
#include "Type.h"

#include "Stl.h"

#include "Opnd.h"

namespace Jitrino {

class Inst;
class Opnd;
class ConstBound;
class PiBoundIter;
class AbcdReasons;

class AbcdReasons {
private:
    AbcdReasons(const AbcdReasons &other) : facts(other.facts) { assert(0); }
    AbcdReasons& operator=(const AbcdReasons &other) { assert(0); return *this; }
public:
    StlSet<SsaTmpOpnd *> facts; // tau operands
    typedef StlSet<SsaTmpOpnd *> facts_type;
    AbcdReasons(MemoryManager &mm) : facts(mm) {};
    void addReasons(AbcdReasons &alsoNeed) {
        facts_type::iterator i = alsoNeed.facts.begin();
        while (i != alsoNeed.facts.end()) {
            facts.insert(*i);
            i++;
        }
    };
    void addReasons(StlVector<AbcdReasons *> *alsoNeeds) { 
        assert(alsoNeeds);
        StlVector<AbcdReasons *>::iterator
            iter = alsoNeeds->begin(),
            end = alsoNeeds->end();
        for ( ; iter != end; ++iter) {
            AbcdReasons *alsoNeed = *iter;
            assert(alsoNeed);
            facts_type::iterator i = alsoNeed->facts.begin();
            while (i != alsoNeed->facts.end()) {
                facts.insert(*i);
                i++;
            }
        }
    };
    void addReason(SsaTmpOpnd *tau) {
        facts.insert(tau);
    };
    void clear() {
        facts.clear();
    }
    void print(::std::ostream &os) const;
};

// rather than having to create new variable objects to represent
// the upper and lower bounds on a variable, we just add a qualifier
// var_is_lb which indicates which of the 2 bounds of that variable this
// bound is based on.
struct VarBound {
    Opnd *the_var;
    bool operator==(const VarBound &other) const {
        return(the_var == other.the_var);
    }
    bool operator<(const VarBound &other) const {
        return(the_var < other.the_var);
    }
    VarBound(Opnd *v) : the_var(v) {};
    VarBound() : the_var(0) {};
    VarBound(const VarBound &other) : the_var(other.the_var) {};
    VarBound(const VarBound &other,
             Opnd *renameFrom, Opnd *renameTo) :
        the_var((other.the_var==renameFrom) ? renameTo : other.the_var)
    {};
    VarBound operator||(const VarBound &other) {
        if (isEmpty()) { return other; }
        else { return *this; };
    }
    VarBound &operator=(const VarBound &other) {
        the_var = other.the_var;
        return *this;
    };
    bool isEmpty() const { return (the_var == 0); };
    void print(::std::ostream &os) const;

    PiBoundIter getPredecessors(bool boundingBelow, 
                                bool useReasons,
                                MemoryManager &mm0) const;
    bool isMinMax(bool isMin) const;
    bool isPhiVar() const;
    bool isConvVar() const;
    VarBound getConvSource() const;
    bool convPassesSource() const; // true if conversion is one-to-one
    size_t hash() const { return ((((size_t)the_var)*17)>>4); };

    bool isConvexFunction() const; // constant inputs => constant outputs
    ConstBound getConvexInputBound(bool isLB, ConstBound outputBound,
                                   VarBound &inputVar) const;

    // true if type1 includes all values from type2
    static bool typeIncludes(Type::Tag type1, Type::Tag type2);
};

class ConstBound {
    int64 the_const;
    enum Flag { Const_PlusInfinity,
                Const_IsConst,
                Const_MinusInfinity,
                Const_IsNull} flag;
public:
    ConstBound() : the_const(0), flag(Const_IsNull) {};
    ConstBound(I_32 c) : the_const(c), flag(Const_IsConst) {};
    ConstBound(int64 c) : the_const(c), flag(Const_IsConst) {};
    ConstBound(bool positive) : the_const(0), 
                                flag(positive ? Const_PlusInfinity
                                     : Const_MinusInfinity) {};
    bool isNull() const { return (flag == Const_IsNull); };
    void setNull() { flag = Const_IsNull; };
    bool isKnown() const { return (flag == Const_IsConst); };
    bool isUnknown() const { return (flag != Const_IsConst); };
    bool isPlusInfinity() const { return (flag == Const_PlusInfinity); };
    bool isMinusInfinity() const { return (flag == Const_MinusInfinity); };
    int64 getInt64() const { return the_const; };
    I_32 getInt32() const { return (I_32)the_const; };
    bool operator ==(const ConstBound &other) const {
        if (flag == other.flag) {
            if (flag == Const_IsConst) {
                return (the_const == other.the_const);
            } else {
                return true;
            }
        } else
            return false;
    }
    bool operator <=(const ConstBound &other) const {
        if (flag == other.flag) {
            if (flag == Const_IsConst) {
                return (the_const <= other.the_const);
            } else {
                return true;
            }
        } else if ((other.flag == Const_PlusInfinity) ||
                   (flag == Const_MinusInfinity)) {
            return true;
        } else
            return false;
    }
    bool operator <(const ConstBound &other) const {
        if (flag == other.flag) {
            if (flag == Const_IsConst) {
                return (the_const < other.the_const);
            } else
                return false;
        } else if ((other.flag == Const_PlusInfinity) ||
                   (flag == Const_MinusInfinity)) {
            return true;
        } else
            return false;
    }
    bool operator >=(const ConstBound &other) const {
        return (other <= *this);
    }
    bool operator >(const ConstBound &other) const {
        return (other < *this);
    }
    ConstBound operator+(const ConstBound &other) const {
        if (flag == other.flag) {
            if (flag == Const_IsConst) {
                int64 result = the_const + other.the_const;
                // check for overflow
                // aeqb has 1 bits wherever a and b are the same.
                int64 aeqb = (the_const ^ other.the_const ^ __INT64_C(-1));
                // overflowed has sign bit set if overflowed
                int64 overflowed = aeqb & (result ^ the_const);
                if (overflowed < 0) {
                    if (the_const > 0) {
                        return ConstBound(true); // PlusInfinity
                    } else {
                        return ConstBound(false); // PlusInfinity
                    }
                } else {
                    return ConstBound(result);
                }
            } else
                return *this;
        } else {
            if (flag == Const_IsConst) {
                return other;
            } else if (other.flag == Const_IsConst) {
                return *this;
            } else {
                assert(0); // is unknown!
                return *this;
            }
        }
    }
    ConstBound operator-() const {
        switch (flag) {
        case Const_IsConst:
            return ConstBound(-the_const);
        case Const_PlusInfinity:
            return ConstBound(false); // MinusInfinity;
        case Const_MinusInfinity:
            return ConstBound(true); // PlusInfinity
    default:
        assert(0);
        break;
        }
        return *this;
    }
    ConstBound operator-(const ConstBound &other) const {
        if (flag == other.flag) {
            if (flag == Const_IsConst) {
                int64 result = the_const - other.the_const;
                // check for overflow
                // avb has 1 bits wherever a and b differ
                int64 avb = (the_const ^ other.the_const);
                // overflowed has sign bit set if overflowed
                int64 overflowed = avb & (result ^ the_const);
                if (overflowed < 0) {
                    if (the_const > 0) {
                        return ConstBound(true); // PlusInfinity
                    } else {
                        return ConstBound(false); // PlusInfinity
                    }
                } else {
                    return ConstBound(result);
                }
            } else {
                // undefined
                assert(0);
                return *this;
            }
        } else {
            if (flag == Const_IsConst) {
                if (other.flag == Const_PlusInfinity) {
                    return ConstBound(false); // MinusInfinity
                } else
                    return ConstBound(true); // PlusInfinity
            } else {
                return *this;
            }
        }
    }
    void print(::std::ostream& os) const;
};
    
// represents condition on some variable x:
//   var_multiple * var_part + const_part
// all arithmetic is appropriate to the given type
//   only compatible operations can be done
class ConstInst;

class PiBound {
    int64 var_multiple;  // 0 if none or undefined or unknown
    VarBound var_part;   // isEmpty() if var_multiple is 0
    int64 const_part;    // 0 if undefined or unknown
    enum Flag { PiBound_Undefined = 0,  // under-constrained
                // as a lower bound, Undefined = -infinity,
                // as an upper bound, Undefined = +infinity,
                PiBound_Normal = 1,
                PiBound_Unknown = 2     // over-constrained
                // as a lower bound, Unknown = +infinity,
                // as an upper bound, Unknown = -infinity,
    } flag;
    enum Type::Tag typetag;
public:
    typedef enum Type::Tag TypeTag;
    TypeTag getType() const { return typetag; };
    PiBound(TypeTag typein, VarBound var, int64 multiple=1) :
        var_multiple(multiple), var_part(var),
        const_part(0), 
        flag(PiBound_Normal), typetag(typein) {};
    PiBound(TypeTag typein, int64 c) : 
        var_multiple(0), var_part(),
        const_part(c), flag(PiBound_Normal), typetag(typein) {};
    PiBound(TypeTag typein, int64 multiple, VarBound var, int64 c) :
        var_multiple(multiple), var_part(var), 
        const_part(c), flag(PiBound_Normal), typetag(typein) {};
    PiBound(TypeTag typein, bool isUnknown) : 
        var_multiple(0), var_part(),
        const_part(0), 
        flag(isUnknown ? PiBound_Unknown : PiBound_Undefined),
        typetag(typein)
    {};
    PiBound(const PiBound &other, Opnd *renameFrom, Opnd *renameTo) :
        var_multiple(other.var_multiple), 
        var_part(other.var_part, renameFrom, renameTo),
        const_part(other.const_part),
        flag(other.flag),
        typetag(other.typetag)
    {};
    PiBound(const PiBound &other, Opnd *renameFrom, 
            const PiBound &renameTo) :
        var_multiple(other.var_multiple),
        var_part(other.var_part),
        const_part(other.const_part),
        flag(other.flag),
        typetag(other.typetag)
    {
        if (other.var_part.the_var == renameFrom) {
            assert(flag == PiBound_Normal);
            if (renameTo.flag == PiBound_Normal) {
                var_multiple = other.var_multiple * renameTo.var_multiple;
                if (var_multiple == 0) {
                    var_part = VarBound();
                } else {
                    assert((other.var_multiple == 1) &&
                           (renameTo.var_multiple == 1) &&
                           (var_multiple == 1)); // for now
                    var_part = renameTo.var_part;
                }
                const_part = other.const_part + renameTo.const_part;
                int64 overflowed = 
                    (((const_part ^ other.const_part) &
                      (const_part ^ renameTo.const_part))
                     & __INT64_C(0x8000000000000));
                if (overflowed) {
                    *this = getUnknown();
                    return;
                }
                assert(renameTo.flag == PiBound_Normal);
                assert(renameTo.typetag == other.typetag);
            } else {
                *this = renameTo;
            }
        }
    };

    bool isUnknown() const { return (flag == PiBound_Unknown); };
    bool isUndefined() const { return (flag == PiBound_Undefined); };
    PiBound getUnknown() const { return PiBound(typetag, true); }
    PiBound getUndefined() const { return PiBound(typetag, false); }

    bool isNormal() const { return (flag == PiBound_Normal); };
    bool isVar() const { return (isNormal() && (const_part == 0)
                                 && (var_multiple == 1)); };
    bool isConst() const { return (isNormal() && (var_multiple == 0)); };
    bool isVarPlusConst() const { return (isNormal() 
                                          && (var_multiple == 1)); };
    VarBound getVar() const { return var_part; };
    int64 getConst() const { return const_part; };
    
    PiBound add(const PiBound &other, bool is_signed) const;
    PiBound neg() const;
    PiBound mul(const PiBound &other, bool is_signed) const;
    PiBound cast(Type::Tag newtype, bool isLb) const;
    PiBound abs() const;

    bool isComparableWith(const PiBound &other) const;

    bool isNonNegativeConstant() const;
    bool isNegativeConstant() const;
    bool isConstant() const;
    void print(::std::ostream& os) const;

    // Does the range [this, infinity] include [other, infinity]?
    bool isLessEq(const PiBound &other) const;
    // Return x such that [x, infinity] includes the union of
    // the intervals [this, infinity] and [other, infinity].
//    PiBound min(const PiBound &other) const;

    // Does the range [-infinity, this] include [-infinity, other]?
    bool isGreaterEq(const PiBound &other) const;
    // Return x such that [-infinity, x] includes the union of
    // the intervals [-infinity, this] and [-infinity, other].
//    PiBound max(const PiBound &other) const;

    PiBound invert(VarBound vb) const {
        if (isUnknown() || isUndefined()) return *this;
        assert (var_multiple == 1);
        int64 negc = -const_part;
        if (const_part == negc) return getUnknown();
        return PiBound(typetag, 1, vb, negc);
    }

    bool operator<(const PiBound &b) const {
        if ((flag < b.flag) ||
            ((flag == b.flag) &&
             ((getVar().the_var < b.getVar().the_var) ||
              ((getVar().the_var == b.getVar().the_var) &&
               (const_part < b.const_part)))))
            return true;
        return false;
    }

    // returns true if ci is representable as a PiBound, 
    // copying it into result
    static bool extractConstant(Type::Tag newtype, ConstInst *ci, PiBound &result);
};


// PiCondition represents a range [lb, ub] (mod 2^k)
// We use PiBound for both bounds, but you can think of it as:
//    lb: Undefined->-infinity; Unknown->+infinity
//    ub: Undefined->+infinity; Unknown->-infinity
// Note that because we use modular arithmetic, it is possible to have
//    ub < lb
// in which case, the range wraps around the (mod 2^k) field.
class PiCondition {
    PiBound lb, ub;
public:
    typedef enum Type::Tag TypeTag;
    PiCondition(const PiBound &l, const PiBound &u) : lb(l), ub(u) {};
    PiCondition(const PiBound &v) : lb(v), ub(v) {};
    PiCondition(const PiCondition &other): lb(other.lb), ub(other.ub){};
    PiCondition(TypeTag typetag, int64 i) : lb(typetag, i), 
                                            ub(typetag, i) {};
    PiCondition(TypeTag typetag, Opnd *opnd) : 
        lb(typetag, VarBound(opnd)),
        ub(typetag, VarBound(opnd)) {};
    PiCondition(const PiCondition &other,
                Opnd *renameFrom,
                Opnd *renameTo) :
        lb(other.lb, renameFrom, renameTo),
        ub(other.ub, renameFrom, renameTo)
    {};
    PiCondition(const PiCondition &other,
                Opnd *renameFrom,
                const PiBound &renameTo) :
        lb(other.lb, renameFrom, renameTo),
        ub(other.ub, renameFrom, renameTo)
    {};


    const PiBound &getLb() const { return lb; };
    const PiBound &getUb() const { return ub; };

    static PiCondition lower_bound(const PiBound &l) {
        return PiCondition(l, PiBound(l.getType(), false));
    };
    static PiCondition upper_bound(const PiBound &u) {
        return PiCondition(PiBound(u.getType(), false), u);
    };
    PiCondition only_lower_bound() const {
        return PiCondition(lb, PiBound(lb.getType(), false));
    };
    PiCondition only_upper_bound() const {
        return PiCondition(PiBound(ub.getType(), false), ub);
    };
    // PiCondition add(const PiCondition &other) const;
    PiCondition neg() const;
    PiCondition add(int64 constant) const;
    PiCondition mul(int64 constant) const;
    PiCondition cast(Type::Tag newtype) const;

    bool isPositiveConstant() const;
    bool isUnknown() const { 
        return (lb.isUnknown() && ub.isUnknown()); 
    };
    void print(::std::ostream& os) const;
    TypeTag getType() const { 
        assert(lb.getType() == ub.getType()); 
        return lb.getType(); 
    };
    static PiCondition typeBounds(TypeTag dstTag, TypeTag srcTag);
    // gets bounds on defining conv of varb, which should be defined by a conv
    static PiCondition convBounds(VarBound varb);
};

extern void printPiCondition(const PiCondition *, ::std::ostream &);

class PiBoundIter {
    bool isLb;
    Inst *instr;  // set to 0 when invalid
    U_32 idx;
    PiBound current;
    MemoryManager &mm;
    AbcdReasons *why;
public:
    PiBoundIter(Inst *i,
                bool boundingBelow, 
                bool useReasons,
                MemoryManager &mm0): isLb(boundingBelow),
                                     instr(i),
                                     idx(0),
                                     current(Type::Int32, true),
                                     mm(mm0),
                                     why(0)
    { 
        init(useReasons);
        setCurrent(); 
    };

    // returns true when invalid
    bool isEmpty() { return (instr == 0); };
    
    bool operator ++() {
        ++idx;
        setCurrent();
        return !isEmpty();
    }
    PiBound getBound(AbcdReasons *why0);
    void print(::std::ostream &) const;
private:
    void setCurrent();
    void init(bool useReasons);
    Opnd *getConstantOpnd(Opnd *op);
};

typedef StlSet<PiBound> AbcdAliasesSet;
class AbcdAliases {
public:
    StlSet<PiBound> theSet;
    AbcdAliases(MemoryManager &mm) : theSet(mm) {};
};

template <typename inttype>
inline bool add_overflowed(inttype sum, inttype a, inttype b) {
    return (((a < 0) == (b < 0)) &&
            ((a < 0) != (sum < 0)));
};

template <typename inttype>
inline bool neg_overflowed(inttype nega, inttype a) {
    return (nega == a);
};

#if 0
template <typename inttype>
inline bool mul_overflowed(inttype prod, inttype a, inttype b) {
    // let's just be really conservative.
    if ((a == 0) || (b == 0)) return true;
    if (((a == -1) && (b == -b)) || ((b == -1) && (a == -a))) {
        if (prod == -prod) 
            return false;
        else
            return true;
    }
    if ((prod / y) != x)
        return true;
    else
        return false;
};
#endif

} //namespace Jitrino 

#endif // _ABCD_BOUNDS_H
