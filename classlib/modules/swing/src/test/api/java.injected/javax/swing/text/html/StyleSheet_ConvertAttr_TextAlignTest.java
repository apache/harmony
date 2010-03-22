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

public class StyleSheet_ConvertAttr_TextAlignTest extends BasicSwingTestCase {
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

    public void testTextAlign() {
        attr = ss.addAttribute(empty, StyleConstants.Alignment,
                               new Integer(StyleConstants.ALIGN_RIGHT));

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.TEXT_ALIGN, name);
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertSame(Integer.class, scValue.getClass());
        assertNotSame(Integer.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
    }

    public void testTextAlignInvalid() {
        attr = ss.addAttribute(empty, StyleConstants.Alignment,
                               new Integer(100));

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(Attribute.TEXT_ALIGN, name);
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertSame(Integer.class, scValue.getClass());
        assertNotSame(Integer.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("left", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_LEFT, ((Integer)scValue).intValue());
    }

    public void testTextAlignLeft() {
        attr = ss.addAttribute(empty, StyleConstants.Alignment,
                               new Integer(StyleConstants.ALIGN_LEFT));

        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertEquals("left", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_LEFT, ((Integer)scValue).intValue());
    }

    public void testTextAlignCenter() {
        attr = ss.addAttribute(empty, StyleConstants.Alignment,
                               new Integer(StyleConstants.ALIGN_CENTER));

        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertEquals("center", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_CENTER,
                     ((Integer)scValue).intValue());
    }

    public void testTextAlignRight() {
        attr = ss.addAttribute(empty, StyleConstants.Alignment,
                               new Integer(StyleConstants.ALIGN_RIGHT));

        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertEquals("right", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_RIGHT, ((Integer)scValue).intValue());
    }

    public void testTextAlignJustify() {
        attr = ss.addAttribute(empty, StyleConstants.Alignment,
                               new Integer(StyleConstants.ALIGN_JUSTIFIED));

        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertEquals("justify", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_JUSTIFIED,
                     ((Integer)scValue).intValue());
    }



    public void testTextAlignStringLeft() {
        ss.addCSSAttribute(simple, Attribute.TEXT_ALIGN, "left");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertEquals("left", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_LEFT, ((Integer)scValue).intValue());
    }

    public void testTextAlignStringCenter() {
        ss.addCSSAttribute(simple, Attribute.TEXT_ALIGN, "center");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertEquals("center", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_CENTER,
                     ((Integer)scValue).intValue());
    }

    public void testTextAlignStringRight() {
        ss.addCSSAttribute(simple, Attribute.TEXT_ALIGN, "right");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertEquals("right", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_RIGHT, ((Integer)scValue).intValue());
    }

    public void testTextAlignStringJustify() {
        ss.addCSSAttribute(simple, Attribute.TEXT_ALIGN, "justify");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertEquals("justify", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_JUSTIFIED,
                     ((Integer)scValue).intValue());
    }

    public void testTextAlignStringJustified() {
        assertEquals(0, simple.getAttributeCount());
        ss.addCSSAttribute(simple, Attribute.TEXT_ALIGN, "justified");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        attr = ss.createSmallAttributeSet(simple);
        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertEquals("justified", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_LEFT,
                     ((Integer)scValue).intValue());
    }

    public void testTextAlignStringInvalid() {
        assertEquals(0, simple.getAttributeCount());
        ss.addCSSAttribute(simple, Attribute.TEXT_ALIGN, "super-align");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        attr = ss.createSmallAttributeSet(simple);
        cssValue = attr.getAttribute(Attribute.TEXT_ALIGN);
        scValue = attr.getAttribute(StyleConstants.Alignment);
        assertEquals("super-align", cssValue.toString());
        assertEquals(StyleConstants.ALIGN_LEFT,
                     ((Integer)scValue).intValue());
    }
}
