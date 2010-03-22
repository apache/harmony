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
* @author Mikhail Y. Fursov
*/

#ifndef _ENTRY_BACKEDGE_PROFILE_COLLECTOR_H_
#define _ENTRY_BACKEDGE_PROFILE_COLLECTOR_H_

#include "DrlProfileCollectionFramework.h"

#include "open/hythread_ext.h"
#include "platform_lowlevel.h"

#include <map>

class EBMethodProfile;

typedef std::map<Method_Handle, EBMethodProfile*> EBProfilesMap;
typedef std::vector<EBMethodProfile*> EBProfiles;

class EBProfileCollector : public ProfileCollector, public TbsEMClient {
public:
    // Profile checking modes
    enum EB_ProfilerMode {
        // check profiles in separate EM thread.
        // in this mode profile collector will register self to EM as 
        // tbs client to receive periodic callbacks to check profiles
        EB_PCMODE_ASYNC,

        // in this mode profile checking is done in user's Java thread
        // profile collector waits for events from user's Java thread and 
        // notifies EM when profile is ready
        EB_PCMODE_SYNC
    };

    EBProfileCollector(EM_PC_Interface* em, const std::string& name, JIT_Handle genJit, EB_ProfilerMode _mode,
        U_32 _eThreshold, U_32 _bThreshold, U_32 _initialTimeout=0, U_32 _timeout=0);
        
    virtual ~EBProfileCollector();

    virtual TbsEMClient* getTbsEmClient() const {return mode == EB_PCMODE_ASYNC ? (TbsEMClient*)this : NULL;}
    
    virtual U_32 getInitialTimeout() const {return initialTimeout;}
    virtual U_32 getTimeout() const {return timeout;}
    virtual void onTimeout();
    virtual MethodProfile* getMethodProfile(Method_Handle mh) const ;
    virtual void classloaderUnloadingCallback(Class_Loader_Handle h);
    
    EBMethodProfile* createProfile(Method_Handle mh);
    void syncModeJitCallback(MethodProfile* mp);

    U_32 getEntryThreshold() const {return eThreshold;}
    U_32 getBackedgeThreshold() const {return bThreshold;}

    EB_ProfilerMode getMode() const {return mode;}

private:

    void cleanUnloadedProfiles(bool removeFromGreen);

    EB_ProfilerMode mode;
    U_32 eThreshold;
    U_32 bThreshold;
    U_32 initialTimeout;
    U_32 timeout;
    bool loggingEnabled;
    std::string catName;
    
    //Mapping of method profile by method handles
    EBProfilesMap profilesByMethod;
    
    // method profiles to check every during tbs callback
    // field is used only in EB_PCMODE_ASYNC mode
    EBProfiles greenProfiles;
    
    // newly created method profiles since the last check during tbs callback
    // field is used only in EB_PCMODE_ASYNC mode
    EBProfiles newProfiles;

    // preallocated mem for temporary (method-local) needs
    EBProfiles tmpProfiles;

    EBProfiles unloadedMethodProfiles;

    mutable osmutex_t profilesLock;
};

class EBMethodProfile : public MethodProfile {
public:
    EBMethodProfile(EBProfileCollector* pc, Method_Handle mh) 
        : MethodProfile(pc, mh), entryCounter(0), backedgeCounter(0){}
    U_32 entryCounter, backedgeCounter;
};


Method_Profile_Handle eb_profiler_create_profile(PC_Handle ph, Method_Handle mh);
void* eb_profiler_get_entry_counter_addr(Method_Profile_Handle mph);
void*eb_profiler_get_backedge_counter_addr(Method_Profile_Handle mph);
void  __stdcall eb_profiler_sync_mode_callback(PC_Handle mph);
char  eb_profiler_is_in_sync_mode(PC_Handle pch);
U_32 eb_profiler_get_entry_threshold(PC_Handle pch);
U_32 eb_profiler_get_backedge_threshold(PC_Handle pch);

#endif
