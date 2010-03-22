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

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS.Attribute;

public class StyleSheet_ConvertAttr_FontFamilyTest extends BasicSwingTestCase {
    private StyleSheet ss;
    private AttributeSet empty;
    private AttributeSet attr;
    private Object cssValue;
    private Object scValue;
    private static final String[] fonts;
    private static final List fontList;

    static {
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        fonts = ge.getAvailableFontFamilyNames();
        fontList = Arrays.asList(fonts);
    }

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        empty = ss.getEmptySet();
    }

    /**
     * Contains a known font on Windows.
     */
    public void testFontFamilyW() {
        attr = ss.addAttribute(empty, StyleConstants.FontFamily, "Arial");
        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.class, name.getClass());
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(name);
        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        if (fontList.contains("Arial")) {
            assertEquals("Arial", scValue.toString());
        } else {
            assertEquals("SansSerif", scValue.toString());
        }
    }

    /**
     * Contains a known font on Linux.
     */
    public void testFontFamilyL() {
        attr = ss.addAttribute(empty, StyleConstants.FontFamily, "Lucida Sans");
        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.class, name.getClass());
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        if (fontList.contains("Lucida Sans")) {
            assertEquals("Lucida Sans", scValue.toString());
        } else {
            assertEquals("SansSerif", scValue.toString());
        }
    }

    /**
     * The value refers to the unknown font.
     */
    public void testFontFamilyNotExists() {
        attr = ss.addAttribute(empty, StyleConstants.FontFamily, "Helvet");

        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("Helvet", cssValue.toString());
        assertEquals("SansSerif", scValue.toString());
    }

    /**
     * Multiple fonts are listed.
     * The name of the first font found on the system is returned as result
     * of the convertion.
     */
    public void testFontFamilyMultiple() {
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        List fontList = Arrays.asList(ge.getAvailableFontFamilyNames());
        boolean hasGaramond = fontList.contains("Garamond");
        boolean hasTimes = fontList.contains("Times New Roman");

        attr = ss.addAttribute(empty, StyleConstants.FontFamily,
                               "Garamond, Times New Roman, serif");

        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("Garamond, Times New Roman, serif", cssValue.toString());
        if (hasGaramond) {
            assertEquals("Garamond", scValue.toString());
        } else if (hasTimes) {
            assertEquals("Times New Roman", scValue.toString());
        } else {
            assertEquals(isHarmony() ? "Serif" : "SansSerif", scValue.toString());
        }
    }

    /**
     * Multiple fonts are listed. The first item refers to non-existent font,
     * the second to the known font on Windows systems.
     */
    public void testFontFamilyMultipleNonExistentW() {
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        List fontList = Arrays.asList(ge.getAvailableFontFamilyNames());
        boolean hasTimes = fontList.contains("Times New Roman");

        attr = ss.addAttribute(empty, StyleConstants.FontFamily,
                               "NonExistentFont, Times New Roman, serif");

        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("NonExistentFont, Times New Roman, serif", cssValue.toString());
        if (hasTimes) {
            // The test is expected to be here when run on Windows
            assertEquals(isHarmony() ? "Times New Roman" : "SansSerif",
                         scValue.toString());
        } else {
            assertEquals(isHarmony() ? "Serif" : "SansSerif",
                         scValue.toString());
        }
    }

    /**
     * Multiple fonts are listed. The first item refers to non-existent font,
     * the second to the known font on Linux systems.
     */
    public void testFontFamilyMultipleNonExistentL() {
        boolean hasLucida = fontList.contains("Lucida Sans");

        attr = ss.addAttribute(empty, StyleConstants.FontFamily,
                               "NonExistentFont, Lucida Sans, serif");

        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("NonExistentFont, Lucida Sans, serif",
                     cssValue.toString());
        if (hasLucida) {
            // The test is expected to be here when run on Linux
            assertEquals(isHarmony() ? "Lucida Sans" : "SansSerif",
                         scValue.toString());
        } else {
            assertEquals(isHarmony() ? "Serif" : "SansSerif",
                         scValue.toString());
        }
    }

    /**
     * Multiple fonts are listed. The two first items refer to non-existent
     * fonts, the last item is generic family. It is the generic family
     * that expected to be the result of convertion.
     */
    public void testFontFamilyMultipleNonExistentG() {
        attr = ss.addAttribute(empty, StyleConstants.FontFamily,
                               "NonExistentFont, UnknownFont, serif");

        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("NonExistentFont, UnknownFont, serif",
                     cssValue.toString());
        assertEquals(isHarmony() ? "Serif" : "SansSerif", scValue.toString());
    }

    /**
     * Attribute set was added <code>CSS.Attribute.FONT_FAMILY</code>
     * property. It is not expected to convert values to and from.
     * <code>StyleConstants</code>.
     */
    public void testFontFamilyNonStyleConstants() {
        attr = ss.addAttribute(empty, Attribute.FONT_FAMILY, "serif");
        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertEquals("serif", cssValue.toString());

        if (isHarmony()) {
            assertSame(String.class, scValue.getClass());
            assertNotSame(String.class, cssValue.getClass());
            assertEquals("Serif", scValue.toString());
        } else {
            assertSame(String.class, cssValue.getClass());
            assertNull(scValue);
        }
    }

    /**
     * Generic family: serif.
     */
    public void testFontFamilySerif() {
        attr = ss.addAttribute(empty, StyleConstants.FontFamily, "serif");
        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("serif", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertEquals("Serif", scValue);
    }

    /**
     * Generic family: sans-serif.
     */
    public void testFontFamilySansSerif() {
        attr = ss.addAttribute(empty, StyleConstants.FontFamily, "sans-serif");
        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("sans-serif", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertEquals("SansSerif", scValue);
    }

    /**
     * Generic family: cursive.
     * <p>Falls back to default sans-serif.
     */
    public void testFontFamilyCursive() {
        attr = ss.addAttribute(empty, StyleConstants.FontFamily, "cursive");
        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("cursive", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertEquals("SansSerif", scValue);
    }

    /**
     * Generic family: fantasy.
     * <p>Falls back to default sans-serif.
     */
    public void testFontFamilyFantasy() {
        attr = ss.addAttribute(empty, StyleConstants.FontFamily, "fantasy");
        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("fantasy", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertEquals("SansSerif", scValue);
    }

    /**
     * Generic family: monospace.
     */
    public void testFontFamilyMonospace() {
        attr = ss.addAttribute(empty, StyleConstants.FontFamily, "monospace");
        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("monospace", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertSame(String.class, scValue.getClass());
        assertEquals("Monospaced", scValue);
    }

    /**
     * Multiple fonts are listed.
     * The first item is generic font family.
     */
    public void testFontFamilyGenericPlusFont() {
        attr = ss.addAttribute(empty, StyleConstants.FontFamily,
                               "monospace, Lucida Sans");

        cssValue = attr.getAttribute(Attribute.FONT_FAMILY);
        scValue = attr.getAttribute(StyleConstants.FontFamily);
        assertEquals("monospace, Lucida Sans", cssValue.toString());
        assertEquals("Monospaced", scValue.toString());
    }
}
