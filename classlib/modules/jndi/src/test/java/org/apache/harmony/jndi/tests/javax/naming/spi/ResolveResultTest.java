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
package org.apache.harmony.jndi.tests.javax.naming.spi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;

import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.spi.ResolveResult;

import junit.framework.TestCase;

public class ResolveResultTest extends TestCase {

	private String strObj;

	private String strName;

	private CompositeName name;

	@Override
    protected void setUp() throws InvalidNameException {
		strObj = "String object";
		strName = "www.eclipse.org/org/index.html";
		name = new CompositeName(strName);
	}

	public void testConstructor_NoParms() {
		MyResolveResult resolveResult = new MyResolveResult();
		assertNull(resolveResult.getResolvedObj());
		assertNull(resolveResult.getRemainingName());
	}

	public void tsetConstructor_Simple() throws InvalidNameException {
		CompositeName expectedName = new CompositeName(strName);
		ResolveResult resolveResult = new ResolveResult(strObj, strName);

		assertEquals(strObj, resolveResult.getResolvedObj());
		assertEquals(expectedName, resolveResult.getRemainingName());
	}

	public void testConstructor_SimpleNull() {
		strName = null;
		try {
			new ResolveResult(null, strName);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testConstructor_ByName() {
		ResolveResult resolveResult = new ResolveResult(strObj, strName);

		assertEquals(strObj, resolveResult.getResolvedObj());
		assertEquals(name, resolveResult.getRemainingName());
		// TO DO R: Is name is cloned? Confirm: yes.
		assertSame(strObj, resolveResult.getResolvedObj());
		assertNotSame(name, resolveResult.getRemainingName());
	}

	public void testConstructor_ByNameNull() {
		/*
		 * try { ResolveResult resolveResult = new ResolveResult(strObj,
		 * (Name)null); fail("Should throw NullPointerException."); } catch
		 * (NullPointerException e) { }
		 */

		ResolveResult resolveResult = new ResolveResult(strObj, (Name) null);
		assertNull(resolveResult.getRemainingName());
		assertSame(strObj, resolveResult.getResolvedObj());
	}

	public void testConstructor_ByNameObjectNull() {
		ResolveResult resolveResult = new ResolveResult(null, name);
		assertNull(resolveResult.getResolvedObj());
	}

	public void testConstructor_InvalidName() {
		strName = "a/'a/b/b";
		ResolveResult resolveResult = new ResolveResult(strObj, strName);

		assertNull(resolveResult.getRemainingName());
		assertEquals(strObj, resolveResult.getResolvedObj());
	}

	public void testConstrcutor_ByCompoundName() throws InvalidNameException {
		Properties props = new Properties();
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		CompoundName compoundName = new CompoundName("a", props);

		ResolveResult resolveResult = new ResolveResult(strObj, compoundName);
		assertEquals(strObj, resolveResult.getResolvedObj());
		assertEquals(compoundName, resolveResult.getRemainingName());
	}

	public void testAppendRemainingComponent() throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, name);
		String nameComponent = "abc";
		resolveResult.appendRemainingComponent(nameComponent);

		assertEquals(strObj, resolveResult.getResolvedObj());
		name.add(nameComponent);
		assertEquals(name, resolveResult.getRemainingName());
	}

	public void testAppendRemainingComponent_Null() {
		ResolveResult resolveResult = new ResolveResult(strObj, name);
		resolveResult.appendRemainingComponent(null);

		assertEquals(strObj, resolveResult.getResolvedObj());
		assertEquals(name, resolveResult.getRemainingName());
	}

