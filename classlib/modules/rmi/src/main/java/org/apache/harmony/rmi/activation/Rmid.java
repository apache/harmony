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

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.MarshalException;
import java.rmi.MarshalledObject;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.activation.ActivationDesc;
import java.rmi.activation.ActivationException;
import java.rmi.activation.ActivationGroup;
import java.rmi.activation.ActivationGroupDesc;
import java.rmi.activation.ActivationGroupID;
import java.rmi.activation.ActivationID;
import java.rmi.activation.ActivationInstantiator;
import java.rmi.activation.ActivationMonitor;
import java.rmi.activation.ActivationSystem;
import java.rmi.activation.Activator;
import java.rmi.activation.UnknownGroupException;
import java.rmi.activation.UnknownObjectException;
import java.rmi.activation.ActivationGroupDesc.CommandEnvironment;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteServer;
import java.rmi.server.RemoteStub;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;

import org.apache.harmony.rmi.common.GetBooleanPropAction;
import org.apache.harmony.rmi.common.GetLongPropAction;
import org.apache.harmony.rmi.common.GetStringPropAction;
import org.apache.harmony.rmi.common.RMIConstants;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIProperties;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.remoteref.UnicastServerRef;
import org.apache.harmony.rmi.server.ExportManager;
import org.apache.harmony.rmi.transport.RMIObjectOutputStream;


/**
 * Represents rmid - RMI Activation Daemon. Implements
 * all 3 remote interfaces that are essential for Activation:
 * <code>ActivationSystem</code>, <code>ActivationMonitor</code>
 * and <code>Activator</code>. This is done to avoid
 * the multiplication of references pointing to Hashtables that keep
 * information about ActivationGroupIDs and ActivationIDs and their
 * mappings to ActivationGroupDescriptors and ActivationDescriptors.
 *
 * RMID is partially crash proof, which means it saves its state into two
 * files: snapshot.rmid and delta.rmid. snapshot.rmid contains the
 * snapshot of the structure that contains information about Activation
 * Groups and Activatable Objects registered in this RMID and delta.rmid
 * reflects the changes occurred since last snapshot.
 *
 * The objects that are saved in Snapshot:
 * <UL>
 * <LI>ActivationID: UID uid, String refType, UnicastRef2 RemoteRef</LI>
 * <LI>ActivationDesc: ActivationGroupID groupID, String className, String
 * location, MarshalledObject data, boolean restart</LI>
 * <LI>ActivationGroupID: ActivationSystem system, UID uid</LI>
 * <LI>ActivationGroupDesc: String className, String location,
 * MarshalledObject data, ActivationGroupDesc.CommandEnvironment env,
 * Properties props</LI>
 * </UL>
 *
 * @author  Victor A. Martynov
 */
