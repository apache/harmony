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

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;
import javax.swing.text.Position.Bias;

/**
 * Tests PlainViewI18N class, in particular how views are laid out.
 *
 */
public class PlainViewI18N_LayoutTest extends SwingTestCase {
    private JTextArea area;

    private Document doc;

    private JFrame frame;

    private View rootView;

    private CompositeView view;

    /**
     * Tests values returned by <code>flipEastAndWestAtEnds()</code>.
     */
    public void testFlipEastAndWest() throws Exception {
        boolean[] forward = new boolean[] { false, false, false, false, false, false, false,
                false, false, false, false, false };
        boolean[] backward = new boolean[] { false, false, false, false, false, false, false,
                false, false, false, false, false };
        getLineView();
        final int length = doc.getLength() + 1;
        for (int i = 0; i <= length; i++) {
            assertEquals("Bias.Forward[" + i + "]", forward[i], view.flipEastAndWestAtEnds(i,
                    Bias.Forward));
            assertEquals("Bias.Backward[" + i + "]", backward[i], view.flipEastAndWestAtEnds(i,
                    Bias.Backward));
        }
    }

    /**
     * Tests how views are laid out: which parts of text they are
     * resposible for.
     */
    public void testViewLayout() throws Exception {
        int[] levels = new int[] { 0, 1, 2, 1, 0 };
        Element bidiRoot = ((AbstractDocument) doc).getBidiRootElement();
        assertEquals(5, bidiRoot.getElementCount());
        for (int i = 0; i < levels.length; i++) {
            Element child = bidiRoot.getElement(i);
            assertEquals(levels[i], StyleConstants.getBidiLevel(child.getAttributes()));
        }
        int[] viewPos = new int[] { 0, 2, 6, 8, 4, 6, 2, 4, 8, 11 };
        getLineView();
        assertEquals(5, view.getViewCount());
        for (int i = 0, posIndex = 0; i < levels.length; i++, posIndex += 2) {
            View child = view.getView(i);
            assertEquals("Start", viewPos[posIndex], child.getStartOffset());
            assertEquals("End", viewPos[posIndex + 1], child.getEndOffset());
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame("PlainViewI18N Layout Test");
        area = new JTextArea("ab\u05DC\u05DD12\u05DE\u05DFcd");
        //                    012     3     456     7     89
        frame.getContentPane().add(area);
        frame.setSize(150, 100);
        frame.pack();
        doc = area.getDocument();
        rootView = area.getUI().getRootView(area).getView(0);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        frame.dispose();
    }

    private void getLineView() {
        if (isHarmony()) {
            view = (CompositeView) rootView.getView(0);
        } else {
            view = (CompositeView) rootView.getView(0).getView(0);
        }
    }
}
