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
#ifndef _BIT_VECTOR_H
#define _BIT_VECTOR_H

#include <assert.h>
#include <string.h>

//MVM
#include <iostream>

using namespace std;

#include "open/types.h"
#include "tl/memory_pool.h"

typedef class tl::MemoryPool MemoryPool;

#define FIX_DCE

// Bit_Vector is a standard dense bit vector implementation.

typedef unsigned  BV_word_type;

#define BV_word_size    (sizeof(BV_word_type))
#define BV_word_n_bits  (BV_word_size << 3)

class Bit_Vector
{
public:


  void set_all() { 
      memset(_words, 0xff, _n_words * BV_word_size); 
  }
  void reset_all() {
      memset(_words, 0x00, _n_words * BV_word_size); 
  }

    
  Bit_Vector () : _n_bits(0), _n_words(0), _words(0) {}
  Bit_Vector(unsigned n_bit, MemoryPool &mem,  bool initial_state=false) :
      _n_bits(n_bit) {
      _n_words =(_n_bits + BV_word_n_bits-1)/BV_word_n_bits;
      _words=(BV_word_type *)mem.alloc(_n_words*BV_word_size);
          if (initial_state)
              set_all();
          else
              reset_all();
  }

  Bit_Vector(MemoryPool &mem, Bit_Vector *src) :
      _n_bits(src->_n_bits), _n_words(src->_n_words), 
      _words((BV_word_type *)mem.alloc(src->_n_words*BV_word_size)) {
      copy_from(src); 
  }

  // Create a bit vector based on allocated memory
  Bit_Vector(unsigned n_bit, BV_word_type *_bv_mem, bool reset) :
    _n_bits(n_bit), _n_words((_n_bits + BV_word_n_bits-1)/BV_word_n_bits),
        _words(_bv_mem) {
    if (reset)
        clear_all();
  }

  void * operator new(size_t sz, MemoryPool & mem) { return mem.alloc(sz); }
  void operator delete (void * UNREF p, MemoryPool& UNREF men) {
      // Added this to avoid warning
      // This method does nothing, because
      // memory allocated via memory manager is freed all at once
  }

  unsigned numbits() const { return _n_bits; }
  unsigned numwords() const { return _n_words; }
  // Bytes required to store n bits
  static unsigned mem_size (unsigned n_bit) {
     unsigned words = (n_bit + BV_word_n_bits-1) / BV_word_n_bits;
     unsigned bytes = words * BV_word_size;
     return bytes;
  }
  //Used by IPF_O1
  void init (unsigned n_bit, MemoryPool &mem) {
    _n_bits = n_bit;
    _n_words = (_n_bits + BV_word_n_bits-1)/BV_word_n_bits;
    _words = (BV_word_type *)mem.alloc(_n_words*BV_word_size);
    clear_all();
  }

  //Used by IPF_O1
  void init_vector_memory (BV_word_type *_bv_mem)  { _words = _bv_mem; }

  void set(unsigned bit) { 
      assert(bit<_n_bits);  
      _words[bit/BV_word_n_bits] |=  ((BV_word_type)1 << (bit % BV_word_n_bits)); 
  }
  void reset(unsigned bit) { 
      assert(bit<_n_bits);    
      _words[bit/BV_word_n_bits] &= ~((BV_word_type)1 << (bit % BV_word_n_bits));
  }
  void clear(unsigned bit) {reset(bit);}

  bool is_set(unsigned bit) { 
      assert(bit<_n_bits); 
      return (_words[bit/BV_word_n_bits] & ((BV_word_type)1 << (bit % BV_word_n_bits))) != 0;
  }

  bool is_clear(unsigned bit) { 
      assert(bit<_n_bits); 
      return (_words[bit/BV_word_n_bits] & ((BV_word_type)1 << (bit % BV_word_n_bits))) == 0;
  }

  void clear_all() {reset_all();}

  void copy_from(Bit_Vector *src) { 
      assert(_n_bits == src->_n_bits);
      for (unsigned i=0; i<_n_words; i++) _words[i]  =  src->_words[i]; 
  }
  void union_from(Bit_Vector *src) {
      assert(_n_bits == src->_n_bits);
      for (unsigned i=0; i<_n_words; i++) _words[i] |=  src->_words[i]; 
  }
  void intersect_from(Bit_Vector *src) { 
      assert(_n_bits == src->_n_bits);
      for (unsigned i=0; i<_n_words; i++) _words[i] &=  src->_words[i]; 
  }
  void subtract      (Bit_Vector *src) {
      assert(_n_bits == src->_n_bits);
      for (unsigned i=0; i<_n_words; i++) _words[i] &= ~src->_words[i]; 
  }
  bool is_empty() { 
      for (unsigned i=0; i<_n_words; i++) if (_words[i]) return false; return true; 
  }
  bool equals(Bit_Vector *src) {
      assert(_n_bits == src->_n_bits);  
      bool result = !memcmp(src->_words, _words, _n_words*BV_word_size);
      return result;
  }
  bool is_subset_of (Bit_Vector *bv) {
    assert(_n_bits==bv->_n_bits);
    for (unsigned i=0; i<_n_words; i++)
        if ((_words[i] & ~bv->_words[i])!=0)
            return false;
    return true;
  }
  bool is_intersection_nonempty (Bit_Vector *bv) {
    assert(_n_words==bv->_n_words);
    for (unsigned i=0; i<_n_words; i++)
        if ((_words[i] & bv->_words[i])!=0)
            return true;
    return false;
  }
  void union_two(Bit_Vector *bv1, Bit_Vector *bv2) {
      assert(_n_bits == bv1->_n_bits && _n_bits == bv2->_n_bits);
      for (unsigned i=0; i<_n_words; i++) _words[i] = (bv1->_words[i] | bv2->_words[i]);
  }
  void intersect_two(Bit_Vector *bv1, Bit_Vector *bv2) {
      assert(_n_bits == bv1->_n_bits && _n_bits == bv2->_n_bits);
      for (unsigned i=0; i<_n_words; i++) _words[i] = (bv1->_words[i] & bv2->_words[i]);
  }

