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

import java.io.IOException;
import java.math.BigInteger;
import javax.naming.ldap.PagedResultsControl;
import junit.framework.TestCase;

/**        
 * <p>This Test class is testing the PagedResultsControls class.</p>
 * <p>In the next tables we are going to see the methods that we test in this class:</p>
 * <table class="t" cellspacing="0">
	<tbody><th>Constructors:</th>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="60" name="sas9nt21" readonly="readonly" value="PagedResultsControl(int pageSize, boolean criticality)" id="f10"></td>
			
		</tr>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="60" name="sas9nt21" readonly="readonly" value="PagedResultsControl(int pageSize, byte[] cookie, boolean criticality)" id="f10"></td>
			
		</tr>     
		      
	</tbody></table>	
 *
 */
public class TestPagedResultsControls extends TestCase {

	/**
	 * <p>This method is not implemted.</p>
	 * @param args Possible parameter to help us initiate all tests.
	 */
	public static void main(String[] args) {
	}

	public TestPagedResultsControls(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsControl.PagedResultsControl(int, boolean)'</p>
	 * <p>Here we are testing if this method constructs a control to set the number of entries to be returned per page of results.</p>
	 * <p>The expected result is an instance of this class.</p>
	 */
	public void testPagedResultsControlIntBoolean001() {
		try {
			PagedResultsControl prc=new PagedResultsControl(0,false);
			assertNotNull(prc);
		} catch (IOException e) {
			fail("Failed with:"+e);
		}

	}

	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsControl.PagedResultsControl(int, boolean)'</p>
	 * <p>Here we are testing if this method constructs a control to set the number of entries to be returned per page of results.</p>
	 * <p>The expected result is an instance of this class.</p>
	 */
	public void testPagedResultsControlIntBoolean002() {
		try {
			PagedResultsControl prc=new PagedResultsControl(0,true);
			assertNotNull(prc);
		} catch (IOException e) {
			fail("Failed with:"+e);
		}

	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsControl.PagedResultsControl(int, boolean)'</p>
	 * <p>Here we are testing if this method constructs a control to set the number of entries to be returned per page of results.</p>
	 * <p>The expected result is an instance of this class.</p>
	 */
	public void testPagedResultsControlIntBoolean003() {
		try {
			PagedResultsControl prc=new PagedResultsControl(100,false);
			assertNotNull(prc);
		} catch (IOException e) {
			fail("Failed with:"+e);
		}

	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsControl.PagedResultsControl(int, boolean)'</p>
	 * <p>Here we are testing if this method constructs a control to set the number of entries to be returned per page of results.</p>
	 * <p>The expected result is an instance of this class.</p>
	 */
	public void testPagedResultsControlIntBoolean004() {
		try {
			PagedResultsControl prc=new PagedResultsControl(100,true);
			assertNotNull(prc);
		} catch (IOException e) {
			fail("Failed with:"+e);
		}

	}

	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsControl.PagedResultsControl(int, boolean)'</p>
	 * <p>Here we are testing if this method constructs a control to set the number of entries to be returned per page of results.</p>
	 * <p>The expected result is an instance of this class.</p>
	 */
	public void testPagedResultsControlIntBoolean005() {
		try {
			PagedResultsControl prc=new PagedResultsControl(1000000,false);
			assertNotNull(prc);
		} catch (IOException e) {
			fail("Failed with:"+e);
		}

	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsControl.PagedResultsControl(int, boolean)'</p>
	 * <p>Here we are testing if this method constructs a control to set the number of entries to be returned per page of results.</p>
	 * <p>The expected result is an instance of this class.</p>
	 */
	public void testPagedResultsControlIntBoolean006() {
		try {
			PagedResultsControl prc=new PagedResultsControl(1000000,true);
			assertNotNull(prc);
		} catch (IOException e) {
			fail("Failed with:"+e);
		}

	}

	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsControl.PagedResultsControl(int, byte[], boolean)'</p>
	 * <p>Here we are testing if this method constructs a control to set the number of entries to be returned per page of results.</p>
	 * <p>The expected result is an instance of this class.</p> 
	 */
	public void testPagedResultsControlIntByteArrayBoolean001() {

		try {
			PagedResultsControl prc=new PagedResultsControl(0,null,true);
			assertNotNull(prc);
		} catch (IOException e) {
			fail("Failed with:"+e);
		}

		
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsControl.PagedResultsControl(int, byte[], boolean)'</p>
	 * <p>Here we are testing if this method constructs a control to set the number of entries to be returned per page of results.</p>
	 * <p>The expected result is an instance of this class.</p> 
	 */
	public void testPagedResultsControlIntByteArrayBoolean002() {

		try {
			PagedResultsControl prc=new PagedResultsControl(0,null,false);
			assertNotNull(prc);
		} catch (IOException e) {
			fail("Failed with:"+e);
		}

		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsControl.PagedResultsControl(int, byte[], boolean)'</p>
	 * <p>Here we are testing if this method constructs a control to set the number of entries to be returned per page of results.</p>
	 * <p>The expected result is an instance of this class.</p> 
	 */
	public void testPagedResultsControlIntByteArrayBoolean003() {

		try {
			byte[] by={10,10};
			PagedResultsControl prc=new PagedResultsControl(10,by,true);
			assertNotNull(prc);
		} catch (IOException e) {
			fail("Failed with:"+e);
		}

		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsControl.PagedResultsControl(int, byte[], boolean)'</p>
	 * <p>Here we are testing if this method constructs a control to set the number of entries to be returned per page of results.</p>
	 * <p>The expected result is an instance of this class.</p> 
	 */
	public void testPagedResultsControlIntByteArrayBoolean004() {

		try {
			byte[] by={10,10};
			PagedResultsControl prc=new PagedResultsControl(10,by,false);
			assertNotNull(prc);
		} catch (IOException e) {
			fail("Failed with:"+e);
		}

		
	}


	
	/**
	 * <p>Test method for 'javax.naming.ldap.BasicControl.getEncodedValue()'</p>
	 * <p>Here we are testing if this method returns retrieves the control's ASN.1 BER encoded value.</p>
	 * <p>The expected result is a byte array representing the control's ASN.1 BER encoded value.</p>
	 */
	public void testGetEncodedValue001() {

		byte[] by={10,10};
		try {
			PagedResultsControl prc=new PagedResultsControl(10,by,false);
			assertEquals("30 07 02 01 0a 04 02 0a 0a",toHexString(prc.getEncodedValue()));
		} catch (IOException e) {
			fail("Failed with:"+e);
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.BasicControl.getEncodedValue()'</p>
	 * <p>Here we are testing if this method returns retrieves the control's ASN.1 BER encoded value.</p>
	 * <p>The expected result is a byte array representing the control's ASN.1 BER encoded value.</p>
	 */
	public void testGetEncodedValue002() {

		try {
			PagedResultsControl prc=new PagedResultsControl(0,null,false);
			assertEquals("30 05 02 01 00 04 00",toHexString(prc.getEncodedValue()));
		} catch (IOException e) {
			fail("Failed with:"+e);
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.BasicControl.getEncodedValue()'</p>
	 * <p>Here we are testing if this method returns retrieves the control's ASN.1 BER encoded value.</p>
	 * <p>The expected result is a byte array representing the control's ASN.1 BER encoded value.</p>
	 */
	public void testGetEncodedValue003() {

		try {
			PagedResultsControl prc=new PagedResultsControl(0,null,true);
			assertEquals("30 05 02 01 00 04 00",toHexString(prc.getEncodedValue()));
		} catch (IOException e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.BasicControl.getEncodedValue()'</p>
	 * <p>Here we are testing if this method returns retrieves the control's ASN.1 BER encoded value.</p>
	 * <p>The expected result is a byte array representing the control's ASN.1 BER encoded value.</p>
	 */
	public void testGetEncodedValue004() {

		byte[] by={1,10,20};
		try {
			PagedResultsControl prc=new PagedResultsControl(0,by,false);
			assertEquals("30 08 02 01 00 04 03 01 0a 14",toHexString(prc.getEncodedValue()));
		} catch (IOException e) {
			fail("Failed with:"+e);
		}
	}
	
	/*
	 * Method to get the string of a byte array.
	 */
	private static String toHexString(byte[] data) {
		BigInteger bi = new BigInteger(data);
		String s = bi.toString(16);
		StringBuffer hex = new StringBuffer();
		if (s.length() % 2 != 0) {
			s = "0" + s;
		}
		for (int i = 0; i < s.length(); i++) {
			hex.append(s.charAt(i));
			if (i % 2 != 0 && i < s.length() - 1) {
				hex.append(" ");
			}
		}
		return hex.toString();
	}

}
