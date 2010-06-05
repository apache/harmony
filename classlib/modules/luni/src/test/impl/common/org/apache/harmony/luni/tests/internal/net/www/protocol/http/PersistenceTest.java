/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.luni.tests.internal.net.www.protocol.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.harmony.luni.internal.net.www.protocol.http.HttpConnection;
import org.apache.harmony.luni.internal.net.www.protocol.http.HttpConnectionManager;

import tests.support.Support_HttpServer;
import tests.support.Support_HttpServerSocket;
import tests.support.Support_Jetty;
import tests.support.Support_PortManager;
import tests.support.Support_URLConnector;

/**
 * Tests for <code>HttpURLConnection</code> persistence.
 * These tests depends on internal implementation.
 */
public class PersistenceTest extends TestCase {

    private static final boolean DEBUG = false;

    private final static Object bound = new Object();

    private static int port;

    static {
        // run-once set up
        try {
            port = Support_Jetty.startDefaultHttpServer();
        } catch (Exception e) {
            fail("Exception during setup jetty : " + e.getMessage());
        }
    }

    static class MockServer extends Thread {
        ServerSocket serverSocket;
        boolean accepted = false;
        boolean started = false;

        public MockServer(String name) throws IOException {
            super(name);
            serverSocket = new ServerSocket(0);
            serverSocket.setSoTimeout(5000);
        }

        public int port() {
            return serverSocket.getLocalPort();
        }

