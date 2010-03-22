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
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.DefaultStyledDocument.AttributeUndoableEdit;
import javax.swing.text.DefaultStyledDocument.ElementBuffer;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import junit.framework.TestCase;

/**
 * Tests DefaultStyledDocument.ElementBuffer class. Here tested the methods
 * that update document structure: insert, remove, change, and their protected
 * counterparts.
 * <p>
 * The contents of the DefaultDocumentEvent is checked.
 * Also AttributeUndoableEdit is tested here a little bit.
 *
 */
public class DefaultStyledDocument_ElementBuffer_UpdateTest extends TestCase {
    private DefaultStyledDocument doc;

    private Element root;

    private ElementBuffer buf;

    private DefaultDocumentEvent event;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        doc.insertString(0, "01234\nabcde", null);
        root = doc.getDefaultRootElement();
    }

    /*
     * DefaultStyledDocument.ElementBuffer.changeUpdate()
     *
     * Checks when DefaultDocumentEvent is filled with edits, and which
     * types of edits are performed during changing of character attributes.
     */
    public void testChangeUpdateChar() {
        buf = doc.new ElementBuffer(root) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void changeUpdate() {
                final List<?> edits = getEdits(event);
                assertEquals(0, edits.size());
                super.changeUpdate();
                assertEquals(0, edits.size());
                assertTrue(event.isInProgress());
            }

            @Override
            public void change(int offset, int length, DefaultDocumentEvent e) {
                event = e;
                final List<?> edits = getEdits(event);
                assertSame(EventType.CHANGE, event.getType());
                assertEquals(0, edits.size());
                super.change(offset, length, event);
                assertEquals(1, edits.size());
            }
        };
        doc.buffer = buf;
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, true);
        doc.setCharacterAttributes(2, 2, attrs, false);
        List<?> edits = getEdits(event);
        assertEquals(2, edits.size());
        assertChange(edits.get(0), root.getElement(0), 0, new int[] { 0, 2, 2, 4, 4, 6 },
                new int[] { 0, 6 });
        AttributeUndoableEdit attrEdit = (AttributeUndoableEdit) edits.get(1);
        assertFalse(attrEdit.isReplacing);
        assertEquals(SimpleAttributeSet.EMPTY, attrEdit.copy);
        assertEquals(attrs, attrEdit.newAttributes);
        assertSame(root.getElement(0).getElement(1), attrEdit.element);
        final Element leaf = root.getElement(0).getElement(1);
        assertEquals(attrs, leaf.getAttributes());
        doc.writeLock();
        try {
            attrEdit.undo();
        } finally {
            doc.writeUnlock();
        }
        assertEquals(SimpleAttributeSet.EMPTY, leaf.getAttributes());
    }

    /*
     * DefaultStyledDocument.ElementBuffer.changeUpdate()
     *
     * Checks when DefaultDocumentEvent is filled with edits, and which
     * types of edits are performed during changing of character attributes.
     */
    public void testChangeUpdateCharDirect() {
        buf = doc.new ElementBuffer(root) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void changeUpdate() {
                final List<?> edits = getEdits(event);
                assertEquals(0, edits.size());
                super.changeUpdate();
                assertEquals(0, edits.size());
                assertTrue(event.isInProgress());
            }

            @Override
            public void change(int offset, int length, DefaultDocumentEvent e) {
                event = e;
                final List<?> edits = getEdits(event);
                assertSame(EventType.CHANGE, event.getType());
                assertEquals(0, edits.size());
                super.change(offset, length, event);
                assertEquals(1, edits.size());
            }
        };
        doc.buffer = buf;
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, true);
        event = doc.new DefaultDocumentEvent(2, 2, EventType.CHANGE);
        doc.writeLock();
        try {
            buf.change(2, 2, event);
        } finally {
            doc.writeUnlock();
        }
        // The call to buf.change is almost like a call
        // doc.setCharacterAttributes(2, 2, SimpleAttributeSet.EMPTY, false);
        // but the latter will also safe attribute changes into
        // AttributeUndoableEdit despite there actually were no changes
        // in attributes.
        List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), root.getElement(0), 0, new int[] { 0, 2, 2, 4, 4, 6 },
                new int[] { 0, 6 });
        final Element leaf = root.getElement(0).getElement(1);
        assertEquals(SimpleAttributeSet.EMPTY, leaf.getAttributes());
    }

    /*
     * DefaultStyledDocument.ElementBuffer.changeUpdate()
     *
     * Checks when DefaultDocumentEvent is filled with edits, and which
     * types of edits are performed during changing of paragraph attributes.
     */
    public void testChangeUpdatePar() {
        buf = doc.new ElementBuffer(root) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void changeUpdate() {
                fail("ElementBuffer.changeUpdate is not expected");
            }

            @Override
            public void change(int offset, int length, DefaultDocumentEvent e) {
                fail("ElementBuffer.change is not expected");
            }
        };
        doc.buffer = buf;
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, true);
        doc.addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                event = (DefaultDocumentEvent) e;
            }

            public void insertUpdate(DocumentEvent e) {
                fail("DocumentListener.insertUpdate is not expected");
            }

            public void removeUpdate(DocumentEvent e) {
                fail("DocumentListener.removeUpdate is not expected");
            }
        });
        doc.setParagraphAttributes(2, 2, attrs, false);
        List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        AttributeUndoableEdit attrEdit = (AttributeUndoableEdit) edits.get(0);
        assertFalse(attrEdit.isReplacing);
        assertEquals(1, attrEdit.copy.getAttributeCount());
        assertTrue(attrEdit.copy.isDefined(AttributeSet.ResolveAttribute));
        assertEquals(attrs, attrEdit.newAttributes);
        assertSame(root.getElement(0), attrEdit.element);
    }

    /*
     * DefaultStyledDocument.ElementBuffer.insertUpdate(ElementSpec[])
     */
    public void testInsertUpdate() throws BadLocationException {
        buf = doc.new ElementBuffer(root) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void insertUpdate(ElementSpec[] spec) {
                final List<?> edits = getEdits(event);
                assertEquals(0, edits.size());
                super.insertUpdate(spec);
                assertEquals(0, edits.size());
                assertTrue(event.isInProgress());
            }

            @Override
            public void insert(int offset, int length, ElementSpec[] spec,
                    DefaultDocumentEvent event) {
                final List<?> edits = getEdits(event);
                assertEquals(0, edits.size());
                super.insert(offset, length, spec, event);
                assertEquals(3, edits.size());
            }
        };
        doc.buffer = buf;
        //        The specs are:
        //        Content:JoinPrevious:1,
        //        EndTag:Originate:0,
        //        StartTag:Fracture:0,
        //        Content:JoinNext:4
        //        It is the same as:
        //        doc.insertString(doc.getLength(), "\nthird", null);
        //
        //        Document contains: "01234\nabcde\n"
        //        Offsets             012345 678901 2
        //                            0          1
        //        Paragraphs          000000 111111
        //        After the text is inserted:
        //                           "01234\nabcde\nthird\n"
        //        Offsets:            012345 678901 234567 8
        //                            0          1
        //        Paragraphs           000000 111111 222222
        //
        //        doc.dump(System.out);
        //<section>
        //  <paragraph
        //    resolver=NamedStyle:default {name=default,}
        //  >
        //    <content>
        //      [0,6][01234
        //]
        //  <paragraph
        //    resolver=NamedStyle:default {name=default,}
        //  >
        //    <content>
        //      [6,12][abcde
        //]
        //  <paragraph
        //    resolver=NamedStyle:default {name=default,}
        //  >
        //    <content>
        //      [12,18][third
        //]
        ElementSpec[] specs = {
                new ElementSpec(SimpleAttributeSet.EMPTY, ElementSpec.ContentType, 1),
                new ElementSpec(null, ElementSpec.EndTagType, 0),
                new ElementSpec(root.getElement(1).getAttributes(), ElementSpec.StartTagType, 0),
                new ElementSpec(SimpleAttributeSet.EMPTY, ElementSpec.ContentType, 5) };
        specs[0].setDirection(ElementSpec.JoinPreviousDirection);
        specs[2].setDirection(ElementSpec.JoinFractureDirection);
        specs[3].setDirection(ElementSpec.JoinNextDirection);
        event = doc.new DefaultDocumentEvent(doc.getLength(), 6, EventType.INSERT);
        int off = doc.getLength();
        // Insert the text into the content, so that ElementBuffer
        // could use proper offsets (or BadLocationException may be thrown).
        doc.getContent().insertString(off, "\nthird");
        doc.writeLock();
        try {
            buf.insert(off, 6, specs, event);
        } finally {
            doc.writeUnlock();
        }
        List<?> edits = getEdits(event);
        assertEquals(3, edits.size());
        assertChange(edits.get(0), root.getElement(1), 0, new int[] { 6, 12 }, new int[] { 6,
                18 });
        assertChange(edits.get(1), root.getElement(2), 0, new int[] { 12, 18 }, new int[] { 17,
                18 });
        assertChange(edits.get(2), root, 2, new int[] { 12, 18 }, new int[] {});
    }

    /*
     * DefaultStyledDocument.ElementBuffer.removeUpdate()
     *
     * Remove a portion so that no structural changes are needed.
     */
    public void testRemoveUpdateNoStrucChange() {
        buf = doc.new ElementBuffer(root) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void removeUpdate() {
                final List<?> edits = getEdits(event);
                assertEquals(0, edits.size());
                super.removeUpdate();
                assertEquals(0, edits.size());
                assertTrue(event.isInProgress());
            }

            @Override
            public void remove(int offset, int length, DefaultDocumentEvent e) {
                event = e;
                final List<?> edits = getEdits(event);
                assertSame(EventType.REMOVE, event.getType());
                assertEquals(0, edits.size());
                super.remove(offset, length, event);
                assertEquals(0, edits.size());
            }
        };
        doc.buffer = buf;
        event = doc.new DefaultDocumentEvent(2, 2, EventType.REMOVE);
        doc.writeLock();
        try {
            buf.remove(2, 2, event);
        } finally {
            doc.writeUnlock();
        }
        List<?> edits = getEdits(event);
        assertEquals(0, edits.size());
    }

    /*
     * DefaultStyledDocument.ElementBuffer.removeUpdate()
     *
     * Remove a portion so that the document structure is changed.
     */
    public void testRemoveUpdateStrucChange() {
        buf = doc.new ElementBuffer(root) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void removeUpdate() {
                final List<?> edits = getEdits(event);
                assertEquals(0, edits.size());
                super.removeUpdate();
                assertEquals(0, edits.size());
                assertTrue(event.isInProgress());
            }

            @Override
            public void remove(int offset, int length, DefaultDocumentEvent e) {
                event = e;
                final List<?> edits = getEdits(event);
                assertSame(EventType.REMOVE, event.getType());
                assertEquals(0, edits.size());
                super.remove(offset, length, event);
                assertEquals(1, edits.size());
            }
        };
        doc.buffer = buf;
        event = doc.new DefaultDocumentEvent(2, 2, EventType.REMOVE);
        doc.writeLock();
        try {
            buf.remove(4, 3, event);
        } finally {
            doc.writeUnlock();
        }
        List<?> edits = getEdits(event);
        assertEquals(1, edits.size());
        assertChange(edits.get(0), root, 0, new int[] { 0, 12 }, new int[] { 0, 6, 6, 12 });
    }

    private static void assertChange(final Object change, final Element element,
            final int index, final int[] addedOffsets, final int[] removedOffsets) {
        DefStyledDoc_Helpers.assertChange(change, element, index, removedOffsets, addedOffsets);
    }

    private static List<?> getEdits(final DefaultDocumentEvent event) {
        return DefStyledDoc_Helpers.getEdits(event);
    }
}
