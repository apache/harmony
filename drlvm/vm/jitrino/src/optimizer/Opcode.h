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

#ifndef _OPCODE_H_
#define _OPCODE_H_

#include "Type.h"

namespace Jitrino {

//
// Runtime exceptions that can be thrown by instructions.
//
enum OpcodeExceptions {
    OverflowException,          // for signed/unsigned arithmetic instruction
    DivideByZeroException,      // for divide and remainder instructions
    NullPointerException,       // any field, element or method access
    OutOfMemoryException,       // object allocation
    StackOverflowException,     // for local memory allocation (localloc)
    CastException,              // for dynamic type casting (Op_TauCast)
    BoundsException,            // array index out of bounds
    ArrayStoreTypeException,    // stored object type incompatible with array element type
    NegativeArraySizeException, // (Java only)
};

//
// ** An instruction's opcode, modifiers, and type are encoded in a single 32-bit
// word as an Operation value; see definition of Operation below.
//

//
// The OverflowModifier is used for the Op_Add, Op_Mul, Op_Sub, and Op_Conv opcodes.
// This modifier indicates whether the instruction will throw an arithmetic overflow
// exception.
// bits 0-1
//
enum OverflowModifier {
    Overflow_None       = 0x1 << 0,    // (signed arith)
    Overflow_Signed     = 0x2 << 0,    // .ovf
    Overflow_Unsigned   = 0x3 << 0,    // .ovf.un
    Overflow_Mask       = 0x3 << 0,
    OverflowModifier_IsShiftedBy = 0,
    OverflowModifier_BitsToEncode = 2,
    OverflowModifier_BitsConsumed = 2
};

inline bool isOverflowModifierSigned(enum OverflowModifier mod)
{
    assert((Overflow_None <= mod) && (mod <= Overflow_Unsigned));
    return (mod != Overflow_Unsigned);
}

//
// The SignedModifier is used for the Op_TauDiv, Op_TauRem, Op_MulHi, and Op_Shr opcodes.
// bit 2-3
//
enum SignedModifier {
    SignedOp            = 0x1 << 2, 
    UnsignedOp          = 0x2 << 2,    // .un
    Signed_Mask         = 0x3 << 2,
    SignedModifier_IsShiftedBy = 2,    // = OverflowModifier_IsShiftedBy + OverflowModifier_BitsConsumed
    SignedModifier_BitsToEncode = 1,
    SignedModifier_BitsConsumed = 2
};

inline bool isSignedModifierSigned(enum SignedModifier mod)
{
    assert((SignedOp <= mod) && (mod <= UnsignedOp));
    return (mod != UnsignedOp);
}

//
// The ComparisonModifier is used by the OP_Cmp and Op_Branch opcodes.
// For less than and less than or equal comparisons, commute the operands and use
// the Cmp_GT, and Cmp_GTE modifiers.
// bits 4-7
//
// Suffix _Un means:
//        - if integers, main function, but treat operands as unsigned integers
//        - if floating point, main function or unordered
//
enum ComparisonModifier {
    Cmp_EQ              = 0x1 << 4, 
    Cmp_NE_Un           = 0x2 << 4,    // .un: unsigned or unordered comparison (true if either parameter is NaN)
    Cmp_GT              = 0x3 << 4, 
    Cmp_GT_Un           = 0x4 << 4,    // .un: unsigned or unordered comparison (true if either parameter is NaN)
    Cmp_GTE             = 0x5 << 4,            
    Cmp_GTE_Un          = 0x6 << 4,    // .un: unsigned or unordered comparison (true if either parameter is NaN)
    // unary boolean comparisons
    Cmp_Zero            = 0x7 << 4,    // also cmp_null
    Cmp_NonZero         = 0x8 << 4,    // also cmp_nonull
    Cmp_Mask            = 0xf << 4,
    ComparisonModifier_IsShiftedBy = 4, // = SignedModifier_IsShiftedBy + SignedModifier_BitsConsumed
    ComparisonModifier_BitsToEncode = 3,
    ComparisonModifier_BitsConsumed = 4
};

inline bool isComparisonModifierSigned(enum ComparisonModifier mod)
{
    switch (mod) {
    case Cmp_EQ: case Cmp_GT: case Cmp_GTE: return true;
    case Cmp_NE_Un: case Cmp_GT_Un: case Cmp_GTE_Un: return false;
    default:
    break;
    }
    assert(((int)SignedOp <= (int)mod) && ((int)mod <= (int)UnsignedOp));
    return ((int)mod != (int)UnsignedOp);
}

// There is a difference in semantics of variable shift instructions between CLI and Java.
// Java says that the shift amount is quantity & mask, where mask is 0x1f for 32 bit shifts,
// and 0x3f for 64 bit shifts. CLI says that this is implementation dependent. The following
// bits capture that notion:

enum ShiftMaskModifier {
    ShiftMask_None       = 0x1 << 8,
    ShiftMask_Masked     = 0x2 << 8,
    ShiftMask_Mask       = 0x3 << 8,
    ShiftMaskModifier_IsShiftedBy = 8,//= ComparisonModifier_IsShiftedBy + ShiftMaskModifier_BitsConsumed
    ShiftMaskModifier_BitsToEncode = 1,
    ShiftMaskModifier_BitsConsumed = 2
};

// Java can specify strict mode for floating-point operations
//
enum StrictModifier {
    Strict_No            = 0x01 << 10,
    Strict_Yes           = 0x02 << 10,
    Strict_Mask          = 0x03 << 10,
    StrictModifier_IsShiftedBy = 10, // = ShiftMaskModifier_IsShiftedBy + ..._BitsConsumed
    StrictModifier_BitsToEncode = 1,
    StrictModifier_BitsConsumed = 2
};

//
// Modifier for Op_DefArg
// NonNullThisArgFlag indicates that the argument is the this
// parameter and is non-null.  Used for the Java this parameter (which
// can never be null) and for CLI when the method is specialized for
// the vtable.
//
// SpecializedToExactType indicates that the parameter's type is exactly
// the type of the instruction.  This is used when a method is specialized
// for the vtable type.
//
// Note, that these are flags, so both or neither may be set.
//
// update:
//     also a modifier for Op_TauCheckNull with different semantics:
//          DefArgNoModifier <-- a common 'chknull' operation that can be eliminated
//                               by the 'hardware NPE' optimization
//          NonNullThisArg   <-- means that 'chknull' checks for 'this' operand 
//                               (of an inlined method) and cannot be eliminated
//                               by the 'hardware NPE' optimization
//          SpecializedToExactType <-- unused
//
enum DefArgModifier {
    DefArgNoModifier        = 0x1 << 12,
    NonNullThisArg          = 0x2 << 12, 
    SpecializedToExactType  = 0x3 << 12,
    DefArgBothModifiers     = 0x4 << 12,
    DefArg_Mask             = 0x7 << 12,
    DefArgModifier_IsShiftedBy = 12, // = StrictModifier_IsShiftedBy + ..._BitsConsumed
    DefArgModifier_BitsToEncode = 2,
    DefArgModifier_BitsConsumed = 3
};

enum StoreModifier {
    Store_NoWriteBarrier    = 0x1 << 15,
    Store_WriteBarrier      = 0x2 << 15,
    Store_Mask              = 0x3 << 15,
    StoreModifier_IsShiftedBy = 15, // = DefArgModifier_IsShiftedBy + ..._BitsConsumed
    StoreModifier_BitsToEncode = 1,
    StoreModifier_BitsConsumed = 2
};

enum ExceptionModifier {
    Exception_Sometimes     = 0x1 << 17,    // the default
    Exception_Always        = 0x2 << 17,
    Exception_Never         = 0x3 << 17,
    Exception_Mask          = 0x3 << 17,
    ExceptionModifier_IsShiftedBy = 17, // = StoreModifier_IsShiftedBy + ..._BitsConsumed
    ExceptionModifier_BitsToEncode = 2,
    ExceptionModifier_BitsConsumed = 2
};

enum SrcNonNullModifier {
    SrcNonNull_No          = 0x1 << 19,  // the default
    SrcNonNull_Yes         = 0x2 << 19,
    SrcNonNull_Mask        = 0x3 << 19,
    SrcNonNullModifier_IsShiftedBy = 19, // = ExceptionModifier_IsShiftedBy + ..._BitsConsumed
    SrcNonNullModifier_BitsToEncode = 1,
    SrcNonNullModifier_BitsConsumed = 2
};

enum AutoCompressModifier {
    AutoCompress_Yes         = 0x1 << 21,  // the default
    AutoCompress_No          = 0x2 << 21,
    AutoCompress_Mask        = 0x3 << 21,
    AutoCompressModifier_IsShiftedBy = 21, // = SrcNonNullModifier_IsShiftedBy + ..._BitsConsumed
    AutoCompressModifier_BitsToEncode = 1,
    AutoCompressModifier_BitsConsumed = 2
};

// If we are marking an instruction (load) as speculative, then the
// back-end should produce either an speculative instruction (IPF's ld.s)
// or enclose it in a 'catchall' region where exceptions are ignored.
//
enum SpeculativeModifier {
    Speculative_Yes          = 0x1 << 23,
    Speculative_No           = 0x2 << 23,
    Speculative_Mask         = 0x3 << 23,
    SpeculativeModifier_IsShiftedBy = 23, // = AutoCompressModifier_IsShiftedBy + ..._BitsConsumed
    SpeculativeModifier_BitsToEncode = 1,
    SpeculativeModifier_BitsConsumed = 2
};

enum ThrowModifier {
    Throw_NoModifier          = 0x1 << 25,
    Throw_CreateStackTrace    = 0x2 << 25,
    Throw_Mask                = 0x3 << 25,
    ThrowModifier_IsShiftedBy = 25, // = SpeculativeModifier_IsShiftedBy + ..._BitsConsumed
    ThrowModifier_BitsToEncode = 1,
    ThrowModifier_BitsConsumed = 2
};

enum NewModifier1 {
    NewModifier1_Value1       = 0x1 << 27,
    NewModifier1_Value2       = 0x2 << 27,
    NewModifier1_Mask         = 0x3 << 27,
    NewModifier1_IsShiftedBy = 27, // = ThrowModifier_IsShiftedBy + ..._BitsConsumed
    NewModifier1_BitsToEncode = 1,
    NewModifier1_BitsConsumed = 2
};

enum NewModifier2 {
    NewModifier2_Value1       = 0x1 << 29,
    NewModifier2_Value2       = 0x2 << 29,
    NewModifier2_Value3       = 0x3 << 29,
    NewModifier2_Mask         = 0x3 << 29,
    NewModifier2_IsShiftedBy = 29, // = NewModifier1_IsShiftedBy + ..._BitsConsumed
    NewModifier2_BitsToEncode = 2,
    NewModifier2_BitsConsumed = 2
};

enum JitHelperCallId {
    Prefetch,
    Memset0,
    InitializeArray,
    FillArrayWithConst,
    SaveThisState, //todo: replace with GetTLS + offset sequence
    ReadThisState, //todo: replace with GetTLS + offset sequence
    LockedCompareAndExchange,
    AddValueProfileValue,
    ArrayCopyDirect,
    ArrayCopyReverse,
    StringCompareTo,
    StringRegionMatches,
    StringIndexOf,
    ClassIsArray,
    ClassGetAllocationHandle,
    ClassGetTypeSize,
    ClassGetArrayElemSize,
    ClassIsInterface,
    ClassIsFinal,
    ClassGetArrayClass,
    ClassIsFinalizable,
    ClassGetFastCheckDepth
};

enum Opcode {
    // Arithmetic
    Op_Add,    Op_Mul,    Op_Sub,   // OverflowModifier, ExceptionModifier
    Op_TauDiv,    Op_TauRem,        // SignedModifier, (opnds must already be checked for 0/overflow)
    Op_Neg,
    Op_MulHi,                       // SignedModifier (but only signed needed now)
    Op_Min,   Op_Max,   Op_Abs,     // no modifiers
    // Bitwise
    Op_And,    Op_Or,    Op_Xor,
    Op_Not,
    // Selection
    Op_Select,                      // (src1 ? src2 : src3)
    // Conversion
    Op_Conv,                        // OverflowModifier, ExceptionModifier
    Op_ConvZE,                      // OverflowModifier, ExceptionModifier    
    Op_ConvUnmanaged,               // OverflowModifier, ExceptionModifier
    // Shift
    Op_Shladd,                      // no mods, 2nd operand must be LdConstant
    Op_Shl,                         // ShiftMaskModifier
    Op_Shr,                         // ShiftMaskModifier, SignedModifier
    // Comparison
    Op_Cmp,                         // ComparisonModifier
    Op_Cmp3,                        // 3-way compare, e.g.: ((s0>s1)?1:((s1>s0)?-1:0))
                                    // for floats, exactly 1 of the two comparisons is unordered; the modifier in
                                    // the instruction applies to the first test
    // Control flow
    Op_Branch,                      // ComparisonModifier
    Op_Jump,                        // (different from the CLI jmp opcode) 
    Op_Switch,
    Op_DirectCall,
    Op_TauVirtualCall,
    Op_IndirectCall,
    Op_IndirectMemoryCall,
    Op_JitHelperCall,               // call to a jit helper routine
    Op_VMHelperCall,                // call to a vm (runtime) helper routine 
    Op_Return,
    // Exception processing
    Op_Catch,
    Op_Throw, 
    Op_PseudoThrow,                 // pseudo instruction to break infinte loops
    Op_ThrowSystemException,        // takes a CompilationInterface::SystemExceptionId parameter
    Op_ThrowLinkingException,       // generate a call to Helper_Throw_LinkingException

