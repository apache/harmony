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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.NoInitialContextException;

import junit.framework.TestCase;

public class NoInitialContextExceptionTest extends TestCase {

	/**
	 * Test serialize NoInitialContextException: write a
	 * NoInitialContextException object into a byte array, and read from it. The
	 * two objects should be equal.
	 */
	public void testSerializable_Simple() throws ClassNotFoundException,
			IOException, InvalidNameException {

		NoInitialContextException exception = new NoInitialContextException(
				"Test exception Serializable: NoInitialContextException");
		exception.setRemainingName(new CompositeName(
				"www.apache.org/foundation"));
		exception.setResolvedName(new CompositeName(
				"http://www.apache.org/index.html"));
		exception.setResolvedObj("This is a string object.");
		exception.setRootCause(new NullPointerException("null pointer"));

		// write to byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(exception);
		byte[] buffer = baos.toByteArray();
		oos.close();
		baos.close();

		// read from byte array
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
		ObjectInputStream ois = new ObjectInputStream(bais);
		NoInitialContextException exception2 = (NoInitialContextException) ois
				.readObject();
		ois.close();
		bais.close();

		assertEquals(exception.getExplanation(), exception2.getExplanation());
		assertEquals(exception.getResolvedObj(), exception2.getResolvedObj());
		assertEquals(exception.getRemainingName(), exception2
				.getRemainingName());
		assertEquals(exception.getResolvedName(), exception2.getResolvedName());
		assertEquals(exception.getRootCause().getMessage(), exception2
				.getRootCause().getMessage());
		assertEquals(exception.getRootCause().getClass(), exception2
				.getRootCause().getClass());
	}

	/**
	 * Test InvalidNameException serialization compatibility
	 */
	public void testSerializable_compatibility() throws InvalidNameException,
			ClassNotFoundException, IOException {
		ObjectInputStream ois = new ObjectInputStream(
                getClass()
                        .getClassLoader()
                        .getResourceAsStream(
                                "/serialization/javax/naming/NoInitialContextException.ser"));
		NoInitialContextException exception2 = (NoInitialContextException) ois
				.readObject();
		ois.close();

		NoInitialContextException exception = new NoInitialContextException(
				"Test exception Serializable: NoInitialContextException");
		exception.setRemainingName(new CompositeName("www.apache.org/foundation"));
		exception.setResolvedName(new CompositeName(
				"http://www.apache.org/index.html"));
		exception.setResolvedObj("This is a string object.");
		exception.setRootCause(new NullPointerException("null pointer"));

		assertEquals(exception.getExplanation(), exception2.getExplanation());
		assertEquals(exception.getResolvedObj(), exception2.getResolvedObj());
		assertEquals(exception.getRemainingName(), exception2
				.getRemainingName());
		assertEquals(exception.getResolvedName(), exception2.getResolvedName());
		assertEquals(exception.getRootCause().getMessage(), exception2
				.getRootCause().getMessage());
		assertEquals(exception.getRootCause().getClass(), exception2
				.getRootCause().getClass());
	}
}
