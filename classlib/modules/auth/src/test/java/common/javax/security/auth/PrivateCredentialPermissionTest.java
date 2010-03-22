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
* @author Maxim V. Makarov
*/

package javax.security.auth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Principal;
import java.util.HashSet;

import junit.framework.TestCase;


/**
 * Tests PrivateCredentialPermission class implementation. 
 */
public class PrivateCredentialPermissionTest extends TestCase {

    private PrivateCredentialPermission p_that;

    private PrivateCredentialPermission p_this;

    String s_that;

    String s_this;

    /**
     * Constructor for PrivateCredentialPermissionTest.
     * 
     * @param name
     */
    public PrivateCredentialPermissionTest(String name) {
        super(name);
    }

    /**
     * [C1 P1 "duke"] implies [C1 P1 "duke" P2 "nuke"]. 
     */
    public final void testImplies_01() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.b.Principal \"duke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertTrue(p_this.implies(p_that));
        assertTrue(p_this.implies(p_this));
    }

    /**
     * [C1 P1 "nuke"] implies [C1 P1 "duke" P2 "nuke"]. 
     */
    public final void testImplies_02() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.c.Principal \"nuke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertTrue(p_this.implies(p_that));

    }

    /**
     * [* P1 "duke"] implies [C1 P1 "duke"] 
     */
    public final void testImplies_03() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "* a.b.Principal \"duke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertTrue(p_this.implies(p_that));

    }

    /**
     * [C1P1 "duke"] implies [C1 P1 "*"] 
     */
    public final void testImplies_04() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.c.Principal \"*\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertTrue(p_this.implies(p_that));

    }

    /**
     *  [C1 P1 "duke" P2 "nuke"] implies [C1 * "*"] 
     */
    public final void testImplies_05() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential * \"*\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertTrue(p_this.implies(p_that));

    }

    /**
     *  [C1 P1 "duke" P2 "nuke"] implies [C1 P1 "duke" P2 "nuke" P3 "duke"] 
     */
    public final void testImplies_06() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\" a.d.Principal \"duke\"";
        s_this = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertTrue(p_this.implies(p_that));

    }

    /**
     *  [C2 P1 "duke" ] does not imply [C1 P1 "duke" P2 "nuke" ] 
     */
    public final void testImplies_07() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.c.Credential a.b.Principal \"duke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.implies(p_that));
    }

    /**
     *  [C1 P3 "duke" ] does not imply [C1 P1 "duke" P2 "nuke" ] 
     */
    public final void testImplies_08() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.d.Principal \"duke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.implies(p_that));
    }

    /**
     *  [C1 P1 "nuke" ] does not imply [C1 P1 "duke" P3 "nuke" ] 
     */
    public final void testImplies_09() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.b.Principal \"nuke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.implies(p_that));

    }

    /**
     *  [C1 P1 "nuke" P2 "buke"] does not imply [C1 P1 "duke" P2 "nuke" ] 
     */
    public final void testImplies_10() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"buke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        p_this.implies(p_that);
        assertFalse(p_this.implies(p_that));

    }

    /**
     *  [C1 P1 "nuke" P2 "nuke" P3 "buke"] does not imply [C1 P1 "duke" P2 "nuke" P3 "kuke"]
     */
    public final void testImplies_110() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\" a.d.Principal \"kuke\"";
        s_this = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\" a.d.Principal \"buke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.implies(p_that));
    }

    /**
     * [C1 P1 "duke" P2 "buke"] does not imply [C1 P1 "*" ] 
     */
    public final void testImplies_11() {
        s_that = "a.b.Credential a.b.Principal \"*\"";
        s_this = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"buke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.implies(p_that));

    }

    /**
     * [C1 P2 "*"] does not imply [C1 P1 "*" ] 
     */
    public final void testImplies_12() {
        s_that = "a.b.Credential a.b.Principal \"*\"";
        s_this = "a.b.Credential a.c.Principal \"*\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.implies(p_that));

    }

    /**
     * [* P3 "duke"] does not imply [C1 P1 "duke" P2 "nuke"]
     */
    public final void testImplies_13() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "* a.d.Principal \"duke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.implies(p_that));
    }

    /**
     * [* P2 "buke"] does not imply [C1 P1 "duke" P2 "nuke"] 
     */
    public final void testImplies_14() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "* a.c.Principal \"buke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.implies(p_that));
    }

    /**
     *  [C1 P1 "buke"] does not imply [C1 P1 "*"] 
     */
    public final void testImplies_15() {
        s_that = "a.b.Credential a.b.Principal \"*\"";
        s_this = "a.b.Credential a.b.Principal \"nuke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.implies(p_that));
    }

    /**
     * [C1 * "*"] does not imply [C1 P1 "duke" P2 "nuke"]
     */
    public final void testImplies_16() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential * \"*\" a.b.Principal \"nuke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.implies(p_that));
    }

    /**
     *  [C1 a.c* "nuke"] not implies [C1 P1 "duke" a.c.P2 "nuke"]    
     */
    public final void testImplies_17() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.c.* \"nuke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        //try {
        assertFalse(p_this.implies(p_that));
        //} catch (AssertionFailedError e){
        //    fail("It seems to me this test should be pass, so \"principalClass\"" +
        //    		" should be has a name like 'a.b.*'");
        //}
    }

    /**
     * A permission is not an instance of PrivateCredentialPermission 
     */
    public final void testImplies_18() {
        AuthPermission p_this = new AuthPermission("name", "read");
        s_that = "a.b.Credential a.c.* \"nuke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        assertFalse(p_that.implies(p_this));
        assertFalse(p_that.implies(null));
    }

    /**
     * Create a correct ctor
     *
     * @throws IllegalArgumentException
     */
    public final void testPCP_01() throws IllegalArgumentException {
        p_this = new PrivateCredentialPermission(
                "a.b.Credential a.b.Principal \"duke\" a.b.Principal \"nuke\"",
                "read");
    }

    /**
     * Create a incorrect ctor  without string of Principal class and Principal name,
     * expected IAE  
     */
    public final void testPCP_02() {
        try {
            p_this = new PrivateCredentialPermission("a.b.Credential", "read");
            fail("new PrivateCredentialPermission('a.b.Credential', 'read') should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Create a incorrect ctor  without string of Principal name,
     * expected IAE  
     */
    public final void testPCP_03() {
        try {
            p_this = new PrivateCredentialPermission(
                    "a.b.Credential a.b.Principal", "read");
            fail("new PrivateCredentialPermission('a.b.Credential a.b.Principal', 'read') should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Create a incorrect ctor  without string of Principal class,
     * expected IAE  
     */
    public final void testPCP_04() {
        try {
            p_this = new PrivateCredentialPermission(
                    "a.b.Credential \"duke\" a.b.Principal \"nuke\"", "read");
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Create a incorrect ctor. Principal Class can not be a wildcard (*) 
     * value if Principal Name is not a wildcard (*) value,
     * expected IAE  
     */
    public final void testPCP_05() {

        try {
            p_this = new PrivateCredentialPermission(
                    "a.b.Credential * \"nuke\"", "read");
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * "Action must be "read" only,
     * expected IAE 
     */
    public final void testPCP_06() throws IllegalArgumentException {

        try {
            p_this = new PrivateCredentialPermission(
                    "a.b.Credential a.b.Principal \"nuke\"", "write");
            fail("Action can be \"read\" only, IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Principal name must be enveloped by quotes,
     * expected IAE
     */
    public final void testPCP_07() throws IllegalArgumentException {

        try {
            p_this = new PrivateCredentialPermission(
                    "a.b.Credential a.b.Principal a.c.Principal", "read");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Principal name must be enveloped by quotes,
     * expected IAE
     */
    public final void testPCP_08() throws IllegalArgumentException {

        try {
            p_this = new PrivateCredentialPermission(
                    "a.b.Credential a.b.Principal \"duke\" a.c.Principal",
                    "read");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Principal name must be enveloped by quotes,
     * expected IAE
     */
    public final void testPCP_09() throws IllegalArgumentException {

        try {
            p_this = new PrivateCredentialPermission(
                    "a.b.Credential a.b.Principal duke\"", "read");
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Principal name must be enveloped by quotes,
     * expected IAE
     */
    public final void testPCP_10() throws IllegalArgumentException {

        try {
            p_this = new PrivateCredentialPermission(
                    "a.b.Credential a.b.Principal \"duke", "read");
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Create a ctor. Code to support a principal class as a.c.* 
     *
     * @throws IllegalArgumentException
     */
    public final void testPCP_11() throws IllegalArgumentException {
        p_this = new PrivateCredentialPermission(
                "a.b.Credential a.b.* \"duke\"", "read");
    }

    /**
     * Create a ctor. [* * "*"]
     *
     * @throws IllegalArgumentException
     */
    public final void testPCP_12() throws IllegalArgumentException {
        p_this = new PrivateCredentialPermission("* * \"*\"", "read");
    }

    /**
     * a target name should not be empty 
     * @throws IllegalArgumentException
     */
    public final void testPCP_13() throws IllegalArgumentException {
        try {
            p_this = new PrivateCredentialPermission(new String(), "read");
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * a target name should not be null 
     */
    public final void testPCP_14() {
        try {
            new PrivateCredentialPermission(null, "read");
            fail("should be throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * test empty principal/name pairs
     */
    public final void testPCP_15() {
        try {
            p_this = new PrivateCredentialPermission("java.lang.Object", "read");
            fail("new PrivateCredentialPermission('java.lang.Object', 'read') should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * [C1 P1 "nuke" P2 "nuke"] equals [C1 P1 "duke" P2 "nuke"]
     */
    public final void testEquals_01() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertTrue(p_this.equals(p_that));
        assertTrue(p_this.equals(p_this));
        assertEquals(p_this.hashCode(), p_that.hashCode());
    }

    /**
     * two permissions are equal if the order of the Principals in 
     * the respective target names is not relevant.
     */
    public final void testEquals_02() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.c.Principal \"nuke\" a.b.Principal \"duke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertTrue(p_this.equals(p_that));
        assertEquals(p_this.hashCode(), p_that.hashCode());
    }

    /**
     * two permissions are not equal even if its have the same credential class  
     * and a different number of principals.
     */
    public final void testEquals_03() {

        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.b.Principal \"duke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.equals(p_that));
    }

    /**
     * two permissions are not equal if either of them has not the same 
     * credential class, principal class and principal name
     */
    public final void testEquals_04() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.c.Credential a.d.Principal \"buke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.equals(p_that));
    }

    /**
     * two permissions are not equal if either of them has not the same 
     * principal class and principal name
     */
    public final void testEquals_05() {
        s_that = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        s_this = "a.b.Credential a.d.Principal \"buke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.equals(p_that));
    }

    /**
     * [C1 P1 "duke"] equals [C1 P1 "duke"] and 
     * hashCode of them equals too.
     */
    public final void testEquals_06() {
        s_that = "a.b.Credential a.b.Principal \"duke\"";
        s_this = "a.b.Credential a.b.Principal \"duke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertTrue(p_this.equals(p_that));
        assertTrue(p_that.equals(p_this));
        assertEquals(p_this.hashCode(), p_that.hashCode());
    }

    /**
     * two permissions are not equal if either of them has not the same principal name
     */
    public final void testEquals_07() {
        s_that = "a.b.Credential a.b.Principal \"duke\"";
        s_this = "a.b.Credential a.b.Principal \"buke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.equals(p_that));
    }

    /**
     * Verifies that a permission object is equal to itself
     */
    public final void testEquals_08() {
        s_this = "a.b.Credential a.b.Principal \"buke\"";
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertTrue(p_this.equals(p_this));
    }

    /**
     * [C1 P1 "duke"] does not equal NULL 
     */
    public final void testEquals_09() {
        s_this = "a.b.Credential a.b.Principal \"buke\"";
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.equals(null));
        AuthPermission other = new AuthPermission("some");
        assertFalse(p_this.implies(other));
    }

    /**
     * two permissions are not equals if either of them has not the same credential class
     */
    public final void testEquals_10() {
        s_that = "a.b.Credential a.b.Principal \"duke\"";
        s_this = "a.c.Credential a.b.Principal \"duke\"";
        p_that = new PrivateCredentialPermission(s_that, "read");
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertFalse(p_this.equals(p_that));
        assertFalse(p_that.equals(p_this));
    }

    /**
     * the method newPermissionCollection() always returns null, 
     * the method getActions() always returns "read" and 
     * the method getCredentialClass() returns the name of the CredentialClass. 
     */
    public final void testGetCredentialClassAndGetAction() {
        s_this = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        p_this = new PrivateCredentialPermission(s_this, "read");
        assertEquals("read", p_this.getActions());
        assertEquals("a.b.Credential", p_this.getCredentialClass());
        assertNull(p_this.newPermissionCollection());
    }

    /**
     * Returns the set of principals which to represent like array[x][y]
     * Implementation specific.
     */
    public final void testGetPrincipals_01() {
        s_this = "a.b.Credential a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        p_this = new PrivateCredentialPermission(s_this, "read");
        String p[][] = p_this.getPrincipals();
        assertEquals("a.b.Principal", p[0][0]);
        assertEquals("duke", p[0][1]);
        assertEquals("a.c.Principal", p[1][0]);
        assertEquals("nuke", p[1][1]);
    }

    /**
     * the same as the method testGetPrincipals_01()
     */
    public final void testGetPrincipals_02() {
        s_this = "a.b.Credential a.d.Principal \"buke\" a.b.Principal \"duke\" a.c.Principal \"nuke\"";
        p_this = new PrivateCredentialPermission(s_this, "read");
        String p[][] = p_this.getPrincipals();
        assertEquals("a.d.Principal", p[0][0]);
        assertEquals("buke", p[0][1]);
        assertEquals("a.b.Principal", p[1][0]);
        assertEquals("duke", p[1][1]);
        assertEquals("a.c.Principal", p[2][0]);
        assertEquals("nuke", p[2][1]);
    }

    public final void testCtor() {

        class MyPrincipal implements Principal {
            String name;

            MyPrincipal(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }
        }

        MyPrincipal mp = new MyPrincipal("duke");
        MyPrincipal mp1 = new MyPrincipal("nuke");
        HashSet<Principal> hash = new HashSet<Principal>();
        hash.add(mp);
        hash.add(mp1);

        PrivateCredentialPermission p1 = new PrivateCredentialPermission(
                "java.lang.Object", hash);

        PrivateCredentialPermission p2 = new PrivateCredentialPermission(
                "java.lang.Object " + MyPrincipal.class.getName() + " \"duke\"",
                "read");
        assertFalse(p1.implies(p2));
        assertTrue(p2.implies(p1));

        PrivateCredentialPermission p3 = new PrivateCredentialPermission(
                "java.lang.Object", new HashSet<Principal>());

        PrivateCredentialPermission p4 = new PrivateCredentialPermission(
                "java.lang.Object * \"*\"", "read");

        assertTrue(p4.implies(p3));
    }

    public final void testDuplicates() {

        // string contains duplicate entries: b "c"
        PrivateCredentialPermission p = new PrivateCredentialPermission(
                "a b \"c\" b \"c\"", "read");

        assertEquals("Size", p.getPrincipals().length, 1);
    }

    public final void testImmutability() {
        PrivateCredentialPermission p = new PrivateCredentialPermission(
                "a b \"c\"", "read");

        assertTrue("Array reference", p.getPrincipals() != p.getPrincipals());
    }

    public final void testWhiteSpaces() {

        String[] illegalTargetNames = new String[] { "a\nb \"c\"", // \n - delimiter
                "a\tb \"c\"", // \t - delimiter
                "a\rb \"c\"", // \r - delimiter
                "a b\n\"c\"", // \n - delimiter
                "a b\t\"c\"", // \t - delimiter
                "a b\r\"c\"", // \r - delimiter
                "a  b \"c\"", // two spaces between credential and principal
                "a b  \"c\"" // two spaces between principal class and name
        };

        for (String element : illegalTargetNames) {
            try {
                new PrivateCredentialPermission(element, "read");
                fail("No expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
            }
        }

        // principal name has a space
        PrivateCredentialPermission p = new PrivateCredentialPermission(
                "a b \"c c\"", "read");

        assertEquals("Principal name:", "c c", p.getPrincipals()[0][1]);
    }

    public final void testSerialization_Wildcard() throws Exception {

        PrivateCredentialPermission all = new PrivateCredentialPermission(
                "* * \"*\"", "read");
        PrivateCredentialPermission p = new PrivateCredentialPermission(
                "a b \"c\"", "read");

        assertTrue(all.implies(p));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oOut = new ObjectOutputStream(out);

        oOut.writeObject(all);
        oOut.flush();
        oOut.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream oIn = new ObjectInputStream(in);

        PrivateCredentialPermission newAll = (PrivateCredentialPermission) oIn
                .readObject();
        
        assertTrue("New implies", newAll.implies(p));
        assertEquals("Equals", all, newAll);
    }

    public final void testSerialization_Golden() throws Exception {

        new PrivateCredentialPermission("a b \"c\" d \"e\"", "read");

        ByteArrayInputStream in = new ByteArrayInputStream(gForm);
        ObjectInputStream sIn = new ObjectInputStream(in);

        PrivateCredentialPermission p = (PrivateCredentialPermission) sIn
                .readObject();

        assertEquals("CredentialClass ", "a", p.getCredentialClass());

        String[][] principals = p.getPrincipals();
        assertEquals("Size:", 1, principals.length);
        assertEquals("PrincipalClass:", "b", principals[0][0]);
        assertEquals("PrincipalName:", "c", principals[0][1]);
    }

    public final void testSerialization_Self() throws Exception {

        ByteArrayInputStream in = new ByteArrayInputStream(selfForm);
        ObjectInputStream sIn = new ObjectInputStream(in);

        PrivateCredentialPermission p = (PrivateCredentialPermission) sIn
                .readObject();

        assertEquals("CredentialClass ", "a", p.getCredentialClass());

        String[][] principals = p.getPrincipals();
        assertEquals("Size:", 1, principals.length);
        assertEquals("PrincipalClass:", "b", principals[0][0]);
        assertEquals("PrincipalName:", "c", principals[0][1]);
    }

    // Golden: PrivateCredentialPermission("a b \"c\"","read");
    // generated using public API
    public static final byte[] gForm = new byte[] { (byte) 0xac, (byte) 0xed,
            (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72, (byte) 0x00,
            (byte) 0x2f, (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
            (byte) 0x78, (byte) 0x2e, (byte) 0x73, (byte) 0x65, (byte) 0x63,
            (byte) 0x75, (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
            (byte) 0x2e, (byte) 0x61, (byte) 0x75, (byte) 0x74, (byte) 0x68,
            (byte) 0x2e, (byte) 0x50, (byte) 0x72, (byte) 0x69, (byte) 0x76,
            (byte) 0x61, (byte) 0x74, (byte) 0x65, (byte) 0x43, (byte) 0x72,
            (byte) 0x65, (byte) 0x64, (byte) 0x65, (byte) 0x6e, (byte) 0x74,
            (byte) 0x69, (byte) 0x61, (byte) 0x6c, (byte) 0x50, (byte) 0x65,
            (byte) 0x72, (byte) 0x6d, (byte) 0x69, (byte) 0x73, (byte) 0x73,
            (byte) 0x69, (byte) 0x6f, (byte) 0x6e, (byte) 0x49, (byte) 0x55,
            (byte) 0xdc, (byte) 0x77, (byte) 0x7b, (byte) 0x50, (byte) 0x7f,
            (byte) 0x4c, (byte) 0x02, (byte) 0x00, (byte) 0x03, (byte) 0x5a,
            (byte) 0x00, (byte) 0x07, (byte) 0x74, (byte) 0x65, (byte) 0x73,
            (byte) 0x74, (byte) 0x69, (byte) 0x6e, (byte) 0x67, (byte) 0x4c,
            (byte) 0x00, (byte) 0x0f, (byte) 0x63, (byte) 0x72, (byte) 0x65,
            (byte) 0x64, (byte) 0x65, (byte) 0x6e, (byte) 0x74, (byte) 0x69,
            (byte) 0x61, (byte) 0x6c, (byte) 0x43, (byte) 0x6c, (byte) 0x61,
            (byte) 0x73, (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x12,
            (byte) 0x4c, (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
            (byte) 0x2f, (byte) 0x6c, (byte) 0x61, (byte) 0x6e, (byte) 0x67,
            (byte) 0x2f, (byte) 0x53, (byte) 0x74, (byte) 0x72, (byte) 0x69,
            (byte) 0x6e, (byte) 0x67, (byte) 0x3b, (byte) 0x4c, (byte) 0x00,
            (byte) 0x0a, (byte) 0x70, (byte) 0x72, (byte) 0x69, (byte) 0x6e,
            (byte) 0x63, (byte) 0x69, (byte) 0x70, (byte) 0x61, (byte) 0x6c,
            (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x0f, (byte) 0x4c,
            (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2f,
            (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6c, (byte) 0x2f,
            (byte) 0x53, (byte) 0x65, (byte) 0x74, (byte) 0x3b, (byte) 0x78,
            (byte) 0x72, (byte) 0x00, (byte) 0x18, (byte) 0x6a, (byte) 0x61,
            (byte) 0x76, (byte) 0x61, (byte) 0x2e, (byte) 0x73, (byte) 0x65,
            (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x69, (byte) 0x74,
            (byte) 0x79, (byte) 0x2e, (byte) 0x50, (byte) 0x65, (byte) 0x72,
            (byte) 0x6d, (byte) 0x69, (byte) 0x73, (byte) 0x73, (byte) 0x69,
            (byte) 0x6f, (byte) 0x6e, (byte) 0xb1, (byte) 0xc6, (byte) 0xe1,
            (byte) 0x3f, (byte) 0x28, (byte) 0x57, (byte) 0x51, (byte) 0x7e,
            (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x4c, (byte) 0x00,
            (byte) 0x04, (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65,
            (byte) 0x74, (byte) 0x00, (byte) 0x12, (byte) 0x4c, (byte) 0x6a,
            (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2f, (byte) 0x6c,
            (byte) 0x61, (byte) 0x6e, (byte) 0x67, (byte) 0x2f, (byte) 0x53,
            (byte) 0x74, (byte) 0x72, (byte) 0x69, (byte) 0x6e, (byte) 0x67,
            (byte) 0x3b, (byte) 0x78, (byte) 0x70, (byte) 0x74, (byte) 0x00,
            (byte) 0x07, (byte) 0x61, (byte) 0x20, (byte) 0x62, (byte) 0x20,
            (byte) 0x22, (byte) 0x63, (byte) 0x22, (byte) 0x00, (byte) 0x74,
            (byte) 0x00, (byte) 0x01, (byte) 0x61, (byte) 0x70 };

    // Self generated: PrivateCredentialPermission("a b \"c\"","read");
    // Note: Set principals = new LinkedHashSet()
    // generated using public API
    public static final byte[] selfForm = { (byte) 0xac, (byte) 0xed,
            (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72, (byte) 0x00,
            (byte) 0x2f, (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
            (byte) 0x78, (byte) 0x2e, (byte) 0x73, (byte) 0x65, (byte) 0x63,
            (byte) 0x75, (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
            (byte) 0x2e, (byte) 0x61, (byte) 0x75, (byte) 0x74, (byte) 0x68,
            (byte) 0x2e, (byte) 0x50, (byte) 0x72, (byte) 0x69, (byte) 0x76,
            (byte) 0x61, (byte) 0x74, (byte) 0x65, (byte) 0x43, (byte) 0x72,
            (byte) 0x65, (byte) 0x64, (byte) 0x65, (byte) 0x6e, (byte) 0x74,
            (byte) 0x69, (byte) 0x61, (byte) 0x6c, (byte) 0x50, (byte) 0x65,
            (byte) 0x72, (byte) 0x6d, (byte) 0x69, (byte) 0x73, (byte) 0x73,
            (byte) 0x69, (byte) 0x6f, (byte) 0x6e, (byte) 0x49, (byte) 0x55,
            (byte) 0xdc, (byte) 0x77, (byte) 0x7b, (byte) 0x50, (byte) 0x7f,
            (byte) 0x4c, (byte) 0x02, (byte) 0x00, (byte) 0x03, (byte) 0x5a,
            (byte) 0x00, (byte) 0x07, (byte) 0x74, (byte) 0x65, (byte) 0x73,
            (byte) 0x74, (byte) 0x69, (byte) 0x6e, (byte) 0x67, (byte) 0x4c,
            (byte) 0x00, (byte) 0x0f, (byte) 0x63, (byte) 0x72, (byte) 0x65,
            (byte) 0x64, (byte) 0x65, (byte) 0x6e, (byte) 0x74, (byte) 0x69,
            (byte) 0x61, (byte) 0x6c, (byte) 0x43, (byte) 0x6c, (byte) 0x61,
            (byte) 0x73, (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x12,
            (byte) 0x4c, (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
            (byte) 0x2f, (byte) 0x6c, (byte) 0x61, (byte) 0x6e, (byte) 0x67,
            (byte) 0x2f, (byte) 0x53, (byte) 0x74, (byte) 0x72, (byte) 0x69,
            (byte) 0x6e, (byte) 0x67, (byte) 0x3b, (byte) 0x4c, (byte) 0x00,
            (byte) 0x0a, (byte) 0x70, (byte) 0x72, (byte) 0x69, (byte) 0x6e,
            (byte) 0x63, (byte) 0x69, (byte) 0x70, (byte) 0x61, (byte) 0x6c,
            (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x0f, (byte) 0x4c,
            (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2f,
            (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6c, (byte) 0x2f,
            (byte) 0x53, (byte) 0x65, (byte) 0x74, (byte) 0x3b, (byte) 0x78,
            (byte) 0x72, (byte) 0x00, (byte) 0x18, (byte) 0x6a, (byte) 0x61,
            (byte) 0x76, (byte) 0x61, (byte) 0x2e, (byte) 0x73, (byte) 0x65,
            (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x69, (byte) 0x74,
            (byte) 0x79, (byte) 0x2e, (byte) 0x50, (byte) 0x65, (byte) 0x72,
            (byte) 0x6d, (byte) 0x69, (byte) 0x73, (byte) 0x73, (byte) 0x69,
            (byte) 0x6f, (byte) 0x6e, (byte) 0xb1, (byte) 0xc6, (byte) 0xe1,
            (byte) 0x3f, (byte) 0x28, (byte) 0x57, (byte) 0x51, (byte) 0x7e,
            (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x4c, (byte) 0x00,
            (byte) 0x04, (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65,
            (byte) 0x74, (byte) 0x00, (byte) 0x12, (byte) 0x4c, (byte) 0x6a,
            (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2f, (byte) 0x6c,
            (byte) 0x61, (byte) 0x6e, (byte) 0x67, (byte) 0x2f, (byte) 0x53,
            (byte) 0x74, (byte) 0x72, (byte) 0x69, (byte) 0x6e, (byte) 0x67,
            (byte) 0x3b, (byte) 0x78, (byte) 0x70, (byte) 0x74, (byte) 0x00,
            (byte) 0x07, (byte) 0x61, (byte) 0x20, (byte) 0x62, (byte) 0x20,
            (byte) 0x22, (byte) 0x63, (byte) 0x22, (byte) 0x00, (byte) 0x74,
            (byte) 0x00, (byte) 0x01, (byte) 0x61, (byte) 0x73, (byte) 0x72,
            (byte) 0x00, (byte) 0x17, (byte) 0x6a, (byte) 0x61, (byte) 0x76,
            (byte) 0x61, (byte) 0x2e, (byte) 0x75, (byte) 0x74, (byte) 0x69,
            (byte) 0x6c, (byte) 0x2e, (byte) 0x4c, (byte) 0x69, (byte) 0x6e,
            (byte) 0x6b, (byte) 0x65, (byte) 0x64, (byte) 0x48, (byte) 0x61,
            (byte) 0x73, (byte) 0x68, (byte) 0x53, (byte) 0x65, (byte) 0x74,
            (byte) 0xd8, (byte) 0x6c, (byte) 0xd7, (byte) 0x5a, (byte) 0x95,
            (byte) 0xdd, (byte) 0x2a, (byte) 0x1e, (byte) 0x02, (byte) 0x00,
            (byte) 0x00, (byte) 0x78, (byte) 0x72, (byte) 0x00, (byte) 0x11,
            (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2e,
            (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6c, (byte) 0x2e,
            (byte) 0x48, (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x53,
            (byte) 0x65, (byte) 0x74, (byte) 0xba, (byte) 0x44, (byte) 0x85,
            (byte) 0x95, (byte) 0x96, (byte) 0xb8, (byte) 0xb7, (byte) 0x34,
            (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x70,
            (byte) 0x77, (byte) 0x0c, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x10, (byte) 0x3f, (byte) 0x40, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x73,
            (byte) 0x72, (byte) 0x00, (byte) 0x39, (byte) 0x6a, (byte) 0x61,
            (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2e, (byte) 0x73,
            (byte) 0x65, (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x69,
            (byte) 0x74, (byte) 0x79, (byte) 0x2e, (byte) 0x61, (byte) 0x75,
            (byte) 0x74, (byte) 0x68, (byte) 0x2e, (byte) 0x50, (byte) 0x72,
            (byte) 0x69, (byte) 0x76, (byte) 0x61, (byte) 0x74, (byte) 0x65,
            (byte) 0x43, (byte) 0x72, (byte) 0x65, (byte) 0x64, (byte) 0x65,
            (byte) 0x6e, (byte) 0x74, (byte) 0x69, (byte) 0x61, (byte) 0x6c,
            (byte) 0x50, (byte) 0x65, (byte) 0x72, (byte) 0x6d, (byte) 0x69,
            (byte) 0x73, (byte) 0x73, (byte) 0x69, (byte) 0x6f, (byte) 0x6e,
            (byte) 0x24, (byte) 0x43, (byte) 0x72, (byte) 0x65, (byte) 0x64,
            (byte) 0x4f, (byte) 0x77, (byte) 0x6e, (byte) 0x65, (byte) 0x72,
            (byte) 0xb2, (byte) 0x2e, (byte) 0x56, (byte) 0x16, (byte) 0xb9,
            (byte) 0x03, (byte) 0x74, (byte) 0x36, (byte) 0x02, (byte) 0x00,
            (byte) 0x02, (byte) 0x4c, (byte) 0x00, (byte) 0x0e, (byte) 0x70,
            (byte) 0x72, (byte) 0x69, (byte) 0x6e, (byte) 0x63, (byte) 0x69,
            (byte) 0x70, (byte) 0x61, (byte) 0x6c, (byte) 0x43, (byte) 0x6c,
            (byte) 0x61, (byte) 0x73, (byte) 0x73, (byte) 0x74, (byte) 0x00,
            (byte) 0x12, (byte) 0x4c, (byte) 0x6a, (byte) 0x61, (byte) 0x76,
            (byte) 0x61, (byte) 0x2f, (byte) 0x6c, (byte) 0x61, (byte) 0x6e,
            (byte) 0x67, (byte) 0x2f, (byte) 0x53, (byte) 0x74, (byte) 0x72,
            (byte) 0x69, (byte) 0x6e, (byte) 0x67, (byte) 0x3b, (byte) 0x4c,
            (byte) 0x00, (byte) 0x0d, (byte) 0x70, (byte) 0x72, (byte) 0x69,
            (byte) 0x6e, (byte) 0x63, (byte) 0x69, (byte) 0x70, (byte) 0x61,
            (byte) 0x6c, (byte) 0x4e, (byte) 0x61, (byte) 0x6d, (byte) 0x65,
            (byte) 0x74, (byte) 0x00, (byte) 0x12, (byte) 0x4c, (byte) 0x6a,
            (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2f, (byte) 0x6c,
            (byte) 0x61, (byte) 0x6e, (byte) 0x67, (byte) 0x2f, (byte) 0x53,
            (byte) 0x74, (byte) 0x72, (byte) 0x69, (byte) 0x6e, (byte) 0x67,
            (byte) 0x3b, (byte) 0x78, (byte) 0x70, (byte) 0x74, (byte) 0x00,
            (byte) 0x01, (byte) 0x62, (byte) 0x74, (byte) 0x00, (byte) 0x01,
            (byte) 0x63, (byte) 0x78 };
}
