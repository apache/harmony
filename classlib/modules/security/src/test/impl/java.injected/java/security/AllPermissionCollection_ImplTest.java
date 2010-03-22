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

package java.security;

import java.util.Enumeration;
import java.util.NoSuchElementException;

import junit.framework.TestCase;


/**
 * Tests for <code>AllPermissionCollection</code>
 * 
 */

public class AllPermissionCollection_ImplTest extends TestCase {

    /**
     * Empty collection implies nothing, non-empty collection implies
     * everything.
     */
    public void testImplies() {
        PermissionCollection pc = new AllPermissionCollection();
        Permission ap = new AllPermission();
        Permission sp = new SecurityPermission("abc");
        assertFalse(pc.implies(ap));
        assertFalse(pc.implies(sp));
        pc.add(ap);
        assertTrue(pc.implies(ap));
        assertTrue(pc.implies(sp));
    }

    /**
     * Can add any number of AllPermission instances, but no any other
     * permissions. Cannot add if collection is read-only.
     */
    public void testAdd() {
        PermissionCollection pc = new AllPermissionCollection();
        Permission ap = new AllPermission();
        Permission sp = new SecurityPermission("abc");
        try {
            pc.add(sp);
            fail("Should not add non-AllPermission");
        } catch (IllegalArgumentException ok) {
        }
        pc.add(ap);
        pc.add(ap);
        pc.add(new AllPermission());

        pc.setReadOnly();
        try {
            pc.add(ap);
            fail("read-only flag is ignored");
        } catch (SecurityException ok) {
        }
    }

    /**
     * Should return non-null empty enumeration for empty collection. For
     * non-empty collection, should always return 1-element enumeration.
     */
    public void testElements() {
        PermissionCollection pc = new AllPermissionCollection();
        Permission ap = new AllPermission();
        Enumeration en = pc.elements();
        assertNotNull(en);
        assertFalse(en.hasMoreElements());

        pc.add(ap);
        en = pc.elements();
        assertTrue(en.hasMoreElements());
        assertTrue(ap.equals(en.nextElement()));
        assertFalse(en.hasMoreElements());

        ap = new AllPermission();
        pc.add(ap);
        en = pc.elements();
        assertTrue(en.hasMoreElements());
        assertTrue(ap.equals(en.nextElement()));
        assertFalse(en.hasMoreElements());
    }

    /**
     * If nothing to enumerate, should throw NoSuchElementException
     */
    public void testElements_NoElements() {
        PermissionCollection pc = new AllPermissionCollection();
        try {
            pc.elements().nextElement();
            fail("should throw NoSuchElementException");
        } catch (NoSuchElementException ok) {}
    }
}
