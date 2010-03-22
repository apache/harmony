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
#ifndef _LOG_H_
#define _LOG_H_

#include <sstream>
#include "LogManager.h"

/**
 * @file
 * Log.h
 *
 * Macros in the given file provide functionality for tracing
 * program execution in a debug mode except INFO and ERROR,
 * which work in a release mode as well as in a debug one.
 */

/**
 * Safe null-checking method for passing a string as a parameter.
 */
#define JDWP_CHECK_NULL(str) ( (str)==0 ? "(null)" : (str) )

/**
 * Traces messages using the corresponding method of a log manager.
 */
#define JDWP_MESSAGE(method, message) { \
    std::ostringstream _oss; \
    _oss << message; \
    AgentBase::GetLogManager().method(_oss.str(), __FILE__, __LINE__); \
}

/**
 * Traces INFO kind of the messages.
 */
#define JDWP_INFO(message) JDWP_MESSAGE(Info, message)

/**
 * Traces ERROR kind of the messages.
 */
#define JDWP_ERROR(message) JDWP_MESSAGE(Error, message)

/**
 * Traces messages of a specific kind. The macro is empty in a release mode.
 */
#define JDWP_TRACE(kind, message) JDWP_TRACE_EX(kind, __FILE__, __LINE__, message)

#define JDWP_TRACE_CMD(message) JDWP_TRACE(LOG_KIND_CMD, message)
#define JDWP_TRACE_EVENT(message) JDWP_TRACE(LOG_KIND_EVENT, message)
#define JDWP_TRACE_PACKET(message) JDWP_TRACE(LOG_KIND_PACKET, message)
#define JDWP_TRACE_THREAD(message) JDWP_TRACE(LOG_KIND_THREAD, message)
#define JDWP_TRACE_DATA(message) JDWP_TRACE(LOG_KIND_DATA, message)
#define JDWP_TRACE_MEMORY(message) JDWP_TRACE(LOG_KIND_MEMORY, message)
#define JDWP_TRACE_MAP(message) JDWP_TRACE(LOG_KIND_MAP, message)
#define JDWP_TRACE_JVMTI(message) JDWP_TRACE(LOG_KIND_JVMTI, message)
#define JDWP_TRACE_FUNC(message) JDWP_TRACE(LOG_KIND_FUNC, message)
#define JDWP_TRACE_MON(message) JDWP_TRACE(LOG_KIND_MON, message)
#define JDWP_TRACE_UTIL(message) JDWP_TRACE(LOG_KIND_UTIL, message)
#define JDWP_TRACE_PROG(message) JDWP_TRACE(LOG_KIND_PROG, message)

/**
 * Traces the JVMTI kind of the messages.
 */
#define JVMTI_TRACE(err, function_call) { \
    JDWP_TRACE_JVMTI(">> " #function_call); \
    err = function_call; \
    JDWP_TRACE_JVMTI("<< " #function_call "=" << err); \
}

/**
 * Prints the message and terminates the program execution.
 */
#define JDWP_DIE(message) { \
    JDWP_ERROR(message); \
    exit(1); \
}

#ifdef NDEBUG

/**
 * The given macros are empty in a release mode.
 */
#define JDWP_LOG(message)
#define JDWP_TRACE_EX(kind, file, line, message)
#define JDWP_TRACE_ENTRY(message)
#define JDWP_ASSERT(assert)
#define JDWP_TRACE_ENABLED(kind) false

#define JDWP_FILE_LINE
#define JDWP_FILE_LINE_PAR
#define JDWP_FILE_LINE_INI
#define JDWP_FILE_LINE_MPAR
#define JDWP_FILE_LINE_DECL

#else // !NDEBUG

/**
 * Traces LOG kind of the messages.
 */
#define JDWP_LOG(message) JDWP_MESSAGE(Log, message)

/**
 * Traces messages of a specific kind.
 */
#define JDWP_TRACE_EX(kind, file, line, message){ \
    std::ostringstream _oss; \
    _oss << message; \
    AgentBase::GetLogManager().Trace(_oss.str(), file, line, kind); \
}

/**
 * Traces the function kind of the messages.
 */
#define JDWP_TRACE_ENTRY(message) \
    std::ostringstream _ose; \
    _ose << message; \
    JdwpTraceEntry _tre(_ose, __FILE__, __LINE__, LOG_KIND_FUNC);

/**
 * Verifies the expression and terminates the program execution, if it
 * is <code>FALSE</code>.
 */
#define JDWP_ASSERT(assert) { \
    if (!(assert)) { \
        JDWP_DIE("assert \"" #assert "\" failed"); \
    } \
}

/**
 * Checks whether a specific kind of messages is enabled for logging.
 */
#define JDWP_TRACE_ENABLED(kind) AgentBase::GetLogManager().TraceEnabled(__FILE__, __LINE__, kind)

/**
 * The following macros provide placement for file and line parameters in a debug mode1.
 */
#define JDWP_FILE_LINE , __FILE__, __LINE__
#define JDWP_FILE_LINE_PAR , const char *file, int line
#define JDWP_FILE_LINE_INI , m_file(file), m_line(line)
#define JDWP_FILE_LINE_MPAR , m_file, m_line
#define JDWP_FILE_LINE_DECL const char* m_file; int m_line;

#endif // NDEBUG

#endif // _LOG_H_
