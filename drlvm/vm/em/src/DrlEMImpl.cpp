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
#define LOG_DOMAIN "em"
#include "cxxlog.h"

#include "DrlEMImpl.h"

#include "EBProfileCollector.h"
#include "EdgeProfileCollector.h"
#include "NValueProfileCollector.h"

#include "open/vm_properties.h"
#include "open/vm_ee.h"
#include "open/ee_em_intf.h"
#include "em_intf.h"
#include "open/vm.h"
#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"
#include "ini.h"

#include "vm_properties.h"

#include <algorithm>
#include <sstream>
#include <fstream>
#include "port_threadunsafe.h"
#include "port_mutex.h"

#define EDGE_PROFILER_STR  "EDGE_PROFILER"
#define VALUE_PROFILER_STR  "VALUE_PROFILER"
#define ENTRY_BACKEDGE_PROFILER_STR  "EB_PROFILER"

#define EM_CONFIG_EXT std::string(".emconf")

DrlEMImpl* DrlEMFactory::emInstance = NULL;

// TODO you may get a question why we get the same execution manager
// for multiple VM instances the proper way is to get rid of static emInstance
DrlEMImpl* DrlEMFactory::createAndInitEMInstance() {
    if (emInstance == NULL) {
        emInstance = new DrlEMImpl();
        if (!emInstance->init()) {
            delete emInstance;
            emInstance = NULL;
        }
    }
    return emInstance;
}

DrlEMImpl* DrlEMFactory::getEMInstance() {
    assert(emInstance!=NULL);
    return emInstance;
}   

void DrlEMFactory::deinitEMInstance() {
    assert(emInstance!=NULL);
    emInstance->deinit();
    delete emInstance;
    emInstance = NULL;
}

static EM_PCTYPE get_pc_type(EM_Handle _this, PC_Handle pc) {
    assert(_this!=NULL);
    assert(pc!=NULL);
    return ((ProfileCollector*)pc)->type;
}

static PC_Handle get_pc(EM_Handle _this,  EM_PCTYPE profile_type,  JIT_Handle jh,  EM_JIT_PC_Role jit_role) {
    assert(_this!=NULL);
    assert(jh!=NULL);
    DrlEMImpl* em = (DrlEMImpl*) _this;
    return (PC_Handle)em->getProfileCollector(profile_type, jh, jit_role);
}

static Method_Profile_Handle 
get_method_profile(EM_Handle _this, PC_Handle pch, Method_Handle mh) {
    assert(_this!=NULL);
    assert(pch!=NULL);
    assert(mh!=NULL);
    ProfileCollector* pc  = (ProfileCollector*)pch;
    return (Method_Profile_Handle)pc->getMethodProfile(mh);
}


RStep::RStep(JIT_Handle _jit, const std::string& _jitName, RChain* _chain, apr_dso_handle_t* _libHandle)
: jit(_jit), jitName(_jitName), catName(std::string(LOG_DOMAIN)+"."+_jitName), 
chain(_chain), loggingEnabled(false), enable_profiling(NULL),
libHandle(_libHandle)
{}


//todo!! replace inlined strings with defines!!
DrlEMImpl::DrlEMImpl() : jh(NULL), _execute_method(NULL), method_lookup_table() {
    nMethodsCompiled=0;
    nMethodsRecompiled=0;
    tick=0;
    port_mutex_create(&recompilationLock, APR_THREAD_MUTEX_NESTED);
    initProfileAccess();
}

DrlEMImpl::~DrlEMImpl() {
    deallocateResources();
    port_mutex_destroy(&recompilationLock);
}

