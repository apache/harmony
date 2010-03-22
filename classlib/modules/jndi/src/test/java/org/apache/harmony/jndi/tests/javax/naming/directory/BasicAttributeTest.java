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
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Random;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.BasicAttribute;

import junit.framework.TestCase;

import org.apache.harmony.jndi.tests.javax.naming.util.Person;

public class BasicAttributeTest extends TestCase {

	private BasicAttribute orderedAttribute;

	private BasicAttribute unorderedAttribute;

	@Override
    protected void setUp() {
		orderedAttribute = new BasicAttribute("Ordered_Attribute", true);
		unorderedAttribute = new BasicAttribute("Unordered_Attribute", false);
	}

	/**
	 * Test BasicAttribute constructor 1) use a specified ID 2) the default
	 * order flag is set to false 3) contain zero value.
	 */
	public void testConstructor_ByID() {
		String ID = "attribute one";
		BasicAttribute attribute = new BasicAttribute(ID);
		assertEquals(ID, attribute.getID());
		assertFalse(attribute.isOrdered());
		assertEquals(0, attribute.size());
	}

	/**
	 * Test BasicAttribute constructor with null ID
	 */
	public void testConstructor_ByIDNull() {
		BasicAttribute attribute = new BasicAttribute(null);
		assertNull(attribute.getID());

	}

	/**
	 * Test BasicAttribute constructor 1) use a specified ID 2) use a specified
	 * order flag 3) contain zero value.
	 */
	public void testConstructor_ByIDOrderFlag() {
		String ID = "attribute two";
		boolean flag = false;
		BasicAttribute attribute = new BasicAttribute(ID, flag);

		assertEquals(ID, attribute.getID());
		assertEquals(flag, attribute.isOrdered());
		assertEquals(0, attribute.size());

		ID = "attribute three";
		flag = true;
		attribute = new BasicAttribute(ID, flag);

		assertEquals(ID, attribute.getID());
		assertEquals(flag, attribute.isOrdered());
		assertEquals(0, attribute.size());
	}

	/**
	 * Test BasicAttribute constructor 1) use a specified ID 2) the default
	 * order flag is set to false 3) specify a initial value
	 */
	public void testConstructor_ByIDInitialValue() throws NamingException {
		String ID = "attribute four";
		Date date = new Date();
		BasicAttribute attribute = new BasicAttribute(ID, date);

		assertEquals(ID, attribute.getID());
		assertFalse(attribute.isOrdered());
		assertEquals(date, attribute.get());
	}

	/**
	 * Test BasicAttribute constructor 1) use a specified ID 2) use a specified
	 * order flag 3) specify a initial value
	 */
	public void testConstructor_ByIDOrderFlagInitialValue()
			throws NamingException {
		String ID = "attribute five";
		boolean flag = true;
		Date date = new Date();
		BasicAttribute attribute = new BasicAttribute(ID, date, flag);

		assertEquals(ID, attribute.getID());
		assertEquals(flag, attribute.isOrdered());
		assertEquals(date, attribute.get());
	}

	/**
	 * test add a simple object through add()
	 */
	public void testAdd_unorder_Simple() throws NamingException {
		int count = 5;

		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			assertTrue(unorderedAttribute.add(persons[i]));
		}

		for (int i = 0; i < count; i++) {
			assertSame(persons[i], unorderedAttribute.get(i));
		}

