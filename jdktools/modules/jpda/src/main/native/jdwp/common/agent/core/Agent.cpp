/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
#ifndef USING_VMI
#define USING_VMI
#endif

#include "AgentBase.h"

#include "AgentEnv.h"
#include "MemoryManager.h"
#include "AgentException.h"
#include "LogManager.h"
#include "Log.h"
#include "jvmti.h"

#include "jdwpcfg.h"

#include "ClassManager.h"
#include "ObjectManager.h"
#include "OptionParser.h"
#include "ThreadManager.h"
#include "RequestManager.h"
#include "TransportManager.h"
#include "PacketDispatcher.h"
#include "EventDispatcher.h"
#include "AgentManager.h"
#include "ExceptionManager.h"

#include <stdlib.h>
#include <string.h>

using namespace jdwp;

AgentEnv *AgentBase::m_agentEnv = 0;
char* AgentBase::m_defaultStratum = 0;
bool isLoaded = false;
bool disableOnUnload = false;

static const char* const AGENT_OPTIONS_ENVNAME = "JDWP_AGENT_OPTIONS";
static const char* const AGENT_OPTIONS_PROPERTY = "jdwp.agent.options";

/**
 * Name for JVMTI extension IDs to be used for CLASS_UNLOAD support.
 */
static const char* JVMTI_EXTENSION_EVENT_ID_CLASS_UNLOAD 
                        = "com.sun.hotspot.events.ClassUnload";
static const char* JVMTI_EXTENSION_FUNC_ID_IS_CLASS_UNLOAD_ENABLED 
                        = "com.sun.hotspot.functions.IsClassUnloadingEnabled";

//-----------------------------------------------------------------------------
// static internal functions
//-----------------------------------------------------------------------------

static void ShowJDWPVersion() {
    const char *buildLevel = BUILD_LEVEL;
    const char *versionString = "JDWP version:";

    PORT_ACCESS_FROM_JAVAVM(AgentBase::GetJavaVM());
    hytty_printf(privatePortLibrary, "%s %s\n\n", versionString, buildLevel);    
}

static void Usage()
{
  const char* usage = 
        "\nUsage: java -agentlib:agent=[help] | [version] |"
        "\n\t[suspend=y|n][,transport=name][,address=addr]"
        "\n\t[,server=y|n][,timeout=n]"
        "\n\t[,trace=none|all|log_kinds][,src=all|sources][,log=filepath]\n"
        "\nWhere:"
        "\n\thelp\t\tOutput this message"
        "\n\tversion\t\tDisplay the JDWP build version"
        "\n\tsuspend=y|n\tSuspend on start (default: y)"
        "\n\ttransport=name\tName of transport to use for connection"
        "\n\taddress=addr\tTransport address for connection"
        "\n\tserver=y|n\tListen for or attach to debugger (default: n)"
        "\n\ttimeout=n\tTime in ms to wait for connection (0-forever)"
        "\n\ttrace=log_kinds\tApplies filtering to log message kind (default: none)"
        "\n\tsrc=sources\tApplies filtering to __FILE__ (default: all)"
        "\n\tlog=filepath\tRedirect output into filepath\n"
        "\nExample:"
        "\n\tjava -agentlib:agent=transport=dt_socket,"
        "address=localhost:7777,server=y\n"
        "\nExamples of tracing parameters:\n"
        "\ttrace=all,log=jdwp.log\n"
        "\t - traces all kinds of messages\n"
        "\ttrace=MEM+MON,src=VirtualMachine.cpp,log=jdwp.log\n"
        "\t - traces only MEM and MON messages from only one corresponding file\n"
        "\nComplete list of log kinds is following:\n"
        "\tMEM - memory alloc/free calls\n"
        "\tMON - monitor enter/exit methods\n"
        "\tMAP - reference/object id mapping\n"
        "\tCMD - jdwp commands\n"
        "\tPACK - jdwp packets\n"
        "\tDATA - command data tracing\n"
        "\tEVENT - jdwp events\n"
        "\tPROG - program flow\n"
        "\tUTIL - utilities messages\n"
        "\tTHRD - thread\n"
        "\tFUNC - function entry and exit\n"
        "\tJVMTI - jvmti calls\n"
        "\tLOG - debug messages\n"
        "\tINFO - information and warning messages\n"
        "\tERROR - error messages\n\n"
    ;

  PORT_ACCESS_FROM_JAVAVM(AgentBase::GetJavaVM());
  hytty_printf(privatePortLibrary, "%s", usage);
}

