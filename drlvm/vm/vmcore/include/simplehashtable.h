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
 * @author Roman S. Bushmanov
 */  

#if !defined(__SIMPLE_HASH_TABLE_H_)
#define __SIMPLE_HASH_TABLE_H_

#include <memory.h>

struct PairEntry{
    PairEntry(void* key_, int value_, void *value1_ = NULL):
        key(key_), value1(value1_), value(value_), next(NULL){
    }
    void *key;
    void *value1;
    int value;
    PairEntry *next;
};

struct SimpleHashtable{
public:
    SimpleHashtable(int bkt_count_): bkt_count(bkt_count_), _size(0), iter(NULL){
        buckets = new PairEntry*[bkt_count]; 
        memset((void*)buckets, 0, bkt_count*sizeof(void*));
    }
    ~SimpleHashtable()
    {
        PairEntry *pe, *pe_tmp;
        for (int i = 0; i < bkt_count; i++)
        {
            pe = buckets[i];
            while (pe != NULL)
            {
                pe_tmp = pe;
                pe = pe->next;
                delete pe_tmp;
            }
        }
        delete[] buckets;
    }
    short hash(void* key){
        // For 32-bit architectures k3 & k4 will be 0 and have no effect on the hash
        // Avoid compilation warnings on IA-32 Linux.
        uint16 k1 = (uint16)(((POINTER_SIZE_INT)key >>  0) & 0xFFFF);
        uint16 k2 = (uint16)(((POINTER_SIZE_INT)key >> 16) & 0xFFFF);
#ifdef _IPF_
        uint16 k3 = (uint16)(((POINTER_SIZE_INT)key >> 32) & 0xFFFF);
        uint16 k4 = (uint16)(((POINTER_SIZE_INT)key >> 48) & 0xFFFF);
        uint16 index = k1 ^ k2 ^ k3 ^ k4;
#else  // !_IPF_
        uint16 index = (uint16)(k1 ^ k2 ^ 0 ^ 0);
#endif // !_IPF_
        uint16 hash = (uint16)(index % bkt_count);
        assert(hash<bkt_count); // Alexei hash is unsigned, so it is positive
        return hash;
    } //hash
    void add(void* key, int value = 1, void* value1 = NULL){
        short index = hash(key);
        if (buckets[index] == NULL){
            PairEntry *pe = new PairEntry(key, value, value1);
            ++_size;
            buckets[index] = pe;
        }else{
            PairEntry *pe0 = NULL, *pe = buckets[index];
            while (pe != NULL){
                if (pe->key == key)
                    break;
                pe0 = pe;
                pe = pe->next;
            }
            if (pe != NULL)
                pe->value += value;
            else{
                pe = new PairEntry(key, value, value1);
                ++_size;
                pe0->next = pe;
            }
        }
    } //add
    bool lookup(void* key, int *value_ptr, void** value1_ptr) {
        short index = hash(key);
        *value_ptr  = 0;
        *value1_ptr = NULL;
        if (buckets[index] == NULL) {
            return false;
        } else {
            PairEntry *pe = buckets[index];
            while (pe != NULL) {
                if (pe->key == key) {
                    break;
                }
                pe = pe->next;
            }
            if (pe != NULL) {
                *value_ptr  = pe->value;
                *value1_ptr = pe->value1;
                return true;
            } else {
                return false;
            }
        }
    } //lookup
    int* lookup_or_add(void* key, int init_value, void* init_value1)
    {
        short index = hash(key);
        if (!buckets[index]) {
            PairEntry *pe = new PairEntry(key, init_value, init_value1);
            ++_size;
            buckets[index] = pe;
            return &pe->value;
        } else {
            PairEntry *pe0 = NULL, *pe = buckets[index];
            while (pe) {
                if (pe->key == key)
                    break;
                pe0 = pe;
                pe = pe->next;
            }
            if (!pe) {
                pe = new PairEntry(key, init_value, init_value1);
                ++_size;
                pe0->next = pe;
            }
            return &pe->value;
        }
    }
    class Iterator{
    public:
        Iterator(SimpleHashtable *owner_):
            owner(owner_), cursor(NULL), curidx(-1){
        }
        PairEntry* next(){
            if (cursor == NULL){
                ++curidx;
                for (; curidx < owner->bkt_count; curidx++)
                    if (owner->buckets[curidx] != NULL)
                        break;
                if (curidx == owner->bkt_count)
                    return NULL;
                cursor = owner->buckets[curidx];
            }
            PairEntry* ret = cursor;
            cursor = cursor->next;
            return ret;
        }
        void reset() {
            cursor = NULL;
            curidx = -1;
        }
    private:
        SimpleHashtable *owner;
        PairEntry *cursor;
        int curidx;
    };
    friend class Iterator;
    Iterator *iterator(){
        if (iter == NULL)
            iter = new Iterator(this);
        return iter;
    }
    int size() { return _size; }
private:
    PairEntry** buckets;
    int bkt_count;
    int _size;
    Iterator *iter;
};

inline void swap(void*& a, void*& b)
{
    void *t = a;
    a = b;
    b = t;
}

inline void quick_sort(PairEntry* _list[], int left, int right)
{
    int pivot, left_arrow, right_arrow;

    left_arrow = left;
    right_arrow = right;
    pivot = _list[(left + right)/2]->value;

    do
    {
        while (_list[right_arrow]->value > pivot)
            right_arrow--;
        while (_list[left_arrow]->value < pivot)
            left_arrow++;
        if (left_arrow <= right_arrow)
        {
            swap((void*&)_list[left_arrow], (void*&)_list[right_arrow]);
            left_arrow++;
            right_arrow--;
        }
    }
    while (right_arrow >= left_arrow);

    if (left < right_arrow)
        quick_sort(_list, left, right_arrow);
    if (left_arrow < right)
        quick_sort(_list, left_arrow, right);
}

inline bool dump(SimpleHashtable &sht, PairEntry **buffer, int &len, int threshold)
{
    if (sht.size() > len) return false;
    SimpleHashtable::Iterator *iter = sht.iterator();  
    iter->reset();
    PairEntry *pe;                                                  
    len = 0;
    while((pe = iter->next()) != NULL){   
        if (pe->value < threshold)continue;
        buffer[len++] = pe;
    }
    return true;
}

#endif
