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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;
import org.apache.harmony.jndi.tests.javax.naming.util.Person;

public class BasicAttributesTest extends TestCase {
	private BasicAttributes ignoreCaseAttributes;

	private BasicAttributes caseSensitiveAttributes;

	static Log log = new Log(BasicAttributesTest.class);

	@Override
    protected void setUp() {
		ignoreCaseAttributes = new BasicAttributes(true);
		caseSensitiveAttributes = new BasicAttributes(false);
	}

	public void testConstructor_simple() {
		BasicAttributes attributes = new BasicAttributes();
		assertFalse(attributes.isCaseIgnored());
		assertEquals(0, attributes.size());
	}

	public void testConstructor_ignoreCase() {
		BasicAttributes attributes = new BasicAttributes(false);
		assertFalse(attributes.isCaseIgnored());
		assertEquals(0, attributes.size());

		attributes = new BasicAttributes(true);
		assertTrue(attributes.isCaseIgnored());
		assertEquals(0, attributes.size());
	}

	public void testConstructor_IDObj_Simple() throws NamingException {
		String id = "Attribute one";
		Person person = Person.getInstance();

		BasicAttributes attributes = new BasicAttributes(id, person);
		assertEquals(1, attributes.size());

		BasicAttribute attribute = new BasicAttribute(id, person);
		assertEquals(attribute, attributes.get(id));
	}

	public void testConstructor_IDObj_Simple_IDNULL() throws NamingException {
		String id = null;
		Person person = Person.getInstance();
		try {
			new BasicAttributes(id, person);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}

	}

	public void testConstructor_IDObj_Simple_ObjNULL() throws NamingException {
		String id = "Attribute one";
		BasicAttributes attributes = new BasicAttributes(id, null);
		assertEquals(1, attributes.size());

		BasicAttribute attribute = new BasicAttribute(id, null);
		assertEquals(attribute, attributes.get(id));
	}

	public void testConstructor_IDObjIgnoreCase() {
		String id = "Attribute one";
		Person person = Person.getInstance();

		BasicAttributes attributes = new BasicAttributes(id, person, false);
		assertEquals(1, attributes.size());
		assertFalse(attributes.isCaseIgnored());
		BasicAttribute attribute = new BasicAttribute(id, person);
		assertEquals(attribute, attributes.get(id));
	}

	public void testConstructor_IDObjIgnoreCase_true() {
		String id = "Attribute one";
		Person person = Person.getInstance();

		BasicAttributes attributes = new BasicAttributes(id, person, true);
		assertEquals(1, attributes.size());
		assertTrue(attributes.isCaseIgnored());
		BasicAttribute attribute = new BasicAttribute(id, person);
		assertEquals(attribute, attributes.get(id));
	}

	public void testClone_IgnoreCase() {
		int count = 5;
		Attribute[] attributes = new Attribute[count];

		for (int i = 0; i < count; i++) {
			Person person = Person.getInstance();
			attributes[i] = new BasicAttribute(person.getName(), person);
			ignoreCaseAttributes.put(attributes[i]);
		}
		BasicAttributes cloneAttributes = (BasicAttributes) ignoreCaseAttributes
				.clone();

		assertEquals(cloneAttributes, ignoreCaseAttributes);

		for (Attribute element : attributes) {
			element.getID();
		}
		cloneAttributes.put("newID", "new Obj");
		assertEquals(ignoreCaseAttributes.size() + 1, cloneAttributes.size());
	}

	public void testClone_CaseSensitive() {
		int count = 5;
		Attribute[] attributes = new Attribute[count];

		for (int i = 0; i < count; i++) {
			Person person = Person.getInstance();
			attributes[i] = new BasicAttribute(person.getName(), person);
			caseSensitiveAttributes.put(attributes[i]);
		}
		BasicAttributes cloneAttributes = (BasicAttributes) caseSensitiveAttributes
				.clone();

		assertEquals(cloneAttributes, caseSensitiveAttributes);

		for (Attribute element : attributes) {
			element.getID();
		}
		cloneAttributes.put("newID", "new Obj");
		assertEquals(caseSensitiveAttributes.size() + 1, cloneAttributes.size());
	}

	public void testGet_IgnoreCase_Simple() {
		int count = 5;
		Attribute[] attributes = new Attribute[count];

		for (int i = 0; i < count; i++) {
			Person person = Person.getInstance();
			attributes[i] = new BasicAttribute(person.getName(), person);
			ignoreCaseAttributes.put(attributes[i]);
		}

		for (int i = 0; i < count; i++) {
			Attribute attribute = ignoreCaseAttributes.get(attributes[i]
					.getID().toUpperCase());
			assertEquals(attributes[i], attribute);
		}

		assertNull(ignoreCaseAttributes.get("Not existing value"));
	}

