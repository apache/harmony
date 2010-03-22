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
 * @author Sergey A. Krivenko
 */
package java.beans.beancontext;

import java.util.Locale;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test class for java.beans.beancontext.BeanContextSupport.
 * <p>
 */

public class BeanContextSupportTest extends TestCase {

    /** STANDARD BEGINNING * */

    /**
     * No arguments constructor to enable serialization.
     * <p>
     */
    public BeanContextSupportTest() {
        super();
    }

    /**
     * Constructs this test case with the given name.
     * <p>
     * 
     * @param name -
     *            The name for this test case.
     *            <p>
     */
    public BeanContextSupportTest(String name) {
        super(name);
    }

    /** TEST CONSTRUCTORS * */

    /**
     * * Test constructor with BeanContext, Locale, boolean, boolean parameters.
     * <p>
     * 
     * @see BeanContextSupport#BeanContextSupport(BeanContext, Locale, boolean,
     *      boolean)
     */
    public void testConstructorBeanContextLocalebooleanboolean() {
        new BeanContextSupport(null, null, true, true);
    }

    /**
     * * Test constructor with BeanContext, Locale, boolean parameters.
     * <p>
     * 
     * @see BeanContextSupport#BeanContextSupport(BeanContext, Locale, boolean)
     */
    public void testConstructorBeanContextLocaleboolean() {
        new BeanContextSupport(null, null, true);
    }

    /**
     * * Test constructor with BeanContext, Locale parameters.
     * <p>
     * 
     * @see BeanContextSupport#BeanContextSupport(BeanContext, Locale)
     */
    public void testConstructorBeanContextLocale() {
        new BeanContextSupport(null, null);
    }

    /**
     * * Test constructor with BeanContext parameter.
     * <p>
     * 
     * @see BeanContextSupport#BeanContextSupport(BeanContext)
     */
    public void testConstructorBeanContext() {
        new BeanContextSupport(null);
    }

    /**
     * * Test constructor with no parameters.
     * <p>
     * 
     * @see BeanContextSupport#BeanContextSupport()
     */
    public void testConstructor() {
        new BeanContextSupport();
    }

    /** TEST METHODS * */

    /**
     * Test method createBCSChild() with Object, Object parameters.
     * <p>
     */
    public void testCreateBCSChildObjectObject() {
        BeanContextSupport sup = new BeanContextSupport();
        sup.createBCSChild(new BeanContextSupport(), new BeanContextSupport());
    }

    /**
     * Test method setLocale() with Locale parameter.
     * <p>
     */
    public void testSetLocaleLocale() throws Exception {
        BeanContextSupport sup = new BeanContextSupport();
        sup.setLocale(null);

        assertEquals("BeanContext should have default locale", java.util.Locale
                .getDefault(), sup.getLocale());
    }

    /**
     * Test method bcsChildren() with no parameters.
     * <p>
     */
    @SuppressWarnings("unchecked")
    public void testBcsChildren() {
        BeanContextSupport sup = new BeanContextSupport();
        sup.add(new BeanContextChildSupport());

        for (java.util.Iterator it = sup.bcsChildren(); it.hasNext();) {
            Object next = it.next();

            assertTrue("Children must be instances of "
                    + "BeanContextSupport.BCSChild class "
                    + "but at least one of them: " + next.getClass(),
                    next instanceof BeanContextSupport.BCSChild);
        }
    }

    /**
     * Test method retainAll() with Collection parameter.
     * <p>
     */
    public void testRetainAllCollection() {
        /*
         * // Create an instance and add one child BeanContextSupport sup = new
         * BeanContextSupport(); BeanContextChildSupport ch = new
         * BeanContextChildSupport(); sup.add(ch); // Create collection with an
         * instance of the child that was added java.util.Collection col = new
         * java.util.ArrayList(); col.add(ch); // Remove all children that are
         * not in the collection // The collection must remain unchanged if
         * (sup.retainAll(col)) { fail("False should be returned"); } // Just
         * one child must be present if (sup.size() != 1) { fail("The size of
         * the collection must be 1"); } // Add a new child in the collection
         * and remove the old one col.clear(); col.add(new Object()); // Remove
         * all children that are not in the collection // The collection must
         * have 0 elements after that if (!sup.retainAll(col)) { fail("True
         * should be returned"); } // No children must be present if (sup.size() !=
         * 0) { fail("The size of the collection must be 0"); }
         */
    }

