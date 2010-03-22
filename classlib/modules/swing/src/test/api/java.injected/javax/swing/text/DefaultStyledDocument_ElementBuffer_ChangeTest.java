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
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.Content;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.DefaultStyledDocument.ElementBuffer;
import junit.framework.TestCase;

/**
 * Tests the behavior of
 * <code>DefaultStyledDocument.ElementBuffer.change()</code> method.
 *
 */
public class DefaultStyledDocument_ElementBuffer_ChangeTest extends TestCase {
    private DefaultStyledDocument doc;

    private Element root;

    private ElementBuffer buf;

    private DefaultDocumentEvent event;

    private Content content;

    private Element paragraph;

    private static final AttributeSet bold = DefStyledDoc_Helpers.bold;

    private static final AttributeSet italic = DefStyledDoc_Helpers.italic;

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
        ((BranchElement) root).replace(1, 0, new Element[] { branch });
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        doc.writeUnlock();
    }

    /**
     * The change is applied to the whole element.
     * No structure are expected to change.
     */
    public void testChangeFullElement() throws Exception {
        Element boldElement = paragraph.getElement(1);
        int offset = boldElement.getStartOffset();
        int length = boldElement.getEndOffset() - offset;
        buf.change(offset, length, createEvent(offset, length));
        assertEquals(0, getEdits(event).size());
        assertChildren(paragraph, new int[] { 0, 5, 5, 9, 9, 15, 15, 16 }, new AttributeSet[] {
                null, bold, italic, null });
    }

    /**
     * The change is applied from the start of an element to its middle.
     * The element is to be split.
     */
    public void testChangeElementStart() throws Exception {
        Element boldElement = paragraph.getElement(1);
        int offset = boldElement.getStartOffset();
        int length = (boldElement.getEndOffset() - offset) / 2;
        buf.change(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), paragraph, 1, new int[] { 5, 9 }, new int[] { 5, 7, 7, 9 });
        assertChildren(paragraph, new int[] { 0, 5, 5, 7, 7, 9, 9, 15, 15, 16 },
                new AttributeSet[] { null, bold, bold, italic, null });
    }

    /**
     * The change is applied from the middle of an element to its end.
     * The element is to be split.
     */
    public void testChangeElementEnd() throws Exception {
        Element boldElement = paragraph.getElement(1);
        int offset = (boldElement.getStartOffset() + boldElement.getEndOffset()) / 2;
        int length = boldElement.getEndOffset() - offset;
        buf.change(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), paragraph, 1, new int[] { 5, 9 }, new int[] { 5, 7, 7, 9 });
        assertChildren(paragraph, new int[] { 0, 5, 5, 7, 7, 9, 9, 15, 15, 16 },
                new AttributeSet[] { null, bold, bold, italic, null });
    }

    /**
     * The change is applied to two entire elements.
     * No structure are expected to change.
     */
    public void testChangeFull2Elements() throws Exception {
        Element plainElement = paragraph.getElement(0);
        Element boldElement = paragraph.getElement(1);
        int offset = plainElement.getStartOffset();
        int length = boldElement.getEndOffset() - offset;
        buf.change(offset, length, createEvent(offset, length));
        assertEquals(0, getEdits(event).size());
        assertChildren(paragraph, new int[] { 0, 5, 5, 9, 9, 15, 15, 16 }, new AttributeSet[] {
                null, bold, italic, null });
    }

    /**
     * The change is applied from the start of an element to the middle of
     * the next element.
     * The former element is to be split, the other is unchanged.
     */
    public void testChange2ElementsStart() throws Exception {
        Element plainElement = paragraph.getElement(0);
        Element boldElement = paragraph.getElement(1);
        int offset = plainElement.getStartOffset();
        int length = (boldElement.getStartOffset() + boldElement.getEndOffset()) / 2;
        buf.change(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), paragraph, 1, new int[] { 5, 9 }, new int[] { 5, 7, 7, 9 });
        assertChildren(paragraph, new int[] { 0, 5, 5, 7, 7, 9, 9, 15, 15, 16 },
                new AttributeSet[] { null, bold, bold, italic, null });
    }

    /**
     * The change is applied from the middle of an element to the end of
     * the next element.
     * The first element is to be split, the other is unchanged.
     */
    public void testChange2ElementsEnd() throws Exception {
        Element plainElement = paragraph.getElement(0);
        Element boldElement = paragraph.getElement(1);
        int offset = (plainElement.getStartOffset() + plainElement.getEndOffset()) / 2;
        int length = boldElement.getEndOffset() - offset;
        buf.change(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), paragraph, 0, new int[] { 0, 5 }, new int[] { 0, 2, 2, 5 });
        assertChildren(paragraph, new int[] { 0, 2, 2, 5, 5, 9, 9, 15, 15, 16 },
                new AttributeSet[] { null, null, bold, italic, null });
    }

    /**
     * The change is applied so that it affects three elements:
     * from the middle of the first element to the middle of the third one.
     * It is expected that the first and third element will be split.
     *
     * Although the second element is left unchanged, it is added to the
     * removed and added lists simultaneously. This way the merge of two
     * edits is performed.
     */
    public void testChange3ElementsStartEnd() throws Exception {
        Element plainElement = paragraph.getElement(0);
        Element italicElement = paragraph.getElement(2);
        int offset = (plainElement.getStartOffset() + plainElement.getEndOffset()) / 2;
        int length = (italicElement.getStartOffset() + italicElement.getEndOffset()) / 2
                - offset;
        buf.change(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), paragraph, 0, new int[] { 0, 5, 5, 9, 9, 15 }, new int[] {
                0, 2, 2, 5, 5, 9, 9, 12, 12, 15 });
        // Merge of two changes is performed by copying elements which
        // lie between split ones
        ElementChange change = (ElementChange) edits.get(0);
        assertSame(change.getChildrenRemoved()[1], // bold [5, 9]
                change.getChildrenAdded()[2]);
        assertChildren(paragraph, new int[] { 0, 2, 2, 5, 5, 9, 9, 12, 12, 15, 15, 16 },
                new AttributeSet[] { null, null, bold, italic, italic, null });
    }

    /**
     * The changes makes elements in two different paragraphs be split.
     */
    public void testChange2Paragraphs() throws Exception {
        Element italicElement = paragraph.getElement(2);
        Element paragraph2 = root.getElement(1);
        int offset = (italicElement.getStartOffset() + italicElement.getEndOffset()) / 2;
        int length = (paragraph2.getStartOffset() + paragraph2.getEndOffset()) / 2 - offset;
        buf.change(offset, length, createEvent(offset, length));
        final List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), paragraph, 2, new int[] { 9, 15 },
                new int[] { 9, 12, 12, 15 });
        assertChange(edits.get(1), paragraph2, 0, new int[] { 16, 21 }, new int[] { 16, 18, 18,
                21 });
        assertChildren(paragraph, new int[] { 0, 5, 5, 9, 9, 12, 12, 15, 15, 16 },
                new AttributeSet[] { null, bold, italic, italic, null });
        assertChildren(paragraph2, new int[] { 16, 18, 18, 21 }, new AttributeSet[] { null,
                null });
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
