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

import java.math.BigInteger;

import javax.naming.ldap.SortControl;
import javax.naming.ldap.SortKey;

import junit.framework.TestCase;

/**
 * This Test class is testing the SortControl class
 */
public class SortControlTest extends TestCase {

    /**
     * tests how constructors with arrayas process null
     */
    public void testSortControl() throws Exception {
        try {
            new SortControl((String[]) null, false);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        try {
            new SortControl((SortKey[]) null, false);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    /**
     * @tests isCritical()
     */
    public void testSortControlStringArrayBoolean002() throws Exception {
        assertTrue(new SortControl("", true).isCritical());
        assertFalse(new SortControl("", false).isCritical());

        assertTrue(new SortControl(new String[0], true).isCritical());
        assertFalse(new SortControl(new String[0], false).isCritical());

        assertTrue(new SortControl(new SortKey[0], true).isCritical());
        assertFalse(new SortControl(new SortKey[0], false).isCritical());

    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.SortControl.getEncodedValue()'
     * </p>
     * <p>
     * Here we are testing if this method retrieves the control's ASN.1 BER
     * encoded value. In this case we create a sort control using an array of
     * sortkey.
     * </p>
     * <p>
     * The expecting result is the control's ASN.1 BER encoded value.
     * </p>
     */
    public void testEncodedValueOfSortControl001() throws Exception {
        SortKey[] sk = { new SortKey("pepe", false, "leo") };

        SortControl sc = new SortControl(sk, true);
        assertEquals("30 10 30 0e 04 04 70 65 70 65 80 03 6c 65 6f 81 01 ff",
                toHexString(sc.getEncodedValue()));
    }
    
    public void testEncodedValueOfSortControlNull() throws Exception{
        String[] sk = {"pepe", null, "", "haha" };

        SortControl sc = new SortControl(sk, true);
        assertEquals("30 18 30 06 04 04 70 65 70 65 30 02 04 00 30 02 04 00 30 06 04 04 68 61 68 61",
                toHexString(sc.getEncodedValue()));
        
        String[] sk2 = {"pepe", "", "haha" };
        sc = new SortControl(sk2, true);
        assertEquals("30 14 30 06 04 04 70 65 70 65 30 02 04 00 30 06 04 04 68 61 68 61",
                toHexString(sc.getEncodedValue()));
        
        SortKey[] sk3 = {new SortKey("pepe", false, "haha"), null, new SortKey("", true, "haha2"), new SortKey("haah", true, "pepe")};
        try{
            sc = new SortControl(sk3, true);
            fail("should throw NPE");
        }catch(NullPointerException e){
        }
        
        SortKey[] sk4 = {new SortKey("pepe", false, "haha"), new SortKey("", true, "haha2"), new SortKey("haah", true, "pepe")};
        sc = new SortControl(sk4, true);
        assertEquals("30 2a 30 0f 04 04 70 65 70 65 80 04 68 61 68 61 81 01 ff 30 09 04 00 80 05 68 61 68 61 32 30 0c 04 04 68 61 61 68 80 04 70 65 70 65",
                toHexString(sc.getEncodedValue()));

    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.SortControl.getEncodedValue()'
     * </p>
     * <p>
     * Here we are testing if this method retrieves the control's ASN.1 BER
     * encoded value. In this case we create a sort control using an array of
     * sortkey.
     * </p>
     * <p>
     * The expecting result is the control's ASN.1 BER encoded value.
     * </p>
     */
    public void testEncodedValueOfSortControl003() throws Exception {
        SortKey[] sk = { new SortKey("", true, "") };

        SortControl sc = new SortControl(sk, true);
        assertEquals("30 06 30 04 04 00 80 00", toHexString(sc
                .getEncodedValue()));

        SortKey[] sk1 = { new SortKey("pepe", true, "laura") };

        sc = new SortControl(sk1, true);
        assertEquals("30 0f 30 0d 04 04 70 65 70 65 80 05 6c 61 75 72 61",
                toHexString(sc.getEncodedValue()));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.SortControl.getEncodedValue()'
     * </p>
     * <p>
     * Here we are testing if this method retrieves the control's ASN.1 BER
     * encoded value. In this case we create a sort control using an array of
     * strings.
     * </p>
     * <p>
     * The expecting result is the control's ASN.1 BER encoded value.
     * </p>
     */
    public void testEncodedValueOfSortControl006() throws Exception {
        String[] sk = { "" };
        SortControl sc = new SortControl(sk, true);
        assertEquals("30 04 30 02 04 00", toHexString(sc.getEncodedValue()));

        String[] sk1 = { "pepe", "", "toto" };
        sc = new SortControl(sk1, true);
        assertEquals(
                "30 14 30 06 04 04 70 65 70 65 30 02 04 00 30 06 04 04 74 6f 74 6f",
                toHexString(sc.getEncodedValue()));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.SortControl.getEncodedValue()'
     * </p>
     * <p>
     * Here we are testing if this method retrieves the control's ASN.1 BER
     * encoded value. In this case we create a sort control using a string.
     * </p>
     * <p>
     * The expecting result is the control's ASN.1 BER encoded value.
     * </p>
     */
    public void testEncodedValueOfSortControl014() throws Exception {
        SortControl sc;

        sc = new SortControl((String) null, false);
        assertEquals("30 04 30 02 04 00", toHexString(sc.getEncodedValue()));

        sc = new SortControl("", true);
        assertEquals("30 04 30 02 04 00", toHexString(sc.getEncodedValue()));

        sc = new SortControl("pepe", false);
        assertEquals("30 08 30 06 04 04 70 65 70 65", toHexString(sc
                .getEncodedValue()));
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
