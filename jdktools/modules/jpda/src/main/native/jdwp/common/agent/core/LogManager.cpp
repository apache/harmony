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
// LogManager.cpp

#include <iostream>
#include <fstream>
#include <cstring>
#include <string>

#include "LogManager.h"
#include "AgentMonitor.h"

using namespace jdwp;
using namespace std;

static struct {
    const char* disp;
    const char* name;
}
s_logKinds[LOG_KIND_NUM] = {
    { "  UNK", "UNK" },
    { "  CMD", "CMD" },
    { "EVENT", "EVENT" },
    { " PACK", "PACK" },
    { " THRD", "THRD" },
    { " DATA", "DATA" },
    { "  MEM", "MEM" },
    { "  MAP", "MAP" },
    { "JVMTI", "JVMTI" },
    { " FUNC", "FUNC" },
    { "  MON", "MON" },
    { " UTIL", "UTIL" },
    { " PROG", "PROG" },
    { "  LOG", "LOG" },
    { " INFO", "INFO" },
    { "ERROR", "ERROR" }
};

STDLogManager::STDLogManager() throw() :
    m_logStream(&clog),
    m_fileFilter(0),
    m_monitor(0)
{
    for (int i = 0; i < LOG_KIND_NUM; i++) {
        m_logKinds[i] = TRACE_KIND_NONE;
    }
    m_logKinds[LOG_KIND_INFO] = TRACE_KIND_ALWAYS;
    m_logKinds[LOG_KIND_ERROR] = TRACE_KIND_ALWAYS;
}

void STDLogManager::Init(const char* log, const char* kindFilter, const char* srcFilter)
    throw(AgentException)
{
    if (srcFilter != 0 && strcmp("all", srcFilter) == 0) {
        srcFilter = 0; // null equvivalent to "all"
    }

    m_fileFilter = srcFilter;
    bool hasFiles = m_fileFilter != 0;// && strstr(m_fileFilter, ".cpp") != 0;

    if (kindFilter == 0 || strcmp("none", kindFilter) == 0) {
        for (int i = 0; i < LOG_KIND_NUM; i++) {
            m_logKinds[i] = TRACE_KIND_NONE;
        }
    }
    else if (strcmp("all", kindFilter) == 0) {
        for (int i = 0; i < LOG_KIND_NUM; i++) {
            m_logKinds[i] = hasFiles ? TRACE_KIND_FILTER_FILE : TRACE_KIND_ALWAYS;
        }
    }
    else {
        for (int i = 0; i < LOG_KIND_NUM; i++) {
            if (strstr(kindFilter, s_logKinds[i].name) != 0) {
                m_logKinds[i] = hasFiles ? TRACE_KIND_FILTER_FILE : TRACE_KIND_ALWAYS;
            }
            else {
                m_logKinds[i] = TRACE_KIND_NONE;
            }
        }
    }

    m_logKinds[LOG_KIND_INFO] = TRACE_KIND_ALWAYS;
    m_logKinds[LOG_KIND_ERROR] = TRACE_KIND_ALWAYS;

    if (log == 0) {
        m_logStream = &clog;
    } else {
        m_logStream = new ofstream(log);
        if (m_logStream == 0) {
            fprintf(stderr, "Cannot open log file: %s\n", log);
            m_logStream = &clog;
        }
    }

    m_monitor = new AgentMonitor("_agent_Log");
}

void STDLogManager::Clean() throw()
{
    if (m_monitor != 0) {
        m_monitor->Enter();
    }

    if (m_logStream != &clog) {
        delete m_logStream;
        m_logStream = &clog;
    }

    // prevent logging in destruction of log's monitor
    AgentMonitor *monitor = m_monitor;
    m_monitor = 0;
    if (0 != monitor) {
        monitor->Exit();
        delete monitor; // <= STDLogManager instance will be called with m_monitor == 0
    }
}

// extract basename from filename

const char* STDLogManager::BaseName(const char* filepath) throw()
{
    size_t len;

    if (filepath == 0)
        return "";

    len = strlen(filepath);
    if (len == 0)
        return filepath;

    for (size_t i = len-1; i > 0; i--) {
        if (filepath[i] == '/' || filepath[i] == '\\' ) {
            return &filepath[i+1];
        }
    }

    return filepath;
}

// STDLogManager intended to use cout, cerr and clog streams.

void STDLogManager::Info(const std::string& message,
        const char *file, int line) throw()
{
    Trace(message, file, line, LOG_KIND_INFO);
}

void STDLogManager::Error(const std::string& message,
        const char *file, int line) throw()
{
    Trace(message, file, line, LOG_KIND_ERROR);
}

void STDLogManager::Log(const std::string& message,
        const char *file, int line) throw()
{
    Trace(message, file, line, LOG_KIND_LOG);
}

void STDLogManager::Trace(const string& message,
        const char *file, int line, int kind) throw()
{
    if (TraceEnabled(file, line, kind)){
        if (m_monitor != 0) {
            m_monitor->Enter();
        }

        file = BaseName(file);
        std::ostream* logStream = m_logStream;
        if (LOG_KIND_ERROR == kind) {
            logStream = &cerr;
        }
        else if (LOG_KIND_INFO == kind) {
            logStream = &cout;
        }

        *logStream
                << s_logKinds[kind].disp << ": "
                << "[" << file << ":" << line << "] "
                << message << endl;

        // duplicate ERROR and INFO message in the log and in the cerr/cout output
        if (logStream != m_logStream && m_logStream != &clog) {
            *m_logStream
                << s_logKinds[kind].disp << ": "
                << "[" << file << ":" << line << "] "
                << message << endl;
        }

        if (m_monitor != 0) {
            m_monitor->Exit();
        }
    }
}

bool STDLogManager::TraceEnabled(const char *file, int line, int kind) throw()
{
    if (TRACE_KIND_FILTER_FILE == m_logKinds[kind]) {
        return strstr(m_fileFilter, BaseName(file)) != 0;
    }
    else {
        return TRACE_KIND_ALWAYS == m_logKinds[kind];
    }
}
