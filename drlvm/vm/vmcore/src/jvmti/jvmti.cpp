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
 * @author Gregory Shimansky
 */  
/*
 * See official specification at:
 * http://java.sun.com/j2se/1.5.0/docs/guide/jvmti/jvmti.html.
 *
 */

#define LOG_DOMAIN "jvmti"
#include "cxxlog.h"

#include "jvmti.h"
#include "jvmti_internal.h"
#include "jvmti_utils.h"
#include "open/vm_properties.h"
#include "open/vm_util.h"
#include "environment.h"
#include <string.h>
//#include "properties.h"
#include "jvmti_break_intf.h"
#include "interpreter_exports.h"

#include "port_filepath.h"
#include "port_dso.h"
#include "port_mutex.h"
#include <apr_strings.h>
#include <apr_atomic.h>

#if defined(PLATFORM_NT) && !defined(_WIN64)
#define AGENT_ONLOAD1 "_Agent_OnLoad@12"
#define AGENT_ONUNLOAD1 "_Agent_OnUnload@4"
#define JVM_ONLOAD1 "_JVM_OnLoad@12"
#define JVM_ONUNLOAD1 "_JVM_OnUnLoad@4"

#define AGENT_ONLOAD2 "Agent_OnLoad"
#define AGENT_ONUNLOAD2 "Agent_OnUnload"
#define JVM_ONLOAD2 "JVM_OnLoad"
#define JVM_ONUNLOAD2 "JVM_OnUnLoad"
#else
#define AGENT_ONLOAD1 "Agent_OnLoad"
#define AGENT_ONUNLOAD1 "Agent_OnUnload"
#define JVM_ONLOAD1 "JVM_OnLoad"
#define JVM_ONUNLOAD1 "JVM_OnUnLoad"

#define AGENT_ONLOAD2 NULL
#define AGENT_ONUNLOAD2 NULL
#define JVM_ONLOAD2 NULL
#define JVM_ONUNLOAD2 NULL
#endif

static void JNICALL jvmtiUnimpStub(JNIEnv*);
Agent *current_loading_agent;

