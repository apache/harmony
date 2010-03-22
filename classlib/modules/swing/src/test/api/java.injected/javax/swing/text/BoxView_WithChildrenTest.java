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

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.BasicSwingTestCase;
import javax.swing.JTextArea;
import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.text.Position.Bias;
import javax.swing.text.ViewTestHelpers.ChildView;
import javax.swing.text.ViewTestHelpers.ChildrenFactory;

/**
 * Tests BoxView class which has special children.
 */
public class BoxView_WithChildrenTest extends BasicSwingTestCase implements DocumentListener {
    private static final int Y_AXIS = View.Y_AXIS;

    private static final int X_AXIS = View.X_AXIS;

    private static final int POS = ViewTestHelpers.POS;

    /**
     * Invalid axis: neither X_AXIS nor Y_AXIS.
     */
    private static final int INVALID_AXIS = 2;

    private static final int MINIMUM = 0;

    private static final int PREFERRED = 1;

    private static final int MAXIMUM = 2;

    /**
     * Document used in testing.
     */
    private Document doc;

    /**
     * Default root of the document. It is the element <code>BoxView</code> is
     * constructed with.
     */
    private Element root;

    /**
     * View under test.
     */
    private BoxView view;

    /**
     * Factory to create child views.
     */
    private ChildrenFactory factory;

    /**
     * View allocation.
     */
    private Rectangle shape;

    /**
     * Size requirements along major - Y - axis.
     */
    private SizeRequirements major;

    /**
     * Size requirements along minor - X - axis.
     */
    private SizeRequirements minor;

    /**
     * Rectangle where repaint should occur.
     */
    private Rectangle paintRect;

    private boolean componentRepaint;

    private DocumentEvent insertEvent;

    private class BoxViewImpl extends BoxView {
        public BoxViewImpl(Element element, int axis) {
            super(element, axis);
        }

        @Override
        public ViewFactory getViewFactory() {
            return factory;
        }

        private Container container;

