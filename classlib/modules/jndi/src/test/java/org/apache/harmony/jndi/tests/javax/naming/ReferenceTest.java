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
import java.util.Enumeration;
import java.util.Random;

import javax.naming.BinaryRefAddr;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import junit.framework.TestCase;

public class ReferenceTest extends TestCase {

	private Reference ref;

	private byte[] buffer;

	@Override
    protected void setUp() {
		int bufferLen = 50;
		buffer = new byte[bufferLen];
		Random random = new Random();
		for (int i = 0; i < bufferLen; i++) {
			buffer[i] = (byte) random.nextInt(0x100);
		}

		String className = "java.util.Hashtable";
		ref = new Reference(className);
	}

	/**
	 * test create Reference using a className
	 */
	public void testConstructor_Simple() {
		String className = "java.util.Hashtable";
		Reference reference = new Reference(className);

		assertEquals(className, reference.getClassName());
		assertNull(reference.getFactoryClassName());
		assertNull(reference.getFactoryClassLocation());
		assertEquals(0, reference.size());
	}

	public void testConstructor_SimpleNull() {
		Reference reference = new Reference(null);

		assertNull(reference.getClassName());
		assertEquals(0, reference.size());
	}

	public void testConstructor_ByRefAddr() {
		String className = "java.util.Hashtable";
		String type = "Binary";
		RefAddr refAddr = new BinaryRefAddr(type, buffer);
		Reference reference = new Reference(className, refAddr);

		assertEquals(className, reference.getClassName());
		assertEquals(refAddr, reference.get(0));
		assertNull(reference.getFactoryClassName());
		assertNull(reference.getFactoryClassLocation());
		assertEquals(1, reference.size());
	}

	public void testConstructor_ByRefAddrNull() {
		Reference reference = new Reference(null, null);

		assertNull(reference.getClassName());
		assertNull(reference.getFactoryClassName());
		assertNull(reference.getFactoryClassLocation());
		assertNull(reference.get(0));
		assertEquals(1, reference.size());
	}

	public void testConstructor_ByFactory() {
		String className = "java.util.Hashtable";
		String factoryName = "factory name";
		String factoryLocation = "file:///home/";
		Reference reference = new Reference(className, factoryName,
				factoryLocation);

		assertEquals(className, reference.getClassName());
		assertEquals(factoryName, reference.getFactoryClassName());
		assertEquals(factoryLocation, reference.getFactoryClassLocation());
		assertEquals(0, reference.size());
	}

	public void testConstructor_ByFactoryNull() {
		Reference reference = new Reference(null, null, null);

		assertNull(reference.getClassName());
		assertNull(reference.getFactoryClassName());
		assertNull(reference.getFactoryClassLocation());
		assertEquals(0, reference.size());
	}

	public void testConstructor_Full() {
		String className = "java.util.Hashtable";
		String factoryName = "factory name";
		String factoryLocation = "file:///home/";

		String type = "Binary";
		RefAddr refAddr = new BinaryRefAddr(type, buffer);

		Reference reference = new Reference(className, refAddr, factoryName,
				factoryLocation);

		assertEquals(className, reference.getClassName());
		assertEquals(factoryName, reference.getFactoryClassName());
		assertEquals(factoryLocation, reference.getFactoryClassLocation());
		assertEquals(1, reference.size());
		assertEquals(refAddr, reference.get(0));
	}

	public void testConstructor_FullNull() {

		Reference reference = new Reference(null, null, null, null);

		assertNull(reference.getClassName());
		assertNull(reference.getFactoryClassName());
		assertNull(reference.getFactoryClassLocation());
		assertNull(reference.get(0));
		assertEquals(1, reference.size());
	}

	public void testAdd_Simple() {
		String type = "Binary";
		BinaryRefAddr refAddr0 = new BinaryRefAddr(type, buffer);
		byte[] buffer1 = { 1, 2, 3, 4 };
		BinaryRefAddr refAddr1 = new BinaryRefAddr(type, buffer1);
		ref.add(refAddr0);
		ref.add(refAddr1);

		assertEquals(2, ref.size());
		assertEquals(refAddr0, ref.get(0));
		assertEquals(refAddr1, ref.get(1));
	}