const struct ti_interface jvmti_table =
{
    (void *)jvmtiUnimpStub,
    jvmtiSetEventNotificationMode,
    (void *)jvmtiUnimpStub,
    jvmtiGetAllThreads,
    jvmtiSuspendThread,
    jvmtiResumeThread,
    jvmtiStopThread,
    jvmtiInterruptThread,
    jvmtiGetThreadInfo,
    jvmtiGetOwnedMonitorInfo,
    jvmtiGetCurrentContendedMonitor,
    jvmtiRunAgentThread,
    jvmtiGetTopThreadGroups,
    jvmtiGetThreadGroupInfo,
    jvmtiGetThreadGroupChildren,
    jvmtiGetFrameCount,
    jvmtiGetThreadState,
    (void *)jvmtiUnimpStub,
    jvmtiGetFrameLocation,
    jvmtiNotifyFramePop,
    jvmtiGetLocalObject,
    jvmtiGetLocalInt,
    jvmtiGetLocalLong,
    jvmtiGetLocalFloat,
    jvmtiGetLocalDouble,
    jvmtiSetLocalObject,
    jvmtiSetLocalInt,
    jvmtiSetLocalLong,
    jvmtiSetLocalFloat,
    jvmtiSetLocalDouble,
    jvmtiCreateRawMonitor,
    jvmtiDestroyRawMonitor,
    jvmtiRawMonitorEnter,
    jvmtiRawMonitorExit,
    jvmtiRawMonitorWait,
    jvmtiRawMonitorNotify,
    jvmtiRawMonitorNotifyAll,
    jvmtiSetBreakpoint,
    jvmtiClearBreakpoint,
    (void *)jvmtiUnimpStub,
    jvmtiSetFieldAccessWatch,
    jvmtiClearFieldAccessWatch,
    jvmtiSetFieldModificationWatch,
    jvmtiClearFieldModificationWatch,
    (void *)jvmtiUnimpStub,
    jvmtiAllocate,
    jvmtiDeallocate,
    jvmtiGetClassSignature,
    jvmtiGetClassStatus,
    jvmtiGetSourceFileName,
    jvmtiGetClassModifiers,
    jvmtiGetClassMethods,
    jvmtiGetClassFields,
    jvmtiGetImplementedInterfaces,
    jvmtiIsInterface,
    jvmtiIsArrayClass,
    jvmtiGetClassLoader,
    jvmtiGetObjectHashCode,
    jvmtiGetObjectMonitorUsage,
    jvmtiGetFieldName,
    jvmtiGetFieldDeclaringClass,
    jvmtiGetFieldModifiers,
    jvmtiIsFieldSynthetic,
    jvmtiGetMethodName,
    jvmtiGetMethodDeclaringClass,
    jvmtiGetMethodModifiers,
    (void *)jvmtiUnimpStub,
    jvmtiGetMaxLocals,
    jvmtiGetArgumentsSize,
    jvmtiGetLineNumberTable,
    jvmtiGetMethodLocation,
    jvmtiGetLocalVariableTable,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    jvmtiGetBytecodes,
    jvmtiIsMethodNative,
    jvmtiIsMethodSynthetic,
    jvmtiGetLoadedClasses,
    jvmtiGetClassLoaderClasses,
    jvmtiPopFrame,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    jvmtiRedefineClasses,
    jvmtiGetVersionNumber,
    jvmtiGetCapabilities,
    jvmtiGetSourceDebugExtension,
    jvmtiIsMethodObsolete,
    jvmtiSuspendThreadList,
    jvmtiResumeThreadList,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    jvmtiGetAllStackTraces,
    jvmtiGetThreadListStackTraces,
    jvmtiGetThreadLocalStorage,
    jvmtiSetThreadLocalStorage,
    jvmtiGetStackTrace,
    (void *)jvmtiUnimpStub,
    jvmtiGetTag,
    jvmtiSetTag,
    jvmtiForceGarbageCollection,
    jvmtiIterateOverObjectsReachableFromObject,
    jvmtiIterateOverReachableObjects,
    jvmtiIterateOverHeap,
    jvmtiIterateOverInstancesOfClass,
    (void *)jvmtiUnimpStub,
    jvmtiGetObjectsWithTags,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    jvmtiSetJNIFunctionTable,
    jvmtiGetJNIFunctionTable,
    jvmtiSetEventCallbacks,
    jvmtiGenerateEvents,
    jvmtiGetExtensionFunctions,
    jvmtiGetExtensionEvents,
    jvmtiSetExtensionEventCallback,
    jvmtiDisposeEnvironment,
    jvmtiGetErrorName,
    jvmtiGetJLocationFormat,
    jvmtiGetSystemProperties,
    jvmtiGetSystemProperty,
    jvmtiSetSystemProperty,
    jvmtiGetPhase,
    jvmtiGetCurrentThreadCpuTimerInfo,
    jvmtiGetCurrentThreadCpuTime,
    jvmtiGetThreadCpuTimerInfo,
    jvmtiGetThreadCpuTime,
    jvmtiGetTimerInfo,
    jvmtiGetTime,
    jvmtiGetPotentialCapabilities,
    (void *)jvmtiUnimpStub,
    jvmtiAddCapabilities,
    jvmtiRelinquishCapabilities,
    jvmtiGetAvailableProcessors,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    jvmtiGetEnvironmentLocalStorage,
    jvmtiSetEnvironmentLocalStorage,
    jvmtiAddToBootstrapClassLoaderSearch,
    jvmtiSetVerboseFlag,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    (void *)jvmtiUnimpStub,
    jvmtiGetObjectSize
};

static void JNICALL jvmtiUnimpStub(JNIEnv* UNREF env)
{
    // If we ever get here, we are in an implemented JVMTI function
    // By looking at the call stack and assembly it should be clear which one
    LDIE(51, "Not implemented");
}

