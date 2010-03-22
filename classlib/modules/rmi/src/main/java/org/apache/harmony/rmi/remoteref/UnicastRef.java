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
package org.apache.harmony.rmi.remoteref;

import java.lang.reflect.Method;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.rmi.MarshalException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteCall;
import java.rmi.server.Operation;

import org.apache.harmony.rmi.client.ClientConnection;
import org.apache.harmony.rmi.client.ClientConnectionManager;
import org.apache.harmony.rmi.client.ClientRemoteCall;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.server.RemoteRefBase;
import org.apache.harmony.rmi.transport.Endpoint;
import org.apache.harmony.rmi.transport.RMIObjectInputStream;
import org.apache.harmony.rmi.transport.RMIObjectOutputStream;
import org.apache.harmony.rmi.transport.RMIProtocolConstants;


/**
 * Implementation of handle for remote objects.
 *
 * @author  Mikhail A. Markov
 */
public class UnicastRef extends RemoteRefBase {

    private static final long serialVersionUID = -2637470287094993242L;

    /** Logger where to write client-side log of remote calls. */
    public static final RMILog clientCallsLog = RMILog.getClientCallsLog();

    // Log where to write server-side log of remote reference activity
    private static final RMILog clientRefLog = RMILog.getClientRefLog();

    /**
     * Default constructor: constructs an empty instance of this class.
     */
    public UnicastRef() {
        ep = null;
        objId = null;
    }

    /**
     * Constructs UnicastRef.
     *
     * @param host host name where remote object impl. is located.
     * @param port port on remote host where remote object accepts RMI calls
     * @param objId Object ID of remoteObject
     */
    public UnicastRef(String host,
                      int port,
                      ObjID objId) {
        this(new Endpoint(host, port, null, null), objId);
    }

    /**
     * Constructs UnicastRef using specified Endpoint and objId.
     *
     * @param ep Endpoint for remote calls
     * @param objId Object ID of remote object
     */
    public UnicastRef(Endpoint ep, ObjID objId) {
        this(ep, objId, false);
    }

    /**
     * Constructs UnicastRef using specified Endpoint and objId.
     *
     * @param ep Endpoint for remote calls
     * @param objId Object ID of remote object
     * @param isLocal if true this UnicastRef belongs to local object
     */
    public UnicastRef(Endpoint ep, ObjID objId, boolean isLocal) {
        this.ep = ep;
        this.objId = objId;
        this.isLocal = isLocal;
    }


    /**
     * @see RemoteRef.invoke(Remote, Method, Object[], long)
     */
    public Object invoke(Remote obj,
                         Method m,
                         Object[] params,
                         long h) throws Exception {
        Object toReturn = null;
        logClientCall(obj, m.toString());

        if (clientRefLog.isLoggable(RMILog.BRIEF)) {
            // rmi.log.100=New call: method = [{0}], hash = {1}
            clientRefLog.log(RMILog.BRIEF, Messages.getString("rmi.log.100", //$NON-NLS-1$
                m, h));
        }

        // initiate a new call
        RemoteCall call = newCall(null, null, -1, h);
        ((ClientRemoteCall) call).setMethod(m);


        try {
            // write arguments for the method called
            RMIObjectOutputStream oout =
                    (RMIObjectOutputStream) call.getOutputStream();
            Class[] paramTypes = m.getParameterTypes();

            try {
                if (params != null) {
                    for (int i = 0; i < params.length; ++i) {
                        oout.writeRMIObject(params[i], paramTypes[i]);
                    }
                }
            } catch (IOException ioe) {
                // rmi.6F=I/O error occurred while marshalling arguments
                throw new MarshalException(Messages.getString("rmi.6F"), ioe); //$NON-NLS-1$
            }

            // execute the call
            invoke(call);

            // read return value
            // if we pass here then server produced no exceptions
            if (m.getReturnType() != Void.TYPE) {
                RMIObjectInputStream oin =
                        (RMIObjectInputStream) call.getInputStream();

                try {
                    toReturn = oin.readRMIObject(m.getReturnType());
                } catch (IOException ioe) {
                    // rmi.70=IOException occurred while unmarshalling return value
                    throw new UnmarshalException(Messages.getString("rmi.70"), ioe); //$NON-NLS-1$
                } catch (ClassNotFoundException cnfe) {
                    // rmi.71=ClassNotFoundException occurred while unmarshalling return value
                    throw new UnmarshalException(Messages.getString("rmi.71"), cnfe); //$NON-NLS-1$
                }
            }
        } catch (IOException ioe) {
            ((ClientRemoteCall) call).close();
            throw ioe;
        }
        done(call);
        return toReturn;
    }

