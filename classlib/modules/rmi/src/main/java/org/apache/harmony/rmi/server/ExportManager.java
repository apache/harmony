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

import java.lang.ref.ReferenceQueue;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.dgc.DGC;
import java.rmi.server.ExportException;
import java.rmi.server.ObjID;
import java.security.AccessController;

import org.apache.harmony.rmi.common.CreateThreadAction;
import org.apache.harmony.rmi.common.InterruptThreadAction;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.remoteref.UnicastServerRef;


/**
 * Manager controlling all exported objects.
 * It is put to org.apache.harmony.rmi.transport package because some methods
 * should be made package protected.
 *
 * @author  Mikhail A. Markov
 */
public class ExportManager {

    // List of all remote objects exported in this VM.
    private static RMIObjectTable exportedObjs = new RMIObjectTable();

    // The only one instance of DGC per VM.
    private static DGC dgcImpl = new DGCImpl();

    // Creates and exports DGC implementation.
    static {
        try {
            UnicastServerRef dgcRef = new UnicastServerRef(0, null, null,
                    ClientDGC.DGC_ID);
            dgcRef.exportObject(dgcImpl, null, false, false, true);
            RMIObjectInfo dgcInfo = new RMIObjectInfo(
                    new RMIReference(dgcImpl), ClientDGC.DGC_ID, dgcRef, null);
            exportedObjs.add(dgcInfo);
        } catch (Exception ex) {
            // rmi.7D=Unable to initialize DGC.
            throw new Error(Messages.getString("rmi.7D"), ex); //$NON-NLS-1$
        }
    }

    // Queue to wait for objects to be collected.
    private static ReferenceQueue dgcQueue = new ReferenceQueue();

    // Thread removing GC-collected objects from the table of Exported objects.
    private static Thread scav;

    // Number of in-progress calls to the objects in this table.
    private static int activeCallsNum = 0;

    // Number of non system (requiring VM-blocking thread) objects.
    private static int nonSystemObjsNum = 0;

    // lock object for working with active calls
    private static class CallsLock {}
    private static Object callsLock = new CallsLock();

    /**
     * Exports specified remote object through pre-initialized UnicastServerRef.
     * Returns info for exported object. If object has already been exported,
     * ExportException will be thrown. The thread listening for incoming
     * connections will be started.
     *
     * @param obj remote object to be exported
     * @param sref initialized UnicastServerRef to export object through
     * @param useProxyStubs If true then Proxy stubs will be generated if stub
     *        class could not be found in classpath and codebase; if false Proxy
     *        stubs will not be tried (this is needed for
     *        UnicastRemoteObject.exportObject(Remote) method because it
     *        returns RemoteStub class (but Proxy class could not be casted
     *        to it)
     *
     * @return stub for exported object
     *
     * @throws RemoteException if any exception occurred while exporting
     *         specified remote object
     */
    public static Remote exportObject(Remote obj,
                                      UnicastServerRef sref,
                                      boolean useProxyStubs)
            throws RemoteException {
        return exportObject(obj, sref, useProxyStubs, true, false);
    }

    /**
     * Exports specified remote object through pre-initialized UnicastServerRef.
     * Returns info for exported object. If object has already been exported,
     * ExportException will be thrown.
     *
     * @param obj remote object to be exported
     * @param sref initialized UnicastServerRef to export object through
     * @param useProxyStubs If true then Proxy stubs will be generated if stub
     *        class could not be found in classpath and codebase; if false Proxy
     *        stubs will not be tried (this is needed for
     *        UnicastRemoteObject.exportObject(Remote) method because it
     *        returns RemoteStub class (but Proxy class could not be casted
     *        to it)
     * @param startListen if false, ServerSocket listening thread will not be
     *        started (this is used for DGC, for example); otherwise listening
     *        thread will be started and object becomes available for
     *        connections from clients
     * @param isSystem if true then existence of this object will not prevent
     *        VM from exiting (for example, for rmiregistry)
     *
     * @return stub for exported object
     *
     * @throws RemoteException if any exception occurred while exporting
     *         specified remote object
     */
    public static Remote exportObject(Remote obj,
                                      UnicastServerRef sref,
                                      boolean useProxyStubs,
                                      boolean startListen,
                                      boolean isSystem)
            throws RemoteException {
        if (isExported(obj)) {
            // rmi.7B=Object {0} has already been exported.
            throw new ExportException(Messages.getString("rmi.7B", obj)); //$NON-NLS-1$
        }
        Remote stub = sref.exportObject(obj, null, useProxyStubs, startListen,
                isSystem);
        RMIReference rref = new RMIReference(obj, dgcQueue);
        RMIObjectInfo info = new RMIObjectInfo(
                rref, sref.getObjId(), sref, stub);
        exportedObjs.add(info);

        if (scav == null) {
            (scav = (Thread) AccessController.doPrivileged(
                    new CreateThreadAction(new Scavenger(),
                            "Scavenger", false))).start(); //$NON-NLS-1$
        }

        if (isSystem) {
            rref.makeStrong(true);
        } else {
            ++nonSystemObjsNum;
        }
        return stub;
    }