    /**
     * Test method removeAll() with Collection parameter.
     * <p>
     */
    public void testRemoveAllCollection() {
        /*
         * // Create an instance and add one child BeanContextSupport sup = new
         * BeanContextSupport(); BeanContextChildSupport ch = new
         * BeanContextChildSupport(); sup.add(ch); // Create collection with an
         * instance of an arbitrary child java.util.Collection col = new
         * java.util.ArrayList(); col.add(new Object()); // Remove all children
         * that are in the collection // The collection should not change after
         * that if (sup.removeAll(col)) { fail("False should be returned"); } //
         * Add a child that is a member of the BeanContext col.add(ch); //
         * Remove all children that are in the collection // The collection
         * should change after that if (!sup.removeAll(col)) { fail("True should
         * be returned"); } // No children must be present if (sup.size() != 0) {
         * fail("The size of the collection must be 0 but is " + sup.size()); }
         */
    }

    /**
     * Test method containsAll() with Collection parameter.
     * <p>
     */
    public void testContainsAllCollection() {
        /*
         * // Create an instance and add two children BeanContextSupport sup =
         * new BeanContextSupport(); BeanContextChildSupport ch = new
         * BeanContextChildSupport(); Object obj = new Object(); sup.add(ch);
         * sup.add(obj); // Create collection with BCS children that just were
         * added java.util.Collection col = new java.util.ArrayList();
         * 
         * for (java.util.Iterator it = sup.bcsChildren(); it.hasNext(); ) {
         * col.add(it.next()); } // Two collections have the same elements if
         * (!sup.containsAll(col)) { fail("True should be returned"); }
         * 
         * sup.remove(obj); // Now they are different if (sup.containsAll(col)) {
         * fail("False should be returned"); }
         */
    }

    /**
     * Test method addAll() with Collection parameter.
     * <p>
     */
    public void testAddAllCollection() {
        /*
         * // Create an instance and add two children BeanContextSupport sup =
         * new BeanContextSupport(); // Create collection with two elements
         * java.util.Collection col = new java.util.ArrayList(); col.add(new
         * BeanContextChildSupport()); col.add(new Object()); // Place two
         * children into the BeanContext if (!sup.addAll(col)) { fail("True
         * should be returned"); } // Two children must be present if
         * (sup.size() != 2) { fail("The size of the collection must be 2 but is " +
         * sup.size()); }
         */
    }

    /**
     * Test method remove() with Object, boolean parameters.
     * <p>
     */
    public void testRemoveObjectboolean() {
        // Create an instance and add one child
        BeanContextSupport sup = new BeanContextSupport();
        BeanContextChildSupport ch = new BeanContextChildSupport();
        sup.add(ch);

        // Remove non-existent child
        assertFalse(sup.remove(new Object(), true));

        // Remove it
        assertTrue(sup.remove(ch, true));

        // No children must be present
        assertEquals("The size of the collection must be 0", 0, sup.size());
    }

    /**
     * Test method remove() with Object parameter.
     * <p>
     */
    public void testRemoveObject() {
        // Create an instance and add one child
        BeanContextSupport sup = new BeanContextSupport();
        BeanContextChildSupport ch = new BeanContextChildSupport();
        sup.add(ch);

        // Remove non-existent child
        assertFalse(sup.remove(new Object()));

        // Remove it
        assertTrue(sup.remove(ch));

        // No children must be present
        assertEquals("The size of the collection must be 0", 0, sup.size());
    }

