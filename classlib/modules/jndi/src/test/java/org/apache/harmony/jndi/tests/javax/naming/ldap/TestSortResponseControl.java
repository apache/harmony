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
import java.io.Serializable;

import javax.naming.AuthenticationNotSupportedException;
import javax.naming.LimitExceededException;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.OperationNotSupportedException;
import javax.naming.ServiceUnavailableException;
import javax.naming.TimeLimitExceededException;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.ldap.SortResponseControl;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

/**        
 * <p>This Test class is testing the SortControl class.</p>
 * <p>In the next tables we see the methods that we test in this class:</p>
 * <table class="t" cellspacing="0">
	<tbody><th>Constructors:</th>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="60" name="sas9nt21" readonly="readonly" value="SortResponseControl(String id, boolean criticality, byte[] value)" id="f10"></td>
			
		</tr>
		      
	</tbody></table>
	<table class="t" cellspacing="0">
	<tbody><th>Method Sumary:</th>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="String" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="60" name="sas9nt21" readonly="readonly" value="getAttributeID()" id="f10"></td>
			
		</tr>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="NamingException" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="60" name="sas9nt21" readonly="readonly" value="getException()" id="f10"></td>
			
		</tr>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="int" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="60" name="sas9nt21" readonly="readonly" value="getResultCode()" id="f10"></td>
			
		</tr>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="boolean" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="60" name="sas9nt21" readonly="readonly" value="isSorted()" id="f10"></td>
			
		</tr>
		      
	</tbody></table> 	
          
 *
 */
public class TestSortResponseControl extends TestCase {


