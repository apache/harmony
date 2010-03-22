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

#ifndef _DRL_EM_IMPL_
#define _DRL_EM_IMPL_

#include "MTable.h"
#include "DrlProfileCollectionFramework.h"
#include "method_lookup.h"
#include "open/em.h"
#include "open/hythread_ext.h"
#include "open/em_profile_access.h"
#include "jni.h"

#include <apr_dso.h>
#include <string>
#include <set>
#include <vector>

class RChain;
class RStep;
class DrlEMImpl;

#define EM_TBS_TICK_TIMEOUT 100
typedef std::vector<RChain*> RChains;
typedef std::vector<RStep*> RSteps;


/** Recompilation step. One recompilation chain can have 1 or more recompilation steps */
class RStep {
public:
    RStep(JIT_Handle _jit, const std::string& _jitName, RChain* _chain, apr_dso_handle_t* _libHandle);
    virtual ~RStep(){}
   
    JIT_Handle jit;
    std::string jitName, catName;
    RChain* chain;
    bool loggingEnabled;
    apr_dso_handle_t* libHandle;

    bool (*enable_profiling)(JIT_Handle, PC_Handle, EM_JIT_PC_Role);
    void (*profile_notification_callback)(JIT_Handle, PC_Handle, Method_Handle);
};


/** Recompilation chain */
class RChain {
public:
    RChain(){};
    virtual ~RChain(){};
    
    bool acceptMethod(Method_Handle mh, size_t n) const { return methodTable.acceptMethod(mh, n);}
    bool addMethodFilter(const std::string& filterString) {return methodTable.addMethodFilter(filterString);}

    RSteps steps;
    MTable methodTable;
};

class DrlEMFactory {
public:
    //EM interface impl:
    static DrlEMImpl* createAndInitEMInstance();
    static DrlEMImpl* getEMInstance();
    static void deinitEMInstance();
private:
    static DrlEMImpl* emInstance;

};

class DrlEMImpl : public EM_PC_Interface {
public:
    DrlEMImpl();
    virtual ~DrlEMImpl();

    virtual bool init();
    virtual void deinit();
    virtual void executeMethod(jmethodID meth, jvalue  *return_value, jvalue *args);
    virtual JIT_Result compileMethod(Method_Handle method_handle);
    virtual void registerCodeChunk(Method_Handle method_handle, void *code_addr,
        size_t size, void *data);
    virtual Method_Handle lookupCodeChunk(void *addr, Boolean is_ip_past,
        void **code_addr, size_t *size, void **data);
    virtual Boolean unregisterCodeChunk(void *addr);
    virtual unsigned int getNumProfilerThreads() const { return tbsClients.empty() ? 0 : 1;}

    virtual void classloaderUnloadingCallback(Class_Loader_Handle class_handle); 

//EM_PC interface impl:
    virtual void methodProfileIsReady(MethodProfile* mp);

    virtual bool needTbsThreadSupport() const;
    virtual void tbsTimeout();
    virtual int getTbsTimeout() const;

//EM PC access interface
    ProfileCollector* getProfileCollector(EM_PCTYPE type, JIT_Handle jh, EM_JIT_PC_Role jitRole) const;

private:
    void initProfileAccess();
    std::string readConfiguration();
    void buildChains(std::string& config);
    bool initJIT(const std::string& libName, apr_dso_handle_t* libHandle, RStep& step);
    bool initProfileCollectors(RChain* chain, const std::string& config);
    ProfileCollector* createProfileCollector(const std::string& profilerName, const std::string& config, RStep* step);
    ProfileCollector* getProfileCollector(const std::string& name) const;
    std::string getJITLibFromCmdLine(const std::string& jitName) const;

    void deallocateResources();
    
    
    JIT_Handle jh;
    void (*_execute_method) (JIT_Handle jit, jmethodID method, jvalue *return_value, jvalue *args);
    RChains chains;
    size_t nMethodsCompiled, nMethodsRecompiled;
    
    ProfileCollectors collectors;
    TbsClients tbsClients;
    
    EM_ProfileAccessInterface profileAccessInterface;

    U_32 tick;
    
    osmutex_t recompilationLock;
    std::set<Method_Profile_Handle> methodsInRecompile;

    Method_Lookup_Table method_lookup_table;
};

#endif
