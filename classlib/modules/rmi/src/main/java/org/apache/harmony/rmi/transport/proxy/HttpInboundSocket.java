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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.harmony.rmi.transport.SocketWrapper;


/**
 * Inbound HTTP socket wrapper.
 *
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public class HttpInboundSocket extends SocketWrapper {

    /**
     * Host to connect to.
     */
    private String host;

    /**
     * Port to connect to.
     */
    private int port;

    /**
     * Constructs this object by wrapping the specified socket.
     *
     * @param   s
     *          Socket to wrap.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public HttpInboundSocket(Socket s)
            throws IOException {
        this(s, null, null);
    }

    /**
     * Constructs this object by wrapping the specified socket
     * with the specified streams.
     *
     * @param   s
     *          Socket to wrap.
     *
     * @param   in
     *          Input stream.
     *
     * @param   out
     *          Output stream.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public HttpInboundSocket(Socket s, InputStream in, OutputStream out)
            throws IOException {
        super(s, in, out);
        host = s.getInetAddress().getHostName();
        port = s.getPort();
        this.in = new HttpInputStream(this.in, true);
        this.out = new HttpOutputStream(this.out, true, host, port);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return ("HttpInboundSocket[" + s.toString() + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + host + ':' + port + ']');
    }
}
