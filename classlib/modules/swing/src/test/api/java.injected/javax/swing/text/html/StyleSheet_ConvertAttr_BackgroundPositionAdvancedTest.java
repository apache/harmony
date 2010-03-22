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

import java.lang.reflect.Field;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.CSS.Attribute;

public class StyleSheet_ConvertAttr_BackgroundPositionAdvancedTest
    extends BasicSwingTestCase {

    /**
     * Expected values for validValues.
     */
    private static final String[] expectedVV = new String[] {
        "0% 0%",      "0% 0%",
        "50% 0%",     "50% 0%",    "50% 0%",
        "100% 0%",    "100% 0%",
        "0% 50%",     "0% 50%",    "0% 50%",
        "50% 50%",    "50% 50%",
        "100% 50%",   "100% 50%",  "100% 50%",
        "0% 100%",    "0% 100%",
        "50% 100%",   "50% 100%",  "50% 100%",
        "100% 100%",  "100% 100%"
    };

    private static final String[] validValues =
        StyleSheet_ConvertAttr_BackgroundPositionTest.validValues;
    private static final String[] percentValues =
        StyleSheet_ConvertAttr_BackgroundPositionTest.percentValues;
    private static final String[] otherValues =
        StyleSheet_ConvertAttr_BackgroundPositionTest.otherValues;

    private StyleSheet ss;
    private MutableAttributeSet simple;
    private Attribute cssKey;
    private Object cssValue;

    protected void setUp() throws Exception {
        super.setUp();
        cssKey = Attribute.BACKGROUND_POSITION;
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testBackgroundPositionValidKeywords() throws Exception {
        if (!isHarmony()) {
            return;
        }

        assertEquals(validValues.length, expectedVV.length);
        for (int i = 0; i < validValues.length; i++) {
            simple.removeAttribute(cssKey);
            ss.addCSSAttribute(simple, cssKey, validValues[i]);
            cssValue = simple.getAttribute(cssKey);
            assertEquals("@ " + i, expectedVV[i], getString());
        }
    }

    public void testBackgroundPositionPercentage() throws Exception {
        if (!isHarmony()) {
            return;
        }

        for (int i = 0; i < percentValues.length; i++) {
            simple.removeAttribute(cssKey);
            ss.addCSSAttribute(simple, cssKey, percentValues[i]);
            cssValue = simple.getAttribute(cssKey);
            assertEquals("@ " + i,
                         getExtendedValue(percentValues[i]), getString());
        }
    }

    public void testBackgroundPositionOther() throws Exception {
        if (!isHarmony()) {
            return;
        }

        for (int i = 0; i < otherValues.length; i++) {
            simple.removeAttribute(cssKey);
            ss.addCSSAttribute(simple, cssKey, otherValues[i]);
            cssValue = simple.getAttribute(cssKey);
            assertEquals("@ " + i,
                         getExtendedValue(otherValues[i]), getString());
        }
    }

    private static String getExtendedValue(String value) {
        return value.indexOf(' ') != -1 ? value
                                        : value + " 50%";
    }

    private Object getHorz() {
        try {
            Field f = cssValue.getClass().getDeclaredField("horz");
            f.setAccessible(true);
            return f.get(cssValue);
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        }
        return null;
    }

    private Object getVert() {
        try {
            Field f = cssValue.getClass().getDeclaredField("vert");
            f.setAccessible(true);
            return f.get(cssValue);
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        }
        return null;
    }

    private String getString() {
        return getHorz() + " " + getVert();
    }
}
