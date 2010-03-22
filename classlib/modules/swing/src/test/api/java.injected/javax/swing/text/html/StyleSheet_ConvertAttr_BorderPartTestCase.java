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

public abstract class StyleSheet_ConvertAttr_BorderPartTestCase extends BasicSwingTestCase {
    protected StyleSheet ss;
    protected MutableAttributeSet simple;
    protected Attribute cssKey;
    protected Object cssValue;
    protected Attribute cssWidthKey;
    protected int sideIndex;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
        sideIndex = -1;
    }

    public void testBlackSolidThin() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "black solid thin");
        cssValue = simple.getAttribute(cssKey);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("black solid thin", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(3, simple.getAttributeCount());

        assertEquals(getColorDeclaration("black"),
                     simple.getAttribute(Attribute.BORDER_COLOR).toString());
        assertEquals(getStyleDeclaration("solid"),
                     simple.getAttribute(Attribute.BORDER_STYLE).toString());
        assertEquals("thin",
                     simple.getAttribute(cssWidthKey).toString());
    }

    public void testSolidThin() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "solid thin");
        cssValue = simple.getAttribute(cssKey);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("solid thin", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(2, simple.getAttributeCount());

        assertEquals(getStyleDeclaration("solid"),
                     simple.getAttribute(Attribute.BORDER_STYLE).toString());
        assertEquals("thin",
                     simple.getAttribute(cssWidthKey).toString());
    }

    public void testBlackThin() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "black thin");
        cssValue = simple.getAttribute(cssKey);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("black thin", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(2, simple.getAttributeCount());

        assertEquals(getColorDeclaration("black"),
                     simple.getAttribute(Attribute.BORDER_COLOR).toString());
        assertEquals("thin",
                     simple.getAttribute(cssWidthKey).toString());
    }

    public void testBlackSolid() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "black solid");
        cssValue = simple.getAttribute(cssKey);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("black solid", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(2, simple.getAttributeCount());

        assertEquals(getColorDeclaration("black"),
                     simple.getAttribute(Attribute.BORDER_COLOR).toString());
        assertEquals(getStyleDeclaration("solid"),
                     simple.getAttribute(Attribute.BORDER_STYLE).toString());
    }

    public void testSolidBlackThin() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "solid black thin");
        cssValue = simple.getAttribute(cssKey);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("solid black thin", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(3, simple.getAttributeCount());

        assertEquals(getColorDeclaration("black"),
                     simple.getAttribute(Attribute.BORDER_COLOR).toString());
        assertEquals(getStyleDeclaration("solid"),
                     simple.getAttribute(Attribute.BORDER_STYLE).toString());
        assertEquals("thin",
                     simple.getAttribute(cssWidthKey).toString());
    }

    public void testSolidFFEEDDThin() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "solid #FFEEDD thin");
        cssValue = simple.getAttribute(cssKey);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("solid #FFEEDD thin", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(3, simple.getAttributeCount());

        assertEquals(getColorDeclaration("#FFEEDD"),
                     simple.getAttribute(Attribute.BORDER_COLOR).toString());
        assertEquals(getStyleDeclaration("solid"),
                     simple.getAttribute(Attribute.BORDER_STYLE).toString());
        assertEquals("thin",
                     simple.getAttribute(cssWidthKey).toString());
    }

    public void testSolidRGBMedium() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "solid rgb(127, 255, 75) medium");
        cssValue = simple.getAttribute(cssKey);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("solid rgb(127, 255, 75) medium", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(3, simple.getAttributeCount());

        assertEquals(getColorDeclaration("rgb(127, 255, 75)"),
                     simple.getAttribute(Attribute.BORDER_COLOR).toString());
        assertEquals(getStyleDeclaration("solid"),
                     simple.getAttribute(Attribute.BORDER_STYLE).toString());
        assertEquals("medium",
                     simple.getAttribute(cssWidthKey).toString());
    }

    public void testInsetRGBLength() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "inset rgb(50%, 100%, 30%) 1px");
        cssValue = simple.getAttribute(cssKey);
        if (!isHarmony()) {
            assertSame(String.class, cssValue.getClass());
            assertEquals("inset rgb(50%, 100%, 30%) 1px", cssValue.toString());
            return;
        }

        assertNull(cssValue);
        assertEquals(3, simple.getAttributeCount());

        assertEquals(getColorDeclaration("rgb(50%, 100%, 30%)"),
                     simple.getAttribute(Attribute.BORDER_COLOR).toString());
        assertEquals(getStyleDeclaration("inset"),
                     simple.getAttribute(Attribute.BORDER_STYLE).toString());
        assertEquals("1px",
                     simple.getAttribute(cssWidthKey).toString());
    }

    protected final String getStyleDeclaration(final String style) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(i == sideIndex ? style : "none");
        }
        return result.toString();
    }

    protected final String getColorDeclaration(final String color) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(i == sideIndex ? color : "white");
        }
        return result.toString();
    }
}