	/**
	 * <p>This method is not implemented.</p>
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
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception like NullPointer.</p>
  	 */
	public void testSortResponseControl001() {

		String Id=null;
		boolean crit=false;
		byte[] ber=null;
		try {
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("Some arguments are null, so an exception must be thrown.");
		} catch (IOException e) {
			fail("Failed with:"+e);
		} catch (NullPointerException e) {
			
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception like NullPointer.</p>
  	 */
	public void testSortResponseControl002() {

		String Id=null;
		boolean crit=true;
		byte[] ber=null;
		try {
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("Some arguments are null, so an exception must be thrown.");
		} catch (IOException e) {
			fail("Failed with:"+e);
		} catch (NullPointerException e) {
			
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception like IOException.</p>
  	 */
	public void testSortResponseControl003() {

		String Id=null;
		boolean crit=true;
		byte[] ber={};
		try {
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("Insufficient data.");
		} catch (IOException e) {
		
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception like IOException.</p>
  	 */
	public void testSortResponseControl004() {

		String Id=null;
		boolean crit=false;
		byte[] ber={};
		try {
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("Insufficient data.");
		} catch (IOException e) {
		
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception like IOException.</p>
  	 */
	public void testSortResponseControl005() {

		String Id=null;
		boolean crit=false;
		byte[] ber=new String("ID").getBytes();
		try {
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("The bytes are not in the ASN.1 BER.");
		} catch (IOException e) {
			
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an instance of the class.</p>
  	 */
	public void testSortResponseControl006() {

		String Id=null;
		boolean crit=false;
		try {
			byte[] ber={48,3,10,1,0};
			assertNotNull(new SortResponseControl(Id,crit,ber));
			
		} catch (IOException e) {
			fail("The bytes are in the ASN.1 BER.");
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception.</p>
  	 */
	public void testSortResponseControl007() {

		String Id="";
		boolean crit=false;
		try {
			byte[] ber=null;
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("The bytes are not in the ASN.1 BER.");
		} catch (IOException e) {
			fail("Should raise another exception.");
		} catch (NullPointerException e) {
			
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception.</p>
  	 */
	public void testSortResponseControl008() {

		String Id="";
		boolean crit=true;
		try {
			byte[] ber=null;
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("The bytes are not in the ASN.1 BER.");
		} catch (IOException e) {
			fail("Should raise another exception.");
		} catch (NullPointerException e) {
			
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception.</p>
  	 */
	public void testSortResponseControl010() {

		String Id="";
		boolean crit=true;
		try {
			byte[] ber={};
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("The bytes are not in the ASN.1 BER.");
		} catch (IOException e) {
			
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception.</p>
  	 */
	public void testSortResponseControl011() {

		String Id="";
		boolean crit=false;
		try {
			byte[] ber={};
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("The bytes are not in the ASN.1 BER.");
		} catch (IOException e) {
			
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception.</p>
  	 */
	public void testSortResponseControl012() {

		String Id="";
		boolean crit=true;
		try {
			byte[] ber={10,20,12};
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("The bytes are not in the ASN.1 BER.");
		} catch (IOException e) {
			
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an exception.</p>
  	 */
	public void testSortResponseControl013() {

		String Id="";
		boolean crit=false;
		try {
			byte[] ber={10,20,12};
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("The bytes are not in the ASN.1 BER.");
		} catch (IOException e) {
			
		}
	}
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an instance of the class.</p>
  	 */
	public void testSortResponseControl014() {

		String Id="";
		boolean crit=false;
		try {
			byte[] ber={48,3,10,1,0};
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			assertNotNull(src);
		} catch (IOException e) {
			fail("The bytes are in the ASN.1 BER.");
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor.</p>
	 * <p>The expected result is an instance of the class.</p>
  	 */
	public void testSortResponseControl015() {

		String Id="";
		boolean crit=true;
		try {
			byte[] ber={48,3,10,1,0};
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			assertNotNull(src);
		} catch (IOException e) {
			fail("The bytes are in the ASN.1 BER.");
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor. With this test
	 * we see that the string Id and the criticality does not matter, olny the BER value.</p>
	 * <p>The expected result is an instance of the class.</p>
  	 */
	public void testSortResponseControl016() {

		String Id="test";
		boolean crit=true;
		try {
			byte[] ber={48,3,10,1,0};
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			assertNotNull(src);
		} catch (IOException e) {
			fail("The bytes are in the ASN.1 BER.");
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor. With this test
	 * we see that the string Id and the criticality does not matter, olny the BER value.</p>
	 * <p>The expected result is an exception.</p>
  	 */
	public void testSortResponseControl017() {

		String Id="test";
		boolean crit=false;
		try {
			byte[] ber={3,10,1,0};
			SortResponseControl src=new SortResponseControl(Id,crit,ber);
			fail("The bytes are not in the ASN.1 BER.");
		} catch (IOException e) {
			
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.SortResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing the constructor. Notice here that 
	 * the ber value is not cloned so any change will affect the control.</p>
	 * <p>The expected result is that a change in the ber value affect the control.</p>
  	 */
	public void testSortResponseControl018(){

		String Id="test";
		boolean crit=false;
		byte[] ber={48,3,10,1,0};
		SortResponseControl src=null;
		byte[] temp=null; 
		try {
			
			src=new SortResponseControl(Id,crit,ber);
			temp=src.getEncodedValue();
		} catch (IOException e) {
			fail("The bytes are in the ASN.1 BER.");
		}
		for(int i=0;i<ber.length;i++){
			ber[i]=0;
		
		}
		for(int i=0;i<ber.length;i++){
			assertSame(ber[i],src.getEncodedValue()[i]);
		}
	}


	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.isSorted()'</p>
	 * <p>Here we are testing if the isSorted method returns the correct answer for sorted results.</p>
	 * <p>The expected result in this case is true.</p>
	 */
	public void testIsSorted001() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,0});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		assertTrue(src.isSorted());
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.isSorted()'</p>
	 * <p>Here we are testing if the isSorted method returns the correct answer for unsorted results.</p>
	 * <p>The expected result in this case is false.</p>
	 */
	public void testIsSorted002() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",false,new byte[]{48,6,10,1,16,4,1,32});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		assertFalse(src.isSorted());
		
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getResultCode()'</p>
	 * <p>Here we are testing if the correct result code is returned.</p>
	 * <p>The expected result is zero.</p>
	 */
	public void testGetResultCode001() {
		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,0});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		assertEquals(0,src.getResultCode());
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getResultCode()'</p>
	 * <p>Here we are testing if the correct result code is returned. Here the sort key does not exist
	 * so the error must be no such attribute.</p>
	 * <p>The expected result is 16.</p>
	 */
	public void testGetResultCode002() {
		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,6,10,1,16,4,1,32});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		assertEquals(16,src.getResultCode());
		
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getResultCode()'</p>
	 * <p>Here we are testing if the correct result code is returned. Here the sort key does exist
	 * but the error must be unwillingToPerform.</p>
	 * <p>The expected result is 53.</p>
	 */
	public void testGetResultCode003() {
		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,7,10,1,53,4,2,67,78});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		assertEquals(53,src.getResultCode());
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getResultCode()'</p>
	 * <p>Here we are testing if the correct result code is returned. Here the sort key does exist
	 * but the error must be unwillingToPerform.</p>
	 * <p>The expected result is 100.</p>
	 */
	public void testGetResultCode004() {
		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,7,10,1,100,4,2,67,78});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		assertEquals(100,src.getResultCode());
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getAttributeID()'</p>
	 * <p>Here we are testing if this method gets the correct attribute ID.</p>
	 * <p>The expected result is in this case is null because no ID was returned by the server.</p>
	 */
	public void testGetAttributeID001() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,80});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		assertEquals(null,src.getAttributeID());
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null naming exception.</p> 
	 */
	public void testGetException001() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,80});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		
		if(src.getException()!=null&&src.getException() instanceof NamingException){
			
		}else{
			fail("The exception must be like NamingException");
		}
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null NoSuchAttributeException.</p>
	 */
	public void testGetException002() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,16});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		
		if(src.getException()!=null&&src.getException() instanceof NoSuchAttributeException){
			
		}else{
			fail("The exception must be like NoSuchAttributeException");
		}
		
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a null exception.</p>
	 */
	public void testGetException003() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,0});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		
		if(src.getException()!=null)fail("The exception must be null");
		
				
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null naming exception.</p>
	 */
	public void testGetException004() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,3});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		
		if(src.getException()!=null&&src.getException() instanceof TimeLimitExceededException){
			
		}else{
			fail("The exception must be like NamingException");
		}
		
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null naming exception.</p>
	 */
	public void testGetException005() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,8});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		
		if(src.getException()!=null&&src.getException() instanceof AuthenticationNotSupportedException){
			
		}else{
			fail("The exception must be like NamingException");
		}
		
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null naming exception.</p>
	 */
	public void testGetException006() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,11});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		
		if(src.getException()!=null&&src.getException() instanceof LimitExceededException){
			
		}else{
			fail("The exception must be like NamingException");
		}
		
		
	}
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null naming exception.</p>
	 */
	public void testGetException007() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,18});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		
		if(src.getException()!=null&&src.getException() instanceof InvalidSearchFilterException){
			
		}else{
			fail("The exception must be like NamingException");
		}
		
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null naming exception.</p>
	 */
	public void testGetException008() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,50});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		
		if(src.getException()!=null&&src.getException() instanceof NoPermissionException){
			
		}else{
			fail("The exception must be like NamingException");
		}
		
		
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null naming exception.</p>
	 */
	public void testGetException009() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,51});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		
		if(src.getException()!=null&&src.getException() instanceof ServiceUnavailableException){
			
		}else{
			fail("The exception must be like NamingException");
		}
		
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null naming exception.</p>
	 */
	public void testGetException010() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,53});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
		
		if(src.getException()!=null&&src.getException() instanceof OperationNotSupportedException){
			
		}else{
			fail("The exception must be like NamingException");
		}
		
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null naming exception.</p>
	 */
	public void testGetException011() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,3,10,1,1});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
	
		if(src.getException()!=null&&src.getException() instanceof NamingException){
			
		}else{
			fail("The exception must be like NamingException");
		}
		
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.SortResponseControl.getException()'</p>
	 * <p>Here we are testing if this method gets the correct NamingException.</p>
	 * <p>The expected result is a not null naming exception.</p>
	 */
	public void testGetException012() {

		SortResponseControl src=null;
		try{
			src=new SortResponseControl("test",true,new byte[]{48,7,10,1,100,4,2,67,78});
		}catch (IOException e) {
			fail("Failed with:"+e);
		}
	
		if(src.getException()!=null&&src.getException() instanceof NamingException){
			
		}else{
			fail("The exception must be like NamingException");
		}
		
		
	}

    public void testSerializationCompatibility() throws Exception{
        String Id = "test";
        boolean crit = true;
        SortResponseControl object;
        byte[] ber = { 48, 3, 10, 1, 0 };
        object = new SortResponseControl(Id, crit, ber);
        SerializationTest.verifyGolden(this, object, SORTRESPONSECONTROL_COMPARATOR);
    }

    // comparator for SortResponseControl
    private static final SerializableAssert SORTRESPONSECONTROL_COMPARATOR = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            SortResponseControl initThr = (SortResponseControl) initial;
            SortResponseControl dserThr = (SortResponseControl) deserialized;

            // verify ResultCode
            int initResultCode = initThr.getResultCode();
            int dserResultCode = dserThr.getResultCode();
            assertTrue(initResultCode == dserResultCode);
            
            // verify BadAttrId
            String initBadAttrId = initThr.getAttributeID();
            String dserBadAttrId = dserThr.getAttributeID();
            assertEquals(initBadAttrId, dserBadAttrId);
        }
    };
}
