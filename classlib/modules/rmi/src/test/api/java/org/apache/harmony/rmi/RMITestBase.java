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
package org.apache.harmony.rmi;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;

import java.rmi.Remote;

import java.rmi.server.UnicastRemoteObject;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.harmony.rmi.common.SubProcess;

import junit.framework.TestCase;


/**
 * Base class for RMI unit tests.
 *
 * @author  Vasily Zakharov
 */
public abstract class RMITestBase extends TestCase {

    /**
     * Direct socket connection configuration.
     */
    protected static final int CONFIG_DIRECT_SOCKET = 0;

    /**
     * Direct HTTP connection configuration.
     */
    protected static final int CONFIG_DIRECT_HTTP = 1;

    /**
     * Proxy HTTP connection configuration.
     */
    protected static final int CONFIG_PROXY_HTTP = 2;

    /**
     * Proxy CGI connection configuration.
     */
    protected static final int CONFIG_PROXY_CGI = 3;

    /**
     * RMI Registry host.
     */
    protected static final String REGISTRY_HOST = "localhost";

    /**
     * Default RMI registry port.
     */
    protected static final int REGISTRY_PORT = 1099;

    /**
     * Custom port to start RMI registry on.
     */
    protected static final int CUSTOM_PORT_1 = 5555;

    /**
     * Custom port to start RMI registry on.
     */
    protected static final int CUSTOM_PORT_2 = 7777;

    /**
     * Custom port to start RMI registry on.
     */
    protected static final int CUSTOM_PORT_3 = 8888;

    /**
     * Custom port to start RMI registry on.
     */
    protected static final int CUSTOM_PORT_4 = 9999;

    /**
     * Test string.
     */
    protected static final String TEST_STRING_1 = "TEST_1_STRING";

    /**
     * Test string.
     */
    protected static final String TEST_STRING_2 = "TEST_2_STRING";

    /**
     * Test string.
     */
    protected static final String TEST_STRING_3 = "TEST_3_STRING";

    /**
     * Test string.
     */
    protected static final String TEST_STRING_4 = "TEST_4_STRING";

    /**
     * HTTP Proxy host.
     */
    protected static final String PROXY_HOST = "your.proxy.host";

    /**
     * HTTP Proxy port.
     */
    protected static final int PROXY_PORT = 3128;

    /**
     * HTTP Proxy access timeout (in milliseconds)
     */
    protected static final int PROXY_TIMEOUT = 3000;

    /**
     * Timeout tick (in milliseconds).
     */
    protected static final int TIMEOUT_TICK = 1000;

    /**
     * Default connection timeout.
     */
    protected static final long CONNECTION_TIMEOUT = 0;

    /**
     * Garbage collector timeout (in milliseconds).
     */
    protected static final int GC_TIMEOUT = 20000;

    /**
     * If verbose logging need to be turned on.
     */
    protected static final boolean VERBOSE = false;

    /**
     * List of exported objects.
     */
    protected HashSet exportedObjects = new HashSet();

    /**
     * No-arg constructor to enable serialization.
     */
    protected RMITestBase() {
        super();
    }

    /**
     * Constructs this test case with the given name.
     *
     * @param   name
     *          Name for this test case.
     */
    protected RMITestBase(String name) {
        super(name);
    }

    /**
     * Sets environment to system properties.
     *
     * @param disableHttp
     * @param eagerHttpFallback
     * @param disableDirectSocket
     * @param enableDirectHTTP
     * @param disablePlainHTTP
     * @param useProxy
     * @param connectionTimeout
     * @param logging
     */
    protected static void setEnvironment(boolean disableHttp,
            boolean eagerHttpFallback, boolean disableDirectSocket,
            boolean enableDirectHTTP, boolean disablePlainHTTP,
            boolean useProxy, long connectionTimeout, boolean logging) {
        System.setProperty("java.rmi.dgc.leaseValue",
                                        Integer.toString(GC_TIMEOUT));
        System.setProperty("java.rmi.server.disableHttp",
                                        Boolean.toString(disableHttp));
        System.setProperty("harmony.rmi.transport.proxy.eagerHttpFallback",
                                        Boolean.toString(eagerHttpFallback));
        System.setProperty("harmony.rmi.transport.disableDirectSocket",
                                        Boolean.toString(disableDirectSocket));
        System.setProperty("harmony.rmi.transport.proxy.enableDirectHTTP",
                                        Boolean.toString(enableDirectHTTP));
        System.setProperty("harmony.rmi.transport.proxy.disablePlainHTTP",
                                        Boolean.toString(disablePlainHTTP));
        System.setProperty("http.proxyHost",
                                        (useProxy ? PROXY_HOST : ""));
        System.setProperty("http.proxyPort",
                                (useProxy ? Integer.toString(PROXY_PORT) : ""));
        System.setProperty("harmony.rmi.transport.connectionTimeout",
                                ((connectionTimeout > 0)
                                    ? Long.toString(connectionTimeout) : ""));
        System.setProperty("harmony.rmi.dgc.logLevel", "VERBOSE");
        System.setProperty("harmony.rmi.transport.logLevel",
                                        (logging ? "VERBOSE" : ""));
        System.setProperty("harmony.rmi.transport.tcp.logLevel",
                                        (logging ? "VERBOSE" : ""));
        System.setProperty("harmony.rmi.transport.proxy.logLevel",
                                        (logging ? "VERBOSE" : ""));
    }

