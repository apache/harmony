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

#include "MemoryAttribute.h"
#include "Type.h"

namespace Jitrino {

//
//  Information about memory contexts
//  {tag, tagName, canBeWrittenByJittedCode, isKilledByCalls, idKind, StorageClass, maybeTypeAliased}
//  maybeTypeAliased means that managed pointers with different type can refer to the same
//  memory location with this context
//
MemoryAttribute::Context::Info MemoryAttribute::Context::info[] = {
    {NoContext,         "NoTag",            true,  true,  NoId,     AnyStorageClass, false},
    {AnyContext,        "AnyTag",           true,  true,  NoId,     AnyStorageClass, true},        
    {ObjectField,       "ObjectField",      true,  true,  FieldId,  HeapStorage,     false},
    {ArrayLength,       "ArrayLength",      false, false, NoId,     HeapStorage,     false},
    {ArrayElement,      "ArrayElement",     true,  true,  NoId,     HeapStorage,     false},
    {VtableAddr,        "VtableAddr",       false, false, NoId,     HeapStorage,     false},
    {ObjectLock,        "ObjectLock",       true,  true,  NoId,     HeapStorage,     true},
    {Unboxed,           "Unboxed",          true,  true,  NoId,     HeapStorage,     true},
    {StackLocation,     "StackLoc",         true,  true,  StackId,  StackStorage,    true},
    {StackIncomingArg,  "StackInArg",       true,  true,  StackId,  StackStorage,    true},
    {StaticMethodAddr,  "StaticMethodAddr", false, true,  MethodId, VmDataStorage,   false},
    {VirtualMethodAddr, "VirtMethodAddr",   false, true,  MethodId, VmDataStorage,   false},
    {StringAddr,        "StringAddr",       false, true,  NoId,     VmDataStorage,   false},
    {StaticField,       "StaticField",      true,  true,  FieldId,  VmDataStorage,   false},
    {JitConstant,       "JitConstant",      false, false, NoId,     JitDataStorage,  false},
    {ProfileCounter,    "ProfileCounter",   true,  true,  CounterId,JitDataStorage,  false},
    {ObjectFieldOffset, "ObjectFieldOffset", false,  false,  FieldId, HeapStorage,     false},
    {ArrayLengthOffset, "ArrayLengthOffset", false, false, NoId,      HeapStorage,     false},
    {ArrayElementOffset, "ArrayElementOffset",false, false, NoId,     HeapStorage,     false},
};

//
//  Checks if two contexts can refer to the same memory location
//
bool MemoryAttribute::Context::maybeAliased(const MemoryAttribute::Context& mc) const {
    //
    //  Check tags
    //
    assert(tag != NoContext && mc.tag != NoContext);
    if (tag == AnyContext || mc.tag == AnyContext)
        return true;
    if (tag != mc.tag)
        return false;
    //
    //  Check ids. 
    //
    assert(tag == mc.tag);
    if (hasUnknownId() || mc.hasUnknownId())
        return true;
    //
    //  For any context except for stack locations context are aliased if and only if
    //  the ids are the same 
    //
    if (tag != StackLocation) {
        assert(size == 0 && mc.size == 0);
        return getId() == mc.getId();
    }
    //
    //  For stack locations id is stack offset. We need to check that
    //  two stack areas do not overlap
    //
    assert(size != 0 && mc.size != 0);
    U_32 offset = id.stackOffset;
    U_32 mcOffset = mc.id.stackOffset;
    return (offset == mcOffset) ||
           (offset > mcOffset && offset < mcOffset + mc.size) ||
           (offset < mcOffset && offset + size > mcOffset);
}

//
//  Unions this context with another one
//
void MemoryAttribute::Context::unionWith(MemoryAttribute::Context& mc) {
    if (mc.tag == NoContext)
        return;
    if (tag == NoContext) {
        tag = mc.tag; 
        id = mc.id;
        size = mc.size;
    }
    else if (tag != mc.tag) {
        tag = AnyContext;
        id.all = UnknownMemoryContextId;
        size = 0;
    }
    else {
        unionIds(mc.id.all);
        unionSizes(mc.size);
    }
}

//
//  Get 32-bit id
//
U_32 MemoryAttribute::Context::getId() const {
    switch (getIdKind()) {
    case StackId: return id.stackOffset;
    case FieldId: return id.fieldDesc->getId(); 
    case MethodId: return id.methodDesc->getId();
    case CounterId: return id.counterId;
    case NoId: return (U_32)id.all;
    default:
        assert(0);
    }
    return 0;
}

//
//  Print context id
//
void MemoryAttribute::Context::printId(::std::ostream& os) const {
    IdKind idKind = getIdKind();
    if (idKind == NoId)
        return;
    if (id.all == UnknownMemoryContextId) 
        os << ":unknown_id";
    else {
        switch (idKind) {
        case StackId: os << ":" << (int)id.stackOffset; 
            break;
        case FieldId: os << ":" << id.fieldDesc->getParentType()->getName() <<
                          "." << id.fieldDesc->getName(); 
            break;
        case MethodId: os << ":" << id.methodDesc->getParentType()->getName() << 
                           "." << id.methodDesc->getName();
            break;
        case CounterId: os << ":" << (int)id.counterId; 
            break;
        default:
            assert(0);
        }
    }
}

} //namespace Jitrino 