void DrlEMImpl::initProfileAccess() {
    profileAccessInterface.get_pc_type = get_pc_type;
    profileAccessInterface.get_pc = get_pc;
    profileAccessInterface.get_method_profile =  get_method_profile;

    //EB profile
    profileAccessInterface.eb_profiler_create_profile = eb_profiler_create_profile;
    profileAccessInterface.eb_profiler_get_entry_counter_addr=eb_profiler_get_entry_counter_addr;
    profileAccessInterface.eb_profiler_get_backedge_counter_addr = eb_profiler_get_backedge_counter_addr;
    profileAccessInterface.eb_profiler_is_in_sync_mode= eb_profiler_is_in_sync_mode;
    profileAccessInterface.eb_profiler_get_entry_threshold = eb_profiler_get_entry_threshold;
    profileAccessInterface.eb_profiler_sync_mode_callback = (void (*)(Method_Profile_Handle))vm_create_helper_for_function((void *(*)(void *))eb_profiler_sync_mode_callback);
    profileAccessInterface.eb_profiler_get_backedge_threshold = eb_profiler_get_backedge_threshold;

    
    //EDGE profile
    profileAccessInterface.edge_profiler_create_profile = edge_profiler_create_profile;
    profileAccessInterface.edge_profiler_get_num_counters = edge_profiler_get_num_counters;
    profileAccessInterface.edge_profiler_get_checksum = edge_profiler_get_checksum;
    profileAccessInterface.edge_profiler_get_counter_addr = edge_profiler_get_counter_addr;
    profileAccessInterface.edge_profiler_get_entry_counter_addr = edge_profiler_get_entry_counter_addr;
    profileAccessInterface.edge_profiler_get_entry_threshold = edge_profiler_get_entry_threshold;
    profileAccessInterface.edge_profiler_get_backedge_threshold = edge_profiler_get_backedge_threshold;
    
    
    //Value profile
    profileAccessInterface.value_profiler_create_profile = value_profiler_create_profile;
    profileAccessInterface.value_profiler_add_value = value_profiler_add_value;
    profileAccessInterface.value_profiler_get_top_value = value_profiler_get_top_value;
    profileAccessInterface.value_profiler_dump_values = value_profiler_dump_values;
    
    return;
}



void DrlEMImpl::deallocateResources() {
    tbsClients.clear();

    for (RChains::const_iterator cit = chains.begin(), cend = chains.end(); cit!=cend; ++cit) {
        RChain* chain = *cit;
        for (RSteps::const_iterator sit = chain->steps.begin(), send = chain->steps.end(); sit!=send; ++sit) {
            RStep* step = *sit;
            //todo:call deinit on JIT instance
            delete step;
        }
        delete chain;
    }
    chains.clear();

    for (ProfileCollectors::iterator it = collectors.begin(), end = collectors.end(); it!=end; ++it) {
        ProfileCollector* pc = *it;
        delete pc;
    }    
    collectors.clear();
}

//_____________________________________________________________________
// Reading and parsing configuration

static bool endsWith(const std::string& str, const std::string& suffix) {
    if (str.length() < suffix.length()) {
        return false;
    }
    return std::equal(suffix.rbegin(), suffix.rend(), str.rbegin());
}
    
std::string prepareLibPath(const std::string& origPath) {
#ifdef PLATFORM_NT
    std::string separator("\\"), libPrefix(""), libSuffix(".dll");
#else
    std::string separator("/"), libPrefix("lib"), libSuffix(".so");
#endif

    std::string path = origPath;
    if (path.find('/') == path.npos && path.find('\\') == path.npos ) {
        char* c_string_tmp_value = vm_properties_get_value(O_A_H_VM_VMDIR, JAVA_PROPERTIES);
        std::string dir = c_string_tmp_value == NULL ? "" : c_string_tmp_value;
        vm_properties_destroy_value(c_string_tmp_value);
        if (libPrefix.length() > 0 && !startsWith(path, libPrefix)) {
            path = libPrefix + path;
        }
        
        if (!dir.empty()) {
            path = dir + separator + path;
        }
    }
    if (!endsWith(path, libSuffix)) {
        path+=libSuffix;
    }
    return path;
  }



static std::string getParam(const std::string& config, const std::string& name) {
    std::istringstream is(config);
    std::string propPrefix = name+"=";
    size_t prefixLen = propPrefix.length();
    std::string line;
    while (std::getline(is, line)) {
        if (line.length() > prefixLen && std::equal(propPrefix.begin(),propPrefix.end(), line.begin())) {
            return line.substr(prefixLen);
        }
    }
    return "";
}


typedef std::vector<std::string> StringList;
static StringList getParamAsList(const std::string& config, const std::string& name, char listSeparator, bool notEmpty) {
    std::string value = getParam(config, name);
    StringList res;
    std::string token;
    for (std::string::const_iterator it = value.begin(), end = value.end(); it!=end; ++it) {
        char c = *it;
        if (c == listSeparator) {
            if (token.empty() && notEmpty) {
                continue;
            }
            res.push_back(token);
            token.clear();
        } else {
            token.push_back(c);
        }
    }
    if (!token.empty()) { //last value
        res.push_back(token);
    }
    return res;
}

