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

#include "EBProfileCollector.h"

#include <algorithm>
#include <assert.h>

#define LOG_DOMAIN "em"
#include "cxxlog.h"

#include <sstream>
#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"
#include "jit_intf.h"
#include "port_mutex.h"


EBProfileCollector::EBProfileCollector(EM_PC_Interface* em, const std::string& name, JIT_Handle genJit, 
                                       EB_ProfilerMode _mode, U_32 _eThreshold, U_32 _bThreshold,
                                       U_32 _initialTimeout, U_32 _timeout) 
                                       : ProfileCollector(em, name, EM_PCTYPE_ENTRY_BACKEDGE, genJit),
                                        mode(_mode), eThreshold(_eThreshold), bThreshold(_bThreshold), 
                                        initialTimeout(_initialTimeout), timeout(_timeout), loggingEnabled(false)
                                       
{
    assert( (mode == EB_PCMODE_SYNC ? (initialTimeout==0 && timeout==0) : timeout > 0) );
    catName = std::string(LOG_DOMAIN) + ".profiler." + name;
    loggingEnabled =  log_is_info_enabled(LOG_DOMAIN);
    if (!loggingEnabled) {
        loggingEnabled = log_is_info_enabled(catName.c_str());
    }
    if (loggingEnabled) {
        std::ostringstream msg;
        msg<< "EM: entry-backedge profiler intialized: "<<name
            <<" entry threshold:"<<eThreshold << " backedge threshold:"<<bThreshold
            <<" mode:"<<(mode == EB_PCMODE_ASYNC? "ASYNC": "SYNC");
        INFO2(catName.c_str(), msg.str().c_str());
    }

    port_mutex_create(&profilesLock, APR_THREAD_MUTEX_NESTED);
}

Method_Profile_Handle eb_profiler_create_profile(PC_Handle ph, Method_Handle mh) {
    ProfileCollector* pc = (ProfileCollector*)ph;
    assert(pc->type == EM_PCTYPE_ENTRY_BACKEDGE);
    EBMethodProfile* profile = ((EBProfileCollector*)pc)->createProfile(mh);
    assert(profile!=NULL);
    return (Method_Profile_Handle)profile;
}

void* eb_profiler_get_entry_counter_addr(Method_Profile_Handle mph) {
    MethodProfile* mp = (MethodProfile*)mph;
    assert(mp->pc->type == EM_PCTYPE_ENTRY_BACKEDGE);
    return (void*)&((EBMethodProfile*)mp)->entryCounter;
}


void* eb_profiler_get_backedge_counter_addr(Method_Profile_Handle mph) {
    MethodProfile* mp = (MethodProfile*)mph;
    assert(mp->pc->type == EM_PCTYPE_ENTRY_BACKEDGE);
    return (void*)&((EBMethodProfile*)mp)->backedgeCounter;
}

char eb_profiler_is_in_sync_mode(PC_Handle pch) {
    assert(pch!=NULL);
    ProfileCollector* pc = (ProfileCollector*)pch;
    assert(pc->type == EM_PCTYPE_ENTRY_BACKEDGE);
    return ((EBProfileCollector*)pc)->getMode() == EBProfileCollector::EB_PCMODE_SYNC;
}


void __stdcall eb_profiler_sync_mode_callback(Method_Profile_Handle mph) {
    assert(mph!=NULL);
    MethodProfile* mp = (MethodProfile*)mph;
    assert(mp->pc->type == EM_PCTYPE_ENTRY_BACKEDGE);
    ((EBProfileCollector*)mp->pc)->syncModeJitCallback(mp);
}

U_32 eb_profiler_get_entry_threshold(PC_Handle pch) {
    assert(pch!=NULL);
    ProfileCollector* pc = (ProfileCollector*)pch;
    assert(pc->type == EM_PCTYPE_ENTRY_BACKEDGE);
    return ((EBProfileCollector*)pc)->getEntryThreshold();
}

U_32 eb_profiler_get_backedge_threshold(PC_Handle pch) {
    assert(pch!=NULL);
    ProfileCollector* pc = (ProfileCollector*)pch;
    assert(pc->type == EM_PCTYPE_ENTRY_BACKEDGE);
    return ((EBProfileCollector*)pc)->getBackedgeThreshold();
}


EBProfileCollector::~EBProfileCollector() {
    for (EBProfilesMap::iterator it = profilesByMethod.begin(), end = profilesByMethod.end(); it!=end; ++it) {
        EBMethodProfile* profile = it->second;
        delete profile;
    }

    port_mutex_destroy(&profilesLock);
}

MethodProfile* EBProfileCollector::getMethodProfile(Method_Handle mh) const {
    port_mutex_lock(&profilesLock);
    MethodProfile* res = NULL;
    EBProfilesMap::const_iterator it = profilesByMethod.find(mh);
    if (it != profilesByMethod.end()) {
        res = it->second;
    }
    port_mutex_unlock(&profilesLock);
    return res;
}