    /**
     * @see RemoteRef.getRefClass(ObjectOutput)
     */
    public String getRefClass(ObjectOutput out) {
        return "UnicastRef"; //$NON-NLS-1$
    }

    /**
     * Writes this UnicastRef object to the specified output stream.
     *
     * @param out the stream to write the object to
     *
     * @throws IOException if any I/O error occurred or class is not serializable
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        ep.writeExternal(out, false);
        writeCommon(out);
    }

    /**
     * Reads data for creating RemoteRef object from the specified input stream.
     *
     * @param in the stream to read data from
     *
     * @throws IOException if any I/O error occurred
     * @throws ClassNotFoundException if class could not be loaded by current
     *         class loader
     */
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException {
        ep = Endpoint.readExternal(in, false);
        readCommon(in);
    }

    /**
     * @see RemoteRef.newCall(RemoteObject, Operation[], int, long)
     */
    public RemoteCall newCall(RemoteObject obj,
                              Operation[] op,
                              int opnum,
                              long hash)
            throws RemoteException {
        if (opnum != -1) {
            if (clientRefLog.isLoggable(RMILog.BRIEF)) {
                // rmi.log.101=New call: method = [{0}], opnum = {1}, hash = {2}
                clientRefLog.log(RMILog.BRIEF, Messages.getString("rmi.log.101", //$NON-NLS-1$
                        new Object[]{op[opnum].toString(), opnum, hash}));
            }
            logClientCall(obj, op[opnum].toString());
        }
        ClientConnection conn = ClientConnectionManager.getConnection(ep);
        RemoteCall call = new ClientRemoteCall(conn);

        if (clientRefLog.isLoggable(RMILog.VERBOSE)) {
            //rmi.log.102=Created new call {0}
            clientRefLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.102", call)); //$NON-NLS-1$
        }

        try {
            // write method signature
            DataOutputStream dout = new DataOutputStream(conn.getOutputStream());
            dout.writeByte(RMIProtocolConstants.CALL_MSG);
            ObjectOutputStream oout =
                    (ObjectOutputStream) call.getOutputStream();
            objId.write(oout);
            oout.writeInt(opnum);
            oout.writeLong(hash);
            return call;
        } catch (IOException ioe) {
            done(call);
            // rmi.72=Unable to marshal call header
            throw new MarshalException(Messages.getString("rmi.72"), ioe); //$NON-NLS-1$
        }
    }

    /**
     * @see RemoteRef.invoke(RemoteCall)
     */
    public void invoke(RemoteCall call) throws Exception {
        if (clientRefLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.103=Execute call {0}
            clientRefLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.103", call)); //$NON-NLS-1$
        }
        call.releaseOutputStream();
        call.executeCall();
    }

    /**
     * @see RemoteRef.done(RemoteCall)
     */
    public void done(RemoteCall call) throws RemoteException {
        if (clientRefLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.104=Finalize call {0}
            clientRefLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.104", call)); //$NON-NLS-1$
        }

        try {
            call.done();
        } catch (Exception ex) {
        }
    }

    // Logs remote method call on client side.
    private void logClientCall(Remote obj, String m) {
        if (clientCallsLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.105=Outbound remote call to endpoint:{0}: method:[{1}], obj:[{2}].
            clientCallsLog.log(RMILog.VERBOSE,Messages.getString("rmi.log.105", //$NON-NLS-1$
                    new Object[]{ ep, m, obj}));
        }
    }
}
