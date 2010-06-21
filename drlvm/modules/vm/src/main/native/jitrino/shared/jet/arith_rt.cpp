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
 * @author Alexander V. Astapchuk
 */
 /**
  * @file
  * @brief Implementation of arithmetic helpers declared in arith_rt.h.
  */
 
#include "arith_rt.h"
#include "Algorithms.h"

#include <stdlib.h>
#include <float.h>
#include <math.h>
#include <assert.h>

#ifndef PLATFORM_POSIX
    #define isnan   _isnan
    #define finite  _finite
#endif
#include "trace.h"


#undef isnan
#define isnan(x)    ((x) != (x))


namespace Jitrino {
namespace Jet {

double __stdcall rt_h_neg_dbl64(double v)
{
    return -v;
}

float __stdcall rt_h_neg_flt32(float v)
{
    return -v;
}

jlong __stdcall rt_h_neg_i64(jlong v)
{
    return -v;
}

int __stdcall rt_h_neg_i32(int v)
{
    return -v;
}

int __stdcall rt_h_lcmp(jlong v1, jlong v2)
{
    if (v1 > v2)   return 1;
    if (v1 < v2)   return -1;
    return 0;
}

int __stdcall rt_h_fcmp_g(float v1, float v2)
{
    if (isnan(v1) || isnan(v2)) {
        return 1;
    }
    if (v1 > v2)   return 1;
    if (v1 < v2)   return -1;
    return 0;
}

int __stdcall rt_h_fcmp_l(float v1, float v2)
{
    if (isnan(v1) || isnan(v2)) {
        return -1;
    }
    return rt_h_fcmp_g(v1,v2);
}

int __stdcall rt_h_dcmp_g(double v1, double v2)
{
    if (isnan(v1) || isnan(v2)) {
        return 1;
    }
    if (v1 > v2) {
        return 1;
    }
    if (v1 < v2) {
        return -1;
    }
    return 0;
}

int __stdcall rt_h_dcmp_l(double v1, double v2)
{
    if (isnan(v1) || isnan(v2)) { return -1; }
    return rt_h_dcmp_g(v1,v2);
}

double __stdcall rt_h_dbl_a(double v1, double v2, JavaByteCodes op)
{
    //rt_dbg_out("d%s: %f %f", instrs[op].name, v1, v2);
    switch(op) {
    case OPCODE_IADD:
        return v1 + v2;
    case OPCODE_ISUB:
        return v1 - v2;
    case OPCODE_IMUL:
        return v1 * v2;
    case OPCODE_IDIV:
        return v1 / v2;
    default:
        assert(op == OPCODE_IREM);
        break;
    }

    assert(op == OPCODE_IREM);
    // return NaN if either of args is NaN
    if (isnan(v1)) {
        return v1;
    }
    if (isnan(v2)) {
        return v2;
    }
    // if v2 is infinity, then result is v1
    if (finite(v1) && !finite(v2)) {
        return v1;
    }
    return jitrino_ieee754_fmod_double(v1,v2);
}

float __stdcall rt_h_flt_a(float v1, float v2, JavaByteCodes op)
{
    //dbg_rt("f%s: %f %f", instrs[op].name, v1, v2);
    switch( op) {
    case OPCODE_IADD:
        return v1 + v2;
    case OPCODE_ISUB:
        return v1 - v2;
    case OPCODE_IMUL:
        return v1 * v2;
    case OPCODE_IDIV:
        return v1 / v2;
    default:
        assert(op == OPCODE_IREM); break;
    }
    
    assert(op == OPCODE_IREM);
    
    // return NaN if either of args is NaN
    if (isnan(v1)) {
        return v1;
    }
    if (isnan(v2)) {
        return v2;
    }
    // if v2 is infinity, then result is v1
    if (finite(v1) && !finite(v2)) {
        return v1;
    }
    return (float)fmod((double)v1,(double)v2);
}

jlong __stdcall rt_h_i64_shift(jlong v1, int v2, JavaByteCodes op)
{
    switch(op) {
    case OPCODE_ISHL:
        return v1 << (v2&0x3F);
    case OPCODE_ISHR:
        return v1 >> (v2&0x3F);
        //  case OPCODE_IUSHR:  return ( v1 >= 0 ) ? v1 >> (v2&0x3F) : (v1 >> (v2&0x3F)) + (2L << ~(v2&0x3F));
    case OPCODE_IUSHR:
        return ((uint64)v1) >> (v2&0x3F);
    default:
        break;
    }
    assert(false);
    return 0;
}

jlong __stdcall rt_h_i64_a(jlong v1, jlong v2, JavaByteCodes op)
{
    switch(op) {
    case OPCODE_IADD:
        return v1 + v2;
    case OPCODE_ISUB:
        return v1 - v2;
    case OPCODE_IOR:
        return v1 | v2;
    case OPCODE_IXOR:
        return v1 ^ v2;
    case OPCODE_IAND:
        return v1 & v2;
    case OPCODE_IMUL:
        return v1 * v2;
        // special cases according to JVM Spec
    case OPCODE_IDIV:
        return (v2 == -1 && v1 == jLONG_MIN) ? v1 : v1 / v2;
    case OPCODE_IREM:
        return (v2 == -1 && v1 == jLONG_MIN) ? 0 : v1 % v2;
    default:
        break;
    }
    assert(false);
    return 0;
}

int __stdcall rt_h_i32_a(int v1, int v2, JavaByteCodes op)
{
    //dbg_rt("i%s: %u %u", instrs[op].name, v1, v2);
    switch(op) {
    case OPCODE_IADD:
        return v1 + v2;
    case OPCODE_ISUB:
        return v1 - v2;
    case OPCODE_IMUL:
        return v1 * v2;
    case OPCODE_IOR:
        return v1 | v2;
    case OPCODE_IXOR:
        return v1 ^ v2;
    case OPCODE_IAND:
        return v1 & v2;
    case OPCODE_ISHL:
        return v1 << (v2&0x1F);
    case OPCODE_ISHR:
        return v1 >> (v2&0x1F);
    case OPCODE_IUSHR:
        return (v1 >= 0) ? v1 >> (v2&0x1F) :
                          (v1 >> (v2&0x1F)) + (2 << ~(v2&0x1F));
    // special cases according to JVM Spec
    case OPCODE_IDIV:
        // With v2==0 may be called from gen_a(). In this case, the return 
        // value will be ignoread anyway.    
        if (v2 == 0) return -1;
        return (v2 == -1 && v1 == INT_MIN) ? v1 : v1 / v2;
    case OPCODE_IREM:
        // With v2==0 may be called from gen_a(). In this case, the return 
        // value will be ignoread anyway.    
        if (v2 == 0) return -1;
        return (v2 == -1 && v1 == INT_MIN) ? 0 : v1 % v2;
    default:
        break;
    }
    assert(false);
    return 0;
}

// i32 ->
int     __stdcall rt_h_i32_2_i8(int i)      { return (char)i; };
int     __stdcall rt_h_i32_2_i16(int i)     { return (short)i; };
int     __stdcall rt_h_i32_2_u16(int i)     { return (unsigned short)i; };
jlong   __stdcall rt_h_i32_2_i64(int i)     { return (jlong)i; };
float   __stdcall rt_h_i32_2_flt(int i)     { return (float)i; };
double  __stdcall rt_h_i32_2_dbl(int i)     { return (double)i; };

// i64 ->
// Workaround for bug in gcc 4.1.x. It doesn't clear upper half of the long
// register when it returns value from rt_h_i64_2_i32
int     __stdcall rt_h_i64_2_i32(int64 i)   { return (I_32)(i & 0xffffffff); };
float   __stdcall rt_h_i64_2_flt(int64 i)   { return (float)i; };
double  __stdcall rt_h_i64_2_dbl(int64 i)   { return (double)i; };
// flt ->
int     __stdcall rt_h_flt_2_i32(float i)
{
    if (isnan(i)) {
        return 0;
    }
    return i<(double)INT_MIN ? 
                    INT_MIN : (i>=(double)INT_MAX ? INT_MAX : (I_32)i);
}

jlong   __stdcall rt_h_flt_2_i64(float i)
{
    if (isnan(i)) {
        return 0;
    }
    return i<(double)jLONG_MIN ? 
                jLONG_MIN : (i>=(double)jLONG_MAX? jLONG_MAX : (jlong)i);
}

double  __stdcall rt_h_flt_2_dbl(float i)
{
    return (double)i;
}

// dbl ->
int     __stdcall rt_h_dbl_2_i32(double i)
{
    if (isnan(i)) {
        return 0;
    }
    return i<(double)INT_MIN ? 
                    INT_MIN : (i>=(double)INT_MAX ? INT_MAX : (int)i);
}

jlong   __stdcall rt_h_dbl_2_i64(double i)
{
    if (isnan(i))  {
        return 0;
    }
    return i<(double)jLONG_MIN ? 
                jLONG_MIN : (i>=(double)jLONG_MAX? jLONG_MAX : (jlong)i);
}

float   __stdcall rt_h_dbl_2_flt(double i)
{
    return (float)i;
}

const void * cnv_matrix_impls[num_jtypes][num_jtypes] = {
    {NULL,  NULL,   NULL,   NULL,   NULL,   NULL,   NULL },
    {NULL,  NULL,   NULL,   NULL,   NULL,   NULL,   NULL },
    {NULL,  NULL,   NULL,   NULL,   NULL,   NULL,   NULL },
    {(void*)&rt_h_i32_2_i8, (void*)&rt_h_i32_2_i16, (void*)&rt_h_i32_2_u16, NULL, 
        (void*)&rt_h_i32_2_i64, (void*)&rt_h_i32_2_flt, (void*)&rt_h_i32_2_dbl },
    {NULL,  NULL,   NULL,   (void*)&rt_h_i64_2_i32,
        NULL, (void*)&rt_h_i64_2_flt,  (void*)&rt_h_i64_2_dbl },
    {NULL,  NULL,   NULL,   (void*)&rt_h_flt_2_i32,  
        (void*)&rt_h_flt_2_i64,  NULL, (void*)&rt_h_flt_2_dbl },
    {NULL,  NULL,   NULL,   (void*)&rt_h_dbl_2_i32,
        (void*)&rt_h_dbl_2_i64,  (void*)&rt_h_dbl_2_flt,  NULL },
};

}}; // ~namespace Jitrino::Jet


