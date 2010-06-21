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

#ifndef _MEMORY_ATTR_H
#define _MEMORY_ATTR_H
#include <iostream>
#include <assert.h>
#include "open/types.h"
#include "VMInterface.h"

namespace Jitrino {

#define UnknownMemoryContextId ((uint64)-1)

//
//  Attributes of memory locations
//
class MemoryAttribute {
public:
    //
    //  Storage class
    //
    enum StorageClass {
        HeapStorage     = 0x01,
        StackStorage    = 0x02,
        JitDataStorage  = 0x04,
        VmDataStorage   = 0x08,
        AnyStorageClass = 0x0F
    };
    //
    //  Context
    //
    class Context {
    public:
        enum Tag {
            NoContext           = 0x00, // cannot be used in any context
            AnyContext          = 0x01, // can be used in any context
            ObjectField         = 0x02,
            ArrayLength         = 0x03,
            ArrayElement        = 0x04,
            VtableAddr          = 0x05, 
            ObjectLock          = 0x06,
            Unboxed             = 0x07, // address of un-boxed value type inside the boxed value type
            StackLocation       = 0x08,
            StackIncomingArg    = 0x09, 
            StaticMethodAddr    = 0x0A,
            VirtualMethodAddr   = 0x0B,
            StringAddr          = 0x0C,
            StaticField         = 0x0D,
            JitConstant         = 0x0E,
            ProfileCounter      = 0x0F,
            ObjectFieldOffset   = 0x10, // not a real address, must be added to an object
            ArrayLengthOffset   = 0x11, // not a real address
            ArrayElementOffset  = 0x12, // not a real address
            NumTags             = 0x13,
        };
    private:
        // Kind of id
        enum IdKind {
            NoId,
            StackId,
            FieldId,
            MethodId,
            CounterId
        };
    public:
        //
        //  Default constructor
        //
        Context() : tag(NoContext), size(0) {
            id.all = UnknownMemoryContextId;
        }
        //
        //  Copy constructor
        //
        Context(const Context & mc) : tag(mc.tag), id(mc.id), size(mc.size) {
        }
        //
        //  Checks if memory location used in this context can be written by JIT'ed code
        //
        static bool isReadOnly(Tag tag) {
            assert(info[tag].tag == tag);
            return !info[tag].canBeWrittenByJittedCode;
        }
        bool    isReadOnly() const {return isReadOnly(tag);}
        //
        //  Checks if memory location used in this context can be written inside calls
        //
        static bool    isKilledByCalls(Tag tag) {
            assert(info[tag].tag == tag);
            return info[tag].killedByCalls;
        }
        bool    isKilledByCalls() const {return isKilledByCalls(tag);}
        //
        //  Checks if managed pointers of different types can refer to the same memory location
        //  that has this context.
        //
        static bool    maybeTypeAliased(Tag tag) {
            assert(info[tag].tag == tag);
            return info[tag].maybeTypeAliased;
        }
        bool    maybeTypeAliased() const {return maybeTypeAliased(tag);}
        //
        //  Storage class queries
        //
        bool    isInHeap() const {return getStorageClass() == HeapStorage;}
        bool    isOnStack() const {return getStorageClass() == StackStorage;}
        bool    maybeInHeap() const {return (getStorageClass() & HeapStorage) != 0;}
        bool    maybeOnStack() const {return (getStorageClass() & StackStorage) != 0;}
        //
        //  Tag queries
        //
        bool    isValid() const {return tag != NoContext;}
        bool    isKnown() const {return tag != AnyContext;}
        //
        //  Tag ids are sequential numbers for tags that allow for easy implementation
        //  of associative containers indexed by tags.
        //  This might be useful if tags become non sequential, e.g., bit positions
        //  so that we can encode multiple tag usage for managed pointers in CLI.
        //
        static U_32  getNumTagIds() {return NumTags;}
        static U_32  getTagId(Tag tag) {return tag;}
        U_32         getTagId() const {return getTagId(tag);}
        static Tag     mapIdToTag(U_32 _id) {return (Tag)_id;}
        //
        //  Checks if memory context is exact
        //
        bool isExact() const {
            assert(info[tag].tag == tag);
            return tag > AnyContext && 
                (info[tag].idKind == NoId || id.all != UnknownMemoryContextId);
        }
        //
        //  Checks if two contexts can refer to the same memory location
        //
        bool maybeAliased(const Context& mc) const;
        //
        //  Checks if two contexts are the same
        //
        bool isEqual(Context& mc) const {
            return tag == mc.tag && getId() == mc.getId() && size == mc.size;
        }
        //
        //  Unions this context with another one
        //
        void unionWith(Context& mc);
        //
        //  Returns tag of the memory context
        //
        Tag getTag() const {return tag;}
        //
        //  Returns storage class of memory locations used in this context
        //
        StorageClass getStorageClass() const {
            assert(info[tag].tag == tag);
            return info[tag].storageClass;
        }
        //
        //  Checks if context can refer to a certain storage class
        //
        bool canReferToStorageClass(StorageClass sc) const {
            return (getStorageClass() & sc) != 0;
        }
        //
        //  Get various ids
        //
        U_32 getStackOffset() const {
            assert(getIdKind() == StackId);
            return id.stackOffset;
        }
        FieldDesc * getFieldDesc() const {
            assert(getIdKind() == FieldId);
            return id.fieldDesc;
        }
        MethodDesc * getMethodDesc() const {
            assert(getIdKind() == MethodId);
            return id.methodDesc;
        }
        U_32     getProfileCounterId() const {
            assert(getIdKind() == CounterId);
            return id.counterId;
        }
        //
        //  Prints tag
        //
    void print(::std::ostream& os) const {
            assert(info[tag].tag == tag);
            os << info[tag].name;
            printId(os);
        }
    protected:
        //
        //  Information about memory contexts
        //
        struct Info {
            Tag          tag;
            const char * name;
            bool         canBeWrittenByJittedCode;
            bool         killedByCalls;
            IdKind       idKind;
            StorageClass storageClass;
            bool         maybeTypeAliased;
        };
    private:
        Context(Tag t, U_32 _size = 0)
            : tag(t), size((U_8)_size) {
            id.all = UnknownMemoryContextId;
            assert(_size <= 0xff);
        }
        void unionIds(uint64 id1) {
            if (id.all != id1)
                id.all = UnknownMemoryContextId;
        }
        void unionSizes(U_8 size1) {
            if (size < size1)
                size = size1;
        }
        IdKind  getIdKind() const {
            assert(info[tag].tag == tag);
            return info[tag].idKind;
        }
        bool    hasUnknownId() const {return id.all == UnknownMemoryContextId;}
        U_32  getId() const;
    void    printId(::std::ostream& os) const;
        //
        // Fields
        //
        Tag      tag : 8;
        union IdUnion {
            U_32       stackOffset;
            FieldDesc *  fieldDesc;
            MethodDesc * methodDesc;
            U_32       counterId;
            uint64       all;
        } id;
        U_8    size; // size of accessed memory in bytes; used for stack locations
        static Info info[];
        friend class MemoryAttributeManager;
    };  // end class Context
};

//
//  Memory attribute manager
//
class MemoryAttributeManager {
public:
    //
    //  Factory methods to create various memory contexts
    //
    MemoryAttribute::Context getUnknownContext() {
        return MemoryAttribute::Context(MemoryAttribute::Context::AnyContext);
    }
    MemoryAttribute::Context getObjectFieldContext(FieldDesc * fieldDesc) {
        MemoryAttribute::Context c(MemoryAttribute::Context::ObjectField);
        c.id.fieldDesc = fieldDesc;
        return c;
    }
    MemoryAttribute::Context getArrayLengthContext() {
        return MemoryAttribute::Context(MemoryAttribute::Context::ArrayLength);
    }
    MemoryAttribute::Context getArrayElementContext() {
        return MemoryAttribute::Context(MemoryAttribute::Context::ArrayElement);
    }
    MemoryAttribute::Context getVtableAddrContext() {
        return MemoryAttribute::Context(MemoryAttribute::Context::VtableAddr);
    }
    MemoryAttribute::Context getObjectLockContext() {
        return MemoryAttribute::Context(MemoryAttribute::Context::ObjectLock);
    }
    MemoryAttribute::Context getUnboxedContext() {
        return MemoryAttribute::Context(MemoryAttribute::Context::Unboxed);
    }
    MemoryAttribute::Context getObjectFieldOffsetContext(FieldDesc * fieldDesc) {
        MemoryAttribute::Context c(MemoryAttribute::Context::ObjectFieldOffset);
        c.id.fieldDesc = fieldDesc;
        return c;
    }
    MemoryAttribute::Context getArrayLengthOffsetContext() {
        return MemoryAttribute::Context(MemoryAttribute::Context::ArrayLengthOffset);
    }
    MemoryAttribute::Context getArrayElementOffsetContext() {
        return MemoryAttribute::Context(MemoryAttribute::Context::ArrayElementOffset);
    }
    MemoryAttribute::Context getAddressContextFromOffsetContext(MemoryAttribute::Context &offsetContext) {
        switch (offsetContext.tag) {
        case MemoryAttribute::Context::ObjectFieldOffset:
            return getObjectFieldContext(offsetContext.getFieldDesc());
        case MemoryAttribute::Context::ArrayLengthOffset:
            return getArrayLengthContext();
        case MemoryAttribute::Context::ArrayElementOffset:
            return getArrayElementContext();
        default:
            break;
        }
        assert(0);
        return getUnknownContext();
    }
    //
    // Get memory context for accessing stack memory of given size at given offset
    //
    MemoryAttribute::Context getStackContext(U_32 offset, U_32 size) {
        assert(size <= 0xff);
        MemoryAttribute::Context c(MemoryAttribute::Context::StackLocation,
                                   (U_8)size);
        c.id.stackOffset = offset;
        return c;
    }
    //
    //  Get memory context for accessing a stack incoming argument at given slot.
    //  This is IPF specific as incoming arguments are in the previous frame
    //  and their stack offset is not known till the final register allocation.
    //
    MemoryAttribute::Context getStackIncomingArgContext(U_32 offset) {
        MemoryAttribute::Context c(MemoryAttribute::Context::StackIncomingArg);
        c.id.stackOffset = offset;
        return c;
    }
    MemoryAttribute::Context getStaticMethodAddrContext(MethodDesc * desc) {
        MemoryAttribute::Context c(MemoryAttribute::Context::StaticMethodAddr);
        c.id.methodDesc = desc;
        return c;
    }
    MemoryAttribute::Context getVirtualMethodAddrContext(MethodDesc * desc) {
        MemoryAttribute::Context c(MemoryAttribute::Context::VirtualMethodAddr);
        c.id.methodDesc = desc;
        return c;
    }
    MemoryAttribute::Context getStringAddrContext() {
        return MemoryAttribute::Context(MemoryAttribute::Context::StringAddr);
    }
    MemoryAttribute::Context getStaticFieldContext(FieldDesc * fieldDesc) {
        MemoryAttribute::Context c(MemoryAttribute::Context::StaticField);
        c.id.fieldDesc = fieldDesc;
        return c;
    }
    MemoryAttribute::Context getJitConstantContext() {
        return MemoryAttribute::Context(MemoryAttribute::Context::JitConstant);
    }
    MemoryAttribute::Context getProfileCounterContext(U_32 counterId) {
        MemoryAttribute::Context c(MemoryAttribute::Context::ProfileCounter);
        c.id.counterId = counterId;
        return c;
    }
    //
    //  Returns a memory context at (offset, size) from the given context
    //
    MemoryAttribute::Context findContextAtOffset(MemoryAttribute::Context& mc,
                                                 U_32                    _offset,
                                                 U_32                    _size) {
        MemoryAttribute::Context context(mc);
        if (context.tag == MemoryAttribute::Context::StackLocation ||
            context.tag == MemoryAttribute::Context::StackIncomingArg) {
            assert(_size <= 0xff);
            context.size = (U_8)_size;
            if (context.id.all != UnknownMemoryContextId)
                context.id.stackOffset += _offset;
        }
        return context;
    }
};

} //namespace Jitrino 

#endif
