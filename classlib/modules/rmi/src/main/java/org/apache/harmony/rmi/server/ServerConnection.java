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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Mikhail A. Markov
 */
package org.apache.harmony.rmi.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.rmi.server.ObjID;
import java.rmi.server.UID;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.remoteref.UnicastServerRef;
import org.apache.harmony.rmi.transport.Endpoint;
import org.apache.harmony.rmi.transport.RMIObjectInputStream;
import org.apache.harmony.rmi.transport.RMIObjectOutputStream;
import org.apache.harmony.rmi.transport.RMIProtocolConstants;


/**
 * Connection opened on server side when connection from the client is accepted.
 * It acknowledges RMI protocol version, RMI protocol type etc. and after that
 * pass the control to the appropriate UnicastServerRef for processing the
 * remote call itself.
 *
 * @author  Mikhail A. Markov
 */
public abstract class ServerConnection
        implements RMIProtocolConstants, Runnable {

    /** Connected socket. */
    protected Socket s;

    /** InputStream open from the socket. */
    protected InputStream in;

    /** OutputStream open from the socket. */
    protected OutputStream out;

    /** Endpoint which this connection connected to. */
    protected Endpoint ep;

    /** ServerConnectionManager managing this connection. */
    protected ServerConnectionManager mgr;

    /** Server reference where current remote call is dispatching. */
    protected UnicastServerRef sref = null;

    /**
     * Constructs ServerConnection working through socket specified.
     *
     * @param s Socket connected to the client
     * @param mgr ServerConnectionManager managing this connection
     *
     * @throws IOException if an I/O error occurred during getting
     *         input/output streams from specified socket
     */
    public ServerConnection(Socket s, ServerConnectionManager mgr)
            throws IOException {
        this.s = s;
        this.mgr = mgr;
        ep = new Endpoint(s.getInetAddress().getHostAddress(),
                          s.getPort(),
                          mgr.getEndpoint().getClientSocketFactory(),
                          mgr.getEndpoint().getServerSocketFactory());
        out = new BufferedOutputStream(s.getOutputStream());
        in = new BufferedInputStream(s.getInputStream());
    }

    /**
     * Acknowledges RMI protocol version, RMI protocol type etc. and wait for
     * remote calls from client. After call message dispatch call to appropriate
     * UnicastServerRef for processing.
     */
    public void run() {
        // sets client host for RemoteServer.getClientHost() method
        ServerConnectionManager.clientHost.set(
                s.getInetAddress().getHostAddress());

        try {
            // acknowledge protocol
            if (clientProtocolAck() < 0) {
                return;
            }

            // wait until RMI call msg is received from client or until client
            // closes the connection
            while (waitCallMsg() != -1) {
                RMIObjectInputStream oin = new RMIObjectInputStream(in);
                final ServerRemoteCall sCall = new ServerRemoteCall(this, oin);
                ObjID id = null;

                try {
                    id = ObjID.read(oin);
                } catch (IOException ioe) {
                    // rmi.74=Unable to read Object ID
                    throw new UnmarshalException(Messages.getString("rmi.74"), //$NON-NLS-1$
                            ioe);
                }
                RMIObjectInfo info = ExportManager.getInfo(id);
                RemoteException exToReturn = null;

                if (info == null) {
                    sref = null;
                    // rmi.75=No objects with {0} exported.
                    exToReturn = new NoSuchObjectException(
                            Messages.getString("rmi.75",id)); //$NON-NLS-1$
                } else {
                    sref = info.sref;

                    if (!sref.isSystem()) {
                        mgr.addActiveCall();
                    }

                    if (ServerConnectionManager.transportLog.isLoggable(
                            RMILog.VERBOSE)) {
                        // rmi.76=Dispatch call for processing
                        ServerConnectionManager.transportLog.log(RMILog.VERBOSE,
                                Messages.getString("rmi.76")); //$NON-NLS-1$
                    }

                    // Dispatch the call for processing
                    Thread curThread = Thread.currentThread();
                    ClassLoader curLoader = curThread.getContextClassLoader();
                    curThread.setContextClassLoader(info.loader);
                    final SecurityManager curMgr = System.getSecurityManager();

                    try {
                        AccessController.doPrivileged(
                                new PrivilegedExceptionAction() {
                                    public Object run() throws IOException {
                                        /*
                                         * First check if we can accept
                                         * the calls from the given endpoint.
                                         */
                                        if (curMgr != null) {
                                            curMgr.checkAccept(ep.getHost(),
                                                    ep.getPort());
                                        }
                                        sref.processCall(sCall);
                                        return null;
                                    }
                                }, info.acc);
                    } catch (PrivilegedActionException pae) {
                        IOException ioe = (IOException) pae.getException();

                        if (ioe instanceof RemoteException) {
                            // rmi.77=RemoteException occurred in server thread
                            exToReturn = new ServerException(Messages
                                    .getString("rmi.77"), //$NON-NLS-1$
                                    ioe);
                        } else {
                            throw ioe;
                        }
                    } finally {
                        curThread.setContextClassLoader(curLoader);
                    }
                }

                if (exToReturn != null) {
                    sCall.releaseInputStream();
                    if (ServerConnectionManager.transportLog.isLoggable(
                            RMILog.VERBOSE)) {
                        // rmi.log.10B=Return exception to the client: {0}
                        ServerConnectionManager.transportLog.log(RMILog.VERBOSE,
                                Messages.getString("rmi.log.10B", exToReturn));//$NON-NLS-1$
                    }
                    DataOutputStream dout = new DataOutputStream(out);
                    RMIObjectOutputStream oout;

                    if (sCall.hasResultStream()) {
                        oout = (RMIObjectOutputStream)
                                sCall.getOutputStream();
                    } else {
                        oout = (RMIObjectOutputStream)
                                sCall.getResultStream(false);
                    }
                    oout.writeObject(exToReturn);
                    oout.flush();
                }

                if (sref != null) {
                    if (!sref.isSystem()) {
                        mgr.removeActiveCall();
                    }
                    sref = null;
                }
                releaseOutputStream();
            }
        } catch (IOException ioe) {
            //ioe.printStackTrace(System.err);
        } finally {
            if (sref != null) {
                if (!sref.isSystem()) {
                    mgr.removeActiveCall();
                }
                sref = null;
            }

            // stop this thread, close the socket and remove this connection
            // from the list of active connections in ConnectionManager
            mgr.stopConnection(this);
        }
    }

    /**
     * Acknowledges protocol with client side.
     *
     * @return acknowledged protocol number
     *
     * @throws RemoteException if any I/O exception occurred during protocol
     *         acknowledgement
     */
    protected abstract int clientProtocolAck() throws IOException;

    /**
     * Waiting until 0x50 code (CALL_MSG) message will be received (this code
     * will be returned as a result of this method call). If the
     * connection with the client is closed while waiting for this code, -1
     * should be returned.
     *
     * @return 0x50 code if this code is received from the client or -1 if
     *         the socket was closed while waiting for CALL_MSG code
     *
     * @throws IOException if any I/O error occurred while communicating with
     *         client
     */
    protected abstract int waitCallMsg() throws IOException;

    /**
     * This method should be called when DGC_ACK response was received.
     * It unregisters the given UID in ClientDGC.
     *
     * @param uid UID DGC_ACK response for which was read
     */
    protected void dgcUnregisterUID(UID uid) {
        ClientDGC.unregisterForDGCAck(uid);
    }

    /**
     * Closes this connection.
     */
    public void close() {
        try {
            s.close();
        } catch (IOException e) {
            if (ServerConnectionManager.transportLog.isLoggable(
                    RMILog.VERBOSE)) {
                // rmi.log.10C=Note: close operation produced exception:
                ServerConnectionManager.transportLog.log(RMILog.VERBOSE,
                        Messages.getString("rmi.log.10C"), e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Returns true if this connection is handling remote call and false
     * otherwise.
     *
     * @return true if this connection is handling remote call and false
     *         otherwise
     */
    public boolean hasActiveCall() {
        return sref != null;
    }

    /**
     * By default does nothing.
     */
    public void releaseInputStream() throws IOException {
    }

    /**
     * By default does nothing.
     */
    public void releaseOutputStream() throws IOException {
    }

    /**
     * Returns open input stream.
     *
     * @return open input stream
     */
    public InputStream getInputStream() {
        return in;
    }

    /**
     * Returns open output stream.
     *
     * @return open output stream
     */
    public OutputStream getOutputStream() {
        return out;
    }

    /**
     * Returns string representation of this connection.
     *
     * @return string representation of this connection
     */
    public String toString() {
        return RMIUtil.getShortName(getClass()) + ": remote endpoint:" + ep; //$NON-NLS-1$
    }
}
