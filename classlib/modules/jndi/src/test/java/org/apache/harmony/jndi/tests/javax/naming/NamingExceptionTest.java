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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

/**
 * Tests for NamingException
 */
public class NamingExceptionTest extends TestCase {

	NamingException ex;

	static Log log = new Log(NamingExceptionTest.class);

	/**
	 * Constructor for NamingExceptionTest.
	 * 
	 * @param arg0
	 */
	public NamingExceptionTest(String arg0) {
		super(arg0);
	}

	/*
	 * @see TestCase#setUp()
	 */
	@Override
    protected void setUp() throws Exception {
		super.setUp();
		ex = new NamingException("test message");
	}

	/*
	 * @see TestCase#tearDown()
	 */
	@Override
    protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testAppendRemainingComponent() throws InvalidNameException {
		log.setMethod("testAppendRemainingComponent");
		ex.appendRemainingComponent("element");
		assertEquals(new CompositeName("element"), ex.getRemainingName());

		ex.setRemainingName(new CompositeName("harmony/apache/org"));
		ex.appendRemainingComponent("element");
		assertEquals("harmony/apache/org/element", ex.getRemainingName()
				.toString());
	}

	public void testAppendRemainingName() throws InvalidNameException {
		log.setMethod("testAppendRemainingName");
		Name name = new CompositeName("element/parent");
		ex.appendRemainingName(name);
		assertEquals(name, ex.getRemainingName());
		assertNotSame(name, ex.getRemainingName());
		ex.setRemainingName(new CompositeName("harmony/apache/org"));
		ex.appendRemainingName(new CompositeName("element/parent"));
		assertEquals(new CompositeName("harmony/apache/org/element/parent"), ex
				.getRemainingName());
	}

	public void testSetResolvedName() throws InvalidNameException {
		log.setMethod("testSetResolvedName");
		Name name = new CompositeName("a/b/c");
		ex.setResolvedName(name);
		assertEquals(name, ex.getResolvedName());
		assertNotSame(name, ex.getResolvedName());
	}

	public void testSetResolvedObj() {
		log.setMethod("testSetResolvedObj");
		Object o = new Object();
		ex.setResolvedObj(o);
		assertEquals(o, ex.getResolvedObj());
		assertSame(o, ex.getResolvedObj());
	}

	/*
	 * Test for String toString()
	 */
	public void testToString() throws InvalidNameException {
		String str1 = "test message";
		String remainName = "harmony/apache/org";
		String rootCause = "root cause1";
		String resolvedName = "element/parent";
		String resovledObj = "this is resolved object";
		log.setMethod("testToString");

		assertTrue(ex.toString().indexOf(str1) > 0);

		ex.setRemainingName(new CompositeName(remainName));
		String str = ex.toString();
		assertTrue(0 < str.indexOf(str1)
				&& str.indexOf(str1) < str.indexOf(remainName));

		ex.setRootCause(new InvalidNameException(rootCause));
		str = ex.toString();
		assertTrue(0 < str.indexOf(str1)
				&& str.indexOf(str1) < str.indexOf(rootCause)
				&& str.indexOf(rootCause) < str.indexOf(remainName));

		ex.setResolvedName(new CompositeName(resolvedName));
		str = ex.toString();
		assertTrue(0 < str.indexOf(str1)
				&& str.indexOf(str1) < str.indexOf(rootCause)
				&& str.indexOf(rootCause) < str.indexOf(remainName));
		assertTrue(0 > str.indexOf(resolvedName));

		ex.setResolvedObj(resovledObj);
		str = ex.toString();
		assertTrue(0 < str.indexOf(str1)
				&& str.indexOf(str1) < str.indexOf(rootCause)
				&& str.indexOf(rootCause) < str.indexOf(remainName));
		assertTrue(0 > str.indexOf(resovledObj));

	}

