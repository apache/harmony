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
import java.net.Socket;

import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class StartTlsResponseImpl extends StartTlsResponse {

    /**
     * 
     */
    private static final long serialVersionUID = -1199041712215453042L;

    private String[] suites;

    private HostnameVerifier verifier;

    private Socket socket; // existing uderlying socket

    private boolean isHandshaked = false; // is handshake finished

    private SSLSocket negotiatedSslSocket = null; // negotiated ssl socket

    @Override
    public void close() throws IOException {
        /*
         * TODO Spec requires close the TLS connection gracefully and reverts
         * back to the underlying connection. RI close the ssl socket but
         * dosen't reverts to former socket when invoke this method. We follow
         * RI here.
         */
        if (negotiatedSslSocket != null) {
            negotiatedSslSocket.close();
        }
    }

    @Override
    public SSLSession negotiate() throws IOException {
        return negotiate(null);
    }

    @Override
    public SSLSession negotiate(SSLSocketFactory factory) throws IOException {

        if (socket == null) {
            // must set socket before negotiate
            return null;
        }

        if (factory == null) {
            factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }

        SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, socket
                .getInetAddress().getHostName(), socket.getPort(), false);

        if (suites != null) {
            sslSocket.setEnabledCipherSuites(suites);
        }

        sslSocket
                .addHandshakeCompletedListener(new HandshakeCompletedListener() {
                    public void handshakeCompleted(HandshakeCompletedEvent event) {
                        isHandshaked = true;
                    }
                });

        sslSocket.startHandshake();

        while (!isHandshaked) {
            // Wait for handshake finish.
        }

        HostnameVerifier defaultVerifier = new HostnameVerifier() {

            public boolean verify(String hostname, SSLSession session) {
                // TODO: add verify logic here
                return false;
            }

        };

        /*
         * TODO: the spec requires to verify server's hostname by default
         * hostname verifier first, if failed, use the callback verifier. RI use
         * callback verifier first then the dafault one. We follow the spec
         * here.
         */
        if (defaultVerifier.verify(sslSocket.getInetAddress().getHostName(),
                sslSocket.getSession())) {
            this.negotiatedSslSocket = sslSocket;
            return sslSocket.getSession();
        } else if (verifier != null
                && verifier.verify(sslSocket.getInetAddress().getHostName(),
                        sslSocket.getSession())) {
            this.negotiatedSslSocket = sslSocket;
            return sslSocket.getSession();
        }

        // negotiation fails
        /* 
         * TODO: What to do if hostname verification failed? RI throws exception
         * of hostname verification.
         */
        return null;
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        this.suites = suites;
    }

    @Override
    public void setHostnameVerifier(HostnameVerifier verifier) {
        this.verifier = verifier;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public SSLSocket getSSLSocket() {
        return this.negotiatedSslSocket;
    }
}

