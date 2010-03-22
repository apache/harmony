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

import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.text.Position.Bias;

/**
 * A bunch of tests for getNextVisualPositionFrom method in bidirectional
 * context. Our implementation differs from the 1.5.
 *
 */
public class PlainViewI18N_VisualPositionTest extends SwingTestCase {
    private Document doc;

    private View view;

    private Rectangle shape;

    private JFrame frame;

    private JTextArea area;

    private Bias[] bias;

    private int length;

    private static final Bias FW = Bias.Forward;

    private static final Bias BW = Bias.Backward;

    private int[] nextPositionEF = { 1, 2, 3, 4, 5, 7, 7, 8, 9, 10, 11, 14, 14, 12, 15, 18, 15,
            16, 19, 20, 21, 22, 22 };

    //    0   1   2   3   4   5   6   7   8   9
    private int[] nextPositionEB = { -1, 2, 1, 4, 3, 6, 7, 6, 9, 10, 11, 10, 14, 12, 13, 18,
            15, 16, 17, 20, 21, 22, 22 };

    private int[] nextPositionWF = { 0, 2, 1, 4, 3, 4, 7, 6, 7, 8, 11, 10, 13, 14, 12, 16, 17,
            18, 15, 18, 19, 20, 21 };

    //    0   1   2   3   4   5   6   7   8   9
    private int[] nextPositionWB = { 0, 0, 1, 2, 3, 4, 5, 5, 7, 8, 9, 10, 11, 14, 11, 14, 17,
            18, 15, 18, 19, 20, 21 };

    private Bias[] nextBiasEF = { BW, FW, BW, FW, FW, BW, FW, FW, FW, BW, FW, BW, FW, FW, BW,
            FW, FW, FW, FW, FW, FW, FW, FW };

    //    0   1   2   3   4   5   6   7   8   9
    private Bias[] nextBiasEB = { FW, BW, FW, BW, FW, FW, BW, FW, FW, FW, BW, FW, BW, FW, FW,
            BW, FW, FW, FW, FW, FW, FW, FW };

    private Bias[] nextBiasWF = { FW, BW, FW, BW, FW, FW, BW, FW, FW, FW, BW, FW, FW, BW, FW,
            FW, FW, BW, FW, FW, FW, FW, FW };

    //    0   1   2   3   4   5   6   7   8   9
    private Bias[] nextBiasWB = { FW, FW, BW, FW, BW, FW, FW, FW, FW, FW, FW, BW, FW, FW, FW,
            FW, FW, FW, BW, FW, FW, FW, FW };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame("PlainViewI18N Visual Position Test");
        area = new JTextArea("a\u05DCb\u05DDc\n" +
        //                    01     23     45
                "\u05DE123\u05DF\n" +
                //                    6     7890     1
                "\u05DC\u05DD\t\u05DE\u05DF\u05E0abcd");
        //                    2     3     4 5     6     7     8901
        frame.getContentPane().add(area);
        frame.setSize(150, 100);
        frame.pack();
        doc = area.getDocument();
        length = doc.getLength();
        view = area.getUI().getRootView(area).getView(0);
        shape = area.getVisibleRect();
        bias = new Bias[1];
    }

    @Override
    protected void tearDown() throws Exception {
        frame.dispose();
        super.tearDown();
    }

    public void testGetNextVisualPositionEastForward() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        for (int i = 0; i <= length; i++) {
            int next = view.getNextVisualPositionFrom(i, FW, shape, SwingConstants.EAST, bias);
            assertEquals("@ " + i, nextPositionEF[i], next);
            assertSame("@ " + i, nextBiasEF[i], bias[0]);
            bias[0] = null;
        }
    }

    public void testGetNextVisualPositionEastBackward() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        for (int i = 0; i <= length; i++) {
            int next = view.getNextVisualPositionFrom(i, BW, shape, SwingConstants.EAST, bias);
            assertEquals("@ " + i, nextPositionEB[i], next);
            assertSame("@ " + i, nextBiasEB[i], bias[0]);
            bias[0] = null;
        }
    }

    public void testGetNextVisualPositionWestForward() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        for (int i = 0; i <= length; i++) {
            int next = view.getNextVisualPositionFrom(i, FW, shape, SwingConstants.WEST, bias);
            assertEquals("@ " + i, nextPositionWF[i], next);
            assertSame("@ " + i, nextBiasWF[i], bias[0]);
            bias[0] = null;
        }
    }

    public void testGetNextVisualPositionWestBackward() throws BadLocationException {
        if (!isHarmony()) {
            return;
        }
        for (int i = 0; i <= length; i++) {
            int next = view.getNextVisualPositionFrom(i, BW, shape, SwingConstants.WEST, bias);
            assertEquals("@ " + i, nextPositionWB[i], next);
            assertSame("@ " + i, nextBiasWB[i], bias[0]);
            bias[0] = null;
        }
    }
}
