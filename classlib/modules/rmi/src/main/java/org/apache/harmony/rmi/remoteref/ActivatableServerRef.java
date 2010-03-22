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
 * @author  Victor A. Martynov
 */
package org.apache.harmony.rmi.remoteref;

import java.io.ObjectOutput;
import java.rmi.activation.ActivationID;
import java.rmi.server.ObjID;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import org.apache.harmony.rmi.transport.Endpoint;


/**
 *
 * @author  Victor A. Martynov
 *
 * ActivatableServerRef
 */
public class ActivatableServerRef extends UnicastServerRef2 {

    private static final long serialVersionUID = 460723133775318075L;

    private ActivationID aid;

    /**
     * Constructor intended to create ActivatableServerRef on the given port.
     *
     * @param aid Activation identifier of this ActivatableRef
     * @param port Port on which ActivatableServerRef will be exported.
     * @param csf Client socket factory.
     * @param ssf Server socket factory.
     */
    public ActivatableServerRef(ActivationID aid,
                                int port,
                                RMIClientSocketFactory csf,
                                RMIServerSocketFactory ssf) {
        super(port, csf, ssf, new ObjID());
        this.aid = aid;
    }

    /**
     * Constructor intended to create ActivatableServerRef on the given port.
     * @param aid Activation identifier of this ActivatableRef
     * @param port Port on which ActivatableServerRef will be exported.
     */
    public ActivatableServerRef(ActivationID aid, int port) {
        this(aid, port, null, null);
    }

    /**
     *
     * @param out
     *
     * @return The String "ActivatableServerRef" that indicates which Serialization
     * rules should be applied to this remote reference.
     */
    public String getRefClass(ObjectOutput out) {
        return "ActivatableServerRef"; //$NON-NLS-1$
    }

    /**
     *
     * @return The instance of the ActivatableRef instance with the same Activation identifier as this ActivatableServerRef.
     */
    protected UnicastRef getClientRef(Endpoint ep, ObjID obj_id) {
        return new ActivatableRef(aid, new UnicastRef2(ep, obj_id, true));
    }
}