	public void testAppendRemainingComponent_InvalidName()
			throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, name);
		String nameComponent = "a/'a/b/b";
		resolveResult.appendRemainingComponent(nameComponent);

		assertEquals(strObj, resolveResult.getResolvedObj());
		name.add(nameComponent);

		assertEquals(name, resolveResult.getRemainingName());
		// Impossible to throw exception
	}

	public void testAppendRemainingComponent_NullRemainingName()
			throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, (Name) null);
		String nameComponent = "a/'a/b'/b";
		CompositeName newName = new CompositeName();
		newName.add(nameComponent);
		resolveResult.appendRemainingComponent(nameComponent);

		assertEquals(newName, resolveResult.getRemainingName());
	}

	public void testAppendRemainingName() throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, name);

		CompositeName newName = new CompositeName("a/b/c/d");
		resolveResult.appendRemainingName(newName);

		assertEquals(strObj, resolveResult.getResolvedObj());
		name.addAll(newName);
		assertEquals(name, resolveResult.getRemainingName());
	}

	public void testAppendRemainingName_Null() {
		ResolveResult resolveResult = new ResolveResult(strObj, name);
		resolveResult.appendRemainingName(null);

		assertEquals(strObj, resolveResult.getResolvedObj());
		assertEquals(name, resolveResult.getRemainingName());
	}

	public void testAppendRemainingName_InvalidName()
			throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, name);

		Properties props = new Properties();
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		CompoundName compoundName = new CompoundName("a", props);
		try {
			resolveResult.appendRemainingName(compoundName);
			fail("Should throw a Error.");
		} catch (Error e) {
		}
	}

	public void testAppendRemainingName_withCompoundName()
			throws InvalidNameException {
		Properties props = new Properties();
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		CompoundName compoundName = new CompoundName("a", props);

		ResolveResult resolveResult = new ResolveResult(strObj, compoundName);
		CompositeName newName = new CompositeName("a/b/c/d");
		try {
			resolveResult.appendRemainingName(newName);
			fail("Should throw a Error here.");
		} catch (Error e) {
		}

		// compoundName.addAll(newName);
		// assertEquals(compoundName, resolveResult.getRemainingName());
	}

	public void testAppendRemainingName_withCompoundName2()
			throws InvalidNameException {
		Properties props = new Properties();
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		CompoundName compoundName = new CompoundName("a", props);

		ResolveResult resolveResult = new ResolveResult(strObj, compoundName);
		CompoundName newName = new CompoundName("b", props);
		resolveResult.appendRemainingName(newName);

		compoundName.addAll(newName);
		assertEquals(compoundName, resolveResult.getRemainingName());
	}

	public void testAppendRemainingName_NullRemainingName()
			throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, (Name) null);
		String nameComponent = "a/'a/b'/b";
		CompositeName newName = new CompositeName();
		newName.add(nameComponent);
		resolveResult.appendRemainingName(newName);

		assertEquals(newName, resolveResult.getRemainingName());
		assertNotSame(newName, resolveResult.getRemainingName());
	}

	public void testSetRemainingName() throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, name);
		name.add("1/2/3/4");
		resolveResult.setRemainingName(name);

		assertNotSame(name, resolveResult.getRemainingName());
		assertEquals(strObj, resolveResult.getResolvedObj());
		assertEquals(name, resolveResult.getRemainingName());
		name.remove(name.size() - 1);
		assertFalse(name.equals(resolveResult.getRemainingName()));
	}

	public void testSetRemainingName_Null() {
		ResolveResult resolveResult = new ResolveResult(strObj, name);
		resolveResult.setRemainingName(null);
		assertNull(resolveResult.getRemainingName());
	}

	public void testSetRemainingName_InvalidName() throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, name);

		Properties props = new Properties();
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		CompoundName compoundName = new CompoundName("a", props);

		resolveResult.setRemainingName(compoundName);
		assertEquals(compoundName, resolveResult.getRemainingName());
	}

	public void testSetRemainingName_withCompoundName()
			throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, name);

		Properties props = new Properties();
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		CompoundName compoundName = new CompoundName("a", props);

		resolveResult.setRemainingName(compoundName);

		CompositeName newName = new CompositeName("a/b/c/d");
		try {
			resolveResult.appendRemainingName(newName);
			fail("Should throw a Error here");
		} catch (Error e) {
		}
		// compoundName.addAll(newName);
		// assertEquals(compoundName, resolveResult.getRemainingName());
	}

	public void testSetRemainingName_withCompoundName2()
			throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, name);

		Properties props = new Properties();
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		CompoundName compoundName = new CompoundName("a", props);

		resolveResult.setRemainingName(compoundName);

		CompoundName newName = new CompoundName("b", props);
		resolveResult.appendRemainingName(newName);

		compoundName.addAll(newName);
		assertEquals(compoundName, resolveResult.getRemainingName());
	}

	public void testSetResolvedObj() {
		ResolveResult resolveResult = new ResolveResult(strObj, name);
		Integer intObj = new Integer(123456);
		resolveResult.setResolvedObj(intObj);

		assertEquals(intObj, resolveResult.getResolvedObj());
		assertSame(intObj, resolveResult.getResolvedObj());
	}

	public void testGetRemainingName() throws InvalidNameException {
		CompositeName expectedName = new CompositeName(strName);
		ResolveResult resolveResult = new ResolveResult(strObj, strName);

		assertEquals(expectedName, resolveResult.getRemainingName());
	}

	public void testGetResolvedObj() throws InvalidNameException {
		ResolveResult resolveResult = new ResolveResult(strObj, strName);
		assertEquals(strObj, resolveResult.getResolvedObj());
	}

	public void testSerializable_Simple() throws ClassNotFoundException,
			IOException {
		ResolveResult resolveResult = new ResolveResult(strObj, strName);

		// write to byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(resolveResult);
		byte[] buffer = baos.toByteArray();
		oos.close();
		baos.close();

		// read from byte array
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
		ObjectInputStream ois = new ObjectInputStream(bais);
		ResolveResult resolveResult2 = (ResolveResult) ois.readObject();
		ois.close();
		bais.close();

		assertEquals(resolveResult.getResolvedObj(), resolveResult2
				.getResolvedObj());
		assertEquals(resolveResult.getRemainingName(), resolveResult2
				.getRemainingName());
	}

	public void testSerializable_compatibility() throws ClassNotFoundException,
			IOException {
		ObjectInputStream ois = new ObjectInputStream(getClass()
                .getClassLoader().getResourceAsStream(
                        "/serialization/javax/naming/spi/ResolveResult.ser"));
		ResolveResult resolveResult2 = (ResolveResult) ois.readObject();
		ois.close();

		ResolveResult resolveResult = new ResolveResult(strObj, strName);

		assertEquals(resolveResult.getResolvedObj(), resolveResult2
				.getResolvedObj());
		assertEquals(resolveResult.getRemainingName(), resolveResult2
				.getRemainingName());
	}

	class MyResolveResult extends ResolveResult {
		/**
         * <p></p>
         */
        private static final long serialVersionUID = 1L;

        public MyResolveResult() {
			super();
		}
	}
}
