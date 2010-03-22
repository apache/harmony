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
import javax.naming.spi.InitialContextFactory;

/**
 * <p>Implementation of the interfaces InitialContextFactory. This class has the intention of
 * give us a context to test another classes.</p> 
 *
 */
public class MockContextFactory implements InitialContextFactory {

	/**
	 * <p>This method creates an Initial Context for beginning name resolution.</p>
	 * @param envmt The properties.
	 * @return The Context representing a LDAP server.
	 * @throws NamingException If an error is encounter.
	 */
	public Context getInitialContext(Hashtable envmt) throws NamingException {

		return new MockInitialLdapContext(envmt);
	
	}
	
	
}
