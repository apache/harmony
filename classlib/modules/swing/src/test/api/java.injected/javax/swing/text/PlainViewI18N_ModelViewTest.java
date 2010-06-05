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

import java.awt.FontMetrics;
import java.awt.Rectangle;
import javax.swing.BasicSwingTestCase;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.text.Position.Bias;

/**
 * Tests model/view conversions in PlainViewI18N class.
 * <p>Some tests tests our implementation only since
 * getNextVisualPosition differs from the 1.5 and it was made knowingly.
 *
 */
public class PlainViewI18N_ModelViewTest extends SwingTestCase {
    private Document doc;

    private View view;

    private Rectangle shape;

    private JFrame frame;

    private JTextArea area;

    private Bias[] bias;

    private FontMetrics metrics;

    private int rtlWidth;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame("PlainViewI18N Model/View Test");
        area = new JTextArea("\u05DC\u05DD\t\u05DE\u05DF\u05E0abcd");
        //                    0     1     2 3     4     5     6789
        frame.getContentPane().add(area);
        frame.setSize(150, 100);
        frame.pack();
        doc = area.getDocument();
        view = area.getUI().getRootView(area).getView(0);
        shape = area.getVisibleRect();
        bias = new Bias[1];
        metrics = area.getFontMetrics(area.getFont());
        rtlWidth = metrics.stringWidth(doc.getText(0, 2));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        frame.dispose();
    }

    /**
     * Tests view.getNextVisualPositionFrom position 0 when moving EAST
     * with Forward and Backward biases.
     */
    public void testGetNextVisualPositionEast01() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        assertEquals(2, view.getNextVisualPositionFrom(0, Bias.Forward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(2, view.getNextVisualPositionFrom(0, Bias.Backward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Backward, bias[0]);
        bias[0] = null;
    }

    /**
     * Tests view.getNextVisualPositionFrom position 1 when moving EAST
     * with Forward and Backward biases.
     */
    public void testGetNextVisualPositionEast02() throws BadLocationException {
        assertEquals(0, view.getNextVisualPositionFrom(1, Bias.Forward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(0, view.getNextVisualPositionFrom(1, Bias.Backward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    /**
     * Tests view.getNextVisualPositionFrom position 2 when moving EAST
     * with Forward and Backward biases.
     */
    public void testGetNextVisualPositionEast03() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        assertEquals(3, view.getNextVisualPositionFrom(2, Bias.Forward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Backward, bias[0]);
        bias[0] = null;
        assertEquals(1, view.getNextVisualPositionFrom(2, Bias.Backward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    /**
     * Tests view.getNextVisualPositionFrom position 2 when moving EAST
     * with Forward and Backward biases.
     */
    public void testGetNextVisualPositionEast04() throws BadLocationException {
        assertEquals(6, view.getNextVisualPositionFrom(3, Bias.Forward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(6, view.getNextVisualPositionFrom(3, Bias.Backward, shape,
                SwingConstants.EAST, bias));
        assertSame(Bias.Backward, bias[0]);
        bias[0] = null;
    }

    /**
     * Tests view.getNextVisualPositionFrom position 0 when moving WEST
     * with Forward and Backward biases.
     */
    public void testGetNextVisualPositionWest01() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        assertEquals(1, view.getNextVisualPositionFrom(0, Bias.Forward, shape,
                SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(-1, view.getNextVisualPositionFrom(0, Bias.Backward, shape,
                SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    /**
     * Tests view.getNextVisualPositionFrom position 1 when moving WEST
     * with Forward and Backward biases.
     */
    public void testGetNextVisualPositionWest02() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        assertEquals(2, view.getNextVisualPositionFrom(1, Bias.Forward, shape,
                SwingConstants.WEST, bias));
        assertSame(Bias.Backward, bias[0]);
        bias[0] = null;
        assertEquals(2, view.getNextVisualPositionFrom(1, Bias.Backward, shape,
                SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    /**
     * Tests view.getNextVisualPositionFrom position 2 when moving WEST
     * with Forward and Backward biases.
     */
    public void testGetNextVisualPositionWest03() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        assertEquals(0, view.getNextVisualPositionFrom(2, Bias.Forward, shape,
                SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(0, view.getNextVisualPositionFrom(2, Bias.Backward, shape,
                SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    /**
     * Tests view.getNextVisualPositionFrom position 2 when moving WEST
     * with Forward and Backward biases.
     */
    public void testGetNextVisualPositionWest04() throws BadLocationException {
        assertEquals(4, view.getNextVisualPositionFrom(3, Bias.Forward, shape,
                SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(2, view.getNextVisualPositionFrom(3, Bias.Backward, shape,
                SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    private static void assertRectEquals(Rectangle rc, int x, int y, int width, int height) {
        assertEquals("Unexpected X", x, rc.x);
        assertEquals("Unexpected Y", y, rc.y);
        assertEquals("Unexpected width", width, rc.width);
        assertEquals("Unexpected height", height, rc.height);
    }

    /**
     * Shows that modelToView may return the same value while positions
     * are different (biases are the same).
     * <p>In the same time it may return different values for the same offset
     * when biases are different.
     * <p>The offsets used are 0, 2.
     */
    public void testModelToView01() throws Exception {
        Rectangle viewAt0F = (Rectangle) view.modelToView(0, shape, Bias.Forward);
        Rectangle viewAt2B = (Rectangle) view.modelToView(2, shape, Bias.Forward);
        assertEquals(viewAt0F.x, viewAt2B.x);
        final int beforeTabWidth = metrics.stringWidth(doc.getText(0, 2));
        assertRectEquals(viewAt0F, beforeTabWidth, 0, 1, metrics.getHeight());
        assertRectEquals((Rectangle) view.modelToView(0, shape, Bias.Backward),
                BasicSwingTestCase.isHarmony() ? beforeTabWidth : 0, 0, 1, metrics.getHeight());
    }

    public void testModelToView02() throws Exception {
        Rectangle viewAt1F = (Rectangle) view.modelToView(1, shape, Bias.Forward);
        Rectangle viewAt1B = (Rectangle) view.modelToView(1, shape, Bias.Backward);
        assertEquals(viewAt1F, viewAt1B);
        assertRectEquals(viewAt1F, metrics.stringWidth(doc.getText(1, 1)), 0, 1, metrics
                .getHeight());
    }

    public void testViewToModel() throws Exception {
        assertEquals(2, view.viewToModel(rtlWidth, 0, shape, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(2, view.viewToModel(0, 0, shape, bias));
        assertSame(Bias.Backward, bias[0]);
        bias[0] = null;
        assertEquals(0, view.viewToModel(rtlWidth - 1, 0, shape, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(2, view.viewToModel(rtlWidth + 1, 0, shape, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    public void testViewToModelRTL() throws Exception {
        View rtl;
        if (BasicSwingTestCase.isHarmony()) {
            rtl = view.getView(0).getView(0);
        } else {
            rtl = view.getView(0).getView(0).getView(0);
        }
        assertEquals(0, rtl.getStartOffset());
        assertEquals(2, rtl.getEndOffset());
        assertEquals(2, rtl.viewToModel(0, 0, shape, bias));
        assertSame(Bias.Backward, bias[0]);
        bias[0] = null;
        assertEquals(2, rtl.viewToModel(rtlWidth, 0, shape, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(2, rtl.viewToModel(rtlWidth + 1, 0, shape, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    public void testViewToModelLTR() throws Exception {
        View ltr;
        if (BasicSwingTestCase.isHarmony()) {
            ltr = view.getView(0).getView(1);
        } else {
            ltr = view.getView(0).getView(0).getView(1);
        }
        assertEquals(2, ltr.getStartOffset());
        assertEquals(3, ltr.getEndOffset());
        assertEquals(2, ltr.viewToModel(rtlWidth, 0, shape, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(2, ltr.viewToModel(rtlWidth - 1, 0, shape, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(2, ltr.viewToModel(rtlWidth + 1, 0, shape, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        Object tabProperty = view.getDocument().getProperty(PlainDocument.tabSizeAttribute);
        int tab = metrics.charWidth('m') * ((Integer) tabProperty).intValue();
        assertEquals(BasicSwingTestCase.isHarmony() ? 3 : 2, ltr.viewToModel(tab, 0, shape,
                bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }
}
