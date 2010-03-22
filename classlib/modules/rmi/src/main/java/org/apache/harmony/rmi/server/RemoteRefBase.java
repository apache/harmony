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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteRef;

import org.apache.harmony.rmi.transport.Endpoint;
import org.apache.harmony.rmi.transport.RMIObjectInputStream;
import org.apache.harmony.rmi.transport.RMIObjectOutputStream;


/**
 * Base class for all RemoteRef implementations.
 * It belongs to org.apache.harmony.rmi.transport package because it should have
 * package protected access to ClientDGC implementation.
 *
 * @author  Mikhail A. Markov
 */
public abstract class RemoteRefBase implements RemoteRef {

    private static final long serialVersionUID = 358378173612121423L;

    /** Endpoint this handle refers to. */
    protected Endpoint ep;

    /** Object ID of remote object. */
    protected ObjID objId;

    /** True if this handle is for local object. */
    protected boolean isLocal;

    /**
     * Returns Object ID for the referenced remote object.
     *
     * @return Object ID for the referenced remote object
     */
    public ObjID getObjId() {
        return objId;
    }

    /**
     * @see RemoteRef.remoteEquals(RemoteRef)
     */
    public boolean remoteEquals(RemoteRef obj) {
        if (!(obj instanceof RemoteRefBase)) {
            return false;
        }
        RemoteRefBase ref = (RemoteRefBase) obj;
        return ep.equals(ref.ep) && (objId.equals(ref.objId));
    }

    /**
     * @see RemoteRef.remoteToString()
     */
    public String remoteToString() {
        return getRefClass(null) + "[endpoint:[" + ep + "]" //$NON-NLS-1$ //$NON-NLS-2$
                + ((isLocal) ? "(local)" : "(remote)") + ", " + objId + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    /**
     * Returns the value returned by remoteToString() method.
     *
     * @return the value returned by remoteToString() method
     */
    public String toString() {
        return remoteToString();
    }

    /**
     * @see RemoteRef.remoteHashCode()
     */
    public int remoteHashCode() {
        return ((objId != null) ? objId.hashCode() : super.hashCode());
    }

    /**
     * Reads everything left except Endpoint info from the given stream and
     * detects if DGC ack is needed.
     *
     * @param in the stream to read data from
     *
     * @throws IOException if any I/O error occurred
     * @throws ClassNotFoundException if class could not be loaded by current
     *         class loader
     */
    protected void readCommon(ObjectInput in)
            throws IOException, ClassNotFoundException {
        objId = ObjID.read(in);
        boolean needAck = in.readBoolean();

        if (in instanceof RMIObjectInputStream) {
            RMIObjectInputStream oin = (RMIObjectInputStream) in;

            if (oin.isRemoteCallStream()) {
                oin.needDGCAck(needAck);
            }
        }
        RMIObjectInfo info = ExportManager.getInfo(objId);

        if ((info == null) || !info.sref.remoteEquals(this)) {
            /*
             * This remote object created in another VM so
             * register it in ClientDGC.
             */
            ClientDGC.registerForRenew(this);
        }
    }

    /**
     * Writes everything left except Endpoint info to the given stream.
     *
     * @param out the stream to write the object to
     *
     * @throws IOException if any I/O error occurred or class is not serializable
     */
    protected void writeCommon(ObjectOutput out) throws IOException {
        objId.write(out);
        boolean isResStream = false;

        if (out instanceof RMIObjectOutputStream) {
            RMIObjectOutputStream oout = (RMIObjectOutputStream) out;
            isResStream = oout.isResultStream();

            if (isResStream) {
                /*
                 * Because this is a result stream (i.e. obtained in
                 * RemoteCall.getResultStream() method), after writing all
                 * objects we will wait for DGC ack call. So, we should register
                 * this ref for holding a strong reference to the referenced
                 * remote object.
                 */
                ClientDGC.registerForDGCAck(oout.getUID(), this);
            }
        }
        out.writeBoolean(isResStream);
    }
}
