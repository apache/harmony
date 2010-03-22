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
 * @author Alexey A. Ivanov
 */
package javax.swing.text.html;

import java.util.Enumeration;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS.Attribute;

public class StyleSheet_ConvertAttr_BoldTest extends BasicSwingTestCase {
    private StyleSheet ss;
    private AttributeSet empty;
    private AttributeSet attr;
    private MutableAttributeSet simple;
    private Object cssValue;
    private Object scValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        empty = ss.getEmptySet();
        simple = new SimpleAttributeSet();
    }

    public void testBold() {
        attr = ss.addAttribute(empty, StyleConstants.Bold, Boolean.TRUE);

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.FONT_WEIGHT, name);
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.FONT_WEIGHT);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertSame(Boolean.class, scValue.getClass());
        assertNotSame(Boolean.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("bold", cssValue.toString());
        assertEquals("true", scValue.toString());
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testBoldFalse() {
        attr = ss.addAttribute(empty, StyleConstants.Bold, Boolean.FALSE);

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.FONT_WEIGHT, name);
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.FONT_WEIGHT);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertSame(Boolean.class, scValue.getClass());
        assertNotSame(Boolean.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("normal", cssValue.toString());
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testBoldBold() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "bold");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("bold", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testBoldNormal() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "normal");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("normal", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testBoldBolder() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "bolder");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        if (isHarmony()) {
            assertEquals("bolder", cssValue.toString());

            attr = ss.createSmallAttributeSet(simple);
            scValue = attr.getAttribute(StyleConstants.Bold);
            assertNotNull(scValue);
            assertTrue(((Boolean)scValue).booleanValue());
        } else {
            assertEquals("normal", cssValue.toString()); // default value
        }
    }

    public void testBoldLighter() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "lighter");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        if (isHarmony()) {
            assertEquals("lighter", cssValue.toString());

            attr = ss.createSmallAttributeSet(simple);
            scValue = attr.getAttribute(StyleConstants.Bold);
            assertNotNull(scValue);
            assertFalse(((Boolean)scValue).booleanValue());
        } else {
            assertEquals("normal", cssValue.toString()); // default value
        }
    }

    public void testBold100() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "100");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("100", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testBold200() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "200");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("200", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testBold300() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "300");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("300", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testBold400() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "400");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("400", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testBold500() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "500");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("500", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testBold600() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "600");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("600", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testBold700() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "700");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("700", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testBold800() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "800");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("800", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testBold900() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "900");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_WEIGHT);
        assertEquals("900", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Bold);
        assertNotNull(scValue);
        assertTrue(((Boolean)scValue).booleanValue());
    }
}