        @Override
        public void run() {
            try {
                synchronized (bound) {
                    started = true;
                    bound.notify();
                }
                try {
                    serverSocket.accept().close();
                    accepted = true;
                } catch (SocketTimeoutException ignore) {
                }
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class MockHTTPServer extends MockServer {
        // HTTP response codes
        static final int OK_CODE = 200;
        static final int NOT_FOUND_CODE = 404;
        // how many times persistent connection will be used
        // by server
        int persUses;
        // result code to be sent to client
        int responseCode;
        // response content to be sent to client
        String response = "<html></html>";
        // client's POST message
        String clientPost = "Hello from client!";

        public MockHTTPServer(String name, int persUses) throws IOException {
            this(name, persUses, OK_CODE);
        }

        public MockHTTPServer(String name, int persUses,
                int responseCode) throws IOException {
            super(name);
            this.persUses = persUses;
            this.responseCode = responseCode;
        }

        public int port() {
            return serverSocket.getLocalPort();
        }

        @Override
        public void run() {
            try {
                synchronized (bound) {
                    started = true;
                    bound.notify();
                }
                InputStream is = null;
                Socket client = null;
                try {
                    client = serverSocket.accept();
                    accepted = true;
                    for (int i=0; i<persUses; i++) {
                        if (DEBUG) {
                            System.out.println("*** Using connection for "
                                    + (i+1) + " time ***");
                        }
                        byte[] buff = new byte[1024];
                        is = client.getInputStream();
                        int num = 0; // number of read bytes
                        int bytik; // read byte value
                        boolean wasEOL = false;
                        // read header (until empty string)
                        while (((bytik = is.read()) > 0)) {
                            if (bytik == '\r') {
                                bytik = is.read();
                            }
                            if (wasEOL && (bytik == '\n')) {
                                break;
                            }
                            wasEOL = (bytik == '\n');
                            buff[num++] = (byte) bytik;
                        }
                        //int num = is.read(buff);
                        String message = new String(buff, 0, num);
                        if (DEBUG) {
                            System.out.println("---- Server got request: ----\n"
                                + message + "-----------------------------");
                        }

                        // Act as Server (not Proxy) side
                        if (message.startsWith("POST")) {
                            // client connection sent some data
                            // if the data was not read with header
                            if (DEBUG) {
                                System.out.println(
                                        "---- Server read client's data: ----");
                            }
                            num = is.read(buff);
                            message = new String(buff, 0, num);
                            if (DEBUG) {
                                System.out.println("'" + message + "'");
                                System.out.println(
                                        "------------------------------------");
                            }
                            // check the received data
                            assertEquals(clientPost, message);
                        }

                        client.getOutputStream().write((
                            "HTTP/1.1 " + responseCode + " OK\n"
                            + "Content-type: text/html\n"
                            + "Content-length: " 
                            + response.length() + "\n\n"
                            + response).getBytes());

                        if (responseCode != OK_CODE) {
                            // wait while test case check closed connection
                            // and interrupt this thread
                            try {
                                while (!isInterrupted()) {
                                    Thread.sleep(1000);
                                }
                            } catch (Exception ignore) { }
                        }
                    }
                } catch (SocketTimeoutException ignore) {
                    ignore.printStackTrace();
                } finally {
                    if (is != null) {
                        is.close();
                    }
                    if (client != null) {
                        client.close();
                    }
                    serverSocket.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setUp() {
        if (DEBUG) {
            System.out.println("\n==============================");
            System.out.println("===== Execution: "+getName());
            System.out.println("==============================");
        }
    }

    /**
     * Test that an HTTP connection persists
     */
    public void testConnectionsPersist() throws IOException, InterruptedException {
        int initialFreeConnections = HttpConnectionManager.getDefault().numFreeConnections();
        MockServer httpServer =
                new MockServer("ServerSocket for HttpURLConnectionTest");
        httpServer.start();
        synchronized(bound) {
            if (!httpServer.started) {
                bound.wait(5000);
            }
        }
        HttpURLConnection c = (HttpURLConnection)
            new URL("http://127.0.0.1:" + httpServer.port()).openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        c.getOutputStream().close();
        assertEquals(initialFreeConnections + 1, HttpConnectionManager.getDefault().numFreeConnections());
        c = (HttpURLConnection)
            new URL("http://127.0.0.1:" + httpServer.port()).openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        OutputStream os = c.getOutputStream();
        assertEquals(initialFreeConnections, HttpConnectionManager.getDefault().numFreeConnections());
        os.close();
        assertEquals(initialFreeConnections + 1, HttpConnectionManager.getDefault().numFreeConnections());
        httpServer.join();
    }

    /**
     * Test that multiple HTTP connections persist
     */
    public void testMultipleConnectionsPersist() throws IOException, InterruptedException {
        int initialFreeConnections = HttpConnectionManager.getDefault().numFreeConnections();
        MockServer httpServer =
                new MockServer("ServerSocket for HttpURLConnectionTest");
       httpServer.start();
       synchronized(bound) {
           if (!httpServer.started) {
               bound.wait(5000);
           }
       }
       MockServer httpServer2 =
           new MockServer("ServerSocket for HttpURLConnectionTest");
       httpServer2.start();
       synchronized(bound) {
           if (!httpServer2.started) {
              bound.wait(5000);
           }
       }
       HttpURLConnection c = (HttpURLConnection)
           new URL("http://127.0.0.1:" + httpServer.port()).openConnection();
       c.setDoOutput(true);
       c.setRequestMethod("POST");
       OutputStream os = c.getOutputStream();
       HttpURLConnection c2 = (HttpURLConnection)
       new URL("http://127.0.0.1:" + httpServer2.port()).openConnection();
       c2.setDoOutput(true);
       c2.setRequestMethod("POST");
       OutputStream os2 = c2.getOutputStream();
       os.close();
       os2.close();
       assertEquals(initialFreeConnections + 2, HttpConnectionManager.getDefault().numFreeConnections());

       c = (HttpURLConnection)
           new URL("http://127.0.0.1:" + httpServer.port()).openConnection();
       c.setDoOutput(true);
       c.setRequestMethod("POST");
       os = c.getOutputStream();
       assertEquals(initialFreeConnections + 1, HttpConnectionManager.getDefault().numFreeConnections());
       c2 = (HttpURLConnection)
       new URL("http://127.0.0.1:" + httpServer2.port()).openConnection();
       c2.setDoOutput(true);
       c2.setRequestMethod("POST");
       os2 = c2.getOutputStream();
       assertEquals(initialFreeConnections, HttpConnectionManager.getDefault().numFreeConnections());
       os.close();
       os2.close();
       assertEquals(initialFreeConnections + 2, HttpConnectionManager.getDefault().numFreeConnections());
       httpServer.join();
       httpServer2.join();
    }

    /**
     * Test that a closed HTTP connection is not kept in the pool of live connections
     * @throws URISyntaxException
     */
    public void testForcedClosure() throws Exception {
        int initialFreeConnections = HttpConnectionManager.getDefault().numFreeConnections();
        MockServer httpServer =
                new MockServer("ServerSocket for HttpURLConnectionTest");
        httpServer.start();
        synchronized(bound) {
            if (!httpServer.started) {
                bound.wait(5000);
            }
        }
        HttpConnection connection = HttpConnectionManager.getDefault().getConnection(new URI("http://127.0.0.1:" + httpServer.port()), 1000);
        HttpConnectionManager.getDefault().returnConnectionToPool(connection);
        assertEquals(initialFreeConnections + 1, HttpConnectionManager.getDefault().numFreeConnections());
        HttpURLConnection c = (HttpURLConnection)
            new URL("http://127.0.0.1:" + httpServer.port()).openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        c.getOutputStream();
        assertEquals(initialFreeConnections, HttpConnectionManager.getDefault().numFreeConnections());
        c.disconnect();
        assertEquals(initialFreeConnections, HttpConnectionManager.getDefault().numFreeConnections());
    }

    /**
     * Test that a connection is closed if the client does not read all the data
     * @throws Exception
     */
    public void testIncorrectUsage() throws Exception {
        int initialFreeConnections = HttpConnectionManager.getDefault().numFreeConnections();
        HttpURLConnection c = (HttpURLConnection)
            new URL("http://localhost:" + port).openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("GET");
        InputStream is = c.getInputStream(); // get the input stream but don't finish reading it
        is.close();
        assertEquals(initialFreeConnections, HttpConnectionManager.getDefault().numFreeConnections());
    }

    /**
     * Test that a connection is closed in case of unsuccessful connection.
     * Here client gets NOT_FOUND response.
     */
    public void testConnectionNonPersistence() throws Exception {
        MockHTTPServer httpServer =
            new MockHTTPServer("HTTP Server for NOT FOUND checking", 1,
                    MockHTTPServer.NOT_FOUND_CODE);
        httpServer.start();
        synchronized(bound) {
            if (!httpServer.started) {
                bound.wait(5000);
            }
        }

        int initialFreeConnections
            = HttpConnectionManager.getDefault().numFreeConnections();

        HttpURLConnection c = (HttpURLConnection)
            new URL("http://localhost:"+httpServer.port()).openConnection();
        if (DEBUG) {
            System.out.println("Actual connection class: "+c.getClass());
        }

        c.setDoInput(true);
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        try {
            c.getInputStream();
            fail("Expected IOException was not thrown");
        } catch (IOException expected) {
            // expected
        } finally {
            httpServer.interrupt();
        }
        assertEquals("Unsuccessful connection was not closed",
                initialFreeConnections,
                HttpConnectionManager.getDefault().numFreeConnections());
    }

    /**
     * Test that a connection is not closed if the client does read all the data
     * @throws Exception 
     */
    public void testCorrectUsage() throws Exception {
        int initialFreeConnections = HttpConnectionManager.getDefault().numFreeConnections();
        HttpURLConnection c = (HttpURLConnection)
            new URL("http://localhost:" + port).openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("GET");
        InputStream is = c.getInputStream();
        byte[] buffer = new byte[128];
        int totalBytes = 0;
        int bytesRead = 0;
        while((bytesRead = is.read(buffer)) > 0){
            totalBytes += bytesRead;
        }
        is.close();
        assertEquals(initialFreeConnections + 1, HttpConnectionManager.getDefault().numFreeConnections());

        HttpURLConnection c2 = (HttpURLConnection)
            new URL("http://localhost:" + port).openConnection();
        c2.setDoOutput(true);
        c2.setRequestMethod("GET");
        InputStream is2 = c2.getInputStream();
        byte[] buffer2 = new byte[128];
        int totalBytes2 = 0;
        int bytesRead2 = 0;
        while((bytesRead2 = is2.read(buffer2)) > 0){
            totalBytes2 += bytesRead2;
        }
        is2.close();
        assertEquals(initialFreeConnections + 1, HttpConnectionManager.getDefault().numFreeConnections());
        assertEquals(totalBytes, totalBytes2);
    }

    /**
     * Test that the http.keepAlive system property has the required effect on persistent connections
     */
    public void testKeepAliveSystemProperty() throws IOException, InterruptedException {
        System.setProperty("http.keepAlive", "false");
        MockServer httpServer =
                new MockServer("ServerSocket for HttpURLConnectionTest");
        httpServer.start();
        synchronized(bound) {
            if (!httpServer.started) {
                bound.wait(5000);
            }
        }
        HttpURLConnection c = (HttpURLConnection)
            new URL("http://127.0.0.1:" + httpServer.port()).openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        OutputStream os = c.getOutputStream();
        os.close();
        assertEquals(0, HttpConnectionManager.getDefault().numFreeConnections());
        httpServer.join();
        System.setProperty("http.keepAlive", "true");
    }

    /**
     * Test that the http.maxConnections system property has the required effect on persistent connections
     * @throws Exception
     */
    public void testMaxConnectionsSystemProperty() throws Exception {
        int initialFreeConnections = HttpConnectionManager.getDefault().numFreeConnections();
        System.setProperty("http.maxConnections", "2");
        HttpURLConnection c = (HttpURLConnection)
            new URL("http://localhost:" + port).openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("GET");
        InputStream is = c.getInputStream();
        c = (HttpURLConnection)
            new URL("http://localhost:" + port).openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("GET");
        InputStream is2 = c.getInputStream();
        c = (HttpURLConnection)
            new URL("http://localhost:" + port).openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("GET");
        InputStream is3 = c.getInputStream();
        byte[] buffer = new byte[128];
        while(is.read(buffer) > 0){
        }
        while(is2.read(buffer) > 0){
        }
        while(is3.read(buffer) > 0){
        }
        is.close();
        is2.close();
        is3.close();
        assertEquals(initialFreeConnections + 2, HttpConnectionManager.getDefault().numFreeConnections());
    }
    
    public void testClosingOutputStream() throws IOException {
//      create a serversocket
        Support_HttpServerSocket serversocket = new Support_HttpServerSocket();
        int portNumber = Support_PortManager.getNextPort();
        // create a client connector
        Support_URLConnector connector = new Support_URLConnector();
        Support_HttpServer server = new Support_HttpServer(serversocket, this);
        
        server.startServer(portNumber);


        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream is;
        int c;
        final String postTestUrl = "http://localhost:" + portNumber
            + Support_HttpServer.POSTTEST;

        String toWrite = "abcdef";
        connector.open(postTestUrl);
        OutputStream out = connector.getOutputStream();
        System.out.println("Output stream = " + out.hashCode());
        out.write(toWrite.getBytes("ISO8859_1"));
        out.close();
        is = connector.getInputStream();
        bout.reset();
        do {
            c = is.read();
            if (c != -1) {
                bout.write(c);
            }
        } while (c != -1);
        is.close();
        connector.close();
        String result = new String(bout.toByteArray(), "ISO8859_1");
        assertTrue("Error sending data 1: " + result, toWrite
                .equals(result));   
        
        toWrite = "zyxwvuts";
        connector.open(postTestUrl);
        connector.setRequestProperty("Transfer-encoding", "chunked");
        out = connector.getOutputStream();
        System.out.println("Output stream = " + out.hashCode());
        out.write(toWrite.getBytes("ISO8859_1"));
        out.close();
        is = connector.getInputStream();
        bout.reset();
        do {
            c = is.read();
            if (c != -1) {
                bout.write(c);
            }
        } while (c != -1);
        is.close();
        connector.close();
        result = new String(bout.toByteArray(), "ISO8859_1");
        assertEquals(toWrite, result);
        
    }

}