    Op_JSR,                         

    Op_Ret,                         

    Op_SaveRet,                     
    // Move instruction 
    Op_Copy,
    Op_DefArg,                      // DefArgModifier
    // Load instructions
    Op_LdConstant,
    Op_LdRef,                       // String or reference
    Op_LdVar,    
    Op_LdVarAddr,
    Op_TauLdInd,
    Op_TauLdField,
    Op_LdStatic,
    Op_TauLdElem,
    Op_LdFieldAddr,                 // lower to ldoffset+addoffset
    Op_LdStaticAddr,
    Op_LdElemAddr,
    Op_TauLdVTableAddr,

    Op_TauLdIntfcVTableAddr,
    Op_TauLdVirtFunAddr,    
    Op_TauLdVirtFunAddrSlot,
    Op_LdFunAddr,
    Op_LdFunAddrSlot,
    // Move these to the loads
    Op_GetVTableAddr,               // obtains the address of the vtable for a particular object type
    Op_GetClassObj,
    // array manipulation
    Op_TauArrayLen,        
    Op_LdArrayBaseAddr,             // load the base (zero'th element) address of array
    Op_AddScaledIndex,              // Add a scaled index to an array element address
    // Store instructions
    Op_StVar,
    Op_TauStInd,                  // StoreModifier
    Op_TauStField,                     // StoreModifier
    Op_TauStElem,                      // StoreModifier
    Op_TauStStatic,                    
    Op_TauStRef,                       // high-level version that will make a call to the VM
    // Runtime exception check instructions
    // all of these take ExceptionModifier
    Op_TauCheckBounds,                 // takes index and array length arguments, ovf mod==none indicates 0<=idx*eltsize<2^31
    Op_TauCheckLowerBound,             // throws unless src0 <= src1
    Op_TauCheckUpperBound,             // throws unless src0 < src1
    Op_TauCheckNull,                   // throws NullPointerException if source is null
    Op_TauCheckZero,                   // for divide by zero exceptions (div and rem)
    Op_TauCheckDivOpnds, // for signed divide overflow in CLI (div/rem of MAXNEGINT, -1): generates an ArithmeticException
    Op_TauCheckElemType,               // Array element type check for aastore
    Op_TauCheckFinite,                 // throws ArithmeticException if value is NaN or +- inifinity

