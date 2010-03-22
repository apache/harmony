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
package org.apache.harmony.jndi.tests.javax.naming.ldap.extras;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import javax.naming.ldap.BasicControl;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.ManageReferralControl;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.Rdn;
import javax.naming.ldap.SortControl;
import javax.naming.ldap.SortResponseControl;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap.MockLdapReferralException;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap.MockStartTlsResponse;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap.MockUnsolicitedNotification;
import junit.framework.TestCase;

/**
 * <p>This class is for test all the serializables classes</p>
 *
 */
public class TestSerialization extends TestCase {

	public static void main(String[] args) {
	}

	public TestSerialization(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test for the class javax.naming.ldap.BasicControl
	 *
	 */
	public void testBasicControl(){

		BasicControl bc=new BasicControl("test");
		BasicControl bc2=null;
		try{
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(bc); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			bc2 = (BasicControl) in.readObject(); 
			in.close(); 
			assertEquals(bc.getID(),bc2.getID());
		}catch (Exception e) {
			fail("Failed with:"+e);
		}
		
	}
	
	/**
	 * Test for the class javax.naming.ldap.LdapName
	 *
	 */
	public void testLdapName(){

		
		try{
			LdapName ln=new LdapName("t=test");
			LdapName ln2=null;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(ln); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			ln2 = (LdapName) in.readObject(); 
			in.close(); 
			assertEquals(ln,ln2);
		}catch (Exception e) {
			fail("Failed with:"+e);
		}

	}
	
	/**
	 * Test for the class javax.naming.ldap.ManageReferralControl
	 *
	 */
	public void testManageReferralControl(){
		
		ManageReferralControl mrc=new ManageReferralControl();
		ManageReferralControl mrc2=null;
		try{
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(mrc); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			mrc2 = (ManageReferralControl) in.readObject(); 
			in.close(); 
			assertEquals(mrc.getID(),mrc2.getID());
		}catch (Exception e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * Test for the class javax.naming.ldap.ResponseControl
	 *
	 */
	public void testPagedResultsResponseControl(){
		
		try{
			PagedResultsControl prc=new PagedResultsControl(0, false);
			PagedResultsControl prc2=null;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(prc); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			prc2 = (PagedResultsControl) in.readObject(); 
			in.close(); 
			assertEquals(prc.getID(),prc2.getID());
		}catch (Exception e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * Test for the class javax.naming.ldap.Rdn
	 *
	 */
	public void testRdn(){
		
		try{
			Rdn rdn=new Rdn("");
			Rdn rdn2=null;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(rdn); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			rdn2 = (Rdn) in.readObject(); 
			in.close(); 
			assertEquals(rdn,rdn2);
		}catch (Exception e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * Test for the class javax.naming.ldap.SortControl
	 *
	 */
	public void testSortControl(){
		try{
			SortControl sc=new SortControl("", false);
			SortControl sc2=null;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(sc); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			sc2 = (SortControl) in.readObject(); 
			in.close(); 
			assertEquals(sc.getID(),sc2.getID());
		}catch (Exception e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * Test for the class javax.naming.ldap.SortResponseControl
	 *
	 */
	public void testSortResponseControl(){
		try{
			SortResponseControl src=new SortResponseControl("", false, new byte[]{48,3,10,1,0});
			SortResponseControl src2=null;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(src); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			src2 = (SortResponseControl) in.readObject(); 
			in.close(); 
			assertEquals(src.getID(),src2.getID());
		}catch (Exception e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * Test for the class javax.naming.ldap.StartTlsRequest
	 *
	 */
	public void testStartTlsRequest(){
		
			StartTlsRequest str=new StartTlsRequest();
			StartTlsRequest str2=null;
		try{
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(str); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			str2 = (StartTlsRequest) in.readObject(); 
			in.close(); 
			assertEquals(str.getID(),str2.getID());
		}catch (Exception e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * Test for the class javax.naming.ldap.StartTlsResponse
	 *
	 */
	public void testStartTlsResponse(){

			MockStartTlsResponse str=new MockStartTlsResponse();
			MockStartTlsResponse str2=null;
		try{
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(str); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			str2 = (MockStartTlsResponse) in.readObject(); 
			in.close(); 
			assertEquals(str.getID(),str2.getID());
		}catch (Exception e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * Test for the class javax.naming.ldap.UnsolicitedNotificationEvent
	 *
	 */
	public void testUnsolicitedNotificationEvent(){
			MockUnsolicitedNotification mun=new MockUnsolicitedNotification();
			UnsolicitedNotificationEvent une=new UnsolicitedNotificationEvent(new Object(), mun);
			UnsolicitedNotificationEvent une2=null;
		try{
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(une); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			une2 = (UnsolicitedNotificationEvent) in.readObject(); 
			in.close(); 
			assertEquals(((MockUnsolicitedNotification)une.getNotification()).getIDSerial(),((MockUnsolicitedNotification)une2.getNotification()).getIDSerial());
			
		}catch (Exception e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * Test for the class javax.naming.ldap.LdapReferralException
	 *
	 */
	public void testLdapReferralException(){
			
			MockLdapReferralException mlre=new MockLdapReferralException();
			MockLdapReferralException mlre2=null;
		try{
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(buffer); 
			out.writeObject(mlre); 
			out.close(); 
			ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
			mlre2 = (MockLdapReferralException) in.readObject(); 
			in.close(); 
			assertEquals(mlre.getExplanation(),mlre2.getExplanation());
		}catch (Exception e) {
			fail("Failed with:"+e);
		}
	}
}
