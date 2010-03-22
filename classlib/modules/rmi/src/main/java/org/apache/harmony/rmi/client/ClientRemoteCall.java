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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.UnexpectedException;
import java.rmi.server.RemoteCall;
import java.rmi.server.UID;

import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.remoteref.UnicastRef;
import org.apache.harmony.rmi.transport.RMIObjectInputStream;
import org.apache.harmony.rmi.transport.RMIObjectOutputStream;
import org.apache.harmony.rmi.transport.RMIProtocolConstants;


/**
 * RemoteCall implementation used by UnicastRef on client's side.
 *
 * @author  Mikhail A. Markov
 */
public class ClientRemoteCall implements RemoteCall, RMIProtocolConstants {

    // Connection to remote server.
    private ClientConnection conn;

    // InputStream for reading objects.
    private RMIObjectInputStream oin = null;

    // OutputStream for sending objects.
    private RMIObjectOutputStream oout = null;

    // true if this remote call was closed
    private boolean isClosed = false;

    // UID received from server
    private UID uid;

    // Method this remote call was created for.
    private Method m = null;

    /**
     * Constructs ClientRemoteCall from existing connection.
     *
     * @param conn opened ClientConnection
     */
    public ClientRemoteCall(ClientConnection conn) {
        this.conn = conn;
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
            oin = new RMIObjectInputStream(conn.getInputStream(), true);
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
     * @see RemoteCall.getResultStream(boolean)
     */
    public ObjectOutput getResultStream(boolean success)
            throws IOException, StreamCorruptedException {
        return getOutputStream();
    }

    /**
     * @see RemoteCall.releaseInputStream()
     */
    public void releaseInputStream() throws IOException {
    }

    /**
     * @see RemoteCall.releaseOutputStream()
     */
    public void releaseOutputStream() throws IOException {
        getOutputStream().flush();
        conn.releaseOutputStream();
    }

    /**
     * @see RemoteCall.done()
     */
    public void done() throws IOException {
        if (!isClosed) {
            if (oin.isRemoteCallStream() && oin.isDGCAckNeeded()) {
                // we need to make a DGC ack call
                conn.sendDGCAck(uid == null ? (uid = new UID()) : uid);
            }
            conn.done();
        }
    }

    /**
     * Closes connection/this remote call.
     *
     */
    public void close() {
        conn.close();
        isClosed = true;
    }

    /**
     * @see RemoteCall.executeCall()
     */
    public void executeCall() throws Exception {
        if (isClosed) {
            // rmi.38=Remote call is already closed.
            throw new RemoteException(Messages.getString("rmi.38")); //$NON-NLS-1$
        }
        byte data;

        try {
            // read input
            if ((data = (new DataInputStream(conn.getInputStream())).readByte())
                    != CALL_OK) {
                // rmi.39=Unknown call status response: {0}
                throw new UnmarshalException(Messages.getString("rmi.39", data)); //$NON-NLS-1$
            }

            // read return value id
            getInputStream();
            data = oin.readByte();
        } catch (UnmarshalException re) {
            throw re;
        } catch (IOException ioe) {
            // rmi.3A=Unable to read call return header:
            throw new UnmarshalException(Messages.getString("rmi.3A"), //$NON-NLS-1$
                    ioe);
        }

        if (data != RETURN_VAL && data != RETURN_EX) {
            // rmi.3B=Unknown call result response: {0}
            throw new UnmarshalException(Messages.getString("rmi.3B", data)); //$NON-NLS-1$
        }
        uid = UID.read(oin);

        if (data == RETURN_EX) {
            getExceptionFromServer();
        }
    }

    /**
     * Sets the method for this RemoteCall to the given value.
     * This value will be used later for throwing UnexpectedException.
     *
     * @param m Method this call was created for
     */
    public void setMethod(Method m) {
        this.m = m;
    }

    /**
     * Returns string representation of this RemoteCall.
     *
     * @return string representation of this RemoteCall
     */
    public String toString() {
        return "ClientRemoteCall: " + conn; //$NON-NLS-1$
    }

    /*
     * Reads exception thrown by server from the opened ObjectInputStream,
     * processes it and throws appropriate exception.
     *
     * @throws Exception exception depending on one thrown by the server side
     */
    private void getExceptionFromServer() throws Exception {
        Object obj = null;

        try {
            obj = oin.readObject();
        } catch (IOException ioe) {
            // rmi.3C=IOException occurred while unmarshalling returned exception
            throw new UnmarshalException(Messages.getString("rmi.3C"), ioe); //$NON-NLS-1$
                    
        } catch (ClassNotFoundException cnfe) {
            // rmi.3D=ClassNotFoundException occurred while unmarshalling returned exception
            throw new UnmarshalException(Messages.getString("rmi.3D"), cnfe); //$NON-NLS-1$
        }

        if (obj instanceof Exception) {
            Exception resEx = (Exception) obj;

            if (!(resEx instanceof RuntimeException) && m != null) {
                boolean ok = false;
                Class[] declaredExs = m.getExceptionTypes();

                for (int i = 0; i < declaredExs.length; ++i) {
                    if (declaredExs[i].isAssignableFrom(resEx.getClass())) {
                        ok = true;
                        break;
                    }
                }

                if (!ok) {
                    // rmi.3E=Remote method threw unexpected exception
                    resEx = new UnexpectedException(Messages.getString("rmi.3E"), resEx); //$NON-NLS-1$
                }
            }

            // Add our stacktrace to the stacktrace of exception received
            StackTraceElement[] origST = resEx.getStackTrace();
            StackTraceElement[] curST = (new Exception()).getStackTrace();
            StackTraceElement[] resST =
                    new StackTraceElement[origST.length + curST.length];
            System.arraycopy(origST, 0, resST, 0, origST.length);
            System.arraycopy(curST, 0, resST, origST.length, curST.length);
            resEx.setStackTrace(resST);

            // logs exception from server
            if (UnicastRef.clientCallsLog.isLoggable(RMILog.BRIEF)) {
                UnicastRef.clientCallsLog.log(RMILog.BRIEF,
                        Messages.getString("rmi.log.92", conn), resEx); //$NON-NLS-1$
            }
            throw resEx;
        } else {
            // rmi.3F=Not Exception type thrown: {0}
            throw new UnexpectedException(Messages.getString("rmi.3F", obj)); //$NON-NLS-1$
        }
    }
}
