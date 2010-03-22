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

package org.apache.harmony.jndi.tests.javax.naming.directory;

import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.InvokeRecord;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

public class InitialDirContextTest extends TestCase {

	static Log log = new Log(InitialDirContextTest.class);

	static Name n;

	static Name urln;

	static String strn;

	static String urlstrn;

	static String s;

	static Object o;

	static Object os[];

	static Integer i;

	static Attributes a;

	static String as[];

	static ModificationItem mis[];

	static SearchControls c;

	static {
		try {
			n = new CompositeName("name/sample");
			urln = new CompositeName("'http://www.apache.org/foundation'");
			strn = "name/sample";
			urlstrn = "http://www.apache.org/foundation";
			s = "str sample";
			o = "object sample";
			os = new Object[0];
			i = new Integer(1);
			a = new BasicAttributes("id sample", "value sample");
			as = new String[] { "attr1", "attr2" };
			mis = new ModificationItem[0];
			c = new SearchControls();
		} catch (InvalidNameException e) {
			e.printStackTrace();
		}
	}

	DirContext ctx = null;

	/**
	 * Constructor for InitialDirContextTest.
	 * 
	 * @param arg0
	 */
	public InitialDirContextTest(String arg0) {
		super(arg0);
	}


	@Override
    protected void setUp() throws Exception {
		super.setUp();
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirContextFactory");
		env.put(Context.URL_PKG_PREFIXES, "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
		ctx = new InitialDirContext(env);
	}


	@Override
    protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testWhatAreUrlStringNames() throws NamingException {
		log.setMethod("testWhatAreUrlStringNames()");

		testIsUrlStringName("http://www.apache.org/foundation", "http");
		testIsUrlStringName("http://www.apache.org", "http");
		testIsUrlStringName("http://", "http");
		testIsUrlStringName("http:", "http");

		testIsUrlStringName("https://www.apache.org/foundation", "https");
		testIsUrlStringName("https://www.apache.org", "https");
		testIsUrlStringName("https://", "https");
		testIsUrlStringName("https:", "https");

		testIsUrlStringName("abc://www.apache.org/foundation", "abc");
		testIsUrlStringName("abc://www.apache.org", "abc");
		testIsUrlStringName("abc://", "abc");
		testIsUrlStringName("abc:", "abc");

		testIsUrlStringName("'http://www.apache.org/foundation'", null);
		testIsUrlStringName("'http://www.apache.org'", null);
		testIsUrlStringName("'http://'", null);
		testIsUrlStringName("'http:'", null);

		testIsUrlStringName("http", null);
		testIsUrlStringName("https", null);
		testIsUrlStringName("abc", null);
		testIsUrlStringName("'http", null);
	}

	public void testWhatAreUrlStringNames_OddCase1() throws NamingException {
		log.setMethod("testWhatAreUrlStringNames_OddCase1()");

		testIsUrlStringName("HTTP2://www.apache.org/foundation", "HTTP2");
		testIsUrlStringName("HTTP2://www.apache.org", "HTTP2");
		testIsUrlStringName("HTTP2://", "HTTP2");
		testIsUrlStringName("HTTP2:", "HTTP2");
	}

	public void testWhatAreUrlStringNames_OddCase2() throws NamingException {
		log.setMethod("testWhatAreUrlStringNames_OddCase2()");

		testIsUrlStringName("a_b_c://www.apache.org/foundation", "a_b_c");
		testIsUrlStringName("a_b_c://www.apache.org", "a_b_c");
		testIsUrlStringName("a_b_c://", "a_b_c");
		testIsUrlStringName("a_b_c:", "a_b_c");
	}

	private void testIsUrlStringName(String str, String expectedSchema)
			throws NamingException {
		ctx.bind(str, o, a);
		assertEquals(expectedSchema, InvokeRecord.getLatestUrlSchema());
		ctx.rebind(str, o, a);
		assertEquals(expectedSchema, InvokeRecord.getLatestUrlSchema());
	}

	/*
	 * Test for void bind(Name, Object, Attributes)
	 */
	public void testBindNameObjectAttributes() throws NamingException {
		log.setMethod("testBindNameObjectAttributes()");
		ctx.bind(n, o, a);
		assertTrue(InvokeRecord.equals(null, "bind", n, o, a));
		ctx.bind(urln, o, a);
		assertTrue(InvokeRecord.equals("http", "bind", urln, o, a));
	}

	/*
	 * Test for void bind(String, Object, Attributes)
	 */
	public void testBindStringObjectAttributes() throws NamingException {
		log.setMethod("testBindStringObjectAttributes()");
		ctx.bind(strn, o, a);
		assertTrue(InvokeRecord.equals(null, "bind", strn, o, a));
		ctx.bind(urlstrn, o, a);
		assertTrue(InvokeRecord.equals("http", "bind", urlstrn, o, a));
	}

	/*
	 * Test for DirContext createSubcontext(Name, Attributes)
	 */
	public void testCreateSubcontextNameAttributes() throws NamingException {
		log.setMethod("testCreateSubcontextNameAttributes()");
		ctx.createSubcontext(n, a);
		assertTrue(InvokeRecord.equals(null, "createSubcontext", n, a));
		ctx.createSubcontext(urln, a);
		assertTrue(InvokeRecord.equals("http", "createSubcontext", urln, a));
	}

	/*
	 * Test for DirContext createSubcontext(String, Attributes)
	 */
	public void testCreateSubcontextStringAttributes() throws NamingException {
		log.setMethod("testCreateSubcontextStringAttributes()");
		ctx.createSubcontext(strn, a);
		assertTrue(InvokeRecord.equals(null, "createSubcontext", strn, a));
		ctx.createSubcontext(urlstrn, a);
		assertTrue(InvokeRecord.equals("http", "createSubcontext", urlstrn, a));
	}

	/*
	 * Test for Attributes getAttributes(Name)
	 */
	public void testGetAttributesName() throws NamingException {
		log.setMethod("testGetAttributesName()");
		ctx.getAttributes(n);
		try {
			assertTrue(InvokeRecord.equals(null, "getAttributes", n));
		} catch (AssertionFailedError e) {
			assertTrue(InvokeRecord.equals(null, "getAttributes", n, null));
		}
		ctx.getAttributes(urln);
		try {
			assertTrue(InvokeRecord.equals("http", "getAttributes", urln));
		} catch (AssertionFailedError e) {
			assertTrue(InvokeRecord.equals("http", "getAttributes", urln, null));
		}
	}

	/*
	 * Test for Attributes getAttributes(Name, String[])
	 */
	public void testGetAttributesNameStringArray() throws NamingException {
		log.setMethod("testGetAttributesNameStringArray()");
		ctx.getAttributes(n, as);
		assertTrue(InvokeRecord.equals(null, "getAttributes", n, as));
		ctx.getAttributes(urln, as);
		assertTrue(InvokeRecord.equals("http", "getAttributes", urln, as));
	}

	/*
	 * Test for Attributes getAttributes(String)
	 */
	public void testGetAttributesString() throws NamingException {
		log.setMethod("testGetAttributesString()");
		ctx.getAttributes(strn);
		try {
			assertTrue(InvokeRecord.equals(null, "getAttributes", strn));
		} catch (AssertionFailedError e) {
			assertTrue(InvokeRecord.equals(null, "getAttributes", strn, null));
		}
		ctx.getAttributes(urlstrn);
		try {
			assertTrue(InvokeRecord.equals("http", "getAttributes", urlstrn));
		} catch (AssertionFailedError e) {
			assertTrue(InvokeRecord.equals("http", "getAttributes", urlstrn,
					null));
		}
	}

	/*
	 * Test for Attributes getAttributes(String, String[])
	 */
	public void testGetAttributesStringStringArray() throws NamingException {
		log.setMethod("testGetAttributesStringStringArray()");
		ctx.getAttributes(strn, as);
		assertTrue(InvokeRecord.equals(null, "getAttributes", strn, as));
		ctx.getAttributes(urlstrn, as);
		assertTrue(InvokeRecord.equals("http", "getAttributes", urlstrn, as));
	}

	/*
	 * Test for DirContext getSchema(Name)
	 */
	public void testGetSchemaName() throws NamingException {
		log.setMethod("testGetSchemaName()");
		ctx.getSchema(n);
		assertTrue(InvokeRecord.equals(null, "getSchema", n));
		ctx.getSchema(urln);
		assertTrue(InvokeRecord.equals("http", "getSchema", urln));
	}

	/*
	 * Test for DirContext getSchema(String)
	 */
	public void testGetSchemaString() throws NamingException {
		log.setMethod("testGetSchemaString()");
		ctx.getSchema(strn);
		assertTrue(InvokeRecord.equals(null, "getSchema", strn));
		ctx.getSchema(urlstrn);
		assertTrue(InvokeRecord.equals("http", "getSchema", urlstrn));
	}

	/*
	 * Test for DirContext getSchemaClassDefinition(Name)
	 */
	public void testGetSchemaClassDefinitionName() throws NamingException {
		log.setMethod("testGetSchemaClassDefinitionName()");
		ctx.getSchemaClassDefinition(n);
		assertTrue(InvokeRecord.equals(null, "getSchemaClassDefinition", n));
		ctx.getSchemaClassDefinition(urln);
		assertTrue(InvokeRecord
				.equals("http", "getSchemaClassDefinition", urln));
	}

	/*
	 * Test for DirContext getSchemaClassDefinition(String)
	 */
	public void testGetSchemaClassDefinitionString() throws NamingException {
		log.setMethod("testGetSchemaClassDefinitionString()");
		ctx.getSchemaClassDefinition(strn);
		assertTrue(InvokeRecord.equals(null, "getSchemaClassDefinition", strn));
		ctx.getSchemaClassDefinition(urlstrn);
		assertTrue(InvokeRecord.equals("http", "getSchemaClassDefinition",
				urlstrn));
	}

	/*
	 * Test for void modifyAttributes(Name, int, Attributes)
	 */
	public void testModifyAttributesNameintAttributes() throws NamingException {
		log.setMethod("testModifyAttributesNameintAttributes()");
		ctx.modifyAttributes(n, i.intValue(), a);
		assertTrue(InvokeRecord.equals(null, "modifyAttributes", n, i, a));
		ctx.modifyAttributes(urln, i.intValue(), a);
		assertTrue(InvokeRecord.equals("http", "modifyAttributes", urln, i, a));
	}

	/*
	 * Test for void modifyAttributes(Name, ModificationItem[])
	 */
	public void testModifyAttributesNameModificationItemArray()
			throws NamingException {
		log.setMethod("testModifyAttributesNameModificationItemArray()");
		ctx.modifyAttributes(n, mis);
		assertTrue(InvokeRecord.equals(null, "modifyAttributes", n, mis));
		ctx.modifyAttributes(urln, mis);
		assertTrue(InvokeRecord.equals("http", "modifyAttributes", urln, mis));
	}

	/*
	 * Test for void modifyAttributes(String, int, Attributes)
	 */
	public void testModifyAttributesStringintAttributes()
			throws NamingException {
		log.setMethod("testModifyAttributesStringintAttributes()");
		ctx.modifyAttributes(strn, i.intValue(), a);
		assertTrue(InvokeRecord.equals(null, "modifyAttributes", strn, i, a));
		ctx.modifyAttributes(urlstrn, i.intValue(), a);
		assertTrue(InvokeRecord.equals("http", "modifyAttributes", urlstrn, i,
				a));
	}

	/*
	 * Test for void modifyAttributes(String, ModificationItem[])
	 */
	public void testModifyAttributesStringModificationItemArray()
			throws NamingException {
		log.setMethod("testModifyAttributesStringModificationItemArray()");
		ctx.modifyAttributes(strn, mis);
		assertTrue(InvokeRecord.equals(null, "modifyAttributes", strn, mis));
		ctx.modifyAttributes(urlstrn, mis);
		assertTrue(InvokeRecord
				.equals("http", "modifyAttributes", urlstrn, mis));
	}

	/*
	 * Test for void rebind(Name, Object, Attributes)
	 */
	public void testRebindNameObjectAttributes() throws NamingException {
		log.setMethod("testRebindNameObjectAttributes()");
		ctx.rebind(n, o, a);
		assertTrue(InvokeRecord.equals(null, "rebind", n, o, a));
		ctx.rebind(urln, o, a);
		assertTrue(InvokeRecord.equals("http", "rebind", urln, o, a));
	}

	/*
	 * Test for void rebind(String, Object, Attributes)
	 */
	public void testRebindStringObjectAttributes() throws NamingException {
		log.setMethod("testRebindStringObjectAttributes()");
		ctx.rebind(strn, o, a);
		assertTrue(InvokeRecord.equals(null, "rebind", strn, o, a));
		ctx.rebind(urlstrn, o, a);
		assertTrue(InvokeRecord.equals("http", "rebind", urlstrn, o, a));
	}

	/*
	 * Test for NamingEnumeration search(Name, Attributes)
	 */
	public void testSearchNameAttributes() throws NamingException {
		log.setMethod("testSearchNameAttributes()");
		ctx.search(n, a);
		assertTrue(InvokeRecord.equals(null, "search", n, a));
		ctx.search(urln, a);
		assertTrue(InvokeRecord.equals("http", "search", urln, a));
	}

	/*
	 * Test for NamingEnumeration search(Name, Attributes, String[])
	 */
	public void testSearchNameAttributesStringArray() throws NamingException {
		log.setMethod("testSearchNameAttributesStringArray()");
		ctx.search(n, a, as);
		assertTrue(InvokeRecord.equals(null, "search", n, a, as));
		ctx.search(urln, a, as);
		assertTrue(InvokeRecord.equals("http", "search", urln, a, as));
	}

	/*
	 * Test for NamingEnumeration search(Name, String, Object[], SearchControls)
	 */
	public void testSearchNameStringObjectArraySearchControls()
			throws NamingException {
		log.setMethod("testSearchNameStringObjectArraySearchControls()");
		ctx.search(n, s, os, c);
		assertTrue(InvokeRecord.equals(null, "search", n, s, os, c));
		ctx.search(urln, s, os, c);
		assertTrue(InvokeRecord.equals("http", "search", urln, s, os, c));
	}

	/*
	 * Test for NamingEnumeration search(Name, String, SearchControls)
	 */
	public void testSearchNameStringSearchControls() throws NamingException {
		log.setMethod("testSearchNameStringSearchControls()");
		ctx.search(n, s, c);
		assertTrue(InvokeRecord.equals(null, "search", n, s, c));
		ctx.search(urln, s, c);
		assertTrue(InvokeRecord.equals("http", "search", urln, s, c));
	}

	/*
	 * Test for NamingEnumeration search(String, Attributes)
	 */
	public void testSearchStringAttributes() throws NamingException {
		log.setMethod("testSearchStringAttributes()");
		ctx.search(strn, a);
		assertTrue(InvokeRecord.equals(null, "search", strn, a));
		ctx.search(urlstrn, a);
		assertTrue(InvokeRecord.equals("http", "search", urlstrn, a));
	}

	/*
	 * Test for NamingEnumeration search(String, Attributes, String[])
	 */
	public void testSearchStringAttributesStringArray() throws NamingException {
		log.setMethod("testSearchStringAttributesStringArray()");
		ctx.search(strn, a, as);
		assertTrue(InvokeRecord.equals(null, "search", strn, a, as));
		ctx.search(urlstrn, a, as);
		assertTrue(InvokeRecord.equals("http", "search", urlstrn, a, as));
	}

	/*
	 * Test for NamingEnumeration search(String, String, Object[],
	 * SearchControls)
	 */
	public void testSearchStringStringObjectArraySearchControls()
			throws NamingException {
		log.setMethod("testSearchStringStringObjectArraySearchControls()");
		ctx.search(strn, s, os, c);
		assertTrue(InvokeRecord.equals(null, "search", strn, s, os, c));
		ctx.search(urlstrn, s, os, c);
		assertTrue(InvokeRecord.equals("http", "search", urlstrn, s, os, c));
	}

	/*
	 * Test for NamingEnumeration search(String, String, SearchControls)
	 */
	public void testSearchStringStringSearchControls() throws NamingException {
		log.setMethod("testSearchStringStringSearchControls()");
		ctx.search(strn, s, c);
		assertTrue(InvokeRecord.equals(null, "search", strn, s, c));
		ctx.search(urlstrn, s, c);
		assertTrue(InvokeRecord.equals("http", "search", urlstrn, s, c));
	}

	/*
	 * Test for void close()
	 */
	public void testClose() throws NamingException {
		log.setMethod("testClose()");
		ctx.close();
		assertTrue(InvokeRecord.equals(null, "close"));
	}

	public void testDefaultConstructor() throws NamingException {
		log.setMethod("testDefaultConstructor()");
		// unstable behavior, hard to dig deep. just make logs.
		try {
			ctx = new InitialDirContext();
			// expected result
		} catch (Throwable e) {
			e.printStackTrace();
			log.log(e);
			fail();
		}
	}
}