static StringList getAllParamsAsList(const std::string& config, const std::string& name) {
    StringList res;
    std::istringstream is(config);
    std::string propPrefix = name+"=";
    size_t prefixLen = propPrefix.length();
    std::string line;
    while (std::getline(is, line)) {
        if (line.length() > prefixLen && std::equal(propPrefix.begin(),propPrefix.end(), line.begin())) {
            res.push_back(line.substr(prefixLen));
        }
    }
    return res;
}


static std::string readFile(const std::string& fileName) {
    std::string config;
    std::ifstream configFile;
    configFile.open(fileName.c_str(), std::ios::in);
    bool rc = false;
    if (configFile.is_open()) {
        std::string line;
        size_t idx = std::string::npos;
        while (getline(configFile, line)) {
            if (startsWith(line, "#")) {
                continue;
            } else if (startsWith(line, "-D") && (idx = line.find('=')) != std::string::npos) {
                std::string name = line.substr(2, idx-2);                   
                std::string value = line.substr(idx+1);
                if (!vm_property_is_set(name.c_str(), JAVA_PROPERTIES)) {
                    vm_properties_set_value(name.c_str(), value.c_str(), JAVA_PROPERTIES);
                }
                continue;
            } else if (startsWith(line, "-XD") && (idx = line.find('=')) != std::string::npos) {
                std::string name = line.substr(3, idx-3);                   
                std::string value = line.substr(idx+1);
                if (!vm_property_is_set(name.c_str(),VM_PROPERTIES)) {
                    vm_properties_set_value(name.c_str(), value.c_str(), VM_PROPERTIES);
                }
                continue;
            } else if (startsWith(line, "-XX:") && line.length() >= 5) {
                idx = line.find('=');
                std::string name;
                std::string value;
                if (idx != std::string::npos) {
                    name = line.substr(4, idx-4);                   
                    value = line.substr(idx+1);
                } else {
                    if (line[4] == '-' ) {
                        value = "off";
                        name = line.substr(5);
                    } else if (line[4] == '+') {
                        value = "on";
                        name = line.substr(5);
                    } else {
                        value = "";
                        name = line.substr(4);
                    }
                }

                if (!vm_property_is_set(name.c_str(),VM_PROPERTIES)) {
                    vm_properties_set_value(name.c_str(), value.c_str(), VM_PROPERTIES);
                }
                continue;
            } 
 
            config+=line + "\n";
        }
        rc = !config.empty();
    } 
    if (!rc) {
        LECHO(1, "EM: Can't read configuration from '{0}'" << fileName.c_str());
    }
    return config;
}

std::string DrlEMImpl::readConfiguration() {
    char* c_string_tmp_value = vm_properties_get_value("em.properties", VM_PROPERTIES);
    std::string  configFileName = c_string_tmp_value == NULL ? "" : c_string_tmp_value;
    vm_properties_destroy_value(c_string_tmp_value);
    if (configFileName.empty()) {
        bool jitTiMode = vm_property_get_boolean("vm.jvmti.enabled", FALSE, VM_PROPERTIES);
        bool interpreterMode = vm_property_get_boolean("vm.use_interpreter", FALSE, VM_PROPERTIES);
        configFileName = interpreterMode ? "interpreter" : (jitTiMode ? "ti" : "client");
    } 
    if (!endsWith(configFileName, EM_CONFIG_EXT)) {
        configFileName = configFileName+EM_CONFIG_EXT;
    }

    if (configFileName.find('/') == configFileName.npos && configFileName.find('\\') == configFileName.npos ) {
        c_string_tmp_value = vm_properties_get_value(O_A_H_VM_VMDIR, JAVA_PROPERTIES);
        std::string defaultConfigDir = c_string_tmp_value == NULL ? "" : c_string_tmp_value;
        vm_properties_destroy_value(c_string_tmp_value);
        
        configFileName = defaultConfigDir + "/" + configFileName;
    }
    std::string config = readFile(configFileName);
    return config;
}

//_______________________________________________________________________
// EM initialization methods

bool DrlEMImpl::init() {
    _execute_method = JIT_execute_method_default;

    std::string config = readConfiguration();
    if (!config.empty()) {
        buildChains(config);
    }
    return !chains.empty();
}


static bool enable_profiling_stub(JIT_Handle jit, PC_Handle pc, EM_JIT_PC_Role role) {
    return false;
}

static void profile_notification_callback_stub(JIT_Handle, PC_Handle, Method_Handle) {
}