jint JNICALL create_jvmti_environment(JavaVM *vm_ext, void **env, jint version)
{
    JavaVM_Internal *vm = (JavaVM_Internal *)vm_ext;
    // FIXME: there should be a check on whether the thread is attached to VM. How?

    jint vmagic = version & JVMTI_VERSION_MASK_INTERFACE_TYPE;
    jint vmajor = version & JVMTI_VERSION_MASK_MAJOR;
    jint vminor = version & JVMTI_VERSION_MASK_MINOR;
    jint vmicro = version & JVMTI_VERSION_MASK_MICRO;
    if (vmagic != JVMTI_VERSION_INTERFACE_JVMTI ||
        vmajor > (JVMTI_VERSION_MAJOR << JVMTI_VERSION_SHIFT_MAJOR) ||
        vminor > (JVMTI_VERSION_MINOR << JVMTI_VERSION_SHIFT_MINOR) ||
        vmicro > (JVMTI_VERSION_MICRO << JVMTI_VERSION_SHIFT_MICRO))
    {
        *env = NULL;
        return JNI_EVERSION;
    }

    TIEnv *newenv;
    jvmtiError error_code;
    error_code = _allocate(sizeof(TIEnv), (unsigned char**)&newenv);
    if (error_code != JVMTI_ERROR_NONE)
    {
        *env = NULL;
        return error_code;
    }

    memset(newenv, 0, sizeof(TIEnv));

    IDATA error_code1 = port_mutex_create(&newenv->environment_data_lock,
        APR_THREAD_MUTEX_NESTED);
    if (error_code1 != APR_SUCCESS)
    {
        _deallocate((unsigned char *)newenv);
        *env = NULL;
        return JVMTI_ERROR_OUT_OF_MEMORY;
    }

    error_code = newenv->allocate_extension_event_callbacks_table();
    if (error_code != JVMTI_ERROR_NONE)
    {
        port_mutex_destroy(&newenv->environment_data_lock);
        _deallocate((unsigned char *)newenv);
        *env = NULL;
        return error_code;
    }

    newenv->functions = &jvmti_table;
    newenv->vm = vm;
    newenv->user_storage = NULL;
    newenv->agent = current_loading_agent;
    memset(&newenv->event_table, 0, sizeof(jvmtiEventCallbacks));
    memset(&newenv->posessed_capabilities, 0, sizeof(jvmtiCapabilities));
    memset(&newenv->global_events, 0, sizeof(newenv->global_events));
    memset(&newenv->event_threads, 0, sizeof(newenv->event_threads));

    // Acquire interface for breakpoint handling
    newenv->brpt_intf =
        vm->vm_env->TI->vm_brpt->new_intf(newenv,
                                          jvmti_process_breakpoint_event,
                                          PRIORITY_SIMPLE_BREAKPOINT,
                                          interpreter_enabled());

    // NCAI interface support
    newenv->ncai_env = NULL; // GetNCAIEnvironment will allocate ncai_env

    LMAutoUnlock lock(&vm->vm_env->TI->TIenvs_lock);
    vm->vm_env->TI->addEnvironment(newenv);
    *env = newenv;
    TRACE2("jvmti", "New environment added: " << newenv);

    return JNI_OK;
}

void DebugUtilsTI::setExecutionMode(Global_Env *p_env)
{
    for (int i = 0; i < p_env->vm_arguments.nOptions; i++) {
        char *option = p_env->vm_arguments.options[i].optionString;

        if (!strncmp(option, "-agentlib:", 10) ||
            !strncmp(option, "-agentpath:", 11) ||
            !strncmp(option, "-Xrun", 5))
        {
            TRACE2("jvmti", "Enabling EM JVMTI mode");
            vm_properties_set_value("vm.jvmti.enabled", "true", VM_PROPERTIES);
            break;
        }
    }
    if (TRUE == vm_property_get_boolean("vm.jvmti.enabled", FALSE, VM_PROPERTIES)) {
        p_env->TI->setEnabled();
    }
}

