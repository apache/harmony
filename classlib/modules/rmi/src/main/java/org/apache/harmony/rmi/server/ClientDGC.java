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

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.rmi.RemoteException;
import java.rmi.dgc.DGC;
import java.rmi.dgc.Lease;
import java.rmi.dgc.VMID;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteRef;
import java.rmi.server.UID;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.harmony.rmi.common.CreateThreadAction;
import org.apache.harmony.rmi.common.GetLongPropAction;
import org.apache.harmony.rmi.common.InterruptThreadAction;
import org.apache.harmony.rmi.common.RMIProperties;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.remoteref.UnicastRef;
import org.apache.harmony.rmi.transport.Endpoint;


/**
 * DGC on Client's side - it's responsible for renewing/cancelling
 * remote objects leases.
 * It is made package protected for security reasons.
 *
 * @author  Mikhail A. Markov
 */
class ClientDGC {

    /** ObjID for DGC. */
    public static final ObjID DGC_ID = new ObjID(ObjID.DGC_ID);

    /*
     * DGC ack timeout (in ms.) during which we hold strong refs to the objects.
     * Default value is 300000 ms (5 minutes).
     */
    private static final long dgcAckTimeout =
        ((Long) AccessController.doPrivileged(
                new GetLongPropAction(RMIProperties.DGCACKTIMEOUT_PROP,
                        5 * 60 * 1000))).longValue();
    /*
     * Max length of time (in ms.) between DGC.clean() calls in case of failed
     * calls. Default value is 180000 ms (3 minutes).
     */
    private static final long cleanInterval =
        ((Long) AccessController.doPrivileged(
                new GetLongPropAction(RMIProperties.DGCCLEANINTERVAL_PROP,
                        3 * 60 * 1000))).longValue();

    // VMID for this VM
    private static final VMID vmid = new VMID();

    /*
     * Sequentially increasing number for DGC calls.
     * It should be accessed vid getSeqNumber() method only.
     */
    private static long seqNum = Long.MIN_VALUE;

    // Table where Endpoint's are keys and RenewInfo are values.
    private static Hashtable epTable = new Hashtable();

    // List of strong refs to remoteObjects for referencing during DGC ack call.
    private static Hashtable dgcAckTable = new Hashtable();

    // Thread renewing leases.
    private static Thread lRenewer;

    // Thread detecting object which were garbage-collected.
    private static Thread roDetector = (Thread) AccessController.doPrivileged(
            new CreateThreadAction(new RemovedObjectsDetector(),
                    "RemovedObjectsDetector", true)); //$NON-NLS-1$

