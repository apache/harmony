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
//MVM
#include <iostream>

using namespace std;

#include <stdio.h>
#include "bit_vector.h"

/* 
 * Sum of bits of correspondent bytes.
 */
static unsigned O3_bitcount[256] = {
    0, 1, 1, 2, 1, 2, 2, 3 /* 00000111 */,  1, 2, 2, 3, 2, 3, 3, 4 /* 00001111 */,
    1, 2, 2, 3, 2, 3, 3, 4 /* 00010111 */,  2, 3, 3, 4, 3, 4, 4, 5 /* 00011111 */,
    1, 2, 2, 3, 2, 3, 3, 4 /* 00100111 */,  2, 3, 3, 4, 3, 4, 4, 5 /* 00101111 */,
    2, 3, 3, 4, 3, 4, 4, 5 /* 00110111 */,  3, 4, 4, 5, 4, 5, 5, 6 /* 00111111 */,
    1, 2, 2, 3, 2, 3, 3, 4 /* 01000111 */,  2, 3, 3, 4, 3, 4, 4, 5 /* 01001111 */,
    2, 3, 3, 4, 3, 4, 4, 5 /* 01010111 */,  3, 4, 4, 5, 4, 5, 5, 6 /* 01011111 */,
    2, 3, 3, 4, 3, 4, 4, 5 /* 01100111 */,  3, 4, 4, 5, 4, 5, 5, 6 /* 01101111 */,
    3, 4, 4, 5, 4, 5, 5, 6 /* 01110111 */,  4, 5, 5, 6, 5, 6, 6, 7 /* 01111111 */,
    1, 2, 2, 3, 2, 3, 3, 4 /* 10000111 */,  2, 3, 3, 4, 3, 4, 4, 5 /* 10001111 */,
    2, 3, 3, 4, 3, 4, 4, 5 /* 10010111 */,  3, 4, 4, 5, 4, 5, 5, 6 /* 10011111 */,
    2, 3, 3, 4, 3, 4, 4, 5 /* 10100111 */,  3, 4, 4, 5, 4, 5, 5, 6 /* 10101111 */,
    3, 4, 4, 5, 4, 5, 5, 6 /* 10110111 */,  4, 5, 5, 6, 5, 6, 6, 7 /* 10111111 */,
    2, 3, 3, 4, 3, 4, 4, 5 /* 11000111 */,  3, 4, 4, 5, 4, 5, 5, 6 /* 11001111 */,
    3, 4, 4, 5, 4, 5, 5, 6 /* 11010111 */,  4, 5, 5, 6, 5, 6, 6, 7 /* 11011111 */,
    3, 4, 4, 5, 4, 5, 5, 6 /* 11100111 */,  4, 5, 5, 6, 5, 6, 6, 7 /* 11101111 */,
    4, 5, 5, 6, 5, 6, 6, 7 /* 11110111 */,  5, 6, 6, 7, 6, 7, 7, 8 /* 11111111 */
};

unsigned Bit_Vector::bits_set()
{
    unsigned result = 0;
    unsigned i;
    for (i=0; i<_n_words-1; i++)
    {
        unsigned char *tmp = (unsigned char *)&_words[i];
        for (unsigned j=0; j<BV_word_size; j++) 
            result += O3_bitcount[tmp[j]];
    }
   
    BV_word_type tmp1 = _words[i];
    unsigned remaining = _n_bits % BV_word_n_bits;
    if (remaining > 0)
        tmp1 &= (((BV_word_type)1 << remaining) - 1);
    unsigned char *tmp = (unsigned char *)&tmp1;
    for (unsigned j=0; j<BV_word_size; j++) 
            result += O3_bitcount[tmp[j]];

    return result;
}

unsigned Bit_Vector::bits_set_without_regs()
{
    return bits_set() - O3_bitcount[_words[0] & 0xff];
}

void Bit_Vector::fill_in_index_array_no_regs(unsigned *array)
{
    unsigned cur = 0;
    for (unsigned i=1; i<_n_words*BV_word_size; i++)
    {
        unsigned char val = ((unsigned char *)_words)[i];
        if (O3_bitcount[val] > 0)
        {
            for (unsigned j=0; j<8; j++)
            {
                if (val & ((BV_word_type)1 << j))
                {
                    array[cur++] = i*8 + j;
                }
            }
        }
    }
}

void Bit_Vector::fill_in_index_array(unsigned *array)
{
    unsigned cur = 0;
    for (unsigned i=0; i<_n_words*BV_word_size; i++)
    {
        unsigned char val = ((unsigned char *)_words)[i];
        if (O3_bitcount[val] > 0)
        {
            for (unsigned j=0; j<8; j++)
            {
                if (val & ((BV_word_type)1 << j))
                {
                    array[cur++] = i*8 + j;
                }
            }
        }
    }
}

void Bit_Vector::fill_in_index_array_inverted(unsigned *array)
{
    unsigned cur = 0;
    for (unsigned i=0; i<_n_words*BV_word_size; i++)
    {
        unsigned char val = (unsigned char)(((unsigned char *)_words)[i] ^ 0xff);
        if (O3_bitcount[val] > 0)
        {
            for (unsigned j=0; j<8; j++)
            {
                if (val & ((BV_word_type)1 << j))
                {
                    array[cur++] = i*8 + j;
                }
            }
        }
    }
}

