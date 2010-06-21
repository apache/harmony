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

#ifndef _MAPSET_H_
#define _MAPSET_H_

#include <assert.h>
#include "open/types.h"
#include "MemoryManager.h"
#include "Stl.h"

namespace Jitrino {

//
//  Set that is based on an STL map. Set elements of type ELEM are indexed
//  by the key of type KEY. Map elements are kept in a RedBlack tree and hence
//  complexity of iterators is log(N) where N is the number of elements.
//
template<class KEY, class ELEM> 
class MapSet : public StlMap<KEY, ELEM> {
public:
    typedef StlMap<KEY,ELEM> Map;
#ifdef PLATFORM_POSIX
    typedef typename Map::const_iterator const_iterator;
    typedef typename Map::iterator iterator;
    typedef typename Map::value_type value_type;
#endif

    //
    // Constructor
    //
    MapSet(MemoryManager& memManager) : Map(memManager) {
    }
    //
    //  Copy constructor
    //
    MapSet(MemoryManager& memManager, const MapSet<KEY,ELEM> &set)
        : Map(memManager) {
        const_iterator iter = set.begin(),
                        end = set.end();
        for (; iter != end; iter++) {
            value_type val = *iter;
            insert(val.first, val.second);
        }
    }
    //
    //  Returns number of elements in the set
    //
    U_32 getSize() const {return (U_32) size();}
    //
    //  Returns true if set contains the element, false otherwise
    //
    bool contains(KEY key) const {
        return find(key) != end();
    }
    //
    //  Inserts element into the set
    //
    void insert(KEY key, ELEM elem) {(*this)[key] = elem;}
    //
    //  Removes element from the set
    //
    void remove(KEY key) {erase(key);}
    

    //
    //  Checks if set is empty
    //
    bool isEmpty() {return empty();}
    //
    //  Copies another set
    //
    void copyFrom(const MapSet<KEY,ELEM>& set) {
        if (this != &set) {
            clear();
            const_iterator iter = set.begin(),
                     end = set.end();
            for (; iter != end; iter++)
                insert(iter->first, iter->second);
        }
    }
    //
    //  Unions with another set
    //
    void unionWith(const MapSet<KEY,ELEM>& set) {
        const_iterator iter = set.begin(),
                              setend  = set.end();
        for (; iter != setend; iter++) {
            iterator iter2 = find(iter->first);
            assert((iter2 == end()) || (iter2->second == iter->second));
            if (iter2 == end())
                insert(iter->first, iter->second);
        }
    }
    //
    //  Intersects with another set
    //
    void intersectWith(const MapSet<KEY,ELEM>& set) {
        //
        // Find elements that are in this set bat are not in 'set' and
        // write them into 'removeList'.
        //
        MemoryManager memManager("MapSet::intersect.memManager");
        KEY * removeList = (KEY *)memManager.alloc(sizeof(KEY) * size());
        U_32 removeListSize = 0;
        const_iterator iter = begin(),
                       setend  = end();
        for (; iter != setend; iter++) {
            if (set.find(iter->first) == set.end())
                removeList[removeListSize++] = iter->first;
        }
        //
        //  Remove elements that are in removeList
        //
        for (U_32 i = 0; i < removeListSize; i++)
            erase(removeList[i]);
    }
    //
    //  Subtracts another set
    //
    void subtract(const MapSet<KEY,ELEM>& set) {
        const_iterator iter = set.begin(),
                              setend  = set.end();
        for (; iter != setend; iter++) 
            erase(iter->first);
    }
    //
    //  Checks if the set is subset of another set
    //
    bool isSubset(const MapSet<KEY,ELEM>& set) const {
        const_iterator iter = begin(),
                              setend  = end();
        for (; iter != setend; iter++) {
            if (set.find(iter->first) == set.end())
                return false;
        }
        return true;
    }
    //
    //  Checks if the set is disjoint to another set
    //
    bool isDisjoint(const MapSet<KEY,ELEM>& set) const {
        const_iterator iter = begin();
        for (; iter != end(); iter++) {
            if (set.find(iter->first) != set.end())
                return false;
        }
        return true;
    }
    //
    //  Checks if the set is equal to another set
    //
    bool isEqual(const MapSet<KEY,ELEM>& set) {
        return isSubset(set) && set.isSubset(*this);
    }


private:
};

} //namespace Jitrino 

#endif // _MAPSET_H_
