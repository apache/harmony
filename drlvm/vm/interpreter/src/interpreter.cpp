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
#include "interpreter.h"
#include "interpreter_exports.h"
#include "interpreter_imports.h"
#include "open/vm_field_access.h"
#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"
#include <math.h>

#include "vtable.h"
#include "exceptions.h"
#include "exceptions_int.h"
#include "vm_arrays.h"
#include "vm_strings.h"
#include "jit_runtime_support_common.h"

#include "interp_native.h"
#include "interp_defs.h"
#include "interp_vm_helpers.h"
#include "ini.h"
#include "compile.h"

#include "thread_manager.h"
#include <sstream>

#ifndef PLATFORM_POSIX
#include <float.h>
#define isnan _isnan
#endif

bool interpreter_enable_debug = true;
int interpreter_enable_debug_trigger = -1;

const char *opcodeNames[256] = {
    "NOP", "ACONST_NULL", "ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2",
    "ICONST_3", "ICONST_4", "ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0",
    "FCONST_1", "FCONST_2", "DCONST_0", "DCONST_1", "BIPUSH", "SIPUSH",
    "LDC", "LDC_W", "LDC2_W", "ILOAD", "LLOAD", "FLOAD", "DLOAD", "ALOAD",
    "ILOAD_0", "ILOAD_1", "ILOAD_2", "ILOAD_3", "LLOAD_0", "LLOAD_1", "LLOAD_2",
    "LLOAD_3", "FLOAD_0", "FLOAD_1", "FLOAD_2", "FLOAD_3", "DLOAD_0", "DLOAD_1",
    "DLOAD_2", "DLOAD_3", "ALOAD_0", "ALOAD_1", "ALOAD_2", "ALOAD_3", "IALOAD",
    "LALOAD", "FALOAD", "DALOAD", "AALOAD", "BALOAD", "CALOAD", "SALOAD",
    "ISTORE", "LSTORE", "FSTORE", "DSTORE", "ASTORE", "ISTORE_0", "ISTORE_1",
    "ISTORE_2", "ISTORE_3", "LSTORE_0", "LSTORE_1", "LSTORE_2", "LSTORE_3",
    "FSTORE_0", "FSTORE_1", "FSTORE_2", "FSTORE_3", "DSTORE_0", "DSTORE_1",
    "DSTORE_2", "DSTORE_3", "ASTORE_0", "ASTORE_1", "ASTORE_2", "ASTORE_3",
    "IASTORE", "LASTORE", "FASTORE", "DASTORE", "AASTORE", "BASTORE", "CASTORE",
    "SASTORE", "POP", "POP2", "DUP", "DUP_X1", "DUP_X2", "DUP2", "DUP2_X1",
    "DUP2_X2", "SWAP", "IADD", "LADD", "FADD", "DADD", "ISUB", "LSUB", "FSUB",
    "DSUB", "IMUL", "LMUL", "FMUL", "DMUL", "IDIV", "LDIV", "FDIV", "DDIV",
    "IREM", "LREM", "FREM", "DREM", "INEG", "LNEG", "FNEG", "DNEG", "ISHL",
    "LSHL", "ISHR", "LSHR", "IUSHR", "LUSHR", "IAND", "LAND", "IOR", "LOR",
    "IXOR", "LXOR", "IINC", "I2L", "I2F", "I2D", "L2I", "L2F", "L2D", "F2I",
    "F2L", "F2D", "D2I", "D2L", "D2F", "I2B", "I2C", "I2S", "LCMP", "FCMPL",
    "FCMPG", "DCMPL", "DCMPG", "IFEQ", "IFNE", "IFLT", "IFGE", "IFGT", "IFLE",
    "IF_ICMPEQ", "IF_ICMPNE", "IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT",
    "IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE", "GOTO", "JSR", "RET", "TABLESWITCH",
    "LOOKUPSWITCH", "IRETURN", "LRETURN", "FRETURN", "DRETURN", "ARETURN",
    "RETURN", "GETSTATIC", "PUTSTATIC", "GETFIELD", "PUTFIELD", "INVOKEVIRTUAL",
    "INVOKESPECIAL", "INVOKESTATIC", "INVOKEINTERFACE", "_OPCODE_UNDEFINED",
    "NEW", "NEWARRAY", "ANEWARRAY", "ARRAYLENGTH", "ATHROW", "CHECKCAST",
    "INSTANCEOF", "MONITORENTER", "MONITOREXIT", "WIDE", "MULTIANEWARRAY",
    "IFNULL", "IFNONNULL", "GOTO_W", "JSR_W",

    "BREAKPOINT",
    "UNKNOWN_0xCB", "UNKNOWN_0xCC", "UNKNOWN_0xCD", "UNKNOWN_0xCE",
    "UNKNOWN_0xCF", "UNKNOWN_0xD0", "UNKNOWN_0xD1", "UNKNOWN_0xD2",
    "UNKNOWN_0xD3", "UNKNOWN_0xD4", "UNKNOWN_0xD5", "UNKNOWN_0xD6",
    "UNKNOWN_0xD7", "UNKNOWN_0xD8", "UNKNOWN_0xD9", "UNKNOWN_0xDA",
    "UNKNOWN_0xDB", "UNKNOWN_0xDC", "UNKNOWN_0xDD", "UNKNOWN_0xDE",
    "UNKNOWN_0xDF", "UNKNOWN_0xE0", "UNKNOWN_0xE1", "UNKNOWN_0xE2",
    "UNKNOWN_0xE3", "UNKNOWN_0xE4", "UNKNOWN_0xE5", "UNKNOWN_0xE6",
    "UNKNOWN_0xE7", "UNKNOWN_0xE8", "UNKNOWN_0xE9", "UNKNOWN_0xEA",
    "UNKNOWN_0xEB", "UNKNOWN_0xEC", "UNKNOWN_0xED", "UNKNOWN_0xEE",
    "UNKNOWN_0xEF", "UNKNOWN_0xF0", "UNKNOWN_0xF1", "UNKNOWN_0xF2",
    "UNKNOWN_0xF3", "UNKNOWN_0xF4", "UNKNOWN_0xF5", "UNKNOWN_0xF6",
    "UNKNOWN_0xF7", "UNKNOWN_0xF8", "UNKNOWN_0xF9", "UNKNOWN_0xFA",
    "UNKNOWN_0xFB", "UNKNOWN_0xFC", "UNKNOWN_0xFD", "UNKNOWN_0xFE",
    "UNKNOWN_0xFF"
};


static void interpreterInvokeStatic(StackFrame& prevFrame, Method *method);
static void interpreterInvokeVirtual(StackFrame& prevFrame, Method *method);
static void interpreterInvokeInterface(StackFrame& prevFrame, Method *method);
static void interpreterInvokeSpecial(StackFrame& prevFrame, Method *method);

/****************************************************/
/*** INLINE FUNCTIONS *******************************/
/****************************************************/

static inline int16
read_uint8(U_8 *addr) {
    return *addr;
}

static inline int16
read_int8(U_8 *addr) {
    return ((I_8*)addr)[0];
}

static inline int16
read_int16(U_8 *addr) {
    int res = (((I_8*)addr)[0] << 8);
    return (int16)(res + (int)addr[1]);
}

static inline uint16
read_uint16(U_8 *addr) {
    return (int16)((addr[0] << 8) + addr[1]);
}

static inline I_32
read_int32(U_8 *addr) {
    U_32 res = (addr[0] << 24) + (addr[1] << 16) + (addr[2] << 8) + addr[3];
    return (I_32) res;
}


static void throwAIOOBE(I_32 index) {
    char buf[64];
    sprintf(buf, "%i", index);
    DEBUG("****** ArrayIndexOutOfBoundsException ******\n");
    interp_throw_exception("java/lang/ArrayIndexOutOfBoundsException", buf);
}

static void throwNPE() {
    DEBUG("****** NullPointerException ******\n");
    interp_throw_exception("java/lang/NullPointerException");
}

static void throwAME(const char *msg) {
    DEBUG("****** AbstractMethodError ******\n");
    interp_throw_exception("java/lang/AbstractMethodError", msg);
}

static void throwIAE(const char *msg) {
    DEBUG("****** IllegalAccessError ******\n");
    interp_throw_exception("java/lang/IllegalAccessError", msg);
}

static inline void
Opcode_NOP(StackFrame& frame) {
    frame.ip++;
}

static inline void
Opcode_ACONST_NULL(StackFrame& frame) {
    frame.stack.push();
    frame.stack.pick().ref = 0;
    frame.stack.ref() = FLAG_OBJECT;
    frame.ip++;
}

#define DEF_OPCODE_CONST32_N(T,V,t,VAL)                 \
static inline void                                      \
Opcode_ ## T ## CONST_ ## V(StackFrame& frame) {        \
    frame.stack.push();                                 \
    frame.stack.pick().t = VAL;                         \
    frame.ip++;                                         \
}

DEF_OPCODE_CONST32_N(I,M1,i,-1) // Opcode_ICONST_M1
DEF_OPCODE_CONST32_N(I,0,i,0)   // Opcode_ICONST_0
DEF_OPCODE_CONST32_N(I,1,i,1)   // Opcode_ICONST_1
DEF_OPCODE_CONST32_N(I,2,i,2)   // Opcode_ICONST_2
DEF_OPCODE_CONST32_N(I,3,i,3)   // Opcode_ICONST_3
DEF_OPCODE_CONST32_N(I,4,i,4)   // Opcode_ICONST_4
DEF_OPCODE_CONST32_N(I,5,i,5)   // Opcode_ICONST_5
DEF_OPCODE_CONST32_N(F,0,f,0)   // Opcode_FCONST_0
DEF_OPCODE_CONST32_N(F,1,f,1)   // Opcode_FCONST_1
DEF_OPCODE_CONST32_N(F,2,f,2)   // Opcode_FCONST_2

#define DEF_OPCODE_CONST64_N(T,V,t)                         \
static inline void                                          \
Opcode_ ## T ## V(StackFrame& frame) {                      \
    frame.stack.push(2);                                    \
    Value2 val;                                             \
    val.t = V;                                              \
    frame.stack.setLong(0, val);                            \
    frame.ip++;                                             \
}

DEF_OPCODE_CONST64_N(LCONST_,0,i64) // Opcode_LCONST_0
DEF_OPCODE_CONST64_N(LCONST_,1,i64) // Opcode_LCONST_1
DEF_OPCODE_CONST64_N(DCONST_,0,d)   // Opcode_DCONST_0
DEF_OPCODE_CONST64_N(DCONST_,1,d)   // Opcode_DCONST_0



static inline void
Opcode_BIPUSH(StackFrame& frame) {
    frame.stack.push();
    frame.stack.pick().i = read_int8(frame.ip + 1);
    DEBUG_BYTECODE("push " << frame.stack.pick().i);
    frame.ip += 2;
}

static inline void
Opcode_SIPUSH(StackFrame& frame) {
    frame.stack.push();
    frame.stack.pick().i = read_int16(frame.ip + 1);
    DEBUG_BYTECODE("push " << frame.stack.pick().i);
    frame.ip += 3;
}

static inline void
Opcode_ALOAD(StackFrame& frame) {
    // get value from local variable
    U_32 varId = read_uint8(frame.ip + 1);
    Value& val = frame.locals(varId);
    ASSERT_TAGS(frame.locals.ref(varId));
   
    // store value in operand stack
    frame.stack.push();
    frame.stack.pick() = val;
    frame.stack.ref() = frame.locals.ref(varId);
    if (frame.locals.ref(varId) == FLAG_OBJECT) { ASSERT_OBJECT(UNCOMPRESS_INTERP(val.ref)); }
    DEBUG_BYTECODE("var" << (int)varId << " -> stack (val = " << (int)frame.stack.pick().i << ")");
    frame.ip += 2;
}

static inline void
Opcode_WIDE_ALOAD(StackFrame& frame) {
    // get value from local variable
    U_32 varId = read_uint16(frame.ip + 2);
    Value& val = frame.locals(varId);
    ASSERT_TAGS(frame.locals.ref(varId));
   
    // store value in operand stack
    frame.stack.push();
    frame.stack.pick() = val;
    frame.stack.ref() = frame.locals.ref(varId);
    DEBUG_BYTECODE("var" << (int)varId << " -> stack (val = " << (int)frame.stack.pick().i << ")");
    frame.ip += 4;
}

static inline void
Opcode_ILOAD(StackFrame& frame) {
    // get value from local variable
    U_32 varId = read_uint8(frame.ip + 1);
    Value& val = frame.locals(varId);
    ASSERT_TAGS(!frame.locals.ref(varId));
   
    // store value in operand stack
    frame.stack.push();
    frame.stack.pick() = val;
    DEBUG_BYTECODE("var" << (int)varId << " -> stack (val = " << (int)frame.stack.pick().i << ")");
    frame.ip += 2;
}

static inline void
Opcode_WIDE_ILOAD(StackFrame& frame) {
    // get value from local variable
    U_32 varId = read_uint16(frame.ip + 2);
    Value& val = frame.locals(varId);
    ASSERT_TAGS(!frame.locals.ref(varId));
   
    // store value in operand stack
    frame.stack.push();
    frame.stack.pick() = val;
    DEBUG_BYTECODE("var" << (int)varId << " -> stack (val = " << (int)frame.stack.pick().i << ")");
    frame.ip += 4;
}

#define DEF_OPCODE_ALOAD_N(N)           \
static inline void                      \
Opcode_ALOAD_ ## N(StackFrame& frame) { \
    Value& val = frame.locals(N);       \
    ASSERT_TAGS(frame.locals.ref(N));   \
    frame.stack.push();                 \
    frame.stack.pick() = val;           \
    frame.stack.ref() = FLAG_OBJECT;               \
    frame.stack.ref() = frame.locals.ref(N);\
    DEBUG_BYTECODE("var" #N " -> stack (val = " << (int)frame.stack.pick().i << ")"); \
    frame.ip++;                         \
}

DEF_OPCODE_ALOAD_N(0) // Opcode_ALOAD_0
DEF_OPCODE_ALOAD_N(1) // Opcode_ALOAD_1
DEF_OPCODE_ALOAD_N(2) // Opcode_ALOAD_2
DEF_OPCODE_ALOAD_N(3) // Opcode_ALOAD_3

#define DEF_OPCODE_ILOAD_N(N)           \
static inline void                      \
Opcode_ILOAD_ ## N(StackFrame& frame) { \
    Value& val = frame.locals(N);       \
    ASSERT_TAGS(!frame.locals.ref(N));  \
    frame.stack.push();                 \
    frame.stack.pick() = val;           \
    DEBUG_BYTECODE("var" #N " -> stack (val = " << (int)frame.stack.pick().i << ")"); \
    frame.ip++;                         \
}

DEF_OPCODE_ILOAD_N(0) // Opcode_ILOAD_0
DEF_OPCODE_ILOAD_N(1) // Opcode_ILOAD_1
DEF_OPCODE_ILOAD_N(2) // Opcode_ILOAD_2
DEF_OPCODE_ILOAD_N(3) // Opcode_ILOAD_3

static inline void
Opcode_LLOAD(StackFrame& frame) {
    // store value to local variable
    U_32 varId = read_uint8(frame.ip + 1);

    frame.stack.push(2);
    frame.stack.pick(s1) = frame.locals(varId + l1);
    frame.stack.pick(s0) = frame.locals(varId + l0);
    ASSERT_TAGS(!frame.locals.ref(varId + 0));
    ASSERT_TAGS(!frame.locals.ref(varId + 1));

    DEBUG_BYTECODE("stack -> var" << (int)varId);
    frame.ip += 2;
}

static inline void
Opcode_WIDE_LLOAD(StackFrame& frame) {
    // store value to local variable
    U_32 varId = read_uint16(frame.ip + 2);

    frame.stack.push(2);
    frame.stack.pick(s1) = frame.locals(varId + l1);
    frame.stack.pick(s0) = frame.locals(varId + l0);
    ASSERT_TAGS(!frame.locals.ref(varId + 0));
    ASSERT_TAGS(!frame.locals.ref(varId + 1));

    DEBUG_BYTECODE("stack -> var" << (int)varId);
    frame.ip += 4;
}

#define DEF_OPCODE_LLOAD_N(N)                           \
static inline void                                      \
Opcode_LLOAD_ ## N(StackFrame& frame) {                 \
    frame.stack.push(2);                                \
    frame.stack.pick(s1) = frame.locals(N + l1);        \
    frame.stack.pick(s0) = frame.locals(N + l0);        \
    ASSERT_TAGS(!frame.locals.ref(N + 0));              \
    ASSERT_TAGS(!frame.locals.ref(N + 1));              \
                                                        \
    DEBUG_BYTECODE("var" #N " -> stack");                        \
    frame.ip++;                                         \
}

