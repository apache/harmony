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
 * AgentException.h
 *
 */

#ifndef _AGENT_EXCEPTION_H_
#define _AGENT_EXCEPTION_H_

#include <exception>
#include <sstream>

#include "jdwp.h"
#include "jvmti.h"
#include "jdwpTransport.h"

namespace jdwp {

    using namespace std;

    /**
     * The base of all agent exceptions.
     */
    class AgentException : public exception {

    public:

        /**
         * A constructor.
         *
         * @param err - JDWP error code
         */
        AgentException(jdwpError err) throw() {
            m_error = err;
        }

        /**
         * A constructor.
         *
         * @param err - JVMTI error code
         */
        AgentException(jvmtiError err) throw() {
            m_error = (jdwpError)err;
        }

        /**
         * Returns the exception name.
         */
        virtual const char* what() const throw() { return "AgentException"; }

        /**
         * Returns JDWP error code.
         */
        jdwpError ErrCode() const throw() {
            return m_error;
        }

    private:
        jdwpError m_error;
    };

    /**
     * Out of memory exception thrown in case of JVMTI failures
     * of memory or reference allocation.
     */
    class OutOfMemoryException : public AgentException {

    public:

        /**
         * A constructor.
         */
        OutOfMemoryException() throw() :
            AgentException(JDWP_ERROR_OUT_OF_MEMORY) { }

        /**
         * Returns the given exception name.
         */
        const char* what() const throw() {
            return "JDWP_ERROR_OUT_OF_MEMORY";
        }
    };

    /**
     * Exception caused by invalid internal state, checking or parameter 
     * validation.
     */
    class InternalErrorException : public AgentException {

    public:

        /**
         * A constructor.
         */
        InternalErrorException() throw() :
            AgentException(JDWP_ERROR_INTERNAL) { }

        /**
         * Returns the given exception name.
         */
        const char* what() const throw() {
            return "JDWP_ERROR_INTERNAL";
        }
    };

    /**
     * Exception thrown by unrealized method or function.
     */
    class NotImplementedException : public AgentException {

    public:

        /**
         * A constructor.
         */
        NotImplementedException() throw() :
            AgentException(JDWP_ERROR_NOT_IMPLEMENTED) { }

        /**
         * Returns the given exception name.
         */
        const char* what() const throw() {
            return "JDWP_ERROR_NOT_IMPLEMENTED";
        }
    };

    /**
     * Exception caused by the parameter validation failure.
     */
    class IllegalArgumentException : public AgentException {

    public:

        /**
         * A constructor.
         */
        IllegalArgumentException() throw() :
            AgentException(JDWP_ERROR_ILLEGAL_ARGUMENT) { }

        /**
         * Returns the given exception name.
         */
        const char* what() const throw() {
            return "JDWP_ERROR_ILLEGAL_ARGUMENT";
        }
    };

    /**
     * Exception caused by invalid index value or out-of-range index.
     */
    class InvalidIndexException : public AgentException {

    public:

        /**
         * A constructor.
         */
        InvalidIndexException() throw() :
            AgentException(JDWP_ERROR_INVALID_INDEX) { }

        /**
         * Returns the given exception name.
         */
        const char* what() const throw() {
            return "JDWP_ERROR_INVALID_INDEX";
        }
    };

    /**
     * Exception caused by failures in agent-transport layer.
     */
    class TransportException : public AgentException {

    public:

        /**
         * A constructor.
         *
         * @param err            - JDWP error code
         * @param transportError - transport error code
         */
        TransportException(jdwpError err = JDWP_ERROR_TRANSPORT_INIT, 
                jdwpTransportError transportError = JDWPTRANSPORT_ERROR_NONE,
                const char* message = 0) 
                throw() : AgentException(err) 
        {
            this->m_transportError = transportError;
            this->m_message = message;
        }

        /**
         * Returns transport error code.
         */
        jdwpTransportError TransportErrorCode() const throw() {
            return m_transportError;
        }

        /**
         * Returns transport error message.
         */
        const char* TransportErrorMessage() const throw() {
            return m_message;
        }

        /**
         * Returns the given exception info.
         */
        const char* what() const throw() {
            return (m_message == 0 ? "TransportException" : m_message);
        }

    private:
        jdwpTransportError m_transportError;
        const char* m_message;
    };

};

#endif // _AGENT_EXCEPTION_H_
