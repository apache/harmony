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
import java.util.Hashtable;
import java.util.List;

import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.ReferralException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.SortControl;
import javax.naming.ldap.UnsolicitedNotification;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotificationListener;

import junit.framework.TestCase;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;
import org.apache.harmony.jndi.provider.ldap.mock.BindResponse;
import org.apache.harmony.jndi.provider.ldap.mock.DisconnectResponse;
import org.apache.harmony.jndi.provider.ldap.mock.EncodableLdapResult;
import org.apache.harmony.jndi.provider.ldap.mock.MockLdapMessage;
import org.apache.harmony.jndi.provider.ldap.mock.MockLdapServer;

public class LdapContextServerMockedTest extends TestCase {
    private MockLdapServer server;

    private Hashtable<Object, Object> env = new Hashtable<Object, Object>();

    @Override
    public void setUp() throws Exception {
        server = new MockLdapServer();
        server.start();
        try {
            Class.forName("com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.jndi.ldap.LdapCtxFactory");
        } catch (Exception e) {
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.harmony.jndi.provider.ldap.LdapContextFactory");
        }
        env.put(Context.PROVIDER_URL, server.getURL());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "");
        env.put(Context.SECURITY_CREDENTIALS, "");
    }

    @Override
    public void tearDown() {
        server.stop();
    }

    public void testRequestControls() throws Exception {

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        InitialLdapContext initialContext = new InitialLdapContext(env, null);

        Control[] reqCtls = initialContext.getRequestControls();
        assertEquals(1, reqCtls.length);
        assertEquals("2.16.840.1.113730.3.4.2", reqCtls[0].getID());
        assertEquals(Control.NONCRITICAL, reqCtls[0].isCritical());

        initialContext.setRequestControls(new Control[] { new SortControl("",
                Control.NONCRITICAL) });

        reqCtls = initialContext.getRequestControls();
        assertEquals(2, reqCtls.length);
        Control control = reqCtls[0];
        if (control instanceof SortControl) {
            assertEquals(Control.NONCRITICAL, reqCtls[0].isCritical());
            assertEquals("2.16.840.1.113730.3.4.2", reqCtls[1].getID());
            assertEquals(Control.NONCRITICAL, reqCtls[1].isCritical());
        } else {
            assertEquals("2.16.840.1.113730.3.4.2", control.getID());
            assertEquals(Control.NONCRITICAL, control.isCritical());
            assertTrue(reqCtls[1] instanceof SortControl);
            assertEquals(Control.NONCRITICAL, reqCtls[1].isCritical());
        }

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                new EncodableLdapResult(), null) });

        LdapContext context = (LdapContext) initialContext.lookup("");
        // request controls are not inherited
        reqCtls = context.getRequestControls();
        assertEquals(1, reqCtls.length);
        assertEquals("2.16.840.1.113730.3.4.2", reqCtls[0].getID());
        assertEquals(Control.NONCRITICAL, reqCtls[0].isCritical());

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        context = context.newInstance(new Control[] { new SortControl("",
                Control.NONCRITICAL) });
        reqCtls = context.getRequestControls();

        assertEquals(2, reqCtls.length);
        control = reqCtls[0];
        if (control instanceof SortControl) {
            assertEquals(Control.NONCRITICAL, reqCtls[0].isCritical());
            assertEquals("2.16.840.1.113730.3.4.2", reqCtls[1].getID());
            assertEquals(Control.NONCRITICAL, reqCtls[1].isCritical());
        } else {
            assertEquals("2.16.840.1.113730.3.4.2", control.getID());
            assertEquals(Control.NONCRITICAL, control.isCritical());
            assertTrue(reqCtls[1] instanceof SortControl);
            assertEquals(Control.NONCRITICAL, reqCtls[1].isCritical());
        }
    }

    public void testConnectControls() throws Exception {

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        InitialDirContext initialDirContext = new InitialDirContext(env);

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                new EncodableLdapResult(), null) });
        LdapContext context = (LdapContext) initialDirContext.lookup("");

        assertNull(context.getConnectControls());

        server = new MockLdapServer(server);
        server.start();
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        context.reconnect(new Control[] { new SortControl("",
                Control.NONCRITICAL) });

        Control[] controls = context.getConnectControls();
        assertNotNull(controls);
        assertEquals(1, controls.length);
        Control c = controls[0];
        assertTrue(c instanceof SortControl);
        assertEquals(Control.NONCRITICAL, c.isCritical());

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                new EncodableLdapResult(), null) });
        context = (LdapContext) context.lookup("");

        // connect controls are inherited
        controls = context.getConnectControls();
        assertNotNull(controls);
        assertEquals(1, controls.length);
        c = controls[0];
        assertTrue(c instanceof SortControl);
        assertEquals(Control.NONCRITICAL, c.isCritical());

    }

    public void testConnectControls2() throws Exception {
        // set connect controls by property "java.naming.ldap.control.connect"
        env.put("java.naming.ldap.control.connect",
                new Control[] { new SortControl("", Control.NONCRITICAL) });

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        InitialDirContext initialDirContext = new InitialDirContext(env);

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                new EncodableLdapResult(), null) });
        LdapContext context = (LdapContext) initialDirContext.lookup("");

        Control[] controls = context.getConnectControls();
        assertNotNull(controls);
        assertEquals(1, controls.length);
        Control c = controls[0];
        assertTrue(c instanceof SortControl);
        assertEquals(Control.NONCRITICAL, c.isCritical());
    }

    public void testConnectControls3() throws Exception {
        // set connect controls by InitialLdapContext
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        InitialLdapContext initialDirContext = new InitialLdapContext(env,
                new Control[] { new SortControl("", Control.NONCRITICAL) });

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                new EncodableLdapResult(), null) });
        LdapContext context = (LdapContext) initialDirContext.lookup("");

        Control[] controls = context.getConnectControls();
        assertNotNull(controls);
        assertEquals(1, controls.length);
        Control c = controls[0];
        assertTrue(c instanceof SortControl);
        assertEquals(Control.NONCRITICAL, c.isCritical());

    }

    public void testnewInstance() throws Exception {
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        InitialLdapContext initialDirContext = new InitialLdapContext(env, null);
        Control[] reqCtls = initialDirContext.getRequestControls();
        assertEquals(1, reqCtls.length);
        assertEquals("2.16.840.1.113730.3.4.2", reqCtls[0].getID());
        assertEquals(Control.NONCRITICAL, reqCtls[0].isCritical());

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        LdapContext context = initialDirContext
                .newInstance(new Control[] { new SortControl("",
                        Control.NONCRITICAL) });

        assertNotSame(initialDirContext, context);
        reqCtls = context.getRequestControls();
        assertEquals(2, reqCtls.length);
        Control control = reqCtls[0];
        if (control instanceof SortControl) {
            assertEquals(Control.NONCRITICAL, reqCtls[0].isCritical());
            assertEquals("2.16.840.1.113730.3.4.2", reqCtls[1].getID());
            assertEquals(Control.NONCRITICAL, reqCtls[1].isCritical());
        } else {
            assertEquals("2.16.840.1.113730.3.4.2", control.getID());
            assertEquals(Control.NONCRITICAL, control.isCritical());
            assertTrue(reqCtls[1] instanceof SortControl);
            assertEquals(Control.NONCRITICAL, reqCtls[1].isCritical());
        }

    }

    public void testRerralIgnore() throws Exception {
        env.put(Context.REFERRAL, "ignore");

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        DirContext context = new InitialDirContext(env);

        EncodableLdapResult result = new EncodableLdapResult();
        result = new EncodableLdapResult(LdapResult.REFERRAL, "", "",
                new String[] { "ldap://localhost" });

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE, result, null) });
        try {
            context.getAttributes("cn=test");
            fail("Should throw PartialResultException");
        } catch (PartialResultException e) {
            // expected
        }

        result = new EncodableLdapResult(LdapResult.REFERRAL, null, null, null);

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_DEL_RESPONSE, result, null) });
        try {
            context.destroySubcontext("cn=test");
            fail("Should throw PartialResultException");
        } catch (PartialResultException e) {
            // expected
        }
    }

    public void testReferralThrow() throws Exception {
        env.put(Context.REFERRAL, "throw");
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        DirContext context = new InitialDirContext(env);

        EncodableLdapResult result = new EncodableLdapResult(
                LdapResult.REFERRAL, "", "",
                new String[] { "ldap://localhost" });

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE, result, null) });
        try {
            context.getAttributes("cn=test");
            fail("Should throw ReferralException");
        } catch (ReferralException e) {
            assertEquals("ldap://localhost", e.getReferralInfo());
        }

        result = new EncodableLdapResult(LdapResult.REFERRAL, "", "",
                new String[] { "ldap://localhost" });

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_DEL_RESPONSE, result, null) });

        try {
            context.destroySubcontext("");
            fail("Should throw ReferralException");
        } catch (ReferralException e) {
            assertEquals("ldap://localhost", e.getReferralInfo());
        }
    }

    public void testSearchReferralFollow() throws Exception {
        env.put(Context.REFERRAL, "follow");
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        DirContext context = new InitialDirContext(env);

        final MockLdapServer referralServer = new MockLdapServer();
        referralServer.start();

        ASN1Encodable ref = new ASN1Encodable() {

            public void encodeValues(Object[] values) {
                List<byte[]> list = new ArrayList<byte[]>();
                list.add(Utils.getBytes(referralServer.getURL()));
                values[0] = list;
            }

        };

        server.setResponseSeq(new LdapMessage[] {
                new LdapMessage(LdapASN1Constant.OP_SEARCH_RESULT_REF, ref,
                        null),
                new LdapMessage(LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                        new EncodableLdapResult(), null) });

        referralServer.setResponseSeq(new LdapMessage[] {
                new LdapMessage(LdapASN1Constant.OP_BIND_RESPONSE,
                        new BindResponse(), null),
                new LdapMessage(LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                        new EncodableLdapResult(), null) });

        context.search("cn=test", null);

    }

    public void testReferralFollow() throws Exception {
        env.put(Context.REFERRAL, "follow");
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        DirContext context = new InitialDirContext(env);

        MockLdapServer referralServer = new MockLdapServer();
        referralServer.start();

        EncodableLdapResult result = new EncodableLdapResult(
                LdapResult.REFERRAL, "", "", new String[] { referralServer
                        .getURL() });

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE, result, null) });

        referralServer.setResponseSeq(new LdapMessage[] {
                new LdapMessage(LdapASN1Constant.OP_BIND_RESPONSE,
                        new BindResponse(), null),
                new LdapMessage(LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                        new EncodableLdapResult(), null) });

        context.getAttributes("cn=test");

        referralServer.stop();
    }

    public void testAddToEnvironment() throws Exception {
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        assertNull(env.get(Context.REFERRAL));

        InitialDirContext initialDirContext = new InitialDirContext(env);

        // Context.REFERRAL changed doesn't cause re-bind operation
        initialDirContext.addToEnvironment(Context.REFERRAL, "ignore");

        assertEquals("ignore", initialDirContext.getEnvironment().get(
                Context.REFERRAL));

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_DEL_RESPONSE, new EncodableLdapResult(),
                null) });

        initialDirContext.destroySubcontext("cn=test");

        /*
         * Context.SECURITY_AUTHENTICATION will case re-bind when invoke context
         * methods at first time
         */
        Object preValue = initialDirContext.addToEnvironment(
                Context.SECURITY_AUTHENTICATION, "none");
        assertFalse("none".equals(preValue));

        server.setResponseSeq(new LdapMessage[] {
                new LdapMessage(LdapASN1Constant.OP_BIND_RESPONSE,
                        new BindResponse(), null),
                new LdapMessage(LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                        new EncodableLdapResult(), null) });

        initialDirContext.lookup("");

        preValue = initialDirContext.addToEnvironment(
                Context.SECURITY_AUTHENTICATION, "simple");
        assertFalse("simple".equals(preValue));

        // initialDirContext is shared connection, will create new connection
        server = new MockLdapServer(server);
        server.start();
        server.setResponseSeq(new LdapMessage[] {
                new LdapMessage(LdapASN1Constant.OP_BIND_RESPONSE,
                        new BindResponse(), null),
                new LdapMessage(LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                        new EncodableLdapResult(), null) });

        initialDirContext.lookup("");

    }

    public void testAddToEnvironment_binaryAttributeProp() throws Exception {
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        assertNull(env.get(Context.REFERRAL));

        InitialDirContext initialDirContext = new InitialDirContext(env);

        initialDirContext.addToEnvironment(
                "java.naming.ldap.attributes.binary", "cn");
        assertEquals("cn", initialDirContext.getEnvironment().get(
                "java.naming.ldap.attributes.binary"));

        initialDirContext.addToEnvironment(
                "java.naming.ldap.attributes.binary", "cn ou");
        assertEquals("cn ou", initialDirContext.getEnvironment().get(
                "java.naming.ldap.attributes.binary"));

        initialDirContext.addToEnvironment(
                "java.naming.ldap.attributes.binary", " cn ");
        assertEquals(" cn ", initialDirContext.getEnvironment().get(
                "java.naming.ldap.attributes.binary"));

        initialDirContext.addToEnvironment(
                "java.naming.ldap.attributes.binary", " cn ou ");
        assertEquals(" cn ou ", initialDirContext.getEnvironment().get(
                "java.naming.ldap.attributes.binary"));

        try {
            initialDirContext.addToEnvironment(
                    "java.naming.ldap.attributes.binary", new Object());
            fail("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
    }

    public void testAddToEnvironment_batchSizeProp() throws Exception {
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        assertNull(env.get(Context.REFERRAL));
        InitialDirContext initialDirContext = new InitialDirContext(env);

        initialDirContext.addToEnvironment(Context.BATCHSIZE, "4");
        assertEquals("4", initialDirContext.getEnvironment().get(
                Context.BATCHSIZE));

        try {
            initialDirContext.addToEnvironment(Context.BATCHSIZE, "wrong");
            fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
            // expected
        }
        assertEquals("4", initialDirContext.getEnvironment().get(
                Context.BATCHSIZE));

        try {
            initialDirContext.addToEnvironment(Context.BATCHSIZE, "3.3");
            fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
            // expected
        }
        assertEquals("4", initialDirContext.getEnvironment().get(
                Context.BATCHSIZE));

        try {
            initialDirContext.addToEnvironment(Context.BATCHSIZE, new Object());
            fail("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
    }

    public void testReconnect() throws Exception {
        Control[] expected = new Control[] { new PagedResultsControl(10,
                Control.NONCRITICAL) };
        env.put("java.naming.ldap.control.connect", expected);

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        LdapContext context = new InitialLdapContext(env, null);

        Control[] controls = context.getConnectControls();
        assertNotNull(controls);
        assertNotSame(expected, controls);

        Control c = controls[0];
        assertTrue(c instanceof PagedResultsControl);
        assertEquals(Control.NONCRITICAL, ((PagedResultsControl) c)
                .isCritical());
        assertEquals(expected[0], c);

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        expected = new Control[] { new SortControl("", Control.NONCRITICAL) };
        context.reconnect(expected);

        controls = context.getConnectControls();
        assertNotNull(controls);
        assertEquals(1, controls.length);
        c = controls[0];
        assertTrue(c instanceof SortControl);
        assertEquals(Control.NONCRITICAL, ((SortControl) c).isCritical());
        assertNotSame(expected, controls);
        assertEquals(expected[0], c);

        expected[0] = new PagedResultsControl(10, Control.NONCRITICAL);
        controls = context.getConnectControls();
        assertNotNull(controls);
        assertEquals(1, controls.length);
        c = controls[0];
        assertTrue(c instanceof SortControl);
        assertEquals(Control.NONCRITICAL, ((SortControl) c).isCritical());

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        context.reconnect(null);

        assertNull(context.getConnectControls());
    }

    public void testReconnect_share_connection() throws Exception {

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        LdapContext context = new InitialLdapContext(env, null);

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        // doesn't create new connection
        context.reconnect(null);

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                new EncodableLdapResult(), null) });
        // another and context share the same connection now
        LdapContext another = (LdapContext) context.lookup("");

        MockLdapServer one = new MockLdapServer(server);
        one.start();

        one.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        // create new connection
        context.reconnect(null);

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        // use original connection
        another.reconnect(null);
    }

    /*
     * This test would block on RI, that because of difference of inner
     * implementation between RI and Harmony, It's hard to emulate using
     * MockServer. If run in real environment, the test will pass both on RI and
     * Harmony.
     */
    public void testFederation() throws Exception {
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        LdapContext context = new InitialLdapContext(env, null);

        /*
         * test invalid name 'test'
         */
        try {
            context.getAttributes(new CompositeName("test"));
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // expected
        }

        /*
         * test name '/usr/bin/cn=test'
         */
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                new EncodableLdapResult(), null) });
        try {
            context.lookup("/usr/bin/cn=test");
            fail("Should throw CannotProceedException");
        } catch (CannotProceedException e) {
            assertEquals("/", e.getAltName().toString());
            assertEquals("usr/bin/cn=test", e.getRemainingName().toString());
            assertNull(e.getRemainingNewName());
            assertTrue(e.getResolvedName() instanceof CompositeName);
            assertEquals(1, e.getResolvedName().size());
            assertEquals("/", e.getResolvedName().toString());
            assertTrue(e.getAltNameCtx() instanceof LdapContext);
            assertEquals(context.getNameInNamespace(), e.getAltNameCtx()
                    .getNameInNamespace());
            assertTrue(e.getResolvedObj() instanceof Reference);

            Reference ref = (Reference) e.getResolvedObj();
            assertEquals(Object.class.getName(), ref.getClassName());
            assertNull(ref.getFactoryClassLocation());
            assertNull(ref.getFactoryClassName());

            assertEquals(1, ref.size());
            RefAddr addr = ref.get(0);
            assertTrue(addr.getContent() instanceof LdapContext);
            assertEquals(context.getNameInNamespace(), ((LdapContext) addr
                    .getContent()).getNameInNamespace());
            assertEquals("nns", addr.getType());
        }

        /*
         * test name 'usr/bin/cn=test'
         */
        try {
            context.getAttributes("usr/bin/cn=test");
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // expected
        }

        /*
         * test name '/'
         */
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                new EncodableLdapResult(), null) });
        try {
            Name name = new CompositeName();
            name.add("");
            context.getAttributes(name);
            fail("Should throw CannotProceedException");
        } catch (CannotProceedException e) {
            assertEquals("/", e.getAltName().toString());
            assertTrue(e.getRemainingName() instanceof CompositeName);
            assertEquals(0, e.getRemainingName().size());
            assertEquals("", e.getRemainingName().toString());
            assertNull(e.getRemainingNewName());
            assertTrue(e.getResolvedName() instanceof CompositeName);
            assertEquals(1, e.getResolvedName().size());
            assertEquals("/", e.getResolvedName().toString());
            assertTrue(e.getAltNameCtx() instanceof LdapContext);
            assertEquals(context.getNameInNamespace(), e.getAltNameCtx()
                    .getNameInNamespace());
            assertTrue(e.getResolvedObj() instanceof Reference);

            Reference ref = (Reference) e.getResolvedObj();
            assertEquals(Object.class.getName(), ref.getClassName());
            assertNull(ref.getFactoryClassLocation());
            assertNull(ref.getFactoryClassName());

            assertEquals(1, ref.size());
            RefAddr addr = ref.get(0);
            assertTrue(addr.getContent() instanceof LdapContext);
            assertEquals(context.getNameInNamespace(), ((LdapContext) addr
                    .getContent()).getNameInNamespace());
            assertEquals("nns", addr.getType());
        }
    }

    public void testUnsolicitedNotification() throws Exception {
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        LdapContext context = new InitialLdapContext(env, null);

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_SEARCH_RESULT_DONE,
                new EncodableLdapResult(), null) });
        EventDirContext eventContext = (EventDirContext) context.lookup("");

        assertTrue(eventContext.targetMustExist());

        MockUnsolicitedNotificationListener listener = new MockUnsolicitedNotificationListener();

        MockLdapMessage message = new MockLdapMessage(new LdapMessage(
                LdapASN1Constant.OP_EXTENDED_RESPONSE,
                new DisconnectResponse(), null));
        message.setMessageId(0);
        server.setResponseSeq(new LdapMessage[] { message });

        eventContext.addNamingListener("", "(objectclass=cn)", new Object[0],
                new SearchControls(), listener);
        server.disconnectNotify();
        Thread.sleep(500);
        assertNull(listener.exceptionEvent);
        assertNotNull(listener.unsolicatedEvent);
        assertTrue(listener.unsolicatedEvent.getSource() instanceof LdapContext);
        UnsolicitedNotification notification = listener.unsolicatedEvent
                .getNotification();
        assertNotNull(notification);
        assertEquals(DisconnectResponse.oid, notification.getID());
        assertNull(notification.getControls());
        assertNull(notification.getException());
        assertNull(notification.getReferrals());
        assertNull(notification.getEncodedValue());
    }

    public void testExtendedOperation() throws Exception {
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });
        LdapContext context = new InitialLdapContext(env, null);

        ASN1Encodable encodableResponse = new ASN1Encodable() {

            public void encodeValues(Object[] values) {
                new EncodableLdapResult().encodeValues(values);
                values[4] = Utils.getBytes("It's my id");
                values[5] = new byte[] { 0, 1, 2, 3 };
            }

        };
        server
                .setResponseSeq(new LdapMessage[] { new LdapMessage(
                        LdapASN1Constant.OP_EXTENDED_RESPONSE,
                        encodableResponse, null) });

        ExtendedResponse response = context
                .extendedOperation(new MockExtendedRequest());
        assertTrue(response instanceof MockExtendedResponse);
        assertEquals("It's my id", response.getID());
        assertEquals(4, response.getEncodedValue().length);
        assertEquals(0, response.getEncodedValue()[0]);
        assertEquals(1, response.getEncodedValue()[1]);
        assertEquals(2, response.getEncodedValue()[2]);
        assertEquals(3, response.getEncodedValue()[3]);

        // test exception
        encodableResponse = new ASN1Encodable() {
            public void encodeValues(Object[] values) {
                new EncodableLdapResult().encodeValues(values);
                values[4] = Utils.getBytes("exception");
                values[5] = new byte[] { 0, 1, 2, 3 };
            }

        };
        server
                .setResponseSeq(new LdapMessage[] { new LdapMessage(
                        LdapASN1Constant.OP_EXTENDED_RESPONSE,
                        encodableResponse, null) });

        try {
            context.extendedOperation(new MockExtendedRequest());
            fail("Should throw NamingException");
        } catch (NamingException e) {
            // expected
            assertEquals("exception", e.getMessage());
        }
    }

    public class MockExtendedRequest implements ExtendedRequest {

        public ExtendedResponse createExtendedResponse(String s, byte[] value,
                int offset, int length) throws NamingException {
            if (s.equalsIgnoreCase("exception")) {
                throw new NamingException("exception");
            }
            return new MockExtendedResponse(s, value);
        }

        public byte[] getEncodedValue() {
            return new byte[0];
        }

        public String getID() {
            return getClass().getName();
        }

    }

    public class MockExtendedResponse implements ExtendedResponse {

        private String id;

        private byte[] values;

        public MockExtendedResponse(String id, byte[] values) {
            this.id = id;
            this.values = values;
        }

        public byte[] getEncodedValue() {
            return values;
        }

        public String getID() {
            return id;
        }

    }

    public class MockUnsolicitedNotificationListener implements
            UnsolicitedNotificationListener {

        UnsolicitedNotificationEvent unsolicatedEvent;

        NamingExceptionEvent exceptionEvent;

        public void notificationReceived(UnsolicitedNotificationEvent e) {
            unsolicatedEvent = e;
        }

        public void namingExceptionThrown(
                NamingExceptionEvent namingExceptionEvent) {
            exceptionEvent = namingExceptionEvent;
        }

    }
}
