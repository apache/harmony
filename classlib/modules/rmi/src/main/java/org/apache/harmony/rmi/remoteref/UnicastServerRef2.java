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
package org.apache.harmony.rmi.remoteref;

import java.io.ObjectOutput;
import java.rmi.server.ObjID;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;


/**
 * Implementation of server-side handle for remote objects using custom
 * socket factories.
 *
 * @author  Mikhail A. Markov
 */
public class UnicastServerRef2 extends UnicastServerRef {

    private static final long serialVersionUID = -3460099617464801595L;

    /**
     * Constructs default UnicastServerRef2 listening on anonymous port and
     * using default client and server socket factories.
     */
    public UnicastServerRef2() {
        super();
    }

    /**
     * Constructs UnicastServerRef2 listening on the port specified and
     * having the given client and server socket factories.
     *
     * @param port port where this UnicastServerRef2 will listen for connections
     * @param csf client-side socket factory for creating client sockets
     * @param ssf server-side socket factory for creating server sockets
     */
    public UnicastServerRef2(int port,
                            RMIClientSocketFactory csf,
                            RMIServerSocketFactory ssf) {
        this(port, csf, ssf, new ObjID());
    }

    /**
     * Constructs UnicastServerRef2 listening on the port specified,
     * using specified client and server socket factories and
     * having the given ObjID.
     *
     * @param port port where this UnicastServerRef2 will listen for connections
     * @param csf client-side socket factory for creating client sockets
     * @param ssf server-side socket factory for creating server sockets
     * @param objId Object ID of remote object
     */
    public UnicastServerRef2(int port,
                             RMIClientSocketFactory csf,
                             RMIServerSocketFactory ssf,
                             ObjID objId) {
        super(port, csf, ssf, objId);
    }

    /**
     * @see RemoteRef.getRefClass(ObjectOutput)
     */
    public String getRefClass(ObjectOutput out) {
        return "UnicastServerRef2"; //$NON-NLS-1$
    }
}