bool DrlEMImpl::initJIT(const std::string& libName, apr_dso_handle_t* libHandle, RStep& step) {
    apr_dso_handle_sym_t fn = NULL;
    if (apr_dso_sym(&fn, libHandle, "JIT_init") != APR_SUCCESS) {
        LECHO(2, "EM: Not a JIT shared lib: '{0}'" << libName.c_str());
        return false;
    }

    void (*_init)(JIT_Handle, const char*, vm_adaptor_t) = 
        (void (*)(JIT_Handle, const char*, vm_adaptor_t)) fn;
    _init(step.jit, step.jitName.c_str(), vm_get_interface);

    bool pcEnabled = false;
    if (apr_dso_sym(&fn, libHandle, "JIT_set_profile_access_interface") == APR_SUCCESS) {
        pcEnabled =  true;
        void (*_setPAInterface)(JIT_Handle, EM_Handle, struct EM_ProfileAccessInterface*) = (void (*)(JIT_Handle, EM_Handle, struct EM_ProfileAccessInterface*))fn;
        _setPAInterface(step.jit, (EM_Handle)this, &profileAccessInterface);
    } 

    if (pcEnabled && apr_dso_sym(&fn, libHandle, "JIT_enable_profiling") == APR_SUCCESS) {
        step.enable_profiling = (bool(*)(JIT_Handle, PC_Handle, EM_JIT_PC_Role))fn;
    } else {
        step.enable_profiling = enable_profiling_stub;
    }

    if (pcEnabled && apr_dso_sym(&fn, libHandle, "JIT_profile_notification_callback") == APR_SUCCESS) {
        step.profile_notification_callback = (void(*)(JIT_Handle, PC_Handle, Method_Handle))fn;
    } else {
        step.profile_notification_callback = profile_notification_callback_stub;
    }

    return true;
}

std::string DrlEMImpl::getJITLibFromCmdLine(const std::string& jitName) const {
    std::string propName = std::string("em.")+jitName+".jitPath";
    char* c_string_tmp_value = vm_properties_get_value(propName.c_str(), VM_PROPERTIES);
    std::string jitLib  = c_string_tmp_value == NULL ? "" : c_string_tmp_value;
    vm_properties_destroy_value(c_string_tmp_value);
    if (jitLib.empty()) {
        c_string_tmp_value = vm_properties_get_value("em.jitPath", VM_PROPERTIES);
        jitLib = c_string_tmp_value == NULL ? "" : c_string_tmp_value;
        vm_properties_destroy_value(c_string_tmp_value);
    }
    return jitLib;
}

