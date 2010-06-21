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
 * @brief Various arithmetic helpers declaration.
 *
 * The helpers normally used to implement complex computations (i.e. float 
 * point remainder) which do not worth to be inlined. A call to a helper 
 * inserted instead. The set of helpers is wider than single float-point 
 * remainder, because they are also used for quick start while porting to a
 * new platfrom - again, calls to the helpers generated instead of inlining
 * them.
 *
 * The file also includes type conversion utilities.
 *
 * All the helpers from this file follow the contract:
 *  - guaranteed not to invoke GC
 *  - use 'stdcall' calling convention (on platforms where it's applicable)
 */

#if !defined(__ARITH_RT_H_INCLUDED__)
#define __ARITH_RT_H_INCLUDED__


#include "jdefs.h"

namespace Jitrino {
namespace Jet {

/**
 * @ingroup JITRINO_JET_RUNTIME_SUPPORT
 * @defgroup RUNTIME_CNV_HELPERS Conversion helpers
 * 
 * A group of runtime helpers that perform conversions from one type another.
 * 'rt_' stands for 'run time', 'h_' stands for 'helper'.
 * The rest of name stands for 'which_type' to convert from and then 
 * 'which_type' to convert to.
 *
 * @{
 */

//
// i32 ->
//
/** @brief Converts from #i32 to #i8.*/
int     __stdcall rt_h_i32_2_i8(int i)        stdcall__;
/** @brief Converts from #i32 to #i16.*/
int     __stdcall rt_h_i32_2_i16(int i)       stdcall__;
/** @brief Converts from #i32 to #u16.*/
int     __stdcall rt_h_i32_2_u16(int i)       stdcall__;
/** @brief Converts from #i32 to #i64.*/
jlong   __stdcall rt_h_i32_2_i64(int i)       stdcall__;
/** @brief Converts from #i32 to #flt32.*/
float   __stdcall rt_h_i32_2_flt(int i)       stdcall__;
/** @brief Converts from #i32 to #dbl64.*/
double  __stdcall rt_h_i32_2_dbl(int i)       stdcall__;
//
// i64 ->
//
/** @brief Converts from #i64 to #i32.*/
int     __stdcall rt_h_i64_2_i32(int64 i)     stdcall__;
/** @brief Converts from #i64 to #flt32.*/
float   __stdcall rt_h_i64_2_flt(int64 i)     stdcall__;
/** @brief Converts from #i64 to #dbl64.*/
double  __stdcall rt_h_i64_2_dbl(int64 i)     stdcall__;
//
// flt ->
//
/** @brief Converts from #flt32 to #i32.*/
int     __stdcall rt_h_flt_2_i32(float i)     stdcall__;
/** @brief Converts from #flt32 to #i64.*/
jlong   __stdcall rt_h_flt_2_i64(float i)     stdcall__;
/** @brief Converts from #flt32 to #dbl64.*/
double  __stdcall rt_h_flt_2_dbl(float i)     stdcall__;
//
// dbl ->
//
/** @brief Converts from #dbl64 to #i32.*/
int     __stdcall rt_h_dbl_2_i32(double i)    stdcall__;
/** @brief Converts from #dbl64 to #i64.*/
jlong   __stdcall rt_h_dbl_2_i64(double i)    stdcall__;
/** @brief Converts from #dbl64 to #flt32.*/
float   __stdcall rt_h_dbl_2_flt(double i)    stdcall__;

/*@}*/  // ~group of RUNTIME_CNV_HELPERS

/**
 * @brief Bunch of conversion helpers, [#jtype FROM][#jtype TO].
 *
 * A matrix of conversion utilities: an item at cnv_matrix_impls[#jtype FROM]
 * [#jtype TO], contains an address of one of a \link RUNTIME_CNV_HELPERS 
 * the functions\endlink.
 */
extern const void * cnv_matrix_impls[num_jtypes][num_jtypes];

/**
 * @ingroup JITRINO_JET_RUNTIME_SUPPORT
 * @defgroup RUNTIME_ARITHMETIC_HELPERS Arithmetic helpers
 * @{
 */

int    __stdcall rt_h_i32_a(int v2, int v1, JavaByteCodes op)       stdcall__;
jlong  __stdcall rt_h_i64_a(jlong v2, jlong v1, JavaByteCodes op)   stdcall__;
double __stdcall rt_h_dbl_a(double v2, double v1, JavaByteCodes op) stdcall__;
float  __stdcall rt_h_flt_a(float v2, float v1, JavaByteCodes op)   stdcall__;

//
// Some operations implemented via separate functions, as they have a bit 
// different signature.
//
double  __stdcall rt_h_neg_dbl64(double v)  stdcall__;
float   __stdcall rt_h_neg_flt32(float v)   stdcall__;
jlong   __stdcall rt_h_neg_i64(jlong v)     stdcall__;
int     __stdcall rt_h_neg_i32(int v)       stdcall__;

int     __stdcall rt_h_lcmp(jlong v2, jlong v1)     stdcall__;
int     __stdcall rt_h_fcmp_g(float v2, float v1)   stdcall__;
int     __stdcall rt_h_fcmp_l(float v2, float v1)   stdcall__;
int     __stdcall rt_h_dcmp_g(double v2, double v1) stdcall__;
int     __stdcall rt_h_dcmp_l(double v2, double v1) stdcall__;

jlong   __stdcall rt_h_i64_shift(jlong v1, int v2, JavaByteCodes op) stdcall__;

/*@}*/  // ~group of RUNTIME_ARITHMETIC_HELPERS


}}; // ~namespace Jitrino::Jet

#endif      // ~__ARITH_RT_H_INCLUDED__