	public void testGet_IgnoreCase_NoValue() {
		assertNull(ignoreCaseAttributes.get("No value"));
	}

	public void testGet_IgnoreCase_IDNull() {
		try {
			ignoreCaseAttributes.get(null);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testGet_CaseSensitive_Simple() {
		int count = 5;
		Attribute[] attributes = new Attribute[count];

		for (int i = 0; i < count; i++) {
			Person person = Person.getInstance();
			attributes[i] = new BasicAttribute(person.getName(), person);
			caseSensitiveAttributes.put(attributes[i]);
		}

		for (int i = 0; i < count; i++) {
			Attribute attribute = caseSensitiveAttributes.get(attributes[i]
					.getID());
			assertEquals(attributes[i], attribute);
		}

		assertNull(caseSensitiveAttributes.get("Not existing value"));
	}

	public void testGet_CaseSensitive_NoValue() {
		assertNull(caseSensitiveAttributes.get("No value"));
	}

	public void testGet_CaseSensitive_IDNull() {
		try {
			caseSensitiveAttributes.get(null);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testGetAll_CaseSensitive() throws NamingException {
		Person person = Person.getInstance();
		Attribute attribute = new BasicAttribute(person.getName(), person);
		caseSensitiveAttributes.put(attribute);

		NamingEnumeration<?> enumeration = caseSensitiveAttributes.getAll();
		int count = 0;
		while (enumeration.hasMore()) {
			count++;
			assertEquals(attribute, enumeration.next());
		}
		assertEquals(1, count);
	}

	public void testGetAll_caseSensitive_NoValue() throws NamingException {
		NamingEnumeration<?> enumeration = caseSensitiveAttributes.getAll();
		int count = 0;
		while (enumeration.hasMore()) {
			count++;
		}

		assertEquals(0, count);
	}

	public void testGetAll_IgnoreCase() throws NamingException {
		Person person = Person.getInstance();
		Attribute attribute = new BasicAttribute(person.getName(), person);
		ignoreCaseAttributes.put(attribute);

		NamingEnumeration<?> enumeration = ignoreCaseAttributes.getAll();
		int count = 0;
		while (enumeration.hasMore()) {
			count++;
			assertEquals(attribute, enumeration.next());
		}
		assertEquals(1, count);
	}

	public void testGetAll_IgnoreCase_NoValue() throws NamingException {
		NamingEnumeration<?> enumeration = ignoreCaseAttributes.getAll();
		int count = 0;
		while (enumeration.hasMore()) {
			count++;
		}
		assertEquals(0, count);
	}

	public void testGetIDs_IgnoreCase() throws NamingException {
		String id = "Ignore case ID";
		Attribute attribute = new BasicAttribute(id, "IgnoreCase value");
		ignoreCaseAttributes.put(attribute);
		NamingEnumeration<?> enumeration = ignoreCaseAttributes.getIDs();
		int count = 0;
		while (enumeration.hasMore()) {
			count++;
			assertEquals(id, enumeration.next());
		}
		assertEquals(1, count);
	}

	public void testGetIDs_CaseSensitive() throws NamingException {
		String id = "Ignore case ID";
		Attribute attribute = new BasicAttribute(id, "IgnoreCase value");
		caseSensitiveAttributes.put(attribute);
		NamingEnumeration<?> enumeration = caseSensitiveAttributes.getIDs();
		int count = 0;
		while (enumeration.hasMore()) {
			count++;
			assertEquals(id, enumeration.next());
		}
		assertEquals(1, count);
	}

	public void testIsCaseIgnored_Ignore() {
		BasicAttributes attributes = new BasicAttributes(true);
		assertTrue(attributes.isCaseIgnored());
	}

	public void testIsCaseIgnored() {
		BasicAttributes attributes = new BasicAttributes(false);
		assertFalse(attributes.isCaseIgnored());
	}

	public void testPut_IgnoreCase_notexistID() {
		Person person = Person.getInstance();
		BasicAttribute attribute = new BasicAttribute(person.getName(), person);
		assertNull(ignoreCaseAttributes.put(attribute));
		assertEquals(attribute, ignoreCaseAttributes.get(person.getName()));
	}

	public void testPut_IgnoreCase_existID() {
		Person person0 = Person.getInstance();
		Person person1 = Person.getInstance();
		BasicAttribute attribute0 = new BasicAttribute(person0.getName()
				.toUpperCase(), person0);
		BasicAttribute attribute1 = new BasicAttribute(person0.getName(),
				person1);
		ignoreCaseAttributes.put(attribute0);
		assertEquals(attribute0, ignoreCaseAttributes.put(attribute1));
		assertEquals(attribute1, ignoreCaseAttributes.get(person0.getName()));
	}

	public void testPut_IgnoreCase_null() {
		try {
			ignoreCaseAttributes.put(null);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	/**
	 * put a attribute with null ID
	 */
	public void testPut_IgnoreCase_nullID() {
		BasicAttribute attribute = new BasicAttribute(null, "NullID");
		try {
			ignoreCaseAttributes.put(attribute);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testPut_CaseSensitive_notexistID() {
		Person person = Person.getInstance();
		BasicAttribute attribute = new BasicAttribute(person.getName(), person);
		assertNull(caseSensitiveAttributes.put(attribute));
		assertEquals(attribute, caseSensitiveAttributes.get(person.getName()));
	}

	public void testPut_CaseSensitive_existID() {
		Person person0 = Person.getInstance();
		Person person1 = Person.getInstance();
		BasicAttribute attribute0 = new BasicAttribute(person0.getName(),
				person0);
		BasicAttribute attribute1 = new BasicAttribute(person0.getName(),
				person1);
		caseSensitiveAttributes.put(attribute0);
		assertEquals(attribute0, caseSensitiveAttributes.put(attribute1));
		assertEquals(attribute1, caseSensitiveAttributes.get(person0.getName()));
	}

	public void testPut_CaseSensitive_null() {
		try {
			caseSensitiveAttributes.put(null);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	/**
	 * put a attribute with null ID
	 */
	public void testPut_CaseSensitive_nullID() {
		BasicAttribute attribute = new BasicAttribute(null, "NullID");
		try {
			caseSensitiveAttributes.put(attribute);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testPut2_IgnoreCase_notexistID() {
		Person person = Person.getInstance();
		BasicAttribute attribute = new BasicAttribute(person.getName(), person);

		assertNull(ignoreCaseAttributes.put(person.getName(), person));
		assertEquals(attribute, ignoreCaseAttributes.get(person.getName()));
	}

	public void testPut2_IgnoreCase_existID() {
		Person person0 = Person.getInstance();
		Person person1 = Person.getInstance();
		BasicAttribute attribute0 = new BasicAttribute(person0.getName()
				.toUpperCase(), person0);
		BasicAttribute attribute1 = new BasicAttribute(person0.getName(),
				person1);
		ignoreCaseAttributes.put(attribute0);
		assertEquals(attribute0, ignoreCaseAttributes.put(person0.getName(),
				person1));
		assertEquals(attribute1, ignoreCaseAttributes.get(person0.getName()));
	}

	public void testPut2_IgnoreCase_null() {
		try {
			ignoreCaseAttributes.put(null, "null id");
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testPut2_CaseSensitive_notexistID() {
		Person person = Person.getInstance();
		BasicAttribute attribute = new BasicAttribute(person.getName(), person);

		assertNull(caseSensitiveAttributes.put(person.getName(), person));
		assertEquals(attribute, caseSensitiveAttributes.get(person.getName()));
	}

	public void testPut2_CaseSensitive_existID() {
		Person person0 = Person.getInstance();
		Person person1 = Person.getInstance();
		BasicAttribute attribute0 = new BasicAttribute(person0.getName(),
				person0);
		BasicAttribute attribute1 = new BasicAttribute(person0.getName(),
				person1);
		caseSensitiveAttributes.put(attribute0);
		assertEquals(attribute0, caseSensitiveAttributes.put(person0.getName(),
				person1));
		assertEquals(attribute1, caseSensitiveAttributes.get(person0.getName()));
	}

	public void testPut2_CaseSensitive_null() {
		try {
			caseSensitiveAttributes.put(null, "null id");
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testRemove_IgnoreCase_exist() {
		Person person = Person.getInstance();
		BasicAttribute attribute = new BasicAttribute(person.getName(), person);
		ignoreCaseAttributes.put(attribute);

		assertEquals(attribute, ignoreCaseAttributes.remove(person.getName()
				.toUpperCase()));

	}

	public void testRemove_IgnoreCase_notexist() {
		assertNull(ignoreCaseAttributes.remove("not this id"));
	}

	public void testRemove_IgnoreCase_Null() {
		try {
			ignoreCaseAttributes.remove(null);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testRemove_CaseSensitive_exist() {
		Person person = Person.getInstance();
		BasicAttribute attribute = new BasicAttribute(person.getName(), person);
		caseSensitiveAttributes.put(attribute);

		assertEquals(attribute, caseSensitiveAttributes
				.remove(person.getName()));

	}

	public void testRemove_CaseSensitive_notexist() {
		assertNull(caseSensitiveAttributes.remove("not this id"));
	}

	public void testRemove_CaseSensitive_Null() {
		try {
			caseSensitiveAttributes.remove(null);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testSize() {
		int count = 10;
		for (int i = 0; i < count; i++) {
			ignoreCaseAttributes.put("ID:" + i, "Value: " + i);
		}
		assertEquals(count, ignoreCaseAttributes.size());
	}

	public void testSize_empty() {
		assertEquals(0, caseSensitiveAttributes.size());
	}

	/**
	 * 1. ignoreCase=true 2. Normal values
	 */
	public void testHashCode_ignoreCase() {
		int count = 10;
		BasicAttribute attribute[] = new BasicAttribute[count];

		int hashCode = 1;
		for (int i = 0; i < count; i++) {
			attribute[i] = new BasicAttribute("ID:" + i, "Value: " + i);
			hashCode += attribute[i].hashCode();
			ignoreCaseAttributes.put(attribute[i]);
		}

		assertEquals(hashCode, ignoreCaseAttributes.hashCode());
	}

	/**
	 * 1. ignoreCase=true 2. no value
	 */
	public void testHashCode_ignoreCase_NoValue() {
		assertEquals(1, ignoreCaseAttributes.hashCode());
	}

	/**
	 * 1. ignoreCase=false 2. Normal values
	 */
	public void testHashCode_CaseSensitive() {
		int count = 10;
		BasicAttribute attribute[] = new BasicAttribute[count];

		int hashCode = 0;
		for (int i = 0; i < count; i++) {
			attribute[i] = new BasicAttribute("ID:" + i, "Value: " + i);
			hashCode += attribute[i].hashCode();
			caseSensitiveAttributes.put(attribute[i]);
		}

		assertEquals(hashCode, caseSensitiveAttributes.hashCode());
	}

	/**
	 * 1. ignoreCase=false 2. no value
	 */
	public void testHashCode_CaseSensitive_NoValue() {
		assertEquals(0, caseSensitiveAttributes.hashCode());
	}

	/**
	 * 1. ignoreCase=true 2. Normal values
	 */
	public void testToString_ignoreCase() {
		int count = 10;
		BasicAttribute attribute[] = new BasicAttribute[count];

		for (int i = 0; i < count; i++) {
			attribute[i] = new BasicAttribute("ID:" + i, "Value: " + i);
			ignoreCaseAttributes.put(attribute[i]);
		}
		// assertEquals(str, ignoreCaseAttributes.toString());
		assertNotNull(ignoreCaseAttributes.toString());
	}

	/**
	 * 1. IgnoreCase=true 2. No value
	 */
	public void testToString_ignoreCase_NoValue() {
		/*
		 * assertEquals( "This Attributes does not have any attributes.\n",
		 * ignoreCaseAttributes.toString());
		 */
		assertNotNull(ignoreCaseAttributes.toString());
	}

	/**
	 * 1. IgnoreCase=false 2. Normal values
	 */
	public void testToString_CaseSensitive() {
		int count = 10;
		BasicAttribute attribute[] = new BasicAttribute[count];

		for (int i = 0; i < count; i++) {
			attribute[i] = new BasicAttribute("ID:" + i, "Value: " + i);
			caseSensitiveAttributes.put(attribute[i]);
		}
		// assertEquals(str, caseSensitiveAttributes.toString());
		assertNotNull(caseSensitiveAttributes.toString());
	}

	/**
	 * 1. IgnoreCase=false 2. no value
	 */
	public void testToString_CaseSensitive_NoValue() {
		/*
		 * assertEquals( "This Attributes does not have any attributes.\n",
		 * caseSensitiveAttributes.toString());
		 */
		assertNotNull(caseSensitiveAttributes.toString());
	}

	/**
	 * 1. ignoreCase=true 2. Normal values
	 */
	public void testEquals_ignoreCase() {
		BasicAttributes basicAttributes0 = new BasicAttributes(true);
		BasicAttributes basicAttributes1 = new BasicAttributes(true);

		int count = 10;
		BasicAttribute attribute[] = new BasicAttribute[count];
		for (int i = 0; i < count; i++) {
			attribute[i] = new BasicAttribute("ID:" + i, "Value: " + i);
			basicAttributes0.put(attribute[i]);
			basicAttributes1.put(attribute[i]);
		}
		assertTrue(basicAttributes0.equals(basicAttributes1));
		assertTrue(basicAttributes1.equals(basicAttributes0));
		assertFalse(basicAttributes0.equals(null));

		basicAttributes0.remove("ID:0");
		assertFalse(basicAttributes0.equals(basicAttributes1));
		assertFalse(basicAttributes1.equals(basicAttributes0));
	}

	/**
	 * 1. ignoreCase=true 2. no value
	 */
	public void testEquals_ignoreCase_NoValue() {
		BasicAttributes basicAttributes0 = new BasicAttributes(true);
		BasicAttributes basicAttributes1 = new BasicAttributes(true);

		assertTrue(basicAttributes0.equals(basicAttributes1));
		assertTrue(basicAttributes1.equals(basicAttributes0));
		assertFalse(basicAttributes0.equals(null));
	}

	public void testEquals_invalidObject() {
		BasicAttributes basicAttributes0 = new BasicAttributes(true);
		assertFalse(basicAttributes0.equals("String"));
	}

	public void testEquals_DifferentIgnoreCase() {
		BasicAttributes basicAttributes0 = new BasicAttributes(true);
		BasicAttributes basicAttributes1 = new BasicAttributes(false);

		assertFalse(basicAttributes0.equals(basicAttributes1));
		assertFalse(basicAttributes1.equals(basicAttributes0));
	}

	public void testEquals_DifferentSize() {
		BasicAttributes basicAttributes0 = new BasicAttributes(true);
		BasicAttributes basicAttributes1 = new BasicAttributes(true);

		basicAttributes1.put("attr1", "value");

		assertFalse(basicAttributes0.equals(basicAttributes1));
		assertFalse(basicAttributes1.equals(basicAttributes0));
	}

	/**
	 * 1. ignoreCase=false 2. Normal values
	 */
	public void testEquals_caseSensitive() {
		BasicAttributes basicAttributes0 = new BasicAttributes(false);
		BasicAttributes basicAttributes1 = new BasicAttributes(false);

		int count = 10;
		BasicAttribute attribute[] = new BasicAttribute[count];
		for (int i = 0; i < count; i++) {
			attribute[i] = new BasicAttribute("ID:" + i, "Value: " + i);
			basicAttributes0.put(attribute[i]);
			basicAttributes1.put(attribute[i]);
		}
		assertTrue(basicAttributes0.equals(basicAttributes1));
		assertTrue(basicAttributes1.equals(basicAttributes0));
		assertFalse(basicAttributes0.equals(null));
	}

	/**
	 * 1. ignoreCase=false 2. no value
	 */
	public void testEquals_caseSensitive_NoValue() {
		BasicAttributes basicAttributes0 = new BasicAttributes(false);
		BasicAttributes basicAttributes1 = new BasicAttributes(false);

		assertTrue(basicAttributes0.equals(basicAttributes1));
		assertTrue(basicAttributes1.equals(basicAttributes0));
		assertFalse(basicAttributes0.equals(null));
	}

	public void testSerializable_Simple() throws ClassNotFoundException,
			IOException {
		int count = 10;
		BasicAttribute attribute[] = new BasicAttribute[count];

		for (int i = 0; i < count; i++) {
			attribute[i] = new BasicAttribute("ID:" + i, "Value: " + i);
			caseSensitiveAttributes.put(attribute[i]);
		}

		// write to byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(caseSensitiveAttributes);
		byte[] buffer = baos.toByteArray();

		// read from byte array
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
		ObjectInputStream ois = new ObjectInputStream(bais);
		BasicAttributes attributes2 = (BasicAttributes) ois.readObject();

		assertEquals(caseSensitiveAttributes, attributes2);
	}

	public void testSerializable_compatibility() throws ClassNotFoundException,
			IOException {
		ObjectInputStream ois = new ObjectInputStream(
                getClass()
                        .getClassLoader()
                        .getResourceAsStream(
                                "/serialization/javax/naming/directory/BasicAttributes.ser"));
		BasicAttributes attributes2 = (BasicAttributes) ois.readObject();

		int count = 10;
		BasicAttribute attribute[] = new BasicAttribute[count];

		for (int i = 0; i < count; i++) {
			attribute[i] = new BasicAttribute("ID:" + i, "Value: " + i);
			caseSensitiveAttributes.put(attribute[i]);
		}

		assertEquals(caseSensitiveAttributes, attributes2);
	}
}
