/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.jndi.tests.javax.naming.ldap;

import java.util.Arrays;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.InitialLdapContext;

import org.apache.harmony.jndi.tests.javax.naming.spi.mock.InvokeRecord;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockLdapContext;

import junit.framework.TestCase;

public class InitialLdapContextTest extends TestCase {
	InitialLdapContext ldapContext;

	@Override
    protected void setUp() throws Exception {
		super.setUp();
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockLdapContextFactory");
		Control[] cs = { new MyControl("c1", new byte[] { 1, 2, 3, 4 }, false),
				new MyControl("c1", new byte[] { 'a', 'b', 'c', 'd' }, true), };
		ldapContext = new InitialLdapContext(env, cs);

	}

	@Override
    protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testDefaultConstructor() throws NamingException {
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				"org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockLdapContextFactory");
		InitialLdapContext ctx = new InitialLdapContext();
		MyExtendedRequest extendedRequest = new MyExtendedRequest("request - 1");
		ctx.extendedOperation(extendedRequest);
		assertTrue(InvokeRecord.equals(null, extendedRequest));

	}

	public void testConstructor_Controls() throws NamingException {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockLdapContextFactory");
		Control[] cs = { new MyControl("c1", new byte[] { 1, 2, 3, 4 }, false),
				new MyControl("c1", new byte[] { 'a', 'b', 'c', 'd' }, true), };

		MyInitialLdapContext ctx = new MyInitialLdapContext(env, cs);
		MockLdapContext defaultCtx = (MockLdapContext) ctx.getDefaultContext();
		Object objCs = defaultCtx.getProps().get(
				"java.naming.ldap.control.connect");
		Object version = defaultCtx.getProps().get("java.naming.ldap.version");
		assertNotNull(version);
		Control[] cs2 = (Control[]) objCs;

		for (int i = 0; i < cs.length; i++) {
			assertEquals(cs2[i], cs[i]);
		}

		assertNotSame(cs2, cs);
		assertSame(cs2[0], cs[0]);
		assertSame(cs2[1], cs[1]);
	}

	public void testConstructor_notldapContext() throws NamingException {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory");
		Control[] cs = { new MyControl("c1", new byte[] { 1, 2, 3, 4 }, false),
				new MyControl("c1", new byte[] { 'a', 'b', 'c', 'd' }, true), };

		MyInitialLdapContext ctx = new MyInitialLdapContext(env, cs);
		try {
			ctx.reconnect(null);
			fail("Should throw NotContextException.");
		} catch (NotContextException e) {
		}

		try {
			ctx.getAttributes("hehe");
			fail("Should throw NotContextException.");
		} catch (NotContextException e) {
		}

		Context defaultContext = ctx.getDefaultContext();
        assertNotNull(defaultContext);
	}

	public void testExtendedOperation() throws NamingException {
		MyExtendedRequest extendedRequest = new MyExtendedRequest("request - 1");
		ldapContext.extendedOperation(extendedRequest);
		assertTrue(InvokeRecord.equals(null, extendedRequest));
	}

	public void testExtendedOperation_ExtendedRequest_null()
			throws NamingException {
		MyExtendedRequest extendedRequest = null;
		ldapContext.extendedOperation(extendedRequest);

		assertTrue(InvokeRecord.equals(null, extendedRequest));
	}

	public void testNewInstance() throws NamingException {
		Control[] ac = { new MyControl("c1", new byte[] { 1, 2, 3, 4 }, false),
				new MyControl("c1", new byte[] { 'a', 'b', 'c', 'd' }, true), };
		ldapContext.newInstance(ac);
		assertTrue(InvokeRecord.equals(null, ac));
	}

	public void testNewInstance_controls_null() throws NamingException {
		Control[] ac = null;
		ldapContext.newInstance(ac);
		assertTrue(InvokeRecord.equals(null, ac));
	}

	public void testReconnect() throws NamingException {
		Control[] ac = { new MyControl("c1", new byte[] { 1, 2, 3, 4 }, false),
				new MyControl("c1", new byte[] { 'a', 'b', 'c', 'd' }, true), };
		ldapContext.reconnect(ac);
		assertTrue(InvokeRecord.equals(null, ac));
	}

	public void testReconnect_controls_null() throws NamingException {
		Control[] ac = null;
		ldapContext.reconnect(ac);

		assertTrue(InvokeRecord.equals(null, ac));
	}

	public void testGetConnectControls() throws NamingException {
		ldapContext.getConnectControls();
		assertTrue(InvokeRecord.equals(null, "getConnectControls"));
	}

	public void testSetRequestControls() throws NamingException {
		Control[] ac = { new MyControl("c1", new byte[] { 1, 2, 3, 4 }, false),
				new MyControl("c1", new byte[] { 'a', 'b', 'c', 'd' }, true), };
		ldapContext.setRequestControls(ac);
		assertTrue(InvokeRecord.equals(null, ac));
	}

	public void testSetRequestControls_Controls_null() throws NamingException {
		Control[] ac = null;
		ldapContext.setRequestControls(ac);
		assertTrue(InvokeRecord.equals(null, ac));
	}

	public void testGetRequestControls() throws NamingException {
		ldapContext.getRequestControls();
		assertTrue(InvokeRecord.equals(null, "getRequestControls"));
	}

	public void testGetResponseControls() throws NamingException {
		ldapContext.getResponseControls();
		assertTrue(InvokeRecord.equals(null, "getResponseControls"));
	}

	class MyInitialLdapContext extends InitialLdapContext {

		public MyInitialLdapContext(Hashtable<String, String> h, Control[] cs)
				throws NamingException {
			super(h, cs);
		}

		public Context getDefaultContext() {
			return super.defaultInitCtx;
		}
	}

	class MyControl implements Control {
        private static final long serialVersionUID = 1L;

        boolean isCritical;

		byte[] encodedValue;

		String id;

		public MyControl(String id, byte[] encodedValue, boolean isCritical) {
			this.id = id;
			this.isCritical = isCritical;
			this.encodedValue = new byte[encodedValue.length];
			System.arraycopy(encodedValue, 0, this.encodedValue, 0,
					this.encodedValue.length);
		}

		public byte[] getEncodedValue() {
			return this.encodedValue;
		}

		public String getID() {
			return this.id;
		}

		public boolean isCritical() {
			return this.isCritical;
		}

		@Override
        public boolean equals(Object arg0) {
			if (arg0 instanceof MyControl) {
				MyControl a = (MyControl) arg0;
				return this.id.equals(a.getID())
						&& (this.isCritical == a.isCritical())
						&& Arrays
								.equals(this.encodedValue, a.getEncodedValue());
			}
			return false;
		}
	}

	class MyExtendedRequest implements ExtendedRequest {
        private static final long serialVersionUID = 1L;

        byte[] encodedValue;

		String id;

		public MyExtendedRequest(String id) {
			this.id = id;
		}

		public ExtendedResponse createExtendedResponse(String s, byte[] aB,
				int i, int i2) throws NamingException {

			return null;
		}

		public byte[] getEncodedValue() {
			return null;
		}

		public String getID() {
			return null;
		}

	}
}
