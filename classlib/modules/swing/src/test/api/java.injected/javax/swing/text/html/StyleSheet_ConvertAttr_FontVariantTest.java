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

public class StyleSheet_ConvertAttr_FontVariantTest extends TestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;
    private Object cssValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testFontVariantNormal() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_VARIANT, "normal");
        cssValue = simple.getAttribute(Attribute.FONT_VARIANT);
        assertEquals("normal", cssValue.toString());
    }

    public void testFontVariantSmallCaps() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_VARIANT, "small-caps");
        cssValue = simple.getAttribute(Attribute.FONT_VARIANT);
        assertEquals("small-caps", cssValue.toString());
    }

    public void testFontVariantInvalid() throws Exception {
        ss.addCSSAttribute(simple, Attribute.FONT_VARIANT, "invalid");
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(simple.getAttribute(Attribute.FONT_VARIANT));
            return;
        }
        cssValue = simple.getAttribute(Attribute.FONT_VARIANT);
        assertEquals("invalid", cssValue.toString());
    }
}