EBMethodProfile* EBProfileCollector::createProfile(Method_Handle mh) {
    EBMethodProfile* profile = new EBMethodProfile(this, mh);

    port_mutex_lock(&profilesLock);

    assert(profilesByMethod.find(mh) == profilesByMethod.end());
    profilesByMethod[mh] = profile;
    if (mode == EB_PCMODE_ASYNC) {
        //can't modify profiles map -> it could be iterated by the checker thread without lock
        newProfiles.push_back(profile);
    }

    port_mutex_unlock(&profilesLock);

    return profile;
}

static void logReadyProfile(const std::string& catName, const std::string& profilerName, EBMethodProfile* mp) {
    const char* methodName = method_get_name(mp->mh);
    Class_Handle ch = method_get_class(mp->mh);
    const char* className = class_get_name(ch);
    const char* signature = method_get_descriptor(mp->mh);

    std::ostringstream msg;
    msg <<"EM: profiler["<<profilerName.c_str()<<"] profile is ready [e:"
        << mp->entryCounter <<" b:"<<mp->backedgeCounter<<"] "
        <<className<<"::"<<methodName<<signature;
    INFO2(catName.c_str(), msg.str().c_str());
}

void EBProfileCollector::onTimeout() {
    assert(mode == EB_PCMODE_ASYNC);
    if(!newProfiles.empty()) {
        port_mutex_lock(&profilesLock);
        greenProfiles.insert(greenProfiles.end(), newProfiles.begin(), newProfiles.end());
        newProfiles.clear();
        port_mutex_unlock(&profilesLock);
    }

    if (!unloadedMethodProfiles.empty()) {
        cleanUnloadedProfiles(true);
    }

    for (EBProfiles::iterator it = greenProfiles.begin(), end = greenProfiles.end(); it!=end; ++it) {
        EBMethodProfile* profile = *it;
        if (profile->entryCounter >= eThreshold || profile->backedgeCounter >= bThreshold) {
            tmpProfiles.push_back(profile);
            *it = NULL;
        }
    }
    if (!tmpProfiles.empty()) {
        port_mutex_lock(&profilesLock);
        std::remove(greenProfiles.begin(), greenProfiles.end(), (EBMethodProfile*)NULL);
        greenProfiles.resize(greenProfiles.size() - tmpProfiles.size());
        port_mutex_unlock(&profilesLock);
        for (EBProfiles::iterator it = tmpProfiles.begin(), end = tmpProfiles.end(); it!=end; ++it) {
            EBMethodProfile* profile = *it;
            if (loggingEnabled) {
                logReadyProfile(catName, name, profile);
            }
            em->methodProfileIsReady(profile);
        }
        tmpProfiles.clear();
    }
}

void EBProfileCollector::syncModeJitCallback(MethodProfile* mp) {
    assert(mode == EB_PCMODE_SYNC);
    assert(mp->pc == this);
    if (loggingEnabled) {
        logReadyProfile(catName, name, (EBMethodProfile*)mp);
    }
    em->methodProfileIsReady(mp);
}

static void addProfilesForClassloader(Class_Loader_Handle h, EBProfiles& from, EBProfiles& to, bool erase) {
    for (EBProfiles::iterator it = from.begin(), end = from.end(); it!=end; ++it) {
        EBMethodProfile* profile = *it;
        Class_Handle ch =  method_get_class(profile->mh);;
        Class_Loader_Handle clh = class_get_class_loader(ch);
        if (clh == h) {
            to.push_back(profile);
            if (erase) {
                *it=NULL;
            }
        }
    }
    if (erase) {
        from.erase(std::remove(from.begin(), from.end(), (EBMethodProfile*)NULL), from.end());
    }
}

void EBProfileCollector::cleanUnloadedProfiles(bool removeFromGreen) {
    for (EBProfiles::const_iterator it = unloadedMethodProfiles.begin(), end = unloadedMethodProfiles.end(); it!=end; ++it) {    
        EBMethodProfile* profile = *it;
        profilesByMethod.erase(profile->mh);
        if (removeFromGreen) {
            EBProfiles::iterator it2 = std::find(greenProfiles.begin(), greenProfiles.end(), profile);
            assert(it2!=greenProfiles.end());
            *it2=NULL;
        }
        delete profile;
    }
    unloadedMethodProfiles.clear();
    if (removeFromGreen) {
        greenProfiles.erase(std::remove(greenProfiles.begin(), greenProfiles.end(), (EBMethodProfile*)NULL), greenProfiles.end());
    }
}

void EBProfileCollector::classloaderUnloadingCallback(Class_Loader_Handle h) {
    port_mutex_lock(&profilesLock);
    
    //can't modify profiles map in async mode here -> it could be iterated by the checker thread without lock
    bool erase = mode != EB_PCMODE_ASYNC;
    addProfilesForClassloader(h, greenProfiles, unloadedMethodProfiles, erase);
    addProfilesForClassloader(h, newProfiles, unloadedMethodProfiles, erase);
    
    if (erase) {
        cleanUnloadedProfiles(false);
    }

    port_mutex_unlock(&profilesLock);
}