    // Timer handling events waiting for DGC ack messages.
    private static final Timer dgcAckTimer =
            (Timer) AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    return new Timer(true);
                }});

    // DGC stub class
    private static Class dgcStubClass;

    // Queue for handling already collected objects.
    private static ReferenceQueue collectedQueue = new ReferenceQueue();

    static {

        // Initialize DGC implementation stub-class.
        try {
            dgcStubClass = Class.forName(
                "org.apache.harmony.rmi.server.DGCImpl_Stub"); //$NON-NLS-1$
        } catch (Exception ex) {
            // rmi.78=Unable to initialize ClientDGC.
            throw new Error(Messages.getString("rmi.78"), ex); //$NON-NLS-1$
        }

        // Start the thread detecting garbage-collected objects.
        roDetector.start();
    }

    /*
     * Registers the given RemoteRef in the table for sending DGC.dirty() calls
     * to the server, exported this object.
     */
    static void registerForRenew(RemoteRefBase ref) {
        RenewInfo info = (RenewInfo) epTable.get(ref.ep);

        if (info == null) {
            info = new RenewInfo(ref.ep);
            info.renewTime = System.currentTimeMillis();
            epTable.put(ref.ep, info);
            info.addToDirtySet(ref);
        } else if (info.addToDirtySet(ref)) {
            info.renewTime = System.currentTimeMillis();
        }

        if (lRenewer == null) {
            (lRenewer = (Thread) AccessController.doPrivileged(
                    new CreateThreadAction(new LeaseRenewer(),
                            "LeaseRenewer", true))).start(); //$NON-NLS-1$
        }
    }

    /*
     * Remove the given id for the specified Endpoint from the table and call
     * DGC.clean() method.
     */
    static void unregisterForRenew(Endpoint ep, ObjID id) {
        RenewInfo info = (RenewInfo) epTable.get(ep);

        if (info == null) {
            return;
        }

        // add ObjID to the list for DGC.clean() calls
        info.addToCleanSet(id);
    }

    /*
     * Adds the given object to the list of strong references associated with
     * the given UID.
     */
    static void registerForDGCAck(UID uid, Object obj) {
        RemoveTask task = (RemoveTask) dgcAckTable.get(uid);

        if (task == null) {
            task = new RemoveTask(uid);
            dgcAckTable.put(uid, task);
            dgcAckTimer.schedule(task, dgcAckTimeout);
        }
        task.addStrongRef(obj);
    }

    /*
     * Removes all strong refs associated with the given UID.
     */
    static void unregisterForDGCAck(UID uid) {
        TimerTask task = (TimerTask) dgcAckTable.get(uid);

        if (task == null) {
            return;
        }
        task.cancel();
        dgcAckTable.remove(uid);
    }

    /*
     * Returns next sequence number
     */
    private static synchronized long getSeqNumber() {
        return seqNum++;
    }


    /*
     * Auxiliary class for removing strong refs to the objects from the table.
     */
    private static class RemoveTask extends TimerTask {
        // UID for removing from the table
        private UID uid;

        // strong references to the objects
        private List strongRefs = new Vector();

        /*
         * Constructs RemoveTask for removing the given UID from the table.
         *
         * @param uid UID to be removed in the future by this task
         */
        RemoveTask(UID uid) {
            this.uid = uid;
        }

        /*
         * Removes strong refs to the objects associated with the stored UID
         * from the table;
         */
        public void run() {
            dgcAckTable.remove(uid);
        }

        /*
         * Adds strong ref to the given object to the list.
         *
         * @param obj Object strong reference to which should be stored
         */
        void addStrongRef(Object obj) {
            strongRefs.add(obj);
        }
    }


    /*
     * Phantom reference holding Endpoint and ObjID information for cancelling
     * leases after object is garbage-collected.
     */
    private static class PhantomRef extends PhantomReference {
        private Endpoint ep;
        private ObjID id;

        /*
         * Constructs PhantomRef. Calls super() with the given queue and obj and
         * stores ep and id.
         */
        PhantomRef(Object obj,
                   ReferenceQueue queue,
                   Endpoint ep,
                   ObjID id) {
            super(obj, queue);
            this.ep = ep;
            this.id = id;
        }
    }


    /*
     * Auxiliary class holding all info needed for leases renewing.
     */
    private static class RenewInfo {

        /*
         * Remote Endpoint where leases should be renewed.
         */
        private Endpoint ep;

        /*
         * Table where objIDs whose leases should be renewed are the keys
         * and PhantomRefs are the values.
         */
        private Hashtable renewTable = new Hashtable();

        /*
         * Table where objIDs whose leases should be cancelled are stored
         */
        private Set cleanSet = Collections.synchronizedSet(new HashSet());

        // Object for tables synchronization.
        private class TablesLock {}
        private Object tablesLock = new TablesLock();

        // When to renew leases.
        private long renewTime;

        // Initialized DGC stub.
        private DGC dgcStub;

        // Thread calling DGC.clean method
        private Thread cleanCaller = null;

        // Time when the first DGC.dirty call failed
        private long failureStartTime = 0;

        // Number of failed DGC.dirty calls.
        private int failedDirtyCallsNum = 0;

        // Base time for calculating renew time in case of failed dirty calls
        private long failedRenewBaseDuration = 0;

        // Lease duration returned by the latest successful DGC.dirty call.
        private long latestLeaseDuration = 0;

        /*
         * Initializes DGC stub and stores the given Endpoint.
         *
         * @param ep RemoteEndpoint where leases should be renewed
         */
        RenewInfo(Endpoint ep) {
            this.ep = ep;
            try {
                dgcStub = (DGC) dgcStubClass.getConstructor(
                        new Class[] { RemoteRef.class }).newInstance(
                                new Object[] { new UnicastRef(ep, DGC_ID) });
            } catch (Exception ex) {
                // rmi.79=Unable to initialized DGC stub.
                throw new Error(Messages.getString("rmi.79"), ex); //$NON-NLS-1$
            }
        }

        /*
         * Calls DGC.dirty() method.
         */
        void dgcDirty() {
            ObjID[] ids;

            synchronized (tablesLock) {
                ids = (ObjID[]) renewTable.keySet().toArray(
                        new ObjID[renewTable.size()]);
            }

            try {
                Lease lease = dgcStub.dirty(ids, getSeqNumber(),
                        new Lease(vmid, DGCImpl.maxDuration));
                failedDirtyCallsNum = 0;
                failureStartTime = 0;
                latestLeaseDuration = lease.getValue();
                renewTime = System.currentTimeMillis()
                        + latestLeaseDuration / 2;
            } catch (RemoteException re) {
                // dirty call failed
                long curTime = System.currentTimeMillis();
                ++failedDirtyCallsNum;

                if (failedDirtyCallsNum == 1) {
                    failureStartTime = curTime;
                    latestLeaseDuration = (latestLeaseDuration > 0)
                            ? latestLeaseDuration : DGCImpl.maxDuration;
                    failedRenewBaseDuration = latestLeaseDuration >> 5;
                }
                renewTime = curTime +
                    failedRenewBaseDuration * ((failedDirtyCallsNum - 1) << 1);

                if (renewTime > failureStartTime + latestLeaseDuration) {
                    renewTime = Long.MAX_VALUE;
                }
            }
        }

        /*
         * Adds ObjID of the given ref to the set of objIDs whose leases should
         * be renewed, creates a PhantomReference for the object for sending
         * clean request to the server's DGC when the object is
         * garbage-collected.
         *
         * @param ref UnicastRef to be registered
         *
         * @return true if this RenewInfo already contained the given ref and
         *         false otherwise
         */
        boolean addToDirtySet(RemoteRefBase ref) {
            synchronized (tablesLock) {
                ObjID id = ref.getObjId();

                if (renewTable.containsKey(id)) {
                    return true;
                }
                renewTable.put(id, new PhantomRef(
                        ref, collectedQueue, ep, id));
                return false;
            }
        }

        /*
         * Adds the given ObjID to the list of objects for DGC.clean() method
         * call and removes it from the dirty set.
         *
         * @param id ObjID of remote object
         */
        void addToCleanSet(ObjID id) {
            synchronized (tablesLock) {
                renewTable.remove(id);
                cleanSet.add(id);

                if (cleanCaller == null) {
                    (cleanCaller = ((Thread) AccessController.doPrivileged(
                            new CreateThreadAction(new CleanCaller(this),
                                    "CleanCaller for " + ep, true)))).start(); //$NON-NLS-1$
                } else {
                    AccessController.doPrivileged(
                            new InterruptThreadAction(cleanCaller));
                }
            }
        }
    }


    /*
     * Auxiliary class renewing leases.
     */
    private static class LeaseRenewer implements Runnable {
        /**
         * Iterates over epTable and renews leases.
         */
        public void run() {
            do {
                long curTime = System.currentTimeMillis();
                long awakeTime = curTime + DGCImpl.maxDuration / 2;

                try {
                    synchronized (epTable) {
                        for (Enumeration eps = epTable.keys();
                                eps.hasMoreElements();) {
                            Endpoint ep = (Endpoint) eps.nextElement();
                            RenewInfo info = (RenewInfo) epTable.get(ep);

                            if (info.renewTime <= curTime) {
                                // we should renew lease for this ids
                                info.dgcDirty();
                            }

                            if (info.renewTime < awakeTime) {
                                awakeTime = info.renewTime;
                            }
                        }
                    }

                    if (awakeTime > curTime) {
                        Thread.sleep(awakeTime - curTime);
                    }
                } catch (InterruptedException ie) {
                }
            } while (epTable.size() != 0);
            lRenewer = null;
        }
    }


    /*
     * Auxiliary thread detecting remote objects which where garbage-collected
     * and spawning the thread calling DGC.clean() method.
     */
    private static class RemovedObjectsDetector implements Runnable {

        /**
         * Thread sitting on blocking ReferenceQueue.remove() method and when
         * it returns PhantomRef - removing the object from the dirty set and
         * calling DGC.clean() method.
         */
        public void run() {
            do {
                try {
                    // this operation blocks the thread
                    PhantomRef ref = (PhantomRef) collectedQueue.remove();

                    unregisterForRenew(ref.ep, ref.id);
                } catch (InterruptedException ie) {
                }
            } while (true);
        }
    }


    /*
     * Auxiliary class calling DGC.clean() method on server side.
     */
    private static class CleanCaller implements Runnable {
        // RenewInfo where this class was created
        private RenewInfo info;

        // Number of failed DGC.clean calls.
        private int failedCleanCallsNum = 0;

        /*
         * Constructs CleanCaller holding the reference to the given RenewInfo.
         */
        CleanCaller(RenewInfo info) {
            this.info = info;
        }

        /**
         * Call DGC.clean() method, in case of failed call retry it in a loop.
         */
        public void run() {
            while (true) {
                boolean success = true;
                Set curCleanSet;

                synchronized (info.tablesLock) {
                    if (info.cleanSet.isEmpty()) {
                        break;
                    }
                    curCleanSet = new HashSet(info.cleanSet);
                }

                try {
                    info.dgcStub.clean((ObjID[]) curCleanSet.toArray(
                            new ObjID[curCleanSet.size()]), getSeqNumber(),
                            vmid, info.failedDirtyCallsNum == 0);

                    // DGC.clean() call succeeded
                    synchronized (info.tablesLock) {
                        info.cleanSet.remove(curCleanSet);

                        if (info.cleanSet.isEmpty()) {
                            break;
                        }
                    }
                } catch(RemoteException re) {
                    // DGC.clean() call failed
                    success = false;
                    failedCleanCallsNum++;

                    if (failedCleanCallsNum > 4) {
                        synchronized (info.tablesLock) {
                            if (!info.renewTable.isEmpty()) {
                                info.cleanSet.clear();
                            }
                        }
                        break;
                    }
                }

                if (Thread.interrupted()) {
                    continue;
                }

                /*
                 * If DGC.clean() call failed we should wait for a cleanInterval
                 * period of time.
                 */
                if (!success) {
                    try {
                        Thread.sleep(cleanInterval);
                    } catch(InterruptedException ie) {
                        continue;
                    }
                }
            }

            synchronized (info.tablesLock) {
                if (!info.renewTable.isEmpty()) {
                    epTable.remove(info.ep);
                }
            }
            info.cleanCaller = null;
        }
    }
}
