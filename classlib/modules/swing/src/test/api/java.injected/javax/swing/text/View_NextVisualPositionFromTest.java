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

import java.awt.Point;
import java.awt.Shape;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.text.Position.Bias;

/**
 * Tests default implementation of View.getNextVisualPositionFrom method.
 *
 */
public class View_NextVisualPositionFromTest extends SwingTestCase {
    /**
     * Specialized caret to get magic caret position. This ensures
     * the one has expected value and don't change.
     */
    private static class MagicCaret extends DefaultCaret {
        private static final long serialVersionUID = 1L;

        public Point magicPoint;

        @Override
        public Point getMagicCaretPosition() {
            return magicPoint;
        }
    }

    private JTextArea area;

    private MagicCaret caret;

    private Document doc;

    private JFrame frame;

    private Shape shape;

    private View view;

    private Bias[] bias;

    public void testGetNextVisualPositionFromEast() throws BadLocationException {
        assertEquals(1, view.getNextVisualPositionFrom(0, Bias.Forward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(1, view.getNextVisualPositionFrom(0, Bias.Backward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(2, view.getNextVisualPositionFrom(1, Bias.Forward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(2, view.getNextVisualPositionFrom(1, Bias.Backward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        // End of document: try to go further
        assertEquals(doc.getLength(), view.getNextVisualPositionFrom(doc.getLength(),
                Bias.Forward, shape, SwingConstants.EAST, bias));
    }

    public void testGetNextVisualPositionFromWest() throws BadLocationException {
        assertEquals(1, view.getNextVisualPositionFrom(2, Bias.Forward, shape,
                SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(1, view.getNextVisualPositionFrom(2, Bias.Backward, shape,
                SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    public void testGetNextVisualPositionFromNorth() throws BadLocationException {
        Shape posRect = view.modelToView(doc.getLength(), shape, Bias.Forward);
        caret.magicPoint = posRect.getBounds().getLocation();
        assertNotNull(caret.getMagicCaretPosition());
        assertEquals(17, view.getNextVisualPositionFrom(doc.getLength(), Bias.Forward, shape,
                SwingConstants.NORTH, bias));
        assertEquals(10, view.getNextVisualPositionFrom(17, Bias.Forward, shape,
                SwingConstants.NORTH, bias));
        assertEquals(6, view.getNextVisualPositionFrom(10, Bias.Forward, shape,
                SwingConstants.NORTH, bias));
        // We reached the first line
        assertEquals(-1, view.getNextVisualPositionFrom(6, Bias.Forward, shape,
                SwingConstants.NORTH, bias));
        // No magic position
        caret.magicPoint = null;
        assertNull(area.getCaret().getMagicCaretPosition());
        assertEquals(2, view.getNextVisualPositionFrom(10, Bias.Forward, shape,
                SwingConstants.NORTH, bias));
    }

    public void testGetNextVisualPositionFromSouth() throws BadLocationException {
        Shape posRect = view.modelToView(doc.getLength(), shape, Bias.Forward);
        caret.magicPoint = posRect.getBounds().getLocation();
        assertNotNull(area.getCaret().getMagicCaretPosition());
        assertEquals(10, view.getNextVisualPositionFrom(6, Bias.Forward, shape,
                SwingConstants.SOUTH, bias));
        assertEquals(17, view.getNextVisualPositionFrom(10, Bias.Forward, shape,
                SwingConstants.SOUTH, bias));
        // 27 == doc.getLength()
        assertEquals(27, view.getNextVisualPositionFrom(17, Bias.Forward, shape,
                SwingConstants.SOUTH, bias));
        // We reached the last line and the last character in document
        assertEquals(27, view.getNextVisualPositionFrom(27, Bias.Forward, shape,
                SwingConstants.SOUTH, bias));
        // No magic position
        caret.magicPoint = null;
        assertNull(area.getCaret().getMagicCaretPosition());
        assertEquals(2, view.getNextVisualPositionFrom(10, Bias.Forward, shape,
                SwingConstants.NORTH, bias));
        assertEquals(13, view.getNextVisualPositionFrom(10, Bias.Forward, shape,
                SwingConstants.SOUTH, bias));
    }

    public void testGetNextVisualPositionFromInvalid() throws BadLocationException {
        // BadLocation left
        try {
            int p = view.getNextVisualPositionFrom(-2, Bias.Forward, shape,
                    SwingConstants.WEST, bias);
            if (!isHarmony()) {
                assertEquals(0, p);
                // If we go to WEST from -1, we get to document end
                p = view.getNextVisualPositionFrom(-1, Bias.Forward, shape,
                        SwingConstants.WEST, bias);
                assertEquals(doc.getLength(), p);
            } else {
                fail("BadLocationException is expected");
            }
        } catch (BadLocationException e) {
        }
        // BadLocation right
        try {
            int p = view.getNextVisualPositionFrom(doc.getLength() + 1, Bias.Forward, shape,
                    SwingConstants.EAST, bias);
            if (!isHarmony()) {
                assertEquals(doc.getLength(), p);
            } else {
                fail("BadLocationException is expected");
            }
        } catch (BadLocationException e) {
        }
        // IllegalArgument for direction
        try {
            view.getNextVisualPositionFrom(0, Bias.Forward, shape, SwingConstants.NORTH_EAST,
                    bias);
            fail("IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
        }
        // ArrayIndexOutOfBounds for bias
        try {
            view.getNextVisualPositionFrom(0, Bias.Forward, shape, SwingConstants.EAST,
                    new Bias[0]);
            fail("ArrayIndexOutOfBoundsException is expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        // NullPointerException for bias
        try {
            view.getNextVisualPositionFrom(0, Bias.Forward, shape, SwingConstants.EAST, null);
            fail("NullPointerException is expected");
        } catch (NullPointerException e) {
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame("Next Position Test");
        area = new JTextArea();
        area.setCaret(caret = new MagicCaret());
        frame.getContentPane().add(area);
        area.setText("1: 0123\n" +
        //01234567
                "2:\n" +
                //890
                "3: 012345\n" +
                //1234567890
                "4: 012");
        //123456
        doc = area.getDocument();
        frame.setSize(100, 150);
        frame.pack();
        view = area.getUI().getRootView(area).getView(0);
        shape = area.getBounds();
        bias = new Bias[1];
        assertNull(bias[0]);
        assertEquals(27, doc.getLength());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        frame.dispose();
    }
}