DEF_OPCODE_LLOAD_N(0) // Opcode_LLOAD_0
DEF_OPCODE_LLOAD_N(1) // Opcode_LLOAD_1
DEF_OPCODE_LLOAD_N(2) // Opcode_LLOAD_2
DEF_OPCODE_LLOAD_N(3) // Opcode_LLOAD_3


    
static inline void
Opcode_ASTORE(StackFrame& frame) {
    Value &val = frame.stack.pick();
    ASSERT_TAGS(frame.stack.ref());

    // store value to local variable
    U_32 varId = read_uint8(frame.ip + 1);
    frame.locals(varId) = val;
    frame.locals.ref(varId) = frame.stack.ref();

    frame.stack.ref() = FLAG_NONE;
    frame.stack.pop();
    DEBUG_BYTECODE("stack -> var" << (int)varId);
    frame.ip += 2;
}

static inline void
Opcode_ISTORE(StackFrame& frame) {
    Value &val = frame.stack.pick();
    ASSERT_TAGS(!frame.stack.ref());

    // store value to local variable
    U_32 varId = read_uint8(frame.ip + 1);
    frame.locals(varId) = val;
    frame.locals.ref(varId) = FLAG_NONE;

    frame.stack.pop();
    DEBUG_BYTECODE("stack -> var" << (int)varId);
    frame.ip += 2;
}

static inline void
Opcode_WIDE_ASTORE(StackFrame& frame) {
    Value &val = frame.stack.pick();
    ASSERT_TAGS(frame.stack.ref());

    // store value to local variable
    U_32 varId = read_uint16(frame.ip + 2);

    frame.locals(varId) = val;
    frame.locals.ref(varId) = frame.stack.ref();

    frame.stack.ref() = FLAG_NONE;
    frame.stack.pop();
    DEBUG_BYTECODE("stack -> var" << (int)varId);
    frame.ip += 4;
}

static inline void
Opcode_WIDE_ISTORE(StackFrame& frame) {
    Value &val = frame.stack.pick();
    ASSERT_TAGS(!frame.stack.ref());

    // store value to local variable
    U_32 varId = read_uint16(frame.ip + 2);
    frame.locals(varId) = val;
    frame.locals.ref(varId) = FLAG_NONE;

    frame.stack.pop();
    DEBUG_BYTECODE("stack -> var" << (int)varId);
    frame.ip += 4;
}

#define DEF_OPCODE_ASTORE_N(N)                      \
static inline void                                  \
Opcode_ASTORE_##N(StackFrame& frame) {              \
    Value& val = frame.stack.pick();                \
    ASSERT_TAGS(frame.stack.ref());                 \
    frame.locals(N) = val;                          \
    frame.locals.ref(N) = frame.stack.ref();        \
    frame.stack.ref() = FLAG_NONE;                         \
    frame.stack.pop();                              \
    DEBUG_BYTECODE("stack -> var" #N );                      \
    frame.ip++;                                     \
}

DEF_OPCODE_ASTORE_N(0) // Opcode_ASTORE_0
DEF_OPCODE_ASTORE_N(1) // Opcode_ASTORE_1
DEF_OPCODE_ASTORE_N(2) // Opcode_ASTORE_2
DEF_OPCODE_ASTORE_N(3) // Opcode_ASTORE_3

#define DEF_OPCODE_ISTORE_N(N)                      \
static inline void                                  \
Opcode_ISTORE_##N(StackFrame& frame) {              \
    Value& val = frame.stack.pick();                \
    ASSERT_TAGS(!frame.stack.ref());                \
    frame.locals(N) = val;                          \
    frame.locals.ref(N) = FLAG_NONE;                \
    frame.stack.pop();                              \
    DEBUG_BYTECODE("stack -> var" #N );                      \
    frame.ip++;                                     \
}

DEF_OPCODE_ISTORE_N(0) // Opcode_ASTORE_0
DEF_OPCODE_ISTORE_N(1) // Opcode_ASTORE_1
DEF_OPCODE_ISTORE_N(2) // Opcode_ASTORE_2
DEF_OPCODE_ISTORE_N(3) // Opcode_ASTORE_3

static inline void
Opcode_LSTORE(StackFrame& frame) {
    U_32 varId = read_uint8(frame.ip + 1);
    ASSERT_TAGS(!frame.stack.ref(0));
    ASSERT_TAGS(!frame.stack.ref(1));
    frame.locals(varId + l1) = frame.stack.pick(s1);
    frame.locals(varId + l0) = frame.stack.pick(s0);
    frame.locals.ref(varId + 0) = FLAG_NONE;
    frame.locals.ref(varId + 1) = FLAG_NONE;
    frame.stack.pop(2);
    DEBUG_BYTECODE("stack -> var" << (int)varId);
    frame.ip += 2;
}

static inline void
Opcode_WIDE_LSTORE(StackFrame& frame) {
    U_32 varId = read_uint16(frame.ip + 2);
    ASSERT_TAGS(!frame.stack.ref(0));
    ASSERT_TAGS(!frame.stack.ref(1));
    frame.locals(varId + l1) = frame.stack.pick(s1);
    frame.locals(varId + l0) = frame.stack.pick(s0);
    frame.locals.ref(varId + 0) = FLAG_NONE;
    frame.locals.ref(varId + 1) = FLAG_NONE;
    frame.stack.pop(2);
    DEBUG_BYTECODE("stack -> var" << (int)varId);
    frame.ip += 4;
}

#define DEF_OPCODE_LSTORE_N(N)                          \
static inline void                                      \
Opcode_LSTORE_ ## N(StackFrame& frame) {                \
    ASSERT_TAGS(!frame.stack.ref(0));                   \
    ASSERT_TAGS(!frame.stack.ref(1));                   \
    frame.locals(N + l1) = frame.stack.pick(s1);        \
    frame.locals(N + l0) = frame.stack.pick(s0);        \
    frame.locals.ref(N + 0) = FLAG_NONE;                \
    frame.locals.ref(N + 1) = FLAG_NONE;                \
    frame.stack.pop(2);                                 \
    DEBUG_BYTECODE("stack -> var" #N);                  \
    frame.ip++;                                         \
}

DEF_OPCODE_LSTORE_N(0) // Opcode_LSTORE_0
DEF_OPCODE_LSTORE_N(1) // Opcode_LSTORE_1
DEF_OPCODE_LSTORE_N(2) // Opcode_LSTORE_2
DEF_OPCODE_LSTORE_N(3) // Opcode_LSTORE_3

#if defined (__INTEL_COMPILER)
#pragma warning(push) 
#pragma warning (disable:1572)    // conversion from pointer to same-sized integral type (potential portability problem)
#endif

static inline int
fcmpl(float a, float b) {
    if (a > b) return 1;
    if (a == b) return 0;
    return -1;
}

static inline int
fcmpg(float a, float b) {
    if (a < b) return -1;
    if (a == b) return 0;
    return 1;
}

static inline int
dcmpl(double a, double b) {
    if (a > b) return 1;
    if (a == b) return 0;
    return -1;
}

static inline int
dcmpg(double a, double b) {
    if (a < b) return -1;
    if (a == b) return 0;
    return 1;
}
#if defined (__INTEL_COMPILER)
#pragma warning(pop) 
#endif


#define DEF_OPCODE_MATH_32_32_TO_32(CODE, expr)                 \
static inline void                                              \
Opcode_##CODE(StackFrame& frame) {                              \
    Value& arg0 = frame.stack.pick(0);                          \
    Value& arg1 = frame.stack.pick(1);                          \
    Value& res = arg1;                                          \
    DEBUG_BYTECODE("(" << arg1.i << ", ");                               \
    expr;                                                       \
    DEBUG_BYTECODE(arg0.i << " ) = " << res.i);                          \
    frame.stack.pop();                                          \
    frame.ip++;                                                 \
}

#define DEF_OPCODE_MATH_32_TO_32(CODE, expr)                    \
static inline void                                              \
Opcode_## CODE(StackFrame& frame) {                             \
    Value& arg = frame.stack.pick(0);                           \
    Value& res = arg;                                           \
    expr;                                                       \
    frame.ip++;                                                 \
}

#define DEF_OPCODE_MATH_32_TO_64(OPCODE, expr)                  \
static inline void                                              \
Opcode_ ## OPCODE(StackFrame& frame) {                          \
    Value& arg = frame.stack.pick();                            \
    Value2 res;                                                 \
    expr;                                                       \
    frame.stack.push();                                         \
    frame.stack.setLong(0, res);                                \
    frame.ip++;                                                 \
}

#define DEF_OPCODE_MATH_64_TO_64(OPCODE, expr)                  \
static inline void                                              \
Opcode_ ## OPCODE(StackFrame& frame) {                          \
    Value2 arg, res;                                            \
    arg = frame.stack.getLong(0);                               \
    expr;                                                       \
    frame.stack.setLong(0, res);                                \
    frame.ip++;                                                 \
}

#define DEF_OPCODE_MATH_64_64_TO_64(CODE, expr)                 \
static inline void                                              \
        Opcode_ ## CODE(StackFrame& frame) {                    \
    Value2 arg0, arg1, res;                                     \
    arg0 = frame.stack.getLong(0);                              \
    arg1 = frame.stack.getLong(2);                              \
    frame.stack.pop(2);                                         \
    expr;                                                       \
    frame.stack.setLong(0, res);                                \
    DEBUG_BYTECODE("(" << arg0.d << ", " << arg1.d << ") = " << res.d);  \
    frame.ip++;                                                 \
}

#define DEF_OPCODE_MATH_64_64_TO_32(CODE, expr)                 \
static inline void                                              \
        Opcode_ ## CODE(StackFrame& frame) {                    \
    Value2 arg0, arg1;                                          \
    Value res;                                                  \
    arg0 = frame.stack.getLong(0);                              \
    arg1 = frame.stack.getLong(2);                              \
    frame.stack.pop(3);                                         \
    expr;                                                       \
    frame.stack.pick() = res;                                   \
    DEBUG_BYTECODE("(" << arg0.d << ", " << arg1.d << ") = " << res.i);  \
    frame.ip++;                                                 \
}

#define DEF_OPCODE_MATH_64_TO_32(CODE,expr)                     \
static inline void                                              \
Opcode_ ## CODE(StackFrame& frame) {                            \
    Value2 arg;                                                 \
    Value res;                                                  \
    arg = frame.stack.getLong(0);                               \
    expr;                                                       \
    frame.stack.pop();                                          \
    frame.stack.pick() = res;                                   \
    frame.ip++;                                                 \
}

#define DEF_OPCODE_MATH_64_32_TO_64(CODE, expr)                 \
static inline void                                              \
Opcode_ ## CODE(StackFrame& frame) {                            \
    Value arg0;                                                 \
    Value2 arg1, res;                                           \
    arg0 = frame.stack.pick(0);                                 \
    arg1 = frame.stack.getLong(1);                              \
    frame.stack.pop();                                          \
    expr;                                                       \
    frame.stack.setLong(0, res);                                \
    frame.ip++;                                                 \
}


DEF_OPCODE_MATH_32_32_TO_32(IADD,  res.i = arg1.i + arg0.i)   // Opcode_IADD
DEF_OPCODE_MATH_32_32_TO_32(ISUB,  res.i = arg1.i - arg0.i)   // Opcode_ISUB
DEF_OPCODE_MATH_32_32_TO_32(IMUL,  res.i = arg1.i * arg0.i)   // Opcode_IMUL
DEF_OPCODE_MATH_32_32_TO_32(IOR,   res.i = arg1.i | arg0.i)    // Opcode_IOR
DEF_OPCODE_MATH_32_32_TO_32(IAND,  res.i = arg1.i & arg0.i)  // Opcode_IAND
DEF_OPCODE_MATH_32_32_TO_32(IXOR,  res.i = arg1.i ^ arg0.i)  // Opcode_IXOR
DEF_OPCODE_MATH_32_32_TO_32(ISHL,  res.i = arg1.i << (arg0.i & 0x1f)) // Opcode_ISHL
DEF_OPCODE_MATH_32_32_TO_32(ISHR,  res.i = arg1.i >> (arg0.i & 0x1f)) // Opcode_ISHR
DEF_OPCODE_MATH_32_32_TO_32(IUSHR, res.i = ((U_32)arg1.i) >> (arg0.i & 0x1f))   // Opcode_IUSHR

DEF_OPCODE_MATH_32_32_TO_32(FADD,  res.f = arg1.f + arg0.f)   // Opcode_FADD
DEF_OPCODE_MATH_32_32_TO_32(FSUB,  res.f = arg1.f - arg0.f)   // Opcode_FSUB
DEF_OPCODE_MATH_32_32_TO_32(FMUL,  res.f = arg1.f * arg0.f)   // Opcode_FMUL
DEF_OPCODE_MATH_32_32_TO_32(FDIV,  res.f = arg1.f / arg0.f)   // Opcode_FDIV
// FIXME: is it correct? bitness is ok?
DEF_OPCODE_MATH_32_32_TO_32(FREM,  res.f = fmodf(arg1.f, arg0.f)) // Opcode_FREM

DEF_OPCODE_MATH_64_64_TO_64(LADD,  res.i64 = arg1.i64 + arg0.i64)
DEF_OPCODE_MATH_64_64_TO_64(LSUB,  res.i64 = arg1.i64 - arg0.i64)
DEF_OPCODE_MATH_64_64_TO_64(LMUL,  res.i64 = arg1.i64 * arg0.i64)
DEF_OPCODE_MATH_64_64_TO_64(LOR,   res.i64 = arg1.i64 | arg0.i64)
DEF_OPCODE_MATH_64_64_TO_64(LAND,  res.i64 = arg1.i64 & arg0.i64)
DEF_OPCODE_MATH_64_64_TO_64(LXOR,  res.i64 = arg1.i64 ^ arg0.i64)

DEF_OPCODE_MATH_64_64_TO_64(DADD, res.d = arg1.d + arg0.d)
DEF_OPCODE_MATH_64_64_TO_64(DSUB, res.d = arg1.d - arg0.d)
DEF_OPCODE_MATH_64_64_TO_64(DMUL, res.d = arg1.d * arg0.d)
DEF_OPCODE_MATH_64_64_TO_64(DDIV, res.d = arg1.d / arg0.d)
DEF_OPCODE_MATH_64_64_TO_64(DREM, res.d = fmod(arg1.d, arg0.d))

DEF_OPCODE_MATH_64_32_TO_64(LSHL, res.i64 = arg1.i64 << (arg0.i & 0x3f))
DEF_OPCODE_MATH_64_32_TO_64(LSHR, res.i64 = arg1.i64 >> (arg0.i & 0x3f))
DEF_OPCODE_MATH_64_32_TO_64(LUSHR, res.i64 = ((uint64)arg1.i64) >> (arg0.i & 0x3f))   // Opcode_LUSHR

DEF_OPCODE_MATH_32_32_TO_32(FCMPL, res.i = fcmpl(arg1.f, arg0.f)) // Opcode_FCMPL
DEF_OPCODE_MATH_32_32_TO_32(FCMPG, res.i = fcmpg(arg1.f, arg0.f)) // Opcode_FCMPG
DEF_OPCODE_MATH_64_64_TO_32(DCMPL, res.i = dcmpl(arg1.d, arg0.d)) // Opcode_FCMPL
DEF_OPCODE_MATH_64_64_TO_32(DCMPG, res.i = dcmpg(arg1.d, arg0.d)) // Opcode_FCMPG


DEF_OPCODE_MATH_32_TO_32(INEG, res.i = -arg.i)     // Opcode_INEG
DEF_OPCODE_MATH_32_TO_32(FNEG, res.f = -arg.f)     // Opcode_FNEG
DEF_OPCODE_MATH_64_TO_64(LNEG, res.i64 = -arg.i64) // Opcode_LNEG
DEF_OPCODE_MATH_64_TO_64(DNEG, res.d = -arg.d)     // Opcode_DNEG


DEF_OPCODE_MATH_32_TO_32(I2F, res.f = (float) arg.i)     // Opcode_I2F
//DEF_OPCODE_MATH_32_TO_32(F2I, res.i = (I_32) arg.f)     // Opcode_F2I
DEF_OPCODE_MATH_32_TO_32(I2B, res.i = (I_8) arg.i)      // Opcode_I2B
DEF_OPCODE_MATH_32_TO_32(I2S, res.i = (int16) arg.i)     // Opcode_I2S
DEF_OPCODE_MATH_32_TO_32(I2C, res.i = (uint16) arg.i)    // Opcode_I2C

DEF_OPCODE_MATH_32_TO_64(I2L, res.i64 = (int64) arg.i)   // Opcode_I2L
DEF_OPCODE_MATH_32_TO_64(I2D, res.d = (double) arg.i)    // Opcode_I2D
//DEF_OPCODE_MATH_32_TO_64(F2L, res.i64 = (int64) arg.f)   // Opcode_F2L
DEF_OPCODE_MATH_32_TO_64(F2D, res.d = (double) arg.f)    // Opcode_F2D

DEF_OPCODE_MATH_64_TO_64(L2D, res.d = (double) arg.i64)  // Opcode_L2D
//DEF_OPCODE_MATH_64_TO_64(D2L, res.i64 = (int64) arg.d)   // Opcode_D2L

