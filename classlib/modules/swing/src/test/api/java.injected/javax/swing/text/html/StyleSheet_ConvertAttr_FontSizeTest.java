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
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS.Attribute;

public class StyleSheet_ConvertAttr_FontSizeTest extends BasicSwingTestCase {
    private static final String[] fontSizeTable = {
        "xx-small", "x-small", "small",
        "medium",
        "large", "x-large", "xx-large"
    };

    private StyleSheet ss;
    private AttributeSet empty;
    private AttributeSet attr;
    private Object cssValue;
    private Object scValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        empty = ss.getEmptySet();
    }

    public void testFontSize() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, new Integer(10));

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.FONT_SIZE, name);
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertNotSame(Integer.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals(tableSizeToCSSKeyword(2), cssValue.toString());
        assertEquals("10", scValue.toString());
        assertEquals(10, ((Integer)scValue).intValue());
    }

    public void testFontSizeXXSmall() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "xx-small");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertNotSame(Integer.class, cssValue.getClass());
//        assertNotSame(String.class, cssValue.getClass());
        assertEquals("xx-small", cssValue.toString());
        assertEquals(8, ((Integer)scValue).intValue());
    }

    public void testFontSizeXSmall() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "x-small");
        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("x-small", cssValue.toString());
        assertEquals(10, ((Integer)scValue).intValue());
    }

    public void testFontSizeSmall() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "small");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("small", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizeMedium() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "medium");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("medium", cssValue.toString());
        assertEquals(14, ((Integer)scValue).intValue());
    }

    public void testFontSizeLarge() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "large");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("large", cssValue.toString());
        assertEquals(18, ((Integer)scValue).intValue());
    }

    public void testFontSizeXLarge() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "x-large");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("x-large", cssValue.toString());
        assertEquals(24, ((Integer)scValue).intValue());
    }

    public void testFontSizeXXLarge() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "xx-large");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("xx-large", cssValue.toString());
        assertEquals(36, ((Integer)scValue).intValue());
    }

    public void testFontSizeSmaller() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "smaller");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("smaller", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizeSmallerParented() {
        AttributeSet parent = ss.addAttribute(empty, StyleConstants.FontSize,
                                              "x-large");
        assertEquals(24, ((Integer)parent.getAttribute(StyleConstants.FontSize))
                         .intValue());
        attr = ss.addAttribute(empty, StyleConstants.ResolveAttribute, parent);
        attr = ss.addAttribute(attr, StyleConstants.FontSize, "smaller");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("smaller", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizeLarger() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "larger");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("larger", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizeLargerParented() {
        AttributeSet parent = ss.addAttribute(empty, StyleConstants.FontSize,
                                              "x-small");
        assertEquals(10, ((Integer)parent.getAttribute(StyleConstants.FontSize))
                         .intValue());
        attr = ss.addAttribute(empty, StyleConstants.ResolveAttribute, parent);
        attr = ss.addAttribute(attr, StyleConstants.FontSize, "larger");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("larger", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizeTable() {
        int[][] size = {
            // {scSizeSet, cssSize, scSizeRead}
             {6,  1,  8},        {7,  1,  8},
             {8,  1,  8},        {9,  2, 10},
            {10,  2, 10},       {11,  3, 12},
            {12,  3, 12},       {13,  4, 14},
            {14,  4, 14},       {15,  5, 18},
            {16,  5, 18},       {17,  5, 18},
            {18,  5, 18},       {19,  6, 24},
            {20,  6, 24},       {21,  6, 24},
            {22,  6, 24},       {23,  6, 24},
            {24,  6, 24},       {25,  7, 36},
            {26,  7, 36},       {27,  7, 36},
            {28,  7, 36},       {29,  7, 36},
            {30,  7, 36},       {31,  7, 36},
            {32,  7, 36},       {33,  7, 36},
            {34,  7, 36},       {35,  7, 36},
            {36,  7, 36},       {37,  7, 36},
            {38,  7, 36},       {39,  7, 36},
            {40,  7, 36}
        };
        for (int i = 0; i < size.length; i++) {
            attr = ss.addAttribute(empty, StyleConstants.FontSize,
                                   new Integer(size[i][0]));

            cssValue = attr.getAttribute(Attribute.FONT_SIZE);
            scValue = attr.getAttribute(StyleConstants.FontSize);
            assertSame("@ " + i, Integer.class, scValue.getClass());
            assertEquals("@ " + i, tableSizeToCSSKeyword(size[i][1]),
                         cssValue.toString());
            assertEquals("@ " + i, size[i][2], ((Integer)scValue).intValue());
        }
    }

    public void testFontSizePercentage() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "150%");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertNotSame(Integer.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("150%", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizePercentageParented() {
        AttributeSet parent = ss.addAttribute(empty, StyleConstants.FontSize,
                                              "large");
        attr = ss.addAttribute(empty, StyleConstants.ResolveAttribute, parent);
        attr = ss.addAttribute(attr, StyleConstants.FontSize, "80%");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertNotSame(Integer.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("80%", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    /**
     * Points, 1pt = 1/72in
     */
    public void testFontSizeAbsolutePt() {
        if (!isHarmony()) {
            return;
        }
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "20pt");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("20pt", cssValue.toString());
        assertEquals(20, ((Integer)scValue).intValue());
    }

    /**
     * Pixels
     */
    public void testFontSizeAbsolutePx() {
        if (!isHarmony()) {
            return;
        }
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "23px");
        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals(29, ((Integer)scValue).intValue());
        assertEquals("23px", cssValue.toString());

        attr = ss.addAttribute(empty, StyleConstants.FontSize, "24px");
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertEquals(31, ((Integer)scValue).intValue());


        attr = ss.addAttribute(empty, StyleConstants.FontSize, "100px");
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertEquals(130, ((Integer)scValue).intValue());
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "200px");
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertEquals(260, ((Integer)scValue).intValue());
    }

    /**
     * Picas, 1pc = 12pt
     */
    public void testFontSizeAbsolutePc() {
        if (!isHarmony()) {
            return;
        }
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "4pc");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("4pc", cssValue.toString());
        assertEquals(48, ((Integer)scValue).intValue());
    }

    public void testFontSizeAbsoluteMm() {
        if (!isHarmony()) {
            return;
        }
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "20mm");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("20mm", cssValue.toString());
        assertEquals(56, ((Integer)scValue).intValue());
    }

    public void testFontSizeAbsoluteCm() {
        if (!isHarmony()) {
            return;
        }
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "2cm");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("2cm", cssValue.toString());
        assertEquals(56, ((Integer)scValue).intValue());
    }

    public void testFontSizeRelativeEm() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "2em");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("2em", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizeRelativeEmParented() {
        AttributeSet parent = ss.addAttribute(empty, StyleConstants.FontSize,
                                              "x-large");
        attr = ss.addAttribute(empty, StyleConstants.ResolveAttribute, parent);
        attr = ss.addAttribute(attr, StyleConstants.FontSize, "2em");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("2em", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizeRelativeEx() {
        AttributeSet parent = ss.addAttribute(empty, StyleConstants.FontSize,
                                              "x-large");
        attr = ss.addAttribute(empty, StyleConstants.ResolveAttribute, parent);
        attr = ss.addAttribute(attr, StyleConstants.FontSize, "3ex");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("3ex", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizeRelativeExParented() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "3ex");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("3ex", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizeAbsoluteIn() {
        if (!isHarmony()) {
            return;
        }

        attr = ss.addAttribute(empty, StyleConstants.FontSize, "2in");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertEquals("2in", cssValue.toString());
        assertEquals(144, ((Integer)scValue).intValue());
    }

    public void testFontSizeAbsoluteParented() {
        AttributeSet parent = ss.addAttribute(empty, StyleConstants.FontSize,
                                              "large");
        attr = ss.addAttribute(empty, StyleConstants.ResolveAttribute, parent);
        attr = ss.addAttribute(attr, StyleConstants.FontSize, "80%");

        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, scValue.getClass());
        assertNotSame(Integer.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("80%", cssValue.toString());
        assertEquals(12, ((Integer)scValue).intValue());
    }

    public void testFontSizeNonStyleConstants() {
        attr = ss.addAttribute(empty, Attribute.FONT_SIZE, "2in");
        cssValue = attr.getAttribute(Attribute.FONT_SIZE);
        scValue = attr.getAttribute(StyleConstants.FontSize);

        if (isHarmony()) {
            assertSame(Integer.class, scValue.getClass());
            assertNotSame(Integer.class, cssValue.getClass());
            assertNotSame(String.class, cssValue.getClass());
            assertEquals(144, ((Integer)scValue).intValue());
        } else {
            assertSame(String.class, cssValue.getClass());
            assertNull(scValue);
        }
        assertEquals("2in", cssValue.toString());
    }

    private String tableSizeToCSSKeyword(final int size) {
        if (isHarmony()) {
            return fontSizeTable[size - 1];
        } else {
            return String.valueOf(size);
        }
    }
}
