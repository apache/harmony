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
 * OptionParser.h
 *
 * Provides access to startup options and actual capabilities of the agent.
 * Parses command-line options of the agent.
 */

#ifndef _OPTION_PARSER_H_
#define _OPTION_PARSER_H_

#include "AgentBase.h"
#include "ExceptionManager.h"

namespace jdwp {

    /**
     * The class provides certain means for parsing and manipulating agent options
     * that are passed to the command line of the target VM.
     */
    class OptionParser : public AgentBase {

    public:

        /**
         * A constructor.
         */
        OptionParser();

        /**
         * A destructor.
         */
        ~OptionParser();

        /**
         * Parses the input string containing the arguments passed to the agent.
         *
         * @param str - the string containing agent arguments
         */
        int Parse(const char* str);

        /**
         * Returns a number of parsed options.
         */
        int GetOptionCount() const {
            return m_optionCount;
        }

        /**
         * Gets an option name and value by the index.
         *
         * @param i     - the option index 
         * @param name  - the option name
         * @param value - the option value
         */
#ifndef NDEBUG
        int GetOptionByIndex(int i, const char *&name, const char *&value)
                const {
            if (i < m_optionCount) {
                name = m_options[i].name;
                value = m_options[i].value;
                return JDWP_ERROR_NONE;
            } else {
                AgentException ex(JDWP_ERROR_INVALID_INDEX);
                JDWP_SET_EXCEPTION(ex);
                return JDWP_ERROR_INVALID_INDEX;
            }
        }
#endif /* NDEBUG */

        /**
         * Looks for an option with the given name.
         *
         * @param name - the option name
         */
        const char *FindOptionValue(const char *name) const;

        /**
         * Returns a value for the agent's <code>help</code> option.
         *
         * @return Boolean.
         */
        bool GetHelp() const {
            return m_help;
        }

        /**
         * Returns a value for the agent's <code>version</code> option.
         *
         * @return Boolean.
         */
        bool GetVersion() const {
            return m_version;
        }

        /**
         * Returns a value for the agent's <code>suspend</code> option.
         *
         * @return Boolean.
         */
        bool GetSuspend() const {
            return m_suspend;
        }

        /**
         * Returns a value for the agent's <code>server</code> option.
         *
         * @return Boolean.
         */
        bool GetServer() const {
            return m_server;
        }

        /**
         * Returns a value for the agent's <code>onuncaught</code> option.
         *
         * @return Boolean.
         */
        bool GetOnuncaught() const {
            return m_onuncaught;
        }

        /**
         * Returns a time-out value for transport operations.
         *
         * @return Java long value.
         */
        jlong GetTimeout() const {
            return m_timeout;
        }

        /**
         * Returns the name of the JDWP transport.
         *
         * @return Zero-terminated string.
         */
        const char *GetTransport() const {
            return m_transport;
        }

        /**
         * Returns the address for the socket transport to connect to.
         *
         * @return Zero-terminated string.
         */
        const char *GetAddress() const {
            return m_address;
        }

        /**
         * Returns the name for log file.
         *
         * @return Zero-terminated string.
         */
        const char *GetLog() const {
            return m_log;
        }

        /**
         * Returns the log kind filter.
         *
         * @return Zero-terminated string.
         */
        const char *GetTraceKindFilter() const {
            return m_kindFilter;
        }

        /**
         * Returns the source-files filter.
         *
         * @return Zero-terminated string.
         */
        const char *GetTraceSrcFilter() const {
            return m_srcFilter;
        }

        /**
         * Returns a value for the agent's <code>onthrow</code> option.
         *
         * @return Zero-terminated string.
         */
        const char *GetOnthrow() const {
            return m_onthrow;
        }

        /**
         * Returns a value for the agent's <code>launch</code> option.
         *
         * @return Zero-terminated string.
         */
        const char *GetLaunch() const {
            return m_launch;
        }

    private:

        /**
         * The helper structure containing the option pair: name and value.
         */
        struct Option {
            const char *name;
            const char *value;
        };

        /**
         * Checks if the input string is "y" or "n"
         *
         * @param str - the input null-terminated string
         *
         * @exception IllegalArgumentException.
         */
        bool IsValidBool(const char *str);

        /**
         * The helper converting string to boolean.
         *
         * @param str - the input null-terminated string
         */
        bool AsciiToBool(const char *str);

        int m_optionCount;
        char *m_optionString;
        Option *m_options;

        bool m_help;
        bool m_version;
        bool m_suspend;
        bool m_server;
        bool m_onuncaught;
        jlong m_timeout;
        const char *m_transport;
        const char *m_address;
        const char *m_log;
        const char *m_kindFilter;
        const char *m_srcFilter;
        const char *m_launch;
        const char *m_onthrow;
    };

} // namespace jdwp

#endif // _OPTION_PARSER_H_
