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

package java.rmi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.harmony.rmi.internal.nls.Messages;

public final class Naming {

    // This class could not be instantiated.
    private Naming() {
    }

    public static String[] list(String name) throws RemoteException, MalformedURLException {
        if (name == null) {
            // rmi.00=URL could not be null.
            throw new NullPointerException(Messages.getString("rmi.00")); //$NON-NLS-1$
        }
        RegistryURL url = getRegistryURL(name, true);
        Registry reg = LocateRegistry.getRegistry(url.host, url.port);
        String[] names = reg.list();
        String regName = "//" + ((url.host == null) ? "" : url.host) + ":" + url.port + "/"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        for (int i = 0; i < names.length; ++i) {
            names[i] = regName + names[i];
        }
        return names;
    }

    public static void rebind(String name, Remote obj) throws RemoteException,
            MalformedURLException {
        if (name == null) {
            // rmi.00=URL could not be null.
            throw new NullPointerException(Messages.getString("rmi.00")); //$NON-NLS-1$
        }
        RegistryURL url = getRegistryURL(name, false);
        Registry reg = LocateRegistry.getRegistry(url.host, url.port);
        reg.rebind(url.name, obj);
    }

    public static void unbind(String name) throws RemoteException, NotBoundException,
            MalformedURLException {
        if (name == null) {
            // rmi.00=URL could not be null.
            throw new NullPointerException(Messages.getString("rmi.00")); //$NON-NLS-1$
        }
        RegistryURL url = getRegistryURL(name, false);
        Registry reg = LocateRegistry.getRegistry(url.host, url.port);
        reg.unbind(url.name);
    }

    public static void bind(String name, Remote obj) throws AlreadyBoundException,
            MalformedURLException, RemoteException {
        if (obj == null) {
            throw new NullPointerException(Messages.getString("rmi.5C")); //$NON-NLS-1$
        }

        if (name == null) {
            // rmi.00=URL could not be null.
            throw new NullPointerException(Messages.getString("rmi.00")); //$NON-NLS-1$
        }
        RegistryURL url = getRegistryURL(name, false);
        Registry reg = LocateRegistry.getRegistry(url.host, url.port);
        reg.bind(url.name, obj);
    }

    public static Remote lookup(String name) throws NotBoundException, MalformedURLException,
            RemoteException {
        if (name == null) {
            // rmi.00=URL could not be null.
            throw new NullPointerException(Messages.getString("rmi.00")); //$NON-NLS-1$
        }
        RegistryURL url = getRegistryURL(name, false);
        Registry reg = LocateRegistry.getRegistry(url.host, url.port);
        return reg.lookup(url.name);
    }

    /*
     * Parse the given name and returns URL containing parsed parameters.
     */
    private static RegistryURL getRegistryURL(String strUrl, boolean ignoreEmptyNames)
            throws MalformedURLException {
        URI uri;

        try {
            uri = new URI(strUrl);
        } catch (URISyntaxException use) {
            // rmi.01=Invalid URL "{0}":{1}
            throw new MalformedURLException(Messages.getString("rmi.01", strUrl, use)); //$NON-NLS-1$
        }
        String prot = uri.getScheme();

        if ((prot != null) && !prot.toLowerCase().equals("rmi")) { //$NON-NLS-1$
            // rmi.02=Non-rmi protocol in URL "{0}": {1}
            throw new MalformedURLException(Messages.getString("rmi.02", strUrl, prot)); //$NON-NLS-1$
        }

        if (uri.getUserInfo() != null) {
            // rmi.03=Invalid character ('@') in URL "{0}" host part.
            throw new MalformedURLException(Messages.getString("rmi.03", strUrl)); //$NON-NLS-1$
        } else if (uri.getQuery() != null) {
            // rmi.04=Invalid character ('?') in URL "{0}" name part.
            throw new MalformedURLException(Messages.getString("rmi.04", strUrl)); //$NON-NLS-1$
        } else if (uri.getFragment() != null) {
            // rmi.05=Invalid character ('\#') in URL "{0}" name part.
            throw new MalformedURLException(Messages.getString("rmi.05", strUrl)); //$NON-NLS-1$
        }
        int port = uri.getPort();
        String auth = uri.getAuthority();

        if (auth != null && auth.startsWith(":") && auth.length() != 1) { //$NON-NLS-1$
            // to handle URLs like "rmi://:1099/xxx"
            try {
                port = Integer.parseInt(auth.substring(1));
            } catch (NumberFormatException nfe) {
                // rmi.06=Invalid port number in URL "{0}": {0}
                throw new MalformedURLException(
                        Messages.getString("rmi.06", strUrl, auth.substring(1))); //$NON-NLS-1$
            }
        }

        if (port == -1) {
            port = Registry.REGISTRY_PORT;
        }
        String path = uri.getPath();

        if (!ignoreEmptyNames) {
            if (path == null || path.length() == 0) {
                // rmi.07=Name could not be empty (URL: "{0}").
                throw new MalformedURLException(Messages.getString("rmi.07", strUrl)); //$NON-NLS-1$
            }
        }

        if (path != null && path.startsWith("/")) { //$NON-NLS-1$
            path = path.substring(1);
        }
        String host = uri.getHost();

        if (host == null) {
            host = "localhost"; //$NON-NLS-1$
        }
        return new RegistryURL(host, port, path);
    }

    /**
     * Auxiliary class holding information about host, port and name.
     */
    private static class RegistryURL {

        // Host name.
        String host;

        // Port number.
        int port;

        // bind name
        String name;

        /**
         * Constructs RegistryURL from the given host, port and bind name.
         */
        RegistryURL(String host, int port, String name) {
            this.host = host;
            this.port = port;
            this.name = name;
        }
    }
}
