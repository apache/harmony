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

public class StyleSheet_ConvertAttr_FontTest extends BasicSwingTestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;

    private String sansSerif;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();

        sansSerif = isHarmony() ? "sans-serif" : "SansSerif";
    }

    // Invalid values
    public void testFont01() {
        ss.addCSSAttribute(simple, Attribute.FONT, "normal");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("normal", "normal", "normal",
                         "medium", "normal", sansSerif);
    }

    public void testFont02() {
        ss.addCSSAttribute(simple, Attribute.FONT, "12pt");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("normal", "normal", "normal",
                         "12pt", "normal", sansSerif);
    }

    public void testFont03() {
        ss.addCSSAttribute(simple, Attribute.FONT, "monospace");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("normal", "normal", "normal",
                         /*size*/ "monospace", "normal", /*family*/ sansSerif);
    }


    // Valid values
    public void testFont05() {
        ss.addCSSAttribute(simple, Attribute.FONT, "small serif");
        assertAttributes("normal", "normal", "normal",
                         "small", "normal", "serif");
    }

    public void testFont06() {
        ss.addCSSAttribute(simple, Attribute.FONT, "italic small serif");
        assertAttributes("italic", "normal", "normal",
                         "small", "normal", "serif");
    }

    public void testFont07() {
        ss.addCSSAttribute(simple, Attribute.FONT, "italic bold small serif");
        assertAttributes("italic", "normal", "bold",
                         "small", "normal", "serif");
    }

    public void testFont08() {
        ss.addCSSAttribute(simple, Attribute.FONT, "bold italic small serif");
        assertAttributes("italic", "normal", "bold",
                         "small", "normal", "serif");
    }

    public void testFont09() {
        ss.addCSSAttribute(simple, Attribute.FONT,
                           "bold small-caps italic small serif");
        assertAttributes("italic", "small-caps", "bold",
                         "small", "normal", "serif");
    }

    public void testFont10() {
        ss.addCSSAttribute(simple, Attribute.FONT,
                           "bold normal small-caps italic small serif");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }
        assertAttributes("normal", "small-caps", "bold",
                         /*size*/ "italic", "normal", "small serif");
    }

    public void testFont11() {
        ss.addCSSAttribute(simple, Attribute.FONT,
                           "bold small-caps italic normal small serif");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }
        assertAttributes("italic", "small-caps", "bold",
                         /*size*/ "normal", "normal", "small serif");
    }

    public void testFont12() {
        ss.addCSSAttribute(simple, Attribute.FONT,
                           "bold italic large 'Times New Roman', Garamond, serif");
        assertAttributes("italic", "normal", "bold",
                         "large", "normal",
                         "'Times New Roman', Garamond, serif");
    }

    public void testFont13() {
        ss.addCSSAttribute(simple, Attribute.FONT,
                           "larger/1.2 Arial, Verdana, sans-serif");
        assertAttributes("normal", "normal", "normal",
                         "larger", "1.2", "Arial, Verdana, sans-serif");
    }

    public void testFont14() {
        ss.addCSSAttribute(simple, Attribute.FONT,
                           "100% / 110% \"Courier New\", \"Lucida Console\", "
                           + "monospace");
        assertAttributes("normal", "normal", "normal",
                         "100%", "110%",
                         "\"Courier New\", \"Lucida Console\", monospace");
    }

    public void testFont15() {
        ss.addCSSAttribute(simple, Attribute.FONT, "smaller /120% fantasy");
        assertAttributes("normal", "normal", "normal",
                         "smaller", "120%", "fantasy");
    }

    public void testFont16() {
        ss.addCSSAttribute(simple, Attribute.FONT, "small/ 120% cursive");
        assertAttributes("normal", "normal", "normal",
                         "small", "120%", "cursive");
    }

    public void testFont17() {
        ss.addCSSAttribute(simple, Attribute.FONT, "x-small/ /18pt sans-serif");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("normal", "normal", "normal",
                         "x-small", "/18pt", "sans-serif");
    }

    public void testFont18() {
        ss.addCSSAttribute(simple, Attribute.FONT, "14/ 18pt sans-serif");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("normal", "normal", "normal",
                         "14", "18pt", "sans-serif");
    }

    public void testFont19() {
        ss.addCSSAttribute(simple, Attribute.FONT, "14pt/");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("normal", "normal", "normal",
                         "14pt", "normal", "SansSerif");
    }

    public void testFont20() {
        ss.addCSSAttribute(simple, Attribute.FONT, "14pt /");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("normal", "normal", "normal",
                         "14pt", "normal", "SansSerif");
    }

    private void assertAttributes(final String style,
                                  final String variant,
                                  final String weight,
                                  final String size,
                                  final String lineHeight,
                                  final String family) {
        assertEquals("Attribute count", 6, simple.getAttributeCount());

        assertEquals("font-style",
                     style, getCSSAttribute(Attribute.FONT_STYLE));
        assertEquals("font-variant",
                     variant, getCSSAttribute(Attribute.FONT_VARIANT));
        assertEquals("font-weight",
                     weight, getCSSAttribute(Attribute.FONT_WEIGHT));
        assertEquals("font-size",
                     size, getCSSAttribute(Attribute.FONT_SIZE));
        assertEquals("line-height",
                     lineHeight, getCSSAttribute(Attribute.LINE_HEIGHT));
        assertEquals("font-family",
                     family, getCSSAttribute(Attribute.FONT_FAMILY));
    }

    private String getCSSAttribute(final Attribute cssKey) {
        final Object result = simple.getAttribute(cssKey);
        return result != null ? result.toString() : null;
    }
}
