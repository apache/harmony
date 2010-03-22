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
import java.util.Enumeration;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS.Attribute;

public class StyleSheet_ConvertAttr_ColorTest extends BasicSwingTestCase {
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

    public void testColor() {
        final Color color = new Color(0xAF11FA);
        attr = ss.addAttribute(empty, StyleConstants.Foreground, color);

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.class, name.getClass());
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertSame(Color.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(Color.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("#af11fa", cssValue.toString());
        assertEquals(color.toString(), scValue.toString());
        assertEquals(color, scValue);
    }

    public void testColorString() {
        if (!isHarmony()) {
            testExceptionalCase(new ClassCastCase() {
                public void exceptionalAction() throws Exception {
                    ss.addAttribute(empty, StyleConstants.Foreground,
                                    "#AD66DA");
                }
            });
            return;
        }
        attr = ss.addAttribute(empty, StyleConstants.Foreground, "#AD66DA");

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertSame(Color.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(Color.class, cssValue.getClass());
        assertEquals("#AD66DA", cssValue.toString());
        assertEquals(new Color(0xAD, 0x66, 0xDA), scValue);
    }

    public void testColorBLACKColor() {
        attr = ss.addAttribute(empty, StyleConstants.Foreground, Color.BLACK);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertSame(Color.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(Color.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("#000000", cssValue.toString());
        assertSame(Color.BLACK, scValue);
    }

    public void testColorWHITEColor() {
        attr = ss.addAttribute(empty, StyleConstants.Foreground, Color.WHITE);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertSame(Color.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(Color.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("#ffffff", cssValue.toString());
        assertSame(Color.WHITE, scValue);
    }

    public void testColorNonSC() {
        attr = ss.addAttribute(empty, Attribute.COLOR, Color.WHITE);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        if (isHarmony()) {
            assertSame(Color.class, scValue.getClass());
            assertNotSame(String.class, cssValue.getClass());
            assertNotSame(Color.class, cssValue.getClass());
            assertEquals("#ffffff", cssValue.toString());
        } else {
            assertNull(scValue);
            assertSame(Color.class, cssValue.getClass());
            assertEquals(Color.WHITE.toString(), cssValue.toString());
        }
    }


    public void testBackgroundColor() {
        final Color color = new Color(0xAF11FA);
        attr = ss.addAttribute(empty, StyleConstants.Background, color);

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.class, name.getClass());
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.BACKGROUND_COLOR);
        scValue = attr.getAttribute(StyleConstants.Background);
        assertSame(Color.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(Color.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("#af11fa", cssValue.toString());
        assertEquals(color.toString(), scValue.toString());
    }

    public void testBackgroundColorString() {
        if (!isHarmony()) {
            testExceptionalCase(new ClassCastCase() {
                public void exceptionalAction() throws Exception {
                    ss.addAttribute(empty, StyleConstants.Background,
                                    "#AD66DA");
                }
            });
            return;
        }

        attr = ss.addAttribute(empty, StyleConstants.Background, "#AD66DA");

        cssValue = attr.getAttribute(Attribute.BACKGROUND_COLOR);
        scValue = attr.getAttribute(StyleConstants.Background);
        assertSame(Color.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(Color.class, cssValue.getClass());
        assertEquals("#AD66DA", cssValue.toString());
        assertEquals(new Color(0xAD, 0x66, 0xDA), scValue);
    }

    public void testBackgroundColorBLACKColor() {
        attr = ss.addAttribute(empty, StyleConstants.Background, Color.BLACK);

        cssValue = attr.getAttribute(Attribute.BACKGROUND_COLOR);
        scValue = attr.getAttribute(StyleConstants.Background);
        assertSame(Color.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(Color.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("#000000", cssValue.toString());
        assertSame(Color.BLACK, scValue);
    }

    public void testBackgroundColorWHITEColor() {
        attr = ss.addAttribute(empty, StyleConstants.Background, Color.WHITE);

        cssValue = attr.getAttribute(Attribute.BACKGROUND_COLOR);
        scValue = attr.getAttribute(StyleConstants.Background);
        assertSame(Color.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(Color.class, cssValue.getClass());
        assertNotSame(scValue, cssValue);
        assertFalse(scValue.equals(cssValue));
        assertEquals("#ffffff", cssValue.toString());
        assertSame(Color.WHITE, scValue);
    }

    public void testBackgroundColorNonSC() {
        attr = ss.addAttribute(empty, Attribute.BACKGROUND_COLOR, Color.WHITE);

        cssValue = attr.getAttribute(Attribute.BACKGROUND_COLOR);
        scValue = attr.getAttribute(StyleConstants.Background);
        if (isHarmony()) {
            assertSame(Color.class, scValue.getClass());
            assertNotSame(String.class, cssValue.getClass());
            assertNotSame(Color.class, cssValue.getClass());
            assertEquals("#ffffff", cssValue.toString());
        } else {
            assertNull(scValue);
            assertSame(Color.class, cssValue.getClass());
            assertEquals(Color.WHITE.toString(), cssValue.toString());
        }
    }



    public void testColorRed01() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "red");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertSame(Color.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(Color.class, cssValue.getClass());
        assertEquals("red", cssValue.toString());
        if (isHarmony()) {
            assertSame(Color.RED, scValue);
        } else {
            assertEquals(Color.RED, scValue);
        }
    }

    public void testColorRed02() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "Red");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("Red", cssValue.toString());
        if (isHarmony()) {
            assertSame(Color.RED, scValue);
        } else {
            assertEquals(Color.RED, scValue);
        }
    }

    public void testColorRed03() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "ReD");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("ReD", cssValue.toString());
        if (isHarmony()) {
            assertSame(Color.RED, scValue);
        } else {
            assertEquals(Color.RED, scValue);
        }
    }

    public void testColorRed04() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rEd");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rEd", cssValue.toString());
        if (isHarmony()) {
            assertSame(Color.RED, scValue);
        } else {
            assertEquals(Color.RED, scValue);
        }
    }

    public void testColorRed05() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "RED");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("RED", cssValue.toString());
        if (isHarmony()) {
            assertSame(Color.RED, scValue);
        } else {
            assertEquals(Color.RED, scValue);
        }
    }

    public void testColorStringUnknown() throws Exception {
        if (!isHarmony()) {
            testExceptionalCase(new NullPointerCase() {
                public void exceptionalAction() throws Exception {
                    ss.addCSSAttribute(simple, Attribute.COLOR, "unknown");
                }
            });
            return;
        }
        assertEquals(0, simple.getAttributeCount());
        ss.addCSSAttribute(simple, Attribute.COLOR, "unknown");
        // No attribute was added
        assertEquals(0, simple.getAttributeCount());
    }

    public void testColorRGB255_0_0() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(255, 0, 0)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertSame(Color.class, scValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertNotSame(Color.class, cssValue.getClass());
        assertEquals("rgb(255, 0, 0)", cssValue.toString());
        assertEquals(Color.RED, scValue);
    }

    public void testColorRGB255_0_0NoSpace() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(255,0,0)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(255,0,0)", cssValue.toString());
        assertEquals(Color.RED, scValue);
    }

