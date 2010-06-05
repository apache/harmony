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
import java.awt.Container;
import java.awt.Font;
import javax.swing.JTextArea;
import junit.framework.TestCase;

/**
 * Tests GlyphView class, its methods to get attributes.
 *
 */
public class GlyphView_AttributesTest extends TestCase {
    private DefaultStyledDocument styledDoc;

    private PlainDocument plainDoc;

    private Element leaf;

    private Element line;

    private GlyphView styledView;

    private GlyphView plainView;

    private GlyphView styledWithParent;

    private GlyphView plainWithParent;

    private Container parent;

    private static final int NONE = -1;

    private static final int FORE = 0;

    private static final int BACK = 1;

    private static final int FONT = 2;

    private static final int AREA_FORE = 3;

    private static final int AREA_FONT = 4;

    private static final String[] PROPERTY_NAMES = { "Foreground", "Background", "Font",
            "Area.Foreground", "Area.Font" };

    /**
     * 0: StyledDocument.getForeground
     * 1: StyledDocument.getBackground
     * 2: StyledDocument.getFont
     * 3: JTextArea.getForeground
     * 4: JTextArea.getFont
     */
    private final boolean[] calledMethods = new boolean[5];

    private static final String text = "test text";

    private static final Color THE_FOREGROUND = new Color(127, 152, 51);

    private static final Font THE_FONT = new Font("Crazy Font", Font.PLAIN, 36);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        styledDoc = new DefaultStyledDocument() {
            private static final long serialVersionUID = 1L;

            @Override
            public Color getForeground(AttributeSet attrs) {
                calledMethods[FORE] = true;
                assertSame(leaf, attrs);
                return super.getForeground(attrs);
            }

            @Override
            public Color getBackground(AttributeSet attrs) {
                calledMethods[BACK] = true;
                assertSame(leaf, attrs);
                return super.getBackground(attrs);
            }

            @Override
            public Font getFont(AttributeSet attrs) {
                calledMethods[FONT] = true;
                assertSame(leaf, attrs);
                return super.getFont(attrs);
            }
        };
        parent = new JTextArea() {
            private static final long serialVersionUID = 1L;

            @Override
            public Color getForeground() {
                calledMethods[AREA_FORE] = true;
                return THE_FOREGROUND;
            }

            @Override
            public Font getFont() {
                calledMethods[AREA_FONT] = true;
                return THE_FONT;
            }
        };
        styledDoc.insertString(0, text, null);
        leaf = styledDoc.getCharacterElement(0);
        styledView = new GlyphView(leaf);
        styledWithParent = new GlyphView(leaf) {
            @Override
            public Container getContainer() {
                return parent;
            }
        };
        plainDoc = new PlainDocument();
        plainDoc.insertString(0, text, null);
        line = plainDoc.getDefaultRootElement().getElement(0);
        plainView = new GlyphView(line);
        plainWithParent = new GlyphView(line) {
            @Override
            public Container getContainer() {
                return parent;
            }
        };
        for (int i = 0; i < calledMethods.length; i++) {
            calledMethods[i] = false;
        }
    }

    public void testGetForeground() {
        assertSame(StyleConstants.getForeground(leaf.getAttributes()), styledView
                .getForeground());
        assertCalledMethods(FORE);
        assertSame(StyleConstants.getForeground(leaf.getAttributes()), styledWithParent
                .getForeground());
        assertCalledMethods(FORE);
        assertNull(plainView.getForeground());
        assertCalledMethods(NONE);
        assertSame(plainWithParent.getForeground(), plainWithParent.getForeground());
        assertCalledMethods(AREA_FORE);
    }

    public void testGetBackground() {
        assertNull(styledView.getBackground());
        assertCalledMethods(NONE);
        assertNull(styledWithParent.getBackground());
        assertCalledMethods(NONE);
        assertNull(plainView.getBackground());
        assertCalledMethods(NONE);
        assertNull(plainWithParent.getBackground());
        assertCalledMethods(NONE);
        setAttribute(StyleConstants.Background, THE_FOREGROUND);
        assertSame(THE_FOREGROUND, styledView.getBackground());
        assertCalledMethods(BACK);
        assertSame(THE_FOREGROUND, styledWithParent.getBackground());
        assertCalledMethods(BACK);
    }

    public void testGetFont() {
        Font font = styledView.getFont();
        assertEquals(StyleConstants.getFontFamily(leaf.getAttributes()), font.getFamily());
        assertEquals(StyleConstants.getFontSize(leaf.getAttributes()), font.getSize());
        assertCalledMethods(FONT);
        font = styledWithParent.getFont();
        assertEquals(StyleConstants.getFontFamily(leaf.getAttributes()), font.getFamily());
        assertEquals(StyleConstants.getFontSize(leaf.getAttributes()), font.getSize());
        assertCalledMethods(FONT);
        assertNull(plainView.getFont());
        assertCalledMethods(NONE);
        assertSame(THE_FONT, plainWithParent.getFont());
        assertCalledMethods(AREA_FONT);
    }

    public void testIsUnderline() {
        assertFalse(styledView.isUnderline());
        assertFalse(styledWithParent.isUnderline());
        assertFalse(plainView.isUnderline());
        assertFalse(plainWithParent.isUnderline());
        setAttribute(StyleConstants.Underline, Boolean.TRUE);
        assertTrue(styledView.isUnderline());
        assertTrue(styledWithParent.isUnderline());
    }

    public void testIsStrikeThrough() {
        assertFalse(styledView.isStrikeThrough());
        assertFalse(styledWithParent.isStrikeThrough());
        assertFalse(plainView.isUnderline());
        assertFalse(plainWithParent.isStrikeThrough());
        setAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        assertTrue(styledView.isStrikeThrough());
        assertTrue(styledWithParent.isStrikeThrough());
    }

    public void testIsSubscript() {
        assertFalse(styledView.isSubscript());
        assertFalse(styledWithParent.isSubscript());
        assertFalse(plainView.isSubscript());
        assertFalse(plainWithParent.isSubscript());
        setAttribute(StyleConstants.Subscript, Boolean.TRUE);
        assertTrue(styledView.isSubscript());
        assertTrue(styledWithParent.isSubscript());
    }

    public void testIsSuperscript() {
        assertFalse(styledView.isSuperscript());
        assertFalse(styledWithParent.isSuperscript());
        assertFalse(plainView.isSuperscript());
        assertFalse(plainWithParent.isSuperscript());
        setAttribute(StyleConstants.Superscript, Boolean.TRUE);
        assertTrue(styledView.isSuperscript());
        assertTrue(styledWithParent.isSuperscript());
    }

    private void setAttribute(final Object key, final Object value) {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(key, value);
        styledDoc.setCharacterAttributes(leaf.getStartOffset(), leaf.getEndOffset()
                - leaf.getStartOffset(), attrs, false);
    }

    private void assertCalledMethods(final int index) {
        for (int i = 0; i < calledMethods.length; i++) {
            if (i == index) {
                assertTrue(PROPERTY_NAMES[i] + " isn't true", calledMethods[i]);
            } else {
                assertFalse(PROPERTY_NAMES[i] + " isn't false", calledMethods[i]);
            }
            calledMethods[i] = false;
        }
    }
}