    /**
     * Test method containsKey() with Object parameter.
     * <p>
     */
    public void testContainsKeyObject() {

        // Create an instance and add a child
        BeanContextSupport sup = new BeanContextSupport();
        BeanContextChildSupport ch = new BeanContextChildSupport();
        sup.add(ch);

        // We should find the child now
        assertTrue(sup.containsKey(ch));
    }

    /**
     * Test method contains() with Object parameter.
     * <p>
     */
    public void testContainsObject() {
        // Create an instance and add a child
        BeanContextSupport sup = new BeanContextSupport();
        BeanContextChildSupport ch = new BeanContextChildSupport();
        sup.add(ch);

        BeanContextSupport.BCSChild bcs = (BeanContextSupport.BCSChild) sup
                .bcsChildren().next();

        // We should find the child now
        if (!sup.contains(bcs)) {
            // fail("True should be returned");
        }
    }

    /**
     * Test method add() with Object parameter.
     * <p>
     */
    public void testAddObject() {
        // Create an instance and add a child
        BeanContextSupport sup = new BeanContextSupport();
        sup.add(new Object());

        // Just one child must be present
        assertEquals("The size of the collection must be 1", 1, sup.size());
    }

    /**
     * Test method toArray() with no parameters.
     * <p>
     */
    public void testToArray() {
        // Create an instance and add two children
        BeanContextSupport sup = new BeanContextSupport();
        sup.add("obj1");
        sup.add("obj2");

        // Convert to array
        Object[] array = sup.toArray();

        // Check length
        assertEquals("The size of the collection must be 2", 2, array.length);
    }

    /**
     * Test method copyChildren() with no parameters.
     * <p>
     */
    public void testCopyChildren() {
        // Create an instance and add two children
        BeanContextSupport sup = new BeanContextSupport();
        sup.add("obj1");
        sup.add("obj2");

        // Convert to array
        Object[] array = sup.copyChildren();

        // Check length
        assertEquals("The size of the collection must be 2", 2, array.length);
    }

    /**
     * Test method removeBeanContextMembershipListener() with
     * BeanContextMembershipListener parameter.
     * <p>
     */
    public void testRemoveBeanContextMembershipListenerBeanContextMembershipListener() {
        // Create BeanContext and BeanContextMembershipListener instances
        BeanContextSupport sup = new BeanContextSupport();
        BeanContextMembershipListener l = getBeanContextMembershipListener();
        sup.addBeanContextMembershipListener(l);
        sup.removeBeanContextMembershipListener(l);

        assertFalse("Listener should not be present", sup.bcmListeners
                .contains(l));
    }

    /**
     * Test method addBeanContextMembershipListener() with
     * BeanContextMembershipListener parameter.
     * <p>
     */
    public void testAddBeanContextMembershipListenerBeanContextMembershipListener() {
        // Create BeanContext and BeanContextMembershipListener instances
        BeanContextSupport sup = new BeanContextSupport();
        BeanContextMembershipListener l = getBeanContextMembershipListener();
        sup.addBeanContextMembershipListener(l);

        assertTrue("Listener should be present", sup.bcmListeners.contains(l));
    }

    /**
     * Test method getBeanContextPeer() with no parameters.
     * <p>
     */
    public void testGetBeanContextPeer() {
        // Create BeanContext instance
        BeanContextSupport sup = new BeanContextSupport();

        // The peer and this context should be equal
        assertEquals("The peer and the BeanContext should be equal", sup, sup
                .getBeanContextPeer());
    }

