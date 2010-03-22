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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.ldap.InitialLdapContext;
import junit.framework.TestCase;

/**
 * <p>This class test is made to test all cases of package where the coverage was not 100%.</p>
 * <p>We are going to find here a lot cases from different classes, notice here that the conventional structure
 * followed in the rest of the project is applied  here.</p>
 * 
 */
public class TestInitialLdapContextWhiteBoxDevelopment extends TestCase {

	public static void main(String[] args) {
	}

	public TestInitialLdapContextWhiteBoxDevelopment(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.InitialLdapContext.getEnviroment()'</p>
	 * <p>Here we are testing if this method can throw an exception of the type NoInitialContextException.</p>
	 */
	public void testGetEnviroment() {
		try {
			System.setProperty(Context.INITIAL_CONTEXT_FACTORY,"org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap.MockContextFactory");
			InitialLdapContext ilc=new InitialLdapContext(null,null);
			ilc.getEnvironment();
			
		} catch (NoInitialContextException e) {
			fail("Failed with:"+e);
		} catch (NamingException e){
			fail("Failed with:"+e);
		}

	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.InitialLdapContext.getConnectControls()'</p>
	 * <p>Here we are testing if this method can throw an exception of the type NoInitialContextException.</p>
	 */
	public void testgetConnectControls() {
		try {
			System.setProperty(Context.INITIAL_CONTEXT_FACTORY,"org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap.MockContextFactory");
			InitialLdapContext ilc=new InitialLdapContext(null,null);
			ilc.getConnectControls();
						
		}catch (NamingException e){
			fail("Failed with:"+e);
		}

	}

}
