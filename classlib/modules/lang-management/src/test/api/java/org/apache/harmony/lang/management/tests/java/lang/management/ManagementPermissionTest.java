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

package org.apache.harmony.lang.management.tests.java.lang.management;

import java.lang.management.ManagementPermission;

import junit.framework.TestCase;

public class ManagementPermissionTest extends TestCase {

    public void testManagementPermissionString() {
        ManagementPermission control = new ManagementPermission("control");
        assertEquals("control", control.getName());

        ManagementPermission monitor = new ManagementPermission("monitor");
        assertEquals("monitor", monitor.getName());

        try {
            new ManagementPermission("invalid");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    public void testManagementPermissionStringString() {
        ManagementPermission control = new ManagementPermission("control", null);
        assertEquals("control", control.getName());

        control = new ManagementPermission("control", "");
        assertEquals("control", control.getName());

        ManagementPermission monitor = new ManagementPermission("monitor", null);
        assertEquals("monitor", monitor.getName());

        monitor = new ManagementPermission("monitor", "");
        assertEquals("monitor", monitor.getName());

        try {
            new ManagementPermission("invalid", null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new ManagementPermission("invalid", "");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new ManagementPermission("control", "actions");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new ManagementPermission("monitor", "actions");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    public void testEquals() {
        ManagementPermission mp1 = new ManagementPermission("monitor");
        ManagementPermission mp2 = new ManagementPermission("monitor");
        assertEquals(mp1, mp2);

        mp1 = new ManagementPermission("control", "");
        mp2 = new ManagementPermission("control", "");
        assertEquals(mp1, mp2);

        mp1 = new ManagementPermission("monitor", null);
        mp2 = new ManagementPermission("monitor", null);
        assertEquals(mp1, mp2);

        mp1 = new ManagementPermission("monitor");
        mp2 = new ManagementPermission("control");
        assertFalse(mp1.equals(mp2));
    }

    public void testImplies() {
        ManagementPermission mp1 = new ManagementPermission("monitor");
        ManagementPermission mp2 = new ManagementPermission("control");
        ManagementPermission mp3 = new ManagementPermission("monitor", "");
        assertTrue(mp1.implies(mp1));
        assertTrue(mp1.implies(mp3));
        assertFalse(mp1.implies(mp2));
        assertFalse(mp2.implies(mp1));
    }

    public void testGetActions() {
        ManagementPermission mp1 = new ManagementPermission("monitor");
        assertEquals("", mp1.getActions());

        mp1 = new ManagementPermission("control", null);
        assertEquals("", mp1.getActions());
    }
}
