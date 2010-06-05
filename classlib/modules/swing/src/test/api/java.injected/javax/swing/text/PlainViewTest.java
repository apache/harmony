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
import java.awt.Shape;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;

/**
 * This class tests PlainView behavior.
 *
 * <p>This class is initialized with a "real" PlainView that was obtained from
 * JTextArea placed in JFrame.
 *
 */
public class PlainViewTest extends SwingTestCase {
    private JTextArea area;

    private Document doc;

    private JFrame frame;

    private Shape shape;

    private PlainView view;

    public void testDrawSelectedText() throws BadLocationException {
        area.setText("line1\nline2");
        Graphics g = view.getGraphics();
        FontMetrics m = view.metrics;
        g.setFont(m.getFont());
        assertEquals(m.charWidth('l'), view.drawSelectedText(g, 0, 0, 0, 1));
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
        area.setText("line1\nline2");
        Graphics g = view.getGraphics();
        FontMetrics m = view.metrics;
        g.setFont(m.getFont());
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

    public void testGetPreferredSpan() {
        area.setText("1: 0\n2: 012345\n3:\n");
        assertEquals(view.metrics.stringWidth("2: 012345"), // longest line
                view.getPreferredSpan(View.X_AXIS), 0.00001f);
        assertEquals(view.metrics.getHeight() * 4, view.getPreferredSpan(View.Y_AXIS), 0.00001f);
        area.setText("\ttext\t1");
        float length = view.nextTabStop(0, 0);
        length += view.metrics.stringWidth("text");
        length = view.nextTabStop(length, 0);
        length += view.metrics.stringWidth("1");
        assertEquals(length, view.getPreferredSpan(View.X_AXIS), 0.00001f);
    }

    /**
     * Generic tests of <code>modelToView(int, Shape, Position.Bias)</code>.
     */
    public void testModelToViewintShapeBias01() throws BadLocationException {
        area.setText("1: 0\n2: 012345\n3:\n");
        //            01234 5678901234 567
        assertTrue(view.modelToView(0, shape, Position.Bias.Backward) instanceof Rectangle);
        assertEquals(new Rectangle(1, view.metrics.getHeight()), view.modelToView(0, shape,
                Position.Bias.Forward));
        assertEquals(new Rectangle(1, view.metrics.getHeight()), view.modelToView(0, shape,
                Position.Bias.Backward));
        assertEquals(
                new Rectangle(view.metrics.charWidth('1'), 0, 1, view.metrics.getHeight()),
                view.modelToView(1, shape, Position.Bias.Forward));
        assertEquals(
                new Rectangle(view.metrics.charWidth('1'), 0, 1, view.metrics.getHeight()),
                view.modelToView(1, shape, Position.Bias.Backward));
        assertEquals(new Rectangle(view.metrics.stringWidth("2: 012"),
                view.metrics.getHeight(), 1, view.metrics.getHeight()), view.modelToView(11,
                shape, Position.Bias.Forward));
        try {
            view.modelToView(-1, shape, Position.Bias.Forward);
            fail("BadLocationException is expected");
        } catch (BadLocationException e) {
        }
        assertEquals(new Rectangle(view.metrics.charWidth('\n')/*0*/,
                view.metrics.getHeight() * 3, 1, view.metrics.getHeight()), view.modelToView(
                doc.getLength() + 1, shape, Position.Bias.Forward));
        try {
            view.modelToView(doc.getLength() + 2, shape, Position.Bias.Forward);
            fail("BadLocationException is expected");
        } catch (BadLocationException e) {
        }
        //        try {
        view.modelToView(0, shape, null);
        // isn't thrown
        //fail("IllegalArgumentException must be thrown");
        //        } catch (IllegalArgumentException e) { }
        doc.insertString(1, "\t", null);
        assertEquals(
                new Rectangle(view.metrics.charWidth('1'), 0, 1, view.metrics.getHeight()),
                view.modelToView(1, shape, Position.Bias.Forward));
        assertEquals(new Rectangle((int) view.nextTabStop(view.metrics.charWidth('1'), 0), 0,
                1, view.metrics.getHeight()), view.modelToView(2, shape, Position.Bias.Forward));
        assertEquals(new Rectangle((int) view.nextTabStop(view.metrics.charWidth('1'), 0)
                + view.metrics.charWidth(':'), 0, 1, view.metrics.getHeight()), view
                .modelToView(3, shape, Position.Bias.Forward));
    }

    /**
     * Tests <code>modelToView(int, Shape, Position.Bias)</code> when
     * <code>shape.getBounds().x != 0</code> and/or
     * <code>shape.getBounds().y != 0</code>.
     */
    public void testModelToViewintShapeBias02() throws BadLocationException {
        area.setText("1: 0\n2: 012345\n3:\n");
        ((Rectangle) shape).setLocation(7, 10);
        assertFalse(((Rectangle) shape).x == 0);
        assertEquals(new Rectangle(((Rectangle) shape).x, ((Rectangle) shape).y, 1,
                view.metrics.getHeight()), view.modelToView(0, shape, Position.Bias.Forward));
    }

    /**
     * Tests <code>modelToView(int, Shape, Position.Bias)</code>
     * with zero-length document.
     */
    public void testModelToViewintShapeBias03() throws BadLocationException {
        area.setText("");
        assertEquals(0, view.getDocument().getLength());
        assertEquals(new Rectangle(1, view.metrics.getHeight()), view.modelToView(0, shape,
                Position.Bias.Forward));
        assertEquals(new Rectangle(1, view.metrics.getHeight()), view.modelToView(1, shape,
                Position.Bias.Forward));
        try {
            view.modelToView(-1, shape, Position.Bias.Forward);
            fail("BadLocationException is expected");
        } catch (BadLocationException e) {
        }
        try {
            view.modelToView(2, shape, Position.Bias.Forward);
            fail("BadLocationException is expected");
        } catch (BadLocationException e) {
        }
    }

    /**
     * Tests nextTabStop method with default tab size of 8.
     */
    public void testNextTabStop01() {
        float tabPos = view.getTabSize() * view.metrics.charWidth('m');
        assertEquals(8, view.getTabSize());
        assertEquals(tabPos, view.nextTabStop(0.0f, 0), 0.00001f);
        assertEquals(tabPos, view.nextTabStop(10.0f, 0), 0.00001f);
        assertEquals(tabPos, view.nextTabStop(tabPos - 1, 0), 0.00001f);
        assertEquals(tabPos * 2, view.nextTabStop(tabPos, 0), 0.00001f);
        // Setting tab size to 4 has no effect on already initialized view
        doc.putProperty(PlainDocument.tabSizeAttribute, new Integer(4));
        assertEquals(4, view.getTabSize());
        // The change has no effect
        assertEquals(tabPos, view.nextTabStop(0.0f, 0), 0.00001f);
        // But after metrics have been updated...
        view.updateMetrics();
        if (isHarmony()) {
            // Our implemetation updates tabSize in updateMetrics
            tabPos = view.getTabSize() * view.metrics.charWidth('m');
        }
        assertEquals(tabPos, view.nextTabStop(0.0f, 0), 0.00001f);
        assertEquals(tabPos * 2, view.nextTabStop(tabPos, 0), 0.00001f);
    }

    /*
     * int viewToModel(float, float, Shape, Position.Bias[])
     */
    public void testViewToModelfloatfloatShapeBiasArray() throws BadLocationException {
        area.setText("1: 0\n2: 012345\n3:\n");
        //            01234 5678901234 567
        int h = view.metrics.getHeight();
        int w = view.metrics.charWidth('1');
        Position.Bias[] bias = new Position.Bias[1];
        assertNull(bias[0]);
        assertEquals(0, view.viewToModel(0f, 0f, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(0, view.viewToModel(w / 4f, h / 4f, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(1, view.viewToModel(w - 1f, h / 2f, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(1, view.viewToModel(w - w / 4f, h / 2f, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        w = view.metrics.charWidth('2');
        // Negative coordinates
        assertEquals(0, view.viewToModel(-1f, h - 0.1f, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(5, view.viewToModel(-1f, h, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(5, view.viewToModel(-1f, h + 0.1f, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(0, view.viewToModel(1f, -1f, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(0, view.viewToModel(w + 1f, -1f, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        // Past last character of line 1
        assertEquals(4, view.viewToModel(view.metrics.stringWidth("1: 0") + 1f, h / 2, shape,
                bias));
        assertEquals(4, view.viewToModel(view.metrics.stringWidth("1: 0") + 1f, h - 0.1f,
                shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(9, view.viewToModel(view.metrics.stringWidth("1: 0"), h, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(9, view.viewToModel(view.metrics.stringWidth("1: 0"), h + 0.1f, shape,
                bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        // Below last line
        h = (int) view.getPreferredSpan(View.Y_AXIS);
        int pos = doc.getLength();
        assertEquals(pos, view.viewToModel(0f, h - 0.1f, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(pos, view.viewToModel(0f, h, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(pos, view.viewToModel(0f, h + 0.1f, shape, bias));
        assertSame(Position.Bias.Forward, bias[0]);
        bias[0] = null;
        // Test with tab
        w = view.metrics.charWidth('1');
        doc.insertString(1, "\t", null);
        int tab = (int) view.nextTabStop(w, 0);
        int tabSize = tab - w;
        assertEquals(1, view.viewToModel(w + tabSize / 2f - 0.5f, 0f, shape, bias));
        assertEquals(2, view.viewToModel(w + tabSize / 2f + 0.5f, 0f, shape, bias));
        assertEquals(2, view.viewToModel(tab - 1f, 0f, shape, bias));
        assertEquals(3, view
                .viewToModel(tab + view.metrics.charWidth(':') - 1f, 0, shape, bias));
    }

    /**
     * Creates JFrame (<code>frame</code>), puts JTextArea (<code>area</code>)
     * into it and initializes <code>doc</code>, <code>root</code>,
     * <code>view</code>, and <code>shape</code> using JTextArea methods.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame("PlainView Test");
        area = new JTextArea(" ");
        frame.getContentPane().add(area);
        frame.setSize(100, 150);
        frame.pack();
        doc = area.getDocument();
        view = (PlainView) area.getUI().getRootView(area).getView(0);
        shape = area.getVisibleRect();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        frame.dispose();
    }
    // Painting methods are not tested
    /*
     public void testDrawLine() {
     }

     public void testPaint() {
     }
     */
}
