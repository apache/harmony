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

public class StyleSheet_ConvertAttr_BackgroundRepeatTest extends TestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;
    private Object cssValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testBackgroundRepeatRepeat() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND_REPEAT, "repeat");

        cssValue = simple.getAttribute(Attribute.BACKGROUND_REPEAT);
        assertEquals("repeat", cssValue.toString());
    }

    public void testBackgroundRepeatRepeatX() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND_REPEAT, "repeat-x");

        cssValue = simple.getAttribute(Attribute.BACKGROUND_REPEAT);
        assertEquals("repeat-x", cssValue.toString());
    }

    public void testBackgroundRepeatRepeatY() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND_REPEAT, "repeat-y");

        cssValue = simple.getAttribute(Attribute.BACKGROUND_REPEAT);
        assertEquals("repeat-y", cssValue.toString());
    }

    public void testBackgroundRepeatNoRepeat() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND_REPEAT, "no-repeat");

        cssValue = simple.getAttribute(Attribute.BACKGROUND_REPEAT);
        assertEquals("no-repeat", cssValue.toString());
    }

    public void testBackgroundRepeatRepeatNo() throws Exception {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND_REPEAT, "repeat-no");
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            assertNull(simple.getAttribute(Attribute.BACKGROUND_REPEAT));
        } else {
            assertEquals(1, simple.getAttributeCount());
            assertEquals("repeat", // default value
                         simple.getAttribute(Attribute.BACKGROUND_REPEAT)
                         .toString());
        }
    }
}
