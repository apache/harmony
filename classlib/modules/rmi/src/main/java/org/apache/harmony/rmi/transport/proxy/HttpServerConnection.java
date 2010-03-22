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
package org.apache.harmony.rmi.transport.proxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.server.UID;

import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.server.ServerConnection;
import org.apache.harmony.rmi.server.ServerConnectionManager;


/**
 * Http extension of ServerConnection.
 *
 * @author  Mikhail A. Markov
 */
public class HttpServerConnection extends ServerConnection
        implements ProxyConstants {

    // If true then this connection was closed.
    private boolean isClosed = false;

    /**
     * Constructs HttpServerConnection working through socket specified.
     *
     * @param s Socket connected to the client
     * @param mgr ConnectionManager managing this connection
     *
     * @throws IOException if an I/O error occurred during getting
     *         input/output streams from specified socket
     */
    public HttpServerConnection(Socket s, ServerConnectionManager mgr)
            throws IOException {
        super(s, mgr);
    }

    /**
     * @see ServerConnection.clientProtocolAck()
     */
    protected int clientProtocolAck() throws IOException {
        byte data;
        DataInputStream din = new DataInputStream(in);

        try {
            // read RMI header
            int header = din.readInt();

            if (header != RMI_HEADER) {
                // rmi.82=Unknown header: {0}
                throw new UnmarshalException(Messages.getString("rmi.82", header)); //$NON-NLS-1$
            }

            // read RMI protocol version
            short ver = din.readShort();

            if (ver != PROTOCOL_VER) {
                // rmi.83=Unknown RMI protocol version: {0}
                throw new UnmarshalException(Messages.getString("rmi.83", ver));//$NON-NLS-1$
            }
        } catch (IOException ioe) {
            // rmi.84=Unable to read RMI protocol header
            throw new UnmarshalException(Messages.getString("rmi.84"), //$NON-NLS-1$
                    ioe);
        }

        if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.85=Using protocol version {0}
            proxyTransportLog.log(RMILog.VERBOSE, Messages.getString("rmi.85", //$NON-NLS-1$
                    PROTOCOL_VER));
        }

        // read protocol type
        if (din.readByte() == SINGLEOP_PROTOCOL) {

            if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.86=Using singleop RMI protocol
                proxyTransportLog.log(RMILog.VERBOSE,
                        Messages.getString("rmi.86")); //$NON-NLS-1$
            }
        } else {
            return -1;
        }

        // protocol is agreed
        return SINGLEOP_PROTOCOL;
    }

    /**
     * @see ServerConnection.waitCallMsg()
     */
    protected int waitCallMsg() throws IOException {
        if (isClosed) {
            return -1;
        }
        int data;

        try {
            data = in.read();
        } catch (IOException ioe) {
            data = -1;
        }

        if (data == -1) {
            if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.123=Connection [{0}] is closed
                proxyTransportLog.log(RMILog.VERBOSE,
                        Messages.getString("rmi.log.123", toString())); //$NON-NLS-1$
            }
            return -1;
        }
        DataOutputStream dout = new DataOutputStream(out);

        if (data == PING_MSG) {
            if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.124=Got ping request
                proxyTransportLog.log(RMILog.VERBOSE,Messages.getString("rmi.log.124")); //$NON-NLS-1$
            }
            releaseInputStream();

            // send ping ack
            dout.writeByte(PING_ACK);
            dout.close();
            return -1;
        } else if (data == DGCACK_MSG) {
            if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.125=Got DGC ack request
                proxyTransportLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.125")); //$NON-NLS-1$
            }
            dgcUnregisterUID(UID.read(new DataInputStream(in)));
            releaseInputStream();
            dout.close();
            return -1;
        } else if (data == CALL_MSG) {
            if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.126=Got call request
                proxyTransportLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.126")); //$NON-NLS-1$
            }
            return data;
        } else {
            if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.127=Unknown request got: {0}
                proxyTransportLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.127", data)); //$NON-NLS-1$
            }
            // rmi.87=Unknown message got: {0}
            throw new RemoteException(Messages.getString("rmi.87", data)); //$NON-NLS-1$
        }
    }

    /**
     * Closes output stream. After that call this connection is treated as
     * closed and could be reused.
     */
    public synchronized void releaseOutputStream() throws IOException {
        if (isClosed) {
            return;
        }
        isClosed = true;
        out.close();
    }

    /**
     * Returns string representation of this connection.
     *
     * @return string representation of this connection
     */
    public String toString() {
        return "HttpServerConnection: remote endpoint:" + ep; //$NON-NLS-1$
    }
}
