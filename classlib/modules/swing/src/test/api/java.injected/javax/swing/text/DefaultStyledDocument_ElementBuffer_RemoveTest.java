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
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.Content;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.DefaultStyledDocument.ElementBuffer;
import junit.framework.TestCase;

/**
 * Tests the behavior of
 * <code>DefaultStyledDocument.ElementBuffer.remove()</code> method.
 *
 */
public class DefaultStyledDocument_ElementBuffer_RemoveTest extends TestCase {
    private DefaultStyledDocument doc;

    private ElementBuffer buf;

    private Element root;

    private Element paragraph;

    private Content content;

    private DefaultDocumentEvent event;

    private static final AttributeSet bold = DefStyledDoc_Helpers.bold;

    private static final AttributeSet italic = DefStyledDoc_Helpers.italic;

    private static final AttributeSet boldFalse;

    private static final AttributeSet italicFalse;
    static {
        MutableAttributeSet mas = new SimpleAttributeSet(bold);
        StyleConstants.setBold(mas, false);
        boldFalse = mas;
        mas = new SimpleAttributeSet(italic);
        StyleConstants.setItalic(mas, false);
        italicFalse = mas;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        root = doc.getDefaultRootElement();
        buf = new DefStyledDoc_Helpers.ElementBufferWithLogging(doc, root);
        doc.buffer = buf;
        paragraph = root.getElement(0);
        content = doc.getContent();
        content.insertString(0, "plainbolditalic\ntext");
        // Create the structure equivalent to this sequence:
        //doc.insertString(doc.getLength(), "plain", null);    // 5 chars
        //doc.insertString(doc.getLength(), "bold", bold);     // 4 chars
        //doc.insertString(doc.getLength(), "italic", italic); // 6 chars
        //doc.insertString(doc.getLength(), "\ntext", null);   // 5 chars
        doc.writeLock(); // Write lock needed to modify document structure
        Element[] leaves = new Element[4];
        leaves[0] = doc.createLeafElement(paragraph, null, 0, 5);
        leaves[1] = doc.createLeafElement(paragraph, bold, 5, 9);
        leaves[2] = doc.createLeafElement(paragraph, italic, 9, 15);
        leaves[3] = doc.createLeafElement(paragraph, null, 15, 16);
        ((BranchElement) paragraph).replace(0, 1, leaves);
        BranchElement branch = (BranchElement) doc.createBranchElement(root, null);
        leaves = new Element[1];
        leaves[0] = doc.createLeafElement(branch, null, 16, 21);
        branch.replace(0, 0, leaves);
        branch.addAttributes(boldFalse);
        branch.addAttributes(italicFalse);
        // Add this branch to the root
        ((BranchElement) root).replace(1, 0, new Element[] { branch });
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        doc.writeUnlock();
    }

    /**
     * The remove region affects an element partially:
     * from its start to its middle.
     * No structure is changed.
     */
    public void testRemoveElementStart() throws Exception {
        final Element boldElement = paragraph.getElement(1);
        final int offset = boldElement.getStartOffset();
        final int length = (boldElement.getEndOffset() - boldElement.getStartOffset()) / 2;
        buf.remove(offset, length, createEvent(offset, length));
        assertEquals(0, getEdits(event).size());
        assertChildren(paragraph, new int[] { 0, 5, 5, 9, 9, 15, 15, 16 }, new AttributeSet[] {
                null, bold, italic, null });
    }

    /**
     * The remove region affects an element partially:
     * from its middle to its end.
     * No structure is changed.
     */
    public void testRemoveElementEnd() throws Exception {
        final Element boldElement = paragraph.getElement(1);
        final int offset = (boldElement.getStartOffset() + boldElement.getEndOffset()) / 2;
        final int length = boldElement.getEndOffset() - offset;
        buf.remove(offset, length, createEvent(offset, length));
        assertEquals(0, getEdits(event).size());
        assertChildren(paragraph, new int[] { 0, 5, 5, 9, 9, 15, 15, 16 }, new AttributeSet[] {
                null, bold, italic, null });
    }

