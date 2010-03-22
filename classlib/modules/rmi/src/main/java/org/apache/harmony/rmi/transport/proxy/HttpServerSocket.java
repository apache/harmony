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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.transport.SocketWrapper;


/**
 * HTTP server socket, detects connection type
 * and forwards the HTTP requests to {@link HttpInboundSocket}.
 *
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public class HttpServerSocket extends ServerSocket implements ProxyConstants {

    /**
     * Creates this socket bound to the specified port.
     *
     * @param   port
     *          Port.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public HttpServerSocket(int port) throws IOException {
        super(port);
    }

    /**
     * Creates this socket bound to the specified port,
     * with the specified backlog.
     *
     * @param   port
     *          Port.
     *
     * @param   backlog
     *          Backlog.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public HttpServerSocket(int port, int backlog) throws IOException {
        super(port, backlog);
    }

    /**
     * {@inheritDoc}
     */
    public Socket accept() throws IOException {
        Socket s = super.accept();

        if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.11F=Inbound connection from [{0}:{1}] to port {2} detected.
            proxyTransportLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.11F", //$NON-NLS-1$
                    new Object[]{s.getInetAddress().getHostName(), s.getPort(),
                        s.getLocalPort()}));
        }

        // Detect if incoming request is using HTTP or direct socket.
        //
        // Note: here we ignore the following for simplicity:
        //          Ignore CRLF if it's present before the POST line.
        //          (See RFC 2616 4.1)
        //
        // Note: Theoretically, we SHOULD return
        //          the status code 501 (Not Implemented)
        //          if the method is not POST (the only method we support).
        //          (See RFC 2616 5.1.1)
        //          But we ignore this, since we need to fall back to direct
        //          connection if HTTP connection cannot be established.
        BufferedInputStream in = new BufferedInputStream(s.getInputStream());
        byte[] buffer = new byte[HTTP_REQUEST_SIGNATURE_LENGTH];
        in.mark(HTTP_REQUEST_SIGNATURE_LENGTH);

        // Use read(), not read(byte[], int, int)
        // because we need a blocking read operation here.
        for (int i = 0; i < HTTP_REQUEST_SIGNATURE_LENGTH; i++) {
            int c = in.read();

            if (c < 0) {
                break;
            }
            buffer[i] = (byte) c;
        }
        boolean isHttp = new String(buffer).equals(HTTP_REQUEST_SIGNATURE);
        in.reset();

        if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
            proxyTransportLog.log(RMILog.VERBOSE,
                    Messages.getString("rmi.log.120", new String(buffer)));//$NON-NLS-1$
        }

        if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.121=Direct socket
            // rmi.log.122= connection from [{0}:{1}] to port {2} detected.
            proxyTransportLog.log(RMILog.VERBOSE, (isHttp ? "HTTP" : Messages //$NON-NLS-1$
                    .getString("rmi.log.121")) //$NON-NLS-1$
                    + Messages.getString("rmi.log.122", //$NON-NLS-1$ 
                            new Object[] { s.getInetAddress().getHostName(),
                                    s.getPort(), s.getLocalPort() }));
        }

        // Direct socket must be wrapped to avoid losing already read data.
        return (isHttp ? new HttpInboundSocket(s, in, null)
                       : new SocketWrapper(s, in, null));
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return ("HttpServerSocket[" + super.toString() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
