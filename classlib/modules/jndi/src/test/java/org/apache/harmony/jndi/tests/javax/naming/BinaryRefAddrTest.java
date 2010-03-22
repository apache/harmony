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
import java.util.Random;

import javax.naming.BinaryRefAddr;
import javax.naming.RefAddr;

import junit.framework.Assert;
import junit.framework.TestCase;

public class BinaryRefAddrTest extends TestCase {

	public void testBinaryRefAddr_SimpleNormale() {
		byte[] ab = new byte[] { 1, 2, 3 };
		byte[] ab2;

		// Test normal condition
		BinaryRefAddr addr = new BinaryRefAddr("binary", ab);
		Assert.assertNotNull(addr);
		Assert.assertEquals("binary", addr.getType());
		ab2 = (byte[]) addr.getContent();
		Assert.assertEquals(ab.length, ab2.length);
		for (int i = ab2.length - 1; i >= 0; i--) {
			Assert.assertEquals(ab[i], ab2[i]);
		}
		assertNotSame(ab, ab2);
	}

	public void testBinaryRefAddr_SimpleNullType() {
		byte[] ab = new byte[] { 1, 2, 3 };
		byte[] ab2;

		// Test null "type" parameter
		BinaryRefAddr addr = new BinaryRefAddr(null, ab);
		Assert.assertNotNull(addr);
		Assert.assertNull(addr.getType());
		ab2 = (byte[]) addr.getContent();
		Assert.assertEquals(ab.length, ab2.length);
		for (int i = ab2.length - 1; i >= 0; i--) {
			Assert.assertEquals(ab[i], ab2[i]);
		}
	}

	public void testBinaryRefAddr_SimpleNullAddress() {
		BinaryRefAddr addr = null;

		// Test null address content
		try {
			addr = new BinaryRefAddr("binary", null);
			fail("Should throw NullPointerException here.");
		} catch (NullPointerException e) {
		}
		Assert.assertNull(addr);
	}

	public void testBinaryRefAddr_ComplexNormal() {
		byte[] ab = new byte[] { 1, 2, 3 };
		byte[] ab2;

		// Test normal condition
		BinaryRefAddr addr = new BinaryRefAddr("binary", ab, 1, 1);
		Assert.assertNotNull(addr);
		Assert.assertEquals("binary", addr.getType());
		ab2 = (byte[]) addr.getContent();
		Assert.assertEquals(ab2.length, 1);
		for (int i = ab2.length - 1; i >= 0; i--) {
			Assert.assertEquals(ab[i + 1], ab2[i]);
		}
		assertNotSame(ab, ab2);
	}

	public void testBinaryRefAddr_ComplexNullType() {
		byte[] ab = new byte[] { 1, 2, 3 };
		byte[] ab2;

		// Test null "type" parameter
		BinaryRefAddr addr = new BinaryRefAddr(null, ab, 1, 1);
		Assert.assertNotNull(addr);
		Assert.assertNull(addr.getType());
		ab2 = (byte[]) addr.getContent();
		Assert.assertEquals(ab2.length, 1);
		for (int i = ab2.length - 1; i >= 0; i--) {
			Assert.assertEquals(ab[i + 1], ab2[i]);
		}
	}

	public void testBinaryRefAddr_ComplexNullAddress() {
		BinaryRefAddr addr = null;

		// Test null address content
		try {
			addr = new BinaryRefAddr("binary", null, 1, 1);
			fail("Should throw NullPointerException here.");
		} catch (NullPointerException e) {
		}
		Assert.assertNull(addr);
	}

