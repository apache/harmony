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

import static javax.swing.text.VisualPositionHelper.assertNextPosition;
import junit.framework.TestCase;

/**
 * Tests <code>View.getNextVisualPositionFrom</code> method on
 * <code>PlainView</code> which doesn't overrides this method.
 *
 * <p>The view is constructed on the <em>root</em> element of
 * document (<code>PlainDocument</code>).
 *
 * <p>Only <code>View.EAST</code> (right) and <code>View.WEST</code> (left)
 * directions are tested here.
 */
public class View_VisualPositionTest extends TestCase {
    private Document doc;
    private View view;
    private Element root;
    private int length;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "line 1\nthe second line is rather long\n"
                            + "the third line", null);
        length = doc.getLength();

        root = doc.getDefaultRootElement();
        view = new PlainView(root);
    }

    public void testGetNextVisualPositionFrom_Right() throws BadLocationException {
        for (int i = 0; i < length; i++) {
            assertNextPosition(i + 1, i, View.EAST, view, null);
        };
    }

    public void testGetNextVisualPositionFrom_RightAtBeginning()
        throws BadLocationException {

        assertNextPosition(0, -1, View.EAST, view, null);

        assertNextPosition(-1, -2, View.EAST, view, null);
        assertNextPosition(-2, -3, View.EAST, view, null);
        assertNextPosition(-3, -4, View.EAST, view, null);

        assertNextPosition(-9, -10, View.EAST, view, null);
    }

    public void testGetNextVisualPositionFrom_RightAtEnd()
        throws BadLocationException {

        assertNextPosition(length, length - 1, View.EAST, view, null);
        assertNextPosition(length, length, View.EAST, view, null);

        assertNextPosition(length, length + 1, View.EAST, view, null);
        assertNextPosition(length, length + 2, View.EAST, view, null);
        assertNextPosition(length, length + 3, View.EAST, view, null);

        assertNextPosition(length, length + 10, View.EAST, view, null);
    }

    public void testGetNextVisualPositionFrom_Left() throws BadLocationException {
        for (int i = 1; i <= length; i++) {
            assertNextPosition(i - 1, i, View.WEST, view, null);
        };
    }

    public void testGetNextVisualPositionFrom_LeftAtBeginning()
        throws BadLocationException {

        assertNextPosition(0, 1, View.WEST, view, null);
        assertNextPosition(0, 0, View.WEST, view, null);

        assertNextPosition(length, -1, View.WEST, view, null);

        assertNextPosition(0, -2, View.WEST, view, null);
        assertNextPosition(0, -3, View.WEST, view, null);

        assertNextPosition(0, -10, View.WEST, view, null);
    }

    public void testGetNextVisualPositionFrom_LeftAtEnd()
        throws BadLocationException {

        assertNextPosition(length - 1, length, View.WEST, view, null);
        assertNextPosition(length, length + 1, View.WEST, view, null);

        assertNextPosition(length + 1, length + 2, View.WEST, view, null);
        assertNextPosition(length + 2, length + 3, View.WEST, view, null);

        assertNextPosition(length + 9, length + 10, View.WEST, view, null);
    }
}
