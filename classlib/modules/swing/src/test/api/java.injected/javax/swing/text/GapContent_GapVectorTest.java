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
package javax.swing.text;

import javax.swing.BasicSwingTestCase;

import junit.framework.TestCase;

public class GapContent_GapVectorTest extends TestCase {
    private GapContent gv;

    @Override
    protected void setUp() throws BadLocationException {
        gv = new GapContent(20);
        gv.insertString(0, "abcdefghij");
        //                  0123456789
    }

    /**
     * Tests constructor GapContent()
     */
    public void testGapContent() {
        GapContent gv = new GapContent();
        assertEquals(10, gv.getArrayLength());
        assertNotNull(gv.getArray());
    }

    /**
     * Tests constructor GapContent(int)
     */
    public void testGapContentint() {
        assertEquals(20, gv.getArrayLength());
        assertNotNull(gv.getArray());
    }

    public void testAllocateArray() {
        final int length = gv.getArrayLength();
        char[] array = (char[]) gv.allocateArray(150);
        assertEquals(150, array.length);
        assertEquals(length, gv.getArrayLength());
    }

    public void testGetArray() {
        assertTrue(gv.getArray() instanceof char[]);
    }

    public void testGetArrayLength() {
        assertEquals(((char[]) gv.getArray()).length, gv.getArrayLength());
    }

    public void testGetGapEnd() {
        assertEquals(19, gv.getGapEnd());
    }

    public void testGetGapStart() {
        assertEquals(10, gv.getGapStart());
    }

    public void testInsertStringValid() throws BadLocationException {
        gv.insertString(0, "z");
        assertEquals(1, gv.getGapStart());
    }

    public void testInsertStringEnd() throws BadLocationException {
        gv.insertString(11, "z");
        assertEquals(12, gv.getGapStart());
        assertEquals("abcdefghij\nz", gv.getString(0, gv.length()));
    }

    public void testInsertStringInvalid() {
        try {
            gv.insertString(12, "z");
            fail("BadLocationException must be thrown");
        } catch (BadLocationException e) {
        }
    }

    public void testInsertStringNull() throws BadLocationException {
        try {
            gv.insertString(0, null);
            fail("NullPointerException is expected");
        } catch (NullPointerException e) { }
        assertEquals(10, gv.getGapStart());
        assertEquals(19, gv.getGapEnd());
    }

    public void testInsertStringEmpty() throws BadLocationException {
        gv.insertString(0, "");
        assertEquals(10, gv.getGapStart());
        assertEquals(19, gv.getGapEnd());
    }

    public void testInsertStringInvalidNull() {
        try {
            gv.insertString(-1, null);

            fail("NullPointerException or BadLocationException must be thrown");
        } catch (NullPointerException e) {
            if (!BasicSwingTestCase.isHarmony()) {
                fail("Unexpected NullPointerException");
            }
        } catch (BadLocationException e) {
            if (BasicSwingTestCase.isHarmony()) {
                fail("Unexpected BadLocationException");
            }
        }
    }

    public void testRemoveValid() throws BadLocationException {
        gv.remove(1, 7);
        assertEquals(1, gv.getGapStart());
        assertEquals(17, gv.getGapEnd());
    }

    public void testRemoveInvalidPosition() {
        try {
            gv.remove(10, 1);
            fail("BadLocationException should be thrown");
        } catch (BadLocationException e) {
        }
    }

    public void testRemoveInvalidSize() {
        // Try to remove more items than the array actually holds
        try {
            gv.remove(7, 10);
            fail("BadLocationException should be thrown");
        } catch (BadLocationException e) {
        }
    }

    public void testRemoveEmpty() throws BadLocationException {
        gv.remove(1, 0);

        assertEquals(10, gv.getGapStart());
        assertEquals(19, gv.getGapEnd());
    }

    public void testRemoveEmptyInvalid() {
        try {
            gv.remove(-1, 0);

            fail("BadLocationException should be thrown");
        } catch (BadLocationException e) { }

        assertEquals(10, gv.getGapStart());
        assertEquals(19, gv.getGapEnd());
    }

    public void testReplace() throws BadLocationException {
        char[] charArray = { 'z', 'y', 'x', 'w', 'v' };
        gv.replace(3, 5, charArray, 3);
        char[] chars1 = { 'a', 'b', 'c', 'z', 'y', 'x' };
        char[] chars2 = { 'i', 'j' };
        char[] array = (char[]) gv.getArray();
        for (int i = 0; i < gv.getGapStart(); i++) {
            assertEquals("1 @ " + i, chars1[i], array[i]);
        }
        for (int i = gv.getGapEnd(), j = 0; j < chars2.length; i++, j++) {
            assertEquals("2 @ " + i, chars2[j], array[i]);
        }
    }

