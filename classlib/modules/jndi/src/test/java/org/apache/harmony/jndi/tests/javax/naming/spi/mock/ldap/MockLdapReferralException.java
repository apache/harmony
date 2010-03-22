/* 
 *  Licensed to the Apache Software Foundation (ASF) under one or more 
 *  contributor license agreements.  See the NOTICE file distributed with 
 *  this work for additional information regarding copyright ownership. 
 *  The ASF licenses this file to You under the Apache License, Version 2.0 
 *  (the "License"); you may not use this file except in compliance with 
 *  the License.  You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */ 
/**
 * @author Hugo Beilis
 * @author Leonardo Soler
 * @author Gabriel Miretti
 * @version 1.0
 */
package org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapReferralException;

/**
 * <p>Implementation of the class LdapReferralException. This class has the intention of
 * give us an instance of the class to test it.</p> 
 *
 */
public class MockLdapReferralException extends LdapReferralException {

	/**
	 * The serial to the class.
	 */
	private static final long serialVersionUID = 1L;

	public MockLdapReferralException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public MockLdapReferralException() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public Context getReferralContext() throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Context getReferralContext(Hashtable<?, ?> arg0)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Context getReferralContext(Hashtable<?, ?> arg0, Control[] arg1)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getReferralInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean skipReferral() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void retryReferral() {
		// TODO Auto-generated method stub

	}

}
