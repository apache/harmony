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

#ifndef _OPNDMAP_H
#define _OPNDMAP_H

#include <iostream>
#include "Stl.h"

namespace Jitrino {
class Opnd;

template <typename FromType, typename ToType>
class SparseScopedMap {
    typedef StlHashMap<FromType, ToType> MapBase;
    MapBase theMap;
    StlVector<FromType> theList; // 0 separates scopes;
    // priorValues has previous value for each non-0 elt in theList:
    StlVector<ToType> priorValues;
    FromType zero;
    U_32 resize_when;
    U_32 resize_by;
public:
    SparseScopedMap(MemoryManager& mm) :
        theMap(mm), 
        theList(mm), 
        priorValues(mm),
        zero(0),
        resize_when(4),
        resize_by(7)
    {};
    SparseScopedMap(size_t n,  
                    MemoryManager& mm,
                    U_32 init_factor=1,
                    U_32 resize_factor=4,
                    U_32 resize_to=7) :
        theMap(mm), 
        theList(mm),
        priorValues(mm),
        zero(0),
        resize_when(resize_factor),
        resize_by(resize_to)
    {
        theList.reserve(n);
        priorValues.reserve(n);
    };
    
    ToType lookup(FromType o) {
        typename MapBase::iterator found = theMap.find(o);
        return (found != theMap.end()) ? (*found).second : ToType(0);
    };
    void insert(FromType org, ToType to) {
        ToType priorValue = theMap[org];
        theMap[org] = to;
        theList.push_back(org);
        priorValues.push_back(priorValue);
    };
    void enter_scope() {
        theList.push_back(zero);
    };
    void exit_scope() {
        FromType o;
        do {
            o = theList.back();
            theList.pop_back();
            if (o != zero) {
                ToType priorValue = priorValues.back();
                priorValues.pop_back();
                if (priorValue != ToType(0)) {
                    theMap[o] = priorValue;
                } else {
                    theMap.erase(o);
                }
            }
        } while (o != zero);
    };
};

//
// Map from original Opnd to Opnd
//

    
class SparseOpndMap : public SparseScopedMap<Opnd *, Opnd *> {
public:
    SparseOpndMap(MemoryManager& mm) :
        SparseScopedMap<Opnd *, Opnd *>(mm) {};
    SparseOpndMap(size_t n, MemoryManager& mm,
                  U_32 init_factor, U_32 resize_factor, U_32 resize_to) :
        SparseScopedMap<Opnd *, Opnd *>(n, mm, init_factor, resize_factor,
                                        resize_to) {};
    Opnd *lookupTillEnd(Opnd *o) {
        Opnd *found = lookup(o);
        if (NULL == found) {
            return NULL;
        }
        Opnd *foundMore;
        while (NULL != (foundMore = lookup(found))) {
            found = foundMore;
        }
        return found;
    }
};


} //namespace Jitrino 

#endif // _OPNDMAP_H
