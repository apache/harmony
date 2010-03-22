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
package org.apache.harmony.jndi.tests.javax.naming;

import javax.naming.Binding;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

public class BindingTest extends TestCase {

	private static final Log log = new Log(BindingTest.class);

	/**
	 * Constructor for TestBinding.
	 * 
	 * @param arg0
	 */
	public BindingTest(String arg0) {
		super(arg0);
	}

	/*
	 * @see TestCase#setUp()
	 */
	@Override
    protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	@Override
    protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testConstructor_Simple() {
		Binding p;

		p = new Binding("name1", new Integer(1));
		assertEquals("name1", p.getName());
		assertEquals("java.lang.Integer", p.getClassName());
		assertEquals(new Integer(1), p.getObject());
		assertTrue(p.isRelative());
	}

	public void testConstructor_NullValue() {
		Binding p;

		p = new Binding("name1", null, null);
		assertEquals("name1", p.getName());
		assertNull(p.getClassName());
		assertNull(p.getObject());
	}

	public void testConstructor_DefaultRelativeValue() {
		Binding p;

		p = new Binding("name1", null);
		assertTrue(p.isRelative());
	}

	public void testToString() {
		log.setMethod("testToString");
		Binding p;

		p = new Binding("name1", null, false);
		assertTrue(p.toString().startsWith("(not relative"));
		p = new Binding("name1", new Integer(3));
		String str = p.toString();
		assertTrue(str.indexOf("name1") > -1);
		assertTrue(str.indexOf("3") > -1);
		assertTrue(str.indexOf("java.lang.Integer") > -1);
	}

	public void testGetSetObject() {
		Binding p;
		log.setMethod("testGetSetObject");
		p = new Binding("name", null);
		p.setObject(new Integer(2));
		assertEquals(new Integer(2), p.getObject());
		assertEquals("java.lang.Integer", p.getClassName());
		p.setObject(null);
		assertNull(p.getObject());
		assertNull(p.getClassName());
		p.setObject(new Float(2));
		assertEquals(new Float(2), p.getObject());
		assertEquals(Float.class.getName(), p.getClassName());
		p.setObject(null);
		assertNull(p.getObject());
		assertNull(p.getClassName());
	}

	public void testGetSetName() {
		Binding p;

		p = new Binding("name", new Integer(1));
		assertEquals("name", p.getName());
		p.setName("name1");
		assertEquals("name1", p.getName());
		p.setName("");
		assertEquals("", p.getName());
	}

	public void testGetSetClassName() {
		Binding p;

		p = new Binding("name", new Integer(1));
		assertEquals(Integer.class.getName(), p.getClassName());
		p.setClassName(Character.class.getName());
		assertEquals(Character.class.getName(), p.getClassName());
	}

	public void testGetSetRelative() {
		Binding p;

		p = new Binding("name", new Integer(1));
		assertTrue(p.isRelative());
		p.setRelative(false);
		assertFalse(p.isRelative());
	}
}
