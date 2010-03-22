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

/**
 * @file
 * AgentBase.h
 *
 */

#ifndef _AGENT_BASE_H_
#define _AGENT_BASE_H_

#include <cstdio>
#include <cstdlib>
#include <sstream>

#include "AgentEnv.h"
#include "MemoryManager.h"
#include "Log.h"

#ifdef NDEBUG
#define CHECK_ENV(env)
#else
#define CHECK_ENV(env) { \
    if (env == 0) { \
        std::fprintf(stderr, \
            "Bad environment: "# env"=%p\n", env); \
        std::exit(1); \
    } \
}
#endif // NDEBUG

#ifdef NDEBUG
#define CHECK_ENV_PTR(env, ptr)
#else
#define CHECK_ENV_PTR(env, ptr) { \
    if (env == 0 || env->ptr == 0) { \
        std::fprintf(stderr, \
            "Bad environment: "# env"=%p, "# ptr"=%p\n", env, env->ptr); \
        std::exit(1); \
    } \
}
#endif // NDEBUG

namespace jdwp {

    /**
     * The base agent class redefining new operators and deleting ones inside 
     * the JDWP namespace.
     *
     * The class uses the <code>MemoryManager</code> object for allocation 
     * and freeing all dynamic memory required by the agent.
     */
    class Allocatable {

    public:

        /**
         * Redefined a new operator in the JDWP namespace.
         */
        void* operator new(size_t size);

        /**
         * Redefined a delete operator in the JDWP namespace.
         */
        void operator delete(void* ptr);

        /**
         * Redefined a new[] operator in the JDWP namespace.
         */
        void* operator new[](size_t size);

        /**
         * Redefined a delete[] operator in the JDWP namespace.
         */
        void operator delete[](void* ptr);
    };

    /**
     * The base class providing a uniform access for key-agent objects.
     * The class gives access to the following units:
     * <ul>
     *   <li>AgentEnv</li>
     *   <li>MemoryManager</li>
     *   <li>LogManager</li>
     *   <li>OptionParser</li>
     *   <li>ThreadManager</li>
     *   <li>TransportManager</li>
     *   <li>ObjectManager</li>
     *   <li>ClassManager</li>
     *   <li>PacketDispatcher</li>
     *   <li>EventDispatcher</li>
     *   <li>RequestManager</li>
     *   <li>jvmtiEnv</li>
     *   <li>JavaVM</li>
     *   <li>JNIEnv</li>
     *   <li>jdwpCapabilities</li>
     * </ul>
     * 
     */
    class AgentBase : public Allocatable {

    public:

        /**
         * A constructor.
         */
        AgentBase() throw() {}

        /**
         * Stores agent environment in the agent base.
         *
         * @param env - the pointer to the agent environment
         */
        static void SetAgentEnv(AgentEnv *env) throw() {
            m_agentEnv = env;
        }

        /**
         * Gets agent environment from the agent base.
         */
        static AgentEnv* GetAgentEnv() throw() {
            return m_agentEnv;
        }

        /**
         * Gets the memory manager reference from the agent base.
         */
        static MemoryManager& GetMemoryManager() throw() {
            CHECK_ENV_PTR(m_agentEnv, memoryManager);
            return *m_agentEnv->memoryManager;
        }

        /**
         * Gets a log manager reference from the agent base.
         */
        static LogManager& GetLogManager() throw() {
            CHECK_ENV_PTR(m_agentEnv, logManager);
            return *m_agentEnv->logManager;
        }

        /**
         * Gets the option-parser reference from the agent base.
         */
        static OptionParser& GetOptionParser() throw() {
            CHECK_ENV_PTR(m_agentEnv, optionParser);
            return *m_agentEnv->optionParser;
        }

        /**
         * Gets the thread-manager reference from the agent base.
         */
        static ThreadManager& GetThreadManager() throw() {
            CHECK_ENV_PTR(m_agentEnv, threadManager);
            return *m_agentEnv->threadManager;
        }

        /**
         * Gets the transport-manager reference from the agent base.
         */
        static TransportManager& GetTransportManager() throw() {
            CHECK_ENV_PTR(m_agentEnv, transportManager);
            return *m_agentEnv->transportManager;
        }

        /**
         * Gets the object-manager reference from the agent base.
         */
        static ObjectManager& GetObjectManager() throw() {
            CHECK_ENV_PTR(m_agentEnv, objectManager);
            return *m_agentEnv->objectManager;
        }

        /**
         * Gets the class-manager reference from the agent base.
         */
        static ClassManager& GetClassManager() throw() {
            CHECK_ENV_PTR(m_agentEnv, classManager);
            return *m_agentEnv->classManager;
        }

        /**
         * Gets the packet-dispatcher reference from the agent base.
         */
        static PacketDispatcher& GetPacketDispatcher() throw() {
            CHECK_ENV_PTR(m_agentEnv, packetDispatcher);
            return *m_agentEnv->packetDispatcher;
        }

        /**
         * Gets the event-dispatcher reference from the agent base.
         */
        static EventDispatcher& GetEventDispatcher() throw() {
            CHECK_ENV_PTR(m_agentEnv, eventDispatcher);
            return *m_agentEnv->eventDispatcher;
        }

        /**
         * Gets the request-manager reference from the agent base.
         */
        static RequestManager& GetRequestManager() throw() {
            CHECK_ENV_PTR(m_agentEnv, requestManager);
            return *m_agentEnv->requestManager;
        }

        /**
         * Gets the agent-manager reference from the agent base.
         */
        static AgentManager& GetAgentManager() throw() {
            CHECK_ENV_PTR(m_agentEnv, agentManager);
            return *m_agentEnv->agentManager;
        }

