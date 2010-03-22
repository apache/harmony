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

public abstract class StyleSheet_ConvertAttr_LengthTestCase extends TestCase {
    protected StyleSheet ss;
    protected MutableAttributeSet simple;
    protected Attribute cssKey;
    protected Object cssValue;
    protected boolean negativeValuesInvalid;
    protected boolean percentageValuesInvalid;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
        negativeValuesInvalid = false;
        percentageValuesInvalid = false;
    }

    public void testLength0_75em() {
        ss.addCSSAttribute(simple, cssKey, "0.75em");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("0.75em", cssValue.toString());
    }

    public void testLength1_25ex() {
        ss.addCSSAttribute(simple, cssKey, "1.25ex");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("1.25ex", cssValue.toString());
    }

    public void testLength0() {
        ss.addCSSAttribute(simple, cssKey, "0");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("0", cssValue.toString());
    }

    public void testLength0px() {
        ss.addCSSAttribute(simple, cssKey, "0px");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("0px", cssValue.toString());
    }

    public void testLength11_1() {
        ss.addCSSAttribute(simple, cssKey, "11.1");
        cssValue = simple.getAttribute(cssKey);
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("11.1", cssValue.toString());
            return;
        }
        assertEquals(0, simple.getAttributeCount());
        assertNull(cssValue);
    }

    public void testLength11_1pt() {
        ss.addCSSAttribute(simple, cssKey, "11.1pt");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("11.1pt", cssValue.toString());
    }

    public void testLength11_1px() {
        ss.addCSSAttribute(simple, cssKey, "11.1px");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("11.1px", cssValue.toString());
    }

    public void testLength11_1mm() {
        ss.addCSSAttribute(simple, cssKey, "11.1mm");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("11.1mm", cssValue.toString());
    }

    public void testLength11_1cm() {
        ss.addCSSAttribute(simple, cssKey, "11.1cm");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("11.1cm", cssValue.toString());
    }

    public void testLength11_1pc() {
        ss.addCSSAttribute(simple, cssKey, "11.1pc");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("11.1pc", cssValue.toString());
    }

    public void testLength11_1in() {
        ss.addCSSAttribute(simple, cssKey, "11.1in");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("11.1in", cssValue.toString());
    }

    public void testLengthMinus11_1pt() {
        ss.addCSSAttribute(simple, cssKey, "-11.1pt");
        cssValue = simple.getAttribute(cssKey);
        if (negativeValuesInvalid) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        } else {
            assertEquals("-11.1pt", cssValue.toString());
        }
    }

    public void testLengthPlus11_1pt() {
        ss.addCSSAttribute(simple, cssKey, "+11.1pt");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("+11.1pt", cssValue.toString());
    }

    public void testLength11_1Percent() {
        ss.addCSSAttribute(simple, cssKey, "11.1%");
        cssValue = simple.getAttribute(cssKey);
        if (percentageValuesInvalid) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        } else {
            assertEquals("11.1%", cssValue.toString());
        }
    }

    public void testLengthPlus11_1Percent() {
        ss.addCSSAttribute(simple, cssKey, "+11.1%");
        cssValue = simple.getAttribute(cssKey);
        if (percentageValuesInvalid) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        } else {
            assertEquals("+11.1%", cssValue.toString());
        }
    }

    public void testLengthMinus11_1Percent() {
        ss.addCSSAttribute(simple, cssKey, "-11.1%");
        cssValue = simple.getAttribute(cssKey);
        if (percentageValuesInvalid || negativeValuesInvalid) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        } else {
            assertEquals("-11.1%", cssValue.toString());
        }
    }
}