    /**
     * An element fully falls into the remove region.
     * This element is to be removed.
     */
    public void testRemoveElementFull() throws Exception {
        final Element boldElement = paragraph.getElement(1);
        final int offset = boldElement.getStartOffset();
        final int length = boldElement.getEndOffset() - offset;
        buf.remove(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), paragraph, 1, new int[] { 5, 9 }, new int[] {});
        assertChildren(paragraph, new int[] { 0, 5, 9, 15, 15, 16 }, new AttributeSet[] { null,
                italic, null });
    }

    /**
     * The remove region affects two consecutive elements:
     * from the start of the first to the middle of the second.
     * The element fully contained in the remove region is to be removed
     * (the first one).
     */
    public void testRemove2ElementsStart() throws Exception {
        final Element plainElement = paragraph.getElement(0);
        final Element boldElement = paragraph.getElement(1);
        final int offset = plainElement.getStartOffset();
        final int length = (boldElement.getStartOffset() + boldElement.getEndOffset()) / 2
                - offset;
        buf.remove(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), paragraph, 0, new int[] { 0, 5 }, new int[] {});
        assertChildren(paragraph, new int[] { 5, 9, 9, 15, 15, 16 }, new AttributeSet[] { bold,
                italic, null });
    }

    /**
     * The remove region affects two consecutive elements:
     * from the middle of the first to the end of the second.
     * The element fully contained in the remove region is to be removed
     * (the second one).
     */
    public void testRemove2ElementsEnd() throws Exception {
        final Element plainElement = paragraph.getElement(0);
        final Element boldElement = paragraph.getElement(1);
        final int offset = (plainElement.getStartOffset() + plainElement.getEndOffset()) / 2;
        final int length = boldElement.getEndOffset() - offset;
        buf.remove(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), paragraph, 1, new int[] { 5, 9 }, new int[] {});
        assertChildren(paragraph, new int[] { 0, 5, 9, 15, 15, 16 }, new AttributeSet[] { null,
                italic, null });
    }

    /**
     * The remove region affects two consecutive elements:
     * from the middle of the first to the middle of the second.
     * No structure is expected to change as the remove region contains
     * no elements which fully fall into it.
     */
    public void testRemove2ElementsStartEnd() throws Exception {
        final Element plainElement = paragraph.getElement(0);
        final Element boldElement = paragraph.getElement(1);
        final int offset = (plainElement.getStartOffset() + plainElement.getEndOffset()) / 2;
        final int length = (boldElement.getStartOffset() + boldElement.getEndOffset()) / 2
                - offset;
        buf.remove(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(0, edits.size());
        assertChildren(paragraph, new int[] { 0, 5, 5, 9, 9, 15, 15, 16 }, new AttributeSet[] {
                null, bold, italic, null });
    }

    /**
     * The remove region contains two elements entirely.
     * Both elements are to be removed.
     */
    public void testRemove2ElementsFull() throws Exception {
        final Element plainElement = paragraph.getElement(0);
        final Element boldElement = paragraph.getElement(1);
        final int offset = plainElement.getStartOffset();
        final int length = boldElement.getEndOffset() - offset;
        buf.remove(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), paragraph, 0, new int[] { 0, 5, 5, 9 }, new int[] {});
        assertChildren(paragraph, new int[] { 9, 15, 15, 16 }, new AttributeSet[] { italic,
                null });
    }

    /**
     * This removes only one character from the document: the new line which
     * separates two paragraphs.
     * The paragraphs merge into one. All their child elements which are
     * not entirely contained in the remove region are copied into the
     * new paragraph.
     */
    public void testRemoveParagraphBreak() throws Exception {
        final int offset = paragraph.getEndOffset() - 1;
        final int length = 1;
        ((AbstractElement) paragraph).addAttributes(bold);
        buf.remove(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), root, 0, new int[] { 0, 16, 16, 21 }, new int[] { 0, 21 });
        final AbstractElement branch = (AbstractElement) root.getElement(0);
        assertChildren(branch, new int[] { 0, 5, 5, 9, 9, 15, 16, 21 }, new AttributeSet[] {
                null, bold, italic, null });
        assertEquals(2, branch.getAttributeCount());
        assertTrue(branch.isDefined(AttributeSet.ResolveAttribute));
        assertTrue(branch.containsAttributes(bold));
    }

    /**
     * Removes an entire element before the paragraph break as well the break
     * itself.
     * Two paragraphs merge. The resulting paragraph doesn't contain the
     * child elements which were entirely contained in the remove region.
     */
    public void testRemoveFullElementParagraphBreak() throws Exception {
        final Element italicElement = paragraph.getElement(2);
        final int offset = italicElement.getStartOffset();
        final int length = paragraph.getEndOffset() - offset;
        ((AbstractElement) paragraph).addAttributes(bold);
        buf.remove(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), root, 0, new int[] { 0, 16, 16, 21 }, new int[] { 0, 21 });
        final AbstractElement branch = (AbstractElement) root.getElement(0);
        assertChildren(branch, new int[] { 0, 5, 5, 9, 16, 21 }, new AttributeSet[] { null,
                bold, null });
        assertEquals(2, branch.getAttributeCount());
        assertTrue(branch.isDefined(AttributeSet.ResolveAttribute));
        assertTrue(branch.containsAttributes(bold));
    }

    /**
     * Remove an entire paragraph.
     * Despite it is not necessary to change the document structure,
     * it is modified as if only part of the paragraph was removed.
     * I.e. a new branch is created, and it contains the children from
     * the following paragraph only because all child elements of the first
     * paragraph fall into the remove region entirely.
     */
    public void testRemoveFullParagraph() throws Exception {
        final int offset = paragraph.getStartOffset();
        final int length = paragraph.getEndOffset() - offset;
        ((AbstractElement) paragraph).addAttributes(bold);
        buf.remove(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), root, 0, new int[] { 0, 16, 16, 21 }, new int[] { 16, 21 });
        final AbstractElement branch = (AbstractElement) root.getElement(0);
        assertChildren(branch, new int[] { 16, 21 }, new AttributeSet[] { null });
        assertEquals(2, branch.getAttributeCount());
        assertTrue(branch.isDefined(AttributeSet.ResolveAttribute));
        assertTrue(branch.containsAttributes(bold));
    }

    private static void assertChange(final Object change, final Element element,
            final int index, final int[] removed, final int[] added) {
        DefStyledDoc_Helpers.assertChange(change, element, index, removed, added);
    }

    private static void assertChildren(final Element element, final int[] offsets,
            final AttributeSet[] attributes) {
        DefStyledDoc_Helpers.assertChildren(element, offsets, attributes);
    }

    private DefaultDocumentEvent createEvent(final int offset, final int length) {
        event = doc.new DefaultDocumentEvent(offset, length, EventType.CHANGE);
        return event;
    }

    private static List<?> getEdits(final DefaultDocumentEvent event) {
        return DefStyledDoc_Helpers.getEdits(event);
    }
}
