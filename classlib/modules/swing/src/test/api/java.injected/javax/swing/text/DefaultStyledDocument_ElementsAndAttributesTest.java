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
import javax.swing.BasicSwingTestCase;
import javax.swing.text.AbstractDocument.AbstractElement;
import junit.framework.TestCase;

/**
 * Tests methods of DefaultStyledDocument which deal with getting paragraph
 * and character elements, setting their attributes and logical styles.
 *
 */
public class DefaultStyledDocument_ElementsAndAttributesTest extends TestCase {
    private DefaultStyledDocument doc;

    private Element root;

    private StyleContext styles;

    private AttributeSet plain;

    private AttributeSet bold;

    private AttributeSet italic;

    private AttributeSet boldItalic;

    private AttributeSet background;

    private AttributeSet foreground;

    private static final class StyledText {
        public final String text;

        public final AttributeSet attr;

        public StyledText(final String text, final AttributeSet attr) {
            this.text = text;
            this.attr = attr;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        styles = new StyleContext();
        doc = new DefaultStyledDocument(styles);
        root = doc.getDefaultRootElement();
        initAttributes();
        initText();
    }

    private void initAttributes() {
        plain = styles.getEmptySet();
        bold = styles.addAttribute(plain, StyleConstants.Bold, Boolean.TRUE);
        italic = styles.addAttribute(plain, StyleConstants.Italic, Boolean.TRUE);
        boldItalic = styles.addAttribute(bold, StyleConstants.Italic, Boolean.TRUE);
        foreground = background = styles.getEmptySet();
        foreground = styles.addAttribute(foreground, StyleConstants.Foreground, Color.CYAN);
        background = styles.addAttribute(background, StyleConstants.Background, Color.BLUE);
    }

    private void initText() throws BadLocationException {
        final StyledText[] styledText = new StyledText[] {
                // First paragraph
                new StyledText("plain", plain),
                new StyledText("bold", bold),
                new StyledText("italic\n", italic),
                // Second and Third
                new StyledText("Bold & Italic", boldItalic),
                new StyledText(" & Plain\n", plain),
                new StyledText("The very plain text", null) };
        int offset = 0;
        for (int i = 0; i < styledText.length; i++) {
            doc.insertString(offset, styledText[i].text, styledText[i].attr);
            offset += styledText[i].text.length();
        }
    }

    /**
     * Tests getting the first paragraph.
     */
    public void testGetParagraphElement01() {
        final Element par = doc.getParagraphElement(5);
        assertSame(root.getElement(0), par);
        assertFalse(par.isLeaf());
        assertEquals(3, par.getElementCount());
        assertEquals(0, par.getStartOffset());
        assertEquals(16, par.getEndOffset());
    }

    /**
     * Tests getting the second paragraph by its start offset.
     */
    public void testGetParagraphElement02() {
        final Element par = doc.getParagraphElement(16);
        assertSame(root.getElement(1), par);
        assertEquals(2, par.getElementCount());
    }

    /**
     * Tests getting paragraph at invalid offset (to the left).
     */
    public void testGetParagraphElement03() {
        final Element par = doc.getParagraphElement(-1);
        assertSame(root.getElement(0), par);
        assertEquals(3, par.getElementCount());
    }

    /**
     * Tests getting paragraph at invalid offset (to the right).
     */
    public void testGetParagraphElement04() {
        final Element par = doc.getParagraphElement(doc.getLength() + 2);
        assertSame(root.getElement(root.getElementCount() - 1), par);
        assertEquals(BasicSwingTestCase.isHarmony() ? 1 : 2, par.getElementCount());
    }

    /**
     * Tests method getCharacterElement on the first leaf
     * of the first paragraph.
     */
    public void testGetCharacterElement01() {
        final Element par = doc.getParagraphElement(0);
        final Element chars = doc.getCharacterElement(2);
        assertSame(par.getElement(0), chars);
        assertTrue(chars.isLeaf());
        assertEquals(0, chars.getStartOffset());
        assertEquals(5, chars.getEndOffset());
    }

