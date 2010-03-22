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

public class StyleSheet_ConvertAttr_VerticalAlignTest extends TestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;
    private Object cssValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testVerticalAlignBaseline() {
        ss.addCSSAttribute(simple, Attribute.VERTICAL_ALIGN, "baseline");
        cssValue = simple.getAttribute(Attribute.VERTICAL_ALIGN);
        assertEquals("baseline", cssValue.toString());
    }

    public void testVerticalAlignMiddle() {
        ss.addCSSAttribute(simple, Attribute.VERTICAL_ALIGN, "middle");
        cssValue = simple.getAttribute(Attribute.VERTICAL_ALIGN);
        assertEquals("middle", cssValue.toString());
    }

    public void testVerticalAlignSub() {
        ss.addCSSAttribute(simple, Attribute.VERTICAL_ALIGN, "sub");
        cssValue = simple.getAttribute(Attribute.VERTICAL_ALIGN);
        assertEquals("sub", cssValue.toString());
    }

    public void testVerticalAlignSuper() {
        ss.addCSSAttribute(simple, Attribute.VERTICAL_ALIGN, "super");
        cssValue = simple.getAttribute(Attribute.VERTICAL_ALIGN);
        assertEquals("super", cssValue.toString());
    }

    public void testVerticalAlignTextTop() {
        ss.addCSSAttribute(simple, Attribute.VERTICAL_ALIGN, "text-top");
        cssValue = simple.getAttribute(Attribute.VERTICAL_ALIGN);
        assertEquals("text-top", cssValue.toString());
    }

    public void testVerticalAlignTextBottom() {
        ss.addCSSAttribute(simple, Attribute.VERTICAL_ALIGN, "text-bottom");
        cssValue = simple.getAttribute(Attribute.VERTICAL_ALIGN);
        assertEquals("text-bottom", cssValue.toString());
    }

    public void testVerticalAlignTop() {
        ss.addCSSAttribute(simple, Attribute.VERTICAL_ALIGN, "top");
        cssValue = simple.getAttribute(Attribute.VERTICAL_ALIGN);
        assertEquals("top", cssValue.toString());
    }

    public void testVerticalAlignBottom() {
        ss.addCSSAttribute(simple, Attribute.VERTICAL_ALIGN, "bottom");
        cssValue = simple.getAttribute(Attribute.VERTICAL_ALIGN);
        assertEquals("bottom", cssValue.toString());
    }


    public void testVerticalAlignInvalid() {
        ss.addCSSAttribute(simple, Attribute.VERTICAL_ALIGN, "botom");
        cssValue = simple.getAttribute(Attribute.VERTICAL_ALIGN);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        } else {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("botom", cssValue.toString());
        }
    }
}