DebugUtilsTI::DebugUtilsTI() :
    event_thread(NULL),
    event_cond_initialized(0),
    agent_counter(1),
    access_watch_list(NULL),
    modification_watch_list(NULL),
    status(false),
    need_create_event_thread(false),
    agents(NULL),
    p_TIenvs(NULL),
    MAX_NOTIFY_LIST(1000),
    loadListNumber(0),
    prepareListNumber(0),
    global_capabilities(0),
    single_step_enabled(false),
    cml_report_inlined(false),
    method_entry_enabled_flag(0),
    method_exit_enabled_flag(0)
{
    jvmtiError UNUSED res = _allocate( MAX_NOTIFY_LIST * sizeof(Class**),
        (unsigned char**)&notifyLoadList );
    assert(res == JVMTI_ERROR_NONE);
    res = _allocate( MAX_NOTIFY_LIST * sizeof(Class**),
        (unsigned char**)&notifyPrepareList );
    assert(res == JVMTI_ERROR_NONE);
    vm_brpt = new VMBreakPoints();
    assert(vm_brpt);
    IDATA status = hythread_tls_alloc(&TL_ti_report);
    assert(status == TM_ERROR_NONE);
    memset(event_needed, 0, TOTAL_EVENT_TYPE_NUM*sizeof(unsigned));

    return;
}

DebugUtilsTI::~DebugUtilsTI()
{
    ReleaseNotifyLists();
    hythread_tls_free(TL_ti_report);
    delete vm_brpt;
    jvmti_destroy_event_thread();
    return;
}

void DebugUtilsTI::SetPendingNotifyLoadClass( Class *klass )
{
    assert(loadListNumber < MAX_NOTIFY_LIST);
    notifyLoadList[loadListNumber++] = klass;
}

void DebugUtilsTI::SetPendingNotifyPrepareClass( Class *klass )
{
    assert(prepareListNumber < MAX_NOTIFY_LIST);
    notifyPrepareList[prepareListNumber++] = klass;
}

unsigned DebugUtilsTI::GetNumberPendingNotifyLoadClass()
{
    return loadListNumber;
}

unsigned DebugUtilsTI::GetNumberPendingNotifyPrepareClass()
{
    return prepareListNumber;
}

Class * DebugUtilsTI::GetPendingNotifyLoadClass( unsigned number )
{
    assert(number < loadListNumber);
    return notifyLoadList[number];
}

Class * DebugUtilsTI::GetPendingNotifyPrepareClass( unsigned number )
{
    assert(number < prepareListNumber);
    return notifyPrepareList[number];
}

void DebugUtilsTI::ReleaseNotifyLists()
{
    if( notifyLoadList ) {
        _deallocate( (unsigned char*)notifyLoadList );
        notifyLoadList = NULL;
        loadListNumber = MAX_NOTIFY_LIST;
    }
    if( notifyPrepareList ) {
        _deallocate( (unsigned char*)notifyPrepareList );
        notifyPrepareList = NULL;
        prepareListNumber = MAX_NOTIFY_LIST;
    }
    return;
}

int DebugUtilsTI::getVersion(char* UNREF version)
{
    return 0;
}

// Return lib name and options string if there are any options
static char *parse_agent_option(apr_pool_t* pool, const char *str, const char *option_str,
                         const char option_separator, char **options)
{
    size_t cmd_length = strlen(option_str);
    const char *lib_name = str + cmd_length;
    char *opts_start = (char *)strchr(lib_name, option_separator);
    size_t lib_name_length;

    if (NULL == opts_start)
        lib_name_length = strlen(lib_name);
    else
    {
        lib_name_length = opts_start - lib_name;
        opts_start++;
    }

    char *path = apr_pstrdup(pool, lib_name);
    path[lib_name_length] = '\0';
    *options = opts_start;
    return path;
}

bool open_agent_library(Agent *agent, const char *lib_name, bool print_error)
{
    if (APR_SUCCESS != apr_dso_load(&agent->agentLib, lib_name, agent->pool))
    {
        if (print_error) {
            char buf[256];
            LWARN(32, "Failed to open agent library {0} : {1}" << lib_name
                << apr_dso_error(agent->agentLib, buf, 256));
        }
        return false;
    }
    else
        return true;
}

bool find_agent_onload_function(Agent *agent, const char *function_name1, const char *function_name2)
{
    apr_dso_handle_sym_t handle = 0;
    apr_status_t status = apr_dso_sym(&handle, agent->agentLib, function_name1);
    if (handle == 0 && function_name2 != NULL)
        status = apr_dso_sym(&handle, agent->agentLib, function_name2);
    agent->Agent_OnLoad_func = (f_Agent_OnLoad)handle;
    return status == APR_SUCCESS;
}