		assertEquals(count, unorderedAttribute.size());
	}

	public void testAdd_unorder_ExistingValue()
			throws CloneNotSupportedException, NamingException {
		Person person = Person.getInstance();
		Person clonePerson = (Person) person.clone();
		unorderedAttribute.add(person);

		assertFalse(unorderedAttribute.add(clonePerson));
		assertEquals(1, unorderedAttribute.size());
		assertEquals(clonePerson, unorderedAttribute.get(0));
	}

	public void testAdd_unordered_ExistingValueArray() {
		String[] team = { "Blue", "Yellow", "Red", };
		String[] newTeam = new String[team.length];
		System.arraycopy(team, 0, newTeam, 0, team.length);

		unorderedAttribute.add(team);
		assertFalse(unorderedAttribute.add(newTeam));
		assertEquals(1, unorderedAttribute.size());
	}

	public void testAdd_unorder_valueNull() throws NamingException {
		assertTrue(unorderedAttribute.add(null));
		assertNull(unorderedAttribute.get(0));
	}

	public void testAdd_unorder_ExistingNull() throws NamingException {
		assertTrue(unorderedAttribute.add(null));
		assertFalse(unorderedAttribute.add(null));
		assertEquals(1, unorderedAttribute.size());
		assertNull(unorderedAttribute.get(0));
	}

	public void testAdd_order_Simple() throws NamingException {
		int count = 5;

		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			assertTrue(orderedAttribute.add(persons[i]));
		}

		for (int i = 0; i < count; i++) {
			assertSame(persons[i], orderedAttribute.get(i));
		}

		assertEquals(count, orderedAttribute.size());
	}

	public void testAdd_order_ExistingValue() throws NamingException,
			CloneNotSupportedException {
		Person person = Person.getInstance();
		Person clonePerson = (Person) person.clone();

		assertTrue(orderedAttribute.add(person));
		assertTrue(orderedAttribute.add(clonePerson));
		assertEquals(2, orderedAttribute.size());
		assertEquals(orderedAttribute.get(0), orderedAttribute.get(1));
	}

	public void testAdd_order_ValueNull() {
		int count = 5;
		for (int i = 0; i < count; i++) {
			assertTrue(orderedAttribute.add(null));
		}

		assertEquals(count, orderedAttribute.size());
	}

	/**
	 * Test void add(int location, Object val)
	 */
	public void testAdd2_order_Simple() throws NamingException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			orderedAttribute.add(i, persons[i]);
		}

		for (int i = 0; i < count; i++) {
			assertEquals(persons[i], orderedAttribute.get(i));
		}
	}

	public void testAdd2_order_ExistValue() throws NamingException {
		String value0 = "string value";
		String value1 = "another string value";
		orderedAttribute.add(0, value0);
		orderedAttribute.add(0, value1);
		assertEquals(2, orderedAttribute.size());
		assertEquals(value1, orderedAttribute.get(0));
		assertEquals(value0, orderedAttribute.get(1));
	}

	public void testAdd2_order_ValueNull() throws NamingException {
		orderedAttribute.add(0, null);
		orderedAttribute.add(0, null);
		assertEquals(2, orderedAttribute.size());
		assertNull(orderedAttribute.get(0));
		assertNull(orderedAttribute.get(1));
	}

	public void testAdd2_order_OutOfRangeLess() throws NamingException {
		try {
			orderedAttribute.add(-1, "Index is -1");
			fail("add(-1, value) should throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	public void testAdd2_order_OutOfRangeOver() throws NamingException {
		try {
			orderedAttribute.add(orderedAttribute.size() + 1,
					"Index is size() + 1");
			fail("add(size() + 1, value) should throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	public void testAdd2_unorder_Simple() throws NamingException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			unorderedAttribute.add(i, persons[i]);
		}

		for (int i = 0; i < count; i++) {
			assertEquals(persons[i], unorderedAttribute.get(i));
		}
	}

	public void testAdd2_unorder_ExistValue() throws NamingException {
		String value = "string value";
		unorderedAttribute.add(0, value);
		try {
			unorderedAttribute.add(0, value);
			fail("An value already exist, throw IllegalStateException.");
		} catch (IllegalStateException e) {
		}

		assertEquals(1, unorderedAttribute.size());
	}

	public void testAdd2_unorder_ExistValueArray() {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
		}
		Person[] newPersons = new Person[count];
		System.arraycopy(persons, 0, newPersons, 0, count);

		unorderedAttribute.add(0, persons);
		try {
			unorderedAttribute.add(0, newPersons);
			fail("An value already exist, should throw IllegalStateException.");
		} catch (IllegalStateException e) {
		}
	}

	public void testAdd2_unorder_ValueNull() throws NamingException {
		unorderedAttribute.add(0, null);
		try {
			unorderedAttribute.add(0, null);
			fail("An value already exist, should throw IllegalStateException.");
		} catch (IllegalStateException e) {
		}

		assertEquals(1, unorderedAttribute.size());
	}

	public void testAdd2_unorder_OutOfRangeLess() throws NamingException {
		try {
			unorderedAttribute.add(-1, "Index is -1");
			fail("add(-1, value) should throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	public void testAdd2_unorder_OutOfRangeOver() throws NamingException {
		try {
			unorderedAttribute.add(orderedAttribute.size() + 1,
					"Index is size() + 1");
			fail("add(size() + 1, value) should throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	/**
	 * test clear() add of the values.
	 */
	public void testClear() {
		int count = 10;
		for (int i = 0; i < count; i++) {
			unorderedAttribute.add(new Integer(i));
			orderedAttribute.add(new Integer(i));
		}
		assertEquals(count, unorderedAttribute.size());
		assertEquals(count, orderedAttribute.size());

		unorderedAttribute.clear();
		orderedAttribute.clear();
		assertEquals(0, unorderedAttribute.size());
		assertEquals(0, orderedAttribute.size());
	}

	/**
	 * test clone()
	 */
	public void testClone_ordered() throws NamingException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			orderedAttribute.add(persons[i]);
		}

		BasicAttribute cloneAttribute = (BasicAttribute) orderedAttribute
				.clone();

		for (int i = 0; i < count; i++) {
			assertSame(orderedAttribute.get(i), cloneAttribute.get(i));
		}
		assertTrue(cloneAttribute.isOrdered());
		assertEquals(orderedAttribute.getID(), cloneAttribute.getID());
		// assertNotSame(orderedAttribute.values, cloneAttribute.values);
		cloneAttribute.add("new object");
		assertEquals(orderedAttribute.size() + 1, cloneAttribute.size());
	}

	public void testClone_unordered() throws NamingException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			unorderedAttribute.add(persons[i]);
		}

		BasicAttribute cloneAttribute = (BasicAttribute) unorderedAttribute
				.clone();

		for (int i = 0; i < count; i++) {
			assertSame(unorderedAttribute.get(i), cloneAttribute.get(i));
		}
		assertFalse(cloneAttribute.isOrdered());
		assertEquals(unorderedAttribute.getID(), cloneAttribute.getID());
		// assertNotSame(unorderedAttribute.values, cloneAttribute.values);
		cloneAttribute.add("new object");
		assertEquals(unorderedAttribute.size() + 1, cloneAttribute.size());
	}

	/**
	 * test contains
	 */
	public void testContains_unordered() {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			unorderedAttribute.add(persons[i]);
		}

		for (int i = 0; i < count; i++) {
			assertTrue(unorderedAttribute.contains(persons[i]));
		}
		Person person = Person.getInstance();
		assertFalse(unorderedAttribute.contains(person));
	}

	public void testContains_unordered_null() {
		unorderedAttribute.add(null);
		assertTrue(unorderedAttribute.contains(null));
	}

	public void testContains_unordered_array() {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
		}
		Person[] newPersons = new Person[count];
		System.arraycopy(persons, 0, newPersons, 0, count);
		unorderedAttribute.add(persons);
		assertTrue(unorderedAttribute.contains(newPersons));
	}

	public void testContains_unordered_IntArray() {
		int count = 5;
		int[] numbers = new int[count];
		for (int i = 0; i < count; i++) {
			numbers[i] = i * 100;
		}
		int[] newNumbers = new int[count];
		System.arraycopy(numbers, 0, newNumbers, 0, count);
		unorderedAttribute.add(numbers);
		assertTrue(unorderedAttribute.contains(newNumbers));
	}

	public void testContains_unordered_ArrayOfArray() {
		Person person0 = Person.getInstance();
		Person person1 = Person.getInstance();

		Object[][] arrays = { { "Blue", "Yellow", "Red" },
				{ person0, person1, },
				{ new Integer(100), new Integer(200), new Integer(300), }, };

		Object[][] newArrays = { { "Blue", "Yellow", "Red" },
				{ person0, person1, },
				{ new Integer(100), new Integer(200), new Integer(300), }, };

		unorderedAttribute.add(arrays);
		assertFalse(unorderedAttribute.contains(newArrays));
		// TO DO: behavior of array of array
	}

	public void testContains_unordered_IntArray2() {
		// TO DO: int array and integer array
		int[] numbers = { 1, 2, 3, };
		Integer[] integers = { new Integer(1), new Integer(2), new Integer(3), };
		orderedAttribute.add(numbers);
		assertFalse(orderedAttribute.contains(integers));
	}

	public void testContains_unordered_arraynull() {
		// TO DO: int array and integer array
		String[] strs = { "Blue", "Yellow", null, "Red", };
		String[] newStrs = { "Blue", "Yellow", null, "Red", };

		orderedAttribute.add(strs);
		assertTrue(orderedAttribute.contains(newStrs));
	}

	public void testContains_unordered_IntShortArray() {
		int[] ints = { 1, 2, 3, 4, };
		short[] shorts = { 1, 2, 3, 4, };

		orderedAttribute.add(ints);
		assertFalse(orderedAttribute.contains(shorts));
		// TO DO: how about int and short array
	}

	public void testContains_ordered() {
		String value = "same value";
		orderedAttribute.add(value);
		orderedAttribute.add(value);
		assertTrue(orderedAttribute.contains(value));
		assertFalse(orderedAttribute.contains(value + "another value"));
	}

	public void testContains_ordered_null() {
		orderedAttribute.add(null);
		orderedAttribute.add(null);
		assertTrue(orderedAttribute.contains(null));
	}

	public void testContains_ordered_array() {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
		}
		Person[] newPersons = new Person[count];
		System.arraycopy(persons, 0, newPersons, 0, count);
		orderedAttribute.add(persons);
		assertTrue(orderedAttribute.contains(newPersons));
	}

	public void testGet_unordered() throws NamingException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			unorderedAttribute.add(persons[i]);
		}
		assertEquals(unorderedAttribute.get(0), unorderedAttribute.get());
	}

	public void testGet_unordered_noValue() throws NamingException {
		try {
			unorderedAttribute.get();
			fail("No value, throw NoSuchElementException.");
			// return -> throw.
		} catch (NoSuchElementException e) {
		}
	}

	public void testGet_unordered_ValueNull() throws NamingException {
		unorderedAttribute.add(null);
		assertNull(unorderedAttribute.get());
	}

	public void testGet_ordered() throws NamingException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			orderedAttribute.add(persons[i]);
		}
		assertEquals(orderedAttribute.get(0), orderedAttribute.get());
	}

	public void testGet_ordered_noValue() throws NamingException {
		try {
			orderedAttribute.get();
			fail("No value, throw NoSuchElementException.");
			// return -> throw.
		} catch (NoSuchElementException e) {
		}
	}

	public void testGet_ordered_ValueNull() throws NamingException {
		orderedAttribute.add(null);
		assertNull(orderedAttribute.get());
	}

	public void testGet2_undered_tooSmall() throws NamingException {
		Person person = Person.getInstance();
		unorderedAttribute.add(person);
		try {
			unorderedAttribute.get(-1);
			fail("get(-1), throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	public void testGet2_undered_tooLarge() throws NamingException {
		Person person = Person.getInstance();
		unorderedAttribute.add(person);
		try {
			unorderedAttribute.get(unorderedAttribute.size());
			fail("get(size()), throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	public void testGetAll_ordered() throws NamingException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			orderedAttribute.add(persons[i]);
		}

		NamingEnumeration<?> enumeration = orderedAttribute.getAll();
		int i = 0;
		while (enumeration.hasMore()) {
			assertEquals(persons[i++], enumeration.next());
		}
	}

	public void testGetAll_ordered_noValue() throws NamingException {
		NamingEnumeration<?> enumeration = orderedAttribute.getAll();
		int count = 0;
		while (enumeration.hasMore()) {
			count++;
		}
		assertEquals(0, count);
	}

	public void testGetAll_unordered() throws NamingException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			unorderedAttribute.add(persons[i]);
		}

		NamingEnumeration<?> enumeration = unorderedAttribute.getAll();
		int i = 0;
		while (enumeration.hasMore()) {
			assertEquals(persons[i++], enumeration.next());
		}
	}

	public void testGetAll_unordered_noValue() throws NamingException {
		NamingEnumeration<?> enumeration = unorderedAttribute.getAll();
		int count = 0;
		while (enumeration.hasMore()) {
			count++;
		}
		assertEquals(0, count);
	}

	public void testGetAttributeDefinition() throws NamingException {
		try {
			orderedAttribute.getAttributeDefinition();
			fail("Should throw OperationNotSupportedException");
		} catch (OperationNotSupportedException e) {
		}
	}

	public void testGetAttributeSyntaxDefinition() throws NamingException {
        try {
            orderedAttribute.getAttributeSyntaxDefinition();
            fail("Should throw OperationNotSupportedException");
        } catch (OperationNotSupportedException e) {
        }
    }

	public void testGetID() {
		String ID = "attribute ID";
		BasicAttribute attribute = new BasicAttribute(ID);
		assertEquals(ID, attribute.getID());
	}

	public void testGetID_null() {
		BasicAttribute attribute = new BasicAttribute(null);
		assertNull(attribute.getID());
	}

	public void testIsOrdered() {
		String ID = "ordered";
		BasicAttribute attribute = new BasicAttribute(ID, true);
		assertTrue(attribute.isOrdered());
	}

	public void testIsOrdered_false() {
		String ID = "unordered";
		BasicAttribute attribute = new BasicAttribute(ID);
		assertFalse(attribute.isOrdered());
	}

	/**
	 * Object remove(int i)
	 */
	public void testRemove_simple() throws NamingException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			unorderedAttribute.add(persons[i]);
		}

		assertEquals(persons[0], unorderedAttribute.remove(0));

		for (int i = 0; i < count - 1; i++) {
			assertSame(persons[i + 1], unorderedAttribute.get(i));
		}
	}

	public void testRemove_novalue() {
		try {
			orderedAttribute.remove(0);
			fail("Should throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	public void testRemove_tooSmall() {
		orderedAttribute.add("value one");
		try {
			orderedAttribute.remove(-1);
			fail("Should throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	public void testRemove_tooLarge() {
		orderedAttribute.add("value one");
		try {
			orderedAttribute.remove(orderedAttribute.size());
			fail("Should throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	/**
	 * TEST: boolean remove(Object obj)
	 */
	public void testRemove2_simple() {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			unorderedAttribute.add(persons[i]);
		}

		for (int i = 0; i < count; i++) {
			assertTrue(unorderedAttribute.remove(persons[i]));
		}
	}

	public void testRemove2_DuplicateValue() throws NamingException {
		Person person = Person.getInstance();
		orderedAttribute.add(0, person);
		orderedAttribute.add(1, "signal");
		orderedAttribute.add(2, person);
		assertTrue(orderedAttribute.remove(person));
		assertEquals(2, orderedAttribute.size());
		assertEquals(person, orderedAttribute.get(1));
	}

	public void testRemove2_NotMatch() {
		Person person = Person.getInstance();
		unorderedAttribute.add(person);
		Person person2 = Person.getInstance();
		assertFalse(unorderedAttribute.remove(person2));
	}

	public void testRemove2_NoValue() {
		assertFalse(unorderedAttribute.remove("Novalue"));
	}

	public void testRemove2_array() {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
		}
		Person[] newPersons = new Person[count];
		System.arraycopy(persons, 0, newPersons, 0, count);

		orderedAttribute.add(persons);
		assertTrue(orderedAttribute.remove(newPersons));
	}

	public void testSet_ordered_Simple() throws NamingException {
		Person person = Person.getInstance();
		orderedAttribute.add(person);
		Person person2 = Person.getInstance();

		assertEquals(person, orderedAttribute.set(0, person2));
		assertEquals(person2, orderedAttribute.get(0));
	}

	public void testSet_ordered_NewValueNull() throws NamingException {
		Person person = Person.getInstance();
		orderedAttribute.add(person);

		assertEquals(person, orderedAttribute.set(0, null));
		assertNull(orderedAttribute.get(0));
	}

	public void testSet_ordered_OldValueNull() throws NamingException {
		orderedAttribute.add(null);
		Person person = Person.getInstance();

		assertNull(orderedAttribute.set(0, person));
		assertEquals(person, orderedAttribute.get(0));
	}

	public void testSet_ordered_IndexTooSmall() {
		orderedAttribute.add("value");
		try {
			orderedAttribute.remove(-1);
			fail("Should throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	public void testSet_ordered_IndexTooLarge() {
		orderedAttribute.add("value");
		try {
			orderedAttribute.remove(orderedAttribute.size());
			fail("Should throw IndexOutOfBoundsException.");
		} catch (IndexOutOfBoundsException e) {
		}
	}

	public void testSet_order_ExistValue() throws NamingException {
		Person person = Person.getInstance();
		orderedAttribute.add(person);
		assertEquals(person, orderedAttribute.set(0, person));
		assertEquals(person, orderedAttribute.get(0));
	}

	public void testSet_unorder_ExistValue() {
		Person person = Person.getInstance();
		unorderedAttribute.add(person);
		try {
			unorderedAttribute.set(0, person);
			fail("Should throw IllegalStateException.");
		} catch (IllegalStateException e) {
		}
	}

	public void testSet_unorder_ExistValueArray() {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
		}
		Person[] newPersons = new Person[count];
		System.arraycopy(persons, 0, newPersons, 0, count);

		unorderedAttribute.add(persons);
		try {
			unorderedAttribute.set(0, newPersons);
			fail("Should throw IllegalStateException.");
		} catch (IllegalStateException e) {
		}
	}

	public void testSize() {
		assertEquals(0, orderedAttribute.size());
		int count = 5;
		for (int i = 0; i < count; i++) {
			orderedAttribute.add("value" + i);
		}

		assertEquals(count, orderedAttribute.size());
		orderedAttribute.clear();
		assertEquals(0, orderedAttribute.size());
	}

	/**
	 * test equals
	 */
	public void testEquals() throws CloneNotSupportedException {
		String ID = "equals";
		Person person = Person.getInstance();
		Person personClone = (Person) person.clone();
		BasicAttribute attribute0 = new BasicAttribute(ID);
		attribute0.add(person);

		BasicAttribute attribute1 = new BasicAttribute(ID);
		attribute1.add(personClone);

		assertTrue(attribute0.equals(attribute1));
		assertTrue(attribute1.equals(attribute0));
		assertFalse(attribute0.equals(null));
	}

	public void testEquals_Array() throws CloneNotSupportedException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
		}
		Person[] newPersons = new Person[count];
		System.arraycopy(persons, 0, newPersons, 0, count);

		String id = "Array Attribute";
		BasicAttribute attribute0 = new BasicAttribute(id, persons, true);

		BasicAttribute attribute1 = new BasicAttribute(id, newPersons, true);

		assertTrue(attribute0.equals(attribute1));
		assertTrue(attribute1.equals(attribute0));
		assertFalse(attribute0.equals(null));
	}

	/**
	 * test equals with different IDs
	 * 
	 */
	public void testNotEquals_ByID() {
		String ID = "equals";
		String ID2 = "not equals";

		BasicAttribute attribute0 = new BasicAttribute(ID);
		BasicAttribute attribute1 = new BasicAttribute(ID2);

		assertFalse(attribute0.equals(attribute1));
	}

	/**
	 * test equals with different ordering setting
	 */
	public void testNotEquals_ByOrderFlag() {
		String ID = "not equals";
		Person person = Person.getInstance();
		BasicAttribute attribute0 = new BasicAttribute(ID, person, false);
		BasicAttribute attribute1 = new BasicAttribute(ID, person, true);

		assertFalse(attribute0.equals(attribute1));
	}

	/**
	 * test equals with different value
	 */
	public void testNotEquals_ByValue() {
		String ID = "not equals";
		Person person0 = Person.getInstance();
		Person person1 = Person.getInstance();

		BasicAttribute attribute0 = new BasicAttribute(ID, person0);
		BasicAttribute attribute1 = new BasicAttribute(ID, person1);

		assertFalse(attribute0.equals(attribute1));
	}

	public void testEquals_IDNull() {
		String strObj = "attribute with null id";
		BasicAttribute attribute0 = new BasicAttribute(null, strObj);
		BasicAttribute attribute1 = new BasicAttribute(null, strObj);
		try {
			attribute0.equals(attribute1);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testEquals_ObjNull() {
		String id = "no-value";
		BasicAttribute attribute0 = new BasicAttribute(id, null);
		BasicAttribute attribute1 = new BasicAttribute(id, null);
		assertTrue(attribute0.equals(attribute1));
	}

	public void testEquals_diff_ordered() {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
		}
		String id = "un-Ordered";
		BasicAttribute unordered0 = new BasicAttribute(id);
		BasicAttribute unordered1 = new BasicAttribute(id);
		for (int i = 0; i < count; i++) {
			unordered0.add(persons[i]);
		}

		for (int i = count - 1; i > -1; i--) {
			unordered1.add(persons[i]);
		}
		assertEquals(unordered0.size(), unordered1.size());
		assertTrue(unordered0.equals(unordered1));
	}

	/**
	 * 1. Check ordered.equals(unordered) 2. Check unordered.equals(ordered) 3.
	 * Check the values have the same order
	 */
	public void testEquals_Ordered_Unordered_1() {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
		}

		for (int i = 0; i < count; i++) {
			orderedAttribute.add(persons[i]);
			unorderedAttribute.add(persons[i]);
		}
		assertFalse(orderedAttribute.equals(unorderedAttribute));
		assertFalse(unorderedAttribute.equals(orderedAttribute));
	}

	/**
	 * 1. Check ordered.equals(unordered) 2. Check unordered.equals(ordered) 3.
	 * the values have the different order
	 */
	public void testEquals_Ordered_Unordered_2() {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
		}

		for (int i = 0; i < count; i++) {
			orderedAttribute.add(persons[i]);
		}

		for (int i = count - 1; i > -1; i--) {
			unorderedAttribute.add(persons[i]);
		}
		assertFalse(unorderedAttribute.equals(orderedAttribute));
		assertFalse(orderedAttribute.equals(unorderedAttribute));
	}

	public void testHashCode_simple() throws NamingException {
		int count = 5;
		for (int i = 0; i < count; i++) {
			orderedAttribute.add("Value: " + i);
		}

		int hashCode = orderedAttribute.getID().hashCode();

		for (int i = 0; i < count; i++) {
			hashCode += orderedAttribute.get(i).hashCode();
		}
		assertEquals(hashCode, orderedAttribute.hashCode());
	}

	public void testHashCode_noValue() {
		assertEquals(unorderedAttribute.getID().hashCode(), unorderedAttribute
				.hashCode());
	}

	public void testHashCode_arrayValue() {
		String[] strs = { "Blue", "Yellow", null, "Red", };
		String id = "Array Attribute";
		BasicAttribute attribute = new BasicAttribute(id, strs);
		int hashCode = id.hashCode();
		for (String element : strs) {
			if (element != null) {
				hashCode += element.hashCode();
			}
		}

		assertEquals(hashCode, attribute.hashCode());
	}

	public void testHashCode_intArrayValue() {
		int[] numbers = new int[10];
		for (int i = 0; i < numbers.length; i++) {
			numbers[i] = i * 10;
		}
		String id = "int-Array";
		BasicAttribute attribute = new BasicAttribute(id, numbers);
		Person person = Person.getInstance();
		attribute.add(person);
		int hashCode = id.hashCode() + person.hashCode();
		for (int element : numbers) {
			hashCode += element;
		}

		assertEquals(hashCode, attribute.hashCode());
	}

	public void testHashCode_DoubleArray() {
		Random random = new Random(100);
		double[] doubles = new double[10];
		for (int i = 0; i < doubles.length; i++) {
			doubles[i] = random.nextDouble() * 1000;
		}
		String id = "double-Array";
		BasicAttribute attribute = new BasicAttribute(id, doubles);
		int hashCode = id.hashCode();
		for (double element : doubles) {
			hashCode += new Double(element).hashCode();
		}
		assertEquals(hashCode, attribute.hashCode());
	}

	public void testHashCode_IDnull() {
		BasicAttribute attribute = new BasicAttribute(null, "ID==NULL");
		try {
			attribute.hashCode();
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testHashCode_ObjNull() {
		String id = "nulls";
		BasicAttribute attribute = new BasicAttribute(id, true);
		for (int i = 0; i < 5; i++) {
			attribute.add(null);
		}
		assertEquals(id.hashCode(), attribute.hashCode());
	}

	public void testToString_simple() {
		// TO DO: explore behavior
		int count = 5;
		for (int i = 0; i < count; i++) {
			orderedAttribute.add("Value: " + i);
		}
		assertNotNull(orderedAttribute.toString());
	}

	public void testToString_noValue() {
		// TO DO: explore behavior
		/*
		 * assertEquals( "Attribute ID: Unordered_Attribute\nAttribute values:
		 * This Attribute does not have any values.\n",
		 * unorderedAttribute.toString());
		 */
		assertNotNull(unorderedAttribute.toString());
	}

	public void testToString_ArrayValue() {
		// TO DO: explore behavior
		String[] strs = { "Blue", "Yellow", null, "Red", };
		String id = "Array Attribute";
		BasicAttribute attribute = new BasicAttribute(id, strs);
		/*
		 * assertEquals( "Attribute ID: " + id + "\nAttribute values: " +
		 * strs.toString() + "\n", attribute.toString());
		 */
		assertNotNull(attribute.toString());
	}

	public void testToString_intValue() {
		// TO DO: explore behavior
		int[] numbers = new int[10];
		for (int i = 0; i < numbers.length; i++) {
			numbers[i] = i * 10;
		}
		String id = "int-Array";
		BasicAttribute attribute = new BasicAttribute(id, numbers);
		/*
		 * assertEquals( "Attribute ID: " + id + "\nAttribute values: " +
		 * numbers.toString() + "\n", attribute.toString());
		 */
		assertNotNull(attribute.toString());
	}

	public void testToString_doubleValue() {
		// TO DO: explore behavior
		Random random = new Random(1000);
		double[] doubles = new double[10];
		for (int i = 0; i < doubles.length; i++) {
			doubles[i] = random.nextDouble() * 1000;
		}
		String id = "double-Array";
		BasicAttribute attribute = new BasicAttribute(id, doubles);

		/*
		 * assertEquals( "Attribute ID: " + id + "\nAttribute values: " +
		 * doubles.toString() + "\n", attribute.toString());
		 */
		assertNotNull(attribute.toString());
	}

	public void testToString_nullValue() {
		// TO DO: explore behavior
		String id = "nulls";
		BasicAttribute attribute = new BasicAttribute(id, true);
		for (int i = 0; i < 5; i++) {
			attribute.add(null);
		}

		assertNotNull(attribute.toString());
	}

	public void testToString_IDNull() {
		// TO DO: explore behavior
		BasicAttribute attribute = new BasicAttribute(null, "ID==NULL");
		assertNotNull(attribute.toString());
	}

	public void testSerializable_Simple() throws ClassNotFoundException,
			IOException {
		int count = 5;
		Person[] persons = new Person[count];
		for (int i = 0; i < count; i++) {
			persons[i] = Person.getInstance();
			unorderedAttribute.add(persons[i]);
		}

		// write to byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(unorderedAttribute);
		byte[] buffer = baos.toByteArray();

		// read from byte array
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
		ObjectInputStream ois = new ObjectInputStream(bais);
		BasicAttribute attribute2 = (BasicAttribute) ois.readObject();

		assertEquals(unorderedAttribute, attribute2);
	}

	public void testSerializable_compatibility() throws ClassNotFoundException,
			IOException {
		ObjectInputStream ois = new ObjectInputStream(
                getClass()
                        .getClassLoader()
                        .getResourceAsStream(
                                "/serialization/javax/naming/directory/BasicAttribute.ser"));
		BasicAttribute attribute2 = (BasicAttribute) ois.readObject();

		BasicAttribute attribute = new BasicAttribute("serializeBasicAttribute");
		int count = 10;
		for (int i = 0; i < count; i++) {
			attribute.add("Int value: " + i * 10);
		}

		assertEquals(attribute, attribute2);
		// TO DO: cause an EOFException
	}
}
