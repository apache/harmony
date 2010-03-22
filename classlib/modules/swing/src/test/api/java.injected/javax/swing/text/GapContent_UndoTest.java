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
import javax.swing.BasicSwingTestCase;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import junit.framework.TestCase;

public class GapContent_UndoTest extends TestCase {
    protected AbstractDocument.Content content;

    protected UndoableEdit insertEdit;

    @Override
    protected void setUp() throws Exception {
        content = new GapContent();
        insertEdit = content.insertString(0, "01234");
    }

    public void testUndoInsertEnd() throws BadLocationException {
        UndoableEdit ue = content.insertString(5, "567");
        ue.undo();
        assertEquals("01234\n", content.getString(0, content.length()));
    }

    public void testUndoInsertStart() throws BadLocationException {
        UndoableEdit ue = content.insertString(0, "321");
        ue.undo();
        assertEquals("01234\n", content.getString(0, content.length()));
    }

    public void testUndoInsertMiddle() throws BadLocationException {
        UndoableEdit ue = content.insertString(3, "999");
        ue.undo();
        assertEquals("01234\n", content.getString(0, content.length()));
    }

    public void testUndoRemoveEnd() throws BadLocationException {
        UndoableEdit ue = content.remove(3, 2);
        ue.undo();
        assertEquals("01234\n", content.getString(0, content.length()));
    }

    public void testUndoRemoveStart() throws BadLocationException {
        UndoableEdit ue = content.remove(0, 2);
        ue.undo();
        assertEquals("01234\n", content.getString(0, content.length()));
    }

    public void testUndoRemoveMiddle() throws BadLocationException {
        UndoableEdit ue = content.remove(3, 2);
        ue.undo();
        assertEquals("01234\n", content.getString(0, content.length()));
    }

    public void testUndoPresentationInsertName() {
        final String name = BasicSwingTestCase.isHarmony() ? "addition" : "";
        assertEquals(name, insertEdit.getPresentationName());
    }

    public void testUndoPresentationRemoveName() throws BadLocationException {
        final UndoableEdit ue = content.remove(0, 1);
        final String name = BasicSwingTestCase.isHarmony() ? "deletion" : "";
        assertEquals(name, ue.getPresentationName());
    }

    /**
     * @return DocumentEdit.text
     */
    private static String getDEText(final UndoableEdit ue) {
        try {
            Field f = ue.getClass().getSuperclass().getDeclaredField("text");
            f.setAccessible(true);
            return (String) (f.get(ue));
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        }
        return null;
    }

    private static Object getDEUndoPos(final UndoableEdit ue) {
        try {
            Field f = ue.getClass().getSuperclass().getDeclaredField("undoPos");
            f.setAccessible(true);
            return f.get(ue);
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        }
        return null;
    }

    public void testDieText() {
        if (BasicSwingTestCase.isHarmony()) {
            insertEdit.die();
            assertNull(getDEText(insertEdit));
        }
    }

    public void testDiePos() {
        if (BasicSwingTestCase.isHarmony()) {
            insertEdit.die();
            assertNull(getDEUndoPos(insertEdit));
        }
    }

    public void testDieCanRedo() {
        insertEdit.die();
        assertFalse(insertEdit.canRedo());
    }

    public void testDieCanUndo() {
        insertEdit.die();
        assertFalse(insertEdit.canUndo());
    }

    public void testCanRedo() {
        assertFalse(insertEdit.canRedo());
    }

    public void testCanRedoUndone() {
        insertEdit.undo();
        assertTrue(insertEdit.canRedo());
    }

    public void testCanUndo() {
        assertTrue(insertEdit.canUndo());
    }

    public void testCanUndoUndone() {
        insertEdit.undo();
        assertFalse(insertEdit.canUndo());
    }

    public void testCantUndo() {
        insertEdit.undo();
        try {
            insertEdit.undo();
            fail("CannotUndoException should be thrown");
        } catch (CannotUndoException e) {
        }
    }

    public void testCantRedo() {
        try {
            insertEdit.redo();
            fail("CannotRedoException should be thrown");
        } catch (CannotRedoException e) {
        }
    }
}
