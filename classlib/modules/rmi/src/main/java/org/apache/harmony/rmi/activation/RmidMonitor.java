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
package org.apache.harmony.rmi.activation;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.activation.ActivationGroupID;
import java.rmi.activation.ActivationID;
import java.util.Hashtable;


/**
 * Monitor interface for RMID.
 *
 * @author  Victor A. Martynov
 */
public interface RmidMonitor {

    /*
     * Callbacks to inform monitor about changes in RMID.
     */

    void setStartMonitor(boolean startMonitor);

    void addGroup(ActivationGroupID gID);
    void activeGroup(ActivationGroupID gID);
    void inactiveGroup(ActivationGroupID gID);
    void removeGroup(ActivationGroupID gID);

    void addObject(ActivationID oID, ActivationGroupID gID);
    void activeObject(ActivationID oID);
    void inactiveObject(ActivationID oID);
    void removeObject(ActivationID oID);

    /**
     * @param port the port on which the registry should be probed.
     * @return Hashtable containing tree structure of the objects
     * registered on the given port, their fields and methods.
     * @throws RemoteException
     * @throws NotBoundException
     */
    Hashtable probeRegistry(int port) throws RemoteException, NotBoundException;
}
