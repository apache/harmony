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
package org.apache.harmony.rmi.transport;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.security.AccessController;

import org.apache.harmony.rmi.common.CreateThreadAction;
import org.apache.harmony.rmi.common.GetBooleanPropAction;
import org.apache.harmony.rmi.common.GetLongPropAction;
import org.apache.harmony.rmi.common.GetStringPropAction;
import org.apache.harmony.rmi.common.RMIProperties;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Endpoint implementation: it contains information about host, port,
 * client-side and server-side socket factories.
 *
 * @author  Mikhail A. Markov
 */
public class Endpoint {

    /** Indicates null client-side factory. */
    public static final int NULL_CSF = 0x00;

    /** Indicates non-null client-side factory. */
    public static final int NONNULL_CSF = 0x01;

    // Host address.
    private final String host;

    // Port number.
    private int port;

    // Client-side socket factory.
    private RMIClientSocketFactory csf;

    // Server-side socket factory.
    private RMIServerSocketFactory ssf;

    // Local host address.
    private static String localHost = getLocalHost();

    // True if local host has been obtained and is not equal to 127.0.0.1
    private static boolean isLocalHostIdentified = false;

    // If non-null then it'll be used as a result of getLocalHost() method call.
    private static String localHostPropVal =
            (String) AccessController.doPrivileged(new GetStringPropAction(
                    RMIProperties.HOSTNAME_PROP));

    // If true then we will forcibly use FQDN by default.
    private static boolean useLocalHostName =
            ((Boolean) AccessController.doPrivileged(new GetBooleanPropAction(
                    RMIProperties.USELOCALHOSTNAME_PROP))).booleanValue();

    /*
     * The time that we will wait to obtain FQDN for this local host (in ms).
     * The default value is 10000 ms (10 sec.).
     */
    private static int localHostNameTimeout =
            ((Long) AccessController.doPrivileged(
                new GetLongPropAction(RMIProperties.LOCALHOSTNAMETIMEOUT_PROP,
                        10000))).intValue();

    /**
     * Constructs Local Endpoint.
     *
     * @param port port number
     * @param csf client-side socket factory
     * @param ssf server-side socket factory
     */
    public Endpoint(int port,
                    RMIClientSocketFactory csf,
                    RMIServerSocketFactory ssf) {
        host = getLocalHost();
        this.port = port;
        this.csf = csf;
        this.ssf = ssf;
    }

    /**
     * Constructs Endpoint.
     *
     * @param host host address/name
     * @param port port number
     * @param csf client-side socket factory
     * @param ssf server-side socket factory
     */
    public Endpoint(String host,
                    int port,
                    RMIClientSocketFactory csf,
                    RMIServerSocketFactory ssf) {
        this.host = host;
        this.port = port;
        this.csf = csf;
        this.ssf = ssf;
    }

    /**
     * Returns Endpoint created from the given Endpoint but having null host
     * (Such Endpoints are used for local endpoints for comparison).
     *
     * @param ep Endpoint to create template from
     *
     * @return created Endpoint
     */
    public static Endpoint createTemplate(Endpoint ep) {
        return new Endpoint(null, ep.port, ep.csf, ep.ssf);
    }

    /**
     * Creates and returns server socket.
     *
     * @return created server socket
     */
    public ServerSocket createServerSocket() throws IOException {
        ServerSocket ss = DefaultRMISocketFactory.getNonNullServerFactory(ssf)
                .createServerSocket(port);

        if (port == 0) {
            port = ss.getLocalPort();
        }
        return ss;
    }

    /**
     * Creates and returns socket.
     *
     * @return created socket
     */
    public Socket createSocket() throws RemoteException {
        Socket s;

        try {
            s = DefaultRMISocketFactory.getNonNullClientFactory(csf)
                    .createSocket(host, port);
        } catch (java.net.UnknownHostException uhe) {
            // rmi.80=Unable to connect to server {0}
            throw new java.rmi.UnknownHostException(
                    Messages.getString("rmi.80", toString()), uhe); //$NON-NLS-1$
        } catch (java.net.ConnectException ce) {
            throw new java.rmi.ConnectException(
                    Messages.getString("rmi.80", toString()), ce); //$NON-NLS-1$
        } catch (IOException ioe) {
            throw new ConnectIOException(
                    Messages.getString("rmi.80", toString()), ioe); //$NON-NLS-1$
        }
        return s;
    }

    /**
     * Returns client-side socket factory of this endpoint.
     *
     * @return client-side socket factory of this endpoint
     */
    public RMIClientSocketFactory getClientSocketFactory() {
        return csf;
    }

    /**
     * Returns server-side socket factory of this endpoint.
     *
     * @return server-side socket factory of this endpoint
     */
    public RMIServerSocketFactory getServerSocketFactory() {
        return ssf;
    }

    /**
     * Returns the port of this endpoint.
     *
     * @return the port of this endpoint
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the host address of this endpoint.
     *
     * @return the host address of this endpoint
     */
    public String getHost() {
        return host;
    }

    /**
     * Compares this Endpoint with another object. Returns true if the given
     * object is an instance of TcpEndpoint and has the same host, port, csf and
     * ssf fields.
     *
     * @return true if objects are equal and false otherwise
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Endpoint)) {
            return false;
        }
        Endpoint anotherEp = (Endpoint) obj;


        if (port != anotherEp.port) {
            return false;
        }

        if ((host == null) ? (host != anotherEp.host)
                : !host.equals(anotherEp.host)) {
            return false;
        }

        if ((csf == null) ? (csf != anotherEp.csf)
                : !csf.equals(anotherEp.csf)) {
            return false;
        }

        if ((ssf == null) ? (ssf != anotherEp.ssf)
                : !ssf.equals(anotherEp.ssf)) {
            return false;
        }
        return true;
    }

    /**
     * Returns hashCode for this Endpoint.
     *
     * @return hashCode for this Endpoint
     */
    public int hashCode() {
        return port;
    }

