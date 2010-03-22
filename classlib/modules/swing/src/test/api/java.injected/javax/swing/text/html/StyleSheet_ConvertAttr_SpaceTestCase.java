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

public abstract class StyleSheet_ConvertAttr_SpaceTestCase extends TestCase {
    protected Attribute shorthandKey;
    protected Attribute topKey;
    protected Attribute rightKey;
    protected Attribute bottomKey;
    protected Attribute leftKey;

    protected StyleSheet ss;
    protected MutableAttributeSet simple;

    protected Object top;
    protected Object right;
    protected Object bottom;
    protected Object left;

    protected String defaultValue;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        simple = new SimpleAttributeSet();
    }

    public void testSpace01() throws Exception {
        ss.addCSSAttribute(simple, shorthandKey, "7pt");
        assertProperties();
        assertEquals("7pt", top.toString());
        assertEquals("7pt", right.toString());
        assertEquals("7pt", bottom.toString());
        assertEquals("7pt", left.toString());
    }

    public void testSpace02() throws Exception {
        ss.addCSSAttribute(simple, shorthandKey, "7pt 14mm");
        assertProperties();
        assertEquals("7pt", top.toString());
        assertEquals("14mm", right.toString());
        assertEquals("7pt", bottom.toString());
        assertEquals("14mm", left.toString());
    }

    public void testSpace03() throws Exception {
        ss.addCSSAttribute(simple, shorthandKey, "7pt 14mm 21cm");
        assertProperties();
        assertEquals("7pt", top.toString());
        assertEquals("14mm", right.toString());
        assertEquals("21cm", bottom.toString());
        assertEquals("14mm", left.toString());
    }

    public void testSpace04() throws Exception {
        ss.addCSSAttribute(simple, shorthandKey, "7pt 14mm 21cm 28in");
        assertProperties();
        assertEquals("7pt", top.toString());
        assertEquals("14mm", right.toString());
        assertEquals("21cm", bottom.toString());
        assertEquals("28in", left.toString());
    }

    public void testSpace05() throws Exception {
        ss.addCSSAttribute(simple, shorthandKey,
                           "7pt 14mm 21cm 28in 144px");
        assertProperties();
        assertEquals("7pt", top.toString());
        assertEquals("14mm", right.toString());
        assertEquals("21cm", bottom.toString());
        assertEquals("28in", left.toString());
    }

    public void testSpace03x() throws Exception {
        ss.addCSSAttribute(simple, shorthandKey, "7pt 14mm x 28in");
        if (BasicSwingTestCase.isHarmony()) {
            assertProperties(0);
            assertAllNull();
            return;
        }
        assertProperties(4);
        assertEquals("7pt", top.toString());
        assertEquals("14mm", right.toString());
        assertEquals(defaultValue, bottom.toString());
        assertEquals("28in", left.toString());
    }

    public void testSpace01x() throws Exception {
        ss.addCSSAttribute(simple, shorthandKey, "em 14mm 14pt ex");
        if (BasicSwingTestCase.isHarmony()) {
            assertProperties(0);
            return;
        }
        assertProperties(4);
        assertEquals(defaultValue, top.toString());
        assertEquals("14mm", right.toString());
        assertEquals("14pt", bottom.toString());
        assertEquals(defaultValue, left.toString());
    }

    public void testSpace01NoUnits() throws Exception {
        ss.addCSSAttribute(simple, shorthandKey, "7");
        if (BasicSwingTestCase.isHarmony()) {
            assertProperties(0);
            return;
        }
        assertProperties(4);
        assertEquals("7", top.toString());
        assertEquals("7", right.toString());
        assertEquals("7", bottom.toString());
        assertEquals("7", left.toString());
    }

    public void testSpace02CommaSpace() throws Exception {
        ss.addCSSAttribute(simple, shorthandKey, "7pt, 14mm");
        if (BasicSwingTestCase.isHarmony()) {
            assertProperties(0);
            return;
        }
        assertProperties(4);
        assertEquals(defaultValue, top.toString());
        assertEquals("14mm", right.toString());
        assertEquals(defaultValue, bottom.toString());
        assertEquals("14mm", left.toString());
    }

    public void testSpace02CommaNoSpace() throws Exception {
        ss.addCSSAttribute(simple, shorthandKey, "7pt,14mm");
        if (BasicSwingTestCase.isHarmony()) {
            assertProperties(0);
            return;
        }
        assertProperties(4);
        assertEquals(defaultValue, top.toString());
        assertEquals(defaultValue, right.toString());
        assertEquals(defaultValue, bottom.toString());
        assertEquals(defaultValue, left.toString());
    }

    private void assertProperties() {
        assertProperties(4);
    }

    private void assertProperties(final int count) {
        assertEquals(count, simple.getAttributeCount());
        getProperties();
        if (count == 0) {
            assertAllNull();
        }
    }

    private void getProperties() {
        top = simple.getAttribute(topKey);
        right = simple.getAttribute(rightKey);
        bottom = simple.getAttribute(bottomKey);
        left = simple.getAttribute(leftKey);
    }

    private void assertAllNull() {
        assertNull(top);
        assertNull(right);
        assertNull(bottom);
        assertNull(left);
    }
}
