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

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocumentTest.DisAbstractedDocument;
import junit.framework.TestCase;

/**
 * Tests for bug:
 * AbstractDocument blocks if an exception is thrown from a listener code.
 *
 */
public class AbstractDocument_ListenerLockTest extends TestCase implements DocumentListener,
        UndoableEditListener {
    private AbstractDocument doc;

    private static class LockTestError extends Error {
        private static final long serialVersionUID = 1L;

        public LockTestError(String string) {
            super(string);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DisAbstractedDocument(new GapContent());
        doc.insertString(0, "test", null);
        doc.addDocumentListener(this);
        doc.addUndoableEditListener(this);
        doc.writeLock();
    }

    public void testFireInsertUpdate() throws Exception {
        try {
            doc.fireInsertUpdate(null);
        } catch (LockTestError e) {
        }
        unlockAndLock();
    }

    public void testFireRemoveUpdate() throws Exception {
        try {
            doc.fireRemoveUpdate(null);
        } catch (LockTestError e) {
        }
        unlockAndLock();
    }

    public void testFireChangedUpdate() throws Exception {
        try {
            doc.fireChangedUpdate(null);
        } catch (LockTestError e) {
        }
        unlockAndLock();
    }

    public void testFireUndoableEditUpdate() throws Exception {
        try {
            doc.fireUndoableEditUpdate(null);
        } catch (LockTestError e) {
        }
        unlockAndLock();
    }

    public void insertUpdate(DocumentEvent e) {
        throwException();
    }

    public void removeUpdate(DocumentEvent e) {
        throwException();
    }

    public void changedUpdate(DocumentEvent e) {
        throwException();
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        throwException();
    }

    private void throwException() {
        throw new LockTestError("Test exception");
    }

    private void unlockAndLock() throws BadLocationException {
        doc.writeUnlock();
        doc.readLock();
        doc.getText(0, doc.getLength());
        doc.readUnlock();
        doc.writeLock();
    }
}
