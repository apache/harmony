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

package org.apache.harmony.jndi.provider.ldap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.net.SocketFactory;

import junit.framework.TestCase;

public class LdapContextFactoryTest extends TestCase {
    private Hashtable<String, Object> env = new Hashtable<String, Object>();

    private LdapContextFactory factory = new LdapContextFactory();

    public void setUp() {
        env.clear();
    }

    public void test_getInitialContext() throws Exception {
        env.put(Context.PROVIDER_URL, "ldap://192.168.1.1:389");

        env.put("java.naming.ldap.factory.socket", MockSocketFactory.class
                .getName());
        factory.getInitialContext(env);
        assertEquals("192.168.1.1", MockSocketFactory.host);
        assertEquals(389, MockSocketFactory.port);
        env.clear();

        env.put(Context.PROVIDER_URL, "ldap:///");

        env.put("java.naming.ldap.factory.socket", MockSocketFactory.class
                .getName());
        factory.getInitialContext(env);
        // default address
        assertEquals("localhost", MockSocketFactory.host);
        // default port
        assertEquals(389, MockSocketFactory.port);
        env.clear();

        env.put(Context.PROVIDER_URL, "ldap:///o=org?objectClass?one");
        env.put("java.naming.ldap.factory.socket", MockSocketFactory.class
                .getName());
        try {
            // only host, port and dn is allowed
            factory.getInitialContext(env);
            fail("Should throws ConfigurationException");
        } catch (ConfigurationException e) {
            // expected
        }
        env.clear();

    }

    public static class MockSocketFactory extends SocketFactory {

        static String host;

        static int port;

        private Socket socket = new MockSocket();

        @Override
        public Socket createSocket(String host, int port) throws IOException,
                UnknownHostException {
            MockSocketFactory.host = host;
            MockSocketFactory.port = port;
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress host, int port)
                throws IOException {
            this.host = host.getHostAddress();
            this.port = port;
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port,
                InetAddress localHost, int localPort) throws IOException,
                UnknownHostException {
            this.host = host;
            this.port = port;
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress address, int port,
                InetAddress localAddress, int localPort) throws IOException {
            this.host = address.getHostAddress();
            this.port = port;
            return socket;
        }

    }

    public static class MockSocket extends Socket {

        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }
    }
}
