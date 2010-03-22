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

import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.UnsolicitedNotification;

public class MockUnsolicitedNotification implements UnsolicitedNotification {

	/**
     * <p></p>
     */
    private static final long serialVersionUID = 1L;

    /*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.ldap.UnsolicitedNotification#getReferrals()
	 */
	public String[] getReferrals() {

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.ldap.UnsolicitedNotification#getException()
	 */
	public NamingException getException() {

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.ldap.ExtendedResponse#getID()
	 */
	public String getID() {

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.ldap.ExtendedResponse#getEncodedValue()
	 */
	public byte[] getEncodedValue() {

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.ldap.HasControls#getControls()
	 */
	public Control[] getControls() throws NamingException {

		return null;
	}

}
