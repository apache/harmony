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

import java.util.Arrays;

import javax.naming.InvalidNameException;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.Rdn;

import junit.framework.TestCase;

/**
 * <p>
 * This class test is made to test all cases of package where the coverage was
 * not 100%.
 * </p>
 * <p>
 * We are going to find here a lot cases from different classes, notice here that
 * the conventional structure followed in the rest of the project is applied
 * here.
 * </p>
 * 
 */
public class TestRdnWhiteBoxDevelopment extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestRdnWhiteBoxDevelopment.class);
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.Rdn(String)'
     * </p>
     * <p>
     * Here we are testing if the constructor can receive several multivalued
     * types.
     * </p>
     * <p>
     * The expected result is a not null instance.
     * </p>
     */
    public void testRdnString() throws Exception {
        new Rdn("test=now+cn=mio+cn=mio2+ou=please+cn=mio3+ou=please2");
        new Rdn("cn=mio+ou=please+cn=mio2+cn=mio3");
        new Rdn("ou=please+test=now+cn=mio+cn=mio2+ou=please+cn=mio3+ou=please2+nueva=prueba");
        new Rdn("au=please+d=d+b=now+cn=mio+cn=mio2+ou=please+cn=mio3+b=please2+na=prueba");
        new Rdn("au=#00420ced");
        new Rdn("au=\\#00420ced");
        new Rdn("t=\\#0FA3TA");
        new Rdn("t=\\4CM\\4E+u=Minombre\\40+a=\\4C\\0d");
        new Rdn("v=a=a");
        new Rdn("v=a=+a=+v=#0D8F");
        new Rdn("v=======");
        new Rdn("v=<");
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.Rdn(String)'
     * </p>
     * <p>
     * Here we are testing if the constructor can receive the special character
     * "\".
     * </p>
     * <p>
     * The expected result is a not null instance.
     * </p>
     */
    public void testQuotedTypeCHECK() {
        try {
            // +v=#0D8F";
            new Rdn("v=+a=+\\\\a=");
            fail("Type should not contains quoted chars");
        } catch (InvalidNameException e) {}
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.Rdn(String)'
     * </p>
     * <p>
     * Here we are testing if the constructor can receive the special character
     * "+".
     * </p>
     * <p>
     * The expected result is a not null instance.
     * </p>
     */
    public void testIgnoreEndingPlusChar() throws Exception {
        Rdn rdn = new Rdn("a=k+");
        Rdn rdn2 = new Rdn("a=k");
        assertTrue(rdn.equals(rdn2));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.Rdn(String)'
     * </p>
     * <p>
     * Here we are testing if the constructor can receive the special character
     * "#".
     * </p>
     * <p>
     * The expected result is that two rdns must not be the same.
     * </p>
     */
    public void testEquals() throws Exception {
        assertNotSame(new Rdn("au=#00420ced"), new Rdn("au=\\#00420ced"));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.equals(Rdn)'
     * </p>
     * <p>
     * Here we are testing if the method can receive a Rdn.
     * </p>
     * <p>
     * The expected result is that two rdns must be the same.
     * </p>
     */
    public void testRDNEqualsCompare() throws Exception {
        Rdn rdn = new Rdn("t=test+v=\\4C\\4C+t=test+a=a=a+v=fsdasd+a=<a");
        Rdn rdn1 = new Rdn(rdn.toString());
        assertTrue(rdn.equals(rdn1));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.equals(Rdn)'
     * </p>
     * <p>
     * Here we are testing if the method can receive a Rdn.
     * </p>
     * <p>
     * The expected result is that two rdns must be the same.
     * </p>
     */
    public void testRDNequals() throws Exception {
        Rdn x1 = new Rdn("t=test");
        Rdn x2 = new Rdn(x1);
        assertEquals(x1, x2);
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.equals(Rdn)'
     * </p>
     * <p>
     * Here we are testing if the method can receive a Rdn.
     * </p>
     * <p>
     * The expected result is that two rdns must be the same.
     * </p>
     */
    public void testRDNequals2() throws Exception {
        Rdn x2 = new Rdn("cn", "#0001");
        Rdn x3 = new Rdn("cn", new byte[] { 0, 1 });

        assertEquals(x2, x3);
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.equals(Rdn)'
     * </p>
     * <p>
     * Here we are testing if the method can receive a Rdn.
     * </p>
     * <p>
     * The expected result is that two rdns must be the same.
     * </p>
     */
    public void testRDNequals3() throws Exception {
        Rdn x1 = null;
        Rdn x2 = null;
        Rdn x3 = null;
        Rdn x4 = null;
        Rdn x5 = null;
        MyBasicAttributes set = new MyBasicAttributes("cn", new byte[] { 116,
                116, 116 });

        x2 = new Rdn("cn=ttt");
        x3 = new Rdn("cn", new byte[] { 116, 116, 116 });
        x1 = new Rdn(x2);
        x4 = new Rdn(set);

        assertFalse("Should not be equals.", x3.equals(x2));
        assertFalse("Should not be equals.", x1.equals(x3));
        assertTrue("Should be equals.", x3.equals(x3));
        assertTrue("Should be equals.", x3.equals(x4));

        x1 = new Rdn("cn", new byte[] { 11, 116, 11 });
        x2 = new Rdn("cn", new byte[] { 116, 116, 116 });
        x3 = new Rdn("cn", new byte[] { 116, 116, 116 });
        x4 = new Rdn("cn=#30");
        x5 = new Rdn("cn", new byte[] { 0 });

        assertFalse(x1.equals(x3));
        assertTrue(x2.equals(x3));
        assertFalse(x5.equals(x4));

    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.equals(Rdn)'
     * </p>
     * <p>
     * Here we are testing if the method can receive a Rdn with a different
     * array.
     * </p>
     * <p>
     * The expected result is that two rdns must not be the same.
     * </p>
     */
    public final void testEqualsArrays() throws Exception {
        Rdn x = new Rdn("t", new byte[] { 01, 00, 02 });
        Rdn y = new Rdn("t", new byte[] { 00, 03, 01 });
        assertFalse(x.equals(y));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.escapedValue(Object)'
     * </p>
     * <p>
     * Here we are testing if this method can escape some values.
     * </p>
     * <p>
     * The expected result is that the method return the string escaped.
     * </p>
     */
    public final void testEscapedValue001() {
        assertEquals("t\u00ef\u00bf\u00bda\\, mar\u00ef\u00bf\u00bda", Rdn.escapeValue("t\u00ef\u00bf\u00bda, mar\u00ef\u00bf\u00bda"));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.escapedValue(Object)'
     * </p>
     * <p>
     * Here we are testing if this method can escape some values.
     * </p>
     * <p>
     * The expected result is that the method return the string escaped.
     * </p>
     */
    public final void testEscapedValue002() {
        assertEquals("#00420ced", Rdn
                .escapeValue(new byte[] { 0, 66, 12, -19 }));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.escapedValue(Object)'
     * </p>
     * <p>
     * Here we are testing if this method can escape some values.
     * </p>
     * <p>
     * The expected result is that the method return the string escaped.
     * </p>
     */
    public final void testescapedValue003() {
        assertEquals("#fa08", Rdn.escapeValue(new byte[] { -6, 8 }));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.escapedValue(Object)'
     * </p>
     * <p>
     * Here we are testing if this method can escape some values.
     * </p>
     * <p>
     * The expected result is that the method return the string escaped.
     * </p>
     */
    public final void testEscapedValue004() {
        assertEquals("t\u00ef\u00bf\u00bda\\, mar\u00ef\u00bf\u00bda \\#\\,sobrante\\>\\<", Rdn
                .escapeValue("t\u00ef\u00bf\u00bda, mar\u00ef\u00bf\u00bda #,sobrante><"));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.escapedValue(Object)'
     * </p>
     * <p>
     * Here we are testing if this method can escape some values.
     * </p>
     * <p>
     * The expected result is that the method return the string escaped.
     * </p>
     */
    public final void testEscapedValue005() {
        assertEquals("t\u00ef\u00bf\u00bda\\, mar\u00ef\u00bf\u00bda \\#\\,sobrante\\>\\<", Rdn
                .escapeValue("t\u00ef\u00bf\u00bda, mar\u00ef\u00bf\u00bda #,sobrante><"));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.toString()'
     * </p>
     * <p>
     * Here we are testing if this method returns the correct string.
     * </p>
     */
    public void testToString001() throws Exception {
        String t = "t=test+v=value+t=test2+v=value2";
        Rdn rdn = new Rdn(t);
        assertEquals("t=test+t=test2+v=value+v=value2", rdn.toString());
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.toString()'
     * </p>
     * <p>
     * Here we are testing if this method returns the correct string.
     * </p>
     */
    public void testToString002() throws Exception {
        String t = "SN=Lu\\C4\\8Di\\C4\\87";
        Rdn rdn = new Rdn(t);
        assertEquals(8, rdn.toString().length());
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.toString()'
     * </p>
     * <p>
     * Here we are testing if this method returns the correct string.
     * </p>
     */
    public void testToString003() throws Exception {
        String t = "v=#080100";
        Rdn rdn = new Rdn("v", new byte[] { 8, 01, 0 });
        assertEquals(t, rdn.toString());
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.toString()'
     * </p>
     * <p>
     * Here we are testing if this method returns the correct string.
     * </p>
     */
    public void testToString004() throws Exception {
        Rdn rdn = new Rdn("v", " ");
        assertFalse("v=\\ " == rdn.toString());
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.escapedValue(Object)'
     * </p>
     * <p>
     * Here we are testing if this method can escape some values.
     * </p>
     * <p>
     * The expected result is that the method return the string escaped.
     * </p>
     */
    public void testUnescapedValue001() {
        assertEquals(Arrays.toString(new byte[] { -6, 8 }), Arrays
                .toString((byte[]) Rdn.unescapeValue("#fa08")));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.escapedValue(Object)'
     * </p>
     * <p>
     * Here we are testing if this method can escape some values.
     * </p>
     * <p>
     * The expected result is that the method return the string escaped.
     * </p>
     */
    public void testUnescapedValue002() {
        try {
            Rdn.unescapeValue("##fa08");
            fail("An exception must be thrown.");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.escapedValue(Object)'
     * </p>
     * <p>
     * Here we are testing if this method can escape some values.
     * </p>
     * <p>
     * The expected result is that the method return the string escaped.
     * </p>
     */
    public void testUnescapedValue003() {
        assertEquals("t=LMN+u=Minombre@+a=L", Rdn
                .unescapeValue("t=\\4CM\\4E+u=Minombre\\40+a=\\4C"));
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.hashCode()'
     * </p>
     * <p>
     * Here we are testing if this method return the hash of an object distinct
     * in the value of a string.
     * </p>
     */
    public void testHashCode() throws Exception {
        Rdn x = new Rdn("t", new byte[] { 01, 00, 02 });
        assertNotNull(x);
        assertNotSame(0, x.hashCode());
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.Rdn.getValue()'
     * </p>
     * <p>
     * Here we are testing if this method retrieves one of this Rdn's value. In
     * this case should raise an exception because the rdn is empty.
     * </p>
     * <p>
     * The expected result is an exception.
     * </p>
     */
    public void testGetValue001() throws Exception {
        new Rdn("t", " ").getValue();
    }

    /**
     * <p>
     * Test method to test if the constructor take literally what is between
     * '"'.
     * </p>
     * 
     */
    public void testRDNComillas() throws Exception {
        Rdn x2 = new Rdn("cn", "\"mio\"");
        Rdn x3 = new Rdn("cn", "\"mio\"");
        assertTrue("Should be equals.", x2.equals(x3));
    }

    /**
     * <p>
     * Test method to test if the constructor take literally what is between
     * '"'.
     * </p>
     * 
     */
    public void testRDNComillas2() throws Exception {
        Rdn x2 = null;
        String temp = "cn=mio\\,zapato\\,veloz\\;\\\" \\#";
        x2 = new Rdn("cn=\"mio,zapato,veloz;\\\" \\#\"");
        assertEquals(0, temp.compareToIgnoreCase(x2.toString()));
    }

    /**
     * <p>
     * Test method to test if the constructor can receive the correct length to a
     * byte array.
     * </p>
     * 
     */
    public void testRDNWrongHexValue() {
        try {
            new Rdn("cn=#A");
            fail();
        } catch (InvalidNameException e) {} catch (IllegalArgumentException e) {}
    }

    /**
     * Here we are testing to send the escaped special characters and utf8.
     * 
     */
    public void testRdnEscapeValue() throws Exception {
        new Rdn("cn=\\;\\\"");
        new Rdn("cn=\\<\\>");
        new Rdn("cn=\\<\\>\\\\");
        new Rdn("cn=\\<\\>\\=");
        new Rdn("cn=\\<\\>\"\\+\"");
        new Rdn("cn=\\<\\>\\4C");
        new Rdn("cn=\\<\\>\\ ");
    }

    /*
     * Class to help us to do the white box.
     */
    private class MyBasicAttributes extends BasicAttributes {

        private static final long serialVersionUID = 1L;

        private Object myo;

        MyBasicAttributes(String x, Object o) {
            super(x, o);
            this.myo = o;
        }

        public Object getValue() {
            return myo;
        }
    }

}
