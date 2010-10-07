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

import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.security.Permission;

import junit.framework.TestCase;

public class RemoteServerTest extends TestCase {

    public void testLog() {
        SecurityManager sm = new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                return;
            }
        };
        SecurityManager previous = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            RemoteServer.setLog(null);
            assertNull(RemoteServer.getLog());
        } finally {
            System.setSecurityManager(previous);
        }
    }

    public void testGetClientHost() {
        try {
            RemoteServer.getClientHost();
            fail();
        } catch (ServerNotActiveException e) {
        	// expected
        }
    }

}
