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

package org.apache.harmony.xnet.provider.jsse;

import org.apache.harmony.xnet.provider.jsse.SSLSocketImpl;
import org.apache.harmony.xnet.provider.jsse.SSLParameters;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;

/**
 * SSLServerSocket implementation
 * @see javax.net.ssl.SSLServerSocket class documentation for more information.
 */
public class SSLServerSocketImpl extends SSLServerSocket {

    private final SSLSocketImpl sslSocket;

    // logger
    private Logger.Stream logger = Logger.getStream("ssocket");

    /**
     * Ctor
     * @param   sslParameters:  SSLParameters
     * @throws  IOException
     */
    protected SSLServerSocketImpl(SSLParameters sslParameters)
        throws IOException {
        super();
        sslSocket = new SSLSocketImpl(sslParameters);
    }

    /**
     * Ctor
     * @param   port:   int
     * @param   sslParameters:  SSLParameters
     * @throws  IOException
     */
    protected SSLServerSocketImpl(int port, SSLParameters sslParameters)
        throws IOException {
        super(port);
        sslSocket = new SSLSocketImpl(sslParameters);
    }

    /**
     * Ctor
     * @param   port:   int
     * @param   backlog:    int
     * @param   sslParameters:  SSLParameters
     * @throws  IOException
     */
    protected SSLServerSocketImpl(int port, int backlog,
            SSLParameters sslParameters) throws IOException {
        super(port, backlog);
        sslSocket = new SSLSocketImpl(sslParameters);
    }

    /**
     * Ctor
     * @param   port:   int
     * @param   backlog:    int
     * @param   iAddress:   InetAddress
     * @param   sslParameters:  SSLParameters
     * @throws  IOException
     */
    protected SSLServerSocketImpl(int port, int backlog,
                                InetAddress iAddress,
                                SSLParameters sslParameters)
        throws IOException {
        super(port, backlog, iAddress);
        sslSocket = new SSLSocketImpl(sslParameters);
    }

    // --------------- SSLParameters based methods ---------------------

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#getSupportedCipherSuites()
     * method documentation for more information
     */
    @Override
    public String[] getSupportedCipherSuites() {
        return sslSocket.getSupportedCipherSuites();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#getEnabledCipherSuites()
     * method documentation for more information
     */
    @Override
    public String[] getEnabledCipherSuites() {
        return sslSocket.getEnabledCipherSuites();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#setEnabledCipherSuites(String[])
     * method documentation for more information
     */
    @Override
    public void setEnabledCipherSuites(String[] suites) {
        sslSocket.setEnabledCipherSuites(suites);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#getSupportedProtocols()
     * method documentation for more information
     */
    @Override
    public String[] getSupportedProtocols() {
        return sslSocket.getSupportedProtocols();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#getEnabledProtocols()
     * method documentation for more information
     */
    @Override
    public String[] getEnabledProtocols() {
        return sslSocket.getEnabledProtocols();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#setEnabledProtocols(String[])
     * method documentation for more information
     */
    @Override
    public void setEnabledProtocols(String[] protocols) {
        sslSocket.setEnabledProtocols(protocols);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#setUseClientMode(boolean)
     * method documentation for more information
     */
    @Override
    public void setUseClientMode(boolean mode) {
        sslSocket.setUseClientMode(mode);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#getUseClientMode()
     * method documentation for more information
     */
    @Override
    public boolean getUseClientMode() {
        return sslSocket.getUseClientMode();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#setNeedClientAuth(boolean)
     * method documentation for more information
     */
    @Override
    public void setNeedClientAuth(boolean need) {
        sslSocket.setNeedClientAuth(need);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#getNeedClientAuth()
     * method documentation for more information
     */
    @Override
    public boolean getNeedClientAuth() {
        return sslSocket.getNeedClientAuth();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#setWantClientAuth(boolean)
     * method documentation for more information
     */
    @Override
    public void setWantClientAuth(boolean want) {
        sslSocket.setWantClientAuth(want);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#getWantClientAuth()
     * method documentation for more information
     */
    @Override
    public boolean getWantClientAuth() {
        return sslSocket.getWantClientAuth();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#setEnableSessionCreation(boolean)
     * method documentation for more information
     */
    @Override
    public void setEnableSessionCreation(boolean flag) {
        sslSocket.setEnableSessionCreation(flag);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLServerSocket#getEnableSessionCreation()
     * method documentation for more information
     */
    @Override
    public boolean getEnableSessionCreation() {
        return sslSocket.getEnableSessionCreation();
    }


    // ------------- ServerSocket's methods overridings ----------------

    /**
     * This method works according to the specification of implemented class.
     * @see java.net.ServerSocket#accept()
     * method documentation for more information
     */
    @Override
    public Socket accept() throws IOException {
        if (logger != null) {
            logger.println("SSLServerSocketImpl.accept ..");
        }
        implAccept(sslSocket);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkAccept(sslSocket.getInetAddress().getHostAddress(),
                        sslSocket.getPort());
            } catch(SecurityException e) {
                sslSocket.close();
                throw e;
            }
        }
        sslSocket.init();
        sslSocket.startHandshake();
        if (logger != null) {
            logger.println("SSLServerSocketImpl: accepted, initialized");
        }
        return sslSocket;
    }

    /**
     * Returns the string representation of the object.
     */
    @Override
    public String toString() {
        return "[SSLServerSocketImpl]";
    }

    // -----------------------------------------------------------------
}

