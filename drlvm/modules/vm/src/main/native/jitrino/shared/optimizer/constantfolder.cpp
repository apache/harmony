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

#include "constantfolder.h"
#include "Inst.h"

#define CAST(a, b) ((a)b)

#include <float.h>
#include <math.h>
#include "PlatformDependant.h"

/*
 * The constant folding optimization is described in [S.Muchnick. Advanced Compiler
 * Design and Implementation. Morgan Kaufmann, San Francisco, CA, 1997].
 */

namespace Jitrino {
#ifdef PLATFORM_POSIX

using namespace std;

inline float _chgsign(float f) {
        return copysignf(f, signbit(f) ? (float)1.0 : (float)-1.0 );
}

inline double _chgsign(double d) {
        return copysign(d, signbit(d) ? 1.0 : -1.0 );
}
#endif

// use templates here to decrease code to write for float2int below.
template <typename tointtype> tointtype minint(tointtype);
template < > inline I_32 minint<I_32>(I_32) { return 0x80000000; }
template < > inline int64 minint<int64>(int64) { 
    return __INT64_C(0x8000000000000000); 
}
template <typename tointtype> tointtype maxint(tointtype);
template < > inline I_32 maxint<I_32>(I_32) { return 0x7fffffff; }
template < > inline int64 maxint<int64>(int64) { 
    return __INT64_C(0x7fffffffffffffff); 
}
template <typename tointtype> tointtype maxuint(tointtype);
template < > inline U_32 maxuint<U_32>(U_32) { return 0xffffffff; }
template < > inline uint64 maxuint<uint64>(uint64) { 
    return __UINT64_C(0xffffffffffffffff); 
}
// get around VC++ problem with uint64->float conversions:
template <typename tointtype> double maxuintasfloat(tointtype);
template < > inline double maxuintasfloat<U_32>(U_32) { 
    return 4294967295.0;
}
template < > inline double maxuintasfloat<uint64>(uint64) { 
    return 18446744073709551615.0;
}

// do float->int conversions and make sure we do it like Java would.
template <typename tointtype, typename fromfloattype>
inline tointtype float2int(fromfloattype f)
{
    if (isnan(f)) return (tointtype) 0;
    if (finite(f) && 
        (((fromfloattype(minint<tointtype>(0))) < f) &&
         (f < fromfloattype(maxint<tointtype>(0)))))
        return (tointtype) f; // both C++ and Java truncate
    if (f < 0.0) return minint<tointtype>(0);
    return maxint<tointtype>(0);
}

template <typename tointtype, typename fromfloattype>
inline tointtype float2uint(fromfloattype s)
{
    if (isnan(s) || (s < 0.0)) return (tointtype) 0;
    if (finite(s) && (s < maxuintasfloat<tointtype>(0)))
        return (tointtype) s;
    return maxuint<tointtype>(0);
}

template <typename uinttype, typename uhalftype, int bitsdiv2>
inline uinttype mulhu(uinttype u, uinttype v)
{
    uhalftype w1, w2, w3, u0, u1, v0, v1;
    uinttype k, t;
    
    u0 = (uhalftype) u;
    u1 = (uhalftype) (u >> bitsdiv2);
    v0 = (uhalftype) v;
    v1 = (uhalftype) (v >> bitsdiv2);

    t = u0*v0;
    k = t >> bitsdiv2;

    t = u1*v0;
    w1 = (uhalftype) t;
    k = t >> bitsdiv2;
    w2 = (uhalftype) k;
    
    t = u0*v1 + w1;
    w1 = (uhalftype) t;
    k = t >> bitsdiv2;

    t = u1*v1 + w2+k;
    w2 = (uhalftype) t;
    k = t >> bitsdiv2;
    w3 = (uhalftype) k;

    uinttype r = ((uinttype)w3 << bitsdiv2) | w2;
    return r;
}

template <typename inttype, typename uinttype, int bitsdiv2>
inline inttype mulhs(inttype u, inttype v) {
    uinttype u0, v0, w0;
    inttype u1, v1, w1, w2, t;

    // e.g., 0xffff if bitsdiv2==16
    uinttype halfmask = ((uinttype)-1) >> bitsdiv2;
    u0 = u & halfmask; u1 = u >> bitsdiv2;
    v0 = v & halfmask; v1 = v >> bitsdiv2;
    w0 = u0*v0;

    t = u1*v0 + (w0 >> bitsdiv2);
    w1 = t & halfmask;
    w2 = t >> bitsdiv2;
    w1 = u0*v1 + w1;
    return u1*v1 + w2 + (w1 >> bitsdiv2);
}

bool 
ConstantFolder::isConstant(Opnd* opnd) {
    return opnd->getInst()->isConst();
}
bool 
ConstantFolder::isConstantZero(Opnd* opnd) {
    ConstInst* inst = opnd->getInst()->asConstInst();
    if (inst == NULL)
        return false;
    ConstInst::ConstValue value = inst->getValue();
    switch (inst->getType()) {
    case Type::Int8:
    case Type::UInt8:
    case Type::Int16:
    case Type::UInt16:
    case Type::Int32:
    case Type::UInt32:
        return value.i4 == 0;
    case Type::UInt64:
    case Type::Int64:
        return value.i8 == 0L;
    case Type::IntPtr:
        return value.i == 0;
    case Type::NullObject:
        return true;
    case Type::Array:
    case Type::Object:
        return value.i == 0;
    default:
    return false;
    }
}

bool 
ConstantFolder::isConstantOne(Opnd* opnd) {
    ConstInst* inst = opnd->getInst()->asConstInst();
    if (inst == NULL)
        return false;
    ConstInst::ConstValue value = inst->getValue();
    switch (inst->getType()) {
    case Type::Int32:
        return value.i4 == 1;
    case Type::Int64:
        return value.i8 == 1;
    default:
    return false;
    }
}

bool 
ConstantFolder::isConstantAllOnes(Opnd* opnd) {
    ConstInst* inst = opnd->getInst()->asConstInst();
    if (inst == NULL)
        return false;
    ConstInst::ConstValue value = inst->getValue();
    switch (inst->getType()) {
    case Type::Int32:
        return value.i4 == (I_32) -1;
    case Type::Int64:
        return value.i8 == (int64) -1;
    default:
    return false;
    }
}

bool
ConstantFolder::isConstant(Inst* inst, I_32& value) {
    ConstInst* constInst = inst->asConstInst();
    if (constInst == NULL || constInst->getType() != Type::Int32)
        return false;
    value = constInst->getValue().i4;
    return true;
}

bool
ConstantFolder::isConstant(Inst* inst, int64& value) {
    ConstInst* constInst = inst->asConstInst();
    if (constInst == NULL || constInst->getType() != Type::Int64)
        return false;
    value = constInst->getValue().i8;
    return true;
}

bool
ConstantFolder::isConstant(Inst* inst, ConstInst::ConstValue& value) {
    ConstInst* constInst = inst->asConstInst();
    if (constInst == NULL)
        return false;
    value = constInst->getValue();
    return true;
}

bool 
ConstantFolder::hasConstant(Inst* inst) {
    return (isConstant(inst->getSrc(0)) || isConstant(inst->getSrc(1)));
}

//-----------------------------------------------------------------------------
// Utilities for constant folding
//-----------------------------------------------------------------------------
bool
ConstantFolder::fold8(Opcode opc, I_8 c1, I_8 c2, I_32& result, bool is_signed) {
    switch (opc) {
    case Op_Add:    result = c1 + c2; return true;
    case Op_Sub:    result = c1 - c2; return true;
    case Op_And:    result = c1 & c2; return true;
    case Op_Or:     result = c1 | c2; return true;
    case Op_Xor:    result = c1 ^ c2; return true;
    case Op_Mul:    
        // mul, div, and rem are different for unsigned values
        if (is_signed) {
            result = c1 * c2; return true;
        } else {
            result = CAST(I_32, CAST(U_32, (CAST(U_8, c1)*CAST(U_8, c2)))); return true;
        }
    // for div and rem, be careful c2 not be 0
    // also, need to handle signed/unsigned based on SignedModifier
    case Op_TauDiv:    
        if (c2 == (I_8)0) return false;
        if (is_signed) {
            result = c1 / c2; return true;
        } else {
            result = CAST(I_32, CAST(U_32, CAST(U_8, c1) / CAST(U_8, c2))); return true;
        }
    case Op_TauRem:
        if (c2 == (I_8)0) return false;
        if (is_signed) {
            result = c1 % c2; return true;
        } else {
            result = CAST(I_32, CAST(U_32, CAST(U_8, c1) % CAST(U_8, c2))); return true;
        }
    case Op_MulHi:  result = (I_32)(I_8)((((int16)c1) * ((int16)c2)) >> 8);
        return true;
    case Op_Min:
        result = ::std::min(c1,c2); return true;
    case Op_Max:
        result = ::std::max(c1,c2); return true;
    default:
    return true;
    }
}
bool
ConstantFolder::fold16(Opcode opc, int16 c1, int16 c2, I_32& result, bool is_signed) {
    switch (opc) {
    case Op_Add:    result = c1 + c2; return true;
    case Op_Sub:    result = c1 - c2; return true;
    case Op_And:    result = c1 & c2; return true;
    case Op_Or:     result = c1 | c2; return true;
    case Op_Xor:    result = c1 ^ c2; return true;
    case Op_Mul:    
        // mul, div, and rem are different for unsigned values
        if (is_signed) {
            result = c1 * c2; return true;
        } else {
            result = CAST(I_32, CAST(U_32, CAST(uint16, c1) * CAST(uint16, c2))); return true;
        }
    // for div and rem, be careful c2 not be 0
    // also, need to handle signed/unsigned based on SignedModifier
    case Op_TauDiv:    
        if (c2 == (int16)0) return false;
        if (is_signed) {
            result = c1 / c2; return true;
        } else {
            result = CAST(I_32, CAST(U_32, CAST(uint16, c1) / CAST(uint16, c2))); return true;
        }
    case Op_TauRem:
        if (c2 == (int16)0) return false;
        if (is_signed) {
            result = c1 % c2; return true;
        } else {
            result = CAST(I_32, CAST(U_32, CAST(uint16, c1) % CAST(uint16, c2))); return true;
        }
    case Op_MulHi:  result = (I_32)((int16)(((I_32)c1) * ((I_32)c2)) >> 16);
        return true;
    case Op_Min:
        result = ::std::min(c1,c2); return true;
    case Op_Max:
        result = ::std::max(c1,c2); return true;
    default:
    return true;
    }
}

bool
ConstantFolder::fold32(Opcode opc, I_32 c1, I_32 c2, I_32& result, bool is_signed) {
    switch (opc) {
    case Op_Add:    result = c1 + c2; return true;
    case Op_Sub:    result = c1 - c2; return true;
    case Op_And:    result = c1 & c2; return true;
    case Op_Or:     result = c1 | c2; return true;
    case Op_Xor:    result = c1 ^ c2; return true;
    case Op_Mul:    
        // mul, div, and rem are different for unsigned values
        if (is_signed) {
            result = c1 * c2; return true;
        } else {
            result = CAST(I_32, CAST(U_32, CAST(U_32, c1) * CAST(U_32, c2))); return true;
        }
    // for div and rem, be careful c2 not be 0
    // also, need to handle signed/unsigned based on SignedModifier
    case Op_TauDiv:    
        if (c2 == (I_32)0) return false;
        if (is_signed) {
            if ((c1 == (I_32)0x80000000) && (c2 == -1)) {
                result = c1; 
                return true;
            }

            result = c1 / c2; return true;
        } else {
            result = CAST(I_32, CAST(U_32, CAST(U_32, c1 / CAST(U_32, c2)))); return true;
        }
    case Op_TauRem:
        if (c2 == (I_32)0) return false;
        if (is_signed) {
            if ((c1 == (I_32)0x80000000) && (c2 == -1)) {
                result = 0; 
                return true;
            }

            result = c1 % c2; return true;
        } else {
            result = CAST(I_32, CAST(U_32, CAST(U_32, c1) % CAST(U_32, c2))); return true;
        }
    case Op_MulHi:
        {
            int64 res = ((int64)c1) * ((int64)c2);
            int64 res2 = (int64)((I_32)(res >> 32));
            result = (I_32) res2;
            return true;
        }
    case Op_Min:
        result = ::std::min(c1,c2); return true;
    case Op_Max:
        result = ::std::max(c1,c2); return true;
    default:
        return false;
    }
}

bool
ConstantFolder::fold64(Opcode opc, int64 c1, int64 c2, int64& result, bool is_signed) {
    switch (opc) {
    case Op_Add:    result = c1 + c2; return true;
    case Op_Sub:    result = c1 - c2; return true;
    case Op_And:    result = c1 & c2; return true;
    case Op_Or:     result = c1 | c2; return true;
    case Op_Xor:    result = c1 ^ c2; return true;
    case Op_Mul:    
        // mul, div, and rem are different for unsigned values
        if (is_signed) {
            result = c1 * c2; 
        } else {
            result = CAST(int64, CAST(uint64, c1) * CAST(uint64, c2));
        }
        return true;
        // for div and rem, be careful c2 not be 0
    case Op_TauDiv:
        if (c2 == (int64)0) return false;
        // LONG.MIN_VALUE / -1 == LONG.MIN_VALUE
        if ((c2 == (int64)-1) && (c1 == ((int64)1<<63))) {
            result = c1;
            return true;
        }
        if (is_signed) {
            result = c1 / c2;
        } else {
            result = CAST(int64, CAST(uint64, c1) / CAST(uint64, c2));
        }
        return true;
    case Op_TauRem:
        if (c2 == (int64)0) return false;
        // LONG.MIN_VALUE % -1 == 0
        if ((c2 == (int64)-1) && (c1 == ((int64)1<<63))) {
            result = 0;
            return true;
        }
        if (is_signed) {
            result = c1 % c2;
        } else {
            result = CAST(int64, CAST(uint64, c1) % CAST(uint64, c2));
        }
        return true;
    case Op_MulHi:
        {
            if (is_signed) {
                result = mulhs<int64, uint64, 32>(c1, c2);
            } else {
                result = mulhu<uint64, U_32, 32>(CAST(uint64, c1),
                                                   CAST(uint64, c2));
            }
            return true;
        }
    case Op_Min:
        result = ::std::min(c1,c2); return true;
    case Op_Max:
        result = ::std::max(c1,c2); return true;
    default:
        return false;
    }
}

bool
ConstantFolder::foldSingle(Opcode opc, float c1, float c2, float& result) {
    switch (opc) {
    case Op_Add:    result = c1 + c2; return true;
    case Op_Sub:    result = c1 - c2; return true;
    case Op_Mul:    result = c1 * c2; return true;
    case Op_TauDiv:
        if (c2 == 0.0) return false;  
        result = c1 / c2; return true;
    case Op_Min:
    case Op_Max:
    default:
        return false;
    }
}

bool
ConstantFolder::foldDouble(Opcode opc, double c1, double c2, double& result) {
    switch (opc) {
    case Op_Add:    result = c1 + c2; return true;
    case Op_Sub:    result = c1 - c2; return true;
    case Op_Mul:    result = c1 * c2; return true;
    case Op_TauDiv:
        if (c2 == ((double)0.0)) return false;  
        result = c1 / c2; return true;
    case Op_Min:
    case Op_Max:
    default:
        return false;
    }
}

bool
ConstantFolder::fold8(Opcode opc, I_8 c, I_32& result) {
    switch (opc) {
    case Op_Not:    result = ~c; return true;
    case Op_Neg:    result = -c; return true;
    case Op_Abs:    result = (c < 0) ? -c : c; return true;
    default:
        return false;
    }
}

bool
ConstantFolder::fold16(Opcode opc, int16 c, I_32& result) {
    switch (opc) {
    case Op_Not:    result = ~c; return true;
    case Op_Neg:    result = -c; return true;
    case Op_Abs:    result = (c < 0) ? -c : c; return true;
    default:
        return false;
    }
}

bool
ConstantFolder::fold32(Opcode opc, I_32 c, I_32& result) {
    switch (opc) {
    case Op_Not:    result = ~c; return true;
    case Op_Neg:    result = -c; return true;
    case Op_Abs:    result = (c < 0) ? -c : c; return true;
    default:
        return false;
    }
}

bool
ConstantFolder::fold64(Opcode opc, int64 c, int64& result) {
    switch (opc) {
    case Op_Not:    result = ~c; return true;
    case Op_Neg:    result = -c; return true;
    case Op_Abs:    result = (c < 0) ? -c : c; return true;
    default:
        return false;
    }
}

bool
ConstantFolder::foldSingle(Opcode opc, float c, float& result) {
    if( Op_Neg == opc) {
        result = (float)_chgsign(c);
        return true;
    }
    return false;
}

bool
ConstantFolder::foldDouble(Opcode opc, double c, double& result) {
    if( Op_Neg == opc) {
        result = _chgsign(c);
        return true;
    }
    return false;
}

bool ConstantFolder::foldConv(Type::Tag fromType, Type::Tag toType, Modifier mod,
                              ConstInst::ConstValue src,
                              ConstInst::ConstValue& res)
{
    bool ovfm = (mod.getOverflowModifier()!=Overflow_None);
    switch (toType) {
    case Type::Int32:
#ifndef PONTER64
    case Type::IntPtr:
#endif
        if (Type::isInteger(fromType)) {
            if (Type::isIntegerOf4Signed(fromType)) {
                res.i4 = src.i4; return true;
            } else if (Type::isIntegerOf4Unsigned(fromType)) {
                res.i4 = src.i4; 
                if (ovfm && (src.i4 < 0)) return false;
                return true;
            } else if (Type::isIntegerOf8Signed(fromType)) {
                res.i4 = CAST(I_32, src.i8);
                if (ovfm && (src.i8 != CAST(int64, res.i4))) return false;
                return true;
            } else if (Type::isIntegerOf8Unsigned(fromType)) {
                res.i4 = CAST(I_32, CAST(uint64, src.i8));
                if (ovfm && (CAST(uint64, src.i8) != CAST(uint64, res.i4))) return false;
                return true;
            }
            assert(0);
        } else if (fromType == Type::Single) {
            res.i4 = float2int<I_32, float>(src.s); 
            if (ovfm && (src.s != CAST(float, res.i4))) return false;
            return true;
        } else if (fromType == Type::Double) {
            res.i4 = float2int<I_32, double>(src.d);
            if (ovfm && (src.d != CAST(double, res.i4))) return false;
            return true;
        }
        break;
    case Type::UInt32:
#ifndef PONTER64
    case Type::UIntPtr:
#endif
        if (Type::isInteger(fromType)) {
            if (Type::isIntegerOf4Bytes(fromType)) {
                res.i4 = src.i4; return true;
            } else if (Type::isIntegerOf8Signed(fromType)) {
                res.i4 = CAST(I_32, CAST(U_32, src.i8));
                if (ovfm && (src.i8 != CAST(int64, CAST(U_32, res.i4)))) return false;
                return true;
            } else if (Type::isIntegerOf8Unsigned(fromType)) {
                res.i4 = CAST(I_32, CAST(U_32, CAST(uint64, src.i8)));
                if (ovfm && (CAST(uint64, src.i8) != CAST(uint64, CAST(U_32, res.i4)))) return false;
                return true;
            }
            assert(0);
        } else if (fromType == Type::Single) {
            res.i4 = float2uint<U_32, float>(src.s);
            if (ovfm && (src.s != CAST(float, CAST(U_32, res.i4)))) return false;
            return true;
        } else if (fromType == Type::Double) {
            res.i4 = float2uint<U_32, double>(src.d);
            if (ovfm && (src.d != CAST(double, CAST(U_32, res.i4)))) return false;
            return true;
        }
        break;
    case Type::Int64:
#ifdef PONTER64
    case Type::IntPtr:
#endif
        assert(!ovfm || mod.getOverflowModifier()==Overflow_Signed);
        if (Type::isInteger(fromType)) {
            if (Type::isIntegerOf4Signed(fromType)) {
                res.i8 = src.i4; return true;
            } else if (Type::isIntegerOf4Unsigned(fromType)) {
                res.i8 = CAST(U_32, src.i4); 
                if (ovfm && (src.i4 < 0)) return false;
                return true;
            } else if (Type::isIntegerOf8Signed(fromType)) {
                res.i8 = src.i8; return true;
            } else if (Type::isIntegerOf8Unsigned(fromType)) {
                res.i8 = src.i8; 
                if (ovfm && (src.i8 < 0)) return false;
                return true;
            }
            assert(0);
        } else if (fromType == Type::Single) {
            res.i8 = float2int<int64, float>(src.s); 
            if (ovfm && (src.s != CAST(float, res.i8))) return false;
            return true;
        } else if (fromType == Type::Double) {
            res.i8 = float2int<int64, double>(src.d);
            if (ovfm && (src.d != CAST(double, res.i8))) return false;
            return true;
        }
        break;
    case Type::UInt64:
#ifdef PONTER64
    case Type::UIntPtr:
#endif
        assert(!ovfm || mod.getOverflowModifier()==Overflow_Unsigned);
        if (Type::isInteger(fromType)) {
            if (Type::isIntegerOf4Signed(fromType)) {
                res.i8 = CAST(uint64, CAST(U_32, src.i4));
                if (ovfm && (src.i4 < 0)) return false;
                return true;
            } else if (Type::isIntegerOf4Unsigned(fromType)) {
                res.i8 = CAST(uint64, CAST(U_32, src.i4)); return true;
            } else if (Type::isIntegerOf8Bytes(fromType)) {
                res.i8 = src.i8; return true;
            }
        } else if (fromType == Type::Single) {
            res.i8 = float2uint<uint64, float>(src.s);
            if (ovfm) return false; 
            return true;
        } else if (fromType == Type::Double) {
            return false;
        }
        break;
    case Type::Single:
        switch (fromType) {
        case Type::Int32:
            res.s = CAST(float, src.i4); return true;
        case Type::UInt32:
            res.s = CAST(float, CAST(U_32, src.i4)); return true;
        case Type::Int64:
            res.s = CAST(float, src.i8);
            if (ovfm && (src.i8 != float2int<int64, float>(res.s))) return false;
            return true;
        case Type::UInt64:
            return false; 
        case Type::Single:
            res.s = src.s; return true;
        case Type::Double:
            res.s = CAST(float, src.d);
            if (ovfm && (src.d != CAST(double, res.s))) return false;
            return true;
        case Type::Float: 
        default:
            break;
        }
        break;
    case Type::Double:
        switch (fromType) {
        case Type::Int32:
            res.d = CAST(double, src.i4); return true;
        case Type::UInt32:
            res.d = CAST(double, CAST(U_32, src.i4)); return true;
        case Type::Int64:
            res.d = CAST(double, src.i8); return true;
        case Type::UInt64:
            return false; 
        case Type::Single:
            res.d = CAST(double, src.s); return true;
        case Type::Double:
            res.d = src.d; return true;
        case Type::Float: 
        default:
            break;
        }
        break;
    case Type::UnmanagedPtr:
        // Let's accept the conversion from properly sized integer
#ifdef PONTER64
        if (Type::isIntegerOf8Bytes(fromType)) {
#else
        if (Type::isIntegerOf4Bytes(fromType)) {
#endif
            return true;
        }
        break;
    default:
        break;
    }
    return false;
}

bool
ConstantFolder::foldCmp32(ComparisonModifier mod, I_32 c1, I_32 c2, I_32& result) {
    switch (mod) {
    case Cmp_EQ:    result = ((c1 == c2)?1:0);                  return true;
    case Cmp_NE_Un: result = ((c1 != c2)?1:0);                  return true;
    case Cmp_GT:    result = ((c1 > c2)?1:0);                   return true;
    case Cmp_GT_Un: result = ((CAST(U_32, c1) > CAST(U_32, c2))?1:0);   return true;
    case Cmp_GTE:   result = ((c1 >= c2)?1:0);                  return true;
    case Cmp_GTE_Un:result = ((CAST(U_32, c1) >= CAST(U_32, c2))?1:0);  return true;
    default: break;
    }
    return false;
}

bool
ConstantFolder::foldCmpRef(ComparisonModifier mod, void* c1, void* c2, I_32& result) {
    switch (mod) {
    case Cmp_EQ:    result = ((c1 == c2)?1:0);                  return true;
    case Cmp_NE_Un: result = ((c1 != c2)?1:0);                  return true;
    case Cmp_GT:    result = ((c1 > c2)?1:0);                   return true;
    case Cmp_GT_Un: result = ((c1 > c2)?1:0);                   return true;
    case Cmp_GTE:   result = ((c1 >= c2)?1:0);                  return true;
    case Cmp_GTE_Un:result = ((c1 >= c2)?1:0);                  return true;
    default: break;
    }
    return false;
}

bool
ConstantFolder::foldCmp64(ComparisonModifier mod, int64 c1, int64 c2, I_32& result) {
    switch (mod) {
    case Cmp_EQ:    result = ((c1 == c2)?1:0);                  return true;
    case Cmp_NE_Un: result = ((c1 != c2)?1:0);                  return true;
    case Cmp_GT:    result = ((c1 > c2)?1:0);                   return true;
    case Cmp_GT_Un: result = ((CAST(uint64, c1) > CAST(uint64, c2))?1:0);   return true;
    case Cmp_GTE:   result = ((c1 >= c2)?1:0);                  return true;
    case Cmp_GTE_Un:result = ((CAST(uint64, c1) >= CAST(uint64, c2))?1:0);  return true;
    default: break;
    }
    return false;
}

bool
ConstantFolder::foldCmpSingle(ComparisonModifier mod, float c1, float c2, I_32& result) {
    switch (mod) {
    case Cmp_EQ:
        if (isnan(c1) || isnan(c2)) { result = false; }
        else { result = ((c1 == c2)?1:0); }
        return true;
    case Cmp_NE_Un: 
        if (isnan(c1) || isnan(c2)) { result = true; }
        else { result = ((c1 != c2)?1:0); }
        return true;
    case Cmp_GT:    
    case Cmp_GT_Un: 
        if (isnan(c1) || isnan(c2)) { result = (mod == Cmp_GT_Un); }
        else { result = ((c1 > c2)?1:0); }
        return true;
    case Cmp_GTE:
    case Cmp_GTE_Un:
        if (isnan(c1) || isnan(c2)) { result = (mod == Cmp_GTE_Un); }
        else { result = ((c1 >= c2)?1:0); }
        return true;
    default: break;
    }
    return false;
}

bool
ConstantFolder::foldCmpDouble(ComparisonModifier mod, double c1, double c2, I_32& result){
    switch (mod) {
    case Cmp_EQ:
        if (isnan(c1) || isnan(c2)) { result = false; }
        else { result = ((c1 == c2)?1:0); }
        return true;
    case Cmp_NE_Un: 
        if (isnan(c1) || isnan(c2)) { result = true; }
        else { result = ((c1 != c2)?1:0); }
        return true;
    case Cmp_GT:    
    case Cmp_GT_Un: 
        if (isnan(c1) || isnan(c2)) { result = (mod == Cmp_GT_Un); }
        else { result = ((c1 > c2)?1:0); }
        return true;
    case Cmp_GTE:
    case Cmp_GTE_Un:
        if (isnan(c1) || isnan(c2)) { result = (mod == Cmp_GTE_Un); }
        else { result = ((c1 >= c2)?1:0); }
        return true;
    default: break;
    }
    return false;
}

bool
ConstantFolder::foldCmp32(ComparisonModifier mod, I_32 c, I_32& result) {
    switch (mod) {
    case Cmp_Zero:      result = ((c == 0)?1:0);    return true;
    case Cmp_NonZero:   result = ((c != 0)?1:0);    return true;
    default: break;
    }
    return false;
}

bool
ConstantFolder::foldCmp64(ComparisonModifier mod, int64 c, I_32& result) {
    switch (mod) {
    case Cmp_Zero:      result = ((c == 0)?1:0);    return true;
    case Cmp_NonZero:   result = ((c != 0)?1:0);    return true;
    default: break;
    }
    return false;
}

bool
ConstantFolder::foldCmpRef(ComparisonModifier mod, void* c, I_32& result) {
    switch (mod) {
    case Cmp_Zero:      result = ((c == NULL)?1:0);    return true;
    case Cmp_NonZero:   result = ((c != NULL)?1:0);    return true;
    default: break;
    }
    return false;
}

bool
ConstantFolder::foldCmp(Type::Tag cmpTypeTag,
                        ComparisonModifier mod,
                        ConstInst::ConstValue val,
                        ConstInst::ConstValue& result) {
    switch (cmpTypeTag) {
    case Type::Int32:   return foldCmp32(mod, val.i4, result.i4);
    case Type::Int64:   return foldCmp64(mod, val.i8, result.i4);
    case Type::UInt32:  return foldCmp32(mod, val.i4, result.i4);
    case Type::UInt64:  return foldCmp64(mod, val.i8, result.i4);
    default: break;
    }
    // fold comparisons of references against null
    if (Type::isObject(cmpTypeTag))
        return foldCmpRef(mod, val.i, result.i4);
    return false;
}
//
// binary comparison
//
bool
ConstantFolder::foldCmp(Type::Tag cmpTypeTag,
                        ComparisonModifier mod,
                        ConstInst::ConstValue val1,
                        ConstInst::ConstValue val2,
                        ConstInst::ConstValue& result) {
    switch (cmpTypeTag) {
    case Type::Int32: case Type::UInt32: return foldCmp32(mod, val1.i4, val2.i4, result.i4);
    case Type::Int64: case Type::UInt64: return foldCmp64(mod, val1.i8, val2.i8, result.i4);
    case Type::Single:  return foldCmpSingle(mod, val1.s, val2.s, result.i4);
    case Type::Double:  return foldCmpDouble(mod, val1.d, val2.d, result.i4);
    default: break;
    }
    return false;
}



bool
ConstantFolder::foldConstant(Type::Tag type,
                             Opcode opc,
                             ConstInst::ConstValue val1,
                             ConstInst::ConstValue val2,
                             ConstInst::ConstValue& result,
                             bool is_signed) {
    
    switch (type) {
    case Type::Int8:
    case Type::UInt8:  return fold8(opc, (I_8)val1.i4, (I_8)val2.i4, result.i4, is_signed);
    case Type::Int16:
    case Type::UInt16: return fold16(opc, (int16)val1.i4, (int16)val2.i4, result.i4, is_signed);
    case Type::Int32:   
    case Type::UInt32: return fold32(opc, val1.i4, val2.i4, result.i4, is_signed);
    case Type::Int64:
    case Type::UInt64: return fold64(opc, val1.i8, val2.i8, result.i8, is_signed);
    case Type::IntPtr:
    case Type::UIntPtr: {
        int psi = sizeof(POINTER_SIZE_INT);
        switch (psi) {
        case 1: return fold8(opc, (I_8)val1.i4, (I_8)val2.i4, result.i4, is_signed);
        case 2: return fold16(opc, (int16)val1.i4, (int16)val2.i4, result.i4, is_signed);
        case 4: return fold32(opc, val1.i4, val2.i4, result.i4, is_signed);
        case 8: return fold64(opc, val1.i8, val2.i8, result.i8, is_signed);
        default: return false;
        }
    }
    case Type::Single: return foldSingle(opc, val1.s, val2.s, result.s);
    case Type::Double: return foldDouble(opc, val1.d, val2.d, result.d);
    default: return false;
    }
}

bool
ConstantFolder::foldConstant(Type::Tag type,
                             Opcode opc,
                             ConstInst::ConstValue val,
                             ConstInst::ConstValue& result) {
    switch (type) {
    case Type::Int32: 
    case Type::UInt32: 
        return fold32(opc, val.i4, result.i4);
    case Type::Int64: 
    case Type::UInt64: 
        return fold64(opc, val.i8, result.i8);
    case Type::Single: 
        return foldSingle(opc, val.s, result.s);
    case Type::Double: 
        return foldDouble(opc, val.d, result.d);
    default: break;
    }
    return false;
}
//
// unary comparison
//

//
// Tries to constant fold the instruction, setting the resulting constant
// value to result.  Returns true if instruction was folded.
//
bool
ConstantFolder::fold(Inst* inst, ConstInst::ConstValue& result) {
    U_32 numSrcs = inst->getNumSrcOperands();
    if (numSrcs == 0)
        return false;
    ConstInst* constSrc0 = inst->getSrc(0)->getInst()->asConstInst();
    if (constSrc0 == NULL)
        return false;
    Opcode opc = inst->getOpcode();
    if (numSrcs == 1) {
        if (opc == Op_Cmp) { 
            return foldCmp(constSrc0->getType(),
                           inst->getComparisonModifier(),
                           constSrc0->getValue(),
                           result);
        }
        Modifier mod = inst->getModifier();
        return foldConstant(inst->getType(),
                            inst->getOpcode(),
                            constSrc0->getValue(),
                            result);
    }
    ConstInst* constSrc1 = inst->getSrc(1)->getInst()->asConstInst();
    if (constSrc1 == NULL)
        return false;
    if (numSrcs == 2) {
        if (opc == Op_Cmp) { 
            assert(constSrc0->getType() == constSrc1->getType());
            return foldCmp(inst->getType(),
                           inst->getComparisonModifier(),
                           constSrc0->getValue(),
                           constSrc1->getValue(),
                           result);
        }
        Modifier mod = inst->getModifier();
        bool is_signed = mod.isSigned();
        return foldConstant(inst->getType(),
                            inst->getOpcode(),
                            constSrc0->getValue(),
                            constSrc1->getValue(),
                            result,
                            is_signed);
    }
    return false;
}


} //namespace Jitrino 
