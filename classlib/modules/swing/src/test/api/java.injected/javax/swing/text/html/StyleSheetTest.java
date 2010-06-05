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

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext.NamedStyle;
import javax.swing.text.StyleContext.SmallAttributeSet;
import javax.swing.text.html.CSS.Attribute;
import javax.swing.text.html.StyleSheet.BoxPainter;
import javax.swing.text.html.StyleSheet.ListPainter;

public class StyleSheetTest extends BasicSwingTestCase {
    private abstract class NumberFormatCase extends ExceptionalCase {
        public Class expectedExceptionClass() {
            return NumberFormatException.class;
        }
    }

    private static final int[] sizes = {8, 10, 12, 14, 18, 24, 36};

    private static final String CSS_RULES =
        "body {\n" +
        "    background-color: yellow;\n" +
        "    color: red;\n" +
        "}\n" +
        "p {\n" +
        "    text-indent: 1.25cm\n" +
        "}\n";

    private StyleSheet ss;
    private AttributeSet empty;
    private AttributeSet attr;
    private MutableAttributeSet simple;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        empty = ss.getEmptySet();
        simple = new SimpleAttributeSet();
    }

//    public void testStyleSheet() {
//
//    }

    /**
     * Shows that <code>StyleConstants.FontSize</code> is converted to
     * <code>CSS.Attribute.FONT_SIZE</code>.
     */
    public void testAddAttribute() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, new Integer(10));
        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.FONT_SIZE, name);
        assertFalse(names.hasMoreElements());

        assertTrue(attr instanceof SmallAttributeSet);
        assertSame(SmallAttributeSet.class, attr.getClass().getSuperclass());
    }

    /**
     * Tests <code>equals</code> method of <em>converter</em> attribute set.
     * <p>
     * This shows that result of <code>equals</code> depends on which object
     * is the receiver and which is the parameter. Also this shows that for
     * hash codes for <em>almost equal</em> objects are different.
     */
    public void testAddAttributeEquals() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, new Integer(10));
        simple.addAttribute(StyleConstants.FontSize, new Integer(10));

        assertTrue(attr.equals(simple));
        if (isHarmony()) {
            // In DRL implementation equals works fine
            assertTrue(simple.equals(attr));
        } else {
            assertFalse(simple.equals(attr));
        }
        assertEquals(attr, simple);
        assertFalse(attr.hashCode() == simple.hashCode());
    }

    /**
     * Shows that attribute is stored <em>AS IS</em> if there's no
     * <code>CSS</code> equivalent to <code>StyleConstants</code> attribute.
     */
    public void testAddAttributeNoCSS() {
        attr = ss.addAttribute(empty, StyleConstants.BidiLevel, new Integer(0));
        assertEquals(1, attr.getAttributeCount());

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(StyleConstants.BidiLevel, name);
        assertFalse(names.hasMoreElements());

        assertTrue(attr instanceof SmallAttributeSet);
        assertSame(SmallAttributeSet.class, attr.getClass().getSuperclass());
    }

    /**
     * Adding an attribute stored as CSS-attribute with StyleConstants key.
     */
    public void testAddAttributeCSSAsSC() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_SIZE, "21pt");
        assertEquals(1, simple.getAttributeCount());
        Object fs = simple.getAttribute(Attribute.FONT_SIZE);
        assertNotSame(String.class, fs.getClass());

        attr = ss.addAttribute(empty, StyleConstants.FontSize, fs);
        assertEquals(1, attr.getAttributeCount());
        Object css = attr.getAttribute(Attribute.FONT_SIZE);
        if (isHarmony()) {
            assertSame(fs, css);
        } else {
            assertNotSame(fs, css);
            assertFalse(css.equals(fs));
            assertSame(fs.getClass(), css.getClass());
            assertEquals(fs.toString(), css.toString());
        }
    }

    /**
     * Adding an attribute stored as CSS-attribute with CSS key.
     */
    public void testAddAttributeCSSAsCSS() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_SIZE, "21pt");
        assertEquals(1, simple.getAttributeCount());
        Object fs = simple.getAttribute(Attribute.FONT_SIZE);
        assertNotSame(String.class, fs.getClass());

        attr = ss.addAttribute(empty, Attribute.FONT_SIZE, fs);
        assertEquals(1, attr.getAttributeCount());
        Object css = attr.getAttribute(Attribute.FONT_SIZE);
        assertSame(fs, css);
    }

    /**
     * Adding an attribute stored as CSS-attribute with CSS key.
     */
    public void testAddAttributeCSSAsString() throws Exception {
        simple.addAttribute(Attribute.FONT_SIZE, "21pt");
        assertEquals(1, simple.getAttributeCount());
        Object fs = simple.getAttribute(Attribute.FONT_SIZE);
        assertSame(String.class, fs.getClass());

        attr = ss.addAttribute(empty, Attribute.FONT_SIZE, fs);
        assertEquals(1, attr.getAttributeCount());
        Object css = attr.getAttribute(Attribute.FONT_SIZE);
        if (isHarmony()) {
            assertNotSame(fs, css);
            assertEquals("21pt", css.toString());
            assertNotSame(fs.getClass(), css.getClass());
            assertNotSame(String.class, css.getClass());
        } else {
            assertSame(fs, css);
        }

        Object sc = attr.getAttribute(StyleConstants.FontSize);
        if (isHarmony()) {
            assertSame(Integer.class, sc.getClass());
            assertEquals(21, ((Integer)sc).intValue());
        } else {
            assertNull(sc);
        }
    }

    /**
     * Adding an attribute stored as CSS-attribute with CSS key.
     */
    public void testAddAttributeCSSAsInteger() throws Exception {
        simple.addAttribute(Attribute.FONT_SIZE, new Integer(21));
        assertEquals(1, simple.getAttributeCount());
        Object fs = simple.getAttribute(Attribute.FONT_SIZE);
        assertSame(Integer.class, fs.getClass());

        attr = ss.addAttribute(empty, Attribute.FONT_SIZE, fs);
        assertEquals(1, attr.getAttributeCount());
        Object css = attr.getAttribute(Attribute.FONT_SIZE);
        if (isHarmony()) {
            assertNotSame(fs, css);
            assertEquals("x-large", css.toString());
            assertNotSame(fs.getClass(), css.getClass());
            assertNotSame(String.class, css.getClass());
        } else {
            assertSame(fs, css);
        }

        Object sc = attr.getAttribute(StyleConstants.FontSize);
        if (isHarmony()) {
            assertSame(Integer.class, sc.getClass());
            assertEquals(24, ((Integer)sc).intValue());
        } else {
            assertNull(sc);
        }
    }

    /**
     * Adding an attribute with CSS-key but with invalid value for that key.
     */
    public void testAddAttributeInvalidValue01() throws Exception {
        if (isHarmony()) {
            testExceptionalCase(new NullPointerCase() {
                public void exceptionalAction() throws Exception {
                    ss.addAttribute(empty, Attribute.FONT_SIZE,
                                    "not-numeral");
                }
            });
            return;
        }
        attr = ss.addAttribute(empty, Attribute.FONT_SIZE, "not-numeral");
        assertEquals(1, attr.getAttributeCount());
        Object css = attr.getAttribute(Attribute.FONT_SIZE);
        assertEquals("not-numeral", css.toString());
        assertSame(String.class, css.getClass());
        Object sc = attr.getAttribute(StyleConstants.FontSize);
        assertNull(sc);
    }

    /**
     * Adding an invalid value for SC-attribute.
     */
    public void testAddAttributeInvalidValue02() throws Exception {
        if (isHarmony()) {
            testExceptionalCase(new NullPointerCase() {
                public void exceptionalAction() throws Exception {
                    ss.addAttribute(empty, StyleConstants.FontSize,
                                    "not-numeral");
                }
            });
            return;
        }
        attr = ss.addAttribute(empty, StyleConstants.FontSize, "not-numeral");
        assertEquals(1, attr.getAttributeCount());
        Object css = attr.getAttribute(Attribute.FONT_SIZE);
        assertEquals("not-numeral", css.toString());
        assertNotSame(String.class, css.getClass());

        Object sc = attr.getAttribute(StyleConstants.FontSize);
        assertSame(Integer.class, sc.getClass());
        assertEquals(12, ((Integer)sc).intValue());
    }

    /**
     * Shows that <code>StyleConstants</code> attributes are converted to
     * corresponding <code>CSS.Attribute</code> ones.
     */
    public void testAddAttributes() {
        simple.addAttribute(StyleConstants.FontSize, new Integer(10));
        simple.addAttribute(StyleConstants.Alignment,
                            new Integer(StyleConstants.ALIGN_CENTER));

        attr = ss.addAttributes(empty, simple);
        assertEquals(2, attr.getAttributeCount());

        Enumeration names = attr.getAttributeNames();
        boolean hasSize = false;
        boolean hasAlign = false;
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            hasSize |= name == Attribute.FONT_SIZE;
            hasAlign |= name == Attribute.TEXT_ALIGN;
        }
        assertTrue(hasSize);
        assertTrue(hasAlign);

        assertTrue(attr instanceof SmallAttributeSet);
        assertSame(SmallAttributeSet.class, attr.getClass().getSuperclass());
    }

    /**
     * Adding an attribute stored as CSS-attribute with StyleConstants key.
     */
    public void testAddAttributesCSSAsSC() {
        ss.addCSSAttribute(simple, Attribute.FONT_SIZE, "21pt");
        assertEquals(1, simple.getAttributeCount());
        Object fs = simple.getAttribute(Attribute.FONT_SIZE);
        assertNotSame(String.class, fs.getClass());
        simple.removeAttribute(Attribute.FONT_SIZE);
        simple.addAttribute(StyleConstants.FontSize, fs);

        attr = ss.addAttributes(empty, simple);
        assertEquals(1, attr.getAttributeCount());
        Object css = attr.getAttribute(Attribute.FONT_SIZE);
        if (isHarmony()) {
            assertSame(fs, css);
        } else {
            assertNotSame(fs, css);
            assertFalse(css.equals(fs));
            assertSame(fs.getClass(), css.getClass());
            assertEquals(fs.toString(), css.toString());
        }
    }

    /**
     * Adding an attribute stored as CSS-attribute with CSS key.
     */
    public void testAddAttributesCSSAsCSS() {
        ss.addCSSAttribute(simple, Attribute.FONT_SIZE, "21pt");
        assertEquals(1, simple.getAttributeCount());
        Object fs = simple.getAttribute(Attribute.FONT_SIZE);
        assertNotSame(String.class, fs.getClass());

        attr = ss.addAttributes(empty, simple);
        assertEquals(1, attr.getAttributeCount());
        Object css = attr.getAttribute(Attribute.FONT_SIZE);
        assertSame(fs, css);
    }

    public void testGetFont() {
        ss.addCSSAttribute(simple, Attribute.FONT_FAMILY, "serif");
        ss.addCSSAttribute(simple, Attribute.FONT_WEIGHT, "bold");
        assertEquals(2, simple.getAttributeCount());

        Font f = ss.getFont(simple);
        assertEquals(12, f.getSize());
        assertEquals("Serif", f.getName());
        assertEquals("Serif", f.getFamily());
        assertTrue(f.isBold());
        assertFalse(f.isItalic());
    }

    public void testGetFontCSSSizeAttribute() {
        ss.addCSSAttribute(simple, Attribute.FONT_SIZE, "21pt");
        assertEquals(1, simple.getAttributeCount());

        Font f = ss.getFont(simple);
        assertEquals(21, f.getSize());
        assertEquals("SansSerif", f.getName());
    }

    public void testGetFontSCSizeAttribute() {
        simple.addAttribute(StyleConstants.FontSize, new Integer(8));

        Font f = ss.getFont(simple);
        assertEquals(12, f.getSize());
        assertEquals("SansSerif", f.getName());
    }

    public void testGetFontCSSAndSCSizeAttributesMixed() {
        ss.addCSSAttribute(simple, Attribute.FONT_SIZE, "21pt");
        simple.addAttribute(StyleConstants.FontSize, new Integer(8));
        assertEquals(2, simple.getAttributeCount());

        Font f = ss.getFont(simple);
        assertEquals(21, f.getSize());
        assertEquals("SansSerif", f.getName());
    }

    public void testGetForeground() {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(50%, 25%, 75%)");
        assertEquals(1, simple.getAttributeCount());

        assertEquals(new Color(127, 63, 191), ss.getForeground(simple));

        assertSame(Color.BLACK, ss.getForeground(empty));
    }

    public void testGetForegroundSC() {
        simple.addAttribute(StyleConstants.Foreground, new Color(63, 127, 191));
        assertSame(Color.BLACK, ss.getForeground(empty));
    }

    public void testGetBackground() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND_COLOR,
                           "rgb(77%, 55%, 33%)");
        assertEquals(1, simple.getAttributeCount());

        assertEquals(new Color(196, 140, 84), ss.getBackground(simple));

        assertNull(ss.getBackground(empty));
    }

    public void testGetBackgroundSC() {
        simple.addAttribute(StyleConstants.Background, new Color(140, 196, 84));
        assertNull(ss.getBackground(empty));
    }

    public void testRemoveAttribute() {
        attr = ss.addAttribute(empty, StyleConstants.FontSize, new Integer(10));
        assertEquals(1, attr.getAttributeCount());
        assertNotNull(attr.getAttribute(Attribute.FONT_SIZE));
        assertNotNull(attr.getAttribute(StyleConstants.FontSize));

        attr = ss.removeAttribute(attr, StyleConstants.FontSize);
        assertEquals(0, attr.getAttributeCount());
        assertNull(attr.getAttribute(Attribute.FONT_SIZE));
        assertNull(attr.getAttribute(StyleConstants.FontSize));
    }

    public void testRemoveAttributesAttributeSetAttributeSet_Copy() {
        initAttributes();

        simple = new SimpleAttributeSet(attr);

        attr = ss.removeAttributes(attr, simple);
        assertEquals(0, attr.getAttributeCount());
        assertNull(attr.getAttribute(Attribute.FONT_WEIGHT));
        assertNull(attr.getAttribute(StyleConstants.Bold));
        assertNull(attr.getAttribute(Attribute.FONT_STYLE));
        assertNull(attr.getAttribute(StyleConstants.Italic));
    }

    public void testRemoveAttributesAttributeSetAttributeSet_CopyReversed() {
        initAttributes();

        simple = new SimpleAttributeSet(attr);

        attr = ss.removeAttributes(simple, attr);
        assertEquals(0, attr.getAttributeCount());
        assertNull(attr.getAttribute(Attribute.FONT_WEIGHT));
        assertNull(attr.getAttribute(StyleConstants.Bold));
        assertNull(attr.getAttribute(Attribute.FONT_STYLE));
        assertNull(attr.getAttribute(StyleConstants.Italic));
    }

    public void testRemoveAttributesAttributeSetAttributeSet_StyleConstants() {
        initAttributes();

        simple.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        simple.addAttribute(StyleConstants.Italic, Boolean.TRUE);

        assertTrue(attr.isEqual(simple));

        attr = ss.removeAttributes(attr, simple);
        if (isHarmony()) {
            assertEquals(0, attr.getAttributeCount());
            assertNull(attr.getAttribute(Attribute.FONT_WEIGHT));
            assertNull(attr.getAttribute(StyleConstants.Bold));
            assertNull(attr.getAttribute(Attribute.FONT_STYLE));
            assertNull(attr.getAttribute(StyleConstants.Italic));
        } else {
            assertEquals(2, attr.getAttributeCount());
            assertNotNull(attr.getAttribute(Attribute.FONT_WEIGHT));
            assertNotNull(attr.getAttribute(StyleConstants.Bold));
            assertNotNull(attr.getAttribute(Attribute.FONT_STYLE));
            assertNotNull(attr.getAttribute(StyleConstants.Italic));
        }
    }

    public void testRemoveAttributesAttributeSetAttributeSet_Reversed() {
        initAttributes();

        simple.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        simple.addAttribute(StyleConstants.Italic, Boolean.TRUE);

        assertTrue(attr.isEqual(simple));

        attr = ss.removeAttributes(simple, attr);
        assertEquals(2, attr.getAttributeCount());
        assertNull(attr.getAttribute(Attribute.FONT_WEIGHT));
        assertNotNull(attr.getAttribute(StyleConstants.Bold));
        assertNull(attr.getAttribute(Attribute.FONT_STYLE));
        assertNotNull(attr.getAttribute(StyleConstants.Italic));
    }

    public void testRemoveAttributesAttributeSetAttributeSet_Mixed() {
        initAttributes();

        simple.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        ss.addCSSAttribute(simple, Attribute.FONT_STYLE, "italic");


        attr = ss.removeAttributes(attr, simple);
        if (isHarmony()) {
            assertEquals(0, attr.getAttributeCount());
            assertNull(attr.getAttribute(Attribute.FONT_WEIGHT));
            assertNull(attr.getAttribute(StyleConstants.Bold));
            assertNull(attr.getAttribute(Attribute.FONT_STYLE));
            assertNull(attr.getAttribute(StyleConstants.Italic));
        } else {
            assertEquals(2, attr.getAttributeCount());
            assertNotNull(attr.getAttribute(Attribute.FONT_WEIGHT));
            assertNotNull(attr.getAttribute(StyleConstants.Bold));
            assertNotNull(attr.getAttribute(Attribute.FONT_STYLE));
            assertNotNull(attr.getAttribute(StyleConstants.Italic));
        }
    }

    public void testRemoveAttributesAttributeSetAttributeSet_MixedSameValue() {
        initAttributes();

        assertEquals(0, simple.getAttributeCount());
        simple.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        simple.addAttribute(Attribute.FONT_STYLE,
                            attr.getAttribute(Attribute.FONT_STYLE));
        assertEquals(2, simple.getAttributeCount());


        assertEquals(2, attr.getAttributeCount());
        assertNotNull(Attribute.FONT_WEIGHT);
        assertNotNull(Attribute.FONT_STYLE);

        attr = ss.removeAttributes(attr, simple);
        if (isHarmony()) {
            assertEquals(0, attr.getAttributeCount());
            assertNull(attr.getAttribute(Attribute.FONT_WEIGHT));
            assertNull(attr.getAttribute(StyleConstants.Bold));
            assertNull(attr.getAttribute(Attribute.FONT_STYLE));
            assertNull(attr.getAttribute(StyleConstants.Italic));
        } else {
            // FONT_STYLE was removed since simple contains the same
            // key-value pair as attr does
            assertEquals(1, attr.getAttributeCount());
            assertNotNull(attr.getAttribute(Attribute.FONT_WEIGHT));
            assertNotNull(attr.getAttribute(StyleConstants.Bold));
            assertNull(attr.getAttribute(Attribute.FONT_STYLE));
            assertNull(attr.getAttribute(StyleConstants.Italic));
        }
    }

    public void testRemoveAttributesAttributeSetEnumeration_StyleConstants() {
        initAttributes();

        simple.addAttribute(StyleConstants.Bold, Boolean.FALSE);
        simple.addAttribute(StyleConstants.Italic, Boolean.FALSE);

        attr = ss.removeAttributes(attr, simple.getAttributeNames());
        if (isHarmony()) {
            assertEquals(0, attr.getAttributeCount());
            assertNull(attr.getAttribute(Attribute.FONT_WEIGHT));
            assertNull(attr.getAttribute(StyleConstants.Bold));
            assertNull(attr.getAttribute(Attribute.FONT_STYLE));
            assertNull(attr.getAttribute(StyleConstants.Italic));
        } else {
            assertEquals(2, attr.getAttributeCount());
            assertNotNull(attr.getAttribute(Attribute.FONT_WEIGHT));
            assertNotNull(attr.getAttribute(StyleConstants.Bold));
            assertNotNull(attr.getAttribute(Attribute.FONT_STYLE));
            assertNotNull(attr.getAttribute(StyleConstants.Italic));
        }
    }

    public void testRemoveAttributesAttributeSetEnumeration_CSS() {
        initAttributes();

        simple.addAttribute(Attribute.FONT_STYLE, Boolean.FALSE);
        simple.addAttribute(Attribute.FONT_WEIGHT, Boolean.FALSE);

        attr = ss.removeAttributes(attr, simple.getAttributeNames());
        assertEquals(0, attr.getAttributeCount());
        assertNull(attr.getAttribute(Attribute.FONT_WEIGHT));
        assertNull(attr.getAttribute(StyleConstants.Bold));
        assertNull(attr.getAttribute(Attribute.FONT_STYLE));
        assertNull(attr.getAttribute(StyleConstants.Italic));
    }

    /**
     * Adds a simple attribute to set.
     * (<code>CSS.Attribute.BACKGROUND_COLOR</code> is used.)
     */
    public void testAddCSSAttribute01() {
        assertEquals(0, simple.getAttributeCount());
        ss.addCSSAttribute(simple, Attribute.BACKGROUND_COLOR, "red");

        assertEquals(1, simple.getAttributeCount());
        Enumeration names = simple.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.BACKGROUND_COLOR, name);
        assertFalse(names.hasMoreElements());

        Object value = simple.getAttribute(Attribute.BACKGROUND_COLOR);
        assertNotSame(Color.class, value.getClass());
        assertNotSame(String.class, value.getClass());
        assertEquals("red", value.toString());

        assertEquals("background-color=red ", simple.toString());
    }

    /**
     * Adds a shorthand attribute to set,
     * <code>CSS.Attribute.BACKGROUND</code> is used.
     * <p>Checks that this shorthand property is converted to individual
     * background-related properties.
     */
    public void testAddCSSAttribute02() {
        assertEquals(0, simple.getAttributeCount());
        ss.addCSSAttribute(simple, Attribute.BACKGROUND, "red repeat-y");

        assertEquals(5, simple.getAttributeCount());

        Attribute[] keys = {
            Attribute.BACKGROUND_ATTACHMENT,
            Attribute.BACKGROUND_COLOR,
            Attribute.BACKGROUND_IMAGE,
            Attribute.BACKGROUND_POSITION,
            Attribute.BACKGROUND_REPEAT
        };
        for (int i = 0; i < keys.length; i++) {
            assertTrue(keys[i] + " not found", simple.isDefined(keys[i]));
        }

        Object[] values = {
            Attribute.BACKGROUND_ATTACHMENT.getDefaultValue(),
            "red",
            isHarmony() ? Attribute.BACKGROUND_IMAGE.getDefaultValue() : null,
            isHarmony() ? "0% 0%" : Attribute.BACKGROUND_POSITION.getDefaultValue(),
            "repeat-y"
        };
        for (int i = 0; i < values.length; i++) {
            Object value = simple.getAttribute(keys[i]);
            assertNotNull("Attr value is null", value);
            assertEquals("Attr: " + keys[i],
                         values[i] != null ? values[i].toString() : null,
                         value.toString());
        }
    }

    /**
     * Shows that <code>equals</code> returns <code>false</code>
     * despite attribute sets contain equal values.
     */
    public void testAddCSSAttribute03() {
        assertEquals(0, simple.getAttributeCount());
        ss.addCSSAttribute(simple, Attribute.BACKGROUND_COLOR, "red");
        assertEquals(1, simple.getAttributeCount());

        MutableAttributeSet mas = new SimpleAttributeSet();
        mas.addAttribute(Attribute.BACKGROUND_COLOR, "red");

        Object key1 = simple.getAttributeNames().nextElement();
        Object key2 = mas.getAttributeNames().nextElement();
        assertEquals(key2, key1);
        assertSame(key2, key1);

        Object value1 = simple.getAttribute(key1);
        Object value2 = mas.getAttribute(key2);
        assertNotSame(value2, value1);
        assertFalse(value2.equals(value1));
        assertEquals(value2.toString(), value1.toString());

        assertEquals("background-color=red ", simple.toString());
        assertEquals("background-color=red ", mas.toString());

        assertFalse(simple.isEqual(mas));
    }

    public void testAddCSSAttributeFromHTML_Color() {
        assertTrue(ss.addCSSAttributeFromHTML(simple, Attribute.COLOR,
                                              "#112233"));
        assertEquals("#112233",
                     simple.getAttribute(Attribute.COLOR).toString());

        // Invalid color
        assertFalse(ss.addCSSAttributeFromHTML(simple, Attribute.COLOR,
                                              "color"));
        assertEquals("#112233",
                     simple.getAttribute(Attribute.COLOR).toString());

        // Standard color
        assertTrue(ss.addCSSAttributeFromHTML(simple, Attribute.COLOR,
                                               "red"));
        assertEquals("red",
                     simple.getAttribute(Attribute.COLOR).toString());
    }

    public void testAddCSSAttributeFromHTML_Align() {
        assertTrue(ss.addCSSAttributeFromHTML(simple, Attribute.TEXT_ALIGN,
                                              "left"));
        assertEquals("left",
                     simple.getAttribute(Attribute.TEXT_ALIGN).toString());

        // Incorrect align value. (The correct one is justify. Either is true
        // for CSS property)
        if (isHarmony()) {
            assertFalse(ss.addCSSAttributeFromHTML(simple, Attribute.TEXT_ALIGN,
                                                   "justified"));
            // Old value preserved
            assertEquals("left",
                         simple.getAttribute(Attribute.TEXT_ALIGN).toString());
        } else {
            assertTrue(ss.addCSSAttributeFromHTML(simple, Attribute.TEXT_ALIGN,
                                                  "justified"));
            assertEquals("justified",
                         simple.getAttribute(Attribute.TEXT_ALIGN).toString());
        }
    }

    public void testAddCSSAttributeFromHTML_Background() {
        assertTrue(ss.addCSSAttributeFromHTML(simple,
                                              Attribute.BACKGROUND_IMAGE,
                                              "bg.jpg"));
        assertEquals(isHarmony() ? "url(bg.jpg)" : "bg.jpg",
                     simple.getAttribute(Attribute.BACKGROUND_IMAGE)
                     .toString());
    }

    public void testAddCSSAttributeFromHTML_AddCSS() {
        final Marker marker = new Marker();
        ss = new StyleSheet() {
            public void addCSSAttribute(final MutableAttributeSet attr,
                                        final Attribute key,
                                        final String value) {
                marker.setOccurred();
                super.addCSSAttribute(attr, key, value);
            }
        };
        assertTrue(ss.addCSSAttributeFromHTML(simple, Attribute.TEXT_ALIGN,
                                              "left"));
        assertEquals(isHarmony(), marker.isOccurred());
    }

    public void testCreateSmallAttributeSet() {
        final Object value = new Integer(12);
        simple.addAttribute(StyleConstants.FontSize, value);
        attr = ss.createSmallAttributeSet(simple);
        assertTrue(attr instanceof SmallAttributeSet);
        assertNotSame(SmallAttributeSet.class, attr.getClass());

        assertEquals(1, attr.getAttributeCount());
        assertSame(value, attr.getAttribute(StyleConstants.FontSize));
        assertNull(attr.getAttribute(Attribute.FONT_SIZE));
    }

    public void testCreateLargeAttributeSet() {
        final Object value = new Integer(12);
        simple.addAttribute(StyleConstants.FontSize, value);
        attr = ss.createLargeAttributeSet(simple);
        assertTrue(attr instanceof SimpleAttributeSet);
        assertNotSame(SimpleAttributeSet.class, attr.getClass());

        assertEquals(1, attr.getAttributeCount());
        assertSame(value, attr.getAttribute(StyleConstants.FontSize));
        assertNull(attr.getAttribute(Attribute.FONT_SIZE));
    }

    public void testGetBoxPainter() {
        BoxPainter bp = ss.getBoxPainter(empty);
        assertNotNull(bp);
        assertNotSame(bp, ss.getBoxPainter(empty));
    }

    public void testGetBoxPainterAttributes() {
        final Marker borderStyle = new Marker();
        final Marker marginTop = new Marker();
        final Marker marginRight = new Marker();
        final Marker marginBottom = new Marker();
        final Marker marginLeft = new Marker();
        final Marker backgroundColor = new Marker();
        final Marker backgroundImage = new Marker();

        simple = new SimpleAttributeSet() {
            public Object getAttribute(Object name) {
                if (name == CSS.Attribute.BORDER_STYLE) {
                    borderStyle.setOccurred();
                } else if (name == CSS.Attribute.MARGIN_TOP) {
                    marginTop.setOccurred();
                } else if (name == CSS.Attribute.MARGIN_RIGHT) {
                    marginRight.setOccurred();
                } else if (name == CSS.Attribute.MARGIN_BOTTOM) {
                    marginBottom.setOccurred();
                } else if (name == CSS.Attribute.MARGIN_LEFT) {
                    marginLeft.setOccurred();
                } else if (name == CSS.Attribute.BACKGROUND_COLOR) {
                    backgroundColor.setOccurred();
                } else if (name == CSS.Attribute.BACKGROUND_IMAGE) {
                    backgroundImage.setOccurred();
                } else {
                    fail("Unexpected attribute is requested: " + name);
                }
                return super.getAttribute(name);
            }
        };
        final BoxPainter bp = ss.getBoxPainter(simple);

        if (isHarmony()) {
            assertFalse(borderStyle.isOccurred());
            assertFalse(marginTop.isOccurred());
            assertFalse(marginRight.isOccurred());
            assertFalse(marginBottom.isOccurred());
            assertFalse(marginLeft.isOccurred());
            assertFalse(backgroundColor.isOccurred());
            assertFalse(backgroundImage.isOccurred());

            bp.setView(null);

            assertFalse(borderStyle.isOccurred());

            assertFalse(backgroundColor.isOccurred());
            assertFalse(backgroundImage.isOccurred());
        } else {
            assertTrue(borderStyle.isOccurred());

            assertTrue(backgroundColor.isOccurred());
            assertTrue(backgroundImage.isOccurred());
        }

        assertTrue(marginTop.isOccurred());
        assertTrue(marginRight.isOccurred());
        assertTrue(marginBottom.isOccurred());
        assertTrue(marginLeft.isOccurred());
    }

    public void testGetBoxPainterAttributesBorderStyle() {
        final Marker borderStyle = new Marker();
        final Marker borderTopWidth = new Marker();
        final Marker borderRightWidth = new Marker();
        final Marker borderBottomWidth = new Marker();
        final Marker borderLeftWidth = new Marker();

        simple = new SimpleAttributeSet() {
            public Object getAttribute(Object name) {
                if (name == CSS.Attribute.BORDER_STYLE) {
                    borderStyle.setOccurred();
                } else if (name == CSS.Attribute.BORDER_TOP_WIDTH) {
                    borderTopWidth.setOccurred();
                } else if (name == CSS.Attribute.BORDER_RIGHT_WIDTH) {
                    borderRightWidth.setOccurred();
                } else if (name == CSS.Attribute.BORDER_BOTTOM_WIDTH) {
                    borderBottomWidth.setOccurred();
                } else if (name == CSS.Attribute.BORDER_LEFT_WIDTH) {
                    borderLeftWidth.setOccurred();
                } else if (name == CSS.Attribute.MARGIN_TOP
                           || name == CSS.Attribute.MARGIN_RIGHT
                           || name == CSS.Attribute.MARGIN_BOTTOM
                           || name == CSS.Attribute.MARGIN_LEFT
                           || name == CSS.Attribute.BACKGROUND_COLOR
                           || name == CSS.Attribute.BACKGROUND_IMAGE) {
                    ;
                } else {
                    fail("Unexpected attribute is requested: " + name);
                }
                return super.getAttribute(name);
            }
        };
        ss.addCSSAttribute(simple, CSS.Attribute.BORDER_STYLE, "solid");
        final BoxPainter bp = ss.getBoxPainter(simple);

        if (isHarmony()) {
            assertFalse(borderStyle.isOccurred());
            assertFalse(borderTopWidth.isOccurred());
            assertFalse(borderRightWidth.isOccurred());
            assertFalse(borderBottomWidth.isOccurred());
            assertFalse(borderLeftWidth.isOccurred());

            bp.setView(null);

            assertFalse(borderStyle.isOccurred());
            assertFalse(borderTopWidth.isOccurred());
        } else {
            assertTrue(borderStyle.isOccurred());
            assertTrue(borderTopWidth.isOccurred());
        }
        assertFalse(borderRightWidth.isOccurred());
        assertFalse(borderBottomWidth.isOccurred());
        assertFalse(borderLeftWidth.isOccurred());
    }

    public void testGetListPainter() {
        ListPainter lp = ss.getListPainter(empty);
        assertNotNull(lp);
        assertNotSame(lp, ss.getListPainter(empty));
    }

    public void testGetListPainterAttributes() {
        final Marker listStyleImage = new Marker();
        final Marker listStyleType = new Marker();

        simple = new SimpleAttributeSet() {
            public Object getAttribute(Object name) {
                if (name == CSS.Attribute.LIST_STYLE_IMAGE) {
                    listStyleImage.setOccurred();
                } else if (name == CSS.Attribute.LIST_STYLE_TYPE) {
                    listStyleType.setOccurred();
                } else {
                    fail("Unexpected attribute is requested: " + name);
                }
                return super.getAttribute(name);
            }
        };
        ss.getListPainter(simple);

        if (isHarmony()) {
            assertFalse(listStyleImage.isOccurred());
            assertFalse(listStyleType.isOccurred());
        } else {
            assertTrue(listStyleImage.isOccurred());
            assertTrue(listStyleType.isOccurred());
        }
    }

    public void testImportStyleSheet() throws Exception {
        final File cssFile = File.createTempFile(getName(), ".css");
        cssFile.deleteOnExit();
        final FileWriter writer = new FileWriter(cssFile);
        writer.write(CSS_RULES);
        writer.close();

        ss.importStyleSheet(cssFile.toURL());

        final Style body = ss.getStyle("body");
        assertEquals(2, body.getAttributeCount());
        final AttributeSet bodyAttr = body.getResolveParent();
        assertTrue(bodyAttr instanceof NamedStyle);
        assertEquals(2, bodyAttr.getAttributeCount());
        assertEquals("yellow", bodyAttr.getAttribute(Attribute.BACKGROUND_COLOR)
                               .toString());
        assertEquals("red", bodyAttr.getAttribute(Attribute.COLOR).toString());

        final Style p = ss.getStyle("p");
        assertEquals(2, p.getAttributeCount());
        final AttributeSet pAttr = p.getResolveParent();
        assertEquals(1, pAttr.getAttributeCount());
        assertEquals("1.25cm", pAttr.getAttribute(Attribute.TEXT_INDENT)
                               .toString());
    }

    public void testImportStyleSheetAddStyles() throws Exception {
        final File cssFile = File.createTempFile(getName(), ".css");
        cssFile.deleteOnExit();
        final FileWriter writer = new FileWriter(cssFile);
        writer.write("body {\n" +
                     "    background-color: white;\n" +
                     "}\n" +
                     "body {\n" +
                     "    color: black;\n" +
                     "}");
        writer.close();

        assertNull(ss.getStyle("body"));

        ss.importStyleSheet(cssFile.toURL());

        final Style body = ss.getStyle("body");
        assertEquals(2, body.getAttributeCount());
        final AttributeSet bodyAttr = body.getResolveParent();
        assertEquals(2, bodyAttr.getAttributeCount());
        assertEquals("white", bodyAttr.getAttribute(Attribute.BACKGROUND_COLOR)
                              .toString());
        assertEquals("black", bodyAttr.getAttribute(Attribute.COLOR).toString());
    }

    public void testImportStyleSheetWithImports() throws Exception {
        final File cssFile = File.createTempFile(getName(), ".css");
        cssFile.deleteOnExit();
        FileWriter writer = new FileWriter(cssFile);
        writer.write("body {\n" +
                     "    color: white;\n" +
                     "}");
        writer.close();

        final File importingCSS = File.createTempFile(getName() + "Importing",
                                                      ".css");
        importingCSS.deleteOnExit();
        writer = new FileWriter(importingCSS);
        writer.write("@import url(" + cssFile.toURL() + ");\n" +
                     "body {\n" +
                     "    text-align: center\n" +
                     "}");
        writer.close();

        assertNull(ss.getStyle("body"));
        assertNull(ss.getStyleSheets());

        ss.importStyleSheet(importingCSS.toURL());

        assertNull(ss.getStyleSheets());

        final Style body = ss.getStyle("body");
        assertEquals(2, body.getAttributeCount());
        final AttributeSet bodyAttr = body.getResolveParent();
        assertEquals(2, bodyAttr.getAttributeCount());
        assertEquals("white", bodyAttr.getAttribute(Attribute.COLOR).toString());
        assertEquals("center", bodyAttr.getAttribute(Attribute.TEXT_ALIGN)
                               .toString());
    }

    public void testImportStyleSheetNull() throws Exception {
        ss.importStyleSheet(null);
    }

    public void testLoadRules() throws Exception {
        final File cssFile = File.createTempFile(getName(), ".css");
        cssFile.deleteOnExit();
        final FileWriter writer = new FileWriter(cssFile);
        writer.write(CSS_RULES);
        writer.close();

        final Marker readerClose = new Marker();
        final FileReader reader = new FileReader(cssFile) {
            public void close() throws IOException {
                readerClose.setOccurred();
                super.close();
            }
        };
        ss.loadRules(reader, null);
        assertEquals(isHarmony(), readerClose.isOccurred());
        reader.close();

        final Style body = ss.getStyle("body");
        assertEquals(3, body.getAttributeCount());
        assertNull(body.getResolveParent());
        assertEquals("yellow", body.getAttribute(Attribute.BACKGROUND_COLOR)
                               .toString());
        assertEquals("red", body.getAttribute(Attribute.COLOR).toString());

        final Style p = ss.getStyle("p");
        assertEquals(2, p.getAttributeCount());
        assertNull(p.getResolveParent());
        assertEquals("1.25cm", p.getAttribute(Attribute.TEXT_INDENT)
                               .toString());
    }

    public void testLoadRulesWithImports() throws Exception {
        final File cssFile = File.createTempFile(getName(), ".css");
        cssFile.deleteOnExit();
        FileWriter writer = new FileWriter(cssFile);
        writer.write("body {\n" +
                     "    color: white;\n" +
                     "}");
        writer.close();

        final File importingCSS = File.createTempFile(getName() + "Importing",
                                                      ".css");
        importingCSS.deleteOnExit();
        writer = new FileWriter(importingCSS);
        writer.write("@import url('" + cssFile.toURL() + "');\n" +
                     "body {\n" +
                     "    text-align: center\n" +
                     "}");
        writer.close();

        assertNull(ss.getStyle("body"));
        assertNull(ss.getStyleSheets());

        ss.loadRules(new FileReader(importingCSS), null);

        assertNull(ss.getStyleSheets());

        final Style body = ss.getStyle("body");
        assertEquals(3, body.getAttributeCount());
        assertEquals("center", body.getAttribute(Attribute.TEXT_ALIGN)
                               .toString());
        final AttributeSet bodyAttr = body.getResolveParent();
        assertEquals(1, bodyAttr.getAttributeCount());
        assertEquals("white", bodyAttr.getAttribute(Attribute.COLOR).toString());
    }

    public void testSetBase() throws MalformedURLException {
        URL base = new URL("http://www.somesite.com/styles/");
        ss.setBase(base);
        assertSame(base, ss.getBase());
    }

    public void testGetBase() {
        assertNull(ss.getBase());
    }

    public void testGetIndexOfSize() {
        int[][] size = {
            // {scSizeSet, cssSize, scSizeRead}
             {6,  1},        {7,  1},
             {8,  1},        {9,  2},
            {10,  2},       {11,  3},
            {12,  3},       {13,  4},
            {14,  4},       {15,  5},
            {16,  5},       {17,  5},
            {18,  5},       {19,  6},
            {20,  6},       {21,  6},
            {22,  6},       {23,  6},
            {24,  6},       {25,  7},
            {26,  7},       {27,  7},
            {28,  7},       {29,  7},
            {30,  7},       {31,  7},
            {32,  7},       {33,  7},
            {34,  7},       {35,  7},
            {36,  7},       {37,  7},
            {38,  7},       {39,  7},
            {40,  7}
        };
        for (int i = 0; i < size.length; i++) {
            assertEquals("@ " + i, size[i][1],
                         StyleSheet.getIndexOfSize(size[i][0]));
        }
    }

    public void testSetBaseFontSizeInt() {
        ss.setBaseFontSize(3);
        assertEquals(sizes[3], (int)ss.getPointSize("+1"));
        assertEquals(sizes[2], (int)ss.getPointSize("+0"));
        assertEquals(sizes[1], (int)ss.getPointSize("-1"));

        ss.setBaseFontSize(5);
        assertEquals(sizes[5], (int)ss.getPointSize("+1"));
        assertEquals(sizes[4], (int)ss.getPointSize("+0"));
        assertEquals(sizes[3], (int)ss.getPointSize("-1"));
    }

    public void testSetBaseFontSizeString() {
        assertEquals(7, sizes.length);
        for (int i = 0; i < sizes.length; i++) {
            ss.setBaseFontSize(String.valueOf(i + 1));
            assertEquals("@ " + i, sizes[i], (int)ss.getPointSize("+0"));
        }
        ss.setBaseFontSize("0");
        assertEquals(sizes[0], (int)ss.getPointSize("+0"));
        ss.setBaseFontSize("8");
        assertEquals(sizes[6], (int)ss.getPointSize("+0"));
    }

    public void testSetBaseFontSizeStringRelativeUp() {
        assertEquals(sizes[3], (int)ss.getPointSize("+0"));
        ss.setBaseFontSize("+1");
        assertEquals(sizes[5], (int)ss.getPointSize("+1"));
        assertEquals(sizes[4], (int)ss.getPointSize("+0"));
        assertEquals(sizes[3], (int)ss.getPointSize("-1"));

        ss.setBaseFontSize("+2");
        assertEquals(sizes[6], (int)ss.getPointSize("+1"));
        assertEquals(sizes[6], (int)ss.getPointSize("+0"));
        assertEquals(sizes[5], (int)ss.getPointSize("-1"));
    }

    public void testSetBaseFontSizeStringRelativeDown() {
        assertEquals(sizes[3], (int)ss.getPointSize("+0"));
        ss.setBaseFontSize("-1");
        assertEquals(sizes[3], (int)ss.getPointSize("+1"));
        assertEquals(sizes[2], (int)ss.getPointSize("+0"));
        assertEquals(sizes[1], (int)ss.getPointSize("-1"));

        ss.setBaseFontSize("-2");
        assertEquals(sizes[1], (int)ss.getPointSize("+1"));
        assertEquals(sizes[0], (int)ss.getPointSize("+0"));
        assertEquals(sizes[0], (int)ss.getPointSize("-1"));
    }

    public void testSetBaseFontSizeStringInvalidLetter() {
        testExceptionalCase(new NumberFormatCase() {
            public void exceptionalAction() throws Exception {
                ss.setBaseFontSize("a");
            }
        });
    }

    public void testSetBaseFontSizeStringInvalidMinus() {
        if (isHarmony()) {
            testExceptionalCase(new NumberFormatCase() {
                public void exceptionalAction() throws Exception {
                    ss.setBaseFontSize("--1");
                }
            });
        } else {
            assertEquals(sizes[3], (int)ss.getPointSize("+0"));
            ss.setBaseFontSize("--1");
            assertEquals(sizes[4], (int)ss.getPointSize("+0"));
            testExceptionalCase(new NumberFormatCase() {
                public void exceptionalAction() throws Exception {
                    ss.setBaseFontSize("---1");
                }
            });
        }
    }

    public void testSetBaseFontSizeStringInvalidPlus() {
        testExceptionalCase(new NumberFormatCase() {
            public void exceptionalAction() throws Exception {
                ss.setBaseFontSize("++1");
            }
        });
    }

    public void testGetPointSizeInt() {
        assertEquals(7, sizes.length);
        for (int i = 0; i < sizes.length; i++) {
            assertEquals("@ " + i, sizes[i], (int)ss.getPointSize(i + 1));
        }
        assertEquals(sizes[0], (int)ss.getPointSize(0));
        assertEquals(sizes[6], (int)ss.getPointSize(8));
    }

    public void testGetPointSizeString() {
        assertEquals(7, sizes.length);
        for (int i = 0; i < sizes.length; i++) {
            assertEquals("@ " + i, sizes[i],
                         (int)ss.getPointSize(String.valueOf(i + 1)));
        }
        assertEquals(sizes[0], (int)ss.getPointSize("0"));
        assertEquals(sizes[6], (int)ss.getPointSize("7"));
    }

    public void testGetPointSizeStringRelative() {
        assertEquals(sizes[4], (int)ss.getPointSize("+1"));
        assertEquals(sizes[5], (int)ss.getPointSize("+2"));
        assertEquals(sizes[6], (int)ss.getPointSize("+3"));
        assertEquals(sizes[6], (int)ss.getPointSize("+4"));
        assertEquals(sizes[2], (int)ss.getPointSize("-1"));
        assertEquals(sizes[1], (int)ss.getPointSize("-2"));
        assertEquals(sizes[0], (int)ss.getPointSize("-3"));
        assertEquals(sizes[0], (int)ss.getPointSize("-4"));
    }

    public void testGetPointSizeStringRelativeBase() {
        ss.setBaseFontSize(5);
        assertEquals(sizes[5], (int)ss.getPointSize("+1"));
        assertEquals(sizes[6], (int)ss.getPointSize("+2"));
        assertEquals(sizes[6], (int)ss.getPointSize("+3"));
        assertEquals(sizes[3], (int)ss.getPointSize("-1"));
        assertEquals(sizes[2], (int)ss.getPointSize("-2"));
        assertEquals(sizes[1], (int)ss.getPointSize("-3"));
    }

    public void testGetPointSizeStringRelativeBaseLeftEnd() {
        ss.setBaseFontSize(1);
        assertEquals(sizes[1], (int)ss.getPointSize("+1"));
        assertEquals(sizes[0], (int)ss.getPointSize("-1"));
        assertEquals(sizes[0], (int)ss.getPointSize("-2"));
        assertEquals(sizes[0], (int)ss.getPointSize("-3"));
    }

    public void testGetPointSizeStringRelativeBaseRightEnd() {
        ss.setBaseFontSize(7);
        assertEquals(sizes[6], (int)ss.getPointSize("+1"));
        assertEquals(sizes[6], (int)ss.getPointSize("+2"));
        assertEquals(sizes[6], (int)ss.getPointSize("+3"));
        assertEquals(sizes[5], (int)ss.getPointSize("-1"));
    }

    public void testGetPointSizeStringInvalidLetter() {
        testExceptionalCase(new NumberFormatCase() {
            public void exceptionalAction() throws Exception {
                ss.getPointSize("a");
            }
        });
    }

    public void testGetPointSizeStringInvalidMinus() {
        if (isHarmony()) {
            testExceptionalCase(new NumberFormatCase() {
                public void exceptionalAction() throws Exception {
                    ss.getPointSize("--1");
                }
            });
        } else {
            assertEquals(sizes[4], (int)ss.getPointSize("--1"));
            testExceptionalCase(new NumberFormatCase() {
                public void exceptionalAction() throws Exception {
                    ss.getPointSize("---1");
                }
            });
        }
    }

    public void testGetPointSizeStringInvalidPlus() {
        testExceptionalCase(new NumberFormatCase() {
            public void exceptionalAction() throws Exception {
                ss.getPointSize("++1");
            }
        });
    }

    /**
     * Tests convertion of standard colors.
     */
    public void testStringToColor01() {
        final String[] names = {
            "aqua",
            "black",
            "blue",
            "fuchsia",
            "gray",
            "green",
            "lime",
            "maroon",
            "navy",
            "olive",
            "purple",
            "red",
            "silver",
            "teal",
            "white",
            "yellow"
        };
        final String[] hex = {
            "#00ffff",
            "#000000",
            "#0000ff",
            "#ff00ff",
            "#808080",
            "#008000",
            "#00ff00",
            "#800000",
            "#000080",
            "#808000",
            "#800080",
            "#ff0000",
            "#c0c0c0",
            "#008080",
            "#ffffff",
            "#ffff00"
        };
        final Color[] values = {
            Color.CYAN,
            Color.BLACK,
            Color.BLUE,
            Color.MAGENTA,
            Color.GRAY,
            new Color(0, 128, 0),
            Color.GREEN,
            new Color(128, 0, 0),
            new Color(0, 0, 128),
            new Color(128, 128, 0),
            new Color(128, 0, 128),
            Color.RED,
            Color.LIGHT_GRAY,
            new Color(0, 128, 128),
            Color.WHITE,
            Color.YELLOW
        };

        assertEquals(names.length, values.length);
        for (int i = 0; i < names.length; i++) {
            Color color = ss.stringToColor(names[i]);
            assertEquals("@ " + i + " '" + names[i] + "'",
                         values[i], color);
            assertEquals("@ " + i + " '" + hex[i] + "'",
                         values[i], ss.stringToColor(hex[i]));
            if (isHarmony()) {
                assertSame("@ " + i + " '" + names[i] + "'",
                           color, ss.stringToColor(names[i]));
                assertSame("@ " + i + " '" + hex[i] + "'",
                           color, ss.stringToColor(hex[i]));
            } else {
                assertNotSame("@ " + i + " '" + names[i] + "'",
                              color, ss.stringToColor(names[i]));
            }
        }
    }

    /**
     * Tests convertion of hex strings.
     */
    public void testStringToColor02() {
        assertEquals(new Color(0x1E, 0x2F, 0xFF), ss.stringToColor("#1E2FFF"));
        assertEquals(new Color(0xFF, 0x11, 0x22), ss.stringToColor("#FF1122"));
        assertEquals(new Color(0x12, 0x33, 0x21), ss.stringToColor("#123321"));

        if (isHarmony()) {
            assertEquals(new Color(0xFF, 0xFF, 0xFF), ss.stringToColor("#fff"));

            assertNull(ss.stringToColor("#f"));

            assertNull(ss.stringToColor("15"));
        } else {
            assertEquals(new Color(0x00, 0x0F, 0xFF), ss.stringToColor("#fff"));

            assertEquals(new Color(0x0, 0x0, 0x0F), ss.stringToColor("#f"));

            assertEquals(new Color(0x0, 0x0, 0x15), ss.stringToColor("15"));
        }


        assertNull(ss.stringToColor("zoom"));
    }

    /**
     * Tests convertion of mixed-case standard names.
     */
    public void testStringToColor03() {
        assertEquals(Color.RED, ss.stringToColor("rEd"));
        assertEquals(Color.BLACK, ss.stringToColor("bLaCk"));
        assertEquals(Color.WHITE, ss.stringToColor("White"));
    }

    /**
     * Tests convertion of extended list of named colors.
     */
    public void testStringToColor04() {
        assertNull(ss.stringToColor("azure"));
        assertNull(ss.stringToColor("blanchedalmond"));
        assertNull(ss.stringToColor("mistyrose"));
        assertNull(ss.stringToColor("lavender"));
        assertNull(ss.stringToColor("floralwhite"));
    }

    /**
     * Tests with empty string.
     */
    public void testStringToColor05() {
        if (isHarmony()) {
            assertNull(ss.stringToColor(""));
        } else {
            assertEquals(Color.BLACK, ss.stringToColor(""));
        }

        if (!isHarmony()) {
            testExceptionalCase(new NullPointerCase() {
                public void exceptionalAction() throws Exception {
                    ss.stringToColor(null);
                }
            });
        } else {
            assertNull(ss.stringToColor(null));
        }
    }

    public void testTranslateHTMLToCSS() {
        if (!isHarmony()) {
            // Calling ss.translateHTMLToCSS with all the classes I know which
            // implement AttributeSet throws ClassCastException
            //ss.translateHTMLToCSS(simple);
            //ss.translateHTMLToCSS(ss.createSmallAttributeSet(simple));
            //ss.translateHTMLToCSS(ss.createLargeAttributeSet(simple));
            //ss.translateHTMLToCSS(ss.new SmallAttributeSet(simple));
            //ss.translateHTMLToCSS(ss.getStyle("default"));
            //ss.translateHTMLToCSS(ss.getRule("default"));
            return;
        }
        simple.addAttribute(HTML.Attribute.BGCOLOR, "yellow");
        simple.addAttribute(HTML.Attribute.BACKGROUND, "bg.jpg");
        simple.addAttribute(HTML.Attribute.WIDTH, "100%");
//        System.out.println("Original:\n" + simple);

        attr = ss.translateHTMLToCSS(simple);
//        System.out.println("\nTranslated:\n" + attr);
        assertEquals("yellow",
                     attr.getAttribute(Attribute.BACKGROUND_COLOR).toString());
        assertEquals("url(bg.jpg)",
                     attr.getAttribute(Attribute.BACKGROUND_IMAGE).toString());
        assertEquals("100%", attr.getAttribute(Attribute.WIDTH).toString());
    }

    private void initAttributes() {
        attr = ss.addAttribute(empty, StyleConstants.Bold, Boolean.TRUE);
        attr = ss.addAttribute(attr, StyleConstants.Italic, Boolean.TRUE);
        assertEquals(2, attr.getAttributeCount());
        assertNotNull(attr.getAttribute(Attribute.FONT_WEIGHT));
        assertNotNull(attr.getAttribute(StyleConstants.Bold));
        assertNotNull(attr.getAttribute(Attribute.FONT_STYLE));
        assertNotNull(attr.getAttribute(StyleConstants.Italic));
    }
}
