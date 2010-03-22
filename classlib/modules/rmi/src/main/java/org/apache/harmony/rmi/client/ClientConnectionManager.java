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
package org.apache.harmony.rmi.client;

import java.net.Socket;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.apache.harmony.rmi.common.CreateThreadAction;
import org.apache.harmony.rmi.common.GetLongPropAction;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIProperties;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.transport.Endpoint;
import org.apache.harmony.rmi.transport.proxy.HttpConnection;
import org.apache.harmony.rmi.transport.proxy.HttpOutboundSocket;
import org.apache.harmony.rmi.transport.tcp.TcpConnection;


/**
 * Manager managing connections from the client side: it holds the list of
 * opened connections so the client could reuse them.
 *
 * @author  Mikhail A. Markov
 */
public final class ClientConnectionManager {

    // List of active connections
    private static Hashtable connsTable = new Hashtable();

    /**
     * Period during which the connection could be reused
     * (and after which it's closed).
     * Default value is 15000 ms.
     */
    public static long connTimeout = ((Long) AccessController.doPrivileged(
            new GetLongPropAction(
                    RMIProperties.CONNECTIONTIMEOUT_PROP, 15000))).longValue();

    /** Log for logging tcp connections activity. */
    public static final RMILog tcpTransportLog = RMILog.getTcpTransportLog();

    // Starts thread managing expired connections.
    static {
        ConnectionsCollector coll = new ConnectionsCollector();
        ((Thread) AccessController.doPrivileged(new CreateThreadAction(
                coll, "ConnectionsCollector", true))).start(); //$NON-NLS-1$
    }

    /**
     * Returns 1-st available connection. If there is no available connections
     * a new one is created.
     *
     * @param ep Endpoint to connect to
     *
     * @return opened connection
     *
     * @throws RemoteException if any error occurred while obtaining connection
     */
    public static ClientConnection getConnection(Endpoint ep)
            throws RemoteException {
        ClientConnection conn = null;

        synchronized (connsTable) {
            Set conns = (Set) connsTable.get(ep);

            if (conns != null && !conns.isEmpty()) {
                for (Iterator iter = conns.iterator(); iter.hasNext();) {
                    conn = (ClientConnection) iter.next();

                    if (conn.isReusable() && conn.reuse()) {
                        return conn;
                    }
                }
            }
        }
        Socket s = ep.createSocket();

        if (s instanceof HttpOutboundSocket) {
            conn = new HttpConnection(s, ep);
        } else {
            conn = new TcpConnection(s, ep);
        }

        synchronized (connsTable) {
            Set conns = (Set) connsTable.get(ep);

            if (conns == null) {
                conns = Collections.synchronizedSet(new HashSet());
            }
            conns.add(conn);
            connsTable.put(ep, conns);
        }
        return conn;
    }

    /**
     * Removes connection from the list of connections.
     *
     * @param conn connection to be removed
     */
    public static void removeConnection(ClientConnection conn) {
        Endpoint ep = conn.getEndpoint();

        synchronized (connsTable) {
            Set conns = (Set) connsTable.get(ep);

            if (conns == null) {
                return;
            }
            conns.remove(conn);

            if (conns.size() == 0) {
                connsTable.remove(ep);
            }
        }
    }

    /*
     * Class checking reusable available connections if they are already
     * expired - and removing them.
     */
    private static class ConnectionsCollector implements Runnable {

        /**
         * Checks reusable available connections if they are already expired and
         * removes them.
         */
        public void run() {
            long wakeUpTime;

            while (true) {
                wakeUpTime = Long.MAX_VALUE;

                synchronized (connsTable) {
                    for (Enumeration eps = connsTable.keys();
                            eps.hasMoreElements();) {
                        Endpoint ep = (Endpoint) eps.nextElement();
                        Set conns = (Set) connsTable.get(ep);

                        if (conns.isEmpty()) {
                            connsTable.remove(ep);
                            continue;
                        }

                        for (Iterator iter = conns.iterator();
                                iter.hasNext();) {
                            ClientConnection conn =
                                    (ClientConnection) iter.next();

                            if (conn.isReusable() && conn.isAvailable()) {
                                long expirTime = conn.getExpiration();
                                long curTime = System.currentTimeMillis();

                                if (expirTime <= curTime) {
                                    // connection is expired
                                    conn.close(false);
                                    iter.remove();

                                    if (tcpTransportLog.isLoggable(
                                            RMILog.VERBOSE)) {
                                        // rmi.log.37={0} connection timeout is expired
                                        tcpTransportLog.log(RMILog.VERBOSE,
                                            Messages.getString("rmi.log.37", conn.toString())); //$NON-NLS-1$
                                    }
                                    continue;
                                }
                                wakeUpTime = Math.min(wakeUpTime, expirTime);
                            }
                        }
                    }
                }
                long sleepTime;

                if (wakeUpTime == Long.MAX_VALUE) {
                    sleepTime = connTimeout;
                } else {
                    sleepTime = wakeUpTime - System.currentTimeMillis();
                }

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}
