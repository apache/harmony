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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.rmi.server.RemoteCall;

import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.transport.RMIObjectInputStream;
import org.apache.harmony.rmi.transport.RMIObjectOutputStream;
import org.apache.harmony.rmi.transport.RMIProtocolConstants;


/**
 * RemoteCall implementation used by UnicastServerRef on server's side.
 *
 * @author  Mikhail A. Markov
 */
public class ServerRemoteCall implements RemoteCall, RMIProtocolConstants {

    // Connection to remote server.
    private ServerConnection conn;

    // InputStream for reading objects.
    private ObjectInputStream oin = null;

    // OutputStream for sending objects.
    private RMIObjectOutputStream oout = null;

    // True if getResultStream has been called.
    private boolean hasResStream = false;

    /**
     * Constructs ServerRemoteCall from existing connection.
     *
     * @param conn opened ServerConnection
     */
    public ServerRemoteCall(ServerConnection conn) {
        this.conn = conn;
    }
    /**
     * Constructs ServerRemoteCall from opened connection and already created
     * ObjectOutputStream.
     *
     * @param conn opened ServerConnection
     * @param oin created ObjectOutputStream
     */
    public ServerRemoteCall(ServerConnection conn, ObjectInputStream oin) {
        this.conn = conn;
        this.oin = oin;
    }

    /**
     * Constructs ObjectInputStream (if it was not created yet) and returns
     * this created stream.
     *
     * @return ObjectInputStream to read objects from
     *
     * @throws IOException if an I/O error occurred during stream construction
     */
    public ObjectInput getInputStream() throws IOException {
        if (oin == null) {
            oin = new RMIObjectInputStream(conn.getInputStream());
        }
        return oin;
    }

    /**
     * Constructs ObjectOutputStream (if it was not created yet) and returns
     * this created stream.
     *
     * @return ObjectOutputStream to write objects to
     *
     * @throws IOException if an I/O error occurred during stream construction
     */

    public ObjectOutput getOutputStream() throws IOException {
        if (oout == null) {
            oout = new RMIObjectOutputStream(conn.getOutputStream());
        }
        return oout;
    }

    /**
     * Writes byte meaning normal call return, writes byte identifying call
     * result (normal return or exception) - depending on success parameter,
     * writes UID of the object (for DGC) and flushes the output stream.
     * This method could be called only once.
     *
     * @param success if true - means that method call was successful (i.e.
     *        with no exception) - return data description will be written to
     *        the output stream
     *
     * @throws IOException if an I/O error occurred while writing to the stream
     * @throws StreamCorruptedException if this method has already been called
     */
    public ObjectOutput getResultStream(boolean success)
            throws IOException, StreamCorruptedException {
        if (hasResStream) {
            // rmi.7A=getResultStream() method has already been called.
            throw new StreamCorruptedException(Messages.getString("rmi.7A")); //$NON-NLS-1$
        }
        (new DataOutputStream(conn.getOutputStream())).writeByte(CALL_OK);

        if (oout == null) {
            oout = new RMIObjectOutputStream(conn.getOutputStream(), true);
        }
        oout.writeByte(success ? RETURN_VAL : RETURN_EX);
        oout.writeUID();
        oout.flush();
        hasResStream = true;
        return oout;
    }

    /**
     * @see RemoteCall.releaseInputStream()
     */
    public void releaseInputStream() throws IOException {
        conn.releaseInputStream();
    }

    /**
     * @see RemoteCall.releaseOutputStream()
     */
    public void releaseOutputStream() throws IOException {
    }

    /**
     * @see RemoteCall.done()
     */
    public void done() throws IOException {
        conn.close();
    }

    /**
     * Not used on server side.
     */
    public void executeCall() throws Exception {
    }

    /**
     * Returns string representation of this RemoteCall.
     *
     * @return string representation of this RemoteCall
     */
    public String toString() {
        return "ServerRemoteCall: connection: " + conn; //$NON-NLS-1$
    }

    /**
     * Returns true if getResultStream was already called before and
     * false otherwise.
     *
     * @return true if getResultStream was already called before and
     *         false otherwise
     */
    public boolean hasResultStream() {
        return hasResStream;
    }
}
