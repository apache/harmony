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

#ifndef _HASHSET_H_
#define _HASHSET_H_

#include <assert.h>
#include "open/types.h"
#include "MemoryManager.h"
#include "Stl.h"

namespace Jitrino {

//
//  Hash set that is based on an STL Hash map. Set elements of type ELEM
//  are indexed by key of type KEY.
//
template<class KEY, class ELEM> class HashSet {
public:
    typedef StlHashMap<KEY,ELEM>::iterator iterator;
    typedef StlHashMap<KEY,ELEM>::const_iterator const_iterator;
    //
    // Constructor
    //
    HashSet(MemoryManager& memManager) : map(memManager) {
    }
    //
    //  Copy constructor
    //
    HashSet(MemoryManager& memManager, const HashSet<KEY,ELEM> &set)
        : map(memManager) {
        const_iterator iter = set.map.begin(),
                        end = set.map.end();
        for (; iter != end; iter++)
            map.insert(*iter);
    }
    //
    //  Returns number of elements in the set
    //
    U_32 getSize() const {return (U_32)map.size();}
    //
    //  Returns true if set contains the element, false otherwise
    //
    bool contains(KEY key) const {
        return map.find(key) != map.end();
    }
    //
    //  Inserts element into the set
    //
    void insert(KEY key, ELEM elem) {map[key] = elem;}
    //
    //  Removes element from the set
    //
    void remove(KEY key) {map.erase(key);}
    //
    //  Removes all element from the set
    //
    void clear() {map.clear();}
    //
    //  Checks if set is empty
    //
    bool isEmpty() {return map.empty();}
    //
    //  Copies another set
    //
    void copyFrom(const HashSet<KEY,ELEM>& set) {
        if (this != &set) {
            map.clear();
            const_iterator iter = set.map.begin(),
                     end = set.map.end();
            for (; iter != end; iter++)
                map.insert(*iter);
        }
    }
    //
    //  Unions with another set
    //
    void unionWith(const HashSet<KEY,ELEM>& set) {
        const_iterator iter = set.map.begin(),
                              end  = set.map.end();
        for (; iter != end; iter++) {
            iterator iter2 = map.find(iter->first);
            assert((iter2 == map.end()) || (iter2->second == iter->second));
            map.insert(*iter);
        }
    }
    //
    //  Intersects with another set
    //
    void intersectWith(const HashSet<KEY,ELEM>& set) {
        //
        // Find elements that are in this set but are not in 'set' and
        // write them into 'removeList'.
        //
        MemoryManager memManager("HashSet::intersect.memManager");
        KEY * removeList = (KEY *)memManager.alloc(sizeof(KEY) * map.size());
        U_32 removeListSize = 0;
        const_iterator iter = map.begin(),
                       end  = map.end();
        for (; iter != end; iter++) {
            if (set.map.find(iter->first) == set.map.end())
                removeList[removeListSize++] = iter->first;
        }
        //
        //  Remove elements that are in removeList
        //
        for (U_32 i = 0; i < removeListSize; i++)
            map.erase(removeList[i]);
    }
    //
    //  Subtracts another set
    //
    void subtract(const HashSet<KEY,ELEM>& set) {
        const_iterator iter = set.map.begin(),
                              end  = set.map.end();
        for (; iter != end; iter++) 
            map.erase(iter->first);
    }
    //
    //  Checks if set is subset of another set
    //
    bool isSubset(const HashSet<KEY,ELEM>& set) const {
        const_iterator iter = map.begin(),
                              end  = map.end();
        for (; iter != end; iter++) {
            if (set.map.find(iter->first) == set.map.end())
                return false;
        }
        return true;
    }
    //
    //  Checks if set is disjoint to another set
    //
    bool isDisjoint(const HashSet<KEY,ELEM>& set) const {
        const_iterator iter = map.begin(),
                              end  = map.end();
        for (; iter != end; iter++) {
            if (set.map.find(iter->first) != set.map.end())
                return false;
        }
        return true;
    }
    //
    //  Checks if set is equal to another set
    //
    bool isEqual(const HashSet<KEY,ELEM>& set) {
        return isSubset(set) && set.isSubset(*this);
    }
    //
    //  Returns an iterator to the first element in the set
    //
    iterator begin() {return map.begin();}
    //
    //  Returns an iterator that points just beyond last element of the set
    //
    iterator end() {return map.end();}
private:
    StlHashMap<U_32,ELEM> map;
};

} //namespace Jitrino 

#endif // _HASHSET_H_
