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

#include "LogManager.h"
#include "AgentMonitor.h"
#include "AgentBase.h"
#include "vmi.h"
#include "hyport.h"

#include <string.h>

using namespace jdwp;

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
    { "ERROR", "ERROR" },
    { "SIMPLE", "SIMPLE" } 
};

STDLogManager::STDLogManager() :
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
    m_logKinds[LOG_KIND_SIMPLE] = TRACE_KIND_ALWAYS;

    if (log == 0) {
        m_fileHandle = -1;
    } else {
        PORT_ACCESS_FROM_JAVAVM(AgentBase::GetJavaVM());
        hyfile_unlink(log); // We do not care about the result of unlink here, failure may be because the file does not exist
        m_fileHandle = hyfile_open(log, HyOpenCreate | HyOpenWrite, 0660);
        if (m_fileHandle == -1) {
            hytty_printf(privatePortLibrary, "Cannot open log file: %s", log);
        }
    }

    m_monitor = new AgentMonitor("_agent_Log");
}

void STDLogManager::Clean()
{
    if (m_monitor != 0) {
        m_monitor->Enter();
    }

    if (m_fileHandle != -1) {
        PORT_ACCESS_FROM_JAVAVM(AgentBase::GetJavaVM());
        hyfile_close(m_fileHandle);
        m_fileHandle = -1;
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

const char* STDLogManager::BaseName(const char* filepath)
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

void STDLogManager::Tracev(int kind, const char* file, int line, const char* format, va_list args) {
    // No need to check kind here, as this is a private function and all public functions using it should check kind before calling tracev
    PORT_ACCESS_FROM_JAVAVM(AgentBase::GetJavaVM());
    if (m_monitor != 0) {
        m_monitor->Enter();
    }

    I_64 timeMillis = hytime_current_time_millis();
    char timestamp[9]; // Buffer for the timestamp
    hystr_ftime(timestamp, 9, "%H:%M:%S");

    int currentMillis = (int)(timeMillis%1000); // get the 1000ths of a second

    char message[5000]; //Buffer of a large size, big enough to contain formatted message strings
    hystr_vprintf(message, 5000, format, args);

    file = BaseName(file);
    if (LOG_KIND_SIMPLE == kind) {
        hyfile_printf(privatePortLibrary, HYPORT_TTY_OUT, "%s\n", message);
    } else {
        hyfile_printf(privatePortLibrary, HYPORT_TTY_ERR, "%s.%03d %s: [%s:%d] %s\n", timestamp, currentMillis, s_logKinds[kind].disp, file, line, message);
    }

    // duplicate ERROR and INFO message in the log and in the cerr/cout output
    if (m_fileHandle != -1) {
        hyfile_printf(privatePortLibrary, m_fileHandle, "%s.%03d %s: [%s:%d] %s\n", timestamp, currentMillis, s_logKinds[kind].disp, file, line, message);
    }

    if (m_monitor != 0) {
        m_monitor->Exit();
    }
}

void STDLogManager::Trace(int kind, const char* file, int line, const char* format, ...) {
    PORT_ACCESS_FROM_JAVAVM(AgentBase::GetJavaVM());
    va_list args;
    va_start(args, format);    
    Tracev(kind, file, line, format, args);
    va_end(args);
}

void STDLogManager::TraceEnter(int kind, const char* file, int line, const char* format, ...) {
    PORT_ACCESS_FROM_JAVAVM(AgentBase::GetJavaVM());
    char *message = (char*)hymem_allocate_memory(strlen(format) + 4);
    va_list args;
    va_start(args, format);
    hystr_printf(privatePortLibrary, message, (U_32)(strlen(format) + 4), ">> %s", format);
    Tracev(kind, file, line, message, args);
    va_end(args);
    hymem_free_memory(message);
}

void STDLogManager::TraceExit(int kind, const char* file, int line, const char* format) {
    PORT_ACCESS_FROM_JAVAVM(AgentBase::GetJavaVM());
    const char *openBracket = strchr(format, '(');
    char *message = (char*)hymem_allocate_memory(openBracket - format + 3);
    hystr_printf(privatePortLibrary, message, (U_32)(openBracket - format + 2), format);
    Trace(kind, file, line, "<< %s)", message);
    hymem_free_memory(message);
}

void STDLogManager::TraceEnterv(int kind, const char* file, int line, const char* format, va_list args) {
    PORT_ACCESS_FROM_JAVAVM(AgentBase::GetJavaVM());
    char *message = (char*)hymem_allocate_memory(strlen(format) + 5);
    hystr_printf(privatePortLibrary, message, (U_32)(strlen(format) + 4), ">> %s", format);
    Tracev(kind, file, line, message, args);
    hymem_free_memory(message);
}

bool STDLogManager::TraceEnabled(int kind, const char* file, int line, const char* format, ...) {
    if (TRACE_KIND_FILTER_FILE == m_logKinds[kind]) {
        return strstr(m_fileFilter, BaseName(file)) != 0;
    }
    else {
        return TRACE_KIND_ALWAYS == m_logKinds[kind];
    }
}

