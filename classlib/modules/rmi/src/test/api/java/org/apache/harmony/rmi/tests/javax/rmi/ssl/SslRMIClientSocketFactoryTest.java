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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.rmi.tests.javax.rmi.ssl;

import java.io.IOException;
import java.net.ServerSocket;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import javax.net.ssl.SSLSocket;

import junit.framework.TestCase;

public class SslRMIClientSocketFactoryTest extends TestCase {

    public void testSslRMIClientSocketFactory() {

        SslRMIClientSocketFactory factory = new SslRMIClientSocketFactory();
        SslRMIClientSocketFactory factory1 = new SslRMIClientSocketFactory();
        assertTrue(factory.equals(factory1));
        assertTrue(factory1.equals(factory));
    }

    public void testCreateSocket() throws Exception {

        SslRMIClientSocketFactory factoryCln = new SslRMIClientSocketFactory();
        SslRMIServerSocketFactory factorySrv = new SslRMIServerSocketFactory();

        ServerSocket ssocket = factorySrv.createServerSocket(0);
        SSLSocket csocket = (SSLSocket) factoryCln.createSocket("localhost",
                ssocket.getLocalPort());
        csocket.close();
        ssocket.close();

        String old = System
                .getProperty("javax.rmi.ssl.client.enabledCipherSuites");
        try {
            System.setProperty("javax.rmi.ssl.client.enabledCipherSuites",
                    "Incorrect");
            ssocket = factorySrv.createServerSocket(0);
            try {
                factoryCln.createSocket("localhost", ssocket.getLocalPort());
                fail("No expected IOException");
            } catch (IOException e) {
            }
            ssocket.close();
        } finally {
            if (old == null) {
                System.clearProperty("javax.rmi.ssl.client.enabledCipherSuites");
            } else {
                System.setProperty("javax.rmi.ssl.client.enabledCipherSuites", old);
            }
        }
    }

}