//-----------------------------------------------------------------------------
// event callbacks
//-----------------------------------------------------------------------------

static void JNICALL
VMInit(jvmtiEnv *jvmti, JNIEnv *jni, jthread thread)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "VMInit(%p,%p,%p)", jvmti, jni, thread));
    jint ver = jni->GetVersion();
    JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "JNI version: 0x%x", ver));

    // initialize agent
    int ret = AgentBase::GetAgentManager().Init(jvmti, jni);
    if (ret != JDWP_ERROR_NONE) {
        goto handleException;
    }

    // if options onthrow or onuncaught are set, defer starting agent and enable notification of EXCEPTION event
    if (AgentBase::GetOptionParser().GetOnthrow() || AgentBase::GetOptionParser().GetOnuncaught()) {
        ret = AgentBase::GetAgentManager().EnableInitialExceptionCatch(jvmti, jni);
        if (ret != JDWP_ERROR_NONE) {
            goto handleException;
        }
    } else {
        ret = AgentBase::GetAgentManager().Start(jvmti, jni);
        if (ret != JDWP_ERROR_NONE) {
            goto handleException;
        }
        RequestManager::HandleVMInit(jvmti, jni, thread);
    }
    return;

handleException:
    AgentException aex = AgentBase::GetExceptionManager().GetLastException();
    JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "JDWP error in VM_INIT: %s", aex.GetExceptionMessage(jni)));        
    ::exit(1);
}

static void JNICALL
VMDeath(jvmtiEnv *jvmti, JNIEnv *jni)
{
    if (AgentBase::GetAgentManager().IsStarted()) {
        // don't print entry trace message after cleaning agent
        JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "VMDeath(%p, %p)", jvmti, jni));

        RequestManager::HandleVMDeath(jvmti, jni);
        AgentBase::SetIsDead(true);

        AgentBase::GetAgentManager().Stop(jni);
    }
    AgentBase::GetAgentManager().Clean(jni);
}

//-----------------------------------------------------------------------------
// start-up and shutdown entry points
//-----------------------------------------------------------------------------