    /**
     * Tests method getCharacterElement on the second leaf
     * of the first paragraph.
     */
    public void testGetCharacterElement02() {
        final Element par = doc.getParagraphElement(0);
        final Element chars = doc.getCharacterElement(5);
        assertSame(par.getElement(1), chars);
        assertEquals(5, chars.getStartOffset());
        assertEquals(9, chars.getEndOffset());
    }

    /**
     * Tests method getCharacterElement on the third (last) leaf
     * of the first paragraph.
     */
    public void testGetCharacterElement03() {
        final Element par = doc.getParagraphElement(0);
        final Element chars = doc.getCharacterElement(15);
        assertSame(par.getElement(2), chars);
        assertEquals(9, chars.getStartOffset());
        assertEquals(16, chars.getEndOffset());
    }

    /**
     * Tests method getCharacterElement with invalid offset (-1).
     */
    public void testGetCharacterElement04() {
        final Element par = root.getElement(0);
        final Element chars = doc.getCharacterElement(-1);
        assertSame(par.getElement(0), chars);
    }

    /**
     * Tests method getCharacterElement with invalid offset (length + 2).
     */
    public void testGetCharacterElement05() {
        final Element par = root.getElement(root.getElementCount() - 1);
        final Element chars = doc.getCharacterElement(doc.getLength() + 2);
        assertSame(par.getElement(par.getElementCount() - 1), chars);
    }

    /**
     * General checks for getLogicalStyle().
     */
    public void testGetLogicalStyle01() {
        final StyleContext styles = (StyleContext) doc.getAttributeContext();
        assertSame(styles.getStyle(StyleContext.DEFAULT_STYLE), doc.getLogicalStyle(5));
        final Element par = doc.getParagraphElement(5);
        final AttributeSet parAttrs = par.getAttributes();
        assertSame(parAttrs.getAttribute(AttributeSet.ResolveAttribute), doc.getLogicalStyle(5));
    }

    /**
     * Checks that logical style is ResolveAttribute of paragraph.
     */
    public void testGetLogicalStyle02() {
        final StyleContext context = new StyleContext();
        final Style style = context.addStyle("aStyle", null);
        final AbstractElement par = (AbstractElement) doc.getParagraphElement(5);
        doc.writeLock();
        try {
            par.addAttribute(AttributeSet.ResolveAttribute, style);
            assertSame(style, doc.getLogicalStyle(5));
        } finally {
            doc.writeUnlock();
        }
    }

    /**
     * Checks what happens when ResolveAttribute of a paragraph is set
     * to non-Style object.
     */
    public void testGetLogicalStyle03() {
        final AbstractElement par = (AbstractElement) doc.getParagraphElement(5);
        final MutableAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, Color.GREEN);
        StyleConstants.setBackground(set, Color.YELLOW);
        doc.writeLock();
        try {
            set.setResolveParent(boldItalic);
            par.setResolveParent(set);
            assertSame(set, par.getResolveParent());
            assertNull(doc.getLogicalStyle(5));
        } finally {
            doc.writeUnlock();
        }
    }

    /**
     * Checks what happens when getLogicalStyle is called with invalid offset.
     */
    public void testGetLogicalStyle04() {
        final Style defaultStyle = doc.getStyle(StyleContext.DEFAULT_STYLE);
        assertSame(defaultStyle, doc.getLogicalStyle(-2));
        assertSame(defaultStyle, doc.getLogicalStyle(doc.getLength() + 2));
    }

    /**
     * Adding character attributes. Tests attribute sets of the elements.
     */
    public void testSetCharacterAttributes01() {
        doc.setCharacterAttributes(3, 8, foreground, false);
        final Element par = doc.getParagraphElement(0);
        assertEquals(5, par.getElementCount());
        assertEquals(plain, par.getElement(0).getAttributes());
        assertEquals(foreground, par.getElement(1).getAttributes());
        assertEquals(styles.addAttributes(bold, foreground), par.getElement(2).getAttributes());
        assertEquals(styles.addAttributes(italic, foreground), par.getElement(3)
                .getAttributes());
        assertEquals(italic, par.getElement(4).getAttributes());
    }

