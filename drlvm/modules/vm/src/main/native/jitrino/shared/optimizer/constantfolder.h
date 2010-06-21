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

#ifndef _CONSTANTFOLDER_H_
#define _CONSTANTFOLDER_H_

#include "Inst.h"

namespace Jitrino {

class Opnd;

class ConstantFolder {
public:
    static bool isConstant(Opnd* opnd);
    static bool isConstantZero(Opnd* opnd);
    static bool isConstantOne(Opnd* opnd);
    static bool isConstantAllOnes(Opnd* opnd);
    static bool isConstant(Inst* inst, I_32& value);
    static bool isConstant(Inst* inst, int64& value);
    static bool isConstant(Inst* inst, ConstInst::ConstValue& value);
    static bool hasConstant(Inst* inst);
    //
    // constant folding methods
    //
    // These methods return true if folding is successful; false otherwise.
    // If folding is successful, the result parameter is set with the folded
    // constant value.
    //
    // binary arithmetic/logical operations
    //
    static bool fold8(Opcode,  I_8 c1,  I_8 c2,  I_32& result, bool is_signed);
    static bool fold16(Opcode, int16 c1, int16 c2, I_32& result, bool is_signed);
    static bool fold32(Opcode, I_32 c1, I_32 c2, I_32& result, bool is_signed);
    static bool fold64(Opcode, int64 c1, int64 c2, int64& result, bool is_signed);
    static bool foldSingle(Opcode, float c1, float c2, float& result);
    static bool foldDouble(Opcode, double c1, double c2, double& result);
    // 
    // unary arithmetic/logical operations
    //
    static bool fold8(Opcode opc,  I_8 c,  I_32& result);
    static bool fold16(Opcode opc, int16 c, I_32& result);
    static bool fold32(Opcode opc, I_32 c, I_32& result);
    static bool fold64(Opcode opc, int64 c, int64& result);
    static bool foldSingle(Opcode, float c1, float& result);
    static bool foldDouble(Opcode, double c1, double& result);
    //
    // binary comparisons
    //
    static bool foldCmp32(ComparisonModifier mod, I_32 c1, I_32 c2, I_32& result);
    static bool foldCmp64(ComparisonModifier mod, int64 c1, int64 c2, I_32& result);
    static bool foldCmpSingle(ComparisonModifier mod, float c1, float c2, I_32& result);
    static bool foldCmpDouble(ComparisonModifier mod, double c1, double c2, I_32& result);
    static bool foldCmpRef(ComparisonModifier, void* c1, void* c2, I_32& result);
    static bool foldCmp(Type::Tag cmpTypeTag,
                        ComparisonModifier mod,
                        ConstInst::ConstValue val1,
                        ConstInst::ConstValue val2,
                        ConstInst::ConstValue& result);
    //
    // unary comparisons
    //
    static bool foldCmp32(ComparisonModifier, I_32 c, I_32& result);
    static bool foldCmp64(ComparisonModifier, int64 c, I_32& result);
    static bool foldCmpRef(ComparisonModifier, void* c, I_32& result);
    //
    // conversions
    //
    static bool foldConv(Type::Tag fromType, Type::Tag toType, Modifier ovfm, 
                         ConstInst::ConstValue src,
                         ConstInst::ConstValue& res);

    // result always written as an I_32 so we overwrite extra bits
    static bool foldCmp(Type::Tag cmpTypeTag,
                        ComparisonModifier mod,
                        ConstInst::ConstValue val,
                        ConstInst::ConstValue& result);

    // result always written as an I_32 or int64 depending on type
    static bool foldConstant(Type::Tag type,
                             Opcode opc,
                             ConstInst::ConstValue val1,
                             ConstInst::ConstValue val2,
                             ConstInst::ConstValue& result,
                             bool is_signed);
    // result always written as an I_32 or int64 depending on type
    static bool foldConstant(Type::Tag type,
                             Opcode opc,
                             ConstInst::ConstValue val,
                             ConstInst::ConstValue& result);

    static bool fold(Inst* inst, ConstInst::ConstValue& result);
};

} //namespace Jitrino 

#endif // _CONSTANTFOLDER_H_
