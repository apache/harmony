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
 *
 */
package javax.swing.text;

import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.BasicSwingTestCase;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.text.CompositeViewTest.CompositeViewImpl;
import javax.swing.text.Position.Bias;
import javax.swing.text.ViewTestHelpers.ChildView;
import javax.swing.text.ViewTestHelpers.ChildrenFactory;

public class CompositeView_NextNSVisPosTest extends SwingTestCase {
    private Document doc;

    private Element root;

    private CompositeView view;

    private Shape shape;

    private JTextArea area;

    private Bias[] bias;

    @Override
    protected void setUp() throws Exception {
        area = new JTextArea() {
            private static final long serialVersionUID = 1L;

            // Change the behavior as if area has our view
            @Override
            public Rectangle modelToView(final int pos) throws BadLocationException {
                return (Rectangle) view.modelToView(pos, shape, Bias.Forward);
            }
        };
        area.setSize(100, 150);
        doc = area.getDocument();
        doc.insertString(0, "line1\nline2\n\u05DC\u05DD\nline3\n", null);
        // positions:        012345 678901 2     3     4 567890
        //                   0          1                     2
        root = doc.getDefaultRootElement();
        view = new CompositeViewImpl(root) {
            private int getY(final ChildView view) {
                int result = 0;
                for (int i = 0; i < view.getID(); i++) {
                    result += ViewTestHelpers.getHeight(i);
                }
                return result;
            }

            @Override
            protected void childAllocation(final int index, final Rectangle rc) {
                // The each view allocation is 16 pixels of height and
                // represents a line-like rectangle
                ChildView view = (ChildView) getView(index);
                rc.y += getY(view);
                rc.height = ViewTestHelpers.getHeight(view.getID());
            }

            @Override
            protected View getViewAtPoint(final int x, final int y, final Rectangle rc) {
                int index = getViewIndex(x, y, rc);
                if (index != -1) {
                    childAllocation(index, rc);
                    return getView(index);
                }
                return null;
            }

            // Link text component (area) with the view
            @Override
            public Container getContainer() {
                return area;
            }
        };
        view.loadChildren(new ChildrenFactory());
        shape = new Rectangle(100, 200, 190, 560);
        bias = new Bias[1];
        super.setUp();
    }

    public void testGetNextEastWestVisualPositionFrom() throws BadLocationException {
        // EAST
        assertEquals(1, view.getNextEastWestVisualPositionFrom(0, Bias.Forward, shape,
                SwingConstants.EAST, bias));
        assertEquals(2, view.getNextEastWestVisualPositionFrom(1, Bias.Forward, shape,
                SwingConstants.EAST, bias));
        // WEST
        assertEquals(1, view.getNextEastWestVisualPositionFrom(2, Bias.Forward, shape,
                SwingConstants.WEST, bias));
        assertEquals(0, view.getNextEastWestVisualPositionFrom(1, Bias.Forward, shape,
                SwingConstants.WEST, bias));
        // Invalid offset
        try {
            view.getNextEastWestVisualPositionFrom(-1, Bias.Forward, shape,
                    SwingConstants.EAST, bias);
            if (BasicSwingTestCase.isHarmony()) {
                fail("BadLocationException must be thrown");
            }
        } catch (BadLocationException e) {
        }
        try {
            view.getNextEastWestVisualPositionFrom(doc.getLength() + 2, Bias.Forward, shape,
                    SwingConstants.EAST, bias);
            if (BasicSwingTestCase.isHarmony()) {
                fail("BadLocationException must be thrown");
            }
        } catch (BadLocationException e) {
        } catch (ArrayIndexOutOfBoundsException e) {
            if (BasicSwingTestCase.isHarmony()) {
                fail("ArrayIndexOutOfBoundsException must not be thrown");
            }
        }
        // Invalid direction
        try {
            view.getNextEastWestVisualPositionFrom(0, Bias.Backward, shape,
                    SwingConstants.NORTH_EAST, bias);
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetNextNorthSouthVisualPositionFrom() throws BadLocationException {
        // NORTH
        assertEquals(-1, view.getNextNorthSouthVisualPositionFrom(0, Bias.Forward, shape,
                SwingConstants.NORTH, bias));
        assertEquals(1, view.getNextNorthSouthVisualPositionFrom(7, Bias.Forward, shape,
                SwingConstants.NORTH, bias));
        // SOUTH
        assertEquals(8, view.getNextNorthSouthVisualPositionFrom(2, Bias.Forward, shape,
                SwingConstants.SOUTH, bias));
        assertEquals(14, view.getNextNorthSouthVisualPositionFrom(8, Bias.Forward, shape,
                SwingConstants.SOUTH, bias));
        try {
            view.getNextNorthSouthVisualPositionFrom(-1, Bias.Forward, shape,
                    SwingConstants.NORTH, bias);
            if (BasicSwingTestCase.isHarmony()) {
                fail("BadLocationException must be thrown");
            }
        } catch (BadLocationException e) {
        }
        // Invalid direction
        try {
            view.getNextNorthSouthVisualPositionFrom(0, Bias.Backward, shape,
                    SwingConstants.NORTH_EAST, bias);
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
    }
}
