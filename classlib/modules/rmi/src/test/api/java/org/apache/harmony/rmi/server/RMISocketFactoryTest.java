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
package org.apache.harmony.rmi.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.security.Permission;

import junit.framework.TestCase;

public class RMISocketFactoryTest extends TestCase {

    public void testGetSetFailureHandler() {
        RMIFailureHandler fh = new RMIFailureHandler() {
            public boolean failure(Exception ex) {
                return false;
            }
        };

        RMISocketFactory.setFailureHandler(fh);
        assertSame(fh, RMISocketFactory.getFailureHandler());
    }

    public void testSetSocketFactory() throws IOException {
        SecurityManager previous = System.getSecurityManager();
        SecurityManager sm = new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                /*
                 * Override checkPermission to allow everything. Specifically,
                 * we want to allow the SecurityManager to be set to null at the
                 * end of the test and we want to allow the 'testClass.jar' file
                 * to be allowed to load.
                 */
                return;
            }

            @Override
            public void checkSetFactory() {
                throw new SecurityException();
            }

        };
        System.setSecurityManager(sm);

        RMISocketFactory sf = new RMISocketFactory() {

            @Override
            public ServerSocket createServerSocket(int port) throws IOException {
                return null;
            }

            @Override
            public Socket createSocket(String host, int port)
                    throws IOException {
                return null;
            }
        };

        try {
            RMISocketFactory.setSocketFactory(sf);
            fail();
        } catch (SecurityException e) {
            return;
        } finally {
            System.setSecurityManager(previous);
        }

    }

}
