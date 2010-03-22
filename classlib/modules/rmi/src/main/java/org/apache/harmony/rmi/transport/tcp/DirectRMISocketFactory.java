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
package org.apache.harmony.rmi.transport.tcp;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.server.RMISocketFactory;


/**
 * RMISocketFactory implementation for direct connections.
 *
 * @author  Mikhail A. Markov
 *
 * @see RMISocketFactory;
 */
public class DirectRMISocketFactory extends RMISocketFactory
        implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -779073015476675531L;

    /**
     * @see RMISocketFactory.createSocket(String, int)
     */
    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

   /**
     * @see RMISocketFactory.createServerSocket(int)
     */
    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }

    /**
     * Does the same as createSocket(String, int) method does but if during
     * the given timeout the Socket could not be created it's return null.
     */
    public Socket createSocket(String host, int port, int timeout)
            throws IOException {
        Socket s = new Socket();

        try {
            s.connect(new InetSocketAddress(host, port), timeout);
        } catch (SocketTimeoutException ste) {
            s = null;
        }
        return s;
    }
}
