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

import javax.naming.ldap.Control;

/**
 * <p>Implementation of the interfaces Control. This class has the intention of
 * give us a Control to test another classes.</p> 
 *
 */
public class MockControl implements Control {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String id;
	private boolean criticality;
	private byte[] value;
	
	/**
	 * Constructor method to the our control.
	 * @param id The Id field of this control.
	 * @param crit The citicality of the control.
	 * @param value The BER value of the control.
	 */
	public MockControl(String id,boolean crit,byte[] value) {
		this.id=id;
		this.criticality=crit;
		this.value=value;
	}
	
	/**
	 * Constructor method to the our control.
	 * @param id The Id field of this control.
	 */
	public MockControl(String id){
		this.id=id;
		this.criticality=false;
		this.value=null;
	}

	/**
	 * This method give us the Id of the control.
	 * @return The Id of the control.
	 */
	public String getID() {
		// TODO Auto-generated method stub
		return id;
	}

	/**
	 * This method give us the criticality of the control.
	 * @return The criticality.
	 */
	public boolean isCritical() {
		// TODO Auto-generated method stub
		return criticality;
	}

	/**
	 * This method give us the BER value of the control.
	 * @return The BER value.
	 */
	public byte[] getEncodedValue() {
		// TODO Auto-generated method stub
		return value;
	}

}
