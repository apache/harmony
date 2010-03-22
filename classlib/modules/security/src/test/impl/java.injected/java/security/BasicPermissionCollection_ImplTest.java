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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;

import junit.framework.TestCase;


/**
 * Tests for <code>BasicPermissionCollection</code>
 * 
 */

public class BasicPermissionCollection_ImplTest extends TestCase {

    /**
     * Can add only BasicPermissions of the same type as the first added. Cannot
     * add if collection is read-only.
     */
    public void testAdd() {
        PermissionCollection pc = new BasicPermissionCollection();
        Permission ap = new AllPermission();
        Permission sp1 = new SecurityPermission("a.b.c");
        Permission sp2 = new SecurityPermission("a.b.*");
        try {
            pc.add(ap);
            fail("Should not add non-BasicPermission");
        } catch (IllegalArgumentException ok) {
        }
        pc.add(sp1);
        pc.add(sp2);
        try {
            pc.add(new BasicPermission("123") {
            });
            fail("Should not add BasicPermission of different type");
        } catch (IllegalArgumentException ok) {
        }

        pc.setReadOnly();
        try {
            pc.add(sp1);
            fail("read-only flag is ignored");
        } catch (SecurityException ok) {
        }
    }

    /**
     * Empty collection implies nothing. Non-empty collection should imply all
     * contained permissions, and should consider contained wildcards (if any).
     */
    public void testImplies() {
        PermissionCollection pc = new BasicPermissionCollection();
        Permission ap = new AllPermission();
        Permission up = new UnresolvedPermission("safds", null, null, null);
        Permission sp1 = new SecurityPermission("a.b.c");
        Permission sp11 = new SecurityPermission("a.b.");
        Permission sp2 = new SecurityPermission("a.b.*");
        Permission sp3 = new SecurityPermission("a.*");
        Permission sp4 = new SecurityPermission("*");

        assertFalse(pc.implies(ap));
        assertFalse(pc.implies(up));
        assertFalse(pc.implies(sp1));

        pc.add(sp3);
        assertTrue(pc.implies(sp2));
        assertTrue(pc.implies(sp1));
        assertTrue(pc.implies(sp11));
        assertTrue(pc.implies(sp3));
        assertFalse(pc.implies(sp4));

        pc.add(sp4);
        assertTrue(pc.implies(sp4));
        assertFalse(pc.implies(ap));
        assertFalse(pc.implies(up));
        assertTrue(pc.implies(new SecurityPermission("skjdnkwje wefkwjef")));
    }

    /**
     * Should return non-null empty enumeration for empty collection. For
     * non-empty collection, should always return enumeration over unique
     * elements.
     */
    public void testElements() {
        PermissionCollection pc = new BasicPermissionCollection();
        Permission sp1 = new SecurityPermission("a.b.c");
        Permission sp2 = new SecurityPermission("a.b.*");
        Permission sp3 = new SecurityPermission("*");
        Enumeration en = pc.elements();
        assertNotNull(en);
        assertFalse(en.hasMoreElements());

        try {
            pc.add(null);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        pc.add(sp1);
        en = pc.elements();
        assertTrue(en.hasMoreElements());
        assertTrue(sp1.equals(en.nextElement()));
        assertFalse(en.hasMoreElements());

        pc.add(sp1);
        en = pc.elements();
        assertTrue(en.hasMoreElements());
        assertTrue(sp1.equals(en.nextElement()));
        assertFalse(en.hasMoreElements());

        pc.add(sp3);
        pc.add(sp2);
        en = pc.elements();
        Collection els = new ArrayList();
        while (en.hasMoreElements()) {
            els.add(en.nextElement());
        }
        assertEquals(3, els.size());
        assertTrue(els.containsAll(Arrays.asList(new Permission[] {
            sp1, sp2, sp3 })));
    }

    /**
     * test on deserialization of incorrect object
     */
    public void testNegDeserialization_01() throws Exception {

        SecurityPermission sp = new SecurityPermission("a.b.c");
        BasicPermissionCollection pc = new BasicPermissionCollection();
        pc.add(sp);
        setField(pc, "permClass", BasicPermission.class);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(pc);
        oos.flush();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
            baos.toByteArray()));
        try {
            in.readObject();
            fail("should throw InvalidObjectException");
        } catch (java.io.InvalidObjectException e) {
        } finally {
            oos.close();
            in.close();
        }
    }

    /**
     * test on deserialization of incorrect object
     */
    public void testNegDeserialization_02() throws Exception {

        SecurityPermission sp = new SecurityPermission("a.b.c");
        BasicPermissionCollection pc = new BasicPermissionCollection();
        pc.add(sp);
        setField(pc, "allEnabled", new Boolean(true));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(pc);
        oos.flush();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
            baos.toByteArray()));
        try {
            in.readObject();
            fail("should throw InvalidObjectException");
        } catch (java.io.InvalidObjectException e) {
        } finally {
            oos.close();
            in.close();
        }
    }

    /**
     * setup a private field
     */
    private void setField(Object obj, String name, Object newval)
        throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, newval);
    }
}
