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

public class StyleSheet_ConvertAttr_BorderTest extends BasicSwingTestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;
    private Object cssValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testBorderThin() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "thin");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("thin", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(4, simple.getAttributeCount());

        assertEquals("thin", simple.getAttribute(Attribute.BORDER_TOP_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_RIGHT_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_BOTTOM_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_LEFT_WIDTH)
                             .toString());
    }

    public void testBorderGreen() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "green");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("green", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(1, simple.getAttributeCount());

        assertEquals("green", simple.getAttribute(Attribute.BORDER_COLOR)
                              .toString());
    }

    public void testBorderSolid() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "solid");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("solid", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(1, simple.getAttributeCount());

        assertEquals("solid", simple.getAttribute(Attribute.BORDER_STYLE)
                              .toString());
    }


    public void testBorderThinYellow() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "thin yellow");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("thin yellow", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(5, simple.getAttributeCount());

        assertEquals("yellow", simple.getAttribute(Attribute.BORDER_COLOR)
                               .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_TOP_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_RIGHT_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_BOTTOM_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_LEFT_WIDTH)
                             .toString());
    }

    public void testBorderThinDotted() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "thin dotted");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("thin dotted", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(5, simple.getAttributeCount());

        assertEquals("dotted", simple.getAttribute(Attribute.BORDER_STYLE)
                               .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_TOP_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_RIGHT_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_BOTTOM_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_LEFT_WIDTH)
                             .toString());
    }

    public void testBorderGreenMedium() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "green medium");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("green medium", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(5, simple.getAttributeCount());

        assertEquals("green", simple.getAttribute(Attribute.BORDER_COLOR)
                              .toString());
        assertEquals("medium", simple.getAttribute(Attribute.BORDER_TOP_WIDTH)
                               .toString());
        assertEquals("medium", simple.getAttribute(Attribute.BORDER_RIGHT_WIDTH)
                               .toString());
        assertEquals("medium", simple.getAttribute(Attribute.BORDER_BOTTOM_WIDTH)
                               .toString());
        assertEquals("medium", simple.getAttribute(Attribute.BORDER_LEFT_WIDTH)
                               .toString());
    }

    public void testBorderGreenDashed() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "green dashed");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("green dashed", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(2, simple.getAttributeCount());

        assertEquals("dashed", simple.getAttribute(Attribute.BORDER_STYLE)
                               .toString());
        assertEquals("green", simple.getAttribute(Attribute.BORDER_COLOR)
                              .toString());
    }

    public void testBorderSolidRed() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "solid red");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("solid red", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(2, simple.getAttributeCount());

        assertEquals("red", simple.getAttribute(Attribute.BORDER_COLOR)
                            .toString());
        assertEquals("solid", simple.getAttribute(Attribute.BORDER_STYLE)
                              .toString());
    }

    public void testBorderSolidThin() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "solid thin");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("solid thin", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(5, simple.getAttributeCount());

        assertEquals("solid", simple.getAttribute(Attribute.BORDER_STYLE)
                              .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_TOP_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_RIGHT_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_BOTTOM_WIDTH)
                             .toString());
        assertEquals("thin", simple.getAttribute(Attribute.BORDER_LEFT_WIDTH)
                             .toString());
    }


    public void testBorderInsetThickAqua() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "inset thick aqua");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("inset thick aqua", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(6, simple.getAttributeCount());

        assertEquals("inset", simple.getAttribute(Attribute.BORDER_STYLE)
                              .toString());
        assertEquals("aqua", simple.getAttribute(Attribute.BORDER_COLOR)
                             .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_TOP_WIDTH)
                              .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_RIGHT_WIDTH)
                              .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_BOTTOM_WIDTH)
                              .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_LEFT_WIDTH)
                              .toString());
    }

    public void testBorderThickAquaInset() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "thick aqua inset");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("thick aqua inset", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(6, simple.getAttributeCount());

        assertEquals("inset", simple.getAttribute(Attribute.BORDER_STYLE)
                              .toString());
        assertEquals("aqua", simple.getAttribute(Attribute.BORDER_COLOR)
                             .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_TOP_WIDTH)
                              .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_RIGHT_WIDTH)
                              .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_BOTTOM_WIDTH)
                              .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_LEFT_WIDTH)
                              .toString());
    }

    public void testBorderAquaInsetThick() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BORDER, "aqua inset thick");
        cssValue = simple.getAttribute(Attribute.BORDER);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("aqua inset thick", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(6, simple.getAttributeCount());

        assertEquals("inset", simple.getAttribute(Attribute.BORDER_STYLE)
                              .toString());
        assertEquals("aqua", simple.getAttribute(Attribute.BORDER_COLOR)
                             .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_TOP_WIDTH)
                              .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_RIGHT_WIDTH)
                              .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_BOTTOM_WIDTH)
                              .toString());
        assertEquals("thick", simple.getAttribute(Attribute.BORDER_LEFT_WIDTH)
                              .toString());
    }
}
