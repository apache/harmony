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
* @author Jack Liu, Mikhail Y. Fursov, Chen-Dong Yuan
*/

#include "EdgeProfileCollector.h"

#define LOG_DOMAIN "em"
#include "cxxlog.h"

#include <algorithm>
#include <assert.h>
#include <sstream>
#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"
#include "jit_intf.h"
#include "port_mutex.h"


Method_Profile_Handle edge_profiler_create_profile( PC_Handle ph,
                                                    Method_Handle mh,
                                                    U_32 numCounters,
                                                    U_32* counterKeys,
                                                    U_32 checkSum )
{
    ProfileCollector* pc = (ProfileCollector*)ph;
    assert(pc->type == EM_PCTYPE_EDGE);

    EdgeMethodProfile* profile =
        ((EdgeProfileCollector*)pc)->createProfile(mh, numCounters, counterKeys, checkSum);
    return (Method_Profile_Handle)profile;
}

void* edge_profiler_get_entry_counter_addr(Method_Profile_Handle mph) {
    MethodProfile* mp = (MethodProfile*)mph;
    assert(mp->pc->type == EM_PCTYPE_EDGE);
    EdgeMethodProfile* emp = (EdgeMethodProfile*)mp;
    return &emp->entryCounter;
}

void* edge_profiler_get_counter_addr(Method_Profile_Handle mph, U_32 key)
{
    MethodProfile* mp = (MethodProfile*)mph;
    assert(mp->pc->type == EM_PCTYPE_EDGE);
    return ((EdgeMethodProfile*)mp)->getCounter( key );
}


U_32 edge_profiler_get_num_counters(Method_Profile_Handle mph)
{
    MethodProfile* mp = (MethodProfile*)mph;
    assert(mp->pc->type == EM_PCTYPE_EDGE);
    return (U_32)((EdgeMethodProfile*)mp)->counters.size();
}


U_32 edge_profiler_get_checksum(Method_Profile_Handle mph)
{
    MethodProfile* mp = (MethodProfile*)mph;
    assert(mp->pc->type == EM_PCTYPE_EDGE);
    return ((EdgeMethodProfile*)mp)->checkSum;
}

U_32 edge_profiler_get_entry_threshold(PC_Handle pch) {
    assert(pch!=NULL);
    ProfileCollector* pc = (ProfileCollector*)pch;
    assert(pc->type == EM_PCTYPE_EDGE);
    return ((EdgeProfileCollector*)pc)->getEntryThreshold();
}

U_32 edge_profiler_get_backedge_threshold(PC_Handle pch) {
    assert(pch!=NULL);
    ProfileCollector* pc = (ProfileCollector*)pch;
    assert(pc->type == EM_PCTYPE_EDGE);
    return ((EdgeProfileCollector*)pc)->getBackedgeThreshold();
}




EdgeProfileCollector::EdgeProfileCollector(EM_PC_Interface* em, const std::string& name, JIT_Handle genJit,
                                           U_32 _initialTimeout, U_32 _timeout, 
                                           U_32 _eThreshold, U_32 _bThreshold)
                                           : ProfileCollector(em, name, EM_PCTYPE_EDGE, genJit), initialTimeout(_initialTimeout), 
                                           timeout(_timeout),eThreshold(_eThreshold), bThreshold(_bThreshold)
{
    port_mutex_create(&profilesLock, APR_THREAD_MUTEX_NESTED);
    catName = std::string(LOG_DOMAIN) + ".profiler." + name;
    loggingEnabled =  log_is_info_enabled(LOG_DOMAIN) || log_is_info_enabled(catName.c_str());
    if (loggingEnabled) {
        std::ostringstream msg;
        msg<< "EM: edge profiler intialized: "<<name
            <<" entry threshold:"<<eThreshold << " edge threshold:"<<bThreshold;
        INFO2(catName.c_str(), msg.str().c_str());
    }
}

EdgeProfileCollector::~EdgeProfileCollector()
{
    EdgeProfilesMap::iterator it;
    for( it = profilesByMethod.begin(); it != profilesByMethod.end(); it++ ){
        EdgeMethodProfile* profile = it->second;
        delete profile;
    }
    port_mutex_destroy(&profilesLock);
}


MethodProfile* EdgeProfileCollector::getMethodProfile(Method_Handle mh) const
{
    port_mutex_lock(&profilesLock);
    MethodProfile* res = NULL;
    EdgeProfilesMap::const_iterator it = profilesByMethod.find(mh);
    if (it != profilesByMethod.end()) {
        res = it->second;    
    }
    port_mutex_unlock(&profilesLock);
    return res;
}



U_32* EdgeMethodProfile::getCounter( U_32 key ) const 
{
    //log2 search
    EdgeMap::const_iterator it = lower_bound(cntMap.begin(), cntMap.end(), key);
    if (it == cntMap.end() || *it != key) {
        return NULL;
    }
    U_32 idx = (U_32)(it - cntMap.begin());
    return (U_32*)&counters.front() + idx;
}

void EdgeMethodProfile::dump( const char* banner )
{
    const char* methodName = method_get_name(mh);
    Class_Handle ch = method_get_class(mh);
    const char* className = class_get_name(ch);
    const char* signature = method_get_descriptor(mh);
    U_32 backEdgeCounter = entryCounter;
    U_32 instrCost = entryCounter;

    assert(  banner != NULL );

    fprintf( stderr, "%s: %s::%s%s\n", banner, className, methodName, signature );

    for( U_32 i = 0; i < counters.size(); i++ ){
        instrCost += counters[i];
        if( counters[i] > backEdgeCounter ){
            backEdgeCounter = counters[i];
        }
    }

    assert( instrCost >= backEdgeCounter );

    fprintf( stderr, "\t%s entry: %d\tcounters: %d\tbackedge: %d\tcost: %u\n",
             _isHot ? "hot" : "cold",
             entryCounter, counters.size(), backEdgeCounter, instrCost );

    return;
}


