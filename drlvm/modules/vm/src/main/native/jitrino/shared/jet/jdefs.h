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
 * @author Alexander Astapchuk
 */
 
/**
 * @file
 * @brief Common definitions and constants used across the Jitrino.JET.
 */
 
#if !defined(__JDEFS_H_INCLUDED__)
#define __JDEFS_H_INCLUDED__

#include "open/types.h"
#include "open/bytecodes.h"
#include <assert.h>
#include <climits>
#include <string.h>

//
// This file is normally included (explicitly or not) in all .jet files,
// so here are some project-wide definitions.
//

// PROJECT_JET (standalone version of Jitrino.JET) implies JET_PROTO. 
#if defined(PROJECT_JET) && !defined(JET_PROTO)
    #define JET_PROTO 1
#endif

// _DEBUG also implies JET_PROTO. 
#if defined(_DEBUG) && !defined(JET_PROTO)
    #define JET_PROTO 1
#endif

/**
 * JET_PROTO - turns on debugging, tracing and experimental features that 
 * are normally out of production build.
 *
 * JET_PROTO at least implies logging (JIT_LOGS), statistics collection 
 * (JIT_STATS) and various tracings (JIT_TRACE - XXX used ?).
 */
#ifdef JET_PROTO
    #if !defined(JIT_LOGS)
        #define JIT_LOGS
    #endif
    #if !defined(JIT_STATS)
        #define JIT_STATS
    #endif
    #if !defined(JIT_TRACE)
        #define JIT_TRACE
    #endif
#endif

#ifdef _WIN32
    #define stdcall__
    /**
     * @brief Defines int64 constant.
     */
    #define MK_I64(a)   ((jlong)(a ## L))
    #define snprintf    _snprintf
    #if _MSC_VER < 1500
        #define vsnprintf    _vsnprintf
    #endif
    #ifndef strcasecmp
        #ifdef _MSC_VER
            #define strcasecmp  _stricmp
        #else
            #define strcasecmp  stricmp
        #endif
    #endif
#else
    // stdcall has no meaning on platforms other than Lin32
    #undef stdcall__
    #if defined(_IA32_) && !defined(stdcall__)
        #define stdcall__    __attribute__ ((__stdcall__))
    #else
    #define stdcall__
    #endif
    #define __stdcall
    #define MK_I64(a)   ((jlong)(a ## LL))
#endif
//
// gcc def on EM64T
#if defined(__x86_64__) && !defined(_EM64T_)
    #define _EM64T_  1
    #undef  _IA32_
#endif

#if defined(__i386__) && !defined(_IA32_)
    #undef  _EM64T_
    #define _IA32_ 1
#endif

#if !defined(_EM64T_) && !defined(_IPF_) && !defined(_IA32_)
    // presuming we're working on ia-32
    #define _IA32_ 1
#endif


/**
 * @brief Number of elements in array.
 */
#define COUNTOF(a) (sizeof(a)/sizeof(a[0]))

