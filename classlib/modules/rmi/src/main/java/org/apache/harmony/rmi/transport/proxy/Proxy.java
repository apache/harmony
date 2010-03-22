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
 * @author  Vasily Zakharov
 */
package org.apache.harmony.rmi.transport.proxy;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.security.AccessController;

import org.apache.harmony.rmi.common.GetBooleanPropAction;
import org.apache.harmony.rmi.common.GetLongPropAction;
import org.apache.harmony.rmi.common.GetStringPropAction;
import org.apache.harmony.rmi.common.RMIConstants;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Provides access to Java proxy system properties.
 *
 * Note: instances of this class and values returned by their methods
 * should not be stored longer than is required to establish a particular
 * connection. Instead, a new instance should be created and the methods
 * of that instance called each time new connection is established.
 *
 * @author  Vasily Zakharov
 */
public final class Proxy implements ProxyConstants {

    /**
     * HTTP proxy host name.
     */
    private final String proxyHost;

    /**
     * HTTP proxy port number.
     */
    private final int proxyPort;

    /**
     * If proxy is set.
     */
    private final boolean proxySet;

    /**
     * Should we enable direct (non-proxy) HTTP connections.
     */
    private final boolean enableDirect;

    /**
     * Creates instance of this class.
     *
     * Note: instances of this class and values returned by their methods
     * should not be stored longer than is required to establish a particular
     * connection. Instead, a new instance should be created and the methods
     * of that instance called each time new connection is established.
     */
    public Proxy() {
        proxyHost = getProxyHost();

        if (proxyHost != null) {
            proxySet = true;
            proxyPort = getProxyPort();
            enableDirect = false;
        } else {
            proxySet = false;
            proxyPort = (-1);
            enableDirect = isDirectEnabled();
        }
        // rmi.log.11D=Proxy configuration:
        // rmi.log.11E=proxy disabled, direct HTTP connections
        if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
            proxyTransportLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.11D") //$NON-NLS-1$
                    + (proxySet ? (proxyHost + ':' + proxyPort)
                            : (Messages.getString("rmi.log.11E") //$NON-NLS-1$
                                + (enableDirect ? "enabled" : "disabled") //$NON-NLS-1$ //$NON-NLS-2$
                                + '.')));
        }
    }

    /**
     * Returns proxy host name or <code>null</code> if proxy host is not set.
     *
     * @return  Proxy host name or <code>null</code> if proxy host is not set.
     */
    public String getHost() {
        return proxyHost;
    }

    /**
     * Returns proxy port number or {@link #HTTP_DEFAULT_PORT} if proxy port
     * is not set or <code>-1</code> if proxy host is not set.
     *
     * @return  Proxy port number or {@link #HTTP_DEFAULT_PORT} if proxy port
     *          is not set or <code>-1</code> if proxy host is not set.
     */
    public int getPort() {
        return proxyPort;
    }

    /**
     * Returns <code>true</code> if proxy host is set in system environment,
     * <code>false</code> otherwise.
     *
     * @return  <code>true</code> if proxy host is set in system environment,
     *          <code>false</code> otherwise.
     */
    public boolean isSet() {
        return proxySet;
    }

    /**
     * Returns new socket connected to specified host and port, probably
     * through a proxy (if proxy is set). If proxy is not set, then if direct
     * HTTP connections are enabled, connection is established directly,
     * otherwise {@link IOException} is thrown.
     *
     * @param   host
     *          Host to connect to.
     *z
     * @param   port
     *          Port to connect to.
     *
     * @return  New socket connected to the specified host and port,
     *          probably through a proxy.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public Socket getSocket(String host, int port) throws IOException {
        if (proxySet) {
            return new Socket(proxyHost, proxyPort);
        } else if (enableDirect) {
            return new Socket(host, port);
        } else {
            // rmi.81=HTTP proxy is not set
            throw new NoRouteToHostException(Messages.getString("rmi.81")); //$NON-NLS-1$
        }
    }

    /**
     * Accesses {@link #PROXY_HOST_PROP} system property
     * and retrieves the proxy host name.
     *
     * @return  Proxy host name or <code>null</code> if proxy host is not set.
     */
    private static String getProxyHost() {
        String host = (String) AccessController.doPrivileged(
                                    new GetStringPropAction(PROXY_HOST_PROP));

        if ((host == null) || (host.length() < 1)) {
            return null;
        }

        host = host.trim();

        return ((host.length() < 1) ? null : host);
    }

    /**
     * Accesses {@link #PROXY_PORT_PROP} system property
     * and retrieves the proxy port number.
     *
     * @return  Proxy port number or {@link #HTTP_DEFAULT_PORT}
     *          if proxy port is not set.
     */
    private static int getProxyPort() {
        return ((Long) AccessController.doPrivileged(new GetLongPropAction(
                PROXY_PORT_PROP, RMIConstants.HTTP_DEFAULT_PORT))).intValue();
    }

    /**
     * Accesses {@link #ENABLE_DIRECT_HTTP_PROP} system property
     * to find out if direct connections are allowed.
     *
     * @return  <code>true</code> if direct connections are allowed,
     *          <code>false</code> otherwise.
     */
    private static boolean isDirectEnabled() {
        return ((Boolean) AccessController.doPrivileged(
                new GetBooleanPropAction(ENABLE_DIRECT_HTTP_PROP)))
                        .booleanValue();
    }
}
