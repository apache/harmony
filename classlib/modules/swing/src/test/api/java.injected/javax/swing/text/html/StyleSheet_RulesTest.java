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
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.html.CSS.Attribute;

public class StyleSheet_RulesTest extends BasicSwingTestCase {
    private StyleSheet ss;
    private Enumeration rules;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
    }

    /**
     * Several rules (selector - property list pairs) in one string.
     */
    public void testAddRule01() throws Exception {
        ss.addRule("p { text-align: right; }\nem {color: red}");
        Style pStyle = null;
        Style emStyle = null;
        Style defStyle = null;
        rules = ss.getStyleNames();
        while (rules.hasMoreElements()) {
            String name = (String)rules.nextElement();
            Style style = ss.getStyle(name);
            if ("p".equals(name)) {
                pStyle = style;
            } else if ("em".equals(name)) {
                emStyle = style;
            } else if (StyleContext.DEFAULT_STYLE.equals(name)) {
                defStyle = style;
            } else {
                fail("Unexpected style: " + style);
            }
        }

        assertEquals(2, pStyle.getAttributeCount());
        assertEquals("right",
                     pStyle.getAttribute(Attribute.TEXT_ALIGN).toString());
        assertNotNull(pStyle.getAttribute(AttributeSet.NameAttribute));
        assertEquals(StyleConstants.ALIGN_RIGHT,
                     ((Integer)pStyle.getAttribute(StyleConstants.Alignment))
                     .intValue());

        assertEquals(2, emStyle.getAttributeCount());
        assertEquals("red",
                     emStyle.getAttribute(Attribute.COLOR).toString());
        assertNotNull(emStyle.getAttribute(AttributeSet.NameAttribute));
        assertEquals(Color.RED,
                     emStyle.getAttribute(StyleConstants.Foreground));

        assertEquals(1, defStyle.getAttributeCount());
        assertNotNull(defStyle.getAttribute(AttributeSet.NameAttribute));
    }

    /**
     * Several properties defined for one selector.
     */
    public void testAddRule02() throws Exception {
        ss.addRule("p { text-align: justify; background-color: black; " +
                   "color: white }");
        Style pStyle = null;
        rules = ss.getStyleNames();
        while (rules.hasMoreElements()) {
            String name = (String)rules.nextElement();
            Style style = ss.getStyle(name);
            if ("p".equals(name)) {
                pStyle = style;
            } else if (!StyleContext.DEFAULT_STYLE.equals(name)) {
                fail("Unexpected style: " + style);
            }
        }

        assertEquals(4, pStyle.getAttributeCount());
        assertEquals("justify",
                     pStyle.getAttribute(Attribute.TEXT_ALIGN).toString());
        assertEquals(StyleConstants.ALIGN_JUSTIFIED,
                     ((Integer)pStyle.getAttribute(StyleConstants.Alignment))
                     .intValue());

        assertEquals("white",
                     pStyle.getAttribute(Attribute.COLOR).toString());
        assertEquals(Color.WHITE,
                     pStyle.getAttribute(StyleConstants.Foreground));

        assertEquals("black",
                     pStyle.getAttribute(Attribute.BACKGROUND_COLOR).toString());
        assertEquals(Color.BLACK,
                     pStyle.getAttribute(StyleConstants.Background));
        assertNotNull(pStyle.getAttribute(AttributeSet.NameAttribute));
    }

    /**
     * Tests that <code>addRule</code> uses <code>addCSSAttribute</code>
     * to fill the attribute set.
     */
    public void testAddRule03() throws Exception {
        final Marker marker = new Marker();
        ss = new StyleSheet() {
            public void addCSSAttribute(final MutableAttributeSet attr,
                                        final Attribute key,
                                        final String value) {
                marker.setOccurred();
//                assertFalse(attr instanceof Style);
                assertSame(Attribute.FONT_SIZE, key);
                assertEquals("21pt", value);
                super.addCSSAttribute(attr, key, value);
            }
        };
        ss.addRule("   p    {  font-size   :   21pt   }    ");

        assertTrue(marker.isOccurred());
    }

    /**
     * Tests that <code>addRule</code> calls <code>addStyle</code> to actually
     * add the rule.
     */
    public void testAddRule04() throws Exception {
        final Marker marker = new Marker();
        ss = new StyleSheet() {
            public Style addStyle(String name, Style parent) {
                marker.setOccurred();
                if (marker.getAuxiliary() != null) {
                    assertEquals("p", name);
                    assertNull(parent);
                } else {
                    assertEquals("default", name);
                    marker.setAuxiliary(name);
                }
                return super.addStyle(name, parent);
            }
        };
        assertTrue(marker.isOccurred());
        marker.setOccurred(false);

        ss.addRule("   p    {  font-size   :   21pt   }    ");
        assertTrue(marker.isOccurred());
    }

    /**
     * Tests that <code>addRule</code> stores rule as Style.
     */
    public void testAddRule05() throws Exception {
        ss.addRule("em { text-decoration: underline }");
        Style emStyle = ss.getStyle("em");

        assertEquals(2, emStyle.getAttributeCount());
        assertEquals("underline",
                     emStyle.getAttribute(CSS.Attribute.TEXT_DECORATION)
                     .toString());
        assertEquals("em", emStyle.getAttribute(AttributeSet.NameAttribute));
    }

    /**
     * Adding Style IS/IS NOT equivalent to adding a rule.
     */
    public void testAddRule06() throws Exception {
        Style emStyle = ss.getStyle("em");
        assertNull(emStyle);

        emStyle = ss.addStyle("em", null);
        ss.addCSSAttribute(emStyle, CSS.Attribute.TEXT_DECORATION, "underline");

        Style emRule = ss.getRule("em");
        assertNotNull(emRule);
        assertEquals(isHarmony() ? 2 : 0, emRule.getAttributeCount());
    }

    /**
     * Adding a property with several selectors.
     */
    public void testAddRule07() throws Exception {
        assertNull(ss.getStyle("ol"));
        assertNull(ss.getStyle("ul"));

        ss.addRule("ol, ul { margin-left: 3pt }");

        Style ol = ss.getStyle("ol");
        assertNotNull(ol.getAttribute(Attribute.MARGIN_LEFT));

        Style ul = ss.getStyle("ul");
        assertNotNull(ul.getAttribute(Attribute.MARGIN_LEFT));
    }

    /**
     * Tags are not case-sensitive in HTML.
     */
    public void testAddRule08() throws Exception {
        assertNull(ss.getStyle("h1"));
        assertNull(ss.getStyle("H1"));
        ss.addRule("H1 { color: blue }");
        assertNotNull(ss.getStyle("h1"));
        assertNull(ss.getStyle("H1"));


        Style h1 = ss.getRule("h1");
        assertEquals(2, h1.getAttributeCount());
        assertEquals("blue", h1.getAttribute(CSS.Attribute.COLOR).toString());

        Style H1 = ss.getRule("H1");
        if (isHarmony()) {
            assertEquals(2, H1.getAttributeCount());
            assertEquals("blue", H1.getAttribute(CSS.Attribute.COLOR).toString());
            assertSame(h1, H1);
        } else {
            assertEquals(0, H1.getAttributeCount());
            assertNull(H1.getAttribute(CSS.Attribute.COLOR));
            assertNotSame(h1, H1);
        }
    }

    /**
     * Classes are case-sensitive.
     */
    public void testAddRule09() throws Exception {
        assertNull(ss.getStyle(".header"));
        assertNull(ss.getStyle(".HEADER"));
        ss.addRule(".HEADER { color: blue }");
        Style headerStyle = ss.getStyle(".header");
        Style HEADERstyle = ss.getStyle(".HEADER");
        Style headerRule = ss.getRule(".header");
        Style HEADERrule = ss.getRule(".HEADER");
        if (isHarmony()) {
            assertNull(headerStyle);
            assertNotNull(HEADERstyle);

            assertEquals(0, headerRule.getAttributeCount());
            assertEquals(2, HEADERrule.getAttributeCount());
        } else {
            assertNotNull(headerStyle);
            assertNull(HEADERstyle);

            assertEquals(2, headerRule.getAttributeCount());
            assertEquals(0, HEADERrule.getAttributeCount());
        }
        assertNotSame(headerRule, HEADERrule);
    }

    /**
     * ids are case-sensitive.
     */
    public void testAddRule10() throws Exception {
        assertNull(ss.getStyle(".id"));
        assertNull(ss.getStyle(".ID"));
        ss.addRule("#ID { color: blue }");
        Style idStyle = ss.getStyle("#id");
        Style IDstyle = ss.getStyle("#ID");
        Style idRule = ss.getRule("#id");
        Style IDrule = ss.getRule("#ID");
        if (isHarmony()) {
            assertNull(idStyle);
            assertNotNull(IDstyle);

            assertEquals(0, idRule.getAttributeCount());
            assertEquals(2, IDrule.getAttributeCount());
        } else {
            assertNotNull(idStyle);
            assertNull(IDstyle);

            assertEquals(2, idRule.getAttributeCount());
            assertEquals(0, IDrule.getAttributeCount());
        }
        assertNotSame(idRule, IDrule);
    }

    /**
     * Parses one property using <code>addCSSAttribute</code> to add
     * the attribute to the set.
     */
    public void testGetDeclaration01() throws Exception {
        final Marker marker = new Marker();
        ss = new StyleSheet() {
            public void addCSSAttribute(final MutableAttributeSet attr,
                                        final Attribute key,
                                        final String value) {
                marker.setOccurred();
                assertSame(Attribute.FONT_SIZE, key);
                assertEquals("13pt", value);
                super.addCSSAttribute(attr, key, value);
            }
        };
        AttributeSet attr = ss.getDeclaration("font-size: 13pt");
        assertTrue(marker.isOccurred());
        assertEquals("13pt", attr.getAttribute(Attribute.FONT_SIZE).toString());
//        assertEquals(13, ((Integer)attr.getAttribute(StyleConstants.FontSize))
//                         .intValue());
        assertNull(attr.getAttribute(StyleConstants.FontSize));

        assertSame(SimpleAttributeSet.class, attr.getClass());
    }

    /**
     * Parses several properties at once.
     */
    public void testGetDeclaration02() throws Exception {
        AttributeSet attr = ss.getDeclaration("font-family: monospace; " +
                                              "color: rgb(11, 33, 99); " +
                                              "text-align: center");
        assertEquals(3, attr.getAttributeCount());
        assertEquals("monospace",
                     attr.getAttribute(Attribute.FONT_FAMILY).toString());
        assertEquals("rgb(11, 33, 99)",
                     attr.getAttribute(Attribute.COLOR).toString());
        assertEquals("center",
                     attr.getAttribute(Attribute.TEXT_ALIGN).toString());
    }
}
