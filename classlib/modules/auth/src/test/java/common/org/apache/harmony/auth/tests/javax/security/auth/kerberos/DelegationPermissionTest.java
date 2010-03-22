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

package org.apache.harmony.auth.tests.javax.security.auth.kerberos;

import java.security.AllPermission;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.SecurityPermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.security.auth.kerberos.DelegationPermission;
import javax.security.auth.kerberos.ServicePermission;

import junit.framework.TestCase;


/** 
 * Tests DelegationPermission class implementation.
 */
public class DelegationPermissionTest extends TestCase {

    /**
     * testing of a correct ctor
     */
    public void testCtor() {
        String name1 = "\"aaa.bbb.com@CCC.COM\" \"ccc.ddd.com@DDD.COM\"";
        DelegationPermission dp;
        dp = new DelegationPermission(name1);
        assertEquals(name1, dp.getName());
        assertEquals("",dp.getActions());
        dp = new DelegationPermission(name1, "action");
        assertEquals("",dp.getActions());
        dp = new DelegationPermission(name1, null);
        assertEquals("",dp.getActions());
    }

    /**
     * testing of a incorrect ctor
     */
    public void testFailCtor() {
        try {
            new DelegationPermission(null);
            fail("no expected NPE");
        } catch(NullPointerException e){
        }
        try {
            new DelegationPermission("");
            fail("no expected IAE");
        } catch(IllegalArgumentException e){
        }
        try {
            new DelegationPermission("\"aaa.bbb.com\" ccc.ddd.com");
            fail("Target name must be enveloped by quotes");
        } catch(IllegalArgumentException e){
        }
        try {
            new DelegationPermission("\"aaa.bbb.com\" ccc.ddd.com\"");
            fail("Target name must be enveloped by quotes");
        } catch(IllegalArgumentException e){
        }
        try {
            new DelegationPermission("\"aaa.bbb.com\" \"ccc.ddd.com");
            fail("Target name must be enveloped by quotes");
        } catch(IllegalArgumentException e){
        }
        try {
            new DelegationPermission("\" \" \" \"");
            //TODO: fail("Target name is empty");
        }catch(IllegalArgumentException e){
        }
        try {
            new DelegationPermission("\"\"");
            fail("Target name is incorrect");
        } catch(IllegalArgumentException e){
        } 
        try {
            new DelegationPermission("\"aaa.bbb.com\" \"\"");
            fail("service principal is empty");
        } catch(IllegalArgumentException e){
        }
        try {
            new DelegationPermission("\"\" \"aaa.bbb.com\"");
            fail("subordinate service principal is empty");
        } catch(IllegalArgumentException e){
        }

    }

    public void testFailCtor_2() {
        try {
            new DelegationPermission("\"AAA\"");
            fail("target name should be specifies a pair of kerberos service principals");
        } catch (IllegalArgumentException e) {
        }
    } 
    
    // testing of the equals method
    @SuppressWarnings("serial")
    public void testEquals() {
        DelegationPermission dp1 = new DelegationPermission("\"AAA\" \"BBB\"");
        DelegationPermission dp2 = new DelegationPermission("\"AAA\" \"BBB\"");
        
        assertTrue(dp1.equals(dp1));
        assertFalse(dp1.equals(new DelegationPermission("\"aaa\" \"bbb\"")));
        assertTrue(dp2.equals(dp1));
        assertTrue(dp1.equals(dp2));
        assertTrue(dp1.hashCode() == dp2.hashCode());
        assertFalse(dp1.equals(new BasicPermission("\"AAA\""){}));
    }
    // testing of the implies method
    public void testImplies() {
        DelegationPermission dp1 = new DelegationPermission("\"AAA\" \"BBB\"");
        DelegationPermission dp2 = new DelegationPermission("\"BBB\" \"AAA\"");
        assertFalse(dp1.implies(dp2));
        assertFalse(dp2.implies(dp1));
        assertTrue(dp1.implies(dp1));
        assertFalse(dp1.implies(new ServicePermission("aaa", "accept")));
        assertFalse(dp1.implies(null));
    }
    
    // testing of the KrbDelegationPermissionCollection
    
    
    // testing of the add collection method
    public void testAddCollection()   {
        DelegationPermission dp = new DelegationPermission("\"AAA\" \"BBB\"");
        PermissionCollection pc1 = dp.newPermissionCollection();
        PermissionCollection pc2 = dp.newPermissionCollection();
        assertNotSame(pc1, pc2);
        pc1.add(new DelegationPermission("\"BBB\" \"AAA\""));
        try {
            pc1.add(new SecurityPermission("aaa"));
            fail("should not add non DelegationPermission");
        } catch (IllegalArgumentException e){
        }
		try {
		    pc1.add(null);
		    fail("permission is null");
		} catch (IllegalArgumentException e) {
		}
        pc1.setReadOnly();
		try {
			pc1.add(new DelegationPermission("\"CCC\" \"AAA\""));
			fail("read-only flag is ignored");
		} catch (SecurityException e) {
		}
    }
    
    public void testImpliesCollection(){
        
        Permission ap = new AllPermission();
        Permission p = new DelegationPermission("\"AAA\" \"BBB\"");
        PermissionCollection pc = p.newPermissionCollection();
        assertFalse(pc.implies(ap));
        assertFalse(pc.implies(p));
        pc.add(p);
        assertTrue(pc.implies(p));
        assertFalse(pc.implies(null));
        DelegationPermission dp1 = new DelegationPermission("\"AAA\" \"BBB\"");
        assertTrue(dp1.implies(dp1));
        DelegationPermission dp2 = new DelegationPermission("\"BBB\" \"AAA\"");
        assertFalse(dp1.implies(dp2));
        assertFalse(dp1.implies(null));
        assertFalse(dp1.implies(new ServicePermission("aaa", "accept")));
    }

	public void testElements() throws Exception {
        Permission p = new DelegationPermission("\"AAA\" \"BBB\"");
        PermissionCollection pc = p.newPermissionCollection();
        try {
            pc.elements().nextElement();
            fail("expected NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
        Enumeration<Permission> en = pc.elements();
        assertNotNull(en);
        assertFalse(en.hasMoreElements());
        Permission sp1 = new DelegationPermission("\"DDD\" \"BBB\"");
        Permission sp2 = new DelegationPermission("\"CCC\" \"BBB\"");
        pc.add(sp1);
        en = pc.elements();
        assertTrue(en.hasMoreElements());
        assertTrue(sp1.equals(en.nextElement()));
        assertFalse(en.hasMoreElements());
        pc.add(sp2);
        en = pc.elements();
        Collection<Permission> c = new ArrayList<Permission>();
        while (en.hasMoreElements()) {
            c.add(en.nextElement());
        }
        assertFalse(en.hasMoreElements());
        assertEquals(2, c.size());
        assertTrue(c.contains(sp1) && c.contains(sp2));
    }

 }
