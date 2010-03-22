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
#ifndef _CLOG_H
#define _CLOG_H

/**
 * @file
 * C logging macro definitions.
 */
#include "logger.h"

/**
 * Macro expansion. It is used, for example, to combine
 * a filename and a line number in
 * <code>__FILELINE__</code> macro.
 */
#define EXPAND_(a) # a
#define EXPAND(a) EXPAND_(a)

/**
 * Expands to a function name.
 */
#define __FILELINE__ __FILE__ ":" EXPAND(__LINE__)

#ifndef __PRETTY_FUNCTION__
#   if defined(_MSC_VER) && (defined(__INTEL_COMPILER) || _MSC_VER >= 1300)
#       define __PRETTY_FUNCTION__ __FUNCSIG__
#   else
#       define __PRETTY_FUNCTION__ __func__
#   endif
#endif

#ifndef LOG_DOMAIN
#define LOG_DOMAIN "harmonyvm"
#endif

/**
 * Prints a simple string message to stdout and exits the program.
 * This macro call works when logging subsystem is not yet fully
 * initialized.
 */
#define DIE(message) { \
    log_printf("[error] "); \
    log_header(LOG_DOMAIN, __FILELINE__, __PRETTY_FUNCTION__); \
    log_printf message; \
    log_abort(); \
}

#define WARN(message) \
    if (log_is_warn_enabled()) { \
        log_printf("[warning] "); \
        log_header(LOG_DOMAIN, __FILELINE__, __PRETTY_FUNCTION__); \
        log_printf message; \
        log_printf("\n"); \
    }

#define CTRACE(message) CTRACE2(LOG_DOMAIN, message)

#define VERIFY(expr, message) \
    if(!(expr)) { \
        DIE(message); \
    }

#define VERIFY_SUCCESS(func) { \
    int ret = func; \
    VERIFY(0 == ret, \
        ("a call to " #func " returned a non-zero error code %d", ret)); \
}

#define LOG_SITE(level, category, out)  { \
    static struct LogSite log_site = {LOG_UNKNOWN, NULL}; \
    if (log_site.state && (log_site.state == LOG_ENABLED \
    || log_cache(log_is_##level##_enabled(category), &log_site))) { \
        out \
        log_printf("\n"); \
    } \
}

/**
 * Lightweight macro to replace a standard C <code>assert()</code>
 * and debugging <code>printf</code>.   
 */
#ifdef NDEBUG
#    define ASSERT(expr, message)
#    define CTRACE2(category, message)
#else
#    define ASSERT(expr, message)  VERIFY(expr, message)
#    define CTRACE2(category, message) LOG_SITE(trace, category, \
        log_printf("[trace] "); \
        log_header(category, __FILELINE__, __PRETTY_FUNCTION__); \
        log_printf message; \
    )

#endif /* NDEBUG */

#endif /* _CLOG_H */
