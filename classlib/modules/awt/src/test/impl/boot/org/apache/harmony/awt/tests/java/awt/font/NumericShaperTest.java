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

package org.apache.harmony.awt.tests.java.awt.font;

import java.awt.font.NumericShaper;

import junit.framework.TestCase;

public class NumericShaperTest extends TestCase {

    private final String testStringNoContext1 = "123456789ab0123456789cde";
    private final String goldenBengaliStringNoContext1 = "\u09E7\u09E8\u09E9\u09EA\u09EB\u09EC\u09ED\u09EE\u09EFab\u09E6\u09E7\u09E8\u09E9\u09EA\u09EB\u09EC\u09ED\u09EE\u09EFcde";

    // text starts with the nonDigit char and ends with digit char
    private final String testStringNoContext2 = "abc0123456789de0123456789";
    private final String goldenBengaliStringNoContext2 = "abc\u09E6\u09E7\u09E8\u09E9\u09EA\u09EB\u09EC\u09ED\u09EE\u09EFde\u09E6\u09E7\u09E8\u09E9\u09EA\u09EB\u09EC\u09ED\u09EE\u09EF";

    // text starts with the BENGALI context digit char and ends with nonDigit char
    private final String testStringContext1 = "\u0983123456789ab0123456789cde";
    private final String goldenBengaliDevanagariStringContext1 = "\u0983\u09E7\u09E8\u09E9\u09EA\u09EB\u09EC\u09ED\u09EE\u09EFab0123456789cde";

    // text starts with the nonDigit char and ends with digit char
    // the first set of digits in text folows by "\u0909" char from the DEVANAGARI script
    // the second set of digits in text folows by "\u0983" char from the BENGALI script
    private final String testStringContext2 = "abc\u09090123456789de\u09830123456789";
    private final String goldenBengaliDevanagariStringContext2 = "abc\u0909\u0966\u0967\u0968\u0969\u096A\u096B\u096C\u096D\u096E\u096Fde\u0983\u09E6\u09E7\u09E8\u09E9\u09EA\u09EB\u09EC\u09ED\u09EE\u09EF";

    private final String testStringContext3 = "0123456789de\u12010123456789";
    private final String goldenTamilDevanagariStringContext3 = "\u0966\u0967\u0968\u0969\u096A\u096B\u096C\u096D\u096E\u096Fde\u12010123456789";

    public NumericShaperTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'java.awt.font.NumericShaper.equals(Object)'
     */
    public final void testEqualsObject() {
        NumericShaper ns;
        NumericShaper ns1;

        // Simple shapers are equal
        ns = NumericShaper.getShaper(NumericShaper.BENGALI);
        assertTrue("NonContextual object isn't equal to itself", ns.equals(ns));

        ns1 = NumericShaper.getShaper(NumericShaper.BENGALI);
        assertTrue("NonContextual object isn't equal to the equal object", ns.equals(ns1));
        
        ns1 = NumericShaper.getShaper(NumericShaper.ARABIC);
        assertFalse("Object equals to the different NumericShaper nonContextual object", ns.equals(ns1));

        // Context shapers with default context are equal
        ns = NumericShaper.getContextualShaper(NumericShaper.BENGALI);
        assertTrue("Contextual(default) object isn't equal to itself", ns.equals(ns));

        ns1 = NumericShaper.getContextualShaper(NumericShaper.BENGALI);
        assertTrue("Contextual(default) object isn't equal to the equal object", ns.equals(ns1));
        
        ns1 = NumericShaper.getContextualShaper(NumericShaper.ARABIC);
        assertFalse("Object equals to the different NumericShaper contextual(default) object", ns.equals(ns1));

        // Context shapers with context are equal
        ns = NumericShaper.getContextualShaper(NumericShaper.ARABIC | NumericShaper.ETHIOPIC, NumericShaper.BENGALI);
        assertTrue("Contextual object isn't equal to itself", ns.equals(ns));

        ns1 = NumericShaper.getContextualShaper(NumericShaper.ARABIC | NumericShaper.ETHIOPIC, NumericShaper.BENGALI);
        assertTrue("Contextual object isn't equal to the equal object", ns.equals(ns1));
        
        ns1 = NumericShaper.getContextualShaper(NumericShaper.ARABIC | NumericShaper.ETHIOPIC, NumericShaper.EUROPEAN);
        assertFalse("Object equals to the different NumericShaper contextual object", ns.equals(ns1));
    }

