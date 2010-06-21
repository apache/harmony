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
#include "jdefs.h"
#include <assert.h>

/**
 * @file
 * @brief Definitions for jdefs.h.
 */

namespace Jitrino {
namespace Jet {

const InstrDesc instrs[OPCODE_COUNT] = {

#ifdef _DEBUG
    #define OPCODE(nam, klass, len, flgs) \
                        {klass, OPCODE_##nam, len, flgs, #nam },
    #define UNDEFINED_OPCODE_HERE() \
                        {ik_none, _OPCODE_UNDEFINED, 1, OPF_NONE, "unused" },
#else
    #define OPCODE(nam, klass, len, flgs) \
                        {klass, len, flgs, #nam },
    #define UNDEFINED_OPCODE_HERE() \
                        {ik_none, 1, OPF_NONE, "unused" },
#endif

    OPCODE(NOP, ik_none, 1, OPF_NONE)

    OPCODE(ACONST_NULL, ik_ls, 1, OPF_NONE)

    OPCODE(ICONST_M1, ik_ls, 1, OPF_NONE)
    OPCODE(ICONST_0, ik_ls, 1, OPF_NONE)
    OPCODE(ICONST_1, ik_ls, 1, OPF_NONE)
    OPCODE(ICONST_2, ik_ls, 1, OPF_NONE)
    OPCODE(ICONST_3, ik_ls, 1, OPF_NONE)
    OPCODE(ICONST_4, ik_ls, 1, OPF_NONE)
    OPCODE(ICONST_5, ik_ls, 1, OPF_NONE)

    OPCODE(LCONST_0, ik_ls, 1, OPF_NONE)
    OPCODE(LCONST_1, ik_ls, 1, OPF_NONE)

    OPCODE(FCONST_0, ik_ls, 1, OPF_NONE)
    OPCODE(FCONST_1, ik_ls, 1, OPF_NONE)
    OPCODE(FCONST_2, ik_ls, 1, OPF_NONE)

    OPCODE(DCONST_0, ik_ls, 1, OPF_NONE)
    OPCODE(DCONST_1, ik_ls, 1, OPF_NONE)

    OPCODE(BIPUSH, ik_ls, 2, OPF_NONE)
    OPCODE(SIPUSH, ik_ls, 3, OPF_NONE)

    OPCODE(LDC, ik_ls, 2, OPF_NONE)
    OPCODE(LDC_W, ik_ls, 3, OPF_NONE)
    OPCODE(LDC2_W, ik_ls, 3, OPF_NONE)

    OPCODE(ILOAD, ik_ls, 2, OPF_VAR_USE|OPF_VAR_OP0|OPF_VAR_TYPE_I32)
    OPCODE(LLOAD, ik_ls, 2, OPF_VAR_USE|OPF_VAR_OP0|OPF_VAR_TYPE_I64)
    OPCODE(FLOAD, ik_ls, 2, OPF_VAR_USE|OPF_VAR_OP0|OPF_VAR_TYPE_FLT)
    OPCODE(DLOAD, ik_ls, 2, OPF_VAR_USE|OPF_VAR_OP0|OPF_VAR_TYPE_DBL)
    OPCODE(ALOAD, ik_ls, 2, OPF_VAR_USE|OPF_VAR_OP0|OPF_VAR_TYPE_OBJ)

    OPCODE(ILOAD_0, ik_ls, 1, OPF_VAR_USE|OPF_VAR0|OPF_VAR_TYPE_I32)
    OPCODE(ILOAD_1, ik_ls, 1, OPF_VAR_USE|OPF_VAR1|OPF_VAR_TYPE_I32)
    OPCODE(ILOAD_2, ik_ls, 1, OPF_VAR_USE|OPF_VAR2|OPF_VAR_TYPE_I32)
    OPCODE(ILOAD_3, ik_ls, 1, OPF_VAR_USE|OPF_VAR3|OPF_VAR_TYPE_I32)
    OPCODE(LLOAD_0, ik_ls, 1, OPF_VAR_USE|OPF_VAR0|OPF_VAR_TYPE_I64)
    OPCODE(LLOAD_1, ik_ls, 1, OPF_VAR_USE|OPF_VAR1|OPF_VAR_TYPE_I64)
    OPCODE(LLOAD_2, ik_ls, 1, OPF_VAR_USE|OPF_VAR2|OPF_VAR_TYPE_I64)
    OPCODE(LLOAD_3, ik_ls, 1, OPF_VAR_USE|OPF_VAR3|OPF_VAR_TYPE_I64)
    OPCODE(FLOAD_0, ik_ls, 1, OPF_VAR_USE|OPF_VAR0|OPF_VAR_TYPE_FLT)
    OPCODE(FLOAD_1, ik_ls, 1, OPF_VAR_USE|OPF_VAR1|OPF_VAR_TYPE_FLT)
    OPCODE(FLOAD_2, ik_ls, 1, OPF_VAR_USE|OPF_VAR2|OPF_VAR_TYPE_FLT)
    OPCODE(FLOAD_3, ik_ls, 1, OPF_VAR_USE|OPF_VAR3|OPF_VAR_TYPE_FLT)
    OPCODE(DLOAD_0, ik_ls, 1, OPF_VAR_USE|OPF_VAR0|OPF_VAR_TYPE_DBL)
    OPCODE(DLOAD_1, ik_ls, 1, OPF_VAR_USE|OPF_VAR1|OPF_VAR_TYPE_DBL)
    OPCODE(DLOAD_2, ik_ls, 1, OPF_VAR_USE|OPF_VAR2|OPF_VAR_TYPE_DBL)
    OPCODE(DLOAD_3, ik_ls, 1, OPF_VAR_USE|OPF_VAR3|OPF_VAR_TYPE_DBL)
    OPCODE(ALOAD_0, ik_ls, 1, OPF_VAR_USE|OPF_VAR0|OPF_VAR_TYPE_OBJ)
    OPCODE(ALOAD_1, ik_ls, 1, OPF_VAR_USE|OPF_VAR1|OPF_VAR_TYPE_OBJ)
    OPCODE(ALOAD_2, ik_ls, 1, OPF_VAR_USE|OPF_VAR2|OPF_VAR_TYPE_OBJ)
    OPCODE(ALOAD_3, ik_ls, 1, OPF_VAR_USE|OPF_VAR3|OPF_VAR_TYPE_OBJ)

    OPCODE(IALOAD, ik_obj, 1, OPF_NONE)
    OPCODE(LALOAD, ik_obj, 1, OPF_NONE)
    OPCODE(FALOAD, ik_obj, 1, OPF_NONE)
    OPCODE(DALOAD, ik_obj, 1, OPF_NONE)
    OPCODE(AALOAD, ik_obj, 1, OPF_NONE)
    OPCODE(BALOAD, ik_obj, 1, OPF_NONE)
    OPCODE(CALOAD, ik_obj, 1, OPF_NONE)
    OPCODE(SALOAD, ik_obj, 1, OPF_NONE)

    OPCODE(ISTORE, ik_ls, 2, OPF_VAR_DEF|OPF_VAR_OP0|OPF_VAR_TYPE_I32)
    OPCODE(LSTORE, ik_ls, 2, OPF_VAR_DEF|OPF_VAR_OP0|OPF_VAR_TYPE_I64)
    OPCODE(FSTORE, ik_ls, 2, OPF_VAR_DEF|OPF_VAR_OP0|OPF_VAR_TYPE_FLT)
    OPCODE(DSTORE, ik_ls, 2, OPF_VAR_DEF|OPF_VAR_OP0|OPF_VAR_TYPE_DBL)
    OPCODE(ASTORE, ik_ls, 2, OPF_VAR_DEF|OPF_VAR_OP0|OPF_VAR_TYPE_OBJ)

    OPCODE(ISTORE_0, ik_ls, 1, OPF_VAR_DEF|OPF_VAR0|OPF_VAR_TYPE_I32)
    OPCODE(ISTORE_1, ik_ls, 1, OPF_VAR_DEF|OPF_VAR1|OPF_VAR_TYPE_I32)
    OPCODE(ISTORE_2, ik_ls, 1, OPF_VAR_DEF|OPF_VAR2|OPF_VAR_TYPE_I32)
    OPCODE(ISTORE_3, ik_ls, 1, OPF_VAR_DEF|OPF_VAR3|OPF_VAR_TYPE_I32)
    OPCODE(LSTORE_0, ik_ls, 1, OPF_VAR_DEF|OPF_VAR0|OPF_VAR_TYPE_I64)
    OPCODE(LSTORE_1, ik_ls, 1, OPF_VAR_DEF|OPF_VAR1|OPF_VAR_TYPE_I64)
    OPCODE(LSTORE_2, ik_ls, 1, OPF_VAR_DEF|OPF_VAR2|OPF_VAR_TYPE_I64)
    OPCODE(LSTORE_3, ik_ls, 1, OPF_VAR_DEF|OPF_VAR3|OPF_VAR_TYPE_I64)
    OPCODE(FSTORE_0, ik_ls, 1, OPF_VAR_DEF|OPF_VAR0|OPF_VAR_TYPE_FLT)
    OPCODE(FSTORE_1, ik_ls, 1, OPF_VAR_DEF|OPF_VAR1|OPF_VAR_TYPE_FLT)
    OPCODE(FSTORE_2, ik_ls, 1, OPF_VAR_DEF|OPF_VAR2|OPF_VAR_TYPE_FLT)
    OPCODE(FSTORE_3, ik_ls, 1, OPF_VAR_DEF|OPF_VAR3|OPF_VAR_TYPE_FLT)
    OPCODE(DSTORE_0, ik_ls, 1, OPF_VAR_DEF|OPF_VAR0|OPF_VAR_TYPE_DBL)
    OPCODE(DSTORE_1, ik_ls, 1, OPF_VAR_DEF|OPF_VAR1|OPF_VAR_TYPE_DBL)
    OPCODE(DSTORE_2, ik_ls, 1, OPF_VAR_DEF|OPF_VAR2|OPF_VAR_TYPE_DBL)
    OPCODE(DSTORE_3, ik_ls, 1, OPF_VAR_DEF|OPF_VAR3|OPF_VAR_TYPE_DBL)

    OPCODE(ASTORE_0, ik_ls, 1, OPF_VAR_DEF|OPF_VAR0|OPF_VAR_TYPE_OBJ)
    OPCODE(ASTORE_1, ik_ls, 1, OPF_VAR_DEF|OPF_VAR1|OPF_VAR_TYPE_OBJ)
    OPCODE(ASTORE_2, ik_ls, 1, OPF_VAR_DEF|OPF_VAR2|OPF_VAR_TYPE_OBJ)
    OPCODE(ASTORE_3, ik_ls, 1, OPF_VAR_DEF|OPF_VAR3|OPF_VAR_TYPE_OBJ)

    OPCODE(IASTORE, ik_obj, 1, OPF_NONE)
    OPCODE(LASTORE, ik_obj, 1, OPF_NONE)
    OPCODE(FASTORE, ik_obj, 1, OPF_NONE)
    OPCODE(DASTORE, ik_obj, 1, OPF_NONE)
    OPCODE(AASTORE, ik_obj, 1, OPF_NONE)
    OPCODE(BASTORE, ik_obj, 1, OPF_NONE)
    OPCODE(CASTORE, ik_obj, 1, OPF_NONE)
    OPCODE(SASTORE, ik_obj, 1, OPF_NONE)

    OPCODE(POP, ik_stack, 1, OPF_NONE)
    OPCODE(POP2, ik_stack, 1, OPF_NONE)
    OPCODE(DUP, ik_stack, 1, OPF_NONE)
    OPCODE(DUP_X1, ik_stack, 1, OPF_NONE)
    OPCODE(DUP_X2, ik_stack, 1, OPF_NONE)
    OPCODE(DUP2, ik_stack, 1, OPF_NONE)
    OPCODE(DUP2_X1, ik_stack, 1, OPF_NONE)
    OPCODE(DUP2_X2, ik_stack, 1, OPF_NONE)
    OPCODE(SWAP, ik_stack, 1, OPF_NONE)

    OPCODE(IADD, ik_a, 1, OPF_NONE)
    OPCODE(LADD, ik_a, 1, OPF_NONE)
    OPCODE(FADD, ik_a, 1, OPF_NONE)
    OPCODE(DADD, ik_a, 1, OPF_NONE)

    OPCODE(ISUB, ik_a, 1, OPF_NONE)
    OPCODE(LSUB, ik_a, 1, OPF_NONE)
    OPCODE(FSUB, ik_a, 1, OPF_NONE)
    OPCODE(DSUB, ik_a, 1, OPF_NONE)

    OPCODE(IMUL, ik_a, 1, OPF_NONE)
    OPCODE(LMUL, ik_a, 1, OPF_NONE)
    OPCODE(FMUL, ik_a, 1, OPF_NONE)
    OPCODE(DMUL, ik_a, 1, OPF_NONE)

    OPCODE(IDIV, ik_a, 1, OPF_NONE)
    OPCODE(LDIV, ik_a, 1, OPF_NONE)
    OPCODE(FDIV, ik_a, 1, OPF_NONE)
    OPCODE(DDIV, ik_a, 1, OPF_NONE)

    OPCODE(IREM, ik_a, 1, OPF_NONE)
    OPCODE(LREM, ik_a, 1, OPF_NONE)
    OPCODE(FREM, ik_a, 1, OPF_NONE)
    OPCODE(DREM, ik_a, 1, OPF_NONE)

    OPCODE(INEG, ik_a, 1, OPF_NONE)
    OPCODE(LNEG, ik_a, 1, OPF_NONE)
    OPCODE(FNEG, ik_a, 1, OPF_NONE)
    OPCODE(DNEG, ik_a, 1, OPF_NONE)

    OPCODE(ISHL, ik_a, 1, OPF_NONE)
    OPCODE(LSHL, ik_a, 1, OPF_NONE)
    OPCODE(ISHR, ik_a, 1, OPF_NONE)
    OPCODE(LSHR, ik_a, 1, OPF_NONE)

    OPCODE(IUSHR, ik_a, 1, OPF_NONE)
    OPCODE(LUSHR, ik_a, 1, OPF_NONE)

    OPCODE(IAND, ik_a, 1, OPF_NONE)
    OPCODE(LAND, ik_a, 1, OPF_NONE)

    OPCODE(IOR, ik_a, 1, OPF_NONE)
    OPCODE(LOR, ik_a, 1, OPF_NONE)

    OPCODE(IXOR, ik_a, 1, OPF_NONE)
    OPCODE(LXOR, ik_a, 1, OPF_NONE)

    OPCODE(IINC, ik_a, 3, OPF_VAR_DEF|OPF_VAR_USE|OPF_VAR_OP0|OPF_VAR_TYPE_I32)

    OPCODE(I2L, ik_cnv, 1, OPF_NONE)
    OPCODE(I2F, ik_cnv, 1, OPF_NONE)
    OPCODE(I2D, ik_cnv, 1, OPF_NONE)
    OPCODE(L2I, ik_cnv, 1, OPF_NONE)
    OPCODE(L2F, ik_cnv, 1, OPF_NONE)
    OPCODE(L2D, ik_cnv, 1, OPF_NONE)
    OPCODE(F2I, ik_cnv, 1, OPF_NONE)
    OPCODE(F2L, ik_cnv, 1, OPF_NONE)
    OPCODE(F2D, ik_cnv, 1, OPF_NONE)
    OPCODE(D2I, ik_cnv, 1, OPF_NONE)
    OPCODE(D2L, ik_cnv, 1, OPF_NONE)
    OPCODE(D2F, ik_cnv, 1, OPF_NONE)
    OPCODE(I2B, ik_cnv, 1, OPF_NONE)
    OPCODE(I2C, ik_cnv, 1, OPF_NONE)
    OPCODE(I2S, ik_cnv, 1, OPF_NONE)

    OPCODE(LCMP, ik_a, 1, OPF_NONE)
    OPCODE(FCMPL, ik_a, 1, OPF_NONE)
    OPCODE(FCMPG, ik_a, 1, OPF_NONE)
    OPCODE(DCMPL, ik_a, 1, OPF_NONE)
    OPCODE(DCMPG, ik_a, 1, OPF_NONE)

    OPCODE(IFEQ, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IFNE, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IFLT, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IFGE, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IFGT, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IFLE, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IF_ICMPEQ, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IF_ICMPNE, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IF_ICMPLT, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IF_ICMPGE, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IF_ICMPGT, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IF_ICMPLE, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IF_ACMPEQ, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IF_ACMPNE, ik_cf, 3, OPF_ENDS_BB)

    OPCODE(GOTO, ik_cf, 3, OPF_ENDS_BB|OPF_DEAD_END)
    OPCODE(JSR, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(RET, ik_cf, 2, OPF_ENDS_BB|OPF_DEAD_END) // no _USE here, its intentional

    OPCODE(TABLESWITCH, ik_cf, 0, OPF_ENDS_BB|OPF_DEAD_END)
    OPCODE(LOOKUPSWITCH, ik_cf, 0, OPF_ENDS_BB|OPF_DEAD_END)

    OPCODE(IRETURN, ik_meth, 1, OPF_ENDS_BB|OPF_DEAD_END|OPF_RETURN)
    OPCODE(LRETURN, ik_meth, 1, OPF_ENDS_BB|OPF_DEAD_END|OPF_RETURN)
    OPCODE(FRETURN, ik_meth, 1, OPF_ENDS_BB|OPF_DEAD_END|OPF_RETURN)
    OPCODE(DRETURN, ik_meth, 1, OPF_ENDS_BB|OPF_DEAD_END|OPF_RETURN)
    OPCODE(ARETURN, ik_meth, 1, OPF_ENDS_BB|OPF_DEAD_END|OPF_RETURN)
    OPCODE(RETURN,  ik_meth, 1, OPF_ENDS_BB|OPF_DEAD_END|OPF_RETURN)

    OPCODE(GETSTATIC, ik_obj, 3, OPF_NONE)
    OPCODE(PUTSTATIC, ik_obj, 3, OPF_NONE)
    OPCODE(GETFIELD, ik_obj, 3, OPF_NONE)
    OPCODE(PUTFIELD, ik_obj, 3, OPF_NONE)

    OPCODE(INVOKEVIRTUAL,   ik_meth, 3, OPF_NONE)
    OPCODE(INVOKESPECIAL,   ik_meth, 3, OPF_NONE)
    OPCODE(INVOKESTATIC,    ik_meth, 3, OPF_NONE)
    OPCODE(INVOKEINTERFACE, ik_meth, 5, OPF_NONE)

    UNDEFINED_OPCODE_HERE()

    OPCODE(NEW, ik_obj, 3, OPF_NONE)
    OPCODE(NEWARRAY, ik_obj, 2, OPF_NONE)
    OPCODE(ANEWARRAY, ik_obj, 3, OPF_NONE)

    OPCODE(ARRAYLENGTH, ik_obj, 1, OPF_NONE)
    OPCODE(ATHROW, ik_throw, 1, OPF_ENDS_BB|OPF_DEAD_END|OPF_NONE)
    OPCODE(CHECKCAST, ik_obj, 3, OPF_NONE)
    OPCODE(INSTANCEOF, ik_obj, 3, OPF_NONE)
    OPCODE(MONITORENTER, ik_obj, 1, OPF_NONE)
    OPCODE(MONITOREXIT, ik_obj, 1, OPF_NONE)
    OPCODE(WIDE, ik_none, 0, OPF_NONE)
    OPCODE(MULTIANEWARRAY, ik_obj, 4, OPF_NONE)

    OPCODE(IFNULL, ik_cf, 3, OPF_ENDS_BB)
    OPCODE(IFNONNULL, ik_cf, 3, OPF_ENDS_BB)

    OPCODE(GOTO_W, ik_cf, 5, OPF_ENDS_BB|OPF_DEAD_END)
    OPCODE(JSR_W, ik_cf, 5, OPF_ENDS_BB)
};

JTypeDesc jtypes[num_jtypes] = {
    {i8,    1,              0, "i8"     },
    {i16,   2,              0, "i16"    },
    {u16,   2,              0, "u16"    },
    {i32,   4,              0, "i32"    },
    {i64,   8,              0, "i64"    },
    {flt32, 4,              0, "flt"    },
    {dbl64, 8,              0, "dbl"    },
    {jobj,  sizeof(void*),  0, "jobj"   },
    {jvoid, 0,              0, "void"   },
    {jretAddr,sizeof(void*),0, "retAddr"},
};

#ifdef _DEBUG
static bool dbg_startup_check( void ) {
    // both 'instrs' and 'jtypes' must be arranged by appropriate 
    // enum type - JavaByteCodes and jtype.
    for( unsigned i=0; i<OPCODE_COUNT; i++ ) {
        assert( i == (unsigned)instrs[i].opcode );
    }
    for( unsigned i=0; i<num_jtypes; i++ ) {
        assert( i == (unsigned)jtypes[i].jt );
    }
    return true;
}

static bool dummy = dbg_startup_check();
#endif


jtype to_jtype(VM_Data_Type vmtype)
{
    //todo: can we simply table-tize it ?
    switch (vmtype) {
    case VM_DATA_TYPE_F8:       return dbl64;
    case VM_DATA_TYPE_F4:       return flt32;
    case VM_DATA_TYPE_INT64:    return i64;
    case VM_DATA_TYPE_INT32:    return i32;
    case VM_DATA_TYPE_CHAR:     return u16;
    case VM_DATA_TYPE_INT16:    return i16;
    case VM_DATA_TYPE_INT8:
    case VM_DATA_TYPE_BOOLEAN:  return i8;
    case VM_DATA_TYPE_VOID:     return jvoid;
    case VM_DATA_TYPE_STRING:
    case VM_DATA_TYPE_CLASS:
    case VM_DATA_TYPE_ARRAY:    return jobj;
    default:
        assert(false);
    }
    return i8;
};

};};    // ~namespace Jitrino::Jet

