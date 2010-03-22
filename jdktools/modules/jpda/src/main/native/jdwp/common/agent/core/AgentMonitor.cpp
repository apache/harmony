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
#include "AgentMonitor.h"
#include "ExceptionManager.h"
#include "jvmti.h"

using namespace jdwp;

AgentMonitor::AgentMonitor(const char* name) {
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, AgentBase::GetJvmtiEnv()->CreateRawMonitor(name, &m_monitor));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error calling CreateRawMonitor: %d", err));
    }
}

AgentMonitor::~AgentMonitor() {
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, AgentBase::GetJvmtiEnv()->DestroyRawMonitor(m_monitor));
}

void AgentMonitor::Enter() const {
    jvmtiError err = AgentBase::GetJvmtiEnv()->RawMonitorEnter(m_monitor);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error calling RawMonitorEnter: %d", err));
    }
}

void AgentMonitor::Wait(jlong timeout) const {
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, AgentBase::GetJvmtiEnv()->RawMonitorWait(m_monitor, timeout));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error calling RawMonitorWait: %d", err));
    }
}

void AgentMonitor::Notify() const {
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, AgentBase::GetJvmtiEnv()->RawMonitorNotify(m_monitor));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error calling RawMonitorNotify: %d", err));
    }
}

void AgentMonitor::NotifyAll() const {
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, AgentBase::GetJvmtiEnv()->RawMonitorNotifyAll(m_monitor));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error calling RawMonitorNotifyAll: %d", err));
    }
}

void AgentMonitor::Exit() const {
    jvmtiError err = AgentBase::GetJvmtiEnv()->RawMonitorExit(m_monitor);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error calling RawMonitorExit: %d", err));
    }
}
