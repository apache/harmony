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
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.security.auth.kerberos.DelegationPermission;
import javax.security.auth.kerberos.ServicePermission;

import junit.framework.TestCase;

/** 
 * Tests ServicePermission class implementation.
 */
public class ServicePermissionTest extends TestCase {

    
    /**
     * @tests javax.security.auth.kerberos.ServicePermission#ServicePermission(
     *        java.lang.String,java.lang.String)
     */
    public void testCtor() {
        ServicePermission sp = new ServicePermission("krbtgt/AAA.COM@BBB.COM", "initiate");
        ServicePermission sp1 = new ServicePermission("host/AAA.COM@BBB.COM", "accept");
        assertEquals("krbtgt/AAA.COM@BBB.COM",sp.getName());
        assertEquals("initiate",sp.getActions());
        assertEquals("host/AAA.COM@BBB.COM",sp1.getName());
        assertEquals("accept",sp1.getActions());
        ServicePermission sp2 = new ServicePermission("host/AAA.COM@BBB.COM", "accept, initiate");
        assertEquals("initiate,accept", sp2.getActions());

        try {
            // Regression for HARMONY-769
            // checks exception order: action parameter is verified first
            new ServicePermission(null, "initiate accept");
            fail("No expected IllegalArgumentException"); 
        } catch(IllegalArgumentException e){
        }
    }
    
