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

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.ControlFactory;
import javax.naming.ldap.InitialLdapContext;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap.MockControl;
import junit.framework.TestCase;

/**
 * <p>This class test is made to test all cases of package where the coverage was not 100%.</p>
 * <p>We are going to find here a lot cases from different classes, notice here that the conventional structure
 * followed in the rest of the project is applied  here.</p>
 * 
 */
public class TestControlFactoryWhiteBoxDevelopment extends TestCase {

	public static void main(String[] args) {
	}

	public TestControlFactoryWhiteBoxDevelopment(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.ControlFactory.getControlInstance(Control)'</p>
	 * <p>Here we are going to test if we can get an instance with the controls sended.</p>
	 */
	public void testGetControlInstanceControl() {

		try {
			
			Hashtable env = new Hashtable();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap.MockContextFactory");
			MockControl[] cs = { new MockControl("c1", false, new byte[] { 1, 2, 3, 4 }), 
					new MockControl("c1", true, new byte[] { 'a', 'b', 'c', 'd' }), };
			MockControl cs2 =  new MockControl("c1", false, new byte[] { 1, 2, 3, 4 }); 
			InitialLdapContext ilc=new InitialLdapContext(env, cs);
			assertEquals(cs2,ControlFactory.getControlInstance(cs2,ilc,env));
			
		} catch (NamingException e) {
			
		}
	}
	
}
