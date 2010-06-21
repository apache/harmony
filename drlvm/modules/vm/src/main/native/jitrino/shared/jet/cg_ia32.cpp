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
#include "open/vm_method_access.h"
#include "compiler.h"
#include "trace.h"

#ifdef WIN32
#include <malloc.h>
#endif
#include <memory.h>
#include <assert.h>
#include <stdlib.h>

#include "enc_ia32.h"

/**
 * @file
 * @brief CodeGen routines implementations specific for IA-32 and Intel 64.
 */

namespace Jitrino {
namespace Jet {

/**
 * Provides fine-tuned implementation for IDIV/IREM operations on IA32-compatible platforms, 
 * in replacement of common arithmetic helper (see arith_rt.h).
 */
bool CodeGen::gen_a_platf(JavaByteCodes op, jtype jt)
{
    if (jt != i32) return false;
    if (op != OPCODE_IDIV && op != OPCODE_IREM) {
        return false;
    }
    //
    // The method is supposed to be platform-depended, and may not have 
    // Encoder support - leaving as-is, without implementing general 
    // support in Encoder
    //
    
    vpark(eax.reg());
    vpark(edx.reg());
    rlock(eax);
    rlock(edx);
    Val& v1 = vstack(1, vis_imm(1));
    Val& v2 = vstack(0, true);
    alu(alu_cmp, v2.as_opnd(), Opnd(-1));
    unsigned br_normal = br(ne, 0, 0);
    alu(alu_cmp, v1.as_opnd(), Opnd(INT_MIN));
    unsigned br_exit = NOTHING;
    if (op == OPCODE_IREM) {
        do_mov(edx, Opnd(0)); // prepare exit value for the corner case
        br_exit = br(eq, 0, 0);
    }
    else {
        do_mov(eax, v1);
        br_exit = br(eq, 0, 0);
    }
    patch(br_normal, ip());
    do_mov(eax, v1);
    //
    // The method is supposed to be platform-depended, and may not have 
    // Encoder support - leaving as-is, without implementing general 
    // support in Encoder
    //
    
    //CDQ
    EncoderBase::Operands args0(RegName_EDX, RegName_EAX);
    ip(EncoderBase::encode(ip(), Mnemonic_CDQ, args0));
    //IDIV
    EncoderBase::Operands args(RegName_EDX, RegName_EAX, 
                               devirt(v2.reg(), i32));
    ip(EncoderBase::encode(ip(), Mnemonic_IDIV, args));
    patch(br_exit, ip());

    vpop();
    vpop();
    vpush(op == OPCODE_IREM ? edx : eax);
    runlock(eax);
    runlock(edx);
    return true;
}

}}; // ~namespace Jitrino::Jet
