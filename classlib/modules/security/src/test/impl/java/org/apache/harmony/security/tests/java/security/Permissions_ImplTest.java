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

/**
* @author Alexey V. Varlamov
*/

package org.apache.harmony.security.tests.java.security;
import java.security.AllPermission;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.SecurityPermission;
import java.security.UnresolvedPermission;

import junit.framework.TestCase;

/**
 * Tests for <code>Permissions</code>
 * 
 */

public class Permissions_ImplTest extends TestCase {

    /**
     * A permission is implied by this collection, if either of the following is
     * true:
     * <ul>
     * <li>This collection contains AllPermission;
     * <li>This collection has elements of the same type as the permission
     * being checked, and they imply it;
     * <li>This collection has UnresolvedPermissions which can be resolved to
     * the checked type, and after resolving they imply the checked one.
     * </ul>
     * The only exception is UnresolvedPermission itself, which is effectively
     * implied only by AllPermission
     */
    public void testImplies() {
        Permissions ps = new Permissions();
        Permission ap = new AllPermission();
        Permission bp1 = new BasicPermission("jhb23jhg5") {
        };
        Permission bp2 = new BasicPermission("&%#&^$HJVH") {

            public PermissionCollection newPermissionCollection() {
                return null;
            }
        };
        Permission sp1 = new SecurityPermission("a.b.c");
        Permission sp2 = new SecurityPermission("a.b.*");
        Permission sp3 = new SecurityPermission("a.*");
        Permission up = new UnresolvedPermission(
            "java.security.SecurityPermission", "*", null, null);

        Permission[] arr = new Permission[] {
            ap, bp1, bp2, sp1, sp2, up };
        for (int i = 0; i < arr.length; i++) {
            assertFalse(ps.implies(arr[i]));
        }

        ps.add(bp1);
        assertTrue(ps.implies(bp1));
        assertFalse(ps.implies(bp2));
        assertFalse(ps.implies(ap));
        assertFalse(ps.implies(sp1));

        ps.add(sp2);
        assertTrue(ps.implies(sp1));
        assertTrue(ps.implies(sp2));
        assertFalse(ps.implies(sp3));

        ps.add(up);
        assertFalse(ps.implies(up));
        assertTrue(ps.implies(sp1));
        assertTrue(ps.implies(sp2));
        assertTrue(ps.implies(sp3));

        ps.add(ap);
        for (int i = 0; i < arr.length; i++) {
            assertTrue(ps.implies(arr[i]));
        }
    }
 }
