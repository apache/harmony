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
 * @author Intel, Konstantin M. Anisimov, Igor V. Chebykin
 *
 */

#include "MemoryAttribute.h"
#include "IpfCodeSelector.h"
#include "IpfIrPrinter.h"

namespace Jitrino {
namespace IPF {

#define HEAPBASE      (opndManager->getHeapBase())
#define HEAPBASEIMM   (opndManager->getHeapBaseImm())
#define VTABLEBASE    (opndManager->getVtableBase())
#define VTABLEBASEIMM (opndManager->getVtableBaseImm())

#define IMM32(o)   ((I_32)(((Opnd *)(o))->getValue()))
#define IMM64(o)   ((int64)(((Opnd *)(o))->getValue()))
#define IMM32U(o)  ((U_32)(((Opnd *)(o))->getValue()))
#define IMM64U(o)  ((uint64)(((Opnd *)(o))->getValue()))

// FP remainder internal helpers (temp solution to be optimized)
float   remF4   (float v0, float v1);
float   remF4   (float v0, float v1)   { 
    return (float)fmod((double)v0,(double)v1);
}

double  remF8   (double v0, double v1);
double  remF8   (double v0, double v1)  {
    return fmod(v0,v1);
} 

//===========================================================================//
// IpfInstCodeSelector
//===========================================================================//

IpfInstCodeSelector::IpfInstCodeSelector(Cfg                  &cfg_, 
                                         BbNode               &node_, 
                                         OpndVector           &opnds_,
                                         CompilationInterface &compilationInterface_) : 
    mm(cfg_.getMM()),
    cfg(cfg_), 
    node(node_), 
    opnds(opnds_),
    compilationInterface(compilationInterface_) {

    opndManager = cfg.getOpndManager();
    p0          = opndManager->getP0();
}

//----------------------------------------------------------------------------//
// InstructionCallback implementation
//----------------------------------------------------------------------------//

//----------------------------------------------------------------------------//
// Add numeric values

CG_OpndHandle *IpfInstCodeSelector::add(ArithmeticOp::Types opType,
                                        CG_OpndHandle       *src1,
                                        CG_OpndHandle       *src2) {

    IPF_LOG << "      add; opType=" << opType << endl;

    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src1)->isImm() && ((Opnd *)src2)->isImm()) {
        switch (opType) {
        case ArithmeticOp::I4:
        case ArithmeticOp::I:
            dst = opndManager->newImm(IMM32(src1) + IMM32(src2)); break;
        case ArithmeticOp::I8:
            dst = opndManager->newImm(IMM64(src1) + IMM64(src2)); break;
        default:
            IPF_ASSERT(0); dst = NULL; break;
        }
    } else {
        dst = opndManager->newRegOpnd(toOpndKind(opType), toDataKind(opType));
        add((RegOpnd *)dst, src1, src2);
    }
    return dst;
}

//----------------------------------------------------------------------------//
// Subtract numeric values

CG_OpndHandle *IpfInstCodeSelector::sub(ArithmeticOp::Types opType,
                                        CG_OpndHandle       *src1,
                                        CG_OpndHandle       *src2) {

    IPF_LOG << "      sub" << endl;

    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src1)->isImm() && ((Opnd *)src2)->isImm()) {
        switch (opType) {
        case ArithmeticOp::I4:
        case ArithmeticOp::I:
            dst = opndManager->newImm(IMM32(src1) - IMM32(src2)); break;
        case ArithmeticOp::I8:
            dst = opndManager->newImm(IMM64(src1) - IMM64(src2)); break;
        default:
            IPF_ASSERT(0); dst = NULL; break;
        }
    } else {
        dst = opndManager->newRegOpnd(toOpndKind(opType), toDataKind(opType));
        sub((RegOpnd *)dst, src1, src2);
    }
    return dst;
}

//----------------------------------------------------------------------------//
// Multiply numeric values

CG_OpndHandle *IpfInstCodeSelector::mul(ArithmeticOp::Types opType, 
                                        CG_OpndHandle       *src1_,
                                        CG_OpndHandle       *src2_) {

    IPF_LOG << "      mul" << endl;
    
    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src1_)->isImm() && ((Opnd *)src2_)->isImm()) {
        // imm is always integer
        switch (opType) {
        case ArithmeticOp::I4:
        case ArithmeticOp::I:
            dst = opndManager->newImm(IMM32(src1_) * IMM32(src2_)); break;
        case ArithmeticOp::I8:
            dst = opndManager->newImm(IMM64(src1_) * IMM64(src2_)); break;
        default:
            IPF_ASSERT(0); dst = NULL; break;
        }
    } else {
        RegOpnd *src1 = toRegOpnd(src1_);
        RegOpnd *src2 = toRegOpnd(src2_);
        RegOpnd *f0   = opndManager->getF0();

        dst  = opndManager->newRegOpnd(toOpndKind(opType), toDataKind(opType));;
        if (dst->isFloating()) {
            Completer cmplt = CMPLT_PC_DYNAMIC;
            
            switch (dst->getDataKind()) {
            case DATA_D: cmplt = CMPLT_PC_DOUBLE; break;
            case DATA_S: cmplt = CMPLT_PC_SINGLE; break;
            default: IPF_ERR << "bad data kind for float mul\n"; break;
            }
            addNewInst(INST_FMA, cmplt, p0, dst, src1, src2, f0);
        } else {
            xma(INST_XMA_L, (RegOpnd *)dst, src1, src2);
        }
    }
    return dst;
}

//----------------------------------------------------------------------------//
// Add integer to a reference

CG_OpndHandle *IpfInstCodeSelector::addRef(RefArithmeticOp::Types opType,
                                           CG_OpndHandle          *refSrc,
                                           CG_OpndHandle          *intSrc) {

    IPF_LOG << "      addRef" << endl;
    IPF_ASSERT(((Opnd *)refSrc)->isReg());

    RegOpnd *dst = opndManager->newRegOpnd(OPND_G_REG, toDataKind(opType));
    add(dst, refSrc, intSrc);
    return dst;
}

//----------------------------------------------------------------------------//
// Subtract integer from a reference

CG_OpndHandle *IpfInstCodeSelector::subRef(RefArithmeticOp::Types opType,
                                           CG_OpndHandle          *refSrc, 
                                           CG_OpndHandle          *intSrc) {

    IPF_LOG << "      subRef" << endl;
    IPF_ASSERT(((Opnd *)refSrc)->isReg());

    RegOpnd *dst = opndManager->newRegOpnd(OPND_G_REG, toDataKind(opType));
    sub(dst, refSrc, intSrc);
    return dst;
}

//----------------------------------------------------------------------------//
// Subtract reference from reference

CG_OpndHandle *IpfInstCodeSelector::diffRef(bool          ovf, 
                                            CG_OpndHandle *src1,
                                            CG_OpndHandle *src2) {

    IPF_LOG << "      diffRef" << endl;
    IPF_ASSERT(((Opnd *)src1)->isReg());
    IPF_ASSERT(((Opnd *)src2)->isReg());

    RegOpnd *dst = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);
    sub(dst, src1, src2);
    return dst;
}   


//----------------------------------------------------------------------------//
// Divide two numeric values

CG_OpndHandle *IpfInstCodeSelector::tau_div(DivOp::Types  opType,
                                            CG_OpndHandle *src1,
                                            CG_OpndHandle *src2,
                                            CG_OpndHandle *tau_src1NonZero) {

    IPF_LOG << "      tau_div" 
        << "; opType=" << opType
        << ", src1=" << IrPrinter::toString((Opnd *)src1)
        << ", src2=" << IrPrinter::toString((Opnd *)src2)
        << endl;

    Opnd *dst = NULL;
    if (ipfConstantFolding && ((Opnd *)src1)->isImm() && ((Opnd *)src2)->isImm() && IMM32(src2)!=0){
        // imm is always integer
        switch (opType) {
        case DivOp::I4: 
            dst = opndManager->newImm(IMM32(src1) / IMM32(src2)); break;
        case DivOp::U4:
            dst = opndManager->newImm(IMM32U(src1) / IMM32U(src2)); break;
        case DivOp::I:
        case DivOp::I8:
            dst = opndManager->newImm(IMM64(src1) / IMM64(src2)); break;
        case DivOp::U:
        case DivOp::U8:
            dst = opndManager->newImm(IMM64U(src1) / IMM64U(src2)); break;
        default:
            IPF_ASSERT(0);
            dst = NULL;
        }
    } else {
        dst = opndManager->newRegOpnd(toOpndKind(opType), toDataKind(opType));
    
        switch (opType) {
            case DivOp::I4: divInt((RegOpnd *)dst, toRegOpnd(src1), toRegOpnd(src2)); break;
            case DivOp::I :  
            case DivOp::I8: divLong((RegOpnd *)dst, toRegOpnd(src1), toRegOpnd(src2)); break;
            case DivOp::F :   
            case DivOp::D : divDouble((RegOpnd *)dst, src1, src2); break;
            case DivOp::S : divFloat ((RegOpnd *)dst, src1, src2); break;
            default       : IPF_ERR << "unexpected type " << opType << endl;
        }
    }
    return dst;
}

//----------------------------------------------------------------------------//
// Get remainder from the division of two numeric values
// On IPF computing integer remainder from division by zero does not result in hardware exception

CG_OpndHandle *IpfInstCodeSelector::tau_rem(DivOp::Types  opType,
                                            CG_OpndHandle *src1,
                                            CG_OpndHandle *src2,
                                            CG_OpndHandle *tau_src2NonZero) {

    IPF_LOG << "      tau_rem; opType=" << opType << endl;

    RegOpnd *dst = opndManager->newRegOpnd(toOpndKind(opType), toDataKind(opType));

    if (dst->isFloating()) {
        if (dst->getDataKind()==DATA_D) {
            divDouble(dst, src1, src2, true);
        } else {
            divFloat(dst, src1, src2, true);
        }
    } else {
        if (dst->getSize() > 4) {
            divLong(dst, toRegOpnd(src1), toRegOpnd(src2), true);
        } else {                   
            divInt (dst, toRegOpnd(src1), toRegOpnd(src2), true);
        }
    }

    return dst;
}

//----------------------------------------------------------------------------//
// Negate numeric value

CG_OpndHandle *IpfInstCodeSelector::neg(NegOp::Types  opType,
                                        CG_OpndHandle *src_) {

    IPF_LOG << "      neg" << endl;

    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src_)->isImm()) {
        switch (opType) {
        case NegOp::I4: 
            dst = opndManager->newImm((I_32)0 - IMM32(src_)); break;
        case NegOp::I:
        case NegOp::I8: 
            dst = opndManager->newImm(0 - IMM64(src_)); break;
        default: 
            IPF_ASSERT(0);
            dst = NULL;
        }
    } else {
        dst = opndManager->newRegOpnd(toOpndKind(opType), toDataKind(opType));
        RegOpnd *src = toRegOpnd(src_);
        RegOpnd *r0  = opndManager->getR0();
    
        if (dst->isFloating()) addNewInst(INST_FNEG, p0, dst, src);
        else                   addNewInst(INST_SUB, p0, dst, r0, src);
    }
    return dst;
}

//----------------------------------------------------------------------------//
// Min

CG_OpndHandle *IpfInstCodeSelector::min_op(NegOp::Types  opType,
                                           CG_OpndHandle *src1,
                                           CG_OpndHandle *src2) {

    IPF_LOG << "      min_op" << endl;

    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src1)->isImm() && ((Opnd *)src2)->isImm()) {
        switch (opType) {
        case NegOp::I4: 
            dst = opndManager->newImm(min(IMM32(src1), IMM32(src2))); break;
        case NegOp::I:
        case NegOp::I8: 
            dst = opndManager->newImm(min(IMM64(src1), IMM64(src2))); break;
        default: 
            IPF_ASSERT(0);
            dst = NULL;
        }
    } else {
        dst = opndManager->newRegOpnd(toOpndKind(opType), toDataKind(opType));
        minMax((RegOpnd *)dst, src1, src2, false);
    }
    return dst; 
}

//----------------------------------------------------------------------------//
// Max

CG_OpndHandle *IpfInstCodeSelector::max_op(NegOp::Types  opType,
                                           CG_OpndHandle *src1,
                                           CG_OpndHandle *src2) {

    IPF_LOG << "      max_op" << endl;
    
    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src1)->isImm() && ((Opnd *)src2)->isImm()) {
        switch (opType) {
        case NegOp::I4: 
            dst = opndManager->newImm(max(IMM32(src1), IMM32(src2))); break;
        case NegOp::I:
        case NegOp::I8: 
            dst = opndManager->newImm(max(IMM64(src1), IMM64(src2))); break;
        default: 
            IPF_ASSERT(0);
            dst = NULL;
        }
    } else {
        dst = opndManager->newRegOpnd(toOpndKind(opType), toDataKind(opType));
        minMax((RegOpnd *)dst, src1, src2, true);
    }
    return dst; 
}

//----------------------------------------------------------------------------//
// Abs

CG_OpndHandle *IpfInstCodeSelector::abs_op(NegOp::Types  opType,
                                           CG_OpndHandle *src_) {
    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src_)->isImm()) {
        switch (opType) {
        case NegOp::I4: 
            dst = opndManager->newImm(abs(IMM32(src_))); break;
        case NegOp::I:
        case NegOp::I8: 
            dst = opndManager->newImm(labs(IMM64(src_))); break;
        default: 
            IPF_ASSERT(0);
            dst = NULL;
        }
    } else {
        RegOpnd *src = toRegOpnd(src_);
        dst = opndManager->newRegOpnd(toOpndKind(opType), toDataKind(opType));
        
        if (dst->isFloating()) {
            // TODO: check all the peculiarities of Math.min/max
             addNewInst(INST_FABS, p0, dst, src);
        } else {
            // cmp.lt truePred, falsePred = src, 0
            // (truePred)  dst = src
            // (falsePred) dst = -src
            RegOpnd *truePred  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
            RegOpnd *falsePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
            RegOpnd *r0        = opndManager->getR0(src);
            
            addNewInst(INST_DEF, p0, dst);
            addNewInst(INST_DEF, p0, src);

            cmp(CMPLT_CMP_CREL_LT, truePred, falsePred, src, r0);
            addNewInst(INST_MOV, truePred, dst, src);
            addNewInst(INST_SUB, falsePred, dst, r0, src);
        }
    }
    return dst;
}

//----------------------------------------------------------------------------//
// Logical and

CG_OpndHandle *IpfInstCodeSelector::and_(IntegerOp::Types opType,
                                         CG_OpndHandle    *src1,
                                         CG_OpndHandle    *src2) {

    IPF_LOG << "      and_ " << endl;

    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src1)->isImm() && ((Opnd *)src2)->isImm()) {
        switch (opType) {
        case IntegerOp::I4:
            dst = opndManager->newImm(IMM32(src1) & IMM32(src2)); break;
        case IntegerOp::I8:
            dst = opndManager->newImm(IMM64(src1) & IMM64(src2)); break;
        case IntegerOp::I  : 
            dst = opndManager->newImm(IMM64U(src1) & IMM64U(src2)); break;
        default: 
            IPF_ASSERT(0);
            dst = NULL;
        }
    } else {
        dst = opndManager->newRegOpnd(OPND_G_REG, toDataKind(opType));
        binOp(INST_AND, (RegOpnd *)dst, src1, src2);
    }
    return dst; 
}

//----------------------------------------------------------------------------//
// Logical or

CG_OpndHandle *IpfInstCodeSelector::or_(IntegerOp::Types opType,
                                        CG_OpndHandle    *src1,
                                        CG_OpndHandle    *src2) {

    IPF_LOG << "      or_ " << endl;

    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src1)->isImm() && ((Opnd *)src2)->isImm()) {
        switch (opType) {
        case IntegerOp::I4:
            dst = opndManager->newImm(IMM32(src1) | IMM32(src2)); break;
        case IntegerOp::I8:
            dst = opndManager->newImm(IMM64(src1) | IMM64(src2)); break;
        case IntegerOp::I  : 
            dst = opndManager->newImm(IMM64U(src1) | IMM64U(src2)); break;
        default: 
            IPF_ASSERT(0);
            dst = NULL;
        }
    } else {
        dst = opndManager->newRegOpnd(OPND_G_REG, toDataKind(opType));
        binOp(INST_OR, (RegOpnd *)dst, src1, src2);
    }
    return dst; 
}

//----------------------------------------------------------------------------//
// Logical xor

CG_OpndHandle *IpfInstCodeSelector::xor_(IntegerOp::Types opType,
                                         CG_OpndHandle    *src1,
                                         CG_OpndHandle    *src2) {

    IPF_LOG << "      xor_ " << endl;

    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src1)->isImm() && ((Opnd *)src2)->isImm()) {
        switch (opType) {
        case IntegerOp::I4:
            dst = opndManager->newImm(IMM32(src1) ^ IMM32(src2)); break;
        case IntegerOp::I8:
            dst = opndManager->newImm(IMM64(src1) ^ IMM64(src2)); break;
        case IntegerOp::I  : 
            dst = opndManager->newImm(IMM64U(src1) ^ IMM64U(src2)); break;
        default: 
            IPF_ASSERT(0);
            dst = NULL;
        }
    } else {
        dst = opndManager->newRegOpnd(OPND_G_REG, toDataKind(opType));
        binOp(INST_XOR, (RegOpnd *)dst, src1, src2);
    }
    return dst; 
}

