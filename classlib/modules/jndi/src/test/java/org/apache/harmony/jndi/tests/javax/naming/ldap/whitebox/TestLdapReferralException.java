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
package org.apache.harmony.jndi.tests.javax.naming.ldap.whitebox;

import org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap.MockLdapReferralException;

import junit.framework.TestCase;

/**
 * <p>This class test is made to test all cases of package where the coverage was not 100%.</p>
 * <p>We are going to find here a lot cases from different classes, notice here that the conventional structure
 * followed in the rest of the project is applied  here.</p>
 * 
 */
public class TestLdapReferralException extends TestCase {

	public static void main(String[] args) {
	}

	public TestLdapReferralException(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Here we are testing if this method receives a string a creates a not null object.
	 *
	 */
	public void testLdapReferralExceptionString(){
		MockLdapReferralException mc=new MockLdapReferralException("test");
		assertNotNull(mc);
	}
}
