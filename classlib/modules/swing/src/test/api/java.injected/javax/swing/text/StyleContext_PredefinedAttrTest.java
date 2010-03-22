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
package javax.swing.text;

import java.awt.Color;
import java.awt.Font;
import junit.framework.TestCase;

/**
 * Tests for getBackground, getFont, getFontMetrics, getForeground
 * methods.
 *
 */
public class StyleContext_PredefinedAttrTest extends TestCase {
    private static final String FONT_FAMILY = "Arial";

    private StyleContext sc;

    public void testGetBackground() {
        Color color = sc.getBackground(sc.getEmptySet());
        assertSame(Color.black, color);
        Color colorAttrValue = new Color(0xFAF0E6);
        AttributeSet as = sc.addAttribute(sc.getEmptySet(), StyleConstants.Background,
                colorAttrValue);
        color = sc.getBackground(as);
        assertSame(colorAttrValue, color);
        Color newAttrValue = new Color(0xFAF0E6);
        as = sc.addAttribute(as, StyleConstants.Background, newAttrValue);
        color = sc.getBackground(as);
        assertSame(colorAttrValue, color);
        assertNotSame(colorAttrValue, newAttrValue);
    }

    /*
     * Font getFont(AttributeSet)
     */
    public void testGetFontAttributeSet() {
        Font fontEmpty = sc.getFont(sc.getEmptySet());
        Font fontEmpty2 = sc.getFont(sc.getEmptySet());
        assertSame(fontEmpty, fontEmpty2);
        AttributeSet as = sc.addAttribute(sc.getEmptySet(), StyleConstants.FontFamily,
                FONT_FAMILY);
        Font fontArial = sc.getFont(as);
        assertEquals(FONT_FAMILY, fontArial.getName());
        assertFalse(fontArial.getName().equals(fontEmpty.getName()));
        assertNotSame(fontEmpty, fontArial);
        as = sc.addAttribute(as, StyleConstants.FontSize, new Integer(12));
        Font fontSize = sc.getFont(as);
        assertSame(fontArial, fontSize); // As default font size is 12
        as = sc.addAttribute(as, StyleConstants.Bold, Boolean.FALSE);
        assertSame(fontSize, sc.getFont(as));
        as = sc.addAttribute(as, StyleConstants.Bold, Boolean.TRUE);
        Font fontBold = sc.getFont(as);
        assertNotSame(fontSize, fontBold);
        assertTrue(fontBold.isBold());
    }

    public void testGetFontMetrics() {
        Font plain = sc.getFont(FONT_FAMILY, Font.PLAIN, 12);
        assertSame(sc.getFontMetrics(plain), sc.getFontMetrics(plain));
    }

    /*
     * Font getFont(String, int, int)
     */
    public void testGetFontStringintint() {
        Font one = sc.getFont(FONT_FAMILY, Font.PLAIN, 12);
        Font two = sc.getFont(FONT_FAMILY, Font.PLAIN, 12);
        assertSame(one, two);
        assertTrue(one.isPlain());
        assertEquals(12, one.getSize());
        one = sc.getFont(FONT_FAMILY, Font.BOLD | Font.ITALIC, 12);
        two = sc.getFont(FONT_FAMILY, Font.BOLD | Font.ITALIC, 12);
        assertSame(one, two);
        assertTrue(one.isBold());
        assertTrue(one.isItalic());
    }

    public void testGetForeground() {
        Color color = sc.getForeground(sc.getEmptySet());
        assertSame(Color.black, color);
        Color colorAttrValue = new Color(0xFFE4E1);
        AttributeSet as = sc.addAttribute(sc.getEmptySet(), StyleConstants.Foreground,
                colorAttrValue);
        color = sc.getForeground(as);
        assertSame(colorAttrValue, color);
        Color newAttrValue = new Color(0xFFE4E1);
        as = sc.addAttribute(as, StyleConstants.Foreground, newAttrValue);
        color = sc.getForeground(as);
        assertSame(colorAttrValue, color);
        assertNotSame(colorAttrValue, newAttrValue);
    }

    @Override
    protected void setUp() throws Exception {
        sc = StyleContextTest.sc = new StyleContext();
    }
}