	public void testAdd_SimpleNull() {
		ref.add(null);

		assertEquals(1, ref.size());
		assertNull(ref.get(0));
	}

	public void testAdd_ByIndex() {
		String type = "Binary";
		BinaryRefAddr refAddr0 = new BinaryRefAddr(type, buffer);
		byte[] buffer1 = { 1, 2, 3, 4 };
		BinaryRefAddr refAddr1 = new BinaryRefAddr(type, buffer1);
		ref.add(0, refAddr0);
		ref.add(1, refAddr1);

		assertEquals(2, ref.size());
		assertEquals(refAddr0, ref.get(0));
		assertEquals(refAddr1, ref.get(1));
	}

	public void testAdd_ByIndexInsert() {
		String type = "Binary";
		BinaryRefAddr refAddr0 = new BinaryRefAddr(type, buffer);
		byte[] buffer1 = { 1, 2, 3, 4 };
		BinaryRefAddr refAddr1 = new BinaryRefAddr(type, buffer1);
		byte[] buffer2 = { 1, 2, 3, 4, 5 };
		BinaryRefAddr refAddr2 = new BinaryRefAddr(type, buffer2);

		ref.add(0, refAddr0);
		ref.add(1, refAddr1);
		ref.add(1, refAddr2);

		assertEquals(3, ref.size());
		assertEquals(refAddr0, ref.get(0));
		assertEquals(refAddr2, ref.get(1));
	}

