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
package org.apache.harmony.rmi.registry;

import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteServer;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.AccessException;
import java.rmi.registry.Registry;

import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.remoteref.UnicastServerRef;
import org.apache.harmony.rmi.remoteref.UnicastServerRef2;
import org.apache.harmony.rmi.server.ExportManager;
import org.apache.harmony.rmi.server.ServerConnectionManager;


/**
 * Transient Registry interface implementation.
 * It does not extend UnicastRemoteObject because all constructors of
 * UnicastRemoteObject export itself, but we need a special export for
 * objects with well-known ObjID numbers.
 *
 * @author  Mikhail A. Markov
 */
public class RegistryImpl extends RemoteServer implements Registry {

    private static final long serialVersionUID = 2202752560834503694L;

    /** Registry Object ID. */
    public static final ObjID regId = new ObjID(ObjID.REGISTRY_ID);

    /* Bindings table. */
    private transient Hashtable table = new Hashtable();

    /**
     * Starts rmiregistry on the specified port. If no port specified then
     * registry will be started on default port. This method ends with a
     * blocking construction to not let the VM exit.
     *
     * @param args arguments which can contain port number
     */
    public static void main(String[] args) throws Exception {
        String usage =
            "Usage: java org.apache.harmony.rmi.registry.RegistryImpl <port>"; //$NON-NLS-1$
        int port = REGISTRY_PORT;

        if (args.length > 1) {
            System.err.println(usage);
            System.exit(-1);
        } else if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                // rmi.console.1B=Invalid port number {0}
                System.out.println(Messages.getString("rmi.console.1B", args[0])); //$NON-NLS-1$
                System.out.println(usage);
                System.exit(-1);
            }
        }

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
        Registry reg = new RegistryImpl(port, null, null);

        // do not let VM exit
        Object obj = new Object();  //$NON-LOCK-1$

        synchronized(obj) {
            obj.wait();
        }
    }

    /**
     * Constructs registry listening on default port and using default client
     * and server socket factories for RMI calls.
     *
     * @throws RemoteException if the registry could not be exported
     */
    public RegistryImpl() throws RemoteException {
        this (REGISTRY_PORT, null, null);
    }

    /**
     * Constructs registry listening on the specified port and using specified
     * client and server socket factories for RMI calls.
     *
     * @param port port to accept connections
     * @param csf client-side socket factory
     * @param ssf server-side socket factory
     *
     * @throws RemoteException if the registry could not be exported
     */
    public RegistryImpl(int port,
                        RMIClientSocketFactory csf,
                        RMIServerSocketFactory ssf)
            throws RemoteException {
        UnicastServerRef sref;

        if (csf != null || ssf != null) {
            sref = new UnicastServerRef2(port, csf, ssf, regId);
        } else {
            sref = new UnicastServerRef(port, csf, ssf, regId);
        }
        ExportManager.exportObject(this, sref, false, true, true);
    }

    /**
     * @see Registry.rebind(String, Remote)
     */
    public void rebind(String name, Remote obj)
            throws RemoteException, AccessException {
        if (name == null) {
            // rmi.5D=name could not be null.
            throw new NullPointerException(Messages.getString("rmi.5D")); //$NON-NLS-1$
        }

        if (obj == null) {
            // rmi.5C=obj could not be null.
            throw new NullPointerException(Messages.getString("rmi.5C")); //$NON-NLS-1$
        }
        checkAccess("RegistryImpl.rebind"); //$NON-NLS-1$
        table.put(name, obj);
    }

    /**
     * @see Registry.bind(String, Remote)
     */
    public void bind(String name, Remote obj)
            throws RemoteException, AlreadyBoundException, AccessException {
        if (name == null) {
            // rmi.5D=name could not be null.
            throw new NullPointerException(Messages.getString("rmi.5D")); //$NON-NLS-1$
        }

        if (obj == null) {
            // rmi.5C=obj could not be null.
            throw new NullPointerException(Messages.getString("rmi.5C")); //$NON-NLS-1$
        }

        if (table.containsKey(name)) {
            // rmi.5E=There is already binding to the name {0}.
            throw new AlreadyBoundException(Messages.getString("rmi.5E", name)); //$NON-NLS-1$
        }
        checkAccess("RegistryImpl.bind"); //$NON-NLS-1$
        table.put(name, obj);
    }

    /**
     * @see Registry.lookup(String)
     */
    public Remote lookup(String name)
            throws RemoteException, NotBoundException, AccessException {
        if (name == null) {
            // rmi.5D=name could not be null.
            throw new NullPointerException(Messages.getString("rmi.5D")); //$NON-NLS-1$
        }
        Remote ref = (Remote) table.get(name);

        if (ref == null) {
            // rmi.5F=Name {0} is not associated with any remote reference.
            throw new NotBoundException(Messages.getString("rmi.5F", name)); //$NON-NLS-1$

        }
        return ref;
    }

    /**
     * @see Registry.unbind(String)
     */
    public void unbind(String name)
            throws RemoteException, NotBoundException, AccessException {
        if (name == null) {
            // rmi.5D=name could not be null.
            throw new NullPointerException(Messages.getString("rmi.5D")); //$NON-NLS-1$
        }
        checkAccess("RegistryImpl.unbind"); //$NON-NLS-1$

        if (table.remove(name) == null) {
            // rmi.5F=Name {0} is not associated with any remote reference.
            throw new NotBoundException(Messages.getString("rmi.5F", name)); //$NON-NLS-1$
        }
    }

    /**
     * @see Registry.list()
     */
    public String[] list()
            throws RemoteException, AccessException {
        String[] names = new String[table.size()];
        Enumeration e = table.keys();

        for (int i = names.length - 1; i >= 0; --i) {
            names[i] = (String) e.nextElement();
        }
        return names;
    }

    /*
     * Checks if request came from local host. In this case the method will
     * successfully return. If request came from non-local host then
     * AccessException will be returned.
     *
     * @param registryMethod method requested access check
     *
     * @throws AccessException if request came from non-local host
     */
    private static void checkAccess(String registryMethod)
            throws AccessException {
        String hostName = ServerConnectionManager.getClientHost();

        try {
            if (!RMIUtil.isLocalHost(hostName)) {
                // rmi.60={0} from non-local host {1} is not allowed
                throw new AccessException(Messages.getString("rmi.60", registryMethod, hostName)); //$NON-NLS-1$
            }
        } catch (UnknownHostException uhe) {
            // rmi.61={0} from unknown host is not allowed
            throw new AccessException(Messages.getString("rmi.61", registryMethod), uhe); //$NON-NLS-1$
        }
    }
}
