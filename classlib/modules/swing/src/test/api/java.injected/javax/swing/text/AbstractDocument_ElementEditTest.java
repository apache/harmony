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

import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.AbstractDocument.ElementEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import junit.framework.TestCase;

/**
 * All test methods should take into account that start and end positions of
 * an element are moved as the document is mutated (text is changed).
 *
 */
public class AbstractDocument_ElementEditTest extends TestCase {
    AbstractDocument doc;

    DefaultDocumentEvent insert;

    DefaultDocumentEvent remove;

    ElementEdit insertEdit;

    ElementEdit removeEdit;

    Element root;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void insertUpdate(final DefaultDocumentEvent event,
                    final AttributeSet attrs) {
                insert = event;
                //if (getName() == "testGetChildrenRemoved")
                //    System.out.println(getName());
                super.insertUpdate(event, attrs);
                insertEdit = (ElementEdit) insert.getChange(root);
            }

            @Override
            protected void removeUpdate(final DefaultDocumentEvent event) {
                remove = event;
                super.removeUpdate(event);
            }

            @Override
            protected void postRemoveUpdate(final DefaultDocumentEvent event) {
                assertSame(remove, event);
                assertNull(remove.getChange(root));
                super.postRemoveUpdate(event);
                removeEdit = (ElementEdit) remove.getChange(root);
            }
        };
        root = doc.getBidiRootElement();
        doc.insertString(0, "01234" + "\u05DC\u05DD\u05DE\u05DF\u05E0", null);
        doc.remove(3, 4);
    }

    public void testUndo01() throws BadLocationException {
        removeEdit.undo();
        assertEquals(2, root.getElementCount());
        assertEquals(2, getBidiLevel(root.getElement(0)));
        assertEquals(1, getBidiLevel(root.getElement(1)));
        insertEdit.undo();
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        // The text is not affected with these undoes
        assertEquals("012\u05DE\u05DF\u05E0", doc.getText(0, doc.getLength()));
        try {
            insertEdit.undo();
            fail("CannotUndoException should be thrown");
        } catch (CannotUndoException e) {
        }
    }

    /**
     * Tests that added and removed children change places when undoing and
     * redoing document structure changes.
     */
    public void testUndo02() throws BadLocationException {
        Element[] added = removeEdit.getChildrenAdded();
        Element[] removed = removeEdit.getChildrenRemoved();
        removeEdit.undo();
        // The added and removed children must change their places after undo
        assertSame(added, removeEdit.getChildrenRemoved());
        assertSame(removed, removeEdit.getChildrenAdded());
        removeEdit.redo();
        // The children must return to thier places after redo
        assertSame(added, removeEdit.getChildrenAdded());
        assertSame(removed, removeEdit.getChildrenRemoved());
    }

    public void testRedo() throws BadLocationException {
        removeEdit.undo();
        insertEdit.undo();
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        // The text is not affected with these undoes
        assertEquals("012\u05DE\u05DF\u05E0", doc.getText(0, doc.getLength()));
        insertEdit.redo();
        assertEquals(2, root.getElementCount());
        assertEquals(2, getBidiLevel(root.getElement(0)));
        assertEquals(1, getBidiLevel(root.getElement(1)));
        removeEdit.redo();
        assertEquals(2, root.getElementCount());
        assertEquals(2, getBidiLevel(root.getElement(0)));
        assertEquals(1, getBidiLevel(root.getElement(1)));
        try {
            removeEdit.redo();
            fail("CannotRedoException should be thrown");
        } catch (CannotRedoException e) {
        }
    }

    public void testElementEdit() {
        Element[] inserted = new Element[] { doc.new BranchElement(null, null),
                doc.new BranchElement(null, null) };
        Element[] removed = new Element[] { doc.new LeafElement(null, null, 0, 1),
                doc.new LeafElement(null, null, 1, 2), doc.new LeafElement(null, null, 2, 3), };
        AbstractDocument.ElementEdit edit = new AbstractDocument.ElementEdit(doc
                .getBidiRootElement(), 1, removed, inserted);
        assertSame(doc.getBidiRootElement(), edit.getElement());
        assertSame(inserted, edit.getChildrenAdded());
        assertSame(removed, edit.getChildrenRemoved());
        assertEquals(1, edit.getIndex());
    }

    private static int getBidiLevel(final Element e) {
        return ((Integer) e.getAttributes().getAttribute(StyleConstants.BidiLevel)).intValue();
    }

    public void testGetChildrenRemoved() {
        Element[] removed = insertEdit.getChildrenRemoved();
        assertEquals(1, removed.length);
        assertTrue(removed[0].isLeaf());
        assertEquals(0, removed[0].getStartOffset());
        // The last position in the document became 7 after remove (was 1, 11)
        assertEquals(7, removed[0].getEndOffset());
        assertEquals(0, getBidiLevel(removed[0]));
        //((AbstractElement)removed[0]).dump(System.out, 0);
        removed = removeEdit.getChildrenRemoved();
        assertEquals(2, removed.length);
        assertTrue(removed[0].isLeaf());
        assertEquals(0, removed[0].getStartOffset());
        // The position between two text direction is 3 after remove (was 5)
        assertEquals(3, removed[0].getEndOffset());
        // The sequence of numeral gets level 2 (it seems like the
        // bidi-algorithm considers them embedded in a RTL run).
        assertEquals(2, getBidiLevel(removed[0]));
        assertTrue(removed[1].isLeaf());
        assertEquals(3, removed[1].getStartOffset());
        assertEquals(7, removed[1].getEndOffset());
        assertEquals(1, getBidiLevel(removed[1]));
    }

    public void testGetChildrenAdded() {
        Element[] added = insertEdit.getChildrenAdded();
        assertEquals(2, added.length);
        assertTrue(added[0].isLeaf());
        assertEquals(0, added[0].getStartOffset());
        assertEquals(3, added[0].getEndOffset());
        assertEquals(2, getBidiLevel(added[0]));
        assertTrue(added[1].isLeaf());
        assertEquals(3, added[1].getStartOffset());
        assertEquals(7, added[1].getEndOffset());
        assertEquals(1, getBidiLevel(added[1]));
        added = removeEdit.getChildrenAdded();
        assertEquals(2, added.length);
        assertTrue(added[0].isLeaf());
        assertEquals(0, added[0].getStartOffset());
        assertEquals(3, added[0].getEndOffset());
        assertEquals(2, getBidiLevel(added[0]));
        assertTrue(added[1].isLeaf());
        assertEquals(3, added[1].getStartOffset());
        assertEquals(7, added[1].getEndOffset());
        assertEquals(1, getBidiLevel(added[1]));
    }

    public void testGetElement() {
        assertSame(root, insertEdit.getElement());
        assertSame(root, removeEdit.getElement());
    }

    public void testGetIndex() throws BadLocationException {
        assertEquals(0, insertEdit.getIndex());
        assertEquals(0, removeEdit.getIndex());
        doc.insertString(6, "\nab\ncd\ne", null);
        doc.insertString(doc.getLength(), "\nnew line", null);
        AbstractDocument.ElementEdit e = (AbstractDocument.ElementEdit) insert.getChange(doc
                .getDefaultRootElement());
        // We've inserted 4 paragraphs of text, then indexes of elements
        // representing them are 0 - 3. When we insert a new paragraph, the
        // element representing the latest paragraph is modified. Hence it
        // is removed and then added again. (Also a new paragraph is added
        // during this process.)
        // Thus the index should be 3 since the first element modified is at 3.
        assertEquals(3, e.getIndex());
    }
}