    /*
     * Test method for 'java.awt.font.NumericShaper.getContextualShaper(int, int)'
     */
    public final void testGetContextualShaperIntInt() {
        NumericShaper ns;
        ns = NumericShaper.getContextualShaper(NumericShaper.ARABIC | NumericShaper.TAMIL, NumericShaper.EASTERN_ARABIC);
        assertNotNull(ns);
        
        assertEquals("Ranges are different", NumericShaper.ARABIC | NumericShaper.TAMIL, ns.getRanges());
        assertTrue("Contextual shaper isContextual() method must return true value", ns.isContextual());

        try{
            // wrong ranges value
            ns = NumericShaper.getShaper(13);
            fail("IlligalArgumentException wasn't thrown with invalid shaper value");
        } catch (IllegalArgumentException e){
            
        }
    }

    /*
     * Test method for 'java.awt.font.NumericShaper.getContextualShaper(int)'
     */
    public final void testGetContextualShaperInt() {
        NumericShaper ns;
        ns = NumericShaper.getContextualShaper(NumericShaper.ARABIC | NumericShaper.TAMIL);
        assertNotNull(ns);
        
        assertEquals("Ranges are not the same", NumericShaper.ARABIC | NumericShaper.TAMIL, ns.getRanges());
        assertTrue("Default contextual shaper isContextual() method must return true value", ns.isContextual());
        
        try{
            // wrong ranges value
            ns = NumericShaper.getShaper(13);
            fail("IlligalArgumentException wasn't thrown with invalid shaper value");
        } catch (IllegalArgumentException e){
            
        }

    }

    /*
     * Test method for 'java.awt.font.NumericShaper.getRanges()'
     */
    public final void testGetRanges() {
        NumericShaper ns;
        int ranges;
        ns = NumericShaper.getShaper(NumericShaper.ARABIC);
        ranges = ns.getRanges();
        assertEquals("Simple shaper ranges have differences", NumericShaper.ARABIC, ranges);
        
        ns = NumericShaper.getContextualShaper(NumericShaper.ARABIC | NumericShaper.TAMIL, NumericShaper.EASTERN_ARABIC);

        ranges = ns.getRanges();
        assertEquals("Contextual shaper ranges have differences", NumericShaper.ARABIC | NumericShaper.TAMIL, ranges);
    }

    /*
     * Test method for 'java.awt.font.NumericShaper.getShaper(int)'
     */
    public final void testGetShaper() {
        NumericShaper ns;
        ns = NumericShaper.getShaper(NumericShaper.EASTERN_ARABIC);
        assertNotNull(ns);
        
        assertEquals("Ranges are different", NumericShaper.EASTERN_ARABIC, ns.getRanges());
        assertFalse("Simple shaper can not be contextual", ns.isContextual());
        
        try{
            ns = NumericShaper.getShaper(NumericShaper.EASTERN_ARABIC | NumericShaper.BENGALI);
            fail("IlligalArgumentException wasn't thrown with invalid shaper value");
        } catch (IllegalArgumentException e){
            
        }
    }

    /*
     * Test method for 'java.awt.font.NumericShaper.isContextual()'
     */
    public final void testIsContextual() {
        NumericShaper ns;

        // Simple shapers 
        ns = NumericShaper.getShaper(NumericShaper.EASTERN_ARABIC); 
        assertFalse("Simple shapers may not be contextual", ns.isContextual());
        
        // Context shapers with default context
        ns = NumericShaper.getContextualShaper(NumericShaper.ARABIC | NumericShaper.TAMIL);
        assertTrue("Default context shapers must be contextual", ns.isContextual());
            
        // Context shapers with context 
        ns = NumericShaper.getContextualShaper(NumericShaper.ARABIC | NumericShaper.TAMIL, NumericShaper.EASTERN_ARABIC);
        assertTrue("Context shapers must be contextual", ns.isContextual());
    }

