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

import java.util.Arrays;

import javax.naming.InvalidNameException;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.Rdn;

import junit.framework.TestCase;

/**
 * <p>This class is use to test the integration of the class LdapName, notice here that is only one test
 * that all the individuals assertion are made inside of it, in addition to give here four methods where we divided
 * in sets of methods to test. </p>  
 * 
 */
public class TestRdnIntegration extends TestCase {

	/**
	 * <p>This method is not implemted.</p>
	 * @param args Possible parameter to help us initiate all tests.
	 */
	public static void main(String[] args) {
	}

	public TestRdnIntegration(String name) {
		super(name);
	}


	/**
	 * <p>Constructor method of the test class.</p>
	 * <p>Here in this case we initiate the inherited constructor, and set the private attributes of the class.</p>
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	/**
	 * <p>Test method to test the complete integration of the class.</p>
	 *
	 */
	public void testIntegration(){
		try {
			//Rdn auxiliary
			Rdn testAux=new Rdn("");
			byte[] aux=new byte[]{116,101,115,116};//this is equal to say "test" in ascii.
			MyBasicAttributes set = new MyBasicAttributes("t","test");
								
			//Rdn to test integration, notice here we are using all constructors.
			Rdn x1=new Rdn("t=test");
			Rdn x2=new Rdn(x1);
			Rdn x3=new Rdn("t",aux);
			Rdn x4=new Rdn(set);
			
			//Checking all rdn if they are not null.
			assertNotNull("Should be not null.",x1);
			assertNotNull("Should be not null.",x2);
			assertNotNull("Should be not null.",x3);
			assertNotNull("Should be not null.",x4);
			
			//Checking all methods of the class.
			//Testing getValue().
			assertEquals("test",x1.getValue());
			assertEquals("test",x2.getValue());
			byte[] aux2=(byte[]) x3.getValue();
			for(int i=0;i<aux.length;i++){
				assertEquals(aux[i],aux2[i]);
			}
			assertEquals(set.getValue().toString(),x4.getValue().toString());
//			Testing getType().
			assertEquals("t",x1.getType());
			assertEquals("t",x2.getType());
			assertEquals("t",x3.getType());
			assertEquals(set.getIDs().nextElement().toString(),x4.getType());
//			Testing toString().
			assertEquals("t=test",x1.toString());
			assertEquals("t=test",x2.toString());
			assertEquals("t=#74657374",x3.toString());
			assertEquals("t=test",x4.toString());
//			Testing compareTo().
			assertEquals(0,x1.compareTo(x2)&x2.compareTo(x3)&x3.compareTo(x4)&x4.compareTo(x1));
			assertTrue(0<x1.compareTo(testAux));
			assertTrue(0>testAux.compareTo(x1));
			assertTrue(0<x2.compareTo(testAux));
			assertTrue(0>testAux.compareTo(x2));
			assertTrue(0<x3.compareTo(testAux));
			assertTrue(0>testAux.compareTo(x3));
			assertTrue(0<x4.compareTo(testAux));
			assertTrue(0>testAux.compareTo(x4));
//			Testing equals().
			assertTrue("Should be equals.",x1.equals(x2));
			assertFalse("Should not be equals.",x2.equals(x3));
			assertFalse("Should not be equals.",x3.equals(x4));
			assertTrue("Should be equals.",x4.equals(x1));
			assertFalse(x1.equals(testAux));
			assertFalse(x2.equals(testAux));
			assertFalse(x3.equals(testAux));
			assertFalse(x4.equals(testAux));
//			Testing escapeValue().
			assertEquals("#74657374",Rdn.escapeValue(aux));
			assertEquals("test\\, this",Rdn.escapeValue("test, this"));
//			Testing unescapeValue().
			assertEquals("[116, 101, 115, 116]",Arrays.toString((byte[])Rdn.unescapeValue("#74657374")));
			assertEquals("test, this",Rdn.unescapeValue("test\\, this"));
//			Testing hascode().
			assertEquals(0,testAux.hashCode());
			assertTrue(0!=x1.hashCode());
			assertTrue(0!=x2.hashCode());
			assertTrue(0!=x3.hashCode());
			assertTrue(0!=x4.hashCode());
//			Testing size()
			assertEquals(1,x1.size());
			assertEquals(1,x2.size());
			assertEquals(1,x3.size());
			assertEquals(1,x4.size());
//			Testing toAttributes()
            assertEquals(0,x2.toAttributes().toString().compareToIgnoreCase(x1.toAttributes().toString()));
            assertEquals(0,x4.toAttributes().toString().compareToIgnoreCase(x1.toAttributes().toString()));
			assertNotNull(x3.toAttributes());
			
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
	}
			
	/*
	 * Class to help us to test the integration.
	 */
	private class MyBasicAttributes extends BasicAttributes {
		
		private static final long serialVersionUID = 1L;
		private Object myo;
		MyBasicAttributes(String x,Object o){
			super(x,o);
			this.myo=o;
		}
		public Object getValue(){
			return myo;
		}
	}
	
}