    // Allocation
    Op_NewObj,                      // OutOfMemoryException
    Op_NewArray,                    // OutOfMemoryException, NegativeArraySizeException
    Op_NewMultiArray,               // OutOfMemoryException, NegativeArraySizeException
    // Synchronization
    Op_TauMonitorEnter,                // (opnd must be non-null)
    // this could take an ExceptionModifier
    Op_TauMonitorExit,                 // (opnd must be non-null), IllegalMonitorStateException
    Op_TypeMonitorEnter,
    Op_TypeMonitorExit,
    // Lowered parts of MonitorEnter/Exit
    Op_LdLockAddr,                  // yields ref:int16
    Op_IncRecCount,                 // allows BalancedMonitorEnter to be used with regular MonitorExit
    Op_TauBalancedMonitorEnter,        // (opnd must be non-null), post-dominated by BalancedMonitorExit
    Op_BalancedMonitorExit,         // (cannot yield exception), dominated by BalancedMonitorEnter
    Op_TauOptimisticBalancedMonitorEnter,     // (opnd must be non-null), post-dominated by BalancedMonitorExit
    Op_OptimisticBalancedMonitorExit,      // (may yield exception), dominated by BalancedMonitorEnter
    Op_MonitorEnterFence,           // (opnd must be non-null)
    Op_MonitorExitFence,            // (opnd must be non-null)
    // type checking
    // cast takes an ExceptionModifier
    Op_TauStaticCast,                  // Compile-time assertion.  Asserts that cast is legal.
    Op_TauCast,                        // CastException (succeeds if argument is null, returns casted object)
    Op_TauAsType,                // returns casted object if argument is an instance of, null otherwise
    Op_TauInstanceOf,           // returns true if argument is a non-null instance of type T
    // type initialization
    Op_InitType,                    // initialize type before static method invocation 
                                    // or field access. 

