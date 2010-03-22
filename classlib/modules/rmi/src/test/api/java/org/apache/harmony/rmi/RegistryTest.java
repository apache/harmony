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
package org.apache.harmony.rmi;

import java.net.InetAddress;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.rmi.server.UnicastRemoteObject;

import org.apache.harmony.rmi.test.MyRemoteObject1;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Unit test for RMI Registry.
 *
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public class RegistryTest extends RMITestBase {

    /**
     * No-arg constructor to enable serialization.
     */
    public RegistryTest() {
        super();
    }

    /**
     * Constructs this test case with the given name.
     *
     * @param   name
     *          Name for this test case.
     */
    public RegistryTest(String name) {
        super(name);
    }

    /**
     * Tests registry creation and destruction.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    public void testBasic() throws Exception {
        System.setSecurityManager(new RMISecurityManager());

        // Create registry.
        int port = CUSTOM_PORT_1;
        Registry reg = LocateRegistry.createRegistry(port);
        System.out.println("Registry on CUSTOM port ("
                           + port + ") created.");

        // Destroy registry.
        UnicastRemoteObject.unexportObject(reg, true);
        System.out.println("Test complete.");
    }

    /**
     * Tests registry operation in detail.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    public void testDetailed() throws Exception {
        try {
            System.setSecurityManager(new RMISecurityManager());

            String localHost = InetAddress.getLocalHost().getHostName();

            // Create registry.
            int port = REGISTRY_PORT;
            Registry reg = LocateRegistry.createRegistry(port);
            System.out.println("Registry on DEFAULT port ("
                               + port + ") created.");

            Remote obj1 = new MyRemoteObject1("RemoteObject1");
            exportedObjects.add(obj1);
            Remote obj2 = new MyRemoteObject1("RemoteObject2");
            exportedObjects.add(obj2);
            Remote obj3 = new MyRemoteObject1("RemoteObject3");
            exportedObjects.add(obj3);
            Remote obj4 = new MyRemoteObject1("RemoteObject4");
            exportedObjects.add(obj4);
            Remote obj5 = new MyRemoteObject1("RemoteObject5");
            exportedObjects.add(obj5);
            Remote obj6 = new MyRemoteObject1("RemoteObject6");
            exportedObjects.add(obj6);
            Remote obj7 = new MyRemoteObject1("RemoteObject7");
            exportedObjects.add(obj7);
            Remote obj8 = new MyRemoteObject1("RemoteObject8");
            exportedObjects.add(obj8);
            System.out.println("Test objects exported.");

            // Check bind.
            System.out.println("Testing valid Naming.bind names...");
            Naming.bind("rmi://" + localHost + ":1099/RemoteObject1", obj1);
            Naming.bind("rmi://127.0.0.1:1099/RemoteObject2", obj2);
            Naming.bind("//" + localHost + ":1099/RemoteObject3", obj3);
            Naming.bind("//localhost:1099/RemoteObject4", obj4);
            Naming.bind("//:1099/RemoteObject5", obj5);
            Naming.bind("//:/RemoteObject6", obj6);
            Naming.bind("//" + localHost + "/RemoteObject7", obj7);
            Naming.bind("RemoteObject8", obj8);
            System.out.println("Done:");
            printArray(Naming.list(""));
            System.out.println("Checking bind complete.");

            // Check rebind.
            System.out.println("Testing valid Naming.rebind names...");
            Naming.rebind("rmi://" + localHost + ":1099/RemoteObject1", obj1);
            Naming.rebind("rmi://127.0.0.1:1099/RemoteObject2", obj2);
            Naming.rebind("//" + localHost + ":1099/RemoteObject3", obj3);
            Naming.rebind("//localhost:1099/RemoteObject4", obj4);
            Naming.rebind("//:1099/RemoteObject5", obj5);
            Naming.rebind("//:/RemoteObject6", obj6);
            Naming.rebind("//" + localHost + "/RemoteObject7", obj7);
            Naming.rebind("RemoteObject8", obj8);
            System.out.println("Done:");
            printArray(Naming.list("//127.0.0.1:/"));
            System.out.println("Checking rebind complete.");

            // Check lookup.
            System.out.println("Testing valid Naming.lookup names...");
            System.out.println(Naming.lookup("rmi://" + localHost + ":1099/RemoteObject1"));
            System.out.println(Naming.lookup("rmi://127.0.0.1:/RemoteObject2"));
            System.out.println(Naming.lookup("//" + localHost + ":1099/RemoteObject3"));
            System.out.println(Naming.lookup("//localhost:1099/RemoteObject4"));
            System.out.println(Naming.lookup("//:1099/RemoteObject5"));
            System.out.println(Naming.lookup("//:/RemoteObject6"));
            System.out.println(Naming.lookup("//" + localHost + "/RemoteObject7"));
            System.out.println(Naming.lookup("RemoteObject8"));
            System.out.println("Done:");
            printArray(Naming.list("rmi://" + localHost + ""));
            System.out.println("Checking lookup complete.");

            // Check unbind.
            System.out.println("Testing valid Naming.unbind names...");
            Naming.unbind("rmi://" + localHost + ":1099/RemoteObject1");
            Naming.unbind("rmi://127.0.0.1:1099/RemoteObject2");
            Naming.unbind("//" + localHost + ":1099/RemoteObject3");
            Naming.unbind("//localhost:1099/RemoteObject4");
            Naming.unbind("//:1099/RemoteObject5");
            Naming.unbind("//:/RemoteObject6");
            Naming.unbind("//" + localHost + "/RemoteObject7");
            Naming.unbind("RemoteObject8");
            System.out.println("Done:");
            printArray(Naming.list("//localhost"));
            System.out.println("Checking unbind complete.");

            // Destroy registry.
            UnicastRemoteObject.unexportObject(reg, true);

            // Create registry.
            port = CUSTOM_PORT_2;
            reg = LocateRegistry.createRegistry(port);
            System.out.println("Registry on CUSTOM port ("
                               + port + ") created.");

            // Check bind.
            System.out.println("Testing valid Naming.bind names...");
            Naming.bind("rmi://" + localHost + ':' + port + "/RemoteObject1", obj1);
            Naming.bind("rmi://127.0.0.1:" + port + "/RemoteObject2", obj2);
            Naming.bind("//" + localHost + ':' + port + "/RemoteObject3", obj3);
            Naming.bind("//localhost:" + port + "/RemoteObject4", obj4);
            Naming.bind("//:" + port + "/RemoteObject5", obj5);
            System.out.println("Done:");
            printArray(Naming.list("//localhost:" + port));
            System.out.println("Checking bind complete.");

            // Check rebind.
            System.out.println("Testing valid Naming.rebind names...");
            Naming.rebind("rmi://" + localHost + ':' + port + "/RemoteObject1", obj1);
            Naming.rebind("rmi://127.0.0.1:" + port + "/RemoteObject2", obj2);
            Naming.rebind("//" + localHost + ':' + port + "/RemoteObject3", obj3);
            Naming.rebind("//localhost:" + port + "/RemoteObject4", obj4);
            Naming.rebind("//:" + port + "/RemoteObject5", obj5);
            System.out.println("Done:");
            printArray(Naming.list("//127.0.0.1:" + port + "/"));
            System.out.println("Checking rebind complete.");

            // Check lookup.
            System.out.println("Testing valid Naming.lookup names...");
            System.out.println(Naming.lookup("rmi://" + localHost + ':' + port + "/RemoteObject1"));
            System.out.println(Naming.lookup("rmi://127.0.0.1:" + port + "/RemoteObject2"));
            System.out.println(Naming.lookup("//" + localHost + ':' + port + "/RemoteObject3"));
            System.out.println(Naming.lookup("//localhost:" + port + "/RemoteObject4"));
            System.out.println(Naming.lookup("//:" + port + "/RemoteObject5"));
            System.out.println("Done:");
            printArray(Naming.list("rmi://:" + port));
            System.out.println("Checking lookup complete.");

            // Check unbind.
            System.out.println("Testing valid Naming.unbind names...");
            Naming.unbind("rmi://" + localHost + ':' + port + "/RemoteObject1");
            Naming.unbind("rmi://127.0.0.1:" + port + "/RemoteObject2");
            Naming.unbind("//" + localHost + ':' + port + "/RemoteObject3");
            Naming.unbind("//localhost:" + port + "/RemoteObject4");
            Naming.unbind("//:" + port + "/RemoteObject5");
            System.out.println("Done:");
            printArray(Naming.list("//localhost:" + port));
            System.out.println("Checking unbind complete.");

            // Destroy registry.
            UnicastRemoteObject.unexportObject(reg, true);
        } finally {
            System.out.println("Unexporting objects");
            unexportObjects();
        }
        System.out.println("Test complete.");
    }

    /**
     * Returns test suite for this class.
     *
     * @return  Test suite for this class.
     */
    public static Test suite() {
        return new TestSuite(RegistryTest.class);
    }

    /**
     * Starts the testing from the command line.
     *
     * @param   args
     *          Command line parameters.
     */
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
}
