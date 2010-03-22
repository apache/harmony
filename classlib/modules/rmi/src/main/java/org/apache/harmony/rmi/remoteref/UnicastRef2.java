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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.rmi.server.ObjID;
import java.rmi.server.RMIClientSocketFactory;

import org.apache.harmony.rmi.transport.Endpoint;


/**
 * Handle of remote objects using custom client socket factory.
 *
 * @author  Mikhail A. Markov
 */
public class UnicastRef2 extends UnicastRef {

    private static final long serialVersionUID = 1358245507919642056L;

    /**
     * Default constructor: constructs an empty instance of this class.
     */
    public UnicastRef2() {
        super();
    }

    /**
     * Constructs UnicastRef2.
     *
     * @param host host name where remote object impl. is located.
     * @param port port on remote host where remote object accepts RMI calls
     * @param objId Object ID of remoteObject
     * @param csf client-side socket factory; if null - then default socket
     *        factory will be used (call RMISocketFactory.getSocketFactory
     *        method and if it returns null, then call
     *        RMISocketFactory.getDefaultSocketFactory)
     */
    public UnicastRef2(String host,
                       int port,
                       RMIClientSocketFactory csf,
                       ObjID objId) {
        this(new Endpoint(host, port, csf, null), objId);
    }

    /**
     * Constructs UnicastRef2 using specified Endpoint.
     *
     * @param ep Endpoint for remote calls
     * @param objId Object ID of remote object
     */
    public UnicastRef2(Endpoint ep, ObjID objId) {
        super(ep, objId);
    }

    /**
     * Constructs UnicastRef2 using specified Endpoint and objId.
     *
     * @param ep Endpoint for remote calls
     * @param objId Object ID of remote object
     * @param isLocal if true this UnicastRef2 belongs to local object
     */
    public UnicastRef2(Endpoint ep, ObjID objId, boolean isLocal) {
        super(ep, objId, isLocal);
    }

    /**
     * @see RemoteRef.getRefClass(ObjectOutput)
     */
    public String getRefClass(ObjectOutput out) {
        return "UnicastRef2"; //$NON-NLS-1$
    }

    /**
     * Writes this UnicastRef2 object to the specified output stream.
     *
     * @param out the stream to write the object to
     *
     * @throws IOException if any I/O error occurred or class is not serializable
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        ep.writeExternal(out, true);
        writeCommon(out);
    }

    /**
     * Reads data for creating RemoteRef object from the specified input stream.
     *
     * @param in the stream to read data from
     *
     * @throws IOException if any I/O error occurred
     * @throws ClassNotFoundException if class could not be loaded by current
     *         class loader
     */
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException {
        ep = Endpoint.readExternal(in, true);
        readCommon(in);
    }
}
