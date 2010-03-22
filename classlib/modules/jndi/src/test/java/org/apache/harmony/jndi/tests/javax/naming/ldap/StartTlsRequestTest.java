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

import javax.naming.NamingException;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap.MockStartTlsResponse;
/**
 * <p>This Test class is testing the class StartTlsRequest in the javax.naming.ldap package.</p>
 * <p>Here in the next tables we are going to find all methods to be tested:</p>
 *  <table class="t" cellspacing="0">
	<tbody><th>Constructors:</th>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="65" name="sas9nt21" readonly="readonly" value="StartTlsRequest()" id="f10"></td>
			
		</tr>
				
	</tbody>
	<table>
	<tbody><th>Method Summary:</th>
		<tr><TD>Return</TD><TD>Method</TD></tr>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="ExtendedResponse" id="f00"></TD>
			<td class="c0" id="c10"><input class="a0" size="65" name="sas9nt21" readonly="readonly" value="createExtendedResponse(String id, byte[] berValue, int offset, int length)" id="f10"></td>
			
		</tr>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="byte[]" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="65" name="sas9nt21" readonly="readonly" value="getEncodedValue()" id="f10"></td>
			
		</tr>
		<tr>
			<td class="c0" id="c00"><input class="a0" size="30" name="sas9nt11" readonly="readonly" value="String" id="f00"></td>
			<td class="c0" id="c10"><input class="a0" size="65" name="sas9nt21" readonly="readonly" value="getID()" id="f10"></td>
			
		</tr>
		
			
	</tbody>
	</table>
 * <hr>
 * 
 */
public class StartTlsRequestTest extends TestCase {

	/**
	 * <p>Test method for 'javax.naming.ldap.StartTlsRequest.getID()'</p>
	 * <p>Here we are testing if this method retrieves the StartTLS request's object identifier string.</p>
	 * <p>The expected result is a string : "1.3.6.1.4.1.1466.20037".</p>
	 */
	public void testGetID() {
        assertEquals("1.3.6.1.4.1.1466.20037", StartTlsRequest.OID);
        assertSame(StartTlsRequest.OID, new StartTlsRequest().getID());
    }

	/**
	 * <p>Test method for 'javax.naming.ldap.StartTlsRequest.getEncodedValue()'</p>
	 * <p>Here we are testing if this method retrieves the StartTLS request's ASN.1 BER encoded value.</p>
	 * <p>The expected result is a null value.</p>
	 */
	public void testGetEncodedValue() {
        assertNull(new StartTlsRequest().getEncodedValue());
    }

	/**
	 * <p>Test method for 'javax.naming.ldap.StartTlsRequest.createExtendedResponse(String, byte[], int, int)'</p>
	 * <p>Here we are testing if this method creates an extended response object that corresponds to the LDAP StartTLS extended request.
	 * In this case we are testing the extended response with the argument ID=""</p>
	 * <p>The expected result is an exception.</p>
	 */
	public void testCreateExtendedResponse005() {
        StartTlsRequest str = new StartTlsRequest();
        try {
            str.createExtendedResponse("", null, 1, 2);
            fail("NamingException expected");
        } catch (NamingException e) {}
    }

    /**
     * <p>Test method for 'javax.naming.ldap.StartTlsRequest.createExtendedResponse(String, byte[], int, int)'</p>
     * <p>Here we are testing if this method creates an extended response object that corresponds to the LDAP StartTLS extended request.
     * In this case we are testing the extended response with the argument ID="1.3.6.1.4.1.1466.20037" and the others arguments should be ignored.</p>
     * <p>Notice here that this package does not have a provider so an implementation does not exist, so this test must not fail with a provider 
     * and fail with no provider.</p>
     * <p>The expected result is a Tls response.</p>
     */
    public void testCreateExtendedResponse004() throws Exception {
        StartTlsRequest str = new StartTlsRequest();
        String ID = "1.3.6.1.4.1.1466.20037";
        int t1 = 210, t2 = 650;
        byte[] t0 = ID.getBytes();

        StartTlsResponse x = (StartTlsResponse) str.createExtendedResponse(ID,
                t0, t1, t2);
        assertEquals(MockStartTlsResponse.class, x.getClass());
    }

    public static class MockStartTlsResponse1 extends MockStartTlsResponse {

        public MockStartTlsResponse1() throws Exception {
            throw new Exception();
        }
    }

    public static class MockStartTlsResponse2 extends MockStartTlsResponse {

        protected MockStartTlsResponse2() {}
    }

    static class MockStartTlsResponse3 extends MockStartTlsResponse {}
}
