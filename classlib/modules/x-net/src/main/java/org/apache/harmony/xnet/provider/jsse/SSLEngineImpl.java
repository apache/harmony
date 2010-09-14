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

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

/**
 * Implementation of SSLEngine.
 * @see javax.net.ssl.SSLEngine class documentation for more information.
 */
public class SSLEngineImpl extends SSLEngine {

    // indicates if peer mode was set
    private boolean peer_mode_was_set = false;
    // indicates if handshake has been started
    private boolean handshake_started = false;
    // indicates if inbound operations finished
    private boolean isInboundDone = false;
    // indicates if outbound operations finished
    private boolean isOutboundDone = false;
    // indicates if close_notify alert had been sent to another peer
    private boolean close_notify_was_sent = false;
    // indicates if close_notify alert had been received from another peer
    private boolean close_notify_was_received = false;
    // indicates if engine was closed (it means that
    // all the works on it are done, except (probably) some finalizing work)
    private boolean engine_was_closed = false;
    // indicates if engine was shutted down (it means that
    // all cleaning work had been done and the engine is not operable)
    private boolean engine_was_shutteddown = false;

    // active session object
    private SSLSessionImpl session;

    // peer configuration parameters
    protected SSLParameters sslParameters;

    // logger
    private Logger.Stream logger = Logger.getStream("engine");

    // Pointer to the SSL struct
    private long SSL;
    // Pointer to the custom struct used for this engine
    private long SSLEngineAddress;
    
    private SSLEngineResult.HandshakeStatus handshakeStatus;
    
    static {
        initImpl();
    }
    
    private static native void initImpl();
    private static native long initSSL(long context);
    private static native long initSSLEngine(long context);
    private static native SSLEngineResult.HandshakeStatus connectImpl(long sslEngineAddress);
    private static native SSLEngineResult.HandshakeStatus acceptImpl(long sslEngineAddress);
    private static native SSLEngineResult wrapImpl(long sslEngineAddress,
            byte[] src, int src_len, byte[] dst, int dst_len);
    private static native SSLEngineResult unwrapImpl(long sslEngineAddress,
            byte[] src, int src_len, byte[] dst, int dst_len);
    
    /**
     * Ctor
     * @param   sslParameters:  SSLParameters
     */
    protected SSLEngineImpl(SSLParameters sslParameters) {
        super();
        this.sslParameters = sslParameters;
        SSL = initSSL(sslParameters.getSSLContextAddress());
        SSLEngineAddress = initSSLEngine(SSL);
    }

    /**
     * Ctor
     * @param   host:   String
     * @param   port:   int
     * @param   sslParameters:  SSLParameters
     */
    protected SSLEngineImpl(String host, int port, SSLParameters sslParameters) {
        super(host, port);
        this.sslParameters = sslParameters;
        SSL = initSSL(sslParameters.getSSLContextAddress());
        SSLEngineAddress = initSSLEngine(SSL);
    }
    
    /**
     * Starts the handshake.
     * @throws  SSLException
     * @see javax.net.ssl.SSLEngine#beginHandshake() method documentation
     * for more information
     */
    @Override
    public void beginHandshake() throws SSLException {
        if (engine_was_closed) {
            throw new SSLException("Engine has already been closed.");
        }
        if (!peer_mode_was_set) {
            throw new IllegalStateException("Client/Server mode was not set");
        }
        // TODO: need to repeat connect/accept if status was waiting on wrap/unwrap previously?
        if (!handshake_started) {
            handshake_started = true;

            if (sslParameters.getUseClientMode()) {
                if (logger != null) {
                    logger.println("SSLEngineImpl: CLIENT connecting");
                }

                handshakeStatus = connectImpl(SSLEngineAddress);
            } else {
                if (logger != null) {
                    logger.println("SSLEngineImpl: SERVER accepting connection");
                }
                handshakeStatus = acceptImpl(SSLEngineAddress);
            }
        }
    }

    private static native void closeInboundImpl(long SSLEngineAddress);    