public class Rmid extends RemoteServer implements ActivationSystem,
        ActivationMonitor, Activator, RmidMBean {

    private static final long serialVersionUID = -4936383024184263236L;

    /**
     * Standard logger for RMI Activation.
     *
     * @see org.apache.harmony.rmi.common.RMILog#getActivationLog()
     */
    private static RMILog rLog = RMILog.getActivationLog();

    /**
     * Internal registry in which the Activation System is registered.
     */
    private static Registry internalRegistry = null;

    /**
     * Port for internal registry.
     *
     * @see java.rmi.activation.ActivationSystem#SYSTEM_PORT
     */
    private static int port = ActivationSystem.SYSTEM_PORT;

    /**
     * The stub for <code>this </code> object.
     */
    private Remote thisStub;

    /**
     * Indicates whether this instance of RMID should start monitor.
     */
    private static boolean startMonitor = false;

    /**
     * The instance of RmidMonitor of this Rmid.
     */
    private static RmidMonitor rmidMonitor = null;

    /**
     * Mapping from ActivationID to its ActivationGroupID.
     */
    public static Hashtable groupIDByActivationID;

    /**
     * Mapping from ActivationGroupID to ActivationGroupInfo.
     */
    static Hashtable groupInfoByGroupId;

    /**
     * The timeout that is given to the ActivationGroup VM to
     * start(milliseconds).
     *
     * @see org.apache.harmony.rmi.common.RMIConstants#DEFAULT_ACTIVATION_EXECTIMEOUT
     * @see org.apache.harmony.rmi.common.RMIProperties#ACTIVATION_EXECTIMEOUT_PROP
     */
    private static long groupStartTimeout =
        RMIConstants.DEFAULT_ACTIVATION_EXECTIMEOUT;

    /**
     * Represents the interval between the snapshots of the RMID
     * current state.
     *
     * @see org.apache.harmony.rmi.common.RMIConstants#DEFAULT_SNAPSHOTINTERVAL
     * @see org.apache.harmony.rmi.common.RMIProperties#ACTIVATION_SNAPSHOTINTERVAL_PROP
     */
    private static long snapshotInterval =
        RMIConstants.DEFAULT_SNAPSHOTINTERVAL;

    /**
     * Indicates whether the debug information about logging - snapshots and
     * deltas of the RMID state - should be printed.
     *
     * @see org.apache.harmony.rmi.common.RMIProperties#ACTIVATION_LOG_DEBUG_PROP
     */
    private static boolean loggingDebug = false;

    /**
     * The maximum amount of activation groups(VMs).
     *
     * @see org.apache.harmony.rmi.common.RMIConstants#MAX_CONCURRENT_STARTING_GROUPS
     * @see org.apache.harmony.rmi.common.RMIProperties#MAXSTARTGROUP_PROP
     */
    private static long maxConcurrentStartingGroups =
        RMIConstants.MAX_CONCURRENT_STARTING_GROUPS;

    /**
     * Indicates whether common activation debug information should be printed.
     * @see org.apache.harmony.rmi.common.RMIProperties#ACTIVATION_DEBUGEXEC_PROP
     */
    private static boolean activationDebug;

    /**
     * Arguments passed to every activation group VM of this Activation System.
     * These arguments are passed in the RMID command line using "-C" option.
     */
    private static String[] groupArgs = null;

    /**
     * Represents the amount of deltas that
     * happened after last snapshot. When this value exceeds
     * snapshotInterval the snapshot is made and this
     * variable is reset to 0.
     */
    private static int deltasCounter = 0;

    /**
     * The flag that indicates that the RMID is in restore phase. No
     * changes that are made to the RMID database during this flag is set
     * are recorded in DELTA_FILE.
     */
    private static boolean restoreLock = false;

    /**
     * This variable represents the amount of groups that can be started
     * immediately. The initial value of this variable is
     * maxConcurrentStartingGroups and when a process of group start is
     * initiated this value is decremented. As soon as group finishes its
     * starting procedures the value is increased.
     */
    private static long startingGroups = maxConcurrentStartingGroups;

    /**
     * The log level of RMID persistence state activities.
     */
    private static Level persistenceDebugLevel = RMILog.SILENT;

    /**
     * The log level of the general debugging information.
     */
    public static Level commonDebugLevel = RMILog.SILENT;

    /**
     * The folder to hold RMID logging information: snapshot and delta files.
     *
     * @see org.apache.harmony.rmi.common.RMIConstants#DEFAULT_LOG_FOLDER
     */
    private static String logFolder = RMIConstants.DEFAULT_LOG_FOLDER;

    private class Lock {}
    private Object lock = new Lock();

    /**
     * The name of the monitor class for RMID.
     *
     * @see RmidMonitor
     * @see org.apache.harmony.rmi.common.RMIConstants#DEFAULT_ACTIVATION_MONITOR_CLASS_NAME
     * @see org.apache.harmony.rmi.common.RMIProperties#ACTIVATION_MONITOR_CLASS_NAME_PROP
     */
    static String monitorClassName;

    /**
     * Initializes activation system. Called in {@link #main(String[]) main}.
     */
    private Rmid(int port) {
        try {
            /*
             * The process of starting RMID should not be interrupted by any
             * incoming calls so we put this whole process into synchronized
             * block on the global lock object.
             */
            synchronized (lock) {
                internalRegistry = LocateRegistry.createRegistry(port);
                // rmi.log.38=Registry created: {0}
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.38", //$NON-NLS-1$
                        internalRegistry));
                // rmi.log.39=Creating Activation System on port {0}.
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.39", //$NON-NLS-1$
                        port));

                UnicastServerRef usr = new UnicastServerRef(port, null,
                        null, new ObjID(RMIConstants.ACTIVATION_SYSTEM_ID));

                thisStub = ExportManager.exportObject(this, usr, false);
                // rmi.log.3A=stub's ref = {0}
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.3A", //$NON-NLS-1$
                        ((RemoteObject) thisStub).getRef()));
                this.ref = ((RemoteStub) thisStub).getRef();

                String activationSystemURL = "rmi://:" + port //$NON-NLS-1$
                        + "/java.rmi.activation.ActivationSystem"; //$NON-NLS-1$
                // rmi.log.3B=URL = {0}
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.3B", activationSystemURL)); //$NON-NLS-1$
                // rmi.log.3C=Stub = {0}
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.3C", thisStub)); //$NON-NLS-1$
                Naming.rebind(activationSystemURL, thisStub);
                // rmi.log.3D=Rebind was successful.
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.3D")); //$NON-NLS-1$

                groupIDByActivationID = new Hashtable();
                groupInfoByGroupId = new Hashtable();

                if (startMonitor) {
                    rmidMonitor = RmidMonitorFactory
                            .getRmidMonitor(monitorClassName);
                    // rmi.log.3E=RmidMonitor created: {0}
                    rLog.log(commonDebugLevel, Messages.getString("rmi.log.3E", //$NON-NLS-1$
                            rmidMonitor));
                    /*
                     * Failed to obtain RmidMonitor.
                     */
                    if (rmidMonitor == null) {
                        startMonitor = false;
                    }
                }

                restore();
            }
        } catch (Throwable t) {
            // rmi.log.3F=Exception in RMID: {0}
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.3F", t)); //$NON-NLS-1$
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Waits until the startup procedure of RMID is completed.
     */
    private void waitStartup() {
        synchronized (lock) {
            //This block was intentionally left empty.
        }
    }

    /**
     * Main method to start activation system.
     *
     * @see RMIConstants#RMID_USAGE
     */
    public static void main(String args[]) {

        /* Setting the security manager. */
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

        /*
         * Reading properties.
         */
        groupStartTimeout = ((Long) AccessController
                .doPrivileged(new GetLongPropAction(
                        RMIProperties.ACTIVATION_EXECTIMEOUT_PROP,
                        groupStartTimeout))).longValue();
        snapshotInterval = ((Long) AccessController
                .doPrivileged(new GetLongPropAction(
                        RMIProperties.ACTIVATION_SNAPSHOTINTERVAL_PROP,
                        snapshotInterval))).longValue();
        loggingDebug = ((Boolean) AccessController
                .doPrivileged(new GetBooleanPropAction(
                        RMIProperties.ACTIVATION_LOG_DEBUG_PROP,
                        loggingDebug))).booleanValue();
        maxConcurrentStartingGroups = ((Long) AccessController
                .doPrivileged(new GetLongPropAction(
                        RMIProperties.MAXSTARTGROUP_PROP,
                        maxConcurrentStartingGroups))).longValue();
        activationDebug = ((Boolean) AccessController
                .doPrivileged(new GetBooleanPropAction(
                        RMIProperties.ACTIVATION_DEBUGEXEC_PROP, false)))
                .booleanValue();
        monitorClassName = (String) AccessController
                .doPrivileged(new GetStringPropAction(
                        RMIProperties.ACTIVATION_MONITOR_CLASS_NAME_PROP,
                        RMIConstants.DEFAULT_ACTIVATION_MONITOR_CLASS_NAME));

        if (loggingDebug) {
            persistenceDebugLevel = RMILog.VERBOSE;
        }

        if (activationDebug) {
            commonDebugLevel = RMILog.VERBOSE;
        }
        // rmi.log.40=\nThe following properties were set on RMID:
        rLog.log(commonDebugLevel,
                Messages.getString("rmi.log.40") + "\n" //$NON-NLS-1$ //$NON-NLS-2$
                        + RMIProperties.ACTIVATION_EXECTIMEOUT_PROP
                        + " = " + groupStartTimeout + "\n" //$NON-NLS-1$ //$NON-NLS-2$
                        + RMIProperties.ACTIVATION_SNAPSHOTINTERVAL_PROP
                        + " = " + snapshotInterval + "\n" //$NON-NLS-1$ //$NON-NLS-2$
                        + RMIProperties.ACTIVATION_LOG_DEBUG_PROP + " = " //$NON-NLS-1$
                        + loggingDebug + "\n" //$NON-NLS-1$
                        + RMIProperties.MAXSTARTGROUP_PROP + " = " //$NON-NLS-1$
                        + maxConcurrentStartingGroups + "\n" //$NON-NLS-1$
                        + RMIProperties.ACTIVATION_DEBUGEXEC_PROP + " = " //$NON-NLS-1$
                        + activationDebug + "\n" //$NON-NLS-1$
                        + RMIProperties.ACTIVATION_MONITOR_CLASS_NAME_PROP
                        + " = " + monitorClassName); //$NON-NLS-1$

        /*
         * The ArrayList for temporary holding of "-C" options.
         */
        ArrayList tmpGroupArgs = new ArrayList();

        /*
         * Parsing command line arguments.
         */
        for (int i = 0; i < args.length; i++) {

            String argument = args[i];

            if (argument.equals("-port")) { //$NON-NLS-1$
                if (i + 1 >= args.length) {
                    // rmi.console.02=Insufficient arguments: port should be specified.
                    System.out.println(Messages.getString("rmi.console.02")); //$NON-NLS-1$
                    printUsage();
                    System.exit(1);
                }

                try {
                    port = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException nfe) {
                    // rmi.console.03=Malformed port number.
                    System.out.println(Messages.getString("rmi.console.03")); //$NON-NLS-1$
                    printUsage();
                    System.exit(1);
                }
                i++;
            } else if (argument.equals("-log")) { //$NON-NLS-1$
                if (i + 1 >= args.length) {
                    // rmi.console.04=Insufficient arguments: log folder should be specified.
                    System.out.println(Messages.getString("rmi.console.04")); //$NON-NLS-1$
                    printUsage();
                    System.exit(1);
                }
                logFolder = args[i + 1];
                i++;
            } else if (argument.equals("-stop")) { //$NON-NLS-1$
                try {
                    ActivationSystem system = ActivationGroup.getSystem();
                    system.shutdown();
                    // rmi.log.41=RMID was shut down
                    rLog.log(commonDebugLevel, Messages.getString("rmi.log.41")); //$NON-NLS-1$
                    return;
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(1);
                }
            } else if (argument.equals("-C")) { //$NON-NLS-1$
                tmpGroupArgs.add(args[i].substring(2));
            } else if (argument.equals("-help")) { //$NON-NLS-1$
                printUsage();
                return;
            } else if (argument.equals("-monitor")) { //$NON-NLS-1$
                // rmi.log.42=Monitor option selected.
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.42")); //$NON-NLS-1$
                startMonitor = true;
            } else {
                /*
                 * Illegal option found.
                 */
                // rmi.console.05=Illegal option: {0}
                System.out.println(Messages.getString("rmi.console.05", argument)); //$NON-NLS-1$
                printUsage();
                System.exit(1);
            }
        }

        /*
         * Extracting collected "-C" options from ArrayList.
         */
        groupArgs = (String[]) tmpGroupArgs
                .toArray(new String[tmpGroupArgs.size()]);

        /*
         * Adding separator at the end of log folder.
         */
        if (!logFolder.endsWith(File.separator)) {
            logFolder = logFolder + File.separator;
        }

        final File dir = new File(logFolder);

        /*
         * Creating log folder.
         */
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                if (!dir.exists() && !dir.mkdir()) {
                    //rmi.console.06=Cannot create log folder: {0}
                    System.out.println(Messages.getString("rmi.console.06", //$NON-NLS-1$
                            logFolder));
                    System.exit(1);
                }
                return null;
            }
        });

        try {
            Rmid rmid = new Rmid(port);
            
            // rmi.log.43=RMID instance created: {0} 
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.43", rmid)); //$NON-NLS-1$
            Thread.sleep(Long.MAX_VALUE);
        } catch (Throwable t) {
            // rmi.log.44=Exception: {0}
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.44", t)); //$NON-NLS-1$
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Prints the usage syntax for RMID.
     */
    private static void printUsage() {
        System.out.println(RMIConstants.RMID_USAGE);
    }

    /* *********************************************************************
     *
     * Next methods belong to ActivationSystem remote interface.
     *
     *********************************************************************/

    public ActivationGroupID registerGroup(ActivationGroupDesc agdesc)
            throws ActivationException {
        waitStartup();
        ActivationGroupID agid = new ActivationGroupID(this);
        ActivationGroupInfo agi = new ActivationGroupInfo(agid, agdesc);
        if (groupInfoByGroupId.containsKey(agid)) {
            // rmi.2E=This group is already registered.
            throw new ActivationException(Messages.getString("rmi.2E")); //$NON-NLS-1$
        }
        groupInfoByGroupId.put(agid, agi);
        if (!restoreLock) {
            writeDelta(Delta.PUT, "group", agid, agdesc); //$NON-NLS-1$
            // rmi.log.45=Delta was saved:
            rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.45") //$NON-NLS-1$
                    + Delta.PUT + "," + "group" + ", " + agid + ", " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    + agdesc);
        }

        return agid;
    }

    public ActivationMonitor activeGroup(ActivationGroupID gID,
            ActivationInstantiator group, long incarnation)
            throws UnknownGroupException, ActivationException {
        waitStartup();
        // rmi.log.46=Rmid.activeGroup: {0}, {1}, {2}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.46", //$NON-NLS-1$
                new Object[]{gID, group, incarnation}));
        // rmi.log.47=groupID2groupInfo_H = {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.47", //$NON-NLS-1$
                groupInfoByGroupId));

        ActivationGroupInfo agi = (ActivationGroupInfo) groupInfoByGroupId
                .get(gID);
        // rmi.log.48=Rmid.activeGroup group info =  {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.48", agi));//$NON-NLS-1$
                        

        if (agi == null) {
            // rmi.2F=Group is not registered: {0}
            throw new UnknownGroupException(Messages.getString("rmi.2F", gID)); //$NON-NLS-1$
        } else if (agi.isActive()) {
            // rmi.30=Group is already active: {0}
            throw new ActivationException(Messages.getString("rmi.30", gID)); //$NON-NLS-1$
        }

        // rmi.log.49=ready to execute agi.active()
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.49")); //$NON-NLS-1$

        agi.active(group, incarnation);
        // rmi.log.4A=Rmid.activeGroup finished.
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.4A")); //$NON-NLS-1$

        return this;
    }

    /**
     * This method is absent in Java Remote Method Invocation
     * specification.
     *
     * @param aID
     * @throws UnknownObjectException  if <code>ActivationID</code>
     * is not registered
     * @throws ActivationException  for general failure
     * @throws RemoteException  if remote call fails
     */
    public ActivationDesc getActivationDesc(ActivationID aID)
            throws UnknownObjectException {
        waitStartup();
        ActivationGroupID agid = (ActivationGroupID) groupIDByActivationID
                .get(aID);
        ActivationGroupInfo info = (ActivationGroupInfo) groupInfoByGroupId
                .get(agid);
        ActivationDesc adesc = info.getActivationDesc(aID);

        if (adesc == null) {
            // rmi.31=No ActivationDesc for ActivationID {0}
            throw new UnknownObjectException(Messages.getString("rmi.31", aID)); //$NON-NLS-1$
        }
        return adesc;
    }

    /**
     * This method is absent in Java Remote Method Invocation
     * specification.
     *
     * @throws UnknownGroupException - if agid is not registered
     * @throws ActivationException - for general failure
     * @throws RemoteException - if remote call fails
     */
    public ActivationGroupDesc getActivationGroupDesc(
            ActivationGroupID agid) throws UnknownObjectException {
        waitStartup();
        ActivationGroupInfo agi = (ActivationGroupInfo) groupInfoByGroupId
                .get(agid);
        if (agi == null) {
            // rmi.32=No ActivationGroupDesc for ActivationGroupID {0}
            throw new UnknownObjectException(Messages.getString("rmi.32", agid)); //$NON-NLS-1$
        }
        return agi.getActivationGroupDesc();
    }

    public ActivationID registerObject(ActivationDesc adesc) {
        waitStartup();
        // rmi.log.4B=ActivationSystemImpl.registerObject():
        rLog.log(commonDebugLevel,Messages.getString("rmi.log.4B")); //$NON-NLS-1$
        ActivationGroupID agid = adesc.getGroupID();
        // rmi.log.4C=agid : {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.4C", agid)); //$NON-NLS-1$
        // rmi.log.4D=Activator stub = {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.4D", thisStub)); //$NON-NLS-1$
        ActivationID aid = new ActivationID((Activator) thisStub);
        // rmi.log.4E=aid : {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.4E", aid)); //$NON-NLS-1$
        // rmi.log.4C=agid : {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.4C", agid)); //$NON-NLS-1$

        ActivationGroupInfo info = (ActivationGroupInfo) groupInfoByGroupId
                .get(agid);
        // rmi.log.50=ActivationGroupInfo = {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.50", info)); //$NON-NLS-1$

        info.registerObject(aid, adesc);
        // rmi.log.51=Activation desc was added.
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.51")); //$NON-NLS-1$
        return aid;

    }

    public ActivationDesc setActivationDesc(ActivationID id,
            ActivationDesc desc) {
        waitStartup();
        ActivationGroupID agid = (ActivationGroupID) groupIDByActivationID
                .get(id);
        ActivationGroupInfo agi = (ActivationGroupInfo) groupInfoByGroupId
                .get(agid);
        return agi.setActivationDesc(id, desc);
    }

    public ActivationGroupDesc setActivationGroupDesc(
            ActivationGroupID id, ActivationGroupDesc desc) {
        waitStartup();
        ActivationGroupInfo agi = (ActivationGroupInfo) groupInfoByGroupId
                .get(id);
        return agi.setActivationGroupDesc(id, desc);
    }

    public void shutdown() {
        synchronized (lock) {
            // rmi.log.52=The Rmid is going to shutdown
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.52")); //$NON-NLS-1$

            Enumeration enumeration = groupInfoByGroupId.elements();
            while (enumeration.hasMoreElements()) {
                try {
                    ActivationGroupInfo agi = (ActivationGroupInfo) enumeration
                            .nextElement();
                    agi.shutdown();
                } catch (Throwable t) {
                    // rmi.log.53=Exception in Rmid.shutdown: {0}
                    rLog.log(commonDebugLevel,Messages.getString("rmi.log.53", t)); //$NON-NLS-1$
                    t.printStackTrace();
                }
            }
            // rmi.log.54=...... Done.
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.54")); //$NON-NLS-1$
            System.exit(0);
        }
    }

    public void unregisterGroup(ActivationGroupID id)
            throws ActivationException, UnknownGroupException,
            RemoteException {
        waitStartup();
        ActivationGroupInfo agi = (ActivationGroupInfo) groupInfoByGroupId
                .remove(id);
        if (agi == null) {
            // rmi.34=Attempt to unregister unknown group {0}
            throw new UnknownGroupException(Messages.getString("rmi.34", id)); //$NON-NLS-1$
        }
        agi.unregister();
    }

    /**
     * @param aID the ActivationID of the object that should be removed.
     */
    public void unregisterObject(ActivationID aID) {
        waitStartup();
        ActivationGroupID gID = (ActivationGroupID) groupIDByActivationID
                .get(aID);
        ActivationGroupInfo gInfo = (ActivationGroupInfo) groupInfoByGroupId
                .get(gID);
        gInfo.unregisterObject(aID);
    }

    /* *********************************************************************
     *
     * Next methods belong to ActivationMonitor remote interface.
     *
     *********************************************************************/

    public void activeObject(ActivationID id, MarshalledObject obj)
            throws RemoteException, UnknownObjectException {
        waitStartup();

        // rmi.log.56=Rmid.activeObject: {0}; {1}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.56", //$NON-NLS-1$
                id, obj));
        ActivationGroupID agid = (ActivationGroupID) groupIDByActivationID
                .get(id);
        // rmi.log.57=agid = {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.57", agid)); //$NON-NLS-1$

        ActivationGroupInfo agi = (ActivationGroupInfo) groupInfoByGroupId
                .get(agid);
        // rmi.log.58=agi = {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.58", agi)); //$NON-NLS-1$

        ObjectInfo oi = (ObjectInfo) agi.objectInfoByActivationID.get(id);
        // rmi.log.59=oi= {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.59", oi)); //$NON-NLS-1$

        oi.active();
        // rmi.log.5A=Rmid.activeObject finished.
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.5A")); //$NON-NLS-1$
    }

    public void inactiveGroup(ActivationGroupID id, long incarnation) {
        waitStartup();

        ActivationGroupInfo agi = (ActivationGroupInfo) groupInfoByGroupId
                .get(id);
        agi.inactive(incarnation);
    }

    public void inactiveObject(ActivationID aID) {
        waitStartup();
        ActivationGroupID gID = (ActivationGroupID) groupIDByActivationID
                .get(aID);
        ActivationGroupInfo gInfo = (ActivationGroupInfo) groupInfoByGroupId
                .get(gID);
        gInfo.inactiveObject(aID);
    }

    /* *********************************************************************
     *
     * Next methods belong to Activator remote interface.
     *
     *********************************************************************/

    public MarshalledObject activate(ActivationID id, boolean force)
            throws ActivationException, UnknownObjectException,
            RemoteException {
        waitStartup();
        // rmi.log.5B=ActivatorImpl.activate({0}; {1})
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.5B", //$NON-NLS-1$
                id, force));

        ActivationGroupID agid = (ActivationGroupID) groupIDByActivationID
                .get(id);
        // rmi.log.57=agid = {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.57", agid)); //$NON-NLS-1$
        ActivationGroupInfo info = (ActivationGroupInfo) groupInfoByGroupId
                .get(agid);
        // rmi.log.5C=info = {0}
        rLog.log(commonDebugLevel, Messages.getString("rmi.log.5C", info)); //$NON-NLS-1$

        return info.activateObject(id, force);
    }

    /**
     * This class holds all the information needed about ActivationGroup.
     * It contains the following information: ActivationGroupID
     * ActivationGroupDesc and the array of activatable objects within this
     * group. The ActivationSystem holds the array of such objects.
     */
    private class ActivationGroupInfo {

        private ActivationGroupID agid;

        private ActivationGroupDesc agdesc;

        private Hashtable objectInfoByActivationID;

        private long incarnation;

        private boolean isActive;

        private ActivationInstantiator activationInstantiator;

        private Process process;

        public ActivationGroupInfo(ActivationGroupID agid,
                ActivationGroupDesc agdesc) {
            this.agdesc = agdesc;
            this.agid = agid;
            objectInfoByActivationID = new Hashtable();
            incarnation = 0;
            isActive = false;

            if (startMonitor) {
                rmidMonitor.addGroup(agid);
            }
        }

        public synchronized void inactiveObject(ActivationID aID) {
            ObjectInfo oi = (ObjectInfo) objectInfoByActivationID.get(aID);
            oi.inactive();
        }

        public synchronized void unregisterObject(ActivationID aID) {
            objectInfoByActivationID.remove(aID);
            groupIDByActivationID.remove(aID);

            if (startMonitor) {
                rmidMonitor.removeObject(aID);
            }
        }

        public synchronized ActivationDesc setActivationDesc(
                ActivationID id, ActivationDesc desc) {
            ObjectInfo oi = (ObjectInfo) objectInfoByActivationID.get(id);
            ActivationDesc oldDesc = oi.getActivationDesc();
            oi.setActivationDesc(desc);
            return oldDesc;
        }

        public synchronized ActivationGroupDesc setActivationGroupDesc(
                ActivationGroupID id, ActivationGroupDesc desc) {
            ActivationGroupDesc oldDesc = agdesc;
            agdesc = desc;
            return oldDesc;
        }

        public synchronized void registerObject(ActivationID id,
                ActivationDesc desc) {

            groupIDByActivationID.put(id, agid);

            ObjectInfo oi = new ObjectInfo(id, desc);

            objectInfoByActivationID.put(id, oi);

            if (!restoreLock) {
                writeDelta(Delta.PUT, "object", id, desc); //$NON-NLS-1$
                // rmi.log.5D=New delta was generated.
                rLog.log(persistenceDebugLevel, Messages
                        .getString("rmi.log.5D")); //$NON-NLS-1$
            }
        }

        public synchronized MarshalledObject activateObject(
                ActivationID id, boolean force)
                throws ActivationException, RemoteException {
            
            // rmi.log.5E=GroupInfo: id={0}; force={1}
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.5E", //$NON-NLS-1$
                id, force));

            if (!isActive) {
                activateGroup();
                // rmi.log.5F=Group was activated.
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.5F")); //$NON-NLS-1$
            } else {
                // rmi.log.60=Group was reused.
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.60")); //$NON-NLS-1$
            }

            ObjectInfo oi = (ObjectInfo) objectInfoByActivationID.get(id);
            // rmi.log.61=activation_instantiator = {0}
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.61", //$NON-NLS-1$
                    activationInstantiator));

            Exception signalException = null;
            try {
                return oi.activate(activationInstantiator);
            } catch (ConnectException ce) {
            } catch (ConnectIOException cioe) {
            } catch (MarshalException me) {
            } catch (Exception e) {
                signalException = e;
            }

            if (signalException == null) {
                // rmi.log.62=The group seems to be dead: Killing process, reactivating group.
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.62")); //$NON-NLS-1$

                if (process != null) {
                    process.destroy();
                }
                isActive = false;
                activationInstantiator = null;
                activateGroup();
                return oi.activate(activationInstantiator);
            }
            // rmi.35=Exception:
            throw new ActivationException(Messages.getString("rmi.35"), signalException); //$NON-NLS-1$
        }

        public synchronized void activateGroup() {

            /*
             * Constructing an appropriate commandline to start activation
             * group.
             *
             */
            String args[];
            ArrayList al = new ArrayList();
            CommandEnvironment ce = agdesc.getCommandEnvironment();

            if (ce != null) {

                String[] options = ce.getCommandOptions();
                String cmd = ce.getCommandPath();
                if (cmd != null) {
                    al.add(cmd);
                } else {
                    al.add("java"); //$NON-NLS-1$
                }
                al.addAll(Arrays.asList(options));
            } else {

                /*
                 * Getting properties that affect group VM execution.
                 */
                String javaVmNameVal = (String) AccessController.doPrivileged(
                        new GetStringPropAction("java.vm.name")); //$NON-NLS-1$

                String javaHomeVal = (String) AccessController.doPrivileged(
                        new GetStringPropAction("java.home")); //$NON-NLS-1$

                String javaClassPathVal = (String) AccessController.doPrivileged(
                        new GetStringPropAction("java.class.path")); //$NON-NLS-1$

                String policy = (String) AccessController.doPrivileged(
                        new GetStringPropAction("java.security.policy")); //$NON-NLS-1$

                String bootClassPathVal = (String)
                        AccessController.doPrivileged(new GetStringPropAction(
                            (javaVmNameVal.equals("J9") //$NON-NLS-1$
                                           ? "org.apache.harmony.boot.class.path" //$NON-NLS-1$
                                            : "sun.boot.class.path"))); //$NON-NLS-1$

                String executable = new File(new File(
                        javaHomeVal, "bin"), "java").getPath(); //$NON-NLS-1$ //$NON-NLS-2$

                // Add name of Java executable to run.
                al.add(executable);

                if (bootClassPathVal != null) {
                    al.add("-Xbootclasspath:" + bootClassPathVal); //$NON-NLS-1$
                }

                if (javaClassPathVal != null) {
                    al.add("-classpath"); //$NON-NLS-1$
                    al.add(javaClassPathVal);
                }

                if (policy != null) {
                    // Apply security policy.
                    al.add("-Djava.security.policy=" + policy); //$NON-NLS-1$
                }
            }

            /*
             * Passing the "-C" options to the ActivationGroup VM.
             */
            for (int i = 0; i < groupArgs.length; i++) {
                // rmi.log.63=Option was passed through '-C': {0}
                rLog.log(commonDebugLevel,Messages.getString("rmi.log.63", //$NON-NLS-1$
                                groupArgs[i]));
                al.add(groupArgs[i]);
            }

            al.add("org.apache.harmony.rmi.activation.ActivationGroupImpl"); //$NON-NLS-1$
            args = (String[]) al.toArray(new String[al.size()]);
            // rmi.log.64=args = {0} 
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.64", Arrays.asList(args))); //$NON-NLS-1$

            try {
                final String[] argsLocal = args;
                process = (Process) AccessController
                        .doPrivileged(new PrivilegedExceptionAction() {
                            public Object run() throws IOException {
                                return Runtime.getRuntime()
                                        .exec(argsLocal);
                            }
                        });

                startingGroups--;
                InputStream in = process.getInputStream();
                InputStream err = process.getErrorStream();

                new DebugThread(in).start();
                new DebugThread(err).start();

                // rmi.log.65=ActivationGroup started: {0}
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.65", //$NON-NLS-1$
                        process));
                incarnation++;

                OutputStream os = process.getOutputStream();
                RMIObjectOutputStream oos = new RMIObjectOutputStream(
                        new BufferedOutputStream(os));

                oos.writeObject(agid);

                // rmi.log.66=Agid written: {0}
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.66", agid)); //$NON-NLS-1$
                oos.writeObject(agdesc);
                // rmi.log.67=Agdesc written: {0}
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.67", agdesc)); //$NON-NLS-1$

                oos.writeLong(incarnation);
                // rmi.log.68=incarnation written: {0}
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.68", //$NON-NLS-1$
                        incarnation));

                oos.flush();
                // rmi.log.69=flushed
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.69")); //$NON-NLS-1$

                oos.close();
                os.close();
                // rmi.log.6A=closed
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.6A")); //$NON-NLS-1$

                if (activationInstantiator == null) {
                    try {
                        this.wait(groupStartTimeout);
                    } catch (InterruptedException t) {
                    }
                }
                startingGroups++;
            } catch (Throwable t) {
                // rmi.log.6B=Cannot start ActivationGroup.\n Exception: {0}
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.6B", t)); //$NON-NLS-1$
                t.printStackTrace();
            }
        }

        /**
         * Callback from ActivationGroup.createGroup that the groups was
         * created.
         */
        public synchronized void active(ActivationInstantiator ai,
                long incarnation) {
            // rmi.log.6C=ActivationGroupInfo.activeGroup[ActInst={0}; incarn={1}]
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.6C", ai, //$NON-NLS-1$
                    incarnation));
            activationInstantiator = ai;
            notify();

            if (this.incarnation != incarnation) {
                // rmi.33=Different incarnations of this group happened.
                throw new RuntimeException(Messages.getString("rmi.33")); //$NON-NLS-1$
            }
            activationInstantiator = ai;
            isActive = true;

            if (startMonitor) {
                rmidMonitor.activeGroup(agid);
            }
            // rmi.log.6D=ActivationGroupInfo.activeGroup finished.
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.6D")); //$NON-NLS-1$
        }

        public ActivationGroupDesc getActivationGroupDesc() {
            return agdesc;
        }

        public boolean isActive() {
            return isActive;
        }

        public ActivationDesc getActivationDesc(ActivationID aid) {
            return (ActivationDesc) ((ObjectInfo) objectInfoByActivationID
                    .get(aid)).getActivationDesc();
        }

        /**
         * Shut the activation group down.
         *
         * @return The exit value of activation group's VM.
         */
        public synchronized int shutdown() {
            if (process != null) {
                process.destroy();
                int val = process.exitValue();
                process = null;
                return val;
            }
            return 0;
        }

        public synchronized void unregister() {
            Enumeration keys = objectInfoByActivationID.keys();

            while (keys.hasMoreElements()) {
                ActivationID id = (ActivationID) keys.nextElement();

                objectInfoByActivationID.remove(id);
                groupIDByActivationID.remove(id);
            }
        }

        public synchronized void inactive(long incarnation) {
            isActive = false;
        }

        public String toString() {
            return "GroupInfo[ ActivationGroupID = " + agid + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Structure to hold just 1 activated object.
     */
    private class ObjectInfo {

        boolean isActive;

        private ActivationDesc desc;

        private ActivationID id;

        MarshalledObject cachedInstance = null;

        public ObjectInfo(ActivationID id, ActivationDesc desc) {
            this.id = id;
            this.desc = desc;
            isActive = false;

            if (startMonitor) {
                rmidMonitor.addObject(id, desc.getGroupID());
            }
        }

        public synchronized MarshalledObject activate(
                ActivationInstantiator ai) throws ActivationException,
                RemoteException {

            // rmi.log.6E=ObjectInfo.activate started. Act Inst = {0}
            rLog.log(commonDebugLevel,Messages.getString("rmi.log.6E", ai)); //$NON-NLS-1$

            if (cachedInstance != null) {
                // rmi.log.6F=Subsequent call to activate, returning cached instance.
                rLog.log(commonDebugLevel, Messages.getString("rmi.log.6F")); //$NON-NLS-1$
                return cachedInstance;
            }

            MarshalledObject mo = ai.newInstance(id, desc);
            // rmi.log.70=ObjectInfo.activate completed: {0}
            rLog.log(commonDebugLevel, Messages.getString("rmi.log.70", mo)); //$NON-NLS-1$

            return mo;
        }

        public ActivationDesc getActivationDesc() {
            return desc;
        }

        public synchronized void setActivationDesc(ActivationDesc desc) {
            this.desc = desc;
        }

        public void active() {
            if (startMonitor) {
                rmidMonitor.activeObject(id);
            }
        }

        public void inactive() {
            /*
             * When the object is being deactivated, the cached instance of
             * its stub becomes invalid.
             */
            cachedInstance = null;

            if (startMonitor) {
                rmidMonitor.inactiveObject(id);
            }
        }
    }

    /**
     * DebugThread - the thread that consumes the contents of the given
     * InputStream and prints it to console. Usually it is used to read
     * information from error and input streams of the ActivationGroup.
     */
    private class DebugThread extends Thread {
        InputStream is = null;

        public DebugThread(InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                byte tmp[] = new byte[1000];

                while (true) {
                    int c = is.read(tmp);
                    if (c == -1) break;
                    byte buf[] = new byte[c];
                    System.arraycopy(tmp, 0, buf, 0, c);
                    if (c != 0) System.out.print(new String(buf));
                }
            } catch (Throwable t) {
            }
        }
    }

    private synchronized void snapshot() {
        try {
            File f = new File(logFolder
                    + RMIConstants.DEFAULT_SNAPSHOT_FILE);
            FileOutputStream fos = new FileOutputStream(f, false);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(new Snapshot());
            out.close();
            fos.close();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    private synchronized void writeDelta(final int op, final String name,
            final Object key, final Object val) {
        deltasCounter++;

        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                if (deltasCounter < snapshotInterval) {

                    try {
                        File f = new File(logFolder
                                + RMIConstants.DEFAULT_DELTA_FILE);
                        FileOutputStream fos = new FileOutputStream(f,
                                true);
                        ObjectOutputStream out = new ObjectOutputStream(
                                fos);
                        out.writeObject(new Delta(op, name, key, val));
                        out.close();
                        fos.close();
                        // rmi.log.71=Delta was written.
                        rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.71")); //$NON-NLS-1$
                    } catch (Throwable t) {
                        t.printStackTrace();
                        System.exit(1);
                    }
                } else {
                    File df = new File(logFolder
                            + RMIConstants.DEFAULT_DELTA_FILE);
                    df.delete();
                    snapshot();
                    deltasCounter = 0;

                }
                return null;
            }
        });
    }

    private synchronized void restore() throws Exception {
        restoreLock = true;
        final File sf = new File(logFolder
                + RMIConstants.DEFAULT_SNAPSHOT_FILE);
        final File df = new File(logFolder
                + RMIConstants.DEFAULT_DELTA_FILE);

        try {

            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {

                    if (sf.exists()) {
                        FileInputStream fis = new FileInputStream(sf);
                        ObjectInputStream in = new ObjectInputStream(fis);
                        in.readObject();
                        in.close();
                        fis.close();
                    }

                    try {
                        if (df.exists()) {
                            FileInputStream fis = new FileInputStream(df);
                            while (true) {
                                ObjectInputStream in = new ObjectInputStream(
                                        fis);
                                Delta d = (Delta) in.readObject();
                                // rmi.log.72=A delta was restored: {0}
                                rLog.log(persistenceDebugLevel,
                                        Messages.getString("rmi.log.72", d)); //$NON-NLS-1$
                            }
                        }
                    } catch (EOFException eofe) {
                        // This section was intentionally left empty.
                        // Indicates that End of File reached -
                        //meaning all deltas were read.
                        return null;
                    }
                    return null;
                }
            });
        } catch (Throwable t) {
            // rmi.log.73=Exception in restore: {0}
            rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.73", t)); //$NON-NLS-1$
            t.printStackTrace();
            /*
             * Returning Activation System into initial state:
             */
            groupIDByActivationID = new Hashtable();
            groupInfoByGroupId = new Hashtable();
            System.gc();
        }

        restoreLock = false;
    }

    class Delta implements Serializable {

        private static final long serialVersionUID = 103662164369676173L;

        private static final int PUT = 0;

        private static final int REMOVE = 1;

        public int op;

        public String name;

        public MarshalledObject mkey;

        public MarshalledObject mval;

        public Delta(int op, String name, Object key, Object val)
                throws Exception {
            this.op = op;
            this.name = name;
            mkey = new MarshalledObject(key);
            mval = new MarshalledObject(val);
        }

        public String toString() {
            try {
                return "Delta: " + op + "," + name + ", " + mkey.get() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        + ", " + mval.get(); //$NON-NLS-1$
            } catch (Throwable t) {
                t.printStackTrace();
                return "" + t; //$NON-NLS-1$
            }
        }

        private synchronized void writeObject(ObjectOutputStream out)
                throws IOException {
            out.writeInt(op);
            out.writeUTF(name);
            out.writeObject(mkey);
            out.writeObject(mval);
        }

        private synchronized void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            op = in.readInt();
            name = in.readUTF();
            mkey = (MarshalledObject) in.readObject();
            mval = (MarshalledObject) in.readObject();
            // rmi.log.75=Delta: Data read: 
            rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.75") + op //$NON-NLS-1$
                    + ", " + name + ", " + mkey.get() + ", " + mval.get()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            if (op == PUT) {
                if (name.equals("group")) { //$NON-NLS-1$
                    ActivationGroupID agid = (ActivationGroupID) mkey
                            .get();
                    // rmi.log.76=Restore agid: {0}
                    rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.76", //$NON-NLS-1$
                            agid));
                    ActivationGroupDesc agdesc = (ActivationGroupDesc) mval
                            .get();

                    ActivationGroupInfo agi = new ActivationGroupInfo(
                            agid, agdesc);
                    // rmi.log.77=Restore agi: {0}
                    rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.77", agi)); //$NON-NLS-1$
                    groupInfoByGroupId.put(agid, agi);
                    // rmi.log.78=The data were put into groupID2groupInfo_H
                    rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.78")); //$NON-NLS-1$
                }
                if (name.equals("object")) { //$NON-NLS-1$
                    // rmi.log.79=Trying to restore ActivationID:
                    rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.79")); //$NON-NLS-1$
                    ActivationID aid = (ActivationID) mkey.get();
                    // rmi.log.0F=aid = {0}
                    rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.0F", aid)); //$NON-NLS-1$
                    ActivationDesc adesc = (ActivationDesc) mval.get();
                    // rmi.log.7A=adesc = {0}
                    rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.7A", adesc)); //$NON-NLS-1$

                    ActivationGroupID agid = adesc.getGroupID();
                    // rmi.log.57=agid = {0}
                    rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.57", agid)); //$NON-NLS-1$

                    ActivationGroupInfo agi =
                        (ActivationGroupInfo) groupInfoByGroupId.get(agid);
                    // rmi.log.58=agi = {0}
                    rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.58", agi)); //$NON-NLS-1$

                    groupIDByActivationID.put(aid, agid);

                    agi.registerObject(aid, adesc);
                    // rmi.log.7D=Object was registered.
                    rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.7D")); //$NON-NLS-1$
                    // rmi.log.7E=Object was put into hashtable.
                    rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.7E")); //$NON-NLS-1$
                }
            }

            if (op == REMOVE) {
                if (name.equals("object")) { //$NON-NLS-1$
                    groupIDByActivationID.remove(mkey.get());
                }

                if (name.equals("group")) { //$NON-NLS-1$
                    groupInfoByGroupId.remove(mkey.get());
                }
            }
        }
    }

    class Snapshot implements Serializable {

        private static final long serialVersionUID = 2006016754895450831L;

        private synchronized void writeObject(ObjectOutputStream out)
                throws IOException {

            Hashtable h0 = new Hashtable();
            Hashtable h1 = new Hashtable();

            Enumeration e0 = groupInfoByGroupId.keys();
            while (e0.hasMoreElements()) {
                ActivationGroupID agid = (ActivationGroupID) e0
                        .nextElement();
                MarshalledObject mo_agid = new MarshalledObject(agid);
                ActivationGroupInfo agi =
                    (ActivationGroupInfo) groupInfoByGroupId.get(agid);
                ActivationGroupDesc agdesc = agi.getActivationGroupDesc();
                h0.put(mo_agid, agdesc);
                Enumeration e1 = agi.objectInfoByActivationID.keys();
                while (e1.hasMoreElements()) {
                    ActivationID aid = (ActivationID) e1.nextElement();
                    ObjectInfo oi = (ObjectInfo) agi.objectInfoByActivationID
                            .get(aid);
                    ActivationDesc adesc = oi.getActivationDesc();
                    h1.put(aid, adesc);
                }
            }
            out.writeObject(h0);
            out.writeObject(h1);
        }

        private synchronized void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            Hashtable h0 = (Hashtable) in.readObject();
            Hashtable h1 = (Hashtable) in.readObject();

            // rmi.log.7F=Restore:
            rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.7F")); //$NON-NLS-1$
            // rmi.log.80=h0 = {0}
            rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.80", h0)); //$NON-NLS-1$
            // rmi.log.81=h1 = {0}
            rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.81", h1)); //$NON-NLS-1$

            Enumeration e0 = h0.keys();
            while (e0.hasMoreElements()) {

                MarshalledObject mo_agid = (MarshalledObject) e0
                        .nextElement();
                ActivationGroupID agid = (ActivationGroupID) mo_agid.get();
                // rmi.log.76=Restore agid: {0}
                rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.76", agid)); //$NON-NLS-1$

                ActivationGroupDesc agdesc = (ActivationGroupDesc) h0
                        .get(mo_agid);

                // rmi.log.82=Restore agdesc: {0}
                rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.82", //$NON-NLS-1$
                        agdesc));
                ActivationGroupInfo agi = new ActivationGroupInfo(agid,
                        agdesc);
                // rmi.log.77=Restore agi: {0}
                rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.77") + agi); //$NON-NLS-1$
                groupInfoByGroupId.put(agid, agi);
                // rmi.log.78=The data were put into groupID2groupInfo_H
                rLog.log(persistenceDebugLevel, Messages.getString("rmi.log.78")); //$NON-NLS-1$
            }

            Enumeration e1 = h1.keys();
            while (e1.hasMoreElements()) {
                ActivationID aid = (ActivationID) e1.nextElement();
                ActivationDesc adesc = (ActivationDesc) h1.get(aid);
                ActivationGroupID agid = adesc.getGroupID();
                ActivationGroupInfo agi =
                    (ActivationGroupInfo) groupInfoByGroupId.get(agid);
                agi.registerObject(aid, adesc);
                groupIDByActivationID.put(aid, agid);
            }
        }
    }
}
