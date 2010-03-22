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
 * The text contains no new line characters.
 *
 */
public class DefaultStyledDocument_ElementBuffer_InsertTextTest extends TestCase {
    private DefaultStyledDocument doc;

    private ElementBuffer buf;

    private Element root;

    private Element paragraph;

    private int insertOffset;

    private Content content;

    private DefaultDocumentEvent event;

    private static final AttributeSet bold = DefStyledDoc_Helpers.bold;

    private static final AttributeSet italic = DefStyledDoc_Helpers.italic;

    private static final String caps = "^^^";

    private static final int capsLen = caps.length();

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
     * the document.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, null)</code>,
     * where <code>insertOffset = 0</code>.
     */
    public void testInsertSameAttrsDocStart() throws Exception {
        insertOffset = 0;
        // doc.insertString(insertOffset, caps, null);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec spec = new ElementSpec(null, ElementSpec.ContentType, capsLen);
        spec.setDirection(ElementSpec.JoinPreviousDirection);
        buf.insert(insertOffset, capsLen, new ElementSpec[] { spec }, event);
        List<?> edits = getEdits(event);
        assertEquals(0, edits.size());
        assertEquals("^^^plain", getText(doc.getCharacterElement(insertOffset)));
    }

    /**
     * Inserting text with different attributes into the beginning of
     * the document.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, italic)</code>,
     * where <code>insertOffset = 0</code>.
     */
    public void testInsertDiffAttrsDocStart() throws Exception {
        insertOffset = 0;
        // doc.insertString(insertOffset, caps, italic);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec spec = new ElementSpec(null, ElementSpec.ContentType, capsLen);
        buf.insert(insertOffset, capsLen, new ElementSpec[] { spec }, event);
        List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), new int[] { 0, 8 }, new int[] { 0, 3, 3, 8 });
        assertEquals("^^^", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("plain", getText(doc.getCharacterElement(insertOffset + capsLen)));
    }

    /**
     * Inserting text into middle of an element with the same attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, bold)</code>,
     * where <code>insertOffset</code> has default value from
     * <code>setUp()</code>.
     */
    public void testInsertSameAttrsMiddle() throws Exception {
        // doc.insertString(insertOffset, caps, bold);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec spec = new ElementSpec(null, ElementSpec.ContentType, capsLen);
        spec.setDirection(ElementSpec.JoinPreviousDirection);
        buf.insert(insertOffset, capsLen, new ElementSpec[] { spec }, event);
        List<?> edits = getEdits(event);
        assertEquals(0, edits.size());
        assertEquals("bo^^^ld", getText(doc.getCharacterElement(insertOffset)));
    }

    /**
     * Inserting text into end of the an element with the same attributes;
     * the following element has different attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, bold)</code>,
     * where 2 is added to default value of <code>insertOffset</code>.
     */
    public void testInsertSameAttrsEnd() throws Exception {
        insertOffset += 2;
        // doc.insertString(insertOffset, caps, bold);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec spec = new ElementSpec(null, ElementSpec.ContentType, capsLen);
        spec.setDirection(ElementSpec.JoinPreviousDirection);
        buf.insert(insertOffset, capsLen, new ElementSpec[] { spec }, event);
        List<?> edits = getEdits(event);
        assertEquals(0, edits.size());
        assertEquals("bold^^^", getText(doc.getCharacterElement(insertOffset)));
    }

    /**
     * Inserting text into the middle of an element with different attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, null)</code>,
     * where <code>insertOffset</code> has default value from
     * <code>setUp()</code>.
     */
    public void testInsertDiffAttrsMiddle() throws Exception {
        // doc.insertString(insertOffset, caps, null);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec spec = new ElementSpec(null, ElementSpec.ContentType, capsLen);
        buf.insert(insertOffset, capsLen, new ElementSpec[] { spec }, event);
        List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), new int[] { 5, 12 }, new int[] { 5, 7, 7, 10, 10, 12 });
        assertEquals("bo", getText(doc.getCharacterElement(insertOffset - 1)));
        assertEquals("^^^", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("ld", getText(doc.getCharacterElement(insertOffset + capsLen)));
    }

    /**
     * Inserting text into element boundary; the text and both elements have
     * different attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, null)</code>,
     * where 2 is added to default value of <code>insertOffset</code>.
     */
    public void testInsertDiffAttrsEnd() throws Exception {
        insertOffset += 2;
        // doc.insertString(insertOffset, caps, null);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec spec = new ElementSpec(null, ElementSpec.ContentType, capsLen);
        buf.insert(insertOffset, capsLen, new ElementSpec[] { spec }, event);
        List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), new int[] { 5, 12 }, new int[] { 5, 9, 9, 12 });
        assertEquals("bold", getText(doc.getCharacterElement(insertOffset - 1)));
        assertEquals("^^^", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("italic", getText(doc.getCharacterElement(insertOffset + capsLen)));
    }

    /**
     * Inserting text into element boundary; the attributes of the text and
     * the following element are the same, the attributes of the previous
     * element are different.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, italic)</code>,
     * where 2 is added to default value of <code>insertOffset</code>.
     */
    public void testInsertDiffSameAttrsEnd() throws Exception {
        insertOffset += 2;
        // doc.insertString(insertOffset, caps, italic);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec spec = new ElementSpec(italic, ElementSpec.ContentType, capsLen);
        spec.setDirection(ElementSpec.JoinNextDirection);
        buf.insert(insertOffset, capsLen, new ElementSpec[] { spec }, event);
        List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), new int[] { 5, 12, 12, 18 }, new int[] { 5, 9, 9, 18 });
        assertEquals("bold", getText(doc.getCharacterElement(insertOffset - 1)));
        assertEquals("^^^italic", getText(doc.getCharacterElement(insertOffset)));
    }

    /**
     * Inserting text into the start of a paragraph with the same attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, null)</code>,
     * where <code>insertOffset = paragraph.getEndOffset()</code>.
     */
    public void testInsertSameAttrsParStart() throws Exception {
        insertOffset = paragraph.getEndOffset();
        // doc.insertString(insertOffset, caps, null);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, capsLen), };
        specs[1].setDirection(ElementSpec.JoinNextDirection);
        specs[2].setDirection(ElementSpec.JoinNextDirection);
        buf.insert(insertOffset, capsLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 15, 19 }, new int[] { 15, 16 });
        assertChange(edits.get(1), new int[] { 19, 24 }, new int[] { 16, 24 });
        assertChildren(paragraph, new int[] { 0, 5, 5, 9, 9, 15, 15, 16 }, new AttributeSet[] {
                null, bold, italic, null });
        assertChildren(root.getElement(1), new int[] { 16, 24 }, new AttributeSet[] { null });
        assertEquals("^^^text\n", getText(doc.getCharacterElement(insertOffset)));
    }

    /**
     * Inserting text into the start of a paragraph with different attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, italic)</code>,
     * where <code>insertOffset = paragraph.getEndOffset()</code>.
     */
    public void testInsertDiffAttrsParStart() throws Exception {
        insertOffset = paragraph.getEndOffset();
        // doc.insertString(insertOffset, caps, italic);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(italic, ElementSpec.ContentType, capsLen), };
        specs[1].setDirection(ElementSpec.JoinNextDirection);
        buf.insert(insertOffset, capsLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), new int[] { 15, 19 }, new int[] { 15, 16 });
        assertChange(edits.get(1), new int[] {}, new int[] { 16, 19 });
        assertChildren(paragraph, new int[] { 0, 5, 5, 9, 9, 15, 15, 16 }, new AttributeSet[] {
                null, bold, italic, null });
        assertChildren(root.getElement(1), new int[] { 16, 19, 19, 24 }, new AttributeSet[] {
                italic, null });
        assertEquals("^^^", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("text\n", getText(doc.getCharacterElement(insertOffset + capsLen)));
    }

    /**
     * Inserting text into the end of the document with the same attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, null)</code>,
     * where <code>insertOffset = doc.getLength()</code>.
     */
    public void testInsertSameAttrsDocEnd() throws Exception {
        insertOffset = doc.getLength();
        // doc.insertString(insertOffset, caps, null);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec spec = new ElementSpec(null, ElementSpec.ContentType, capsLen);
        spec.setDirection(ElementSpec.JoinPreviousDirection);
        buf.insert(insertOffset, capsLen, new ElementSpec[] { spec }, event);
        List<?> edits = getEdits(event);
        assertEquals(0, edits.size());
        assertChildren(root.getElement(1), new int[] { 16, 24 }, new AttributeSet[] { null });
        assertEquals("text^^^\n", getText(doc.getCharacterElement(insertOffset)));
    }

    /**
     * Inserting text into the end of the document with different attributes.
     * <p>
     * This test is equivalent to
     * <code>doc.insertString(insertOffset, caps, italic)</code>,
     * where <code>insertOffset = doc.getLength()</code>.
     */
    public void testInsertDiffAttrsDocEnd() throws Exception {
        insertOffset = doc.getLength();
        // doc.insertString(insertOffset, caps, italic);
        content.insertString(insertOffset, caps);
        event = doc.new DefaultDocumentEvent(insertOffset, capsLen, EventType.INSERT);
        ElementSpec spec = new ElementSpec(italic, ElementSpec.ContentType, capsLen);
        buf.insert(insertOffset, capsLen, new ElementSpec[] { spec }, event);
        List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), new int[] { 16, 24 }, new int[] { 16, 20, 20, 23, 23, 24 });
        assertChildren(root.getElement(1), new int[] { 16, 20, 20, 23, 23, 24 },
                new AttributeSet[] { null, italic, null });
        assertEquals("text", getText(doc.getCharacterElement(insertOffset - 1)));
        assertEquals("^^^", getText(doc.getCharacterElement(insertOffset)));
        assertEquals("\n", getText(doc.getCharacterElement(insertOffset + capsLen)));
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