    // Labels & markers
    Op_Label,                       // special label instructions for branch labels, finally, catch
    Op_MethodEntry,                 // method entry label
    Op_MethodEnd,                   // end of a method
    
    // Special SSA nodes
    Op_Phi,                         // merge point
    Op_TauPi,                       // leverage split based on condition 

    // Profile instrumentation instructions
    Op_IncCounter,                  // Increment a profile counter by 1
    Op_Prefetch,

    // Compressed Pointer instructions
    Op_UncompressRef,               // uncmpref = (cmpref<<s) + heapbase 
    Op_CompressRef,                 // cmpref = uncmpref - heapbase

    Op_LdFieldOffset,               // just offset
    Op_LdFieldOffsetPlusHeapbase,   // offset + heapbase
    Op_LdArrayBaseOffset,           // offset of array base
    Op_LdArrayBaseOffsetPlusHeapbase, // offset of array base
    Op_LdArrayLenOffset,              // offset of array length field
    Op_LdArrayLenOffsetPlusHeapbase,  // offset of array length field

    Op_AddOffset,                   // add uncompref+offset
    Op_AddOffsetPlusHeapbase,       // add compref+offsetPlusHeapbase (uncompressing)

    // ADDED FOR TAU:

    Op_TauPoint,
    Op_TauEdge,
    Op_TauAnd,
    Op_TauUnsafe,
    Op_TauSafe,

    Op_TauCheckCast,
    Op_TauHasType,
    Op_TauHasExactType,
    Op_TauIsNonNull,

    // prefixes: unaligned, volatile, tail,
    Op_IdentHC,
    
    NumOpcodes,       
};

class Modifier {
    U_32 value;
public:
    struct Kind {
        enum Enum {
            // these values must be disjoint bits:
            None = 0,
            Overflow = 1,
            Signed = 2,
            Comparison = 4,
            ShiftMask = 8,
            Strict = 16,
            DefArg = 32,
            SrcNonNull = 64,
            Store = 128,
            Exception = 256,
            AutoCompress = 512,
            Speculative = 1024,
            Throw = 2048,
            NewModifier1 = 4096,
            NewModifier2 = 8192,
            
            // these are combinations for convenient use in Opcode.cpp:
            Signed_and_Strict = (2 | 16),
            Overflow_and_Exception = (1 | 256),
            Overflow_and_Exception_and_Strict = (1 | 256 | 16),
            ShiftMask_and_Signed = (8 | 2),
            SrcNonNull_and_Exception = (64 | 256),