bool find_agent_onunload_function(Agent *agent, const char *function_name1, const char *function_name2)
{
    apr_dso_handle_sym_t handle = 0;
    apr_status_t status = apr_dso_sym(&handle, agent->agentLib, function_name1);
    if (handle == 0 && function_name2 != NULL)
        status = apr_dso_sym(&handle, agent->agentLib, function_name2);
    agent->Agent_OnUnLoad_func = (f_Agent_OnUnLoad)handle;
    return status == APR_SUCCESS;
}

jint load_agentpath(Agent *agent, const char *str, JavaVM_Internal *vm)
{
    char *lib_name, *agent_options;
    lib_name = parse_agent_option(agent->pool, str, "-agentpath:", '=', &agent_options);
    if (!open_agent_library(agent, lib_name, true))
        return -1;

    if (!find_agent_onload_function(agent, AGENT_ONLOAD1, AGENT_ONLOAD2))
    {
        char buf[256];
        LWARN(33, "No agent entry function found in library {0} : {1}" << lib_name
            << apr_dso_error(agent->agentLib, buf, 256));
        return -1;
    }
#ifdef _DEBUG
    if (NULL == agent_options)
    {
        TRACE2("jvmti", "Calling onload in lib " << lib_name << " with NULL options");
    }
    else
    {
        TRACE2("jvmti", "Calling onload in lib " << lib_name << " with options " << agent_options);
    }
#endif
    find_agent_onunload_function(agent, AGENT_ONUNLOAD1, AGENT_ONUNLOAD2);
    assert(agent->Agent_OnLoad_func);
    jint result = agent->Agent_OnLoad_func(vm, agent_options, NULL);
    if (0 != result)
        LWARN(34, "Agent library {0} initialization function returned {1}" << lib_name << result);
    return result;
}

static void generate_platform_lib_name(apr_pool_t* pool, JavaVM_Internal *vm, 
                                       const char *lib_name,
                                       char **p_path1, char **p_path2)
{
    char *vm_libs = vm_properties_get_value("vm.boot.library.path", JAVA_PROPERTIES);
    assert(vm_libs);
    char *path1 = apr_pstrdup(pool, vm_libs);
    char *path2 = port_dso_name_decorate(lib_name, pool);
    path1 = port_filepath_merge(path1, path2, pool);
    *p_path1 = path1;
    *p_path2 = path2;
    vm_properties_destroy_value(vm_libs);
}

jint load_agentlib(Agent *agent, const char *str, JavaVM_Internal *vm)
{
    char *lib_name, *agent_options;
    lib_name = parse_agent_option(agent->pool, str, "-agentlib:", '=', &agent_options);
    char *path1, *path2, *path;
    generate_platform_lib_name(agent->pool, vm, lib_name, &path1, &path2);

    bool status = open_agent_library(agent, path1, false);
    if (!status)
    {
        status = open_agent_library(agent, path2, true);
        if (!status)
        {
            LWARN(35, "Failed to open agent library {0}" << path2);
            return -1;
        }
        else
            path = path2;
    }
    else
    {
        path = path1;
    }

    if (!find_agent_onload_function(agent, AGENT_ONLOAD1, AGENT_ONLOAD2))
    {
        char buf[256];
        LWARN(33, "No agent entry function found in library {0} : {1}" << path
            << apr_dso_error(agent->agentLib, buf, 256));
        return -1;
    }
#ifdef _DEBUG
    if (NULL == agent_options)
    {
        TRACE2("jvmti", "Calling onload in lib " << path << " with NULL options");
    }
    else
    {
        TRACE2("jvmti", "Calling onload in lib " << path << " with options " << agent_options);
    }
#endif
    find_agent_onunload_function(agent, AGENT_ONUNLOAD1, AGENT_ONUNLOAD2);
    assert(agent->Agent_OnLoad_func);
    jint result = agent->Agent_OnLoad_func(vm, agent_options, NULL);
    if (0 != result)
        LWARN(34, "Agent library {0} initialization function returned {1}" << path << result);
    return result;
}