//----------------------------------------------------------------------------//
// Logical not

CG_OpndHandle *IpfInstCodeSelector::not_(IntegerOp::Types opType, 
                                         CG_OpndHandle    *src) {

    IPF_LOG << "      not_ " << endl;
    
    Opnd *dst;
    if (ipfConstantFolding && ((Opnd *)src)->isImm()) {
        switch (opType) {
        case IntegerOp::I4:
            dst = opndManager->newImm(~IMM32(src)); break;
        case IntegerOp::I8:
            dst = opndManager->newImm(~IMM64(src)); break;
        case IntegerOp::I  : 
            dst = opndManager->newImm(~IMM64U(src)); break;
        default: 
            IPF_ASSERT(0);
            dst = NULL;
        }
    } else {
        uint64 val = 0;
        if (opType == IntegerOp::I4) val = 0xFFFFFFFF;
        else                         val = 0xFFFFFFFFFFFFFFFFL;
    
        Opnd    *allOnes = opndManager->newImm(val);
        dst     = opndManager->newRegOpnd(OPND_G_REG, toDataKind(opType));
    
        binOp(INST_XOR, (RegOpnd *)dst, src, allOnes);
    }
    return dst; 
}

//----------------------------------------------------------------------------//
// Shift left

CG_OpndHandle *IpfInstCodeSelector::shl(IntegerOp::Types  opType,
                                        CG_OpndHandle    *value,
                                        CG_OpndHandle    *shiftAmount) {

    IPF_LOG << "      shl " << endl;

    if (ipfConstantFolding && ((Opnd *)value)->isImm() && ((Opnd *)shiftAmount)->isImm()) {
        return opndManager->newImm(((Opnd *)value)->getValue() << ((Opnd *)shiftAmount)->getValue());
    }

    RegOpnd *dst = opndManager->newRegOpnd(OPND_G_REG, toDataKind(opType));
    Opnd *shiftcount = (Opnd *)shiftAmount;
    int bits = 5;
    
    switch (opType) {
    case IntegerOp::I:
    case IntegerOp::I4: bits = 5; break;
    case IntegerOp::I8: bits = 6; break;
    }
    
    shift(INST_SHL, dst, value, shiftcount, bits);
    return dst;
}

//----------------------------------------------------------------------------//
// Shift right

CG_OpndHandle *IpfInstCodeSelector::shr(IntegerOp::Types opType,
                                        CG_OpndHandle    *value,
                                        CG_OpndHandle    *shiftAmount) {

    IPF_LOG << "      shr " << endl;

    if (ipfConstantFolding && ((Opnd *)value)->isImm() && ((Opnd *)shiftAmount)->isImm()) {
        return opndManager->newImm(((Opnd *)value)->getValue() >> ((Opnd *)shiftAmount)->getValue());
    }

    RegOpnd *dst = opndManager->newRegOpnd(OPND_G_REG, toDataKind(opType));
    int bits = 5;
    
    switch (opType) {
    case IntegerOp::I:
    case IntegerOp::I4: bits = 5; break;
    case IntegerOp::I8: bits = 6; break;
    }
    
    if (opType == IntegerOp::I4 && ((Opnd *)value)->getDataKind()==DATA_I32) {
        sxt(value, 32);
    }
    shift(INST_SHR, dst, value, shiftAmount, bits);
    return dst;
}

//----------------------------------------------------------------------------//
// Shift right unsigned

CG_OpndHandle *IpfInstCodeSelector::shru(IntegerOp::Types opType,
                                         CG_OpndHandle    *value,
                                         CG_OpndHandle    *shiftAmount) {

    IPF_LOG << "      shru " << endl;
    
    if (ipfConstantFolding && ((Opnd *)value)->isImm() && ((Opnd *)shiftAmount)->isImm()) {
        if (opType==IntegerOp::I4) {
            return opndManager->newImm((uint64)((U_32)((I_32)(((Opnd *)value)->getValue()))) >> ((Opnd *)shiftAmount)->getValue());
        } else {
            return opndManager->newImm((uint64)(((Opnd *)value)->getValue()) >> ((Opnd *)shiftAmount)->getValue());
        }
    }

    RegOpnd *dst = opndManager->newRegOpnd(OPND_G_REG, toDataKind(opType));
    int bits = 5;
    
    switch (opType) {
    case IntegerOp::I:
    case IntegerOp::I4: bits = 5; break;
    case IntegerOp::I8: bits = 6; break;
    }

    if (opType == IntegerOp::I4 && ((Opnd *)value)->getDataKind()==DATA_I32) {
        zxt(value, 32);
    }
    shift(INST_SHR_U, dst, value, shiftAmount, bits);
    return dst;
}

//----------------------------------------------------------------------------//
// Shift left and add

CG_OpndHandle *IpfInstCodeSelector::shladd(IntegerOp::Types opType,
                                           CG_OpndHandle    *value_,
                                           U_32           imm,
                                           CG_OpndHandle    *addto_) {

    IPF_LOG << "      shladd " << endl;
    IPF_ASSERT(imm>=1 && imm<=4);

    if (ipfConstantFolding && ((Opnd *)value_)->isImm() && ((Opnd *)addto_)->isImm()) {
        return opndManager->newImm((((Opnd *)value_)->getValue() << imm) + ((Opnd *)addto_)->getValue());
    }

    RegOpnd *value = toRegOpnd(value_);
    RegOpnd *addto = toRegOpnd(addto_);
    Opnd    *count = opndManager->newImm(imm);
    RegOpnd *dst   = opndManager->newRegOpnd(OPND_G_REG, toDataKind(opType));
   
    sxt(value, dst->getSize());
    sxt(addto, dst->getSize());
    addNewInst(INST_SHLADD, p0, dst, value, count, addto);
    return dst;
}

//----------------------------------------------------------------------------//
// Convert to integer

CG_OpndHandle *IpfInstCodeSelector::convToInt(ConvertToIntOp::Types       opType,
                                              bool                        isSigned,
                                              bool                        isZeroExtend,
                                              ConvertToIntOp::OverflowMod ovfMod,
                                              Type                        *dstType, 
                                              CG_OpndHandle               *src_) {

    IPF_LOG << "      convToInt " << IrPrinter::toString((Opnd *)src_);
    IPF_LOG << " to " << Type::tag2str(dstType->tag);
    IPF_LOG << "; isSigned=" << isSigned;
    IPF_LOG << "; opType=" << opType << endl;

    if (ipfConstantFolding && ((Opnd *)src_)->isImm()) {
        switch (opType) {
            case ConvertToIntOp::I1: break;
            case ConvertToIntOp::I2: break;
            case ConvertToIntOp::I4: break;
            default                : return src_;
        }
    }

    RegOpnd *src = toRegOpnd(src_);
    RegOpnd *dst = opndManager->newRegOpnd(OPND_G_REG, toDataKind(dstType->tag));

    if (src->isFloating()) {
        // convert fp to int (signed saturating conversion)
        if (dst->getSize() > 4) saturatingConv8(dst, src);
        else                    saturatingConv4(dst, src);
    } else {
        // convert int to int
        Completer cmplt = CMPLT_INVALID;
        switch (opType) {
            case ConvertToIntOp::I1: cmplt = CMPLT_XSZ_1; break;
            case ConvertToIntOp::I2: cmplt = CMPLT_XSZ_2; break;
            case ConvertToIntOp::I4: cmplt = CMPLT_XSZ_4; break;
            default                : break;
        }

        InstCode instCode = dst->isSigned() 
            ? INST_SXT 
            : INST_ZXT;
        if (cmplt == CMPLT_INVALID) addNewInst(INST_MOV, p0, dst, src);
        else                        addNewInst(instCode, cmplt, p0, dst, src);
    }

    return dst;
}

//----------------------------------------------------------------------------//
// Convert to floating-point

CG_OpndHandle *IpfInstCodeSelector::convToFp(ConvertToFpOp::Types opType, 
                                             Type                 *dstType, 
                                             CG_OpndHandle        *src_) {

    IPF_LOG << "      convToFp" << endl;

    RegOpnd *src         = toRegOpnd(src_);  
    DataKind srcDataKind = src->getDataKind();
    DataKind dstDataKind = toDataKind(dstType->tag);
    
    if (dstDataKind == srcDataKind) return src;
    
    RegOpnd *dst = opndManager->newRegOpnd(OPND_F_REG, dstDataKind);
    
    if (src->isFloating()) {
        // convert from fp to fp
        Completer cmplt = dstDataKind == DATA_D ? CMPLT_PC_DOUBLE : CMPLT_PC_SINGLE;
        addNewInst(INST_FNORM, cmplt, p0, dst, src);
    } else {
        // convert from int to fp
        bool     isSigned = (opType != ConvertToFpOp::FloatFromUnsigned);
        InstCode instCode = (isSigned ? INST_FCVT_XF : INST_FCVT_XUF);
        sxt(src, 8);
        addNewInst(INST_SETF_SIG, p0, dst, src);
        addNewInst(instCode, p0, dst, dst);
    }

    return dst;
}

//----------------------------------------------------------------------------//
// Load 32-bit integer constant

CG_OpndHandle *IpfInstCodeSelector::ldc_i4(I_32 val) {

    IPF_LOG << "      ldc_i4; val=" << val << endl;
    
    Opnd *dst;
    if (ipfConstantFolding) {
        dst = opndManager->newImm((int64)val);
    } else {
        dst = opndManager->newRegOpnd(OPND_G_REG, DATA_I32);
        ldc((RegOpnd *)dst, (int64)val);
    }
    return dst;
}

//----------------------------------------------------------------------------//
// Load 64-bit integer constant

CG_OpndHandle *IpfInstCodeSelector::ldc_i8(int64 val) {

    IPF_LOG << "      ldc_i8; val=" << val << endl;

    Opnd *dst;
    if (ipfConstantFolding) {
        dst = opndManager->newImm(val);
    } else {
        dst = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);
        ldc((RegOpnd *)dst, val);
    }
    return dst;
}

//----------------------------------------------------------------------------//
// Load single FP constant

CG_OpndHandle *IpfInstCodeSelector::ldc_s(float val) {

    IPF_LOG << "      ldc_s; val=" << val << endl;

    if (val==0) {
        return opndManager->getF0();
    } else if (val==1) {
        return opndManager->getF1();
    }

    union {
        float  fr;
        U_32 gr;
    } tmpVal;
    
    tmpVal.fr = val;
    
    RegOpnd  *dst      = opndManager->newRegOpnd(OPND_F_REG, DATA_S);
    Opnd     *immOpnd  = opndManager->newImm(tmpVal.gr);
    RegOpnd  *r3       = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);
    InstCode  instCode = immOpnd->isFoldableImm(22) ? INST_MOV : INST_MOVL;
    
    addNewInst(instCode, p0, r3, immOpnd);
    addNewInst(INST_SETF_S, p0, dst, r3);

    //FloatConstant *fc       = new(mm) FloatConstant(val);
    //ConstantRef   *constref = opndManager->newConstantRef(fc);
    //RegOpnd       *r3       = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);
    //RegOpnd       *dst      = opndManager->newRegOpnd(OPND_F_REG, DATA_S);
    //   
    //addNewInst(INST_MOVL, p0, r3, constref);
    //addNewInst(INST_LDF, CMPLT_FSZ_S, p0, dst, r3);

    return dst;
}

//----------------------------------------------------------------------------//
// Load double FP constant

CG_OpndHandle *IpfInstCodeSelector::ldc_d(double val) {

    IPF_LOG << "      ldc_d; val=" << val << endl;

    if (val==0) {
        return opndManager->getF0();
    } else if (val==1) {
        return opndManager->getF1();
    }

    union {
        double fr;
        uint64 gr;
    } tmpVal;
    
    tmpVal.fr = val;
    
    RegOpnd  *dst      = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    Opnd     *immOpnd  = opndManager->newImm(tmpVal.gr);
    RegOpnd  *r3       = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);
    InstCode  instCode = immOpnd->isFoldableImm(22) ? INST_MOV : INST_MOVL;
    
    addNewInst(instCode, p0, r3, immOpnd);
    addNewInst(INST_SETF_D, p0, dst, r3);
    
    //DoubleConstant *fc       = new(mm) DoubleConstant(val);
    //ConstantRef    *constref = opndManager->newConstantRef(fc);
    //RegOpnd        *r3       = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);
    //RegOpnd        *dst      = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    //
    //addNewInst(INST_MOVL, p0, r3, constref);
    //addNewInst(INST_LDF,  CMPLT_FSZ_D, p0, dst, r3);

    return dst;
}

//----------------------------------------------------------------------------//
// Load Null

CG_OpndHandle *IpfInstCodeSelector::ldnull(bool compressed) {

    IPF_LOG << "      ldnull; compressed=" << boolalpha << compressed << endl;
    
    if (false && ipfConstantFolding) {
        if (opndManager->areRefsCompressed() == false) {
            return opndManager->newImm(0); // return opndManager->getR0();
        } 
    
        if (compressed) {
            return opndManager->newImm(0); // return opndManager->getR0();
        } else {
            return HEAPBASEIMM; // return HEAPBASE;
        }
    } else {
        if (opndManager->areRefsCompressed() == false) {
            return opndManager->getR0();
        } 
    
        if (compressed) {
            return opndManager->getR0();
        } else {
            return HEAPBASE;
        }
    }
}

//----------------------------------------------------------------------------//
// Load variable

CG_OpndHandle *IpfInstCodeSelector::ldVar(Type *dstType, U_32 varId) {

    IPF_LOG << "      ldVar; dstType=" << Type::tag2str(dstType->tag) << ", varId=" << varId << endl;

    if (opnds[varId] == opndManager->getTau()) { 
        IPF_LOG << "        tau operation - ignore" << endl;
        return opndManager->getTau();
    }

    if (ipfConstantFolding && opnds[varId]->isImm()) {
        return opnds[varId];
    }
    
    RegOpnd *src = toRegOpnd(opnds[varId]);
    RegOpnd *dst = opndManager->newRegOpnd(toOpndKind(dstType->tag), toDataKind(dstType->tag));

    sxt(src, dst->getSize());
    addNewInst(INST_MOV, p0, dst, src);
    return dst;
}

//----------------------------------------------------------------------------//
// Store variable

void IpfInstCodeSelector::stVar(CG_OpndHandle *_src, U_32 varId) {

    IPF_LOG << "      stVar" 
        << "; varId=" << varId
        << "; src=" << IrPrinter::toString((Opnd *)_src)
        << endl;
    
    if (_src==opndManager->getTau() || opnds[varId]==opndManager->getTau()) { 
        IPF_LOG << "        tau operation - ignore" << endl;
        return;
    }

    if (ipfConstantFolding && opnds[varId]->isImm() && ((Opnd *)_src)->isImm()) {
        opnds[varId]->setValue(((Opnd *)_src)->getValue());
    }
    
    IPF_ASSERT(opnds[varId]->isReg());
    
    Opnd *src = (Opnd *)_src;
    if (src->isReg() || src->isImm(22)) {
        addNewInst(INST_MOV, p0, opnds[varId], src);
    } else {
        addNewInst(INST_MOVL, p0, opnds[varId], src);
    }
}

//----------------------------------------------------------------------------//
// Define an argument

CG_OpndHandle *IpfInstCodeSelector::defArg(U_32 inArgPosition, Type *type) {


    OpndKind opndKind = toOpndKind(type->tag);
    DataKind dataKind = toDataKind(type->tag);
    
    Opnd *arg = opndManager->newInArg(opndKind, dataKind, inArgPosition);    
    IPF_LOG << "      defArg " << IrPrinter::toString(arg) << " " << type->getName() << endl;

    if (arg->isFloating()) {
        BbNode  *prologNode = opndManager->getPrologNode();
        RegOpnd *newarg     = opndManager->newRegOpnd(opndKind, dataKind);
        Inst    *inst       = new(mm) Inst(mm, INST_MOV, p0, newarg, arg);
        prologNode->addInst(inst);
        arg = newarg;                                     // it will be moved on preserved reg

        IPF_LOG << "        " << IrPrinter::toString(inst) << endl;
    }

    return arg;
}

//----------------------------------------------------------------------------//
// Compare two values. Result is an integer.

CG_OpndHandle *IpfInstCodeSelector::cmp(CompareOp::Operators cmpOp,
                                        CompareOp::Types     opType,
                                        CG_OpndHandle        *src1,
                                        CG_OpndHandle        *src2,
                                        int ifNaNResult) {

    IPF_LOG << "      cmp" 
        << "; opType=" << opType
        << "; src1=" << IrPrinter::toString((Opnd *)src1)
        << "; src2=" << IrPrinter::toString((Opnd *)src2)
        << "\n";

    InstCode  instCode   = toInstCmp(opType);
    bool      isFloating = (instCode == INST_FCMP);
    Completer crel       = toCmpltCrel(cmpOp, isFloating);
    RegOpnd   *truePred  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd   *falsePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd   *r0        = opndManager->getR0();
    RegOpnd   *dst       = opndManager->newRegOpnd(OPND_G_REG, DATA_I32);
    
    addNewInst(INST_DEF, p0, dst);

    cmp(instCode, crel, truePred, falsePred, src1, src2);
    addNewInst(INST_MOV, truePred, dst, opndManager->newImm(1));
    addNewInst(INST_MOV, falsePred, dst, r0);
    
    return dst;
}