	public void testAdd_ByIndexInvalidGreat() {
		String type = "Binary";
		BinaryRefAddr refAddr = new BinaryRefAddr(type, buffer);
		try {
			ref.add(1, refAddr);
			fail("This should throw a ArrayIndexOutOfBoundsException");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testAdd_ByIndexInvalidLess() {
		String type = "Binary";
		BinaryRefAddr refAddr = new BinaryRefAddr(type, buffer);
		try {
			ref.add(-1, refAddr);
			fail("This should throw a ArrayIndexOutOfBoundsException");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testGet_SimpleInvalidGreat() {
		String type = "Binary";
		BinaryRefAddr refAddr = new BinaryRefAddr(type, buffer);
		ref.add(refAddr);

		try {
			ref.get(ref.size());
			fail("This should throw a ArrayIndexOutOfBoundsException");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testGet_SimpleInvalidLess() {
		String type = "Binary";
		BinaryRefAddr refAddr = new BinaryRefAddr(type, buffer);
		ref.add(refAddr);

		try {
			ref.get(-1);
			fail("This should throw a ArrayIndexOutOfBoundsException");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testGet_ByType() {
		String[] types = { "Binary", "String", };

		byte[][] buffers = { { 1, 2, 3, 4 }, { 5, 6, 7, 8 }, };

		BinaryRefAddr[] refAddrs = new BinaryRefAddr[types.length];

		for (int i = 0; i < types.length; i++) {
			refAddrs[i] = new BinaryRefAddr(types[i], buffers[i]);
			ref.add(refAddrs[i]);
		}

		for (int i = 0; i < types.length; i++) {
			assertEquals(refAddrs[i], ref.get(types[i]));
		}
	}

	public void testGet_ByTypeNotExist() {
		String type = "Binary";
		BinaryRefAddr refAddr = new BinaryRefAddr(type, buffer);
		ref.add(refAddr);

		assertNull(ref.get("String"));
	}

	public void testGet_TypeNull() {
		String type = "Binary";
		BinaryRefAddr refAddr = new BinaryRefAddr(type, buffer);
		ref.add(refAddr);
		try {
			ref.get(null);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testGetAll_Simple() {
		String type = "Binary";
		BinaryRefAddr refAddr = new BinaryRefAddr(type, buffer);
		ref.add(refAddr);

		Enumeration<?> allAddrs = ref.getAll();
		assertTrue(allAddrs.hasMoreElements());
		assertEquals(refAddr, allAddrs.nextElement());
	}

	public void testGetAll_Empty() {
		Enumeration<?> allAddrs = ref.getAll();
		assertFalse(allAddrs.hasMoreElements());
	}

	public void testRemove_Simple() {
		String type = "Binary";
		BinaryRefAddr refAddr = new BinaryRefAddr(type, buffer);
		ref.add(refAddr);

		assertEquals(1, ref.size());

		assertEquals(ref.remove(0), refAddr);

		assertEquals(0, ref.size());
	}

	public void testRemove_Invalid() {
		try {
			ref.remove(0);
			fail("This should throw a ArrayIndexOutOfBoundsException");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testClear_Simple() {
		String type = "Binary";
		BinaryRefAddr refAddr = new BinaryRefAddr(type, buffer);
		int count = 10;
		for (int i = 0; i < count; i++) {
			ref.add(refAddr);
		}
		assertEquals(count, ref.size());
		ref.clear();
		assertEquals(0, ref.size());
	}

	public void testClear_Empty() {
		ref.clear();
		assertEquals(0, ref.size());
	}

	public void testEquals_Simple() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";

		Reference reference0 = new Reference(className, classFactory, location);
		Reference reference1 = new Reference(className, classFactory, location);
		assertTrue(reference0.equals(reference1));
		assertTrue(reference0.equals(reference0));
		assertTrue(reference1.equals(reference0));
		assertFalse(reference0.equals(null));
	}

	public void testEquals_SimpleWithStrAddr() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";
		StringRefAddr addr = new StringRefAddr("String address",
				"this is a string");
		Reference reference0 = new Reference(className, addr, classFactory,
				location);
		Reference reference1 = new Reference(className, addr, classFactory,
				location);
		assertTrue(reference0.equals(reference1));
		assertTrue(reference0.equals(reference0));
		assertTrue(reference1.equals(reference0));
		assertFalse(reference0.equals(null));
	}

	public void testEquals_IgnoreFactory() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";
		StringRefAddr addr = new StringRefAddr("String address",
				"this is a string");
		Reference reference0 = new Reference(className, addr, classFactory,
				location);
		Reference reference1 = new Reference(className, addr, "", location);
		assertTrue(reference0.equals(reference1));
	}

	public void testEquals_IgnoreFactoryLocation() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";
		StringRefAddr addr = new StringRefAddr("String address",
				"this is a string");
		Reference reference0 = new Reference(className, addr, classFactory,
				location);
		Reference reference1 = new Reference(className, addr, classFactory, "");
		assertTrue(reference0.equals(reference1));
	}

	public void testEquals_NotEquals1() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";
		StringRefAddr addr = new StringRefAddr("String address",
				"this is a string");
		Reference reference0 = new Reference(className, addr, classFactory,
				location);
		Reference reference1 = new Reference("java.lang.StringBuffer", addr,
				classFactory, location);
		assertFalse(reference0.equals(reference1));
	}

	public void testEquals_NotEquals2() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";
		StringRefAddr addr = new StringRefAddr("String address",
				"this is a string");
		BinaryRefAddr addr1 = new BinaryRefAddr("Binary address", new byte[] {
				1, 2, 3, 4, 5 });
		Reference reference0 = new Reference(className, addr, classFactory,
				location);
		Reference reference1 = new Reference(className, addr1, classFactory,
				location);
		assertFalse(reference0.equals(reference1));
	}

	public void testEquals_NotInstance() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";

		Reference reference0 = new Reference(className, classFactory, location);
		assertFalse(reference0.equals("reference"));
	}

	public void testEquals_NullClassName() {
		String classFactory = "class factory";
		String location = "/home/neuser";

		Reference reference0 = new Reference(null, classFactory, location);
		Reference reference1 = new Reference(null, classFactory, location);

		try {
			reference0.equals(reference1);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testEquals_NullClassName2() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";

		Reference reference0 = new Reference(null, classFactory, location);
		Reference reference2 = new Reference(className, classFactory, location);

		// try {
		assertFalse(reference0.equals(reference2));
		// fail("Should throw NullPointerException.");
		// } catch (NullPointerException e) {
		// }
	}

	public void testEquals_NullClassName3() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";

		Reference reference0 = new Reference(null, classFactory, location);
		Reference reference2 = new Reference(className, classFactory, location);

		try {
			reference2.equals(reference0);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testHashcode_Simple() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";
		StringRefAddr addr0 = new StringRefAddr("String address",
				"this is a string");
		StringRefAddr addr1 = new StringRefAddr("String address",
				"this is another string");
		Reference reference = new Reference(className, addr0, classFactory,
				location);
		reference.add(addr1);
		assertEquals(
				className.hashCode() + addr0.hashCode() + addr1.hashCode(),
				reference.hashCode());
	}

	public void testHashcode_AddressNull() {
		String className = "java.lang.String";
		Reference reference = new Reference(className);
		assertEquals(className.hashCode(), reference.hashCode());
	}

	public void testToString_Simple() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";
		StringRefAddr addr0 = new StringRefAddr("String address",
				"this is a string");
		StringRefAddr addr1 = new StringRefAddr("String address",
				"this is another string");
		Reference reference = new Reference(className, addr0, classFactory,
				location);
		reference.add(addr1);

		/*
		 * assertEquals( "Reference class name: " + className + "\nReference
		 * addresses:\n\t" + addr0.toString() + "\n\t" + addr1.toString() +
		 * "\n", reference.toString());
		 */
		assertNotNull(reference.toString());
        assertEquals("Reference Class Name: " + reference.getClassName() + "\n"
                + addr0.toString() + addr1.toString(), reference.toString());
    }

	public void testToString_AddressNull() {
		String className = "java.lang.String";
		Reference reference = new Reference(className);
		/*
		 * assertEquals( "Reference class name: " + className + "\nReference
		 * addresses:\n", reference.toString());
		 */
		assertNotNull(reference.toString());
        assertEquals(
                "Reference Class Name: " + reference.getClassName() + "\n",
                reference.toString());
    }

	public void testClone_Simple() {
		String className = "java.lang.String";
		String classFactory = "class factory";
		String location = "/home/neuser";
		StringRefAddr addr = new StringRefAddr("String address",
				"this is a string");
		Reference reference = new Reference(className, addr, classFactory,
				location);

		Reference cloneRef = (Reference) reference.clone();
		assertEquals(reference, cloneRef);
		assertNotSame(reference, cloneRef);
	}

	public void testClone_AddressNull() {
		String className = "java.lang.String";
		Reference reference = new Reference(className);

		Reference cloneRef = (Reference) reference.clone();
		assertEquals(reference, cloneRef);
		assertNotSame(reference, cloneRef);
	}

	public void testClone_DiffAddress() {
		String className = "java.lang.String";
		StringRefAddr addr = new StringRefAddr("string address", "/home/neuser");
		Reference reference = new Reference(className);
		reference.add(addr);
		Reference cloneRef = (Reference) reference.clone();
		reference.clear();
		assertFalse(reference.equals(cloneRef));
	}

	public void testSerializable_Simple() throws ClassNotFoundException,
			IOException {
		Reference reference = new Reference("dazzle.naming.Reference",
				"dazzle.naming.factory.RefFactory", "http://www.apache.org");
		StringRefAddr addr = new StringRefAddr("StringRefAddr",
				"This is a String RefAddr.");
		BinaryRefAddr addr2 = new BinaryRefAddr("BinaryRefAddr", new byte[] {
				'a', 'b', 'c' });
		reference.add(addr);
		reference.add(addr2);

		// write to byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(reference);
		byte[] buffer = baos.toByteArray();
		oos.close();
		baos.close();

		// read from byte array
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
		ObjectInputStream ois = new ObjectInputStream(bais);
		Reference reference2 = (Reference) ois.readObject();
		ois.close();
		bais.close();

		assertEquals(reference, reference2);
	}

	public void testSerializable_compatibility() throws ClassNotFoundException,
			IOException {
		ObjectInputStream ois = new ObjectInputStream(getClass()
                .getClassLoader().getResourceAsStream(
                        "/serialization/javax/naming/Reference.ser"));
		Reference reference2 = (Reference) ois.readObject();
		ois.close();

		Reference reference = new Reference("dazzle.naming.Reference",
				"dazzle.naming.factory.RefFactory", "http://www.apache.org");
		StringRefAddr addr = new StringRefAddr("StringRefAddr",
				"This is a String RefAddr.");
		BinaryRefAddr addr2 = new BinaryRefAddr("BinaryRefAddr", new byte[] {
				'a', 'b', 'c' });
		reference.add(addr);
		reference.add(addr2);

		assertEquals(reference, reference2);
	}
}