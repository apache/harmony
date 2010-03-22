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
import javax.swing.BasicSwingTestCase;

public class BoxViewTest extends BasicSwingTestCase {
    private Document doc;

    private Element root;

    private BoxView view;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "line1\nline\t2", null);
        root = doc.getDefaultRootElement();
        view = new BoxView(root, View.X_AXIS);
    }

    public void testGetAlignment() {
        assertEquals(0.5f, view.getAlignment(View.X_AXIS), 0.00001f);
        assertEquals(0.5f, view.getAlignment(View.Y_AXIS), 0.00001f);
        try {
            view.getAlignment(3);
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testPreferenceChanged() {
        view.layout(width, height);
        assertTrue(view.isLayoutValid(View.X_AXIS));
        assertTrue(view.isLayoutValid(View.Y_AXIS));
        view.preferenceChanged(null, true, false);
        assertFalse(view.isLayoutValid(View.X_AXIS));
        assertTrue(view.isLayoutValid(View.Y_AXIS));
        view.layout(width, height);
        view.preferenceChanged(null, false, true);
        assertTrue(view.isLayoutValid(View.X_AXIS));
        assertFalse(view.isLayoutValid(View.Y_AXIS));
    }

    public void testReplace() {
        // Check child cound and size of the cache of allocations
        assertEquals(0, view.getViewCount());
        try {
            view.getOffset(view.getAxis(), 0);
            fail("ArrayIndexOutOfBoundsException is expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        try {
            view.getSpan(view.getAxis(), 0);
            fail("ArrayIndexOutOfBoundsException is expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        // Do layout and check it became valid
        view.layout(width, height);
        assertTrue(view.isLayoutValid(View.X_AXIS));
        assertTrue(view.isLayoutValid(View.Y_AXIS));
        // Add two children into the view
        view.replace(0, 0, new View[] { new PlainView(root), new PlainView(root) });
        // Check the side effects
        assertFalse(view.isLayoutValid(View.X_AXIS));
        assertFalse(view.isLayoutValid(View.Y_AXIS));
        // Now we can get offset and span of child at index 1
        view.getOffset(view.getAxis(), 1);
        view.getSpan(view.getAxis(), 1);
        // ...but can't get ones of child at index 2 as it doesn't exist
        try {
            view.getOffset(view.getAxis(), 2);
            fail("ArrayIndexOutOfBoundsException is expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        try {
            view.getSpan(view.getAxis(), 2);
            fail("ArrayIndexOutOfBoundsException is expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private static void assertCalled(final boolean[] called, final boolean layout,
            final boolean major, final boolean minor) {
        assertEquals(layout, called[0]);
        assertEquals(major, called[1]);
        assertEquals(minor, called[2]);
        // Reset flags
        called[0] = called[1] = called[2] = false;
    }

    public void testSetSize() {
        final boolean[] called = new boolean[] { false, false, false };
        view = new BoxView(root, View.X_AXIS) {
            @Override
            public void layout(final int w, final int h) {
                assertEquals(width - getLeftInset() - getRightInset(), w);
                assertEquals(height - getTopInset() - getBottomInset(), h);
                called[0] = true;
                super.layout(w, h);
            }

            @Override
            protected void layoutMajorAxis(final int targetSpan, final int axis,
                    final int[] offsets, final int[] spans) {
                called[1] = true;
                super.layoutMajorAxis(targetSpan, axis, offsets, spans);
            }

            @Override
            protected void layoutMinorAxis(final int targetSpan, final int axis,
                    final int[] offsets, final int[] spans) {
                called[2] = true;
                super.layoutMinorAxis(targetSpan, axis, offsets, spans);
            }
        };
        view.setSize(width, height);
        assertCalled(called, true, true, true);
        // Call with same sizes doesn't do the layout itself
        view.setSize(width, height);
        assertCalled(called, true, false, false);
        // Set only left inset, layout of the major axis (X) must be updated
        view.setInsets((short) 0, (short) 1, (short) 0, (short) 0);
        view.setSize(width, height);
        assertCalled(called, true, true, false);
        // Set new insets and try again
        view.setParagraphInsets(CompositeViewTest.getAttributeSet());
        view.setSize(width, height);
        assertCalled(called, true, true, true);
    }

    /**
     * Rectangle that is used in tests of is{After,Before}.
     */
    private static final Rectangle rect = new Rectangle(10, 15, 20, 30);

    /**
     * Tests isAfter with default axis value of X.
     *
     * Differences in values are marked with asterisk in trailing comment.
     */
    public void testIsAfter01() {
        assertEquals(View.X_AXIS, view.getAxis());
        assertFalse(view.isAfter(0, 0, rect));
        assertFalse(view.isAfter(9, 0, rect));
        assertFalse(view.isAfter(0, 14, rect));
        assertFalse(view.isAfter(9, 14, rect));
        assertFalse(view.isAfter(9, 15, rect));
        assertFalse(view.isAfter(9, 35, rect));
        assertFalse(view.isAfter(10, 14, rect));
        assertFalse(view.isAfter(10, 15, rect));
        assertFalse(view.isAfter(20, 15, rect));
        assertFalse(view.isAfter(20, 14, rect));
        assertFalse(view.isAfter(20, 45, rect));
        assertFalse(view.isAfter(20, 46, rect)); // *
        assertFalse(view.isAfter(20, 50, rect)); // *
        assertFalse(view.isAfter(30, 20, rect));
        assertTrue(view.isAfter(31, 20, rect)); // *
        assertTrue(view.isAfter(31, 15, rect)); // *
        assertTrue(view.isAfter(31, 0, rect)); // *
    }

    /**
     * Tests isAfter with axis value set to Y.
     *
     * Differences in values are marked with asterisk in trailing comment.
     */
    public void testIsAfter02() {
        view.setAxis(View.Y_AXIS);
        assertFalse(view.isAfter(0, 0, rect));
        assertFalse(view.isAfter(9, 0, rect));
        assertFalse(view.isAfter(0, 14, rect));
        assertFalse(view.isAfter(9, 14, rect));
        assertFalse(view.isAfter(9, 15, rect));
        assertFalse(view.isAfter(9, 35, rect));
        assertFalse(view.isAfter(10, 14, rect));
        assertFalse(view.isAfter(10, 15, rect));
        assertFalse(view.isAfter(20, 15, rect));
        assertFalse(view.isAfter(20, 14, rect));
        assertFalse(view.isAfter(20, 45, rect));
        assertTrue(view.isAfter(20, 46, rect)); // *
        assertTrue(view.isAfter(20, 50, rect)); // *
        assertFalse(view.isAfter(30, 20, rect));
        assertFalse(view.isAfter(31, 20, rect)); // *
        assertFalse(view.isAfter(31, 15, rect)); // *
        assertFalse(view.isAfter(31, 0, rect)); // *
    }

    /**
     * Tests isBefore with default axis value of X.
     *
     * Differences in values are marked with asterisk in trailing comment.
     */
    public void testIsBefore01() {
        assertTrue(view.isBefore(0, 0, rect));
        assertTrue(view.isBefore(9, 0, rect));
        assertTrue(view.isBefore(0, 14, rect));
        assertTrue(view.isBefore(9, 14, rect));
        assertTrue(view.isBefore(9, 15, rect)); // *
        assertTrue(view.isBefore(9, 35, rect)); // *
        assertFalse(view.isBefore(10, 14, rect)); // *
        assertFalse(view.isBefore(10, 15, rect));
        assertFalse(view.isBefore(20, 15, rect));
        assertFalse(view.isBefore(20, 14, rect)); // *
    }

    /**
     * Tests isBefore with axis value set to Y.
     *
     * Differences in values are marked with asterisk in trailing comment.
     */
    public void testIsBefore02() {
        view.setAxis(View.Y_AXIS);
        assertTrue(view.isBefore(0, 0, rect));
        assertTrue(view.isBefore(9, 0, rect));
        assertTrue(view.isBefore(0, 14, rect));
        assertTrue(view.isBefore(9, 14, rect));
        assertFalse(view.isBefore(9, 15, rect)); // *
        assertFalse(view.isBefore(9, 35, rect)); // *
        assertTrue(view.isBefore(10, 14, rect)); // *
        assertFalse(view.isBefore(10, 15, rect));
        assertFalse(view.isBefore(20, 15, rect));
        assertTrue(view.isBefore(20, 14, rect)); // *
    }

    public void testBoxView() {
        view = new BoxView(root, View.X_AXIS);
        assertSame(root, view.getElement());
        assertEquals(View.X_AXIS, view.getAxis());
        view = new BoxView(root, View.Y_AXIS);
        assertSame(root, view.getElement());
        assertEquals(View.Y_AXIS, view.getAxis());
        final boolean[] setAxisCalled = new boolean[1];
        view = new BoxView(root, -1251) {
            @Override
            public void setAxis(int axis) {
                setAxisCalled[0] = true;
                super.setAxis(axis);
            }
        };
        assertSame(root, view.getElement());
        assertEquals(-1251, view.getAxis());
        assertFalse(setAxisCalled[0]);
    }

    public void testGetAxis() {
        assertEquals(View.X_AXIS, view.getAxis());
    }

    public void testGetHeight() {
        assertEquals(0, view.getHeight());
        assertFalse(view.isLayoutValid(View.X_AXIS));
        view.layout(width, height);
        assertEquals(height, view.getHeight());
        view.setSize(width, height);
        assertEquals(height, view.getHeight());
        // Layout invalidation doesn't change the height
        view.layoutChanged(View.Y_AXIS);
        assertEquals(height, view.getHeight());
        // Put some insets
        view.setInsets((short) 10, (short) 0, (short) 0, (short) 0);
        view.setSize(width, height);
        if (isHarmony()) {
            assertEquals(height - view.getTopInset(), view.getHeight());
        } else {
            // The width isn't changed
            assertEquals(height, view.getHeight());
        }
        view.setInsets((short) 0, (short) 0, (short) 10, (short) 0);
        view.setSize(width, height);
        if (isHarmony()) {
            assertEquals(height - view.getBottomInset(), view.getHeight());
        } else {
            // The width is reduced by right inset twice
            assertEquals(height - 2 * view.getBottomInset(), view.getHeight());
        }
        // No matter if insets are set, layout width isn't changed
        view.setInsets((short) 7, (short) 0, (short) 4, (short) 0);
        view.layout(width, height);
        if (isHarmony()) {
            assertEquals(height, view.getHeight());
        } else {
            assertEquals(height + (view.getTopInset() - view.getBottomInset()), view
                    .getHeight());
        }
    }

    public void testGetWidth() {
        assertEquals(0, view.getWidth());
        assertFalse(view.isLayoutValid(View.X_AXIS));
        view.layout(width, height);
        assertEquals(width, view.getWidth());
        view.setSize(width, height);
        assertEquals(width, view.getWidth());
        // Layout invalidation doesn't change the width
        view.layoutChanged(View.X_AXIS);
        assertEquals(width, view.getWidth());
        // Put some insets
        view.setInsets((short) 0, (short) 10, (short) 0, (short) 0);
        view.setSize(width, height);
        if (isHarmony()) {
            assertEquals(width - view.getLeftInset(), view.getWidth());
        } else {
            // The width isn't changed
            assertEquals(width, view.getWidth());
        }
        view.setInsets((short) 0, (short) 0, (short) 0, (short) 10);
        view.setSize(width, height);
        if (isHarmony()) {
            assertEquals(width - view.getRightInset(), view.getWidth());
        } else {
            // The width is reduced by right inset twice
            assertEquals(width - 2 * view.getRightInset(), view.getWidth());
        }
        // No matter if insets are set, layout width isn't changed
        view.setInsets((short) 0, (short) 7, (short) 0, (short) 4);
        view.layout(width, height);
        if (isHarmony()) {
            assertEquals(width, view.getWidth());
        } else {
            assertEquals(width + (view.getLeftInset() - view.getRightInset()), view.getWidth());
        }
    }

    public void testIsAllocationValid() {
        assertFalse(view.isAllocationValid());
        view.layout(width, height);
        assertTrue(view.isAllocationValid());
        view.preferenceChanged(null, true, false);
        assertFalse(view.isAllocationValid());
    }

    public void testIsLayoutValid() {
        assertFalse(view.isLayoutValid(View.X_AXIS));
        assertFalse(view.isLayoutValid(View.Y_AXIS));
        view.layout(width, height);
        assertTrue(view.isLayoutValid(View.X_AXIS));
        assertTrue(view.isLayoutValid(View.Y_AXIS));
    }

    private final int width = 150;

    private final int height = 325;

    public void testLayout() {
        final boolean[] called = new boolean[] { false, false };
        view = new BoxView(root, View.X_AXIS) {
            @Override
            protected void layoutMajorAxis(final int targetSpan, final int axis,
                    final int[] offsets, final int[] spans) {
                called[0] = true;
                assertEquals(width, targetSpan);
                assertEquals(X_AXIS, axis);
                super.layoutMajorAxis(targetSpan, axis, offsets, spans);
            }

            @Override
            protected void layoutMinorAxis(final int targetSpan, final int axis,
                    final int[] offsets, final int[] spans) {
                called[1] = true;
                assertEquals(height, targetSpan);
                assertEquals(Y_AXIS, axis);
                super.layoutMinorAxis(targetSpan, axis, offsets, spans);
            }
        };
        assertFalse(view.isLayoutValid(View.X_AXIS));
        assertFalse(view.isLayoutValid(View.Y_AXIS));
        view.layout(width, height);
        assertTrue(view.isLayoutValid(View.X_AXIS));
        assertTrue(view.isLayoutValid(View.Y_AXIS));
        assertTrue(called[0]);
        called[0] = false;
        assertTrue(called[1]);
        called[1] = false;
        view.preferenceChanged(null, true, false);
        assertFalse(view.isLayoutValid(View.X_AXIS));
        assertTrue(view.isLayoutValid(View.Y_AXIS));
        view.layout(width, height);
        assertTrue(called[0]);
        called[0] = false;
        assertFalse(called[1]);
        called[1] = false;
    }

    /**
     * Tests that <code>layoutChanged</code> isn't called
     * when width or height passed to
     * <code>layout</code> is changed.
     */
    public void testLayout02() {
        final boolean[] called = new boolean[] { false };
        view = new BoxView(root, View.X_AXIS) {
            @Override
            public void layoutChanged(int axis) {
                called[0] = false;
                super.layoutChanged(axis);
            }
        };
        assertFalse(view.isLayoutValid(View.X_AXIS));
        assertFalse(view.isLayoutValid(View.Y_AXIS));
        view.layout(width, height);
        assertTrue(view.isLayoutValid(View.X_AXIS));
        assertTrue(view.isLayoutValid(View.Y_AXIS));
        assertFalse(called[0]);
        view.layout(width, height);
        assertFalse(called[0]);
        view.layout(width - 10, height);
        assertFalse(called[0]);
        view.layout(width - 10, height - 20);
        assertFalse(called[0]);
    }

    public void testLayoutChanged() {
        view.layout(width, height);
        assertTrue(view.isLayoutValid(View.X_AXIS));
        assertTrue(view.isLayoutValid(View.Y_AXIS));
        view.layoutChanged(View.Y_AXIS);
        assertTrue(view.isLayoutValid(View.X_AXIS));
        assertFalse(view.isLayoutValid(View.Y_AXIS));
    }

    public void testSetAxis() {
        view.setAxis(View.Y_AXIS);
        assertEquals(View.Y_AXIS, view.getAxis());
        view.setAxis(View.X_AXIS);
        assertEquals(View.X_AXIS, view.getAxis());
        view.setAxis(-1251);
        assertEquals(-1251, view.getAxis());
    }
    /*
     public void testPaint() {
     }

     public void testPaintChild() {
     }
     */
}
