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
 * LogManager.h
 *
 */

#ifndef _LOG_MANAGER_H_
#define _LOG_MANAGER_H_

#include <stdarg.h>
#include <stdlib.h>
#include "vmi.h"
#include "hyport.h"

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
        LOG_KIND_SIMPLE,
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
        virtual void Init(const char* log, const char* kindFilter, const char* srcFilter) {};

        /**
         * Cleanups the log manager.
         */
        virtual void Clean() {};

        /* Trace methods */
        virtual void Trace(int kind, const char* file, int line, const char* format, ...) {};

        virtual void TraceEnter(int kind, const char* file, int line, const char* format, ...) {};
        virtual void TraceEnterv(int kind, const char* file, int line, const char* format, va_list args) {};
        virtual void TraceExit(int kind, const char* file, int line, const char* format) {};        

        static inline void EmptyFunction(int kind, const char* file, int line, const char* format, ...) {};

        /**
         * Checks whether the print to log is enabled for the Trace log type.
         *
         * @param file - the name of the source file (__FILE__ macro)
         * @param line - the number of the code line (__LINE__ macro)
         * @param kind - the log kind
         *
         * @return Boolean.
         */
        virtual bool TraceEnabled(int kind, const char* file, int line, const char* format, ...) {return false;};

        inline void* operator new(size_t size) {
            return malloc(size);
        }

        inline void operator delete(void* ptr) {
            free(ptr);
        }

    private:
        virtual void Tracev(int kind, const char* file, int line, const char* format, va_list args) {};
    };

    /**
     * JDWP Log manager class.
     */
    class STDLogManager : public LogManager {

    public:

        /**
         * A constructor.
         */
        STDLogManager();

        /**
         * Initializes the log manager.
         */
        void Init(const char* log, const char* kindFilter, const char* srcFilter);

        /**
         * Cleanups the log manager.
         */
        void Clean();

        /* Trace methods */
        void Trace(int kind, const char* file, int line, const char* format, ...);

        void TraceEnter(int kind, const char* file, int line, const char* format, ...);
        void TraceEnterv(int kind, const char* file, int line, const char* format, va_list args);
        void TraceExit(int kind, const char* file, int line, const char* format);


        /**
         * Checks whether the print to log is enabled for the Trace log type.
         *
         * @param file - the name of the source file (__FILE__ macro)
         * @param line - the number of the code line (__LINE__ macro)
         * @param kind - log kind
         *
         * @return Boolean.
         */
        bool TraceEnabled(int kind, const char* file, int line, const char* format, ...);

        /**
         * Gets a short file name from the full path.
         *
         * @param filepath - the path name
         *
         * @return Zero terminated string.
         */
        static const char* BaseName(const char* filepath);

    private:
        void Tracev(int kind, const char* file, int line, const char* format, va_list args);

        const char*     m_fileFilter;
        IDATA           m_fileHandle;
        AgentMonitor*   m_monitor;
        int             m_logKinds[LOG_KIND_NUM];
    };

}

#endif // _LOG_MANAGER_H_
