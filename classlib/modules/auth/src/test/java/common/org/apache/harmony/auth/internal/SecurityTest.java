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
* @author Stepan M. Mishura
*/

package org.apache.harmony.auth.internal;

import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AllPermission;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.Vector;
import java.util.logging.LoggingPermission;

import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;
import javax.security.auth.SubjectTest.MyClass1;
import javax.security.auth.SubjectTest.MyClass2;

import junit.framework.TestCase;

/**
 * Custom security manager 
 * 
 * Note: this class is subject to change,
 * please notify the author before using it 
 * 
 */

public class SecurityTest extends TestCase {

    public static boolean testing = false;

    private Permissions permissionsDenied = new Permissions();

    private Permissions permissionsGranted = new Permissions();

    // Represents a mode(grant/deny) for installed Security Manager.
    private boolean mode = true;

    public SecurityTest() {
        denyMode(); //set default mode
    }

    public SecurityTest(String name) {
        super(name);
        denyMode(); //set default mode
    }

    @Override
    protected void runTest() throws Throwable {
        if (System.getSecurityManager() != null) {
            fail("There MUST be no security manager installed!");
        }

        SecurityManager sm = new SecurityManager() {
            @Override
            public void checkPermission(Permission permission) {
                //System.out.println("P: " + permission);
                if (mode) { //deny mode
                    if (!permissionsDenied.implies(permission)) {
                        return;
                    }
                } else { // grant mode

                    //System.out.println("P: " + permissionsGranted);
                    if (permissionsGranted.implies(permission)) {
                        return;
                    }
                }

                // throw exception
                ProtectionDomain domain = new ProtectionDomain(null,
                        new Permissions());

                ProtectionDomain[] context = new ProtectionDomain[] { domain };
                AccessControlContext accContext = new AccessControlContext(
                        context);

                accContext.checkPermission(permission);
            }
        };

        System.setSecurityManager(sm);

        try {
            super.runTest();
        } finally {
            System.setSecurityManager(null);
        }
    }

    /**
     * Verifies that exception has correct associated permission class
     *
     * @param exception - to be verified
     * @param permission - permission class for comparing
     */
    public final void assertEquals(AccessControlException exception,
            Class<? extends Permission> permission) {
        if (!permission.isInstance(exception.getPermission())) {
            fail("No expected " + permission.getName());
        }
    }

    /**
     * Grants specified permission
     * It is used only in grant mode
     */
    public void grantPermission(Permission permission) {

        assertFalse("Grant mode", mode);

        permissionsGranted.add(permission);

        //System.out.println("PG: " + permission);
    }

    /**
     * Denies specified permission
     * It is used only in deny mode
     */
    public void denyPermission(Permission permission) {

        assertTrue("Deny mode", mode);

        permissionsDenied.add(permission);
    }

    /**
     * Sets deny mode
     * all permissions are granted, test can only deny specific permission
     */
    public void denyMode() {
        mode = true;

        permissionsGranted.add(new AllPermission());
        permissionsDenied = new Permissions();
    }

    /**
     * Sets grant mode
     * all permissions are denied, test can only grant specific permission
     */
    public void grantMode() {
        mode = false;

        permissionsDenied.add(new AllPermission());
        permissionsGranted = new Permissions();

        // junit harness stuff
        permissionsGranted
                .add(new PropertyPermission("line.separator", "read"));
        permissionsGranted.add(new RuntimePermission("exitVM"));
        permissionsGranted.add(new LoggingPermission("control", null));

        //grant permission to install security manager :-)
        permissionsGranted.add(new RuntimePermission("setSecurityManager"));
    }

    /**
     * Tests iterator interface
     * 
     */
    @SuppressWarnings("unchecked")
    public static class IteratorTest extends SecurityTest {

        /**
         * Tested <code>set</code>. Must be initialized in derived class 
         */
        public Set set;

        /**
         * Set's <code>element</code>. Must be initialized in derived class
         */
        public Object element;

        public IteratorTest() {
            super("IteratorTestSuite");
        }

        public IteratorTest(String name) {
            super(name);
        }

        @Override
        protected void setUp() throws Exception {
            super.setUp();

            assertTrue("IteratorTest: suite MUST be initialized", set != null
                    && element != null);

            assertEquals("IteratorTest: set MUST be empty", 0, set.size());
        }

        /**
         * Checks return value of Iterator.hasNext() for empty set
         * 
         * Expected: must return false 
         */
        public void testHasNext_EmptySet() {
            assertFalse("Set is empty", set.iterator().hasNext());
        }

        /**
         * Checks return value of Iterator.hasNext() for not empty set
         * 
         * Expected: must return true
         */
        public void testHasNext() {

            set.add(element);

            assertTrue("Set is not empty", set.iterator().hasNext());
        }

        /**
         * Checks Iterator.next() for empty set
         * 
         * Expected: NoSuchElementException
         */
        public void testNext_EmptySet_NoSuchElementException() {

            try {
                set.iterator().next();
                fail("No expected NoSuchElementException");
            } catch (NoSuchElementException e) {
            }
        }