	public void testBinaryRefAddr_TooSmallIndex() {
		BinaryRefAddr addr = null;

		// Test too small index
		try {
			addr = new BinaryRefAddr("binary", new byte[] { 2, 3 }, -1, 1);
			fail("Should throw ArrayIndexOutOfBoundsException here.");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		Assert.assertNull(addr);
	}

	public void testBinaryRefAddr_TooBigIndex() {
		BinaryRefAddr addr = null;

		// Test too big index
		try {
			addr = new BinaryRefAddr("binary", new byte[] { 2, 3 }, 2, 1);
			fail("Should throw ArrayIndexOutOfBoundsException here.");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		Assert.assertNull(addr);
	}

	public void testBinaryRefAddr_ComplexZeroSize() {
		byte[] ab;
		BinaryRefAddr addr = null;

		// Test zero size
		addr = new BinaryRefAddr("binary", new byte[] { 2, 3 }, 0, 0);
		ab = (byte[]) addr.getContent();
		Assert.assertEquals(ab.length, 0);
		Assert.assertNotNull(addr);
	}

	public void testBinaryRefAddr_TooSmallSize() {
		BinaryRefAddr addr = null;

		// Test too small size
		try {
			addr = new BinaryRefAddr("binary", new byte[] { 2, 3 }, 0, -1);
			fail("Should throw NegativeArraySizeException here.");
		} catch (NegativeArraySizeException e) {
		}
		Assert.assertNull(addr);
	}

	public void testBinaryRefAddr_TooBigSize() {
		BinaryRefAddr addr = null;

		// Test too big size
		try {
			addr = new BinaryRefAddr("binary", new byte[] { 2, 3 }, 0, 3);
			fail("Should throw ArrayIndexOutOfBoundsException here.");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		Assert.assertNull(addr);

	}

	public void testGetType() {
		// Test empty type
		BinaryRefAddr addr = new BinaryRefAddr("", new byte[] { 1 });
		Assert.assertEquals("", addr.getType());

		// Other conditions are tested in testBinaryRefAddr_XXX
	}

	public void testEquals_Simple() {
		String type = "Binary Address";
		int count = 10;
		byte[] address0 = new byte[count];
		byte[] address1 = new byte[count];
		Random random = new Random(100);
		for (int i = 0; i < count; i++) {
			address0[i] = (byte) random.nextInt();
			address1[i] = address0[i];
		}
		BinaryRefAddr addr0 = new BinaryRefAddr(type, address0);
		BinaryRefAddr addr1 = new BinaryRefAddr(type, address1);
		assertTrue(addr0.equals(addr0));
		assertFalse(addr0.equals(null));
		assertTrue(addr1.equals(addr0));
		assertTrue(addr0.equals(addr1));
	}

	public void testEquals_TypeNull() {
		int count = 10;
		byte[] address0 = new byte[count];
		byte[] address1 = new byte[count];
		Random random = new Random(10);
		for (int i = 0; i < count; i++) {
			address0[i] = (byte) random.nextInt();
			address1[i] = address0[i];
		}

		BinaryRefAddr addr0 = new BinaryRefAddr(null, address0);
		BinaryRefAddr addr1 = new BinaryRefAddr(null, address1);
		try {
			addr0.equals(addr1);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}
	}

	public void testtestEquals_refAddr() {
		String type = "Binary Type";
		byte[] address = { 1, 2, 3, 4 };
		BinaryRefAddr addr = new BinaryRefAddr(type, address);
		MyRefAddr addr2 = new MyRefAddr(type, address);

		assertFalse(addr.equals(addr2));
	}

	public void testHashcode_Simple() {
		String type = "Binary Address";

		int count = 10;
		byte[] address = new byte[count];
		Random random = new Random(20);
		for (int i = 0; i < count; i++) {
			address[i] = (byte) random.nextInt();
		}

		int hashCode = type.hashCode();
		for (byte element : address) {
			hashCode += element;
		}
		BinaryRefAddr addr = new BinaryRefAddr(type, address);
		assertEquals(hashCode, addr.hashCode());
	}

	public void testHashcode_TypeNull() {
		byte[] address = { 1, 2, 3, };

		BinaryRefAddr addr = new BinaryRefAddr(null, address);
		try {
			addr.hashCode();
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}

	}

	public void testGetContent_Simple() {
		String type = "Binary Address";
		byte[] address = { 1, 2, 3, 4, 5, 6 };
		BinaryRefAddr addr = new BinaryRefAddr(type, address);

		assertTrue(java.util.Arrays.equals(address, (byte[]) addr.getContent()));
	}

	public void testToString() {
		String type = "Binary Address";
		byte[] address = { 'a', 3, 0x7F, (byte) 0x80, (byte) 90, (byte) 0xFF };

		BinaryRefAddr addr = new BinaryRefAddr(type, address);

		String str = "The type of the address is: " + type
				+ "\nThe content of the address is:";
		for (byte element : address) {
			str += " " + Integer.toHexString(element);
		}
		str += "\n";
		// assertEquals(str, addr.toString());
		assertNotNull(addr.toString());
	}

	public void testToString_TypeNull() {
		byte[] address = { 1, 2, 3, };
		BinaryRefAddr addr = new BinaryRefAddr(null, address);
		// assertEquals(str, addr.toString());
		assertNotNull(addr.toString());
	}

	public void testSerializable_Simple() throws ClassNotFoundException,
			IOException {
		String type = "Binary Address";
		int count = 10;
		byte[] address = new byte[count];
		Random random = new Random(20);
		for (int i = 0; i < count; i++) {
			address[i] = (byte) random.nextInt();
		}
		BinaryRefAddr addr = new BinaryRefAddr(type, address);

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
		BinaryRefAddr addr2 = (BinaryRefAddr) ois.readObject();
		ois.close();
		bais.close();

		assertEquals(addr, addr2);
	}

	public void testSerializable_compatibility() throws ClassNotFoundException,
			IOException {
		// TO DO R: need to implement
		ObjectInputStream ois = new ObjectInputStream(getClass()
                .getClassLoader().getResourceAsStream(
                        "/serialization/javax/naming/BinaryRefAddr.ser"));
		BinaryRefAddr addr = (BinaryRefAddr) ois.readObject();
		ois.close();

		String type = "Binary Address";
		int count = 100;
		byte[] address = new byte[count];
		for (int i = 0; i < count; i++) {
			address[i] = (byte) i;
		}
		BinaryRefAddr addr2 = new BinaryRefAddr(type, address);
		assertEquals(addr, addr2);
	}

	class MyRefAddr extends RefAddr {
        private static final long serialVersionUID = 1L;
        byte[] address;

		public MyRefAddr(String type, byte[] address) {
			super(type);
			this.address = new byte[address.length];
			System.arraycopy(address, 0, this.address, 0, address.length);
		}

		@Override
        public Object getContent() {
			return address;
		}
	}
}