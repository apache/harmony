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
#ifndef __INSTR_PROPS_H_
#define __INSTR_PROPS_H_

#include <assert.h>
#include "stackmap.h"

//
// instruction's possible characteristics (flags)
//
const U_8 PI_JUMP = 1;
const U_8 PI_WIDEJUMP = 2;
const U_8 PI_DIRECT = 4;
const U_8 PI_SWITCH = 8;
const U_8 PI_CANWIDE = 16;

struct ParseInfo {
    U_8 instr_min_len;   // length of the instruction with operands
    U_8 flags;           // union of appropriate masks, see above 
};


//
// instruction's opcodes
//
enum OpCode {
    OP_AALOAD            = 0x32,
    OP_AASTORE           = 0x53,
    OP_ACONST_NULL       = 0x01,
    OP_ALOAD             = 0x19,
    OP_ALOAD_0           = 0x2a,
    OP_ALOAD_1           = 0x2b,
    OP_ALOAD_2           = 0x2c,
    OP_ALOAD_3           = 0x2d,
    OP_ANEWARRAY         = 0xbd,
    OP_ARETURN           = 0xb0,
    OP_ARRAYLENGTH       = 0xbe,
    OP_ASTORE            = 0x3a,
    OP_ASTORE_0          = 0x4b,
    OP_ASTORE_1          = 0x4c,
    OP_ASTORE_2          = 0x4d,
    OP_ASTORE_3          = 0x4e,
    OP_ATHROW            = 0xbf,
    OP_BALOAD            = 0x33,
    OP_BASTORE           = 0x54,
    OP_BIPUSH            = 0x10,
    OP_CALOAD            = 0x34,
    OP_CASTORE           = 0x55,
    OP_CHECKCAST         = 0xc0,
    OP_D2F               = 0x90,
    OP_D2I               = 0x8e,
    OP_D2L               = 0x8f,
    OP_DADD              = 0x63,
    OP_DALOAD            = 0x31,
    OP_DASTORE           = 0x52,
    OP_DCMPG             = 0x98,
    OP_DCMPL             = 0x97,
    OP_DCONST_0          = 0x0e,
    OP_DCONST_1          = 0x0f,
    OP_DDIV              = 0x6f,
    OP_DLOAD             = 0x18,
    OP_DLOAD_0           = 0x26,
    OP_DLOAD_1           = 0x27,
    OP_DLOAD_2           = 0x28,
    OP_DLOAD_3           = 0x29,
    OP_DMUL              = 0x6b,
    OP_DNEG              = 0x77,
    OP_DREM              = 0x73,
    OP_DRETURN           = 0xaf,
    OP_DSTORE            = 0x39,
    OP_DSTORE_0          = 0x47,
    OP_DSTORE_1          = 0x48,
    OP_DSTORE_2          = 0x49,
    OP_DSTORE_3          = 0x4a,
    OP_DSUB              = 0x67,
    OP_DUP               = 0x59,
    OP_DUP_X1            = 0x5a,
    OP_DUP_X2            = 0x5b,
    OP_DUP2              = 0x5c,
    OP_DUP2_X1           = 0x5d,
    OP_DUP2_X2           = 0x5e,
    OP_F2D               = 0x8d,
    OP_F2I               = 0x8b,
    OP_F2L               = 0x8c,
    OP_FADD              = 0x62,
    OP_FALOAD            = 0x30,
    OP_FASTORE           = 0x51,
    OP_FCMPG             = 0x96,
    OP_FCMPL             = 0x95,
    OP_FCONST_0          = 0x0b,
    OP_FCONST_1          = 0x0c,
    OP_FCONST_2          = 0x0d,
    OP_FDIV              = 0x6e,
    OP_FLOAD             = 0x17,
    OP_FLOAD_0           = 0x22,
    OP_FLOAD_1           = 0x23,
    OP_FLOAD_2           = 0x24,
    OP_FLOAD_3           = 0x25,
    OP_FMUL              = 0x6a,
    OP_FNEG              = 0x76,
    OP_FREM              = 0x72,
    OP_FRETURN           = 0xae,
    OP_FSTORE            = 0x38,
    OP_FSTORE_0          = 0x43,
    OP_FSTORE_1          = 0x44,
    OP_FSTORE_2          = 0x45,
    OP_FSTORE_3          = 0x46,
    OP_FSUB              = 0x66,
    OP_GETFIELD          = 0xb4,
    OP_GETSTATIC         = 0xb2,
    OP_GOTO              = 0xa7,
    OP_GOTO_W            = 0xc8,
    OP_I2B               = 0x91,
    OP_I2C               = 0x92,
    OP_I2D               = 0x87,
    OP_I2F               = 0x86,
    OP_I2L               = 0x85,
    OP_I2S               = 0x93,
    OP_IADD              = 0x60,
    OP_IALOAD            = 0x2e,
    OP_IAND              = 0x7e,
    OP_IASTORE           = 0x4f,
    OP_ICONST_0          = 0x03,
    OP_ICONST_1          = 0x04,
    OP_ICONST_2          = 0x05,
    OP_ICONST_3          = 0x06,
    OP_ICONST_4          = 0x07,
    OP_ICONST_5          = 0x08,
    OP_ICONST_M1         = 0x02,
    OP_IDIV              = 0x6c,
    OP_IF_ACMPEQ         = 0xa5,
    OP_IF_ACMPNE         = 0xa6,
    OP_IF_ICMPEQ         = 0x9f,
    OP_IF_ICMPGE         = 0xa2,
    OP_IF_ICMPGT         = 0xa3,
    OP_IF_ICMPLE         = 0xa4,
    OP_IF_ICMPLT         = 0xa1,
    OP_IF_ICMPNE         = 0xa0,
    OP_IFEQ              = 0x99,
    OP_IFGE              = 0x9c,
    OP_IFGT              = 0x9d,
    OP_IFLE              = 0x9e,
    OP_IFLT              = 0x9b,
    OP_IFNE              = 0x9a,
    OP_IFNONNULL         = 0xc7,
    OP_IFNULL            = 0xc6,
    OP_IINC              = 0x84,
    OP_ILOAD             = 0x15,
    OP_ILOAD_0           = 0x1a,
    OP_ILOAD_1           = 0x1b,
    OP_ILOAD_2           = 0x1c,
    OP_ILOAD_3           = 0x1d,
    OP_IMUL              = 0x68,
    OP_INEG              = 0x74,
    OP_INSTANCEOF        = 0xc1,
    OP_INVOKEINTERFACE   = 0xb9,
    OP_INVOKESPECIAL     = 0xb7,
    OP_INVOKESTATIC      = 0xb8,
    OP_INVOKEVIRTUAL     = 0xb6,
    OP_IOR               = 0x80,
    OP_IREM              = 0x70,
    OP_IRETURN           = 0xac,
    OP_ISHL              = 0x78,
    OP_ISHR              = 0x7a,
    OP_ISTORE            = 0x36,
    OP_ISTORE_0          = 0x3b,
    OP_ISTORE_1          = 0x3c,
    OP_ISTORE_2          = 0x3d,
    OP_ISTORE_3          = 0x3e,
    OP_ISUB              = 0x64,
    OP_IUSHR             = 0x7c,
    OP_IXOR              = 0x82,
    OP_JSR               = 0xa8,
    OP_JSR_W             = 0xc9,
    OP_L2D               = 0x8a,
    OP_L2F               = 0x89,
    OP_L2I               = 0x88,
    OP_LADD              = 0x61,
    OP_LALOAD            = 0x2f,
    OP_LAND              = 0x7f,
    OP_LASTORE           = 0x50,
    OP_LCMP              = 0x94,
    OP_LCONST_0          = 0x09,
    OP_LCONST_1          = 0x0a,
    OP_LDC               = 0x12,
    OP_LDC_W             = 0x13,
    OP_LDC2_W            = 0x14,
    OP_LDIV              = 0x6d,
    OP_LLOAD             = 0x16,
    OP_LLOAD_0           = 0x1e,
    OP_LLOAD_1           = 0x1f,
    OP_LLOAD_2           = 0x20,
    OP_LLOAD_3           = 0x21,
    OP_LMUL              = 0x69,
    OP_LNEG              = 0x75,
    OP_LOOKUPSWITCH      = 0xab,
    OP_LOR               = 0x81,
    OP_LREM              = 0x71,
    OP_LRETURN           = 0xad,
    OP_LSHL              = 0x79,
    OP_LSHR              = 0x7b,
    OP_LSTORE            = 0x37,
    OP_LSTORE_0          = 0x3f,
    OP_LSTORE_1          = 0x40,
    OP_LSTORE_2          = 0x41,
    OP_LSTORE_3          = 0x42,
    OP_LSUB              = 0x65,
    OP_LUSHR             = 0x7d,
    OP_LXOR              = 0x83,
    OP_MONITORENTER      = 0xc2,
    OP_MONITOREXIT       = 0xc3,
    OP_MULTIANEWARRAY    = 0xc5,
    OP_NEW               = 0xbb,
    OP_NEWARRAY          = 0xbc,
    OP_NOP               = 0x00,
    OP_POP               = 0x57,
    OP_POP2              = 0x58,
    OP_PUTFIELD          = 0xb5,
    OP_PUTSTATIC         = 0xb3,
    OP_RET               = 0xa9,
    OP_RETURN            = 0xb1,
    OP_SALOAD            = 0x35,
    OP_SASTORE           = 0x56,
    OP_SIPUSH            = 0x11,
    OP_SWAP              = 0x5f,
    OP_TABLESWITCH       = 0xaa,
    OP_WIDE              = 0xc4,

