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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.remoteref.UnicastServerRef;
import org.apache.harmony.rmi.remoteref.UnicastServerRef2;
import org.apache.harmony.rmi.server.ExportManager;


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 */
public class UnicastRemoteObject extends RemoteServer {

    private static final long serialVersionUID = 4974527148936298033L;
    private int port;
    private RMIClientSocketFactory csf = null;
    private RMIServerSocketFactory ssf = null;

    /**
     * @com.intel.drl.spec_ref
     */
    protected UnicastRemoteObject(int port,
                                  RMIClientSocketFactory csf,
                                  RMIServerSocketFactory ssf)
            throws RemoteException {
        this.csf = csf;
        this.ssf = ssf;
        this.port = port;
        exportObject(this, port, csf, ssf);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    protected UnicastRemoteObject(int port) throws RemoteException {
        this.port = port;
        exportObject(this, port);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    protected UnicastRemoteObject() throws RemoteException {
        this(0);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Object clone() throws CloneNotSupportedException {
        try {
            UnicastRemoteObject clonedObj = (UnicastRemoteObject) super.clone();
            clonedObj.export();
            return clonedObj;
        } catch (RemoteException re) {
            // rmi.1A=Unable to clone the object
            throw new ServerCloneException(Messages.getString("rmi.1A"), re); //$NON-NLS-1$
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Remote exportObject(Remote obj,
                                      int port,
                                      RMIClientSocketFactory csf,
                                      RMIServerSocketFactory ssf)
            throws RemoteException {
        return exportObject(obj, port, csf, ssf, true);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static RemoteStub exportObject(Remote obj) throws RemoteException {
        return (RemoteStub) exportObject(obj, 0, null, null, false);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Remote exportObject(Remote obj, int port)
            throws RemoteException {
        return exportObject(obj, port, null, null, true);
    }

    /*
     * Exports the given remote object.
     *
     * @param obj Remote object to be exported.
     * @param port Port to export object on.
     * @param csf Client-side socket factory
     * @param ssf Server-side socket factory
     * @param useProxyStubs If true then Proxy stubs will be generated if stub
     *        class could not be found in classpath and codebase; if false Proxy
     *        stubs will not be tried (this is needed for exportObject(Remote)
     *        method because it returns RemoteStub class (but Proxy class could
     *        not be casted to it)
     *
     * @return stub for exported object
     *
     * @throws RemoteException if any error occurred while exporting object
     */
    private static Remote exportObject(Remote obj,
                                       int port,
                                       RMIClientSocketFactory csf,
                                       RMIServerSocketFactory ssf,
                                       boolean useProxyStubs)
            throws RemoteException {
        UnicastServerRef sref;

        if (csf != null || ssf != null) {
            sref = new UnicastServerRef2(port, csf, ssf);
        } else {
            sref = new UnicastServerRef(port, csf, ssf);
        }
        Remote stub = ExportManager.exportObject(obj, sref,
                useProxyStubs);

        if (obj instanceof UnicastRemoteObject) {
            ((UnicastRemoteObject) obj).ref = sref;
        }
        return stub;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static boolean unexportObject(Remote obj, boolean force)
            throws NoSuchObjectException {
        return ExportManager.unexportObject(obj, force);
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        export();
    }

    /*
     * Exports this UnicastRemoteObject from pre-initialized fields. This method
     * is used by readObject() and clone() methods.
     */
    private void export() throws RemoteException {
        if (csf != null || ssf != null) {
            exportObject(this, port, csf, ssf);
        } else {
            exportObject(this, port);
        }
    }
}
