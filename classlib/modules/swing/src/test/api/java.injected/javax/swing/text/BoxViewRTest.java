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
import javax.swing.SizeRequirements;
import javax.swing.text.ViewTestHelpers.ChildView;
import javax.swing.text.ViewTestHelpers.ChildrenFactory;

public class BoxViewRTest extends BasicSwingTestCase {
    private Document doc;

    private Element root;

    private BoxView view;

    private static final int width = 150;

    private static final int height = 325;

    private static class Child extends ChildView {
        private int width = -1;

        private int height = -1;

        public Child(final Element element, final int id) {
            super(element, id);
        }

        @Override
        public void setSize(float width, float height) {
            this.width = (int) width;
            this.height = (int) height;
            super.setSize(width, height);
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        root = doc.getDefaultRootElement();
        view = new BoxView(root, View.Y_AXIS);
    }

    /**
     * Checks that children have their sizes set after setSize.
     */
    public void testSetSizeChildren() throws Exception {
        View[] children = new View[] { new Child(root.getElement(0), 0),
                new Child(root.getElement(1), 1), };
        view.replace(0, view.getViewCount(), children);
        view.setSize(width, height);
        Child child = (Child) view.getView(0);
        assertEquals(view.getSpan(View.X_AXIS, 0), child.getWidth());
        assertEquals(view.getSpan(View.Y_AXIS, 0), child.getHeight());
        child = (Child) view.getView(1);
        assertEquals(view.getSpan(View.X_AXIS, 1), child.getWidth());
        assertEquals(view.getSpan(View.Y_AXIS, 1), child.getHeight());
    }

    /**
     * Checks that children have their sizes set after layout.
     */
    public void testLayoutChildren() throws Exception {
        View[] children = new View[] { new Child(root.getElement(0), 0),
                new Child(root.getElement(1), 1), };
        view.replace(0, view.getViewCount(), children);
        view.layout(width, height);
        Child child = (Child) view.getView(0);
        final int childWidth = child.getWidth();
        final int childHeight = child.getHeight();
        assertEquals(view.getSpan(View.X_AXIS, 0), childWidth);
        assertEquals(view.getSpan(View.Y_AXIS, 0), childHeight);
        assertTrue(childWidth != -1);
        assertTrue(childHeight != -1);
        child = (Child) view.getView(1);
        assertEquals(view.getSpan(View.X_AXIS, 1), child.getWidth());
        assertEquals(view.getSpan(View.Y_AXIS, 1), child.getHeight());
    }

    /**
     * Tests that layout will succeed even if a child changes its preferences
     * while layout is being performed.
     * <p>There's no limit to layout tries in our implementation. Hence layout may
     * cause stack overflow if a child always changes its preferences.
     */
    public void testLayout() throws Exception {
        View[] children = new View[] { new Child(root.getElement(0), 0),
                new Child(root.getElement(1), 1) {
                    private int iteration = 0;

                    private int[] heights = { 20, 40, 15, 11, 23 };

                    @Override
                    public float getPreferredSpan(int axis) {
                        if (axis == X_AXIS) {
                            return super.getPreferredSpan(axis);
                        }
                        return heights[iteration < heights.length ? iteration
                                : heights.length];
                    }

                    @Override
                    public void setSize(float width, float height) {
                        super.setSize(width, height);
                        if (++iteration < heights.length) {
                            preferenceChanged(view, false, true);
                        }
                    }
                } };
        view.replace(0, view.getViewCount(), children);
        view.layout(300, 500);
        Rectangle shape = new Rectangle(300, 500);
        Rectangle[] allocs = { new Rectangle(0, 0, 25, 32),
                new Rectangle(56, 32, 75, isHarmony() ? 23 : 40) };
        for (int i = 0; i < view.getViewCount(); i++) {
            assertEquals("@ " + i, allocs[i], view.getChildAllocation(i, shape));
        }
        assertEquals(75, (int) view.getPreferredSpan(View.X_AXIS));
        assertEquals(isHarmony() ? 55 : 47, (int) view.getPreferredSpan(View.Y_AXIS));
    }

    /**
     * Tests that layout is invalidated upon change of axis.
     */
    public void testSetAxisLayout() {
        assertEquals(View.Y_AXIS, view.getAxis());
        view.layout(width, height);
        assertTrue(view.isAllocationValid());
        assertTrue(view.isLayoutValid(View.X_AXIS));
        assertTrue(view.isLayoutValid(View.Y_AXIS));
        view.setAxis(View.X_AXIS);
        assertFalse(view.isLayoutValid(View.X_AXIS));
        assertFalse(view.isLayoutValid(View.Y_AXIS));
        assertFalse(view.isAllocationValid());
    }

    public void testBaselineLayout() throws BadLocationException {
        doc.insertString(0, "uno\ndos\t2\ntres\ncuatro", null);
        final Marker major = new Marker(true);
        final Marker minor = new Marker(true);
        final Marker baseline = new Marker(true);
        view = new BoxView(root, View.Y_AXIS) {
            @Override
            protected SizeRequirements baselineRequirements(int axis, SizeRequirements r) {
                baseline.setOccurred();
                return super.baselineRequirements(axis, r);
            }

            @Override
            protected SizeRequirements calculateMajorAxisRequirements(int axis,
                    SizeRequirements r) {
                major.setOccurred();
                return super.calculateMajorAxisRequirements(axis, r);
            }

            @Override
            protected SizeRequirements calculateMinorAxisRequirements(int axis,
                    SizeRequirements r) {
                minor.setOccurred();
                return super.calculateMinorAxisRequirements(axis, r);
            }
        };
        view.loadChildren(new ChildrenFactory());
        final int[] offsets = new int[view.getViewCount()];
        final int[] spans = new int[view.getViewCount()];
        view.baselineLayout(width, View.X_AXIS, offsets, spans);
        assertFalse(baseline.isOccurred());
        assertFalse(major.isOccurred());
        assertTrue(minor.isOccurred());
        view.baselineLayout(height, View.Y_AXIS, offsets, spans);
        assertFalse(baseline.isOccurred());
        assertTrue(major.isOccurred());
        assertFalse(minor.isOccurred());
        view.baselineLayout(width, View.X_AXIS, offsets, spans);
        assertFalse(baseline.isOccurred());
        assertFalse(major.isOccurred());
        assertFalse(minor.isOccurred());
        view.baselineLayout(height, View.Y_AXIS, offsets, spans);
        assertFalse(baseline.isOccurred());
        assertFalse(major.isOccurred());
        assertFalse(minor.isOccurred());
    }

    /**
     * Test <code>getResizeWeight</code> where minor axis requirements state
     * they are not resizeble.
     */
    public void testGetResizeWeight() {
        view = new BoxView(root, View.Y_AXIS) {
            @Override
            protected SizeRequirements calculateMinorAxisRequirements(final int axis,
                    final SizeRequirements r) {
                SizeRequirements result = r != null ? r : new SizeRequirements();
                result.minimum = result.preferred = result.maximum = 121;
                return result;
            }
        };
        assertEquals(0, view.getResizeWeight(View.X_AXIS)); // minor
    }
}
