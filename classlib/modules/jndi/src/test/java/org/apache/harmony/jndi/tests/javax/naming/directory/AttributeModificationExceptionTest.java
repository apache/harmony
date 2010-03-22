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

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.directory.AttributeModificationException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.harmony.jndi.tests.javax.naming.util.Log;
import junit.framework.TestCase;

public class AttributeModificationExceptionTest extends TestCase {

	private static final Log log = new Log(AttributeModificationExceptionTest.class);

	/*
	 * -------------------------------------------------------------------
	 * Constants
	 * -------------------------------------------------------------------
	 */

	/**
	 * Constructor for AttributeModificationExceptionTest.
	 * 
	 * @param arg0
	 */
	public AttributeModificationExceptionTest(String arg0) {
		super(arg0);
	}

	/*
	 * -------------------------------------------------------------------
	 * Methods
	 * -------------------------------------------------------------------
	 */

	public void testGetterAndSetter() {
		log.setMethod("testGetterAndSetter()");
		AttributeModificationException ex = new AttributeModificationException(
				"sample message");
		ModificationItem items[] = new ModificationItem[] { new ModificationItem(
				DirContext.ADD_ATTRIBUTE, new BasicAttribute("sample id",
						"sample value")), };
		ex.setUnexecutedModifications(items);
		assertSame(items, ex.getUnexecutedModifications());
	}

	public void testToString() {
		log.setMethod("testToString()");
		String str;

		AttributeModificationException ex = new AttributeModificationException(
				"sample message");
		str = ex.toString();
		assertTrue(str.indexOf("sample message") >= 0);
		assertFalse(str.indexOf("sample id") >= 0);
		assertFalse(str.indexOf("sample value") >= 0);

		ModificationItem items[] = new ModificationItem[] { new ModificationItem(
				DirContext.ADD_ATTRIBUTE, new BasicAttribute("sample id",
						"sample value")), };

		ex.setUnexecutedModifications(items);
		str = ex.toString();
		assertTrue(str.indexOf("sample message") >= 0);
		assertTrue(str.indexOf("sample id") >= 0);
		assertTrue(str.indexOf("sample value") >= 0);
	}

	public void testGetUnexecutedModifications() {
		AttributeModificationException exception = new AttributeModificationException(
				"Test");
		assertNull(exception.getUnexecutedModifications());
	}

	public void testGetUnexecutedModifications2() {
		AttributeModificationException exception = new AttributeModificationException(
				null);
		assertNull(exception.getUnexecutedModifications());
	}

	public void testGetUnexecutedModifications3() {
		AttributeModificationException exception = new AttributeModificationException();
		assertNull(exception.getUnexecutedModifications());
	}

	public void testSetRemainingName() throws InvalidNameException {
		AttributeModificationException ex1 = new AttributeModificationException(
				"Test 1");
		AttributeModificationException ex2 = new AttributeModificationException(
				"Test 2");
		Name name = new CompositeName("TestSetRemainingName");
		ex1.setRootCause(ex2);
		ex1.setRemainingName(name);
		boolean check = ex1.toString().indexOf(
				ex1.getRemainingName().toString() + "'") > 0;
		assertTrue(check);
	}

	public void testSetRemainingName2() throws InvalidNameException {
		AttributeModificationException ex1 = new AttributeModificationException(
				"Test 1");
		AttributeModificationException ex2 = new AttributeModificationException(
				"Test 2");
		Name name = new CompositeName("TestSetRemainingName2");
		ex1.setRemainingName(name);
		ex1.setRootCause(ex2);
		boolean check = ex1.toString().indexOf(
				"[Root exception is " + ex2.toString()) > 0;
		assertTrue(check);
	}

	public void testSetUnexecutedModifications() throws InvalidNameException {
		AttributeModificationException ex1 = new AttributeModificationException(
				"Test 1");
		AttributeModificationException ex2 = new AttributeModificationException(
				"Test 2");
		Name name = new CompositeName("TestSetUnexecutedModifications");
		ex1.setRemainingName(name);
		ex1.setRootCause(ex2);
		ModificationItem[] items = {
				new ModificationItem(DirContext.ADD_ATTRIBUTE,
						new BasicAttribute("test1")),
				new ModificationItem(DirContext.ADD_ATTRIBUTE,
						new BasicAttribute("test2")), };
		ex1.setUnexecutedModifications(items);
		assertTrue(ex1.toString(false).equals(ex1.toString()));
		assertTrue(ex1.toString(true).equals(ex1.toString()));
	}
}
