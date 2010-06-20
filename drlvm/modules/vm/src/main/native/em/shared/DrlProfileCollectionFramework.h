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

#ifndef _DRL_PROFILE_COLLECTION_FRAMEWORK_H_
#define _DRL_PROFILE_COLLECTION_FRAMEWORK_H_

//TODO: review private/protected members
#include "open/em.h"
#include "open/em_profile_access.h"

#include <vector>
#include <string>

class TbsEMClient;
class ProfileCollector;
class MethodProfile;

typedef std::vector<TbsEMClient*> TbsClients;
typedef std::vector<ProfileCollector*> ProfileCollectors;
typedef std::vector<JIT_Handle> Jits;

class EM_PC_Interface {
public:
    virtual ~EM_PC_Interface(){};
    virtual void methodProfileIsReady(MethodProfile* mp) =0;
};

class MethodProfile {
public:
    MethodProfile(ProfileCollector* _pc, Method_Handle _mh) 
        : pc(_pc), mh(_mh){}
    virtual ~MethodProfile(){}

    ProfileCollector* pc;
    Method_Handle mh;
};

class ProfileCollector {
public:
    ProfileCollector(EM_PC_Interface* _em, const std::string& _name, EM_PCTYPE _type, JIT_Handle _genJit) 
        :em(_em), name(_name), type(_type), genJit(_genJit){}
    virtual ~ProfileCollector(){}

    virtual TbsEMClient* getTbsEmClient() const = 0;
    virtual MethodProfile* getMethodProfile(Method_Handle mh) const = 0;
    
    virtual void addUseJit(JIT_Handle jit) { useJits.push_back(jit);}
    
    virtual void classloaderUnloadingCallback(Class_Loader_Handle h) {}

    EM_PC_Interface* em;
    std::string name;
    EM_PCTYPE type;
    JIT_Handle genJit;
    Jits useJits;
};

class TbsEMClient {
public:
    TbsEMClient() : nextTick(0) {}
    virtual ~TbsEMClient(){};

    virtual U_32 getInitialTimeout() const  = 0;
    virtual U_32 getTimeout() const = 0;
    virtual void onTimeout() = 0;

    virtual U_32 getNextTick() const {return nextTick;}
    virtual void setNextTick(U_32 n) {nextTick = n;}
private:
    U_32 nextTick;
};


#endif
