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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Vector;
import javax.swing.BasicSwingTestCase;
import javax.swing.undo.UndoableEdit;

public class GapContentTest extends AbstractDocument_ContentTest {
    protected AbstractDocument.Content content;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        obj = content = new GapContent(30);
        obj.insertString(0, "This is a test string.");
    }

    public void testGetPositionsInRangeVector() throws BadLocationException {
        Vector<Object> v = new Vector<Object>();
        v.add(new Object());
        v.add(new Object());
        content.createPosition(0);
        content.createPosition(1);
        content.createPosition(2);
        ((GapContent) content).getPositionsInRange(v, 0, 3);
        if (BasicSwingTestCase.isHarmony()) {
            // Position at offset 0 WILL NOT be included
            assertEquals(4, v.size());
        } else {
            // Position at offset 0 WILL be included
            assertEquals(5, v.size());
        }
    }

    public void testGetPositionsInRange() throws BadLocationException {
        Vector<Position> pos = new Vector<Position>();
        for (int i = 0; i < content.length(); i += 2) {
            Position p = content.createPosition(i);
            if (i >= 3 && i <= 3 + 9) {
                pos.add(p);
            }
        }
        Vector<?> v = ((GapContent) content).getPositionsInRange(null, 3, 9);
        assertEquals(pos.size(), v.size());
        int[] offsets = new int[v.size()];
        for (int i = 0; i < pos.size(); i++) {
            offsets[i] = pos.get(i).getOffset();
        }
        UndoableEdit ue = content.remove(0, 9);
        ue.undo();
        for (int i = 0; i < pos.size(); i++) {
            assertEquals(offsets[i], pos.get(i).getOffset());
        }
    }

    public void testUpdatePositions() throws BadLocationException {
        GapContent cont1 = new GapContent();
        final Vector<Position> pos = new Vector<Position>();
        final int posSize = 5;
        final int buffsize = 10;
        cont1 = new GapContent() {
            private static final long serialVersionUID = 1L;

            @Override
            public UndoableEdit remove(int where, int len) throws BadLocationException {
                UndoableEdit u = super.remove(where, len);
                return u;
            }

            @Override
            public UndoableEdit insertString(int offset, String str)
                    throws BadLocationException {
                UndoableEdit u = super.insertString(offset, str);
                return u;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void updateUndoPositions(Vector vector, int offset, int len) {
                super.updateUndoPositions(vector, 1, 2);
            }
        };
        for (int i = 0; i < posSize; i++) {
            pos.add(cont1.createPosition(i));
        }
        StringBuffer f = new StringBuffer();
        for (int i = 0; i < buffsize; i++) {
            f.append("a");
        }
        cont1.insertString(0, f.toString());
        cont1.remove(0, 10).undo();
    }

    /**
     * Tests that the position at offset of offset + len is included in
     * the returned vector.
     */
    public void testGetPositionsInRangeEnd() throws BadLocationException {
        content.createPosition(10);
        Vector<?> v = ((GapContent) content).getPositionsInRange(null, 0, 10);
        assertEquals(1, v.size());
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

    private void isContentArraySame(final boolean expected) {
        if (BasicSwingTestCase.isHarmony()) {
            if (expected) {
                assertSame(((GapContent) obj).getArray(), text.array);
            } else {
                assertNotSame(((GapContent) obj).getArray(), text.array);
            }
        }
    }

    @Override
    public void testGetCharsAfterGap() throws BadLocationException {
        super.testGetCharsAfterGap();
        isContentArraySame(true);
    }

    public void testGetCharsAfterGapNoImplied() throws BadLocationException {
        // Move the gap
        obj.insertString(10, "big ");
        // Don't include the implied char
        obj.getChars(19, 7, text);
        assertEquals("string.", text.toString());
        isContentArraySame(true);
    }

    @Override
    public void testGetCharsBeforeGap() throws BadLocationException {
        super.testGetCharsBeforeGap();
        isContentArraySame(true);
    }

    @Override
    public void testGetCharsFullLength() throws BadLocationException {
        super.testGetCharsFullLength();
        isContentArraySame(false);
    }

    public void testGetCharsFullActualLength() throws BadLocationException {
        obj.getChars(0, obj.length() - 1, text);
        assertEquals("This is a test string.", text.toString());
        isContentArraySame(true);
    }

    @Override
    public void testGetCharsImpliedChar() throws BadLocationException {
        super.testGetCharsImpliedChar();
        isContentArraySame(false);
    }

    public void testGetCharsImpliedCharPartial() throws BadLocationException {
        obj = content = new GapContent();
        assertEquals(1, content.length());
        text.setPartialReturn(false);
        content.getChars(0, 1, text);
        assertEquals("\n", text.toString());
        assertEquals(((GapContent) content).getArrayLength(), text.array.length);
        text.setPartialReturn(true);
        content.getChars(0, 1, text);
        assertEquals("\n", text.toString());
        assertEquals(((GapContent) content).getArrayLength(), text.array.length);
    }

    @Override
    public void testGetCharsPartial() throws BadLocationException {
        super.testGetCharsPartial();
        isContentArraySame(true);
    }

    @Override
    public void testGetCharsWithGap() throws BadLocationException {
        super.testGetCharsWithGap();
        isContentArraySame(false);
    }

    // Regression for HARMONY-2566
    public void testGetCharsMaxInteger() {
        try {
            content.getChars(1, Integer.MAX_VALUE, null);
            fail("BadLocationException is expected");
        } catch (BadLocationException e) {
        }
    }

    public void testGetCharsNullSegment() throws BadLocationException {
        try {
            content.getChars(1, 1, null);
            fail("NullPointerException is expected");
        } catch (NullPointerException e) {
        }
    }

    public void testCreatePositionBeforeUndo() throws BadLocationException {
        UndoableEdit ue = content.remove(3, 8);
        Position pos = content.createPosition(3);
        assertEquals(3, pos.getOffset());
        ue.undo();
        assertEquals(11, pos.getOffset());
        ue.redo();
        assertEquals(3, pos.getOffset());
    }

    public void testCreatePositionAfterUndone() throws BadLocationException {
        UndoableEdit ue = content.remove(3, 8);
        ue.undo();
        Position pos = content.createPosition(5);
        assertEquals(5, pos.getOffset());
        ue.redo();
        assertEquals(3, pos.getOffset());
        ue.undo();
        assertEquals(5, pos.getOffset());
    }

    public void testCreatePositionAfterInsert() throws BadLocationException {
        UndoableEdit ue = content.insertString(10, "big ");
        Position pos = content.createPosition(12);
        assertEquals(12, pos.getOffset());
        ue.undo();
        assertEquals(10, pos.getOffset());
        ue.redo();
        assertEquals(12, pos.getOffset());
    }

    public static int getIndex(final Position pos) {
        try {
            Field f = pos.getClass().getDeclaredField("index");
            f.setAccessible(true);
            return f.getInt(pos);
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        }
        return -1000;
    }

    public static List<?> getPositionList(final GapContent content) {
        try {
            Field f = content.getClass().getDeclaredField("gapContentPositions");
            f.setAccessible(true);
            ContentPositions gapContentPositions = (ContentPositions) f.get(content);
            f = gapContentPositions.getClass().getSuperclass().getDeclaredField("positionList");
            f.setAccessible(true);
            return (List<?>) (f.get(gapContentPositions));
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        }
        return null;
    }
}