    public void testColorRGB255_0_0MoreSpace() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR,
                           "rgb(  255   ,  0 ,   0  )");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(  255   ,  0 ,   0  )", cssValue.toString());
        assertEquals(Color.RED, scValue);
    }

    public void testColorRGB0_300_0() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(0, 300, 0)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(0, 300, 0)", cssValue.toString());
        assertEquals(Color.GREEN, scValue);
    }

    public void testColorRGBMinus25_127_0() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(-25, 127, 0)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(-25, 127, 0)", cssValue.toString());
        assertEquals(new Color(0, 127, 0), scValue);
    }

    public void testColorRGBIntPercent0_0_100() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(0%, 0%, 100%)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(0%, 0%, 100%)", cssValue.toString());
        assertEquals(Color.BLUE, scValue);
    }

    public void testColorRGBIntPercent0_50_100() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(0%, 50%, 100%)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(0%, 50%, 100%)", cssValue.toString());
        assertEquals(new Color(0, 255 / 2, 255), scValue);
    }

    public void testColorRGBIntPercent0_50_200() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(0%, 50%, 200%)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(0%, 50%, 200%)", cssValue.toString());
        assertEquals(new Color(0, 255 / 2, 255), scValue);
    }

    public void testColorRGBIntPercentMinus25_50_75() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(-25%, 50%, 75%)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(-25%, 50%, 75%)", cssValue.toString());
        assertEquals(new Color(0, 255 / 2, 255 * 3 / 4), scValue);
    }

    public void testColorRGBFloatPercent1p5_0_100() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(1.5%, 0%, 100.00%)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(1.5%, 0%, 100.00%)", cssValue.toString());
        assertEquals((int)(255 * 0.01) + 1, (int)(255 * 0.015));
        assertEquals(new Color((int)(255 * 0.015), 0, 255), scValue);
    }

    public void testColorRGBFloatPercent1p2_0_100() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(1.1%, 0%, 100.00%)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(1.1%, 0%, 100.00%)", cssValue.toString());
        assertEquals((int)(255 * 0.01), (int)(255 * 0.011));
        assertEquals(new Color((int)(255 * 0.011), 0, 255), scValue);
    }

    public void testColorRGBFloatPercent0_50p5_100() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(0.000%, 50.5%, 100%)");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(0.000%, 50.5%, 100%)", cssValue.toString());
        assertEquals((int)(255 * 0.5) + 1, (int)(255 * 0.505));
        assertEquals(new Color(0, (int)(255 * 0.505), 255), scValue);
    }

    public void testColorRGBNumberMissed() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(127, , 255)");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(127, , 255)", cssValue.toString());
        assertEquals(new Color(127, 255, 0), scValue);
    }

    public void testColorRGBCommaMissed() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(127, 0 255)");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(127, 0 255)", cssValue.toString());
        assertEquals(new Color(127, 0, 255), scValue);
    }

    public void testColorRGBAbsolutePercentangeMixed() throws Exception {
        ss.addCSSAttribute(simple, Attribute.COLOR, "rgb(50%, 0, 255)");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.COLOR);
        scValue = attr.getAttribute(StyleConstants.Foreground);
        assertEquals("rgb(50%, 0, 255)", cssValue.toString());
        assertEquals(new Color(127, 0, 255), scValue);
    }



    public void testBackgroundColorTransparent() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND_COLOR, "transparent");
        if (!isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.BACKGROUND_COLOR);
        scValue = attr.getAttribute(StyleConstants.Background);
        assertEquals("transparent", cssValue.toString());
        assertNull(scValue);
    }
}