    /**
     * Adding character attributes. Tests the resulting offsets of
     * the elements.
     */
    public void testSetCharacterAttributes02() {
        doc.setCharacterAttributes(3, 8, foreground, false);
        final Element par = doc.getParagraphElement(0);
        assertEquals(5, par.getElementCount());
        Element span = par.getElement(0); // Plain (no attributes)
        assertEquals(0, span.getStartOffset());
        assertEquals(3, span.getEndOffset());
        span = par.getElement(1); // Foreground only
        assertEquals(3, span.getStartOffset());
        assertEquals(5, span.getEndOffset());
        span = par.getElement(2); // Bold + Foreground
        assertEquals(5, span.getStartOffset());
        assertEquals(9, span.getEndOffset());
        span = par.getElement(3); // Italic + Foreground
        assertEquals(9, span.getStartOffset());
        assertEquals(11, span.getEndOffset());
        span = par.getElement(4); // Italic only
        assertEquals(11, span.getStartOffset());
        assertEquals(16, span.getEndOffset());
    }

    /**
     * Replacing character attributes. Tests attribute sets of the elements.
     *
     * It seems like the elements with equal attribute sets should be combined.
     */
    public void testSetCharacterAttributes03() {
        doc.setCharacterAttributes(3, 8, background, true);
        final Element par = doc.getParagraphElement(0);
        assertEquals(5, par.getElementCount());
        assertEquals(plain, par.getElement(0).getAttributes());
        assertEquals(background, par.getElement(1).getAttributes());
        assertEquals(background, par.getElement(2).getAttributes());
        assertEquals(background, par.getElement(3).getAttributes());
        assertEquals(italic, par.getElement(4).getAttributes());
    }

    /**
     * Replacing character attributes. Tests the resulting offsets of
     * the elements.
     */
    public void testSetCharacterAttributes04() {
        doc.setCharacterAttributes(3, 8, background, true);
        final Element par = doc.getParagraphElement(0);
        assertEquals(5, par.getElementCount());
        Element span = par.getElement(0); // Plain (no attributes)
        assertEquals(0, span.getStartOffset());
        assertEquals(3, span.getEndOffset());
        span = par.getElement(1); // Background only
        assertEquals(3, span.getStartOffset());
        assertEquals(5, span.getEndOffset());
        span = par.getElement(2); // Background only
        assertEquals(5, span.getStartOffset());
        assertEquals(9, span.getEndOffset());
        span = par.getElement(3); // Background only
        assertEquals(9, span.getStartOffset());
        assertEquals(11, span.getEndOffset());
        span = par.getElement(4); // Italic only
        assertEquals(11, span.getStartOffset());
        assertEquals(16, span.getEndOffset());
    }

