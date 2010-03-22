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

import java.util.EventListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocumentTest.DisAbstractedDocument;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import junit.framework.TestCase;

public class AbstractDocument_ListenerTest extends TestCase implements DocumentListener,
        UndoableEditListener {
    private AbstractDocument doc;

    DocumentEvent change;

    DocumentEvent insert;

    DocumentEvent remove;

    UndoableEditEvent undo;

    /**
     * Initializes fixture for tests.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DisAbstractedDocument(new GapContent());
        change = null;
        insert = null;
        remove = null;
        undo = null;
    }

    /**
     * Merely saves the event in change field.
     */
    public void changedUpdate(final DocumentEvent event) {
        change = event;
    }

    /**
     * Merely saves the event in insert field.
     */
    public void insertUpdate(final DocumentEvent event) {
        insert = event;
    }

    /**
     * Merely saves the event in remove field.
     */
    public void removeUpdate(final DocumentEvent event) {
        remove = event;
    }

    /**
     * Merely saves the event in undo field.
     */
    public void undoableEditHappened(final UndoableEditEvent event) {
        undo = event;
    }

    /**
     * Checks that the fields, for which array item is true, are not
     * null (i.e. listener has been called).
     * Array elements correspond to fields in this order:
     * change, insert, remove, undo.
     *
     * @param state states of fields
     */
    private void checkCalledEvents(final boolean[] state) {
        if (state[0]) {
            assertNotNull("change IS null", change);
        } else {
            assertNull("change IS NOT null", change);
        }
        if (state[1]) {
            assertNotNull("insert IS null", insert);
        } else {
            assertNull("insert IS NOT null", insert);
        }
        if (state[2]) {
            assertNotNull("remove IS null", remove);
        } else {
            assertNull("remove IS NOT null", remove);
        }
        if (state[3]) {
            assertNotNull("undo IS null", undo);
        } else {
            assertNull("undo IS NOT null", undo);
        }
    }

    /**
     * Helper method which constructs the array before calling its
     * counterpart to perform real checking.
     *
     * @param change true if changeUpdate is supposed to be called
     * @param insert true if insertUpdate is supposed to be called
     * @param remove true if insertUpdate is supposed to be called
     * @param undo   true if undoableEditHapped is supposed to be called
     */
    private void checkCalledEvents(final boolean change, final boolean insert,
            final boolean remove, final boolean undo) {
        checkCalledEvents(new boolean[] { change, insert, remove, undo });
    }

    public void testAddDocumentListener() throws BadLocationException {
        doc.insertString(0, "text", null);
        checkCalledEvents(false, false, false, false);
        doc.addDocumentListener(this);
        doc.insertString(0, "test", null);
        checkCalledEvents(false, true, false, false);
        insert = null;
        doc.remove(0, 4);
        checkCalledEvents(false, false, true, false);
        remove = null;
        doc.replace(2, 1, "s", null);
        checkCalledEvents(false, true, true, false);
        assertNotSame(insert, remove);
    }

    public void testRemoveDocumentListener() throws BadLocationException {
        doc.addDocumentListener(this);
        doc.insertString(0, "text", null);
        checkCalledEvents(false, true, false, false);
        doc.removeDocumentListener(this);
        insert = null;
        doc.insertString(0, "test", null);
        checkCalledEvents(false, false, false, false);
        insert = null;
        doc.remove(0, 4);
        checkCalledEvents(false, false, false, false);
        remove = null;
        doc.replace(2, 1, "s", null);
        checkCalledEvents(false, false, false, false);
    }

    static final DocumentListener docListener = new DocumentListener() {
        public void changedUpdate(final DocumentEvent event) {
        }

        public void insertUpdate(final DocumentEvent event) {
        }

        public void removeUpdate(final DocumentEvent event) {
        }
    };

    static final UndoableEditListener undoListener = new UndoableEditListener() {
        public void undoableEditHappened(final UndoableEditEvent event) {
        }
    };

    public void testGetDocumentListeners() {
        doc.addDocumentListener(this);
        doc.addDocumentListener(docListener);
        doc.addUndoableEditListener(undoListener);
        DocumentListener[] listeners = doc.getDocumentListeners();
        assertEquals(2, listeners.length);
    }

    public void testGetListeners() {
        doc.addDocumentListener(this);
        doc.addDocumentListener(docListener);
        doc.addUndoableEditListener(undoListener);
        EventListener[] listeners;
        listeners = doc.getListeners(DocumentListener.class);
        assertEquals(2, listeners.length);
        listeners = doc.getListeners(UndoableEditListener.class);
        assertEquals(1, listeners.length);
    }

    public void testGetUndoableEditListeners() {
        doc.addDocumentListener(this);
        doc.addDocumentListener(docListener);
        doc.addUndoableEditListener(undoListener);
        UndoableEditListener[] listeners = doc.getUndoableEditListeners();
        assertEquals(1, listeners.length);
    }

    public void testAddUndoableEditListener() throws BadLocationException {
        doc.insertString(0, "text", null);
        checkCalledEvents(false, false, false, false);
        doc.addUndoableEditListener(this);
        doc.insertString(0, "test", null);
        checkCalledEvents(false, false, false, true);
        undo = null;
        doc.remove(0, 4);
        checkCalledEvents(false, false, false, true);
        undo = null;
        doc.replace(2, 1, "s", null);
        checkCalledEvents(false, false, false, true);
    }

    public void testRemoveUndoableEditListener() throws BadLocationException {
        doc.addUndoableEditListener(this);
        doc.insertString(0, "text", null);
        checkCalledEvents(false, false, false, true);
        doc.removeUndoableEditListener(this);
        undo = null;
        doc.insertString(0, "test", null);
        checkCalledEvents(false, false, false, false);
        undo = null;
        doc.remove(0, 4);
        checkCalledEvents(false, false, false, false);
        undo = null;
        doc.replace(2, 1, "s", null);
        checkCalledEvents(false, false, false, false);
    }

    private static DocumentEvent docEvent = new DocumentEvent() {
        public int getLength() {
            return 0;
        }

        public int getOffset() {
            return 0;
        }

        public EventType getType() {
            return null;
        }

        public Document getDocument() {
            return null;
        }

        public ElementChange getChange(final Element element) {
            return null;
        }
    };

    public void testFireUndoableEditUpdate() {
        UndoableEditEvent undoEvent = new UndoableEditEvent(doc, new AbstractUndoableEdit());
        doc.addUndoableEditListener(this);
        doc.fireUndoableEditUpdate(undoEvent);
        checkCalledEvents(false, false, false, true);
        assertSame(undoEvent, undo);
    }

    public void testFireRemoveUpdate() {
        doc.addDocumentListener(this);
        doc.fireRemoveUpdate(docEvent);
        checkCalledEvents(false, false, true, false);
        assertSame(docEvent, remove);
    }

    public void testFireInsertUpdate() {
        doc.addDocumentListener(this);
        doc.fireInsertUpdate(docEvent);
        checkCalledEvents(false, true, false, false);
        assertSame(docEvent, insert);
    }

    public void testFireChangedUpdate() {
        doc.addDocumentListener(this);
        doc.fireChangedUpdate(docEvent);
        checkCalledEvents(true, false, false, false);
        assertSame(docEvent, change);
    }

    /**
     * Tests if listeners get called when inserting empty/null string and
     * when removing zero-length text.
     */
    public void testInsertString01() throws BadLocationException {
        doc.addDocumentListener(this);
        doc.addUndoableEditListener(this);
        doc.insertString(0, "", null);
        checkCalledEvents(false, false, false, false);
        doc.insertString(0, null, null);
        checkCalledEvents(false, false, false, false);
        doc.remove(0, 0);
        checkCalledEvents(false, false, false, false);
    }

    private static class NoUndoContent extends GapContent {
        private static final long serialVersionUID = 1L;

        @Override
        public UndoableEdit insertString(int offset, String str) throws BadLocationException {
            super.insertString(offset, str);
            return null;
        }

        @Override
        public UndoableEdit remove(int offset, int length) throws BadLocationException {
            super.remove(offset, length);
            return null;
        }
    }

    /**
     * Tests which events are fired at text insert
     * when content doesn't support undo.
     */
    public void testInsertString02() throws Exception {
        doc = new DisAbstractedDocument(new NoUndoContent());
        doc.addDocumentListener(this);
        doc.addUndoableEditListener(this);
        doc.insertString(0, "test string\nthe second line", null);
        checkCalledEvents(false, true, false, false);
    }

    /**
     * Tests which events are fired at RTL-text insert
     * when content doesn't support undo.
     */
    public void testInsertString03() throws Exception {
        doc = new DisAbstractedDocument(new NoUndoContent());
        doc.addDocumentListener(this);
        doc.addUndoableEditListener(this);
        doc.insertString(0, "\u05DC\u05DD", null);
        checkCalledEvents(false, true, false, false);
        assertNotNull(insert.getChange(doc.getBidiRootElement()));
    }

    /**
     * Tests which events are fired at text remove
     * when content doesn't support undo.
     */
    public void testRemove01() throws Exception {
        doc = new DisAbstractedDocument(new NoUndoContent());
        doc.insertString(0, "test string\nthe second line", null);
        doc.addDocumentListener(this);
        doc.addUndoableEditListener(this);
        doc.remove(0, 4);
        checkCalledEvents(false, false, true, false);
    }

    /**
     * Tests which events are fired at RTL-text remove
     * when content doesn't support undo.
     */
    public void testRemove02() throws Exception {
        doc = new DisAbstractedDocument(new NoUndoContent());
        doc.insertString(0, "\u05DC\u05DD test string", null);
        doc.addDocumentListener(this);
        doc.addUndoableEditListener(this);
        doc.remove(0, 2);
        checkCalledEvents(false, false, true, false);
        assertNotNull(remove.getChange(doc.getBidiRootElement()));
    }

    /**
     * Tests which events are fired when content support undo for
     * remove but doesn't support one for insert.
     */
    public void testInsertRemove() throws Exception {
        doc = new DisAbstractedDocument(new GapContent() {
            private static final long serialVersionUID = 1L;

            @Override
            public UndoableEdit insertString(int where, String str) throws BadLocationException {
                super.insertString(where, str);
                return null;
            }
        });
        doc.addDocumentListener(this);
        doc.addUndoableEditListener(this);
        doc.insertString(0, "text", null);
        checkCalledEvents(false, true, false, false);
        insert = null;
        doc.remove(0, 2);
        checkCalledEvents(false, false, true, true);
    }
}
