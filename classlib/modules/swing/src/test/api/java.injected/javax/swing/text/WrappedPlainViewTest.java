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
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;

/**
 * Tests WrappedPlainView methods which require "real" initialization.
 *
 */
public class WrappedPlainViewTest extends SwingTestCase {
    private Document doc;

    private WrappedPlainView view;

    private JTextArea textArea;

    private JFrame frame;

    private Rectangle shape;

    private static final int X_AXIS = View.X_AXIS;

    private static final int Y_AXIS = View.Y_AXIS;

    private int width = 145;

    private int height = 100;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame("WrappedPlainView Test");
        doc = new PlainDocument();
        doc.insertString(0, "one, two, three, four, five\n" + "eins, zwei, drei, vier, funf\n"
                + "uno, dos, tres, cuatro, cinco", null);
        textArea = new JTextArea(doc);
        textArea.setLineWrap(true);
        frame.getContentPane().add(textArea);
        frame.setSize(width, height);
        frame.pack();
        View rootView = textArea.getUI().getRootView(textArea);
        view = (WrappedPlainView) rootView.getView(0);
        shape = textArea.getVisibleRect();
        view.setSize(shape.width, shape.height);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        frame.dispose();
    }

    private int getSum(final int axis) {
        int sum = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            View child = view.getView(i);
            sum += (int) child.getPreferredSpan(axis);
        }
        return sum;
    }

    public void testGetMaximumSpan() {
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(X_AXIS));
        assertEquals(getSum(Y_AXIS), (int) view.getMaximumSpan(Y_AXIS));
    }

    public void testGetMinimumSpan() {
        assertEquals(view.getWidth(), (int) view.getMinimumSpan(X_AXIS));
        assertEquals(getSum(Y_AXIS), (int) view.getMinimumSpan(Y_AXIS));
    }

    public void testGetPreferredSpan() {
        assertEquals(view.getWidth(), (int) view.getPreferredSpan(X_AXIS));
        assertEquals(getSum(Y_AXIS), (int) view.getPreferredSpan(Y_AXIS));
        assertEquals(shape.width, view.getWidth());
        assertEquals(shape.width, (int) view.getView(0).getPreferredSpan(X_AXIS));
    }

    public void testDrawSelectedText() throws BadLocationException {
        textArea.setText("line1\nline2");
        Graphics g = textArea.getGraphics();
        g.setFont(textArea.getFont());
        FontMetrics m = g.getFontMetrics();
        assertEquals(m.charWidth('l'), view.drawSelectedText(g, 0, 0, 0, 1));
        assertEquals(5 + m.charWidth('l'), view.drawSelectedText(g, 5, 0, 0, 1));
        assertEquals(m.stringWidth("line1"), view.drawSelectedText(g, 0, 0, 0, 5));
        assertEquals(m.stringWidth("line1\nli"), view.drawSelectedText(g, 0, 0, 0, 8));
        try {
            view.drawSelectedText(g, 0, 0, -1, 1);
            fail("BadLocationException expected");
        } catch (BadLocationException e) {
        }
        try {
            view.drawUnselectedText(g, 0, 0, 13, 13);
            fail("BadLocationException expected");
        } catch (BadLocationException e) {
        }
        try {
            view.drawSelectedText(g, 0, 0, 10, 2);
            fail("BadLocationException expected");
        } catch (BadLocationException e) {
        }
    }

    public void testDrawUnselectedText() throws BadLocationException {
        textArea.setText("line1\nline2");
        Graphics g = textArea.getGraphics();
        g.setFont(textArea.getFont());
        FontMetrics m = g.getFontMetrics();
        assertEquals(m.charWidth('l'), view.drawUnselectedText(g, 0, 0, 0, 1));
        assertEquals(5 + m.charWidth('l'), view.drawUnselectedText(g, 5, 0, 0, 1));
        assertEquals(m.stringWidth("line1"), view.drawUnselectedText(g, 0, 0, 0, 5));
        assertEquals(m.stringWidth("line1\nli"), view.drawUnselectedText(g, 0, 0, 0, 8));
        try {
            view.drawUnselectedText(g, 0, 0, -1, 1);
            fail("BadLocationException expected");
        } catch (BadLocationException e) {
        }
        try {
            view.drawUnselectedText(g, 0, 0, 13, 13);
            fail("BadLocationException expected");
        } catch (BadLocationException e) {
        }
        try {
            view.drawUnselectedText(g, 0, 0, 10, 2);
            fail("BadLocationException expected");
        } catch (BadLocationException e) {
        }
    }
    /*
     public void testPaint() {
     }
     */
}
