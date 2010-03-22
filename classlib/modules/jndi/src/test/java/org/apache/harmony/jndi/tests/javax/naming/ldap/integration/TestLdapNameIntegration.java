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
package org.apache.harmony.jndi.tests.javax.naming.ldap.integration;

import java.util.ArrayList;
import java.util.Enumeration;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import junit.framework.TestCase;

/**
 * <p>This class is use to test the integration of the class LdapName, notice here that is only one test
 * that all the individuals assertion are made inside of it, in addition to give here four methods where we divided
 * in sets of methods to test. </p>  
 * 
 */
public class TestLdapNameIntegration extends TestCase {

	/**
	 * Attribute to help us to test the complete integration of the class.
	 */
	private Rdn aux;
	/**
	 * Attribute to help us to test the complete integration of the class.
	 */
	private ArrayList<Rdn> list=null;
	
	/**
	 * <p>This method is not implemted.</p>
	 * @param args Possible parameter to help us initiate all tests.
	 */
	public static void main(String[] args) {
	}

	public TestLdapNameIntegration(String name) {
		super(name);
	}

	/**
	 * <p>Constructor method of the test class.</p>
	 * <p>Here in this case we initiate the inherited constructor, and set the private attributes of the class.</p>
	 */
	protected void setUp() throws Exception {
		super.setUp();
		aux=new Rdn("t=test");
		list=new ArrayList<Rdn>();
		
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * <p>Method to test all the add methods.</p>
	 *
	 */
	public void testIntegrationConstructorAndAddMethods(){
		
		try {
			
			//Testing Constructor
			LdapName temp=new LdapName("t=test");
			LdapName temp2=new LdapName(list);
			list.add(aux);
			LdapName temp3=new LdapName(list);
			assertNotNull(temp);
			assertNotNull(temp2);
			assertNotNull(temp3);
			
			//Testing Methods
			temp.add(0,aux);
			temp.add(0,"f=flag");
			temp.add(aux);
			temp.add("f=flag");
			temp.addAll(0,list);
			temp.addAll(0,temp2);
			temp.addAll(list);
			temp.addAll(temp2);

			
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
			
	}
	
	/**
	 * <p>Method to test all the add methods in addition to the test of 
	 * compareTo,equals,endsWith,startsWith.</p>
	 *
	 */
	public void testIntegrationConstructorAndMethods(){
		
		try {
			
			//Testing Constructor
			LdapName temp=new LdapName("t=test");
			LdapName temp2=new LdapName(list);
			list.add(aux);
			LdapName temp3=new LdapName(list);
			assertNotNull(temp);
			assertNotNull(temp2);
			assertNotNull(temp3);
			
			//Testing Add Methods
			temp.add(0,aux);
			temp.add(0,"f=flag");
			temp.add(aux);
			temp.add("f=flag");
			temp.addAll(0,list);
			temp.addAll(0,temp2);
			temp.addAll(list);
			temp.addAll(temp2);

			
			//Testing Clone, Equlas and CompareTo 
			if(!temp.clone().getClass().equals(temp.getClass()))fail("Fail.");
			assertEquals(0,temp.compareTo(temp));
			assertTrue(temp.compareTo(temp2)>0);
			assertTrue(temp.compareTo(temp3)>0);
			assertFalse(temp.equals(null));
			assertTrue(temp.equals(temp));
			assertFalse(temp.equals(temp2));
			
			//Testing SartWith and EndWith methods
			assertTrue(temp.endsWith(list));
			assertTrue(temp.endsWith(temp2));
			assertTrue(temp.startsWith(list));
			assertTrue(temp.startsWith(temp2));
			
			
		} catch (InvalidNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}
	
	/**
	 * <p>Method to test all the add methods in addition to the test of get methods.</p>
	 *
	 */
	public void testIntegrationConstructorAndMethods001(){
		
		try {
			
			//Testing Constructor
			LdapName temp=new LdapName("t=test");
			LdapName temp2=new LdapName(list);
			list.add(aux);
			LdapName temp3=new LdapName(list);
			assertNotNull(temp);
			assertNotNull(temp2);
			assertNotNull(temp3);
			
			//Testing Add Methods
			temp.add(0,aux);
			temp.add(0,"f=flag");
			temp.add(aux);
			temp.add("f=flag");
			temp.addAll(0,list);
			temp.addAll(0,temp2);
			temp.addAll(list);
			temp.addAll(temp2);

			
			//Testing get methods 
			LdapName en=(LdapName) temp.clone();
			assertEquals("t=test",temp.get(0));
			assertNotNull(temp.getAll());
			Enumeration<String> enume=temp.getAll();
			Enumeration<String> enume2=en.getAll();
			while(enume.hasMoreElements()&&enume2.hasMoreElements()){
				if(enume.nextElement().compareTo(enume2.nextElement())!=0)fail("Fail.");
				
			}
			if(!temp.getClass().equals(temp2.getClass()))fail("Fail.");
			assertTrue(temp.startsWith(temp.getPrefix(0)));
			assertTrue(temp.endsWith(temp.getSuffix(0)));
			assertTrue(temp.endsWith(temp.getRdns()));
			assertTrue(temp.getRdn(0).equals(aux));
			
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
			
	}
	
	/**
	 * <p>Method to test all the add methods in addition to the test of the remaining methods.</p>
	 *
	 */
	public void testIntegrationConstructorAndMethods002(){
		
		try {
			
			//Testing Constructor
			LdapName temp=new LdapName("t=test");
			LdapName temp2=new LdapName(list);
			list.add(aux);
			LdapName temp3=new LdapName(list);
			assertNotNull(temp);
			assertNotNull(temp2);
			assertNotNull(temp3);
			
			//Testing Add Methods
			temp.add(0,aux);
			temp.add(0,"f=flag");
			temp.add(aux);
			temp.add("f=flag");
			temp.addAll(0,list);
			temp.addAll(0,temp2);
			temp.addAll(list);
			temp.addAll(temp2);

			
			//Testing methods 
			assertTrue(temp.hashCode()!=0);
			assertFalse(temp.isEmpty());
			assertNotNull(temp.remove(0));
			assertTrue(temp.size()!=0);
			while(temp.size()>0){
				assertNotNull(temp.remove(temp.size()-1));
			}
			assertTrue(temp.isEmpty());
			
			//Testing String
			String x="t=test,T=TEST";
			LdapName temp4=new LdapName(x);
			assertEquals(x.getBytes().length,temp4.toString().getBytes().length);
			
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
			
	}
	
	/**
	 * <p>Test method to test the complete integration of the class.</p>
	 *
	 */
	public void testCompleteIntegration(){
		try {
			
			//Testing Constructor
			LdapName temp=new LdapName("t=test");
			LdapName temp2=new LdapName(list);
			list.add(aux);
			LdapName temp3=new LdapName(list);
			assertNotNull(temp);
			assertNotNull(temp2);
			assertNotNull(temp3);
			
			//Testing Add Methods
			temp.add(0,aux);
			temp.add(0,"f=flag");
			temp.add(aux);
			temp.add("f=flag");
			temp.addAll(0,list);
			temp.addAll(0,temp2);
			temp.addAll(list);
			temp.addAll(temp2);

			//Testing String
			String x="t=test,T=TEST";
			LdapName temp4=new LdapName(x);
			assertEquals(x.getBytes().length,temp4.toString().getBytes().length);
			
			//Testing get methods 
			LdapName en=(LdapName) temp.clone();
			assertEquals("t=test",temp.get(0));
			assertNotNull(temp.getAll());
			Enumeration<String> enume=temp.getAll();
			Enumeration<String> enume2=en.getAll();
			while(enume.hasMoreElements()&&enume2.hasMoreElements()){
				if(enume.nextElement().compareTo(enume2.nextElement())!=0)fail("Fail.");
				
			}
			if(!temp.getClass().equals(temp2.getClass()))fail("Fail.");
			assertTrue(temp.startsWith(temp.getPrefix(0)));
			assertTrue(temp.endsWith(temp.getSuffix(0)));
			assertTrue(temp.endsWith(temp.getRdns()));
			assertTrue(temp.getRdn(0).equals(aux));
			
			//Testing Clone, Equlas and CompareTo 
			if(!temp.clone().getClass().equals(temp.getClass()))fail("Fail.");
			assertEquals(0,temp.compareTo(temp));
			assertTrue(temp.compareTo(temp2)>0);
			assertTrue(temp.compareTo(temp3)>0);
			assertFalse(temp.equals(null));
			assertTrue(temp.equals(temp));
			assertFalse(temp.equals(temp2));
			
			//Testing SartWith and EndWith methods
			assertTrue(temp.endsWith(list));
			assertTrue(temp.endsWith(temp2));
			assertTrue(temp.startsWith(list));
			assertTrue(temp.startsWith(temp2));
			
			//Testing methods 
			assertTrue(temp.hashCode()!=0);
			assertFalse(temp.isEmpty());
			assertNotNull(temp.remove(0));
			assertTrue(temp.size()!=0);
			while(temp.size()>0){
				assertNotNull(temp.remove(temp.size()-1));
			}
			assertTrue(temp.isEmpty());
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
	}
}