            Store_AutoCompress = (Store | AutoCompress),
            AutoCompress_Speculative = (AutoCompress | Speculative),
            Exception_and_DefArg = (Exception | DefArg)
        };
    };
    
    Modifier() : value(0) { };
    Modifier(U_32 mask, U_32 value0) : value(value0) {
        assert((mask & value) == value);
        assert(value0 != 0);
    };
    Modifier(enum OverflowModifier mod) : value((U_32)mod) { 
        assert((Overflow_None <= value) && (value <= Overflow_Unsigned));
    }
    Modifier(enum SignedModifier mod) : value((U_32)mod) { 
        assert((SignedOp <= value) && (value <= UnsignedOp));
    }
    Modifier(enum ComparisonModifier mod) : value((U_32)mod) { 
        assert((Cmp_EQ <= value) && (value <= Cmp_NonZero));
    }
    Modifier(enum ShiftMaskModifier mod) : value((U_32)mod) { 
        assert((ShiftMask_None <= value) && (value <= ShiftMask_Masked));
    }
    Modifier(enum StrictModifier mod) : value((U_32)mod) { 
        assert((Strict_No <= value) && (value <= Strict_Yes));
    }
    Modifier(enum DefArgModifier mod) : value((U_32)mod) { 
        assert((DefArgNoModifier <= value) && (value <= DefArgBothModifiers));
    }
    Modifier(enum StoreModifier mod) : value((U_32)mod) { 
        assert((Store_NoWriteBarrier <= value) && (value <= Store_WriteBarrier));
    }
    Modifier(enum ExceptionModifier mod) : value((U_32)mod) { 
        assert((Exception_Sometimes <= value) && (value <= Exception_Never));
    }
    Modifier(enum SrcNonNullModifier mod) : value((U_32)mod) { 
        assert((SrcNonNull_No <= value) && (value <= SrcNonNull_Yes));
    }
    Modifier(enum AutoCompressModifier mod) : value((U_32)mod) { 
        assert((AutoCompress_Yes <= value) && (value <= AutoCompress_No));
    }
    Modifier(enum SpeculativeModifier mod) : value((U_32)mod) {
        assert((Speculative_Yes <= value) && (value <= Speculative_No));
    }
    Modifier(enum ThrowModifier mod) : value((U_32)mod) { 
        assert((Throw_NoModifier <= value) && (value <= Throw_CreateStackTrace));
    }
    Modifier(enum NewModifier1 mod) : value((U_32)mod) { 
        assert((NewModifier1_Value1 <= value) && (value <= NewModifier1_Value2));
    }
    Modifier(enum NewModifier2 mod) : value((U_32)mod) { 
        assert((NewModifier2_Value1 <= value) && (value <= NewModifier2_Value3));
    }
    Modifier operator|(const Modifier &other) const {
        assert((value & other.value) == 0);
        U_32 newvalue = value | other.value;
        return Modifier(newvalue, newvalue);
    };
    enum OverflowModifier getOverflowModifier() const { 
        assert(hasOverflowModifier());
        return (enum OverflowModifier)(value & Overflow_Mask); 
    };
    bool hasOverflowModifier() const { return ((value & Overflow_Mask) != 0); };
    void setOverflowModifier(enum OverflowModifier newmod) {
        assert(hasOverflowModifier());
        assert((newmod & Overflow_Mask) == newmod);
        U_32 newval = value & (~Overflow_Mask);
        value = newval | newmod;
    }
    bool hasSignedModifier() const { return ((value & Signed_Mask) != 0); };
    enum SignedModifier getSignedModifier() const { 
        assert(hasSignedModifier());
        return (enum SignedModifier)(value & Signed_Mask); 
    };
    void setSignedModifier(enum SignedModifier newmod) {
        assert(hasSignedModifier());
        assert((newmod & Signed_Mask) == newmod);
        U_32 newval = value & (~Signed_Mask);
        value = newval | newmod;
    }
    bool hasComparisonModifier() const { return ((value & Cmp_Mask) != 0); };
    enum ComparisonModifier getComparisonModifier() const { 
        assert(hasComparisonModifier());
        return (enum ComparisonModifier)(value & Cmp_Mask);
    };
    void setComparisonModifier(enum ComparisonModifier newmod) {
        assert(hasComparisonModifier());
        assert((newmod & Cmp_Mask) == newmod);
        U_32 newval = value & (~Cmp_Mask);
        value = newval | newmod;
    }
    bool hasShiftMaskModifier() const { return ((value & ShiftMask_Mask) != 0); };
    enum ShiftMaskModifier getShiftMaskModifier() const { 
        assert(hasShiftMaskModifier());
        return (enum ShiftMaskModifier)(value & ShiftMask_Mask);
    };
    void setShiftMaskModifier(enum ShiftMaskModifier newmod) {
        assert(hasShiftMaskModifier());
        assert((newmod & ShiftMask_Mask) == newmod);
        U_32 newval = value & (~ShiftMask_Mask);
        value = newval | newmod;
    }
    bool hasStrictModifier() const { return ((value & Strict_Mask) != 0); };
    enum StrictModifier getStrictModifier() const { 
        assert(hasStrictModifier());
        return (enum StrictModifier)(value & Strict_Mask); 
    };
    void setStrictModifier(enum StrictModifier newmod) {
        assert(hasStrictModifier());
        assert((newmod & Strict_Mask) == newmod);
        U_32 newval = value & (~Strict_Mask);
        value = newval | newmod;
    }
    bool hasDefArgModifier() const { return ((value & DefArg_Mask) != 0); };
    enum DefArgModifier getDefArgModifier() const { 
        assert(hasDefArgModifier());
        return (enum DefArgModifier)(value & DefArg_Mask); 
    };
    void setDefArgModifier(enum DefArgModifier newmod) {
        assert(hasDefArgModifier());
        assert((newmod & DefArg_Mask) == newmod);
        U_32 newval = value & (~DefArg_Mask);
        value = newval | newmod;
    }
    bool hasSrcNonNullModifier() const { return ((value & SrcNonNull_Mask) != 0); };
    enum SrcNonNullModifier getSrcNonNullModifier() const { 
        assert(hasSrcNonNullModifier());
        return (enum SrcNonNullModifier)(value & SrcNonNull_Mask); 
    };
    void setSrcNonNullModifier(enum SrcNonNullModifier newmod) {
        assert(hasSrcNonNullModifier());
        assert((newmod & SrcNonNull_Mask) == newmod);
        U_32 newval = value & (~SrcNonNull_Mask);
        value = newval | newmod;
    }
    bool hasStoreModifier() const { return ((value & Store_Mask) != 0); };
    enum StoreModifier getStoreModifier() const { 
        assert(hasStoreModifier());
        return (enum StoreModifier)(value & Store_Mask); 
    };
    void setStoreModifier(enum StoreModifier newmod) {
        assert(hasStoreModifier());
        assert((newmod & Store_Mask) == newmod);
        U_32 newval = value & (~Store_Mask);
        value = newval | newmod;
    }
    bool hasExceptionModifier() const { return ((value & Exception_Mask) != 0); };
    enum ExceptionModifier getExceptionModifier() const { 
        assert(hasExceptionModifier());
        return (enum ExceptionModifier)(value & Exception_Mask); 
    };
    void setExceptionModifier(enum ExceptionModifier newmod) {
        assert(hasExceptionModifier());
        assert((newmod & Exception_Mask) == newmod);
        U_32 newval = value & (~Exception_Mask);
        value = newval | newmod;
    }
    bool hasAutoCompressModifier() const { return ((value & AutoCompress_Mask) != 0); };
    enum AutoCompressModifier getAutoCompressModifier() const { 
        assert(hasAutoCompressModifier());
        return (enum AutoCompressModifier)(value & AutoCompress_Mask); 
    };
    void setAutoCompressModifier(enum AutoCompressModifier newmod) {
        assert(hasAutoCompressModifier());
        assert((newmod & AutoCompress_Mask) == newmod);
        U_32 newval = value & (~AutoCompress_Mask);
        value = newval | newmod;
    }
    bool hasSpeculativeModifier() const { return ((value & Speculative_Mask) != 0); };
    enum SpeculativeModifier getSpeculativeModifier() const {
        return (enum SpeculativeModifier)(value & Speculative_Mask);
    };
    void setSpeculativeModifier(enum SpeculativeModifier newmod) {
        assert(hasSpeculativeModifier());
        U_32 newval = value & (~Speculative_Mask);
        value = newval | newmod;
    }
    bool hasThrowModifier() const { return ((value & Throw_Mask) != 0); };
    enum ThrowModifier getThrowModifier() const { 
        assert(hasThrowModifier());
        return (enum ThrowModifier)(value & Throw_Mask); 
    };
    void setThrowModifier(enum ThrowModifier newmod) {
        assert(hasThrowModifier());
        assert((newmod & Throw_Mask) == newmod);
        U_32 newval = value & (~Throw_Mask);
        value = newval | newmod;
    }
    bool hasNewModifier1() const { return ((value & NewModifier1_Mask) != 0); };
    enum NewModifier1 getNewModifier1() const { 
        assert(hasNewModifier1());
        return (enum NewModifier1)(value & NewModifier1_Mask); 
    };
    void setNewModifier1(enum NewModifier1 newmod) {
        assert(hasNewModifier1());
        assert((newmod & NewModifier1_Mask) == newmod);
        U_32 newval = value & (~NewModifier1_Mask);
        value = newval | newmod;
    }
    bool hasNewModifier2() const { return ((value & NewModifier2_Mask) != 0); };
    enum NewModifier2 getNewModifier2() const { 
        assert(hasNewModifier2());
        return (enum NewModifier2)(value & NewModifier2_Mask); 
    };
    void setNewModifier2(enum NewModifier2 newmod) {
        assert(hasNewModifier2());
        assert((newmod & NewModifier2_Mask) == newmod);
        U_32 newval = value & (~NewModifier2_Mask);
        value = newval | newmod;
    }
    bool isSigned() const { 
        if (hasSignedModifier()) return (getSignedModifier()==SignedOp);
        if (hasOverflowModifier()) {
            enum OverflowModifier omod = getOverflowModifier();
            return ((omod == Overflow_None) || (omod == Overflow_Signed));
        };
        return false;
    }

