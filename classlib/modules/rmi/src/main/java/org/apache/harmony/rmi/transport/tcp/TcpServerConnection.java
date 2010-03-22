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
package org.apache.harmony.rmi.transport.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.server.UID;
import java.security.AccessController;

import org.apache.harmony.rmi.common.GetLongPropAction;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIProperties;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.server.ServerConnection;
import org.apache.harmony.rmi.server.ServerConnectionManager;


/**
 * Tcp extension of ServerConnection.
 *
 * @author  Mikhail A. Markov
 */
public class TcpServerConnection extends ServerConnection {

    /** Log for logging tcp connections activity. */
    protected static final RMILog tcpTransportLog = RMILog.getTcpTransportLog();

    /*
     * The time used as an idle timeout for incoming connections (in ms).
     * Default value is 2 * 3600 * 1000 ms (2 hours).
     */
    private static int readTimeout = ((Long) AccessController.doPrivileged(
            new GetLongPropAction(RMIProperties.READTIMEOUT_PROP,
                    2 * 3600 * 1000))).intValue();

    /**
     * Constructs TcpServerConnection working through socket specified.
     *
     * @param s Socket connected to the client
     * @param mgr ConnectionManager managing this connection
     *
     * @throws IOException if an I/O error occurred during getting
     *         input/output streams from specified socket
     */
    public TcpServerConnection(Socket s, ServerConnectionManager mgr)
            throws IOException {
        super(s, mgr);
        s.setSoTimeout(readTimeout);
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
                throw new UnmarshalException(Messages.getString("rmi.83", ver)); //$NON-NLS-1$
            }
        } catch (IOException ioe) {
            // rmi.84=Unable to read RMI protocol header
            throw new UnmarshalException(Messages.getString("rmi.84"), //$NON-NLS-1$
                    ioe);
        }

        if (tcpTransportLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.85=Using protocol version {0}
            tcpTransportLog.log(RMILog.VERBOSE, Messages.getString("rmi.85", //$NON-NLS-1$
                    PROTOCOL_VER));
        }
        DataOutputStream dout = new DataOutputStream(out);

        // read protocol type
        if (din.readByte() == STREAM_PROTOCOL) {
            if (tcpTransportLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.90=Using stream RMI protocol
                tcpTransportLog.log(RMILog.VERBOSE,
                        Messages.getString("rmi.90")); //$NON-NLS-1$
            }
        } else {
            dout.writeByte(PROTOCOL_NOT_SUPPORTED);
            dout.flush();
            return -1;
        }

        // send ack msg
        dout.writeByte(PROTOCOL_ACK);

        // send client's host and port
        String host = s.getInetAddress().getHostAddress();
        int port = s.getPort();
        dout.writeUTF(host);
        dout.writeInt(port);
        dout.flush();

        if (tcpTransportLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.136=Server is seeing client as {0}:{1}
            tcpTransportLog.log(RMILog.VERBOSE,
                    Messages.getString("rmi.log.136", host, port)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // read host and port
        din.readUTF();
        din.readInt();

        // protocol is agreed
        return STREAM_PROTOCOL;
    }

    /**
     * @see ServerConnection.waitCallMsg()
     */
    protected int waitCallMsg() throws IOException {
        int data;

        while (true) {
            try {
                data = in.read();
            } catch (IOException ioe) {
                data = -1;
            }

            if (data == -1) {
                if (tcpTransportLog.isLoggable(RMILog.BRIEF)) {
                    // rmi.log.123=Connection [{0}] is closed
                    tcpTransportLog.log(RMILog.BRIEF,
                            Messages.getString("rmi.log.123", toString())); //$NON-NLS-1$ //$NON-NLS-2$
                }
                return -1;
            }
            DataOutputStream dout = new DataOutputStream(out);

            if (data == PING_MSG) {
                if (tcpTransportLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.124=Got ping request
                    tcpTransportLog.log(RMILog.VERBOSE,
                            Messages.getString("rmi.log.124")); //$NON-NLS-1$
                }

                // send ping ack
                dout.writeByte(PING_ACK);
                dout.flush();
            } else if (data == DGCACK_MSG) {
                if (tcpTransportLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.125=Got DGC ack request
                    tcpTransportLog.log(RMILog.VERBOSE,
                            Messages.getString("rmi.log.125")); //$NON-NLS-1$
                }
                dgcUnregisterUID(UID.read(new DataInputStream(in)));
            } else if (data == CALL_MSG) {
                if (tcpTransportLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.126=Got call request
                    tcpTransportLog.log(RMILog.VERBOSE,
                            Messages.getString("rmi.log.126")); //$NON-NLS-1$
                }
                return data;
            } else {
                if (tcpTransportLog.isLoggable(RMILog.BRIEF)) {
                    // rmi.log.127=Unknown request got: {0}
                    tcpTransportLog.log(RMILog.BRIEF,
                            Messages.getString("rmi.log.127", data)); //$NON-NLS-1$
                }
                // rmi.91=Unknown message got: {0}
                throw new RemoteException(Messages.getString("rmi.91", data)); //$NON-NLS-1$
            }
        }
    }

    /**
     * Returns string representation of this connection.
     *
     * @return string representation of this connection
     */
    public String toString() {
        return "TcpServerConnection: remote endpoint:" + ep; //$NON-NLS-1$
    }
}
