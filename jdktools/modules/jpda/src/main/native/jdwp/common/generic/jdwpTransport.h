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

/**
 * @file
 * jdwpTransport.h
 *
 */

#ifndef _JDWPTRANSPORT_H_
#define _JDWPTRANSPORT_H_

#include "jni.h"

#ifdef __cplusplus 
extern "C" { 
#endif 

struct jdwpTransportNativeInterface_; 

struct _jdwpTransportEnv; 

#ifdef __cplusplus 
typedef _jdwpTransportEnv jdwpTransportEnv; 
#else 
typedef const struct jdwpTransportNativeInterface_ *jdwpTransportEnv; 
#endif 

/**
 * JDWP transport version number.
 */
enum {
    JDWPTRANSPORT_VERSION_1_0               = 0x00010000 
};

/**
 * The list of JDWP transport error codes.
 */
typedef enum {
    JDWPTRANSPORT_ERROR_MSG_NOT_AVAILABLE   = 204,
    JDWPTRANSPORT_ERROR_TIMEOUT             = 203,
    JDWPTRANSPORT_ERROR_IO_ERROR            = 202,
    JDWPTRANSPORT_ERROR_ILLEGAL_STATE       = 201,
    JDWPTRANSPORT_ERROR_INTERNAL            = 113,
    JDWPTRANSPORT_ERROR_OUT_OF_MEMORY       = 110,
    JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT    = 103,
    JDWPTRANSPORT_ERROR_NONE                = 0
} jdwpTransportError;
    
/**
 * The structure contains JDWP transport capabilities.
 * One bit per capability.
 */
typedef struct {
    unsigned int can_timeout_attach         :1;
    unsigned int can_timeout_accept         :1;
    unsigned int can_timeout_handshake      :1;
    unsigned int reserved3                  :1;
    unsigned int reserved4                  :1;
    unsigned int reserved5                  :1;
    unsigned int reserved6                  :1;
    unsigned int reserved7                  :1;
    unsigned int reserved8                  :1;
    unsigned int reserved9                  :1;
    unsigned int reserved10                 :1;
    unsigned int reserved11                 :1;
    unsigned int reserved12                 :1;
    unsigned int reserved13                 :1;
    unsigned int reserved14                 :1;
    unsigned int reserved15                 :1;
} JDWPTransportCapabilities;

/**
 * JDWP transport flags.
 */
enum {
    JDWPTRANSPORT_FLAGS_REPLY               = 0x80,
    JDWPTRANSPORT_FLAGS_NONE                = 0x0
};

/**
 * The JDWP transport callback function type.
 * See the JVM JPDA/JDWP specification for details.
 */
typedef struct jdwpTransportCallback {
    void *(*alloc)(jint numBytes);
    void (*free)(void *buffer);
} jdwpTransportCallback;

/**
 * The structure for the JDWP reply packet.
 * See the JVM JPDA/JDWP specification for details.
 */
typedef struct {

    /**
     * The entire packet size in bytes.
     */
    jint len;

    /**
     * The packet identification number.
     */
    jint id;

    /**
     * The packet type flag = JDWPTRANSPORT_FLAGS_REPLY.
     */
    jbyte flags;

    /**
     * The error code for the command packet that was replied to.
     */
    jshort errorCode;

    /**
     * Specific packet data.
     */
    jbyte *data;

} jdwpReplyPacket;

/**
 * The structure for the JDWP command packet.
 * See the JVM JPDA/JDWP specification for details.
 */
typedef struct {

    /**
     * The entire packet size in bytes.
     */
    jint len;

    /**
     * The packet identification number.
     */
    jint id;

    /**
     * The packet type flag = JDWPTRANSPORT_FLAGS_NONE.
     */
    jbyte flags;

    /**
     * The command set number.
     */
    jbyte cmdSet;

    /**
     * The command number.
     */
    jbyte cmd;

    /**
     * The specific packet data.
     */
    jbyte *data;

} jdwpCmdPacket;

/**
 * The unified structure for the JDWP command or reply packet.
 */
typedef struct {
    union {

        /**
         * The command packet.
         */
        jdwpCmdPacket cmd;

        /**
         * The reply packet.
         */
        jdwpReplyPacket reply;

    } type;
} jdwpPacket; 

typedef jint (JNICALL *jdwpTransport_OnLoad_t)(JavaVM *jvm,
            jdwpTransportCallback *callback,
            jint version, jdwpTransportEnv** env);

/**
 * The structure representing transport-native interface with capabilities
 * to establish a connection with the debugger as well as to communicate with it.
 */
struct jdwpTransportNativeInterface_ {

    void *reserved1;

    /**
     * Gets capabilities of the used transport component.
     *
     * @param[in]  env              - the transport environment pointer
     * @param[out] capabilitiesPtr  - the pointer to capabilities descriptor
     *
     * @return The transport-error code.
     */
    jdwpTransportError (JNICALL *GetCapabilities)(jdwpTransportEnv* env,
            JDWPTransportCapabilities *capabilitiesPtr);

    /**
     * Connects to the debugger listening on the specified address
     * and establishes a communication channel after a successful handshake.
     *
     * @param[in] env              - the transport environment pointer
     * @param[out] address         - the address at which the debugger listens
     * @param[in] attachTimeout    - the time-out for connection to the debugger
     * @param[in] handshakeTimeout - the time-out for handshake with the debugger
     *
     * @return The transport-error code.
     */
    jdwpTransportError (JNICALL *Attach)(jdwpTransportEnv* env,
            const char* address, jlong attachTimeout, jlong handshakeTimeout);

    /**
     * Starts listening at the specified address.
     *
     * @param[in] env             - the ransport environment pointer
     * @param[in] address         - the address at which the listen starts
     * @param[out] actualAddress  - the real address at which the listen will 
     *                              start
     *
     * @return The transport-error code.
     */
    jdwpTransportError (JNICALL *StartListening)(jdwpTransportEnv* env,
            const char* address, char** actualAddress);

    /**
     * Stops listening operation.
     *
     * @param[in] env - the transport environment pointer
     *
     * @return The transport-error code.
     */
    jdwpTransportError (JNICALL *StopListening)(jdwpTransportEnv* env);

    /**
     * Accepts the debugger and establishes a communication channel after a 
     * successful handshake.
     *
     * @param[in] env              - the transport environment pointer
     * @param[in] attachTimeout    - the time-out for connection to the debugger
     * @param[in] handshakeTimeout - the time-out for handshake with the debugger
     *
     * @return The transport-error code.
     */
    jdwpTransportError (JNICALL *Accept)(jdwpTransportEnv* env,
            jlong acceptTimeout, jlong handshakeTimeout);

    /**
     * Checks whether the communication channel is opened.
     *
     * @param[in] env - the transport environment pointer
     *
     * @return Boolean.
     */
    jboolean (JNICALL *IsOpen)(jdwpTransportEnv* env);

    /**
     * Closes the opened communication channel.
     *
     * @param[in] env - the transport environment pointer
     *
     * @return The transport-error code.
     */
    jdwpTransportError (JNICALL *Close)(jdwpTransportEnv* env);

    /**
     * Reads a packet from the debugger.
     *
     * @param[in] env     - the transport environment pointer
     * @param[out] packet - the packet structure
     *
     * @return The transport-error code.
     */
    jdwpTransportError (JNICALL *ReadPacket)(jdwpTransportEnv* env,
            jdwpPacket *packet);

    /**
     * Sends the given packet to the debugger.
     *
     * @param[in] env    - the transport environment pointer
     * @param[in] packet - the packet structure
     *
     * @return The transport-error code.
     */
    jdwpTransportError (JNICALL *WritePacket)(jdwpTransportEnv* env,
            const jdwpPacket* packet);

    /**
     * Gets the last transport-error description.
     *
     * @param[in] env   - the transport environment pointer
     * @param[out] msg  - the pointer to the string buffer for an error message
     *
     * @return The transport-error code.
     */
    jdwpTransportError (JNICALL *GetLastError)(jdwpTransportEnv* env,
            char** msg);

};

/**
 * The structure contain pointer to the Transport native interface and
 * wraps its code into C++ style members.
 */
struct _jdwpTransportEnv {

    const struct jdwpTransportNativeInterface_ *functions;

#ifdef __cplusplus

    /**
     * Gets capabilities of the used transport component.
     *
     * @param[out] capabilitiesPtr  - the pointer to capabilities descriptor
     *
     * @return The transport-error code.
     */
    jdwpTransportError GetCapabilities(JDWPTransportCapabilities *capabilitiesPtr)
    {
        return functions->GetCapabilities(this, capabilitiesPtr);
    }

    /**
     * Connects to the debugger listening on the specified address
     * and establishes a communication channel after a successful handshake.
     *
     * @param[in] address           - the address at which the debugger listens
     * @param[in] attachTimeout     - the time-out for connection to the debugger
     * @param[in] handshakeTimeout  - the time-out for handshake with the debugger
     *
     * @return The transport-error code.
     */
    jdwpTransportError Attach(const char* address, jlong attachTimeout,
            jlong handshakeTimeout)
    {
        return functions->Attach(this, address, attachTimeout, handshakeTimeout);
    }

    /**
     * Starts listening at the specified address.
     *
     * @param[in] address        - the address at which our listen starts
     * @param[out] actualAddress - the real address at which the listen will 
     *                             start
     *
     * @return The transport-error code.
     */
    jdwpTransportError StartListening(const char* address,
            char** actualAddress)
    {
        return functions->StartListening(this, address, actualAddress);
    }

    /**
     * Stops listening operation.
     *
     * @return The transport-error code.
     */
    jdwpTransportError StopListening(void)
    {
        return functions->StopListening(this);
    }

    /**
     * Accepts the debugger and establishes a communication channel after a 
     * successful handshake.
     *
     * @param[in] acceptTimeout     - the time-out for accepting connection  
     *                                from the debugger
     * @param[in] handshakeTimeout  - the time-out for a handshake with the 
     *                                debugger
     *
     * @return The transport-error code.
     */
    jdwpTransportError Accept(jlong acceptTimeout, jlong handshakeTimeout)
    {
        return functions->Accept(this, acceptTimeout, handshakeTimeout);
    }

    /**
     * Checks whether the communication channel is opened.
     *
     * @return Boolean.
     */
    jboolean IsOpen(void)
    {
        return functions->IsOpen(this);
    }

    /**
     * Closes the opened communication channel.
     *
     * @return The transport-error code.
     */
    jdwpTransportError Close(void)
    {
        return functions->Close(this);
    }

    /**
     * Reads a packet from the debugger.
     *
     * @param[out] packet - the packet structure
     *
     * @return The transport-error code.
     */
    jdwpTransportError ReadPacket(jdwpPacket *packet) 
    {
        return functions->ReadPacket(this, packet);
    }

    /**
     * Sends the given packet to the debugger.
     *
     * @param[in] packet - the packet structure
     *
     * @return The transport-error code.
     */
    jdwpTransportError WritePacket(const jdwpPacket* packet) 
    {
        return functions->WritePacket(this, packet);
    }

    /**
     * Gets the last transport error description.
     *
     * @param[out] msg - the pointer to the string buffer for an error message
     *
     * @return The transport-error code.
     */
    jdwpTransportError GetLastError(char** msg) 
    {
        return functions->GetLastError(this, msg);
    }

#endif
};

#ifdef __cplusplus
}
#endif

#endif
