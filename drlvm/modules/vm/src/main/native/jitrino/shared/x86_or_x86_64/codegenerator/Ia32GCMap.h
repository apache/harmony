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
 */

#ifndef _IA32_GC_MAP_H_
#define _IA32_GC_MAP_H_

#include "Stl.h"
#include "MemoryManager.h"
#include "Ia32IRManager.h"
#include "Ia32StackInfo.h"
#include "Ia32BCMap.h"

#ifdef _DEBUG
#define GCMAP_TRACK_IDS
#endif 

namespace Jitrino
{
namespace Ia32 {

    class GCSafePointsInfo;
    class GCSafePoint;
    class GCSafePointOpnd;
    class StackInfo;

    class GCMap {
        typedef StlVector<GCSafePoint*> GCSafePoints;
    public:
        GCMap(MemoryManager& mm);

        void registerInsts(IRManager& irm);

        POINTER_SIZE_INT getByteSize() const ;
        static POINTER_SIZE_INT readByteSize(const U_8* input);
        void write(U_8*);
        const GCSafePointsInfo* getGCSafePointsInfo() const {return offsetsInfo;}
        
        static const POINTER_SIZE_INT* findGCSafePointStart(const POINTER_SIZE_INT* image, POINTER_SIZE_INT ip);
        static void checkObject(TypeManager& tm, const void* p);

    private:
        void processBasicBlock(IRManager& irm, const Node* block);
        void registerGCSafePoint(IRManager& irm, const BitSet& ls, Inst* inst);
        void registerHardwareExceptionPoint(Inst* inst);
        bool isHardwareExceptionPoint(const Inst* inst) const;

        
        
        MemoryManager& mm;
        GCSafePoints gcSafePoints;
        GCSafePointsInfo* offsetsInfo;

    };

    class GCSafePoint {
        friend class GCMap;
        typedef StlVector<GCSafePointOpnd*> GCOpnds;
    public:
        GCSafePoint(MemoryManager& mm, POINTER_SIZE_INT _ip):gcOpnds(mm), ip(_ip) {
#ifdef GCMAP_TRACK_IDS
            instId = 0;
            hardwareExceptionPoint = false;
#endif
        }
        GCSafePoint(MemoryManager& mm, const POINTER_SIZE_INT* image);

        POINTER_SIZE_INT getUint32Size() const;
        void write(POINTER_SIZE_INT* image) const;
        U_32 getNumOpnds() const {return (U_32)gcOpnds.size();}
        static POINTER_SIZE_INT getIP(const POINTER_SIZE_INT* image);

        void enumerate(GCInterface* gcInterface, const JitFrameContext* c, const StackInfo& stackInfo) const;
    
    private:
        //return address in memory where opnd value is saved
        POINTER_SIZE_INT getOpndSaveAddr(const JitFrameContext* ctx, const StackInfo& sInfo,const GCSafePointOpnd* gcOpnd) const;
        GCOpnds gcOpnds;
        POINTER_SIZE_INT ip;
#ifdef GCMAP_TRACK_IDS
        POINTER_SIZE_INT instId;
        bool hardwareExceptionPoint;
    public: 
        bool isHardwareExceptionPoint() const {return hardwareExceptionPoint;}
#endif 
    };

    class GCSafePointOpnd {
        friend class GCSafePoint;
        static const U_32 OBJ_MASK  = 0x1;
        static const U_32 REG_MASK  = 0x2;
#ifdef _EM64T_
        static const U_32 COMPRESSED_MASK  = 0x4;
#endif

#ifdef GCMAP_TRACK_IDS
        // flags + val + mptrOffset + firstId
        static const U_32 IMAGE_SIZE_UINT32 = 4; //do not use sizeof due to the potential struct layout problems
#else 
        // flags + val + mptrOffset 
        static const POINTER_SIZE_INT IMAGE_SIZE_UINT32 = 3;
#endif 

    public:
        
#ifdef _EM64T_
        GCSafePointOpnd(bool isObject, bool isOnRegister, I_32 _val, I_32 _mptrOffset, bool isCompressed=false) : val(_val), mptrOffset(_mptrOffset) {
            flags = flags | (isCompressed ? COMPRESSED_MASK: 0);
#else
        GCSafePointOpnd(bool isObject, bool isOnRegister, I_32 _val, I_32 _mptrOffset) : val(_val), mptrOffset(_mptrOffset) {
#endif
            flags = isObject ? OBJ_MASK : 0;
            flags = flags | (isOnRegister ? REG_MASK: 0);
#ifdef GCMAP_TRACK_IDS
            firstId = 0;
#endif
        }
        
        bool isObject() const {return (flags & OBJ_MASK)!=0;}
        bool isMPtr() const  {return !isObject();}
        
        bool isOnRegister() const { return (flags & REG_MASK)!=0;}
        bool isOnStack() const {return !isOnRegister();}
        
#ifdef _EM64T_
        bool isCompressed() const { return (flags & COMPRESSED_MASK)!=0;}
#endif      
        RegName getRegName() const { assert(isOnRegister()); return RegName(val);}
        I_32 getDistFromInstESP() const { assert(isOnStack()); return val;}

        I_32 getMPtrOffset() const {return mptrOffset;}
        void getMPtrOffset(int newOffset) {mptrOffset = newOffset;}

#ifdef GCMAP_TRACK_IDS
        U_32 firstId;
#endif

    private:
        GCSafePointOpnd(U_32 _flags, I_32 _val, I_32 _mptrOffset) : flags(_flags), val(_val), mptrOffset(_mptrOffset) {}

        //first bit is location, second is type
        U_32 flags;        
        //opnd placement ->Register or offset
        I_32 val; 
        I_32 mptrOffset;
    };


    class GCMapCreator : public SessionAction {
        void runImpl();
        U_32 getNeedInfo()const{ return NeedInfo_LivenessInfo;}
        U_32 getSideEffects() {return Log::isEnabled();}
        bool isIRDumpEnabled(){ return true;}
    };        

    class InfoBlockWriter : public SessionAction {
        void runImpl();
        U_32 getNeedInfo()const{ return 0; }
        U_32 getSideEffects()const{ return 0; }
        bool isIRDumpEnabled(){ return false; }
    };

}} //namespace

#endif /* _IA32_GC_MAP_H_ */
