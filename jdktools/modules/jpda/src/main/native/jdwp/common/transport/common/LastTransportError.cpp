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
 * @author Viacheslav G. Rybalov
 */
#include "LastTransportError.h"

void (*LastTransportError::m_free)(void *buffer) = 0;

LastTransportError::LastTransportError(const char* messagePtr, int errorStatus, 
        void* (*alloc)(jint numBytes), void (*free)(void *buffer))
{
    m_treadId = GetCurrentThreadId();
    m_lastErrorMessage = messagePtr;
    m_lastErrorMessagePrefix = "";
    m_lastErrorStatus = errorStatus;
    m_alloc = alloc;
    m_free = free;
    m_next = 0;
}

LastTransportError::~LastTransportError(void)
{
    if (m_next != 0) {
        delete m_next;
    }
}

void* 
LastTransportError::operator new(size_t bytes, void* (*alloc)(jint numBytes), void (*free)(void *buffer))
{
    return (*alloc)((jint)bytes);
}

void 
LastTransportError::operator delete(void* address)
{
    (*m_free)(address);
}

void 
LastTransportError::operator delete(void* address, void* (*alloc)(jint numBytes), void (*free)(void *buffer))
{
    (*free)(address);
}


jdwpTransportError
LastTransportError::insertError(const char* messagePtr, int errorStatus)
{
    if (ThreadId_equal(m_treadId, GetCurrentThreadId())) {
        m_lastErrorMessage = messagePtr;
        m_lastErrorStatus = errorStatus;
        m_lastErrorMessagePrefix = "";
    } else if (m_next != 0) {
        return m_next->insertError(messagePtr, errorStatus);
    } else {
        m_next = new(m_alloc, m_free) LastTransportError(messagePtr, errorStatus, m_alloc, m_free);
        if (m_next == 0) {
            return JDWPTRANSPORT_ERROR_OUT_OF_MEMORY;
        }
    }
    return JDWPTRANSPORT_ERROR_NONE;
}

jdwpTransportError 
LastTransportError::addErrorMessagePrefix(const char* prefixPtr)
{
    if (ThreadId_equal(m_treadId, GetCurrentThreadId())) {
        m_lastErrorMessagePrefix = (prefixPtr == 0 ? "" : prefixPtr);
    } else if (m_next != 0) {
        return m_next->addErrorMessagePrefix(prefixPtr);
    }
    return JDWPTRANSPORT_ERROR_NONE;
}

int 
LastTransportError::GetLastErrorStatus()
{
    if (ThreadId_equal(m_treadId, GetCurrentThreadId())) {
        return m_lastErrorStatus;
    } else if (m_next != 0) {
        return m_next->GetLastErrorStatus();
    }
    return 0;
}

char* 
LastTransportError::GetLastErrorMessage() 
{
    if (ThreadId_equal(m_treadId, GetCurrentThreadId())) {
        char buf[32];
        sprintf(buf, "%d", m_lastErrorStatus);

        size_t strLength = (m_lastErrorStatus == 0) ? 
            strlen(m_lastErrorMessagePrefix) + strlen(m_lastErrorMessage) + 1 :
            strlen(m_lastErrorMessagePrefix) + strlen(m_lastErrorMessage) + strlen(" (error code: )") + strlen(buf) + 1;

        char* message = (char*)(*m_alloc)((jint)strLength);
        if (message == 0) {
            return 0;
        }

        if (m_lastErrorStatus == 0) {
            sprintf(message, "%s%s", m_lastErrorMessagePrefix, m_lastErrorMessage);
        } else {
            sprintf(message, "%s%s (error code: %s)", m_lastErrorMessagePrefix, m_lastErrorMessage, buf);
        }
        return message;

    } else if (m_next != 0) {
        return m_next->GetLastErrorMessage();
    }
    return 0;
}
