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

import java.rmi.RemoteException;
import java.rmi.dgc.DGC;
import java.rmi.dgc.Lease;
import java.rmi.dgc.VMID;
import java.rmi.server.ObjID;
import java.security.AccessController;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.apache.harmony.rmi.common.CreateThreadAction;
import org.apache.harmony.rmi.common.GetLongPropAction;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIProperties;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Distributed Garbage Collector implementation (Server side).
 * It is made package protected for security reasons.
 *
 * @author  Mikhail A. Markov
 */
class DGCImpl implements DGC {

    /* Log where to write dgc activity. */
    static final RMILog dgcLog = RMILog.getDGCLog();

    /*
     * Maximum lease duration granted by this DGC.
     * Default value is 10 minutes.
     */
    static final long maxDuration =
        ((Long) AccessController.doPrivileged(
                new GetLongPropAction(RMIProperties.DGCLEASEVALUE_PROP,
                        10 * 60 * 1000))).longValue();

    /*
     * How often we will check DGC leases.
     * Default value is half of maxDuration time.
     */
    private static final long checkInterval =
        ((Long) AccessController.doPrivileged(
                new GetLongPropAction(RMIProperties.DGCCHECKINTERVAL_PROP,
                        maxDuration / 2))).longValue();

    /*
     * Table where VMIDs are keys and HashSets of ObjIDs are the values.
     */
    private Hashtable vmidTable = new Hashtable();


    // Thread checking leases expirations.
    private static Thread expTracker;

    /**
     * @see DGC.dirty(ObjID[], long, Lease)
     */
    public Lease dirty(ObjID[] ids, long seqNum, Lease lease)
            throws RemoteException {
        VMID vmid = lease.getVMID();

        if (vmid == null) {
            vmid = new VMID();

            if (dgcLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.10E=Created new VMID: {0}
                dgcLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.10E", vmid)); //$NON-NLS-1$
            }
        }
        long duration = lease.getValue();
        if (duration > maxDuration || duration < 0) {
            duration = maxDuration;
        }
        Lease l = new Lease(vmid, duration);

        synchronized (vmidTable) {
            Set s = (Set) vmidTable.get(vmid);

            if (s == null) {
                if (ids == null || ids.length == 0) {
                    // Nothing to do: no VM with such VMID registered
                    return l;
                }
                s = Collections.synchronizedSet(new HashSet());
                vmidTable.put(vmid, s);
            }

            if (expTracker == null) {
                (expTracker = (Thread) AccessController.doPrivileged(
                        new CreateThreadAction(new ExpirationTracker(),
                                "ExpirationTracker", true))).start(); //$NON-NLS-1$
            }

            for (int i = 0; i < ids.length; ++i) {
                s.add(ids[i]);

                if (dgcLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.10F=Added {0}, {1}, duration ={2}
                    dgcLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.10F", //$NON-NLS-1$
                            new Object[]{ids[i], vmid, duration}));
                }
            }

            for (Iterator allIds = s.iterator(); allIds.hasNext();) {
                RMIObjectInfo info = ExportManager.getInfo(
                        (ObjID) allIds.next());

                if (info == null) {
                    /*
                     * Object with this id has not been exported or has been
                     * garbage-collected.
                     */
                    allIds.remove();
                    continue;
                }
                info.dgcDirty(vmid, seqNum, duration);
            }
        }

        if (dgcLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.110=Updated {0}
            dgcLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.110", vmid)); //$NON-NLS-1$
        }
        return l;
    }

    /**
     * @see DGC.clean(ObjID[], long, VMID, boolean)
     */
    public void clean(ObjID[] ids, long seqNum, VMID vmid, boolean strong)
            throws RemoteException {
        synchronized (vmidTable) {
            Set s = (Set) vmidTable.get(vmid);

            if (s == null) {
                return;
            }

            if (ids != null && ids.length > 0) {
                for (int i = 0; i < ids.length; ++i) {
                    RMIObjectInfo info = ExportManager.getInfo(ids[i]);

                    if (info == null || info.dgcClean(vmid, seqNum, strong)) {
                        s.remove(ids[i]);

                        if (dgcLog.isLoggable(RMILog.VERBOSE)) {
                            // rmi.log.111=Removed {0},{1}
                            dgcLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.111", //$NON-NLS-1$
                                    ids[i], vmid));
                        }
                    }
                }
            } else {
                for (Iterator allIds = s.iterator(); allIds.hasNext();) {
                    RMIObjectInfo info = ExportManager.getInfo(
                            (ObjID) allIds.next());

                    if (info == null || info.dgcClean(vmid, seqNum, strong)) {
                        allIds.remove();
                    }
                }
            }

            if (s.isEmpty()) {
                vmidTable.remove(vmid);

                if (dgcLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.112=Removed {0}
                    dgcLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.112", vmid)); //$NON-NLS-1$
                }
            }
        }
    }


    /*
     * Auxiliary class checking expiration times and removing expired entries
     * from the list of active objects.
     */
    private class ExpirationTracker implements Runnable {
        /**
         * Checks expiration times until no objects left in the table.
         * Sleeps for a checkInterval period of time between checks.
         */
        public void run() {
            do {
                try {
                    Thread.sleep(checkInterval);
                } catch (InterruptedException ie) {
                }
            } while (checkExpiration());
            expTracker = null;
        }

        /*
         * Checks expiration times and removes expired entries
         * from the list of active objects.
         *
         * @return false if there are no objects in the table left and false
         *         otherwise
         */
        boolean checkExpiration() {
            if (vmidTable.size() == 0) {
                return false;
            }
            long curTime = System.currentTimeMillis();

            synchronized (vmidTable) {
                for (Enumeration en = vmidTable.keys(); en.hasMoreElements();) {
                    VMID vmid = (VMID) en.nextElement();
                    Set s = (Set) vmidTable.get(vmid);

                    for (Iterator iter = s.iterator(); iter.hasNext();) {
                        RMIObjectInfo info = ExportManager.getInfo(
                                (ObjID) iter.next());

                        if (info == null || info.dgcClean(vmid)) {
                            iter.remove();

                            if (info != null
                                    && dgcLog.isLoggable(RMILog.VERBOSE)) {
                                dgcLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.113", //$NON-NLS-1$
                                        info.id, vmid));
                            }
                        }
                    }
                }
            }
            return true;
        }
    }
}