    /**
     * Sets environment for direct socket connections.
     *
     * @param   config
     *          Configuration to set environment for.
     */
    protected static void setEnvironmentForConfig(int config) {
        switch (config) {
        case CONFIG_DIRECT_SOCKET:
            setEnvironment(
                    false,  // disableHttp
                    false,  // eagerHttpFallback
                    false,  // disableDirectSocket
                    false,  // enableDirectHTTP
                    false,  // disablePlainHTTP
                    false,  // useProxy
                    CONNECTION_TIMEOUT,  // connectionTimeout
                    VERBOSE // Logging
                    );
            break;
        case CONFIG_DIRECT_HTTP:
            setEnvironment(
                    false,  // disableHttp
                    false,  // eagerHttpFallback
                    true,   // disableDirectSocket
                    true,   // enableDirectHTTP
                    false,  // disablePlainHTTP
                    false,  // useProxy
                    CONNECTION_TIMEOUT,  // connectionTimeout
                    VERBOSE // Logging
                    );
            break;
        case CONFIG_PROXY_HTTP:
            setEnvironment(
                    false,  // disableHttp
                    false,  // eagerHttpFallback
                    true,   // disableDirectSocket
                    false,  // enableDirectHTTP
                    false,  // disablePlainHTTP
                    true,   // useProxy
                    CONNECTION_TIMEOUT,  // connectionTimeout
                    VERBOSE // Logging
                    );
            break;
        case CONFIG_PROXY_CGI:
            setEnvironment(
                    false,  // disableHttp
                    false,  // eagerHttpFallback
                    true,   // disableDirectSocket
                    false,  // enableDirectHTTP
                    true,   // disablePlainHTTP
                    true,   // useProxy
                    CONNECTION_TIMEOUT,  // connectionTimeout
                    VERBOSE // Logging
                    );
            break;
        default:
            assert false : ("Bad config number: " + config);
        }
    }

    /**
     * Starts process in a separate JVM.
     *
     * @param   className
     *          Name of the class to run in process created.
     *
     * @param   id
     *          Identifier of function to run (class specific).
     *
     * @param   config
     *          Number of the configuration to run.
     *
     * @param   endorsed
     *          If endorsedDirs and bootClassPath should be propagated.
     *
     * @return  Subprocess created.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    protected static SubProcess startProcess(String className,
            String id, int config, boolean endorsed) throws Exception {
        return JavaInvoker.invokeSimilar(null, className,
                new String[] { id, Integer.toString(config) },
                endorsed, endorsed);
    }

    /**
     * Checks if the specified socket/port is available.
     *
     * @param   host
     *          Host to check.
     *
     * @param   port
     *          Port to check.
     *
     * @param   timeout
     *          Time (in milliseconds) to wait for response.
     *
     * @return  <code>true</code> if the specified host/port is available,
     *          <code>false</code> otherwise.
     */
    protected static boolean checkSocket(String host, int port, int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeout);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks if proxy is available.
     *
     * @return  <code>true</code> if proxy is available,
     *          <code>false</code> otherwise.
     */
    protected static boolean checkProxy() {
        return checkSocket(PROXY_HOST, PROXY_PORT, PROXY_TIMEOUT);
    }

    /**
     * Checks if proxy is available.
     *
     * @param   testName
     *          Test name (for diagnostics purposes).
     *
     * @return  <code>true</code> if proxy is available,
     *          <code>false</code> otherwise.
     */
    protected static boolean checkProxy(String testName) {
        boolean ret = checkProxy();

        if (!ret) {
            System.out.println("WARNING: " + testName + " is skipped "
                    + "because proxy (" + PROXY_HOST + ':' + PROXY_PORT
                    + ") is not accessible.");
        }
        return ret;
    }

    /**
     * Prints the specified array to standard output.
     *
     * @param   array
     *          Array to print.
     */
    protected static void printArray(Object[] array) {
        if (array == null) {
            System.out.println("    Array is NULL.");
        }

        for (int i = 0; i < array.length; ++i) {
            System.out.println("    " + array[i]);
        }
    }

    /**
     * Aborts current process with the specified code.
     * Really calls {@link System#exit(int) System.exit(exitCode)}.
     *
     * @param   exitCode
     */
    protected static void abort(int exitCode) {
        System.exit(exitCode);
    }

    /**
     * Aborts current process.
     * Really calls {@link System#exit(int) System.exit(-1)}.
     */
    protected static void abort() {
        abort(-1);
    }

    /**
     * Unexports exported objects.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    protected void unexportObjects() throws Exception {
        for (Iterator i = exportedObjects.iterator(); i.hasNext(); ) {
            Remote obj = (Remote) i.next();
            System.err.println("Unexporting " + obj + " ...");
            UnicastRemoteObject.unexportObject(obj, true);
            System.err.println("Done.\n");
        }
    }
}
