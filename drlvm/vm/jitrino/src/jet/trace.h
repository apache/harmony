/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Alexander Astapchuk
 */
 
/**
 * @file 
 * @brief Debugging stuff declaration - tracing and logging utilities.
 */
 
#if !defined(__TRACE_H_INCLUDED__)
#define __TRACE_H_INCLUDED__

#include "mib.h"
#include "open/rt_types.h"

#include <string>
using std::string;

namespace Jitrino {
namespace Jet {

class JFrame;

#ifdef _DEBUG
/**
 * @def LOG_FILE_NAME
 * @brief Name of the file where the \link #dbg debug output \endlink goes.
 */
    #define LOG_FILE_NAME           "jet.dbg.log"
#else
    #define LOG_FILE_NAME           "jet.log"
#endif

#ifdef _DEBUG
    /**
     * @def RUNTIME_LOG_FILE_NAME
     * @brief Name of the file where the \link #rt_dbg debug output from the 
     *        managed code \endlink goes.
     */
    #define RUNTIME_LOG_FILE_NAME   "jet.dbg.rt.log"
#else
    #define RUNTIME_LOG_FILE_NAME   "jet.rt.log"
#endif

/**
 * @brief Performs debugging output, logged to file #LOG_FILE_NAME
 *
 * The function creates file if it does not exist, or overwrites its content
 * if it does exist.
 * @note Use with caution in multi-threaded environment. The function itself 
 *       is thread-safe (no static variables used except of \c FILE*). 
 *       However no syncronization performed during the output, so the output
 *       from different threads may interleave with each other.
 * 
 * @param frmt - format specificator, same as printf()'s
 */
void dbg(const char * frmt, ...);

/**
 * @brief Used to perform debugging output from the managed code, logged to 
 *        #RUNTIME_LOG_FILE_NAME.
 *
 * The output goes to file #RUNTIME_LOG_FILE_NAME to avoid intermixing with
 * output produced by #dbg(const char*, ...).
 *
 * The function also uses 2 counters: depth counter and total number of 
 * outputs.
 *
 * The depth counter incremented if the \c msg string starts with 'enter',
 * and decremented if the string starts with 'exit' (case-insensitive). The 
 * 'enter' and 'exit' strings are the strings used to track method's 
 * \link #DBG_TRACE_EE enter/exit \endlink, so the depth reflects somehow the
 * real call depth.
 * 
 * Total number of outputs is simple counter incremented on each rt_dbg call.
 *  
 * The output is the \c msg string, preceded with depth counter, and with 
 * total outputs counter at the end.
 *
 * The counters may be used to install breakpoints by value.
 *
 * @note Use with caution in multi-threaded environment. The function itself
 *       is thread-safe, but counters work is perfromed without interlocked 
 *       operations, so may be inadequate and may show different values from 
 *       one run to another for multiple threads.
 * @note The depth counter does not reflect if method finishes abruptly.
 *
 * @param msg - message to print out
 */
void __stdcall dbg_rt_out(const char * msg) stdcall__;

/**
 * Same as dbg(), but the output goes into #RUNTIME_LOG_FILE_NAME.
 *
 * The output goes through #dbg_rt_out and is processed accordingly.
 */
void dbg_rt(const char * frmt, ...);

/**
 * @brief Dumps the native stack frame, addressed by \c addr.
 * @note implementation is obsolete, need update.
 * @todo implementation is obsolete, need update.
 */
void dump_frame(const JitFrameContext* ctx, const MethodInfoBlock& info);

 /**
  * @brief Removes leading and trailing white spaces.
  */
::std::string trim(const char * p);

extern unsigned g_tbreak_id;

}
}; // ~namespace Jitrino::Jet


#endif      // ~__TRACE_H_INCLUDED__
