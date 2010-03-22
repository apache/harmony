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
 * AgentMonitor.h
 *
 */

#ifndef _AGENT_MONITOR_H_
#define _AGENT_MONITOR_H_

#include "AgentBase.h"
#include "Log.h"

#include "jvmti.h"

namespace jdwp {

    /**
     * The class provides wrapping for the JVMTI raw monitor.
     * The raw monitor ID is stored in <code>AgentMonitor</code> and safely
     * manipulated in the <code>AgentMonitor</code> methods. The raw monitor ID is
     * created in ctor and automatically destroyed in dtor.
     */
    class AgentMonitor : public AgentBase {

    public:

        /**
         * Constructs the JVMTI raw monitor.
         *
         * @param name - monitor name
         */
        AgentMonitor(const char* name) throw(AgentException);

        /**
         * Destructs the JVMTI raw monitor.
         */
        ~AgentMonitor() throw(AgentException);

        /**
         * Locks the JVMTI raw monitor.
         */
        void Enter() const throw(AgentException);

        /**
         * Suspends the current thread for the given time-out.
         *
         * @param timeout - wait time-out
         */
        void Wait(jlong timeout = 0) const throw(AgentException);

        /**
         * Notifies the suspended thread waiting on the given monitor.
         */
        void Notify() const throw(AgentException);

        /**
         * Notifies all suspended threads waiting on the given monitor.
         */
        void NotifyAll() const throw(AgentException);

        /**
         * Unlocks the JVMTI raw monitor.
         */
        void Exit() const throw(AgentException);

    private:
        jrawMonitorID m_monitor;
    };

    /**
     * The class provides automatic <code>AgentMonitor<code> locking/unlocking.
     * The monitor stored in a reference is locked in the 
     * <code>MonitorAutoLock</code> ctor and is automatically unlocked
     * in the dtor.
     */
    class MonitorAutoLock : public AgentBase {

    public:

        /**
         * The constructor locks the agent monitor.
         *
         * @param monitor - agent monitor
         */
        MonitorAutoLock(AgentMonitor &monitor
            JDWP_FILE_LINE_PAR) : m_lock(monitor) JDWP_FILE_LINE_INI {
            JDWP_TRACE_EX(LOG_KIND_MON, m_file, m_line, "Enter: " << &m_lock);
            m_lock.Enter();
        }

        /**
         * The constructor locks the agent monitor.
         *
         * @param monitor - agent monitor
         */
        MonitorAutoLock(AgentMonitor *monitor
            JDWP_FILE_LINE_PAR) : m_lock(*monitor) JDWP_FILE_LINE_INI {
            JDWP_TRACE_EX(LOG_KIND_MON, m_file, m_line, "Enter: " << &m_lock);
            m_lock.Enter();
        }

        /**
         * The destructor unlocks the agent monitor.
         */
        ~MonitorAutoLock() {
            JDWP_TRACE_EX(LOG_KIND_MON, m_file, m_line, "Exit : " << &m_lock);
            m_lock.Exit();
        }

    private:
        JDWP_FILE_LINE_DECL;
        AgentMonitor &m_lock;
    };
}

#endif // _AGENT_MONITOR_H_
