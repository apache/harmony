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
// AgentMonitor.cpp

#include "AgentMonitor.h"
#include "jvmti.h"

using namespace jdwp;

AgentMonitor::AgentMonitor(const char* name) throw(AgentException) {
    jvmtiError err;
    JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->CreateRawMonitor(name, &m_monitor));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
}

AgentMonitor::~AgentMonitor() throw(AgentException) {
    jvmtiError err;
    JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->DestroyRawMonitor(m_monitor));
    // check for error only in debug mode
    JDWP_ASSERT(err==JVMTI_ERROR_NONE);
}

void AgentMonitor::Enter() const throw(AgentException) {
    jvmtiError err;
//    JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->RawMonitorEnter(m_monitor));
    err = AgentBase::GetJvmtiEnv()->RawMonitorEnter(m_monitor);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
}

void AgentMonitor::Wait(jlong timeout) const throw(AgentException) {
    jvmtiError err;
    JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->RawMonitorWait(m_monitor, timeout));
//    err = AgentBase::GetJvmtiEnv()->RawMonitorWait(m_monitor, timeout);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
}

void AgentMonitor::Notify() const throw(AgentException) {
    jvmtiError err;
    JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->RawMonitorNotify(m_monitor));
//    err = AgentBase::GetJvmtiEnv()->RawMonitorNotify(m_monitor);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
}

void AgentMonitor::NotifyAll() const throw(AgentException) {
    jvmtiError err;
    JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->RawMonitorNotifyAll(m_monitor));
//    err = AgentBase::GetJvmtiEnv()->RawMonitorNotifyAll(m_monitor);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
}

void AgentMonitor::Exit() const throw(AgentException) {
    jvmtiError err;
    //JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->RawMonitorExit(m_monitor));
    err = AgentBase::GetJvmtiEnv()->RawMonitorExit(m_monitor);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
}