    /**
     * Closes inbound operations of this engine
     * @throws  SSLException
     * @see javax.net.ssl.SSLEngine#closeInbound() method documentation
     * for more information
     */
    @Override
    public void closeInbound() throws SSLException {
        if (logger != null) {
            logger.println("closeInbound() "+isInboundDone);
        }
        if (isInboundDone) {
            return;
        }
        isInboundDone = true;

        closeInboundImpl(SSLEngineAddress);

        engine_was_closed = true;
        if (!handshake_started) {
            // engine is closing before initial handshake has been made
            shutdown();
        }
    }

    private static native void closeOutboundImpl(long SSLEngineAddress);

    /**
     * Closes outbound operations of this engine
     * @see javax.net.ssl.SSLEngine#closeOutbound() method documentation
     * for more information
     */
    @Override
    public void closeOutbound() {
        if (logger != null) {
            logger.println("closeOutbound() "+isOutboundDone);
        }
        if (isOutboundDone) {
            return;
        }
        isOutboundDone = true;
        
        closeOutboundImpl(SSLEngineAddress);

        if (!handshake_started) {
            // engine is closing before initial handshake has been made
            shutdown();
        }
        engine_was_closed = true;
    }

    /**
     * Returns handshake's delegated tasks to be run
     * @return the delegated task to be executed.
     * @see javax.net.ssl.SSLEngine#getDelegatedTask() method documentation
     * for more information
     */
    @Override
    public Runnable getDelegatedTask() {
        return null;
        //return handshakeProtocol.getTask();
    }

    /**
     * Returns names of supported cipher suites.
     * @return array of strings containing the names of supported cipher suites
     * @see javax.net.ssl.SSLEngine#getSupportedCipherSuites() method
     * documentation for more information
     */
    @Override
    public String[] getSupportedCipherSuites() {
        return sslParameters.getSupportedCipherSuites();
    }

