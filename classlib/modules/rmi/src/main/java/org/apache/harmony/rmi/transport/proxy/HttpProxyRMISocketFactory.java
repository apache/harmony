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
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
package org.apache.harmony.rmi.transport.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessController;

import org.apache.harmony.rmi.common.GetBooleanPropAction;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Socket factory for HTTP proxy connections. Returns {@link HttpOutboundSocket}
 * for client and {@link HttpServerSocket} for server sockets.
 *
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public class HttpProxyRMISocketFactory extends ProxyRMISocketFactory
        implements ProxyConstants  {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -8113740863920118588L;

    /**
     * {@inheritDoc}
     */
    public Socket createSocket(Proxy proxy, String host, int port)
            throws IOException {
        Socket s;

        // Check if plain HTTP is disabled.
        if (((Boolean) AccessController.doPrivileged(new GetBooleanPropAction(
                DISABLE_PLAIN_HTTP_PROP))).booleanValue()) {
            if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.131=Plain HTTP connections disabled, trying CGI connection.
                proxyTransportLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.131")); //$NON-NLS-1$
            }
        } else {
            try {
                // Try plain HTTP connection.
                s = new HttpOutboundSocket(proxy, host, port, false);

                if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.132=Plain HTTP connection to [{0}:{1}] from port {2} succeeded.
                    proxyTransportLog.log(RMILog.VERBOSE,
                            Messages.getString("rmi.log.132", new Object[]{host, port, s.getLocalPort()})); //$NON-NLS-1$
                }

                return s;
            } catch (IOException e) {
                if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.133=Plain HTTP connection to [{0}:{1}] failed: {2}. Trying CGI connection.
                    proxyTransportLog.log(RMILog.VERBOSE,
                            Messages.getString("rmi.log.133", //$NON-NLS-1$
                            new Object[]{ host, port, e}));
                }
            }
        }

        try {
            // Try CGI HTTP connection.
            s = new HttpOutboundSocket(proxy, host, port, true);

            if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.134=CGI HTTP connection to [{0}:{1}] from port {2} succeeded.
                proxyTransportLog.log(RMILog.VERBOSE,
                        Messages.getString("rmi.log.134", //$NON-NLS-1$
                                new Object[]{host, port, s.getLocalPort()}));
            }
            return s;
        } catch (IOException e) {
            if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.135=CGI HTTP connection to [{0}:{1}] failed: {2}
                proxyTransportLog.log(RMILog.VERBOSE,
                        Messages.getString("rmi.log.135", //$NON-NLS-1$
                        new Object[]{ host, port, e}));
            }
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ServerSocket createServerSocket(int port) throws IOException {
        return new HttpServerSocket(port);
    }
}
