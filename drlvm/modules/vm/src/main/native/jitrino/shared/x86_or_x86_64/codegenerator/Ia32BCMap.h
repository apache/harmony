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
* @author Intel, Vitaly N. Chaiko
*/

#ifndef _IA32_BC_MAP_H_
#define _IA32_BC_MAP_H_

#include "Stl.h"
#include "MemoryManager.h"
#include "Ia32IRManager.h"

namespace Jitrino {

namespace Ia32 {

typedef StlHashMap<U_32, uint16>      BCByNCMap;
/**
   * Bcmap is simple storage with precise mapping between native offset in a method to
   * byte code, i.e. if there is no byte code for certain native offset in a method then
   * invalid value is returned.
   */

class BcMap {
public:
    BcMap(MemoryManager& memMgr) : theMap(memMgr) {}

    U_32 getByteSize() const {
        return 4 /*size*/+(U_32)theMap.size() * (4 + 2/*native offset + bc offset*/);
    }

    void write(U_8* image) {
        *((U_32*)image)=(U_32)theMap.size();
        U_32 imageOffset = 4;
        for (BCByNCMap::const_iterator it = theMap.begin(), end = theMap.end(); it!=end; it++) {
            U_32 nativeOffset = it->first;
            uint16 bcOffset = it->second;
            *((U_32*)(image + imageOffset)) = nativeOffset;
            imageOffset+=4;
            *((uint16*)(image + imageOffset)) = bcOffset;
            imageOffset+=2;
        }
        return;
    }

    POINTER_SIZE_INT readByteSize(const U_8* image) const {
        U_32 sizeOfMap = *(U_32*)image;;
        return 4 + sizeOfMap * (4+2);
    }

    
    void setEntry(U_32 key, uint16 value) {
        theMap[key] =  value;
    }

    static uint16 get_bc_offset_for_native_offset(U_32 ncOffset, U_8* image) {
        U_32 mapSize = *(U_32*)image; //read map size
        U_32 imageOffset=4;
        for (U_32 i = 0; i < mapSize; i++) {
            U_32 nativeOffset = *(U_32*)(image+imageOffset);
            imageOffset+=4;
            uint16 bcOffset = *(uint16*)(image+imageOffset);
            imageOffset+=2;
            if (nativeOffset == ncOffset) {
                return bcOffset;
            }
        }
        return ILLEGAL_BC_MAPPING_VALUE;
    }

    static U_32 get_native_offset_for_bc_offset(uint16 bcOff, U_8* image) {
        U_32 mapSize = *(U_32*)image; //read map size
        U_32 imageOffset=4;
        for (U_32 i = 0; i < mapSize; i++) {
            U_32 nativeOffset = *(U_32*)(image+imageOffset);
            imageOffset+=4;
            uint16 bcOffset = *(uint16*)(image+imageOffset);
            imageOffset+=2;
            if (bcOffset == bcOff) {
                return nativeOffset;
            }
        }
        return MAX_UINT32;
    }

private:
    BCByNCMap theMap;
};

}} //namespace

#endif /* _IA32_BC_MAP_H_ */
