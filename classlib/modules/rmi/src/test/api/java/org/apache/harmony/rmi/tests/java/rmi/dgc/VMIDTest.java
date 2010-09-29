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
package org.apache.harmony.rmi.tests.java.rmi.dgc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.rmi.dgc.VMID;

import junit.framework.TestCase;

public class VMIDTest extends TestCase {

	public void testEquals() {
		VMID vmid = new VMID();
		VMID vmid2 = new VMID();

		assertTrue(vmid.equals(vmid));
		assertFalse(vmid.equals(vmid2));
	}

	public void testHashCode() {
		VMID vmid = new VMID();
		VMID vmid2 = new VMID();

		assertTrue(vmid.hashCode() != vmid2.hashCode());
		assertTrue(vmid.hashCode() == vmid.hashCode());
	}

	public void testToString() {
		VMID vmid = new VMID();
		VMID vmid2 = new VMID();
		assertEquals(vmid.toString(), vmid.toString());

		assertFalse(vmid.toString().equals(vmid2.toString()));
	}

	public void testIsUnique() {
		VMID vmid = new VMID();
		boolean unique = true;
		try {
			InetAddress.getLocalHost();
		} catch (Exception ex) {
			// This exception is related to the behavior of vmid.isUnique
			unique = false;
		}

		assertTrue(unique == vmid.isUnique());
	}

	public void testReadWriteObject() throws IOException, ClassNotFoundException {
		VMID vmid = new VMID();

		File tmpFile = File.createTempFile("VMIDTest", "tmp");
		ObjectOutputStream output = new ObjectOutputStream(
				new FileOutputStream(tmpFile));
		output.writeObject(vmid);
		output.flush();
		output.close();

		VMID vmid2 = null;
		ObjectInputStream input = new ObjectInputStream(new FileInputStream(
				tmpFile));
		vmid2 = (VMID) input.readObject();
		input.close();
		tmpFile.delete();

		assertTrue(vmid.equals(vmid2));
	}

}
