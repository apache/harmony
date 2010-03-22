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

public class StyleSheet_ConvertAttr_BackgroundTest extends BasicSwingTestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testBackground01() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND, "red no-repeat");

        assertAttributes("red", isHarmony() ? "none" : null, "no-repeat",
                         "scroll", isHarmony() ? "0% 0%" : null);
    }

    public void testBackground02() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND, "no-repeat red");

        assertAttributes("red", isHarmony() ? "none" : null, "no-repeat",
                         "scroll", isHarmony() ? "0% 0%" : null);
    }

    public void testBackground03() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "no-repeat red fixed url(bg.jpg)");

        assertAttributes("red", "url(bg.jpg)", "no-repeat",
                         "fixed", isHarmony() ? "0% 0%" : null);
    }

    public void testBackground04() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "no-repeat red fixed url('bg.jpg') top");

        assertAttributes("red", "url('bg.jpg')", "no-repeat", "fixed", "top");
    }

    public void testBackground05() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "no-repeat red fixed url(\"bg.jpg\") top center");

        assertAttributes("red", "url(\"bg.jpg\")", "no-repeat",
                         "fixed", "top center");
    }

    public void testBackground06() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "no-repeat red fixed url(\"bg.jpg\") top top");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("red", "url(\"bg.jpg\")", "no-repeat",
                         "fixed", "top top");
    }

    public void testBackground07() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "no-repeat red url(\"bg.jpg scroll");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("red", null, "no-repeat", "scroll", null);
    }

    public void testBackground08() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "repeat-x blue bg.jpg scroll");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("blue", null, "repeat-x", "scroll", null);
    }

    public void testBackground09() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "repeat-x #AABBCC fixed");

        assertAttributes("#AABBCC", isHarmony() ? "none" : null,
                         "repeat-x", "fixed", isHarmony() ? "0% 0%" : null);
    }

    public void testBackground10() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "repeat-y none rgb(50%, 100%, 25%) fixed center");

        assertAttributes(isHarmony() ? "rgb(50%, 100%, 25%)" : "rgb(50%,",
                         isHarmony() ? "none" : null,
                         "repeat-y",
                         "fixed",
                         isHarmony() ? "center" : "100%, 25%)");
    }

    public void testBackground11() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "repeat-y none green fixed center repeat");
        if (isHarmony()) {
            assertEquals(0, simple.getAttributeCount());
            return;
        }

        assertAttributes("green", null, "repeat-y", "fixed", "center");
    }

    public void testBackground12() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND, "none none");
        if (!isHarmony()) {
            return;
        }
        assertEquals(0, simple.getAttributeCount());
    }

    public void testBackground13() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND, "black white");
        if (!isHarmony()) {
            return;
        }
        assertEquals(0, simple.getAttributeCount());
    }

    public void testBackground14() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND, "fixed scroll");
        if (!isHarmony()) {
            return;
        }
        assertEquals(0, simple.getAttributeCount());
    }

    public void testBackground15() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "50% blue url(bg.jpg) top");
        if (!isHarmony()) {
            return;
        }
        assertEquals(0, simple.getAttributeCount());
    }

    public void testBackground16() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "50% fixed url(bg.jpg) 10px");
        if (!isHarmony()) {
            return;
        }
        assertAttributes("transparent", "url(bg.jpg)", "repeat",
                         "fixed", "50% 10px");
    }

    public void testBackground17() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "url(bg.jpg) none");
        if (!isHarmony()) {
            return;
        }
        assertEquals(0, simple.getAttributeCount());
    }

    public void testBackground18() {
        ss.addCSSAttribute(simple, Attribute.BACKGROUND,
                           "fixed url(bg.jpg) left top transparent");
        if (!isHarmony()) {
            return;
        }
        assertAttributes("transparent", "url(bg.jpg)", "repeat",
                         "fixed", "left top");
    }

    private void assertAttributes(final String color,
                                  final String image,
                                  final String repeat,
                                  final String attachment,
                                  final String position) {
//        assertEquals("Attribute count",
//                     isHarmony() ? 5 : 4, simple.getAttributeCount());

        assertEquals("background-color",
                     color, getCSSAttribute(Attribute.BACKGROUND_COLOR));
        assertEquals("background-image",
                     image, getCSSAttribute(Attribute.BACKGROUND_IMAGE));
        assertEquals("background-repeat",
                     repeat, getCSSAttribute(Attribute.BACKGROUND_REPEAT));
        assertEquals("background-attachment",
                     attachment,
                     getCSSAttribute(Attribute.BACKGROUND_ATTACHMENT));
        assertEquals("background-position",
                     position, getCSSAttribute(Attribute.BACKGROUND_POSITION));
    }

    private String getCSSAttribute(final Attribute cssKey) {
        final Object result = simple.getAttribute(cssKey);
        return result != null ? result.toString() : null;
    }

}
