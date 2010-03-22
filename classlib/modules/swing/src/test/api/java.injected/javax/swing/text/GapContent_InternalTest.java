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

import java.util.List;
import javax.swing.BasicSwingTestCase;

/**
 * Tests some internal functionality of GapContent but not its methods.
 *
 */
public class GapContent_InternalTest extends BasicSwingTestCase {
    protected GapContent content;

    @Override
    protected void setUp() throws Exception {
        content = new GapContent(30);
        content.insertString(0, "This is a test string.");
    }

    public void testBufferExpansion() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        content = new GapContent();
        int size = content.getArrayLength();
        char c = '0';
        while (size == content.getArrayLength()) {
            content.insertString(content.getGapStart(), new String(new char[] { c }));
            c++;
        }
        assertEquals("012345678\n", content.getString(0, content.length()));
    }

    public void testInsertPosition() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        content.shiftGap(5);
        content.createPosition(10);
        content.createPosition(4);
        content.createPosition(5);
        content.createPosition(15);
        content.createPosition(2);
        content.createPosition(23);
        int[] positions = { 2, 4, 12, 17, 22, 30 };
        int[] offsets = { 2, 4, 5, 10, 15, 23 };
        List<?> posList = getPositionList(content);
        for (int i = 0; i < posList.size(); i++) {
            Position p = (Position) posList.get(i);
            assertEquals("Indexes are different @ " + i, positions[i], getIndex(p));
            assertEquals("Offsets are different @ " + i, offsets[i], p.getOffset());
        }
    }

    /**
     * Test that positionList is sorted.
     * @throws BadLocationException
     */
    public void testPositionSort() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        GapContent g = new GapContent();
        g.insertString(0, "123\n");
        int[] pos = { 0, 0, 0, 5, 5, 4 };
        for (int i = 0; i < pos.length; i++) {
            g.createPosition(pos[i]);
        }
        List<?> posList = getPositionList(g);
        // Check all the positions are sorted
        Position dmL;
        Position dmR;
        for (int i = 0; i < posList.size() - 1; i++) {
            dmL = (Position) posList.get(i);
            dmR = (Position) posList.get(i + 1);
            assertFalse(getIndex(dmL) > getIndex(dmR));
        }
    }

    public void testPositionGC() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        final Position[] pos = new Position[5];
        for (int i = 0; i < 10; i++) {
            Position p = content.createPosition(i * 2);
            if (i < 5) {
                pos[i] = p;
            }
        }
        assertEquals(10, getPositionList(content).size());
        for (int i = 0; i < 3; i++) {
            System.gc();
        }
        // if this code is omitted the saved objects are
        // considered to be unreachable, so the garbage
        // collector deletes refences to them
        for (int i = 0; i < pos.length; i++) {
            pos[i].getOffset();
        }
        // Create another position object. This will implicitly
        // remove all unreachable positions
        content.createPosition(20);
        assertEquals(6, getPositionList(content).size());
    }

    private static List<?> getPositionList(final GapContent content) {
        return GapContentTest.getPositionList(content);
    }

    private static int getIndex(final Position pos) {
        return GapContentTest.getIndex(pos);
    }
}
