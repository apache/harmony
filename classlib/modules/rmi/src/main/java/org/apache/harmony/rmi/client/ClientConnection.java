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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.rmi.server.UID;

import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.transport.Endpoint;
import org.apache.harmony.rmi.transport.RMIProtocolConstants;


/**
 * Connection opened by client side. It acknowledges RMI protocol version,
 * RMI protocol type etc.
 *
 * @author  Mikhail A. Markov
 */
public abstract class ClientConnection implements RMIProtocolConstants {

    /** Connected socket. */
    protected Socket s;

    /** InputStream open from the socket. */
    protected InputStream in;

    /** OutputStream open from the socket. */
    protected OutputStream out;

    /** Endpoint this connection connected to. */
    protected Endpoint ep;

    /* Log for logging tcp connections activity. */
    private static final RMILog transportLog = RMILog.getTransportLog();

    /* log for logging dgc activity. */
    private static final RMILog dgcLog = RMILog.getDGCLog();

    /**
     * Constructs ClientConnection, obtains input/output streams and acknowledge
     * protocol with server side.
     *
     * @param s Connected socket
     * @param ep server's endpoint
     *
     * @throws RemoteException if any I/O error occurred during connection
     *         creation
     */
    public ClientConnection(Socket s, Endpoint ep) throws RemoteException {
        this.s = s;
        this.ep = ep;

        try {
            out = new BufferedOutputStream(s.getOutputStream());
            in = new BufferedInputStream(s.getInputStream());
        } catch (IOException ioe) {
            // rmi.40=Unable to establish connection to server
            throw new ConnectException(Messages.getString("rmi.40"), ioe); //$NON-NLS-1$
        }
        serverProtocolAck();
    }

    /**
     * Opens a connection to the given Endpoint and writes DGC ack there.
     *
     * @param uid UID to be send
     */
    public void sendDGCAck(UID uid) {
        ClientConnection conn = null;

        try {
            conn = ClientConnectionManager.getConnection(ep);
            DataOutputStream dout = new DataOutputStream(out);
            dout.writeByte(RMIProtocolConstants.DGCACK_MSG);
            uid.write(dout);
            dout.flush();
            conn.releaseOutputStream();
            conn.done();
        } catch (IOException ioe) {
            if (conn != null) {
                conn.close();
            }
        }

        if (dgcLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.93=Sent DGC ack to {0} for {1}
            dgcLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.93", ep, uid)); //$NON-NLS-1$
        }
    }

    /**
     * Acknowledge protocol with server side.
     *
     * @return acknowledged protocol number
     *
     * @throws RemoteException if any I/O exception occurred during protocol
     *         acknowledgement
     */
    protected abstract int serverProtocolAck() throws RemoteException;

    /**
     * Writes RMI protocol header and RMI protocol version to the open
     * OutputStream.
     *
     * @param dout DataOutputStream to write header to
     *
     * @throws RemoteException if any I/O error occurred while writing header
     */
    protected void writeHeader(DataOutputStream dout) throws RemoteException {
        try {
            dout.writeInt(RMI_HEADER);
            dout.writeShort(PROTOCOL_VER);
        } catch (IOException ioe) {
            // rmi.41=Unable to write RMI protocol header
            throw new ConnectIOException(Messages.getString("rmi.41"), ioe); //$NON-NLS-1$
        }

        if (transportLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.94=Using protocol version {0}
            transportLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.94", //$NON-NLS-1$
                    PROTOCOL_VER));
        }
    }

    /**
     * Closes this connection (i.e. closes opened Socket) and remove this
     * Connection from the list of active connections in ConnectionManager.
     */
    public void close() {
        close(true);
    }

    /**
     * Closes this connection (i.e. closes opened Socket) and if remove
     * parameter is true then remove this Connection from the list of active
     * connections in ConnectionManager.
     *
     * @param remove if true then remove this Connection from the list of active
     *        connections in ConnectionManager
     */
    public void close(boolean remove) {
        try {
            s.close();
        } catch (Exception ex) {
        }

        if (remove) {
            ClientConnectionManager.removeConnection(this);
        }
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
     * Signals to the connection that remote call is done.
     */
    public abstract void done();

    /**
     * By default flushes output stream of this connection.
     *
     * @throws IOException if any I/O error occurred
     */
    public void releaseOutputStream() throws IOException {
        out.flush();
    }

    /**
     * If this connection is reusable and available then reuse it.
     *
     * @return true if this connection was successfully prepared for reusing
     */
    public abstract boolean reuse();

    /**
     * Returns true if this connection is reusable and has no active
     * remote calls.
     *
     * @return true if this connection is reusable and has no active
     *         remote calls
     */
    public abstract boolean isAvailable();

    /**
     * True if this connection could be reused for another remote call.
     *
     * @return true if this connection could be reused for another remote call
     */
    public abstract boolean isReusable();

    /**
     * If this connection is available returns time when it could be closed
     * (if it'll be still in available state).
     *
     * @return returns time when the connection could be closed
     */
    public abstract long getExpiration();

    /**
     * Returns endpoint this connection connected to.
     *
     * @return endpoint this connection connected to
     */
    public Endpoint getEndpoint() {
        return ep;
    }

    /**
     * Returns string representation of this connection.
     *
     * @return string representation of this connection
     */
    public String toString() {
        return RMIUtil.getShortName(getClass()) + ": endpoint:" + ep; //$NON-NLS-1$
    }
}
