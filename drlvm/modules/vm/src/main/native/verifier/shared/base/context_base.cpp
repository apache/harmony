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
#include "open/vm_class_loading.h"

#include "verifier.h"
#include "context_base.h"

/**
 * Data for parsing for each instruction (length and flags).
 */
ParseInfo vf_Context_Base::parseTable[255] = {

    /* 0x00 OP_NOP */           { 1, 0 },

    /* 0x01 OP_ACONST_NULL */   { 1, 0 },
    /* 0x02 OP_ICONST_M1 */     { 1, 0 },
    /* 0x03 OP_ICONST_0 */      { 1, 0 },
    /* 0x04 OP_ICONST_1 */      { 1, 0 },
    /* 0x05 OP_ICONST_2 */      { 1, 0 },
    /* 0x06 OP_ICONST_3 */      { 1, 0 },
    /* 0x07 OP_ICONST_4 */      { 1, 0 },
    /* 0x08 OP_ICONST_5 */      { 1, 0 },
    /* 0x09 OP_LCONST_0 */      { 1, 0 },
    /* 0x0a OP_LCONST_1 */      { 1, 0 },
    /* 0x0b OP_FCONST_0 */      { 1, 0 },
    /* 0x0c OP_FCONST_1 */      { 1, 0 },
    /* 0x0d OP_FCONST_2 */      { 1, 0 },
    /* 0x0e OP_DCONST_0 */      { 1, 0 },
    /* 0x0f OP_DCONST_1 */      { 1, 0 },

    /* 0x10 OP_BIPUSH */        { 2, 0 },
    /* 0x11 OP_SIPUSH */        { 3, 0 },

    /* 0x12 OP_LDC */           { 2, 0 },
    /* 0x13 OP_LDC_W */         { 3, 0 },
    /* 0x14 OP_LDC2_W */        { 3, 0 },

    /* 0x15 OP_ILOAD */         { 2, PI_CANWIDE },
    /* 0x16 OP_LLOAD */         { 2, PI_CANWIDE },
    /* 0x17 OP_FLOAD */         { 2, PI_CANWIDE },
    /* 0x18 OP_DLOAD */         { 2, PI_CANWIDE },
    /* 0x19 OP_ALOAD */         { 2, PI_CANWIDE },

    /* 0x1a OP_ILOAD_0 */       { 1, 0 },
    /* 0x1b OP_ILOAD_1 */       { 1, 0 },
    /* 0x1c OP_ILOAD_2 */       { 1, 0 },
    /* 0x1d OP_ILOAD_3 */       { 1, 0 },
    /* 0x1e OP_LLOAD_0 */       { 1, 0 },
    /* 0x1f OP_LLOAD_1 */       { 1, 0 },
    /* 0x20 OP_LLOAD_2 */       { 1, 0 },
    /* 0x21 OP_LLOAD_3 */       { 1, 0 },
    /* 0x22 OP_FLOAD_0 */       { 1, 0 },
    /* 0x23 OP_FLOAD_1 */       { 1, 0 },
    /* 0x24 OP_FLOAD_2 */       { 1, 0 },
    /* 0x25 OP_FLOAD_3 */       { 1, 0 },
    /* 0x26 OP_DLOAD_0 */       { 1, 0 },
    /* 0x27 OP_DLOAD_1 */       { 1, 0 },
    /* 0x28 OP_DLOAD_2 */       { 1, 0 },
    /* 0x29 OP_DLOAD_3 */       { 1, 0 },
    /* 0x2a OP_ALOAD_0 */       { 1, 0 },
    /* 0x2b OP_ALOAD_1 */       { 1, 0 },
    /* 0x2c OP_ALOAD_2 */       { 1, 0 },
    /* 0x2d OP_ALOAD_3 */       { 1, 0 },

    /* 0x2e OP_IALOAD */        { 1, 0 },
    /* 0x2f OP_LALOAD */        { 1, 0 },
    /* 0x30 OP_FALOAD */        { 1, 0 },
    /* 0x31 OP_DALOAD */        { 1, 0 },
    /* 0x32 OP_AALOAD */        { 1, 0 },
    /* 0x33 OP_BALOAD */        { 1, 0 },
    /* 0x34 OP_CALOAD */        { 1, 0 },
    /* 0x35 OP_SALOAD */        { 1, 0 },

    /* 0x36 OP_ISTORE */        { 2, PI_CANWIDE },
    /* 0x37 OP_LSTORE */        { 2, PI_CANWIDE },
    /* 0x38 OP_FSTORE */        { 2, PI_CANWIDE },
    /* 0x39 OP_DSTORE */        { 2, PI_CANWIDE },
    /* 0x3a OP_ASTORE */        { 2, PI_CANWIDE },

    /* 0x3b OP_ISTORE_0 */      { 1, 0 },
    /* 0x3c OP_ISTORE_1 */      { 1, 0 },
    /* 0x3d OP_ISTORE_2 */      { 1, 0 },
    /* 0x3e OP_ISTORE_3 */      { 1, 0 },
    /* 0x3f OP_LSTORE_0 */      { 1, 0 },
    /* 0x40 OP_LSTORE_1 */      { 1, 0 },
    /* 0x41 OP_LSTORE_2 */      { 1, 0 },
    /* 0x42 OP_LSTORE_3 */      { 1, 0 },
    /* 0x43 OP_FSTORE_0 */      { 1, 0 },
    /* 0x44 OP_FSTORE_1 */      { 1, 0 },
    /* 0x45 OP_FSTORE_2 */      { 1, 0 },
    /* 0x46 OP_FSTORE_3 */      { 1, 0 },
    /* 0x47 OP_DSTORE_0 */      { 1, 0 },
    /* 0x48 OP_DSTORE_1 */      { 1, 0 },
    /* 0x49 OP_DSTORE_2 */      { 1, 0 },
    /* 0x4a OP_DSTORE_3 */      { 1, 0 },
    /* 0x4b OP_ASTORE_0 */      { 1, 0 },
    /* 0x4c OP_ASTORE_1 */      { 1, 0 },
    /* 0x4d OP_ASTORE_2 */      { 1, 0 },
    /* 0x4e OP_ASTORE_3 */      { 1, 0 },

    /* 0x4f OP_IASTORE */       { 1, 0 },
    /* 0x50 OP_LASTORE */       { 1, 0 },
    /* 0x51 OP_FASTORE */       { 1, 0 },
    /* 0x52 OP_DASTORE */       { 1, 0 },
    /* 0x53 OP_AASTORE */       { 1, 0 },
    /* 0x54 OP_BASTORE */       { 1, 0 },
    /* 0x55 OP_CASTORE */       { 1, 0 },
    /* 0x56 OP_SASTORE */       { 1, 0 },

    /* 0x57 OP_POP */           { 1, 0 },
    /* 0x58 OP_POP2 */          { 1, 0 },

    /* 0x59 OP_DUP */           { 1, 0 },
    /* 0x5a OP_DUP_X1 */        { 1, 0 },
    /* 0x5b OP_DUP_X2 */        { 1, 0 },
    /* 0x5c OP_DUP2 */          { 1, 0 },
    /* 0x5d OP_DUP2_X1 */       { 1, 0 },
    /* 0x5e OP_DUP2_X2 */       { 1, 0 },

    /* 0x5f OP_SWAP */          { 1, 0 },

    /* 0x60 OP_IADD */          { 1, 0 },
    /* 0x61 OP_LADD */          { 1, 0 },
    /* 0x62 OP_FADD */          { 1, 0 },
    /* 0x63 OP_DADD */          { 1, 0 },
    /* 0x64 OP_ISUB */          { 1, 0 },
    /* 0x65 OP_LSUB */          { 1, 0 },
    /* 0x66 OP_FSUB */          { 1, 0 },
    /* 0x67 OP_DSUB */          { 1, 0 },
    /* 0x68 OP_IMUL */          { 1, 0 },
    /* 0x69 OP_LMUL */          { 1, 0 },
    /* 0x6a OP_FMUL */          { 1, 0 },
    /* 0x6b OP_DMUL */          { 1, 0 },
    /* 0x6c OP_IDIV */          { 1, 0 },
    /* 0x6d OP_LDIV */          { 1, 0 },
    /* 0x6e OP_FDIV */          { 1, 0 },
    /* 0x6f OP_DDIV */          { 1, 0 },
    /* 0x70 OP_IREM */          { 1, 0 },
    /* 0x71 OP_LREM */          { 1, 0 },
    /* 0x72 OP_FREM */          { 1, 0 },
    /* 0x73 OP_DREM */          { 1, 0 },
    /* 0x74 OP_INEG */          { 1, 0 },
    /* 0x75 OP_LNEG */          { 1, 0 },
    /* 0x76 OP_FNEG */          { 1, 0 },
    /* 0x77 OP_DNEG */          { 1, 0 },
    /* 0x78 OP_ISHL */          { 1, 0 },
    /* 0x79 OP_LSHL */          { 1, 0 },
    /* 0x7a OP_ISHR */          { 1, 0 },
    /* 0x7b OP_LSHR */          { 1, 0 },
    /* 0x7c OP_IUSHR */         { 1, 0 },
    /* 0x7d OP_LUSHR */         { 1, 0 },
    /* 0x7e OP_IAND */          { 1, 0 },
    /* 0x7f OP_LAND */          { 1, 0 },
    /* 0x80 OP_IOR */           { 1, 0 },
    /* 0x81 OP_LOR */           { 1, 0 },
    /* 0x82 OP_IXOR */          { 1, 0 },
    /* 0x83 OP_LXOR */          { 1, 0 },

    /* 0x84 OP_IINC */          { 3, PI_CANWIDE },

    /* 0x85 OP_I2L */           { 1, 0 },
    /* 0x86 OP_I2F */           { 1, 0 },
    /* 0x87 OP_I2D */           { 1, 0 },
    /* 0x88 OP_L2I */           { 1, 0 },
    /* 0x89 OP_L2F */           { 1, 0 },
    /* 0x8a OP_L2D */           { 1, 0 },
    /* 0x8b OP_F2I */           { 1, 0 },
    /* 0x8c OP_F2L */           { 1, 0 },
    /* 0x8d OP_F2D */           { 1, 0 },
    /* 0x8e OP_D2I */           { 1, 0 },
    /* 0x8f OP_D2L */           { 1, 0 },
    /* 0x90 OP_D2F */           { 1, 0 },
    /* 0x91 OP_I2B */           { 1, 0 },
    /* 0x92 OP_I2C */           { 1, 0 },
    /* 0x93 OP_I2S */           { 1, 0 },

    /* 0x94 OP_LCMP */          { 1, 0 },
    /* 0x95 OP_FCMPL */         { 1, 0 },
    /* 0x96 OP_FCMPG */         { 1, 0 },
    /* 0x97 OP_DCMPL */         { 1, 0 },
    /* 0x98 OP_DCMPG */         { 1, 0 },

    /* 0x99 OP_IFEQ */          { 3, PI_JUMP },
    /* 0x9a OP_IFNE */          { 3, PI_JUMP },
    /* 0x9b OP_IFLT */          { 3, PI_JUMP },
    /* 0x9c OP_IFGE */          { 3, PI_JUMP },
    /* 0x9d OP_IFGT */          { 3, PI_JUMP },
    /* 0x9e OP_IFLE */          { 3, PI_JUMP },
    /* 0x9f OP_IF_ICMPEQ */     { 3, PI_JUMP },
    /* 0xa0 OP_IF_ICMPNE */     { 3, PI_JUMP },
    /* 0xa1 OP_IF_ICMPLT */     { 3, PI_JUMP },
    /* 0xa2 OP_IF_ICMPGE */     { 3, PI_JUMP },
    /* 0xa3 OP_IF_ICMPGT */     { 3, PI_JUMP },
    /* 0xa4 OP_IF_ICMPLE */     { 3, PI_JUMP },
    /* 0xa5 OP_IF_ACMPEQ */     { 3, PI_JUMP },
    /* 0xa6 OP_IF_ACMPNE */     { 3, PI_JUMP },

    /* 0xa7 OP_GOTO */          { 3, PI_JUMP | PI_DIRECT },

    /* 0xa8 OP_JSR */           { 3, PI_JUMP},
    /* 0xa9 OP_RET */           { 2, PI_DIRECT | PI_CANWIDE },

    /* 0xaa OP_TABLESWITCH */   { 16, PI_SWITCH },
    /* 0xab OP_LOOKUPSWITCH */  { 9, PI_SWITCH },

    /* 0xac OP_IRETURN */       { 1, PI_DIRECT },
    /* 0xad OP_LRETURN */       { 1, PI_DIRECT },
    /* 0xae OP_FRETURN */       { 1, PI_DIRECT },
    /* 0xaf OP_DRETURN */       { 1, PI_DIRECT },
    /* 0xb0 OP_ARETURN */       { 1, PI_DIRECT },
    /* 0xb1 OP_RETURN */        { 1, PI_DIRECT },

    /* 0xb2 OP_GETSTATIC */     { 3, 0 },
    /* 0xb3 OP_PUTSTATIC */     { 3, 0 },
    /* 0xb4 OP_GETFIELD */      { 3, 0 },
    /* 0xb5 OP_PUTFIELD */      { 3, 0 },

    /* 0xb6 OP_INVOKEVIRTUAL */ { 3, 0 },
    /* 0xb7 OP_INVOKESPECIAL */ { 3, 0 },
    /* 0xb8 OP_INVOKESTATIC */  { 3, 0 },

    /* 0xb9 OP_INVOKEINTERFACE */ { 5, 0 },

    /* oxba XXX_UNUSED_XXX */   {0, 0},

    /* 0xbb OP_NEW */           { 3, 0 },
    /* 0xbc OP_NEWARRAY */      { 2, 0 },
    /* 0xbd OP_ANEWARRAY */     { 3, 0 },

    /* 0xbe OP_ARRAYLENGTH */   { 1, 0 },

    /* 0xbf OP_ATHROW */        { 1, PI_DIRECT },

    /* 0xc0 OP_CHECKCAST */     { 3, 0 },
    /* 0xc1 OP_INSTANCEOF */    { 3, 0 },

    /* 0xc2 OP_MONITORENTER */  { 1, 0 },
    /* 0xc3 OP_MONITOREXIT */   { 1, 0 },

    /* 0xc4 OP_WIDE */          { 2, 0 },

    /* 0xc5 OP_MULTIANEWARRAY */{ 4, 0 },

    /* 0xc6 OP_IFNULL */        { 3, PI_JUMP },
    /* 0xc7 OP_IFNONNULL */     { 3, PI_JUMP },

    /* 0xc8 OP_GOTO_W */        { 5, PI_JUMP | PI_DIRECT | PI_WIDEJUMP },
    /* 0xc9 OP_JSR_W */         { 5, PI_JUMP | PI_WIDEJUMP },
};