    /*
     * Test method for 'java.awt.font.NumericShaper.shape(char[], int, int, int)'
     */
    public final void testShapeCharArrayIntIntInt() {
        NumericShaper ns;
        
        int context = NumericShaper.DEVANAGARI;
        
        // non-contextual shaper - context ignored
        ns = NumericShaper.getShaper(NumericShaper.BENGALI);
        
        // test text that starts with the digit char and ends with nonDigit char 
        char[] chars = testStringNoContext1.toCharArray(); 

        ns.shape(chars, 0, chars.length, context);
        
        for (int i=0; i < chars.length; i++){
            assertEquals("shaped char at pos[" + i + "] not equals to the golden one", (int)goldenBengaliStringNoContext1.charAt(i), chars[i]);
        }

        // shaper without default context
        ns = NumericShaper.getContextualShaper(NumericShaper.TAMIL | NumericShaper.DEVANAGARI);
        
        // test text starts with the TAMIL context digit char and ends with nonDigit char
        chars = testStringContext3.toCharArray(); 

        ns.shape(chars, 0, chars.length, context);
        
        for (int i=0; i < chars.length; i++){
            assertEquals("shaped char at pos[" + i + "] not equals to the golden one", (int)goldenTamilDevanagariStringContext3.charAt(i), chars[i]);
        }


        // offset >= length
        try{
            ns.shape(chars, chars.length + 1, 1, context);
            fail("IndexOutOfBoundsException expected but wasn't thrown!");
        } catch (IndexOutOfBoundsException e){
            // expected
        }
        
        // offset < 0
        try{
            ns.shape(chars, -1, 1, context);
            fail("IndexOutOfBoundsException expected but wasn't thrown!");
        } catch (IndexOutOfBoundsException e){
            // expected
        }

        // offset+length out of range
        try{
            ns.shape(chars, chars.length -1, 2);
            fail("IndexOutOfBoundsException expected but wasn't thrown!");
        } catch (IndexOutOfBoundsException e){
            // expected
        }

    }

    /*
     * Test method for 'java.awt.font.NumericShaper.shape(char[], int, int)'
     */
    public final void testShapeCharArrayIntInt() {
        NumericShaper ns;
        
        /*************************/
        /* Non-Contextual shaper */  
        /*************************/

        ns = NumericShaper.getShaper(NumericShaper.BENGALI);
        
        // test text that starts with the digit char and ends with nonDigit char 
        char[] chars = testStringNoContext1.toCharArray(); 

        ns.shape(chars, 0, chars.length);
        
        for (int i=0; i < chars.length; i++){
            assertEquals("shaped char at pos[" + i + "] not equals to the golden one", (int)goldenBengaliStringNoContext1.charAt(i), chars[i]);
        }

        // text starts with the nonDigit char and ends with digit char
        chars = testStringNoContext2.toCharArray();
        ns.shape(chars, 0, chars.length);
        
        for (int i=0; i < chars.length; i++){
            assertEquals("shaped char at pos[" + i + "] not equals to the golden one", (int)goldenBengaliStringNoContext2.charAt(i), chars[i]);
        }

        /*********************/
        /* Contextual shaper */  
        /*********************/
        
        // shaper without default context
        ns = NumericShaper.getContextualShaper(NumericShaper.BENGALI | NumericShaper.DEVANAGARI);
        
        // test text starts with the BENGALI context digit char and ends with nonDigit char
        chars = testStringContext1.toCharArray(); 

        ns.shape(chars, 0, chars.length);
        
        for (int i=0; i < chars.length; i++){
            assertEquals("shaped char at pos[" + i + "] not equals to the golden one", (int)goldenBengaliDevanagariStringContext1.charAt(i), chars[i]);
        }

        // text starts with the nonDigit char and ends with digit char
        // the first set of digits in text folows by "\u0909" char from the DEVANGARI script
        // the second set of digits in text folows by "\u0983" char from the BENGALI script
        chars = testStringContext2.toCharArray(); 

        ns.shape(chars, 0, chars.length);
        
        for (int i=0; i < chars.length; i++){
            assertEquals("shaped char at pos[" + i + "] not equals to the golden one", (int)goldenBengaliDevanagariStringContext2.charAt(i), chars[i]);
        }

        // shaper with acceptable default context
        ns = NumericShaper.getContextualShaper(NumericShaper.TAMIL | NumericShaper.DEVANAGARI, NumericShaper.DEVANAGARI);
        
        // text starts with Digit char and ends with digit char
        // These digits must be replaced with default context digits
        chars = testStringContext3.toCharArray(); 
        
        ns.shape(chars, 0, chars.length);
        
        for (int i=0; i < chars.length; i++){
            assertEquals("shaped char at pos[" + i + "] not equals to the golden one", (int)goldenTamilDevanagariStringContext3.charAt(i), chars[i]);
        }
        
        // shaper with nonacceptable default context
        ns = NumericShaper.getContextualShaper(NumericShaper.TAMIL | NumericShaper.DEVANAGARI, NumericShaper.KANNADA);
        
        // text starts with Digit char and ends with digit char
        // All digits must stay without changes
        chars = testStringContext3.toCharArray(); 
        
        ns.shape(chars, 0, chars.length);
        
        for (int i=0; i < chars.length; i++){
            assertEquals("shaped char at pos[" + i + "] not equals to the golden one", (int)testStringContext3.charAt(i), chars[i]);
        }
        
        // offset >= length
        try{
            ns.shape(chars, chars.length + 1, 1);
            fail("IndexOutOfBoundsException expected but wasn't thrown!");
        } catch (IndexOutOfBoundsException e){
            // expected
        }
        
        // offset < 0
        try{
            ns.shape(chars, -1, 1);
            fail("IndexOutOfBoundsException expected but wasn't thrown!");
        } catch (IndexOutOfBoundsException e){
            // expected
        }

        // offset+length out of range
        try{
            ns.shape(chars, chars.length -1, 2);
            fail("IndexOutOfBoundsException expected but wasn't thrown!");
        } catch (IndexOutOfBoundsException e){
            // expected
        }
        
    }

