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

import javax.rmi.ssl.SslRMIServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

import junit.framework.TestCase;

public class SslRMIServerSocketFactoryTest extends TestCase {

    public void testSslRMIServerSocketFactory() {
        SslRMIServerSocketFactory factory = new SslRMIServerSocketFactory();
        SslRMIServerSocketFactory factory1 = new SslRMIServerSocketFactory(
                null, null, false);
        assertTrue(factory.equals(factory1));
        assertTrue(factory1.equals(factory));
        assertNull(factory.getEnabledCipherSuites());
        assertNull(factory.getEnabledProtocols());
        assertFalse(factory.getNeedClientAuth());

        factory1 = new SslRMIServerSocketFactory(null, null, true);
        assertTrue(factory1.getNeedClientAuth());
        assertFalse(factory.equals(factory1));
        assertFalse(factory1.equals(factory));

        factory1 = new SslRMIServerSocketFactory(null,
                new String[] { "TLSv1" }, false);
        assertFalse(factory.equals(factory1));
        assertFalse(factory1.equals(factory));
        
        SSLServerSocketFactory tmpfac = (SSLServerSocketFactory) SSLServerSocketFactory
                .getDefault();        
        factory1 = new SslRMIServerSocketFactory(tmpfac.getDefaultCipherSuites(),
                null, false);
        assertFalse(factory.equals(factory1));
        assertFalse(factory1.equals(factory));

        try {
            new SslRMIServerSocketFactory(new String[] { "Incorrect" }, null,
                    false);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            new SslRMIServerSocketFactory(null, new String[] { "Incorrect" },
                    false);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }
    
    public void testCreateServerSocket() throws Exception {
        SslRMIServerSocketFactory factory = new SslRMIServerSocketFactory();

        factory.createServerSocket(0).close();
        
        try {
            factory.createServerSocket(-1);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
