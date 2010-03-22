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
 * @author Andrey Chernyshev
 */  


#ifndef _sync_bits_H
#define _sync_bits_H


// On IPF the lower 32 bits are the same as on the IA32. 
// The upper 32 bits are used only when there is a pointer in the field. 

// These are all POINTER_SIZE_INT but I can't cast them since they are
// used inside of ASM code so things like (POINTER_SIZE_INT)0x0000001E breaks syntatically
// since it isn't C code...
// a max of 4K threads
#define BUSY_FORWARDING_OFFSET                  0x00000000
#define QUICK_RECURSION_MASK                    0x0000001E
#define QUICK_RECURSION_LEFT_SHIFT_COUNT        0x00000001
#define QUICK_RECURSION_ABOUT_TO_OVERFLOW       0x0000001C
#define SLOW_LOCKING                            0x0000001E
#define QUICK_RECURSION_INC_DEC_REMENT          0x00000002
#define QUICK_THREAD_INDEX_MASK                 0x0001FFE0
#define QUICK_THREAD_INDEX_LEFT_SHIFT_COUNT     0x00000005
#define QUICK_THREAD_INDEX_WIDTH                0x0000000C
#define QUICK_HASH_MASK                         0xFFFE0000
#define QUICK_HASH_MASK_WIDTH                   0x0000000F
#define QUICK_HASH_LEFT_SHIFT_COUNT             0x00000011
#ifdef POINTER64
#define LOCK_BLOCK_POINTER_MASK                 0xFFFFffffFFFFFFE0
#else
#define LOCK_BLOCK_POINTER_MASK                 0xFFFFFFE0
#endif
#define SINGLE_THREAD_W_RECURSION_SET_TO_ONE    0x00000022

#endif // \include\sync_bits.h

