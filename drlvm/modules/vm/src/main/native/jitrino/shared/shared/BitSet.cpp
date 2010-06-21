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
 * @author Intel, Mikhail Y. Fursov
 *
 */

#include "BitSet.h"

namespace Jitrino
{
    
// use 64-bit words if compiled for IPF

#define WORD_SHIFT_AMOUNT 5
#define WORD_NBITS (1<<WORD_SHIFT_AMOUNT)
#define WORD_MASK (WORD_NBITS-1) //0x0000001F

//
// returns the word containing bitNumber
//
static inline U_32 getWordIndex(U_32 bitNumber) {
    return (bitNumber >> WORD_SHIFT_AMOUNT);
}
//
// returns a mask for bitNumber masking 
//
static inline U_32 getBitMask(U_32 bitNumber) {
    return (0x0001 << (bitNumber & WORD_MASK));
}

static U_32 getNumWords(U_32 setSize) {
    return setSize == 0 ? 0 : getWordIndex(setSize-1) + 1;
}

/*
static U_32 getNumBytes(U_32 setSize) {
    return setSize == 0 ? 0 : (setSize + 7) >> 3;
}
*/
//
// Constructors
//

static U_32* stub=(U_32*)0xDEADBEEF;

BitSet::BitSet(MemoryManager& memManager, U_32 size)
:words(0), setSize(0), wordsCapacity(0), mm(memManager)
{
    if (size != 0)
    {
        alloc(size);
        clear();
    } else {
        words = stub;
    }
}


BitSet::BitSet(MemoryManager& memManager, const BitSet& set)
:mm(memManager)
{
    assert(set.words != 0);
    alloc(set.setSize);
    copy(set.words);
}


BitSet::BitSet(const BitSet& set)
:mm(set.mm)
{
    assert(set.words != 0);
    alloc(set.setSize);
    copy(set.words);
}


BitSet::BitSet(MemoryManager& memManager, const BitSet& set, U_32 size)
:mm(memManager)
{
    alloc(size);
    copyFromSmallerSet(set);
}


BitSet& BitSet::operator = (const BitSet& set)
{
    if (this != &set)
    {
        assert(set.words != 0);
        if (wordsCapacity < getNumWords(set.setSize))
            alloc(set.setSize);
        setSize = set.setSize;
        copy(set.words);
    }
    return *this;
}


void BitSet::alloc(U_32 size)
{
    assert(size != 0);
    words = new (mm) U_32[wordsCapacity = getNumWords(setSize = size)];
}


void BitSet::copy(U_32* src)
{
    for (int i = getNumWords(setSize); --i >= 0;)
        words[i] = src[i];

    clearTrailingBits();
}


void BitSet::resize(U_32 newSetSize) {
    if (newSetSize > setSize ) {
        U_32 newWordsCapacity = getNumWords(newSetSize);
        if (newWordsCapacity > wordsCapacity) {
            U_32 *oldWords = words;
            words = new (mm) U_32[newWordsCapacity];
            for (U_32 i=0; i<wordsCapacity; i++) words[i] = oldWords[i];
            wordsCapacity = newWordsCapacity;
        }
        clearTrailingBits();
        setSize = newSetSize;
    } else if (newSetSize < setSize) {
        assert(newSetSize != 0);
        setSize = newSetSize;
        clearTrailingBits();
    }
}


void BitSet::clearTrailingBits() 
{
    U_32 mk = getBitMask(setSize) - 1;
    for (U_32 i = getWordIndex(setSize); i < wordsCapacity; ++i) {
        words[i] &= mk;
        mk = 0;
    }
}


void BitSet::resizeClear(U_32 newSetSize) {
    U_32 newWordsCapacity = getNumWords(newSetSize);
    if (newWordsCapacity > wordsCapacity) 
        words = new (mm) U_32[wordsCapacity = newWordsCapacity];

    for (U_32 i=0; i<wordsCapacity; i++) 
        words[i] = 0;
    setSize = newSetSize;
}


//
//  Sets all bits to false
//
void BitSet::clear() {
    U_32 numWords = getNumWords(setSize);
    for (U_32 i=0; i<numWords; i++)   words[i] = 0;
}
//
//  Sets all bits to true
//
void BitSet::setAll() {
    assert(words != 0);
    U_32 numWords = getNumWords(setSize);
    for (U_32 i=0; i<numWords; i++)  words[i] = ~((U_32)0);
    clearTrailingBits();
}
//
//  Checks if set has any bits set to true
//
bool BitSet::isEmpty() const {
    assert(words != 0);
    U_32 numWords = getNumWords(setSize);
    for (U_32 i=0; i<numWords; i++) if (words[i] != 0) return false;
    return true;
}
//
//  Sets 32 bits to values indicated by a bit mask and returns old values
//
U_32 BitSet::set32Bits(U_32 firstBitNumber, U_32 value) {
    assert((words != 0 && firstBitNumber < setSize) || firstBitNumber % 32 == 0);
    U_32 wordIndex = getWordIndex(firstBitNumber);
    U_32 oldValue = words[wordIndex];
    words[wordIndex] = value;
    return oldValue;
}
//
//  Returns values of 32 bits encoded as a bit mask
//
U_32 BitSet::get32Bits(U_32 firstBitNumber) {
    assert((words != 0 && firstBitNumber < setSize) || firstBitNumber % 32 == 0);
    return words[getWordIndex(firstBitNumber)];
}
//
//  Copies another set
//
void BitSet::copyFrom(const BitSet& set) {
    assert(set.words != 0);
    if (this != &set) {
        if (words == 0)
            alloc(set.setSize);
        assert(setSize == set.setSize);
        U_32 numWords = getNumWords(setSize);
        for (U_32 i=0; i<numWords; i++) words[i] = set.words[i];
    }
}
//
//  Copies from a smaller set to another set
//
void BitSet::copyFromSmallerSet(const BitSet& set) {
    assert(set.words != 0);
    assert(this != &set);
    if (words == 0)
        alloc(set.setSize);
    assert(setSize >= set.setSize);
    U_32 numWords1 = getNumWords(setSize);
    U_32 numWords2 = getNumWords(set.setSize);
    assert(numWords1 >= numWords2);
    U_32 i;
    for (i=0; i<numWords2; i++) words[i] = set.words[i];
    for (i=numWords2; i<numWords1; i++) words[i] = 0;
}
//
//  Unions with another set
//
void BitSet::unionWith(const BitSet& set) {
    assert(words != 0 && set.words != 0 && setSize == set.setSize);
    U_32 numWords = getNumWords(setSize);
    for (U_32 i=0; i<numWords; i++) words[i] |= set.words[i];
}
//
//  Intersects with another set
//
void BitSet::intersectWith(const BitSet& set) {
    assert(words != 0 && set.words != 0 && setSize == set.setSize);
    U_32 numWords = getNumWords(setSize);
    for (U_32 i=0; i<numWords; i++) words[i] &= set.words[i];
}
//
//  Subtracts another set
//
void BitSet::subtract(const BitSet& set) {
    assert(words != 0 && set.words != 0 && setSize == set.setSize);
    U_32 numWords = getNumWords(setSize);
    for (U_32 i=0; i<numWords; i++) words[i] &= ~(set.words[i]);
}
//
//  Checks if this set is equal to another one
//
bool BitSet::isEqual(const BitSet& set) {
    assert(words != 0 && set.words != 0);
    if (setSize != set.setSize) return false;
    U_32 numWords = getNumWords(setSize);
    for (U_32 i=0; i<numWords; i++) {
        if (words[i] != set.words[i]) return false;
    }
    return true;
}
//
//  Checks if set is disjoint from another set
//
bool BitSet::isDisjoint(const BitSet& set) {
    assert(words != 0 && set.words != 0 && setSize == set.setSize);
    U_32 numWords = getNumWords(setSize);
    for (U_32 i=0; i<numWords; i++) {
        if ((words[i] & set.words[i]) != 0) return false;
    }
    return true;
}
//
//  Checks if every bit in a set is less than or equal to every bit in another set (where false < true).
//
bool BitSet::isLessOrEqual(const BitSet& set) {
    assert(words != 0 && set.words != 0 && setSize == set.setSize);
    U_32 numWords = getNumWords(setSize);
    for  (U_32 i=0; i<numWords; i++) {
        if ((words[i] & (~set.words[i])) != 0) return false;
    }
    return true;
}

void BitSet::visitElems(Visitor& visitor) const {
    assert(words != 0);
    U_32 bitNumber = 0;
    U_32 numWords = getNumWords(setSize);
    for (U_32 i=0; i<numWords; i++) {
        U_32 word = words[i];
        for (U_32 mask = 0x0001; mask != 0; mask = mask << 1) {
            if ((word & mask) != 0)
                visitor.visit(bitNumber);
            bitNumber++;
        }
    }
}

void BitSet::print(::std::ostream& os) {
    Printer printer(os);
    os << " SetElems [";
    visitElems(printer);
    os << " ] " << ::std::endl;
}


BitSet::IterB::IterB (const BitSet& set)        
{
    init(set);
}


void BitSet::IterB::init (const BitSet& set)        
{
    ptr  = set.words - 1;
    end  = set.words + getNumWords(set.setSize);
    idx  = -1;
}


int BitSet::IterB::getNext ()
{
    if ((idx & 31) == 31)
        mask = 0;
    else
        mask >>= 1, ++idx;

    while (mask == 0)
    {
        if (++ptr == end)
            return -1;

        mask = *ptr;
        (idx &= ~31) += 32;
    }

    while ((mask & 1) == 0)
        mask >>= 1, ++idx;

    return idx;
}



} // namespace Jitrino
