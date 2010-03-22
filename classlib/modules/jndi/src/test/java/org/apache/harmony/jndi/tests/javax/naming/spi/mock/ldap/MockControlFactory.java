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

import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.ControlFactory;

/**
 * <p>Implementation of the interfaces ControlFactory. This class has the intention of
 * give us a Control to test another classes.</p> 
 *
 */
public class MockControlFactory extends ControlFactory {

	/**
	 * Constructor method that initiate the inherti constructor.
	 *
	 */
	public MockControlFactory() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Method not implemented.
	 */
	public Control getControlInstance(Control arg0) throws NamingException {
		return null;
	}

}
