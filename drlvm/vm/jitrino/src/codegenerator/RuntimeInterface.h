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

#ifndef _RTINTFC_H_
#define _RTINTFC_H_

#include "open/types.h"
#include "Type.h"
#include "Jitrino.h"
#include "VMInterface.h"
#include "Stl.h"

namespace Jitrino
{

/**
 * Class responsible for runtime operations the JIT (CG in particular) performs:
 * stack unwinding, root set enumeration, and code patching.
 */
class RuntimeInterface {
public:
    virtual ~RuntimeInterface() {}
    virtual void  unwindStack(MethodDesc* methodDesc, ::JitFrameContext* context, bool isFirst) = 0;

    virtual void  getGCRootSet(MethodDesc* methodDesc, GCInterface* gcInterface, 
        const ::JitFrameContext* context, bool isFirst) = 0;

    virtual void  fixHandlerContext(MethodDesc* methodDesc, ::JitFrameContext* context, bool isFirst) = 0;

    virtual void* getAddressOfThis(MethodDesc* methodDesc, const ::JitFrameContext* context, bool isFirst) = 0;
    virtual bool  isSOEArea(MethodDesc* methodDesc, const ::JitFrameContext* context, bool isFirst) {return false;}

    virtual bool getBcLocationForNative(MethodDesc* method, POINTER_SIZE_INT native_pc, uint16 *bc_pc) = 0;
    virtual bool getNativeLocationForBc(MethodDesc* method,  uint16 bc_pc, POINTER_SIZE_INT *native_pc) = 0;

    virtual bool  recompiledMethodEvent(MethodDesc * methodDesc, void * data) = 0;

    virtual U_32          getInlineDepth(InlineInfoPtr ptr, U_32 offset) { return 0; }
    virtual Method_Handle   getInlinedMethod(InlineInfoPtr ptr, U_32 offset, U_32 inline_depth) { return NULL; }
    virtual uint16   getInlinedBc(InlineInfoPtr ptr, U_32 offset, U_32 inline_depth)  = 0;

};

/**
 * Registry of bytecode<->native mapping for inlined methods within a compiled method.
 * Allows to record the mapping at compile time and extract/query it at runtime.
 */
class InlineInfoMap {
public:
    /*
     * Inline info for a single call site or instruction,
     * linked to a chain of inlined calls.
     */
    class Entry {
    public:
        Entry(Entry* parent, uint16 _bcOffset, Method_Handle _method) 
            : parentEntry(parent), bcOffset(_bcOffset), method(_method){}
        
        Entry* parentEntry;
        uint16  bcOffset;
        Method_Handle method;

        /**
         * Counts number of inlined calls in the chain, including this.
         */
        U_32 getInlineDepth() const { 
            return (parentEntry == 0) ? 1 : 1 + parentEntry->getInlineDepth(); 
        }
    };

    InlineInfoMap(MemoryManager& mm) : memManager(mm), entries(mm), entryByOffset(mm){}

    /**
     * Creates a loose entry, not registered at the map.
     */
    Entry* newEntry(Entry* parent, Method_Handle mh, uint16 bcOffset);

    /**
     * Records unique mapping of a top-level entry to the specified native offset.
     * Parent entries are added to a common list for future references.
     */
    void registerEntry(Entry* entry, U_32 nativeOff);

    bool isEmpty() const { return entries.empty();}
    U_32 getImageSize() const;
    void write(InlineInfoPtr output);

    static const Entry* getEntryWithMaxDepth(InlineInfoPtr ptr, U_32 nativeOffs);
    static const Entry* getEntry(InlineInfoPtr ptr, U_32 nativeOffs, U_32 inlineDepth);
private:
    MemoryManager& memManager;
    /** 
     * List of all entries w/o duplicates 
     */
    StlVector<Entry*> entries;
    /**
     * Unique mapping of native offset -> top-level entry
     */
    StlMap<U_32, Entry*> entryByOffset;
};

}
#endif // _RTINTFC_H_