/**
 * Obtains the length of a compound instruction.
 */
int vf_Context_Base::instr_get_len_compound(Address instr, OpCode opcode) {
    if( opcode == OP_WIDE ) {
        ParseInfo &pi = instr_get_parse_info( (OpCode)m_bytecode[instr+1] );

        if( !(pi.flags & PI_CANWIDE) ) {
            // return some big value - error will occur later
            return 0x20000123;
        }

        return 2*pi.instr_min_len;
    }


    Address def_adr = (instr & (~3) ) + 4;
    if( opcode == OP_TABLESWITCH) {
        int lowbyte = read_uint32(m_bytecode + def_adr + 4);
        int hibyte = read_uint32(m_bytecode + def_adr + 8);

        // protect from integer overflow
        if( hibyte < lowbyte || hibyte - lowbyte > 0x20000000) {
            // return some big value - error will occur later
            return 0x20000123;
        }

        return def_adr + 12 + (hibyte - lowbyte + 1) * 4 - instr;
    } else {
        assert( opcode == OP_LOOKUPSWITCH );

        //minimal length of OP_LOOKUPSWITCH is 9 bytes, while its required value may exceed 9 bytes, have to check bounds
        if( (unsigned)def_adr + 8 > m_code_length ) {
            // return some big value - error will occur later
            return 0x20000123;
        }

        unsigned npairs = read_uint32(m_bytecode + def_adr + 4);

        // protect from integer overflow
        if( npairs > 0x20000000) {
            // return some big value - error will occur later
            return 0x20000123;
        }

        int next = def_adr + 8;

        if (npairs) {
            int old_value = read_uint32(m_bytecode + next);
            next += 8;
            // integer values must be sorted - verify
            for( unsigned i = 1; i < npairs; i++) {
                int new_value = read_uint32(m_bytecode + next);
                next += 8;
                if( old_value >= new_value ) {
                    // return some big value - error will occur later
                    return 0x20000123;
                }
                old_value = new_value;
            }
        }

        return next - instr;
    }
}

