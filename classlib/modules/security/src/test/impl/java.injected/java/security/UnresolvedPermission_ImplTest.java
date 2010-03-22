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

import java.security.cert.Certificate;

import junit.framework.TestCase;

import org.apache.harmony.security.tests.support.cert.MyCertificate;

/**
 * Tests for <code>UnresolvedPermission</code> class fields and methods
 * 
 */

public class UnresolvedPermission_ImplTest extends TestCase {  
    
    /**
     * This test is valid since 1.5 release only. Checks that UnresolvedPermission returns the proper 
     * data for target permission. For non-empty certificates array, 
     * returns a new array each time this method is called.
     */
    public void testTargetData()
    {
        String type = "laskjhlsdk 2345346";
        String name = "^%#UHVKU^%V  887y";
        String action = "JHB ^%(*&T klj3h4";
        UnresolvedPermission up = new UnresolvedPermission(type, name, action, null);
        assertEquals(type, up.getUnresolvedType());
        assertEquals(name, up.getUnresolvedName()); 
        assertEquals(action, up.getUnresolvedActions()); 
        assertNull(up.getUnresolvedCerts());
    }
    
    public void testEquals()
    {
        String type = "KJHGUiy 24y";
        String name = "kjhsdkfj ";
        String action = "T klj3h4";
        UnresolvedPermission up = new UnresolvedPermission(type, name, action, null);
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action, null);
        assertFalse(up.equals(null));
        assertFalse(up.equals(new Object()));
        assertFalse(up.equals(new BasicPermission("df"){}));
        assertTrue(up.equals(up));
        assertTrue(up.equals(up2));
        assertTrue(up.hashCode() == up2.hashCode());
        up2 = new UnresolvedPermission(type, name, action, new java.security.cert.Certificate[0]);
        assertTrue(up.hashCode() == up2.hashCode());
        up2 = new UnresolvedPermission(type, name, action, new java.security.cert.Certificate[2]);
        //case of trivial collections {null} 
        up = new UnresolvedPermission(type, name, action, new java.security.cert.Certificate[10]);
        assertTrue(up.hashCode() == up2.hashCode());
    }

    /**
     * resolve the unresolved permission to the permission of specified class.
     */
    public void testResolve()
    {
        String name = "abc";
        UnresolvedPermission up = new UnresolvedPermission("java.security.SecurityPermission", name, null, null);
        Permission expected = new SecurityPermission(name);
        //test valid input
        assertEquals(expected, up.resolve(SecurityPermission.class));
        
        //test invalid class
        assertNull(up.resolve(Object.class));
        
        //test invalid signers
        //up = new UnresolvedPermission("java.security.SecurityPermission", name, null, new java.security.cert.Certificate[1]);
        //assertNull(up.resolve(SecurityPermission.class));
        
        //another valid case
        up = new UnresolvedPermission("java.security.AllPermission", null, null, new java.security.cert.Certificate[0]);
        assertEquals(new AllPermission(name, ""), up.resolve(AllPermission.class));
    }
    
    public static final String type = "java.util.PropertyPermission";

    public static final String name = "os.name";

    public static final String action = "write,read";

    public static final byte[] testEncoding1 = new byte[] { (byte) 1 };

    public static final byte[] testEncoding2 = new byte[] { (byte) 2 };

    public static final byte[] testEncoding3 = new byte[] { (byte) 1, (byte) 2,
            (byte) 3 };

    public static final Certificate cert1 = new MyCertificate("TEST_TYPE1",
            testEncoding1);

    public static final Certificate cert2 = new MyCertificate("TEST_TYPE2",
            testEncoding2);

    public static final Certificate cert3 = new MyCertificate("TEST_TYPE3",
            testEncoding3);

    public void test_Constructor() {
        UnresolvedPermission up = new UnresolvedPermission(type, name, action,
                null);
        assertNull(up.getUnresolvedCerts());

        up = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[0]);
        assertEquals(0, up.getUnresolvedCerts().length);

        up = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[2]);
        assertEquals(2, up.getUnresolvedCerts().length);
    }

    public void test_Equals_Scenario0() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                null);
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                null);
        assertEquals(up1, up2);
    }

    public void test_Equals_Scenario1() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                null);
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[0]);
        assertFalse(up1.equals(up2));
    }

    public void test_Equals_Scenario2() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                null);
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[2]);
        assertFalse(up1.equals(up2));
    }

    public void test_Equals_Scenario3() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[0]);
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[0]);
        assertEquals(up1, up2);
    }

    public void test_Equals_Scenario4() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[0]);
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[2]);
        assertFalse(up1.equals(up2));
    }

    public void test_Equals_Scenario5() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[2]);
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[5]);
        assertFalse(up1.equals(up2));
    }

    public void test_Equals_Scenario6() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert1 });
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert1 });
        assertEquals(up1, up2);
    }

    public void test_Equals_Scenario7() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert1, cert2 });
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert1, cert2 });
        assertEquals(up1, up2);
    }

    public void test_Equals_Scenario8() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert1, cert2 });
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert2, cert1 });
        assertEquals(up1, up2);
    }

    public void test_Equals_Scenario9() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert1, cert2, cert3 });
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert2, cert1, cert3 });
        assertEquals(up1, up2);
    }
    
    public void test_Equals_Scenario10() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[2]);
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[2]);
        // Non-bug difference, RI throw NPE here.
        assertEquals(up1, up2);
    }

    public void test_Equals_Scenario11() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert1, cert2 });
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert1, null, cert2 });
        assertFalse(up1.equals(up2));
    }
    
    public void test_Equals_Scenario12() {
        UnresolvedPermission up1 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert1, null, null });
        UnresolvedPermission up2 = new UnresolvedPermission(type, name, action,
                new java.security.cert.Certificate[] { cert1, null, cert1 });
        assertEquals(up1, up2);
    }
}
