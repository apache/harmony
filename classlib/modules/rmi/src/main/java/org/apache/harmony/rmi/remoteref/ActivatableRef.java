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
 * @author  Victor A. Martynov
 */
package org.apache.harmony.rmi.remoteref;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.Remote;
import java.rmi.StubNotFoundException;
import java.rmi.activation.ActivationDesc;
import java.rmi.activation.ActivationID;
import java.rmi.activation.UnknownObjectException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RemoteRef;
import java.rmi.server.RemoteStub;

import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Activatable ref is the Remote Reference that organizes the next level of indirection during the remote method invocation.
 * It means that ActivatableRef contains another RemoteRef inside that can be 'null' if the ActivatableRef wasn't yet activated or will contain
 * the Remote Reference to the active object.
 *
 * @author  Victor A. Martynov
 *
 * ActivatableRef
 */
public class ActivatableRef extends UnicastRef2 {

    private static final long serialVersionUID = -2842326509564440186L;

    RMILog rlog = RMILog.getActivationLog();

    /**
     * The ActivationID of the Activatable Object of this ActivatableRef.
     */
    protected ActivationID id;

    /**
     *  The internal Remote Reference of this ActivatableRef. This ref is 'null' initially
     *  but when the remote call happens, 'ref' points to the active object.
     */
    protected RemoteRef ref;

    /**
     * Default constructor. It is used mostly in Deserialization.
     */
    public ActivatableRef() {
    }

    /**
     * Special constructor to create ActivatableRef with the given Remote Reference and ActivationID.
     * Used in ActivatableServerRef.
     * @param aid The handle for the Activatable Object.
     * @param ref The internal reference of this ActivatableRef.
     */
    public ActivatableRef(ActivationID aid, RemoteRef ref) {
        this.ref = ref;
        this.id = aid;
    }

    /**
     * Returns the Remote Stub for the given activatable class.
     */
    public static RemoteStub getStub(ActivationDesc desc, ActivationID aid)
            throws StubNotFoundException {

        String cn = desc.getClassName();
        String stubName = ""; //$NON-NLS-1$

        try {
            Class cl = RMIClassLoader.loadClass(desc.getLocation(), cn);
            Class rcl = RMIUtil.getRemoteClass(cl);
            stubName = rcl.getName() + "_Stub"; //$NON-NLS-1$
            Class stubClass = RMIClassLoader.loadClass((String) null, stubName);
            Constructor constructor = stubClass.getConstructor(new Class[] { RemoteRef.class });
            RemoteStub stub = (RemoteStub) constructor.newInstance(new Object[] {
                    new ActivatableRef(aid, null)
            });
            return stub;

        } catch (Exception ex) {
            // rmi.68=Stub {0} not found.
            throw new StubNotFoundException(Messages.getString("rmi.68", stubName), //$NON-NLS-1$ //$NON-NLS-2$
                    ex);
        }
    }


    /**
     * The getRefClass method returns "ActivatableRef" String.
     */
    public String getRefClass(ObjectOutput objectoutput) {
        return "ActivatableRef"; //$NON-NLS-1$
    }

    /**
     * To obtain the description of the Serialization of this class see the Serialized form of
     * java.rmi.server.RemoteObject.
     */
    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeObject(id);

        if (ref == null) {
            out.writeUTF(""); //$NON-NLS-1$
        } else {
            out.writeUTF(ref.getRefClass(out));
            ref.writeExternal(out);
        }
    }

    /**
     * To obtain the description of the Serialization of this class see the Serialized form of
     * java.rmi.server.RemoteObject.
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = (ActivationID) in.readObject();

        String s = in.readUTF();

        if (s.equals("")) { //$NON-NLS-1$
            return;
        }
        Class extRefTypeClass = Class.forName(RemoteRef.packagePrefix +"."+ s); //$NON-NLS-1$

        try {
            ref = (RemoteRef)extRefTypeClass.newInstance();
        }
        catch(Throwable  t) {
            // rmi.73=Instantiation failed.
            throw new ClassNotFoundException(Messages.getString("rmi.73"), t); //$NON-NLS-1$
        }
        ref.readExternal(in);
    }

    /**
     * If the internal remote reference of this ActivatableRef is null, the activatable object is activated using
     * ActivationID.activate() method. After that the remote call is delegated to the ref, by means of calling its 'invoke' method.
     */
    public Object invoke(Remote obj, Method method, Object[] params, long opnum)
            throws Exception {
        Exception signal_exception  = null;
        RemoteRef rref;

        // rmi.log.106=$$$$$$$$$ ActivatableRef.invoke:
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.106")+obj+", "+method+";"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        if(ref == null) {
            // rmi.log.107=ref == null
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.107")); //$NON-NLS-1$

            RemoteStub stub = (RemoteStub)id.activate(false); //ToDo Check whether it returns Remote or RemoteStub
            // rmi.log.3C=Stub = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.3C", stub)); //$NON-NLS-1$

            ActivatableRef aref = (ActivatableRef)stub.getRef();
            // rmi.log.108=aref = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.108", aref)); //$NON-NLS-1$

            ref = aref.ref; // Global variable stored for next calls
            rref = aref.ref; // local variable
        } else {
            rref = ref;
        }

        /*
         * If the group's VM was killed(or something bad happened to it) we may have stale activatable reference to the object.
         * In this case rref.invoke() will throw 3 types of Exceptions: ConnectException, ConnectIOException and UnknownObjectException
         * which should be caught and activation group should be activated again.
         */
        try {
            return rref.invoke(obj, method, params, opnum);
        }
        catch(ConnectException ce) {
        }
        catch(ConnectIOException cioe) {
        }
        catch(UnknownObjectException uoe) {
        }
        catch(Exception t) {
            signal_exception = t;
        }

        // rmi.log.109=signal_exception = {0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.109", signal_exception)); //$NON-NLS-1$

        if (signal_exception == null) {
            RemoteStub stub = (RemoteStub)id.activate(true);
            ActivatableRef aref = (ActivatableRef) stub.getRef();
            ref = aref.ref;
            rref = aref.ref;
            return rref.invoke(obj, method, params, opnum);
        }
        else {
            throw signal_exception;
        }
    }

    /**
     * Standard remoteEquals implementation.
     *
     * @param ref
     *
     * @return
     */
    public boolean remoteEquals(RemoteRef ref) {
        if (ref instanceof ActivatableRef) {
            ActivationID id = ((ActivatableRef)ref).id;
            return this.id.equals(id);
        }
        return false;
    }
}
