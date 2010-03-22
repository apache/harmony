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
 * @author Roman I. Chernyatchik
 */
package javax.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.util.Enumeration;
import javax.swing.JEditorPane.PlainEditorKit;
import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

//TODO: add multhithreaded tests for all thread-safe
public class JTextPaneTest extends SwingTestCase {
    private JTextPane textPane;

    private MutableAttributeSet attrs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        textPane = new JTextPane();
        // init character attributeSet
        attrs = new SimpleAttributeSet();
        StyleConstants.setStrikeThrough(attrs, true);
        StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_CENTER);
        StyleConstants.setUnderline(attrs, true);
        textPane.getDocument().insertString(0, "Hello  !", attrs);
        StyleConstants.setUnderline(attrs, false);
        textPane.getDocument().insertString(6, "world", attrs);
        textPane.getDocument().insertString(12, "\n World is beautifull!", attrs);
    }

    public void testJTextPane() {
        assertNotNull(textPane.getDocument());
        assertNotNull(textPane.getEditorKit());
    }

    public void testGetUIClassID() {
        assertNull(null);
        assertEquals("TextPaneUI", textPane.getUIClassID());
    }

    public void testSetDocument() {
        StyledDocument doc = new DefaultStyledDocument();
        textPane.setDocument(doc);
        assertSame(doc, textPane.getDocument());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                textPane.setDocument(new PlainDocument());
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                textPane.setDocument(null);
            }
        });
    }

    public void testSetStyledDocument() {
        StyledDocument doc = new DefaultStyledDocument();
        textPane.setDocument(doc);
        assertSame(doc, textPane.getDocument());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                textPane.setDocument(new PlainDocument());
            }
        });
    }

    public void testGetStyledEditorKit() {
        assertSame(textPane.getEditorKit(), textPane.getStyledEditorKit());
        assertSame(textPane.getStyledEditorKit(), textPane.getStyledEditorKit());
    }

    public void testCreateDefaultEditorKit() {
        EditorKit editorKit1 = textPane.createDefaultEditorKit();
        EditorKit editorKit2 = textPane.createDefaultEditorKit();
        assertNotSame(editorKit1, editorKit2);
        assertEquals("javax.swing.text.StyledEditorKit", editorKit1.getClass().getName());
    }

    public void testSetEditorKit() {
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                textPane.setEditorKit(new PlainEditorKit());
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                textPane.setEditorKit(null);
            }
        });
    }

    public void testGetStyle() {
        Style style;
        style = textPane.getStyle(StyleContext.DEFAULT_STYLE);
        assertEquals(StyleContext.DEFAULT_STYLE, style.getName());
        assertSame(textPane.getStyledDocument().getStyle(StyleContext.DEFAULT_STYLE), textPane
                .getStyle(StyleContext.DEFAULT_STYLE));
        textPane.addStyle("child", style);
        style = textPane.getStyle("child");
        assertEquals("child", style.getName());
        assertEquals(StyleContext.DEFAULT_STYLE, ((Style) style.getResolveParent()).getName());
        assertSame(textPane.getStyledDocument().getStyle("child"), textPane.getStyle("child"));
    }

    public void testAddStyle() {
        Style parent = textPane.addStyle("parent", null);
        Style child = textPane.addStyle("child", parent);
        assertEquals(1, parent.getAttributeCount());
        assertNull(parent.getResolveParent());
        assertEquals(2, child.getAttributeCount());
        assertNotNull(child.getResolveParent());
        parent.addAttribute(StyleConstants.Bold, Boolean.FALSE);
        child.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        assertFalse(((Boolean) parent.getAttribute(StyleConstants.Bold)).booleanValue());
        assertTrue(((Boolean) child.getAttribute(StyleConstants.Bold)).booleanValue());
        // Add styles with diff parameters
        Style parent1 = textPane.addStyle("p1", null);
        Style parent2 = textPane.addStyle("p2", null);
        Object[] styles = { null, null, "one", null, null, parent1, "two", parent2 };
        for (int i = 0; i < styles.length; i += 2) {
            Style style = textPane.addStyle((String) styles[i], (Style) styles[i + 1]);
            assertEquals("Iteration: " + i, (String) styles[i], style.getName());
            assertSame("Iteration: " + i, styles[i + 1], style.getResolveParent());
        }
        // unnamed style
        Style anotherChild = textPane.addStyle(null, parent);
        assertEquals(1, anotherChild.getAttributeCount());
        assertNotNull(anotherChild.getResolveParent());
        //not unique name of the style
        Style anotherStyle;
        anotherStyle = textPane.addStyle("child", null);
        assertNotSame(child, anotherStyle);
        assertNotNull(anotherStyle);
        anotherStyle = textPane.addStyle("child", parent);
        assertNotSame(child, anotherStyle);
        assertNotNull(anotherStyle);
    }

    public void testRemoveStyle() {
        Style parent = textPane.addStyle("parent", null);
        Style child = textPane.addStyle("child", parent);
        textPane.removeStyle("parent");
        assertNull(textPane.getStyle("parent"));
        assertEquals(2, child.getAttributeCount());
        assertNotNull(child.getResolveParent());
        assertNull(child.getAttribute("resolver"));
    }

    public void testGetLogicalStyle() throws BadLocationException {
        textPane.getStyledDocument().insertString(11, "bold", attrs);
        Style style = textPane.addStyle("bold", textPane.getStyle(StyleContext.DEFAULT_STYLE));
        textPane.setCaretPosition(1);
        style.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        textPane.setLogicalStyle(style);
        style = textPane.getLogicalStyle();
        textPane.setCaretPosition(3);
        assertSame(style, textPane.getLogicalStyle());
        assertTrue(((Boolean) style.getAttribute(StyleConstants.Bold)).booleanValue());
        attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, true);
        textPane.setParagraphAttributes(attrs, true);
        assertNull(textPane.getLogicalStyle());
    }

    public void testSetLogicalStyle() throws BadLocationException {
        // Set text
        attrs = textPane.getInputAttributes();
        attrs.removeAttributes(attrs.getAttributeNames());
        textPane.setText("");
        StyleConstants.setBold(attrs, true);
        textPane.getStyledDocument().insertString(0, "bold", attrs);
        StyleConstants.setBold(attrs, false);
        StyleConstants.setItalic(attrs, true);
        textPane.getStyledDocument().insertString(4, "italic\n", attrs);
        StyleConstants.setItalic(attrs, false);
        StyleConstants.setBold(attrs, true);
        // Add style
        Style style = textPane.addStyle("bold", textPane.getStyle(StyleContext.DEFAULT_STYLE));
        // Set style
        textPane.setCaretPosition(1);
        style.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        assertFalse(StyleConstants.isBold(textPane.getParagraphAttributes()));
        textPane.setLogicalStyle(style);
        style = textPane.getLogicalStyle();
        textPane.setCaretPosition(3);
        assertSame(style, textPane.getLogicalStyle());
        assertTrue(((Boolean) style.getAttribute(StyleConstants.Bold)).booleanValue());
        assertTrue(StyleConstants.isBold(textPane.getParagraphAttributes()));
        assertTrue(StyleConstants.isBold(getCharacterAttributes(1)));
        // Set paragraph attributes
        attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, true);
        textPane.setParagraphAttributes(attrs, true);
        assertNull(textPane.getLogicalStyle());
        // Set another style
        textPane.getStyledDocument().setCharacterAttributes(1, 1, attrs, true);
        assertFalse(StyleConstants.isUnderline(textPane.getParagraphAttributes()));
        assertFalse(StyleConstants.isUnderline(getCharacterAttributes(1)));
        style = textPane.addStyle("underline", textPane.getStyle(StyleContext.DEFAULT_STYLE));
        style.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        textPane.setLogicalStyle(style);
        assertNotNull(textPane.getLogicalStyle());
        assertEquals("underline", textPane.getLogicalStyle().getAttribute(
                StyleConstants.NameAttribute));
        assertTrue(StyleConstants.isUnderline(textPane.getParagraphAttributes()));
        assertTrue(StyleConstants.isUnderline(getCharacterAttributes(1)));
    }

    public void testGetParagraphAttributes() {
        // init paragraph attributeSet
        textPane.setCaretPosition(1);
        Element paragraph = textPane.getStyledDocument().getParagraphElement(
                textPane.getCaretPosition());
        attrs = new SimpleAttributeSet();
        StyleConstants.setStrikeThrough(attrs, true);
        StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_CENTER);
        textPane.getStyledDocument().setParagraphAttributes(paragraph.getStartOffset(),
                paragraph.getEndOffset() - paragraph.getStartOffset(), attrs, true);
        // tests
        AttributeSet textAttrs;
        textPane.setCaretPosition(1);
        textAttrs = textPane.getParagraphAttributes();
        assertFalse(StyleConstants.isUnderline(textAttrs));
        assertTrue(StyleConstants.isStrikeThrough(textAttrs));
        assertEquals(StyleConstants.ALIGN_CENTER, StyleConstants.getAlignment(textAttrs));
        textPane.setCaretPosition(8);
        textAttrs = textPane.getParagraphAttributes();
        assertFalse(StyleConstants.isUnderline(textAttrs));
        assertTrue(StyleConstants.isStrikeThrough(textAttrs));
        assertEquals(StyleConstants.ALIGN_CENTER, StyleConstants.getAlignment(textAttrs));
    }

    public void testSetParagraphAttributes() {
        StyledDocument doc = textPane.getStyledDocument();
        AttributeSet textAttrs;
        // The attributes are applied to the paragraph at the current caret
        // position.
        textPane.setCaretPosition(1);
        StyleConstants.setSubscript(attrs, false);
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
        textAttrs = textPane.getParagraphAttributes();
        assertFalse(StyleConstants.isSubscript(textAttrs));
        StyleConstants.setSubscript(attrs, true);
        textPane.setParagraphAttributes(attrs, true);
        textAttrs = textPane.getParagraphAttributes();
        assertTrue(StyleConstants.isSubscript(textAttrs));
        // The attributes are applied to the to the paragraphs that intersect
        // the selection
        textPane.select(1, 2);
        clearAndSetParagraphSubscript(doc);
        textAttrs = getParagraphAttributes(1);
        assertTrue(StyleConstants.isSubscript(textAttrs));
        textAttrs = getParagraphAttributes(18);
        assertFalse(StyleConstants.isSubscript(textAttrs));
        textPane.select(1, 13);
        clearAndSetParagraphSubscript(doc);
        textAttrs = getParagraphAttributes(1);
        assertTrue(StyleConstants.isSubscript(textAttrs));
        textAttrs = getParagraphAttributes(18);
        assertFalse(StyleConstants.isSubscript(textAttrs));
        textPane.select(1, 14);
        clearAndSetParagraphSubscript(doc);
        textAttrs = getParagraphAttributes(1);
        assertTrue(StyleConstants.isSubscript(textAttrs));
        textAttrs = getParagraphAttributes(18);
        assertTrue(StyleConstants.isSubscript(textAttrs));
        textPane.select(18, 19);
        clearAndSetParagraphSubscript(doc);
        textAttrs = getParagraphAttributes(1);
        assertFalse(StyleConstants.isSubscript(textAttrs));
        textAttrs = getParagraphAttributes(18);
        assertTrue(StyleConstants.isSubscript(textAttrs));
        textPane.select(13, 19);
        textPane.getUI().getRootView(textPane).getView(0);
        clearAndSetParagraphSubscript(doc);
        textAttrs = getParagraphAttributes(1);
        assertFalse(StyleConstants.isSubscript(textAttrs));
        textAttrs = getParagraphAttributes(18);
        assertTrue(StyleConstants.isSubscript(textAttrs));
        textPane.select(12, 19);
        clearAndSetParagraphSubscript(doc);
        textAttrs = getParagraphAttributes(1);
        assertTrue(StyleConstants.isSubscript(textAttrs));
        textAttrs = getParagraphAttributes(18);
        assertTrue(StyleConstants.isSubscript(textAttrs));
        textPane.select(1, 18);
        clearAndSetParagraphSubscript(doc);
        textAttrs = getParagraphAttributes(1);
        assertTrue(StyleConstants.isSubscript(textAttrs));
        textAttrs = getParagraphAttributes(18);
        assertTrue(StyleConstants.isSubscript(textAttrs));
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                textPane.setParagraphAttributes(null, true);
            }
        });
        textPane.select(1, 1);
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                textPane.setParagraphAttributes(null, true);
            }
        });
    }

    public void testGetCharacterAttributes() {
        AttributeSet textAttrs;
        textPane.setCaretPosition(1);
        textAttrs = textPane.getCharacterAttributes();
        assertTrue(StyleConstants.isUnderline(textAttrs));
        assertEquals(StyleConstants.ALIGN_CENTER, StyleConstants.getAlignment(textAttrs));
        assertTrue(StyleConstants.isStrikeThrough(textAttrs));
        textPane.setCaretPosition(8);
        textAttrs = textPane.getCharacterAttributes();
        assertFalse(StyleConstants.isUnderline(textAttrs));
        assertEquals(StyleConstants.ALIGN_CENTER, StyleConstants.getAlignment(textAttrs));
        assertTrue(StyleConstants.isStrikeThrough(textAttrs));
    }

    public void testSetCharacterAttributes() {
        StyledDocument doc = textPane.getStyledDocument();
        // The attributes are applied to the paragraph at the current caret
        // position.
        textPane.setCaretPosition(1);
        StyleConstants.setSubscript(textPane.getInputAttributes(), false);
        assertFalse(StyleConstants.isSubscript(textPane.getCharacterAttributes()));
        assertFalse(StyleConstants.isSubscript(textPane.getInputAttributes()));
        clearAndSetCharacterSubscript(doc);
        assertFalse(StyleConstants.isSubscript(textPane.getCharacterAttributes()));
        assertTrue(StyleConstants.isSubscript(textPane.getInputAttributes()));
        textPane.select(2, 1);
        StyleConstants.setSubscript(textPane.getInputAttributes(), false);
        assertFalse(StyleConstants.isSubscript(textPane.getInputAttributes()));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(1)));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(18)));
        clearAndSetCharacterSubscript(doc);
        assertTrue(StyleConstants.isSubscript(textPane.getInputAttributes()));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(1)));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(18)));
        // The attributes are applied to the to the paragraphs that intersect
        // the selection
        textPane.select(1, 2);
        StyleConstants.setSubscript(textPane.getInputAttributes(), false);
        assertFalse(StyleConstants.isSubscript(textPane.getInputAttributes()));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(1)));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(18)));
        clearAndSetCharacterSubscript(doc);
        assertFalse(StyleConstants.isSubscript(textPane.getInputAttributes()));
        assertTrue(StyleConstants.isSubscript(getCharacterAttributes(1)));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(18)));
        textPane.select(2, 13);
        StyleConstants.setSubscript(textPane.getInputAttributes(), false);
        assertFalse(StyleConstants.isSubscript(textPane.getInputAttributes()));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(2)));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(18)));
        clearAndSetCharacterSubscript(doc);
        assertFalse(StyleConstants.isSubscript(textPane.getInputAttributes()));
        assertTrue(StyleConstants.isSubscript(getCharacterAttributes(2)));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(18)));
        textPane.select(13, 19);
        StyleConstants.setSubscript(textPane.getInputAttributes(), false);
        assertFalse(StyleConstants.isSubscript(textPane.getInputAttributes()));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(1)));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(18)));
        clearAndSetCharacterSubscript(doc);
        assertFalse(StyleConstants.isSubscript(textPane.getInputAttributes()));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(1)));
        assertTrue(StyleConstants.isSubscript(getCharacterAttributes(18)));
        textPane.select(1, 18);
        StyleConstants.setSubscript(textPane.getInputAttributes(), false);
        assertFalse(StyleConstants.isSubscript(textPane.getInputAttributes()));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(1)));
        StyleConstants.setSubscript(textPane.getInputAttributes(), false);
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(19)));
        clearAndSetCharacterSubscript(doc);
        assertFalse(StyleConstants.isSubscript(textPane.getInputAttributes()));
        assertTrue(StyleConstants.isSubscript(getCharacterAttributes(1)));
        assertFalse(StyleConstants.isSubscript(getCharacterAttributes(19)));
    }

    public void testGetInputAttributes() {
        MutableAttributeSet inpAttr = ((StyledEditorKit) textPane.getEditorKit())
                .getInputAttributes();
        assertSame(textPane.getInputAttributes(), inpAttr);
    }

    public void testInsertComponent() {
        AttributeSet attributes;
        textPane.setCaretPosition(1);
        attrs = textPane.getInputAttributes();
        assertAttributes(attrs, false, false, true, false, false, true);
        textPane.insertComponent(new JButton("Format C:\\>"));
        assertAttributes(attrs, false, false, false, false, false, false);
        assertNull(StyleConstants.getComponent(attrs));
        attributes = textPane.getStyledDocument().getCharacterElement(1).getAttributes();
        assertAttributes(attributes, false, false, false, false, false, false);
        assertNotNull(StyleConstants.getComponent(attributes));
        attrs = new SimpleAttributeSet(attributes);
        StyleConstants.setUnderline(attrs, true);
        textPane.getStyledDocument().setCharacterAttributes(1, 1, attrs, true);
        textPane.setCaretPosition(1);
        assertAttributes(textPane.getInputAttributes(), false, false, true, false, false, true);
        textPane.select(2, 2);
        assertAttributes(textPane.getInputAttributes(), false, false, false, false, false, true);
        textPane.replaceSelection("*");
        attrs = textPane.getInputAttributes();
        assertAttributes(attrs, false, false, false, false, false, true);
        assertNull(StyleConstants.getComponent(attrs));
        attributes = textPane.getStyledDocument().getCharacterElement(2).getAttributes();
        assertAttributes(attributes, false, false, false, false, false, true);
        assertNull(StyleConstants.getComponent(attributes));
    }

    public void testInsertIcon() {
        textPane.setEditable(false);
        textPane.setCaretPosition(3);
        attrs = textPane.getInputAttributes();
        assertAttributes(attrs, false, false, true, false, false, true);
        textPane.insertIcon(new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.drawRect(x, y, getIconWidth(), getIconHeight());
            }

            public int getIconWidth() {
                return 20;
            }

            public int getIconHeight() {
                return 40;
            }
        });
        assertAttributes(attrs, false, false, false, false, false, false);
        Element iconElement = textPane.getStyledDocument().getDefaultRootElement()
                .getElement(0).getElement(1);
        AttributeSet attributes = iconElement.getAttributes();
        assertNotNull(attributes.getAttribute(StyleConstants.IconAttribute));
        assertAttributes(attributes, false, false, false, false, false, false);
    }

    public void testReplaceSelection() throws BadLocationException {
        AttributeSet textAttrs;
        // There is replacement and selection
        textPane.select(6, 22);
        textAttrs = getCharacterAttributes(6);
        assertTrue(StyleConstants.isUnderline(getCharacterAttributes(5)));
        assertFalse(StyleConstants.isUnderline(getCharacterAttributes(6)));
        textPane.replaceSelection("_BUGGGGS_are_");
        assertFalse(StyleConstants.isUnderline(getCharacterAttributes(6)));
        compareAttributes(textAttrs, 6, 18);
        assertEquals("Hello _BUGGGGS_are_ beautifull!!", textPane.getText());
        // There is replacement and no selection
        textPane.setCaretPosition(1);
        textPane.select(6, 6);
        textPane.replaceSelection("_BUGGGGS!_The");
        assertEquals("Hello _BUGGGGS!_The_BUGGGGS_are_ beautifull!!", textPane.getText());
        textPane.setCaretPosition(3);
        textPane.setCaretPosition(6);
        textPane.replaceSelection("q");
        assertEquals(textPane.getStyledDocument().getCharacterElement(6).getAttributes(),
                textPane.getStyledDocument().getCharacterElement(7).getAttributes());
        textPane.select(2, 3);
        textPane.replaceSelection("");
        // There is selection and no replacement
        textPane.select(0, 6);
        textPane.replaceSelection("");
        assertEquals("_BUGGGGS!_The_BUGGGGS_are_ beautifull!!", textPane.getText());
        textPane.select(1, 27);
        textPane.replaceSelection("");
        assertEquals("_beautifull!!", textPane.getText());
        // There is no selection and no replacement
        textPane.select(1, 1);
        textPane.replaceSelection("");
        //Document is not editable
        textPane.setEditable(false);
        textPane.select(1, 1);
        textPane.replaceSelection("Hello");
        assertEquals("_beautifull!!", textPane.getText());
        textPane.select(2, 4);
        textPane.replaceSelection("Hello");
        assertEquals("_beautifull!!", textPane.getText());
        textPane = new JTextPane();
        textPane.replaceSelection("1");
        assertEquals(textPane.getStyledDocument().getCharacterElement(0).getAttributes(),
                textPane.getStyledDocument().getCharacterElement(1).getAttributes());
        textPane.select(0, 1);
        textPane.replaceSelection("");
        attrs = new SimpleAttributeSet();
        StyleConstants.setUnderline(attrs, true);
        textPane.getStyledDocument().insertString(0, "Hello!", attrs);
        textPane.select(0, 0);
        textPane.replaceSelection("1");
        assertEquals(textPane.getStyledDocument().getCharacterElement(0).getAttributes(),
                textPane.getStyledDocument().getCharacterElement(1).getAttributes());
        textPane.select(0, 1);
        textPane.replaceSelection("2");
        assertEquals(textPane.getStyledDocument().getCharacterElement(0).getAttributes(),
                textPane.getStyledDocument().getCharacterElement(1).getAttributes());
        textPane.setCaretPosition(1);
        textPane.insertIcon(MetalIconFactory.getFileChooserNewFolderIcon());
        textPane.select(2, 2);
        textPane.replaceSelection("3");
        attrs = new SimpleAttributeSet(textPane.getStyledDocument().getCharacterElement(1)
                .getAttributes());
        assertAttributes(attrs, false, false, false, false, false, false);
        assertNotNull(StyleConstants.getIcon(attrs));
        attrs = new SimpleAttributeSet(textPane.getStyledDocument().getCharacterElement(2)
                .getAttributes());
        assertAttributes(attrs, false, false, false, false, false, false);
        assertNull(StyleConstants.getIcon(attrs));
    }

    public void testParamString() {
        String tmp = textPane.paramString();
        assertNotNull(tmp);
    }

    private AttributeSet getParagraphAttributes(final int position) {
        return textPane.getStyledDocument().getParagraphElement(position).getAttributes();
    }

    private AttributeSet getCharacterAttributes(final int position) {
        return textPane.getStyledDocument().getCharacterElement(position).getAttributes();
    }

    private void clearAndSetParagraphSubscript(final StyledDocument doc) {
        StyleConstants.setSubscript(attrs, false);
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
        StyleConstants.setSubscript(attrs, true);
        textPane.setParagraphAttributes(attrs, true);
    }

    private void clearAndSetCharacterSubscript(final StyledDocument doc) {
        StyleConstants.setSubscript(attrs, false);
        doc.setCharacterAttributes(0, doc.getLength(), attrs, false);
        StyleConstants.setSubscript(attrs, true);
        textPane.setCharacterAttributes(attrs, true);
    }

    private void compareAttributes(final AttributeSet textAttrs, final int startOffset,
            final int endOffset) {
        for (int i = startOffset; i < endOffset + 1; i++) {
            compareAttributeSets(textAttrs, getCharacterAttributes(i));
        }
    }

    private void compareAttributeSets(final AttributeSet expectedTextAttrs,
            final AttributeSet textAttrs) {
        assertEquals(expectedTextAttrs.getAttributeCount(), textAttrs.getAttributeCount());
        for (Enumeration<?> expectedNames = expectedTextAttrs.getAttributeNames(); expectedNames
                .hasMoreElements();) {
            Object expectedName = expectedNames.nextElement();
            assertEquals(expectedTextAttrs.getAttribute(expectedName), textAttrs
                    .getAttribute(expectedName));
        }
        for (Enumeration<?> names = textAttrs.getAttributeNames(); names.hasMoreElements();) {
            Object name = names.nextElement();
            assertEquals(textAttrs.getAttribute(name), expectedTextAttrs.getAttribute(name));
        }
    }

    private void assertAttributes(final AttributeSet attrs, final boolean isBold,
            final boolean isItalic, final boolean isStrikeThrough, final boolean isSubscript,
            final boolean isSuperScript, final boolean isUnderline) {
        assertEquals(isBold, StyleConstants.isBold(attrs));
        assertEquals(isItalic, StyleConstants.isItalic(attrs));
        assertEquals(isStrikeThrough, StyleConstants.isStrikeThrough(attrs));
        assertEquals(isSubscript, StyleConstants.isSubscript(attrs));
        assertEquals(isSuperScript, StyleConstants.isSuperscript(attrs));
        assertEquals(isUnderline, StyleConstants.isUnderline(attrs));
    }

    public void testConstructor() {
        try {      
            new JTextPane(null); 
            fail("NPE should be thrown");
        } catch (NullPointerException npe) {              
            // PASSED            
        }
    }
}