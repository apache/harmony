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
 * @file
 * ClassManager.h
 *
 * Provides initialization of JDWP agent.
 */

#ifndef _AGENT_MANAGER_H_
#define _AGENT_MANAGER_H_

#include "jni.h"
#include "jvmti.h"
#include "AgentBase.h"

namespace jdwp {

    /**
     * This class provides initialization of JDWP agent.
     */
    class AgentManager : public AgentBase {

    public :
        
        /**
         * A constructor.
         */
        AgentManager() {
            m_isStarted = false;
        }

        /**
         * A destructor.
         */
        ~AgentManager() {
        }
        
        /**
         * Initializes all agent modules.
         */
        int Init(jvmtiEnv *jvmti, JNIEnv *jni);

        /**
         * Start all agent threads.
         */
        int Start(jvmtiEnv *jvmti, JNIEnv *jni);

        /**
         * Stop all agent threads.
         */
        void Stop(JNIEnv *jni);

        /**
         * Clean all agent modules.
         */
        void Clean(JNIEnv *jni);

        /*                      
         * Returns started status of this agent.
         */
        bool IsStarted() {
            return m_isStarted;
        }

        /*
         * Sets started status of this agent.
         */
        void SetStarted(bool isStarted) {
            m_isStarted = isStarted;
        }

        /**
         * Enables catching initial EXCEPTION event to launch debugger.
         */
        int EnableInitialExceptionCatch(jvmtiEnv *jvmti, JNIEnv *jni);

        /**
         * Disables catching initial EXCEPTION event to launch debugger.
         */
        int DisableInitialExceptionCatch(jvmtiEnv *jvmti, JNIEnv *jni);

    private :

        bool volatile m_isStarted;

    }; //class AgentManager

}// namespace jdwp

#endif //_AGENT_MANAGER_H_
