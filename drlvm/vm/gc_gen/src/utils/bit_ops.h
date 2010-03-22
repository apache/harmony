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
 * @author Ji, Qi, 2006/10/25
 */
 
#ifndef _BIT_OPS_H_
#define _BIT_OPS_H_

#include "../common/gc_common.h"

inline unsigned int word_get_first_set_lsb(POINTER_SIZE_INT target_word)
{
  assert(target_word != 0);
  POINTER_SIZE_INT bit_offset = 0;

#if defined(_IPF_) || defined(_WIN64)
    while( ! (target_word & ((POINTER_SIZE_INT)1 << bit_offset)) ){
      bit_offset++;
    }
#else /* !_IPF_ && !_WIN64 */
#ifdef PLATFORM_POSIX  /* linux X86 32/64 */
    __asm__ __volatile__(
      "bsf %1,%0\n"
      :"=r"(bit_offset)
      :"m"(target_word)
    );
#else /* !PLATFORM_POSIX */
#ifdef WIN32 
    __asm{
      bsf eax, target_word
      mov bit_offset, eax
    }
#endif /* WIN32 */
#endif /* !PLATFORM_POSIX */
#endif /* !_IPF_ && !_WIN64 */

  assert(bit_offset < BITS_PER_WORD);
  return (unsigned int)bit_offset;

}

inline unsigned int words_get_next_set_lsb(POINTER_SIZE_INT* words, unsigned int count, unsigned int start_idx)
{
  unsigned int bit_offset;
  
  assert(start_idx < 128);
  
  unsigned int start_word_index = start_idx >> BIT_SHIFT_TO_BITS_PER_WORD;
  unsigned int start_bit_offset = start_idx & BIT_MASK_TO_BITS_PER_WORD;
  
  bit_offset = start_idx - start_bit_offset;
  for(unsigned int i = start_word_index; i < count; i ++ ){
    POINTER_SIZE_INT cur_word = *(words + i);
    
    if(start_word_index == i){
      POINTER_SIZE_INT mask = ~(((POINTER_SIZE_INT)1 << start_bit_offset) - 1);
      cur_word = cur_word & mask;
    }
  
     if(cur_word != 0){
       bit_offset += word_get_first_set_lsb(cur_word);
       return bit_offset;
     }
     
     bit_offset += BITS_PER_WORD;
   }
  
  return bit_offset;
}

inline void words_set_bit(POINTER_SIZE_INT* words, unsigned int count, unsigned int start_idx)
{
  assert(start_idx < 128);
  
  unsigned int word_index = start_idx >> BIT_SHIFT_TO_BITS_PER_WORD;
  unsigned int bit_offset = start_idx & BIT_MASK_TO_BITS_PER_WORD;
  
  if(word_index >= count) return;
  
  volatile POINTER_SIZE_INT* p_word = words + word_index;
  POINTER_SIZE_INT old_value = *p_word;
  POINTER_SIZE_INT mask = (POINTER_SIZE_INT)1 << bit_offset;
  POINTER_SIZE_INT new_value = old_value|mask;
  while (true) {
    POINTER_SIZE_INT temp = atomic_casptrsz(p_word, new_value, old_value);
    if (temp == old_value) break;
    old_value = *p_word;
    new_value = old_value|mask;
  }
  return;
}

inline void words_clear_bit(POINTER_SIZE_INT* words, unsigned int count, unsigned int start_idx)
{
  assert(start_idx < 128);
  
  unsigned int word_index = start_idx >> BIT_SHIFT_TO_BITS_PER_WORD;
  unsigned int bit_offset = start_idx & BIT_MASK_TO_BITS_PER_WORD;
  
  if(word_index >= count) return;
  
  volatile POINTER_SIZE_INT* p_word = words + word_index;
  POINTER_SIZE_INT old_value = *p_word;
  POINTER_SIZE_INT mask = ~((POINTER_SIZE_INT)1 << bit_offset);
  POINTER_SIZE_INT new_value = old_value & mask;
  while (true) {
    POINTER_SIZE_INT temp = atomic_casptrsz(p_word, new_value, old_value);
    if (temp == old_value) break;
    old_value = *p_word;
    new_value = old_value & mask;
  }
  return;
}
#endif
