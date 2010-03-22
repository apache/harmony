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

import java.io.EOFException;

import java.rmi.RMISecurityManager;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;

import org.apache.harmony.rmi.common.SubProcess;
import org.apache.harmony.rmi.test.MyRemoteInterface1;
import org.apache.harmony.rmi.test.TestObject;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Unit test for RMI Distributed Garbage Collector.
 *
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public class DGCTest extends RMITestBase {

    /**
     * String to identify that a registry process must be started.
     */
    private static final String REGISTRY_ID = "registry";

    /**
     * String to identify that a server process for test 0 must be started.
     */
    private static final String SERVER_ID_0 = "server0";

    /**
     * String to identify that a client process for test 0 must be started.
     */
    private static final String CLIENT_ID_0 = "client0";

    /**
     * String to identify that a server process for test 3 must be started.
     */
    private static final String SERVER_ID_3 = "server3";

    /**
     * Garbage collector tick (in milliseconds).
     */
    private static final int GC_TICK = 10000;

    /**
     * No-arg constructor to enable serialization.
     */
    public DGCTest() {
        super();
    }

    /**
     * Constructs this test case with the given name.
     *
     * @param   name
     *          Name for this test case.
     */
    public DGCTest(String name) {
        super(name);
    }

    /**
     * Test0
     *
     * @throws  Exception
     *          If some error occurs.
     */
    public void test0() throws Exception {
        System.out.println("test0 starting");
        test0(CONFIG_DIRECT_SOCKET, true,
              CONFIG_DIRECT_SOCKET, true,
              CONFIG_DIRECT_SOCKET, true);
        System.out.println("test0 complete");
    }

    /**
     * Test3
     *
     * @throws  Exception
     *          If some error occurs.
     */
    public void test3() throws Exception {
        System.out.println("test3 starting");
        test3(CONFIG_DIRECT_SOCKET, true);
        System.out.println("test3 complete");
    }

    /**
     * Test0
     *
     * @param   configServer
     *          Server configuration to set environment for.
     *
     * @param   endorsedServer
     *          If endorsedDirs and bootClassPath should be propagated to
     *          server.
     *
     * @param   configClient
     *          Client configuration to set environment for.
     *
     * @param   endorsedClient
     *          If endorsedDirs and bootClassPath should be propagated to
     *          client.
     *
     * @param   configRegistry
     *          Registry configuration to set environment for.
     *
     * @param   endorsedRegistry
     *          If endorsedDirs and bootClassPath should be propagated to
     *          registry.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    private void test0(int configServer, boolean endorsedServer,
            int configClient, boolean endorsedClient, int configRegistry,
            boolean endorsedRegistry) throws Exception {
        SubProcess registry = null;
        SubProcess server = null;
        SubProcess client = null;

        try {
            System.out.println("test0: creating registry");
            registry = startProcess("org.apache.harmony.rmi.DGCTest",
                    REGISTRY_ID, configRegistry, endorsedRegistry);
            registry.pipeError();
            System.out.println("test0: Expecting READY from registry");
            registry.expect();
            registry.pipeInput();

            System.out.println("test0: starting server");
            server = startProcess("org.apache.harmony.rmi.DGCTest",
                    SERVER_ID_0, configServer, endorsedServer);
            server.pipeError();
            server.closeOutput();
            System.out.println("test0: Expecting READY from server");
            server.expect();

            System.out.println("test0: starting client");
            client = startProcess("org.apache.harmony.rmi.DGCTest",
                    CLIENT_ID_0, configClient, endorsedClient);
            client.pipeInput();
            client.pipeError();
            client.closeOutput();

            System.out.println("test0: Expecting STARTED from server");
            server.expect("TestObject.test1() started");
            server.pipeInput();

            System.out.println("test0: destroying registry");
            registry.destroy();

            System.out.println("test0: destroying client");
            client.destroy();

            System.out.println("test0: waiting for server to return");
            assertEquals("Test server return", 0, server.waitFor());
        } finally {
            if (registry != null) {
                registry.destroy();
            }
            if (client != null) {
                client.destroy();
            }
            if (server != null) {
                server.destroy();
            }
        }
    }

    /**
     * Test3
     *
     * @param   config
     *          Configuration to set environment for.
     *
     * @param   endorsed
     *          If endorsedDirs and bootClassPath should be propagated to
     *          test VM.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    private void test3(int config, boolean endorsed) throws Exception {
        SubProcess server = null;

        try {
            System.out.println("test3: starting server");
            server = startProcess("org.apache.harmony.rmi.DGCTest",
                    SERVER_ID_3, config, endorsed);
            server.pipeInput();
            server.pipeError();
            server.closeOutput();
            assertEquals("Test server return", 0, server.waitFor());
        } finally {
            if (server != null) {
                server.destroy();
            }
        }
    }

    /**
     * Runs registry process, wait for READY and exits with export
     * or stays on if input stream is closed.
     *
     * @param   config
     *          Number of the configuration to run.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    private void runRegistry(int config) throws Exception {
        System.err.println("Registry starting");
        System.setSecurityManager(new RMISecurityManager());
        setEnvironmentForConfig(config);
        Registry reg = LocateRegistry.createRegistry(REGISTRY_PORT);
        System.err.println("Registry initialized, telling READY to parent");
        SubProcess.tellOut();
        System.err.println("Expecting READY from parent");

        try {
            SubProcess.expectIn();
            UnicastRemoteObject.unexportObject(reg, true);
            System.err.println("Registry exiting");
        } catch (EOFException e) {
            System.err.println("EOFException caught, registry stays on");
        }
    }

    /**
     * Runs test server process.
     *
     * @param   config
     *          Number of the configuration to run.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    private void runTestServer0(int config) throws Exception {
        System.err.println("Test server started");
        System.setSecurityManager(new RMISecurityManager());
        setEnvironmentForConfig(config);
        MyRemoteInterface1 obj = new TestObject();
        UnicastRemoteObject.exportObject(obj, CUSTOM_PORT_4);
        LocateRegistry.getRegistry().rebind(
                TEST_STRING_1, RemoteObject.toStub(obj));
        GCThread.create();
        System.err.println("Test server initialized, telling READY to parent");
        SubProcess.tellOut();
    }

    /**
     * Runs test client process.
     *
     * @param   config
     *          Number of the configuration to run.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    private void runTestClient0(int config) throws Exception {
        System.err.println("Test client started");
        System.setSecurityManager(new RMISecurityManager());
        setEnvironmentForConfig(config);
        Registry reg = LocateRegistry.getRegistry();
        MyRemoteInterface1 mri = (MyRemoteInterface1) reg.lookup(TEST_STRING_1);
        mri.test1();
        System.err.println("Test client completed");
    }

    /**
     * Runs test server process.
     *
     * @param   config
     *          Number of the configuration to run.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    private void runTestServer3(int config) throws Exception {
        System.err.println("Test server started");
        System.setSecurityManager(new RMISecurityManager());
        setEnvironmentForConfig(config);
        Registry reg = LocateRegistry.createRegistry(REGISTRY_PORT);
        TestObject obj = new TestObject();
        UnicastRemoteObject.exportObject(obj, REGISTRY_PORT);
        obj = null;
        System.gc();
        System.err.println("Test server exiting");
    }

    /**
     * Calls system garbage collector ({@link System#gc()}) periodically.
     */
    static class GCThread extends Thread {

        /**
         * Creates this thread and marks it as daemon thread.
         */
        public GCThread() {
            super();
            setDaemon(true);
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            while (true) {
                try {
                    sleep(GC_TICK);
                } catch (InterruptedException e) {}

                System.out.println("GCThread: Calling GC");
                System.gc();
            }
        }

        /**
         * Creates new GCThread thread.
         */
        public static void create() {
            new GCThread().start();
        }
    }

    /**
     * Returns test suite for this class.
     *
     * @return  Test suite for this class.
     */
    public static Test suite() {
        return new TestSuite(DGCTest.class);
    }

    /**
     * Starts the testing from the command line.
     *
     * @param   args
     *          Command line parameters.
     */
    public static void main(String args[]) {
        switch (args.length) {
        case 0:
            // Run tests normally.
            junit.textui.TestRunner.run(suite());
            break;
        case 2:
            // Run registry, test server or client process.
            int config = new Integer(args[1]).intValue();
            String param = args[0].intern();
            DGCTest dgcTest = new DGCTest();

            try {
                if (param == REGISTRY_ID) {
                    dgcTest.runRegistry(config);
                } else if (param == SERVER_ID_0) {
                    dgcTest.runTestServer0(config);
                } else if (param == CLIENT_ID_0) {
                    dgcTest.runTestClient0(config);
                } else if (param == SERVER_ID_3) {
                    dgcTest.runTestServer3(config);
                } else {
                    System.err.println("Bad parameter: " + param);
                    abort();
                }
            } catch (Exception e) {
                System.err.println("Child process ("
                        + param + ", " + config + ") failed: " + e);
                e.printStackTrace();
                abort();
            }
            System.err.println("Child process ("
                    + param + ", " + config + ") is terminating OK");
            break;
        default:
            System.err.println("Bad number of parameters: "
                    + args.length + ", expected: 2.");
            abort();
        }
    }
}
