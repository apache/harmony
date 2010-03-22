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
 * @author Elena V. Sayapina 
 */ 

package javax.print.attribute;

import java.util.Locale;

import junit.framework.TestCase;


public class TextSyntaxTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TextSyntaxTest.class);
    }

    static {
        System.out.println("TextSyntax testing...");
    }

    TextSyntax ts1, ts2;

    /*
     * TextSyntax() constructor testing. 
     */
    public final void testTextSyntax() {
        try {
            String str = null;
            ts1 = new textSyntax(str, Locale.UK);
            fail("NullPointerException wasn't trown when expected");
        } catch (NullPointerException e) {
        }

        ts1 = new textSyntax("text", null);
        assertEquals(Locale.getDefault(), ts1.getLocale());
        assertEquals("text", ts1.getValue());
    }

    /*
     * hashCode() method testing. 
     */
    public final void testHashCode() {

        ts1 = new textSyntax("hello", Locale.UK);
        ts2 = new textSyntax("hello", Locale.UK);
        assertEquals(ts1, ts2);

        ts1 = new textSyntax("", Locale.UK);
        ts2 = new textSyntax("", Locale.CANADA);
        assertFalse(ts1 == ts2);
    }

    /*
     * equals(Object object) method testing. 
     */
    public final void testEqualsObject() {

        ts1 = new textSyntax("text", Locale.UK);
        ts2 = new textSyntax("text", Locale.UK);
        assertTrue(ts1.equals(ts2));


        ts1 = new textSyntax(" a", Locale.UK);
        ts2 = new textSyntax("a", Locale.UK);
        assertFalse(ts1.equals(ts2));
    }

    /*
     * getLocale() method testing. 
     */
    public final void testGetLocale() {
        Locale locale = Locale.ITALY;
        ts1 = new textSyntax("text", locale);
        assertEquals(locale, ts1.getLocale());

        ts1 = new textSyntax("text", null);
        assertNotNull(ts1.getLocale());
    }

    /*
     * getValue() method testing. 
     */
    public final void testGetValue() {
        ts1 = new textSyntax("Hello world!", null);
        assertEquals("Hello world!", ts1.getValue());
    }

    /*
     * toString() method testing. 
     */
    public final void testToString() {
        Locale locale = Locale.ITALY;
        ts1 = new textSyntax(" text ", locale);
        assertEquals(" text ", ts1.toString());
    }


    /*
     * Auxiliary class
     */
    public class textSyntax extends TextSyntax {

        public textSyntax(String value, Locale locale) {
            super(value, locale);
        }
    }


}