    // --------------- SSLParameters based methods ---------------------

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#getEnabledCipherSuites() method
     * documentation for more information
     */
    @Override
    public String[] getEnabledCipherSuites() {
        return sslParameters.getEnabledCipherSuites();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#setEnabledCipherSuites(String) method
     * documentation for more information
     */
    @Override
    public void setEnabledCipherSuites(String[] suites) {
        sslParameters.setEnabledCipherSuites(SSL, suites);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#getSupportedProtocols() method
     * documentation for more information
     */
    @Override
    public String[] getSupportedProtocols() {
        return sslParameters.getSupportedProtocols();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#getEnabledProtocols() method
     * documentation for more information
     */
    @Override
    public String[] getEnabledProtocols() {
        return sslParameters.getEnabledProtocols();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#setEnabledProtocols(String) method
     * documentation for more information
     */
    @Override
    public void setEnabledProtocols(String[] protocols) {
        sslParameters.setEnabledProtocols(SSL, protocols);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#setUseClientMode(boolean) method
     * documentation for more information
     */
    @Override
    public void setUseClientMode(boolean mode) {
        if (handshake_started) {
            throw new IllegalArgumentException(
            "Could not change the mode after the initial handshake has begun.");
        }
        sslParameters.setUseClientMode(mode);
        peer_mode_was_set = true;
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#getUseClientMode() method
     * documentation for more information
     */
    @Override
    public boolean getUseClientMode() {
        return sslParameters.getUseClientMode();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#setNeedClientAuth(boolean) method
     * documentation for more information
     */
    @Override
    public void setNeedClientAuth(boolean need) {
        sslParameters.setNeedClientAuth(SSL, need);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#getNeedClientAuth() method
     * documentation for more information
     */
    @Override
    public boolean getNeedClientAuth() {
        return sslParameters.getNeedClientAuth();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#setWantClientAuth(boolean) method
     * documentation for more information
     */
    @Override
    public void setWantClientAuth(boolean want) {
        sslParameters.setWantClientAuth(SSL, want);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#getWantClientAuth() method
     * documentation for more information
     */
    @Override
    public boolean getWantClientAuth() {
        return sslParameters.getWantClientAuth();
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#setEnableSessionCreation(boolean) method
     * documentation for more information
     */
    @Override
    public void setEnableSessionCreation(boolean flag) {
        sslParameters.setEnableSessionCreation(flag);
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#getEnableSessionCreation() method
     * documentation for more information
     */
    @Override
    public boolean getEnableSessionCreation() {
        return sslParameters.getEnableSessionCreation();
    }

    // -----------------------------------------------------------------

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#getHandshakeStatus() method
     * documentation for more information
     */
    @Override
    public SSLEngineResult.HandshakeStatus getHandshakeStatus() {
        if (!handshake_started || engine_was_shutteddown) {
            // initial handshake has not been started yet
            return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
        }
        return handshakeStatus;
    }
    
    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#getSession() method
     * documentation for more information
     */
    @Override
    public SSLSession getSession() {
        if (session != null) {
            return session;
        }
        return SSLSessionImpl.NULL_SESSION;
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#isInboundDone() method
     * documentation for more information
     */
    @Override
    public boolean isInboundDone() {
        return isInboundDone || engine_was_closed;
    }

    /**
     * This method works according to the specification of implemented class.
     * @see javax.net.ssl.SSLEngine#isOutboundDone() method
     * documentation for more information
     */
    @Override
    public boolean isOutboundDone() {
        return isOutboundDone;
    }

    /**
     * Decodes one complete SSL/TLS record provided in the source buffer.
     * If decoded record contained application data, this data will
     * be placed in the destination buffers.
     * For more information about TLS record fragmentation see
     * TLS v 1 specification (http://www.ietf.org/rfc/rfc2246.txt) p 6.2.
     * @param src source buffer containing SSL/TLS record.
     * @param dsts destination buffers to place received application data.
     * @see javax.net.ssl.SSLEngine#unwrap(ByteBuffer,ByteBuffer[],int,int)
     * method documentation for more information
     */
    @Override
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts,
                                int offset, int length) throws SSLException {
        if (engine_was_shutteddown) {
            return new SSLEngineResult(SSLEngineResult.Status.CLOSED,
                    SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0);
        }
        if ((src == null) || (dsts == null)) {
            throw new IllegalStateException(
                    "Some of the input parameters are null");
        }

        if (!handshake_started) {
            beginHandshake();
        }

        // only use the first buffer at the moment
        byte[] dst = dsts[0].array();
        int dst_len = dst.length;
        return unwrapImpl(SSLEngineAddress, src.array(), src.array().length, dst, dst_len);
    }

    /**
     * Encodes the application data into SSL/TLS record. If handshake status
     * of the engine differs from NOT_HANDSHAKING the operation can work
     * without consuming of the source data.
     * For more information about TLS record fragmentation see
     * TLS v 1 specification (http://www.ietf.org/rfc/rfc2246.txt) p 6.2.
     * @param srcs the source buffers with application data to be encoded
     * into SSL/TLS record.
     * @param offset the offset in the destination buffers array pointing to
     * the first buffer with the source data.
     * @param len specifies the maximum number of buffers to be procesed.
     * @param dst the destination buffer where encoded data will be placed.
     * @see javax.net.ssl.SSLEngine#wrap(ByteBuffer[],int,int,ByteBuffer) method
     * documentation for more information
     */
    @Override
    public SSLEngineResult wrap(ByteBuffer[] srcs, int offset,
                            int len, ByteBuffer dst) throws SSLException {
        if (engine_was_shutteddown) {
            return new SSLEngineResult(SSLEngineResult.Status.CLOSED,
                    SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0);
        }
        if ((srcs == null) || (dst == null)) {
            throw new IllegalStateException(
                    "Some of the input parameters are null");
        }
        if (dst.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }

        if (!handshake_started) {
            beginHandshake();
        }
        
        // only use the first buffer at the moment
        byte[] src = srcs[0].array();
        int src_len = src.length;
        
        return wrapImpl(SSLEngineAddress, src, src_len, dst.array(), dst.array().length);
    }
    
    // Shutdownes the engine and makes all cleanup work.
    private void shutdown() {
        engine_was_closed = true;
        engine_was_shutteddown = true;
        isOutboundDone = true;
        isInboundDone = true;
    }


    private SSLEngineResult.Status getEngineStatus() {
        return (engine_was_closed)
            ? SSLEngineResult.Status.CLOSED
            : SSLEngineResult.Status.OK;
    }
}