static void __print_a_bv (ostream &cout, BV_word_type *_vector, unsigned _n_elems) {
    unsigned print_count = 0;
    for (unsigned i = 0; i < _n_elems; i++) {
        BV_word_type v = _vector[i];
        if (v==0)
            continue;
        for(unsigned j = 0; j<BV_word_n_bits; j++,v>>=1)
            if (v & 1) {
                if (print_count++)
                    cout << " ";
                cout << (unsigned)(j+i*BV_word_n_bits);
            }
    }
}

static inline void _print_a_range (ostream &cout, unsigned *p_print_count,
                            unsigned lo,
                            unsigned hi) {
    char buf[32];
    assert(lo<=hi);
    if ((*p_print_count)++)
        cout << " ";
    sprintf(buf,(lo==hi?"%u":"%u-%u"), lo, hi);
    cout << buf;
}


static void __print_a_bv_in_range (ostream &cout,BV_word_type *_vector, unsigned _n_elems) {
    unsigned print_count = 0;
    #define BV_INV_LEADING_BIT1 -1
    int leading_bit1 = BV_INV_LEADING_BIT1, ending_bit1 = 0;
    for (unsigned i = 0; i < _n_elems; i++) {
        BV_word_type v = _vector[i];
        if (v==0) {
            if (leading_bit1 >= 0) {
                _print_a_range(cout, &print_count, leading_bit1, ending_bit1);
                leading_bit1 = BV_INV_LEADING_BIT1;
            }
            continue;
        }
        for(unsigned j = 0; j<BV_word_n_bits; j++,v>>=1)
            if (v & 1) {
                if (leading_bit1 >= 0) {
                    ending_bit1++;
                } else
                    leading_bit1 = ending_bit1 = j+i*BV_word_n_bits;
            } else
                if (leading_bit1 >= 0) {
                    _print_a_range(cout, &print_count, leading_bit1, ending_bit1);
                    leading_bit1 = BV_INV_LEADING_BIT1;
                }
    }
    if (leading_bit1 >= 0) {
        _print_a_range(cout,&print_count, leading_bit1, ending_bit1);
        leading_bit1 = BV_INV_LEADING_BIT1;
    }
}

void Bit_Vector::print (ostream &cout, char *name) {
    if (name) {
        cout << "(set " << name << " {";
    } else
        cout << "(set {";

    __print_a_bv(cout,_words, _n_words);

    cout << "})\n";
}

void Bit_Vector::print_in_range (ostream & cout,char *name) {
    if (name) {
        cout << "(set " << name << " {";
    } else
        cout << "(set {";

    __print_a_bv_in_range(cout,_words, _n_words);

    cout << "})\n";
}

//---------------------------------------------------------------------------
// Bit_Vector_Array
//
Bit_Vector_Array::Bit_Vector_Array (unsigned n_bv,
                                    unsigned n_bv_bit,
                                    MemoryPool &mem ) {
    _n_bv = n_bv;
    _n_bv_bit = n_bv_bit;
    unsigned n_bv_byte = Bit_Vector::mem_size(n_bv_bit);
    assert((n_bv_byte%BV_word_size)==0);
    _n_bv_elem = n_bv_byte / BV_word_size;
    unsigned n_byte = n_bv_byte * n_bv;
    _vector_array = (BV_word_type *)mem.alloc(n_byte);
    clear_all();
}

Bit_Vector_Array::~Bit_Vector_Array () {}

void Bit_Vector_Array::init (unsigned n_bv,
                             unsigned n_bv_bit,
                             BV_word_type *_bva_mem ) {
    _n_bv = n_bv;
    _n_bv_bit = n_bv_bit;
    unsigned n_bv_byte = Bit_Vector::mem_size(n_bv_bit);
    assert((n_bv_byte%BV_word_size)==0);
    _n_bv_elem = n_bv_byte / BV_word_size;
    _vector_array = _bva_mem;
    clear_all();
}

unsigned Bit_Vector_Array::mem_size (unsigned n_bv, unsigned n_bv_bit) {
    unsigned n_bv_word = (n_bv_bit + BV_word_n_bits-1) / BV_word_n_bits;
    unsigned n_bv_byte = n_bv_word * BV_word_size;
    return n_bv_byte * n_bv;
}

void Bit_Vector_Array::clear_all () {
    unsigned n_elems = _n_bv_elem * _n_bv;
    for (unsigned i = 0; i < n_elems; i++)
        _vector_array[i] = 0;
}


void Bit_Vector_Array::print (ostream &cout, char *name) {
    if (name) {
        cout << "(set " << name << " {";
    } else
        cout << "(set {";

    BV_word_type *bv = _vector_array;
    for(unsigned j=0; j<_n_bv; j++,bv+=_n_bv_elem) {
        if (j)
            cout << "/";
        __print_a_bv(cout,bv, _n_bv_elem);
    }

    cout << "})\n";
}

void Bit_Vector_Array::print_in_range (ostream &cout, char *name) {
    if (name) {
        cout << "(set " << name << " {";
    } else
        cout << "(set {";

    BV_word_type *bv = _vector_array;
    for(unsigned j=0; j<_n_bv; j++,bv+=_n_bv_elem) {
        if (j)
            cout << "/";
        __print_a_bv_in_range(cout,bv, _n_bv_elem);
    }

    cout << "})\n";
}
