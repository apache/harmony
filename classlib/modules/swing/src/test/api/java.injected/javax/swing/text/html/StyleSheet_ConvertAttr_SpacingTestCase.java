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

public abstract class StyleSheet_ConvertAttr_SpacingTestCase
    extends StyleSheet_ConvertAttr_LengthTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        percentageValuesInvalid = true;
    }

    public void testSpacingNormal() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "normal");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("normal", cssValue.toString());
    }

    public void testSpacingInvalid() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "condensed");
        cssValue = simple.getAttribute(cssKey);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(cssValue);
        } else {
            assertEquals("condensed", cssValue.toString());
        }
    }

    public void testLength11_1Percent() {
        percentageValuesInvalid = BasicSwingTestCase.isHarmony();
        super.testLength11_1Percent();
    }

    public void testLengthMinus11_1Percent() {
        percentageValuesInvalid = BasicSwingTestCase.isHarmony();
        super.testLengthMinus11_1Percent();
    }

    public void testLengthPlus11_1Percent() {
        percentageValuesInvalid = BasicSwingTestCase.isHarmony();
        super.testLengthPlus11_1Percent();
    }
}
