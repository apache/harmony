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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.activation.ActivationGroupID;
import java.rmi.activation.ActivationID;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.Hashtable;


/**
 * The parent class for all RMID monitors.
 *
 * @author  Victor A. Martynov
 */
public class RmidMonitorAdapter implements RmidMonitor {

    public Hashtable probeRegistry(int port) throws RemoteException,
            NotBoundException {
        Hashtable table = new Hashtable();
        Registry r = LocateRegistry.getRegistry(port);
        String s[] = r.list();

        for (int i = 0; i < s.length; i++) {
            Remote robj = r.lookup(s[i]);
            Class cl = robj.getClass();
            Field f[] = cl.getDeclaredFields();
            Method m[] = cl.getDeclaredMethods();
            Object buf[] = new Object[2];

            buf[0] = f;
            buf[1] = m;
            table.put(robj, buf);
        }

        return table;
    }

    public void setStartMonitor(boolean startMonitor) {
    }

    public void addGroup(ActivationGroupID gID) {
    }

    public void activeGroup(ActivationGroupID gID) {
    }

    public void inactiveGroup(ActivationGroupID gID) {
    }

    public void removeGroup(ActivationGroupID gID) {
    }

    public void addObject(ActivationID oID, ActivationGroupID gID) {
    }

    public void activeObject(ActivationID oID) {
    }

    public void inactiveObject(ActivationID oID) {
    }

    public void removeObject(ActivationID oID) {
    }
}