void DrlEMImpl::buildChains(std::string& config) {
    bool loggingEnabled =  log_is_info_enabled(LOG_DOMAIN);
    StringList chainNames = getParamAsList(config, "chains", ',', true);
    if (chainNames.empty()) {
        LECHO(3, "EM: No 'chains' property found in configuration");
        return;
    }
    bool failed = false;
    for (StringList::const_iterator chainIt = chainNames.begin(), chainEnd = chainNames.end(); chainIt!=chainEnd; ++chainIt) {
        std::string chainName = *chainIt;
        if (std::count(chainNames.begin(), chainNames.end(), chainName)!=1) {
            failed = true;
            break;
        }
        RChain* chain = new RChain();
        chains.push_back(chain);
        StringList jitsInChain= getParamAsList(config, chainName + ".jits", ',', true);
        for (StringList::const_iterator jitIt = jitsInChain.begin(), jitEnd = jitsInChain.end(); jitIt!=jitEnd; ++jitIt) {
            std::string jitName= *jitIt;
            std::string jitLib = getJITLibFromCmdLine(jitName);
            if (jitLib.empty()) {
                jitLib = getParam(config, jitName+".file");
            } 
            if (jitLib.empty()) {
                LECHO(4, "EM: No JIT library specified for JIT :'{0}'" << jitLib.c_str());
                failed = true;
                break;
            }
            std::string fullJitLibPath = prepareLibPath(jitLib);
            apr_dso_handle_t* libHandle;
            JIT_Handle jh = vm_load_jit(fullJitLibPath.c_str(), &libHandle); //todo: do not load the same dll twice!!!
            if (jh == NULL) {
                LECHO(5, "EM: JIT library loading error:'{0}'" << fullJitLibPath.c_str());
                failed = true;
                break;
            }
            RStep* step = new RStep(jh, jitName, chain, libHandle);
            step->loggingEnabled = loggingEnabled || log_is_info_enabled(step->catName.c_str());
            chain->steps.push_back(step);

            if (!initJIT(fullJitLibPath, libHandle, *step)) {
                failed = true;
                break;
            }
        }
        failed = failed || chain->steps.empty();
        if (!failed) { 
            // reading chain filters
            StringList filters = getAllParamsAsList(config, chainName+".filter");
            for (StringList::const_iterator filterIt = filters.begin(), filterEnd = filters.end(); filterIt!=filterEnd; ++filterIt) {
                const std::string& filter = *filterIt;
                bool res = chain->addMethodFilter(filter);
                if (!res) {
                    LECHO(6, "EM: Invalid filter :'{0}'" << filter.c_str());
                }
            }
        }
        if (failed) {
            break;
        }
    }
    if (!failed) { //initialize profile collectors
        for (RChains::const_iterator it = chains.begin(), end = chains.end(); it!=end; ++it) {
            RChain* chain = *it;
            failed = !initProfileCollectors(chain, config);
            if (failed) {
                break;
            }
        }
    }

    //The next lines do the following:
    //If we have only 1 execution engine in CFG - check if it have JIT_execute_method
    //This way interpreter mode is supported today.
    //This code must be refactored when mixed (JIT/interpreter) mode is implemented
    if (!failed && chains.size()==1) {
        RChain* chain = *chains.begin();
        assert(chain->steps.size() > 0);
        RStep* step = *chain->steps.begin();
        
        apr_dso_handle_sym_t fn = NULL;
        apr_dso_sym(&fn, step->libHandle, "JIT_execute_method");
        if (fn!=NULL) {
            _execute_method = (void(*)(JIT_Handle,jmethodID, jvalue*, jvalue*)) fn;    
        }
    }


    if (failed) {
        deallocateResources();
    }
}

void DrlEMImpl::deinit() {
}

//______________________________________________________________________________
// EM runtime
void DrlEMImpl::executeMethod(jmethodID meth, jvalue  *return_value, jvalue *args) {
    //do not choose JIT here, this method will call-back from vm with compileMethod request.
    _execute_method(0, meth, return_value, args);
}

JIT_Result DrlEMImpl::compileMethod(Method_Handle mh) {
    //initial method compilation. Select chain to use.

    UNSAFE_REGION_START
    // May affect logging statistics, also used in debug-intended method filter
    // Does not affect normal execution
    nMethodsCompiled++;
    UNSAFE_REGION_END

    //these vars are used for logging
    const char* methodName = NULL;
    const char* className = NULL;
    const char* signature = NULL;
    size_t n = nMethodsCompiled;

    assert(!chains.empty());
    for (RChains::const_iterator it = chains.begin(), end = chains.end(); it!=end; ++it) {
        RChain* chain = *it;
        if (chain->acceptMethod(mh, nMethodsCompiled)) {
            assert(!chain->steps.empty());
            for (size_t i=0;i<chain->steps.size(); i++) {
                RStep* step = chain->steps[i];
                if (step->loggingEnabled) {
                    methodName = method_get_name(mh);
                    Class_Handle ch = method_get_class(mh);
                    className = class_get_name(ch);
                    signature = method_get_descriptor(mh);
                    std::ostringstream msg;
                    msg <<"EM: compile start:["<<step->jitName.c_str()<<" n="<<n<<"] "
                        <<className<<"."<<methodName<<signature;
                    INFO2(step->catName.c_str(), msg.str().c_str());
                }

                JIT_Result res = vm_compile_method(step->jit, mh);

                if (step->loggingEnabled) {
                    std::ostringstream msg;
                    msg << "EM: compile done:["<<step->jitName.c_str()<<" n="<<n<<": "
                        <<(res ==JIT_SUCCESS ? "OK" : "FAILED")<<"] "<<className<<"."<<methodName<<signature;
                    INFO2(step->catName.c_str(), msg.str().c_str());
                }

                if (res == JIT_SUCCESS) {
                    return JIT_SUCCESS;
                }
            }
        }
    }
    return JIT_FAILURE;
}



//______________________________________________________________________________
// Profile collectors initialization and recompilation

static U_32 toNum(const std::string& numStr, bool *rc ) {
    if (isNum(numStr)) {
        *rc = true;
        return atoi(numStr.c_str());
    }
    *rc = false;
    return 0;
}