    bool isUnSigned() const { 
        return !isSigned();
    }

    bool isSignedModifierSigned() const { 
        assert(hasSignedModifier());
            return (getSignedModifier()==SignedOp);
    }

protected:
    friend class Operation;
    unsigned short encode(Opcode opcode, U_32 bits) const;

    Modifier(Opcode opcode, U_32 encoding);
    
private:
    void addEncoding(U_32 &encoded, U_32 &bitsused, U_32 mask, 
                     U_32 minval, U_32 maxval, U_32 isshiftedby,
                     U_32 bitstoencode) const;
    void addDecoding(U_32 &encoding, U_32 &bitsused, U_32 mask, 
                     U_32 minval, U_32 maxval, U_32 isshiftedby,
                     U_32 bitstoencode);
};

#define OPERATION_OPCODE_BITS 8
#define OPERATION_MODIFIER_BITS 12
#define OPERATION_TYPE_BITS 6

class Operation {
private:
    unsigned short opcode : OPERATION_OPCODE_BITS;
    unsigned short modifiers : OPERATION_MODIFIER_BITS;
    unsigned short typetag : OPERATION_TYPE_BITS;

public:
    Opcode getOpcode() const { return (enum Opcode)opcode; };
    Modifier getModifier() const { return Modifier(getOpcode(), modifiers); };
    Type::Tag getType() const { return (Type::Tag) typetag; };
    void setType(Type::Tag newType) { typetag = newType; };
    void setModifier(Modifier newmod) {
        modifiers = newmod.encode(getOpcode(), OPERATION_MODIFIER_BITS);
    }