#if defined (__INTEL_COMPILER)
#pragma warning( push )
#pragma warning (disable:1683) // to get rid of remark #1683: explicit conversion of a 64-bit integral type to a smaller integral type
#endif

DEF_OPCODE_MATH_64_TO_32(D2F, res.f = (float) arg.d)     // Opcode_D2F
//DEF_OPCODE_MATH_64_TO_32(D2I, res.i = (I_32) arg.d)     // Opcode_D2I
DEF_OPCODE_MATH_64_TO_32(L2F, res.f = (float) arg.i64)   // Opcode_L2F
DEF_OPCODE_MATH_64_TO_32(L2I, res.i = (I_32) arg.i64)   // Opcode_L2I

#if defined (__INTEL_COMPILER)
#pragma warning( pop )
#endif

static inline void
Opcode_D2I(StackFrame& frame) {
    Value2 arg;
    Value res;
    arg = frame.stack.getLong(0);

    int64 val = arg.i64;
    int64 exponent = val & ((int64)0x7FF << 52);
    int64 max_exp = ((int64)0x3FF + 31) << 52;

    if (exponent < max_exp) {
        res.i = (int) arg.d;
    } else {
        if (isnan(arg.d)) {
            res.i = 0;
        } else if (arg.d > 0) {
            res.i = (I_32)2147483647;
        } else {
            res.i = (I_32)2147483648u;
        }
    }

    frame.stack.pop();
    frame.stack.pick() = res;
    frame.ip++;
}

static inline void
Opcode_F2I(StackFrame& frame) {
    Value& arg = frame.stack.pick(0);

    int val = arg.i;
    int exponent = val & (0xFF << 23);
    int max_exp = (0x7F + 31) << 23;

    if (exponent < max_exp) {
        arg.i = (I_32) arg.f;
    } else {
        if (isnan(arg.f)) {
            arg.i = 0;
        } else if (arg.f > 0) {
            arg.i = (I_32)2147483647;
        } else {
            arg.i = (I_32)2147483648u;
        }
    }
    frame.ip++;
}

static inline void
Opcode_D2L(StackFrame& frame) {
    Value2 arg;
    Value2 res;
    arg = frame.stack.getLong(0);

    int64 val = arg.i64;
    int64 exponent = val & ((int64)0x7FF << 52);
    int64 max_exp = ((int64)0x3FF + 63) << 52;

    if (exponent < max_exp) {
        res.i64 = (int64) arg.d;
    } else {
        if (isnan(arg.d)) {
            res.i64 = (int64) 0;
        } else if (arg.d > 0) {
            res.i64 = (int64)(((uint64)(int64)-1) >> 1); // 7FFFF......
        } else {
            res.i64 = ((int64)-1) << 63; // 80000......
        }
    }

    frame.stack.setLong(0, res);
    frame.ip++;
}

static inline void
Opcode_F2L(StackFrame& frame) {
    Value arg;
    Value2 res;
    arg = frame.stack.pick(0);

    int val = arg.i;
    int exponent = val & (0xFF << 23);
    int max_exp = (0x7F + 63) << 23;

    if (exponent < max_exp) {
        res.i64 = (int64) arg.f;
    } else {
        if (isnan(arg.f)) {
            res.i64 = (int64) 0;
        } else if (arg.f > 0) {
            res.i64 = (int64)(((uint64)(int64)-1) >> 1); // 7FFFF......
        } else {
            res.i64 = ((int64)-1) << 63; // 80000......
        }
    }

    frame.stack.push();
    frame.stack.setLong(0, res);
    frame.ip++;
}

#define DEF_OPCODE_DIV_32_32_TO_32(CODE, expr)                  \
static inline void                                              \
Opcode_##CODE(StackFrame& frame) {                              \
    Value& arg0 = frame.stack.pick(0);                          \
    if (arg0.i == 0) {                                          \
        interp_throw_exception("java/lang/ArithmeticException");  \
       return;                                                 \
    }                                                           \
    Value& arg1 = frame.stack.pick(1);                          \
    Value& res = arg1;                                          \
    expr;                                                       \
    frame.stack.pop();                                          \
    frame.ip++;                                                 \
}

#if defined (__INTEL_COMPILER) 
#pragma warning( push )
#pragma warning (disable:1683) // to get rid of remark #1683: explicit conversion of a 64-bit integral type to a smaller integral type
#endif

DEF_OPCODE_DIV_32_32_TO_32(IDIV,  res.i = (I_32)((int64) arg1.i / arg0.i))   // Opcode_IDIV
DEF_OPCODE_DIV_32_32_TO_32(IREM,  res.i = (I_32)((int64) arg1.i % arg0.i))   // Opcode_IREM

#if defined (__INTEL_COMPILER)
#pragma warning( pop )
#endif

static inline void
Opcode_LDIV(StackFrame& frame) {
    Value2 arg0, arg1, res;
    arg0 = frame.stack.getLong(0);
    if (arg0.i64 == 0) {
       interp_throw_exception("java/lang/ArithmeticException");
       return;
    }
    arg1 = frame.stack.getLong(2);
    frame.stack.pop(2);

#ifdef _EM64T_
    if (arg1.i64 == -arg1.i64) {
        if (arg0.i64 == -1) {
            res.i64 = arg1.i64;
            frame.stack.setLong(0, res);
            frame.ip++;
            return;
        }
    }
#endif
    res.i64 = arg1.i64 / arg0.i64;
    frame.stack.setLong(0, res);
    frame.ip++;
}
static inline void
Opcode_LREM(StackFrame& frame) {
    Value2 arg0, arg1, res;
    arg0 = frame.stack.getLong(0);
    if (arg0.i64 == 0) {
         interp_throw_exception("java/lang/ArithmeticException");       
        return;
    }
    arg1 = frame.stack.getLong(2);
    frame.stack.pop(2);
#ifdef _EM64T_
    if (arg1.i64 == -arg1.i64) {
        if (arg0.i64 == -1) {
            res.i64 = 0l;
            frame.stack.setLong(0, res);
            frame.ip++;
            return;
        }
    }
#endif
    res.i64 = arg1.i64 % arg0.i64;
    frame.stack.setLong(0, res);
    frame.ip++;
}

#define DEF_OPCODE_CMP(CMP,check)                   \
static inline void                                  \
Opcode_##CMP(StackFrame& frame) {                   \
    I_32 val = frame.stack.pick().i;               \
    frame.stack.ref() = FLAG_NONE; /* for OPCODE_IFNULL */ \
    DEBUG_BYTECODE("val = " << (int)val);                    \
    if (val check) {                                \
    frame.ip += read_int16(frame.ip + 1);           \
    DEBUG_BYTECODE(", going to instruction");                \
    } else {                                        \
    DEBUG_BYTECODE(", false condition");                     \
    frame.ip += 3;                                  \
    }                                               \
    frame.stack.pop();                              \
    hythread_safe_point();                          \
    hythread_exception_safe_point();                \
}

DEF_OPCODE_CMP(IFEQ,==0) // Opcode_IFEQ
DEF_OPCODE_CMP(IFNE,!=0) // Opcode_IFNE
DEF_OPCODE_CMP(IFGE,>=0) // Opcode_IFGE
DEF_OPCODE_CMP(IFGT,>0)  // Opcode_IFGT
DEF_OPCODE_CMP(IFLE,<=0) // Opcode_IFLE
DEF_OPCODE_CMP(IFLT,<0)  // Opcode_IFLT

#define DEF_OPCODE_IF_ICMPXX(NAME,cmp)                          \
static inline void                                              \
Opcode_IF_ICMP ## NAME(StackFrame& frame) {                     \
    I_32 val0 = frame.stack.pick(1).i;                         \
    I_32 val1 = frame.stack.pick(0).i;                         \
    frame.stack.ref(1) = FLAG_NONE;                             \
    frame.stack.ref(0) = FLAG_NONE;                             \
    if (val0 cmp val1) {                                        \
        frame.ip += read_int16(frame.ip + 1);                   \
        DEBUG_BYTECODE(val1 << " " << val0 << " branch taken"); \
    } else {                                                    \
    frame.ip += 3;                                              \
        DEBUG_BYTECODE(val1 << " " << val0 << " branch not taken");\
    }                                                           \
    frame.stack.pop(2);                                         \
    hythread_safe_point();                                      \
    hythread_exception_safe_point();                            \
}

DEF_OPCODE_IF_ICMPXX(EQ,==) // Opcode_IF_ICMPEQ OPCODE_IF_ACMPEQ
DEF_OPCODE_IF_ICMPXX(NE,!=) // Opcode_IF_ICMPNE OPCODE_IF_ACMPNE
DEF_OPCODE_IF_ICMPXX(GE,>=) // Opcode_IF_ICMPGE
DEF_OPCODE_IF_ICMPXX(GT,>)  // Opcode_IF_ICMPGT
DEF_OPCODE_IF_ICMPXX(LE,<=) // Opcode_IF_ICMPLE
DEF_OPCODE_IF_ICMPXX(LT,<)  // Opcode_IF_ICMPLT


static inline void
Opcode_LCMP(StackFrame& frame) {
    Value2 v0, v1;
    int res;
    v1 = frame.stack.getLong(0);
    v0 = frame.stack.getLong(2);
    if (v0.i64 < v1.i64) res = -1;
    else if (v0.i64 == v1.i64) res = 0;
    else res = 1;
    frame.stack.pop(3);
    frame.stack.pick().i = res;
    DEBUG_BYTECODE("res = " << res);
    frame.ip++;
}

static bool
ldc(StackFrame& frame, U_32 index) {
    Class* clazz = frame.method->get_class();
    ConstantPool& cp = clazz->get_constant_pool();

#ifndef NDEBUG
    switch(cp.get_tag(index)) {
        case CONSTANT_String:
            DEBUG_BYTECODE("#" << (int)index << " String: \"" << cp.get_string_chars(index) << "\"");
            break;
        case CONSTANT_Integer:
            DEBUG_BYTECODE("#" << (int)index << " Integer: " << (int)cp.get_int(index));
            break;
        case CONSTANT_Float:
            DEBUG_BYTECODE("#" << (int)index << " Float: " << cp.get_float(index));
            break;
        case CONSTANT_Class:
            DEBUG_BYTECODE("#" << (int)index << " Class: \"" << class_cp_get_class_name(clazz, index) << "\"");
            break;
        default:
            DEBUG_BYTECODE("#" << (int)index << " Unknown type = " << cp.get_tag(index));
            LDIE(4, "ldc instruction: unexpected type ({0}) of constant pool entry [{1}]"
                 << cp.get_tag(index) << index);
            break;
    }
#endif

    frame.stack.push();
    if(cp.is_string(index))
    {
        String* str = cp.get_string(index);
        frame.stack.pick().ref = COMPRESS_INTERP(vm_instantiate_cp_string_resolved(str));
        frame.stack.ref() = FLAG_OBJECT;
        return !check_current_thread_exception();
    } 
    else if (cp.is_class(index))
    {
        Class *other_class = interp_resolve_class(clazz, index);
        if (!other_class) {
             return false;
        }
        assert(!hythread_is_suspend_enabled());
        
        frame.stack.pick().ref = COMPRESS_INTERP(*(other_class->get_class_handle()));
        frame.stack.ref() = FLAG_OBJECT;
        
        return !exn_raised();
    }
    
    frame.stack.pick().u = cp.get_4byte(index);
    return true;
}


static inline void
Opcode_LDC(StackFrame& frame) {
    U_32 index = read_uint8(frame.ip + 1);
    if (!ldc(frame, index)) return;
    frame.ip += 2;
}

static inline void
Opcode_LDC_W(StackFrame& frame) {
    U_32 index = read_uint16(frame.ip + 1);
    if(!ldc(frame, index)) return;
    frame.ip += 3;
}

static inline void
Opcode_LDC2_W(StackFrame& frame) {
    U_32 index = read_uint16(frame.ip + 1);

    Class *clazz = frame.method->get_class();
    ConstantPool& cp = clazz->get_constant_pool();
    frame.stack.push(2);
    Value2 val;
    val.u64 = ((uint64)cp.get_8byte_high_word(index) << 32)
        | cp.get_8byte_low_word(index);
    frame.stack.setLong(0, val);
    DEBUG_BYTECODE("#" << (int)index << " (val = " << val.d << ")");
    frame.ip += 3;
}

// TODO ivan 20041005: check if the types defined somewhere else
enum ArrayElementType {
    AR_BOOLEAN = 4, // values makes sense for opcode NEWARRAY
    AR_CHAR,
    AR_FLOAT,
    AR_DOUBLE,
    AR_BYTE,
    AR_SHORT,
    AR_INT,
    AR_LONG
};

static inline void
Opcode_NEWARRAY(StackFrame& frame) {
    int type = read_int8(frame.ip + 1);
    Class *clazz = NULL;

    // TODO ivan 20041005: can be optimized, rely on order
    Global_Env *env = VM_Global_State::loader_env;
    switch (type) {
        case AR_BOOLEAN: clazz = env->ArrayOfBoolean_Class; break;
        case AR_CHAR:    clazz = env->ArrayOfChar_Class; break;
        case AR_FLOAT:   clazz = env->ArrayOfFloat_Class; break;
        case AR_DOUBLE:  clazz = env->ArrayOfDouble_Class; break;
        case AR_BYTE:    clazz = env->ArrayOfByte_Class; break;
        case AR_SHORT:   clazz = env->ArrayOfShort_Class; break;
        case AR_INT:     clazz = env->ArrayOfInt_Class; break;
        case AR_LONG:    clazz = env->ArrayOfLong_Class; break;
        default: DIE(("Invalid array type"));
    }
    assert(clazz);

    // TODO: is it possible to optimize?
    // array data size = length << (type & 3);
    // how it can be usable?
    I_32 length = frame.stack.pick().i;

    if (length < 0) {
        interp_throw_exception("java/lang/NegativeArraySizeException");
        return;
    }

    Vector_Handle array  = vm_new_vector_primitive(clazz,length);
    if (check_current_thread_exception()) {
        // OutOfMemoryError occured
        return;
    }
   
    frame.stack.pick().ref = COMPRESS_INTERP((ManagedObject*)array);
    DEBUG_BYTECODE(" (val = " << (int)frame.stack.pick().i << ")");
    frame.stack.ref() = FLAG_OBJECT;
    frame.ip += 2;
}

static inline void
Opcode_ANEWARRAY(StackFrame& frame) {
    int classId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();

    Class *objClass = interp_resolve_class(clazz, classId);
    if (!objClass) return; // exception

    Class *arrayClass = interp_class_get_array_of_class(objClass);

    I_32 length = frame.stack.pick().i;

    if (length < 0) {
        interp_throw_exception("java/lang/NegativeArraySizeException");
        return;
    }


    Vector_Handle array = vm_new_vector(arrayClass, length);
    if (check_current_thread_exception()) {
        // OutOfMemoryError occured
        return;
    }

    set_vector_length(array, length);
    DEBUG_BYTECODE("length = " << length);

    frame.stack.pick().ref = COMPRESS_INTERP((ManagedObject*)array);
    frame.stack.ref() = FLAG_OBJECT;
    frame.ip += 3;
}

static inline bool
allocDimensions(StackFrame& frame, Class *arrayClass, int depth) {

    int *length = (int*)ALLOC_FRAME(sizeof(int) * (depth));
    int *pos = (int*)ALLOC_FRAME(sizeof(int) * (depth));
    Class **clss = (Class**)ALLOC_FRAME(sizeof(Class*) * (depth));
    int d;
    int max_depth = depth - 1;

   // check dimensions phase
    for(d = 0; d < depth; d++) {
        pos[d] = 0;
        int len = length[d] = frame.stack.pick(depth - 1 - d).i; 

        if (len < 0) {
            interp_throw_exception("java/lang/NegativeArraySizeException");
            return false;
        }
        
        if (len == 0) {
            if (d < max_depth) max_depth = d;
        }

        frame.stack.pick(depth - 1 - d).ref = 0;
        frame.stack.ref(depth - 1 - d) = FLAG_OBJECT;
    }

    // init Class* array
    Class *c = clss[0] = arrayClass;

    for(d = 1; d < depth; d++) {
        c = c->get_array_element_class();
        clss[d] = c;
    }

    // init root element
    ManagedObject* array = (ManagedObject*) vm_new_vector(clss[0], length[0]);
    if (check_current_thread_exception()) {
        // OutOfMemoryError occured
        return false;
    }
    set_vector_length(array, length[0]);
    frame.stack.pick(depth - 1).ref = COMPRESS_INTERP(array);
    if (max_depth == 0) return true;

    d = 1;
    // allocation dimensions
    while(true) {
        ManagedObject *element = (ManagedObject*) vm_new_vector(clss[d], length[d]);
        if (check_current_thread_exception()) {
            // OutOfMemoryError occured
            return false;
        }

        set_vector_length(element, length[d]);

        if (d != max_depth) {
            frame.stack.pick(depth - 1 - d).ref = COMPRESS_INTERP(element);
            d++;
            continue;
        }

        while(true) {
            array = UNCOMPRESS_INTERP(frame.stack.pick((depth - 1) - (d - 1)).ref);
            // addr can be a pointer to either ManagedObject* or COMPRESSED_REFERENCE
            ManagedObject** addr = get_vector_element_address_ref(array, pos[d-1]);

            STORE_UREF_BY_ADDR(addr, element);
            pos[d-1]++;

            if (pos[d-1] < length[d-1]) {
                break;
            }

            pos[d-1] = 0;
            element = array;
            d--;

            if (d == 0) return true;
        }
    }
}

