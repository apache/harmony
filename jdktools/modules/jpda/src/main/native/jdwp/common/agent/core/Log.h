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
 * Log.h
 *
 * Macros in the given file provide functionality for tracing
 * program execution in a debug mode except INFO, SIMPLE and 
 * ERROR, which work in a release mode as well as in a debug 
 * one. 
 */

#ifndef _LOG_H_
#define _LOG_H_

#include <stdarg.h>
#include "LogManager.h"

/**
 * Safe null-checking method for passing a string as a parameter.
 */
#define JDWP_CHECK_NULL(str) ( (str)==0 ? "(null)" : (str) )

/**
 * Macro definition for assertion checking in JDWP agent code.
 */
#ifdef NDEBUG
 #define JDWP_ASSERT(assert)
#else // !NDEBUG
 #define JDWP_ASSERT(assert) { \
    if (!(assert)) { \
        JDWP_TRACE(LOG_DEBUG, (LOG_ERROR_FL, "assert \"%s\" failed", #assert)); \
        ::exit(1); \
    } \
 }
#endif // !NDEBUG


/**
 * Defines to indicate the current levels of tracing available. 
 *  LOG_RELEASE indicates tracepoints that will be executed when
 * building in both release and debug configuration. 
 *  LOG_DEBUG indicates tracepoints that will be executed when 
 * building in debug configuration only. 
 */
#define LOG_RELEASE JDWP_TRACE_RELEASE
#define LOG_DEBUG JDWP_TRACE_DEBUG

/**
 * Macros to check if the specified kind of tracing is enabled.
 *  
 * 2 versions - one takes just the kind of tracing, the other 
 * can take a variable argument list as specified to the 
 * JDWP_TRACE macro.
 */
 #define JDWP_TRACE_ENABLED(kind) AgentBase::GetLogManager().TraceEnabled(kind, __FILE__, __LINE__, "")
 #define JDWP_TRACE_ENABLED_VAARGS(args) AgentBase::GetLogManager().TraceEnabled args

/**
 * Trace JVMTI calls - has format 
 *  JVMTI_TRACE(level, err, function_call)
 * where 
 *  level - the tracing level, either LOG_RELEASE or LOG_DEBUG
 *  err - the variable to store the return code in
 *  function_call - the function call to execute
 */
#define JVMTI_TRACE(level, err, function_call) { \
    JDWP_TRACE(level, (LOG_JVMTI_FL, ">> %s", #function_call)); \
    err = function_call; \
    JDWP_TRACE(level, (LOG_JVMTI_FL, "<< %s=%d", #function_call, err)); \
}

/**
* Macros providing trace for JDWP agent code 
*  
* Tracepoints are of the format: 
*  JDWP_TRACE(level, (kind, file, line, format, tokens));
* where 
*  level - either LOG_RELEASE or LOG_DEBUG
*  kind - the type of trace, e.g. LOG_KIND_EVENT
*  file - the file name
*  line - the line number
*  format - the format string, e.g. "Hello %s"
*  tokens - the tokens to be parsed into the format string,
*           e.g."World"
*/
#define _JDWP_TRACE(args) if (JDWP_TRACE_ENABLED_VAARGS(args)) AgentBase::GetLogManager().Trace args
#define _JDWP_TRACE_ENTRY(args) JdwpTraceEntry _tre args

#ifdef NDEBUG
#define JDWP_TRACE_DEBUG(func, args)
#else
#define JDWP_TRACE_DEBUG(func, args) func(args)
#endif
#define JDWP_TRACE_RELEASE(func, args) func(args)

#define JDWP_TRACE(level, args) level(_JDWP_TRACE, args)
#define JDWP_TRACE_ENTRY(level, args) level(_JDWP_TRACE_ENTRY, args)

/**
 * The following macros provide placement for file and line parameters in a debug mode1.
 */
#define JDWP_FILE_LINE , __FILE__, __LINE__
#define JDWP_FILE_LINE_PAR , const char *file, int line
#define JDWP_FILE_LINE_INI , m_file(file), m_line(line)
#define JDWP_FILE_LINE_MPAR , m_file, m_line
#define JDWP_FILE_LINE_DECL const char* m_file; int m_line;

/**
 * Utility defines for kind, file, line in the JDWP_TRACE macros 
 * above.
 */
#define LOG_CMD_FL LOG_KIND_CMD, __FILE__, __LINE__
#define LOG_EVENT_FL LOG_KIND_EVENT, __FILE__, __LINE__
#define LOG_PACKET_FL LOG_KIND_PACKET, __FILE__, __LINE__
#define LOG_THREAD_FL LOG_KIND_THREAD, __FILE__, __LINE__
#define LOG_DATA_FL LOG_KIND_DATA, __FILE__, __LINE__
#define LOG_MEMORY_FL LOG_KIND_MEMORY, __FILE__, __LINE__
#define LOG_MAP_FL LOG_KIND_MAP, __FILE__, __LINE__
#define LOG_JVMTI_FL LOG_KIND_JVMTI, __FILE__, __LINE__
#define LOG_FUNC_FL LOG_KIND_FUNC, __FILE__, __LINE__
#define LOG_MON_FL LOG_KIND_MON, __FILE__, __LINE__
#define LOG_UTIL_FL LOG_KIND_UTIL, __FILE__, __LINE__
#define LOG_PROG_FL LOG_KIND_PROG, __FILE__, __LINE__
#define LOG_LOG_FL LOG_KIND_LOG, __FILE__, __LINE__
#define LOG_INFO_FL LOG_KIND_INFO, __FILE__, __LINE__
#define LOG_ERROR_FL LOG_KIND_ERROR, __FILE__, __LINE__
#define LOG_SIMPLE_FL LOG_KIND_SIMPLE, __FILE__, __LINE__
#define LOG_NUM_FL LOG_KIND_NUM, __FILE__, __LINE__


#endif // _LOG_H_
