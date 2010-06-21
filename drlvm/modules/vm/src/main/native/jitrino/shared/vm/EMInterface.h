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

#ifndef _EMINTERFACE_H_
#define _EMINTERFACE_H_

#include "open/em_profile_access.h"
#include "VMInterface.h"

namespace Jitrino {

enum ProfileType {
    ProfileType_Invalid = 0,
    ProfileType_EntryBackedge = 1,
    ProfileType_Edge = 2,
    ProfileType_Value = 3
};

enum JITProfilingRole{
    JITProfilingRole_GEN = 1,
    JITProfilingRole_USE = 2
};

class MethodProfile {
public:
    MethodProfile(Method_Profile_Handle _handle, ProfileType _type, MethodDesc& _md)
        : handle(_handle), type(_type), md(_md){}

    virtual ~MethodProfile(){};

    Method_Profile_Handle getHandle() const { return handle;} 

    MethodDesc& getMethod() const {return md;}

    ProfileType getProfileType() const {return type;}

private:
    Method_Profile_Handle handle;
    ProfileType type;
    MethodDesc& md;
};

class EntryBackedgeMethodProfile : public MethodProfile {
public:
    EntryBackedgeMethodProfile(Method_Profile_Handle mph, MethodDesc& md, U_32* _entryCounter, U_32 *_backedgeCounter)
        : MethodProfile(mph, ProfileType_EntryBackedge, md),  entryCounter(_entryCounter), backedgeCounter(_backedgeCounter){}

        U_32 getEntryExecCount() const {return *entryCounter;}
        U_32 getBackedgeExecCount() const {return *backedgeCounter;}
        U_32* getEntryCounter() const {return entryCounter;}
        U_32* getBackedgeCounter() const {return backedgeCounter;}

private:
    U_32* entryCounter;
    U_32* backedgeCounter;
};

class EdgeMethodProfile : public MethodProfile {
public:
    EdgeMethodProfile (Method_Profile_Handle handle, MethodDesc& md,  EM_ProfileAccessInterface* _profileAccessInterface)
        : MethodProfile(handle, ProfileType_Edge, md), profileAccessInterface(_profileAccessInterface){}

        U_32  getNumCounters() const;
        U_32  getCheckSum() const;
        U_32* getEntryCounter() const;
        U_32* getCounter(U_32 key) const;

private:
    EM_ProfileAccessInterface* profileAccessInterface;
};

class ValueMethodProfile: public MethodProfile {
public:
    ValueMethodProfile (Method_Profile_Handle handle, MethodDesc& md,  EM_ProfileAccessInterface* _profileAccessInterface)
        : MethodProfile(handle, ProfileType_Value, md), profileAccessInterface(_profileAccessInterface){}

        POINTER_SIZE_INT getTopValue(U_32 instructionKey) const;
        void dumpValues(std::ostream& os) const;

private:
    EM_ProfileAccessInterface* profileAccessInterface;
};


class ProfilingInterface {
public:

    PC_Handle getPCHandle(ProfileType type) const;
    EM_ProfileAccessInterface* getEMProfileAccessInterface() const { return profileAccessInterface; }

    MethodProfile* getMethodProfile(MemoryManager& mm, ProfileType type, MethodDesc& md, JITProfilingRole role=JITProfilingRole_USE) const;
    // Returns EM method profile handle. This method is needed when we need to update method profile
    // at run-time i.e. when there is no any memory managers available.
    Method_Profile_Handle getMethodProfileHandle(ProfileType type, MethodDesc& md) const;

    EM_PCTYPE getProfileType(PC_Handle pc) const;

    bool hasMethodProfile(ProfileType type, MethodDesc& md, JITProfilingRole role=JITProfilingRole_USE) const;
    bool enableProfiling(PC_Handle pc, JITProfilingRole role);
    bool isProfilingEnabled(ProfileType pcType, JITProfilingRole jitRole) const;


    U_32 getProfileMethodCount(MethodDesc& md, JITProfilingRole role = JITProfilingRole_USE) const;

    EntryBackedgeMethodProfile* createEBMethodProfile(MemoryManager& mm, MethodDesc& md);
    bool isEBProfilerInSyncMode() const;

    typedef void PC_Callback_Fn(Method_Profile_Handle);
    PC_Callback_Fn* getEBProfilerSyncModeCallback() const;


    EdgeMethodProfile* createEdgeMethodProfile(MemoryManager& mm, MethodDesc& md, U_32 numEdgeCounters, U_32* counterKeys, U_32 checkSum);


    U_32 getMethodEntryThreshold() const;
    U_32 getBackedgeThreshold() const;

    EntryBackedgeMethodProfile* getEBMethodProfile(MemoryManager& mm, MethodDesc& md, JITProfilingRole role=JITProfilingRole_USE) const {
        return (EntryBackedgeMethodProfile*)getMethodProfile(mm, ProfileType_EntryBackedge, md, role);
    }

    EdgeMethodProfile* getEdgeMethodProfile(MemoryManager& mm, MethodDesc& md, JITProfilingRole role=JITProfilingRole_USE) const {
        return (EdgeMethodProfile*)getMethodProfile(mm, ProfileType_Edge, md, role);    
    }
    
    
    // value profiler
    ValueMethodProfile* createValueMethodProfile (MemoryManager& mm, MethodDesc& md, U_32 numKeys, U_32* Keys);
    
    ValueMethodProfile* getValueMethodProfile(MemoryManager& mm, MethodDesc& md, JITProfilingRole role=JITProfilingRole_USE) const {
        return (ValueMethodProfile*)getMethodProfile(mm, ProfileType_Value, md, role);    
    }

    ProfilingInterface(EM_Handle _em, JIT_Handle _jit, EM_ProfileAccessInterface* emProfileAccess)
        : emHandle(_em), ebPCHandle(NULL), edgePCHandle(NULL), valuePCHandle(NULL), jitHandle(_jit), profileAccessInterface(emProfileAccess), 
        jitRole(JITProfilingRole_USE), profilingEnabled(false){}

private:
    EM_Handle emHandle;
    // Various types of the profile collectors
    PC_Handle ebPCHandle, edgePCHandle, valuePCHandle;
    // ProfileType pcType;
    JIT_Handle jitHandle;
    EM_ProfileAccessInterface* profileAccessInterface;
    // Only one role supported at one time
    JITProfilingRole jitRole;
    // There is only one flag so edge and value profile may work only simultaneously
    // TODO: Better solution is needed when we want to have independent profiles
    bool profilingEnabled;
};

};//namespace

#endif //_EMINTERFACE_H_
