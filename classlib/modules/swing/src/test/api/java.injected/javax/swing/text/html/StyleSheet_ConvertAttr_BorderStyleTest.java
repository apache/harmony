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

import javax.swing.BasicSwingTestCase;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.CSS.Attribute;

import junit.framework.TestCase;

public class StyleSheet_ConvertAttr_BorderStyleTest extends TestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;
    private Object cssValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testBorderStyleNone() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "none");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals("none", cssValue.toString());
        } else {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        }
    }

    public void testBorderStyleDotted() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "dotted");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        assertEquals("dotted", cssValue.toString());
    }

    public void testBorderStyleDashed() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "dashed");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        assertEquals("dashed", cssValue.toString());
    }

    public void testBorderStyleSolid() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "solid");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        assertEquals("solid", cssValue.toString());
    }

    public void testBorderStyleDouble() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "double");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        assertEquals("double", cssValue.toString());
    }

    public void testBorderStyleGroove() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "groove");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        assertEquals("groove", cssValue.toString());
    }

    public void testBorderStyleRidge() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "ridge");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        assertEquals("ridge", cssValue.toString());
    }

    public void testBorderStyleInset() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "inset");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        assertEquals("inset", cssValue.toString());
    }

    public void testBorderStyleOutset() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "outset");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        assertEquals("outset", cssValue.toString());
    }

    public void testBorderStyleSunken() throws Exception {
        // Invalid value
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "sunken");
        assertEquals(0, simple.getAttributeCount());
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        assertNull(cssValue);
    }


    public void testBorderStyleInsetOutset() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "inset outset");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("inset outset", cssValue.toString());
        } else {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        }
    }

    public void testBorderStyleOutsetInsetSolid() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE, "outset inset solid");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("outset inset solid", cssValue.toString());
        } else {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        }
    }

    public void testBorderStyleInsetRidgeOutsetGroove() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE,
                           "inset ridge outset groove");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("inset ridge outset groove", cssValue.toString());
        } else {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        }
    }


    public void testBorderStyleInsetRidgeOutsetEtched() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER_STYLE,
                           "inset ridge outset etched");
        cssValue = simple.getAttribute(Attribute.BORDER_STYLE);
        assertEquals(0, simple.getAttributeCount());
        assertNull(cssValue);
    }
}