//----------------------------------------------------------------------------//
// Check if operand is equal to zero. Result is integer.

CG_OpndHandle *IpfInstCodeSelector::czero(CompareZeroOp::Types opType,
                                          CG_OpndHandle        *src) {

    IPF_LOG << "      czero" 
        << "; src=" << IrPrinter::toString((Opnd *)src)
        << endl;

    InstCode  instCode   = toInstCmp(opType);
    Completer crel       = CMPLT_CMP_CREL_EQ;
    RegOpnd   *truePred  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd   *falsePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd   *r0        = opndManager->getR0();
    RegOpnd   *dst       = opndManager->newRegOpnd(OPND_G_REG, DATA_I32);
    
    addNewInst(INST_DEF, p0, dst);

    cmp(instCode, crel, truePred, falsePred, src, r0);
    addNewInst(INST_MOV, truePred, dst, opndManager->newImm(1));
    addNewInst(INST_MOV, falsePred, dst, r0);

    return dst;
}

//----------------------------------------------------------------------------//
// Check if operand is not equal to zero. Result is integer.

CG_OpndHandle *IpfInstCodeSelector::cnzero(CompareZeroOp::Types opType,
                                           CG_OpndHandle        *src) {

    IPF_LOG << "      cnzero" 
        << "; src=" << IrPrinter::toString((Opnd *)src)
        << endl;

    InstCode  instCode   = toInstCmp(opType);
    Completer crel       = CMPLT_CMP_CREL_NE;
    RegOpnd   *truePred  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd   *falsePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd   *r0        = opndManager->getR0();
    RegOpnd   *dst       = opndManager->newRegOpnd(OPND_G_REG, DATA_I32);
    
    addNewInst(INST_DEF, p0, dst);

    cmp(instCode, crel, truePred, falsePred, src, r0);
    addNewInst(INST_MOV, truePred, dst, opndManager->newImm(1));
    addNewInst(INST_MOV, falsePred, dst, r0);

    return dst;
}

//----------------------------------------------------------------------------//
// Copy operand

CG_OpndHandle *IpfInstCodeSelector::copy(CG_OpndHandle *src_) {

    IPF_LOG << "      copy" 
        << "; src_=" << IrPrinter::toString((Opnd *)src_)
        << endl;

    if ((Opnd *)src_ == opndManager->getTau()) { 
        IPF_LOG << "        tau operation - ignore" << endl;
        return src_;
    }

    RegOpnd *src = toRegOpnd(src_);
    RegOpnd *dst = opndManager->newRegOpnd(src->getOpndKind(), src->getDataKind());

    addNewInst(INST_MOV, p0, dst, src);
    return dst;
}

//----------------------------------------------------------------------------//
// Statically cast object to type.
// This cast requires no runtime check. It's a compiler assertion. 

CG_OpndHandle *IpfInstCodeSelector::tau_staticCast(ObjectType    *toType, 
                                                   CG_OpndHandle *obj,
                                                   CG_OpndHandle *tauIsType) {

    IPF_LOG << "      tau_staticCast" << endl;

    if (((Opnd *)obj)->isImm()) {
        return obj;
    }
    
    RegOpnd *dst = opndManager->newRegOpnd(OPND_G_REG, toDataKind(toType->tag));
    
    addNewInst(INST_MOV, p0, dst, obj);
    return dst;
}

//----------------------------------------------------------------------------//
// Branch if result of cmp is true

void IpfInstCodeSelector::branch(CompareOp::Operators cmpOp,
                                 CompareOp::Types     opType,
                                 CG_OpndHandle        *src1,
                                 CG_OpndHandle        *src2) {

    IPF_LOG << "      branch" 
        << "; src1=" << IrPrinter::toString((Opnd *)src1)
        << "; src2=" << IrPrinter::toString((Opnd *)src2)
        << endl;

    if (false // TODO: need update cfg: branch edge target and "br" instruction target are different
            && ipfConstantFolding && ((Opnd *)src1)->isImm() && ((Opnd *)src2)->isImm()) {
        RegOpnd   *truePred  = opndManager->getP0();
        NodeRef   *target    = opndManager->newNodeRef();
        int64      v1 = ((Opnd *)src1)->getValue(), v2 = ((Opnd *)src2)->getValue();
        bool       cmpres = false;
    
        switch (cmpOp) {
        case CompareOp::Eq  : cmpres = (v1 == v2); break;
        case CompareOp::Ne  : cmpres = (v1 != v2); break;
        case CompareOp::Gt  : cmpres = (v1 > v2); break;
        case CompareOp::Gtu : cmpres = ((uint64)v1 > (uint64)v2); break;
        case CompareOp::Ge  : cmpres = (v1 >= v2); break;
        case CompareOp::Geu : cmpres = ((uint64)v1 >= (uint64)v2); break;
        default             : IPF_ERR << "unexpected cmpOp type " << cmpOp << endl;
        }
        if (cmpres) {
            addNewInst(INST_BR, CMPLT_BTYPE_COND, CMPLT_WH_SPTK, CMPLT_PH_MANY, truePred, target);
        }
    } else {
        InstCode  instCode   = toInstCmp(opType);
        bool      isFloating = (instCode == INST_FCMP);
        Completer crel       = toCmpltCrel(cmpOp, isFloating);
        RegOpnd   *truePred  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
        NodeRef   *target    = opndManager->newNodeRef();
    
        cmp(instCode, crel, truePred, p0, src1, src2);
        addNewInst(INST_BR, CMPLT_BTYPE_COND, CMPLT_WH_DPTK, CMPLT_PH_MANY, truePred, target);
    }
}

//----------------------------------------------------------------------------//
// Branch if src is zero

void IpfInstCodeSelector::bzero(CompareZeroOp::Types opType,
                                CG_OpndHandle        *src) {

    IPF_LOG << "      bzero" 
        << "; src=" << IrPrinter::toString((Opnd *)src)
        << endl;
    IPF_ASSERT(((Opnd *)src)->isReg());

    InstCode instCode  = toInstCmp(opType);
    RegOpnd  *truePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    NodeRef  *target   = opndManager->newNodeRef();
    RegOpnd  *zero     = NULL;

    if (opType==CompareZeroOp::Ref && opndManager->areRefsCompressed()) {
        zero = HEAPBASE;                            // if refs are compressed - zero is HEAPBASE
    } else {
        zero = opndManager->getR0((RegOpnd *)src);  // get "0" corresponding to src size/sign
    }

    cmp(instCode, CMPLT_CMP_CREL_EQ, truePred, p0, src, zero);
    addNewInst(INST_BR, CMPLT_BTYPE_COND, CMPLT_WH_DPTK, CMPLT_PH_MANY, truePred, target);
}

//----------------------------------------------------------------------------//
// Branch if src is not zero

void IpfInstCodeSelector::bnzero(CompareZeroOp::Types opType,
                                 CG_OpndHandle        *src) {

    IPF_LOG << "      bnzero" 
        << "; src=" << IrPrinter::toString((Opnd *)src)
        << endl;
    IPF_ASSERT(((Opnd *)src)->isReg());

    InstCode instCode  = toInstCmp(opType);
    RegOpnd  *truePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    NodeRef  *target   = opndManager->newNodeRef();
    RegOpnd  *zero     = NULL;

    if (opType==CompareZeroOp::Ref && opndManager->areRefsCompressed()) {
        zero = HEAPBASE;                            // if refs are compressed - zero is HEAPBASE
    } else {
        zero = opndManager->getR0((RegOpnd *)src);  // get "0" corresponding to src size/sign
    } 

    cmp(instCode, CMPLT_CMP_CREL_NE, truePred, p0, src, zero);
    addNewInst(INST_BR, CMPLT_BTYPE_COND, CMPLT_WH_DPTK, CMPLT_PH_MANY, truePred, target);
}

//----------------------------------------------------------------------------//
//  Switch
//
//       cmp.eq     p8,p9   = r0, r0                // set p8 = true
//       cmp.lt.unc p6,p7   = maxTgt, trg           // if target is greater than  
//                                                  // max target p6=true p7=false
//  (p7) cmp.ne.and p8,p7   = fallThroughTgt, trg   // if target is fall through target
//                                                  // p7=false p8=false
//  (p6) mov        tgt     = defTgt                // target is default target
//  (p8) mov        tgtAddr = switchTblAddr         // load switch table address
//  (p8) shladd     r14     = trg, 3, tgtAddr       // calculate switch table address 
//                                                  // containing target address
//  (p8) ld8        r14     = [r14]                 // load target address
//  (p8) mov        b1      = r14                   // load target address to branch register
//  (p8) br.cond.sptk b1                            // branch to target
//                                                  // if p8 is false - fall through

void IpfInstCodeSelector::tableSwitch(CG_OpndHandle *src, U_32 nTargets) {

    IPF_LOG << "      tableSwitch" << endl;

    Constant *switchTable = new(mm) SwitchConstant(mm);

    Opnd *r0                  = opndManager->getR0();
    Opnd *p6                  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);   // default target is taken
    Opnd *p7                  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);   // check fall through target
    Opnd *p8                  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);   // fall through is not taken
    Opnd *switchTblAddr       = opndManager->newConstantRef(switchTable, DATA_SWITCH_REF);  // switch table address
    Opnd *tgtValue            = (Opnd *)src;
    Opnd *maxTgtValue         = opndManager->newImm(nTargets-1);               // max target value
    Opnd *defTgtValue         = opndManager->newImm(0);                        // default target value
    Opnd *fallThroughTgtValue = opndManager->newImm(0);                        // FT target value
    Opnd *tgt                 = opndManager->newRegOpnd(OPND_G_REG, DATA_I64); // target
    Opnd *maxTgt              = opndManager->newRegOpnd(OPND_G_REG, DATA_I64); // max target
    Opnd *defTgt              = opndManager->newRegOpnd(OPND_G_REG, DATA_I64); // default target
    Opnd *fallThroughTgt      = opndManager->newRegOpnd(OPND_G_REG, DATA_I64); // FT target
    Opnd *shlCnt              = opndManager->newImm(3);
    Opnd *tgtAddr             = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);
    Opnd *branchTgt           = opndManager->newRegOpnd(OPND_B_REG, DATA_I64);
    
    addNewInst(INST_DEF, p0, tgt);
    addNewInst(INST_DEF, p0, defTgt);
    addNewInst(INST_DEF, p0, tgtAddr);
    addNewInst(INST_DEF, p0, branchTgt);

    // mov to maxTgtValue, defTgtValue, fallThroughTgtValue to GRs
    addNewInst(INST_MOV, p0, tgt,            tgtValue);
    addNewInst(INST_MOV, p0, maxTgt,         maxTgtValue);
    addNewInst(INST_MOV, p0, defTgt,         defTgtValue);
    addNewInst(INST_MOV, p0, fallThroughTgt, fallThroughTgtValue);
    // just make 1 in p8
    addNewInst(INST_CMP4, CMPLT_CMP_CREL_EQ, p0, p8, p0, r0, r0);
    // compare with default
    addNewInst(INST_CMP4, CMPLT_CMP_CREL_GT, CMPLT_CMP_CTYPE_UNC, p0, p6, p7, tgt, maxTgt);

    Inst *tmpinst = new(mm) Inst(mm, INST_CMP4, CMPLT_CMP_CREL_LT, CMPLT_CMP_CTYPE_OR_ANDCM, p7, p6, p7, tgt, r0);
    tmpinst->addOpnd(p6);
    tmpinst->addOpnd(p7);
    addInst(tmpinst);

    addNewInst(INST_MOV, p6, tgt, defTgt);
    // compare if through target
    addNewInst(INST_CMP4, CMPLT_CMP_CREL_NE, CMPLT_CMP_CTYPE_AND, p0, p8, p0, fallThroughTgt, tgt, p8);
    // if not through target load tgt Address
    addNewInst(INST_MOV,            p8, tgtAddr, switchTblAddr);
    addNewInst(INST_SHLADD,         p8, tgtAddr, tgt, shlCnt, tgtAddr);
    addNewInst(INST_LD, CMPLT_SZ_8, p8, tgtAddr, tgtAddr);
    addNewInst(INST_MOV,            p8, branchTgt, tgtAddr);
    addNewInst(INST_SWITCH,         p8, branchTgt, switchTblAddr, defTgtValue, fallThroughTgtValue);
}

//----------------------------------------------------------------------------//
// Direct call to the method

CG_OpndHandle *IpfInstCodeSelector::call(U_32        numArgs, 
                                         CG_OpndHandle **args, 
                                         Type          *retType,
                                         MethodDesc    *desc) {

    return tau_call(numArgs, args, retType, desc, NULL, NULL);
}

//----------------------------------------------------------------------------//
// Direct call to the method 

CG_OpndHandle *IpfInstCodeSelector::tau_call(U_32        numArgs, 
                                             CG_OpndHandle **args, 
                                             Type          *retType,
                                             MethodDesc    *desc,
                                             CG_OpndHandle *tauNullCheckedFirstArg,
                                             CG_OpndHandle *tauTypesChecked) {

    IPF_LOG << "      tau_call; method=" << desc->getName() 
        << ", desc=" << desc << ", addr=0x" << hex << *((uint64 *)desc->getIndirectAddress()) 
        << dec << endl;

    MethodRef *methodRef = opndManager->newMethodRef(desc);
    RegOpnd   *retOpnd   = NULL;

    if(retType != NULL) {
        retOpnd = opndManager->newRegOpnd(toOpndKind(retType->tag), toDataKind(retType->tag));
    }
    
    directCall(numArgs, (Opnd **)args, retOpnd, methodRef, p0);

    return retOpnd;
}

//----------------------------------------------------------------------------//
// Indirect call

CG_OpndHandle *IpfInstCodeSelector::tau_calli(U_32        numArgs, 
                                              CG_OpndHandle **args, 
                                              Type          *retType, 
                                              CG_OpndHandle *methodPtr,
                                              CG_OpndHandle *nonNullFirstArgTau,
                                              CG_OpndHandle *tauTypesChecked) {

    IPF_LOG << "      tau_calli; numArgs=" << numArgs 
        << ", retType=" << (retType ? Type::tag2str(retType->tag) : "NULL") << endl;
    IPF_ASSERT(((Opnd *)methodPtr)->isReg());

    RegOpnd *retOpnd = NULL;
    if(retType != NULL) {
        retOpnd = opndManager->newRegOpnd(toOpndKind(retType->tag), toDataKind(retType->tag));
    }

    indirectCall(numArgs, (Opnd **)args, retOpnd, (RegOpnd *)methodPtr, p0);
    return retOpnd;
}

//----------------------------------------------------------------------------//

void IpfInstCodeSelector::ret() {

    IPF_LOG << "      ret" << endl;

    RegOpnd *b0 = opndManager->getB0();
    addNewInst(INST_BR, CMPLT_BTYPE_RET, p0, b0);
}

//----------------------------------------------------------------------------//
// Return with a value

void IpfInstCodeSelector::ret(CG_OpndHandle *retValue_) {

    IPF_LOG << "      ret" << endl;

    RegOpnd *retValue = toRegOpnd(retValue_);
    RegOpnd *b0       = opndManager->getB0();
    RegOpnd *retOpnd  = NULL;
    if(retValue->isFloating()) retOpnd = opndManager->newRegOpnd(OPND_F_REG, DATA_F,   RET_F_REG);
    else                       retOpnd = opndManager->newRegOpnd(OPND_G_REG, DATA_I64, RET_G_REG);
    
    addNewInst(INST_MOV, p0, retOpnd, retValue);
    addNewInst(INST_BR, CMPLT_BTYPE_RET, p0, b0, retOpnd);
}

//----------------------------------------------------------------------------//
// Throw an exception

void IpfInstCodeSelector::throwException(CG_OpndHandle *exceptionObj, bool createStackTrace) {

    IPF_LOG << "      throwException; createStackTrace=" << createStackTrace << endl;

    Opnd *helperArgs[] = { (Opnd *)exceptionObj };

    VM_RT_SUPPORT hId;
    if (createStackTrace) hId = VM_RT_THROW_SET_STACK_TRACE;
    else                  hId = VM_RT_THROW;
    
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    
    directCall(1, helperArgs, NULL, helperAddress, p0);
}

//----------------------------------------------------------------------------//
// Throw system exception