    /**
     * Unexports specified remote object so it becomes unavailable for
     * receiving remote calls. If force parameter is false then the object will
     * be unexported only if there are no pending or in-progress remote calls
     * to it, otherwise (if force parameter is true) the object will be
     * unexported forcibly.
     *
     * @param obj remote object to be unexported
     * @param force if false then specified object will only be unexported if
     *        there are no pending or in-progress calls to it; otherwise
     *        the object will be unexported forcibly (even if there are such
     *        calls)
     *
     * @throws NoSuchObjectException if specified object has not been exported
     *         or has already been unexported
     */
    public static boolean unexportObject(Remote obj, boolean force)
            throws NoSuchObjectException {
        RMIReference ref = new RMIReference(obj);
        RMIObjectInfo info = exportedObjs.getByRef(ref);

        if (info == null) {
            // rmi.7C=Object {0} is not exported.
            throw new NoSuchObjectException(Messages.getString("rmi.7C", obj)); //$NON-NLS-1$
        }
        boolean succeeded = info.sref.unexportObject(force);

        if (succeeded) {
            exportedObjs.removeByRef(ref);

            synchronized (callsLock) {
                if (!info.sref.isSystem()) {
                        --nonSystemObjsNum;
                }
                scavInterrupt();
            }
        }
        return succeeded;
    }

    /**
     * Returns stub for specified remote object, or throws NoSuchObjectException
     * if object was not exported via this class.
     *
     * @param obj remote object for which stub is needed
     *
     * @return stub for specified remote object if it was exported
     *
     * @throws NoSuchObjectException if specified object was not exported via
     *         this class
     */
    public static Remote getStub(Remote obj) throws NoSuchObjectException {
        RMIObjectInfo info = exportedObjs.getByRef(new RMIReference(obj));

        if (info == null) {
            // rmi.7C=Object {0} is not exported.
            throw new NoSuchObjectException(Messages.getString("rmi.7C", obj)); //$NON-NLS-1$
        }
        return info.stub;
    }

    /**
     * Returns true if specified remote object was exported via this class.
     *
     * @param obj remote object to check
     *
     * @return true if specified remote object was exported via this class
     */
    public static boolean isExported(Remote obj) {
        return exportedObjs.containsByRef(new RMIReference(obj));
    }

    /*
     * Returns RMIObjectInfo in the list of exported objects using the given
     * Object ID as a key.
     *
     * @param id Object ID to be used as a key
     *
     * @return RMIObjectInfo found
     */
    static RMIObjectInfo getInfo(ObjID id) {
        return exportedObjs.getById(id);
    }

    /*
     * Increase the number of active calls by one.
     */
    static void addActiveCall() {
        synchronized (callsLock) {
            ++activeCallsNum;

            if (scav == null) {
                (scav = (Thread) AccessController.doPrivileged(
                        new CreateThreadAction(new Scavenger(),
                                "Scavenger", false))).start(); //$NON-NLS-1$
            }
        }
    }

    /*
     * Decrease the number of active calls by one.
     */
    static void removeActiveCall() {
        synchronized (callsLock) {
            --activeCallsNum;
            scavInterrupt();
        }
    }

    /*
     * Interrupts thread removing objects from the table, if the number of
     * active calls and number of exported system objects are both zero.
     */
    private static void scavInterrupt() {
        if (activeCallsNum == 0 && nonSystemObjsNum == 0 && scav != null) {
            AccessController.doPrivileged(new InterruptThreadAction(scav));
            scav = null;
        }
    }


    /*
     * Thread removing objects from the table when they are scheduled for GC.
     * It blocks VM from exiting as it's run with setDaemon(false).
     */
    private static class Scavenger implements Runnable {
        public void run() {
            try {
                do {
                    // this operation blocks the thread
                    RMIReference ref = (RMIReference) dgcQueue.remove();

                    // removes objects from the table
                    RMIObjectInfo info = exportedObjs.removeByRef(ref);

                    synchronized (callsLock) {
                        if (info != null) {
                            if (!info.sref.isSystem()) {
                                --nonSystemObjsNum;
                            }
                        }

                        if (nonSystemObjsNum == 0 && activeCallsNum == 0) {
                            break;
                        }
                    }
                } while (!Thread.interrupted());
            } catch (InterruptedException ie) {
            }
            scav = null;
        }
    }
}
