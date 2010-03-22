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
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
package org.apache.harmony.rmi.test;

import java.rmi.RemoteException;

import java.rmi.server.UnicastRemoteObject;


/**
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public class MyRemoteObject2 extends UnicastRemoteObject
        implements MyRemoteInterface2 {

    String param = null;

    public MyRemoteObject2(String str) throws RemoteException {
        param = str;
    }

    public String test2() throws RemoteException {
        System.out.println("MyRemoteObject2: test2 called");
        return param;
    }
}
