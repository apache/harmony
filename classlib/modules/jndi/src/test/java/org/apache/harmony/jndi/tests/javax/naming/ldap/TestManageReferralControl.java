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

package org.apache.harmony.jndi.tests.javax.naming.ldap;

import javax.naming.ldap.ManageReferralControl;
import junit.framework.TestCase;

/**
 * <p>Test cases for all methods of the class ManageReferralControl.<p>
 * 
 * <p>The next table contains a list of the methods to be tested:<p>
	<table class="t" cellspacing="0">
	<tbody><th>Constructors:</th>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="20" name="sas9nt11" readonly="readonly" value="" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="40" name="sas9nt21" readonly="readonly" value="ManageReferralControl()" id="f10"></td>
			
		</tr>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="20" name="sas9nt11" readonly="readonly" value="" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="40" name="sas9nt21" readonly="readonly" value="ManageReferralControl(boolean criticality)" id="f10"></td>
			
		</tr>
	</tbody>
 *  
 */
public class TestManageReferralControl extends TestCase {

	/**
	 * <p>This method is not implemted.</p>
	 * @param args Possible parameter to help us initiate all tests.
	 */
	public static void main(String[] args) {
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.ManageReferralControl.ManageReferralControl()'<p>
	 * <p>Here we are testing the constructor method that create an object of the class
	 * ManageReferralControl, wich has no enter parameters.<p>
	 * <p>The expected result is a non null and critical instance of this class.<p> 
	 */
	public void testManageReferralControl() {
		
		ManageReferralControl mrf=new ManageReferralControl();
		assertNotNull(mrf);
		assertTrue(mrf.isCritical());
		
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.ManageReferralControl.ManageReferralControl(boolean)'<p>
	 * <p>Here we are testing the constructor method that create an object of the class, wich has as a 
	 * parameter, the criticality of the object. The enter parameter in this case is true.<p>
	 * <p>The expected result is an object with the mention criticality.<p>
	 */
	public void testManageReferralControlBoolean001() {

		ManageReferralControl mrc = new ManageReferralControl(true);
		assertNotNull(mrc);
		assertTrue(mrc.isCritical());
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.ManageReferralControl.ManageReferralControl(boolean)'<p>
	 * <p>Here we are testing the constructor method that create an object of the class, wich has as a 
	 * parameter, the criticality of the object. The enter parameter is false.<p>
	 * <p>The expected result is an object with the mention criticality.<p>
	 */
	public void testManageReferralControlBoolean002() {

		ManageReferralControl mrc = new ManageReferralControl(false);
		assertNotNull(mrc);
		assertFalse(mrc.isCritical());
		
	}
	/**
	 * <p>Test Method for 'javax.naming.ldap.ManageReferralControl.getID()'<p>
	 * <p>Here we are testing the ID of this object. The expected ID is the 
	 * String : "2.16.840.1.113730.3.4.2."<p>
	 */
	public void testGetID(){
		
		ManageReferralControl mrc = new ManageReferralControl();
		assertEquals("2.16.840.1.113730.3.4.2",mrc.getID());
	}

}
