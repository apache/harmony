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

public class StyleSheet_ConvertAttr_BackgroundPositionTest extends TestCase {
    static final String[] validValues = new String[] {
        "top left", "left top",
        "top", "top center", "center top",
        "right top", "top right",
        "left", "left center", "center left",
        "center", "center center",
        "right", "right center", "center right",
        "bottom left", "left bottom",
        "bottom", "bottom center", "center bottom",
        "bottom right", "right bottom"
    };

    static final String[] percentValues = new String[] {
        "0%", "0% 0%",
        "10%", "10% 0%",
        "50%", "50% 50%", "50% 100%", "100% 50%",
        "100%", "100% 100%", "100% 85%",
        "100%", "100% 100%", "100% 85%"
    };

    static final String[] otherValues = new String[] {
        "1.11px", "1.11px 50%", "1.11px 1.11px",
        "1.11pt", "1.11pt 50%", "1.11pt 1.11pt",
        "1.11mm", "1.11mm 50%", "1.11mm 1.11mm",
        "1.11cm", "1.11cm 50%", "1.11cm 1.11cm",
        "1.11in", "1.11in 50%", "1.11in 1.11in",
        "1.11pc", "1.11pc 50%", "1.11pc 1.11pc",
        "1.11em", "1.11em 50%", "1.11em 1.11em",
        "1.11ex", "1.11ex 50%", "1.11ex 1.11ex",

        "-1.11px", "-1.11px 50%", "-1.11px 1.11px",
        "-50%", "1.11pt -50%", "1.11px -1.11px",
        "-1.11px -1.11px"
    };

    private static final String[] invalidValues = new String[] {
        "top top",
        "left left",
        "right right",
        "bottom bottom",

        "top 50%",
        "25% right",
        "1pt center",
        "bottom 1px",

        "10px 10px 10px"
    };

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
        for (int i = 0; i < validValues.length; i++) {
            simple.removeAttribute(cssKey);
            ss.addCSSAttribute(simple, cssKey, validValues[i]);
            cssValue = simple.getAttribute(cssKey);
            assertEquals("@ " + i, validValues[i], cssValue.toString());
        }
    }

    public void testBackgroundPositionInvalidKeywords() throws Exception {
        for (int i = 0; i < invalidValues.length; i++) {
            simple.removeAttribute(cssKey);
            ss.addCSSAttribute(simple, cssKey, invalidValues[i]);
            cssValue = simple.getAttribute(cssKey);
            if (BasicSwingTestCase.isHarmony()) {
                assertNull("@ " + i, cssValue);
            } else {
                assertEquals("@ " + i, invalidValues[i], cssValue.toString());
            }
        }
    }

    public void testBackgroundPositionPercentage() throws Exception {
        for (int i = 0; i < percentValues.length; i++) {
            simple.removeAttribute(cssKey);
            ss.addCSSAttribute(simple, cssKey, percentValues[i]);
            cssValue = simple.getAttribute(cssKey);
            assertEquals("@ " + i, percentValues[i], cssValue.toString());
        }
    }

    public void testBackgroundPositionOther() throws Exception {
        for (int i = 0; i < otherValues.length; i++) {
            simple.removeAttribute(cssKey);
            ss.addCSSAttribute(simple, cssKey, otherValues[i]);
            cssValue = simple.getAttribute(cssKey);
            assertEquals("@ " + i, otherValues[i], cssValue.toString());
        }
    }
}
