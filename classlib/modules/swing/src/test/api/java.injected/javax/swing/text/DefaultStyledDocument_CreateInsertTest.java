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

import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.DefaultStyledDocument.AttributeUndoableEdit;
import javax.swing.text.DefaultStyledDocument.ElementBuffer;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import junit.framework.TestCase;

/**
 * Tests <code>create()</code> and <code>insert()</code> methods of
 * DefaultStyledDocument.
 *
 */
public class DefaultStyledDocument_CreateInsertTest extends TestCase implements
        DocumentListener {
    private DefaultStyledDocument doc;

    private ElementBuffer buf;

    private Element root;

    private DocumentEvent event;

    private static final String plainText = "plain";

    private static final String boldText = "bold";

    private static final String italicText = "italic";

    private static final String newLine = "\n";

    private static final String paragraph2Text = "line\n";

    private static final AttributeSet bold = DefStyledDoc_Helpers.bold;

    private static final AttributeSet italic = DefStyledDoc_Helpers.italic;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefStyledDoc_Helpers.DefStyledDocWithLogging();
        root = doc.getDefaultRootElement();
        buf = new DefStyledDoc_Helpers.ElementBufferWithLogging(doc, root);
        doc.buffer = buf;
    }

    /**
     * Calls <code>create()</code> on empty document.
     * The attributes of the default root modified before the call to make
     * sure they are reset on call.
     */
    public void testCreateWithoutText() throws BadLocationException {
        final ElementSpec[] specs = {
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, plainText.toCharArray(), 0,
                        plainText.length()),
                new ElementSpec(bold, ElementSpec.ContentType, boldText.toCharArray(), 0,
                        boldText.length()),
                new ElementSpec(italic, ElementSpec.ContentType, italicText.toCharArray(), 0,
                        italicText.length()),
                new ElementSpec(null, ElementSpec.ContentType, newLine.toCharArray(), 0,
                        newLine.length()),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, paragraph2Text.toCharArray(), 0,
                        paragraph2Text.length()),
                new ElementSpec(null, ElementSpec.EndTagType), };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        // Add an attribute to the root
        doc.writeLock();
        try {
            ((AbstractElement) root).addAttributes(bold);
        } finally {
            doc.writeUnlock();
        }
        doc.addDocumentListener(this);
        assertNull(event);
        doc.create(specs);
        assertNotNull(event);
        assertEquals(0, event.getOffset());
        assertEquals(plainText.length() + boldText.length() + italicText.length()
                + newLine.length() + paragraph2Text.length(), event.getLength());
        assertSame(EventType.INSERT, event.getType());
        assertChildren(root.getElement(0), plainText + boldText + italicText + newLine,
                new int[] { 0, 5, 5, 9, 9, 15, 15, 16 }, new AttributeSet[] { null, bold,
                        italic, null });
        assertChildren(root.getElement(1), paragraph2Text, new int[] { 16, 21 },
                new AttributeSet[] { null });
        assertEquals(0, root.getElement(1).getAttributes().getAttributeCount());
        assertChildren(root.getElement(2), newLine, new int[] { 21, 22 },
                new AttributeSet[] { null });
        final List<?> edits = DefStyledDoc_Helpers.getEdits((DefaultDocumentEvent) event);
        assertEquals(6, edits.size());
        final AttributeUndoableEdit undo = (AttributeUndoableEdit) edits.get(1);
        assertSame(root, undo.element);
        assertTrue(undo.copy.isEqual(bold));
        assertEquals(0, undo.newAttributes.getAttributeCount());
        assertTrue(undo.isReplacing);
        assertChange(edits.get(2), root.getElement(root.getElementCount() - 1), 0, new int[] {
                0, 22 }, new int[] { 21, 22 });
        assertChange(edits.get(3), root.getElement(0), 0, new int[] {}, new int[] { 0, 5, 5, 9,
                9, 15, 15, 16 });
        assertChange(edits.get(4), root.getElement(1), 0, new int[] {}, new int[] { 16, 21 });
        assertChange(edits.get(5), root, 0, new int[] {}, new int[] { 0, 16, 16, 21 });
    }

    /**
     * Calls <code>create</code> on non-empty document.
     * This asserts the document contents is removed before applying
     * new structure.
     */
    public void testCreateWithText() throws BadLocationException {
        doc.insertString(0, "some text", bold);
        doc.insertString(doc.getLength(), "\nmore text", italic);
        final ElementSpec[] specs = {
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, plainText.toCharArray(), 0,
                        plainText.length()),
                new ElementSpec(bold, ElementSpec.ContentType, boldText.toCharArray(), 0,
                        boldText.length()),
                new ElementSpec(italic, ElementSpec.ContentType, italicText.toCharArray(), 0,
                        italicText.length()),
                new ElementSpec(null, ElementSpec.ContentType, newLine.toCharArray(), 0,
                        newLine.length()),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, paragraph2Text.toCharArray(), 0,
                        paragraph2Text.length()),
                new ElementSpec(null, ElementSpec.EndTagType), };
        doc.addDocumentListener(this);
        assertNull(event);
        doc.create(specs);
        assertNotNull(event);
        assertEquals(0, event.getOffset());
        assertEquals(plainText.length() + boldText.length() + italicText.length()
                + newLine.length() + paragraph2Text.length(), event.getLength());
        assertSame(EventType.INSERT, event.getType());
        assertChildren(root.getElement(0), plainText + boldText + italicText + newLine,
                new int[] { 0, 5, 5, 9, 9, 15, 15, 16 }, new AttributeSet[] { null, bold,
                        italic, null });
        assertChildren(root.getElement(1), paragraph2Text, new int[] { 16, 21 },
                new AttributeSet[] { null });
        assertEquals(0, root.getElement(1).getAttributes().getAttributeCount());
        assertChildren(root.getElement(2), newLine, new int[] { 21, 22 },
                new AttributeSet[] { null });
        final List<?> edits = DefStyledDoc_Helpers.getEdits((DefaultDocumentEvent) event);
        assertEquals(6, edits.size());
        assertChange(edits.get(2), root.getElement(root.getElementCount() - 1), 0, new int[] {
                0, 22 }, new int[] { 21, 22 });
        assertChange(edits.get(3), root.getElement(0), 0, new int[] {}, new int[] { 0, 5, 5, 9,
                9, 15, 15, 16 });
        assertChange(edits.get(4), root.getElement(1), 0, new int[] {}, new int[] { 16, 21 });
        assertChange(edits.get(5), root, 0, new int[] {}, new int[] { 0, 16, 16, 21 });
    }

    public void testInsertWithStartTag() throws BadLocationException {
        doc.insertString(0, "some  text", bold);
        doc.insertString(doc.getLength(), "\nmore text", italic);
        ElementSpec[] specs = {
                new ElementSpec(null, ElementSpec.ContentType, plainText.toCharArray(), 0,
                        plainText.length()),
                new ElementSpec(bold, ElementSpec.ContentType, boldText.toCharArray(), 0,
                        boldText.length()),
                new ElementSpec(italic, ElementSpec.ContentType, italicText.toCharArray(), 0,
                        italicText.length()),
                new ElementSpec(null, ElementSpec.ContentType, newLine.toCharArray(), 0,
                        newLine.length()),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, paragraph2Text.toCharArray(), 0,
                        paragraph2Text.length()),
                //It is mandatory for our implementation.
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[specs.length - 1].setDirection(ElementSpec.JoinFractureDirection);
        doc.addDocumentListener(this);
        assertNull(event);
        doc.insert(5, specs);
        assertNotNull(event);
        assertEquals(5, event.getOffset());
        assertEquals(plainText.length() + boldText.length() + italicText.length()
                + newLine.length() + paragraph2Text.length(), event.getLength());
        assertSame(EventType.INSERT, event.getType());
        assertChildren(root.getElement(0), "some " + plainText + boldText + italicText
                + newLine, new int[] { 0, 5, 5, 10, 10, 14, 14, 20, 20, 21 },
                new AttributeSet[] { bold, null, bold, italic, null });
        assertChildren(root.getElement(1), paragraph2Text, new int[] { 21, 26 },
                new AttributeSet[] { null });
        assertEquals(0, root.getElement(1).getAttributes().getAttributeCount());
        assertChildren(root.getElement(2), " text\n", new int[] { 26, 31, 31, 32 },
                new AttributeSet[] { bold, italic });
        assertChildren(root.getElement(3), "more text\n", new int[] { 32, 41, 41, 42 },
                new AttributeSet[] { italic, null });
    }

    /**
     * Calls <code>insert()</code> on non-empty document.
     * The <code>ElementSpec</code>s applied contain no
     * <code>StartTagType</code> specs.
     */
    public void testInsertNoStartTags() throws BadLocationException {
        doc.insertString(0, "some  text", bold);
        doc.insertString(doc.getLength(), "\nmore text", italic);
        ElementSpec[] specs = {
                new ElementSpec(null, ElementSpec.ContentType, plainText.toCharArray(), 0,
                        plainText.length()),
                new ElementSpec(bold, ElementSpec.ContentType, boldText.toCharArray(), 0,
                        boldText.length()),
                new ElementSpec(italic, ElementSpec.ContentType, italicText.toCharArray(), 0,
                        italicText.length()),
                new ElementSpec(null, ElementSpec.ContentType, newLine.toCharArray(), 0,
                        newLine.length()),
                new ElementSpec(null, ElementSpec.ContentType, paragraph2Text.toCharArray(), 0,
                        paragraph2Text.length()), };
        doc.addDocumentListener(this);
        assertNull(event);
        doc.insert(5, specs);
        assertNotNull(event);
        assertEquals(5, event.getOffset());
        assertEquals(plainText.length() + boldText.length() + italicText.length()
                + newLine.length() + paragraph2Text.length(), event.getLength());
        assertSame(EventType.INSERT, event.getType());
        assertChildren(root.getElement(0), "some " + plainText + boldText + italicText
                + newLine + paragraph2Text + " text\n", new int[] { 0, 5, 5, 10, 10, 14, 14,
                20, 20, 21, 21, 26, 26, 31, 31, 32 }, new AttributeSet[] { bold, null, bold,
                italic, null, null, bold, italic });
        assertChildren(root.getElement(1), "more text\n", new int[] { 32, 41, 41, 42 },
                new AttributeSet[] { italic, null });
    }

    private static void assertChange(final Object change, final Element element,
            final int index, final int[] removedOffsets, final int[] addedOffsets) {
        DefStyledDoc_Helpers.assertChange(change, element, index, removedOffsets, addedOffsets);
    }

    private static void assertChildren(final Element element, final String text,
            final int[] offsets, final AttributeSet[] attributes) throws BadLocationException {
        DefStyledDoc_Helpers.assertChildren(element, offsets, attributes);
        assertEquals("element.text " + element, text, getText(element));
    }

    private static String getText(final Element element) throws BadLocationException {
        final int start = element.getStartOffset();
        final int end = element.getEndOffset();
        return element.getDocument().getText(start, end - start);
    }

    public void insertUpdate(DocumentEvent e) {
        event = e;
    }

    public void removeUpdate(DocumentEvent e) {
    }

    public void changedUpdate(DocumentEvent e) {
        fail("changedUpdate is not expected to be called");
    }
}
