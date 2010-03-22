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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.naming.ldap.SortKey;
import junit.framework.TestCase;

/**
 * <p>This class test is made to test all cases of package where the coverage was not 100%.</p>
 * <p>We are going to find here a lot cases from different classes, notice here that the conventional structure
 * followed in the rest of the project is applied  here.</p>
 * 
 */
public class TestLdapNameWhiteBoxDevelopment extends TestCase {

	public static void main(String[] args) {
	}

	public TestLdapNameWhiteBoxDevelopment(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.add(int, String)'</p>
	 * <p>Here we are testing if this method adds a single component at a specified position within this LDAP name.</p>
	 * <p>Here we are testing if a naming exception is thrown if a name is trying of be added.</p>
	 */
	public void testAddAll(){
		try {
			LdapName x=new LdapName("t=test");
			x.addAll(0,new CompositeName());
			assertNotNull(x);
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
		
	}
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.add(int, String)'</p>
	 * <p>Here we are testing if this method adds a single component at a specified position within this LDAP name.</p>
	 * <p>Here we are testing if a naming exception is thrown if a name is trying of be added.</p>
	 */
	public void testAddAll001(){
		try {
			LdapName x=new LdapName("t=test");
			x.addAll(1,new CompositeName());
			assertNotNull(x);
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
		
	}
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.add(int, String)'</p>
	 * <p>Here we are testing if this method adds a single component at a specified position within this LDAP name.</p>
	 * <p>Here we are testing if a naming exception is thrown if a name is trying of be added.</p>
	 */
	public void testAddAll002(){
		try {
			LdapName x=new LdapName("t=test");
			x.addAll(1,new CompoundName("",new Properties()));
			assertNotNull(x);
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
		
	}
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.add(int, String)'</p>
	 * <p>Here we are testing if this method adds a single component at a specified position within this LDAP name.</p>
	 * <p>Here we are testing if a naming exception is thrown if a name is trying of be added.</p>
	 */
	public void testAddAll003(){
		try {
			LdapName x=new LdapName("t=test");
			x.addAll(1,new CompositeName("/"));
			assertNotNull(x);
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.clone()'</p>
	 * <p>Here we are testing if a clone of an object of LdapName is equal to the original.</p>
	 * <p>The expected result in this case is true.</p>
	 */
	public void testClone001() {
		
		LdapName ln;
		try {
			List<Rdn> lista = new ArrayList<Rdn>();
			Rdn x=new Rdn("t"," ");
			lista.add(x);
			ln = new LdapName(lista);
			LdapName cloned=(LdapName) ln.clone();
			assertNotNull(cloned);
			assertEquals(cloned,ln);			
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.clone()'</p>
	 * <p>Here we are testing if a clone of an object of LdapName is equal to the original.</p>
	 * <p>The expected result in this case is true.</p>
	 */
	public void testClone002() {
		
		LdapName ln;
		try {
			List<Rdn> lista = new ArrayList<Rdn>();
			Rdn x=new Rdn("t","asd");
			lista.add(x);
			ln = new LdapName(lista);
			LdapName cloned=(LdapName) ln.clone();
			assertNotNull(cloned);
			assertTrue(cloned.equals(ln));
			assertEquals(cloned.getRdns().hashCode(),ln.getRdns().hashCode());
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}catch (ClassCastException e) {
			fail("Failed with:"+e);
		}
		
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.compareTo(Object)'</p>
	 * <p>Here we are testing if this method compares this LdapName with the specified Object for order.</p>
	 * <p>The expected result is a classcastException.</p>
	 */
	public void testCompareTo001() {
		
		try {
			LdapName ln = new LdapName("t=test,cn=common");
			Rdn tocomp=null;
			int i;
			i = ln.compareTo(tocomp);
			fail("The string is null.");
		} catch (ClassCastException e) {
			
		} catch (Throwable e) { 
			fail("Failed with: "+e); 
					
		}

	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.compareTo(Object)'</p>
	 * <p>Here we are testing if this method compares this LdapName with the specified Object for order.</p>
	 * <p>The expected result is a classcastException.</p>
	 */
	public void testCompareTo002() {
		
		try {
			LdapName ln = new LdapName("t=test,cn=common");
			int i;
			i = ln.compareTo(new SortKey(""));
			fail("The string is null.");
		} catch (ClassCastException e) {
			
		} catch (Throwable e) { 
			fail("Failed with: "+e); 
					
		}

	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.endsWith(Name)'</p>
	 * <p>Here we are testing if this method determines whether this LDAP name ends with a specified LDAP name suffix.</p>
	 * <p>The expected result is a false.</p>
	 */
	public void testEndsWithName001() {
		
		try {
			LdapName ln=new LdapName("t=test,t=test");
			LdapName t=new LdapName("t=test,t=etest");
			assertFalse(ln.endsWith(t.getRdns()));
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}

	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.getAll()'</p>
	 * <p>Here we are testing if this method retrieves the components of this name as an enumeration of strings.</p>
	 * <p>The expected result is if an empty name returns a non-null enumeration.</p>
	 */
	public void testGetAll001() {

		try {
			LdapName ln=new LdapName("");
			Enumeration<String> x=ln.getAll();
			assertNotNull(x);
			assertEquals(x.hasMoreElements(),ln.getRdns().iterator().hasNext());
		} catch (InvalidNameException e) {
			
		}
	}
	
	
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.startsWith(List<Rdn>)'</p>
	 * <p>Here we are testing if this method determines whether the specified RDN sequence forms a prefix of this LDAP name.</p>
	 * <p>The expected result is false.</p>
	 */
	public void testStartsWithListOfRdn004() {
		
		LinkedList<Rdn> test=new LinkedList<Rdn>();
		try {
			test.add(new Rdn("t=test"));
			LdapName x=new LdapName("t=t");
			assertFalse(x.startsWith(test));
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}

	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.LdapName.equals(Object)'</p>
	 * <p>Here we are testing if this method determines whether the specified object is equal to the originaly one.</p>
	 * <p>The expected result is false.</p>
	 */
	public void testequals(){
		LinkedList<Rdn> test=new LinkedList<Rdn>();
		LinkedList<Rdn> test1=new LinkedList<Rdn>();
		try {
			test.add(new Rdn("t=test"));
			test1.add(new Rdn("t=test"));
			test1.add(new Rdn("t=test"));
			LdapName x=new LdapName(test);
			LdapName y=new LdapName(test1);
			assertFalse(x.equals(y));
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
	}
	
}
