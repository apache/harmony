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

package java.rmi.activation;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ActivationSystem extends Remote {
    int SYSTEM_PORT = 1098;

    ActivationMonitor activeGroup(ActivationGroupID gID, ActivationInstantiator aInst,
            long incarnation) throws UnknownGroupException, ActivationException,
            RemoteException;

    ActivationGroupDesc setActivationGroupDesc(ActivationGroupID gID, ActivationGroupDesc gDesc)
            throws ActivationException, UnknownGroupException, RemoteException;

    ActivationDesc setActivationDesc(ActivationID aID, ActivationDesc aDesc)
            throws ActivationException, UnknownObjectException, UnknownGroupException,
            RemoteException;

    ActivationID registerObject(ActivationDesc aDesc) throws ActivationException,
            UnknownGroupException, RemoteException;

    ActivationGroupID registerGroup(ActivationGroupDesc gDesc) throws ActivationException,
            RemoteException;

    ActivationGroupDesc getActivationGroupDesc(ActivationGroupID gID)
            throws ActivationException, UnknownGroupException, RemoteException;

    ActivationDesc getActivationDesc(ActivationID aID) throws ActivationException,
            UnknownObjectException, RemoteException;

    void unregisterObject(ActivationID aID) throws ActivationException, UnknownObjectException,
            RemoteException;

    void unregisterGroup(ActivationGroupID gID) throws ActivationException,
            UnknownGroupException, RemoteException;

    void shutdown() throws RemoteException;
}