static inline void
Opcode_MULTIANEWARRAY(StackFrame& frame) {
    int classId = read_uint16(frame.ip + 1);
    int depth = read_uint8(frame.ip + 3);
    Class *clazz = frame.method->get_class();

    Class *arrayClass = interp_resolve_class(clazz, classId);
    if (!arrayClass) return; // exception

    DEBUG_BYTECODE(class_get_name(arrayClass) << " " << depth);

    bool success = allocDimensions(frame, arrayClass, depth);
    if (!success) {
        return;
    }

    frame.stack.popClearRef(depth - 1);
    DEBUG_BYTECODE(" (val = " << (int)frame.stack.pick().i << ")");
    frame.ip += 4;
}

static inline void
Opcode_NEW(StackFrame& frame) {
    U_32 classId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();

    Class *objClass = interp_resolve_class_new(clazz, classId);
    if (!objClass) return; // exception

    DEBUG_BYTECODE("cless = " << class_get_name(objClass));

    class_initialize(objClass);

    if (check_current_thread_exception()) {
        return;
    }

    ManagedObject *obj = class_alloc_new_object(objClass);

    if (check_current_thread_exception()) {
        // OutOfMemoryError occured
        return;
    }

    assert(obj);
   
    frame.stack.push();

    frame.stack.pick().ref = COMPRESS_INTERP(obj);
    DEBUG_BYTECODE(" (val = " << (int)frame.stack.pick().i << ")");
    frame.stack.ref() = FLAG_OBJECT;
    frame.ip += 3;
}

static inline void
Opcode_POP(StackFrame& frame) {
    frame.stack.ref() = FLAG_NONE;
    frame.stack.pop();
    frame.ip++;
}

static inline void
Opcode_POP2(StackFrame& frame) {
    frame.stack.ref(0) = FLAG_NONE;
    frame.stack.ref(1) = FLAG_NONE;
    frame.stack.pop(2);
    frame.ip++;
}

static inline void
Opcode_SWAP(StackFrame& frame) {
    Value tmp = frame.stack.pick(0);
    frame.stack.pick(0) = frame.stack.pick(1);
    frame.stack.pick(1) = tmp;

    U_8 ref = frame.stack.ref(0);
    frame.stack.ref(0) = frame.stack.ref(1);
    frame.stack.ref(1) = ref;
    frame.ip++;
}

static inline void
Opcode_DUP(StackFrame& frame) {
    frame.stack.push();
    frame.stack.pick(0) = frame.stack.pick(1);
    frame.stack.ref(0) = frame.stack.ref(1);
    frame.ip++;
}

static inline void
Opcode_DUP2(StackFrame& frame) {
    frame.stack.push(2);
    frame.stack.pick(0) = frame.stack.pick(2);
    frame.stack.pick(1) = frame.stack.pick(3);

    frame.stack.ref(0) = frame.stack.ref(2);
    frame.stack.ref(1) = frame.stack.ref(3);
    frame.ip++;
}

static inline void
Opcode_DUP_X1(StackFrame& frame) {
    frame.stack.push();
    frame.stack.pick(0) = frame.stack.pick(1);
    frame.stack.pick(1) = frame.stack.pick(2);
    frame.stack.pick(2) = frame.stack.pick(0);

    frame.stack.ref(0) = frame.stack.ref(1);
    frame.stack.ref(1) = frame.stack.ref(2);
    frame.stack.ref(2) = frame.stack.ref(0);
    frame.ip++;
}

static inline void
Opcode_DUP_X2(StackFrame& frame) {
    frame.stack.push();
    frame.stack.pick(0) = frame.stack.pick(1);
    frame.stack.pick(1) = frame.stack.pick(2);
    frame.stack.pick(2) = frame.stack.pick(3);
    frame.stack.pick(3) = frame.stack.pick(0);
    
    frame.stack.ref(0) = frame.stack.ref(1);
    frame.stack.ref(1) = frame.stack.ref(2);
    frame.stack.ref(2) = frame.stack.ref(3);
    frame.stack.ref(3) = frame.stack.ref(0);
    frame.ip++;
}

static inline void
Opcode_DUP2_X1(StackFrame& frame) {
    frame.stack.push(2);
    frame.stack.pick(0) = frame.stack.pick(2);
    frame.stack.pick(1) = frame.stack.pick(3);
    frame.stack.pick(2) = frame.stack.pick(4);
    frame.stack.pick(3) = frame.stack.pick(0);
    frame.stack.pick(4) = frame.stack.pick(1);

    frame.stack.ref(0) = frame.stack.ref(2);
    frame.stack.ref(1) = frame.stack.ref(3);
    frame.stack.ref(2) = frame.stack.ref(4);
    frame.stack.ref(3) = frame.stack.ref(0);
    frame.stack.ref(4) = frame.stack.ref(1);
    frame.ip++;
}

static inline void
Opcode_DUP2_X2(StackFrame& frame) {
    frame.stack.push(2);
    frame.stack.pick(0) = frame.stack.pick(2);
    frame.stack.pick(1) = frame.stack.pick(3);
    frame.stack.pick(2) = frame.stack.pick(4);
    frame.stack.pick(3) = frame.stack.pick(5);
    frame.stack.pick(4) = frame.stack.pick(0);
    frame.stack.pick(5) = frame.stack.pick(1);

    frame.stack.ref(0) = frame.stack.ref(2);
    frame.stack.ref(1) = frame.stack.ref(3);
    frame.stack.ref(2) = frame.stack.ref(4);
    frame.stack.ref(3) = frame.stack.ref(5);
    frame.stack.ref(4) = frame.stack.ref(0);
    frame.stack.ref(5) = frame.stack.ref(1);
    frame.ip++;
}

static inline void
Opcode_IINC(StackFrame& frame) {
    U_32 varNum = read_uint8(frame.ip + 1);
    int incr = read_int8(frame.ip + 2);
    frame.locals(varNum).i += incr;
    DEBUG_BYTECODE("var" << (int)varNum << " += " << incr);
    frame.ip += 3;
}

static inline void
Opcode_WIDE_IINC(StackFrame& frame) {
    U_32 varNum = read_uint16(frame.ip + 2);
    int incr = read_int16(frame.ip + 4);
    frame.locals(varNum).i += incr;
    DEBUG_BYTECODE(" += " << incr);
    frame.ip += 6;
}

static inline void 
Opcode_ARRAYLENGTH(StackFrame& frame) {
    REF st_ref = frame.stack.pick().ref;
    if (st_ref == 0) {
        throwNPE();
        return;
    }
    ManagedObject *ref = UNCOMPRESS_INTERP(st_ref);

    frame.stack.ref() = FLAG_NONE;
    frame.stack.pick().i = get_vector_length((Vector_Handle)ref);
    DEBUG_BYTECODE("length = " << frame.stack.pick().i);
    frame.ip++;
}

static inline void
Opcode_AALOAD(StackFrame& frame) {
    REF st_ref = frame.stack.pick(1).ref;
    if (st_ref == 0) {
        throwNPE();
        return;
    }
    Vector_Handle array = (Vector_Handle) UNCOMPRESS_INTERP(st_ref);
    U_32 length = get_vector_length(array);
    U_32 pos = frame.stack.pick(0).u;

    DEBUG_BYTECODE("length = " << (int)length << " pos = " << (int)pos);

    if (pos >= length) {
        throwAIOOBE(pos);
        return;
    }

    frame.stack.pop();

    // Array contains elements according to REFS_IS_COMPRESSED_MODE
    // But even in compressed mode on 64-bit platform interpreter's
    // stack can contain uncompressed references (according to REF32)
    // So we'll convert references if needed
    ManagedObject** pelem = get_vector_element_address_ref(array, pos);
    ManagedObject* urefelem = UNCOMPRESS_REF(*pelem);
    frame.stack.pick().ref = COMPRESS_INTERP(urefelem);
    frame.ip++;
}

#define DEF_OPCODE_XALOAD(CODE,arraytype,type,store)                            \
static inline void                                                              \
Opcode_ ## CODE(StackFrame& frame) {                                            \
    REF ref = frame.stack.pick(1).ref;                                          \
    if (ref == 0) {                                                             \
        throwNPE();                                                             \
        return;                                                                 \
    }                                                                           \
    Vector_Handle array = (Vector_Handle) UNCOMPRESS_INTERP(ref);               \
    U_32 length = get_vector_length(array);                                   \
    U_32 pos = frame.stack.pick(0).u;                                         \
                                                                                \
    DEBUG_BYTECODE("length = " << (int)length << " pos = " << (int)pos); \
                                                                                \
    if (pos >= length) {                                                        \
        throwAIOOBE(pos);                                                       \
        return;                                                                 \
    }                                                                           \
                                                                                \
    frame.stack.pop();                                                          \
                                                                                \
    type* addr = get_vector_element_address_ ## arraytype(array, pos);          \
    frame.stack.pick().store = *addr;                                           \
    frame.stack.ref() = FLAG_NONE;                                              \
    frame.ip++;                                                                 \
}

DEF_OPCODE_XALOAD(BALOAD, int8, I_8, i)
DEF_OPCODE_XALOAD(CALOAD, uint16, uint16, u)
DEF_OPCODE_XALOAD(SALOAD, int16, int16, i)
DEF_OPCODE_XALOAD(IALOAD, int32, I_32, i)
DEF_OPCODE_XALOAD(FALOAD, f32, float, f)

static inline void
Opcode_LALOAD(StackFrame& frame) {
    REF ref = frame.stack.pick(1).ref;
    if (ref == 0) {
        throwNPE();
        return;
    }
    Vector_Handle array = (Vector_Handle) UNCOMPRESS_INTERP(ref);
    U_32 length = get_vector_length(array);
    U_32 pos = frame.stack.pick(0).u;

    DEBUG_BYTECODE("length = " << (int)length << " pos = " << (int)pos);

    if (pos >= length) {
        throwAIOOBE(pos);
        return;
    }

    frame.stack.ref(1) = FLAG_NONE;

    Value2* addr = (Value2*) get_vector_element_address_int64(array, pos);
    frame.stack.setLong(0, *addr);
    frame.ip++;
}

static inline void
Opcode_AASTORE(StackFrame& frame) {
    REF ref = frame.stack.pick(2).ref;
    if (ref == 0) {
        throwNPE();
        return;
    }
    Vector_Handle array = (Vector_Handle) UNCOMPRESS_INTERP(ref);
    U_32 length = get_vector_length(array);
    U_32 pos = frame.stack.pick(1).u;

    DEBUG_BYTECODE("length = " << (int)length << " pos = " << (int)pos);

    if (pos >= length) {
        throwAIOOBE(pos);
        return;
    }

    // Check ArrayStoreException
    ManagedObject *arrayObj = (ManagedObject*) array;
    Class *arrayClass = arrayObj->vt()->clss;
    Class *elementClass = arrayClass->get_array_element_class();
    ManagedObject* obj = UNCOMPRESS_INTERP(frame.stack.pick().ref);
    if (!obj == 0 && !vm_instanceof(obj, elementClass)) {
        interp_throw_exception("java/lang/ArrayStoreException");
        return;
    }

    // Array contains elements according to REFS_IS_COMPRESSED_MODE
    // But even in compressed mode on 64-bit platform interpreter's
    // stack can contain uncompressed references (according to REF32)
    // So we'll convert references if needed
    ManagedObject** pelem = get_vector_element_address_ref(array, pos);
    STORE_UREF_BY_ADDR(pelem, obj);
    frame.stack.ref(2) = FLAG_NONE;
    frame.stack.ref(0) = FLAG_NONE;
    frame.stack.pop(3);
    frame.ip++;
}

#define DEF_OPCODE_IASTORE(CODE,arraytype,type,ldtype)                          \
static inline void                                                              \
Opcode_ ## CODE(StackFrame& frame) {                                            \
    REF ref = frame.stack.pick(2).ref;                                          \
    if (ref == 0) {                                                             \
        throwNPE();                                                             \
        return;                                                                 \
    }                                                                           \
    Vector_Handle array = (Vector_Handle) UNCOMPRESS_INTERP(ref);               \
    U_32 length = get_vector_length(array);                                   \
    U_32 pos = frame.stack.pick(1).u;                                         \
                                                                                \
    DEBUG_BYTECODE("length = " << (int)length << " pos = " << (int)pos); \
                                                                                \
    if (pos >= length) {                                                        \
        throwAIOOBE(pos);                                                       \
        return;                                                                 \
    }                                                                           \
                                                                                \
    type* addr = (type*) get_vector_element_address_ ## arraytype(array, pos);  \
    *addr = frame.stack.pick().ldtype;                                          \
    frame.stack.ref(2) = FLAG_NONE;                                             \
    frame.stack.pop(3);                                                         \
    frame.ip++;                                                                 \
}

#if defined (__INTEL_COMPILER) 
#pragma warning( push )
#pragma warning (disable:810) // to get rid of remark #810: conversion from "int" to "unsigned char" may lose significant bits
#endif

DEF_OPCODE_IASTORE(CASTORE, uint16, uint16, u)
DEF_OPCODE_IASTORE(BASTORE, int8, I_8, i)
DEF_OPCODE_IASTORE(SASTORE, int16, int16, i)
DEF_OPCODE_IASTORE(IASTORE, int32, I_32, i)
DEF_OPCODE_IASTORE(FASTORE, f32, float, f)

#if defined (__INTEL_COMPILER)
#pragma warning( pop )
#endif

static inline void
Opcode_LASTORE(StackFrame& frame) {
    REF ref = frame.stack.pick(3).ref;
    if (ref == 0) {
        throwNPE();
        return;
    }
    Vector_Handle array = (Vector_Handle) UNCOMPRESS_INTERP(ref);
    U_32 length = get_vector_length(array);
    U_32 pos = frame.stack.pick(2).u;

    DEBUG_BYTECODE("length = " << (int)length << " pos = " << (int)pos);

    if (pos >= length) {
        /* FIXME ivan 20041005: array index out of bounds exception */
        throwAIOOBE(pos);
        return;
    }

    Value2* addr = (Value2*) get_vector_element_address_int64(array, pos);
    *addr = frame.stack.getLong(0);
    frame.stack.ref(3) = FLAG_NONE;
    frame.stack.pop(4);
    frame.ip++;
}

static inline void
Opcode_PUTSTATIC(StackFrame& frame) {
    U_32 fieldId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();

    Field *field = interp_resolve_static_field(clazz, fieldId, true);
    if (!field) return; // exception

    // FIXME: is it possible to move the code into !cp_is_resolved condition above?
    class_initialize(field->get_class());

    if (check_current_thread_exception()) {
        return;
    }

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_FIELD_MODIFICATION) {
        Method *method = frame.method;
        M2N_ALLOC_MACRO;
        putstatic_callback(field, frame);
        M2N_FREE_MACRO;
    }


    if (field->is_final()) {
        if(!frame.method->get_class()->is_initializing()) {
            throwIAE(field_get_name(field));
            return;
        }
    }

    void* addr = field_get_address(field);

    DEBUG_BYTECODE(field->get_name()->bytes << " " << field->get_descriptor()->bytes
            << " (val = " << (int)frame.stack.pick().i << ")");

    switch (field->get_java_type()) {
#ifdef COMPACT_FIELDS // use compact fields on ipf
        case VM_DATA_TYPE_BOOLEAN:
        case VM_DATA_TYPE_INT8:
            *(U_8*)addr = (U_8) frame.stack.pick().u;
            frame.stack.pop();
            break;

        case VM_DATA_TYPE_CHAR:
        case VM_DATA_TYPE_INT16:
            *(uint16*)addr = (uint16) frame.stack.pick().u;
            frame.stack.pop();
            break;

#else // ia32 not using compact fields
        case VM_DATA_TYPE_BOOLEAN:
            *(U_32*)addr = (U_8) frame.stack.pick().u;
            frame.stack.pop();
            break;
        case VM_DATA_TYPE_CHAR:
            *(U_32*)addr = (uint16) frame.stack.pick().u;
            frame.stack.pop();
            break;

        case VM_DATA_TYPE_INT8:
            *(I_32*)addr = (I_8) frame.stack.pick().i;
            frame.stack.pop();
            break;
        case VM_DATA_TYPE_INT16:
            *(I_32*)addr = (int16) frame.stack.pick().i;
            frame.stack.pop();
            break;
#endif
        case VM_DATA_TYPE_INT32:
        case VM_DATA_TYPE_F4:
            *(I_32*)addr = frame.stack.pick().i;
            frame.stack.pop();
            break;

        case VM_DATA_TYPE_ARRAY:
        case VM_DATA_TYPE_CLASS:
            {
                ManagedObject* val = UNCOMPRESS_INTERP(frame.stack.pick().ref);
                STORE_UREF_BY_ADDR(addr, val);
                frame.stack.ref() = FLAG_NONE;
                frame.stack.pop();
                break;
            }
        case VM_DATA_TYPE_INT64:
        case VM_DATA_TYPE_F8:
            {
                double *vaddr = (double*) addr;
                *vaddr = frame.stack.getLong(0).d;
                frame.stack.pop(2);
                break;
            }

        default:
            LDIE(52, "Unexpected data type");
    }
    frame.ip += 3;
}