void IpfInstCodeSelector::throwSystemException(CompilationInterface::SystemExceptionId id) {

    IPF_LOG << "      throwSystemException" << endl;
    
    //VM_RT_SUPPORT hId = VM_RT_UNKNOWN;
    ObjectType* excType = NULL; 
    switch (id) {
        case CompilationInterface::Exception_NullPointer: 
            excType = compilationInterface.findClassUsingBootstrapClassloader(NULL_POINTER_EXCEPTION);
            //hId = VM_RT_NULL_PTR_EXCEPTION;
            break;
        case CompilationInterface::Exception_ArrayIndexOutOfBounds: 
            excType = compilationInterface.findClassUsingBootstrapClassloader(INDEX_OUT_OF_BOUNDS);
            //hId = VM_RT_IDX_OUT_OF_BOUNDS;
            break;
        case CompilationInterface::Exception_ArrayTypeMismatch: 
            excType = compilationInterface.findClassUsingBootstrapClassloader(ARRAY_STORE_EXCEPTION);
            //hId = VM_RT_ARRAY_STORE_EXCEPTION;
            break;
        case CompilationInterface::Exception_DivideByZero: 
            excType = compilationInterface.findClassUsingBootstrapClassloader(DIVIDE_BY_ZERO_EXCEPTION);
            //hId = VM_RT_DIVIDE_BY_ZERO_EXCEPTION;
            break;
        default: 
            IPF_ERR << "unexpected id " << id << endl;
    }
    
    throwException(excType);
}

void IpfInstCodeSelector::throwException(ObjectType* excType)
{
    assert(excType);

    Opnd *helperOpnds1[] = {
        opndManager->newImm((int64) excType->getObjectSize()),
        opndManager->newImm((int64) excType->getAllocationHandle())
    };
    
    VM_RT_SUPPORT hId = VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE;
    uint64   address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd*    helperAddress = opndManager->newImm(address);
    OpndKind opndKind       = toOpndKind(excType->tag);
    DataKind dataKind       = toDataKind(excType->tag);
    RegOpnd* retOpnd       = opndManager->newRegOpnd(opndKind, dataKind);
    
    directCall(2, helperOpnds1, retOpnd, helperAddress, p0);

    Opnd * helperOpnds2[] = { (Opnd*)retOpnd };
    MethodDesc* md = compilationInterface.resolveMethod( excType, 
            DEFAUlT_COSTRUCTOR_NAME, DEFAUlT_COSTRUCTOR_DESCRIPTOR);
    call(1, (CG_OpndHandle **)helperOpnds2, NULL, md);

    Opnd * helperOpnds3[] = { (Opnd*)retOpnd };

    hId = VM_RT_THROW;
    address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    helperAddress = opndManager->newImm(address);

    directCall(1, helperOpnds3, NULL, helperAddress, p0);
}
//----------------------------------------------------------------------------//
// Throw linking exception

void IpfInstCodeSelector::throwLinkingException(Class_Handle encClass,
                                                U_32       cp_ndx,
                                                U_32       opcode)
{

    IPF_LOG << "      throwLinkingException" << endl;

    Opnd *helperArgs[] = { 
        opndManager->newImm((int64) encClass),
        opndManager->newImm(cp_ndx),
        opndManager->newImm(opcode)
    };
    
    VM_RT_SUPPORT hId = VM_RT_THROW_LINKING_EXCEPTION;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    
    directCall(3, helperArgs, NULL, helperAddress, p0);
}

//----------------------------------------------------------------------------//
// Copy exception object from r8 to new RegOpnd

CG_OpndHandle *IpfInstCodeSelector::catchException(Type *exceptionType) {

    IPF_LOG << "      catchException" << endl;
    
    RegOpnd *exceptionObj = opndManager->newRegOpnd(OPND_G_REG, DATA_U64, RET_G_REG);
    RegOpnd *dst          = opndManager->newRegOpnd(OPND_G_REG, DATA_U64);
    
    addNewInst(INST_DEF, p0, exceptionObj); // DON'T REMOVE, THIS IS NOT FOR OPTIMIZATION
    addNewInst(INST_MOV, p0, dst, exceptionObj);
    return dst;
}

//----------------------------------------------------------------------------//
// Throw null pointer exception if base is NULL

CG_OpndHandle *IpfInstCodeSelector::tau_checkNull(CG_OpndHandle *base, bool checksThisForInlinedMethod) {
    
    IPF_LOG << "      tau_checkNull; base=" << IrPrinter::toString((Opnd *)base) << endl;
    IPF_ASSERT(((Opnd *)base)->isReg());

    RegOpnd *truePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd *zero     = NULL;
    
    if (opndManager->areRefsCompressed()) {
        zero = HEAPBASE;
    } else {
        zero = opndManager->getR0((RegOpnd *)base);
    }

    // p0  cmp.eq  p2, p0 = base, zero
    cmp(CMPLT_CMP_CREL_EQ, truePred, p0, base, zero);

    // p2  brl.call  b0 = helperAddress
    ObjectType* excType = compilationInterface.findClassUsingBootstrapClassloader(NULL_POINTER_EXCEPTION);
    throwException(excType);
    //VM_RT_SUPPORT hId = VM_RT_NULL_PTR_EXCEPTION;
    //uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    //Opnd    *helperAddress = opndManager->newImm(address);
    //directCall(0, NULL, NULL, helperAddress, truePred, CMPLT_WH_DPNT);

    return opndManager->getTau();    // return fake value (we do not use tau)
}

//----------------------------------------------------------------------------//
// Throw index out of range exception if index is larger than array length

CG_OpndHandle *IpfInstCodeSelector::tau_checkBounds(CG_OpndHandle *arrayLen, 
                                                    CG_OpndHandle *index) {

    IPF_LOG << "      tau_checkBounds" << endl;

    // p0  cmp.ge  p2, p0 = index, arrayLen
    RegOpnd *truePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    cmp(CMPLT_CMP_CREL_GE, truePred, p0, index, arrayLen);

    // p2  brl.call  b0 = helperAddress
    ObjectType* excType = compilationInterface.findClassUsingBootstrapClassloader(INDEX_OUT_OF_BOUNDS);
    throwException(excType);
    //VM_RT_SUPPORT hId = VM_RT_IDX_OUT_OF_BOUNDS;
    //uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    //Opnd    *helperAddress = opndManager->newImm(address);
    //directCall(0, NULL, NULL, helperAddress, truePred, CMPLT_WH_DPNT);

    return opndManager->getTau();    // return fake value (we do not use tau);
}

//----------------------------------------------------------------------------//
// Throw index out of range exception if (a > b)

CG_OpndHandle *IpfInstCodeSelector::tau_checkLowerBound(CG_OpndHandle *a,
                                                        CG_OpndHandle *b) {

    IPF_LOG << "      tau_checkLowerBound" << endl;

    // p0  cmp.gt  p2, p0 = a, b
    RegOpnd *truePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    cmp(CMPLT_CMP_CREL_GT, truePred, p0, a, b);

    // p2  brl.call  b0 = helperAddress
    ObjectType* excType = compilationInterface.findClassUsingBootstrapClassloader(INDEX_OUT_OF_BOUNDS);
    throwException(excType);
    //VM_RT_SUPPORT hId = VM_RT_IDX_OUT_OF_BOUNDS;
    //uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    //Opnd    *helperAddress = opndManager->newImm(address);
    //directCall(0, NULL, NULL, helperAddress, truePred, CMPLT_WH_DPNT);

    return opndManager->getTau();    // return fake value (we do not use tau);
}

//----------------------------------------------------------------------------//
// Throw index out of range exception if (a >=u b)

CG_OpndHandle *IpfInstCodeSelector::tau_checkUpperBound(CG_OpndHandle *a,
                                                        CG_OpndHandle *b) {

    IPF_LOG << "      tau_checkUpperBound" << endl;

    // p0  cmp.ge  p2, p0 = a, b
    RegOpnd *truePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    cmp(CMPLT_CMP_CREL_GEU, truePred, p0, a, b);

    // p2  brl.call  b0 = helperAddress
    ObjectType* excType = compilationInterface.findClassUsingBootstrapClassloader(INDEX_OUT_OF_BOUNDS);
    throwException(excType);
    //VM_RT_SUPPORT hId = VM_RT_IDX_OUT_OF_BOUNDS;
    //uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    //Opnd    *helperAddress = opndManager->newImm(address);
    //directCall(0, NULL, NULL, helperAddress, truePred, CMPLT_WH_DPNT);

    return opndManager->getTau();    // return fake value (we do not use tau);
}

//----------------------------------------------------------------------------//
// p0   brl.call b0, Helper_IsValidElemType
// p0   mov retOpnd, r8
// p0   cmp p3, p0 = retOpnd, r0
// p3   brl.call b0, Helper_ElemTypeException

CG_OpndHandle *IpfInstCodeSelector::tau_checkElemType(CG_OpndHandle *array, 
                                                      CG_OpndHandle *src,
                                                      CG_OpndHandle *tauNullChecked, 
                                                      CG_OpndHandle *tauIsArray) {

    IPF_LOG << "      tau_checkElemType" << endl;

    Opnd *helperArgs[] = {
        (Opnd *)src,
        (Opnd *)array
    };

    // p0   brl.call b0, Helper_IsValidElemType
    // p0   mov retOpnd, r8
    VM_RT_SUPPORT hId = VM_RT_AASTORE_TEST;
    uint64  address         = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress1 = opndManager->newImm(address);
    RegOpnd *retOpnd        = opndManager->newRegOpnd(OPND_G_REG, DATA_U64);
    directCall(2, helperArgs, retOpnd, helperAddress1, p0);

    // p0   cmp p3, p0 = retOpnd, r0
    RegOpnd *r0       = opndManager->getR0(retOpnd);
    RegOpnd *truePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    cmp(CMPLT_CMP_CREL_EQ, truePred, p0, retOpnd, r0);

    // p3   brl.call b0, Helper_ElemTypeException
    ObjectType* excType = compilationInterface.findClassUsingBootstrapClassloader(ARRAY_STORE_EXCEPTION);
    throwException(excType);
    //hId     = VM_RT_ARRAY_STORE_EXCEPTION;
    //address = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    //Opnd    *helperAddress2 = opndManager->newImm(address);
    //directCall(0, NULL, NULL, helperAddress2, truePred, CMPLT_WH_DPNT);
    
    return opndManager->getTau();    // return fake value (we do not use tau);
}

//----------------------------------------------------------------------------//
// Throw DivideByZeroException if checkZero's argument is 0

CG_OpndHandle *IpfInstCodeSelector::tau_checkZero(CG_OpndHandle *src_) {

    IPF_LOG << "      tau_checkZero; src=" << IrPrinter::toString((Opnd *)src_) << endl;

    // p0  cmp.eq  p2, p0 = base, r0
    RegOpnd *src      = toRegOpnd(src_);
    RegOpnd *r0       = opndManager->getR0(src);
    RegOpnd *truePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    cmp(CMPLT_CMP_CREL_EQ, truePred, p0, src, r0);

    // p2  brl.call  b0 = helperAddress
    ObjectType* excType = compilationInterface.findClassUsingBootstrapClassloader(DIVIDE_BY_ZERO_EXCEPTION);
    throwException(excType);
    //VM_RT_SUPPORT hId = VM_RT_DIVIDE_BY_ZERO_EXCEPTION;
    //uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    //Opnd    *helperAddress = opndManager->newImm(address);
    //directCall(0, NULL, NULL, helperAddress, truePred, CMPLT_WH_DPNT);

    return opndManager->getTau();    // return fake value (we do not use tau)
}

//----------------------------------------------------------------------------//
// Cast object to type and return the tau tied to this cast if casting is legal,
// otherwise throw an exception. 
// checkCast is different from cast - checkCast returns the tau while cast returns
// the casted object (which is the same as the argument object)

CG_OpndHandle *IpfInstCodeSelector::tau_checkCast(ObjectType    *toType, 
                                                  CG_OpndHandle *obj,
                                                  CG_OpndHandle *tauCheckedNull) {

    IPF_LOG << "      tau_checkCast" << endl;

    Opnd *helperArgs[] = { 
        (Opnd *)obj,
        (Opnd *)opndManager->newImm((uint64) toType->getRuntimeIdentifier())
    };
    
    VM_RT_SUPPORT hId = VM_RT_CHECKCAST;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    directCall(2, helperArgs, NULL, helperAddress, p0);
    
    return opndManager->getTau();    // return fake value (we do not use tau)
}

//----------------------------------------------------------------------------//
// Store indirect

void IpfInstCodeSelector::tau_stInd(CG_OpndHandle *src_, 
                                    CG_OpndHandle *ptr_, 
                                    Type::Tag     memType, 
                                    bool          autoCompressRef,
                                    CG_OpndHandle *tauBaseNonNull,
                                    CG_OpndHandle *tauAddressInRange, 
                                    CG_OpndHandle *tauElemTypeChecked) {

    IPF_LOG << "      tau_stInd" << endl;

    DataKind  dataKind = toDataKind(memType);
    InstCode  instCode = IpfType::isFloating(dataKind) ? INST_STF : INST_ST;
    Completer cmplt    = toCmpltSz(dataKind);
    RegOpnd   *src     = toRegOpnd(src_);
    RegOpnd   *ptr     = toRegOpnd(ptr_);

    if (autoCompressRef) {
        IPF_ASSERT(opndManager->areRefsCompressed());
        dataKind = DATA_U32;
        cmplt    = toCmpltSz(dataKind);
        RegOpnd *tmp = opndManager->newRegOpnd(OPND_G_REG, dataKind);
        sub(tmp, src, HEAPBASE);

        addNewInst(instCode, cmplt, p0, ptr, tmp);
    } else {
        addNewInst(instCode, cmplt, p0, ptr, src);
    }
}

//----------------------------------------------------------------------------//
// Load indirect

CG_OpndHandle *IpfInstCodeSelector::tau_ldInd(Type          *dstType, 
                                              CG_OpndHandle *ptr_, 
                                              Type::Tag     memType,
                                              bool          autoUncompressRef,
                                              bool          speculateLoad,
                                              CG_OpndHandle *tauBaseNonNull,
                                              CG_OpndHandle *tauAddressInRange) {

    IPF_LOG << "      tau_ldInd;" 
        << " dstType=" << Type::tag2str(dstType->tag) 
        << ", memType=" << Type::tag2str(memType) 
        << ", autoUncompressRef=" << autoUncompressRef << endl;

    DataKind  dataKind = toDataKind(memType);
    InstCode  instCode = IpfType::isFloating(dataKind) ? INST_LDF : INST_LD;
    Completer cmplt    = toCmpltSz(dataKind);
    RegOpnd   *ptr     = toRegOpnd(ptr_);
    RegOpnd   *dst     = opndManager->newRegOpnd(toOpndKind(dstType->tag), toDataKind(dstType->tag));

    if (autoUncompressRef) {
        IPF_ASSERT(opndManager->areRefsCompressed());
        cmplt = toCmpltSz(DATA_U32);
        addNewInst(instCode, cmplt, p0, dst, ptr);
        add(dst, dst, HEAPBASE);
    } else {
        addNewInst(instCode, cmplt, p0, dst, ptr);
        sxt(dst, 8, cmplt);
    }

    return dst;
}

//----------------------------------------------------------------------------//
// Load string 

CG_OpndHandle *IpfInstCodeSelector::ldString(MethodDesc *enclosingMethod,
                                             U_32     stringToken,
                                             bool       uncompress) {

    IPF_LOG << "      ldString" << endl;

    Opnd *helperArgs[] = {
        opndManager->newImm(stringToken),
        opndManager->newImm((int64) enclosingMethod->getParentType()->getRuntimeIdentifier())
    };
    
    RegOpnd *retOpnd = opndManager->newRegOpnd(OPND_G_REG, DATA_BASE);
    VM_RT_SUPPORT hId = VM_RT_LDC_STRING;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    
    directCall(2, helperArgs, retOpnd, helperAddress, p0);
    return retOpnd;
}

//----------------------------------------------------------------------------//
// Load address of the object lock 
// (aka lock owner field in the synchronization header)

CG_OpndHandle *IpfInstCodeSelector::ldLockAddr(CG_OpndHandle *obj) {

    IPF_LOG << "      ldLockAddr(" << IrPrinter::toString((Opnd *)obj) << ")" << endl;

    RegOpnd *dst    = opndManager->newRegOpnd(OPND_G_REG, DATA_MPTR);
    Opnd    *offset = opndManager->newImm(LOC_OFFSET);

    add(dst, offset, obj);
    return dst;
} 

//----------------------------------------------------------------------------//
// Load address of the virtual/interface table slot that contains function address

CG_OpndHandle *IpfInstCodeSelector::tau_ldVirtFunAddr(Type          *dstType, 
                                                      CG_OpndHandle *vtableAddr, 
                                                      MethodDesc    *methodDesc,
                                                      CG_OpndHandle *tauVtableHasDesc) {

    IPF_LOG << "      tau_ldVirtFunAddr; dstType==" << Type::tag2str(dstType->tag) 
        << ", method=" << methodDesc->getName() << endl;
    IPF_ASSERT(((Opnd *)vtableAddr)->isReg());

    uint64  offsetVal = methodDesc->getOffset();     // get method offset in class VTable
    Opnd    *offset   = opndManager->newImm(offsetVal);
    RegOpnd *dst      = opndManager->newRegOpnd(OPND_G_REG, DATA_U64);

    add(dst, offset, vtableAddr);
    addNewInst(INST_LD, CMPLT_SZ_8, p0, dst, dst);
    return dst;
}

//----------------------------------------------------------------------------//
// Load virtual table address

