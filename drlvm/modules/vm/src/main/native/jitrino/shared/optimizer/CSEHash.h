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

#ifndef _CSE_HASH_H_
#define _CSE_HASH_H_

#include <iostream>
#include "open/types.h"
#include "HashTable.h"
#include "Opcode.h"

namespace Jitrino {

class Inst;
class Opnd;
class FieldDesc;
class VarOpnd;
class Type;

class CSEHashKey {
public:
    CSEHashKey() {
        opcode = 0;
        opnd1 = opnd2 = opnd3 = 0;
    }
    CSEHashKey(int i) {
        assert(i == 0);
        opcode = 0; opnd1 = opnd2 = opnd3 = 0;
    }
    CSEHashKey(CSEHashKey* key) {
        copy(key);
    }
    CSEHashKey(U_32 opc) {
        opcode = opc;
        opnd1 = opnd2 = opnd3 = 0;
    }
    CSEHashKey(U_32 opc, U_32 opnd) {
        opcode = opc; 
        opnd1 = opnd; 
        opnd2 = 0;
        opnd3 = 0;
    }
    CSEHashKey(U_32 opc, U_32 op1, U_32 op2) {
        opcode = opc;
        opnd1 = op1;
        opnd2 = op2;
        opnd3 = 0;
    }
    CSEHashKey(U_32 opc, U_32 op1, U_32 op2, U_32 op3) {
        opcode = opc;
        opnd1 = op1;
        opnd2 = op2;
        opnd3 = op3;
    }
    bool equals(const CSEHashKey* key) const {
        return ((opcode == key->opcode) && (opnd1 == key->opnd1)
                && (opnd2 == key->opnd2) && (opnd3 == key->opnd3));
    }
    bool operator==(const CSEHashKey &other) const {
        return ((opcode == other.opcode) && (opnd1 == other.opnd1)
                && (opnd2 == other.opnd2) && (opnd3 == other.opnd3));
    }
    bool operator!=(const CSEHashKey &other) const {
        return (!(*this == other));
    }
    operator size_t() const { 
        return (size_t) getHashCode();
    }

    int compare(const CSEHashKey& other) const 
    {
        return (
          opcode < other.opcode?-1:opcode > other.opcode?1:
          opnd1 < other.opnd1?-1:opnd1 > other.opnd1?1:
          opnd2 < other.opnd2?-1:opnd2 > other.opnd2?1:
          opnd3 < other.opnd3?-1:opnd3 > other.opnd3?1:
          0
      );
    }
    bool operator<(const CSEHashKey& other) const
    {
      return compare(other)<0;
    }
    bool operator>(const CSEHashKey& other) const
    {
        return compare(other)>0;
    }
    bool operator<=(const CSEHashKey& other) const
    {
      return compare(other)<=0;
    }
    bool operator>=(const CSEHashKey& other) const
    {
        return compare(other)>=0;
    }

    U_32 getHashCode() const {
        return opcode + (opnd1 ^ opnd2) + opnd3;
    }
    void copy(const CSEHashKey* key) {
        opcode = key->opcode;
        opnd1 = key->opnd1;
        opnd2 = key->opnd2;
        opnd3 = key->opnd3;
    }
    bool isNull() const { return ((opcode == 0) && (opnd1 == 0) 
                            && (opnd2 == 0) && (opnd3 == 0)); };
    void print (::std::ostream &os) const { 
        os << "(" << (int) opcode << ","
           << (int) opnd1 << ","
           << (int) opnd2 << ","
           << (int) opnd3 << ")";
    }
private:
    U_32   opcode;
    U_32   opnd1;
    U_32   opnd2;
    U_32   opnd3;
};

class CSEHash : public KeyLinkHashTable<CSEHashKey> {
public:
    CSEHash(MemoryManager& mm, U_32 size) : KeyLinkHashTable<CSEHashKey>(mm, size) {}
};

#define CSE_NUM_HASH_LINKS 32
#define CSE_HASH_TABLE_SIZE 128  

class FixedCSEHash : public FixedKeyLinkHashTable<CSEHashKey, CSE_NUM_HASH_LINKS> {
public:
    FixedCSEHash(MemoryManager& mm, U_32 size) 
        : FixedKeyLinkHashTable<CSEHashKey, CSE_NUM_HASH_LINKS>(mm, size) {}
};

class CSEHashTable {
public:
    CSEHashTable(MemoryManager& mm) 
        : numCSE(0), hashTable(mm, CSE_HASH_TABLE_SIZE) {}
    
    virtual ~CSEHashTable() {}

    void    kill() {hashTable.removeAll();}
    virtual Inst*  lookupKey(CSEHashKey* key) { return lookupKeyBase(key); }

    Inst*    lookup(U_32 opc) {
        CSEHashKey key(opc);            
        return lookupKey(&key);
    }
    Inst*    lookup(U_32 opc, U_32 op1) {
        CSEHashKey key(opc, op1);        
        return lookupKey(&key);
    }
    Inst*    lookup(U_32 opc, U_32 op1, U_32 op2) {
        CSEHashKey key(opc, op1, op2);    
        return lookupKey(&key);
    }
    Inst*    lookup(U_32 opc, U_32 op1, U_32 op2, U_32 op3) {
        CSEHashKey key(opc, op1, op2, op3);
        return lookupKey(&key);
    }
    void    remove(U_32 opc, U_32 op1) {
        CSEHashKey key(opc, op1);
        hashTable.remove(&key);
    }
    void    insert(U_32 opc, Inst* inst) {
        CSEHashKey key(opc);            
        hashTable.insert(&key, inst);
    }
    void    insert(U_32 opc, U_32 op1, Inst* inst) {
        CSEHashKey key(opc, op1);        
        hashTable.insert(&key, inst);
    }
    void    insert(U_32 opc, U_32 op1, U_32 op2, Inst* inst) {
        CSEHashKey key(opc, op1, op2);    
        hashTable.insert(&key, inst);
    }
    void    insert(U_32 opc, U_32 op1, U_32 op2, U_32 op3, Inst* inst) {
        CSEHashKey key(opc, op1, op2, op3);    
        hashTable.insert(&key, inst);
    }
    void    insert(CSEHashKey key, Inst* inst) {
        hashTable.insert(&key, inst);
    }
    U_32  numCSE;

protected:
    Inst*  lookupKeyBase(CSEHashKey* key);

private:
    // hash table
    FixedCSEHash        hashTable;
};

class ScopedCSEHashTable : public CSEHashTable {
public:
    ScopedCSEHashTable(MemoryManager& mm, ScopedCSEHashTable* outerScope) 
        : CSEHashTable(mm), _outerScope(outerScope) {}

    virtual ~ScopedCSEHashTable() {}
    Inst*  lookupKey(CSEHashKey* key) {
        ScopedCSEHashTable* table = this;
        Inst* inst = NULL;
        while(table != NULL && inst == NULL) {
            inst = table->lookupKeyBase(key);
            table = table->_outerScope;
        }
        return inst;
    }
    ScopedCSEHashTable* getOuterScope() { return _outerScope; };
private:
    ScopedCSEHashTable* _outerScope;
};

} //namespace Jitrino 

#endif // _CSE_HASH_
