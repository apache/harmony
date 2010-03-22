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

package org.apache.harmony.jndi.tests.javax.naming.ldap;

import java.util.Arrays;

import javax.naming.ldap.Control;

public class MockControl implements Control {

    private static final long serialVersionUID = 1L;

    boolean isCritical;

	byte[] encodedValue;

	String id;

	public MockControl(String id) {
		this(id, null, false);
	}

	public MockControl(String id, byte[] encodedValue, boolean isCritical) {
		this.id = id;
		this.isCritical = isCritical;
		if (encodedValue != null) {
			this.encodedValue = new byte[encodedValue.length];
			System.arraycopy(encodedValue, 0, this.encodedValue, 0,
					this.encodedValue.length);
		}
	}

	public void setID(String id) {
		this.id = id;
	}

	public byte[] getEncodedValue() {
		return this.encodedValue;
	}

	public String getID() {
		return this.id;
	}

	public boolean isCritical() {
		return this.isCritical;
	}

	@Override
    public boolean equals(Object arg0) {
		if (arg0 instanceof MockControl) {
			MockControl a = (MockControl) arg0;
			return this.id.equals(a.getID())
					&& (this.isCritical == a.isCritical())
					&& Arrays.equals(this.encodedValue, a.getEncodedValue());
		}
		return false;
	}

}
