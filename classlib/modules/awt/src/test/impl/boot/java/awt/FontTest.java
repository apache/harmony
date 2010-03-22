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

package java.awt;

import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Collections;

import junit.framework.TestCase;

public class FontTest extends TestCase {

    private final Font f = new Font("dialog", Font.PLAIN, 12);

    /**
     * Checks Font.getLineMetrics() methods if FontRenderContext parameter is
     * NULL.
     * 
     */
    public void test_Font_getLineMetrics_WithNullFRC() {
        // Regression for Harmony-1465
        final String str = "test";
        try {
            f.getLineMetrics(str, null);
            fail("NullPointerException expected but wasn't thrown!");
        } catch (NullPointerException e) {
            // as expected
        }

        try {
            f.getLineMetrics(str, 1, 3, null);
            fail("NullPointerException expected but wasn't thrown!");
        } catch (NullPointerException e) {
            // as expected
        }

        try {
            f.getLineMetrics(str.toCharArray(), 1, 3, null);
            fail("NullPointerException expected but wasn't thrown!");
        } catch (NullPointerException e) {
            // as expected
        }

        try {
            AttributedString as = new AttributedString("test");
            as.addAttribute(TextAttribute.FONT, f, 0, 2);

            f.getLineMetrics(as.getIterator(), 1, 3, null);
            fail("NullPointerException expected but wasn't thrown!");
        } catch (NullPointerException e) {
            // as expected
        }
    }

    public void test_Font_getMaxCharBounds_WithNullFRC() {
        // Regression for HARMONY-1549
        try {
            Font font = Font.decode("dialog");
            System.out.println(font.getMaxCharBounds(null));
            fail("NullPointerException expected!");
        } catch (NullPointerException e) {
            // expected
        }

    }

    public void test_Font_getFamily_WithNullLocale() {
        // Regression for Harmony-1543
        try {
            Font fnt = Font.getFont(Collections.<Attribute, Object>emptyMap());
            fnt.getFamily(null);
            fail("NullPointerException expected!");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void test_Font_getFont_WithNullSystemProperty() {
        // Regression for HARMONY-1546
        try {
            Font.getFont((String) null);
            fail("NullPointerException expected!");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            Font.getFont((String) null, new Font("dialog", Font.PLAIN, 12));
            fail("NullPointerException expected!");
        } catch (NullPointerException e) {
            // expected
        }
    }
    
    /*
     * Compatibility test. We check that Harmony throws same 
     * exceptions as RI does.
     */
    public void test_Font_getStringBounds_WithNullTextSource() {
        // regression test for Harmony-1591
        Font font = new Font("dialog", Font.PLAIN, 12);
        FontRenderContext cnt = new FontRenderContext(null, false, true);
        try {
            font.getStringBounds((char[]) null, -16, 1, cnt);
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            font.getStringBounds((String) null, -16, 1, cnt);
        } catch (NullPointerException e) {
            // expected
        }

        try {
            font.getStringBounds((CharacterIterator) null, -16, 1, cnt);
        } catch (NullPointerException e) {
            // expected
        }

        try {
            font.getStringBounds((String) null, cnt);
        } catch (NullPointerException e) {
            // expected
        }
    }
    
    /**
     * Checks Font.getStringBounds() methods if FontRenderContext parameter is NULL. 
     *
     */
    public void test_Font_getStringBounds_WithNullFRC() {
        // regression test for Harmony-1595
        Font font = Font.decode("Arial");
        try {
            font.getStringBounds(new char[] { 'a' }, 0, 1,
                    (FontRenderContext) null);
            fail("NullPointerException expected!");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            font.getStringBounds(new StringCharacterIterator("a"), 0, 1,
                    (FontRenderContext) null);
            fail("NullPointerException expected!");
        } catch (NullPointerException e) {
            // expected
        }

        String str = "str";
        try {
            font.getStringBounds(str, 0, 1, (FontRenderContext) null);
            fail("NullPointerException expected!");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            font.getStringBounds(str, (FontRenderContext) null);
            fail("NullPointerException expected!");
        } catch (NullPointerException e) {
            // expected
        }
    }

}
