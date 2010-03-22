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
import java.util.Arrays;
import javax.naming.ldap.PagedResultsResponseControl;
import junit.framework.TestCase;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

/**        
 * <p>This Test class is testing the PagedResultsControls class.</p>
 * <p>In the next tables we are going to see the methods that we test in this class:</p>
 * <table class="t" cellspacing="0">
	<tbody><th>Constructors:</th>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="60" name="sas9nt21" readonly="readonly" value="PagedResultsResponseControl(String id, boolean criticality, byte[] value)" id="f10"></td>
			
		</tr>
		</tbody></table>
		<table>
		<tbody><th>Method Summary:</th>
		<tr><TD>Return</TD><TD>Method</TD></tr>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="int" id="f00"></TD>
			<td class="c0" id="c10"><input class="a0" size="65" name="sas9nt21" readonly="readonly" value="getResultSize()" id="f10"></td>
			
		</tr>

		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="byte[]" id="f00"></TD>
			<td class="c0" id="c10"><input class="a0" size="65" name="sas9nt21" readonly="readonly" value="getCookie()" id="f10"></td>
			
		</tr>
		      
	</tbody></table>	
 *
 */
public class TestPagedResultsResponseControl extends TestCase {

	/**
	 * <p>This method is not implemted.</p>
	 * @param args Possible parameter to help us initiate all tests.
	 */
	public static void main(String[] args) {
	}

