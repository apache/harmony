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
 * @author Intel, George A. Timoshenko
 *
 */

#include <stdio.h>
#include "JavaByteCodeParser.h"
#include "Log.h"

namespace Jitrino {

static const char *opcode_names[256] = {
    "NOP", "ACONST_NULL", "ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2",
    "ICONST_3", "ICONST_4", "ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0",
    "FCONST_1", "FCONST_2", "DCONST_0", "DCONST_1", "BIPUSH", "SIPUSH",
    "LDC", "LDC_W", "LDC2_W", "ILOAD", "LLOAD", "FLOAD", "DLOAD", "ALOAD",
    "ILOAD_0", "ILOAD_1", "ILOAD_2", "ILOAD_3", "LLOAD_0", "LLOAD_1",
    "LLOAD_2",
    "LLOAD_3", "FLOAD_0", "FLOAD_1", "FLOAD_2", "FLOAD_3", "DLOAD_0",
    "DLOAD_1",
    "DLOAD_2", "DLOAD_3", "ALOAD_0", "ALOAD_1", "ALOAD_2", "ALOAD_3",
    "IALOAD",
    "LALOAD", "FALOAD", "DALOAD", "AALOAD", "BALOAD", "CALOAD", "SALOAD",
    "ISTORE", "LSTORE", "FSTORE", "DSTORE", "ASTORE", "ISTORE_0", "ISTORE_1",
    "ISTORE_2", "ISTORE_3", "LSTORE_0", "LSTORE_1", "LSTORE_2", "LSTORE_3",
    "FSTORE_0", "FSTORE_1", "FSTORE_2", "FSTORE_3", "DSTORE_0", "DSTORE_1",
    "DSTORE_2", "DSTORE_3", "ASTORE_0", "ASTORE_1", "ASTORE_2", "ASTORE_3",
    "IASTORE", "LASTORE", "FASTORE", "DASTORE", "AASTORE", "BASTORE",
    "CASTORE",
    "SASTORE", "POP", "POP2", "DUP", "DUP_X1", "DUP_X2", "DUP2", "DUP2_X1",
    "DUP2_X2", "SWAP", "IADD", "LADD", "FADD", "DADD", "ISUB", "LSUB", "FSUB",
    "DSUB", "IMUL", "LMUL", "FMUL", "DMUL", "IDIV", "LDIV", "FDIV", "DDIV",
    "IREM", "LREM", "FREM", "DREM", "INEG", "LNEG", "FNEG", "DNEG", "ISHL",
    "LSHL", "ISHR", "LSHR", "IUSHR", "LUSHR", "IAND", "LAND", "IOR", "LOR",
    "IXOR", "LXOR", "IINC", "I2L", "I2F", "I2D", "L2I", "L2F", "L2D", "F2I",
    "F2L", "F2D", "D2I", "D2L", "D2F", "I2B", "I2C", "I2S", "LCMP", "FCMPL",
    "FCMPG", "DCMPL", "DCMPG", "IFEQ", "IFNE", "IFLT", "IFGE", "IFGT", "IFLE",
    "IF_ICMPEQ", "IF_ICMPNE", "IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT",
    "IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE", "GOTO", "JSR", "RET",
    "TABLESWITCH",
    "LOOKUPSWITCH", "IRETURN", "LRETURN", "FRETURN", "DRETURN", "ARETURN",
    "RETURN", "GETSTATIC", "PUTSTATIC", "GETFIELD", "PUTFIELD",
    "INVOKEVIRTUAL",
    "INVOKESPECIAL", "INVOKESTATIC", "INVOKEINTERFACE", "_OPCODE_UNDEFINED",
    "NEW", "NEWARRAY", "ANEWARRAY", "ARRAYLENGTH", "ATHROW", "CHECKCAST",
    "INSTANCEOF", "MONITORENTER", "MONITOREXIT", "WIDE", "MULTIANEWARRAY",
    "IFNULL", "IFNONNULL", "GOTO_W", "JSR_W",
};

static char bytecodelength[] = { 
    1, // 0x00:    nop                          
    1, // 0x01:    aconst_null                  
    1, // 0x02:    iconst(-1)                     
    1, // 0x03:    iconst(0)                      
    1, // 0x04:    iconst(1)                      
    1, // 0x05:    iconst(2)                      
    1, // 0x06:    iconst(3)                      
    1, // 0x07:    iconst(4)                      
    1, // 0x08:    iconst(5)                      
    1, // 0x09:    lconst(0)                      
    1, // 0x0a:    lconst(1)                      
    1, // 0x0b:    fconst(0.0)                    
    1, // 0x0c:    fconst(1.0)                    
    1, // 0x0d:    fconst(2.0)                    
    1, // 0x0e:    dconst(0.0)                    
    1, // 0x0f:    dconst(1.0)                    
    // 0x1[0-f]
    2, // 0x10:    bipush
    3, // 0x11:    sipush            
    2, // 0x12:    ldc                
    3, // 0x13:    ldc               
    3, // 0x14:    ldc2              
    2, // 0x15:    iload              
    2, // 0x16:    lload              
    2, // 0x17:    fload              
    2, // 0x18:    dload              
    2, // 0x19:    aload              
    1, // 0x1a:    iload(0)                       
    1, // 0x1b:    iload(1)                       
    1, // 0x1c:    iload(2)                       
    1, // 0x1d:    iload(3)                       
    1, // 0x1e:    lload(0)                       
    1, // 0x1f:    lload(1)                       
    // 0x2[0-f]        unused: 0x24
    1, // 0x20:    lload(2)                       
    1, // 0x21:    lload(3)                       
    1, // 0x22:    fload(0)                       
    1, // 0x23:    fload(1)                       
    1, // 0x24:    fload(2)                       
    1, // 0x25:    fload(3)                       
    1, // 0x26:    dload(0)                       
    1, // 0x27:    dload(1)                       
    1, // 0x28:    dload(2)                       
    1, // 0x29:    dload(3)                       
    1, // 0x2a:    aload(0)                       
    1, // 0x2b:    aload(1)                       
    1, // 0x2c:    aload(2)                       
    1, // 0x2d:    aload(3)                       
    1, // 0x2e:    iaload                       
    1, // 0x2f:    laload                       
    // 0x3[0-f]
    1, // 0x30:    faload                       
    1, // 0x31:    daload                       
    1, // 0x32:    aaload                       
    1, // 0x33:    baload                       
    1, // 0x34:    caload                       
    1, // 0x35:    saload                       
    2, // 0x36:    istore         
    2, // 0x37:    lstore         
    2, // 0x38:    fstore         
    2, // 0x39:    dstore         
    2, // 0x3a:    astore         
    1, // 0x3b:    istore(0)                  
    1, // 0x3c:    istore(1)                  
    1, // 0x3d:    istore(2)                  
    1, // 0x3e:    istore(3)                  
    1, // 0x3f:    lstore(0)                  
    // 0x4[0-f]
    1, // 0x40:    lstore(1)                  
    1, // 0x41:    lstore(2)                  
    1, // 0x42:    lstore(3)                  
    1, // 0x43:    fstore(0)                  
    1, // 0x44:    fstore(1)                  
    1, // 0x45:    fstore(2)                  
    1, // 0x46:    fstore(3)                  
    1, // 0x47:    dstore(0)                  
    1, // 0x48:    dstore(1)                  
    1, // 0x49:    dstore(2)                  
    1, // 0x4a:    dstore(3)                  
    1, // 0x4b:    astore(0)                  
    1, // 0x4c:    astore(1)                  
    1, // 0x4d:    astore(2)                  
    1, // 0x4e:    astore(3)                  
    1, // 0x4f:    iastore                      
    // 0x5[0-f]
    1, // 0x50:    lastore                      
    1, // 0x51:    fastore                      
    1, // 0x52:    dastore                      
    1, // 0x53:    aastore                      
    1, // 0x54:    bastore                      
    1, // 0x55:    castore                      
    1, // 0x56:    sastore                      
    1, // 0x57:    pop                          
    1, // 0x58:    pop2                         
    1, // 0x59:    dup                          
    1, // 0x5a:    dup_x1                       
    1, // 0x5b:    dup_x2                       
    1, // 0x5c:    dup2                         
    1, // 0x5d:    dup2_x1                      
    1, // 0x5e:    dup2_x2                      
    1, // 0x5f:    swap                         
    // 0x6[0-f]        
    1, // 0x60:    iadd                         
    1, // 0x61:    ladd                         
    1, // 0x62:    fadd                         
    1, // 0x63:    dadd                         
    1, // 0x64:    isub                         
    1, // 0x65:    lsub                         
    1, // 0x66:    fsub                         
    1, // 0x67:    dsub                         
    1, // 0x68:    imul                         
    1, // 0x69:    lmul                         
    1, // 0x6a:    fmul                         
    1, // 0x6b:    dmul                         
    1, // 0x6c:    idiv                         
    1, // 0x6d:    ldiv                         
    1, // 0x6e:    fdiv                         
    1, // 0x6f:    ddiv                         
    // 0x7[0-f]
    1, // 0x70:    irem                         
    1, // 0x71:    lrem                         
    1, // 0x72:    frem                         
    1, // 0x73:    drem                         
    1, // 0x74:    ineg                         
    1, // 0x75:    lneg                         
    1, // 0x76:    fneg                         
    1, // 0x77:    dneg                         
    1, // 0x78:    ishl                         
    1, // 0x79:    lshl                         
    1, // 0x7a:    ishr                         
    1, // 0x7b:    lshr                         
    1, // 0x7c:    iushr                        
    1, // 0x7d:    lushr                        
    1, // 0x7e:    iand                         
    1, // 0x7f:    land                         
    // 0x8[0-f]
    1, // 0x80:    ior                          
    1, // 0x81:    lor                          
    1, // 0x82:    ixor                         
    1, // 0x83:    lxor                         
    3, // 0x84:    iinc
    1, // 0x85:    i2l                          
    1, // 0x86:    i2f                          
    1, // 0x87:    i2d                          
    1, // 0x88:    l2i                          
    1, // 0x89:    l2f                          
    1, // 0x8a:    l2d                          
    1, // 0x8b:    f2i                          
    1, // 0x8c:    f2l                          
    1, // 0x8d:    f2d                          
    1, // 0x8e:    d2i                          
    1, // 0x8f:    d2l                          
    // 0x9
    1, // 0x90:    d2f                          
    1, // 0x91:    i2b                          
    1, // 0x92:    i2c                          
    1, // 0x93:    i2s                          
    1, // 0x94:    lcmp                         
    1, // 0x95:    fcmpl                        
    1, // 0x96:    fcmpg                        
    1, // 0x97:    dcmpl                        
    1, // 0x98:    dcmpg                        
    3, // 0x99:    ifeq
    3, // 0x9a:    ifne
    3, // 0x9b:    iflt
    3, // 0x9c:    ifge
    3, // 0x9d:    ifgt
    3, // 0x9e:    ifle
    3, // 0x9f:    if_icmpeq   

    3, // 0xa0:    if_ifcmpne
    3, // 0xa1:    if_icmplt
    3, // 0xa2:    if_icmpge
    3, // 0xa3:    if_icmpgt
    3, // 0xa4:    if_icmple
    3, // 0xa5:    if_acmpeq
    3, // 0xa6:    if_acmpne
    3, // 0xa7:    goto 
    3, // 0xa8:    jsr
    2, // 0xa9:    ret
    0, // 0xaa:    switch
    0, // 0xab:    lookup
    1, // 0xac:    ireturn          
    1, // 0xad:    lreturn          
    1, // 0xae:    freturn          
    1, // 0xaf:    dreturn          
    // 0xb[0-f]         unused: 0xba
    1, // 0xb0:    areturn          
    1, // 0xb1:    return_          
    3, // 0xb2:    getstatic                     
    3, // 0xb3:    putstatic                     
    3, // 0xb4:    getfield                      
    3, // 0xb5:    putfield                      
    3, // 0xb6:    invokevirtual                 
    3, // 0xb7:    invokespecial                 
    3, // 0xb8:    invokestatic                  
    5, // 0xb9:    invokeinterface
    0, // 0xba;    unused
    3, // 0xbb:    new_                          
    2, // 0xbc:    newarray                       
    3, // 0xbd:    anewarray                     
    1, // 0xbe:    arraylength                              
    1, // 0xbf:    athrow              
    // 0xc[0-f]    unused: 0xc[a-f]
    3, // 0xc0:    checkcast                     
    3, // 0xc1:    instanceof
    1, // 0xc2:    monitorenter                             
    1, // 0xc3:    monitorexit                              
    0, // 0xc4:    wide
    4, // 0xc5:    multianewarray
    3, // 0xc6:    ifnull
    3, // 0xc7:    ifnonnull
    5, // 0xc8:    gotolong
    5, // 0xc9:    jsrlong
    };


//
// big-endian ordering
//
int16    si16(const U_8* bcp)    {
    return (((int16)(bcp)[0] << 8) | bcp[1]);
}
uint16    su16(const U_8* bcp)    {
    return (((uint16)(bcp)[0] << 8)| bcp[1]);
}
I_32    si32(const U_8* bcp)    {
    return (((U_32)(bcp)[0] << 24) | 
            ((U_32)(bcp)[1] << 16) |
            ((U_32)(bcp)[2] << 8)  |
            bcp[3]);
}
uint64    si64(const U_8* bcp)    {
    return (((uint64)bcp[0] << 56) |
            ((uint64)bcp[1] << 48) |
            ((uint64)bcp[2] << 40) |
            ((uint64)bcp[3] << 32) |
            ((uint64)bcp[4] << 24) |
            ((uint64)bcp[5] << 16) |
            ((uint64)bcp[6] <<  8) |
            ((uint64)bcp[7])
            );
} //si64

//
//
bool 
JavaByteCodeParserCallback::parseByteCode(const U_8* byteCodes,U_32 off) {
    bool linearPassEnd = false;
    currentOffset = off;
    const U_8* bcp = byteCodes + off;

    // find length of the byte code instruction
    U_32 len = 0; 
    const U_8  opcode = *bcp;
    assert (opcode <= 0xc9);
    if (Log::isEnabled()) {
        Log::out()<<"---------------------PARSING BYTECODE: "<<opcode_names[opcode]<<"        bc-offset="<<off<<std::endl;
    }


    len = bytecodelength[opcode];
    if (len == 0) { // variable length cases
        switch (opcode) {
        case 0xaa: {   
           JavaSwitchTargetsIter switchIter(bcp,off);
           len = switchIter.getLength();
           break;
        }
        case 0xab: {
           JavaLookupSwitchTargetsIter switchIter(bcp,off);
           len = switchIter.getLength();    
           break;
        }
        case 0xc4:     // wide
           if ((su8(bcp+1)) == 0x84) // iinc
              len = 6;
           else
              len = 4;
           break;
        default:
           assert(0);
        }
    }
    offset(off);
    if (isLinearPass) {
        if (prepassVisited && ! prepassVisited->getBit(off)) {
            nextOffset = currentOffset + len;
            return true;
        }
    }


    switch (opcode) {
    // 0x0[0-f]
    case 0x00:    nop();                          break;
    case 0x01:    aconst_null();                  break;
    case 0x02:    iconst(-1);                     break;
    case 0x03:    iconst(0);                      break;
    case 0x04:    iconst(1);                      break;
    case 0x05:    iconst(2);                      break;
    case 0x06:    iconst(3);                      break;
    case 0x07:    iconst(4);                      break;
    case 0x08:    iconst(5);                      break;
    case 0x09:    lconst(0);                      break;
    case 0x0a:    lconst(1);                      break;
    case 0x0b:    fconst(0.0);                    break;
    case 0x0c:    fconst(1.0);                    break;
    case 0x0d:    fconst(2.0);                    break;
    case 0x0e:    dconst(0.0);                    break;
    case 0x0f:    dconst(1.0);                    break;
    // 0x1[0-f]
    case 0x10:    bipush(si8(bcp+1));             break;
    case 0x11:    sipush(si16(bcp+1));            break;
    case 0x12:    ldc(su8(bcp+1));                break;
    case 0x13:    ldc(su16(bcp+1));               break;
    case 0x14:    ldc2(su16(bcp+1));              break;
    case 0x15:    iload(su8(bcp+1));              break;
    case 0x16:    lload(su8(bcp+1));              break;
    case 0x17:    fload(su8(bcp+1));              break;
    case 0x18:    dload(su8(bcp+1));              break;
    case 0x19:    aload(su8(bcp+1));              break;
    case 0x1a:    iload(0);                       break;
    case 0x1b:    iload(1);                       break;
    case 0x1c:    iload(2);                       break;
    case 0x1d:    iload(3);                       break;
    case 0x1e:    lload(0);                       break;
    case 0x1f:    lload(1);                       break;
    // 0x2[0-f]        unused: 0x24
    case 0x20:    lload(2);                       break;
    case 0x21:    lload(3);                       break;
    case 0x22:    fload(0);                       break;
    case 0x23:    fload(1);                       break;
    case 0x24:    fload(2);                       break;
    case 0x25:    fload(3);                       break;
    case 0x26:    dload(0);                       break;
    case 0x27:    dload(1);                       break;
    case 0x28:    dload(2);                       break;
    case 0x29:    dload(3);                       break;
    case 0x2a:    aload(0);                       break;
    case 0x2b:    aload(1);                       break;
    case 0x2c:    aload(2);                       break;
    case 0x2d:    aload(3);                       break;
    case 0x2e:    iaload();                       break;
    case 0x2f:    laload();                       break;
    // 0x3[0-f]
    case 0x30:    faload();                       break;
    case 0x31:    daload();                       break;
    case 0x32:    aaload();                       break;
    case 0x33:    baload();                       break;
    case 0x34:    caload();                       break;
    case 0x35:    saload();                       break;
    case 0x36:    istore(su8(bcp+1),off);         break;
    case 0x37:    lstore(su8(bcp+1),off);         break;
    case 0x38:    fstore(su8(bcp+1),off);         break;
    case 0x39:    dstore(su8(bcp+1),off);         break;
    case 0x3a:    astore(su8(bcp+1),off);         break;
    case 0x3b:    istore(0,off);                  break;
    case 0x3c:    istore(1,off);                  break;
    case 0x3d:    istore(2,off);                  break;
    case 0x3e:    istore(3,off);                  break;
    case 0x3f:    lstore(0,off);                  break;
    // 0x4[0-f]
    case 0x40:    lstore(1,off);                  break;
    case 0x41:    lstore(2,off);                  break;
    case 0x42:    lstore(3,off);                  break;
    case 0x43:    fstore(0,off);                  break;
    case 0x44:    fstore(1,off);                  break;
    case 0x45:    fstore(2,off);                  break;
    case 0x46:    fstore(3,off);                  break;
    case 0x47:    dstore(0,off);                  break;
    case 0x48:    dstore(1,off);                  break;
    case 0x49:    dstore(2,off);                  break;
    case 0x4a:    dstore(3,off);                  break;
    case 0x4b:    astore(0,off);                  break;
    case 0x4c:    astore(1,off);                  break;
    case 0x4d:    astore(2,off);                  break;
    case 0x4e:    astore(3,off);                  break;
    case 0x4f:    iastore();                      break;
    // 0x5[0-f]
    case 0x50:    lastore();                      break;
    case 0x51:    fastore();                      break;
    case 0x52:    dastore();                      break;
    case 0x53:    aastore();                      break;
    case 0x54:    bastore();                      break;
    case 0x55:    castore();                      break;
    case 0x56:    sastore();                      break;
    case 0x57:    pop();                          break;
    case 0x58:    pop2();                         break;
    case 0x59:    dup();                          break;
    case 0x5a:    dup_x1();                       break;
    case 0x5b:    dup_x2();                       break;
    case 0x5c:    dup2();                         break;
    case 0x5d:    dup2_x1();                      break;
    case 0x5e:    dup2_x2();                      break;
    case 0x5f:    swap();                         break;
    // 0x6[0-f]        
    case 0x60:    iadd();                         break;
    case 0x61:    ladd();                         break;
    case 0x62:    fadd();                         break;
    case 0x63:    dadd();                         break;
    case 0x64:    isub();                         break;
    case 0x65:    lsub();                         break;
    case 0x66:    fsub();                         break;
    case 0x67:    dsub();                         break;
    case 0x68:    imul();                         break;
    case 0x69:    lmul();                         break;
    case 0x6a:    fmul();                         break;
    case 0x6b:    dmul();                         break;
    case 0x6c:    idiv();                         break;
    case 0x6d:    ldiv();                         break;
    case 0x6e:    fdiv();                         break;
    case 0x6f:    ddiv();                         break;
    // 0x7[0-f]
    case 0x70:    irem();                         break;
    case 0x71:    lrem();                         break;
    case 0x72:    frem();                         break;
    case 0x73:    drem();                         break;
    case 0x74:    ineg();                         break;
    case 0x75:    lneg();                         break;
    case 0x76:    fneg();                         break;
    case 0x77:    dneg();                         break;
    case 0x78:    ishl();                         break;
    case 0x79:    lshl();                         break;
    case 0x7a:    ishr();                         break;
    case 0x7b:    lshr();                         break;
    case 0x7c:    iushr();                        break;
    case 0x7d:    lushr();                        break;
    case 0x7e:    iand();                         break;
    case 0x7f:    land();                         break;
    // 0x8[0-f]
    case 0x80:    ior();                          break;
    case 0x81:    lor();                          break;
    case 0x82:    ixor();                         break;
    case 0x83:    lxor();                         break;
    case 0x84:    iinc(su8(bcp+1),si8(bcp+2));    break;
    case 0x85:    i2l();                          break;
    case 0x86:    i2f();                          break;
    case 0x87:    i2d();                          break;
    case 0x88:    l2i();                          break;
    case 0x89:    l2f();                          break;
    case 0x8a:    l2d();                          break;
    case 0x8b:    f2i();                          break;
    case 0x8c:    f2l();                          break;
    case 0x8d:    f2d();                          break;
    case 0x8e:    d2i();                          break;
    case 0x8f:    d2l();                          break;
    case 0x90:    d2f();                          break;
    case 0x91:    i2b();                          break;
    case 0x92:    i2c();                          break;
    case 0x93:    i2s();                          break;
    case 0x94:    lcmp();                         break;
    case 0x95:    fcmpl();                        break;
    case 0x96:    fcmpg();                        break;
    case 0x97:    dcmpl();                        break;
    case 0x98:    dcmpg();                        break;

    case 0x99:    
        ifeq(off+si16(bcp+1),off+3);        
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0x9a:    
        ifne(off+si16(bcp+1),off+3);        
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0x9b:    
        iflt(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0x9c:    
        ifge(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0x9d:    
        ifgt(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0x9e:    
        ifle(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0x9f:    
        if_icmpeq(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    // 0xa[0-f]    
    case 0xa0:    
        if_icmpne(off+si16(bcp+1),off+3);    
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0xa1:    
        if_icmplt(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0xa2:    
        if_icmpge(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0xa3:
        if_icmpgt(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0xa4:    
        if_icmple(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0xa5:    
        if_acmpeq(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0xa6:    
        if_acmpne(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0xa7:    
        goto_(off+si16(bcp+1),off+3);
        linearPassEnd = true;
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0xa8:    
        jsr(off+si16(bcp+1),off+3);
        linearPassEnd = true; 
        if (labelStack != NULL) {
            // If labelStack is stack then the order of pushes should be reverted.
            // For now it is line. (labelStack is a Queue)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
            labelStack->push((U_8*)byteCodes + off+3);
        }
        break;
    case 0xa9:    
        ret(su8(bcp+1), byteCodes);    
        linearPassEnd = true;    
        break;
    case 0xaa:    
    {
       JavaSwitchTargetsIter switchIter(bcp,off);
       tableswitch(&switchIter);
       linearPassEnd = true;
        if (labelStack != NULL) {
            JavaSwitchTargetsIter switchIter2(bcp,off);
            while (switchIter2.hasNext()) {
                labelStack->push((U_8*)byteCodes + switchIter2.getNextTarget());
            }
            labelStack->push((U_8*)byteCodes + switchIter2.getDefaultTarget());
        }
        break;
    }
    case 0xab:
    {
        JavaLookupSwitchTargetsIter switchIter(bcp,off);
        lookupswitch(&switchIter);
        linearPassEnd = true;
        if (labelStack != NULL) {
            JavaLookupSwitchTargetsIter switchIter2(bcp,off);
            while (switchIter2.hasNext()) {
                U_32 key;
                labelStack->push((U_8*)byteCodes + switchIter2.getNextTarget(&key));
            }
            labelStack->push((U_8*)byteCodes + switchIter2.getDefaultTarget());
        }
        break;
    }
    case 0xac:    ireturn(off); linearPassEnd = true;         break;
    case 0xad:    lreturn(off); linearPassEnd = true;         break;
    case 0xae:    freturn(off); linearPassEnd = true;         break;
    case 0xaf:    dreturn(off); linearPassEnd = true;         break;
    // 0xb[0-f]         unused: 0xba
    case 0xb0:    areturn(off); linearPassEnd = true;         break;
    case 0xb1:    return_(off); linearPassEnd = true;         break;
    case 0xb2:    getstatic(su16(bcp+1));                     break;
    case 0xb3:    putstatic(su16(bcp+1));                     break;
    case 0xb4:    getfield(su16(bcp+1));                      break;
    case 0xb5:    putfield(su16(bcp+1));                      break;
    case 0xb6:    invokevirtual(su16(bcp+1));                 break;
    case 0xb7:    invokespecial(su16(bcp+1));                 break;
    case 0xb8:    invokestatic(su16(bcp+1));                  break;
    case 0xb9:    invokeinterface(su16(bcp+1),su8(bcp+3));    break;
    case 0xbb:    new_(su16(bcp+1));                          break;
    case 0xbc:    newarray(su8(bcp+1));                       break;
    case 0xbd:    anewarray(su16(bcp+1));                     break;
    case 0xbe:    arraylength();                              break;
    case 0xbf:    athrow();    linearPassEnd = true;          break;
    // 0xc[0-f]    unused: 0xc[a-f]
    case 0xc0:    checkcast(su16(bcp+1));                     break;
    case 0xc1:    len = instanceof(bcp,su16(bcp+1),off);      break;
    case 0xc2:    monitorenter();                             break;
    case 0xc3:    monitorexit();                              break;
    case 0xc4:    
    {
        // wide
        switch (su8(bcp+1)) {
        case 0x15: iload(su16(bcp+2));                        break;
        case 0x16: lload(su16(bcp+2));                        break;
        case 0x17: fload(su16(bcp+2));                        break;
        case 0x18: dload(su16(bcp+2));                        break;
        case 0x19: aload(su16(bcp+2));                        break;
        case 0x36: istore(su16(bcp+2),off);                   break;
        case 0x37: lstore(su16(bcp+2),off);                   break;
        case 0x38: fstore(su16(bcp+2),off);                   break;
        case 0x39: dstore(su16(bcp+2),off);                   break;
        case 0x3a: astore(su16(bcp+2),off);                   break;
        case 0xa9: ret(su16(bcp+2), byteCodes);    linearPassEnd = true; break;
        case 0x84: iinc(su16(bcp+2),si16(bcp+4));             break;
        default:
            assert(0);
            return false;
        }
    }
    break;
    case 0xc5:    multianewarray(su16(bcp+1),su8(bcp+3));     break;
    case 0xc6:    
        ifnull(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0xc7:    
        ifnonnull(off+si16(bcp+1),off+3);
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si16(bcp+1));
        break;
    case 0xc8:    
        goto_(off+si32(bcp+1),off+5);
        linearPassEnd = true;
        if (labelStack != NULL)
            labelStack->push((U_8*)byteCodes + off+si32(bcp+1));
        break;
    case 0xc9:    
        jsr(off+si32(bcp+1),off+5);
        linearPassEnd = true;
        if (labelStack != NULL) {
            // If labelStack is stack then the order of pushes should be reverted.
            // For now it is line. (labelStack is a Queue)
            labelStack->push((U_8*)byteCodes + off+si32(bcp+1));
            labelStack->push((U_8*)byteCodes + off+5);
        }
        break;
    default:
        assert(0);
        return false;
    }

    offset_done(off);

    if (isLinearPass == true) {
        nextOffset = currentOffset + len;
        return true;
    }
    linearPassDone = linearPassEnd;
    if (linearPassEnd == false) {
        // continue with next instruction,
        // but only if it has not been visited
        nextOffset = currentOffset + len;
        return true;
    } 
    // get next label by popping label stack
    while (!labelStack->isEmpty()) {
        nextOffset = (U_32) (labelStack->pop() - byteCodes);
        if (!visited->getBit(nextOffset)) {
            return true;
        }
    }
    return false;
}

} //namespace Jitrino 
