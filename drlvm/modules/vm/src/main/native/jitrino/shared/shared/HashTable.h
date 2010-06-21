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

#ifndef _HASHTABLE_H_
#define _HASHTABLE_H_

#include <assert.h>
#include "Dlink.h"
#include "MemoryManager.h"
#include "port_threadunsafe.h"

namespace Jitrino {

class HashTableIterImpl; // forward declaration

//
// hash table: implementation is done using void*
// class HashTable provides syntactic sugar around HashTableImpl
// to get type safety
//
struct HashTableLink {
    HashTableLink(void* k, void* e, HashTableLink* nxt) 
        : keyPtr(k), elemPtr(e), next(nxt) {}
    HashTableLink() {
        keyPtr = elemPtr = NULL;
        next = NULL;
    }
    void* keyPtr;
    void* elemPtr;
    HashTableLink* next;
};

class HashTableImpl {
public:
    HashTableImpl(MemoryManager& mm, U_32 size) 
        : memManager(mm), table(0), tableSize(size) {
        table = new (memManager) HashTableLink*[tableSize];
        for (U_32 i=0; i<tableSize; i++)
            table[i] = NULL;
    }
    HashTableImpl(MemoryManager& mm,U_32 size,HashTableLink** t) 
        : memManager(mm), table(t), tableSize(size) {
        removeAll();
    }
    virtual ~HashTableImpl() {}

    void* lookup(void* key) const {
        HashTableLink* entry = lookupEntry(key);
        if (entry == NULL)
            return NULL;
        return entry->elemPtr;
    }
    void  insert(void* key, void* value) {
        HashTableLink* entry = lookupEntry(key);
        if (entry == NULL) {
            U_32 idx = getTableIndex(key);
            HashTableLink* link = createLink(key,value);
            link->next = table[idx];
            table[idx] = link;
        } else {
            entry->elemPtr = value;
        }
    }
    void    remove(void* key) {
        U_32 idx = getTableIndex(key);
        HashTableLink* prev = NULL;
        for (HashTableLink* e = table[idx]; e != NULL; prev = e, e = e->next) {
            if (equals(e->keyPtr,key)) {
                if (e == table[idx]) 
                    table[idx] = e->next;
                else
                    prev->next = e->next;
                freeLink(e);
                return;         
            } 
        }
    }
    void    removeAll() {
        for (U_32 i=0; i<tableSize; i++) {
            HashTableLink* link;
            if ((link = table[i]) == NULL)
                continue;
            do {
                HashTableLink* next = link->next;
                freeLink(link);
                link = next;
            } while (link != NULL);
            table[i] = NULL;
        }
    }
protected:
    virtual HashTableLink* lookupEntry(void* key) const {
#ifdef _DEBUG
        UNSAFE_REGION_START
        ((HashTableImpl *)this)->numLookup++;
        UNSAFE_REGION_END
#endif
        for (HashTableLink* e = table[getTableIndex(key)]; e != NULL; e = e->next) {
#ifdef _DEBUG
            UNSAFE_REGION_START
            ((HashTableImpl *)this)->numLookupEntry++;
            UNSAFE_REGION_END
#endif
            if (equals(e->keyPtr,key)) {
#ifdef _DEBUG
                UNSAFE_REGION_START
                ((HashTableImpl *)this)->numFound++;
                UNSAFE_REGION_END
#endif
                return e;
            }
        }
        return NULL;
    }
    U_32 getTableIndex(void* key) const {
        return getHashCode(key) % tableSize;
    }
    virtual HashTableLink*  createLink(void* key,void* elem) {
        return new (memManager) HashTableLink(key,elem,NULL);
    }
    virtual void    freeLink(HashTableLink* link) {
        delete link;
    }
    virtual bool    equals(void* key1,void* key2) const = 0;
    virtual U_32  getHashCode(void* key) const = 0;

    friend class HashTableIterImpl;
    //
    // protected fields
    //
    MemoryManager&  memManager;
    HashTableLink** table;
    U_32          tableSize;

public:
    static U_32 numLookup;
    static U_32 numLookupEntry;
    static U_32 numFound;
};

//
// the iterator that traverses  every element in the table
// 
class HashTableIterImpl {
public:
    HashTableIterImpl(HashTableImpl* ht) 
    : hashTable(ht), nextEntry(0) {
        nextElem = hashTable->table[0];
        searchNextElem();
    }
    bool getNextElem(void*& key, void*& elem) {
        if (nextElem != NULL) {
            elem = nextElem->elemPtr;
            key  = nextElem->keyPtr;
            nextElem = nextElem->next;
            searchNextElem();
            return true;
        }
        return false;
    }
protected:
    void searchNextElem() {
        if (nextElem != NULL) 
            return;
        // skip entry table entries
        for (nextEntry++; nextEntry < hashTable->tableSize; nextEntry++) {
            if (hashTable->table[nextEntry] != NULL) {
                nextElem = hashTable->table[nextEntry];
                break;
            }
        }
    }