namespace Jitrino {
namespace Jet {

// Nothing (?) portable is defined by lib.c.limits for 64bit integers, thus 
// declaring our own.

/**
 * @brief Represents a Java's long aka 'signed int64'.
 */
typedef int64   jlong;

/**
 * @brief A maximum, positive value a #jlong can have.
 */
#define jLONG_MAX   MK_I64(0x7FFFFFFFFFFFFFFF)

/**
 * @brief A minimum, negative value a #jlong can have.
 */
#define jLONG_MIN   MK_I64(0x8000000000000000)

/**
 * @brief Empty value.
 *
 * Normally used when zero is not applicable to signal empty/non-initialized
 * value i.e. for PC values.
 */
#define NOTHING                   (~(unsigned)0)

/**
 * @brief Tests whether the value fits into 8 bits.
 */
inline bool fits_i8(int val)
{
    return (CHAR_MIN <= val && val <= CHAR_MAX);
}

/**
 * @brief Tests whether the value fits into 16 bits.
 */
inline bool fits_i16(int val)
{
    return (SHRT_MIN <= val && val <= SHRT_MAX);
}

/**
 * @brief Extracts lower 32 bits from the given 64 bits value.
 */
inline int lo32(jlong jl) { return (int)(jl & 0xFFFFFFFF); };

/**
 * @brief Extracts higher 32 bits from the given 64 bits value.
 */
inline int hi32(jlong jl) { return (int)(jl>>32); };

/**
 * @brief Composes a 64bit value from 2 32 bit values.
 */
inline jlong mk_i64(int hi, int lo) { return ((jlong)hi)<<32 | lo; };

/*
 * @brief Size of the platform's machine word, in bits.
 */
const unsigned WORD_SIZE = sizeof(unsigned)*CHAR_BIT;

/**
 * @brief Returns word index for a given index in bit array.
 */
inline unsigned word_no(unsigned idx)
{
    return idx/WORD_SIZE;
}

/**
 * @brief Returns bit index in a word for the given index in a bit array.
 */
inline unsigned bit_no(unsigned idx)
{
    return idx%WORD_SIZE;
}
/**
 * @brief Returns number of words needed to store the given number of bits.
 */
#define words(num)  ((num+WORD_SIZE-1)/WORD_SIZE)

/**
 * @brief Sets a bit in the bit array at the specified position.
 * @param p - pointer the the bit array
 * @param idx - index of the bit
 */
inline void set(unsigned * p, unsigned idx)
{
    p[ word_no(idx) ] |= 1<<bit_no(idx);
}

/**
 * @brief Clears a bit in the bit array at the specified position.
 * @param p - pointer the the bit array
 * @param idx - index of the bit
 */
inline void clr(unsigned * p, unsigned idx)
{
    p[ word_no(idx) ] &= ~(1<<bit_no(idx));
}

/**
 * @brief Tests a bit in the provided bit array.
 * @param p - pointer the the bit array
 * @param idx - index of the bit
 * @return \b true if the bit set, \b false otherwise
 */
inline bool tst(const unsigned * p, unsigned idx)
{
    return 0 != (p[word_no(idx)] & (1<<bit_no(idx)));
}

/**
 * @brief Converts string to bool.
 *
 * The following strings (case-insensitive) are considered as \c true:
 * on, true, t, yes, y. Any other means \c false.
 */
inline bool to_bool(const char * val)
{
    return  !strcasecmp(val, "on") ||
            !strcasecmp(val, "yes") || !strcasecmp(val, "y") ||
            !strcasecmp(val, "true") || !strcasecmp(val, "t");
}


/**
 * @defgroup JMF_ Compilation control flags
 *
 * A bunch of flags, a Java method may be compiled with. Some of them also
 * affect runtime of the method.
 * 
 * Various java method flags used during compilation and some of them are 
 * also used at runtime.
 *
 * JMF_ stands for Java method's flag.
 */
 
/// @{

/** @brief Method reports 'this' during root set enumeration.*/
#define JMF_REPORT_THIS     (0x00000001)

/** @brief Generate code to perform a GC polling on back branches.*/
#define JMF_BBPOLLING       (0x00000002)

/** @brief Generate profiling code for back branches and method entry.*/
#define JMF_PROF_ENTRY_BE   (0x00000004)

/**
 * @brief Generate code so back branches and method entry counters get
 *        checked synchronously, during runtime, at method entrance. 
 */
#define JMF_PROF_SYNC_CHECK (0x00000008)

#ifdef JET_PROTO
#define JMF_ALIGN_STACK     (0x00010000)
#define JMF_SP_FRAME        (0x00020000)
#define JMF_STATIC_GC_MAP   (0x00040000)
#else
/** 
 * @brief Aligns stack
 * @note Experimental feature, not for production build.
 */
#define JMF_ALIGN_STACK     (0) 
/** 
 * @brief Use sp-based stack frame instead of bp-based.
 * @note Experimental feature, not for production build.
 */
#define JMF_SP_FRAME        (0)
/** 
 * @brief Use static (computed at compile-time) GC-map for operand stack, 
 * rather than dynamic (updated at runtime).
 * @note Experimental feature, not for production build.
 */
#define JMF_STATIC_GC_MAP   (0)
#endif

/**
 * @defgroup DBG_ Debugging flags
 *
 * These flags are also 'Java method's flags' but used for the debugging 
 * purposes only.
 *
 * Equal to zero in release mode, so a test against it (xx & DBG_ ) 
 * effectively leads to zero at compile time and thus the debug tracing code
 * is removed by optimizing compiler.
 *
 * These flags may be turned on in release mode if \b JIT_LOGS macro defined.
 *
 * @{
 */
// The latest PMF/Log are working well without noticeable overhead, 
// may have the tracing functionality turned on always.
#if 1 //defined(JIT_LOGS) || defined(JET_PROTO)
    #define DBG_BRK             (0x00100000)
    #define DBG_TRACE_EE        (0x00200000)
    #define DBG_TRACE_BC        (0x00400000)
    #define DBG_TRACE_RT        (0x00800000)
    #define DBG_DUMP_BBS        (0x01000000)
    #define DBG_TRACE_CG        (0x02000000)
    #define DBG_TRACE_LAYOUT    (0x04000000)
    #define DBG_DUMP_CODE       (0x08000000)
    #define DBG_TRACE_SUMM      (0x10000000)
    #define DBG_CHECK_STACK     (0x20000000)
#else
    /** @brief Break at method's entry.*/
    #define DBG_BRK             (0x00000000)
    /** 
     * @brief Trace method's enter/exit.
     * 
     * Turns on tracing of input args and return value for a method.
     * Also turns on tracing of values returned from a method or a helper
     * call.
     * @see CodeGen::gen_save_ret
     */
    #define DBG_TRACE_EE        (0x00000000)
    /** @brief Trace execution of each bytecode instruction. */
    #define DBG_TRACE_BC        (0x00000000)
    /** 
     * @brief Trace runtime support events - stack unwinding, root set 
     * enumeration, byte code <-> native code mapping, etc.
     */
    #define DBG_TRACE_RT        (0x00000000)
    /** @brief Dump basic blocks, before code generation phase.*/
    #define DBG_DUMP_BBS        (0x00000000)
    /** @brief Trace code generation.*/
    #define DBG_TRACE_CG        (0x00000000)
    /** @brief Trace code layout (address ranges).*/
    #define DBG_TRACE_LAYOUT    (0x00000000)
    /** @brief Dump whole code after it's done.*/
    #define DBG_DUMP_CODE       (0x00000000)
    /** @brief Trace short summary about compiled method.*/
    #define DBG_TRACE_SUMM      (0x00000000)
    /** @brief Generates code to ensure stack integrity.*/
    #define DBG_CHECK_STACK     (0x00000000)
#endif
/// @{  //~DBG_

/// @}  //~JMF_


/**
 * Enum which describes a kind/group of bytecode instruction, according to 
 * the JVM Spec.
 */
enum InstrKind  {
    /// arithmetics
    ik_a,
    /// control transfer
    ik_cf,
    /// type conversion
    ik_cnv,
    /// load/store
    ik_ls,
    /// method invocation and return
    ik_meth,
    /// object creation and manipulation
    ik_obj,
    /// stack management
    ik_stack,
    /// throwing exceptions
    ik_throw,
    /// used for other opcodes like 'nop', 'wide' and 'unused'
    ik_none

};

/**
 * @defgroup OPF_ Opcode flags - various traits of byte code instructions.
 * @{
 */

/**
 * @brief (OPF stands for OPcode Flag) No special flags for the given opcode.
 */
#define OPF_NONE        (0x00000000)

/** 
 * Opcode ends basic block (ATHROW/GOTOs/conditional branch, etc).
 */
#define OPF_ENDS_BB     (0x00001000)
/** 
 * Opcode has no fall through pass (ATHROW/GOTO/RETURN/etc).
 */
#define OPF_DEAD_END    (0x00002000)
/**
 * An instruction is one of return opcodes.
 */
#define OPF_RETURN      (0x00004000)
/**
 * An instruction starts a basic block.
 *
 * @note This is not a trait of an opcode, but rather of an instruction on 
 *       a particular control flow. It's placed into OPF_ section as it's 
 *       stored in the same field of JInst.
 */
#define OPF_STARTS_BB   (0x00008000)
/**
 * Instruction uses or defines local variable #0.
 */
#define OPF_VAR0            (0x00000000)
/**
 * Instruction uses or defines local variable #1.
 */
#define OPF_VAR1            (0x00000001)
/**
 * Instruction uses or defines local variable #2.
 */
#define OPF_VAR2            (0x00000002)
/**
 * Instruction uses or defines local variable #3.
 */
#define OPF_VAR3            (0x00000003)
/**
 * Instruction uses or defines local variable whose index defined by first
 * instruction operand (JInst::op0).
 */
#define OPF_VAR_OP0         (0x00000004)
/**
 * Mask used to extract the OPF_VAR_ index.
 */
#define OPF_VAR_IDX_MASK    (0x0000000F)

/**
 * Mask used to extract def-use info from opcode flags.
 */
#define OPF_VAR_DU_MASK     (0x00000300)
/**
 * If set, then opcode defines a local variable an index given by its first
 * operand (JInst::op0).
 */
#define OPF_VAR_DEF         (0x00000100)
/**
 * If set, then opcode uses a local variable an index given by its first
 * operand (JInst::op0).
 */
#define OPF_VAR_USE         (0x00000200)
/**
 * Mask used to extract from opcode flags a type of operation performed by 
 * the opcode.
 */
#define OPF_VAR_TYPE_MASK   (0x000000F0)
#define OPF_VAR_TYPE_SHIFT  (4)
/**
 * Instruction operates with #i32 (or lesser) type.
 */
#define OPF_VAR_TYPE_I32    (i32<<OPF_VAR_TYPE_SHIFT)
/**
 * Instruction operates with #jobj type.
 */
#define OPF_VAR_TYPE_OBJ    (jobj<<OPF_VAR_TYPE_SHIFT)
/**
 * Instruction operates with #i64 type.
 */
#define OPF_VAR_TYPE_I64    (i64<<OPF_VAR_TYPE_SHIFT)
/**
 * Instruction operates with #flt32 type.
 */
#define OPF_VAR_TYPE_FLT    (flt32<<OPF_VAR_TYPE_SHIFT)
/**
 * Instruction operates with #dbl64 type.
 */
#define OPF_VAR_TYPE_DBL    (dbl64<<OPF_VAR_TYPE_SHIFT)

/// @} // ~OPF_

/**
 * @brief An info associated with an bytecode instruction.
 */
struct InstrDesc  {
    /**
     * @brief A kind of instruction. Used to groups processing of similar 
     *        instructions into same function.
     */
    InstrKind       ik;
#ifdef _DEBUG
    /**
     * @brief A byte code value. Only used internally in DEBUG mode to make 
     *        sure the \link #instrs array \endlink arranged properly.
     */
    JavaByteCodes   opcode;
#endif
    /**
     * @brief Total length of the instruction including additional bytes 
     *        (if any). 0 for 'wide' and for variable-length instructions.
     */
    unsigned        len;
    /**
     * @brief Various characteristics of the given opcode - see OPF_ flags.
     */
    unsigned        flags;
    /**
     * @brief Printable name of the opcode.
     */
    const char *    name;
    char            adding[32-20];
};

extern const InstrDesc instrs[OPCODE_COUNT];

/**
 * @brief Enumerates possible Java types
 *
 * The values are ordered by complexity ascending.
 * The following is intentionally \b true: <code>i8<i16<u16<i32<i64</code>.
 */
enum jtype {
    /// signed 8 bits integer - Java's \c boolean or \c byte
    i8,
    /// signed 16-bits integer - Java's \c short
    i16,
    /// unsigned 16-bit integer - Java's \c char
    u16,
    /// signed 32 bit integer - Java's \c int
    i32,
    /// signed 64 bit integer - Java's \c long
    i64,
    /// single-precision 32 bit float - Java's \c float
    flt32,
    /// double-precision 64 bit float - Java's \c double
    dbl64,
    /// any object type
    jobj,
    /// void. no more, no less
    jvoid,
    /// jretAddr - a very special type for JSR things
    jretAddr,
    /// max number of types
    jtypes_count, 
    /// max number of types
    num_jtypes = jtypes_count,
#ifdef _EM64T_
    iplatf=i64,
#else
    /// platform-native size for integer (fits into general-purpose register)
    iplatf=i32, 
#endif
};

/// Info associated with #jtype.
struct JTypeDesc {
    /// jtype itself
    jtype        jt;
    /**
     * size in bytes of the type on current platform.
     * @note: for #jobj, the size of uncompressed reference is specified.
     */
    unsigned     size;
    /** 
     * offset, in bytes, of first item in an array of items of the type
     */
    unsigned     rt_offset;
    /// human-readable name of the type 
    const char * name;
};

/**
 * @brief Info about all #jtype types.
 */
extern JTypeDesc jtypes[num_jtypes];

/**
 *@brief Tests whether specified #jtype represents floating point value.
 */
inline bool is_f( jtype jt )
{
    return jt==dbl64 || jt==flt32;
}

/**
 * @brief Tests whether specified #jtype occupies 2 slots (#i64 and #dbl64).
 */
inline bool is_wide(jtype jt)
{
    return jt==dbl64 || jt==i64;
}

/**
 * @brief Converts a #VM_Data_Type into #jtype.
 *
 * Java's byte (VM_DATA_TYPE_INT8) and boolean (VM_DATA_TYPE_BOOLEAN) are 
 * both returned as #i8.
 *
 * VM_DATA_TYPE_STRING, VM_DATA_TYPE_CLASS and VM_DATA_TYPE_ARRAY are all 
 * mapped onto #jobj.
 */
jtype to_jtype(VM_Data_Type vmtype);

}
};    // ~namespace Jitrino::Jet

#endif  // __JDEFS_H_INCLUDED__
