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
package java.rmi.registry;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteRef;

import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.registry.RegistryImpl;
import org.apache.harmony.rmi.remoteref.UnicastRef;
import org.apache.harmony.rmi.remoteref.UnicastRef2;


/**
 * @com.intel.drl.spec_ref
 * This class could not be instantiated.
 *
 * @author  Mikhail A. Markov
 */
public final class LocateRegistry {

    // This class could not be instantiated.
    private LocateRegistry() {
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Registry createRegistry(int port,
                                          RMIClientSocketFactory csf,
                                          RMIServerSocketFactory ssf)
            throws RemoteException {
        if (port < 0) {
            // rmi.15=Port value out of range: {0}
            throw new IllegalArgumentException(Messages.getString("rmi.15", port)); //$NON-NLS-1$
        }
        return new RegistryImpl(port, csf, ssf);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Registry getRegistry(String host,
                                       int port,
                                       RMIClientSocketFactory csf)
            throws RemoteException {
        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException uhe) {
                host = "localhost"; //$NON-NLS-1$
            }
        }

        if (port <= 0) {
            port = Registry.REGISTRY_PORT;
        }

        try {
            Class regClass = Class.forName(
                    "org.apache.harmony.rmi.registry.RegistryImpl_Stub"); //$NON-NLS-1$
            RemoteRef ref;

            if (csf == null) {
                ref = new UnicastRef(host, port, new ObjID(ObjID.REGISTRY_ID));
            } else {
                ref = new UnicastRef2(host, port, csf,
                        new ObjID(ObjID.REGISTRY_ID));
            }
            return (Registry) regClass.getConstructor(
                    new Class[] { RemoteRef.class }).newInstance(
                            new Object[] { ref });
        } catch (Exception ex) {
            // rmi.16=Unable to get registry.
            throw new RemoteException(Messages.getString("rmi.16"), ex); //$NON-NLS-1$
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Registry getRegistry(String host, int port)
            throws RemoteException {
        return getRegistry(host, port, null);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Registry getRegistry(String host) throws RemoteException {
        return getRegistry(host, Registry.REGISTRY_PORT);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Registry getRegistry(int port) throws RemoteException {
        return getRegistry(null, port);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Registry createRegistry(int port) throws RemoteException {
        return createRegistry(port, null, null);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Registry getRegistry() throws RemoteException {
        return getRegistry(Registry.REGISTRY_PORT);
    }
}