    bool hasComparisonModifier() const {
        return getModifier().hasComparisonModifier();
    }
    enum ComparisonModifier getComparisonModifier() const {
        return getModifier().getComparisonModifier();
    };
    void setComparisonModifier(enum ComparisonModifier newMod) {
        Modifier mod = getModifier();
        mod.setComparisonModifier(newMod);
        setModifier(mod);
    };
    bool hasSignedModifier() const {
        return getModifier().hasSignedModifier();
    }
    enum SignedModifier getSignedModifier() const {
        return getModifier().getSignedModifier();
    };
    void setSignedModifier(enum SignedModifier newMod) {
        Modifier mod = getModifier();
        mod.setSignedModifier(newMod);
        setModifier(mod);
    };
    bool hasOverflowModifier() const {
        return getModifier().hasOverflowModifier();
    }
    enum OverflowModifier getOverflowModifier() const {
        return getModifier().getOverflowModifier();
    };
    void setOverflowModifier(enum OverflowModifier newMod) {
        Modifier mod = getModifier();
        mod.setOverflowModifier(newMod);
        setModifier(mod);
    };
    bool hasShiftMaskModifier() const {
        return getModifier().hasShiftMaskModifier();
    }
    enum ShiftMaskModifier getShiftMaskModifier() const {
        return getModifier().getShiftMaskModifier();
    };
    void setShiftMaskModifier(enum ShiftMaskModifier newMod) {
        Modifier mod = getModifier();
        mod.setShiftMaskModifier(newMod);
        setModifier(mod);
    };
    bool hasStrictModifier() const {
        return getModifier().hasStrictModifier();
    }
    enum StrictModifier getStrictModifier() const {
        return getModifier().getStrictModifier();
    };
    void setStrictModifier(enum StrictModifier newMod) {
        Modifier mod = getModifier();
        mod.setStrictModifier(newMod);
        setModifier(mod);
    };
    bool hasDefArgModifier() const {
        return getModifier().hasDefArgModifier();
    }
    enum DefArgModifier getDefArgModifier() const {
        return getModifier().getDefArgModifier();
    };
    void setDefArgModifier(enum DefArgModifier newMod) {
        Modifier mod = getModifier();
        mod.setDefArgModifier(newMod);
        setModifier(mod);
    };
    bool hasSrcNonNullModifier() const {
        return getModifier().hasSrcNonNullModifier();
    }
    enum SrcNonNullModifier getSrcNonNullModifier() const {
        return getModifier().getSrcNonNullModifier();
    };
    void setSrcNonNullModifier(enum SrcNonNullModifier newMod) {
        Modifier mod = getModifier();
        mod.setSrcNonNullModifier(newMod);
        setModifier(mod);
    };
    bool hasStoreModifier() const {
        return getModifier().hasStoreModifier();
    }
    enum StoreModifier getStoreModifier() const {
        return getModifier().getStoreModifier();
    };
    void setStoreModifier(enum StoreModifier newMod) {
        Modifier mod = getModifier();
        mod.setStoreModifier(newMod);
        setModifier(mod);
    };
    bool hasExceptionModifier() const {
        return getModifier().hasExceptionModifier();
    }
    enum ExceptionModifier getExceptionModifier() const {
        return getModifier().getExceptionModifier();
    };
    void setExceptionModifier(enum ExceptionModifier newMod) {
        Modifier mod = getModifier();
        mod.setExceptionModifier(newMod);
        setModifier(mod);
    };
    bool hasAutoCompressModifier() const {
        return getModifier().hasAutoCompressModifier();
    }
    enum AutoCompressModifier getAutoCompressModifier() const {
        return getModifier().getAutoCompressModifier();
    };
    void setAutoCompressModifier(enum AutoCompressModifier newMod) {
        Modifier mod = getModifier();
        mod.setAutoCompressModifier(newMod);
        setModifier(mod);
    };

