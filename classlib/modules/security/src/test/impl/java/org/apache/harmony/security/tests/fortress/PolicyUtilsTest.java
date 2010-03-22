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

import java.io.File;
import java.net.URL;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Security;
import java.security.SecurityPermission;
import java.security.UnresolvedPermission;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;

import org.apache.harmony.security.fortress.PolicyUtils;
import junit.framework.TestCase;


/**
 * Tests for <code>PolicyUtils</code> class fields and methods.
 * 
 */

public class PolicyUtilsTest extends TestCase {

    /** Tests valid expansion of ${key} entries. */
    public void testExpand() throws Exception {
        String[] input = new String[] { "${key.1}", "abcd${key.1}",
                "a ${key.1} b ${$key$}${key.2}", "$key.1", "${}" };
        String[] output = new String[] { "value1", "abcdvalue1",
                "a value1 b ${${${${${${value.2", "$key.1", "" };
        Properties props = new Properties();
        props.put("key.1", "value1");
        props.put("key.2", "value.2");
        props.put("$key$", "${${${${${${");
        props.put("", "");
        for (int i = 0; i < output.length; i++) {
            assertEquals(output[i], PolicyUtils.expand(input[i], props));
        }
    }

    /** Tests ExpansionFailedException for missing keys of ${key} entries. */
    public void testExpandFailed() throws Exception {
        try {
            PolicyUtils.expand("${key.123}", new Properties());
            fail("Should throw ExpansionFailedException");
        }
        catch (PolicyUtils.ExpansionFailedException ok) {}
    }

    /** Tests valid URL-specific expansion. */
    public void testExpandURL() throws Exception {
        String input = "file:/${my.home}" + File.separator + "lib/extensions/";
        Properties props = new Properties();
        String q = File.separator + "drl" + File.separator + "tools1.2";
        props.put("my.home", q);
        assertEquals("file://drl/tools1.2/lib/extensions/", PolicyUtils.expandURL(input,
                props));
    }

    /** Tests valid expansion of ${{protocol:data}} entries. */
    public void testExpandGeneral() throws Exception {
        String[] input = new String[] { "${{a:b}}", "a ${{self}}${{a: made}}",
                "${{}}" };
        String[] output = new String[] { "b", "a this made", "" };
        PolicyUtils.GeneralExpansionHandler handler = new PolicyUtils.GeneralExpansionHandler() {

            public String resolve(String protocol, String data) {
                if ("a".equals(protocol)) {
                    return data;
                }
                if ("self".equals(protocol)) {
                    return "this";
                }
                if ("".equals(protocol)) {
                    return protocol;
                }
                return null;
            }
        };
        for (int i = 0; i < output.length; i++) {
            assertEquals(output[i], PolicyUtils
                    .expandGeneral(input[i], handler));
        }
    }

    /** 
     * Tests ExpansionFailedException for undefined protocol 
     * of ${{protocol:data}} entries. 
     */
    public void testExpandGeneralFailed() throws Exception {
        try {
            PolicyUtils.expandGeneral("${{c}}",
                    new PolicyUtils.GeneralExpansionHandler() {

                        public String resolve(String protocol, String data)
                                throws PolicyUtils.ExpansionFailedException {
                            throw new PolicyUtils.ExpansionFailedException("");
                        }
                    });
            fail("Should throw ExpansionFailedException");
        }
        catch (PolicyUtils.ExpansionFailedException ok) {}
    }

    /** 
     * Tests positive/negative/invalid/missing values of 
     * &quot;policy.expandProperties&quot; security property.
     */
    public void testCanExpandProperties() {
        final String key = "policy.expandProperties";
        String OLD = Security.getProperty(key);
        try {
            Security.setProperty(key, "true");
            assertTrue(PolicyUtils.canExpandProperties());
            Security.setProperty(key, "false");
            assertFalse(PolicyUtils.canExpandProperties());
            Security.setProperty(key, "");
            assertTrue(PolicyUtils.canExpandProperties());
            Security.setProperty(key, "laejhg");
            assertTrue(PolicyUtils.canExpandProperties());
        }
        finally {
            Security.setProperty(key, OLD);
        }
    }

