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

public class StyleSheet_ConvertAttr_ListStyleTest extends TestCase {
    private StyleSheet ss;
    private MutableAttributeSet simple;
    private Object cssValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testListStyle01() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "none");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("none", cssValue.toString());
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("none", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyle02() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "disc");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("disc", cssValue.toString());
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("disc", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyle03() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "decimal");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("decimal", cssValue.toString());
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("decimal", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyle04() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "outside");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("outside", cssValue.toString());
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("disc", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyle05() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "inside");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("inside", cssValue.toString());
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("disc", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("inside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyle06() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "url(bullet.gif)");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("url(bullet.gif)", cssValue.toString());
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("disc", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("url(bullet.gif)", cssValue.toString());
    }

    public void testListStyle07() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "square inside");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("square inside", cssValue.toString());
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("square", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("inside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyle08() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "inside square");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("inside square", cssValue.toString());
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("square", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("inside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyle09() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "square url(buller.gif)");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("square url(buller.gif)", cssValue.toString());
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("square", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("url(buller.gif)", cssValue.toString());
    }

    public void testListStyle10() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "url(buller.gif) inside square");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("url(buller.gif) inside square", cssValue.toString());
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("square", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("inside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("url(buller.gif)", cssValue.toString());
    }

    public void testListStyle11() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "disc square");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("disc square", cssValue.toString());
            return;
        }

        assertEquals(0, simple.getAttributeCount());

//        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
//        assertEquals("square", cssValue.toString());
//        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
//        assertEquals("inside", cssValue.toString());
//        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
//        assertEquals("url(buller.gif)", cssValue.toString());
    }

    public void testListStyle12() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "inside outside");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("inside outside", cssValue.toString());
            return;
        }

        assertEquals(0, simple.getAttributeCount());
    }

    public void testListStyle13() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "url(bullet.gif) url('square.gif')");
        if (!BasicSwingTestCase.isHarmony()) {
            assertEquals(1, simple.getAttributeCount());
            cssValue = simple.getAttribute(Attribute.LIST_STYLE);
            assertEquals("url(bullet.gif) url('square.gif')",
                         cssValue.toString());
            return;
        }

        assertEquals(0, simple.getAttributeCount());
    }

    public void testListStyle14() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "none none");
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("none", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyle15() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "none disc");
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("disc", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyle16() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE, "disc none");
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("disc", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("none", cssValue.toString());
    }

    public void testListStyle17() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "none url(bullet.gif)");
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("none", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("url(bullet.gif)", cssValue.toString());
    }

    public void testListStyle18() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "url(bullet.gif) none");
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }

        assertEquals(3, simple.getAttributeCount());

        cssValue = simple.getAttribute(Attribute.LIST_STYLE_TYPE);
        assertEquals("none", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_POSITION);
        assertEquals("outside", cssValue.toString());
        cssValue = simple.getAttribute(Attribute.LIST_STYLE_IMAGE);
        assertEquals("url(bullet.gif)", cssValue.toString());
    }

    public void testListStyle19() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "url(bullet.gif) none none");
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }

        assertEquals(0, simple.getAttributeCount());
    }

    public void testListStyle20() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "url(bullet.gif) none disc");
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }

        assertEquals(0, simple.getAttributeCount());
    }

    public void testListStyle21() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "url(bullet.gif) disc none");
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }

        assertEquals(0, simple.getAttributeCount());
    }

    public void testListStyle22() throws Exception {
        ss.addCSSAttribute(simple, Attribute.LIST_STYLE,
                           "url(bullet.gif) disc inside none");
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }

        assertEquals(0, simple.getAttributeCount());
    }
}