CG_OpndHandle *IpfInstCodeSelector::tau_ldVTableAddr(Type          *dstType, 
                                                     CG_OpndHandle *base,
                                                     CG_OpndHandle *tauBaseNonNull) {

    IPF_LOG << "      tau_ldVTableAddr; dstType==" << Type::tag2str(dstType->tag) << endl;
    IPF_ASSERT(((Opnd *)base)->isReg());

    Opnd    *offset = opndManager->getVtableOffset();
    RegOpnd *vtable = opndManager->newRegOpnd(OPND_G_REG, toDataKind(dstType->tag));
    RegOpnd *addr   = NULL;

    if(offset == NULL) {   // if VTable has offset 0 - base represents VTable address
        addr = (RegOpnd *)base;
    } else {
        addr = opndManager->newRegOpnd(OPND_G_REG, toDataKind(dstType->tag));
        add(addr, offset, base);
    }

    Completer completer = CMPLT_INVALID;
    if(opndManager->areVtablePtrsCompressed()) completer = CMPLT_SZ_4;
    else                                       completer = CMPLT_SZ_8;

    addNewInst(INST_LD, completer, p0, vtable, addr);
    
    if (dstType->tag==Type::VTablePtr && opndManager->areVtablePtrsCompressed()) {
        add(vtable, vtable, VTABLEBASE);
    }
    
    return vtable;
}

//----------------------------------------------------------------------------//
// get vtable constant (a constant pointer)

CG_OpndHandle *IpfInstCodeSelector::getVTableAddr(Type       *dstType, 
                                                  ObjectType *base) {

    uint64 value = (uint64) base->getVTable();
//    if (dstType->tag==Type::VTablePtr && opndManager->areVtablePtrsCompressed()) {
//        value += (uint64) compilationInterface.getVTableBase();
//    }
    
    Opnd *addr = opndManager->newImm(value);
    IPF_LOG << "      getVTableAddr" << endl << "        addr " << IrPrinter::toString(addr) << endl;

    return addr;
}

//----------------------------------------------------------------------------//
// get java.langObject

CG_OpndHandle *IpfInstCodeSelector::getClassObj(Type       *dstType, 
                                                  ObjectType *base) {

    IPF_LOG << "      getClassObj" << endl;

    uint64 typeRuntimeId = (uint64) base->getRuntimeIdentifier();
    Opnd   *helperArgs1[] = { opndManager->newImm(typeRuntimeId) };

    VM_RT_SUPPORT hId1 = VM_RT_CLASS_2_JLC;
    uint64  address1        = (uint64) compilationInterface.getRuntimeHelperAddress(hId1);
    Opnd*   helperAddress1  = opndManager->newImm(address1);
    OpndKind opndKind       = toOpndKind(dstType->tag);
    DataKind dataKind       = toDataKind(dstType->tag);
    RegOpnd* retOpnd        = opndManager->newRegOpnd(opndKind, dataKind);
    
    directCall(1, helperArgs1, retOpnd, helperAddress1, p0);
    return retOpnd; 
}

//----------------------------------------------------------------------------//
// Load interface table address

CG_OpndHandle *IpfInstCodeSelector::tau_ldIntfTableAddr(Type          *dstType, 
                                                        CG_OpndHandle *base, 
                                                        NamedType     *vtableType) {

    IPF_LOG << "      tau_ldIntfTableAddr; dstType==" << Type::tag2str(dstType->tag)  
        << "; vtableType=" << Type::tag2str(vtableType->tag) << endl;

    Opnd *helperArgs[] = { 
        (Opnd *)base,
        (Opnd *)opndManager->newImm((uint64) vtableType->getRuntimeIdentifier())
    };
    
    VM_RT_SUPPORT hId = VM_RT_GET_INTERFACE_VTABLE_VER0;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    RegOpnd *retOpnd       = opndManager->newRegOpnd(toOpndKind(dstType->tag), toDataKind(dstType->tag));

    directCall(2, helperArgs, retOpnd, helperAddress, p0);
    return retOpnd;
}

//----------------------------------------------------------------------------//
// Load address of the field at "base + offset"

CG_OpndHandle *IpfInstCodeSelector::ldFieldAddr(Type          *fieldRefType,
                                                CG_OpndHandle *base, 
                                                FieldDesc     *fieldDesc) {

    IPF_LOG << "      ldFieldAddr " << fieldDesc->getName() 
        << "(" << Type::tag2str(fieldRefType->tag) << ")"
        << "; base=" << IrPrinter::toString((Opnd *)base)
        << endl;

    Opnd    *fieldOffset  = opndManager->newImm(fieldDesc->getOffset());
    RegOpnd *fieldAddress = opndManager->newRegOpnd(OPND_G_REG, DATA_MPTR);

    add(fieldAddress, fieldOffset, base);
    return fieldAddress;
}

//----------------------------------------------------------------------------//
// Load static field address

CG_OpndHandle *IpfInstCodeSelector::ldStaticAddr(Type *fieldRefType, FieldDesc *fieldDesc) {

    IPF_LOG << "      ldStaticAddr " << fieldDesc->getName() 
        << "(" << Type::tag2str(fieldRefType->tag) << ")";

    Opnd    *dst = opndManager->newImm((uint64) fieldDesc->getAddress());
    IPF_LOG << " addr is " << IrPrinter::toString(dst) << endl;
    return dst;
}

//----------------------------------------------------------------------------//
// Check if an object has given type.
// If it is return 1 else return 0.

CG_OpndHandle *IpfInstCodeSelector::tau_instanceOf(ObjectType    *type, 
                                                   CG_OpndHandle *obj,
                                                   CG_OpndHandle *tauCheckedNull) {
    IPF_LOG << "      tau_instanceOf" << endl;

    Opnd *helperArgs[] = { 
        (Opnd *)obj,
        opndManager->newImm((uint64) type->getRuntimeIdentifier()) 
    };
    
    VM_RT_SUPPORT hId = VM_RT_INSTANCEOF;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    RegOpnd *retOpnd       = opndManager->newRegOpnd(OPND_G_REG, DATA_I32);

    directCall(2, helperArgs, retOpnd, helperAddress, p0);
    return retOpnd;
}

//----------------------------------------------------------------------------//
// Initialize type

void IpfInstCodeSelector::initType(Type *type) {

    IPF_LOG << "      initType" << endl;
    IPF_ASSERT(type->isObject());

    uint64 typeRuntimeId = (uint64) type->asNamedType()->getRuntimeIdentifier();
    Opnd   *helperArgs[] = { opndManager->newImm(typeRuntimeId) };
    
    VM_RT_SUPPORT hId = VM_RT_INITIALIZE_CLASS;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    
    directCall(1, helperArgs, NULL, helperAddress, p0);
}

//----------------------------------------------------------------------------//
// Create new object

CG_OpndHandle *IpfInstCodeSelector::newObj(ObjectType *objType) {

    IPF_LOG << "      newObj" << endl;
    
    Opnd *helperArgs[] = {
        opndManager->newImm((int64) objType->getObjectSize()),
        opndManager->newImm((int64) objType->getAllocationHandle())
    };
    
    VM_RT_SUPPORT hId = VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE;
    uint64   address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd     *helperAddress = opndManager->newImm(address);
    OpndKind opndKind       = toOpndKind(objType->tag);
    DataKind dataKind       = toDataKind(objType->tag);
    RegOpnd  *retOpnd       = opndManager->newRegOpnd(opndKind, dataKind);
    
    directCall(2, helperArgs, retOpnd, helperAddress, p0);
    return retOpnd;
}

//----------------------------------------------------------------------------//
// Create new array

CG_OpndHandle *IpfInstCodeSelector::newArray(ArrayType     *arrayType,
                                             CG_OpndHandle *numElems) {

    IPF_LOG << "      newArray of " << arrayType->getElementType()->getName() << endl;

    Opnd *helperArgs[] = {
        (Opnd *)numElems,
        opndManager->newImm((int64) arrayType->getAllocationHandle())
    };
    
    VM_RT_SUPPORT hId = VM_RT_NEW_VECTOR_USING_VTABLE;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    RegOpnd *retOpnd       = opndManager->newRegOpnd(OPND_G_REG, DATA_BASE);

    directCall(2, helperArgs, retOpnd, helperAddress, p0);
    return retOpnd;
}

//----------------------------------------------------------------------------//
// Create multi-dimensional new array

CG_OpndHandle *IpfInstCodeSelector::newMultiArray(ArrayType      *arrayType, 
                                                   U_32        numDims, 
                                                   CG_OpndHandle **dims) {

    Opnd *helperArgs[2 + numDims];
    
    helperArgs[0] = opndManager->newImm((uint64)arrayType->getRuntimeIdentifier());
    helperArgs[1] = opndManager->newImm(numDims);
    for (U_32 i = 0; i < numDims; i++) {
        helperArgs[i + 2] = (Opnd *)dims[numDims - 1 - i];
    }

    VM_RT_SUPPORT hId = VM_RT_MULTIANEWARRAY_RESOLVED;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    RegOpnd *retOpnd       = opndManager->newRegOpnd(OPND_G_REG, DATA_BASE);

    directCall(2 + numDims, helperArgs, retOpnd, helperAddress, p0);
    return retOpnd;
}
//----------------------------------------------------------------------------//
// Compute address of the first array element

CG_OpndHandle *IpfInstCodeSelector::ldElemBaseAddr(CG_OpndHandle *array) {

    IPF_LOG << "      ldElemBaseAddr " << endl;

    Opnd    *offset   = opndManager->newImm(opndManager->getElemBaseOffset());
    RegOpnd *elemBase = opndManager->newRegOpnd(OPND_G_REG, DATA_MPTR);
    add(elemBase, offset, array);
    
    return elemBase;
}

//----------------------------------------------------------------------------//
// Compute address of the array element given 
// address of the first element and index

CG_OpndHandle *IpfInstCodeSelector::addElemIndex(Type          *elemType,
                                                 CG_OpndHandle *elemBase_,
                                                 CG_OpndHandle *index_) {

    IPF_LOG << "      addElemIndex; type=" << Type::tag2str(elemType->tag) << endl;

    RegOpnd *elemBase = toRegOpnd(elemBase_);
    RegOpnd *index    = toRegOpnd(index_);
    RegOpnd *dst      = opndManager->newRegOpnd(OPND_G_REG, DATA_U64);
    int16   elemSize  = IpfType::getSize(toDataKind(elemType->tag));
    int16   countVal  = 0;

    switch(elemSize) {
        case 1  : countVal = -1; break;
        case 2  : countVal = 1;  break;
        case 4  : countVal = 2;  break;
        case 8  : countVal = 3;  break;
        case 16 : countVal = 0;  break;
        default : IPF_ERR << "elemSize =" << elemSize << endl;
    }
    
    if(countVal >= 0) {
        Opnd    *count = opndManager->newImm(countVal);
        addNewInst(INST_SHLADD, p0, dst, index, count, elemBase);
    } else {
        addNewInst(INST_ADD, p0, dst, index, elemBase);
    }

    return dst;
}

//----------------------------------------------------------------------------//
// Load array length

CG_OpndHandle *IpfInstCodeSelector::tau_arrayLen(Type          *dstType, 
                                                 ArrayType     *arrayType,
                                                 Type          *lenType,
                                                 CG_OpndHandle *base,
                                                 CG_OpndHandle *tauArrayNonNull, 
                                                 CG_OpndHandle *tauIsArray) {

    IPF_LOG << "      tau_arrayLen" << endl;

    uint64  offset      = arrayType->getArrayLengthOffset();
    Opnd    *offsetOpnd = opndManager->newImm(offset);
    RegOpnd *addr       = opndManager->newRegOpnd(OPND_G_REG, DATA_MPTR);
    add(addr, offsetOpnd, base);
    
    return tau_ldInd(dstType, addr, lenType->tag, false, false, NULL, NULL);
}

//----------------------------------------------------------------------------//
// Acquire monitor for an object

void IpfInstCodeSelector::tau_monitorEnter(CG_OpndHandle *obj, 
                                           CG_OpndHandle *tauIsNonNull) {

    IPF_LOG << "      tau_monitorEnter" << endl;
    
    Opnd *helperArgs[] = { (Opnd *)obj };

    VM_RT_SUPPORT hId = VM_RT_MONITOR_ENTER;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    
    directCall(1, helperArgs, NULL, helperAddress, p0);
}

//----------------------------------------------------------------------------//
// Release monitor for an object

void IpfInstCodeSelector::tau_monitorExit(CG_OpndHandle *obj,
                                          CG_OpndHandle *tauIsNonNull) {

    IPF_LOG << "      tau_monitorExit" << endl;

    Opnd *helperArgs[] = { (Opnd *)obj };

    VM_RT_SUPPORT hId = VM_RT_MONITOR_EXIT;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    
    directCall(1, helperArgs, NULL, helperAddress, p0);
}

//----------------------------------------------------------------------------//
// Acquire monitor for a class

void IpfInstCodeSelector::typeMonitorEnter(NamedType *type) {

    IPF_LOG << "      typeMonitorEnter" << endl;

    uint64 typeRuntimeId = (uint64) type->getRuntimeIdentifier();
    Opnd   *helperArgs1[] = { opndManager->newImm(typeRuntimeId) };

    VM_RT_SUPPORT hId1 = VM_RT_CLASS_2_JLC;
    uint64  address1        = (uint64) compilationInterface.getRuntimeHelperAddress(hId1);
    Opnd*   helperAddress1  = opndManager->newImm(address1);
    OpndKind opndKind       = toOpndKind(type->tag);
    DataKind dataKind       = toDataKind(type->tag);
    RegOpnd* retOpnd        = opndManager->newRegOpnd(opndKind, dataKind);
    
    directCall(1, helperArgs1, retOpnd, helperAddress1, p0);

    Opnd   *helperArgs2[] = { retOpnd };

    VM_RT_SUPPORT hId2 = VM_RT_MONITOR_ENTER;
    uint64  address2        = (uint64) compilationInterface.getRuntimeHelperAddress(hId2);
    Opnd*   helperAddress2  = opndManager->newImm(address2);
    
    directCall(1, helperArgs2, NULL, helperAddress2, p0);
}

//----------------------------------------------------------------------------//
// Release monitor for a class

void IpfInstCodeSelector::typeMonitorExit(NamedType *type) {

    IPF_LOG << "      typeMonitorExit" << endl;

    uint64 typeRuntimeId = (uint64) type->getRuntimeIdentifier();
    Opnd   *helperArgs1[] = { opndManager->newImm(typeRuntimeId) };

    VM_RT_SUPPORT hId1 = VM_RT_CLASS_2_JLC;
    uint64  address1        = (uint64) compilationInterface.getRuntimeHelperAddress(hId1);
    Opnd*   helperAddress1  = opndManager->newImm(address1);
    OpndKind opndKind       = toOpndKind(type->tag);
    DataKind dataKind       = toDataKind(type->tag);
    RegOpnd* retOpnd        = opndManager->newRegOpnd(opndKind, dataKind);
    
    directCall(1, helperArgs1, retOpnd, helperAddress1, p0);

    Opnd   *helperArgs2[] = { retOpnd };

    VM_RT_SUPPORT hId2 = VM_RT_MONITOR_EXIT;
    uint64  address2        = (uint64) compilationInterface.getRuntimeHelperAddress(hId2);
    Opnd*   helperAddress2  = opndManager->newImm(address2);
    
    directCall(1, helperArgs2, NULL, helperAddress2, p0);
}

//----------------------------------------------------------------------------//
// Wrapper around balanced monitor enter. Set cmpxchg role to BalancedMonitorEnter

CG_OpndHandle *IpfInstCodeSelector::tau_balancedMonitorEnter(CG_OpndHandle *obj, 
                                                             CG_OpndHandle *lockAddr,
                                                             CG_OpndHandle *tauIsNonNull) {
    IPF_LOG << "      tau_balancedMonitorEnter" << endl;

    tau_monitorEnter(obj, tauIsNonNull);
    return opndManager->getR0();
}

//----------------------------------------------------------------------------//
// Balanced monitor exit

void IpfInstCodeSelector::balancedMonitorExit(CG_OpndHandle *obj, 
                                              CG_OpndHandle *lockAddr, 
                                              CG_OpndHandle *oldLock) {
    IPF_LOG << "      balancedMonitorExit" << endl;

    tau_monitorExit(obj, NULL);
}

//----------------------------------------------------------------------------//
// CG helper protected methods 
//----------------------------------------------------------------------------//

//----------------------------------------------------------------------------//
// Create direct call to the method 

void IpfInstCodeSelector::directCall(U_32    numArgs, 
                                     Opnd      **args, 
                                     RegOpnd   *retOpnd,
                                     Opnd      *methodAddress,
                                     RegOpnd   *pred,
                                     Completer whetherHint) {

    RegOpnd *b0       = opndManager->getB0();
    RegOpnd *convOpnd = makeConvOpnd(retOpnd);
    Inst    *callInst = new(mm) Inst(mm, INST_BRL13, CMPLT_BTYPE_CALL, whetherHint, CMPLT_PH_MANY
        , pred, convOpnd, b0, methodAddress);
    
    opndManager->setContainCall(true);           // set flag OpndManager::containCall
    makeCallArgs(numArgs, args, callInst, pred); // add instructions moving args in appropriate locations

    addInst(callInst);                           // add "call" inst 
    makeRetVal(retOpnd, convOpnd, pred);         // add instruction moving ret value from r8/f8
}

