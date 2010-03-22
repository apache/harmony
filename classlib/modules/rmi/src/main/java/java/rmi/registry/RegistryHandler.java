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

package java.rmi.registry;

import java.rmi.RemoteException;
import java.rmi.UnknownHostException;


/**
 * @deprecated This interface was used by previous versions of RMI. It should
 *      not be accessed anymore. There is no replacement for this.
 */
@Deprecated
public interface RegistryHandler {

    /**
     * @deprecated not used for registry implementations retrieval since
     *      Java v1.2
     */
    @Deprecated
    public Registry registryImpl(int port) throws RemoteException;

    /**
     * @deprecated not used for registry stubs retrieval since Java v1.2
     */
    @Deprecated
    public Registry registryStub(String host, int port)
            throws RemoteException, UnknownHostException;
}
