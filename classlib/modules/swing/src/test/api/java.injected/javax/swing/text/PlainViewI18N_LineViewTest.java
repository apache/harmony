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

import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;

/**
 * Tests PlainViewI18N.LineView class.
 */
public class PlainViewI18N_LineViewTest extends SwingTestCase {
    /**
     * This is helper class which has a JTextArea as a container without
     * actually being associated with it. (Having it doesn't require
     * to create JFrame etc.)
     */
    public static class PlainViewI18NWithTextArea extends PlainViewI18N {
        public final JTextArea textArea;

        public PlainViewI18NWithTextArea(Element element, Document doc) {
            super(element);
            textArea = new JTextArea(doc);
        }

        @Override
        public Container getContainer() {
            return textArea;
        }
    }

    public static final String LTR = "\u0061\u0062";

    public static final String RTL = "\u05DC\u05DD";

    public static final String newLine = "\n";

    public static final String defText = LTR + newLine + RTL + newLine + LTR + RTL + newLine
            + RTL + LTR + RTL;

    public static final int LTRLength = LTR.length();

    public static final int RTLLength = RTL.length();

    private static final int HEIGHT = 200;

    private static final int WIDTH = 100;

    private static final int X_AXIS = View.X_AXIS;

    private static final int Y_AXIS = View.Y_AXIS;

    private Segment buffer;

    private PlainDocument doc;

    private PlainViewI18N parent;

    private Element root;

    private PlainViewI18N.LineView view;

    /**
     * Tests getPreferredSpan with LRT only text
     */
    public void testGetPreferredSpan01() throws Exception {
        if (!isHarmony()) {
            return;
        }
        Element line = root.getElement(0);
        view = parent.new LineView(line);
        view.setParent(parent);
        parent.setSize(WIDTH, HEIGHT); // this will initialize metrics
        final FontMetrics metrics = getMetrics();
        assertEquals(metrics.stringWidth(getViewText()), (int) view.getPreferredSpan(X_AXIS));
        assertEquals(metrics.getHeight(), (int) view.getPreferredSpan(Y_AXIS));
        // Insert tab character and recreate view
        doc.insertString(line.getStartOffset() + 1, "\t", null);
        view = parent.new LineView(line);
        view.setParent(parent);
        assertEquals(Utilities.getTabbedTextWidth(getViewTextSegment(), metrics, 0, parent, 0),
                (int) view.getPreferredSpan(X_AXIS));
        assertEquals(metrics.getHeight(), (int) view.getPreferredSpan(Y_AXIS));
    }

    /**
     * Tests getPreferredSpan with RTL only text
     */
    public void testGetPreferredSpan02() throws Exception {
        if (!isHarmony()) {
            return;
        }
        Element line = root.getElement(1);
        view = parent.new LineView(line);
        view.setParent(parent);
        parent.setSize(WIDTH, HEIGHT); // this will initialize metrics
        final FontMetrics metrics = getMetrics();
        assertEquals(metrics.stringWidth(getViewText()), (int) view.getPreferredSpan(X_AXIS));
        assertEquals(metrics.getHeight(), (int) view.getPreferredSpan(Y_AXIS));
        // Insert tab character and recreate view
        doc.insertString(line.getStartOffset() + 1, "\t", null);
        view = parent.new LineView(line);
        view.setParent(parent);
        assertEquals(4, view.getViewCount());
        assertEquals(Utilities.getTabbedTextWidth(getViewTextSegment(), metrics, 0, parent, 0),
                (int) view.getPreferredSpan(X_AXIS));
        assertEquals(metrics.getHeight(), (int) view.getPreferredSpan(Y_AXIS));
    }

    public void testGetResizeWeight() {
        if (!isHarmony()) {
            return;
        }
        parent.getPreferredSpan(X_AXIS); // Update metrics
        view = parent.new LineView(root.getElement(3));
        view.loadChildren(null);
        assertEquals(0, view.getResizeWeight(X_AXIS));
        assertEquals(0, view.getResizeWeight(Y_AXIS));
    }

    /**
     * Tests constructor and loadChildren behaviour: start/end offset and
     * number of children.
     * <b>Only LTR text</b>.
     */
    public void testLineView01() {
        if (!isHarmony()) {
            return;
        }
        view = parent.new LineView(root.getElement(0));
        view.loadChildren(null);
        assertEquals(1, view.getViewCount());
        checkChild(view.getView(0), view.getStartOffset(), view.getEndOffset());
    }

    /**
     * <b>Only RTL text</b>.
     */
    public void testLineView02() {
        if (!isHarmony()) {
            return;
        }
        view = parent.new LineView(root.getElement(1));
        view.loadChildren(null);
        assertEquals(1, view.getViewCount());
        checkChild(view.getView(0), view.getStartOffset(), view.getEndOffset());
    }

    /**
     * <b>LTR + RTL</b>.
     */
    public void testLineView03() {
        if (!isHarmony()) {
            return;
        }
        view = parent.new LineView(root.getElement(2));
        view.loadChildren(null);
        assertEquals(3, view.getViewCount());
        int offset = view.getStartOffset();
        checkChild(view.getView(0), offset, offset + LTRLength);
        offset += LTRLength;
        checkChild(view.getView(1), offset, offset + RTLLength);
        offset += RTLLength;
        checkChild(view.getView(2), offset, offset + newLine.length());
    }

    /**
     * <b>RTL + LTR + RTL</b>.
     */
    public void testLineView04() {
        if (!isHarmony()) {
            return;
        }
        view = parent.new LineView(root.getElement(3));
        view.loadChildren(null);
        assertEquals(3, view.getViewCount());
        int offset = view.getStartOffset();
        checkChild(view.getView(0), offset, offset + RTLLength);
        offset += RTLLength;
        checkChild(view.getView(1), offset, offset + LTRLength);
        offset += LTRLength;
        checkChild(view.getView(2), offset, offset + RTLLength + 1); // +newLine
    }

    public void testIsAfter() throws Exception {
        // Regression for HARMONY-2212
        if (!isHarmony()) {
            return;
        }
        view = parent.new LineView(root.getElement(0));
        view.loadChildren(null);
        assertEquals(1, view.getViewCount());
        assertFalse(view.isAfter(31, 10, new Rectangle(30, 5, 5, 10)));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!isHarmony()) {
            return;
        }
        doc = new PlainDocument();
        doc.insertString(0, defText, null);
        root = doc.getDefaultRootElement();
        buffer = new Segment();
        parent = new PlainViewI18NWithTextArea(root, doc);
    }

    private void checkChild(View child, int start, int end) {
        assertSame("Element of a child must equal to one of parent", view.getElement(), child
                .getElement());
        assertEquals("Start offsets are different", start, child.getStartOffset());
        assertEquals("End offsets are different", end, child.getEndOffset());
    }

    private FontMetrics getMetrics() {
        final Container container = view.getContainer();
        return container.getFontMetrics(container.getFont());
    }

    private String getViewText() throws BadLocationException {
        return getViewTextSegment().toString();
    }

    private Segment getViewTextSegment() throws BadLocationException {
        final int start = view.getStartOffset();
        final int end = view.getEndOffset();
        doc.getText(start, end - start - 1, buffer);
        return buffer;
    }
}