    public void testFailedCtor() {
        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", "read");
        	fail("incorrect actions"); 
        } catch(IllegalArgumentException e){
        }

        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", "");
        	fail("actions is empty"); 
        } catch(IllegalArgumentException e){
        }

        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", null);
        	fail("actions is null");
        } catch(NullPointerException e){
        } catch(IllegalArgumentException e){}

        try {
            new ServicePermission(null, "accept");
        	fail("permission is null");
        } catch(NullPointerException e){
        }
        try {
            new ServicePermission("", "accept");
        	//TODO: fail("No expected IAE");  // 
        } catch(IllegalArgumentException e){}
        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", "accept, read");
        	fail("Incorrect actions"); 
        } catch(IllegalArgumentException e){
        }
        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", "initiate, read");
        	fail("Incorrect actions"); 
        } catch(IllegalArgumentException e){
        }
        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", "read, initiate ");
        	fail("Incorrect actions"); 
        } catch(Exception e){
        }
        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", "read, accept ");
        	fail("Incorrect actions"); 
        }catch(IllegalArgumentException e){
        }
        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", ", accept ");
        	//TODO: fail("No expected IAE"); 
        } catch(IllegalArgumentException e){
        }
        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", "initiate, accept, read");
        	fail("Incorrect actions"); 
        } catch(IllegalArgumentException e){
        }
        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", "initiate, read, accept");
        	fail("Incorrect actions"); 
        } catch(IllegalArgumentException e){
        }
        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", "initiate, accept, accept");
        	//TODO: fail("Incorrect actions"); 
        } catch(IllegalArgumentException e){
        }
        try {
            new ServicePermission("krbtgt/AAA.COM@BBB.COM", "initiate accept");
        	fail("Incorrect actions"); 
        } catch(IllegalArgumentException e){
        }
    }
    
    public void testEquals() {
        ServicePermission sp = new ServicePermission("host/AAA.COM@BBB.COM", "accept");
        ServicePermission sp1 = new ServicePermission("host/AAA.COM@BBB.COM", "initiate");
        ServicePermission sp2 = new ServicePermission("host/AAA.COM@BBB.COM", "initiate, accept");
        assertTrue(sp.equals(sp));
        assertTrue(sp.hashCode() == sp.hashCode());
        assertFalse(sp.equals(sp1));
        assertFalse(sp.hashCode() == sp1.hashCode());
        assertFalse(sp.equals(sp2));
        assertFalse(sp1.equals(sp2));
        assertTrue(sp2.equals(sp2));
        assertFalse(sp.equals(new DelegationPermission("\"AAA\" \"BBB\"", "action")));
        assertFalse(sp.equals(null));
    }
    
    public void testImplies() {
        ServicePermission sp1;
        ServicePermission sp = new ServicePermission("host/AAA.COM@BBB.COM", "accept");
        sp1 = new ServicePermission("*", "initiate, accept");
        assertTrue(sp.implies(sp));
        assertFalse(sp.implies(sp1));
        assertTrue(sp1.implies(sp));
        assertTrue(sp1.implies(sp1));
        sp1 = new ServicePermission("*", "accept");
        assertTrue(sp1.implies(sp));
        sp1 = new ServicePermission("*", "initiate");
        assertFalse(sp1.implies(sp));
        assertFalse(sp1.implies(new ServicePermission("*", "accept, initiate")));
        assertTrue(new ServicePermission("host/AAA.COM@BBB.COM", "initiate, accept").implies(sp));
        assertTrue(new ServicePermission("host/AAA.COM@BBB.COM", "accept").implies(sp));
        assertFalse(new ServicePermission("host/AAA.COM@BBB.COM", "initiate").implies(sp));
        assertFalse(sp1.implies(null));
    }
    
    // tests for KrbServicePermissionCollection
    
    public void testAddCollection() {
        ServicePermission sp = new ServicePermission("AAA", "accept");
        PermissionCollection pc  = sp.newPermissionCollection();
        
        try {
            pc.add(new DelegationPermission("\"aaa\" \"bbb\""));
            fail("Should not add non DelegationPermission");
        } catch (IllegalArgumentException e) {
        }
        
        try {
            pc.add(null);
            fail("no expected IAE");
        } catch (IllegalArgumentException e) {
        }
        
        pc.add(new ServicePermission("AAA", "accept"));
        pc.add(new ServicePermission("BBB", "accept, initiate"));
        
        pc.setReadOnly();
        try {
            pc.add(sp);
            fail("read-only flag is ignored");
        } catch (SecurityException e) {
        }
    }
    
    public void testImpliesCollection(){
        
        Permission ap = new AllPermission();
        Permission p = new ServicePermission("AAA", "accept");
        PermissionCollection pc = p.newPermissionCollection();
        assertFalse(pc.implies(ap));
        assertFalse(pc.implies(p));
        pc.add(p);
        assertTrue(pc.implies(p));
        assertFalse(pc.implies(null));
        assertFalse(pc.implies(new ServicePermission("BBB", "initiate")));
        assertFalse(pc.implies(new ServicePermission("CCC", "accept")));
        pc.add(new ServicePermission("*", "accept, initiate"));
        assertTrue(pc.implies(new ServicePermission("*", "accept")));
        assertTrue(pc.implies(new ServicePermission("*", "initiate")));
        assertTrue(pc.implies(new ServicePermission("BBB", "initiate")));
        assertTrue(pc.implies(new ServicePermission("CCC", "accept")));

        
    }
    
    public void testElements() {
        Permission p = new ServicePermission("AAA", "accept");
        PermissionCollection pc = p.newPermissionCollection();
        
		try {
			pc.elements().nextElement();
			fail("expected NoSuchElementException");
		} catch (NoSuchElementException e) {
		}

        Enumeration<Permission> en = pc.elements();
        assertNotNull(en);
        assertFalse(en.hasMoreElements());
        
        Permission sp1 = new ServicePermission("BBB", "accept, initiate");
        Permission sp2 = new ServicePermission("CCC", "initiate");
        Permission sp3 = new ServicePermission("DDD", "accept");
        
        pc.add(sp1);
        en = pc.elements();
        assertTrue(en.hasMoreElements());
        assertTrue(sp1.equals(en.nextElement()));
        assertFalse(en.hasMoreElements());
        pc.add(sp2);
        pc.add(sp3);
        en = pc.elements();
        Collection<Permission> c = new ArrayList<Permission>();
        while (en.hasMoreElements())
        {
            c.add(en.nextElement());
        }
        assertFalse(en.hasMoreElements());
        assertEquals(3, c.size());
        assertTrue(c.contains(sp1) && c.contains(sp2) && c.contains(sp3));
    }
    
    public void testActions() {

        String[] validActions = new String[] { " accept ", // spaces 
                "accept,ACCEPT,accept", 
                "initiate,INITIATE,initiate", 
                "\naccept,accept,accept\n", // leading & trailing \n 
                "\naccept,accept,accept\n", // leading & trailing \n 
                "\naccept,initiate,accept\n", // leading & trailing \n 
                "\ninitiate\n,\raccept,initiate\n", // leading & trailing \n 
                "\naccept\n", // leading & trailing \n 
                "\naccept\n", // leading & trailing \n 
                "\taccept\t", // leading & trailing \t 
                "\taccept\t", // leading & trailing \r
                "accept , initiate", // spaces
                "accept\n,\ninitiate", // \n 
                "accept\t,\tinitiate", // \t 
                "accept\r,\rinitiate", // \r 
                "AccepT", // first & last upper case
                "InitiatE", //  first & last upper case
                "Accept, initiatE" //  first & last upper case
        };

        for (String element : validActions) {
            new ServicePermission("*", element);
        }

        String[] invalidActions = new String[] { "accept initiate", // space
                "accept\ninitiate", // delimiter \n 
                "accept\tinitiate", // delimiter \t 
                "accept\tinitiate", // delimiter \r
                "accept, ", // ','
                "accept,", // ','
                " ,accept" // ','
        };
        for (String element : invalidActions) {
            try {
                new ServicePermission("*", element);
                fail("No expected IllegalArgumentException for action: "
                        + element);
            } catch (IllegalArgumentException e) {
            }
        }
    }
}
