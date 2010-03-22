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

import java.rmi.server.UnicastRemoteObject;


/**
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public class MyRemoteObject extends UnicastRemoteObject
        implements MyRemoteInterface {

    public MyRemoteObject() throws RemoteException {
        super();
    }

    public void test_String_Void(String param) throws RemoteException {
        System.out.println(
                "MyRemoteObject: test_String_Void: param = " + param);
    }

    public String test_Void_String() throws RemoteException {
        System.out.println("MyRemoteObject: test_Void_String");
        return "MyRemoteObject.test_Void_String";
    }

    public void test_Int_Void(int param) throws RemoteException {
        System.out.println("MyRemoteObject: test_Int_Void: param = " + param);
    }

    public int test_Void_Int() throws RemoteException {
        System.out.println("MyRemoteObject: test_Void_Int");
        return 987654321;
    }

    public void test_Remote_Void(Remote param) throws RemoteException {
        System.out.println(
                "MyRemoteObject: test_Remote_Void: param = " + param);
        System.out.println("Call param.test method: "
                + ((MyRemoteInterface1) param).test1());
    }

    public void test_UnicastRemoteObject_Void(Remote param)
            throws RemoteException {
        System.out.println(
                "MyRemoteObject: test_UnicastRemoteObject_Void: param = "
                + param);
    }

    public Remote test_Void_Remote() throws RemoteException {
        System.out.println("MyRemoteObject: test_Void_Remote");
        return new MyRemoteObject2("MyRemoteObject.test_Void_Remote");
    }

    public long test_Long_Long(long param) throws RemoteException {
        System.out.println("MyRemoteObject: test_Long_Long: param = " + param);
        return 998877665544332211L;
    }

    public String test_String_String(String param) throws RemoteException {
        System.out.println(
                "MyRemoteObject: test_String_String: param = " + param);
        return "MyRemoteObject.test_String_String";
    }

    public Remote test_Remote_Remote(Remote param) throws RemoteException {
        System.out.println(
                "MyRemoteObject: test_Remote_Remote: param = " + param);
        System.out.println("Call param.test method: "
                + ((MyRemoteInterface1) param).test1());
        return new MyRemoteObject2("MyRemoteObject.test_Remote_Remote");
    }

    public void test_RemoteString_Void(Remote param1, String param2)
            throws RemoteException {
        System.out.println("MyRemoteObject: test_RemoteString_Void: param1 = "
                + param1 + ", param2 = " + param2);
        System.out.println("Call param1.test method: "
                + ((MyRemoteInterface1) param1).test1());
    }

    public Remote test_RemoteRemote_Remote(Remote param1, Remote param2)
            throws RemoteException {
        System.out.println("MyRemoteObject: test_RemoteRemote_Remote: param1 = "
                + param1 + ", param2 = " + param2);
        System.out.println("Call param1.test method: "
                + ((MyRemoteInterface1) param1).test1());
        System.out.println("Call param2.test method: "
                + ((MyRemoteInterface3) param2).test3());
        return new MyRemoteObject2("MyRemoteObject.test_RemoteRemote_Remote");
    }

    public void test_BooleanStringRemote_Void(boolean param1, String param2,
            Remote param3) throws RemoteException {
        System.out.println("MyRemoteObject: test_BooleanStringRemote_Void: "
                + "param1 = " + param1 + ", param2 = " + param2 + ", param3 = "
                + param3);
        System.out.println("Call param3.test method: "
                + ((MyRemoteInterface1) param3).test1());
    }

    public void test_Proxy_Void(Object param) throws RemoteException {
        System.out.println("MyRemoteObject: test_Proxy_Void: param = " + param);
    }

    public void test_IntArray_Void(int[] param) throws RemoteException {
        System.out.println("MyRemoteObject: test_IntArray_Void: param = ");
        printIntArray(param);
    }

    public int[] test_Void_IntArray() throws RemoteException {
        System.out.println("MyRemoteObject: test_Void_IntArray");
        return new int[] { 1, 2 };
    }

    public void test_Array_Void(String[] param) throws RemoteException {
        System.out.println("MyRemoteObject: test_Array_Void: param = ");
        printArray(param);
    }

    public String[] test_Void_Array() throws RemoteException {
        System.out.println("MyRemoteObject: test_Void_Array");
        return new String[] { "MyRemoteObject.test_Void_Array 1",
                "MyRemoteObject.test_Void_Array 2" };
    }

    public void test_RemoteArray_Void(Remote[] param) throws RemoteException {
        System.out.println("MyRemoteObject: test_RemoteArray_Void: param = ");
        printArray(param);
    }

    public Remote[] test_Void_RemoteArray() throws RemoteException {
        System.out.println("MyRemoteObject: test_Void_RemoteArray");
        return new Remote[] {
                new MyRemoteObject1("MyRemoteObject.test_Void_RemoteArray 1"),
                new MyRemoteObject1("MyRemoteObject.test_Void_RemoteArray 2") };
    }

    public void test_Exception() throws RemoteException, MyException {
        System.out.println("MyRemoteObject: test_Exception");
        throw new MyException("MyRemoteObject.test_Exception");
    }

    public void test_Error() throws RemoteException, MyException {
        System.out.println("MyRemoteObject: test_Error");
        throw new Error("MyRemoteObject.test_Error");
    }

    public void test_RuntimeException() throws RemoteException, MyException {
        System.out.println("MyRemoteObject: test_RuntimeException");
        throw new RuntimeException("MyRemoteObject.text_RuntimeException");
    }

    public void test_RemoteException() throws RemoteException {
        System.out.println("MyRemoteObject: test_RemoteException");
        throw new RemoteException("MyRemoteObject.text_RemoteException");
    }

    public static void printArray(Object[] objs) {
        if (objs == null) {
            System.out.println("    Array is NULL.");
        }

        for (int i = 0; i < objs.length; ++i) {
            System.out.println("    " + objs[i]);
        }
    }

    public static void printIntArray(int[] arr) {
        if (arr == null) {
            System.out.println("    Int Array is NULL.");
        }

        for (int i = 0; i < arr.length; ++i) {
            System.out.println("    " + arr[i]);
        }
    }
}