JNIEXPORT jint JNICALL 
Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{
    if (isLoaded) {
        // We are already loaded - exit with an error message
        PORT_ACCESS_FROM_JAVAVM(vm);
        hyfile_printf(privatePortLibrary, HYPORT_TTY_ERR, "Error: JDWP agent already loaded - please check java command line options\n");
        disableOnUnload = true; // If we're already loaded, dont call unload as it may cause a crash
        return JNI_ERR;
    }
    isLoaded = true;

//    static STDMemoryManager mm;
    static VMMemoryManager mm;
    static STDLogManager slm;
    static AgentEnv env;

    memset(&env, 0, sizeof(env));
    env.memoryManager = &mm;
    env.logManager = &slm;
    env.jvm = vm;
    env.jvmti = 0;
    env.extensionEventClassUnload = 0;
    env.isDead = false;
    AgentBase::SetAgentEnv(&env);
    AgentBase::SetDefaultStratum(NULL);

    jvmtiEnv *jvmti = 0;
    jvmtiError err;

    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Agent_OnLoad(%p,%p,%p)", vm, (void*)options, reserved));

    // get JVMTI environment
    {
        jint ret =
            (vm)->GetEnv(reinterpret_cast<void**>(&jvmti), JVMTI_VERSION_1_0);
        if (ret != JNI_OK || jvmti == 0) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to get JMVTI environment, return code = %d", ret));
            return JNI_ERR;
        }
        env.jvmti = jvmti;
        jint version;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetVersionNumber(&version));
        JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "JVMTI version: 0x%x", version));
    }

    // inital ExceptionManager before first try block
    env.exceptionManager = new ExceptionManager();
    env.exceptionManager->Init(AgentBase::GetJniEnv());

    // parse agent options
    env.optionParser = new OptionParser();

    // add options from environment variable and/or system property
    {
        char* envOptions = getenv(AGENT_OPTIONS_ENVNAME);
        char* propOptions = 0;
        jvmtiError err;

        JVMTI_TRACE(LOG_DEBUG, err,
            jvmti->GetSystemProperty(AGENT_OPTIONS_PROPERTY, &propOptions));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "No system property: %s, err=%d", AGENT_OPTIONS_PROPERTY, err));
            propOptions = 0;
        }
        JvmtiAutoFree af(propOptions);

        if (envOptions != 0 || propOptions != 0) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Add options from: \n\tcommand line: %s\n\tenvironment %s: %s\n\tproperty %s: %s", 
                      JDWP_CHECK_NULL(options), AGENT_OPTIONS_ENVNAME, JDWP_CHECK_NULL(envOptions), AGENT_OPTIONS_PROPERTY, JDWP_CHECK_NULL(propOptions)));

            size_t fullLength = ((options == 0) ? 0 : strlen(options) + 1)
                + ((propOptions == 0) ? 0 : strlen(propOptions) + 1)
                + ((envOptions == 0) ? 0 : strlen(envOptions) + 1);
            char* fullOptions = static_cast<char*>
                (AgentBase::GetMemoryManager().Allocate(fullLength JDWP_FILE_LINE));
            fullOptions[0] = '\0';
            if (options != 0) {
                strcat(fullOptions, options);
            }
            if (envOptions != 0) {
                if (fullOptions[0] != '\0') {
                    strcat(fullOptions, ",");
                }
                strcat(fullOptions, envOptions);
            }
            if (propOptions != 0) {
                if (fullOptions[0] != '\0') {
                    strcat(fullOptions, ",");
                }
                strcat(fullOptions, propOptions);
            }
            options = fullOptions;
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Full options: %s", JDWP_CHECK_NULL(options)));
        }

        if (AgentBase::GetOptionParser().Parse(options) != JDWP_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error: Bad agent options: %s", options));
            delete env.optionParser;
            return JNI_ERR;
        }
    }

    // initialize LogManager module first
    AgentBase::GetLogManager().Init(
        AgentBase::GetOptionParser().GetLog(),
        AgentBase::GetOptionParser().GetTraceKindFilter(),
        AgentBase::GetOptionParser().GetTraceSrcFilter()
    );

    #ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_LOG)) {
          int optCount = AgentBase::GetOptionParser().GetOptionCount();
          JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "parsed %d options:", optCount));
          for (int k = 0; k < optCount; k++) {
              const char *name, *value;
              AgentBase::GetOptionParser().GetOptionByIndex(k, name, value);
              JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "[%d]: %s=%s", k, JDWP_CHECK_NULL(name), JDWP_CHECK_NULL(value)));
          }
    }
    #endif // NDEBUG

    // exit if help option specified
    if (AgentBase::GetOptionParser().GetHelp()) {
        Usage();
        JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "exit"));
        delete env.optionParser;
        exit(0);
    }

    // exit if version option specified
    if (AgentBase::GetOptionParser().GetVersion()) {
        ShowJDWPVersion();
        JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "exit"));
        delete env.optionParser;
        exit(0);
    }

    // check for required options
    if (AgentBase::GetOptionParser().GetTransport() == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error: No agent option specified: transport"));
        return JNI_ERR;
    }
    if (!AgentBase::GetOptionParser().GetServer() 
            && AgentBase::GetOptionParser().GetAddress() == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error: No agent option specified: address"));
        return JNI_ERR;
    }

    // create all other modules
    env.classManager = new ClassManager();
    env.objectManager = new ObjectManager();
    env.threadManager = new ThreadManager();
    env.requestManager = new RequestManager();
    env.transportManager = new TransportManager();
    env.packetDispatcher = new PacketDispatcher();
    env.eventDispatcher = new EventDispatcher();
    env.agentManager = new AgentManager();

#ifndef NDEBUG
    // display system properties
    if (JDWP_TRACE_ENABLED(LOG_KIND_LOG)) {
        jint pCount;
        char **properties = 0;

        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetSystemProperties(&pCount, &properties));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to get system properties: %d", err));
        }
        JvmtiAutoFree afp(properties);

        JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "System properties:"));
        for (jint j = 0; j < pCount; j++) {
            char *value = 0;
            JvmtiAutoFree afj(properties[j]);
            JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetSystemProperty(properties[j], &value));
            if (err != JVMTI_ERROR_NONE) {
                JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to get system property: %s", JDWP_CHECK_NULL(properties[j])));
            }
            JvmtiAutoFree afv(value);
            JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "  %d: %s=%s", j, JDWP_CHECK_NULL(properties[j]), JDWP_CHECK_NULL(value)));
        }
    }
