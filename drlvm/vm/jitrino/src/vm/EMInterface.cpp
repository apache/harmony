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

#include "EMInterface.h"
#include "JITInstanceContext.h"

#include <assert.h>

namespace Jitrino {

PC_Handle ProfilingInterface::getPCHandle(ProfileType type) const {
    switch (type) {
        case ProfileType_EntryBackedge:
            return ebPCHandle;
        case ProfileType_Edge:
            return edgePCHandle;
        case ProfileType_Value:
            return valuePCHandle;
        default:
            assert(0);
    }
    return NULL;
}

MethodProfile* ProfilingInterface::getMethodProfile(MemoryManager& mm, ProfileType type, MethodDesc& md, JITProfilingRole role) const {
    
    Method_Profile_Handle mpHandle = profileAccessInterface->get_method_profile(emHandle, getPCHandle(type), md.getMethodHandle());
    if (mpHandle==0) {
        return NULL;
    }
    MethodProfile* p = NULL;
    if (type == ProfileType_Edge) {
        p = new (mm) EdgeMethodProfile(mpHandle, md, profileAccessInterface);
    } else if (type == ProfileType_Value) {
        p = new (mm) ValueMethodProfile(mpHandle, md, profileAccessInterface);
    } else {
        U_32* eCounter = (U_32*)profileAccessInterface->eb_profiler_get_entry_counter_addr(mpHandle);
        U_32* bCounter = (U_32*)profileAccessInterface->eb_profiler_get_backedge_counter_addr(mpHandle);
        p = new (mm) EntryBackedgeMethodProfile(mpHandle, md, eCounter, bCounter);
    }
    return p;
}

Method_Profile_Handle ProfilingInterface::getMethodProfileHandle(ProfileType type, MethodDesc& md) const {
    return profileAccessInterface->get_method_profile(emHandle, getPCHandle(type), md.getMethodHandle());
}

EM_PCTYPE ProfilingInterface::getProfileType(PC_Handle pch) const {
    return profileAccessInterface->get_pc_type(emHandle, pch);
}

bool ProfilingInterface::hasMethodProfile(ProfileType type, MethodDesc& md, JITProfilingRole role) const {

    PC_Handle pcHandle = getPCHandle(type);

    if (pcHandle == NULL) {
        return false;
    }
    if (jitRole != role) {
        return false;
    }
    if (profileAccessInterface != NULL) {
        Method_Profile_Handle mpHandle = profileAccessInterface->get_method_profile(emHandle, pcHandle, md.getMethodHandle());
        return mpHandle!=0;
    }
    return false;
}

U_32 ProfilingInterface::getProfileMethodCount(MethodDesc& md, JITProfilingRole role) const {
    assert(jitRole == role);
    PC_Handle pcHandle = getPCHandle(ProfileType_Edge);
    ProfileType pcType = ProfileType_Edge;
    if (pcHandle == NULL) {
        pcHandle = getPCHandle(ProfileType_EntryBackedge);
        pcType = ProfileType_EntryBackedge;
    }
    assert (pcHandle != NULL);
    Method_Handle methodHandle = md.getMethodHandle();
    Method_Profile_Handle mph = profileAccessInterface->get_method_profile(emHandle, pcHandle, methodHandle);
    if (mph == NULL) {
        return 0;
    }
    U_32* counterAddr = NULL;
    if (pcType == ProfileType_Edge) { 
        counterAddr = (U_32*)profileAccessInterface->edge_profiler_get_entry_counter_addr(mph);
    } else {
        counterAddr = (U_32*)profileAccessInterface->eb_profiler_get_entry_counter_addr(mph);
    }
    return *counterAddr;
}

bool ProfilingInterface::enableProfiling(PC_Handle pc, JITProfilingRole role) {
    EM_PCTYPE _pcType =  profileAccessInterface->get_pc_type(emHandle, pc);
    if (_pcType != EM_PCTYPE_EDGE && _pcType != EM_PCTYPE_ENTRY_BACKEDGE && _pcType != EM_PCTYPE_VALUE) {
        return false;
    }
    JITInstanceContext* jitMode = JITInstanceContext::getContextForJIT(jitHandle);
    if (jitMode->isJet()) {
        if (role == JITProfilingRole_GEN) {
            profilingEnabled = _pcType == EM_PCTYPE_ENTRY_BACKEDGE; 
        } else {
            profilingEnabled = false;
        }
    } else { //OPT
        profilingEnabled = true;
    }
    if (profilingEnabled) {
        jitRole = role;
        switch(_pcType)
        {
        case EM_PCTYPE_EDGE:
            edgePCHandle = pc;
            break;
        case EM_PCTYPE_ENTRY_BACKEDGE:
            ebPCHandle = pc;
            break;
        case EM_PCTYPE_VALUE:
            valuePCHandle = pc;
            break;
        default:
            assert(0);
            return false;
        }
    }
    return profilingEnabled;
}

bool ProfilingInterface::isProfilingEnabled(ProfileType pcType, JITProfilingRole role) const {
    if(!profilingEnabled || (jitRole != role) || (getPCHandle(pcType) == NULL)){
        return false;
    }
    return true;
}

EntryBackedgeMethodProfile* ProfilingInterface::createEBMethodProfile(MemoryManager& mm, MethodDesc& md) {
    assert(isProfilingEnabled(ProfileType_EntryBackedge, JITProfilingRole_GEN));
    PC_Handle pcHandle = getPCHandle(ProfileType_EntryBackedge);
    Method_Profile_Handle mpHandle = profileAccessInterface->eb_profiler_create_profile(pcHandle, md.getMethodHandle());
    assert(mpHandle!=0);
    U_32* eCounter = (U_32*)profileAccessInterface->eb_profiler_get_entry_counter_addr(mpHandle);
    U_32* bCounter = (U_32*)profileAccessInterface->eb_profiler_get_backedge_counter_addr(mpHandle);

    EntryBackedgeMethodProfile* p = new (mm) EntryBackedgeMethodProfile(mpHandle, md, eCounter, bCounter);
    return p;
}


EdgeMethodProfile* ProfilingInterface::createEdgeMethodProfile( MemoryManager& mm,
                                                                  MethodDesc& md,
                                                                  U_32 numCounters,
                                                                  U_32* counterKeys,
                                                                  U_32 checkSum )
{
    assert(isProfilingEnabled(ProfileType_Edge, JITProfilingRole_GEN));
    PC_Handle pcHandle = getPCHandle(ProfileType_Edge);
    Method_Profile_Handle mpHandle =  profileAccessInterface->edge_profiler_create_profile( 
        pcHandle, md.getMethodHandle(), numCounters, counterKeys, checkSum);
    assert( mpHandle != NULL );

    EdgeMethodProfile* p = new (mm) EdgeMethodProfile(mpHandle, md, profileAccessInterface);
    return p;
}

ValueMethodProfile* ProfilingInterface::createValueMethodProfile(MemoryManager& mm,
                                                                    MethodDesc& md,
                                                                    U_32 numKeys,
                                                                    U_32* Keys)
{
    assert(isProfilingEnabled(ProfileType_Value, JITProfilingRole_GEN));
    PC_Handle pcHandle = getPCHandle(ProfileType_Value);
    Method_Profile_Handle mpHandle =  profileAccessInterface->value_profiler_create_profile( 
        pcHandle, md.getMethodHandle(), numKeys, Keys);
    assert(mpHandle != NULL);

    ValueMethodProfile* p = new (mm) ValueMethodProfile(mpHandle, md, profileAccessInterface);
    return p;
}


U_32 ProfilingInterface::getMethodEntryThreshold() const {
    PC_Handle pcHandle = getPCHandle(ProfileType_Edge);
    if (pcHandle != NULL) {
        return profileAccessInterface->edge_profiler_get_entry_threshold(pcHandle);
    } else if ((pcHandle = getPCHandle(ProfileType_EntryBackedge)) != NULL) {
        return profileAccessInterface->eb_profiler_get_entry_threshold(pcHandle);
    }
    assert(0);
    return 0;
}

U_32 ProfilingInterface::getBackedgeThreshold() const {
    PC_Handle pcHandle = getPCHandle(ProfileType_Edge);
    if (pcHandle != NULL) {
        return profileAccessInterface->edge_profiler_get_backedge_threshold(pcHandle);
    } else if ((pcHandle = getPCHandle(ProfileType_EntryBackedge)) != NULL) {
        return profileAccessInterface->eb_profiler_get_backedge_threshold(pcHandle);
    }
    assert(0);
    return 0;
}

bool ProfilingInterface::isEBProfilerInSyncMode() const {
    PC_Handle pcHandle = getPCHandle(ProfileType_EntryBackedge);
    assert(pcHandle!=NULL);
    return profileAccessInterface->eb_profiler_is_in_sync_mode(pcHandle)!=0;
}

ProfilingInterface::PC_Callback_Fn* ProfilingInterface::getEBProfilerSyncModeCallback() const {
    assert(profileAccessInterface->eb_profiler_sync_mode_callback!=NULL);
    return (PC_Callback_Fn*)profileAccessInterface->eb_profiler_sync_mode_callback;
}


U_32  EdgeMethodProfile::getNumCounters() const {
    return profileAccessInterface->edge_profiler_get_num_counters(getHandle());
}

U_32  EdgeMethodProfile::getCheckSum() const {
    return profileAccessInterface->edge_profiler_get_checksum(getHandle());
}

U_32* EdgeMethodProfile::getEntryCounter() const {
    return (U_32*)profileAccessInterface->edge_profiler_get_entry_counter_addr(getHandle());
}

U_32* EdgeMethodProfile::getCounter(U_32 key) const  {
    U_32* counter = (U_32*)profileAccessInterface->edge_profiler_get_counter_addr(getHandle(), key);
    return counter;
}

POINTER_SIZE_INT ValueMethodProfile::getTopValue(U_32 instructionKey) const {
    return profileAccessInterface->value_profiler_get_top_value(getHandle(), instructionKey);
}

void ValueMethodProfile::dumpValues(std::ostream& os) const {
    profileAccessInterface->value_profiler_dump_values(getHandle(), os);
}

} //namespace

