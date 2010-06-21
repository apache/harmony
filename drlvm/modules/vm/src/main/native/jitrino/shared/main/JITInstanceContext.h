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

#ifndef _JIT_INSTANCE_CONTEXT_H_
#define _JIT_INSTANCE_CONTEXT_H_

#include "open/em.h"
#include "MemoryManager.h"

#include <assert.h>
#include <string>
#include <string.h>



namespace Jitrino {

class PMF;
class ProfilingInterface;

class JITInstanceContext {

public:

    JITInstanceContext(MemoryManager&, JIT_Handle _jitHandle, const char* _jitName);

    JIT_Handle getJitHandle() const {return jitHandle;}

    const std::string& getJITName() const {return jitName;}

    PMF& getPMF () const {return *pmf;}

    ProfilingInterface* getProfilingInterface() const {return profInterface;}
    void setProfilingInterface(ProfilingInterface* pi) {assert(profInterface == NULL); profInterface = pi;}

    bool isJet() const {return useJet;}
    static bool isNameReservedForJet(const char* jitName) {return strlen(jitName)>=3 && !strncmp(jitName, "JET", 3);}

    static JITInstanceContext* getContextForJIT(JIT_Handle jitHandle);

    MemoryManager& getGlobalMemoryManager() const {return mm;}

private:

    JIT_Handle      jitHandle;
    std::string     jitName;
    PMF*            pmf;
    ProfilingInterface* profInterface;
    bool useJet;
    MemoryManager&  mm;
};

}//namespace

#endif
