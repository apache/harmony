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

import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Position.Bias;

import junit.framework.TestCase;

/**
 * Tests how <code>CompositeView</code> traverses view hierarchy to calculate
 * next visual position.
 *
 * <p>The behaviour is tested using <code>BoxView</code>
 * (instead of <code>CompositeView</code>),
 * and <code>GlyphView</code> instances are used as child views.
 *
 * <p>Only <code>View.EAST</code> (right) and <code>View.WEST</code> (left)
 * directions are tested here.
 */
public class CompositeView_VisualPositionTest extends TestCase {
    private static class FlipCallResult {
        private final View view;
        private final int offset;
        private final Bias bias;
        private final boolean result;

        FlipCallResult(final View view,
                       final int offset, final Bias bias,
                       final boolean result) {
            this.view = view;
            this.offset = offset;
            this.bias = bias;
            this.result = result;
        }

        void assertValues(final View view,
                          final int offset, final Bias bias,
                          final boolean result) {
            assertSame("Flip.view", view, this.view);
            assertEquals("Flip.offset", offset, this.offset);
            assertSame("Flip.bias", bias, this.bias);
            assertEquals("Flip.result", result, this.result);
        }
    }

    private static class VisPosCallResult {
        private final View view;
        private final int offset;
        private final Bias bias;
        private final int result;
        private final Bias resultBias;

        VisPosCallResult(final View view,
                         final int offset, final Bias bias,
                         final int result, final Bias resultBias) {
            this.view = view;
            this.offset = offset;
            this.bias = bias;
            this.result = result;
            this.resultBias = resultBias;
        }

        void assertValues(final View view,
                          final int offset, final Bias bias,
                          final int result, final Bias resultBias) {
            assertSame("VisPos.view", view, this.view);
            assertEquals("VisPos.offset", offset, this.offset);
            assertSame("VisPos.bias", bias, this.bias);
            assertEquals("VisPos.result", result, this.result);
            assertSame("VisPos.resultBias", resultBias, this.resultBias);
        }
    }

    private static final Bias Forward = Bias.Forward;
    private static final Bias Backward = Bias.Backward;

    private Document doc;
    private View view;
    private Element root;
    private int length;
    private Bias[] biasRet;
    private ViewFactory factory;

    private List<VisPosCallResult> visPosCalled;
    private List<FlipCallResult> flipCalled;

