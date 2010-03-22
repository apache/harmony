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
package org.apache.harmony.rmi.activation;

import java.io.BufferedInputStream;
import java.lang.reflect.Constructor;
import java.rmi.MarshalledObject;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.activation.Activatable;
import java.rmi.activation.ActivationDesc;
import java.rmi.activation.ActivationException;
import java.rmi.activation.ActivationGroup;
import java.rmi.activation.ActivationGroupDesc;
import java.rmi.activation.ActivationGroupID;
import java.rmi.activation.ActivationID;
import java.rmi.activation.UnknownObjectException;
import java.rmi.server.RMIClassLoader;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;

import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.transport.RMIObjectInputStream;


/**
 * The implementation of ActivationGroup.
 *
 * @author  Victor A. Martynov
 */
public class ActivationGroupImpl extends ActivationGroup {

    private static final long serialVersionUID = 5526311808915869570L;

    private ActivationGroupID groupID;

    private boolean isGroupActive = true;

    private static RMILog rlog = RMILog.getActivationLog();

    private Hashtable active_objects = new Hashtable();

    /**
     *
     */
    private Class[] special_constructor_parameters = { ActivationID.class,
            MarshalledObject.class };

    /**
     * This main method is used to start new VMs for ActivationGroups. Four
     * parameters needed to create ActivationGroup are: <br>
     * ActivationGroupID <br>
     * ActivationGroupDesc <br>
     * incarnation The parameters needed to create ActivationGroup correctly are
     * passed through the standard input stream in the following order: <br>
     * ActivationGroupID -> ActivationGroupDesc -> incarnation
     */
    public static void main(String args[]) {
        // rmi.log.4C=ActivationGroupImpl.main:
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.4C")); //$NON-NLS-1$
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

        try {
            // rmi.log.4F=System.in.available = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.4F", //$NON-NLS-1$
                    System.in.available()));

            RMIObjectInputStream ois = new RMIObjectInputStream(
                    new BufferedInputStream(System.in));
            // rmi.log.55=ois = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.55", ois)); //$NON-NLS-1$
            ActivationGroupID agid = (ActivationGroupID) ois.readObject();
            // rmi.log.57=agid = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.57", agid)); //$NON-NLS-1$
            ActivationGroupDesc agdesc = (ActivationGroupDesc) ois.readObject();
            // rmi.log.74=agdesc = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.74", agdesc)); //$NON-NLS-1$
            long incarnation = ois.readLong();
            // rmi.log.7B=incarnation = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.7B", incarnation)); //$NON-NLS-1$
            ActivationGroup.createGroup(agid, agdesc, incarnation);
        } catch (Throwable t) {
            // rmi.log.7C=: Exception: {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.7C", t)); //$NON-NLS-1$
            t.printStackTrace();
        }
    }

    /**
     * Method from ActivationInstantiator interface.
     */
    public MarshalledObject newInstance(final ActivationID aid,
            final ActivationDesc adesc) throws ActivationException {

        // rmi.log.83=ActivationGroupImpl: ActivationGroupImpl.newInstance started.
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.83")); //$NON-NLS-1$
        // Checking that we try to activate object in correct group.
        if (!groupID.equals(adesc.getGroupID())) {
            // rmi.36=Attempt to activate object from different group.
            throw new ActivationException(Messages.getString("rmi.36")); //$NON-NLS-1$
        }

        if (isGroupActive == false) {
            // rmi.37=Attempt to activate object in inactive group.
            throw new ActivationException(Messages.getString("rmi.37")); //$NON-NLS-1$
        }

        /**
         */

        ActiveObject ao = (ActiveObject) active_objects.get(aid);
        // rmi.log.84=ActivationGroupImpl: active object = {0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.84", ao)); //$NON-NLS-1$

        if (ao != null) {
            return ao.remote_object_stub;
        }
        try {

            // rmi.log.85=Ready to load active class: [location={0}; name={1}]
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.85", //$NON-NLS-1$
                    adesc.getLocation(), adesc.getClassName()));
            final Class aclass = RMIClassLoader.loadClass(adesc.getLocation(),
                    adesc.getClassName());
            // rmi.log.86=active class = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.86", aclass)); //$NON-NLS-1$

            Remote rmt = (Remote) AccessController
                    .doPrivileged(new PrivilegedExceptionAction() {

                        public Object run() throws Exception {
                            Constructor aconstructor = aclass
                                    .getDeclaredConstructor(special_constructor_parameters);
                            // rmi.log.87=Activatable Constructor: {0}
                            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.87", aconstructor)); //$NON-NLS-1$

                            aconstructor.setAccessible(true);

                            Object parameters[] = new Object[] { aid,
                                    adesc.getData() };
                            return (Remote) aconstructor
                                    .newInstance(parameters);
                        }
                    });

            // rmi.log.88=rmt.getClass = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.88", rmt.getClass())); //$NON-NLS-1$

            // rmi.log.89=newInstance: Remote Object = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.89", rmt)); //$NON-NLS-1$

            ao = new ActiveObject(rmt);
            // rmi.log.91=active object = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.91", ao)); //$NON-NLS-1$

            active_objects.put(aid, ao);
            // rmi.log.8A=ao was put into Hashtable
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.8A")); //$NON-NLS-1$
            // rmi.log.8B=calling newInstance of the superclass.
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.8B")); //$NON-NLS-1$

            super.activeObject(aid, ao.remote_object_stub);
            return ao.remote_object_stub;
        } catch (Throwable t) {
            // rmi.log.44=Exception: {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.44", t), t); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * Constructor
     */
    public ActivationGroupImpl(ActivationGroupID agid, MarshalledObject data)
            throws RemoteException, ActivationException {
        super(agid);
        groupID = agid;
        // rmi.log.8C=ActivationGroup was constructed.
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.8C")); //$NON-NLS-1$
    }

    public boolean inactiveObject(ActivationID id) throws ActivationException,
            UnknownObjectException, RemoteException {
        ActiveObject ao = (ActiveObject) active_objects.get(id);
        if (ao == null) {
            // rmi.93=Object was not registered or already deactivated.
            throw new UnknownObjectException(Messages.getString("rmi.93")); //$NON-NLS-1$
        }

        Activatable.unexportObject(ao.getImpl(), false);
        super.inactiveObject(id);
        active_objects.remove(id);
        return true;
    }

    public void activeObject(ActivationID id, Remote obj)
            throws ActivationException, UnknownObjectException, RemoteException {
        ActiveObject ao = new ActiveObject(obj);
        active_objects.put(id, ao);
        super.activeObject(id, ao.getStub());
    }

    private class ActiveObject {
        Remote remote_object_impl;

        MarshalledObject remote_object_stub;

        ActiveObject(Remote rmt) {

            // rmi.log.8E=ActiveObject: ActiveObject.<init>:
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.8E")); //$NON-NLS-1$ //$NON-NLS-2$
            remote_object_impl = rmt;
            try {

                remote_object_stub = new MarshalledObject(rmt);
                // rmi.log.8F=ActiveObject: remote_object_impl = {0}; remote_object_stub={1}
                rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.8F", //$NON-NLS-1$
                        remote_object_impl, remote_object_stub));
            } catch (Throwable t) {
                // rmi.log.90=ActiveObject: Failed to marshal remote stub: {0}
                rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.90", t)); //$NON-NLS-1$
                t.printStackTrace();
            }
        }

        public Remote getImpl() {
            return remote_object_impl;
        }

        public MarshalledObject getStub() {
            return remote_object_stub;
        }
    }
}
