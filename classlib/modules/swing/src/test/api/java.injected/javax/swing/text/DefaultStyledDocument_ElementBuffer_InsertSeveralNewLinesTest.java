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
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.Content;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.DefaultStyledDocument.ElementBuffer;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import junit.framework.TestCase;

/**
 * Tests DefaultStyledDocument.ElementBuffer.insert() method with
 * ElementSpec array that was obtained from real-life text insertions.
 * The text contains several new line characters as well as other characters
 * too.
 *
 */
public class DefaultStyledDocument_ElementBuffer_InsertSeveralNewLinesTest extends TestCase {
    private DefaultStyledDocument doc;

    private ElementBuffer buf;

    private Element root;

    private Element paragraph;

    private int insertOffset;

    private Content content;

    private DefaultDocumentEvent event;

    private static final AttributeSet bold = DefStyledDoc_Helpers.bold;

    private static final AttributeSet italic = DefStyledDoc_Helpers.italic;

    private static final String newLine = "\n";

    private static final int newLineLen = newLine.length();

    private static final String newLines2 = "\n\n";

    private static final int newLines2Len = newLines2.length();

    private static final String newLines3 = "\n\n\n";

    private static final int newLines3Len = newLines3.length();

    private static final String newLines2Text = "0\n1\n";

    private static final String newLines3Text = "0\n1\n2\n";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        root = doc.getDefaultRootElement();
        buf = new DefStyledDoc_Helpers.ElementBufferWithLogging(doc, root);
        doc.buffer = buf;
        paragraph = root.getElement(0);
        content = doc.getContent();
        paragraph = root.getElement(0);
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
        // Add this branch to the root
        ((BranchElement) root).replace(1, 0, new Element[] { branch });
        insertOffset = 5 + 2;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        doc.writeUnlock();
    }

    /**
     * Inserting text with the same attributes into the beginning of
     * the document. The text contains <i>two</i> new line characters only.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLines2, null)</code>,
     * where <code>insertOffset = 0</code>.
     */
    public void testInsertSameAttrsDocStart2NewLines() throws Exception {
        insertOffset = 0;
        // doc.insertString(insertOffset, newLines2, null);
        content.insertString(insertOffset, newLines2);
        event = doc.new DefaultDocumentEvent(insertOffset, newLines2Len, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[specs.length - 1].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLines2Len, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(3, edits.size());
        assertChange(edits.get(0), 0, new int[] { 0, 7, 7, 11, 11, 17, 17, 18 }, new int[] { 0,
                1 });
        assertChange(edits.get(1), 0, new int[] {}, new int[] { 1, 2 });
        assertChange(edits.get(2), 1, new int[] {}, new int[] { 1, 2, 2, 18 });
        assertChildren(root.getElement(0), new int[] { 0, 1 }, new AttributeSet[] { null });
        assertChildren(root.getElement(1), new int[] { 1, 2 }, new AttributeSet[] { null });
        assertChildren(root.getElement(2), new int[] { 2, 7, 7, 11, 11, 17, 17, 18 },
                new AttributeSet[] { null, bold, italic, null });
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset + newLineLen)));
        assertEquals("plain", getText(doc.getCharacterElement(insertOffset + newLines2Len)));
    }

    /**
     * Inserting text with the same attributes into the beginning of
     * the document. The text contains <i>three</i> new line characters only.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLines3, null)</code>,
     * where <code>insertOffset = 0</code>.
     */
    public void testInsertSameAttrsDocStart3NewLines() throws Exception {
        insertOffset = 0;
        // doc.insertString(insertOffset, newLines3, null);
        content.insertString(insertOffset, newLines3);
        event = doc.new DefaultDocumentEvent(insertOffset, newLines3Len, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[specs.length - 1].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLines3Len, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(4, edits.size());
        assertChange(edits.get(0), 0, new int[] { 0, 8, 8, 12, 12, 18, 18, 19 }, new int[] { 0,
                1 });
        assertChange(edits.get(1), 0, new int[] {}, new int[] { 1, 2 });
        assertChange(edits.get(2), 0, new int[] {}, new int[] { 2, 3 });
        assertChange(edits.get(3), 1, new int[] {}, new int[] { 1, 2, 2, 3, 3, 19 });
        assertChildren(root.getElement(0), new int[] { 0, 1 }, new AttributeSet[] { null });
        assertChildren(root.getElement(1), new int[] { 1, 2 }, new AttributeSet[] { null });
        assertChildren(root.getElement(2), new int[] { 2, 3 }, new AttributeSet[] { null });
        assertChildren(root.getElement(3), new int[] { 3, 8, 8, 12, 12, 18, 18, 19 },
                new AttributeSet[] { null, bold, italic, null });
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset + newLineLen)));
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset + newLines2Len)));
        assertEquals("plain", getText(doc.getCharacterElement(insertOffset + newLines3Len)));
    }

    /**
     * Inserting text with the same attributes into the beginning of
     * the document. The text contains <i>two</i> new line characters and
     * one digit before each of them.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLines2Text, null)</code>,
     * where <code>insertOffset = 0</code>.
     */
    public void testInsertSameAttrsDocStart2NewLinesText() throws Exception {
        insertOffset = 0;
        // doc.insertString(insertOffset, newLines2Text, null);
        content.insertString(insertOffset, newLines2Text);
        event = doc.new DefaultDocumentEvent(insertOffset, newLines2Len * 2, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, newLineLen * 2),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, newLineLen * 2),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[specs.length - 1].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLines2Len * 2, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(3, edits.size());
        assertChange(edits.get(0), 0, new int[] { 0, 9, 9, 13, 13, 19, 19, 20 }, new int[] { 0,
                2 });
        assertChange(edits.get(1), 0, new int[] {}, new int[] { 2, 4 });
        assertChange(edits.get(2), 1, new int[] {}, new int[] { 2, 4, 4, 20 });
        assertChildren(root.getElement(0), new int[] { 0, 2 }, new AttributeSet[] { null });
        assertChildren(root.getElement(1), new int[] { 2, 4 }, new AttributeSet[] { null });
        assertChildren(root.getElement(2), new int[] { 4, 9, 9, 13, 13, 19, 19, 20 },
                new AttributeSet[] { null, bold, italic, null });
        assertEquals("0\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("1\n", getText(doc.getCharacterElement(insertOffset + newLineLen * 2)));
        assertEquals("plain", getText(doc.getCharacterElement(insertOffset + newLines2Len * 2)));
    }

    /**
     * Inserting text with the same attributes into the beginning of
     * the document. The text contains <i>three</i> new line characters and
     * one digit before each of them.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLines3Text, null)</code>,
     * where <code>insertOffset = 0</code>.
     */
    public void testInsertSameAttrsDocStart3NewLinesText() throws Exception {
        insertOffset = 0;
        // doc.insertString(insertOffset, newLines3Text, null);
        content.insertString(insertOffset, newLines3Text);
        event = doc.new DefaultDocumentEvent(insertOffset, newLines3Len * 2, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, newLineLen * 2),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(bold, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, newLineLen * 2),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(italic, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, newLineLen * 2),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(italic, ElementSpec.StartTagType) };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[specs.length - 1].setDirection(ElementSpec.JoinFractureDirection);
        ((AbstractElement) root.getElement(0)).addAttributes(bold);
        ((AbstractElement) root.getElement(0)).addAttributes(italic);
        buf.insert(insertOffset, newLines3Len * 2, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(4, edits.size());
        assertChange(edits.get(0), 0, new int[] { 0, 11, 11, 15, 15, 21, 21, 22 }, new int[] {
                0, 2 });
        assertChange(edits.get(1), 0, new int[] {}, new int[] { 2, 4 });
        assertChange(edits.get(2), 0, new int[] {}, new int[] { 4, 6 });
        assertChange(edits.get(3), 1, new int[] {}, new int[] { 2, 4, 4, 6, 6, 22 });
        assertChildren(root.getElement(0), new int[] { 0, 2 }, new AttributeSet[] { null });
        assertChildren(root.getElement(1), new int[] { 2, 4 }, new AttributeSet[] { null });
        assertChildren(root.getElement(2), new int[] { 4, 6 }, new AttributeSet[] { null });
        assertChildren(root.getElement(3), new int[] { 6, 11, 11, 15, 15, 21, 21, 22 },
                new AttributeSet[] { null, bold, italic, null });
        // Assert attributes of the paragraph
        Element paragraph = root.getElement(0);
        assertEquals(3, paragraph.getAttributes().getAttributeCount());
        assertTrue(paragraph.getAttributes().isDefined(AttributeSet.ResolveAttribute));
        assertTrue(paragraph.getAttributes().containsAttributes(bold));
        assertTrue(paragraph.getAttributes().containsAttributes(italic));
        paragraph = root.getElement(1);
        assertTrue(paragraph.getAttributes().isEqual(bold));
        paragraph = root.getElement(2);
        assertTrue(paragraph.getAttributes().isEqual(italic));
        paragraph = root.getElement(3);
        assertEquals(3, paragraph.getAttributes().getAttributeCount());
        assertTrue(paragraph.getAttributes().isDefined(AttributeSet.ResolveAttribute));
        assertTrue(paragraph.getAttributes().containsAttributes(bold));
        assertTrue(paragraph.getAttributes().containsAttributes(italic));
        assertEquals("0\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("1\n", getText(doc.getCharacterElement(insertOffset + newLineLen * 2)));
        assertEquals("2\n", getText(doc.getCharacterElement(insertOffset + newLines2Len * 2)));
        assertEquals("plain", getText(doc.getCharacterElement(insertOffset + newLines3Len * 2)));
    }

    private String getText(final int offset, final int length) throws BadLocationException {
        return doc.getText(offset, length);
    }

    private String getText(final Element element) throws BadLocationException {
        return getText(element.getStartOffset(), element.getEndOffset()
                - element.getStartOffset());
    }

    private static void assertChange(final Object change, final int index,
            final int[] removedOffsets, final int[] addedOffsets) {
        assertEquals("Change index", index, ((ElementChange) change).getIndex());
        DefStyledDoc_Helpers.assertChange((ElementChange) change, removedOffsets, addedOffsets);
    }

    private static void assertChildren(final Element element, final int[] offsets,
            final AttributeSet[] attributes) {
        DefStyledDoc_Helpers.assertChildren(element, offsets, attributes);
    }

    private static List<?> getEdits(final DefaultDocumentEvent event) {
        return DefStyledDoc_Helpers.getEdits(event);
    }
}