        /**
         * Gets the pointer to JVMTI environment from the agent base.
         */
        static jvmtiEnv* GetJvmtiEnv() throw() {
            CHECK_ENV_PTR(m_agentEnv, jvmti);
            return m_agentEnv->jvmti;
        }

        /**
         * Gets the pointer to the target VM from the agent base.
         */
        static JavaVM* GetJavaVM() throw() {
            CHECK_ENV_PTR(m_agentEnv, jvm);
            return m_agentEnv->jvm;
        }

        /**
         * Gets the pointer to JNI environment from the agent base.
         */
        static JNIEnv* GetJniEnv() throw() {
            JNIEnv *jniEnv = 0;
            CHECK_ENV_PTR(m_agentEnv, jvm);
            (m_agentEnv->jvm)->GetEnv((void **)&jniEnv, JNI_VERSION_1_4);
            return jniEnv;
        }

        /**
         * Gets JDWP capabilities from the agent base.
         */
        static jdwpCapabilities& GetCapabilities() throw() {
            CHECK_ENV(m_agentEnv);
            return m_agentEnv->caps;
        }

        /**
         * Gets a dead status of the agent.
         */
        static bool IsDead() throw() {
            CHECK_ENV(m_agentEnv);
            return m_agentEnv->isDead;
        }

        /**
         * Sets a dead status to the agent.
         */
        static void SetIsDead(bool isDead) throw() {
            CHECK_ENV(m_agentEnv);
            m_agentEnv->isDead = isDead;
        }
        
    private:

        AgentBase(const AgentBase& r) { }

        const AgentBase& operator=(const AgentBase& r) {
            return *this;
        }

        static AgentEnv *m_agentEnv;
    };

    inline void* Allocatable::operator new(size_t size) {
        return AgentBase::GetMemoryManager().Allocate(size JDWP_FILE_LINE);
    }

    inline void Allocatable::operator delete(void* ptr) {
        AgentBase::GetMemoryManager().Free(ptr JDWP_FILE_LINE);
    }

    inline void* Allocatable::operator new[](size_t size) {
        return AgentBase::GetMemoryManager().Allocate(size JDWP_FILE_LINE);
    }

    inline void Allocatable::operator delete[](void* ptr) {
        AgentBase::GetMemoryManager().Free(ptr JDWP_FILE_LINE);
    }

    /**
     * The class provides automatic JVMTI memory freeing.
     * A pointer to the previously allocated memory by JVMTI environment
     * is stored in the JvmtiAutoFree constructor and automatically freed 
     * in destructor.
     */
    class JvmtiAutoFree {

    public:

        /**
         * A constructor.
         * A pointer to the JVMTI memory is saved here.
         */
        JvmtiAutoFree(void* ptr) throw() : m_ptr(ptr) { }

        /**
         * A destructor.
         * The saved pointer to the JVMTI memory is freed here.
         */
        ~JvmtiAutoFree() throw() {
            if (m_ptr != 0) {
                jvmtiError err;
                JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->
                    Deallocate(reinterpret_cast<unsigned char*>(m_ptr)));
                JDWP_ASSERT(err==JVMTI_ERROR_NONE);
            }
        }

    private:

        JvmtiAutoFree(const JvmtiAutoFree& other) : m_ptr(other.m_ptr) { }
        const JvmtiAutoFree& operator=(const JvmtiAutoFree& r) {
            return *this;
        }

        void* m_ptr;
    };

    /**
     * The class provides automatic agent memory deallocation.
     * A pointer to the previously allocated memory by the agent MemoryManager
     * is stored in the AgentAutoFree ctor and automatically freed in the dtor.
     */
    class AgentAutoFree {

    public:

        /**
         * A constructor.
         * A pointer to the agent memory is saved here.
         */
        AgentAutoFree(void* ptr JDWP_FILE_LINE_PAR) throw() : m_ptr(ptr) JDWP_FILE_LINE_INI { }

        /**
         * A destructor.
         * The saved pointer to the agent memory is freed here.
         */
        ~AgentAutoFree() throw() {
            if (m_ptr != 0) {
                AgentBase::GetMemoryManager().Free(m_ptr JDWP_FILE_LINE_MPAR);
            }
        }

    private:

        AgentAutoFree(const AgentAutoFree& other) : m_ptr(other.m_ptr) { }
        const AgentAutoFree& operator=(const AgentAutoFree& r) {
            return *this;
        }

        JDWP_FILE_LINE_DECL;
        void* m_ptr;
    };

#ifndef NDEBUG
    
    /**
     * The given class is used in the debug mode only for tracing entry/exit of 
     * agent functions.
     */
    class JdwpTraceEntry {

    public:

        /**
         * A constructor.
         * Traces method entry in the log.
         */
        JdwpTraceEntry(std::ostringstream& os, const char* file, int line, int kind)
            : m_stream(os), m_file(file), m_line(line), m_kind(kind) {
            AgentBase::GetLogManager().Trace(">> " + m_stream.str(), m_file, m_line, m_kind);
        }

        /**
         * A destructor.
         * Traces method exit in the log.
         */
        ~JdwpTraceEntry() {
            AgentBase::GetLogManager().Trace("<< " + m_stream.str(), m_file, m_line, m_kind);
        }

    private:
        std::ostringstream& m_stream;
        const char* m_file;
        int         m_line;
        int         m_kind;
    };
#endif // NDEBUG

} // namespace jdwp

#endif // _AGENT_BASE_H_
