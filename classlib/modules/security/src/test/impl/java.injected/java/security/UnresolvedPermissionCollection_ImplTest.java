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

import java.util.*;

import junit.framework.TestCase;


/**
 * Tests for <code>UnresolvedPermissionCollection</code> class fields and methods
 * 
 */

public class UnresolvedPermissionCollection_ImplTest extends TestCase {

    /** 
     * Can add any number of UnresolvedPermission instances, but no any other permissions.
     * Cannot add if collection is read-only.
     */
    public void testAdd()
    {
        PermissionCollection pc = new UnresolvedPermissionCollection();
        Permission sp = new SecurityPermission("abc");
        Permission up1 = new UnresolvedPermission("131234", null, null, null);
        Permission up2 = new UnresolvedPermission("KUJKHVKJgyuygjhb", "xcv456", "26r ytf", 
                new java.security.cert.Certificate[0]);
        
        try {
            pc.add(sp);
            fail("Should not add non-UnresolvedPermission");
        }
        catch (IllegalArgumentException ok) {}
        
        pc.add(up1);
        pc.add(up1);
        pc.add(up2);
        
        pc.setReadOnly();
        try {
            pc.add(up1);
            fail("read-only flag is ignored");
        }
        catch (SecurityException ok) {}
    }
    
    /** This collection never implies any permission. */
    public void testImplies()
    {
        Permission ap = new AllPermission();
        Permission up = new UnresolvedPermission("131234", null, null, null);
        PermissionCollection pc = up.newPermissionCollection();
        
        assertFalse(pc.implies(ap));
        assertFalse(pc.implies(up));
        
        //pc.add(up);
        //assertFalse(pc.implies(up));
    }
    
    /**
     * Should return non-null empty enumeration for empty collection.
     * For non-empty collection, should always return enumeration over unique elements.
     */
    public void testElements()
    {
        PermissionCollection pc = new UnresolvedPermissionCollection();
        Permission up1 = new UnresolvedPermission("131234", null, null, null);
        Permission up2 = new UnresolvedPermission("131234", "ui23rjh", null, null);
        Permission up3 = new UnresolvedPermission("KUJKHVKJgyuygjhb", "xcv456", "26r ytf", 
                new java.security.cert.Certificate[0]);
        
        Enumeration en = pc.elements();
        assertNotNull(en);
        assertFalse(en.hasMoreElements());
        
        pc.add(up1);
        en = pc.elements();
        assertTrue(en.hasMoreElements());
        assertTrue(up1.equals(en.nextElement()));
        assertFalse(en.hasMoreElements());
        
        //no check for duplicate elements - this is too implementation specific.
        /*pc.add(up1);
        en = pc.elements();
        assertTrue(en.hasMoreElements());
        assertTrue(up1.equals(en.nextElement()));
        assertFalse(en.hasMoreElements());*/
        
        pc.add(up2);
        pc.add(up3);
        en = pc.elements();
        Collection els = new ArrayList();
        while (en.hasMoreElements())
        {
            els.add(en.nextElement());
        }
        assertEquals(3, els.size());
        assertTrue(els.contains(up1) && els.contains(up2) && els.contains(up3));
    }
    
    /**
     * For null collection passed, should behave correctly:
     * <ul>
     * <li>If nothing resolved, returns null and does not remove elements
     * <li>If some permission resolved, returns proper collection and removes resolved elements
     * </ul>  
     */
    public void testResolveCollection()
    {
        UnresolvedPermissionCollection upc = new UnresolvedPermissionCollection();
        Permission up = new UnresolvedPermission("java.security.AllPermission", "xcv456", "26r ytf", 
                new java.security.cert.Certificate[0]);
        Permission ap = new AllPermission();
        Permission bp = new BasicPermission("sfwertsdg"){};
        
        PermissionCollection resolved = upc.resolveCollection(ap, null);
        assertNull(resolved);
        
        upc.add(up);
        resolved = upc.resolveCollection(bp, null);
        assertNull(resolved);
        assertTrue(up.equals(upc.elements().nextElement()));
        
        resolved = upc.resolveCollection(ap, null);
        assertNotNull(resolved);
        assertTrue(ap.equals(resolved.elements().nextElement()));
        assertFalse("resolved permission should be removed from unresolved collection", upc.elements().hasMoreElements());
    }
    
    /**
     * For real collection passed, should behave correctly:
     * <ul>
     * <li>If nothing resolved, returns the collection and does not remove elements
     * <li>If some permission resolved, returns the collection and removes resolved elements
     * </ul>  
     */
    public void testResolveCollectionReturnedCollection()
    {
        UnresolvedPermissionCollection upc = new UnresolvedPermissionCollection();
        Permission up3 = new UnresolvedPermission("java.security.AllPermission", "xcv456", null, null);
        Permission ap = new AllPermission();
        PermissionCollection apc = new AllPermissionCollection();
        
        PermissionCollection resolved = upc.resolveCollection(ap, apc);
        assertSame("should return the passed collection if it is not null", apc, resolved);
        // retest the same for case of actually resolved permission
        upc.add(up3);
        resolved = upc.resolveCollection(ap, apc);
        assertSame("should return the passed collection if it is not null", apc, resolved);
    }
    
    /**
     * Test for case when some permissions of the expected type were not resolved for some reason,
     * while others were resolved. Returned collection should contain resolved permissions only,
     * and the unresolved collection should retain unresolved ones only.  
     */
    public void testResolveCollectionPartial()
    {
        UnresolvedPermissionCollection upc = new UnresolvedPermissionCollection();
        String name = "ui23rjh";
        Permission up1 = new UnresolvedPermission("java.security.SecurityPermission", null, null, null);
        Permission up2 = new UnresolvedPermission("java.security.SecurityPermission", name, null, null);
        Permission sp = new SecurityPermission(name);
        
        upc.add(up1);
        upc.add(up2);
        PermissionCollection resolved = upc.resolveCollection(new SecurityPermission("34po5ijh"), null);
        assertNotNull(resolved);
        Enumeration els = resolved.elements();
        assertTrue(sp.equals(els.nextElement()));
        assertFalse(els.hasMoreElements());
        els = upc.elements();
        assertTrue("resolved permission should be removed from unresolved collection", 
                up1.equals(els.nextElement()));
        assertFalse(els.hasMoreElements());
    }    
}
