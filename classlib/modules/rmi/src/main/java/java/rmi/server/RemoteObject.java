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
package java.rmi.server;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.MarshalException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.UnmarshalException;

import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.server.ExportManager;


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 */
public abstract class RemoteObject implements Remote, Serializable {

    private static final long serialVersionUID = -3215090123894869218L;

    /** @com.intel.drl.spec_ref */
    protected transient RemoteRef ref;

    /**
     * @com.intel.drl.spec_ref
     */
    protected RemoteObject() {
        ref = null;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    protected RemoteObject(RemoteRef ref) {
        this.ref = ref;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public RemoteRef getRef() {
        return ref;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public String toString() {
        String clName = RMIUtil.getShortName(getClass());
        return (ref == null) ? clName
                : clName + "[" + ref.remoteToString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof RemoteObject)) {
            return (obj == null) ? false : obj.equals(this);
        }

        if (ref != null) {
            return ref.remoteEquals(((RemoteObject) obj).ref);
        }
        return (this == obj);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public int hashCode() {
        return (ref != null) ? ref.remoteHashCode() : super.hashCode();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Remote toStub(Remote obj) throws NoSuchObjectException {
        if (obj instanceof RemoteStub) {
            return (RemoteStub) obj;
        }
        return ExportManager.getStub(obj);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
        if (ref == null) {
            // rmi.17=Invalid remote object: RemoteRef = null
            throw new MarshalException(Messages.getString("rmi.17")); //$NON-NLS-1$
        }
        String refType = ref.getRefClass(out);

        if (refType != null && refType.length() != 0) {
            out.writeUTF(refType);
            ref.writeExternal(out);
        } else {
            out.writeUTF(""); //$NON-NLS-1$
            out.writeObject(ref);
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        String refName = null;

        try {
            refName = in.readUTF();

            if (refName.length() == 0) {
                // read RemoteRef object
                ref = (RemoteRef) in.readObject();
                return;
            }

            // well-known RemoteRef types
            // TODO: the following line is a temporary solution. Line after
            //       that should be uncommented later.
            String refClName = "org.apache.harmony.rmi.remoteref." + refName; //$NON-NLS-1$
            //String refClName = RemoteRef.packagePrefix + "." + refName;
            ref = ((RemoteRef) Class.forName(refClName).newInstance());
            ref.readExternal(in);
        } catch (Exception ex) {
            // rmi.18=Unable to create RemoteRef instance
            throw new UnmarshalException(Messages.getString("rmi.18"), ex);//$NON-NLS-1$
        }
    }
}
