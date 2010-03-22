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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Properties;

import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.InvalidNameException;
import javax.naming.Name;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

/**
 * unit test for composite name
 * 
 */
public class CompositeNameTest extends TestCase {

	/*
	 * -------------------------------------------------------------------
	 * Constants
	 * -------------------------------------------------------------------
	 */
	private static final Log log = new Log(CompositeNameTest.class);

	private static final char SEPARATOR = '/';

	/*
	 * -------------------------------------------------------------------
	 * Instance variables (Should be private)
	 * -------------------------------------------------------------------
	 */
	private CompositeName name;

	private final String[] elements = { "www.apache.org", "gbank" };

	private String initName;

	private Properties props;

	/**
	 * Constructor for TestCompositeName.
	 * 
	 * @param arg0
	 */
	public CompositeNameTest(String arg0) {
		super(arg0);
		StringBuffer sb = new StringBuffer();
		for (String element : elements) {
			sb.append(element).append(SEPARATOR);
		}
		initName = sb.toString();
	}

	/*
	 * -------------------------------------------------------------------
	 * Methods
	 * -------------------------------------------------------------------
	 */

	@Override
    protected void setUp() throws Exception {
		super.setUp();
		name = new CompositeName(initName);
		props = new Properties();
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
	}

	@Override
    protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * test constructor with null string parameter
	 * 
	 * @throws InvalidNameException
	 */
	public void TestConstructorNull() throws InvalidNameException {
		log.setMethod("TestConstructorNull");
		try {
			name = new CompositeName((String) null);
			log.log("fail: try to construct a compositename with null succeed");
			fail();
		} catch (NullPointerException e) {
		}
	}

	public void testConstructorSimple() throws InvalidNameException {
		assertNameEquals(new CompositeName("a/b/c"), new String[] { "a", "b",
				"c" });
	}

	public void testConstructorException() throws InvalidNameException {
		try {
			name = new CompositeName("'abc'd/ef");
			fail();
		} catch (InvalidNameException e) {
		}
		name = new CompositeName("abc'abc'/ab");
		assertNameEquals(name, new String[] { "abc'abc'", "ab" });
	}

	/**
	 * test toString()
	 * 
	 * @throws InvalidNameException
	 */
	public void testToString() throws InvalidNameException {
		log.setMethod("testToString()");
		assertEquals("", new CompositeName("").toString());
		assertEquals("/", new CompositeName("/").toString());
		assertEquals("//", new CompositeName("//").toString());
		assertEquals("/a/", new CompositeName("/a/").toString());
		name.add("abc\"abc");
		assertEquals(4, name.size());
		name.add("abc/abc");
		assertEquals(5, name.size());
		name.add("abc\\abc");
		assertEquals(6, name.size());
		assertEquals(new CompositeName(name.toString()), name);
		assertNameEquals(name, new String[] { "www.apache.org", "gbank",
				"", "abc\"abc", "abc/abc", "abc\\abc" });

        assertEquals("test's", new CompositeName("test's").toString());
        assertEquals("test", new CompositeName("'test'").toString());
        
        name = new CompositeName("O=\"s,g");
        assertEquals("O=\"s,g", name.toString());
        
        name = new CompositeName("\"O=/s\"");
        assertEquals("O=/s", name.get(0));

        name.add("s/s");
        assertEquals(2, name.size());
        assertEquals("\"O=/s\"/\"s/s\"", name.toString());

        name.add("ss");
        assertEquals(3, name.size());
        assertEquals("\"O=/s\"/\"s/s\"/ss", name.toString());

        name = new CompositeName("\"O=\\s\"");
        assertEquals("O=\\s", name.toString());

        CompositeName name = new CompositeName("");
        name.add("abc/abc");
        assertEquals(1, name.size());

        CompositeName newCompositeName = new CompositeName(name.toString());
        assertEquals(1, newCompositeName.size());
        assertEquals(newCompositeName, name);
        
        name = new CompositeName("O=\"s,g\"");
        assertEquals("O=\"s,g\"", name.toString());
        
        name = new CompositeName("O=#");
        assertEquals("O=#", name.toString());
        
        name = new CompositeName("O=\\s");
        assertEquals("O=\\s", name.toString());
        
        name = new CompositeName("\"O=\\s\"");
        assertEquals("O=\\s", name.toString());
        
        name = new CompositeName("O=/s");
        assertEquals("O=/s", name.toString());

        name = new CompositeName("\"O=\"/\"s\"");
        assertEquals(2,name.size());
        assertEquals("O=/s", name.toString());
        
        name = new CompositeName("");
        assertEquals(0,name.size());
        assertEquals("", name.toString());
        
        name = new CompositeName("\"\"");
        assertEquals(1,name.size());
        assertEquals("/", name.toString());
        
        name = new CompositeName("\"\"/\"\"");
        assertEquals(2,name.size());
        assertEquals("//", name.toString());
        
        name = new CompositeName("\"O=/s\"");
        assertEquals("\"O=/s\"", name.toString());

	}