static inline void
Opcode_GETSTATIC(StackFrame& frame) {
    U_32 fieldId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();

    Field *field = interp_resolve_static_field(clazz, fieldId, false);
    if (!field) return; // exception

    // FIXME: is it possible to move the code into !cp_is_resolved condition above?
    class_initialize(field->get_class());

    if (check_current_thread_exception()) {
        return;
    }

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_FIELD_ACCESS) {
        Method *method = frame.method;
        M2N_ALLOC_MACRO;
        getstatic_callback(field, frame);
        M2N_FREE_MACRO;
    }

    void *addr = field_get_address(field);
    frame.stack.push();

    switch (field->get_java_type()) {
#ifdef COMPACT_FIELDS // use compact fields on ipf
        case VM_DATA_TYPE_BOOLEAN:
            frame.stack.pick().u = *(U_8*)addr;
            break;

        case VM_DATA_TYPE_INT8:
            frame.stack.pick().i = *(I_8*)addr;
            break;

        case VM_DATA_TYPE_CHAR:
            frame.stack.pick().u = *(uint16*)addr;
            break;

        case VM_DATA_TYPE_INT16:
            frame.stack.pick().i = *(int16*)addr;
            break;

#else // ia32 not using compact fields
        case VM_DATA_TYPE_BOOLEAN:
        case VM_DATA_TYPE_CHAR:
            frame.stack.pick().u = *(U_32*)addr;
            break;

        case VM_DATA_TYPE_INT8:
        case VM_DATA_TYPE_INT16:
#endif
        case VM_DATA_TYPE_INT32:
        case VM_DATA_TYPE_F4:
            frame.stack.pick().i = *(I_32*)addr;
            break;
        case VM_DATA_TYPE_ARRAY:
        case VM_DATA_TYPE_CLASS:
            {
                ManagedObject* val = UNCOMPRESS_REF(*((ManagedObject**)addr));
                frame.stack.pick().ref = COMPRESS_INTERP(val);
                frame.stack.ref() = FLAG_OBJECT;
                break;
            }
        case VM_DATA_TYPE_INT64:
        case VM_DATA_TYPE_F8:
            {
                Value2 val;
                val.d = *(double*)addr;
                frame.stack.push();
                frame.stack.setLong(0, val);
                break;
            }

        default:
            LDIE(52, "Unexpected data type");
    }
    DEBUG_BYTECODE(field->get_name()->bytes << " " << field->get_descriptor()->bytes
            << " (val = " << (int)frame.stack.pick().i << ")");
    frame.ip += 3;
}

static inline void
Opcode_PUTFIELD(StackFrame& frame) {
    U_32 fieldId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();

    Field *field = interp_resolve_nonstatic_field(clazz, fieldId, true);
    if (!field) return; // exception
    
    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_FIELD_MODIFICATION) {
        Method *method = frame.method;
        M2N_ALLOC_MACRO;
        putfield_callback(field, frame);
        M2N_FREE_MACRO;
    }

    if (field->is_final() && clazz != field->get_class()) {
        throwIAE(field_get_name(field));
        return;
    }

    DEBUG_BYTECODE(field->get_name()->bytes << " " << field->get_descriptor()->bytes
            << " (val = " << (int)frame.stack.pick().i << ")");

    uint16 obj_ref_pos = 1;

    if (field->get_java_type() == VM_DATA_TYPE_INT64 ||
        field->get_java_type() == VM_DATA_TYPE_F8)
        ++obj_ref_pos;

    REF ref = frame.stack.pick(obj_ref_pos).ref;

    if (ref == 0) {
        throwNPE();
        return;
    }

    ManagedObject *obj = UNCOMPRESS_INTERP(ref);
    U_8* addr = ((U_8*)obj) + field->get_offset();

    switch (field->get_java_type()) {

#ifdef COMPACT_FIELDS // use compact fields on ipf
        case VM_DATA_TYPE_BOOLEAN:
        case VM_DATA_TYPE_INT8:
            *(U_8*)addr = (U_8)frame.stack.pick(0).u;
            frame.stack.ref(1) = FLAG_NONE;
            frame.stack.pop(2);
            break;

        case VM_DATA_TYPE_CHAR:
        case VM_DATA_TYPE_INT16:
            *(uint16*)addr = (uint16)frame.stack.pick(0).u;
            frame.stack.ref(1) = FLAG_NONE;
            frame.stack.pop(2);
            break;

#else // ia32 not using compact fields
        case VM_DATA_TYPE_BOOLEAN:
            *(U_32*)addr = (U_8)frame.stack.pick(0).u;
            frame.stack.ref(1) = FLAG_NONE;
            frame.stack.pop(2);
            break;

        case VM_DATA_TYPE_INT8:
            *(I_32*)addr = (I_8)frame.stack.pick(0).i;
            frame.stack.ref(1) = FLAG_NONE;
            frame.stack.pop(2);
            break;

        case VM_DATA_TYPE_CHAR:
            *(U_32*)addr = (uint16)frame.stack.pick(0).u;
            frame.stack.ref(1) = FLAG_NONE;
            frame.stack.pop(2);
            break;

        case VM_DATA_TYPE_INT16:
            *(I_32*)addr = (int16)frame.stack.pick(0).i;
            frame.stack.ref(1) = FLAG_NONE;
            frame.stack.pop(2);
            break;
#endif
        case VM_DATA_TYPE_INT32:
        case VM_DATA_TYPE_F4:
            *(I_32*)addr = frame.stack.pick(0).i;
            frame.stack.ref(1) = FLAG_NONE;
            frame.stack.pop(2);
            break;

        case VM_DATA_TYPE_ARRAY:
        case VM_DATA_TYPE_CLASS:
            {
                ManagedObject* val = UNCOMPRESS_INTERP(frame.stack.pick(0).ref);
                STORE_UREF_BY_ADDR(addr, val);
                frame.stack.ref(0) = FLAG_NONE;
                frame.stack.ref(1) = FLAG_NONE;
                frame.stack.pop(2);
                break;
            }
        case VM_DATA_TYPE_INT64:
        case VM_DATA_TYPE_F8:
            {
                double *vaddr = (double*) addr;
                *vaddr = frame.stack.getLong(0).d;
                frame.stack.ref(2) = FLAG_NONE;
                frame.stack.pop(3);
                break;
            }

        default:
            LDIE(52, "Unexpected data type");
    }
    frame.ip += 3;
}

static inline void
Opcode_GETFIELD(StackFrame& frame) {
    U_32 fieldId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();

    Field *field = interp_resolve_nonstatic_field(clazz, fieldId, false);
    if (!field) return; // exception

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_FIELD_ACCESS) {
        Method *method = frame.method;
        M2N_ALLOC_MACRO;
        getfield_callback(field, frame);
        M2N_FREE_MACRO;
    }

    REF ref = frame.stack.pick(0).ref;
    if (ref == 0) {
        throwNPE();
        return;
    }
    ManagedObject *obj = UNCOMPRESS_INTERP(ref);
    U_8 *addr = ((U_8*)obj) + field->get_offset();
    frame.stack.ref() = FLAG_NONE;

    switch (field->get_java_type()) {

#ifdef COMPACT_FIELDS // use compact fields on ipf
        case VM_DATA_TYPE_BOOLEAN:
            frame.stack.pick(0).u = (U_32) *(U_8*)addr;
            break;

        case VM_DATA_TYPE_INT8:
            frame.stack.pick(0).i = (I_32) *(I_8*)addr;
            break;

        case VM_DATA_TYPE_CHAR:
            frame.stack.pick(0).u = (U_32) *(uint16*)addr;
            break;

        case VM_DATA_TYPE_INT16:
            frame.stack.pick(0).i = (I_32) *(int16*)addr;
            break;

#else // ia32 - not using compact fields
        case VM_DATA_TYPE_BOOLEAN:
        case VM_DATA_TYPE_CHAR:
            frame.stack.pick(0).u = *(U_32*)addr;
            break;
        case VM_DATA_TYPE_INT8:
        case VM_DATA_TYPE_INT16:
#endif
        case VM_DATA_TYPE_INT32:
        case VM_DATA_TYPE_F4:
            frame.stack.pick(0).i = *(I_32*)addr;
            break;

        case VM_DATA_TYPE_ARRAY:
        case VM_DATA_TYPE_CLASS:
            {
                ManagedObject* val = UNCOMPRESS_REF(*((ManagedObject**)addr));
                frame.stack.pick(0).ref = COMPRESS_INTERP(val);
                frame.stack.ref() = FLAG_OBJECT;
                break;
            }
        case VM_DATA_TYPE_INT64:
        case VM_DATA_TYPE_F8:
            {
                Value2 val;
                val.d = *(double*)addr;
                frame.stack.push();
                frame.stack.setLong(0, val);
                break;
            }

        default:
            LDIE(52, "Unexpected data type");
    }
    DEBUG_BYTECODE(field->get_name()->bytes << " " << field->get_descriptor()->bytes
            << " (val = " << (int)frame.stack.pick().i << ")");

    frame.ip += 3;
}

static inline void
Opcode_INVOKEVIRTUAL(StackFrame& frame) {
    U_32 methodId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();

    Method *method = interp_resolve_virtual_method(clazz, methodId);
    if (!method) return; // exception

    DEBUG_BYTECODE(method);

    hythread_exception_safe_point();
    if(check_current_thread_exception()) {
        return;
    }
    hythread_safe_point();
    interpreterInvokeVirtual(frame, method);
}

static inline void
Opcode_INVOKEINTERFACE(StackFrame& frame) {
    U_32 methodId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();

    Method *method = interp_resolve_interface_method(clazz, methodId);
    if (!method) return; // exception
    
    DEBUG_BYTECODE(method);

    hythread_exception_safe_point();
    if(check_current_thread_exception()) {
        return;
    }
    hythread_safe_point();
    interpreterInvokeInterface(frame, method);
}

static inline void
Opcode_INVOKESTATIC(StackFrame& frame) {
    U_32 methodId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();

    Method *method = interp_resolve_static_method(clazz, methodId);
    if (!method) return; // exception

    DEBUG_BYTECODE(method);

    // FIXME: is it possible to move the code into !cp_is_resolved condition above?
    class_initialize(method->get_class());

    hythread_exception_safe_point();
    if (check_current_thread_exception()) {
        return;
    }

    hythread_safe_point();
    interpreterInvokeStatic(frame, method);
}

static inline void
Opcode_INVOKESPECIAL(StackFrame& frame) {
    U_32 methodId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();

    Method *method = interp_resolve_special_method(clazz, methodId);
    if (!method) return; // exception
    
    DEBUG_BYTECODE(method);

    hythread_exception_safe_point();
    if (check_current_thread_exception()) {
        return;
    }

    hythread_safe_point();
    interpreterInvokeSpecial(frame, method);
}

static inline void
Opcode_TABLESWITCH(StackFrame& frame) {
    U_8* oldip = frame.ip;
    U_8* ip = frame.ip + 1;
    ip = ((ip - (U_8*)frame.method->get_byte_code_addr() + 3) & ~3)
        + (U_8*)frame.method->get_byte_code_addr();
    I_32 deflt = read_int32(ip);
    I_32 low =   read_int32(ip+4);
    I_32 high =    read_int32(ip+8);
    I_32 val = frame.stack.pick().i;
    frame.stack.pop();
    DEBUG_BYTECODE("val = " << val << " low = " << low << " high = " << high);
    if (val < low || val > high) {
        DEBUG_BYTECODE("default offset taken!\n");
        frame.ip = oldip + deflt;
    }
    else {
        frame.ip = oldip + read_int32(ip + 12 + ((val - low) << 2));
    }
}

static inline void
Opcode_LOOKUPSWITCH(StackFrame& frame) {
    U_8* oldip = frame.ip;
    U_8* ip = frame.ip + 1;
    ip = ((ip - (U_8*)frame.method->get_byte_code_addr() + 3) & ~3)
        + (U_8*)frame.method->get_byte_code_addr();
    I_32 deflt = read_int32(ip);
    I_32 num = read_int32(ip+4);
    I_32 val = frame.stack.pick().i;
    frame.stack.pop();

    for(int i = 0; i < num; i++) {
        I_32 key = read_int32(ip + 8 + i * 8);
        if (val == key) {
            frame.ip = oldip + read_int32(ip + 12 + i * 8);
            return;
        }

    }
    DEBUG_BYTECODE("default offset taken!\n");
    frame.ip = oldip + deflt;
}

static inline void
Opcode_GOTO(StackFrame& frame) {
    hythread_safe_point();
    hythread_exception_safe_point();
    DEBUG_BYTECODE("going to instruction");
    frame.ip += read_int16(frame.ip + 1);
}

static inline void
Opcode_GOTO_W(StackFrame& frame) {
    hythread_safe_point();
    hythread_exception_safe_point();
    DEBUG_BYTECODE("going to instruction");
    frame.ip += read_int32(frame.ip + 1);
}

static inline void
Opcode_JSR(StackFrame& frame) {
    U_32 retAddr = (U_32)(frame.ip + 3 -
        (U_8*)frame.method->get_byte_code_addr());
    frame.stack.push();
    frame.stack.pick().u = retAddr;
    frame.stack.ref() = FLAG_RET_ADDR;
    DEBUG_BYTECODE("going to instruction");
    frame.ip += read_int16(frame.ip + 1);
}

static inline void
Opcode_JSR_W(StackFrame& frame) {
    U_32 retAddr = (U_32)(frame.ip + 5 -
        (U_8*)frame.method->get_byte_code_addr());
    frame.stack.push();
    frame.stack.pick().u = retAddr;
    frame.stack.ref() = FLAG_RET_ADDR;
    DEBUG_BYTECODE("going to instruction");
    frame.ip += read_int32(frame.ip + 1);
}

static inline void
Opcode_RET(StackFrame& frame) {
    U_32 varNum = read_uint8(frame.ip + 1);
    U_32 retAddr = frame.locals(varNum).u;
    assert(frame.locals.ref(varNum) == FLAG_RET_ADDR);
    frame.ip = retAddr + (U_8*)frame.method->get_byte_code_addr();
}

static inline void
Opcode_WIDE_RET(StackFrame& frame) {
    U_32 varNum = read_uint16(frame.ip + 2);
    U_32 retAddr = frame.locals(varNum).u;
    frame.ip = retAddr + (U_8*)frame.method->get_byte_code_addr();
}

static inline void
Opcode_CHECKCAST(StackFrame& frame) {
    U_32 classId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();
    Class *objClass = interp_resolve_class(clazz, classId);
    if (!objClass) return; // exception
    
    DEBUG_BYTECODE("class = " << class_get_name(objClass));

    ManagedObject *obj = UNCOMPRESS_INTERP(frame.stack.pick().ref);

    if (!(obj == 0 || vm_instanceof(obj, objClass))) {
        interp_throw_exception("java/lang/ClassCastException", obj->vt()->clss->get_java_name()->bytes);
        return;
    }
    frame.ip += 3;
}

static inline void
Opcode_INSTANCEOF(StackFrame& frame) {
    U_32 classId = read_uint16(frame.ip + 1);
    Class *clazz = frame.method->get_class();
    Class *objClass = interp_resolve_class(clazz, classId);
    if (!objClass) return; // exception
    
    DEBUG_BYTECODE("class = " << class_get_name(objClass));

    ManagedObject *obj = UNCOMPRESS_INTERP(frame.stack.pick().ref);
    // FIXME ivan 20041027: vm_instanceof checks null pointers, it assumes
    // that null is Class::managed_null, but uncompress_compressed_reference
    // doesn't return managed_null for null compressed references
    frame.stack.pick().u = (obj != 0) && vm_instanceof(obj, objClass);

    frame.stack.ref() = FLAG_NONE;
    frame.ip += 3;
}

static inline void
Opcode_MONITORENTER(StackFrame& frame) {
    REF ref = frame.stack.pick().ref;
    if (ref == 0) {
        throwNPE();
        return;
    }
    frame.locked_monitors->monitor = UNCOMPRESS_INTERP(ref);

    M2N_ALLOC_MACRO;
    vm_monitor_enter_wrapper(frame.locked_monitors->monitor);
    M2N_FREE_MACRO;

    if (exn_raised()) return;

    frame.stack.ref() = FLAG_NONE;
    frame.stack.pop();
    frame.ip++;
}

