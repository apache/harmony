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
 * @author Alexey A. Ivanov, Roman I. Chernyatchik
 */
package javax.swing.text;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Vector;
import javax.swing.undo.UndoableEdit;

/**
 * Tests StringContent class.
 *
 */
public class StringContentTest extends AbstractDocument_ContentTest {
    private StringContent content;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        obj = new StringContent();
        obj.insertString(0, "This is a test string.");
        content = new StringContent();
        content.insertString(0, "012345");
    }

    @Override
    public void testGetCharsNegativeLength() {
        if (isHarmony()) {
            testExceptionalCase(new BadLocationCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    obj.getChars(0, -2, new Segment());
                }

                @Override
                public String expectedExceptionMessage() {
                    return "Length must be non-negative";
                }
            });
        } else {
            try {
                obj.getChars(0, -2, new Segment());
            } catch (BadLocationException e) {
            }
        }
    }

    @Override
    public void testGetCharsPartial() throws BadLocationException {
        obj.insertString(10, "big ");
        text.setPartialReturn(true);
        obj.getChars(8, 10, text);
        assertEquals("a big test", text.toString());
    }

    @Override
    public void testGetStringNegativeLength() {
        if (isHarmony()) {
            testExceptionalCase(new BadLocationCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    obj.getString(0, -2);
                }

                @Override
                public String expectedExceptionMessage() {
                    return "Length must be non-negative";
                }
            });
        } else {
            testExceptionalCase(new StringIndexOutOfBoundsCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    obj.getString(0, -2);
                }
            });
        }
    }

    public void testStringContent() throws BadLocationException {
        content = new StringContent();
        assertEquals(1, content.length());
        content.getChars(0, content.length(), text);
        assertEquals("\n", text.toString());
        assertEquals(10, text.array.length);
    }

    public void testStringContent_WithValidValues() throws BadLocationException {
        content = new StringContent(20);
        assertEquals(1, content.length());
        content.getChars(0, content.length(), text);
        assertEquals("\n", text.toString());
        assertEquals(20, text.array.length);
    }

    public void testStringContent_WithInvalidValues() throws BadLocationException {
        content = new StringContent(0);
        assertEquals(1, content.length());
        content.getChars(0, content.length(), text);
        assertEquals("\n", text.toString());
        assertEquals(1, text.array.length);
    }

    public void testCreatePositionBeforeUndo() throws BadLocationException {
        UndoableEdit undoable;
        content = new StringContent(10);
        content.insertString(0, "0123456789");
        undoable = content.remove(3, 5);
        Position pos1 = content.createPosition(3);
        assertEquals(3, pos1.getOffset());
        undoable.undo();
        content = new StringContent(10);
        content.insertString(0, "0123456789");
        undoable = content.remove(3, 5);
        Position pos = content.createPosition(3);
        assertEquals(3, pos.getOffset());
        undoable.undo();
        assertEquals(8, pos.getOffset());
        undoable.redo();
        assertEquals(3, pos.getOffset());
    }

    public void testCreatePositionAfterUndone() throws BadLocationException {
        UndoableEdit undoable;
        content = new StringContent(10);
        content.insertString(0, "0123456789");
        undoable = content.remove(3, 5);
        undoable.undo();
        Position pos = content.createPosition(5);
        assertEquals(5, pos.getOffset());
        undoable.redo();
        assertEquals(3, pos.getOffset());
        undoable.undo();
        assertEquals(5, pos.getOffset());
    }

    public void testCreatePositionAfterInsert() throws BadLocationException {
        UndoableEdit undoable;
        content = new StringContent(10);
        content.insertString(0, "0123456789");
        undoable = content.insertString(10, "big ");
        Position pos = content.createPosition(12);
        assertEquals(12, pos.getOffset());
        undoable.undo();
        assertEquals(10, pos.getOffset());
        undoable.redo();
        assertEquals(12, pos.getOffset());
    }

    public void testCreatePosition_WithInvalidValues() throws BadLocationException {
        content = new StringContent(10);
        content.insertString(0, "012345");
        if (isHarmony()) {
            testExceptionalCase(new BadLocationCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    content.createPosition(-1);
                }

                @Override
                public String expectedExceptionMessage() {
                    return "Invalid position offset";
                }
            });
        } else {
            content.createPosition(-1);
        }
        content.createPosition(12);
    }

    public void testGetChars_WithValidValues() throws BadLocationException {
        content = new StringContent();
        content.getChars(0, 1, text);
        content.getChars(0, 0, text);
        content.getChars(0, content.length(), text);
        assertEquals("\n", text.toString());
        assertEquals(10, text.array.length);
    }

    public void testGetChars_WithInvalidValues() throws BadLocationException {
        content = new StringContent();
        if (isHarmony()) {
            testExceptionalCase(new BadLocationCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    content.getChars(-5, 1, text);
                }

                @Override
                public String expectedExceptionMessage() {
                    return "Invalid start position";
                }
            });
        } else {
            content.getChars(-5, 1, text);
        }
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                content.getChars(0, 2, text);
            }
        });
        if (isHarmony()) {
            testExceptionalCase(new BadLocationCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    content.getChars(0, -2, text);
                }

                @Override
                public String expectedExceptionMessage() {
                    return "Length must be non-negative";
                }
            });
        } else {
            content.getChars(0, -2, text);
        }
    }

    public void testGetPositionsInRangeVector() throws BadLocationException {
        Vector<Object> v = new Vector<Object>();
        v.add(new Object());
        v.add(new Object());
        content.createPosition(0);
        content.createPosition(1);
        content.createPosition(2);
        content.getPositionsInRange(v, 0, 3);
        if (isHarmony()) {
            // Position at offset 0 WILL NOT be included
            assertEquals(4, v.size());
        } else {
            // Position at offset 0 WILL be included
            assertEquals(5, v.size());
        }
    }

    public void testGetPositionsInRange() throws BadLocationException {
        content.createPosition(10);
        Vector<?> v = content.getPositionsInRange(null, 0, 10);
        assertEquals(1, v.size());
    }

    public void testGetString_WithValidValues() throws BadLocationException {
        content.getString(0, 0);
        content.getString(0, content.length());
    }

    public void testGetString_WithInValidValues() throws BadLocationException {
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                content.getString(0, content.length() + 1);
            }
        });
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                content.getString(0, content.length() + 5);
            }
        });
        if (isHarmony()) {
            testExceptionalCase(new BadLocationCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    content.getString(-1, content.length() - 2);
                }
            });
        } else {
            testExceptionalCase(new StringIndexOutOfBoundsCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    content.getString(-1, content.length() - 2);
                }
            });
        }
        if (isHarmony()) {
            testExceptionalCase(new BadLocationCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    content.getString(1, -1);
                }
            });
        } else {
            testExceptionalCase(new StringIndexOutOfBoundsCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    content.getString(1, -1);
                }
            });
        }
    }

    public void testInsertString_WithValidValues() throws BadLocationException {
        UndoableEdit ue = content.insertString(2, "^^^");
        assertNotNull(ue);
        content.getChars(0, content.length(), text);
        assertEquals(20, text.array.length);
        assertEquals("01^^^2345\n", content.getString(0, content.length()));
        ue.undo();
        assertEquals("012345\n", content.getString(0, content.length()));
        ue.redo();
        assertEquals("01^^^2345\n", content.getString(0, content.length()));
    }

    public void testInsertString_WithInvalidValues() throws BadLocationException {
        content = new StringContent();
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                content.insertString(-1, "12345");
            }
        });
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                content.insertString(1, "12345");
            }
        });
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                content.insertString(0, null);
            }
        });
    }

    public void testInsertString_UndoableEdit() throws BadLocationException {
        UndoableEdit undoable;
        final String redoName = isHarmony() ? "Redo addition" : "Redo";
        final String undoName = isHarmony() ? "Undo addition" : "Undo";
        undoable = content.insertString(0, "012345");
        assertEquals(redoName, undoable.getRedoPresentationName());
        assertEquals(undoName, undoable.getUndoPresentationName());
        assertTrue(undoable.isSignificant());
        assertFalse(undoable.canRedo());
        assertTrue(undoable.canUndo());
        undoable.undo();
        assertTrue(undoable.canRedo());
        assertFalse(undoable.canUndo());
        undoable.redo();
        assertFalse(undoable.canRedo());
        assertTrue(undoable.canUndo());
        assertFalse(undoable.addEdit(null));
        assertFalse(undoable.addEdit(undoable));
    }

    public void testRemove_WithValidValues() throws BadLocationException {
        content = new StringContent();
        content.insertString(0, "012345^11111");
        content.getChars(0, content.length(), text);
        assertEquals(13, text.count);
        assertEquals(20, text.array.length);
        UndoableEdit undoable = content.remove(2, 0);
        content.getChars(0, content.length(), text);
        assertEquals(13, text.count);
        assertEquals(20, text.array.length);
        assertNotNull(undoable);
        assertEquals("012345^11111\n", content.getString(0, content.length()));
        undoable = content.remove(2, 5);
        content.getChars(0, content.length(), text);
        assertEquals(8, text.count);
        assertEquals(20, text.array.length);
        assertNotNull(undoable);
        assertEquals("0111111\n", content.getString(0, content.length()));
        undoable.undo();
        assertEquals("012345^11111\n", content.getString(0, content.length()));
        undoable.redo();
        assertEquals("0111111\n", content.getString(0, content.length()));
    }

    public void testRemove_UndoableEdit() throws BadLocationException {
        UndoableEdit undoable = content.remove(2, 3);
        final String redoName = isHarmony() ? "Redo deletion" : "Redo";
        final String undoName = isHarmony() ? "Undo deletion" : "Undo";
        assertEquals(redoName, undoable.getRedoPresentationName());
        assertEquals(undoName, undoable.getUndoPresentationName());
        assertTrue(undoable.isSignificant());
        assertFalse(undoable.canRedo());
        assertTrue(undoable.canUndo());
        assertEquals(redoName, undoable.getRedoPresentationName());
        assertEquals(undoName, undoable.getUndoPresentationName());
        assertTrue(undoable.isSignificant());
        assertFalse(undoable.canRedo());
        assertTrue(undoable.canUndo());
        undoable.undo();
        assertTrue(undoable.canRedo());
        assertFalse(undoable.canUndo());
        undoable.redo();
        assertFalse(undoable.canRedo());
        assertTrue(undoable.canUndo());
        assertFalse(undoable.addEdit(null));
        assertFalse(undoable.addEdit(undoable));
    }

    public void testRemove_WithInvalidValues() throws BadLocationException {
        content = new StringContent();
        content.insertString(0, "012345^11111");
        content.getChars(0, content.length(), text);
        assertEquals(13, text.count);
        assertEquals(20, text.array.length);
        UndoableEdit ue = content.remove(2, 5);
        content.getChars(0, content.length(), text);
        assertEquals(8, text.count);
        assertEquals(20, text.array.length);
        assertNotNull(ue);
        assertEquals("0111111\n", content.getString(0, content.length()));
        ue.undo();
        assertEquals("012345^11111\n", content.getString(0, content.length()));
        ue.redo();
        assertEquals("0111111\n", content.getString(0, content.length()));
    }

    public void testPositionGC() throws BadLocationException {
        Vector<WeakReference<Position>> pos = new Vector<WeakReference<Position>>(10);
        ReferenceQueue<Position> rq = new ReferenceQueue<Position>();
        for (int i = 0; i < content.length(); i += 2) {
            pos.add(new WeakReference<Position>(content.createPosition(i), rq));
        }
        int count = 0;
        int i;
        for (i = 0; i < 100; i++) {
            System.gc();
            Reference<?> r;
            if ((r = rq.poll()) != null) {
                pos.remove(r);
                count++;
                if (pos.size() == 0) {
                    return;
                }
            }
        }
        fail("Not all Position objects are removed (" + pos.size() + "/" + count + ").");
    }

    public void testLength_ForStringContent() throws Exception {
        content = new StringContent();
        assertEquals(1, content.length());
        content.insertString(0, " Hello word ");
        assertEquals(13, content.length());
        content.insertString(0, " Hello word ");
        assertEquals(25, content.length());
        content.remove(0, 0);
        assertEquals(25, content.length());
        content.remove(0, 2);
        assertEquals(23, content.length());
        content.remove(1, 3);
        assertEquals(20, content.length());
        content.remove(content.length() - 2, 1);
        assertEquals(19, content.length());
        content = new StringContent(20);
        assertEquals(1, content.length());
        content.insertString(0, " Hello word ");
        assertEquals(13, content.length());
        content.insertString(0, " Hello word ");
        assertEquals(25, content.length());
        content.remove(0, 0);
        assertEquals(25, content.length());
        content.remove(0, 2);
        assertEquals(23, content.length());
        content.remove(1, 3);
        assertEquals(20, content.length());
        content.remove(content.length() - 2, 1);
        assertEquals(19, content.length());
    }

    public void testInnerContentSize() throws BadLocationException {
        content = new StringContent(30);
        insertStringManyTimes("a", 160, content);
        content.getChars(0, content.length(), text);
        assertEquals(160, text.count);
        assertEquals(30 * 8, text.array.length);
        content = new StringContent();
        insertStringManyTimes("a", 159, content);
        content.getChars(0, content.length(), text);
        assertEquals(159, text.count);
        assertEquals(10 * 16, text.array.length);
        content.insertString(0, "i");
        content.getChars(0, content.length(), text);
        assertEquals(160, text.count);
        assertEquals(10 * 16 * 2, text.array.length);
        // If after insert operation new content length
        // exceeds current content size, content size
        // will increaze:
        content = new StringContent();
        content.insertString(0, createString(100));
        content.getChars(0, content.length(), text);
        assertEquals(100, text.count);
        if (isHarmony()) {
            assertEquals(200, text.array.length);
        } else {
            assertEquals(100, text.array.length);
        }
        insertStringManyTimes("a", 11, content);
        content.getChars(0, content.length(), text);
        assertEquals(110, text.count);
        assertEquals(200, text.array.length);
        // Rule1
        //
        // If after insert operation new content length
        // doesn't exceed content size, then content size
        // wont be enlarged.
        content = new StringContent(101);
        content.insertString(0, createString(99));
        content.getChars(0, content.length(), text);
        assertEquals(99, text.count);
        assertEquals(101, text.array.length);
        content.insertString(0, "i");
        content.getChars(0, content.length(), text);
        assertEquals(100, text.count);
        assertEquals(101, text.array.length);
        // This test shows that Rule1 is "initial length free"
        content = new StringContent(101);
        content.insertString(0, createString(100));
        content.getChars(0, content.length(), text);
        assertEquals(100, text.count);
        assertEquals(101, text.array.length);
        content.insertString(0, "i");
        content.getChars(0, content.length(), text);
        assertEquals(101, text.count);
        assertEquals(202, text.array.length);
        content.insertString(0, createString(103));
        content.getChars(0, content.length(), text);
        assertEquals(203, text.count);
        assertEquals(404, text.array.length);
        content.insertString(0, createString(301));
        content.getChars(0, content.length(), text);
        assertEquals(503, text.count);
        assertEquals(808, text.array.length);
        // Rule2
        //
        // If after insert operation new content length
        // exceeds content size more than twice, new
        content = new StringContent(251);
        content.insertString(0, createString(503));
        content.getChars(0, content.length(), text);
        assertEquals(503, text.count);
        if (isHarmony()) {
            // newSize = (length()+insertedStringLength) * 2
            assertEquals(1006, text.array.length);
        } else {
            // content size will be changed to new content length
            assertEquals(503, text.array.length);
        }
        //Rule3
        //
        //If after insert operation new content length
        //exceeds content size less than twice, new
        //content size will be changed to (content size) * 2
        content = new StringContent(252);
        content.insertString(0, createString(503));
        content.getChars(0, content.length(), text);
        assertEquals(503, text.count);
        assertEquals(504, text.array.length);
    }

    private void insertStringManyTimes(String str, int ntimes, StringContent content)
            throws BadLocationException {
        for (int i = 1; i < ntimes; i++) {
            content.insertString(0, str);
        }
    }

    private String createString(final int size) {
        StringBuffer result = new StringBuffer();
        for (int i = 1; i < size; i++) {
            result.append('a');
        }
        return result.toString();
    }
}
