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

package org.apache.harmony.jndi.provider.rmi.registry;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.naming.NamingException;
import javax.naming.Reference;

/**
 * This interface provides access to a remote reference.
 */
public interface RemoteReference extends Remote {

    /**
     * Returns the remote reference.
     * 
     * @return Remote reference.
     * 
     * @throws NamingException
     *             If naming exception occurs.
     * 
     * @throws RemoteException
     *             If RMI remote exception occurs.
     */
    public Reference getReference() throws NamingException, RemoteException;

}
