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
 * AgentException.h
 *
 */

#ifndef _AGENT_EXCEPTION_H_
#define _AGENT_EXCEPTION_H_

#include <string.h>
#include <stdlib.h>
#include "jdwp.h"
#include "jdwpTransport.h"
#include "jvmti.h"
#include "vmi.h"

namespace jdwp {

    typedef enum exceptions {
        ENUM_AgentException = 0,
		ENUM_OutOfMemoryException = 1,
		ENUM_InternalErrorException = 2,
		ENUM_NotImplementedException = 3,
		ENUM_IllegalArgumentException = 4,
		ENUM_InvalidStackFrameException = 5,
		ENUM_InvalidIndexException = 6,
		ENUM_TransportException = 7
    } exceptions;

    /**
     * The base of all agent exceptions.
     */
    class AgentException {

    public:

        AgentException() { }

        /**
         * A constructor.
         *
         * @param err - JDWP error code
         */
        AgentException(jdwpError err, const char *message = NULL)  {
            m_error = err;
            m_transportError = JDWPTRANSPORT_ERROR_NONE;
            m_message = message;
            m_mustFree = false;

            switch(m_error) {
            case JDWP_ERROR_OUT_OF_MEMORY:
                m_exception = ENUM_OutOfMemoryException;
                break;
            case JDWP_ERROR_INTERNAL:
                m_exception = ENUM_InternalErrorException;
                break;
            case JDWP_ERROR_NOT_IMPLEMENTED:
                m_exception = ENUM_NotImplementedException;
                break;
            case JDWP_ERROR_ILLEGAL_ARGUMENT:
                m_exception = ENUM_IllegalArgumentException;
                break;
            case JDWP_ERROR_OPAQUE_FRAME:
                m_exception = ENUM_InvalidStackFrameException;
                break;
            case JDWP_ERROR_INVALID_INDEX:
                m_exception = ENUM_InvalidIndexException;
                break;
            default:
                m_exception = ENUM_AgentException;
            }
        }

        /**
         * A constructor.
         *
         * @param err - JVMTI error code
         */
        AgentException(jvmtiError err, const char *message = NULL)  {
            m_error = (jdwpError)err;
            m_transportError = JDWPTRANSPORT_ERROR_NONE;
            m_exception = ENUM_AgentException;
            m_message = message;
            m_mustFree = false;
        }

        AgentException(jdwpTransportError err, const char *message = NULL)  {
            m_transportError = err;
            m_error = JDWP_ERROR_NONE;
            m_exception = ENUM_TransportException;
            m_message = message;
            m_mustFree = false;
        }

        AgentException(jdwpError err, jdwpTransportError terr, const char *message = NULL)  {
            m_error = err;
            m_transportError = terr;            
            m_exception = ENUM_TransportException;
            m_message = message;
            m_mustFree = false;
        }

        /**
         * A copy constructor.
         */
        AgentException(const AgentException& ex) {
            if (&ex == NULL) {
                m_mustFree = false;
                return;
            }

            m_error = ex.m_error;
            m_transportError = ex.m_transportError;
            m_exception = ex.m_exception;

            if (ex.m_message == NULL) {
                m_message = NULL;
                m_mustFree = false;
            } else {
                char *temp = (char *)malloc(strlen(ex.m_message) + 1);
                strcpy(temp, ex.m_message);
                m_message = (const char*)temp;
                m_mustFree = true;
            }
        }

        /**
         * A destructor.
         */
        virtual ~AgentException() {
            if (m_mustFree) {
                free((void*)m_message);
            }
        }

        /**
         * Returns if the exception type is expected type.
         */
        bool Compare(exceptions ex) {
            return (ex == ENUM_AgentException)?true:(m_exception == ex);
        }

        /**
         * Returns the exception name.
         */
        virtual const char* what() const { 
            switch(m_exception) {
            case ENUM_OutOfMemoryException:
                return "OutOfMemoryException";
            case ENUM_InternalErrorException:
                return "InternalException";
            case ENUM_NotImplementedException:
                return "NotImplementedException";
            case ENUM_IllegalArgumentException:
                return "IllegalArgumentException";
            case ENUM_InvalidStackFrameException:
                return "InvalidStackFrameException";
            case ENUM_InvalidIndexException:
                return "InvalidIndexException";
            case ENUM_TransportException:
                return "TransportException";
            default:
                return "AgentException";
            }
        }

        /**
         * Returns JDWP error code.
         */
        jdwpError ErrCode() const  {
            return m_error;
        }

        /**
         * Returns JDWP transport error code.
         */
        jdwpTransportError TransportErrCode() const  {
            return m_transportError;
        }

        /**
        * Return exceptions
        */
        exceptions ExceptionType() const {
            return m_exception;
        }

        /**
         * Returns error message.
         */
        const char* ErrorMessage() const  {
            return m_message;
        }

        /**
         * Returns a formatted error message for this Exception
         */
        const char* GetExceptionMessage(JNIEnv* jni) {
            PORT_ACCESS_FROM_ENV(jni);

            // Create a full error message from this exception
            int allocateSize = strlen(what()) + 16; // Gives enough space for the type and error codes to be displayed
            const char *exMsg;
            if (m_message != NULL)  {
                allocateSize += strlen(m_message);
                exMsg = m_message;
            } else {
                exMsg = "";
            }
            char *message = (char*)hymem_allocate_memory(allocateSize);
            if (m_exception == ENUM_TransportException) {
                // If this is a TransportException, give transport error code
                hystr_printf(privatePortLibrary, message, (U_32)allocateSize, "%s [%d/%d] %s", what(), m_error, m_transportError, exMsg);
            } else {
                hystr_printf(privatePortLibrary, message, (U_32)allocateSize, "%s [%d] %s", what(), m_error, exMsg);
            }

            return message;
        }

        inline void* operator new(size_t size) {
            return malloc(size);
        }

        inline void operator delete(void* ptr) {
            free(ptr);
        }

    private:
        exceptions m_exception;
        jdwpError m_error;
        jdwpTransportError m_transportError;
        const char *m_message;
        bool m_mustFree;
    };
};

#endif // _AGENT_EXCEPTION_H_