/*
* Set method constraints
*/
void vf_Context_Base::set_class_constraints() {
    if( !class_constraints ) return;

    vf_ClassLoaderData_t *cl_data;
    Class_Loader_Handle currentClassLoader = class_get_class_loader(k_class);

    // lock data modification
    class_loader_lock( currentClassLoader );
    cl_data =
        (vf_ClassLoaderData_t*)class_loader_get_verifier_data_ptr( currentClassLoader );

    // create class loader data
    if( cl_data == NULL ) {
        Memory *new_pool = new Memory;
        cl_data = (vf_ClassLoaderData_t*)new_pool->malloc(sizeof(vf_ClassLoaderData_t));
        cl_data->pool = new_pool;
        cl_data->hash = new vf_Hash();
        cl_data->string = new vf_Hash();
    }
    Memory **pool = &cl_data->pool;
    vf_Hash *hash = cl_data->hash;
    vf_Hash *string = cl_data->string;

    // create class hash entry
    vf_HashEntry_t *hash_entry = hash->NewHashEntry( class_get_name( k_class ) );

    for( vf_TypeConstraint *constraint = class_constraints;
        constraint;
        constraint = constraint->next )
    {
        // create new constraint
        vf_TypeConstraint *cc = (vf_TypeConstraint*)(*pool)->malloc(sizeof(vf_TypeConstraint));

        // set class constraint
        // create hash entry for target class
        cc->target = string->NewHashEntry( constraint->target )->key;
        // create hash entry for checked class
        cc->source = string->NewHashEntry( constraint->source )->key;

        cc->next = (vf_TypeConstraint*)hash_entry->data_ptr;
        hash_entry->data_ptr = cc;
    }

    // unlock data modification
    class_loader_set_verifier_data_ptr( currentClassLoader, cl_data );
    class_loader_unlock( currentClassLoader );
    return;
}
