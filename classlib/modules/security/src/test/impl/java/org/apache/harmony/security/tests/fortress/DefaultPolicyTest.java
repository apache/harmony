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

package org.apache.harmony.security.tests.fortress;

import java.net.URL;
import java.security.cert.Certificate;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import org.apache.harmony.security.PolicyEntry;
import org.apache.harmony.security.UnresolvedPrincipal;
import org.apache.harmony.security.fortress.DefaultPolicy;
import org.apache.harmony.security.fortress.DefaultPolicyParser;
import junit.framework.TestCase;


/**
 * Tests for DefaultPolicy
 * 
 */
public class DefaultPolicyTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DefaultPolicyTest.class);
    }

    static class TestParser extends DefaultPolicyParser {

        PolicyEntry[] content;

        public TestParser(PolicyEntry[] content) {
            this.content = content;
        }

        public Collection parse(URL location, Properties system)
            throws Exception {
            if (content != null) {
                return Arrays.asList(content);
            }
            throw new RuntimeException();
        }
    }

    /**
     * Tests that policy is really resetted on refresh(). 
     */
    public void testRefresh() {
        Permission sp = new SecurityPermission("sdf");
        PolicyEntry[] pe = new PolicyEntry[] { new PolicyEntry(null, null,
            Arrays.asList(new Permission[] { sp })) };
        TestParser tp = new TestParser(pe);
        DefaultPolicy policy = new DefaultPolicy(tp);
        CodeSource cs = new CodeSource(null, (Certificate[])null);
        assertTrue(policy.getPermissions(cs).implies(sp));

        tp.content = new PolicyEntry[0];
        policy.refresh();
        assertFalse(policy.getPermissions(cs).implies(sp));

        tp.content = null;
        policy.refresh();
        assertFalse(policy.getPermissions(cs).implies(sp));
    }

    /**
     * Tests that refresh() does not fail on failing parser.
     */
    public void testRefresh_Failure() {
        CodeSource cs = new CodeSource(null, (Certificate[])null);
        DefaultPolicy policy = new DefaultPolicy(new TestParser(null));
        policy.refresh();
        assertFalse(policy.getPermissions(cs).elements().hasMoreElements());
    }

    /**
     * Tests proper policy evaluation for CodeSource parameters.
     */
    public void testGetPermissions_CodeSource() throws Exception {
        CodeSource cs = new CodeSource(null, (Certificate[])null);
        CodeSource cs2 = new CodeSource(new URL("http://a.b.c"),
            (Certificate[])null);
        Permission sp1 = new SecurityPermission("aaa");
        Permission sp2 = new SecurityPermission("bbb");
        Permission sp3 = new SecurityPermission("ccc");
        PolicyEntry pe1 = new PolicyEntry(cs, null, Arrays
            .asList(new Permission[] { sp1 }));
        PolicyEntry pe2 = new PolicyEntry(cs2, new HashSet(), Arrays
            .asList(new Permission[] { sp2 }));
        PolicyEntry pe3 = new PolicyEntry(cs, Arrays
            .asList(new Principal[] { new FakePrincipal("qqq") }), Arrays
            .asList(new Permission[] { sp3 }));
        PolicyEntry[] peArray = new PolicyEntry[] {
            pe1, pe2, pe3 };
        DefaultPolicy policy = new DefaultPolicy(new TestParser(peArray));

        assertTrue(policy.getPermissions(cs).implies(sp1));
        assertFalse(policy.getPermissions(cs).implies(sp2));
        assertFalse(policy.getPermissions(cs).implies(sp3));

        assertTrue(policy.getPermissions(cs2).implies(sp1));
        assertTrue(policy.getPermissions(cs2).implies(sp2));
        assertFalse(policy.getPermissions(cs2).implies(sp3));
    }

    /**
     * Tests proper policy evaluation for ProtectionDomain parameters.
     */
    public void testGetPermissions_ProtectionDomain() throws Exception {
        Permission sp1 = new SecurityPermission("aaa");
        Permission sp2 = new SecurityPermission("bbb");
        Permission sp3 = new SecurityPermission("ccc");
        Permission sp4 = new SecurityPermission("ddd");
        Permission spZ = new SecurityPermission("zzz");
        PermissionCollection pcZ = spZ.newPermissionCollection();
        pcZ.add(spZ);
        CodeSource cs = new CodeSource(null, (Certificate[])null);
        CodeSource cs2 = new CodeSource(new URL("http://a.b.c"),
            (Certificate[])null);
        ProtectionDomain pd1 = new ProtectionDomain(cs, null);
        ProtectionDomain pd2 = new ProtectionDomain(cs2, pcZ, null,
            new Principal[] { new FakePrincipal("qqq") });

        PolicyEntry pe1 = new PolicyEntry(cs, null, Arrays
            .asList(new Permission[] { sp1 }));
        PolicyEntry pe2 = new PolicyEntry(cs2, Arrays
            .asList(new Principal[] { new UnresolvedPrincipal(
                UnresolvedPrincipal.WILDCARD, UnresolvedPrincipal.WILDCARD) }),
            Arrays.asList(new Permission[] { sp2 }));
        PolicyEntry pe3 = new PolicyEntry(cs, Arrays
            .asList(new Principal[] { new UnresolvedPrincipal(
                FakePrincipal.class.getName(), "qqq") }), Arrays
            .asList(new Permission[] { sp3 }));
        PolicyEntry pe4 = new PolicyEntry(cs2, Arrays
            .asList(new Principal[] { new UnresolvedPrincipal(
                FakePrincipal.class.getName(), "ttt") }), Arrays
            .asList(new Permission[] { sp4 }));
        PolicyEntry[] peArray = new PolicyEntry[] {
            pe1, pe2, pe3, pe4 };
        DefaultPolicy policy = new DefaultPolicy(new TestParser(peArray));

        assertTrue(policy.getPermissions(pd1).implies(sp1));
        assertFalse(policy.getPermissions(pd1).implies(sp2));
        assertFalse(policy.getPermissions(pd1).implies(sp3));
        assertFalse(policy.getPermissions(pd1).implies(sp4));

        assertTrue(policy.getPermissions(pd2).implies(sp1));
        assertTrue(policy.getPermissions(pd2).implies(sp2));
        assertTrue(policy.getPermissions(pd2).implies(sp3));
        assertFalse(policy.getPermissions(pd2).implies(sp4));
    }
}