//----------------------------------------------------------------------------//
// Create indirect call to the method 

void IpfInstCodeSelector::indirectCall(U_32    numArgs, 
                                       Opnd      **args, 
                                       RegOpnd   *retOpnd,
                                       RegOpnd   *methodPtr,
                                       RegOpnd   *pred,
                                       Completer whetherHint) {

    RegOpnd *b0        = opndManager->getB0();
    RegOpnd *convOpnd  = makeConvOpnd(retOpnd);
    RegOpnd *callTgt   = opndManager->newRegOpnd(OPND_B_REG, DATA_U64);
    Inst    *ldAddress = new(mm) Inst(mm, INST_MOV, pred, callTgt, methodPtr);
    Inst    *callInst  = new(mm) Inst(mm, INST_BR13, CMPLT_BTYPE_CALL, CMPLT_WH_SPTK, CMPLT_PH_MANY, pred, convOpnd, b0, callTgt);

    opndManager->setContainCall(true);           // set flag OpndManager::containCall (method contains call)
    makeCallArgs(numArgs, args, callInst, pred); // add instructions moving args in appropriate locations

    addInst(ldAddress);                          // add inst loading method address 
    addInst(callInst);                           // add "call" inst 
    makeRetVal(retOpnd, convOpnd, pred);         // add instruction moving ret value from r8/f8
}

//----------------------------------------------------------------------------//
// Move out args in regs and stack positions according with IPF software convention

void IpfInstCodeSelector::makeCallArgs(U_32 numArgs, Opnd **args, Inst *callInst, RegOpnd *pred) {
    
    int16 numFpOutArgs = 0;
    for(U_32 argPosition=0; argPosition<numArgs; argPosition++) {
        
        Opnd     *opnd    = args[argPosition];
        OpndKind opndKind = OPND_INVALID;
        DataKind dataKind = DATA_INVALID;
        InstCode instCode = INST_INVALID;
        I_32    location = LOCATION_INVALID;
        bool     isFp     = opnd->isFloating();

        if (opnd->isReg() == true) {                                  // opnd is register
            opndKind   = opnd->getOpndKind();                         // outArg has the same opndKind
            dataKind   = opnd->getDataKind();                         // outArg has the same dataKind
            instCode   = INST_MOV;
        } else {                                                      // opnd is imm
            opndKind   = OPND_G_REG;
            dataKind   = DATA_U64;
            instCode   = opnd->isFoldableImm(22) ? INST_MOV : INST_MOVL;
        }

        if (argPosition < MAX_REG_ARG) {                              // arg is going on register
            if (isFp) location = F_OUTARG_BASE + numFpOutArgs++;      // real location
            else      location = opndManager->newOutReg(argPosition); // temporary location
        } else {                                                      // arg is going on stack
            location = opndManager->newOutSlot(argPosition);          // location is area local offset
        }

        Opnd *outArg = opndManager->newRegOpnd(opndKind, dataKind, location);

        addNewInst(instCode, pred, outArg, opnd); 
        callInst->addOpnd(outArg);                                    // add the opnd in call opnds list (for data flow analysis)
    }
}

//----------------------------------------------------------------------------//
// make opnd representing return value of method (r8/f8)

RegOpnd *IpfInstCodeSelector::makeConvOpnd(RegOpnd *retOpnd) {

    if(retOpnd == NULL) return opndManager->getR0(); // method has return type "void"

    if (retOpnd->isFloating()) return opndManager->getF8();
    else                       return opndManager->getR8();
}

//----------------------------------------------------------------------------//
// create mov to free r8/f8

void IpfInstCodeSelector::makeRetVal(RegOpnd *retOpnd, RegOpnd *convOpnd, RegOpnd *pred) {

    if(retOpnd == NULL) return;      // method has return type "void"
    
    addNewInst(INST_MOV, pred, retOpnd, convOpnd);
}

//----------------------------------------------------------------------------//
// Generate "cmp" instruction for two opnds (int, float or imm). 

void IpfInstCodeSelector::cmp(InstCode      instCode,
                              Completer     cmpRelation, 
                              RegOpnd       *truePred,
                              RegOpnd       *falsePred,
                              CG_OpndHandle *src1_, 
                              CG_OpndHandle *src2_) {

    RegOpnd *src1 = toRegOpnd(src1_);
    RegOpnd *src2 = toRegOpnd(src2_);
    
    if (instCode == INST_CMP)  { sxt(src1, 8); sxt(src2, 8); }
    if (instCode == INST_CMP4) { sxt(src1, 4); sxt(src2, 4); }

    addNewInst(instCode, cmpRelation, p0, truePred, falsePred, src1, src2);
}

//----------------------------------------------------------------------------//
// Choose and generate appropriate "cmp" instruction for two opnds (int, float or imm). 

void IpfInstCodeSelector::cmp(Completer     cmpRelation, 
                              RegOpnd       *truePred,
                              RegOpnd       *falsePred,
                              CG_OpndHandle *src1_, 
                              CG_OpndHandle *src2_) {

    Opnd *src1 = (Opnd *)src1_;
    Opnd *src2 = (Opnd *)src2_;
    
    // fcmp
    if (src1->isFloating() || src2->isFloating()) {
        cmp(INST_FCMP, cmpRelation, truePred, falsePred, src1, src2);
        return;
    }
    
    // cmp
    if (src1->getSize()>4 || src1->getSize()>4) {
        cmp(INST_CMP, cmpRelation, truePred, falsePred, src1, src2);
        return;
    } 

    // cmp4    
    cmp(INST_CMP4, cmpRelation, truePred, falsePred, src1, src2);
}

//----------------------------------------------------------------------------//

void IpfInstCodeSelector::sxt(CG_OpndHandle *src_, int16 refSize, Completer srcSize) {

    Opnd *src = (Opnd *)src_;
    
    if (src->isReg()      == false) return;  // ignore non reg opnd (imm)
    if (src->isFloating() == true)  return;  // ignore fp opnd
    if (src->isSigned()   == false) return;  // ignore unsigned opnd
    
    Completer cmplt    = (srcSize==CMPLT_INVALID ? toCmpltSz(src->getDataKind()) : srcSize);
    int16     srcbytes = (cmplt==CMPLT_SZ_1 ? 1 : (cmplt==CMPLT_SZ_2 ? 2 : (cmplt==CMPLT_SZ_4?4:8)));
    if (srcbytes >= refSize) return;         // nothing to do

    addNewInst(INST_SXT, cmplt, p0, src, src);
}

//----------------------------------------------------------------------------//

void IpfInstCodeSelector::zxt(CG_OpndHandle *src_, int16 refSize, Completer srcSize) {

    Opnd *src = (Opnd *)src_;
    
    if (src->isReg()      == false) return;  // ignore non reg opnd (imm)
    if (src->isFloating() == true)  return;  // ignore fp opnd
    
    Completer cmplt    = (srcSize==CMPLT_INVALID?toCmpltSz(src->getDataKind()):srcSize);
    int16     srcbytes = (cmplt==CMPLT_SZ_1?1:(cmplt==CMPLT_SZ_2?2:(cmplt==CMPLT_SZ_4?4:8)));
    if (srcbytes >= refSize) return;         // nothing to do

    addNewInst(INST_ZXT, cmplt, p0, src, src);
}

//----------------------------------------------------------------------------//
// If opnd is imm this method generates "mov" from imm to gr
// if imm==0 then return r0
//
RegOpnd *IpfInstCodeSelector::toRegOpnd(CG_OpndHandle *opnd_) {

    if(((Opnd *)opnd_)->isReg()) return (RegOpnd *)opnd_;
    IPF_ASSERT(((Opnd *)opnd_)->isImm());

    Opnd     *opnd    = (Opnd *)opnd_;
    if (opnd->getValue()==0) {
        return opndManager->getR0();
    } else {
        RegOpnd  *dst     = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);
        InstCode instCode = opnd->isFoldableImm(22) ? INST_MOV : INST_MOVL;
        
        addNewInst(instCode, p0, dst, opnd);
        return dst;
    }
}

//----------------------------------------------------------------------------//
// Add int or fp values

void IpfInstCodeSelector::add(RegOpnd *dst, CG_OpndHandle *src1_, CG_OpndHandle *src2_) {

    Opnd    *src1 = (Opnd *)src1_;
    Opnd    *src2 = (Opnd *)src2_;

    // fadd fr, fr
    if(dst->isFloating()) {
        Completer cmplt = CMPLT_PC_DYNAMIC;
        
        switch (dst->getDataKind()) {
        case DATA_D: cmplt = CMPLT_PC_DOUBLE; break;
        case DATA_S: cmplt = CMPLT_PC_SINGLE; break;
        default: IPF_ERR << "bad data kind for float add\n"; break;
        }
        addNewInst(INST_FADD, cmplt, p0, dst, src1, src2);
        return;
    }

    sxt(src1, dst->getSize());    // sxt src1 if appropriate
    sxt(src2, dst->getSize());    // sxt src2 if appropriate

    // imm opnd must be on first position
    if(src2->isImm()) {
        Opnd *buf = src1;
        src1      = src2;
        src2      = toRegOpnd(buf);
    }
    
    // add imm, gr
    if (src1->isImm()) {
        // imm14
        if(src1->isFoldableImm(14)) {
            addNewInst(INST_ADDS, p0, dst, src1, src2);
            return;
        } 
        // imm
        RegOpnd *buf = toRegOpnd(src1);
        addNewInst(INST_ADD, p0, dst, buf, src2);
        return;
    } 

    // add gr, gr
    addNewInst(INST_ADD, p0, dst, src1, src2);
}

//----------------------------------------------------------------------------//
// Sub int or fp values

void IpfInstCodeSelector::sub(RegOpnd *dst, CG_OpndHandle *src1_, CG_OpndHandle *src2_) {

    Opnd    *src1 = (Opnd *)src1_;
    Opnd    *src2 = (Opnd *)src2_;

    // sub fr, fr
    if(dst->isFloating()) {
        Completer cmplt = CMPLT_PC_DYNAMIC;
        
        switch (dst->getDataKind()) {
        case DATA_D: cmplt = CMPLT_PC_DOUBLE; break;
        case DATA_S: cmplt = CMPLT_PC_SINGLE; break;
        default: IPF_ERR << "bad data kind for float sub\n"; break;
        }
        addNewInst(INST_FSUB, cmplt, p0, dst, src1, src2);
        return;
    }

    sxt(src1, dst->getSize());    // sxt src1 if appropriate
    sxt(src2, dst->getSize());    // sxt src2 if appropriate

    // imm opnd must be on first position
    if(src2->isImm()) {
        Opnd *buf = src2;
        src2      = toRegOpnd(buf);
    }
    
    // sub imm, gr
    if (src1->isImm()) {

        // imm8
        if(src1->isFoldableImm(8)) {
            addNewInst(INST_SUB, p0, dst, src1, src2);
            return;
        } 
        
        // imm
        RegOpnd *buf = toRegOpnd(src1);
        addNewInst(INST_SUB, p0, dst, buf, src2);
        return;
    } 

    // sub gr, gr
    addNewInst(INST_SUB, p0, dst, src1, src2);
}

//----------------------------------------------------------------------------//
// Only INST_AND, INST_OR, INST_XOR

void IpfInstCodeSelector::binOp(InstCode       instCode,
                                RegOpnd        *dst,
                                CG_OpndHandle  *src1_,
                                CG_OpndHandle  *src2_)  {

    IPF_ASSERT(instCode==INST_AND || instCode==INST_OR || instCode==INST_XOR);
    
    Opnd    *src1 = (Opnd *)src1_;
    Opnd    *src2 = (Opnd *)src2_;

    IPF_ASSERT(src1->isReg() || src2->isReg());

    // imm opnd must be on first position
    if(src2->isImm()) {
        Opnd *buf = src1;
        src1      = src2;
        src2      = toRegOpnd(buf);
    }
    
    sxt(src1, dst->getSize());    // sxt src1 if appropriate
    sxt(src2, dst->getSize());    // sxt src2 if appropriate

    // sub imm, gr
    if (src1->isImm()) {

        // imm8
        if(src1->isFoldableImm(8)) {
            addNewInst(instCode, p0, dst, src1, src2);
            return;
        } 
        
        // imm
        RegOpnd *buf = toRegOpnd(src1);
        addNewInst(instCode, p0, dst, buf, src2);
        return;
    } 

    // sub gr, gr
    addNewInst(instCode, p0, dst, src1, src2);
}

//----------------------------------------------------------------------------//

void IpfInstCodeSelector::shift(InstCode      instCode,
                                RegOpnd       *dst,
                                CG_OpndHandle *value_,
                                CG_OpndHandle *count_,
                                int            bits)  {

    RegOpnd *value = toRegOpnd(value_);
    Opnd    *count = (Opnd *)count_;

    sxt(value, dst->getSize());

    if (count->isImm()) {
        if (!count->isFoldableImm(bits)) {
//            RegOpnd *tmp = toRegOpnd(count);
//            
//            addNewInst(INST_AND, p0, tmp, opndManager->newImm(bits==5?0x1F:0x2F), tmp);
            count = opndManager->newImm(count->getValue() & (bits==5?0x1F:0x3F));
        }
    } else {
        RegOpnd *tmp = (RegOpnd *)count;
        
        if (value_==count_) {
            tmp = opndManager->newRegOpnd(OPND_G_REG, count->getDataKind());
            addNewInst(INST_MOV, p0, tmp, count);
        }
        
        addNewInst(INST_AND, p0, tmp, opndManager->newImm(bits==5?0x1F:0x3F), tmp);
        count = tmp;
    }
    
    // shx gr = gr, imm
    if (count->isImm()) {

        // imm6
        if(count->isFoldableImm(6)) {
            addNewInst(instCode, p0, dst, value, count);
            return;
        } 
        
        // imm
        RegOpnd *buf = toRegOpnd(count);
        addNewInst(instCode, p0, dst, value, buf);
        return;
    } 

    // shx gr = gr, gr
    addNewInst(instCode, p0, dst, value, count);
}

//----------------------------------------------------------------------------//
// Load N-bit integer constant

void IpfInstCodeSelector::ldc(RegOpnd *dst, int64 val) {

//    if (val == 0) {
//        IPF_LOG << "        opnd \"r0\"" << endl;
//        return dst->setLocation(0);
//    }

    Opnd     *immOpnd = opndManager->newImm(val);
    InstCode instCode = immOpnd->isFoldableImm(22) ? INST_MOV : INST_MOVL;
    
    addNewInst(instCode, p0, dst, immOpnd);
}

//----------------------------------------------------------------------------//

void IpfInstCodeSelector::minMax(RegOpnd       *dst,
                                 CG_OpndHandle *src1_, 
                                 CG_OpndHandle *src2_,
                                 bool          max) {

    Opnd    *src1 = (Opnd *)src1_;
    Opnd    *src2 = (Opnd *)src2_;

    if (dst->isFloating()) {
        // TODO: check all the peculiarities of Math.min/max
        InstCode instCode = max ? INST_FMAX : INST_FMIN;
        addNewInst(instCode, p0, dst, src1, src2);
    } else {
        Completer crel      = max ? CMPLT_CMP_CREL_LT : CMPLT_CMP_CREL_GT;
        RegOpnd   *truePred  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
        RegOpnd   *falsePred = opndManager->newRegOpnd(OPND_P_REG, DATA_P);

        cmp(crel, truePred, falsePred, src1, src2);
        addNewInst(INST_MOV, truePred, dst, src1);
        addNewInst(INST_MOV, falsePred, dst, src2);
     }
}

//----------------------------------------------------------------------------//
// Generate instruction sequence for integer multiplication

void IpfInstCodeSelector::xma(InstCode      instCode,
                              RegOpnd       *dst,
                              CG_OpndHandle *src1, 
                              CG_OpndHandle *src2) {

    IPF_ASSERT(((Opnd *)src1)->isReg());
    IPF_ASSERT(((Opnd *)src2)->isReg());

    RegOpnd *f0   = opndManager->getF0();
    RegOpnd *buf1 = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    RegOpnd *buf2 = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    RegOpnd *dstF = opndManager->newRegOpnd(OPND_F_REG, DATA_D);

    sxt(src1, 8);    // sxt src1 if appropriate
    sxt(src2, 8);    // sxt src2 if appropriate

    addNewInst(INST_SETF_SIG, p0, buf1, src1);
    addNewInst(INST_SETF_SIG, p0, buf2, src2);
    addNewInst(instCode,      p0, dstF, buf1, buf2, f0);
    addNewInst(INST_GETF_SIG, p0, dst, dstF);
}

//----------------------------------------------------------------------------//
// Convert floating-point to 64-bit integer with saturation
//
//      ldfd       tf1 = ALMOST_MAXINT8 (see below for details)
//      mov        t1  = -1  
//      fcvt.fx    tf2 = src
//      fcmp.gt    p1  = src, tf1
//      fcmp.unord p2  = src, src
//      getf.sig   dst = tf2
// (p1) shr.u      dst = t1, 1
// (p2) mov        dst = 0
//

