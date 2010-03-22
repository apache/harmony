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

import java.text.CharacterIterator;
import junit.framework.TestCase;

public class SegmentTest extends TestCase {
    private Segment s;

    private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k' };

    @Override
    protected void setUp() {
        s = new Segment(arr, 2, 6);
    }

    public void testClone() {
        Segment clone = (Segment) s.clone();
        assertEquals(s.array, clone.array);
        assertEquals(s.count, clone.count);
        assertEquals(s.offset, clone.offset);
        assertEquals(s.getIndex(), clone.getIndex());
        assertEquals(s.getClass(), clone.getClass());
        assertEquals(s.isPartialReturn(), clone.isPartialReturn());
        assertNotSame(s, clone);
    }

    public void testToString() {
        assertEquals("cdefgh", s.toString());
    }

    public void testToStringEmpty() {
        s = new Segment();
        assertNull(s.array);
        assertEquals("", s.toString());
    }

    public void testCurrent() {
        assertEquals(arr[0], s.current());
        assertEquals(s.array[s.getIndex()], s.current());
        s.setIndex(2);
        assertEquals(s.array[2], s.current());
        s.setIndex(s.getEndIndex());
        assertEquals(CharacterIterator.DONE, s.current());
        s = new Segment();
        assertEquals(CharacterIterator.DONE, s.current());
    }

    public void testFirst() {
        assertEquals(arr[2], s.first());
        assertEquals(s.array[s.getBeginIndex()], s.first());
        assertEquals(s.getBeginIndex(), s.getIndex());
        s = new Segment();
        assertEquals(CharacterIterator.DONE, s.first());
        assertEquals(s.getBeginIndex(), s.getIndex());
    }

    public void testLast() {
        assertEquals(arr[7], s.last());
        assertEquals(s.array[s.getEndIndex() - 1], s.last());
        assertEquals(s.getEndIndex() - 1, s.getIndex());
        s = new Segment();
        assertEquals(CharacterIterator.DONE, s.last());
        assertEquals(s.getEndIndex(), s.getIndex());
        s = new Segment(arr, 2, 0);
        assertEquals(CharacterIterator.DONE, s.last());
        assertEquals(s.getEndIndex(), s.getIndex());
    }

    public void testNext() {
        assertEquals(arr[1], s.next());
        assertEquals(1, s.getIndex());
        s.setIndex(4);
        assertEquals(arr[5], s.next());
        s.setIndex(s.getEndIndex());
        assertEquals(CharacterIterator.DONE, s.next());
        s.setIndex(s.getEndIndex() - 1);
        assertEquals(CharacterIterator.DONE, s.next());
        s = new Segment();
        assertEquals(CharacterIterator.DONE, s.next());
    }

    public void testPrevious() {
        s.setIndex(2);
        assertEquals(CharacterIterator.DONE, s.previous());
        s.setIndex(5);
        assertEquals(arr[4], s.previous());
        s = new Segment();
        assertEquals(CharacterIterator.DONE, s.previous());
    }

    public void testGetBeginIndex() {
        assertEquals(2, s.getBeginIndex());
        s = new Segment();
        assertEquals(0, s.getBeginIndex());
    }

    public void testGetEndIndex() {
        assertEquals(8, s.getEndIndex());
        s = new Segment();
        assertEquals(0, s.getEndIndex());
    }

    public void testGetIndex() {
        assertEquals(0, s.getIndex());
        s.setIndex(5);
        assertEquals(5, s.getIndex());
        s = new Segment();
        assertEquals(0, s.getIndex());
    }

    /*
     * void Segment()
     */
    public void testSegment() {
        s = new Segment();
        assertNull(s.array);
        assertEquals(0, s.count);
        assertEquals(0, s.offset);
    }

    public void testIsPartialReturn() {
        // Default state must be FALSE
        assertFalse(s.isPartialReturn());
    }

    public void testSetIndex() {
        try {
            s.setIndex(-1);
            fail("IllegalArgumentException was expected");
        } catch (IllegalArgumentException e) {
        }
        assertEquals(0, s.getIndex());
        try {
            s.setIndex(s.getEndIndex() + 1);
            fail("IllegalArgumentException was expected");
        } catch (IllegalArgumentException e) {
        }
        assertEquals(0, s.getIndex());
        assertEquals(CharacterIterator.DONE, s.setIndex(s.getEndIndex()));
        assertEquals(s.getEndIndex(), s.getIndex());
        assertEquals(arr[4], s.setIndex(4));
        assertEquals(4, s.getIndex());
    }

    public void testSetPartialReturn() {
        s.setPartialReturn(true);
        assertTrue(s.isPartialReturn());
    }

    /*
     * void Segment(char[], int, int)
     */
    public void testSegmentcharArrayintint() {
        assertEquals(arr, s.array);
        assertEquals(2, s.offset);
        assertEquals(6, s.count);
    }
}
