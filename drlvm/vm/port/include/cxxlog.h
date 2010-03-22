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
#ifndef _CXXLOG_H
#define _CXXLOG_H

/**
 * @file
 * Additional C++ logging macro definitions.
 */
#include "clog.h"
#include "loggerstring.h"
#include "logparams.h"

#define LDIE(message_number, messagedef_and_params) { \
    LogParams log_params(0x4c444945, message_number); \
    log_params << messagedef_and_params; \
    log_printf(log_params.release()); \
    log_abort(); \
}

#define LWARN(message_number, messagedef_and_params) \
    if (log_is_warn_enabled()) { \
        LogParams log_params(0x5741524e, message_number); \
        log_params << messagedef_and_params; \
        log_printf(log_params.release()); \
    }

#define INFO(message) INFO2(LOG_DOMAIN, message)
#define INFO2(category, message) LOG_SITE(info, category, \
        log_header(category, __FILELINE__, __PRETTY_FUNCTION__); \
        LoggerString logger_string; \
        logger_string << message; \
    )

#define LINFO(message_number, messagedef_and_params) \
    LINFO2(LOG_DOMAIN, message_number, messagedef_and_params)
#define ECHO(message) INFO2(LOG_INFO, message)
#define LECHO(message_number, messagedef_and_params) \
    LINFO2(LOG_INFO, message_number, messagedef_and_params)
#define LINFO2(category, message_number, messagedef_and_params) LOG_SITE(info, category, \
        LogParams log_params(0x4543484f, message_number); \
        log_params << messagedef_and_params; \
        log_printf(log_params.release()); \
    )

#define TRACE(message)  TRACE2(LOG_DOMAIN, message)

/**
 * Lightweight macro to replace a standard C <code>assert()</code>
 * and debugging <code>printf</code>.   
 */
#ifdef NDEBUG
#    define TRACE2(category, message)
#else
#    define TRACE2(category, message) LOG_SITE(trace, category, \
        log_printf("[trace] "); \
        log_header(category, __FILELINE__, __PRETTY_FUNCTION__); \
        LoggerString logger_string; \
        logger_string << message; \
    )
#endif /* NDEBUG */

#endif /* _CXXLOG_H */
