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
 * LogManager.h
 *
 */

#ifndef _LOG_MANAGER_H_
#define _LOG_MANAGER_H_

#include <iostream>
#include <string>

#include "AgentException.h"

namespace jdwp {

    /**
     * Log kinds enumeration.
     */
    enum {
        LOG_KIND_UNKNOWN = 0,
        LOG_KIND_CMD,
        LOG_KIND_EVENT,
        LOG_KIND_PACKET,
        LOG_KIND_THREAD,
        LOG_KIND_DATA,
        LOG_KIND_MEMORY,
        LOG_KIND_MAP,
        LOG_KIND_JVMTI,
        LOG_KIND_FUNC,
        LOG_KIND_MON,
        LOG_KIND_UTIL,
        LOG_KIND_PROG,
        LOG_KIND_LOG,
        LOG_KIND_INFO,
        LOG_KIND_ERROR,

        LOG_KIND_NUM
    };

    /**
     * Trace kinds enumeration.
     */
    enum {
        TRACE_KIND_NONE = 0,
        TRACE_KIND_FILTER_FILE,
        TRACE_KIND_ALWAYS
    };

    class AgentMonitor;

    /**
     * Log manager interface.
     */
    class LogManager {

    public:

        /**
         * Initializes the log manager.
         */
        virtual void Init(const char* log, const char* kindFilter, const char* srcFilter)
            throw(AgentException) = 0;

        /**
         * Cleanups the log manager.
         */
        virtual void Clean() throw() = 0;

        /**
         * Prints the given message of the Info type.
         *
         * @param message - the log message
         * @param file    - the name of the source file (__FILE__ macro)
         * @param line    - the number of the code line (__LINE__ macro)
         */
        virtual void Info(const std::string& message,
            const char *file, int line) throw() = 0;

        /**
         * Prints the given message of the Error type.
         *
         * @param message - the log message
         * @param file    - the name of the source file (__FILE__ macro)
         * @param line    - the number of the code line (__LINE__ macro)
         */
        virtual void Error(const std::string& message,
            const char *file, int line) throw() = 0;

        /**
         * Prints the given message of the Log type.
         *
         * @param message - the log message
         * @param file    - the name of the source file (__FILE__ macro)
         * @param line    - the number of the code line (__LINE__ macro)
         */
        virtual void Log(const std::string& message,
            const char *file, int line) throw() = 0;

        /**
         * Prints the given message of the Trace type.
         *
         * @param message - the log message
         * @param file    - the name of the source file (__FILE__ macro)
         * @param line    - the number of the code line (__LINE__ macro)
         * @param kind    - the log kind
         */
        virtual void Trace(const std::string& message,
            const char *file, int line, int kind) throw() = 0;

        /**
         * Checks whether the print to log is enabled for the Trace log type.
         *
         * @param file - the name of the source file (__FILE__ macro)
         * @param line - the number of the code line (__LINE__ macro)
         * @param kind - the log kind
         *
         * @return Boolean.
         */
        virtual bool TraceEnabled(const char *file, int line, int kind) throw() = 0;
    };

    /**
     * JDWP Log manager class.
     */
    class STDLogManager : public LogManager {

    public:

        /**
         * A constructor.
         */
        STDLogManager() throw();

        /**
         * Initializes the log manager.
         */
        void Init(const char* log, const char* kindFilter, const char* srcFilter)
            throw(AgentException);

        /**
         * Cleanups the log manager.
         */
        void Clean() throw();

        /**
         * Prints the given message of the Info type.
         *
         * @param message - the log message
         * @param file    - the name of the source file (__FILE__ macro)
         * @param line    - the number of the code line (__LINE__ macro)
         */
        void Info(const std::string& message,
            const char *file, int line) throw();

        /**
         * Prints the given message of the Error type.
         *
         * @param message - the log message
         * @param file    - the name of the source file (__FILE__ macro)
         * @param line    - the number of the code line (__LINE__ macro)
         */
        void Error(const std::string& message,
            const char *file, int line) throw();

        /**
         * Prints the given message of the Log type.
         *
         * @param message - the log message
         * @param file    - the name of the source file (__FILE__ macro)
         * @param line    - the number of the code line (__LINE__ macro)
         */
        void Log(const std::string& message,
            const char *file, int line) throw();

        /**
         * Prints the given message of the Trace type.
         *
         * @param message - the log message
         * @param file    - the name of the source file (__FILE__ macro)
         * @param line    - the number of the code line (__LINE__ macro)
         * @param kind    - the log kind
         */
        void Trace(const std::string& message,
            const char *file, int line, int kind) throw();

        /**
         * Checks whether the print to log is enabled for the Trace log type.
         *
         * @param file - the name of the source file (__FILE__ macro)
         * @param line - the number of the code line (__LINE__ macro)
         * @param kind - log kind
         *
         * @return Boolean.
         */
        bool TraceEnabled(const char *file, int line, int kind) throw();

        /**
         * Gets a short file name from the full path.
         *
         * @param filepath - the path name
         *
         * @return Zero terminated string.
         */
        static const char* BaseName(const char* filepath) throw();

    private:
        const char*     m_fileFilter;
        std::ostream*   m_logStream;
        AgentMonitor*   m_monitor;
        int             m_logKinds[LOG_KIND_NUM];
    };
}

#endif // _LOG_MANAGER_H_