void IpfInstCodeSelector::saturatingConv8(RegOpnd *dst, CG_OpndHandle *src_) {
    
    IPF_LOG << "      saturatingConv8" << endl;

    IPF_ASSERT(((Opnd *)src_)->isFloating());

    RegOpnd *src = (RegOpnd *)src_;
    RegOpnd *tf1 = NULL;
    RegOpnd *tf2 = opndManager->newRegOpnd(OPND_F_REG, src->getDataKind());
    RegOpnd *p1  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd *p2  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd *t1  = opndManager->newRegOpnd(OPND_G_REG, dst->getDataKind());

    if (src->getDataKind() == DATA_S) {
        // maximal int64 that is exactly representable in single FP format
        tf1 = (RegOpnd *)ldc_s((float)__INT64_C(0x7fffff0000000000));
    } else {
        // maximal int64 that is exactly representable in double FP format
        int64 max_int64 = __INT64_C(0x7ffffffffffff800);
        tf1 = (RegOpnd *)ldc_d((double)max_int64);
    }

    sxt(src, 8);    // sxt src if appropriate
    addNewInst(INST_MOV, p0, t1, opndManager->newImm(-1));
    addNewInst(INST_FCVT_FX_TRUNC, p0, tf2, src);
    addNewInst(INST_FCMP, CMPLT_CMP_CREL_GT, CMPLT_FCMP_FCTYPE_NONE, p0, p1, p0, src, tf1);
    addNewInst(INST_FCMP, CMPLT_FCMP_FREL_UNORD, CMPLT_FCMP_FCTYPE_NONE, p0, p2, p0, src, src);
    addNewInst(INST_GETF_SIG, p0, dst, tf2);
    addNewInst(INST_SHR_U, p1, dst, t1, opndManager->newImm(1));
    addNewInst(INST_MOV, p2, dst, opndManager->newImm(0));
}

//----------------------------------------------------------------------------//
// Convert floating-point to 32-bit integer with saturation. Then sign/zero
// extend based on dstType.
//
//      ldfd     tf1 = MAXINT4
//      ldfd     tf2 = MININT4
//      mov      t1  = -1  
//      fcvt.fx  tf3 = src
//      fcmp.gt  p1  = src, tf1
//      fcmp.lt  p2  = src, tf2
//      getf.sig t2  = tf3
//      sxt4     dst = t2
// (p1) shr.u    dst = t1, 33
// (p2) shl      dst = t1, 31
//
// New code
//             ldfd       tf = MAXINT4
//             fcmp.lt    p11,p10 = src, tf
// (p10)       movl       dst = 0x7fffffff
// (p10)       cmp.ne     p10 = r0,r0
//
// (p11)       ldfd       tf = MININT4
// (p11)       fcmp.gt    p11,p10 = src, tf
// (p10)       movl       dst = 0x80000000
//
// (p11)       fcvt.fx    tf = src
// (p11)       getf.sig   dst = tf

void IpfInstCodeSelector::saturatingConv4(RegOpnd *dst, CG_OpndHandle *src_) {

    IPF_LOG << "      saturatingConv4" << endl;

    IPF_ASSERT(((Opnd *)src_)->isFloating());
    
    union {
        float  fr;
        U_32 gr;
    } fval;
    union {
        double fr;
        uint64 gr;
    } dval;
    
    RegOpnd *r0  = opndManager->getR0();
    RegOpnd *src = (RegOpnd *)src_;
    RegOpnd *p11 = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd *p10 = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd *tf  = NULL;
    RegOpnd *tr  = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);

    if (src->getDataKind() == DATA_S) {
        tf = opndManager->newRegOpnd(OPND_F_REG, DATA_S);
        fval.fr = (float)0x7fffffff;
        addNewInst(INST_MOVL, p0, tr, opndManager->newImm(fval.gr));
        addNewInst(INST_SETF_S, p0, tf, tr);
    } else {
        tf = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
        dval.fr = (double)0x7fffffff;
        addNewInst(INST_MOVL, p0, tr, opndManager->newImm(dval.gr));
        addNewInst(INST_SETF_D, p0, tf, tr);
    }
    addNewInst(INST_FCMP, CMPLT_CMP_CREL_LT, p0, p11, p10, src, tf);
    addNewInst(INST_MOVL, p10, dst, opndManager->newImm(0x7fffffff));
    addNewInst(INST_CMP, CMPLT_CMP_CREL_NE, p10, p10, p0, r0, r0);

    if (src->getDataKind() == DATA_S) {
        fval.fr = (float)((int)0x80000000);
        addNewInst(INST_MOVL, p11, tr, opndManager->newImm(fval.gr));
        addNewInst(INST_SETF_S, p11, tf, tr);
    } else {
        tf = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
        dval.fr = (double)((int) 0x80000000);
        addNewInst(INST_MOVL, p11, tr, opndManager->newImm(dval.gr));
        addNewInst(INST_SETF_D, p11, tf, tr);
    }
    addNewInst(INST_FCMP, CMPLT_CMP_CREL_GT, p11, p11, p10, src, tf);
    addNewInst(INST_MOVL, p10, dst, opndManager->newImm(0x80000000));

    addNewInst(INST_FCVT_FX_TRUNC, p11, tf, src);
    addNewInst(INST_GETF_SIG, p11, dst, tf);

}

//----------------------------------------------------------------------------//
// Divide two integer values. 

void IpfInstCodeSelector::divInt(RegOpnd *dst, CG_OpndHandle *src1, CG_OpndHandle *src2, bool rem) {

    IPF_ASSERT(((Opnd *)src1)->isReg());
    IPF_ASSERT(((Opnd *)src2)->isReg());
    
    RegOpnd *f0   = opndManager->getF0();
    RegOpnd *f1   = opndManager->getF1();
    RegOpnd *p6   = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd *f6   = opndManager->newRegOpnd(OPND_F_REG, DATA_D); 
    RegOpnd *f7   = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    RegOpnd *f8   = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    RegOpnd *f9   = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    RegOpnd *f10  = opndManager->newRegOpnd(OPND_F_REG, DATA_D);

    sxt(src1, 8);
    sxt(src2, 8);

    addNewInst(INST_DEF,                    p0,f6 );
    addNewInst(INST_DEF,                    p0,f7 );
    addNewInst(INST_DEF,                    p0,f8 );
    addNewInst(INST_DEF,                    p0,f9 );
    addNewInst(INST_DEF,                    p0,f10);
    
    addNewInst(INST_SETF_SIG, p0, f10, src1);
    addNewInst(INST_SETF_SIG, p0, f9,  src2);
    addNewInst(INST_FCVT_XF,  p0, f6,  f10);
    addNewInst(INST_FCVT_XF,  p0, f7,  f9);
    addNewInst(INST_MOV,      p0, dst, opndManager->newImm(0x0ffdd));
    addNewInst(INST_SETF_EXP, p0, f9,  dst);

    addNewInst(INST_FRCPA, p0, f8, p6, f6, f7); 
    addNewInst(INST_FMA,   p6, f6, f6, f8, f0);
    addNewInst(INST_FNMA,  p6, f7, f7, f8, f1);
    addNewInst(INST_FMA,   p6, f6, f7, f6, f6);
    addNewInst(INST_FMA,   p6, f7, f7, f7, f9);
    addNewInst(INST_FMA,   p6, f8, f7, f6, f6);
    addNewInst(INST_FCVT_FX_TRUNC, p0, f8, f8);

    if (rem) {
        RegOpnd *msrc2 = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);
        addNewInst(INST_SUB, p0, msrc2, opndManager->getR0(), src2);
        addNewInst(INST_SETF_SIG, p0, f9, msrc2);
        addNewInst(INST_XMA_L, p0, f8,  f9, f8, f10);
    }

    // result is in the least significant 32 bits of r8 (if b != 0)
    addNewInst(INST_GETF_SIG, p0, dst, f8);
}

//----------------------------------------------------------------------------//
// Divide two long values. 

void IpfInstCodeSelector::divLong(RegOpnd *dst, CG_OpndHandle *src1, CG_OpndHandle *src2, bool rem) {

    IPF_ASSERT(((Opnd *)src1)->isReg());
    IPF_ASSERT(((Opnd *)src2)->isReg());
    
    RegOpnd *f0   = opndManager->getF0();
    RegOpnd *f1   = opndManager->getF1();
    RegOpnd *p6   = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
    RegOpnd *f6   = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    RegOpnd *f7   = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    RegOpnd *f8   = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    RegOpnd *f9   = opndManager->newRegOpnd(OPND_F_REG, DATA_D);
    RegOpnd *f10  = opndManager->newRegOpnd(OPND_F_REG, DATA_D);

    sxt(src1, 8);
    sxt(src2, 8);

    addNewInst(INST_DEF,                    p0,f6 );
    addNewInst(INST_DEF,                    p0,f7 );
    addNewInst(INST_DEF,                    p0,f8 );
    addNewInst(INST_DEF,                    p0,f9 );
    addNewInst(INST_DEF,                    p0,f10);
    
    addNewInst(INST_SETF_SIG, p0, f10, src1);
    addNewInst(INST_SETF_SIG, p0, f9, src2);

    addNewInst(INST_FCVT_XF, p0, f6, f10);
    addNewInst(INST_FCVT_XF, p0, f7, f9);

    addNewInst(INST_FRCPA, p0, f8, p6, f6, f7);
    addNewInst(INST_FNMA,  p6, f9, f7, f8, f1);
    addNewInst(INST_FMA,   p6, f8, f9, f8, f8);
    addNewInst(INST_FMA,   p6, f9, f9, f9, f0);
    addNewInst(INST_FMA,   p6, f8, f9, f8, f8);
    addNewInst(INST_FMA,   p6, f9, f8, f6, f0);
    addNewInst(INST_FNMA,  p6, f7, f7, f9, f6);
    addNewInst(INST_FMA,   p6, f8, f7, f8, f9);
    addNewInst(INST_FCVT_FX_TRUNC, p0, f8, f8);

    if (rem) {
        RegOpnd *msrc2 = opndManager->newRegOpnd(OPND_G_REG, DATA_I64);
        addNewInst(INST_SUB, p0, msrc2, opndManager->getR0(), src2);
        addNewInst(INST_SETF_SIG, p0, f9, msrc2);
        addNewInst(INST_XMA_L, p0, f8,  f9, f8, f10);
    }
    
    addNewInst(INST_GETF_SIG, p0, dst, f8);
}

//----------------------------------------------------------------------------//
// Divide two double values. 

void IpfInstCodeSelector::divDouble(RegOpnd *dst, CG_OpndHandle *src1, CG_OpndHandle *src2, bool rem) {

    IPF_ASSERT(((Opnd *)src1)->isReg());
    IPF_ASSERT(((Opnd *)src2)->isReg());
    
    if (!rem) {
        RegOpnd *f0 = opndManager->getF0();
        RegOpnd *f1 = opndManager->getF1();

        RegOpnd *fRes = dst, *fA = (RegOpnd *)src1, *fB = (RegOpnd *)src2;
        RegOpnd *pX = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
        RegOpnd *fe = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fq0 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fq1 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fe2 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fy1 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fe4 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fq2 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fy2 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fq3 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fy3 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fr = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 

        //
        //  Group 0
        //           frcpa.s0   fRes,pX = fA,fB        
        //  Group 1
        //      (pX) fnma.s1    fe    = fRes,fB,f1    
        //      (pX) fmpy.s1    fq0   = fA,fRes       
        //  Group 2
        //      (pX) fmpy.s1    fe2   = fe,fe      
        //      (pX) fma.s1     fq1   = fq0,fe,fq0   
        //      (pX) fma.s1     fy1   = fRes,fe,fRes 
        //  Group 3
        //      (pX) fmpy.s1    fe4   = fe2,fe2        
        //      (pX) fma.s1     fq2   = fq1,fe2,fq1
        //      (pX) fma.s1     fy2   = fy1,fe2,fy1
        //  Group 4
        //      (pX) fma.d.s1   fq3   = fq2,fe4,fq2
        //      (pX) fma.s1     fy3   = fy2,fe4,fy2
        //  Group 5
        //      (pX) fnma.s1    fr    = fB,fq3,fA
        //  Group 6
        //      (pX) fma.d.s0   fRes  = fr,fy3,fq3
        //
        addNewInst(INST_DEF,                    p0, fe );
        addNewInst(INST_DEF,                    p0, fq0);
        addNewInst(INST_DEF,                    p0, fq1);
        addNewInst(INST_DEF,                    p0, fe2);
        addNewInst(INST_DEF,                    p0, fy1);
        addNewInst(INST_DEF,                    p0, fe4);
        addNewInst(INST_DEF,                    p0, fq2);
        addNewInst(INST_DEF,                    p0, fy2);
        addNewInst(INST_DEF,                    p0, fq3);
        addNewInst(INST_DEF,                    p0, fy3);
        addNewInst(INST_DEF,                    p0, fr );

        addNewInst(INST_FRCPA,                CMPLT_SF0, p0, fRes, pX,   fA,   fB);

        addNewInst(INST_FNMA,                 CMPLT_SF1, pX, fe,   fRes, fB,   f1);
        addNewInst(INST_FMA,                  CMPLT_SF1, pX, fq0,  fA,   fRes, f0);

        addNewInst(INST_FMA,                  CMPLT_SF1, pX, fe2,  fe,   fe,   f0);
        addNewInst(INST_FMA,                  CMPLT_SF1, pX, fq1,  fq0,  fe,   fq0);
        addNewInst(INST_FMA,                  CMPLT_SF1, pX, fy1,  fRes, fe,   fRes);

        addNewInst(INST_FMA,                  CMPLT_SF1, pX, fe4,  fe2,  fe2,  f0);
        addNewInst(INST_FMA,                  CMPLT_SF1, pX, fq2,  fq1,  fe2,  fq1);
        addNewInst(INST_FMA,                  CMPLT_SF1, pX, fy2,  fy1,  fe2,  fy1);
        
        addNewInst(INST_FMA, CMPLT_PC_DOUBLE, CMPLT_SF1, pX, fq3,  fq2,  fe4,  fq2);
        addNewInst(INST_FMA,                  CMPLT_SF1, pX, fy3,  fy2,  fe4,  fy2);

        addNewInst(INST_FNMA,                 CMPLT_SF1, pX, fr,   fB,   fq3,  fA);

        addNewInst(INST_FMA, CMPLT_PC_DOUBLE, CMPLT_SF0, pX, fRes, fr,   fy3,  fq3);
    } else {
        // Call internal helper to do FP remainder. We only inline the integer  
        // remainder sequence
        //
        Opnd *helperArgs[] = { (Opnd *)src1, (Opnd *)src2 };
    
        uint64  address        = (uint64) remF8;
        Opnd    *helperAddress = opndManager->newImm(address);
        
        directCall(2, helperArgs, dst, helperAddress, p0);
    }
}                                       

//----------------------------------------------------------------------------//

CG_OpndHandle *IpfInstCodeSelector::ldRef(Type *dstType,
                                          MethodDesc* enclosingMethod,
                                          U_32 refToken,
                                          bool uncompress) 
{
    assert(dstType->isSystemString() || dstType->isSystemClass());
    
    RegOpnd *retOpnd = opndManager->newRegOpnd(OPND_G_REG, toDataKind(dstType->tag));
    Opnd *helperArgs[] = {
        opndManager->newImm(refToken),
        opndManager->newImm((int64) enclosingMethod->getParentType()->getRuntimeIdentifier())
    };
    VM_RT_SUPPORT hId = VM_RT_LDC_STRING;
    uint64  address        = (uint64) compilationInterface.getRuntimeHelperAddress(hId);
    Opnd    *helperAddress = opndManager->newImm(address);
    
    directCall(2, helperArgs, retOpnd, helperAddress, p0);

    return retOpnd;
}

//----------------------------------------------------------------------------//

void IpfInstCodeSelector::methodEntry(MethodDesc *meth) { 
    
    if (compilationInterface.getCompilationParams().exe_notify_method_entry) {
        NOT_IMPLEMENTED_V("methodEntry"); 
    }

}

void IpfInstCodeSelector::methodEnd(MethodDesc *meth, CG_OpndHandle *retopnd) { 
    
    if (compilationInterface.getCompilationParams().exe_notify_method_exit) {
        NOT_IMPLEMENTED_V("methodEnd"); 
    }

}

//----------------------------------------------------------------------------//
// Divide two float values. 

