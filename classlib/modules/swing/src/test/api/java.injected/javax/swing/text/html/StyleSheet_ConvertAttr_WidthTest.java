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

public class StyleSheet_ConvertAttr_WidthTest
    extends StyleSheet_ConvertAttr_LengthTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        cssKey = CSS.Attribute.WIDTH;
        negativeValuesInvalid = true;
    }

    public void testAuto() {
        ss.addCSSAttribute(simple, cssKey, "auto");
        cssValue = simple.getAttribute(cssKey);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("auto", cssValue.toString());
        } else {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        }
    }

    public void testLength0_75em() {
        ss.addCSSAttribute(simple, cssKey, "0.75em");
        cssValue = simple.getAttribute(cssKey);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("0.75em", cssValue.toString());
        } else {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        }
    }

    public void testLength1_25ex() {
        ss.addCSSAttribute(simple, cssKey, "1.25ex");
        cssValue = simple.getAttribute(cssKey);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("1.25ex", cssValue.toString());
        } else {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        }
    }

    public void testLengthMinus11_1Percent() {
        negativeValuesInvalid = BasicSwingTestCase.isHarmony();
        super.testLengthMinus11_1Percent();
    }

    public void testLengthMinus11_1pt() {
        negativeValuesInvalid = BasicSwingTestCase.isHarmony();
        super.testLengthMinus11_1pt();
    }
}
