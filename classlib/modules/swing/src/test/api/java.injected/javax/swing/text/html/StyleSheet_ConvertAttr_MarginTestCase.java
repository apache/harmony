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
import javax.swing.text.html.CSS.Attribute;

public abstract class StyleSheet_ConvertAttr_MarginTestCase
    extends BasicSwingTestCase {

    protected StyleSheet ss;

    protected AttributeSet empty;
    protected AttributeSet attr;
    protected MutableAttributeSet simple;

    protected Attribute cssKey;
    protected Object cssValue;
    protected Object scKey;
    protected Object scValue;

    protected String defUnits;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        empty = ss.getEmptySet();
        simple = new SimpleAttributeSet();
        defUnits = isHarmony() ? "pt" : "";
    }

    // Specifies size in points.
    public void testLength() {
        attr = ss.addAttribute(empty, scKey, new Float(11.1));

        Enumeration names = attr.getAttributeNames();
        Object name = names.nextElement();
        assertSame(cssKey, name);
        assertFalse(names.hasMoreElements());

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertSame(Float.class, scValue.getClass());
        assertNotSame(Float.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals(11.1f, ((Float)scValue).floatValue());
        assertEquals("11.1" + defUnits, cssValue.toString());
    }

    public void testLengthString() {
        if (!isHarmony()) {
            testExceptionalCase(new ClassCastCase() {
                public void exceptionalAction() throws Exception {
                    ss.addAttribute(empty, scKey, "11.1pt");
                }
            });
            return;
        }
        attr = ss.addAttribute(empty, scKey, "11.1pt");

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals(11.1f, ((Float)scValue).floatValue());
        assertEquals("11.1pt", cssValue.toString());
    }

    public void testLengthInteger() {
        testExceptionalCase(new ExceptionalCase() {
            public void exceptionalAction() throws Exception {
                ss.addAttribute(empty, scKey, new Integer(11));
            }

            public Class expectedExceptionClass() {
                return isHarmony() ? NullPointerException.class
                                   : ClassCastException.class;
            }
        });
    }

    public void testLength11_1() {
        ss.addCSSAttribute(simple, cssKey, "11.1");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals("11.1", cssValue.toString());
        assertEquals(11.1f, ((Float)scValue).floatValue());
    }

    public void testLength0() {
        ss.addCSSAttribute(simple, cssKey, "0");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals("0", cssValue.toString());
        assertEquals(0f, ((Float)scValue).floatValue());
    }

    public void testLength0px() {
        ss.addCSSAttribute(simple, cssKey, "0px");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals("0px", cssValue.toString());
        assertEquals(0f, ((Float)scValue).floatValue());
    }

    public void testLength11_1pt() {
        ss.addCSSAttribute(simple, cssKey, "11.1pt");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertSame(Float.class, scValue.getClass());
        assertNotSame(Float.class, cssValue.getClass());
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("11.1pt", cssValue.toString());
        assertEquals(11.1f, ((Float)scValue).floatValue());
    }

    public void testLength11_1px() {
        ss.addCSSAttribute(simple, cssKey, "11.1px");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals("11.1px", cssValue.toString());
        assertEquals(14.43f, ((Float)scValue).floatValue());
    }

    public void testLength11_1mm() {
        ss.addCSSAttribute(simple, cssKey, "11.1mm");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals("11.1mm", cssValue.toString());
        assertEquals(31.464506f, ((Float)scValue).floatValue(), 0.00007f);
    }

    public void testLength11_1cm() {
        ss.addCSSAttribute(simple, cssKey, "11.1cm");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals("11.1cm", cssValue.toString());
        assertEquals(314.64506f, ((Float)scValue).floatValue(), 0.0007f);
    }

    public void testLength11_1pc() {
        ss.addCSSAttribute(simple, cssKey, "11.1pc");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals("11.1pc", cssValue.toString());
        assertEquals(133.20001f, ((Float)scValue).floatValue());
    }

    public void testLength11_1in() {
        ss.addCSSAttribute(simple, cssKey, "11.1in");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals("11.1in", cssValue.toString());
        assertEquals(799.2f, ((Float)scValue).floatValue());
    }

    public void testLengthMinus11_1pt() {
        ss.addCSSAttribute(simple, cssKey, "-11.1pt");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals("-11.1pt", cssValue.toString());
        assertEquals(isHarmony() ? -11.1f : 0, ((Float)scValue).floatValue());
    }

    private static void assertEquals(final float expected, final float actual) {
        assertEquals(expected, actual, 0f);
    }
}
