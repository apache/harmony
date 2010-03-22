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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.harmony.rmi.common.CreateThreadAction;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.transport.Endpoint;
import org.apache.harmony.rmi.transport.proxy.HttpInboundSocket;
import org.apache.harmony.rmi.transport.proxy.HttpServerConnection;
import org.apache.harmony.rmi.transport.tcp.TcpServerConnection;


/**
 * Manager waiting for client connections and initiating communication with
 * them.
 *
 * @author  Mikhail A. Markov
 */
public class ServerConnectionManager implements Runnable {

    // Client host for RemoteServer.getClientHost() method
    static ThreadLocal clientHost = new ThreadLocal();

    /*
     * Table of local endpoints (keys) and appropriate mapped connection
     * manager (values).
     */
    private static Hashtable localEps = new Hashtable();

    // ServerSocket where this manager waits for connections
    private ServerSocket ss;

    // Local Endpoint for this connection
    private Endpoint ep;

    // List of accepted(active) connections
    private Set conns = Collections.synchronizedSet(new HashSet());


    // Number of in-progress calls to the objects in this table.
    private int activeCallsNum = 0;

    // lock object for working with active calls
    private class CallsLock {}
    private Object callsLock = new CallsLock();

    /*
     * Default wait time after 5 consecutive failed accept attempts
     * if RMIFailureHandler is not set.
     */
    private static final long defaultFailureDelay = 3000;

    // Number of failed accepts attempts.
    private long failedAcceptsNum = 0;

    // Log for logging transport-layer activity
    static final RMILog transportLog = RMILog.getTransportLog();

    /**
     * Constructs ServerConnectionManager and creates ServerSocket.
     *
     * @param sref server-side handle for exported remote object
     *
     * @throws IOException if any I/O error occurred during opening ServerSocket
     */
    private ServerConnectionManager(Endpoint localEp)
            throws IOException {
        ep = localEp;
        ss = ep.createServerSocket();
    }

    /**
     * Returns ServerConnectionManager corresponding to the given ep. If such a
     * manager does not exist creates it and put record to the table.
     *
     * @param ep Endpoint to get ServerConnectionManager for
     *
     * @return found (created) ServerConnectionManager
     *
     * @throws IOException and and I/O error occurred during manager creation
     */
    public static synchronized ServerConnectionManager getMgr(Endpoint ep)
            throws IOException {
        ServerConnectionManager mgr;
        Endpoint tmpl = null;

        if (ep.getPort() != 0) {
            tmpl = Endpoint.createTemplate(ep);
            mgr = (ServerConnectionManager) localEps.get(tmpl);

            if (mgr != null) {
                // verify that we can listen on the Endpoint's port
                SecurityManager sm = System.getSecurityManager();

                if (sm != null) {
                    sm.checkListen(ep.getPort());
                }
                return mgr;
            }
        }
        mgr = new ServerConnectionManager(ep);
        ((Thread) AccessController.doPrivileged(
                new CreateThreadAction(mgr, "ServerConnectionManager[" //$NON-NLS-1$
                        + mgr.getEndpoint() + "]", true))).start(); //$NON-NLS-1$
        if (tmpl == null) {
            tmpl = Endpoint.createTemplate(ep);
        }
        localEps.put(tmpl, mgr);
        return mgr;
    }

    /**
     * Returns the string representation of the client's host for RMI calls
     * which are processed in the current thread (this method is intended to be
     * called by RemoteServer.getClientHost() method).
     *
     * @return string representation of the client's host for RMI calls which
     *         are processed in the current thread
     */
    public static String getClientHost() {
        return (String) clientHost.get();
    }

    /**
     * Returns true if there are in-progress calls to remote objects
     * associated with this manager.
     *
     * @return true if there are in-progress calls to remote object
     *         associated with this manager
     */
    public boolean hasActiveCalls() {
        synchronized (callsLock) {
            return (activeCallsNum != 0);
        }
    }

    /**
     * Returns server's endpoint.
     *
     * @return server's endpoint;
     */
    public Endpoint getEndpoint() {
        return ep;
    }

    /**
     * Starts waiting for incoming remote calls. When connection from remote
     * is accepted, separate thread to process remote call is spawned. Waits
     * for connections until this thread will not be interrupted.
     */
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Socket s = ss.accept();
                startConnection(s);
                failedAcceptsNum = 0;
            } catch (Exception ex) {
                RMIFailureHandler rfh = RMISocketFactory.getFailureHandler();

                if (rfh != null) {
                    if (rfh.failure(ex)) {
                        return;
                    }
                } else {
                    // We will try to immediately accept another client again,
                    // but if we have a bad client which fails our accept tries
                    // for a number of times, we should sleep for a while.
                    if (failedAcceptsNum >= 5) {
                        try {
                            Thread.sleep(defaultFailureDelay);
                        } catch (InterruptedException ie) {
                            return;
                        }
                        failedAcceptsNum = 0;
                    }
                }
            }
        }
    }

    /**
     * Stops specified connection with remote client: closes opened Socket,
     * stops appropriate thread and removes this connection from the list of
     * active connections.
     *
     * @param conn connection to be stopped
     */
    public void stopConnection(ServerConnection conn) {
        conn.close();
        conns.remove(conn);
    }

    /**
     * Increase the number of active calls by one.
     */
    protected void addActiveCall() {
        synchronized (callsLock) {
            ++activeCallsNum;
            ExportManager.addActiveCall();
        }
    }

    /**
     * Decrease the number of active calls by one.
     */
    protected void removeActiveCall() {
        synchronized (callsLock) {
            --activeCallsNum;
            ExportManager.removeActiveCall();
        }
    }

    /*
     * Starts separate thread communicating with remote client to process
     * remote call.
     *
     * @param s Socket connected with remote client
     *
     * @return connection to the remote client
     *
     * @throws IOException if any I/O error occurred while starting connection
     */
    private ServerConnection startConnection(Socket s)
            throws IOException {
        ServerConnection conn;

        if (s instanceof HttpInboundSocket) {
            conn = new HttpServerConnection(s, this);
        } else {
            conn = new TcpServerConnection(s, this);
        }
        conns.add(conn);

        /*
         * Start the thread in non-system group
         * (see comment for CreateThreadAction class).
         */
        Thread connThread = (Thread) AccessController.doPrivileged(
                new CreateThreadAction(conn, "Call from " + conn.ep, true, //$NON-NLS-1$
                        false));
        connThread.start();

        if (transportLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.10A=Accepted {0}
            transportLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.10A", conn)); //$NON-NLS-1$
        }
        return conn;
    }
}
