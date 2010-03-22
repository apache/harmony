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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.undo;

import javax.swing.BasicSwingTestCase;
import javax.swing.UIManager;
import junit.framework.TestCase;

public class AbstractUndoableEditTest extends TestCase {
    private static final String UNDO_NAME_KEY = "AbstractUndoableEdit.undoText";

    private static final String REDO_NAME_KEY = "AbstractUndoableEdit.redoText";

    protected AbstractUndoableEdit obj;

    private String defaultUndoName;

    private String defaultRedoName;

    @Override
    protected void setUp() throws Exception {
        obj = new AbstractUndoableEdit();
        defaultUndoName = UIManager.getString(UNDO_NAME_KEY);
        defaultRedoName = UIManager.getString(REDO_NAME_KEY);
    }

    @Override
    protected void tearDown() throws Exception {
        UIManager.put(UNDO_NAME_KEY, defaultUndoName);
        UIManager.put(REDO_NAME_KEY, defaultRedoName);
    }

    public void testToString() {
        String s = obj.toString();
        assertNotNull(s);
        assertNotSame("", s);
    }

    protected boolean isAlive(final Object o) {
        return o.toString().indexOf("alive: true") != -1;
    }

    protected boolean hasBeenDone(final Object o) {
        return o.toString().indexOf("hasBeenDone: true") != -1;
    }

    public void testAbstractUndoableEdit() {
        if (BasicSwingTestCase.isHarmony()) {
            assertTrue(isAlive(obj));
        }
    }

    public void testDie() {
        obj.die();
        if (BasicSwingTestCase.isHarmony()) {
            assertFalse(isAlive(obj));
        }
        boolean wasException = false;
        try {
            obj.redo();
        } catch (CannotRedoException e) {
            wasException = true;
        }
        assertTrue("CannotRedoException was expected", wasException);
        wasException = false;
        try {
            obj.undo();
        } catch (CannotUndoException e) {
            wasException = true;
        }
        assertTrue("CannotUndoException was expected", wasException);
    }

    public void testRedo() {
        obj.undo();
        obj.redo();
        if (BasicSwingTestCase.isHarmony()) {
            assertTrue(hasBeenDone(obj));
        }
        if (!obj.canRedo()) {
            boolean wasException = false;
            try {
                obj.redo();
            } catch (CannotRedoException e) {
                wasException = true;
            }
            assertTrue("CannotRedoException was expected", wasException);
        }
    }

    public void testUndo() {
        obj.undo();
        if (BasicSwingTestCase.isHarmony()) {
            assertFalse(hasBeenDone(obj));
        }
        if (!obj.canUndo()) {
            boolean wasException = false;
            try {
                obj.undo();
            } catch (CannotUndoException e) {
                wasException = true;
            }
            assertTrue("CannotUndoException was expected", wasException);
        }
    }

    public void testCanRedo() {
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(isAlive(obj) && !hasBeenDone(obj), obj.canRedo());
        }
        obj.die();
        assertFalse(obj.canRedo());
    }

    public void testCanUndo() {
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(isAlive(obj) && hasBeenDone(obj), obj.canUndo());
        }
        obj.die();
        assertFalse(obj.canUndo());
    }

    public void testIsSignificant() {
        assertTrue(obj.isSignificant());
    }

    public void testGetPresentationName() {
        assertEquals("", obj.getPresentationName());
    }

    public void testGetRedoPresentationName() {
        assertEquals(UIManager.getString(REDO_NAME_KEY), obj.getRedoPresentationName());
    }

    public void testGetRedoPresentationNameModified() {
        String redoName = "name of Redo";
        UIManager.put(REDO_NAME_KEY, redoName);
        assertEquals(redoName, obj.getRedoPresentationName());
        redoName = "alternative redo";
        UIManager.put(REDO_NAME_KEY, redoName);
        assertEquals(redoName, obj.getRedoPresentationName());
    }

    public void testGetUndoPresentationName() {
        assertEquals(UIManager.getString(UNDO_NAME_KEY), obj.getUndoPresentationName());
    }

    public void testGetUndoPresentationNameModified() {
        String undoName = "name of Undo";
        UIManager.put(UNDO_NAME_KEY, undoName);
        assertEquals(undoName, obj.getUndoPresentationName());
        undoName = "alternative undo";
        UIManager.put(UNDO_NAME_KEY, undoName);
        assertEquals(undoName, obj.getUndoPresentationName());
    }

    public void testGetUndoPresentationNameNull() {
        obj = new AbstractUndoableEdit() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getPresentationName() {
                return null;
            }
        };
        assertEquals(UIManager.getString(UNDO_NAME_KEY), obj.getUndoPresentationName());
    }

    public void testAddEdit() {
        assertFalse(obj.addEdit(null));
    }

    public void testReplaceEdit() {
        assertFalse(obj.replaceEdit(null));
    }
}