static inline void
Opcode_MONITOREXIT(StackFrame& frame) {
    REF ref = frame.stack.pick().ref;
    if (ref == 0) {
        throwNPE();
        return;
    }
    M2N_ALLOC_MACRO;
    vm_monitor_exit_wrapper(UNCOMPRESS_INTERP(ref));
    M2N_FREE_MACRO;

    if (check_current_thread_exception())
        return;
    
    MonitorList *ml = frame.locked_monitors;

    frame.locked_monitors = ml->next;
    ml->next = frame.free_monitors;
    frame.free_monitors = ml;

    frame.stack.ref() = FLAG_NONE;
    frame.stack.pop();
    frame.ip++;
}

static inline void
Opcode_ATHROW(StackFrame& frame) {
    REF ref = frame.stack.pick().ref;
    ManagedObject *obj = UNCOMPRESS_INTERP(ref);
    if (obj == NULL) {
        throwNPE();
        return;
    }

    // TODO: optimize, can add a flag to class which implements Throwable
    // and set it when throwing for the first time.
    if (!vm_instanceof(obj, VM_Global_State::loader_env->java_lang_Throwable_Class)) {
        DEBUG("****** java.lang.VerifyError ******\n");
        interp_throw_exception("java/lang/VerifyError");
        return;
    }
    DEBUG_BYTECODE(obj);
    assert(!hythread_is_suspend_enabled());
    set_current_thread_exception(obj);
}

bool
findExceptionHandler(StackFrame& frame, ManagedObject **exception, Handler **hh) {
    assert(!check_current_thread_exception());
    assert(!hythread_is_suspend_enabled());

    Method *m = frame.method;
    DEBUG_BYTECODE("Searching for exception handler:");
    DEBUG_BYTECODE("   In " << m);

    U_32 ip = (U_32)(frame.ip - (U_8*)m->get_byte_code_addr());
    DEBUG_BYTECODE("ip = " << (int)ip);

    // When VM is in shutdown stage we need to execute final block to
    // release monitors and propogate an exception to the upper frames.
    if (VM_Global_State::loader_env->IsVmShutdowning()) {
        for(U_32 i = 0; i < m->num_bc_exception_handlers(); i++) {
            Handler *h = m->get_bc_exception_handler_info(i);
            if (h->get_catch_type_index() == 0 &&
                h->get_start_pc() <= ip && ip < h->get_end_pc()) {
                *hh = h;
                return true;
            }
        }
        return false;
    }

    Class *clazz = m->get_class();

    for(U_32 i = 0; i < m->num_bc_exception_handlers(); i++) {
        Handler *h = m->get_bc_exception_handler_info(i);
        DEBUG_BYTECODE("handler" << (int) i
                << ": start_pc=" << (int) h->get_start_pc()
                << " end_pc=" << (int) h->get_end_pc());

        if (ip < h->get_start_pc()) continue;
        if (ip >= h->get_end_pc()) continue;

        U_32 catch_type_index = h->get_catch_type_index();

        if (catch_type_index) {
            // 0 - is handler for all
            // if !0 - check if the handler catches this exception type

            DEBUG_BYTECODE("catch type index = " << (int)catch_type_index);

            // WARNING: GC may occur here !!!
            Class *obj = interp_resolve_class(clazz, catch_type_index);

            if (!obj) {
                // possible if verifier is disabled
                return false;
            }

            if (!vm_instanceof(*exception, obj)) continue;
        }
        *hh = h;
        return true;
    }
    return false;
}

static inline bool
processExceptionHandler(StackFrame& frame, ManagedObject **exception) {
    Method *m = frame.method;
    Handler *h;
    if (findExceptionHandler(frame, exception, &h)){
        DEBUG_BYTECODE("Exception caught: " << (*exception));
        DEBUG_BYTECODE("Found handler!\n");
        frame.ip = (U_8*)m->get_byte_code_addr() + h->get_handler_pc();
        return true;
    }
    return false;
}

void 
findCatchMethod(ManagedObject **exception, Method **catch_method, jlocation *catch_location) {
    StackFrame *frame = getLastStackFrame();
    *catch_method = NULL;
    *catch_location = 0;

    for(StackFrame *frame = getLastStackFrame(); frame; frame = frame->prev) {
        Method *method = frame->method;
        if (method->is_native()) continue;

        Handler *h;

        if(findExceptionHandler(*frame, exception, &h)) {
            *catch_method = frame->method;
            *catch_location = h->get_handler_pc();
            return;
        }
        clear_current_thread_exception();
    }
}


static inline void
stackDump(FILE * file, StackFrame& frame) {

    StackFrame *f = &frame;

    while(f) {
        Method *m = f->method;
        Class *c = m->get_class();
        const char* fname = NULL;
        int line = -2;
        get_file_and_line(m, f->ip, false, -1, &fname, &line);
        const char* filename = fname ? fname : "NULL";

#ifdef INTERPRETER_DEEP_DEBUG
        fprintf(file, "%s.%s%s (%s:%i) last bcs: (8 of %i): %s %s %s %s %s %s %s %s",
            c->name->bytes, m->get_name()->bytes, m->get_descriptor()->bytes,
            filename, line, f->n_last_bytecode,
            opcodeNames[f->last_bytecodes[(f->n_last_bytecode-1)&7]],
            opcodeNames[f->last_bytecodes[(f->n_last_bytecode-2)&7]],
            opcodeNames[f->last_bytecodes[(f->n_last_bytecode-3)&7]],
            opcodeNames[f->last_bytecodes[(f->n_last_bytecode-4)&7]],
            opcodeNames[f->last_bytecodes[(f->n_last_bytecode-5)&7]],
            opcodeNames[f->last_bytecodes[(f->n_last_bytecode-6)&7]],
            opcodeNames[f->last_bytecodes[(f->n_last_bytecode-7)&7]],
            opcodeNames[f->last_bytecodes[(f->n_last_bytecode-8)&7]]);
#else
    fprintf(file, "  %s.%s%s (%s:%i)\n", class_get_name(c), m->get_name()->bytes,
        m->get_descriptor()->bytes, filename, line);
#endif
        f = f->prev;
    }
}

void stack_dump(FILE *f, VM_thread *thread) {
    StackFrame *frame = getLastStackFrame(thread);
    stackDump(f, *frame);
}

#ifdef _WIN32
#include <io.h>
#endif

void stack_dump(int fd, VM_thread *thread) {
    FILE *f;
#ifdef _WIN32
    fd = _dup(fd);
    assert(fd != -1);
    f = _fdopen(fd, "w");
#else
    fd = dup(fd);
    assert(fd != -1);
    f = fdopen(fd, "w");
#endif
    assert(f);
    StackFrame *frame = getLastStackFrame(thread);
    stackDump(f, *frame);
    fclose(f);
}

void stack_dump(VM_thread *thread) {
    StackFrame *frame = getLastStackFrame(thread);
    stackDump(stderr, *frame);
}

void stack_dump() {
    StackFrame *frame = getLastStackFrame();
    stackDump(stderr, *frame);
}

static inline
void UNUSED dump_all_java_stacks() {
    hythread_iterator_t  iterator;
    hythread_suspend_all(&iterator, NULL);
    VM_thread *thread = jthread_get_vm_thread(hythread_iterator_next(&iterator));
    while(thread) {
        stack_dump(thread);
        thread = jthread_get_vm_thread(hythread_iterator_next(&iterator));
    }

    hythread_resume_all( NULL);

    INFO("****** END OF JAVA STACKS *****\n");
}

void
method_exit_callback_with_frame(Method *method, StackFrame& frame) {
    NativeObjectHandles handles;
    
    bool exc = check_current_thread_exception();
    jvalue val;

    val.j = 0;

    if (exc) {
        method_exit_callback(method, true, val);
        return;
    }


    switch(method->get_return_java_type()) {
        ObjectHandle h;

        case JAVA_TYPE_VOID:
            val.i = 0;
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            h = oh_allocate_local_handle();
            h->object = UNCOMPRESS_INTERP(frame.stack.pick().ref);
            val.l = (jobject) h;
            break;

        case JAVA_TYPE_FLOAT:
        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
        case JAVA_TYPE_CHAR:
        case JAVA_TYPE_SHORT:
        case JAVA_TYPE_INT:
            val.i = frame.stack.pick().i;
            break;

        case JAVA_TYPE_LONG:
        case JAVA_TYPE_DOUBLE:
            val.j = frame.stack.getLong(0).i64;
            break;

        default:
            LDIE(53, "Unexpected java type");
    }

    method_exit_callback(method, false, val);
}

