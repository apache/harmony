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

package java.rmi.activation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;
import java.rmi.server.UID;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;

public class ActivationID implements Serializable {
    private static final long serialVersionUID = -4608673054848209235L;

    private static final RMILog rlog = RMILog.getActivationLog();

    /**
     * A unique identifier for the object.
     */
    private transient UID uid = new UID();

    /**
     * A remote reference to the object's activator.
     */
    private transient Activator activator;

    public ActivationID(Activator activator) {
        super();
        this.activator = activator;
    }

    public Remote activate(boolean force) throws ActivationException, UnknownObjectException,
            RemoteException {
        // rmi.log.00=ActivationID.activate: activator = {0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.00", activator)); //$NON-NLS-1$
        try {
            MarshalledObject stub = activator.activate(this, force);
            // rmi.log.01=ActivationID.activate:stub={0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.01", stub)); //$NON-NLS-1$
            Remote deserialized_stub = (Remote) stub.get();
            // rmi.log.02=ActivationID.activate: deserialized_stub = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.02", deserialized_stub)); //$NON-NLS-1$
            // rmi.log.03=<<<<<<<<< ActivationID.activate COMPLETED.
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.03")); //$NON-NLS-1$
            return deserialized_stub;
        } catch (IOException ioe) {
            // rmi.0E=An IOException occurred while deserializing the object from its internal representation.
            throw new RemoteException(Messages.getString("rmi.0E")); //$NON-NLS-1$
        } catch (ClassNotFoundException cnfe) {
            // rmi.0F=A ClassNotFoundException occurred while deserializing the object from its internal representation.
            throw new RemoteException(Messages.getString("rmi.0F")); //$NON-NLS-1$
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ActivationID) {
            ActivationID castedObj = (ActivationID) obj;
            boolean p0, p1;
            p0 = uid.equals(castedObj.uid);
            p1 = activator.equals(castedObj.activator);
            return p0 && p1;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return activator.hashCode() ^ uid.hashCode();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // rmi.log.06=ActivationID.readObject:
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.06")); //$NON-NLS-1$
        try {
            uid = (UID) in.readObject();
            // rmi.log.07=UID={0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.07", uid)); //$NON-NLS-1$
            String refType = in.readUTF();
            // rmi.log.08=refType={0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.08", refType)); //$NON-NLS-1$
            Class<?> cl = Class.forName("org.apache.harmony.rmi.remoteref." //$NON-NLS-1$
                    + refType);
            RemoteRef ref = (RemoteRef) cl.newInstance();
            // rmi.log.09=ref = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.09", ref)); //$NON-NLS-1$
            ref.readExternal(in);
            // rmi.log.0A=readExternal finished successfully.
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.0A")); //$NON-NLS-1$
            Class<?> activator_class = RMIClassLoader.loadClass((String) null,
                    "org.apache.harmony.rmi.activation.Rmid_Stub"); //$NON-NLS-1$
            Class[] constructor_parameter_classes = { RemoteRef.class };
            Constructor<?> constructor = activator_class
                    .getConstructor(constructor_parameter_classes);
            Object[] constructor_parameters = { ref };
            activator = (Activator) constructor.newInstance(constructor_parameters);
            // rmi.log.0B=ActivationID.readObject COMPLETED.
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.0B")); //$NON-NLS-1$
        } catch (Throwable t) {
            // rmi.09=Unable to deserialize ActivationID: {0}
            throw new IOException(Messages.getString("rmi.09", t)); //$NON-NLS-1$
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
        // rmi.log.0C=ActivationID.writeObject:
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.0C")); //$NON-NLS-1$
        try {
            out.writeObject(uid);
            // rmi.log.0D=activator = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.0D", activator)); //$NON-NLS-1$
            RemoteRef ref = ((RemoteObject) activator).getRef();
            // rmi.log.09=ref = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.09", ref)); //$NON-NLS-1$
            String refType = ref.getRefClass(out);
            // rmi.log.08=refType={0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.08", refType)); //$NON-NLS-1$
            out.writeUTF(refType);
            ref.writeExternal(out);
            // rmi.log.04=ActivationID.writeObject COMPLETED.
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.04")); //$NON-NLS-1$
        } catch (Throwable t) {
            // rmi.0A=Unable to serialize ActivationID: {0}
            throw new IOException(Messages.getString("rmi.0A", t.getMessage()));//$NON-NLS-1$
        }
    }

    @Override
    public String toString() {
        return "ActivationID: [" + uid + "; " + activator + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
    }
}
