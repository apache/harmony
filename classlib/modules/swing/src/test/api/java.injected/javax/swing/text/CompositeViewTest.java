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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.BasicSwingTestCase;
import javax.swing.SwingConstants;
import javax.swing.text.Position.Bias;

public class CompositeViewTest extends BasicSwingTestCase {
    private Document doc; // Document used in tests

    private Element root; // Default root element of the document

    private CompositeView view; // View object used in tests

    private ViewFactory factory; // View factory used to create new views

    private Shape shape; // View allocation (area to render into)

    private Bias[] bias; // Place for bias return

    private boolean childrenLoaded;

    private int modelPosition;

    /**
     * Controls implementation of isAfter(), isBefore() in CompositeViewImpl.
     */
    static boolean useBoth = true;

    static boolean useX;

    /**
     * Just implements all abstract methods of CompositeView.
     */
    protected static class CompositeViewImpl extends CompositeView {
        public CompositeViewImpl(final Element element) {
            super(element);
        }

        @Override
        protected void childAllocation(final int index, final Rectangle rc) {
        }

        @Override
        protected View getViewAtPoint(final int x, final int y, final Rectangle shape) {
            return null;
        }

        @Override
        protected boolean isAfter(final int x, final int y, final Rectangle rc) {
            boolean result;
            if (useBoth) {
                result = x > rc.x + rc.width || y > rc.y + rc.height;
            } else if (useX) {
                result = x > rc.x + rc.width;
            } else {
                result = y > rc.y + rc.height;
            }
            return result;
        }

        @Override
        protected boolean isBefore(final int x, final int y, final Rectangle rc) {
            boolean result;
            if (useBoth) {
                result = x < rc.x || y < rc.y;
            } else if (useX) {
                result = x < rc.x;
            } else {
                result = y < rc.y;
            }
            return result;
        }

        @Override
        public void paint(final Graphics g, final Shape shape) {
        }

        @Override
        public float getPreferredSpan(final int axis) {
            return 0;
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "line1\nline2\n\u05DC\u05DD\nline3\n", null);
        // positions:        012345 678901 2     3     4 567890
        //                   0          1                     2
        view = new CompositeViewImpl(root = doc.getDefaultRootElement());
        view.loadChildren(factory = new ViewFactory() {
            public View create(final Element line) {
                return new PlainView(line);
            }
        });
        shape = new Rectangle(100, 200);
        bias = new Position.Bias[1];
    }

    public void testGetChildAllocation() {
        final class Params {
            boolean called;

            int index;

            Rectangle rc;

            public void check(final int i, final Rectangle r) {
                assertTrue(called);
                assertEquals(i, index);
                if (r != null) {
                    assertEquals(rc, r);
                }
                called = false;
            }
        }
        final Params params = new Params();
        view = new CompositeViewImpl(root) {
            @Override
            protected void childAllocation(final int index, final Rectangle rc) {
                params.called = true;
                params.index = index;
                params.rc = rc;
            }
        };
        // Make insets
        view.setParagraphInsets(getAttributeSet());
        assertNull(view.getChildAllocation(0, null));
        params.check(0, null);
        Shape alloc = view.getChildAllocation(1, shape);
        // The shape parameter is passed through getInsideAllocation
        params.check(1, new Rectangle(7, 0, 100 - 7 - 4, 200 - 5));
        assertSame(params.rc, alloc);
        assertSame(alloc, view.getChildAllocation(1, new Rectangle(0, 0, 300, 200)));
    }

