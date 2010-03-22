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

public abstract class StyleSheet_ConvertAttr_BorderWidthTestCase
    extends StyleSheet_ConvertAttr_LengthTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        negativeValuesInvalid = true;
        percentageValuesInvalid = true;
    }

    public void testBorderWidthThin() {
        ss.addCSSAttribute(simple, cssKey, "thin");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("thin", cssValue.toString());
    }

    public void testBorderWidthMedium() {
        ss.addCSSAttribute(simple, cssKey, "medium");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("medium", cssValue.toString());
    }

    public void testBorderWidthThick() {
        ss.addCSSAttribute(simple, cssKey, "thick");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("thick", cssValue.toString());
    }

    public void testBorderWidthSmall() {
        ss.addCSSAttribute(simple, cssKey, "small");
        cssValue = simple.getAttribute(cssKey);
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("medium", simple.getAttribute(cssKey).toString());
            return;
        }
        assertEquals(0, simple.getAttributeCount());
        assertNull(cssValue);
    }

    public void testLength0_75em() {
        if (!BasicSwingTestCase.isHarmony()) {
            ss.addCSSAttribute(simple, cssKey, "0.75em");
            assertEquals(1, simple.getAttributeCount());
            assertEquals("medium", simple.getAttribute(cssKey).toString());
            return;
        }
        super.testLength0_75em();
    }

    public void testLength1_25ex() {
        if (!BasicSwingTestCase.isHarmony()) {
            ss.addCSSAttribute(simple, cssKey, "1.25ex");
            assertEquals(1, simple.getAttributeCount());
            assertEquals("medium", simple.getAttribute(cssKey).toString());
            return;
        }
        super.testLength1_25ex();
    }

    public void testLengthMinus11_1pt() {
        negativeValuesInvalid = BasicSwingTestCase.isHarmony();
        super.testLengthMinus11_1pt();
    }

    public void testLength11_1Percent() {
        percentageValuesInvalid = BasicSwingTestCase.isHarmony();
        super.testLength11_1Percent();
    }

    public void testLengthMinus11_1Percent() {
        negativeValuesInvalid = BasicSwingTestCase.isHarmony();
        percentageValuesInvalid = BasicSwingTestCase.isHarmony();
        super.testLengthMinus11_1Percent();
    }

    public void testLengthPlus11_1Percent() {
        percentageValuesInvalid = BasicSwingTestCase.isHarmony();
        super.testLengthPlus11_1Percent();
    }
}