#endif // NDEBUG

    // manage JVMTI and JDWP capabilities
    {
        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));

        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetPotentialCapabilities(&caps));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to get potential capabilities: %d", err));
            return JNI_ERR;
        }

        // Map JVMTI capabilities onto JDWP capabilities
        env.caps.canWatchFieldModification = caps.can_generate_field_modification_events;
        env.caps.canWatchFieldAccess = caps.can_generate_field_access_events;
        env.caps.canGetBytecodes = caps.can_get_bytecodes;
        env.caps.canGetSyntheticAttribute = caps.can_get_synthetic_attribute;
        env.caps.canGetOwnedMonitorInfo = caps.can_get_owned_monitor_info;
        env.caps.canGetCurrentContendedMonitor = caps.can_get_current_contended_monitor;
        env.caps.canGetMonitorInfo = caps.can_get_monitor_info;
        env.caps.canRedefineClasses = caps.can_redefine_classes;
        env.caps.canAddMethod = 0;
        env.caps.canUnrestrictedlyRedefineClasses = 0;
        env.caps.canPopFrames = caps.can_pop_frame;
        env.caps.canUseInstanceFilters = 1;
        env.caps.canGetSourceDebugExtension = caps.can_get_source_debug_extension;
        env.caps.canRequestVMDeathEvent = 1;
        env.caps.canSetDefaultStratum = 1;
        // New JDWP capabilities for Java 6
        env.caps.canGetInstanceInfo = 1;
        env.caps.canRequestMonitorEvents = 1;
        env.caps.canGetMonitorFrameInfo = 1;
        env.caps.canUseSourceNameFilters = 0;
        env.caps.canGetConstantPool = caps.can_get_constant_pool;
        env.caps.canForceEarlyReturn = caps.can_force_early_return;

        // Request JVMTI capabilities are made available
        caps.can_tag_objects = 1;
        caps.can_generate_monitor_events = 1;

        // these caps should be added for full agent functionality
        // caps.can_suspend = 1;
        // caps.can_signal_thread = 1;
        // caps.can_get_source_file_name = 1;
        // caps.can_get_line_numbers = 1;
        // caps.can_access_local_variables = 1;
        caps.can_generate_single_step_events = 1;
        // caps.can_generate_exception_events = 1;
        // caps.can_generate_frame_pop_events = 1;
        // caps.can_generate_breakpoint_events = 1;
        // caps.can_generate_method_entry_events = 1;
        // caps.can_generate_method_exit_events = 1;
        // caps.can_redefine_any_class = 1;

        // these caps look unnecessary for JDWP agent
        caps.can_maintain_original_method_order = 0;
        caps.can_redefine_any_class = 0;
        caps.can_get_current_thread_cpu_time = 0;
        caps.can_get_thread_cpu_time = 0;
        caps.can_generate_all_class_hook_events = 0;
        caps.can_generate_compiled_method_load_events = 0;
        caps.can_generate_vm_object_alloc_events = 0;
        caps.can_generate_native_method_bind_events = 0;
        caps.can_generate_garbage_collection_events = 0;
        caps.can_generate_object_free_events = 0;

        JVMTI_TRACE(LOG_DEBUG, err, jvmti->AddCapabilities(&caps));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to add capabilities: %d", err));
            return JNI_ERR;
        }
    }

    // set up initial event callbacks
    {
        jvmtiEventCallbacks ecbs;
        memset(&ecbs, 0, sizeof(ecbs));

        ecbs.VMInit = &VMInit;
        ecbs.VMDeath = &VMDeath;

        ecbs.Breakpoint = &RequestManager::HandleBreakpoint;
        //ecbs.ClassLoad = &ClassLoad;
        ecbs.ClassPrepare = &RequestManager::HandleClassPrepare;
        ecbs.Exception = &RequestManager::HandleException;
        //ecbs.ExceptionCatch = &ExceptionCatch;
        ecbs.FieldAccess = &RequestManager::HandleFieldAccess;
        ecbs.FieldModification = &RequestManager::HandleFieldModification;
        ecbs.FramePop = &RequestManager::HandleFramePop;
        ecbs.MethodEntry = &RequestManager::HandleMethodEntry;
        ecbs.MethodExit = &RequestManager::HandleMethodExit;
        ecbs.SingleStep = &RequestManager::HandleSingleStep;
        ecbs.ThreadEnd = &RequestManager::HandleThreadEnd;
        ecbs.ThreadStart = &RequestManager::HandleThreadStart;
        // New event callbacks for Java 6
        ecbs.MonitorContendedEnter = &RequestManager::HandleMonitorContendedEnter;
        ecbs.MonitorContendedEntered = &RequestManager::HandleMonitorContendedEntered;
        ecbs.MonitorWait = &RequestManager::HandleMonitorWait;
        ecbs.MonitorWaited = &RequestManager::HandleMonitorWaited;

        JVMTI_TRACE(LOG_DEBUG, err,
            jvmti->SetEventCallbacks(&ecbs, static_cast<jint>(sizeof(ecbs))));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to set event callbacks: %d", err));
            return JNI_ERR;
        }

        JVMTI_TRACE(LOG_DEBUG, err, jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_VM_INIT, 0));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to enable VM_INIT event: %d", err));
            return JNI_ERR;
        }
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_VM_DEATH, 0));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to enable VM_DEATH event: %d", err));
            return JNI_ERR;
        }
    }

    // find JVMTI extension event for CLASS_UNLOAD
    {
        jint extensionEventsCount = 0;
        jvmtiExtensionEventInfo* extensionEvents = 0;

        jvmtiError err;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetExtensionEvents(&extensionEventsCount, &extensionEvents));
        JvmtiAutoFree afv(extensionEvents);
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to get JVMTI extension events: %d", err));
            return JNI_ERR;
        }

        if (extensionEvents != 0 && extensionEventsCount > 0) {
            for (int i = 0; i < extensionEventsCount; i++) {
                if (strcmp(extensionEvents[i].id, JVMTI_EXTENSION_EVENT_ID_CLASS_UNLOAD) == 0) {
                    JDWP_TRACE(LOG_RELEASE, (LOG_LOG_FL, "CLASS_UNLOAD extension event: index=%d id=%d param_count=%d descr=%s", 
                             extensionEvents[i].extension_event_index, extensionEvents[i].id, extensionEvents[i].param_count, extensionEvents[i].short_description));
                    // store info about found extension event 
                    env.extensionEventClassUnload = static_cast<jvmtiExtensionEventInfo*>
                        (AgentBase::GetMemoryManager().Allocate(sizeof(jvmtiExtensionEventInfo) JDWP_FILE_LINE));
                    *(env.extensionEventClassUnload) = extensionEvents[i];
                } else {
                    // free allocated memory for not used extension events
                    JVMTI_TRACE(LOG_DEBUG, err, jvmti->Deallocate(
                        reinterpret_cast<unsigned char*>(extensionEvents[i].id)));
                    JVMTI_TRACE(LOG_DEBUG, err, jvmti->Deallocate(
                        reinterpret_cast<unsigned char*>(extensionEvents[i].short_description)));
                    if (extensionEvents[i].params != 0) {
                        for (int j = 0; j < extensionEvents[i].param_count; j++) {
                            JVMTI_TRACE(LOG_DEBUG, err, jvmti->Deallocate(
                                reinterpret_cast<unsigned char*>(extensionEvents[i].params[j].name)));
                        }
                        JVMTI_TRACE(LOG_DEBUG, err, jvmti->Deallocate(
                            reinterpret_cast<unsigned char*>(extensionEvents[i].params)));
                    }
                }
            }
        }
    }
    return JNI_OK;
}

JNIEXPORT void JNICALL
Agent_OnUnload(JavaVM *vm)
{
    if (disableOnUnload) {
        return;
    }

    if (AgentBase::GetAgentEnv() != 0) {
        delete &AgentBase::GetEventDispatcher();
        delete &AgentBase::GetPacketDispatcher();
        delete &AgentBase::GetTransportManager();
        delete &AgentBase::GetRequestManager();
        delete &AgentBase::GetThreadManager();
        delete &AgentBase::GetObjectManager();
        delete &AgentBase::GetClassManager();
        delete &AgentBase::GetOptionParser();
        delete &AgentBase::GetAgentManager();
    }
}