	public TestPagedResultsResponseControl(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.PagedResultsResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing if this method constructs a paged-results response control.</p>
	 * <p>The expected result is a Nullpointerexception.</p>
	 */
	public void testPagedResultsResponseControl001() {

		try {
			PagedResultsResponseControl x=new PagedResultsResponseControl(null,false,null);
			fail("Arguments can not be null.");
		} catch (NullPointerException e) {
			
		} catch (IOException e) {
			fail("A null pointer exception must be thrown.");
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.PagedResultsResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing if this method constructs a paged-results response control.</p>
	 * <p>The expected result is a Nullpointerexception.</p>
	 */
	public void testPagedResultsResponseControl002() {

		try {
			PagedResultsResponseControl x=new PagedResultsResponseControl(null,true,null);
			fail("Arguments can not be null.");
		} catch (NullPointerException e) {
			
		} catch (IOException e) {
			fail("A null pointer exception must be thrown.");
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.PagedResultsResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing if this method constructs a paged-results response control.</p>
	 * <p>The expected result is a IOexception.</p>
	 */
	public void testPagedResultsResponseControl003() {

		try {
			byte[] b={};
			PagedResultsResponseControl x=new PagedResultsResponseControl(null,false,b);
			fail("The byte array must not be empty.");
		} catch (IOException e) {
			
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.PagedResultsResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing if this method constructs a paged-results response control.</p>
	 * <p>The expected result is a IOexception.</p>
	 */
	public void testPagedResultsResponseControl004() {

		try {
			byte[] b={};
			PagedResultsResponseControl x=new PagedResultsResponseControl(null,true,b);
			fail("The byte array must not be empty.");
		} catch (IOException e) {
			
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.PagedResultsResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing if this method constructs a paged-results response control.</p>
	 * <p>The expected result is a IOexception.</p>
	 */
	public void testPagedResultsResponseControl005() {

		try {
			byte[] b={1,2,3,4,0};
			PagedResultsResponseControl x=new PagedResultsResponseControl(null,false,b);
			fail("The byte array is not ok.");
		} catch (IOException e) {
			
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.PagedResultsResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing if this method constructs a paged-results response control.</p>
	 * <p>The expected result is a IOexception.</p>
	 */
	public void testPagedResultsResponseControl006() {

		try {
			byte[] b={1,2,3,4,0};
			PagedResultsResponseControl x=new PagedResultsResponseControl(null,true,b);
			fail("The byte array is not ok.");
		} catch (IOException e) {
			
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.PagedResultsResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing if this method constructs a paged-results response control.</p>
	 * <p>The expected result is an instance of the class.</p>
	 */
	public void testPagedResultsResponseControl007() {

		try {
			byte[] b={48,5,2,1,0,4,0};
			PagedResultsResponseControl x=new PagedResultsResponseControl(null,true,b);
			
		} catch (IOException e) {
			fail("The arguments are ok.");
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.PagedResultsResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing if this method constructs a paged-results response control.</p>
	 * <p>The expected result is an instance of the class.</p>
	 */
	public void testPagedResultsResponseControl008() {

		try {
			byte[] b={48,5,2,1,0,4,0};
			PagedResultsResponseControl x=new PagedResultsResponseControl(null,false,b);
			
		} catch (IOException e) {
			fail("The arguments are ok.");
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.PagedResultsResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing if this method constructs a paged-results response control.</p>
	 * <p>The expected result is an instance of the class.</p>
	 */
	public void testPagedResultsResponseControl010() {

		try {
			byte[] b={48,5,2,1,0,4,0};
			PagedResultsResponseControl x=new PagedResultsResponseControl("",false,b);
			
		} catch (IOException e) {
			fail("The arguments are ok.");
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.PagedResultsResponseControl(String, boolean, byte[])'</p>
	 * <p>Here we are testing if this method constructs a paged-results response control.</p>
	 * <p>The expected result is that the byte array must be copy not clone so any chance in original affect the response.</p>
	 */
	public void testPagedResultsResponseControl011() {

		try {
			byte[] b={48,5,2,1,0,4,0};
			byte[] c={48,5,2,1,0,4,0};
			PagedResultsResponseControl x=new PagedResultsResponseControl("",false,b);
			byte[] aux = x.getEncodedValue().clone();
			for (int i = 0; i < b.length; i++) {
				b[i]=1;
			}
			for (int i = 0; i < b.length; i++) {
				assertSame(c[i],aux[i]);
				assertSame(b[i],x.getEncodedValue()[i]);
			}
		} catch (IOException e) {
			fail("The arguments are ok.");
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.getResultSize()'</p>
	 * <p>Here we are testing if this method retrieves (an estimate of) the number of entries in the search result.</p>
	 * <p>The expected result is in this case zero.</p> 
	 */
	public void testGetResultSize001() {

		byte[] b={48,5,2,1,0,4,0};
		try {
			PagedResultsResponseControl x=new PagedResultsResponseControl("",false,b);
			assertEquals(0,x.getResultSize());
		} catch (IOException e) {
			fail("Failed with:"+e);
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.getResultSize()'</p>
	 * <p>Here we are testing if this method retrieves (an estimate of) the number of entries in the search result.</p>
	 * <p>The expected result is in this case zero.</p> 
	 */
	public void testGetResultSize002() {

		byte[] b={48,5,2,1,10,4,0};
		try {
			PagedResultsResponseControl x=new PagedResultsResponseControl("",false,b);
			assertEquals(10,x.getResultSize());
		} catch (IOException e) {
			fail("Failed with:"+e);
		}
	}

	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.getCookie()'</p>
	 * <p>Here we are testing if this method retrieves the server-generated cookie.</p>
	 * <p>The expected result in this case is null.</p>
	 */
	public void testGetCookie001() {

		byte[] b={48,5,2,1,0,4,0};
		try {
			PagedResultsResponseControl x=new PagedResultsResponseControl("",false,b);
			assertEquals(null,x.getCookie());
		} catch (IOException e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.getCookie()'</p>
	 * <p>Here we are testing if this method retrieves the server-generated cookie.</p>
	 * <p>The expected result in this case is a not null array.</p>
	 */
	public void testGetCookie002() {

		byte[] b={48,7,2,1,0,4,2,1,1};
		byte[] c={1,1};
		try {
			PagedResultsResponseControl x=new PagedResultsResponseControl("",false,b);
			for (int i = 0; i < x.getCookie().length; i++) {
				assertEquals(c[i],x.getCookie()[i]);
			}	
		} catch (IOException e) {
			fail("Failed with:"+e);
		}
	}
	
	/**
	 * <p>Test method for 'javax.naming.ldap.PagedResultsResponseControl.getCookie()'</p>
	 * <p>Here we are testing if this method retrieves the server-generated cookie.</p>
	 * <p>The expected result in this case is not null.</p>
	 */
	public void testGetCookie003() {

		byte[] b={48,6,2,1,0,4,1,0};
		try {
			PagedResultsResponseControl x=new PagedResultsResponseControl("",false,b);
			assertNotNull(x.getCookie());
		} catch (IOException e) {
			fail("Failed with:"+e);
		}
	}

    public void testSerializationCompatibility() throws Exception{
        byte[] b={48,5,2,1,0,4,0};
        PagedResultsResponseControl object=new PagedResultsResponseControl("test", true, b);
        SerializationTest.verifyGolden(this, object, PAGEDRESULTSRESPONSECONTROL_COMPARATOR);
    }
    
    // comparator for PagedResultsResponseControl
    private static final SerializableAssert PAGEDRESULTSRESPONSECONTROL_COMPARATOR = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            PagedResultsResponseControl initThr = (PagedResultsResponseControl) initial;
            PagedResultsResponseControl dserThr = (PagedResultsResponseControl) deserialized;

            // verify ResultSize
            int initResultSize = initThr.getResultSize();
            int dserResultSize = dserThr.getResultSize();
            assertTrue(initResultSize == dserResultSize);
            
            // verify Cookie
            byte[] initCookie = initThr.getCookie();
            byte[] dserCookie = dserThr.getCookie();
            assertTrue(Arrays.equals(initCookie, dserCookie));
        }
    };
}
