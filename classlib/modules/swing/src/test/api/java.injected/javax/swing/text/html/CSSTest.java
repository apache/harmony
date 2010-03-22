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

import java.io.IOException;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.StyleContext;
import javax.swing.text.html.CSS.Attribute;

public class CSSTest extends BasicSwingTestCase {
    public void testGetAllAttributeKeys() {
        Attribute[] keys = CSS.getAllAttributeKeys();
        if (isHarmony()) {
            // The array contains only the public attribute keys
            assertEquals(53, keys.length);
        } else {
            // The array contains two more attributes than declared in the Spec
            assertEquals(55, keys.length);
        }
        Attribute[] allAttributeKeys = {
            Attribute.BACKGROUND,
            Attribute.BACKGROUND_ATTACHMENT,
            Attribute.BACKGROUND_COLOR,
            Attribute.BACKGROUND_IMAGE,
            Attribute.BACKGROUND_POSITION,
            Attribute.BACKGROUND_REPEAT,
            Attribute.BORDER,
            Attribute.BORDER_BOTTOM,
            Attribute.BORDER_BOTTOM_WIDTH,
            Attribute.BORDER_COLOR,
            Attribute.BORDER_LEFT,
            Attribute.BORDER_LEFT_WIDTH,
            Attribute.BORDER_RIGHT,
            Attribute.BORDER_RIGHT_WIDTH,
            Attribute.BORDER_STYLE,
            Attribute.BORDER_TOP,
            Attribute.BORDER_TOP_WIDTH,
            Attribute.BORDER_WIDTH,
            Attribute.CLEAR,
            Attribute.COLOR,
            Attribute.DISPLAY,
            Attribute.FLOAT,
            Attribute.FONT,
            Attribute.FONT_FAMILY,
            Attribute.FONT_SIZE,
            Attribute.FONT_STYLE,
            Attribute.FONT_VARIANT,
            Attribute.FONT_WEIGHT,
            Attribute.HEIGHT,
            Attribute.LETTER_SPACING,
            Attribute.LINE_HEIGHT,
            Attribute.LIST_STYLE,
            Attribute.LIST_STYLE_IMAGE,
            Attribute.LIST_STYLE_POSITION,
            Attribute.LIST_STYLE_TYPE,
            Attribute.MARGIN,
            Attribute.MARGIN_BOTTOM,
            Attribute.MARGIN_LEFT,
            Attribute.MARGIN_RIGHT,
            Attribute.MARGIN_TOP,
            Attribute.PADDING,
            Attribute.PADDING_BOTTOM,
            Attribute.PADDING_LEFT,
            Attribute.PADDING_RIGHT,
            Attribute.PADDING_TOP,
            Attribute.TEXT_ALIGN,
            Attribute.TEXT_DECORATION,
            Attribute.TEXT_INDENT,
            Attribute.TEXT_TRANSFORM,
            Attribute.VERTICAL_ALIGN,
            Attribute.WHITE_SPACE,
            Attribute.WIDTH,
            Attribute.WORD_SPACING
        };
        final boolean[] found = new boolean[allAttributeKeys.length];

        for (int i = 0; i < allAttributeKeys.length; i++) {
            for (int j = 0; j < allAttributeKeys.length; j++) {
                if (allAttributeKeys[i] == keys[j]) {
                    found[i] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < found.length; i++) {
            assertTrue("Attribute " + allAttributeKeys[i] + " was not found",
                       found[i]);
        }
    }

    public void testGetAttribute() throws Exception {
        // These properties are defined only in CSS Level 2
        assertNull(CSS.getAttribute("position"));
        assertNull(CSS.getAttribute("cursor"));
        assertNull(CSS.getAttribute("border-collapse"));

        if (!isHarmony()) {
            // These properties are defined somewhere but they're not public
            assertNotNull(CSS.getAttribute("border-spacing"));
            assertNotNull(CSS.getAttribute("caption-side"));
        }


        // Standard properties defined in CSS Level 1
        assertSame(Attribute.BACKGROUND,
                   CSS.getAttribute("background"));
        assertSame(Attribute.BACKGROUND_ATTACHMENT,
                   CSS.getAttribute("background-attachment"));
        assertSame(Attribute.BACKGROUND_COLOR,
                   CSS.getAttribute("background-color"));
        assertSame(Attribute.BACKGROUND_IMAGE,
                   CSS.getAttribute("background-image"));
        assertSame(Attribute.BACKGROUND_POSITION,
                   CSS.getAttribute("background-position"));
        assertSame(Attribute.BACKGROUND_REPEAT,
                   CSS.getAttribute("background-repeat"));
        assertSame(Attribute.BORDER,
                   CSS.getAttribute("border"));
        assertSame(Attribute.BORDER_BOTTOM,
                   CSS.getAttribute("border-bottom"));
        assertSame(Attribute.BORDER_BOTTOM_WIDTH,
                   CSS.getAttribute("border-bottom-width"));
        assertSame(Attribute.BORDER_COLOR,
                   CSS.getAttribute("border-color"));
        assertSame(Attribute.BORDER_LEFT,
                   CSS.getAttribute("border-left"));
        assertSame(Attribute.BORDER_LEFT_WIDTH,
                   CSS.getAttribute("border-left-width"));
        assertSame(Attribute.BORDER_RIGHT,
                   CSS.getAttribute("border-right"));
        assertSame(Attribute.BORDER_RIGHT_WIDTH,
                   CSS.getAttribute("border-right-width"));
        assertSame(Attribute.BORDER_STYLE,
                   CSS.getAttribute("border-style"));
        assertSame(Attribute.BORDER_TOP,
                   CSS.getAttribute("border-top"));
        assertSame(Attribute.BORDER_TOP_WIDTH,
                   CSS.getAttribute("border-top-width"));
        assertSame(Attribute.BORDER_WIDTH,
                   CSS.getAttribute("border-width"));
        assertSame(Attribute.CLEAR,
                   CSS.getAttribute("clear"));
        assertSame(Attribute.COLOR,
                   CSS.getAttribute("color"));
        assertSame(Attribute.DISPLAY,
                   CSS.getAttribute("display"));
        assertSame(Attribute.FLOAT,
                   CSS.getAttribute("float"));
        assertSame(Attribute.FONT,
                   CSS.getAttribute("font"));
        assertSame(Attribute.FONT_FAMILY,
                   CSS.getAttribute("font-family"));
        assertSame(Attribute.FONT_SIZE,
                   CSS.getAttribute("font-size"));
        assertSame(Attribute.FONT_STYLE,
                   CSS.getAttribute("font-style"));
        assertSame(Attribute.FONT_VARIANT,
                   CSS.getAttribute("font-variant"));
        assertSame(Attribute.FONT_WEIGHT,
                   CSS.getAttribute("font-weight"));
        assertSame(Attribute.HEIGHT,
                   CSS.getAttribute("height"));
        assertSame(Attribute.LETTER_SPACING,
                   CSS.getAttribute("letter-spacing"));
        assertSame(Attribute.LINE_HEIGHT,
                   CSS.getAttribute("line-height"));
        assertSame(Attribute.LIST_STYLE,
                   CSS.getAttribute("list-style"));
        assertSame(Attribute.LIST_STYLE_IMAGE,
                   CSS.getAttribute("list-style-image"));
        assertSame(Attribute.LIST_STYLE_POSITION,
                   CSS.getAttribute("list-style-position"));
        assertSame(Attribute.LIST_STYLE_TYPE,
                   CSS.getAttribute("list-style-type"));
        assertSame(Attribute.MARGIN,
                   CSS.getAttribute("margin"));
        assertSame(Attribute.MARGIN_BOTTOM,
                   CSS.getAttribute("margin-bottom"));
        assertSame(Attribute.MARGIN_LEFT,
                   CSS.getAttribute("margin-left"));
        assertSame(Attribute.MARGIN_RIGHT,
                   CSS.getAttribute("margin-right"));
        assertSame(Attribute.MARGIN_TOP,
                   CSS.getAttribute("margin-top"));
        assertSame(Attribute.PADDING,
                   CSS.getAttribute("padding"));
        assertSame(Attribute.PADDING_BOTTOM,
                   CSS.getAttribute("padding-bottom"));
        assertSame(Attribute.PADDING_LEFT,
                   CSS.getAttribute("padding-left"));
        assertSame(Attribute.PADDING_RIGHT,
                   CSS.getAttribute("padding-right"));
        assertSame(Attribute.PADDING_TOP,
                   CSS.getAttribute("padding-top"));
        assertSame(Attribute.TEXT_ALIGN,
                   CSS.getAttribute("text-align"));
        assertSame(Attribute.TEXT_DECORATION,
                   CSS.getAttribute("text-decoration"));
        assertSame(Attribute.TEXT_INDENT,
                   CSS.getAttribute("text-indent"));
        assertSame(Attribute.TEXT_TRANSFORM,
                   CSS.getAttribute("text-transform"));
        assertSame(Attribute.VERTICAL_ALIGN,
                   CSS.getAttribute("vertical-align"));
        assertSame(Attribute.WHITE_SPACE,
                   CSS.getAttribute("white-space"));
        assertSame(Attribute.WIDTH,
                   CSS.getAttribute("width"));
        assertSame(Attribute.WORD_SPACING,
                   CSS.getAttribute("word-spacing"));
    }

    public void testStaticAttributeKeys() {
        final Attribute[] attrs = CSS.getAllAttributeKeys();
        for (int i = 0; i < attrs.length; i++) {
            Object staticKey = StyleContext.getStaticAttributeKey(attrs[i]);
            assertSame("Static attribute for " + attrs[i] + ", index " + i,
                       attrs[i], StyleContext.getStaticAttribute(staticKey));
        }
    }

    public void testSerializable() throws IOException, ClassNotFoundException {
        CSS css = new CSS();
        CSS read = (CSS)serializeObject(css);
        assertNotNull(read);
    }
}
