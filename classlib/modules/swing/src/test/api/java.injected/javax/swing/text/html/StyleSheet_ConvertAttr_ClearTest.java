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

public class StyleSheet_ConvertAttr_ClearTest extends TestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;
    private Object cssValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testClearNone() {
        ss.addCSSAttribute(simple, Attribute.CLEAR, "none");
        cssValue = simple.getAttribute(Attribute.CLEAR);
        assertEquals("none", cssValue.toString());
    }

    public void testClearLeft() {
        ss.addCSSAttribute(simple, Attribute.CLEAR, "left");
        cssValue = simple.getAttribute(Attribute.CLEAR);
        assertEquals("left", cssValue.toString());
    }

    public void testClearRight() {
        ss.addCSSAttribute(simple, Attribute.CLEAR, "right");
        cssValue = simple.getAttribute(Attribute.CLEAR);
        assertEquals("right", cssValue.toString());
    }

    public void testClearBoth() {
        ss.addCSSAttribute(simple, Attribute.CLEAR, "both");
        cssValue = simple.getAttribute(Attribute.CLEAR);
        assertEquals("both", cssValue.toString());
    }

    public void testClearInvalid() {
        ss.addCSSAttribute(simple, Attribute.CLEAR, "top");
        cssValue = simple.getAttribute(Attribute.CLEAR);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        } else {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("top", cssValue.toString());
        }
    }
}