    bool hasThrowModifier() const {
        return getModifier().hasThrowModifier();
    }
    enum ThrowModifier getThrowModifier() const {
        return getModifier().getThrowModifier();
    };
    void setThrowModifier(enum ThrowModifier newMod) {
        Modifier mod = getModifier();
        mod.setThrowModifier(newMod);
        setModifier(mod);
    };
    bool hasNewModifier1() const {
        return getModifier().hasNewModifier1();
    }
    enum NewModifier1 getNewModifier1() const {
        return getModifier().getNewModifier1();
    };
    void setNewModifier1(enum NewModifier1 newMod) {
        Modifier mod = getModifier();
        mod.setNewModifier1(newMod);
        setModifier(mod);
    };
    bool hasSpeculativeModifier() const {
        return getModifier().hasSpeculativeModifier();
    }
    enum SpeculativeModifier getSpeculativeModifier() const {
        return getModifier().getSpeculativeModifier();
    };
    void setSpeculativeModifier(enum SpeculativeModifier newMod) {
        Modifier mod = getModifier();
        mod.setSpeculativeModifier(newMod);
        setModifier(mod);
    };
    bool hasNewModifier2() const {
        return getModifier().hasNewModifier2();
    }
    enum NewModifier2 getNewModifier2() const {
        return getModifier().getNewModifier2();
    };
    void setNewModifier2(enum NewModifier2 newMod) {
        Modifier mod = getModifier();
        mod.setNewModifier2(newMod);
        setModifier(mod);
    };

    U_32 encodeForHashing() const { 
        if (Type::isReference((Type::Tag)typetag)) {
            return ((opcode << (OPERATION_MODIFIER_BITS+OPERATION_TYPE_BITS))
                    | (modifiers << OPERATION_TYPE_BITS)
                    | (U_32)Type::SystemObject);
        } else {
            return ((opcode << (OPERATION_MODIFIER_BITS+OPERATION_TYPE_BITS))
                    | (modifiers << OPERATION_TYPE_BITS)
                    | typetag);
        }
    }

public:
    Operation() : opcode(0), modifiers(0), typetag(0)
    {
    }
    Operation(enum Opcode opcode0) :
        opcode((unsigned short) opcode0),
        modifiers(0),
        typetag(0)
    {
        assert(((unsigned) Type::NumTypeTags) < (1<<OPERATION_TYPE_BITS));
        assert(((unsigned) opcode0) < (1<<OPERATION_OPCODE_BITS));
    };
    Operation(enum Opcode opcode0, Type::Tag typetag0) :
        opcode((unsigned short)opcode0),
        modifiers(0),
        typetag((unsigned short)typetag0)
    {
        assert(((unsigned) opcode0) < (1<<OPERATION_OPCODE_BITS));
        assert(((unsigned) typetag0) < (1<<OPERATION_TYPE_BITS));
    };
    // detect automatic argument conversions
    Operation(enum Opcode opcode0, Type::Tag typetag0,
              Modifier modifier) :
        opcode((unsigned short)opcode0), 
        modifiers(modifier.encode(opcode0, OPERATION_MODIFIER_BITS)),
        typetag((unsigned short)typetag0)
    {
        assert(((unsigned) opcode0) < (1<<OPERATION_OPCODE_BITS));
        assert(((unsigned) typetag0) < (1<<OPERATION_TYPE_BITS));
    };

    bool hasModifier(Modifier::Kind::Enum modifierKind) const;
    int getModifier(Modifier::Kind::Enum modifierKind) const;

    bool isSigned() const { 
        return getModifier().isSigned();
    }
    bool isUnSigned() const { 
        return getModifier().isUnSigned();
    }
    bool isSignedModifierSigned() const { 
        return getModifier().isSignedModifierSigned();
    }

    const char* getModifierString() const;
    const char* getOpcodeFormatString() const;
    const char* getOpcodeString() const;

    bool canThrow() const;
    bool isCheck() const;
    bool isMovable() const;
    bool isCSEable() const;
    bool isStoreOrSync() const;
    bool mustEndBlock() const;

    bool isLoad() const;

    bool isNonEssential() const;
    bool isConstant() const;

    bool operator==(const Operation &other) const {
        return ((opcode == other.opcode) && (modifiers == other.modifiers) 
                && (typetag == other.typetag));
    }
private:
    friend class Modifier;
};

} //namespace Jitrino 

#endif // _OPCODE_H_
