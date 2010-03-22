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
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;


/**
 * Template of a socket factory for proxy connections.
 *
 * @author  Vasily Zakharov
 */
public abstract class ProxyRMISocketFactory extends RMISocketFactory
        implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 4993249257557742620L;

    /**
     * {@inheritDoc}
     *
     * Equivalent to <code>createSocket(new Proxy(), host, port)</code>
     */
    public final Socket createSocket(String host, int port) throws IOException {
        return createSocket(new Proxy(), host, port);
    }

    /**
     * Creates socket for specified host and port using the specified proxy
     * configuration.
     *
     * @param   proxy
     *          Proxy configuration.
     *
     * @param   host
     *          Host to connect to.
     *
     * @param   port
     *          Port to connect to.
     *
     * @return  Created socket.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public abstract Socket createSocket(Proxy proxy, String host, int port)
        throws IOException;
}