	/**
	 * test getAll()
	 * 
	 */
	public void testGetAll() {
		log.setMethod("testGetAll()");
		Enumeration<?> enumeration = name.getAll();
		for (String element : elements) {
			assertTrue(element.equals(enumeration.nextElement()));
		}
		assertTrue("".equals(enumeration.nextElement()));
	}

	/**
	 * test get()
	 * 
	 */
	public void testGet() {
		log.setMethod("testGet()");
		for (int j = 0; j < elements.length; j++) {
			assertEquals(elements[j], name.get(j));
		}
		try {
			name.get(-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.get(name.size());
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	/**
	 * test getPrefix()
	 * 
	 */
	public void testGetPrefix() {
		log.setMethod("testGetPrefix()");
		Name prefix = name.getPrefix(0);
		assertEquals("", prefix.toString());
		try {
			name.getPrefix(elements.length + 2);
			fail("name.getPrefix(elements.length + 2)");
		} catch (IndexOutOfBoundsException e) {
		}
		try {
			name.getPrefix(-1);
			fail("name.getPrefix(-1)");
		} catch (IndexOutOfBoundsException e) {
		}
		prefix = name.getPrefix(1);
		assertEquals(elements[0], prefix.toString());
	}

	/**
	 * test getSuffix
	 * 
	 */
	public void testGetSuffix() {
		log.setMethod("testGetSuffix()");
		Name suffix = name.getSuffix(elements.length + 1);
		assertEquals("", suffix.toString());
		try {
			name.getSuffix(elements.length + 2);
			fail("name.getSuffix(elements.length + 2)");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.getSuffix(-1);
			fail("name.getSuffix(-1)");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		suffix = name.getSuffix(2);
		assertEquals("/", suffix.toString());
	}

	/**
	 * Test addAll(Name), include exceptional case
	 */
	public void testAddAllName() throws InvalidNameException {
		log.setMethod("testAddAllName()");
		int oldSize = name.size();
		name.addAll(new CompositeName(initName));
		assertEquals(name.size(), oldSize * 2);
		assertEquals(name.getPrefix(3).toString(), name.getSuffix(3).toString());
		assertEquals(name.getPrefix(3).toString(), initName);
		name = new CompositeName("a");
		try {
			name.addAll(new CompoundName("b/c", props));
			fail("name.addAll(new CompoundName(\"b/c\", props));");
		} catch (InvalidNameException t) {
		}
		try {
			name.addAll(null);
			fail("Add null should throw NullPointerException");
		} catch (NullPointerException e) {
		}
	}

	/**
	 * test addAll(int), include exceptional case
	 */
	public void testAddAllintName() throws InvalidNameException {
		log.setMethod("testAddAllintName()");
		int oldSize = name.size();
		name.addAll(1, new CompositeName(initName));
		assertEquals(name.size(), oldSize * 2);
		assertEquals(name.getSuffix(1).getPrefix(3).toString(), initName);
		name = new CompositeName("a");
		try {
			name.addAll(0, new CompoundName("b/c", props));
			fail("name.addAll(0, new CompoundName(\"b/c\", props));");
		} catch (InvalidNameException t) {
		}
		try {
			name.addAll(0, null);
			fail("Add null should throw NullPointerException");
		} catch (NullPointerException e) {
		}
		try {
			name.addAll(-1, new CompositeName(initName));
			fail("-1 should be out of bound.");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.addAll(name.size() + 1, new CompositeName(initName));
			fail((name.size() + 1) + " should out of bound.");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	/**
	 * test add(String), include exceptional case of null parameter
	 */
	public void testAddString() throws InvalidNameException {
		log.setMethod("testAddString()");
		int oldSize = name.size();
		name.add(elements[0]);
		assertEquals(name.size(), oldSize + 1);
		assertEquals(elements[0], name.getSuffix(3).toString());
		name.add(null);
		assertEquals(name.size(), oldSize + 2);
		try {
			assertNull(name.getSuffix(4).toString());
			fail();
		} catch (NullPointerException e) {
		}

	}

	/**
	 * Test add(int, String), include boundary case
	 */
	public void testAddintString() throws InvalidNameException {
		log.setMethod("testAddintString()");
		int oldSize = name.size();
		name.add(1, elements[0]);
		assertEquals(name.size(), oldSize + 1);
		assertEquals(name.getSuffix(1).toString(), initName);
		try {
			name.add(oldSize + 2, elements[0]);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.add(-1, elements[0]);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	/**
	 * test remove, include boundary case
	 * 
	 * @throws InvalidNameException
	 */
	public void testRemove() throws InvalidNameException {
		log.setMethod("testRemove()");
		int oldSize = name.size();
		name.remove(1);
		assertEquals(name.size(), oldSize - 1);
		assertEquals(name.toString(), elements[0] + "/");
		try {
			name.remove(-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.remove(oldSize - 1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}

	}

	/**
	 * test size()
	 * 
	 * @throws InvalidNameException
	 */
	public void testSize() throws InvalidNameException {
		log.setMethod("testSize()");
		assertEquals(elements.length + 1, name.size());
		name = new CompositeName("/");
		assertEquals(1, name.size());
	}

	/**
	 * test isEmpty()
	 * 
	 * @throws InvalidNameException
	 */
    @SuppressWarnings("unused")
	public void testIsEmpty() throws InvalidNameException {
		log.setMethod("testIsEmpty()");
		assertFalse(name.isEmpty());
		for (String element : elements) {
			name.remove(0);
		}
		name.remove(0);
		assertTrue(name.isEmpty());
		name = new CompositeName("");
		assertTrue(name.isEmpty());

	}

	/**
	 * test startWith(), include exceptional case of null and CompoundName
	 * 
	 * @throws InvalidNameException
	 */
	public void testStartsWith() throws InvalidNameException {
		log.setMethod("testStartsWith()");
		CompositeName start = new CompositeName(elements[0]);
		assertTrue(name.startsWith(start));
		start = new CompositeName(elements[1]);
		assertFalse(name.startsWith(start));
		try {
			assertFalse(name.startsWith(null));
		} catch (Throwable e) {
			log.log("start with null?", e);
		}
		try {
			assertFalse(name.startsWith(new CompoundName(elements[0], props)));
		} catch (Throwable e) {
			log.log("start with compoundName?", e);
		}
	}

	/**
	 * test endsWith(), include exceptional case of null and CompoundName
	 * 
	 * @throws InvalidNameException
	 */
	public void testEndsWith() throws InvalidNameException {
		log.setMethod("testEndsWith()");
		CompositeName end = new CompositeName("");
		assertTrue(name.endsWith(end));
		end = new CompositeName("12345");
		assertFalse(name.endsWith(end));
		try {
			name.endsWith(null);
		} catch (Throwable e) {
			log.log("end with null?", e);
		}
		try {
			assertFalse(name.endsWith(new CompoundName("", props)));
		} catch (Throwable e) {
			log.log("end with compoundName?", e);
		}
	}

	/**
	 * Special characters are as follows: The separator is / The escape
	 * character is \ Quotes can be used - both single quotes and double quotes
	 * are allowed. This allows you to quote strings which contain chars such as /
	 * which are part of a CompositeName element to avoid them being read as a
	 * separator.
	 * 
	 * @throws InvalidNameException
	 */
	public void testSpecialCharacter() throws InvalidNameException {
		log.setMethod("testSpecialCharacter()");
		// The name "a//a" has 3 elements. The middle element is empty and the
		// first & third elements are both "a".
		name = new CompositeName("a//a");
		Enumeration<?> enumeration = name.getAll();
		assertEquals("a", enumeration.nextElement());
		assertEquals("", enumeration.nextElement());
		assertEquals("a", enumeration.nextElement());

		// The name "a/'b/c'/a" has 3 elements. The middle element is b/c.
		name = new CompositeName("a/'b/c'/a");
		enumeration = name.getAll();
		assertEquals("a", enumeration.nextElement());
		assertEquals("b/c", enumeration.nextElement());
		assertEquals("a", enumeration.nextElement());

		name = new CompositeName("a/a'b/c'c/a");
		enumeration = name.getAll();
		assertEquals("a", enumeration.nextElement());
		assertEquals("a'b", enumeration.nextElement());
		assertEquals("c'c", enumeration.nextElement());
		assertEquals("a", enumeration.nextElement());
		name = new CompositeName("a/a'b/c'/a/\\abc/ab\\\"c");
		enumeration = name.getAll();
		assertEquals("a", enumeration.nextElement());
		assertEquals("a'b", enumeration.nextElement());
		assertEquals("c'", enumeration.nextElement());
		assertEquals("a", enumeration.nextElement());
		assertEquals("\\abc", enumeration.nextElement());
		assertEquals("ab\"c", enumeration.nextElement());

		name = new CompositeName("\"ab/c\"/ab");
		assertNameEquals(name, new String[] { "ab/c", "ab" });

		// The name "a/'b/a" is invalid as there is no closing quote for the '
		// character.
		try {
			name = new CompositeName("a/'b/a");
			fail("a/'b/a");
		} catch (InvalidNameException e) {
		}

		// The name "a/\"b/a" is interpreted as a/"b/a and is invalid as there
		// is no closing quote for the embedded escaped " character.
		try {
			name = new CompositeName("a/\"'b/a");
			fail("a/\"'b/a");
		} catch (InvalidNameException e) {
		}
		try {
			name = new CompositeName("a/bcd/a\\");
			fail("a/bcd/a\\");
		} catch (InvalidNameException e) {
		}
	}

	/**
	 * test equals, include exceptional case, i.e, CompoundName, null, etc.
	 * 
	 * @throws InvalidNameException
	 */
	public void testEquals() throws InvalidNameException {
		log.setMethod("testEquals()");
		Name name2 = null;
		name2 = new CompositeName(initName);
		assertTrue(name.equals(name2));
		assertTrue(name2.equals(name));
		name2.add("abc");
		assertFalse(name2.equals(name));
		name2 = new CompoundName("abc", props);
		name = new CompositeName("abc");
		assertFalse(name.equals(name2));
		assertFalse(name.equals(null));
		assertTrue(name.equals(name));
	}

	/**
	 * test compareTo(), include exceptional case, i.e, CompoundName, null, etc.
	 * 
	 * @throws InvalidNameException
	 */
	public void testCompareTo() throws InvalidNameException {
		log.setMethod("testCompareTo()");
		Name name1 = new CompositeName("a/b/c/d");
		Name name2 = new CompositeName("ab/b/c/d");
		Name name3 = new CompositeName("a/b/c/d/e");
		Name name4 = new CompositeName("b/b/c/d");
		assertEquals(-1, name1.compareTo(name2));
		assertEquals(1, name3.compareTo(name1));
		assertEquals(-1, name1.compareTo(name4));
		assertEquals(name1.compareTo(name1), 0);
		try {
			name1.compareTo(null);
			fail();
		} catch (ClassCastException e) {
		}
		name2 = new CompoundName("a/b/c/d", props);
		try {
			name1.compareTo(name2);
			fail();
		} catch (ClassCastException e) {
		}
	}

	/**
	 * test hashCode()
	 * 
	 * @throws InvalidNameException
	 */
	public void testHashCode() throws InvalidNameException {
		log.setMethod("testHashCode()");
		Name name2 = new CompositeName(initName);
		assertEquals(name.hashCode(), name2.hashCode());
		assertEquals(767782430, name2.hashCode());
		name2 = new CompositeName("a");
		assertEquals(97, name2.hashCode());
	}

	/**
	 * test serialization
	 * 
	 * @throws Exception
	 */
	public void testWriteReadObject() throws Exception {
		log.setMethod("testWriteReadObject()");
		CompositeName name = new CompositeName("a/b/c/d");
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(bout);
		stream.writeObject(name);
		stream.close();
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
				bout.toByteArray()));
		CompositeName name2 = (CompositeName) in.readObject();
		assertTrue(name.equals(name2));
		in.close();
	}

	/**
	 * utility method for name assertion
	 * 
	 * @param n
	 * @param elems
	 */
	private void assertNameEquals(Name n, String[] elems) {

		try {
			// compare
			assertEquals(elems.length, n.size());

			for (int i = 0; i < n.size(); i++) {
				assertEquals(elems[i], n.get(i));
			}

			int i = 0;
			Enumeration<?> enumeration = n.getAll();
			while (enumeration.hasMoreElements()) {
				assertEquals(elems[i++], enumeration.nextElement());
			}
		} catch (RuntimeException e) {
			// log
			StringBuffer buf = new StringBuffer();
			buf.append("Assert name ");
			buf.append(n.toString());
			buf.append(" has elements [" + elems.length + "]{");
			for (int i = 0; i < elems.length; i++) {
				if (i > 0) {
					buf.append(",");
				}
				buf.append(elems[i]);
			}
			buf.append("}");
			log.log(buf.toString());

			throw e;
		}
	}

	/**
	 * test serialization compatibility
	 * 
	 * @throws Exception
	 */
	public void testSerializationCompatibility() throws Exception {
		log.setMethod("testSerializationCompatibility()");

		try {
			ObjectInputStream in = new ObjectInputStream(getClass()
                    .getResourceAsStream(
                            "/serialization/javax/naming/CompositeName.ser"));
			CompositeName name = (CompositeName) in.readObject();
			assertEquals(new CompositeName("a/b/c/d"), name);
			in.close();
		} catch (Exception e) {
			log.log(e);
			throw e;
		}
	}

	/**
	 * test clone
	 * 
	 */
	public void testClone() throws InvalidNameException {
		CompositeName name = new CompositeName("a/b/c/d");
		CompositeName name2 = (CompositeName) name.clone();
		assertEquals(name, name2);
		name.add(1, elements[0]);
		assertFalse(name.equals(name2));
	}

	public void testConstructorEnum() {
		log.setMethod("testConstructorEnum");
		CompositeName name2 = new MockCompositeName(name.getAll());
		assertEquals(name2, name);
		try {
			name2 = new MockCompositeName((Enumeration<String>) null);
			fail();
		} catch (NullPointerException e) {
		}
	}

	// mock class to test protected methods
	public class MockCompositeName extends CompositeName {
        private static final long serialVersionUID = 1L;

        public MockCompositeName(Enumeration<String> enumeration) {
			super(enumeration);
		}
	}
}
