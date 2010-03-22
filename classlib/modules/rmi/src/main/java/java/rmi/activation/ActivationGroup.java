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

import java.lang.reflect.Constructor;
import java.rmi.MarshalledObject;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessController;

import org.apache.harmony.rmi.common.GetStringPropAction;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;

public abstract class ActivationGroup extends UnicastRemoteObject
        implements ActivationInstantiator {
    private static final long serialVersionUID = -7696947875314805420L;

    private static final RMILog rlog = RMILog.getActivationLog();

    /**
     * The ActivationSystem for this VM.
     */
    private static ActivationSystem current_AS;

    /**
     * Current ActivationGroupID
     */
    private static ActivationGroupID current_AGID;

    /**
     * Current ActivationGroup.
     */
    private static ActivationGroup current_AG;

    public static synchronized ActivationGroup createGroup(ActivationGroupID id,
            ActivationGroupDesc desc, long incarnation) throws ActivationException {
        // rmi.log.17=ActivationGroup.createGroup [id={0}, desc={1},
        // incarnation={2}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.17", //$NON-NLS-1$ 
                new Object[] { id, desc, incarnation }));
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkSetFactory();
        }
        /*
         * Classname of the ActivationGroup implementation. If the group class
         * name was given in 'desc' assign it to group_CN, use default
         * otherwise.
         */
        String group_CN = (desc.getClassName() == null) ? "org.apache.harmony.rmi.activation.ActivationGroupImpl" //$NON-NLS-1$
                : desc.getClassName();
        // rmi.log.18=group_CN = {0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.18", group_CN)); //$NON-NLS-1$
        if (current_AG != null) {
            // rmi.11=The ActivationGroup for this VM already exists.
            throw new ActivationException(Messages.getString("rmi.11")); //$NON-NLS-1$
        }
        try {
            // rmi.log.19=Ready to load ActivationGroupImpl class
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.19")); //$NON-NLS-1$
            Class<?> cl = RMIClassLoader.loadClass(desc.getLocation(), group_CN);
            // rmi.log.1A=ag class = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.1A", cl)); //$NON-NLS-1$
            Class[] special_constructor_parameter_classes = { ActivationGroupID.class,
                    MarshalledObject.class };
            Constructor<?> constructor = cl.getConstructor(special_constructor_parameter_classes);
            Object[] constructor_parameters = { id, desc.getData() };
            ActivationGroup ag = (ActivationGroup) constructor
                    .newInstance(constructor_parameters);
            // rmi.log.1B=ag = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.1B", ag)); //$NON-NLS-1$
            current_AS = id.getSystem();
            // rmi.log.1C=current_AS = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.1C", current_AS)); //$NON-NLS-1$
            ag.incarnation = incarnation;
            // rmi.log.1D=ag.incarnation = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.1D", ag.incarnation)); //$NON-NLS-1$
            ag.monitor = current_AS.activeGroup(id, ag, incarnation);
            // rmi.log.1E=ag.monitor = {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.1E", ag.monitor)); //$NON-NLS-1$
            current_AG = ag;
            current_AGID = id;
        } catch (Throwable t) {
            // rmi.log.1F=Exception in createGroup: {0}
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.1F", t)); //$NON-NLS-1$
            // rmi.12=Unable to create group.
            throw new ActivationException(Messages.getString("rmi.12"), t); //$NON-NLS-1$
        }
        // rmi.log.20=Group created: {0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.20", current_AG)); //$NON-NLS-1$
        return current_AG;
    }

    public static synchronized ActivationGroupID currentGroupID() {
        return current_AGID;
    }

    public static synchronized ActivationSystem getSystem() throws ActivationException {
        // rmi.log.21=---------- ActivationGroup.getSystem() ----------
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.21")); //$NON-NLS-1$
        try {
            if (current_AS == null) {
                String port = AccessController.doPrivileged(new GetStringPropAction(
                        "java.rmi.activation.port", //$NON-NLS-1$
                        ActivationSystem.SYSTEM_PORT + "")); //$NON-NLS-1$
                current_AS = (ActivationSystem) Naming.lookup("//:" + port //$NON-NLS-1$
                        + "/java.rmi.activation.ActivationSystem"); //$NON-NLS-1$
                // rmi.log.22=Activation System was got using Naming.lookup() at
                // port {0}
                rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.22", port)); //$NON-NLS-1$
            }
        } catch (Throwable t) {
            // rmi.13=getSystem fails.
            throw new ActivationException(Messages.getString("rmi.13"), t); //$NON-NLS-1$
        }
        // rmi.log.1C=current_AS = {0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.1C", current_AS)); //$NON-NLS-1$

        // rmi.log.23=current_AS.ref = {0}
        if (current_AS instanceof RemoteObject) { 
            rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.23", //$NON-NLS-1$
                    ((RemoteObject) current_AS).getRef()));
        }
        // rmi.log.24=---------- END -> ActivationGroup.getSystem() ----------
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.24")); //$NON-NLS-1$
        return current_AS;
    }

    public static synchronized void setSystem(ActivationSystem system)
            throws ActivationException {
        if (current_AS != null) {
            // rmi.14=The ActivationSystem for this ActivationGroup was already
            // defined.
            throw new ActivationException(Messages.getString("rmi.14")); //$NON-NLS-1$
        }
        current_AS = system;
    }

    static synchronized ActivationGroup getCurrentAG() {
        return current_AG;
    }

    private ActivationGroupID groupID;

    private ActivationMonitor monitor;

    private long incarnation;

    protected ActivationGroup(ActivationGroupID groupID) throws RemoteException {
        /**
         * We need to export this group, so we call the constructor of the
         * superclass.
         */
        super(0);
        this.groupID = groupID;
    }

    protected void activeObject(ActivationID id, MarshalledObject<? extends Remote> mobj)
            throws ActivationException, UnknownObjectException, RemoteException {        
        // rmi.log.14=ActivationGroup.activeObject: {0}; {1}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.14", id, mobj)); //$NON-NLS-1$
        // rmi.log.15=monitor: {0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.15", monitor)); //$NON-NLS-1$
        monitor.activeObject(id, mobj);
        // rmi.log.16=ActivationGroup.activeObject finished.
        rlog.log(RMILog.VERBOSE,Messages.getString("rmi.log.16")); //$NON-NLS-1$
    }

    public abstract void activeObject(ActivationID id, Remote obj)
            throws ActivationException, UnknownObjectException, RemoteException;

    protected void inactiveGroup() throws UnknownGroupException,
            RemoteException {
        monitor.inactiveGroup(groupID, incarnation);
    }

    public boolean inactiveObject(ActivationID id) throws ActivationException,
            UnknownObjectException, RemoteException {
        monitor.inactiveObject(id);
        return true;
    }
}