    HashTableImpl* hashTable;
    U_32         nextEntry;
    HashTableLink* nextElem;
};

template<class KEY>
struct LinkWithKey : HashTableLink {
    LinkWithKey(KEY* k,void* e,HashTableLink* next) :
      HashTableLink(&key,e,next), key(k) {}
    LinkWithKey() : HashTableLink(&key,NULL,NULL), key() {}
    KEY key;
};

template <class KEY>
class KeyLinkHashTable : HashTableImpl {
public:
    KeyLinkHashTable(MemoryManager& mm,U_32 size) : HashTableImpl(mm,size) {}
    virtual ~KeyLinkHashTable() {}
    void*   lookup(KEY* key) const {return HashTableImpl::lookup(key);}
    void    insert(KEY* key,void* value) {HashTableImpl::insert(key,value);}
    void    remove(KEY* key) {HashTableImpl::remove(key);}
    void    removeAll() {HashTableImpl::removeAll();}
protected:
    virtual HashTableLink* lookupEntry(void* key) const {
        return HashTableImpl::lookupEntry(key);
    }
    virtual HashTableLink* createLink(void* key,void* elem) {
        return new (memManager) LinkWithKey<KEY>((KEY*)key,elem,NULL);
    }
    virtual void freeLink(HashTableLink* link) {
        delete (LinkWithKey<KEY>*)link;
    }
    virtual bool equals(void* key1,void* key2) const {
        return ((KEY*)key1)->equals((KEY*)key2);
    }
    virtual U_32 getHashCode(void* key) const {
        return ((KEY*)key)->getHashCode();
    }
};

template<class KEY,U_32 NUM_LINKS>
class FixedKeyLinkHashTable : public KeyLinkHashTable<KEY> {
public:
    FixedKeyLinkHashTable(MemoryManager& mm,U_32 size) 
        : KeyLinkHashTable<KEY>(mm,size) {
        freeList = &links[0];
        for (U_32 i=0; i<NUM_LINKS-1; i++) {
            // initialize 
            links[i].next = &links[i+1];
        }
    }
protected:
    virtual HashTableLink* lookupEntry(void* key) const {
        Link* link = (Link*)KeyLinkHashTable<KEY>::lookupEntry(key);
        if (link == NULL)
            return NULL;
        ((FixedKeyLinkHashTable *)this)->moveToFront(link);
        return link;
    }
    virtual HashTableLink* createLink(void* key,void* elem) {
        Link* link;
        if (freeList == NULL) {
            // get last guy at the end of mru list
            link = (Link*)mruList.getPrev()->getElem();
            FixedKeyLinkHashTable<KEY,NUM_LINKS>::remove(&link->key);
        }
        assert(freeList != NULL);
        link = freeList;
        freeList = (Link*)freeList->next;
        link->elemPtr = elem;
        link->key.copy((KEY*)key);
        moveToFront(link);
        return link;
    }
    virtual void    freeLink(HashTableLink* link) {
        Link* l = (Link*)link;
        l->next = freeList;
        freeList = l;
        l->elemPtr = NULL;
        l->mruLink.unlink();
    }
private:
    struct Link : LinkWithKey<KEY> {
        Link() : LinkWithKey<KEY>() {
            mruLink.setElem(this);
        }
        DlinkElem   mruLink;
    };
    void    moveToFront(Link* link) {
        link->mruLink.unlink();
        link->mruLink.insertAfter(&mruList);
    }
    Link        links[NUM_LINKS];
    Link*       freeList;
    DlinkElem   mruList;
};

struct DoublePtrKey {
    DoublePtrKey(DoublePtrKey* key) {
        copy(key);
    }
    DoublePtrKey(void* k1, void*k2) {
        key1 = k1; key2 = k2;
    }
    void    copy(const DoublePtrKey* key) {
        key1 = key->key1; key2 = key->key2;
    }
    bool    equals(const DoublePtrKey* key) const {
        return (key1 == key->key1 && key2 == key->key2);
    }
    U_32  getHashCode() const {
        return ((U_32)(((POINTER_SIZE_INT)key1)>>3) ^ (U_32)(((POINTER_SIZE_INT)key2)>>3));
    }
    void* key1;
    void* key2;
};

class DoubleKeyHashTable : KeyLinkHashTable<DoublePtrKey> {
public:
    DoubleKeyHashTable(MemoryManager& mm,U_32 size) 
        : KeyLinkHashTable<DoublePtrKey>(mm,size) {}
    virtual ~DoubleKeyHashTable() {}
    void*   lookup(void* key1,void* key2) const {
        DoublePtrKey key(key1,key2);
        return KeyLinkHashTable<DoublePtrKey>::lookup(&key);
    }
    void    insert(void* key1,void* key2,void* value) {
        DoublePtrKey key(key1,key2);
        KeyLinkHashTable<DoublePtrKey>::insert(&key,value);
    }
    void    remove(void* key1,void* key2) {
        DoublePtrKey key(key1,key2);
        KeyLinkHashTable<DoublePtrKey>::remove(&key);
    }
    void    removeAll() {
        KeyLinkHashTable<DoublePtrKey>::removeAll();
    }
};

//
// redefine functions to provide types
//
template <class KEY, class VALUE>
class HashTable : public HashTableImpl {
public:
    HashTable(MemoryManager& mm,U_32 sz) : HashTableImpl(mm,sz) {}
    VALUE* lookup(KEY* key) const {return (VALUE*)HashTableImpl::lookup(key);}
    void   insert(KEY* key, VALUE* value) {HashTableImpl::insert(key,value);}
    void   remove(KEY* key) {HashTableImpl::remove(key);}
protected:
    virtual bool   keyEquals(KEY* key1, KEY* key2) const = 0;
    virtual U_32 getKeyHashCode(KEY* key) const = 0;
private:
    bool   equals(void* key1, void* key2) const {return keyEquals((KEY*)key1,(KEY*)key2);}
    U_32 getHashCode(void* key) const {return getKeyHashCode((KEY*)key);}
};

template <class KEY, class VALUE>
class ConstHashTable : public HashTableImpl {
public:
    ConstHashTable(MemoryManager& mm,U_32 sz) : HashTableImpl(mm,sz) {}
    const VALUE* lookup(const KEY* key) const {return (const VALUE*)HashTableImpl::lookup((void *)key);}
    void   insert(const KEY* key, const VALUE* value) {HashTableImpl::insert((void *)key,(void *)value);}
    void   remove(const KEY* key) {HashTableImpl::remove((void *)key);}
protected:
    virtual bool   keyEquals(const KEY* key1, const KEY* key2) const = 0;
    virtual U_32 getKeyHashCode(const KEY* key) const = 0;
private:
    bool   equals(void* key1, void* key2) const {return keyEquals((const KEY*)key1,(const KEY*)key2);}
    U_32 getHashCode(void* key) const {return getKeyHashCode((const KEY*)key);}
};

template <class ELEM_TYPE>
class PtrHashTable : public HashTable<void,ELEM_TYPE> {
public:
    PtrHashTable(MemoryManager& mm,U_32 size) : HashTable<void,ELEM_TYPE>(mm,size) {}
protected:
    virtual bool keyEquals(void* key1,void* key2) const {
        return key1 == key2;
    }
    virtual U_32 getKeyHashCode(void* key) const {
        // return hash of address bits
        return ((U_32)(((POINTER_SIZE_INT)key) >> sizeof(void*)));
    }
};

template <class KEY, class ELEM_TYPE>
class HashTableIter : public HashTableIterImpl {
public:
    HashTableIter(HashTable<KEY,ELEM_TYPE>* ht) : HashTableIterImpl(ht) {}
    bool getNextElem(KEY*& key, ELEM_TYPE*& elem) {
        return HashTableIterImpl::getNextElem((void*&)key,(void*&)elem);
    }
};

template <class KEY, class ELEM_TYPE>
class ConstHashTableIter : public HashTableIterImpl {
public:
    ConstHashTableIter(ConstHashTable<KEY,ELEM_TYPE>* ht) : HashTableIterImpl(ht) {}
    bool getNextElem(const KEY*& key, const ELEM_TYPE*& elem) {
        return HashTableIterImpl::getNextElem((void*&)key,(void*&)elem);
    }
};

} //namespace Jitrino 

#endif // _HASHTABLE_H_