void
interpreter(StackFrame &frame) {
    U_8 *first = NULL;
    U_8 ip0 = 0;
    bool breakpoint_processed = false;
    int stackLength = 0;
    size_t available;
    
    TRACE("interpreter: " << frame.method);

    assert(frame.method->is_static() || frame.This);
    
    M2N_ALLOC_MACRO;
    if (get_thread_ptr()->jvmti_thread.p_exception_object_ti || exn_raised()) {
         frame.exc = get_current_thread_exception();
         goto got_exception;
    }
    assert(!hythread_is_suspend_enabled());



    available = get_available_stack_size();
    
    if (available < 100000) {
        int &state = get_thread_ptr()->interpreter_state;

        if (!(state & INTERP_STATE_STACK_OVERFLOW)) {
            state |= INTERP_STATE_STACK_OVERFLOW;
            interp_throw_exception("java/lang/StackOverflowError");

            if (frame.framePopListener)
                frame_pop_callback(frame.framePopListener, frame.method, true);
            M2N_FREE_MACRO;
            return;
        }
    }

    frame.ip = (U_8*) frame.method->get_byte_code_addr();

    if (interpreter_ti_notification_mode
            & INTERPRETER_TI_METHOD_ENTRY_EVENT) {
        M2N_ALLOC_MACRO;
        method_entry_callback(frame.method);
        M2N_FREE_MACRO;
    }

    if (frame.method->is_synchronized()) {
        MonitorList *ml = (MonitorList*) ALLOC_FRAME(sizeof(MonitorList));
        frame.locked_monitors = ml;
        ml->next = 0;

        if (frame.method->is_static()) {
            ml->monitor = *(frame.method->get_class()->get_class_handle());
        } else {
            ml->monitor = UNCOMPRESS_INTERP(frame.locals(0).ref);
        }
        vm_monitor_enter_wrapper(ml->monitor);
    }

    breakpoint_processed = false;

    while (true) {
        ip0 = *frame.ip;

        DEBUG_BYTECODE("\n(" << frame.stack.getIndex()
                   << ") " << opcodeNames[ip0] << ": ");

restart:
        if (interpreter_ti_notification_mode) {
            if (frame.jvmti_pop_frame == POP_FRAME_NOW) {
                MonitorList *ml = frame.locked_monitors;
                while(ml) {
                    M2N_ALLOC_MACRO;
                    vm_monitor_exit_wrapper(ml->monitor);
                    M2N_FREE_MACRO;
                    ml = ml->next;
                }
                M2N_FREE_MACRO;
                return;
            }
            if (!breakpoint_processed &&
                    interpreter_ti_notification_mode
                    & INTERPRETER_TI_SINGLE_STEP_EVENT) {
                M2N_ALLOC_MACRO;
                single_step_callback(frame);
                M2N_FREE_MACRO;
            }
            if (frame.jvmti_pop_frame == POP_FRAME_NOW) {
                MonitorList *ml = frame.locked_monitors;
                while(ml) {
                    vm_monitor_exit_wrapper(ml->monitor);
                    ml = ml->next;
                }
                M2N_FREE_MACRO;
                return;
            }
            breakpoint_processed = false;

            if (get_thread_ptr()->jvmti_thread.p_exception_object_ti || exn_raised()) {
                frame.exc = get_current_thread_exception();
                goto got_exception;
            }
        }

#ifdef INTERPRETER_DEEP_DEBUG
        frame.last_bytecodes[(frame.n_last_bytecode++) & 7] = ip0;
#endif

        assert(!hythread_is_suspend_enabled());
        assert(&frame == getLastStackFrame());
        
        switch(ip0) {
            case OPCODE_NOP:
                Opcode_NOP(frame); break;

            case OPCODE_ICONST_M1: Opcode_ICONST_M1(frame); break;
            case OPCODE_ACONST_NULL: Opcode_ACONST_NULL(frame); break;
            case OPCODE_ICONST_0: Opcode_ICONST_0(frame); break;
            case OPCODE_ICONST_1: Opcode_ICONST_1(frame); break;
            case OPCODE_ICONST_2: Opcode_ICONST_2(frame); break;
            case OPCODE_ICONST_3: Opcode_ICONST_3(frame); break;
            case OPCODE_ICONST_4: Opcode_ICONST_4(frame); break;
            case OPCODE_ICONST_5: Opcode_ICONST_5(frame); break;
            case OPCODE_FCONST_0: Opcode_FCONST_0(frame); break;
            case OPCODE_FCONST_1: Opcode_FCONST_1(frame); break;
            case OPCODE_FCONST_2: Opcode_FCONST_2(frame); break;

            case OPCODE_LCONST_0: Opcode_LCONST_0(frame); break;
            case OPCODE_LCONST_1: Opcode_LCONST_1(frame); break;
            case OPCODE_DCONST_0: Opcode_DCONST_0(frame); break;
            case OPCODE_DCONST_1: Opcode_DCONST_1(frame); break;

            case OPCODE_ALOAD_0: Opcode_ALOAD_0(frame); break;
            case OPCODE_ALOAD_1: Opcode_ALOAD_1(frame); break;
            case OPCODE_ALOAD_2: Opcode_ALOAD_2(frame); break;
            case OPCODE_ALOAD_3: Opcode_ALOAD_3(frame); break;
            case OPCODE_ILOAD_0: Opcode_ILOAD_0(frame); break;
            case OPCODE_ILOAD_1: Opcode_ILOAD_1(frame); break;
            case OPCODE_ILOAD_2: Opcode_ILOAD_2(frame); break;
            case OPCODE_ILOAD_3: Opcode_ILOAD_3(frame); break;
            case OPCODE_FLOAD_0: Opcode_ILOAD_0(frame); break;
            case OPCODE_FLOAD_1: Opcode_ILOAD_1(frame); break;
            case OPCODE_FLOAD_2: Opcode_ILOAD_2(frame); break;
            case OPCODE_FLOAD_3: Opcode_ILOAD_3(frame); break;

            case OPCODE_ASTORE_0: Opcode_ASTORE_0(frame); break;
            case OPCODE_ASTORE_1: Opcode_ASTORE_1(frame); break;
            case OPCODE_ASTORE_2: Opcode_ASTORE_2(frame); break;
            case OPCODE_ASTORE_3: Opcode_ASTORE_3(frame); break;

            case OPCODE_ISTORE_0: Opcode_ISTORE_0(frame); break;
            case OPCODE_ISTORE_1: Opcode_ISTORE_1(frame); break;
            case OPCODE_ISTORE_2: Opcode_ISTORE_2(frame); break;
            case OPCODE_ISTORE_3: Opcode_ISTORE_3(frame); break;
            case OPCODE_FSTORE_0: Opcode_ISTORE_0(frame); break;
            case OPCODE_FSTORE_1: Opcode_ISTORE_1(frame); break;
            case OPCODE_FSTORE_2: Opcode_ISTORE_2(frame); break;
            case OPCODE_FSTORE_3: Opcode_ISTORE_3(frame); break;

            case OPCODE_ALOAD: Opcode_ALOAD(frame); break;
            case OPCODE_ILOAD: Opcode_ILOAD(frame); break;
            case OPCODE_FLOAD: Opcode_ILOAD(frame); break;

            case OPCODE_ASTORE: Opcode_ASTORE(frame); break;
            case OPCODE_ISTORE: Opcode_ISTORE(frame); break;
            case OPCODE_FSTORE: Opcode_ISTORE(frame); break;

            case OPCODE_LLOAD_0: Opcode_LLOAD_0(frame); break;
            case OPCODE_LLOAD_1: Opcode_LLOAD_1(frame); break;
            case OPCODE_LLOAD_2: Opcode_LLOAD_2(frame); break;
            case OPCODE_LLOAD_3: Opcode_LLOAD_3(frame); break;
            case OPCODE_DLOAD_0: Opcode_LLOAD_0(frame); break;
            case OPCODE_DLOAD_1: Opcode_LLOAD_1(frame); break;
            case OPCODE_DLOAD_2: Opcode_LLOAD_2(frame); break;
            case OPCODE_DLOAD_3: Opcode_LLOAD_3(frame); break;
            case OPCODE_LSTORE_0: Opcode_LSTORE_0(frame); break;
            case OPCODE_LSTORE_1: Opcode_LSTORE_1(frame); break;
            case OPCODE_LSTORE_2: Opcode_LSTORE_2(frame); break;
            case OPCODE_LSTORE_3: Opcode_LSTORE_3(frame); break;
            case OPCODE_DSTORE_0: Opcode_LSTORE_0(frame); break;
            case OPCODE_DSTORE_1: Opcode_LSTORE_1(frame); break;
            case OPCODE_DSTORE_2: Opcode_LSTORE_2(frame); break;
            case OPCODE_DSTORE_3: Opcode_LSTORE_3(frame); break;

            case OPCODE_LLOAD: Opcode_LLOAD(frame); break;
            case OPCODE_DLOAD: Opcode_LLOAD(frame); break;
            case OPCODE_LSTORE: Opcode_LSTORE(frame); break;
            case OPCODE_DSTORE: Opcode_LSTORE(frame); break;

            case OPCODE_IADD: Opcode_IADD(frame); break;
            case OPCODE_ISUB: Opcode_ISUB(frame); break;
            case OPCODE_IMUL: Opcode_IMUL(frame); break;
            case OPCODE_IOR: Opcode_IOR(frame); break;
            case OPCODE_IAND: Opcode_IAND(frame); break;
            case OPCODE_IXOR: Opcode_IXOR(frame); break;
            case OPCODE_ISHL: Opcode_ISHL(frame); break;
            case OPCODE_ISHR: Opcode_ISHR(frame); break;
            case OPCODE_IUSHR: Opcode_IUSHR(frame); break;
            case OPCODE_IDIV: Opcode_IDIV(frame); goto check_exception;
            case OPCODE_IREM: Opcode_IREM(frame); goto check_exception;

            case OPCODE_FADD: Opcode_FADD(frame); break;
            case OPCODE_FSUB: Opcode_FSUB(frame); break;
            case OPCODE_FMUL: Opcode_FMUL(frame); break;
            case OPCODE_FDIV: Opcode_FDIV(frame); break;
            case OPCODE_FREM: Opcode_FREM(frame); break;

            case OPCODE_INEG: Opcode_INEG(frame); break;
            case OPCODE_FNEG: Opcode_FNEG(frame); break;
            case OPCODE_LNEG: Opcode_LNEG(frame); break;
            case OPCODE_DNEG: Opcode_DNEG(frame); break;

            case OPCODE_LADD: Opcode_LADD(frame); break;
            case OPCODE_LSUB: Opcode_LSUB(frame); break;
            case OPCODE_LMUL: Opcode_LMUL(frame); break;
            case OPCODE_LOR: Opcode_LOR(frame); break;
            case OPCODE_LAND: Opcode_LAND(frame); break;
            case OPCODE_LXOR: Opcode_LXOR(frame); break;
            case OPCODE_LSHL: Opcode_LSHL(frame); break;
            case OPCODE_LSHR: Opcode_LSHR(frame); break;
            case OPCODE_LUSHR: Opcode_LUSHR(frame); break;
            case OPCODE_LDIV: Opcode_LDIV(frame); goto check_exception;
            case OPCODE_LREM: Opcode_LREM(frame); goto check_exception;

            case OPCODE_DADD: Opcode_DADD(frame); break;
            case OPCODE_DSUB: Opcode_DSUB(frame); break;
            case OPCODE_DMUL: Opcode_DMUL(frame); break;
            case OPCODE_DDIV: Opcode_DDIV(frame); break;
            case OPCODE_DREM: Opcode_DREM(frame); break;


            case OPCODE_RETURN:
            case OPCODE_IRETURN:
            case OPCODE_FRETURN:
            case OPCODE_ARETURN:
            case OPCODE_LRETURN:
            case OPCODE_DRETURN:
                    if (frame.locked_monitors) {
                        vm_monitor_exit_wrapper(frame.locked_monitors->monitor);
                        assert(!frame.locked_monitors->next);
                    }
                    if (interpreter_ti_notification_mode
                            & INTERPRETER_TI_METHOD_EXIT_EVENT)
                    method_exit_callback_with_frame(frame.method, frame);

                    if (frame.framePopListener)
                        frame_pop_callback(frame.framePopListener, frame.method, false);
            M2N_FREE_MACRO;
                return;

            case OPCODE_BIPUSH: Opcode_BIPUSH(frame); break;
            case OPCODE_SIPUSH: Opcode_SIPUSH(frame); break;

            case OPCODE_IINC: Opcode_IINC(frame); break;
            case OPCODE_POP: Opcode_POP(frame); break;
            case OPCODE_POP2: Opcode_POP2(frame); break;
            case OPCODE_SWAP: Opcode_SWAP(frame); break;
            case OPCODE_DUP: Opcode_DUP(frame); break;
            case OPCODE_DUP2: Opcode_DUP2(frame); break;
            case OPCODE_DUP_X1: Opcode_DUP_X1(frame); break;
            case OPCODE_DUP_X2: Opcode_DUP_X2(frame); break;
            case OPCODE_DUP2_X1: Opcode_DUP2_X1(frame); break;
            case OPCODE_DUP2_X2: Opcode_DUP2_X2(frame); break;

            case OPCODE_MULTIANEWARRAY:
                                Opcode_MULTIANEWARRAY(frame);
                                goto check_exception;
            case OPCODE_ANEWARRAY:
                                Opcode_ANEWARRAY(frame);
                                goto check_exception;
            case OPCODE_NEWARRAY:
                                Opcode_NEWARRAY(frame);
                                goto check_exception;
            case OPCODE_NEW:
                                Opcode_NEW(frame);
                                goto check_exception;

            case OPCODE_ARRAYLENGTH:
                                Opcode_ARRAYLENGTH(frame);
                                goto check_exception;

            case OPCODE_CHECKCAST:
                                Opcode_CHECKCAST(frame);
                                goto check_exception;
            case OPCODE_INSTANCEOF:
                                Opcode_INSTANCEOF(frame);
                                goto check_exception;

            case OPCODE_ATHROW:
                                Opcode_ATHROW(frame);
                                frame.exc = get_current_thread_exception();
                                goto got_exception;

            case OPCODE_MONITORENTER:
                                {
                                    MonitorList *new_ml = frame.free_monitors;
                                    if (new_ml) {
                                        frame.free_monitors = new_ml->next;
                                    } else {
                                        new_ml = (MonitorList*) ALLOC_FRAME(sizeof(MonitorList));
                                    }
                                    new_ml->next = frame.locked_monitors;
                                    new_ml->monitor = NULL;
                                    frame.locked_monitors = new_ml;
                                    Opcode_MONITORENTER(frame);
                                    frame.exc = get_current_thread_exception();

                                    if (frame.exc != 0) {
                                        frame.locked_monitors = new_ml->next;

                                        new_ml->next = frame.free_monitors;
                                        frame.free_monitors = new_ml;
                                        goto got_exception;
                                    }
                                }
                                break;
            case OPCODE_MONITOREXIT:
                                Opcode_MONITOREXIT(frame);
                                goto check_exception;

            case OPCODE_BASTORE: Opcode_BASTORE(frame); goto check_exception;
            case OPCODE_CASTORE: Opcode_CASTORE(frame); goto check_exception;
            case OPCODE_SASTORE: Opcode_SASTORE(frame); goto check_exception;
            case OPCODE_IASTORE: Opcode_IASTORE(frame); goto check_exception;
            case OPCODE_FASTORE: Opcode_FASTORE(frame); goto check_exception;
            case OPCODE_AASTORE: Opcode_AASTORE(frame); goto check_exception;

            case OPCODE_BALOAD: Opcode_BALOAD(frame); goto check_exception;
            case OPCODE_CALOAD: Opcode_CALOAD(frame); goto check_exception;
            case OPCODE_SALOAD: Opcode_SALOAD(frame); goto check_exception;
            case OPCODE_IALOAD: Opcode_IALOAD(frame); goto check_exception;
            case OPCODE_FALOAD: Opcode_FALOAD(frame); goto check_exception;
            case OPCODE_AALOAD: Opcode_AALOAD(frame); goto check_exception;
                                
            case OPCODE_LASTORE: Opcode_LASTORE(frame); goto check_exception;
            case OPCODE_DASTORE: Opcode_LASTORE(frame); goto check_exception;

            case OPCODE_LALOAD: Opcode_LALOAD(frame); goto check_exception;
            case OPCODE_DALOAD: Opcode_LALOAD(frame); goto check_exception;

            case OPCODE_TABLESWITCH: Opcode_TABLESWITCH(frame); break;
            case OPCODE_LOOKUPSWITCH: Opcode_LOOKUPSWITCH(frame); break;
            case OPCODE_GOTO: Opcode_GOTO(frame); goto check_exception;
            case OPCODE_GOTO_W: Opcode_GOTO_W(frame); goto check_exception;
            case OPCODE_JSR: Opcode_JSR(frame); break;
            case OPCODE_JSR_W: Opcode_JSR_W(frame); break;
            case OPCODE_RET: Opcode_RET(frame); break;
            case OPCODE_PUTSTATIC:
                                Opcode_PUTSTATIC(frame);
                                goto check_exception;
            case OPCODE_GETSTATIC:
                                Opcode_GETSTATIC(frame);
                                goto check_exception;

            case OPCODE_PUTFIELD:
                                Opcode_PUTFIELD(frame);
                                goto check_exception;
            case OPCODE_GETFIELD:
                                Opcode_GETFIELD(frame);
                                goto check_exception;
            case OPCODE_INVOKESTATIC:
                                Opcode_INVOKESTATIC(frame);
                                frame.exc = get_current_thread_exception();
                                if (frame.exc != 0) goto got_exception;
                                frame.ip += 3;
                                break;

            case OPCODE_INVOKESPECIAL:
                                Opcode_INVOKESPECIAL(frame);
                                frame.exc = get_current_thread_exception();
                                if (frame.exc != 0) goto got_exception;
                                frame.ip += 3;
                                break;

            case OPCODE_INVOKEVIRTUAL:
                                Opcode_INVOKEVIRTUAL(frame);
                                frame.exc = get_current_thread_exception();
                                if (frame.exc != 0) goto got_exception;
                                frame.ip += 3;
                                break;

            case OPCODE_INVOKEINTERFACE:
                                Opcode_INVOKEINTERFACE(frame);
                                frame.exc = get_current_thread_exception();
                                if (frame.exc != 0) goto got_exception;
                                frame.ip += 5;
                                break;

            case OPCODE_LDC: Opcode_LDC(frame); goto check_exception;
            case OPCODE_LDC_W: Opcode_LDC_W(frame); goto check_exception;
            case OPCODE_LDC2_W: Opcode_LDC2_W(frame); break;

            case OPCODE_IFNULL:
            case OPCODE_IFEQ: Opcode_IFEQ(frame); goto check_exception;

            case OPCODE_IFNONNULL:
            case OPCODE_IFNE: Opcode_IFNE(frame); goto check_exception;
            case OPCODE_IFGE: Opcode_IFGE(frame); goto check_exception;
            case OPCODE_IFGT: Opcode_IFGT(frame); goto check_exception;
            case OPCODE_IFLE: Opcode_IFLE(frame); goto check_exception;
            case OPCODE_IFLT: Opcode_IFLT(frame); goto check_exception;

            case OPCODE_IF_ACMPEQ:
            case OPCODE_IF_ICMPEQ: Opcode_IF_ICMPEQ(frame); goto check_exception;

            case OPCODE_IF_ACMPNE:
            case OPCODE_IF_ICMPNE: Opcode_IF_ICMPNE(frame); goto check_exception;

            case OPCODE_IF_ICMPGE: Opcode_IF_ICMPGE(frame); goto check_exception;
            case OPCODE_IF_ICMPGT: Opcode_IF_ICMPGT(frame); goto check_exception;
            case OPCODE_IF_ICMPLE: Opcode_IF_ICMPLE(frame); goto check_exception;
            case OPCODE_IF_ICMPLT: Opcode_IF_ICMPLT(frame); goto check_exception;

            case OPCODE_FCMPL: Opcode_FCMPL(frame); break;
            case OPCODE_FCMPG: Opcode_FCMPG(frame); break;
            case OPCODE_DCMPL: Opcode_DCMPL(frame); break;
            case OPCODE_DCMPG: Opcode_DCMPG(frame); break;

            case OPCODE_LCMP: Opcode_LCMP(frame); break;

            case OPCODE_I2L: Opcode_I2L(frame); break;
            case OPCODE_I2D: Opcode_I2D(frame); break;
            case OPCODE_F2L: Opcode_F2L(frame); break;
            case OPCODE_F2D: Opcode_F2D(frame); break;
            case OPCODE_F2I: Opcode_F2I(frame); break;
            case OPCODE_I2F: Opcode_I2F(frame); break;
            case OPCODE_I2B: Opcode_I2B(frame); break;
            case OPCODE_I2S: Opcode_I2S(frame); break;
            case OPCODE_I2C: Opcode_I2C(frame); break;
            case OPCODE_D2F: Opcode_D2F(frame); break;
            case OPCODE_D2I: Opcode_D2I(frame); break;
            case OPCODE_L2F: Opcode_L2F(frame); break;
            case OPCODE_L2I: Opcode_L2I(frame); break;
            case OPCODE_L2D: Opcode_L2D(frame); break;
            case OPCODE_D2L: Opcode_D2L(frame); break;
            case OPCODE_BREAKPOINT:
                             ip0 = Opcode_BREAKPOINT(frame);
                             breakpoint_processed = true;
                             goto restart;

            case OPCODE_WIDE:
            {
                U_8* ip1 = frame.ip + 1;
                switch(*ip1) {
                    case OPCODE_ALOAD: Opcode_WIDE_ALOAD(frame); break;
                    case OPCODE_ILOAD: Opcode_WIDE_ILOAD(frame); break;
                    case OPCODE_FLOAD: Opcode_WIDE_ILOAD(frame); break;
                    case OPCODE_DLOAD: Opcode_WIDE_LLOAD(frame); break;
                    case OPCODE_LLOAD: Opcode_WIDE_LLOAD(frame); break;
                    case OPCODE_ASTORE: Opcode_WIDE_ASTORE(frame); break;
                    case OPCODE_ISTORE: Opcode_WIDE_ISTORE(frame); break;
                    case OPCODE_FSTORE: Opcode_WIDE_ISTORE(frame); break;
                    case OPCODE_LSTORE: Opcode_WIDE_LSTORE(frame); break;
                    case OPCODE_DSTORE: Opcode_WIDE_LSTORE(frame); break;
                    case OPCODE_IINC: Opcode_WIDE_IINC(frame); break;
                    case OPCODE_RET: Opcode_WIDE_RET(frame); break;
                    default:
                     INFO("wide bytecode " << (int)*ip1 << " not implemented\n");
                     stackDump(stdout, frame);
                     DIE(("Unexpected wide bytecode"));
                }
                break;
            }

            default: INFO("bytecode " << (int)ip0 << " not implemented\n");
                     stackDump(stdout, frame);
                     DIE(("Unexpected bytecode"));
        }
        assert(&frame == getLastStackFrame());
        continue;

check_exception:
        frame.exc = get_current_thread_exception();
        if (frame.exc == 0) continue;
got_exception:
        assert(&frame == getLastStackFrame());

        clear_current_thread_exception();

        if (interpreter_ti_notification_mode) {
            frame.exc_catch =
                (ManagedObject*) get_thread_ptr()->jvmti_thread.p_exception_object_ti;
            p_TLS_vmthread->jvmti_thread.p_exception_object_ti = NULL;

            if (frame.exc != frame.exc_catch) {

                Method *method = frame.method;
                jlocation loc = frame.ip - method->get_byte_code_addr();

                if (frame.exc_catch != NULL) {

                    // EXCEPTION_CATCH should be generated for frame.exc_catch
                    jvmti_interpreter_exception_catch_event_callback_call(
                            frame.exc_catch, method, loc);

                    assert(!exn_raised());

                    // event is sent
                    frame.exc_catch = NULL;

                    // if no pending exception continue execution
                    if (frame.exc == NULL) continue;
                }

                // EXCEPTION event to be generated
                assert(frame.exc);
                Method *catch_method;
                jlocation catch_location;
                findCatchMethod(&frame.exc, &catch_method, &catch_location);
                M2N_ALLOC_MACRO;
                jvmti_interpreter_exception_event_callback_call(frame.exc, method, loc, catch_method, catch_location);
                M2N_FREE_MACRO;
                assert(!exn_raised());
                if (interpreter_ti_notification_mode)
                    p_TLS_vmthread->jvmti_thread.p_exception_object_ti =
                        (volatile ManagedObject*) frame.exc;
            }
        }

        if (processExceptionHandler(frame, &frame.exc)) {
            frame.stack.clear();
            frame.stack.push();
            frame.stack.pick().ref = COMPRESS_INTERP(frame.exc);
            frame.stack.ref() = FLAG_OBJECT;
            frame.exc = NULL;
            
            int &state = get_thread_ptr()->interpreter_state;

            if (state & INTERP_STATE_STACK_OVERFLOW) {
                state &= ~INTERP_STATE_STACK_OVERFLOW;
            }
            continue;
        }

        set_current_thread_exception(frame.exc);
        
        if (frame.locked_monitors) {
            M2N_ALLOC_MACRO;
            vm_monitor_exit_wrapper(frame.locked_monitors->monitor);
            M2N_FREE_MACRO;
            assert(!frame.locked_monitors->next);
        }

        DEBUG_TRACE("<EXCEPTION> ");
        if (interpreter_ti_notification_mode
                & INTERPRETER_TI_METHOD_EXIT_EVENT)
            method_exit_callback_with_frame(frame.method, frame);

        if (frame.framePopListener)
            frame_pop_callback(frame.framePopListener, frame.method, true);

        M2N_FREE_MACRO;
        assert(!hythread_is_suspend_enabled());
        return;
    }
}

