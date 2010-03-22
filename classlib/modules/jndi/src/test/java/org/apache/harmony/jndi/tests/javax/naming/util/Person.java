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
package org.apache.harmony.jndi.tests.javax.naming.util;

import java.io.Serializable;

import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

public class Person implements Serializable, Cloneable, Referenceable {
	/**
     * <p></p>
     */
    private static final long serialVersionUID = 1L;

    private static int maxId = 0;

	int id;

	String name;

	String address;

	int gender;

	private Person() {
	}

	public static synchronized Person getInstance() {
		Person result = new Person();
		result.setId(++maxId);
		result.setName("name" + maxId);
		result.setGender(0);
		result.setAddress("address" + maxId);
		return result;
	}

	/**
	 * @return
	 */
	public final String getAddress() {
		return address;
	}

	/**
	 * @return
	 */
	public final int getGender() {
		return gender;
	}

	/**
	 * @return
	 */
	public final int getId() {
		return id;
	}

	/**
	 * @return
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @param string
	 */
	public final void setAddress(String string) {
		address = string;
	}

	/**
	 * @param i
	 */
	public final void setGender(int i) {
		gender = i;
	}

	/**
	 * @param i
	 */
	public final void setId(int i) {
		id = i;
	}

	/**
	 * @param string
	 */
	public final void setName(String string) {
		name = string;
	}

	@Override
    public String toString() {
		StringBuffer buffer = new StringBuffer(100);
		buffer.append(this.getClass().getName()).append(" : ");
		buffer.append("id=").append(id);
		buffer.append(",name=").append(name);
		buffer.append(",gender=").append(gender);
		buffer.append(",address=").append(address);
		return buffer.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
    public boolean equals(Object o) {
		boolean result = false;
		if (o instanceof Person) {
			Person p = (Person) o;
			int id2 = p.getId();
			int gender2 = p.getGender();
			String name2 = p.getName();
			String address2 = p.getAddress();
			result = (id == id2)
					&& (gender == gender2)
					&& (null == name2 ? null == name : name2.equals(name))
					&& (null == address2 ? null == address : address2
							.equals(address));
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
    public int hashCode() {
		int result = 17;
		result = 37 * result + id;
		result = 37 * result + gender;
		if (null != name) {
			result = 37 * result + name.hashCode();
		}
		if (null != address) {
			result = 37 * result + address.hashCode();
		}
		return result;
	}

	@Override
    public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Referenceable#getReference()
	 */
	public Reference getReference() {
		StringRefAddr addr = new StringRefAddr("StringRefAddr", toString());
		Reference reference = new Reference(getClass().getName(), addr);
		return reference;
	}
}
