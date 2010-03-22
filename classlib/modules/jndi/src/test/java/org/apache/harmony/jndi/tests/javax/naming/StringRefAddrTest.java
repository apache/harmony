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

import javax.naming.StringRefAddr;

import junit.framework.TestCase;

public class StringRefAddrTest extends TestCase {

	public void testConstructor_Simple() {
		String type = "StringAddr";
		String address = "/home/neuser";
		StringRefAddr addr = new StringRefAddr(type, address);
		assertEquals(type, addr.getType());
		assertEquals(address, addr.getContent());
	}

	public void testConstructor_AddressNull() {
		String type = "StringAddr";
		StringRefAddr addr = new StringRefAddr(type, null);
		assertEquals(type, addr.getType());
		assertNull(addr.getContent());
	}

	public void testGetType_Normal() {
		StringRefAddr addr = new StringRefAddr("type", "content");
		assertEquals("type", addr.getType());
	}

	public void testGetType_Null() {
		StringRefAddr addr = new StringRefAddr(null, "content");
		assertNull(addr.getType());
	}

	public void testGetContent_Normal() {
		StringRefAddr addr = new StringRefAddr("type", "content");
		assertEquals("content", addr.getContent());
	}

	public void testGetContent_Null() {
		StringRefAddr addr = new StringRefAddr("type", null);
		assertNull(addr.getContent());
	}

	public void testConstructor_Null() {
		StringRefAddr addr = new StringRefAddr(null, null);
		assertNull(addr.getType());
		assertNull(addr.getContent());
	}

	public void testEquals_Simple() {
		String type = "String address";
		String address = "this is a simple object";
		StringRefAddr addr0 = new StringRefAddr(type, address);
		StringRefAddr addr1 = new StringRefAddr(type, address);
		assertTrue(addr0.equals(addr1));
	}

	public void testEquals_NotEquals() {
		String type = "String address";
		String address0 = "this is a simple object";
		String address1 = "this is another simple object";
		StringRefAddr addr0 = new StringRefAddr(type, address0);
		StringRefAddr addr1 = new StringRefAddr(type, address1);
		assertFalse(addr0.equals(addr1));
	}

	public void testEquals_AddressNull() {
		String type = "null";
		StringRefAddr addr0 = new StringRefAddr(type, null);
		StringRefAddr addr1 = new StringRefAddr(type, null);
		assertTrue(addr0.equals(addr0));
		assertFalse(addr0.equals(null));
		assertTrue(addr0.equals(addr1));
		assertTrue(addr1.equals(addr0));
	}

	public void testEquals_ObjNull() {
		String type = "String address";
		String address = "this is a simple object";
		StringRefAddr addr0 = new StringRefAddr(type, address);
		assertFalse(addr0.equals(null));
	}

	public void testEquals_TypeNull() {
		String address = "this is a simple object";
		StringRefAddr addr0 = new StringRefAddr(null, address);
		StringRefAddr addr1 = new StringRefAddr(null, address);
		try {
			addr0.equals(addr1);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testHashcode_Simple() {
		String type = "String address";
		String address = "this is a simple object";
		StringRefAddr addr0 = new StringRefAddr(type, address);
		assertEquals(type.hashCode() + address.hashCode(), addr0.hashCode());
	}

	public void testHashcode_TypeNull() {
		String content = "null";
		StringRefAddr addr0 = new StringRefAddr(null, content);
		try {
			addr0.hashCode();
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}

	}

	public void testHashcode_AddressNull() {
		String type = "null";
		StringRefAddr addr0 = new StringRefAddr(type, null);
		assertEquals(type.hashCode(), addr0.hashCode());
	}

	public void testToString_Simple() {
		String type = "address type";
		String address = "this is a simple object";
		StringRefAddr addr0 = new StringRefAddr(type, address);
		/*
		 * assertEquals( "The type of the address is: " + type + "\nThe content
		 * of the address is: " + address + "\n", addr0.toString());
		 */
		assertNotNull(addr0.toString());
	}

	public void testToString_AddressNull() {
		String type = "null";
		StringRefAddr addr0 = new StringRefAddr(type, null);
		// System.out.println();
		/*
		 * assertEquals( "The type of the address is: " + type + "\nThe content
		 * of the address is: null\n", addr0.toString());
		 */
		assertNotNull(addr0.toString());
	}

	public void testToString_typeNull() {
		String address = "this is a simple object with null type";
		StringRefAddr stringRefAddr = new StringRefAddr(null, address);
		// assertEquals(str, stringRefAddr.toString());
		assertNotNull(stringRefAddr.toString());
	}

	public void testSerializable_Simple() throws ClassNotFoundException,
			IOException {
		String type = "String address";
		String address = "this is a simple object";
		StringRefAddr addr = new StringRefAddr(type, address);

		// write to byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(addr);
		byte[] buffer = baos.toByteArray();
		oos.close();
		baos.close();

		// read from byte array
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
		ObjectInputStream ois = new ObjectInputStream(bais);
		StringRefAddr addr2 = (StringRefAddr) ois.readObject();
		ois.close();
		bais.close();

		assertEquals(addr, addr2);
	}

	public void testSerializable_compatibility() throws ClassNotFoundException,
			IOException {
		ObjectInputStream ois = new ObjectInputStream(getClass()
                .getClassLoader().getResourceAsStream(
                        "/serialization/javax/naming/StringRefAddr.ser"));
		StringRefAddr addr = (StringRefAddr) ois.readObject();
		ois.close();

		String type = "StringAddr";
		String address = "/home/anyuser";
		StringRefAddr addr2 = new StringRefAddr(type, address);

		assertEquals(addr, addr2);
	}
}