    /**
     * Writes this Endpoint to the given ObjectOutput.
     *
     * @param out ObjectOutput to write this Endpoint to
     * @param writeCsf do we need to write client-side factory or not
     *
     * @throws IOException if any I/O error occurred during writing
     */
    public void writeExternal(ObjectOutput out, boolean writeCsf)
            throws IOException {
        if (writeCsf) {
            if (csf == null) {
                out.writeByte(NULL_CSF);
            } else {
                out.writeByte(NONNULL_CSF);
            }
        }
        out.writeUTF(host);
        out.writeInt(port);

        if (writeCsf && csf != null) {
            out.writeObject(csf);
        }
    }

    /**
     * Reads data for creating Endpoint object from the specified input stream.
     *
     * @param in the stream to read data from
     * @param readCsf do we need to read client-side factory or not
     *
     * @return created Endpoint
     *
     * @throws IOException if any I/O error occurred
     * @throws ClassNotFoundException if class could not be loaded by current
     *         class loader
     */
    public static Endpoint readExternal(ObjectInput in, boolean readCsf)
            throws IOException, ClassNotFoundException {
        int inCsf = NULL_CSF;

        if (readCsf) {
            inCsf = in.readUnsignedByte();
        }
        String host = (String) in.readUTF();
        int port = in.readInt();
        RMIClientSocketFactory csf = null;

        if (readCsf && inCsf == NONNULL_CSF) {
            csf = (RMIClientSocketFactory) in.readObject();
        }
        return new Endpoint(host, port, csf, null);
    }

    /**
     * Returns string representation of this Endpoint.
     *
     * @return string representation of this Endpoint
     */
    public String toString() {
        String str = "[" + host + ":" + port; //$NON-NLS-1$ //$NON-NLS-2$

        if (csf != null) {
            str += ", csf: " + csf; //$NON-NLS-1$
        }

        if (ssf != null) {
            str += ", ssf: " + ssf; //$NON-NLS-1$
        }
        return str  + "]"; //$NON-NLS-1$
    }

    /*
     * Returns local host. If local host was already obtained, then return it
     * as a result. Otherwise perform the following steps:
     * 1) Reads java.rmi.server.hostname property if it is not equal to zero
     *    then returns it's value
     * 2) Obtains local host by calling InetAddress.getLocalHost()
     * 3) If java.rmi.server.useLocalHostname property is set to true then tries
     *    to obtain FQDN (fully qualified domain name) for hostname obtained
     *    in step 2 and returns it as a result.
     * 4) If property above is not set (or set to false) then returns the result
     *    of InetAddress.getLocalHost().getHostAddress() method call.
     *
     * @return local host
     */
    private static synchronized String getLocalHost() {
        if (isLocalHostIdentified) {
            return localHost;
        }
        if (localHostPropVal != null) {
            isLocalHostIdentified = true;
            localHost = localHostPropVal;
            return localHost;
        }

        try {
            InetAddress iaddr = InetAddress.getLocalHost();
            byte[] addr = iaddr.getAddress();

            if (useLocalHostName) {
                localHost = getFQDN(iaddr);
            } else {
                localHost = iaddr.getHostAddress();
            }
            isLocalHostIdentified = true;
        } catch (Exception ex) {
            localHost = null;
        }
        return localHost;
    }

    /*
     * Returns Fully Qualified Domain Name (FQDN) for the specified InetAddress.
     * It'll try to obtain this name no longer then the time specified
     * in harmony.rmi.transport.tcp.localHostNameTimeOut property.
     *
     * @param iaddr InetAddress to obtain FQDN
     *
     * @return obtained FQDN for the given InetAddress
     */
    private static String getFQDN(InetAddress iaddr)
            throws UnknownHostException {
        String hostName = iaddr.getHostName();

        if (hostName.indexOf('.') >= 0) {
            // contains dots (so we think that it is a FQDN already)
            return hostName;
        }

        // does not contain dots, so we presume that getHostName()
        // did not return fqdn
        String addr = iaddr.getHostAddress();
        FQDNGetter getter = new FQDNGetter(addr);
        Thread fqdnThread = (Thread) AccessController.doPrivileged(
                new CreateThreadAction(getter, "FQDN getter.", true)); //$NON-NLS-1$

        try {
            synchronized (getter) {
                fqdnThread.start();
                getter.wait(localHostNameTimeout);
            }
        } catch (InterruptedException ie) {
        }
        String fqdn = getter.getFQDN();

        if (fqdn == null || fqdn.indexOf('.') < 0) {
            return addr;
        }
        return fqdn;
    }


    /*
     * Obtains Fully Qualified Domain Name.
     */
    private static class FQDNGetter implements Runnable {
        private String addr;
        private String name;

        FQDNGetter(String addr) {
            this.addr = addr;
        }

        public void run() {
            try {
                name = InetAddress.getByName(addr).getHostName();
            } catch (UnknownHostException uhe) {
            } finally {
                synchronized (this) {
                    notify();
                }
            }
        }

        private String getFQDN() {
            return name;
        }
    }
}
