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

#if  defined(__linux__) || defined(MACOSX)
    #include <stdint.h>
#else
    #include <stddef.h>
#endif

/* Need to include inttypes.h to get intptr_t and uintptr_t */
#if defined(ZOS)
#include <inttypes.h>
#endif

#include <jni.h>

#define jlong2addr(a, x) ((a *)((uintptr_t)(x)))
#define addr2jlong(x) ((jlong)((uintptr_t)(x)))

#define float2jint(x)   *((jint *)(&x))
#define double2jlong(x) *((jlong *)(&x))

#define jint2float(x)   *((float *)(&x))
#define jlong2double(x) *((double *)(&x))

#define byte_swap_16(x) ((short)((((x) >> 8) & 0xff) | \
                         (((x) & 0xff) << 8)))

#define byte_swap_32(x) ((((x) & 0xff000000) >> 24) | (((x) & 0x00ff0000) >>  8) | \
             (((x) & 0x0000ff00) <<  8) | (((x) & 0x000000ff) << 24))

#define byte_swap_64(x) ((((x) & 0xff00000000000000ull) >> 56) | \
                         (((x) & 0x00ff000000000000ull) >>  40) | \
                         (((x) & 0x0000ff0000000000ull) >> 24)  | \
                         (((x) & 0x000000ff00000000ull) >> 8)   | \
                         (((x) & 0x00000000ff000000ull) << 8)   | \
                         (((x) & 0x0000000000ff0000ull) << 24)  | \
                         (((x) & 0x000000000000ff00ull) << 40)  | \
                         (((x) & 0x00000000000000ffull) << 56))


#if defined(LINUX_X86_64) || defined(LINUX_IA64)

#include <string.h>
#define get_unaligned(type, ptr)                                  \
({                                                                \
    type __tmp;                                                   \
    memmove(&__tmp, (const void*) (ptr), sizeof(type));           \
    __tmp;                                                        \
})

#define set_unaligned(type, ptr, val)                           \
({                                                              \
    memmove((void*) (ptr), &val, sizeof(type));                 \
    (void)0;                                                    \
})

#else

#define get_unaligned(type, ptr) ( *((type *)((uintptr_t)(ptr))) )
#define set_unaligned(type, ptr, val) ( (void) (*((type *)((uintptr_t)(ptr))) = val) )

#endif 
