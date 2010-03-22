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
 * TransportManager.h
 *
 */

#ifndef _TRANSPORT_MANAGER_H_
#define _TRANSPORT_MANAGER_H_

#include "jdwpTransport.h"
#include "AgentBase.h"
#include "AgentException.h"
#include "Log.h"
#include "TransportManager_pd.h"

namespace jdwp {

    /**
     * The given class provides a high level interface with the JDWP transport.
     * At first the function Init() must be invoked. It loads and
     * initializes specified transport. Further the function 
     * PrepareConnection() must be invoked. It makes some 
     * preparations for establishing connection. To connect with the debugger, 
     * function Connect() must be invoked.
     */
    class TransportManager : public AgentBase {
    public:

        /**
         * A constructor.
         */
        TransportManager();

        /**
         * A destructor.
         */
        ~TransportManager();

        /**
         * Transport library initialization.
         *
         * @param transportName  - the name of the transport (library)
         * @param libPath        - the library path
         *
         * @exception TransportException() - transport initialization
         *            error happens.
         */
        void Init(const char* transportName, 
                const char* libPath) throw(TransportException);

        /**
         * Connection preparation. If the agent is server, then starts listening.
         * Must be invoked first.
         *
         * @param address           - the string representing the address of 
         *                            the debugger ("host:port")
         * @param isServer          - if the agent is a server then <code>TRUE</code>, 
         *                            otherwise <code>FALSE</code>
         * @param connectTimeout    - attach either accept time-out
         * @param handshakeTimeout  - the handshake time-out
         *
         * @exception TransportException() - transport error happens.
         */
        void PrepareConnection(const char* address, bool isServer, 
                jlong connectTimeout, jlong handshakeTimeout) throw(TransportException);
        
        /**
         * Establish connection with the debugger.
         * Must be invoked after Init().
         *
         * @exception TransportException() - transport error happens.
         */
        void Connect() throw(TransportException);
        
        /**
         * Launch the debugger and establish connection.
         *
         * @param command - a command line to start the debugger
         *
         * @exception AgentException() - any error happens.
         */
        void Launch(const char* command) throw(AgentException);
        
        /**
         * The given function does a blocking read on an open connection.
         *
         * @param packet - the <code>jdwpPacket</code> structure address 
         *                 populated by this function
         *
         * @exception TransportException() - transport error happens.
         */
        void Read(jdwpPacket* packet) throw(TransportException);
        
        /**
         * Writes a JDWP packet to an open connection.
         *
         * @param packet - the <code>jdwpPacket</code> structure address
         * 
         * @exception TransportException() - transport error 
         *            happens.
         */
        void Write(const jdwpPacket *packet) throw(TransportException);
        
        /**
         * Close an open connection. The connection may be established again.
         * 
         * @exception TransportException() - transport error happens.
         */
        void Reset() throw(TransportException);

        void Clean() throw(TransportException);
        
        /**
         * Check the connection.
         * @return Returns <code>TRUE</code> if connection is open, otherwise <code>FALSE</code>.
         */
        bool IsOpen();

        /**
         * Returns the string representation of the last transport error.
         * The caller is responsible to free the returned string.
         *
         * @exception TransportException() - transport error happens.
         */
        char* GetLastTransportError() throw(TransportException);

    protected:

    private:
        jlong m_connectTimeout;                  // attachTimeout or acceptTimeout timeout
        jlong m_handshakeTimeout;                // handshakeTimeout
        bool m_ConnectionPrepared;               // if true PrepareConnection done
        bool m_isConnected;                      // true if connection is established
        bool m_isServer;                         // is jdwp agent server or not
        const char* m_transportName;             // transport name
        char* m_address;                         // transport address
        jdwpTransportEnv* m_env;                 // jdwpTransport environment
        LoadedLibraryHandler m_loadedLib;        // transport library handler
        char* m_lastErrorMessage;                // last error message

        void CheckReturnStatus(jdwpTransportError err) throw(TransportException);
        void StartDebugger(const char* command, int extra_argc, const char* extra_argv[]) throw(AgentException);
        void TracePacket(const char* message, const jdwpPacket* packet);
        LoadedLibraryHandler LoadTransport(const char* dirName, const char* transportName);

        static const char* onLoadDecFuncName;
        static const char* unLoadDecFuncName;
        static const char pathSeparator;

    };//class TransportManager

}//jdwp

#endif // _TRANSPORT_MANAGER_H_