ProfileCollector* DrlEMImpl::createProfileCollector(const std::string& profilerName, const std::string& config, RStep* step)  {
    ProfileCollector* pc = getProfileCollector(profilerName);
    if (pc != NULL){
        return NULL;
    }    
    std::string profilerType = getParam(config, profilerName+".profilerType");
    if (profilerType!=ENTRY_BACKEDGE_PROFILER_STR && profilerType!=EDGE_PROFILER_STR  && profilerType!=VALUE_PROFILER_STR) {
        LECHO(7, "EM: Unsupported profiler type");
        return NULL;
    }

    if (profilerType == ENTRY_BACKEDGE_PROFILER_STR || profilerType == EDGE_PROFILER_STR) {
        EBProfileCollector::EB_ProfilerMode ebMode = EBProfileCollector::EB_PCMODE_SYNC;
        std::string mode = profilerType==EDGE_PROFILER_STR ? "ASYNC" : getParam(config, profilerName+".mode");
        
        if (mode == "ASYNC") {
            ebMode = EBProfileCollector::EB_PCMODE_ASYNC;
        }  else if (mode!="SYNC") {
            LECHO(8, "EM: unsupported profiler mode");
            return NULL;
        }
        
        bool ok = false;
        U_32 eThreshold = toNum(getParam(config, profilerName+".entryThreshold"), &ok);//todo: default values..
            if (!ok) {
            LECHO(9, "EM: illegal '{0}' value" << "entryThreshold");
            return NULL;
            }
                U_32 bThreshold = toNum(getParam(config, profilerName+".backedgeThreshold"), &ok);
        if (!ok) {
            LECHO(9, "EM: illegal '{0}' value" << "backedgeThreshold");
            return NULL;
        }
        U_32 tbsTimeout = 0, tbsInitialTimeout = 0;
        if (ebMode == EBProfileCollector::EB_PCMODE_ASYNC) {
            tbsTimeout= toNum(getParam(config, profilerName+".tbsTimeout"), &ok);
            if (!ok) {
                LECHO(9, "EM: illegal '{0}' value" << "tbsTimeout");
                return NULL;
                }
            tbsInitialTimeout= toNum(getParam(config, profilerName+".tbsInitialTimeout"), &ok);
            if (!ok) {
                LECHO(9, "EM: illegal '{0}' value" << "tbsInitialTimeout");
                return NULL;
            }
        }
        if (profilerType == EDGE_PROFILER_STR) {
            pc = new EdgeProfileCollector(this, profilerName, step->jit, tbsInitialTimeout, tbsTimeout, eThreshold, bThreshold);
        } else {
            pc = new EBProfileCollector(this, profilerName, step->jit, ebMode, eThreshold, bThreshold, tbsInitialTimeout, tbsTimeout);
        }
    } else if (profilerType == VALUE_PROFILER_STR) {
        typedef ValueProfileCollector VPC;
        int vpSteadySize = 4, vpClearSize = 0, vpClearInterval = 0;
        std::string vpalgo = getParam(config, profilerName+".vpalgo");
        VPC::algotypes vpMode = VPC::TNV_FIRST_N;
        if (vpalgo == "TNV_DIVIDED") {
            vpMode = VPC::TNV_DIVIDED;
        } else if (vpalgo != "TNV_FIRST_N") {
            LECHO(10, "EM: unsupported value profiler algotype");
            return NULL;
        }

        std::string strategy = getParam(config, profilerName+".updateStrategy");
        ProfileUpdateStrategy updateStrategy = UPDATE_LOCKED;
        if (strategy == "FLAGGED_ALL") {
            updateStrategy = UPDATE_FLAGGED_ALL;
        }else if (strategy == "FLAGGED_INSERT") {
            updateStrategy = UPDATE_FLAGGED_INSERT;
        }else if (strategy == "UNSAFE") {
            updateStrategy = UPDATE_UNSAFE;
        }else if (strategy == "LOCKED_INSERT") {
            updateStrategy = UPDATE_LOCKED_INSERT;
        }else if (strategy != "LOCKED") {
            LECHO(10, "EM: unsupported value profiler updateStrategy");
            return NULL;
        }

        if (vpMode != VPC::TNV_FIRST_N && updateStrategy != UPDATE_LOCKED) {
            LECHO(10, "EM: unsupported value profiler combination of "
                    "vpalgo and updateStrategy, only TNV_FIRST_N vpalgo "
                    "curretly sypports other than LOCKED strategy");
            return NULL;
        }

        bool ok = false;
        vpSteadySize = toNum(getParam(config, profilerName+".vpSteadySize"), &ok);
        if (!ok) {
            LECHO(9, "EM: illegal '{0}' value" << "SteadySize");
            return NULL;
        }
        if (vpMode == VPC::TNV_DIVIDED) {
            vpClearSize = toNum(getParam(config, profilerName+".vpClearSize"), &ok);
            if (!ok) {
                LECHO(9, "EM: illegal '{0}' value" << "ClearSize");
                return NULL;
            }
            vpClearInterval = toNum(getParam(config, profilerName+".vpClearInterval"), &ok);
            if (!ok) {
                LECHO(9, "EM: illegal '{0}' value" << "ClearInterval");
                return NULL;
            }
        }
        pc = new ValueProfileCollector(this, profilerName, step->jit, vpSteadySize,
                vpClearSize, vpClearInterval, vpMode, updateStrategy);
    }
    return pc;
}