  BV_word_type first_word() { 
      assert(_words != NULL); 
      return _words[0]; 
  }
  unsigned bits_set();
  unsigned bits_set_without_regs();
  void fill_in_index_array(unsigned *array);
  void fill_in_index_array_no_regs(unsigned *array);
  void fill_in_index_array_inverted(unsigned *array);
  void print (ostream &cout, char *name=NULL);
  void print_in_range (ostream &cout, char *name=NULL);
#ifdef FIX_DCE
  BV_word_type get_words(unsigned i) const {assert(i<_n_words) ;return _words[i]; }
#endif
private:
  unsigned _n_bits;                    // vector size (in bits)
  unsigned _n_words;                   // vector size (in words)  
  BV_word_type *_words;   // the actual vector
};

//---------------------------------------------------------------------------
// Element of a list of Bit Vectors
//

class Bit_Vector_List_Element {
  public:
    Bit_Vector_List_Element (Bit_Vector *vec, const unsigned char *num) {
        elem = vec; id = num; next = NULL;
    }

    ~Bit_Vector_List_Element () { /*is this correct????*/ } ;

    void *operator new (size_t sz,MemoryPool& mem) {
        return mem.alloc(sz);
    }
    void operator delete (void * UNREF p, MemoryPool& UNREF mem) {
        // Added this to avoid warning
        // This method does nothing, because
        // memory allocated via memory manager is freed all at once
    }

    Bit_Vector *elem;
    const unsigned char *id; // XXX - add comment here (?)
    Bit_Vector_List_Element *next;
};

//---------------------------------------------------------------------------
// List of Bit Vectors (works as stack)
//

class Bit_Vector_List {
  public:
    Bit_Vector_List () { front = NULL; }
    ~Bit_Vector_List ();
    void *operator new (size_t sz, MemoryPool& mem) {
        return mem.alloc(sz);
    }
    void operator delete (void * UNREF p, MemoryPool& UNREF mem) {
        // Added this to avoid warning
        // This method does nothing, because
        // memory allocated via memory manager is freed all at once
    }
    void push (Bit_Vector_List_Element *bvle) {
        bvle->next = front;
        front = bvle;
    }
    Bit_Vector_List_Element *pop () {
        if (front == NULL) return NULL;
        Bit_Vector_List_Element *temp = front;
        front = front->next;
        return temp;
    }

    Bit_Vector_List_Element *front;
};


//---------------------------------------------------------------------------
// Bit_Vector_Array
//
class Bit_Vector_Array {
  public:
    Bit_Vector_Array (unsigned n_bv, unsigned n_bv_bit, MemoryPool &mem);
    Bit_Vector_Array (unsigned n_bv, unsigned n_bv_bit, BV_word_type *_bva_mem)
      { init(n_bv, n_bv_bit, _bva_mem); }
    Bit_Vector_Array () : _vector_array(NULL), _n_bv_bit(0), _n_bv_elem(0),
                          _n_bv(0) {}
    ~Bit_Vector_Array ();

    void init (unsigned n_bv, unsigned n_bv_bit, BV_word_type *_bva_mem);

    // clear all bits
    void clear_all ();
        
    BV_word_type *bv (unsigned n) {
        assert(n<_n_bv);
        return _vector_array + n * _n_bv_elem;
    }

    static unsigned mem_size (unsigned n_bv, unsigned n_bv_bit);
    void *operator new (size_t sz, MemoryPool& mem) { return mem.alloc(sz); }
    void operator delete (void * UNREF p, MemoryPool& UNREF mem) {
        // Added this to avoid warning
        // This method does nothing, because
        // memory allocated via memory manager is freed all at once
    }
    void print (ostream &cout,char *name=NULL);
    void print_in_range (ostream &cout,char *name=NULL);

  private:
    BV_word_type *_vector_array; // the actual vector
    unsigned _n_bv_bit;                 // vector size (in bits)
    unsigned _n_bv_elem;                // vector size (in words)
    unsigned _n_bv;                     // number of bit vectors
};



#endif // _BIT_VECTOR_H
