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

#include "RuntimeInterface.h"

namespace Jitrino {

InlineInfoMap::Entry* InlineInfoMap::newEntry(Entry* parent, Method_Handle mh, uint16 bcOffset) {
    Entry* e = new (memManager) Entry(parent, bcOffset, mh);
    return e;
}

void InlineInfoMap::registerEntry(Entry* e, U_32 nativeOffs) {
    entryByOffset[nativeOffs] = e;
    for (Entry* current = e; current!=NULL; current = current->parentEntry) {
        if (std::find(entries.begin(), entries.end(), current)!=entries.end()) {
            assert(current!=e); //possible if inlined method has multiple entries, skip this method marker.
        } else {
            entries.push_back(current);
        }
    }
}

static U_32 getIndexSize(size_t nEntriesInIndex) {
    return (U_32)(2 * nEntriesInIndex * sizeof(U_32) + sizeof(U_32)); //zero ending list of [nativeOffset, entryOffsetInImage] pairs
}

U_32
InlineInfoMap::getImageSize() const {
    if (isEmpty()) {
        return sizeof(U_32);
    }
    return getIndexSize(entryByOffset.size())   //index size
          + (U_32)(entries.size() * sizeof(Entry)); //all entries size;
}


/* serialized inline_info layout
nativeOffset1 -> offset to InlineInfoMap::Entry in the image with max inline depth   (32bit + 32bit)
nativeOffset2 -> offset to InlineInfoMap::Entry in the image with max inline depth
nativeOffset3 -> offset to InlineInfoMap::Entry in the image with max inline depth
...
0 -> 0
InlineInfoMap::Entry1
InlineInfoMap::Entry2
...
*/
void
InlineInfoMap::write(InlineInfoPtr image)
{
    if (isEmpty()) {
        *(U_32*)image=0;
        return;
    }

    //write all entries first;
    Entry*  entriesInImage = (Entry*)((char*)image + getIndexSize(entryByOffset.size()));
    Entry*  entriesPtr = entriesInImage; 
    for (StlVector<Entry*>::iterator it = entries.begin(), end = entries.end(); it != end; it++) {
        Entry* e = *it;
        *entriesPtr = *e;
        entriesPtr++;
    }
    assert(((char*)entriesPtr) == ((char*)image) + getImageSize());

    //now update parentEntry reference to written entries
    for (U_32 i=0; i < entries.size(); i++) {
        Entry* imageChild = entriesInImage + i;
        Entry* compileTimeParent = imageChild->parentEntry;
        if (compileTimeParent!=NULL) {
            size_t parentIdx = std::find(entries.begin(), entries.end(), compileTimeParent) - entries.begin();
            assert(parentIdx<entries.size());
            Entry* imageParent = entriesInImage + parentIdx;
            imageChild->parentEntry = imageParent;
        }
    }

    //now write index header
    U_32* header = (U_32*)image;
    for (StlMap<U_32, Entry*>::iterator it = entryByOffset.begin(), end = entryByOffset.end(); it!=end; it++) {
        U_32 nativeOffset = it->first;
        Entry* compileTimeEntry = it->second;
        size_t entryIdx = std::find(entries.begin(), entries.end(), compileTimeEntry) - entries.begin();
        assert(entryIdx<entries.size());
        Entry* imageEntry = entriesInImage + entryIdx;
        *header = nativeOffset;
        header++;
        *header = (U_32)((char*)imageEntry - (char*)image);
        header++;
    }
    *header = 0;
    header++;
    assert((char*)header == (char*)entriesInImage);
}


const InlineInfoMap::Entry* InlineInfoMap::getEntryWithMaxDepth(InlineInfoPtr ptr, U_32 nativeOffs) {
    U_32* header = (U_32*)ptr;
    while (*header!=0) {
        U_32 nativeOffset = *header;
        header++;
        U_32 entryOffset = *header;
        header++;
        if (nativeOffset == nativeOffs) {
            Entry* e = (Entry*)((char*)ptr + entryOffset);
            return e;
        }
    }
    return NULL;
}

const InlineInfoMap::Entry* InlineInfoMap::getEntry(InlineInfoPtr ptr, U_32 nativeOffs, U_32 inlineDepth) {
    const Entry* e = getEntryWithMaxDepth(ptr, nativeOffs);
    while (e!=NULL) {
        if (e->getInlineDepth() == inlineDepth) {
            return e;
        }
        e = e->parentEntry;
    }
    return NULL;
}

} //namespace Jitrino 
