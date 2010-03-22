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

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public interface MyRemoteInterface extends Remote {

    public void test_String_Void(String param) throws RemoteException;
    public String test_Void_String() throws RemoteException;
    public void test_Int_Void(int param) throws RemoteException;
    public int test_Void_Int() throws RemoteException;
    public void test_Remote_Void(Remote param) throws RemoteException;
    public void test_UnicastRemoteObject_Void(Remote param) throws RemoteException;
    public Remote test_Void_Remote() throws RemoteException;
    public long test_Long_Long(long param) throws RemoteException;
    public String test_String_String(String param) throws RemoteException;
    public Remote test_Remote_Remote(Remote param) throws RemoteException;
    public void test_RemoteString_Void(Remote param1, String param2)
            throws RemoteException;
    public Remote test_RemoteRemote_Remote(Remote param1, Remote param2)
            throws RemoteException;
    public void test_BooleanStringRemote_Void(boolean param1, String param2,
            Remote param3) throws RemoteException;
    public void test_Proxy_Void(Object param) throws RemoteException;
    public void test_IntArray_Void(int[] param) throws RemoteException;
    public int[] test_Void_IntArray() throws RemoteException;
    public void test_Array_Void(String[] param) throws RemoteException;
    public String[] test_Void_Array() throws RemoteException;
    public void test_RemoteArray_Void(Remote[] param) throws RemoteException;
    public Remote[] test_Void_RemoteArray() throws RemoteException;
    public void test_Exception() throws RemoteException, MyException;
    public void test_Error() throws RemoteException, MyException;
    public void test_RuntimeException() throws RemoteException, MyException;
    public void test_RemoteException() throws RemoteException;
}
