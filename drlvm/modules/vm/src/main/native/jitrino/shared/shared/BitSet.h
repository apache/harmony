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

#ifndef _BITSET_H_
#define _BITSET_H_

#include <assert.h>
#include "open/types.h"
#include "MemoryManager.h"
#include <iostream>


namespace Jitrino
{
    
class BitSet  
{

public:
    //
    // Constructors
    //
    BitSet(MemoryManager&, U_32 size);
    //
    //  copy constructors
    //
    BitSet(MemoryManager&, const BitSet&);
    BitSet(const BitSet&);
    //
    //  This constructor is useful when a bit set needs to be expanded into a larger one
    //
    BitSet(MemoryManager&, const BitSet&, U_32 size);
    //
    // assignment operator (resizes the set if necessary)
    //
    BitSet& operator = (const BitSet&);
    //
    // resizes BitSet. Sets new bits to FALSE
    //
    void resize(U_32 newSetSize);
    //
    // resizes BitSet. Sets all bits to FALSE
    //
    void resizeClear(U_32 newSetSize);
    //
    // returns the size of the set
    //
    U_32 getSetSize() const {return setSize;}
    //
    // returns the value of the bit
    //
    bool getBit(U_32 bitNumber) const
    {
        assert(words != 0 && bitNumber < setSize);
        return (reinterpret_cast<char*>(words)[bitNumber >> 3] & (1 << (bitNumber & 7))) != 0;
    }
    //
    // sets a bit to the given value and returns old value of bit
    //
    bool setBit(U_32 bitNumber, bool value = true)
    {
        assert(words != 0 && bitNumber < setSize);
        char& wd = reinterpret_cast<char*>(words)[bitNumber >> 3];
        char  mk = (char)(1 << (bitNumber & 7));

        bool old = (wd & mk) != 0;
        if (value)
            wd |= mk;
        else
            wd &= ~mk;

        return old;
    }
    //
    //  Sets all bits to false
    //
    void clear();
    //
    //  Sets all bits to true
    //
    void setAll();
    //
    //  Checks if set has any bits set to true
    //
    bool isEmpty() const;
    //
    //  Sets 32 bits to values indicated by a bit mask and returns old values
    //
    U_32 set32Bits(U_32 firstBitNumber, U_32 value);
    //
    //  Returns values of 32 bits encoded as a bit mask
    //
    U_32 get32Bits(U_32 firstBitNumber);
    //
    //  Copies another set
    //
    void copyFrom(const BitSet& set);
    //
    //  Copies from a smaller set to another set
    //
    void copyFromSmallerSet(const BitSet& set);
    //
    //  Unions with another set
    //
    void unionWith(const BitSet& set);
    //
    //  Intersects with another set
    //
    void intersectWith(const BitSet& set);
    //
    //  Subtracts another set
    //
    void subtract(const BitSet& set);
    //
    //  Checks if set is equal to another set
    //
    bool isEqual(const BitSet& set);
    //
    //  Checks if set is disjoint from another set
    //
    bool isDisjoint(const BitSet& set);
    //
    //  Checks if every bit in a set is less than or equal to every bit in another set (where false < true).
    //
    bool isLessOrEqual(const BitSet& set);


    class Visitor;

    void visitElems(Visitor& visitor) const;

    class Printer;

    void print(::std::ostream& os);


    class IterB;

private:

    //
    // makes initial allocation
    //
    void alloc(U_32 size);
    void copy (U_32* src);
    //
    // zero bits that out of setSize
    //
    void clearTrailingBits();
    //
    // words containing the set's bits; 
    //
    U_32* words;
    //
    // size of set (in bits)
    //
    U_32 setSize;
    //
    // number of words reserved
    //
    U_32 wordsCapacity; 
    //
    // mem manager used to resize the set
    //
    MemoryManager& mm;
};


class BitSet::Visitor {
public:
    virtual ~Visitor() {}
    virtual void visit(U_32 elem) = 0;
};


class BitSet::Printer : public BitSet::Visitor {
public:
    Printer(::std::ostream & _os) : os(_os) {}
    virtual ~Printer() {}
    void visit(U_32 i) { os << " " << (int) i;}
private:
    ::std::ostream & os;
};


class BitSet::IterB
{
public:

    IterB (const BitSet& set);

    void init (const BitSet& set);
    int  getNext ();

protected:

    U_32 * ptr, * end;
    U_32 mask;
    int idx;
};


} // namespace Jitrino

#endif // _BITSET_H_
