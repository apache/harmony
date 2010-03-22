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

import junit.framework.TestCase;

public class StyleSheet_ConvertAttr_ItalicTest extends TestCase {
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

    public void testItalic() {
        attr = ss.addAttribute(empty, StyleConstants.Italic, Boolean.TRUE);

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.FONT_STYLE, name);
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.FONT_STYLE);
        scValue = attr.getAttribute(StyleConstants.Italic);
        assertSame(Boolean.class, scValue.getClass());
        assertNotSame(Boolean.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("italic", cssValue.toString());
        assertEquals("true", scValue.toString());
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testItalicFalse() {
        attr = ss.addAttribute(empty, StyleConstants.Italic, Boolean.FALSE);

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.FONT_STYLE, name);
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.FONT_STYLE);
        scValue = attr.getAttribute(StyleConstants.Italic);
        assertSame(Boolean.class, scValue.getClass());
        assertNotSame(Boolean.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals(BasicSwingTestCase.isHarmony() ? "normal" : "",
                     cssValue.toString());
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testItalicItalic() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_STYLE, "italic");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_STYLE);
        assertEquals("italic", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Italic);
        assertNotNull(scValue);
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testItalicNormal() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_STYLE, "normal");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_STYLE);
        assertEquals("normal", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Italic);
        assertNotNull(scValue);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testItalicOblique() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_STYLE, "oblique");
        assertEquals(1, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.FONT_STYLE);
        assertEquals("oblique", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Italic);
        assertNotNull(scValue);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testItalicInvalidValue() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_STYLE, "invalid");
        cssValue = simple.getAttribute(Attribute.FONT_STYLE);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
            return;
        }

        assertEquals(1, simple.getAttributeCount());
        assertEquals("invalid", cssValue.toString());

        attr = ss.createSmallAttributeSet(simple);
        scValue = attr.getAttribute(StyleConstants.Italic);
        assertNotNull(scValue);
        assertFalse(((Boolean)scValue).booleanValue());
    }
}