    /**
     * Tests cases of enabled/disabled system URL.
     */
    public void testGetPolicyURLs01() throws Throwable {
        final String KEY_DYNAMIC = "policy.allowSystemProperty";
        String OLD_DYNAMIC = Security.getProperty(KEY_DYNAMIC);

        final String KEY = "dsfvdf";
        Properties arg = new Properties();
        arg.put(KEY, "http://foo.bar.com");
        try {
            Security.setProperty(KEY_DYNAMIC, "true");
            URL[] result = PolicyUtils.getPolicyURLs(arg, KEY, "");
            assertNotNull(result);
            assertEquals(1, result.length);
            assertEquals(new URL("http://foo.bar.com"), result[0]);

            Security.setProperty(KEY_DYNAMIC, "false");
            result = PolicyUtils.getPolicyURLs(arg, KEY, "");
            assertNotNull(result);
            assertEquals(0, result.length);

            Security.setProperty(KEY_DYNAMIC, "");
            result = PolicyUtils.getPolicyURLs(arg, KEY, "");
            assertNotNull(result);
            assertEquals(1, result.length);
            assertEquals(new URL("http://foo.bar.com"), result[0]);
        }
        finally {
            Security.setProperty(KEY_DYNAMIC, OLD_DYNAMIC);
        }
    }

    /** Tests finding algorithm for numbered locations in security properties. */
    public void testGetPolicyURLs02() throws Throwable {
        final String PREFIX = "testGetPolicyURLs02.";
        String[] OLD = new String[5];
        for (int i = 0; i < OLD.length; i++) {
            OLD[i] = Security.getProperty(PREFIX + i);
        }

        try {
            Security.setProperty(PREFIX + 0, "http://foo0.bar.com");
            Security.setProperty(PREFIX + 1, "http://foo1.bar.com");
            Security.setProperty(PREFIX + 2, "http://foo2.bar.com");
            Security.setProperty(PREFIX + 4, "http://foo4.bar.com");
            URL[] result = PolicyUtils.getPolicyURLs(new Properties(), "",
                    PREFIX);
            assertNotNull(result);
            assertEquals(2, result.length);
            assertEquals(new URL("http://foo1.bar.com"), result[0]);
            assertEquals(new URL("http://foo2.bar.com"), result[1]);

            Security.setProperty(PREFIX + 1, "slkjdfhk/svfv*&^");
            Security.setProperty(PREFIX + 3, "dlkfjvb3lk5jt");
            result = PolicyUtils.getPolicyURLs(new Properties(), "", PREFIX);
            assertNotNull(result);
            assertEquals(2, result.length);
            assertEquals(new URL("http://foo2.bar.com"), result[0]);
            assertEquals(new URL("http://foo4.bar.com"), result[1]);
        }
        finally {
            for (int i = 0; i < OLD.length; i++) {
                Security
                        .setProperty(PREFIX + i, (OLD[i] == null) ? "" : OLD[i]);
            }
        }
    }