jint load_xrun(Agent *agent, const char *str, JavaVM_Internal *vm)
{
    char *lib_name, *agent_options;
    lib_name = parse_agent_option(agent->pool, str, "-xrun", ':', &agent_options);
    char *path1, *path2, *path;
    generate_platform_lib_name(agent->pool, vm, lib_name, &path1, &path2);

    bool status = open_agent_library(agent, path1, false);
    if (!status)
    {
        status = open_agent_library(agent, path2, true);
        if (!status)
        {
            LWARN(35, "Failed to open agent library {0}" << path2);
            return -1;
        }
        else
            path = path2;
    }
    else
    {
        path = path1;
    }

    if (!find_agent_onload_function(agent, AGENT_ONLOAD1, AGENT_ONLOAD2))
    {
        if (!find_agent_onload_function(agent, JVM_ONLOAD1, JVM_ONLOAD2))
        {
            char buf[256];
            LWARN(33, "No agent entry function found in library {0} : {1}" << path 
                << apr_dso_error(agent->agentLib, buf, 256));
            return -1;
        }
        else
            find_agent_onunload_function(agent, JVM_ONUNLOAD1, JVM_ONUNLOAD2);
    }
    else
        find_agent_onunload_function(agent, AGENT_ONUNLOAD1, AGENT_ONUNLOAD2);
#ifdef _DEBUG
    if (NULL == agent_options)
    {
        TRACE2("jvmti", "Calling onload in lib " << path << " with NULL options");
    }
    else
    {
        TRACE2("jvmti", "Calling onload in lib " << path << " with options " << agent_options);
    }
#endif
    assert(agent->Agent_OnLoad_func);
    jint result = agent->Agent_OnLoad_func(vm, agent_options, NULL);
    if (0 != result)
        LWARN(34, "Agent library {0} initialization function returned {1}" << path << result);
    return result;
}

jint DebugUtilsTI::Init(JavaVM *vm)
{
    phase = JVMTI_PHASE_ONLOAD;
    Agent* agent = this->getAgents();

    /*
     * 0. create default jvmtiEnv
     * 1. exclude name of DLL from char*
     * 2. exclude options from char*
     * 3. load DLL and start AgentOnLoad and AgentOnUnload
     * 4. filling of internal structure of TI
     */

    /* ************************************************************** */

    /*
     * If agentsList is NULL -> it means that there were not any agents in
     * command line -> and it means that TI is disabled.
     */
    if (agent==NULL)
        return 0;
    else
    {
        status = true;

        cml_report_inlined = (bool) vm_property_get_boolean(
                "vm.jvmti.compiled_method_load.inlined",
                FALSE, VM_PROPERTIES);

        while (agent)
        {
            int result = 0;
            const char *str = agent->agentName;
            agent->agent_id = agent_counter++;
            agent->dynamic_agent = JNI_FALSE;

            TRACE2("jvmti", "Agent str = " << str);

            current_loading_agent = agent;
            if (strncmp(str, "-agentpath:",  11) == 0)
                result = load_agentpath(agent, str, (JavaVM_Internal*)vm);
            else if (strncmp(str, "-agentlib:", 10) == 0)
                result = load_agentlib(agent, str, (JavaVM_Internal*)vm);
            else if (strncmp(str, "-Xrun:", 5) == 0)
                result = load_xrun(agent, str, (JavaVM_Internal*)vm);
            else
                LDIE(22, "Unknown agent loading option {0}" << str);
            current_loading_agent = NULL;


            if (0 != result)
                return result;
            agent = agent->next;
        }
    }

    nextPhase(JVMTI_PHASE_PRIMORDIAL);

    return 0;
}

/* Calls Agent_OnUnLoad() for agents where it was found, then unloads agents.
*/
void DebugUtilsTI::Shutdown(JavaVM *vm)
{
    Agent* agent = this->getAgents();

    while (agent != NULL)
    {
        if (agent->Agent_OnUnLoad_func != NULL)
        {
            TRACE2("jvmti", "Calling OnUnload in lib " << agent->agentName);
            agent->Agent_OnUnLoad_func(vm);
        }

        if (APR_SUCCESS != apr_dso_unload(agent->agentLib))
        {
            char buf[256];
            LWARN(36, "Failed to unload agent library {0} : {1}" << agent->agentName
                << apr_dso_error(agent->agentLib, buf, 256));
        }

        agent = agent->next;
    }
}

