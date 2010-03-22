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

#ifndef _EDGE_PROFILE_COLLECTOR_H_
#define _EDGE_PROFILE_COLLECTOR_H_

#include "DrlProfileCollectionFramework.h"
#include "open/hythread_ext.h"

#include <vector>
#include <map>

class EdgeMethodProfile;
typedef std::map<Method_Handle, EdgeMethodProfile*> EdgeProfilesMap;
typedef std::vector<EdgeMethodProfile*> EdgeProfiles;


class EdgeProfileCollector : public ProfileCollector, public TbsEMClient {
public:
    EdgeProfileCollector(EM_PC_Interface* em, const std::string& name, JIT_Handle genJit,
                                           U_32 _initialTimeout, U_32 _timeout, 
                                           U_32 _eThreshold, U_32 _bThreshold);
    virtual ~EdgeProfileCollector();

    virtual TbsEMClient* getTbsEmClient() const {return (TbsEMClient*)this;}

    virtual U_32 getInitialTimeout() const {return initialTimeout;}
    virtual U_32 getTimeout() const {return timeout;}
    virtual void onTimeout();
    virtual void classloaderUnloadingCallback(Class_Loader_Handle h);

    MethodProfile* getMethodProfile(Method_Handle mh) const ;
    EdgeMethodProfile* createProfile(Method_Handle mh, U_32 numCounters, U_32* counterKeys, U_32 checkSum);

    U_32 getEntryThreshold() const {return eThreshold;}
    U_32 getBackedgeThreshold() const {return bThreshold;}


private:
    void cleanUnloadedProfiles();

    U_32 initialTimeout;
    U_32 timeout;
    U_32 eThreshold;
    U_32 bThreshold;
    bool   loggingEnabled;
    std::string catName;
   
    bool isMethodHot( EdgeMethodProfile* profile );    
    typedef std::map<Method_Handle, EdgeMethodProfile*> EdgeProfilesMap;
    EdgeProfilesMap profilesByMethod;
    EdgeProfiles greenProfiles;
    EdgeProfiles newProfiles;
    EdgeProfiles tmpProfiles;
    EdgeProfiles unloadedMethodProfiles;
    mutable osmutex_t profilesLock;
};

class EdgeMethodProfile : public MethodProfile {
private:
    typedef std::vector<U_32> EdgeMap;
public:
    EdgeMethodProfile(EdgeProfileCollector* pc, Method_Handle mh) 
        : MethodProfile(pc, mh), entryCounter(0),
        checkSum(0), _isHot(false) {}

    void dump( const char* banner );
    void setHotMethod() { _isHot = true; }
    bool isHot() const  { return _isHot; }
    U_32* getCounter( U_32 key ) const;

    U_32 entryCounter;   // point to the method entry counter
    std::vector<U_32> counters;
    EdgeMap cntMap;        // map to a counter, given a key

    U_32  checkSum;       // checksum provided by instrumentation

private:
    bool    _isHot;
};


Method_Profile_Handle edge_profiler_create_profile(PC_Handle ph, Method_Handle mh, U_32 numCounters, U_32* counterKeys, U_32 checkSum);
U_32  edge_profiler_get_num_counters(Method_Profile_Handle mph);
U_32  edge_profiler_get_checksum(Method_Profile_Handle mph);
void* edge_profiler_get_entry_counter_addr(Method_Profile_Handle mph);
void* edge_profiler_get_counter_addr(Method_Profile_Handle mph, U_32 key);
U_32  edge_profiler_get_entry_threshold(PC_Handle pch);
U_32  edge_profiler_get_backedge_threshold(PC_Handle pch);

#endif