    // HARMONY-1809
    public void testReplaceInvalidRemovePosition() throws Exception {
        gv.replace(-2, 2, null, 0);
        if (BasicSwingTestCase.isHarmony()) {
            // Harmony just ignores the operation
            // because GapContent.removeItems throws BadLocationException;
            assertEquals("abcdefghij\n", gv.getString(0, gv.length()));
        } else {
            assertEquals("cdefghij\n", gv.getString(0, gv.length()));
        }
    }

    // HARMONY-1809
    public void testReplaceInvalidRemoveLength() throws Exception {
        gv.replace(5, 6, null, 0);
        if (BasicSwingTestCase.isHarmony()) {
            // Harmony just ignores the operation
            // because GapContent.removeItems throws BadLocationException;
            assertEquals("abcdefghij\n", gv.getString(0, gv.length()));
        } else {
            assertEquals("abcde", gv.getString(0, gv.length()));
        }
    }

    // HARMONY-1809
    public void testReplaceNullInsert() throws Exception {
        gv.replace(0, 0, null, 0);
        assertEquals(10, gv.getGapStart());
        assertEquals(19, gv.getGapEnd());
        assertEquals("abcdefghij\n", gv.getString(0, gv.length()));
    }

    // HARMONY-1809
    public void testReplaceInvalidInsertLength() throws Exception {
        try {
            gv.replace(0, 0, new char[] {'1'}, 2);
            fail("ArrayIndexOutOfBounds is expected");
        } catch (ArrayIndexOutOfBoundsException e) { }

        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(0, gv.getGapStart());
            assertEquals(9, gv.getGapEnd());
            assertEquals("abcdefghij\n", gv.getString(0, gv.length()));
        } else {
            assertEquals(2, gv.getGapStart());
            assertEquals(9, gv.getGapEnd());
            assertEquals("ababcdefghij\n", gv.getString(0, gv.length()));
        }
    }

    // HARMONY-1809
    public void testReplaceInvalidInsertLengthNegative() throws Exception {
        try {
            gv.replace(0, 0, new char[] {'1'}, -1);
            fail("ArrayIndexOutOfBounds is expected");
        } catch (ArrayIndexOutOfBoundsException e) { }

        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(0, gv.getGapStart());
            assertEquals(9, gv.getGapEnd());
            assertEquals("abcdefghij\n", gv.getString(0, gv.length()));
        } else {
            assertEquals(-1, gv.getGapStart());
            assertEquals(9, gv.getGapEnd());
            assertEquals("bcdefghij\n", gv.getString(0, gv.length()));
        }
    }

    public void testShiftEnd() {
        gv.shiftGap(5);
        gv.shiftEnd(20);
        // Check the gap boundaries and its size
        assertEquals(5, gv.getGapStart());
        assertEquals(36, gv.getGapEnd());
        assertEquals(31, gv.getGapEnd() - gv.getGapStart());
        // Check that values are copied
        assertEquals('e', ((char[]) gv.getArray())[gv.getGapStart() - 1]);
        assertEquals('f', ((char[]) gv.getArray())[gv.getGapEnd()]);
        try {
            gv.shiftEnd(1);
            fail("ArrayIndexOutOfBoundsException is expected as the size of "
                    + "the array can't be decreased.");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    public void testShiftEndOverfull() throws BadLocationException {
        gv.insertString(gv.getGapStart(), "0123456");
        int size = gv.getArrayLength();
        assertEquals(20, size);
        char c = '7';
        while (size == gv.getArrayLength()) {
            gv.insertString(gv.getGapStart(), String.valueOf(c));
            c++;
        }
        assertEquals(19, gv.getGapStart());
        assertEquals(41, gv.getGapEnd());
        assertEquals(42, gv.getArrayLength());
    }

    public void testShiftGapToLeft() {
        gv.shiftGap(5);
        assertEquals(5, gv.getGapStart());
        assertEquals(14, gv.getGapEnd());
    }

    public void testShiftGapToRight() {
        gv.shiftGap(2);
        gv.shiftGap(7);
        assertEquals(7, gv.getGapStart());
        assertEquals(16, gv.getGapEnd());
        char[] array = (char[]) gv.getArray();
        assertEquals('g', array[gv.getGapStart() - 1]);
        assertEquals('h', array[gv.getGapEnd()]);
    }

    public void testShiftGapEndUp() {
        gv.shiftGap(5);
        gv.shiftGapEndUp(17);
        assertEquals(17, gv.getGapEnd());
    }

    public void testShiftGapStartDown() {
        gv.shiftGap(5);
        gv.shiftGapStartDown(3);
        assertEquals(3, gv.getGapStart());
    }
}
