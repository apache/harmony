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
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.Content;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.DefaultStyledDocument.ElementBuffer;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import junit.framework.TestCase;

/**
 * Contains the test case that failed when inserting into the document using
 * <code>insertString</code> method.
 * This is to test that <code>ElementSpec</code>s when applied will generate
 * the required structure; and that <code>insertUpdate()</code> generates the
 * expected set of <code>ElementSpec</code>s.
 *
 */
public class DefaultStyledDocument_ElementBuffer_InsertMiscTest extends TestCase implements
        DocumentListener {
    private DefaultStyledDocument doc;

    private ElementBuffer buf;

    private Element root;

    private Content content;

    private DefaultDocumentEvent event;

    private DefaultDocumentEvent insertEvent;

    private ElementSpec[] insertSpecs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        root = doc.getDefaultRootElement();
        buf = new DefStyledDoc_Helpers.ElementBufferWithLogging(doc, root) {
            private static final long serialVersionUID = 1L;

            @Override
            public void insert(int offset, int length, ElementSpec[] spec,
                    DefaultDocumentEvent event) {
                super.insert(offset, length, insertSpecs = spec, event);
            }
        };
        doc.buffer = buf;
        content = doc.getContent();
        doc.addDocumentListener(this);
        doc.writeLock();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        doc.writeUnlock();
    }

    /**
     * Tests that ElementSpecs for inserting non-attributed string
     * <code>"first\nsecond\nthird"</code> into an empty document will
     * lead to the expected document structure.
     */
    public void testInsertThreePars() throws Exception {
        final String text = "first\nsecond\nthird";
        final int textLen = text.length();
        //doc.insertString(0, text, null);
        content.insertString(0, text);
        event = doc.new DefaultDocumentEvent(0, textLen, EventType.INSERT);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, 6),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, 7),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, 5), };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[specs.length - 2].setDirection(ElementSpec.JoinFractureDirection);
        specs[specs.length - 1].setDirection(ElementSpec.JoinNextDirection);
        buf.insert(0, textLen, specs, event);
        List<?> edits = getEdits(event);
        assertEquals(4, edits.size());
        assertChange(edits.get(0), 0, new int[] { 0, 19 }, new int[] { 0, 6 });
        assertChange(edits.get(1), 0, new int[] {}, new int[] { 6, 13 });
        assertChange(edits.get(2), 0, new int[] { 18, 19 }, new int[] { 13, 19 });
        assertChange(edits.get(3), 1, new int[] {}, new int[] { 6, 13, 13, 19 });
        assertChildren(root.getElement(0), new int[] { 0, 6 }, new AttributeSet[] { null });
        assertChildren(root.getElement(1), new int[] { 6, 13 }, new AttributeSet[] { null });
        assertChildren(root.getElement(2), new int[] { 13, 19 }, new AttributeSet[] { null });
        assertEquals("first\n", getText(doc.getCharacterElement(0)));
        assertEquals("second\n", getText(doc.getCharacterElement(0 + 6)));
        assertEquals("third\n", getText(doc.getCharacterElement(0 + 6 + 7)));
    }

    /**
     * Tests that DefaultStyledDocument.insertUpdate generates the expected
     * set of ElementSpecs for inserting non-attributed string
     * <code>"first\nsecond\nthird"</code> into an empty document and that
     * the document has the expected structure after the insert.
     */
    public void testInsertUpdateThreePars() throws Exception {
        final String text = "first\nsecond\nthird";
        doc.insertString(0, text, null);
        //        doc.dump(System.out);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.ContentType, 6),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, 7),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, 5), };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[specs.length - 2].setDirection(ElementSpec.JoinFractureDirection);
        specs[specs.length - 1].setDirection(ElementSpec.JoinNextDirection);
        assertSpecs(specs, insertSpecs);
        List<?> edits = getEdits(insertEvent);
        assertEquals(5, edits.size());
        assertChange(edits.get(1), 0, new int[] { 0, 19 }, new int[] { 0, 6 });
        assertChange(edits.get(2), 0, new int[] {}, new int[] { 6, 13 });
        assertChange(edits.get(3), 0, new int[] { 18, 19 }, new int[] { 13, 19 });
        assertChange(edits.get(4), 1, new int[] {}, new int[] { 6, 13, 13, 19 });
        assertChildren(root.getElement(0), new int[] { 0, 6 }, new AttributeSet[] { null });
        assertChildren(root.getElement(1), new int[] { 6, 13 }, new AttributeSet[] { null });
        assertChildren(root.getElement(2), new int[] { 13, 19 }, new AttributeSet[] { null });
        assertEquals("first\n", getText(doc.getCharacterElement(0)));
        assertEquals("second\n", getText(doc.getCharacterElement(0 + 6)));
        assertEquals("third\n", getText(doc.getCharacterElement(0 + 6 + 7)));
    }

    public void testInsertRightAfterNewLineWithNewLine() throws Exception {
        final String initialText = "one\ntwo\nthree";
        doc.insertString(0, initialText, null);
        final int offset = root.getElement(0).getEndOffset();
        doc.insertString(offset, "^^^\n", null);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, 4),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType) };
        specs[specs.length - 1].setDirection(ElementSpec.JoinNextDirection);
        assertSpecs(specs, insertSpecs);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), 0, new int[] { 0, 8 }, new int[] { 0, 4 });
        assertChange(edits.get(2), 0, new int[] {}, new int[] { 4, 8 });
        assertChange(edits.get(3), 1, new int[] {}, new int[] { 4, 8 });
        assertChildren(root.getElement(0), new int[] { 0, 4 }, new AttributeSet[] { null });
        assertChildren(root.getElement(1), new int[] { 4, 8 }, new AttributeSet[] { null });
        assertChildren(root.getElement(2), new int[] { 8, 12 }, new AttributeSet[] { null });
        assertChildren(root.getElement(3), new int[] { 12, 18 }, new AttributeSet[] { null });
        assertEquals("one\n", getText(doc.getCharacterElement(0)));
        assertEquals("^^^\n", getText(doc.getCharacterElement(0 + 4)));
        assertEquals("two\n", getText(doc.getCharacterElement(0 + 4 + 4)));
        assertEquals("three\n", getText(doc.getCharacterElement(0 + 4 + 4 + 4)));
    }

    public void testInsertRightAfterNewLineWithoutNewLine() throws Exception {
        final String initialText = "one\ntwo\nthree";
        doc.insertString(0, initialText, null);
        final int offset = root.getElement(0).getEndOffset();
        doc.insertString(offset, "^^^", null);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, 3), };
        specs[specs.length - 2].setDirection(ElementSpec.JoinNextDirection);
        specs[specs.length - 1].setDirection(ElementSpec.JoinNextDirection);
        assertSpecs(specs, insertSpecs);
        List<?> edits = getEdits(insertEvent);
        assertEquals(3, edits.size());
        assertChange(edits.get(1), 0, new int[] { 0, 7 }, new int[] { 0, 4 });
        assertChange(edits.get(2), 0, new int[] { 7, 11 }, new int[] { 4, 11 });
        assertChildren(root.getElement(0), new int[] { 0, 4 }, new AttributeSet[] { null });
        assertChildren(root.getElement(1), new int[] { 4, 11 }, new AttributeSet[] { null });
        assertChildren(root.getElement(2), new int[] { 11, 17 }, new AttributeSet[] { null });
        assertEquals("one\n", getText(doc.getCharacterElement(0)));
        assertEquals("^^^two\n", getText(doc.getCharacterElement(0 + 4)));
        assertEquals("three\n", getText(doc.getCharacterElement(0 + 4 + 7)));
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

    private static void assertSpecs(final ElementSpec[] specs, final ElementSpec[] insertSpecs) {
        DefStyledDoc_Helpers.assertSpecs(specs, insertSpecs);
    }

    private static List<?> getEdits(final DefaultDocumentEvent event) {
        return DefStyledDoc_Helpers.getEdits(event);
    }

    public void changedUpdate(DocumentEvent e) {
    }

    public void insertUpdate(DocumentEvent e) {
        insertEvent = (DefaultDocumentEvent) e;
    }

    public void removeUpdate(DocumentEvent e) {
    }
}
