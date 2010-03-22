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

public class StyleSheet_ConvertAttr_BorderColorTest extends BasicSwingTestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;
    private Attribute cssKey;
    private Object cssValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
        cssKey = CSS.Attribute.BORDER_COLOR;
    }

    public void testRed() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "red");
        cssValue = simple.getAttribute(cssKey);
        assertNotSame(String.class, cssValue.getClass());
        assertEquals("red", cssValue.toString());
    }

    public void testRedGreen() throws Exception {
        if (!isHarmony()) {
            testExceptionalCase(new NullPointerCase() {
                public void exceptionalAction() throws Exception {
                    ss.addCSSAttribute(simple, cssKey, "red green");
                }
            });
            return;
        }

        ss.addCSSAttribute(simple, cssKey, "red green");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("red green", cssValue.toString());
    }

    public void testRedGreenFuchsia() throws Exception {
        if (!isHarmony()) {
            testExceptionalCase(new NullPointerCase() {
                public void exceptionalAction() throws Exception {
                    ss.addCSSAttribute(simple, cssKey, "red green fuchsia");
                }
            });
            return;
        }

        ss.addCSSAttribute(simple, cssKey, "red green fuchsia");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("red green fuchsia", cssValue.toString());
    }

    public void testRedGreenFuchsiaAqua() throws Exception {
        if (!isHarmony()) {
            testExceptionalCase(new NullPointerCase() {
                public void exceptionalAction() throws Exception {
                    ss.addCSSAttribute(simple, cssKey, "red green fuchsia aqua");
                }
            });
            return;
        }

        ss.addCSSAttribute(simple, cssKey, "red green fuchsia aqua");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("red green fuchsia aqua", cssValue.toString());
    }

    public void testHex() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "#FFEEDD");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("#FFEEDD", cssValue.toString());
    }

    public void testHexHex() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "#FFEEDD #EEDDFF");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("#FFEEDD #EEDDFF", cssValue.toString());
    }

    public void testRGB() throws Exception {
        ss.addCSSAttribute(simple, cssKey, "rgb(127, 255, 75)");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("rgb(127, 255, 75)", cssValue.toString());
    }

    public void testRGB_RGB() throws Exception {
        ss.addCSSAttribute(simple, cssKey,
                           "rgb(127, 255, 75) rgb(50%, 100%, 30%)");
        cssValue = simple.getAttribute(cssKey);
        assertEquals("rgb(127, 255, 75) rgb(50%, 100%, 30%)",
                     cssValue.toString());
    }
}
