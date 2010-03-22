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
 * @author Roman I. Chernyatchik
 */
package javax.swing.text;

import java.util.Vector;
import javax.swing.BasicSwingTestCase;
import javax.swing.undo.UndoableEdit;

public class StringContentTest_CommonTest extends GapContentTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        obj = content = new StringContent(30);
        obj.insertString(0, "This is a test string.");
    }

    @Override
    public void testGetCharsImpliedCharPartial() throws BadLocationException {
        obj = content = new StringContent();
        assertEquals(1, content.length());
        text.setPartialReturn(false);
        content.getChars(0, 1, text);
        assertEquals("\n", text.toString());
        text.setPartialReturn(true);
        content.getChars(0, 1, text);
        assertEquals("\n", text.toString());
    }

    @Override
    public void testGetPositionsInRangeVector() throws BadLocationException {
        Vector<Object> v = new Vector<Object>();
        v.add(new Object());
        v.add(new Object());
        content.createPosition(0);
        content.createPosition(1);
        content.createPosition(2);
        ((StringContent) content).getPositionsInRange(v, 0, 3);
        if (BasicSwingTestCase.isHarmony()) {
            // Position at offset 0 WILL NOT be included
            assertEquals(4, v.size());
        } else {
            // Position at offset 0 WILL be included
            assertEquals(5, v.size());
        }
    }

    /**
     * Tests that the position at offset of offset + len is included in
     * the returned vector.
     */
    @Override
    public void testGetPositionsInRangeEnd() throws BadLocationException {
        content.createPosition(10);
        Vector<?> v = ((StringContent) content).getPositionsInRange(null, 0, 10);
        assertEquals(1, v.size());
    }

    @Override
    public void testGetPositionsInRange() throws BadLocationException {
        Vector<Position> pos = new Vector<Position>();
        for (int i = 0; i < content.length(); i += 2) {
            Position p = content.createPosition(i);
            if (i >= 3 && i <= 3 + 9) {
                pos.add(p);
            }
        }
        Vector<?> v = ((StringContent) content).getPositionsInRange(null, 3, 9);
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

    @Override
    public void testGetCharsNegativeLength() {
        // Is Already tested in StringContentTest
    }

    @Override
    public void testGetCharsAfterGapNoImplied() throws BadLocationException {
        // N/A
    }

    @Override
    public void testGetCharsAfterGap() throws BadLocationException {
        // N/A
    }

    @Override
    public void testGetCharsBeforeGap() throws BadLocationException {
        // N/A
    }

    @Override
    public void testGetCharsFullLength() throws BadLocationException {
        // N/A
    }

    @Override
    public void testGetCharsFullActualLength() throws BadLocationException {
        // N/A
    }

    @Override
    public void testGetCharsImpliedChar() throws BadLocationException {
        // N/A
    }

    @Override
    public void testGetCharsPartial() throws BadLocationException {
        // N/A
    }

    @Override
    public void testGetCharsWithGap() throws BadLocationException {
        // N/A
    }

    @Override
    public void testGetStringNegativeLength() {
        // Is Already tested in StringContentTest
    }
}