	/*
	 * Test for String toString(boolean)
	 */
	public void testToStringboolean() throws InvalidNameException {
		String str1 = "test message";
		String remainName = "harmony/apache/org";
		String rootCause = "root cause1";
		String resolvedName = "element/parent";
		String resovledObj = "this is resolved object";
		log.setMethod("testToStringboolean");

		String str = ex.toString(true);
		assertTrue(str.indexOf(str1) > 0);

		ex.setRemainingName(new CompositeName(remainName));
		str = ex.toString(true);
		assertTrue(0 < str.indexOf(str1)
				&& str.indexOf(str1) < str.indexOf(remainName));

		ex.setRootCause(new InvalidNameException(rootCause));
		str = ex.toString(true);
		assertTrue(0 < str.indexOf(str1)
				&& str.indexOf(str1) < str.indexOf(rootCause)
				&& str.indexOf(rootCause) < str.indexOf(remainName));

		ex.setResolvedName(new CompositeName(resolvedName));
		str = ex.toString(true);
		assertTrue(0 < str.indexOf(str1)
				&& str.indexOf(str1) < str.indexOf(rootCause)
				&& str.indexOf(rootCause) < str.indexOf(remainName));
		assertTrue(0 > str.indexOf(resolvedName));

		ex.setResolvedObj(resovledObj);
		str = ex.toString(true);
		assertTrue(0 < str.indexOf(str1)
				&& str.indexOf(str1) < str.indexOf(rootCause)
				&& str.indexOf(rootCause) < str.indexOf(remainName)
				&& str.indexOf(remainName) < str.indexOf(resovledObj));
	}

	public void testSerializationCompatibility() throws Exception {
		log.setMethod("testSerializationCompatibility()");

		try {
			ObjectInputStream in = new ObjectInputStream(getClass()
                    .getResourceAsStream(
                            "/serialization/javax/naming/NamingException.ser"));
			NamingException ex = (NamingException) in.readObject();
			assertEquals("test purpsoe", ex.getMessage());
			assertEquals(new CompositeName("RemainingName"), ex
					.getRemainingName());
			assertEquals(new CompositeName("RemainingName"), ex
					.getResolvedName());
			assertEquals(new Integer(1), ex.getResolvedObj());
			assertEquals("root exception", ex.getRootCause().getMessage());
			in.close();
		} catch (Exception e) {
			log.log(e);
			throw e;
		}
	}

	public void testPrintStackTrace() throws InvalidNameException, IOException {
		log.setMethod("testPrintStackTrace");
		ex.setRemainingName(new CompositeName("element/parent"));
		ex.setResolvedName(new CompositeName("remained name"));
		ex.setResolvedObj("resolved obj");
		PrintStream stdErr = System.err;
		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(bStream);
		System.setErr(stream);

		ex.printStackTrace();
		String trace1 = "element/parent";
		String trace2 = "root cause1";
		String str = new String(bStream.toByteArray());
		assertTrue(str.indexOf(trace1) > 0);

		bStream.reset();

		ex.printStackTrace(stream);
		str = new String(bStream.toByteArray());
		assertTrue(str.indexOf(trace1) > 0);
		bStream.reset();

		ex.printStackTrace(new PrintWriter(stream, true));
		str = new String(bStream.toByteArray());
		assertTrue(str.indexOf(trace1) > 0);
		bStream.reset();

		ex.setRootCause(new Exception(trace2));
		ex.printStackTrace();
		str = new String(bStream.toByteArray());
		assertTrue(str.indexOf(trace1) > 0);
		assertTrue(str.indexOf(trace2) > 0);
		bStream.reset();

		ex.printStackTrace(stream);
		str = new String(bStream.toByteArray());
		assertTrue(str.indexOf(trace1) > 0);
		assertTrue(str.indexOf(trace2) > 0);
		bStream.reset();

		ex.printStackTrace(new PrintWriter(stream, true));
		str = new String(bStream.toByteArray());
		assertTrue(str.indexOf(trace1) > 0);
		assertTrue(str.indexOf(trace2) > 0);
		bStream.reset();

		System.setErr(stdErr);
		stream.close();
	}

	public void testSetRemainingName() {
		NamingException exception = new NamingException("Test");
		exception.setRemainingName(null);
		assertNull(exception.getRemainingName());
	}

	public void testSetResolvedName2() {
		NamingException exception = new NamingException("Test");
		exception.setResolvedName(null);
		assertNull(exception.getResolvedName());
	}
}