    /**
     * Tests expansion in system and security URLs.
     */
    public void testGetPolicyURLs03() throws Throwable {
        final String KEY_DYNAMIC = "policy.allowSystemProperty";
        final String OLD_DYNAMIC = Security.getProperty(KEY_DYNAMIC);
        final String KEY_EXP = "policy.expandProperties";
        final String OLD_EXP = Security.getProperty(KEY_EXP);
        final String PREFIX = "testGetPolicyURLs03.";
        String[] OLD = new String[5];
        for (int i = 0; i < OLD.length; i++) {
            OLD[i] = Security.getProperty(PREFIX + i);
        }

        final String KEY = "dsfvdf";
        Properties arg = new Properties();
        arg.put(KEY, "file://${foo.path}/${foo.name}");
        arg.put("foo.path", "path");
        arg.put("foo.name", "name");
        arg.put("foo", "acme");
        Security.setProperty(KEY_DYNAMIC, "true");
        Security.setProperty(KEY_EXP, "true");
        Security.setProperty(PREFIX + 1, "http://foo0.${foo}.org");
        Security.setProperty(PREFIX + 2, "http://${bar}.com");
        Security.setProperty(PREFIX + 3,
                "http://foo2.bar.com/${foo.path}/${foo.name}");
        try {

            URL[] result = PolicyUtils.getPolicyURLs(arg, KEY, PREFIX);
            assertNotNull(result);
            assertEquals(3, result.length);
            assertEquals(new URL("http://foo0.acme.org"), result[0]);
            assertEquals(new URL("http://foo2.bar.com/path/name"), result[1]);
            assertEquals(new URL("file://path/name"), result[2]);

            //expansion here cannot be switched off
            Security.setProperty(KEY_EXP, "false");
            result = PolicyUtils.getPolicyURLs(arg, KEY, PREFIX);
            assertNotNull(result);
            assertEquals(3, result.length);
            assertEquals(new URL("http://foo0.acme.org"), result[0]);
            assertEquals(new URL("http://foo2.bar.com/path/name"), result[1]);
            assertEquals(new URL("file://path/name"), result[2]);
        }
        finally {
            Security.setProperty(KEY_DYNAMIC, OLD_DYNAMIC);
            Security.setProperty(KEY_EXP, OLD_EXP);
            for (int i = 0; i < OLD.length; i++) {
                Security
                        .setProperty(PREFIX + i, (OLD[i] == null) ? "" : OLD[i]);
            }
        }
    }

    /** Tests conversion of null, empty and non-empty heterogeneous collections. */
    public void testToPermissionCollection() {
        Permission p1 = new SecurityPermission("abc");
        Permission p2 = new AllPermission();
        Collection c1 = Arrays.asList(new Permission[] { p1, p2, });

        PermissionCollection pc = PolicyUtils.toPermissionCollection(null);
        assertNotNull(pc);
        assertFalse(pc.elements().hasMoreElements());

        pc = PolicyUtils.toPermissionCollection(new HashSet());
        assertNotNull(pc);
        assertFalse(pc.elements().hasMoreElements());

        pc = PolicyUtils.toPermissionCollection(c1);
        assertNotNull(pc);
        Enumeration en = pc.elements();
        Collection c2 = new HashSet();
        c2.add(en.nextElement());
        c2.add(en.nextElement());
        assertFalse(en.hasMoreElements());
        assertTrue(c2.contains(p1));
        assertTrue(c2.contains(p2));
    }
    
    public void testInstantiatePermission() throws Throwable {
        String name = "abc";
        Permission expected = new SecurityPermission(name);
        //test valid input
        assertEquals(expected, PolicyUtils.instantiatePermission(SecurityPermission.class, name, null));
        assertEquals(expected, PolicyUtils.instantiatePermission(SecurityPermission.class, name, "4t46"));
        
        //test invalid class
        try {
            PolicyUtils.instantiatePermission(UnresolvedPermission.class, null, null);
            fail("IllegalArgumentException expected on invalid class argument");
        } catch (IllegalArgumentException ok) {}        
    }

    /** 
     * Tests various combinations of arrays:
     * null/empty/containing null/containing real objects. 
     */
    public void testMatchSubset() {
        assertTrue(PolicyUtils.matchSubset(null, null));
        assertTrue(PolicyUtils.matchSubset(new Object[] {}, null));
        assertTrue(PolicyUtils.matchSubset(new Object[] { null }, null));
        assertTrue(PolicyUtils.matchSubset(new Object[] {},
                new Object[] { null }));
        assertTrue(PolicyUtils.matchSubset(new Object[] { "1", "2" },
                new Object[] { "3", "2", "1" }));
        assertTrue(PolicyUtils.matchSubset(new Object[] { "1", null },
                new Object[] { "3", "2", "1" }));
        assertFalse(PolicyUtils.matchSubset(new Object[] { "1", null },
                new Object[] { "3", "2", }));
    }
}
