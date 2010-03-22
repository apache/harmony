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
import java.util.Vector;
import javax.swing.undo.UndoableEdit;
import junit.framework.TestCase;

public class GapContent_ComplexUndoTest extends TestCase {
    protected AbstractDocument.Content content;

    @Override
    protected void setUp() throws Exception {
        content = new GapContent();
    }

    /**
     * Insert some characters, undo all insertions and then redo them.
     * We should get the same string.
     *
     * @throws BadLocationException
     */
    public void testUndoRedo1() throws BadLocationException {
        char[] chars = { '0', '1', '2', '3', '4' };
        UndoableEdit[] undo = new UndoableEdit[chars.length];
        int i;
        for (i = 0; i < 5; i++) {
            undo[i] = content.insertString(i, new String(chars, i, 1));
        }
        // Now undo till the end
        for (i = undo.length - 1; i >= 0; i--) {
            undo[i].undo();
        }
        // Then redo
        for (i = 0; i < undo.length; i++) {
            undo[i].redo();
        }
        // This should give us the same result
        assertEquals("01234", content.getString(0, content.length() - 1));
    }

    /**
     * Add some characters, insert several in the middle, undo all actions,
     * redo all actions. The content should be the same.
     *
     * @throws BadLocationException
     */
    public void testUndoRedo2() throws BadLocationException {
        char[] chars = { '0', '1', '2', '3', '4', 'a', 'b', 'c' };
        UndoableEdit[] undo = new UndoableEdit[chars.length];
        int i;
        for (i = 0; i < 5; i++) {
            undo[i] = content.insertString(i, new String(chars, i, 1));
        }
        for (; i < chars.length; i++) {
            undo[i] = content.insertString(i - 2, new String(chars, i, 1));
        }
        // Now undo till the end
        for (i = undo.length - 1; i >= 3; i--) {
            undo[i].undo();
        }
        // Then redo
        for (++i; i < undo.length; i++) {
            undo[i].redo();
        }
        // This should give us the same result
        assertEquals("012abc34", content.getString(0, content.length() - 1));
    }

    /**
     * Insert character into the content causing the buffer to grow.
     *
     * @throws BadLocationException
     */
    public void testUndoRedo3() throws BadLocationException {
        content.insertString(0, "012345678");
        char[] chars = { '9', '0', '1', 'a', 'b', 'c' };
        UndoableEdit[] undo = new UndoableEdit[chars.length];
        int i;
        for (i = 0; i < 3; i++) {
            undo[i] = content.insertString(content.length() - 1, new String(chars, i, 1));
        }
        for (; i < chars.length; i++) {
            undo[i] = content.insertString(i, new String(chars, i, 1));
        }
        // Now undo till the end
        for (i = undo.length - 1; i >= 3; i--) {
            undo[i].undo();
        }
        // Then redo
        for (++i; i < undo.length; i++) {
            undo[i].redo();
        }
        // This should give us the same result
        assertEquals("012abc345678901", content.getString(0, content.length() - 1));
    }

    /**
     * Creates positions at specified offsets and stores them in array.
     *
     * @param offsets array of offsets where positions are to be created
     * @param positions array of positions to store to
     * @throws BadLocationException
     */
    private void addPositions(final int[] offsets, final Position[] positions)
            throws BadLocationException {
        for (int i = 0; i < offsets.length; i++) {
            positions[i] = content.createPosition(offsets[i]);
        }
    }

    /**
     * Checks that positions have the expected offsets.
     *
     * @param offsets expected offsets
     * @param positions position array to be checked
     */
    private void checkPositions(final int[] offsets, final Position[] positions) {
        for (int i = 0; i < offsets.length; i++) {
            assertEquals(offsets[i], positions[i].getOffset());
        }
    }

    /**
     * Make sure the positions get updated correctly while using
     * undo and redo.
     *
     * @throws BadLocationException
     */
    public void testUndoRedo4() throws BadLocationException {
        content.insertString(0, "yuiop\n123456\n789987\nabcdef");
        //                       012345 6789012 3456789 012345
        int[] off1 = { 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24 };
        Position[] pos1 = new Position[off1.length];
        addPositions(off1, pos1);
        UndoableEdit[] undo = new UndoableEdit[3];
        // Remove the second line from the text
        undo[0] = content.remove(6, 7);
        assertEquals("yuiop\n789987\nabcdef\n", content.getString(0, content.length()));
        assertEquals("p\n", content.getString(pos1[2].getOffset(), pos1[6].getOffset()
                - pos1[2].getOffset()));
        // Remove the second line from the text (it was the third one)
        undo[1] = content.remove(6, 7);
        assertEquals("yuiop\nabcdef\n", content.getString(0, content.length()));
        assertEquals("p\n", content.getString(pos1[2].getOffset(), pos1[6].getOffset()
                - pos1[2].getOffset()));
        // Insert a string at the same position (in the beginning
        // of the second line)
        undo[2] = content.insertString(6, "qwerty");
        assertEquals("yuiop\nqwertyabcdef\n", content.getString(0, content.length()));
        // Add more positions
        int[] off2 = { 6, 7, 8, 9, 10 };
        Position[] pos2 = new Position[off2.length];
        int[] off2Del = { 6, 6, 6, 6, 6 };
        addPositions(off2, pos2);
        assertEquals("p\nqwer", content.getString(pos1[2].getOffset(), pos2[4].getOffset()
                - pos1[2].getOffset()));
        // Now undo twice
        // Undo the first time
        undo[2].undo();
        // Check the positions we added the second time, and
        // the text between two positions
        checkPositions(off2Del, pos2);
        assertEquals("", content.getString(pos1[4].getOffset(), pos2[1].getOffset()
                - pos1[4].getOffset()));
        // Undo the second time
        undo[1].undo();
        assertEquals("789987\n", content.getString(pos1[4].getOffset(), pos2[1].getOffset()
                - pos1[4].getOffset()));
        // And redo once
        undo[1].redo();
        // Check positions and text in the buffer
        checkPositions(off2Del, pos2);
        assertEquals("yuiop\nabcdef\n", content.getString(0, content.length()));
        // This assertion was failing, throwing the exception as
        // pos1[4] was negative :-), but of course shouldn't
        assertEquals("", content.getString(pos1[4].getOffset(), pos2[1].getOffset()
                - pos1[4].getOffset()));
    }

    /**
     * Undo after some positions have been garbage collected.
     *
     * @throws BadLocationException
     */
    public void testUndoGC() throws BadLocationException {
        content.insertString(0, "012345678");
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
                    break;
                }
            }
        }
        // This call causes all the garbage collected positions to be
        // removed from the internal list
        UndoableEdit ue = content.remove(0, 5);
        assertEquals("5678", content.getString(0, content.length() - 1));
        // Test (it shouldn't fail with any NullPointerException)
        ue.undo();
        assertEquals("012345678", content.getString(0, content.length() - 1));
    }
}
