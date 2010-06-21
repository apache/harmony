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
 
#ifndef _OPTARITHMETIC_H
#define _OPTARITHMETIC_H

namespace Jitrino {
/*
 * Implemented algorithms are similar to the ones described in 
 * [T.Granlund and P.L.Montgomery. Division by Invariant Integers using 
 * Multiplication. PLDI, 1994]
 */

template <typename inttype, int width> inline int popcount(inttype x);
template <>
inline
int popcount<I_32, 32> (I_32 x)
{
#ifdef _USE_ITANIUM_INTRINSICS_
    U_32 y = x; // avoid sign extension
    __m64 z = _m_from_int(int(y));
    __m64 r = __m64_popcnt(z);
    return _m_to_int(r);
#else
    x = x - ((x >>1) & 0x55555555);
    x = (x & 0x33333333) + ((x >> 2) & 0x33333333);
    x = (x + (x >> 4)) & 0x0f0f0f0f;
    x = x + (x >> 8);
    x = x + (x >> 16);
    return x & 0x0000003F;
#endif
}
template <>
inline
int popcount<int64, 64> (int64 x)
{
#ifdef _USE_ITANIUM_INTRINSICS_
    __m64 y = _m_fron_int(int(x));
    __m64 r = __m64_popcnt(y);
    return _m_to_int(r);
#else
    int64 tmp5 = 0x55555555;
    tmp5 = tmp5 | (tmp5 << 32);
    x = x - ((x >>1) & tmp5);

    int64 tmp3 = 0x33333333;
    tmp3 = tmp3 | (tmp3 << 32);
    x = (x & tmp3) + ((x >> 2) & tmp3);

    int64 tmp0f = 0x0f0f0f0f;
    tmp0f = tmp0f | (tmp0f << 32);
    x = (x + (x >> 4)) & tmp0f;

    x = x + (x >> 8);
    x = x + (x >> 16);
    x = x + (x >> 32);
    return int(x & 0x0000007F);
#endif
}

template <typename inttype>
bool isPowerOf2(inttype d) {
    if (d < 0) {
        d = -d;
    }
    // turn trailing 0s into 1s, others become 0
    inttype justrightzeros = (~d) & (d - 1);
    inttype bitandleftbits = ~justrightzeros;
    // rightmost 1-bit and trailing 0s:
    inttype bitandrightzeros = d ^ (d - 1);
    inttype justrightbit = bitandrightzeros & bitandleftbits;
    if (d == justrightbit) {
        return true;
    } else {
        return false;
    }
}

template <typename inttype, typename uinttype, 
          int width>
void
getMagic(inttype d, inttype *magicNum, inttype *shiftBy)
{
    // d must not be -1, 0, 1
    assert((d < -1) || (d > 1));

    const uinttype hiBitSet = (uinttype)1 << (width-1);

    uinttype ad = abs(d);                         // ad = |d|
    uinttype t = hiBitSet + (((uinttype)d) >> (width-1));
    uinttype anc = t - 1 - (t%ad);                // anc = |nc|

    // initial value, we try values from [width-1, 2*width]
    uinttype p = width-1;                 // power of 2 to divide by

    // these are maintained incrementally in loop as p is changed
    uinttype q1 = hiBitSet / anc;             // q1 = 2**p/|nc|
    uinttype r1 = hiBitSet - q1*anc;              // r1 = rem(2**p, |nc|)
    uinttype q2 = hiBitSet / ad;                  // q2 = 2**p /|d|
    uinttype r2 = hiBitSet - q2*ad;               // r2 = rem(2**p, |d|)

    uinttype delta = 0;
    
    do {
        p = p + 1;
        
        // increment q1, r1, q2, r2
        q1 = q1 << 1;
        r1 = r1 << 1;
        q2 = q2 << 1;
        r2 = r2 << 1;
        // r1 overflows into q1
        if (r1 >= anc) {
            q1 = q1 + 1;
            r1 = r1 - anc;
        }
        // r2 overflows into q2
        if (r2 >= ad) {
            q2 = q2 + 1;
            r2 = r2 - ad;
        }

        delta = ad - r2;

    } while ((q1 < delta) || ((q1 == delta) && (r1 == 0)));

    inttype mag1 = q2 + 1;
    *magicNum = (d < 0) ? -mag1 : mag1;
    *shiftBy = p - width;
}

// isolate leftmost 1
template <typename inttype>
inline
inttype leftmost1(inttype x);

template <>
inline
I_32 leftmost1<I_32>(I_32 x)
{
    if (x == 1) return 1;
    if (x == 0) return 0;
    x = x | (x >> 1);
    x = x | (x >> 2);
    x = x | (x >> 4);
    x = x | (x >> 8);
    x = x | (x >> 16);
    x = x >> 1;
    x = x + 1;
    return x;
}

template <>
inline
int64 leftmost1<int64>(int64 x)
{
    if (x == 1) return 1;
    if (x == 0) return 0;
    x = x | (x >> 1);
    x = x | (x >> 2);
    x = x | (x >> 4);
    x = x | (x >> 8);
    x = x | (x >> 16);
    x = x | (x >> 32);
    x = x >> 1;
    x = x + 1;
    return x;
}

// number of leading (leftmost) zeros
template <typename inttype, int width>
inline
int nlz(inttype x);

template <>
inline
int nlz<I_32, 32>(I_32 x)
{
    x = x | (x >> 1);
    x = x | (x >> 2);
    x = x | (x >> 4);
    x = x | (x >> 8);
    x = x | (x >> 16);
    return popcount<I_32, 32>(~x);
}

template <>
inline
int nlz<int64, 64>(int64 x)
{
    x = x | (x >> 1);
    x = x | (x >> 2);
    x = x | (x >> 4);
    x = x | (x >> 8);
    x = x | (x >> 16);
    x = x | (x >> 32);
    return popcount<int64, 64>(~x);
}

// number of trailing (rightmost) zeros
template <typename inttype, int width> 
inline 
int ntz(inttype x)
{
    inttype trailingzeromask = (~x) & (x - 1);
    return popcount<inttype, width>(trailingzeromask);
}

template <typename inttype, int width>
inline
int whichPowerOf2(inttype d) {
    bool isNegative = ((d < 0) && (d != -d));
    if (isNegative) {
        d = -d;
    }
    int i = ntz<inttype, width>(d);
    if (isNegative)
        return -i;
    else
        return i;
}

template <typename inttype>
inline
int shifttomakeOdd(inttype d, inttype &newd)
{
    assert(d != 0);
    int i = 0;
    while ((d & 1) == 0) {
        ++i;
        d = d >> 1;
    }
    newd = d;
    return i;
}

template <typename uinttype>
inline uinttype isqrt_iter(uinttype n, uinttype approx)
{
    uinttype q = n / approx;
    uinttype newapprox = (approx + q) / 2;
    while (q != newapprox) {
        if (newapprox == approx) return approx;
        approx = newapprox;
        q = n / approx;
        newapprox = (approx + q) / 2;
    }
    return newapprox;
}

template <typename uinttype> inline uinttype isqrt(uinttype n);

template <> inline uint64 isqrt(uint64 n) { return isqrt_iter(n, (uint64) 65536); }
template <> inline U_32 isqrt(U_32 n) { return isqrt_iter(n, (U_32) 256); }
template <> inline uint16 isqrt(uint16 n) { return isqrt_iter(n, (uint16) 16); }
template <> inline U_8 isqrt(U_8 n) { return isqrt_iter(n, (U_8) 4); }

} //namespace Jitrino 

#endif // _OPTARITHMETIC_H
