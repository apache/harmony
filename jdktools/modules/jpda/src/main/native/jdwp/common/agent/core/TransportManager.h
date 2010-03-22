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
 * TransportManager.h
 *
 */

#ifndef _TRANSPORT_MANAGER_H_
#define _TRANSPORT_MANAGER_H_


#include "TransportManager_pd.h"
#include "jdwpTransport.h"
#include "AgentBase.h"
#include "AgentException.h"
#include "Log.h"
#include "AgentMonitor.h"
#include "vmi.h"
#include "hythread.h"
#include "hyport.h"

namespace jdwp {

typedef UDATA LoadedLibraryHandler;

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
        int Init(const char* transportName, 
                const char* libPath);

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
        int PrepareConnection(const char* address, bool isServer, 
                jlong connectTimeout, jlong handshakeTimeout);
        
        /**
         * Establish connection with the debugger.
         * Must be invoked after Init().
         *
         * @exception TransportException() - transport error happens.
         */
        int Connect();
        
        /**
         * Launch the debugger and establish connection.
         *
         * @param command - a command line to start the debugger
         *
         * @exception AgentException() - any error happens.
         */
        int Launch(const char* command);
        
        /**
         * The given function does a blocking read on an open connection.
         *
         * @param packet - the <code>jdwpPacket</code> structure address 
         *                 populated by this function
         *
         * @exception TransportException() - transport error happens.
         */
        int Read(jdwpPacket* packet);
        
        /**
         * Writes a JDWP packet to an open connection.
         *
         * @param packet - the <code>jdwpPacket</code> structure address
         * 
         * @exception TransportException() - transport error 
         *            happens.
         */
        int Write(const jdwpPacket *packet);
        
        /**
         * Close an open connection. The connection may be established again.
         * 
         * @exception TransportException() - transport error happens.
         */
        int Reset();

        void Clean();
        
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
        char* GetLastTransportError();

    protected:

    private:
        jlong m_connectTimeout;                  // attachTimeout or acceptTimeout timeout
        jlong m_handshakeTimeout;                // handshakeTimeout
        bool m_ConnectionPrepared;               // if true PrepareConnection done
        bool m_isConnected;                      // true if connection is established
        bool m_isServer;                         // is jdwp agent server or not
        bool m_isCleaned;                        // is clean method is invoked
        const char* m_transportName;             // transport name
        char* m_address;                         // transport address
        jdwpTransportEnv* m_env;                 // jdwpTransport environment
        LoadedLibraryHandler m_loadedLib;        // transport library handler
        char* m_lastErrorMessage;                // last error message
#if defined(WIN32) || defined(WIN64)
	AgentMonitor* m_sendMonitor;
#endif
        int CheckReturnStatus(jdwpTransportError err);
        int StartDebugger(const char* command, int extra_argc, const char* extra_argv[]);
        void TracePacket(const char* message, const jdwpPacket* packet);
        LoadedLibraryHandler LoadTransport(const char* dirName, const char* transportName);

        static const char* onLoadDecFuncName;
        static const char* unLoadDecFuncName;
        static const char pathSeparator;

    };//class TransportManager

}//jdwp

#endif // _TRANSPORT_MANAGER_H_