void IpfInstCodeSelector::divFloat(RegOpnd *dst, CG_OpndHandle *src1, CG_OpndHandle *src2, bool rem) {

    IPF_ASSERT(((Opnd *)src1)->isReg());
    IPF_ASSERT(((Opnd *)src2)->isReg());

    if (!rem) {    
        RegOpnd *f0 = opndManager->getF0();
        RegOpnd *f1 = opndManager->getF1();

        RegOpnd *fRes = dst, *fA = (RegOpnd *)src1, *fB = (RegOpnd *)src2;
        RegOpnd *pX  = opndManager->newRegOpnd(OPND_P_REG, DATA_P);
        RegOpnd *fe  = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fq0 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fq1 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fe2 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fe4 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fq2 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
        RegOpnd *fq3 = opndManager->newRegOpnd(OPND_F_REG, DATA_F); 
    
        //
        //  Group 0
        //           frcpa.s0   fRes,pX = fA,fB     
        //  Group 1
        //      (pX) fnma.s1    fe    = fRes,fB,f1       
        //      (pX) fmpy.s1    fq0   = fA,fRes        
        //  Group 2
        //      (pX) fmpy.s1    fe2   = fe,fe       
        //      (pX) fma.s1     fq1   = fq0,fe,fq0   
        //  Group 3
        //      (pX) fmpy.s1    fe4   = fe2,fe2      
        //      (pX) fma.s1     fq2   = fq1,fe2,fq1
        //  Group 4
        //      (pX) fma.d.s1   fq3   = fq2,fe4,fq2
        //      (pX) fnorm.s.s0 fRes  = fq3
        //

        addNewInst(INST_DEF,                    p0, fe );
        addNewInst(INST_DEF,                    p0, fq0);
        addNewInst(INST_DEF,                    p0, fq1);
        addNewInst(INST_DEF,                    p0, fe2);
        addNewInst(INST_DEF,                    p0, fe4);
        addNewInst(INST_DEF,                    p0, fq2);
        addNewInst(INST_DEF,                    p0, fq3);
        
        addNewInst(INST_FRCPA,                  CMPLT_SF0, p0, fRes, pX,   fA,   fB);

        addNewInst(INST_FNMA,                   CMPLT_SF1, pX, fe,   fRes, fB,   f1);
        addNewInst(INST_FMA,                    CMPLT_SF1, pX, fq0,  fA,   fRes, f0);
        
        addNewInst(INST_FMA,                    CMPLT_SF1, pX, fe2,  fe,   fe,   f0);
        addNewInst(INST_FMA,                    CMPLT_SF1, pX, fq1,  fq0,  fe,   fq0);
        
        addNewInst(INST_FMA,                    CMPLT_SF1, pX, fe4,  fe2,  fe2,  f0);
        addNewInst(INST_FMA,                    CMPLT_SF1, pX, fq2,  fq1,  fe2,  fq1);
        
        addNewInst(INST_FMA,   CMPLT_PC_DOUBLE, CMPLT_SF1, pX, fq3,  fq2,  fe4,  fq2);
        addNewInst(INST_FNORM, CMPLT_PC_SINGLE, CMPLT_SF0, pX, fRes, fq3);
    } else {
        // Call internal helper to do FP remainder. We only inline the integer  
        // remainder sequence
        //
        Opnd *helperArgs[] = { (Opnd *)src1, (Opnd *)src2 };
    
        uint64  address        = (uint64) remF4;
        Opnd    *helperAddress = opndManager->newImm(address);
        
        directCall(2, helperArgs, dst, helperAddress, p0);
    }
}                                       

//----------------------------------------------------------------------------//
// create new inst and add it in current node
//----------------------------------------------------------------------------//

void IpfInstCodeSelector::addInst(Inst *inst) { 

    IPF_LOG << "        " << IrPrinter::toString(inst) << endl;
    node.addInst(inst);
}

//----------------------------------------------------------------------------//

Inst& IpfInstCodeSelector::addNewInst(InstCode      instCode, 
                                      CG_OpndHandle *op1, 
                                      CG_OpndHandle *op2, 
                                      CG_OpndHandle *op3, 
                                      CG_OpndHandle *op4, 
                                      CG_OpndHandle *op5, 
                                      CG_OpndHandle *op6) {

    Inst* inst = new(mm) Inst(mm, instCode, (Opnd *)op1, (Opnd *)op2, (Opnd *)op3, 
                                        (Opnd *)op4, (Opnd *)op5, (Opnd *)op6);
    addInst(inst);
    return *inst;
}

//----------------------------------------------------------------------------//

Inst& IpfInstCodeSelector::addNewInst(InstCode      instCode,
                                      Completer     comp1, 
                                      CG_OpndHandle *op1, 
                                      CG_OpndHandle *op2, 
                                      CG_OpndHandle *op3, 
                                      CG_OpndHandle *op4, 
                                      CG_OpndHandle *op5, 
                                      CG_OpndHandle *op6) {

    Inst* inst = new(mm) Inst(mm, instCode, comp1, (Opnd *)op1, (Opnd *)op2, (Opnd *)op3, 
                                               (Opnd *)op4, (Opnd *)op5, (Opnd *)op6);
    addInst(inst);
    return *inst;
}

//----------------------------------------------------------------------------//

Inst& IpfInstCodeSelector::addNewInst(InstCode      instCode,
                                      Completer     comp1, 
                                      Completer     comp2, 
                                      CG_OpndHandle *op1, 
                                      CG_OpndHandle *op2, 
                                      CG_OpndHandle *op3, 
                                      CG_OpndHandle *op4, 
                                      CG_OpndHandle *op5, 
                                      CG_OpndHandle *op6) {

    Inst* inst = new(mm) Inst(mm, instCode, comp1, comp2, (Opnd *)op1, (Opnd *)op2, (Opnd *)op3, 
                                                      (Opnd *)op4, (Opnd *)op5, (Opnd *)op6);
    addInst(inst);
    return *inst;
}

//----------------------------------------------------------------------------//

Inst& IpfInstCodeSelector::addNewInst(InstCode      instCode,
                                      Completer     comp1, 
                                      Completer     comp2, 
                                      Completer     comp3, 
                                      CG_OpndHandle *op1, 
                                      CG_OpndHandle *op2, 
                                      CG_OpndHandle *op3, 
                                      CG_OpndHandle *op4, 
                                      CG_OpndHandle *op5, 
                                      CG_OpndHandle *op6) {

    Inst* inst = new(mm) Inst(mm, instCode, comp1, comp2, comp3, (Opnd *)op1, (Opnd *)op2, (Opnd *)op3, 
                                                      (Opnd *)op4, (Opnd *)op5, (Opnd *)op6);
    addInst(inst);
    return *inst;
}

//----------------------------------------------------------------------------//
// convertors from HLO to CG types
//----------------------------------------------------------------------------//

DataKind IpfInstCodeSelector::toDataKind(Type::Tag tag) {

    switch(tag) {
        case Type::Boolean                : return DATA_U8;
        case Type::Char                   : return DATA_U16;
        case Type::Int8                   : return DATA_I8;
        case Type::Int16                  : return DATA_I16;
        case Type::Int32                  : return DATA_I32;
        case Type::Int64                  : return DATA_I64;
        case Type::UInt8                  : return DATA_U8;
        case Type::UInt16                 : return DATA_U16;
        case Type::UInt32                 : return DATA_U32;
        case Type::UInt64                 : return DATA_U64;
        case Type::IntPtr                 : return DATA_U64;
        case Type::VTablePtr              : return DATA_U64;
        case Type::Single                 : return DATA_S;
        case Type::Double                 : return DATA_D;
        case Type::Float                  : return DATA_F;
        case Type::Array                  : return DATA_BASE;
        case Type::Object                 : return DATA_BASE;
        case Type::NullObject             : return DATA_BASE;
        case Type::SystemClass            : return DATA_BASE;
        case Type::SystemObject           : return DATA_BASE;
        case Type::SystemString           : return DATA_BASE;
        case Type::ManagedPtr             : return DATA_MPTR;
        case Type::Tau                    : return DATA_INVALID;
        case Type::CompressedSystemClass  :
        case Type::CompressedSystemString :
        case Type::CompressedSystemObject : 
        case Type::CompressedObject       :
        case Type::CompressedArray        : return DATA_U32;
        default : IPF_ERR << "unexpected tag " << Type::tag2str(tag) << endl;
    }
    return DATA_INVALID;
}

//----------------------------------------------------------------------------//

OpndKind IpfInstCodeSelector::toOpndKind(Type::Tag tag) {

    switch(tag) {
        case Type::Array        : return OPND_G_REG;
        case Type::Object       : return OPND_G_REG;
        case Type::NullObject   : return OPND_G_REG;
        case Type::SystemObject : return OPND_G_REG;
        case Type::SystemClass  : return OPND_G_REG;
        case Type::Boolean      : return OPND_G_REG;
        case Type::Char         : return OPND_G_REG;
        case Type::Int8         : return OPND_G_REG;
        case Type::Int16        : return OPND_G_REG;
        case Type::Int32        : return OPND_G_REG;
        case Type::Int64        : return OPND_G_REG;
        case Type::UInt8        : return OPND_G_REG;
        case Type::UInt16       : return OPND_G_REG;
        case Type::UInt32       : return OPND_G_REG;
        case Type::UInt64       : return OPND_G_REG;
        case Type::Single       : return OPND_F_REG;
        case Type::Double       : return OPND_F_REG;
        case Type::Float        : return OPND_F_REG;
        case Type::IntPtr       : return OPND_G_REG;
        case Type::ManagedPtr   : return OPND_G_REG;
        case Type::VTablePtr    : return OPND_G_REG;
        case Type::SystemString : return OPND_G_REG;
        case Type::Tau          : return OPND_INVALID;
        default                 : IPF_ERR << "unexpected tag " << Type::tag2str(tag) << endl;
    }
    return OPND_INVALID;
}

//----------------------------------------------------------------------------//

DataKind IpfInstCodeSelector::toDataKind(IntegerOp::Types type) {

    switch(type) {
        case IntegerOp::I4 : return DATA_I32;
        case IntegerOp::I8 : return DATA_I64;
        case IntegerOp::I  : return DATA_U64;
        default            : IPF_ERR << "unexpected type " << type << endl; 
    }
    return DATA_INVALID;
}

//----------------------------------------------------------------------------//

DataKind IpfInstCodeSelector::toDataKind(NegOp::Types type) {

    switch (type) {
        case NegOp::F  : return DATA_F;
        case NegOp::D  : return DATA_D;
        case NegOp::S  : return DATA_S;
        case NegOp::I4 : return DATA_I32;
        case NegOp::I  :
        case NegOp::I8 : return DATA_I64;
        default        : IPF_ERR << "unexpected type " << type << endl; 
    }
    return DATA_INVALID;
}

//----------------------------------------------------------------------------//

OpndKind IpfInstCodeSelector::toOpndKind(NegOp::Types type) {

    switch (type) {
        case NegOp::F  : 
        case NegOp::D  : 
        case NegOp::S  : return OPND_F_REG;
        case NegOp::I4 : 
        case NegOp::I  :
        case NegOp::I8 : return OPND_G_REG;
        default        : IPF_ERR << "unexpected type " << type << endl; 
    }
    return OPND_INVALID;
}

//----------------------------------------------------------------------------//

DataKind IpfInstCodeSelector::toDataKind(ArithmeticOp::Types type) {

    switch (type) {
        case ArithmeticOp::F  : return DATA_F;  
        case ArithmeticOp::D  : return DATA_D;
        case ArithmeticOp::S  : return DATA_S;
        case ArithmeticOp::I4 : return DATA_I32;
        case ArithmeticOp::I8 : return DATA_I64;
        case ArithmeticOp::I  : return DATA_I64;
        default               : IPF_ERR << "unexpected type " << type << endl; 
    }
    return DATA_INVALID;
}

//----------------------------------------------------------------------------//

OpndKind IpfInstCodeSelector::toOpndKind(ArithmeticOp::Types type) {

    switch(type) {
        case ArithmeticOp::F  :  
        case ArithmeticOp::D  :  
        case ArithmeticOp::S  : return OPND_F_REG;
        case ArithmeticOp::I4 : 
        case ArithmeticOp::I8 : 
        case ArithmeticOp::I  : return OPND_G_REG;
        default               : IPF_ERR << "unexpected type " << type << endl; 
    }
    return OPND_INVALID;
}

//----------------------------------------------------------------------------//

DataKind IpfInstCodeSelector::toDataKind(RefArithmeticOp::Types type) {
    
    switch(type) {
        case RefArithmeticOp::I4 : return DATA_I32;
        case RefArithmeticOp::I  : return DATA_I64;
        default                  : IPF_ERR << "unexpected type " << type << endl;
    }
    return DATA_INVALID;
}

//----------------------------------------------------------------------------//

DataKind IpfInstCodeSelector::toDataKind(ConvertToIntOp::Types type, bool isSigned) {
    
    switch(type) {
        case ConvertToIntOp::I1 : return isSigned ? DATA_I8  : DATA_U8;
        case ConvertToIntOp::I2 : return isSigned ? DATA_I16 : DATA_U16;
        case ConvertToIntOp::I  :
        case ConvertToIntOp::I4 : return isSigned ? DATA_I32 : DATA_U32;
        case ConvertToIntOp::I8 : return isSigned ? DATA_I64 : DATA_U64;
        default                 : IPF_ERR << "unexpected type " << type << endl;
    }
    return DATA_INVALID;
}

//----------------------------------------------------------------------------//

OpndKind IpfInstCodeSelector::toOpndKind(DivOp::Types type) {

    switch(type) {
        case DivOp::I4 :
        case DivOp::U4 :
        case DivOp::I  :
        case DivOp::I8 :
        case DivOp::U  :
        case DivOp::U8 : return OPND_G_REG;
        case DivOp::F  :
        case DivOp::S  :
        case DivOp::D  : return OPND_F_REG;
        default        : IPF_ERR << "unexpected type " << type << endl;
    }
    return OPND_INVALID;
}

//----------------------------------------------------------------------------//

DataKind IpfInstCodeSelector::toDataKind(DivOp::Types type) {

    switch(type) {
        case DivOp::I4 : return DATA_I32;
        case DivOp::U4 : return DATA_U32;
        case DivOp::I  :
        case DivOp::I8 : return DATA_I64;
        case DivOp::U  :
        case DivOp::U8 : return DATA_U64;
        case DivOp::F  : return DATA_F;
        case DivOp::S  : return DATA_S;
        case DivOp::D  : return DATA_D;
        default        : IPF_ERR << "unexpected type " << type << endl;
    }
    return DATA_INVALID;
}

//----------------------------------------------------------------------------//

InstCode IpfInstCodeSelector::toInstCmp(CompareOp::Types type) {
    
    switch(type) {
        case CompareOp::I4      : return INST_CMP4;
        case CompareOp::I8      : 
        case CompareOp::I       : 
        case CompareOp::Ref     : 
        case CompareOp::CompRef : return INST_CMP;
        case CompareOp::F       : 
        case CompareOp::S       : 
        case CompareOp::D       : return INST_FCMP;
        default                 : IPF_ERR << "unexpected type " << type << endl;
    }
    return INST_INVALID;
}

//----------------------------------------------------------------------------//

InstCode IpfInstCodeSelector::toInstCmp(CompareZeroOp::Types type) {
    
    switch(type) {
        case CompareZeroOp::I4      : return INST_CMP4;
        case CompareZeroOp::I8      : 
        case CompareZeroOp::I       : 
        case CompareZeroOp::Ref     : return INST_CMP;
        case CompareZeroOp::CompRef : return INST_CMP4;
        default                     : IPF_ERR << "unexpected type " << type << endl;
    }
    return INST_INVALID;
}

//----------------------------------------------------------------------------//

Completer IpfInstCodeSelector::toCmpltCrel(CompareOp::Operators cmpOp, bool isFloating) {

    if (isFloating) {
        switch (cmpOp) {
        case CompareOp::Eq  : return CMPLT_FCMP_FREL_EQ;
        case CompareOp::Ne  : return CMPLT_FCMP_FREL_NEQ;
        case CompareOp::Gt  : return CMPLT_FCMP_FREL_GT;
        case CompareOp::Gtu : return CMPLT_FCMP_FREL_NLE;
        case CompareOp::Ge  : return CMPLT_FCMP_FREL_GE;
        case CompareOp::Geu : return CMPLT_FCMP_FREL_NLT;
        default             : IPF_ERR << "unexpected type " << cmpOp << endl; 
        }
    } else {
        switch (cmpOp) {
        case CompareOp::Eq  : return CMPLT_CMP_CREL_EQ;
        case CompareOp::Ne  : return CMPLT_CMP_CREL_NE;
        case CompareOp::Gt  : return CMPLT_CMP_CREL_GT;
        case CompareOp::Gtu : return CMPLT_CMP_CREL_GTU;
        case CompareOp::Ge  : return CMPLT_CMP_CREL_GE;
        case CompareOp::Geu : return CMPLT_CMP_CREL_GEU;
        default             : IPF_ERR << "unexpected type " << cmpOp << endl;
        }
    }

    return CMPLT_INVALID;
}

//----------------------------------------------------------------------------//

Completer IpfInstCodeSelector::toCmpltSz(DataKind dataKind) {

    switch(dataKind) {
        case DATA_I8   :
        case DATA_U8   : return CMPLT_SZ_1;
        case DATA_I16  :
        case DATA_U16  : return CMPLT_SZ_2;
        case DATA_I32  :
        case DATA_U32  : return CMPLT_SZ_4;
        case DATA_I64  :
        case DATA_U64  :
        case DATA_BASE :
        case DATA_MPTR : return CMPLT_SZ_8;
        case DATA_S    : return CMPLT_FSZ_S;
        case DATA_D    : return CMPLT_FSZ_D;
        case DATA_F    : return CMPLT_FSZ_E;
        default        : IPF_ERR << "unexpected dataKind " << dataKind << endl; 
    }
    return CMPLT_INVALID;
}

} //namespace IPF
} //namespace Jitrino 
