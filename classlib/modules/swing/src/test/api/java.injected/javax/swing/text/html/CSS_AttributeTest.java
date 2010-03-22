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

import javax.swing.text.html.CSS.Attribute;

import junit.framework.TestCase;

/**
 * Tests the values of fields of <code>CSS.Attribute</code> class, i.e.
 * its name (<code>toString()</code>), default value and inheritance.
 *
 */
public class CSS_AttributeTest extends TestCase {
    private Attribute attr;

    public void testBackground() {
        attr = CSS.Attribute.BACKGROUND;
        assertEquals("background", attr.toString());
        assertNull(attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBackgroundAttachment() {
        attr = CSS.Attribute.BACKGROUND_ATTACHMENT;
        assertEquals("background-attachment", attr.toString());
        assertEquals("scroll", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBackgroundColor() {
        attr = CSS.Attribute.BACKGROUND_COLOR;
        assertEquals("background-color", attr.toString());
        assertEquals("transparent", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBackgroundImage() {
        attr = CSS.Attribute.BACKGROUND_IMAGE;
        assertEquals("background-image", attr.toString());
        assertEquals("none", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBackgroundPosition() {
        attr = CSS.Attribute.BACKGROUND_POSITION;
        assertEquals("background-position", attr.toString());
        assertNull(attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBackgroundRepeat() {
        attr = CSS.Attribute.BACKGROUND_REPEAT;
        assertEquals("background-repeat", attr.toString());
        assertEquals("repeat", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorder() {
        attr = CSS.Attribute.BORDER;
        assertEquals("border", attr.toString());
        assertNull(attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderBottom() {
        attr = CSS.Attribute.BORDER_BOTTOM;
        assertEquals("border-bottom", attr.toString());
        assertNull(attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderBottomWidth() {
        attr = CSS.Attribute.BORDER_BOTTOM_WIDTH;
        assertEquals("border-bottom-width", attr.toString());
        assertEquals("medium", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderColor() {
        attr = CSS.Attribute.BORDER_COLOR;
        assertEquals("border-color", attr.toString());
        assertNull(attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderLeft() {
        attr = CSS.Attribute.BORDER_LEFT;
        assertEquals("border-left", attr.toString());
        assertNull(attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderLeftWidth() {
        attr = CSS.Attribute.BORDER_LEFT_WIDTH;
        assertEquals("border-left-width", attr.toString());
        assertEquals("medium", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderRight() {
        attr = CSS.Attribute.BORDER_RIGHT;
        assertEquals("border-right", attr.toString());
        assertNull(attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderRightWidth() {
        attr = CSS.Attribute.BORDER_RIGHT_WIDTH;
        assertEquals("border-right-width", attr.toString());
        assertEquals("medium", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderStyle() {
        attr = CSS.Attribute.BORDER_STYLE;
        assertEquals("border-style", attr.toString());
        assertEquals("none", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderTop() {
        attr = CSS.Attribute.BORDER_TOP;
        assertEquals("border-top", attr.toString());
        assertNull(attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderTopWidth() {
        attr = CSS.Attribute.BORDER_TOP_WIDTH;
        assertEquals("border-top-width", attr.toString());
        assertEquals("medium", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testBorderWidth() {
        attr = CSS.Attribute.BORDER_WIDTH;
        assertEquals("border-width", attr.toString());
        assertEquals("medium", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testClear() {
        attr = CSS.Attribute.CLEAR;
        assertEquals("clear", attr.toString());
        assertEquals("none", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testColor() {
        attr = CSS.Attribute.COLOR;
        assertEquals("color", attr.toString());
        assertNull(attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testDisplay() {
        attr = CSS.Attribute.DISPLAY;
        assertEquals("display", attr.toString());
        assertEquals("block", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testFloat() {
        attr = CSS.Attribute.FLOAT;
        assertEquals("float", attr.toString());
        assertEquals("none", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testFont() {
        attr = CSS.Attribute.FONT;
        assertEquals("font", attr.toString());
        assertNull(attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testFontFamily() {
        attr = CSS.Attribute.FONT_FAMILY;
        assertEquals("font-family", attr.toString());
        assertNull(attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testFontSize() {
        attr = CSS.Attribute.FONT_SIZE;
        assertEquals("font-size", attr.toString());
        assertEquals("medium", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testFontStyle() {
        attr = CSS.Attribute.FONT_STYLE;
        assertEquals("font-style", attr.toString());
        assertEquals("normal", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testFontVariant() {
        attr = CSS.Attribute.FONT_VARIANT;
        assertEquals("font-variant", attr.toString());
        assertEquals("normal", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testFontWeight() {
        attr = CSS.Attribute.FONT_WEIGHT;
        assertEquals("font-weight", attr.toString());
        assertEquals("normal", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testHeight() {
        attr = CSS.Attribute.HEIGHT;
        assertEquals("height", attr.toString());
        assertEquals("auto", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testLetterSpacing() {
        attr = CSS.Attribute.LETTER_SPACING;
        assertEquals("letter-spacing", attr.toString());
        assertEquals("normal", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testLineHeight() {
        attr = CSS.Attribute.LINE_HEIGHT;
        assertEquals("line-height", attr.toString());
        assertEquals("normal", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testListStyle() {
        attr = CSS.Attribute.LIST_STYLE;
        assertEquals("list-style", attr.toString());
        assertNull(attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testListStyleImage() {
        attr = CSS.Attribute.LIST_STYLE_IMAGE;
        assertEquals("list-style-image", attr.toString());
        assertEquals("none", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testListStylePosition() {
        attr = CSS.Attribute.LIST_STYLE_POSITION;
        assertEquals("list-style-position", attr.toString());
        assertEquals("outside", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testListStyleType() {
        attr = CSS.Attribute.LIST_STYLE_TYPE;
        assertEquals("list-style-type", attr.toString());
        assertEquals("disc", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testMargin() {
        attr = CSS.Attribute.MARGIN;
        assertEquals("margin", attr.toString());
        assertNull(attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testMarginBottom() {
        attr = CSS.Attribute.MARGIN_BOTTOM;
        assertEquals("margin-bottom", attr.toString());
        assertEquals("0", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testMarginLeft() {
        attr = CSS.Attribute.MARGIN_LEFT;
        assertEquals("margin-left", attr.toString());
        assertEquals("0", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testMarginRight() {
        attr = CSS.Attribute.MARGIN_RIGHT;
        assertEquals("margin-right", attr.toString());
        assertEquals("0", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testMarginTop() {
        attr = CSS.Attribute.MARGIN_TOP;
        assertEquals("margin-top", attr.toString());
        assertEquals("0", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testPadding() {
        attr = CSS.Attribute.PADDING;
        assertEquals("padding", attr.toString());
        assertNull(attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testPaddingBottom() {
        attr = CSS.Attribute.PADDING_BOTTOM;
        assertEquals("padding-bottom", attr.toString());
        assertEquals("0", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testPaddingLeft() {
        attr = CSS.Attribute.PADDING_LEFT;
        assertEquals("padding-left", attr.toString());
        assertEquals("0", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testPaddingRight() {
        attr = CSS.Attribute.PADDING_RIGHT;
        assertEquals("padding-right", attr.toString());
        assertEquals("0", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testPaddingTop() {
        attr = CSS.Attribute.PADDING_TOP;
        assertEquals("padding-top", attr.toString());
        assertEquals("0", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testTextAlign() {
        attr = CSS.Attribute.TEXT_ALIGN;
        assertEquals("text-align", attr.toString());
        assertNull(attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testTextDecoration() {
        attr = CSS.Attribute.TEXT_DECORATION;
        assertEquals("text-decoration", attr.toString());
        assertEquals("none", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testTextIndent() {
        attr = CSS.Attribute.TEXT_INDENT;
        assertEquals("text-indent", attr.toString());
        assertEquals("0", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testTextTransform() {
        attr = CSS.Attribute.TEXT_TRANSFORM;
        assertEquals("text-transform", attr.toString());
        assertEquals("none", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testVerticalAlign() {
        attr = CSS.Attribute.VERTICAL_ALIGN;
        assertEquals("vertical-align", attr.toString());
        assertEquals("baseline", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testWhiteSpace() {
        attr = CSS.Attribute.WHITE_SPACE;
        assertEquals("white-space", attr.toString());
        assertEquals("normal", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }

    public void testWidth() {
        attr = CSS.Attribute.WIDTH;
        assertEquals("width", attr.toString());
        assertEquals("auto", attr.getDefaultValue());
        assertFalse(attr.isInherited());
    }

    public void testWordSpacing() {
        attr = CSS.Attribute.WORD_SPACING;
        assertEquals("word-spacing", attr.toString());
        assertEquals("normal", attr.getDefaultValue());
        assertTrue(attr.isInherited());
    }
}
