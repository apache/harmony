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

public class StyleSheet_ConvertAttr_TextDecorationTest extends BasicSwingTestCase {
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

    public void testUnderline() {
        attr = ss.addAttribute(empty, StyleConstants.Underline,
                               Boolean.TRUE);

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.TEXT_DECORATION, name);
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        scValue = attr.getAttribute(StyleConstants.Underline);
        assertSame(Boolean.class, scValue.getClass());
        assertNotSame(Boolean.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("underline", cssValue.toString());
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testUnderlineFalse() {
        attr = ss.addAttribute(empty, StyleConstants.Underline,
                               Boolean.FALSE);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        scValue = attr.getAttribute(StyleConstants.Underline);
        assertEquals(isHarmony() ? "none" : "", cssValue.toString());
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testStrikeThrough() {
        attr = ss.addAttribute(empty, StyleConstants.StrikeThrough,
                               Boolean.TRUE);

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.TEXT_DECORATION, name);
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertSame(Boolean.class, scValue.getClass());
        assertNotSame(Boolean.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("line-through", cssValue.toString());
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testStrikeThroughFalse() {
        attr = ss.addAttribute(empty, StyleConstants.StrikeThrough,
                               Boolean.FALSE);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertEquals(isHarmony() ? "none" : "", cssValue.toString());
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testTextDecorationNone() throws Exception {
        ss.addCSSAttribute(simple, Attribute.TEXT_DECORATION, "none");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("none", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.Underline);
        assertFalse(((Boolean)scValue).booleanValue());

        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testTextDecorationUnderline() throws Exception {
        ss.addCSSAttribute(simple, Attribute.TEXT_DECORATION, "underline");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("underline", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.Underline);
        assertTrue(((Boolean)scValue).booleanValue());

        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testTextDecorationLineThrough() throws Exception {
        ss.addCSSAttribute(simple, Attribute.TEXT_DECORATION, "line-through");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("line-through", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.Underline);
        assertFalse(((Boolean)scValue).booleanValue());

        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testTextDecorationUnderlineLineThrough() throws Exception {
        ss.addCSSAttribute(simple, Attribute.TEXT_DECORATION,
                           "underline line-through");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("underline line-through", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.Underline);
        assertTrue(((Boolean)scValue).booleanValue());

        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testTextDecorationUnderlineThenLineThrough() throws Exception {
        ss.addCSSAttribute(simple, Attribute.TEXT_DECORATION, "underline");
        assertEquals(1, simple.getAttributeCount());
        ss.addCSSAttribute(simple, Attribute.TEXT_DECORATION, "line-through");
        assertEquals(1, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("line-through", cssValue.toString());
    }

    public void testTextDecorationUnderlineLineThroughSC() throws Exception {
        attr = ss.addAttribute(empty, StyleConstants.Underline, Boolean.TRUE);
        assertEquals(1, attr.getAttributeCount());
        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("underline", cssValue.toString());
        scValue = attr.getAttribute(StyleConstants.Underline);
        assertTrue(((Boolean)scValue).booleanValue());
        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertFalse(((Boolean)scValue).booleanValue());

        attr = ss.addAttribute(attr, StyleConstants.StrikeThrough,
                               Boolean.TRUE);
        assertEquals(1, attr.getAttributeCount());
        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        if (isHarmony()) {
            assertEquals("underline line-through", cssValue.toString());
            scValue = attr.getAttribute(StyleConstants.Underline);
            assertTrue(((Boolean)scValue).booleanValue());
            scValue = attr.getAttribute(StyleConstants.StrikeThrough);
            assertTrue(((Boolean)scValue).booleanValue());
        } else {
            assertEquals("line-through", cssValue.toString());
            scValue = attr.getAttribute(StyleConstants.Underline);
            assertFalse(((Boolean)scValue).booleanValue());
            scValue = attr.getAttribute(StyleConstants.StrikeThrough);
            assertTrue(((Boolean)scValue).booleanValue());
        }
    }

    public void testTextDecorationStrikeThrough() throws Exception {
        ss.addCSSAttribute(simple, Attribute.TEXT_DECORATION, "strike-through");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("strike-through", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.Underline);
        assertFalse(((Boolean)scValue).booleanValue());

        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testTextDecorationOverline() throws Exception {
        ss.addCSSAttribute(simple, Attribute.TEXT_DECORATION, "overline");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("overline", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.Underline);
        assertFalse(((Boolean)scValue).booleanValue());

        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testTextDecorationBlink() throws Exception {
        ss.addCSSAttribute(simple, Attribute.TEXT_DECORATION, "blink");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("blink", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.Underline);
        assertFalse(((Boolean)scValue).booleanValue());

        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testAddAttributesUnderline() throws Exception {
        simple.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        attr = ss.addAttributes(empty, simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("underline", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.Underline);
        assertTrue(((Boolean)scValue).booleanValue());

        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertFalse(((Boolean)scValue).booleanValue());
    }

    public void testAddAttributesStrikeThrough() throws Exception {
        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        attr = ss.addAttributes(empty, simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("line-through", cssValue.toString());

        scValue = attr.getAttribute(StyleConstants.Underline);
        assertFalse(((Boolean)scValue).booleanValue());

        scValue = attr.getAttribute(StyleConstants.StrikeThrough);
        assertTrue(((Boolean)scValue).booleanValue());
    }

    public void testAddAttributesUnderlineStrikeThrough() throws Exception {
        attr = ss.addAttribute(empty, StyleConstants.Underline, Boolean.TRUE);
        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        attr = ss.addAttributes(attr, simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        if (isHarmony()) {
            assertEquals("underline line-through", cssValue.toString());
            scValue = attr.getAttribute(StyleConstants.Underline);
            assertTrue(((Boolean)scValue).booleanValue());
            scValue = attr.getAttribute(StyleConstants.StrikeThrough);
            assertTrue(((Boolean)scValue).booleanValue());
        } else {
            assertTrue("underline".equals(cssValue.toString())
                       ^ "line-through".equals(cssValue.toString()));
        }
    }

    public void testAddAttributesStrikeThroughUnderline() throws Exception {
        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        simple.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        assertEquals(2, simple.getAttributeCount());

        attr = ss.addAttributes(empty, simple);
        assertEquals(1, attr.getAttributeCount());

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        if (isHarmony()) {
            assertEquals("underline line-through", cssValue.toString());
            scValue = attr.getAttribute(StyleConstants.Underline);
            assertTrue(((Boolean)scValue).booleanValue());
            scValue = attr.getAttribute(StyleConstants.StrikeThrough);
            assertTrue(((Boolean)scValue).booleanValue());
        } else {
            assertTrue("underline".equals(cssValue.toString())
                       ^ "line-through".equals(cssValue.toString()));
        }
    }

    public void testIsDefinedUnderline() throws Exception {
        attr = ss.addAttribute(empty, StyleConstants.Underline, Boolean.TRUE);

        assertTrue(attr.isDefined(Attribute.TEXT_DECORATION));
        assertTrue(attr.isDefined(StyleConstants.Underline));
        assertTrue(attr.isDefined(StyleConstants.StrikeThrough));
    }

    public void testIsDefinedStrikeThrough() throws Exception {
        attr = ss.addAttribute(empty, StyleConstants.StrikeThrough,
                               Boolean.TRUE);

        assertTrue(attr.isDefined(Attribute.TEXT_DECORATION));
        assertTrue(attr.isDefined(StyleConstants.Underline));
        assertTrue(attr.isDefined(StyleConstants.StrikeThrough));
    }

    public void testContainsAttributeUnderline() throws Exception {
        attr = ss.addAttribute(empty, StyleConstants.Underline, Boolean.TRUE);

        assertTrue(attr.containsAttribute(StyleConstants.Underline,
                                          Boolean.TRUE));
        assertFalse(attr.containsAttribute(StyleConstants.Underline,
                                           Boolean.FALSE));
        assertFalse(attr.containsAttribute(StyleConstants.StrikeThrough,
                                           Boolean.TRUE));
        assertTrue(attr.containsAttribute(StyleConstants.StrikeThrough,
                                          Boolean.FALSE));
    }

    public void testContainsAttributeStrikeThrough() throws Exception {
        attr = ss.addAttribute(empty, StyleConstants.StrikeThrough,
                               Boolean.TRUE);

        assertFalse(attr.containsAttribute(StyleConstants.Underline,
                                           Boolean.TRUE));
        assertTrue(attr.containsAttribute(StyleConstants.Underline,
                                          Boolean.FALSE));
        assertTrue(attr.containsAttribute(StyleConstants.StrikeThrough,
                                          Boolean.TRUE));
        assertFalse(attr.containsAttribute(StyleConstants.StrikeThrough,
                                           Boolean.FALSE));
    }

    public void testContainsAttributeUnderlineStrikeThrough() throws Exception {
        if (!isHarmony()) {
            return;
        }

        attr = ss.addAttribute(empty, StyleConstants.Underline, Boolean.TRUE);
        attr = ss.addAttribute(attr, StyleConstants.StrikeThrough,
                               Boolean.TRUE);

        assertTrue(attr.containsAttribute(StyleConstants.Underline,
                                          Boolean.TRUE));
        assertFalse(attr.containsAttribute(StyleConstants.Underline,
                                           Boolean.FALSE));
        assertTrue(attr.containsAttribute(StyleConstants.StrikeThrough,
                                          Boolean.TRUE));
        assertFalse(attr.containsAttribute(StyleConstants.StrikeThrough,
                                           Boolean.FALSE));
    }

    public void testContainsAttributesUnderline() throws Exception {
        simple.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        attr = ss.addAttributes(empty, simple);
        assertTrue(attr.containsAttributes(simple));

        simple.removeAttribute(StyleConstants.Underline);
        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        assertFalse(attr.containsAttributes(simple));
    }

    public void testContainsAttributesUnderlineFalse() throws Exception {
        attr = ss.addAttribute(empty, StyleConstants.Underline, Boolean.TRUE);
        assertTrue(attr.containsAttributes(simple));

        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.FALSE);
        assertTrue(attr.containsAttributes(simple));
    }

    public void testContainsAttributesStrikeThrough() throws Exception {
        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        attr = ss.addAttributes(empty, simple);
        assertTrue(attr.containsAttributes(simple));

        simple.removeAttribute(StyleConstants.StrikeThrough);
        simple.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        assertFalse(attr.containsAttributes(simple));
    }

    public void testContainsAttributesStrikeThroughFalse() throws Exception {
        attr = ss.addAttribute(empty, StyleConstants.StrikeThrough,
                               Boolean.TRUE);

        simple.addAttribute(StyleConstants.Underline, Boolean.FALSE);
        assertTrue(attr.containsAttributes(simple));
    }

    public void testContainsAttributesUnderlineStrikeThrough()
        throws Exception {

        if (!isHarmony()) {
            return;
        }

        simple.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        attr = ss.addAttributes(empty, simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("underline line-through", cssValue.toString());

        assertTrue(attr.containsAttributes(simple));

        simple.removeAttribute(StyleConstants.StrikeThrough);
        assertTrue(attr.containsAttributes(simple));
    }

    public void testRemoveAttributeUnderline() throws Exception {
        attr = ss.addAttribute(empty, StyleConstants.Underline, Boolean.TRUE);
        assertEquals(1, attr.getAttributeCount());

        attr = ss.removeAttribute(attr, StyleConstants.StrikeThrough);
        if (isHarmony()) {
            assertEquals(1, attr.getAttributeCount());
        } else {
            assertEquals(0, attr.getAttributeCount());
            return;
        }

        attr = ss.removeAttribute(attr, StyleConstants.Underline);
        assertEquals(0, attr.getAttributeCount());
    }

    public void testRemoveAttributeStrikeThrough() throws Exception {
        attr = ss.addAttribute(empty, StyleConstants.StrikeThrough,
                               Boolean.TRUE);


        attr = ss.removeAttribute(attr, StyleConstants.Underline);
        if (isHarmony()) {
            assertEquals(1, attr.getAttributeCount());
        } else {
            assertEquals(0, attr.getAttributeCount());
            return;
        }

        attr = ss.removeAttribute(attr, StyleConstants.StrikeThrough);
        assertEquals(0, attr.getAttributeCount());
    }

    public void testRemoveAttributesUnderline() throws Exception {
        simple.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        attr = ss.addAttributes(empty, simple);
        assertTrue(attr.containsAttributes(simple));

        attr = ss.removeAttributes(attr, simple);
        if (isHarmony()) {
            assertEquals(0, attr.getAttributeCount());
        } else {
            assertEquals(1, attr.getAttributeCount());
            cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
            assertEquals("underline", cssValue.toString());
        }
    }

    public void testRemoveAttributesStrikeThrough() throws Exception {
        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        attr = ss.addAttributes(empty, simple);
        assertTrue(attr.containsAttributes(simple));

        attr = ss.removeAttributes(attr, simple);
        if (isHarmony()) {
            assertEquals(0, attr.getAttributeCount());
        } else {
            assertEquals(1, attr.getAttributeCount());
            cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
            assertEquals("line-through", cssValue.toString());
        }
    }

    public void testRemoveAttributesUnderlineStrikeThrough01()
        throws Exception {

        if (!isHarmony()) {
            return;
        }

        simple.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        attr = ss.addAttributes(empty, simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("underline line-through", cssValue.toString());

        assertTrue(attr.containsAttributes(simple));

        attr = ss.removeAttributes(attr, simple);
        assertEquals(0, attr.getAttributeCount());
    }

    public void testRemoveAttributesUnderlineStrikeThrough02()
        throws Exception {

        if (!isHarmony()) {
            return;
        }

        simple.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        attr = ss.addAttributes(empty, simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("underline line-through", cssValue.toString());

        simple.removeAttribute(StyleConstants.Underline);
        attr = ss.removeAttributes(attr, simple);
        assertEquals(1, attr.getAttributeCount());
        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("underline", cssValue.toString());
    }

    public void testRemoveAttributesUnderlineStrikeThrough03()
        throws Exception {

        if (!isHarmony()) {
            return;
        }

        simple.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        simple.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        attr = ss.addAttributes(empty, simple);

        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("underline line-through", cssValue.toString());

        simple.removeAttribute(StyleConstants.StrikeThrough);
        attr = ss.removeAttributes(attr, simple);
        assertEquals(1, attr.getAttributeCount());
        cssValue = attr.getAttribute(Attribute.TEXT_DECORATION);
        assertEquals("line-through", cssValue.toString());
    }
}
