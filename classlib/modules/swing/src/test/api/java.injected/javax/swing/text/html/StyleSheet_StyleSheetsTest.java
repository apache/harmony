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

import javax.swing.text.Style;
import javax.swing.text.html.CSS.Attribute;

import junit.framework.TestCase;

public class StyleSheet_StyleSheetsTest extends TestCase {
    private StyleSheet ss;
    private StyleSheet second;
    private StyleSheet third;
    private StyleSheet[] sheets;
    private Style rule;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
    }

    public void testGetStyleSheets() {
        sheets = ss.getStyleSheets();
        assertNull(sheets);

        second = new StyleSheet();
        ss.addStyleSheet(second);
        sheets = ss.getStyleSheets();
        assertEquals(1, sheets.length);
        assertSame(second, sheets[0]);
    }

    public void testAddStyleSheet() {
        second = new StyleSheet();
        third = new StyleSheet();
        ss.addStyleSheet(second);
        ss.addStyleSheet(third);

        sheets = ss.getStyleSheets();
        assertEquals(2, sheets.length);
        assertSame(third, sheets[0]);
        assertSame(second, sheets[1]);
    }

    public void testAddStyleSheetDuplicate() {
        second = new StyleSheet();
        ss.addStyleSheet(second);
        assertEquals(1, ss.getStyleSheets().length);
        ss.addStyleSheet(second);
        assertEquals(1, ss.getStyleSheets().length);
    }

    public void testAddStyleSheetUpdateRule() {
        rule = ss.getRule("p");
        second = new StyleSheet();
        second.addRule("p { color: red }");
        ss.addStyleSheet(second);

        assertEquals(2, rule.getAttributeCount());
        assertEquals("red", rule.getAttribute(Attribute.COLOR).toString());

        third = new StyleSheet();
        third.addRule("p { color: blue }");
        ss.addStyleSheet(third);

        assertEquals(4, rule.getAttributeCount());
        assertEquals("blue", rule.getAttribute(Attribute.COLOR).toString());
    }

    public void testAddStyleSheetOverrideRule() {
        ss.addRule("p { color: black }");
        rule = ss.getRule("p");
        assertEquals("black", rule.getAttribute(Attribute.COLOR).toString());

        second = new StyleSheet();
        second.addRule("p { color: red }");
        ss.addStyleSheet(second);
        assertEquals("black", rule.getAttribute(Attribute.COLOR).toString());
    }

    public void testRemoveStyleSheet() {
        second = new StyleSheet();
        third  = new StyleSheet();
        ss.addStyleSheet(second);
        ss.addStyleSheet(third);

        assertEquals(2, ss.getStyleSheets().length);

        ss.removeStyleSheet(second);
        assertEquals(1, ss.getStyleSheets().length);
        assertEquals(third, ss.getStyleSheets()[0]);

        ss.removeStyleSheet(second);
        assertEquals(1, ss.getStyleSheets().length);

        ss.removeStyleSheet(third);
        assertNull(ss.getStyleSheets());
    }

    public void testRemoveStyleSheetRuleChange() {
        rule = ss.getRule("p");
        second = new StyleSheet();
        second.addRule("p { color: red }");
        ss.addStyleSheet(second);

        third = new StyleSheet();
        third.addRule("p { color: blue }");
        ss.addStyleSheet(third);
        assertEquals("blue", rule.getAttribute(Attribute.COLOR).toString());

        ss.removeStyleSheet(third);
        assertEquals("red", rule.getAttribute(Attribute.COLOR).toString());

        ss.removeStyleSheet(second);
        assertEquals(0, rule.getAttributeCount());
        assertNull(rule.getAttribute(Attribute.COLOR));
    }
}
