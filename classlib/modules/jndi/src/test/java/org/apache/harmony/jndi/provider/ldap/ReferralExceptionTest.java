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

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.ldap.Control;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.SortControl;

import junit.framework.TestCase;

import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.mock.BindResponse;
import org.apache.harmony.jndi.provider.ldap.mock.MockLdapServer;

public class ReferralExceptionTest extends TestCase {
	private MockLdapServer server;

	public void setUp() throws Exception {
		server = new MockLdapServer();
		server.start();
	}

	public void tearDown() {
		server.stop();
	}

	public void testGetReferralInfo() throws Exception {
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		env.put(Context.REFERRAL, "throw");
		String[] referrals = new String[] { "ldap://localhost:389",
				"ldap://localhost:333" };
		ReferralExceptionImpl ex = new ReferralExceptionImpl("cn=dn",
				referrals, env);
		// try {
		// context.search("", null);
		// } catch (ReferralException e) {
		// ex = e;
		// }

		assertEquals(referrals[0], ex.getReferralInfo());

		assertTrue(ex.skipReferral());

		assertEquals(referrals[1], ex.getReferralInfo());

		assertFalse(ex.skipReferral());

		assertNull(ex.getReferralInfo());
	}

	public void testGetReferralContext() throws Exception {
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();

		env.put(Context.REFERRAL, "throw");
		env.put("test.getReferralContext", "GetReferralContext");

		String[] referrals = new String[] { server.getURL() };
		ReferralExceptionImpl ex = new ReferralExceptionImpl("cn=dn",
				referrals, env);

		assertEquals(referrals[0], ex.getReferralInfo());

		server.setResponseSeq(new LdapMessage[] { new LdapMessage(
				LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

		Context refContext = ex.getReferralContext();

		Hashtable<Object, Object> refEnv = (Hashtable<Object, Object>) refContext
				.getEnvironment();

		assertEquals(env.get(Context.REFERRAL), refEnv.get(Context.REFERRAL));
		assertEquals(env.get("test.getReferralContext"), refEnv
				.get("test.getReferralContext"));

		assertFalse(ex.skipReferral());

		assertNull(ex.getReferralInfo());
	}

	public void testGetReferralContext2() throws Exception {
		Hashtable<Object, Object> initialEnv = new Hashtable<Object, Object>();

		initialEnv.put(Context.REFERRAL, "throw");
		initialEnv.put("test.getReferralContext", "GetReferralContext");

		String[] referrals = new String[] { server.getURL() };
		ReferralExceptionImpl ex = new ReferralExceptionImpl("cn=dn",
				referrals, initialEnv);

		Hashtable<Object, Object> env = new Hashtable<Object, Object>();

		env.put(Context.REFERRAL, "follow");
		env.put("test.getReferralContext", "changed");

		server.setResponseSeq(new LdapMessage[] { new LdapMessage(
				LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

		assertEquals(referrals[0], ex.getReferralInfo());
		Context refContext = ex.getReferralContext(env);

		Hashtable<Object, Object> refEnv = (Hashtable<Object, Object>) refContext
				.getEnvironment();

		assertEquals(env.get(Context.REFERRAL), refEnv.get(Context.REFERRAL));
		assertEquals(env.get("test.getReferralContext"), refEnv
				.get("test.getReferralContext"));

		assertFalse(ex.skipReferral());

		assertNull(ex.getReferralInfo());
	}

    public void testGetReferralContext3() throws Exception {
        Hashtable<Object, Object> initialEnv = new Hashtable<Object, Object>();

        initialEnv.put(Context.REFERRAL, "throw");
        initialEnv.put("test.getReferralContext", "GetReferralContext");

        String[] referrals = new String[] { server.getURL() };
        ReferralExceptionImpl ex = new ReferralExceptionImpl("cn=dn",
                referrals, initialEnv);

        Hashtable<Object, Object> env = new Hashtable<Object, Object>();

        env.put(Context.REFERRAL, "follow");
        env.put("test.getReferralContext", "changed");

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        assertEquals(referrals[0], ex.getReferralInfo());
        Context refContext = ex.getReferralContext(env, new Control[] {
                new PagedResultsControl(1, true),
                new SortControl("hello", true) });

        Hashtable<Object, Object> refEnv = (Hashtable<Object, Object>) refContext
                .getEnvironment();

        assertEquals(env.get(Context.REFERRAL), refEnv.get(Context.REFERRAL));
        assertEquals(env.get("test.getReferralContext"), refEnv
                .get("test.getReferralContext"));
        Control[] cs = (Control[]) refEnv
                .get("java.naming.ldap.control.connect");
        
        assertNotNull(cs);
        assertEquals(2, cs.length);
        assertTrue(cs[0] instanceof PagedResultsControl);
        assertTrue(cs[1] instanceof SortControl);

        assertFalse(ex.skipReferral());

        assertNull(ex.getReferralInfo());
        
        // do nothing
        ex.retryReferral();
    }
}