        /**
         * Checks Iterator.next() for not empty set
         * 
         * Expected: no exception, returned element is equals to added
         */
        public void testNext() {

            set.add(element);

            Iterator it = set.iterator();

            assertEquals("Element", it.next(), element);
            assertFalse("Next element", it.hasNext());
        }

        /**
         * Calls Iterator.next() twice for set with one element
         * 
         * Expected: NoSuchElementException
         */
        public void testNext_NoSuchElementException() {

            set.add(element);

            Iterator it = set.iterator();

            it.next();
            try {
                it.next();
                fail("No expected NoSuchElementException");
            } catch (NoSuchElementException e) {
            }
        }

        /**
         * Remove element from set
         * 
         * Expected: no exception, size must be 0
         */
        public void testRemove() {

            set.add(element);

            Iterator it = set.iterator();

            it.next();
            it.remove();

            assertFalse("Next element", it.hasNext());
            assertEquals("Set size", 0, set.size());
        }

        /**
         * Tries to remove element from empty set.
         * Iterator.next() was not invoked before
         * 
         * Expected: IllegalStateException
         */
        public void testRemove_EmptySet_IllegalStateException() {

            try {
                set.iterator().remove();
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        /**
         * Tries to remove element from not empty set.
         * Iterator.next() was not invoked before
         * 
         * Expected: IllegalStateException
         */
        public void testRemove_IllegalStateException_NoNext() {

            set.add(element);

            try {
                set.iterator().remove();
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        /**
         * Tries to remove element from not empty set twice.
         * 
         * Expected: IllegalStateException
         */
        public void testRemove_IllegalStateException_2Remove() {

            set.add(element);

            Iterator it = set.iterator();

            it.next();
            it.remove();
            try {
                it.remove();
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }
    }

    /**
     * Tests iterator interface for read only set
     * 
     */
    @SuppressWarnings("unchecked")
    public static class ReadOnlyIteratorTest extends IteratorTest {

        public void setReadOnly() {
            throw new UnsupportedOperationException(
                    "setReadOnly MUST be implemented in derived class");
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testHasNext_EmptySet()
         */
        @Override
        public void testHasNext_EmptySet() {
            setReadOnly();
            super.testHasNext_EmptySet();
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testHasNext()
         */
        @Override
        public void testHasNext() {

            set.add(element);
            setReadOnly();

            assertTrue("Set is not empty", set.iterator().hasNext());
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testNext_EmptySet_NoSuchElementException()
         */
        @Override
        public void testNext_EmptySet_NoSuchElementException() {
            setReadOnly();
            super.testNext_EmptySet_NoSuchElementException();
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testNext()
         */
        @Override
        public void testNext() {

            set.add(element);
            setReadOnly();

            Iterator it = set.iterator();

            assertEquals("Element", it.next(), element);
            assertFalse("Next element", it.hasNext());
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testNext_NoSuchElementException()
         */
        @Override
        public void testNext_NoSuchElementException() {

            set.add(element);
            setReadOnly();

            Iterator it = set.iterator();

            it.next();
            try {
                it.next();
                fail("No expected NoSuchElementException");
            } catch (NoSuchElementException e) {
            }
        }

        /**
         * Remove element from read only set
         * 
         * Expected: IllegalStateException
         */
        @Override
        public void testRemove() {

            set.add(element);
            setReadOnly();

            Iterator it = set.iterator();

            it.next();
            try {
                it.remove();
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testRemove_EmptySet_IllegalStateException()
         */
        @Override
        public void testRemove_EmptySet_IllegalStateException() {
            setReadOnly();
            super.testRemove_EmptySet_IllegalStateException();
        }

        /**
         * Tries to remove element from read only set.
         * Iterator.next() was not invoked before
         * 
         * Expected: IllegalStateException
         */
        @Override
        public void testRemove_IllegalStateException_NoNext() {

            set.add(element);
            setReadOnly();

            try {
                set.iterator().remove();
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        /**
         * Tries to remove element from read only set.
         * 
         * Expected: IllegalStateException
         */
        @Override
        public void testRemove_IllegalStateException_2Remove() {

            set.add(element);

            Iterator it = set.iterator();

            it.next();
            setReadOnly();
            try {
                it.remove();
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }
    }

    /**
     * Tests iterator interface for secure set
     * 
     */
    @SuppressWarnings("unchecked")
    public static class SecureIteratorTest extends IteratorTest {

        public void setSecure() {
            throw new UnsupportedOperationException(
                    "setSecure MUST be implemented in derived class");
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testHasNext_EmptySet()
         */
        @Override
        public void testHasNext_EmptySet() {
            setSecure();
            super.testHasNext_EmptySet();
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testHasNext()
         */
        @Override
        public void testHasNext() {
            set.add(element);
            setSecure();

            assertTrue("Set is not empty", set.iterator().hasNext());
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testNext_EmptySet_NoSuchElementException()
         */
        @Override
        public void testNext_EmptySet_NoSuchElementException() {
            setSecure();
            super.testNext_EmptySet_NoSuchElementException();
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testNext_NoSuchElementException()
         */
        @Override
        public void testNext_NoSuchElementException() {
            set.add(element);
            setSecure();

            Iterator it = set.iterator();

            it.next();
            try {
                it.next();
                fail("No expected NoSuchElementException");
            } catch (NoSuchElementException e) {
            }
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testNext()
         */
        @Override
        public void testNext() {
            set.add(element);
            setSecure();

            Iterator it = set.iterator();

            assertEquals("Element", it.next(), element);
            assertFalse("Next element", it.hasNext());
        }

        /**
         * Tries to remove element from secure set
         * 
         * Expected: AccessControlException
         */
        @Override
        public void testRemove() {
            set.add(element);
            setSecure();

            Iterator it = set.iterator();

            it.next();
            try {
                it.remove();
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }

        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testRemove_EmptySet_IllegalStateException()
         * 
         * Expected: AccessControlException instead IllegalStateException for empty set
         */
        @Override
        public void testRemove_EmptySet_IllegalStateException() {

            setSecure();
            try {
                super.testRemove_EmptySet_IllegalStateException();
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }

        /**
         * @see org.apache.harmony.auth.internal.SecurityTest.IteratorTest#testRemove_IllegalStateException_NoNext()
         * 
         * Expected: AccessControlException instead IllegalStateException
         */
        @Override
        public void testRemove_IllegalStateException_NoNext() {

            set.add(element);
            setSecure();

            try {
                set.iterator().remove();
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }

        /**
         * While iterating the set was secured
         * 
         * Expected: AccessControlException
         */
        @Override
        public void testRemove_IllegalStateException_2Remove() {
            set.add(element);

            Iterator it = set.iterator();

            it.next();
            setSecure();
            try {
                it.remove();
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static class SetTest extends SecurityTest {

        /**
         * Tested <code>set</code>. Must be initialized in derived class 
         */
        public Set set;

        /**
         * Set's <code>element</code>. Must be initialized in derived class
         */
        public Object element;

        /**
         * Is used as collection parameter
         */
        public HashSet<Object> hash = new HashSet<Object>();

        @Override
        protected void setUp() throws Exception {
            super.setUp();

            assertTrue("SetTest: suite MUST be initialized", set != null
                    && element != null);

            assertEquals("SetTest: set MUST be empty", 0, set.size());

            hash.add(element);
        }

        //
        // Testing: boolean Set.add(Object o)
        //
        public void testAdd_NewElement() {
            assertTrue("Adding new element", set.add(element));
            assertEquals("Size", 1, set.size());
        }

        public void testAdd_ExistingElement() {
            set.add(element);
            assertFalse("Adding existing element", set.add(element));
            assertEquals("Size", 1, set.size());
        }

        //
        // Testing: boolean Set.addAll(Collection c)
        //

        public void testAddAll_NullParameter() {
            try {
                set.addAll(null);
                fail("No expected NullPointerException");
            } catch (NullPointerException npe) {
            }
        }

        public void testAddAll_EmptySet() {
            assertFalse("Adding empty set", set.addAll(new HashSet()));
            assertEquals("Size", 0, set.size());
        }

        public void testAddAll_NewElement() {
            assertTrue("Adding new element", set.addAll(hash));
            assertEquals("Size", 1, set.size());
        }

        public void testAddAll_ExistingElement() {
            set.add(element);
            assertFalse("Adding existing element", set.addAll(hash));
            assertEquals("Size", 1, set.size());
        }

        //
        // Testing: void Set.clear()
        //
        public void testClear_EmptySet() {
            set.clear();
            assertEquals("Set MUST be empty", 0, set.size());
        }

        public void testClear_NotEmptySet() {
            set.add(element);
            set.clear();
            assertEquals("Set MUST be empty", 0, set.size());
        }

        //
        // Testing: boolean Set.contains(Object o)
        //
        public void testContains_NotExistingElement() {
            assertFalse("Set doesn't contain element", set.contains(element));
        }

        public void testContains_ExistingElement() {
            set.add(element);
            assertTrue("Set contains element", set.contains(element));
        }

        //
        // Testing: boolean Set.containsAll(Collection c)
        //
        public void testContainsAll_NullParameter() {
            try {
                set.containsAll(null);
                fail("No expected NullPointerException");
            } catch (NullPointerException npe) {
            }
        }

        public void testContainsAll_EmptySet() {
            assertTrue("Empty set", set.containsAll(new HashSet()));
        }

        public void testContainsAll_NotExistingElement() {
            assertFalse("Set doesn't contain element", set.containsAll(hash));
        }

        public void testContainsAll_ExistingElement() {
            set.add(element);
            assertTrue("Set contains element", set.containsAll(hash));
        }

        //
        // Testing: boolean Set.isEmpty()
        //

        public void testIsEmpty_EmptySet() {
            assertTrue("Set is empty", set.isEmpty());
        }

        public void testIsEmpty_NotEmptySet() {
            set.add(element);
            assertFalse("Set is not empty", set.isEmpty());
        }

        //
        // Testing: Iterator Set.iterator()
        // 
        // NOTE: doesn't test Iterator interface
        //

        public void testIterator_EmptySet() {
            assertNotNull("Iterator", set.iterator());
        }

        public void testIterator_NotEmptySet() {
            set.add(element);
            assertNotNull("Iterator", set.iterator());
        }

        //
        // Testing: boolean Set.remove(Object o)
        //

        public void testRemove_NotExistingElement() {
            assertFalse("Removing absent element", set.remove(element));
            assertEquals("Size", 0, set.size());
        }

        public void testRemove_ExistingElement() {
            set.add(element);
            assertTrue("Removing element", set.remove(element));
            assertEquals("Size", 0, set.size());
        }

        //
        // Testing: boolean Set.removeAll(Collection c)
        //

        public void testRemoveAll_NullParameter_EmptySet() {

            try {
                set.removeAll(null);
            } catch (NullPointerException npe) {
            }
        }

        public void testRemoveAll_NullParameter_NotEmptySet() {

            set.add(element);
            try {
                set.removeAll(null);
                fail("No expected NullPointerException");
            } catch (NullPointerException npe) {
            }
        }

        public void testRemoveAll_EmptySet() {
            assertFalse("Removing empty set", set.removeAll(new HashSet()));
            assertEquals("Size", 0, set.size());
        }

        public void testRemoveAll_NotEmptySet() {
            set.add(element);
            assertFalse("Removing empty set", set.removeAll(new HashSet()));
            assertEquals("Size", 1, set.size());
        }

        public void testRemoveAll_NotExistingElement() {
            assertFalse("Removing elements", set.removeAll(hash));
            assertEquals("Size MUST NOT change", 0, set.size());
        }

        public void testRemoveAll_ExistingElement() {
            set.add(element);
            assertTrue("Removing elements", set.removeAll(hash));
            assertEquals("Size", 0, set.size());
        }

        //
        // Testing: boolean Set.retainAll(Collection c)
        //
        public void testRetainAll_NullParameter_EmptySet() {

            try {
                set.retainAll(null);

                // BUG Expected:  no exceptions
                if (!testing) {
                    fail("No expected NullPointerException");
                }
            } catch (NullPointerException npe) {
            }
        }

        public void testRetainAll_NullParameter_NotEmptySet() {

            set.add(element);
            try {
                set.retainAll(null);
                fail("No expected NullPointerException");
            } catch (NullPointerException npe) {
            }
        }

        public void testRetainAll_EmptySet() {
            assertFalse("Removing all elements", set.retainAll(new HashSet()));
            assertEquals("Size", 0, set.size());
        }

        public void testRetainAll_NotEmptySet() {
            set.add(element);
            assertTrue("Removing all elements", set.retainAll(new HashSet()));
            assertEquals("Size", 0, set.size());
        }

        public void testRetainAll_NotExistingElement() {
            assertFalse("Removing all elements", set.retainAll(hash));
            assertEquals("Size", 0, set.size());
        }

        public void testRetainAll_ExistingElement() {
            set.add(element);
            assertFalse("Removing elements", set.retainAll(hash));
            assertEquals("Size", 1, set.size());
        }

        //
        // Testing: Object[] Set.toArray()
        //
        public void testToArray_EmptySet() {
            assertEquals("Set is empty", set.toArray().length, 0);
        }

        public void testToArray_NotEmptySet() {
            set.add(element);
            assertEquals("Set is not empty", set.toArray().length, 1);
            assertTrue("Set element", set.toArray()[0] == element);
        }

        public void testToArray_Immutability() {
            set.add(element);
            set.toArray()[0] = null;
            assertTrue("Element", set.toArray()[0] == element);
        }

        //TODO test Object[] Set.toArray(Object[] a)
    }

    @SuppressWarnings("unchecked")
    public static class UnsupportedNullTest extends SecurityTest {

        /**
         * Tested <code>set</code>. Must be initialized in derived class 
         */
        public Set set;

        /**
         * Set's <code>element</code>. Must be initialized in derived class
         */
        public Object element;

        /**
         * Is used as collection parameter
         */
        public HashSet hash = new HashSet();

        @Override
        protected void setUp() throws Exception {
            super.setUp();

            assertTrue("UnsupportedNullTest: suite MUST be initialized",
                    set != null && element != null);

            assertEquals("UnsupportedNullTest: set MUST be empty",
                    0, set.size());

            hash.add(null);
        }

        public void testAdd() {

            if (testing) {
                try {
                    set.add(null);
                    // priv/pub credential set: no NullPointerException
                } catch (NullPointerException e) {
                    assertEquals("Size", 0, set.size());
                } catch (SecurityException e) {
                    // principal set: SecurityException
                }
            } else {
                try {
                    set.add(null);
                    fail("No expected NullPointerException");
                } catch (NullPointerException e) {
                }
                assertEquals("Size", 0, set.size());
            }
        }

        public void testAddAll() {

            if (testing) {
                try {
                    set.addAll(hash);
                    // priv/pub credentials set: no NullPointerException
                } catch (NullPointerException e) {
                    assertEquals("Size", 0, set.size());
                } catch (SecurityException e) {
                    // principal set: SecurityException
                }
            } else {
                try {
                    set.addAll(hash);
                    fail("No expected NullPointerException");
                } catch (NullPointerException e) {
                }
                assertEquals("Size", 0, set.size());
            }
        }

        public void testRemove() {
            try {
                assertFalse("Removing absent NULL element", set.remove(null));
            } catch (NullPointerException e) {
            }
            assertEquals("Size", 0, set.size());
        }

        public void testRemoveAll() {
            try {
                assertFalse("Removing NULL element", set.removeAll(hash));
            } catch (NullPointerException e) {
            }
            assertEquals("Size", 0, set.size());
        }

        public void testRetainAll_EmptySet() {
            try {
                assertFalse("Retaining NULL element", set.retainAll(hash));
            } catch (NullPointerException npe) {
            }
        }

        public void testRetainAll_NotEmptySet() {
            set.add(element);
            try {
                assertTrue("Retaining NULL element", set.retainAll(hash));
            } catch (NullPointerException npe) {
            }
            assertEquals("Set is empty", 0, set.size());
        }
    }

    @SuppressWarnings("unchecked")
    public static class IneligibleElementTest extends SecurityTest {

        /**
         * Tested <code>set</code>. Must be initialized in derived class 
         */
        public Set set;

        /**
         * Set's <code>element</code>. Must be initialized in derived class
         */
        public Object element;

        /**
         * Is used as collection parameter
         */
        public HashSet hash = new HashSet();

        /**
         * Set's <code>ineligible element</code>. Must be initialized in derived class
         */
        public Object iElement;

        /**
         * Is used as collection parameter
         */
        public HashSet iHash = new HashSet();

        @Override
        protected void setUp() throws Exception {
            super.setUp();

            assertTrue("IneligibleElementTest: suite MUST be initialized",
                    set != null && element != null && iElement != null);

            assertEquals("IneligibleElementTest: set MUST be empty",
                    0, set.size());

            hash.add(null);

            iHash.add(iElement);
        }

        public void testAdd() {

            try {
                set.add(iElement);

                fail("No expected ClassCastException or IllegalArgumentException");
            } catch (ClassCastException e) {
            } catch (IllegalArgumentException e) {
            } catch (SecurityException e) {
                if (!testing) {
                    // all sets - SecurityException
                    throw e;
                }
            }
            assertEquals("Size", 0, set.size());
        }

        // test against Class sets
        public void testAdd_Object() {

            if (iElement.getClass() == Object.class) {
                return;
            }

            try {
                set.add(new Object());

                if (!testing) {
                    // all Class sets - no exception
                    fail("No expected ClassCastException or IllegalArgumentException");
                }
            } catch (ClassCastException e) {
            } catch (IllegalArgumentException e) {
            }
        }

        public void testAddAll() {

            try {
                set.addAll(iHash);

                fail("No expected ClassCastException or IllegalArgumentException");
            } catch (ClassCastException e) {
            } catch (IllegalArgumentException e) {
            } catch (SecurityException e) {
                if (!testing) {
                    // all sets - SecurityException
                    throw e;
                }
            }
            assertEquals("Size", 0, set.size());
        }

        public void testContains() {

            try {
                assertFalse("Set doesn't contain element", set
                        .contains(iElement));
            } catch (ClassCastException e) {
            }
        }

        public void testContainsAll() {

            try {
                assertFalse("Set doesn't contain element", set
                        .containsAll(iHash));
            } catch (ClassCastException e) {
            }
        }

        public void testRemove() {

            try {
                assertFalse("Removing absent element", set.remove(iElement));
            } catch (ClassCastException e) {
            }
        }

        public void testRemoveAll() {

            try {
                assertFalse("Removing absent element", set.removeAll(iHash));
            } catch (ClassCastException e) {
            } catch (IllegalArgumentException e) {
            }
        }

        public void testRetainAll_EmptySet() {

            try {
                assertFalse("Retaining ineligible element", set
                        .retainAll(iHash));
            } catch (ClassCastException e) {
            } catch (IllegalArgumentException e) {
            }
        }

        public void testRetainAll_NotEmptySet_IneligibleElement() {

            set.add(element);
            try {
                assertTrue("Retaining ineligible element", set.retainAll(iHash));
            } catch (ClassCastException e) {
            } catch (IllegalArgumentException e) {
            }
            assertEquals("Now set is empty", 0, set.size());
        }
    }

    @SuppressWarnings("unchecked")
    public static class ReadOnlySetTest extends SetTest {

        public void setReadOnly() {
            throw new UnsupportedOperationException(
                    "setReadOnly MUST be implemented in derived class");
        }

        @Override
        public void testAdd_ExistingElement() {

            set.add(element);
            setReadOnly();

            try {
                set.add(element);
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        @Override
        public void testAdd_NewElement() {

            setReadOnly();
            try {
                set.add(element);
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        @Override
        public void testAddAll_EmptySet() {
            setReadOnly();
            super.testAddAll_EmptySet();
        }

        @Override
        public void testAddAll_ExistingElement() {

            set.add(element);
            setReadOnly();
            try {
                set.addAll(hash);
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        @Override
        public void testAddAll_NewElement() {
            setReadOnly();
            try {
                set.addAll(hash);
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        @Override
        public void testAddAll_NullParameter() {
            setReadOnly();
            super.testAddAll_NullParameter();
        }

        @Override
        public void testClear_EmptySet() {
            setReadOnly();
            super.testClear_EmptySet();
        }

        @Override
        public void testClear_NotEmptySet() {

            set.add(element);
            setReadOnly();

            try {
                set.clear();
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        @Override
        public void testContains_ExistingElement() {
            set.add(element);
            setReadOnly();
            assertTrue("Set contains element", set.contains(element));
        }

        @Override
        public void testContains_NotExistingElement() {
            setReadOnly();
            super.testContains_NotExistingElement();
        }

        @Override
        public void testContainsAll_EmptySet() {
            setReadOnly();
            super.testContainsAll_EmptySet();
        }

        @Override
        public void testContainsAll_ExistingElement() {
            set.add(element);
            setReadOnly();
            assertTrue("Set contains element", set.containsAll(hash));
        }

        @Override
        public void testContainsAll_NotExistingElement() {
            setReadOnly();
            super.testContainsAll_NotExistingElement();
        }

        @Override
        public void testContainsAll_NullParameter() {
            setReadOnly();
            super.testContainsAll_NullParameter();
        }

        @Override
        public void testIsEmpty_EmptySet() {
            setReadOnly();
            super.testIsEmpty_EmptySet();
        }

        @Override
        public void testIsEmpty_NotEmptySet() {
            set.add(element);
            setReadOnly();
            assertFalse("Set is not empty", set.isEmpty());
        }

        @Override
        public void testIterator_EmptySet() {
            setReadOnly();
            super.testIterator_EmptySet();
        }

        @Override
        public void testIterator_NotEmptySet() {
            set.add(element);
            setReadOnly();
            assertNotNull("Iterator", set.iterator());
        }

        @Override
        public void testRemove_ExistingElement() {

            set.add(element);
            setReadOnly();

            try {
                set.remove(element);
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        @Override
        public void testRemove_NotExistingElement() {
            setReadOnly();
            super.testRemove_NotExistingElement();
        }

        @Override
        public void testRemoveAll_EmptySet() {
            setReadOnly();
            super.testRemoveAll_EmptySet();
        }

        @Override
        public void testRemoveAll_ExistingElement() {

            set.add(element);
            setReadOnly();

            try {
                set.removeAll(hash);
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        @Override
        public void testRemoveAll_NotEmptySet() {

            set.add(element);
            setReadOnly();
            set.removeAll(new HashSet());
        }

        @Override
        public void testRemoveAll_NotExistingElement() {
            setReadOnly();
            super.testRemoveAll_NotExistingElement();
        }

        @Override
        public void testRemoveAll_NullParameter_EmptySet() {
            setReadOnly();
            super.testRemoveAll_NullParameter_EmptySet();
        }

        @Override
        public void testRemoveAll_NullParameter_NotEmptySet() {

            set.add(element);
            setReadOnly();

            try {
                set.removeAll(null);
                fail("No expected NullPointerException");
            } catch (NullPointerException npe) {
            }
        }

        @Override
        public void testRetainAll_EmptySet() {
            setReadOnly();
            super.testRetainAll_EmptySet();
        }

        @Override
        public void testRetainAll_ExistingElement() {
            set.add(element);
            setReadOnly();
            set.retainAll(hash);
        }

        @Override
        public void testRetainAll_NotEmptySet() {
            set.add(element);
            setReadOnly();

            try {
                set.retainAll(new HashSet());
                fail("No expected IllegalStateException");
            } catch (IllegalStateException e) {
            }
        }

        @Override
        public void testRetainAll_NotExistingElement() {
            setReadOnly();
            super.testRetainAll_NotExistingElement();
        }

        @Override
        public void testRetainAll_NullParameter_EmptySet() {
            setReadOnly();
            super.testRetainAll_NullParameter_EmptySet();
        }

        @Override
        public void testRetainAll_NullParameter_NotEmptySet() {

            set.add(element);
            setReadOnly();

            try {
                set.retainAll(null);
                fail("No expected NullPointerException");
            } catch (NullPointerException npe) {
            }
        }

        @Override
        public void testToArray_EmptySet() {
            setReadOnly();
            super.testToArray_EmptySet();
        }

        @Override
        public void testToArray_Immutability() {
            set.add(element);
            setReadOnly();

            set.toArray()[0] = null;
            assertTrue("Element", set.toArray()[0] == element);
        }

        @Override
        public void testToArray_NotEmptySet() {
            set.add(element);
            setReadOnly();

            assertEquals("Set is not empty", set.toArray().length, 1);
            assertTrue("Set element", set.toArray()[0] == element);
        }
    }

    @SuppressWarnings("unchecked")
    public static class SecureSetTest extends SetTest {

        public void setSecure() {
            throw new UnsupportedOperationException(
                    "setSecure MUST be implemented in derived class");
        }

        //        public void testRetainAll_NotEmptySet() {
        //            try {
        //                super.testRetainAll_NotEmptySet();
        //                fail("No expected AccessControlException");
        //            } catch (AccessControlException e) {
        //                assertEquals(e, AuthPermission.class);
        //            }
        //        }

        @Override
        public void testAdd_ExistingElement() {

            set.add(element);
            setSecure();

            try {
                set.add(element);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }

        @Override
        public void testAdd_NewElement() {

            setSecure();
            try {
                set.add(element);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }

        @Override
        public void testAddAll_EmptySet() {
            setSecure();
            super.testAddAll_EmptySet();
        }

        @Override
        public void testAddAll_ExistingElement() {

            set.add(element);
            setSecure();

            try {
                set.addAll(hash);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }

        @Override
        public void testAddAll_NewElement() {

            setSecure();
            try {
                set.addAll(hash);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }

        @Override
        public void testAddAll_NullParameter() {
            setSecure();
            super.testAddAll_NullParameter();
        }

        @Override
        public void testClear_EmptySet() {
            setSecure();
            super.testClear_EmptySet();
        }

        @Override
        public void testClear_NotEmptySet() {

            set.add(element);
            setSecure();

            try {
                set.clear();
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }

        @Override
        public void testContains_ExistingElement() {
            set.add(element);
            setSecure();
            assertTrue("Set contains element", set.contains(element));
        }

        @Override
        public void testContains_NotExistingElement() {
            setSecure();
            super.testContains_NotExistingElement();
        }

        @Override
        public void testContainsAll_EmptySet() {
            setSecure();
            super.testContainsAll_EmptySet();
        }

        @Override
        public void testContainsAll_ExistingElement() {
            set.add(element);
            setSecure();
            assertTrue("Set contains element", set.containsAll(hash));
        }

        @Override
        public void testContainsAll_NotExistingElement() {
            setSecure();
            super.testContainsAll_NotExistingElement();
        }

        @Override
        public void testContainsAll_NullParameter() {
            setSecure();
            super.testContainsAll_NullParameter();
        }

        @Override
        public void testIsEmpty_EmptySet() {
            setSecure();
            super.testIsEmpty_EmptySet();
        }

        @Override
        public void testIsEmpty_NotEmptySet() {
            set.add(element);
            setSecure();
            assertFalse("Set is not empty", set.isEmpty());
        }

        @Override
        public void testIterator_EmptySet() {
            setSecure();
            super.testIterator_EmptySet();
        }

        @Override
        public void testIterator_NotEmptySet() {
            set.add(element);
            setSecure();
            assertNotNull("Iterator", set.iterator());
        }

        @Override
        public void testRemove_ExistingElement() {

            set.add(element);
            setSecure();

            try {
                set.remove(element);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }

        @Override
        public void testRemove_NotExistingElement() {
            setSecure();
            super.testRemove_NotExistingElement();
        }

        @Override
        public void testRemoveAll_EmptySet() {
            setSecure();
            super.testRemoveAll_EmptySet();
        }

        @Override
        public void testRemoveAll_ExistingElement() {

            set.add(element);
            setSecure();

            try {
                set.removeAll(hash);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }

        @Override
        public void testRemoveAll_NotEmptySet() {
            set.add(element);
            setSecure();

            assertFalse("Removing empty set", set.removeAll(new HashSet()));
            assertEquals("Size", 1, set.size());
        }

        @Override
        public void testRemoveAll_NotExistingElement() {
            setSecure();
            super.testRemoveAll_NotExistingElement();
        }

        @Override
        public void testRemoveAll_NullParameter_EmptySet() {
            setSecure();
            super.testRemoveAll_NullParameter_EmptySet();
        }

        @Override
        public void testRemoveAll_NullParameter_NotEmptySet() {

            set.add(element);
            setSecure();

            try {
                set.removeAll(null);
                fail("No expected NullPointerException");
            } catch (NullPointerException npe) {
            }
        }

        @Override
        public void testRetainAll_EmptySet() {
            setSecure();
            super.testRetainAll_EmptySet();
        }

        @Override
        public void testRetainAll_ExistingElement() {
            set.add(element);
            setSecure();

            assertFalse("Removing elements", set.retainAll(hash));
            assertEquals("Size", 1, set.size());
        }

        @Override
        public void testRetainAll_NotEmptySet() {
            set.add(element);
            setSecure();

            try {
                set.retainAll(new HashSet());
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, AuthPermission.class);
            }
        }

        @Override
        public void testRetainAll_NotExistingElement() {
            setSecure();
            super.testRetainAll_NotExistingElement();
        }

        @Override
        public void testRetainAll_NullParameter_EmptySet() {
            setSecure();
            super.testRetainAll_NullParameter_EmptySet();
        }

        @Override
        public void testRetainAll_NullParameter_NotEmptySet() {
            set.add(element);
            setSecure();

            try {
                set.retainAll(null);
                fail("No expected NullPointerException");
            } catch (NullPointerException npe) {
            }
        }

        @Override
        public void testToArray_EmptySet() {
            setSecure();
            super.testToArray_EmptySet();
        }

        @Override
        public void testToArray_Immutability() {
            set.add(element);
            setSecure();

            set.toArray()[0] = null;
            assertTrue("Element", set.toArray()[0] == element);
        }

        @Override
        public void testToArray_NotEmptySet() {
            set.add(element);
            setSecure();

            assertEquals("Set is not empty", set.toArray().length, 1);
            assertTrue("Set element", set.toArray()[0] == element);
        }
    }

    public static class ObjectTest extends SecurityTest {

        //
        // obj1, obj2, obj3 are equals object
        // it is implied that obj1 is object of tested class  
        //
        public Object obj1;

        public Object obj2;

        public Object obj3;

        // Set of objects that are not equal to obj1, obj2, obj3
        public Vector<Object> notEqual;

        // Checks that references to testing objects are different
        // because we are not going to test Object.equals(Object)
        @Override
        protected void setUp() throws Exception {
            super.setUp();

            assertTrue("ObjectTest: suite MUST be initialized", obj1 != null
                    && obj2 != null && obj3 != null && obj1 != obj2
                    && obj1 != obj3 && obj2 != obj3 && notEqual != null);
        }

        public void testEquals_Reflexivity() {
            assertTrue(obj1.equals(obj1));
        }

        public void testEquals_Symmetry() {
            assertTrue(obj1.equals(obj2));
            assertTrue(obj2.equals(obj1));
        }

        public void testEquals_Transitivity() {
            assertTrue(obj1.equals(obj2));
            assertTrue(obj2.equals(obj3));
            assertTrue(obj1.equals(obj3));
        }

        public void testEquals_Consistenty() {
            assertTrue(obj1.equals(obj3));
            assertTrue(obj1.equals(obj3));
        }

        public void testEquals_NullValue() {
            assertFalse(obj1.equals(null));
        }

        public void testEquals_NotEqual() {
            for (Object name : notEqual) {
                assertFalse(obj1.equals(name));
            }
        }

        public void testHashCode() {
            assertEquals(obj1.hashCode(), obj2.hashCode());
            assertEquals(obj1.hashCode(), obj3.hashCode());
        }
    }
    
    @SuppressWarnings("unchecked")
    public static class SubjectSetObjectTest extends ObjectTest {

        public Subject subject = new Subject();
        
        public MyClass1 element1 = new MyClass1();

        public MyClass1 element2 = new MyClass1();

        public SubjectSetObjectTest(){
            
            subject.getPrincipals().add(element1);
            subject.getPrincipals().add(element2);

            //reverse order
            subject.getPrivateCredentials().add(element2);
            subject.getPrivateCredentials().add(element1);

            subject.getPublicCredentials().add(element1);
            subject.getPublicCredentials().add(element2);

            
            // init obj3
            HashSet hash = new HashSet();

            hash.add(element1);
            hash.add(element2);
            
            obj3 = hash;

            // init obj3
            HashSet hash1 = new HashSet();
            hash1.add(element1);
            hash1.add(new MyClass1());
            Subject s1 = new Subject(false, hash1, hash1, hash1);

            HashSet hash2 = new HashSet();
            hash1.add(element2);
            hash1.add(new MyClass2());
            Subject s2 = new Subject(false, hash2, hash2, hash2);

            Subject s3 = new Subject();

            notEqual = new Vector();
            
            notEqual.add(s1.getPrincipals());
            notEqual.add(s1.getPrivateCredentials());
            notEqual.add(s1.getPublicCredentials());
            notEqual.add(s1.getPrincipals(MyClass1.class));
            notEqual.add(s1.getPrivateCredentials(MyClass1.class));
            notEqual.add(s1.getPublicCredentials(MyClass1.class));

            notEqual.add(s2.getPrincipals());
            notEqual.add(s2.getPrivateCredentials());
            notEqual.add(s2.getPublicCredentials());
            notEqual.add(s2.getPrincipals(MyClass1.class));
            notEqual.add(s2.getPrivateCredentials(MyClass1.class));
            notEqual.add(s2.getPublicCredentials(MyClass1.class));

            notEqual.add(s3.getPrincipals());
            notEqual.add(s3.getPrivateCredentials());
            notEqual.add(s3.getPublicCredentials());
            notEqual.add(s3.getPrincipals(MyClass1.class));
            notEqual.add(s3.getPrivateCredentials(MyClass1.class));
            notEqual.add(s3.getPublicCredentials(MyClass1.class));

            notEqual.add(new HashSet());
            notEqual.add(new Object());
        }
    }
}
