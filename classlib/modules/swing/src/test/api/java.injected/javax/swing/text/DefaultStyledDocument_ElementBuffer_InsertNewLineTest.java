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
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import junit.framework.TestCase;

/**
 * Tests DefaultStyledDocument.ElementBuffer.insert() method with
 * ElementSpec array that was obtained from real-life text insertions.
 * The text contains only new line character.
 *
 */
public class DefaultStyledDocument_ElementBuffer_InsertNewLineTest extends TestCase {
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
     * the document.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, null)</code>,
     * where <code>insertOffset = 0</code>.
     */
    public void testInsertSameAttrsDocStart() throws Exception {
        insertOffset = 0;
        // doc.insertString(insertOffset, newLine, null);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[2].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 0, 6, 6, 10, 10, 16, 16, 17 },
                new int[] { 0, 1 });
        assertChange(edits.get(1), new int[] {}, new int[] { 1, 17 });
        assertChildren(root.getElement(0), new int[] { 0, 1 }, new AttributeSet[] { null });
        assertChildren(root.getElement(1), new int[] { 1, 6, 6, 10, 10, 16, 16, 17 },
                new AttributeSet[] { null, bold, italic, null });
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("plain", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    /**
     * Inserting text with different attributes into the beginning of
     * the document.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, italic)</code>,
     * where <code>insertOffset = 0</code>.
     */
    public void testInsertDiffAttrsDocStart() throws Exception {
        insertOffset = 0;
        // doc.insertString(insertOffset, newLine, italic);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(italic, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[2].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 0, 6, 6, 10, 10, 16, 16, 17 },
                new int[] { 0, 1 });
        assertChange(edits.get(1), new int[] {}, new int[] { 1, 17 });
        assertChildren(root.getElement(0), new int[] { 0, 1 }, new AttributeSet[] { italic });
        assertChildren(root.getElement(1), new int[] { 1, 6, 6, 10, 10, 16, 16, 17 },
                new AttributeSet[] { null, bold, italic, null });
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("plain", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    /**
     * Inserting text into middle of an element with the same attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, bold)</code>,
     * where <code>insertOffset</code> has default value from
     * <code>setUp()</code>.
     */
    public void testInsertSameAttrsMiddle() throws Exception {
        // doc.insertString(insertOffset, newLine, bold);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[2].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 5, 10, 10, 16, 16, 17 }, new int[] { 5, 8 });
        assertChange(edits.get(1), new int[] {}, new int[] { 8, 17 });
        assertChildren(root.getElement(0), new int[] { 0, 5, 5, 8 }, new AttributeSet[] { null,
                bold });
        assertChildren(root.getElement(1), new int[] { 8, 10, 10, 16, 16, 17 },
                new AttributeSet[] { bold, italic, null });
        assertEquals("bo\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("ld", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    /**
     * Inserting text into end of the an element with the same attributes;
     * the following element has different attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, bold)</code>,
     * where 2 is added to default value of <code>insertOffset</code>.
     */
    public void testInsertSameAttrsEnd() throws Exception {
        insertOffset += 2;
        // doc.insertString(insertOffset, newLine, bold);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[2].setDirection(ElementSpec.JoinFractureDirection);
        // Spec [0] has wrong attributes (should be bold) but everything works
        // the way it supposed to.
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 10, 16, 16, 17 }, new int[] {});
        assertChange(edits.get(1), new int[] {}, new int[] { 10, 17 });
        assertChildren(root.getElement(0), new int[] { 0, 5, 5, 10 }, new AttributeSet[] {
                null, bold });
        assertChildren(root.getElement(1), new int[] { 10, 16, 16, 17 }, new AttributeSet[] {
                italic, null });
        assertEquals("bold\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("italic", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    /**
     * Inserting text into the middle of an element with different attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, null)</code>,
     * where <code>insertOffset</code> has default value from
     * <code>setUp()</code>.
     */
    public void testInsertDiffAttrsMiddle() throws Exception {
        // doc.insertString(insertOffset, newLine, null);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[2].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 5, 10, 10, 16, 16, 17 },
                new int[] { 5, 7, 7, 8 });
        assertChange(edits.get(1), new int[] {}, new int[] { 8, 17 });
        assertChildren(root.getElement(0), new int[] { 0, 5, 5, 7, 7, 8 }, new AttributeSet[] {
                null, bold, null });
        assertChildren(root.getElement(1), new int[] { 8, 10, 10, 16, 16, 17 },
                new AttributeSet[] { bold, italic, null });
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("ld", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    /**
     * Inserting text into element boundary; the text and both elements have
     * different attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, null)</code>,
     * where 2 is added to default value of <code>insertOffset</code>.
     */
    public void testInsertDiffAttrsEnd() throws Exception {
        insertOffset += 2;
        // doc.insertString(insertOffset, newLine, null);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[2].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 5, 10, 10, 16, 16, 17 },
                new int[] { 5, 9, 9, 10 });
        assertChange(edits.get(1), new int[] {}, new int[] { 10, 17 });
        assertChildren(root.getElement(0), new int[] { 0, 5, 5, 9, 9, 10 }, new AttributeSet[] {
                null, bold, null });
        assertChildren(root.getElement(1), new int[] { 10, 16, 16, 17 }, new AttributeSet[] {
                italic, null });
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("italic", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    /**
     * Inserting text into element boundary; the attributes of the text and
     * the following element are the same, the attributes of the previous
     * element are different.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, italic)</code>,
     * where 2 is added to default value of <code>insertOffset</code>.
     */
    public void testInsertDiffSameAttrsEnd() throws Exception {
        insertOffset += 2;
        // doc.insertString(insertOffset, newLine, italic);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(italic, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[2].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 5, 10, 10, 16, 16, 17 },
                new int[] { 5, 9, 9, 10 });
        assertChange(edits.get(1), new int[] {}, new int[] { 10, 17 });
        assertChildren(root.getElement(0), new int[] { 0, 5, 5, 9, 9, 10 }, new AttributeSet[] {
                null, bold, italic });
        assertChildren(root.getElement(1), new int[] { 10, 16, 16, 17 }, new AttributeSet[] {
                italic, null });
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("italic", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    /**
     * Inserting text into the start of a paragraph with the same attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, null)</code>,
     * where <code>insertOffset = paragraph.getEndOffset()</code>.
     */
    public void testInsertSameAttrsParStart() throws Exception {
        insertOffset = paragraph.getEndOffset();
        // doc.insertString(insertOffset, newLine, null);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[4].setDirection(ElementSpec.JoinNextDirection);
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(3, edits.size());
        assertChange(edits.get(0), new int[] { 15, 17 }, new int[] { 15, 16 });
        assertChange(edits.get(1), new int[] {}, new int[] { 16, 17 });
        assertChange(edits.get(2), new int[] {}, new int[] { 16, 17 });
        assertChildren(root.getElement(0), new int[] { 0, 5, 5, 9, 9, 15, 15, 16 },
                new AttributeSet[] { null, bold, italic, null });
        assertChildren(root.getElement(1), new int[] { 16, 17 }, new AttributeSet[] { null });
        assertChildren(root.getElement(2), new int[] { 17, 22 }, new AttributeSet[] { null });
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("text\n", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    /**
     * Inserting text into the start of a paragraph with different attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, italic)</code>,
     * where <code>insertOffset = paragraph.getEndOffset()</code>.
     */
    public void testInsertDiffAttrsParStart() throws Exception {
        insertOffset = paragraph.getEndOffset();
        // doc.insertString(insertOffset, newLine, italic);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(italic, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[4].setDirection(ElementSpec.JoinNextDirection);
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(3, edits.size());
        assertChange(edits.get(0), new int[] { 15, 17 }, new int[] { 15, 16 });
        assertChange(edits.get(1), new int[] {}, new int[] { 16, 17 });
        assertChange(edits.get(2), new int[] {}, new int[] { 16, 17 });
        assertChildren(root.getElement(0), new int[] { 0, 5, 5, 9, 9, 15, 15, 16 },
                new AttributeSet[] { null, bold, italic, null });
        assertChildren(root.getElement(1), new int[] { 16, 17 }, new AttributeSet[] { italic });
        assertChildren(root.getElement(2), new int[] { 17, 22 }, new AttributeSet[] { null });
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("text\n", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    /**
     * Inserting text into the end of the document with the same attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, null)</code>,
     * where <code>insertOffset = doc.getLength()</code>.
     */
    public void testInsertSameAttrsDocEnd() throws Exception {
        insertOffset = doc.getLength();
        // doc.insertString(insertOffset, newLine, null);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[2].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 16, 22 }, new int[] { 16, 21 });
        assertChange(edits.get(1), new int[] {}, new int[] { 21, 22 });
        assertChildren(root.getElement(1), new int[] { 16, 21 }, new AttributeSet[] { null });
        assertChildren(root.getElement(2), new int[] { 21, 22 }, new AttributeSet[] { null });
        assertEquals("text\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    /**
     * Inserting text into the end of the document with different attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, newLine, italic)</code>,
     * where <code>insertOffset = doc.getLength()</code>.
     */
    public void testInsertDiffAttrsDocEnd() throws Exception {
        insertOffset = doc.getLength();
        // doc.insertString(insertOffset, newLine, italic);
        content.insertString(insertOffset, newLine);
        event = doc.new DefaultDocumentEvent(insertOffset, newLineLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(italic, ElementSpec.ContentType, newLineLen),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[2].setDirection(ElementSpec.JoinFractureDirection);
        buf.insert(insertOffset, newLineLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 16, 22 }, new int[] { 16, 20, 20, 21 });
        assertChange(edits.get(1), new int[] {}, new int[] { 21, 22 });
        assertChildren(root.getElement(1), new int[] { 16, 20, 20, 21 }, new AttributeSet[] {
                null, italic });
        assertChildren(root.getElement(2), new int[] { 21, 22 }, new AttributeSet[] { null });
        assertEquals("text", getText(doc.getCharacterElement(insertOffset - 1)));
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset + newLineLen)));
    }

    private String getText(final int offset, final int length) throws BadLocationException {
        return doc.getText(offset, length);
    }

    private String getText(final Element element) throws BadLocationException {
        return getText(element.getStartOffset(), element.getEndOffset()
                - element.getStartOffset());
    }

    private static void assertChange(final Object change, final int[] removedOffsets,
            final int[] addedOffsets) {
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
/*
 The dump of the document after the initial set up.

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
 [9,15][italic]
 <content>
 [15,16][
 ]
 <paragraph>
 <content>
 [16,21][text
 ]
 <bidi root>
 <bidi level
 bidiLevel=0
 >
 [0,21][plainbolditalic
 text
 ]
 */