ProfileCollector* DrlEMImpl::getProfileCollector(const std::string& name) const {
    for (ProfileCollectors::const_iterator it = collectors.begin(), end = collectors.end(); it!=end; ++it) {
        ProfileCollector* c = *it;
        if (c->name == name) {
            return c;
        }
    }
    return NULL;
}

bool DrlEMImpl::initProfileCollectors(RChain* chain, const std::string& config) {
    bool failed = false;
    for (RSteps::const_iterator it = chain->steps.begin(), end = chain->steps.end(); it!=end; ++it) {
        RStep* step = *it;
        StringList genProfNames = getParamAsList(config, step->jitName + ".genProfile", ',', false);
        for (StringList::const_iterator profIt = genProfNames.begin(), profEnd = genProfNames.end(); profIt!=profEnd; ++profIt) {
            std::string profilerName = *profIt;
            if (!profilerName.empty()) {
                ProfileCollector* pc = createProfileCollector(profilerName, config, step);
                if (pc == NULL) {
                    LECHO(11, "EM: profile configuration failed: {0}" << profilerName.c_str());
                    failed = true;
                    break;
                }
                bool genOk = step->enable_profiling(step->jit, (PC_Handle)pc, EM_JIT_PROFILE_ROLE_GEN);
                if (genOk) {
                    collectors.push_back(pc);
                    TbsEMClient* tbsClient = pc->getTbsEmClient();
                    if (tbsClient!=NULL) {
                        assert(tbsClient->getTimeout() != 0 && tbsClient->getTimeout()!=0);
                        tbsClient->setNextTick(tbsClient->getInitialTimeout());
                        tbsClients.push_back(tbsClient);
                    }
                } else {
                    LECHO(12, "EM: profile generation is not supported: {0}" << profilerName.c_str());
                    delete pc;
                    failed = true;
                    break;
                }

            }
        }
        StringList useProfNames = getParamAsList(config, step->jitName + ".useProfile", ',', false);
        for (StringList::const_iterator profIt = useProfNames.begin(), profEnd = useProfNames.end(); profIt!=profEnd; ++profIt) {
            std::string profilerName = *profIt;
            if (!profilerName.empty()) {
                ProfileCollector* pc = getProfileCollector(profilerName);
                bool invalidChain = true;
                if (pc!=NULL) {
                    for(RSteps::const_iterator it2=chain->steps.begin(); it2 <it; ++it2) {
                        RStep* prevStep = *it2;
                        if (prevStep->jit == pc->genJit) {
                            invalidChain = false;
                            break;
                        }
                    }
                }
                bool useOk = !invalidChain && (pc!=NULL && step->enable_profiling(step->jit, (PC_Handle)pc, EM_JIT_PROFILE_ROLE_USE));
                if (useOk) {
                    pc->addUseJit(step->jit);
                } else {
                    if (pc == NULL) {
                        LECHO(13, "EM: profile not found: {0}" << profilerName.c_str());
                    } else if (invalidChain) {
                        LECHO(14, "EM: illegal use of profile: {0}" << profilerName.c_str());
                    } else {
                        LECHO(15, "EM: profile usage is not supported: {0}" << profilerName.c_str());
                    }
                }
            }
        }
    }
    if (!failed && !tbsClients.empty()) {
        //timer.start(this);
    }
    return !failed;
}