void
interpreter_execute_method(
        Method   *method,
        jvalue   *return_value,
        jvalue   *args) {

    assert(!hythread_is_suspend_enabled());

    StackFrame frame;
    memset(&frame, 0, sizeof(frame));
    frame.method = method;
    frame.prev = getLastStackFrame();

    // TODO: not optimal to call is_static twice.
    if (!method->is_static()) {
            frame.This = args[0].l->object;
    }
    if (frame.prev == 0) {
        get_thread_ptr()->firstFrame = (void*) &frame;
    }
    setLastStackFrame(&frame);

    if (method->is_native()) {
        interpreter_execute_native_method(method, return_value, args);
        setLastStackFrame(frame.prev);
        return;
    }

    DEBUG_TRACE("\n{{{ interpreter_invoke: " << method);

    DEBUG("\tmax stack = " << method->get_max_stack() << endl);
    DEBUG("\tmax locals = " << method->get_max_locals() << endl);

    // Setup locals and stack on C stack.
    SETUP_LOCALS_AND_STACK(frame, method);

    int pos = 0;
    int arg = 0;

    if(!method->is_static()) {
        frame.locals.ref(pos) = FLAG_OBJECT;
        REF ref = 0;
        ObjectHandle h = (ObjectHandle) args[arg++].l;
        if (h) {
            ref = COMPRESS_INTERP(h->object);
            frame.This = h->object;
        }
        frame.locals(pos++).ref = ref;
    }

    Arg_List_Iterator iter = method_get_argument_list(method);

    Java_Type typ;
    DEBUG("\targs types = ");
    while((typ = curr_arg(iter)) != JAVA_TYPE_END) {
        DEBUG((char)typ);
        ObjectHandle h;

        switch(typ) {
        case JAVA_TYPE_LONG:
        case JAVA_TYPE_DOUBLE:
            Value2 val;
            val.i64 = args[arg++].j;
            frame.locals.setLong(pos, val);
            pos += 2;
            break;
        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
            {
                h = (ObjectHandle) args[arg++].l;

                REF ref = (h == NULL) ? 0 : COMPRESS_INTERP(h->object);
                frame.locals.ref(pos) = FLAG_OBJECT;
                frame.locals(pos++).ref = ref;
                ASSERT_OBJECT(UNCOMPRESS_INTERP(ref));
            }
            break;
        case JAVA_TYPE_FLOAT:
            frame.locals(pos++).f = args[arg++].f;
            break;
        case JAVA_TYPE_SHORT:
            frame.locals(pos++).i = (I_32)args[arg++].s;
            break;
        case JAVA_TYPE_CHAR:
            frame.locals(pos++).i = (U_32)args[arg++].c;
            break;
        case JAVA_TYPE_BYTE:
            frame.locals(pos++).i = (I_32)args[arg++].b;
            break;
        case JAVA_TYPE_BOOLEAN:
            frame.locals(pos++).i = (I_32)args[arg++].z;
            break;
        default:
            frame.locals(pos++).u = (U_32)args[arg++].i;
            break;
        }
        iter = advance_arg_iterator(iter);
    }
    DEBUG(endl);

    Java_Type ret_type = method->get_return_java_type();
    
    interpreter(frame);

    jvalue *resultPtr = (jvalue*) return_value;

    /**
      gwu2: We should clear the result before checking exception,
      otherwise we could exit this method with some undefined result,
      JNI wrappers like CallObjectMethod will allocate object handle 
      according to the value of result, so an object handle refering wild
      pointer will be inserted into local object list, and will be 
      enumerated,... It'll definitely fail.
    */
    if ((resultPtr != NULL) && (ret_type != JAVA_TYPE_VOID)) { 
        resultPtr->l = 0; //clear it  
    }
                                                     
    if (check_current_thread_exception()) {
        setLastStackFrame(frame.prev);
        DEBUG_TRACE("<EXCEPTION> interpreter_invoke }}}\n");
        return;
    }

    switch(ret_type) {
        case JAVA_TYPE_LONG:
        case JAVA_TYPE_DOUBLE:
            {
                Value2 val;
                val = frame.stack.getLong(0);
                resultPtr->j = val.i64;
            }
            break;

        case JAVA_TYPE_FLOAT:
            resultPtr->f = frame.stack.pick().f;
            break;

        case JAVA_TYPE_VOID:
            break;

        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
        case JAVA_TYPE_CHAR:
        case JAVA_TYPE_SHORT:
        case JAVA_TYPE_INT:
        case VM_DATA_TYPE_MP:
        case VM_DATA_TYPE_UP:
            resultPtr->i = frame.stack.pick().i;
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
         
            { 
                ASSERT_OBJECT(UNCOMPRESS_INTERP(frame.stack.pick().ref));
                ObjectHandle h = 0; 
                if (frame.stack.pick().ref) {
                    h = oh_allocate_local_handle();
                    h->object = UNCOMPRESS_INTERP(frame.stack.pick().ref);
                } 
                resultPtr->l = h;
            }
            break;

        default:
            LDIE(53, "Unexpected java type");
    }
    setLastStackFrame(frame.prev);
    DEBUG_TRACE("interpreter_invoke }}}\n");
}

static void
interpreterInvokeStatic(StackFrame& prevFrame, Method *method) {

    StackFrame frame;
    memset(&frame, 0, sizeof(frame));
    frame.prev = &prevFrame;
    frame.method = method;
    assert(frame.prev == getLastStackFrame());
    setLastStackFrame(&frame);

    if(method->is_native()) {
        interpreterInvokeStaticNative(prevFrame, frame, method);
        setLastStackFrame(frame.prev);
        return;
    }

    DEBUG_TRACE("\n{{{ invoke_static     : " << method);

    DEBUG("\tmax stack = " << method->get_max_stack() << endl);
    DEBUG("\tmax locals = " << method->get_max_locals() << endl);

    // Setup locals and stack on C stack.
    SETUP_LOCALS_AND_STACK(frame, method);

    frame.This = *(method->get_class()->get_class_handle()); 
    int args = method->get_num_arg_slots();

    for(int i = args-1; i >= 0; --i) {
        frame.locals(i) = prevFrame.stack.pick(args-1 - i);
        frame.locals.ref(i) = prevFrame.stack.ref(args-1 - i);
    }

    frame.jvmti_pop_frame = POP_FRAME_AVAILABLE;
    
    interpreter(frame);

    if (frame.jvmti_pop_frame == POP_FRAME_NOW) {
        setLastStackFrame(frame.prev);
        clear_current_thread_exception();
        frame.prev->ip -= 3;
        DEBUG_TRACE("<POP_FRAME> invoke_static }}}\n");
        return;
    }

    prevFrame.stack.popClearRef(args);

    if (check_current_thread_exception()) {
        setLastStackFrame(frame.prev);
        DEBUG_TRACE("<EXCEPTION> invoke_static }}}\n");
        return;
    }

    switch(method->get_return_java_type()) {
        case JAVA_TYPE_VOID:
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            prevFrame.stack.push();
            prevFrame.stack.pick() = frame.stack.pick();
            prevFrame.stack.ref() = FLAG_OBJECT;
            break;

        case JAVA_TYPE_FLOAT:
        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
        case JAVA_TYPE_CHAR:
        case JAVA_TYPE_SHORT:
        case JAVA_TYPE_INT:
            prevFrame.stack.push();
            prevFrame.stack.pick() = frame.stack.pick();
            break;

        case JAVA_TYPE_LONG:
        case JAVA_TYPE_DOUBLE:
            prevFrame.stack.push(2);
            prevFrame.stack.pick(s0) = frame.stack.pick(s0);
            prevFrame.stack.pick(s1) = frame.stack.pick(s1);
            break;

        default:
            LDIE(53, "Unexpected java type");
    }

    setLastStackFrame(frame.prev);
    DEBUG_TRACE("invoke_static }}}\n");
}

static void
interpreterInvoke(StackFrame& prevFrame, Method *method, int args, ManagedObject *obj, bool intf) {

    StackFrame frame;
    memset(&frame, 0, sizeof(frame));
    frame.prev = &prevFrame;
    assert(frame.prev == getLastStackFrame());
    frame.method = method;
    frame.This = obj;
#ifndef NDEBUG
    frame.dump_bytecodes = prevFrame.dump_bytecodes;
#endif
    setLastStackFrame(&frame);


    if(method->is_native()) {
        interpreterInvokeVirtualNative(prevFrame, frame, method, args);
        setLastStackFrame(frame.prev);
        return;
    }

    DEBUG("\tmax stack = " << method->get_max_stack() << endl);
    DEBUG("\tmax locals = " << method->get_max_locals() << endl);

    // Setup locals and stack on C stack.
    SETUP_LOCALS_AND_STACK(frame, method);

    for(int i = args-1; i >= 0; --i) {
        frame.locals(i) = prevFrame.stack.pick(args-1 - i);
        frame.locals.ref(i) = prevFrame.stack.ref(args-1 - i);
    }


    frame.jvmti_pop_frame = POP_FRAME_AVAILABLE;

    interpreter(frame);

    if (frame.jvmti_pop_frame == POP_FRAME_NOW) {
        setLastStackFrame(frame.prev);
        clear_current_thread_exception();
        if (intf) frame.prev->ip -= 5; else frame.prev->ip -= 3;
        return;
    }

    prevFrame.stack.popClearRef(args);

    if (check_current_thread_exception()) {
        setLastStackFrame(frame.prev);
        return;
    }

    switch(method->get_return_java_type()) {
        case JAVA_TYPE_VOID:
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            prevFrame.stack.push();
            prevFrame.stack.pick() = frame.stack.pick();
            prevFrame.stack.ref() = FLAG_OBJECT;
            break;

        case JAVA_TYPE_FLOAT:
        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
        case JAVA_TYPE_CHAR:
        case JAVA_TYPE_SHORT:
        case JAVA_TYPE_INT:
            prevFrame.stack.push();
            prevFrame.stack.pick() = frame.stack.pick();
            break;

        case JAVA_TYPE_LONG:
        case JAVA_TYPE_DOUBLE:
            prevFrame.stack.push(2);
            prevFrame.stack.pick(s0) = frame.stack.pick(s0);
            prevFrame.stack.pick(s1) = frame.stack.pick(s1);
            break;

        default:
            LDIE(53, "Unexpected java type");
    }

    setLastStackFrame(frame.prev);
}

static void
interpreterInvokeVirtual(StackFrame& prevFrame, Method *method) {

    int args = method->get_num_arg_slots();
    REF ref = prevFrame.stack.pick(args-1).ref;

    if (ref == 0) {
        throwNPE();
        return;
    }

    ManagedObject *obj = UNCOMPRESS_INTERP(ref);
    ASSERT_OBJECT(obj);

    Class *objClass = obj->vt()->clss;
    method = objClass->get_method_from_vtable(method->get_index());

    if (method->is_abstract()) {
        std::ostringstream str;
        str << class_get_name(method_get_class(method)) << "." <<
            method_get_name(method) << method_get_descriptor(method);
        throwAME(str.str().c_str());
        return;
    }

    DEBUG_TRACE("\n{{{ invoke_virtual    : " << method);

    interpreterInvoke(prevFrame, method, args, obj, false);
    DEBUG_TRACE("invoke_virtual }}}\n");
}

static void
interpreterInvokeInterface(StackFrame& prevFrame, Method *method) {

    int args = method->get_num_arg_slots();
    REF ref = prevFrame.stack.pick(args-1).ref;

    if (ref == 0) {
        throwNPE();
        return;
    }

    ManagedObject *obj = UNCOMPRESS_INTERP(ref);
    ASSERT_OBJECT(obj);

    if (!vm_instanceof(obj, method->get_class())) {
        interp_throw_exception("java/lang/IncompatibleClassChangeError",
            class_get_name(method_get_class(method)));
        return;
    }

    Class *clss = obj->vt()->clss;
    Method* found_method = class_lookup_method_recursive(clss, method->get_name(), method->get_descriptor());

    if (found_method == NULL) {
        interp_throw_exception("java/lang/AbstractMethodError",
            method->get_name()->bytes);
        return;
    }
    method = found_method;

    if (method->is_abstract()) {
        std::ostringstream str;
        str << class_get_name(method_get_class(method)) << "." <<
            method_get_name(method) << method_get_descriptor(method);
        throwAME(str.str().c_str());
        return;
    }

    if (!method->is_public()) {
        std::ostringstream str;
        str << class_get_name(method_get_class(method)) << "." <<
            method_get_name(method) << method_get_descriptor(method);
        throwIAE(str.str().c_str());
        return;
    }

    DEBUG_TRACE("\n{{{ invoke_interface  : " << method);

    interpreterInvoke(prevFrame, method, args, obj, true);

    DEBUG_TRACE("invoke_interface }}}\n");
}

static void
interpreterInvokeSpecial(StackFrame& prevFrame, Method *method) {

    int args = method->get_num_arg_slots();
    REF ref = prevFrame.stack.pick(args-1).ref;

    if (ref == 0) {
        throwNPE();
        return;
    }

    if (method->is_abstract()) {
        std::ostringstream str;
        str << class_get_name(method_get_class(method)) << "." <<
            method_get_name(method) << method_get_descriptor(method);
        throwAME(str.str().c_str());
        return;
    }

    ManagedObject *obj = UNCOMPRESS_INTERP(ref);

    StackFrame frame;
    memset(&frame, 0, sizeof(frame));
    frame.prev = &prevFrame;
    assert(frame.prev == getLastStackFrame());
    frame.method = method;
    frame.This = obj;
    setLastStackFrame(&frame);

    if(method->is_native()) {
        interpreterInvokeVirtualNative(prevFrame, frame, method, args);

        setLastStackFrame(frame.prev);
        return;
    }

    DEBUG_TRACE("\n{{{ invoke_special    : " << method);

    DEBUG("\tmax stack = " << method->get_max_stack() << endl);
    DEBUG("\tmax locals = " << method->get_max_locals() << endl);

    // Setup locals and stack on C stack.
    SETUP_LOCALS_AND_STACK(frame, method);

    for(int i = args-1; i >= 0; --i) {
        frame.locals(i) = prevFrame.stack.pick(args-1 - i);
        frame.locals.ref(i) = prevFrame.stack.ref(args-1 - i);
    }

    frame.jvmti_pop_frame = POP_FRAME_AVAILABLE;

    interpreter(frame);

    if (frame.jvmti_pop_frame == POP_FRAME_NOW) {
        setLastStackFrame(frame.prev);
        clear_current_thread_exception();
        frame.prev->ip -= 3;
        DEBUG_TRACE("<POP_FRAME> invoke_special }}}\n");
        return;
    }

    prevFrame.stack.popClearRef(args);

    if (check_current_thread_exception()) {
        setLastStackFrame(frame.prev);
        DEBUG_TRACE("<EXCEPTION> invoke_special }}}\n");
        return;
    }

    switch(method->get_return_java_type()) {
        case JAVA_TYPE_VOID:
            break;

        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            prevFrame.stack.push();
            prevFrame.stack.pick() = frame.stack.pick();
            prevFrame.stack.ref() = FLAG_OBJECT;
            break;

        case JAVA_TYPE_FLOAT:
        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
        case JAVA_TYPE_CHAR:
        case JAVA_TYPE_SHORT:
        case JAVA_TYPE_INT:
            prevFrame.stack.push();
            prevFrame.stack.pick() = frame.stack.pick();
            break;

        case JAVA_TYPE_LONG:
        case JAVA_TYPE_DOUBLE:
            prevFrame.stack.push(2);
            prevFrame.stack.pick(s0) = frame.stack.pick(s0);
            prevFrame.stack.pick(s1) = frame.stack.pick(s1);
            break;

        default:
            LDIE(53, "Unexpected java type");
    }
    setLastStackFrame(frame.prev);
    DEBUG_TRACE("invoke_special }}}\n");
}

GenericFunctionPointer
interpreterGetNativeMethodAddr(Method* method) {

    assert(method->is_native());

    if (method->get_state() == Method::ST_Linked ) {
        return (GenericFunctionPointer) method->get_code_addr();
    }

    NativeCodePtr f = (NativeCodePtr)interp_find_native(method);

    hythread_suspend_enable();
    jvmti_process_native_method_bind_event( (jmethodID) method, f, &f);
    hythread_suspend_disable();

    // TODO: check if we need synchronization here
    if( f ) {
        method->set_code_addr(f);
        method->set_state( Method::ST_Linked );
    }

    return (GenericFunctionPointer)f;
}
