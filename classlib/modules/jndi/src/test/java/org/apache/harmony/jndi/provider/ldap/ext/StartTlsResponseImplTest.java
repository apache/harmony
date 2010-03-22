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

package org.apache.harmony.jndi.provider.ldap.ext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import junit.framework.TestCase;

public class StartTlsResponseImplTest extends TestCase {

    private final String HOST = "127.0.0.1";

    private final int PORT = 12345;

    public StartTlsResponseImplTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * After negotiation, a SSlSocket will be created and attach to the
     * underlying Socket which is used before negotiation. This test case
     * compare the local port, peer's host, peer' port before and after the
     * negotiation. Test case fail If they are different.
     */
    public void test_negotiate() throws IOException {

        // This serverSocket is never used, just for creating a client socket.
        ServerSocket server = new ServerSocket(PORT);

        Socket socket = new Socket(HOST, PORT);
        MockSSLSocketFactory factory = new MockSSLSocketFactory();

        StartTlsResponseImpl tls = new StartTlsResponseImpl();

        tls.setSocket(socket);

        tls.setHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        tls.negotiate(factory);

        //Fail if SSLSocket is not created.
        assertTrue(tls.getSSLSocket() instanceof SSLSocket);

        assertEquals(socket.getInetAddress().getHostName(), tls.getSSLSocket()
                .getInetAddress().getHostName());
        assertEquals(socket.getPort(), tls.getSSLSocket().getPort());
        assertEquals(socket.getLocalPort(), tls.getSSLSocket().getLocalPort());

        socket.close();
    }

    /*
     * This MockSSLSocketFactory is used for create and SSLSocket above an
     * underlying simple socket. It won't do any connection.
     */
    public static class MockSSLSocketFactory extends SSLSocketFactory {

        /*
         * Create a MockSSLSocket, and attach it to an underlying simple socket.
         * Won't do any connection.
         */
        @Override
        public Socket createSocket(Socket s, String host, int port,
                boolean autoClose) throws IOException {
            SSLSocket sslSocket = new MockSSLSocket(s, host, port);
            return sslSocket;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            throw new Error("should not be here");
        }

        @Override
        public String[] getSupportedCipherSuites() {
            throw new Error("should not be here");
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException,
                UnknownHostException {
            throw new Error("should not be here");
        }

        @Override
        public Socket createSocket(String host, int port,
                InetAddress localHost, int localPort) throws IOException,
                UnknownHostException {
            throw new Error("should not be here");
        }

        @Override
        public Socket createSocket(InetAddress host, int port)
                throws IOException {
            throw new Error("should not be here");
        }

        @Override
        public Socket createSocket(InetAddress address, int port,
                InetAddress localAddress, int localPort) throws IOException {
            throw new Error("should not be here");
        }

    }

    /*
     * This MocketSSLSocket won't do any connection, nor any communication with
     * a peer. It only stores peer host name, peer port, local host name and
     * local port.
     */
    public static class MockSSLSocket extends SSLSocket {

        private int port; // peer's port

        private String localHost;

        private int localPort;

        private String[] suites;

        private List<HandshakeCompletedListener> listeners;

        private InetAddress address; // peer's address

        protected MockSSLSocket(Socket socket, String host, int port)
                throws UnknownHostException {
            this.port = port;
            this.localHost = socket.getLocalAddress().getHostAddress();
            this.localPort = socket.getLocalPort();
            address = InetAddress.getByName(host);
            listeners = new LinkedList<HandshakeCompletedListener>();
        }

        /*
         * Won't do any handshake with the peer, just notify registed listeners.
         */
        @Override
        public void startHandshake() throws IOException {
            for (HandshakeCompletedListener listener : listeners) {
                if (this.listeners != null) {
                    listener.handshakeCompleted(null);
                } else {
                    throw new Error(
                            "should not be here, at MockHandshake.startHandshake()");
                }
            }
        }

        @Override
        public void addHandshakeCompletedListener(
                HandshakeCompletedListener listener) {
            listeners.add(listener);
        }

        @Override
        public SSLSession getSession() {
            return null;
        }

        @Override
        public void setEnabledCipherSuites(String[] suites) {
            this.suites = suites;
        }

        @Override
        public String[] getEnabledCipherSuites() {
            return this.suites;
        }

        public InetAddress getInetAddress() {
            return this.address;
        }

        public int getPort() {
            return this.port;
        }

        public String getLocalHost() {
            return this.localHost;
        }

        public int getLocalPort() {
            return this.localPort;
        }

        @Override
        public boolean getEnableSessionCreation() {
            throw new Error("should not be here");
        }

        @Override
        public String[] getEnabledProtocols() {
            throw new Error("should not be here");
        }

        @Override
        public boolean getNeedClientAuth() {
            throw new Error("should not be here");
        }

        @Override
        public String[] getSupportedCipherSuites() {
            throw new Error("should not be here");
        }

        @Override
        public String[] getSupportedProtocols() {
            throw new Error("should not be here");
        }

        @Override
        public boolean getUseClientMode() {
            throw new Error("should not be here");
        }

        @Override
        public boolean getWantClientAuth() {
            throw new Error("should not be here");
        }

        @Override
        public void removeHandshakeCompletedListener(
                HandshakeCompletedListener listener) {
            throw new Error("should not be here");
        }

        @Override
        public void setEnableSessionCreation(boolean flag) {
            throw new Error("should not be here");
        }

        @Override
        public void setEnabledProtocols(String[] protocols) {
            throw new Error("should not be here");
        }

        @Override
        public void setNeedClientAuth(boolean need) {
            throw new Error("should not be here");
        }

        @Override
        public void setUseClientMode(boolean mode) {
            throw new Error("should not be here");
        }

        @Override
        public void setWantClientAuth(boolean want) {
            throw new Error("should not be here");
        }

        @Override
        public SSLParameters getSSLParameters() {
            throw new Error("should not be here");
        }

        @Override
        public void setSSLParameters(SSLParameters params) {
            throw new Error("should not be here");
        }
    }

}