void DrlEMImpl::methodProfileIsReady(MethodProfile* mp) {
    
    port_mutex_lock(&recompilationLock);
    if (methodsInRecompile.find((Method_Profile_Handle)mp)!=methodsInRecompile.end()) {
        //method is already recompiling by another thread or by this thread(recursion)
        port_mutex_unlock(&recompilationLock);
        return;
    }
    
    methodsInRecompile.insert((Method_Profile_Handle)mp);
    nMethodsRecompiled++;
    port_mutex_unlock(&recompilationLock);

    const char* methodName = NULL;
    const char* className = NULL;
    const char* signature = NULL;
    size_t n = nMethodsRecompiled;

    JIT_Handle jit = mp->pc->genJit;
    for (RChains::const_iterator it = chains.begin(), end = chains.end(); it!=end; ++it) {
        RChain* chain = *it;
        for (RSteps::const_iterator sit = chain->steps.begin(), send = chain->steps.end(); sit!=send; ++sit) {
            RStep* step = *sit;
            if (step->jit == jit) {

                //notify JIT which has generated this profile that profile is ready.
                step->profile_notification_callback(step->jit, mp->pc, mp->mh);

                ++sit;
                RStep* nextStep = sit!=send ? *sit: NULL;
                if (nextStep != NULL) {
                    if (nextStep->loggingEnabled) {
                        methodName = method_get_name(mp->mh);
                        Class_Handle ch = method_get_class(mp->mh);
                        className = class_get_name(ch);
                        signature = method_get_descriptor(mp->mh);
                        std::ostringstream msg;
                        msg <<"EM: recompile start:["<<nextStep->jitName.c_str()<<" n="<<n<<"] "
                            <<className<<"."<<methodName<<signature;
                        INFO2(nextStep->catName.c_str(), msg.str().c_str());
                    } 

                    JIT_Result res = vm_compile_method(nextStep->jit, mp->mh);

                    if (nextStep->loggingEnabled) {
                        std::ostringstream msg;
                        msg << "EM: recompile done:["<<nextStep->jitName.c_str()<<" n="<<n<<": "
                            <<(res ==JIT_SUCCESS ? "OK" : "FAILED")<<"] "<<className<<"."<<methodName<<signature;
                        INFO2(nextStep->catName.c_str(), msg.str().c_str());
                    }

                }
            }
        }
    }
    port_mutex_lock(&recompilationLock);
    methodsInRecompile.erase((Method_Profile_Handle)mp);
    port_mutex_unlock(&recompilationLock);
}

ProfileCollector* DrlEMImpl::getProfileCollector(EM_PCTYPE type, JIT_Handle jh, EM_JIT_PC_Role jitRole) const {
    for (ProfileCollectors::const_iterator it = collectors.begin(), end = collectors.end(); it!=end; ++it) {
        ProfileCollector* pc = *it;
        if (pc->type == type) {
            bool matched = false;
            if (jitRole == EM_JIT_PROFILE_ROLE_GEN) {
                matched =  pc->genJit == jh;
            } else {
                matched =  std::find(pc->useJits.begin(), pc->useJits.end(), jh)!=pc->useJits.end();
            }
            if (matched) {
                return pc;
            }
        }
    }
    return NULL;
}

bool DrlEMImpl::needTbsThreadSupport() const {
    return !tbsClients.empty();
}

void DrlEMImpl::tbsTimeout() {
    for (TbsClients::const_iterator it = tbsClients.begin(), end = tbsClients.end(); it!=end; ++it) {
        TbsEMClient* c = *it;
        if (c->getNextTick() == tick) {
//            printf("tick!\n");
            c->onTimeout();
            c->setNextTick(tick + c->getTimeout());
        }
    }
    tick++;
}

int DrlEMImpl::getTbsTimeout() const {
    return 100;
}   



void DrlEMImpl::classloaderUnloadingCallback(Class_Loader_Handle class_handle) {
    //notify every profile collector about classloader unloading
}

void DrlEMImpl::registerCodeChunk(Method_Handle method_handle, void *code_addr,
    size_t size, void *data)
{
    method_lookup_table.add(method_handle, code_addr, size, data);
}

Method_Handle DrlEMImpl::lookupCodeChunk(void *ip, Boolean is_ip_past,
    void **code_addr, size_t *size, void **data)
{
    return method_lookup_table.find(ip, is_ip_past, code_addr, size, data);
}

Boolean DrlEMImpl::unregisterCodeChunk(void *addr)
{
    return method_lookup_table.remove(addr);
}
