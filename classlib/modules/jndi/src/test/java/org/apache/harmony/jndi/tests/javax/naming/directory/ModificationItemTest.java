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

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.harmony.jndi.tests.javax.naming.util.Log;
import junit.framework.TestCase;

public class ModificationItemTest extends TestCase {

	static Log log = new Log(ModificationItemTest.class);

	BasicAttribute attr = new BasicAttribute("id_sample", "value_sample");

	public void testModificationItem() {
		log.setMethod("testModificationItem()");
		ModificationItem item;

		item = new ModificationItem(DirContext.ADD_ATTRIBUTE, attr);
		assertEquals(DirContext.ADD_ATTRIBUTE, item.getModificationOp());
		assertEquals(attr, item.getAttribute());
	}

	public void testModificationItem_InvalidOp() {
		log.setMethod("testModificationItem_InvalidOp()");

		try {
			new ModificationItem(-255, attr);
			fail("Should throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testModificationItem_NullAttribute() {
		log.setMethod("testModificationItem_NullAttribute()");

		try {
			new ModificationItem(DirContext.ADD_ATTRIBUTE, null);
			fail("Should throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testGetAttribute() {
		log.setMethod("testGetAttribute()");
		ModificationItem item;

		item = new ModificationItem(DirContext.ADD_ATTRIBUTE, attr);
		assertEquals(DirContext.ADD_ATTRIBUTE, item.getModificationOp());
		assertEquals(attr, item.getAttribute());
	}

	public void testGetModificationOp() {
		log.setMethod("testGetModificationOp()");
		ModificationItem item;

		item = new ModificationItem(DirContext.ADD_ATTRIBUTE, attr);
		assertEquals(DirContext.ADD_ATTRIBUTE, item.getModificationOp());
		assertEquals(attr, item.getAttribute());
	}

	/*
	 * Test for String toString()
	 */
	public void testToString() {
		log.setMethod("testToString()");
		ModificationItem item;

		item = new ModificationItem(DirContext.ADD_ATTRIBUTE, attr);
		assertTrue(null != item.toString());
	}

}
