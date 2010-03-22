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

package org.apache.harmony.jndi.provider.ldap.asn1;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.harmony.security.asn1.ASN1Boolean;
import org.apache.harmony.security.asn1.ASN1Enumerated;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;

public class UtilsTest extends TestCase {

	private final String testStr = "test123";

	private final char[] testCharArray = testStr.toCharArray();

	private byte[] testByteArray;

	public UtilsTest() {
		super();

		try {
			testByteArray = testStr.getBytes(Utils.CODING_CHARSET);
		} catch (UnsupportedEncodingException e) {
			// never reached, because UTF-8 is supported by all java
			// platform
		}
	}
	
    public void test_conjoinSequence() {
        ASN1Sequence first = new ASN1Sequence(new ASN1Type[] {
                ASN1OctetString.getInstance(), ASN1Integer.getInstance(),
                ASN1Boolean.getInstance() }) {
            {
                setDefault(Boolean.FALSE, 2);
                setOptional(1);
            }
        };
        ASN1Sequence second = new ASN1Sequence(new ASN1Type[] {
                ASN1Integer.getInstance(), ASN1Enumerated.getInstance(),
                ASN1OctetString.getInstance() }) {
            {
                setOptional(2);
            }

        };
        ASN1Sequence result = Utils.conjoinSequence(first, second);

        assertEquals(first.type.length + second.type.length, result.type.length);

        for (int i = 0; i < first.type.length; i++) {
            assertEquals(first.type[i], result.type[i]);
            assertEquals(first.OPTIONAL[i], result.OPTIONAL[i]);
            assertEquals(first.DEFAULT[i], result.DEFAULT[i]);
        }

        int index = first.type.length;
        for (int i = 0; i < second.type.length; i++) {
            assertEquals(second.type[i], result.type[i + index]);
            assertEquals(second.OPTIONAL[i], result.OPTIONAL[i + index]);
            assertEquals(second.DEFAULT[i], result.DEFAULT[i + index]);
        }

        result = Utils.conjoinSequence(first, null);

        assertEquals(first.type.length, result.type.length);
        for (int i = 0; i < first.type.length; i++) {
            assertEquals(first.type[i], result.type[i]);
            assertEquals(first.OPTIONAL[i], result.OPTIONAL[i]);
            assertEquals(first.DEFAULT[i], result.DEFAULT[i]);
        }

        result = Utils.conjoinSequence(null, second);
        assertEquals(second.type.length, result.type.length);
        for (int i = 0; i < second.type.length; i++) {
            assertEquals(second.type[i], result.type[i]);
            assertEquals(second.OPTIONAL[i], result.OPTIONAL[i]);
            assertEquals(second.DEFAULT[i], result.DEFAULT[i]);
        }
    }
    
    public void test_getString(){
    	assertEquals("",Utils.getString(null));
    	
    	assertEquals(testStr,Utils.getString(testStr));
    	assertEquals(testStr,Utils.getString(testByteArray));
    	assertEquals(testStr,Utils.getString(testCharArray));
    	
    	try{
    		Utils.getString(new Object());
    		fail("ClassCastException expected here.");
    	}catch (ClassCastException e){
    		//expected
    	}
    }
    
    public void test_getBytes(){
    	byte[] bytes = Utils.getBytes(null);
    	assertTrue((bytes instanceof byte[]) && (bytes.length == 0));

    	assertTrue(Arrays.equals(testByteArray,Utils.getBytes(testStr)));
    	assertEquals(testByteArray,Utils.getBytes(testByteArray));
    	assertTrue(Arrays.equals(testByteArray,Utils.getBytes(testCharArray)));
    	
    	try{
    		Utils.getBytes(new Object());
    		fail("ClassCastException expected here.");
    	}catch (ClassCastException e){
    		//expected
    	}
    }
    
    public void test_getCharArray(){
    	char[] chars = Utils.getCharArray(null);
    	assertTrue((chars instanceof char[]) && (chars.length == 0));

    	assertTrue(Arrays.equals(testCharArray,Utils.getCharArray(testStr)));
    	assertTrue(Arrays.equals(testCharArray,Utils.getCharArray(testByteArray)));
    	assertEquals(testCharArray,Utils.getCharArray(testCharArray));
    	
    	try{
    		Utils.getCharArray(new Object());
    		fail("ClassCastException expected here.");
    	}catch (ClassCastException e){
    		//expected
    	}
    }
    
}
