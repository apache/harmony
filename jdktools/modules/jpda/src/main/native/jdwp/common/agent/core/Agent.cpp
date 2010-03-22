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

/**
 * @author Pavel N. Vyssotski
 */
// Agent.cpp

#include <string.h>
#include <cstdlib>
#include <cstdio>

#include "jvmti.h"

#include "AgentEnv.h"
#include "AgentBase.h"
#include "MemoryManager.h"
#include "AgentException.h"
#include "LogManager.h"
#include "Log.h"

#include "ClassManager.h"
#include "ObjectManager.h"
#include "OptionParser.h"
#include "ThreadManager.h"
#include "RequestManager.h"
#include "TransportManager.h"
#include "PacketDispatcher.h"
#include "EventDispatcher.h"
#include "AgentManager.h"

using namespace jdwp;

AgentEnv *AgentBase::m_agentEnv = 0;

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

static void Usage()
{
    std::fprintf(stdout,
        "\nUsage: java -agentlib:agent=[help] |"
        "\n\t[suspend=y|n][,transport=name][,address=addr]"
        "\n\t[,server=y|n][,timeout=n]"
#ifndef NDEBUG
        "\n\t[,trace=none|all|log_kinds][,src=all|sources][,log=filepath]\n"
#endif//NDEBUG
        "\nWhere:"
        "\n\thelp\t\tOutput this message"
        "\n\tsuspend=y|n\tSuspend on start (default: y)"
        "\n\ttransport=name\tName of transport to use for connection"
        "\n\taddress=addr\tTransport address for connection"
        "\n\tserver=y|n\tListen for or attach to debugger (default: n)"
        "\n\ttimeout=n\tTime in ms to wait for connection (0-forever)"
#ifndef NDEBUG
        "\n\ttrace=log_kinds\tApplies filtering to log message kind (default: none)"
        "\n\tsrc=sources\tApplies filtering to __FILE__ (default: all)"
        "\n\tlog=filepath\tRedirect output into filepath\n"
#endif//NDEBUG
        "\nExample:"
        "\n\tjava -agentlib:agent=transport=dt_socket,"
        "address=localhost:7777,server=y\n"
#ifndef NDEBUG
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
#endif//NDEBUG
    );
}

//-----------------------------------------------------------------------------
// event callbacks
//-----------------------------------------------------------------------------

static void JNICALL
VMInit(jvmtiEnv *jvmti, JNIEnv *jni, jthread thread)
{
    try {
        JDWP_TRACE_ENTRY("VMInit(" << jvmti << ',' << jni << ',' << thread << ')');
        jint ver = jni->GetVersion();
        JDWP_LOG("JNI version: 0x" << hex << ver);

        // initialize agent
        AgentBase::GetAgentManager().Init(jvmti, jni);
 
        // if options onthrow or onuncaught are set, defer starting agent and enable notification of EXCEPTION event
        if (AgentBase::GetOptionParser().GetOnthrow() || AgentBase::GetOptionParser().GetOnuncaught()) {
            AgentBase::GetAgentManager().EnableInitialExceptionCatch(jvmti, jni);
        } else {
            AgentBase::GetAgentManager().Start(jvmti, jni);
            RequestManager::HandleVMInit(jvmti, jni, thread);
        }
    } catch (TransportException& e) {
        JDWP_DIE("JDWP transport error in VM_INIT: " << e.TransportErrorMessage() << " [" << e.ErrCode() << "]");
    } catch (AgentException& e) {
        JDWP_DIE("JDWP error in VM_INIT: " << e.what() << " [" << e.ErrCode() << "]");
    }
}

static void JNICALL
VMDeath(jvmtiEnv *jvmti, JNIEnv *jni)
{
    try {
        if (AgentBase::GetAgentManager().IsStarted()) {
            // don't print entry trace message after cleaning agent
            JDWP_TRACE_ENTRY("VMDeath(" << jvmti << ',' << jni << ')');

            RequestManager::HandleVMDeath(jvmti, jni);
            AgentBase::SetIsDead(true);

            AgentBase::GetAgentManager().Stop(jni);
        }
        AgentBase::GetAgentManager().Clean(jni);
    } catch (AgentException& e) {
        JDWP_INFO("JDWP error in VM_DEATH: " << e.what() << " [" << e.ErrCode() << "]");
    }
}

//-----------------------------------------------------------------------------
// start-up and shutdown entry points
//-----------------------------------------------------------------------------

