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

import java.util.Properties;

import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.InvalidNameException;
import javax.naming.LinkException;
import javax.naming.Name;

import junit.framework.TestCase;

public class LinkExceptionTest extends TestCase {

	/*
	 * -------------------------------------------------------------------
	 * Constants
	 * -------------------------------------------------------------------
	 */

	/**
	 * Constructor for TestLinkException.
	 * 
	 * @param arg0
	 */
	public LinkExceptionTest(String arg0) {
		super(arg0);
	}

	/*
	 * -------------------------------------------------------------------
	 * Methods
	 * -------------------------------------------------------------------
	 */

	public void testConstructor() {
		LinkException ex = new LinkException();
		assertNull(ex.getMessage());
	}

	public void testConstructorAndGetterSetter() throws InvalidNameException {
		LinkException ex = new LinkException("msg");
		ex.setLinkExplanation("link msg");
		CompositeName n = new CompositeName("");
		ex.setLinkRemainingName(n);
		ex.setLinkResolvedName(n);
		ex.setLinkResolvedObj("resolved obj sample");

		assertEquals("msg", ex.getMessage());
		assertEquals("link msg", ex.getLinkExplanation());
		n.add("changed");
		assertFalse(n.equals(ex.getLinkRemainingName()));
		assertFalse(n.equals(ex.getLinkResolvedName()));
		assertEquals("resolved obj sample", ex.getLinkResolvedObj());
	}

	public void testToString() throws InvalidNameException {
		LinkException ex = new LinkException("msg");
		ex.setLinkRemainingName(new CompositeName("a/b/c"));
		ex.setLinkResolvedObj("resolved obj sample");

		String str = ex.toString();
		assertTrue(str.indexOf("msg") >= 0);
		assertTrue(str.indexOf("a/b/c") >= 0);
		assertFalse(str.indexOf("resolved obj sample") >= 0);

		str = ex.toString(false);
		assertTrue(str.indexOf("msg") >= 0);
		assertTrue(str.indexOf("a/b/c") >= 0);
		assertFalse(str.indexOf("resolved obj sample") >= 0);

		str = ex.toString(true);
		assertTrue(str.indexOf("msg") >= 0);
		assertTrue(str.indexOf("a/b/c") >= 0);
		assertTrue(str.indexOf("resolved obj sample") >= 0);
	}

	public void testSetLinkResolvedName() throws InvalidNameException {
		LinkException ex = new LinkException("Test");
		Properties env = new Properties();
		env.put("jndi.syntax.direction", "flat");
		Name name = new CompoundName("Test", env);
		ex.setLinkResolvedName(name);
		ex.setLinkResolvedName(null);
		assertNull(ex.getLinkResolvedName());
	}

	public void testSetLinkRemainingName() throws InvalidNameException {
		LinkException ex = new LinkException("Test");
		Properties env = new Properties();
		env.put("jndi.syntax.direction", "flat");
		Name name = new CompoundName("Test", env);
		ex.setLinkRemainingName(name);
		ex.setLinkRemainingName(null);
		assertNull(ex.getLinkRemainingName());
	}

	public void testLinkException() {
		/*
		 * Create instance of LinkException, call the setLinkResolvedObj method
		 * with null value passed and check for toString(true) method's return
		 * value. The toString(true) returns, the resolved object is - 'null'
		 * and this message is not required for null object and this causes the
		 * Test Failure.
		 */
		LinkException ex = new LinkException("testLinkException");
		ex.setLinkResolvedObj(null);
		// System.out.println(ex.toString(true));
	}
}