EdgeMethodProfile* EdgeProfileCollector::createProfile( Method_Handle mh,
                                                        U_32 numCounters,
                                                        U_32* counterKeys,
                                                        U_32 checkSum)
{
    port_mutex_lock(&profilesLock);

    EdgeMethodProfile* profile = new EdgeMethodProfile(this, mh);

    // Allocate space for edge counters.
    assert( profile->cntMap.empty() );
    profile->counters.resize(numCounters);
    profile->checkSum = checkSum;
    profile->cntMap.insert(profile->cntMap.begin(), counterKeys, counterKeys + numCounters);
    std::sort(profile->cntMap.begin(), profile->cntMap.end());
    
    assert(std::adjacent_find(profile->cntMap.begin(), profile->cntMap.end())==profile->cntMap.end());
    assert(profilesByMethod.find(mh) == profilesByMethod.end());
    profilesByMethod[mh] = profile;
    newProfiles.push_back(profile);

    port_mutex_unlock(&profilesLock);

    return profile;
}


bool EdgeProfileCollector::isMethodHot( EdgeMethodProfile* profile )
{
    U_32 entryCounter = profile->entryCounter;
    if( entryCounter >= eThreshold ){
        return true;
    }

    const U_32 cutoff = bThreshold;

    for( U_32 i = 0; i < profile->counters.size(); i++ ){
        if( profile->counters[i] >= cutoff ){
            return true;
        }
    }

    return false;
}


static void logReadyProfile(const std::string& catName, const std::string& profilerName, EdgeMethodProfile* mp) {
    const char* methodName = method_get_name(mp->mh);
    Class_Handle ch = method_get_class(mp->mh);
    const char* className = class_get_name(ch);
    const char* signature = method_get_descriptor(mp->mh);

    U_32 backEgdeMaxValue = mp->counters.empty() ? 0 : *std::max_element( mp->counters.begin(), mp->counters.end());
    std::ostringstream msg;
    msg <<"EM: profiler["<<profilerName.c_str()<<"] profile is ready [e:"
        << mp->entryCounter<<" b:"<<backEgdeMaxValue<<"] " <<className<<"::"<<methodName<<signature;
    INFO2(catName.c_str(), msg.str().c_str());
}


void EdgeProfileCollector::onTimeout() {
    if(!newProfiles.empty()) {
        port_mutex_lock(&profilesLock);
        greenProfiles.insert(greenProfiles.end(), newProfiles.begin(), newProfiles.end());
        newProfiles.clear();
        port_mutex_unlock(&profilesLock);
    }

    if (!unloadedMethodProfiles.empty()) {
        cleanUnloadedProfiles();
    }

    
    for (EdgeProfiles::iterator it = greenProfiles.begin(), end = greenProfiles.end(); it!=end; ++it) {
        EdgeMethodProfile* profile = *it;
        if( isMethodHot( profile ) ){
            profile->setHotMethod();
            tmpProfiles.push_back(profile);
            *it = NULL;
        }
    }

    if (!tmpProfiles.empty()) {
        port_mutex_lock(&profilesLock);
        std::remove(greenProfiles.begin(), greenProfiles.end(), (EdgeMethodProfile*)NULL);
        greenProfiles.resize(greenProfiles.size() - tmpProfiles.size());
        port_mutex_unlock(&profilesLock);
        for (EdgeProfiles::iterator it = tmpProfiles.begin(), end = tmpProfiles.end(); it!=end; ++it) {
            EdgeMethodProfile* profile = *it;
            if (loggingEnabled) {
                logReadyProfile(catName, name, profile);
            }
            em->methodProfileIsReady(profile);
        }
        tmpProfiles.clear();
    }
}

void EdgeProfileCollector::cleanUnloadedProfiles() {
    for (EdgeProfiles::const_iterator it = unloadedMethodProfiles.begin(), end = unloadedMethodProfiles.end(); it!=end; ++it) {    
        EdgeMethodProfile* profile = *it;
        profilesByMethod.erase(profile->mh);

        EdgeProfiles::iterator it2 = std::find(greenProfiles.begin(), greenProfiles.end(), profile);
        assert(it2!=greenProfiles.end());
        *it2=NULL;

        delete profile;
    }
    unloadedMethodProfiles.clear();
    greenProfiles.erase(std::remove(greenProfiles.begin(), greenProfiles.end(), (EdgeMethodProfile*)NULL), greenProfiles.end());
}


static void addProfilesForClassloader(Class_Loader_Handle h, EdgeProfiles& from, EdgeProfiles& to) {
    for (EdgeProfiles::iterator it = from.begin(), end = from.end(); it!=end; ++it) {
        EdgeMethodProfile* profile = *it;
        Class_Handle ch =  method_get_class(profile->mh);;
        Class_Loader_Handle clh = class_get_class_loader(ch);
        if (clh == h) {
            to.push_back(profile);
        }
    }
}

void EdgeProfileCollector::classloaderUnloadingCallback(Class_Loader_Handle h) {
    port_mutex_lock(&profilesLock);

    //can't modify profiles map in async mode here -> it could be iterated by the checker thread without lock
    addProfilesForClassloader(h, greenProfiles, unloadedMethodProfiles);
    addProfilesForClassloader(h, newProfiles, unloadedMethodProfiles);

    port_mutex_unlock(&profilesLock);
}
