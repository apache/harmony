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

import junit.framework.TestCase;

public class StyleSheet_ConvertAttr_ListStyleTypeTest extends TestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;
    private Object cssValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testListStyleTypeDisc() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE_TYPE, "disc");
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("disc", cssValue.toString());
    }

    public void testListStyleTypeCircle() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE_TYPE, "circle");
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("circle", cssValue.toString());
    }

    public void testListStyleTypeSquare() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE_TYPE, "square");
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("square", cssValue.toString());
    }

    public void testListStyleTypeDecimal() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE_TYPE, "decimal");
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("decimal", cssValue.toString());
    }

    public void testListStyleTypeLowerRoman() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE_TYPE, "lower-roman");
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("lower-roman", cssValue.toString());
    }

    public void testListStyleTypeUpperRoman() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE_TYPE, "upper-roman");
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("upper-roman", cssValue.toString());
    }

    public void testListStyleTypeLowerAlpha() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE_TYPE, "lower-alpha");
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("lower-alpha", cssValue.toString());
    }

    public void testListStyleTypeUpperAlpha() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE_TYPE, "upper-alpha");
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("upper-alpha", cssValue.toString());
    }

    public void testListStyleTypeNone() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE_TYPE, "none");
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyleTypeDisk() throws Exception {
        // Invalid value
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE_TYPE, "disk");
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        } else {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("disc", cssValue.toString()); // default value
        }
    }
}