    /**
     * Test method vetoableChange() with PropertyChangeEvent parameter.
     * <p>
     */
    // public void testVetoableChangePropertyChangeEvent() {
    // /** @todo: not implemented yet in the class * */
    // Create BeanContext instance
    // BeanContextSupport sup = new BeanContextSupport();
    // sup.vetoableChange(null);
    // }
    /**
     * Test method propertyChange() with PropertyChangeEvent parameter.
     * <p>
     */
    // public void testPropertyChangePropertyChangeEvent() {
    // /** @todo: not implemented yet in the class * */
    // // Create BeanContext instance
    // BeanContextSupport sup = new BeanContextSupport();
    // // sup.propertyChange(null);
    // }
    /**
     * Test method isEmpty() with no parameters.
     * <p>
     */
    public void testIsEmpty() {
        // Create BeanContext instance
        BeanContextSupport sup = new BeanContextSupport();

        assertTrue("The collection of children should be empty", sup.isEmpty());

        sup.add(new Object());

        assertFalse("The collection of children should not be empty", sup
                .isEmpty());
    }

    /**
     * Test method clear() with no parameters.
     * <p>
     */
    public void testClear() {
        /*
         * // Create BeanContext instance BeanContextSupport sup = new
         * BeanContextSupport(); // Add a child and then clear sup.add(new
         * Object()); sup.clear();
         * 
         * if (!sup.isEmpty()) { fail("The collection of children should be
         * empty"); }
         */
    }

    /**
     * Test method size() with no parameters.
     * <p>
     */
    public void testSize() {
        // Create BeanContext instance
        BeanContextSupport sup = new BeanContextSupport();

        assertEquals("The size of the collection should be equal to 0", 0, sup
                .size());

        sup.add(new Object());

        assertEquals("The collection of children should not be empty", 1, sup
                .size());
    }

    /**
     * Test method getResourceAsStream() with String, BeanContextChild=null
     * parameters.
     * <p>
     */
    public void test_getResourceAsStreamLlava_lang_StringLjava_beans_beancontext_BeanContextChild() {
        BeanContextSupport obj = new BeanContextSupport();
        try {
            obj.getResourceAsStream(new String(), null);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
        }
    }

    /**
     * Test method getResourceAsStream() with String=null, BeanContextChild=null
     * parameters.
     * <p>
     */
    public void test_getResourceAsStreamLlava_lang_StringLjava_beans_beancontext_BeanContextChild2() {
        BeanContextSupport obj = new BeanContextSupport();
        try {
            obj.getResourceAsStream(null, null);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
        }
    }

    /**
     * Test method vetoableChange() with PropertyChangeEvent=null parameter.
     * <p>
     * 
     * @throws Exception
     */
    public void test_vetoableChangeLjava_beans_PropertyChangeEvent()
            throws Exception {
        BeanContextSupport obj = new BeanContextSupport();
        try {
            obj.vetoableChange(null);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
        }
    }

    /**
     * Test method getResource() with String!=null, BeanContextChild=null
     * parameters.
     * <p>
     */
    public void test_getResourceLjava_lang_StringLjava_beans_beancontext_BeanContextChild() {
        BeanContextSupport obj = new BeanContextSupport();
        try {
            obj.getResource("", null);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
        }
    }

    /**
     * Test method getResource() with String=null, BeanContextChild=null
     * parameters.
     * <p>
     */
    public void test_getResourceLjava_lang_StringLjava_beans_beancontext_BeanContextChild2() {
        BeanContextSupport obj = new BeanContextSupport();
        try {
            obj.getResource(null, null);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
        }
    }

    /** UTILITY METHODS * */

    /**
     * Create BeanContextMembershipListener instance
     */
    private BeanContextMembershipListener getBeanContextMembershipListener() {
        return new BeanContextMembershipListener() {

            public void childrenAdded(BeanContextMembershipEvent bcme) {
            }

            public void childrenRemoved(BeanContextMembershipEvent bcme) {
            }
        };
    }

    /** STANDARD ENDING * */

    /**
     * Start testing from the command line.
     * <p>
     */
    public static Test suite() {
        return new TestSuite(BeanContextSupportTest.class);
    }

    /**
     * Start testing from the command line.
     * <p>
     * 
     * @param args -
     *            Command line parameters.
     *            <p>
     */
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
}
