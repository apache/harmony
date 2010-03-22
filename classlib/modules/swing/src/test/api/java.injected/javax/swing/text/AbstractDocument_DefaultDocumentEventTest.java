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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Vector;
import javax.swing.BasicSwingTestCase;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.AbstractDocument.ElementEdit;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import junit.framework.TestCase;

/**
 * This class tests functions of AbstractDocument.DefaultDocumentEvent.
 *
 */
public class AbstractDocument_DefaultDocumentEventTest extends TestCase implements
        DocumentListener, UndoableEditListener {
    /**
     * Document object used in tests.
     */
    AbstractDocument doc;

    /**
     * Event for text insert.
     */
    DefaultDocumentEvent insert;

    /**
     * Event for text remove.
     */
    DefaultDocumentEvent remove;

    /**
     * Event for change in attributes.
     */
    DefaultDocumentEvent change;

    /**
     * Undoable event.
     */
    UndoableEditEvent undoEvent;

    private static final String UNDO_TEXT_KEY = "AbstractDocument.undoText";

    private static final String REDO_TEXT_KEY = "AbstractDocument.redoText";

    private String undoName;

    private String redoName;

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
                assertTrue(insert.isInProgress());
                super.insertUpdate(event, attrs);
            }

            @Override
            protected void removeUpdate(final DefaultDocumentEvent event) {
                remove = event;
                assertTrue(remove.isInProgress());
                super.removeUpdate(event);
            }

            @Override
            protected void postRemoveUpdate(final DefaultDocumentEvent event) {
                assertSame(remove, event);
                super.postRemoveUpdate(event);
                assertTrue(remove.isInProgress());
            }
        };
        doc.insertString(0, "01234" + "\u05DC\u05DD\u05DE\u05DF\u05E0", null);
        doc.remove(3, 4);
        change = doc.new DefaultDocumentEvent(0, 10, DocumentEvent.EventType.CHANGE);
        undoName = UIManager.getString(UNDO_TEXT_KEY);
        redoName = UIManager.getString(REDO_TEXT_KEY);
    }

    /**
     * Test of basic functionality of addEdit method.
     * @throws BadLocationException
     */
    public void testAddEdit01() throws BadLocationException {
        // insert.isInProgress is false, so we can add no more edits
        assertFalse(insert.addEdit(remove));
        DefaultDocumentEvent edit = doc.new DefaultDocumentEvent(0, 10,
                DocumentEvent.EventType.CHANGE);
        assertTrue(edit.addEdit(insert));
        assertTrue(edit.addEdit(remove));
        assertEquals(2, getEdits(edit).size());
        // Stop collecting and make undo
        edit.end();
        edit.undo();
        // The document should be in its intial state
        assertEquals("", doc.getText(0, doc.getLength()));
    }

    private static HashMap<?, ?> getChanges(final DefaultDocumentEvent event) {
        try {
            Field f = event.getClass().getDeclaredField("changes");
            f.setAccessible(true);
            return (HashMap<?, ?>) (f.get(event));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Internal only test to test switching to hashtable storage.
     */
    public void testAddEdit02() {
        // Used as parent for Edits
        final class Parent extends BranchElement {
            private static final long serialVersionUID = 1L;

            public Parent() {
                doc.super(null, null);
            }
        }
        // Element edits
        final class Child extends ElementEdit {
            private static final long serialVersionUID = 1L;

            public boolean undone = false;

            public boolean redone = false;

            public Child(final Element parent) {
                super(parent, 0, null, null);
            }

            @Override
            public void undo() {
                undone = true;
            }

            @Override
            public void redo() {
                redone = true;
            }
        }
        DefaultDocumentEvent edit = doc.new DefaultDocumentEvent(0, 10, EventType.CHANGE);
        Parent[] parent = new Parent[15];
        Child[] child = new Child[parent.length];
        for (int i = 0; i < parent.length; i++) {
            assertTrue("Can't add edit at " + i, edit.addEdit(child[i] = new Child(
                    parent[i] = new Parent())));
            if (BasicSwingTestCase.isHarmony()) {
                if (i < 10) {
                    assertNull(getChanges(edit));
                } else {
                    assertNotNull(getChanges(edit));
                }
            }
        }
        edit.addEdit((UndoableEdit) getEdits(edit).get(0));
        edit.end();
        // A new edit shouldn't be added 'cause we have called end()
        assertFalse(edit.addEdit((UndoableEdit) getEdits(edit).get(1)));
        edit.undo();
        edit.redo();
        // Do the check
        for (int i = 0; i < parent.length; i++) {
            ElementChange change = edit.getChange(parent[i]);
            assertSame("Objects are not same at " + i, child[i], change);
            assertTrue("Undo didn't get called at " + i, child[i].undone);
            assertTrue("Redo didn't get called at " + i, child[i].redone);
        }
        assertEquals(16, getEdits(edit).size());
    }

    private static Vector<?> getEdits(final DefaultDocumentEvent event) {
        try {
            Class<?> eventSuperClass = event.getClass().getSuperclass();
            Field f = eventSuperClass.getDeclaredField("edits");
            f.setAccessible(true);
            return (Vector<?>) (f.get(event));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * Class under test for String toString()
     */
    public void testToString() {
        String ref = "[";
        Vector<?> edits = getEdits(insert);
        for (int i = 0; i < edits.size(); i++) {
            AbstractUndoableEdit edit = (AbstractUndoableEdit) edits.get(i);
            if (i != 0) {
                ref += ", ";
            }
            ref += edit.toString();
        }
        ref += "]";
        assertEquals(ref, insert.toString());
    }

    public void testGetUndoPresentationName() {
        assertEquals(undoName + " " + insert.getPresentationName(), insert
                .getUndoPresentationName());
        assertEquals(undoName + " " + remove.getPresentationName(), remove
                .getUndoPresentationName());
        assertEquals(undoName + " " + change.getPresentationName(), change
                .getUndoPresentationName());
    }

    public void testGetUndoPresentationNameModified() {
        UIManager.put(UNDO_TEXT_KEY, "ODNU");
        try {
            assertEquals("ODNU " + UIManager.getString("AbstractDocument.additionText"), insert
                    .getUndoPresentationName());
        } finally {
            UIManager.put(UNDO_TEXT_KEY, undoName);
        }
    }

    public void testGetRedoPresentationName() {
        assertEquals(redoName + " " + insert.getPresentationName(), insert
                .getRedoPresentationName());
        assertEquals(redoName + " " + remove.getPresentationName(), remove
                .getRedoPresentationName());
        assertEquals(redoName + " " + change.getPresentationName(), change
                .getRedoPresentationName());
    }

    public void testGetRedoPresentationNameModified() {
        UIManager.put(REDO_TEXT_KEY, "ODER");
        try {
            assertEquals("ODER " + UIManager.getString("AbstractDocument.additionText"), insert
                    .getRedoPresentationName());
        } finally {
            UIManager.put(REDO_TEXT_KEY, redoName);
        }
    }

    public void testGetPresentationName() {
        assertSame(UIManager.getString("AbstractDocument.additionText"), insert
                .getPresentationName());
        assertSame(UIManager.getString("AbstractDocument.deletionText"), remove
                .getPresentationName());
        assertSame(UIManager.getString("AbstractDocument.styleChangeText"), change
                .getPresentationName());
    }

    public void testIsSignificant() {
        assertTrue(insert.isSignificant());
        assertTrue(remove.isSignificant());
    }

    public void testUndo01() throws BadLocationException {
        remove.undo();
        insert.undo();
        assertEquals("", doc.getText(0, doc.getLength()));
        assertEquals(1, doc.getBidiRootElement().getElementCount());
        try {
            insert.undo();
            fail("CannotUndoException should be thrown");
        } catch (CannotUndoException e) {
        }
    }

    public void testUndo02() throws BadLocationException {
        final class UndoPlainDocument extends PlainDocument {
            private static final long serialVersionUID = 1L;

            BranchElement altRoot = new BranchElement(null, null);

            boolean undone = false;

            boolean redone = false;

            @Override
            protected void insertUpdate(final DefaultDocumentEvent event,
                    final AttributeSet attrs) {
                insert = event;
                super.insertUpdate(event, attrs);
                event.addEdit(new ElementEdit(altRoot, 0, new Element[0], new Element[0]) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void undo() {
                        super.undo();
                        assertSame(Thread.currentThread(), getCurrentWriter());
                        undone = true;
                    }

                    @Override
                    public void redo() {
                        super.redo();
                        assertSame(Thread.currentThread(), getCurrentWriter());
                        redone = true;
                    }
                });
            }
        }
        ;
        doc = new UndoPlainDocument();
        doc.insertString(0, "test", null);
        ElementChange change = insert.getChange(((UndoPlainDocument) doc).altRoot);
        assertNotNull(change);
        assertEquals(0, change.getChildrenAdded().length);
        assertEquals(0, change.getChildrenRemoved().length);
        // Additional assertions are in one of undo methods()
        insert.undo();
        assertTrue(((UndoPlainDocument) doc).undone);
        assertFalse(((UndoPlainDocument) doc).redone);
        // Additional assertions are in one of redo methods()
        insert.redo();
        assertTrue(((UndoPlainDocument) doc).redone);
    }

    public void testUndo03() throws BadLocationException {
        doc.remove(0, doc.getLength());
        doc.addDocumentListener(this);
        doc.addUndoableEditListener(this);
        doc.insertString(0, "01234" + "\u05DC\u05DD\u05DE\u05DF\u05E0", null);
        doc.remove(3, 4);
        // Save what we've got
        DefaultDocumentEvent insertEvent = insert;
        //Element insertChildrenAdded[] = insert.
        DefaultDocumentEvent removeEvent = remove;
        assertSame(undoEvent.getEdit(), remove);
        insert = null;
        remove = null;
        undoEvent = null;
        // Try to undo
        removeEvent.undo();
        assertNull(remove);
        assertNotNull(insert);
        assertNull(undoEvent);
        DefaultDocumentEvent undoInsertEvent = insert;
        insert = null;
        insertEvent.undo();
        assertNotNull(remove);
        assertNull(insert);
        assertNull(undoEvent);
        DefaultDocumentEvent undoRemoveEvent = remove;
        remove = null;
        assertFalse(undoInsertEvent.canUndo());
        assertTrue(undoInsertEvent.canRedo());
        assertSame(insertEvent, undoRemoveEvent);
        assertSame(EventType.INSERT, insertEvent.getType());
        assertSame(EventType.INSERT, undoRemoveEvent.getType());
        assertFalse(undoRemoveEvent.canUndo());
        assertTrue(undoRemoveEvent.canRedo());
        assertSame(removeEvent, undoInsertEvent);
        assertSame(EventType.REMOVE, removeEvent.getType());
        assertSame(EventType.REMOVE, undoInsertEvent.getType());
        //undoInsertEvent.undo();
        //insertEvent.redo();
        //removeEvent.redo();
    }

    public void testRedo() throws BadLocationException {
        remove.undo();
        insert.undo();
        assertEquals("", doc.getText(0, doc.getLength()));
        insert.redo();
        remove.redo();
        assertEquals("012\u05DE\u05DF\u05E0", doc.getText(0, doc.getLength()));
        assertEquals(2, doc.getBidiRootElement().getElementCount());
        try {
            remove.redo();
            fail("CannotRedoException should be thrown");
        } catch (CannotRedoException e) {
        }
    }

    public void testDefaultDocumentEvent() {
        DefaultDocumentEvent event = doc.new DefaultDocumentEvent(10, 7,
                DocumentEvent.EventType.CHANGE);
        assertEquals(10, event.getOffset());
        assertEquals(7, event.getLength());
        assertSame(DocumentEvent.EventType.CHANGE, event.getType());
    }

    /**
     * The method getChange is used in setUp of
     * AbstractDocument_ElementEditTest.
     */
    public void testGetChange() {
        assertTrue(insert.getChange(doc.getBidiRootElement()) instanceof ElementEdit);
        assertNull(insert.getChange(doc.getDefaultRootElement()));
    }

    public void testGetDocument() {
        assertSame(doc, insert.getDocument());
        assertSame(doc, remove.getDocument());
    }

    public void testGetType() {
        assertSame(DocumentEvent.EventType.INSERT, insert.getType());
        assertSame(DocumentEvent.EventType.REMOVE, remove.getType());
    }

    public void testGetOffset() {
        assertEquals(0, insert.getOffset());
        assertEquals(3, remove.getOffset());
    }

    public void testGetLength() {
        assertEquals(10, insert.getLength());
        assertEquals(4, remove.getLength());
    }

    public void changedUpdate(final DocumentEvent event) {
        fail("changeUpdate isn't supposed to be called");
    }

    public void insertUpdate(final DocumentEvent event) {
        insert = (DefaultDocumentEvent) event;
    }

    public void removeUpdate(final DocumentEvent event) {
        remove = (DefaultDocumentEvent) event;
    }

    public void undoableEditHappened(final UndoableEditEvent event) {
        undoEvent = event;
    }
}