    /**
     * General tests for <code>replace</code> method.
     */
    public void testReplace01() throws BadLocationException {
        int count = root.getElementCount();
        assertEquals(count, view.getViewCount());
        int end = root.getElement(0).getEndOffset();
        doc.insertString(0, "\nnew1\nnew2", null); // inserted 10 chars
        View[] views = new View[] { new PlainView(root.getElement(0)),
                new PlainView(root.getElement(1)) };
        View[] removed = new View[count - 1 - 1];
        for (int i = 0, index = 1; i < removed.length; i++, index++) {
            removed[i] = view.getView(index);
            assertSame(view, removed[i].getParent());
        }
        view.replace(1, count - 1, views);
        assertEquals(3, view.getViewCount()); // removed all except [0], added 2
        // Check first item is not deleted
        assertEquals(0, view.getView(0).getStartOffset());
        assertEquals(end + 10, view.getView(0).getEndOffset());
        // Check other items
        assertSame(views[0], view.getView(1));
        assertSame(views[1], view.getView(2));
        // Check parents of views added
        assertSame(view, views[0].getParent());
        assertSame(view, views[1].getParent());
        // Check removed views have no parent
        for (int i = 0; i < removed.length; i++) {
            assertNull(removed[i].getParent());
        }
    }

    public void testGetView() {
        assertSame(root.getElement(0), view.getView(0).getElement());
        assertTrue(view.getView(0) instanceof PlainView);
        assertSame(root.getElement(1), view.getView(1).getElement());
        // Invalid indexes
        try {
            view.getView(-1);
            fail("ArrayIndexOutOfBoundsException must be thrown");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        try {
            view.getView(view.getViewCount());
            fail("ArrayIndexOutOfBoundsException must be thrown");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    public void testGetViewCount() {
        assertEquals(root.getElementCount(), view.getViewCount());
    }

    public void testSetParent() {
        view = new CompositeViewImpl(root) {
            @Override
            protected void loadChildren(final ViewFactory factory) {
                childrenLoaded = true;
                assertSame(getViewFactory(), factory);
                super.loadChildren(factory);
            }
        };
        View parent = new PlainView(root.getElement(0)) {
            @Override
            public ViewFactory getViewFactory() {
                return factory;
            }
        };
        assertFalse(childrenLoaded);
        view.setParent(parent);
        assertTrue(childrenLoaded);
        assertSame(parent, view.getParent());
        // Call setParent one more time
        childrenLoaded = false;
        view.setParent(new PlainView(root));
        // As the view already has children, loadChildren must not be called
        assertFalse(childrenLoaded);
    }

    public void testSetParentNull() throws Exception {
        view = new CompositeViewImpl(root) {
            @Override
            protected void loadChildren(final ViewFactory factory) {
                childrenLoaded = true;
                assertSame(getViewFactory(), factory);
                super.loadChildren(factory);
            }
        };
        assertEquals(0, view.getViewCount());
        assertFalse(childrenLoaded);
        view.setParent(null);
        assertFalse(childrenLoaded);
    }

    public void testSetParentNoViewFactory() throws Exception {
        view = new CompositeViewImpl(root) {
            @Override
            protected void loadChildren(final ViewFactory factory) {
                childrenLoaded = true;
                assertNull(factory);
                super.loadChildren(factory);
            }
        };
        assertEquals(0, view.getViewCount());
        assertFalse(childrenLoaded);
        final PlainView parent = new PlainView(root);
        assertNull(parent.getViewFactory());
        view.setParent(parent);
        assertTrue(childrenLoaded);
    }

    public void testCompositeView() {
        view = new CompositeViewImpl(root);
        assertEquals(0, view.getViewCount());
        assertNull(view.getViewFactory());
        assertNull(view.getParent());
    }

    public void testGetInsideAllocation() {
        view.setParagraphInsets(getAttributeSet());
        Rectangle rc = view.getInsideAllocation(new Rectangle(20, 30, 50, 40));
        assertEquals(20 + 7, rc.x);
        assertEquals(30 + 0, rc.y);
        assertEquals(50 - 7 - 4, rc.width);
        assertEquals(40 - 0 - 5, rc.height);
        // Returns the same instance whenever called
        assertSame(rc, view.getInsideAllocation(new Rectangle(rc)));
        assertNull(view.getInsideAllocation(null));
    }

    // Regression test for HARMONY-2189
    public void testGetInsideAllocationOverridden() {
        final Marker top = new Marker();
        final Marker left = new Marker();
        final Marker bottom = new Marker();
        final Marker right = new Marker();

        view = new CompositeViewImpl(root = doc.getDefaultRootElement()) {
            @Override
            protected short getTopInset() {
                top.setOccurred();
                return 12;
            }

            @Override
            protected short getLeftInset() {
                left.setOccurred();
                return 11;
            }

            @Override
            protected short getBottomInset() {
                bottom.setOccurred();
                return 3;
            }

            @Override
            protected short getRightInset() {
                right.setOccurred();
                return 9;
            }
        };
        view.loadChildren(factory);

        view.setParagraphInsets(getAttributeSet());
        assertFalse(top.isOccurred());
        assertFalse(left.isOccurred());
        assertFalse(bottom.isOccurred());
        assertFalse(right.isOccurred());

        Rectangle rc = view.getInsideAllocation(new Rectangle(20, 30, 50, 40));
        assertTrue(top.isOccurred());
        assertTrue(left.isOccurred());
        assertTrue(bottom.isOccurred());
        assertTrue(right.isOccurred());

        assertEquals(20 + 11, rc.x);
        assertEquals(30 + 12, rc.y);
        assertEquals(50 - 11 - 9, rc.width);
        assertEquals(40 - 12 - 3, rc.height);
    }

    /*
     * See tests for individual methods namely EastWest and NorthSouth in
     * class CompositeView_NextNSVisPosTest
     * @throws BadLocationException
     */
    public void testGetNextVisualPositionFrom() throws BadLocationException {
        final class CompositeViewNextPos extends CompositeViewImpl {
            public CompositeViewNextPos(final Element element) {
                super(element);
            }

            int offset;

            Position.Bias bias;

            Shape shape;

            int dir;

            boolean isEastWest;

            boolean isNorthSouth;

            @Override
            protected int getNextEastWestVisualPositionFrom(final int pos,
                    final Position.Bias b, final Shape a, final int direction,
                    final Position.Bias[] biasRet) {
                isEastWest = true;
                isNorthSouth = false;
                offset = pos;
                bias = b;
                shape = a;
                dir = direction;
                return 0;
            }

            @Override
            protected int getNextNorthSouthVisualPositionFrom(final int pos,
                    final Position.Bias b, final Shape a, final int direction,
                    final Position.Bias[] biasRet) {
                isEastWest = false;
                isNorthSouth = true;
                offset = pos;
                bias = b;
                shape = a;
                dir = direction;
                return 0;
            }
        }
        CompositeViewNextPos view = new CompositeViewNextPos(root);
        view.getNextVisualPositionFrom(0, Bias.Backward, shape, SwingConstants.EAST, bias);
        assertTrue(view.isEastWest);
        view.isEastWest = false;
        assertFalse(view.isNorthSouth);
        assertEquals(0, view.offset);
        assertSame(Bias.Backward, view.bias);
        assertSame(shape, view.shape);
        assertEquals(SwingConstants.EAST, view.dir);
        view.getNextVisualPositionFrom(5, Bias.Forward, shape, SwingConstants.WEST, bias);
        assertTrue(view.isEastWest);
        view.isEastWest = false;
        assertFalse(view.isNorthSouth);
        assertEquals(5, view.offset);
        assertSame(Bias.Forward, view.bias);
        assertSame(shape, view.shape);
        assertEquals(SwingConstants.WEST, view.dir);
        // Vertical directions
        view.getNextVisualPositionFrom(1, Bias.Backward, shape, SwingConstants.SOUTH, bias);
        assertFalse(view.isEastWest);
        assertTrue(view.isNorthSouth);
        view.isNorthSouth = false;
        assertEquals(1, view.offset);
        assertSame(Bias.Backward, view.bias);
        assertSame(shape, view.shape);
        assertEquals(SwingConstants.SOUTH, view.dir);
        view.getNextVisualPositionFrom(0, Bias.Forward, shape, SwingConstants.NORTH, bias);
        assertFalse(view.isEastWest);
        assertTrue(view.isNorthSouth);
        view.isNorthSouth = false;
        assertEquals(0, view.offset);
        assertSame(Bias.Forward, view.bias);
        assertSame(shape, view.shape);
        assertEquals(SwingConstants.NORTH, view.dir);
        try {
            view.getNextVisualPositionFrom(0, Bias.Backward, shape, SwingConstants.NORTH_EAST,
                    bias);
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testFlipEastAndWestAtEnds() {
        assertFalse(view.flipEastAndWestAtEnds(0, Position.Bias.Backward));
    }

    /**
     * Tests the number of children before and after the call.
     */
    public void testLoadChildren01() {
        view = new CompositeViewImpl(root);
        assertEquals(0, view.getViewCount());
        view.loadChildren(factory);
        assertEquals(root.getElementCount(), view.getViewCount());
        assertSame(view, view.getView(0).getParent());
        assertSame(root.getElement(0), view.getView(0).getElement());
    }

    public void testLoadChildrenNull() {
        view = new CompositeViewImpl(root);
        assertEquals(0, view.getViewCount());
        view.loadChildren(null);
        assertEquals(0, view.getViewCount());
    }

    /**
     * This tests <code>int getViewIndex(int, Bias)</code>.
     * <p>The object is constructed upon root element of plain text document.
     * This element starts at document start (offset of 0) and ends at
     * document end (<code>view.getEndOffset() == doc.getLength()</code>).
     */
    public void testGetViewIndexintBias() {
        final Marker marker = new Marker(true);
        view = new CompositeViewImpl(root) {
            @Override
            protected int getViewIndexAtPosition(final int pos) {
                marker.setOccurred();
                modelPosition = pos;
                return -10101;
            }
        };
        view.loadChildren(factory);
        // Before document beginning
        assertEquals(-1, view.getViewIndex(-1, Bias.Forward));
        assertFalse(marker.isOccurred());
        assertEquals(-1, view.getViewIndex(-1, Bias.Backward));
        assertFalse(marker.isOccurred());
        // The first position (0)
        view.getViewIndex(0, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(0, modelPosition);
        assertEquals(-1, view.getViewIndex(0, Bias.Backward));
        assertFalse(marker.isOccurred());
        // In the middle
        view.getViewIndex(4, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(4, modelPosition);
        view.getViewIndex(4, Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(3, modelPosition);
        view.getViewIndex(7, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(7, modelPosition);
        view.getViewIndex(7, Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(6, modelPosition);
        // At an edge
        view.getViewIndex(5, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(5, modelPosition);
        view.getViewIndex(5, Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(4, modelPosition);
        view.getViewIndex(6, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(6, modelPosition);
        view.getViewIndex(6, Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(5, modelPosition);
        // In the end
        final int lastOffset = doc.getLength();
        view.getViewIndex(doc.getLength(), Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(doc.getLength(), modelPosition);
        view.getViewIndex(doc.getLength(), Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(lastOffset - 1, modelPosition);
        assertEquals(-1, view.getViewIndex(doc.getLength() + 3, Bias.Forward));
        assertFalse(marker.isOccurred());
        assertEquals(-1, view.getViewIndex(doc.getLength() + 3, Bias.Backward));
        assertFalse(marker.isOccurred());
    }

    /**
     * This tests <code>int getViewIndex(int, Bias)</code>.
     * <p>The object is constructed upon element which represents a paragraph
     * of a styled text document. The element niether starts at 0, nor it ends
     * at <code>doc.getLength()</code>.
     */
    public void testGetViewIndexintBiasNotRoot() throws BadLocationException {
        doc = new DefaultStyledDocument();
        final MutableAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        final MutableAttributeSet italic = new SimpleAttributeSet();
        StyleConstants.setItalic(italic, true);
        doc.insertString(doc.getLength(), "line1\n", null); //  0 (6)
        doc.insertString(doc.getLength(), "plain", null); //  6 (5) [ 6, 11]
        doc.insertString(doc.getLength(), "bold", bold); // 11 (4) [11, 15]
        doc.insertString(doc.getLength(), "italic", italic); // 15 (6) [15, 21]
        doc.insertString(doc.getLength(), "\nline3: ", null); // 21 (8) [21, 22]
        doc.insertString(doc.getLength(), "bold2", bold); // 29 (5)
        root = doc.getDefaultRootElement();
        final Element paragraph = root.getElement(1);
        assertEquals(4, paragraph.getElementCount());
        assertEquals(6, paragraph.getStartOffset());
        assertEquals(22, paragraph.getEndOffset());
        final Marker marker = new Marker(true);
        view = new CompositeViewImpl(paragraph) {
            @Override
            protected int getViewIndexAtPosition(final int pos) {
                marker.setOccurred();
                modelPosition = pos;
                return -10101;
            }
        };
        // Init the view by adding children
        view.loadChildren(factory);
        // Before document beginning
        assertEquals(-1, view.getViewIndex(-1, Bias.Forward));
        assertFalse(marker.isOccurred());
        assertEquals(-1, view.getViewIndex(-1, Bias.Backward));
        assertFalse(marker.isOccurred());
        // Before the start offset (6)
        assertEquals(-1, view.getViewIndex(0, Bias.Forward));
        assertFalse(marker.isOccurred());
        assertEquals(-1, view.getViewIndex(0, Bias.Backward));
        assertFalse(marker.isOccurred());
        assertEquals(-1, view.getViewIndex(3, Bias.Forward));
        assertFalse(marker.isOccurred());
        assertEquals(-1, view.getViewIndex(3, Bias.Backward));
        assertFalse(marker.isOccurred());
        // At the start offset (6)
        assertEquals(-1, view.getViewIndex(5, Bias.Forward));
        assertFalse(marker.isOccurred());
        assertEquals(-1, view.getViewIndex(5, Bias.Backward));
        assertFalse(marker.isOccurred());
        assertEquals(-10101, view.getViewIndex(6, Bias.Forward));
        assertTrue(marker.isOccurred());
        assertEquals(6, modelPosition);
        assertEquals(-1, view.getViewIndex(6, Bias.Backward));
        assertFalse(marker.isOccurred());
        assertEquals(-10101, view.getViewIndex(7, Bias.Forward));
        assertTrue(marker.isOccurred());
        assertEquals(7, modelPosition);
        assertEquals(-10101, view.getViewIndex(7, Bias.Backward));
        assertTrue(marker.isOccurred());
        assertEquals(6, modelPosition);
        // At an edge
        view.getViewIndex(10, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(10, modelPosition);
        view.getViewIndex(10, Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(9, modelPosition);
        view.getViewIndex(11, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(11, modelPosition);
        view.getViewIndex(11, Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(10, modelPosition);
        view.getViewIndex(12, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(12, modelPosition);
        view.getViewIndex(12, Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(11, modelPosition);
        // In the end
        view.getViewIndex(21, Bias.Forward);
        assertTrue(marker.isOccurred());
        assertEquals(21, modelPosition);
        view.getViewIndex(21, Bias.Backward);
        assertTrue(marker.isOccurred());
        assertEquals(20, modelPosition);
        assertEquals(-1, view.getViewIndex(22, Bias.Forward));
        assertFalse(marker.isOccurred());
        assertEquals(-10101, view.getViewIndex(22, Bias.Backward));
        assertTrue(marker.isOccurred());
        assertEquals(21, modelPosition);
        assertEquals(-1, view.getViewIndex(23, Bias.Forward));
        assertFalse(marker.isOccurred());
        assertEquals(-1, view.getViewIndex(23, Bias.Backward));
        assertFalse(marker.isOccurred());
        assertEquals(-1, view.getViewIndex(24, Bias.Forward));
        assertFalse(marker.isOccurred());
        assertEquals(-1, view.getViewIndex(24, Bias.Backward));
        assertFalse(marker.isOccurred());
    }

    public void testGetViewIndexAtPosition() {
        assertEquals(0, view.getViewIndexAtPosition(-1));
        assertEquals(0, view.getViewIndexAtPosition(0));
        assertEquals(0, view.getViewIndexAtPosition(5));
        assertEquals(1, view.getViewIndexAtPosition(6));
        assertEquals(1, view.getViewIndexAtPosition(11));
        assertEquals(2, view.getViewIndexAtPosition(12));
        assertEquals(view.getViewCount() - 1, view.getViewIndexAtPosition(doc.getLength()));
        assertEquals(view.getViewCount() - 1, view.getViewIndexAtPosition(doc.getLength() + 2));
    }

    public void testGetViewIndexAtPositionNotRoot() throws BadLocationException {
        doc = new DefaultStyledDocument();
        final MutableAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        final MutableAttributeSet italic = new SimpleAttributeSet();
        StyleConstants.setItalic(italic, true);
        doc.insertString(doc.getLength(), "line1\n", null); //  0 (6)
        doc.insertString(doc.getLength(), "plain", null); //  6 (5)
        doc.insertString(doc.getLength(), "bold", bold); // 11 (4)
        doc.insertString(doc.getLength(), "italic", italic); // 15 (6)
        doc.insertString(doc.getLength(), "\nline3: ", null); // 21 (8) / 22
        doc.insertString(doc.getLength(), "bold2", bold); // 29 (5)
        root = doc.getDefaultRootElement();
        final Element paragraph = root.getElement(1);
        assertEquals(4, paragraph.getElementCount());
        assertEquals(6, paragraph.getStartOffset());
        assertEquals(22, paragraph.getEndOffset());
        view = new CompositeViewImpl(paragraph);
        view.loadChildren(factory);
        assertEquals(0, view.getViewIndexAtPosition(-1));
        assertEquals(0, view.getViewIndexAtPosition(0));
        assertEquals(0, view.getViewIndexAtPosition(5));
        assertEquals(0, view.getViewIndexAtPosition(6));
        assertEquals(0, view.getViewIndexAtPosition(7));
        assertEquals(0, view.getViewIndexAtPosition(10));
        assertEquals(1, view.getViewIndexAtPosition(11));
        assertEquals(1, view.getViewIndexAtPosition(12));
        assertEquals(1, view.getViewIndexAtPosition(14));
        assertEquals(2, view.getViewIndexAtPosition(15));
        assertEquals(2, view.getViewIndexAtPosition(16));
        assertEquals(3, view.getViewIndexAtPosition(21));
        assertEquals(3, view.getViewIndexAtPosition(22));
        assertEquals(3, view.getViewIndexAtPosition(23));
        assertEquals(3, view.getViewIndexAtPosition(doc.getLength()));
    }

    public void testGetViewAtPosition() {
        Rectangle alloc;
        alloc = view.getInsideAllocation(shape);
        assertSame(view.getView(0), view.getViewAtPosition(-1, alloc));
        assertEquals(alloc, view.getChildAllocation(-1, shape));
        alloc = view.getInsideAllocation(shape);
        assertSame(view.getView(0), view.getViewAtPosition(0, alloc));
        assertEquals(alloc, view.getChildAllocation(0, shape));
        alloc = view.getInsideAllocation(shape);
        assertSame(view.getView(0), view.getViewAtPosition(5, alloc));
        assertEquals(alloc, view.getChildAllocation(5, shape));
        alloc = view.getInsideAllocation(shape);
        assertSame(view.getView(1), view.getViewAtPosition(6, alloc));
        assertEquals(alloc, view.getChildAllocation(6, shape));
        alloc = view.getInsideAllocation(shape);
        assertSame(view.getView(1), view.getViewAtPosition(11, alloc));
        assertEquals(alloc, view.getChildAllocation(11, shape));
        alloc = view.getInsideAllocation(shape);
        assertSame(view.getView(2), view.getViewAtPosition(12, alloc));
        assertEquals(alloc, view.getChildAllocation(12, shape));
        alloc = view.getInsideAllocation(shape);
        assertSame(view.getView(view.getViewCount() - 1), view.getViewAtPosition(doc
                .getLength(), alloc));
        assertEquals(alloc, view.getChildAllocation(doc.getLength(), shape));
    }

    /**
     * Keys for attribute set creation.
     */
    private static Object[] keys = { StyleConstants.SpaceAbove, StyleConstants.LeftIndent,
            StyleConstants.SpaceBelow, StyleConstants.RightIndent };

    /**
     * Values for attribute set creation.
     */
    private static Object[] values = { new Float(0.1f), // top inset
            new Float(7.0f), // left
            new Float(5.3f), // bottom
            new Float(4.7f) // right
    };

    /**
     * Creates an attribute set with a subset of key-value pairs.
     *
     * @param start the first index to use
     * @param end <code>(end - 1)</code> is the last index to use
     *
     * @return created attribute set
     */
    static AttributeSet getAttributeSet(final int start, final int end) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        for (int i = start; i < end; i++) {
            attrs.addAttribute(keys[i], values[i]);
        }
        return attrs;
    }

    /**
     * Creates attribute set with all the key-value pairs.
     *
     * @return created attribute set
     */
    static AttributeSet getAttributeSet() {
        return getAttributeSet(0, 4);
    }

    public void testGetLeftInset() {
        AttributeSet attrs = getAttributeSet(1, 2);
        view.setParagraphInsets(attrs);
        assertEquals(7.0f, StyleConstants.getLeftIndent(attrs), 0.00001f);
        assertInsets(0, 7, 0, 0);
    }

    public void testGetTopInset() {
        AttributeSet attrs = getAttributeSet(0, 1);
        view.setParagraphInsets(attrs);
        assertEquals(0.1f, StyleConstants.getSpaceAbove(attrs), 0.00001f);
        assertInsets(0, 0, 0, 0);
    }

    public void testGetRightInset() {
        AttributeSet attrs = getAttributeSet(3, 4);
        view.setParagraphInsets(attrs);
        assertEquals(4.7f, StyleConstants.getRightIndent(attrs), 0.00001f);
        assertInsets(0, 0, 0, 4);
    }

    public void testGetBottomInset() {
        AttributeSet attrs = getAttributeSet(2, 3);
        view.setParagraphInsets(attrs);
        assertEquals(5.3f, StyleConstants.getSpaceBelow(attrs), 0.00001f);
        assertInsets(0, 0, 5, 0);
    }

    /**
     * Checks view has the expected inset values.
     *
     * @param top expected top inset
     * @param left expected left inset
     * @param bottom expected bottom inset
     * @param right expected right inset
     */
    private void assertInsets(final short top, final short left, final short bottom,
            final short right) {
        assertEquals("Top", top, view.getTopInset());
        assertEquals("Left", left, view.getLeftInset());
        assertEquals("Bottom", bottom, view.getBottomInset());
        assertEquals("Right", right, view.getRightInset());
    }

    /**
     * Checks view has the expected inset values. A conveniece method
     * to convert <code>int</code> to <code>short</code>
     *
     * @param top expected top inset
     * @param left expected left inset
     * @param bottom expected bottom inset
     * @param right expected right inset
     */
    private void assertInsets(final int top, final int left, final int bottom, final int right) {
        assertInsets((short) top, (short) left, (short) bottom, (short) right);
    }

    public void testSetInsets() {
        assertInsets(0, 0, 0, 0);
        view.setInsets((short) 5, (short) 10, (short) 7, (short) 13);
        assertInsets(5, 10, 7, 13);
    }

    public void testSetParagraphInsets() {
        view.setParagraphInsets(getAttributeSet());
        assertInsets(0, 7, 5, 4);
    }
}