    private boolean boxViewFlip;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "line 1\nthe second line is rather long\n"
                            + "the third line", null);
        root = doc.getDefaultRootElement();
        length = doc.getLength();

        factory = new ViewFactory() {
            public View create(Element element) {
                return new GlyphView(element) {
                    {
                        checkPainter();
                    }

                    @Override
                    public int viewToModel(float fx, float fy,
                                           Shape a, Bias[] bias) {
                        fail(toString() + ".viewToModel is called");
                        return super.viewToModel(fx, fy, a, bias);
                    }

                    @Override
                    public Shape modelToView(int pos, Shape a, Bias b)
                        throws BadLocationException {

                        fail(toString() + ".modelToView is called");
                        return super.modelToView(pos, a, b);
                    }

                    @Override
                    public boolean isVisible() {
                        fail(toString() + ".isVisible() is called");
                        return super.isVisible();
                    }

                    @Override
                    public int getNextVisualPositionFrom(int pos, Bias b, Shape a, int direction, Bias[] biasRet) throws BadLocationException {
                        final int result =
                            super.getNextVisualPositionFrom(pos, b, a,
                                                            direction, biasRet);
                        visPosCalled.add(new VisPosCallResult(this, pos, b, result, biasRet[0]));
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "GV[" + getStartOffset() + ", "
                                     + getEndOffset() + "]";
                    }
                };
            }
        };
        view = new BoxView(root, View.Y_AXIS) {
            {
                loadChildren(factory);
            }

            @Override
            public int viewToModel(float fx, float fy, Shape a, Bias[] bias) {
                fail("BV.viewToModel is called");
                return super.viewToModel(fx, fy, a, bias);
            }

            @Override
            public Shape modelToView(int pos, Shape a, Bias b)
                throws BadLocationException {

                fail("BV.modelToView is called");
                return super.modelToView(pos, a, b);
            }

            @Override
            protected boolean flipEastAndWestAtEnds(int position, Bias bias) {
                final boolean result =
                    position == 2 || position == 6 || position == 38
                    ? boxViewFlip
                    : super.flipEastAndWestAtEnds(position, bias);
                flipCalled.add(new FlipCallResult(this, position, bias, result));
                return result;
            }
        };
        assertEquals(root.getElementCount(), view.getViewCount());

        biasRet = new Bias[1];

        visPosCalled = new ArrayList<VisPosCallResult>();
        flipCalled = new ArrayList<FlipCallResult>();
    }

    public void testGetNextVisualPositionFrom_Right_01Edge_NonFlipped()
        throws BadLocationException {

        boxViewFlip = false;

        // Forward
        assertNextPosition(7, Forward, 6, Forward, View.EAST);
        assertEquals(2, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         6, Forward, -1, null);
        visPosCalled.get(1).assertValues(view.getView(1), -1, Forward, 7, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 6, Forward, false);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(7, Forward, 6, Backward, View.EAST);
        assertEquals(2, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         6, Backward, -1, null);
        visPosCalled.get(1).assertValues(view.getView(1), -1, Backward, 7, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 6, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Right_01Edge_Flipped()
        throws BadLocationException {

        boxViewFlip = true;

        // Forward
        assertNextPosition(-1, null, 6, Forward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         6, Forward, -1, null);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 6, Forward, true);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(-1, null, 6, Backward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         6, Backward, -1, null);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 6, Backward, true);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Right_12Edge_NonFlipped()
        throws BadLocationException {

        boxViewFlip = false;

        // Forward
        assertNextPosition(39, Forward, 38, Forward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(2),
                                         38, Forward, 39, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 38, Forward, false);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(39, Forward, 38, Backward, View.EAST);
        assertEquals(3, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(1),
                                         38, Backward, -1, null);
        visPosCalled.get(1).assertValues(view.getView(2),
                                         -1, Backward, 38, Forward);
        visPosCalled.get(2).assertValues(view.getView(2),
                                         38, Forward, 39, Forward);

        assertEquals(2, flipCalled.size());
        flipCalled.get(0).assertValues(view, 38, Backward, false);
        flipCalled.get(0).assertValues(view, 38, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Right_12Edge_Flipped()
        throws BadLocationException {

        boxViewFlip = true;

        // Forward
        assertNextPosition(39, Forward, 38, Forward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(2),
                                         38, Forward, 39, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 38, Forward, true);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(0, Forward, 38, Backward, View.EAST);
        assertEquals(2, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(1),
                                         38, Backward, -1, null);
        visPosCalled.get(1).assertValues(view.getView(0),
                                         -1, Backward, 0, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 38, Backward, true);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Right_0Middle_NonFlipped()
        throws BadLocationException {

        boxViewFlip = false;

        // Forward
        assertNextPosition(3, Forward, 2, Forward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         2, Forward, 3, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 2, Forward, false);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(3, Forward, 2, Backward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         2, Backward, 3, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 2, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Right_0Middle_Flipped()
        throws BadLocationException {

        boxViewFlip = true;

        // Forward
        assertNextPosition(3, Forward, 2, Forward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         2, Forward, 3, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 2, Forward, true);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(3, Forward, 2, Backward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         2, Backward, 3, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 2, Backward, true);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Right_AtBeginning0()
        throws BadLocationException {

        // Forward
        assertNextPosition(1, Forward, 0, Forward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         0, Forward, 1, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 0, Forward, false);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(1, Forward, 0, Backward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         0, Backward, 1, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 0, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Right_AtBeginningMinus1()
        throws BadLocationException {

        assertEquals(-1, view.getViewIndex(-1, Forward));

        // Forward
        assertNextPosition(0, Forward, -1, Forward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         -1, Forward, 0, Forward);

        assertEquals(0, flipCalled.size());

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(0, Forward, -1, Backward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         -1, Backward, 0, Forward);

        assertEquals(0, flipCalled.size());

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Right_AtBeginningMinus2()
        throws BadLocationException {

        assertEquals(-1, view.getViewIndex(-2, Forward));
        try {
            assertNextPosition(length, Forward, -2, Forward, View.EAST);
            fail("ArrayIndexOutOfBoundsException is expected");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }


    public void testGetNextVisualPositionFrom_Right_AtEndLength()
        throws BadLocationException {

        assertEquals(2, view.getViewIndex(length, Forward));

        // Forward
        assertNextPosition(-1, null, length, Forward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(2),
                                         length, Forward, -1, null);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, length, Forward, false);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(-1, null, length, Backward, View.EAST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(2),
                                         length, Backward, -1, null);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, length, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Right_AtEndLength1()
        throws BadLocationException {

        assertEquals(-1, view.getViewIndex(length + 1, Forward));
        try {
            assertNextPosition(length, Forward, length + 1, Forward, View.EAST);
            fail("ArrayIndexOutOfBoundsException is expected");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }



    public void testGetNextVisualPositionFrom_Left_01Edge_NonFlipped()
        throws BadLocationException {

        boxViewFlip = false;

        // Forward
        assertNextPosition(5, Forward, 6, Forward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         6, Forward, 5, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 6, Forward, false);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(5, Forward, 6, Backward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         6, Backward, 5, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 6, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Left_01Edge_Flipped()
        throws BadLocationException {

        boxViewFlip = true;

        // Forward
        assertNextPosition(5, Forward, 6, Forward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         6, Forward, 5, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 6, Forward, true);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(5, Forward, 6, Backward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         6, Backward, 5, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 6, Backward, true);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Left_12Edge_NonFlipped()
        throws BadLocationException {

        boxViewFlip = false;

        // Forward
        assertNextPosition(37, Forward, 38, Forward, View.WEST);
        assertEquals(2, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(2),
                                         38, Forward, -1, null);
        visPosCalled.get(1).assertValues(view.getView(1),
                                         -1, Forward, 37, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 38, Forward, false);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(37, Forward, 38, Backward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(1),
                                         38, Backward, 37, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 38, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Left_12Edge_Flipped()
        throws BadLocationException {

        boxViewFlip = true;

        // Forward
        assertNextPosition(-1, null, 38, Forward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(2),
                                         38, Forward, -1, null);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 38, Forward, true);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(37, Forward, 38, Backward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(1),
                                         38, Backward, 37, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 38, Backward, true);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Left_0Middle_NonFlipped()
        throws BadLocationException {

        boxViewFlip = false;

        // Forward
        assertNextPosition(1, Forward, 2, Forward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         2, Forward, 1, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 2, Forward, false);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(1, Forward, 2, Backward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         2, Backward, 1, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 2, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Left_0Middle_Flipped()
        throws BadLocationException {

        boxViewFlip = true;

        // Forward
        assertNextPosition(1, Forward, 2, Forward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         2, Forward, 1, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 2, Forward, true);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(1, Forward, 2, Backward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         2, Backward, 1, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 2, Backward, true);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Left_AtBeginning0()
        throws BadLocationException {

        // Forward
        assertNextPosition(-1, null, 0, Forward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         0, Forward, -1, null);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 0, Forward, false);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(-1, null, 0, Backward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(0),
                                         0, Backward, -1, null);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, 0, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Left_AtBeginningMinus1()
        throws BadLocationException {

        // Forward
        assertNextPosition(length, Forward, -1, Forward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(2),
                                         -1, Forward, length, Forward);

        assertEquals(0, flipCalled.size());

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(length, Forward, -1, Backward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(2),
                                         -1, Backward, length, Forward);

        assertEquals(0, flipCalled.size());
//        flipCalled.get(0).assertValues(view, 0, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Left_AtBeginningMinus2()
    throws BadLocationException {

        try {
            assertNextPosition(length, Forward, -2, Forward, View.WEST);
            fail("ArrayIndexOutOfBoundsException is expected");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }


    public void testGetNextVisualPositionFrom_Left_AtEndLength()
        throws BadLocationException {

        // Forward
        assertNextPosition(length - 1, Forward, length, Forward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(2),
                                         length, Forward, length - 1, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, length, Forward, false);

        visPosCalled.clear();
        flipCalled.clear();


        // Backward
        assertNextPosition(length - 1, Forward, length, Backward, View.WEST);
        assertEquals(1, visPosCalled.size());
        visPosCalled.get(0).assertValues(view.getView(2),
                                         length, Backward, length - 1, Forward);

        assertEquals(1, flipCalled.size());
        flipCalled.get(0).assertValues(view, length, Backward, false);

        visPosCalled.clear();
        flipCalled.clear();
    }

    public void testGetNextVisualPositionFrom_Left_AtEndLength1()
        throws BadLocationException {

        try {
            assertNextPosition(length, Forward, length + 1, Forward, View.WEST);
            fail("ArrayIndexOutOfBoundsException is expected");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }


    private void assertNextPosition(final int expectedPosition,
                                    final Bias expectedBias,
                                    final int position,
                                    final Bias bias,
                                    final int direction)
        throws BadLocationException {

        biasRet[0] = null;
        assertEquals(bias + " at " + position,
                     expectedPosition,
                     view.getNextVisualPositionFrom(position, bias,
                                                    null, direction,
                                                    biasRet));
        assertSame(expectedBias, biasRet[0]);
    }
}
