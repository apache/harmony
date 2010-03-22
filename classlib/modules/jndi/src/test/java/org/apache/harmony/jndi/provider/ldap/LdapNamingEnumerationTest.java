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

package org.apache.harmony.jndi.provider.ldap;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import javax.naming.NamingException;

import junit.framework.TestCase;

public class LdapNamingEnumerationTest extends TestCase {
    private LdapNamingEnumeration<Object> enu;

    public void test_constructor() {
        enu = new LdapNamingEnumeration<Object>(null, null);
    }

    public void test_Enumeration() throws Exception {
        ArrayList<Object> list = new ArrayList<Object>();
        enu = new LdapNamingEnumeration<Object>(list, null);
        assertFalse(enu.hasMore());

        list = new ArrayList<Object>();
        list.add(new Object());
        list.add(new Object());

        enu = new LdapNamingEnumeration<Object>(list, null);
        assertTrue(enu.hasMore());
        assertNotNull(enu.next());
        assertTrue(enu.hasMore());
        assertNotNull(enu.next());
        assertFalse(enu.hasMore());
        try {
            enu.next();
            fail("Should throws NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }

        NamingException ex = new NamingException();
        enu = new LdapNamingEnumeration<Object>(list, ex);
        assertTrue(enu.hasMore());
        assertNotNull(enu.next());
        assertTrue(enu.hasMore());
        assertNotNull(enu.next());
        try {
            enu.hasMore();
            fail("Should throws NamingException");
        } catch (NamingException e) {
            // expected
            assertEquals(ex, e);
        }
        try {
            enu.next();
            fail("Should throws NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    /**
     * uses hasMoreElements() and nextElement() to iterate
     * 
     * @throws Exception
     */
    public void test_Enumeration_01() throws Exception {
        ArrayList<Object> list = new ArrayList<Object>();
        enu = new LdapNamingEnumeration<Object>(list, null);
        assertFalse(enu.hasMoreElements());
        try {
            enu.nextElement();
            fail("Should throws NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected;
        }

        list = new ArrayList<Object>();
        list.add(new Object());
        list.add(new Object());

        enu = new LdapNamingEnumeration<Object>(list, null);
        assertTrue(enu.hasMoreElements());
        assertNotNull(enu.nextElement());
        assertTrue(enu.hasMoreElements());
        assertNotNull(enu.nextElement());
        assertFalse(enu.hasMoreElements());
        try {
            enu.nextElement();
            fail("Should throws NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }

        NamingException ex = new NamingException();
        enu = new LdapNamingEnumeration<Object>(list, ex);
        assertTrue(enu.hasMoreElements());
        assertNotNull(enu.nextElement());
        assertTrue(enu.hasMoreElements());
        assertNotNull(enu.nextElement());
        assertFalse(enu.hasMoreElements());
        try {
            enu.nextElement();
            fail("Should throws NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    public void test_close() throws Exception {
        ArrayList<Object> list = new ArrayList<Object>();
        list.add(new Object());
        list.add(new Object());
        enu = new LdapNamingEnumeration<Object>(list, null);
        assertTrue(enu.hasMore());
        enu.close();
        assertFalse(enu.hasMore());
        assertFalse(enu.hasMoreElements());
        
        try {
            enu.next();
            fail("Should throws NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }

        try {
            enu.nextElement();
            fail("Should throws NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
    }
}
