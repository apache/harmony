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
 * Tests GapContent methods which were in GapVector in respect to updating
 * positions.
 *
 */
public class GapContent_PositionTest extends BasicSwingTestCase {
    private GapContent content;

    /**
     * Offsets in the document when the document is not changed.
     */
    private static final int[] offsets = { 0, 5, 10, 15, 20 };

    private Position[] positions;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        content = new GapContent(30);
        content.insertString(0, "This is a test string.");
        content.shiftGap(7);
        // Add some positions to the content
        int[] offsets = { 0, 10, 5, 20, 15 };
        positions = new Position[offsets.length];
        for (int i = 0; i < offsets.length; i++) {
            positions[i] = content.createPosition(offsets[i]);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        for (int i = 0; i < offsets.length; i++) {
            positions[i] = null;
        }
        super.tearDown();
    }

    public void testShiftGapLeft() {
        if (!isHarmony()) {
            return;
        }
        content.shiftGap(2);
        checkPositions(new int[] { 0, 12, 17, 22, 27 });
        checkPositionOffsets(offsets);
    }

    public void testShiftGapRight() {
        if (!isHarmony()) {
            return;
        }
        content.shiftGap(15);
        checkPositions(new int[] { 0, 5, 10, 22, 27 });
        checkPositionOffsets(offsets);
    }

    /**
     * Checks that position indexes are the same as expected.
     *
     * @param pos an array of expected position indexes
     */
    private void checkPositions(final int[] pos) {
        Position p;
        final List<?> list = GapContentTest.getPositionList(content);
        for (int i = 0; i < pos.length; i++) {
            p = (Position) list.get(i);
            assertEquals(pos[i], GapContentTest.getIndex(p));
        }
    }

    /**
     * Checks that position offsets are the same as expected.
     *
     * @param pos an array of expected position indexes
     */
    private void checkPositionOffsets(final int[] offsets) {
        Position p;
        List<?> list = GapContentTest.getPositionList(content);
        for (int i = 0; i < offsets.length; i++) {
            p = (Position) list.get(i);
            assertEquals(offsets[i], p.getOffset());
        }
    }

    public void testShiftEnd() {
        if (!isHarmony()) {
            return;
        }
        content.shiftEnd(20);
        checkPositionOffsets(offsets);
        checkPositions(new int[] { 0, 5, 29, 34, 39 });
    }

    public void testShiftGapStartDown() {
        if (!isHarmony()) {
            return;
        }
        content.shiftGapStartDown(3);
        checkPositionOffsets(new int[] { 0, 3, 6, 11, 16 });
        checkPositions(new int[] { 0, 14, 17, 22, 27 });
    }

    public void testShiftGapEndUp() {
        if (!isHarmony()) {
            return;
        }
        content.shiftGapEndUp(22);
        checkPositionOffsets(new int[] { 0, 5, 7, 7, 12 });
        checkPositions(new int[] { 0, 5, 22, 22, 27 });
    }

    public void testResetMarksAtZero() {
        if (!isHarmony()) {
            return;
        }
        content.shiftGapStartDown(0);
        // No explicit call to content.resetMarksAtZero() is made,
        // as it is called by content.shiftGapStartDown method
        checkPositions(new int[] { 0, 0, 17, 22, 27 });
        checkPositionOffsets(new int[] { 0, 0, 3, 8, 13 });
    }
}