    OP_XXX_UNUSED_XXX    = 0xba,
    OP_MAXCODE           = 0xc9,
};

//store properties (stackmaps, workmaps, etc) for each instruction
class InstrPropsBase {
private:

    //storage for all the data
    Memory *memory;

    //to avoid divisions we do bit AND with hash_mask when looking a starting index in hash table list
    int hash_mask;

public:
    //hash table to store data for instructions
    PropsHeadBase **propHashTable;

    //table size
    int hash_size;

    void init(Memory &mem, int code_len) {
        memory = &mem;

        //calcluate hash_size
        hash_size = 16;
        int clen = code_len >> 8;
        while( clen ) {
            hash_size = hash_size << 1;
            clen = clen >> 1;
        }

        hash_mask = hash_size - 1;
        propHashTable = (PropsHeadBase**)mem.calloc(hash_size * sizeof(PropsHeadBase*));
    }

    //return properties for the given instruction
    PropsHeadBase* getInstrProps(Address instr) {
        PropsHeadBase *pro = propHashTable[instr & hash_mask];
        while( pro && pro->instr != instr ) {
            pro = pro->next;
        }
        return pro;
    }

    //sets properties for the given instruction
    void setInstrProps(Address instr, PropsHeadBase *map) {
        //properties for the instruction don't exit yet
        assert(!getInstrProps(instr));

        int hash = instr & hash_mask;
        map->next = propHashTable[hash];
        map->instr = instr;
        propHashTable[hash] = map;

    }
};


#endif
