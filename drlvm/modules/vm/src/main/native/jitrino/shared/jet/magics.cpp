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
 * @file
 * @brief Magics and WBs support for MMTk.
*/
 
#include "compiler.h"
#include "enc_defs.h"
#include "enc.h"

#include "open/vm_class_info.h"
#include "open/vm.h"
#include "jit_intf.h"
#include "VMMagic.h"

#include <vector>
using std::vector;

namespace Jitrino {
namespace Jet {


static size_t sizeof_jt(jtype jt) {
    static size_t sizes[] =  { 
        1, //i8,
        2, //i16, 
        2, //u16,
        4, //i32,
        8, //i64,
        4, //flt32,
        8, //dbl64,
        sizeof(POINTER_SIZE_INT), //jobj,
        sizeof(POINTER_SIZE_INT), //jvoid,
        sizeof(POINTER_SIZE_INT), //jretAddr,
        0, //jtypes_count, 
    };
    size_t res= sizes[jt];
    assert(res >= 1 && res<=8 && (res%2==0 || res == 1));
    return res;
}

/** creates new opnd with the specified type and generates move from old one to new one */
static void vstack_replace_top_opnd(Compiler* c, jtype jt) {
    Opnd src = c->vstack(0).as_opnd();
    Opnd dst(jt, c->valloc(jt));

    size_t dstSize = sizeof_jt(dst.jt());
    size_t srcSize = sizeof_jt(src.jt());
    assert((srcSize==4 || srcSize ==  8) && (dstSize == 4 || dstSize == 8));
    if (srcSize == dstSize) { //simple mov
        c->mov(dst, src);
        c->vpop();  //pop src
        c->vpush(dst);
    } else if (srcSize > dstSize) { //truncation
        c->do_mov(dst, src);
        c->vpop(); //lo&hi parts are popped with a single pop operation for i64 type
        c->vpush(dst); //push result
    } else { // expansion
        assert(srcSize<dstSize);
        c->do_mov(dst, src, true); //fill lower part -> dst
        Opnd hi(jt, c->g_iconst_0); //fill hi part with 0
        c->vpop(); //pop old value
        c->vpush2(dst, hi); //push new value
    }
}

bool Compiler::gen_magic(void)
{
    const JInst& jinst = m_insts[m_pc];
    JavaByteCodes opkod = jinst.opcode;

    if (opkod != OPCODE_INVOKEVIRTUAL && 
        opkod != OPCODE_INVOKESTATIC &&
        opkod != OPCODE_INVOKESPECIAL)
    {
        return false;
    }
    
    const char* kname = class_cp_get_entry_class_name(m_klass, (unsigned short)jinst.op0);

    if (!VMMagicUtils::isVMMagicClass(kname)) {
        return false;
    }
    // This is a magic -> transform it

    const char* mname = class_cp_get_entry_name(m_klass, (unsigned short)jinst.op0);

    jtype magicType = iplatf;

    if (!strcmp(mname, "fromLong")) {
        vstack_replace_top_opnd(this, magicType);
        return true;
    }

    if (!strcmp(mname, "toLong")) {
        vstack_replace_top_opnd(this, i64);
        return true;
    }
    return false;


/**     Old IA32 implementation. This implementation is not GC safe, 
        it keeps all magic classes as objects and magics becomes a part of GC enumeration
        This code must be refactored or removed


    vector<jtype> args;
    jtype retType;
    bool is_static = opkod == OPCODE_INVOKESTATIC;
    get_args_info(is_static, jinst.op0, args, &retType);

    // ADD, SUB, DIFF, etc - 2 args arithmetics
    ALU oper = alu_count;
    
    if (!strcmp(mname, "add"))          { oper = alu_add; }
    else if (!strcmp(mname, "plus"))    { oper = alu_add; }
    else if (!strcmp(mname, "sub"))     { oper = alu_sub; }
    else if (!strcmp(mname, "minus"))     { oper = alu_sub; }
    else if (!strcmp(mname, "diff"))    { oper = alu_sub; }
    else if (!strcmp(mname, "or"))      { oper = alu_or; }
    else if (!strcmp(mname, "xor"))     { oper = alu_xor; }
    else if (!strcmp(mname, "and"))     { oper = alu_and; }
    if (oper != alu_count) {
        Val& v0 = vstack(0, true);
        Val& v1 = vstack(1, true);
        Opnd newObj(jobj, valloc(jobj));
        mov(newObj, v1.as_opnd());
        alu(oper, newObj, v0.as_opnd());
        vpop();
        vpop();
        vpush(newObj);
        return true;
    }
    
    JavaByteCodes shiftOp = OPCODE_NOP;
    if (!strcmp(mname, "lsh"))        {shiftOp = OPCODE_ISHL;}
    else if (!strcmp(mname, "rsha"))  {shiftOp = OPCODE_ISHR;}
    else if (!strcmp(mname, "rshl"))  {shiftOp = OPCODE_IUSHR;}
    
    if (shiftOp != OPCODE_NOP) {
        Opnd shiftAmount = vstack(0, false).as_opnd(i32);
        shiftAmount = vstack(0, true).as_opnd(i32);
        rlock(shiftAmount.reg());
        vpop();
        //changing type of obj opnd type to ia32
        vstack_replace_top_opnd(this, i32);
        vpush(shiftAmount);
        runlock(shiftAmount.reg());
        //processing as java bytecode and converting back to the obj type
        gen_a(shiftOp, i32);
        vstack_replace_top_opnd(this, jobj);
        return true;
    }
    
    if (!strcmp(mname, "not")) {
        Opnd v1 = vstack(0, true).as_opnd(jobj);
        rlock(v1.reg());
        Opnd v2(jobj, valloc(jobj));
        mov(v2, v1);
        bitwise_not(v2);
        runlock(v1.reg());
        vpop();
        vpush(v2);
        return true;
    }
    
    //
    // EQ, GE, GT, LE, LT, sXX - 2 args compare
    //
    COND cm = cond_none;
    if (!strcmp(mname, "EQ"))       { cm = eq; }
    if (!strcmp(mname, "equals"))   { cm = eq; }
    else if (!strcmp(mname, "NE"))  { cm = ne; }
    // unsigned compare
    else if (!strcmp(mname, "GE"))  { cm = ae; }
    else if (!strcmp(mname, "GT"))  { cm = above; }
    else if (!strcmp(mname, "LE"))  { cm = be;}
    else if (!strcmp(mname, "LT"))  { cm = below; }
    // signed compare
    else if (!strcmp(mname, "sGE"))  { cm = ge; }
    else if (!strcmp(mname, "sGT"))  { cm = gt; }
    else if (!strcmp(mname, "sLE"))  { cm = le;}
    else if (!strcmp(mname, "sLT"))  { cm = lt; }
    //     
    if (cm != cond_none) {
        Opnd o1 = vstack(1, true).as_opnd(i32); 
        Opnd o2 = vstack(0, true).as_opnd(i32); 
        alu(alu_cmp, o1, o2);
        vpop();
        vpop();
        Opnd boolResult(i32, valloc(i32));
        rlock(boolResult.reg());
        mov(boolResult, g_iconst_0);
        cmovcc(cm, boolResult, vaddr(i32, &g_iconst_1));
        runlock(boolResult.reg());
        vpush(boolResult);
        return true;
    } 

    //
    // is<Smth> one arg testing
    //
    bool oneArgCmp = false;
    int theConst = 0;
    if (!strcmp(mname, "isZero")) { oneArgCmp = true; theConst = 0; }
    else if (!strcmp(mname, "isMax")) { oneArgCmp = true; theConst = ~0; }
    else if (!strcmp(mname, "isNull")) { oneArgCmp = true; theConst = 0; }
    if (oneArgCmp) {
        AR regVal = vstack(0, true).reg();
        rlock(regVal);
        alu(alu_cmp, Opnd(jobj, regVal), theConst);
        
        //save the result
        AR resultReg = valloc(i32);
        rlock(resultReg);
        mov(resultReg, Opnd(g_iconst_0)); 
        cmovcc(z, resultReg, vaddr(i32, &g_iconst_1));
        runlock(resultReg);
        vpop();
        vpush(Opnd(i32, resultReg));

        runlock(regVal);
        return true;
    }
    
    //
    // fromXXX - static creation from something
    //
    if (strcmp(mname, "fromLong")) {
        assert(0);
    } 
    else if (!strcmp(mname, "fromIntSignExtend")) {
        vstack_replace_top_opnd(this, jobj);
        return true;
    }
    else if (!strcmp(mname, "fromIntZeroExtend")) {
        vstack_replace_top_opnd(this, jobj);
        return true;
    }
    else if (!strcmp(mname, "fromObject") || 
             !strcmp(mname, "toAddress") ||
             !strcmp(mname, "toObjectReference")) 
    {
        vstack_replace_top_opnd(this, jobj);
        return true;
    }

    const char* msig = method_get_descriptor(meth);
    //
    // load<type> things
    //
    jtype jt = jvoid;
    bool load = true;
    bool has_offset = false;

    if (!strcmp(mname, "loadObjectReference"))  { jt = jobj; }
    else if (!strcmp(mname, "loadAddress"))     { jt = jobj; }
    else if (!strcmp(mname, "loadWord"))        { jt = jobj; }
    else if (!strcmp(mname, "loadByte"))        { jt = i8; }
    else if (!strcmp(mname, "loadChar"))        { jt = u16; }
    else if (!strcmp(mname, "loadDouble"))      { jt = dbl64; }
    else if (!strcmp(mname, "loadFloat"))       { jt = flt32; }
    else if (!strcmp(mname, "loadInt"))         { jt = i32; }
    else if (!strcmp(mname, "loadLong"))        { jt = i64; }
    else if (!strcmp(mname, "loadShort"))       { jt = i16; }
    else if (!strcmp(mname, "prepareWord"))              { jt = i32; }
    else if (!strcmp(mname, "prepareObjectReference"))   { jt = jobj;}
    else if (!strcmp(mname, "prepareAddress"))           { jt = jobj;}
    else if (!strcmp(mname, "prepareInt"))               { jt = i32; }
    else if (!strcmp(mname, "store")) {
        load = false;
        // store() must have at least one arg
        assert(strlen(msig) > strlen("()V"));
        char ch = msig[1]; // first symbol after '('.
        VM_Data_Type vdt = (VM_Data_Type)ch;
        switch(vdt) {
        case VM_DATA_TYPE_BOOLEAN:  // i8
        case VM_DATA_TYPE_INT8:     jt = i8; break;
        case VM_DATA_TYPE_INT16:    jt = i16; break;
        case VM_DATA_TYPE_CHAR:     jt = u16; break;
        case VM_DATA_TYPE_INT32:    jt = i32; break;
        case VM_DATA_TYPE_INT64:    jt = i64; break;
        case VM_DATA_TYPE_F4:       jt = flt32; break;
        case VM_DATA_TYPE_F8:       jt = dbl64; break;
        case VM_DATA_TYPE_ARRAY:    // jobj
        case VM_DATA_TYPE_CLASS:    jt = jobj; break;
        default: assert(false);
        }
        jtype retType;
        vector<jtype> args;
        get_args_info(meth, args, &retType);
        assert(args.size()>=2);
        has_offset = args.size() > 2;
    }
    if (jt != jvoid) {
        size_t jt_size = sizeof_jt(jt);
        if (load) {
            // if loadXX() has any arg, then it's offset
            if(strncmp(msig, "()", 2)) {
                has_offset = true;
            }
        }
        unsigned addr_depth = has_offset ? 1 : 0;
        if (!load) {
            ++addr_depth;
            if (is_wide(jt)) {
                ++addr_depth;
            }
        }
        AR addrReg = vstack(addr_depth, true).reg();
        rlock(addrReg);
        
        if (has_offset) {
            //Add offset. Save to the new location.
            AR addrWithOffsetReg = valloc(jobj);
            mov(addrWithOffsetReg, addrReg);
            runlock(addrReg);
            addrReg = addrWithOffsetReg;
            rlock(addrReg);

            AR offsetReg = vstack(0, true).reg();
            vpop();
            alu(alu_add, addrReg, offsetReg);
        }

        if (load) {
            vpop();
            if (!is_big(jt)) {
                AR resReg = valloc(jt);
                ld(jt, resReg, addrReg);
                Opnd resOpnd(jt, resReg);
                if (jt_size < 4) {
                    Opnd extendedOpnd(i32, valloc(i32));
                    if (jt == u16) {
                        zx2(extendedOpnd, resOpnd);
                    } else {
                        sx(extendedOpnd, resOpnd);
                    }
                    resOpnd = extendedOpnd;
                } 
                vpush(resOpnd);

            } else { //code is taken from array element load -> TODO: avoid duplication
                AR ar_lo = valloc(jt);
                Opnd lo(jt, ar_lo);
                rlock(lo);

                do_mov(lo, Opnd(i32, addrReg, 0));
                
                AR ar_hi = valloc(jt);
                Opnd hi(jt, ar_hi);
                rlock(hi);
                Opnd mem_hi(jt, Opnd(i32, addrReg, 4));
                do_mov(hi, mem_hi);
                vpush2(lo, hi);
                runlock(lo);
                runlock(hi);
            }
        } else {
            Opnd v0 = vstack(0, true).as_opnd(jt);
            if (!is_big(jt)) {
                mov(Opnd(jt, addrReg, 0), v0);
            } else {
                do_mov(Opnd(i32, addrReg, 0), v0);
                Opnd v1 = vstack(1, true).as_opnd(jt);
                do_mov(Opnd(i32, addrReg, 4), v1);
            }
            vpop();   // pop out value
            vpop(); // pop out Address
        }
        runlock(addrReg);
        return true;
    }
    //
    // max, one, zero
    //
    bool loadConst = false;
    if (!strcmp(mname, "max"))          { loadConst = true; theConst = -1;}
    else if (!strcmp(mname, "one"))     { loadConst = true; theConst =  1;}
    else if (!strcmp(mname, "zero"))    { loadConst = true; theConst =  0;}
    else if (!strcmp(mname, "nullReference"))
                                        { loadConst = true; theConst =  0;}
    if (loadConst) {
        Opnd regOpnd(jobj, valloc(jobj));
        mov(regOpnd, theConst);
        vpush(regOpnd);
        return true;
    }
    //
    // toInt, toLong, toObjectRef, toWord(), etc.
    //
    jt = jvoid;
    if (!strcmp(mname, "toInt"))            { jt = i32;  }
    else if (!strcmp(mname, "toLong"))      { jt = i64;  }
    else if (!strcmp(mname, "toObjectRef")) { jt = jobj; }
    else if (!strcmp(mname, "toWord"))      { jt = jobj; }
    else if (!strcmp(mname, "toAddress"))   { jt = jobj; }
    else if (!strcmp(mname, "toObject"))    { jt = jobj; }
    else if (!strcmp(mname, "toExtent"))    { jt = jobj; }
    else if (!strcmp(mname, "toOffset"))    { jt = jobj; }
    if (jt != jvoid) {
        if (jt!=i64) {
            vstack_replace_top_opnd(this, jt);
            return true;
        }
        vstack_replace_top_opnd(this, i32);
    
        Opnd srcOpnd = vstack(0, true).as_opnd(i32);
        Opnd lo(jt, valloc(jt));
        do_mov(lo, srcOpnd);

        Opnd hi(jt, g_iconst_0);
        vpop();
        vpush2(lo, hi);
        return true;
    }

    if (!strcmp(mname, "attempt")) {
        AR addrReg;
        if (args.size() == 4) { //attempt with Offset
            AR newAddressReg = valloc(jobj);
            rlock(newAddressReg);
            addrReg= vstack(3, true).reg();
            mov(newAddressReg, addrReg);
            AR offsetReg= vstack(0, true).reg();
            alu(alu_add, newAddressReg, offsetReg);
            runlock(newAddressReg);
            addrReg = newAddressReg;
            vpop();
        } else {
            addrReg = vstack(2, true).reg();
        }
        rlock(addrReg);
        AR newReg = vstack(0, true).reg();
        rlock(newReg);
        AR oldReg = vstack(1, true).reg();

        cmpxchg(true, addrReg, newReg, oldReg);

        runlock(addrReg);
        runlock(newReg);

        //save the result
        AR resultReg = valloc(i32);
        rlock(resultReg);
        mov(resultReg, Opnd(g_iconst_0)); 
        cmovcc(z, resultReg, vaddr(i32, &g_iconst_1));
        runlock(resultReg);

        //fixing the stack and saving the result.
        vpop();
        vpop();
        vpop();
        vpush(Opnd(i32, resultReg));
        
        return true;
    }

    //
    // xArray stuff
    //
    if (!strcmp(mname, "create")) {
        VM_Data_Type atype = VM_DATA_TYPE_INT32;
        Class_Handle elem_class = class_get_class_of_primitive_type(atype);
        assert(elem_class != NULL);
        Class_Handle array_class = class_get_array_of_class(elem_class);
        assert(array_class != NULL);
        Allocation_Handle ah = class_get_allocation_handle(array_class);
        gen_new_array(ah);
        return true;
    }

    if (!strcmp(mname, "get")) {
        gen_arr_load(jobj);
        return true;
    }
    
    if (!strcmp(mname, "set")) {
        gen_arr_store(jobj, false);
        return true;
    }
    if (!strcmp(mname, "length")) {
        gen_array_length();
        return true;
    }
    //assert(false);
    return false;

*/
}

}};             // ~namespace Jitrino::Jet