    /*
     * Test method for 'java.awt.font.NumericShaper.shape(char[], int, int)' with 
     * illegal parameters.
     */
    public final void testShapeCharArrayIntInt_IllegalArguments() {
        // regression test for Harmony-1584
        int ranges = NumericShaper.ARABIC;
        NumericShaper localNumericShaper = NumericShaper
                .getContextualShaper(ranges);
        char[] chars = new char[] {};
        int start = 0;
        int count = 1;
        try {
            localNumericShaper.shape(chars, start, count);
            fail("len = 0: ArrayIndexOutOfBoundsException expected!");
        } catch (ArrayIndexOutOfBoundsException expectedException) {
            // expected
        }

        chars = new char[] {'a', 'b', 'c'};
        start = -1;
        count = 1;
        try {
            localNumericShaper.shape(chars, start, count);
            fail("start < 0: ArrayIndexOutOfBoundsException expected!");
        } catch (ArrayIndexOutOfBoundsException expectedException) {
            // expected
        }

        // count < 0: silent run expected
        start = 1;
        count = -1;
        localNumericShaper.shape(chars, start, count);

        start = 3;
        count = 5;
        try {
            localNumericShaper.shape(chars, start, count);
            fail("start + count > len: ArrayIndexOutOfBoundsException expected!");
        } catch (ArrayIndexOutOfBoundsException expectedException) {
            // expected
        }

    }

    /*
     * Test method for 'java.awt.font.NumericShaper.shape(char[], int, int, int)' with 
     * illegal parameters. 
     */
    public final void testShapeCharArrayIntIntInt_IllegalArguments() {
        // regression test for Harmony-1584
        int ranges = NumericShaper.ARABIC;
        NumericShaper localNumericShaper = NumericShaper
                .getContextualShaper(ranges);
        char[] chars = new char[] {};
        int start = 0;
        int count = 1;
        int index = NumericShaper.ARABIC;
        try {
            localNumericShaper.shape(chars, start, count, index);
            fail("len = 0: ArrayIndexOutOfBoundsException expected!");
        } catch (ArrayIndexOutOfBoundsException expectedException) {
            //expected
        }

        chars = new char[] {'a', 'b', 'c'};
        start = -1;
        count = 1;
        try {
            localNumericShaper.shape(chars, start, count, index);
            fail("start < 0: ArrayIndexOutOfBoundsException expected!");
        } catch (ArrayIndexOutOfBoundsException expectedException) {
            // expected
        }

        // count < 0: silent run expected
        start = 1;
        count = -1;
        localNumericShaper.shape(chars, start, count, index);

        start = 3;
        count = 5;
        try {
            localNumericShaper.shape(chars, start, count, index);
            fail("start + count > len: ArrayIndexOutOfBoundsException expected!");
        } catch (ArrayIndexOutOfBoundsException expectedException) {
            // expected
        }

    }

}