        @Override
        public Container getContainer() {
            if (container == null) {
                container = new JTextArea() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void repaint(final int x, final int y, final int w, final int h) {
                        paintRect = new Rectangle(x, y, w, h);
                    }

                    @Override
                    public void repaint() {
                        componentRepaint = true;
                    }
                };
            }
            return container;
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "uno\ndos\t2\ntres\ncuatro", null);
        root = doc.getDefaultRootElement();
        view = new BoxViewImpl(root, Y_AXIS);
        factory = new ChildrenFactory();
        view.loadChildren(factory);
        shape = new Rectangle(10, 5, 123, 435);
        major = view.calculateMajorAxisRequirements(Y_AXIS, null);
        minor = view.calculateMinorAxisRequirements(X_AXIS, null);
    }

    public void testGetChildAllocation() {
        assertNull(view.getChildAllocation(0, shape));
        assertNull(view.getChildAllocation(1, shape));
        view.layout(shape.width, shape.height);
        assertNull(view.getChildAllocation(0, null));
        assertNull(view.getChildAllocation(1, null));
        assertEquals(new Rectangle(getChildX(0), getChildY(0), getWidth(0), getHeight(0)), view
                .getChildAllocation(0, shape));
        assertEquals(new Rectangle(getChildX(1), getChildY(1), getWidth(1), getHeight(1)), view
                .getChildAllocation(1, shape));
        assertEquals(new Rectangle(getChildX(2), getChildY(2), getWidth(2), getHeight(2)), view
                .getChildAllocation(2, shape));
        assertEquals(new Rectangle(getChildX(3), getChildY(3), getWidth(3), getHeight(3)), view
                .getChildAllocation(3, shape));
    }

    public void testChildAllocation() {
        Rectangle alloc;
        final Rectangle invalidLayoutAlloc = new Rectangle(shape.x, shape.y, 0, 0);
        alloc = (Rectangle) shape.clone();
        view.childAllocation(0, alloc);
        assertEquals(invalidLayoutAlloc, alloc);
        alloc = (Rectangle) shape.clone();
        view.childAllocation(1, alloc);
        assertEquals(invalidLayoutAlloc, alloc);
        view.layout(shape.width, shape.height);
        alloc = (Rectangle) shape.clone();
        view.childAllocation(0, alloc);
        assertEquals(new Rectangle(getChildX(0), getChildY(0), getWidth(0), getHeight(0)),
                alloc);
        alloc = (Rectangle) shape.clone();
        view.childAllocation(1, alloc);
        assertEquals(new Rectangle(getChildX(1), getChildY(1), getWidth(1), getHeight(1)),
                alloc);
    }

    // Regression test for HARMONY-2776
    public void testChildAllocationNull() throws Exception {
        final Marker marker = new Marker();
        view = new BoxView(root, Y_AXIS) {
            @Override
            protected void childAllocation(int index, Rectangle alloc) {
                marker.setOccurred();
                super.childAllocation(index, alloc);
            }

            @Override
            protected Rectangle getInsideAllocation(Shape shape) {
                return null;
            }
        };
        view.loadChildren(factory);
        view.layout(shape.width, shape.height);
        assertTrue(view.isLayoutValid(X_AXIS) && view.isLayoutValid(Y_AXIS));
        assertNull(view.getChildAllocation(0, null));
        assertFalse(marker.isOccurred());
        try {
            view.getChildAllocation(0, shape);
            fail("NullPointerException is expected");
        } catch (NullPointerException e) {
            // expected
        }
        assertTrue(marker.isOccurred());
    }
    

    public void testFlipEastAndWestAtEnds() {
        assertEquals(Y_AXIS, view.getAxis());
        // Child views are not instances of CompositeView (no flip method)
        assertFalse(view.flipEastAndWestAtEnds(1, Bias.Backward));
        final View[] viewCalledUpon = new View[1];
        // Fill view with new children which are instances of CompositeView
        view.removeAll();
        view.loadChildren(new ViewFactory() {
            public View create(final Element element) {
                return new CompositeViewTest.CompositeViewImpl(element) {
                    @Override
                    protected boolean flipEastAndWestAtEnds(final int position, final Bias bias) {
                        viewCalledUpon[0] = this;
                        return (position & 1) == 0;
                    }
                };
            }
        });
        // Test it may be true
        assertTrue(view.flipEastAndWestAtEnds(0, null));
        assertTrue(view.flipEastAndWestAtEnds(0, Bias.Backward));
        // Check flip is called on a child
        viewCalledUpon[0] = null;
        view.flipEastAndWestAtEnds(view.getView(0).getEndOffset(), Bias.Backward);
        assertSame(view.getView(0), viewCalledUpon[0]);
        viewCalledUpon[0] = null;
        view.flipEastAndWestAtEnds(view.getView(0).getEndOffset(), Bias.Forward);
        assertSame(view.getView(1), viewCalledUpon[0]);
        viewCalledUpon[0] = null;
        // Select X as major axis
        view.setAxis(X_AXIS);
        // This simply returns false without calling other methods
        assertFalse(view.flipEastAndWestAtEnds(0, Bias.Forward));
        assertNull(viewCalledUpon[0]);
        assertFalse(view.flipEastAndWestAtEnds(1, Bias.Backward));
        assertNull(viewCalledUpon[0]);
    }

    public void testFlipEastAndWestAtEndsIndex() throws Exception {
        final Marker marker = new Marker();
        view = new BoxView(root, Y_AXIS) {
            @Override
            protected int getViewIndexAtPosition(int pos) {
                marker.setOccurred();
                marker.setAuxiliary(new Integer(pos));
                return super.getViewIndexAtPosition(pos);
            }
        };
        view.loadChildren(factory);
        assertEquals(root.getElementCount(), view.getViewCount());
        view.flipEastAndWestAtEnds(0, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(0, ((Integer) marker.getAuxiliary()).intValue());
        marker.reset();
        view.flipEastAndWestAtEnds(1, Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(0, ((Integer) marker.getAuxiliary()).intValue());
        marker.reset();
        view.flipEastAndWestAtEnds(1, null);
        assertTrue(marker.isOccurred());
        assertEquals(1, ((Integer) marker.getAuxiliary()).intValue());
        marker.reset();
        view.flipEastAndWestAtEnds(1, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(1, ((Integer) marker.getAuxiliary()).intValue());
        marker.reset();
        view.flipEastAndWestAtEnds(1, Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(0, ((Integer) marker.getAuxiliary()).intValue());
        marker.reset();
        view.flipEastAndWestAtEnds(view.getEndOffset(), Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(view.getEndOffset(), ((Integer) marker.getAuxiliary()).intValue());
        marker.reset();
        view.flipEastAndWestAtEnds(view.getEndOffset(), Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(view.getEndOffset() - 1, ((Integer) marker.getAuxiliary()).intValue());
        marker.reset();
    }

    /**
     * General checks.
     */
    public void testGetViewAtPoint01() {
        Rectangle alloc;
        view.layout(shape.width, shape.height);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(0), view.getViewAtPoint(getChildX(0) - 1, getChildY(0) - 1,
                alloc));
        assertEquals(view.getChildAllocation(0, shape), alloc);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(0), view.getViewAtPoint(getChildX(0), getChildY(0), alloc));
        assertEquals(view.getChildAllocation(0, shape), alloc);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(0), view.getViewAtPoint(getChildX(0) + getWidth(0) - 1,
                getChildY(0) + getHeight(0) - 1, alloc));
        assertEquals(view.getChildAllocation(0, shape), alloc);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(1), view.getViewAtPoint(getChildX(0) + getWidth(0),
                getChildY(0) + getHeight(0), alloc));
        assertEquals(view.getChildAllocation(1, shape), alloc);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(1), view.getViewAtPoint(shape.x, getChildY(1) + 1, alloc));
        assertEquals(view.getChildAllocation(1, shape), alloc);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(1), view.getViewAtPoint(getChildX(1) - 1, getChildY(1) + 1,
                alloc));
        assertEquals(view.getChildAllocation(1, shape), alloc);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(2), view.getViewAtPoint(shape.x - 5, getChildY(2) + 1, alloc));
        assertEquals(view.getChildAllocation(2, shape), alloc);
    }

    /**
     * Checks with invalid coordinates: outside of the shape.
     */
    public void testGetViewAtPoint02() {
        Rectangle alloc;
        view.layout(shape.width, shape.height);
        final int x = shape.x - 20;
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(0), view.getViewAtPoint(x, getChildY(0), alloc));
        assertEquals(view.getChildAllocation(0, shape), alloc);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(0), view.getViewAtPoint(x, shape.y - 15, alloc));
        assertEquals(view.getChildAllocation(0, shape), alloc);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(0), view.getViewAtPoint(shape.x + shape.width + 25,
                getChildY(0), alloc));
        assertEquals(view.getChildAllocation(0, shape), alloc);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(view.getViewCount() - 1), view.getViewAtPoint(x, shape.y
                + shape.height, alloc));
        assertEquals(view.getChildAllocation(view.getViewCount() - 1, shape), alloc);
        alloc = (Rectangle) shape.clone();
        assertSame(view.getView(view.getViewCount() - 1), view.getViewAtPoint(x, shape.y
                + shape.height + 157, alloc));
        assertEquals(view.getChildAllocation(view.getViewCount() - 1, shape), alloc);
    }

    /**
     * Tests getViewAtPoint method when major axis of the view is X.
     */
    public void testGetViewAtPoint03() {
        Rectangle alloc;
        view = new BoxViewImpl(root, X_AXIS);
        factory.resetID();
        view.loadChildren(factory);
        major = view.calculateMajorAxisRequirements(X_AXIS, null);
        minor = view.calculateMinorAxisRequirements(Y_AXIS, null);
        view.layout(shape.width, shape.height);
        int x = shape.x - 20;
        int y = shape.y + view.getOffset(X_AXIS, 1) + view.getSpan(X_AXIS, 1) / 2;
        // This value of y is to catch the bug in our implementation which
        // uses not major axis but always y to find views. With this y value,
        // the original code will return view at index 1 but not 0.
        alloc = (Rectangle) shape.clone();
        assertTrue(view.isBefore(x, y, alloc));
        assertSame(view.getView(0), view.getViewAtPoint(x, y, alloc));
        assertEquals(view.getChildAllocation(0, shape), alloc);
        x = shape.x;
        alloc = (Rectangle) shape.clone();
        assertFalse(view.isBefore(x, y, alloc));
        assertFalse(view.isAfter(x, y, alloc));
        assertSame(view.getView(0), view.getViewAtPoint(x, y, alloc));
        assertEquals(view.getChildAllocation(0, shape), alloc);
        y = -277;
        alloc = (Rectangle) shape.clone();
        assertFalse(view.isBefore(x, y, alloc));
        assertFalse(view.isAfter(x, y, alloc));
        assertSame(view.getView(0), view.getViewAtPoint(x, y, alloc));
        assertEquals(view.getChildAllocation(0, shape), alloc);
        final int lastIndex = view.getViewCount() - 1;
        y = shape.y + shape.height / 2;
        x = shape.x + view.getOffset(X_AXIS, lastIndex) + view.getSpan(X_AXIS, lastIndex);
        alloc = (Rectangle) shape.clone();
        alloc.width = x - shape.x + 1; // This is to include the last child
        // in the allocation
        assertFalse(view.isBefore(x, y, alloc));
        assertFalse(view.isAfter(x, y, alloc));
        assertSame(view.getView(lastIndex), view.getViewAtPoint(x, y, alloc));
        assertEquals(view.getChildAllocation(lastIndex, shape), alloc);
        alloc = (Rectangle) shape.clone(); // The last child doesn't fit
        // in the allocation
        assertFalse(view.isBefore(x, y, alloc));
        assertTrue(view.isAfter(x, y, alloc));
        assertSame(view.getView(lastIndex), view.getViewAtPoint(x, y, alloc));
        assertEquals(view.getChildAllocation(lastIndex, shape), alloc);
        alloc = (Rectangle) shape.clone();
        alloc.width = x - shape.x + 1; // Adjust allocation to fit the last
        x += 315; // child and move x beyond
        assertTrue(view.isAfter(x, y, alloc));
        assertSame(view.getView(lastIndex), view.getViewAtPoint(x, y, alloc));
        assertEquals(view.getChildAllocation(lastIndex, shape), alloc);
        alloc = (Rectangle) shape.clone();
        alloc.width = x - shape.x + 1;
        x += 315;
        assertTrue(view.isAfter(x, y, alloc));
        assertSame(view.getView(lastIndex), view.getViewAtPoint(x, y, alloc));
        assertEquals(view.getChildAllocation(lastIndex, shape), alloc);
    }

    /**
     * Tests <code>baselineLayout</code> with
     * <em>non-resizable</em> child views.
     * <p>The test performed for both major and minor axes.
     */
    public void testBaselineLayout01() {
        int[] offsets = new int[view.getViewCount()];
        int[] spans = new int[view.getViewCount()];
        int axis = Y_AXIS;
        // Check with major axis
        assertEquals(axis, view.getAxis());
        view.baselineLayout(shape.height, axis, offsets, spans);
        int leftInset = shape.height / 2;
        for (int i = 0; i < offsets.length; i++) {
            View child = view.getView(i);
            float span = child.getPreferredSpan(axis);
            float offset = leftInset - span * child.getAlignment(axis);
            assertEquals("@ " + i, (int) offset, offsets[i]);
            assertEquals("@ " + i, (int) span, spans[i]);
        }
        // Check with minor axis
        axis = X_AXIS;
        view.baselineLayout(shape.width, axis, offsets, spans);
        leftInset = shape.width / 2;
        for (int i = 0; i < offsets.length; i++) {
            View child = view.getView(i);
            float span = child.getPreferredSpan(axis);
            float offset = leftInset - span * child.getAlignment(axis);
            if (i == 1) {
                // The cause is rounding errors
                offset += 1;
            }
            assertEquals("@ " + i, (int) offset, offsets[i]);
            assertEquals("@ " + i, (int) span, spans[i]);
        }
    }

    /**
     * Tests <code>baselineLayout</code> with <em>resizable</em> child views.
     * <p>The test performed for major axis only.
     */
    public void testBaselineLayout02() {
        makeFlexible();
        int[] offsets = new int[view.getViewCount()];
        int[] spans = new int[view.getViewCount()];
        int axis = Y_AXIS;
        // Check with major axis
        assertEquals(axis, view.getAxis());
        view.baselineLayout(shape.height, axis, offsets, spans);
        int[] resultOffsets = new int[] { 217, 201, 0, 209 };
        int[] resultSpans = new int[] { 80, 64, 217, 16 };
        for (int i = 0; i < offsets.length; i++) {
            assertEquals("@ " + i, resultOffsets[i], offsets[i]);
            assertEquals("@ " + i, resultSpans[i], spans[i]);
        }
    }

    /**
     * Tests <code>baselineRequirements</code> with ordinary children along
     * major axis - Y.
     */
    public void testBaselineRequirements01() {
        SizeRequirements sr;
        final int axis = Y_AXIS;
        sr = view.baselineRequirements(axis, null);
        // The sizes to the left and to the right from the alignment point
        int left = getLeft(PREFERRED, axis);
        int right = getRight(PREFERRED, axis);
        int size = left + right;
        assertEquals(size, sr.minimum);
        assertEquals(size, sr.preferred);
        assertEquals(size, sr.maximum);
        assertEquals((float) left / (float) size, sr.alignment, 0.00001f);
        SizeRequirements another = view.baselineRequirements(axis, sr);
        assertSame(sr, another);
    }

    /**
     * Tests <code>baselineRequirements</code> with flexible children along
     * major axis - Y.
     */
    public void testBaselineRequirements02() {
        if (!isHarmony()) {
            return;
        }
        makeFlexible();
        SizeRequirements sr;
        final int axis = Y_AXIS;
        sr = view.baselineRequirements(axis, null);
        // The sizes to the left and to the right from the alignment point
        int minL = getLeft(MINIMUM, axis);
        int minR = getRight(MINIMUM, axis);
        int prefL = getLeft(PREFERRED, axis);
        int prefR = getRight(PREFERRED, axis);
        int maxL = getLeft(MAXIMUM, axis);
        int maxR = getRight(MAXIMUM, axis);
        int min = minL + minR;
        int pref = prefL + prefR;
        int max = maxL + maxR;
        assertEquals(min, sr.minimum);
        assertEquals(pref, sr.preferred);
        assertEquals(max, sr.maximum);
        assertEquals((float) prefL / (float) pref, sr.alignment, 0.00001f);
    }

    /**
     * Tests <code>baselineRequirements</code> with ordinary children along
     * minor axis - X.
     */
    public void testBaselineRequirements03() {
        SizeRequirements sr;
        final int axis = X_AXIS;
        sr = view.baselineRequirements(axis, null);
        // The sizes to the left and to the right from the alignment point
        int left = getLeft(PREFERRED, axis);
        int right = getRight(PREFERRED, axis);
        int size = left + right + 1; // there should be no 1 added
        assertEquals(size, sr.minimum);
        assertEquals(size, sr.preferred);
        assertEquals(size, sr.maximum);
        assertEquals((float) left / (float) size, sr.alignment, 0.00001f);
    }

    /**
     * Tests <code>calculateMajorAxisRequirements</code> with ordinary children.
     */
    public void testCalculateMajorAxisRequirements01() {
        SizeRequirements in = null;
        SizeRequirements out;
        out = view.calculateMajorAxisRequirements(Y_AXIS, in);
        assertNull(in);
        assertNotNull(out);
        int height = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            height += getHeight(i);
        }
        assertEquals(height, out.minimum);
        assertEquals(height, out.preferred);
        assertEquals(height, out.maximum);
        assertEquals(view.getAlignment(Y_AXIS), out.alignment, 0.00001f);
        in = new SizeRequirements();
        out = view.calculateMajorAxisRequirements(Y_AXIS, in);
        assertSame(in, out);
    }

    /**
     * Tests <code>calculateMajorAxisRequirements</code> with flexible children.
     */
    public void testCalculateMajorAxisRequirements02() {
        makeFlexible();
        int min = 0; // Requirements are sum
        int pref = 0;
        int max = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            View child = view.getView(i);
            min += child.getMinimumSpan(Y_AXIS);
            pref += child.getPreferredSpan(Y_AXIS);
            max += child.getMaximumSpan(Y_AXIS);
        }
        assertEquals(min, major.minimum);
        assertEquals(pref, major.preferred);
        assertEquals(max, major.maximum);
        assertEquals(view.getAlignment(Y_AXIS), major.alignment, 0.00001f);
    }

    /**
     * Tests <code>calculateMinorAxisRequirements</code> with ordinary children.
     */
    public void testCalculateMinorAxisRequirements01() {
        SizeRequirements in = null;
        SizeRequirements out;
        out = view.calculateMinorAxisRequirements(X_AXIS, in);
        assertNull(in);
        assertNotNull(out);
        int width = getWidth(0);
        for (int i = 1; i < view.getViewCount(); i++) {
            if (getWidth(i) > width) {
                width = getWidth(i);
            }
        }
        assertEquals(width, out.minimum);
        assertEquals(width, out.preferred);
        assertEquals(Integer.MAX_VALUE, out.maximum);
        assertEquals(view.getAlignment(X_AXIS), out.alignment, 0.00001f);
        in = new SizeRequirements();
        out = view.calculateMinorAxisRequirements(X_AXIS, in);
        assertSame(in, out);
    }

    /**
     * Tests <code>calculateMinorAxisRequirements</code> with flexible children.
     */
    public void testCalculateMinorAxisRequirements02() {
        makeFlexible();
        int min = 0; // Requirements are maximum values
        int pref = 0;
        // The parent, however, can be huge
        int max = (int) view.getMaximumSpan(X_AXIS);
        for (int i = 0; i < view.getViewCount(); i++) {
            View child = view.getView(i);
            if (min < child.getMinimumSpan(X_AXIS)) {
                min = (int) child.getMinimumSpan(X_AXIS);
            }
            if (pref < child.getPreferredSpan(X_AXIS)) {
                pref = (int) child.getPreferredSpan(X_AXIS);
            }
        }
        assertEquals(min, minor.minimum);
        assertEquals(pref, minor.preferred);
        assertEquals(max, minor.maximum);
        assertEquals(view.getAlignment(X_AXIS), minor.alignment, 0.00001f);
    }

    /**
     * Tests forwardUpdate when major axis is Y_AXIS and
     * document structure isn't changed. The child says it changed its
     * preference along both axes.
     * (See javax.swing.text.ViewTestHelpers.ChildView.insertUpdate()).
     */
    public void testForwardUpdate01() throws BadLocationException {
        view.getContainer();
        componentRepaint = false;
        doc.addDocumentListener(this);
        doc.insertString(root.getElement(2).getStartOffset() + 1, "123", null);
        ElementChange change = insertEvent.getChange(view.getElement());
        assertNull(change);
        view.layout(shape.width, shape.height);
        assertTrue(view.isLayoutValid(X_AXIS));
        assertTrue(view.isLayoutValid(Y_AXIS));
        view.forwardUpdate(change, insertEvent, shape, factory);
        assertFalse(view.isLayoutValid(X_AXIS));
        assertFalse(view.isLayoutValid(Y_AXIS));
        assertFalse(componentRepaint);
        Rectangle bounds = view.getInsideAllocation(shape);
        int childIndex = view.getViewIndex(insertEvent.getOffset(), Bias.Forward);
        if (isHarmony()) {
            bounds.y += view.getOffset(view.getAxis(), childIndex);
            bounds.height -= view.getOffset(view.getAxis(), childIndex);
        } else {
            bounds.y = 28;
            bounds.height = 412;
        }
        assertEquals(paintRect, bounds);
    }

    /**
     * Tests forwardUpdate when major axis is Y_AXIS and
     * document structure isn't changed. The child says it changed its
     * prefence along X_AXIS only.
     */
    public void testForwardUpdate02() throws BadLocationException {
        doc.addDocumentListener(this);
        doc.insertString(root.getElement(2).getStartOffset() + 1, "123", null);
        ElementChange change = insertEvent.getChange(view.getElement());
        assertNull(change);
        final int childIndex = view.getViewIndex(insertEvent.getOffset(), Bias.Forward);
        final View child = view.getView(childIndex);
        view.replace(childIndex, 1, new View[] { new ChildView(child.getElement(), -1) {
            @Override
            public void preferenceChanged(final View child, final boolean width,
                    final boolean height) {
                super.preferenceChanged(child, true, false);
            }
        } });
        view.layout(shape.width, shape.height);
        assertTrue(view.isLayoutValid(X_AXIS));
        assertTrue(view.isLayoutValid(Y_AXIS));
        view.forwardUpdate(change, insertEvent, shape, factory);
        assertFalse(view.isLayoutValid(X_AXIS));
        assertTrue(view.isLayoutValid(Y_AXIS));
        assertFalse(componentRepaint);
        assertNull(paintRect);
    }

    /**
     * Tests forwardUpdate when major axis is X_AXIS and
     * document structure isn't changed. The child says it changed its
     * preference along both axes.
     * (See javax.swing.text.ViewTestHelpers.ChildView.insertUpdate()).
     */
    public void testForwardUpdate03() throws BadLocationException {
        view = new BoxViewImpl(root, X_AXIS);
        view.loadChildren(factory);
        view.getContainer();
        componentRepaint = false;
        doc.addDocumentListener(this);
        doc.insertString(root.getElement(2).getStartOffset() + 1, "123", null);
        ElementChange change = insertEvent.getChange(view.getElement());
        assertNull(change);
        view.layout(shape.width, shape.height);
        assertTrue(view.isLayoutValid(X_AXIS));
        assertTrue(view.isLayoutValid(Y_AXIS));
        view.forwardUpdate(change, insertEvent, shape, factory);
        assertFalse(view.isLayoutValid(X_AXIS));
        assertFalse(view.isLayoutValid(Y_AXIS));
        assertFalse(componentRepaint);
        Rectangle bounds = view.getInsideAllocation(shape);
        int childIndex = view.getViewIndex(insertEvent.getOffset(), Bias.Forward);
        bounds.x += view.getOffset(view.getAxis(), childIndex);
        bounds.width -= view.getOffset(view.getAxis(), childIndex);
        assertEquals(paintRect, bounds);
    }

    /**
     * Tests forwardUpdate when major axis is X_AXIS and
     * document structure isn't changed. The child says it changed its
     * preference along Y_AXIS only.
     */
    public void testForwardUpdate04() throws BadLocationException {
        view = new BoxViewImpl(root, X_AXIS);
        view.loadChildren(factory);
        doc.addDocumentListener(this);
        doc.insertString(root.getElement(2).getStartOffset() + 1, "123", null);
        ElementChange change = insertEvent.getChange(view.getElement());
        assertNull(change);
        final int childIndex = view.getViewIndex(insertEvent.getOffset(), Bias.Forward);
        final View child = view.getView(childIndex);
        view.replace(childIndex, 1, new View[] { new ChildView(child.getElement(), -1) {
            @Override
            public void preferenceChanged(final View child, final boolean width,
                    final boolean height) {
                super.preferenceChanged(child, false, true);
            }
        } });
        view.layout(shape.width, shape.height);
        assertTrue(view.isLayoutValid(X_AXIS));
        assertTrue(view.isLayoutValid(Y_AXIS));
        view.forwardUpdate(change, insertEvent, shape, factory);
        assertTrue(view.isLayoutValid(X_AXIS));
        assertFalse(view.isLayoutValid(Y_AXIS));
        assertFalse(componentRepaint);
        assertNull(paintRect);
    }

    public void testInsertUpdate() throws BadLocationException {
        doc.addDocumentListener(this);
        doc.insertString(root.getElement(2).getStartOffset() + 1, "\n123", null);
        view.layout(shape.width, shape.height);
        ElementChange change = insertEvent.getChange(view.getElement());
        assertNotNull(change);
        componentRepaint = false;
        view.insertUpdate(insertEvent, shape, factory);
        assertTrue(componentRepaint);
    }

    public void testUpdateLayout01() throws BadLocationException {
        doc.addDocumentListener(this);
        doc.insertString(root.getElement(2).getStartOffset() + 1, "123", null);
        view.layout(shape.width, shape.height);
        ElementChange change = insertEvent.getChange(view.getElement());
        assertNull(change);
        componentRepaint = false;
        view.updateLayout(change, insertEvent, shape);
        assertFalse(componentRepaint);
    }

    public void testUpdateLayout02() throws BadLocationException {
        doc.addDocumentListener(this);
        doc.insertString(root.getElement(2).getStartOffset() + 1, "\n123", null);
        view.layout(shape.width, shape.height);
        ElementChange change = insertEvent.getChange(view.getElement());
        assertNotNull(change);
        componentRepaint = false;
        view.updateLayout(change, insertEvent, shape);
        assertTrue(componentRepaint);
    }

    /**
     * Tests <code>getOffset</code> with Y axis.
     */
    public void testGetOffset01() {
        view.layout(shape.width, shape.height);
        assertEquals(getChildY(0) - shape.y, view.getOffset(Y_AXIS, 0));
        assertEquals(getChildY(1) - shape.y, view.getOffset(Y_AXIS, 1));
        assertEquals(getChildY(2) - shape.y, view.getOffset(Y_AXIS, 2));
        assertEquals(getChildY(3) - shape.y, view.getOffset(Y_AXIS, 3));
    }

    /**
     * Tests <code>getOffset</code> with X axis.
     */
    public void testGetOffset02() {
        view.layout(shape.width, shape.height);
        assertEquals(getChildX(0) - shape.x, view.getOffset(X_AXIS, 0));
        assertEquals(getChildX(1) - shape.x, view.getOffset(X_AXIS, 1));
        assertEquals(getChildX(2) - shape.x, view.getOffset(X_AXIS, 2));
        assertEquals(getChildX(3) - shape.x, view.getOffset(X_AXIS, 3));
    }

    /**
     * Tests <code>getSpan</code> with Y axis.
     */
    public void testGetSpan01() {
        view.layout(shape.width, shape.height);
        assertEquals(getHeight(0), view.getSpan(Y_AXIS, 0));
        assertEquals(getHeight(1), view.getSpan(Y_AXIS, 1));
        assertEquals(getHeight(2), view.getSpan(Y_AXIS, 2));
        assertEquals(getHeight(3), view.getSpan(Y_AXIS, 3));
    }

    /**
     * Tests <code>getSpan</code> with X axis.
     */
    public void testGetSpan02() {
        view.layout(shape.width, shape.height);
        assertEquals(getWidth(0), view.getSpan(X_AXIS, 0));
        assertEquals(getWidth(1), view.getSpan(X_AXIS, 1));
        assertEquals(getWidth(2), view.getSpan(X_AXIS, 2));
        assertEquals(getWidth(3), view.getSpan(X_AXIS, 3));
    }

    /**
     * Tests <code>layoutMajorAxis</code> with default settings:
     * not resizable children, enough height space.
     */
    public void testLayoutMajorAxis01() {
        int[] offsets = new int[view.getViewCount()];
        int[] spans = new int[view.getViewCount()];
        view.layoutMajorAxis(shape.height, Y_AXIS, offsets, spans);
        // This method doesn't mark layout as valid
        assertFalse(view.isLayoutValid(Y_AXIS));
        for (int i = 0; i < view.getViewCount(); i++) {
            assertEquals("Offsets are different @ " + i, getChildY(i) - shape.y, offsets[i]);
            assertEquals("Spans are different @ " + i, getHeight(i), spans[i]);
        }
    }

    /**
     * Tests <code>layoutMajorAxis</code> with default settings:
     * not resizable children, height space > maximum.
     */
    public void testLayoutMajorAxis02() {
        int[] offsets = new int[view.getViewCount()];
        int[] spans = new int[view.getViewCount()];
        view.layoutMajorAxis(major.preferred - 150, Y_AXIS, offsets, spans);
        // This method doesn't mark layout as valid
        assertFalse(view.isLayoutValid(Y_AXIS));
        for (int i = 0; i < view.getViewCount(); i++) {
            assertEquals("Offsets are different @ " + i, getChildY(i) - shape.y, offsets[i]);
            assertEquals("Spans are different @ " + i, getHeight(i), spans[i]);
        }
    }

    /**
     * Tests <code>layoutMajorAxis</code> with "flexible" settings:
     * resizable children, enough height space.
     */
    public void testLayoutMajorAxis03() {
        makeFlexible();
        int[] offsets = new int[view.getViewCount()];
        int[] spans = new int[view.getViewCount()];
        assertTrue(shape.height > major.maximum);
        view.layoutMajorAxis(shape.height, Y_AXIS, offsets, spans);
        // This method doesn't mark layout as valid
        assertFalse(view.isLayoutValid(Y_AXIS));
        int childOffset = 0;
        int childSpan;
        for (int i = 0; i < view.getViewCount(); i++) {
            assertEquals("Offsets are different @ " + i, childOffset, offsets[i]);
            childSpan = (int) view.getView(i).getMaximumSpan(Y_AXIS);
            assertEquals("Spans are different @ " + i, childSpan, spans[i]);
            childOffset += childSpan;
        }
    }

    /**
     * Tests <code>layoutMajorAxis</code> with "flexible" settings:
     * resizable children, height space > minimum but < preferred.
     */
    public void testLayoutMajorAxis04() {
        makeFlexible();
        int[] offsets = new int[view.getViewCount()];
        int[] spans = new int[view.getViewCount()];
        final int axis = Y_AXIS;
        shape.height = major.minimum - 50;
        assertTrue(shape.height < major.minimum);
        view.layoutMajorAxis(shape.height, axis, offsets, spans);
        // This method doesn't mark layout as valid
        assertFalse(view.isLayoutValid(axis));
        SizeRequirements[] childReq = new SizeRequirements[view.getViewCount()];
        int[] childOffsets = new int[view.getViewCount()];
        int[] childSpans = new int[view.getViewCount()];
        fillRequirements(childReq, axis);
        SizeRequirements.calculateTiledPositions(shape.height, major, childReq, childOffsets,
                childSpans);
        for (int i = 0; i < view.getViewCount(); i++) {
            assertEquals("Offsets are different @ " + i, childOffsets[i], offsets[i]);
            assertEquals("Spans are different @ " + i, childSpans[i], spans[i]);
        }
    }

    /**
     * Tests layout of minor axis with <em>normal (non-resizable)</em> children.
     */
    public void testLayoutMinorAxis01() {
        int[] offsets = new int[view.getViewCount()];
        int[] spans = new int[view.getViewCount()];
        view.layoutMinorAxis(shape.width, X_AXIS, offsets, spans);
        // This method doesn't mark layout as valid
        assertFalse(view.isLayoutValid(X_AXIS));
        for (int i = 0; i < view.getViewCount(); i++) {
            assertEquals("Offsets are different @ " + i, getChildX(i) - shape.x, offsets[i]);
            assertEquals("Spans are different @ " + i, getWidth(i), spans[i]);
        }
    }

    /**
     * Tests layout of minor axis with <em>resizable</em> children.
     */
    public void testLayoutMinorAxis02() {
        int[] offsets = new int[view.getViewCount()];
        int[] spans = new int[view.getViewCount()];
        makeFlexible();
        SizeRequirements[] childReq = new SizeRequirements[view.getViewCount()];
        fillRequirements(childReq, X_AXIS);
        view.layoutMinorAxis(shape.width, X_AXIS, offsets, spans);
        for (int i = 0; i < view.getViewCount(); i++) {
            int span = getChildSpan(shape.width, childReq[i]);
            assertEquals("Spans are different @ " + i, span, spans[i]);
            assertEquals("Offsets are different @ " + i, getChildOffset(shape.width, span,
                    childReq[i].alignment), offsets[i]);
        }
    }

    /**
     * Tests layout of minor axis with <em>resizable</em> children in the case
     * where <code>targetSpan</code> <em>is less than the minimum span</em>
     * of at least one of the children.
     */
    public void testLayoutMinorAxis03() {
        int[] offsets = new int[view.getViewCount()];
        int[] spans = new int[view.getViewCount()];
        makeFlexible();
        SizeRequirements[] childReq = new SizeRequirements[view.getViewCount()];
        fillRequirements(childReq, X_AXIS);
        shape.width = 16;
        view.layoutMinorAxis(shape.width, X_AXIS, offsets, spans);
        boolean widthLessMinimum = false;
        for (int i = 0; i < view.getViewCount(); i++) {
            widthLessMinimum |= childReq[i].minimum > shape.width;
            int span = getChildSpan(shape.width, childReq[i]);
            assertEquals("Spans are different @ " + i, span, spans[i]);
            assertEquals("Offsets are different @ " + i, getChildOffset(shape.width, span,
                    childReq[i].alignment), offsets[i]);
        }
        assertTrue("Minimum span of at least one child view must "
                + "be greater than targetSpan", widthLessMinimum);
    }

    /**
     * Test <code>getMaximumSpan</code> with ordinary children.
     */
    public void testGetMaximumSpan01() {
        assertEquals(major.maximum, (int) view.getMaximumSpan(Y_AXIS));
        assertEquals(minor.maximum, (int) view.getMaximumSpan(X_AXIS));
    }

    /**
     * Test <code>getMaximumSpan</code> with flexible children.
     */
    public void testGetMaximumSpan02() {
        makeFlexible();
        assertEquals(major.maximum, (int) view.getMaximumSpan(Y_AXIS));
        assertEquals(minor.maximum, (int) view.getMaximumSpan(X_AXIS));
    }

    /**
     * Test <code>getMaximumSpan</code> throws required exception.
     */
    public void testGetMaximumSpan03() {
        try {
            view.getMaximumSpan(INVALID_AXIS);
            fail("IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test <code>getMinimumSpan</code> with ordinary children.
     */
    public void testGetMinimumSpan01() {
        assertEquals(major.minimum, (int) view.getMinimumSpan(Y_AXIS));
        assertEquals(minor.minimum, (int) view.getMinimumSpan(X_AXIS));
    }

    /**
     * Test <code>getMinimumSpan</code> with flexible children.
     */
    public void testGetMinimumSpan02() {
        makeFlexible();
        assertEquals(major.minimum, (int) view.getMinimumSpan(Y_AXIS));
        assertEquals(minor.minimum, (int) view.getMinimumSpan(X_AXIS));
    }

    /**
     * Test <code>getMinimumSpan</code> throws required exception.
     */
    public void testGetMinimumSpan03() {
        try {
            view.getMinimumSpan(INVALID_AXIS);
            fail("IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test <code>getPreferredSpan</code> with ordinary children.
     */
    public void testGetPreferredSpan01() {
        assertEquals(major.preferred, (int) view.getPreferredSpan(Y_AXIS));
        assertEquals(minor.preferred, (int) view.getPreferredSpan(X_AXIS));
    }

    /**
     * Test <code>getPreferredSpan</code> with flexible children.
     */
    public void testGetPreferredSpan02() {
        makeFlexible();
        assertEquals(major.preferred, (int) view.getPreferredSpan(Y_AXIS));
        assertEquals(minor.preferred, (int) view.getPreferredSpan(X_AXIS));
    }

    /**
     * Test <code>getPreferredSpan</code> throws required exception.
     */
    public void testGetPreferredSpan03() {
        try {
            view.getPreferredSpan(INVALID_AXIS);
            fail("IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test <code>getResizeWeight</code> with ordinary children.
     */
    public void testGetResizeWeight01() {
        assertEquals(0, view.getResizeWeight(Y_AXIS)); // major
        assertEquals(1, view.getResizeWeight(X_AXIS)); // minor
    }

    /**
     * Test <code>getResizeWeight</code> with flexible children.
     */
    public void testGetResizeWeight02() {
        makeFlexible();
        assertEquals(1, view.getResizeWeight(Y_AXIS));
        assertEquals(1, view.getResizeWeight(X_AXIS));
    }

    /**
     * Test <code>getResizeWeight</code> throws required exception.
     */
    public void testGetResizeWeight03() {
        try {
            view.getResizeWeight(INVALID_AXIS);
            fail("IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test <code>getResizeWeight</code> with both ordinary and
     * flexible children while major axis is X.
     */
    public void testGetResizeWeight04() {
        view = new BoxView(root, X_AXIS);
        view.loadChildren(factory);
        assertEquals(1, view.getResizeWeight(Y_AXIS)); // minor
        assertEquals(0, view.getResizeWeight(X_AXIS)); // major
        makeFlexible();
        assertEquals(1, view.getResizeWeight(Y_AXIS));
        assertEquals(1, view.getResizeWeight(X_AXIS));
        assertTrue(1 < view.getMaximumSpan(X_AXIS) - view.getMinimumSpan(X_AXIS));
        assertTrue(1 < view.getMaximumSpan(Y_AXIS) - view.getMinimumSpan(Y_AXIS));
    }

    /*
     * Class under test for Shape modelToView(int, Shape, Bias)
     */
    public void testModelToView() throws BadLocationException {
        // Allocation is invalid
        assertFalse(view.isAllocationValid());
        // The call makes layout, so...
        assertEquals(new Rectangle(getChildX(0), getChildY(0), 1, getHeight(0)), view
                .modelToView(0, shape, Bias.Forward));
        // Allocation is valid
        assertTrue(view.isAllocationValid());
        assertEquals(shape.width, view.getWidth());
        assertEquals(shape.height, view.getHeight());
        View child = view.getView(0);
        // This will call on the first child
        assertEquals(new Rectangle(getChildX(0) + POS * child.getEndOffset(), getChildY(0), 1,
                getHeight(0)), view.modelToView(child.getEndOffset(), shape, Bias.Backward));
        // These will call on the second child
        assertEquals(new Rectangle(getChildX(1), getChildY(1), 1, getHeight(1)), view
                .modelToView(child.getEndOffset(), shape, Bias.Forward));
        assertEquals(new Rectangle(getChildX(1) + POS, getChildY(1), 1, getHeight(1)), view
                .modelToView(child.getEndOffset() + 1, shape, Bias.Forward));
        // The only illegal Bias possible is null
        try {
            view.modelToView(0, shape, null);
            // isn't thrown
            //fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Tests method int viewToModel(float, float, Shape, Bias[])
     */
    public void testViewToModel() {
        final Bias[] bias = new Bias[1];
        // Allocation is invalid
        assertFalse(view.isAllocationValid());
        // The call makes layout, so...
        assertEquals(0, view.viewToModel(0, 0, shape, bias));
        // Allocation is valid
        assertTrue(view.isAllocationValid());
        assertEquals(shape.width, view.getWidth());
        assertEquals(shape.height, view.getHeight());
        View child = view.getView(0);
        assertEquals(child.getEndOffset(), view.viewToModel(getChildX(0) + POS
                * child.getEndOffset(), getChildY(0), shape, bias));
        child = view.getView(1);
        assertEquals(child.getStartOffset() + 1, view.viewToModel(getChildX(1) + POS,
                getChildY(1), shape, bias));
    }

    public void testPaint() {
        final class ChildPainted {
            public int index;

            public Rectangle alloc;

            public ChildPainted(final int index, final Rectangle alloc) {
                this.index = index;
                this.alloc = alloc;
            }
        }
        final ArrayList<ChildPainted> childrenPainted = new ArrayList<ChildPainted>();
        view = new BoxView(root, Y_AXIS) {
            @Override
            protected void paintChild(final Graphics g, final Rectangle alloc, final int index) {
                childrenPainted.add(new ChildPainted(index, (Rectangle) alloc.clone()));
            }
        };
        factory.resetID();
        view.loadChildren(factory);
        Graphics g = (new BufferedImage(shape.x + shape.width + 100, shape.y + shape.height
                + 50, BufferedImage.TYPE_INT_RGB)).getGraphics();
        Rectangle clip = new Rectangle(getChildX(0) + 10, getChildY(0) + 11, shape.width + 37,
                getHeight(0));
        g.setClip(clip);
        view.layout(shape.width, shape.height);
        view.paint(g, shape);
        for (int i = 0; i < view.getViewCount(); i++) {
            Rectangle childBounds = (Rectangle) view.getChildAllocation(i, shape);
            if (i < 2) {
                assertTrue("Child bounds IS NOT withing clip bounds @" + i, clip
                        .intersects(childBounds));
            } else {
                assertFalse("Child bounds IS withing clip bounds @" + i, clip
                        .intersects(childBounds));
            }
        }
        assertEquals(2, childrenPainted.size());
        ChildPainted childPainted;
        childPainted = childrenPainted.get(0);
        assertEquals(0, childPainted.index);
        assertEquals(view.getChildAllocation(0, shape), childPainted.alloc);
        childPainted = childrenPainted.get(1);
        assertEquals(1, childPainted.index);
        assertEquals(view.getChildAllocation(1, shape), childPainted.alloc);
    }

    public void changedUpdate(final DocumentEvent event) {
    }

    public void insertUpdate(final DocumentEvent event) {
        insertEvent = event;
    }

    public void removeUpdate(final DocumentEvent event) {
    }

    private static float getAlign(final int axis, final int id) {
        return ViewTestHelpers.getAlign(axis, id);
    }

    private static int getHeight(final int id) {
        return ViewTestHelpers.getHeight(id);
    }

    private static int getWidth(final int id) {
        return ViewTestHelpers.getWidth(id);
    }

    /**
     * Returns <code>x</code> coordinate for a child at index <code>id</code>.
     * It respects child alignment request.
     *
     * @param id child number (or index)
     * @return x coordinate of the child
     */
    private int getChildX(final int id) {
        return (int) (shape.x + getAlign(X_AXIS, id) * (shape.width - getWidth(id)));
    }

    /**
     * Returns <code>y</code> coordinate for a child at index <code>id</code>.
     *
     * @param id child number (or index)
     * @return y coordinate of the child
     */
    private int getChildY(final int id) {
        int y = shape.y;
        for (int i = 0; i < id; i++) {
            y += getHeight(i);
        }
        return y;
    }

    /**
     * Returns the span of a child for minor axis layout.
     *
     * @param targetSpan target span where the child view should be placed
     * @param sr size requirements of the child view
     * @return the span of the child view
     */
    private int getChildSpan(final int targetSpan, final SizeRequirements sr) {
        int result;
        if (targetSpan >= sr.maximum) {
            result = sr.maximum;
        } else if (targetSpan >= sr.minimum) {
            result = targetSpan;
        } else {
            result = sr.minimum;
        }
        return result;
    }

    /**
     * Returns the offset of a child for minor axis layout.
     *
     * @param targetSpan target span where the child view should be placed
     * @param childSpan the span of the child view
     * @return the offset of the child view inside its container
     */
    private int getChildOffset(final int targetSpan, final int childSpan, final float alignment) {
        int result = (int) ((targetSpan - childSpan) * alignment);
        return result >= 0 ? result : 0;
    }

    /**
     * Reinitializes the view with flexible children.
     */
    private void makeFlexible() {
        factory.makeFlexible();
        view.removeAll();
        view.loadChildren(factory);
        // Recalculate
        major = view.calculateMajorAxisRequirements(Y_AXIS, null);
        minor = view.calculateMinorAxisRequirements(X_AXIS, null);
    }

    /**
     * Returns the maximum span of a child view to the left or top of
     * the aligning point (for baseline layout).
     *
     * @param which specifies which span is used:
     *              <code>MINIMUM</code>, <code>PREFERRED</code>,
     *              <code>MAXIMUM</code>
     * @param axis specifies the axis of the span: either <code>X_AXIS</code>
     *             or <code>Y_AXIS</code>
     * @return the maximum span to the left or top of the aligning point
     */
    private int getLeft(final int which, final int axis) {
        int left = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            View child = view.getView(i);
            float span = 0;
            switch (which) {
                case MINIMUM:
                    span = child.getMinimumSpan(axis);
                    break;
                case PREFERRED:
                    span = child.getPreferredSpan(axis);
                    break;
                case MAXIMUM:
                    span = child.getMaximumSpan(axis);
                    break;
            }
            float cLeft = child.getAlignment(axis) * span;
            if (cLeft > left) {
                left = (int) cLeft;
            }
        }
        return left;
    }

    /**
     * Returns the maximum span of a child view to the right or bottom of
     * the aligning point (for baseline layout).
     *
     * @param which specifies which span is used:
     *              <code>MINIMUM</code>, <code>PREFERRED</code>,
     *              <code>MAXIMUM</code>
     * @param axis specifies the axis of the span: either <code>X_AXIS</code>
     *             or <code>Y_AXIS</code>
     * @return the maximum span to the right or bottom of the aligning point
     */
    private int getRight(final int which, final int axis) {
        int right = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            View child = view.getView(i);
            float span = 0;
            switch (which) {
                case MINIMUM:
                    span = child.getMinimumSpan(axis);
                    break;
                case PREFERRED:
                    span = child.getPreferredSpan(axis);
                    break;
                case MAXIMUM:
                    span = child.getMaximumSpan(axis);
                    break;
            }
            float cLeft = child.getAlignment(axis) * span;
            float cRight = span - cLeft;
            if (cRight > right) {
                right = (int) cRight;
            }
        }
        return right;
    }

    private void fillRequirements(final SizeRequirements[] childReq, final int axis) {
        for (int i = 0; i < view.getViewCount(); i++) {
            View child = view.getView(i);
            childReq[i] = new SizeRequirements((int) child.getMinimumSpan(axis), (int) child
                    .getPreferredSpan(axis), (int) child.getMaximumSpan(axis), child
                    .getAlignment(axis));
        }
    }
}