JNIEXPORT jint JNICALL 
Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{

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

    jvmtiEnv *jvmti = 0;
    jvmtiError err;

    JDWP_TRACE_ENTRY("Agent_OnLoad(" << vm << "," << (void*)options << "," << reserved << ")");

    // get JVMTI environment
    {
        jint ret =
            (vm)->GetEnv(reinterpret_cast<void**>(&jvmti), JVMTI_VERSION_1_0);
        if (ret != JNI_OK || jvmti == 0) {
            JDWP_INFO("Unable to get JMVTI environment, return code = " << ret);
            return JNI_ERR;
        }
        env.jvmti = jvmti;
        jint version;
        JVMTI_TRACE(err, jvmti->GetVersionNumber(&version));
        JDWP_LOG("JVMTI version: 0x" << hex << version);
    }

    // parse agent options
    try {
        env.optionParser = new OptionParser();

        // add options from environment variable and/or system property
        {
            char* envOptions = getenv(AGENT_OPTIONS_ENVNAME);
            char* propOptions = 0;
            jvmtiError err;

            JVMTI_TRACE(err,
                jvmti->GetSystemProperty(AGENT_OPTIONS_PROPERTY, &propOptions));
            if (err != JVMTI_ERROR_NONE) {
                JDWP_LOG("No system property: "
                    << AGENT_OPTIONS_PROPERTY << ", err=" << err);
                propOptions = 0;
            }
            JvmtiAutoFree af(propOptions);

            if (envOptions != 0 || propOptions != 0) {
                JDWP_INFO("Add options from: "
                    << endl << "\tcommand line: " << JDWP_CHECK_NULL(options)
                    << endl << "\tenvironment " << AGENT_OPTIONS_ENVNAME
                    << ": " << JDWP_CHECK_NULL(envOptions)
                    << endl << "\tproperty " << AGENT_OPTIONS_PROPERTY
                    << ": " << JDWP_CHECK_NULL(propOptions)
                );

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
                JDWP_INFO("Full options: " << JDWP_CHECK_NULL(options));
            }

            AgentBase::GetOptionParser().Parse(options);
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
              JDWP_LOG("parsed " << optCount << " options:");
              for (int k = 0; k < optCount; k++) {
                  const char *name, *value;
                  AgentBase::GetOptionParser().GetOptionByIndex(k, name, value);
                  JDWP_LOG("[" << k << "]: " << JDWP_CHECK_NULL(name) << " = "
                      << JDWP_CHECK_NULL(value));
              }
        }
        #endif // NDEBUG

        // exit if help option specified
        if (AgentBase::GetOptionParser().GetHelp()) {
            Usage();
            JDWP_LOG("exit" << endl);
            delete env.optionParser;
            std::exit(0);
        }

        // check for required options
        if (AgentBase::GetOptionParser().GetTransport() == 0) {
            JDWP_INFO("JDWP error: No agent option specified: " << "transport");
            return JNI_ERR;
        }
        if (!AgentBase::GetOptionParser().GetServer() 
                && AgentBase::GetOptionParser().GetAddress() == 0) {
            JDWP_INFO("JDWP error: No agent option specified: " << "address");
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
    } catch (IllegalArgumentException&) {
        JDWP_INFO("JDWP error: Bad agent options: " << options);
        delete env.optionParser;
        return JNI_ERR;
    } catch (AgentException& e) {
        JDWP_INFO("JDWP error: " << e.what() << " [" << e.ErrCode() << "]");
        return JNI_ERR;
    }

#ifndef NDEBUG
    // display system properties
    if (JDWP_TRACE_ENABLED(LOG_KIND_LOG)) {
        jint pCount;
        char **properties = 0;

        JVMTI_TRACE(err, jvmti->GetSystemProperties(&pCount, &properties));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_INFO("Unable to get system properties: " << err);
        }
        JvmtiAutoFree afp(properties);

        JDWP_LOG("System properties:");
        for (jint j = 0; j < pCount; j++) {
            char *value = 0;
            JvmtiAutoFree afj(properties[j]);
            JVMTI_TRACE(err, jvmti->GetSystemProperty(properties[j], &value));
            if (err != JVMTI_ERROR_NONE) {
                JDWP_INFO("Unable to get system property: "
                    << JDWP_CHECK_NULL(properties[j]));
            }
            JvmtiAutoFree afv(value);
            JDWP_LOG("  " << j << ": " << JDWP_CHECK_NULL(properties[j])
                << " = " << JDWP_CHECK_NULL(value));
        }
    }
#endif // NDEBUG

    // manage JVMTI and JDWP capabilities
    {
        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));

        JVMTI_TRACE(err, jvmti->GetPotentialCapabilities(&caps));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_INFO("Unable to get potential capabilities: " << err);
            return JNI_ERR;
        }

        // map directly into JDWP caps
        env.caps.canWatchFieldModification =
            caps.can_generate_field_modification_events;
        env.caps.canWatchFieldAccess = caps.can_generate_field_access_events;
        env.caps.canGetBytecodes = caps.can_get_bytecodes;
        env.caps.canGetSyntheticAttribute = caps.can_get_synthetic_attribute;
        env.caps.canGetOwnedMonitorInfo = caps.can_get_owned_monitor_info;
        env.caps.canGetCurrentContendedMonitor =
            caps.can_get_current_contended_monitor;
        env.caps.canGetMonitorInfo = caps.can_get_monitor_info;
        env.caps.canPopFrames = caps.can_pop_frame;
        env.caps.canRedefineClasses = caps.can_redefine_classes;
        env.caps.canGetSourceDebugExtension =
            caps.can_get_source_debug_extension;

        env.caps.canAddMethod = 0;
        env.caps.canUnrestrictedlyRedefineClasses = 0;
        env.caps.canUseInstanceFilters = 1;
        env.caps.canRequestVMDeathEvent = 1;
        env.caps.canSetDefaultStratum = 0;

        // these caps should be added for full agent functionality
        // caps.can_suspend = 1;
        // caps.can_signal_thread = 1;
        // caps.can_get_source_file_name = 1;
        // caps.can_get_line_numbers = 1;
        // caps.can_access_local_variables = 1;
        // caps.can_generate_single_step_events = 1;
        // caps.can_generate_exception_events = 1;
        // caps.can_generate_frame_pop_events = 1;
        // caps.can_generate_breakpoint_events = 1;
        // caps.can_generate_method_entry_events = 1;
        // caps.can_generate_method_exit_events = 1;
        // caps.can_redefine_any_class = 1;

        // these caps look unnecessary for JDWP agent
        caps.can_tag_objects = 0;
        caps.can_maintain_original_method_order = 0;
        caps.can_redefine_any_class = 0;
        caps.can_get_current_thread_cpu_time = 0;
        caps.can_get_thread_cpu_time = 0;
        caps.can_generate_all_class_hook_events = 0;
        caps.can_generate_compiled_method_load_events = 0;
        caps.can_generate_monitor_events = 0;
        caps.can_generate_vm_object_alloc_events = 0;
        caps.can_generate_native_method_bind_events = 0;
        caps.can_generate_garbage_collection_events = 0;
        caps.can_generate_object_free_events = 0;

        JVMTI_TRACE(err, jvmti->AddCapabilities(&caps));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_INFO("Unable to add capabilities: " << err);
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

        JVMTI_TRACE(err,
            jvmti->SetEventCallbacks(&ecbs, static_cast<jint>(sizeof(ecbs))));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_INFO("Unable to set event callbacks: " << err);
            return JNI_ERR;
        }

        JVMTI_TRACE(err, jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_VM_INIT, 0));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_INFO("Unable to enable VM_INIT event: " << err);
            return JNI_ERR;
        }
        JVMTI_TRACE(err, jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_VM_DEATH, 0));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_INFO("Unable to enable VM_DEATH event: " << err);
            return JNI_ERR;
        }
    }

    // find JVMTI extension event for CLASS_UNLOAD
    {
        jint extensionEventsCount = 0;
        jvmtiExtensionEventInfo* extensionEvents = 0;

        jvmtiError err;
        JVMTI_TRACE(err, jvmti->GetExtensionEvents(&extensionEventsCount, &extensionEvents));
        JvmtiAutoFree afv(extensionEvents);
        if (err != JVMTI_ERROR_NONE) {
            JDWP_INFO("Unable to get JVMTI extension events: " << err);
            return JNI_ERR;
        }

        if (extensionEvents != 0 && extensionEventsCount > 0) {
            for (int i = 0; i < extensionEventsCount; i++) {
                if (strcmp(extensionEvents[i].id, JVMTI_EXTENSION_EVENT_ID_CLASS_UNLOAD) == 0) {
                    JDWP_LOG("CLASS_UNLOAD extension event: " 
                            << " index=" << extensionEvents[i].extension_event_index
                            << " id=" << extensionEvents[i].id
                            << " param_count=" << extensionEvents[i].param_count
                            << " descr=" << extensionEvents[i].short_description);
                    // store info about found extension event 
                    env.extensionEventClassUnload = static_cast<jvmtiExtensionEventInfo*>
                        (AgentBase::GetMemoryManager().Allocate(sizeof(jvmtiExtensionEventInfo) JDWP_FILE_LINE));
                    *(env.extensionEventClassUnload) = extensionEvents[i];
                } else {
                    // free allocated memory for not used extension events
                    JVMTI_TRACE(err, jvmti->Deallocate(
                        reinterpret_cast<unsigned char*>(extensionEvents[i].id)));
                    JVMTI_TRACE(err, jvmti->Deallocate(
                        reinterpret_cast<unsigned char*>(extensionEvents[i].short_description)));
                    if (extensionEvents[i].params != 0) {
                        for (int j = 0; j < extensionEvents[i].param_count; j++) {
                            JVMTI_TRACE(err, jvmti->Deallocate(
                                reinterpret_cast<unsigned char*>(extensionEvents[i].params[j].name)));
                        }
                        JVMTI_TRACE(err, jvmti->Deallocate(
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
//    JDWP_TRACE_ENTRY("Agent_OnUnload(" << vm << ")");
    
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