    /**
     * Adds character attributes with <code>null</code> attributes.
     * <code>replace</code> is <code>false</code>.
     */
    public void testSetCharacterAttributes05() {
        try {
            doc.setCharacterAttributes(3, 8, null, false);
            fail("NullPointerException is expected.");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Adds character attributes with <code>null</code> attributes.
     * <code>replace</code> is <code>true</code>.
     */
    public void testSetCharacterAttributes06() {
        try {
            doc.setCharacterAttributes(3, 8, null, true);
            fail("NullPointerException is expected.");
        } catch (NullPointerException e) {
        }
    }

    public void testSetLogicalStyle() {
        final StyleContext context = new StyleContext();
        final Style logicalStyle = context.addStyle("aStyle", null);
        logicalStyle.addAttribute(StyleConstants.Foreground, Color.CYAN);
        logicalStyle.addAttribute(StyleConstants.Background, Color.BLUE);
        doc.setLogicalStyle(16, logicalStyle);
        assertNotSame(logicalStyle, doc.getLogicalStyle(15)); // 1st par end - 1
        assertSame(logicalStyle, doc.getLogicalStyle(16)); // 2nd par start
        assertSame(logicalStyle, doc.getLogicalStyle(37)); // 2nd par end - 1
        assertNotSame(logicalStyle, doc.getLogicalStyle(38)); // 3rd par start
        assertSame(doc.getStyle(StyleContext.DEFAULT_STYLE), doc.getLogicalStyle(38));
    }

    public void testSetLogicalStyleInvalid() {
        final StyleContext context = new StyleContext();
        final Style logicalStyle = context.addStyle("aStyle", null);
        logicalStyle.addAttribute(StyleConstants.Foreground, Color.CYAN);
        logicalStyle.addAttribute(StyleConstants.Background, Color.BLUE);
        final Style defaultStyle = doc.getStyle(StyleContext.DEFAULT_STYLE);
        doc.setLogicalStyle(-2, logicalStyle);
        assertSame(logicalStyle, doc.getLogicalStyle(0)); // 1st par end - 1
        assertSame(defaultStyle, doc.getLogicalStyle(16)); // 2nd par start
        assertSame(defaultStyle, doc.getLogicalStyle(37)); // 2nd par end - 1
        assertSame(defaultStyle, doc.getLogicalStyle(38));
        doc.setLogicalStyle(doc.getLength() + 2, logicalStyle);
        assertSame(logicalStyle, doc.getLogicalStyle(doc.getLength()));
    }

    /**
     * Adds paragraph attributes to only one paragraph.
     */
    public void testSetParagraphAttributes01() {
        doc.setParagraphAttributes(3, 13, foreground, false);
        assertFalse(doc.getLogicalStyle(3).containsAttributes(foreground));
        // The paragraph itself contains attributes, but not its resolve parent
        assertTrue(root.getElement(0).getAttributes().containsAttributes(foreground));
        assertFalse(root.getElement(1).getAttributes().containsAttributes(foreground));
        assertFalse(root.getElement(2).getAttributes().containsAttributes(foreground));
    }

    /**
     * Adds paragraph attributes to only one paragraph.
     */
    public void testSetParagraphAttributes02() {
        doc.setParagraphAttributes(3, 14, foreground, false);
        assertFalse(doc.getLogicalStyle(3).containsAttributes(foreground));
        assertFalse(doc.getLogicalStyle(3 + 14).containsAttributes(foreground));
        assertTrue(root.getElement(0).getAttributes().containsAttributes(foreground));
        assertTrue(root.getElement(1).getAttributes().containsAttributes(foreground));
        assertFalse(root.getElement(2).getAttributes().containsAttributes(foreground));
    }

    /**
     * Tests setParagraphAttributes when <code>replace</code> is
     * <code>true</code>.
     */
    public void testSetParagraphAttributes03() {
        doc.setParagraphAttributes(3, 1, foreground, false);
        doc.setParagraphAttributes(3, 1, background, true);
        // The attributes fully replaced including resolve parent
        assertNull(doc.getLogicalStyle(3));
        assertEquals(background, root.getElement(0).getAttributes());
        final Style defaultStyle = doc.getStyle(StyleContext.DEFAULT_STYLE);
        assertSame(defaultStyle, doc.getLogicalStyle(16));
        assertSame(defaultStyle, doc.getLogicalStyle(38));
    }

    /**
     * Tests setParagraphAttributes with null argument.
     */
    public void testSetParagraphAttributes04() {
        try {
            doc.setParagraphAttributes(3, 1, null, false);
            fail("NullPointerException is expected");
        } catch (NullPointerException e) {
        }
    }
}
/* The dump of the document used in these tests (after setUp()).

 <section>
 <paragraph
 resolver=NamedStyle:default {name=default,}
 >
 <content>
 [0,5][plain]
 <content
 bold=true
 >
 [5,9][bold]
 <content
 italic=true
 >
 [9,16][italic
 ]
 <paragraph
 resolver=NamedStyle:default {name=default,}
 >
 <content
 bold=true
 italic=true
 >
 [16,29][Bold & Italic]
 <content>
 [29,38][ & Plain
 ]
 <paragraph
 resolver=NamedStyle:default {name=default,}
 >
 <content>
 [38,57][The very plain text]
 <content>
 [57,58][
 ]
 <bidi root>
 <bidi level
 bidiLevel=0
 >
 [0,58][plainbolditalic
 Bold & Italic & Plain
 Th...]

 */
