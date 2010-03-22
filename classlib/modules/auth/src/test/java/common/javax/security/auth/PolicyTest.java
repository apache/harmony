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

package javax.security.auth;

import java.io.FilePermission;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Enumeration;
import junit.framework.TestCase;
import org.apache.harmony.auth.tests.support.SecurityChecker;
import org.apache.harmony.auth.tests.support.TestUtils;
import tests.support.resource.Support_Resources;

/**
 * Tests Policy class
 */
@SuppressWarnings("deprecation")
public class PolicyTest extends TestCase {

    /**
     * Tests that setPolicy() is properly secured via SecurityManager.
     */
    public void testSetPolicy() {
        SecurityManager old = System.getSecurityManager();
        Policy oldPolicy = null;
        oldPolicy = Policy.getPolicy();

        try {
            SecurityChecker checker = new SecurityChecker(new AuthPermission(
                    "setPolicy"), true);
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
     * Tests that getPolicy() is properly secured via SecurityManager.
     */
    public void testGetPolicy_CheckPermission() {
        SecurityManager old = System.getSecurityManager();
        Policy oldPolicy = null;
        oldPolicy = Policy.getPolicy();

        try {
            Policy.setPolicy(new TestProvider());
            SecurityChecker checker = new SecurityChecker(new AuthPermission(
                    "getPolicy"), true);
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
        @Override
        public PermissionCollection getPermissions(Subject subject, CodeSource cs) {
            return null;
        }

        @Override
        public void refresh() {
        }
    }

    public static class FakePolicy {
        // This is not policy class
    }
    /**
     * Tests loading of a default provider, both valid and invalid class
     * references.
     */
    public void testGetPolicy_LoadDefaultProvider() {
        Policy oldPolicy = null;
        try {
            oldPolicy = Policy.getPolicy();
        } catch (Throwable ignore) {
        }
        String POLICY_PROVIDER = "auth.policy.provider";
        String oldProvider = Security.getProperty(POLICY_PROVIDER);
        try {
            Security.setProperty(POLICY_PROVIDER, TestProvider.class.getName());
            Policy.setPolicy(null);
            Policy p = Policy.getPolicy();
            assertNotNull(p);
            assertEquals(TestProvider.class.getName(), p.getClass().getName());

            // absent class
            Security.setProperty(POLICY_PROVIDER, "a.b.c.D");
            Policy.setPolicy(null);
            try {
                p = Policy.getPolicy();
                fail("No SecurityException on failed provider");
            } catch (SecurityException ok) {
            }
            
            // not a policy class
            Security.setProperty(POLICY_PROVIDER, FakePolicy.class.getName());
            Policy.setPolicy(null);
            try {
                p = Policy.getPolicy();
                fail("No expected SecurityException");
            } catch (SecurityException ok) {
            }
        } finally {
            TestUtils.setSystemProperty(POLICY_PROVIDER, oldProvider);
            Policy.setPolicy(oldPolicy);
        }
    }

    //
    //
    //
    //
    //

    static String inputFile1 = Support_Resources
            .getAbsoluteResourcePath("auth_policy1.txt");

    static String inputFile2 = Support_Resources
            .getAbsoluteResourcePath("auth_policy2.txt");

    private static final String POLICY_PROP = "java.security.auth.policy";
    
    public void test_GetPermissions() throws Exception {

        PermissionCollection c;
        Permission per;
        Subject subject;
        
        CodeSource source;

        String oldProp = System.getProperty(POLICY_PROP);
        try {
            System.setProperty(POLICY_PROP, inputFile1);

            Policy p = Policy.getPolicy();
            p.refresh();

            //
            // Both parameters are null
            //

            c = p.getPermissions(null, null);
            assertFalse("Read only for empty", c.isReadOnly());
            assertFalse("Elements for empty", c.elements().hasMoreElements());

            //
            // Subject parameter is provided (CodeBase is not important)
            //
            // Principal javax.security.auth.MyPrincipal "duke"
            //

            // no principals at all
            subject = new Subject();
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());

            // different name "kuke" not "duke"
            subject.getPrincipals().add(new MyPrincipal("kuke"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());

            // different class with required principal's name
            subject.getPrincipals().add(new OtherPrincipal("duke"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());

            // subclass with required principal's name
            subject.getPrincipals().add(new FakePrincipal("duke"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());

            // add required principal's name
            subject.getPrincipals().add(new MyPrincipal("duke"));

            Enumeration<Permission> e = p.getPermissions(subject, null).elements();

            per = e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            assertEquals("Permission: ", per, new FilePermission("/home/duke",
                    "read, write"));

            // check: CodeBase is not important
            source = new CodeSource(new URL("http://dummy.xxx"),
                    (Certificate[]) null);
            c = p.getPermissions(subject, source);
            assertTrue("Elements: ", c.elements().hasMoreElements());

            source = new CodeSource(new URL("http://dummy.xxx"),
                    (CodeSigner[]) null);
            c = p.getPermissions(subject, source);
            assertTrue("Elements: ", c.elements().hasMoreElements());

            //
            // Subject and CodeBase parameter are provided
            //
            // Principal javax.security.auth.MyPrincipal "dummy"
            // CodeBase "http://dummy.xxx"
            //
            source = new CodeSource(new URL("http://dummy.xxx"),
                    (Certificate[]) null);
            subject = new Subject();
            subject.getPrincipals().add(new MyPrincipal("dummy"));

            e = p.getPermissions(subject, source).elements();
            per = e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            assertEquals("Permission: ", per, new RuntimePermission(
                    "createClassLoader"));

            // reset subject : no principals at all
            subject = new Subject();
            c = p.getPermissions(subject, source);
            assertFalse("Elements: ", c.elements().hasMoreElements());

            // different name "kuke" not "dummy"
            subject.getPrincipals().add(new MyPrincipal("kuke"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());

            // different class with required principal's name
            subject.getPrincipals().add(new OtherPrincipal("dummy"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());

            //
            // Principal javax.security.auth.MyPrincipal "my"
            // Principal javax.security.auth.OtherPrincipal "other"
            //
            subject = new Subject();
            subject.getPrincipals().add(new MyPrincipal("my"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());

            subject.getPrincipals().add(new OtherPrincipal("other"));
            e = p.getPermissions(subject, null).elements();
            per = e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            assertEquals("Permission: ", per, new AllPermission());

            //
            // Principal javax.security.auth.MyPrincipal "bunny"
            //
            subject = new Subject();
            subject.getPrincipals().add(new MyPrincipal("bunny"));

            e = p.getPermissions(subject, null).elements();

            Permission[] get = new Permission[2];
            get[0] = e.nextElement();
            get[1] = e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());

            Permission[] set = new Permission[2];
            set[0] = new FilePermission("/home/bunny", "read, write");
            set[1] = new RuntimePermission("stopThread");

            if (get[0].equals(set[0])) {
                assertEquals("Permission: ", set[1], get[1]);
            } else {
                assertEquals("Permission: ", set[0], get[1]);
                assertEquals("Permission: ", set[1], get[0]);
            }

        } finally {
            TestUtils.setSystemProperty(POLICY_PROP, oldProp);
        }
    }

    public void test_Refresh() {

        Permission per;
        Subject subject;
        Enumeration<?> e;

        String oldProp = System.getProperty(POLICY_PROP);
        try {
            //
            // first policy file to be read
            //
            System.setProperty(POLICY_PROP, inputFile1);

            Policy p = Policy.getPolicy();
            p.refresh();

            subject = new Subject();
            subject.getPrincipals().add(new MyPrincipal("duke"));

            e = p.getPermissions(subject, null).elements();

            per = (Permission) e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            assertEquals("Permission: ", per, new FilePermission("/home/duke",
                    "read, write"));

            //
            // second policy file to be read
            //
            System.setProperty(POLICY_PROP, inputFile2);

            p.refresh();

            e = p.getPermissions(subject, null).elements();

            per = (Permission) e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            assertEquals("Permission: ", per, new RuntimePermission(
                    "createClassLoader"));
        } finally {
            TestUtils.setSystemProperty(POLICY_PROP, oldProp);
        }
    }
}