void DebugUtilsTI::addAgent(const char* str) {
    Agent* newagent = new Agent(str);

    newagent->next = getAgents();
    agents = newagent;
}

Agent* DebugUtilsTI::getAgents() {
    return this->agents;
}

void DebugUtilsTI::setAgents(Agent *agents) {
    this->agents = agents;
}

bool DebugUtilsTI::shouldReportLocally() {
    //default value is that ti enabled on thread level
    return hythread_tls_get(hythread_self(), this->TL_ti_report) != NULL;
}

void DebugUtilsTI::doNotReportLocally() {
    //default value is that ti enabled on thread level
    hythread_tls_set(hythread_self(), this->TL_ti_report, NULL);
}

void DebugUtilsTI::reportLocally() {
    hythread_tls_set(hythread_self(), this->TL_ti_report, this);
}

bool DebugUtilsTI::isEnabled() {
    return status;
}

void DebugUtilsTI::addEventSubscriber(jvmtiEvent event_type) {
    apr_atomic_inc32((volatile apr_uint32_t*)&(event_needed[event_type - JVMTI_MIN_EVENT_TYPE_VAL]));
}

void DebugUtilsTI::removeEventSubscriber(jvmtiEvent event_type) {
    apr_atomic_dec32((volatile apr_uint32_t*)&(event_needed[event_type - JVMTI_MIN_EVENT_TYPE_VAL]));
}


bool DebugUtilsTI::hasSubscribersForEvent(jvmtiEvent event_type) {
    return event_needed[event_type - JVMTI_MIN_EVENT_TYPE_VAL] != 0;
}


bool DebugUtilsTI::shouldReportEvent(jvmtiEvent event_type) {
    return isEnabled()
        && hasSubscribersForEvent(event_type)
        && shouldReportLocally();
}


void DebugUtilsTI::setEnabled() {
    this->status = true;
    return;
}

void DebugUtilsTI::setDisabled() {
    this->status = false;
    return;
}

bool DebugUtilsTI::needCreateEventThread() {
    return need_create_event_thread;
}

void DebugUtilsTI::enableEventThreadCreation() {
    need_create_event_thread = true;
    return;
}

void DebugUtilsTI::disableEventThreadCreation() {
    need_create_event_thread = false;
    return;
}

jvmtiError jvmti_translate_jit_error(OpenExeJpdaError error)
{
    switch (error)
    {
    case EXE_ERROR_NONE:
        return JVMTI_ERROR_NONE;
    case EXE_ERROR_INVALID_METHODID:
        return JVMTI_ERROR_INTERNAL;
    case EXE_ERROR_INVALID_LOCATION:
        return JVMTI_ERROR_INTERNAL;
    case EXE_ERROR_TYPE_MISMATCH:
        return JVMTI_ERROR_TYPE_MISMATCH;
    case EXE_ERROR_INVALID_SLOT:
        return JVMTI_ERROR_INVALID_SLOT;
    case EXE_ERROR_UNSUPPORTED:
        return JVMTI_ERROR_INTERNAL;
    default:
        return JVMTI_ERROR_INTERNAL;
    }
}

void jvmti_get_compilation_flags(OpenMethodExecutionParams *flags)
{
    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;

    if (!ti->isEnabled())
        return;

    if (ti->is_cml_report_inlined())
        flags->exe_notify_compiled_method_load = 1;

    flags->exe_do_code_mapping = flags->exe_do_local_var_mapping = 1;

    if (ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_METHOD_ENTRY))
        flags->exe_notify_method_entry = 1;

    if (ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_METHOD_EXIT) ||
        ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_FRAME_POP_NOTIFICATION))
        flags->exe_notify_method_exit = 1;

    if (ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_FIELD_ACCESS_EVENT))
        flags->exe_notify_field_access = true;
    if (ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_FIELD_MODIFICATION_EVENT))
        flags->exe_notify_field_modification = true;

    if (ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_POP_FRAME)) {
        flags->exe_restore_context_after_unwind = true;
        flags->exe_provide_access_to_this = true;
    }
}

