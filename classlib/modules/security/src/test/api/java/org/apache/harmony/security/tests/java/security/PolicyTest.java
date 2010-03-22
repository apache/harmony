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

import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Security;
import java.security.SecurityPermission;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;

import junit.framework.TestCase;

import org.apache.harmony.security.tests.support.SecurityChecker;
import org.apache.harmony.security.tests.support.TestUtils;

import tests.support.resource.Support_Resources;

/**
 * Tests for <code>Policy</code>
 */
public class PolicyTest extends TestCase {

    public static final String JAVA_SECURITY_POLICY = "java.security.policy";

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PolicyTest.class);
    }

    /**
     * @tests java.security.Policy#setPolicy(java.security.Policy)
     */
    public void test_setPolicyLjava_security_Policy() {
        SecurityManager old = System.getSecurityManager();
        Policy oldPolicy = Policy.getPolicy();
        try {
            SecurityChecker checker = new SecurityChecker(
                new SecurityPermission("setPolicy"), true);
            System.setSecurityManager(checker);
            Policy custom = new TestProvider();
            Policy.setPolicy(custom);
            assertTrue(checker.checkAsserted);
            assertSame(custom, Policy.getPolicy());

            checker.reset();
            checker.enableAccess = false;
            try {
                Policy.setPolicy(new TestProvider());
                fail("SecurityException is intercepted");
            } catch (SecurityException ok) {
            }
        } finally {
            System.setSecurityManager(old);
            Policy.setPolicy(oldPolicy);
        }
    }

    /**
     * @tests java.security.Policy#getPolicy()
     */
    public void test_getPolicy() {
        SecurityManager old = System.getSecurityManager();
        Policy oldPolicy = Policy.getPolicy();
        try {
            Policy.setPolicy(new TestProvider());
            SecurityChecker checker = new SecurityChecker(
                new SecurityPermission("getPolicy"), true);
            System.setSecurityManager(checker);
            Policy.getPolicy();
            assertTrue(checker.checkAsserted);

            checker.reset();
            checker.enableAccess = false;
            try {
                Policy.getPolicy();
                fail("SecurityException is intercepted");
            } catch (SecurityException ok) {
            }
        } finally {
            System.setSecurityManager(old);
            Policy.setPolicy(oldPolicy);
        }
    }

    public static class TestProvider extends Policy {

        PermissionCollection pc;

        public PermissionCollection getPermissions(CodeSource cs) {
            return pc;
        }

        public void refresh() {
        }
    }
    
    /** 
     * Tests that getPermissions() does proper permission evaluation.
     */
    public void testGetPermissions() {
        SecurityPermission sp = new SecurityPermission("abc");
        SecurityPermission sp2 = new SecurityPermission("fbdf");
        PermissionCollection spc = sp.newPermissionCollection();
        spc.add(sp2);
        ProtectionDomain pd = new ProtectionDomain(null, null);
        ProtectionDomain pd2 = new ProtectionDomain(null, spc);
        TestProvider policy = new TestProvider();
        policy.pc = sp.newPermissionCollection();
                 
        //case1: empty policy, no static permissions in PD
        PermissionCollection pc4pd = policy.getPermissions(pd);
        assertNotNull(pc4pd);
        Enumeration en = pc4pd.elements();
        assertFalse(en.hasMoreElements());
        
        //case2: empty policy, some static permissions in PD
        pc4pd = policy.getPermissions(pd2);
        assertNotNull(pc4pd);
        //no check for static permissions
        
        //case3: non-empty policy, no static permissions in PD
        policy.pc.add(sp);
        pc4pd = policy.getPermissions(pd);
        assertNotNull(pc4pd);
        Collection c = new HashSet();
        for (en = pc4pd.elements();en.hasMoreElements(); c.add(en.nextElement())) {
        }

        assertTrue(c.contains(sp));
        
        //case4: non-empty policy, some static permissions in PD
        pc4pd = policy.getPermissions(pd2);
        assertNotNull(pc4pd);
        c = new HashSet();
        for (en = pc4pd.elements();en.hasMoreElements(); c.add(en.nextElement())) {
        }

        assertTrue(c.contains(sp));
        //no check for static permissions
    }
    
    /**
     * @tests java.security.Policy#getPolicy()
     * @tests java.security.Policy#setPolicy()
     */
    public void testResetingPolicyToDefault() {

        Policy oldPolicy = Policy.getPolicy();
        assertNotNull("Got a null system security policy", oldPolicy);

        try {

            Policy.setPolicy(null); // passing null resets policy
            Policy newPolicy = Policy.getPolicy();

            assertNotNull(newPolicy);
            assertNotSame(oldPolicy, newPolicy);

            assertEquals("Policy class name", Security
                    .getProperty("policy.provider"), newPolicy.getClass()
                    .getName());
        } finally {
            Policy.setPolicy(oldPolicy);
        }
    }
    
    /**
     * Test property expansion in policy files 
     */
    public void testPropertyExpansion() throws Exception {

        // Regression for HARMONY-1963 and HARMONY-2910
        String policyFile = Support_Resources
                .getAbsoluteResourcePath("PolicyTest.txt");
        String oldJavaPolicy = System.getProperty(JAVA_SECURITY_POLICY);
        Policy oldPolicy = Policy.getPolicy();

        try {
            System.setProperty(JAVA_SECURITY_POLICY, policyFile);

            // test: absolute paths
            assertCodeBasePropertyExpansion("/11111/*", "/11111/-");
            assertCodeBasePropertyExpansion("/22222/../22222/*", "/22222/-");
            assertCodeBasePropertyExpansion("/33333/*", "/33333/../33333/-");
            assertCodeBasePropertyExpansion("/44444/../44444/-", "/44444/*");
            assertCodeBasePropertyExpansion("/55555/../55555/-", "/55555/../55555/-");
            assertCodeBasePropertyExpansion("/666 66 66/-", "/666 66 66/-");

            // test: relative paths
            assertCodeBasePropertyExpansion("11111/*", "11111/-");
            assertCodeBasePropertyExpansion("22222/../22222/*", "22222/-");
            assertCodeBasePropertyExpansion("33333/*", "33333/../33333/-");
            assertCodeBasePropertyExpansion("44444/../44444/-", "44444/*");
            assertCodeBasePropertyExpansion("55555/../55555/-", "55555/../55555/-");
            assertCodeBasePropertyExpansion("666 66 66/-", "666 66 66/-");
        } finally {
            TestUtils.setSystemProperty(JAVA_SECURITY_POLICY, oldJavaPolicy);
            Policy.setPolicy(oldPolicy);
        }
    }

    /**
     * Asserts codeBase property expansion in policy file
     * 
     * @param codeSourceURL -
     *            code source for policy object
     * @param codeBaseURL -
     *            system propery value for expansion in policy file
     */
    private void assertCodeBasePropertyExpansion(String codeSourceURL,
            String codeBaseURL) throws Exception {

        Policy.setPolicy(null); //reset policy
        System.setProperty("test.bin.dir", codeBaseURL);

        Policy p = Policy.getPolicy();
        CodeSource codeSource = new CodeSource(
                new URL("file:" + codeSourceURL),
                (java.security.cert.Certificate[]) null);

        PermissionCollection pCollection = p.getPermissions(codeSource);
        Enumeration<Permission> elements = pCollection.elements();

        SecurityPermission perm = new SecurityPermission(
                "codeBaseForPolicyTest");

        while (elements.hasMoreElements()) {
            if (elements.nextElement().equals(perm)) {
                return; // passed
            }
        }
        fail("Failed to find SecurityPermission for codeSource="
                + codeSourceURL + ", codeBase=" + codeBaseURL);
    }
}
