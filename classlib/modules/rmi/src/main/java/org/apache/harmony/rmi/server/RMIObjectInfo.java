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

import java.rmi.Remote;
import java.rmi.dgc.VMID;
import java.rmi.server.ObjID;
import java.rmi.server.Unreferenced;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import org.apache.harmony.rmi.common.CreateThreadAction;
import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.remoteref.UnicastServerRef;


/**
 * Holds info characterizing exported remote objects.
 *
 * @author  Mikhail A. Markov
 */
final class RMIObjectInfo {

    // Reference to remote object.
    final RMIReference ref;

    // Object ID for remote object.
    final ObjID id;

    // Stub for remote object.
    final Remote stub;

    // Server handle for remote object
    final UnicastServerRef sref;

    // AccessControlContext when this Info was created
    final AccessControlContext acc;

    // ClassLoader for dispatching calls.
    final ClassLoader loader;

    // Table where VMIDs are keys and DGCExpirationInfos are values
    final Hashtable vmidTable = new Hashtable();

    /*
     * Table of VMID's remembered after DGC.clean() call with strong=true.
     * VMIDs are keys and Long() representing sequence numbers are values.
     */
    final Hashtable rememberedTable = new Hashtable();

    /*
     * Constructs RemoteObjectInfo holding the specified information.
     * The ClassLoader for dispatching calls is chosen using the following
     * rules:
     * 1) If current thread context ClassLoader is null, then ClassLoader
     *    which loads Class of remote object will be the ClassLoader for
     *    dispatching calls
     * 2) If ClassLoader which loads Class of remote object is null, then
     *    current thread context ClassLoader will be the ClassLoader for
     *    dispatching calls
     * 3) If context ClassLoader of the current thread is equal/parent
     *    of the ClassLoader which loads Class of remote object, then
     *    current thread context ClassLoader will be the ClassLoader for for
     *    dispatching calls.
     * 4) Otherwise ClassLoader which loads Class of remote object will
     *    be the ClassLoader fo dispatching calls.
     *
     * @param ref reference to remote object
     * @param id Object ID for remote object
     * @param sref UnicastServerRef for dispatching remote calls
     * @param stub stub for remote object
     */
    RMIObjectInfo(RMIReference ref,
                  ObjID id,
                  UnicastServerRef sref,
                  Remote stub) {
        this.ref = ref;
        this.id = id;
        this.sref = sref;
        this.stub = stub;
        acc = AccessController.getContext();
        ClassLoader objCl = ref.get().getClass().getClassLoader();
        ClassLoader threadCl = Thread.currentThread().getContextClassLoader();

        if (threadCl == null) {
            loader = objCl;
        } else if (objCl == null || RMIUtil.isParentLoader(threadCl, objCl)) {
            loader = threadCl;
        } else {
            loader = objCl;
        }
    }

    /*
     * Called by DGC to signal that dirty call received to the object with
     * such ObjID.
     */
    void dgcDirty(VMID vmid, long seqNum, long duration) {
        synchronized (vmidTable) {
            DGCExpirationInfo info = (DGCExpirationInfo) vmidTable.get(vmid);

            if (info != null && info.seqNum >= seqNum) {
                return;
            }
            Long l = (Long) rememberedTable.get(vmid);

            if (l != null) {
                if (l.longValue() > seqNum) {
                    return;
                } else {
                    rememberedTable.remove(vmid);
                }
            }
            ref.makeStrong(true);
            vmidTable.put(vmid, new DGCExpirationInfo(duration, seqNum));
        }
    }

    /*
     * Called by DGC to signal that clean call received to the object with
     * such ObjID.
     */
    boolean dgcClean(VMID vmid, long seqNum, boolean strong) {
        synchronized (vmidTable) {
            DGCExpirationInfo info = (DGCExpirationInfo) vmidTable.get(vmid);

            if (info != null && info.seqNum >= seqNum) {
                return false;
            }
            vmidTable.remove(vmid);

            if (strong) {
                Long l = (Long) rememberedTable.get(vmid);

                if (l != null && l.longValue() > seqNum) {
                    return true;
                }
                rememberedTable.put(vmid, new Long(seqNum));
            }

            if (vmidTable.isEmpty()) {
                unreferenced();
            }
            return true;
        }
    }

    /*
     * Called by DGC to verify if the given VMID reference expired.
     */
    boolean dgcClean(VMID vmid) {
        synchronized (vmidTable) {
            DGCExpirationInfo info = (DGCExpirationInfo) vmidTable.get(vmid);

            if (info != null && info.expTime > System.currentTimeMillis()) {
                return false;
            }
            vmidTable.remove(vmid);

            if (vmidTable.isEmpty()) {
                unreferenced();
            }
            return true;
        }
    }

    /*
     * Spawn thread calling unreferenced() method of the object to be
     * unreferenced and makes the reference to the object as weak.
     */
    private synchronized void unreferenced() {
        final Object obj = ref.get();

        if (obj instanceof Unreferenced) {
            /*
             * Spawn unreferenced thread.
             * Start this thread with setDaemon(false) to protect VM
             * from exiting while unreferencing the object.
             * The thread is started in non-system group
             * (see comment for CreateThreadAction class).
             */
            Thread uThread = ((Thread) AccessController.doPrivileged(
                    new CreateThreadAction(new Runnable() {
                        public void run() {
                            AccessController.doPrivileged(
                                    new PrivilegedAction() {
                                        public Object run() {
                                            ((Unreferenced) obj).unreferenced();
                                            return null;
                                        }
                                    }, acc);
                        }
                    }, "Unreferenced", false, false))); //$NON-NLS-1$
            uThread.setContextClassLoader(loader);
            uThread.start();
        }
        ref.makeStrong(false);
    }


    /*
     * Auxiliary class holding expiration time and sequence number when
     * this expiration was created.
     */
    private static class DGCExpirationInfo {
        // Expiration time in ms.
        long expTime;

        // Sequence number.
        long seqNum;

        /*
         * Constructs an empty DGCExpirationInfo.
         */
        DGCExpirationInfo() {
        }

        /*
         * Constructs ExpirationInfo class from the given duration and
         * sequence number.
         *
         * @param duration time period after which this entry expires
         * @param seqNumber Sequence number to handle consecutive calls
         */
        DGCExpirationInfo(long duration, long seqNum) {
            this.expTime = System.currentTimeMillis() + duration;
            this.seqNum = seqNum;
        }
    }
}
