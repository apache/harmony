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
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Security;
import java.security.SecurityPermission;

import junit.framework.TestCase;

/**
 * Tests for <code>Policy</code>
 * 
 */

public class Policy_ImplTest extends TestCase {

    public static class TestProvider extends Policy {

        PermissionCollection pc;

        public PermissionCollection getPermissions(CodeSource cs) {
            return pc;
        }

        public void refresh() {
        }
    }

    /**
     * Tests loading of a default provider, both valid and invalid class
     * references.
     */
    public void testGetPolicy_LoadDefaultProvider() {
        Policy oldPolicy = Policy.getPolicy();
        String POLICY_PROVIDER = "policy.provider";
        String oldProvider = Security.getProperty(POLICY_PROVIDER);
        try {
            Security.setProperty(POLICY_PROVIDER, TestProvider.class.getName());
            Policy.setPolicy(null);
            Policy p = Policy.getPolicy();
            assertNotNull(p);
            assertEquals(TestProvider.class.getName(), p.getClass().getName());

            Security.setProperty(POLICY_PROVIDER, "a.b.c.D");
            Policy.setPolicy(null);
            p = Policy.getPolicy();
            assertNotNull(p);
            //exact type of default provider does not matter
            //assertEquals(DefaultPolicy.class.getName(), p.getClass().getName());
        } finally {
            Security.setProperty(POLICY_PROVIDER, (oldProvider == null) ? ""
                : oldProvider);
            Policy.setPolicy(oldPolicy);
        }
    }
    
    /** 
     * Tests that implies() does proper permission evaluation.
     */
    public void testImplies() {
        TestProvider policy = new TestProvider();
        SecurityPermission sp = new SecurityPermission("abc");
        policy.pc = sp.newPermissionCollection();
        
        policy.pc.add(sp);
        assertTrue(policy.implies(new ProtectionDomain(null, null), sp));
        assertFalse(policy.implies(null, sp));
        assertFalse(policy.implies(new ProtectionDomain(null, null), null));
        
        //RI throws NullPointerException.
        try {
            policy.implies(null, null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected.
        }
        
        
        ProtectionDomain pd = new ProtectionDomain(null, policy.pc);
        policy.pc = null;
        assertTrue(policy.implies(pd, sp));        
        assertFalse(policy.implies(pd, new AllPermission()));
    }

}
